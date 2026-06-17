package me.grish.veinforge.ui.hud.elements;

import lombok.Getter;
import me.grish.veinforge.VeinForge;
import me.grish.veinforge.VeinForgeClient;
import me.grish.veinforge.client.overlay.TextHud;
import me.grish.veinforge.handler.GameStateHandler;
import me.grish.veinforge.macro.impl.CommissionMacro.CommissionMacro;
import me.grish.veinforge.macro.impl.CommissionMacro.states.CommissionMacroState;
import me.grish.veinforge.ui.hud.ColorPalette;
import me.grish.veinforge.util.helper.location.Location;
import me.grish.veinforge.util.tablist.TabListParser;
import me.grish.veinforge.util.tablist.WidgetType;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

public class CommissionHUD extends TextHud {

   @Getter
   private final static CommissionHUD instance = new CommissionHUD();

   public static CommissionHUD getInstance() {
      return instance;
   }
   private final transient CommissionMacro commissionMacro = CommissionMacro.getInstance();
   public boolean commHudResetStats = false;
   private float currentProgress = 0.0f;

   public CommissionHUD() {
      super();
      this.x = 5;
      this.y = 5;
   }

   @Override
   protected int getAccentColor() {
      return ColorPalette.CYAN_400;
   }

   @Override
   protected int getExtraHeight() {
      return commissionMacro.isEnabled() ? 10 : 0;
   }

   @Override
   protected void postRender(GuiGraphicsExtractor context, int panelW, int panelH, float scale) {
      if (!commissionMacro.isEnabled() || currentProgress <= 0) return;

      int padding = getPaddingPx();
      int barY = panelH - padding - 8;
      drawProgressBar(context, padding, barY, panelW - padding * 2, 4, currentProgress, getAccentColor());
   }

   @Override
   protected void getLines(List<String> lines, boolean example) {
      long uptime;
      int totalComms;
      long totalHotmXp;
      boolean isRunning;
      String currentCommissionName = null;
      CommissionMacroState currentState = null;
      long lastCompleteAtMs = 0L;
      long nowMs = System.currentTimeMillis();
      if (example) {
         uptime = (2 * 3600L) + (14 * 60L) + 7;
         totalComms = 18;
         totalHotmXp = 5600L;
         isRunning = true;
         currentCommissionName = "Mithril Miner";
         lastCompleteAtMs = nowMs - 75_000L;
         currentState = null;
         currentProgress = 0.65f;
      } else {
         uptime = commissionMacro.uptime.getTimePassed() / 1000;
         totalComms = commissionMacro.getActualCommissionCounter();
         totalHotmXp = commissionMacro.getTotalHotmXP();
         isRunning = commissionMacro.isEnabled();
         currentState = commissionMacro.getCurrentState();
         lastCompleteAtMs = commissionMacro.getLastCommissionCompleteAtMs();
         if (commissionMacro.getCurrentCommission() != null) {
            currentCommissionName = commissionMacro.getCurrentCommission().getName();
         }
      }
      long hotmExpGained = totalHotmXp;

      // Calculate commissions per hour
      int commsPerHour = 0;
      if (uptime > 0) {
         commsPerHour = (int) ((float) totalComms / uptime * 3600);
      }
      long hotmXpPerHour = 0L;
      if (uptime > 0) {
         hotmXpPerHour = (long) ((double) hotmExpGained / uptime * 3600);
      }

      long avgSecondsPerComm = totalComms > 0 ? (uptime / totalComms) : 0L;
      long elapsedSinceLast = lastCompleteAtMs > 0L ? (nowMs - lastCompleteAtMs) / 1000L : 0L;
      boolean recentlyCompleted = isRunning && lastCompleteAtMs > 0L && (nowMs - lastCompleteAtMs) <= 5_000L;

      if (!example) {
          if (isRunning && avgSecondsPerComm > 0) {
              currentProgress = (float) Math.min(1.0, Math.max(0.0, (double) elapsedSinceLast / (double) avgSecondsPerComm));
          } else {
              currentProgress = 0;
          }
      }

      String statusLabel;
      if (!isRunning) {
         statusLabel = "PAUSED";
      } else if (recentlyCompleted) {
         statusLabel = "COMPLETE";
      } else {
         statusLabel = "RUNNING";
      }
      String stateLabel = isRunning ? getStateLabel(currentState) : null;

      lines.add("§b§lDWARVEN COMMISSION");
      lines.add("§8§m------------------------");

      String runtimeStr = formatElapsedTime(uptime);
      String statusColor = isRunning ? "§a" : "§c";
      lines.add("§8» §7Status: " + statusColor + statusLabel + " §8(§b" + runtimeStr + "§8)");
      if (stateLabel != null) {
          lines.add("§8» §7State: §e" + stateLabel);
      }

      lines.add("§8» §7Comms: §f" + totalComms + " §8(§3" + commsPerHour + "/h§8)");
      lines.add("§8» §7HOTM XP: §d" + formatNumberWithK(hotmExpGained) + " §8(§d" + formatNumberWithK(hotmXpPerHour) + "/h§8)");

      if (isRunning) {
         lines.add("§8» §7Current: §b" + (currentCommissionName != null ? currentCommissionName : "Unknown"));
      }

      if (isRunning && avgSecondsPerComm > 0) {
         lines.add("§8§m------------------------");
         lines.add("§8» §7Avg: §f" + formatElapsedTime(avgSecondsPerComm) + " §8| §7ETA: §f" + formatElapsedTime(Math.max(0L, avgSecondsPerComm - elapsedSinceLast)));
      }
   }

   private String formatNumberWithK(long number) {
      if (number >= 1000) {
         double dividedNumber = number / 1000.0;
         if (dividedNumber % 1 == 0) {
            return String.format("%.0fk", dividedNumber);
         } else if (dividedNumber * 10 % 1 == 0) {
            return String.format("%.1fk", dividedNumber);
         } else {
            return String.format("%.2fk", dividedNumber);
         }
      }
      return String.valueOf(number);
   }

   private String getStateLabel(CommissionMacroState state) {
      if (state == null) {
         return "Idle";
      }
      String name = state.getClass().getSimpleName();
      if (name.endsWith("State")) {
         name = name.substring(0, name.length() - "State".length());
      }
      return name;
   }

   private String formatElapsedTime(long elapsedTimeSeconds) {
      long seconds = elapsedTimeSeconds % 60;
      long minutes = (elapsedTimeSeconds / 60) % 60;
      long hours = (elapsedTimeSeconds / 3600);

      return String.format("%02d:%02d:%02d", hours, minutes, seconds);
   }

   @Override
   protected boolean shouldShow() {
      if (!super.shouldShow()) return false;
      boolean macroTypeCondition = (VeinForge.config().general.macroType == 0);
      if (!macroTypeCondition) {
         return false;
      }

      if (VeinForge.config().commission.dwarvenCommission.showDwarvenCommHUDOutside) {
         return true;
      }

      Location location = GameStateHandler.getInstance().getCurrentLocation();
      boolean locationCondition = location == Location.DWARVEN_MINES
              || location == Location.CRYSTAL_HOLLOWS
              || location == Location.GLACITE_MINESHAFT;
      if (locationCondition) {
         return true;
      }

      var data = TabListParser.getCached();
      List<String> commissionLines = data == null ? null : data.widgetLines.get(WidgetType.COMMISSIONS);
      boolean hasCommissionWidget = commissionLines != null && !commissionLines.isEmpty();

      return hasCommissionWidget;
   }
}
