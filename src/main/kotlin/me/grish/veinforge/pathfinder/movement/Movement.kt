package me.grish.veinforge.pathfinder.movement

import me.grish.veinforge.VeinForge
import net.minecraft.core.BlockPos

abstract class Movement(override val mm: VeinForge, override val source: BlockPos, override val dest: BlockPos) :
    IMovement {

    override var costs: Double = 1e6
    override fun getCost() = costs
}
