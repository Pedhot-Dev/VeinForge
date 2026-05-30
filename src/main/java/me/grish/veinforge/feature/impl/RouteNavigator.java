package me.grish.veinforge.feature.impl;

import lombok.Getter;
import lombok.Setter;
import me.grish.veinforge.VeinForge;
import me.grish.veinforge.feature.AbstractFeature;
import me.grish.veinforge.handler.RotationHandler;
import me.grish.veinforge.macro.MacroManager;
import me.grish.veinforge.macro.impl.GlacialMacro.GlacialMacro;
import me.grish.veinforge.util.AngleUtil;
import me.grish.veinforge.util.BlockUtil;
import me.grish.veinforge.util.KeyBindUtil;
import me.grish.veinforge.util.PlayerUtil;
import me.grish.veinforge.util.helper.RotationConfiguration;
import me.grish.veinforge.util.helper.RotationConfiguration.RotationType;
import me.grish.veinforge.util.helper.Target;
import me.grish.veinforge.util.helper.route.Route;
import me.grish.veinforge.util.helper.route.RouteWaypoint;
import me.grish.veinforge.util.helper.route.WaypointType;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

// This works under the assumption that the blocks between every node are clear and traversable.
// Checks to determine if the player can move between two nodes must be done beforehand; otherwise, it will cause bugs.

public class RouteNavigator extends AbstractFeature {

   private static RouteNavigator instance;
   private Route routeToFollow;
   private int currentRouteIndex = -1;
   private int targetRouteIndex = -1;
   private State state = State.STARTING;
   private boolean isQueued = false;
   @Getter
   private NavError navError = NavError.NONE;
   @Setter
   private RotationType rotationType = RotationType.CLIENT;
   private Vec3 rotationTarget = null;

   public static RouteNavigator getInstance() {
      if (instance == null) {
         instance = new RouteNavigator();
      }
      return instance;
   }

   @Override
   public String getName() {
      return "RouteNavigator";
   }

   @Override
   public void resetStatesAfterStop() {
      this.state = State.STARTING;
      this.rotationType = RotationType.CLIENT;
      KeyBindUtil.releaseAllExcept();
      RotationHandler.getInstance().stop();
   }

   @Override
   public boolean shouldNotCheckForFailsafe() {
      return this.state == State.AOTV_VERIFY;
   }

   public void queueRoute(final Route routeToFollow) {
      if (this.enabled) {
         return;
      }
      this.routeToFollow = routeToFollow;
      this.currentRouteIndex = -1;
      this.targetRouteIndex = -1;
      this.isQueued = true;
   }

   public void goTo(final int index) {
      if (this.routeToFollow == null || this.routeToFollow.isEmpty()) {
         sendError("No Route Was Selected or its empty.");
         return;
      }
      this.targetRouteIndex = index;
      this.currentRouteIndex = this.getCurrentIndex(PlayerUtil.getBlockStandingOn()) - 1;
      this.normalizeIndices();
      this.navError = NavError.NONE;
      this.enabled = true;
      this.start();
   }

   public void gotoNext() {
      this.goTo(this.targetRouteIndex + 1);
   }

   public void start(final Route routeToFollow) {
      this.routeToFollow = routeToFollow;
      this.enabled = true;
      this.targetRouteIndex = -1;
      this.normalizeIndices();
      this.currentRouteIndex = -1;
      this.navError = NavError.NONE;

      this.start();
      send("Enabling RouteNavigator.");
   }

   public void pause() {
      this.enabled = false;
      this.timer.reset();
      this.resetStatesAfterStop();

      send("Pausing RouteNavigator");
   }

   @Override
   public void stop() {
      if (!this.enabled) {
         return;
      }

      this.enabled = false;
      this.isQueued = false;
      this.routeToFollow = null;
      this.timer.reset();
      this.targetRouteIndex = -1;
      this.currentRouteIndex = -1;
      this.rotationTarget = null;
      this.resetStatesAfterStop();

      send("RouteNavigator Stopped");
   }

   public void stop(NavError error) {
      this.navError = error;
      this.stop();
   }

   private void normalizeIndices() {
      this.targetRouteIndex = this.normalizeIndex(this.targetRouteIndex);
      this.currentRouteIndex = this.normalizeIndex(this.currentRouteIndex);
      if (this.targetRouteIndex < this.currentRouteIndex) {
         this.targetRouteIndex += this.routeToFollow.size();
      }
   }

   private int normalizeIndex(final int index) {
      return (index + this.routeToFollow.size()) % this.routeToFollow.size();
   }

   private int getLookTime(final RouteWaypoint waypoint) {
      if (this.rotationType == RotationType.SERVER) {
         return VeinForge.config().delays.delayAutoAotvServerRotation;
      }
      if (waypoint.getTransportMethod().ordinal() == 0) {
         return VeinForge.config().delays.delayAutoAotvLookDelay;
      }
      return VeinForge.config().delays.delayAutoAotvEtherwarpLookDelay;
   }

   public int getCurrentIndex(final BlockPos playerBlock) {
      int index = this.routeToFollow.indexOf(new RouteWaypoint(playerBlock, WaypointType.ETHERWARP));
      if (index != -1) {
         return index;
      }
      return this.routeToFollow.getClosest(playerBlock).map(routeWaypoint -> this.routeToFollow.indexOf(routeWaypoint)).orElse(-1);
   }

   public boolean succeeded() {
      return !this.enabled && this.navError == NavError.NONE;
   }

   @Override
   protected void onTick() {
      if (!this.enabled) {
         return;
      }

      if (mc.player == null || mc.level == null) {
         log("Player or World is null, stopping RouteNavigator.");
         this.stop(NavError.NONE);
         return;
      }

      switch (this.state) {
         case STARTING: {
            this.swapState(State.DETECT_ROUTE, 0);
            break;
         }
         case DETECT_ROUTE: {
            if (this.currentRouteIndex++ == this.targetRouteIndex) {
               this.swapState(State.END_VERIFY, 0);
               return;
            }
            if (this.routeToFollow.get(this.currentRouteIndex).getTransportMethod() == WaypointType.WALK) {
               this.swapState(State.WALK, 0);
            } else {
               this.swapState(State.ROTATION, 0);
            }

            log("Going To Index: " + this.currentRouteIndex);
            break;
         }
         case ROTATION: {
            RouteWaypoint nextPoint = this.routeToFollow.get(this.currentRouteIndex);

            if (nextPoint.getTransportMethod() == WaypointType.ETHERWARP) {
               BlockPos block = nextPoint.toBlockPos();

               if (mc.level.getBlockState(block).getBlock() == Blocks.SNOW) {
                  block = block.below();
               }

               this.rotationTarget = BlockUtil.getClosestVisibleSidePos(block);
            } else {
               this.rotationTarget = BlockUtil.getClosestVisibleSidePos(nextPoint.toBlockPos());
            }

            RotationConfiguration config = new RotationConfiguration(new Target(this.rotationTarget),
                    this.getLookTime(nextPoint),
                    this.rotationType,
                    null)
                                                   .followTarget(true);

            RotationHandler.getInstance().easeTo(config);
            this.swapState(State.ROTATION_VERIFY, 2000);
            log("Rotating to " + this.rotationTarget);
            break;
         }
         case ROTATION_VERIFY: {
            if (this.timer.isScheduled() && this.timer.passed()) {
               sendError("Could not look in time. Disabling.");
               this.stop(NavError.TIME_FAIL);
               return;
            }

            if (!AngleUtil.isLookingAt(this.rotationTarget, 0.5f) && !RotationHandler.getInstance().isFollowingTarget()) {
               return;
            }

            System.out.println("IsLookingAt: " + AngleUtil.isLookingAtDebug(this.rotationTarget, 0.5f));
            System.out.println("Following: " + RotationHandler.getInstance().isFollowingTarget());
            int sneakTime = 0;

            RouteWaypoint target = this.routeToFollow.get(this.currentRouteIndex);
            if (target.getTransportMethod() == WaypointType.ETHERWARP && !mc.options.keyShift.isDown()) {
               KeyBindUtil.setKeyBindState(mc.options.keyShift, true);
               sneakTime = 250;
            }

            this.swapState(State.AOTV, sneakTime);
            break;
         }
         case AOTV: {
            if (this.timer.isScheduled() && !this.timer.passed()) {
               return;
            }
            // Todo: test Etherwarp
            if (this.routeToFollow.get(this.currentRouteIndex + 1).getTransportMethod() != WaypointType.ETHERWARP) {
               KeyBindUtil.setKeyBindState(mc.options.keyShift, false);
            }
            KeyBindUtil.rightClick();
            System.out.println("clicked");
            this.swapState(State.AOTV_VERIFY, 2000);
            break;
         }
         case AOTV_VERIFY: {
            if (this.timer.isScheduled() && this.timer.passed()) {
               // Check if the current macro is the GlacialMacro to see if we can walk to the target instead
               if (MacroManager.getInstance().getCurrentMacro() instanceof GlacialMacro) {
                  sendError("Did not receive teleport packet in time. Attempting to walk to the target instead.");
                  this.swapState(State.WALK, 0);
               } else {
                  // Keep the original behavior for all other macros.
                  sendError("Did not receive teleport packet in time. Disabling");
                  this.stop(NavError.TIME_FAIL);
               }
               return;
            }
            break;
         }
         case WALK:
            BlockPos source = this.routeToFollow.get(this.currentRouteIndex).toBlockPos();
            if (this.currentRouteIndex == 0) {
               log("queued first");
               BlockPos p = PlayerUtil.getBlockStandingOn();
               Pathfinder.getInstance().queue(p, source);
            }
            if (this.currentRouteIndex + 1 <= this.targetRouteIndex) {
               log("queued next");
               RouteWaypoint target = this.routeToFollow.get(this.currentRouteIndex + 1);
               Pathfinder.getInstance().queue(source, target.toBlockPos());
            }
            if (!Pathfinder.getInstance().isRunning()) {
               log("Started");
               Pathfinder.getInstance().setInterpolationState(true);
               Pathfinder.getInstance().start();
            }
            this.swapState(State.WALK_VERIFY, 2000);
            log("Walking");
            break;
         case WALK_VERIFY:
            BlockPos targetPos = this.routeToFollow.get(this.currentRouteIndex).toBlockPos();
            if (Pathfinder.getInstance().completedPathTo(targetPos) || (!Pathfinder.getInstance().isRunning() && Pathfinder.getInstance().succeeded()) || PlayerUtil.getBlockStandingOn().equals(targetPos)) {
               log("Completed path. going to next");
               this.swapState(State.STARTING, 0);
               log("Done Walking");
               return;
            }

            if (Pathfinder.getInstance().failed()) {
               sendError("Pathfinding failed");
               this.stop(NavError.PATHFIND_FAILED);
               return;
            }

            // Check for timeout - if pathfinding is taking too long, increase the timer
            if (this.timer.isScheduled() && this.timer.passed()) {
               if (Pathfinder.getInstance().isRunning()) {
                  log("Pathfinding still in progress, extending timer");
                  this.timer.schedule(5000); // Extend by 5 more seconds
               } else {
                  logError("Pathfinding timeout - pathfinder stopped without completion");
                  this.stop(NavError.TIME_FAIL);
                  return;
               }
            }

            break;
         case END_VERIFY: {
            // Todo: add something to verify if player is at the end or not preferably something that checks for distance from end

            if (this.isQueued) {
               this.pause();
            } else {
               this.stop();
            }
            break;
         }
      }
   }

   @Override
   protected void onPacketReceive(Packet<?> packet) {
      if (!this.enabled) {
         return;
      }
      if (this.state != State.AOTV_VERIFY) {
         return;
      }
      if (!(packet instanceof ClientboundPlayerPositionPacket positionPacket)) {
         return;
      }

      this.swapState(State.STARTING, 0);
      RotationHandler.getInstance().stop();

      if (mc.player == null) {
         return;
      }

      // PlayerPositionLookS2CPacket can encode position as relative deltas.
      // If a relative flag is present, add the player's current position component.
      Vec3 currentPlayerPos = mc.player.position();
      Vec3 pos = new Vec3(
              positionPacket.change().position().x + (positionPacket.relatives().contains(Relative.X) ? currentPlayerPos.x : 0),
              positionPacket.change().position().y + (positionPacket.relatives().contains(Relative.Y) ? currentPlayerPos.y : 0),
              positionPacket.change().position().z + (positionPacket.relatives().contains(Relative.Z) ? currentPlayerPos.z : 0)
      );
      if (pos.distanceTo(this.routeToFollow.get(this.currentRouteIndex).toVec3d()) > 6) {
         this.swapState(State.ROTATION, 0);
      }
   }

   @Override
   protected void onWorldRender(WorldRenderContext context) {
      if (!this.isQueued) {
         return;
      }

      this.routeToFollow.drawRoute();
   }

   public void swapState(final State toState, final int delay) {
      this.state = toState;
      this.timer.schedule(delay);
   }

   enum State {
      STARTING, DETECT_ROUTE, ROTATION, ROTATION_VERIFY, AOTV, AOTV_VERIFY, WALK, WALK_VERIFY, END_VERIFY
   }

   public enum NavError {
      NONE, TIME_FAIL, PATHFIND_FAILED
   }
}
