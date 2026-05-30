package me.grish.veinforge.util.helper.graph;

import me.grish.veinforge.util.helper.route.RouteWaypoint;

public record GraphNormalizationResult(Graph<RouteWaypoint> graph, GraphValidationResult validation) {

   public boolean hasChanges() {
      return validation.hasViolations();
   }
}
