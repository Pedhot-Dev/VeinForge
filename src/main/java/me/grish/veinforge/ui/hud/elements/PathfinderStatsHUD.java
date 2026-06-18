package me.grish.veinforge.ui.hud.elements;

import lombok.Getter;
import me.grish.veinforge.client.overlay.TextHud;
import me.grish.veinforge.feature.impl.Pathfinder;
import me.grish.veinforge.pathfinder.calculate.PathfindingTelemetry;
import me.grish.veinforge.ui.hud.ColorPalette;

import java.util.List;
import java.util.Locale;

public class PathfinderStatsHUD extends TextHud {

   @Getter
   private static final PathfinderStatsHUD instance = new PathfinderStatsHUD();

   public PathfinderStatsHUD() {
      super();
      this.x = 5;
      this.y = 140;
      this.enabled = true;
   }

   public static PathfinderStatsHUD getInstance() {
      return instance;
   }

   @Override
   protected int getAccentColor() {
      return ColorPalette.EMERALD_400;
   }

   @Override
   protected boolean shouldShow() {
      if (!super.shouldShow()) {
         return false;
      }
      Pathfinder pathfinder = Pathfinder.getInstance();
      return pathfinder.isRunning() || pathfinder.isRenderOnlyMode() || pathfinder.getLastTelemetry() != null;
   }

   @Override
   protected void getLines(List<String> lines, boolean example) {
      if (example) {
         lines.add("§b§lPATHFINDER STATS");
         lines.add("§8§m------------------------");
         lines.add("§8» §7Status: §aSUCCESS");
         lines.add("§8» §7Time: §f44.2ms §8(§7smooth: §f2.6ms§8)");
         lines.add("§8» §7Nodes: §f815 §8(§7peak: §f243§8)");
         lines.add("§8» §7Length: §f58 §8(§7smooth: §f14§8)");
         return;
      }

      PathfindingTelemetry telemetry = Pathfinder.getInstance().getLastTelemetry();
      lines.add("§b§lPATHFINDER STATS");
      lines.add("§8§m------------------------");
      if (telemetry == null) {
         lines.add("§8» §7No telemetry recorded.");
         lines.add("§8» §7Use §f/debug path§7 first.");
         return;
      }

      String status = telemetry.success() ? "§aSUCCESS" : "§cFAILED";
      String failure = telemetry.failureReason().isEmpty() ? "-" : telemetry.failureReason();
      lines.add("§8» §7Status: " + status + (telemetry.directWalk() ? " §8(§bdirect§8)" : ""));
      lines.add(String.format(Locale.ROOT, "§8» §7Time: §f%.1fms §8(§7smooth: §f%.1fms§8)", telemetry.searchMs(), telemetry.smoothingMs()));
      lines.add("§8» §7Nodes: §f" + telemetry.expandedNodes() + " §8(§7peak: §f" + telemetry.openSetPeak() + "§8)");
      lines.add("§8» §7Length: §f" + telemetry.pathLength() + " §8(§7smooth: §f" + telemetry.smoothedPathLength() + "§8)");
      if (!telemetry.success()) {
          lines.add("§8» §7Error: §f" + failure);
      }
   }
}
