package me.grish.veinforge.event;

import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Event fired when an entity is spawned, despawned, or moved.
 */
public class UpdateEntityEvent {

   public static final byte TYPE_SPAWNED = 0;
   public static final byte TYPE_DESPAWNED = 1;
   public static final byte TYPE_MOVED = 2;
   private static final List<Consumer<UpdateEntityEvent>> listeners = new ArrayList<>();
   public final LivingEntity entity;
   public final byte updateType;
   public long newHash;

   public UpdateEntityEvent(LivingEntity entity) {
      this.entity = entity;
      this.updateType = TYPE_SPAWNED;
   }

   public UpdateEntityEvent(LivingEntity entity, byte updateType) {
      this.entity = entity;
      this.updateType = updateType;
   }

   public UpdateEntityEvent(LivingEntity entity, long newHash) {
      this.entity = entity;
      this.updateType = TYPE_MOVED;
      this.newHash = newHash;
   }

   public static void register(Consumer<UpdateEntityEvent> listener) {
      listeners.add(listener);
   }

   public static void fire(UpdateEntityEvent event) {
      for (Consumer<UpdateEntityEvent> listener : listeners) {
         listener.accept(event);
      }
   }
}
