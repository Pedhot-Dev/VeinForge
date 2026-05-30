package me.grish.veinforge.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.notenoughupdates.moulconfig.processor.BuiltinMoulConfigGuis;
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver;
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor;
import net.fabricmc.loader.api.FabricLoader;

import me.grish.veinforge.VeinForge;
import me.grish.veinforge.VeinForgeClient;

public class ConfigManager {

   private static final Path CONFIG_PATH = FabricLoader.getInstance()
                                                   .getConfigDir()
                                                   .resolve("veinforge")
                                                   .resolve("veinforge.json");
   private static final Path BACKUP_PATH = CONFIG_PATH.resolveSibling("veinforge.json.bak");
   private static final Path TEMP_PATH = CONFIG_PATH.resolveSibling("veinforge.json.tmp");
   private static final Gson gson = new GsonBuilder()
                                            .setPrettyPrinting()
                                            .create();
   private final Object ioLock = new Object();
   public MoulConfigProcessor<VeinForgeConfig> processor;

   /**
    * MoulConfig's colour editor expects `speed:alpha:r:g:b`. Older VeinForge configs stored
    * `r:g:b[:a]`, which can crash the editor when the first component is 0.
    */
   private static boolean normalizeColourOptions(VeinForgeConfig config) {
      if (config == null || config.routeMiner == null || config.debug == null) {
         return false;
      }

      boolean changed = false;

      String updatedNode = normalizeMoulColourString(config.routeMiner.routeBuilderNodeColor);
      if (updatedNode != null && !updatedNode.equals(config.routeMiner.routeBuilderNodeColor)) {
         config.routeMiner.routeBuilderNodeColor = updatedNode;
         changed = true;
      }

      String updatedTracer = normalizeMoulColourString(config.routeMiner.routeBuilderTracerColor);
      if (updatedTracer != null && !updatedTracer.equals(config.routeMiner.routeBuilderTracerColor)) {
         config.routeMiner.routeBuilderTracerColor = updatedTracer;
         changed = true;
      }

      String updatedDebug = normalizeMoulColourString(config.debug.debugRouteColor);
      if (updatedDebug != null && !updatedDebug.equals(config.debug.debugRouteColor)) {
         config.debug.debugRouteColor = updatedDebug;
         changed = true;
      }

      return changed;
   }

   /**
    * Returns a value safe for MoulConfig's colour editor (`speed:alpha:r:g:b`).
    * Accepts `r:g:b`, `r:g:b:a`, or already-normalized strings.
    */
   private static String normalizeMoulColourString(String value) {
      if (value == null) {
         return null;
      }
      String trimmed = value.trim();
      if (trimmed.isEmpty()) {
         return value;
      }

      String[] parts = trimmed.split(":");
      if (parts.length >= 5) {
         return trimmed;
      }

      Integer r = null;
      Integer g = null;
      Integer b = null;
      Integer a = null;

      try {
         if (parts.length == 3) {
            r = Integer.parseInt(parts[0].trim());
            g = Integer.parseInt(parts[1].trim());
            b = Integer.parseInt(parts[2].trim());
            a = 255;
         } else if (parts.length == 4) {
            r = Integer.parseInt(parts[0].trim());
            g = Integer.parseInt(parts[1].trim());
            b = Integer.parseInt(parts[2].trim());
            a = Integer.parseInt(parts[3].trim());
         } else {
            return value;
         }
      } catch (NumberFormatException e) {
         return value;
      }

      r = clampByte(r);
      g = clampByte(g);
      b = clampByte(b);
      a = clampByte(a);
      return "0:" + a + ":" + r + ":" + g + ":" + b;
   }

   private static int clampByte(int v) {
      if (v < 0) {
         return 0;
      }
      if (v > 255) {
         return 255;
      }
      return v;
   }

   public void firstLoad() {
      VeinForgeConfig loadedConfig = readConfigWithDefaults(CONFIG_PATH);
      if (loadedConfig == null) {
         loadedConfig = readConfigWithDefaults(BACKUP_PATH);
         if (loadedConfig == null) {
            VeinForge.LOGGER.warn("No readable config found; creating defaults at {}", CONFIG_PATH);
            loadedConfig = new VeinForgeConfig();
         } else {
            VeinForge.LOGGER.warn("Recovered config from backup: {}", BACKUP_PATH);
         }

         VeinForgeClient.config = loadedConfig;
         saveConfig();
      } else {
         VeinForgeClient.config = loadedConfig;
      }

      if (normalizeColourOptions(VeinForgeClient.config)) {
         saveConfig();
      }
      recreateProcessor();
   }

   public void saveConfig() {
      synchronized (ioLock) {
         try {
            Path parent = CONFIG_PATH.getParent();
            if (parent != null) {
               Files.createDirectories(parent);
            }

            byte[] jsonBytes = gson.toJson(VeinForgeClient.config).getBytes(StandardCharsets.UTF_8);
            Files.write(
                    TEMP_PATH,
                    jsonBytes,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );

            if (Files.exists(CONFIG_PATH)) {
               Files.copy(CONFIG_PATH, BACKUP_PATH, StandardCopyOption.REPLACE_EXISTING);
            }

            moveTempToConfig();
         } catch (IOException | RuntimeException e) {
            VeinForge.LOGGER.error("Failed to save config: {}", CONFIG_PATH, e);
            try {
               Files.deleteIfExists(TEMP_PATH);
            } catch (IOException cleanupError) {
               VeinForge.LOGGER.warn("Failed to clean up temp config file: {}", TEMP_PATH, cleanupError);
            }
         }
      }
   }

   public void recreateProcessor() {
      processor = new MoulConfigProcessor<>(VeinForgeClient.config);
      BuiltinMoulConfigGuis.addProcessors(processor);
      ConfigProcessorDriver driver = new ConfigProcessorDriver(processor);
      driver.warnForPrivateFields = false;
      driver.checkExpose = false;
      driver.processConfig(VeinForgeClient.config);
   }

   private static VeinForgeConfig readConfigWithDefaults(Path path) {
      if (!Files.exists(path)) {
         return null;
      }

      try {
         String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
         JsonElement parsed = JsonParser.parseString(json);
         if (!parsed.isJsonObject()) {
            throw new IllegalStateException("Config root must be a JSON object");
         }

         JsonObject merged = gson.toJsonTree(new VeinForgeConfig()).getAsJsonObject();
         deepMergeInto(merged, parsed.getAsJsonObject());

         VeinForgeConfig config = gson.fromJson(merged, VeinForgeConfig.class);
         if (config == null) {
            throw new IllegalStateException("Gson returned null config");
         }
         return config;
      } catch (Exception e) {
         VeinForge.LOGGER.warn("Failed to read config from {}: {}", path, e.toString());
         VeinForge.LOGGER.debug("Config read failure details for {}", path, e);
         return null;
      }
   }

   private static void deepMergeInto(JsonObject target, JsonObject source) {
      for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
         String key = entry.getKey();
         JsonElement sourceValue = entry.getValue();
         JsonElement targetValue = target.get(key);

         if (sourceValue != null
                 && sourceValue.isJsonObject()
                 && targetValue != null
                 && targetValue.isJsonObject()) {
            deepMergeInto(targetValue.getAsJsonObject(), sourceValue.getAsJsonObject());
            continue;
         }

         target.add(key, sourceValue);
      }
   }

   private static void moveTempToConfig() throws IOException {
      try {
         Files.move(
                 TEMP_PATH,
                 CONFIG_PATH,
                 StandardCopyOption.REPLACE_EXISTING,
                 StandardCopyOption.ATOMIC_MOVE
         );
      } catch (AtomicMoveNotSupportedException ignored) {
         Files.move(
                 TEMP_PATH,
                 CONFIG_PATH,
                 StandardCopyOption.REPLACE_EXISTING
         );
      }
   }
}
