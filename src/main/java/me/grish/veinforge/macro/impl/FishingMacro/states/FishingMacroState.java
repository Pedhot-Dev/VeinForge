package me.grish.veinforge.macro.impl.FishingMacro.states;

import me.grish.veinforge.macro.impl.FishingMacro.FishingMacro;
import me.grish.veinforge.util.Logger;

public interface FishingMacroState {

    void onStart(FishingMacro macro);

    FishingMacroState onTick(FishingMacro macro);

    void onEnd(FishingMacro macro);

    default void log(String message) {
        Logger.sendLog("[" + this.getClass().getSimpleName() + "] " + message);
    }

    default void send(String message) {
        Logger.sendMessage("[" + this.getClass().getSimpleName() + "] " + message);
    }
}
