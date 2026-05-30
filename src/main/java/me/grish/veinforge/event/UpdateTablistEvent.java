package me.grish.veinforge.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Event fired when tablist updates.
 */
public record UpdateTablistEvent(List<String> tablist, long timestamp) {

   private static final List<Consumer<UpdateTablistEvent>> listeners = new ArrayList<>();

   public static void register(Consumer<UpdateTablistEvent> listener) {
      listeners.add(listener);
   }

   public static void fire(List<String> tablist) {
      UpdateTablistEvent event = new UpdateTablistEvent(tablist, System.currentTimeMillis());
      for (Consumer<UpdateTablistEvent> listener : listeners) {
         listener.accept(event);
      }
   }
}
