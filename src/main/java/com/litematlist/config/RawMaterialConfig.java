package com.litematlist.config;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import java.util.*;

/**
 * 原材料分析配置：白名单和配方优先级。
 */
public class RawMaterialConfig {

    /**
     * 配方类型配置：每个配方类型有名称、映射的 String 列表、启用状态。
     */
    public static class RecipeTypeConfig {
        public final String key;
        public final String[] mappedTypes;
        public boolean enabled;

        public RecipeTypeConfig(String key, boolean defaultEnabled, String... mappedTypes) {
            this.key = key;
            this.mappedTypes = mappedTypes;
            this.enabled = defaultEnabled;
        }

        public String getDisplayName() {
            return switch (key) {
                case "stonecutter" -> "切石机";
                case "crafting" -> "工作台";
                case "furnace" -> "熔炉";
                case "smithing" -> "锻造台";
                default -> key;
            };
        }
    }

    /** 默认配方类型列表（按优先级排序） */
    private static final List<RecipeTypeConfig> DEFAULT_RECIPE_TYPES = List.of(
        new RecipeTypeConfig("stonecutter", true, "stonecutter"),
        new RecipeTypeConfig("crafting", true, "crafting_shaped", "crafting_shapeless"),
        new RecipeTypeConfig("furnace", true, "smelting"),
        new RecipeTypeConfig("smithing", true, "smithing_transform")
    );

    /** 当前配方类型列表（按优先级排序，可修改启用状态和顺序） */
    public static final List<RecipeTypeConfig> recipeTypeOrder = new ArrayList<>();
    static {
        for (RecipeTypeConfig def : DEFAULT_RECIPE_TYPES) {
            recipeTypeOrder.add(new RecipeTypeConfig(def.key, def.enabled, def.mappedTypes));
        }
    }

    /** 获取当前启用的配方类型映射列表（按优先级，扁平化） */
    public static List<String> getEnabledRecipeTypes() {
        List<String> result = new ArrayList<>();
        for (RecipeTypeConfig config : recipeTypeOrder) {
            if (config.enabled) {
                for (String t : config.mappedTypes) {
                    result.add(t);
                }
            }
        }
        return result;
    }

    /**
     * 获取当前启用的配方类型分组列表（按优先级）。
     * 每个分组对应一个 RecipeTypeConfig，其 mappedTypes 作为一组传递给 getDisplayEntryFromRecipeBook。
     * 参考 Litematica MaterialListJsonEntry.build()——将多种类型作为 List 传入。
     */
    public static List<List<String>> getEnabledRecipeTypeGroups() {
        List<List<String>> result = new ArrayList<>();
        for (RecipeTypeConfig config : recipeTypeOrder) {
            if (config.enabled) {
                result.add(Arrays.asList(config.mappedTypes));
            }
        }
        return result;
    }

    /** 默认白名单：溯源到这些物品时停止继续分解 */
    public static final Set<Item> DEFAULT_WHITELIST = Set.of(
        // 矿物
        Items.COAL, Items.IRON_INGOT, Items.GOLD_INGOT, Items.REDSTONE,
        Items.EMERALD, Items.LAPIS_LAZULI, Items.DIAMOND, Items.NETHERITE_INGOT,
        // 粗矿
        Items.RAW_IRON, Items.RAW_GOLD, Items.RAW_COPPER,
        // 建筑
        Items.SNOW_BLOCK, Items.WHITE_WOOL, Items.GLASS, Items.STONE, Items.CLAY,
        Items.DIORITE, Items.ANDESITE, Items.GRANITE, Items.QUARTZ, Items.TUFF,
        Items.COBBLED_DEEPSLATE, Items.BLACKSTONE,
        // 铜块（4种氧化状态，切制铜块变种不在白名单中，可继续溯源）
        Items.COPPER_BLOCK, Items.EXPOSED_COPPER, Items.WEATHERED_COPPER, Items.OXIDIZED_COPPER,
        // 4种铁轨
        Items.RAIL, Items.POWERED_RAIL, Items.DETECTOR_RAIL, Items.ACTIVATOR_RAIL,
        // 16色地毯
        Items.WHITE_CARPET, Items.ORANGE_CARPET, Items.MAGENTA_CARPET, Items.LIGHT_BLUE_CARPET,
        Items.YELLOW_CARPET, Items.LIME_CARPET, Items.PINK_CARPET, Items.GRAY_CARPET,
        Items.LIGHT_GRAY_CARPET, Items.CYAN_CARPET, Items.PURPLE_CARPET, Items.BLUE_CARPET,
        Items.BROWN_CARPET, Items.GREEN_CARPET, Items.RED_CARPET, Items.BLACK_CARPET,
        // 功能
        Items.SHULKER_BOX, Items.BONE_MEAL, Items.STICK, Items.WHEAT,
        Items.GLOWSTONE, Items.ENDER_EYE, Items.BOOK, Items.AMETHYST_SHARD,
        Items.RESIN_CLUMP,
        // 新增：粘液块、蜂蜜块、箱子、铁砧、木炭（工作台和熔炉不在白名单中，可继续溯源至木板和圆石）
        Items.SLIME_BLOCK, Items.HONEY_BLOCK,
        Items.CHEST, Items.ANVIL, Items.CHARCOAL,
        // 16种染料
        Items.WHITE_DYE, Items.ORANGE_DYE, Items.MAGENTA_DYE, Items.LIGHT_BLUE_DYE,
        Items.YELLOW_DYE, Items.LIME_DYE, Items.PINK_DYE, Items.GRAY_DYE,
        Items.LIGHT_GRAY_DYE, Items.CYAN_DYE, Items.PURPLE_DYE, Items.BLUE_DYE,
        Items.BROWN_DYE, Items.GREEN_DYE, Items.RED_DYE, Items.BLACK_DYE,
        // 19种盔甲纹饰模板
        Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE,
        Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE,
        Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE,
        Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE,
        Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE,
        Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE,
        Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE,
        Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE,
        Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE,
        Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE,
        Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE,
        Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE,
        Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE,
        Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE,
        Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE,
        Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE,
        Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE,
        Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE,
        Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE
    );

    /** 当前白名单（可被玩家修改） */
    public static Set<Item> currentWhitelist = new HashSet<>(DEFAULT_WHITELIST);

    /** 最大递归深度，防止无限递归 */
    public static final int MAX_RECURSION_DEPTH = 20;
}