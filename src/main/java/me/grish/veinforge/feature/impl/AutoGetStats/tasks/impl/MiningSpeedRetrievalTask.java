package me.grish.veinforge.feature.impl.AutoGetStats.tasks.impl;

import me.grish.veinforge.VeinForge;
import me.grish.veinforge.feature.impl.AutoGetStats.tasks.AbstractInventoryTask;
import me.grish.veinforge.feature.impl.AutoGetStats.tasks.TaskStatus;
import me.grish.veinforge.util.InventoryUtil;
import me.grish.veinforge.util.helper.Clock;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A task that retrieves the Mining Speed value from the player's SkyBlock GUI.
 */
public class MiningSpeedRetrievalTask extends AbstractInventoryTask<Integer> {

   private static final Pattern MINING_SPEED_PATTERN = Pattern.compile("Mining Speed\\s+([\\d,]+\\.?\\d*)");
   private final Minecraft mc = Minecraft.getInstance();
   private final Clock timer = new Clock();
   private Integer miningSpeed;

   @Override
   public void init() {
      taskStatus = TaskStatus.RUNNING;

      InventoryUtil.holdItem(VeinForge.config().general.miningTool);

      if (!InventoryUtil.getInventoryName().equals("Your Equipment and Stats")) {
         if (mc.gui.screen() != null) {
            InventoryUtil.closeScreen();
         }

         if (mc.player != null) {
            mc.player.connection.sendCommand("stats");
         }
      }

      timer.schedule(1000);
   }

   @Override
   public void onTick() {
      if (!timer.passed() && timer.isScheduled()) {
         return;
      }

      if (!InventoryUtil.getInventoryName().equals("Your Equipment and Stats")) {
         taskStatus = TaskStatus.FAILURE;
         error = "Cannot open Stats Menu";
         return;
      }

      List<String> loreList = InventoryUtil.getItemLoreFromOpenContainer("Mining Stats");
      for (String lore : loreList) {
         Matcher matcher = MINING_SPEED_PATTERN.matcher(lore);
         if (matcher.find()) {
            try {
               // The number - for example, "2,000" or "123.45" or "1,234.56"
               String numberAsString = matcher.group(1);
               String cleanNumberString = numberAsString.replace(",", "");

               // Mining speeds from the 'stats menu' can be a decimal
               double rawMiningSpeed = Double.parseDouble(cleanNumberString);
               miningSpeed = (int) rawMiningSpeed;

               taskStatus = TaskStatus.SUCCESS;
               return;
            } catch (NumberFormatException e) {
               taskStatus = TaskStatus.FAILURE;
               error = "Found 'Mining Speed' but failed to parse the number in line: '" + lore + "'. Exiting with error: " + e.getMessage();
               return;
            }
         }
      }

      taskStatus = TaskStatus.FAILURE;
      error = "Failed to get mining speed in GUI";
   }

   @Override
   public void end() {
      InventoryUtil.closeScreen();
   }

   @Override
   public Integer getResult() {
      return miningSpeed;
   }
}
