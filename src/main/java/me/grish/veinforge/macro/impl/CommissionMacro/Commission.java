package me.grish.veinforge.macro.impl.CommissionMacro;

import lombok.Getter;
import me.grish.veinforge.util.CommissionUtil;
import me.grish.veinforge.util.EntityUtil;
import me.grish.veinforge.util.Logger;
import me.grish.veinforge.util.TablistUtil;
import me.grish.veinforge.util.helper.location.SubLocation;
import me.grish.veinforge.util.helper.route.RouteWaypoint;
import me.grish.veinforge.util.helper.route.WaypointType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public enum Commission {
   // maybe set it to null and choose a random one? - DO NOT REARRANGE THIS
   MITHRIL_MINER("Mithril Miner", SubLocation.RAMPARTS_QUARRY),
   TITANIUM_MINER("Titanium Miner", SubLocation.RAMPARTS_QUARRY),
   UPPER_MITHRIL("Upper Mines Mithril", SubLocation.UPPER_MINES),
   UPPER_TITANIUM("Upper Mines Titanium", SubLocation.UPPER_MINES),
   ROYAL_MITHRIL("Royal Mines Mithril", SubLocation.ROYAL_MINES),
   ROYAL_TITANIUM("Royal Mines Titanium", SubLocation.ROYAL_MINES),
   LAVA_MITHRIL("Lava Springs Mithril", SubLocation.LAVA_SPRINGS),
   LAVA_TITANIUM("Lava Springs Titanium", SubLocation.LAVA_SPRINGS),
   CLIFFSIDE_MITHRIL("Cliffside Veins Mithril", SubLocation.CLIFFSIDE_VEINS),
   CLIFFSIDE_TITANIUM("Cliffside Veins Titanium", SubLocation.CLIFFSIDE_VEINS),
   RAMPARTS_MITHRIL("Rampart's Quarry Mithril", SubLocation.RAMPARTS_QUARRY),
   RAMPARTS_TITANIUM("Rampart's Quarry Titanium", SubLocation.RAMPARTS_QUARRY),
   GOBLIN_SLAYER("Goblin Slayer", SubLocation.GOBLIN_BURROWS),
   GLACITE_WALKER_SLAYER("Glacite Walker Slayer", SubLocation.GREAT_ICE_WALL),
   MINES_SLAYER("Mines Slayer", SubLocation.GOBLIN_BURROWS),
   COMMISSION_CLAIM("Claim Commission", SubLocation.THE_FORGE), // theres no set location for this so yea
   REFUEL("Refuel Drill", SubLocation.FORGE_BASIN); // theres no set location for this so yea
   // Do not want this
//  TREASURE_HOARDER_SLAYER("Treasure Hoarder Puncher", SubLocation.TREASURE_HUNTER_CAMP),
//  STAR_CENTRY_SLAYER("Star Sentry Puncher	"),

   private static final Map<String, Commission> COMMISSIONS;
   private static final Map<SubLocation, RouteWaypoint[]> VEINS;
   private static final double PROGRESS_WEIGHT_PENALTY = 6.0;
   private static final double WAYPOINT_PLAYER_AVOID_RADIUS = 9.0;
   private static final Set<SubLocation> GENERIC_MINER_LOCATIONS = EnumSet.of(
           SubLocation.RAMPARTS_QUARRY,
           SubLocation.UPPER_MINES,
           SubLocation.ROYAL_MINES,
           SubLocation.LAVA_SPRINGS,
           SubLocation.CLIFFSIDE_VEINS
   );

   static {
      Map<String, Commission> commissionsMap = new HashMap<>();
      for (Commission comm : Commission.values()) {
         commissionsMap.put(comm.name, comm);
      }
      COMMISSIONS = Collections.unmodifiableMap(commissionsMap);

      Map<SubLocation, RouteWaypoint[]> veinsMap = new EnumMap<SubLocation, RouteWaypoint[]>(SubLocation.class) {{
         put(SubLocation.FORGE_BASIN, new RouteWaypoint[]{
                 new RouteWaypoint(-9, 144, -20, WaypointType.WALK)
         });
         put(SubLocation.THE_FORGE, new RouteWaypoint[]{
                 new RouteWaypoint(44, 134, 21, WaypointType.WALK),
                 new RouteWaypoint(58, 197, -11, WaypointType.WALK),
                 new RouteWaypoint(171, 149, 33, WaypointType.WALK),
                 new RouteWaypoint(-75, 152, -11, WaypointType.WALK),
                 new RouteWaypoint(-132, 173, -53, WaypointType.WALK)
         });
         put(SubLocation.CLIFFSIDE_VEINS, new RouteWaypoint[]{
                 new RouteWaypoint(93, 144, 51, WaypointType.WALK),
                 new RouteWaypoint(28, 130, 26, WaypointType.WALK)
         });
         put(SubLocation.ROYAL_MINES, new RouteWaypoint[]{
                 new RouteWaypoint(115, 153, 83, WaypointType.WALK),
                 new RouteWaypoint(141, 152, 27, WaypointType.WALK)
         });
         put(SubLocation.GREAT_ICE_WALL, new RouteWaypoint[]{new RouteWaypoint(5, 127, 143, WaypointType.WALK)});
         put(SubLocation.GOBLIN_BURROWS, new RouteWaypoint[]{new RouteWaypoint(-56, 134, 153, WaypointType.WALK)});
         put(SubLocation.RAMPARTS_QUARRY, new RouteWaypoint[]{
                 new RouteWaypoint(-41, 138, -13, WaypointType.WALK),
                 new RouteWaypoint(-58, 146, -18, WaypointType.WALK)
         });
         put(SubLocation.UPPER_MINES, new RouteWaypoint[]{
                 new RouteWaypoint(-111, 166, -74, WaypointType.WALK),
                 new RouteWaypoint(-145, 206, -30, WaypointType.WALK)
         });
         put(SubLocation.TREASURE_HUNTER_CAMP, new RouteWaypoint[]{new RouteWaypoint(-115, 204, -53, WaypointType.WALK)});
         put(SubLocation.LAVA_SPRINGS, new RouteWaypoint[]{new RouteWaypoint(53, 197, -24, WaypointType.WALK)});
      }};
      VEINS = Collections.unmodifiableMap(veinsMap);
   }

   @Getter
   private final String name;
   @Getter
   private final SubLocation location;
   private final int priority;

   Commission(String name, SubLocation location) {
      this.name = name;
      this.location = location;
      if (name.endsWith("Miner")) {
         this.priority = 1;
      } else {
         this.priority = 0;
      }
   }

   public static Commission getCommission(final String name) {
      return COMMISSIONS.get(name);
   }

   // this is incredibly bad
   public static List<Commission> getBestCommissionFrom(List<Commission> commissions) {
      if (commissions.isEmpty()) {
         return Collections.emptyList();
      }

      Map<Commission, Double> commissionsWithProgress = new LinkedHashMap<>();
      for (Commission commission : commissions) {
         commissionsWithProgress.put(commission, 0.0);
      }
      return getBestCommissionFrom(commissionsWithProgress);
   }

   public static List<Commission> getBestCommissionFrom(Map<Commission, Double> commissionsWithProgress) {
      if (commissionsWithProgress == null || commissionsWithProgress.isEmpty()) {
         return Collections.emptyList();
      }

      Commission best = null;
      double bestEffectiveScore = Double.MAX_VALUE;
      int bestBaseScore = Integer.MAX_VALUE;
      double bestProgress = -1.0;

      for (Map.Entry<Commission, Double> entry : commissionsWithProgress.entrySet()) {
         Commission candidate = entry.getKey();
         if (candidate == null) {
            continue;
         }

         int baseScore = getSelectionScore(candidate);
         double progress = clampProgress(entry.getValue());
         double effectiveScore = baseScore + ((1.0 - progress) * PROGRESS_WEIGHT_PENALTY);

         if (best == null
                     || effectiveScore < bestEffectiveScore
                     || (effectiveScore == bestEffectiveScore && progress > bestProgress)
                     || (effectiveScore == bestEffectiveScore && progress == bestProgress && baseScore < bestBaseScore)) {
            best = candidate;
            bestEffectiveScore = effectiveScore;
            bestBaseScore = baseScore;
            bestProgress = progress;
         }
      }

      if (best == null) {
         return Collections.emptyList();
      }

      Logger.sendLog("Selected commission by weighted score: " + best.getName()
                              + " (base=" + bestBaseScore
                              + ", progress=" + Math.round(bestProgress * 100.0) + "%"
                              + ", effective=" + String.format(Locale.US, "%.2f", bestEffectiveScore) + ")");
      return new ArrayList<>(Collections.singletonList(best));
   }

   private static int getSelectionScore(Commission commission) {
      if (commission == null) {
         return Integer.MAX_VALUE;
      }

      String commissionName = commission.getName();
      if (commissionName.contains("Titanium")) {
         return 5;
      }
      if (commissionName.contains("Mithril Miner")) {
         return 15;
      }
      if (commissionName.contains("Mithril")) {
         return 10;
      }
      if (commissionName.contains("Glacite Walker")) {
         return 20;
      }
      if (commissionName.contains("Goblin") || commission == MINES_SLAYER) {
         return 30;
      }
      if (commissionName.contains("Treasure Hoarder")) {
         return 50;
      }
      return 100;
   }

   private static double clampProgress(Double progress) {
      if (progress == null) {
         return 0.0;
      }
      if (progress < 0.0) {
         return 0.0;
      }
      if (progress > 1.0) {
         return 1.0;
      }
      return progress;
   }

   public static RouteWaypoint[] getWaypoints(SubLocation location) {
      return VEINS.get(location);
   }

   public static boolean isMithrilLocation(SubLocation location) {
      return GENERIC_MINER_LOCATIONS.contains(location);
   }

   public RouteWaypoint getWaypoint() {
      if (this.name.equals("Claim Commission")) {
         return closestWaypointTo(CommissionUtil.getClosestEmissaryPosition());
      }

      if (this == MITHRIL_MINER || this == TITANIUM_MINER) {
         Set<SubLocation> targetLocations = new HashSet<>();
         boolean inCommissionSection = false;

         // Check for overlapping commissions
         for (String text : TablistUtil.getCachedTablist()) {
            if (!inCommissionSection) {
               if (text.contains("Commissions:")) {
                  inCommissionSection = true;
               }
               continue;
            }

            if (text.trim().isEmpty()) {
               break;
            }

            if (text.contains(":")) {
               String commName = text.split(": ")[0].trim();
               Commission otherComm = Commission.getCommission(commName);
               // If we find another active commission that requires a specific location which supports mithril
               if (otherComm != null && otherComm != this && GENERIC_MINER_LOCATIONS.contains(otherComm.location)) {
                  targetLocations.add(otherComm.location);
               }
            }
         }

         Collection<SubLocation> locationsToCheck = !targetLocations.isEmpty() ?
                                                            targetLocations : GENERIC_MINER_LOCATIONS;

         List<RouteWaypoint> allWaypoints = locationsToCheck.stream()
                                                    .map(VEINS::get)
                                                    .filter(Objects::nonNull)
                                                    .flatMap(Arrays::stream)
                                                    .collect(java.util.stream.Collectors.toList());

         if (allWaypoints.isEmpty()) {
            throw new IllegalStateException("No waypoints found for generic miner commission.");
         }

         return selectBestWaypoint(allWaypoints);
      }

      RouteWaypoint[] locs = VEINS.get(this.location);
      if (locs != null && locs.length > 0) {
         return selectBestWaypoint(Arrays.asList(locs));
      }
      throw new IllegalStateException("No route waypoints available for location: " + this.location);
   }

   public RouteWaypoint closestWaypointTo(Vec3 pos) {
      RouteWaypoint[] locs = VEINS.get(this.location);
      if (locs != null && locs.length > 0) {
         return Arrays.stream(locs).min(Comparator.comparing(it -> it.toVec3d().distanceTo(pos))).get();
      }
      throw new IllegalStateException("No route waypoints available for location: " + this.location);
   }

   private static RouteWaypoint selectBestWaypoint(Collection<RouteWaypoint> candidates) {
      if (candidates == null || candidates.isEmpty()) {
         throw new IllegalArgumentException("Candidates cannot be empty.");
      }

      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null) {
         return candidates.iterator().next();
      }

      Vec3 playerPos = mc.player.position();
      double avoidRadiusSq = WAYPOINT_PLAYER_AVOID_RADIUS * WAYPOINT_PLAYER_AVOID_RADIUS;

      return candidates.stream()
                     .min(Comparator
                                  .comparingInt((RouteWaypoint waypoint) -> countNearbyRealPlayers(waypoint.toVec3d(), avoidRadiusSq))
                                  .thenComparingDouble(waypoint -> waypoint.toVec3d().distanceToSqr(playerPos)))
                     .orElseGet(() -> candidates.iterator().next());
   }

   private static int countNearbyRealPlayers(Vec3 target, double radiusSq) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null) {
         return 0;
      }

      return (int) mc.level.players().stream()
                      .filter(player -> !player.is(mc.player))
                      .filter(Player::isAlive)
                      .filter(player -> !EntityUtil.isNpc(player))
                      .filter(player -> player.position().distanceToSqr(target) <= radiusSq)
                      .count();
   }
}
