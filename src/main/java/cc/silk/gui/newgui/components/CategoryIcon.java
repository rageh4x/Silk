package cc.silk.gui.newgui.components;

import cc.silk.module.Category;
import cc.silk.utils.render.TextureRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class CategoryIcon {
    private static final Map<Category, Identifier> ICON_MAP = new HashMap<>();
    private static final int ICON_SIZE = 12;

    static {
        ICON_MAP.put(Category.COMBAT, Identifier.of("silk", "imgs/sword.png"));
        ICON_MAP.put(Category.PLAYER, Identifier.of("silk", "imgs/user.png"));
        ICON_MAP.put(Category.MOVEMENT, Identifier.of("silk", "imgs/accessibility.png"));
        ICON_MAP.put(Category.MISC, Identifier.of("silk", "imgs/harddrive.png"));
        ICON_MAP.put(Category.RENDER, Identifier.of("silk", "imgs/eye.png"));
        ICON_MAP.put(Category.CLIENT, Identifier.of("silk", "imgs/computer.png"));
    }

    public static void drawIcon(DrawContext context, Category category, float x, float y, Color color) {
        Identifier icon = ICON_MAP.get(category);
        if (icon == null)
            return;

        int colorInt = (color.getAlpha() << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();

        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        TextureRenderer.drawCenteredQuad(context, icon, x + ICON_SIZE / 2f, y + ICON_SIZE / 2f, ICON_SIZE, ICON_SIZE, 0,
                colorInt, true);

        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    public static int getIconSize() {
        return ICON_SIZE;
    }
}
