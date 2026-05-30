package me.grish.veinforge.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.*;

public class PowderMacro {

   @ConfigOption(
           name = "Powder Type",
           desc = "Select which powder the macro targets"
   )
   @ConfigEditorDropdown(values = {"Gemstone", "Mithril"})
   public int powderType = 0;

   @ConfigOption(
           name = "Area Lock",
           desc = "Restrict macro to a Crystal Hollows quadrant"
   )
   @ConfigEditorDropdown(values = {"None", "Jungle", "Goblin Holdout", "Mithril Deposits", "Precursor Remnants"})
   public int areaLock = 0;

   @ConfigOption(
           name = "Ignore Chests",
           desc = "Do not pause mining to solve treasure chests"
   )
   @ConfigEditorBoolean
   public boolean ignoreChests = false;
}
