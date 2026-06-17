package me.grish.veinforge.command;

import me.grish.veinforge.VeinForge;
import me.grish.veinforge.VeinForgeClient;
import me.grish.veinforge.feature.impl.RouteBuilder;
import me.grish.veinforge.handler.GraphHandler;
import me.grish.veinforge.handler.RouteHandler;
import me.grish.veinforge.util.KeyPressUtil;
import me.grish.veinforge.util.Logger;
import me.grish.veinforge.util.helper.route.WaypointType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Objects;

public class RouteBuilderCommand {

   public void main() {
      Logger.sendMessage("Use these commands to manage route waypoints.");
      info("   1. /rb list -> List all available routes.");
      info("   2. /rb new <route-name> -> Create and select a new route.");
      info("   3. /rb select <route-name> -> Select the specified route name. A new route will be created if none exist.");
      info("   4. /rb add <walk|etherwarp|mine> -> Add the block player is standing on to selected route.");
      info("   5. /rb remove <index> -> Remove the block player is standing on from selected route.");
      info("   6. /rb replace <index> <walk|etherwarp|mine> -> Replaces Specified Index from the route with block player is standing on.");
      info("   7. /rb delete <route-name> -> Deletes the route.");
      info("   8. /rb keys -> Show RouteBuilder keybinds and actions.");
      info("   9. /rb toggle -> Toggle RouteBuilder on/off.");
      info("   Note: Route Miner requires at least 1 waypoint. 'Default' is read-only.");
      keys();
   }

   public void keys() {
      var config = VeinForge.config();
      if (config == null) {
         Logger.sendError("Config is not loaded yet.");
         return;
      }

      Logger.sendMessage("RouteBuilder keybinds:");
      info("   Toggle RouteBuilder: " + KeyPressUtil.getKeyName(config.routeMiner.routeBuilder));
      info("   Add WALK waypoint: " + KeyPressUtil.getKeyName(config.routeMiner.routeBuilderWalkAddKeybind));
      info("   Add ETHERWARP waypoint: " + KeyPressUtil.getKeyName(config.routeMiner.routeBuilderEtherwarpAddKeybind));
      info("   Remove closest waypoint: " + KeyPressUtil.getKeyName(config.routeMiner.routeBuilderRemoveKeybind));
      Logger.sendMessage("Graph Editor keybinds (for graph links):");
      for (String hint : GraphHandler.instance.getEditorControlHelpLines()) {
         info("   " + hint);
      }
   }

   public void list() {
      StringBuilder sb = new StringBuilder();
      sb.append("Available Routes: ");

      RouteHandler.getInstance().getRoutes().forEach((key, val) -> {
         String str = key;
         if (Objects.equals(RouteHandler.getInstance().getSelectedRoute(), val)) str += "*";
         sb.append(str).append(", ");
      });

      Logger.sendMessage(sb.toString());
   }

   public void reload() {
      RouteHandler.getInstance().loadData();
      Logger.sendMessage("Refreshed routes file.");
   }

   public void select(final String routeName) {
      String normalized = normalizeRouteName(routeName);
      if (normalized.isEmpty()) {
         Logger.sendError("Route name cannot be empty.");
         return;
      }

      VeinForge.config().routeMiner.selectedRoute = normalized;
      RouteHandler.getInstance().selectRoute(normalized);
      VeinForgeClient.configManager.saveConfig();
      int waypointCount = RouteHandler.getInstance().getRouteSize(normalized);
      Logger.sendMessage("Selected route: " + normalized + " (" + waypointCount + " waypoint" + (waypointCount == 1 ? "" : "s") + ")");
      if (waypointCount == 0) {
         Logger.sendWarning("Selected route has no waypoints. Add one with P/I or /rb add <walk|etherwarp|mine>.");
      }
   }

   public void create(final String routeName) {
      String normalized = normalizeRouteName(routeName);
      if (normalized.isEmpty()) {
         Logger.sendError("Route name cannot be empty.");
         return;
      }

      boolean created = RouteHandler.getInstance().createRoute(normalized);
      this.select(normalized);
      if (created) {
         Logger.sendMessage("Created route: " + normalized);
      } else {
         Logger.sendWarning("Route already exists. Selected existing route: " + normalized);
      }
   }

   public void toggle() {
      RouteBuilder.getInstance().toggle();
   }

   public void add(final String name) {
      if (isRouteBuilderNotRunning()) return;
      WaypointType type = WaypointType.ETHERWARP;

      if (name.equalsIgnoreCase("walk")) {
         type = WaypointType.WALK;
      } else if (name.equalsIgnoreCase("mine")) {
         type = WaypointType.MINE;
      } else if (!name.equalsIgnoreCase("etherwarp")) {
         Logger.sendError("You must specify a proper option. Run /rb for more information.");
         return;
      }

      if (RouteBuilder.getInstance().addToRoute(type)) {
         Logger.sendMessage("Added " + type.name().charAt(0) + type.name().substring(1).toLowerCase());
      }
   }

   public void remove(int index) {
      if (isRouteBuilderNotRunning()) return;
      RouteBuilder.getInstance().removeFromRoute(index - 1);
      Logger.sendMessage("Removed point at index: " + index);
   }

   public void delete(final String routeName) {
      if (isRouteBuilderNotRunning()) return;
      String normalized = normalizeRouteName(routeName);
      if (normalized.isEmpty()) {
         Logger.sendError("Route name cannot be empty.");
         return;
      }
      RouteHandler.getInstance().deleteRoute(normalized);
      Logger.sendMessage("Deleted Route: " + normalized);
   }

   public void replace(final int indexToReplace, final String name) {
      if (isRouteBuilderNotRunning()) return;
      if (indexToReplace <= 0) return;
      WaypointType type = WaypointType.ETHERWARP;

      if (name.equalsIgnoreCase("walk")) {
         type = WaypointType.WALK;
      } else if (name.equalsIgnoreCase("mine")) {
         type = WaypointType.MINE;
      } else if (!name.equalsIgnoreCase("etherwarp")) {
         Logger.sendError("You must specify a proper option. Run /rb for more information.");
         return;
      }

      RouteBuilder.getInstance().replaceNode(indexToReplace - 1);
      Logger.sendMessage("Replaced index " + indexToReplace + " with " + type.name().charAt(0) + type.name().substring(1).toLowerCase());
   }

   private boolean isRouteBuilderNotRunning() {
      if (!RouteBuilder.getInstance().isRunning()) {
         var config = VeinForge.config();
         String keybind = config == null
                                  ? "the configured keybind"
                                  : KeyPressUtil.getKeyName(config.routeMiner.routeBuilder);
         Logger.sendError("Route Builder is not enabled! Enable it by pressing " + keybind + ".");
         return true;
      }

      return false;
   }

   private void info(final String message) {
      if (Minecraft.getInstance().player != null) {
         Minecraft.getInstance().player.sendSystemMessage(Component.literal("§e" + message));
      }
   }

   private static String normalizeRouteName(String routeName) {
      if (routeName == null) {
         return "";
      }
      return routeName.trim().replaceAll("\\s+", " ");
   }

}
