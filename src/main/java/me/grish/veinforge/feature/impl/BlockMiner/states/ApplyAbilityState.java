package me.grish.veinforge.feature.impl.BlockMiner.states;

import me.grish.veinforge.VeinForge;
import me.grish.veinforge.feature.impl.BlockMiner.BlockMiner;
import me.grish.veinforge.handler.RotationHandler;
import me.grish.veinforge.util.AngleUtil;
import me.grish.veinforge.util.BlockUtil;
import me.grish.veinforge.util.KeyBindUtil;
import me.grish.veinforge.util.helper.Clock;
import me.grish.veinforge.util.helper.RotationConfiguration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * ApplyAbilityState
 * <p>
 * State responsible for activating the mining ability.
 * Waits for 0.2 seconds ({@code timer1}) and then right-clicks.
 * Then waits for 0.2 more seconds ({{@code timer2}}) to transition into the next state
 * <p>
 * If the player uses pickobulus, rotate to the farthest blue wool before right-clicking
 * <p>
 * Automatically throws error if it presses 2 times consecutively
 */
public class ApplyAbilityState implements BlockMinerState {

   private final Minecraft mc = Minecraft.getInstance();
   private final Clock timer1 = new Clock();
   private final Clock timer2 = new Clock();

   private final long COOLDOWN = 200; // 0.2-second cooldown for activating ability

   @Override
   public void onStart(BlockMiner blockMiner) {
      log("Entering Apply Ability State");

      // Start the cooldown timer
      timer2.reset();
      timer1.reset();
      timer1.schedule(COOLDOWN);

      // Check if the pickaxe ability is pickobulus
      if (BlockMiner.getInstance().getPickaxeAbility() == BlockMiner.PickaxeAbility.PICKOBULUS) {
         final BlockPos blueWool = getFarthestBlueWool();

         if (blueWool == null) {
            log("Cannot find blue wool. Skipping the rotation.");
            return;
         }

         final List<Vec3> points = BlockUtil.bestPointsOnBestSide(blueWool);
         Vec3 targetPoint = new Vec3(
                 blueWool.getX() + 0.5,
                 blueWool.getY() + 0.5,
                 blueWool.getZ() + 0.5
         );

         if (!points.isEmpty()) {
            targetPoint = points.get(0);
         }

         if (targetPoint != null) {
            log("Rotating to blue wool");
            RotationHandler.getInstance().easeTo(new RotationConfiguration(
                    AngleUtil.getRotation(targetPoint),
                    VeinForge.config().getRandomRotationTime(),
                    null
            ));
         }
      }

      // Release all keys to prepare for the right click
      if (mc.screen == null) {
         KeyBindUtil.releaseAllExcept();
      }
   }

   @Override
   public BlockMinerState onTick(BlockMiner blockMiner) {

      // If the first timer has ended and the player is not rotating, press right-click
      if (timer1.isScheduled() && timer1.passed() && !RotationHandler.getInstance().isEnabled()) {
         timer1.reset();
         timer2.reset();
         timer2.schedule(COOLDOWN);
         blockMiner.setLastAbilityUse(System.currentTimeMillis());
         KeyBindUtil.rightClick();
      }

      // If the second timer has ended, transition back to the starting state
      if (timer2.isScheduled() && timer2.passed()) {
         return new StartingState();
      }

      // Wait for the timer to expire
      return this;
   }

   private BlockPos getFarthestBlueWool() {
      BlockPos playerBlockPos = mc.player.blockPosition();

      BlockPos farthestBlueWool = null;
      double maxDistance = 0;

      for (int x = -5; x <= 5; x++) {
         for (int y = -5; y <= 5; y++) {
            for (int z = -5; z <= 5; z++) {
               final BlockPos checkPos = playerBlockPos.offset(x, y, z);
               final Block block = mc.level.getBlockState(checkPos).getBlock();

               if (block == Blocks.LIGHT_BLUE_WOOL) {
                  double distance = playerBlockPos.distSqr(checkPos);

                  if (distance > maxDistance) {
                     maxDistance = distance;
                     farthestBlueWool = checkPos;
                  }
               }
            }
         }
      }

      return farthestBlueWool;
   }

   @Override
   public void onEnd(BlockMiner blockMiner) {
      log("Exiting Apply Ability State");
   }
}
