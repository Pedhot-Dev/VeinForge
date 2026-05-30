package me.grish.veinforge.config.Categorie;

import io.github.notenoughupdates.moulconfig.annotations.*;
import org.lwjgl.glfw.GLFW;

public class RouteMiner {

   @ConfigOption(name = "Enabled", desc = "Enable Route Miner")
   @ConfigEditorBoolean
   public boolean enabled = false;

   @ConfigOption(
           name = "Report Bugs",
           desc = "Make sure to report bugs in #bug-reports in the discord server."
   )
   @ConfigEditorInfoText
   public String ignored1 = "";

   @ConfigOption(
           name = "Route Building Info",
           desc = "Run /rb for more information on route building."
   )
   @ConfigEditorInfoText
   public String ignored2 = "";

   @ConfigOption(name = "Selected Route", desc = "")
   @ConfigEditorText
   public String selectedRoute = "";

   @ConfigOption(name = "Mine Gemstone", desc = "")
   @ConfigEditorBoolean
   public boolean routeMineGemstone = false;

   @ConfigOption(name = "Mine Topaz", desc = "")
   @ConfigEditorBoolean
   public boolean routeMineTopaz = false;

   @ConfigOption(name = "Mine Ore", desc = "")
   @ConfigEditorBoolean
   public boolean routeMineOre = false;

   @ConfigOption(name = "Mine Glacite", desc = "")
   @ConfigEditorBoolean
   public boolean routeMineGlacite = false;

   @ConfigOption(name = "Mine Umber", desc = "")
   @ConfigEditorBoolean
   public boolean routeMineUmber = false;

   @ConfigOption(name = "Mine Tungsten", desc = "")
   @ConfigEditorBoolean
   public boolean routeMineTungsten = false;

   @ConfigOption(
           name = "Enable RouteBuilder",
           desc = "Key used to toggle RouteBuilder (waypoint route editing mode)."
   )
   @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_LEFT_BRACKET)
   public int routeBuilder = GLFW.GLFW_KEY_LEFT_BRACKET;

   @ConfigOption(
           name = "Add Block To Route (Walk)",
           desc = "Adds your current block as a WALK waypoint to the selected route."
   )
   @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_P)
   public int routeBuilderWalkAddKeybind = GLFW.GLFW_KEY_P;

   @ConfigOption(
           name = "Add Block To Route (Etherwarp)",
           desc = "Adds your current block as an ETHERWARP waypoint to the selected route."
   )
   @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_I)
   public int routeBuilderEtherwarpAddKeybind = GLFW.GLFW_KEY_I;

   @ConfigOption(
           name = "Remove Block From Route",
           desc = "Removes the closest waypoint in the selected route."
   )
   @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_O)
   public int routeBuilderRemoveKeybind = GLFW.GLFW_KEY_O;

   @ConfigOption(
           name = "Route Node Color",
           desc = "The Color of The Blocks On a Route"
   )
   @ConfigEditorColour
   public String routeBuilderNodeColor = "0:100:0:255:255";

   @ConfigOption(
           name = "Route Tracer Color",
           desc = "The Color of The Line Between Blocks On a Route"
   )
   @ConfigEditorColour
   public String routeBuilderTracerColor = "0:100:0:255:255";

   @ConfigOption(
           name = "Graph Editor: Add Unidirectional",
           desc = "Adds a one-way edge from selected parent to your current block."
   )
   @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_KP_7)
   public int routeBuilderUnidi = GLFW.GLFW_KEY_KP_7;

   @ConfigOption(
           name = "Graph Editor: Place Walk Node",
           desc = "Places/selects a WALK node at your current block."
   )
   @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_KP_1)
   public int routeBuilderGraphWalkNode = GLFW.GLFW_KEY_KP_1;

   @ConfigOption(
           name = "Graph Editor: Place Etherwarp Node",
           desc = "Places/selects an ETHERWARP node at your current block."
   )
   @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_KP_2)
   public int routeBuilderGraphEtherwarpNode = GLFW.GLFW_KEY_KP_2;

   @ConfigOption(
           name = "Graph Editor: Add Bidirectional",
           desc = "Adds a two-way edge between selected parent and your current block."
   )
   @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_KP_8)
   public int routeBuilderBidi = GLFW.GLFW_KEY_KP_8;

   @ConfigOption(
           name = "Graph Editor: Select Parent",
           desc = "Selects the current block as the parent node for edge creation."
   )
   @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_KP_4)
   public int routeBuilderSelect = GLFW.GLFW_KEY_KP_4;

   @ConfigOption(
           name = "Graph Editor: Move Selected",
           desc = "Moves the selected node to your current block, preserving connectivity."
   )
   @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_KP_5)
   public int routeBuilderMove = GLFW.GLFW_KEY_KP_5;

   @ConfigOption(
           name = "Graph Editor: Delete Selected",
           desc = "Deletes the selected node and all of its edges."
   )
   @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_KP_6)
   public int routeBuilderDelete = GLFW.GLFW_KEY_KP_6;
}
