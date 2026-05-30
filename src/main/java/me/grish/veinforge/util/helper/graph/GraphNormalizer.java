package me.grish.veinforge.util.helper.graph;

import me.grish.veinforge.util.helper.route.RouteWaypoint;

import java.util.Map;
import java.util.Set;

public final class GraphNormalizer {

   private GraphNormalizer() {
   }

   public static GraphNormalizationResult normalizeLegacy(Graph<RouteWaypoint> graph) {
      GraphValidationResult validation = GraphValidator.validate(graph);
      Graph<RouteWaypoint> normalized = new Graph<>();

      for (Map.Entry<RouteWaypoint, Set<RouteWaypoint>> entry : graph.map.entrySet()) {
         RouteWaypoint source = entry.getKey();
         normalized.add(source);
         for (RouteWaypoint neighbor : entry.getValue()) {
            normalized.add(source, neighbor, false);
         }
      }

      return new GraphNormalizationResult(normalized, validation);
   }
}
