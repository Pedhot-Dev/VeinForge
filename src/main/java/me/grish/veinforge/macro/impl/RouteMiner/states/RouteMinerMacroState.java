package me.grish.veinforge.macro.impl.RouteMiner.states;

import me.grish.veinforge.macro.impl.RouteMiner.RouteMinerMacro;
import me.grish.veinforge.util.Logger;

/**
 * Interface representing the state of the RouteMiner Macro.
 * Each state defines its own behavior for starting, ticking, and ending.
 */
public interface RouteMinerMacroState {

   void onStart(RouteMinerMacro macro);

   RouteMinerMacroState onTick(RouteMinerMacro macro);

   void onEnd(RouteMinerMacro macro);

   default void log(String message) {
      System.out.println("[" + this.getClass().getSimpleName() + "] " + message);
   }

   default void logError(String message) {
      System.out.println("[" + this.getClass().getSimpleName() + "] ERROR: " + message);
   }

   default void send(String message) {
      Logger.addMessage("[" + this.getClass().getSimpleName() + "] " + message);
   }

}
