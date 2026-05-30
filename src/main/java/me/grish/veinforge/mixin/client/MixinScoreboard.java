package me.grish.veinforge.mixin.client;

import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * Scoreboard null-safety fixes (from Patcher).
 * Prevents log spam from null teams/objectives.
 */
@Mixin(Scoreboard.class)
public abstract class MixinScoreboard {

   @Shadow
   public abstract PlayerTeam getPlayerTeam(String name);

   @Inject(method = "removePlayerTeam", at = @At("HEAD"), cancellable = true)
   private void veinforge$checkIfTeamIsNull(PlayerTeam team, CallbackInfo ci) {
      if (team == null) ci.cancel();
   }

   @Redirect(method = "removePlayerTeam", at = @At(value = "INVOKE", target = "Ljava/util/Map;remove(Ljava/lang/Object;)Ljava/lang/Object;", ordinal = 0, remap = false))
   private <K, V> V veinforge$checkIfRegisteredNameIsNull(Map<K, V> instance, K o) {
      if (o != null) return instance.remove(o);
      return null;
   }

   @Inject(method = "removeObjective", at = @At("HEAD"), cancellable = true)
   private void veinforge$checkIfObjectiveIsNull(Objective objective, CallbackInfo ci) {
      if (objective == null) ci.cancel();
   }

   @Redirect(method = "removeObjective", at = @At(value = "INVOKE", target = "Ljava/util/Map;remove(Ljava/lang/Object;)Ljava/lang/Object;", ordinal = 0, remap = false))
   private <K, V> V veinforge$checkIfNameIsNull(Map<K, V> instance, K o) {
      if (o != null) return instance.remove(o);
      return null;
   }

   @Inject(method = "addPlayerTeam", at = @At(value = "CONSTANT", args = "stringValue=A team with the name '"), cancellable = true)
   private void veinforge$returnExistingTeam(String name, CallbackInfoReturnable<PlayerTeam> cir) {
      PlayerTeam existing = this.getPlayerTeam(name);
      if (existing != null) {
         cir.setReturnValue(existing);
      }
   }
}
