package me.grish.veinforge.mixin.client;

import me.grish.veinforge.event.SpawnParticleEvent;
import me.grish.veinforge.event.UpdateEntityEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class MixinClientWorld {

    @Inject(
            method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void veinforge$onAddParticle(
            ParticleOptions effect,
            boolean longDistance,
            boolean alwaysSpawn,
            double x,
            double y,
            double z,
            double velocityX,
            double velocityY,
            double velocityZ,
            CallbackInfo ci
    ) {
        SpawnParticleEvent event = SpawnParticleEvent.fire(
                effect.getType(),
                longDistance,
                x, y, z,
                velocityX, velocityY, velocityZ
        );
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "addEntity(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
    private void veinforge$onAddEntity(Entity entity, CallbackInfo ci) {
        if (entity instanceof LivingEntity livingEntity) {
            UpdateEntityEvent.fire(new UpdateEntityEvent(livingEntity));
        }
    }

    @Inject(method = "removeEntity(ILnet/minecraft/world/entity/Entity$RemovalReason;)V", at = @At("HEAD"))
    private void veinforge$onRemoveEntity(int entityId, Entity.RemovalReason removalReason, CallbackInfo ci) {
        ClientLevel world = (ClientLevel) (Object) this;
        Entity entity = world.getEntity(entityId);
        if (entity instanceof LivingEntity livingEntity) {
            UpdateEntityEvent.fire(new UpdateEntityEvent(livingEntity, UpdateEntityEvent.TYPE_DESPAWNED));
        }
    }
}
