package me.grish.veinforge.event;

import net.minecraft.network.protocol.Packet;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Events for packet interception.
 */
public class PacketEvent {

   private static final List<Consumer<Received>> receivedListeners = new ArrayList<>();
   private static final List<Consumer<Sent>> sentListeners = new ArrayList<>();

   public static void registerReceived(Consumer<Received> listener) {
      receivedListeners.add(listener);
   }

   public static void registerSent(Consumer<Sent> listener) {
      sentListeners.add(listener);
   }

   public static void fireReceived(Received event) {
      for (Consumer<Received> listener : receivedListeners) {
         listener.accept(event);
      }
   }

   public static void fireSent(Sent event) {
      for (Consumer<Sent> listener : sentListeners) {
         listener.accept(event);
      }
   }

   public static class Received {
      private final Packet<?> packet;
      private boolean cancelled = false;

      public Received(Packet<?> packet) {
         this.packet = packet;
      }

      public Packet<?> getPacket() {
         return this.packet;
      }

      public boolean isCancelled() {
         return this.cancelled;
      }

      public void setCancelled(boolean cancelled) {
         this.cancelled = cancelled;
      }
   }

   public static class Sent {
      private final Packet<?> packet;
      private boolean cancelled = false;

      public Sent(Packet<?> packet) {
         this.packet = packet;
      }

      public Packet<?> getPacket() {
         return this.packet;
      }

      public boolean isCancelled() {
         return this.cancelled;
      }

      public void setCancelled(boolean cancelled) {
         this.cancelled = cancelled;
      }
   }
}
