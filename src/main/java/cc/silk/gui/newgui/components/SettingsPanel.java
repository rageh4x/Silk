package cc.silk.gui.newgui.components;

import cc.silk.module.Module;
import cc.silk.module.setting.*;
import cc.silk.utils.render.GuiGlowHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;

import java.awt.*;

public class SettingsPanel {
    private static final Color PANEL_BG = new Color(18, 18, 22, 255);
    private static final Color HEADER_BG = new Color(28, 28, 32, 255);
    private static final Color BORDER_COLOR = new Color(40, 40, 46, 255);
    private static final int HEADER_HEIGHT = 22;
    private static final float CORNER_RADIUS = 4f;
    private static final int SETTING_HEIGHT = 16;
    private static final int NUMBER_SETTING_HEIGHT = 22;
    private static final int PADDING = 5;
    private final Module module;
    private final int width = 160;
    private float targetX, currentX, y;
    private int height;
    private ModeSetting expandedDropdown = null;
    private ColorPicker colorPicker = null;
    private boolean visible = false;
    private float animationProgress = 0f;
    private Setting draggingSlider = null;
    private KeybindSetting listeningKeybind = null;
    private NumberSetting typingSlider = null;
    private String sliderInputText = "";
    private RangeSetting draggingRangeSlider = null;
    private boolean draggingMinHandle = false;
    private RangeSetting typingRangeSlider = null;
    private boolean typingRangeMin = false;
    private String rangeInputText = "";
    private int screenWidth = 0;
    private int screenHeight = 0;

    public SettingsPanel(Module module, float x, float y) {
        this.module = module;
        this.targetX = x;
        this.currentX = x - width;
        this.y = y;
        calculateHeight();
    }

    private static Color getAccentColor() {
        return cc.silk.module.modules.client.NewClickGUIModule.getAccentColor();
    }

    public boolean isDraggingSlider() {
        return draggingSlider != null;
    }

    private void calculateHeight() {
        int totalHeight = HEADER_HEIGHT + PADDING * 2;
        for (Setting setting : module.getSettings()) {
            if (setting instanceof NumberSetting || setting instanceof RangeSetting) {
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
        clampToScreen();
    }

    public void setScreenDimensions(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        clampToScreen();
    }

    private void clampToScreen() {
        if (screenWidth > 0 && screenHeight > 0) {
            if (targetX + width > screenWidth) {
                targetX = screenWidth - width - 10;
            }
            if (targetX < 10) {
                targetX = 10;
            }

            if (y + height > screenHeight) {
                y = screenHeight - height - 10;
            }
            if (y < 10) {
                y = 10;
            }
        }
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

        if (colorPicker != null) {
            colorPicker.update(delta);
        }
    }

    public void render(DrawContext context, int mouseX, int mouseY, float alpha) {
        if (animationProgress <= 0f)
            return;

        float transparency = cc.silk.module.modules.client.ClientSettingsModule.getGuiTransparency();
        float renderAlpha = alpha * animationProgress * (1f - transparency);
        int panelAlpha = (int) (255 * renderAlpha);

        Color panelBg = new Color(PANEL_BG.getRed(), PANEL_BG.getGreen(), PANEL_BG.getBlue(), panelAlpha);
        Color headerBg = new Color(HEADER_BG.getRed(), HEADER_BG.getGreen(), HEADER_BG.getBlue(), panelAlpha);
        Color accentColor = getAccentColor();

        int ix = (int) currentX;
        int iy = (int) y;
        int iwidth = width;
        int iheight = height;

        context.fill(ix, iy, ix + iwidth, iy + iheight, panelBg.getRGB());
        context.fill(ix, iy, ix + iwidth, iy + HEADER_HEIGHT, headerBg.getRGB());

        int textColor = new Color(240, 240, 245, panelAlpha).getRGB();
        context.drawText(MinecraftClient.getInstance().textRenderer, module.getName(), ix + PADDING, iy + (HEADER_HEIGHT - 8) / 2, textColor, false);

        float settingY = y + HEADER_HEIGHT + PADDING;
        for (Setting setting : module.getSettings()) {
            renderSetting(context, setting, settingY, mouseX, mouseY, renderAlpha, false);
            if (setting instanceof NumberSetting || setting instanceof RangeSetting) {
                settingY += NUMBER_SETTING_HEIGHT;
            } else {
                settingY += SETTING_HEIGHT;
            }
        }

        if (expandedDropdown != null) {
            settingY = y + HEADER_HEIGHT + PADDING;
            for (Setting setting : module.getSettings()) {
                if (setting == expandedDropdown) {
                    renderSetting(context, setting, settingY, mouseX, mouseY, renderAlpha, true);
                    break;
                }

                if (setting instanceof NumberSetting || setting instanceof RangeSetting) {
                    settingY += NUMBER_SETTING_HEIGHT;
                } else {
                    settingY += SETTING_HEIGHT;
                }
            }
        }

        if (colorPicker != null && colorPicker.isAnimating()) {
            colorPicker.render(context, mouseX, mouseY, renderAlpha);
        }
    }

    private void renderSetting(DrawContext context, Setting setting, float settingY, int mouseX, int mouseY, float alpha,
            boolean renderDropdownOnly) {
        if (setting instanceof BooleanSetting) {
            renderBooleanSetting(context, (BooleanSetting) setting, settingY, mouseX, mouseY, alpha);
        } else if (setting instanceof RangeSetting) {
            renderRangeSetting(context, (RangeSetting) setting, settingY, mouseX, mouseY, alpha);
        } else if (setting instanceof NumberSetting) {
            renderNumberSetting(context, (NumberSetting) setting, settingY, mouseX, mouseY, alpha);
        } else if (setting instanceof ModeSetting) {
            renderModeSetting(context, (ModeSetting) setting, settingY, mouseX, mouseY, alpha, renderDropdownOnly);
        } else if (setting instanceof ColorSetting) {
            renderColorSetting(context, (ColorSetting) setting, settingY, mouseX, mouseY, alpha);
        } else if (setting instanceof KeybindSetting) {
            renderKeybindSetting(context, (KeybindSetting) setting, settingY, mouseX, mouseY, alpha);
        }
    }

    private void renderRangeSetting(DrawContext context, RangeSetting setting, float settingY, int mouseX, int mouseY, float alpha) {
        float sliderY = settingY + 13;
        float sliderWidth = width - PADDING * 2;
        float sliderHeight = cc.silk.module.modules.client.ClientSettingsModule.getSliderHeight();
        int bgAlpha = (int) (255 * alpha);
        Color accentColor = getAccentColor();
        Color sliderBg = new Color(30, 30, 35, bgAlpha);
        Color sliderFill = new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), bgAlpha);

        context.fill((int)(currentX + PADDING), (int)sliderY, (int)(currentX + PADDING + sliderWidth), (int)(sliderY + sliderHeight), sliderBg.getRGB());

        double range = setting.getMax() - setting.getMin();
        double minPercentage = range > 0 ? (setting.getMinValue() - setting.getMin()) / range : 0;
        double maxPercentage = range > 0 ? (setting.getMaxValue() - setting.getMin()) / range : 1;
        float minX = (float) (sliderWidth * minPercentage);
        float maxX = (float) (sliderWidth * maxPercentage);
        float fillWidth = maxX - minX;

        if (fillWidth > 0) {
            context.fill((int)(currentX + PADDING + minX), (int)sliderY, (int)(currentX + PADDING + minX + fillWidth), (int)(sliderY + sliderHeight), sliderFill.getRGB());
        }

        String displayText = setting.getName();
        String valueText = (typingRangeSlider == setting) ? rangeInputText + "_" : formatNumber(setting.getMinValue()) + " - " + formatNumber(setting.getMaxValue());
        int textColor = new Color(200, 200, 200, bgAlpha).getRGB();

        context.drawText(MinecraftClient.getInstance().textRenderer, displayText, (int)(currentX + PADDING), (int)(settingY + 2), textColor, false);
        context.drawText(MinecraftClient.getInstance().textRenderer, valueText, (int)(currentX + width - PADDING - MinecraftClient.getInstance().textRenderer.getWidth(valueText)), (int)(settingY + 2), textColor, false);
    }

    private void renderBooleanSetting(DrawContext context, BooleanSetting setting, float settingY, int mouseX, int mouseY, float alpha) {
        float toggleWidth = cc.silk.module.modules.client.ClientSettingsModule.getToggleWidth();
        float toggleHeight = cc.silk.module.modules.client.ClientSettingsModule.getToggleHeight();
        float toggleX = currentX + width - PADDING - toggleWidth;
        float toggleY = settingY + 3;

        int bgAlpha = (int) (255 * alpha);
        boolean isOn = setting.getValue();

        Color accentColor = getAccentColor();
        Color toggleBg = isOn
                ? new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), bgAlpha)
                : new Color(40, 40, 46, bgAlpha);

        context.fill((int)toggleX, (int)toggleY, (int)(toggleX + toggleWidth), (int)(toggleY + toggleHeight), toggleBg.getRGB());

        int textColor = new Color(200, 200, 200, bgAlpha).getRGB();
        context.drawText(MinecraftClient.getInstance().textRenderer, setting.getName(), (int)(currentX + PADDING), (int)(settingY + 3), textColor, false);
    }

    private void renderNumberSetting(DrawContext context, NumberSetting setting, float settingY, int mouseX, int mouseY, float alpha) {
        float sliderY = settingY + 13;
        float sliderWidth = width - PADDING * 2;
        float sliderHeight = cc.silk.module.modules.client.ClientSettingsModule.getSliderHeight();
        int bgAlpha = (int) (255 * alpha);
        Color accentColor = getAccentColor();
        Color sliderBg = new Color(30, 30, 35, bgAlpha);
        Color sliderFill = new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), bgAlpha);

        context.fill((int)(currentX + PADDING), (int)sliderY, (int)(currentX + PADDING + sliderWidth), (int)(sliderY + sliderHeight), sliderBg.getRGB());
        
        double percentage = (setting.getValue() - setting.getMin()) / (setting.getMax() - setting.getMin());
        float fillWidth = (float) (sliderWidth * percentage);
        if (fillWidth > 0) {
            context.fill((int)(currentX + PADDING), (int)sliderY, (int)(currentX + PADDING + fillWidth), (int)(sliderY + sliderHeight), sliderFill.getRGB());
        }

        String displayText = setting.getName();
        String valueText = (typingSlider == setting) ? sliderInputText + "_" : formatNumber(setting.getValue());
        int textColor = new Color(200, 200, 200, bgAlpha).getRGB();

        context.drawText(MinecraftClient.getInstance().textRenderer, displayText, (int)(currentX + PADDING), (int)(settingY + 2), textColor, false);
        context.drawText(MinecraftClient.getInstance().textRenderer, valueText, (int)(currentX + width - PADDING - MinecraftClient.getInstance().textRenderer.getWidth(valueText)), (int)(settingY + 2), textColor, false);
    }

    private void renderModeSetting(DrawContext context, ModeSetting setting, float settingY, int mouseX, int mouseY, float alpha, boolean renderDropdownOnly) {
        String mode = setting.getMode();
        int bgAlpha = (int) (255 * alpha);
        Color accentColor = getAccentColor();
        Color dropdownBg = new Color(30, 30, 35, bgAlpha);

        float dropdownWidth = 60;
        float dropdownHeight = 14;
        float dropdownX = currentX + width - PADDING - dropdownWidth;
        float dropdownY = settingY + 1;

        if (!renderDropdownOnly) {
            context.fill((int)dropdownX, (int)dropdownY, (int)(dropdownX + dropdownWidth), (int)(dropdownY + dropdownHeight), dropdownBg.getRGB());
            int textColor = new Color(200, 200, 200, bgAlpha).getRGB();
            context.drawText(MinecraftClient.getInstance().textRenderer, mode, (int)(dropdownX + 4), (int)(dropdownY + 2), textColor, false);
            context.drawText(MinecraftClient.getInstance().textRenderer, setting.getName(), (int)(currentX + PADDING), (int)(settingY + 3), textColor, false);
        }

        if (expandedDropdown == setting && renderDropdownOnly) {
            float menuY = dropdownY + dropdownHeight + 2;
            int optionCount = setting.getModes().size();
            float menuHeight = optionCount * 14;
            Color menuBg = new Color(25, 25, 30, bgAlpha);

            context.fill((int)dropdownX, (int)menuY, (int)(dropdownX + dropdownWidth), (int)(menuY + menuHeight), menuBg.getRGB());

            float optionY = menuY;
            int textColor = new Color(200, 200, 200, bgAlpha).getRGB();
            for (String option : setting.getModes()) {
                context.drawText(MinecraftClient.getInstance().textRenderer, option, (int)(dropdownX + 4), (int)(optionY + 2), textColor, false);
                optionY += 14;
            }
        }
    }

    private void renderColorSetting(DrawContext context, ColorSetting setting, float settingY, int mouseX, int mouseY, float alpha) {
        int previewSize = 12;
        float previewX = currentX + width - PADDING - previewSize;
        float previewY = settingY + 2;

        Color color = setting.getValue();
        int colorAlpha = (int) (color.getAlpha() * alpha);
        Color displayColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), colorAlpha);

        context.fill((int)previewX, (int)previewY, (int)(previewX + previewSize), (int)(previewY + previewSize), displayColor.getRGB());
        int textColor = new Color(200, 200, 200, (int) (255 * alpha)).getRGB();
        context.drawText(MinecraftClient.getInstance().textRenderer, setting.getName(), (int)(currentX + PADDING), (int)(settingY + 3), textColor, false);
    }

    private void renderKeybindSetting(DrawContext context, KeybindSetting setting, float settingY, int mouseX, int mouseY, float alpha) {
        int bgAlpha = (int) (255 * alpha);
        boolean isListening = listeningKeybind == setting;
        String keyText = isListening ? "..." : getKeyName(setting.getKeyCode());

        float modeButtonWidth = 30;
        float modeButtonHeight = 14;
        float modeButtonX = currentX + width - PADDING - modeButtonWidth;
        float modeButtonY = settingY + 1;

        float buttonWidth = 40;
        float buttonHeight = 14;
        float buttonX = modeButtonX - buttonWidth - 3;
        float buttonY = settingY + 1;

        Color accentColor = getAccentColor();
        Color buttonBg = new Color(30, 30, 35, bgAlpha);

        context.fill((int)buttonX, (int)buttonY, (int)(buttonX + buttonWidth), (int)(buttonY + buttonHeight), buttonBg.getRGB());
        context.fill((int)modeButtonX, (int)modeButtonY, (int)(modeButtonX + modeButtonWidth), (int)(modeButtonY + modeButtonHeight), buttonBg.getRGB());

        int textColor = new Color(200, 200, 200, bgAlpha).getRGB();
        context.drawText(MinecraftClient.getInstance().textRenderer, keyText, (int)(buttonX + 4), (int)(buttonY + 2), textColor, false);
        context.drawText(MinecraftClient.getInstance().textRenderer, setting.isHoldMode() ? "Hold" : "Toggle", (int)(modeButtonX + 4), (int)(modeButtonY + 2), textColor, false);
        context.drawText(MinecraftClient.getInstance().textRenderer, setting.getName(), (int)(currentX + PADDING), (int)(settingY + 3), textColor, false);
    }

    private String formatNumber(double value) {
        if (value == (long) value) {
            return String.format("%d", (long) value);
        } else {
            return String.format("%.2f", value);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (animationProgress <= 0f)
            return false;

        if (mouseClickedForKeybind(mouseX, mouseY, button)) {
            return true;
        }

        if (button == 1) {
            float settingY = y + HEADER_HEIGHT + PADDING;
            for (Setting setting : module.getSettings()) {
                int currentHeight = (setting instanceof NumberSetting || setting instanceof RangeSetting)
                        ? NUMBER_SETTING_HEIGHT
                        : SETTING_HEIGHT;

                if (setting instanceof NumberSetting && mouseY >= settingY && mouseY <= settingY + currentHeight) {
                    if (mouseX >= currentX && mouseX <= currentX + width) {
                        typingSlider = (NumberSetting) setting;
                        sliderInputText = formatNumber(((NumberSetting) setting).getValue());
                        return true;
                    }
                } else if (setting instanceof RangeSetting && mouseY >= settingY
                        && mouseY <= settingY + currentHeight) {
                    if (mouseX >= currentX && mouseX <= currentX + width) {
                        typingRangeSlider = (RangeSetting) setting;
                        float sliderY = settingY + 13;
                        float sliderWidth = width - PADDING * 2;
                        double range = ((RangeSetting) setting).getMax() - ((RangeSetting) setting).getMin();
                        double minPercentage = range > 0 ? (((RangeSetting) setting).getMinValue()
                                - ((RangeSetting) setting).getMin()) / range : 0;
                        double maxPercentage = range > 0 ? (((RangeSetting) setting).getMaxValue()
                                - ((RangeSetting) setting).getMin()) / range : 1;
                        float minX = (float) (sliderWidth * minPercentage);
                        float maxX = (float) (sliderWidth * maxPercentage);
                        float midX = (minX + maxX) / 2f;
                        float relativeX = (float) (mouseX - (currentX + PADDING));

                        typingRangeMin = relativeX < midX;
                        rangeInputText = formatNumber(typingRangeMin ? ((RangeSetting) setting).getMinValue()
                                : ((RangeSetting) setting).getMaxValue());
                        return true;
                    }
                }

                settingY += currentHeight;
            }
            return false;
        }

        if (button != 0)
            return false;

        if (colorPicker != null && colorPicker.isAnimating()) {
            boolean clickedInside = colorPicker.mouseClicked(mouseX, mouseY, button);
            if (!clickedInside) {
                colorPicker.hide();
            }
            return true;
        }

        if (expandedDropdown != null) {
            float settingY = y + HEADER_HEIGHT + PADDING;
            for (Setting setting : module.getSettings()) {
                if (setting == expandedDropdown) {
                    float dropdownWidth = 60;
                    float dropdownX = currentX + width - PADDING - dropdownWidth;
                    float dropdownY = settingY + 1;
                    float dropdownHeight = 14;
                    float menuY = dropdownY + dropdownHeight + 2;

                    int optionIndex = 0;
                    for (String option : expandedDropdown.getModes()) {
                        float optionY = menuY + (optionIndex * 14);
                        if (mouseX >= dropdownX && mouseX <= dropdownX + dropdownWidth &&
                                mouseY >= optionY && mouseY <= optionY + 14) {
                            expandedDropdown.setMode(option);
                            expandedDropdown = null;
                            return true;
                        }
                        optionIndex++;
                    }

                    expandedDropdown = null;
                    return true;
                }

                int currentHeight = (setting instanceof NumberSetting || setting instanceof RangeSetting)
                        ? NUMBER_SETTING_HEIGHT
                        : SETTING_HEIGHT;
                settingY += currentHeight;
            }
        }

        if (!isMouseOver(mouseX, mouseY))
            return false;

        float settingY = y + HEADER_HEIGHT + PADDING;
        for (Setting setting : module.getSettings()) {
            int currentHeight = (setting instanceof NumberSetting || setting instanceof RangeSetting)
                    ? NUMBER_SETTING_HEIGHT
                    : SETTING_HEIGHT;

            if (mouseY >= settingY && mouseY <= settingY + currentHeight) {
                if (setting instanceof BooleanSetting) {
                    ((BooleanSetting) setting).toggle();
                    return true;
                } else if (setting instanceof RangeSetting) {
                    RangeSetting rangeSetting = (RangeSetting) setting;
                    float sliderY = settingY + 13;
                    float sliderWidth = width - PADDING * 2;
                    double range = rangeSetting.getMax() - rangeSetting.getMin();
                    double minPercentage = range > 0 ? (rangeSetting.getMinValue() - rangeSetting.getMin()) / range : 0;
                    double maxPercentage = range > 0 ? (rangeSetting.getMaxValue() - rangeSetting.getMin()) / range : 1;
                    float minHandleX = currentX + PADDING + (float) (sliderWidth * minPercentage);
                    float maxHandleX = currentX + PADDING + (float) (sliderWidth * maxPercentage);

                    float distToMin = Math.abs((float) mouseX - minHandleX);
                    float distToMax = Math.abs((float) mouseX - maxHandleX);

                    draggingRangeSlider = rangeSetting;
                    draggingMinHandle = distToMin < distToMax;
                    updateRangeSliderValue(rangeSetting, mouseX, draggingMinHandle);
                    return true;
                } else if (setting instanceof NumberSetting) {
                    draggingSlider = setting;
                    updateSliderValue((NumberSetting) setting, mouseX);
                    return true;
                } else if (setting instanceof KeybindSetting keybindSetting) {
                    float modeButtonWidth = 30;
                    float modeButtonX = currentX + width - PADDING - modeButtonWidth;
                    float modeButtonY = settingY + 1;
                    float modeButtonHeight = 14;

                    if (mouseX >= modeButtonX && mouseX <= modeButtonX + modeButtonWidth &&
                            mouseY >= modeButtonY && mouseY <= modeButtonY + modeButtonHeight) {
                        keybindSetting.toggleHoldMode();
                        return true;
                    }

                    listeningKeybind = (listeningKeybind == setting) ? null : keybindSetting;
                    return true;
                } else if (setting instanceof ModeSetting modeSetting) {
                    float dropdownWidth = 60;
                    float dropdownX = currentX + width - PADDING - dropdownWidth;
                    float dropdownY = settingY + 1;
                    float dropdownHeight = 14;

                    if (mouseX >= dropdownX && mouseX <= dropdownX + dropdownWidth &&
                            mouseY >= dropdownY && mouseY <= dropdownY + dropdownHeight) {
                        expandedDropdown = (expandedDropdown == modeSetting) ? null : modeSetting;
                        return true;
                    }

                    if (expandedDropdown == modeSetting) {
                        float menuY = dropdownY + dropdownHeight + 2;
                        int optionIndex = 0;
                        for (String option : modeSetting.getModes()) {
                            float optionY = menuY + (optionIndex * 14);
                            if (mouseX >= dropdownX && mouseX <= dropdownX + dropdownWidth &&
                                    mouseY >= optionY && mouseY <= optionY + 14) {
                                modeSetting.setMode(option);
                                expandedDropdown = null;
                                return true;
                            }
                            optionIndex++;
                        }
                    }
                } else if (setting instanceof ColorSetting) {
                    int previewSize = 12;
                    float previewX = currentX + width - PADDING - previewSize;
                    float previewY = settingY + 2;

                    if (mouseX >= previewX && mouseX <= previewX + previewSize &&
                            mouseY >= previewY && mouseY <= previewY + previewSize) {
                        float pickerX = currentX + width + 5;
                        float pickerY = settingY;

                        if (colorPicker != null && colorPicker.isVisible()) {
                            colorPicker.hide();
                            colorPicker = null;
                        } else {
                            colorPicker = new ColorPicker((ColorSetting) setting, pickerX, pickerY);
                            colorPicker.show(pickerX, pickerY, screenWidth, screenHeight);
                        }
                        return true;
                    }
                }
            }
            settingY += currentHeight;
        }

        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (colorPicker != null && colorPicker.isAnimating()) {
            return colorPicker.mouseDragged(mouseX, mouseY, button);
        }

        if (draggingRangeSlider != null && button == 0) {
            updateRangeSliderValue(draggingRangeSlider, mouseX, draggingMinHandle);
            return true;
        }

        if (draggingSlider instanceof NumberSetting && button == 0) {
            updateSliderValue((NumberSetting) draggingSlider, mouseX);
            return true;
        }
        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingSlider = null;
            draggingRangeSlider = null;
        }

        if (colorPicker != null) {
            colorPicker.mouseReleased(mouseX, mouseY, button);
        }
    }

    private void updateSliderValue(NumberSetting setting, double mouseX) {
        float sliderWidth = width - PADDING * 2;
        float sliderX = currentX + PADDING;

        double percentage = Math.max(0, Math.min(1, (mouseX - sliderX) / sliderWidth));
        double newValue = setting.getMin() + (setting.getMax() - setting.getMin()) * percentage;
        setting.setValue(newValue);
    }

    private void updateRangeSliderValue(RangeSetting setting, double mouseX, boolean updateMin) {
        float sliderWidth = width - PADDING * 2;
        float sliderX = currentX + PADDING;

        double percentage = Math.max(0, Math.min(1, (mouseX - sliderX) / sliderWidth));
        double range = setting.getMax() - setting.getMin();
        double newValue = range > 0 ? setting.getMin() + range * percentage : setting.getMin();

        if (updateMin) {
            setting.setMinValue(newValue);
        } else {
            setting.setMaxValue(newValue);
        }
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

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (typingRangeSlider != null) {
            if (keyCode == 256) {
                typingRangeSlider = null;
                rangeInputText = "";
                return true;
            } else if (keyCode == 257 || keyCode == 335) {
                try {
                    double value = Double.parseDouble(rangeInputText);
                    value = Math.max(typingRangeSlider.getMin(), Math.min(typingRangeSlider.getMax(), value));
                    if (typingRangeMin) {
                        typingRangeSlider.setMinValue(value);
                    } else {
                        typingRangeSlider.setMaxValue(value);
                    }
                } catch (NumberFormatException ignored) {
                }
                typingRangeSlider = null;
                rangeInputText = "";
                return true;
            } else if (keyCode == 259) {
                if (!rangeInputText.isEmpty()) {
                    rangeInputText = rangeInputText.substring(0, rangeInputText.length() - 1);
                }
                return true;
            }
            return false;
        }

        if (typingSlider != null) {
            if (keyCode == 256) {
                typingSlider = null;
                sliderInputText = "";
                return true;
            } else if (keyCode == 257 || keyCode == 335) {
                try {
                    double value = Double.parseDouble(sliderInputText);
                    value = Math.max(typingSlider.getMin(), Math.min(typingSlider.getMax(), value));
                    typingSlider.setValue(value);
                } catch (NumberFormatException ignored) {
                }
                typingSlider = null;
                sliderInputText = "";
                return true;
            } else if (keyCode == 259) {
                if (!sliderInputText.isEmpty()) {
                    sliderInputText = sliderInputText.substring(0, sliderInputText.length() - 1);
                }
                return true;
            }
            return false;
        }

        if (listeningKeybind != null) {
            if (keyCode == 256) {
                listeningKeybind.setKeyCode(0);
            } else {
                listeningKeybind.setKeyCode(keyCode);
            }
            listeningKeybind = null;
            return true;
        }
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (typingRangeSlider != null) {
            if (Character.isDigit(chr) || chr == '.' || chr == '-') {
                if (chr == '.' && rangeInputText.contains(".")) {
                    return true;
                }
                if (chr == '-' && !rangeInputText.isEmpty()) {
                    return true;
                }
                rangeInputText += chr;
                return true;
            }
        }

        if (typingSlider != null) {
            if (Character.isDigit(chr) || chr == '.' || chr == '-') {
                if (chr == '.' && sliderInputText.contains(".")) {
                    return true;
                }
                if (chr == '-' && !sliderInputText.isEmpty()) {
                    return true;
                }
                sliderInputText += chr;
                return true;
            }
        }
        return false;
    }

    public boolean mouseClickedForKeybind(double mouseX, double mouseY, int button) {
        if (listeningKeybind != null) {
            int mouseKeyCode = -100 - button;
            listeningKeybind.setKeyCode(mouseKeyCode);
            listeningKeybind = null;
            return true;
        }
        return false;
    }

    public boolean isListeningForKey() {
        return listeningKeybind != null;
    }

    private String getKeyName(int keyCode) {
        if (keyCode == 0 || keyCode == -1)
            return "NONE";
        if (keyCode >= 65 && keyCode <= 90) {
            return String.valueOf((char) keyCode);
        }
        return "KEY " + keyCode;
    }

    public void renderGlow(DrawContext context, float alpha) {
        if (animationProgress <= 0f) {
            return;
        }
        GuiGlowHelper.drawGuiGlow(context, currentX, y, width, height, CORNER_RADIUS);
    }
            }
