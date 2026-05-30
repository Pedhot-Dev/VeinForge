package me.grish.veinforge.pathfinder.movement.movements

import me.grish.veinforge.VeinForge
import me.grish.veinforge.pathfinder.movement.CalculationContext
import me.grish.veinforge.pathfinder.movement.Movement
import me.grish.veinforge.pathfinder.movement.MovementHelper
import me.grish.veinforge.pathfinder.movement.MovementResult
import net.minecraft.core.BlockPos
import kotlin.math.sqrt

// Todo: rewrite its shit
class MovementDiagonal(mm: VeinForge, from: BlockPos, to: BlockPos) : Movement(mm, from, to) {

    override fun calculateCost(ctx: CalculationContext, res: MovementResult) {
        calculateCost(ctx, source.x, source.y, source.z, dest.x, dest.z, res)
        costs = res.cost
    }

    companion object {
        private val SQRT_2 = sqrt(2.0)
        fun calculateCost(
            ctx: CalculationContext,
            x: Int,
            y: Int,
            z: Int,
            destX: Int,
            destZ: Int,
            res: MovementResult
        ) {
            res.set(destX, y, destZ)
            cost(ctx, x, y, z, destX, destZ, res)
        }

        // Todo: IS PROBABLY FUCKED verify that it isnt fucking cancer
        private fun cost(ctx: CalculationContext, x: Int, y: Int, z: Int, destX: Int, destZ: Int, res: MovementResult) {
            if (!MovementHelper.canWalkThrough(ctx.bsa, destX, y + 2, destZ)) return

            var ascend = false
            var descend = false
            val sourceState = ctx.get(x, y, z)
            var destState = ctx.get(destX, y, destZ)
            if (!MovementHelper.canWalkThrough(ctx.bsa, destX, y + 1, destZ)) {
                ascend = true
                if (!MovementHelper.canWalkThrough(ctx.bsa, x, y + 3, z) || !MovementHelper.canStandOn(
                        ctx.bsa,
                        destX,
                        y + 1,
                        destZ
                    ) || !MovementHelper.canWalkThrough(ctx.bsa, destX, y + 2, destZ)
                ) {
                    return
                }
                destState = ctx.get(destX, y + 1, destZ)
                res.y = y + 1
            } else {
                if (!MovementHelper.canStandOn(ctx.bsa, destX, y, destZ, destState)) {
                    descend = true
                    if (!MovementHelper.canStandOn(
                            ctx.bsa,
                            destX,
                            y - 1,
                            destZ
                        ) || !MovementHelper.canWalkThrough(ctx.bsa, destX, y, destZ)
                    ) {
                        return
                    }
                    destState = ctx.get(destX, y - 1, destZ)
                    res.y = y - 1
                }
            }

            var cost = ctx.cost.ONE_BLOCK_WALK_COST

            if (MovementHelper.isLadder(sourceState)) {
                return
            }

            if (MovementHelper.isWotah(ctx.get(x, y + 1, z))) {
                if (ascend) return
                cost = ctx.cost.ONE_BLOCK_WALK_IN_WATER_COST * SQRT_2
            } else {
                cost *= ctx.cost.SPRINT_MULTIPLIER
            }

            val ALOWState = ctx.get(x, y + 1, destZ)
            val BLOWState = ctx.get(destX, y + 1, z)

            val ATOP = MovementHelper.canWalkThrough(ctx.bsa, x, y + 3, destZ)
            val AMID = MovementHelper.canWalkThrough(ctx.bsa, x, y + 2, destZ)
            val ALOW = MovementHelper.canWalkThrough(ctx.bsa, x, y + 1, destZ, ALOWState)
            val BTOP = MovementHelper.canWalkThrough(ctx.bsa, destX, y + 3, z)
            val BMID = MovementHelper.canWalkThrough(ctx.bsa, destX, y + 2, z)
            val BLOW = MovementHelper.canWalkThrough(ctx.bsa, destX, y + 1, z, BLOWState)

            if (!(ATOP && AMID && ALOW && BTOP && BMID && BLOW)) {
                return
            }
            if (!(ascend || descend)) {
                res.cost = cost * SQRT_2
                return
            }

            // Safe way to get sourceMaxY
            val sourceMaxY = try {
                sourceState.getCollisionShape(ctx.world, BlockPos(x, y, z)).bounds().maxY
            } catch (e: UnsupportedOperationException) {
                y.toDouble()
            }

            if (ascend) {
                // Safe way to get destMaxY
                val destMaxY = try {
                    destState.getCollisionShape(ctx.world, BlockPos(destX, y + 1, destZ)).bounds().maxY
                } catch (e: UnsupportedOperationException) {
                    (y + 1.0)
                }
                when {
                    destMaxY - sourceMaxY <= 0.5 -> res.cost = cost * SQRT_2
                    destMaxY - sourceMaxY <= 1.125 -> res.cost = cost * SQRT_2 + ctx.cost.JUMP_ONE_BLOCK_COST
                    else -> res.cost = ctx.cost.INF_COST
                }
                return
            }

            if (descend) {
                // Safe way to get destMaxY
                val destMaxY = try {
                    destState.getCollisionShape(ctx.world, BlockPos(destX, y - 1, destZ)).bounds().maxY
                } catch (e: UnsupportedOperationException) {
                    (y - 1.0)
                }
                when {
                    sourceMaxY - destMaxY <= 0.5 -> res.cost = cost * SQRT_2
                    sourceMaxY - destMaxY <= 1.0 -> res.cost = ctx.cost.N_BLOCK_FALL_COST[1] + cost * SQRT_2
                    else -> res.cost = ctx.cost.INF_COST
                }
            }
        }
    }
}
