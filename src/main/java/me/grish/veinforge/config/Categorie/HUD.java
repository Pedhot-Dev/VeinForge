package me.grish.veinforge.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import me.grish.veinforge.config.ConfigActions;
import me.grish.veinforge.config.VeinForgeConfig;

public class HUD {

    @ConfigOption(
            name = "HUD Editor",
            desc = "Open the HUD editor to move/anchor HUD elements"
    )
    @ConfigEditorButton(buttonText = "Open")
    public transient Runnable openHudEditor = ConfigActions::openHudEditor;

    @ConfigOption(name = "Enable Commission HUD", desc = "Show the commission overlay")
    @ConfigEditorBoolean
    public boolean enableCommissionHud = true;
    public VeinForgeConfig.HUDPos commissionHUD = new VeinForgeConfig.HUDPos(5, 5, 0, 1.0f);

    @ConfigOption(name = "Enable Glacial HUD", desc = "Show the glacial commission overlay")
    @ConfigEditorBoolean
    public boolean enableGlacialHud = false;
    public VeinForgeConfig.HUDPos glacialHUD = new VeinForgeConfig.HUDPos(5, 5, 0, 1.0f);

    @ConfigOption(name = "Enable Debug HUD", desc = "Show debug overlay")
    @ConfigEditorBoolean
    public boolean enableDebugHud = false;
    public VeinForgeConfig.HUDPos debugHUD = new VeinForgeConfig.HUDPos(1, 10, 0, 1.0f);

    @ConfigOption(name = "Enable RouteBuilder HUD", desc = "Show RouteBuilder edit overlay")
    @ConfigEditorBoolean
    public boolean enableRouteBuilderHud = true;
    public VeinForgeConfig.HUDPos routeBuilderHUD = new VeinForgeConfig.HUDPos(5, 90, 0, 1.0f);

    @ConfigOption(name = "Enable Pathfinder Stats HUD", desc = "Show latest pathfinding telemetry")
    @ConfigEditorBoolean
    public boolean enablePathfinderStatsHud = false;
    public VeinForgeConfig.HUDPos pathfinderStatsHUD = new VeinForgeConfig.HUDPos(5, 140, 0, 1.0f);

    @ConfigOption(name = "Enable Fishing HUD", desc = "Show Galatea fishing runtime details")
    @ConfigEditorBoolean
    public boolean enableFishingHud = true;
    public VeinForgeConfig.HUDPos fishingHUD = new VeinForgeConfig.HUDPos(5, 185, 0, 1.0f);
}
