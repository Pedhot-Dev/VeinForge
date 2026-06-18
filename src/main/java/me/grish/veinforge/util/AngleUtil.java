package me.grish.veinforge.util;

import me.grish.veinforge.VeinForge;
import me.grish.veinforge.util.helper.Angle;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class AngleUtil {

    private static final Minecraft mc = Minecraft.getInstance();
    private static final double randomAddition = (Math.random() * 0.3 - 0.15);

    public static Angle getPlayerAngle() {
        if (mc.player == null) return new Angle(0, 0);
        return new Angle(get360RotationYaw(), mc.player.getXRot());
    }

    public static float get360RotationYaw(float yaw) {
        return (yaw % 360 + 360) % 360;
    }

    // This is MathHelper::wrapAngleTo180_float
    public static float normalizeAngle(float yaw) {
        return Mth.wrapDegrees(yaw);
    }

    public static float get360RotationYaw() {
        if (mc.player == null) {
            return 0;
        }
        return get360RotationYaw(mc.player.getYRot());
    }

    public static float clockwiseDifference(float initialYaw360, float targetYaw360) {
        return get360RotationYaw(targetYaw360 - initialYaw360);
    }

    public static float antiClockwiseDifference(float initialYaw360, float targetYaw360) {
        return get360RotationYaw(initialYaw360 - targetYaw360);
    }

    public static float smallestAngleDifference(float initialYaw360, float targetYaw360) {
        return Math.min(clockwiseDifference(initialYaw360, targetYaw360), antiClockwiseDifference(initialYaw360, targetYaw360));
    }

    public static Vec3 getVectorForRotation(float pitch, float yaw) {
        float f = Mth.cos(-yaw * 0.017453292F - 3.1415927F);
        float f1 = Mth.sin(-yaw * 0.017453292F - 3.1415927F);
        float f2 = -Mth.cos(-pitch * 0.017453292F);
        float f3 = Mth.sin(-pitch * 0.017453292F);
        return new Vec3(f1 * f2, f3, f * f2);
    }

    public static Vec3 getVectorForRotation(float yaw) {
        return new Vec3(-Mth.sin(-yaw * 0.017453292F - 3.1415927F), 0, -Mth.cos(-yaw * 0.017453292F - 3.1415927F));
    }

    public static Angle getRotation(Vec3 to) {
        if (mc.player == null) return new Angle(0, 0);
        return getRotation(mc.player.getEyePosition(), to);
    }

    public static Angle getRotation(Entity to) {
        if (mc.player == null) return new Angle(0, 0);
        return getRotation(mc.player.getEyePosition(),
                to.position().add(0, Math.min(((to.getBbHeight() * 0.85) + randomAddition), 1.7), 0));
    }

    public static Angle getRotation(BlockPos pos) {
        if (mc.player == null) return new Angle(0, 0);
        return getRotation(mc.player.getEyePosition(), Vec3.atCenterOf(pos));
    }

    public static Angle getRotation(Vec3 from, BlockPos pos) {
        return getRotation(from, Vec3.atCenterOf(pos));
    }

    public static Angle getRotation(Vec3 from, Vec3 to) {
        double xDiff = to.x - from.x;
        double yDiff = to.y - from.y;
        double zDiff = to.z - from.z;

        double dist = Math.sqrt(xDiff * xDiff + zDiff * zDiff);

        float yaw = (float) Math.toDegrees(Math.atan2(zDiff, xDiff)) - 90F;
        float pitch = (float) -Math.toDegrees(Math.atan2(yDiff, dist));

        return new Angle(yaw, pitch);
    }

    public static float getRotationYaw(Vec3 to) {
        if (mc.player == null) return 0;
        return (float) -Math.toDegrees(Math.atan2(to.x - mc.player.getX(), to.z - mc.player.getZ()));
    }

    public static float getRotationYaw(Vec3 from, Vec3 to) {
        return (float) -Math.toDegrees(Math.atan2(to.x - from.x, to.z - from.z));
    }

    public static float getRotationYaw360(Vec3 to) {
        if (mc.player == null) return 0;
        float yaw = (float) -Math.toDegrees(Math.atan2(to.x - mc.player.getX(), to.z - mc.player.getZ()));
        if (yaw < 0) {
            return yaw + 360.0f;
        }
        return yaw;
    }

    public static float getRotationYaw360(Vec3 from, Vec3 to) {
        float yaw = (float) -Math.toDegrees(Math.atan2(to.x - from.x, to.z - from.z));
        if (yaw < 0) {
            return yaw + 360.0f;
        }
        return yaw;
    }

    public static float getNeededYawChange(float start, float end) {
        return normalizeAngle(end - start);
    }

    public static Angle getNeededChange(Angle startAngle, Angle endAngle) {
        float yawChange = normalizeAngle(normalizeAngle(endAngle.getYaw()) - normalizeAngle(startAngle.getYaw()));
        return new Angle(yawChange, endAngle.getPitch() - startAngle.getPitch());
    }

    public static boolean isLookingAtDebug(Vec3 vec, float distance) {
        VeinForge.LOGGER.debug("PlayerAngle: {}", getPlayerAngle());
        VeinForge.LOGGER.debug("RotationForVec: {}", getRotation(vec));
        Angle change = getNeededChange(getPlayerAngle(), getRotation(vec));
        VeinForge.LOGGER.debug("Change: {}, Dist: {}", change, distance);
        return Math.abs(change.getYaw()) <= distance && Math.abs(change.getPitch()) <= distance;
    }

    public static boolean isLookingAt(Vec3 vec, float distance) {
        Angle change = getNeededChange(getPlayerAngle(), getRotation(vec));
        return Math.abs(change.getYaw()) <= distance && Math.abs(change.getPitch()) <= distance;
    }
}
