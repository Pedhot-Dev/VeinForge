package me.grish.veinforge.failsafe.impl;

import lombok.Getter;
import me.grish.veinforge.failsafe.AbstractFailsafe;
import me.grish.veinforge.macro.MacroManager;
import me.grish.veinforge.util.EntityUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

// TODO: Check if it causes excessive lag before re-implementing!
public class PlayerFailsafe extends AbstractFailsafe {

   @Getter
   private static final PlayerFailsafe instance = new PlayerFailsafe();
   private final Minecraft mc = Minecraft.getInstance();
   private final Map<Entity, Long> playerStaringTimes = new HashMap<>();

   @Override
   public String getName() {
      return "PlayerFailsafe";
   }

   @Override
   public Failsafe getFailsafeType() {
      return Failsafe.TELEPORT;
   }

   @Override
   public int getPriority() {
      return 5;
   }

   @Override
   public boolean onTick() {
      if (mc.level == null || mc.player == null) return false;

      List<Entity> players = StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false).filter(
              (entity) -> entity instanceof AbstractClientPlayer && entity != mc.player && !EntityUtil.isNpc(entity)
      ).collect(Collectors.toList());

      boolean playerBlocking = false;
      long currentTime = System.currentTimeMillis();
      playerStaringTimes.keySet().removeIf(player -> !players.contains(player));

      for (Entity player : players) {
         double distanceSquared = player.blockPosition().distSqr(mc.player.blockPosition());
         boolean isBlocking = distanceSquared < 3;
         boolean isLookingAtUs = isPlayerLookingAtMe(player);

         if (isLookingAtUs && isBlocking) {
            if (!playerStaringTimes.containsKey(player)) {
               playerStaringTimes.put(player, currentTime);
               return false;
            }

            long staringDuration = currentTime - playerStaringTimes.get(player);

            if (staringDuration > 1000) {
               playerBlocking = true;
               break;
            }
         } else {
            playerStaringTimes.remove(player);
         }
      }

      return playerBlocking;
   }

   private boolean isPlayerLookingAtMe(Entity player) {
      Vec3 lookVec = player.getViewVector(1.0F);
      Vec3 playerToMeVec = new Vec3(
              mc.player.getX() - player.getX(),
              mc.player.getBoundingBox().minY + mc.player.getEyeHeight(mc.player.getPose()) - (player.getY() + player.getEyeHeight(player.getPose())),
              mc.player.getZ() - player.getZ()
      );

      double d0 = playerToMeVec.length();
      playerToMeVec = playerToMeVec.normalize();

      double dot = lookVec.dot(playerToMeVec);
      double fovCosine = Math.cos(Math.toRadians(30F));
      return dot > fovCosine;
   }

   @Override
   public boolean onChat(String message) {
      return message.toLowerCase().contains(mc.getUser().getName().toLowerCase());
   }

   @Override
   public boolean react() {
      warn("Stopping macro due to player nearby.");
      MacroManager.getInstance().disable();
      return true;
   }

}
