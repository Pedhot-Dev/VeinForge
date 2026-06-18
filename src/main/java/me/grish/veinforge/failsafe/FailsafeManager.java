package me.grish.veinforge.failsafe;

import me.grish.veinforge.VeinForge;
import me.grish.veinforge.event.BlockChangeEvent;
import me.grish.veinforge.failsafe.AbstractFailsafe.Failsafe;
import me.grish.veinforge.failsafe.impl.*;
import me.grish.veinforge.feature.FeatureManager;
import me.grish.veinforge.macro.MacroManager;
import me.grish.veinforge.util.Logger;
import me.grish.veinforge.util.StrafeUtil;
import me.grish.veinforge.util.helper.AudioManager;
import me.grish.veinforge.util.helper.Clock;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.protocol.Packet;

import java.util.*;

public class FailsafeManager {

    private static FailsafeManager instance;
    public final List<AbstractFailsafe> failsafes = new ArrayList<>();
    public final Queue<AbstractFailsafe> emergencyQueue = new PriorityQueue<>(Comparator.comparing(AbstractFailsafe::getPriority));
    private final Clock timer = new Clock();
    public Optional<AbstractFailsafe> triggeredFailsafe = Optional.empty();
    public Set<Failsafe> failsafesToIgnore = new HashSet<>();

    // TODO: Implement all failsafe later!
    public FailsafeManager() {
        this.failsafes.addAll(Arrays.asList(
                DisconnectFailsafe.getInstance(),
                KnockbackFailsafe.getInstance(),
                WorldChangeFailsafe.getInstance(),
                ProfileFailsafe.getInstance(),
                ItemChangeFailsafe.getInstance(),
                NameMentionFailsafe.getInstance()
        ));
    }

    public static FailsafeManager getInstance() {
        if (instance == null) {
            instance = new FailsafeManager();
        }
        return instance;
    }

    public void stopFailsafes() {
        triggeredFailsafe = Optional.empty();
        emergencyQueue.clear();
    }

    public boolean shouldNotCheckForFailsafe() {
        return !MacroManager.getInstance().isRunning() || this.triggeredFailsafe.isPresent();
    }

    public void onTick() {
        if (this.shouldNotCheckForFailsafe()) {
            return;
        }

        List<AbstractFailsafe> failsafeCopy = new ArrayList<>(failsafes);
        this.failsafesToIgnore.clear();
        this.failsafesToIgnore.addAll(FeatureManager.getInstance().getFailsafesToIgnore());

        for (AbstractFailsafe failsafe : failsafeCopy) {
            if (!this.failsafesToIgnore.contains(failsafe.getFailsafeType()) && failsafe.onTick()) {
                this.emergencyQueue.add(failsafe);
            }
        }

        this.onTickChooseEmergency();
        this.onTickReact();
    }

    public void onBlockChange(BlockChangeEvent event) {
        if (this.shouldNotCheckForFailsafe()) {
            return;
        }

        failsafes.forEach(failsafe -> {
            if (!this.failsafesToIgnore.contains(failsafe.getFailsafeType()) && failsafe.onBlockChange(event)) {
                this.emergencyQueue.add(failsafe);
            }
        });
    }

    public void onPacketReceive(Packet<?> packet) {
        if (this.shouldNotCheckForFailsafe()) {
            return;
        }

        failsafes.forEach(failsafe -> {
            if (!this.failsafesToIgnore.contains(failsafe.getFailsafeType()) && failsafe.onPacketReceive(packet)) {
                this.emergencyQueue.add(failsafe);
            }
        });
    }

    public void onChat(String message) {
        if (this.shouldNotCheckForFailsafe()) {
            return;
        }

        failsafes.forEach(failsafe -> {
            if (!this.failsafesToIgnore.contains(failsafe.getFailsafeType()) && failsafe.onChat(message)) {
                this.emergencyQueue.add(failsafe);
            }
        });
    }

    public void onWorldUnload() {
        if (this.shouldNotCheckForFailsafe()) {
            return;
        }

        failsafes.forEach(failsafe -> {
            if (!this.failsafesToIgnore.contains(failsafe.getFailsafeType()) && failsafe.onWorldUnload()) {
                this.emergencyQueue.add(failsafe);
            }
        });
    }

    public void onDisconnect() {
        if (this.shouldNotCheckForFailsafe()) {
            return;
        }

        failsafes.forEach(failsafe -> {
            if (!this.failsafesToIgnore.contains(failsafe.getFailsafeType()) && failsafe.onDisconnect()) {
                this.emergencyQueue.add(failsafe);
            }
        });
    }

    public void onScreenOpen(Screen screen) {
        if (this.shouldNotCheckForFailsafe()) {
            return;
        }

        for (AbstractFailsafe failsafe : this.failsafes) {
            if (!this.failsafesToIgnore.contains(failsafe.getFailsafeType()) && failsafe.onScreenOpen(screen)) {
                this.emergencyQueue.add(failsafe);
            }
        }
    }

    public void removeFailsafeFromQueue(AbstractFailsafe failsafe) {
        boolean removed = emergencyQueue.remove(failsafe);
        if (removed) {
            System.out.println("Successfully removed failsafe: " + failsafe.getFailsafeType());
        } else {
            System.out.println("Failsafe not found in the queue: " + failsafe.getFailsafeType());
        }
    }

    private void onTickChooseEmergency() {
        if (this.shouldNotCheckForFailsafe()) {
            return;
        }
        if (this.triggeredFailsafe.isPresent()) {
            return;
        }
        if (this.emergencyQueue.isEmpty()) {
            return;
        }

        StrafeUtil.forceStop = true;
        if (!this.timer.isScheduled()) {
            this.timer.schedule(VeinForge.config().failsafe.failsafeToggleDelay);
        } else if (this.timer.passed()) {
            AudioManager.getInstance().playSound();
            this.triggeredFailsafe = Optional.ofNullable(this.emergencyQueue.peek());
            this.emergencyQueue.clear();
            this.timer.reset();
        }
    }

    private void onTickReact() {
        if (!this.triggeredFailsafe.isPresent()) {
            return;
        }

        if (this.triggeredFailsafe.get().react()) {
            StrafeUtil.forceStop = false;
            this.triggeredFailsafe = Optional.empty();
            this.emergencyQueue.clear();
            this.failsafes.forEach(AbstractFailsafe::resetStates);
        }
    }

    public boolean isFailsafeActive(Failsafe failsafe) {
        Logger.sendLog("failsafequeue: " + this.emergencyQueue);
        return this.emergencyQueue.stream().anyMatch(it -> it.getFailsafeType().equals(failsafe));
    }
}
