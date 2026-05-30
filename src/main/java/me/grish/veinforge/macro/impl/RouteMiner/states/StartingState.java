package me.grish.veinforge.macro.impl.RouteMiner.states;

import me.grish.veinforge.macro.impl.RouteMiner.RouteMinerMacro;
import me.grish.veinforge.util.InventoryUtil;

/**
 * The initial state of the Route Miner Macro.
 * This state checks if the player has the proper items to start macro.
 * If not, it will disable itself.
 */
public class StartingState implements RouteMinerMacroState {

   @Override
   public void onStart(RouteMinerMacro macro) {
      log("Entering Starting State");
   }

   @Override
   public RouteMinerMacroState onTick(RouteMinerMacro macro) {
      if (!InventoryUtil.areItemsInHotbar(macro.getNecessaryItems())) {
         macro.disable("Please put the following items in hotbar: " + InventoryUtil.getMissingItemsInHotbar(macro.getNecessaryItems()));
         return null;
      }

      return new GettingStatsState();
   }

   @Override
   public void onEnd(RouteMinerMacro macro) {
      log("Exiting Starting State");
   }

}
