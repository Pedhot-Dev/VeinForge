package me.grish.veinforge.util;

public final class StrafeUtil {

   public static volatile boolean enabled = false;
   public static volatile boolean forceStop = false;
   public static volatile float yaw = 0.0f;
   public static volatile float strength = 1.0f;

   public static boolean shouldEnable() {
      return !forceStop && enabled;
   }
}
