package me.grish.veinforge.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class Failsafe {

    @ConfigOption(
            name = "Enable Failsafe Trigger Sound",
            desc = "Makes a sound when a failsafe has been triggered"
    )
    @ConfigEditorBoolean
    public boolean enableFailsafeSound = true;

    @ConfigOption(
            name = "Time to wait before toggling failsafe (in ms)",
            desc = ""
    )
    @ConfigEditorSlider(minValue = 0, maxValue = 15000, minStep = 100)
    public int failsafeToggleDelay = 3000;

    @ConfigOption(name = "Vertical Knockback Threshold", desc = "")
    @ConfigEditorSlider(minValue = 3000, maxValue = 10000, minStep = 100)
    public int verticalKnockbackThreshold = 4000;

    @ConfigOption(
            name = "Name Mention Failsafe Behaviour",
            desc = "The action Name Mention Failsafe will take when your name is mentioned in chat"
    )
    @ConfigEditorDropdown(values = {"Pause Macro", "Change Lobby"})
    public int nameMentionFailsafeBehaviour = 0;

    @ConfigOption(
            name = "Failsafe Sound Type",
            desc = "The failsafe sound type to play when a failsafe has been triggered"
    )
    @ConfigEditorDropdown(values = {"Minecraft", "Custom"})
    public int failsafeSoundType = 0;

    @ConfigOption(
            name = "Minecraft Sound",
            desc = "The Minecraft sound to play when a failsafe has been triggered"
    )
    @ConfigEditorDropdown(values = {"Ping", "Anvil"})
    public int failsafeMcSoundSelected = 1;

    @ConfigOption(
            name = "Custom Sound",
            desc = "The custom sound to play when a failsafe has been triggered"
    )
    @ConfigEditorDropdown(
            values = {
                    "Custom", "Voice", "Metal Pipe", "AAAAAAAAAA", "Loud Buzz",
            }
    )
    public int failsafeSoundSelected = 1;

    @ConfigOption(
            name = "Number of times to play custom sound",
            desc = "The number of times to play custom sound when a failsafe has been triggered"
    )
    @ConfigEditorSlider(minValue = 1, maxValue = 10, minStep = 1)
    public int failsafeSoundTimes = 10;

    @ConfigOption(
            name = "Failsafe Sound Volume (in %)",
            desc = "The volume of the failsafe sound"
    )
    @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 1f)
    public float failsafeSoundVolume = 50.0f;

    @ConfigOption(
            name = "Max out Minecraft sounds while pinging",
            desc = "Maxes out the sounds while failsafe"
    )
    @ConfigEditorBoolean
    public boolean maxOutMinecraftSounds = false;
}
