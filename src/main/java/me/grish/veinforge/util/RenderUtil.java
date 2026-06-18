package me.grish.veinforge.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.grish.veinforge.util.render.VFRenderLayers;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.List;

public final class RenderUtil {

    private static final Minecraft mc = Minecraft.getInstance();

    private static final float DEFAULT_TRACER_THICKNESS_PX = 2.5f;
    private static final float DEFAULT_TRACER_HALO_PX = 7.5f;

    private static final float MIN_TRACER_THICKNESS_WORLD = 0.01f;
    private static final float MAX_TRACER_THICKNESS_WORLD = 0.20f;

    private static final float DEFAULT_LINE_WIDTH = 2.0f;

    private static PoseStack activePoseStack;
    private static SubmitNodeCollector activeNodeCollector;

    private RenderUtil() {
    }

    public static void beginWorldRender(LevelRenderContext context) {
        activeNodeCollector = context.submitNodeCollector();
        activePoseStack = context.poseStack();

        if (activePoseStack != null) {
            Vec3 cameraPos = mc.gameRenderer.mainCamera().position();
            activePoseStack.pushPose();
            activePoseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        }
    }

    public static void endWorldRender() {
        if (activePoseStack != null) {
            activePoseStack.popPose();
        }
        activeNodeCollector = null;
        activePoseStack = null;
    }

    public static void drawPoint(Vec3 vec, Color color) {
        drawBox(new AABB(
                vec.x - 0.05,
                vec.y - 0.05,
                vec.z - 0.05,
                vec.x + 0.05,
                vec.y + 0.05,
                vec.z + 0.05
        ), color, true);
    }

    public static void drawLine(Vec3 start, Vec3 end, Color color) {
        drawLine(start, end, color, true);
    }

    public static void drawThinLine(Vec3 start, Vec3 end, Color color) {
        drawThinLine(start, end, color, true);
    }

    public static void drawThinLine(Vec3 start, Vec3 end, Color color, boolean overlayLine) {
        if (mc.level == null || mc.player == null) return;
        if (activePoseStack == null || activeNodeCollector == null) return;

        Color thinColor = withAlpha(color, Math.min(255, Math.max(color.getAlpha(), 185)));
        drawSimpleWorldLine(start, end, thinColor);
        if (overlayLine) {
            drawNoDepthDebugLine(start, end, thinColor);
        }
    }

    public static void drawLine(Vec3 start, Vec3 end, Color color, boolean overlayLine) {
        if (mc.level == null || mc.player == null) return;
        if (activePoseStack == null || activeNodeCollector == null) return;

        if (!overlayLine) {
            drawSimpleWorldLine(start, end, color);
            return;
        }

        float coreHalf = worldHalfWidthForPixels(start, end, DEFAULT_TRACER_THICKNESS_PX);
        float haloHalf = worldHalfWidthForPixels(start, end, DEFAULT_TRACER_HALO_PX);

        drawSimpleWorldLine(start, end, withAlpha(color, Math.min(255, Math.max(color.getAlpha(), 170))));

        drawNoDepthDebugLine(start, end, withAlpha(color, Math.min(255, Math.max(color.getAlpha(), 170))));

        drawRibbonSegment(start, end, withAlpha(color, (int) Math.round(color.getAlpha() * 0.90)), coreHalf, VFRenderLayers.QUADS_DEPTH);
        drawRibbonSegment(start, end, withAlpha(color, Math.min(255, Math.max(120, Math.min(color.getAlpha(), 190)))), haloHalf, VFRenderLayers.QUADS_NO_DEPTH);
    }

    public static void drawPolyline(List<Vec3> points, Color color) {
        drawPolyline(points, color, true);
    }

    public static void drawPolyline(List<Vec3> points, Color color, boolean seeThrough) {
        if (points == null || points.size() < 2) return;
        if (mc.level == null || mc.player == null) return;
        if (activePoseStack == null || activeNodeCollector == null) return;

        float coreHalf = worldHalfWidthForPixels(points.get(0), points.get(points.size() - 1), DEFAULT_TRACER_THICKNESS_PX);
        float haloHalf = worldHalfWidthForPixels(points.get(0), points.get(points.size() - 1), DEFAULT_TRACER_HALO_PX);

        if (!seeThrough) {
            drawRibbonPolyline(points, withAlpha(color, (int) Math.round(color.getAlpha() * 0.90)), coreHalf, VFRenderLayers.QUADS_DEPTH);
            return;
        }

        for (int i = 1; i < points.size(); i++) {
            drawSimpleWorldLine(points.get(i - 1), points.get(i), withAlpha(color, Math.min(255, Math.max(color.getAlpha(), 170))));
            drawNoDepthDebugLine(points.get(i - 1), points.get(i), withAlpha(color, Math.min(255, Math.max(color.getAlpha(), 170))));
        }

        drawRibbonPolyline(points, withAlpha(color, (int) Math.round(color.getAlpha() * 0.90)), coreHalf, VFRenderLayers.QUADS_DEPTH);
        drawRibbonPolyline(points, withAlpha(color, Math.min(255, Math.max(120, Math.min(color.getAlpha(), 190)))), haloHalf, VFRenderLayers.QUADS_NO_DEPTH);
    }

    private static void drawSimpleWorldLine(Vec3 start, Vec3 end, Color color) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = color.getAlpha();

        float dx = (float) (end.x - start.x);
        float dy = (float) (end.y - start.y);
        float dz = (float) (end.z - start.z);
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        final float nx, ny, nz;
        if (len <= 0.0001f) {
            nx = 0.0f;
            ny = 1.0f;
            nz = 0.0f;
        } else {
            nx = dx / len;
            ny = dy / len;
            nz = dz / len;
        }

        activeNodeCollector.submitCustomGeometry(activePoseStack, RenderTypes.linesTranslucent(), (pose, consumer) -> {
            consumer.addVertex(pose, (float) start.x, (float) start.y, (float) start.z)
                    .setColor(r, g, b, a)
                    .setNormal(pose, nx, ny, nz)
                    .setLineWidth(DEFAULT_LINE_WIDTH);
            consumer.addVertex(pose, (float) end.x, (float) end.y, (float) end.z)
                    .setColor(r, g, b, a)
                    .setNormal(pose, nx, ny, nz)
                    .setLineWidth(DEFAULT_LINE_WIDTH);
        });
    }

    private static Color withAlpha(Color base, int alpha) {
        int a = Math.max(0, Math.min(255, alpha));
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), a);
    }

    private static float worldHalfWidthForPixels(Vec3 a, Vec3 b, float px) {
        Vec3 mid = a.add(b).scale(0.5);
        Vec3 cam = mc.gameRenderer.mainCamera().position();
        double dist = cam.distanceTo(mid);
        double unitsPerPx = unitsPerPixel(dist);
        float half = (float) (Math.max(0.0, px) * unitsPerPx * 0.5);
        if (half < MIN_TRACER_THICKNESS_WORLD * 0.5f) {
            half = MIN_TRACER_THICKNESS_WORLD * 0.5f;
        }
        float maxHalf = MAX_TRACER_THICKNESS_WORLD * 0.5f;
        if (half > maxHalf) {
            half = maxHalf;
        }
        return half;
    }

    private static double unitsPerPixel(double distance) {
        int fbH = mc.getWindow().getHeight();
        if (fbH <= 0) return 0.01;
        int fovDeg = mc.options.fov().get();
        double fovRad = Math.toRadians(fovDeg);
        double scale = 1.35;
        return ((2.0 * distance * Math.tan(fovRad * 0.5)) / (double) fbH) * scale;
    }

    private static void drawNoDepthDebugLine(Vec3 start, Vec3 end, Color color) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = color.getAlpha();

        activeNodeCollector.submitCustomGeometry(activePoseStack, VFRenderLayers.LINES_NO_DEPTH, (pose, consumer) -> {
            consumer.addVertex(pose, (float) start.x, (float) start.y, (float) start.z).setColor(r, g, b, a);
            consumer.addVertex(pose, (float) end.x, (float) end.y, (float) end.z).setColor(r, g, b, a);
        });
    }

    private static void drawRibbonSegment(Vec3 start, Vec3 end, Color color, float halfWidth, net.minecraft.client.renderer.rendertype.RenderType layer) {
        drawRibbonPolyline(java.util.List.of(start, end), color, halfWidth, layer);
    }

    private static void drawRibbonPolyline(List<Vec3> points, Color color, float halfWidth, net.minecraft.client.renderer.rendertype.RenderType layer) {
        if (points.size() < 2) return;

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = color.getAlpha();

        activeNodeCollector.submitCustomGeometry(activePoseStack, layer, (pose, consumer) -> {
            Vec3 cam = mc.gameRenderer.mainCamera().position();

            Vec3 prevLeft = null;
            Vec3 prevRight = null;

            for (int i = 0; i < points.size(); i++) {
                Vec3 p = points.get(i);
                Vec3 pPrev = i == 0 ? p : points.get(i - 1);
                Vec3 pNext = i == points.size() - 1 ? p : points.get(i + 1);

                Vec3 dirPrev = p.subtract(pPrev);
                Vec3 dirNext = pNext.subtract(p);

                Vec3 dirA = dirPrev.lengthSqr() < 1.0E-6 ? dirNext : dirPrev;
                Vec3 dirB = dirNext.lengthSqr() < 1.0E-6 ? dirPrev : dirNext;
                if (dirA.lengthSqr() < 1.0E-6) continue;

                dirA = dirA.normalize();
                if (dirB.lengthSqr() < 1.0E-6) {
                    dirB = dirA;
                } else {
                    dirB = dirB.normalize();
                }

                Vec3 view = cam.subtract(p);
                if (view.lengthSqr() < 1.0E-6) {
                    view = new Vec3(0.0, 1.0, 0.0);
                } else {
                    view = view.normalize();
                }

                Vec3 perpA = safePerp(dirA, view);
                Vec3 perpB = safePerp(dirB, view);

                Vec3 miter = perpA.add(perpB);
                if (miter.lengthSqr() < 1.0E-6) miter = perpB;
                miter = miter.normalize();

                double denom = miter.dot(perpA);
                if (Math.abs(denom) < 0.2) denom = denom < 0 ? -0.2 : 0.2;

                double miterScale = halfWidth / denom;
                double max = halfWidth * 4.0;
                if (miterScale > max) miterScale = max;
                if (miterScale < -max) miterScale = -max;

                Vec3 offset = miter.scale(miterScale);
                Vec3 left = p.add(offset);
                Vec3 right = p.subtract(offset);

                if (prevLeft != null && prevRight != null) {
                    consumer.addVertex(pose, (float) prevLeft.x, (float) prevLeft.y, (float) prevLeft.z).setColor(r, g, b, a);
                    consumer.addVertex(pose, (float) prevRight.x, (float) prevRight.y, (float) prevRight.z).setColor(r, g, b, a);
                    consumer.addVertex(pose, (float) right.x, (float) right.y, (float) right.z).setColor(r, g, b, a);
                    consumer.addVertex(pose, (float) left.x, (float) left.y, (float) left.z).setColor(r, g, b, a);
                }

                prevLeft = left;
                prevRight = right;
            }
        });
    }

    private static Vec3 safePerp(Vec3 direction, Vec3 view) {
        Vec3 perp = direction.cross(view);
        if (perp.lengthSqr() < 1.0E-8) {
            perp = direction.cross(new Vec3(0.0, 1.0, 0.0));
            if (perp.lengthSqr() < 1.0E-8) {
                perp = direction.cross(new Vec3(1.0, 0.0, 0.0));
            }
        }
        if (perp.lengthSqr() < 1.0E-8) return new Vec3(0.0, 1.0, 0.0);
        return perp.normalize();
    }

    public static void outlineBlock(BlockPos pos, Color color) {
        drawBox(new AABB(pos), color, false);
    }

    public static void drawBlock(BlockPos blockPos, Color color) {
        drawBox(new AABB(blockPos), color, true);
    }

    public static void drawBox(AABB box, Color color) {
        drawBox(box, color, true);
    }

    public static void drawBox(AABB box, Color color, boolean filled) {
        if (mc.level == null || mc.player == null) return;
        if (activePoseStack == null || activeNodeCollector == null) return;

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = color.getAlpha();

        if (filled) {
            activeNodeCollector.submitCustomGeometry(activePoseStack, RenderTypes.debugFilledBox(), (pose, consumer) -> {
                addFilledBoxVertices(consumer, pose, box, r, g, b, a);
            });
        } else {
            activeNodeCollector.submitCustomGeometry(activePoseStack, RenderTypes.lines(), (pose, consumer) -> {
                addOutlineBoxVertices(consumer, pose, box, r, g, b, a);
            });
        }
    }

    public static void drawMultiLineText(GuiGraphicsExtractor drawContext, List<String> lines, Color color, float scale) {
        if (lines == null || lines.isEmpty()) return;
        Font textRenderer = mc.font;
        int scaledWidth = mc.getWindow().getGuiScaledWidth();
        int yOffset = 0;
        for (String line : lines) {
            int width = textRenderer.width(line);
            int x = (scaledWidth - width) / 2;
            drawContext.text(textRenderer, line, x, yOffset, color.getRGB());
            yOffset += textRenderer.lineHeight * 2;
        }
    }

    public static void drawCenterTopText(GuiGraphicsExtractor drawContext, String text, Color color) {
        drawCenterTopText(drawContext, text, color, 3.0f);
    }

    public static void drawCenterTopText(GuiGraphicsExtractor drawContext, String text, Color color, float scale) {
        if (text == null) return;
        Font textRenderer = mc.font;
        int scaledWidth = mc.getWindow().getGuiScaledWidth();
        int width = textRenderer.width(text);
        int x = (scaledWidth - width) / 2;
        drawContext.text(textRenderer, text, x, 0, color.getRGB());
    }

    public static void drawText(String text, double x, double y, double z, float scale) {
        if (mc.level == null || mc.player == null || text == null || text.isEmpty()) return;
        if (activePoseStack == null || activeNodeCollector == null) return;

        Font font = mc.font;
        int textWidth = font.width(text);

        activePoseStack.pushPose();

        activePoseStack.translate(x, y, z);

        activePoseStack.mulPose(mc.gameRenderer.mainCamera().rotation());

        float textScale = scale * 0.025f;
        activePoseStack.scale(-textScale, -textScale, textScale);

        float textX = -textWidth / 2.0f;
        float textY = -(font.lineHeight / 2.0f);

        net.minecraft.util.FormattedCharSequence orderedText =
                net.minecraft.network.chat.Component.literal(text).getVisualOrderText();

        activeNodeCollector.submitText(
                activePoseStack,
                textX, textY,
                orderedText,
                false,
                Font.DisplayMode.SEE_THROUGH,
                0xFFFFFFFF,
                0x40000000,
                0xF000F0,
                0
        );

        activePoseStack.popPose();
    }

    private static void addFilledBoxVertices(VertexConsumer buffer, PoseStack.Pose entry, AABB box, int r, int g, int b, int a) {
        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;
        double maxX = box.maxX;
        double maxY = box.maxY;
        double maxZ = box.maxZ;

        addQuad(buffer, entry, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        addQuad(buffer, entry, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, r, g, b, a);
        addQuad(buffer, entry, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, r, g, b, a);
        addQuad(buffer, entry, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        addQuad(buffer, entry, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);
        addQuad(buffer, entry, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, r, g, b, a);
    }

    private static void addOutlineBoxVertices(VertexConsumer buffer, PoseStack.Pose entry, AABB box, int r, int g, int b, int a) {
        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;
        double maxX = box.maxX;
        double maxY = box.maxY;
        double maxZ = box.maxZ;

        addLine(buffer, entry, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        addLine(buffer, entry, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        addLine(buffer, entry, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        addLine(buffer, entry, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);

        addLine(buffer, entry, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        addLine(buffer, entry, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        addLine(buffer, entry, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        addLine(buffer, entry, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);

        addLine(buffer, entry, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        addLine(buffer, entry, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        addLine(buffer, entry, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        addLine(buffer, entry, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
    }

    private static void addQuad(
            VertexConsumer buffer,
            PoseStack.Pose entry,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            double x3, double y3, double z3,
            double x4, double y4, double z4,
            int r, int g, int b, int a
    ) {
        buffer.addVertex(entry, (float) x1, (float) y1, (float) z1).setColor(r, g, b, a);
        buffer.addVertex(entry, (float) x2, (float) y2, (float) z2).setColor(r, g, b, a);
        buffer.addVertex(entry, (float) x3, (float) y3, (float) z3).setColor(r, g, b, a);
        buffer.addVertex(entry, (float) x4, (float) y4, (float) z4).setColor(r, g, b, a);
    }

    private static void addLine(
            VertexConsumer buffer,
            PoseStack.Pose entry,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            int r, int g, int b, int a
    ) {
        float dx = (float) (x2 - x1);
        float dy = (float) (y2 - y1);
        float dz = (float) (z2 - z1);
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len <= 0.0001f) {
            dx = 0.0f;
            dy = 1.0f;
            dz = 0.0f;
        } else {
            dx /= len;
            dy /= len;
            dz /= len;
        }

        buffer.addVertex(entry, (float) x1, (float) y1, (float) z1)
                .setColor(r, g, b, a)
                .setNormal(entry, dx, dy, dz)
                .setLineWidth(DEFAULT_LINE_WIDTH);

        buffer.addVertex(entry, (float) x2, (float) y2, (float) z2)
                .setColor(r, g, b, a)
                .setNormal(entry, dx, dy, dz)
                .setLineWidth(DEFAULT_LINE_WIDTH);
    }

    public static void renderQueuedLineOverlays(GuiGraphicsExtractor drawContext) {
    }

    public static Color parseColor(String value, Color fallback) {
        if (value == null || value.isEmpty()) return fallback;
        String[] parts = value.split(":");
        if (parts.length >= 5) {
            try {
                int a = Integer.parseInt(parts[1].trim());
                int r = Integer.parseInt(parts[2].trim());
                int g = Integer.parseInt(parts[3].trim());
                int b = Integer.parseInt(parts[4].trim());
                return new Color(r, g, b, a);
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
        if (parts.length < 3) return fallback;
        try {
            int r = Integer.parseInt(parts[0].trim());
            int g = Integer.parseInt(parts[1].trim());
            int b = Integer.parseInt(parts[2].trim());
            int a = parts.length >= 4 ? Integer.parseInt(parts[3].trim()) : 255;
            return new Color(r, g, b, a);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
