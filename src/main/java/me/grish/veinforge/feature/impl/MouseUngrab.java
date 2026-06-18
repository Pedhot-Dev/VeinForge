package me.grish.veinforge.feature.impl;

import me.grish.veinforge.VeinForge;
import me.grish.veinforge.feature.AbstractFeature;

public class MouseUngrab extends AbstractFeature {

    private static volatile MouseUngrab instance;
    private boolean mouseUngrabbed = false;

    public static MouseUngrab getInstance() {
        if (instance == null) {
            synchronized (MouseUngrab.class) {
                if (instance == null) {
                    instance = new MouseUngrab();
                }
            }
        }
        return instance;
    }

    public void ungrabMouse() {
        if (mouseUngrabbed || !mc.mouseHandler.isMouseGrabbed()) {
            return;
        }

        mc.mouseHandler.releaseMouse();
        // TODO: In Fabric 1.21, we cannot easily replace mc.mouse to prevent re-grabbing.
        // If re-grabbing is an issue, a Mixin into Mouse.lockCursor() is needed.

        mouseUngrabbed = true;
        log("Mouse ungrabbed successfully.");
    }

    public void regrabMouse() {
        if (!mouseUngrabbed || mc.mouseHandler.isMouseGrabbed()) {
            return;
        }

        if (mc.gui.screen() == null) {
            mc.mouseHandler.grabMouse();
        }

        mouseUngrabbed = false;
        log("Mouse regrabbed successfully.");
    }

    @Override
    public String getName() {
        return "Ungrab Mouse";
    }

    @Override
    public boolean isEnabled() {
        return VeinForge.config().general.ungrabMouse;
    }

    @Override
    public boolean shouldStartAtLaunch() {
        return this.isEnabled();
    }

    @Override
    public void start() {
        log("MouseUngrab::start");
        try {
            ungrabMouse();
        } catch (Exception e) {
            log("Failed to ungrab mouse: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        log("MouseUngrab::stop");
        try {
            regrabMouse();
        } catch (Exception e) {
            log("Failed to regrab mouse: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
