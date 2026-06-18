package me.grish.veinforge.feature.impl;

import me.grish.veinforge.VeinForge;
import me.grish.veinforge.feature.AbstractFeature;
import me.grish.veinforge.handler.RouteHandler;
import me.grish.veinforge.util.KeyPressUtil;
import me.grish.veinforge.util.Logger;
import me.grish.veinforge.util.PlayerUtil;
import me.grish.veinforge.util.helper.route.Route;
import me.grish.veinforge.util.helper.route.RouteWaypoint;
import me.grish.veinforge.util.helper.route.WaypointType;
import net.minecraft.client.Minecraft;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RouteBuilder extends AbstractFeature {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static RouteBuilder instance;

    public static RouteBuilder getInstance() {
        if (instance == null) {
            instance = new RouteBuilder();
        }
        return instance;
    }

    @Override
    public String getName() {
        return "RouteBuilder";
    }

    public void toggle() {
        if (!this.enabled) {
            this.start();
        } else {
            this.stop();
        }
    }

    @Override
    public void start() {
        this.enabled = true;
        scheduler.schedule(
                RouteHandler.getInstance()::saveData,
                0,
                TimeUnit.MILLISECONDS
        );
        send("Enabling RouteBuilder.");
    }

    @Override
    public void stop() {
        this.enabled = false;
        send("Disabling RouteBuilder.");
    }

    @Override
    protected void onTick() {
        var config = VeinForge.config();
        if (config == null) {
            return;
        }

        com.mojang.blaze3d.platform.Window window = Minecraft.getInstance().getWindow();
        boolean walkPressed = KeyPressUtil.wasPressed(window, config.routeMiner.routeBuilderWalkAddKeybind, this.enabled);
        boolean etherwarpPressed = KeyPressUtil.wasPressed(window, config.routeMiner.routeBuilderEtherwarpAddKeybind, this.enabled);
        boolean removePressed = KeyPressUtil.wasPressed(window, config.routeMiner.routeBuilderRemoveKeybind, this.enabled);

        if (!this.enabled) {
            return;
        }

        if (walkPressed) {
            if (this.addToRoute(WaypointType.WALK)) {
                Logger.sendMessage("Added Walk");
            }
        }

        if (etherwarpPressed) {
            if (this.addToRoute(WaypointType.ETHERWARP)) {
                Logger.sendMessage("Added Etherwarp");
            }
        }

        if (removePressed) {
            Route selectedRoute = RouteHandler.getInstance().getSelectedRoute();
            if (selectedRoute.isEmpty()) {
                return;
            }

            if (PlayerUtil.getBlockStandingOn() != null) {
                Optional<RouteWaypoint> closest = selectedRoute.getClosest(PlayerUtil.getBlockStandingOn());
                if (!closest.isPresent()) {
                    return;
                }

                int index = selectedRoute.indexOf(closest.get());

                if (index == -1) {
                    return;
                }

                this.removeFromRoute(index);
                Logger.sendMessage("Removed Waypoint");
            }
        }
    }

    public boolean addToRoute(final WaypointType method) {
        return RouteHandler.getInstance().addToCurrentRoute(
                PlayerUtil.getBlockStandingOn(),
                method
        );
    }

    public void removeFromRoute(int index) {
        RouteHandler.getInstance().removeFromCurrentRoute(index);
    }

    public void replaceNode(final int index) {
        RouteHandler.getInstance().replaceInCurrentRoute(
                index,
                new RouteWaypoint(
                        PlayerUtil.getBlockStandingOn(),
                        WaypointType.ETHERWARP
                )
        );
    }
}
