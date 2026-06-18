package me.grish.veinforge.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class Delays {

    @ConfigOption(
            name = "Rotation Time",
            desc = "Time it takes to rotate to the next block while mining mithril"
    )
    @ConfigEditorSlider(minValue = 50, maxValue = 1000, minStep = 10)
    public int rotationTime = 300;

    @ConfigOption(name = "Rotation Time Randomization", desc = "")
    @ConfigEditorSlider(minValue = 50, maxValue = 1000, minStep = 10)
    public int rotationTimeRandomizer = 300;

    @ConfigOption(name = "Tick Glide Offset", desc = "")
    @ConfigEditorSlider(minValue = 0, maxValue = 10, minStep = 1)
    public int tickGlideOffset = 4;

    @ConfigOption(name = "Sneak Time", desc = "")
    @ConfigEditorSlider(minValue = 0, maxValue = 2000, minStep = 50)
    public int sneakTime = 500;

    @ConfigOption(name = "Sneak Time Randomizer", desc = "")
    @ConfigEditorSlider(minValue = 0, maxValue = 2000, minStep = 50)
    public int sneakTimeRandomizer = 300;

    @ConfigOption(name = "GUI Delay", desc = "Time to wait in a gui")
    @ConfigEditorSlider(minValue = 50, maxValue = 2000, minStep = 50)
    public int delaysGuiDelay = 450;

    @ConfigOption(
            name = "GUI Delay Randomizer",
            desc = "Maximum random time to add to GUI Delay Time"
    )
    @ConfigEditorSlider(minValue = 50, maxValue = 1000, minStep = 50)
    public int delaysGuiDelayRandomizer = 250;

    @ConfigOption(
            name = "Aotv Look Delay (Right Click)",
            desc = "Rotation time to look at next block while aotving"
    )
    @ConfigEditorSlider(minValue = 50, maxValue = 1000, minStep = 10)
    public int delayAutoAotvLookDelay = 250;

    @ConfigOption(
            name = "Aotv Look Delay (Etherwarp)",
            desc = "Rotation time to look at next block while Etherwarping"
    )
    @ConfigEditorSlider(minValue = 50, maxValue = 2000, minStep = 10)
    public int delayAutoAotvEtherwarpLookDelay = 500;

    @ConfigOption(
            name = "Server Side Rotation Time",
            desc = "Rotation time to look at next block with client side rotation"
    )
    @ConfigEditorSlider(minValue = 0, maxValue = 2000, minStep = 10)
    public int delayAutoAotvServerRotation = 500;
}
