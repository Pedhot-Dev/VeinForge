package me.grish.veinforge.macro.impl.CommissionMacro.states;

import me.grish.veinforge.VeinForge;
import me.grish.veinforge.feature.impl.RouteNavigator;
import me.grish.veinforge.handler.GameStateHandler;
import me.grish.veinforge.handler.GraphHandler;
import me.grish.veinforge.macro.impl.CommissionMacro.Commission;
import me.grish.veinforge.macro.impl.CommissionMacro.CommissionMacro;
import me.grish.veinforge.util.PlayerUtil;
import me.grish.veinforge.util.helper.location.SubLocation;
import me.grish.veinforge.util.helper.route.Route;
import me.grish.veinforge.util.helper.route.RouteWaypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.List;

public class PathingState implements CommissionMacroState {

   private static final long COMMISSION_EMPTY_MESSAGE_COOLDOWN_MS = 5000; // 5 seconds
   // Cooldown for "Commission is empty" message
   private static long lastCommissionEmptyMessageTime = 0L;
   private final RouteNavigator routeNavigator = RouteNavigator.getInstance();
   private final String GRAPH_NAME = "Commission Macro";
   private int attempts = 0;
   private boolean changeLobbyAttempted = false;
   private boolean warpingToForge = false;
   private boolean startedNavigator = false;
   private long lastGuiWaitLogMs = 0L;
   private Commission targetCommission = null;

   @Override
   public void onStart(CommissionMacro macro) {
      log("Starting pathing state");
      startedNavigator = false;

      if (Minecraft.getInstance().screen != null) {
         long now = System.currentTimeMillis();
         if (now - lastGuiWaitLogMs > 1000L) {
            log("GUI is open, delaying pathing until closed");
            lastGuiWaitLogMs = now;
         }
         return;
      }

      Commission commission = macro.getCurrentCommission();
      targetCommission = commission;

      // When using royal pigeon or refueling using abiphone, no path finding is needed
      if ((commission == Commission.COMMISSION_CLAIM && VeinForge.config().commission.dwarvenCommission.commClaimMethod == 1)
                  || commission == Commission.REFUEL) {
         return;
      }

      if (commission == null) {
         log("Commission is empty!");
         return;
      }

      RouteWaypoint end = commission.getWaypoint();
      BlockPos endPos = end.toBlockPos();
      BlockPos playerPos = PlayerUtil.getBlockStandingOn();
      BlockPos forgePos = new BlockPos(0, 149, -69);
      SubLocation currentSubLocation = GameStateHandler.getInstance().getCurrentSubLocation();
      boolean isInTargetLocation = currentSubLocation == commission.getLocation() ||
                                           (commission == Commission.MITHRIL_MINER && Commission.isMithrilLocation(currentSubLocation));

      double distFromPlayer = Math.sqrt(playerPos.distSqr(endPos));
      double distFromForge = Math.sqrt(forgePos.distSqr(endPos));

      log("Pathing check: subLocation=" + currentSubLocation + ", commissionLocation=" + commission.getLocation()
                  + ", inTarget=" + isInTargetLocation);
      if (!isInTargetLocation && distFromForge + 10 < distFromPlayer && VeinForge.config().commission.dwarvenCommission.forgePathing) {
         log("The distance from the forge is: " + distFromForge + ". The distance from the player is: " + distFromPlayer);
         send("Warping to the forge for faster pathing.");
         warpingToForge = true;
         return;
      }

      List<RouteWaypoint> nodes = GraphHandler.instance.findPathFrom(GRAPH_NAME, PlayerUtil.getBlockStandingOn(), end);

      if (nodes.isEmpty()) {
         logError("Starting block: " + PlayerUtil.getBlockStandingOn() + ", Ending block: " + end);
         macro.disable("Could not find a path to the target block! Please send the logs to the developer.");
         return;
      }

      routeNavigator.start(new Route(nodes));
      startedNavigator = true;
   }

   @Override
   public CommissionMacroState onTick(CommissionMacro macro) {
      if (warpingToForge) {
         warpingToForge = false;
         return new WarpingState();
      }

      if (Minecraft.getInstance().screen != null) {
         return this;
      }

      if (macro.getCurrentCommission() == null) {
         long currentTime = System.currentTimeMillis();
         if (currentTime - lastCommissionEmptyMessageTime > COMMISSION_EMPTY_MESSAGE_COOLDOWN_MS) {
            send("Commission is empty! Waiting for tab-list updates. " +
                         "Note that this is usually temporary and caused by lags in the server.");
            lastCommissionEmptyMessageTime = currentTime;
         }
         return new WarpingState();
      }

      if (macro.getCurrentCommission() == Commission.COMMISSION_CLAIM && VeinForge.config().commission.dwarvenCommission.commClaimMethod == 1) {
         return new ClaimingCommissionState();
      }

      if (startedNavigator && routeNavigator.isRunning() && targetCommission != null && macro.getCurrentCommission() != targetCommission) {
         log("Commission target changed from " + targetCommission.getName() + " to " + macro.getCurrentCommission().getName()
                     + ". Restarting path to avoid stale destination.");
         routeNavigator.stop();
         startedNavigator = false;
      }

      if (!startedNavigator) {
         onStart(macro);
         return this;
      }

      if (routeNavigator.isRunning()) {
         return this;
      }

      if (routeNavigator.succeeded()) {
         log("Pathing succeeded. Deciding next state for commission: " + macro.getCurrentCommission().getName());
         this.attempts = 0;
         this.changeLobbyAttempted = false;

         switch (macro.getCurrentCommission().getName()) {
            case "Mithril":
            case "Titanium":
               return new MiningState();
            case "Glacite Walker Slayer":
            case "Goblin Slayer":
               return new MobKillingState();
            case "Claim":
               return new ClaimingCommissionState();
            default:
               if (macro.getCurrentCommission().getName().contains("Titanium") || macro.getCurrentCommission().getName().contains("Mithril")) {
                  return new MiningState();
               } else if (macro.getCurrentCommission().getName().contains("Claim")) {
                  return new ClaimingCommissionState();
               } else {
                  return new MobKillingState();
               }
         }
      }

      switch (routeNavigator.getNavError()) {
         case NONE:
            if (attempts < 1) {
               attempts++;
               logError("Navigator stopped unexpectedly. Retrying path...");
               onStart(macro);
               return this;
            }
            macro.disable("Route navigator failed, but no error is detected. Please contact the developer.");
            break;
         case TIME_FAIL:
         case PATHFIND_FAILED:
            if (changeLobbyAttempted) {
               macro.disable("Failed to pathfind after multiple attempts. Please check your setup or contact the developer.");
               break;
            }
            attempts++;
            if (attempts >= 3) {
               logError("Failed to pathfind after multiple retries. Warping to a new lobby.");
               this.attempts = 0;
               this.changeLobbyAttempted = true;
               return new NewLobbyState();
            } else {
               logError("Failed to pathfind. Retrying to pathfind. Attempt " + attempts);
               onStart(macro);
               return this;
            }
      }
      return null;
   }

   @Override
   public void onEnd(CommissionMacro macro) {
      routeNavigator.stop();
      log("Ending pathing state");
   }
}
