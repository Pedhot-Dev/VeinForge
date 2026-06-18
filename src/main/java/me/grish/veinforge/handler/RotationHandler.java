package me.grish.veinforge.handler;

import lombok.Getter;
import me.grish.veinforge.VeinForge;
import me.grish.veinforge.event.MotionUpdateEvent;
import me.grish.veinforge.util.AngleUtil;
import me.grish.veinforge.util.helper.Angle;
import me.grish.veinforge.util.helper.RotationConfiguration;
import me.grish.veinforge.util.helper.Target;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class RotationHandler {

    private static final Logger log = LoggerFactory.getLogger(RotationHandler.class);
    private static final float MAX_YAW_SPEED_DEG_PER_SEC = 240f;
    private static final float MAX_PITCH_SPEED_DEG_PER_SEC = 180f;
    private static final float NEAR_SLOWDOWN_DEG = 10f;
    private static final float MIN_SPEED_FACTOR = 0.35f;
    private static final float OVERSHOOT_CHANCE = 0.05f;
    private static final float OVERSHOOT_YAW_MIN = 0.4f;
    private static final float OVERSHOOT_YAW_MAX = 0.9f;
    private static final float OVERSHOOT_PITCH_MIN = 0.2f;
    private static final float OVERSHOOT_PITCH_MAX = 0.8f;
    private static RotationHandler instance;
    private final Queue<RotationConfiguration> rotations = new LinkedList<>();
    private final Minecraft mc = Minecraft.getInstance();
    private final Angle startRotation = new Angle(0f, 0f);
    private final Random random = new Random();
    private boolean enabled;
    private long startTime;
    private long endTime;
    private Target target = new Target(new Angle(0, 0));
    private float lastBezierYaw = 0;
    private float lastBezierPitch = 0;
    private float serverSideYaw = 0;
    private float serverSidePitch = 0;
    private int randomMultiplier1 = 1;
    private int randomMultiplier2 = 1;
    private long lastUpdateMs;
    private boolean overshootEnabled;
    private float overshootYaw;
    private float overshootPitch;
    private boolean followingTarget = false;
    private boolean stopRequested = false;

    @Getter
    private RotationConfiguration configuration;

    public static RotationHandler getInstance() {
        if (instance == null) {
            instance = new RotationHandler();
        }
        return instance;
    }

    public RotationHandler queueRotation(RotationConfiguration... configs) {
        this.rotations.addAll(Arrays.asList(configs));
        return instance;
    }

    public void start() {
        if (this.rotations.isEmpty() || this.enabled) {
            return;
        }
        this.easeTo(rotations.poll());
    }

    public void easeTo(RotationConfiguration configuration) {
        this.configuration = configuration;
        this.startTime = System.currentTimeMillis();
        this.lastUpdateMs = this.startTime;
        this.startRotation.setRotation(configuration.from().orElse(AngleUtil.getPlayerAngle()));
        this.target = configuration.target().get();

        Angle change = AngleUtil.getNeededChange(this.startRotation, this.target.getTargetAngle());
        this.endTime = this.startTime + getTime(pythagoras(change.getYaw(), change.getPitch()), configuration.time());

        this.randomMultiplier1 = randomMultiplier2 = random.nextBoolean() ? VeinForge.config().debug.rotationCurve : -VeinForge.config().debug.rotationCurve;

        this.lastBezierYaw = 0;
        this.lastBezierPitch = 0;

        initOvershoot(change);

        if (configuration.rotationType() == RotationConfiguration.RotationType.SERVER) {
            if (serverSideYaw == 0 && serverSidePitch == 0) {
                serverSideYaw = mc.player.getYRot();
                serverSidePitch = mc.player.getXRot();
            } else {
                this.startRotation.setYaw(AngleUtil.get360RotationYaw(serverSideYaw));
                this.startRotation.setPitch(serverSidePitch);
            }
        }

        this.stopRequested = false;
        this.enabled = true;
    }

    private void reset() {
        if (this.stopRequested) {
            this.configuration = null;
            this.target = null;
            this.startTime = this.endTime = 0L;
            this.serverSideYaw = this.serverSidePitch = this.lastBezierYaw = this.lastBezierPitch = 0;
        }
        this.enabled = false;
        this.followingTarget = false;
        this.stopRequested = false;
    }

    //so that we can stop rotation from anywhere, not JUST a tick (crashes if you call it from other events that aren't synchronized properly)
    public void stop() {
        this.rotations.clear();
        this.stopRequested = true;
        this.enabled = false;
    }

    public void onTick() {
        // Called from EventManager
    }

    // Called manually from EventManager now (needs to be added there)
    public void onWorldRender(LevelRenderContext context) {
        if (!enabled || this.configuration == null || this.configuration.rotationType() != RotationConfiguration.RotationType.CLIENT) {
            return;
        }

        Angle bezierAngle = getBezierAngle();

        Angle desiredDelta = new Angle(bezierAngle.getYaw() - lastBezierYaw, bezierAngle.getPitch() - lastBezierPitch);
        Angle appliedDelta = applyHumanizedDelta(desiredDelta, mc.player.getYRot(), mc.player.getXRot());

        mc.player.setYRot(mc.player.getYRot() + appliedDelta.getYaw());
        mc.player.setXRot(mc.player.getXRot() + appliedDelta.getPitch());

        lastBezierYaw += appliedDelta.getYaw();
        lastBezierPitch += appliedDelta.getPitch();
        if (System.currentTimeMillis() > this.endTime || this.stopRequested) {
            handleRotationEnd();
        }
    }

    // EventManager must call this when MotionUpdateEvent is fired (if it exists)
    public void onMotionUpdate(MotionUpdateEvent event) {
        if (!enabled || this.configuration == null || this.configuration.rotationType() != RotationConfiguration.RotationType.SERVER) {
            return;
        }

        Angle bezierAngle = getBezierAngle();

        Angle desiredDelta = new Angle(bezierAngle.getYaw() - lastBezierYaw, bezierAngle.getPitch() - lastBezierPitch);
        Angle appliedDelta = applyHumanizedDelta(desiredDelta, serverSideYaw, serverSidePitch);

        serverSideYaw += appliedDelta.getYaw();
        serverSidePitch += appliedDelta.getPitch();
        event.yaw = serverSideYaw;
        event.pitch = serverSidePitch;

        lastBezierYaw += appliedDelta.getYaw();
        lastBezierPitch += appliedDelta.getPitch();
        if (System.currentTimeMillis() > this.endTime || this.stopRequested) {
            handleRotationEnd();
        }
    }

    private Angle getBezierAngle() {
        float totalTime = (float) (this.endTime - this.startTime);
        float timeProgress = Math.min(totalTime, System.currentTimeMillis() - this.startTime) / totalTime;
        float rotationProgress = configuration.easeFunction().invoke(timeProgress);

        Angle bezierEnd = AngleUtil.getNeededChange(this.startRotation, this.target.getTargetAngle());
        Angle control1 = new Angle(bezierEnd.getYaw() * 0.05f * this.randomMultiplier1, bezierEnd.getYaw() * 0.1f * this.randomMultiplier2);
        Angle control2 = new Angle(bezierEnd.getYaw() - bezierEnd.getYaw() * 0.05f * this.randomMultiplier2, bezierEnd.getPitch() - bezierEnd.getYaw() * 0.1f * this.randomMultiplier1);

        double bezierYawSoFar = bezier(rotationProgress, control1.getYaw(), control2.getYaw(), bezierEnd.getYaw());
        double bezierPitchSoFar = bezier(rotationProgress, control1.getPitch(), control2.getPitch(), bezierEnd.getPitch());

        if (overshootEnabled) {
            double wobble = Math.sin(Math.PI * rotationProgress);
            bezierYawSoFar += wobble * overshootYaw;
            bezierPitchSoFar += wobble * overshootPitch;
        }
        return new Angle((float) bezierYawSoFar, (float) bezierPitchSoFar);
    }

    private double bezier(float t, float c1, float c2, float end) {
        return 3 * Math.pow((1 - t), 2) * t * c1 + 3 * (1 - t) * Math.pow(t, 2) * c2 + Math.pow(t, 3) * end;
    }

    private void handleRotationEnd() {
        if (!this.stopRequested) {
            if (this.configuration.followTarget()) {
                System.out.println("Following Target");
                this.easeTo(configuration);
                this.followingTarget = true;
                return;
            }

            configuration.callback().ifPresent(Runnable::run);

            if (!this.rotations.isEmpty()) {
                this.easeTo(this.rotations.poll());
                return;
            }

            if (this.configuration.rotationType() == RotationConfiguration.RotationType.SERVER && this.configuration.easeBackToClientSide()) {
                RotationConfiguration newConf = new RotationConfiguration(AngleUtil.getPlayerAngle(), this.configuration.time(),
                        RotationConfiguration.RotationType.SERVER, () -> {
                });
                this.easeTo(newConf);
                return;
            }
        }
        this.reset();
    }

    private Angle applyHumanizedDelta(Angle desiredDelta, float currentYaw, float currentPitch) {
        long now = System.currentTimeMillis();
        float dtSec = (now - lastUpdateMs) / 1000f;
        dtSec = Math.min(0.05f, Math.max(0.001f, dtSec));
        lastUpdateMs = now;

        Angle remaining = AngleUtil.getNeededChange(new Angle(currentYaw, currentPitch), target.getTargetAngle());
        float remainingMag = (float) pythagoras(remaining.getYaw(), remaining.getPitch());
        float slowdownFactor = 1.0f;
        if (remainingMag < NEAR_SLOWDOWN_DEG) {
            slowdownFactor = Math.max(MIN_SPEED_FACTOR, remainingMag / NEAR_SLOWDOWN_DEG);
        }

        float maxYawDelta = MAX_YAW_SPEED_DEG_PER_SEC * dtSec * slowdownFactor;
        float maxPitchDelta = MAX_PITCH_SPEED_DEG_PER_SEC * dtSec * slowdownFactor;

        float clampedYaw = Mth.clamp(desiredDelta.getYaw(), -maxYawDelta, maxYawDelta);
        float clampedPitch = Mth.clamp(desiredDelta.getPitch(), -maxPitchDelta, maxPitchDelta);
        return new Angle(clampedYaw, clampedPitch);
    }

    private void initOvershoot(Angle change) {
        overshootEnabled = false;
        overshootYaw = 0f;
        overshootPitch = 0f;

        if (!VeinForge.config().general.randomizedRotations) {
            return;
        }
        if (random.nextFloat() > OVERSHOOT_CHANCE) {
            return;
        }
        if (Math.abs(change.getYaw()) < 8f && Math.abs(change.getPitch()) < 6f) {
            return;
        }

        overshootEnabled = true;
        overshootYaw = signedRandomBetween(change.getYaw(), OVERSHOOT_YAW_MIN, OVERSHOOT_YAW_MAX);
        overshootPitch = signedRandomBetween(change.getPitch(), OVERSHOOT_PITCH_MIN, OVERSHOOT_PITCH_MAX);
    }

    private float signedRandomBetween(float change, float min, float max) {
        float sign = change >= 0 ? 1f : -1f;
        return sign * (min + (max - min) * random.nextFloat());
    }

    private double pythagoras(float yaw, float pitch) {
        return Math.sqrt(yaw * yaw + pitch * pitch);
    }

    private long getTime(double pythagoras, long time) {
        if (time <= 0) {
            return 1;
        }
        if (pythagoras < 25) {
            return (long) (time * 0.65);
        }
        if (pythagoras < 45) {
            return (long) (time * 0.77);
        }
        if (pythagoras < 80) {
            return (long) (time * 0.9);
        }
        if (pythagoras > 100) {
            return (long) (time * 1.1);
        }
        return (long) (time * 1.0);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public boolean isFollowingTarget() {
        return this.followingTarget;
    }

    public void stopFollowingTarget() {
        if (this.configuration != null) {
            this.configuration.followTarget(false);
        }
    }
}
