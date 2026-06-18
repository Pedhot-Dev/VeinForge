package me.grish.veinforge.feature.route;

import me.grish.veinforge.util.PlayerUtil;
import me.grish.veinforge.util.RenderUtil;
import me.grish.veinforge.util.helper.graph.Graph;
import me.grish.veinforge.util.helper.route.RouteWaypoint;
import me.grish.veinforge.util.helper.route.WaypointType;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class GraphOverlayRenderer {

    private static final int GRAPH_RENDER_RADIUS_BLOCKS = 96;
    private static final double GRAPH_RENDER_RADIUS_SQ = (double) GRAPH_RENDER_RADIUS_BLOCKS * (double) GRAPH_RENDER_RADIUS_BLOCKS;
    private static final int MAX_RENDER_NODES = 1200;
    private static final int MAX_RENDER_EDGES = 900;
    private static final int MAX_RENDER_FOCUS_EDGE_ARROWS = 240;
    private static final double EDGE_RENDER_Y_OFFSET = 1.12D;
    private static final Color COLOR_NODE_DEFAULT = new Color(120, 235, 140, 185);
    private static final Color COLOR_NODE_FOCUS = new Color(235, 75, 75, 220);
    private static final Color COLOR_EDGE_ONE_WAY = new Color(138, 255, 88, 255);
    private static final Color COLOR_EDGE_TWO_WAY = new Color(70, 205, 65, 255);
    private static final Color COLOR_EDGE_ARROW = new Color(225, 245, 255, 220);
    private static final Color COLOR_STANDING = new Color(70, 180, 120, 200);
    private static final Color COLOR_HOVERED = new Color(120, 220, 240, 220);

    private final Minecraft mc = Minecraft.getInstance();
    private final Object graphLock;
    private final Function<String, Graph<RouteWaypoint>> graphLookup;
    private final Map<String, Map<BlockPos, List<RouteWaypoint>>> waypointIndex = new HashMap<>();
    private final Map<String, Map<RouteWaypoint, Integer>> incomingEdgeCounts = new HashMap<>();
    private final Map<String, GraphRenderCache> renderCache = new HashMap<>();
    private final Set<String> cacheDirty = new HashSet<>();

    private Graph<RouteWaypoint> cachedGraph;
    private BlockPos cachedHoveredBlock;
    private RouteWaypoint cachedHoveredWaypoint;
    private BlockPos cachedStandingBlock;

    public GraphOverlayRenderer(Object graphLock, Function<String, Graph<RouteWaypoint>> graphLookup) {
        this.graphLock = graphLock;
        this.graphLookup = graphLookup;
    }

    private static Vec3 midpoint(Vec3 a, Vec3 b) {
        return a.add(b).scale(0.5);
    }

    private static double distanceToBlockCenterSq(BlockPos pos, Vec3 point) {
        double dx = (pos.getX() + 0.5) - point.x;
        double dy = (pos.getY() + 0.5) - point.y;
        double dz = (pos.getZ() + 0.5) - point.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public Graph<RouteWaypoint> getCachedGraph() {
        return cachedGraph;
    }

    public BlockPos getCachedHoveredBlock() {
        return cachedHoveredBlock;
    }

    public RouteWaypoint getCachedHoveredWaypoint() {
        return cachedHoveredWaypoint;
    }

    public BlockPos getCachedStandingBlock() {
        return cachedStandingBlock;
    }

    public void markCacheDirty(String graphKey) {
        cacheDirty.add(graphKey);
    }

    public void onTick(
            boolean shouldRenderGraphOverlay,
            boolean editing,
            String renderGraphKey,
            Supplier<Graph<RouteWaypoint>> activeGraphSupplier,
            WaypointType editorPlacementType,
            RouteWaypoint lastPos
    ) {
        if (!shouldRenderGraphOverlay || mc.player == null) {
            cachedGraph = null;
            cachedHoveredBlock = null;
            cachedHoveredWaypoint = null;
            cachedStandingBlock = null;
            return;
        }

        cachedGraph = editing ? activeGraphSupplier.get() : lookupGraph(renderGraphKey);
        if (cachedGraph == null) {
            cachedHoveredBlock = null;
            cachedHoveredWaypoint = null;
            cachedStandingBlock = null;
            return;
        }

        cachedHoveredBlock = getHoveredBlock();
        cachedStandingBlock = PlayerUtil.getBlockStandingOn();
        cachedHoveredWaypoint = cachedHoveredBlock == null
                ? null
                : getWaypointAt(cachedGraph, cachedHoveredBlock, renderGraphKey, editing, editorPlacementType, lastPos);
    }

    public void onWorldRender(
            LevelRenderContext context,
            boolean shouldRenderGraphOverlay,
            boolean editing,
            String renderGraphKey,
            Supplier<Graph<RouteWaypoint>> activeGraphSupplier,
            RouteWaypoint lastPos
    ) {
        if (!shouldRenderGraphOverlay || mc.player == null) {
            return;
        }

        Graph<RouteWaypoint> graph = cachedGraph;
        if (graph == null) {
            graph = editing ? activeGraphSupplier.get() : lookupGraph(renderGraphKey);
        }
        RouteWaypoint hoveredWaypoint = cachedHoveredWaypoint;
        BlockPos standingBlock = cachedStandingBlock;
        if (graph == null) {
            return;
        }

        ensureCache(graph, renderGraphKey);
        GraphRenderCache cache = renderCache.get(renderGraphKey);
        if (cache == null) {
            return;
        }

        BlockPos reference = standingBlock != null ? standingBlock : mc.player.blockPosition();
        RouteWaypoint focus = hoveredWaypoint != null ? hoveredWaypoint : (editing ? lastPos : null);
        if (focus == null && standingBlock != null) {
            focus = getClosestWaypoint(cache, standingBlock);
        }

        int renderedNodes = 0;
        for (NodeRender node : cache.nodes) {
            if (reference.distSqr(node.blockPos) > GRAPH_RENDER_RADIUS_SQ) {
                continue;
            }

            boolean isFocus = focus != null && focus.equals(node.waypoint);
            RenderUtil.drawBlock(node.blockPos, isFocus ? COLOR_NODE_FOCUS : COLOR_NODE_DEFAULT);
            renderedNodes++;
            if (renderedNodes >= MAX_RENDER_NODES) {
                break;
            }
        }

        int renderedEdges = 0;
        int arrowBudget = MAX_RENDER_FOCUS_EDGE_ARROWS;
        for (EdgeRender edge : cache.edges) {
            if (reference.distSqr(edge.fromPos) > GRAPH_RENDER_RADIUS_SQ
                    && reference.distSqr(edge.toPos) > GRAPH_RENDER_RADIUS_SQ
                    && distanceToBlockCenterSq(reference, edge.midCenter) > GRAPH_RENDER_RADIUS_SQ) {
                continue;
            }

            boolean outgoingFocus = focus != null && focus.equals(edge.fromWaypoint);
            Color edgeColor = edge.bidirectional ? COLOR_EDGE_TWO_WAY : COLOR_EDGE_ONE_WAY;

            RenderUtil.drawThinLine(edge.fromCenter, edge.toCenter, edgeColor, true);

            if (!edge.bidirectional && outgoingFocus && arrowBudget > 0) {
                drawArrowHead(edge.fromCenter, edge.toCenter, COLOR_EDGE_ARROW);
                arrowBudget--;
            }
            renderedEdges++;
            if (renderedEdges >= MAX_RENDER_EDGES) {
                break;
            }
        }

        if (standingBlock != null) {
            RenderUtil.outlineBlock(standingBlock, COLOR_STANDING);
        }
        if (hoveredWaypoint != null) {
            RenderUtil.outlineBlock(new BlockPos(hoveredWaypoint.getX(), hoveredWaypoint.getY(), hoveredWaypoint.getZ()), COLOR_HOVERED);
        }
        if (lastPos != null) {
            RenderUtil.drawBlock(new BlockPos(lastPos.getX(), lastPos.getY(), lastPos.getZ()), COLOR_NODE_FOCUS);
        }
    }

    public int getIncomingEdgeCount(Graph<RouteWaypoint> graph, RouteWaypoint target, String renderGraphKey) {
        if (target == null || renderGraphKey == null || renderGraphKey.isEmpty()) {
            return 0;
        }
        ensureCache(graph, renderGraphKey);
        Map<RouteWaypoint, Integer> counts = incomingEdgeCounts.get(renderGraphKey);
        if (counts == null) {
            return 0;
        }
        return counts.getOrDefault(target, 0);
    }

    private Graph<RouteWaypoint> lookupGraph(String graphKey) {
        if (graphLookup == null || graphKey == null || graphKey.isEmpty()) {
            return null;
        }
        return graphLookup.apply(graphKey);
    }

    private BlockPos getHoveredBlock() {
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        return ((BlockHitResult) mc.hitResult).getBlockPos();
    }

    private RouteWaypoint getWaypointAt(
            Graph<RouteWaypoint> graph,
            BlockPos pos,
            String graphKey,
            boolean editing,
            WaypointType editorPlacementType,
            RouteWaypoint lastPos
    ) {
        if (pos == null) {
            return null;
        }
        ensureCache(graph, graphKey);
        Map<BlockPos, List<RouteWaypoint>> index = waypointIndex.get(graphKey);
        if (index == null) {
            return null;
        }
        List<RouteWaypoint> atPos = index.get(pos);
        return selectWaypointForPosition(atPos, pos, editing, editorPlacementType, lastPos);
    }

    private RouteWaypoint selectWaypointForPosition(
            List<RouteWaypoint> candidates,
            BlockPos pos,
            boolean editing,
            WaypointType editorPlacementType,
            RouteWaypoint lastPos
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        if (lastPos != null
                && pos != null
                && lastPos.getX() == pos.getX()
                && lastPos.getY() == pos.getY()
                && lastPos.getZ() == pos.getZ()
                && candidates.contains(lastPos)) {
            return lastPos;
        }

        WaypointType preferred = editing ? editorPlacementType : WaypointType.WALK;
        if (preferred == null) {
            preferred = WaypointType.WALK;
        }

        for (RouteWaypoint waypoint : candidates) {
            if (waypoint.getTransportMethod() == preferred) {
                return waypoint;
            }
        }
        for (RouteWaypoint waypoint : candidates) {
            if (waypoint.getTransportMethod() == WaypointType.WALK) {
                return waypoint;
            }
        }
        return candidates.get(0);
    }

    private void ensureCache(Graph<RouteWaypoint> graph, String graphKey) {
        if (graph == null || graphKey == null || graphKey.isEmpty()) {
            return;
        }
        if (!cacheDirty.contains(graphKey)
                && waypointIndex.containsKey(graphKey)
                && incomingEdgeCounts.containsKey(graphKey)
                && renderCache.containsKey(graphKey)) {
            return;
        }

        Map<BlockPos, List<RouteWaypoint>> index = new HashMap<>();
        Map<RouteWaypoint, Integer> incoming = new HashMap<>();
        List<NodeRender> nodes = new ArrayList<>();
        List<EdgeRender> edges = new ArrayList<>();

        synchronized (graphLock) {
            Map<RouteWaypoint, NodeRender> nodeByWaypoint = new HashMap<>();

            for (RouteWaypoint waypoint : graph.map.keySet()) {
                BlockPos pos = new BlockPos(waypoint.getX(), waypoint.getY(), waypoint.getZ());
                Vec3 center = new Vec3(
                        waypoint.getX() + 0.5,
                        waypoint.getY() + EDGE_RENDER_Y_OFFSET,
                        waypoint.getZ() + 0.5
                );
                NodeRender render = new NodeRender(waypoint, pos, center);
                nodeByWaypoint.put(waypoint, render);
                nodes.add(render);

                index.computeIfAbsent(pos, ignored -> new ArrayList<>()).add(waypoint);
                incoming.putIfAbsent(waypoint, 0);
            }

            for (Map.Entry<RouteWaypoint, Set<RouteWaypoint>> entry : graph.map.entrySet()) {
                NodeRender fromNode = nodeByWaypoint.get(entry.getKey());
                if (fromNode == null) {
                    continue;
                }
                for (RouteWaypoint neighbor : entry.getValue()) {
                    incoming.merge(neighbor, 1, Integer::sum);
                    NodeRender toNode = nodeByWaypoint.get(neighbor);
                    if (toNode == null) {
                        BlockPos pos = new BlockPos(neighbor.getX(), neighbor.getY(), neighbor.getZ());
                        Vec3 center = new Vec3(
                                neighbor.getX() + 0.5,
                                neighbor.getY() + EDGE_RENDER_Y_OFFSET,
                                neighbor.getZ() + 0.5
                        );
                        toNode = new NodeRender(neighbor, pos, center);
                        nodeByWaypoint.put(neighbor, toNode);
                        nodes.add(toNode);
                        index.computeIfAbsent(pos, ignored -> new ArrayList<>()).add(neighbor);
                        incoming.putIfAbsent(neighbor, 0);
                    }
                    boolean bidirectional = graph.map.getOrDefault(neighbor, Collections.emptySet()).contains(entry.getKey());
                    edges.add(new EdgeRender(
                            entry.getKey(),
                            neighbor,
                            bidirectional,
                            fromNode.blockPos,
                            toNode.blockPos,
                            midpoint(fromNode.center, toNode.center),
                            fromNode.center,
                            toNode.center
                    ));
                }
            }
        }

        waypointIndex.put(graphKey, index);
        incomingEdgeCounts.put(graphKey, incoming);
        renderCache.put(graphKey, new GraphRenderCache(nodes, edges));
        cacheDirty.remove(graphKey);
    }

    private RouteWaypoint getClosestWaypoint(GraphRenderCache cache, BlockPos from) {
        if (cache == null || from == null || cache.nodes.isEmpty()) {
            return null;
        }

        RouteWaypoint nearest = null;
        double best = Double.MAX_VALUE;
        for (NodeRender node : cache.nodes) {
            double dist = from.distSqr(node.blockPos);
            if (dist < best) {
                best = dist;
                nearest = node.waypoint;
            }
        }
        return nearest;
    }

    private void drawArrowHead(Vec3 from, Vec3 to, Color color) {
        Vec3 dir = to.subtract(from);
        if (dir.lengthSqr() < 1.0E-6) {
            return;
        }

        Vec3 forward = dir.normalize();
        Vec3 side = forward.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1.0E-6) {
            side = forward.cross(new Vec3(1, 0, 0));
        }
        side = side.normalize();

        Vec3 tip = to.add(forward.scale(-0.08));
        Vec3 back = forward.scale(-0.38);
        Vec3 wingA = tip.add(back).add(side.scale(0.16));
        Vec3 wingB = tip.add(back).add(side.scale(-0.16));

        RenderUtil.drawThinLine(tip, wingA, color, true);
        RenderUtil.drawThinLine(tip, wingB, color, true);
    }

    private record NodeRender(RouteWaypoint waypoint, BlockPos blockPos, Vec3 center) {
    }

    private record EdgeRender(
            RouteWaypoint fromWaypoint,
            RouteWaypoint toWaypoint,
            boolean bidirectional,
            BlockPos fromPos,
            BlockPos toPos,
            Vec3 midCenter,
            Vec3 fromCenter,
            Vec3 toCenter
    ) {
    }

    private record GraphRenderCache(List<NodeRender> nodes, List<EdgeRender> edges) {
    }
}
