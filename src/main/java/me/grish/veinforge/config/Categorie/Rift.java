package me.grish.veinforge.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.*;

public class Rift {

   @ConfigOption(
           name = "Enable Rift",
           desc = "Master switch for the Rift module"
   )
   @ConfigEditorBoolean
   public boolean riftEnabled = false;

   @ConfigOption(
           name = "Auto Heal",
           desc = "Automatically use healing items during Rift fights"
   )
   @ConfigEditorBoolean
   public boolean riftAutoHeal = false;

   @ConfigOption(
           name = "Auto Impel",
           desc = "Automatically respond to Impel prompts"
   )
   @ConfigEditorBoolean
   public boolean riftAutoImpel = false;

   @ConfigOption(
           name = "Auto Holy Ice",
           desc = "Use Holy Ice during Twinclaws"
   )
   @ConfigEditorBoolean
   public boolean riftAutoHolyIce = false;

   @ConfigOption(
           name = "Auto Stake Swap",
           desc = "Swap to a Steak Stake when the boss is stakeable"
   )
   @ConfigEditorBoolean
   public boolean riftAutoStakeSwap = false;

   @ConfigOption(
           name = "Auto Stun Snake",
           desc = "Automatically stun snakes while mining in Living Cave or Living Stillness"
   )
   @ConfigEditorBoolean
   public boolean riftAutoStunSnake = false;

   @ConfigOption(
           name = "Predict Snake Tail",
           desc = "Render predicted snake tail positions in Living Cave or Living Stillness"
   )
   @ConfigEditorBoolean
   public boolean riftPredictSnakeTail = false;
}
