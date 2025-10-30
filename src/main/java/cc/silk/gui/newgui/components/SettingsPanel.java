package cc.silk.gui.newgui.components;

import cc.silk.module.Module;
import cc.silk.module.setting.*;
import cc.silk.utils.render.NanoVGRenderer;
import net.minecraft.client.gui.DrawContext;
import java.awt.*;

public class SettingsPanel {
    private final Module module;
    private float targetX, currentX, y;
    private final int width = 160;
    private int height;
    private static final Color PANEL_BG = new Color(18, 18, 22, 255);
    private static final Color HEADER_BG = new Color(28, 28, 32, 255);
    private static final Color ACCENT_COLOR = new Color(88, 101, 242, 255);
    private static final Color BORDER_COLOR = new Color(40, 40, 46, 255);
    private static final int HEADER_HEIGHT = 22;
    private static final float CORNER_RADIUS = 4f;
    private static final int SETTING_HEIGHT = 20;
    private static final int NUMBER_SETTING_HEIGHT = 26;
    private static final int PADDING = 5;
    private boolean visible = false;
    private float animationProgress = 0f;
    private Setting draggingSlider = null;

    public SettingsPanel(Module module, float x, float y) {
        this.module = module;
        this.targetX = x;
        this.currentX = x - width;
        this.y = y;
        calculateHeight();
    }

    private void calculateHeight() {
        int totalHeight = HEADER_HEIGHT + PADDING * 2;
        for (Setting setting : module.getSettings()) {
            if (setting instanceof KeybindSetting)
                continue;
            if (setting instanceof NumberSetting) {
                totalHeight += NUMBER_SETTING_HEIGHT;
            } else {
                totalHeight += SETTING_HEIGHT;
            }
        }
        this.height = totalHeight;
    }

    public void show(float x, float y) {
        this.targetX = x;
        this.y = y;
        this.visible = true;
    }

    public void hide() {
        this.visible = false;
    }

    public void update(float delta) {
        if (visible) {
            animationProgress += delta * 8f;
            if (animationProgress > 1f)
                animationProgress = 1f;
        } else {
            animationProgress -= delta * 8f;
            if (animationProgress < 0f)
                animationProgress = 0f;
        }
        float easedProgress = 1f - (float) Math.pow(1f - animationProgress, 3);
        currentX = (targetX - width) + (width * easedProgress);
    }

    public void renderNanoVG(DrawContext context, int mouseX, int mouseY, float alpha) {
        if (animationProgress <= 0f)
            return;

        float renderAlpha = alpha * animationProgress;
        int panelAlpha = (int) (255 * renderAlpha);

        Color panelBg = new Color(PANEL_BG.getRed(), PANEL_BG.getGreen(), PANEL_BG.getBlue(), panelAlpha);
        Color borderColor = new Color(BORDER_COLOR.getRed(), BORDER_COLOR.getGreen(), BORDER_COLOR.getBlue(),
                panelAlpha);
        Color headerBg = new Color(HEADER_BG.getRed(), HEADER_BG.getGreen(), HEADER_BG.getBlue(), panelAlpha);

        NanoVGRenderer.drawRoundedRectWithContext(context, currentX, y, width, height, CORNER_RADIUS, panelBg);
        NanoVGRenderer.drawRoundedRectOutlineWithContext(context, currentX, y, width, height, CORNER_RADIUS, 1f,
                borderColor);
        NanoVGRenderer.drawRoundedRectWithContext(context, currentX, y, width, HEADER_HEIGHT, CORNER_RADIUS, headerBg);

        // Render NanoVG shapes for settings
        float settingY = y + HEADER_HEIGHT + PADDING;
        for (Setting setting : module.getSettings()) {
            if (setting instanceof KeybindSetting)
                continue;

            renderSettingNanoVG(context, setting, settingY, mouseX, mouseY, renderAlpha);
            if (setting instanceof NumberSetting) {
                settingY += NUMBER_SETTING_HEIGHT;
            } else {
                settingY += SETTING_HEIGHT;
            }
        }
    }

    public void renderMinecraft(DrawContext context, int mouseX, int mouseY, float alpha) {
        if (animationProgress <= 0f)
            return;

        float renderAlpha = alpha * animationProgress;
        int panelAlpha = (int) (255 * renderAlpha);

        Color headerBg = new Color(HEADER_BG.getRed(), HEADER_BG.getGreen(), HEADER_BG.getBlue(), panelAlpha);
        Color separatorColor = new Color(ACCENT_COLOR.getRed(), ACCENT_COLOR.getGreen(), ACCENT_COLOR.getBlue(),
                (int) (80 * renderAlpha));

        context.fill((int) currentX, (int) (y + HEADER_HEIGHT - CORNER_RADIUS), (int) (currentX + width),
                (int) (y + HEADER_HEIGHT), headerBg.getRGB());

        context.fill((int) currentX, (int) (y + HEADER_HEIGHT), (int) (currentX + width), (int) (y + HEADER_HEIGHT + 1),
                separatorColor.getRGB());

        cc.silk.utils.render.font.fonts.FontRenderer font = cc.silk.SilkClient.INSTANCE.getFontManager()
                .getSize(11, cc.silk.utils.render.font.FontManager.Type.Inter);
        Color textColor = new Color(240, 240, 245, panelAlpha);
        font.drawString(context.getMatrices(), module.getName(), currentX + PADDING, y + (HEADER_HEIGHT - 9) / 2f,
                textColor);

        float settingY = y + HEADER_HEIGHT + PADDING;
        for (Setting setting : module.getSettings()) {
            if (setting instanceof KeybindSetting)
                continue;

            renderSettingMinecraft(context, setting, settingY, mouseX, mouseY, renderAlpha);
            if (setting instanceof NumberSetting) {
                settingY += NUMBER_SETTING_HEIGHT;
            } else {
                settingY += SETTING_HEIGHT;
            }
        }
    }

    private void renderSettingNanoVG(DrawContext context, Setting setting, float settingY, int mouseX, int mouseY,
            float alpha) {
        cc.silk.utils.render.font.fonts.FontRenderer font = cc.silk.SilkClient.INSTANCE.getFontManager()
                .getSize(10, cc.silk.utils.render.font.FontManager.Type.Inter);
        int textAlpha = (int) (255 * alpha);
        Color textColor = new Color(200, 200, 200, textAlpha);

        if (setting instanceof BooleanSetting) {
            renderBooleanSetting(context, (BooleanSetting) setting, settingY, mouseX, mouseY, alpha, font, textColor);
        } else if (setting instanceof NumberSetting) {
            renderNumberSetting(context, (NumberSetting) setting, settingY, mouseX, mouseY, alpha, font, textColor);
        } else if (setting instanceof ModeSetting) {
            renderModeSetting(context, (ModeSetting) setting, settingY, mouseX, mouseY, alpha, font, textColor);
        } else if (setting instanceof ColorSetting) {
            renderColorSetting(context, (ColorSetting) setting, settingY, mouseX, mouseY, alpha, font, textColor);
        }
    }

    private void renderSettingMinecraft(DrawContext context, Setting setting, float settingY, int mouseX, int mouseY,
            float alpha) {
        // Text rendering is handled in the individual setting render methods
    }

    private void renderBooleanSetting(DrawContext context, BooleanSetting setting, float settingY,
            int mouseX, int mouseY, float alpha,
            cc.silk.utils.render.font.fonts.FontRenderer font, Color textColor) {
        font.drawString(context.getMatrices(), setting.getName(), currentX + PADDING, settingY + 5, textColor);

        int checkboxSize = 10;
        float checkboxX = currentX + width - PADDING - checkboxSize;
        float checkboxY = settingY + 5;

        int bgAlpha = (int) (255 * alpha);
        Color checkboxBg = new Color(30, 30, 35, bgAlpha);
        Color checkboxBorder = new Color(ACCENT_COLOR.getRed(), ACCENT_COLOR.getGreen(), ACCENT_COLOR.getBlue(),
                (int) (100 * alpha));
        Color checkColor = new Color(ACCENT_COLOR.getRed(), ACCENT_COLOR.getGreen(), ACCENT_COLOR.getBlue(), bgAlpha);

        // Rounded checkbox background
        NanoVGRenderer.drawRoundedRect(checkboxX, checkboxY, checkboxSize, checkboxSize, 2f, checkboxBg);
        NanoVGRenderer.drawRoundedRectOutline(checkboxX, checkboxY, checkboxSize, checkboxSize, 2f, 1f, checkboxBorder);

        if (setting.getValue()) {
            NanoVGRenderer.drawRoundedRect(checkboxX + 2, checkboxY + 2, checkboxSize - 4, checkboxSize - 4, 1f,
                    checkColor);
        }
    }

    private void renderNumberSetting(DrawContext context, NumberSetting setting, float settingY,
            int mouseX, int mouseY, float alpha,
            cc.silk.utils.render.font.fonts.FontRenderer font, Color textColor) {
        String displayText = setting.getName() + ": " + formatNumber(setting.getValue());
        font.drawString(context.getMatrices(), displayText, currentX + PADDING, settingY + 3, textColor);

        float sliderY = settingY + 15;
        float sliderWidth = width - PADDING * 2;
        float sliderHeight = 4;

        int bgAlpha = (int) (255 * alpha);
        Color sliderBg = new Color(30, 30, 35, bgAlpha);
        Color sliderFill = new Color(ACCENT_COLOR.getRed(), ACCENT_COLOR.getGreen(), ACCENT_COLOR.getBlue(), bgAlpha);
        Color sliderHandle = new Color(ACCENT_COLOR.getRed(), ACCENT_COLOR.getGreen(), ACCENT_COLOR.getBlue(),
                (int) (255 * alpha));

        // Rounded slider background
        NanoVGRenderer.drawRoundedRect(currentX + PADDING, sliderY, sliderWidth, sliderHeight, 2f, sliderBg);

        double percentage = (setting.getValue() - setting.getMin()) / (setting.getMax() - setting.getMin());
        float fillWidth = (float) (sliderWidth * percentage);

        if (fillWidth > 0) {
            NanoVGRenderer.drawRoundedRect(currentX + PADDING, sliderY, fillWidth, sliderHeight, 2f, sliderFill);
        }

        // Rounded slider handle
        float handleX = currentX + PADDING + fillWidth - 3;
        float handleWidth = 6;
        float handleHeight = 10;
        NanoVGRenderer.drawRoundedRect(handleX, sliderY - 3, handleWidth, handleHeight, 2f, sliderHandle);
    }

    private void renderModeSetting(DrawContext context, ModeSetting setting, float settingY,
            int mouseX, int mouseY, float alpha,
            cc.silk.utils.render.font.fonts.FontRenderer font, Color textColor) {
        font.drawString(context.getMatrices(), setting.getName(), currentX + PADDING, settingY + 2, textColor);

        String mode = setting.getMode();
        float modeWidth = font.getStringWidth(mode);

        int bgAlpha = (int) (255 * alpha);
        Color modeBg = new Color(30, 30, 35, bgAlpha);
        Color modeBorder = new Color(ACCENT_COLOR.getRed(), ACCENT_COLOR.getGreen(), ACCENT_COLOR.getBlue(),
                (int) (100 * alpha));
        Color modeText = new Color(ACCENT_COLOR.getRed(), ACCENT_COLOR.getGreen(), ACCENT_COLOR.getBlue(), bgAlpha);

        float modeX = currentX + width - PADDING - modeWidth - 6;
        float modeY = settingY + 10;
        float modeBoxWidth = modeWidth + 6;
        float modeBoxHeight = 12;

        // Rounded mode selector
        NanoVGRenderer.drawRoundedRect(modeX - 3, modeY - 2, modeBoxWidth, modeBoxHeight, 3f, modeBg);
        NanoVGRenderer.drawRoundedRectOutline(modeX - 3, modeY - 2, modeBoxWidth, modeBoxHeight, 3f, 1f, modeBorder);

        font.drawString(context.getMatrices(), mode, modeX, modeY, modeText);
    }

    private void renderColorSetting(DrawContext context, ColorSetting setting, float settingY,
            int mouseX, int mouseY, float alpha,
            cc.silk.utils.render.font.fonts.FontRenderer font, Color textColor) {
        font.drawString(context.getMatrices(), setting.getName(), currentX + PADDING, settingY + 5, textColor);

        int previewSize = 12;
        float previewX = currentX + width - PADDING - previewSize;
        float previewY = settingY + 4;

        Color color = setting.getValue();
        int colorAlpha = (int) (color.getAlpha() * alpha);
        Color displayColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), colorAlpha);

        Color border = new Color(ACCENT_COLOR.getRed(), ACCENT_COLOR.getGreen(), ACCENT_COLOR.getBlue(),
                (int) (150 * alpha));

        // Rounded color preview
        NanoVGRenderer.drawRoundedRect(previewX, previewY, previewSize, previewSize, 3f, displayColor);
        NanoVGRenderer.drawRoundedRectOutline(previewX, previewY, previewSize, previewSize, 3f, 1f, border);
    }

    private String formatNumber(double value) {
        if (value == (long) value) {
            return String.format("%d", (long) value);
        } else {
            return String.format("%.2f", value);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (animationProgress <= 0f || button != 0)
            return false;
        if (!isMouseOver(mouseX, mouseY))
            return false;

        float settingY = y + HEADER_HEIGHT + PADDING;
        for (Setting setting : module.getSettings()) {
            if (setting instanceof KeybindSetting)
                continue;

            int currentHeight = (setting instanceof NumberSetting) ? NUMBER_SETTING_HEIGHT : SETTING_HEIGHT;

            if (mouseY >= settingY && mouseY <= settingY + currentHeight) {
                if (setting instanceof BooleanSetting) {
                    ((BooleanSetting) setting).toggle();
                    return true;
                } else if (setting instanceof NumberSetting) {
                    draggingSlider = setting;
                    updateSliderValue((NumberSetting) setting, mouseX);
                    return true;
                } else if (setting instanceof ModeSetting) {
                    ((ModeSetting) setting).cycle();
                    return true;
                }
            }
            settingY += currentHeight;
        }

        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (draggingSlider instanceof NumberSetting && button == 0) {
            updateSliderValue((NumberSetting) draggingSlider, mouseX);
            return true;
        }
        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingSlider = null;
        }
    }

    private void updateSliderValue(NumberSetting setting, double mouseX) {
        float sliderWidth = width - PADDING * 2;
        float sliderX = currentX + PADDING;

        double percentage = Math.max(0, Math.min(1, (mouseX - sliderX) / sliderWidth));
        double newValue = setting.getMin() + (setting.getMax() - setting.getMin()) * percentage;
        setting.setValue(newValue);
    }

    private boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= currentX && mouseX <= currentX + width &&
                mouseY >= y && mouseY <= y + height;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isAnimating() {
        return animationProgress > 0f;
    }

    public float getAnimationProgress() {
        return animationProgress;
    }

    public float getCurrentX() {
        return currentX;
    }

    public float getY() {
        return y;
    }
}
