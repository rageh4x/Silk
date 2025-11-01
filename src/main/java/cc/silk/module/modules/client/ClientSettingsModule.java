package cc.silk.module.modules.client;

import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.BooleanSetting;
import cc.silk.module.setting.ColorSetting;
import cc.silk.module.setting.ModeSetting;

import java.awt.Color;

public class ClientSettingsModule extends Module {

    public static final ColorSetting accentColor = new ColorSetting("Accent Color", new Color(124, 77, 255, 255));
    public static final ModeSetting fontStyle = new ModeSetting("Font", "Inter.ttf", "Inter.ttf", "jetbrainsmono.ttf", "poppins-medium.ttf", "Monaco.ttf");
    public static final BooleanSetting scrollableCategories = new BooleanSetting("Scrollable Categories", false);

    public ClientSettingsModule() {
        super("ClientSettings", "Customize GUI appearance", 0, Category.CLIENT);
        this.addSettings(accentColor, fontStyle, scrollableCategories);
        this.setEnabled(true);
    }

    public static Color getAccentColor() {
        return accentColor.getValue();
    }

    public static String getFontStyle() {
        return fontStyle.getMode();
    }

    public static boolean isScrollable() {
        return scrollableCategories.getValue();
    }
}
