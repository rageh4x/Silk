package cc.silk.gui.newgui.components;

import cc.silk.module.Module;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;

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
    private String searchQuery = "";

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

    public void render(DrawContext context, float panelX, float panelY, int mouseX, int mouseY, float alpha) {
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

        int ix = (int) x;
        int iy = (int) y;
        int iwidth = (int) buttonWidth;
        int iheight = (int) buttonHeight;

        if (module.isEnabled()) {
            Color enabledBg = new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(),
                    (int) (255 * combinedAlpha));
            context.fill(ix, iy, ix + iwidth, iy + iheight, enabledBg.getRGB());
        } else if (hoverAlpha > 0.01f) {
            Color hoverBg = new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(),
                    (int) (60 * combinedAlpha));
            context.fill(ix, iy, ix + iwidth, iy + iheight, hoverBg.getRGB());
        }

        int textColor;
        if (module.isEnabled()) {
            textColor = new Color(255, 255, 255, (int) (255 * combinedAlpha)).getRGB();
        } else {
            textColor = new Color(200, 200, 210, (int) (255 * combinedAlpha)).getRGB();
        }

        int textY = iy + (iheight - 8) / 2;
        int textX = ix + 4;
        context.drawText(MinecraftClient.getInstance().textRenderer, module.getName(), textX, textY, textColor, false);
    }

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
            } else return button == 1;
        }
        return false;
    }

    public Module getModule() {
        return module;
    }

    public float getSearchAlpha() {
        return searchAlpha;
    }

    private boolean isMouseOver(float x, float y, float width, float height, double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
