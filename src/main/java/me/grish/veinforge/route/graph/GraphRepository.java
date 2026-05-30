package me.grish.veinforge.route.graph;

import me.grish.veinforge.VeinForge;
import me.grish.veinforge.util.Logger;
import me.grish.veinforge.util.helper.graph.Graph;
import me.grish.veinforge.util.helper.graph.GraphV2;
import me.grish.veinforge.util.helper.route.RouteWaypoint;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class GraphRepository {

   private final Object fileWriteLock = new Object();

   public boolean writeGraphToDisk(String graphKey, Graph<RouteWaypoint> snapshot) {
      if (graphKey == null || graphKey.isEmpty() || snapshot == null) {
         return false;
      }

      final GraphV2 graphV2 = GraphV2.fromGraph(snapshot, graphKey, true);

      synchronized (fileWriteLock) {
         try (BufferedWriter writer = Files.newBufferedWriter(
                 VeinForge.routesDirectory.resolve(graphKey + ".json"),
                 StandardCharsets.UTF_8
         )) {
            writer.write(VeinForge.gson.toJson(graphV2));
            Logger.sendLog("Saved graph: " + graphKey);
            return true;
         } catch (Exception e) {
            Logger.sendLog("Failed to save graph: " + graphKey);
            return false;
         }
      }
   }
}
