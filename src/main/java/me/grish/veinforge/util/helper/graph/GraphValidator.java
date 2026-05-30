package me.grish.veinforge.util.helper.graph;

import me.grish.veinforge.util.helper.route.RouteWaypoint;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class GraphValidator {

   private GraphValidator() {
   }

   public static GraphValidationResult validate(Graph<RouteWaypoint> graph) {
      if (graph == null) {
         return new GraphValidationResult(0, 0, 0, 0, 0);
      }

      int duplicateEdges = 0;
      int selfLoops = 0;
      int danglingEdges = 0;
      int edgeCount = 0;
      Set<RouteWaypoint> nodes = graph.map.keySet();

      for (Map.Entry<RouteWaypoint, Set<RouteWaypoint>> entry : graph.map.entrySet()) {
         Set<RouteWaypoint> seen = new HashSet<>();
         for (RouteWaypoint neighbor : entry.getValue()) {
            edgeCount++;
            if (neighbor.equals(entry.getKey())) {
               selfLoops++;
            }
            if (!nodes.contains(neighbor)) {
               danglingEdges++;
            }
            if (!seen.add(neighbor)) {
               duplicateEdges++;
            }
         }
      }

      return new GraphValidationResult(nodes.size(), edgeCount, duplicateEdges, selfLoops, danglingEdges);
   }

   public static GraphValidationResult validateStrict(Graph<RouteWaypoint> graph) {
      GraphValidationResult result = validate(graph);
      if (result.hasViolations()) {
         throw new IllegalStateException(
                 "Graph validation failed: duplicates=" + result.duplicateEdges()
                         + ", selfLoops=" + result.selfLoops()
                         + ", dangling=" + result.danglingEdges()
         );
      }
      return result;
   }
}
