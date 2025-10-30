package cc.silk.gui.newgui.components;

import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.utils.render.NanoVGRenderer;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryPanel {
    private final Category category;
    private float x, y;
    private final int width;
    private final List<ModuleButton> moduleButtons = new ArrayList<>();

    private static final int HEADER_HEIGHT = 18;
    private static final int MODULE_HEIGHT = 12;
    private static final int MAX_VISIBLE_MODULES = 15;

    private static final Color HEADER_COLOR = new Color(28, 28, 32, 255);
    private static final Color PANEL_BG = new Color(18, 18, 22, 255);
    private static final Color ACCENT_COLOR = new Color(88, 101, 242, 255);
    private static final Color SEPARATOR_COLOR = new Color(88, 101, 242, 80);
    private static final float CORNER_RADIUS = 4f;

    private float scrollOffset = 0;
    private float maxScroll = 0;

    private boolean dragging = false;
    private float dragOffsetX, dragOffsetY;
    
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
        float moduleY = 0;

        for (Module module : modules) {
            moduleButtons.add(new ModuleButton(module, 0, moduleY, width, MODULE_HEIGHT));
            moduleY += MODULE_HEIGHT;
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

    public void render(DrawContext context, int mouseX, int mouseY, float alpha) {
        int visibleModules = Math.min(moduleButtons.size(), MAX_VISIBLE_MODULES);
        int totalHeight = HEADER_HEIGHT + 1 + (visibleModules * MODULE_HEIGHT);

        maxScroll = Math.max(0, (moduleButtons.size() - MAX_VISIBLE_MODULES) * MODULE_HEIGHT);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        int panelAlpha = (int) (255 * alpha);
        Color panelBg = new Color(PANEL_BG.getRed(), PANEL_BG.getGreen(), PANEL_BG.getBlue(), panelAlpha);
        Color headerColor = new Color(HEADER_COLOR.getRed(), HEADER_COLOR.getGreen(), HEADER_COLOR.getBlue(),
                panelAlpha);
        Color separatorColor = new Color(SEPARATOR_COLOR.getRed(), SEPARATOR_COLOR.getGreen(), SEPARATOR_COLOR.getBlue(), 
                (int)(SEPARATOR_COLOR.getAlpha() * alpha / 255.0));
        Color borderColor = new Color(40, 40, 46, panelAlpha);

        NanoVGRenderer.drawRoundedRectWithContext(context, x, y, width, totalHeight, CORNER_RADIUS, panelBg);
        NanoVGRenderer.drawRoundedRectOutlineWithContext(context, x, y, width, totalHeight, CORNER_RADIUS, 1f, borderColor);
        
        NanoVGRenderer.drawRoundedRectWithContext(context, x, y, width, HEADER_HEIGHT, CORNER_RADIUS, headerColor);
        
        context.fill((int) x, (int) (y + HEADER_HEIGHT - CORNER_RADIUS), (int) (x + width), (int) (y + HEADER_HEIGHT), headerColor.getRGB());
        
        context.fill((int) x, (int) (y + HEADER_HEIGHT), (int) (x + width), (int) (y + HEADER_HEIGHT + 1),
                separatorColor.getRGB());

        String categoryName = category.getName();
        cc.silk.utils.render.font.fonts.FontRenderer font = cc.silk.SilkClient.INSTANCE.getFontManager().getSize(12,
                cc.silk.utils.render.font.FontManager.Type.Inter);

        float iconX = x + 5;
        float textY = y + (HEADER_HEIGHT - 9) / 2f;
        float iconY = y + (HEADER_HEIGHT - CategoryIcon.getIconSize()) / 2f;

        Color iconColor = new Color(ACCENT_COLOR.getRed(), ACCENT_COLOR.getGreen(), ACCENT_COLOR.getBlue(), (int) (255 * alpha));
        CategoryIcon.drawIcon(context, category, iconX, iconY, iconColor);

        float textX = iconX + CategoryIcon.getIconSize() + 4;
        Color textColor = new Color(240, 240, 245, (int) (255 * alpha));
        font.drawString(context.getMatrices(), categoryName, textX, textY, textColor);

        int scissorX = (int) x;
        int scissorY = (int) (y + HEADER_HEIGHT + 1);
        int scissorWidth = width;
        int scissorHeight = visibleModules * MODULE_HEIGHT;

        context.enableScissor(scissorX, scissorY, scissorX + scissorWidth, scissorY + scissorHeight);

        float moduleY = y + HEADER_HEIGHT + 1 - scrollOffset;
        for (ModuleButton button : moduleButtons) {
            button.render(context, x, moduleY, mouseX, mouseY, alpha);
            moduleY += MODULE_HEIGHT;
        }

        context.disableScissor();

        if (moduleButtons.size() > MAX_VISIBLE_MODULES) {
            drawScrollbar(context, totalHeight, alpha);
        }
    }

    public void render(DrawContext context, int mouseX, int mouseY) {
        render(context, mouseX, mouseY, 1f);
    }
    
    public void renderSettingsPanel(DrawContext context, int mouseX, int mouseY, float alpha) {
        if (settingsPanel != null && settingsPanel.isAnimating()) {
            float lineAlpha = alpha * settingsPanel.getAnimationProgress();
            Color lineColor = new Color(ACCENT_COLOR.getRed(), ACCENT_COLOR.getGreen(), ACCENT_COLOR.getBlue(), (int)(150 * lineAlpha));
            
            float lineStartX = x + width;
            float lineStartY = settingsPanelSourceY + (MODULE_HEIGHT / 2f);
            float lineEndX = settingsPanel.getCurrentX();
            float lineEndY = settingsPanel.getY() + 11;
            
            context.fill((int)lineStartX, (int)lineStartY, (int)lineEndX, (int)(lineStartY + 1), lineColor.getRGB());
            
            if (lineEndY != lineStartY) {
                float verticalLineX = lineEndX;
                float minY = Math.min(lineStartY, lineEndY);
                float maxY = Math.max(lineStartY, lineEndY);
                context.fill((int)verticalLineX, (int)minY, (int)(verticalLineX + 1), (int)maxY, lineColor.getRGB());
            }
            
            settingsPanel.render(context, mouseX, mouseY, alpha);
        }
    }

    private void drawScrollbar(DrawContext context, int totalHeight, float alpha) {
        int scrollbarHeight = (int) ((float) MAX_VISIBLE_MODULES / moduleButtons.size()
                * (MAX_VISIBLE_MODULES * MODULE_HEIGHT));
        int scrollbarY = (int) (y + HEADER_HEIGHT + 1
                + (scrollOffset / maxScroll) * ((MAX_VISIBLE_MODULES * MODULE_HEIGHT) - scrollbarHeight));

        Color scrollbarBg = new Color(35, 35, 40, (int) (180 * alpha));
        Color scrollbarColor = new Color(ACCENT_COLOR.getRed(), ACCENT_COLOR.getGreen(), ACCENT_COLOR.getBlue(), (int) (200 * alpha));
        
        context.fill((int) (x + width - 3), (int)(y + HEADER_HEIGHT + 1), (int) (x + width - 1), 
                (int)(y + HEADER_HEIGHT + 1 + MAX_VISIBLE_MODULES * MODULE_HEIGHT), scrollbarBg.getRGB());
        
        context.fill((int) (x + width - 3), scrollbarY, (int) (x + width - 1), scrollbarY + scrollbarHeight,
                scrollbarColor.getRGB());
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (isMouseOver(mouseX, mouseY)) {
            scrollOffset -= (float) (amount * MODULE_HEIGHT);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
            return true;
        }
        return false;
    }

    private boolean isMouseOver(double mouseX, double mouseY) {
        int visibleModules = Math.min(moduleButtons.size(), MAX_VISIBLE_MODULES);
        int totalHeight = HEADER_HEIGHT + (visibleModules * MODULE_HEIGHT);
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + totalHeight;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOverHeader(mouseX, mouseY)) {
            dragging = true;
            dragOffsetX = (float) (mouseX - x);
            dragOffsetY = (float) (mouseY - y);
            return true;
        }

        int visibleModules = Math.min(moduleButtons.size(), MAX_VISIBLE_MODULES);
        int visibleHeight = visibleModules * MODULE_HEIGHT;
        float panelHeaderY = y + HEADER_HEIGHT + 1;

        if (settingsPanel != null && settingsPanel.isAnimating()) {
            if (settingsPanel.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        
        float moduleY = y + HEADER_HEIGHT + 1 - scrollOffset;
        for (ModuleButton moduleButton : moduleButtons) {
            if (moduleButton.mouseClicked(x, moduleY, mouseX, mouseY, button, panelHeaderY, visibleHeight)) {
                if (button == 1 && moduleButton.getModule().getSettings().size() > 1) {
                    float settingsX = x + width + 5;
                    float settingsY = moduleButton.getAbsoluteY(y + HEADER_HEIGHT + 1 - scrollOffset);
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
}
