package me.grish.veinforge.feature.impl;

import lombok.Getter;
import me.grish.veinforge.VeinForge;
import me.grish.veinforge.feature.AbstractFeature;
import me.grish.veinforge.handler.RotationHandler;
import me.grish.veinforge.macro.impl.CommissionMacro.Commission;
import me.grish.veinforge.macro.impl.CommissionMacro.CommissionMacro;
import me.grish.veinforge.util.CommissionUtil;
import me.grish.veinforge.util.InventoryUtil;
import me.grish.veinforge.util.InventoryUtil.ClickMode;
import me.grish.veinforge.util.InventoryUtil.ClickType;
import me.grish.veinforge.util.KeyBindUtil;
import me.grish.veinforge.util.helper.RotationConfiguration;
import me.grish.veinforge.util.helper.RotationConfiguration.RotationType;
import me.grish.veinforge.util.helper.Target;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class AutoCommissionClaim extends AbstractFeature {

    private static final int PIGEON_PREPARE_DELAY_MS = 120;
    private static final int ROTATION_SETTLE_DELAY_MS = 120;
    private static final int SWAP_TO_OPEN_DELAY_MS = 150;
    private static final int GUI_OPEN_TIMEOUT_MS = 3500;
    private static final int GUI_RETRY_DELAY_MS = 150;
    private static final int CLAIM_FIRST_CLICK_DELAY_MS = 120;
    private static final int CLAIM_LOOP_DELAY_BASE_MS = 110;
    private static final int CLAIM_LOOP_DELAY_RANDOM_MS = 70;
    private static final int CLAIM_EMPTY_CONFIRM_LOOPS = 4;
    private static final int NEXT_COMMISSION_READ_DELAY_MS = 150;
    private static final int SWAP_BACK_DELAY_MS = 150;
    private static final int PIGEON_COOLDOWN_RETRY_DELAY_MS = 3500;
    private static final int NPC_ROTATION_TIME_MS = 350;

    private static AutoCommissionClaim instance;
    private final Set<String> countedClaimSignatures = new HashSet<>();
    private State state = State.STARTING;
    private ClaimError claimError = ClaimError.NONE;
    private Optional<Player> emissary = Optional.empty();
    @Getter
    private List<Commission> nextComm = new ArrayList<>();
    private int emptyClaimChecks = 0;
    private int retry = 0;
    private int commClaimMethod = 0;

    public static AutoCommissionClaim getInstance() {
        if (instance == null) {
            instance = new AutoCommissionClaim();
        }

        return instance;
    }

    @Override
    public String getName() {
        return "AutoCommissionClaim";
    }

    @Override
    public void start() {
        commClaimMethod = VeinForge.config().commission.dwarvenCommission.commClaimMethod;
        if (VeinForge.config().general.macroType == 1) {
            commClaimMethod = 1;        // Always use Royal Pigeon for Glacial Macro
        }
        this.enabled = true;
        this.nextComm = null;
        this.claimError = ClaimError.NONE;
        this.countedClaimSignatures.clear();
        this.emptyClaimChecks = 0;
    }

    @Override
    public void stop() {
        if (!this.enabled) {
            return;
        }

        this.enabled = false;
        this.emissary = Optional.empty();
        this.timer.reset();
        this.resetStatesAfterStop();
        send("AutoCommissionClaim Stopped");
    }

    @Override
    public void resetStatesAfterStop() {
        this.state = State.STARTING;
        this.retry = 0;
        this.countedClaimSignatures.clear();
        this.emptyClaimChecks = 0;
    }

    @Override
    public boolean shouldNotCheckForFailsafe() {
        return true;
    }

    public void stop(ClaimError error) {
        this.claimError = error;
        this.stop();
    }

    public boolean succeeded() {
        return !this.enabled && this.claimError == ClaimError.NONE;
    }

    public ClaimError claimError() {
        return this.claimError;
    }

    @Override
    protected void onTick() {
        if (!this.enabled) {
            return;
        }

        if (this.retry > 3) {
            log("Tried too many times but failed. stopping");
            this.stop(ClaimError.INACCESSIBLE_NPC);
            return;
        }

        switch (this.state) {
            case STARTING:
                int time = PIGEON_PREPARE_DELAY_MS;
                switch (commClaimMethod) {
                    case 0:
                        time = 0;
                        break;
                    case 1:
                        if (!InventoryUtil.holdItem("Royal Pigeon")) {
                            this.stop(ClaimError.NO_ITEMS);
                            break;
                        }
                        break;
                }
                this.swapState(State.ROTATING, time);
                break;
            case ROTATING:
                if (this.isTimerRunning()) {
                    return;
                }
                if (commClaimMethod == 0) {
                    this.emissary = CommissionUtil.getClosestEmissary();

                    if (this.emissary.isPresent()) {
                        Player emissaryEntity = this.emissary.get();
                        log("Found Emissary: " + emissaryEntity.getName().getString());

                        if (mc.player.distanceToSqr(emissaryEntity) > 16) {
                            log("Emissary " + emissaryEntity.getName().getString() + " is too far away.");
                            sendError("Emissary is too far away.");
                            this.stop(ClaimError.INACCESSIBLE_NPC);
                            return;
                        } else {
                            log("Rotating to Emissary: " + emissaryEntity.getName().getString());
                            RotationHandler.getInstance().easeTo(new RotationConfiguration(new Target(emissaryEntity), NPC_ROTATION_TIME_MS, RotationType.CLIENT, null));
                        }
                    } else {
                        log("Could not find nearby Emissary. Current position: " + mc.player.position());
                        this.stop(ClaimError.NPC_NOT_UNLOCKED);
                        return;
                    }
                }
                this.swapState(State.SWAPPING_TO_ALT, ROTATION_SETTLE_DELAY_MS);
                break;

            case SWAPPING_TO_ALT:
                if (this.isTimerRunning()) {
                    return;
                }

                if (VeinForge.config().commission.dwarvenCommission.commSwapBeforeClaiming) {
                    if (!InventoryUtil.holdItem(VeinForge.config().commission.dwarvenCommission.altMiningTool)) {
                        this.stop(ClaimError.NO_ITEMS);
                        sendError("Cannot hold Alt Mining Tool: " + VeinForge.config().commission.dwarvenCommission.altMiningTool);
                        break;
                    }
                    this.swapState(State.OPENING, 0);
                    break;
                }
                this.swapState(State.OPENING, SWAP_TO_OPEN_DELAY_MS);

                break;
            case OPENING:
                time = GUI_OPEN_TIMEOUT_MS;
                switch (commClaimMethod) {
                    case 0:
                        if (RotationHandler.getInstance().isEnabled()) {
                            return;
                        }
                        if (!this.emissary.isPresent()) {
                            this.stop(ClaimError.NPC_NOT_UNLOCKED);
                            return;
                        }
                        Entity emissaryEntity = this.emissary.get();
                        if (mc.player.distanceToSqr(emissaryEntity) > 16) {
                            this.stop(ClaimError.INACCESSIBLE_NPC);
                            return;
                        }

                        // Always attempt direct interaction with the selected emissary.
                        // Relying on hitResult/entityLookingAt can stall if crosshair state is stale.
                        if (mc.gameMode != null) {
                            mc.gameMode.interact(mc.player, emissaryEntity, new net.minecraft.world.phys.EntityHitResult(emissaryEntity), net.minecraft.world.InteractionHand.MAIN_HAND);
                        } else {
                            KeyBindUtil.rightClick();
                        }
                        break;
                    case 1:
                        KeyBindUtil.resetRightClickDelayTimer();
                        KeyBindUtil.rightClick();
                }

                log("Scheduler timer for : " + time);
                this.swapState(State.VERIFYING_GUI, time);
                break;
            case VERIFYING_GUI:
                if (this.hasTimerEnded()) {
                    if (this.retry < 3) {
                        this.retry++;
                        log("Commission GUI did not open in time. Retrying interaction attempt " + this.retry + "/3");
                        this.swapState(State.ROTATING, GUI_RETRY_DELAY_MS);
                    } else {
                        this.stop(ClaimError.INACCESSIBLE_NPC);
                        sendError("Opened a Different Inventory Named: " + InventoryUtil.getInventoryName());
                    }
                    break;
                }
                switch (commClaimMethod) {
                    case 0:
                    case 1:
                        if (!(mc.player.containerMenu instanceof ChestMenu) || !InventoryUtil.getInventoryName().contains("Commissions")) {
                            break;
                        }
                        this.retry = 0;
                        this.emptyClaimChecks = 0;
                        this.swapState(State.CLAIMING, CLAIM_FIRST_CLICK_DELAY_MS);
                        break;
                }
                break;
            case CLAIMING:
                if (this.isTimerRunning()) {
                    break;
                }
                final int slotToClick = CommissionUtil.getClaimableCommissionSlot();
                State nextState;
                if (slotToClick != -1) {
                    emptyClaimChecks = 0;
                    String claimSignature = buildClaimSignature(slotToClick);
                    if (countedClaimSignatures.add(claimSignature)) {
                        long hotmXp = CommissionUtil.getClaimableCommissionHotmXp(slotToClick);
                        CommissionMacro macro = CommissionMacro.getInstance();
                        if (macro.isEnabled()) {
                            macro.recordCommissionClaim(hotmXp);
                        }
                    } else {
                        log("Ignoring duplicate claim stats update for signature: " + claimSignature);
                    }
                    InventoryUtil.clickContainerSlot(slotToClick, ClickType.LEFT, ClickMode.PICKUP);
                    nextState = State.CLAIMING;
                } else {
                    emptyClaimChecks++;
                    if (emptyClaimChecks >= CLAIM_EMPTY_CONFIRM_LOOPS) {
                        log("No Commission To Claim after " + emptyClaimChecks + " checks.");
                        nextState = State.NEXT_COMM;
                    } else {
                        nextState = State.CLAIMING;
                    }
                }
                this.swapState(nextState, getFastClaimLoopDelay());
                break;
            case NEXT_COMM:
                if (this.isTimerRunning()) {
                    break;
                }


                if (mc.player.containerMenu instanceof ChestMenu) {
                    this.nextComm = CommissionUtil.getCommissionFromContainer((ChestMenu) mc.player.containerMenu);
                }
                this.swapState(State.ENDING, NEXT_COMMISSION_READ_DELAY_MS);
                break;

            case ENDING:
                if (this.isTimerRunning()) {
                    return;
                }
                InventoryUtil.closeScreen();

                if (VeinForge.config().commission.dwarvenCommission.commSwapBeforeClaiming) {
                    this.swapState(State.SWAPPING_BACK, SWAP_BACK_DELAY_MS);
                    break;
                }
                this.stop();
                break;
            case SWAPPING_BACK:
                if (this.isTimerRunning()) {
                    return;
                }
                if (VeinForge.config().commission.dwarvenCommission.commSwapBeforeClaiming) {
                    if (!InventoryUtil.holdItem(VeinForge.config().general.miningTool)) {
                        this.stop(ClaimError.NO_ITEMS);
                        sendError("Cannot hold Mining Tool: " + VeinForge.config().general.miningTool);
                        break;
                    }
                }
                this.stop();
                break;
        }
    }

    @Override
    protected void onChat(String message) {
        if (!this.enabled || this.state != State.CLAIMING) {
            return;
        }

        if (message.startsWith("This ability is on cooldown for ")) {
            this.retry++;
            log("Pigeon Cooldown Detected, Waiting for " + (PIGEON_COOLDOWN_RETRY_DELAY_MS / 1000.0) + " Seconds");
            this.swapState(State.OPENING, PIGEON_COOLDOWN_RETRY_DELAY_MS);
        }
    }

    private void swapState(final State state, final int time) {
        this.state = state;
        if (time == 0) {
            this.timer.reset();
        } else {
            this.timer.schedule(time);
        }
    }

    private int getFastClaimLoopDelay() {
        return CLAIM_LOOP_DELAY_BASE_MS + (int) (Math.random() * CLAIM_LOOP_DELAY_RANDOM_MS);
    }

    private String buildClaimSignature(int slot) {
        if (!(mc.player.containerMenu instanceof ChestMenu chest)) {
            return "slot=" + slot;
        }

        ItemStack stack = chest.getSlot(slot).getItem();
        if (stack == null || stack.isEmpty()) {
            return "slot=" + slot;
        }

        StringBuilder signature = new StringBuilder("slot=").append(slot);
        String name = sanitizeLoreLine(stack.getHoverName().getString());
        if (!name.isEmpty()) {
            signature.append("|name=").append(name);
        }

        List<String> lore = InventoryUtil.getItemLore(stack);
        if (lore != null) {
            for (String line : lore) {
                String cleaned = sanitizeLoreLine(line);
                if (!cleaned.isEmpty()) {
                    signature.append("|lore=").append(cleaned);
                }
            }
        }

        return signature.toString();
    }

    private String sanitizeLoreLine(String line) {
        if (line == null) {
            return "";
        }
        String stripped = ChatFormatting.stripFormatting(line);
        if (stripped == null) {
            return "";
        }
        return stripped.replace('\u00A0', ' ').trim();
    }

    enum State {
        STARTING, ROTATING, SWAPPING_TO_ALT, OPENING, VERIFYING_GUI, CLAIMING, NEXT_COMM, SWAPPING_BACK, ENDING
    }

    public enum ClaimError {
        NONE, INACCESSIBLE_NPC, NO_ITEMS, TIMEOUT, NPC_NOT_UNLOCKED
    }
}
