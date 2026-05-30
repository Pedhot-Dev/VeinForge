package me.grish.veinforge.feature.impl.AutoMobKiller.states;

import me.grish.veinforge.feature.impl.AutoMobKiller.AutoMobKiller;

public class StartingState implements AutoMobKillerState {

   @Override
   public void onStart(AutoMobKiller mobKiller) {
      log("Entering Starting State");
   }

   @Override
   public AutoMobKillerState onTick(AutoMobKiller mobKiller) {
      return new FindMobState();
   }

   @Override
   public void onEnd(AutoMobKiller mobKiller) {
      log("Exiting Starting State");
   }

}
