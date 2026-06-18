package me.grish.veinforge.feature;

import me.grish.veinforge.failsafe.AbstractFailsafe.Failsafe;
import me.grish.veinforge.feature.impl.*;
import me.grish.veinforge.feature.impl.AutoDrillRefuel.AutoDrillRefuel;
import me.grish.veinforge.feature.impl.AutoGetStats.AutoGetStats;
import me.grish.veinforge.feature.impl.AutoMobKiller.AutoMobKiller;
import me.grish.veinforge.feature.impl.BlockMiner.BlockMiner;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class FeatureManager {

    private static FeatureManager instance;
    public final Set<AbstractFeature> allFeatures = new LinkedHashSet<>();

    public FeatureManager() {
        this.allFeatures.addAll(Arrays.asList(
                AutoCommissionClaim.getInstance(),
                AutoGetStats.getInstance(),
                AutoMobKiller.getInstance(),
                AutoWarp.getInstance(),
                BlockMiner.getInstance(),
                CommissionDebugMode.getInstance(),
                MouseUngrab.getInstance(),
                Pathfinder.getInstance(),
                RouteBuilder.getInstance(),
                RouteNavigator.getInstance(),
                AutoDrillRefuel.getInstance(),
                AutoChestUnlocker.instance,
                WorldScanner.getInstance(),
                AutoSell.getInstance(),
                Rift.getInstance()
        ));
    }

    public static FeatureManager getInstance() {
        if (instance == null) {
            instance = new FeatureManager();
        }
        return instance;
    }

    public void enableAll() {
        this.allFeatures.forEach(it -> {
            if (it.shouldStartAtLaunch()) {
                it.start();
            }
        });
    }

    public void disableAll() {
        this.allFeatures.forEach(it -> {
            if (it.isRunning()) {
                it.stop();
            }
        });
    }

    public void pauseAll() {
        this.allFeatures.forEach(it -> {
            if (it.isRunning()) {
                it.pause();
            }
        });
    }

    public void resumeAll() {
        this.allFeatures.forEach(it -> {
            if (it.isRunning()) {
                it.resume();
            }
        });
    }

    public boolean shouldNotCheckForFailsafe() {
        return this.allFeatures.stream().filter(AbstractFeature::isRunning).anyMatch(AbstractFeature::shouldNotCheckForFailsafe);
    }

    public Set<Failsafe> getFailsafesToIgnore() {
        Set<Failsafe> failsafes = new HashSet<>();
        this.allFeatures.forEach(it -> {
            if (it.isRunning()) {
                failsafes.addAll(it.getFailsafesToIgnore());
            }
        });
        return failsafes;
    }
}
