package me.grish.veinforge.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class PlayerUtil {

    private static final Minecraft mc = Minecraft.getInstance();

    // requires more testing
    public static BlockPos getBlockStandingOn() {
        // 0.25 = 3 layers of snow
        // if there is more than 3 layers of snow then i should consider that as a full block i guess
        // but there is no snow check in pathfinder so this will probably not work at all in snowy areas
        return BlockPos.containing(mc.player.getX(), Math.ceil(mc.player.getY() - 0.25) - 1, mc.player.getZ());
    }

    public static Vec3 getPlayerEyePos() {
        return mc.player.getEyePosition();
    }

    public static BlockPos getBlockStandingOnFloor() {
        return BlockPos.containing(mc.player.getX(), Math.floor(mc.player.getY()) - 1, mc.player.getZ());
    }

    public static Vec3 getNextTickPosition() {
        return mc.player.position().add(mc.player.getDeltaMovement().x, 0, mc.player.getDeltaMovement().z);
    }

    public static Vec3 getNextTickPosition(float mult) {
        return mc.player.position().add(mc.player.getDeltaMovement().x * mult, 0, mc.player.getDeltaMovement().z * mult);
    }

    public static Entity getEntityCuttingOtherEntity(Entity e) {
        return getEntityCuttingOtherEntity(e, entity -> true);
    }

    public static Entity getEntityCuttingOtherEntity(Entity e, Predicate<Entity> predicate) {
        List<Entity> possible = mc.level.getEntities(e, e.getBoundingBox().inflate(0.3D, 2.0D, 0.3D), a -> {
            boolean flag1 = (a.isAlive() && !a.equals(mc.player));
            boolean flag2 = !(a instanceof LargeFireball);
            boolean flag3 = !(a instanceof FishingHook);
            boolean flag4 = predicate.test(a);
            return flag1 && flag2 && flag3 && flag4;
        });
        if (!possible.isEmpty()) {
            return Collections.min(possible, Comparator.comparing(e2 -> e2.distanceTo(e)));
        }
        return null;
    }

    public static boolean isPlayerSuffocating() {
        AABB playerBB = mc.player.getBoundingBox().inflate(-0.15, -0.15, -0.15);
        return mc.level.getBlockCollisions(mc.player, playerBB).iterator().hasNext();
    }

    public static Direction getHorizontalFacing(float yaw) {
        return Direction.fromYRot(yaw);
    }
}
