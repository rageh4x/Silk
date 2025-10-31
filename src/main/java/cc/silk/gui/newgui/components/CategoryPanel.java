package cc.silk.gui.newgui.components;

import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.utils.render.NanoVGRenderer;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryPanel {
    private final Category category;
    private float x, y;
    private final int width;
    private final List<ModuleButton> moduleButtons = new ArrayList<>();

    private static final int HEADER_HEIGHT = 16;
    private static final int MODULE_HEIGHT = 14;

    private static final Color HEADER_COLOR = new Color(28, 28, 32, 255);
    private static final Color PANEL_BG = new Color(18, 18, 22, 255);
    private static final Color ACCENT_COLOR = new Color(88, 101, 242, 255);
    private static final Color SEPARATOR_COLOR = new Color(88, 101, 242, 80);
    private static final float CORNER_RADIUS = 4f;

    private boolean dragging = false;
    private float dragOffsetX, dragOffsetY;

    public boolean isDragging() {
        return dragging;
    }

    private SettingsPanel settingsPanel = null;
    private float settingsPanelSourceY = 0;

    private String searchQuery = "";

    public CategoryPanel(Category category, float x, float y, int width) {
        this.category = category;
        this.x = x;
        this.y = y;
        this.width = width;

        initModules();
    }

    private void initModules() {
        List<Module> modules = cc.silk.SilkClient.INSTANCE.getModuleManager().getModulesByCategory(category);

        for (Module module : modules) {
            moduleButtons.add(new ModuleButton(module, 0, 0, width, MODULE_HEIGHT));
        }
    }

    public void update(float deltaTime) {
        if (settingsPanel != null) {
            settingsPanel.update(deltaTime);
        }

        for (ModuleButton button : moduleButtons) {
            button.updateSearch(searchQuery, deltaTime);
        }
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query.toLowerCase();
    }

    public void render(int mouseX, int mouseY, float alpha, float scale, int centerX, int centerY) {
        int visibleCount = 0;
        for (ModuleButton button : moduleButtons) {
            if (button.getSearchAlpha() > 0.01f) {
                visibleCount++;
            }
        }

        int totalHeight = HEADER_HEIGHT + (visibleCount * MODULE_HEIGHT);

        int panelAlpha = (int) (255 * alpha);
        Color panelBg = new Color(PANEL_BG.getRed(), PANEL_BG.getGreen(), PANEL_BG.getBlue(), panelAlpha);
        Color headerColor = new Color(HEADER_COLOR.getRed(), HEADER_COLOR.getGreen(), HEADER_COLOR.getBlue(),
                panelAlpha);
        Color borderColor = new Color(40, 40, 46, panelAlpha);

        NanoVGRenderer.drawRoundedRect(x, y, width, totalHeight, CORNER_RADIUS, panelBg);
        NanoVGRenderer.drawRoundedRectOutline(x, y, width, totalHeight, CORNER_RADIUS, 1f, borderColor);

        NanoVGRenderer.drawRoundedRect(x, y, width, HEADER_HEIGHT, CORNER_RADIUS, headerColor);

        String categoryName = category.getName();
        float fontSize = 10f;
        float iconSize = CategoryIcons.getIconSize();
        float iconX = x + 6 + iconSize / 2f;
        float iconY = y + HEADER_HEIGHT / 2f;
        float textX = x + 6 + iconSize + 4;
        float textY = y + (HEADER_HEIGHT - fontSize) / 2f;

        Color iconColor = new Color(ACCENT_COLOR.getRed(), ACCENT_COLOR.getGreen(), ACCENT_COLOR.getBlue(),
                (int) (255 * alpha));
        Color textColor = new Color(240, 240, 245, (int) (255 * alpha));

        CategoryIcons.drawIcon(category, iconX, iconY, iconSize, iconColor);
        NanoVGRenderer.drawText(categoryName, textX, textY, fontSize, textColor);

        float moduleY = y + HEADER_HEIGHT;

        for (ModuleButton button : moduleButtons) {
            if (button.getSearchAlpha() > 0.01f) {
                button.render(x, moduleY, mouseX, mouseY, alpha);
                moduleY += MODULE_HEIGHT;
            }
        }
    }

    public void renderSettingsPanel(int mouseX, int mouseY, float alpha, int screenWidth, int screenHeight) {
        if (settingsPanel != null && settingsPanel.isAnimating()) {
            settingsPanel.setScreenDimensions(screenWidth, screenHeight);
            float lineAlpha = alpha * settingsPanel.getAnimationProgress();
            Color lineColor = new Color(ACCENT_COLOR.getRed(), ACCENT_COLOR.getGreen(), ACCENT_COLOR.getBlue(),
                    (int) (150 * lineAlpha));

            float lineStartX = x + width;
            float lineStartY = settingsPanelSourceY + (MODULE_HEIGHT / 2f);
            float lineEndX = settingsPanel.getCurrentX();
            float lineEndY = settingsPanel.getY() + 11;

            NanoVGRenderer.drawLine(lineStartX, lineStartY, lineEndX, lineStartY, 1f, lineColor);

            if (lineEndY != lineStartY) {
                NanoVGRenderer.drawLine(lineEndX, lineStartY, lineEndX, lineEndY, 1f, lineColor);
            }

            settingsPanel.render(mouseX, mouseY, alpha);
        }
    }

    public SettingsPanel getSettingsPanel() {
        return settingsPanel;
    }

    public boolean hasActiveSettingsPanel() {
        return settingsPanel != null && settingsPanel.isAnimating();
    }

    private boolean isMouseOver(double mouseX, double mouseY) {
        int visibleCount = 0;
        for (ModuleButton button : moduleButtons) {
            if (button.getSearchAlpha() > 0.01f) {
                visibleCount++;
            }
        }
        int totalHeight = HEADER_HEIGHT + (visibleCount * MODULE_HEIGHT);
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + totalHeight;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOverHeader(mouseX, mouseY)) {
            dragging = true;
            dragOffsetX = (float) (mouseX - x);
            dragOffsetY = (float) (mouseY - y);
            return true;
        }

        if (settingsPanel != null && settingsPanel.isAnimating()) {
            if (settingsPanel.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        float moduleY = y + HEADER_HEIGHT;
        for (ModuleButton moduleButton : moduleButtons) {
            if (moduleButton.getSearchAlpha() > 0.01f) {
                if (moduleButton.mouseClicked(x, moduleY, mouseX, mouseY, button)) {
                    if (button == 1 && moduleButton.getModule().getSettings().size() > 1) {
                        float settingsX = x + width + 5;
                        float settingsY = moduleY;
                        settingsPanelSourceY = moduleY;

                        if (settingsPanel != null && settingsPanel.isVisible()) {
                            settingsPanel.hide();
                        } else {
                            settingsPanel = new SettingsPanel(moduleButton.getModule(), settingsX, settingsY);
                            settingsPanel.show(settingsX, settingsY);
                        }
                    }
                    return true;
                }
                moduleY += MODULE_HEIGHT;
            }
        }

        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
        if (settingsPanel != null) {
            settingsPanel.mouseReleased(mouseX, mouseY, button);
        }
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (settingsPanel != null && settingsPanel.mouseDragged(mouseX, mouseY, button)) {
            return true;
        }
        if (dragging) {
            x = (float) (mouseX - dragOffsetX);
            y = (float) (mouseY - dragOffsetY);
            return true;
        }
        return false;
    }

    private boolean isMouseOverHeader(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + HEADER_HEIGHT;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (settingsPanel != null && settingsPanel.isAnimating()) {
            return settingsPanel.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }
}
