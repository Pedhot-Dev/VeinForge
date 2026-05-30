package me.grish.veinforge.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Utility class for raytracing and collision detection.
 */
public class RaytracingUtil {

   private static final Minecraft mc = Minecraft.getInstance();

   /**
    * Checks if the player can see a specific point.
    */
   public static boolean canSeePoint(Vec3 point) {
      return canSeePoint(PlayerUtil.getPlayerEyePos(), point);
   }

   /**
    * Checks if there is a line of sight between two points (block-only).
    */
   public static boolean canSeePoint(Vec3 from, Vec3 point) {
      final HitResult result = raytrace(from, point);
      if (result == null || result.getType() == HitResult.Type.MISS) {
         return true;
      }

      final Vec3 r = result.getLocation();
      return r.distanceToSqr(point) < 0.01;
   }

   /**
    * Checks if the player can see a specific point (block and entity collision).
    */
   public static boolean canSeePointWithEntities(Vec3 point) {
      return canSeePointWithEntities(PlayerUtil.getPlayerEyePos(), point);
   }

   /**
    * Checks if there is a line of sight between two points (block and entity collision).
    */
   public static boolean canSeePointWithEntities(Vec3 from, Vec3 point) {
      final HitResult result = raytraceWithEntities(from, point, e -> true);
      if (result == null || result.getType() == HitResult.Type.MISS) {
         return true;
      }

      if (result.getType() != HitResult.Type.BLOCK) {
         return false;
      }

      return result.getLocation().distanceToSqr(point) < 0.01;
   }

   /**
    * Raytrace towards a direction from a position for a set distance (block-only).
    */
   public static HitResult raytraceTowards(Vec3 v1, Vec3 v2, double distance) {
      Vec3 normalized = v2.subtract(v1).normalize();
      return raytrace(v1, v1.add(normalized.x * distance, normalized.y * distance, normalized.z * distance));
   }

   /**
    * Performs a standard block raytrace using vanilla ClipContext.
    */
   public static HitResult raytrace(Vec3 v1, Vec3 v2) {
      if (mc.level == null || mc.player == null) return null;
      return mc.level.clip(new ClipContext(
              v1,
              v2,
              ClipContext.Block.COLLIDER,
              ClipContext.Fluid.NONE,
              mc.player
      ));
   }

   /**
    * Performs a raytrace that includes both blocks and entities using vanilla logic.
    */
   public static HitResult raytraceWithEntities(Vec3 from, Vec3 to, Predicate<Entity> predicate) {
      if (mc.level == null || mc.player == null) return null;

      HitResult blockHit = raytrace(from, to);
      double bestDistSq = (blockHit == null || blockHit.getType() == HitResult.Type.MISS)
                                  ? from.distanceToSqr(to)
                                  : from.distanceToSqr(blockHit.getLocation());
      EntityHitResult bestEntityHit = null;

      AABB searchBox = new AABB(from, to).inflate(1.0);

      for (Entity entity : mc.level.getEntities(mc.player, searchBox, e -> e != mc.player && e.isAlive() && predicate.test(e))) {
         AABB bb = entity.getBoundingBox().inflate(0.3);
         Optional<Vec3> hit = bb.clip(from, to);
         if (!hit.isPresent()) {
            continue;
         }

         double distSq = from.distanceToSqr(hit.get());
         if (distSq < bestDistSq) {
            bestDistSq = distSq;
            bestEntityHit = new EntityHitResult(entity, hit.get());
         }
      }

      return bestEntityHit != null ? bestEntityHit : blockHit;
   }

   // --- Optimized DDA Raycast Section ---

   /**
    * Proxy to fastRayTrace for block-specific raytracing.
    */
   public static HitResult rayTraceToBlocks(Vec3 startVec, Vec3 endVec, List<Block> blocks) {
      return fastRayTrace(startVec, endVec, blocks);
   }

   /**
    * Performs a fast raytrace using DDA algorithm, detecting both blocks and entities.
    * This is highly optimized for performance.
    */
   public static HitResult fastRayTrace(Entity sourceEntity, Vec3 startVec, Vec3 endVec, List<Block> targetBlocks) {
      if (mc.level == null) return BlockHitResult.miss(endVec, Direction.UP, BlockPos.containing(endVec));

      // --- STAGE 1: Block Raytrace (Fast DDA Algorithm) ---

      // Default result is a miss at the end of the line
      HitResult resultBlockHit = BlockHitResult.miss(
              endVec,
              Direction.getApproximateNearest(
                      endVec.x - startVec.x,
                      endVec.y - startVec.y,
                      endVec.z - startVec.z
              ),
              BlockPos.containing(endVec)
      );

      // Grid coordinates for DDA iteration
      int startX = Mth.floor(startVec.x);
      int startY = Mth.floor(startVec.y);
      int startZ = Mth.floor(startVec.z);

      int endX = Mth.floor(endVec.x);
      int endY = Mth.floor(endVec.y);
      int endZ = Mth.floor(endVec.z);

      double currentX = startVec.x;
      double currentY = startVec.y;
      double currentZ = startVec.z;

      // Check starting block
      BlockPos startPos = new BlockPos(startX, startY, startZ);
      BlockState startState = mc.level.getBlockState(startPos);

      // Optimization: check if start block matches target or is a conductor
      if ((targetBlocks.isEmpty() && startState.isRedstoneConductor(mc.level, startPos)) || targetBlocks.contains(startState.getBlock())) {
         BlockHitResult startHit = startState.getCollisionShape(mc.level, startPos).clip(startVec, endVec, startPos);
         if (startHit != null) {
            resultBlockHit = startHit;
         }
      }

      // Proceed with DDA traversal if no hit in the starting block
      if (resultBlockHit.getType() == HitResult.Type.MISS) {
         int maxSteps = 200;

         // Step directions for each axis
         int stepX = endX > startX ? 1 : -1;
         int stepY = endY > startY ? 1 : -1;
         int stepZ = endZ > startZ ? 1 : -1;

         double dx = endVec.x - startVec.x;
         double dy = endVec.y - startVec.y;
         double dz = endVec.z - startVec.z;

         // Current block coordinates in the grid iteration
         int currBlockX = startX;
         int currBlockY = startY;
         int currBlockZ = startZ;

         while (maxSteps-- >= 0) {
            if (currBlockX == endX && currBlockY == endY && currBlockZ == endZ) {
               break;
            }

            // Plane exit determination logic
            boolean xDifferent = currBlockX != endX;
            boolean yDifferent = currBlockY != endY;
            boolean zDifferent = currBlockZ != endZ;

            double xExit = xDifferent ? (stepX > 0 ? currBlockX + 1.0 : currBlockX) : Double.MAX_VALUE;
            double yExit = yDifferent ? (stepY > 0 ? currBlockY + 1.0 : currBlockY) : Double.MAX_VALUE;
            double zExit = zDifferent ? (stepZ > 0 ? currBlockZ + 1.0 : currBlockZ) : Double.MAX_VALUE;

            double xDist = xDifferent ? (xExit - currentX) / dx : Double.MAX_VALUE;
            double yDist = yDifferent ? (yExit - currentY) / dy : Double.MAX_VALUE;
            double zDist = zDifferent ? (zExit - currentZ) / dz : Double.MAX_VALUE;

            // Safeguard against NaN or negative-zero values
            if (xDist < 0) xDist = Double.MAX_VALUE;
            if (yDist < 0) yDist = Double.MAX_VALUE;
            if (zDist < 0) zDist = Double.MAX_VALUE;

            Direction exitFace;

            if (xDist < yDist && xDist < zDist) {
               exitFace = stepX > 0 ? Direction.WEST : Direction.EAST;
               currentX = xExit;
               currentY += dy * xDist;
               currentZ += dz * xDist;
               currBlockX += stepX;
            } else if (yDist < zDist) {
               exitFace = stepY > 0 ? Direction.DOWN : Direction.UP;
               currentX += dx * yDist;
               currentY = yExit;
               currentZ += dz * yDist;
               currBlockY += stepY;
            } else {
               exitFace = stepZ > 0 ? Direction.NORTH : Direction.SOUTH;
               currentX += dx * zDist;
               currentY += dy * zDist;
               currentZ = zExit;
               currBlockZ += stepZ;
            }

            BlockPos newPos = new BlockPos(currBlockX, currBlockY, currBlockZ);
            BlockState newState = mc.level.getBlockState(newPos);
            Block newBlock = newState.getBlock();

            // Predicate: should we check this block for actual collision?
            boolean shouldCheckBlock = targetBlocks.isEmpty()
                                               ? newState.isRedstoneConductor(mc.level, newPos)
                                               : targetBlocks.contains(newBlock);

            if (shouldCheckBlock) {
               BlockHitResult hit = newState.getCollisionShape(mc.level, newPos).clip(startVec, endVec, newPos);
               if (hit != null) {
                  resultBlockHit = hit;
                  break; // Block hit found, terminate traversal
               }
            }
         }
      }

      // --- STAGE 2: Entity Raytrace ---

      // Determine effective end of the ray (at block hit or end of range)
      Vec3 effectiveEndVec = resultBlockHit.getLocation();
      double distToBlockSqr = startVec.distanceToSqr(effectiveEndVec);

      // Broad-phase entity check using inflated AABB path
      AABB area = new AABB(startVec, effectiveEndVec).inflate(1.0);

      // Vanilla utility for fast entity collision
      EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
              sourceEntity,
              startVec,
              effectiveEndVec,
              area,
              (e) -> !e.isSpectator() && e.isPickable(),
              distToBlockSqr
      );

      // --- STAGE 3: Select Winner ---
      // If entity hit is closer than (or equal to) block hit, return entity
      if (entityHit != null) {
         return entityHit;
      }

      return resultBlockHit;
   }


   /**
    * Performs a fast block-only raytrace using DDA logic.
    */
   public static HitResult fastRayTrace(Vec3 startVec, Vec3 endVec, List<Block> targetBlocks) {
      if (mc.level == null) return BlockHitResult.miss(endVec, Direction.UP, BlockPos.containing(endVec));

      // Convert coordinates to block grid
      int startX = (int) Math.floor(startVec.x);
      int startY = (int) Math.floor(startVec.y);
      int startZ = (int) Math.floor(startVec.z);

      int endX = (int) Math.floor(endVec.x);
      int endY = (int) Math.floor(endVec.y);
      int endZ = (int) Math.floor(endVec.z);

      // Start block check
      BlockPos startPos = new BlockPos(startX, startY, startZ);
      BlockState startState = mc.level.getBlockState(startPos);

      if ((targetBlocks.isEmpty() && startState.isRedstoneConductor(mc.level, startPos)) || targetBlocks.contains(startState.getBlock())) {
         BlockHitResult startHit = startState.getCollisionShape(mc.level, startPos).clip(startVec, endVec, startPos);
         if (startHit != null) {
            return startHit;
         }
      }

      HitResult closestHit = null;
      int maxSteps = 200; // Loop safety

      while (maxSteps-- >= 0) {
         if (startX == endX && startY == endY && startZ == endZ) {
            return closestHit != null ? closestHit : BlockHitResult.miss(endVec, Direction.getApproximateNearest(endVec.x - startVec.x, endVec.y - startVec.y, endVec.z - startVec.z), BlockPos.containing(endVec));
         }

         boolean xDifferent = startX != endX;
         boolean yDifferent = startY != endY;
         boolean zDifferent = startZ != endZ;

         double xExit = xDifferent ? (endX > startX ? startX + 1.0 : startX) : 999.0;
         double yExit = yDifferent ? (endY > startY ? startY + 1.0 : startY) : 999.0;
         double zExit = zDifferent ? (endZ > startZ ? startZ + 1.0 : startZ) : 999.0;

         double dx = endVec.x - startVec.x;
         double dy = endVec.y - startVec.y;
         double dz = endVec.z - startVec.z;

         double xDist = xDifferent ? (xExit - startVec.x) / dx : 999.0;
         double yDist = yDifferent ? (yExit - startVec.y) / dy : 999.0;
         double zDist = zDifferent ? (zExit - startVec.z) / dz : 999.0;

         if (xDist == -0.0) xDist = -1.0E-4;
         if (yDist == -0.0) yDist = -1.0E-4;
         if (zDist == -0.0) zDist = -1.0E-4;

         Direction exitFace;

         if (xDist < yDist && xDist < zDist) {
            exitFace = endX > startX ? Direction.WEST : Direction.EAST;
            startVec = new Vec3(xExit, startVec.y + dy * xDist, startVec.z + dz * xDist);
         } else if (yDist < zDist) {
            exitFace = endY > startY ? Direction.DOWN : Direction.UP;
            startVec = new Vec3(startVec.x + dx * yDist, yExit, startVec.z + dz * yDist);
         } else {
            exitFace = endZ > startZ ? Direction.NORTH : Direction.SOUTH;
            startVec = new Vec3(startVec.x + dx * zDist, startVec.y + dy * zDist, zExit);
         }

         // Move to next block in grid using direction logic
         startX = Mth.floor(startVec.x) - (exitFace == Direction.EAST ? 1 : 0);
         startY = Mth.floor(startVec.y) - (exitFace == Direction.UP ? 1 : 0);
         startZ = Mth.floor(startVec.z) - (exitFace == Direction.SOUTH ? 1 : 0);

         BlockPos newPos = new BlockPos(startX, startY, startZ);
         BlockState newState = mc.level.getBlockState(newPos);
         Block newBlock = newState.getBlock();

         boolean shouldCheckBlock = targetBlocks.isEmpty()
                                            ? newState.isRedstoneConductor(mc.level, newPos)
                                            : targetBlocks.contains(newBlock);

         if (shouldCheckBlock) {
            BlockHitResult hit = newState.getCollisionShape(mc.level, newPos).clip(startVec, endVec, newPos);
            if (hit != null) {
               return hit;
            }
         }

         // Temporary hit to return as miss if nothing found by end
         final Vec3 currentPos = startVec;
         closestHit = new HitResult(currentPos) {
            @Override
            public Type getType() {
               return Type.MISS;
            }
         };
      }

      return closestHit;
   }

   /**
    * Logic for finding the entity or block directly under the crosshair.
    */
   public static HitResult findCrosshairTarget(Entity camera, double blockInteractionRange, double entityInteractionRange, float tickProgress) {
      double d = Math.max(blockInteractionRange, entityInteractionRange);
      double e = Mth.square(d);
      Vec3 startVec = camera.getEyePosition(tickProgress);
      HitResult hitResult = camera.pick(d, tickProgress, false);
      double f = hitResult.getLocation().distanceToSqr(startVec);
      if (hitResult.getType() != HitResult.Type.MISS) {
         e = f;
         d = Math.sqrt(f);
      }

      Vec3 viewVec = camera.getViewVector(tickProgress);
      Vec3 endVec = startVec.add(viewVec.x * d, viewVec.y * d, viewVec.z * d);
      AABB box = camera.getBoundingBox().expandTowards(viewVec.scale(d)).inflate(1.0D, 1.0D, 1.0D);
      EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(camera, startVec, endVec, box, EntitySelector.CAN_BE_PICKED, e);

      return entityHitResult != null && entityHitResult.getLocation().distanceToSqr(startVec) < f
                     ? ensureTargetInRange(entityHitResult, startVec, entityInteractionRange)
                     : ensureTargetInRange(hitResult, startVec, blockInteractionRange);
   }

   /**
    * Ensures the hit result is within interaction limits.
    */
   private static HitResult ensureTargetInRange(HitResult hitResult, Vec3 cameraPos, double interactionRange) {
      Vec3 loc = hitResult.getLocation();
      if (!loc.closerThan(cameraPos, interactionRange)) {
         Direction direction = Direction.getApproximateNearest(loc.x - cameraPos.x, loc.y - cameraPos.y, loc.z - cameraPos.z);
         return BlockHitResult.miss(loc, direction, BlockPos.containing(loc));
      } else {
         return hitResult;
      }
   }
}
