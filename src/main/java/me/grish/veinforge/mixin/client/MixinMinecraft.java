package me.grish.veinforge.mixin.client;

import me.grish.veinforge.macro.MacroManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for MinecraftClient to intercept key presses and focus handling.
 */
@Mixin(Minecraft.class)
public class MixinMinecraft {

   @Shadow
   public LocalPlayer player;

   /**
    * Prevent losing focus when macro is running.
    */
   @Inject(method = "setWindowActive", at = @At("HEAD"), cancellable = true)
   private void onWindowFocusChanged(boolean focused, CallbackInfo ci) {
      if (!focused && MacroManager.getInstance().isRunning()) {
         ci.cancel();
      }
   }
}
