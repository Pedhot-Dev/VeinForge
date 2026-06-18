package me.grish.veinforge.feature.impl.AutoMobKiller.states;

import me.grish.veinforge.feature.impl.AutoMobKiller.AutoMobKiller;
import me.grish.veinforge.feature.impl.Pathfinder;
import me.grish.veinforge.handler.RotationHandler;
import me.grish.veinforge.util.KeyBindUtil;
import me.grish.veinforge.util.helper.Clock;
import me.grish.veinforge.util.helper.RotationConfiguration;
import me.grish.veinforge.util.helper.Target;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.EntityHitResult;

public class KillState implements AutoMobKillerState {

    private static final double MELEE_RANGE_SQ = 9.0;
    private static final long LOST_SIGHT_REPATH_DELAY_MS = 150L;
    private static final long CHASE_REPATH_INTERVAL_MS = 100L;
    private static final long LAST_SEEN_TIMEOUT_MS = 180L;
    private static final long CLOSE_RANGE_STUCK_TIMEOUT_MS = 900L;
    private static final long REAIM_INTERVAL_MS = 240L;
    private static final long REAIM_ROTATION_TIME_MS = 90L;

    private final Minecraft mc = Minecraft.getInstance();
    private final Clock attackDelay = new Clock();
    private final Clock lostSightTimer = new Clock();
    private final Clock lastSeenTimer = new Clock();
    private final Clock closeRangeStuckTimer = new Clock();
    private final Clock reaimTimer = new Clock();
    private final Clock chaseRepathTimer = new Clock();
    private BlockPos lastChaseTarget = null;

    @Override
    public void onStart(AutoMobKiller mobKiller) {
        log("Entering Kill State");
        attackDelay.reset();
        lostSightTimer.reset();
        lastSeenTimer.reset();
        closeRangeStuckTimer.reset();
        reaimTimer.reset();
        chaseRepathTimer.reset();
        lastChaseTarget = null;
    }

    @Override
    public AutoMobKillerState onTick(AutoMobKiller mobKiller) {
        if (mobKiller.getTargetMob() == null) {
            Pathfinder.getInstance().stop();
            return new FindMobState();
        }

        if (!mobKiller.getTargetMob().isAlive()) {
            Pathfinder.getInstance().stop();
            RotationHandler.getInstance().stop();
            return new FindMobState();
        }

        double distanceSq = mc.player.distanceToSqr(mobKiller.getTargetMob());
        boolean inMeleeRange = distanceSq <= MELEE_RANGE_SQ;
        boolean hasLineOfSight = mc.player.hasLineOfSight(mobKiller.getTargetMob());
        double targetYDelta = mobKiller.getTargetMob().getY() - mc.player.getY();
        long lastSeenTimeoutMs = mobKiller.getSlayerProfile() == AutoMobKiller.SlayerProfile.GOBLIN ? 420L : LAST_SEEN_TIMEOUT_MS;
        long closeRangeStuckTimeoutMs = mobKiller.getSlayerProfile() == AutoMobKiller.SlayerProfile.GOBLIN ? 1_250L : CLOSE_RANGE_STUCK_TIMEOUT_MS;

        if (hasLineOfSight) {
            lastSeenTimer.reset();
        } else {
            if (!lastSeenTimer.isScheduled()) {
                lastSeenTimer.schedule(lastSeenTimeoutMs);
            } else if (lastSeenTimer.passed()) {
                mobKiller.blacklistTargetMob();
                Pathfinder.getInstance().stop();
                return new FindMobState();
            }
        }

        if (!hasLineOfSight || !inMeleeRange) {
            if (!lostSightTimer.isScheduled()) {
                lostSightTimer.schedule(LOST_SIGHT_REPATH_DELAY_MS);
            }

            if (lostSightTimer.passed()) {
                if (Pathfinder.getInstance().failed()) {
                    mobKiller.blacklistTargetMob();
                    Pathfinder.getInstance().stop();
                    return new FindMobState();
                }
                chaseTarget(mobKiller);
            }
        } else {
            lostSightTimer.reset();
        }

        if (inMeleeRange && hasLineOfSight) {
            if (!closeRangeStuckTimer.isScheduled()) {
                closeRangeStuckTimer.schedule(closeRangeStuckTimeoutMs);
            } else if (closeRangeStuckTimer.passed()) {
                mobKiller.blacklistTargetMob();
                Pathfinder.getInstance().stop();
                return new FindMobState();
            }
        } else {
            closeRangeStuckTimer.reset();
        }

        if (!reaimTimer.isScheduled() || reaimTimer.passed()) {
            RotationHandler.getInstance().easeTo(new RotationConfiguration(
                    new Target(mobKiller.getTargetMob()),
                    REAIM_ROTATION_TIME_MS,
                    null
            ));
            reaimTimer.schedule(REAIM_INTERVAL_MS);
        }

        if (!inMeleeRange || !hasLineOfSight) {
            KeyBindUtil.setKeyBindState(mc.options.keyJump, false);
            return this;
        }

        if (targetYDelta > 2.0 || targetYDelta < -5.0) {
            mobKiller.blacklistTargetMob();
            Pathfinder.getInstance().stop();
            return new FindMobState();
        }

        KeyBindUtil.setKeyBindState(mc.options.keyJump, targetYDelta < -2.0);

        boolean crosshairOnTarget = mc.hitResult instanceof EntityHitResult && ((EntityHitResult) mc.hitResult).getEntity() == mobKiller.getTargetMob();
        if (!crosshairOnTarget) {
            return this;
        }

        if (mc.player.getAttackStrengthScale(0.0F) < 0.92F) {
            return this;
        }

        if (attackDelay.isScheduled() && !attackDelay.passed()) {
            return this;
        }

        KeyBindUtil.leftClick();
        attackDelay.schedule(85);
        closeRangeStuckTimer.reset();
        return this;
    }

    @Override
    public void onEnd(AutoMobKiller mobKiller) {
        KeyBindUtil.setKeyBindState(mc.options.keyJump, false);
        log("Exiting Kill State");
    }

    private void chaseTarget(AutoMobKiller mobKiller) {
        if (chaseRepathTimer.isScheduled() && !chaseRepathTimer.passed()) {
            return;
        }

        BlockPos chaseTarget = mobKiller.getApproachBlockForTarget(false);
        if (chaseTarget == null) {
            return;
        }

        Pathfinder pathfinder = Pathfinder.getInstance();
        if (!chaseTarget.equals(lastChaseTarget) || !pathfinder.isRunning()) {
            pathfinder.stopAndRequeue(chaseTarget);
            lastChaseTarget = chaseTarget;
        }
        if (!pathfinder.isRunning()) {
            pathfinder.start();
        }
        chaseRepathTimer.schedule(CHASE_REPATH_INTERVAL_MS);
    }

}
