package cc.silk.module.modules.render;

import cc.silk.SilkClient;
import cc.silk.event.impl.render.Render2DEvent;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.*;
import cc.silk.utils.render.NanoVGRenderer;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Formatting;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HUD extends Module {
    public static final BooleanSetting arrayList = new BooleanSetting("ArrayList", true);
    public static final NumberSetting arrayListScale = new NumberSetting("ArrayList Scale", 0.5, 3.0, 1.0, 0.1);
    public static final ModeSetting colorMode = new ModeSetting("Color Mode", "Astolfo", "Astolfo", "Theme", "Custom");
    public static final ColorSetting customColor = new ColorSetting("Custom Color", new Color(255, 255, 255));
    public static final ModeSetting fontMode = new ModeSetting("ArrayList Font", "MC", "MC", "Inter", "JetbrainsMono",
            "Poppins");
    public static final ModeSetting suffixMode = new ModeSetting("Suffix", "Space", "Space", "-", ">", "[ ]");
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
    public static final BooleanSetting info = new BooleanSetting("Info", true);
    public static final BooleanSetting bpsCounter = new BooleanSetting("BPS", true);
    public static final BooleanSetting fpsCounter = new BooleanSetting("FPS", true);
    public static final NumberSetting scale = new NumberSetting("Scale", 0.5, 3.0, 1.0, 0.1);

    private final DecimalFormat decimalFormat = new DecimalFormat("0.00");

    public HUD() {
        super("HUD", "Renders information", -1, Category.RENDER);
        addSettings(arrayList, arrayListScale, colorMode, customColor, fontMode, suffixMode, hideVisuals, lowercase,
                backBarMode, outline, waveAnimation, waveDirection, waveSpeed, waveSpread, glow, glowIntensity, padding,
                opacity, info, bpsCounter, fpsCounter, scale);
    }

    public static int getAstolfo(int offset) {
        int i = (int) ((System.currentTimeMillis() / 11 + offset) % 360);
        i = (i > 180 ? 360 - i : i) + 180;
        return Color.HSBtoRGB(i / 360f, 0.55f, 1f);
    }

    @EventHandler
    private void onEventRender2D(Render2DEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (mc.currentScreen != null) {
            return;
        }

        try {
            NanoVGRenderer.beginFrame();
        } catch (Exception e) {
            return;
        }

        if (arrayList.getValue()) {
            List<Module> enabledModules = new ArrayList<>();
            for (Module module : SilkClient.INSTANCE.getModuleManager().getModules()) {
                if (!module.isEnabled())
                    continue;
                if (hideVisuals.getValue() && module.getModuleCategory() == Category.RENDER)
                    continue;
                enabledModules.add(module);
            }

            int fontSize = (int) (16 * arrayListScale.getValue());
            int fontId = getFontIdForMode(fontMode.getMode());

            enabledModules.sort(Comparator.comparingDouble(
                    ri -> -getTextWidthWithFormatting(getFullName(ri), fontSize)));

            int i = padding.getValueInt();
            int totalWidth = event.getWidth();

            for (Module m : enabledModules) {
                int moduleHeight = (int) NanoVGRenderer.getTextHeight(fontSize);
                i += moduleHeight + (int) (3 * arrayListScale.getValue());
            }

            i = padding.getValueInt();
            double waveTime = (System.currentTimeMillis() / 1000.0) * waveSpeed.getValue();
            int moduleIndex = 0;

            int prevBackgroundLeft = -1;

            for (Module m : enabledModules) {
                String fullName = getFullName(m);
                int color = switch (colorMode.getMode()) {
                    case "Astolfo" -> getAstolfo(i * 3);
                    case "Theme" -> getThemeColor().getRGB();
                    case "Custom" -> getCustomColor().getRGB();
                    default -> 0;
                };

                int moduleWidth = (int) Math.ceil(getTextWidthWithFormatting(fullName, fontSize));
                int moduleHeight = (int) Math.ceil(NanoVGRenderer.getTextHeight(fontSize));

                int scaledPadding1 = (int) (1 * arrayListScale.getValue());
                int scaledPadding2 = (int) (2 * arrayListScale.getValue());
                int scaledPadding3 = (int) (3 * arrayListScale.getValue());
                int scaledPadding5 = (int) (5 * arrayListScale.getValue());

                int backgroundLeft = totalWidth - moduleWidth - padding.getValueInt() - scaledPadding3;

                int textColor = color;
                if (waveAnimation.getValue()) {
                    double wavePhase = switch (waveDirection.getMode()) {
                        case "Up" -> waveTime - moduleIndex * waveSpread.getValue();
                        case "Sideways" -> waveTime + (totalWidth - backgroundLeft) * 0.01 * waveSpread.getValue();
                        default -> waveTime + moduleIndex * waveSpread.getValue();
                    };
                    double waveValue = (Math.sin(wavePhase) + 1.0) * 0.5;
                    double wave = Math.sin(waveValue * Math.PI * 0.5);
                    textColor = applyWaveColor(color, wave);
                }
                int backgroundTop = i - scaledPadding2;
                int backgroundRight = totalWidth - padding.getValueInt() + scaledPadding5;
                int backgroundBottom = i + moduleHeight + scaledPadding1;

                if (opacity.getValueInt() > 0) {
                    Color backgroundColor = new Color(0, 0, 0, opacity.getValueInt());
                    NanoVGRenderer.drawRect(backgroundLeft, backgroundTop,
                            backgroundRight - backgroundLeft, backgroundBottom - backgroundTop, backgroundColor);
                }

                boolean isFirst = moduleIndex == 0;
                boolean isLast = moduleIndex == enabledModules.size() - 1;

                if (outline.getValue()) {
                    if (glow.getValue()) {
                        renderBordersWithGlow(event, backgroundLeft, backgroundTop, backgroundRight, backgroundBottom,
                                prevBackgroundLeft, isFirst, isLast, color);
                    } else {
                        renderBorders(event, backgroundLeft, backgroundTop, backgroundRight, backgroundBottom,
                                prevBackgroundLeft, isFirst, isLast, color);
                    }
                }

                prevBackgroundLeft = backgroundLeft - 1;

                switch (backBarMode.getMode()) {
                    case "Full" -> {
                        int x = totalWidth - padding.getValueInt() + scaledPadding3;
                        int y = i - scaledPadding2;
                        int width = scaledPadding5 - scaledPadding3;
                        int height = moduleHeight + scaledPadding1 + scaledPadding2;
                        NanoVGRenderer.drawRect(x, y, width, height, new Color(color, true));
                    }
                    case "Rise" -> {
                        int x = totalWidth - padding.getValueInt() + scaledPadding3;
                        int y = i - scaledPadding1;
                        int width = scaledPadding5 - scaledPadding3;
                        int height = moduleHeight + scaledPadding1;
                        NanoVGRenderer.drawRect(x, y, width, height, new Color(color, true));
                    }
                }

                String displayText = stripFormatting(fullName);
                int textX = totalWidth - moduleWidth - padding.getValueInt();
                int textY = i;

                if (glow.getValue()) {
                    renderTextWithGlowNanoVG(displayText, textX, textY, textColor, fontId, fontSize);
                } else {
                    NanoVGRenderer.drawTextWithFont(displayText, textX, textY, fontSize,
                            new Color(textColor, true), fontId);
                }

                i += moduleHeight + (int) (3 * arrayListScale.getValue());
                moduleIndex++;
            }
        }

        if (info.getValue()) {
            String bps = String.valueOf(decimalFormat.format(
                    Math.hypot(mc.player.getX() - mc.player.prevX, mc.player.getZ() - mc.player.prevZ) * 20.0F));
            String fps = String.valueOf(MinecraftClient.getInstance().getCurrentFps());

            int infoFontSize = (int) (16 * scale.getValue());
            int x = (int) (10 * scale.getValue());
            int lineHeight = (int) (NanoVGRenderer.getTextHeight(infoFontSize) * scale.getValue());

            int y = event.getHeight() - (fpsCounter.getValue() && bpsCounter.getValue() ? 2 * lineHeight : lineHeight)
                    - (int) (10 * scale.getValue());

            if (fpsCounter.getValue()) {
                int color = switch (colorMode.getMode()) {
                    case "Astolfo" -> getAstolfo(3);
                    case "Theme" -> getThemeColor(1).getRGB();
                    case "Custom" -> getCustomColor().getRGB();
                    default -> 0;
                };
                NanoVGRenderer.drawText("FPS: " + fps, x, y, infoFontSize, new Color(color, true));
                y -= lineHeight;
            }

            if (bpsCounter.getValue()) {
                int color = switch (colorMode.getMode()) {
                    case "Astolfo" -> getAstolfo(6);
                    case "Theme" -> getThemeColor(2).getRGB();
                    case "Custom" -> getCustomColor().getRGB();
                    default -> 0;
                };
                NanoVGRenderer.drawText("BPS: " + bps, x, y, infoFontSize, new Color(color, true));
            }
        }

        try {
            NanoVGRenderer.endFrame();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getFontIdForMode(String mode) {
        return switch (mode) {
            case "Inter" -> NanoVGRenderer.getRegularFontId();
            case "JetbrainsMono" -> NanoVGRenderer.getJetBrainsFontId();
            case "Poppins" -> NanoVGRenderer.getPoppinsFontId();
            default -> NanoVGRenderer.getRegularFontId();
        };
    }

    private String stripFormatting(String text) {
        return text.replaceAll("§.", "");
    }

    private float getTextWidthWithFormatting(String text, int fontSize) {
        String stripped = stripFormatting(text);
        int fontId = getFontIdForMode(fontMode.getMode());
        return NanoVGRenderer.getTextWidthWithFont(stripped, fontSize, fontId);
    }

    private String getFullName(Module m) {
        if (lowercase.getValue()) {
            if (m.getSuffix() == null) {
                return m.getName().toLowerCase();
            } else {
                switch (suffixMode.getMode()) {
                    case "Space":
                        return (m.getName() + " " + Formatting.GRAY + m.getSuffix()).toLowerCase();
                    case "-":
                        return (m.getName() + Formatting.GRAY + " - " + m.getSuffix()).toLowerCase();
                    case ">":
                        return (m.getName() + Formatting.GRAY + " > " + m.getSuffix()).toLowerCase();
                    case "[ ]":
                        return (m.getName() + Formatting.DARK_GRAY + " [" + Formatting.GRAY + m.getSuffix()
                                + Formatting.DARK_GRAY + "]").toLowerCase();
                }
            }
        } else {
            if (m.getSuffix() == null) {
                return m.getName();
            } else {
                switch (suffixMode.getMode()) {
                    case "Space":
                        return m.getName() + " " + Formatting.GRAY + m.getSuffix();
                    case "-":
                        return m.getName() + Formatting.GRAY + " - " + m.getSuffix();
                    case ">":
                        return m.getName() + Formatting.GRAY + " > " + m.getSuffix();
                    case "[ ]":
                        return m.getName() + Formatting.DARK_GRAY + " [" + Formatting.GRAY + m.getSuffix()
                                + Formatting.DARK_GRAY + "]";
                }
            }
        }
        return "";
    }

    private void renderBorders(Render2DEvent event, int backgroundLeft, int backgroundTop,
                               int backgroundRight, int backgroundBottom, int prevBackgroundLeft,
                               boolean isFirst, boolean isLast, int color) {
        Color c = new Color(color, true);

        if (isFirst) {
            NanoVGRenderer.drawRect(backgroundLeft - 1, backgroundTop - 1,
                    backgroundRight - backgroundLeft + 2, 1, c);
        }

        NanoVGRenderer.drawRect(backgroundLeft - 1, backgroundTop, 1, backgroundBottom - backgroundTop, c);
        NanoVGRenderer.drawRect(backgroundRight, backgroundTop, 1, backgroundBottom - backgroundTop, c);

        if (prevBackgroundLeft != -1 && prevBackgroundLeft != backgroundLeft - 1) {
            if (prevBackgroundLeft < backgroundLeft - 1) {
                NanoVGRenderer.drawRect(prevBackgroundLeft, backgroundTop - 1,
                        backgroundLeft - prevBackgroundLeft, 1, c);
            } else {
                NanoVGRenderer.drawRect(backgroundLeft - 1, backgroundTop - 1,
                        prevBackgroundLeft - backgroundLeft + 2, 1, c);
            }
        }

        if (isLast) {
            NanoVGRenderer.drawRect(backgroundLeft - 1, backgroundBottom,
                    backgroundRight - backgroundLeft + 2, 1, c);
        }
    }

    private void renderBordersWithGlow(Render2DEvent event, int backgroundLeft, int backgroundTop,
                                       int backgroundRight, int backgroundBottom, int prevBackgroundLeft,
                                       boolean isFirst, boolean isLast, int color) {
        float intensity = glowIntensity.getValueFloat();
        Color c = new Color(color, true);

        if (isFirst) {
            drawRectWithGlow(backgroundLeft - 1, backgroundTop - 1,
                    backgroundRight - backgroundLeft + 2, 1, c, intensity);
        }

        drawRectWithGlow(backgroundLeft - 1, backgroundTop, 1, backgroundBottom - backgroundTop, c, intensity);
        drawRectWithGlow(backgroundRight, backgroundTop, 1, backgroundBottom - backgroundTop, c, intensity);

        if (prevBackgroundLeft != -1 && prevBackgroundLeft != backgroundLeft - 1) {
            if (prevBackgroundLeft < backgroundLeft - 1) {
                drawRectWithGlow(prevBackgroundLeft, backgroundTop - 1,
                        backgroundLeft - prevBackgroundLeft, 1, c, intensity);
            } else {
                drawRectWithGlow(backgroundLeft - 1, backgroundTop - 1,
                        prevBackgroundLeft - backgroundLeft + 2, 1, c, intensity);
            }
        }

        if (isLast) {
            drawRectWithGlow(backgroundLeft - 1, backgroundBottom,
                    backgroundRight - backgroundLeft + 2, 1, c, intensity);
        }
    }

    private void drawRectWithGlow(int x, int y, int width, int height, Color color, float intensity) {
        for (int pass = 0; pass < 4; pass++) {
            int glowExpand = 0;
            int glowAlpha = 0;

            switch (pass) {
                case 0:
                    glowExpand = 3;
                    glowAlpha = (int) (color.getAlpha() * intensity * 0.15f);
                    break;
                case 1:
                    glowExpand = 2;
                    glowAlpha = (int) (color.getAlpha() * intensity * 0.3f);
                    break;
                case 2:
                    glowExpand = 1;
                    glowAlpha = (int) (color.getAlpha() * intensity * 0.5f);
                    break;
                case 3:
                    glowExpand = 0;
                    glowAlpha = color.getAlpha();
                    break;
            }

            Color glowColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), glowAlpha);
            NanoVGRenderer.drawRect(x - glowExpand, y - glowExpand,
                    width + (glowExpand * 2), height + (glowExpand * 2), glowColor);
        }
    }

    private void renderTextWithGlowNanoVG(String text, int x, int y, int color, int fontId, int fontSize) {
        float intensity = glowIntensity.getValueFloat();
        Color c = new Color(color, true);

        for (int pass = 0; pass < 4; pass++) {
            int glowAlpha = 0;
            int offsetX = 0;
            int offsetY = 0;

            switch (pass) {
                case 0:
                    glowAlpha = (int) (c.getAlpha() * intensity * 0.2f);
                    offsetX = 1;
                    offsetY = 1;
                    break;
                case 1:
                    glowAlpha = (int) (c.getAlpha() * intensity * 0.3f);
                    offsetX = 1;
                    offsetY = 0;
                    break;
                case 2:
                    glowAlpha = (int) (c.getAlpha() * intensity * 0.3f);
                    offsetX = 0;
                    offsetY = 1;
                    break;
                case 3:
                    glowAlpha = c.getAlpha();
                    offsetX = 0;
                    offsetY = 0;
                    break;
            }

            Color glowColor = new Color(c.getRed(), c.getGreen(), c.getBlue(), glowAlpha);
            NanoVGRenderer.drawTextWithFont(text, x + offsetX, y + offsetY, fontSize, glowColor, fontId);
        }
    }

    private int applyWaveColor(int baseColor, double waveValue) {
        Color base = new Color(baseColor, true);

        double wave = Math.pow(waveValue, 0.5);

        int r = (int) (base.getRed() + (255 - base.getRed()) * wave);
        int g = (int) (base.getGreen() + (255 - base.getGreen()) * wave);
        int b = (int) (base.getBlue() + (255 - base.getBlue()) * wave);

        return (base.getAlpha() << 24) | (r << 16) | (g << 8) | b;
    }

    private Color getThemeColor() {
        return new Color(100, 150, 255);
    }

    private Color getThemeColor(int variant) {
        return switch (variant) {
            case 1 -> new Color(200, 0, 255);
            case 2 -> new Color(100, 0, 255);
            default -> getThemeColor();
        };
    }

    private Color getCustomColor() {
        return customColor.getValue();
    }
}