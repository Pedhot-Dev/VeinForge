package me.grish.veinforge.pathfinder.movement

import me.grish.veinforge.VeinForge
import net.minecraft.core.BlockPos

interface IMovement {
    val mm: VeinForge
    val source: BlockPos
    val dest: BlockPos
    val costs: Double // plural cuz kotlin gae

    fun getCost(): Double
    fun calculateCost(ctx: CalculationContext, res: MovementResult)
}
