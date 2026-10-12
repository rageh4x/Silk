package cc.silk.gui.newgui;

import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.gui.newgui.components.CategoryPanel;
import cc.silk.gui.newgui.components.ConfigPanel;
import cc.silk.gui.FriendsScreen;
import cc.silk.gui.effects.SnowEffect;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class NewClickGUI extends Screen {
    private final List<CategoryPanel> panels = new ArrayList<>();
    private ConfigPanel configPanel;
    private static final int PANEL_WIDTH = 110;
    private static final int PANEL_SPACING = 5;

    private float animationProgress = 0f;
    private boolean closing = false;
    private long lastFrameTime = System.currentTimeMillis();
    
    private final SnowEffect snowEffect = new SnowEffect();

    private String searchQuery = "";
    private boolean searchFocused = false;
    private float searchExpandProgress = 0f;
    private float searchBarY = 0;
    private static final int SEARCH_BAR_WIDTH = 300;
    private static final int SEARCH_ICON_SIZE = 35;

    private static final int FRIENDS_BUTTON_SIZE = 35;
    private float friendsButtonX = 0;
    private float friendsButtonY = 0;

    public NewClickGUI() {
        super(Text.literal("ClickGUI"));
        initPanels();
    }

    private void initPanels() {
        try {
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

            configPanel = new ConfigPanel(x, y, PANEL_WIDTH);
        } catch (Exception ignored) {}
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

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

        int centerX = width / 2;
        int centerY = height / 2;
        float alpha = animationProgress;
        float scale = 1.0f;

        try {
            for (CategoryPanel panel : panels) {
                panel.update(deltaTime);
                panel.render(mouseX, mouseY, alpha, scale, centerX, centerY);
            }

            if (configPanel != null) {
                configPanel.update(deltaTime);
                configPanel.render(mouseX, mouseY, alpha, scale, centerX, centerY);
            }
        } catch (Exception e) {
            // Error handling taaki game crash na ho
        }
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
        try {
            for (CategoryPanel panel : panels) {
                if (panel.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
            if (configPanel != null && configPanel.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        } catch (Exception ignored) {}
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
