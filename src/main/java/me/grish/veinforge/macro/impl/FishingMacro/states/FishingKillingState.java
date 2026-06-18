package me.grish.veinforge.macro.impl.FishingMacro.states;

import me.grish.veinforge.VeinForge;
import me.grish.veinforge.feature.impl.AutoMobKiller.AutoMobKiller;
import me.grish.veinforge.handler.RotationHandler;
import me.grish.veinforge.macro.impl.FishingMacro.FishingMacro;
import me.grish.veinforge.util.AngleUtil;
import me.grish.veinforge.util.FishingUtil;
import me.grish.veinforge.util.InventoryUtil;
import me.grish.veinforge.util.KeyBindUtil;
import me.grish.veinforge.util.helper.Angle;
import me.grish.veinforge.util.helper.RotationConfiguration;
import me.grish.veinforge.util.helper.Target;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class FishingKillingState implements FishingMacroState {
    private static final long STATUS_LOG_INTERVAL_MS = 3_000L;
    private static final long ROD_WARNING_COOLDOWN_MS = 5_000L;

    private static final long NO_CATCH_MIN_MS = 10_000L;
    private static final long NO_CATCH_MAX_MS = 15_000L;
    private static final long POST_CAST_STABILIZE_MIN_MS = 500L;
    private static final long POST_CAST_STABILIZE_MAX_MS = 900L;
    private static final long ARMOR_CHECK_INTERVAL_MS = 120L;
    private static final long BITE_REEL_DELAY_MIN_MS = 80L;
    private static final long BITE_REEL_DELAY_MAX_MS = 160L;
    private static final long NO_CATCH_RECAST_DELAY_MS = 120L;
    private static final String BITE_MARKER_ARMOR_NAME = "!!!";
    private static final double BITE_MARKER_SCAN_RADIUS_BLOCKS = 64.0D;
    private static final double HOOK_FALLBACK_MAX_DIST_SQ = 48.0D * 48.0D;

    private static final double STUCK_KILL_RADIUS_BLOCKS = 2.0D;
    private static final long STUCK_AIM_LOCK_HOLD_MS = 40L;
    private static final long STUCK_AIM_LOCK_RETRY_MS = 20L;
    private static final float STUCK_AIM_LOCK_MAX_YAW_ERROR_DEG = 5.0F;
    private static final float STUCK_AIM_LOCK_MAX_PITCH_ERROR_DEG = 3.5F;
    private static final int STUCK_COMBO_CYCLE_LENGTH = 6;
    private static final double AXE_MELEE_RANGE_BLOCKS = 4.0D;
    private static final double CAP_KILL_SLAYER_RANGE_BLOCKS = 7.0D;
    private static final long CAP_KILL_NO_REACHABLE_TIMEOUT_MS = 300L;
    private static final long CAP_KILL_FINISH_CAST_DELAY_MS = 60L;
    private static final long CAP_KILL_REENTRY_COOLDOWN_MS = 5_000L;
    private static final long CAP_KILL_REENTRY_SLAYER_COOLDOWN_MS = 1_200L;
    private static final long CAP_KILL_RIGHT_CLICK_MIN_INTERVAL_MS = 100L;
    private static final long CAP_KILL_RIGHT_CLICK_MAX_INTERVAL_MS = 200L;
    private static final long CAP_KILL_LEFT_CLICK_MIN_INTERVAL_MS = 100L;
    private static final long CAP_KILL_LEFT_CLICK_MAX_INTERVAL_MS = 200L;
    private static final long CAP_KILL_SLAYER_ASSIST_MIN_INTERVAL_MS = 2_200L;
    private static final long CAP_KILL_SLAYER_ASSIST_MAX_INTERVAL_MS = 3_200L;
    private static final long CAP_KILL_SLAYER_POST_CAST_WAIT_MIN_MS = 2_000L;
    private static final long CAP_KILL_SLAYER_POST_CAST_WAIT_MAX_MS = 3_000L;
    private static final long KILL_AIM_REAIM_INTERVAL_MS = 110L;
    private static final long KILL_AIM_ROTATION_MIN_MS = 45L;
    private static final long KILL_AIM_ROTATION_MAX_MS = 85L;
    private static final long KILL_AIM_POST_ROTATION_HOLD_MS = 12L;
    private static final float KILL_AIM_LOCK_MAX_YAW_ERROR_DEG = 5.5F;
    private static final float KILL_AIM_LOCK_MAX_PITCH_ERROR_DEG = 4.0F;
    private static final float KILL_AIM_FORCE_REAIM_YAW_ERROR_DEG = 12.0F;
    private static final float KILL_AIM_FORCE_REAIM_PITCH_ERROR_DEG = 8.0F;
    private static final float FISHING_IDLE_YAW_DEG = -0.20F;
    private static final float FISHING_IDLE_PITCH_DEG = -4.35F;
    private static final long FISHING_LOOK_EASE_MIN_MS = 140L;
    private static final long FISHING_LOOK_EASE_MAX_MS = 220L;
    private static final long CAST_ALIGN_TIMEOUT_PAD_MS = 300L;
    private static final float FISHING_LOOK_MAX_YAW_JITTER_DEG = 0.14F;
    private static final float FISHING_LOOK_MAX_PITCH_JITTER_DEG = 0.08F;
    private static final double FISHING_ANCHOR_X = -694.4D;
    private static final double FISHING_ANCHOR_Z = 79.2D;
    private static final double FISHING_ALLOWED_RADIUS_BLOCKS = 5.0D;
    private static final double FISHING_ALLOWED_RADIUS_SQ = FISHING_ALLOWED_RADIUS_BLOCKS * FISHING_ALLOWED_RADIUS_BLOCKS;
    private static final long FISHING_RADIUS_WARNING_COOLDOWN_MS = 5_000L;
    private static final String HUD_REASON_WAITING_TRIGGER = "WAITING_TRIGGER";
    private static final String HUD_REASON_OUT_OF_RANGE = "OUT_OF_RANGE";
    private static final String HUD_REASON_NO_LOS = "NO_LOS";
    private static final String HUD_REASON_OUTSIDE_RADIUS = "OUTSIDE_RADIUS";
    private static final String HUD_REASON_CAP_KILL = "CAP_KILL_ACTIVE";
    private static final String HUD_REASON_STUCK = "STUCK_COMBAT";
    private static final String HUD_REASON_AUTOMOB = "AUTOMOB_ACTIVE";
    private static final String HUD_REASON_OUTSIDE_GALATEA = "OUTSIDE_GALATEA";

    private final Minecraft mc = Minecraft.getInstance();
    private final Random random = new Random();
    private final Set<Integer> trackedNearbyStriderIds = new HashSet<>();
    private int striderTriggerCount = -1;
    private long nextStatusLogAtMs = 0L;
    private long nextRodWarningAtMs = 0L;
    private long nextKillAttemptAtMs = 0L;
    private long noCatchDeadlineAtMs = 0L;
    private long stabilizeUntilMs = 0L;
    private long nextArmorCheckAtMs = 0L;
    private long pendingReelAtMs = 0L;
    private FishingLoopState fishingLoopState = FishingLoopState.NEEDS_CAST;
    private long nextFishingActionAtMs = 0L;
    private long castAtMs = 0L;
    private boolean castAlignInProgress = false;
    private boolean castAlignCompleted = false;
    private long castAlignDeadlineAtMs = 0L;

    private boolean inStuckCombat = false;
    private int stuckComboStep = 0;
    private long nextStuckAttackAtMs = 0L;
    private long stuckAimLockedSinceMs = 0L;
    private int stuckTargetEntityId = -1;
    private boolean stuckLockEstablished = false;
    private boolean capKillMode = false;
    private long nextCapKillAttackAtMs = 0L;
    private long nextCapKillMeleeAtMs = 0L;
    private long capKillNoReachableDeadlineAtMs = 0L;
    private long capKillSuppressedUntilAtMs = 0L;
    private long nextCapKillSlayerAssistAtMs = 0L;
    private boolean capKillArmed = true;
    private long nextKillAimAtMs = 0L;
    private int capKillTargetEntityId = -1;
    private int hudNearbyStriders = 0;
    private int hudReachableStriders = 0;
    private int hudMinStriderCount = 0;
    private int hudMaxStriderCount = 0;
    private long hudLastUpdatedAtMs = 0L;
    private String hudReason = HUD_REASON_WAITING_TRIGGER;
    private long nextRadiusWarningAtMs = 0L;
    private long combatAimReadyAtMs = 0L;
    private int combatAimTargetEntityId = -1;

    @Override
    public void onStart(FishingMacro macro) {
        long now = System.currentTimeMillis();
        nextStatusLogAtMs = 0L;
        nextRodWarningAtMs = 0L;
        nextKillAttemptAtMs = 0L;
        noCatchDeadlineAtMs = 0L;
        stabilizeUntilMs = 0L;
        nextArmorCheckAtMs = 0L;
        pendingReelAtMs = 0L;
        trackedNearbyStriderIds.clear();

        fishingLoopState = FishingLoopState.NEEDS_CAST;
        castAtMs = 0L;
        nextFishingActionAtMs = now + 200L;
        resetCastAlignmentState();

        inStuckCombat = false;
        stuckComboStep = 0;
        nextStuckAttackAtMs = 0L;
        stuckAimLockedSinceMs = 0L;
        stuckTargetEntityId = -1;
        stuckLockEstablished = false;
        capKillMode = false;
        nextCapKillAttackAtMs = 0L;
        nextCapKillMeleeAtMs = 0L;
        capKillNoReachableDeadlineAtMs = 0L;
        capKillSuppressedUntilAtMs = 0L;
        nextCapKillSlayerAssistAtMs = 0L;
        capKillArmed = true;
        nextKillAimAtMs = 0L;
        capKillTargetEntityId = -1;
        hudNearbyStriders = 0;
        hudReachableStriders = 0;
        hudMinStriderCount = 0;
        hudMaxStriderCount = 0;
        hudLastUpdatedAtMs = now;
        hudReason = HUD_REASON_WAITING_TRIGGER;
        nextRadiusWarningAtMs = 0L;
        combatAimReadyAtMs = 0L;
        combatAimTargetEntityId = -1;

        pickNextTriggerCount();
        scheduleNoCatchDeadline(now);
        log("Entered FishingKillingState");
    }

    @Override
    public FishingMacroState onTick(FishingMacro macro) {
        if (!macro.isInGalatea()) {
            setHudReason(HUD_REASON_OUTSIDE_GALATEA);
            stopAutoMobKillerIfRunning();
            return new WarpingState();
        }

        long now = System.currentTimeMillis();

        if (!isInsideFishingRadius()) {
            setHudReason(HUD_REASON_OUTSIDE_RADIUS);
            stopAutoMobKillerIfRunning();
            warnOutsideFishingRadius(now);
            return new PathfindingState();
        }

        if (AutoMobKiller.getInstance().isRunning()) {
            setHudReason(HUD_REASON_AUTOMOB);
            return this;
        }

        Entity closeStrider = findNearestStriderWithinRadius(STUCK_KILL_RADIUS_BLOCKS);
        Entity stuckTarget = resolveActiveStuckTarget(closeStrider);
        if (stuckTarget != null) {
            if (!inStuckCombat) {
                inStuckCombat = true;
                stuckComboStep = 0;
                nextStuckAttackAtMs = 0L;
                stuckAimLockedSinceMs = 0L;
                stuckTargetEntityId = stuckTarget.getId();
                stuckLockEstablished = false;
                logEvent("TARGET_SWITCH", "reason=enter id=" + stuckTarget.getId());
                stopFishingBeforeCombat(now);
                RotationHandler.getInstance().stop();
                nextKillAimAtMs = 0L;
                combatAimReadyAtMs = 0L;
                combatAimTargetEntityId = -1;
                log("Stuck Strider in melee radius. Starting close-range combo.");
            }
            if (stuckTargetEntityId != stuckTarget.getId()) {
                int previousTargetId = stuckTargetEntityId;
                stuckTargetEntityId = stuckTarget.getId();
                stuckAimLockedSinceMs = 0L;
                stuckLockEstablished = false;
                RotationHandler.getInstance().stop();
                nextKillAimAtMs = 0L;
                combatAimReadyAtMs = 0L;
                combatAimTargetEntityId = -1;
                logEvent("TARGET_SWITCH", "reason=reacquire from=" + previousTargetId + " to=" + stuckTarget.getId());
            }
            setHudReason(HUD_REASON_STUCK);
            if (!updateCombatAim(stuckTarget, now)) {
                return this;
            }
            executeStuckCombo(now, stuckTarget);
            return this;
        }

        if (inStuckCombat) {
            inStuckCombat = false;
            stuckComboStep = 0;
            nextStuckAttackAtMs = 0L;
            stuckAimLockedSinceMs = 0L;
            stuckTargetEntityId = -1;
            stuckLockEstablished = false;
            scheduleNextCast(now + getCombatActionDelayMs());
            RotationHandler.getInstance().stop();
            nextKillAimAtMs = 0L;
            combatAimReadyAtMs = 0L;
            combatAimTargetEntityId = -1;
            alignToFishingDirection();
        }

        int minStriderCount = Math.min(
                VeinForge.config().fishing.galateaFishing.galateaStriderMinCount,
                VeinForge.config().fishing.galateaFishing.galateaStriderMaxCount
        );
        int maxStriderCount = Math.max(
                VeinForge.config().fishing.galateaFishing.galateaStriderMinCount,
                VeinForge.config().fishing.galateaFishing.galateaStriderMaxCount
        );
        int capKillStartThreshold = getCapKillStartThreshold(minStriderCount, maxStriderCount);

        if (striderTriggerCount < minStriderCount || striderTriggerCount > maxStriderCount) {
            pickNextTriggerCount();
        }

        Set<Integer> nearbyStriderIds = collectNearbyStriderIds(VeinForge.config().fishing.galateaFishing.galateaStriderScanRadius);
        int nearbyStriders = nearbyStriderIds.size();
        int reachableStriders = countReachableStridersWithinRadius(getCapKillTargetRangeBlocks());
        logStriderScannerDelta(nearbyStriderIds, nearbyStriders);
        updateHudSnapshot(now, minStriderCount, maxStriderCount, nearbyStriders, reachableStriders);
        boolean inConfiguredRange = nearbyStriders >= minStriderCount && nearbyStriders <= maxStriderCount;
        if (nearbyStriders < capKillStartThreshold) {
            capKillArmed = true;
        }

        if (!capKillMode && capKillArmed && now >= capKillSuppressedUntilAtMs && nearbyStriders >= capKillStartThreshold) {
            startCapKillMode(now, nearbyStriders, capKillStartThreshold);
        }

        if (capKillMode) {
            runCapKillMode(now, nearbyStriders);
            return this;
        }

        runFishingLoop(now);
        setHudReason(HUD_REASON_WAITING_TRIGGER);

        if (now >= nextKillAttemptAtMs && inConfiguredRange && nearbyStriders >= striderTriggerCount) {
            // User-requested behavior: only engage immediately if a Strider is close enough.
            // No roaming chase/pathing from this state.
            nextKillAttemptAtMs = now + getCombatActionDelayMs();
        } else {
            maybeLogStatus(nearbyStriders, minStriderCount, maxStriderCount);
        }

        return this;
    }

    @Override
    public void onEnd(FishingMacro macro) {
        stopAutoMobKillerIfRunning();
        fishingLoopState = FishingLoopState.NEEDS_CAST;
        castAtMs = 0L;
        nextFishingActionAtMs = 0L;
        resetCastAlignmentState();
        stabilizeUntilMs = 0L;
        nextArmorCheckAtMs = 0L;
        pendingReelAtMs = 0L;
        trackedNearbyStriderIds.clear();
        inStuckCombat = false;
        stuckComboStep = 0;
        nextStuckAttackAtMs = 0L;
        stuckAimLockedSinceMs = 0L;
        stuckTargetEntityId = -1;
        stuckLockEstablished = false;
        capKillMode = false;
        nextCapKillAttackAtMs = 0L;
        nextCapKillMeleeAtMs = 0L;
        capKillNoReachableDeadlineAtMs = 0L;
        capKillSuppressedUntilAtMs = 0L;
        nextCapKillSlayerAssistAtMs = 0L;
        capKillArmed = true;
        capKillTargetEntityId = -1;
        hudLastUpdatedAtMs = System.currentTimeMillis();
        hudReason = HUD_REASON_WAITING_TRIGGER;
        nextRadiusWarningAtMs = 0L;
        combatAimReadyAtMs = 0L;
        combatAimTargetEntityId = -1;
        RotationHandler.getInstance().stop();
        log("Leaving FishingKillingState");
    }

    private void runFishingLoop(long now) {
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (now < nextFishingActionAtMs) {
            return;
        }

        String rodName = VeinForge.config().fishing.generalFishing.fishingRod;
        if (rodName == null || rodName.trim().isEmpty()) {
            return;
        }

        if (fishingLoopState == FishingLoopState.NEEDS_CAST) {
            if (!InventoryUtil.holdItem(rodName)) {
                warnRodMissing(now, rodName);
                resetCastAlignmentState();
                nextFishingActionAtMs = now + 500L;
                return;
            }

            if (!castAlignInProgress) {
                castAlignCompleted = false;
                long easeMs = alignToFishingDirection(() -> castAlignCompleted = true);
                castAlignInProgress = true;
                castAlignDeadlineAtMs = now + easeMs + CAST_ALIGN_TIMEOUT_PAD_MS;
                nextFishingActionAtMs = now + 40L;
                return;
            }

            if (!castAlignCompleted && now < castAlignDeadlineAtMs) {
                nextFishingActionAtMs = now + 40L;
                return;
            }

            if (!castAlignCompleted) {
                log("Cast alignment timeout reached. Casting anyway.");
                logEvent("TIMEOUT", "type=cast_align");
            }

            resetCastAlignmentState();
            KeyBindUtil.rightClick();
            castAtMs = now;
            fishingLoopState = FishingLoopState.WAITING_FOR_BITE;
            stabilizeUntilMs = now + randomRange(POST_CAST_STABILIZE_MIN_MS, POST_CAST_STABILIZE_MAX_MS);
            nextArmorCheckAtMs = stabilizeUntilMs;
            pendingReelAtMs = 0L;
            scheduleNoCatchDeadline(now);
            log("Cast rod. stabilizeUntilMs=" + stabilizeUntilMs + ", noCatchDeadlineMs=" + noCatchDeadlineAtMs);
            logEvent("CAST", "phase=initial noCatchDeadlineMs=" + noCatchDeadlineAtMs);
            nextFishingActionAtMs = now + 120L;
            return;
        }

        if (pendingReelAtMs > 0L) {
            if (now >= pendingReelAtMs) {
                reelAndScheduleNextCast(now, rodName, true, "BITE_MARKER", getCastActionDelayMs());
                scheduleNoCatchDeadline(now);
            } else {
                nextFishingActionAtMs = now + 60L;
            }
            return;
        }

        if (now >= noCatchDeadlineAtMs) {
            log("No catch timeout reached. Recasting.");
            logEvent("TIMEOUT", "type=no_catch castAgeMs=" + (now - castAtMs));
            alignToFishingDirection();
            reelAndScheduleNextCast(now, rodName, true, "NO_CATCH_TIMEOUT", NO_CATCH_RECAST_DELAY_MS);
            scheduleNoCatchDeadline(now);
            return;
        }

        if (now >= stabilizeUntilMs && now >= nextArmorCheckAtMs) {
            nextArmorCheckAtMs = now + ARMOR_CHECK_INTERVAL_MS;
            int markerCount = countNearbyBiteArmorMarkers();
            if (markerCount > 0) {
                long reelDelay = randomRange(BITE_REEL_DELAY_MIN_MS, BITE_REEL_DELAY_MAX_MS);
                pendingReelAtMs = now + reelDelay;
                log("Bite marker detected. count=" + markerCount + ", scheduling reel in " + reelDelay + "ms.");
                nextFishingActionAtMs = pendingReelAtMs;
                return;
            }
        }

        long castElapsed = now - castAtMs;
        long maxWaitMs = Math.max(5L, VeinForge.config().fishing.generalFishing.maxWaitSeconds) * 1000L;
        if (castElapsed >= maxWaitMs) {
            logEvent("TIMEOUT", "type=max_wait castAgeMs=" + castElapsed + " maxWaitMs=" + maxWaitMs);
            reelAndScheduleNextCast(now, rodName, true, "MAX_WAIT", getCastActionDelayMs());
            scheduleNoCatchDeadline(now);
        } else {
            nextFishingActionAtMs = now + 120L;
        }
    }

    private void startCapKillMode(long now, int nearbyStriders, int maxStriderCount) {
        capKillMode = true;
        capKillArmed = false;
        capKillNoReachableDeadlineAtMs = 0L;
        nextCapKillSlayerAssistAtMs = now;
        capKillTargetEntityId = -1;
        nextCapKillAttackAtMs = now;
        nextCapKillMeleeAtMs = now;
        stopFishingBeforeCombat(now);
        RotationHandler.getInstance().stop();
        nextKillAimAtMs = 0L;
        combatAimReadyAtMs = 0L;
        combatAimTargetEntityId = -1;
        log("Strider cap trigger reached (" + nearbyStriders + "/" + maxStriderCount + "). Starting stationary kill routine.");
    }

    private void runCapKillMode(long now, int nearbyStriders) {
        double capKillRangeBlocks = getCapKillTargetRangeBlocks();
        Entity target = resolveCapKillTarget(capKillRangeBlocks);
        if (target == null) {
            if (nearbyStriders <= 0) {
                finishCapKillMode(now, "All nearby Striders cleared. Returning to fishing.");
                return;
            }

            setHudReason(determineCapKillNoTargetReason(capKillRangeBlocks));
            if (capKillNoReachableDeadlineAtMs == 0L) {
                capKillNoReachableDeadlineAtMs = now + CAP_KILL_NO_REACHABLE_TIMEOUT_MS;
            }
            maybeLogCapKillStatus(nearbyStriders, 0, now);
            if (now >= capKillNoReachableDeadlineAtMs) {
                finishCapKillMode(now, "No reachable Striders in range. Returning to fishing.");
            }
            return;
        }

        capKillNoReachableDeadlineAtMs = 0L;
        int reachableStriders = countReachableStridersWithinRadius(capKillRangeBlocks);
        hudReachableStriders = reachableStriders;
        maybeLogCapKillStatus(nearbyStriders, reachableStriders, now);

        if (mc.player == null) {
            return;
        }
        double rangeSq = capKillRangeBlocks * capKillRangeBlocks;
        if (mc.player.distanceToSqr(target) > rangeSq) {
            capKillTargetEntityId = -1;
            setHudReason(HUD_REASON_OUT_OF_RANGE);
            return;
        }
        if (!mc.player.hasLineOfSight(target)) {
            capKillTargetEntityId = -1;
            setHudReason(HUD_REASON_NO_LOS);
            return;
        }

        setHudReason(HUD_REASON_CAP_KILL);
        if (!updateCombatAim(target, now)) {
            return;
        }

        if (now >= nextCapKillAttackAtMs) {
            long nextRightClickDelayMs = performCapKillRightClick(now);
            if (nextRightClickDelayMs <= 0L) {
                log("Cap kill: right-click action skipped (weapon/ability unavailable).");
                nextRightClickDelayMs = randomRange(CAP_KILL_RIGHT_CLICK_MIN_INTERVAL_MS, CAP_KILL_RIGHT_CLICK_MAX_INTERVAL_MS);
            }
            nextCapKillAttackAtMs = now + nextRightClickDelayMs;
        }

        if (now >= nextCapKillMeleeAtMs) {
            if (isAxeMeleeRange(target) && !performCapKillMeleeHit()) {
                finishCapKillMode(now, "Required weapon missing. Returning to fishing.");
                return;
            }
            nextCapKillMeleeAtMs = now + randomRange(CAP_KILL_LEFT_CLICK_MIN_INTERVAL_MS, CAP_KILL_LEFT_CLICK_MAX_INTERVAL_MS);
        }
    }

    private String determineCapKillNoTargetReason(double radius) {
        Entity nearby = findNearestStriderWithinRadius(radius);
        if (nearby == null) {
            return HUD_REASON_OUT_OF_RANGE;
        }
        if (mc.player != null && !mc.player.hasLineOfSight(nearby)) {
            return HUD_REASON_NO_LOS;
        }
        return HUD_REASON_OUT_OF_RANGE;
    }

    private void finishCapKillMode(long now, String reason) {
        capKillMode = false;
        capKillTargetEntityId = -1;
        capKillNoReachableDeadlineAtMs = 0L;
        nextCapKillSlayerAssistAtMs = 0L;
        capKillArmed = true;
        capKillSuppressedUntilAtMs = now + getCapKillReentryCooldownMs();
        RotationHandler.getInstance().stop();
        nextKillAimAtMs = 0L;
        combatAimReadyAtMs = 0L;
        combatAimTargetEntityId = -1;
        alignToFishingDirection();
        scheduleNextCast(now + CAP_KILL_FINISH_CAST_DELAY_MS);
        log("Cap kill routine finished. " + reason);
    }

    private void maybeLogCapKillStatus(int nearbyStriders, int reachableStriders, long now) {
        if (now < nextStatusLogAtMs) {
            return;
        }
        nextStatusLogAtMs = now + STATUS_LOG_INTERVAL_MS;
        log("Cap kill active. Nearby=" + nearbyStriders + ", reachable=" + reachableStriders + ", range=" + getCapKillTargetRangeBlocks());
    }

    private void updateHudSnapshot(long now, int minStriderCount, int maxStriderCount, int nearbyStriders, int reachableStriders) {
        hudMinStriderCount = minStriderCount;
        hudMaxStriderCount = maxStriderCount;
        hudNearbyStriders = nearbyStriders;
        hudReachableStriders = reachableStriders;
        hudLastUpdatedAtMs = now;
    }

    public int getHudNearbyStriders() {
        return hudNearbyStriders;
    }

    public int getHudReachableStriders() {
        return hudReachableStriders;
    }

    public int getHudMinStriderCount() {
        return hudMinStriderCount;
    }

    public int getHudMaxStriderCount() {
        return hudMaxStriderCount;
    }

    public int getStriderTriggerCount() {
        return striderTriggerCount;
    }

    public boolean isCapKillMode() {
        return capKillMode;
    }

    public boolean isInStuckCombat() {
        return inStuckCombat;
    }

    public int getCapKillTargetEntityId() {
        return capKillTargetEntityId;
    }

    public long getNextCapKillAttackAtMs() {
        return nextCapKillAttackAtMs;
    }

    public long getNextCapKillMeleeAtMs() {
        return nextCapKillMeleeAtMs;
    }

    public long getCastAtMs() {
        return castAtMs;
    }

    public String getFishingLoopStateName() {
        return fishingLoopState.name();
    }

    public long getHudLastUpdatedAtMs() {
        return hudLastUpdatedAtMs;
    }

    public String getHudReason() {
        return hudReason;
    }

    private void setHudReason(String reason) {
        if (reason == null || reason.isEmpty()) {
            return;
        }
        hudReason = reason;
    }

    private Entity resolveCapKillTarget(double rangeBlocks) {
        if (mc.player == null || mc.level == null) {
            capKillTargetEntityId = -1;
            return null;
        }

        Entity locked = findEntityById(capKillTargetEntityId);
        if (isCapKillTargetValid(locked, rangeBlocks)) {
            return locked;
        }

        Entity fallback = findNearestReachableStriderWithinRadius(rangeBlocks);
        capKillTargetEntityId = fallback == null ? -1 : fallback.getId();
        return fallback;
    }

    private Entity findEntityById(int id) {
        if (id < 0 || mc.level == null) {
            return null;
        }
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity != null && entity.getId() == id) {
                return entity;
            }
        }
        return null;
    }

    private boolean isCapKillTargetValid(Entity entity, double radius) {
        if (entity == null || mc.player == null) {
            return false;
        }
        if (!isStriderEntity(entity)) {
            return false;
        }
        if (mc.player.distanceToSqr(entity) > radius * radius) {
            return false;
        }
        return mc.player.hasLineOfSight(entity);
    }

    private boolean updateCombatAim(Entity target, long now) {
        if (mc.player == null || target == null) {
            return false;
        }
        RotationHandler rotationHandler = RotationHandler.getInstance();
        if (combatAimTargetEntityId != target.getId()) {
            combatAimTargetEntityId = target.getId();
            combatAimReadyAtMs = 0L;
            nextKillAimAtMs = 0L;
            if (rotationHandler.isEnabled()) {
                rotationHandler.stop();
            }
        }

        if (rotationHandler.isEnabled() && now >= nextKillAimAtMs && shouldForceCombatReaim(target)) {
            rotationHandler.stop();
        }

        if (!rotationHandler.isEnabled() && (combatAimReadyAtMs == 0L || now >= nextKillAimAtMs)) {
            long rotateMs = randomRange(KILL_AIM_ROTATION_MIN_MS, KILL_AIM_ROTATION_MAX_MS);
            rotationHandler.easeTo(new RotationConfiguration(
                    createCombatAimTarget(target),
                    rotateMs,
                    () -> combatAimReadyAtMs = System.currentTimeMillis() + KILL_AIM_POST_ROTATION_HOLD_MS
            ).easeFunction(RotationConfiguration.Ease.EASE_OUT_CUBIC));
            nextKillAimAtMs = now + KILL_AIM_REAIM_INTERVAL_MS;
            return false;
        }

        if (rotationHandler.isEnabled()) {
            return false;
        }
        if (now < combatAimReadyAtMs) {
            return false;
        }
        return isCombatAimLocked(target);
    }

    private boolean isCombatAimLocked(Entity target) {
        if (mc.player == null || target == null) {
            return false;
        }
        Angle desired = getCombatAimAngle(target);
        float yawError = Math.abs(Mth.wrapDegrees(desired.getYaw() - mc.player.getYRot()));
        float pitchError = Math.abs(desired.getPitch() - mc.player.getXRot());
        return yawError <= KILL_AIM_LOCK_MAX_YAW_ERROR_DEG && pitchError <= KILL_AIM_LOCK_MAX_PITCH_ERROR_DEG;
    }

    private boolean shouldForceCombatReaim(Entity target) {
        if (mc.player == null || target == null) {
            return false;
        }
        Angle desired = getCombatAimAngle(target);
        float yawError = Math.abs(Mth.wrapDegrees(desired.getYaw() - mc.player.getYRot()));
        float pitchError = Math.abs(desired.getPitch() - mc.player.getXRot());
        return yawError > KILL_AIM_FORCE_REAIM_YAW_ERROR_DEG || pitchError > KILL_AIM_FORCE_REAIM_PITCH_ERROR_DEG;
    }

    private Target createCombatAimTarget(Entity target) {
        return new Target(target).additionalY(getCombatAimYOffset(target));
    }

    private Angle getCombatAimAngle(Entity target) {
        return AngleUtil.getRotation(target.position().add(0.0D, getCombatAimYOffset(target), 0.0D));
    }

    private float getCombatAimYOffset(Entity target) {
        if (target == null) {
            return 1.0F;
        }
        float targetHeight = target.getBbHeight();
        return Mth.clamp(targetHeight * 0.70F, 0.90F, 1.35F);
    }

    private boolean isInsideFishingRadius() {
        if (mc.player == null) {
            return true;
        }
        double dx = mc.player.getX() - FISHING_ANCHOR_X;
        double dz = mc.player.getZ() - FISHING_ANCHOR_Z;
        return (dx * dx + dz * dz) <= FISHING_ALLOWED_RADIUS_SQ;
    }

    private void warnOutsideFishingRadius(long now) {
        if (now < nextRadiusWarningAtMs) {
            return;
        }
        nextRadiusWarningAtMs = now + FISHING_RADIUS_WARNING_COOLDOWN_MS;
        log("Outside fishing radius (" + FISHING_ALLOWED_RADIUS_BLOCKS + " blocks). Repositioning to fishing anchor.");
    }

    private long performCapKillRightClick(long now) {
        if (shouldUseSlayerFastSwapEnabled() && now >= nextCapKillSlayerAssistAtMs) {
            String slayerWeapon = VeinForge.config().fishing.galateaFishing.galateaFishingWeapon;
            if (slayerWeapon != null && !slayerWeapon.trim().isEmpty() && InventoryUtil.holdItem(slayerWeapon)) {
                KeyBindUtil.rightClick();
                log("Cap kill: Slayer ability assist cast.");
                nextCapKillSlayerAssistAtMs = now + randomRange(CAP_KILL_SLAYER_ASSIST_MIN_INTERVAL_MS, CAP_KILL_SLAYER_ASSIST_MAX_INTERVAL_MS);
                String axeName = VeinForge.config().fishing.galateaFishing.galateaAxe;
                if (axeName != null && !axeName.trim().isEmpty()) {
                    InventoryUtil.holdItem(axeName);
                }
                return randomRange(CAP_KILL_SLAYER_POST_CAST_WAIT_MIN_MS, CAP_KILL_SLAYER_POST_CAST_WAIT_MAX_MS);
            }
        }

        String axeName = VeinForge.config().fishing.galateaFishing.galateaAxe;
        if (axeName == null || axeName.trim().isEmpty()) {
            return -1L;
        }
        if (!InventoryUtil.holdItem(axeName)) {
            log("Cap kill: axe not found in hotbar: " + axeName);
            return -1L;
        }

        KeyBindUtil.rightClick();
        return randomRange(CAP_KILL_RIGHT_CLICK_MIN_INTERVAL_MS, CAP_KILL_RIGHT_CLICK_MAX_INTERVAL_MS);
    }

    private boolean performCapKillMeleeHit() {
        String axeName = VeinForge.config().fishing.galateaFishing.galateaAxe;
        if (axeName == null || axeName.trim().isEmpty()) {
            return false;
        }
        if (!InventoryUtil.holdItem(axeName)) {
            log("Cap kill: axe not found in hotbar for melee hit: " + axeName);
            return false;
        }

        KeyBindUtil.leftClick();
        return true;
    }

    private boolean isAxeMeleeRange(Entity target) {
        if (mc.player == null || target == null) {
            return false;
        }
        double rangeSq = AXE_MELEE_RANGE_BLOCKS * AXE_MELEE_RANGE_BLOCKS;
        return mc.player.distanceToSqr(target) <= rangeSq;
    }

    private void stopFishingBeforeCombat(long now) {
        String rodName = VeinForge.config().fishing.generalFishing.fishingRod;
        if (FishingUtil.getPlayerFishingHook(mc, HOOK_FALLBACK_MAX_DIST_SQ) != null
                && rodName != null
                && !rodName.trim().isEmpty()
                && InventoryUtil.holdItem(rodName)) {
            KeyBindUtil.rightClick();
        }
        scheduleNextCast(now + getCombatActionDelayMs());
    }

    private void executeStuckCombo(long now, Entity target) {
        if (now < nextStuckAttackAtMs) {
            return;
        }
        if (!isStuckAimLocked(target)) {
            if (stuckAimLockedSinceMs != 0L || stuckLockEstablished) {
                logEvent("LOCK", "state=lost id=" + target.getId());
            }
            stuckAimLockedSinceMs = 0L;
            stuckLockEstablished = false;
            nextStuckAttackAtMs = now + STUCK_AIM_LOCK_RETRY_MS;
            return;
        }
        if (stuckAimLockedSinceMs == 0L) {
            stuckAimLockedSinceMs = now;
            logEvent("LOCK", "state=acquiring id=" + target.getId());
            nextStuckAttackAtMs = now + STUCK_AIM_LOCK_HOLD_MS;
            return;
        }
        if (now - stuckAimLockedSinceMs < STUCK_AIM_LOCK_HOLD_MS) {
            nextStuckAttackAtMs = now + STUCK_AIM_LOCK_RETRY_MS;
            return;
        }
        if (!stuckLockEstablished) {
            stuckLockEstablished = true;
            logEvent("LOCK", "state=ready id=" + target.getId());
        }

        String axeName = VeinForge.config().fishing.galateaFishing.galateaAxe;
        if (axeName == null || axeName.trim().isEmpty()) {
            return;
        }
        boolean useSlayerAbilityStep = shouldUseSlayerAbilityStep();
        if (useSlayerAbilityStep) {
            String slayerWeapon = VeinForge.config().fishing.galateaFishing.galateaFishingWeapon;
            if (slayerWeapon != null && !slayerWeapon.trim().isEmpty() && InventoryUtil.holdItem(slayerWeapon)) {
                // Slayer mode helper cast: trigger ability once, then return to axe combo.
                KeyBindUtil.rightClick();
                logEvent("HIT", "type=slayer_rc id=" + target.getId() + " step=" + stuckComboStep);
            } else if (!InventoryUtil.holdItem(axeName)) {
                return;
            } else {
                KeyBindUtil.rightClick();
                logEvent("HIT", "type=axe_rc id=" + target.getId() + " step=" + stuckComboStep);
            }
        } else {
            if (!InventoryUtil.holdItem(axeName)) {
                return;
            }

            if ((stuckComboStep % 2) == 0) {
                KeyBindUtil.rightClick();
                logEvent("HIT", "type=axe_rc id=" + target.getId() + " step=" + stuckComboStep);
            } else {
                KeyBindUtil.leftClick();
                logEvent("HIT", "type=axe_lc id=" + target.getId() + " step=" + stuckComboStep);
            }
        }

        stuckComboStep++;
        if (stuckComboStep >= STUCK_COMBO_CYCLE_LENGTH) {
            stuckComboStep = 0;
        }

        long interval = getCombatClickIntervalMs();
        nextStuckAttackAtMs = now + interval;
    }

    private boolean isStuckAimLocked(Entity target) {
        if (mc.player == null || target == null) {
            return false;
        }
        Angle desired = getCombatAimAngle(target);
        float yawError = Math.abs(Mth.wrapDegrees(desired.getYaw() - mc.player.getYRot()));
        float pitchError = Math.abs(desired.getPitch() - mc.player.getXRot());
        return yawError <= STUCK_AIM_LOCK_MAX_YAW_ERROR_DEG && pitchError <= STUCK_AIM_LOCK_MAX_PITCH_ERROR_DEG;
    }

    private void reelAndScheduleNextCast(long now, String rodName, boolean rightClick, String reason, long nextCastDelayMs) {
        log("Reeling triggered. reason=" + reason + ", castAgeMs=" + (now - castAtMs));
        logEvent("REEL", "reason=" + reason + " castAgeMs=" + (now - castAtMs) + " nextCastDelayMs=" + Math.max(60L, nextCastDelayMs));
        if (rightClick) {
            if (InventoryUtil.holdItem(rodName)) {
                KeyBindUtil.rightClick();
            } else {
                warnRodMissing(now, rodName);
            }
        }
        scheduleNextCast(now + Math.max(60L, nextCastDelayMs));
    }

    private void scheduleNextCast(long nextCastAtMs) {
        fishingLoopState = FishingLoopState.NEEDS_CAST;
        castAtMs = 0L;
        resetCastAlignmentState();
        nextFishingActionAtMs = nextCastAtMs;
    }

    private void scheduleNoCatchDeadline(long now) {
        noCatchDeadlineAtMs = now + randomRange(NO_CATCH_MIN_MS, NO_CATCH_MAX_MS);
    }

    private int countNearbyBiteArmorMarkers() {
        return FishingUtil.countNearbyArmorStandsContainingName(
                mc,
                BITE_MARKER_ARMOR_NAME,
                BITE_MARKER_SCAN_RADIUS_BLOCKS,
                16.0D
        );
    }

    private long getCastActionDelayMs() {
        int base = Math.max(100, VeinForge.config().fishing.generalFishing.castDelayMs);
        int randomizer = Math.max(0, VeinForge.config().fishing.generalFishing.castDelayRandomizerMs);
        return base + (randomizer > 0 ? random.nextInt(randomizer + 1) : 0);
    }

    private long getCombatActionDelayMs() {
        int base = Math.max(50, VeinForge.config().fishing.galateaFishing.galateaCombatDelayMs);
        int randomizer = Math.max(0, VeinForge.config().fishing.galateaFishing.galateaCombatDelayRandomizerMs);
        return base + (randomizer > 0 ? random.nextInt(randomizer + 1) : 0);
    }

    private long getCombatClickIntervalMs() {
        return randomRange(100L, 200L);
    }

    private boolean shouldUseSlayerFastSwapEnabled() {
        if (!shouldUseSlayerWeaponMode()) {
            return false;
        }
        if (!VeinForge.config().fishing.galateaFishing.galateaFastWeaponSwap) {
            return false;
        }

        String slayerWeapon = VeinForge.config().fishing.galateaFishing.galateaFishingWeapon;
        return slayerWeapon != null && !slayerWeapon.trim().isEmpty();
    }

    private boolean shouldUseSlayerAbilityStep() {
        return shouldUseSlayerFastSwapEnabled() && stuckComboStep == 0;
    }

    private boolean shouldUseSlayerWeaponMode() {
        return VeinForge.config().fishing.galateaFishing.galateaKillMode == 1;
    }

    private int getCapKillStartThreshold(int minStriderCount, int maxStriderCount) {
        return shouldUseSlayerCapKillPattern() ? minStriderCount : maxStriderCount;
    }

    private double getCapKillTargetRangeBlocks() {
        return shouldUseSlayerCapKillPattern() ? CAP_KILL_SLAYER_RANGE_BLOCKS : AXE_MELEE_RANGE_BLOCKS;
    }

    private long getCapKillReentryCooldownMs() {
        return shouldUseSlayerCapKillPattern() ? CAP_KILL_REENTRY_SLAYER_COOLDOWN_MS : CAP_KILL_REENTRY_COOLDOWN_MS;
    }

    private long alignToFishingDirection() {
        return alignToFishingDirection(null);
    }

    private long alignToFishingDirection(Runnable callback) {
        if (mc.player == null) {
            return 0L;
        }
        float targetYaw = FISHING_IDLE_YAW_DEG + randomFloatRange(-FISHING_LOOK_MAX_YAW_JITTER_DEG, FISHING_LOOK_MAX_YAW_JITTER_DEG);
        float targetPitch = FISHING_IDLE_PITCH_DEG + randomFloatRange(-FISHING_LOOK_MAX_PITCH_JITTER_DEG, FISHING_LOOK_MAX_PITCH_JITTER_DEG);
        long easeMs = randomRange(FISHING_LOOK_EASE_MIN_MS, FISHING_LOOK_EASE_MAX_MS);

        RotationHandler.getInstance().easeTo(new RotationConfiguration(
                new Angle(targetYaw, targetPitch),
                easeMs,
                callback
        ));
        return easeMs;
    }

    private void resetCastAlignmentState() {
        castAlignInProgress = false;
        castAlignCompleted = false;
        castAlignDeadlineAtMs = 0L;
    }

    private boolean shouldUseSlayerCapKillPattern() {
        return shouldUseSlayerFastSwapEnabled();
    }

    private long randomRange(long min, long max) {
        if (max <= min) {
            return min;
        }
        long bound = (max - min) + 1L;
        return min + (Math.abs(random.nextLong()) % bound);
    }

    private float randomFloatRange(float min, float max) {
        if (max <= min) {
            return min;
        }
        return min + random.nextFloat() * (max - min);
    }

    private Set<Integer> collectNearbyStriderIds(int scanRadius) {
        Set<Integer> ids = new HashSet<>();
        if (mc.player == null || mc.level == null) {
            return ids;
        }

        double radiusSq = (double) scanRadius * (double) scanRadius;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!isStriderEntity(entity)) {
                continue;
            }
            if (mc.player.distanceToSqr(entity) <= radiusSq) {
                ids.add(entity.getId());
            }
        }
        return ids;
    }

    private void logStriderScannerDelta(Set<Integer> currentNearby, int nearbyCount) {
        if (trackedNearbyStriderIds.isEmpty() && currentNearby.isEmpty()) {
            return;
        }

        Set<Integer> added = new HashSet<>(currentNearby);
        added.removeAll(trackedNearbyStriderIds);
        if (!added.isEmpty()) {
            log("Strider scanner: +" + added.size() + " added nearby. total=" + nearbyCount + ", trigger=" + striderTriggerCount);
        }

        Set<Integer> removed = new HashSet<>(trackedNearbyStriderIds);
        removed.removeAll(currentNearby);
        if (!removed.isEmpty()) {
            log("Strider scanner: -" + removed.size() + " removed nearby. total=" + nearbyCount + ", trigger=" + striderTriggerCount);
        }

        trackedNearbyStriderIds.clear();
        trackedNearbyStriderIds.addAll(currentNearby);
    }

    private Entity findNearestStriderWithinRadius(double radius) {
        if (mc.player == null || mc.level == null) {
            return null;
        }

        double radiusSq = radius * radius;
        Entity best = null;
        double bestDistSq = Double.MAX_VALUE;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!isStriderEntity(entity)) {
                continue;
            }

            double distSq = mc.player.distanceToSqr(entity);
            if (distSq > radiusSq) {
                continue;
            }

            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                best = entity;
            }
        }

        return best;
    }

    private Entity resolveActiveStuckTarget(Entity closeStrider) {
        if (mc.player == null || mc.level == null) {
            return null;
        }
        if (inStuckCombat && stuckTargetEntityId >= 0) {
            Entity locked = mc.level.getEntity(stuckTargetEntityId);
            if (locked != null && locked.isAlive() && isStriderEntity(locked)) {
                return locked;
            }
        }
        return closeStrider;
    }

    private Entity findNearestReachableStriderWithinRadius(double radius) {
        if (mc.player == null || mc.level == null) {
            return null;
        }

        double radiusSq = radius * radius;
        Entity best = null;
        double bestDistSq = Double.MAX_VALUE;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!isStriderEntity(entity)) {
                continue;
            }

            double distSq = mc.player.distanceToSqr(entity);
            if (distSq > radiusSq) {
                continue;
            }
            if (!mc.player.hasLineOfSight(entity)) {
                continue;
            }

            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                best = entity;
            }
        }

        return best;
    }

    private int countReachableStridersWithinRadius(double radius) {
        if (mc.player == null || mc.level == null) {
            return 0;
        }

        int count = 0;
        double radiusSq = radius * radius;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!isStriderEntity(entity)) {
                continue;
            }
            if (mc.player.distanceToSqr(entity) > radiusSq) {
                continue;
            }
            if (!mc.player.hasLineOfSight(entity)) {
                continue;
            }
            count++;
        }
        return count;
    }

    private boolean isStriderEntity(Entity entity) {
        if (entity == null || !entity.isAlive() || entity == mc.player) {
            return false;
        }
        String cleanedName = FishingUtil.cleanEntityName(entity);
        if (cleanedName.isEmpty()) {
            return false;
        }
        return cleanedName.toLowerCase(Locale.ROOT).contains("strider");
    }

    private void pickNextTriggerCount() {
        int minStriderCount = Math.min(
                VeinForge.config().fishing.galateaFishing.galateaStriderMinCount,
                VeinForge.config().fishing.galateaFishing.galateaStriderMaxCount
        );
        int maxStriderCount = Math.max(
                VeinForge.config().fishing.galateaFishing.galateaStriderMinCount,
                VeinForge.config().fishing.galateaFishing.galateaStriderMaxCount
        );
        int range = (maxStriderCount - minStriderCount) + 1;
        striderTriggerCount = minStriderCount + random.nextInt(Math.max(1, range));
    }

    private void maybeLogStatus(int nearbyStriders, int minStriderCount, int maxStriderCount) {
        long now = System.currentTimeMillis();
        if (now < nextStatusLogAtMs) {
            return;
        }
        nextStatusLogAtMs = now + STATUS_LOG_INTERVAL_MS;
        if (nearbyStriders >= maxStriderCount) {
            log(
                    "Strider cap reached. Nearby=" + nearbyStriders +
                            ", trigger=" + striderTriggerCount +
                            ", range=[" + minStriderCount + ", " + maxStriderCount + "]" +
                            ". Waiting for reachable melee targets."
            );
            return;
        }
        log(
                "Waiting for Striders. Nearby=" + nearbyStriders +
                        ", trigger=" + striderTriggerCount +
                        ", range=[" + minStriderCount + ", " + maxStriderCount + "]"
        );
    }

    private void warnRodMissing(long now, String rodName) {
        if (now < nextRodWarningAtMs) {
            return;
        }
        nextRodWarningAtMs = now + ROD_WARNING_COOLDOWN_MS;
        log("Fishing rod not found in hotbar: " + rodName);
    }

    private void logEvent(String tag, String details) {
        log("[FDBG|" + tag + "] " + details);
    }

    private void stopAutoMobKillerIfRunning() {
        if (AutoMobKiller.getInstance().isRunning()) {
            AutoMobKiller.getInstance().stop();
        }
    }

    private enum FishingLoopState {
        NEEDS_CAST,
        WAITING_FOR_BITE
    }
}
