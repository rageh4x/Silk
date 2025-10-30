package cc.silk.module.modules.render;

import cc.silk.event.impl.render.EventRender2D;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.utils.render.NanoVGRenderer;
import meteordevelopment.orbit.EventHandler;

import java.awt.*;

public class NanoVGTest extends Module {

    public NanoVGTest() {
        super("NanoVG Test", "Test module for NanoVG rounded rectangles", Category.RENDER);
    }

    @EventHandler
    private void onRender2D(EventRender2D event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        int screenWidth = event.getWidth();
        int screenHeight = event.getHeight();

        NanoVGRenderer.beginFrame();

        NanoVGRenderer.drawRoundedRect(
            screenWidth / 2f - 100, 
            screenHeight / 2f - 150, 
            200, 
            80, 
            15, 
            new Color(100, 150, 255, 200)
        );

        NanoVGRenderer.drawRoundedRectOutline(
            screenWidth / 2f - 100, 
            screenHeight / 2f - 50, 
            200, 
            80, 
            15, 
            3, 
            new Color(255, 100, 100, 255)
        );

        NanoVGRenderer.drawRoundedRectGradient(
            screenWidth / 2f - 100, 
            screenHeight / 2f + 50, 
            200, 
            80, 
            15, 
            new Color(255, 100, 255, 200), 
            new Color(100, 100, 255, 200)
        );

        NanoVGRenderer.drawCircle(
            screenWidth / 2f + 150, 
            screenHeight / 2f, 
            30, 
            new Color(100, 255, 100, 200)
        );

        NanoVGRenderer.endFrame();
    }
}
