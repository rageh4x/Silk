package cc.silk.utils.render.nanovg;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.nanovg.NanoVG.*;

public class NanoVGFrameManager {
    private static boolean inFrame = false;

    public static void beginFrame() {
        RenderSystem.assertOnRenderThread();

        if (!NanoVGContext.isInitialized() || !NanoVGContext.isValid()) {
            NanoVGContext.init();
            NanoVGFontManager.loadFonts();
        }

        if (inFrame) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null) {
            return;
        }

        int framebufferWidth = mc.getWindow().getFramebufferWidth();
        int framebufferHeight = mc.getWindow().getFramebufferHeight();

        if (framebufferWidth <= 0 || framebufferHeight <= 0) {
            return;
        }

        try {
            RenderSystem.assertOnRenderThreadOrInit();

            mc.getFramebuffer().beginWrite(true);

            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_STENCIL_TEST);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);

            NanoVGContext.assertValid();
            nvgBeginFrame(NanoVGContext.getHandle(), framebufferWidth, framebufferHeight, 1f);

            int scaledWidth = mc.getWindow().getScaledWidth();
            int scaledHeight = mc.getWindow().getScaledHeight();
            float scaleX = (float) framebufferWidth / (float) scaledWidth;
            float scaleY = (float) framebufferHeight / (float) scaledHeight;
            nvgScale(NanoVGContext.getHandle(), scaleX, scaleY);

            inFrame = true;
        } catch (Exception e) {
            inFrame = false;
            throw e;
        }
    }

    public static void endFrame() {
        if (!inFrame) {
            return;
        }

        RenderSystem.assertOnRenderThread();

        try {
            NanoVGContext.assertValid();
            nvgEndFrame(NanoVGContext.getHandle());
        } catch (Exception e) {
            inFrame = false;
            throw e;
        }

        inFrame = false;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.getFramebuffer() != null) {
            mc.getFramebuffer().beginWrite(true);
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_STENCIL_TEST);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        org.lwjgl.opengl.GL30.glBindVertexArray(0);
        org.lwjgl.opengl.GL15.glBindBuffer(org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER, 0);
        org.lwjgl.opengl.GL15.glBindBuffer(org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public static boolean isInFrame() {
        return inFrame;
    }

    public static void resetInFrame() {
        inFrame = false;
    }
}
