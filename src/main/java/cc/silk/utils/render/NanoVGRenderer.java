package cc.silk.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL3.*;
import static org.lwjgl.system.MemoryStack.stackPush;

@UtilityClass
public class NanoVGRenderer {
    private static long vg = -1;
    private static boolean initialized = false;
    private static boolean inFrame = false;
    private static boolean usePerDrawStateIsolation = true;

    private static boolean savedDepthTest;
    private static boolean savedBlend;
    private static boolean savedCull;
    private static int savedBlendSrc;
    private static int savedBlendDst;

    private static int regularFont = -1;
    private static int boldFont = -1;
    private static int iconFont = -1;
    private static int jetbrainsFont = -1;
    private static int poppinsFont = -1;
    private static int monacoFont = -1;

    public static void init() {
        if (initialized)
            return;

        vg = nvgCreate(NVG_ANTIALIAS);
        if (vg == 0) {
            throw new RuntimeException("Failed to initialize NanoVG");
        }

        System.out.println("[NanoVG] Initialized without stencil strokes (AMD compatibility mode)");
        loadFonts();
        initialized = true;
    }

    public static void setPerDrawStateIsolation(boolean enabled) {
        usePerDrawStateIsolation = enabled;
        System.out.println("[NanoVG] Per-draw state isolation " + (enabled ? "enabled" : "disabled"));
    }

    public static boolean isPerDrawStateIsolationEnabled() {
        return usePerDrawStateIsolation;
    }

    private static void loadFonts() {
        try {
            System.out.println("[NanoVG] Attempting to load fonts...");
            regularFont = loadFont("inter", "silk/fonts/Inter.ttf");
            boldFont = loadFont("inter-bold", "silk/fonts/Inter.ttf");
            jetbrainsFont = loadFont("jetbrains", "silk/fonts/JetbrainsMono.ttf");
            poppinsFont = loadFont("poppins", "silk/fonts/Poppins-Medium.ttf");
            monacoFont = loadFont("monaco", "silk/fonts/monaco.ttf");

            try {
                iconFont = loadFont("fa-solid", "silk/fonts/font awesome 7 freesolid900.otf");
                System.out.println("[NanoVG] Font Awesome Solid loaded (ID: " + iconFont + ")");
            } catch (Exception e) {
                System.out.println("[NanoVG] Font Awesome not found: " + e.getMessage());
                System.out.println("[NanoVG] Trying fallback icon font...");
                try {
                    iconFont = loadFont("icons", "silk/fonts/icons.ttf");
                    System.out.println("[NanoVG] Icon font loaded (ID: " + iconFont + ")");
                } catch (Exception e2) {
                    System.out.println("[NanoVG] No icon font found, icons will use regular font");
                    iconFont = regularFont;
                }
            }

            System.out.println("[NanoVG] Regular font ID: " + regularFont);
            System.out.println("[NanoVG] Bold font ID: " + boldFont);
            System.out.println("[NanoVG] JetBrains font ID: " + jetbrainsFont);
            System.out.println("[NanoVG] Poppins font ID: " + poppinsFont);
            System.out.println("[NanoVG] Monaco font ID: " + monacoFont);

            if (regularFont == -1) {
                System.err.println("[NanoVG] Warning: Failed to load custom fonts, trying fallback...");
                loadFallbackFonts();
            } else {
                System.out.println("[NanoVG] Fonts loaded successfully!");
            }
        } catch (Exception e) {
            System.err.println("[NanoVG] Error loading fonts: " + e.getMessage());
            e.printStackTrace();
            loadFallbackFonts();
        }
    }

    private static void loadFallbackFonts() {
        System.out.println("[NanoVG] Loading fallback fonts...");
        regularFont = nvgCreateFont(vg, "sans", "sans");
        boldFont = nvgCreateFont(vg, "sans-bold", "sans-bold");

        if (regularFont == -1) {
            regularFont = 0;
        }
        if (boldFont == -1) {
            boldFont = regularFont;
        }

        System.out.println("[NanoVG] Fallback font IDs - Regular: " + regularFont + ", Bold: " + boldFont);
    }

    private static int loadFont(String name, String path) throws IOException {
        System.out.println("[NanoVG] Loading font: " + name + " from " + path);

        // graph this is for feather support cuz fonts didnt work on it
        InputStream is = null;

        is = NanoVGRenderer.class.getClassLoader().getResourceAsStream(path);
        if (is == null)
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (is == null)
            is = ClassLoader.getSystemResourceAsStream(path);
        if (is == null)
            is = NanoVGRenderer.class.getResourceAsStream("/" + path);

        if (is == null) {
            System.err.println("[NanoVG] Font resource not found: " + path);
            return -1;
        }

        try {
            return createFontFromStream(name, is);
        } finally {
            is.close();
        }
    }

    private static int createFontFromStream(String name, InputStream is) throws IOException {
        ReadableByteChannel rbc = Channels.newChannel(is);
        ByteBuffer buffer = MemoryUtil.memAlloc(8192);

        while (rbc.read(buffer) != -1) {
            if (buffer.remaining() == 0) {
                ByteBuffer newBuffer = MemoryUtil.memAlloc(buffer.capacity() * 2);
                buffer.flip();
                newBuffer.put(buffer);
                MemoryUtil.memFree(buffer);
                buffer = newBuffer;
            }
        }

        buffer.flip();
        System.out.println("[NanoVG] Font buffer size: " + buffer.remaining() + " bytes");

        int font = nvgCreateFontMem(vg, name, buffer, false);

        if (font == -1) {
            System.err.println("[NanoVG] nvgCreateFontMem failed for: " + name);
            MemoryUtil.memFree(buffer);
            throw new RuntimeException("Failed to create font: " + name);
        }

        System.out.println("[NanoVG] Font created successfully: " + name + " (ID: " + font + ")");
        return font;
    }

    public static void beginFrame() {
        if (!initialized)
            init();

        if (inFrame)
            return;

        MinecraftClient mc = MinecraftClient.getInstance();

        mc.getFramebuffer().beginWrite(true);

        int framebufferWidth = mc.getWindow().getFramebufferWidth();
        int framebufferHeight = mc.getWindow().getFramebufferHeight();
        int scaledWidth = mc.getWindow().getScaledWidth();
        int scaledHeight = mc.getWindow().getScaledHeight();

        if (framebufferWidth <= 0 || framebufferHeight <= 0) {
            System.err.println("[NanoVG] Invalid framebuffer dimensions: " +
                    framebufferWidth + "x" + framebufferHeight);
            return;
        }

        savedDepthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        savedBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        savedCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        savedBlendSrc = GL11.glGetInteger(GL11.GL_BLEND_SRC);
        savedBlendDst = GL11.glGetInteger(GL11.GL_BLEND_DST);

        RenderSystem.glBindVertexArray(0);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        nvgBeginFrame(vg, framebufferWidth, framebufferHeight, 1f);

        float scaleX = (float) framebufferWidth / (float) scaledWidth;
        float scaleY = (float) framebufferHeight / (float) scaledHeight;
        nvgScale(vg, scaleX, scaleY);

        inFrame = true;
    }

    public static void endFrame() {
        if (!inFrame)
            return;

        nvgEndFrame(vg);
        inFrame = false;

        if (savedDepthTest) {
            RenderSystem.enableDepthTest();
        } else {
            RenderSystem.disableDepthTest();
        }

        if (savedBlend) {
            RenderSystem.enableBlend();
        } else {
            RenderSystem.disableBlend();
        }

        if (savedCull) {
            RenderSystem.enableCull();
        } else {
            RenderSystem.disableCull();
        }

        GL11.glBlendFunc(savedBlendSrc, savedBlendDst);

        MinecraftClient mc = MinecraftClient.getInstance();
        mc.getFramebuffer().beginWrite(true);

        restoreMinecraftState();
    }

    public static void restoreMinecraftState() {
        RenderSystem.glBindVertexArray(0);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515);
        RenderSystem.enableCull();
        RenderSystem.disableScissor();
        RenderSystem.resetTextureMatrix();
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(true);
        RenderSystem.setShaderTexture(0, 0);
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, Color color) {
        if (usePerDrawStateIsolation)
            nvgSave(vg);
        try (MemoryStack stack = stackPush()) {
            NVGColor nvgColor = NVGColor.malloc(stack);
            nvgColor.r(color.getRed() / 255f);
            nvgColor.g(color.getGreen() / 255f);
            nvgColor.b(color.getBlue() / 255f);
            nvgColor.a(color.getAlpha() / 255f);

            nvgBeginPath(vg);
            nvgRoundedRect(vg, x, y, width, height, radius);
            nvgFillColor(vg, nvgColor);
            nvgFill(vg);
        } finally {
            if (usePerDrawStateIsolation)
                nvgRestore(vg);
        }
    }

    public static void drawRoundedRectOutline(float x, float y, float width, float height, float radius,
            float strokeWidth, Color color) {
        if (usePerDrawStateIsolation)
            nvgSave(vg);
        try (MemoryStack stack = stackPush()) {
            NVGColor nvgColor = NVGColor.malloc(stack);
            nvgColor.r(color.getRed() / 255f);
            nvgColor.g(color.getGreen() / 255f);
            nvgColor.b(color.getBlue() / 255f);
            nvgColor.a(color.getAlpha() / 255f);

            nvgBeginPath(vg);
            nvgRoundedRect(vg, x, y, width, height, radius);
            nvgStrokeColor(vg, nvgColor);
            nvgStrokeWidth(vg, strokeWidth);
            nvgStroke(vg);
        } finally {
            if (usePerDrawStateIsolation)
                nvgRestore(vg);
        }
    }

    public static void drawRoundedRectGradient(float x, float y, float width, float height, float radius,
            Color colorTop, Color colorBottom) {
        if (usePerDrawStateIsolation)
            nvgSave(vg);
        try (MemoryStack stack = stackPush()) {
            NVGColor color1 = NVGColor.malloc(stack);
            color1.r(colorTop.getRed() / 255f);
            color1.g(colorTop.getGreen() / 255f);
            color1.b(colorTop.getBlue() / 255f);
            color1.a(colorTop.getAlpha() / 255f);

            NVGColor color2 = NVGColor.malloc(stack);
            color2.r(colorBottom.getRed() / 255f);
            color2.g(colorBottom.getGreen() / 255f);
            color2.b(colorBottom.getBlue() / 255f);
            color2.a(colorBottom.getAlpha() / 255f);

            NVGPaint paint = nvgLinearGradient(vg, x, y, x, y + height, color1, color2, NVGPaint.malloc(stack));

            nvgBeginPath(vg);
            nvgRoundedRect(vg, x, y, width, height, radius);
            nvgFillPaint(vg, paint);
            nvgFill(vg);
        } finally {
            if (usePerDrawStateIsolation)
                nvgRestore(vg);
        }
    }

    public static void drawCircle(float x, float y, float radius, Color color) {
        if (usePerDrawStateIsolation)
            nvgSave(vg);
        try (MemoryStack stack = stackPush()) {
            NVGColor nvgColor = NVGColor.malloc(stack);
            nvgColor.r(color.getRed() / 255f);
            nvgColor.g(color.getGreen() / 255f);
            nvgColor.b(color.getBlue() / 255f);
            nvgColor.a(color.getAlpha() / 255f);

            nvgBeginPath(vg);
            nvgCircle(vg, x, y, radius);
            nvgFillColor(vg, nvgColor);
            nvgFill(vg);
        } finally {
            if (usePerDrawStateIsolation)
                nvgRestore(vg);
        }
    }

    public static void drawText(String text, float x, float y, float size, Color color, boolean bold) {
        if (!inFrame) {
            throw new IllegalStateException("Must call beginFrame() before drawing text");
        }
        if (!initialized)
            init();

        int fontId = getFontId(bold);

        if (usePerDrawStateIsolation)
            nvgSave(vg);
        try (MemoryStack stack = stackPush()) {
            NVGColor nvgColor = NVGColor.malloc(stack);
            nvgColor.r(color.getRed() / 255f);
            nvgColor.g(color.getGreen() / 255f);
            nvgColor.b(color.getBlue() / 255f);
            nvgColor.a(color.getAlpha() / 255f);

            nvgFontFaceId(vg, fontId);
            nvgFontSize(vg, size);
            nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
            nvgFillColor(vg, nvgColor);
            nvgText(vg, x, y, text);
        } finally {
            if (usePerDrawStateIsolation)
                nvgRestore(vg);
        }
    }

    public static void drawText(String text, float x, float y, float size, Color color) {
        drawText(text, x, y, size, color, false);
    }

    public static void drawTextWithFont(String text, float x, float y, float size, Color color, int fontId) {
        if (!inFrame) {
            throw new IllegalStateException("Must call beginFrame() before drawing text");
        }
        if (!initialized)
            init();

        if (usePerDrawStateIsolation)
            nvgSave(vg);
        try (MemoryStack stack = stackPush()) {
            NVGColor nvgColor = NVGColor.malloc(stack);
            nvgColor.r(color.getRed() / 255f);
            nvgColor.g(color.getGreen() / 255f);
            nvgColor.b(color.getBlue() / 255f);
            nvgColor.a(color.getAlpha() / 255f);

            nvgFontFaceId(vg, fontId);
            nvgFontSize(vg, size);
            nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
            nvgFillColor(vg, nvgColor);
            nvgText(vg, x, y, text);
        } finally {
            if (usePerDrawStateIsolation)
                nvgRestore(vg);
        }
    }

    public static int getPoppinsFontId() {
        return poppinsFont > 0 ? poppinsFont : regularFont;
    }

    public static int getJetBrainsFontId() {
        return jetbrainsFont > 0 ? jetbrainsFont : regularFont;
    }

    public static int getRegularFontId() {
        return regularFont > 0 ? regularFont : 0;
    }

    public static int getMonacoFontId() {
        return monacoFont > 0 ? monacoFont : regularFont;
    }

    private static int getFontId(boolean bold) {
        try {
            String fontStyle = cc.silk.module.modules.client.ClientSettingsModule.getFontStyle();

            if ("JetbrainsMono.ttf".equals(fontStyle) || "jetbrainsmono.ttf".equals(fontStyle)) {
                return jetbrainsFont > 0 ? jetbrainsFont : regularFont;
            } else if ("Poppins-Medium.ttf".equals(fontStyle) || "poppins-medium.ttf".equals(fontStyle)) {
                return poppinsFont > 0 ? poppinsFont : regularFont;
            } else if ("monaco.ttf".equals(fontStyle) || "Monaco.ttf".equals(fontStyle)) {
                return monacoFont > 0 ? monacoFont : regularFont;
            } else if ("Inter.ttf".equals(fontStyle)) {
                return regularFont > 0 ? regularFont : 0;
            }
        } catch (Exception e) {

        }

        int fontId = bold ? boldFont : regularFont;

        if (fontId <= 0) {
            fontId = regularFont > 0 ? regularFont : boldFont;
        }

        if (fontId <= 0) {
            fontId = 1;
        }

        return fontId;
    }

    public static void drawIcon(String icon, float x, float y, float size, Color color) {
        if (!inFrame) {
            throw new IllegalStateException("Must call beginFrame() before drawing text");
        }
        if (!initialized)
            init();

        int fontId = iconFont > 0 ? iconFont : regularFont;

        if (usePerDrawStateIsolation)
            nvgSave(vg);
        try (MemoryStack stack = stackPush()) {
            NVGColor nvgColor = NVGColor.malloc(stack);
            nvgColor.r(color.getRed() / 255f);
            nvgColor.g(color.getGreen() / 255f);
            nvgColor.b(color.getBlue() / 255f);
            nvgColor.a(color.getAlpha() / 255f);

            nvgFontFaceId(vg, fontId);
            nvgFontSize(vg, size);
            nvgTextAlign(vg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
            nvgFillColor(vg, nvgColor);
            nvgText(vg, x, y, icon);
        } finally {
            if (usePerDrawStateIsolation)
                nvgRestore(vg);
        }
    }

    public static float getTextWidth(String text, float size, boolean bold) {
        if (!initialized)
            init();

        try (MemoryStack stack = stackPush()) {
            float[] bounds = new float[4];
            nvgFontFaceId(vg, getFontId(bold));
            nvgFontSize(vg, size);
            nvgTextBounds(vg, 0, 0, text, bounds);
            return bounds[2] - bounds[0];
        }
    }

    public static float getTextWidth(String text, float size) {
        return getTextWidth(text, size, false);
    }

    public static float getTextWidthWithFont(String text, float size, int fontId) {
        if (!initialized)
            init();

        try (MemoryStack stack = stackPush()) {
            float[] bounds = new float[4];
            nvgFontFaceId(vg, fontId);
            nvgFontSize(vg, size);
            nvgTextBounds(vg, 0, 0, text, bounds);
            return bounds[2] - bounds[0];
        }
    }

    public static float getTextHeight(float size) {
        return size;
    }

    public static boolean isInFrame() {
        return inFrame;
    }

    public static void drawRect(float x, float y, float width, float height, Color color) {
        if (usePerDrawStateIsolation)
            nvgSave(vg);
        try (MemoryStack stack = stackPush()) {
            NVGColor nvgColor = NVGColor.malloc(stack);
            nvgColor.r(color.getRed() / 255f);
            nvgColor.g(color.getGreen() / 255f);
            nvgColor.b(color.getBlue() / 255f);
            nvgColor.a(color.getAlpha() / 255f);

            nvgBeginPath(vg);
            nvgRect(vg, x, y, width, height);
            nvgFillColor(vg, nvgColor);
            nvgFill(vg);
        } finally {
            if (usePerDrawStateIsolation)
                nvgRestore(vg);
        }
    }

    public static void drawLine(float x1, float y1, float x2, float y2, float strokeWidth, Color color) {
        if (usePerDrawStateIsolation)
            nvgSave(vg);
        try (MemoryStack stack = stackPush()) {
            NVGColor nvgColor = NVGColor.malloc(stack);
            nvgColor.r(color.getRed() / 255f);
            nvgColor.g(color.getGreen() / 255f);
            nvgColor.b(color.getBlue() / 255f);
            nvgColor.a(color.getAlpha() / 255f);

            nvgBeginPath(vg);
            nvgMoveTo(vg, x1, y1);
            nvgLineTo(vg, x2, y2);
            nvgStrokeColor(vg, nvgColor);
            nvgStrokeWidth(vg, strokeWidth);
            nvgStroke(vg);
        } finally {
            if (usePerDrawStateIsolation)
                nvgRestore(vg);
        }
    }

    public static void save() {
        nvgSave(vg);
    }

    public static void restore() {
        nvgRestore(vg);
    }

    public static void translate(float x, float y) {
        nvgTranslate(vg, x, y);
    }

    public static void scale(float x, float y) {
        nvgScale(vg, x, y);
    }

    public static void scissor(float x, float y, float width, float height) {
        nvgScissor(vg, x, y, width, height);
    }

    public static void resetScissor() {
        nvgResetScissor(vg);
    }

    public static int loadImage(String path) {
        if (!initialized)
            init();

        ByteBuffer buffer = null;
        try {
            InputStream is = NanoVGRenderer.class.getClassLoader().getResourceAsStream(path);
            if (is == null)
                is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
            if (is == null)
                is = ClassLoader.getSystemResourceAsStream(path);
            if (is == null)
                is = NanoVGRenderer.class.getResourceAsStream("/" + path);

            if (is == null) {
                System.err.println("[NanoVG] Image resource not found: " + path);
                return -1;
            }

            buffer = MemoryUtil.memAlloc(8192);
            ReadableByteChannel rbc = Channels.newChannel(is);

            while (rbc.read(buffer) != -1) {
                if (buffer.remaining() == 0) {
                    ByteBuffer newBuffer = MemoryUtil.memAlloc(buffer.capacity() * 2);
                    buffer.flip();
                    newBuffer.put(buffer);
                    MemoryUtil.memFree(buffer);
                    buffer = newBuffer;
                }
            }

            buffer.flip();
            System.out.println("[NanoVG] Loading image: " + path + " (buffer size: " + buffer.remaining() + " bytes)");

            int image = nvgCreateImageMem(vg, NVG_IMAGE_GENERATE_MIPMAPS, buffer);

            if (image == -1) {
                System.err.println("[NanoVG] Failed to create image: " + path);
                return -1;
            }

            System.out.println("[NanoVG] Successfully loaded image: " + path + " (ID: " + image + ")");
            is.close();

            return image;
        } catch (Exception e) {
            System.err.println("[NanoVG] Error loading image: " + path);
            e.printStackTrace();
            return -1;
        } finally {
            if (buffer != null) {
                MemoryUtil.memFree(buffer);
            }
        }
    }

    public static void drawImage(int imageId, float x, float y, float width, float height, Color tint) {
        if (!inFrame) {
            throw new IllegalStateException("Must call beginFrame() before drawing image");
        }

        if (imageId == -1) {
            return;
        }

        if (usePerDrawStateIsolation)
            nvgSave(vg);
        try (MemoryStack stack = stackPush()) {
            float alpha = tint.getAlpha() / 255f;

            NVGPaint paint = nvgImagePattern(vg, x, y, width, height, 0, imageId, alpha, NVGPaint.malloc(stack));

            nvgBeginPath(vg);
            nvgRect(vg, x, y, width, height);
            nvgFillPaint(vg, paint);
            nvgFill(vg);
        } finally {
            if (usePerDrawStateIsolation)
                nvgRestore(vg);
        }
    }

    public static void cleanup() {
        if (vg != -1) {
            nvgDelete(vg);
            vg = -1;
        }
        initialized = false;
    }
}
