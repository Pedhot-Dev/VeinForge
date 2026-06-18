package me.grish.veinforge.handler;

import lombok.Getter;
import me.grish.veinforge.VeinForge;
import me.grish.veinforge.event.UpdateScoreboardEvent;
import me.grish.veinforge.event.UpdateTablistEvent;
import me.grish.veinforge.event.UpdateTablistFooterEvent;
import me.grish.veinforge.util.InventoryUtil;
import me.grish.veinforge.util.Logger;
import me.grish.veinforge.util.ScoreboardUtil;
import me.grish.veinforge.util.helper.location.Location;
import me.grish.veinforge.util.helper.location.SubLocation;
import me.grish.veinforge.util.tablist.TabListParser;
import me.grish.veinforge.util.tablist.WidgetType;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks game state including location, server, and buff status.
 * Events are called from EventManager.
 */
public class GameStateHandler {

    private static final GameStateHandler instance = new GameStateHandler();
    private final Minecraft mc = Minecraft.getInstance();
    private final Pattern areaPattern = Pattern.compile("Area:\\s(.+)");


    @Getter
    private String serverIp = "";
    @Getter
    private Location currentLocation = Location.KNOWHERE;
    @Getter
    private SubLocation currentSubLocation = SubLocation.KNOWHERE;
    @Getter
    private boolean godpotActive = false;
    @Getter
    private boolean cookieActive = false;

    public static GameStateHandler getInstance() {
        return instance;
    }

    public boolean isPlayerInSkyBlock() {
        return this.currentLocation != Location.LIMBO
                && this.currentLocation != Location.LOBBY
                && this.currentLocation != Location.KNOWHERE;
    }

    public boolean inDwarvenMines() {
        return currentLocation == Location.DWARVEN_MINES;
    }

    // ==================== Event Handlers (called from EventManager) ====================

    /**
     * Called every tick.
     */
    public void onTick() {
        // Update server IP if available
        if (mc.getCurrentServer() != null && mc.getCurrentServer().ip != null) {
            this.serverIp = mc.getCurrentServer().ip;
        }
    }

    /**
     * Called when the world unloads.
     */
    public void onWorldUnload() {
        currentLocation = Location.KNOWHERE;
        currentSubLocation = SubLocation.KNOWHERE;
    }

    /**
     * Called when the world loads.
     */
    public void onWorldLoad() {
        if (mc.getCurrentServer() != null && mc.getCurrentServer().ip != null) {
            this.serverIp = mc.getCurrentServer().ip;
        }
    }

    /**
     * Called when the player dies.
     */
    public void onPlayerDeath(String damageType) {
        var config = VeinForge.config();
        if ("fall".equals(damageType) && config != null
                && config.general.ignoreFallDamageInPathfinding) {
            Logger.sendWarning("You died to fall damage while 'Ignore Fall Damage In Pathfinding' was enabled.");
        }
    }

    /**
     * Called when tablist updates.
     */
    public void onTablistUpdate(UpdateTablistEvent event) {
        if (event.tablist().isEmpty()) {
            return;
        }
        final List<String> tabList = event.tablist();
        final List<String> scoreboard = ScoreboardUtil.getScoreboard();

        // Prefer SkyBlock "Info" column widget parsing when available.
        try {
            var data = TabListParser.getCached();
            if (data != null && data.widgetLines != null) {
                List<String> infoLines = data.widgetLines.get(WidgetType.GENERAL_INFO);
                if (infoLines != null) {
                    for (String raw : infoLines) {
                        if (raw == null) {
                            continue;
                        }
                        String cleaned = ScoreboardUtil.sanitizeString(raw).trim();
                        if (!cleaned.startsWith("Area: ")) {
                            continue;
                        }
                        Matcher matcher = this.areaPattern.matcher(cleaned);
                        if (matcher.find()) {
                            String area = matcher.group(1);
                            this.currentLocation = Location.fromName(area);
                            return;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        if (tabList.size() == 1 && InventoryUtil.isInventoryEmpty()) {
            this.currentLocation = Location.LIMBO;
            this.currentSubLocation = SubLocation.KNOWHERE;
            return;
        }

        for (String tabline : tabList) {
            if (!tabline.startsWith("Area: ")) {
                continue;
            }
            final Matcher matcher = this.areaPattern.matcher(tabline);
            if (!matcher.find()) {
                return;
            }

            final String area = matcher.group(1);
            this.currentLocation = Location.fromName(area);
            return;
        }

        if (!ScoreboardUtil.getScoreboardTitle().contains("SKYBLOCK") && !scoreboard.isEmpty() && scoreboard.get(scoreboard.size() - 1).equalsIgnoreCase("www.hypixel.net")) {
            this.currentLocation = Location.LOBBY;
            return;
        }
        this.currentLocation = Location.KNOWHERE;
    }

    /**
     * Called when tablist footer updates.
     */
    public void onTablistFooterUpdate(UpdateTablistFooterEvent event) {
        final List<String> footer = event.footer();
        for (int i = 0; i < footer.size(); i++) {
            if (footer.get(i).contains("Active Effects")) {
                this.godpotActive = footer.get(++i).contains("You have a God Potion active!");
            }
            if (footer.get(i).contains("Cookie Buff")) {
                this.cookieActive = !footer.get(++i).contains("Not active!");
                break;
            }
        }
    }

    /**
     * Called when scoreboard updates.
     */
    public void onScoreboardUpdate(UpdateScoreboardEvent event) {
        SubLocation detected = SubLocation.KNOWHERE;
        String sourceLine = null;

        // Primary: SkyBlock scoreboard location line typically includes a marker icon (e.g. "⏣" or "ф").
        for (int i = 0; i < event.scoreboard().size(); i++) {
            final String line = event.scoreboard().get(i);
            if (line == null) {
                continue;
            }
            if (!(line.contains("⏣") || line.contains("ф"))) {
                continue;
            }

            detected = SubLocation.fromName(ScoreboardUtil.sanitizeString(line).trim());
            sourceLine = line;
            break;
        }

        // Fallback: some scoreboard implementations may drop the icon/prefix; try matching by name.
        if (detected == SubLocation.KNOWHERE) {
            for (int i = 0; i < event.scoreboard().size(); i++) {
                final String line = event.scoreboard().get(i);
                if (line == null) {
                    continue;
                }

                final String cleaned = ScoreboardUtil.sanitizeString(line).trim();
                if (cleaned.isEmpty() || cleaned.contains(":")) {
                    continue;
                }

                SubLocation candidate = SubLocation.fromName(cleaned);
                if (candidate != SubLocation.KNOWHERE) {
                    detected = candidate;
                    sourceLine = line;
                    break;
                }
            }
        }

        if (detected != SubLocation.KNOWHERE) {
            this.currentSubLocation = detected;
        }
    }
}
