package me.grish.veinforge.macro.impl.CommissionMacro.states;

import me.grish.veinforge.VeinForge;
import me.grish.veinforge.feature.impl.AutoGetStats.AutoGetStats;
import me.grish.veinforge.feature.impl.AutoGetStats.tasks.impl.MiningSpeedRetrievalTask;
import me.grish.veinforge.feature.impl.AutoGetStats.tasks.impl.PickaxeAbilityRetrievalTask;
import me.grish.veinforge.feature.impl.BlockMiner.BlockMiner;
import me.grish.veinforge.macro.impl.CommissionMacro.CommissionMacro;

public class GettingStatsState implements CommissionMacroState {

    private final AutoGetStats autoInventory = AutoGetStats.getInstance();
    private MiningSpeedRetrievalTask miningSpeedRetrievalTask;
    private PickaxeAbilityRetrievalTask pickaxeAbilityRetrievalTask;

    @Override
    public void onStart(CommissionMacro macro) {
        log("Entering getting stats state");
        miningSpeedRetrievalTask = new MiningSpeedRetrievalTask();
        pickaxeAbilityRetrievalTask = new PickaxeAbilityRetrievalTask();
        AutoGetStats.getInstance().startTask(miningSpeedRetrievalTask);
        AutoGetStats.getInstance().startTask(pickaxeAbilityRetrievalTask);
    }

    @Override
    public CommissionMacroState onTick(CommissionMacro macro) {
        if (!AutoGetStats.getInstance().hasFinishedAllTasks())
            return this;

        if (miningSpeedRetrievalTask.getError() != null) {
            macro.disable("Failed to get stats with the following error: " + miningSpeedRetrievalTask.getError());
            return null;
        }

        if (pickaxeAbilityRetrievalTask.getError() != null) {
            macro.disable("Failed to get pickaxe ability with the following error: " + pickaxeAbilityRetrievalTask.getError());
            return null;
        }

        macro.setMiningSpeed(miningSpeedRetrievalTask.getResult());
        macro.setPickaxeAbility(VeinForge.config().general.usePickaxeAbility ? pickaxeAbilityRetrievalTask.getResult() : BlockMiner.PickaxeAbility.NONE);
        return new StartingState();
    }

    @Override
    public void onEnd(CommissionMacro macro) {
        autoInventory.stop();
        log("Exiting getting stats state");
    }
}
