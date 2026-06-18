package me.grish.veinforge.util;

import me.grish.veinforge.pathfinder.helper.BlockStateAccessor;
import me.grish.veinforge.pathfinder.movement.MovementHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class EntityUtil {

    private static final Minecraft mc = Minecraft.getInstance();

    public static boolean isNpc(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (!(entity instanceof RemotePlayer)) {
            return false;
        }
        // Assuming TablistUtil is ported
        return !TablistUtil.getTabListPlayersSkyblock().contains(entity.getName().getString());
    }

    public static BlockPos getBlockStandingOn(Entity entity) {
        return new BlockPos((int) entity.getX(), (int) Math.ceil(entity.getY() - 0.25) - 1, (int) entity.getZ());
    }

    public static Optional<Entity> getEntityLookingAt() {
        if (mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult) {
            return Optional.of(((net.minecraft.world.phys.EntityHitResult) mc.hitResult).getEntity());
        }
        return Optional.empty();
    }

    public static boolean isStandDead(String name) {
        return getHealthFromStandName(name) == 0;
    }

    public static int getHealthFromStandName(String name) {
        int health = 0;
        try {
            String[] arr = name.split(" ");
            health = Integer.parseInt(arr[arr.length - 1].split("/")[0].replace(",", ""));
        } catch (Exception ignored) {
        }
        return health;
    }

    public static Entity getEntityCuttingOtherEntity(Entity e, Class<?> entityType) {
        AABB box = e.getBoundingBox().inflate(0.3D, 2.0D, 0.3D);
        List<Entity> possible = mc.level.getEntities(e, box, a -> {
            boolean flag1 = (!a.isAlive() && !a.equals(mc.player)); // wait, isDead -> !isAlive?
            // Old code: !a.isDead. So a.isAlive().
            // And !a.equals(mc.player)
            if (!a.isAlive() || a.equals(mc.player)) return false;

            boolean flag2 = !(a instanceof ArmorStand);
            boolean flag3 = !(a instanceof Fireball);
            boolean flag4 = !(a instanceof FishingHook);
            boolean flag5 = (entityType == null || entityType.isInstance(a));
            return flag2 && flag3 && flag4 && flag5;
        });

        if (!possible.isEmpty())
            return Collections.min(possible, Comparator.comparing(e2 -> e2.distanceTo(e)));
        return null;
    }

    public static List<LivingEntity> getEntities(Set<String> entityNames, Set<LivingEntity> entitiesToIgnore) {
        List<LivingEntity> entities = new ArrayList<>();
        if (mc.level == null || mc.player == null) return entities;

        if (entityNames == null || entityNames.isEmpty()) {
            return entities;
        }

        // mc.world.loadedEntityList is gone. Use mc.world.getEntities()
        // Iterate once and avoid stream allocations; caller performs final scoring.
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof ArmorStand)) continue;
            if (!entity.isAlive()) continue;

            String customName = entity.getCustomName() != null ? entity.getCustomName().getString() : "";
            if (customName.isEmpty()) continue;
            if (customName.contains(mc.player.getName().getString())) continue;

            boolean nameMatch = false;
            for (String entityName : entityNames) {
                if (customName.contains(entityName)) {
                    nameMatch = true;
                    break;
                }
            }

            if (nameMatch && ((LivingEntity) entity).getHealth() > 0) { // ArmorStand is LivingEntity in 1.21? Yes.
                Entity livingBase = getEntityCuttingOtherEntity(entity, null);

                if (livingBase instanceof LivingEntity) {
                    if (!entitiesToIgnore.contains(livingBase)
                            && !livingBase.equals(mc.player)
                            && getHealthFromStandName(customName) != 1) {
                        entities.add((LivingEntity) livingBase);
                    }
                }
            }
        }
        return entities;
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public static BlockPos nearbyBlock(LivingEntity entityLivingBase) {
        BlockPos closestBlock = null;
        double closestDistance = Double.MAX_VALUE;
        if (mc.level == null) return entityLivingBase.blockPosition();

        BlockStateAccessor bsa = new BlockStateAccessor(mc.level);

        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos currentPos = entityLivingBase.blockPosition().offset(x, y, z);

                    if (MovementHelper.INSTANCE.canStandOn(
                            bsa,
                            currentPos.getX(),
                            currentPos.getY(),
                            currentPos.getZ(),
                            bsa.get(currentPos.getX(), currentPos.getY(), currentPos.getZ())
                    ) && RaytracingUtil.canSeePoint(new Vec3(currentPos.getX(), currentPos.getY(), currentPos.getZ()), entityLivingBase.getEyePosition())) { // 1.0F eyes? getEyePos() is precise.
                        double distance = currentPos.distSqr(PlayerUtil.getBlockStandingOn());

                        if (distance < closestDistance) {
                            closestBlock = currentPos;
                            closestDistance = distance;
                        }
                    }
                }
            }
        }

        if (closestBlock == null) {
            return getBlockStandingOn(entityLivingBase);
        }

        return closestBlock;
    }

}
