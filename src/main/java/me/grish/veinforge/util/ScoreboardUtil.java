package me.grish.veinforge.util;

import me.grish.veinforge.event.UpdateScoreboardEvent;
import me.grish.veinforge.event.UpdateScoreboardLineEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScoreboardUtil {

    private static final Minecraft mc = Minecraft.getInstance();

    private static final CopyOnWriteArrayList<String> cachedScoreboard = new CopyOnWriteArrayList<>();
    private static final Pattern COLD_REGEX = Pattern.compile("Cold: -?(\\d{1,3})");

    public static int cold = 0;

    private static Object lastWorld = null;
    private static String lastColdLine = null;

    public static void update() {
        if (lastWorld != mc.level) {
            lastWorld = mc.level;
            cachedScoreboard.clear();
            cold = 0;
            lastColdLine = null;
        }

        List<String> newLines = scrapeSidebarLines();

        if (!cachedScoreboard.equals(newLines)) {
            cachedScoreboard.clear();
            cachedScoreboard.addAll(newLines);
            UpdateScoreboardEvent.fire(List.copyOf(newLines));
        }

        String coldLine = null;
        for (String line : newLines) {
            if (line.contains("Cold:")) {
                coldLine = line;
                break;
            }
        }

        if (!Objects.equals(lastColdLine, coldLine)) {
            UpdateScoreboardLineEvent.fire(coldLine != null ? coldLine : "Cold:");
            lastColdLine = coldLine;
        }
    }

    public static List<String> getScoreboard() {
        return new ArrayList<>(cachedScoreboard);
    }

    public static Objective getSidebarObjective() {
        if (mc.level == null) {
            return null;
        }
        Scoreboard scoreboard = mc.level.getScoreboard();
        if (scoreboard == null) {
            return null;
        }

        return scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
    }

    /**
     * Returns a best-effort snapshot of raw sidebar entries (unfiltered).
     * Intended for debugging scoreboard parsing issues.
     */
    public static List<PlayerScoreEntry> getSidebarEntriesRaw() {
        if (mc.level == null) {
            return Collections.emptyList();
        }

        Scoreboard scoreboard = mc.level.getScoreboard();
        if (scoreboard == null) {
            return Collections.emptyList();
        }

        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) {
            return Collections.emptyList();
        }

        try {
            return new ArrayList<>(scoreboard.listPlayerScores(objective));
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    /**
     * Scrapes the current sidebar lines using the same logic as the periodic cache update.
     */
    public static List<String> scrapeSidebarLinesNow() {
        return scrapeSidebarLines();
    }

    private static List<String> scrapeSidebarLines() {
        if (mc.level == null) {
            return Collections.emptyList();
        }

        Scoreboard scoreboard = mc.level.getScoreboard();
        if (scoreboard == null) {
            return Collections.emptyList();
        }

        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) {
            return Collections.emptyList();
        }

        List<PlayerScoreEntry> entries;
        try {
            entries = new ArrayList<>(scoreboard.listPlayerScores(objective));
        } catch (Exception ignored) {
            return Collections.emptyList();
        }

        entries.removeIf(entry -> entry == null
                || entry.owner() == null
                || entry.owner().startsWith("#")
                || entry.isHidden());

        entries.sort(Comparator
                .comparingInt(PlayerScoreEntry::value)
                .reversed()
                .thenComparing(PlayerScoreEntry::owner));

        if (entries.size() > 15) {
            entries = entries.subList(0, 15);
        }

        List<String> lines = new ArrayList<>(entries.size());
        for (PlayerScoreEntry entry : entries) {
            // Hypixel-style scoreboards store the visible text in the team's prefix/suffix.
            // The score holder itself is usually a dummy formatting code (e.g. "§w").
            final String owner = entry.owner();
            final PlayerTeam team = getTeamForScoreHolder(scoreboard, owner);

            if (team != null) {
                final String prefix = team.getPlayerPrefix() != null ? team.getPlayerPrefix().getString() : "";
                final String suffix = team.getPlayerSuffix() != null ? team.getPlayerSuffix().getString() : "";

                // The score holder is usually a dummy formatting code; don't rely on it for text.
                // Keep Unicode markers (e.g. "⏣") so GameStateHandler can detect sub-locations.
                final String rendered = (prefix + suffix).trim();
                if (!rendered.isEmpty()) {
                    lines.add(rendered);
                    continue;
                }
                // If the team is present but has no visible text, fall back to the entry's name.
                Component name = entry.ownerName();
                if (name == null) {
                    name = Component.literal(owner);
                }
                final String fallback = name.getString().trim();
                if (!fallback.isEmpty()) {
                    lines.add(fallback);
                }
            } else {
                Component name = entry.ownerName();
                if (name == null) {
                    name = Component.literal(owner);
                }
                // Keep Unicode markers (e.g. "⏣") so GameStateHandler can detect sub-locations.
                final String rendered = name.getString().trim();
                if (!rendered.isEmpty()) {
                    lines.add(rendered);
                }
            }
        }

        return lines;
    }

    private static PlayerTeam getTeamForScoreHolder(Scoreboard scoreboard, String scoreHolder) {
        if (scoreboard == null || scoreHolder == null) {
            return null;
        }

        // This is the mapping-safe way (gets remapped in production).
        // Hypixel uses "fake players" as score holders, with the visible text stored in a Team.
        return scoreboard.getPlayersTeam(scoreHolder);
    }

    public static String getScoreboardTitle() {
        if (mc.level == null) {
            return "";
        }
        Scoreboard scoreboard = mc.level.getScoreboard();
        if (scoreboard == null) {
            return "";
        }

        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) {
            return "";
        }

        return sanitizeString(objective.getDisplayName().getString());
    }

    public static String sanitizeString(String scoreboard) {
        char[] arr = scoreboard.toCharArray();
        StringBuilder cleaned = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            char c = arr[i];
            if (c >= 32 && c < 127) {
                cleaned.append(c);
            }
            if (c == 167) {
                i++;
            }
        }
        return cleaned.toString();
    }

    public static void onScoreboardLineUpdate(UpdateScoreboardLineEvent event) {
        String line = event.line();
        if (line == null) {
            cold = 0;
            return;
        }

        if (line.contains("Cold:")) {
            Matcher coldMatcher = COLD_REGEX.matcher(line);
            if (coldMatcher.find()) {
                cold = Integer.parseInt(coldMatcher.group(1));
            } else {
                cold = 0;
            }
        }

        if (cachedScoreboard.stream().noneMatch(l -> l != null && l.contains("Cold:"))) {
            cold = 0;
        }
    }

    public void onChatDetection(String message) {
        if (message == null) return;

        String cleanMessage = ChatFormatting.stripFormatting(message);
        if (cleanMessage != null && cleanMessage.contains("The warmth of the campfire reduced your") && cleanMessage.contains("Cold")) {
            cold = 0;
        }
    }

}
