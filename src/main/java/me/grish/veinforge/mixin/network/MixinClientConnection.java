package me.grish.veinforge.mixin.network;

import io.netty.channel.ChannelHandlerContext;
import me.grish.veinforge.event.PacketEvent;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for ClientConnection to intercept packets.
 */
@Mixin(Connection.class)
public class MixinClientConnection {

    @Inject(
            method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void veinforge$onPacketReceived(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        PacketEvent.Received event = new PacketEvent.Received(packet);
        PacketEvent.fireReceived(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void veinforge$onPacketSent(Packet<?> packet, CallbackInfo ci) {
        PacketEvent.Sent event = new PacketEvent.Sent(packet);
        PacketEvent.fireSent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
