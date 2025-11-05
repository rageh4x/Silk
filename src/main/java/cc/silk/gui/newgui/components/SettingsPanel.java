package cc.silk.gui.newgui.components;

import cc.silk.module.Module;
import cc.silk.module.setting.*;
import cc.silk.utils.render.nanovg.NanoVGRenderer;

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

    public void render(int mouseX, int mouseY, float alpha) {
        if (animationProgress <= 0f)
            return;

        float transparency = cc.silk.module.modules.client.ClientSettingsModule.getGuiTransparency();
        float renderAlpha = alpha * animationProgress * (1f - transparency);
        int panelAlpha = (int) (255 * renderAlpha);

        Color panelBg = new Color(PANEL_BG.getRed(), PANEL_BG.getGreen(), PANEL_BG.getBlue(), panelAlpha);
        Color borderColor = new Color(BORDER_COLOR.getRed(), BORDER_COLOR.getGreen(), BORDER_COLOR.getBlue(),
                panelAlpha);
        Color headerBg = new Color(HEADER_BG.getRed(), HEADER_BG.getGreen(), HEADER_BG.getBlue(), panelAlpha);
        Color accentColor = getAccentColor();
        Color separatorColor = new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(),
                (int) (80 * renderAlpha));

        NanoVGRenderer.drawRoundedRect(currentX, y, width, height, CORNER_RADIUS, panelBg);
        NanoVGRenderer.drawRoundedRectOutline(currentX, y, width, height, CORNER_RADIUS, 1f, borderColor);

        NanoVGRenderer.drawRoundedRect(currentX, y, width, HEADER_HEIGHT, CORNER_RADIUS, headerBg);
        NanoVGRenderer.drawRect(currentX, y + HEADER_HEIGHT, width, 1, separatorColor);

        Color textColor = new Color(240, 240, 245, panelAlpha);
        float fontSize = 11f;
        NanoVGRenderer.drawText(module.getName(), currentX + PADDING, y + (HEADER_HEIGHT - fontSize) / 2f, fontSize,
                textColor);
        float settingY = y + HEADER_HEIGHT + PADDING;
        for (Setting setting : module.getSettings()) {
            renderSetting(setting, settingY, mouseX, mouseY, renderAlpha, false);
            if (setting instanceof NumberSetting) {
                settingY += NUMBER_SETTING_HEIGHT;
            } else {
                settingY += SETTING_HEIGHT;
            }
        }

        if (expandedDropdown != null) {
            settingY = y + HEADER_HEIGHT + PADDING;
            for (Setting setting : module.getSettings()) {
                if (setting == expandedDropdown) {
                    renderSetting(setting, settingY, mouseX, mouseY, renderAlpha, true);
                    break;
                }

                if (setting instanceof NumberSetting) {
                    settingY += NUMBER_SETTING_HEIGHT;
                } else {
                    settingY += SETTING_HEIGHT;
                }
            }
        }

        if (colorPicker != null && colorPicker.isAnimating()) {
            colorPicker.render(mouseX, mouseY, renderAlpha);
        }
    }

    private void renderSetting(Setting setting, float settingY, int mouseX, int mouseY, float alpha,
                               boolean renderDropdownOnly) {
        if (setting instanceof BooleanSetting) {
            renderBooleanSetting((BooleanSetting) setting, settingY, mouseX, mouseY, alpha);
        } else if (setting instanceof NumberSetting) {
            renderNumberSetting((NumberSetting) setting, settingY, mouseX, mouseY, alpha);
        } else if (setting instanceof ModeSetting) {
            renderModeSetting((ModeSetting) setting, settingY, mouseX, mouseY, alpha, renderDropdownOnly);
        } else if (setting instanceof ColorSetting) {
            renderColorSetting((ColorSetting) setting, settingY, mouseX, mouseY, alpha);
        } else if (setting instanceof KeybindSetting) {
            renderKeybindSetting((KeybindSetting) setting, settingY, mouseX, mouseY, alpha);
        }
    }

    private void renderBooleanSetting(BooleanSetting setting, float settingY, int mouseX, int mouseY, float alpha) {
        float toggleWidth = 20;
        float toggleHeight = 10;
        float toggleX = currentX + width - PADDING - toggleWidth;
        float toggleY = settingY + 3;

        int bgAlpha = (int) (255 * alpha);
        boolean isOn = setting.getValue();

        Color accentColor = getAccentColor();
        Color toggleBg = isOn
                ? new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), bgAlpha)
                : new Color(40, 40, 46, bgAlpha);

        NanoVGRenderer.drawRoundedRect(toggleX, toggleY, toggleWidth, toggleHeight, toggleHeight / 2f, toggleBg);

        float handleSize = 6;
        float handleX = isOn
                ? toggleX + toggleWidth - handleSize - 2
                : toggleX + 2;
        float handleY = toggleY + 2;

        Color handleColor = new Color(255, 255, 255, bgAlpha);
        NanoVGRenderer.drawCircle(handleX + handleSize / 2f, handleY + handleSize / 2f, handleSize / 2f, handleColor);

        Color textColor = new Color(200, 200, 200, bgAlpha);
        float fontSize = 9f;
        NanoVGRenderer.drawText(setting.getName(), currentX + PADDING, settingY + 3, fontSize, textColor);
    }

    private void renderNumberSetting(NumberSetting setting, float settingY, int mouseX, int mouseY, float alpha) {
        float sliderY = settingY + 13;
        float sliderWidth = width - PADDING * 2;
        float sliderHeight = 4;
        float sliderRadius = sliderHeight / 2f;
        int bgAlpha = (int) (255 * alpha);
        Color accentColor = getAccentColor();
        Color sliderBg = new Color(30, 30, 35, bgAlpha);
        Color sliderFill = new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), bgAlpha);
        Color textColor = new Color(200, 200, 200, bgAlpha);
        Color valueColor = new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), bgAlpha);
        NanoVGRenderer.drawRoundedRect(currentX + PADDING, sliderY, sliderWidth, sliderHeight, sliderRadius, sliderBg);
        double percentage = (setting.getValue() - setting.getMin()) / (setting.getMax() - setting.getMin());
        float fillWidth = (float) (sliderWidth * percentage);
        if (fillWidth > 0) {
            NanoVGRenderer.drawRoundedRect(currentX + PADDING, sliderY, fillWidth, sliderHeight, sliderRadius, sliderFill);
        }
        float handleRadius = 5;
        float handleX = currentX + PADDING + fillWidth;
        float handleY = sliderY + sliderHeight / 2f;
        Color handleGlow = new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), (int) (60 * alpha));
        NanoVGRenderer.drawCircle(handleX, handleY, handleRadius + 1.5f, handleGlow);
        Color handleColor = new Color(255, 255, 255, bgAlpha);
        NanoVGRenderer.drawCircle(handleX, handleY, handleRadius, handleColor);
        String displayText = setting.getName();
        String valueText = formatNumber(setting.getValue());
        float fontSize = 9f;
        float valueWidth = NanoVGRenderer.getTextWidth(valueText, fontSize);
        NanoVGRenderer.drawText(displayText, currentX + PADDING, settingY + 2, fontSize, textColor);
        NanoVGRenderer.drawText(valueText, currentX + width - PADDING - valueWidth, settingY + 2, fontSize, valueColor);
    }

    private void renderModeSetting(ModeSetting setting, float settingY, int mouseX, int mouseY, float alpha,
                                   boolean renderDropdownOnly) {
        String mode = setting.getMode();
        float fontSize = 9f;

        int bgAlpha = (int) (255 * alpha);
        Color accentColor = getAccentColor();
        Color dropdownBg = new Color(30, 30, 35, bgAlpha);
        Color dropdownBorder = new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(),
                (int) (100 * alpha));
        Color textColor = new Color(200, 200, 200, bgAlpha);
        Color modeText = new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), bgAlpha);

        float dropdownWidth = 60;
        float dropdownHeight = 14;
        float dropdownX = currentX + width - PADDING - dropdownWidth;
        float dropdownY = settingY + 1;

        if (!renderDropdownOnly) {
            NanoVGRenderer.drawRoundedRect(dropdownX, dropdownY, dropdownWidth, dropdownHeight, 3f, dropdownBg);
            NanoVGRenderer.drawRoundedRectOutline(dropdownX, dropdownY, dropdownWidth, dropdownHeight, 3f, 1f,
                    dropdownBorder);

            String arrow = expandedDropdown == setting ? "▲" : "▼";
            float arrowWidth = NanoVGRenderer.getTextWidth(arrow, fontSize);
            float availableWidth = dropdownWidth - 8 - arrowWidth - 4;

            String displayMode = mode;
            float modeWidth = NanoVGRenderer.getTextWidth(displayMode, fontSize);
            if (modeWidth > availableWidth) {
                while (modeWidth > availableWidth && displayMode.length() > 3) {
                    displayMode = displayMode.substring(0, displayMode.length() - 1);
                    modeWidth = NanoVGRenderer.getTextWidth(displayMode + "...", fontSize);
                }
                displayMode += "...";
            }

            NanoVGRenderer.drawText(displayMode, dropdownX + 4, dropdownY + 2, fontSize, modeText);
            NanoVGRenderer.drawText(arrow, dropdownX + dropdownWidth - arrowWidth - 4, dropdownY + 2, fontSize,
                    textColor);

            NanoVGRenderer.drawText(setting.getName(), currentX + PADDING, settingY + 3, fontSize, textColor);
        }

        if (expandedDropdown == setting && renderDropdownOnly) {
            float menuY = dropdownY + dropdownHeight + 2;
            int optionCount = setting.getModes().size();
            float menuHeight = optionCount * 14;

            Color menuBg = new Color(25, 25, 30, bgAlpha);
            Color hoverBg = new Color(40, 40, 46, bgAlpha);
            Color menuBorder = new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(),
                    (int) (150 * alpha));

            NanoVGRenderer.drawRoundedRect(dropdownX, menuY, dropdownWidth, menuHeight, 3f, menuBg);
            NanoVGRenderer.drawRoundedRectOutline(dropdownX, menuY, dropdownWidth, menuHeight, 3f, 1.5f, menuBorder);

            float optionY = menuY;
            for (String option : setting.getModes()) {
                boolean isHovered = mouseX >= dropdownX && mouseX <= dropdownX + dropdownWidth &&
                        mouseY >= optionY && mouseY <= optionY + 14;
                boolean isSelected = option.equals(mode);

                if (isHovered) {
                    NanoVGRenderer.drawRect(dropdownX + 1, optionY, dropdownWidth - 2, 14, hoverBg);
                }

                String displayOption = option;
                float optionWidth = NanoVGRenderer.getTextWidth(displayOption, fontSize);
                float maxOptionWidth = dropdownWidth - 8;
                if (optionWidth > maxOptionWidth) {
                    while (optionWidth > maxOptionWidth && displayOption.length() > 3) {
                        displayOption = displayOption.substring(0, displayOption.length() - 1);
                        optionWidth = NanoVGRenderer.getTextWidth(displayOption + "...", fontSize);
                    }
                    displayOption += "...";
                }

                Color optionColor = isSelected ? modeText : textColor;
                NanoVGRenderer.drawText(displayOption, dropdownX + 4, optionY + 2, fontSize, optionColor);
                optionY += 14;
            }
        }
    }

    private void renderColorSetting(ColorSetting setting, float settingY, int mouseX, int mouseY, float alpha) {
        int previewSize = 12;
        float previewX = currentX + width - PADDING - previewSize;
        float previewY = settingY + 2;

        Color color = setting.getValue();
        int colorAlpha = (int) (color.getAlpha() * alpha);
        Color displayColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), colorAlpha);
        Color accentColor = getAccentColor();
        Color border = new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(),
                (int) (150 * alpha));
        Color textColor = new Color(200, 200, 200, (int) (255 * alpha));

        NanoVGRenderer.drawRoundedRect(previewX, previewY, previewSize, previewSize, 3f, displayColor);
        NanoVGRenderer.drawRoundedRectOutline(previewX, previewY, previewSize, previewSize, 3f, 1f, border);

        float fontSize = 9f;
        NanoVGRenderer.drawText(setting.getName(), currentX + PADDING, settingY + 3, fontSize, textColor);
    }

    private void renderKeybindSetting(KeybindSetting setting, float settingY, int mouseX, int mouseY, float alpha) {
        int bgAlpha = (int) (255 * alpha);
        boolean isListening = listeningKeybind == setting;

        String keyText = isListening ? "..." : getKeyName(setting.getKeyCode());
        float fontSize = 9f;
        float keyWidth = NanoVGRenderer.getTextWidth(keyText, fontSize);

        float buttonWidth = Math.max(keyWidth + 12, 40);
        float buttonHeight = 14;
        float buttonX = currentX + width - PADDING - buttonWidth;
        float buttonY = settingY + 1;

        Color accentColor = getAccentColor();
        Color buttonBg = isListening
                ? new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), (int) (100 * alpha))
                : new Color(30, 30, 35, bgAlpha);
        Color buttonBorder = new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(),
                isListening ? (int) (200 * alpha) : (int) (100 * alpha));
        Color textColor = new Color(200, 200, 200, bgAlpha);
        Color keyColor = isListening
                ? new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), bgAlpha)
                : new Color(200, 200, 200, bgAlpha);

        NanoVGRenderer.drawRoundedRect(buttonX, buttonY, buttonWidth, buttonHeight, 3f, buttonBg);
        NanoVGRenderer.drawRoundedRectOutline(buttonX, buttonY, buttonWidth, buttonHeight, 3f, 1f, buttonBorder);

        float textX = buttonX + (buttonWidth - keyWidth) / 2f;
        NanoVGRenderer.drawText(keyText, textX, buttonY + 2, fontSize, keyColor);

        NanoVGRenderer.drawText(setting.getName(), currentX + PADDING, settingY + 3, fontSize, textColor);
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

                int currentHeight = (setting instanceof NumberSetting) ? NUMBER_SETTING_HEIGHT : SETTING_HEIGHT;
                settingY += currentHeight;
            }
        }

        if (!isMouseOver(mouseX, mouseY))
            return false;

        float settingY = y + HEADER_HEIGHT + PADDING;
        for (Setting setting : module.getSettings()) {
            int currentHeight = (setting instanceof NumberSetting) ? NUMBER_SETTING_HEIGHT : SETTING_HEIGHT;

            if (mouseY >= settingY && mouseY <= settingY + currentHeight) {
                if (setting instanceof BooleanSetting) {
                    ((BooleanSetting) setting).toggle();
                    return true;
                } else if (setting instanceof NumberSetting) {
                    draggingSlider = setting;
                    updateSliderValue((NumberSetting) setting, mouseX);
                    return true;
                } else if (setting instanceof KeybindSetting) {
                    listeningKeybind = (listeningKeybind == setting) ? null : (KeybindSetting) setting;
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

        switch (keyCode) {
            case 32:
                return "SPACE";
            case 256:
                return "ESC";
            case 257:
                return "ENTER";
            case 258:
                return "TAB";
            case 259:
                return "BACKSPACE";
            case 260:
                return "INSERT";
            case 261:
                return "DELETE";
            case 262:
                return "RIGHT";
            case 263:
                return "LEFT";
            case 264:
                return "DOWN";
            case 265:
                return "UP";
            case 266:
                return "PAGE UP";
            case 267:
                return "PAGE DOWN";
            case 268:
                return "HOME";
            case 269:
                return "END";
            case 280:
                return "CAPS LOCK";
            case 281:
                return "SCROLL LOCK";
            case 282:
                return "NUM LOCK";
            case 283:
                return "PRINT SCREEN";
            case 284:
                return "PAUSE";
            case 290:
                return "F1";
            case 291:
                return "F2";
            case 292:
                return "F3";
            case 293:
                return "F4";
            case 294:
                return "F5";
            case 295:
                return "F6";
            case 296:
                return "F7";
            case 297:
                return "F8";
            case 298:
                return "F9";
            case 299:
                return "F10";
            case 300:
                return "F11";
            case 301:
                return "F12";
            case 340:
                return "LSHIFT";
            case 341:
                return "LCTRL";
            case 342:
                return "LALT";
            case 343:
                return "LSUPER";
            case 344:
                return "RSHIFT";
            case 345:
                return "RCTRL";
            case 346:
                return "RALT";
            case 347:
                return "RSUPER";
            case 348:
                return "MENU";
            default:
                if (keyCode >= 320 && keyCode <= 329) {
                    return "NUMPAD " + (keyCode - 320);
                }
                if (keyCode <= -100) {
                    int mouseButton = -100 - keyCode;
                    switch (mouseButton) {
                        case 0:
                            return "MOUSE LEFT";
                        case 1:
                            return "MOUSE RIGHT";
                        case 2:
                            return "MOUSE MIDDLE";
                        default:
                            return "MOUSE " + (mouseButton + 1);
                    }
                }
                if (keyCode >= 65 && keyCode <= 90) {
                    return String.valueOf((char) keyCode);
                }
                if (keyCode >= 48 && keyCode <= 57) {
                    return String.valueOf((char) keyCode);
                }
                return "KEY " + keyCode;
        }
    }
}
