package me.grish.veinforge.ui.hud.elements;

import lombok.Getter;
import me.grish.veinforge.VeinForge;
import me.grish.veinforge.client.overlay.TextHud;
import me.grish.veinforge.handler.GameStateHandler;
import me.grish.veinforge.macro.impl.GlacialMacro.GlacialMacro;
import me.grish.veinforge.macro.impl.GlacialMacro.GlaciteVeins;
import me.grish.veinforge.ui.hud.ColorPalette;
import me.grish.veinforge.util.TablistUtil;
import me.grish.veinforge.util.helper.location.SubLocation;

import java.util.List;
import java.util.Map;

public class GlacialCommissionHUD extends TextHud {

   @Getter
   private static final GlacialCommissionHUD instance = new GlacialCommissionHUD();
    private final transient GlacialMacro glacialMacro = GlacialMacro.getInstance();

   public static GlacialCommissionHUD getInstance() {
      return instance;
   }

   public GlacialCommissionHUD() {
      super();
      this.x = 5;
      this.y = 5;
   }

   @Override
   protected int getAccentColor() {
      return ColorPalette.SKY_400;
   }

   @Override
   protected void getLines(List<String> lines, boolean example) {
      long uptime;
      int totalComms;
      long hotmExpGained;

      if (example) {
         uptime = (45 * 60L) + 12;
         totalComms = 7;
         hotmExpGained = (long) totalComms * 750;
      } else {
         uptime = glacialMacro.uptime.getTimePassed() / 1000;
         totalComms = glacialMacro.getCommissionCounter();
         hotmExpGained = (long) totalComms * 750;
      }

      int commsPerHour = 0;
      if (uptime > 0) {
         commsPerHour = (int) ((float) totalComms / uptime * 3600);
      }

      lines.add("§b§lGLACIAL COMMISSION");
      lines.add("§8§m------------------------");
      lines.add("§8» §7Status: " + (glacialMacro.isEnabled() ? "§aRUNNING" : "§cPAUSED") + " §8(§b" + formatElapsedTime(uptime) + "§8)");
      lines.add("§8» §7Comms: §f" + totalComms + " §8(§3" + commsPerHour + "/h§8)");
      lines.add("§8» §7HOTM XP: §d" + formatNumberWithK(hotmExpGained) + " §8(§d" + formatNumberWithK((long) commsPerHour * 900) + "/h§8)");

      if (example) {
         lines.add("§8§m------------------------");
         lines.add("§8» §7Mining: §eUmber");
         lines.add("§8» §bGLACITE: §a3/7 §8(§f42%§8)");
         lines.add("§8» §bUMBER: §a5/9 §8(§f61%§8)");
         return;
      }

      if (glacialMacro.isEnabled() && glacialMacro.getCurrentState() != null) {
         lines.add("§8§m------------------------");
         lines.add("§8» §7State: §e" + glacialMacro.getCurrentState().getClass().getSimpleName().replace("State", ""));

         Map<GlaciteVeins, Double> commPercentages = TablistUtil.getGlaciteComs();
         List<GlaciteVeins> typesToMine = glacialMacro.getTypeToMine();

         if (!typesToMine.isEmpty()) {
            for (GlaciteVeins veinType : typesToMine) {
               double percentage = commPercentages.getOrDefault(veinType, 0.0);
               int totalVeins = GlaciteVeins.getVeins(veinType).length;
               long blacklistedCount = glacialMacro.getPreviousVeins().keySet().stream()
                                               .filter(pair -> pair.first() == veinType)
                                               .count();
               long availableCount = totalVeins - blacklistedCount;

               lines.add(String.format("§8» §b%s: §a%d/%d §8(§f%.0f%%§8)", veinType.toString(), availableCount, totalVeins, percentage));
            }
         }
      }
   }

   private String formatNumberWithK(long number) {
      if (number >= 1000) {
         double dividedNumber = number / 1000.0;
         if (dividedNumber == (long) dividedNumber) {
            return String.format("%dk", (long) dividedNumber);
         } else {
            return String.format("%.1fk", dividedNumber).replace(".0", "");
         }
      }
      return String.valueOf(number);
   }

   private String formatElapsedTime(long elapsedTimeSeconds) {
      long seconds = elapsedTimeSeconds % 60;
      long minutes = (elapsedTimeSeconds / 60) % 60;
      long hours = (elapsedTimeSeconds / 3600);

      return String.format("%02d:%02d:%02d", hours, minutes, seconds) + (!glacialMacro.isEnabled() ? " §7(Paused)" : "");
   }

   @Override
   protected boolean shouldShow() {
      if (!super.shouldShow()) return false;
      boolean macroTypeCondition = (VeinForge.config().general.macroType == 1);
      SubLocation currentSub = GameStateHandler.getInstance().getCurrentSubLocation();
      boolean locationCondition = currentSub == SubLocation.GLACITE_TUNNELS || currentSub == SubLocation.DWARVEN_BASE_CAMP || VeinForge.config().commission.glacialCommission.showGlacialHUDOutside;

      return macroTypeCondition && locationCondition;
   }
}
