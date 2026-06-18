package me.grish.veinforge.macro.impl.CommissionMacro.states;

import me.grish.veinforge.VeinForge;
import me.grish.veinforge.feature.impl.AutoMobKiller.AutoMobKiller;
import me.grish.veinforge.macro.impl.CommissionMacro.Commission;
import me.grish.veinforge.macro.impl.CommissionMacro.CommissionMacro;
import me.grish.veinforge.util.CommissionUtil;

import java.util.Set;

public class MobKillingState implements CommissionMacroState {

    @Override
    public void onStart(CommissionMacro macro) {
        log("Starting mob killing state");
        Set<String> mobName = CommissionUtil.getMobForCommission(macro.getCurrentCommission());

        if (mobName == null) {
            logError("Current commission: " + macro.getCurrentCommission());
            macro.disable("Mob name not found! Please send the logs to the developer ");
            return;
        }

        AutoMobKiller.getInstance().start(mobName, macro.getCurrentCommission().getName().startsWith("Glacite") ?
                        VeinForge.config().general.miningTool : VeinForge.config().commission.dwarvenCommission.slayerWeapon,
                getSlayerProfile(macro.getCurrentCommission()));
    }

    @Override
    public CommissionMacroState onTick(CommissionMacro macro) {
        if (macro.getCurrentCommission() == Commission.COMMISSION_CLAIM) {
            return new PathingState();
        }

        if (AutoMobKiller.getInstance().isRunning()) {
            return this;
        }

        switch (AutoMobKiller.getInstance().getError()) {
            case NONE:
                log("Mob killer stopped unexpectedly. Restarting mob killer loop.");
                return new StartingState();
            case NO_ENTITIES:
                log("No entities found in Mob Killer after stopping. Restarting commission loop.");
                return new StartingState();
        }

        return null;
    }

    @Override
    public void onEnd(CommissionMacro macro) {
        AutoMobKiller.getInstance().stop();
        log("Ending mob killing state");
    }

    private AutoMobKiller.SlayerProfile getSlayerProfile(Commission commission) {
        if (commission == null) {
            return AutoMobKiller.SlayerProfile.GENERIC;
        }

        switch (commission) {
            case GOBLIN_SLAYER:
            case MINES_SLAYER:
                return AutoMobKiller.SlayerProfile.GOBLIN;
            case GLACITE_WALKER_SLAYER:
                return AutoMobKiller.SlayerProfile.GLACITE;
            default:
                return AutoMobKiller.SlayerProfile.GENERIC;
        }
    }
}
