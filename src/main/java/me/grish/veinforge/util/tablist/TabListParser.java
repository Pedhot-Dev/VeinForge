package me.grish.veinforge.util.tablist;

import me.grish.veinforge.util.Logger;
import me.grish.veinforge.util.ScoreboardUtil;
import me.grish.veinforge.util.TablistUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class TabListParser {

   private static volatile TabListData cached = TabListData.EMPTY;

   public static TabListData getCached() {
      return cached;
   }

   /**
    * Stage 1 parser for the SkyBlock tablist widgets.
    * Produces lines grouped by widget prefix (e.g. "Commissions", "Powders", etc.).
    */
   public static TabListData parseTabList() {
      try {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player == null || mc.gui == null) {
            return TabListData.EMPTY;
         }

         List<PlayerInfo> ordered = TablistUtil.playerOrdering.sortedCopy(mc.player.connection.getOnlinePlayers());

         TabListData data = new TabListData();
         boolean inInfoColumn = false;
         WidgetType currentWidget = null;

         for (PlayerInfo entry : ordered) {
            Component displayName = mc.gui.hud.getTabList().getNameForDisplay(entry);
            if (displayName == null) {
               continue;
            }

            String content = ScoreboardUtil.sanitizeString(displayName.getString());
            String trimmed = content.trim();
            String profileName = entry.getProfile().name();

            // Column header detection: Hypixel typically uses "...a" names for headers.
            if (profileName.endsWith("a")) {
               if ("Info".equals(trimmed)) {
                  inInfoColumn = true;
                  continue;
               }
               if (inInfoColumn && !trimmed.isEmpty()) {
                  break;
               }
            }

            if (!inInfoColumn) {
               continue;
            }

            if (trimmed.isEmpty()) {
               continue;
            }

            if (content.startsWith(" ")) {
               if (currentWidget != null) {
                  data.widgetLines.get(currentWidget).add(content);
               }
               continue;
            }

            String prefix = content;
            int colon = content.indexOf(':');
            if (colon != -1) {
               prefix = content.substring(0, colon);
            }

            WidgetType widget = WidgetType.byPrefix(prefix);
            if (widget != null) {
               currentWidget = widget;
               data.widgetLines.putIfAbsent(currentWidget, new ArrayList<>());
               data.widgetLines.get(currentWidget).add(content);
            } else {
               currentWidget = null;
            }
         }

         data.activeWidgets = data.widgetLines.keySet();
         return data;
      } catch (Exception e) {
         Logger.sendLog("Failed to parse tablist widgets: " + e.getMessage());
         return TabListData.EMPTY;
      }
   }

   public static void updateCache() {
      cached = parseTabList();
   }
}
