package cc.silk.gui.newgui.components;

import cc.silk.SilkClient;
import cc.silk.profiles.ProfileManager;
import cc.silk.utils.render.GuiGlowHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigPanel {
    private static final float SCROLL_SPEED = 20f;
    private static final int HEADER_HEIGHT = 16;
    private static final int CONFIG_ENTRY_HEIGHT = 14;
    private static final int MAX_VISIBLE_CONFIGS = 8;
    private static final Color HEADER_COLOR = new Color(28, 28, 32, 255);
    private static final Color PANEL_BG = new Color(18, 18, 22, 255);
    private static final Color ENTRY_HOVER_BG = new Color(40, 40, 45, 255);
    private static final Color TEXT_COLOR = new Color(240, 240, 245, 255);
    private static final float CORNER_RADIUS = 4f;
    private static final long DELETE_CONFIRM_TIMEOUT = 3000;
    private final int width;
    private final List<ConfigEntry> configs = new ArrayList<>();
    private float x, y;
    private boolean dragging = false;
    private float dragOffsetX, dragOffsetY;
    private float scrollOffset = 0f;
    private float targetScrollOffset = 0f;
    private String newConfigName = "";
    private boolean namingNewConfig = false;
    private String selectedConfig = null;
    private String deleteConfirmConfig = null;
    private long deleteConfirmTime = 0;

    public ConfigPanel(float x, float y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
        refreshConfigs();
    }

    public void refreshConfigs() {
        configs.clear();
        ProfileManager profileManager = SilkClient.INSTANCE.getProfileManager();
        if (profileManager != null) {
            File profileDir = profileManager.getProfileDir();
            if (profileDir.exists() && profileDir.isDirectory()) {
                File[] files = profileDir.listFiles((dir, name) -> name.endsWith(".json"));
                if (files != null) {
                    Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                    for (File file : files) {
                        String name = file.getName().replace(".json", "");
                        configs.add(new ConfigEntry(name));
                    }
                }
            }
        }
    }

    public void render(DrawContext context, int mouseX, int mouseY, float alpha, float scale, int centerX, int centerY) {
        int bgAlpha = (int) (255 * alpha);
        float transparency = cc.silk.module.modules.client.ClientSettingsModule.getGuiTransparency();

        int panelAlpha = (int) (255 * alpha * (1f - transparency));
        Color panelBg = new Color(PANEL_BG.getRed(), PANEL_BG.getGreen(), PANEL_BG.getBlue(), panelAlpha);
        Color headerBg = new Color(HEADER_COLOR.getRed(), HEADER_COLOR.getGreen(), HEADER_COLOR.getBlue(), panelAlpha);
        int textColor = new Color(TEXT_COLOR.getRed(), TEXT_COLOR.getGreen(), TEXT_COLOR.getBlue(), bgAlpha).getRGB();
        Color accentColor = cc.silk.module.modules.client.NewClickGUIModule.getAccentColor();

        int displayedConfigs = Math.min(configs.size() + 1, MAX_VISIBLE_CONFIGS);
        int totalHeight = HEADER_HEIGHT + (displayedConfigs * CONFIG_ENTRY_HEIGHT);

        int ix = (int) x;
        int iy = (int) y;
        int iwidth = width;

        context.fill(ix, iy, ix + iwidth, iy + totalHeight, panelBg.getRGB());
        context.fill(ix, iy, ix + iwidth, iy + HEADER_HEIGHT, headerBg.getRGB());

        int textX = ix + 6;
        int textY = iy + (HEADER_HEIGHT - 8) / 2;
        context.drawText(MinecraftClient.getInstance().textRenderer, "Configs", textX, textY, textColor, false);

        float contentY = y + HEADER_HEIGHT;
        scrollOffset += (targetScrollOffset - scrollOffset) * 0.3f;

        float currentY = contentY - scrollOffset;
        for (ConfigEntry config : configs) {
            renderConfigEntry(context, config, currentY, mouseX, mouseY, alpha, textColor, accentColor);
            currentY += CONFIG_ENTRY_HEIGHT;
        }

        renderNewConfigEntry(context, currentY, mouseX, mouseY, alpha, textColor, accentColor);
    }

    private void renderConfigEntry(DrawContext context, ConfigEntry config, float entryY, int mouseX, int mouseY, float alpha,
                                   int textColor, Color accentColor) {
        boolean hover = isMouseOver(mouseX, mouseY, x, entryY, width, CONFIG_ENTRY_HEIGHT);
        boolean selected = config.name.equals(selectedConfig);

        int iy = (int) entryY;
        int ix = (int) x;
        int iwidth = width;

        if (selected) {
            int selectedAlpha = (int) (50 * alpha);
            Color selectedBg = new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), selectedAlpha);
            context.fill(ix, iy, ix + iwidth, iy + CONFIG_ENTRY_HEIGHT, selectedBg.getRGB());
        } else if (hover) {
            int hoverAlpha = (int) (30 * alpha);
            Color hoverBg = new Color(ENTRY_HOVER_BG.getRed(), ENTRY_HOVER_BG.getGreen(), ENTRY_HOVER_BG.getBlue(), hoverAlpha);
            context.fill(ix, iy, ix + iwidth, iy + CONFIG_ENTRY_HEIGHT, hoverBg.getRGB());
        }

        String displayName = config.name;
        int entryTextX = ix + 4;
        int entryTextY = iy + (CONFIG_ENTRY_HEIGHT - 8) / 2;
        context.drawText(MinecraftClient.getInstance().textRenderer, displayName, entryTextX, entryTextY, textColor, false);

        config.bounds = new float[]{x, entryY, width, CONFIG_ENTRY_HEIGHT};
    }

    private void renderNewConfigEntry(DrawContext context, float entryY, int mouseX, int mouseY, float alpha,
                                      int textColor, Color accentColor) {
        boolean hover = isMouseOver(mouseX, mouseY, x, entryY, width, CONFIG_ENTRY_HEIGHT);

        int iy = (int) entryY;
        int ix = (int) x;
        int iwidth = width;

        if (hover || namingNewConfig) {
            int hoverAlpha = (int) (50 * alpha);
            Color hoverBg = new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), hoverAlpha);
            context.fill(ix, iy, ix + iwidth, iy + CONFIG_ENTRY_HEIGHT, hoverBg.getRGB());
        }

        String displayText = namingNewConfig ? (newConfigName.isEmpty() ? "_" : newConfigName) : "+ New";
        int newEntryTextX = ix + 4;
        int newEntryTextY = iy + (CONFIG_ENTRY_HEIGHT - 8) / 2;
        context.drawText(MinecraftClient.getInstance().textRenderer, displayText, newEntryTextX, newEntryTextY, textColor, false);
    }

    private boolean isMouseOver(double mouseX, double mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 && button != 1)
            return false;

        if (isMouseOver(mouseX, mouseY, x, y, width, HEADER_HEIGHT)) {
            if (button == 1) {
                refreshConfigs();
                return true;
            }
            if (button == 0) {
                dragging = true;
                x = Math.round(x);
                y = Math.round(y);
                dragOffsetX = (float) (mouseX - x);
                dragOffsetY = (float) (mouseY - y);
                return true;
            }
        }

        float contentY = y + HEADER_HEIGHT;
        float currentY = contentY - scrollOffset;

        for (ConfigEntry config : configs) {
            if (config.bounds != null && isMouseOver(mouseX, mouseY,
                    config.bounds[0], config.bounds[1], config.bounds[2], config.bounds[3])) {

                if (button == 0) {
                    selectedConfig = config.name;
                    deleteConfirmConfig = null;
                }
                return true;
            }
            currentY += CONFIG_ENTRY_HEIGHT;
        }

        if (isMouseOver(mouseX, mouseY, x, currentY, width, CONFIG_ENTRY_HEIGHT)) {
            if (namingNewConfig && !newConfigName.isEmpty()) {
                ProfileManager pm = SilkClient.INSTANCE.getProfileManager();
                if (pm != null) {
                    pm.saveProfile(newConfigName);
                    refreshConfigs();
                }
                newConfigName = "";
                namingNewConfig = false;
            } else {
                namingNewConfig = true;
                newConfigName = "";
            }
            return true;
        }

        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging) {
            x = Math.round((float) (mouseX - dragOffsetX));
            y = Math.round((float) (mouseY - dragOffsetY));
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int displayedConfigs = Math.min(configs.size() + 1, MAX_VISIBLE_CONFIGS);
        int totalHeight = HEADER_HEIGHT + (displayedConfigs * CONFIG_ENTRY_HEIGHT);

        if (isMouseOver(mouseX, mouseY, x, y, width, totalHeight)) {
            if (configs.size() + 1 > MAX_VISIBLE_CONFIGS) {
                float maxScroll = Math.max(0, (configs.size() + 1 - MAX_VISIBLE_CONFIGS) * CONFIG_ENTRY_HEIGHT);
                targetScrollOffset = Math.max(0,
                        Math.min(maxScroll, targetScrollOffset - (float) amount * SCROLL_SPEED));
            }
            return true;
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (namingNewConfig) {
            if (keyCode == 259) {
                if (!newConfigName.isEmpty()) {
                    newConfigName = newConfigName.substring(0, newConfigName.length() - 1);
                }
                return true;
            } else if (keyCode == 257 || keyCode == 335) {
                if (!newConfigName.isEmpty()) {
                    ProfileManager pm = SilkClient.INSTANCE.getProfileManager();
                    if (pm != null) {
                        pm.saveProfile(newConfigName);
                        refreshConfigs();
                    }
                    newConfigName = "";
                }
                namingNewConfig = false;
                return true;
            } else if (keyCode == 256) {
                namingNewConfig = false;
                newConfigName = "";
                return true;
            }
        }
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (namingNewConfig) {
            if (Character.isLetterOrDigit(chr) || chr == '_' || chr == '-') {
                newConfigName += chr;
                return true;
            }
        }
        return false;
    }

    public boolean isDragging() {
        return dragging;
    }

    public void update(float deltaTime) {
        if (deleteConfirmConfig != null && System.currentTimeMillis() - deleteConfirmTime >= DELETE_CONFIRM_TIMEOUT) {
            deleteConfirmConfig = null;
        }
    }

    public void renderGlow(DrawContext context, float alpha, float scale, int centerX, int centerY) {
        int displayedConfigs = Math.min(configs.size() + 1, MAX_VISIBLE_CONFIGS);
        int totalHeight = HEADER_HEIGHT + (displayedConfigs * CONFIG_ENTRY_HEIGHT);
        
        float transformedX = (x - centerX) * scale + centerX;
        float transformedY = (y - centerY) * scale + centerY;
        float transformedWidth = width * scale;
        float transformedHeight = totalHeight * scale;
        
        GuiGlowHelper.drawGuiGlow(context, transformedX, transformedY, transformedWidth, transformedHeight, CORNER_RADIUS * scale);
    }
    
    private static class ConfigEntry {
        String name;
        float[] bounds;

        ConfigEntry(String name) {
            this.name = name;
        }
    }
}
