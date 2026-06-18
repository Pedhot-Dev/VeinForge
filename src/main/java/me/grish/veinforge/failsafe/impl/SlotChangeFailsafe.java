package me.grish.veinforge.failsafe.impl;

import lombok.Getter;
import me.grish.veinforge.failsafe.AbstractFailsafe;
import me.grish.veinforge.macro.MacroManager;
import me.grish.veinforge.util.Logger;
import me.grish.veinforge.util.helper.Clock;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;

public class SlotChangeFailsafe extends AbstractFailsafe {

    @Getter
    private static final SlotChangeFailsafe instance = new SlotChangeFailsafe();
    private final Minecraft mc = Minecraft.getInstance();
    private final Clock timer = new Clock();
    private int lastSelectedSlot;
    private boolean slotChanged = false;

    private SlotChangeFailsafe() {
        this.lastSelectedSlot = mc.player != null ? mc.player.getInventory().getSelectedSlot() : -1;
    }

    @Override
    public String getName() {
        return "SlotChangeFailsafe";
    }

    @Override
    public Failsafe getFailsafeType() {
        return Failsafe.SLOT_CHANGE;
    }

    @Override
    public int getPriority() {
        return 5;
    }

    @Override
    public boolean onTick() {
        if (slotChanged && timer.passed()) {
            Logger.sendLog("Timer passed after slot change");
            return true;
        }

        return false;
    }

    @Override
    public boolean onPacketReceive(Packet<?> packet) {
        if (packet instanceof ClientboundSetHeldSlotPacket(int slotIndex)) {

            if (slotIndex != lastSelectedSlot) {
                log("Slot changed by S09 packet from " + lastSelectedSlot + " to " + slotIndex);
                slotChanged = true;
                lastSelectedSlot = slotIndex;

                if (!timer.isScheduled()) {
                    timer.schedule(2000);
                }
            }
        }

        return false;
    }

    @Override
    public boolean react() {
        if (slotChanged) {
            MacroManager.getInstance().disable();
            warn("Slot selection changed! Disabling macro.");
            slotChanged = false;
            return true;
        }

        return false;
    }

    @Override
    public void resetStates() {
        timer.reset();
        slotChanged = false;
        log("SlotChangeFailsafe state reset.");
    }
}
