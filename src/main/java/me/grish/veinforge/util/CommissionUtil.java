package me.grish.veinforge.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import kotlin.Pair;
import me.grish.veinforge.VeinForge;
import me.grish.veinforge.macro.impl.CommissionMacro.Commission;
import me.grish.veinforge.util.tablist.TabListParser;
import me.grish.veinforge.util.tablist.WidgetType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommissionUtil {

    public static final List<Pair<String, Vec3>> emissaries = Arrays.asList(
            new Pair<>("Ceanna", new Vec3(42.50, 134.50, 22.50)),
            new Pair<>("Carlton", new Vec3(-72.50, 153.00, -10.50)),
            new Pair<>("Wilson", new Vec3(171.50, 150.00, 31.50)),
            new Pair<>("Lilith", new Vec3(58.50, 198.00, -8.50)),
            new Pair<>("Fraiser", new Vec3(-132.50, 174.00, -50.50))
    );
    private static final Pattern HOTM_XP_PATTERN =
            Pattern.compile("\\+(\\d{1,3}(?:,\\d{3})*)\\s+hotm\\s+(?:xp|exp)",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern COMMISSION_PROGRESS_PATTERN =
            Pattern.compile("(\\d{1,3}(?:\\.\\d+)?)\\s*%");
    private static final double COMPLETE_PROGRESS_THRESHOLD = 0.999D;
    private static final Minecraft mc = Minecraft.getInstance();

    private static final Map<Commission, Set<String>> slayerMob = ImmutableMap.of(
            Commission.GOBLIN_SLAYER, ImmutableSet.of("Goblin", "Knifethrower", "Fireslinger"),
            Commission.MINES_SLAYER, ImmutableSet.of("Goblin", "Knifethrower", "Fireslinger", "Glacite Walker"),
            Commission.GLACITE_WALKER_SLAYER, ImmutableSet.of("Glacite Walker"));

    public static Set<String> getMobForCommission(Commission commission) {
        return slayerMob.get(commission);
    }

    public static Optional<Player> getEmissary(Vec3 pos) {
        if (mc.level == null) return Optional.empty();
        return mc.level.players().stream()
                .filter(entity -> entity.getX() == pos.x && entity.getY() == pos.y && entity.getZ() == pos.z
                        && !entity.getName().getString().contains("Sentry")
                        && EntityUtil.isNpc(entity))
                .findFirst()
                .map(it -> (Player) it);
    }

    public static Optional<Player> getClosestEmissary() {
        if (mc.player == null || mc.level == null) return Optional.empty();
        Vec3 pos = emissaries
                .stream()
                .min(Comparator.comparing(it -> mc.player.position().distanceToSqr(it.getSecond())))
                .map(Pair::getSecond)
                .orElse(null);
        if (pos == null) {
            return Optional.empty();
        }

        return mc.level.players().stream()
                .filter(entity -> entity.getX() == pos.x && entity.getY() == pos.y && entity.getZ() == pos.z
                        && !entity.getName().getString().contains("Sentry")
                        && EntityUtil.isNpc(entity))
                .findFirst()
                .map(it -> (Player) it);
    }

    public static Vec3 getClosestEmissaryPosition() {
        if (mc.player == null) return null;
        return emissaries
                .stream()
                .min(Comparator.comparing(it -> mc.player.position().distanceToSqr(it.getSecond())))
                .map(Pair::getSecond)
                .orElse(null);
    }

    public static List<Commission> getCurrentCommissionsFromTablist() {
        List<Commission> parsedFromWidgets = getCurrentCommissionsFromWidgetTablist();
        if (!parsedFromWidgets.isEmpty()) {
            return parsedFromWidgets;
        }

        Map<Commission, Double> commsWithProgress = new LinkedHashMap<>();
        boolean foundCommission = false;
        for (final String text : TablistUtil.getCachedTablist()) {
            if (!foundCommission) {
                if (text.equalsIgnoreCase("Commissions:")) {
                    foundCommission = true;
                }
                continue;
            }

            if (isClaimIndicatorLine(text)) {
                return Collections.singletonList(Commission.COMMISSION_CLAIM);
            }

            String[] split = text.split(":", 2);
            Commission comm = Commission.getCommission(split[0].trim());
            if (comm != null) {
                double progressRatio = parseCommissionProgressRatio(split.length > 1 ? split[1] : text);
                if (isCompletedProgress(progressRatio)) {
                    return Collections.singletonList(Commission.COMMISSION_CLAIM);
                }
                commsWithProgress.merge(comm, progressRatio, Math::max);
            }

            if (text.isEmpty()) {
                break;
            }
        }

        return Commission.getBestCommissionFrom(commsWithProgress);
    }

    private static List<Commission> getCurrentCommissionsFromWidgetTablist() {
        TabListParser.updateCache();
        var data = TabListParser.getCached();
        if (data == null || data.widgetLines == null) {
            return Collections.emptyList();
        }

        List<String> lines = data.widgetLines.get(WidgetType.COMMISSIONS);
        if (lines == null || lines.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Commission, Double> commsWithProgress = new LinkedHashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);
            if (raw == null) {
                continue;
            }

            String text = ScoreboardUtil.sanitizeString(raw).trim();
            if (text.isEmpty()) {
                continue;
            }

            // Skip widget header line.
            if (i == 0 && (text.equalsIgnoreCase("Commissions") || text.equalsIgnoreCase("Commissions:") || text.equalsIgnoreCase("Commission") || text.equalsIgnoreCase("Commission:"))) {
                continue;
            }

            if (isClaimIndicatorLine(text)) {
                return Collections.singletonList(Commission.COMMISSION_CLAIM);
            }

            String left = text;
            int idx = text.indexOf(":");
            if (idx != -1) {
                left = text.substring(0, idx);
            }

            Commission comm = Commission.getCommission(left.trim());
            if (comm != null) {
                double progressRatio = parseCommissionProgressRatio(text);
                if (isCompletedProgress(progressRatio)) {
                    return Collections.singletonList(Commission.COMMISSION_CLAIM);
                }
                commsWithProgress.merge(comm, progressRatio, Math::max);
            }
        }

        return Commission.getBestCommissionFrom(commsWithProgress);
    }

    public static int getClaimableCommissionSlot() {
        if (mc.player == null || !(mc.player.containerMenu instanceof ChestMenu chest)) {
            return -1;
        }
        // In Fabric, we access the inventory via the container's slots
        for (int i = 0; i < chest.slots.size(); i++) {
            final ItemStack stack = chest.getSlot(i).getItem();
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            for (final String lore : InventoryUtil.getItemLore(stack)) {
                String cleanedLore = cleanLore(lore).trim();
                if (cleanedLore.equalsIgnoreCase("completed")) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static long getClaimableCommissionHotmXp(int slot) {
        if (slot == -1) {
            return 0L;
        }
        return parseHotmXpFromLore(InventoryUtil.getLoreOfItemInContainer(slot));
    }

    private static long parseHotmXpFromLore(List<String> lore) {
        long total = 0L;

        if (lore == null) {
            return 0L;
        }

        for (String rawLine : lore) {
            if (rawLine == null) {
                continue;
            }

            String cleanedLine = cleanLore(rawLine);

            Matcher matcher = HOTM_XP_PATTERN.matcher(cleanedLine);
            if (!matcher.find()) {
                continue;
            }

            String value = matcher.group(1).replace(",", "");

            try {
                long xp = Long.parseLong(value);
                total += xp;
                Logger.sendLog("Added " + xp + " XP (running total=" + total + ")");
            } catch (NumberFormatException e) {
                Logger.sendWarning("Failed to parse HOTM XP number: '" + value + "'");
            }
        }
        return total;
    }

    private static String cleanLore(String s) {
        if (s == null) return "";
        return ChatFormatting.stripFormatting(s).replace('\u00A0', ' ');
    }

    public static List<Commission> getCommissionFromContainer(ChestMenu container) {
        Map<Commission, Double> commsWithProgress = new LinkedHashMap<>();
        // Assuming the first 27-54 slots are the chest, and the last 36 are player inventory
        // But we usually only care about the chest slots for commissions
        for (int i = 0; i < container.slots.size(); i++) {
            final ItemStack stack = container.getSlot(i).getItem();
            if (stack == null || stack.isEmpty() || !ChatFormatting.stripFormatting(stack.getHoverName().getString()).startsWith("Commission")) {
                continue;
            }
            List<String> loreList = InventoryUtil.getItemLore(stack);
            for (int j = 0; j < loreList.size(); j++) {
                if (loreList.get(j).isEmpty()) {
                    if (j + 1 < loreList.size()) {
                        Commission comm = Commission.getCommission(cleanLore(loreList.get(++j)).trim());
                        if (comm != null) {
                            if (comm == Commission.COMMISSION_CLAIM) {
                                return Collections.singletonList(comm);
                            }
                            double progressRatio = parseCommissionProgressFromLore(loreList);
                            if (isCompletedProgress(progressRatio)) {
                                return Collections.singletonList(Commission.COMMISSION_CLAIM);
                            }
                            commsWithProgress.merge(comm, progressRatio, Math::max);
                        }
                    }
                    break;
                }
            }
        }
        return Commission.getBestCommissionFrom(commsWithProgress);
    }

    private static double parseCommissionProgressFromLore(List<String> loreList) {
        if (loreList == null) {
            return 0.0;
        }

        double progressRatio = 0.0;
        for (String loreLine : loreList) {
            String cleaned = cleanLore(loreLine);
            if (cleaned.equalsIgnoreCase("completed")) {
                return 1.0;
            }
            progressRatio = Math.max(progressRatio, parseCommissionProgressRatio(cleaned));
        }
        return progressRatio;
    }

    private static double parseCommissionProgressRatio(String text) {
        if (text == null) {
            return 0.0;
        }

        String upper = text.toUpperCase(Locale.ROOT);
        if (upper.contains("DONE") || upper.contains("COMPLETED") || upper.contains("COMPLETE")) {
            return 1.0;
        }

        Matcher matcher = COMMISSION_PROGRESS_PATTERN.matcher(text);
        if (!matcher.find()) {
            return 0.0;
        }

        try {
            double percent = Double.parseDouble(matcher.group(1));
            double ratio = percent / 100.0;
            return Math.max(0.0, Math.min(1.0, ratio));
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }

    private static boolean isClaimIndicatorLine(String text) {
        if (text == null) {
            return false;
        }

        String upper = text.toUpperCase(Locale.ROOT);
        return upper.contains("DONE");
    }

    private static boolean isCompletedProgress(double progressRatio) {
        return progressRatio >= COMPLETE_PROGRESS_THRESHOLD;
    }

    public static List<LivingEntity> getMobList(String mobName, Set<LivingEntity> mobsToIgnore) {
        if (mc.level == null || mc.player == null) return new ArrayList<>();
        List<LivingEntity> mobs = new ArrayList<>();
        for (Entity mob : mc.level.entitiesForRendering()) {
            if (mob instanceof LivingEntity && mob.getName().getString().trim().equals(mobName) && mob.isAlive() && !mobsToIgnore.contains(mob)) {
                mobs.add((LivingEntity) mob);
            }
        }

        Vec3 playerPos = mc.player.position();
        float normalizedYaw = AngleUtil.normalizeAngle(mc.player.getYRot());
        mobs.sort(Comparator.comparingDouble(mob -> {
                    Vec3 mobPos = mob.position();
                    double distanceCost =
                            Math.hypot(playerPos.x - mobPos.x, playerPos.z - mobPos.z) + Math.abs(mobPos.y - playerPos.y) * 2;
                    double angleCost = Math.abs(AngleUtil.normalizeAngle((normalizedYaw - AngleUtil.getRotation(mob).yaw)));
                    return distanceCost * VeinForge.config().debug.mobKillerDistCost + angleCost * VeinForge.config().debug.mobKillerRotCost;
                }
        ));
        return mobs;
    }

    public static List<Pair<Player, Pair<Double, Double>>> getMobListDebug(String mobName, Set<Player> mobsToIgnore) {
        if (mc.level == null || mc.player == null) return new ArrayList<>();
        List<Pair<Player, Pair<Double, Double>>> mobs = new ArrayList<>();
        Vec3 playerPos = mc.player.position();
        float normalizedYaw = AngleUtil.normalizeAngle(mc.player.getYRot());
        for (Player mob : mc.level.players()) {
            if (mob.getName().getString().trim().equals(mobName) && mob.isAlive() && !mobsToIgnore.contains(mob)) {
                Vec3 mobPos = mob.position();
                double distanceCost =
                        Math.hypot(playerPos.x - mobPos.x, playerPos.z - mobPos.z) + Math.abs(mobPos.y - playerPos.y) * 2;
                double angleCost = Math.abs(AngleUtil.normalizeAngle((normalizedYaw - AngleUtil.getRotation(mob).yaw)));
                mobs.add(new Pair<>(mob, new Pair<>(distanceCost * 0.6, angleCost * 0.1)));
            }
        }

        mobs.sort(Comparator.comparing(a -> {
            Pair<Double, Double> b = a.getSecond();
            return b.getFirst() + b.getSecond();
        }));
        return mobs;
    }
}
