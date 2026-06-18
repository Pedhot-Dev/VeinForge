package me.grish.veinforge.macro.impl.CommissionMacro;

import lombok.Getter;
import lombok.Setter;
import me.grish.veinforge.VeinForge;
import me.grish.veinforge.event.UpdateTablistEvent;
import me.grish.veinforge.failsafe.impl.NameMentionFailsafe;
import me.grish.veinforge.feature.FeatureManager;
import me.grish.veinforge.feature.impl.BlockMiner.BlockMiner;
import me.grish.veinforge.macro.AbstractMacro;
import me.grish.veinforge.macro.impl.CommissionMacro.states.*;
import me.grish.veinforge.ui.hud.elements.CommissionHUD;
import me.grish.veinforge.util.CommissionUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CommissionMacro extends AbstractMacro {
    private static final long PENDING_COMMISSION_VALIDATION_TIMEOUT_MS = 2_000L;
    private static final long CHAT_CLAIM_GUARD_TIMEOUT_MS = 20_000L;

    @Getter
    private static final CommissionMacro instance = new CommissionMacro();

    @Getter
    private CommissionMacroState currentState;

    @Getter
    private int actualCommissionCounter = 0;
    @Getter
    private long totalHotmXP = 0L;

    @Getter
    @Setter
    private int miningSpeed = 0;

    @Getter
    @Setter
    private BlockMiner.PickaxeAbility pickaxeAbility = BlockMiner.PickaxeAbility.NONE;

    @Getter
    @Setter
    private Commission currentCommission;
    @Getter
    private Commission pendingCommission;
    private long pendingCommissionDeadlineMs = 0L;
    private long chatClaimGuardUntilMs = 0L;

    @Getter
    private long lastCommissionCompleteAtMs = 0L;

    @Override
    public String getName() {
        return "Commission Macro";
    }

    @Override
    public void onEnable() {
        currentState = new StartingState();
        lastCommissionCompleteAtMs = 0L;
        clearPendingCommission();
        chatClaimGuardUntilMs = 0L;
        log("CommMacro::onEnable");
    }

    @Override
    public void onDisable() {
        if (currentState != null) {
            currentState.onEnd(this);
        }
        this.miningSpeed = 0;
        clearPendingCommission();
        chatClaimGuardUntilMs = 0L;
        if (CommissionHUD.getInstance().commHudResetStats) {
            this.actualCommissionCounter = 0;
            this.totalHotmXP = 0L;
        }
        log("CommMacro::onDisable");
        FeatureManager.getInstance().disableAll();
    }

    @Override
    public void onPause() {
        FeatureManager.getInstance().pauseAll();
        log("CommMacro::onPause");
    }

    @Override
    public void onResume() {
        FeatureManager.getInstance().resumeAll();
        log("CommMacro::onResume");
    }

    @Override
    public List<String> getNecessaryItems() {
        List<String> items = new ArrayList<>();
        items.add(VeinForge.config().general.miningTool);
        items.add(VeinForge.config().commission.dwarvenCommission.slayerWeapon);

        if (VeinForge.config().commission.dwarvenCommission.commClaimMethod == 1) {
            items.add("Royal Pigeon");
        }

        if (VeinForge.config().general.drillRefuel) {
            items.add("Abiphone");
        }
        return items;
    }

    public void onTick() {
        if (!this.isEnabled()) {
            return;
        }

        if (NameMentionFailsafe.getInstance().isLobbyChangeRequested()) {
            log("Name mention detected inside CommissionMacro onTick, changing lobbies");
            NameMentionFailsafe.getInstance().resetStates();
            transitionTo(new NewLobbyState());
        }

        if (this.isTimerRunning()) {
            return;
        }

        if (currentState == null)
            return;

        CommissionMacroState nextState = currentState.onTick(this);
        transitionTo(nextState);
    }


    private void transitionTo(CommissionMacroState nextState) {
        // Skip if no state change
        if (currentState == nextState)
            return;

        currentState.onEnd(this);
        currentState = nextState;

        if (currentState == null) {
            log("null state, returning");
            return;
        }

        currentState.onStart(this);
    }

    @Override
    public void onChat(String message) {
        if (!this.isEnabled() || message == null || currentState == null) {
            return;
        }

        if (!(currentState instanceof MiningState || currentState instanceof MobKillingState)) {
            return;
        }

        if (currentCommission == null || currentCommission == Commission.COMMISSION_CLAIM) {
            return;
        }

        if (!looksLikeSystemCommissionCompletionMessage(message)) {
            return;
        }

        log("Commission completion detected from chat. Triggering immediate claim flow.");
        clearPendingCommission();
        setCurrentCommission(Commission.COMMISSION_CLAIM);
        chatClaimGuardUntilMs = System.currentTimeMillis() + CHAT_CLAIM_GUARD_TIMEOUT_MS;
        if (!(currentState instanceof ClaimingCommissionState || currentState instanceof PathingState)) {
            transitionTo(new PathingState());
        }
    }

    public void recordCommissionClaim(long hotmXp) {
        this.actualCommissionCounter++;
        if (hotmXp > 0L) {
            this.totalHotmXP += hotmXp;
        }
        lastCommissionCompleteAtMs = System.currentTimeMillis();
    }

    @Override
    public void onTablistUpdate(UpdateTablistEvent event) {
        if (!this.isEnabled() || currentState instanceof WarpingState || currentState instanceof NewLobbyState) {
            return;
        }

        List<Commission> comms = CommissionUtil.getCurrentCommissionsFromTablist();
        if (comms.isEmpty()) {
            log("Cannot find commissions!");
            return;
        }

        Commission tablistCommission = comms.get(0);
        if (isChatClaimGuardActive() && tablistCommission != Commission.COMMISSION_CLAIM) {
            log("Ignoring non-claim tablist update during chat-triggered claim guard: " + tablistCommission.getName());
            return;
        }

        if (pendingCommission != null) {
            if (tablistCommission == Commission.COMMISSION_CLAIM) {
                return;
            }

            if (System.currentTimeMillis() > pendingCommissionDeadlineMs) {
                log("Pending commission validation timed out. Using tablist commission: " + tablistCommission.getName());
                clearPendingCommission();
                setCurrentCommission(tablistCommission);
                return;
            }

            if (tablistCommission == pendingCommission) {
                log("Tablist confirmed pending commission: " + tablistCommission.getName());
                clearPendingCommission();
                setCurrentCommission(tablistCommission);
                return;
            }

            log("Pending commission mismatch. GUI suggested " + pendingCommission.getName()
                    + ", tablist says " + tablistCommission.getName() + ". Switching to tablist target.");
            clearPendingCommission();
            setCurrentCommission(tablistCommission);
            return;
        }

        setCurrentCommission(tablistCommission);
    }

    private boolean looksLikeSystemCommissionCompletionMessage(String message) {
        if (message.contains(":")) {
            return false;
        }

        String upper = message.toUpperCase(Locale.ROOT);
        boolean hasCompletionKeyword = upper.contains("COMPLETE");
        boolean hasCommissionKeyword = upper.contains("COMMISSION");
        if (!(hasCompletionKeyword && hasCommissionKeyword)) {
            return false;
        }

        String currentName = currentCommission.getName().toUpperCase(Locale.ROOT);
        return upper.contains(currentName) || upper.contains("COMMISSION COMPLETED");
    }

    public void setPendingCommission(Commission commission) {
        if (commission == null) {
            clearPendingCommission();
            return;
        }

        pendingCommission = commission;
        pendingCommissionDeadlineMs = System.currentTimeMillis() + PENDING_COMMISSION_VALIDATION_TIMEOUT_MS;
        chatClaimGuardUntilMs = 0L;
        log("Pending commission set from GUI: " + commission.getName());
    }

    public void clearPendingCommission() {
        pendingCommission = null;
        pendingCommissionDeadlineMs = 0L;
    }

    private boolean isChatClaimGuardActive() {
        if (chatClaimGuardUntilMs <= 0L) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (now <= chatClaimGuardUntilMs) {
            return true;
        }

        chatClaimGuardUntilMs = 0L;
        return false;
    }
}
