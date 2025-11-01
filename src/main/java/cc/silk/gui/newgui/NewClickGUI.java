package cc.silk.gui.newgui;

import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.gui.newgui.components.CategoryPanel;
import cc.silk.gui.newgui.components.ConfigPanel;
import cc.silk.utils.render.NanoVGRenderer;
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

    private String searchQuery = "";
    private boolean searchFocused = false;
    private float searchBarY = 0;
    private static final int SEARCH_BAR_HEIGHT = 30;
    private static final int SEARCH_BAR_WIDTH = 300;
    private static final java.awt.Color SEARCH_BG = new java.awt.Color(28, 28, 32, 255);
    private static final java.awt.Color SEARCH_TEXT = new java.awt.Color(240, 240, 245, 255);
    private static final java.awt.Color SEARCH_PLACEHOLDER = new java.awt.Color(130, 130, 140, 255);

    public NewClickGUI() {
        super(Text.literal("ClickGUI"));
        initPanels();
    }

    private void initPanels() {
        NanoVGRenderer.init();

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

        // Add config panel on the right side
        configPanel = new ConfigPanel(x, y, PANEL_WIDTH);
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

        float scale = easeOutCubic(animationProgress);
        float alpha = animationProgress;

        for (CategoryPanel panel : panels) {
            panel.update(deltaTime);
            panel.setSearchQuery(searchQuery);
        }

        if (configPanel != null) {
            configPanel.update(deltaTime);
        }

        int centerX = width / 2;
        int centerY = height / 2;

        CategoryPanel draggedPanel = null;
        for (CategoryPanel panel : panels) {
            if (panel.isDragging()) {
                draggedPanel = panel;
                break;
            }
        }

        boolean configDragging = configPanel != null && configPanel.isDragging();

        NanoVGRenderer.beginFrame();

        NanoVGRenderer.save();
        NanoVGRenderer.translate(centerX, centerY);
        NanoVGRenderer.scale(scale, scale);
        NanoVGRenderer.translate(-centerX, -centerY);

        renderSearchBar(mouseX, mouseY, alpha);

        for (CategoryPanel panel : panels) {
            if (panel != draggedPanel) {
                panel.render(mouseX, mouseY, alpha, scale, centerX, centerY);
            }
        }

        if (draggedPanel != null) {
            draggedPanel.render(mouseX, mouseY, alpha, scale, centerX, centerY);
        }

        // Render config panel
        if (configPanel != null && !configDragging) {
            configPanel.render(mouseX, mouseY, alpha, scale, centerX, centerY);
        }

        for (CategoryPanel panel : panels) {
            panel.renderSettingsPanel(mouseX, mouseY, alpha, width, height);
        }

        // Render config panel on top if dragging
        if (configPanel != null && configDragging) {
            configPanel.render(mouseX, mouseY, alpha, scale, centerX, centerY);
        }

        NanoVGRenderer.restore();

        NanoVGRenderer.endFrame();
    }

    private void renderSearchBar(int mouseX, int mouseY, float alpha) {
        searchBarY = height - SEARCH_BAR_HEIGHT - 20;
        float searchBarX = (width - SEARCH_BAR_WIDTH) / 2f;

        int bgAlpha = (int) (255 * alpha);
        Color bgColor = new Color(SEARCH_BG.getRed(), SEARCH_BG.getGreen(), SEARCH_BG.getBlue(), bgAlpha);

        Color accentColor = cc.silk.module.modules.client.NewClickGUIModule.getAccentColor();
        Color borderColor = new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(),
                searchFocused ? bgAlpha : (int) (100 * alpha));

        float cornerRadius = 6f;
        NanoVGRenderer.drawRoundedRect(searchBarX, searchBarY, SEARCH_BAR_WIDTH, SEARCH_BAR_HEIGHT, cornerRadius,
                bgColor);

        float borderThickness = searchFocused ? 2f : 1f;
        NanoVGRenderer.drawRoundedRectOutline(searchBarX, searchBarY, SEARCH_BAR_WIDTH, SEARCH_BAR_HEIGHT,
                cornerRadius, borderThickness, borderColor);

        String displayText = searchQuery.isEmpty() ? "Search modules..." : searchQuery;
        Color textColor = searchQuery.isEmpty()
                ? new Color(SEARCH_PLACEHOLDER.getRed(), SEARCH_PLACEHOLDER.getGreen(), SEARCH_PLACEHOLDER.getBlue(),
                        bgAlpha)
                : new Color(SEARCH_TEXT.getRed(), SEARCH_TEXT.getGreen(), SEARCH_TEXT.getBlue(), bgAlpha);

        float fontSize = 12f;
        float textX = searchBarX + 10;
        float textY = searchBarY + (SEARCH_BAR_HEIGHT - fontSize) / 2f;
        NanoVGRenderer.drawText(displayText, textX, textY, fontSize, textColor);

        if (searchFocused && System.currentTimeMillis() % 1000 < 500) {
            float cursorX = textX + NanoVGRenderer.getTextWidth(searchQuery, fontSize);
            NanoVGRenderer.drawRect(cursorX, textY - 1, 1, fontSize + 2, textColor);
        }
    }

    private float easeOutCubic(float t) {
        return 1f - (float) Math.pow(1f - t, 3);
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
        float searchBarX = (width - SEARCH_BAR_WIDTH) / 2f;
        if (mouseX >= searchBarX && mouseX <= searchBarX + SEARCH_BAR_WIDTH &&
                mouseY >= searchBarY && mouseY <= searchBarY + SEARCH_BAR_HEIGHT) {
            searchFocused = true;
            return true;
        } else {
            searchFocused = false;
        }

        if (configPanel != null && configPanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        for (CategoryPanel panel : panels) {
            if (panel.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (configPanel != null && configPanel.charTyped(chr, modifiers)) {
            return true;
        }

        if (searchFocused) {
            searchQuery += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (configPanel != null && configPanel.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        for (CategoryPanel panel : panels) {
            if (panel.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }

        if (searchFocused) {
            if (keyCode == 259) {
                if (!searchQuery.isEmpty()) {
                    searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                }
                return true;
            } else if (keyCode == 257 || keyCode == 335) {
                searchFocused = false;
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (configPanel != null) {
            configPanel.mouseReleased(mouseX, mouseY, button);
        }

        for (CategoryPanel panel : panels) {
            panel.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (configPanel != null && configPanel.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
            return true;
        }

        for (CategoryPanel panel : panels) {
            if (panel.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (configPanel != null && configPanel.mouseScrolled(mouseX, mouseY, verticalAmount)) {
            return true;
        }

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
