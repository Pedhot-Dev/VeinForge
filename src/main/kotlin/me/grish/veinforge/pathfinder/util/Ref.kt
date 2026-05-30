package me.grish.veinforge.pathfinder.util

import com.mojang.blaze3d.vertex.Tesselator
import net.minecraft.client.Minecraft

val mc
    get() = Minecraft.getInstance()
val player
    get() = mc.player
val world
    get() = mc.level
val tessellator
    get() = Tesselator.getInstance()
val gameSettings
    get() = mc.options
