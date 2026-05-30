package me.grish.veinforge.failsafe.impl;

import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import me.grish.veinforge.failsafe.AbstractFailsafe;
import me.grish.veinforge.macro.MacroManager;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.Set;

public class BadEffectFailsafe extends AbstractFailsafe {

   @Getter
   private static final BadEffectFailsafe instance = new BadEffectFailsafe();
   private final Set<Holder<MobEffect>> BAD_EFFECTS = ImmutableSet.of(
           MobEffects.POISON,
           MobEffects.WITHER,
           MobEffects.WEAKNESS,
           MobEffects.BLINDNESS,
           MobEffects.HUNGER,
           MobEffects.SLOWNESS,
           MobEffects.MINING_FATIGUE
   );

   @Override
   public String getName() {
      return "BadEffectFailsafe";
   }

   @Override
   public Failsafe getFailsafeType() {
      return Failsafe.BAD_EFFECTS;
   }

   @Override
   public int getPriority() {
      return 7;
   }

   @Override
   public boolean onTick() {
      if (mc.player == null) return false;
      for (MobEffectInstance effect : mc.player.getActiveEffects()) {
         if (BAD_EFFECTS.contains(effect.getEffect())) return true;
      }

      return false;
   }

   @Override
   public boolean react() {
      MacroManager.getInstance().disable();
      warn("Bad effect detected! Disabling macro.");
      return true;
   }

}
