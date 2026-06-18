package me.grish.veinforge.macro.impl.FishingMacro;

import lombok.Getter;
import lombok.Setter;
import me.grish.veinforge.VeinForge;
import me.grish.veinforge.failsafe.FailsafeManager;
import me.grish.veinforge.failsafe.impl.NameMentionFailsafe;
import me.grish.veinforge.feature.FeatureManager;
import me.grish.veinforge.handler.GameStateHandler;
import me.grish.veinforge.handler.GraphHandler;
import me.grish.veinforge.macro.AbstractMacro;
import me.grish.veinforge.macro.MacroManager;
import me.grish.veinforge.macro.impl.FishingMacro.states.FishingMacroState;
import me.grish.veinforge.macro.impl.FishingMacro.states.WarpingState;
import me.grish.veinforge.util.KeyBindUtil;
import me.grish.veinforge.util.helper.location.Location;
import me.grish.veinforge.util.helper.location.SubLocation;

import java.util.ArrayList;
import java.util.List;

public class FishingMacro extends AbstractMacro {
    private static final long SAFETY_WARNING_COOLDOWN_MS = 5_000L;

    @Getter
    private static final FishingMacro instance = new FishingMacro();
    private final List<String> necessaryItems = new ArrayList<>();
    @Getter
    @Setter
    private FishingMacroState currentState;
    private long nextConfigWarningAtMs = 0L;
    private long nextSafetyWarningAtMs = 0L;

    @Override
    public String getName() {
        return "Galatea Macro";
    }

    @Override
    public void onEnable() {
        this.necessaryItems.clear();
        this.nextConfigWarningAtMs = 0L;
        this.nextSafetyWarningAtMs = 0L;
        this.timer.reset();
        KeyBindUtil.releaseAllExcept();
        ensureGraphReady();
        this.currentState = new WarpingState();
        this.currentState.onStart(this);
        log(getName() + " enabled");
    }

    @Override
    public void onDisable() {
        if (this.currentState != null) {
            this.currentState.onEnd(this);
        }
        this.currentState = null;
        this.timer.reset();
        KeyBindUtil.releaseAllExcept();
        log(getName() + " disabled");
    }

    @Override
    public void onPause() {
        FeatureManager.getInstance().pauseAll();
        KeyBindUtil.releaseAllExcept();
        log(getName() + " paused");
    }

    @Override
    public void onResume() {
        FeatureManager.getInstance().resumeAll();
        log(getName() + " resumed");
    }

    @Override
    public List<String> getNecessaryItems() {
        this.necessaryItems.clear();
        String rodName = VeinForge.config().fishing.generalFishing.fishingRod;
        if (rodName != null && !rodName.trim().isEmpty()) {
            this.necessaryItems.add(rodName);
        }

        String axeName = VeinForge.config().fishing.galateaFishing.galateaAxe;
        if (axeName != null && !axeName.trim().isEmpty()) {
            this.necessaryItems.add(axeName);
        }

        String weaponName = VeinForge.config().fishing.galateaFishing.galateaFishingWeapon;
        if (usesSlayerWeaponMode() && weaponName != null && !weaponName.trim().isEmpty()) {
            this.necessaryItems.add(weaponName);
        }
        return this.necessaryItems;
    }

    @Override
    public void onTick() {
        if (!this.isEnabled() || this.isTimerRunning() || this.currentState == null) {
            return;
        }

        if (handleNameMentionLobbyChangeRequest()) {
            return;
        }

        if (isFailsafePendingOrActive()) {
            KeyBindUtil.releaseAllExcept();
            return;
        }

        if (!validateConfigForCurrentMode()) {
            return;
        }

        FishingMacroState nextState = this.currentState.onTick(this);
        transitionTo(nextState);
    }

    @Override
    public void onChat(String message) {
        if (!this.isEnabled() || message == null || message.isEmpty()) {
        }
    }

    public boolean isInGalatea() {
        GameStateHandler handler = GameStateHandler.getInstance();
        Location location = handler.getCurrentLocation();
        SubLocation subLocation = handler.getCurrentSubLocation();

        return location == Location.GALATEA
                || location == Location.BACKWATER_BAYOU
                || subLocation == SubLocation.GALATEA
                || subLocation == SubLocation.BACKWATER_BAYOU;
    }

    private void transitionTo(FishingMacroState nextState) {
        if (this.currentState == nextState || nextState == null) {
            return;
        }

        this.currentState.onEnd(this);
        this.currentState = nextState;
        this.currentState.onStart(this);
    }

    public boolean usesSlayerWeaponMode() {
        return VeinForge.config().fishing.galateaFishing.galateaKillMode == 1;
    }

    private boolean validateConfigForCurrentMode() {
        String rodName = VeinForge.config().fishing.generalFishing.fishingRod;
        if (rodName == null || rodName.trim().isEmpty()) {
            warnConfig("Set Fishing Rod in Galatea Macro config first.");
            return false;
        }

        String axeName = VeinForge.config().fishing.galateaFishing.galateaAxe;
        if (axeName == null || axeName.trim().isEmpty()) {
            warnConfig("Set Galatea Axe in Galatea Macro config first.");
            return false;
        }

        if (usesSlayerWeaponMode()) {
            String weaponName = VeinForge.config().fishing.galateaFishing.galateaFishingWeapon;
            if (weaponName == null || weaponName.trim().isEmpty()) {
                warnConfig("Kill Mode is Slayer Weapon, but no secondary weapon is configured.");
                return false;
            }
        }

        return true;
    }

    private void warnConfig(String message) {
        long now = System.currentTimeMillis();
        if (now < nextConfigWarningAtMs) {
            return;
        }
        nextConfigWarningAtMs = now + 5_000L;
        warn(message);
    }

    private void ensureGraphReady() {
        if (!VeinForge.config().fishing.generalFishing.useGraph) {
            return;
        }

        String graphName = VeinForge.config().fishing.getGraphName();
        boolean created = false;
        if (!GraphHandler.instance.hasGraph(graphName)) {
            if (GraphHandler.instance.createGraph(graphName)) {
                created = true;
            } else {
                warn("Fishing graph was missing and could not be created: " + graphName);
            }
        }
        GraphHandler.instance.switchGraph(graphName);
        if (created) {
            GraphHandler.instance.saveNow();
            send("Created missing fishing graph: " + graphName + ". Use /graph edit " + graphName);
        }
    }

    private boolean handleNameMentionLobbyChangeRequest() {
        if (!NameMentionFailsafe.getInstance().isLobbyChangeRequested()) {
            return false;
        }

        NameMentionFailsafe.getInstance().resetStates();
        attemptLobbyEscape();
        triggerSafetyStop("Safety stop: name mention lobby-change request received.");
        return true;
    }

    private boolean isFailsafePendingOrActive() {
        FailsafeManager failsafeManager = FailsafeManager.getInstance();
        return failsafeManager.triggeredFailsafe.isPresent() || !failsafeManager.emergencyQueue.isEmpty();
    }

    private void attemptLobbyEscape() {
        if (mc.player == null || mc.player.connection == null) {
            return;
        }
        mc.player.connection.sendCommand("l");
        warnSafety("Executing emergency /l before disabling macro.");
    }

    private void triggerSafetyStop(String reason) {
        warnSafety(reason);
        KeyBindUtil.releaseAllExcept();
        MacroManager.getInstance().disable();
    }

    private void warnSafety(String message) {
        long now = System.currentTimeMillis();
        if (now < nextSafetyWarningAtMs) {
            return;
        }
        nextSafetyWarningAtMs = now + SAFETY_WARNING_COOLDOWN_MS;
        warn(message);
    }
}
