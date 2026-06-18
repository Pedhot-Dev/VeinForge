package me.grish.veinforge.command;

import me.grish.veinforge.VeinForge;
import me.grish.veinforge.feature.impl.AutoGetStats.AutoGetStats;
import me.grish.veinforge.feature.impl.AutoGetStats.tasks.TaskStatus;
import me.grish.veinforge.feature.impl.AutoGetStats.tasks.impl.MiningSpeedRetrievalTask;
import me.grish.veinforge.feature.impl.AutoGetStats.tasks.impl.PickaxeAbilityRetrievalTask;
import me.grish.veinforge.feature.impl.AutoMobKiller.AutoMobKiller;
import me.grish.veinforge.feature.impl.BlockMiner.BlockMiner;
import me.grish.veinforge.feature.impl.Pathfinder;
import me.grish.veinforge.feature.impl.RouteNavigator;
import me.grish.veinforge.handler.GameStateHandler;
import me.grish.veinforge.handler.GraphHandler;
import me.grish.veinforge.macro.impl.CommissionMacro.Commission;
import me.grish.veinforge.macro.impl.CommissionMacro.CommissionMacro;
import me.grish.veinforge.pathfinder.calculate.PathfindingTelemetry;
import me.grish.veinforge.util.*;
import me.grish.veinforge.util.helper.MineableBlock;
import me.grish.veinforge.util.helper.graph.Graph;
import me.grish.veinforge.util.helper.location.Location;
import me.grish.veinforge.util.helper.location.SubLocation;
import me.grish.veinforge.util.helper.route.Route;
import me.grish.veinforge.util.helper.route.RouteWaypoint;
import me.grish.veinforge.util.tablist.TabListParser;
import me.grish.veinforge.util.tablist.WidgetType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;

import java.util.*;

public class DebugCommand {
    private static final Map<String, SubLocation> DWARVEN_PATH_TARGETS = new LinkedHashMap<>();
    private static final Map<String, Commission> SLAYER_DEBUG_TARGETS = new LinkedHashMap<>();

    static {
        DWARVEN_PATH_TARGETS.put("forge_basin", SubLocation.FORGE_BASIN);
        DWARVEN_PATH_TARGETS.put("the_forge", SubLocation.THE_FORGE);
        DWARVEN_PATH_TARGETS.put("cliffside_veins", SubLocation.CLIFFSIDE_VEINS);
        DWARVEN_PATH_TARGETS.put("royal_mines", SubLocation.ROYAL_MINES);
        DWARVEN_PATH_TARGETS.put("great_ice_wall", SubLocation.GREAT_ICE_WALL);
        DWARVEN_PATH_TARGETS.put("goblin_burrows", SubLocation.GOBLIN_BURROWS);
        DWARVEN_PATH_TARGETS.put("ramparts_quarry", SubLocation.RAMPARTS_QUARRY);
        DWARVEN_PATH_TARGETS.put("upper_mines", SubLocation.UPPER_MINES);
        DWARVEN_PATH_TARGETS.put("treasure_hunter_camp", SubLocation.TREASURE_HUNTER_CAMP);
        DWARVEN_PATH_TARGETS.put("lava_springs", SubLocation.LAVA_SPRINGS);

        SLAYER_DEBUG_TARGETS.put("goblin", Commission.GOBLIN_SLAYER);
        SLAYER_DEBUG_TARGETS.put("goblin_slayer", Commission.GOBLIN_SLAYER);
        SLAYER_DEBUG_TARGETS.put("glacite", Commission.GLACITE_WALKER_SLAYER);
        SLAYER_DEBUG_TARGETS.put("glacite_walker", Commission.GLACITE_WALKER_SLAYER);
        SLAYER_DEBUG_TARGETS.put("glacite_walker_slayer", Commission.GLACITE_WALKER_SLAYER);
        SLAYER_DEBUG_TARGETS.put("mines", Commission.MINES_SLAYER);
        SLAYER_DEBUG_TARGETS.put("mines_slayer", Commission.MINES_SLAYER);
    }

    private MiningSpeedRetrievalTask debugMiningSpeedTask;
    private PickaxeAbilityRetrievalTask debugPickaxeAbilityTask;

    private static String safeObjectiveName(Objective objective) {
        try {
            return objective.getName();
        } catch (Exception ignored) {
            return "<unknown>";
        }
    }

    private static String escapeForDump(String s) {
        if (s == null) {
            return "";
        }
        // Keep the output copy/paste friendly.
        return s.replace("\\", "\\\\")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                .replace("\"", "\\\"");
    }

    public void main() {
        Logger.sendMessage("Debug commands:");
        Logger.sendMessage("/debug item");
        Logger.sendMessage("/debug nbt");
        Logger.sendMessage("/debug location");
        Logger.sendMessage("/debug sublocation");
        Logger.sendMessage("/debug where");
        Logger.sendMessage("/debug scoreboard");
        Logger.sendMessage("/debug tablist");
        Logger.sendMessage("/debug commission");
        Logger.sendMessage("/debug footer");
        Logger.sendMessage("/debug state");
        Logger.sendMessage("/debug fishing-stage");
        Logger.sendMessage("/debug entities");
        Logger.sendMessage("/debug slayer start <mob>");
        Logger.sendMessage("/debug slayer status");
        Logger.sendMessage("/debug slayer stop");
        Logger.sendMessage("/debug path <x> <y> <z> [render_only]");
        Logger.sendMessage("/debug path location <location> [render_only]");
        Logger.sendMessage("/debug path stop");
        Logger.sendMessage("/debug path stats");
    }

    public void item() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            Logger.sendError("Player not available.");
            return;
        }
        int slot = mc.player.getInventory().getSelectedSlot();
        ItemStack stack = mc.player.getInventory().getItem(slot);
        if (stack == null || stack.isEmpty()) {
            Logger.sendError("No item in current hotbar slot.");
            return;
        }
        String displayName = ChatFormatting.stripFormatting(stack.getHoverName().getString());
        String internalId = InventoryUtil.getItemId(stack);
        Item item = stack.getItem();
        int itemId = BuiltInRegistries.ITEM.getId(item);
        String translationKey = item.getDescriptionId();
        String nbt = stack.getComponents().toString();
        Logger.sendMessage("Hotbar slot: " + (slot + 1));
        Logger.sendMessage("Display: " + displayName);
        Logger.sendMessage("Item ID: " + internalId);
        Logger.sendMessage("Minecraft Raw ID: " + itemId);
        Logger.sendMessage("Translation Key: " + translationKey);
        Logger.sendMessage("Components: " + nbt);
    }

    public void nbt() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            Logger.sendError("Player not available.");
            return;
        }

        int slot = mc.player.getInventory().getSelectedSlot();
        ItemStack stack = mc.player.getInventory().getItem(slot);
        if (stack == null || stack.isEmpty()) {
            Logger.sendError("No item in current hotbar slot.");
            return;
        }

        String displayName = ChatFormatting.stripFormatting(stack.getHoverName().getString());
        String internalId = InventoryUtil.getItemId(stack);
        Item item = stack.getItem();
        int itemId = BuiltInRegistries.ITEM.getId(item);
        String translationKey = item.getDescriptionId();
        String components = stack.getComponents().toString();

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag customTag = customData == null ? null : customData.copyTag();

        StringBuilder out = new StringBuilder(4096);
        out.append("=== VeinForge Held Item NBT Dump ===\n");
        out.append("slot=").append(slot + 1).append('\n');
        out.append("count=").append(stack.getCount()).append('\n');
        out.append("display=\"").append(escapeForDump(displayName)).append("\"\n");
        out.append("internal_id=\"").append(escapeForDump(internalId)).append("\"\n");
        out.append("item_raw_id=").append(itemId).append('\n');
        out.append("translation_key=\"").append(escapeForDump(translationKey)).append("\"\n");
        out.append("components=\"").append(escapeForDump(components)).append("\"\n");
        if (customTag == null || customTag.isEmpty()) {
            out.append("custom_data=<none>\n");
        } else {
            out.append("custom_data=\"").append(escapeForDump(customTag.toString())).append("\"\n");
        }
        out.append("=== End VeinForge Held Item NBT Dump ===");

        Logger.sendConsoleRaw(out.toString());
        Logger.sendMessage("Dumped held item NBT/components to console.");
    }

    public void location() {
        GameStateHandler handler = GameStateHandler.getInstance();
        Location location = handler.getCurrentLocation();
        Logger.sendMessage("Location: " + location.name() + " (" + location.getName() + ")");
        Logger.sendMessage("Server IP: " + handler.getServerIp());
    }

    public void sublocation() {
        SubLocation subLocation = GameStateHandler.getInstance().getCurrentSubLocation();
        Logger.sendMessage("SubLocation: " + subLocation.name() + " (" + subLocation.getName() + ")");
    }

    public void where() {
        GameStateHandler handler = GameStateHandler.getInstance();
        Location location = handler.getCurrentLocation();
        SubLocation subLocation = handler.getCurrentSubLocation();
        Logger.sendLog("Location: " + location.name() + " (" + location.getName() + ")"
                + " | SubLocation: " + subLocation.name() + " (" + subLocation.getName() + ")");
    }

    public void scoreboard() {
        String title = ScoreboardUtil.getScoreboardTitle();
        List<String> lines = ScoreboardUtil.getScoreboard();
        Logger.sendMessage("Scoreboard title: " + title);
        Logger.sendMessage("Scoreboard lines: " + lines.size());
        int limit = Math.min(5, lines.size());
        for (int i = 0; i < limit; i++) {
            Logger.sendMessage("[" + i + "] " + ScoreboardUtil.sanitizeString(lines.get(i)));
        }
    }

    /**
     * Dumps the full sidebar scoreboard to the Minecraft console/stdout in one go.
     */
    public void scoreboardDump() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) {
            Logger.sendConsole("Scoreboard dump: world not available");
            return;
        }

        String title = ScoreboardUtil.getScoreboardTitle();
        List<String> cached = ScoreboardUtil.getScoreboard();
        List<String> live = ScoreboardUtil.scrapeSidebarLinesNow();
        List<PlayerScoreEntry> rawEntries = ScoreboardUtil.getSidebarEntriesRaw();
        Objective objective = ScoreboardUtil.getSidebarObjective();

        StringBuilder out = new StringBuilder(4096);
        out.append("=== VeinForge Sidebar Scoreboard Dump ===\n");
        out.append("time_ms=").append(System.currentTimeMillis()).append('\n');
        out.append("world=").append(mc.level.dimension().identifier()).append('\n');
        out.append("title=\"").append(escapeForDump(title)).append("\"\n");
        out.append("objective=").append(objective != null ? safeObjectiveName(objective) : "<null>").append('\n');
        out.append("cached_lines=").append(cached.size()).append('\n');
        for (int i = 0; i < cached.size(); i++) {
            out.append("cached[").append(i).append("]=\"")
                    .append(escapeForDump(Objects.toString(cached.get(i), "")))
                    .append("\"\n");
        }
        out.append("live_lines=").append(live.size()).append('\n');
        for (int i = 0; i < live.size(); i++) {
            out.append("live[").append(i).append("]=\"")
                    .append(escapeForDump(Objects.toString(live.get(i), "")))
                    .append("\"\n");
        }
        out.append("raw_entries=").append(rawEntries.size()).append('\n');
        for (int i = 0; i < rawEntries.size(); i++) {
            PlayerScoreEntry e = rawEntries.get(i);
            if (e == null) {
                out.append("raw[").append(i).append("]=<null>\n");
                continue;
            }
            String owner;
            try {
                owner = Objects.toString(e.owner(), "");
            } catch (Exception ex) {
                owner = "<error>";
            }
            String name;
            try {
                name = e.ownerName() != null ? e.ownerName().getString() : "";
            } catch (Exception ex) {
                name = "<error>";
            }
            boolean hidden;
            try {
                hidden = e.isHidden();
            } catch (Exception ex) {
                hidden = false;
            }
            int value;
            try {
                value = e.value();
            } catch (Exception ex) {
                value = 0;
            }

            out.append("raw[").append(i).append("] value=").append(value)
                    .append(" hidden=").append(hidden)
                    .append(" owner=\"").append(escapeForDump(owner)).append("\"")
                    .append(" name=\"").append(escapeForDump(name)).append("\"\n");
        }
        out.append("=== End VeinForge Sidebar Scoreboard Dump ===");

        Logger.sendConsoleRaw(out.toString());
    }

    public void tablist() {
        List<String> tablist = TablistUtil.getCachedTablist();
        Logger.sendMessage("Tablist lines: " + tablist.size());
        int limit = Math.min(5, tablist.size());
        for (int i = 0; i < limit; i++) {
            Logger.sendMessage("[" + i + "] " + tablist.get(i));
        }
    }

    public void commission() {
        List<Commission> comms = CommissionUtil.getCurrentCommissionsFromTablist();
        if (comms == null || comms.isEmpty()) {
            Logger.sendMessage("Commissions: none detected");
        } else {
            Logger.sendMessage("Commissions: " + comms.size());
            for (int i = 0; i < comms.size(); i++) {
                Logger.sendMessage("[" + i + "] " + comms.get(i).getName());
            }
        }

        // Also show what the widget parser sees (helpful when commissions don't parse).
        try {
            TabListParser.updateCache();
            var data = TabListParser.getCached();
            var lines = data != null ? data.widgetLines.get(WidgetType.COMMISSIONS) : null;
            if (lines == null || lines.isEmpty()) {
                Logger.sendMessage("Tab widgets: COMMISSIONS not found");
            } else {
                Logger.sendMessage("Tab widgets COMMISSIONS lines: " + lines.size());
                int limit = Math.min(6, lines.size());
                for (int i = 0; i < limit; i++) {
                    Logger.sendMessage("[" + i + "] " + ScoreboardUtil.sanitizeString(lines.get(i)));
                }
            }
        } catch (Exception e) {
            Logger.sendMessage("Tab widgets parse failed: " + e.getMessage());
        }
    }

    public void footer() {
        List<String> footer = TablistUtil.getCachedTablistFooter();
        Logger.sendMessage("Tablist footer lines: " + footer.size());
        int limit = Math.min(5, footer.size());
        for (int i = 0; i < limit; i++) {
            Logger.sendMessage("[" + i + "] " + footer.get(i));
        }
    }

    public void state() {
        GameStateHandler handler = GameStateHandler.getInstance();
        Logger.sendMessage("Location: " + handler.getCurrentLocation().name());
        Logger.sendMessage("SubLocation: " + handler.getCurrentSubLocation().name());
        Logger.sendMessage("SkyBlock: " + handler.isPlayerInSkyBlock());
        Logger.sendMessage("GodPot: " + handler.isGodpotActive());
        Logger.sendMessage("Cookie: " + handler.isCookieActive());
    }

    public void fishingStage() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            Logger.sendError("Player/world not available.");
            return;
        }

        GameStateHandler handler = GameStateHandler.getInstance();
        GraphHandler graphHandler = GraphHandler.instance;
        Graph<RouteWaypoint> graph = graphHandler.getActiveGraph();

        BlockPos standing = PlayerUtil.getBlockStandingOn();
        Vec3 pos = mc.player.position();
        float yaw = mc.player.getYRot();
        float pitch = mc.player.getXRot();

        Logger.sendMessage("Fishing Stage Snapshot:");
        Logger.sendMessage("Graph: " + graphHandler.getActiveGraphKey()
                + " | nodes=" + (graph == null ? 0 : graph.map.size())
                + " | editing=" + graphHandler.isEditing());
        Logger.sendMessage("Location: " + handler.getCurrentLocation().name()
                + " | SubLocation: " + handler.getCurrentSubLocation().name());
        Logger.sendMessage("Block: x=" + standing.getX() + " y=" + standing.getY() + " z=" + standing.getZ());
        Logger.sendMessage(String.format(Locale.ROOT, "Pos: x=%.3f y=%.3f z=%.3f", pos.x, pos.y, pos.z));
        Logger.sendMessage(String.format(Locale.ROOT, "Look: yaw=%.2f pitch=%.2f", yaw, pitch));
        Logger.sendMessage(String.format(Locale.ROOT,
                "Copy: %d %d %d %.2f %.2f",
                standing.getX(),
                standing.getY(),
                standing.getZ(),
                yaw,
                pitch));

        RouteWaypoint nearest = findNearestWaypoint(graph, standing);
        if (nearest != null) {
            double dist = Math.sqrt(nearest.toBlockPos().distSqr(standing));
            Logger.sendMessage("Nearest Graph Node: " + nearest + " | dist=" + String.format(Locale.ROOT, "%.2f", dist));
        } else {
            Logger.sendMessage("Nearest Graph Node: none (active graph has no nodes)");
        }
    }

    public void entities() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            Logger.sendError("World or player not available.");
            return;
        }
        int total = 0;
        int armorStands = 0;
        int players = 0;
        int others = 0;
        List<Entity> nearby = new ArrayList<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == null) {
                continue;
            }
            total++;
            if (entity instanceof ArmorStand) {
                armorStands++;
            } else if (entity instanceof Player) {
                players++;
            } else {
                others++;
            }
            nearby.add(entity);
        }
        Logger.sendMessage("Entities: total=" + total + " stands=" + armorStands
                + " players=" + players + " others=" + others);
        nearby.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(mc.player)));
        int limit = Math.min(5, nearby.size());
        for (int i = 0; i < limit; i++) {
            Entity entity = nearby.get(i);
            String name;
            if (entity instanceof ArmorStand stand) {
                name = ChatFormatting.stripFormatting(stand.getCustomName() != null ? stand.getCustomName().getString() : stand.getName().getString());
            } else {
                name = ChatFormatting.stripFormatting(entity.getName().getString());
            }
            double dist = Math.sqrt(entity.distanceToSqr(mc.player));
            Logger.sendMessage("[" + i + "] " + entity.getClass().getSimpleName()
                    + " name=\"" + name + "\" dist=" + String.format("%.1f", dist));
        }
    }

    public void mineHereStop() {
        BlockMiner miner = BlockMiner.getInstance();
        if (miner.isRunning()) {
            miner.stop();
            Logger.sendMessage("Stopped BlockMiner test mining.");
        } else {
            Logger.sendMessage("BlockMiner test mining is not running.");
        }
    }

    public void mineHere(Integer overrideSpeed) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            Logger.sendError("Player/world not available.");
            return;
        }

        BlockMiner miner = BlockMiner.getInstance();
        if (miner.isRunning()) {
            miner.stop();
        }

        MineableBlock[] blocksToMine = {
                MineableBlock.GRAY_MITHRIL,
                MineableBlock.GREEN_MITHRIL,
                MineableBlock.BLUE_MITHRIL,
                MineableBlock.TITANIUM
        };
        int[] mithrilPriority = {10, 6, 3, 1};

        int speed;
        if (overrideSpeed != null) {
            speed = overrideSpeed;
        } else {
            speed = CommissionMacro.getInstance().getMiningSpeed();
        }

        if (speed <= 0) {
            Logger.sendError("Mining speed is 0. Use /debug minehere <speed> (e.g. /debug minehere 1500).\nOr start Commission Macro once so stats are loaded.");
            return;
        }

        BlockMiner.PickaxeAbility ability = CommissionMacro.getInstance().getPickaxeAbility();
        miner.setWaitThreshold(me.grish.veinforge.VeinForge.config().general.oreRespawnWaitThreshold * 1000);
        miner.start(blocksToMine, speed, ability, mithrilPriority, me.grish.veinforge.VeinForge.config().general.miningTool);

        Logger.sendMessage("Started mine-here test: mithril+titanium in current area.");
        Logger.sendMessage("Speed=" + speed + ", Ability=" + ability.name() + ", WaitThresholdMs=" + (me.grish.veinforge.VeinForge.config().general.oreRespawnWaitThreshold * 1000));
    }

    public List<String> getDwarvenPathTargetKeys() {
        return new ArrayList<>(DWARVEN_PATH_TARGETS.keySet());
    }

    public List<String> getSlayerDebugTargetKeys() {
        return new ArrayList<>(SLAYER_DEBUG_TARGETS.keySet());
    }

    public void slayerStop() {
        AutoMobKiller killer = AutoMobKiller.getInstance();
        if (killer.isRunning()) {
            killer.stop();
            Logger.sendMessage("Stopped slayer debug mob killer.");
            return;
        }
        Logger.sendMessage("Slayer debug mob killer is not running.");
    }

    public void slayerStatus() {
        AutoMobKiller killer = AutoMobKiller.getInstance();
        Logger.sendMessage("Slayer debug status: running=" + killer.isRunning() + ", state=" + killer.getCurrentStateName() + ", error=" + killer.getError().name() + ", blacklist=" + killer.getBlacklistedMobs().size());
        if (killer.getTargetMob() != null) {
            Logger.sendMessage("Target: " + killer.getTargetMob().getName().getString() + " @ " + killer.getTargetMob().blockPosition());
        }
    }

    public void slayerStart(String targetKey) {
        if (targetKey == null || targetKey.trim().isEmpty()) {
            Logger.sendError("Missing slayer target. Use one of: " + String.join(", ", getSlayerDebugTargetKeys()));
            return;
        }

        String key = targetKey.trim().toLowerCase(Locale.ROOT);
        Commission commission = SLAYER_DEBUG_TARGETS.get(key);
        if (commission == null) {
            Logger.sendError("Unknown slayer target: " + targetKey + ". Use one of: " + String.join(", ", getSlayerDebugTargetKeys()));
            return;
        }

        Set<String> mobNames = CommissionUtil.getMobForCommission(commission);
        if (mobNames == null || mobNames.isEmpty()) {
            Logger.sendError("No mob mapping exists for " + commission.getName());
            return;
        }

        String weapon = commission == Commission.GLACITE_WALKER_SLAYER
                ? VeinForge.config().general.miningTool
                : VeinForge.config().commission.dwarvenCommission.slayerWeapon;

        if (weapon == null || weapon.trim().isEmpty()) {
            Logger.sendError("Required weapon is not configured for " + commission.getName());
            return;
        }

        AutoMobKiller killer = AutoMobKiller.getInstance();
        if (killer.isRunning()) {
            killer.stop();
        }

        killer.start(mobNames, weapon);
        Logger.sendMessage("Started slayer debug: " + commission.getName());
        Logger.sendMessage("Mobs: " + String.join(", ", mobNames));
        Logger.sendMessage("Weapon: " + weapon);
    }

    public void pathDwarven(String targetKey) {
        pathDwarven(targetKey, false);
    }

    public void pathDwarven(String targetKey, boolean renderOnly) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            Logger.sendError("Player/world not available.");
            return;
        }

        if (targetKey == null) {
            Logger.sendError("Missing target. Use one of: " + String.join(", ", getDwarvenPathTargetKeys()));
            return;
        }

        String key = targetKey.trim().toLowerCase();
        SubLocation location = DWARVEN_PATH_TARGETS.get(key);
        if (location == null) {
            Logger.sendError("Unknown target: " + targetKey + ". Use one of: " + String.join(", ", getDwarvenPathTargetKeys()));
            return;
        }

        RouteWaypoint[] candidates = Commission.getWaypoints(location);
        if (candidates == null || candidates.length == 0) {
            Logger.sendError("No waypoint route is mapped for " + location.getName());
            return;
        }

        RouteWaypoint target = Arrays.stream(candidates)
                .min(Comparator.comparingDouble(it -> it.toVec3d().distanceTo(mc.player.position())))
                .orElse(null);

        if (target == null) {
            Logger.sendError("Cannot pick target waypoint for " + location.getName());
            return;
        }

        if (renderOnly) {
            RouteNavigator.getInstance().stop();
            Pathfinder pathfinder = Pathfinder.getInstance();
            if (pathfinder.isRunning() || pathfinder.isRenderOnlyMode()) {
                pathfinder.stop();
            }
            pathfinder.queue(target.toBlockPos());
            pathfinder.startRenderOnly();
            Logger.sendMessage("Render-only path preview to " + location.getName() + " (" + key + ")");
            return;
        }

        List<RouteWaypoint> nodes = GraphHandler.instance.findPathFrom("Commission Macro", PlayerUtil.getBlockStandingOn(), target);
        if (nodes == null || nodes.isEmpty()) {
            Logger.sendError("No graph path found from current position to " + location.getName());
            return;
        }

        RouteNavigator.getInstance().start(new Route(nodes));
        Logger.sendMessage("Pathfinding to " + location.getName() + " (" + key + ")");
    }

    public void pathToBlock(int x, int y, int z) {
        pathToBlock(x, y, z, false);
    }

    public void pathToBlock(int x, int y, int z, boolean renderOnly) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            Logger.sendError("Player/world not available.");
            return;
        }

        BlockPos target = new BlockPos(x, y, z);
        RouteNavigator.getInstance().stop();

        Pathfinder pathfinder = Pathfinder.getInstance();
        if (pathfinder.isRunning() || pathfinder.isRenderOnlyMode()) {
            pathfinder.stop();
        }
        pathfinder.queue(target);
        if (renderOnly) {
            pathfinder.startRenderOnly();
            Logger.sendMessage("Rendering-only path preview to block " + x + ", " + y + ", " + z);
            return;
        }
        pathfinder.start();

        Logger.sendMessage("Pathfinding to block " + x + ", " + y + ", " + z);
    }

    public void pathStop() {
        RouteNavigator.getInstance().stop();
        Pathfinder.getInstance().stop();
        Logger.sendMessage("Stopped debug pathing.");
    }

    public void pathStats() {
        PathfindingTelemetry telemetry = Pathfinder.getInstance().getLastTelemetry();
        if (telemetry == null) {
            Logger.sendWarning("No path telemetry recorded yet. Run /debug path first.");
            return;
        }

        String status = telemetry.success() ? "success" : "failed";
        String failureReason = telemetry.failureReason().isEmpty() ? "-" : telemetry.failureReason();
        Logger.sendMessage("[PathStats] status=" + status + " failure_reason=" + failureReason);
        Logger.sendMessage(String.format(Locale.ROOT,
                "[PathStats] search_ms=%.2f smoothing_ms=%.2f total_ms=%.2f",
                telemetry.searchMs(),
                telemetry.smoothingMs(),
                telemetry.getTotalMs()));
        Logger.sendMessage("[PathStats] expanded_nodes=" + telemetry.expandedNodes()
                + " open_set_peak=" + telemetry.openSetPeak()
                + " iterations=" + telemetry.iterations());
        Logger.sendMessage("[PathStats] path_length=" + telemetry.pathLength()
                + " smoothed_path_length=" + telemetry.smoothedPathLength()
                + " direct_walk=" + telemetry.directWalk());
    }


    public void getStatsStart() {
        AutoGetStats auto = AutoGetStats.getInstance();

        debugMiningSpeedTask = new MiningSpeedRetrievalTask();
        debugPickaxeAbilityTask = new PickaxeAbilityRetrievalTask();

        auto.startTask(debugMiningSpeedTask);
        auto.startTask(debugPickaxeAbilityTask);

        Logger.sendMessage("[GetStats] Starting stats retrieval probe. Use '/debug getstats status' to check.");
    }

    public void getStatsStatus() {
        AutoGetStats auto = AutoGetStats.getInstance();

        if (debugMiningSpeedTask == null || debugPickaxeAbilityTask == null) {
            Logger.sendWarning("No active GettingStats probe. Run /debug getstats start.");
            return;
        }

        TaskStatus miningStatus = debugMiningSpeedTask.getTaskStatus();
        TaskStatus abilityStatus = debugPickaxeAbilityTask.getTaskStatus();

        Logger.sendMessage("[GetStats] queueFinished=" + auto.hasFinishedAllTasks()
                + " miningStatus=" + miningStatus
                + " abilityStatus=" + abilityStatus);

        if (debugMiningSpeedTask.getError() != null) {
            Logger.sendError("[GetStats] MiningSpeed error: " + debugMiningSpeedTask.getError());
        }
        if (debugPickaxeAbilityTask.getError() != null) {
            Logger.sendError("[GetStats] PickaxeAbility error: " + debugPickaxeAbilityTask.getError());
        }

        if (miningStatus.isSuccessful()) {
            Logger.sendMessage("[GetStats] MiningSpeed result=" + debugMiningSpeedTask.getResult());
        }
        if (abilityStatus.isSuccessful()) {
            Logger.sendMessage("[GetStats] PickaxeAbility result=" + debugPickaxeAbilityTask.getResult());
        }
    }

    private RouteWaypoint findNearestWaypoint(Graph<RouteWaypoint> graph, BlockPos from) {
        if (graph == null || from == null || graph.map.isEmpty()) {
            return null;
        }

        RouteWaypoint nearest = null;
        double bestDistSq = Double.MAX_VALUE;
        for (RouteWaypoint waypoint : graph.map.keySet()) {
            if (waypoint == null) {
                continue;
            }
            double distSq = waypoint.toBlockPos().distSqr(from);
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                nearest = waypoint;
            }
        }
        return nearest;
    }
}
