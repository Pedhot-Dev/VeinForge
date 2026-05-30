package me.grish.veinforge.feature.impl;

import kotlin.Pair;
import lombok.Getter;
import lombok.Setter;
import me.grish.veinforge.VeinForge;
import me.grish.veinforge.handler.RotationHandler;
import me.grish.veinforge.pathfinder.calculate.Path;
import me.grish.veinforge.pathfinder.movement.CalculationContext;
import me.grish.veinforge.pathfinder.movement.MovementHelper;
import me.grish.veinforge.util.*;
import me.grish.veinforge.util.helper.Angle;
import me.grish.veinforge.util.helper.Clock;
import me.grish.veinforge.util.helper.RotationConfiguration;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.*;
import java.util.List;

public class PathExecutor {

   // Constants for anti-cheat/human-like behavior
   private static final double FORWARD_KEY_RELEASE_PROBABILITY = 0.02;
   private static final int MAX_YAW_DIFF_FOR_SPRINT = 40;
   private static final int JUMP_DELAY_MIN = 80;
   private static final int JUMP_DELAY_RANDOM = 120;
   private static final double ROTATION_HUMAN_ERROR_FACTOR = 1.2;
   private static final double COLLISION_CHECK_DISTANCE = 0.7;
   private static final int SEGMENT_TIMEOUT_MS = 30000; // 30 seconds per path segment
   private static final double STEP_UP_NO_JUMP_RISE = 0.6;
   private static final double MIN_FORWARD_RISE = -0.05;
   private static final double MAX_JUMPABLE_RISE = 1.25;
   private static final double MAX_JUMP_PROBE_DISTANCE = 2.2;
   private static final double NODE_REACHED_HORIZONTAL_DIST = 0.7;
   private static final double NODE_REACHED_VERTICAL_TOLERANCE = 1.35;
   private static final double SEGMENT_PROGRESS_SWITCH_THRESHOLD = 0.65;
   private static final double STUCK_SPEED_THRESHOLD = 0.05;
   private static final int STUCK_DETECTION_MS = 1000;
   private static final int STUCK_RECOVERY_TIMEOUT_MS = 700;
   private static PathExecutor instance;
   private final Minecraft mc = Minecraft.getInstance();
   @Getter
   private final Deque<Path> pathQueue = new LinkedList<>();
   private final Map<Long, List<Long>> map = new HashMap<>();
   private final List<BlockPos> blockPath = new ArrayList<>();
   private final Clock stuckTimer = new Clock();
   private final Clock stuckRecoveryWindow = new Clock();
   private final Clock nodeSwitchDelay = new Clock();
   private final Clock jumpDelay = new Clock();
   private final Clock segmentTimeout = new Clock();
   private final List<Runnable> onFinishCallbacks = new ArrayList<>();
   private final List<Runnable> onFailCallbacks = new ArrayList<>();
   private final Random random = new Random();
   private final Clock dynamicPitch = new Clock();
   @Getter
   private boolean enabled = false;
   @Getter
   private String stopReason = "Not started";
   private Path prev;
   private Path curr;
   private boolean failed = false;
   private boolean succeeded = false;
   private boolean pastTarget = false;
   private boolean attemptedStuckRecovery = false;
   private boolean pendingStuckRecoveryJump = false;
   private double lastPitch = 10 + (15 - 10) * random.nextDouble();
   private int target = 0;
   private int previous = -1;
   private long nodeChangeTime = 0;

   private boolean interpolated = true;
   private float interpolYawDiff = 0f;

   @Getter
   private State state = State.STARTING_PATH;

   @Setter
   private boolean allowSprint = true;
   @Setter
   private boolean allowInterpolation = false;

   public static PathExecutor getInstance() {
      if (instance == null) {
         instance = new PathExecutor();
      }
      return instance;
   }

   public void onFinish(Runnable callback) {
      onFinishCallbacks.add(callback);
   }

   public void onFail(Runnable callback) {
      onFailCallbacks.add(callback);
   }

   public void queuePath(Path path) {
      if (path.getPath().isEmpty()) {
         this.stopReason = "Rejected empty path segment";
         error("Path is empty");
         failed = true;
         return;
      }

      BlockPos start = path.getStart();
      Path lastPath = (this.curr != null) ? this.curr : this.pathQueue.peekLast();

      if (lastPath != null && !lastPath.getGoal().isAtGoal(start.getX(), start.getY(), start.getZ())) {
         this.stopReason = "Rejected disjoint path segment";
         error("This path segment does not start at last path's goal. LastpathGoal: " + lastPath.getGoal() + ", ThisPathStart: " + start);
         failed = true;
         return;
      }

      this.pathQueue.offer(path);
   }

   public void start() {
      this.state = State.STARTING_PATH;
      this.enabled = true;
      this.stopReason = "Running";
   }

   public void stop() {
      stop(this.stopReason);
   }

   public void stop(String reason) {
      if (reason != null && !reason.trim().isEmpty()) {
         this.stopReason = reason.trim();
      }

      this.enabled = false;
      this.pathQueue.clear();
      this.blockPath.clear();
      this.map.clear();
      this.curr = null;
      this.prev = null;
      this.target = 0;
      this.previous = -1;
      this.pastTarget = false;
      this.state = State.END;
      this.interpolYawDiff = 0f;
      this.allowSprint = true;
      this.allowInterpolation = false;
      this.nodeChangeTime = 0;
      this.interpolated = true;
      this.segmentTimeout.reset();
      this.jumpDelay.reset();
      this.stuckTimer.reset();
      this.stuckRecoveryWindow.reset();
      this.attemptedStuckRecovery = false;
      this.pendingStuckRecoveryJump = false;
      StrafeUtil.enabled = false;
      RotationHandler.getInstance().stop();
      KeyBindUtil.releaseAllExcept();
      log("stopped. reason: " + this.stopReason);

      // Execute callbacks
      if (this.succeeded) {
         onFinishCallbacks.forEach(Runnable::run);
      } else if (this.failed) {
         onFailCallbacks.forEach(Runnable::run);
      }
      onFinishCallbacks.clear();
      onFailCallbacks.clear();
   }

   public void clearQueue() {
      this.pathQueue.clear();
      this.curr = null;
      this.succeeded = true;
      this.failed = false;
      this.interpolated = false;
      this.target = 0;
      this.previous = -1;
   }

   public void clearQueuedPaths() {
      this.pathQueue.clear();
      this.succeeded = false;
      this.failed = false;
   }

   public boolean onTick() {
      if (!enabled) {
         return false;
      }

      double horizontalSpeed = Math.hypot(mc.player.getDeltaMovement().x, mc.player.getDeltaMovement().z);
      if (this.attemptedStuckRecovery && this.stuckRecoveryWindow.isScheduled() && this.stuckRecoveryWindow.passed()) {
         if (horizontalSpeed < STUCK_SPEED_THRESHOLD) {
            this.failed = true;
            this.succeeded = false;
            this.stop("Stuck recovery jump failed near " + PlayerUtil.getBlockStandingOn() + " (target index " + this.target + ")");
            return false;
         }
         this.attemptedStuckRecovery = false;
         this.pendingStuckRecoveryJump = false;
         this.stuckRecoveryWindow.reset();
      }

      if (this.stuckTimer.isScheduled() && this.stuckTimer.passed()) {
         if (!this.attemptedStuckRecovery) {
            this.attemptedStuckRecovery = true;
            this.pendingStuckRecoveryJump = true;
            this.stuckRecoveryWindow.schedule(STUCK_RECOVERY_TIMEOUT_MS);
            this.stuckTimer.reset();
            log("Was stuck for a second. Attempting one recovery jump.");
         } else {
            log("Was Stuck For a Second.");
            this.failed = true;
            this.succeeded = false;
            this.stop("Stuck for " + STUCK_DETECTION_MS + "ms near " + PlayerUtil.getBlockStandingOn() + " (target index " + this.target + ")");
            return false;
         }
      }

      if (this.segmentTimeout.isScheduled() && this.segmentTimeout.passed()) {
         error("Path segment timed out after " + SEGMENT_TIMEOUT_MS + "ms");
         this.failed = true;
         this.succeeded = false;
         this.stop("Path segment timeout after " + SEGMENT_TIMEOUT_MS + "ms (target index " + this.target + ")");
      }

      BlockPos playerPos = PlayerUtil.getBlockStandingOn();
      if (this.curr != null) {
         // this is utterly useless but im useless as well
         List<Long> blockHashes = this.map.get(this.pack(playerPos.getX(), playerPos.getZ()));
         int current = -1;
         if (blockHashes != null && !blockHashes.isEmpty()) {
            int bestY = -1;
            double playerY = mc.player.getY();
            for (Long blockHash : blockHashes) {
               Pair<Integer, Integer> block = this.unpack(blockHash);
               int blockY = block.getFirst();
               int blockTarget = block.getSecond();
               if (blockTarget > this.previous) {
                  if (bestY == -1 || (blockY < playerY && blockY > bestY) || (blockY >= playerY && blockY < bestY)) {
                     bestY = block.getFirst();
                     current = blockTarget;
                  }
               }
            }
         }

         if (current != -1 && current > previous) {
            this.previous = current;
            this.target = current + 1;
            this.state = State.TRAVERSING;
            this.pastTarget = false;
            this.interpolated = false;
            this.interpolYawDiff = 0;
            this.nodeChangeTime = System.currentTimeMillis();
            log("changed target from " + this.previous + " to " + this.target);
            RotationHandler.getInstance().stop();
         }

         if (horizontalSpeed < STUCK_SPEED_THRESHOLD) {
            if (!this.stuckTimer.isScheduled()) {
               this.stuckTimer.schedule(STUCK_DETECTION_MS);
            }
         } else {
            this.stuckTimer.reset();
            this.attemptedStuckRecovery = false;
            this.pendingStuckRecoveryJump = false;
            this.stuckRecoveryWindow.reset();
         }
      } else {
         if (this.stuckTimer.isScheduled()) {
            this.stuckTimer.reset();
         }
         this.attemptedStuckRecovery = false;
         this.pendingStuckRecoveryJump = false;
         this.stuckRecoveryWindow.reset();
         if (this.pathQueue.isEmpty()) {
            return true;
         }
      }

      if (this.curr == null || this.target == this.blockPath.size()) {
         log("Path traversed");
         if (this.pathQueue.isEmpty()) {
            log("Pathqueue is empty");
            if (this.curr != null) {
               this.curr = null;
               this.target = 0;
               this.previous = -1;
            }
            this.state = State.WAITING;
            this.stopReason = "Path queue drained; waiting";
            return true;
         }
         this.succeeded = true;
         this.failed = false;
         this.prev = this.curr;
         this.target = 1;
         this.previous = 0;
         loadPath(this.pathQueue.poll());
         if (this.target == this.blockPath.size()) {
            return true;
         }
         log("loaded new path target: " + this.target + ", prev: " + this.previous);
      }

      BlockPos target = this.blockPath.get(this.target);
      Vec3 playerPosVec = mc.player.position();
      double horizontalDistToCurrent = Math.hypot(playerPosVec.x - target.getX() - 0.5, playerPosVec.z - target.getZ() - 0.5);
      double verticalDistToCurrent = Math.abs(mc.player.getY() - target.getY());
      boolean closeToCurrentNode = horizontalDistToCurrent <= NODE_REACHED_HORIZONTAL_DIST
              && verticalDistToCurrent <= NODE_REACHED_VERTICAL_TOLERANCE;

      if (this.target < this.blockPath.size() - 1) {
         BlockPos nextTarget = this.blockPath.get(this.target + 1);
         Vec3 currentCenter = new Vec3(target.getX() + 0.5, 0.0, target.getZ() + 0.5);
         Vec3 nextCenter = new Vec3(nextTarget.getX() + 0.5, 0.0, nextTarget.getZ() + 0.5);
         Vec3 segment = nextCenter.subtract(currentCenter);
         double segmentLengthSqr = segment.x * segment.x + segment.z * segment.z;
         boolean passedCurrentNode = false;
         if (segmentLengthSqr > 1.0E-6) {
            Vec3 playerFromCurrent = new Vec3(playerPosVec.x, 0.0, playerPosVec.z).subtract(currentCenter);
            double projectedProgress = (playerFromCurrent.x * segment.x + playerFromCurrent.z * segment.z) / segmentLengthSqr;
            passedCurrentNode = projectedProgress >= SEGMENT_PROGRESS_SWITCH_THRESHOLD;
         }
         this.pastTarget = passedCurrentNode;

         if ((closeToCurrentNode || passedCurrentNode) && nodeSwitchDelay.passed()) {
            this.previous = this.target;
            this.target++;
            nodeSwitchDelay.schedule(50 + random.nextInt(70));
            target = this.blockPath.get(this.target);
            log("advanced to next target (" + (closeToCurrentNode ? "close" : "segment progress") + ")");
         }
      }

      boolean onGround = mc.player.onGround();

      int targetX = target.getX();
      int targetZ = target.getZ();
      double horizontalDistToTarget = Math.hypot(mc.player.getX() - targetX - 0.5, mc.player.getZ() - targetZ - 0.5);
      float yaw = AngleUtil.getRotationYaw360(mc.player.position(), new Vec3(targetX + 0.5, 0.0, targetZ + 0.5));
      float yawDiff = Math.abs(AngleUtil.get360RotationYaw() - yaw);

      if (this.interpolYawDiff == 0) {
         this.interpolYawDiff = yaw - AngleUtil.get360RotationYaw();
      }
      // Disable StrafeUtil for realistic client-side movement
      StrafeUtil.enabled = false;

      // Rotate player to face target direction
      if (yawDiff > 3 && !RotationHandler.getInstance().isEnabled()) {
         float rotYaw = yaw;

         // look at a block thats at least 5 blocks away instead of looking at the target which helps reduce buggy rotation
         for (int i = this.target; i < this.blockPath.size(); i++) {
            BlockPos rotationTarget = this.blockPath.get(i);
            if (Math.hypot(mc.player.getX() - rotationTarget.getX(), mc.player.getZ() - rotationTarget.getZ()) > 5) {
               rotYaw = AngleUtil.getRotation(rotationTarget).getYaw(); // Fixed getYaw
               float humanError = (float) (random.nextGaussian() * ROTATION_HUMAN_ERROR_FACTOR);
               rotYaw += humanError;
               break;
            }
         }

         float time = VeinForge.config().debug.useFixedRotation ? VeinForge.config().debug.fixedRotationTime : Math.max(220, (long) (360 - horizontalDistToTarget * VeinForge.config().debug.rotationMultiplier));

         if (!dynamicPitch.isScheduled() || dynamicPitch.passed()) {
            lastPitch = 10 + (15 - 10) * random.nextDouble();
            dynamicPitch.schedule(1000);
         }

         // TODO: Implement back route miner
         RotationHandler.getInstance().easeTo(
                 new RotationConfiguration(
                         new Angle(rotYaw, (float) lastPitch),
                         (long) time, null
                 )
         );
      }

      // Calculate which WASD keys to press based on current player rotation (not target direction)
      // This makes movement purely client-side and realistic
      Vec3 targetVec = new Vec3(targetX + 0.5, mc.player.getY(), targetZ + 0.5);
      List<KeyMapping> neededKeys = KeyBindUtil.getNeededKeyPresses(mc.player.position(), targetVec);
      List<KeyMapping> keyBindings = new ArrayList<>(neededKeys);

      // Preserve attack/use item state
      if (mc.options.keyUse.isDown()) {
         keyBindings.add(mc.options.keyUse);
      }
      if (mc.options.keyAttack.isDown()) {
         keyBindings.add(mc.options.keyAttack);
      }

      // Jump only when path requires a one-block step-up and landing space is valid.
      boolean shouldJump = shouldJumpOneBlock(playerPos, target, horizontalDistToTarget);
      boolean recoveryJumping = false;
      if (shouldJump && onGround && (!this.jumpDelay.isScheduled() || this.jumpDelay.passed())) {
         keyBindings.add(mc.options.keyJump);
         this.jumpDelay.schedule(JUMP_DELAY_MIN + random.nextInt(JUMP_DELAY_RANDOM));
         this.state = State.JUMPING;
      }
      if (this.pendingStuckRecoveryJump && onGround && (!this.jumpDelay.isScheduled() || this.jumpDelay.passed())) {
         keyBindings.add(mc.options.keyJump);
         this.jumpDelay.schedule(JUMP_DELAY_MIN + random.nextInt(JUMP_DELAY_RANDOM));
         this.pendingStuckRecoveryJump = false;
         recoveryJumping = true;
         this.state = State.JUMPING;
         log("Issued stuck recovery jump.");
      }

      // Apply all the calculated key presses
      KeyBindUtil.holdThese(keyBindings.toArray(new KeyMapping[0]));

      // Handle sprinting - only sprint when moving relatively straight
      KeyBindUtil.setKeyBindState(mc.options.keySprint, this.allowSprint && yawDiff < MAX_YAW_DIFF_FOR_SPRINT && !shouldJump && !recoveryJumping);

      return mc.player.position().distanceToSqr(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5) < 100;
   }

   public void loadPath(Path path) {
      this.blockPath.clear();
      this.map.clear();

      this.curr = path;
      this.blockPath.addAll(this.curr.getSmoothedPath());
      for (int i = 0; i < this.blockPath.size(); i++) {
         BlockPos pos = this.blockPath.get(i);
         this.map.computeIfAbsent(this.pack(pos.getX(), pos.getZ()), k -> new ArrayList<>()).add(this.pack(pos.getY(), i));
      }
   }

   public void onRender() {
      if (this.target != -1 && this.target < this.blockPath.size()) {
         BlockPos playerPos = PlayerUtil.getBlockStandingOn();
         BlockPos target = this.blockPath.get(this.target);
         int targetX = target.getX();
         int targetZ = target.getZ();
         Vec3 playerPosVec = mc.player.position();
         float yaw = AngleUtil.getRotationYaw360(playerPosVec, new Vec3(targetX + 0.5, 0.0, targetZ + 0.5));
         Vec3 pos = new Vec3(playerPosVec.x, playerPos.getY() + 0.5, playerPosVec.z);
         Vec3 vec4Rot = AngleUtil.getVectorForRotation(yaw);
         // Keep marker just above the player's feet; this is a delta, not an absolute Y.
         Vec3 newV = pos.add(vec4Rot.x, +1, vec4Rot.z);
         RenderUtil.drawBlock(new BlockPos((int) newV.x, (int) newV.y, (int) newV.z), // BlockPos.ofFloored check?
                 new Color(255, 0, 0, 255));
         RenderUtil.drawBlock(playerPos, new Color(255, 255, 0, 100));
      }
   }

   public Path getPreviousPath() {
      return this.prev;
   }

   public Path getCurrentPath() {
      return this.curr;
   }

   public boolean failed() {
      return !this.enabled && this.failed;
   }

   public boolean ended() {
      return !this.enabled && this.succeeded;
   }

   private boolean shouldJumpOneBlock(BlockPos playerPos, BlockPos targetPos, double horizontalDistToTarget) {
      if (this.curr == null) {
         return false;
      }

      CalculationContext ctx = this.curr.getCtx();
      if (shouldJumpTowardTarget(ctx, playerPos, targetPos, horizontalDistToTarget)) {
         return true;
      }

      if (this.target < this.blockPath.size() - 1) {
         BlockPos nextTarget = this.blockPath.get(this.target + 1);
         double horizontalDistToNext = Math.hypot(mc.player.getX() - nextTarget.getX() - 0.5, mc.player.getZ() - nextTarget.getZ() - 0.5);
         return shouldJumpTowardTarget(ctx, playerPos, nextTarget, horizontalDistToNext);
      }

      return false;
   }

   private boolean shouldJumpTowardTarget(CalculationContext ctx, BlockPos playerPos, BlockPos desiredTarget, double horizontalDist) {
      if (horizontalDist > MAX_JUMP_PROBE_DISTANCE) {
         return false;
      }

      int stepX = Integer.compare(desiredTarget.getX(), playerPos.getX());
      int stepZ = Integer.compare(desiredTarget.getZ(), playerPos.getZ());
      if (stepX == 0 && stepZ == 0) {
         return false;
      }

      return shouldJumpForDirection(ctx, playerPos, stepX, stepZ);
   }

   private boolean shouldJumpForDirection(CalculationContext ctx, BlockPos playerPos, int stepX, int stepZ) {
      if (stepX == 0 && stepZ == 0) {
         return false;
      }

      int landingX = playerPos.getX() + stepX;
      int landingZ = playerPos.getZ() + stepZ;
      int playerY = playerPos.getY();
      double sourceSurfaceY = getSurfaceY(ctx, playerPos.getX(), playerY, playerPos.getZ(), stepX, stepZ);

      double bestRise = Double.POSITIVE_INFINITY;
      for (int y = playerY - 1; y <= playerY + 2; y++) {
         if (!isStandableWithHeadroom(ctx, landingX, y, landingZ)) {
            continue;
         }

         double destSurfaceY = getSurfaceY(ctx, landingX, y, landingZ, stepX, stepZ);
         double rise = destSurfaceY - sourceSurfaceY;
         if (rise < MIN_FORWARD_RISE || rise > MAX_JUMPABLE_RISE) {
            continue;
         }
         bestRise = Math.min(bestRise, rise);
      }

      if (bestRise == Double.POSITIVE_INFINITY) {
         return false;
      }
      return bestRise > STEP_UP_NO_JUMP_RISE;
   }

   private boolean isStandableWithHeadroom(CalculationContext ctx, int x, int y, int z) {
      if (!MovementHelper.INSTANCE.canStandOn(ctx.getBsa(), x, y, z, ctx.get(x, y, z))) {
         return false;
      }
      if (!MovementHelper.INSTANCE.canWalkThrough(ctx.getBsa(), x, y + 1, z, ctx.get(x, y + 1, z))) {
         return false;
      }
      return MovementHelper.INSTANCE.canWalkThrough(ctx.getBsa(), x, y + 2, z, ctx.get(x, y + 2, z));
   }

   private double getSurfaceY(CalculationContext ctx, int x, int y, int z, int stepX, int stepZ) {
      BlockState state = ctx.get(x, y, z);
      return y + getSurfaceOffset(ctx, state, x, y, z, stepX, stepZ);
   }

   private double getSurfaceOffset(CalculationContext ctx, BlockState state, int x, int y, int z, int stepX, int stepZ) {
      if (state.getBlock() instanceof SlabBlock || state.getBlock() instanceof StairBlock) {
         return MovementHelper.INSTANCE.hasTop(state, stepX, stepZ) ? 1.0 : 0.5;
      }

      double maxY = MovementHelper.INSTANCE.collisionMaxY(state, ctx.getWorld(), new BlockPos(x, y, z));
      return Math.max(0.0, maxY);
   }

   private long pack(int x, int z) {
      return ((long) x << 32) | (z & 0xFFFFFFFFL);
   }

   public Pair<Integer, Integer> unpack(long packed) {
      return new Pair<>((int) (packed >> 32), (int) packed);
   }

   void log(String message) {
      Logger.sendLog(getMessage(message));
   }

   void send(String message) {
      Logger.sendMessage(getMessage(message));
   }

   void error(String message) {
      Logger.sendError(getMessage(message));
   }

   void note(String message) {
      Logger.sendNote(getMessage(message));
   }

   String getMessage(String message) {
      return "[PathExecutor] " + message;
   }

   // Deprecated? Anti-cheat method not fully implemented in old code?
   private boolean shouldAvoidForwardMovement(float yaw, BlockPos playerPos) {
      Vec3 eye = PlayerUtil.getPlayerEyePos();
      Vec3 forward = AngleUtil.getVectorForRotation(yaw);
      Vec3 probe = eye.add(forward.x * COLLISION_CHECK_DISTANCE, 0, forward.z * COLLISION_CHECK_DISTANCE);
      HitResult hit = RaytracingUtil.fastRayTrace(mc.player, eye, probe, Collections.emptyList());
      if (hit != null && (hit.getType() == HitResult.Type.BLOCK || hit.getType() == HitResult.Type.ENTITY)) {
         return true;
      }

      BlockPos ahead = BlockPos.containing(
              mc.player.getX() + forward.x * COLLISION_CHECK_DISTANCE,
              playerPos.getY(),
              mc.player.getZ() + forward.z * COLLISION_CHECK_DISTANCE
      );
      return !BlockUtil.canStandOn(ahead);
   }

   enum State {
      STARTING_PATH, TRAVERSING, JUMPING, WAITING, END
   }
}
