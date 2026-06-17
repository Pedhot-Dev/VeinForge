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
import me.grish.veinforge.ui.hud.ColorPalette;
import me.grish.veinforge.util.ScoreboardUtil;

import java.util.List;

public class DebugHUD extends TextHud {

   @Getter
   private final static DebugHUD instance = new DebugHUD();

   public static DebugHUD getInstance() {
      return instance;
   }

   public DebugHUD() {
      super();
      this.x = 1;
      this.y = 10;
      this.enabled = false;
   }

   @Override
   protected int getAccentColor() {
      return 0xFF94A3B8; // Slate 400
   }

   @Override
   protected void getLines(List<String> lines, boolean example) {

      if (example) {
         lines.add("§f§lDEBUG CONSOLE");
         lines.add("§8§m------------------------");
         lines.add("§8» §7Macro: §fCommission §8(§aRUN§8)");
         lines.add("§8» §7State: §fMiningState");
         lines.add("§8» §7System: §f165 FPS §8| §f23ms");
         lines.add("§8» §7Pos: §f123, 70, 456");
         return;
      }

      lines.add("§f§lDEBUG CONSOLE");
      lines.add("§8§m------------------------");
      if (mc.player == null) {
         lines.add("§8» §cPlayer not found");
         return;
      }

      getMacroFailsafeLine(lines);
      lines.add("§8» §7System: §f" + mc.getFps() + " FPS §8| §f" + (mc.isWindowActive() ? "§aFocused" : "§cBackground"));
      lines.add("§8» §7Pos: §f" + mc.player.getBlockX() + ", " + mc.player.getBlockY() + ", " + mc.player.getBlockZ());
      lines.add("§8» §7Rot: §f" + String.format("%.1f", mc.player.getYRot()) + " / " + String.format("%.1f", mc.player.getXRot()));

      String title = ScoreboardUtil.getScoreboardTitle();
      if (title != null && !title.isEmpty()) {
         lines.add("§8» §7Score: §f" + title);
      }
   }

   private void getMacroFailsafeLine(List<String> lines) {
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
         failsafeLabel = "§cTRIG";
      } else {
         failsafeLabel = "§aOK";
      }

      lines.add("§8» §7Macro: §f" + macroName + " §8(" + runLabel + "§8)");
      lines.add("§8» §7State: §f" + stateLabel);
      lines.add("§8» §7Failsafe: " + failsafeLabel);
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
