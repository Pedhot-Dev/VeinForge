package me.grish.veinforge.feature.impl;

import com.mojang.blaze3d.platform.InputConstants;
import me.grish.veinforge.VeinForge;
import me.grish.veinforge.feature.AbstractFeature;
import me.grish.veinforge.handler.GraphHandler;
import me.grish.veinforge.macro.impl.CommissionMacro.Commission;
import me.grish.veinforge.macro.impl.CommissionMacro.CommissionMacro;
import me.grish.veinforge.util.*;
import me.grish.veinforge.util.helper.MineableBlock;
import me.grish.veinforge.util.helper.route.Route;
import me.grish.veinforge.util.helper.route.RouteWaypoint;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Commission Debug Mode - Simulation mode for Commission Macro.
 * <p>
 * Displays routes, pathfinding information, and simulates block selection logic
 * without sending any inputs to the server.
 * Useful for debugging route configurations and understanding macro behavior.
 */
public class CommissionDebugMode extends AbstractFeature {

   private static final Minecraft mc = Minecraft.getInstance();
   private static final String GRAPH_NAME = "Commission Macro";
   private static final long RECALCULATION_INTERVAL_MS = 500; // Recalculate every 500ms when moving
   // Color palette for different commissions
   private static final Color[] ROUTE_COLORS = {
           new Color(255, 100, 100, 180), // Red
           new Color(100, 255, 100, 180), // Green
           new Color(100, 100, 255, 180), // Blue
           new Color(255, 255, 100, 180), // Yellow
           new Color(255, 100, 255, 180), // Magenta
           new Color(100, 255, 255, 180), // Cyan
           new Color(255, 165, 0, 180),   // Orange
           new Color(128, 0, 128, 180),   // Purple
   };
   private static CommissionDebugMode instance;
   // Calculated routes for visualization
   private final Map<Commission, Route> calculatedRoutes = new LinkedHashMap<>();
   private final Map<Commission, Color> routeColors = new HashMap<>();

   // Mining Simulation Data
   private final MineableBlock[] blocksToMine = {MineableBlock.GRAY_MITHRIL, MineableBlock.GREEN_MITHRIL, MineableBlock.BLUE_MITHRIL, MineableBlock.TITANIUM};
   private final int[] mithrilPriority = {10, 6, 3, 1};
   private final int[] prioritiseTitanium = {10, 6, 3, 20};
   private final int[] titaniumPriority = {3, 2, 1, 20};
   // Enhanced Debug Data
   private final List<BlockPos> debugCandidates = new ArrayList<>();
   private final List<BlockPos> debugObstructed = new ArrayList<>();
   private final List<BlockPos> debugSearchOrigins = new ArrayList<>();
   private final Map<BlockPos, Double> debugCosts = new HashMap<>();
   private final Map<BlockPos, String> debugRejectReasons = new HashMap<>();
   private BlockPos simulatedTargetBlock = null;
   private double simulatedTargetCost = 0;
   private String simulatedTargetReason = "";
   // State tracking
   private BlockPos lastPlayerPos = null;
   private long lastCalculationTime = 0;
   // Custom target for debug commands
   private BlockPos customTarget = null;
   private Route customRoute = null;
   private String customTargetName = null;

   public static CommissionDebugMode getInstance() {
      if (instance == null) {
         instance = new CommissionDebugMode();
      }
      return instance;
   }

   @Override
   public String getName() {
      return "CommissionDebugMode";
   }

   public void toggle() {
      if (this.enabled) {
         stop();
      } else {
         start();
      }
   }

   @Override
   public void start() {
      this.enabled = true;
      this.calculatedRoutes.clear();
      this.routeColors.clear();
      this.lastPlayerPos = null;
      resetSimulationState();

      send("Commission Debug Mode §aENABLED§r - Showing route and enhanced mining logic visualizations");
      // Play enable sound
      if (mc.player != null) {
         mc.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F, 2.0F);
      }
      calculateAllRoutes();
   }

   @Override
   public void stop() {
      this.enabled = false;
      this.calculatedRoutes.clear();
      this.routeColors.clear();
      this.lastPlayerPos = null;
      resetSimulationState();

      send("Commission Debug Mode §cDISABLED");
      // Play disable sound
      if (mc.player != null) {
         mc.player.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
      }
   }

   @Override
   public void resetStatesAfterStop() {
      this.calculatedRoutes.clear();
      this.routeColors.clear();
      this.customRoute = null;
      resetSimulationState();
   }

   private void resetSimulationState() {
      this.simulatedTargetBlock = null;
      this.debugCandidates.clear();
      this.debugObstructed.clear();
      this.debugCosts.clear();
      this.debugSearchOrigins.clear();
      this.debugRejectReasons.clear();
   }

   public void setCustomTarget(BlockPos target, String name) {
      this.customTarget = target;
      this.customTargetName = name;
      this.enabled = true;
      calculateAllRoutes();
      send("§aDebug path set to: §f" + name + " " + target);
   }

   public void clearCustomTarget() {
      this.customTarget = null;
      this.customRoute = null;
      this.customTargetName = null;
      calculateAllRoutes();
      send("§aCleared custom debug target");
   }

   /**
    * Calculate routes to all possible commission waypoints from current position.
    */
   private void calculateAllRoutes() {
      if (mc.player == null || mc.level == null) {
         return;
      }

      calculatedRoutes.clear();
      BlockPos playerPos = PlayerUtil.getBlockStandingOn();

      // Get current commissions from tablist
      List<Commission> currentCommissions = CommissionUtil.getCurrentCommissionsFromTablist();

      // Also include all commission types for full visualization
      Set<Commission> commissionsToShow = new LinkedHashSet<>(currentCommissions);

      // Add other common commissions for reference (optional, can be config-driven)
      if (commissionsToShow.isEmpty()) {
         // If no commissions detected, show common mining spots
         commissionsToShow.add(Commission.MITHRIL_MINER);
         commissionsToShow.add(Commission.TITANIUM_MINER);
         commissionsToShow.add(Commission.GOBLIN_SLAYER);
      }

      int colorIndex = 0;
      for (Commission commission : commissionsToShow) {
         try {
            RouteWaypoint endpoint = commission.getWaypoint();
            List<RouteWaypoint> pathNodes = GraphHandler.instance.findPathFrom(
                    GRAPH_NAME, playerPos, endpoint
            );

            if (!pathNodes.isEmpty()) {
               Route route = new Route(pathNodes);
               calculatedRoutes.put(commission, route);
               routeColors.put(commission, ROUTE_COLORS[colorIndex % ROUTE_COLORS.length]);
               colorIndex++;
            }
         } catch (Exception e) {
            // Ignore route errors in debug mode
         }
      }

      // Calculate custom route if set
      if (customTarget != null) {
         try {
            List<RouteWaypoint> pathNodes = GraphHandler.instance.findPathFrom(
                    GRAPH_NAME, playerPos, new RouteWaypoint(customTarget, me.grish.veinforge.util.helper.route.WaypointType.WALK)
            );

            if (!pathNodes.isEmpty()) {
               customRoute = new Route(pathNodes);
               log("Calculated custom route to " + customTargetName);
            } else {
               send("§cCould not find path to " + customTargetName);
               customRoute = null;
            }
         } catch (Exception e) {
            log("Failed to calculate custom route: " + e.getMessage());
         }
      }

      lastPlayerPos = playerPos;
      lastCalculationTime = System.currentTimeMillis();
   }

   private void simulateMiningLogic() {
      if (mc.player == null || mc.level == null) return;

      resetSimulationState();

      // Determine commission context
      Commission activeCommission = null;
      List<Commission> currentCommissions = CommissionUtil.getCurrentCommissionsFromTablist();
      if (!currentCommissions.isEmpty()) {
         activeCommission = currentCommissions.get(0);
      } else if (calculatedRoutes.containsKey(Commission.MITHRIL_MINER)) {
         activeCommission = Commission.MITHRIL_MINER;
      } else if (calculatedRoutes.containsKey(Commission.TITANIUM_MINER)) {
         activeCommission = Commission.TITANIUM_MINER;
      }

      if (activeCommission == null) {
         simulatedTargetReason = "No active commission";
         return;
      }

      // Only simulate mining for mining commissions
      if (!activeCommission.getName().contains("Miner") && !activeCommission.getName().contains("Mithril") && !activeCommission.getName().contains("Titanium")) {
         simulatedTargetReason = "Not a mining commission";
         return;
      }

      // Logic from MiningState
      int[] priorityToUse;
      if (activeCommission.getName().contains("Titanium")) {
         priorityToUse = titaniumPriority;
      } else {
         priorityToUse = VeinForge.config().commission.dwarvenCommission.prioritiseTitanium ? prioritiseTitanium : mithrilPriority;
      }

      Map<Block, Integer> blockPriority = new HashMap<>();
      for (int i = 0; i < blocksToMine.length; i++) {
         for (Block b : blocksToMine[i].getBlocks()) {
            if (b != null) {
               blockPriority.put(b, priorityToUse[i]);
            }
         }
      }

      int miningSpeed = CommissionMacro.getInstance().getMiningSpeed();
      if (miningSpeed == 0) miningSpeed = 2000; // Default estimate if not running

      // --- Use BlockUtil with Debug Context ---
      BlockPos playerBlock = PlayerUtil.getBlockStandingOn();
      debugSearchOrigins.addAll(BlockUtil.getWalkableBlocksAround(playerBlock));

      List<BlockPos> allCandidates = BlockUtil.findMineableBlocksFromAccessiblePositions(
              blockPriority, null, miningSpeed, new BlockUtil.MiningDebugContext() {
                 @Override
                 public void onBlockRejected(BlockPos pos, String reason) {
                    if ("Obstructed".equals(reason)) {
                       debugObstructed.add(pos);
                    }
                    debugRejectReasons.put(pos, reason);
                 }

                 @Override
                 public void onBlockCandidate(BlockPos pos, double cost) {
                    debugCosts.put(pos, cost);
                 }
              }
      );

      // Sort candidates by cost
      allCandidates.sort(Comparator.comparingDouble(debugCosts::get));
      debugCandidates.addAll(allCandidates);

      if (!allCandidates.isEmpty()) {
         simulatedTargetBlock = allCandidates.get(0);
         simulatedTargetCost = debugCosts.get(simulatedTargetBlock);

         // Recalculate cost factors for display
         BlockState state = mc.level.getBlockState(simulatedTargetBlock);
         int p = blockPriority.getOrDefault(state.getBlock(), 1);
         double hardness = BlockUtil.getBlockStrength(state);
         float angleChange = AngleUtil.getNeededChange(AngleUtil.getPlayerAngle(), AngleUtil.getRotation(simulatedTargetBlock)).lengthSqrt();
         double distSq = mc.player.distanceToSqr(Vec3.atCenterOf(simulatedTargetBlock));

         simulatedTargetReason = String.format("Cost: %.2f (H:%.0f A:%.1f D:%.1f P:%d)", simulatedTargetCost, hardness, angleChange, distSq, p);
      } else {
         simulatedTargetReason = "No valid blocks found";
      }
   }

   @Override
   protected void onTick() {
      if (!this.enabled) {
         return;
      }

      if (mc.player == null || mc.level == null) {
         return;
      }

      // Check if we need to recalculate routes (player moved significantly)
      BlockPos currentPos = PlayerUtil.getBlockStandingOn();
      long currentTime = System.currentTimeMillis();

      if (lastPlayerPos == null ||
                  (!currentPos.equals(lastPlayerPos) && currentTime - lastCalculationTime > RECALCULATION_INTERVAL_MS)) {
         calculateAllRoutes();
         simulateMiningLogic();
      }
   }

   @Override
   protected void onWorldRender(LevelRenderContext context) {
      if (!this.enabled) {
         return;
      }

      if (mc.player == null || mc.level == null) {
         return;
      }

      // --- Mining Debug Rendering ---

      // 1. Draw Search Origins (Walkable blocks)
      for (BlockPos pos : debugSearchOrigins) {
         // Tiny blue dots at feet
         RenderUtil.drawBlock(pos, new Color(0, 0, 255, 30));
      }

      // 2. Draw Obstructed Blocks (Red)
      for (BlockPos pos : debugObstructed) {
         RenderUtil.drawBlock(pos, new Color(255, 0, 0, 40));
         // Optional: RenderUtil.drawText("X", pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.5f);
      }

      // 3. Draw Candidate Blocks (Green/Aqua)
      // Skip index 0 (Target) as it gets special highlighting
      for (int i = 1; i < debugCandidates.size(); i++) {
         BlockPos pos = debugCandidates.get(i);
         // Shade by cost? Lower cost = greener, Higher cost = yellower
         RenderUtil.drawBlock(pos, new Color(0, 255, 255, 40));

         Double cost = debugCosts.get(pos);
         if (cost != null) {
            RenderUtil.drawText(String.format("%.1f", cost), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.4f);
         }
      }

      // 4. Draw Simulated Target Block (Gold)
      if (simulatedTargetBlock != null) {
         // Draw box
         RenderUtil.drawBlock(simulatedTargetBlock, new Color(255, 215, 0, 150)); // Gold color
         // Draw line from eyes
         RenderUtil.drawLine(
                 mc.player.getEyePosition(),
                 new Vec3(simulatedTargetBlock.getX() + 0.5, simulatedTargetBlock.getY() + 0.5, simulatedTargetBlock.getZ() + 0.5),
                 new Color(255, 215, 0)
         );
         // Draw text
         RenderUtil.drawText("§6TARGET\n§f" + simulatedTargetReason,
                 simulatedTargetBlock.getX() + 0.5, simulatedTargetBlock.getY() + 1.5, simulatedTargetBlock.getZ() + 0.5, 0.5F);
      }

      // Draw current player position marker
      BlockPos playerPos = PlayerUtil.getBlockStandingOn();
      RenderUtil.drawBlock(playerPos, new Color(0, 255, 0, 100));
      RenderUtil.drawText("§aYOU", playerPos.getX() + 0.5, playerPos.getY() + 2.5, playerPos.getZ() + 0.5, 1.0F);

      // Draw each calculated route
      for (Map.Entry<Commission, Route> entry : calculatedRoutes.entrySet()) {
         Commission commission = entry.getKey();
         Route route = entry.getValue();
         Color color = routeColors.getOrDefault(
                 commission,
                 RenderUtil.parseColor(VeinForge.config().debug.debugRouteColor, new Color(0, 255, 0, 180))
         );

         drawRouteWithInfo(route, commission, color);
      }

      // Draw custom route
      if (customRoute != null) {
         drawRouteWithCustomInfo(customRoute, customTargetName, Color.WHITE);
      }
   }

   /**
    * Draw a route with waypoint markers and commission info.
    */
   private void drawRouteWithInfo(Route route, Commission commission, Color color) {
      if (route.isEmpty()) {
         return;
      }

      // Draw each waypoint
      for (int i = 0; i < route.size(); i++) {
         RouteWaypoint waypoint = route.get(i);
         BlockPos pos = waypoint.toBlockPos();

         // Draw waypoint block
         RenderUtil.drawBlock(pos, color);

         // Draw waypoint label (show only for first and last, or every 3rd to avoid clutter)
         if (i == 0 || i == route.size() - 1 || i % 3 == 0) {
            String label = (i + 1) + ". " + waypoint.getTransportMethod().name().charAt(0);
            RenderUtil.drawText(label, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 0.6F);
         }

         // Draw line from previous waypoint
         if (i > 0) {
            RouteWaypoint prevWaypoint = route.get(i - 1);
            RenderUtil.drawLine(
                    prevWaypoint.toVec3d().add(0.5, 0.5, 0.5),
                    waypoint.toVec3d().add(0.5, 0.5, 0.5),
                    color
            );
         }
      }

      // Draw destination marker with commission name
      RouteWaypoint lastWaypoint = route.get(route.size() - 1);
      BlockPos endPos = lastWaypoint.toBlockPos();

      // Calculate distance from player
      double distance = Math.sqrt(mc.player.distanceToSqr(endPos.getX() + 0.5, endPos.getY() + 0.5, endPos.getZ() + 0.5));

      String endLabel = "§l" + commission.getName() + "§r §7(" + String.format("%.1f", distance) + "m)";
      RenderUtil.drawText(endLabel, endPos.getX() + 0.5, endPos.getY() + 2.2, endPos.getZ() + 0.5, 0.8F);
   }

   private void drawRouteWithCustomInfo(Route route, String name, Color color) {
      if (route.isEmpty()) return;

      for (int i = 0; i < route.size(); i++) {
         RouteWaypoint waypoint = route.get(i);
         BlockPos pos = waypoint.toBlockPos();
         RenderUtil.drawBlock(pos, color);
         if (i > 0) {
            RouteWaypoint prev = route.get(i - 1);
            RenderUtil.drawLine(prev.toVec3d().add(0.5, 0.5, 0.5), waypoint.toVec3d().add(0.5, 0.5, 0.5), color);
         }
      }

      BlockPos endPos = route.get(route.size() - 1).toBlockPos();
      double distance = Math.sqrt(mc.player.distanceToSqr(endPos.getX() + 0.5, endPos.getY() + 0.5, endPos.getZ() + 0.5));
      String endLabel = "§l" + name + "§r §7(" + String.format("%.1f", distance) + "m)";
      RenderUtil.drawText(endLabel, endPos.getX() + 0.5, endPos.getY() + 2.2, endPos.getZ() + 0.5, 0.8F);
   }

   @Override
    protected void onHudRender(GuiGraphicsExtractor drawContext) {
      if (!this.enabled) {
         return;
      }

      if (mc.player == null) {
         return;
      }

      // Build info lines for HUD overlay
      List<String> infoLines = new ArrayList<>();
      infoLines.add("§6§l[Commission Debug Mode]§r");
      infoLines.add("");

      if (calculatedRoutes.isEmpty()) {
         infoLines.add("§cNo routes calculated");
         infoLines.add("§7Make sure you're in Dwarven Mines");
      } else {
         infoLines.add("§aRoutes Calculated: §f" + calculatedRoutes.size());
         infoLines.add("");

         for (Map.Entry<Commission, Route> entry : calculatedRoutes.entrySet()) {
            Commission commission = entry.getKey();
            Route route = entry.getValue();
            Color color = routeColors.get(commission);

            String colorCode = getColorCode(color);
            double distance = 0;
            if (!route.isEmpty()) {
               BlockPos endPos = route.get(route.size() - 1).toBlockPos();
               distance = Math.sqrt(mc.player.distanceToSqr(endPos.getX() + 0.5, endPos.getY() + 0.5, endPos.getZ() + 0.5));
            }

            infoLines.add(colorCode + "● §f" + commission.getName() + " §7- " +
                                  route.size() + " nodes, " + String.format("%.0f", distance) + "m");
         }
      }

      infoLines.add("");
      if (simulatedTargetBlock != null) {
         infoLines.add("§6Target Block Found:");
         infoLines.add("§f " + simulatedTargetBlock.getX() + ", " + simulatedTargetBlock.getY() + ", " + simulatedTargetBlock.getZ());
         infoLines.add("§7 " + simulatedTargetReason);
         infoLines.add("§bCandidates: §f" + debugCandidates.size() + " §cObstructed: §f" + debugObstructed.size());
      } else {
         infoLines.add("§7No mineable blocks in range");
         if (!simulatedTargetReason.isEmpty()) infoLines.add("§8(" + simulatedTargetReason + ")");
      }

      infoLines.add("");
      infoLines.add("§7Press §e" + getKeybindName() + "§7 to toggle");

      RenderUtil.drawMultiLineText(drawContext, infoLines, Color.WHITE, 0.8F);
   }

   private String getColorCode(Color color) {
      if (color.getRed() > 200 && color.getGreen() < 150 && color.getBlue() < 150) return "§c";
      if (color.getGreen() > 200 && color.getRed() < 150 && color.getBlue() < 150) return "§a";
      if (color.getBlue() > 200 && color.getRed() < 150 && color.getGreen() < 150) return "§9";
      if (color.getRed() > 200 && color.getGreen() > 200 && color.getBlue() < 150) return "§e";
      if (color.getRed() > 200 && color.getBlue() > 200 && color.getGreen() < 150) return "§d";
      if (color.getGreen() > 200 && color.getBlue() > 200 && color.getRed() < 150) return "§b";
      if (color.getRed() > 200 && color.getGreen() > 100 && color.getBlue() < 50) return "§6";
      return "§5";
   }

   private String getKeybindName() {
      try {
         int keyCode = VeinForge.config().debug.commissionDebugKeybind;
         return InputConstants.Type.KEYSYM.getOrCreate(keyCode).getDisplayName().getString();
      } catch (Exception e) {
         return "F7";
      }
   }
}
