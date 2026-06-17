package me.grish.veinforge.util;

import kotlin.Pair;
import me.grish.veinforge.VeinForge;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class InventoryUtil {

   private static final Minecraft mc = Minecraft.getInstance();

   public static boolean holdItem(String item) {
      int slot = getHotbarSlotOfItem(item);
      if (slot == -1) {
         return false;
      }
      if (mc.player != null) {
         mc.player.getInventory().setSelectedSlot(slot);
      }
      return true;
   }

   public static int getSlotIdOfItemInContainer(String item) {
      return getSlotIdOfItemInContainer(item, false);
   }

   public static int getSlotIdOfItemInContainer(String item, boolean equals) {
      Slot slot = getSlotOfItemInContainer(item, equals);
      return slot != null ? slot.index : -1;
   }

   public static Slot getSlotOfItemInContainer(String item) {
      return getSlotOfItemInContainer(item, false);
   }

   public static Slot getSlotOfItemInContainer(String item, boolean equals) {
      if (mc.player == null) return null;
      for (Slot slot : mc.player.containerMenu.slots) {
         if (slot.hasItem()) {
            String itemName = ChatFormatting.stripFormatting(slot.getItem().getHoverName().getString());
            if (itemName == null) continue;
            if (equals) {
               if (itemName.equalsIgnoreCase(item)) {
                  return slot;
               }
            } else {
               if (itemName.contains(item)) {
                  return slot;
               }
            }
         }
      }
      return null;
   }

   public static int getSlotIdFromItemId(String itemId) {
      if (mc.player == null) return -1;
      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (!stack.isEmpty()) {
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (id.equalsIgnoreCase(itemId) || id.endsWith(":" + itemId)) {
               return i;
            }
         }
      }
      return -1;
   }

   public static ItemStack getItemFromId(String itemId) {
      if (mc.player == null) return ItemStack.EMPTY;
      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (!stack.isEmpty()) {
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (id.equalsIgnoreCase(itemId) || id.endsWith(":" + itemId)) {
               return stack;
            }
         }
      }
      return ItemStack.EMPTY;
   }

   public static int getFirstEmptySlotId() {
      Slot slot = getFirstEmptySlot();
      return slot != null ? slot.index : -1;
   }

   public static Slot getFirstEmptySlot() {
      if (mc.player == null) return null;
      for (Slot slot : mc.player.containerMenu.slots) {
         if (!slot.hasItem()) {
            return slot;
         }
      }
      return null;
   }


   public static int getHotbarSlotOfItem(String items) {
      if (items.isEmpty() || mc.player == null) {
         return -1;
      }
      for (int i = 0; i < 9; i++) {
         ItemStack slot = mc.player.getInventory().getItem(i);
         if (slot.isEmpty()) {
            continue;
         }

         if (slot.getHoverName().getString().contains(items)) {
            return i;
         }
      }
      return -1;
   }

   public static List<String> getMissingItemsInInventory(Collection<String> requiredItems) {
      List<String> missingItems = new ArrayList<>(requiredItems);
      if (mc.player == null) return missingItems;

      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (!stack.isEmpty()) {
            String displayName = stack.getHoverName().getString();
            missingItems.removeIf(displayName::contains);
         }
      }

      return missingItems;
   }

   public static boolean areItemsInInventory(Collection<String> items) {
      return getMissingItemsInInventory(items).isEmpty();
   }

   public static List<String> getMissingItemsInHotbar(Collection<String> requiredItems) {
      List<String> missingItems = new ArrayList<>(requiredItems);
      // missingItems.forEach(System.out::println); // Removed debug print
      if (mc.player == null) return missingItems;

      for (int i = 0; i < 9; i++) { // Hotbar is 0-8 (9 slots)
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (!stack.isEmpty()) {
            String cleanName = ChatFormatting.stripFormatting(stack.getHoverName().getString());
            if (cleanName != null) {
               missingItems.removeIf(cleanName::contains);
            }
         }
      }
      return missingItems;
   }

   public static boolean areItemsInHotbar(Collection<String> items) {
      return getMissingItemsInHotbar(items).isEmpty();
   }

   public static Pair<List<Integer>, List<String>> getAvailableHotbarSlots(Collection<String> items) {
      List<String> itemsToMove = new ArrayList<>(items);
      List<Integer> slotsToMoveTo = new ArrayList<>();
      if (mc.player == null) return new Pair<>(slotsToMoveTo, itemsToMove);

      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);

         if (stack.isEmpty()) {
            slotsToMoveTo.add(i);
         } else if (!itemsToMove.removeIf(item -> stack.getHoverName().getString().contains(item))) {
            slotsToMoveTo.add(i);
         }

         if (itemsToMove.isEmpty()) {
            break;
         }
      }

      return new Pair<>(slotsToMoveTo, itemsToMove);
   }

   public static String getInventoryName(AbstractContainerMenu container) {
      if (mc.gui.screen() instanceof AbstractContainerScreen<?> screen) {
         if (screen.getMenu() == container) {
            return screen.getTitle().getString();
         }
      }
      return "";
   }

   public static String getInventoryName() {
      if (mc.player == null) return "";
      return getInventoryName(mc.player.containerMenu);
   }

   public static void clickContainerSlot(int slot, ClickType mouseButton, ClickMode mode) {
      clickContainerSlot(slot, mouseButton.ordinal(), mode.ordinal());
   }

   public static void clickContainerSlot(int slot, int mouseButton, ClickMode mode) {
      clickContainerSlot(slot, mouseButton, mode.ordinal());
   }

   public static void clickContainerSlot(int slot, int mouseButton, int clickMode) {
      if (mc.gameMode == null || mc.player == null || mc.gui.screen() == null) {
         return;
      }

      Integer containerId = resolveContainerId();
      if (containerId == null) {
         return;
      }

      net.minecraft.world.inventory.ContainerInput actionType = net.minecraft.world.inventory.ContainerInput.PICKUP;
      if (clickMode == 1) {
         actionType = net.minecraft.world.inventory.ContainerInput.QUICK_MOVE;
      } else if (clickMode == 2) {
         actionType = net.minecraft.world.inventory.ContainerInput.SWAP;
      }

      mc.gameMode.handleContainerInput(
              containerId,
              slot,
              mouseButton,
              actionType,
              mc.player
      );
   }

   public static void swapSlots(int slot, int hotbarSlot) {
      if (mc.gameMode == null || mc.player == null || mc.gui.screen() == null) {
         return;
      }

      Integer containerId = resolveContainerId();
      if (containerId == null) {
         return;
      }

      mc.gameMode.handleContainerInput(
              containerId,
              slot,
              hotbarSlot,
              ContainerInput.SWAP,
              mc.player
      );
   }

   private static Integer resolveContainerId() {
      if (mc.player == null || mc.gui.screen() == null) {
         return null;
      }

      if (mc.player.containerMenu instanceof ChestMenu) {
         // The `closeScreen()` method already uses `mc.gui.setScreen(null)`.
         return mc.player.containerMenu.containerId;
      }

      if (mc.gui.screen() instanceof InventoryScreen) {
         return mc.player.inventoryMenu.containerId;
      }

      if (mc.gui.screen() instanceof AbstractContainerScreen<?> screen) {
         return screen.getMenu().containerId;
      }

      return null;
   }

   public static void openInventory() {
      // KeyBinding.onTick(mc.gameSettings.keyBindInventory.getKeyCode());
      // In modern MC, we shouldn't manually tick keybinds like this usually.
      // But to simulate press:
      if (mc.options.keyInventory.isUnbound()) { // This is hard to simulate directly without Input util
         // Just open the screen if possible, but usually this opens player inventory
         // mc.gui.setScreen(new InventoryScreen(mc.player));
      }
      // TODO: Verify if we need to simulate key press or just open screen
   }

   public static void closeScreen() {
      if (mc.gui.screen() != null && mc.player != null) {
         // CRITICAL: Release all keys BEFORE closing container
         // This prevents key state desync between GUI open/close cycles
         KeyBindUtil.releaseAllExcept();

         // Get the container sync ID before closing
         int syncId = mc.player.containerMenu.containerId;

         // Close the container/screen on client side
         mc.gui.setScreen(null);

         // Tell the server to close the container
         // This prevents desync between client and server container states
         if (mc.getConnection() != null) {
            mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundContainerClosePacket(syncId));
         }

         // CRITICAL: Ensure keys stay released for 1 tick after GUI close
         // This prevents ghost key presses due to timing issues in modern Minecraft
         mc.execute(() -> {
            if (mc.gui.screen() == null) {
               KeyBindUtil.releaseAllExcept();
            }
         });
      }
   }

   public static List<String> getItemLoreFromOpenContainer(String name) {
      if (mc.player == null) return new ArrayList<>();
      AbstractContainerMenu openContainer = mc.player.containerMenu;
      for (Slot slot : openContainer.slots) {
         if (!slot.hasItem()) {
            continue;
         }
         ItemStack stack = slot.getItem();
         String itemName = ChatFormatting.stripFormatting(stack.getHoverName().getString());
         if (itemName == null || !itemName.contains(name)) {
            continue;
         }
         return getItemLore(stack);
      }
      return new ArrayList<>();
   }

   public static List<String> getItemLoreFromInventory(String name) {
      if (mc.player == null) return new ArrayList<>();
      AbstractContainerMenu container = mc.player.inventoryMenu;
      for (Slot slot : container.slots) {
         if (!slot.hasItem()) {
            continue;
         }
         ItemStack stack = slot.getItem();
         String itemName = ChatFormatting.stripFormatting(stack.getHoverName().getString());
         if (itemName == null || !itemName.contains(name)) {
            continue;
         }
         return getItemLore(stack);
      }
      return new ArrayList<>();
   }

   public static List<String> getItemLore(ItemStack itemStack) {
      List<String> loreList = new ArrayList<>();
      if (itemStack == null) return loreList;

      ItemLore lore = itemStack.get(DataComponents.LORE);
      if (lore != null) {
         for (Component text : lore.lines()) {
            String s = ChatFormatting.stripFormatting(text.getString());
            if (s != null) loreList.add(s);
         }
      }
      return loreList;
   }

   public static List<String> getLoreOfItemInContainer(int slotId) {
      if (slotId == -1 || mc.player == null) {
         return new ArrayList<>();
      }
      // Note: container.getSlot expects index, which might match slotId if it's from the same container
      if (slotId >= 0 && slotId < mc.player.containerMenu.slots.size()) {
         ItemStack itemStack = mc.player.containerMenu.getSlot(slotId).getItem();
         if (itemStack.isEmpty()) {
            return new ArrayList<>();
         }
         return getItemLore(itemStack);
      }
      return new ArrayList<>();
   }

   public static int getAmountOfItemInInventory(String item) {
      int amount = 0;
      if (mc.player == null) return 0;
      for (Slot slot : mc.player.inventoryMenu.slots) {
         if (slot.hasItem()) {
            String itemName = ChatFormatting.stripFormatting(slot.getItem().getHoverName().getString());
            if (itemName != null && itemName.equals(item)) {
               amount += slot.getItem().getCount();
            }
         }
      }
      return amount;
   }

   public static String getItemId(ItemStack stack) {
      if (stack.isEmpty()) {
         return "";
      }
      return SkyBlockItemIdUtil.getSkyBlockIdOrDisplayName(stack);
   }

   public static boolean isInventoryLoaded() {
      if (mc.player == null || mc.player.containerMenu == null) {
         return false;
      }
      if (!(mc.gui.screen() instanceof AbstractContainerScreen)) {
         return false;
      }
      // Logic to check if last slot is loaded (implies container items received)
      AbstractContainerMenu handler = mc.player.containerMenu;
      return !handler.slots.isEmpty();

      // This logic is a bit flaky in general but keeping semantics:
      // Check if last slot of the main inventory part (not player inventory) is present?
      // Original code checked lower chest inventory last slot.
      // We'll just check if the container has slots.
   }

   public static boolean isInventoryEmpty() {
      if (mc.player == null) return true;
      for (int i = 0; i < 36; i++) {
         if (!mc.player.getInventory().getItem(i).isEmpty()) {
            return false;
         }
      }
      return true;
   }

   public static String getFullName(String name) {
      if (mc.player == null) return "";
      for (Slot slot : mc.player.containerMenu.slots) {
         if (!slot.hasItem()) {
            continue;
         }
         String itemName = ChatFormatting.stripFormatting(slot.getItem().getHoverName().getString());
         if (itemName != null && itemName.toLowerCase().contains(name.toLowerCase())) {
            return itemName;
         }
      }
      return "";
   }

   public static int getDrillFuelCapacity(String drillName) {
      List<String> loreList = InventoryUtil.getItemLoreFromInventory(drillName);
      if (loreList.isEmpty()) {
         return -1;
      }
      for (String lore : loreList) {
         if (!lore.startsWith("Fuel: ")) {
            continue;
         }
         try {
            return Integer.parseInt(lore.split("/")[1].replace("k", "000"));
         } catch (Exception e) {
            Logger.sendNote("Could not retrieve fuel capacity. Lore: " + lore + ", Splitted: " + Arrays.toString(lore.split("/")));
            VeinForge.LOGGER.debug("Failed to parse drill fuel capacity from lore: {}", lore, e);
            break;
         }
      }
      return -1;
   }

   public static int getDrillRemainingFuel(String drillName) {
      List<String> loreList = InventoryUtil.getItemLoreFromInventory(drillName);
      if (loreList.isEmpty()) {
         return -1;
      }
      for (String lore : loreList) {
         if (!lore.startsWith("Fuel: ")) {
            continue;
         }
         try {
            return Integer.parseInt(lore.split(" ")[1].split("/")[0].replace(",", ""));
         } catch (Exception e) {
            Logger.sendNote("Could not retrieve fuel. Lore: " + lore + ", Splitted: " + Arrays.toString(lore.split("/")));
            VeinForge.LOGGER.debug("Failed to parse drill fuel from lore: {}", lore, e);
            break;
         }
      }
      return -1;

   }

   public enum ClickType {
      LEFT, RIGHT
   }

   public enum ClickMode {
      PICKUP,      // Ordinary left click
      QUICK_MOVE,  // Shift click
      SWAP
   }
}
