package me.grish.veinforge.feature.impl.AutoGetStats.tasks.impl;

import me.grish.veinforge.VeinForge;
import me.grish.veinforge.feature.impl.AutoGetStats.tasks.AbstractInventoryTask;
import me.grish.veinforge.feature.impl.AutoGetStats.tasks.TaskStatus;
import me.grish.veinforge.util.InventoryUtil;
import me.grish.veinforge.util.helper.Clock;
import net.minecraft.client.Minecraft;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A task that retrieves the Mining boost value from the player's HOTM GUI.
 */
public class MiningBoostRetrievalTask extends AbstractInventoryTask<Integer> {

   private final Minecraft mc = Minecraft.getInstance();
   private final Clock timer = new Clock();
   private Integer speedBoost;

   @Override
   public void init() {
      taskStatus = TaskStatus.RUNNING;

      InventoryUtil.holdItem(VeinForge.config().general.miningTool);

      if (mc.gui.screen() != null) {
         InventoryUtil.closeScreen();
      }

      if (mc.player != null) {
         mc.player.connection.sendCommand("hotm");
      }
      timer.schedule(1000);
   }

   @Override
   public void onTick() {
      if (!timer.passed() && timer.isScheduled()) {
         return;
      }

      final int speedBoostSlot = InventoryUtil.getSlotIdOfItemInContainer("Mining Speed Boost");
      final String speedBoostLore = String.join(" ", InventoryUtil.getLoreOfItemInContainer(speedBoostSlot));

      final Matcher matcher = Pattern.compile("\\+(\\d+)%").matcher(speedBoostLore);
      if (matcher.find()) {
         speedBoost = Integer.parseInt(matcher.group(1));
         taskStatus = TaskStatus.SUCCESS;
         return;
      }

      taskStatus = TaskStatus.FAILURE;
      error = "Cannot parse speed boost. You may have scrolled up in your HOTM GUI.";
   }

   @Override
   public void end() {
      InventoryUtil.closeScreen();
   }

   @Override
   public Integer getResult() {
      return speedBoost;
   }
}
