package me.grish.veinforge;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import me.grish.veinforge.config.VeinForgeConfig;
import me.grish.veinforge.handler.GraphHandler;
import me.grish.veinforge.handler.RouteHandler;
import me.grish.veinforge.util.Logger;
import me.grish.veinforge.util.helper.graph.*;
import me.grish.veinforge.util.helper.route.RouteWaypoint;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Static utility class for VeinForge.
 * Entry point is now VeinForgeClient.
 */
public class VeinForge {

   public static final String MOD_ID = "veinforge";
   public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
   public static final Gson gson = new GsonBuilder()
                                           .registerTypeAdapter(new TypeToken<Graph<RouteWaypoint>>() {
                                           }.getType(), new GraphSerializer())
                                           .excludeFieldsWithoutExposeAnnotation()
                                           .setPrettyPrinting()
                                           .create();
   private static final Path CONFIG_DIRECTORY = FabricLoader.getInstance()
                                                        .getConfigDir()
                                                        .resolve("veinforge");
   public static final Path routesDirectory = CONFIG_DIRECTORY.resolve("graphs");
   public static final Path routesFile = CONFIG_DIRECTORY.resolve("routes.json");

   private static final int EXECUTOR_MAX_THREADS = Math.min(
           8,
           Math.max(2, Runtime.getRuntime().availableProcessors())
   );
   private static final int EXECUTOR_CORE_THREADS = Math.min(4, EXECUTOR_MAX_THREADS);
   private static final ThreadPoolExecutor executor = createExecutor();
   private static final int EXECUTOR_QUEUE_CAPACITY = 512;
   private static final List<String> expectedRoutes = Arrays.asList(
           "Glacial Macro.json", "Commission Macro.json", "Galatea Fishing.json"
   );

   public static boolean routesLoaded = false;

   // Convenience accessors
   public static VeinForgeConfig config() {
      return VeinForgeClient.config;
   }

   public static Executor executor() {
      return executor;
   }

   private static ThreadPoolExecutor createExecutor() {
      AtomicInteger threadId = new AtomicInteger(1);

      RejectedExecutionHandler rejectedExecutionHandler = (runnable, exec) -> {
         if (exec.isShutdown()) {
            return;
         }
         Logger.sendWarning("VeinForge executor saturated; running task on calling thread");
         LOGGER.warn("VeinForge executor saturated; running task on calling thread");
         runnable.run();
      };

      ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
              EXECUTOR_CORE_THREADS,
              EXECUTOR_MAX_THREADS,
              60L,
              TimeUnit.SECONDS,
              new LinkedBlockingQueue<>(EXECUTOR_QUEUE_CAPACITY),
              runnable -> {
                 Thread thread = new Thread(runnable);
                 thread.setName("veinforge-worker-" + threadId.getAndIncrement());
                 thread.setDaemon(true);
                 thread.setUncaughtExceptionHandler((t, e) -> {
                    Logger.sendError("Uncaught exception in " + t.getName() + ": " + e);
                    LOGGER.error("Uncaught exception in {}", t.getName(), e);
                 });
                 return thread;
              },
              rejectedExecutionHandler
      );
      threadPoolExecutor.allowCoreThreadTimeOut(true);
      return threadPoolExecutor;
   }

   public static Minecraft mc() {
      return Minecraft.getInstance();
   }

   /**
    * Load routes from disk. Called after world is ready.
    */
   public static void loadRoutes() {
      if (routesLoaded) return;
      routesLoaded = true;

      try {
         if (!Files.exists(routesDirectory)) {
            Logger.sendLog("Routes directory not found, creating it now.");
         }
         Files.createDirectories(routesDirectory);
      } catch (Exception e) {
         Logger.sendWarning("Failed to create routes directory: " + routesDirectory);
         LOGGER.warn("Failed to create routes directory: {}", routesDirectory, e);
      }

      // Bootstrap defaults (never delete unknown/user files).
      for (String file : expectedRoutes) {
         Path filePath = routesDirectory.resolve(file);
         if (Files.exists(filePath)) {
            continue;
         }

         try (InputStream in = VeinForge.class.getResourceAsStream("/veinforge/" + file)) {
            if (in == null) {
               Logger.sendWarning("Default graph missing from jar: " + file);
               continue;
            }
            Files.copy(in, filePath);
         } catch (Exception e) {
            Logger.sendWarning("Failed to copy default graph: " + file);
            LOGGER.warn("Failed to copy default graph: {}", file, e);
         }
      }

      // Load expected graphs and preserve/also load extra user graphs.
      try (Stream<Path> paths = Files.list(routesDirectory)) {
         paths
                 .filter(Files::isRegularFile)
                 .filter(p -> p.getFileName().toString().endsWith(".json"))
                 .forEach(p -> {
                    if (!loadGraph(p)) {
                       Logger.sendWarning("Couldn't load " + p.getFileName());
                    }
                 });
      } catch (Exception e) {
         Logger.sendWarning("Failed to list graphs in: " + routesDirectory);
         LOGGER.warn("Failed to list graphs in: {}", routesDirectory, e);
      }

      RouteHandler.getInstance().loadData();

      // Select route from config
      if (config() != null && !config().routeMiner.selectedRoute.isEmpty()) {
         RouteHandler.getInstance().selectRoute(config().routeMiner.selectedRoute);
      }
   }

   private static boolean loadGraph(Path path) {
      String graphKey = path.getFileName().toString().replace(".json", "");
      try (Reader reader = Files.newBufferedReader(path)) {
         JsonElement element = JsonParser.parseReader(reader);
         if (!element.isJsonObject()) {
            throw new IllegalStateException("Graph file must be a JSON object");
         }
         JsonObject object = element.getAsJsonObject();
         Graph<RouteWaypoint> graph;

         if (object.has("schemaVersion")) {
            GraphV2 graphV2 = gson.fromJson(object, GraphV2.class);
            graph = graphV2.toGraphStrict();
         } else if (object.has("map")) {
            Logger.sendWarning("GraphV1 will be removed in the future; use GraphV2: " + graphKey);
            graph = gson.fromJson(object, new TypeToken<Graph<RouteWaypoint>>() {
            }.getType());
            GraphNormalizationResult normalized = GraphNormalizer.normalizeLegacy(graph);
            graph = normalized.graph();
            if (normalized.hasChanges()) {
               Logger.sendLog("Normalized legacy graph for: " + graphKey
                                      + " (duplicates=" + normalized.validation().duplicateEdges()
                                      + ", selfLoops=" + normalized.validation().selfLoops()
                                      + ", dangling=" + normalized.validation().danglingEdges() + ")");
            }
         } else {
            throw new IllegalStateException("Unsupported graph format: " + graphKey);
         }
         GraphHandler.instance.putGraph(graphKey, graph);
         Logger.sendLog("Loaded graph for: " + graphKey);
         return true;
      } catch (Exception e) {
         Logger.sendError("Something went wrong while loading the graph for: " + graphKey);
         LOGGER.error("Something went wrong while loading the graph for: {}", graphKey, e);
         return false;
      }
   }
}
