package cc.silk.gui.newgui;

import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.gui.newgui.components.CategoryPanel;
import cc.silk.utils.render.NanoVGRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
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
    
    private String searchQuery = "";
    private boolean searchFocused = false;
    private float searchBarY = 0;
    private static final int SEARCH_BAR_HEIGHT = 30;
    private static final int SEARCH_BAR_WIDTH = 300;
    private static final java.awt.Color SEARCH_BG = new java.awt.Color(28, 28, 32, 255);
    private static final java.awt.Color SEARCH_BORDER = new java.awt.Color(88, 101, 242, 255);
    private static final java.awt.Color SEARCH_TEXT = new java.awt.Color(240, 240, 245, 255);
    private static final java.awt.Color SEARCH_PLACEHOLDER = new java.awt.Color(130, 130, 140, 255);

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

        for (CategoryPanel panel : panels) {
            panel.update(deltaTime);
            panel.setSearchQuery(searchQuery);
        }

        // Begin NanoVG frame for all NanoVG rendering
        NanoVGRenderer.beginFrame();
        
        context.getMatrices().push();

        int centerX = width / 2;
        int centerY = height / 2;
        context.getMatrices().translate(centerX, centerY, 0);
        context.getMatrices().scale(scale, scale, 1f);
        context.getMatrices().translate(-centerX, -centerY, 0);

        // Render all NanoVG shapes for panels
        for (CategoryPanel panel : panels) {
            panel.renderNanoVG(context, mouseX, mouseY, alpha);
        }

        context.getMatrices().pop();

        // Render NanoVG shapes for settings panels
        for (CategoryPanel panel : panels) {
            panel.renderSettingsPanelNanoVG(context, mouseX, mouseY, alpha);
        }
        
        // Render search bar NanoVG shapes
        renderSearchBarNanoVG(context, mouseX, mouseY, alpha);
        
        // End NanoVG frame and restore MC state
        NanoVGRenderer.endFrame();
        
        // Now render all Minecraft elements (text, fills, etc.)
        context.getMatrices().push();
        context.getMatrices().translate(centerX, centerY, 0);
        context.getMatrices().scale(scale, scale, 1f);
        context.getMatrices().translate(-centerX, -centerY, 0);

        for (CategoryPanel panel : panels) {
            panel.renderMinecraft(context, mouseX, mouseY, alpha);
        }

        context.getMatrices().pop();

        for (CategoryPanel panel : panels) {
            panel.renderSettingsPanelMinecraft(context, mouseX, mouseY, alpha);
        }
        
        renderSearchBarMinecraft(context, mouseX, mouseY, alpha);
    }
    
    private void renderSearchBarNanoVG(DrawContext context, int mouseX, int mouseY, float alpha) {
        searchBarY = height - SEARCH_BAR_HEIGHT - 20;
        float searchBarX = (width - SEARCH_BAR_WIDTH) / 2f;
        
        int bgAlpha = (int)(255 * alpha);
        java.awt.Color bgColor = new java.awt.Color(SEARCH_BG.getRed(), SEARCH_BG.getGreen(), SEARCH_BG.getBlue(), bgAlpha);
        java.awt.Color borderColor = new java.awt.Color(SEARCH_BORDER.getRed(), SEARCH_BORDER.getGreen(), SEARCH_BORDER.getBlue(), 
                searchFocused ? bgAlpha : (int)(100 * alpha));
        
        float cornerRadius = 6f;
        NanoVGRenderer.drawRoundedRectWithContext(context, searchBarX, searchBarY, SEARCH_BAR_WIDTH, SEARCH_BAR_HEIGHT, cornerRadius, bgColor);
        
        float borderThickness = searchFocused ? 2f : 1f;
        NanoVGRenderer.drawRoundedRectOutlineWithContext(context, searchBarX, searchBarY, SEARCH_BAR_WIDTH, SEARCH_BAR_HEIGHT, cornerRadius, borderThickness, borderColor);
    }
    
    private void renderSearchBarMinecraft(DrawContext context, int mouseX, int mouseY, float alpha) {
        searchBarY = height - SEARCH_BAR_HEIGHT - 20;
        float searchBarX = (width - SEARCH_BAR_WIDTH) / 2f;
        
        int bgAlpha = (int)(255 * alpha);
        cc.silk.utils.render.font.fonts.FontRenderer font = cc.silk.SilkClient.INSTANCE.getFontManager()
                .getSize(12, cc.silk.utils.render.font.FontManager.Type.Inter);
        
        String displayText = searchQuery.isEmpty() ? "Search modules..." : searchQuery;
        java.awt.Color textColor = searchQuery.isEmpty() ? 
                new java.awt.Color(SEARCH_PLACEHOLDER.getRed(), SEARCH_PLACEHOLDER.getGreen(), SEARCH_PLACEHOLDER.getBlue(), bgAlpha) :
                new java.awt.Color(SEARCH_TEXT.getRed(), SEARCH_TEXT.getGreen(), SEARCH_TEXT.getBlue(), bgAlpha);
        
        float textX = searchBarX + 10;
        float textY = searchBarY + (SEARCH_BAR_HEIGHT - 9) / 2f;
        font.drawString(context.getMatrices(), displayText, textX, textY, textColor);
        
        if (searchFocused && System.currentTimeMillis() % 1000 < 500) {
            float cursorX = textX + font.getStringWidth(searchQuery);
            context.fill((int)cursorX, (int)(textY - 1), (int)(cursorX + 1), (int)(textY + 10), textColor.getRGB());
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
        
        for (CategoryPanel panel : panels) {
            if (panel.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocused) {
            searchQuery += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
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
