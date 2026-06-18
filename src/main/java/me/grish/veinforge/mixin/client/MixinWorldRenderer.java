package me.grish.veinforge.mixin.client;

import me.grish.veinforge.event.BlockDestroyEvent;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class MixinWorldRenderer {

    @Inject(method = "destroyBlockProgress(ILnet/minecraft/core/BlockPos;I)V", at = @At("HEAD"))
    private void veinforge$onSetBlockBreakingInfo(int entityId, BlockPos pos, int progress, CallbackInfo ci) {
        if (pos == null || progress < 0) {
            return;
        }

        float normalized = progress / 9.0f;
        if (normalized < 0.0f) {
            normalized = 0.0f;
        } else if (normalized > 1.0f) {
            normalized = 1.0f;
        }

        BlockDestroyEvent.fire(pos, normalized);
    }
}
