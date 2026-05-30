package me.grish.veinforge.util.helper.graph;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import me.grish.veinforge.VeinForge;
import me.grish.veinforge.util.helper.route.RouteWaypoint;
import me.grish.veinforge.util.helper.route.WaypointType;

import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;

public class GraphSerializer implements JsonSerializer<Graph<RouteWaypoint>>, JsonDeserializer<Graph<RouteWaypoint>> {

   @Override
   public JsonElement serialize(Graph<RouteWaypoint> src, Type typeOfSrc, JsonSerializationContext context) {
      JsonObject res = new JsonObject();
      JsonObject map = new JsonObject();

      for (Entry<RouteWaypoint, Set<RouteWaypoint>> entry : src.map.entrySet()) {
         RouteWaypoint waypoint = entry.getKey();
         String keyString = waypoint.getX() + "," + waypoint.getY() + "," + waypoint.getZ() + "," + waypoint.getTransportMethod().name();

         JsonElement valueElement = context.serialize(entry.getValue());
         map.add(keyString, valueElement);
      }

      res.add("map", map);
      return res;
   }

   @Override
   @SuppressWarnings("unchecked")
   public Graph<RouteWaypoint> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
      Graph<RouteWaypoint> graph = new Graph<>();
      JsonObject map = json.getAsJsonObject().getAsJsonObject("map");

      for (Entry<String, JsonElement> entry : map.entrySet()) {
         try {
            // The key is a string like "33,119,419,WALK" so we need to manually parse it into RouteWaypoint
            String[] keyParts = entry.getKey().split(",");
            if (keyParts.length != 4) {
               throw new JsonParseException("Invalid RouteWaypoint key format: " + entry.getKey());
            }

            int x = Integer.parseInt(keyParts[0]);
            int y = Integer.parseInt(keyParts[1]);
            int z = Integer.parseInt(keyParts[2]);
            WaypointType transportMethod = WaypointType.valueOf(keyParts[3]);
            RouteWaypoint key = new RouteWaypoint(x, y, z, transportMethod);

            Set<RouteWaypoint> value = context.deserialize(entry.getValue(), new TypeToken<Set<RouteWaypoint>>() {
            }.getType());

            graph.map.put(key, value == null ? new LinkedHashSet<>() : new LinkedHashSet<>(value));
         } catch (JsonParseException | NumberFormatException e) {
            VeinForge.LOGGER.warn("Error deserializing entry for key: {}", entry.getKey(), e);
         }
      }

      return graph;
   }
}
