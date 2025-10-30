package cc.silk.utils.render;

import cc.silk.utils.render.font.fonts.FontRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;

import java.awt.*;

import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL3.*;

@UtilityClass
public class NanoVGRenderer {
    private static long vg = -1;
    private static boolean initialized = false;
    private static final NVGColor colorBuffer = NVGColor.create();
    private static final NVGColor colorBuffer2 = NVGColor.create();
    private static final NVGPaint paintBuffer = NVGPaint.create();
    private static boolean inFrame = false;

    public static void init() {
        if (initialized)
            return;

        vg = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
        if (vg == 0) {
            throw new RuntimeException("Failed to initialize NanoVG");
        }

        initialized = true;
    }

    public static void beginFrame() {
        if (!initialized)
            init();

        if (inFrame)
            return;

        MinecraftClient mc = MinecraftClient.getInstance();
        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();
        float scale = 1.0f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        nvgBeginFrame(vg, width, height, scale);
        inFrame = true;
    }

    public static void endFrame() {
        if (!inFrame)
            return;

        nvgEndFrame(vg);
        inFrame = false;

        restoreMinecraftState();
    }
    
    public static void restoreMinecraftState() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.enableCull();
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, Color color) {
        nvgRGBAf(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f,
                colorBuffer);
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, width, height, radius);
        nvgFillColor(vg, colorBuffer);
        nvgFill(vg);
    }

    public static void drawRoundedRectOutline(float x, float y, float width, float height, float radius,
            float strokeWidth, Color color) {
        nvgRGBAf(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f,
                colorBuffer);
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, width, height, radius);
        nvgStrokeColor(vg, colorBuffer);
        nvgStrokeWidth(vg, strokeWidth);
        nvgStroke(vg);
    }

    public static void drawRoundedRectGradient(float x, float y, float width, float height, float radius,
            Color colorTop, Color colorBottom) {
        nvgRGBAf(colorTop.getRed() / 255f, colorTop.getGreen() / 255f, colorTop.getBlue() / 255f,
                colorTop.getAlpha() / 255f, colorBuffer);
        nvgRGBAf(colorBottom.getRed() / 255f, colorBottom.getGreen() / 255f, colorBottom.getBlue() / 255f,
                colorBottom.getAlpha() / 255f, colorBuffer2);

        nvgLinearGradient(vg, x, y, x, y + height, colorBuffer, colorBuffer2, paintBuffer);

        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, width, height, radius);
        nvgFillPaint(vg, paintBuffer);
        nvgFill(vg);
    }

    public static void drawCircle(float x, float y, float radius, Color color) {
        nvgRGBAf(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f,
                colorBuffer);
        nvgBeginPath(vg);
        nvgCircle(vg, x, y, radius);
        nvgFillColor(vg, colorBuffer);
        nvgFill(vg);
    }

    public static void drawText(MatrixStack matrices, FontRenderer font, String text, float x, float y, Color color) {
        boolean wasInFrame = inFrame;
        if (inFrame) {
            endFrame();
        }
        font.drawString(matrices, text, x, y, color);
        if (wasInFrame) {
            beginFrame();
        }
    }

    public static float getTextWidth(FontRenderer font, String text) {
        return font.getStringWidth(text);
    }

    public static void drawRoundedRectWithContext(DrawContext context, float x, float y, float width, float height,
            float radius, Color color) {
        if (!inFrame) {
            throw new IllegalStateException("Must call beginFrame() before drawing with NanoVG");
        }
        drawRoundedRect(x, y, width, height, radius, color);
    }

    public static void drawRoundedRectOutlineWithContext(DrawContext context, float x, float y, float width,
            float height, float radius, float strokeWidth, Color color) {
        if (!inFrame) {
            throw new IllegalStateException("Must call beginFrame() before drawing with NanoVG");
        }
        drawRoundedRectOutline(x, y, width, height, radius, strokeWidth, color);
    }
    
    public static boolean isInFrame() {
        return inFrame;
    }

    public static void cleanup() {
        if (vg != -1) {
            nvgDelete(vg);
            vg = -1;
        }
        colorBuffer.free();
        colorBuffer2.free();
        paintBuffer.free();
        initialized = false;
    }
}
