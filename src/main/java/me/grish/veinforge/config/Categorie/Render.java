package me.grish.veinforge.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class Render {
    @ConfigOption(name = "Spin", desc = "Spins the player client side")
    @ConfigEditorBoolean
    public boolean spin = false;

    @ConfigOption(name = "Spin Speed", desc = "")
    @ConfigEditorSlider(minValue = 0, maxValue = 20, minStep = 1)
    public int spinSpeed = 10;

    @ConfigOption(name = "Spin Mode", desc = "")
    @ConfigEditorDropdown(values = {
            "Left", "Right", "Pitch", "Seizure",
            "Dinnerbone", "Wobble", "Orbit", "Breathing"
    })
    public int spinMode = 0;
}
