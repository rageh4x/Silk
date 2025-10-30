package cc.silk.gui.newgui.components;

import cc.silk.module.Module;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

public class ModuleButton {
    private final Module module;
    private final float relativeX, relativeY;
    private final int width, height;

    private float searchAlpha = 1f;
    private float targetSearchAlpha = 1f;
    private boolean matchesSearch = true;

    public ModuleButton(Module module, float relativeX, float relativeY, int width, int height) {
        this.module = module;
        this.relativeX = relativeX;
        this.relativeY = relativeY;
        this.width = width;
        this.height = height;
    }

    public void updateSearch(String query, float deltaTime) {
        this.searchQuery = query;

        if (query.isEmpty()) {
            matchesSearch = true;
            targetSearchAlpha = 1f;
        } else {
            matchesSearch = module.getName().toLowerCase().contains(query);
            targetSearchAlpha = matchesSearch ? 1f : 0f;
        }

        float speed = 8f;
        if (searchAlpha < targetSearchAlpha) {
            searchAlpha += deltaTime * speed;
            if (searchAlpha > targetSearchAlpha)
                searchAlpha = targetSearchAlpha;
        } else if (searchAlpha > targetSearchAlpha) {
            searchAlpha -= deltaTime * speed;
            if (searchAlpha < targetSearchAlpha)
                searchAlpha = targetSearchAlpha;
        }
    }

    public void render(DrawContext context, float panelX, float panelY, int mouseX, int mouseY, float alpha) {
        if (searchAlpha <= 0.01f)
            return;

        float combinedAlpha = alpha * searchAlpha;

        float x = panelX + relativeX;
        float y = panelY + relativeY;
        float buttonWidth = width;
        float buttonHeight = height;

        boolean hovered = isMouseOver(x, y, buttonWidth, buttonHeight, mouseX, mouseY);

        if (hovered && searchAlpha > 0.5f) {
            Color hoverBg = new Color(30, 30, 35, (int) (180 * combinedAlpha));
            context.fill((int) x, (int) y, (int) (x + buttonWidth), (int) (y + buttonHeight), hoverBg.getRGB());
        }

        if (module.isEnabled()) {
            Color accentColor = new Color(88, 101, 242, (int) (255 * combinedAlpha));
            context.fill((int) x, (int) y, (int) (x + 2), (int) (y + buttonHeight), accentColor.getRGB());

            Color glowColor = new Color(88, 101, 242, (int) (40 * combinedAlpha));
            context.fill((int) (x + 2), (int) y, (int) (x + 4), (int) (y + buttonHeight), glowColor.getRGB());
        }

        if (matchesSearch && searchAlpha > 0.9f && !searchQuery.isEmpty()) {
            Color highlightBg = new Color(88, 101, 242, (int) (30 * combinedAlpha));
            context.fill((int) x, (int) y, (int) (x + buttonWidth), (int) (y + buttonHeight), highlightBg.getRGB());
        }

        cc.silk.utils.render.font.fonts.FontRenderer font = cc.silk.SilkClient.INSTANCE.getFontManager().getSize(11,
                cc.silk.utils.render.font.FontManager.Type.Inter);
        Color textColor;
        if (module.isEnabled()) {
            textColor = new Color(240, 240, 245, (int) (255 * combinedAlpha));
        } else if (hovered) {
            textColor = new Color(200, 200, 210, (int) (255 * combinedAlpha));
        } else {
            textColor = new Color(130, 130, 140, (int) (255 * combinedAlpha));
        }
        float textY = y + (buttonHeight - 8) / 2f;
        float textX = x + (module.isEnabled() ? 10 : 6);
        font.drawString(context.getMatrices(), module.getName(), textX, textY, textColor);
    }

    private String searchQuery = "";

    public void render(DrawContext context, float panelX, float panelY, int mouseX, int mouseY) {
        render(context, panelX, panelY, mouseX, mouseY, 1f);
    }

    public boolean mouseClicked(float panelX, float panelY, double mouseX, double mouseY, int button,
            float panelHeaderY, int visibleHeight) {
        if (searchAlpha <= 0.01f)
            return false;

        float x = panelX + relativeX;
        float y = panelY + relativeY;
        float buttonWidth = width;
        float buttonHeight = height;

        if (y < panelHeaderY || y + buttonHeight > panelHeaderY + visibleHeight) {
            return false;
        }

        if (isMouseOver(x, y, buttonWidth, buttonHeight, mouseX, mouseY)) {
            if (button == 0) {
                module.toggle();
                return true;
            } else if (button == 1) {
                return true;
            }
        }
        return false;
    }

    public Module getModule() {
        return module;
    }

    public float getAbsoluteY(float panelY) {
        return panelY + relativeY;
    }

    private boolean isMouseOver(float x, float y, float width, float height, double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
