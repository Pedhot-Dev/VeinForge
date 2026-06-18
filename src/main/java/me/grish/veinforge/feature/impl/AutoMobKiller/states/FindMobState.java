package me.grish.veinforge.feature.impl.AutoMobKiller.states;

import me.grish.veinforge.VeinForge;
import me.grish.veinforge.feature.impl.AutoMobKiller.AutoMobKiller;
import me.grish.veinforge.feature.impl.Pathfinder;
import me.grish.veinforge.util.AngleUtil;
import me.grish.veinforge.util.BlockUtil;
import me.grish.veinforge.util.EntityUtil;
import me.grish.veinforge.util.helper.Clock;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class FindMobState implements AutoMobKillerState {

    private static final long SEARCH_ANCHOR_DELAY_MS = 2_500L;
    private static final long SEARCH_ANCHOR_REPATH_MS = 1_250L;

    private final Minecraft mc = Minecraft.getInstance();
    private final Clock noMobLogTimer = new Clock();
    private final Clock noMobAnchorTimer = new Clock();
    private final Clock anchorRepathTimer = new Clock();

    @Override
    public void onStart(AutoMobKiller mobKiller) {
        log("Entering Find Mob State");
        noMobLogTimer.reset();
        noMobAnchorTimer.reset();
        anchorRepathTimer.reset();
    }

    @Override
    public AutoMobKillerState onTick(AutoMobKiller mobKiller) {

        LivingEntity mob = findBestMob(mobKiller);

        if (mob == null) {
            if (!noMobLogTimer.isScheduled()) {
                noMobLogTimer.schedule(2_000);
            }

            if (noMobLogTimer.passed()) {
                log("No mobs found yet, continuing to search...");
                noMobLogTimer.schedule(2_000);
            }

            if (!noMobAnchorTimer.isScheduled()) {
                noMobAnchorTimer.schedule(SEARCH_ANCHOR_DELAY_MS);
            }

            if (noMobAnchorTimer.passed()) {
                moveTowardSearchAnchor(mobKiller);
            }

            mobKiller.setError(AutoMobKiller.MKError.NO_ENTITIES);
            return this;
        }

        noMobAnchorTimer.reset();
        anchorRepathTimer.reset();
        mobKiller.setError(AutoMobKiller.MKError.NONE);
        mobKiller.updateTargetMob(mob);
        mobKiller.setTargetMobOriginalPos(mob.position());
        return new PathfindingState();
    }

    private LivingEntity findBestMob(AutoMobKiller mobKiller) {
        if (mc.level == null || mc.player == null) return null;

        AutoMobKiller.SlayerProfile slayerProfile = mobKiller.getSlayerProfile();
        boolean nearestOnly = slayerProfile == AutoMobKiller.SlayerProfile.GOBLIN;
        double playerCrowdingRadiusSq = slayerProfile == AutoMobKiller.SlayerProfile.GENERIC ? 16.0 : 2.25;

        List<LivingEntity> mobs = EntityUtil.getEntities(mobKiller.getMobsToKill(), mobKiller.getBlacklistedMobs());
        if (mobs.isEmpty()) {
            return null;
        }

        float normalizedYaw = AngleUtil.normalizeAngle(mc.player.getYRot());
        float distanceCostWeight = (float) VeinForge.config().debug.mobKillerDistCost / 100f;
        float rotationCostWeight = (float) VeinForge.config().debug.mobKillerRotCost / 100f;
        double bestScore = Double.MAX_VALUE;
        LivingEntity bestMob = null;

        for (LivingEntity mob : mobs) {
            if (mob == null || !mob.isAlive()) continue;
            double distanceSq = mc.player.distanceToSqr(mob);
            if (distanceSq >= (42 * 42)) continue;
            if (!BlockUtil.canStandOn(EntityUtil.getBlockStandingOn(mob))) continue;
            if (!slayerProfile.isTargetInPreferredZone(mob)) continue;
            if (slayerProfile == AutoMobKiller.SlayerProfile.GLACITE && !mc.player.hasLineOfSight(mob)) continue;
            if (!nearestOnly && isCrowdedByOtherPlayer(mob, playerCrowdingRadiusSq)) continue;

            double distanceCost = Math.sqrt(distanceSq);
            double angleCost = Math.abs(AngleUtil.getNeededYawChange(normalizedYaw, AngleUtil.getRotationYaw(mob.position())));
            double score = nearestOnly
                    ? distanceSq
                    : distanceCost * distanceCostWeight + angleCost * rotationCostWeight;
            if (score < bestScore) {
                bestScore = score;
                bestMob = mob;
            }
        }

        return bestMob;
    }


    @Override
    public void onEnd(AutoMobKiller mobKiller) {
        log("Exiting Find Mob State");
    }

    private void moveTowardSearchAnchor(AutoMobKiller mobKiller) {
        AutoMobKiller.SlayerProfile profile = mobKiller.getSlayerProfile();
        if (!profile.hasAnchorPoint() || mc.player == null) {
            return;
        }

        BlockPos anchor = profile.getAnchorPoint();
        if (mc.player.blockPosition().distSqr(anchor) <= 25.0) {
            return;
        }

        if (anchorRepathTimer.isScheduled() && !anchorRepathTimer.passed()) {
            return;
        }

        Pathfinder pathfinder = Pathfinder.getInstance();
        pathfinder.setSprintState(VeinForge.config().commission.dwarvenCommission.mobKillerSprint);
        pathfinder.setInterpolationState(VeinForge.config().commission.dwarvenCommission.mobKillerInterpolate);
        pathfinder.stopAndRequeue(anchor);
        if (!pathfinder.isRunning()) {
            pathfinder.start();
        }

        anchorRepathTimer.schedule(SEARCH_ANCHOR_REPATH_MS);
    }

    private boolean isCrowdedByOtherPlayer(LivingEntity mob, double crowdRadiusSq) {
        for (Player player : mc.level.players()) {
            if (player == null || player == mc.player) continue;
            if (EntityUtil.isNpc(player)) continue;
            if (player.distanceToSqr(mob) < crowdRadiusSq) {
                return true;
            }
        }
        return false;
    }

}
