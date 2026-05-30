package me.grish.veinforge.feature.impl;

import lombok.Getter;
import me.grish.veinforge.feature.AbstractFeature;
import me.grish.veinforge.util.helper.Clock;
import me.grish.veinforge.util.helper.FifoQueue;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.world.phys.Vec3;

public class LagDetector extends AbstractFeature {

   private static LagDetector instance;
   private final Clock lagTimer = new Clock();
   private final Clock recentlyLagged = new Clock();
   private final FifoQueue<Float> tpsHistory = new FifoQueue<>(20);
   @Getter
   private long lastReceivedPacketTime = -1;
   private Vec3 lastPacketPosition = null;
   private float timeJoined = 0;

   public static LagDetector getInstance() {
      if (instance == null) {
         instance = new LagDetector();
      }
      return instance;
   }

   @Override
   public String getName() {
      return "LagDetector";
   }

   public Vec3 getLastPacketPosition() {
      if (lastPacketPosition == null) {
         if (mc.player == null) return Vec3.ZERO;
         return mc.player.position();
      }

      return lastPacketPosition;
   }

   public boolean isLagging() {
      return getTimeSinceLastTick() > 1.3;
   }

   public boolean wasJustLagging() {
      return recentlyLagged.isScheduled() && !recentlyLagged.passed();
   }

   public long getLaggingTime() {
      return System.currentTimeMillis() - lastReceivedPacketTime;
   }

   @Override
   protected void onWorldLoad(ClientLevel world) {
      timeJoined = System.currentTimeMillis();
      tpsHistory.clear();
   }

   @Override
   protected void onPacketReceive(Packet<?> packet) {
      if (mc.player == null || mc.level == null) return;
      if (!(packet instanceof ClientboundSetTimePacket)) return;

      long now = System.currentTimeMillis();
      float timeElapsed = (now - lastReceivedPacketTime) / 1000F;
      tpsHistory.add(clamp(20F / timeElapsed));
      lastReceivedPacketTime = now;
      lastPacketPosition = mc.player.position();
   }

   @Override
   protected void onTick() {
      if (mc.player == null || mc.level == null) return;
      if (lastReceivedPacketTime == -1) return;
      if (isLagging()) {
         recentlyLagged.schedule(900);
      }
      if (recentlyLagged.isScheduled() && recentlyLagged.passed()) {
         recentlyLagged.reset();
      }
   }

   public float getTickRate() {
      if (mc.player == null || mc.level == null) return 0F;
      if (System.currentTimeMillis() - timeJoined < 5000) return 20F;

      int ticks = 0;
      float sumTickRates = 0f;
      for (float tickRate : tpsHistory) {
         if (tickRate > 0) {
            sumTickRates += tickRate;
            ticks++;
         }
      }
      return ticks > 0 ? sumTickRates / ticks : 0F;
   }

   public float getTimeSinceLastTick() {
      long now = System.currentTimeMillis();
      if (now - timeJoined < 5000) return 0F;
      return (now - lastReceivedPacketTime) / 1000F;
   }

   private float clamp(float value) {
      return Math.max((float) 0.0, Math.min((float) 20.0, value));
   }

}
