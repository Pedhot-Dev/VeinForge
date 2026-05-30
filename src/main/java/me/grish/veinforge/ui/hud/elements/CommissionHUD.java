package me.grish.veinforge.ui.hud.elements;

import lombok.Getter;
import me.grish.veinforge.VeinForge;
import me.grish.veinforge.VeinForgeClient;
import me.grish.veinforge.client.overlay.TextHud;
import me.grish.veinforge.handler.GameStateHandler;
import me.grish.veinforge.macro.impl.CommissionMacro.CommissionMacro;
import me.grish.veinforge.macro.impl.CommissionMacro.states.CommissionMacroState;
import me.grish.veinforge.util.helper.location.Location;
import me.grish.veinforge.util.tablist.TabListParser;
import me.grish.veinforge.util.tablist.WidgetType;

import java.util.List;

public class CommissionHUD extends TextHud {

   @Getter
   private final static CommissionHUD instance = new CommissionHUD();
   private final transient CommissionMacro commissionMacro = CommissionMacro.getInstance();
   public boolean commHudResetStats = false;

   public CommissionHUD() {
      super();
      this.x = 5;
      this.y = 5;
   }

   @Override
   protected int getAccentColor() {
      return 0xFF22D3EE;
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

      String statusLabel;
      if (!isRunning) {
         statusLabel = "Paused";
      } else if (recentlyCompleted) {
         statusLabel = "Complete";
      } else {
         statusLabel = "Running";
      }
      String stateLabel = isRunning ? getStateLabel(currentState) : null;

      lines.add("§b§lDwarven Commission Macro");
      lines.add("§8§m------------------------");

      String runtimeStr = formatElapsedTime(uptime);
      lines.add("§8» §7" + (isRunning ? "§a" : "§c") + statusLabel + " §8(§b" + runtimeStr + "§8)" + (stateLabel != null ? " §8- §e" + stateLabel : ""));

      lines.add("§8» §7Comms: §a" + totalComms + " §8(§3" + commsPerHour + "/h§8)");
      lines.add("§8» §7XP: §d" + formatNumberWithK(hotmExpGained) + " §8(§d" + formatNumberWithK(hotmXpPerHour) + "/h§8)");

      if (isRunning && avgSecondsPerComm > 0) {
         double progressRatio = Math.min(1.0, Math.max(0.0, (double) elapsedSinceLast / (double) avgSecondsPerComm));
         int progressPercent = (int) Math.round(progressRatio * 100.0);
         long etaSeconds = Math.max(0L, avgSecondsPerComm - elapsedSinceLast);
         lines.add("§8» §7Prog: §e" + progressPercent + "% §8(ETA: §f" + formatElapsedTime(etaSeconds) + "§8)");
         lines.add("§8» §7Avg: §f" + formatElapsedTime(avgSecondsPerComm) + " §8| §7Last: §f" + formatElapsedTime(elapsedSinceLast));
      }
      if (isRunning) {
         lines.add("§8» §7Now: §b" + (currentCommissionName != null ? currentCommissionName : "Unknown"));
      }
      String version = VeinForgeClient.instance != null ? VeinForgeClient.instance.VERSION : "unknown";
      lines.add("§8§m------------------------");
      lines.add("§8VeinForge §7v" + version);
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
