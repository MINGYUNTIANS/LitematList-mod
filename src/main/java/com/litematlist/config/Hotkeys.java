package com.litematlist.config;

import java.util.List;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import com.litematlist.LitematListMod;
//#endif

public class Hotkeys
{
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final String HOTKEYS_KEY = LitematListMod.MOD_ID + ".config.hotkeys";
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
    public static final ConfigHotkey OPEN_MAIN_GUI = new ConfigHotkey(
            "openMainGui",
            "L,C",
            KeybindSettings.RELEASE_EXCLUSIVE,
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            "litematlist.config.hotkeys.comment.openMainGui",
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            "打开 LitematList 材料列表",
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            "litematlist.config.hotkeys.name.openMainGui"
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    ).apply(HOTKEYS_KEY);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            "打开 LitematList 材料列表"
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    );
//#endif

    public static final ConfigHotkey OPEN_CONFIG_GUI = new ConfigHotkey(
            "openConfigGui",
            "V,C",
            KeybindSettings.RELEASE_EXCLUSIVE,
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            "litematlist.config.hotkeys.comment.openConfigGui",
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            "打开 LitematList 配置",
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            "litematlist.config.hotkeys.name.openConfigGui"
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    ).apply(HOTKEYS_KEY);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            "打开 LitematList 配置"
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    );
//#endif

    public static final ConfigHotkey REFRESH_MATERIAL_HUD = new ConfigHotkey(
            "refreshMaterialHud",
            "",
            KeybindSettings.RELEASE_EXCLUSIVE,
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            "litematlist.config.hotkeys.comment.refreshMaterialHud",
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            "HUD-手动刷新列表",
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            "litematlist.config.hotkeys.name.refreshMaterialHud"
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    ).apply(HOTKEYS_KEY);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            "HUD-手动刷新列表"
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    );
//#endif

    public static final ConfigHotkey HUD_NEXT_PAGE = new ConfigHotkey(
            "hudNextPage",
            "",
            KeybindSettings.RELEASE_EXCLUSIVE,
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            "litematlist.config.hotkeys.comment.hudNextPage",
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            "HUD-下一页",
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            "litematlist.config.hotkeys.name.hudNextPage"
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    ).apply(HOTKEYS_KEY);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            "HUD-下一页"
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    );
//#endif

    public static final ConfigHotkey HUD_PREV_PAGE = new ConfigHotkey(
            "hudPrevPage",
            "",
            KeybindSettings.RELEASE_EXCLUSIVE,
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            "litematlist.config.hotkeys.comment.hudPrevPage",
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            "HUD-上一页",
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            "litematlist.config.hotkeys.name.hudPrevPage"
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    ).apply(HOTKEYS_KEY);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            "HUD-上一页"
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    );
//#endif

    public static final ConfigHotkey QUICK_OPEN_HUD_CONFIG = new ConfigHotkey(
            "quickOpenHudConfig",
            "",
            KeybindSettings.RELEASE_EXCLUSIVE,
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            "litematlist.config.hotkeys.comment.quickOpenHudConfig",
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            "快捷打开HUD界面设置",
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            "litematlist.config.hotkeys.name.quickOpenHudConfig"
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    ).apply(HOTKEYS_KEY);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            "快捷打开HUD界面设置"
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    );
//#endif

    public static final ConfigHotkey TOGGLE_HUD = new ConfigHotkey(
            "toggleHud",
            "",
            KeybindSettings.RELEASE_EXCLUSIVE,
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            "litematlist.config.hotkeys.comment.toggleHud",
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            "上传的材料列表HUD开关",
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            "litematlist.config.hotkeys.name.toggleHud"
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    ).apply(HOTKEYS_KEY);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            "上传的材料列表HUD开关"
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    );
//#endif

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