package me.grish.veinforge.failsafe.impl;

import lombok.Getter;
import me.grish.veinforge.VeinForge;
import me.grish.veinforge.failsafe.AbstractFailsafe;
import me.grish.veinforge.macro.MacroManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NameMentionFailsafe extends AbstractFailsafe {

   @Getter
   private static final NameMentionFailsafe instance = new NameMentionFailsafe();

   private static final Pattern SENDER_NAME_PATTERN = Pattern.compile("^.*?(?<senderName>[a-zA-Z0-9_]+)§?f?:");

   @Getter
   private boolean lobbyChangeRequested = false;

   @Override
   public int getPriority() {
      return 10;
   }

   @Override
   public String getName() {
      return "NameMentionFailsafe";
   }

   @Override
   public Failsafe getFailsafeType() {
      return Failsafe.NAME_MENTION;
   }

   @Override
   public void resetStates() {
      this.lobbyChangeRequested = false;
   }

   @Override
   public boolean onChat(String message) {
      if (mc.player == null) return false;
      String playerName = mc.player.getName().getString();
      String unformattedMessage = message;

      String senderName = null;
      Matcher matcher = SENDER_NAME_PATTERN.matcher(unformattedMessage);
      if (matcher.find()) {
         senderName = matcher.group("senderName");
      }

      if (senderName != null && senderName.equalsIgnoreCase(playerName)) {
         return false;
      }

      if (unformattedMessage.toLowerCase().contains(playerName.toLowerCase() + " invited ")) {
         return false;
      }

      //if (unformattedMessage.toLowerCase().contains("has invited you to join their party!")) { notify() }

      Pattern mentionPattern = Pattern.compile("\\b" + Pattern.quote(playerName) + "\\b", Pattern.CASE_INSENSITIVE);
      boolean isMentioned = mentionPattern.matcher(unformattedMessage).find();

      if (isMentioned) {
         if (VeinForge.config().failsafe.nameMentionFailsafeBehaviour == 1) { // 1 = Change Lobby
            warn("Your name was mentioned in chat. Changing lobbies.");
            lobbyChangeRequested = true;
            return false;
         } else { // 0 = Pause/Disable Macro
            return true;
         }
      }
      return false;
   }

   @Override
   public boolean react() {
      MacroManager.getInstance().disable();
      warn("Your name was mentioned in chat. Macro disabled.");
      return true;
   }
}