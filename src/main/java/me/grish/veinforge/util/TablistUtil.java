package me.grish.veinforge.util;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import me.grish.veinforge.event.UpdateTablistEvent;
import me.grish.veinforge.event.UpdateTablistFooterEvent;
import me.grish.veinforge.macro.impl.GlacialMacro.GlaciteVeins;
import me.grish.veinforge.mixin.gui.PlayerListHudAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TablistUtil {

   public static final Ordering<PlayerInfo> playerOrdering = Ordering.from(
           new PlayerComparator());

   private static final CopyOnWriteArrayList<String> cachedTablist = new CopyOnWriteArrayList<>();
   private static final CopyOnWriteArrayList<String> cachedTablistFooter = new CopyOnWriteArrayList<>();

   public static List<String> getCachedTablist() {
      return new ArrayList<>(cachedTablist);
   }

   public static void setCachedTablist(List<String> tablist) {
      cachedTablist.clear();
      cachedTablist.addAll(tablist);
   }

   public static List<String> getCachedTablistFooter() {
      return new ArrayList<>(cachedTablistFooter);
   }

   public static void update() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.gui == null) {
         return;
      }

      List<String> newTablist = new ArrayList<>();
      List<String> newFooter = new ArrayList<>();

      try {
         Component header = ((PlayerListHudAccessor) mc.gui.hud.getTabList()).veinforge$getHeader();
         if (header != null) {
            addSplitLines(newTablist, header.getString());
            if (!newTablist.isEmpty() && !newTablist.get(newTablist.size() - 1).isEmpty()) {
               newTablist.add("");
            }
         }
      } catch (Exception ignored) {
      }

      for (String line : getTabListPlayersUnprocessed()) {
         if (line == null) {
            continue;
         }
         String cleaned = ScoreboardUtil.sanitizeString(line).trim();
         newTablist.add(cleaned);
      }

      try {
         Component footer = ((PlayerListHudAccessor) mc.gui.hud.getTabList()).veinforge$getFooter();
         if (footer != null) {
            addSplitLines(newFooter, footer.getString());
         }
      } catch (Exception ignored) {
      }

      if (!cachedTablist.equals(newTablist)) {
         setCachedTablist(newTablist);
         UpdateTablistEvent.fire(List.copyOf(newTablist));
      }

      if (!cachedTablistFooter.equals(newFooter)) {
         setCachedTabListFooter(newFooter);
         UpdateTablistFooterEvent.fire(List.copyOf(newFooter));
      }
   }

   private static void addSplitLines(List<String> out, String text) {
      if (text == null || text.isEmpty()) {
         return;
      }
      String[] parts = text.split("\\n");
      for (String part : parts) {
         if (part == null) {
            continue;
         }
         String cleaned = ScoreboardUtil.sanitizeString(part).trim();
         out.add(cleaned);
      }
   }

   public static void setCachedTabListFooter(List<String> tabListFooter) {
      cachedTablistFooter.clear();
      cachedTablistFooter.addAll(tabListFooter);
   }

   public static List<String> getTabListPlayersUnprocessed() {
      try {
         if (Minecraft.getInstance().player == null) return new ArrayList<>();

         Collection<PlayerInfo> entries = Minecraft.getInstance().player.connection.getOnlinePlayers();
         List<PlayerInfo> players = playerOrdering.sortedCopy(entries);

         List<String> result = new ArrayList<>();

         for (PlayerInfo info : players) {
//            String name = Minecraft.getInstance().gui.tabList.getNameForDisplay(info).getString();
            Minecraft mc = Minecraft.getInstance();
            String name = mc.gui.hud.getTabList()
                    .getNameForDisplay(info)
                    .getString();
            result.add(name);
         }
         return result;
      } catch (Exception e) {
         return new ArrayList<>();
      }
   }

   public static List<String> getTabListPlayersSkyblock() {
      try {
         List<String> tabListPlayersFormatted = getTabListPlayersUnprocessed();
         if (tabListPlayersFormatted.isEmpty()) return new ArrayList<>();

         List<String> playerList = new ArrayList<>();
         // tabListPlayersFormatted.remove(0); // remove "Players (x)" - Logic might differ in modern? Keeping safe.
         // Modern tab list usually just lists players. "Players (x)" might be a header?
         // If it is a header, it's not in the player list usually.

         String firstPlayer = null;
         for (String s : tabListPlayersFormatted) {
            int a = s.indexOf("]");
            if (a == -1) {
               continue;
            }
            if (s.length() < a + 2) {
               continue; // if the player name is too short (e.g. "§c[§f]"
            }

            s = s.substring(a + 2);

            // Remove Minecraft formatting codes and non-ASCII characters
            s = s.replaceAll("§[0-9a-fk-or]", "");
            s = s.replaceAll("[^\\x00-\\x7F]", "");
            s = s.trim();

            if (firstPlayer == null) {
               firstPlayer = s;
            } else if (s.equals(firstPlayer)) // it returns two copy of the player list for some reason
            {
               break;
            }
            playerList.add(s);
         }
         return playerList;
      } catch (Exception e) {
         return new ArrayList<>();
      }
   }

   public static Map<GlaciteVeins, Double> getGlaciteComs() {
      Pattern glaciteComPattern = Pattern.compile("(.+?) (Gemstone )?Collector: ?(\\d{1,3}(\\.\\d+)?%|DONE)");
      Map<GlaciteVeins, Double> comms = new HashMap<>();
      boolean foundCommission = false;
      for (final String text : cachedTablist) {
         if (!foundCommission) {
            if (text.equalsIgnoreCase("Commissions:")) {
               foundCommission = true;
            }
            continue;
         }

         Matcher glaciteComMatcher = glaciteComPattern.matcher(text);
         if (glaciteComMatcher.find()) {
            GlaciteVeins material = convertMaterial(glaciteComMatcher.group(1).trim());
            String progressStr = glaciteComMatcher.group(3);

            double progressValue;
            if ("DONE".equals(progressStr)) {
               progressValue = 100;
            } else {
               progressValue = Double.parseDouble(progressStr.replace("%", ""));
            }

            if (material != null) {
               comms.put(material, progressValue);
            }
         }
      }

      return comms;
   }

   private static GlaciteVeins convertMaterial(String material) {
      switch (material) {
         case "Amber":
            return GlaciteVeins.AMBER;
         case "Sapphire":
            return GlaciteVeins.SAPPHIRE;
         case "Amethyst":
            return GlaciteVeins.AMETHYST;
         case "Ruby":
            return GlaciteVeins.RUBY;
         case "Jade":
            return GlaciteVeins.JADE;
         case "Aquamarine":
            return GlaciteVeins.AQUAMARINE;
         case "Onyx":
            return GlaciteVeins.ONYX;
         case "Peridot":
            return GlaciteVeins.PERIDOT;
         case "Citrine":
            return GlaciteVeins.CITRINE;
         case "Topaz":
            return GlaciteVeins.TOPAZ;
         case "Tungsten":
            return GlaciteVeins.TUNGSTEN;
         case "Umber":
            return GlaciteVeins.UMBER;
         case "Glacite":
            return GlaciteVeins.GLACITE;
         default:
            return null;
      }
   }

   @Environment(EnvType.CLIENT)
   static class PlayerComparator implements Comparator<PlayerInfo> {

      private PlayerComparator() {
      }

      public int compare(PlayerInfo o1, PlayerInfo o2) {
         PlayerTeam team1 = o1.getTeam();
         PlayerTeam team2 = o2.getTeam();
         return ComparisonChain.start().compareTrueFirst(
                         o1.getGameMode() != GameType.SPECTATOR,
                         o2.getGameMode() != GameType.SPECTATOR
                 )
                        .compare(
                                team1 != null ? team1.getName() : "",
                                team2 != null ? team2.getName() : ""
                        )
                        .compare(o1.getProfile().name(), o2.getProfile().name()).result();
      }
   }
}
