package me.grish.veinforge.macro.impl.GlacialMacro.states;

import me.grish.veinforge.feature.impl.AutoWarp;
import me.grish.veinforge.macro.impl.GlacialMacro.GlacialMacro;
import me.grish.veinforge.util.helper.Clock;
import me.grish.veinforge.util.helper.location.SubLocation;

public class TeleportingState implements GlacialMacroState {
   private final GlacialMacroState nextState;
   private final Clock timeout = new Clock();
   private int retries = 0;

   // The state to transition to upon successful warp
   public TeleportingState(GlacialMacroState nextState) {
      this.nextState = nextState;
   }

   @Override
   public void onStart(GlacialMacro macro) {
      log("Attempting to warp to Dwarven Base Camp.");
      AutoWarp.getInstance().start(null, SubLocation.DWARVEN_BASE_CAMP);
      timeout.schedule(15000); // 15 second timeout for warping
   }

   @Override
   public GlacialMacroState onTick(GlacialMacro macro) {
      if (AutoWarp.getInstance().isRunning()) {
         if (timeout.passed()) {
            logError("AutoWarp timed out.");
            AutoWarp.getInstance().stop();
            return handleFailure();
         }
         return this; // Stay in this state while warping
      }

      if (AutoWarp.getInstance().hasSucceeded()) {
         log("Successfully warped.");
         return nextState;
      } else {
         logError("AutoWarp failed. Reason: " + AutoWarp.getInstance().getFailReason());
         return handleFailure();
      }
   }

   private GlacialMacroState handleFailure() {
      if (++retries > 3) {
         return new ErrorHandlingState("Failed to warp to the Glacite Tunnels after 3 attempts.");
      }
      log("Retrying warp (" + retries + "/3)...");
      AutoWarp.getInstance().start(null, SubLocation.DWARVEN_BASE_CAMP);
      timeout.schedule(15000);
      return this;
   }

   @Override
   public void onEnd(GlacialMacro macro) {
      if (AutoWarp.getInstance().isRunning()) {
         AutoWarp.getInstance().stop();
      }
   }
}