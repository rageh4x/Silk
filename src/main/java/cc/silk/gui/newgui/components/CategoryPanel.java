package cc.silk.gui.newgui.components;

import cc.silk.module.Category;
import cc.silk.module.Module;
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
    private static final int MODULE_HEIGHT = 14;
    private static final int MAX_VISIBLE_MODULES = 15;

    private static final Color HEADER_COLOR = new Color(35, 35, 38, 255);
    private static final Color PANEL_BG = new Color(25, 25, 28, 255);

    private float scrollOffset = 0;
    private float maxScroll = 0;

    private boolean dragging = false;
    private float dragOffsetX, dragOffsetY;

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

    public void render(DrawContext context, int mouseX, int mouseY) {
        int visibleModules = Math.min(moduleButtons.size(), MAX_VISIBLE_MODULES);
        int totalHeight = HEADER_HEIGHT + 1 + (visibleModules * MODULE_HEIGHT);

        maxScroll = Math.max(0, (moduleButtons.size() - MAX_VISIBLE_MODULES) * MODULE_HEIGHT);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        context.fill((int) x, (int) y, (int) (x + width), (int) (y + totalHeight), PANEL_BG.getRGB());
        context.fill((int) x, (int) y, (int) (x + width), (int) (y + HEADER_HEIGHT), HEADER_COLOR.getRGB());
        context.fill((int) x, (int) (y + HEADER_HEIGHT), (int) (x + width), (int) (y + HEADER_HEIGHT + 1),
                new Color(50, 50, 55, 255).getRGB());

        String categoryName = category.getName();
        cc.silk.utils.render.font.fonts.FontRenderer font = cc.silk.SilkClient.INSTANCE.getFontManager().getSize(12,
                cc.silk.utils.render.font.FontManager.Type.Inter);
        float textY = y + (HEADER_HEIGHT - 9) / 2f;
        font.drawString(context.getMatrices(), categoryName, x + 6, textY, Color.WHITE);

        int scissorX = (int) x;
        int scissorY = (int) (y + HEADER_HEIGHT + 1);
        int scissorWidth = width;
        int scissorHeight = visibleModules * MODULE_HEIGHT;

        context.enableScissor(scissorX, scissorY, scissorX + scissorWidth, scissorY + scissorHeight);

        float moduleY = y + HEADER_HEIGHT + 1 - scrollOffset;
        for (ModuleButton button : moduleButtons) {
            button.render(context, x, moduleY, mouseX, mouseY);
            moduleY += MODULE_HEIGHT;
        }

        context.disableScissor();

        if (moduleButtons.size() > MAX_VISIBLE_MODULES) {
            drawScrollbar(context, totalHeight);
        }
    }

    private void drawScrollbar(DrawContext context, int totalHeight) {
        int scrollbarHeight = (int) ((float) MAX_VISIBLE_MODULES / moduleButtons.size()
                * (MAX_VISIBLE_MODULES * MODULE_HEIGHT));
        int scrollbarY = (int) (y + HEADER_HEIGHT + 1
                + (scrollOffset / maxScroll) * ((MAX_VISIBLE_MODULES * MODULE_HEIGHT) - scrollbarHeight));

        context.fill((int) (x + width - 2), scrollbarY, (int) (x + width), scrollbarY + scrollbarHeight,
                new Color(80, 80, 80, 255).getRGB());
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

        float moduleY = y + HEADER_HEIGHT + 1 - scrollOffset;
        for (ModuleButton moduleButton : moduleButtons) {
            if (moduleButton.mouseClicked(x, moduleY, mouseX, mouseY, button, panelHeaderY, visibleHeight)) {
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
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
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
