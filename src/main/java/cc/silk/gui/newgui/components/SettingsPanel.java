package cc.silk.gui.newgui.components;

import cc.silk.module.Module;
import cc.silk.module.setting.*;
import net.minecraft.client.gui.DrawContext;
import java.awt.*;
import java.util.List;

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

    public void render(DrawContext context, int mouseX, int mouseY, float alpha) {
        if (animationProgress <= 0f)
            return;

        float renderAlpha = alpha * animationProgress;
        int panelAlpha = (int) (255 * renderAlpha);

        context.fill((int) currentX, (int) y, (int) (currentX + width), (int) (y + height),
                new Color(PANEL_BG.getRed(), PANEL_BG.getGreen(), PANEL_BG.getBlue(), panelAlpha).getRGB());

        Color borderColor = new Color(BORDER_COLOR.getRed(), BORDER_COLOR.getGreen(), BORDER_COLOR.getBlue(),
                panelAlpha);
        context.fill((int) currentX, (int) y, (int) (currentX + width), (int) (y + 1), borderColor.getRGB());
        context.fill((int) currentX, (int) (y + height - 1), (int) (currentX + width), (int) (y + height),
                borderColor.getRGB());
        context.fill((int) currentX, (int) y, (int) (currentX + 1), (int) (y + height), borderColor.getRGB());
        context.fill((int) (currentX + width - 1), (int) y, (int) (currentX + width), (int) (y + height),
                borderColor.getRGB());

        context.fill((int) currentX, (int) y, (int) (currentX + width), (int) (y + HEADER_HEIGHT),
                new Color(HEADER_BG.getRed(), HEADER_BG.getGreen(), HEADER_BG.getBlue(), panelAlpha).getRGB());
        context.fill((int) currentX, (int) y, (int) (currentX + width), (int) (y + 2),
                new Color(ACCENT_COLOR.getRed(), ACCENT_COLOR.getGreen(), ACCENT_COLOR.getBlue(), panelAlpha).getRGB());

        Color separatorColor = new Color(ACCENT_COLOR.getRed(), ACCENT_COLOR.getGreen(), ACCENT_COLOR.getBlue(),
                (int) (80 * renderAlpha));
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

            renderSetting(context, setting, settingY, mouseX, mouseY, renderAlpha);
            if (setting instanceof NumberSetting) {
                settingY += NUMBER_SETTING_HEIGHT;
            } else {
                settingY += SETTING_HEIGHT;
            }
        }
    }

    private void renderSetting(DrawContext context, Setting setting, float settingY, int mouseX, int mouseY,
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

        context.fill((int) checkboxX, (int) checkboxY, (int) (checkboxX + checkboxSize),
                (int) (checkboxY + checkboxSize), checkboxBg.getRGB());

        context.fill((int) checkboxX, (int) checkboxY, (int) (checkboxX + checkboxSize), (int) (checkboxY + 1),
                checkboxBorder.getRGB());
        context.fill((int) checkboxX, (int) (checkboxY + checkboxSize - 1), (int) (checkboxX + checkboxSize),
                (int) (checkboxY + checkboxSize), checkboxBorder.getRGB());
        context.fill((int) checkboxX, (int) checkboxY, (int) (checkboxX + 1), (int) (checkboxY + checkboxSize),
                checkboxBorder.getRGB());
        context.fill((int) (checkboxX + checkboxSize - 1), (int) checkboxY, (int) (checkboxX + checkboxSize),
                (int) (checkboxY + checkboxSize), checkboxBorder.getRGB());

        if (setting.getValue()) {
            context.fill((int) (checkboxX + 2), (int) (checkboxY + 2), (int) (checkboxX + checkboxSize - 2),
                    (int) (checkboxY + checkboxSize - 2), checkColor.getRGB());
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
        Color sliderGlow = new Color(ACCENT_COLOR.getRed(), ACCENT_COLOR.getGreen(), ACCENT_COLOR.getBlue(),
                (int) (60 * alpha));

        context.fill((int) (currentX + PADDING), (int) sliderY, (int) (currentX + PADDING + sliderWidth),
                (int) (sliderY + sliderHeight), sliderBg.getRGB());

        double percentage = (setting.getValue() - setting.getMin()) / (setting.getMax() - setting.getMin());
        float fillWidth = (float) (sliderWidth * percentage);
        context.fill((int) (currentX + PADDING), (int) sliderY, (int) (currentX + PADDING + fillWidth),
                (int) (sliderY + sliderHeight), sliderFill.getRGB());

        if (fillWidth > 2) {
            context.fill((int) (currentX + PADDING + fillWidth - 2), (int) (sliderY - 1),
                    (int) (currentX + PADDING + fillWidth), (int) (sliderY + sliderHeight + 1), sliderGlow.getRGB());
        }

        float handleX = currentX + PADDING + fillWidth - 3;
        float handleWidth = 6;
        context.fill((int) handleX, (int) (sliderY - 3), (int) (handleX + handleWidth),
                (int) (sliderY + sliderHeight + 3), sliderHandle.getRGB());
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

        context.fill((int) (modeX - 3), (int) (modeY - 2), (int) (modeX + modeWidth + 3), (int) (modeY + 10),
                modeBg.getRGB());

        context.fill((int) (modeX - 3), (int) (modeY - 2), (int) (modeX + modeWidth + 3), (int) (modeY - 1),
                modeBorder.getRGB());
        context.fill((int) (modeX - 3), (int) (modeY + 9), (int) (modeX + modeWidth + 3), (int) (modeY + 10),
                modeBorder.getRGB());
        context.fill((int) (modeX - 3), (int) (modeY - 2), (int) (modeX - 2), (int) (modeY + 10), modeBorder.getRGB());
        context.fill((int) (modeX + modeWidth + 2), (int) (modeY - 2), (int) (modeX + modeWidth + 3),
                (int) (modeY + 10), modeBorder.getRGB());

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

        context.fill((int) previewX, (int) previewY, (int) (previewX + previewSize), (int) (previewY + previewSize),
                displayColor.getRGB());

        Color border = new Color(ACCENT_COLOR.getRed(), ACCENT_COLOR.getGreen(), ACCENT_COLOR.getBlue(),
                (int) (150 * alpha));
        context.fill((int) previewX, (int) previewY, (int) (previewX + previewSize), (int) (previewY + 1),
                border.getRGB());
        context.fill((int) previewX, (int) (previewY + previewSize - 1), (int) (previewX + previewSize),
                (int) (previewY + previewSize), border.getRGB());
        context.fill((int) previewX, (int) previewY, (int) (previewX + 1), (int) (previewY + previewSize),
                border.getRGB());
        context.fill((int) (previewX + previewSize - 1), (int) previewY, (int) (previewX + previewSize),
                (int) (previewY + previewSize), border.getRGB());
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
