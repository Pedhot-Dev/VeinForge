package me.grish.veinforge.event;

import me.grish.veinforge.VeinForge;
import me.grish.veinforge.config.ConfigGuiManager;
import me.grish.veinforge.failsafe.FailsafeManager;
import me.grish.veinforge.feature.AbstractFeature;
import me.grish.veinforge.feature.FeatureManager;
import me.grish.veinforge.feature.impl.RouteBuilder;
import me.grish.veinforge.handler.GameStateHandler;
import me.grish.veinforge.handler.GraphHandler;
import me.grish.veinforge.handler.RotationHandler;
import me.grish.veinforge.handler.RouteHandler;
import me.grish.veinforge.macro.MacroManager;
import me.grish.veinforge.ui.hud.HUDManager;
import me.grish.veinforge.util.KeyPressUtil;
import me.grish.veinforge.util.RenderUtil;
import me.grish.veinforge.util.ScoreboardUtil;
import me.grish.veinforge.util.TablistUtil;
import me.grish.veinforge.util.tablist.TabListParser;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Central event manager that registers all Fabric callbacks.
 * Replaces the old Forge MinecraftForge.EVENT_BUS system.
 */
public class EventManager {

   public static void registerAll() {
      registerInternalEventBus();
      registerTickEvents();
      registerRenderEvents();
      registerInputEvents();
   }

   private static void registerInternalEventBus() {
      UpdateScoreboardEvent.register(GameStateHandler.getInstance()::onScoreboardUpdate);
      UpdateScoreboardLineEvent.register(ScoreboardUtil::onScoreboardLineUpdate);

      UpdateTablistEvent.register(event -> {
         TabListParser.updateCache();
         GameStateHandler.getInstance().onTablistUpdate(event);
         MacroManager.getInstance().onTablistUpdate(event);
         FeatureManager.getInstance().allFeatures.forEach(feature -> feature.handleTablistUpdate(event));
      });

      UpdateTablistFooterEvent.register(GameStateHandler.getInstance()::onTablistFooterUpdate);
   }

   private static void registerTickEvents() {
      ClientTickEvents.END_CLIENT_TICK.register(client -> {
         if (client.level == null || client.player == null) return;

         // Tick all managers
         GameStateHandler.getInstance().onTick();
         RotationHandler.getInstance().onTick();
         GraphHandler.instance.onTick();
         MacroManager.getInstance().onTick();
         FailsafeManager.getInstance().onTick();

         // Tick all features
         FeatureManager.getInstance().allFeatures.forEach(AbstractFeature::handleTick);

         // Update utilities
         ScoreboardUtil.update();
         TablistUtil.update();
      });
   }

   // NOTE (Fabric 1.21+/26.2 migration):
   // LevelRenderEvents.AFTER_ENTITIES no longer exists in this API version.
   // The render pipeline was split into multiple explicit phases (opaque terrain,
   // solid features, translucent features, overlays, etc.).
   //
   // This callback is now using AFTER_SOLID_FEATURES as the closest replacement
   // for the old "after entities rendered in world" timing.
   //the old "after entities rendered in world
   // Behavior may not be identical to the previous event. The render pipeline is
   // more granular now, so exact ordering relative to entities/translucency can differ.
   // If visual glitches appear, this may need adjustment to a different phase
   // such as AFTER_TRANSLUCENT_FEATURES or BEFORE_TRANSLUCENT_TERRAIN.
   private static void registerRenderEvents() {
      LevelRenderEvents.AFTER_SOLID_FEATURES.register(context -> {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level == null || mc.player == null) return;

         RenderUtil.beginWorldRender(context);
         RotationHandler.getInstance().onWorldRender(context);
         RouteHandler.getInstance().onWorldRender(context);
         GraphHandler.instance.onWorldRender(context);
         MacroManager.getInstance().onWorldRender(context);
         FeatureManager.getInstance().allFeatures.forEach(feature -> feature.handleWorldRender(context));
         RenderUtil.endWorldRender();
      });

      // HUD rendering (replaces RenderGameOverlayEvent)
      net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.attachElementAfter(net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.SUBTITLES, net.minecraft.resources.Identifier.fromNamespaceAndPath(me.grish.veinforge.VeinForge.MOD_ID, "hud"), (drawContext, tickCounter) -> {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level == null || mc.player == null) return;

         HUDManager.getInstance().onHudRender(drawContext);
         MacroManager.getInstance().onHudRender(drawContext);
         FeatureManager.getInstance().allFeatures.forEach(feature -> feature.handleHudRender(drawContext));

         // Render any queued screen-space debug lines last (always visible).
         RenderUtil.renderQueuedLineOverlays(drawContext);
      });
   }

   private static void registerInputEvents() {
      // Key input is handled via mixin or KeyBindingHelper
      // We'll use a tick-based check for now
      ClientTickEvents.END_CLIENT_TICK.register(client -> {
         if (client.level == null || client.player == null) return;

         handleRouteBuilderShortcut(client);
         GraphHandler.instance.onInput();
         MacroManager.getInstance().onInput();
         handleConfigGuiShortcut(client);
      });
   }

   private static void handleRouteBuilderShortcut(Minecraft client) {
      var config = VeinForge.config();
      if (config == null) return;

      int key = config.routeMiner.routeBuilder;
      boolean pressed = KeyPressUtil.wasPressed(client.getWindow(), key, client.gui.screen() == null);
      if (pressed) {
         RouteBuilder.getInstance().toggle();
      }
   }

   private static void handleConfigGuiShortcut(Minecraft client) {
      var config = VeinForge.config();
      if (config == null) return;
      int key = config.general.openConfigGuiKeybind;
      if (key == GLFW.GLFW_KEY_UNKNOWN) return;
      boolean pressed = KeyPressUtil.wasPressed(client.getWindow(), key, client.gui.screen() == null);
      if (pressed) {
         ConfigGuiManager.openConfigGui(null);
      }
   }
}
