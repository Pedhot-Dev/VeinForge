package me.grish.veinforge.util;

import me.grish.veinforge.VeinForge;
import me.grish.veinforge.handler.GameStateHandler;
import me.grish.veinforge.pathfinder.helper.BlockStateAccessor;
import me.grish.veinforge.pathfinder.movement.CalculationContext;
import me.grish.veinforge.pathfinder.movement.MovementHelper;
import me.grish.veinforge.util.helper.heap.MinHeap;
import me.grish.veinforge.util.helper.location.Location;
import me.grish.veinforge.util.helper.location.SubLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.stream.Collectors;


public class BlockUtil {

   // Credit: GTC
   public static final Map<Direction, float[]> BLOCK_SIDES = new HashMap<Direction, float[]>() {{
      put(Direction.DOWN, new float[]{0.5f, 0.01f, 0.5f});
      put(Direction.UP, new float[]{0.5f, 0.99f, 0.5f});
      put(Direction.WEST, new float[]{0.01f, 0.5f, 0.5f});
      put(Direction.EAST, new float[]{0.99f, 0.5f, 0.5f});
      put(Direction.NORTH, new float[]{0.5f, 0.5f, 0.01f});
      put(Direction.SOUTH, new float[]{0.5f, 0.5f, 0.99f});
      put(null, new float[]{0.5f, 0.5f, 0.5f}); // Handles the null case
   }};
   private static final int DEFAULT_BLOCK_STRENGTH = 5000;
   private static final Minecraft mc = Minecraft.getInstance();

   public static BlockPos getBlockLookingAt() {
      if (mc.hitResult == null) {
         return null;
      }
      if (!(mc.hitResult instanceof BlockHitResult)) {
         return null;
      }
      return ((BlockHitResult) mc.hitResult).getBlockPos();
   }

   public static List<BlockPos> getWalkableBlocksAround(BlockPos playerPos) {
      List<BlockPos> walkableBlocks = new ArrayList<>();
      if (mc.level == null) return walkableBlocks;

      BlockStateAccessor bsa = new BlockStateAccessor(mc.level);
      int yOffset = MovementHelper.INSTANCE.isBottomSlab(bsa.get(playerPos.getX(), playerPos.getY(), playerPos.getZ())) ? -1 : 0;

      for (int i = -1; i <= 1; i++) {
         for (int j = yOffset; j <= 0; j++) {
            for (int k = -1; k <= 1; k++) {
               int x = playerPos.getX() + i;
               int y = playerPos.getY() + j;
               int z = playerPos.getZ() + k;

               if (MovementHelper.INSTANCE.canStandOn(bsa, x, y, z, bsa.get(x, y, z)) &&
                           MovementHelper.INSTANCE.canWalkThrough(bsa, x, y + 1, z, bsa.get(x, y + 1, z)) &&
                           MovementHelper.INSTANCE.canWalkThrough(bsa, x, y + 2, z, bsa.get(x, y + 2, z))) {
                  walkableBlocks.add(new BlockPos(x, y, z));
               }
            }
         }
      }
      return walkableBlocks;
   }

   public static List<BlockPos> findMineableBlocksFromAccessiblePositions(
           Map<Block, Integer> blockPriorities, BlockPos blockToIgnore, int miningSpeed) {
      return findMineableBlocksFromAccessiblePositions(blockPriorities, blockToIgnore, miningSpeed, null);
   }

   public static List<BlockPos> findMineableBlocksFromAccessiblePositions(
           Map<Block, Integer> blockPriorities, BlockPos blockToIgnore, int miningSpeed, MiningDebugContext debugContext) {

      final MinHeap<BlockPos> blocks = new MinHeap<>(500);
      final Set<Long> visitedPositions = new HashSet<>(1000);

      final BlockPos playerBlock = PlayerUtil.getBlockStandingOn();
      final List<BlockPos> walkableBlocks = getWalkableBlocksAround(playerBlock);

      if (blockToIgnore != null) {
         visitedPositions.add(longHash(blockToIgnore.getX(), blockToIgnore.getY(), blockToIgnore.getZ()));
      }

      for (final BlockPos blockPos : walkableBlocks) {
         if (mc.player == null) continue;
         final Vec3 eye = new Vec3(
                 blockPos.getX() + 0.5d,
                 blockPos.getY() + mc.player.getEyeHeight(mc.player.getPose()),
                 blockPos.getZ() + 0.5d
         );

         MinHeap<BlockPos> batch = findMineableBlocksAroundPoint(eye, blockPriorities, visitedPositions, miningSpeed, debugContext);
         for (BlockPos pos : batch.getBlocks()) {
            double cost = batch.getCost(pos);
            blocks.add(pos, cost);
         }
      }

      return blocks.getBlocks();
   }

   public static List<BlockPos> findMineableBlocksAroundHead(
           Map<Block, Integer> blockPriorities, BlockPos blockToIgnore, int miningSpeed) {
      if (mc.player == null) return new ArrayList<>();
      final Vec3 eye = new Vec3(
              mc.player.getX(),
              mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()),
              mc.player.getZ()
      );

      final Set<Long> blocksToIgnore = new HashSet<>();
      if (blockToIgnore != null) {
         blocksToIgnore.add(longHash(blockToIgnore.getX(), blockToIgnore.getY(), blockToIgnore.getZ()));
      }

      return findMineableBlocksAroundPoint(eye, blockPriorities, blocksToIgnore, miningSpeed, null).getBlocks();
   }

   public static MinHeap<BlockPos> findMineableBlocksAroundPoint(Vec3 point, Map<Block, Integer> blockPriorities, Set<Long> blocksToIgnore, int miningSpeed) {
      return findMineableBlocksAroundPoint(point, blockPriorities, blocksToIgnore, miningSpeed, null);
   }

   public static MinHeap<BlockPos> findMineableBlocksAroundPoint(Vec3 point, Map<Block, Integer> blockPriorities, Set<Long> blocksToIgnore, int miningSpeed, MiningDebugContext debugContext) {
      final MinHeap<BlockPos> blocks = new MinHeap<>(500);

      final int HORIZONTAL_RADIUS = 5;
      final int VERTICAL_LOWER = -3;
      final int VERTICAL_UPPER = 4;
      final double MAX_DISTANCE = 4;

      // Calculate bounds for the block
      final double baseX = point.x;
      final double baseY = point.y;
      final double baseZ = point.z;

      // Process the blocks in an optimized order (Y first for better cache locality)
      for (int y = VERTICAL_LOWER; y <= VERTICAL_UPPER; y++) {
         final double actualY = baseY + y;

         for (int x = -HORIZONTAL_RADIUS; x <= HORIZONTAL_RADIUS; x++) {
            final double actualX = baseX + x;

            for (int z = -HORIZONTAL_RADIUS; z <= HORIZONTAL_RADIUS; z++) {
               final double actualZ = baseZ + z;

               final BlockPos pos = new BlockPos((int) actualX, (int) actualY, (int) actualZ);

               // Skip if in ignore
               final long hash = longHash(pos.getX(), pos.getY(), pos.getZ());
               if (blocksToIgnore.contains(hash)) {
                  continue;
               }

               // Mark as visited immediately
               blocksToIgnore.add(hash);

               // The maximum reach for player is 4 blocks
               final double dx = baseX - actualX;
               final double dy = baseY - actualY;
               final double dz = baseZ - actualZ;
               final double distSq = dx * dx + dy * dy + dz * dz;

               if (distSq > MAX_DISTANCE * MAX_DISTANCE) {
                  continue;
               }

               // Check if it's a target block
               if (mc.level == null) continue;
               final BlockState state = mc.level.getBlockState(pos);
               final Block block = state.getBlock();
               if (!blockPriorities.containsKey(block))
                  continue;

               final int blockPriority = blockPriorities.get(block);

               // 0 means no chance for selection
               if (blockPriority == 0)
                  continue;

               // Check visibility
               if (!hasVisibleSide(pos)) {
                  if (debugContext != null) debugContext.onBlockRejected(pos, "Obstructed");
                  continue;
               }

               // Calculate mining cost components
               final double hardness = getBlockStrength(state);
               final float angleChange = AngleUtil.getNeededChange(AngleUtil.getPlayerAngle(), AngleUtil.getRotation(pos)).lengthSqrt();

               // Calculate final cost and add to heap
               double miningCost = hardness / (miningSpeed * 1.0d) * VeinForge.config().debug.miningCoefficient
                                           + angleChange * VeinForge.config().debug.angleCoefficient
                                           + distSq * VeinForge.config().debug.distanceCoefficient;
               miningCost /= (blockPriority * 1.0d);

               if (debugContext != null) debugContext.onBlockCandidate(pos, miningCost);
               blocks.add(pos, miningCost);
            }
         }
      }
      return blocks;
   }

   public static int getBlockStrength(int stateID) {
      // stateID is a runtime raw state ID; translate to BlockState and key on stable blocks instead.
      try {
         return getBlockStrength(Block.stateById(stateID));
      } catch (Exception ignored) {
         return DEFAULT_BLOCK_STRENGTH;
      }
   }

   public static int getBlockStrength(BlockState state) {
      if (state == null) {
         return DEFAULT_BLOCK_STRENGTH;
      }

      Block block = state.getBlock();

      // SkyBlock hardness table (block-id stable). Values based on in-game "block strength".
      // Note: some SkyBlock materials are represented by vanilla blocks (wool/prismarine/etc.).

      // Basic blocks
      if (block == Blocks.NETHERRACK) {
         return 8;
      }
      if (block == Blocks.COBBLESTONE) {
         return 20;
      }
      if (block == Blocks.END_STONE) {
         return 30;
      }
      if (block == Blocks.OBSIDIAN) {
         return 500;
      }

      // Hard Stone vs vanilla stone: Hard Stone is most commonly encountered in mining areas.
      if (block == Blocks.STONE) {
         return isMiningHardStoneArea() ? 50 : 15;
      }
      if (block == Blocks.DEEPSLATE || block == Blocks.COBBLED_DEEPSLATE) {
         // Dark Hard Stone (approximation)
         return 100;
      }

      // Vanilla ores (approx.)
      if (block == Blocks.COAL_ORE
                  || block == Blocks.IRON_ORE
                  || block == Blocks.GOLD_ORE
                  || block == Blocks.REDSTONE_ORE
                  || block == Blocks.LAPIS_ORE
                  || block == Blocks.DIAMOND_ORE
                  || block == Blocks.EMERALD_ORE
                  || block == Blocks.DEEPSLATE_COAL_ORE
                  || block == Blocks.DEEPSLATE_IRON_ORE
                  || block == Blocks.DEEPSLATE_GOLD_ORE
                  || block == Blocks.DEEPSLATE_REDSTONE_ORE
                  || block == Blocks.DEEPSLATE_LAPIS_ORE
                  || block == Blocks.DEEPSLATE_DIAMOND_ORE
                  || block == Blocks.DEEPSLATE_EMERALD_ORE) {
         return 30;
      }

      // Pure ores
      if (block == Blocks.DIAMOND_BLOCK
                  || block == Blocks.GOLD_BLOCK
                  || block == Blocks.REDSTONE_BLOCK
                  || block == Blocks.LAPIS_BLOCK
                  || block == Blocks.EMERALD_BLOCK
                  || block == Blocks.IRON_BLOCK
                  || block == Blocks.COAL_BLOCK) {
         return 600;
      }

      // Sulphur
      if (block == Blocks.SPONGE) {
         return 500;
      }

      // Titanium
      if (block == Blocks.POLISHED_DIORITE) {
         return 2000;
      }

      // Mithril variants
      if (block == Blocks.WOOL.gray()){
         return 500;
      }
      if (block == Blocks.WOOL.lightBlue()) {
         return 1500;
      }
      if (block == Blocks.PRISMARINE || block == Blocks.DARK_PRISMARINE || block == Blocks.PRISMARINE_BRICKS) {
         return 800;
      }

      // Tungsten / Umber (legacy mapping commonly uses clay/terracotta)
      if (block == Blocks.CLAY || block == Blocks.TERRACOTTA) {
         return 5600;
      }

      // Glacite
      if (block == Blocks.PACKED_ICE) {
         return 6000;
      }

      // Gemstones (stained glass + panes)
      if (block == Blocks.STAINED_GLASS.red() || block == Blocks.STAINED_GLASS_PANE.red()) {
         return 2300; // Ruby
      }
      if (block == Blocks.STAINED_GLASS.white() || block == Blocks.STAINED_GLASS_PANE.white()
                  || block == Blocks.STAINED_GLASS.lightBlue() || block == Blocks.STAINED_GLASS_PANE.lightBlue()
                  || block == Blocks.STAINED_GLASS.purple() || block == Blocks.STAINED_GLASS_PANE.purple()
                  || block == Blocks.STAINED_GLASS.yellow() || block == Blocks.STAINED_GLASS_PANE.yellow()
                  || block == Blocks.STAINED_GLASS.lime() || block == Blocks.STAINED_GLASS_PANE.lime()) {
         return 3000; // Opal/Sapphire/Amethyst/Amber/Jade
      }
      if (block == Blocks.STAINED_GLASS.orange() || block == Blocks.STAINED_GLASS_PANE.orange()) {
         return 3800; // Topaz
      }
      if (block == Blocks.STAINED_GLASS.magenta() || block == Blocks.STAINED_GLASS_PANE.magenta()) {
         return 4800; // Jasper
      }
      if (block == Blocks.STAINED_GLASS.black() || block == Blocks.STAINED_GLASS_PANE.black()
                  || block == Blocks.STAINED_GLASS.cyan() || block == Blocks.STAINED_GLASS_PANE.cyan()
                  || block == Blocks.STAINED_GLASS.brown() || block == Blocks.STAINED_GLASS_PANE.brown()
                  || block == Blocks.STAINED_GLASS.green() || block == Blocks.STAINED_GLASS_PANE.green()) {
         return 5200; // Onyx/Aquamarine/Citrine/Peridot
      }

      return DEFAULT_BLOCK_STRENGTH;
   }

   private static boolean isMiningHardStoneArea() {
      try {
         GameStateHandler handler = GameStateHandler.getInstance();
         Location location = handler.getCurrentLocation();
         SubLocation subLocation = handler.getCurrentSubLocation();

         if (location == Location.DWARVEN_MINES || location == Location.CRYSTAL_HOLLOWS) {
            return true;
         }

         return subLocation == SubLocation.CRYSTAL_HOLLOWS
                        || subLocation == SubLocation.MITHRIL_DEPOSITS
                        || subLocation == SubLocation.MINES_OF_DIVAN
                        || subLocation == SubLocation.DIVANS_GATEWAY
                        || subLocation == SubLocation.GLACITE_TUNNELS
                        || subLocation == SubLocation.GLACITE_MINESHAFT;
      } catch (Exception ignored) {
         return false;
      }
   }

   public static int getMiningTime(int stateId, final int miningSpeed) {
      try {
         return getMiningTime(Block.stateById(stateId), miningSpeed);
      } catch (Exception ignored) {
         return (int) Math.ceil((DEFAULT_BLOCK_STRENGTH * 30) / (float) miningSpeed) + VeinForge.config().delays.tickGlideOffset;
      }
   }

   public static int getMiningTime(BlockState state, final int miningSpeed) {
      return (int) Math.ceil((getBlockStrength(state) * 30) / (float) miningSpeed) + VeinForge.config().delays.tickGlideOffset;
   }

   public static Vec3 getSidePos(BlockPos block, Direction face) {
      final float[] offset = BLOCK_SIDES.get(face);
      return new Vec3(block.getX() + offset[0], block.getY() + offset[1], block.getZ() + offset[2]);
   }

   public static boolean canSeeSide(BlockPos block, Direction side) {
      return RaytracingUtil.canSeePointWithEntities(getSidePos(block, side));
   }

   public static boolean canSeeSide(Vec3 from, BlockPos block, Direction side) {
      return RaytracingUtil.canSeePointWithEntities(from, getSidePos(block, side));
   }

   public static List<Direction> getAllVisibleSides(BlockPos block) {
      final List<Direction> sides = new ArrayList<>();
      for (Direction face : BLOCK_SIDES.keySet()) {
         if (face != null && !shouldRenderSide(block, face)) {
            continue;
         }
         if (canSeeSide(block, face)) {
            sides.add(face);
         }
      }
      return sides;
   }

   public static List<Direction> getAllVisibleSides(Vec3 from, BlockPos block) {
      final List<Direction> sides = new ArrayList<>();
      for (Direction face : BLOCK_SIDES.keySet()) {
         if (face != null && !shouldRenderSide(block, face)) {
            continue;
         }
         if (canSeeSide(from, block, face)) {
            sides.add(face);
         }
      }
      return sides;
   }

   public static Vec3 getClosestVisibleSidePos(BlockPos block) {
      Direction face = null;
      if (isFullCube(block)) {
         if (mc.player == null) return Vec3.ZERO;
         final Vec3 eyePos = mc.player.getEyePosition();
         double dist = Double.MAX_VALUE;
         for (Direction side : BLOCK_SIDES.keySet()) {
            if (side != null && !shouldRenderSide(block, side)) {
               continue;
            }
            final double distanceToThisSide = eyePos.distanceTo(getSidePos(block, side));
            if (canSeeSide(block, side) && distanceToThisSide < dist) {
               if (side == null && face != null) {
                  continue;
               }
               dist = distanceToThisSide;
               face = side;
            }
         }
      }
      final float[] offset = BLOCK_SIDES.get(face);
      return new Vec3(block.getX() + offset[0], block.getY() + offset[1], block.getZ() + offset[2]);
   }

   public static Vec3 getClosestVisibleSidePos(Vec3 from, BlockPos block) {
      Direction face = null;
      if (isFullCube(block)) {
         double dist = Double.MAX_VALUE;
         for (Direction side : BLOCK_SIDES.keySet()) {
            if (side != null && !shouldRenderSide(block, side)) {
               continue;
            }
            final double distanceToThisSide = from.distanceTo(getSidePos(block, side));
            if (canSeeSide(from, block, side) && distanceToThisSide < dist) {
               if (side == null && face != null) {
                  continue;
               }
               dist = distanceToThisSide;
               face = side;
            }
         }
      }
      final float[] offset = BLOCK_SIDES.get(face);
      return new Vec3(block.getX() + offset[0], block.getY() + offset[1], block.getZ() + offset[2]);
   }

   public static Direction getClosestVisibleSide(BlockPos block) {
      if (!isFullCube(block)) {
         return null;
      }
      if (mc.player == null) return null;
      final Vec3 eyePos = mc.player.getEyePosition();
      double dist = Double.MAX_VALUE;
      Direction face = null;
      for (Direction side : BLOCK_SIDES.keySet()) {
         if (side != null && !shouldRenderSide(block, side)) {
            continue;
         }
         final double distanceToThisSide = eyePos.distanceTo(getSidePos(block, side));
         if (canSeeSide(block, side) && distanceToThisSide < dist) {
            if (side == null && face != null) {
               continue;
            }
            dist = distanceToThisSide;
            face = side;
         }
      }
      return face;
   }

   public static Direction getClosestVisibleSide(Vec3 from, BlockPos block) {
      if (!isFullCube(block)) {
         return null;
      }
      double dist = Double.MAX_VALUE;
      Direction face = null;
      for (Direction side : BLOCK_SIDES.keySet()) {
         if (side != null && !shouldRenderSide(block, side)) {
            continue;
         }
         final double distanceToThisSide = from.distanceTo(getSidePos(block, side));
         if (canSeeSide(from, block, side) && distanceToThisSide < dist) {
            if (side == null && face != null) {
               continue;
            }
            dist = distanceToThisSide;
            face = side;
         }
      }
      return face;
   }

   public static boolean hasVisibleSide(BlockPos block) {
      if (!isFullCube(block)) {
         return false;
      }
      for (Direction side : Direction.values()) {
         if (side != null && !shouldRenderSide(block, side)) {
            continue;
         }
         if (canSeeSide(block, side)) {
            return true;
         }
      }
      return false;
   }

   public static boolean hasVisibleSide(Vec3 from, BlockPos block) {
      if (!isFullCube(block)) {
         return false;
      }
      for (Direction side : Direction.values()) {
         if (!shouldRenderSide(block, side)) {
            continue;
         }
         if (canSeeSide(from, block, side)) {
            return true;
         }
      }
      return false;
   }

   public static List<Vec3> bestPointsOnBestSide(final BlockPos block) {
      return pointsOnBlockSide(block, getClosestVisibleSide(block)).stream()
                     .filter(RaytracingUtil::canSeePointWithEntities)
                     .sorted(Comparator.comparingDouble(i -> AngleUtil.getNeededChange(AngleUtil.getPlayerAngle(), AngleUtil.getRotation(i)).getValue()))
                     .collect(Collectors.toList());
   }

   public static List<Vec3> bestPointsOnBestSide(Vec3 from, final BlockPos block) {
      return pointsOnBlockSide(block, getClosestVisibleSide(from, block)).stream()
                     .filter(it -> RaytracingUtil.canSeePointWithEntities(from, it))
                     .sorted(Comparator.comparingDouble(i -> AngleUtil.getNeededChange(AngleUtil.getPlayerAngle(), AngleUtil.getRotation(from, i)).getValue()))
                     .collect(Collectors.toList());
   }

   public static List<Vec3> bestPointsOnVisibleSides(final BlockPos block) {
      if (mc.player == null) return new ArrayList<>();
      return pointsOnVisibleSides(block).stream()
                     .filter(RaytracingUtil::canSeePointWithEntities)
                     .sorted(Comparator.comparingDouble(mc.player.getEyePosition()::distanceTo))
                     .collect(Collectors.toList());
   }

   public static List<Vec3> bestPointsOnVisibleSides(Vec3 from, final BlockPos block) {
      return pointsOnVisibleSides(block).stream()
                     .filter(it -> RaytracingUtil.canSeePointWithEntities(from, it))
                     .sorted(Comparator.comparingDouble(from::distanceTo))
                     .collect(Collectors.toList());
   }

   private static List<Vec3> pointsOnVisibleSides(final BlockPos block) {
      final List<Vec3> points = new ArrayList<>();
      for (Direction side : getAllVisibleSides(block)) {
         points.addAll(pointsOnBlockSide(block, side));
      }
      return points;
   }

   private static List<Vec3> pointsOnVisibleSides(Vec3 from, final BlockPos block) {
      final List<Vec3> points = new ArrayList<>();
      for (Direction side : getAllVisibleSides(from, block)) {
         points.addAll(pointsOnBlockSide(block, side));
      }
      return points;
   }

   private static List<Vec3> pointsOnBlockSide(final BlockPos block, final Direction side) {
      final Set<Vec3> points = new HashSet<>();

      if (side != null) {
         float[] it = BLOCK_SIDES.get(side);
         for (int i = 0; i < 20; i++) {
            float x = it[0];
            float y = it[1];
            float z = it[2];
            if (x == 0.5f) {
               x = randomVal();
            }
            if (y == 0.5f) {
               y = randomVal();
            }
            if (z == 0.5f) {
               z = randomVal();
            }
            Vec3 point = new Vec3(block.getX() + x, block.getY() + y, block.getZ() + z);
            points.add(point);
         }
      } else {
         for (float[] bside : BLOCK_SIDES.values()) {
            for (int i = 0; i < 20; i++) {
               float x = bside[0];
               float y = bside[1];
               float z = bside[2];
               if (x == 0.5f) {
                  x = randomVal();
               }
               if (y == 0.5f) {
                  y = randomVal();
               }
               if (z == 0.5f) {
                  z = randomVal();
               }
               Vec3 point = new Vec3(block.getX() + x, block.getY() + y, block.getZ() + z);
               points.add(point);
            }
         }
      }
      return new ArrayList<>(points);
   }

   private static float randomVal() {
      return (new Random().nextInt(6) + 2) / 10.0f;
   }

   public static boolean canWalkBetween(CalculationContext ctx, BlockPos start, BlockPos end) {
      int ey = end.getY();
      int ex = end.getX();
      int ez = end.getZ();
      BlockState endState = ctx.get(ex, ey, ez);
      if (!MovementHelper.INSTANCE.canStandOn(ctx.getBsa(), ex, ey, ez, endState)) {
         return false;
      }
      if (!MovementHelper.INSTANCE.canWalkThrough(ctx.getBsa(), ex, ey + 1, ez, ctx.get(ex, ey + 1, ez))) {
         return false;
      }
      if (!MovementHelper.INSTANCE.canWalkThrough(ctx.getBsa(), ex, ey + 2, ez, ctx.get(ex, ey + 2, ez))) {
         return false;
      }
      return !me.grish.veinforge.pathfinder.util.BlockUtil.INSTANCE.bresenham(ctx, start, end);
   }

   public static boolean canWalkBetween(CalculationContext ctx, Vec3 start, Vec3 end) {
      int ey = Mth.floor(end.y);
      int ex = Mth.floor(end.x);
      int ez = Mth.floor(end.z);

      BlockState endState = ctx.get(ex, ey, ez);
      if (!MovementHelper.INSTANCE.canStandOn(ctx.getBsa(), ex, ey, ez, endState)) {
         return false;
      }
      if (!MovementHelper.INSTANCE.canWalkThrough(ctx.getBsa(), ex, ey + 1, ez, ctx.get(ex, ey + 1, ez))) {
         return false;
      }
      if (!MovementHelper.INSTANCE.canWalkThrough(ctx.getBsa(), ex, ey + 2, ez, ctx.get(ex, ey + 2, ez))) {
         return false;
      }
      return !me.grish.veinforge.pathfinder.util.BlockUtil.INSTANCE.bresenham(ctx, start, end);
   }

   public static boolean canStandOn(BlockPos pos) {
      if (mc.level == null) return false;
      BlockStateAccessor bsa = new BlockStateAccessor(mc.level);
      int x = pos.getX();
      int y = pos.getY();
      int z = pos.getZ();
      return MovementHelper.INSTANCE.canStandOn(bsa, x, y, z, bsa.get(x, y, z)) &&
                     MovementHelper.INSTANCE.canWalkThrough(bsa, x, y + 1, z, bsa.get(x, y + 1, z)) &&
                     MovementHelper.INSTANCE.canWalkThrough(bsa, x, y + 2, z, bsa.get(x, y + 2, z));
   }

   private static boolean isFullCube(BlockPos pos) {
      if (mc.level == null) {
         return false;
      }
      BlockState state = mc.level.getBlockState(pos);
      // isFullCube in 1.21?
      // state.isOpaqueFullCube() or similar.
      // Actually MovementHelper used state.isSolidBlock(world, pos)
      // state.isFullCube(world, pos) is what the old code used.
      // In 1.21, state.isFullCube(BlockView, BlockPos) exists.
      return state.isCollisionShapeFullBlock(mc.level, pos);
   }

   private static boolean shouldRenderSide(BlockPos pos, Direction side) {
      if (mc.level == null) {
         return false;
      }

      BlockState state = mc.level.getBlockState(pos);
      BlockState adjacent = mc.level.getBlockState(pos.relative(side));
      // BlockState adjacent = mc.world.getBlockState(pos.offset(side));
      // return !state.isSideInvisible(adjacent, side);
      // As discussed, Block.shouldDrawSide(state, world, pos, side, adjacent)
      // Or state.shouldBlockVision(world, pos, side)
      // For now, I'll use state.isSideInvisible if available, otherwise just return true as a fallback to avoid errors.
      // Checking Yarn 1.21: BlockState.isSideInvisible(BlockState, Direction)

      return Block.shouldRenderFace(state, adjacent, side);
   }

   // Stole from baritoe
   public static long longHash(int x, int y, int z) {
      long hash = 3241;
      hash = 3457689L * hash + x;
      hash = 8734625L * hash + y;
      hash = 2873465L * hash + z;
      return hash;
   }


   public interface MiningDebugContext {
      default void onBlockRejected(BlockPos pos, String reason) {
      }

      default void onBlockCandidate(BlockPos pos, double cost) {
      }
   }
}
