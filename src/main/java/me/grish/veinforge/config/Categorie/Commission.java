package me.grish.veinforge.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.*;
import me.grish.veinforge.config.ConfigActions;

public class Commission {

    @Category(name = "Dwarven Commission", desc = "Settings for regular Dwarven commissions")
    public DwarvenCommission dwarvenCommission = new DwarvenCommission();

    @Category(name = "Glacial Commission", desc = "Settings for Glacial commissions")
    public GlacialCommission glacialCommission = new GlacialCommission();

    public static class DwarvenCommission {

        @ConfigOption(name = "Claim Method", desc = "")
        @ConfigEditorDropdown(values = {"NPC", "Royal Pigeon"})
        public int commClaimMethod = 0;

        @ConfigOption(
                name = "Prioritise Titanium",
                desc = "Always mine Titanium, even if it is not the commission"
        )
        @ConfigEditorBoolean
        public boolean prioritiseTitanium = false;

        @ConfigOption(name = "Alt. Mining Tool", desc = "")
        @ConfigEditorText
        public String altMiningTool = "";

        @ConfigOption(
                name = "Set Alt Mining Tool",
                desc = "Set the alt tool name from your currently held item"
        )
        @ConfigEditorButton(buttonText = "Set from hand")
        public transient Runnable setAltMiningToolButton = ConfigActions::setAltMiningTool;

        @ConfigOption(
                name = "Swap before claiming commission",
                desc = "Swaps to the alternative mining tool before claiming the commission"
        )
        @ConfigEditorBoolean
        public boolean commSwapBeforeClaiming = false;

        @ConfigOption(
                name = "Slayer Weapon",
                desc = "Weapon used when killing goblins"
        )
        @ConfigEditorText
        public String slayerWeapon = "";

        @ConfigOption(
                name = "Set Slayer Weapon",
                desc = "Set the slayer weapon name from your currently held item"
        )
        @ConfigEditorButton(buttonText = "Set from hand")
        public transient Runnable setSlayerWeaponButton = ConfigActions::setSlayerWeapon;

        @ConfigOption(
                name = "Sprint During MobKiller",
                desc = "Allow Sprinting while Mob Killer is active (looks sussy)"
        )
        @ConfigEditorBoolean
        public boolean mobKillerSprint = true;

        @ConfigOption(
                name = "Interpolate During MobKiller",
                desc = "Allows more natural movement"
        )
        @ConfigEditorBoolean
        public boolean mobKillerInterpolate = true;

        @ConfigOption(
                name = "Warp to forge during pathing",
                desc = "If next commission is closer from the forge, it will warp and path from there"
        )
        @ConfigEditorBoolean
        public boolean forgePathing = true;

        @ConfigOption(
                name = "Show Commission HUD outside mines",
                desc = "Toggle HUD Visibility outside of dwarven mines"
        )
        @ConfigEditorBoolean
        public boolean showDwarvenCommHUDOutside = true;
    }

    public static class GlacialCommission {

        @ConfigOption(
                name = "Warning",
                desc = "You MUST use Royal Pigeon for Glacial Commissions!"
        )
        @ConfigEditorInfoText
        public String warningInfo = "";

        @ConfigOption(
                name = "Cold Threshold Info",
                desc = "Set the threshold of coldness to where the macro will warp back to the base"
        )
        @ConfigEditorInfoText
        public String coldThresholdInfo = "";

        @ConfigOption(
                name = "Cold Threshold",
                desc = "The threshold of coldness to where the macro will warp back to the base"
        )
        @ConfigEditorSlider(minValue = 1, maxValue = 100, minStep = 1)
        public int coldThreshold = 50;

        @ConfigOption(name = "Reset Stats When Disabled", desc = "")
        @ConfigEditorBoolean
        public boolean glacialHudResetStats = false;

        @ConfigOption(
                name = "Show Glacial HUD Outside Glacial Mines",
                desc = ""
        )
        @ConfigEditorBoolean
        public boolean showGlacialHUDOutside = true;
    }
}
