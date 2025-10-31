package cc.silk.gui.newgui.components;

import cc.silk.module.Module;
import cc.silk.utils.render.NanoVGRenderer;

import java.awt.*;

public class ModuleButton {
    private final Module module;
    private final float relativeX, relativeY;
    private final int width, height;

    private float searchAlpha = 1f;
    private float targetSearchAlpha = 1f;
    private boolean matchesSearch = true;
    private float hoverAlpha = 0f;
    private float targetHoverAlpha = 0f;

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

        float hoverSpeed = 10f;
        if (hoverAlpha < targetHoverAlpha) {
            hoverAlpha += deltaTime * hoverSpeed;
            if (hoverAlpha > targetHoverAlpha)
                hoverAlpha = targetHoverAlpha;
        } else if (hoverAlpha > targetHoverAlpha) {
            hoverAlpha -= deltaTime * hoverSpeed;
            if (hoverAlpha < targetHoverAlpha)
                hoverAlpha = targetHoverAlpha;
        }
    }

    public void render(float panelX, float panelY, int mouseX, int mouseY, float alpha) {
        if (searchAlpha <= 0.01f)
            return;

        float combinedAlpha = alpha * searchAlpha;

        float x = panelX + relativeX;
        float y = panelY + relativeY;
        float buttonWidth = width;
        float buttonHeight = height;

        boolean hovered = isMouseOver(x, y, buttonWidth, buttonHeight, mouseX, mouseY);
        targetHoverAlpha = hovered ? 1f : 0f;

        Color accentColor = cc.silk.module.modules.client.NewClickGUIModule.getAccentColor();
        
        if (module.isEnabled()) {
            Color enabledBg = new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), (int) (255 * combinedAlpha));
            NanoVGRenderer.drawRect(x, y, buttonWidth, buttonHeight, enabledBg);
        } else if (hoverAlpha > 0.01f) {
            float expandedHeight = buttonHeight * hoverAlpha;
            Color hoverBg = new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), (int) (60 * combinedAlpha));
            NanoVGRenderer.drawRect(x, y, buttonWidth, expandedHeight, hoverBg);
        }

        if (matchesSearch && searchAlpha > 0.9f && !searchQuery.isEmpty()) {
            Color highlightBorder = new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), (int) (150 * combinedAlpha));
            NanoVGRenderer.drawRect(x, y, 2, buttonHeight, highlightBorder);
        }

        Color textColor;
        if (module.isEnabled()) {
            textColor = new Color(255, 255, 255, (int) (255 * combinedAlpha));
        } else if (hoverAlpha > 0.01f) {
            int baseAlpha = 130;
            int hoverBoost = (int) (110 * hoverAlpha);
            textColor = new Color(200, 200, 210, (int) ((baseAlpha + hoverBoost) * combinedAlpha));
        } else {
            textColor = new Color(130, 130, 140, (int) (255 * combinedAlpha));
        }

        float fontSize = 9f;
        float textY = y + (buttonHeight - fontSize) / 2f - 0.5f;
        float textX = x + 4;
        NanoVGRenderer.drawText(module.getName(), textX, textY, fontSize, textColor);
    }

    private String searchQuery = "";

    public boolean mouseClicked(float panelX, float panelY, double mouseX, double mouseY, int button) {
        if (searchAlpha <= 0.01f)
            return false;

        float x = panelX + relativeX;
        float y = panelY + relativeY;
        float buttonWidth = width;
        float buttonHeight = height;

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

    public float getSearchAlpha() {
        return searchAlpha;
    }

    private boolean isMouseOver(float x, float y, float width, float height, double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
