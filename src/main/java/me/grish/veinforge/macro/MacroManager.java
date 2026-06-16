package me.grish.veinforge.macro;

import me.grish.veinforge.VeinForge;
import me.grish.veinforge.event.PacketEvent;
import me.grish.veinforge.event.UpdateTablistEvent;
import me.grish.veinforge.feature.FeatureManager;
import me.grish.veinforge.feature.impl.MouseUngrab;
import me.grish.veinforge.macro.impl.CommissionMacro.CommissionMacro;
import me.grish.veinforge.macro.impl.FishingMacro.FishingMacro;
import me.grish.veinforge.macro.impl.GlacialMacro.GlacialMacro;
import me.grish.veinforge.macro.impl.MiningMacro.MiningMacro;
import me.grish.veinforge.macro.impl.PowderMacro.PowderMacro;
import me.grish.veinforge.macro.impl.RouteMiner.RouteMinerMacro;
import me.grish.veinforge.util.KeyPressUtil;
import me.grish.veinforge.util.Logger;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Manages macro lifecycle and event dispatch.
 * Events are called from EventManager.
 */
public class MacroManager {

   private static final MacroManager instance = new MacroManager();
   private final Minecraft mc = Minecraft.getInstance();
   private AbstractMacro currentMacro;

   public static MacroManager getInstance() {
      return instance;
   }

   public AbstractMacro getCurrentMacro() {
      var config = VeinForge.config();
      if (config == null) return null;

      switch (config.general.macroType) {
         case 0:
            return CommissionMacro.getInstance();
         case 1:
            return GlacialMacro.getInstance();
         case 2:
            return MiningMacro.getInstance();
         case 3:
            return RouteMinerMacro.getInstance();
         case 4:
            return PowderMacro.getInstance();
         case 5:
            return FishingMacro.getInstance();
         default:
            return CommissionMacro.getInstance();
      }
   }

   /**
    * The macro currently managed by MacroManager (running or paused), or null if off.
    */
   public AbstractMacro getActiveMacro() {
      return currentMacro;
   }

   public void toggle() {
      log("Toggling");
      if (currentMacro != null) {
         log("CurrMacro != null");
         this.disable();
      } else {
         log("CurrMacro == null");
         this.enable();
      }
   }

   public void enable() {
      log("Macro::enable");
      FeatureManager.getInstance().enableAll();
      this.currentMacro = this.getCurrentMacro();
      if (this.currentMacro == null) {
         error("No macro selected!");
         return;
      }
      send(this.currentMacro.getName() + " Enabled");
      this.currentMacro.enable();
   }

   public void disable() {
      if (this.currentMacro == null) {
         return;
      }

      var config = VeinForge.config();
      if (config != null && config.debug.debugMode) {
         VeinForge.LOGGER.debug("Macro disable stack trace", new Throwable());
      }

      log("Macro::disable");
      FeatureManager.getInstance().disableAll();
      MouseUngrab.getInstance().regrabMouse();
      this.currentMacro.disable();
      send(this.currentMacro.getName() + " Disabled");
      this.currentMacro = null;
   }

   public void pause() {
      if (this.currentMacro == null) {
         return;
      }
      log("Macro::pause");
      this.currentMacro.pause();
      send(this.currentMacro.getName() + " Paused");
   }

   public void resume() {
      if (this.currentMacro == null) {
         return;
      }
      log("Macro::resume");
      this.currentMacro.resume();
      send(this.currentMacro.getName() + " Resumed");
   }

   public boolean isEnabled() {
      return this.currentMacro != null;
   }

   public boolean isRunning() {
      return this.currentMacro != null && this.currentMacro.isEnabled();
   }

   // ==================== Event Handlers (called from EventManager) ====================

   /**
    * Called every client tick.
    */
   public void onTick() {
      if (this.currentMacro == null) {
         return;
      }

      // Disable if macro stopped itself
      if (!currentMacro.isEnabled()) {
         this.disable();
         return;
      }

      this.currentMacro.onTick();
   }

   /**
    * Called for key input checks.
    */
   public void onInput() {
      var config = VeinForge.config();
      if (config == null) return;

      if (KeyPressUtil.wasPressed(mc.getWindow(), config.general.toggleMacro, true)) {
         toggle();
      }
   }

   /**
    * Called when a chat message is received.
    */
   public void onChat(String message) {
      if (this.currentMacro == null) {
         return;
      }
      this.currentMacro.onChat(message);
   }

   /**
    * Called when tablist updates.
    */
   public void onTablistUpdate(UpdateTablistEvent event) {
      if (this.currentMacro == null) {
         return;
      }
      this.currentMacro.onTablistUpdate(event);
   }

   /**
    * Called for world rendering.
    */
   public void onWorldRender(LevelRenderContext context) {
      if (this.currentMacro == null) {
         return;
      }
      this.currentMacro.onWorldRender(context);
   }

   /**
    * Called for HUD rendering.
    */
    public void onHudRender(GuiGraphicsExtractor drawContext) {
      if (this.currentMacro == null) {
         return;
      }
      this.currentMacro.onOverlayRender(drawContext);
   }

   /**
    * Called when a packet is received.
    */
   public void onPacketReceive(PacketEvent.Received event) {
      if (this.currentMacro == null) {
         return;
      }
      this.currentMacro.onReceivePacket(event);
   }

   // ==================== Logging Utilities ====================

   public void log(String message) {
      Logger.sendLog(getMessage(message));
   }

   public void send(String message) {
      Logger.sendMessage(getMessage(message));
   }

   public void error(String message) {
      Logger.sendError(getMessage(message));
   }

   public void warn(String message) {
      Logger.sendWarning(getMessage(message));
   }

   public String getMessage(String message) {
      return "[MacroHandler] " + message;
   }
}
