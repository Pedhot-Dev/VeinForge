package me.grish.veinforge.feature.impl.AutoMobKiller.states;

import me.grish.veinforge.VeinForge;
import me.grish.veinforge.feature.impl.AutoMobKiller.AutoMobKiller;
import me.grish.veinforge.feature.impl.Pathfinder;
import me.grish.veinforge.util.PlayerUtil;
import me.grish.veinforge.util.helper.Clock;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public class PathfindingState implements AutoMobKillerState {

   private static final double ENTER_KILL_RANGE_SQ = 9.0;
   private static final double TARGET_DRIFT_REPATH_THRESHOLD_SQ = 6.25; // 2.5 blocks
   private static final int MAX_REPATH_ATTEMPTS = 4;
   private static final long PATHING_TIMEOUT_MS = 8_000L;
   private static final long REPATH_DELAY_MS = 180L;

   private final Clock timeout = new Clock();
   private final Clock repathDelay = new Clock();
   private int pathAttempts = 0;
   private BlockPos lastQueuedTarget = null;

   @Override
   public void onStart(AutoMobKiller mobKiller) {
      log("Entering pathfinding state");
      pathAttempts = 0;
      timeout.reset();
      timeout.schedule(PATHING_TIMEOUT_MS);
      repathDelay.reset();
      lastQueuedTarget = null;
      Pathfinder.getInstance().setSprintState(VeinForge.config().commission.dwarvenCommission.mobKillerSprint);
      Pathfinder.getInstance().setInterpolationState(VeinForge.config().commission.dwarvenCommission.mobKillerInterpolate);
      queuePathToTarget(mobKiller, true);
   }

   @Override
   public AutoMobKillerState onTick(AutoMobKiller mobKiller) {
      if (mobKiller.getTargetMob() == null) {
         Pathfinder.getInstance().stop();
         return new FindMobState();
      }

      if (isInKillRange(mobKiller)) {
         return new KillState();
      }

      if (!mobKiller.getTargetMob().isAlive()) {
         Pathfinder.getInstance().stop();
         log("Target mob is no longer alive. Re-choosing a mob.");
         return new StartingState();
      }

      if (mobKiller.getTargetMob().position().distanceToSqr(mobKiller.getTargetMobOriginalPos()) > TARGET_DRIFT_REPATH_THRESHOLD_SQ) {
         if (++pathAttempts > MAX_REPATH_ATTEMPTS) {
            log("Target mob moved away from original location too many times. Re-choosing a mob.");
            mobKiller.blacklistTargetMob();
            Pathfinder.getInstance().stop();
            return new StartingState();
         }
         mobKiller.setTargetMobOriginalPos(mobKiller.getTargetMob().position());
         queuePathToTarget(mobKiller, true);
         return this;
      }

      if (Pathfinder.getInstance().failed()) {
         log("Pathfinder failed while approaching mob. Blacklisting and finding another target.");
         mobKiller.blacklistTargetMob();
         Pathfinder.getInstance().stop();
         return new StartingState();
      }

      if (!Pathfinder.getInstance().isRunning()) {
         if (!repathDelay.isScheduled() || repathDelay.passed()) {
            if (++pathAttempts > MAX_REPATH_ATTEMPTS) {
               log("Pathfinding stopped too many times before reaching kill range. Re-choosing a mob.");
               mobKiller.blacklistTargetMob();
               Pathfinder.getInstance().stop();
               return new StartingState();
            }

            queuePathToTarget(mobKiller, false);
         }
      }

      if (timeout.passed()) {
         log("Pathfinding timeout. Re-choosing a mob.");
         mobKiller.blacklistTargetMob();
         Pathfinder.getInstance().stop();
         return new StartingState();
      }

      return this;
   }

   @Override
   public void onEnd(AutoMobKiller mobKiller) {
      // Do not forcibly stop on every transition; KillState can continue with active movement.
   }

   private boolean isInKillRange(AutoMobKiller mobKiller) {
      if (Minecraft.getInstance().player == null || mobKiller.getTargetMob() == null) {
         return false;
      }

      return PlayerUtil.getNextTickPosition().distanceToSqr(mobKiller.getTargetMob().position()) <= ENTER_KILL_RANGE_SQ
                  && Minecraft.getInstance().player.hasLineOfSight(mobKiller.getTargetMob());
   }

   private void queuePathToTarget(AutoMobKiller mobKiller, boolean forceRefreshApproachTarget) {
      Pathfinder pathfinder = Pathfinder.getInstance();
      BlockPos approachTarget = mobKiller.getApproachBlockForTarget(forceRefreshApproachTarget);
      if (approachTarget == null) {
         return;
      }

      boolean sameQueuedTarget = approachTarget.equals(lastQueuedTarget);
      if (!sameQueuedTarget || !pathfinder.isRunning()) {
         pathfinder.stopAndRequeue(approachTarget);
         lastQueuedTarget = approachTarget;
      }
      if (!pathfinder.isRunning()) {
         pathfinder.start();
      }
      repathDelay.schedule(REPATH_DELAY_MS);
   }
}
