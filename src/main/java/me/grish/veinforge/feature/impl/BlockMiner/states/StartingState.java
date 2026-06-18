package me.grish.veinforge.feature.impl.BlockMiner.states;

import me.grish.veinforge.VeinForge;
import me.grish.veinforge.feature.impl.BlockMiner.BlockMiner;

/**
 * StartingState
 * <p>
 * Initial state in the BlockMiner state machine.
 * Acts as an entry point and determines the next appropriate state.
 */
public class StartingState implements BlockMinerState {

    /**
     * Prevents rapid re-entry into {@link ApplyAbilityState} before the server chat packet
     * updates our pickaxe ability state.
     */
    private static final long ABILITY_RETRY_DELAY_MS = 2000;

    @Override
    public void onStart(BlockMiner miner) {
        log("Entering Starting State");
    }

    @Override
    public BlockMinerState onTick(BlockMiner miner) {
        return canUsePickaxeAbility(miner) ? new ApplyAbilityState() : new ChoosingBlockState();
    }

    private boolean canUsePickaxeAbility(BlockMiner miner) {
        long now = System.currentTimeMillis();

        if (now - miner.getLastAbilityUse() < ABILITY_RETRY_DELAY_MS) {
            return false;
        }

        if (miner.getPickaxeAbilityCooldownEndMs() > now) {
            miner.setPickaxeAbilityState(BlockMiner.PickaxeAbilityState.UNAVAILABLE);
            return false;
        }

        if (miner.getPickaxeAbilityCooldownEndMs() != 0L && miner.getPickaxeAbilityCooldownEndMs() <= now) {
            miner.setPickaxeAbilityCooldownEndMs(0L);
            miner.setPickaxeAbilityState(BlockMiner.PickaxeAbilityState.AVAILABLE);
        }

        if (now - miner.getLastAbilityUse() > 120000) {
            miner.setPickaxeAbilityState(BlockMiner.PickaxeAbilityState.AVAILABLE);
        }

        boolean hasAbility = miner.getPickaxeAbility() != BlockMiner.PickaxeAbility.NONE;
        boolean isAvailable = miner.getPickaxeAbilityState() == BlockMiner.PickaxeAbilityState.AVAILABLE;

        return VeinForge.config().general.usePickaxeAbility && hasAbility && isAvailable;
    }

    @Override
    public void onEnd(BlockMiner miner) {
        log("Exiting Starting State");
    }
}
