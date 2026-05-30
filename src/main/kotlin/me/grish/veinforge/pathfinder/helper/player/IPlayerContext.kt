package me.grish.veinforge.pathfinder.helper.player

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.multiplayer.MultiPlayerGameMode
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos

interface IPlayerContext {
    val mc: Minecraft
    val player: LocalPlayer?
    val playerController: MultiPlayerGameMode?
    val world: ClientLevel?
    val playerPosition: BlockPos?
}
