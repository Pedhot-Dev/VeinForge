package me.grish.veinforge.feature.impl.rift;

import me.grish.veinforge.VeinForge;
import me.grish.veinforge.event.BlockChangeEvent;
import me.grish.veinforge.util.KeyBindUtil;
import me.grish.veinforge.util.Logger;
import me.grish.veinforge.util.RenderUtil;
import me.grish.veinforge.util.helper.Clock;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class AutoStunSnake {

   private static final String FROZEN_WATER_PUNGI_ID = "FROZEN_WATER_PUNGI";
   private static final int MAX_TAIL_LENGTH = 12;
   private static final long TAIL_TIMEOUT_MS = 3000L;
   private static final long USE_COOLDOWN_MS = 500L;
   private static final long STATUS_LOG_COOLDOWN_MS = 2000L;
   private static final int MIN_SWAP_BACK_DELAY_MS = 150;
   private static final int MAX_SWAP_BACK_DELAY_MS = 350;

   private static final AutoStunSnake instance = new AutoStunSnake();
   private final Minecraft mc = Minecraft.getInstance();
   private final Clock useCooldown = new Clock();
   private final Clock missingItemLogCooldown = new Clock();
   private final Clock statusLogCooldown = new Clock();
   private final CopyOnWriteArrayList<SnakeTrail> trails = new CopyOnWriteArrayList<>();
   private BlockPos pendingLapis;
   private BlockPos pendingGlass;

   public static AutoStunSnake getInstance() {
      return instance;
   }

   public void onTick() {
      if (!VeinForge.config().rift.riftAutoStunSnake) {
         return;
      }
      if (mc.level == null || mc.player == null) {
         logStatus("Skipping: world or player is null.");
         reset();
         return;
      }
      trails.removeIf(this::shouldRemoveTrail);
      if (mc.gameMode == null || mc.gameMode.isDestroying()) {
         logStatus("Skipping: player is hitting a block.");
         return;
      }
      if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) {
         logStatus("Skipping: no block targeted.");
         return;
      }
      BlockPos hitPos = ((BlockHitResult) mc.hitResult).getBlockPos();
      BlockState state = mc.level.getBlockState(hitPos);
      if (!isStunBlock(state)) {
         logStatus("Skipping: target is not a stun block.");
         return;
      }
      if (useCooldown.isScheduled() && !useCooldown.passed()) {
         logStatus("Skipping: use cooldown active.");
         return;
      }
      int slot = findHotbarSlotById(FROZEN_WATER_PUNGI_ID);
      if (slot == -1) {
         logMissingItem();
         return;
      }
      useCooldown.schedule(USE_COOLDOWN_MS);
      useItemFromHotbar(slot);
      Logger.sendLog("[Rift] AutoStunSnake used Frozen Water Pungi.");
   }

   public void onBlockChange(BlockChangeEvent event) {
      if (!VeinForge.config().rift.riftAutoStunSnake) {
         return;
      }
      if (mc.level == null) {
         return;
      }
      handleBlockUpdate(event.pos(), event.newState());
   }

   public void onWorldRender(WorldRenderContext context) {
      if (!VeinForge.config().rift.riftPredictSnakeTail || trails.isEmpty() || mc.level == null) {
         return;
      }
      for (SnakeTrail trail : trails) {
         if (trail.positions.size() <= 1) {
            continue;
         }
         for (int i = 1; i < trail.positions.size(); i++) {
            BlockPos prev = trail.positions.get(i - 1);
            BlockPos curr = trail.positions.get(i);
            RenderUtil.drawLine(
                    new Vec3(prev.getX() + 0.5D, prev.getY() + 0.5D, prev.getZ() + 0.5D),
                    new Vec3(curr.getX() + 0.5D, curr.getY() + 0.5D, curr.getZ() + 0.5D),
                    new Color(0, 255, 0, 255)
            );
         }
      }
   }

   public void onWorldUnload() {
      reset();
   }

   private void reset() {
      trails.clear();
      pendingLapis = null;
      pendingGlass = null;
      useCooldown.reset();
   }

   private void logStatus(String message) {
      if (statusLogCooldown.isScheduled() && !statusLogCooldown.passed()) {
         return;
      }
      statusLogCooldown.schedule(STATUS_LOG_COOLDOWN_MS);
      Logger.sendLog("[Rift] AutoStunSnake: " + message);
   }

   private boolean shouldRemoveTrail(SnakeTrail trail) {
      if (System.currentTimeMillis() - trail.lastUpdatedMs > TAIL_TIMEOUT_MS) {
         return true;
      }
      if (trail.positions.size() > MAX_TAIL_LENGTH) {
         return true;
      }
      BlockPos head = trail.positions.get(0);
      BlockState state = mc.level.getBlockState(head);
      return !isAnyStainedGlass(state) && !state.isAir();
   }

   private void handleBlockUpdate(BlockPos pos, BlockState state) {
      SnakeTrail trail;
      if (!isAnyStainedGlass(state) && !state.is(Blocks.LAPIS_BLOCK)) {
         trail = findTrailContaining(pos);
         if (trail == null) {
            return;
         }
         trail.positions.remove(pos);
         trail.positions.remove(pos);
         trail.touch();
         if (trail.positions.isEmpty()) {
            trails.remove(trail);
            Logger.sendLog("[Rift] AutoStunSnake trail cleared.");
         }
         return;
      }

      if (state.is(Blocks.LAPIS_BLOCK)) {
         if (pendingGlass == null) {
            pendingLapis = pos;
            return;
         }
         if (pos.distSqr(pendingGlass) > 3.0D) {
            pendingLapis = pos;
            return;
         }
         trail = findTrailContaining(pendingGlass);
         if (trail == null) {
            trail = new SnakeTrail(pendingGlass);
            trails.add(trail);
            Logger.sendLog("[Rift] AutoStunSnake trail created.");
         }
         trail.add(pos);
         pendingGlass = null;
      }

      if (isAnyStainedGlass(state)) {
         if (pendingLapis != null) {
            if (pos.distSqr(pendingLapis) > 3.0D) {
               pendingGlass = pos;
               return;
            }
            trail = findTrailContaining(pos);
            if (trail == null) {
               trail = new SnakeTrail(pos);
               trails.add(trail);
               Logger.sendLog("[Rift] AutoStunSnake trail created.");
            }
            trail.add(pendingLapis);
            pendingLapis = null;
         } else {
            pendingGlass = pos;
         }
      }
   }

   private SnakeTrail findTrailContaining(BlockPos pos) {
      for (SnakeTrail trail : trails) {
         if (trail.positions.contains(pos)) {
            return trail;
         }
      }
      return null;
   }

   private boolean isStunBlock(BlockState state) {
      if (state.is(Blocks.LAPIS_BLOCK)) {
         return true;
      }
      if (!isAnyStainedGlass(state)) {
         return false;
      }
      return state.is(Blocks.BLUE_STAINED_GLASS) || state.is(Blocks.LIGHT_BLUE_STAINED_GLASS);
   }

   private int findHotbarSlotById(String itemId) {
      if (itemId == null || itemId.trim().isEmpty() || mc.player == null) {
         return -1;
      }
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (stack.isEmpty()) {
            continue;
         }
         if (itemId.equals(getItemId(stack))) {
            return i;
         }
      }
      return -1;
   }

   private void logMissingItem() {
      if (missingItemLogCooldown.isScheduled() && !missingItemLogCooldown.passed()) {
         return;
      }
      missingItemLogCooldown.schedule(5000L);
      Logger.sendLog("[Rift] AutoStunSnake: Frozen Water Pungi not found in hotbar.");
   }

   private void useItemFromHotbar(int slot) {
      if (mc.player == null) {
         return;
      }
      int previousSlot = mc.player.getInventory().getSelectedSlot();
      if (previousSlot != slot) {
         mc.player.getInventory().setSelectedSlot(slot);
      }
      KeyBindUtil.resetRightClickDelayTimer();
      KeyBindUtil.rightClick();
      if (previousSlot == slot) {
         return;
      }
      int swapBackDelayMs = java.util.concurrent.ThreadLocalRandom.current()
                                    .nextInt(MIN_SWAP_BACK_DELAY_MS, MAX_SWAP_BACK_DELAY_MS + 1);
      VeinForge.executor().execute(() -> {
         try {
            Thread.sleep(swapBackDelayMs);
         } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
         }
         mc.execute(() -> {
            if (mc.player != null) {
               mc.player.getInventory().setSelectedSlot(previousSlot);
            }
         });
      });
   }

   private String getItemId(ItemStack stack) {
      if (stack.isEmpty()) {
         return "";
      }
      return me.grish.veinforge.util.InventoryUtil.getItemId(stack);
   }

   private boolean isAnyStainedGlass(BlockState state) {
      return state.getBlock() instanceof StainedGlassBlock;
   }

   private static final class SnakeTrail {
      private final CopyOnWriteArrayList<BlockPos> positions = new CopyOnWriteArrayList<>();
      private long lastUpdatedMs;

      private SnakeTrail(BlockPos start) {
         positions.add(start);
         lastUpdatedMs = System.currentTimeMillis();
      }

      private void add(BlockPos pos) {
         positions.add(pos);
         touch();
      }

      private void touch() {
         lastUpdatedMs = System.currentTimeMillis();
      }
   }
}
