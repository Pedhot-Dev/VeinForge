package me.grish.veinforge.pathfinder.util

import me.grish.veinforge.mixin.client.KeyBindingAccessor
import net.minecraft.client.KeyMapping
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3
import kotlin.math.ceil
import kotlin.math.floor

fun LivingEntity.getStandingOnCeil() = BlockPos.containing(x, ceil(y) - 1, z)
fun LivingEntity.getStandingOnFloor() = BlockPos.containing(x, floor(y) - 1, z)

fun KeyMapping.setPressed(pressed: Boolean) {
    KeyMapping.set((this as KeyBindingAccessor).boundKey, pressed)
}

fun BlockPos.toVec3() = Vec3(x.toDouble() + 0.5, y.toDouble() + 0.5, z.toDouble() + 0.5)
fun BlockPos.toVec3Top(): Vec3 = toVec3().add(0.0, 0.5, 0.0)
fun Vec3.toBlockPos(): BlockPos = BlockPos.containing(x, y, z)

fun LivingEntity.lastTickPositionCeil(): BlockPos {
    return BlockPos.containing(xOld, ceil(yOld) - 1, zOld)
}
