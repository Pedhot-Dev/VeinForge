package me.grish.veinforge.ui.hud.elements;

import lombok.Getter;
import me.grish.veinforge.client.overlay.TextHud;
import me.grish.veinforge.feature.impl.Pathfinder;
import me.grish.veinforge.pathfinder.calculate.PathfindingTelemetry;

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

   @Override
   protected int getAccentColor() {
      return 0xFF14B8A6;
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
         lines.add("§b§lPathfinder Stats");
         lines.add("§7Status: §asuccess");
         lines.add("§7search_ms: §f44.21 §8| §7smoothing_ms: §f2.63");
         lines.add("§7expanded: §f815 §8| §7open_peak: §f243");
         lines.add("§7path_len: §f58 §8| §7smooth_len: §f14");
         lines.add("§7failure: §f-");
         return;
      }

      PathfindingTelemetry telemetry = Pathfinder.getInstance().getLastTelemetry();
      lines.add("§b§lPathfinder Stats");
      if (telemetry == null) {
         lines.add("§7No telemetry yet");
         lines.add("§8Run §f/debug path <x> <y> <z>§8 first");
         return;
      }

      String status = telemetry.isSuccess() ? "§asuccess" : "§cfailed";
      String failure = telemetry.getFailureReason().isEmpty() ? "-" : telemetry.getFailureReason();
      lines.add("§7Status: " + status + (telemetry.isDirectWalk() ? " §8(§bdirect§8)" : ""));
      lines.add(String.format(Locale.ROOT,
                              "§7search_ms: §f%.2f §8| §7smoothing_ms: §f%.2f",
                              telemetry.getSearchMs(),
                              telemetry.getSmoothingMs()));
      lines.add("§7expanded: §f" + telemetry.getExpandedNodes() + " §8| §7open_peak: §f" + telemetry.getOpenSetPeak());
      lines.add("§7path_len: §f" + telemetry.getPathLength() + " §8| §7smooth_len: §f" + telemetry.getSmoothedPathLength());
      lines.add("§7failure: §f" + failure);
   }
}
