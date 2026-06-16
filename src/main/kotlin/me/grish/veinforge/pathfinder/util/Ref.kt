package me.grish.veinforge.pathfinder.util

import net.minecraft.client.Minecraft

val mc
    get() = Minecraft.getInstance()
val player
    get() = mc.player
val world
    get() = mc.level
val gameSettings
    get() = mc.options
