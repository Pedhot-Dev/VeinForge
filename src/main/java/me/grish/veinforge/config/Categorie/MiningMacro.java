package me.grish.veinforge.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.*;

public class MiningMacro {

   @ConfigOption(name = "Ore Type", desc = "")
   @ConfigEditorDropdown(
           values = {
                   "Mithril",
                   "Diamond",
                   "Emerald",
                   "Redstone",
                   "Lapis",
                   "Gold",
                   "Iron",
                   "Coal",
           }
   )
   public int oreType = 0;

   @ConfigOption(name = "Mine Gray Mithril (Gray Wool)", desc = "")
   @ConfigEditorBoolean
   public boolean mineGrayMithril = true;

   @ConfigOption(name = "Mine Green Mithril (Prismarine)", desc = "")
   @ConfigEditorBoolean
   public boolean mineGreenMithril = true;

   @ConfigOption(name = "Mine Blue Mithril (Blue Wool)", desc = "")
   @ConfigEditorBoolean
   public boolean mineBlueMithril = true;

   @ConfigOption(name = "Mine Titanium", desc = "")
   @ConfigEditorBoolean
   public boolean mineTitanium = true;

   @ConfigOption(name = "Default Priority - Gray Mithril", desc = "")
   @ConfigEditorSlider(minValue = 0, maxValue = 30, minStep = 1)
   public int mithrilPriorityGrayDefault = 1;

   @ConfigOption(name = "Default Priority - Green Mithril", desc = "")
   @ConfigEditorSlider(minValue = 0, maxValue = 30, minStep = 1)
   public int mithrilPriorityGreenDefault = 3;

   @ConfigOption(name = "Default Priority - Blue Mithril", desc = "")
   @ConfigEditorSlider(minValue = 0, maxValue = 30, minStep = 1)
   public int mithrilPriorityBlueDefault = 6;

   @ConfigOption(name = "Default Priority - Titanium", desc = "")
   @ConfigEditorSlider(minValue = 0, maxValue = 30, minStep = 1)
   public int mithrilPriorityTitaniumDefault = 10;
}
