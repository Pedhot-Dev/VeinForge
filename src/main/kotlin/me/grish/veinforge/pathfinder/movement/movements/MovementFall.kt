package me.grish.veinforge.pathfinder.movement.movements

import me.grish.veinforge.VeinForge
import me.grish.veinforge.pathfinder.movement.CalculationContext
import me.grish.veinforge.pathfinder.movement.Movement
import me.grish.veinforge.pathfinder.movement.MovementResult
import net.minecraft.core.BlockPos

class MovementFall(mm: VeinForge, source: BlockPos, dest: BlockPos) : Movement(mm, source, dest) {
    override fun calculateCost(ctx: CalculationContext, res: MovementResult) {
        calculateCost(ctx, source.x, source.y, source.z, dest.x, dest.z, res)
        costs = res.cost
    }

    companion object {
        fun calculateCost(
            ctx: CalculationContext,
            x: Int,
            y: Int,
            z: Int,
            destX: Int,
            destZ: Int,
            res: MovementResult
        ) {
            res.set(destX, y - 1, destZ)
            MovementDescend.calculateCost(ctx, x, y, z, destX, destZ, res)
        }
    }
}
