package me.grish.veinforge.ui.hud.elements;

import lombok.Getter;
import me.grish.veinforge.VeinForge;
import me.grish.veinforge.client.overlay.AbstractHUDElement;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.awt.Color;

public class InventoryHUD extends AbstractHUDElement {

    @Getter
    private static final InventoryHUD instance = new InventoryHUD();

    private static final int SLOT_SIZE = 16;
    private static final int SLOT_PADDING = 2; // Padding between slots
    private static final int CELL_SIZE = SLOT_SIZE + SLOT_PADDING * 2; // 20
    private static final int COLUMNS = 9;
    private static final int ROWS = 4; // 3 main + 1 hotbar
    private static final int WINDOW_PADDING = 5;
    
    // JS reference used 190x90.
    // 9 cols * 20 = 180. + 2*5 padding = 190.
    // 4 rows * 20 = 80. + 2*5 padding = 90.
    // Matches perfectly.

    private InventoryHUD() {
        super();
        this.x = 10;
        this.y = 10;
    }

    @Override
    public void render(GuiGraphicsExtractor context, float tickDelta) {
        if (!isEnabled()) return;
        
        Player player = mc.player;
        if (player == null) return;

        int width = getWidth();
        int height = getHeight();
        float actualX = getActualX(width);
        float actualY = getActualY(height);

        // Background
        int bgColor = new Color(0, 0, 0, 64).getRGB(); // 0.25 alpha approx
        
        // Main Background
        context.fill((int) actualX, (int) actualY, (int) actualX + width, (int) actualY + height, bgColor);

        // Outline
        if (VeinForge.config().hud.inventoryHudOutline) {
            drawChromaOutline(context, (int) actualX, (int) actualY, width, height);
        }

        // Draw Items
        // Slots 9-35 (Main Inventory)
        for (int i = 9; i < 36; i++) {
            int row = (i - 9) / 9; // 0, 1, 2
            int col = (i - 9) % 9;
            renderItem(context, player.getInventory().getItem(i), actualX, actualY, col, row);
        }

        // Slots 0-8 (Hotbar) - Row 3
        for (int i = 0; i < 9; i++) {
            renderItem(context, player.getInventory().getItem(i), actualX, actualY, i, 3);
        }
    }

    private void renderItem(GuiGraphicsExtractor context, ItemStack stack, float startX, float startY, int col, int row) {
        if (stack.isEmpty()) return;

        int x = (int) (startX + WINDOW_PADDING + col * CELL_SIZE + SLOT_PADDING);
        int y = (int) (startY + WINDOW_PADDING + row * CELL_SIZE + SLOT_PADDING);

        context.item(stack, x, y);
        context.itemDecorations(mc.font, stack, x, y);
    }

    private void drawChromaOutline(GuiGraphicsExtractor context, int x, int y, int width, int height) {
        long time = System.currentTimeMillis();
        // Simple chroma effect implementation
        // For a perfect gradient we'd need to draw lines with gradients, but simple solid color cycling is easier 
        // and matches the JS implementation which used `(Date.now() % 4000) / 4000` for a single color.
        
        float hue = (time % 4000L) / 4000.0f;
        int color = Color.HSBtoRGB(hue, 1.0f, 1.0f);
        
        // Draw 4 lines
        context.fill(x - 1, y - 1, x + width + 1, y, color); // Top
        context.fill(x - 1, y + height, x + width + 1, y + height + 1, color); // Bottom
        context.fill(x - 1, y, x, y + height, color); // Left
        context.fill(x + width, y, x + width + 1, y + height, color); // Right
    }

    @Override
    public int getWidth() {
        return WINDOW_PADDING * 2 + COLUMNS * CELL_SIZE; // 10 + 180 = 190
    }

    @Override
    public int getHeight() {
        return WINDOW_PADDING * 2 + ROWS * CELL_SIZE; // 10 + 80 = 90
    }
}
