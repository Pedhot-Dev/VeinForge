package me.grish.veinforge.feature.route;

import me.grish.veinforge.VeinForge;
import me.grish.veinforge.route.graph.GraphPathfinder;
import me.grish.veinforge.util.KeyPressUtil;
import me.grish.veinforge.util.Logger;
import me.grish.veinforge.util.PlayerUtil;
import me.grish.veinforge.util.helper.graph.Graph;
import me.grish.veinforge.util.helper.route.RouteWaypoint;
import me.grish.veinforge.util.helper.route.WaypointType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.function.Supplier;

public class GraphEditorFeature {

   private final Minecraft mc = Minecraft.getInstance();
   private final GraphPathfinder pathfinder;
   private volatile WaypointType editorPlacementType = WaypointType.WALK;
   private RouteWaypoint lastPos = null;

   public GraphEditorFeature(GraphPathfinder pathfinder) {
      this.pathfinder = pathfinder;
   }

   public WaypointType getEditorPlacementType() {
      return editorPlacementType;
   }

   public RouteWaypoint getLastPos() {
      return lastPos;
   }

   public void resetPlacementType() {
      editorPlacementType = WaypointType.WALK;
   }

   public boolean onInput(
           boolean editing,
           Supplier<Graph<RouteWaypoint>> activeGraphSupplier,
           Object graphLock
   ) {
      var config = VeinForge.config();
      if (config == null) {
         return false;
      }

      var window = mc.getWindow();
      boolean selectPressed = KeyPressUtil.wasPressed(window, config.routeMiner.routeBuilderSelect, editing);
      boolean unidiPressed = KeyPressUtil.wasPressed(window, config.routeMiner.routeBuilderUnidi, editing);
      boolean bidiPressed = KeyPressUtil.wasPressed(window, config.routeMiner.routeBuilderBidi, editing);
      boolean movePressed = KeyPressUtil.wasPressed(window, config.routeMiner.routeBuilderMove, editing);
      boolean deletePressed = KeyPressUtil.wasPressed(window, config.routeMiner.routeBuilderDelete, editing);
      boolean walkNodePressed = KeyPressUtil.wasPressed(window, config.routeMiner.routeBuilderGraphWalkNode, editing);
      boolean etherwarpNodePressed = KeyPressUtil.wasPressed(window, config.routeMiner.routeBuilderGraphEtherwarpNode, editing);

      if (!editing) {
         return false;
      }

      Graph<RouteWaypoint> graph = activeGraphSupplier == null ? null : activeGraphSupplier.get();
      if (graph == null) {
         return false;
      }

      BlockPos standingBlock = PlayerUtil.getBlockStandingOn();
      if (standingBlock == null) {
         return false;
      }

      boolean changed = false;

      if (walkNodePressed || etherwarpNodePressed) {
         WaypointType selectedType = etherwarpNodePressed ? WaypointType.ETHERWARP : WaypointType.WALK;
         editorPlacementType = selectedType;
         RouteWaypoint placedNode = new RouteWaypoint(standingBlock, selectedType);
         synchronized (graphLock) {
            graph.add(placedNode);
         }
         lastPos = placedNode;
         changed = true;
         Logger.sendMessage("Graph node set to " + selectedType + " at current block.");
      }

      RouteWaypoint currentWaypoint = resolveWaypointAtPosition(graph, standingBlock, editorPlacementType, true);
      if (currentWaypoint == null) {
         return changed;
      }

      if (selectPressed) {
         synchronized (graphLock) {
            graph.add(currentWaypoint);
         }
         lastPos = currentWaypoint;
         changed = true;
         Logger.sendMessage("Changed parent to " + currentWaypoint.getTransportMethod() + " node.");
      }

      if (unidiPressed || bidiPressed) {
         boolean isBidi = bidiPressed;
         synchronized (graphLock) {
            if (lastPos != null) {
               graph.add(lastPos, currentWaypoint, isBidi);
               Logger.sendMessage("Added " + (isBidi ? "Bidirectional" : "Unidirectional"));
            } else {
               graph.add(currentWaypoint);
               Logger.sendMessage("Added Single Waypoint");
            }
         }
         lastPos = currentWaypoint;
         changed = true;
      }

      if (movePressed && lastPos != null) {
         synchronized (graphLock) {
            graph.update(lastPos, currentWaypoint);
         }
         lastPos = currentWaypoint;
         changed = true;
         Logger.sendMessage("Updated");
      }

      if (deletePressed && lastPos != null) {
         synchronized (graphLock) {
            graph.remove(lastPos);
         }
         lastPos = null;
         changed = true;
         Logger.sendMessage("Removed");
      }

      return changed;
   }

   private RouteWaypoint resolveWaypointAtPosition(
           Graph<RouteWaypoint> graph,
           BlockPos pos,
           WaypointType preferredType,
           boolean createIfMissing
   ) {
      if (graph == null || pos == null) {
         return null;
      }
      RouteWaypoint candidate = new RouteWaypoint(pos, preferredType == null ? WaypointType.WALK : preferredType);
      RouteWaypoint resolved = pathfinder.resolveWaypoint(graph, candidate);
      if (resolved != null) {
         return resolved;
      }
      return createIfMissing ? candidate : null;
   }
}
