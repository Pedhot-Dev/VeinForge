package me.grish.veinforge.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.*;
import org.lwjgl.glfw.GLFW;

public class Debug {

   @ConfigOption(name = "Debug Route Color", desc = "")
   @ConfigEditorColour
   public String debugRouteColor = "0:180:0:255:0";

   @ConfigOption(name = "Commission Debug Keybind", desc = "")
   @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
   public int commissionDebugKeybind = GLFW.GLFW_KEY_UNKNOWN;

   @ConfigOption(name = "Debug Mode", desc = "Enable debug mode")
   @ConfigEditorBoolean
   public boolean debugMode = false;

   @ConfigOption(name = "Mining Coefficient", desc = "")
   @ConfigEditorSlider(minValue = 0, maxValue = 1000, minStep = 10)
   public int miningCoefficient = 200;

   @ConfigOption(name = "Angle Coefficient", desc = "")
   @ConfigEditorSlider(minValue = 0, maxValue = 10, minStep = 1)
   public int angleCoefficient = 1;

   @ConfigOption(name = "Distance Coefficient", desc = "")
   @ConfigEditorSlider(minValue = 0, maxValue = 20, minStep = 1)
   public int distanceCoefficient = 10;

   @ConfigOption(name = "MobKiller Distance Cost", desc = "")
   @ConfigEditorSlider(minValue = 0, maxValue = 1000, minStep = 10)
   public int mobKillerDistCost = 100;

   @ConfigOption(name = "MobKiller Rotation Cost", desc = "")
   @ConfigEditorSlider(minValue = 0, maxValue = 1000, minStep = 10)
   public int mobKillerRotCost = 5;

   @ConfigOption(name = "Rotation Curve", desc = "")
   @ConfigEditorSlider(minValue = 0, maxValue = 5, minStep = 1)
   public int rotationCurve = 1;

   @ConfigOption(name = "Use Fixed Rotation Time", desc = "")
   @ConfigEditorBoolean
   public boolean useFixedRotation = false;

   @ConfigOption(name = "Fixed Rotation Time", desc = "")
   @ConfigEditorSlider(minValue = 0, maxValue = 2000, minStep = 50)
   public int fixedRotationTime = 500;

   @ConfigOption(name = "Rotation Multiplier", desc = "")
   @ConfigEditorSlider(minValue = 0f, maxValue = 10f, minStep = 0.1f)
   public float rotationMultiplier = 2f;
}
