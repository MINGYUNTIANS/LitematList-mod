package com.litematlist.config;

import fi.dy.masa.malilib.util.StringUtils;

public enum ConfigGuiTab
{
    GENERIC ("litematlist.gui.button.config_gui.generic"),
    TOGGLES ("litematlist.gui.button.config_gui.toggles"),
    STATS ("litematlist.gui.button.config_gui.stats"),
    COLORS ("litematlist.gui.button.config_gui.colors"),
    HOTKEYS ("litematlist.gui.button.config_gui.hotkeys");

    private final String translationKey;

    ConfigGuiTab(String translationKey)
    {
        this.translationKey = translationKey;
    }

    public String getDisplayName()
    {
        return StringUtils.translate(this.translationKey);
    }
}