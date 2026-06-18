package me.grish.veinforge.mixin.client;

import me.grish.veinforge.event.MotionUpdateEvent;
import me.grish.veinforge.macro.MacroManager;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for ClientPlayerEntity to intercept rotation updates and item dropping.
 */
@Mixin(value = LocalPlayer.class, priority = Integer.MAX_VALUE)
public abstract class MixinClientPlayerEntity {

    @Unique
    private float veinforge$serverYaw = 0f;
    @Unique
    private float veinforge$serverPitch = 0f;

    /**
     * Fire MotionUpdateEvent before sending position/rotation to server.
     */
    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void onSendMovementPacketsPre(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        MotionUpdateEvent event = new MotionUpdateEvent(self.getYRot(), self.getXRot());
        MotionUpdateEvent.fire(event);
        this.veinforge$serverYaw = event.yaw;
        this.veinforge$serverPitch = event.pitch;
    }

    /**
     * Override yaw sent to server.
     */
    @Redirect(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"))
    private float overrideYaw(LocalPlayer instance) {
        return this.veinforge$serverYaw;
    }

    /**
     * Override pitch sent to server.
     */
    @Redirect(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"))
    private float overridePitch(LocalPlayer instance) {
        return this.veinforge$serverPitch;
    }

    /**
     * Prevent item dropping while macro is running.
     */
    @Inject(method = "drop(Z)Z", at = @At("HEAD"), cancellable = true)
    private void onDropSelectedItem(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        if (MacroManager.getInstance().isRunning()) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
