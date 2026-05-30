package me.grish.veinforge.client.overlay;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public abstract class AbstractHUDElement {

   @Getter
   @Setter
   protected float x = 5;
   @Getter
   @Setter
   protected float y = 5;
   @Getter
   @Setter
   protected boolean enabled = true;
   @Getter
   @Setter
   protected float scale = 1.0f;

   // 0: Top-Left, 1: Top-Right, 2: Bottom-Left, 3: Bottom-Right
   @Getter
   @Setter
   protected int anchor = 0;
   protected Minecraft mc = Minecraft.getInstance();

   public abstract void render(GuiGraphics context, float tickDelta);

   /**
    * Render variant used by the HUD editor.
    * Defaults to normal rendering.
    */
   public void renderForEditor(GuiGraphics context, float tickDelta) {
      render(context, tickDelta);
   }

   public abstract int getWidth();

   public abstract int getHeight();

   /**
    * Size variant used by the HUD editor.
    * Defaults to normal size.
    */
   public int getEditorWidth() {
      return getWidth();
   }

   /**
    * Size variant used by the HUD editor.
    * Defaults to normal size.
    */
   public int getEditorHeight() {
      return getHeight();
   }

   public float getActualX(int elementWidth) {
      int screenWidth = mc.getWindow().getGuiScaledWidth();
      float raw = switch (anchor) {
         case 1, 3 -> screenWidth - x - elementWidth;
         default -> x;
      };
      float maxX = Math.max(0, screenWidth - elementWidth);
      return Math.max(0, Math.min(raw, maxX));
   }

   public float getActualY(int elementHeight) {
      int screenHeight = mc.getWindow().getGuiScaledHeight();
      float raw = switch (anchor) {
         case 2, 3 -> screenHeight - y - elementHeight;
         default -> y;
      };
      float maxY = Math.max(0, screenHeight - elementHeight);
      return Math.max(0, Math.min(raw, maxY));
   }

   public float getActualX() {
      return getActualX(getWidth());
   }

   public float getActualY() {
      return getActualY(getHeight());
   }

   public boolean isHovered(double mouseX, double mouseY) {
      return isHovered(mouseX, mouseY, getWidth(), getHeight());
   }

   public boolean isHovered(double mouseX, double mouseY, int width, int height) {
      float ax = getActualX(width);
      float ay = getActualY(height);
      return mouseX >= ax && mouseX <= ax + width && mouseY >= ay && mouseY <= ay + height;
   }
}
