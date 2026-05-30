package me.grish.veinforge.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.AABB;

import java.lang.reflect.Field;

/**
 * Shared helpers for fishing-oriented macros and features.
 */
public final class FishingUtil {
   private static Field cachedFishingHookField;

   private FishingUtil() {
   }

   public static int countNearbyArmorStandsContainingName(Minecraft mc, String marker, double horizontalRadius, double verticalRadius) {
      if (mc == null || mc.player == null || mc.level == null || marker == null || marker.trim().isEmpty()) {
         return 0;
      }

      int count = 0;
      AABB scan = mc.player.getBoundingBox().inflate(horizontalRadius, verticalRadius, horizontalRadius);
      for (Entity entity : mc.level.getEntities(mc.player, scan, e -> e instanceof ArmorStand)) {
         String name = cleanEntityName(entity);
         if (name.contains(marker)) {
            count++;
         }
      }
      return count;
   }

   public static FishingHook getPlayerFishingHook(Minecraft mc, double fallbackMaxDistSq) {
      if (mc == null || mc.player == null) {
         return null;
      }

      if (cachedFishingHookField == null) {
         cachedFishingHookField = findFishingHookField(mc.player.getClass());
      }

      if (cachedFishingHookField != null) {
         try {
            Object value = cachedFishingHookField.get(mc.player);
            if (value instanceof FishingHook) {
               return (FishingHook) value;
            }
         } catch (Exception ignored) {
         }
      }

      if (mc.level == null) {
         return null;
      }

      double hookScanRadius = Math.sqrt(Math.max(1.0D, fallbackMaxDistSq));
      AABB scan = mc.player.getBoundingBox().inflate(hookScanRadius, 16.0D, hookScanRadius);
      FishingHook closestOwned = null;
      double bestOwnedDistSq = Double.MAX_VALUE;
      FishingHook closestAny = null;
      double bestAnyDistSq = Double.MAX_VALUE;

      for (Entity entity : mc.level.getEntities(mc.player, scan, e -> e instanceof FishingHook)) {
         FishingHook hook = (FishingHook) entity;
         double distSq = hook.distanceToSqr(mc.player);
         if (isOwnedByPlayer(mc, hook) && distSq < bestOwnedDistSq) {
            bestOwnedDistSq = distSq;
            closestOwned = hook;
         }
         if (distSq < bestAnyDistSq) {
            bestAnyDistSq = distSq;
            closestAny = hook;
         }
      }

      if (closestOwned != null) {
         return closestOwned;
      }
      if (closestAny != null && bestAnyDistSq <= fallbackMaxDistSq) {
         return closestAny;
      }
      return null;
   }

   public static String cleanEntityName(Entity entity) {
      if (entity == null) {
         return "";
      }

      String name = "";
      if (entity.getCustomName() != null) {
         name = entity.getCustomName().getString();
      }
      if (name == null || name.trim().isEmpty()) {
         name = entity.getName().getString();
      }
      if (name == null) {
         return "";
      }
      String stripped = ChatFormatting.stripFormatting(name);
      return stripped == null ? "" : stripped;
   }

   private static boolean isOwnedByPlayer(Minecraft mc, FishingHook hook) {
      try {
         return hook.getPlayerOwner() == mc.player;
      } catch (Throwable ignored) {
      }
      try {
         return hook.getOwner() == mc.player;
      } catch (Throwable ignored) {
      }
      return false;
   }

   private static Field findFishingHookField(Class<?> startClass) {
      Class<?> type = startClass;
      while (type != null) {
         try {
            Field field = type.getDeclaredField("fishing");
            field.setAccessible(true);
            return field;
         } catch (NoSuchFieldException ignored) {
            type = type.getSuperclass();
         }
      }
      return null;
   }
}
