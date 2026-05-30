package me.grish.veinforge.util;

import me.grish.veinforge.VeinForge;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public class UseItemAbility {

   private static final int HOTBAR_SIZE = 9;
   private static final int MIN_SWAP_BACK_DELAY_MS = 150;
   private static final int MAX_SWAP_BACK_DELAY_MS = 400;
   private static final Minecraft mc = Minecraft.getInstance();

   public static boolean useItemAbility(String itemName, int hotbarSlot) {
      int swapBackDelayMs = java.util.concurrent.ThreadLocalRandom.current()
                                    .nextInt(MIN_SWAP_BACK_DELAY_MS, MAX_SWAP_BACK_DELAY_MS + 1);
      return useItemAbility(itemName, hotbarSlot, swapBackDelayMs);
   }

   public static boolean useItemAbility(
           String itemName,
           int hotbarSlot,
           int swapBackDelayMs
   ) {
      if (mc.player == null || itemName == null || itemName.trim().isEmpty()) {
         return false;
      }

      if (hotbarSlot < 0 || hotbarSlot >= HOTBAR_SIZE) {
         return false;
      }

      ItemStack stack = mc.player.getInventory().getItem(hotbarSlot);
      if (stack == null || stack.isEmpty()) {
         return false;
      }

      String displayName = ChatFormatting.stripFormatting(stack.getHoverName().getString());
      if (displayName == null || !displayName.toLowerCase().contains(itemName.toLowerCase())) {
         return false;
      }

      int previousSlot = mc.player.getInventory().getSelectedSlot();
      mc.player.getInventory().setSelectedSlot(hotbarSlot);
      KeyBindUtil.resetRightClickDelayTimer();
      KeyBindUtil.rightClick();

      if (previousSlot == hotbarSlot) {
         return true;
      }

      if (swapBackDelayMs <= 0) {
         mc.player.getInventory().setSelectedSlot(previousSlot);
         return true;
      }

      VeinForge.executor().execute(() -> {
         try {
            Thread.sleep(swapBackDelayMs);
         } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
         }

         mc.execute(() -> {
            if (mc.player != null) {
               mc.player.getInventory().setSelectedSlot(previousSlot);
            }
         });
      });

      return true;
   }
}
