package me.grish.veinforge.pathfinder.helper

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.status.ChunkStatus

class BlockStateAccessor(val world: ClientLevel) {
    fun get(x: Int, y: Int, z: Int): BlockState {
        return if (isBlockInLoadedChunks(x, z)) {
            world.getBlockState(BlockPos(x, y, z))
        } else {
            Blocks.AIR.defaultBlockState()
        }
    }

    fun isBlockInLoadedChunks(blockX: Int, blockZ: Int): Boolean {
        val chunkX = blockX shr 4
        val chunkZ = blockZ shr 4
        return world.chunkSource.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) != null
    }
}
