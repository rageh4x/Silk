package cc.silk.module.modules.client;

import cc.silk.gui.ClickGui;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.ModeSetting;
import cc.silk.gui.theme.ThemeManager;
import org.lwjgl.glfw.GLFW;

public final class ClickGUIModule extends Module {
    private static final ModeSetting scale = new ModeSetting("Scale", "Small", "Small", "Medium", "Large");
    // Theme setting; names are populated from ThemeManager so adding new themes is just a register call
    public static final ModeSetting theme = new ModeSetting("Theme", ThemeManager.getDefaultName(), ThemeManager.getThemeNamesArray());
    int prevGuiScale;

    public ClickGUIModule() {
        super("Click Gui", "Toggles the Silk GUI", GLFW.GLFW_KEY_RIGHT_SHIFT, Category.CLIENT);
        addSettings(scale, theme);
    }

    @Override
    public void onEnable() {
        prevGuiScale = mc.options.getGuiScale().getValue();
        if (mc.currentScreen != null) return;
        switch (scale.getMode()) {
            case "Small" -> mc.options.getGuiScale().setValue(1);
            case "Medium" -> mc.options.getGuiScale().setValue(2);
            case "Large" -> mc.options.getGuiScale().setValue(3);
        }
        mc.setScreen(new ClickGui());
        super.onEnable();
    }

    @Override
    public void onDisable() {
        mc.options.getGuiScale().setValue(prevGuiScale);
        if (mc.currentScreen instanceof ClickGui) {
            mc.setScreen(null);
        }
        super.onDisable();
    }
}
