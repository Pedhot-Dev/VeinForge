package me.grish.veinforge.macro.impl.RouteMiner.states;

import me.grish.veinforge.VeinForge;
import me.grish.veinforge.feature.impl.Pathfinder;
import me.grish.veinforge.handler.RotationHandler;
import me.grish.veinforge.handler.RouteHandler;
import me.grish.veinforge.macro.impl.RouteMiner.RouteMinerMacro;
import me.grish.veinforge.util.*;
import me.grish.veinforge.util.helper.Clock;
import me.grish.veinforge.util.helper.RotationConfiguration;
import me.grish.veinforge.util.helper.route.Route;
import me.grish.veinforge.util.helper.route.RouteWaypoint;
import me.grish.veinforge.util.helper.route.WaypointType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Moving State is responsible for navigating to the next waypoint
 * and handling the logic for the Route Miner Macro.
 * TODO: Use RouteNavigator (temporary fix bc etherwarp doesn't work properly in RouteNavigator?)
 */
public class MovingState implements RouteMinerMacroState {

   private static final Minecraft mc = Minecraft.getInstance();
   private final Clock etherWarpDelay = new Clock();
   private RouteWaypoint routeTarget;
   private boolean hasClicked = false;

   private boolean isWalking = false;

   @Override
   public void onStart(RouteMinerMacro macro) {
      log("Entering Moving State");
      Route route = RouteHandler.getInstance().getSelectedRoute();
      routeTarget = route.get(macro.getRouteIndex() + 1);

      if (routeTarget.getTransportMethod() == WaypointType.ETHERWARP) {
         InventoryUtil.holdItem("Aspect of the Void");
         KeyBindUtil.setKeyBindState(mc.options.keyShift, true);
         List<Vec3> points = BlockUtil.bestPointsOnBestSide(routeTarget.toBlockPos());
         Vec3 point = routeTarget.toVec3d().add(0.5, 0.5, 0.5);

         if (!points.isEmpty()) {
            point = points.get(0);
         }

         RotationHandler.getInstance().easeTo(new RotationConfiguration(
                 AngleUtil.getRotation(point),
                 VeinForge.config().delays.delayAutoAotvEtherwarpLookDelay,
                 null
         ));
      } else {
         KeyBindUtil.setKeyBindState(mc.options.keyShift, false);
      }
   }

   @Override
   public RouteMinerMacroState onTick(RouteMinerMacro macro) {
      switch (routeTarget.getTransportMethod()) {
         case ETHERWARP:
            if (RotationHandler.getInstance().isEnabled()) {
               return this;
            }

            if (!hasClicked) {
               KeyBindUtil.rightClick();
               etherWarpDelay.schedule(250);
               hasClicked = true;
               return this;
            }

            if (etherWarpDelay.passed()) {
               macro.setRouteIndex(macro.getRouteIndex() + 1);
               return new MovingState();
            }

            return this;
         case WALK:
            if (isWalking) {
               if (
                       Pathfinder.getInstance().completedPathTo(routeTarget.toBlockPos()) ||
                               (!Pathfinder.getInstance().isRunning() && Pathfinder.getInstance().succeeded()) ||
                               PlayerUtil.getBlockStandingOn().equals(routeTarget.toBlockPos())
               ) {
                  macro.setRouteIndex(macro.getRouteIndex() + 1);
                  return new MovingState();
               }

               if (Pathfinder.getInstance().failed()) {
                  macro.disable("Pathfinding failed");
                  return null;
               }

               return this;
            }

            Pathfinder.getInstance().queue(routeTarget.toBlockPos());
            Pathfinder.getInstance().start();
            isWalking = true;

            break;
         default:
            return new MiningState();
      }

      return this;
   }

   @Override
   public void onEnd(RouteMinerMacro macro) {
      log("Exiting Moving State");
   }

}
