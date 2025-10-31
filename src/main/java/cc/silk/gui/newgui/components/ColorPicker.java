package cc.silk.gui.newgui.components;

import cc.silk.module.setting.ColorSetting;
import cc.silk.utils.render.NanoVGRenderer;
import java.awt.*;

public class ColorPicker {
    private final ColorSetting setting;
    private float x, y;
    private final int width = 180;
    private final int height = 250;

    private static final Color PANEL_BG = new Color(18, 18, 22, 255);
    private static final Color ACCENT_COLOR = new Color(88, 101, 242, 255);
    private static final Color BORDER_COLOR = new Color(40, 40, 46, 255);
    private static final float CORNER_RADIUS = 4f;
    private static final int PADDING = 8;

    private boolean visible = false;
    private float animationProgress = 0f;

    private float hue = 0f;
    private float saturation = 1f;
    private float brightness = 1f;
    private int alpha = 255;

    private boolean draggingSV = false;
    private boolean draggingHue = false;
    private boolean draggingAlpha = false;

    public ColorPicker(ColorSetting setting, float x, float y) {
        this.setting = setting;
        this.x = x;
        this.y = y;
        colorToHSV(setting.getValue());
    }

    private void colorToHSV(Color color) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
        this.alpha = color.getAlpha();
    }

    private Color hsvToColor() {
        int rgb = Color.HSBtoRGB(hue, saturation, brightness);
        Color color = new Color(rgb);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public void show(float x, float y) {
        this.x = x;
        this.y = y;
        this.visible = true;
        colorToHSV(setting.getValue());
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
    }

    public void render(int mouseX, int mouseY, float alpha) {
        if (animationProgress <= 0f)
            return;

        float renderAlpha = alpha * animationProgress;
        int panelAlpha = (int) (255 * renderAlpha);

        Color panelBg = new Color(PANEL_BG.getRed(), PANEL_BG.getGreen(), PANEL_BG.getBlue(), panelAlpha);
        Color borderColor = new Color(BORDER_COLOR.getRed(), BORDER_COLOR.getGreen(), BORDER_COLOR.getBlue(),
                panelAlpha);

        NanoVGRenderer.drawRoundedRect(x, y, width, height, CORNER_RADIUS, panelBg);
        NanoVGRenderer.drawRoundedRectOutline(x, y, width, height, CORNER_RADIUS, 1f, borderColor);

        float currentY = y + PADDING;
        int svSize = width - PADDING * 2;

        renderSVPicker(currentY, renderAlpha);
        currentY += svSize + 8;

        renderHueSlider(currentY, renderAlpha);
        currentY += 20;

        renderAlphaSlider(currentY, renderAlpha);
        currentY += 20;

        renderPreview(currentY, renderAlpha);
    }

    private void renderSVPicker(float startY, float alpha) {
        int svSize = width - PADDING * 2;
        float svX = x + PADDING;
        float svY = startY;

        int panelAlpha = (int) (255 * alpha);

        int resolution = 32;
        float pixelSize = (float) svSize / resolution;

        for (int i = 0; i < resolution; i++) {
            for (int j = 0; j < resolution; j++) {
                float s = (float) i / resolution;
                float v = 1f - ((float) j / resolution);

                int rgb = Color.HSBtoRGB(hue, s, v);
                Color color = new Color(rgb);
                Color renderColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), panelAlpha);

                NanoVGRenderer.drawRect(svX + (i * pixelSize), svY + (j * pixelSize), pixelSize, pixelSize,
                        renderColor);
            }
        }

        float pickerX = svX + (saturation * svSize);
        float pickerY = svY + ((1f - brightness) * svSize);

        Color pickerOutline = new Color(255, 255, 255, panelAlpha);
        NanoVGRenderer.drawCircle(pickerX, pickerY, 5, pickerOutline);
        Color pickerInner = new Color(0, 0, 0, panelAlpha);
        NanoVGRenderer.drawCircle(pickerX, pickerY, 3, pickerInner);
    }

    private void renderHueSlider(float startY, float alpha) {
        int sliderWidth = width - PADDING * 2;
        float sliderX = x + PADDING;
        float sliderY = startY;
        float sliderHeight = 12;

        int panelAlpha = (int) (255 * alpha);

        int segments = 60;
        float segmentWidth = (float) sliderWidth / segments;

        for (int i = 0; i < segments; i++) {
            float h = (float) i / segments;
            int rgb = Color.HSBtoRGB(h, 1f, 1f);
            Color color = new Color(rgb);
            Color renderColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), panelAlpha);

            NanoVGRenderer.drawRect(sliderX + (i * segmentWidth), sliderY, segmentWidth, sliderHeight, renderColor);
        }

        float handleX = sliderX + (hue * sliderWidth);
        Color handleColor = new Color(255, 255, 255, panelAlpha);
        NanoVGRenderer.drawRect(handleX - 2, sliderY - 2, 4, sliderHeight + 4, handleColor);
    }

    private void renderAlphaSlider(float startY, float alpha) {
        int sliderWidth = width - PADDING * 2;
        float sliderX = x + PADDING;
        float sliderY = startY;
        float sliderHeight = 12;

        int panelAlpha = (int) (255 * alpha);

        Color currentColor = hsvToColor();

        int segments = 60;
        float segmentWidth = (float) sliderWidth / segments;

        for (int i = 0; i < segments; i++) {
            int a = (int) ((float) i / segments * 255);
            Color renderColor = new Color(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(),
                    (int) (a * alpha));

            NanoVGRenderer.drawRect(sliderX + (i * segmentWidth), sliderY, segmentWidth, sliderHeight, renderColor);
        }

        float handleX = sliderX + ((float) this.alpha / 255f * sliderWidth);
        Color handleColor = new Color(255, 255, 255, panelAlpha);
        NanoVGRenderer.drawRect(handleX - 2, sliderY - 2, 4, sliderHeight + 4, handleColor);
    }

    private void renderPreview(float startY, float alpha) {
        int previewSize = 30;
        float previewX = x + PADDING;
        float previewY = startY;

        Color currentColor = hsvToColor();
        int panelAlpha = (int) (255 * alpha);
        Color displayColor = new Color(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(),
                (int) (currentColor.getAlpha() * alpha));
        Color borderColor = new Color(ACCENT_COLOR.getRed(), ACCENT_COLOR.getGreen(), ACCENT_COLOR.getBlue(),
                panelAlpha);

        NanoVGRenderer.drawRoundedRect(previewX, previewY, previewSize, previewSize, 4f, displayColor);
        NanoVGRenderer.drawRoundedRectOutline(previewX, previewY, previewSize, previewSize, 4f, 1.5f, borderColor);

        String hexText = String.format("#%02X%02X%02X%02X",
                currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), currentColor.getAlpha());
        Color textColor = new Color(200, 200, 200, panelAlpha);
        NanoVGRenderer.drawText(hexText, previewX + previewSize + 8, previewY + 10, 9f, textColor);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (animationProgress <= 0f || button != 0)
            return false;

        if (!isMouseOver(mouseX, mouseY))
            return false;

        int svSize = width - PADDING * 2;
        float svX = x + PADDING;
        float svY = y + PADDING;

        if (mouseX >= svX && mouseX <= svX + svSize && mouseY >= svY && mouseY <= svY + svSize) {
            draggingSV = true;
            updateSV(mouseX, mouseY, svX, svY, svSize);
            return true;
        }

        int sliderWidth = width - PADDING * 2;
        float sliderX = x + PADDING;
        float hueY = y + PADDING + svSize + 8;

        if (mouseX >= sliderX && mouseX <= sliderX + sliderWidth &&
                mouseY >= hueY && mouseY <= hueY + 12) {
            draggingHue = true;
            updateHue(mouseX, sliderX, sliderWidth);
            return true;
        }

        float alphaY = hueY + 20;
        if (mouseX >= sliderX && mouseX <= sliderX + sliderWidth &&
                mouseY >= alphaY && mouseY <= alphaY + 12) {
            draggingAlpha = true;
            updateAlpha(mouseX, sliderX, sliderWidth);
            return true;
        }

        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (button != 0)
            return false;

        int svSize = width - PADDING * 2;
        float svX = x + PADDING;
        float svY = y + PADDING;
        int sliderWidth = width - PADDING * 2;
        float sliderX = x + PADDING;

        if (draggingSV) {
            updateSV(mouseX, mouseY, svX, svY, svSize);
            return true;
        }

        if (draggingHue) {
            updateHue(mouseX, sliderX, sliderWidth);
            return true;
        }

        if (draggingAlpha) {
            updateAlpha(mouseX, sliderX, sliderWidth);
            return true;
        }

        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingSV = false;
            draggingHue = false;
            draggingAlpha = false;
        }
    }

    private void updateSV(double mouseX, double mouseY, float svX, float svY, int svSize) {
        saturation = Math.max(0, Math.min(1, (float) (mouseX - svX) / svSize));
        brightness = Math.max(0, Math.min(1, 1f - (float) (mouseY - svY) / svSize));
        Color color = hsvToColor();
        setting.setValue(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    private void updateHue(double mouseX, float sliderX, int sliderWidth) {
        hue = Math.max(0, Math.min(1, (float) (mouseX - sliderX) / sliderWidth));
        Color color = hsvToColor();
        setting.setValue(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    private void updateAlpha(double mouseX, float sliderX, int sliderWidth) {
        alpha = (int) Math.max(0, Math.min(255, ((mouseX - sliderX) / sliderWidth) * 255));
        Color color = hsvToColor();
        setting.setValue(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    private boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isAnimating() {
        return animationProgress > 0f;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }
}
