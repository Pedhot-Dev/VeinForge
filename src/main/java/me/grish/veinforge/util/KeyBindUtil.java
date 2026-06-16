package me.grish.veinforge.util;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.platform.InputConstants;
import me.grish.veinforge.mixin.client.KeyBindingAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KeyBindUtil {
   private static final Minecraft mc = Minecraft.getInstance();
   public static final KeyMapping[] allKeys = {
           mc.options.keyAttack,
           mc.options.keyUse,
           mc.options.keyDown,
           mc.options.keyUp,
           mc.options.keyLeft,
           mc.options.keyRight,
           mc.options.keyJump,
           mc.options.keyShift,
           mc.options.keySprint,
   };

   public static final KeyMapping[] allKeys2 = {
           mc.options.keyDown,
           mc.options.keyUp,
           mc.options.keyLeft,
           mc.options.keyRight,
           mc.options.keyJump,
   };
   public static final Map<KeyMapping, Integer> keyBindMap = ImmutableMap.of(
           mc.options.keyUp, 0,
           mc.options.keyLeft, 90,
           mc.options.keyDown, 180,
           mc.options.keyRight, -90
   );

   public static void rightClick() {
      KeyMapping.click(((KeyBindingAccessor) mc.options.keyUse).getBoundKey());
   }

   public static void leftClick() {
      KeyMapping.click(((KeyBindingAccessor) mc.options.keyAttack).getBoundKey());
   }

   public static void middleClick() {
      KeyMapping.click(((KeyBindingAccessor) mc.options.keyPickItem).getBoundKey());
   }

   public static void onTick(KeyMapping key) {
      if (mc.screen == null) {
         KeyMapping.set(((KeyBindingAccessor) key).getBoundKey(), true);
      }
   }

   public static int getRightClickDelayTimer() {
      return mc.rightClickDelay;
   }

   public static void resetRightClickDelayTimer() {
      mc.rightClickDelay = 0;
   }

   public static void setKeyBindState(KeyMapping key, boolean pressed) {
      if (pressed) {
         if (mc.screen != null && key != null) {
            realSetKeyBindState(key, false);
            return;
         }
      }
      realSetKeyBindState(key, pressed);
   }

   private static void realSetKeyBindState(KeyMapping key, boolean pressed) {
      if (key == null) return;
      InputConstants.Key boundKey = ((KeyBindingAccessor) key).getBoundKey();
      if (pressed) {
         if (!key.isDown()) {
            KeyMapping.set(boundKey, true);
         }
      } else {
         if (key.isDown()) {
            KeyMapping.set(boundKey, false);
         }
      }
   }

   public static void stopMovement() {
      stopMovement(false);
   }

   public static void stopMovement(boolean ignoreAttack) {
      realSetKeyBindState(mc.options.keyUp, false);
      realSetKeyBindState(mc.options.keyDown, false);
      realSetKeyBindState(mc.options.keyRight, false);
      realSetKeyBindState(mc.options.keyLeft, false);
      if (!ignoreAttack) {
         realSetKeyBindState(mc.options.keyAttack, false);
         realSetKeyBindState(mc.options.keyUse, false);
      }
      realSetKeyBindState(mc.options.keyShift, false);
      realSetKeyBindState(mc.options.keyJump, false);
      realSetKeyBindState(mc.options.keySprint, false);
   }

   public static void holdThese(boolean withAttack, KeyMapping... keyBinding) {
      releaseAllExcept(keyBinding);
      for (KeyMapping key : keyBinding) {
         if (key != null)
            realSetKeyBindState(key, true);
      }
      if (withAttack) {
         realSetKeyBindState(mc.options.keyAttack, true);
      }
   }

   public static void holdThese(KeyMapping... keyBinding) {
      releaseAllExcept(keyBinding);
      for (KeyMapping key : keyBinding) {
         if (key != null)
            realSetKeyBindState(key, true);
      }
   }

   public static void releaseAllExcept(KeyMapping... keyBinding) {
      for (KeyMapping key : allKeys) {
         if (key != null && !contains(keyBinding, key) && key.isDown()) {
            realSetKeyBindState(key, false);
         }
      }
   }

   public static boolean contains(KeyMapping[] keyBinding, KeyMapping key) {
      if (key == null) return false;
      InputConstants.Key boundKey = ((KeyBindingAccessor) key).getBoundKey();
      for (KeyMapping keyBind : keyBinding) {
         if (keyBind != null && ((KeyBindingAccessor) keyBind).getBoundKey().getValue() == boundKey.getValue())
            return true;
      }
      return false;
   }

   public static boolean areAllKeybindsReleased() {
      for (KeyMapping key : allKeys2) {
         if (key != null && key.isDown())
            return false;
      }
      return true;
   }

   public static KeyMapping[] getHeldKeybinds() {
      KeyMapping[] keybinds = new KeyMapping[allKeys.length];
      int i = 0;
      for (KeyMapping key : allKeys) {
         if (key != null && key.isDown()) {
            keybinds[i] = key;
            i++;
         }
      }
      return keybinds;
   }

   public static List<KeyMapping> getNeededKeyPresses(Vec3 orig, Vec3 dest) {
      List<KeyMapping> keys = new ArrayList<>();

      double[] delta = {orig.x - dest.x, orig.z - dest.z};
      float requiredAngle = (float) (Mth.atan2(delta[0], -delta[1]) * (180.0 / Math.PI));

      float angleDifference = AngleUtil.normalizeAngle(requiredAngle - mc.player.getYRot()) * -1;

      keyBindMap.forEach((key, yaw) -> {
         if (Math.abs(yaw - angleDifference) < 67.5 || Math.abs(yaw - (angleDifference + 360.0)) < 67.5) {
            keys.add(key);
         }
      });
      return keys;
   }

   public static List<KeyMapping> getNeededKeyPresses(float neededYaw) {
      List<KeyMapping> keys = new ArrayList<>();
      neededYaw = AngleUtil.normalizeAngle(neededYaw - mc.player.getYRot()) * -1;
      float finalNeededYaw = neededYaw;
      keyBindMap.forEach((key, yaw) -> {
         if (Math.abs(yaw - finalNeededYaw) < 67.5 || Math.abs(yaw - (finalNeededYaw + 360.0)) < 67.5) {
            keys.add(key);
         }
      });
      return keys;
   }

   public static List<KeyMapping> getOppositeKeys(List<KeyMapping> kbs) {
      List<KeyMapping> keys = new ArrayList<>();
      kbs.forEach(key -> {
         switch (key.getDefaultKey().getValue()) {
            case GLFW.GLFW_KEY_W:
               keys.add(mc.options.keyDown);
               break;
            case GLFW.GLFW_KEY_A:
               keys.add(mc.options.keyRight);
               break;
            case GLFW.GLFW_KEY_S:
               keys.add(mc.options.keyLeft);
               break;
            case GLFW.GLFW_KEY_D:
               keys.add(mc.options.keyUp);
               break;
         }
      });
      return keys;
   }

   public static List<KeyMapping> getKeyPressesToDecelerate(Vec3 orig, Vec3 dest) {
      return getOppositeKeys(getNeededKeyPresses(orig, dest));
   }
}
