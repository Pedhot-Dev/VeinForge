package me.grish.veinforge.config.Categorie;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class MainCategory {
    @Expose
    @ConfigOption(name = "Enabled", desc = "tesr")
    @ConfigEditorBoolean
    public boolean test = true;
}
