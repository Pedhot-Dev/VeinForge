package me.grish.veinforge.macro.impl.PowderMacro;

import lombok.Getter;
import lombok.Setter;
import me.grish.veinforge.VeinForge;
import me.grish.veinforge.feature.FeatureManager;
import me.grish.veinforge.feature.impl.AutoChestUnlocker;
import me.grish.veinforge.feature.impl.AutoGetStats.AutoGetStats;
import me.grish.veinforge.feature.impl.AutoGetStats.tasks.impl.MiningSpeedRetrievalTask;
import me.grish.veinforge.feature.impl.AutoGetStats.tasks.impl.PickaxeAbilityRetrievalTask;
import me.grish.veinforge.feature.impl.BlockMiner.BlockMiner;
import me.grish.veinforge.handler.GameStateHandler;
import me.grish.veinforge.macro.AbstractMacro;
import me.grish.veinforge.macro.impl.PowderMacro.states.PowderMacroState;
import me.grish.veinforge.macro.impl.PowderMacro.states.StartingState;
import me.grish.veinforge.util.helper.MineableBlock;
import me.grish.veinforge.util.helper.location.Location;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.ChestBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PowderMacro extends AbstractMacro {

   private static final int CHEST_SCAN_RADIUS_HORIZONTAL = 6;
   private static final int CHEST_SCAN_RADIUS_VERTICAL = 4;
   private static final long CHEST_SCAN_INTERVAL_MS = 300L;
   private static final long CHEST_RETRY_COOLDOWN_MS = 20_000L;
   private static final long ENVIRONMENT_INVALID_GRACE_MS = 7_000L;

   @Getter
   private static final PowderMacro instance = new PowderMacro();
   private final BlockMiner blockMiner = BlockMiner.getInstance();
   private final AutoChestUnlocker chestUnlocker = AutoChestUnlocker.instance;
   private final Map<BlockPos, Long> chestCooldowns = new HashMap<>();

   @Getter
   @Setter
   private PowderMacroState currentState;
   private MiningSpeedRetrievalTask miningSpeedRetrievalTask;
   private PickaxeAbilityRetrievalTask pickaxeAbilityRetrievalTask;
   @Getter
   @Setter
   private int miningSpeed = 0;
   @Getter
   @Setter
   private BlockMiner.PickaxeAbility pickaxeAbility = BlockMiner.PickaxeAbility.NONE;
   private long nextChestScanMs = 0L;
   private long environmentInvalidSinceMs = -1L;
   private String lastEnvironmentReason = "";

   @Override
   public String getName() {
      return "Powder Macro";
   }

   @Override
   public void onEnable() {
      resetRuntime();
      requestStats();
      this.currentState = new StartingState();
      this.currentState.onStart(this);
      log("Powder Macro enabled");
   }

   @Override
   public void onDisable() {
      if (this.currentState != null) {
         this.currentState.onEnd(this);
      }
      if (blockMiner.isRunning()) {
         blockMiner.stop();
      }
      if (chestUnlocker.isRunning()) {
         chestUnlocker.stop();
      }
      AutoChestUnlocker.chestQueue.clear();
      this.currentState = null;
      log("Powder Macro disabled");
   }

   @Override
   public void onPause() {
      FeatureManager.getInstance().pauseAll();
      log("Powder Macro paused");
   }

   @Override
   public void onResume() {
      FeatureManager.getInstance().resumeAll();
      log("Powder Macro resumed");
   }

   @Override
   public void onTick() {
      if (!this.isEnabled() || this.currentState == null || isTimerRunning()) {
         return;
      }

      PowderMacroState nextState = this.currentState.onTick(this);
      transitionTo(nextState);
   }

   public void transitionTo(PowderMacroState nextState) {
      if (nextState == null || this.currentState == nextState) {
         return;
      }

      this.currentState.onEnd(this);
      this.currentState = nextState;
      this.currentState.onStart(this);
   }

   public boolean updateStatsIfReady() {
      if (this.miningSpeed > 0) {
         return true;
      }
      if (this.miningSpeedRetrievalTask == null || this.pickaxeAbilityRetrievalTask == null) {
         return false;
      }
      if (!AutoGetStats.getInstance().hasFinishedAllTasks()) {
         return false;
      }
      if (this.miningSpeedRetrievalTask.getError() != null) {
         disable("Failed to get Mining Speed: " + this.miningSpeedRetrievalTask.getError());
         return false;
      }
      if (this.pickaxeAbilityRetrievalTask.getError() != null) {
         disable("Failed to get Pickaxe Ability: " + this.pickaxeAbilityRetrievalTask.getError());
         return false;
      }

      Integer resolvedMiningSpeed = this.miningSpeedRetrievalTask.getResult();
      if (resolvedMiningSpeed == null || resolvedMiningSpeed <= 0) {
         disable("Invalid mining speed received from AutoGetStats.");
         return false;
      }

      this.miningSpeed = resolvedMiningSpeed;
      this.pickaxeAbility = this.pickaxeAbilityRetrievalTask.getResult() == null
                                    ? BlockMiner.PickaxeAbility.NONE
                                    : this.pickaxeAbilityRetrievalTask.getResult();
      log("Loaded powder stats: miningSpeed=" + this.miningSpeed + ", pickaxeAbility=" + this.pickaxeAbility);
      return true;
   }

   public boolean validateEnvironment() {
      String invalidReason = getEnvironmentInvalidReason();
      if (invalidReason == null) {
         this.environmentInvalidSinceMs = -1L;
         this.lastEnvironmentReason = "";
         return true;
      }

      long now = System.currentTimeMillis();
      if (!invalidReason.equals(this.lastEnvironmentReason)) {
         this.lastEnvironmentReason = invalidReason;
         this.environmentInvalidSinceMs = now;
         return false;
      }

      if (this.environmentInvalidSinceMs == -1L) {
         this.environmentInvalidSinceMs = now;
         return false;
      }

      if (now - this.environmentInvalidSinceMs >= ENVIRONMENT_INVALID_GRACE_MS) {
         disable(invalidReason);
      }
      return false;
   }

   private String getEnvironmentInvalidReason() {
      Location location = GameStateHandler.getInstance().getCurrentLocation();
      if (location != Location.CRYSTAL_HOLLOWS) {
         return "Powder Macro requires Crystal Hollows. Current location: " + location.getName();
      }
      if (!isInsideAreaLock()) {
         return "Outside selected area lock (" + getAreaLockName(VeinForge.config().powderMacro.areaLock) + ").";
      }
      return null;
   }

   public boolean isInsideAreaLock() {
      int areaLock = VeinForge.config().powderMacro.areaLock;
      if (areaLock == 0) {
         return true;
      }
      if (mc.player == null) {
         return false;
      }

      double x = mc.player.getX();
      double z = mc.player.getZ();
      switch (areaLock) {
         case 1:
            return x < 512.0 && z < 512.0; // Jungle
         case 2:
            return x < 512.0 && z > 512.0; // Goblin Holdout
         case 3:
            return x > 512.0 && z < 512.0; // Mithril Deposits
         case 4:
            return x > 512.0 && z > 512.0; // Precursor Remnants
         default:
            return true;
      }
   }

   public boolean ensureMinerRunning() {
      if (this.blockMiner.isRunning()) {
         return true;
      }
      if (this.miningSpeed <= 0) {
         return false;
      }

      MineableBlock[] targets = getTargetBlocks();
      int[] priorities = new int[targets.length];
      for (int i = 0; i < priorities.length; i++) {
         priorities[i] = 1;
      }

      this.blockMiner.setWaitThreshold(VeinForge.config().general.oreRespawnWaitThreshold * 1000);
      this.blockMiner.start(
              targets,
              this.miningSpeed,
              this.pickaxeAbility,
              priorities,
              VeinForge.config().general.miningTool
      );
      return true;
   }

   public void restartMiner() {
      if (this.blockMiner.isRunning()) {
         this.blockMiner.stop();
      }
      this.blockMiner.setError(BlockMiner.BlockMinerError.NONE);
      ensureMinerRunning();
   }

   public BlockMiner.BlockMinerError getMinerError() {
      return this.blockMiner.getError();
   }

   public boolean isMinerRunning() {
      return this.blockMiner.isRunning();
   }

   public Optional<BlockPos> findNearbyChest() {
      if (VeinForge.config().powderMacro.ignoreChests || mc.player == null || mc.level == null) {
         return Optional.empty();
      }

      long now = System.currentTimeMillis();
      if (now < this.nextChestScanMs) {
         return Optional.empty();
      }
      this.nextChestScanMs = now + CHEST_SCAN_INTERVAL_MS;
      cleanupChestCooldowns(now);

      BlockPos playerPos = mc.player.blockPosition();
      BlockPos best = null;
      double bestDistance = Double.MAX_VALUE;

      for (int y = -CHEST_SCAN_RADIUS_VERTICAL; y <= CHEST_SCAN_RADIUS_VERTICAL; y++) {
         for (int x = -CHEST_SCAN_RADIUS_HORIZONTAL; x <= CHEST_SCAN_RADIUS_HORIZONTAL; x++) {
            for (int z = -CHEST_SCAN_RADIUS_HORIZONTAL; z <= CHEST_SCAN_RADIUS_HORIZONTAL; z++) {
               BlockPos scanPos = playerPos.offset(x, y, z);
               if (!(mc.level.getBlockState(scanPos).getBlock() instanceof ChestBlock)) {
                  continue;
               }
               if (this.chestCooldowns.containsKey(scanPos)) {
                  continue;
               }

               double distance = mc.player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(scanPos));
               if (distance < bestDistance) {
                  bestDistance = distance;
                  best = scanPos.immutable();
               }
            }
         }
      }

      return Optional.ofNullable(best);
   }

   public void beginChestSolve(BlockPos chestPos) {
      if (chestPos == null) {
         return;
      }

      setChestCooldown(chestPos, CHEST_RETRY_COOLDOWN_MS);

      if (this.blockMiner.isRunning()) {
         this.blockMiner.stop();
      }
      if (this.chestUnlocker.isRunning()) {
         this.chestUnlocker.stop();
      }

      AutoChestUnlocker.chestQueue.clear();
      AutoChestUnlocker.chestQueue.add(chestPos.immutable());

      String miningTool = VeinForge.config().general.miningTool == null ? "" : VeinForge.config().general.miningTool;
      this.chestUnlocker.start(miningTool, true);
   }

   public boolean isChestUnlockerRunning() {
      return this.chestUnlocker.isRunning();
   }

   public void stopChestUnlocker() {
      if (this.chestUnlocker.isRunning()) {
         this.chestUnlocker.stop();
      }
   }

   public void completeChestSolve() {
      this.nextChestScanMs = System.currentTimeMillis() + 800L;
   }

   @Override
   public List<String> getNecessaryItems() {
      List<String> items = new ArrayList<>();
      String miningTool = VeinForge.config().general.miningTool;
      if (miningTool != null && !miningTool.trim().isEmpty()) {
         items.add(miningTool);
      }
      return items;
   }

   private MineableBlock[] getTargetBlocks() {
      if (VeinForge.config().powderMacro.powderType == 1) {
         // Mithril powder mode still uses normal mining (no nuker), but switches target blocks.
         return new MineableBlock[]{
                 MineableBlock.GRAY_MITHRIL,
                 MineableBlock.GREEN_MITHRIL,
                 MineableBlock.BLUE_MITHRIL,
                 MineableBlock.TITANIUM
         };
      }

      // Gemstone powder mode mines hardstone normally.
      return new MineableBlock[]{MineableBlock.HARDSTONE};
   }

   private void requestStats() {
      this.miningSpeedRetrievalTask = new MiningSpeedRetrievalTask();
      this.pickaxeAbilityRetrievalTask = new PickaxeAbilityRetrievalTask();
      AutoGetStats.getInstance().startTask(this.miningSpeedRetrievalTask);
      AutoGetStats.getInstance().startTask(this.pickaxeAbilityRetrievalTask);
   }

   private void resetRuntime() {
      this.miningSpeed = 0;
      this.pickaxeAbility = BlockMiner.PickaxeAbility.NONE;
      this.miningSpeedRetrievalTask = null;
      this.pickaxeAbilityRetrievalTask = null;
      this.nextChestScanMs = 0L;
      this.environmentInvalidSinceMs = -1L;
      this.lastEnvironmentReason = "";
      this.chestCooldowns.clear();
      AutoChestUnlocker.chestQueue.clear();
   }

   private void cleanupChestCooldowns(long now) {
      Iterator<Map.Entry<BlockPos, Long>> iterator = this.chestCooldowns.entrySet().iterator();
      while (iterator.hasNext()) {
         Map.Entry<BlockPos, Long> entry = iterator.next();
         if (entry.getValue() <= now) {
            iterator.remove();
         }
      }
   }

   private void setChestCooldown(BlockPos chestPos, long cooldownMs) {
      this.chestCooldowns.put(chestPos.immutable(), System.currentTimeMillis() + cooldownMs);
   }

   private String getAreaLockName(int areaLock) {
      switch (areaLock) {
         case 1:
            return "Jungle";
         case 2:
            return "Goblin Holdout";
         case 3:
            return "Mithril Deposits";
         case 4:
            return "Precursor Remnants";
         default:
            return "None";
      }
   }
}
