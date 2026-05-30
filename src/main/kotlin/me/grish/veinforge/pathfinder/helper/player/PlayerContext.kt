package me.grish.veinforge.pathfinder.helper.player

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import kotlin.math.ceil

class PlayerContext(override val mc: Minecraft) : IPlayerContext {
    override val player get() = mc.player
    override val playerController get() = mc.gameMode
    override val world get() = mc.level

    // Block player is standing on
    override val playerPosition: BlockPos?
        get() {
            val p = player ?: return null
            return BlockPos.containing(p.x, ceil(p.y) - 1, p.z)
        }
}
