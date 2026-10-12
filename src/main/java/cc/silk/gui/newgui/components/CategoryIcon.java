package cc.silk.gui.newgui.components;

import cc.silk.module.Category;
import net.minecraft.client.gui.DrawContext;
import java.awt.Color;

public class CategoryIcon {
    private static final int ICON_SIZE = 12;

    public static void init() {
        // NanoVG ki zaroorat nahi hai
    }

    public static void drawIcon(DrawContext context, Category category, int x, int y, Color color) {
        context.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, color.getRGB());
    }

    public static int getIconSize() {
        return ICON_SIZE;
    }
}
