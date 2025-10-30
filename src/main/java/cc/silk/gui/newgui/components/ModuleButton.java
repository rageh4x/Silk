package cc.silk.gui.newgui.components;

import cc.silk.module.Module;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

public class ModuleButton {
    private final Module module;
    private final float relativeX, relativeY;
    private final int width, height;
    


    public ModuleButton(Module module, float relativeX, float relativeY, int width, int height) {
        this.module = module;
        this.relativeX = relativeX;
        this.relativeY = relativeY;
        this.width = width;
        this.height = height;
    }

    public void render(DrawContext context, float panelX, float panelY, int mouseX, int mouseY, float alpha) {
        float x = panelX + relativeX;
        float y = panelY + relativeY;
        float buttonWidth = width;
        float buttonHeight = height;
        
        boolean hovered = isMouseOver(x, y, buttonWidth, buttonHeight, mouseX, mouseY);
        
        if (module.isEnabled()) {
            Color accentColor = new Color(100, 120, 255, (int)(255 * alpha));
            context.fill((int)x, (int)y, (int)(x + 2), (int)(y + buttonHeight), accentColor.getRGB());
        }
        
        cc.silk.utils.render.font.fonts.FontRenderer font = cc.silk.SilkClient.INSTANCE.getFontManager().getSize(11, cc.silk.utils.render.font.FontManager.Type.Inter);
        Color textColor;
        if (module.isEnabled()) {
            textColor = new Color(255, 255, 255, (int)(255 * alpha));
        } else if (hovered) {
            textColor = new Color(200, 200, 200, (int)(255 * alpha));
        } else {
            textColor = new Color(140, 140, 145, (int)(255 * alpha));
        }
        float textY = y + (buttonHeight - 8) / 2f;
        float textX = x + (module.isEnabled() ? 8 : 6);
        font.drawString(context.getMatrices(), module.getName(), textX, textY, textColor);
    }
    
    public void render(DrawContext context, float panelX, float panelY, int mouseX, int mouseY) {
        render(context, panelX, panelY, mouseX, mouseY, 1f);
    }

    public boolean mouseClicked(float panelX, float panelY, double mouseX, double mouseY, int button, float panelHeaderY, int visibleHeight) {
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
            }
        }
        return false;
    }

    private boolean isMouseOver(float x, float y, float width, float height, double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
