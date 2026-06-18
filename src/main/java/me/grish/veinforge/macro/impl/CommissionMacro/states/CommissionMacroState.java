package me.grish.veinforge.macro.impl.CommissionMacro.states;

import me.grish.veinforge.macro.impl.CommissionMacro.CommissionMacro;
import me.grish.veinforge.util.Logger;

public interface CommissionMacroState {

    void onStart(CommissionMacro macro);

    CommissionMacroState onTick(CommissionMacro macro);

    void onEnd(CommissionMacro macro);

    default void log(String message) {
        Logger.sendLog("[" + this.getClass().getSimpleName() + "] " + message);
    }

    default void logError(String message) {
        Logger.sendLog("[" + this.getClass().getSimpleName() + "] ERROR: " + message);
    }

    default void send(String message) {
        Logger.addMessage("[" + this.getClass().getSimpleName() + "] " + message);
    }

}
