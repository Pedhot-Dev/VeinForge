package me.grish.veinforge.client.overlay;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

public abstract class TextHud extends AbstractHUDElement {

   private static final int PADDING_PX = 4;
   private static final int LINE_SPACING_PX = 1;
   private static final int SHADOW_OFFSET_PX = 0; // Removed shadow offset
   private static final int ACCENT_WIDTH_PX = 2;
   private static final int MAX_PANEL_WIDTH_PX = 320;
   private static final int MIN_PANEL_WIDTH_PX = 116;
   private static final int PANEL_CORNER_RADIUS_PX = 5;
   private static final int SAFE_MARGIN_PX = 4;

   private static final int COLOR_SHADOW = 0x00000000; // Transparent shadow
   private static final int COLOR_BG_OUTER = 0x800A1118; // More transparent
   private static final int COLOR_BG_INNER = 0x90131D27; // More transparent
   private static final int COLOR_BG_TOP = 0x10FFFFFF; // Fainter gradient
   private static final int COLOR_BG_BOTTOM = 0x10000000; // Fainter gradient
   private static final int COLOR_BORDER = 0x40FFFFFF; // Fainter border
   private static final int COLOR_TEXT_MAIN = 0xF2FFFFFF;

   public TextHud() {
      super();
   }

   private static String plainOrSelf(String line) {
      String plain = ChatFormatting.stripFormatting(line);
      return plain != null ? plain : line;
   }

   private static Component fontComponent(String text) {
      // Use default font to avoid tearing
      return Component.literal(text);
   }

   private static Style applyLegacyCode(Style style, ChatFormatting code) {
      if (code == null) {
         return style;
      }

      if (code == ChatFormatting.RESET) {
         return Style.EMPTY;
      }

      if (code.isColor()) {
         return Style.EMPTY
                        .withColor(code)
                        .withBold(false)
                        .withItalic(false)
                        .withUnderlined(false)
                        .withStrikethrough(false)
                        .withObfuscated(false);
      }

      return switch (code) {
         case BOLD -> style.withBold(true); // Ensure bold is handled if standard font supports it
         case ITALIC -> style.withItalic(true);
         case UNDERLINE -> style.withUnderlined(true);
         case STRIKETHROUGH -> style.withStrikethrough(true);
         case OBFUSCATED -> style.withObfuscated(true);
         default -> style;
      };
   }

   private static Component componentWithHudFont(String text) {
      if (text == null || text.isEmpty()) {
         return fontComponent("");
      }

      MutableComponent out = Component.empty();
      Style style = Style.EMPTY;
      StringBuilder segment = new StringBuilder(text.length());

      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         if (c == '§' && i + 1 < text.length()) {
            if (segment.length() > 0) {
               out.append(Component.literal(segment.toString()).withStyle(style));
               segment.setLength(0);
            }
            ChatFormatting code = ChatFormatting.getByCode(Character.toLowerCase(text.charAt(i + 1)));
            style = applyLegacyCode(style, code);
            i++;
            continue;
         }
         segment.append(c);
      }

      if (segment.length() > 0) {
         out.append(Component.literal(segment.toString()).withStyle(style));
      }

      if (out.getString().isEmpty()) {
         return fontComponent("");
      }
      return out;
   }

   private static boolean isBlankLine(String line) {
      return plainOrSelf(line).trim().isEmpty();
   }

   private static boolean isDividerLine(String line) {
      String plain = plainOrSelf(line).trim();
      if (plain.isEmpty()) {
         return false;
      }

      int dividerChars = 0;
      for (int i = 0; i < plain.length(); i++) {
         char c = plain.charAt(i);
         if (c == '-' || c == '|' || c == '=' || c == '_') {
            dividerChars++;
         }
      }

      return dividerChars >= Math.max(6, plain.length() - 2);
   }

   private static void trimBlankLines(List<String> lines) {
      while (!lines.isEmpty() && isBlankLine(lines.get(0))) {
         lines.remove(0);
      }
      while (!lines.isEmpty() && isBlankLine(lines.get(lines.size() - 1))) {
         lines.remove(lines.size() - 1);
      }
   }

   private static void collapseBlankRuns(List<String> lines) {
      boolean lastBlank = false;
      for (int i = 0; i < lines.size(); i++) {
         boolean blank = isBlankLine(lines.get(i));
         if (blank && lastBlank) {
            lines.remove(i);
            i--;
            continue;
         }
         lastBlank = blank;
      }
   }

   protected int getAccentColor() {
      return 0xFF22D3EE;
   }

   protected int getPaddingPx() {
      return PADDING_PX;
   }

   protected int getLineSpacingPx() {
      return LINE_SPACING_PX;
   }

   protected int getMaxPanelWidthPx() {
      return MAX_PANEL_WIDTH_PX;
   }

   protected abstract void getLines(List<String> lines, boolean example);

   private float resolveRenderScale() {
      float clamped = Math.max(0.7f, Math.min(1.8f, getScale()));
      float snapped = Math.round(clamped * 4.0f) / 4.0f;
      return Math.max(0.75f, Math.min(1.75f, snapped));
   }

   private int computeMaxLineWidth(List<String> lines) {
      int maxWidth = 0;
      for (String line : lines) {
         maxWidth = Math.max(maxWidth, mc.font.width(componentWithHudFont(line)));
      }
      return maxWidth;
   }

   private int lineWidth(String line) {
      return mc.font.width(componentWithHudFont(line));
   }

   private String truncateToWidth(String line, int maxWidth) {
      if (line == null || line.isEmpty()) {
         return line;
      }
      if (lineWidth(line) <= maxWidth) {
         return line;
      }

      String ellipsis = "...";
      StringBuilder out = new StringBuilder(line.length());

      for (int i = 0; i < line.length(); i++) {
         char c = line.charAt(i);
         if (c == '§' && i + 1 < line.length()) {
            out.append(c).append(line.charAt(i + 1));
            i++;
            continue;
         }

         out.append(c);
         String candidate = out + ellipsis;
          if (lineWidth(candidate) > maxWidth) {
             out.setLength(Math.max(0, out.length() - 1));
             break;
          }
      }

      if (out.length() == 0) {
         return ellipsis;
      }
      return out + ellipsis;
   }

   private List<String> buildLines(boolean example, float requestedScale) {
      List<String> lines = new ArrayList<>();
      getLines(lines, example);
      trimBlankLines(lines);
      collapseBlankRuns(lines);

      if (lines.isEmpty()) {
         return lines;
      }

      int contentMaxWidth = Math.max(80, getMaxPanelWidthPx() - (getPaddingPx() * 2));
      int screenWidth = mc.getWindow().getGuiScaledWidth();
      int availableScreenWidth = Math.max(96, screenWidth - (SAFE_MARGIN_PX * 2));
      int maxBaseWidthFromScreen = Math.max(96, (int) Math.floor(availableScreenWidth / Math.max(0.1f, requestedScale)));
      int maxContentFromScreen = Math.max(80, maxBaseWidthFromScreen - (getPaddingPx() * 2));
      contentMaxWidth = Math.min(contentMaxWidth, maxContentFromScreen);
      for (int i = 0; i < lines.size(); i++) {
         lines.set(i, truncateToWidth(lines.get(i), contentMaxWidth));
      }
      return lines;
   }

   private int computeContentHeight(List<String> lines) {
      int lineStep = mc.font.lineHeight + getLineSpacingPx();
      return (lines.size() * lineStep) - getLineSpacingPx();
   }

    private void fillRoundedRect(GuiGraphicsExtractor context, int x1, int y1, int x2, int y2, int radius, int color) {
      if (x2 <= x1 || y2 <= y1) {
         return;
      }
      int r = Math.max(0, Math.min(radius, Math.min((x2 - x1) / 2, (y2 - y1) / 2)));
      if (r <= 0) {
         context.fill(x1, y1, x2, y2, color);
         return;
      }

      context.fill(x1 + r, y1, x2 - r, y2, color);
      context.fill(x1, y1 + r, x2, y2 - r, color);

      for (int i = 0; i < r; i++) {
         int inset = r - i - 1;
         context.fill(x1 + inset, y1 + i, x2 - inset, y1 + i + 1, color);
         context.fill(x1 + inset, y2 - i - 1, x2 - inset, y2 - i, color);
      }
   }

    private void drawRoundedOutline(GuiGraphicsExtractor context, int x1, int y1, int x2, int y2, int radius, int color) {
      int w = x2 - x1;
      int h = y2 - y1;
      if (w <= 1 || h <= 1) {
         return;
      }
      int r = Math.max(0, Math.min(radius, Math.min(w / 2, h / 2)));
      if (r <= 0) {
         context.renderOutline(x1, y1, w, h, color);
         return;
      }

      context.fill(x1 + r, y1, x2 - r, y1 + 1, color);
      context.fill(x1 + r, y2 - 1, x2 - r, y2, color);
      context.fill(x1, y1 + r, x1 + 1, y2 - r, color);
      context.fill(x2 - 1, y1 + r, x2, y2 - r, color);

      for (int i = 0; i < r; i++) {
         int inset = r - i - 1;
         context.fill(x1 + inset, y1 + i, x1 + inset + 1, y1 + i + 1, color);
         context.fill(x2 - inset - 1, y1 + i, x2 - inset, y1 + i + 1, color);
         context.fill(x1 + inset, y2 - i - 1, x1 + inset + 1, y2 - i, color);
         context.fill(x2 - inset - 1, y2 - i - 1, x2 - inset, y2 - i, color);
      }
   }

    private void drawPanelChrome(GuiGraphicsExtractor context, int panelX, int panelY, int panelW, int panelH) {
      // context.fill(panelX + SHADOW_OFFSET_PX, panelY + SHADOW_OFFSET_PX, panelX + panelW + SHADOW_OFFSET_PX, panelY + panelH + SHADOW_OFFSET_PX, COLOR_SHADOW);

      fillRoundedRect(context, panelX, panelY, panelX + panelW, panelY + panelH, PANEL_CORNER_RADIUS_PX, COLOR_BG_OUTER);
      fillRoundedRect(context, panelX + 1, panelY + 1, panelX + panelW - 1, panelY + panelH - 1, PANEL_CORNER_RADIUS_PX - 1, COLOR_BG_INNER);

      fillRoundedRect(context, panelX + 2, panelY + 2, panelX + panelW - 2, panelY + 4, PANEL_CORNER_RADIUS_PX - 2, COLOR_BG_TOP);
      fillRoundedRect(context, panelX + 2, panelY + panelH - 3, panelX + panelW - 2, panelY + panelH - 2, PANEL_CORNER_RADIUS_PX - 2, COLOR_BG_BOTTOM);

      fillRoundedRect(context, panelX, panelY + 1, panelX + ACCENT_WIDTH_PX, panelY + panelH - 1, 1, getAccentColor());
      fillRoundedRect(context, panelX + ACCENT_WIDTH_PX, panelY + 1, panelX + ACCENT_WIDTH_PX + 1, panelY + panelH - 1, 1, 0x1AFFFFFF); // Fainter accent border
      drawRoundedOutline(context, panelX, panelY, panelX + panelW, panelY + panelH, PANEL_CORNER_RADIUS_PX, COLOR_BORDER);
   }

   private float fitScaleToScreen(int basePanelW, int basePanelH, float preferredScale) {
      if (basePanelW <= 0 || basePanelH <= 0) {
         return preferredScale;
      }
      int screenW = mc.getWindow().getGuiScaledWidth();
      int screenH = mc.getWindow().getGuiScaledHeight();
      float fitX = (screenW - (SAFE_MARGIN_PX * 2f)) / (float) basePanelW;
      float fitY = (screenH - (SAFE_MARGIN_PX * 2f)) / (float) basePanelH;
      float fitScale = Math.min(fitX, fitY);
      if (Float.isNaN(fitScale) || fitScale <= 0f) {
         return preferredScale;
      }
      return Math.min(preferredScale, fitScale);
   }

   private int clampPanelX(int panelX, int panelW) {
      int screenW = mc.getWindow().getGuiScaledWidth();
      int minX = 0;
      int maxX = Math.max(minX, screenW - panelW);
      return Math.max(minX, Math.min(panelX, maxX));
   }

   private int clampPanelY(int panelY, int panelH) {
      int screenH = mc.getWindow().getGuiScaledHeight();
      int minY = 0;
      int maxY = Math.max(minY, screenH - panelH);
      return Math.max(minY, Math.min(panelY, maxY));
   }

    private void renderInternal(GuiGraphicsExtractor context, float tickDelta, boolean example, boolean forceShow) {
      if (!forceShow && !shouldShow()) return;

      float preferredScale = resolveRenderScale();
      List<String> lines = buildLines(example, preferredScale);

      if (lines.isEmpty()) {
         return;
      }

      int padding = getPaddingPx();
      int lineStep = mc.font.lineHeight + getLineSpacingPx();

      int maxWidth = computeMaxLineWidth(lines);
      int contentHeight = computeContentHeight(lines);

      int basePanelW = Math.max(MIN_PANEL_WIDTH_PX, maxWidth + (padding * 2));
      int basePanelH = contentHeight + (padding * 2);
      float scale = fitScaleToScreen(basePanelW, basePanelH, preferredScale);
      int panelW = Math.max(1, Math.round(basePanelW * scale));
      int panelH = Math.max(1, Math.round(basePanelH * scale));
      int panelX = clampPanelX(Math.round(getActualX(panelW)), panelW);
      int panelY = clampPanelY(Math.round(getActualY(panelH)), panelH);

      context.pose().pushMatrix();
      context.pose().translate(panelX, panelY);
      context.pose().scale(scale, scale);

      drawPanelChrome(context, 0, 0, basePanelW, basePanelH);

      int renderX = padding;
      int renderY = padding;

      int currentY = renderY;
      boolean firstTextLine = true;
      for (int i = 0; i < lines.size(); i++) {
         String line = lines.get(i);

         if (isBlankLine(line)) {
            currentY += lineStep;
            continue;
         }

         if (isDividerLine(line)) {
             int separatorY = currentY + (mc.font.lineHeight / 2);
             context.fill(padding, separatorY, basePanelW - padding, separatorY + 1, 0x3FFFFFFF);
             currentY += lineStep;
             continue;
          }

          if (firstTextLine) {
             Component titleComponent = componentWithHudFont(line);
             context.text(mc.font, titleComponent, renderX, currentY, getAccentColor(), false);

             if (i + 1 < lines.size()) {
                int sepY = currentY + mc.font.lineHeight + 2;
                context.fill(padding, sepY, basePanelW - padding, sepY + 1, 0x3EFFFFFF);
             }
             firstTextLine = false;
             currentY += lineStep;
             continue;
          }

          context.text(mc.font, componentWithHudFont(line), renderX, currentY, COLOR_TEXT_MAIN, false);
          currentY += lineStep;
      }

      context.pose().popMatrix();
   }

   @Override
    public void render(GuiGraphicsExtractor context, float tickDelta) {
      renderInternal(context, tickDelta, false, false);
   }

   @Override
    public void renderForEditor(GuiGraphicsExtractor context, float tickDelta) {
      renderInternal(context, tickDelta, true, true);
   }

   protected boolean shouldShow() {
      return enabled && mc.player != null && mc.level != null;
   }

   @Override
   public int getWidth() {
      boolean example = mc.level == null || mc.player == null;
      return computeWidth(example);
   }

   @Override
   public int getEditorWidth() {
      return computeWidth(true);
   }

   private int computeWidth(boolean example) {
      float preferredScale = resolveRenderScale();
      List<String> lines = buildLines(example, preferredScale);

      if (lines.isEmpty()) {
         return 0;
      }

      int baseWidth = Math.max(MIN_PANEL_WIDTH_PX, computeMaxLineWidth(lines) + (getPaddingPx() * 2));
      int baseHeight = computeContentHeight(lines) + (getPaddingPx() * 2);
      float scale = fitScaleToScreen(baseWidth, baseHeight, preferredScale);
      return Math.max(1, Math.round(baseWidth * scale));
   }

   @Override
   public int getHeight() {
      boolean example = mc.level == null || mc.player == null;
      return computeHeight(example);
   }

   @Override
   public int getEditorHeight() {
      return computeHeight(true);
   }

   private int computeHeight(boolean example) {
      float preferredScale = resolveRenderScale();
      List<String> lines = buildLines(example, preferredScale);

      if (lines.isEmpty()) {
         return 0;
      }

      int baseWidth = Math.max(MIN_PANEL_WIDTH_PX, computeMaxLineWidth(lines) + (getPaddingPx() * 2));
      int baseHeight = computeContentHeight(lines) + (getPaddingPx() * 2);
      float scale = fitScaleToScreen(baseWidth, baseHeight, preferredScale);
      return Math.max(1, Math.round(baseHeight * scale));
   }
}
