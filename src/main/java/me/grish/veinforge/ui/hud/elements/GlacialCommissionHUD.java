package me.grish.veinforge.ui.hud.elements;

import lombok.Getter;
import me.grish.veinforge.VeinForge;
import me.grish.veinforge.VeinForgeClient;
import me.grish.veinforge.client.overlay.TextHud;
import me.grish.veinforge.handler.GameStateHandler;
import me.grish.veinforge.macro.impl.GlacialMacro.GlacialMacro;
import me.grish.veinforge.macro.impl.GlacialMacro.GlaciteVeins;
import me.grish.veinforge.util.TablistUtil;
import me.grish.veinforge.util.helper.location.SubLocation;

import java.util.List;
import java.util.Map;

public class GlacialCommissionHUD extends TextHud {

   @Getter
   private static final GlacialCommissionHUD instance = new GlacialCommissionHUD();
   private final transient GlacialMacro glacialMacro = GlacialMacro.getInstance();

   public GlacialCommissionHUD() {
      super();
      this.x = 5;
      this.y = 5;
   }

   @Override
   protected int getAccentColor() {
      return 0xFF38BDF8;
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

      lines.add("§b§lGlacial Commission Macro");
      lines.add("§8§m------------------------");
      lines.add("§8» §7Runtime: §b" + formatElapsedTime(uptime));
      lines.add("§8» §7Commissions completed: §a" + totalComms);
      lines.add("§8» §7Commissions/hour: §3" + commsPerHour);
      lines.add("§8» §7HOTM XP gained: §d" + formatNumberWithK(hotmExpGained));
      lines.add("§8» §7HOTM XP/hour: §d" + formatNumberWithK((long) commsPerHour * 900));
      lines.add("§8§m------------------------");

      if (example) {
         lines.add("§8» §7Status: §eMining");
         lines.add("§8» §7Commission Info:");
         lines.add("   §f- §bGLACITE: §a3/7 §7(§e42.8%§7)");
         lines.add("   §f- §bUMBER: §a5/9 §7(§e61.2%§7)");
         String version = VeinForgeClient.instance != null ? VeinForgeClient.instance.VERSION : "unknown";
         lines.add("§8VeinForge §7v" + version);
         return;
      }

      if (glacialMacro.isEnabled() && glacialMacro.getCurrentState() != null) {
         lines.add("§8» §7Status: §e" + glacialMacro.getCurrentState().getClass().getSimpleName());

         Map<GlaciteVeins, Double> commPercentages = TablistUtil.getGlaciteComs();
         List<GlaciteVeins> typesToMine = glacialMacro.getTypeToMine();

         if (!typesToMine.isEmpty()) {
            lines.add("§8» §7Commission Info:");
            for (GlaciteVeins veinType : typesToMine) {
               double percentage = commPercentages.getOrDefault(veinType, 0.0);

               // Calculate available (non-blacklisted) veins
               int totalVeins = GlaciteVeins.getVeins(veinType).length;
               long blacklistedCount = glacialMacro.getPreviousVeins().keySet().stream()
                                               .filter(pair -> pair.first() == veinType)
                                               .count();
               long availableCount = totalVeins - blacklistedCount;

               String line = String.format("   §f- §b%s: §a%d/%d §7(§e%.1f%%§7)", veinType.toString(), availableCount, totalVeins, percentage);
               lines.add(line);
            }
         }
      }
      String version = VeinForgeClient.instance != null ? VeinForgeClient.instance.VERSION : "unknown";
      lines.add("§8VeinForge §7v" + version);
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
