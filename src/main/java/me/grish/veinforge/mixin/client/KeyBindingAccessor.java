package me.grish.veinforge.mixin.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin for KeyBinding internals.
 */
@Mixin(KeyMapping.class)
public interface KeyBindingAccessor {

   @Accessor("key")
   InputConstants.Key getBoundKey();
}
