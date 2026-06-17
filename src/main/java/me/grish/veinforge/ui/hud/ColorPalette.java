package me.grish.veinforge.ui.hud;

import java.awt.Color;

public class ColorPalette {
    // Primary palette
    public static final int SLATE_900 = 0xFF0F172A;
    public static final int SLATE_800 = 0xFF1E293B;
    public static final int SLATE_700 = 0xFF334155;

    public static final int CYAN_400 = 0xFF22D3EE;
    public static final int CYAN_500 = 0xFF06B6D4;

    public static final int VIOLET_400 = 0xFFA78BFA;
    public static final int VIOLET_500 = 0xFF8B5CF6;

    public static final int EMERALD_400 = 0xFF34D399;
    public static final int EMERALD_500 = 0xFF10B981;

    public static final int AMBER_400 = 0xFFFBBF24;
    public static final int AMBER_500 = 0xFFF59E0B;

    public static final int ROSE_400 = 0xFFFB7185;
    public static final int ROSE_500 = 0xFFF43F5E;

    public static final int SKY_400 = 0xFF38BDF8;
    public static final int SKY_500 = 0xFF0EA5E9;

    public static final int INDIGO_400 = 0xFF818CF8;
    public static final int INDIGO_500 = 0xFF6366F1;

    // HUD Backgrounds (with alpha)
    public static final int BG_DARK = 0xE00F172A; // 88% opacity
    public static final int BG_MEDIUM = 0xD01E293B; // 81% opacity
    public static final int BORDER_LIGHT = 0x30FFFFFF;

    public static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    public static int getAccentForHUD(String hudName) {
        return switch (hudName.toLowerCase()) {
            case "commission" -> CYAN_400;
            case "glacial" -> SKY_400;
            case "fishing" -> INDIGO_400;
            case "routebuilder" -> VIOLET_400;
            case "inventory" -> AMBER_400;
            case "pathfinder" -> EMERALD_400;
            default -> CYAN_400;
        };
    }
}
