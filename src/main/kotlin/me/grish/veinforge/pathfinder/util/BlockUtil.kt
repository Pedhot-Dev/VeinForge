package me.grish.veinforge.pathfinder.util

import me.grish.veinforge.pathfinder.helper.BlockStateAccessor
import me.grish.veinforge.pathfinder.movement.CalculationContext
import me.grish.veinforge.pathfinder.movement.MovementHelper
import me.grish.veinforge.pathfinder.util.BlockUtil.bresenham
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.level.block.SlabBlock
import net.minecraft.world.level.block.SnowLayerBlock
import net.minecraft.world.level.block.StairBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.Half
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

object BlockUtil {
    fun canWalkOnBlock(pos: BlockPos): Boolean {
        val state = world?.getBlockState(pos) ?: return false
        val worldInstance = world ?: return false
        val stateAbove = worldInstance.getBlockState(pos.above())
        return state.isSolidRender && state.fluidState.isEmpty && stateAbove.isAir
    }

    fun neighbourGenerator(
        mainBlock: BlockPos,
        xD1: Int,
        xD2: Int,
        yD1: Int,
        yD2: Int,
        zD1: Int,
        zD2: Int
    ): List<BlockPos> {
        val neighbours: MutableList<BlockPos> = ArrayList()
        for (x in xD1..xD2) {
            for (y in yD1..yD2) {
                for (z in zD1..zD2) {
                    neighbours.add(BlockPos(mainBlock.x + x, mainBlock.y + y, mainBlock.z + z))
                }
            }
        }
        return neighbours
    }

    fun isStairSlab(block: BlockPos): Boolean {
        val blockState = world?.getBlockState(block) ?: return false
        return blockState.block is StairBlock || blockState.block is StairBlock
    }

//    fun blocksBetweenValid(ctx: CalculationContext = CalculationContext(VeinForge.instance), startPoss: BlockPos, endPoss: BlockPos): Boolean {
//        val blocksBetween = bresenham(ctx, startPoss.toVec3(), endPoss.toVec3())
//        if (blocksBetween.isEmpty()) {
//            return false
//        }
//        for (i in blocksBetween.indices) {
//            val curr = blocksBetween[i]
//            if (!MovementHelper.canStandOn(ctx.bsa, curr.x, curr.y, curr.z)
//                || !MovementHelper.canWalkThrough(ctx.bsa, curr.x, curr.y + 1, curr.z)
//                || !MovementHelper.canWalkThrough(ctx.bsa, curr.x, curr.y + 2, curr.z)) {
//                return false
//            }
//            if (i == 0) continue
//            if (!canWalkOn(ctx, blocksBetween[i - 1], curr)) {
//                return false
//            }
//        }
//        return true
//    }

    fun getDirectionToWalkOnStairs(state: BlockState): Direction {
        val facing = state.getValue(StairBlock.FACING)
        return if (state.getValue(StairBlock.HALF) == Half.TOP) {
            Direction.DOWN
        } else {
            facing
        }
    }

    fun getPlayerDirectionToBeAbleToWalkOnBlock(startPos: BlockPos, endPoss: BlockPos): Direction {
        val deltaX: Int = endPoss.x - startPos.x
        val deltaZ: Int = endPoss.z - startPos.z

        return if (abs(deltaX) > abs(deltaZ)) {
            if (deltaX > 0) Direction.EAST else Direction.WEST
        } else {
            if (deltaZ > 0) Direction.SOUTH else Direction.NORTH
        }
    }

    fun canWalkOn(ctx: CalculationContext, startPos: BlockPos, endPos: BlockPos): Boolean {
        val startState = ctx.bsa.get(startPos.x, startPos.y, startPos.z)
        val endState = ctx.bsa.get(endPos.x, endPos.y, endPos.z)
        if (!endState.isSolidRender) {
            return endPos.y - startPos.y <= 1
        }
        val sourceMaxY = MovementHelper.collisionMaxY(startState, ctx.world, startPos)
        val destMaxY = MovementHelper.collisionMaxY(endState, ctx.world, endPos)
        if (endState.block is StairBlock && destMaxY - sourceMaxY > 1.0) {
            return MovementHelper.isValidStair(
                endState,
                endPos.x - startPos.x,
                endPos.z - startPos.z
            )
        }
        return destMaxY - sourceMaxY <= .5
    }

    fun bresenham(ctx: CalculationContext, start: BlockPos, end: BlockPos): Boolean {
        return bresenham(
            ctx,
            Vec3.atCenterOf(start),
            Vec3.atCenterOf(end)
        )
    }

    fun bresenham(ctx: CalculationContext, start: Vec3, end: Vec3): Boolean {
        var currentPos = start

        val x1 = Mth.floor(end.x)
        val y1 = Mth.floor(end.y)
        val z1 = Mth.floor(end.z)
        var x0 = Mth.floor(currentPos.x)
        var y0 = Mth.floor(currentPos.y)
        var z0 = Mth.floor(currentPos.z)

        var lastState = ctx.bsa.get(x0, y0, z0)
        var lastPos = BlockPos(x0, y0, z0)

        var iterations = 200
        while (iterations-- >= 0) {
            if (x0 == x1 && y0 == y1 && z0 == z1) {
                return true
            }

            var hasNewX = true
            var hasNewY = true
            var hasNewZ = true
            var newX = 999.0
            var newY = 999.0
            var newZ = 999.0

            if (x1 > x0) {
                newX = x0 + 1.0
            } else if (x1 < x0) {
                newX = x0 + 0.0
            } else {
                hasNewX = false
            }

            if (y1 > y0) {
                newY = y0 + 1.0
            } else if (y1 < y0) {
                newY = y0 + 0.0
            } else {
                hasNewY = false
            }

            if (z1 > z0) {
                newZ = z0 + 1.0
            } else if (z1 < z0) {
                newZ = z0 + 0.0
            } else {
                hasNewZ = false
            }

            var stepX = 999.0
            var stepY = 999.0
            var stepZ = 999.0

            val dx = end.x - currentPos.x
            val dy = end.y - currentPos.y
            val dz = end.z - currentPos.z

            if (hasNewX) stepX = (newX - currentPos.x) / dx
            if (hasNewY) stepY = (newY - currentPos.y) / dy
            if (hasNewZ) stepZ = (newZ - currentPos.z) / dz

            if (stepX == -0.0) stepX = -1.0E-4
            if (stepY == -0.0) stepY = -1.0E-4
            if (stepZ == -0.0) stepZ = -1.0E-4

            val direction: Direction?
            if (stepX < stepY && stepX < stepZ) {
                direction = if (x1 > x0) Direction.WEST else Direction.EAST
                currentPos = Vec3(newX, currentPos.y + dy * stepX, currentPos.z + dz * stepX)
            } else if (stepY < stepZ) {
                direction = if (y1 > y0) Direction.DOWN else Direction.UP
                currentPos = Vec3(currentPos.x + dx * stepY, newY, currentPos.z + dz * stepY)
            } else {
                direction = if (z1 > z0) Direction.NORTH else Direction.SOUTH
                currentPos = Vec3(currentPos.x + dx * stepZ, currentPos.y + dy * stepZ, newZ)
            }

            x0 = Mth.floor(currentPos.x) - (if (direction == Direction.EAST) 1 else 0)
            y0 = Mth.floor(currentPos.y) - (if (direction == Direction.UP) 1 else 0)
            z0 = Mth.floor(currentPos.z) - (if (direction == Direction.SOUTH) 1 else 0)

            var currState = ctx.bsa.get(x0, y0, z0)
            var i = 0

            if (!MovementHelper.canStandOn(ctx.bsa, x0, y0, z0, currState) || !MovementHelper.canWalkThrough(
                    ctx.bsa,
                    x0,
                    y0 + 1,
                    z0
                ) || !MovementHelper.canWalkThrough(ctx.bsa, x0, y0 + 2, z0)
            ) {
                i = -3
                var foundValidBlock = false
                while (++i <= 3) {
                    if (i == 0) continue
                    currState = ctx.bsa.get(x0, y0 + i, z0)
                    if (!MovementHelper.canStandOn(ctx.bsa, x0, y0 + i, z0, currState)) {
                        continue
                    }
                    if (!MovementHelper.canWalkThrough(ctx.bsa, x0, y0 + i + 1, z0)) {
                        continue
                    }
                    if (!MovementHelper.canWalkThrough(ctx.bsa, x0, y0 + i + 2, z0)) {
                        continue
                    }
                    foundValidBlock = true
                    break
                }
                if (!foundValidBlock) {
                    return false
                }
            }

            val delta = (y0 + i) - lastPos.y
            if (delta > 0) {
                if (delta > 1) {
                    return false
                }

                var sourceHeight = -1.0
                var destHeight = -1.0
                var snow = false

                if (lastState.block is SnowLayerBlock) {
                    sourceHeight = (lastState.getValue(SnowLayerBlock.LAYERS) - 1) * 0.125
                    snow = true
                }

                if (currState.block is SnowLayerBlock) {
                    destHeight = (currState.getValue(SnowLayerBlock.LAYERS) - 1) * 0.125
                    snow = true
                }

                if (!snow) {
                    val stepX = x0 - lastPos.x
                    val stepZ = z0 - lastPos.z
                    val sourceSurfaceOffset =
                        getWalkSurfaceOffset(ctx, lastState, lastPos.x, lastPos.y, lastPos.z, stepX, stepZ)
                    val destSurfaceOffset =
                        getWalkSurfaceOffset(ctx, currState, x0, y0 + i, z0, stepX, stepZ)
                    val sourceSurfaceY = lastPos.y + sourceSurfaceOffset
                    val destSurfaceY = y0 + i + destSurfaceOffset

                    if (destSurfaceY - sourceSurfaceY > 0.5) {
                        return false
                    }
                } else {
                    if (sourceHeight == -1.0) {
                        val level = world
                        sourceHeight = if (level != null) {
                            lastState.getCollisionShape(level, lastPos).max(Direction.Axis.Y)
                        } else {
                            0.0
                        }
                    }
                    if (destHeight == -1.0) {
                        val level = world
                        destHeight = if (level != null) {
                            currState.getCollisionShape(level, BlockPos(x0, y0 + i, z0)).max(Direction.Axis.Y)
                        } else {
                            0.0
                        }
                    }
                    if (destHeight - sourceHeight > -0.5) {
                        return false
                    }
                }
            }

            lastState = currState
            lastPos = BlockPos(x0, y0 + i, z0)
        }
        return false
    }

    private fun getWalkSurfaceOffset(
        ctx: CalculationContext,
        state: BlockState,
        x: Int,
        y: Int,
        z: Int,
        stepX: Int,
        stepZ: Int
    ): Double {
        if (state.block is SlabBlock || state.block is StairBlock) {
            return if (MovementHelper.hasTop(state, stepX, stepZ)) 1.0 else 0.5
        }
        return MovementHelper.collisionMaxY(state, ctx.world, BlockPos(x, y, z))
    }

    fun canWalkBetween(ctx: CalculationContext, start: BlockPos, end: BlockPos): Boolean {
        val endState = ctx.get(end.x, end.y, end.z)
        if (!MovementHelper.canStandOn(ctx.bsa, end.x, end.y, end.z, endState)) {
            return false
        }
        if (!MovementHelper.canWalkThrough(ctx.bsa, end.x, end.y + 1, end.z, ctx.get(end.x, end.y + 1, end.z))) {
            return false
        }
        if (!MovementHelper.canWalkThrough(ctx.bsa, end.x, end.y + 2, end.z, ctx.get(end.x, end.y + 2, end.z))) {
            return false
        }
        return bresenham(ctx, start, end)
    }

    fun canWalkBetween(ctx: CalculationContext, start: Vec3, end: Vec3): Boolean {
        val endPos = BlockPos.containing(end)
        if (!MovementHelper.canStandOn(ctx.bsa, endPos.x, endPos.y, endPos.z, ctx.get(endPos.x, endPos.y, endPos.z))) {
            return false
        }
        if (!MovementHelper.canWalkThrough(
                ctx.bsa,
                endPos.x,
                endPos.y + 1,
                endPos.z,
                ctx.get(endPos.x, endPos.y + 1, endPos.z)
            )
        ) {
            return false
        }
        if (!MovementHelper.canWalkThrough(
                ctx.bsa,
                endPos.x,
                endPos.y + 2,
                endPos.z,
                ctx.get(endPos.x, endPos.y + 2, endPos.z)
            )
        ) {
            return false
        }
        return bresenham(ctx, start, end)
    }

    fun canStandOn(pos: BlockPos): Boolean {
        val worldInstance = world ?: return false
        val bsa = BlockStateAccessor(worldInstance)
        return MovementHelper.canStandOn(bsa, pos.x, pos.y, pos.z, bsa.get(pos.x, pos.y, pos.z)) &&
                MovementHelper.canWalkThrough(bsa, pos.x, pos.y + 1, pos.z, bsa.get(pos.x, pos.y + 1, pos.z)) &&
                MovementHelper.canWalkThrough(bsa, pos.x, pos.y + 2, pos.z, bsa.get(pos.x, pos.y + 2, pos.z))
    }
}
