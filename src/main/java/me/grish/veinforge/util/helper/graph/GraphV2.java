package me.grish.veinforge.util.helper.graph;

import com.google.gson.annotations.Expose;
import me.grish.veinforge.util.helper.route.RouteWaypoint;
import me.grish.veinforge.util.helper.route.WaypointType;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class GraphV2 {

   @Expose
   private final int schemaVersion = 2;
   @Expose
   private GraphMetadata metadata;
   @Expose
   private List<GraphNode> nodes;
   @Expose
   private List<GraphEdge> edges;
   @Expose
   private GraphStats stats;

   public GraphV2() {
   }

   public static GraphV2 fromGraph(Graph<RouteWaypoint> graph, String graphName, boolean directed) {
      GraphValidationResult validation = GraphValidator.validateStrict(graph);
      GraphV2 v2 = new GraphV2();
      v2.metadata = GraphMetadata.create(graphName, directed);
      v2.nodes = graph.map.keySet().stream()
                         .map(waypoint -> new GraphNode(waypoint))
                         .sorted(Comparator.comparing(GraphNode::getId))
                         .collect(Collectors.toList());

      List<GraphEdge> edgeList = new ArrayList<>();
      for (Map.Entry<RouteWaypoint, Set<RouteWaypoint>> entry : graph.map.entrySet()) {
         for (RouteWaypoint neighbor : entry.getValue()) {
            edgeList.add(new GraphEdge(entry.getKey().toString(), neighbor.toString()));
         }
      }
      edgeList.sort(Comparator.comparing(GraphEdge::getFrom).thenComparing(GraphEdge::getTo));
      v2.edges = edgeList;

      v2.stats = new GraphStats(
              validation.nodeCount(),
              validation.edgeCount(),
              0,
              0,
              validation.danglingEdges()
      );
      return v2;
   }

   public Graph<RouteWaypoint> toGraphStrict() {
      if (schemaVersion != 2) {
         throw new IllegalStateException("Unsupported graph schema version: " + schemaVersion);
      }
      if (nodes == null || edges == null) {
         throw new IllegalStateException("Graph nodes and edges must be provided");
      }

      Map<String, RouteWaypoint> nodesById = new HashMap<>();
      for (GraphNode node : nodes) {
         GraphNodeId parsed = GraphNodeId.parse(node.getId());
         if (node.getX() != parsed.x || node.getY() != parsed.y || node.getZ() != parsed.z) {
            throw new IllegalStateException("Node id does not match coordinates: " + node.getId());
         }
         if (!node.getTransport().equals(parsed.transport.name())) {
            throw new IllegalStateException("Node id transport mismatch: " + node.getId());
         }
         if (nodesById.containsKey(node.getId())) {
            throw new IllegalStateException("Duplicate node id: " + node.getId());
         }
         nodesById.put(node.getId(), new RouteWaypoint(node.getX(), node.getY(), node.getZ(), parsed.transport));
      }

      Graph<RouteWaypoint> graph = new Graph<>();
      for (RouteWaypoint waypoint : nodesById.values()) {
         graph.add(waypoint);
      }

      Set<String> edgeKeys = new HashSet<>();
      for (GraphEdge edge : edges) {
         if (!nodesById.containsKey(edge.getFrom()) || !nodesById.containsKey(edge.getTo())) {
            throw new IllegalStateException("Dangling edge: " + edge.getFrom() + " -> " + edge.getTo());
         }
         if (edge.getFrom().equals(edge.getTo())) {
            throw new IllegalStateException("Self-loop edge: " + edge.getFrom());
         }
         String key = edge.getFrom() + "->" + edge.getTo();
         if (!edgeKeys.add(key)) {
            throw new IllegalStateException("Duplicate edge: " + key);
         }
         graph.add(nodesById.get(edge.getFrom()), nodesById.get(edge.getTo()), false);
      }

      GraphValidator.validateStrict(graph);
      return graph;
   }

   public int getSchemaVersion() {
      return schemaVersion;
   }

   public GraphMetadata getMetadata() {
      return metadata;
   }

   public List<GraphNode> getNodes() {
      return nodes;
   }

   public List<GraphEdge> getEdges() {
      return edges;
   }

   public GraphStats getStats() {
      return stats;
   }

   private record GraphNodeId(int x, int y, int z, WaypointType transport) {

      private static GraphNodeId parse(String id) {
         if (id == null) {
            throw new IllegalStateException("Node id cannot be null");
         }
         String[] parts = id.split(",");
         if (parts.length != 4) {
            throw new IllegalStateException("Invalid node id: " + id);
         }
         try {
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            WaypointType transport = WaypointType.valueOf(parts[3]);
            return new GraphNodeId(x, y, z, transport);
         } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid node id: " + id, e);
         }
      }
   }

   public static class GraphMetadata {
      @Expose
      private String graphName;
      @Expose
      private String world;
      @Expose
      private String generatedAt;
      @Expose
      private String generator;
      @Expose
      private boolean directed;
      @Expose
      private List<String> transportEnum;

      public GraphMetadata() {
      }

      public static GraphMetadata create(String graphName, boolean directed) {
         GraphMetadata metadata = new GraphMetadata();
         metadata.graphName = graphName;
         metadata.world = "unknown";
         metadata.generatedAt = Instant.now().toString();
         metadata.generator = "RouteBuilder";
         metadata.directed = directed;
         metadata.transportEnum = new ArrayList<>();
         for (WaypointType type : WaypointType.values()) {
            metadata.transportEnum.add(type.name());
         }
         return metadata;
      }

      public String getGraphName() {
         return graphName;
      }

      public String getWorld() {
         return world;
      }

      public String getGeneratedAt() {
         return generatedAt;
      }

      public String getGenerator() {
         return generator;
      }

      public boolean isDirected() {
         return directed;
      }

      public List<String> getTransportEnum() {
         return transportEnum;
      }
   }

   public static class GraphNode {
      @Expose
      private String id;
      @Expose
      private int x;
      @Expose
      private int y;
      @Expose
      private int z;
      @Expose
      private String transport;

      public GraphNode() {
      }

      public GraphNode(RouteWaypoint waypoint) {
         this.id = waypoint.toString();
         this.x = waypoint.getX();
         this.y = waypoint.getY();
         this.z = waypoint.getZ();
         this.transport = waypoint.getTransportMethod().name();
      }

      public String getId() {
         return id;
      }

      public int getX() {
         return x;
      }

      public int getY() {
         return y;
      }

      public int getZ() {
         return z;
      }

      public String getTransport() {
         return transport;
      }
   }

   public static class GraphEdge {
      @Expose
      private String from;
      @Expose
      private String to;

      public GraphEdge() {
      }

      public GraphEdge(String from, String to) {
         this.from = from;
         this.to = to;
      }

      public String getFrom() {
         return from;
      }

      public String getTo() {
         return to;
      }
   }

   public static class GraphStats {
      @Expose
      private int nodeCount;
      @Expose
      private int edgeCount;
      @Expose
      private int duplicateEdgesRemoved;
      @Expose
      private int selfLoopsRemoved;
      @Expose
      private int danglingEdgesFound;

      public GraphStats() {
      }

      public GraphStats(int nodeCount, int edgeCount, int duplicateEdgesRemoved, int selfLoopsRemoved, int danglingEdgesFound) {
         this.nodeCount = nodeCount;
         this.edgeCount = edgeCount;
         this.duplicateEdgesRemoved = duplicateEdgesRemoved;
         this.selfLoopsRemoved = selfLoopsRemoved;
         this.danglingEdgesFound = danglingEdgesFound;
      }

      public int getNodeCount() {
         return nodeCount;
      }

      public int getEdgeCount() {
         return edgeCount;
      }

      public int getDuplicateEdgesRemoved() {
         return duplicateEdgesRemoved;
      }

      public int getSelfLoopsRemoved() {
         return selfLoopsRemoved;
      }

      public int getDanglingEdgesFound() {
         return danglingEdgesFound;
      }
   }
}
