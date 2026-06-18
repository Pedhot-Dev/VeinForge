package me.grish.veinforge.macro.impl.PowderMacro.states;

import me.grish.veinforge.feature.impl.BlockMiner.BlockMiner;
import me.grish.veinforge.macro.impl.PowderMacro.PowderMacro;
import net.minecraft.core.BlockPos;

import java.util.Optional;

public class GemstoneState implements PowderMacroState {
    @Override
    public void onStart(PowderMacro macro) {
        log("Entered Gemstone powder mode.");
    }

    @Override
    public PowderMacroState onTick(PowderMacro macro) {
        if (!macro.validateEnvironment()) {
            return null;
        }

        BlockMiner.BlockMinerError error = macro.getMinerError();
        switch (error) {
            case NONE:
                break;
            case NO_POINTS_FOUND:
                logError("Miner could not find a valid break point. Restarting miner.");
                macro.restartMiner();
                return this;
            case NOT_ENOUGH_BLOCKS:
                macro.disable("No mineable hardstone found nearby for Powder Macro.");
                return null;
            case NO_TOOLS_AVAILABLE:
                macro.disable("Mining tool was not found in hotbar.");
                return null;
            case NO_TARGET_BLOCKS:
                macro.disable("Powder Macro has no target blocks configured.");
                return null;
            case NO_PICKAXE_ABILITY:
                macro.disable("Could not determine pickaxe ability for Block Miner.");
                return null;
        }

        if (!macro.isMinerRunning()) {
            macro.ensureMinerRunning();
            return this;
        }

        Optional<BlockPos> chest = macro.findNearbyChest();
        if (chest.isPresent()) {
            log("Found nearby powder chest at " + chest.get() + ". Switching to treasure state.");
            return new TreasureState(chest.get());
        }

        return this;
    }

    @Override
    public void onEnd(PowderMacro macro) {
        log("Exiting Gemstone powder mode.");
    }
}
