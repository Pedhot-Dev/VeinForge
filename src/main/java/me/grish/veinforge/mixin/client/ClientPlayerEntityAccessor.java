package me.grish.veinforge.mixin.client;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin for Entity internals (lastYaw/lastPitch).
 */
@Mixin(Entity.class)
public interface ClientPlayerEntityAccessor {

   @Accessor("yRotO")
   float getLastYaw();

   @Accessor("yRotO")
   void setLastYaw(float yaw);

   @Accessor("xRotO")
   float getLastPitch();

   @Accessor("xRotO")
   void setLastPitch(float pitch);
}
