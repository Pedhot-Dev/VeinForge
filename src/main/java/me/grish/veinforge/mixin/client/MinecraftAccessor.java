package me.grish.veinforge.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor mixin for MinecraftClient internals.
 */
@Mixin(Minecraft.class)
public interface MinecraftAccessor {

   @Accessor("missTime")
   int getAttackCooldown();

   @Accessor("missTime")
   void setAttackCooldown(int attackCooldown);

   @Accessor("rightClickDelay")
   int getItemUseCooldown();

   @Accessor("rightClickDelay")
   void setItemUseCooldown(int itemUseCooldown);

   @Invoker("startUseItem")
   void rightClick();

   @Invoker("startAttack")
   boolean leftClick();

   @Invoker("pickBlock")
   void middleClick();

   @Accessor("rightClickDelay")
   int getRightClickDelayTimer();

   @Accessor("rightClickDelay")
   void setRightClickDelayTimer(int delay);
}
