package me.grish.veinforge.macro.impl.PowderMacro.states;

import me.grish.veinforge.macro.impl.PowderMacro.PowderMacro;
import net.minecraft.core.BlockPos;

public class TreasureState implements PowderMacroState {
   private static final long CHEST_SOLVE_TIMEOUT_MS = 15_000L;

   private final BlockPos targetChest;
   private long startMs;

   public TreasureState(BlockPos targetChest) {
      this.targetChest = targetChest == null ? BlockPos.ZERO : targetChest.immutable();
   }

   @Override
   public void onStart(PowderMacro macro) {
      this.startMs = System.currentTimeMillis();
      log("Starting chest solve for " + targetChest);
      macro.beginChestSolve(targetChest);
   }

   @Override
   public PowderMacroState onTick(PowderMacro macro) {
      if (!macro.validateEnvironment()) {
         return null;
      }

      if (!macro.isChestUnlockerRunning()) {
         log("Chest solver finished. Returning to gemstone mining.");
         macro.completeChestSolve();
         macro.restartMiner();
         return new GemstoneState();
      }

      if (System.currentTimeMillis() - startMs >= CHEST_SOLVE_TIMEOUT_MS) {
         logError("Chest solve timed out. Returning to gemstone mining.");
         macro.stopChestUnlocker();
         macro.completeChestSolve();
         macro.restartMiner();
         return new GemstoneState();
      }

      return this;
   }

   @Override
   public void onEnd(PowderMacro macro) {
      log("Exiting treasure state.");
   }
}
