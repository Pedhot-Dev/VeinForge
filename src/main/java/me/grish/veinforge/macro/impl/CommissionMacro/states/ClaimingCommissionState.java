package me.grish.veinforge.macro.impl.CommissionMacro.states;

import me.grish.veinforge.feature.impl.AutoCommissionClaim;
import me.grish.veinforge.macro.impl.CommissionMacro.Commission;
import me.grish.veinforge.macro.impl.CommissionMacro.CommissionMacro;

import java.util.List;

public class ClaimingCommissionState implements CommissionMacroState {
   @Override
   public void onStart(CommissionMacro macro) {
      log("Starting claiming commission state");
      macro.clearPendingCommission();
      AutoCommissionClaim.getInstance().start();
   }

   @Override
   public CommissionMacroState onTick(CommissionMacro macro) {

      if (AutoCommissionClaim.getInstance().isRunning()) {
         return this;
      }

      if (AutoCommissionClaim.getInstance().succeeded()) {
         List<Commission> newCommissions = AutoCommissionClaim.getInstance().getNextComm();

         if (newCommissions != null && !newCommissions.isEmpty()) {
            Commission guiCommission = newCommissions.get(0);
            if (guiCommission == Commission.COMMISSION_CLAIM) {
               log("Claiming successful, but GUI still reports claim state. Waiting for fresh commissions.");
               return new StartingState();
            }
            macro.setPendingCommission(guiCommission);
            macro.setCurrentCommission(guiCommission);
            log("Claiming successful. Next commission from GUI: " + guiCommission.getName()
                        + ". Starting pathing while waiting for tablist validation.");
         } else {
            log("Claiming successful, but no new commissions were read. Restarting cycle.");
            return new StartingState();
         }

         return new PathingState();
      }

      switch (AutoCommissionClaim.getInstance().claimError()) {
         case NONE:
            macro.disable("Auto commission claiming failed, but no error is detected. Please contact the developer.");
            break;
         case INACCESSIBLE_NPC:
            log("The NPC was inaccessible while claiming commission");
            return new WarpingState();
         case TIMEOUT:
            log("Timeout in auto commission claiming");
            return new StartingState();
         case NO_ITEMS:
            macro.disable("No royal pigeons found, but this shouldn't happen. Please contact the developer.");
            break;
         case NPC_NOT_UNLOCKED:
            macro.disable("You have not unlocked Emissaries at Commission Milestone 1. Please post mc logs in #bug-report if this is a mistake.");
            break;
      }
      return null;
   }

   @Override
   public void onEnd(CommissionMacro macro) {
      AutoCommissionClaim.getInstance().stop();
      log("Ending claiming commission state");
   }
}
