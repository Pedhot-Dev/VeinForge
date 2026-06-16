package me.grish.veinforge.handler;

import com.google.gson.annotations.Expose;
import me.grish.veinforge.VeinForge;
import me.grish.veinforge.feature.route.GraphEditorFeature;
import me.grish.veinforge.feature.route.GraphOverlayRenderer;
import me.grish.veinforge.macro.AbstractMacro;
import me.grish.veinforge.route.graph.GraphPathfinder;
import me.grish.veinforge.route.graph.GraphRepository;
import me.grish.veinforge.util.KeyPressUtil;
import me.grish.veinforge.util.Logger;
import me.grish.veinforge.util.helper.graph.Graph;
import me.grish.veinforge.util.helper.route.RouteWaypoint;
import me.grish.veinforge.util.helper.route.WaypointType;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages graph-based waypoint editing and pathfinding.
 * Events are called from EventManager.
 */
public class GraphHandler {

   public static final GraphHandler instance = new GraphHandler();
   private static final long SAVE_DEBOUNCE_MS = 250L;

   @Expose
   private final Map<String, Graph<RouteWaypoint>> graphs = new HashMap<>();

   private final Object saveMonitor = new Object();
   private final Object graphLock = new Object();
   private final GraphPathfinder pathfinder = new GraphPathfinder();
   private final GraphRepository graphRepository = new GraphRepository();
   private final GraphEditorFeature graphEditorFeature = new GraphEditorFeature(pathfinder);
   private final GraphOverlayRenderer overlayRenderer = new GraphOverlayRenderer(graphLock, this::getGraphByKey);

   private volatile String activeGraphKey = "default";
   private volatile String debugGraphKey = "default";
   private volatile boolean editing = false;
   private volatile boolean debugRenderEnabled = false;
   private volatile boolean dirty = false;
   private boolean saveLoopRunning = false;
   private long changeSeq = 0;
   private long lastSavedSeq = 0;

   public Graph<RouteWaypoint> getCachedGraph() {
      return overlayRenderer.getCachedGraph();
   }

   public BlockPos getCachedHoveredBlock() {
      return overlayRenderer.getCachedHoveredBlock();
   }

   public RouteWaypoint getCachedHoveredWaypoint() {
      return overlayRenderer.getCachedHoveredWaypoint();
   }

   public BlockPos getCachedStandingBlock() {
      return overlayRenderer.getCachedStandingBlock();
   }

   public RouteWaypoint getLastPos() {
      return graphEditorFeature.getLastPos();
   }

   public String getActiveGraphKey() {
      return activeGraphKey;
   }

   public boolean isEditing() {
      return editing;
   }

   public boolean isDirty() {
      return dirty;
   }

   public boolean isDebugRenderEnabled() {
      return debugRenderEnabled;
   }

   public String getDebugGraphKey() {
      return debugGraphKey;
   }

   public WaypointType getEditorPlacementType() {
      return graphEditorFeature.getEditorPlacementType();
   }

   public List<String> getKnownGraphNames() {
      List<String> names;
      synchronized (graphLock) {
         names = new ArrayList<>(graphs.keySet());
      }
      names.sort(String::compareToIgnoreCase);
      return names;
   }

   public boolean hasGraphs() {
      synchronized (graphLock) {
         return !graphs.isEmpty();
      }
   }

   public boolean hasGraph(String graphName) {
      synchronized (graphLock) {
         return resolveExistingGraphKeyInternal(graphName) != null;
      }
   }

   public void putGraph(String graphName, Graph<RouteWaypoint> graph) {
      if (graph == null) {
         return;
      }
      String key = normalizeGraphName(graphName);
      if (key.isEmpty()) {
         return;
      }
      synchronized (graphLock) {
         graphs.put(key, graph);
      }
      markCacheDirty(key);
   }

   public List<String> getEditorControlHints() {
      var config = VeinForge.config();
      if (config == null) {
         return Collections.emptyList();
      }

      return Arrays.asList(
              "Place WALK node at current block: " + KeyPressUtil.getKeyName(config.routeMiner.routeBuilderGraphWalkNode),
              "Place ETHERWARP node at current block: " + KeyPressUtil.getKeyName(config.routeMiner.routeBuilderGraphEtherwarpNode),
              "Choose start node (where links begin): " + KeyPressUtil.getKeyName(config.routeMiner.routeBuilderSelect),
              "Create one-way link to your current block: " + KeyPressUtil.getKeyName(config.routeMiner.routeBuilderUnidi),
              "Create two-way link between nodes: " + KeyPressUtil.getKeyName(config.routeMiner.routeBuilderBidi),
              "Move chosen node to your current block: " + KeyPressUtil.getKeyName(config.routeMiner.routeBuilderMove),
              "Delete chosen node and its links: " + KeyPressUtil.getKeyName(config.routeMiner.routeBuilderDelete)
      );
   }

   public List<String> getEditorControlHelpLines() {
      var config = VeinForge.config();
      if (config == null) {
         return Collections.emptyList();
      }

      List<String> lines = new ArrayList<>();
      lines.add("Step 1: Stand on the node you want to edit.");
      lines.add("Step 2: Create/select a node type at your block:");
      lines.add(" - " + KeyPressUtil.getKeyName(config.routeMiner.routeBuilderGraphWalkNode) + " (WALK node)");
      lines.add(" - " + KeyPressUtil.getKeyName(config.routeMiner.routeBuilderGraphEtherwarpNode) + " (ETHERWARP node)");
      lines.add("Step 3: Press " + KeyPressUtil.getKeyName(config.routeMiner.routeBuilderSelect) + " to choose parent node.");
      lines.add("Step 4: Stand on a target node and press one of:");
      lines.add(" - " + KeyPressUtil.getKeyName(config.routeMiner.routeBuilderUnidi) + " (one-way link)");
      lines.add(" - " + KeyPressUtil.getKeyName(config.routeMiner.routeBuilderBidi) + " (two-way link)");
      lines.add("Move selected node: " + KeyPressUtil.getKeyName(config.routeMiner.routeBuilderMove));
      lines.add("Delete selected node: " + KeyPressUtil.getKeyName(config.routeMiner.routeBuilderDelete));
      lines.add("Save immediately: /graph save");
      return lines;
   }

   public boolean enableDebugRender(String graphName) {
      String key = resolveGraphKey(graphName);
      if (key == null || key.isEmpty()) {
         return false;
      }
      if (!hasGraph(key)) {
         return false;
      }

      debugGraphKey = key;
      debugRenderEnabled = true;
      markCacheDirty(key);
      return true;
   }

   public void disableDebugRender() {
      debugRenderEnabled = false;
   }

   private String getRenderGraphKey() {
      if (editing) {
         return activeGraphKey;
      }
      if (debugRenderEnabled) {
         return debugGraphKey;
      }
      return activeGraphKey;
   }

   private boolean shouldRenderGraphOverlay() {
      return editing || debugRenderEnabled;
   }

   public Graph<RouteWaypoint> getActiveGraph() {
      synchronized (graphLock) {
         return graphs.computeIfAbsent(activeGraphKey, k -> new Graph<>());
      }
   }

   public void switchGraph(String graphName) {
      String key = resolveGraphKey(graphName);
      if (key == null || key.isEmpty()) {
         key = normalizeGraphName(graphName);
      }
      if (key == null || key.isEmpty()) {
         return;
      }
      activeGraphKey = key;
      getActiveGraph();
      Logger.sendMessage("Switched to graph: " + key);
   }

   public void switchGraph(AbstractMacro macro) {
      if (macro == null) {
         return;
      }
      switchGraph(macro.getName());
   }

   public void toggleEdit(String graphName) {
      String key = resolveGraphKey(graphName);
      if (key == null || key.isEmpty() || !hasGraph(key)) {
         return;
      }
      activeGraphKey = key;
      if (editing) {
         stop();
      } else {
         start();
      }
      Logger.sendMessage(editing ? "Editing " + key : "Stopped Editing " + key);
      if (editing) {
         sendEditorControlsHint();
      }
   }

   public void toggleEdit() {
      if (editing) {
         stop();
      } else {
         start();
      }
      Logger.sendMessage(editing ? "Editing " + activeGraphKey : "Stopped Editing " + activeGraphKey);
      if (editing) {
         sendEditorControlsHint();
      }
   }

   public boolean createGraph(String graphName) {
      String normalized = normalizeGraphName(graphName);
      if (normalized.isEmpty()) {
         return false;
      }
      synchronized (graphLock) {
         if (resolveExistingGraphKeyInternal(normalized) != null) {
            return false;
         }
         graphs.put(normalized, new Graph<>());
      }
      markCacheDirty(normalized);
      return true;
   }

   public String resolveGraphKey(String graphName) {
      synchronized (graphLock) {
         String existing = resolveExistingGraphKeyInternal(graphName);
         if (existing != null) {
            return existing;
         }

         String normalized = normalizeGraphName(graphName);
         return normalized.isEmpty() ? null : normalized;
      }
   }

   private String resolveExistingGraphKeyInternal(String graphName) {
      String normalized = normalizeGraphName(graphName);
      if (normalized.isEmpty()) {
         return null;
      }

      if (graphs.containsKey(normalized)) {
         return normalized;
      }

      for (String key : graphs.keySet()) {
         if (normalizeGraphName(key).equalsIgnoreCase(normalized)) {
            return key;
         }
      }
      return null;
   }

   private static String normalizeGraphName(String graphName) {
      if (graphName == null) {
         return "";
      }
      return graphName.trim().replaceAll("\\s+", " ");
   }

   public void start() {
      editing = true;
      debugRenderEnabled = false;
      debugGraphKey = activeGraphKey;
      graphEditorFeature.resetPlacementType();
      synchronized (saveMonitor) {
         if (saveLoopRunning) {
            saveMonitor.notifyAll();
            return;
         }
         saveLoopRunning = true;
      }
      VeinForge.executor().execute(this::save);
   }

   public void stop() {
      editing = false;
      synchronized (saveMonitor) {
         saveMonitor.notifyAll();
      }
   }

   public double distance(String graphName, BlockPos start, RouteWaypoint end) {
      List<RouteWaypoint> route = findPathFrom(graphName, start, end);
      if (route.size() < 2) {
         return -1;
      }

      double distance = 0;
      for (int i = 0; i < route.size() - 1; i++) {
         distance += route.get(i).toBlockPos().distSqr(route.get(i + 1).toBlockPos());
      }

      return distance;
   }

   public List<RouteWaypoint> findPath(BlockPos start, RouteWaypoint end) {
      Graph<RouteWaypoint> graph = getActiveGraph();
      return pathfinder.findPathWithNearestReachableStart(graph, start, end);
   }

   public List<RouteWaypoint> findPath(RouteWaypoint first, RouteWaypoint second) {
      Graph<RouteWaypoint> graph = getActiveGraph();
      return pathfinder.findPathUsingExplicitStart(graph, first, second);
   }

   public List<RouteWaypoint> findPathFrom(String graphName, BlockPos start, RouteWaypoint end) {
      Graph<RouteWaypoint> graph = getGraphByKey(graphName);
      return pathfinder.findPathWithNearestReachableStart(graph, start, end);
   }

   public List<RouteWaypoint> findPathFrom(String graphName, RouteWaypoint first, RouteWaypoint second) {
      Graph<RouteWaypoint> graph = getGraphByKey(graphName);
      return pathfinder.findPathUsingExplicitStart(graph, first, second);
   }

   public void save() {
      try {
         while (true) {
            String graphKeyToSave;
            long seqToSave;

            synchronized (saveMonitor) {
               while (editing && changeSeq == lastSavedSeq) {
                  try {
                     saveMonitor.wait();
                  } catch (InterruptedException e) {
                     Thread.currentThread().interrupt();
                     return;
                  }
               }

               if (!editing && changeSeq == lastSavedSeq) {
                  return;
               }

               seqToSave = changeSeq;
               graphKeyToSave = activeGraphKey;

               long stableSince = System.currentTimeMillis();
               while (editing) {
                  long elapsed = System.currentTimeMillis() - stableSince;
                  long remaining = SAVE_DEBOUNCE_MS - elapsed;
                  if (remaining <= 0) {
                     break;
                  }
                  try {
                     saveMonitor.wait(remaining);
                  } catch (InterruptedException e) {
                     Thread.currentThread().interrupt();
                     return;
                  }

                  if (changeSeq != seqToSave) {
                     seqToSave = changeSeq;
                     graphKeyToSave = activeGraphKey;
                     stableSince = System.currentTimeMillis();
                  }
               }
            }

            boolean saved = writeGraphToDisk(graphKeyToSave);
            synchronized (saveMonitor) {
               if (saved) {
                  lastSavedSeq = Math.max(lastSavedSeq, seqToSave);
               }
               dirty = changeSeq != lastSavedSeq;
            }
         }
      } finally {
         synchronized (saveMonitor) {
            saveLoopRunning = false;
            saveMonitor.notifyAll();
         }
      }
   }

   private void markDirty() {
      synchronized (saveMonitor) {
         dirty = true;
         changeSeq++;
         saveMonitor.notifyAll();
      }
   }

   private void sendEditorControlsHint() {
      Logger.sendMessage("Graph Editor controls:");
      for (String hint : getEditorControlHelpLines()) {
         Logger.sendMessage(" - " + hint);
      }
      Logger.sendMessage(" - Full help: /graph keys");
   }

   private boolean writeGraphToDisk(String graphKey) {
      return graphRepository.writeGraphToDisk(graphKey, snapshotGraph(graphKey));
   }

   private Graph<RouteWaypoint> snapshotGraph(String graphKey) {
      if (graphKey == null || graphKey.isEmpty()) {
         return null;
      }
      synchronized (graphLock) {
         Graph<RouteWaypoint> graph = graphs.get(graphKey);
         if (graph == null) {
            return null;
         }

         Graph<RouteWaypoint> snapshot = new Graph<>();
         for (Map.Entry<RouteWaypoint, Set<RouteWaypoint>> entry : graph.map.entrySet()) {
            // Preserve stable iteration order for reproducible JSON output.
            snapshot.map.put(entry.getKey(), new LinkedHashSet<>(entry.getValue()));
         }
         return snapshot;
      }
   }

   public void saveNow() {
      final String graphKey = activeGraphKey;
      final long seqAtRequest;
      synchronized (saveMonitor) {
         seqAtRequest = changeSeq;
      }
      VeinForge.executor().execute(() -> {
         boolean saved = writeGraphToDisk(graphKey);
         if (!saved) {
            return;
         }
         synchronized (saveMonitor) {
            if (changeSeq == seqAtRequest) {
               lastSavedSeq = Math.max(lastSavedSeq, seqAtRequest);
            }
            dirty = changeSeq != lastSavedSeq;
         }
      });
   }

   /**
    * Called for key input checks.
    */
   public void onInput() {
      boolean changed = graphEditorFeature.onInput(editing, this::getActiveGraph, graphLock);
      if (changed) {
         markCacheDirty(activeGraphKey);
         markDirty();
      }
   }

   /**
    * Called every tick to update caches.
    */
   public void onTick() {
      overlayRenderer.onTick(
              shouldRenderGraphOverlay(),
              editing,
              getRenderGraphKey(),
              this::getActiveGraph,
              graphEditorFeature.getEditorPlacementType(),
              graphEditorFeature.getLastPos()
      );
   }

   /**
    * Called for world rendering.
    */
   public void onWorldRender(LevelRenderContext context) {
      overlayRenderer.onWorldRender(
              context,
              shouldRenderGraphOverlay(),
              editing,
              getRenderGraphKey(),
              this::getActiveGraph,
              graphEditorFeature.getLastPos()
      );
   }

   /**
    * Called for HUD rendering.
    */
    public void onHudRender(GuiGraphicsExtractor drawContext) {
      // Deprecated: moved to RouteBuilderHUD (movable + consistent panel styling).
   }

   public int getIncomingEdgeCount(Graph<RouteWaypoint> graph, RouteWaypoint target) {
      return overlayRenderer.getIncomingEdgeCount(graph, target, getRenderGraphKey());
   }

   private void markCacheDirty(String graphKey) {
      overlayRenderer.markCacheDirty(graphKey);
   }

   private Graph<RouteWaypoint> getGraphByKey(String graphKey) {
      if (graphKey == null || graphKey.isEmpty()) {
         return null;
      }
      synchronized (graphLock) {
         return graphs.get(graphKey);
      }
   }
}
