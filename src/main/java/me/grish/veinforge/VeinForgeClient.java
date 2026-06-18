package me.grish.veinforge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.Command;
import me.grish.veinforge.command.CommandManager;
import me.grish.veinforge.config.ConfigGuiManager;
import me.grish.veinforge.config.ConfigManager;
import me.grish.veinforge.config.VeinForgeConfig;
import me.grish.veinforge.event.*;
import me.grish.veinforge.failsafe.FailsafeManager;
import me.grish.veinforge.feature.FeatureManager;
import me.grish.veinforge.handler.GameStateHandler;
import me.grish.veinforge.handler.GraphHandler;
import me.grish.veinforge.handler.RotationHandler;
import me.grish.veinforge.handler.RouteHandler;
import me.grish.veinforge.macro.MacroManager;
import me.grish.veinforge.ui.hud.HUDManager;
import me.grish.veinforge.util.ChatPacketUtil;
import me.grish.veinforge.util.Logger;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;


public class VeinForgeClient implements ClientModInitializer {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final String MOD_ID = "veinforge";
    public static ConfigManager configManager;
    public static VeinForgeConfig config;
    public static VeinForgeClient instance;

    public final String VERSION = FabricLoader.getInstance()
            .getModContainer(MOD_ID)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");

    public static Minecraft mc() {
        return Minecraft.getInstance();
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        Logger.sendLog("Initializing VeinForge...");

        // Initialize config
        initializeConfig();

        // Initialize managers (will be registered to events later)
        initializeManagers();

        // Register Fabric events
        EventManager.registerAll();

        // Register commands
        new CommandManager().registerAll();

        // Load routes after a tick to ensure world is ready
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level != null && !VeinForge.routesLoaded) {
                VeinForge.loadRoutes();
            }
        });

        Logger.sendLog("VeinForge initialized!");
    }

    private void initializeConfig() {
        configManager = new ConfigManager();
        configManager.firstLoad();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            if (configManager != null && config != null) {
                configManager.saveConfig();
            }
        });

        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> {
                    Command<FabricClientCommandSource> action = context -> {
                        Minecraft.getInstance().execute(() -> {
                            ConfigGuiManager.openConfigGui(null);
                        });
                        return 1;
                    };

                    dispatcher.register(literal("veinforge").executes(action));
                    dispatcher.register(literal("vf").executes(action));
                }
        );
    }

    private void initializeManagers() {
        // Managers are singletons - just access them to ensure they're created
        GameStateHandler.getInstance();
        RotationHandler.getInstance();
        RouteHandler.getInstance();
        GraphHandler.instance.toString(); // Static instance
        MacroManager.getInstance();
        FailsafeManager.getInstance();
        FeatureManager.getInstance();
        HUDManager.getInstance().loadPositions();

        PacketEvent.registerReceived(event -> Minecraft.getInstance().execute(() -> {
            FailsafeManager.getInstance().onPacketReceive(event.getPacket());
            MacroManager.getInstance().onPacketReceive(event);
            FeatureManager.getInstance().allFeatures.forEach(feature -> feature.handlePacketReceive(event.getPacket()));

            String message = ChatPacketUtil.extractMessage(event.getPacket());
            if (message != null) {
                FailsafeManager.getInstance().onChat(message);
                MacroManager.getInstance().onChat(message);
                FeatureManager.getInstance().allFeatures.forEach(feature -> feature.handleChat(message));
            }
        }));

        BlockChangeEvent.register(event -> Minecraft.getInstance().execute(() -> {
            FailsafeManager.getInstance().onBlockChange(event);
            FeatureManager.getInstance().allFeatures.forEach(feature -> feature.handleBlockChange(event));
        }));

        BlockDestroyEvent.register(event ->
                FeatureManager.getInstance().allFeatures.forEach(feature -> feature.handleBlockDestroy(event))
        );

        SpawnParticleEvent.register(event ->
                FeatureManager.getInstance().allFeatures.forEach(feature -> feature.handleParticleSpawn(event))
        );

        MotionUpdateEvent.register(RotationHandler.getInstance()::onMotionUpdate);
    }
}
