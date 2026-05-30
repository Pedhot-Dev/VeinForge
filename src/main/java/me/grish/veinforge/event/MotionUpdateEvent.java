package me.grish.veinforge.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Event fired before sending player movement packets to server.
 * Allows modifying the yaw/pitch that gets sent.
 */
@Getter
@AllArgsConstructor
public class MotionUpdateEvent {

   private static final List<Consumer<MotionUpdateEvent>> listeners = new ArrayList<>();

   public float yaw;
   public float pitch;

   public static void register(Consumer<MotionUpdateEvent> listener) {
      listeners.add(listener);
   }

   public static void fire(MotionUpdateEvent event) {
      for (Consumer<MotionUpdateEvent> listener : listeners) {
         listener.accept(event);
      }
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }
}
