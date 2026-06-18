package me.grish.veinforge.failsafe.impl;

import lombok.Getter;
import me.grish.veinforge.failsafe.AbstractFailsafe;
import me.grish.veinforge.macro.MacroManager;
import me.grish.veinforge.util.InventoryUtil;
import me.grish.veinforge.util.Logger;
import me.grish.veinforge.util.SkyBlockItemIdUtil;
import me.grish.veinforge.util.helper.Clock;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ItemChangeFailsafe extends AbstractFailsafe {

    @Getter
    private static final ItemChangeFailsafe instance = new ItemChangeFailsafe();
    private final Clock timer = new Clock();
    private final Map<String, Integer> removedItems = new HashMap<>();

    @Override
    public String getName() {
        return "ItemChangeFailsafe";
    }

    @Override
    public Failsafe getFailsafeType() {
        return Failsafe.ITEM_CHANGE;
    }

    @Override
    public int getPriority() {
        return 5;
    }

    @Override
    public boolean onTick() {
        if (mc.gui.screen() instanceof AbstractContainerScreen) {
            if (!this.removedItems.isEmpty() || this.timer.isScheduled()) {
                resetStates();
            }
            return false;
        }

        if (this.removedItems.isEmpty()) {
            if (this.timer.isScheduled()) {
                this.timer.reset();
            }
            return false;
        }

        if (!this.timer.isScheduled()) {
            this.timer.schedule(2000);
        }

        if (this.timer.passed()) {
            for (Map.Entry<String, Integer> entry : removedItems.entrySet()) {
                warn("Necessary item with ID '" + entry.getKey() + "' is confirmed missing from slot " + entry.getValue() + " after timeout.");
            }
            return true;
        }

        return false;
    }


    @Override
    public boolean onPacketReceive(Packet<?> packet) {
        if (mc.gui.screen() instanceof AbstractContainerScreen) {
            if (!this.removedItems.isEmpty() || this.timer.isScheduled()) {
                resetStates();
            }
            return false;
        }

        if (!(packet instanceof ClientboundContainerSetSlotPacket p)) {
            return false;
        }

        int slot = p.getSlot();

        // Slots 1-44 are main player inventory
        if (slot <= 0 || slot >= 45) {
            return false;
        }

        Slot oldSlotObj = mc.player.inventoryMenu.getSlot(slot);
        ItemStack oldStackInSlot = oldSlotObj.hasItem() ? oldSlotObj.getItem() : null;
        ItemStack newStackFromPacket = p.getItem();

        String oldItemId = (oldStackInSlot != null) ? InventoryUtil.getItemId(oldStackInSlot) : "";
        String newItemId = (newStackFromPacket != null) ? InventoryUtil.getItemId(newStackFromPacket) : "";

        String oldStableId = (oldStackInSlot != null) ? SkyBlockItemIdUtil.getSkyBlockId(oldStackInSlot) : "";

        // Get and filter the list of necessary items
        List<String> necessaryDisplayNames = MacroManager.getInstance().getCurrentMacro().getNecessaryItems()
                .stream()
                .filter(name -> name != null && !name.isEmpty())
                .collect(Collectors.toList());

        // Check if the removed/replaced item was necessary
        if (oldStackInSlot != null && (newStackFromPacket == null || !oldItemId.equals(newItemId))) {
            String oldDisplayName = ChatFormatting.stripFormatting(oldStackInSlot.getHoverName().getString());
            boolean necessaryByStableId = !oldStableId.isEmpty() && necessaryDisplayNames.stream().anyMatch(it -> it.equalsIgnoreCase(oldStableId));
            boolean necessaryByName = oldDisplayName != null && necessaryDisplayNames.stream().anyMatch(oldDisplayName::contains);

            if (necessaryByStableId || necessaryByName) {
                removedItems.put(oldItemId, slot);
                log("Necessary item '" + oldDisplayName + "' (ID: " + oldItemId + ") was removed/replaced from slot " + slot);
            }
        }

        if (newStackFromPacket != null && (oldStackInSlot == null || !newItemId.equals(oldItemId))) {
            Integer originalSlotOfThisItem = removedItems.get(newItemId);

            if (originalSlotOfThisItem != null) {
                String newDisplayName = ChatFormatting.stripFormatting(newStackFromPacket.getHoverName().getString());

                if (!originalSlotOfThisItem.equals(slot)) {
                    // check if the original slot still contains item
                    Slot originalSlotObject = mc.player.inventoryMenu.getSlot(originalSlotOfThisItem);
                    if (originalSlotObject != null && originalSlotObject.hasItem()) {
                        String idInOriginalSlot = InventoryUtil.getItemId(originalSlotObject.getItem());

                        if (newItemId.equals(idInOriginalSlot)) {
                            log("An item with ID " + newItemId + " was detected in slot " + slot + ", but an identical item remains in the originally tracked slot " + originalSlotOfThisItem + ". This is not a move. Ignoring.");
                            removedItems.remove(newItemId);
                            return false;
                        }
                    }

                    log("Tracked necessary item '" + newDisplayName + "' (ID: " + newItemId + ") from original slot " + originalSlotOfThisItem + " now in slot " + slot + ".");
                    warn("Necessary item '" + newDisplayName + "' (ID: " + newItemId + ") was MOVED from slot " + originalSlotOfThisItem + " to slot " + slot + ". Triggering failsafe!");
                    return true;
                } else {
                    log("Tracked necessary item '" + newDisplayName + "' (ID: " + newItemId + ") has reappeared in its original slot " + originalSlotOfThisItem + ".");
                    removedItems.remove(newItemId);
                }
            }
        }
        return false;
    }

    @Override
    public boolean onChat(String message) {
        if (this.removedItems.isEmpty()) {
            return false;
        }

        if (message.equals("Oh no! Your Pickonimbus 2000 broke!")) {
            Integer removed = this.removedItems.remove("PICKONIMBUS");
            if (removed == null) {
                removed = this.removedItems.remove("PICKONIMBUS_2000");
            }
            if (removed != null) {
                error("Pickonimbus broke. Ignoring");
            }
        }
        return false;
    }

    @Override
    public boolean react() {
        MacroManager.getInstance().disable();
        Logger.sendWarning("Your item has been changed! Disabling macro.");
        return true;
    }

    @Override
    public void resetStates() {
        this.timer.reset();
        this.removedItems.clear();
    }
}
