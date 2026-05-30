package me.grish.veinforge.macro.impl.CommissionMacro.states;

import me.grish.veinforge.VeinForge;
import me.grish.veinforge.feature.impl.BlockMiner.BlockMiner;
import me.grish.veinforge.macro.impl.CommissionMacro.Commission;
import me.grish.veinforge.macro.impl.CommissionMacro.CommissionMacro;
import me.grish.veinforge.util.CommissionUtil;
import me.grish.veinforge.util.InventoryUtil;
import me.grish.veinforge.util.helper.MineableBlock;

import java.util.List;

public class MiningState implements CommissionMacroState {

   private final BlockMiner miner = BlockMiner.getInstance();
   private final MineableBlock[] blocksToMine = {MineableBlock.GRAY_MITHRIL, MineableBlock.GREEN_MITHRIL, MineableBlock.BLUE_MITHRIL,
           MineableBlock.TITANIUM};

   private final int[] mithrilPriority = {10, 6, 3, 1};
   private final int[] prioritiseTitanium = {10, 6, 3, 20};
   private final int[] titaniumPriority = {3, 2, 1, 20};

   @Override
   public void onStart(CommissionMacro macro) {
      log("Starting mining state");

      int[] priorityToUse;
      boolean hasActiveTitaniumCommission = hasActiveTitaniumCommission();
      if (hasActiveTitaniumCommission) {
         if (!macro.getCurrentCommission().getName().contains("Titanium")) {
            log("Detected overlapping titanium commission. Prioritizing titanium blocks.");
         }
         priorityToUse = titaniumPriority;
      } else {
         priorityToUse = VeinForge.config().commission.dwarvenCommission.prioritiseTitanium ? prioritiseTitanium : mithrilPriority;
      }

      miner.start(
              blocksToMine,
              macro.getMiningSpeed(),
              CommissionMacro.getInstance().getPickaxeAbility(),
              priorityToUse,
              VeinForge.config().general.miningTool
      );
   }

   @Override
   public CommissionMacroState onTick(CommissionMacro macro) {

      String miningTool = VeinForge.config().general.miningTool;
      if (miningTool.toLowerCase().contains("drill") || InventoryUtil.getFullName(miningTool).contains("Drill")) {
         //log("Fuel detected: " + InventoryUtil.getDrillRemainingFuel(miningTool));
         if (InventoryUtil.getDrillRemainingFuel(miningTool) <= 100) {
            log("Less than 100 fuel left in drill. Starting to refuel");
            if (VeinForge.config().general.drillRefuel)
               return new RefuelState();
            else {
               macro.disable("Very little fuel left in drill");
               return null;
            }
         }
      }

      if (macro.getCurrentCommission() == Commission.COMMISSION_CLAIM) {
         return new PathingState();
      }

      if (miner.isRunning()) {
         return this;
      }

      // TODO: Pathfind to a new vein when not enough blocks nearby
      switch (miner.getError()) {
         case NONE:
            break;
         case NO_POINTS_FOUND:
            log("Restarting because the block chosen cannot be mined");
            return new MiningState();
         case NOT_ENOUGH_BLOCKS:
            log("Not enough blocks nearby! Restarting macro");
            return new StartingState();
         case NO_PICKAXE_ABILITY:
            macro.disable("Cannot find messages for pickaxe ability! " +
                                  "Either enable any pickaxe ability in HOTM or enable chat messages. You can also disable pickaxe ability in configs.");
            break;
         default:
            logError("Block miner error: " + miner.getError().name());
            macro.disable("Block miner failed unexpectedly! Please send the logs to the developer");
            break;
      }
      return null;
   }

   @Override
   public void onEnd(CommissionMacro macro) {
      miner.stop();
      log("Ending mining state");
   }

   private boolean hasActiveTitaniumCommission() {
      List<Commission> activeCommissions = CommissionUtil.getCurrentCommissionsFromTablist();
      for (Commission commission : activeCommissions) {
         if (commission.getName().contains("Titanium")) {
            return true;
         }
      }
      return false;
   }
}
