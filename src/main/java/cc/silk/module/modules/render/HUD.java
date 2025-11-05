package cc.silk.module.modules.render;

import cc.silk.SilkClient;
import cc.silk.event.impl.render.Render2DEvent;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.*;
import cc.silk.utils.render.nanovg.NanoVGRenderer;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Formatting;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HUD extends Module {

    public static final BooleanSetting arrayList = new BooleanSetting("ArrayList", true);
    public static final NumberSetting arrayListScale = new NumberSetting("ArrayList Scale", 0.5, 3.0, 1.0, 0.1);
    public static final ModeSetting colorMode = new ModeSetting("Color Mode", "Astolfo", "Astolfo", "Theme", "Custom");
    public static final ColorSetting customColor = new ColorSetting("Custom Color", new Color(255, 255, 255));
    public static final ModeSetting fontMode = new ModeSetting("ArrayList Font", "MC", "MC", "Inter", "JetbrainsMono", "Poppins");
    public static final BooleanSetting hideVisuals = new BooleanSetting("Hide visuals", true);
    public static final BooleanSetting lowercase = new BooleanSetting("Lowercase", false);
    public static final ModeSetting backBarMode = new ModeSetting("Backbar Mode", "None", "None", "Full", "Rise");
    public static final BooleanSetting outline = new BooleanSetting("Outline", true);
    public static final BooleanSetting waveAnimation = new BooleanSetting("Wave Animation", false);
    public static final ModeSetting waveDirection = new ModeSetting("Wave Direction", "Down", "Down", "Up", "Sideways");
    public static final NumberSetting waveSpeed = new NumberSetting("Wave Speed", 0.1, 5.0, 1.5, 0.1);
    public static final NumberSetting waveSpread = new NumberSetting("Wave Spread", 0.1, 5.0, 0.6, 0.1);
    public static final BooleanSetting glow = new BooleanSetting("Glow", true);
    public static final NumberSetting glowIntensity = new NumberSetting("Glow Intensity", 0.1, 2.0, 0.6, 0.05);
    public static final NumberSetting padding = new NumberSetting("Offset", 0, 40, 5, 1);
    public static final NumberSetting opacity = new NumberSetting("BG Opacity", 0, 255, 80, 1);
    public static final NumberSetting scale = new NumberSetting("Scale", 0.5, 3.0, 1.0, 0.1);

    private final MinecraftClient mc = MinecraftClient.getInstance();

    public HUD() {
        super("HUD", "Renders information", -1, Category.RENDER);
        addSettings(arrayList, arrayListScale, colorMode, customColor, fontMode, hideVisuals, lowercase,
                backBarMode, outline, waveAnimation, waveDirection, waveSpeed, waveSpread, glow, glowIntensity, padding,
                opacity, scale);
    }

    public static int getAstolfo(int offset) {
        int i = (int) ((System.currentTimeMillis() / 11 + offset) % 360);
        i = (i > 180 ? 360 - i : i) + 180;
        return Color.HSBtoRGB(i / 360f, 0.55f, 1f);
    }

    @EventHandler
    private void onEventRender2D(Render2DEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;

        NanoVGRenderer.beginFrame();

        if (arrayList.getValue()) {
            List<Module> enabledModules = new ArrayList<>();
            for (Module module : SilkClient.INSTANCE.getModuleManager().getModules()) {
                if (!module.isEnabled()) continue;
                if (hideVisuals.getValue() && module.getModuleCategory() == Category.RENDER) continue;
                enabledModules.add(module);
            }

            enabledModules.sort(Comparator.comparingDouble(ri -> -getTextWidthWithFormatting(getFullName(ri), (int)(16 * arrayListScale.getValue()))));

            int i = padding.getValueInt();
            int totalWidth = event.getWidth();
            int moduleIndex = 0;
            double waveTime = (System.currentTimeMillis() / 1000.0) * waveSpeed.getValue();

            for (Module m : enabledModules) {
                String fullName = getFullName(m);
                int fontSize = (int)(16 * arrayListScale.getValue());
                int fontId = getFontIdForMode(fontMode.getMode());

                int color = switch (colorMode.getMode()) {
                    case "Astolfo" -> getAstolfo(i * 3);
                    case "Theme" -> getThemeColor().getRGB();
                    case "Custom" -> getCustomColor().getRGB();
                    default -> 0;
                };

                int moduleWidth = (int) Math.ceil(getTextWidthWithFormatting(fullName, fontSize));
                int moduleHeight = (int) Math.ceil(NanoVGRenderer.getTextHeight(fontSize));
                int backgroundLeft = totalWidth - moduleWidth - padding.getValueInt() - (int)(3 * arrayListScale.getValue());

                if (waveAnimation.getValue()) {
                    double wavePhase = switch (waveDirection.getMode()) {
                        case "Up" -> waveTime - moduleIndex * waveSpread.getValue();
                        case "Sideways" -> waveTime + (totalWidth - backgroundLeft) * 0.01 * waveSpread.getValue();
                        default -> waveTime + moduleIndex * waveSpread.getValue();
                    };
                    double waveValue = (Math.sin(wavePhase) + 1.0) * 0.5;
                    color = applyWaveColor(color, Math.sin(waveValue * Math.PI * 0.5));
                }

                if (opacity.getValueInt() > 0) {
                    Color bg = new Color(0, 0, 0, opacity.getValueInt());
                    NanoVGRenderer.drawRect(backgroundLeft, i - (int)(2 * arrayListScale.getValue()),
                            moduleWidth + (int)(6 * arrayListScale.getValue()), moduleHeight + (int)(3 * arrayListScale.getValue()), bg);
                }

                if (glow.getValue()) {
                    renderTextWithGlowNanoVG(fullName, totalWidth - moduleWidth - padding.getValueInt(), i, color, fontId, fontSize);
                } else {
                    NanoVGRenderer.drawTextWithFont(fullName, totalWidth - moduleWidth - padding.getValueInt(), i, fontSize, new Color(color, true), fontId);
                }

                i += moduleHeight + (int)(3 * arrayListScale.getValue());
                moduleIndex++;
            }
        }

        NanoVGRenderer.endFrame();
    }


    private int getFontIdForMode(String mode) {
        return switch (mode) {
            case "JetbrainsMono" -> NanoVGRenderer.getJetBrainsFontId();
            case "Poppins" -> NanoVGRenderer.getPoppinsFontId();
            default -> NanoVGRenderer.getRegularFontId();
        };
    }

    private String stripFormatting(String text) {
        return text.replaceAll("§.", "");
    }

    private float getTextWidthWithFormatting(String text, int fontSize) {
        return NanoVGRenderer.getTextWidthWithFont(stripFormatting(text), fontSize, getFontIdForMode(fontMode.getMode()));
    }

    private int applyWaveColor(int baseColor, double waveValue) {
        Color base = new Color(baseColor, true);
        double wave = Math.pow(waveValue, 0.5);
        int r = (int)(base.getRed() + (255 - base.getRed()) * wave);
        int g = (int)(base.getGreen() + (255 - base.getGreen()) * wave);
        int b = (int)(base.getBlue() + (255 - base.getBlue()) * wave);
        return (base.getAlpha() << 24) | (r << 16) | (g << 8) | b;
    }

    private Color getThemeColor() {
        return new Color(100, 150, 255);
    }

    private Color getCustomColor() {
        return customColor.getValue();
    }

    private String getFullName(Module m) {
       return lowercase.getValue() ? m.getName().toLowerCase() : m.getName();
    }

    private void renderTextWithGlowNanoVG(String text, int x, int y, int color, int fontId, int fontSize) {
        float intensity = glowIntensity.getValueFloat();
        Color c = new Color(color, true);
        for (int pass = 0; pass < 4; pass++) {
            int glowAlpha = 0, offsetX = 0, offsetY = 0;
            switch (pass) {
                case 0 -> { glowAlpha = (int)(c.getAlpha() * intensity * 0.2f); offsetX = 1; offsetY = 1; }
                case 1 -> { glowAlpha = (int)(c.getAlpha() * intensity * 0.3f); offsetX = 1; }
                case 2 -> { glowAlpha = (int)(c.getAlpha() * intensity * 0.3f); offsetY = 1; }
                case 3 -> glowAlpha = c.getAlpha();
            }
            Color glowColor = new Color(c.getRed(), c.getGreen(), c.getBlue(), glowAlpha);
            NanoVGRenderer.drawTextWithFont(text, x + offsetX, y + offsetY, fontSize, glowColor, fontId);
        }
    }
}
