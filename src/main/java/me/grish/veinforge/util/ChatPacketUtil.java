package me.grish.veinforge.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;

/**
 * Extracts displayable text from inbound chat-related packets.
 */
public final class ChatPacketUtil {

   private ChatPacketUtil() {
   }

   /**
    * @return a best-effort plain string, or null if not a chat packet.
    */
   public static String extractMessage(Packet<?> packet) {
      if (packet == null) {
         return null;
      }

      if (packet instanceof ClientboundSystemChatPacket messagePacket) {
         // GameMessageS2CPacket is used for both chat (overlay=false) and action bar (overlay=true).
         return strip(messagePacket.content().getString());
      }

      if (packet instanceof ClientboundPlayerChatPacket chatPacket) {
         // Player chat. Body content is unformatted text.
         return strip(chatPacket.body().content());
      }

      if (packet instanceof ClientboundSetActionBarTextPacket overlayPacket) {
         // Action bar overlay.
         return strip(overlayPacket.text().getString());
      }

      return null;
   }

   private static String strip(String s) {
      if (s == null) {
         return null;
      }
      String stripped = ChatFormatting.stripFormatting(s);
      if (stripped == null) {
         stripped = s;
      }
      stripped = stripped.trim();
      return stripped.isEmpty() ? null : stripped;
   }
}
