package me.grish.veinforge.config;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.common.text.StructuredText;
import me.grish.veinforge.VeinForgeClient;
import me.grish.veinforge.config.Categorie.*;

@SuppressWarnings("deprecation")
public class VeinForgeConfig extends Config {

   @Category(name = "General", desc = "General Settings")
   public General general = new General();
   @Category(name = "Commission", desc = "Commission Settings")
   public Commission commission = new Commission();
   @Category(name = "Mining Macro", desc = "Mining Macro Settings")
   public MiningMacro miningMacro = new MiningMacro();
   @Category(name = "Route Miner", desc = "Route Miner Settings")
   public RouteMiner routeMiner = new RouteMiner();
   @Category(name = "Powder Macro", desc = "Powder Macro Settings")
   public PowderMacro powderMacro = new PowderMacro();
   @Category(name = "Fishing", desc = "Fishing Macro Settings")
   public Fishing fishing = new Fishing();
   @Category(name = "Rift", desc = "Rift Settings")
   public Rift rift = new Rift();
   @Category(name = "Delays", desc = "Delay Settings")
   public Delays delays = new Delays();
   @Category(name = "Failsafe", desc = "Failsafe Settings")
   public Failsafe failsafe = new Failsafe();
   @Category(name = "Debug", desc = "Debug Settings")
   public Debug debug = new Debug();
   @Category(name = "HUD", desc = "HUD Settings")
   public HUD hud = new HUD();
   @Category(name = "Render", desc = "Render Settings")
   public Render render = new Render();
//   @Expose
//   @Category(name = "Main", desc = "")
//   public MainCategory mainCategory = new MainCategory();

   @Override
   public StructuredText getTitle() {
      return StructuredText.of(
              "VeinForge " + VeinForgeClient.instance.VERSION
      );
   }

   public int getRandomRotationTime() {
      int sampled = (
              delays.rotationTime +
                      (int) (Math.random() * delays.rotationTimeRandomizer)
      );
      int humanFast = (int) Math.round(sampled * 0.8d);
      return Math.max(120, humanFast);
   }

   public int getRandomSneakTime() {
      return (
              delays.sneakTime +
                      (int) (Math.random() * delays.sneakTimeRandomizer)
      );
   }

   public int getRandomGuiWaitDelay() {
      return (
              delays.delaysGuiDelay +
                      (int) (Math.random() * delays.delaysGuiDelayRandomizer)
      );
   }

   public static class HUDPos {
      public float x;
      public float y;
      public int anchor;
      public float scale;

      public HUDPos(float x, float y, int anchor, float scale) {
         this.x = x;
         this.y = y;
         this.anchor = anchor;
         this.scale = scale;
      }
   }
}
