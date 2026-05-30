package me.grish.veinforge.util;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;

import java.util.HashMap;
import java.util.Map;

/**
 * Rising-edge key press detection for raw GLFW key codes.
 */
public class KeyPressUtil {

   private static final Map<Integer, Boolean> lastDownByKey = new HashMap<>();

   /**
    * Updates tracked key state and returns true only on a down transition.
    *
    * @param windowHandle GLFW window handle
    * @param keyCode      GLFW key code
    * @param active       if false, the key state is still synced but never fires
    */
   public static boolean wasPressed(Window window, int keyCode, boolean active) {
      if (keyCode == -1) {
         return false;
      }

      if (window == null) {
         return false;
      }

      boolean down = InputConstants.isKeyDown(window, keyCode);
      boolean lastDown = lastDownByKey.getOrDefault(keyCode, false);
      lastDownByKey.put(keyCode, down);
      return active && down && !lastDown;
   }

   public static String getKeyName(int keyCode) {
      if (keyCode < 0) {
         return "Unbound";
      }

      try {
         return InputConstants.Type.KEYSYM.getOrCreate(keyCode).getDisplayName().getString();
      } catch (Exception ignored) {
         return String.valueOf(keyCode);
      }
   }
}
