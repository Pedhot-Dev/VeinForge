package me.grish.veinforge.util;

import me.grish.veinforge.VeinForge;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

public abstract class Logger {

    protected static final Minecraft mc = Minecraft.getInstance();
    private static final Map<String, String> lastMessages = new HashMap<>();

    public static void addMessage(String text) {
        if (mc.player == null || mc.level == null) {
            VeinForge.LOGGER.info("{}", ChatFormatting.stripFormatting(text));
        } else {
            // Always defer chat writes to avoid rendering-phase violations.
            mc.execute(() -> {
                if (mc.player != null && mc.level != null) {
                    mc.player.sendSystemMessage(Component.nullToEmpty(text));
                } else {
                    VeinForge.LOGGER.info("{}", ChatFormatting.stripFormatting(text));
                }
            });
        }
    }

    public static void sendMessage(final String message) {
        addMessage(formatPrefix("§bVeinForge", message));
        VeinForge.LOGGER.info("{}", message == null ? "null" : ChatFormatting.stripFormatting(message));
    }

    public static void sendWarning(final String message) {
        addMessage("§c§l[WARNING] §8» §e" + message);
        VeinForge.LOGGER.warn("{}", message == null ? "null" : ChatFormatting.stripFormatting(message));
    }

    public static void sendError(final String message) {
        addMessage("§l§4§kZ§r§l§4[VeinForge]§kH§r §8» §c" + message);
        VeinForge.LOGGER.error("{}", message == null ? "null" : ChatFormatting.stripFormatting(message));
    }

    public static void sendNote(final String message) {
        sendMessage(message);
    }

    public static void sendLog(final String message) {
        if (isDuplicate("debug", message)) return;

        if (VeinForge.config() != null && VeinForge.config().debug.debugMode && mc.player != null) {
            addMessage("§l§2[VeinForge] §8» §7" + message);
            VeinForge.LOGGER.debug("{}", message == null ? "null" : ChatFormatting.stripFormatting(message));
        } else {
            VeinForge.LOGGER.info("{}", message == null ? "null" : ChatFormatting.stripFormatting(message));
        }
    }

    /**
     * Prints directly to the game console/stdout (never to chat, never de-duplicated).
     * Useful for large debug dumps that need to be copy/pasted.
     */
    public static void sendConsole(final String message) {
        VeinForge.LOGGER.info("{}", message == null ? "null" : ChatFormatting.stripFormatting(message));
    }

    /**
     * Same as {@link #sendConsole(String)} but does not add a prefix.
     */
    public static void sendConsoleRaw(final String message) {
        VeinForge.LOGGER.info("{}", message == null ? "null" : ChatFormatting.stripFormatting(message));
    }

    public static void sendNotification(String title, String message, Long duration) {
        if (isDuplicate("notification", message)) return;
        // Notifications.INSTANCE.send(title, message, duration);
        // Fallback or use standard MC toast if possible, for now just log
        sendLog("Notification: " + title + " - " + message);
    }

    private static boolean isDuplicate(String type, String message) {
        if (lastMessages.containsKey(type) && lastMessages.get(type).equals(message)) {
            return true;
        }
        lastMessages.put(type, message);
        return false;
    }

    private static String formatPrefix(String prefix, String message) {
        return ChatFormatting.RED + "[" + ChatFormatting.BLUE + prefix + ChatFormatting.RED + "] §8» §e" + message;
    }

    public abstract String getName();

    protected void log(String message) {
        sendLog(formatMessage(message));
    }

    protected void send(String message) {
        sendMessage(formatMessage(message));
    }

    protected void error(String message) {
        sendError(formatMessage(message));
    }

    protected void warn(String message) {
        sendWarning(formatMessage(message));
    }

    protected void note(String message) {
        sendNote(formatMessage(message));
    }

    protected String formatMessage(String message) {
        return "[" + getName() + "] " + message;
    }
}
