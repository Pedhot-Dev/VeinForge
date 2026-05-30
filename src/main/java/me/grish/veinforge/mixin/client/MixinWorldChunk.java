package me.grish.veinforge.mixin.client;

import me.grish.veinforge.event.BlockChangeEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to fire BlockChangeEvent when blocks change.
 */
@Mixin(LevelChunk.class)
public class MixinWorldChunk {

   @Inject(method = "setBlockState", at = @At("RETURN"))
   private void veinforge$onSetBlockState(BlockPos pos, BlockState state, int moved, CallbackInfoReturnable<BlockState> cir) {
      final BlockState old = cir.getReturnValue();
      if (old == null || state == old) return;
      BlockChangeEvent.fire(pos, old, state);
   }
}
