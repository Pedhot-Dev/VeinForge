package me.grish.veinforge.failsafe;

import me.grish.veinforge.event.BlockChangeEvent;
import me.grish.veinforge.util.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.protocol.Packet;

public abstract class AbstractFailsafe {

    protected final Minecraft mc = Minecraft.getInstance();

    public abstract int getPriority();

    public abstract String getName();

    public abstract Failsafe getFailsafeType();

    public boolean onBlockChange(BlockChangeEvent event) {
        return false;
    }

    public boolean onPacketReceive(Packet<?> packet) {
        return false;
    }

    public boolean onTick() {
        return false;
    }

    public boolean onChat(String message) {
        return false;
    }

    public boolean onScreenOpen(Screen screen) {
        return false;
    }

    public boolean onWorldUnload() {
        return false;
    }

    public boolean onDisconnect() {
        return false;
    }

    public abstract boolean react();

    public void resetStates() {
    }

    protected void log(String message) {
        Logger.sendLog(formatMessage(message));
    }

    protected void send(String message) {
        Logger.sendMessage(formatMessage(message));
    }

    protected void error(String message) {
        Logger.sendError(formatMessage(message));
    }

    protected void warn(String message) {
        Logger.sendWarning(formatMessage(message));
    }

    protected void note(String message) {
        Logger.sendNote(formatMessage(message));
    }

    protected String formatMessage(String message) {
        return "[" + getName() + "] " + message;
    }

    public enum Failsafe {
        BAD_EFFECTS,
        BLOCK_CHANGE,
        DISCONNECT,
        ITEM_CHANGE,
        KNOCKBACK,
        ROTATION,
        TELEPORT,
        BEDROCK_CHECK,
        SLOT_CHANGE,
        PLAYER_PROFILE_OPEN,
        NAME_MENTION
    }
}
