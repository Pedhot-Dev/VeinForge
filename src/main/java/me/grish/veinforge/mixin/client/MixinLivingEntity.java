package me.grish.veinforge.mixin.client;

import me.grish.veinforge.util.StrafeUtil;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Mixin for LivingEntity to override yaw during movement for strafe.
 * Based on Baritone's omnisprint implementation.
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    /**
     * Override yaw during jump for strafe.
     */
    @Redirect(method = "jumpFromGround", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"))
    private float overrideJumpYaw(LivingEntity self) {
        if (self instanceof LocalPlayer && StrafeUtil.shouldEnable()) {
            return StrafeUtil.yaw;
        }
        return self.getYRot();
    }

    /**
     * Override yaw during travel for strafe.
     */
    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F", ordinal = 0))
    private float overrideTravelYaw(LivingEntity self) {
        if (self instanceof LocalPlayer && StrafeUtil.shouldEnable()) {
            return StrafeUtil.yaw;
        }
        return self.getYRot();
    }
}
