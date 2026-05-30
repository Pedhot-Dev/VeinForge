package me.grish.veinforge.macro.impl.PowderMacro.states;

import me.grish.veinforge.macro.impl.PowderMacro.PowderMacro;
import me.grish.veinforge.util.Logger;

public interface PowderMacroState {

   void onStart(PowderMacro macro);

   PowderMacroState onTick(PowderMacro macro);

   void onEnd(PowderMacro macro);

   default void log(String message) {
      Logger.sendLog("[" + this.getClass().getSimpleName() + "] " + message);
   }

   default void send(String message) {
      Logger.addMessage("[" + this.getClass().getSimpleName() + "] " + message);
   }

   default void logError(String message) {
      Logger.sendLog("[" + this.getClass().getSimpleName() + "] ERROR: " + message);
   }
}
