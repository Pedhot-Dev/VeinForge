package me.grish.veinforge.pathfinder.movement

import me.grish.veinforge.pathfinder.helper.BlockStateAccessor
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.tags.FluidTags
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.CauldronBlock
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.level.block.FenceGateBlock
import net.minecraft.world.level.block.LadderBlock
import net.minecraft.world.level.block.SlabBlock
import net.minecraft.world.level.block.SnowLayerBlock
import net.minecraft.world.level.block.StairBlock
import net.minecraft.world.level.block.TrapDoorBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.Half
import net.minecraft.world.level.block.state.properties.SlabType

object MovementHelper {

    fun collisionMaxY(state: BlockState, world: BlockGetter, pos: BlockPos): Double {
        return try {
            val shape = state.getCollisionShape(world, pos)
            if (shape.isEmpty) 0.0 else shape.bounds().maxY
        } catch (_: UnsupportedOperationException) {
            0.0
        }
    }

    fun canWalkThrough(
        bsa: BlockStateAccessor,
        x: Int,
        y: Int,
        z: Int,
        state: BlockState = bsa.get(x, y, z)
    ): Boolean {
        val canWalk = canWalkThroughBlockState(state, bsa)
        if (canWalk != null) {
            return canWalk
        }
        return canWalkThroughPosition(bsa, x, y, z, state)
    }

    fun canWalkThroughBlockState(state: BlockState, bsa: BlockStateAccessor? = null): Boolean? {
        val block = state.block
        return when {
            state.isAir -> true
            block == Blocks.FIRE || block == Blocks.TRIPWIRE || block == Blocks.COBWEB || block == Blocks.END_PORTAL || block == Blocks.COCOA || block is TrapDoorBlock -> false
            block is DoorBlock -> state.getValue(DoorBlock.OPEN)
            block is FenceGateBlock -> state.getValue(FenceGateBlock.OPEN)
            block is net.minecraft.world.level.block.CarpetBlock -> null
            block is SnowLayerBlock -> null
            !state.fluidState.isEmpty -> {
                if (!state.fluidState.isSource) {
                    false
                } else {
                    null
                }
            }

            block is CauldronBlock -> false
            block == Blocks.LADDER -> false
            else -> {
                try {
                    if (bsa != null) {
                        state.getCollisionShape(bsa.world, BlockPos.ZERO).isEmpty
                    } else {
                        null
                    }
                } catch (exception: Throwable) {
                    println("The block ${state.block.descriptionId} requires a special case due to the exception ${exception.message}")
                    null
                }
            }
        }
    }

    fun canWalkThroughPosition(
        bsa: BlockStateAccessor,
        x: Int,
        y: Int,
        z: Int,
        state: BlockState
    ): Boolean {
        val block = state.block

        if (block is net.minecraft.world.level.block.CarpetBlock) {
            return canStandOn(bsa, x, y - 1, z)
        }

        if (block is SnowLayerBlock) {
            if (!bsa.isBlockInLoadedChunks(x, z)) {
                return true
            }
            if (state.getValue(SnowLayerBlock.LAYERS) >= 1) {
                return false
            }
            return canStandOn(bsa, x, y - 1, z)
        }

        if (!state.fluidState.isEmpty) {
            if (isFlowing(x, y, z, state, bsa)) {
                return false
            }

            val up = bsa.get(x, y + 1, z)
            if (!up.fluidState.isEmpty || up.block == Blocks.LILY_PAD) {
                return false
            }
            return state.fluidState.`is`(FluidTags.WATER)
        }

        return state.getCollisionShape(bsa.world, BlockPos(x, y, z)).isEmpty
    }

    fun canStandOn(bsa: BlockStateAccessor, x: Int, y: Int, z: Int, state: BlockState = bsa.get(x, y, z)): Boolean {
        val block = state.block
        return when {
            state.isSolidRender -> true
            block == Blocks.REDSTONE_BLOCK -> true
            block == Blocks.LADDER -> true
            block == Blocks.FARMLAND || block == Blocks.GRASS_BLOCK -> true
            block == Blocks.ENDER_CHEST || block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST -> true
            block == Blocks.GLASS || block == Blocks.STAINED_GLASS.white() -> true
            block is StairBlock -> true
            block == Blocks.SEA_LANTERN -> true
            isWotah(state) -> {
                val up = bsa.get(x, y + 1, z).block
                up == Blocks.LILY_PAD || up is net.minecraft.world.level.block.CarpetBlock
            }

            isLava(state) -> false
            block is SlabBlock -> true
            block is SnowLayerBlock -> true
            else -> false
        }
    }

    fun possiblyFlowing(state: BlockState): Boolean {
        return !state.fluidState.isEmpty && !state.fluidState.isSource
    }

    fun isFlowing(x: Int, y: Int, z: Int, state: BlockState, bsa: BlockStateAccessor): Boolean {
        if (state.fluidState.isEmpty) {
            return false
        }
        if (!state.fluidState.isSource) {
            return true
        }
        return possiblyFlowing(bsa.get(x + 1, y, z)) ||
                possiblyFlowing(bsa.get(x - 1, y, z)) ||
                possiblyFlowing(bsa.get(x, y, z + 1)) ||
                possiblyFlowing(bsa.get(x, y, z - 1))
    }

    fun isWotah(state: BlockState): Boolean {
        return state.fluidState.`is`(FluidTags.WATER)
    }

    fun isLava(state: BlockState): Boolean {
        return state.fluidState.`is`(FluidTags.LAVA)
    }

    fun isBottomSlab(state: BlockState): Boolean {
        return state.block is SlabBlock && state.getValue(SlabBlock.TYPE) == SlabType.BOTTOM
    }

    fun isValidStair(state: BlockState, dx: Int, dz: Int): Boolean {
        if (dx == dz) return false
        if (state.block !is StairBlock) return false
        if (state.getValue(StairBlock.HALF) != Half.BOTTOM) return false

        val stairFacing = state.getValue(StairBlock.FACING)

        return when {
            dz == -1 -> stairFacing == Direction.NORTH
            dz == 1 -> stairFacing == Direction.SOUTH
            dx == -1 -> stairFacing == Direction.WEST
            dx == 1 -> stairFacing == Direction.EAST
            else -> false
        }
    }

    fun isValidReversedStair(state: BlockState, dx: Int, dz: Int): Boolean {
        if (dx == dz) return false
        if (state.block !is StairBlock) return false
        if (state.getValue(StairBlock.HALF) != Half.BOTTOM) return false

        val stairFacing = state.getValue(StairBlock.FACING)

        return when {
            dz == 1 -> stairFacing == Direction.NORTH
            dz == -1 -> stairFacing == Direction.SOUTH
            dx == 1 -> stairFacing == Direction.WEST
            dx == -1 -> stairFacing == Direction.EAST
            else -> false
        }
    }

    fun hasTop(state: BlockState, dX: Int, dZ: Int): Boolean {
        return !(isBottomSlab(state) || isValidStair(state, dX, dZ))
    }

    fun avoidWalkingInto(state: BlockState): Boolean {
        val block = state.block
        return !state.fluidState.isEmpty || block == Blocks.FIRE || block == Blocks.CACTUS || block == Blocks.END_PORTAL || block == Blocks.COBWEB
    }

    fun getFacing(dx: Int, dz: Int): Direction {
        return when {
            dx == 0 && dz == 0 -> Direction.UP
            dx > 0 -> Direction.EAST
            dx < 0 -> Direction.WEST
            dz > 0 -> Direction.SOUTH
            else -> Direction.NORTH
        }
    }

    fun isLadder(state: BlockState): Boolean {
        return state.block == Blocks.LADDER
    }

    fun canWalkIntoLadder(ladderState: BlockState, dx: Int, dz: Int): Boolean {
        return isLadder(ladderState) && ladderState.getValue(LadderBlock.FACING) != getFacing(
            dx,
            dz
        )
    }
}
