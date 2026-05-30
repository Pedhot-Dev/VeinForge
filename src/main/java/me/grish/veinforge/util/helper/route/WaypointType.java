package me.grish.veinforge.util.helper.route;

public enum WaypointType {
   AOTV,           // Right Click Aotv/Aote
   ETHERWARP,      // Shift + Right Click Aotv/Aote
   WALK,           // Walks Between Nodes (Add it after pathfinder)
   MINE            // User starts mining at this node (needed for RouteMiner)
}
