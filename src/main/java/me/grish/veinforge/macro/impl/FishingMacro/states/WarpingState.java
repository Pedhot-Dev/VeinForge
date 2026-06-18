package me.grish.veinforge.macro.impl.FishingMacro.states;

import me.grish.veinforge.feature.impl.AutoWarp;
import me.grish.veinforge.handler.GameStateHandler;
import me.grish.veinforge.macro.impl.FishingMacro.FishingMacro;
import me.grish.veinforge.util.helper.Clock;
import me.grish.veinforge.util.helper.location.SubLocation;

import java.util.Locale;

public class WarpingState implements FishingMacroState {
    private static final int MAX_WARP_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2_000L;
    private static final long WARNING_COOLDOWN_MS = 5_000L;

    private final AutoWarp autoWarp = AutoWarp.getInstance();
    private final Clock retryDelay = new Clock();
    private final Clock warningDelay = new Clock();
    private int retryCount = 0;

    @Override
    public void onStart(FishingMacro macro) {
        this.retryCount = 0;
        this.retryDelay.reset();
        this.warningDelay.reset();
        log("Entered WarpingState");
        if (!isOnHypixel()) {
            warnThrottled("Not connected to Hypixel. Waiting before auto-warping.");
            return;
        }
        if (!macro.isInGalatea()) {
            startGalateaWarp();
        }
    }

    @Override
    public FishingMacroState onTick(FishingMacro macro) {
        if (!isOnHypixel()) {
            if (autoWarp.isRunning()) {
                autoWarp.stop();
            }
            warnThrottled("Not connected to Hypixel. Cannot use /warp or /play commands.");
            return this;
        }

        if (macro.isInGalatea()) {
            if (autoWarp.isRunning()) {
                autoWarp.stop();
            }
            return new PathfindingState();
        }

        if (!GameStateHandler.getInstance().isPlayerInSkyBlock()) {
            warnThrottled("Not in SkyBlock yet. Attempting to enter SkyBlock before warping to Galatea.");
        }

        if (autoWarp.isRunning()) {
            return this;
        }

        if (autoWarp.hasSucceeded()) {
            if (!retryDelay.isScheduled()) {
                retryDelay.schedule(RETRY_DELAY_MS);
                return this;
            }
            if (!retryDelay.passed()) {
                return this;
            }
            retryDelay.reset();
            if (retryCount >= MAX_WARP_RETRIES) {
                warnThrottled("Warp finished but Galatea was not detected. Retrying...");
                retryCount = 0;
            }
            startGalateaWarp();
            return this;
        }

        switch (autoWarp.getFailReason()) {
            case NO_SCROLL:
                warnThrottled("Missing /warp galatea travel scroll. Warp manually to Galatea.");
                return this;
            case FAILED_TO_WARP:
                if (retryCount >= MAX_WARP_RETRIES) {
                    warnThrottled("Failed to warp to Galatea after " + MAX_WARP_RETRIES + " retries. Trying again...");
                    retryCount = 0;
                }
                if (!retryDelay.isScheduled()) {
                    retryDelay.schedule(RETRY_DELAY_MS);
                    return this;
                }
                if (!retryDelay.passed()) {
                    return this;
                }
                retryDelay.reset();
                startGalateaWarp();
                return this;
            case NONE:
            default:
                startGalateaWarp();
                return this;
        }
    }

    private void startGalateaWarp() {
        retryCount++;
        autoWarp.start(null, SubLocation.GALATEA);
        log("Warping to Galatea (attempt " + retryCount + ")");
    }

    @Override
    public void onEnd(FishingMacro macro) {
        if (autoWarp.isRunning()) {
            autoWarp.stop();
        }
        retryDelay.reset();
        warningDelay.reset();
        log("Leaving WarpingState");
    }

    private void warnThrottled(String message) {
        if (!warningDelay.isScheduled() || warningDelay.passed()) {
            log(message);
            warningDelay.schedule(WARNING_COOLDOWN_MS);
        }
    }

    private boolean isOnHypixel() {
        String ip = GameStateHandler.getInstance().getServerIp();
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        return ip.toLowerCase(Locale.ROOT).contains("hypixel.net");
    }
}
