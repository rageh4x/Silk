package cc.silk.gui.newgui.components;

import cc.silk.module.setting.ColorSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;

import java.awt.*;

public class ColorPicker {
    private static final Color PANEL_BG = new Color(18, 18, 22, 255);
    private static final Color ACCENT_COLOR = new Color(88, 101, 242, 255);
    private static final Color BORDER_COLOR = new Color(40, 40, 46, 255);
    private static final float CORNER_RADIUS = 4f;
    private static final int PADDING = 12;
    private static final int BUTTON_HEIGHT = 22;
    private static final int BUTTON_SPACING = 6;
    private final ColorSetting setting;
    private final int width = 180;
    private final int height = 290;
    private float x, y;
    private boolean visible = false;
    private float animationProgress = 0f;

    private float hue = 0f;
    private float saturation = 1f;
    private float brightness = 1f;
    private int alpha = 255;

    private boolean draggingSV = false;
    private boolean draggingHue = false;
    private boolean draggingAlpha = false;
    
    private static Color clipboard = null;

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

    public void show(float x, float y, int screenWidth, int screenHeight) {
        this.x = Math.max(0, Math.min(x, screenWidth - width));
        this.y = Math.max(0, Math.min(y, screenHeight - height));
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

    public void render(DrawContext context, int mouseX, int mouseY, float alpha) {
        if (animationProgress <= 0f)
            return;

        float renderAlpha = alpha * animationProgress;
        int panelAlpha = (int) (255 * renderAlpha);

        Color panelBg = new Color(PANEL_BG.getRed(), PANEL_BG.getGreen(), PANEL_BG.getBlue(), panelAlpha);
        
        int ix = (int) x;
        int iy = (int) y;
        context.fill(ix, iy, ix + width, iy + height, panelBg.getRGB());

        float currentY = y + PADDING;
        int svSize = width - PADDING * 2;

        renderSVPicker(context, currentY, renderAlpha);
        currentY += svSize + 8;

        renderHueSlider(context, currentY, renderAlpha);
        currentY += 20;

        renderAlphaSlider(context, currentY, renderAlpha);
        currentY += 20;

        renderPreview(context, currentY, renderAlpha);
        currentY += 38;
        
        renderButtons(context, currentY, mouseX, mouseY, renderAlpha);
    }

    private void renderSVPicker(DrawContext context, float startY, float alpha) {
        int svSize = width - PADDING * 2;
        float svX = x + PADDING;
        float svY = startY;
        int panelAlpha = (int) (255 * alpha);

        int resolution = 16;
        float pixelSize = (float) svSize / resolution;

        for (int i = 0; i < resolution; i++) {
            for (int j = 0; j < resolution; j++) {
                float s = (float) i / resolution;
                float v = 1f - ((float) j / resolution);

                int rgb = Color.HSBtoRGB(hue, s, v);
                Color color = new Color(rgb);
                Color renderColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), panelAlpha);

                context.fill((int)(svX + (i * pixelSize)), (int)(svY + (j * pixelSize)), 
                             (int)(svX + (i * pixelSize) + pixelSize + 1), (int)(svY + (j * pixelSize) + pixelSize + 1), renderColor.getRGB());
            }
        }
    }

    private void renderHueSlider(DrawContext context, float startY, float alpha) {
        int sliderWidth = width - PADDING * 2;
        float sliderX = x + PADDING;
        float sliderY = startY;
        float sliderHeight = 12;
        int panelAlpha = (int) (255 * alpha);

        int segments = 30;
        float segmentWidth = (float) sliderWidth / segments;

        for (int i = 0; i < segments; i++) {
            float h = (float) i / segments;
            int rgb = Color.HSBtoRGB(h, 1f, 1f);
            Color color = new Color(rgb);
            Color renderColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), panelAlpha);

            context.fill((int)(sliderX + (i * segmentWidth)), (int)sliderY, 
                         (int)(sliderX + (i * segmentWidth) + segmentWidth + 1), (int)(sliderY + sliderHeight), renderColor.getRGB());
        }
    }

    private void renderAlphaSlider(DrawContext context, float startY, float alpha) {
        int sliderWidth = width - PADDING * 2;
        float sliderX = x + PADDING;
        float sliderY = startY;
        float sliderHeight = 12;
        int panelAlpha = (int) (255 * alpha);

        Color currentColor = hsvToColor();
        int segments = 30;
        float segmentWidth = (float) sliderWidth / segments;

        for (int i = 0; i < segments; i++) {
            int a = (int) ((float) i / segments * 255);
            Color renderColor = new Color(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), (int) (a * alpha));

            context.fill((int)(sliderX + (i * segmentWidth)), (int)sliderY, 
                         (int)(sliderX + (i * segmentWidth) + segmentWidth + 1), (int)(sliderY + sliderHeight), renderColor.getRGB());
        }
    }

    private void renderPreview(DrawContext context, float startY, float alpha) {
        int previewSize = 30;
        float previewX = x + PADDING;
        float previewY = startY;

        Color currentColor = hsvToColor();
        int panelAlpha = (int) (255 * alpha);
        Color displayColor = new Color(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), (int) (currentColor.getAlpha() * alpha));

        context.fill((int)previewX, (int)previewY, (int)(previewX + previewSize), (int)(previewY + previewSize), displayColor.getRGB());
        
        String hexText = String.format("#%02X%02X%02X%02X", currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), currentColor.getAlpha());
        int textColor = new Color(200, 200, 200, panelAlpha).getRGB();
        context.drawText(MinecraftClient.getInstance().textRenderer, hexText, (int)(previewX + previewSize + 8), (int)(previewY + 10), textColor, false);
    }
    
    private void renderButtons(DrawContext context, float startY, int mouseX, int mouseY, float alpha) {
        int buttonWidth = (width - PADDING * 2 - BUTTON_SPACING) / 2;
        float copyButtonX = x + PADDING;
        float pasteButtonX = copyButtonX + buttonWidth + BUTTON_SPACING;
        float buttonY = startY;
        
        int panelAlpha = (int) (255 * alpha);
        Color copyBg = new Color(35, 35, 40, panelAlpha);
        
        context.fill((int)copyButtonX, (int)buttonY, (int)(copyButtonX + buttonWidth), (int)(buttonY + BUTTON_HEIGHT), copyBg.getRGB());
        context.fill((int)pasteButtonX, (int)buttonY, (int)(pasteButtonX + buttonWidth), (int)(buttonY + BUTTON_HEIGHT), copyBg.getRGB());

        int textColor = new Color(200, 200, 200, panelAlpha).getRGB();
        context.drawText(MinecraftClient.getInstance().textRenderer, "Copy", (int)(copyButtonX + 15), (int)(buttonY + 7), textColor, false);
        context.drawText(MinecraftClient.getInstance().textRenderer, "Paste", (int)(pasteButtonX + 15), (int)(buttonY + 7), textColor, false);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (animationProgress <= 0f || button != 0)
            return false;

        if (!isMouseOver(mouseX, mouseY))
            return false;

        int svSize = width - PADDING * 2;
        float svX = x + PADDING;
        float svY = y + PADDING;
        
        int buttonWidth = (width - PADDING * 2 - BUTTON_SPACING) / 2;
        float copyButtonX = x + PADDING;
        float pasteButtonX = copyButtonX + buttonWidth + BUTTON_SPACING;
        float buttonY = y + height - PADDING - BUTTON_HEIGHT;
        
        if (mouseX >= copyButtonX && mouseX <= copyButtonX + buttonWidth &&
            mouseY >= buttonY && mouseY <= buttonY + BUTTON_HEIGHT) {
            clipboard = hsvToColor();
            return true;
        }
        
        if (mouseX >= pasteButtonX && mouseX <= pasteButtonX + buttonWidth &&
            mouseY >= buttonY && mouseY <= buttonY + BUTTON_HEIGHT && clipboard != null) {
            colorToHSV(clipboard);
            setting.setValue(clipboard.getRed(), clipboard.getGreen(), clipboard.getBlue(), clipboard.getAlpha());
            return true;
        }

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
