package cc.silk.gui.theme;

import java.awt.Color;

/**
 * Simple container for GUI theme colors. Add more fields as needed.
 */
public final class Theme {
    public final String name;
    public final Color backgroundTop;
    public final Color backgroundBottom;
    public final Color containerBg;
    public final Color headerBg;
    public final Color sidebarBg;
    public final Color panelBg;
    public final Color panelAltBg;
    public final Color accent;
    public final Color text;
    public final Color muted;

    public Theme(String name,
                 Color backgroundTop,
                 Color backgroundBottom,
                 Color containerBg,
                 Color headerBg,
                 Color sidebarBg,
                 Color panelBg,
                 Color panelAltBg,
                 Color accent,
                 Color text,
                 Color muted) {
        this.name = name;
        this.backgroundTop = backgroundTop;
        this.backgroundBottom = backgroundBottom;
        this.containerBg = containerBg;
        this.headerBg = headerBg;
        this.sidebarBg = sidebarBg;
        this.panelBg = panelBg;
        this.panelAltBg = panelAltBg;
        this.accent = accent;
        this.text = text;
        this.muted = muted;
    }
}
