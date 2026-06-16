package me.grish.veinforge.handler;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.Expose;
import lombok.Getter;
import me.grish.veinforge.VeinForge;
import me.grish.veinforge.VeinForgeClient;
import me.grish.veinforge.feature.impl.RouteBuilder;
import me.grish.veinforge.util.Logger;
import me.grish.veinforge.util.helper.route.Route;
import me.grish.veinforge.util.helper.route.RouteWaypoint;
import me.grish.veinforge.util.helper.route.WaypointType;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@Getter
public class RouteHandler {
   private static final long SAVE_DEBOUNCE_MS = 250L;
   public static RouteHandler instance;
   private final Object saveLock = new Object();
   @Expose
   private final HashMap<String, Route> routes = new HashMap<String, Route>() {{
      put("Default", new Route());
   }};
   private Route selectedRoute = this.routes.get("Default");
   private volatile boolean dirty = false;
   private long lastDirtyAtMs = 0L;

   public static RouteHandler getInstance() {
      if (instance == null) {
         instance = new RouteHandler();
      }
      return instance;
   }

   public void onWorldRender(LevelRenderContext context) {
      boolean shouldRender = isRouteRenderActive();
      if (!shouldRender || selectedRoute == null || selectedRoute.isEmpty()) {
         return;
      }

      selectedRoute.drawRoute();
   }

   public void selectRoute(String routeName) {
      String normalized = normalizeRouteName(routeName);
      if (normalized.isEmpty()) {
         return;
      }

      String resolved = resolveExistingRouteKey(normalized);
      String targetKey = resolved != null ? resolved : normalized;
      if (!this.routes.containsKey(targetKey)) {
         this.createRoute(targetKey);
      }
      this.selectedRoute = routes.get(targetKey);
      this.markDirty();
   }

   public boolean createRoute(String routeName) {
      String normalized = normalizeRouteName(routeName);
      if (normalized.isEmpty()) return false;
      if (resolveExistingRouteKey(normalized) != null) return false;
      this.routes.put(normalized, new Route());
      this.markDirty();
      return true;
   }

   public boolean addToCurrentRoute(final BlockPos block, final WaypointType method) {
      if (this.selectedRoute == this.routes.get("Default")) {
         Logger.sendError("Cannot edit Default route. Use /rb new <name> and then /rb select <name>.");
         return false;
      }

      if (block == null) {
         Logger.sendError("Cannot add waypoint because your standing block could not be resolved.");
         return false;
      }

      final RouteWaypoint waypoint = new RouteWaypoint(block, method);
      if (this.selectedRoute.indexOf(waypoint) != -1) {
         return false;
      }

      this.selectedRoute.insert(waypoint);
      this.markDirty();
      return true;
   }

   public void removeFromCurrentRoute(final int index) {
      this.selectedRoute.remove(index);
      this.markDirty();
   }

   public void replaceInCurrentRoute(final int index, final RouteWaypoint waypoint) {
      this.selectedRoute.replace(index, waypoint);
      this.markDirty();
   }

   public void deleteRoute(final String routeName) {
      String resolved = resolveExistingRouteKey(routeName);
      if (resolved == null) {
         return;
      }

      if (this.selectedRoute == this.routes.remove(resolved)) {
         this.selectedRoute = this.routes.get("Default");
         VeinForge.config().routeMiner.selectedRoute = "";
         VeinForgeClient.configManager.saveConfig();
      }

      this.markDirty();
   }

   private String resolveExistingRouteKey(String routeName) {
      if (routeName == null) {
         return null;
      }
      String normalized = normalizeRouteName(routeName);
      if (normalized.isEmpty()) {
         return null;
      }

      if (this.routes.containsKey(normalized)) {
         return normalized;
      }

      for (String key : this.routes.keySet()) {
         if (normalizeRouteName(key).equalsIgnoreCase(normalized)) {
            return key;
         }
      }

      return null;
   }

   private static String normalizeRouteName(String routeName) {
      if (routeName == null) {
         return "";
      }
      return routeName.trim().replaceAll("\\s+", " ");
   }

   public boolean hasRoute(String routeName) {
      return resolveExistingRouteKey(routeName) != null;
   }

   public int getRouteSize(String routeName) {
      String resolved = resolveExistingRouteKey(routeName);
      if (resolved == null) {
         return 0;
      }

      Route route = this.routes.get(resolved);
      return route == null ? 0 : route.size();
   }

   public String getSelectedRouteName() {
      for (Map.Entry<String, Route> entry : this.routes.entrySet()) {
         if (entry.getValue() == this.selectedRoute) {
            return entry.getKey();
         }
      }

      if (VeinForge.config() != null) {
         String configured = normalizeRouteName(VeinForge.config().routeMiner.selectedRoute);
         if (!configured.isEmpty()) {
            return configured;
         }
      }

      return "Default";
   }

   public void markDirty() {
      synchronized (saveLock) {
         this.dirty = true;
         this.lastDirtyAtMs = System.currentTimeMillis();
         saveLock.notifyAll();
      }
   }

   public void loadData() {
      if (!Files.exists(VeinForge.routesFile)) {
         ensureDefaultRoutePresent();
         rebindSelectedRouteFromConfig();
         return;
      }

      try (Reader reader = Files.newBufferedReader(VeinForge.routesFile)) {
         JsonElement root = JsonParser.parseReader(reader);
         if (root == null || !root.isJsonObject()) {
            throw new IllegalStateException("routes.json root must be a JSON object");
         }
         JsonObject jsonObject = root.getAsJsonObject();
         JsonElement routesElement = jsonObject.has("routes")
                                     ? jsonObject.get("routes")
                                     : jsonObject;
         if (routesElement == null || routesElement.isJsonNull()) {
            throw new IllegalStateException("routes.json does not contain routes");
         }

         HashMap<String, Route> loadedRoutes = VeinForge.gson.fromJson(
                 routesElement,
                 new TypeToken<HashMap<String, Route>>() {
                 }.getType()
         );
         if (loadedRoutes == null) {
            throw new IllegalStateException("routes.json contained null routes");
         }

         routes.clear();
         routes.putAll(loadedRoutes);
      } catch (Exception e) {
         Logger.sendWarning("Failed to load routes: " + VeinForge.routesFile);
         VeinForge.LOGGER.error("Failed to load routes: {}", VeinForge.routesFile, e);
      } finally {
         ensureDefaultRoutePresent();
         rebindSelectedRouteFromConfig();
      }
   }

   public synchronized void saveData() {
      while (RouteBuilder.getInstance().isRunning()) {
         try {
            boolean shouldSave;
            synchronized (saveLock) {
               while (RouteBuilder.getInstance().isRunning() && !this.dirty) {
                  saveLock.wait(500L);
               }
               if (!RouteBuilder.getInstance().isRunning()) {
                  break;
               }

               long now = System.currentTimeMillis();
               long waitMs = SAVE_DEBOUNCE_MS - (now - lastDirtyAtMs);
               if (waitMs > 0L) {
                  saveLock.wait(Math.min(waitMs, 500L));
               }

               shouldSave = this.dirty;
               this.dirty = false;
            }

            if (!shouldSave) {
               continue;
            }

            String data = VeinForge.gson.toJson(instance);
            Files.write(VeinForge.routesFile, data.getBytes(StandardCharsets.UTF_8));
         } catch (IOException e) {
            Logger.sendWarning("Route save loop crashed; will retry");
            VeinForge.LOGGER.error("Route save loop crashed", e);
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
         }
      }
   }

   public void onRender(LevelRenderContext context) {
      if (!isRouteRenderActive() || this.selectedRoute == null || this.selectedRoute.isEmpty()) {
         return;
      }

      this.selectedRoute.drawRoute();
   }

   private boolean isRouteRenderActive() {
      return RouteBuilder.getInstance().isRunning();
   }

   private void ensureDefaultRoutePresent() {
      if (!this.routes.containsKey("Default")) {
         this.routes.put("Default", new Route());
      }
   }

   private void rebindSelectedRouteFromConfig() {
      String configuredRouteName = "";
      if (VeinForge.config() != null) {
         configuredRouteName = normalizeRouteName(VeinForge.config().routeMiner.selectedRoute);
      }

      if (!configuredRouteName.isEmpty()) {
         String resolved = resolveExistingRouteKey(configuredRouteName);
         if (resolved != null) {
            this.selectedRoute = this.routes.get(resolved);
            return;
         }
      }

      this.selectedRoute = this.routes.get("Default");
   }
}
