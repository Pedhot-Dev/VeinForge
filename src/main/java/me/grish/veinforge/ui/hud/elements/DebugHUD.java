package me.grish.veinforge.ui.hud.elements;

import lombok.Getter;
import me.grish.veinforge.client.overlay.TextHud;
import me.grish.veinforge.failsafe.FailsafeManager;
import me.grish.veinforge.macro.AbstractMacro;
import me.grish.veinforge.macro.MacroManager;
import me.grish.veinforge.macro.impl.CommissionMacro.CommissionMacro;
import me.grish.veinforge.macro.impl.GlacialMacro.GlacialMacro;
import me.grish.veinforge.macro.impl.PowderMacro.PowderMacro;
import me.grish.veinforge.macro.impl.RouteMiner.RouteMinerMacro;
import me.grish.veinforge.util.ScoreboardUtil;

import java.util.List;

public class DebugHUD extends TextHud {

   @Getter
   private final static DebugHUD instance = new DebugHUD();

   public DebugHUD() {
      super();
      this.x = 1;
      this.y = 10;
      this.enabled = false;
   }

   @Override
   protected void getLines(List<String> lines, boolean example) {

      if (example) {
         lines.add("§f§lDebug");
         lines.add("§7Macro §fCommission Macro §8(§aRUN §8/ §fMiningState§8) §8| §7Failsafe §fOK");
         lines.add("§7FPS §f165 §8| §7Ping §f23ms");
         lines.add("§7Location §fDwarven Mines §8| §7Sub §fRoyal Mines");
         lines.add("§7Yaw/Pitch §f123.4 §8/ §f-12.3");
         lines.add("§7Scoreboard §fCommission Tracker");
         return;
      }

      lines.add("§f§lDebug");
      if (mc.player == null) {
         lines.add("§7No player");
         return;
      }

      lines.add(getMacroFailsafeLine());

      lines.add("§7FPS §f" + mc.getFps() + " §8| §7Focus §f" + mc.isWindowActive());
      lines.add("§7Yaw/Pitch §f" + String.format("%.1f", mc.player.getYRot()) + " §8/ §f" + String.format("%.1f", mc.player.getXRot()));

      String title = ScoreboardUtil.getScoreboardTitle();
      if (title != null && !title.isEmpty()) {
         lines.add("§7Scoreboard §f" + title);
      }
   }

   private String getMacroFailsafeLine() {
      MacroManager mm = MacroManager.getInstance();
      AbstractMacro active = mm.getActiveMacro();
      AbstractMacro selected = mm.getCurrentMacro();

      String macroName = active != null ? active.getName() : (selected != null ? selected.getName() : "None");
      boolean running = active != null && active.isEnabled();
      boolean paused = active != null && !active.isEnabled();

      String runLabel;
      if (running) {
         runLabel = "§aRUN";
      } else if (paused) {
         runLabel = "§ePAUSE";
      } else {
         runLabel = "§cOFF";
      }

      String stateLabel = getMacroStateLabel(active);

      FailsafeManager fm = FailsafeManager.getInstance();
      String failsafeLabel;
      if (fm.triggeredFailsafe.isPresent()) {
         failsafeLabel = "§cTRIG " + fm.triggeredFailsafe.get().getFailsafeType();
      } else if (!fm.emergencyQueue.isEmpty()) {
         failsafeLabel = "§eQ " + fm.emergencyQueue.size();
      } else {
         failsafeLabel = "§aOK";
      }

      return "§7Macro §f" + macroName + " §8(" + runLabel + " §8/ §f" + stateLabel + "§8) §8| §7Failsafe " + failsafeLabel;
   }

   private String getMacroStateLabel(AbstractMacro macro) {
      if (macro == null) {
         return "-";
      }

      if (macro instanceof CommissionMacro cm) {
         return cm.getCurrentState() == null ? "-" : cm.getCurrentState().getClass().getSimpleName();
      }
      if (macro instanceof GlacialMacro gm) {
         return gm.getCurrentState() == null ? "-" : gm.getCurrentState().getClass().getSimpleName();
      }
      if (macro instanceof RouteMinerMacro rm) {
         return rm.getCurrentState() == null ? "-" : rm.getCurrentState().getClass().getSimpleName();
      }
      if (macro instanceof PowderMacro pm) {
         return pm.getCurrentState() == null ? "-" : pm.getCurrentState().getClass().getSimpleName();
      }

      return "-";
   }
}
