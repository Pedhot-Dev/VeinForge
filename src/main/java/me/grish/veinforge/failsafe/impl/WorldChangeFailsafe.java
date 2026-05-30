package me.grish.veinforge.failsafe.impl;

import lombok.Getter;
import me.grish.veinforge.failsafe.AbstractFailsafe;
import me.grish.veinforge.feature.impl.AutoWarp;
import me.grish.veinforge.macro.MacroManager;

public class WorldChangeFailsafe extends AbstractFailsafe {

   @Getter
   private static final WorldChangeFailsafe instance = new WorldChangeFailsafe();
   private static final Failsafe failsafeType = Failsafe.TELEPORT;

   @Override
   public String getName() {
      return "WorldChangeFailsafe";
   }

   @Override
   public Failsafe getFailsafeType() {
      return failsafeType;
   }

   @Override
   public int getPriority() {
      return 5;
   }

   @Override
   public boolean react() {
      warn("Stopping macro due to world change.");
      MacroManager.getInstance().disable();
      return true;
   }

   @Override
   public boolean onWorldUnload() {
      if (!MacroManager.getInstance().isEnabled()) return false;
      return AutoWarp.getInstance() != null && AutoWarp.getInstance().isDoneWarping();
   }

}
