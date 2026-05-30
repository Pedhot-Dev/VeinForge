package me.grish.veinforge.ui.hud.elements;

import lombok.Getter;
import me.grish.veinforge.VeinForge;
import me.grish.veinforge.client.overlay.TextHud;
import me.grish.veinforge.macro.AbstractMacro;
import me.grish.veinforge.macro.MacroManager;
import me.grish.veinforge.macro.impl.FishingMacro.FishingMacro;
import me.grish.veinforge.macro.impl.FishingMacro.states.FishingKillingState;
import me.grish.veinforge.macro.impl.FishingMacro.states.FishingMacroState;

import java.util.List;
import java.util.Locale;

public class FishingHUD extends TextHud {

   @Getter
   private static final FishingHUD instance = new FishingHUD();

   private FishingHUD() {
      super();
      this.x = 5;
      this.y = 185;
      this.enabled = true;
   }

   @Override
   protected int getAccentColor() {
      return 0xFF60A5FA;
   }

   @Override
   protected boolean shouldShow() {
      if (!super.shouldShow()) {
         return false;
      }
      AbstractMacro active = MacroManager.getInstance().getActiveMacro();
      return active instanceof FishingMacro && active.isEnabled();
   }

   @Override
   protected void getLines(List<String> lines, boolean example) {
      if (example) {
         lines.add("§9§lGalatea Macro");
         lines.add("§8§m------------------------");
         lines.add("§8» §7Status: §aRUNNING §8| §7State: §fFishingKilling");
         lines.add("§8» §7Mode: §fMelee §8| §7Swap: §aON");
         lines.add("§8» §7Striders: §f29 §8(§f11 reachable§8)");
         lines.add("§8» §7Trigger: §f24 §8[§f20-30§8]");
         lines.add("§8» §7Combat: §aCAP §8| §cSTUCK §8| §7Target: §f18231");
         lines.add("§8» §7Reason: §fNO_LOS §8| §7Loop: §fWAITING_FOR_BITE");
         lines.add("§8» §7Timers: §fRC 1.3s §8| §fLC 0.2s §8| §fCast 6.8s");
         lines.add("§8§m------------------------");
         return;
      }

      MacroManager manager = MacroManager.getInstance();
      AbstractMacro active = manager.getActiveMacro();
      boolean activeFishing = active instanceof FishingMacro;
      FishingMacro macro = FishingMacro.getInstance();
      if (!activeFishing || !macro.isEnabled()) {
         return;
      }
      FishingMacroState state = macro.getCurrentState();

      String stateName = "Idle";
      if (state != null) {
         stateName = simplifyStateName(state.getClass().getSimpleName());
      }

      String killMode = VeinForge.config().fishing.galateaFishing.galateaKillMode == 1 ? "Slayer" : "Melee";
      String fastSwap = VeinForge.config().fishing.galateaFishing.galateaFastWeaponSwap ? "§aON" : "§cOFF";

      lines.add("§9§lGalatea Macro");
      lines.add("§8§m------------------------");
      lines.add("§8» §7Status: §aRUNNING §8| §7State: §f" + stateName);
      lines.add("§8» §7Mode: §f" + killMode + " §8| §7Swap: " + fastSwap);

      if (state instanceof FishingKillingState killingState) {
         long now = System.currentTimeMillis();
         int nearby = killingState.getHudNearbyStriders();
         int reachable = killingState.getHudReachableStriders();
         int trigger = killingState.getStriderTriggerCount();
         int min = killingState.getHudMinStriderCount();
         int max = killingState.getHudMaxStriderCount();
         String capKill = killingState.isCapKillMode() ? "§aCAP" : "§8CAP";
         String stuck = killingState.isInStuckCombat() ? "§cSTUCK" : "§8STUCK";
         String reason = killingState.getHudReason();
         int targetId = killingState.getCapKillTargetEntityId();
         long rcInMs = Math.max(0L, killingState.getNextCapKillAttackAtMs() - now);
         long lcInMs = Math.max(0L, killingState.getNextCapKillMeleeAtMs() - now);
         long castAgeMs = killingState.getCastAtMs() > 0L ? now - killingState.getCastAtMs() : 0L;
         long staleMs = Math.max(0L, now - killingState.getHudLastUpdatedAtMs());

         lines.add("§8» §7Striders: §f" + nearby + " §8(§f" + reachable + " reachable§8)");
         lines.add("§8» §7Trigger: §f" + trigger + " §8[§f" + min + "-" + max + "§8]");
         lines.add("§8» §7Combat: " + capKill + " §8| " + stuck + " §8| §7Target: §f" + (targetId >= 0 ? targetId : "-"));
         lines.add("§8» §7Reason: §f" + (reason == null || reason.isEmpty() ? "-" : reason) + " §8| §7Loop: §f" + killingState.getFishingLoopStateName());
         lines.add("§8» §7Timers: §fRC " + formatSeconds(rcInMs) + "s §8| §fLC " + formatSeconds(lcInMs) + "s §8| §fCast " + formatSeconds(castAgeMs) + "s");
         lines.add("§8» §7Sync age: §f" + formatSeconds(staleMs) + "s");
      } else {
         lines.add("§8» §7Waiting for FishingKilling state...");
      }
      lines.add("§8§m------------------------");
   }

   private String simplifyStateName(String stateName) {
      if (stateName == null || stateName.isEmpty()) {
         return "Idle";
      }
      if (stateName.endsWith("State")) {
         return stateName.substring(0, stateName.length() - "State".length());
      }
      return stateName;
   }

   private String formatSeconds(long millis) {
      return String.format(Locale.ROOT, "%.1f", millis / 1000.0D);
   }
}
