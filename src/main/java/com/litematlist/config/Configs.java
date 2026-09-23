package com.litematlist.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import com.google.common.collect.ImmutableList;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.util.FileUtils;
import com.litematlist.LitematListMod;

public class Configs implements IConfigHandler
{
    private static final String CONFIG_FILE_NAME = LitematListMod.MOD_ID + ".json";

    public static class Generic
    {
        public static final ConfigBoolean SHOW_ESC_HINT = new ConfigBoolean(
                "showEscHint",
                true,
                "全屏预览时显示「按ESC退出」悬浮提示"
        ).apply(LitematListMod.MOD_ID + ".config.generic");

        public static final ConfigBoolean ALLOW_MANUAL_REORDER = new ConfigBoolean(
                "allowManualReorder",
                false,
                "允许玩家手动拖拽改变材料顺序"
        ).apply(LitematListMod.MOD_ID + ".config.generic");

        public static final ConfigBoolean ALWAYS_TAKE_OVER_MATERIAL_LIST = new ConfigBoolean(
                "alwaysTakeOverMaterialList",
                true,
                "始终接管 Litematica 的材料列表数据，供 TweakerMore 自动备货读取"
        ).apply(LitematListMod.MOD_ID + ".config.generic");

        public static final ConfigBoolean DISABLE_MATERIAL_HUD = new ConfigBoolean(
                "disableMaterialHud",
                false,
                "禁用投影自带材料列表 HUD 显示"
        ).apply(LitematListMod.MOD_ID + ".config.generic");

        public static final ConfigBoolean DISABLE_SYNCMATICA_MATERIAL_HUD = new ConfigBoolean(
                "disableSyncmaticaRevolutionMaterialHud",
                false,
                "禁用「共享原理图增强」(Syncmatica Revolution) 模组认领材料后显示的 HUD"
        ).apply(LitematListMod.MOD_ID + ".config.generic");

        public static final ConfigBoolean SHOW_UPLOAD_AREA_BUTTON = new ConfigBoolean(
                "showUploadAreaButton",
                true,
                "在主页面右上角显示「上传区域」按钮，用于对接 TweakerMore 自动备货和HUD显示列表"
        ).apply(LitematListMod.MOD_ID + ".config.generic");

        public static final ConfigBoolean ALLOW_MANUAL_HUD_REFRESH = new ConfigBoolean(
                "allowManualHudRefresh",
                true,
                "允许使用快捷键手动刷新材料列表HUD"
        ).apply(LitematListMod.MOD_ID + ".config.generic");

        public static final ConfigBoolean WHITELIST_DELETE_CONFIRM = new ConfigBoolean(
                "whitelistDeleteConfirm",
                true,
                "删除白名单物品时显示确认对话框"
        ).apply(LitematListMod.MOD_ID + ".config.generic");

        public static final ConfigBoolean TREE_ZOOM_CONTROL = new ConfigBoolean(
                "treeZoomControl",
                false,
                "在树状图界面右下角显示缩放档位控制（【-】百分比【+】），可整体缩放预览树状图"
        ).apply(LitematListMod.MOD_ID + ".config.generic");

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                SHOW_ESC_HINT,
                ALLOW_MANUAL_REORDER,
                ALWAYS_TAKE_OVER_MATERIAL_LIST,
                DISABLE_MATERIAL_HUD,
                DISABLE_SYNCMATICA_MATERIAL_HUD,
                SHOW_UPLOAD_AREA_BUTTON,
                ALLOW_MANUAL_HUD_REFRESH,
                WHITELIST_DELETE_CONFIRM,
                TREE_ZOOM_CONTROL
        );
    }

    /**
     * 「功能开关」配置页：原理图解析相关的布尔开关。
     * 全部使用 ConfigBooleanHotkeyed：每行同时显示 开关名称、开关、快捷键。
     */
    public static class FeatureToggles
    {
        public static final ConfigBooleanHotkeyed CALC_BUILDING_MATERIALS = new ConfigBooleanHotkeyed(
                "calcBuildingMaterials",
                true,
                "",
                "开启后解析原理图时，将方块（建筑材料）对应的物品计入材料列表\n关闭后材料列表只统计原理图的实体数据、容器数据以及实体容器数据"
        ).apply(LitematListMod.MOD_ID + ".config.feature_toggles");

        public static final ConfigBooleanHotkeyed CALC_SCHEMATIC_ENTITIES = new ConfigBooleanHotkeyed(
                "calcSchematicEntities",
                false,
                "",
                "开启后解析原理图时，将其中保存的实体（矿车、盔甲架、船、箱船等）对应的物品一并计入材料列表\n盔甲架穿戴/手持的物品（含纹饰模板与镶嵌材质）跟随本开关一并计入"
        ).apply(LitematListMod.MOD_ID + ".config.feature_toggles");

        public static final ConfigBooleanHotkeyed CALC_SCHEMATIC_ENTITY_CONTAINERS = new ConfigBooleanHotkeyed(
                "calcSchematicEntityContainers",
                false,
                "",
                "开启且「是否计算原理图实体」开启时，统计实体中容器的数据（漏斗矿车、箱船、驴背箱子等）"
        ).apply(LitematListMod.MOD_ID + ".config.feature_toggles");

        public static final ConfigBooleanHotkeyed CALCULATE_CONTAINER_DATA = new ConfigBooleanHotkeyed(
                "calculateContainerData",
                false,
                "",
                "开启后解析原理图时，将容器（箱子、木桶、漏斗、潜影盒等）内的物品一并计入材料列表"
        ).apply(LitematListMod.MOD_ID + ".config.generic");

        public static final ConfigBooleanHotkeyed SYNC_RENDER_LAYER = new ConfigBooleanHotkeyed(
                "syncRenderLayer",
                false,
                "",
                "开启后，「从投影同步」的材料列表在打开时会按投影当前渲染层规则（全部/单层/层级范围/上方所有/下方所有）重新解析原理图，只统计命中层级的方块"
        ).apply(LitematListMod.MOD_ID + ".config.generic");

        public static final ImmutableList<IConfigBase> TOGGLE_LIST = ImmutableList.of(
                CALC_BUILDING_MATERIALS,
                CALC_SCHEMATIC_ENTITIES,
                CALC_SCHEMATIC_ENTITY_CONTAINERS,
                CALCULATE_CONTAINER_DATA,
                SYNC_RENDER_LAYER
        );
    }

    /**
     * 「统计」配置页：按容器/实体类型细分是否统计（全部默认开启）。
     * 仅当对应类型开关为开、且所属类别开关（是否计算容器数据 /
     * 是否计算原理图实体容器数据 / 是否计算原理图实体）也为开时才统计。
     */
    public static class Statistics
    {
        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                cb("chest", "箱子（是否统计箱子内的物品）"),
                cb("trappedChest", "陷阱箱"),
                cb("shulkerBox", "潜影盒（含各种染色潜影盒）"),
                cb("largeChest", "大型箱子（相邻合并的大箱子）"),
                cb("largeTrappedChest", "大型陷阱箱"),
                cb("furnace", "熔炉"),
                cb("smoker", "烟熏炉"),
                cb("blastFurnace", "高炉"),
                cb("brewingStand", "酿造台"),
                cb("dispenser", "发射器"),
                cb("dropper", "投掷器"),
                cb("hopper", "漏斗"),
                cb("crafter", "合成器"),
                cb("barrel", "木桶"),
                cb("decoratedPot", "陶罐（含各种纹饰变种）"),
                cb("chiseledBookshelf", "雕文书架（按普通书计入）"),
                cb("lectern", "讲台（有书时按书与笔计入）"),
                cb("chestMinecart", "运输矿车"),
                cb("chestBoat", "运输船"),
                cb("hopperMinecart", "漏斗矿车"),
                cb("donkeyWithChest", "有箱子的驴"),
                cb("llamaWithChest", "有箱子的羊驼"),
                cb("armorStandArmor", "盔甲架上的盔甲（含纹饰模板与镶嵌材质）"),
                cb("snowGolem", "雪傀儡（召唤材料）"),
                cb("ironGolem", "铁傀儡（召唤材料）"),
                cb("wither", "凋零（召唤材料）"),
                cb("copperGolem", "铜傀儡（召唤材料）")
        );

        private static ConfigBoolean cb(String name, String comment)
        {
            return new ConfigBoolean(name, true, comment)
                    .apply(LitematListMod.MOD_ID + ".config.statistics");
        }

        /** 查询类型开关是否开启；未匹配到类型时返回 true（保持「类别开关为准」的旧行为）。 */
        public static boolean enabled(String key)
        {
            for (IConfigBase option : OPTIONS)
            {
                if (((ConfigBoolean) option).getName().equals(key))
                {
                    return ((ConfigBoolean) option).getBooleanValue();
                }
            }
            return true;
        }

        /** 全部统计开关状态的快照字符串（防抖检测用，任一开关变化即整体变化）。 */
        public static String snapshot()
        {
            StringBuilder sb = new StringBuilder(OPTIONS.size());
            for (IConfigBase option : OPTIONS)
            {
                sb.append(((ConfigBoolean) option).getBooleanValue() ? '1' : '0');
            }
            return sb.toString();
        }
    }

    public static void loadFromFile()
    {
        Path configFile = FileUtils.getConfigDirectoryAsPath().resolve(CONFIG_FILE_NAME);

        if (Files.exists(configFile) && Files.isReadable(configFile))
        {
            try
            {
                JsonElement element = JsonParser.parseString(Files.readString(configFile));

                if (element != null && element.isJsonObject())
                {
                    JsonObject root = element.getAsJsonObject();
                    ConfigUtils.readConfigBase(root, "Generic", Generic.OPTIONS);
                    ConfigUtils.readConfigBase(root, "FeatureToggles", FeatureToggles.TOGGLE_LIST);
                    ConfigUtils.readConfigBase(root, "Statistics", Statistics.OPTIONS);
                    ConfigUtils.readConfigBase(root, "Hotkeys", Hotkeys.HOTKEY_LIST);
                }
            }
            catch (Exception e)
            {
                LitematListMod.LOGGER.error("读取配置文件失败", e);
            }
        }
    }

    public static void saveToFile()
    {
        Path dir = FileUtils.getConfigDirectoryAsPath();

        if (Files.exists(dir) == false)
        {
            try { Files.createDirectories(dir); }
            catch (Exception e) { LitematListMod.LOGGER.error("创建配置目录失败", e); }
        }

        Path configFile = dir.resolve(CONFIG_FILE_NAME);
        JsonObject root = new JsonObject();

        ConfigUtils.writeConfigBase(root, "Generic", Generic.OPTIONS);
        ConfigUtils.writeConfigBase(root, "FeatureToggles", FeatureToggles.TOGGLE_LIST);
        ConfigUtils.writeConfigBase(root, "Statistics", Statistics.OPTIONS);
        ConfigUtils.writeConfigBase(root, "Hotkeys", Hotkeys.HOTKEY_LIST);

        try
        {
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(root);
            Files.writeString(configFile, json);
        }
        catch (Exception e)
        {
            LitematListMod.LOGGER.error("保存配置文件失败", e);
        }
    }

    @Override
    public void load()
    {
        loadFromFile();
    }

    @Override
    public void save()
    {
        saveToFile();
    }
}