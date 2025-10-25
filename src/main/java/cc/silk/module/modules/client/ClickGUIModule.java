package cc.silk.module.modules.client;

import cc.silk.gui.ClickGui;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.ModeSetting;
import org.lwjgl.glfw.GLFW;

public final class ClickGUIModule extends Module {
    private static final ModeSetting scale = new ModeSetting("Scale", "Small", "Small", "Medium", "Large");
    int prevGuiScale;

    public ClickGUIModule() {
        super("Click Gui", "Toggles the Silk GUI", GLFW.GLFW_KEY_RIGHT_SHIFT, Category.CLIENT);
        addSettings(scale);
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
