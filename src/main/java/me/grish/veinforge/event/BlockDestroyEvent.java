package me.grish.veinforge.event;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Event fired when a block is being destroyed (mining progress).
 */
public record BlockDestroyEvent(BlockPos block, float progress) {

   private static final List<Consumer<BlockDestroyEvent>> listeners = new ArrayList<>();

   public static void register(Consumer<BlockDestroyEvent> listener) {
      listeners.add(listener);
   }

   public static void fire(BlockPos block, float progress) {
      BlockDestroyEvent event = new BlockDestroyEvent(block, progress);
      for (Consumer<BlockDestroyEvent> listener : listeners) {
         listener.accept(event);
      }
   }
}
