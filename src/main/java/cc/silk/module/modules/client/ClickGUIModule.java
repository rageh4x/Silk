package cc.silk.module.modules.client;

import cc.silk.gui.newgui.NewClickGUI;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.ModeSetting;
import cc.silk.gui.theme.ThemeManager;
import org.lwjgl.glfw.GLFW;

public final class ClickGUIModule extends Module {
    public static final ModeSetting theme = new ModeSetting("Theme", ThemeManager.getDefaultName(),
            ThemeManager.getThemeNamesArray());

    public ClickGUIModule() {
        super("Click Gui", "Toggles the Silk GUI", GLFW.GLFW_KEY_RIGHT_SHIFT, Category.CLIENT);
        addSettings(theme);
    }

    @Override
    public void onEnable() {
        try {
            if (mc.currentScreen == null) {
                mc.setScreen(new NewClickGUI());
            }
        } catch (Exception e) {
            // Android par agar NanoVG load na ho toh crash hone se bachane ke liye try-catch
            System.err.println("ClickGUI open nahi ho saka: " + e.getMessage());
        }
        setEnabled(false);
    }
}
