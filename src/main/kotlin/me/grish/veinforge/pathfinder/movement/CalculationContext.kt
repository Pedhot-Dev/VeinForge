package me.grish.veinforge.pathfinder.movement

import me.grish.veinforge.VeinForge
import me.grish.veinforge.pathfinder.costs.ActionCosts
import me.grish.veinforge.pathfinder.helper.BlockStateAccessor
import me.grish.veinforge.pathfinder.helper.player.PlayerContext
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.level.block.state.BlockState

class CalculationContext(sprintFactor: Double = 0.13, walkFactor: Double = 0.1, sneakFactor: Double = 0.03) {
    companion object {
        private const val SAFE_FALL_HEIGHT_WHEN_DAMAGE_ENABLED = 17
    }

    val playerContext = PlayerContext(Minecraft.getInstance())
    val world: ClientLevel = playerContext.world ?: throw IllegalStateException("World not available")
    val player = playerContext.player ?: throw IllegalStateException("Player not available")
    val bsa = BlockStateAccessor(world)
    val jumpBoostAmplifier = player.getEffect(MobEffects.JUMP_BOOST)?.amplifier ?: -1
    val cost = ActionCosts(sprintFactor, walkFactor, sneakFactor, jumpBoostAmplifier)
    private val generalConfig = VeinForge.config()?.general
    val maxFallHeight = if (generalConfig?.ignoreFallDamageInPathfinding == true) {
        256 // Hard cap from ActionCosts.N_BLOCK_FALL_COST table.
    } else {
        SAFE_FALL_HEIGHT_WHEN_DAMAGE_ENABLED
    }

    fun get(x: Int, y: Int, z: Int): BlockState {
        return bsa.get(x, y, z)
    }
}
