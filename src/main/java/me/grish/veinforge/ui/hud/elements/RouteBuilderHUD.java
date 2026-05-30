package me.grish.veinforge.ui.hud.elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import me.grish.veinforge.VeinForge;
import me.grish.veinforge.client.overlay.TextHud;
import me.grish.veinforge.feature.impl.RouteBuilder;
import me.grish.veinforge.handler.GraphHandler;
import me.grish.veinforge.handler.RouteHandler;
import me.grish.veinforge.util.KeyPressUtil;
import me.grish.veinforge.util.PlayerUtil;
import me.grish.veinforge.util.helper.graph.Graph;
import me.grish.veinforge.util.helper.route.Route;
import me.grish.veinforge.util.helper.route.RouteWaypoint;

public class RouteBuilderHUD extends TextHud {

   @Getter
   private static final RouteBuilderHUD instance = new RouteBuilderHUD();

   private final transient GraphHandler graphHandler = GraphHandler.instance;
   private final transient RouteHandler routeHandler = RouteHandler.getInstance();
   private final transient RouteBuilder routeBuilder = RouteBuilder.getInstance();

   public RouteBuilderHUD() {
      super();
      this.x = 5;
      this.y = 90;
      this.enabled = true;
   }

   @Override
   protected int getAccentColor() {
      return 0xFF8B5CF6;
   }

   @Override
   protected boolean shouldShow() {
      if (!super.shouldShow()) {
         return false;
      }
      return routeBuilder.isRunning() || graphHandler.isEditing() || graphHandler.isDebugRenderEnabled();
   }

   @Override
   protected void getLines(List<String> lines, boolean example) {
      if (example) {
         lines.add("§d§lRouteBuilder");
         lines.add("§8§m------------------------");
         lines.add("§8» §7Editing: §aON");
         lines.add("§8» §7Graph: §fCommission Macro §8(§cDirty§8)");
         lines.add("§8» §7Node mode: §fETHERWARP");
         lines.add("§8» §7Nodes: §a15");
         lines.add("§8» §aStand: §f-3, 110, 15");
         lines.add("§8» §cSelect: §f-4, 110, 16 §8(§f2.4m§8)");
         lines.add("§8» §bHover: §f-2, 110, 14 §8(§f6.1m§8)");
         lines.add("§8» §7Node: §fWALK");
         lines.add("§8» §7Conn: §7Out:§f2 §7In:§f1");
         lines.add("§8» §7To: §f-2,110,13 §8, §f-1,110,14");
         lines.add("§8§m------------------------");
         lines.add("§8Controls:");
         lines.add("§8» §7Select parent: §f[KP4]");
         lines.add("§8» §7Add uni/bi edge: §f[KP7]/[KP8]");
         lines.add("§8» §7Move/Delete selected: §f[KP5]/[KP6]");
         lines.add("§8» §7Save now: §f/graph save");
         return;
      }

      boolean routeEditing = routeBuilder.isRunning();
      if (routeEditing && !graphHandler.isEditing() && !graphHandler.isDebugRenderEnabled()) {
         addRouteEditorLines(lines);
         return;
      }

      boolean editing = graphHandler.isEditing();
      boolean debugRender = graphHandler.isDebugRenderEnabled();
      Graph<RouteWaypoint> graph = graphHandler.getCachedGraph();
      if (graph == null) {
         graph = graphHandler.getActiveGraph();
      }

      BlockPos standingBlock = graphHandler.getCachedStandingBlock();
      BlockPos hoveredBlock = graphHandler.getCachedHoveredBlock();
      RouteWaypoint hoveredWaypoint = graphHandler.getCachedHoveredWaypoint();
      RouteWaypoint selected = graphHandler.getLastPos();

      lines.add("§d§lRouteBuilder");
      lines.add("§8§m------------------------");
      lines.add("§8» §7Editing: " + (editing ? "§aON" : "§cOFF"));
      if (!editing && debugRender) {
         lines.add("§8» §7Debug View: §aON");
      }
      String shownGraph = editing ? graphHandler.getActiveGraphKey() : (debugRender ? graphHandler.getDebugGraphKey() : graphHandler.getActiveGraphKey());
      lines.add("§8» §7Graph: §f" + shownGraph + (graphHandler.isDirty() ? " §8(§cDirty§8)" : " §8(§aSaved§8)"));
      if (editing) {
         lines.add("§8» §7Node mode: §f" + graphHandler.getEditorPlacementType());
      }
      lines.add("§8» §7Nodes: §a" + graph.map.size());

      if (standingBlock != null) {
         lines.add("§8» §aStand: §f" + standingBlock.getX() + ", " + standingBlock.getY() + ", " + standingBlock.getZ());
      }
      if (selected != null) {
         String dist = formatDistanceTo(selected.toBlockPos());
         lines.add(
                 "§8» §cSelect: §f" + selected.getX() + ", " + selected.getY() + ", " + selected.getZ()
                         + (dist == null ? "" : " §8(§f" + dist + "§8)")
         );
      } else {
         lines.add("§8» §cSelect: §7None");
      }
      if (hoveredBlock != null) {
         String dist = formatDistanceTo(hoveredBlock);
         lines.add(
                 "§8» §bHover: §f" + hoveredBlock.getX() + ", " + hoveredBlock.getY() + ", " + hoveredBlock.getZ()
                         + (dist == null ? "" : " §8(§f" + dist + "§8)")
         );
      } else {
         lines.add("§8» §bHover: §7None");
      }

      RouteWaypoint infoWaypoint = hoveredWaypoint != null ? hoveredWaypoint : selected;
      if (infoWaypoint != null) {
         Set<RouteWaypoint> edges = graph.map.getOrDefault(infoWaypoint, Collections.emptySet());
         int outgoing = edges.size();
         int incoming = graphHandler.getIncomingEdgeCount(graph, infoWaypoint);

         lines.add("§8» §7Node: §f" + infoWaypoint.getTransportMethod());
         lines.add("§8» §7Conn: §7Out:§f" + outgoing + " §7In:§f" + incoming);

         String outPreview = formatOutgoingPreview(edges, 2);
         if (outPreview != null) {
            lines.add("§8» §7To: " + outPreview);
         }
      }

      lines.add("§8§m------------------------");
      lines.add("§8Controls:");
      for (String hint : graphHandler.getEditorControlHints()) {
         lines.add("§8» §7" + hint.replace(": ", ": §f"));
      }
      lines.add("§8» §7Colors: §aNode §8| §cSelected §8| §aOne-way §8| §2Two-way");
      lines.add("§8» §7Save now: §f/graph save");
      lines.add("§8» §7Show controls: §f/graph keys");
      lines.add("§8» §7Render only: §f/graph debug show <name>");
   }

   private void addRouteEditorLines(List<String> lines) {
      lines.add("§d§lRouteBuilder");
      lines.add("§8§m------------------------");
      lines.add("§8» §7Editing: §aON");
      lines.add("§8» §7Mode: §fRoute Editor");

      Route selectedRoute = routeHandler.getSelectedRoute();
      String selectedRouteName = routeHandler.getSelectedRouteName();
      int waypointCount = selectedRoute == null ? 0 : selectedRoute.size();
      lines.add("§8» §7Route: §f" + selectedRouteName);
      lines.add("§8» §7Waypoints: §a" + waypointCount);

      BlockPos standingBlock = PlayerUtil.getBlockStandingOn();
      if (standingBlock != null) {
         lines.add("§8» §aStand: §f" + standingBlock.getX() + ", " + standingBlock.getY() + ", " + standingBlock.getZ());
      } else {
         lines.add("§8» §aStand: §7Unknown");
      }

      if (selectedRoute != null && !selectedRoute.isEmpty() && standingBlock != null) {
         Optional<RouteWaypoint> closest = selectedRoute.getClosest(standingBlock);
         if (closest.isPresent()) {
            RouteWaypoint waypoint = closest.get();
            int index = selectedRoute.indexOf(waypoint) + 1;
            String dist = formatDistanceTo(waypoint.toBlockPos());
            lines.add(
                    "§8» §bClosest: §f#" + index
                            + " §7(" + waypoint.getTransportMethod() + ") §f"
                            + waypoint.getX() + ", " + waypoint.getY() + ", " + waypoint.getZ()
                            + (dist == null ? "" : " §8(§f" + dist + "§8)")
            );
         } else {
            lines.add("§8» §bClosest: §7None");
         }
      } else {
         lines.add("§8» §bClosest: §7None");
      }

      lines.add("§8§m------------------------");
      lines.add("§8Controls:");
      var config = VeinForge.config();
      if (config != null) {
         lines.add("§8» §7Add WALK: §f[" + KeyPressUtil.getKeyName(config.routeMiner.routeBuilderWalkAddKeybind) + "]");
         lines.add("§8» §7Add ETHERWARP: §f[" + KeyPressUtil.getKeyName(config.routeMiner.routeBuilderEtherwarpAddKeybind) + "]");
         lines.add("§8» §7Remove closest: §f[" + KeyPressUtil.getKeyName(config.routeMiner.routeBuilderRemoveKeybind) + "]");
      }
      lines.add("§8» §7Commands: §f/rb add|remove|replace");
      lines.add("§8» §7Show keys: §f/rb keys");
   }

   private String formatOutgoingPreview(Set<RouteWaypoint> edges, int max) {
      if (edges == null || edges.isEmpty() || max <= 0) {
         return null;
      }

      List<RouteWaypoint> list = new ArrayList<>(edges);
      list.sort(
              Comparator.comparingInt(RouteWaypoint::getX)
                      .thenComparingInt(RouteWaypoint::getY)
                      .thenComparingInt(RouteWaypoint::getZ)
      );

      StringBuilder sb = new StringBuilder();
      int shown = Math.min(max, list.size());
      for (int i = 0; i < shown; i++) {
         RouteWaypoint wp = list.get(i);
         if (i > 0) {
            sb.append("§8, ");
         }
         sb.append("§f");
         sb.append(wp.getX()).append(",").append(wp.getY()).append(",").append(wp.getZ());
      }
      if (list.size() > shown) {
         sb.append(" §8+").append(list.size() - shown);
      }
      return sb.toString();
   }

   private String formatDistanceTo(BlockPos pos) {
      if (pos == null || mc.player == null) {
         return null;
      }
      Vec3 player = new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ());
      Vec3 target = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
      double d = player.distanceTo(target);
      return String.format("%.1fm", d);
   }
}
