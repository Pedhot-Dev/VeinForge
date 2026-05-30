package me.grish.veinforge.ui.screen;

import me.grish.veinforge.client.overlay.AbstractHUDElement;
import me.grish.veinforge.ui.hud.HUDManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class HUDEditorScreen extends Screen {

   private static final int RESET_BTN_W = 98;
   private static final int RESET_BTN_H = 16;
   private static final int SAVE_BTN_W = 78;
   private static final int BTN_GAP = 6;
   private static final float MIN_SCALE = 0.70f;
   private static final float MAX_SCALE = 1.80f;
   private static final float SCALE_STEP = 0.25f;
   private final Screen parent;
   private AbstractHUDElement draggingElement = null;
   private AbstractHUDElement selectedElement = null;
   private float dragOffsetX = 0;
   private float dragOffsetY = 0;

   private void saveHudLayout() {
      HUDManager.getInstance().savePositions();
   }

   public HUDEditorScreen(Screen parent) {
      super(Component.literal("HUD Editor"));
      this.parent = parent;
   }

   private void setElementPosFromActual(AbstractHUDElement element, float actualX, float actualY, int w, int h) {
      actualX = Math.round(actualX);
      actualY = Math.round(actualY);

      int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
      int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();

      switch (element.getAnchor()) {
         case 1: // Top-Right
            element.setX(screenW - actualX - w);
            element.setY(actualY);
            break;
         case 2: // Bottom-Left
            element.setX(actualX);
            element.setY(screenH - actualY - h);
            break;
         case 3: // Bottom-Right
            element.setX(screenW - actualX - w);
            element.setY(screenH - actualY - h);
            break;
         default: // Top-Left
            element.setX(actualX);
            element.setY(actualY);
            break;
      }
   }

   @Override
   public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
      // Avoid vanilla blurred background here; calling blur more than once per frame throws.
      context.fill(0, 0, this.width, this.height, 0xB0000000);

      // Subtle grid to make positioning easier.
      int minor = 40;
      int major = 160;
      int minorColor = 0x12FFFFFF;
      int majorColor = 0x22FFFFFF;

      for (int x = 0; x < this.width; x += minor) {
         int color = (x % major == 0) ? majorColor : minorColor;
         context.fill(x, 0, x + 1, this.height, color);
      }
      for (int y = 0; y < this.height; y += minor) {
         int color = (y % major == 0) ? majorColor : minorColor;
         context.fill(0, y, this.width, y + 1, color);
      }

      // Screen center cross.
      int cx = this.width / 2;
      int cy = this.height / 2;
      context.fill(cx - 1, 0, cx + 1, this.height, 0x25FFFFFF);
      context.fill(0, cy - 1, this.width, cy + 1, 0x25FFFFFF);
   }

   @Override
   public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
      List<AbstractHUDElement> elements = HUDManager.getInstance().getEditableElements();
      for (AbstractHUDElement element : elements) {
         int w = element.getEditorWidth();
         int h = element.getEditorHeight();
         float ax = element.getActualX(w);
         float ay = element.getActualY(h);

         boolean hovered = element.isHovered(mouseX, mouseY, w, h);

         element.renderForEditor(context, delta);

         int x0 = (int) ax;
         int y0 = (int) ay;
         boolean isDragging = element == draggingElement;
         boolean isSelected = element == selectedElement;

         int outlineColor = isDragging ? 0xFFFFB000 : (isSelected ? 0xFF22D3EE : 0x80FFFFFF);
         context.renderOutline(x0, y0, w, h, outlineColor);
         if (isSelected && !isDragging) {
            context.renderOutline(x0 - 1, y0 - 1, w + 2, h + 2, 0x8022D3EE);
         }

         String anchorLabel = switch (element.getAnchor()) {
            case 1 -> "TR";
            case 2 -> "BL";
            case 3 -> "BR";
            default -> "TL";
         };
         context.drawString(font, anchorLabel, x0 + 2, y0 + 2, 0xD0FFFFFF);

         if (hovered || isSelected || isDragging) {
            // Corner handles (only when interacting to keep editor light)
            int handle = 4;
            int handleFill = isDragging ? 0xFFFFB000 : 0xE0FFFFFF;
            int handleBorder = isDragging ? 0xFFFFE0A0 : (isSelected ? 0xFF22D3EE : 0xFF0B0F14);

            context.fill(x0 - handle, y0 - handle, x0 + handle + 1, y0 + handle + 1, handleFill);
            context.renderOutline(x0 - handle, y0 - handle, handle * 2 + 1, handle * 2 + 1, handleBorder);

            context.fill(x0 + w - handle, y0 - handle, x0 + w + handle + 1, y0 + handle + 1, handleFill);
            context.renderOutline(x0 + w - handle, y0 - handle, handle * 2 + 1, handle * 2 + 1, handleBorder);

            context.fill(x0 - handle, y0 + h - handle, x0 + handle + 1, y0 + h + handle + 1, handleFill);
            context.renderOutline(x0 - handle, y0 + h - handle, handle * 2 + 1, handle * 2 + 1, handleBorder);

            context.fill(x0 + w - handle, y0 + h - handle, x0 + w + handle + 1, y0 + h + handle + 1, handleFill);
            context.renderOutline(x0 + w - handle, y0 + h - handle, handle * 2 + 1, handle * 2 + 1, handleBorder);

            String label = element.getClass().getSimpleName() + "  " + w + "x" + h + "  " + Math.round(element.getScale() * 100.0f) + "%";
            int labelY = Math.max(2, y0 - 10);
            context.drawString(font, label, x0, labelY, 0xE0FFFFFF);
         }
      }

      int resetX = this.width - RESET_BTN_W - 8;
      int resetY = 8;
      int saveX = resetX - SAVE_BTN_W - BTN_GAP;
      boolean resetHover = mouseX >= resetX && mouseX <= resetX + RESET_BTN_W && mouseY >= resetY && mouseY <= resetY + RESET_BTN_H;
      boolean saveHover = mouseX >= saveX && mouseX <= saveX + SAVE_BTN_W && mouseY >= resetY && mouseY <= resetY + RESET_BTN_H;

      int saveBg = saveHover ? 0xC022302A : 0xA014161A;
      int saveBorder = saveHover ? 0xFF22D3EE : 0x60FFFFFF;
      context.fill(saveX, resetY, saveX + SAVE_BTN_W, resetY + RESET_BTN_H, saveBg);
      context.renderOutline(saveX, resetY, SAVE_BTN_W, RESET_BTN_H, saveBorder);
      context.drawString(font, "Save layout", saveX + 8, resetY + 4, 0xE0FFFFFF);

      int resetBg = resetHover ? 0xC0222A30 : 0xA014161A;
      int resetBorder = resetHover ? 0xFF22D3EE : 0x60FFFFFF;
      context.fill(resetX, resetY, resetX + RESET_BTN_W, resetY + RESET_BTN_H, resetBg);
      context.renderOutline(resetX, resetY, RESET_BTN_W, RESET_BTN_H, resetBorder);
      context.drawString(font, "Reset layout", resetX + 6, resetY + 4, 0xE0FFFFFF);

      context.drawString(font, "Click select | Drag move | Scroll resize | Arrows nudge (Shift=10) | Right click anchor", 5, height - 15, 0xFFFFFF);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (selectedElement == null || verticalAmount == 0.0D) {
         return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
      }

      float oldScale = selectedElement.getScale();
      float delta = verticalAmount > 0 ? SCALE_STEP : -SCALE_STEP;
      float nextScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, oldScale + delta));
      nextScale = Math.round(nextScale / SCALE_STEP) * SCALE_STEP;

      if (Math.abs(nextScale - oldScale) < 0.0001f) {
         return true;
      }

      int oldW = selectedElement.getEditorWidth();
      int oldH = selectedElement.getEditorHeight();
      float oldAx = selectedElement.getActualX(oldW);
      float oldAy = selectedElement.getActualY(oldH);

      selectedElement.setScale(nextScale);

      int newW = selectedElement.getEditorWidth();
      int newH = selectedElement.getEditorHeight();
      float newAx = oldAx;
      float newAy = oldAy;
      newAx = Math.max(0, Math.min(this.width - newW, newAx));
      newAy = Math.max(0, Math.min(this.height - newH, newAy));
      setElementPosFromActual(selectedElement, newAx, newAy, newW, newH);
      saveHudLayout();
      return true;
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent click, boolean bl) {
      double mouseX = click.x();
      double mouseY = click.y();
      int button = click.button();

      int resetX = this.width - RESET_BTN_W - 8;
      int resetY = 8;
      int saveX = resetX - SAVE_BTN_W - BTN_GAP;

      if (button == 0
                  && mouseX >= saveX && mouseX <= saveX + SAVE_BTN_W
                  && mouseY >= resetY && mouseY <= resetY + RESET_BTN_H) {
         saveHudLayout();
         return true;
      }

      if (button == 0
                  && mouseX >= resetX && mouseX <= resetX + RESET_BTN_W
                  && mouseY >= resetY && mouseY <= resetY + RESET_BTN_H) {
         draggingElement = null;
         selectedElement = null;
         HUDManager.getInstance().resetPositionsToDefaults();
         saveHudLayout();
         return true;
      }

      List<AbstractHUDElement> elements = HUDManager.getInstance().getEditableElements();
      for (int i = elements.size() - 1; i >= 0; i--) {
         AbstractHUDElement element = elements.get(i);
         int w = element.getEditorWidth();
         int h = element.getEditorHeight();
         if (element.isHovered(mouseX, mouseY, w, h)) {
            if (button == 0) { // Left click to drag
               selectedElement = element;
               draggingElement = element;
               float ax = element.getActualX(w);
               float ay = element.getActualY(h);
               dragOffsetX = (float) mouseX - ax;
               dragOffsetY = (float) mouseY - ay;
               return true;
            } else if (button == 1) { // Right click to cycle anchor
               selectedElement = element;
               float ax = element.getActualX(w);
               float ay = element.getActualY(h);
               element.setAnchor((element.getAnchor() + 1) % 4);
               setElementPosFromActual(element, ax, ay, w, h);
               saveHudLayout();
               return true;
            }
         }
      }
      if (button == 0) {
         selectedElement = null;
      }
      return super.mouseClicked(click, bl);
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent click) {
      int button = click.button();
      if (button == 0) {
         draggingElement = null;
         saveHudLayout();
      }
      return super.mouseReleased(click);
   }

   @Override
   public boolean keyPressed(KeyEvent input) {
      if (selectedElement == null) {
         return super.keyPressed(input);
      }

      int step = (input.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0 ? 10 : 1;
      int dx = 0;
      int dy = 0;
      if (input.key() == GLFW.GLFW_KEY_LEFT) dx = -step;
      if (input.key() == GLFW.GLFW_KEY_RIGHT) dx = step;
      if (input.key() == GLFW.GLFW_KEY_UP) dy = -step;
      if (input.key() == GLFW.GLFW_KEY_DOWN) dy = step;

      if (dx == 0 && dy == 0) {
         return super.keyPressed(input);
      }

      int w = selectedElement.getEditorWidth();
      int h = selectedElement.getEditorHeight();

      float ax = selectedElement.getActualX(w);
      float ay = selectedElement.getActualY(h);

      float newAx = ax + dx;
      float newAy = ay + dy;

      newAx = Math.max(0, Math.min(this.width - w, newAx));
      newAy = Math.max(0, Math.min(this.height - h, newAy));

      setElementPosFromActual(selectedElement, newAx, newAy, w, h);
      saveHudLayout();
      return true;
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
      double mouseX = click.x();
      double mouseY = click.y();
      int button = click.button();

      if (draggingElement != null) {
         int w = draggingElement.getEditorWidth();
         int h = draggingElement.getEditorHeight();

         float newAx = (float) mouseX - dragOffsetX;
         float newAy = (float) mouseY - dragOffsetY;

         // Clamp to screen bounds.
         newAx = Math.max(0, Math.min(this.width - w, newAx));
         newAy = Math.max(0, Math.min(this.height - h, newAy));

         setElementPosFromActual(draggingElement, newAx, newAy, w, h);
         return true;
      }
      return super.mouseDragged(click, deltaX, deltaY);
   }

   @Override
   public void onClose() {
      saveHudLayout();
      if (parent != null) {
         Minecraft.getInstance().setScreen(parent);
         return;
      }
      super.onClose();
   }

   @Override
   public void removed() {
      saveHudLayout();
      super.removed();
   }
}
