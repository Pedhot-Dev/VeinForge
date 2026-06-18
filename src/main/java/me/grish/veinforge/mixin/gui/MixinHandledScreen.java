package me.grish.veinforge.mixin.gui;

import me.grish.veinforge.macro.MacroManager;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to prevent slot clicks when macro is running.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class MixinHandledScreen {

    @Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V", at = @At("HEAD"), cancellable = true)
    private void veinforge$onMouseClick(Slot slot, int slotId, int button, ContainerInput actionType, CallbackInfo ci) {
        if (MacroManager.getInstance().isRunning()) {
            ci.cancel();
        }
    }
}
