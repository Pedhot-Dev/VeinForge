package me.grish.veinforge.failsafe.impl;

import lombok.Getter;
import me.grish.veinforge.failsafe.AbstractFailsafe;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;

public class ProfileFailsafe extends AbstractFailsafe {

   @Getter
   private static final ProfileFailsafe instance = new ProfileFailsafe();

   private static final String TRIGGER_PHRASE = "Profile";

   @Override
   public String getName() {
      return "ProfileFailsafe";
   }

   @Override
   public Failsafe getFailsafeType() {
      return Failsafe.PLAYER_PROFILE_OPEN;
   }

   @Override
   public int getPriority() {
      return 2;
   }

   @Override
   public boolean onScreenOpen(Screen screen) {
      if (screen == null) {
         return false;
      }

      if (screen instanceof ContainerScreen guiChest) {
         String inventoryName = ChatFormatting.stripFormatting(guiChest.getTitle().getString());
         if (inventoryName != null && inventoryName.toLowerCase().contains(TRIGGER_PHRASE.toLowerCase())) {
            note("Detected inventory open with name containing " + inventoryName);
            return true;
         }
      }
      return false;
   }

   @Override
   public boolean react() {
      if (mc.screen != null && mc.player != null) {
         mc.execute(() -> {
            if (mc.screen != null && mc.player != null) {
               // Get the container sync ID before closing
               int syncId = mc.player.containerMenu.containerId;

               // Close the screen on client side
               mc.setScreen(null);

               // Tell the server to close the container
               // This prevents desync between client and server container states
               if (mc.getConnection() != null) {
                  mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundContainerClosePacket(syncId));
               }

               note("Closing the menu... continuing");
            }
         });
      } else {
         warn("Menu already closed... continuing");
      }
      return true;
   }
}