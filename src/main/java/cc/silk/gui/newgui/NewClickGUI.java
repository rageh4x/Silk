package cc.silk.gui.newgui;

import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.gui.newgui.components.CategoryPanel;
import cc.silk.utils.render.NanoVGRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class NewClickGUI extends Screen {
    private final List<CategoryPanel> panels = new ArrayList<>();
    private static final int PANEL_WIDTH = 110;
    private static final int PANEL_SPACING = 5;
    
    private float animationProgress = 0f;
    private boolean closing = false;
    private long lastFrameTime = System.currentTimeMillis();

    public NewClickGUI() {
        super(Text.literal("ClickGUI"));
        initPanels();
    }

    private void initPanels() {
        cc.silk.SilkClient.INSTANCE.getFontManager().initialize();

        int x = 20;
        int y = 20;

        for (Category category : Category.values()) {
            if (category == Category.CONFIG)
                continue;

            List<Module> modules = cc.silk.SilkClient.INSTANCE.getModuleManager().getModulesByCategory(category);
            if (!modules.isEmpty()) {
                panels.add(new CategoryPanel(category, x, y, PANEL_WIDTH));
                x += PANEL_WIDTH + PANEL_SPACING;
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastFrameTime) / 1000f;
        lastFrameTime = currentTime;
        
        if (closing) {
            animationProgress -= deltaTime * 4f;
            if (animationProgress <= 0f) {
                animationProgress = 0f;
                if (client != null) {
                    client.setScreen(null);
                }
                return;
            }
        } else {
            animationProgress += deltaTime * 4f;
            if (animationProgress > 1f) {
                animationProgress = 1f;
            }
        }
        
        float scale = easeOutCubic(animationProgress);
        float alpha = animationProgress;
        
        context.getMatrices().push();
        
        int centerX = width / 2;
        int centerY = height / 2;
        context.getMatrices().translate(centerX, centerY, 0);
        context.getMatrices().scale(scale, scale, 1f);
        context.getMatrices().translate(-centerX, -centerY, 0);
        
        for (CategoryPanel panel : panels) {
            panel.render(context, mouseX, mouseY, alpha);
        }
        
        context.getMatrices().pop();
    }
    
    private float easeOutCubic(float t) {
        return 1f - (float)Math.pow(1f - t, 3);
    }
    
    @Override
    public void close() {
        closing = true;
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (CategoryPanel panel : panels) {
            if (panel.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (CategoryPanel panel : panels) {
            panel.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        for (CategoryPanel panel : panels) {
            if (panel.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (CategoryPanel panel : panels) {
            if (panel.mouseScrolled(mouseX, mouseY, verticalAmount)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
