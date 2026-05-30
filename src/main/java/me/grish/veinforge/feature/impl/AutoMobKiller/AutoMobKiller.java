package me.grish.veinforge.feature.impl.AutoMobKiller;

import lombok.Getter;
import lombok.Setter;
import me.grish.veinforge.feature.AbstractFeature;
import me.grish.veinforge.feature.impl.AutoMobKiller.states.AutoMobKillerState;
import me.grish.veinforge.feature.impl.AutoMobKiller.states.StartingState;
import me.grish.veinforge.feature.impl.Pathfinder;
import me.grish.veinforge.util.EntityUtil;
import me.grish.veinforge.util.InventoryUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * MobKiller
 * <p>
 * Main controller class for automatic mob killer feature.
 * Implements a state machine pattern to manage different phases of the killing process.
 * Handles mob selection, pathfinding, and attack management.
 */
public class AutoMobKiller extends AbstractFeature {

   private static final long APPROACH_BLOCK_CACHE_MS = 250L;
   private static final int MAX_BLACKLIST_GENERIC = 12;
   private static final int MAX_BLACKLIST_GOBLIN = 4;
   private static final int MAX_BLACKLIST_GLACITE = 8;
   private static AutoMobKiller instance;
   /**
    * Names of mobs to kill (ex: Glacite Walker)
    */
   @Getter
   private final Set<String> mobsToKill = new HashSet<>();
   private AutoMobKillerState currentState;
   @Getter
   @Setter
   private MKError error = MKError.NONE;
   @Getter
   @Setter
   private SlayerProfile slayerProfile = SlayerProfile.GENERIC;
   /**
    * Target mob to kill
    */
   @Getter
   @Setter
   private LivingEntity targetMob = null;
   /**
    * Original position of the target mob (to check if it has moved away)
    */
   @Getter
   @Setter
   private Vec3 targetMobOriginalPos = null;
   /**
    * Number of Re-pathing attempts
    */
   @Getter
   @Setter
   private int pathAttempts = 0;
   /**
    * Blacklisted mobs (from failed pathfinding attempts)
    */
   @Getter
   @Setter
   private Set<LivingEntity> blacklistedMobs = new HashSet<>();
   private final Deque<LivingEntity> blacklistedOrder = new ArrayDeque<>();
   private LivingEntity approachBlockTarget = null;
   private BlockPos cachedApproachBlock = null;
   private BlockPos cachedApproachMobStanding = null;
   private long cachedApproachComputedAt = 0L;

   public static AutoMobKiller getInstance() {
      if (instance == null) {
         instance = new AutoMobKiller();
      }

      return instance;
   }

   @Override
   public String getName() {
      return "AutoMobKiller";
   }

   /**
    * Starts the AutoMobKiller with specified parameters. Will continue to kill mobs {@code mobsToKill} until stop() is called or no entities found in 10 seconds.
    *
    * @param mobsToKill List of mob names to kill
    * @param weaponName Name of the melee weapon that will be used to kill mobs
    */
   public void start(Collection<String> mobsToKill, String weaponName) {
      start(mobsToKill, weaponName, SlayerProfile.GENERIC);
   }

   /**
    * Starts the AutoMobKiller with specified parameters.
    *
    * @param mobsToKill    List of mob names to kill
    * @param weaponName    Name of the melee weapon that will be used to kill mobs
    * @param slayerProfile Commission-specific profile used for target filtering and fallback movement
    */
   public void start(Collection<String> mobsToKill, String weaponName, SlayerProfile slayerProfile) {
      if (!InventoryUtil.holdItem(weaponName)) {
         sendError("Weapon not found in inventory!");
         stop();
         return;
      }

      this.mobsToKill.clear();
      this.blacklistedMobs.clear();
      this.blacklistedOrder.clear();
      this.targetMob = null;
      this.targetMobOriginalPos = null;
      this.pathAttempts = 0;
      this.slayerProfile = slayerProfile == null ? SlayerProfile.GENERIC : slayerProfile;
      this.mobsToKill.addAll(mobsToKill);
      this.error = MKError.NONE;
      resetApproachBlockCache();

      this.currentState = new StartingState();
      this.enabled = true;
      log("MobKiller started");
   }

   @Override
   public void stop() {
      if (!this.enabled) {
         return;
      }

      this.enabled = false;

      this.mobsToKill.clear();
      this.blacklistedMobs.clear();
      this.blacklistedOrder.clear();
      this.targetMob = null;
      this.targetMobOriginalPos = null;
      this.currentState = null;
      this.pathAttempts = 0;
      this.slayerProfile = SlayerProfile.GENERIC;
      resetApproachBlockCache();

      Pathfinder.getInstance().stop();
      log("MobKiller stopped");
   }

   @Override
   protected void onTick() {
      if (mc.screen != null) {
         return;
      }

      if (currentState == null)
         return;

      pruneBlacklistedMobs();
      AutoMobKillerState nextState = currentState.onTick(this);
      transitionTo(nextState);
   }

   private void transitionTo(AutoMobKillerState nextState) {
      // Skip if no state change
      if (currentState == nextState)
         return;

      currentState.onEnd(this);
      currentState = nextState;

      if (currentState == null) {
         log("null state, returning");
         return;
      }

      currentState.onStart(this);
   }

   public enum MKError {
      NONE,        // No error
      NO_ENTITIES  // No entities found
   }

   public enum SlayerProfile {
      GENERIC(null),
      GOBLIN(new BlockPos(-134, 143, 142)),
      GLACITE(new BlockPos(5, 127, 143));

      private final BlockPos anchorPoint;

      SlayerProfile(BlockPos anchorPoint) {
         this.anchorPoint = anchorPoint;
      }

      public boolean hasAnchorPoint() {
         return anchorPoint != null;
      }

      public BlockPos getAnchorPoint() {
         return anchorPoint;
      }

      public boolean isTargetInPreferredZone(LivingEntity entity) {
         if (entity == null) {
            return false;
         }

         double x = entity.getX();
         double y = entity.getY();
         double z = entity.getZ();

         switch (this) {
            case GOBLIN:
               return y > 127.0
                           && (z <= 153.0 || x >= -157.0)
                           && (z >= 148.0 || x <= -77.0);
            case GLACITE:
               return y >= 127.0
                           && y <= 132.0
                           && z >= 147.0
                           && z <= 180.0
                           && x <= 42.0;
            case GENERIC:
            default:
               return true;
         }
      }
   }

   public String getCurrentStateName() {
      return this.currentState == null ? "NONE" : this.currentState.getClass().getSimpleName();
   }

   public void updateTargetMob(LivingEntity mob) {
      this.targetMob = mob;
      resetApproachBlockCache();
   }

   public BlockPos getApproachBlockForTarget(boolean forceRefresh) {
      if (targetMob == null) {
         return null;
      }

      BlockPos currentMobStanding = EntityUtil.getBlockStandingOn(targetMob);
      long now = System.currentTimeMillis();

      boolean sameTarget = targetMob == approachBlockTarget;
      boolean cacheFresh = (now - cachedApproachComputedAt) <= APPROACH_BLOCK_CACHE_MS;
      boolean mobStillNearby = cachedApproachMobStanding != null && currentMobStanding.distSqr(cachedApproachMobStanding) <= 1.0;

      if (!forceRefresh && sameTarget && cacheFresh && mobStillNearby && cachedApproachBlock != null) {
         return cachedApproachBlock;
      }

      cachedApproachBlock = EntityUtil.nearbyBlock(targetMob);
      cachedApproachMobStanding = currentMobStanding;
      cachedApproachComputedAt = now;
      approachBlockTarget = targetMob;
      return cachedApproachBlock;
   }

   public void blacklistTargetMob() {
      blacklistMob(this.targetMob);
   }

   public void blacklistMob(LivingEntity mob) {
      if (mob == null) {
         return;
      }

      pruneBlacklistedMobs();

      if (blacklistedMobs.add(mob)) {
         blacklistedOrder.offerLast(mob);
      }

      int maxSize = getMaxBlacklistSize();
      while (blacklistedOrder.size() > maxSize) {
         LivingEntity oldest = blacklistedOrder.pollFirst();
         if (oldest != null) {
            blacklistedMobs.remove(oldest);
         }
      }
   }

   public void pruneBlacklistedMobs() {
      Iterator<LivingEntity> iterator = blacklistedOrder.iterator();
      while (iterator.hasNext()) {
         LivingEntity mob = iterator.next();
         if (mob == null || !mob.isAlive() || mob.isRemoved()) {
            iterator.remove();
            blacklistedMobs.remove(mob);
         }
      }
   }

   private int getMaxBlacklistSize() {
      switch (slayerProfile) {
         case GOBLIN:
            return MAX_BLACKLIST_GOBLIN;
         case GLACITE:
            return MAX_BLACKLIST_GLACITE;
         case GENERIC:
         default:
            return MAX_BLACKLIST_GENERIC;
      }
   }

   private void resetApproachBlockCache() {
      approachBlockTarget = null;
      cachedApproachBlock = null;
      cachedApproachMobStanding = null;
      cachedApproachComputedAt = 0L;
   }
}
