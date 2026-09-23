package com.litematlist.config;

import java.util.List;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import com.litematlist.LitematListMod;

public class Hotkeys
{
    private static final String HOTKEYS_KEY = LitematListMod.MOD_ID + ".config.hotkeys";

    public static final ConfigHotkey OPEN_MAIN_GUI = new ConfigHotkey(
            "openMainGui",
            "L,C",
            KeybindSettings.RELEASE_EXCLUSIVE,
            "litematlist.config.hotkeys.comment.openMainGui",
            "打开 LitematList 材料列表",
            "litematlist.config.hotkeys.name.openMainGui"
    ).apply(HOTKEYS_KEY);

    public static final ConfigHotkey OPEN_CONFIG_GUI = new ConfigHotkey(
            "openConfigGui",
            "V,C",
            KeybindSettings.RELEASE_EXCLUSIVE,
            "litematlist.config.hotkeys.comment.openConfigGui",
            "打开 LitematList 配置",
            "litematlist.config.hotkeys.name.openConfigGui"
    ).apply(HOTKEYS_KEY);

    public static final ConfigHotkey REFRESH_MATERIAL_HUD = new ConfigHotkey(
            "refreshMaterialHud",
            "",
            KeybindSettings.RELEASE_EXCLUSIVE,
            "litematlist.config.hotkeys.comment.refreshMaterialHud",
            "手动刷新材料列表HUD",
            "litematlist.config.hotkeys.name.refreshMaterialHud"
    ).apply(HOTKEYS_KEY);

    public static final ConfigHotkey HUD_NEXT_PAGE = new ConfigHotkey(
            "hudNextPage",
            "",
            KeybindSettings.RELEASE_EXCLUSIVE,
            "litematlist.config.hotkeys.comment.hudNextPage",
            "HUD-下一页",
            "litematlist.config.hotkeys.name.hudNextPage"
    ).apply(HOTKEYS_KEY);

    public static final ConfigHotkey HUD_PREV_PAGE = new ConfigHotkey(
            "hudPrevPage",
            "",
            KeybindSettings.RELEASE_EXCLUSIVE,
            "litematlist.config.hotkeys.comment.hudPrevPage",
            "HUD-上一页",
            "litematlist.config.hotkeys.name.hudPrevPage"
    ).apply(HOTKEYS_KEY);

    public static final ConfigHotkey QUICK_OPEN_HUD_CONFIG = new ConfigHotkey(
            "quickOpenHudConfig",
            "",
            KeybindSettings.RELEASE_EXCLUSIVE,
            "litematlist.config.hotkeys.comment.quickOpenHudConfig",
            "快捷打开HUD界面设置",
            "litematlist.config.hotkeys.name.quickOpenHudConfig"
    ).apply(HOTKEYS_KEY);

    public static final ConfigHotkey TOGGLE_HUD = new ConfigHotkey(
            "toggleHud",
            "",
            KeybindSettings.RELEASE_EXCLUSIVE,
            "litematlist.config.hotkeys.comment.toggleHud",
            "上传的材料列表HUD开关",
            "litematlist.config.hotkeys.name.toggleHud"
    ).apply(HOTKEYS_KEY);

    public static final List<ConfigHotkey> HOTKEY_LIST = ImmutableList.of(
            OPEN_MAIN_GUI,
            OPEN_CONFIG_GUI,
            REFRESH_MATERIAL_HUD,
            HUD_NEXT_PAGE,
            HUD_PREV_PAGE,
            QUICK_OPEN_HUD_CONFIG,
            TOGGLE_HUD
    );
}