package me.grish.veinforge.route.graph;

import me.grish.veinforge.util.helper.graph.Graph;
import me.grish.veinforge.util.helper.route.RouteWaypoint;
import me.grish.veinforge.util.helper.route.WaypointType;
import net.minecraft.core.BlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GraphPathfinder {

   public List<RouteWaypoint> findPathWithNearestReachableStart(
           Graph<RouteWaypoint> graph,
           BlockPos startPos,
           RouteWaypoint endCandidate
   ) {
      if (graph == null || startPos == null || endCandidate == null) {
         return Collections.emptyList();
      }

      RouteWaypoint resolvedEnd = resolveWaypoint(graph, endCandidate);
      if (resolvedEnd == null) {
         return Collections.emptyList();
      }

      RouteWaypoint startWaypoint = findNearestReachableStart(graph, startPos, resolvedEnd);
      if (startWaypoint == null) {
         return Collections.emptyList();
      }

      return trimFirstWaypointIfSafe(graph.findPath(startWaypoint, resolvedEnd), startPos);
   }

   public List<RouteWaypoint> findPathUsingExplicitStart(
           Graph<RouteWaypoint> graph,
           RouteWaypoint startCandidate,
           RouteWaypoint endCandidate
   ) {
      if (graph == null || startCandidate == null || endCandidate == null) {
         return Collections.emptyList();
      }

      RouteWaypoint resolvedEnd = resolveWaypoint(graph, endCandidate);
      if (resolvedEnd == null) {
         return Collections.emptyList();
      }

      RouteWaypoint resolvedStart = resolveWaypoint(graph, startCandidate);
      BlockPos startPos = startCandidate.toBlockPos();
      if (resolvedStart == null) {
         resolvedStart = findNearestReachableStart(graph, startPos, resolvedEnd);
      }
      if (resolvedStart == null) {
         return Collections.emptyList();
      }

      return trimFirstWaypointIfSafe(graph.findPath(resolvedStart, resolvedEnd), startPos);
   }

   public RouteWaypoint resolveWaypoint(Graph<RouteWaypoint> graph, RouteWaypoint candidate) {
      if (graph == null || candidate == null) {
         return null;
      }

      if (graph.map.containsKey(candidate)) {
         return candidate;
      }

      RouteWaypoint fallbackWalk = null;
      RouteWaypoint fallbackAny = null;

      for (RouteWaypoint node : graph.map.keySet()) {
         if (node.getX() != candidate.getX() || node.getY() != candidate.getY() || node.getZ() != candidate.getZ()) {
            continue;
         }
         if (node.getTransportMethod() == candidate.getTransportMethod()) {
            return node;
         }
         if (node.getTransportMethod() == WaypointType.WALK && fallbackWalk == null) {
            fallbackWalk = node;
         }
         if (fallbackAny == null) {
            fallbackAny = node;
         }
      }

      return fallbackWalk != null ? fallbackWalk : fallbackAny;
   }

   private List<RouteWaypoint> trimFirstWaypointIfSafe(List<RouteWaypoint> route, BlockPos playerPos) {
      if (route.size() < 2 || playerPos == null) {
         return route;
      }

      if (playerPos.distSqr(route.get(0).toBlockPos()) >= route.get(0).toBlockPos().distSqr(route.get(1).toBlockPos())) {
         return route;
      }

      List<RouteWaypoint> trimmed = new ArrayList<>(route);
      trimmed.remove(0);
      return trimmed;
   }

   private RouteWaypoint findNearestReachableStart(Graph<RouteWaypoint> graph, BlockPos from, RouteWaypoint end) {
      if (graph == null || from == null || end == null || graph.map.isEmpty()) {
         return null;
      }

      Set<RouteWaypoint> reachable = collectNodesThatCanReachEnd(graph, end);
      if (reachable.isEmpty()) {
         return null;
      }

      return reachable.stream()
                      .min(Comparator.comparingDouble(node -> from.distSqr(node.toBlockPos())))
                      .orElse(null);
   }

   private Set<RouteWaypoint> collectNodesThatCanReachEnd(Graph<RouteWaypoint> graph, RouteWaypoint end) {
      Map<RouteWaypoint, Set<RouteWaypoint>> reverse = new HashMap<>();

      for (RouteWaypoint node : graph.map.keySet()) {
         reverse.putIfAbsent(node, new LinkedHashSet<>());
      }

      for (Map.Entry<RouteWaypoint, Set<RouteWaypoint>> entry : graph.map.entrySet()) {
         RouteWaypoint source = entry.getKey();
         for (RouteWaypoint target : entry.getValue()) {
            reverse.computeIfAbsent(target, ignored -> new LinkedHashSet<>()).add(source);
         }
      }

      Set<RouteWaypoint> visited = new HashSet<>();
      Deque<RouteWaypoint> queue = new ArrayDeque<>();
      queue.add(end);

      while (!queue.isEmpty()) {
         RouteWaypoint current = queue.poll();
         if (!visited.add(current)) {
            continue;
         }

         for (RouteWaypoint parent : reverse.getOrDefault(current, Collections.emptySet())) {
            if (!visited.contains(parent)) {
               queue.add(parent);
            }
         }
      }

      return visited;
   }
}
