package me.grish.veinforge.macro;

import lombok.Getter;
import me.grish.veinforge.event.PacketEvent;
import me.grish.veinforge.event.UpdateTablistEvent;
import me.grish.veinforge.ui.hud.elements.CommissionHUD;
import me.grish.veinforge.util.Logger;
import me.grish.veinforge.util.helper.Clock;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

public abstract class AbstractMacro {

    protected final Minecraft mc = Minecraft.getInstance();
    public Clock timer = new Clock();
    public Clock uptime = new Clock();
    @Getter
    private boolean enabled = false;


    public abstract String getName();

    public void enable() {
        log("AbstractMacro::enable");
        this.onEnable();
        this.uptime.start(CommissionHUD.getInstance().commHudResetStats);
        this.enabled = true;
    }

    public void disable(String reason) {
        error(reason);
        this.disable();
    }

    public void disable() {
        log("AbstractMacro::disable");
        this.uptime.stop(CommissionHUD.getInstance().commHudResetStats);
        this.enabled = false;
        this.onDisable();
    }

    public void pause() {
        log("AbstractMacro::pause");
        this.uptime.stop(false);
        this.enabled = false;
        this.onPause();
    }

    public void resume() {
        log("AbstractMacro::resume");
        this.onResume();
        this.uptime.start(false);
        this.enabled = true;
    }

    public void toggle() {
        if (this.enabled) {
            this.disable();
        } else {
            this.enable();
        }
    }

    public abstract List<String> getNecessaryItems();

    public boolean hasTimerEnded() {
        return this.timer.isScheduled() && this.timer.passed();
    }

    public boolean isTimerRunning() {
        return this.timer.isScheduled() && !this.timer.passed();
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void onPause() {
    }

    public void onResume() {
    }

    public void onTick() {
    }

    public void onWorldRender(LevelRenderContext context) {
    }

    public void onChat(String message) {
    }

    public void onTablistUpdate(UpdateTablistEvent event) {
    }

    public void onOverlayRender(GuiGraphicsExtractor drawContext) {
    }

    public void onReceivePacket(PacketEvent.Received event) {
    }

    public void onSendPacket(PacketEvent.Sent event) {
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
}
