package me.grish.veinforge.event;

import lombok.Getter;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Event fired when a particle spawns.
 */
@Getter
public class SpawnParticleEvent {

   private static final List<Consumer<SpawnParticleEvent>> listeners = new ArrayList<>();

   private final ParticleType<?> particleType;
   private final boolean longDistance;
   private final double x;
   private final double y;
   private final double z;
   private final double velocityX;
   private final double velocityY;
   private final double velocityZ;
   private boolean cancelled = false;

   public SpawnParticleEvent(
           ParticleType<?> particleType,
           boolean longDistance,
           double x, double y, double z,
           double velocityX, double velocityY, double velocityZ
   ) {
      this.particleType = particleType;
      this.longDistance = longDistance;
      this.x = x;
      this.y = y;
      this.z = z;
      this.velocityX = velocityX;
      this.velocityY = velocityY;
      this.velocityZ = velocityZ;
   }

   public static void register(Consumer<SpawnParticleEvent> listener) {
      listeners.add(listener);
   }

   public static SpawnParticleEvent fire(
           ParticleType<?> particleType,
           boolean longDistance,
           double x, double y, double z,
           double velocityX, double velocityY, double velocityZ
   ) {
      SpawnParticleEvent event = new SpawnParticleEvent(particleType, longDistance, x, y, z, velocityX, velocityY, velocityZ);
      for (Consumer<SpawnParticleEvent> listener : listeners) {
         listener.accept(event);
      }
      return event;
   }

   public Vec3 getPos() {
      return new Vec3(x, y, z);
   }

   public boolean isCancelled() {
      return this.cancelled;
   }

   public void cancel() {
      this.cancelled = true;
   }
}
