package me.grish.veinforge.macro.impl.CommissionMacro.states;

import me.grish.veinforge.macro.impl.CommissionMacro.Commission;
import me.grish.veinforge.macro.impl.CommissionMacro.CommissionMacro;

public class WaitingForCommissionUpdateState implements CommissionMacroState {
   private final long timeoutMs;
   private final long startedAtMs;

   public WaitingForCommissionUpdateState(long timeoutMs) {
      this.timeoutMs = timeoutMs;
      this.startedAtMs = System.currentTimeMillis();
   }

   @Override
   public void onStart(CommissionMacro macro) {
      log("Waiting for commission list confirmation after chat completion event.");
   }

   @Override
   public CommissionMacroState onTick(CommissionMacro macro) {
      if (macro.getCurrentCommission() == Commission.COMMISSION_CLAIM) {
         log("Tablist confirmed commission completion. Proceeding to claim flow.");
         return new PathingState();
      }

      if (System.currentTimeMillis() - startedAtMs >= timeoutMs) {
         log("Commission completion was not confirmed by tablist in time. Returning to main loop.");
         return new StartingState();
      }

      return this;
   }

   @Override
   public void onEnd(CommissionMacro macro) {
      log("Leaving waiting-for-confirmation state.");
   }
}
