package me.grish.veinforge.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.*;
import me.grish.veinforge.config.ConfigActions;

public class Fishing {

    @Category(name = "General Fishing", desc = "Settings shared by all fishing macros")
    public GeneralFishing generalFishing = new GeneralFishing();

    @Category(name = "Galatea Fishing", desc = "Combat and kill-cycle settings for Galatea fishing")
    public GalateaFishing galateaFishing = new GalateaFishing();

    @Category(name = "Galatea Safety", desc = "Safety and movement checks for Galatea fishing")
    public GalateaSafety galateaSafety = new GalateaSafety();

    public String getGraphName() {
        return "Galatea Fishing";
    }

    public static class GeneralFishing {

        @ConfigOption(
                name = "Fishing Rod",
                desc = "Fishing rod item name to hold while fishing"
        )
        @ConfigEditorText
        public String fishingRod = "";

        @ConfigOption(
                name = "Set Fishing Rod",
                desc = "Set the fishing rod name from your currently held item"
        )
        @ConfigEditorButton(buttonText = "Set from hand")
        public transient Runnable setFishingRodButton = ConfigActions::setFishingRod;

        @ConfigOption(
                name = "Cast Delay (ms)",
                desc = "Base delay between cast/reel actions"
        )
        @ConfigEditorSlider(minValue = 150, maxValue = 1500, minStep = 10)
        public int castDelayMs = 450;

        @ConfigOption(
                name = "Cast Delay Randomizer (ms)",
                desc = "Maximum random delay added on top of cast delay"
        )
        @ConfigEditorSlider(minValue = 0, maxValue = 1000, minStep = 10)
        public int castDelayRandomizerMs = 200;

        @ConfigOption(
                name = "Max Wait For Bite (seconds)",
                desc = "Recast if no bite is detected after this duration"
        )
        @ConfigEditorSlider(minValue = 5, maxValue = 120, minStep = 1)
        public int maxWaitSeconds = 30;

        @ConfigOption(
                name = "Use Fishing Graph",
                desc = "Ensure Galatea fishing graph exists and make it active while macro runs"
        )
        @ConfigEditorBoolean
        public boolean useGraph = true;
    }

    public static class GalateaFishing {

        @ConfigOption(
                name = "Galatea Axe",
                desc = "Axe used for melee finish on Galatea mobs"
        )
        @ConfigEditorText
        public String galateaAxe = "";

        @ConfigOption(
                name = "Set Galatea Axe",
                desc = "Set the Galatea axe from your currently held item"
        )
        @ConfigEditorButton(buttonText = "Set from hand")
        public transient Runnable setGalateaAxeButton = ConfigActions::setGalateaAxe;

        @ConfigOption(
                name = "Fishing Weapon",
                desc = "Optional secondary weapon used when Kill Mode is Slayer Weapon"
        )
        @ConfigEditorText
        public String galateaFishingWeapon = "";

        @ConfigOption(
                name = "Set Fishing Weapon",
                desc = "Set the optional secondary weapon from your currently held item"
        )
        @ConfigEditorButton(buttonText = "Set from hand")
        public transient Runnable setGalateaFishingWeaponButton = ConfigActions::setGalateaFishingWeapon;

        @ConfigOption(
                name = "Kill Mode",
                desc = "Melee uses Galatea Axe, Slayer Weapon uses the optional secondary weapon"
        )
        @ConfigEditorDropdown(values = {"Melee (Axe)", "Slayer Weapon"})
        public int galateaKillMode = 0;

        @ConfigOption(
                name = "Fast Weapon Swap",
                desc = "Enable fast switching between fishing weapon and axe"
        )
        @ConfigEditorBoolean
        public boolean galateaFastWeaponSwap = true;

        @ConfigOption(
                name = "Combat Action Delay (ms)",
                desc = "Base delay between Galatea combat actions"
        )
        @ConfigEditorSlider(minValue = 50, maxValue = 1200, minStep = 10)
        public int galateaCombatDelayMs = 220;

        @ConfigOption(
                name = "Combat Delay Randomizer (ms)",
                desc = "Maximum random delay added to combat action timing"
        )
        @ConfigEditorSlider(minValue = 0, maxValue = 600, minStep = 5)
        public int galateaCombatDelayRandomizerMs = 30;

        @ConfigOption(
                name = "Strider Scan Radius",
                desc = "Radius in blocks used to scan nearby Striders"
        )
        @ConfigEditorSlider(minValue = 4, maxValue = 16, minStep = 1)
        public int galateaStriderScanRadius = 10;

        @ConfigOption(
                name = "Strider Min Count",
                desc = "Minimum Strider count needed to trigger kill cycle"
        )
        @ConfigEditorSlider(minValue = 1, maxValue = 30, minStep = 1)
        public int galateaStriderMinCount = 20;

        @ConfigOption(
                name = "Strider Max Count",
                desc = "Maximum Strider count range used by kill cycle trigger"
        )
        @ConfigEditorSlider(minValue = 1, maxValue = 30, minStep = 1)
        public int galateaStriderMaxCount = 30;
    }

    public static class GalateaSafety {

        @ConfigOption(
                name = "Stop On GUI Open",
                desc = "Stop Fishing macro if any non-chat screen opens while running"
        )
        @ConfigEditorBoolean
        public boolean galateaSafetyStopOnGuiOpen = true;

        @ConfigOption(
                name = "Stop On GM Message",
                desc = "Stop Fishing macro when a moderator-like chat pattern is detected"
        )
        @ConfigEditorBoolean
        public boolean galateaSafetyStopOnModeratorMessage = true;

        @ConfigOption(
                name = "Stop On Direct Message",
                desc = "Stop Fishing macro when a direct/private chat message is detected"
        )
        @ConfigEditorBoolean
        public boolean galateaSafetyStopOnDirectMessage = true;

        @ConfigOption(
                name = "Unexpected Move Horizontal",
                desc = "Horizontal movement threshold in blocks before adding a safety strike"
        )
        @ConfigEditorSlider(minValue = 0.5f, maxValue = 6.0f, minStep = 0.05f)
        public float galateaSafetyUnexpectedMoveHorizontal = 1.75f;

        @ConfigOption(
                name = "Unexpected Move Vertical",
                desc = "Vertical movement threshold in blocks before adding a safety strike"
        )
        @ConfigEditorSlider(minValue = 0.3f, maxValue = 4.0f, minStep = 0.05f)
        public float galateaSafetyUnexpectedMoveVertical = 0.85f;

        @ConfigOption(
                name = "Movement Sample Interval (ms)",
                desc = "How often to sample player movement for unexpected drift checks"
        )
        @ConfigEditorSlider(minValue = 100, maxValue = 1000, minStep = 10)
        public int galateaSafetyMovementSampleIntervalMs = 250;

        @ConfigOption(
                name = "Movement Strike Window (ms)",
                desc = "Strike window; strikes older than this are cleared"
        )
        @ConfigEditorSlider(minValue = 1000, maxValue = 15000, minStep = 100)
        public int galateaSafetyMovementStrikeWindowMs = 5000;

        @ConfigOption(
                name = "Movement Strikes To Stop",
                desc = "How many unexpected movement strikes trigger macro stop"
        )
        @ConfigEditorSlider(minValue = 1, maxValue = 5, minStep = 1)
        public int galateaSafetyMovementStrikesToStop = 2;
    }
}
