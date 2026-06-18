package me.grish.veinforge.macro.impl.FishingMacro.states;

import me.grish.veinforge.feature.impl.Pathfinder;
import me.grish.veinforge.feature.impl.RouteNavigator;
import me.grish.veinforge.handler.GraphHandler;
import me.grish.veinforge.handler.RotationHandler;
import me.grish.veinforge.macro.impl.FishingMacro.FishingMacro;
import me.grish.veinforge.util.KeyBindUtil;
import me.grish.veinforge.util.PlayerUtil;
import me.grish.veinforge.util.helper.Angle;
import me.grish.veinforge.util.helper.Clock;
import me.grish.veinforge.util.helper.RotationConfiguration;
import me.grish.veinforge.util.helper.graph.Graph;
import me.grish.veinforge.util.helper.route.Route;
import me.grish.veinforge.util.helper.route.RouteWaypoint;
import me.grish.veinforge.util.helper.route.WaypointType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PathfindingState implements FishingMacroState {
    private static final long RETRY_DELAY_MS = 3_000L;
    private static final long WARNING_COOLDOWN_MS = 5_000L;
    private static final long LOOK_SETTLE_MS = 400L;
    private static final long MANUAL_SNAP_TIMEOUT_MS = 2_500L;
    private static final double TARGET_CENTER_TOLERANCE_SQ = 0.11D * 0.11D;
    private static final double MANUAL_SNAP_START_DIST_SQ = 2.25D;
    private static final double MANUAL_SNAP_ABORT_DIST_SQ = 9.0D;
    private static final Vec3 FISHING_SNAP_POINT = new Vec3(-694.4D, 120.0D, 79.2D);

    private static final List<DivergenceTarget> DIVERGENCE_TARGETS = Collections.unmodifiableList(List.of(
            new DivergenceTarget("south_reaches_a", -695, 120, 79, -0.20f, -4.35f)
            // Debug disabled:
            // new DivergenceTarget("south_reaches_b", -692, 120, 82, 90.25f, -2.55f)
    ));

    private final Minecraft mc = Minecraft.getInstance();
    private final RouteNavigator routeNavigator = RouteNavigator.getInstance();
    private final Pathfinder pathfinder = Pathfinder.getInstance();
    private final Clock retryDelay = new Clock();
    private final Clock warningDelay = new Clock();
    private final Random random = new Random();

    private DivergenceTarget selectedTarget;
    private boolean navigationStarted;
    private boolean navigationViaPathfinder;
    private boolean reachedTarget;
    private boolean appliedTargetLook;
    private long reachedTargetAtMs;
    private int exactSnapAttempts;
    private boolean manualSnapActive;
    private long manualSnapStartedAtMs;

    @Override
    public void onStart(FishingMacro macro) {
        routeNavigator.stop();
        pathfinder.stop();
        retryDelay.reset();
        warningDelay.reset();
        navigationStarted = false;
        navigationViaPathfinder = false;
        reachedTarget = false;
        appliedTargetLook = false;
        reachedTargetAtMs = 0L;
        exactSnapAttempts = 0;
        manualSnapActive = false;
        manualSnapStartedAtMs = 0L;
        selectedTarget = chooseRandomTarget();
        log("Entered PathfindingState. Selected divergence target: " + selectedTarget.name + " @ " + selectedTarget.waypoint);
    }

    @Override
    public FishingMacroState onTick(FishingMacro macro) {
        if (!macro.isInGalatea()) {
            return new WarpingState();
        }

        if (reachedTarget) {
            KeyBindUtil.releaseAllExcept();
            applyTargetLookOnce();
            if (reachedTargetAtMs == 0L) {
                reachedTargetAtMs = System.currentTimeMillis();
            }
            if (System.currentTimeMillis() - reachedTargetAtMs >= LOOK_SETTLE_MS) {
                return new FishingKillingState();
            }
            return this;
        }

        if (manualSnapActive) {
            runManualSnap();
            return this;
        }

        if (isCenteredOnSelectedTarget()) {
            reachedTarget = true;
            log("Reached exact divergence block center: " + selectedTarget.waypoint.toBlockPos());
            return this;
        }

        if (isStandingOnSelectedTarget()) {
            startManualSnap("Standing on target block, centering on exact position.");
            return this;
        }

        if (!navigationStarted) {
            attemptStartNavigation(macro);
            return this;
        }

        if (routeNavigator.isRunning()) {
            return this;
        }

        if (!navigationViaPathfinder && routeNavigator.succeeded()) {
            if (isCenteredOnSelectedTarget()) {
                reachedTarget = true;
                log("Reached divergence target exactly: " + selectedTarget.name + " @ " + selectedTarget.waypoint);
            } else if (isNearSelectedTargetForManualSnap()) {
                startManualSnap("Route completed near target, running manual snap.");
            } else {
                warnThrottled("Graph path ended off-target. Snapping to exact block " + selectedTarget.waypoint.toBlockPos());
                startDirectPathfinder();
            }
            return this;
        }

        if (navigationViaPathfinder && pathfinder.isRunning()) {
            return this;
        }

        if (navigationViaPathfinder && pathfinder.succeeded()) {
            if (isCenteredOnSelectedTarget()) {
                reachedTarget = true;
                log("Reached divergence target exactly via Pathfinder: " + selectedTarget.name + " @ " + selectedTarget.waypoint);
                return this;
            }

            if (isNearSelectedTargetForManualSnap()) {
                startManualSnap("Pathfinder completed near target, running manual snap.");
                return this;
            }

            if (distanceToSelectedCenterSq() <= MANUAL_SNAP_ABORT_DIST_SQ) {
                startManualSnap("Pathfinder completed close to target, forcing center snap.");
                return this;
            }

            navigationStarted = false;
            navigationViaPathfinder = false;
            exactSnapAttempts++;
            warnThrottled("Pathfinder finished but not on exact block (current="
                    + PlayerUtil.getBlockStandingOn()
                    + ", target=" + selectedTarget.waypoint.toBlockPos()
                    + ", attempts=" + exactSnapAttempts + "). Retrying exact snap.");
            return this;
        }

        navigationStarted = false;
        navigationViaPathfinder = false;
        if (!retryDelay.isScheduled()) {
            retryDelay.schedule(RETRY_DELAY_MS);
            return this;
        }
        if (!retryDelay.passed()) {
            return this;
        }
        retryDelay.reset();
        attemptStartNavigation(macro);
        return this;
    }

    @Override
    public void onEnd(FishingMacro macro) {
        routeNavigator.stop();
        pathfinder.stop();
        retryDelay.reset();
        warningDelay.reset();
        appliedTargetLook = false;
        reachedTargetAtMs = 0L;
        exactSnapAttempts = 0;
        manualSnapActive = false;
        manualSnapStartedAtMs = 0L;
        KeyBindUtil.releaseAllExcept();
        log("Leaving PathfindingState");
    }

    private void attemptStartNavigation(FishingMacro macro) {
        RouteWaypoint targetNode = resolveGraphTargetNode();
        if (targetNode == null) {
            warnThrottled("Galatea Macro graph has no nodes. Falling back to direct Pathfinder.");
            startDirectPathfinder();
            return;
        }

        List<RouteWaypoint> nodes = GraphHandler.instance.findPath(
                PlayerUtil.getBlockStandingOn(),
                targetNode
        );

        if (nodes == null || nodes.isEmpty()) {
            warnThrottled("No graph path found to divergence target " + selectedTarget.name
                    + ". Falling back to direct Pathfinder.");
            startDirectPathfinder();
            return;
        }

        pathfinder.stop();
        routeNavigator.start(new Route(nodes));
        navigationStarted = true;
        navigationViaPathfinder = false;
        log("Pathfinding started toward " + selectedTarget.name + " via graph node " + targetNode + " using " + nodes.size() + " nodes.");
    }

    private void applyTargetLookOnce() {
        if (appliedTargetLook) {
            return;
        }
        appliedTargetLook = true;
        RotationHandler.getInstance().easeTo(new RotationConfiguration(
                new Angle(selectedTarget.yaw, selectedTarget.pitch),
                300L,
                null
        ));
        log("Applied target look: yaw=" + selectedTarget.yaw + " pitch=" + selectedTarget.pitch);
    }

    private DivergenceTarget chooseRandomTarget() {
        return DIVERGENCE_TARGETS.get(random.nextInt(DIVERGENCE_TARGETS.size()));
    }

    private void startDirectPathfinder() {
        routeNavigator.stop();
        pathfinder.stop();
        pathfinder.queue(selectedTarget.waypoint.toBlockPos());
        pathfinder.start();
        navigationStarted = true;
        navigationViaPathfinder = true;
        log("Pathfinding started toward " + selectedTarget.name + " via direct Pathfinder to " + selectedTarget.waypoint.toBlockPos());
    }

    private boolean isStandingOnSelectedTarget() {
        return PlayerUtil.getBlockStandingOn().equals(selectedTarget.waypoint.toBlockPos());
    }

    private boolean isCenteredOnSelectedTarget() {
        if (mc.player == null) {
            return false;
        }
        if (!isStandingOnSelectedTarget()) {
            return false;
        }
        Vec3 targetCenter = getSelectedTargetCenter();
        double dx = mc.player.getX() - targetCenter.x;
        double dz = mc.player.getZ() - targetCenter.z;
        return (dx * dx + dz * dz) <= TARGET_CENTER_TOLERANCE_SQ;
    }

    private boolean isNearSelectedTargetForManualSnap() {
        if (mc.player == null) {
            return false;
        }
        return distanceToSelectedCenterSq() <= MANUAL_SNAP_START_DIST_SQ;
    }

    private Vec3 getSelectedTargetCenter() {
        return new Vec3(FISHING_SNAP_POINT.x, selectedTarget.waypoint.toBlockPos().getY(), FISHING_SNAP_POINT.z);
    }

    private double distanceToSelectedCenterSq() {
        if (mc.player == null) {
            return Double.MAX_VALUE;
        }
        Vec3 targetCenter = getSelectedTargetCenter();
        double dx = mc.player.getX() - targetCenter.x;
        double dz = mc.player.getZ() - targetCenter.z;
        return (dx * dx + dz * dz);
    }

    private void startManualSnap(String reason) {
        routeNavigator.stop();
        pathfinder.stop();
        navigationStarted = false;
        navigationViaPathfinder = false;
        manualSnapActive = true;
        manualSnapStartedAtMs = System.currentTimeMillis();
        log(reason);
    }

    private void runManualSnap() {
        if (mc.player == null) {
            manualSnapActive = false;
            KeyBindUtil.releaseAllExcept();
            return;
        }

        if (isCenteredOnSelectedTarget()) {
            manualSnapActive = false;
            KeyBindUtil.releaseAllExcept();
            reachedTarget = true;
            log("Manual snap complete on exact target center.");
            return;
        }

        Vec3 targetCenter = getSelectedTargetCenter();
        double dx = mc.player.getX() - targetCenter.x;
        double dz = mc.player.getZ() - targetCenter.z;
        double distSq = dx * dx + dz * dz;

        if (distSq > MANUAL_SNAP_ABORT_DIST_SQ || System.currentTimeMillis() - manualSnapStartedAtMs > MANUAL_SNAP_TIMEOUT_MS) {
            manualSnapActive = false;
            KeyBindUtil.releaseAllExcept();
            navigationStarted = false;
            navigationViaPathfinder = false;
            exactSnapAttempts++;
            warnThrottled("Manual snap failed/timed out, retrying exact pathing.");
            return;
        }

        List<KeyMapping> keys = KeyBindUtil.getNeededKeyPresses(
                mc.player.position(),
                new Vec3(targetCenter.x, mc.player.getY(), targetCenter.z)
        );
        if (keys.isEmpty()) {
            KeyBindUtil.releaseAllExcept();
        } else {
            KeyBindUtil.holdThese(keys.toArray(new KeyMapping[0]));
        }
        KeyBindUtil.setKeyBindState(mc.options.keySprint, false);
        KeyBindUtil.setKeyBindState(mc.options.keyJump, false);
    }

    private RouteWaypoint resolveGraphTargetNode() {
        Graph<RouteWaypoint> graph = GraphHandler.instance.getActiveGraph();
        if (graph == null || graph.map.isEmpty()) {
            return null;
        }

        BlockPos targetBlock = selectedTarget.waypoint.toBlockPos();
        RouteWaypoint nearest = null;
        double bestDistSq = Double.MAX_VALUE;

        for (RouteWaypoint node : graph.map.keySet()) {
            if (node == null) {
                continue;
            }
            double distSq = node.toBlockPos().distSqr(targetBlock);
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                nearest = node;
            }
        }
        return nearest;
    }

    private void warnThrottled(String message) {
        if (!warningDelay.isScheduled() || warningDelay.passed()) {
            log(message);
            warningDelay.schedule(WARNING_COOLDOWN_MS);
        }
    }

    private static class DivergenceTarget {
        private final String name;
        private final RouteWaypoint waypoint;
        private final float yaw;
        private final float pitch;

        private DivergenceTarget(String name, int x, int y, int z, float yaw, float pitch) {
            this.name = name;
            this.waypoint = new RouteWaypoint(x, y, z, WaypointType.WALK);
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
}
