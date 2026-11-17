package com.litematlist;

import com.litematlist.LitematicReader.MaterialEntry;
import com.litematlist.config.RawMaterialConfig;
//#if MC == 11904
//$$import com.litematlist.gui.MaterialDetailScreen;
//#endif
//#if MC == 12101
//$$import com.litematlist.gui.MaterialDetailScreen;
//#endif
import com.litematlist.gui.MaterialDataStore;
//#if MC == 11701
//$$import com.litematlist.gui.MaterialDetailScreen;
//#endif
//#if MC == 12108
//$$import com.litematlist.gui.MaterialDetailScreen;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
import fi.dy.masa.malilib.util.game.RecipeBookUtils;
//#endif
//#if MC == 12101 || MC == 12104
//$$import com.litematlist.RecipeBookUtils;
//#endif
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.recipe.*;
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$import net.minecraft.recipe.Ingredient;
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.recipe.display.*;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.registry.Registries;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.registry.RegistryWrapper;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.registry.entry.RegistryEntry;
//#endif
import net.minecraft.util.Identifier;
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.util.context.ContextParameterMap;
//#endif
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.math.Fraction;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import org.apache.commons.lang3.tuple.Pair;
//#endif

import java.util.*;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
/**
 * 配方树分析器：从成品材料列表递归溯源，构建配方树结构。
 * 基于 RawMaterialAnalyzer 逻辑，但输出 RecipeTreeNode 树而非扁平列表。
 */
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$import net.minecraft.recipe.Recipe;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$import net.minecraft.recipe.RecipeManager;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$import net.minecraft.recipe.RecipeType;
//#endif
//#if MC == 11701
//$$import net.minecraft.util.registry.Registry;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
public class RawMaterialTreeAnalyzer {
//#if MC == 11701
//$$
//#endif
//#if MC == 11701
//$$    /** 1.17.1 兼容：RegistryEntry 类不存在（1.18 才引入），直接用 Item 作为标识对象。 */
//#endif
//#if MC == 11701
//$$    private static Item getEntry(Item item) {
//#endif
//#if MC == 11701
//$$        return item;
//#endif
//#if MC == 11701
//$$    }
//#endif
//#if MC == 11701
//$$
//#endif
//#if MC == 11701
//$$    /** 1.17.1 兼容：由 ItemStack 取 Item。 */
//#endif
//#if MC == 11701
//$$    private static Item getEntry(ItemStack stack) {
//#endif
//#if MC == 11701
//$$        return stack.getItem();
//#endif
//#if MC == 11701
//$$    }
//#endif
//#if MC == 11701
//$$
//#endif
//#if MC == 11701
//$$    /** 1.17.1 兼容：陶罐物品（1.20.3+ 才新增；本版本不存在，取回 null/AIR）。 */
//#endif
//#if MC == 11701
//$$    private static Item decoratedPotItem() {
//#endif
//#if MC == 11701
//$$        Identifier id = Identifier.tryParse("minecraft:decorated_pot");
//#endif
//#if MC == 11701
//$$        return id == null ? null : Registry.ITEM.get(id);
//#endif
//#if MC == 11701
//$$    }
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 通配成品物品ID列表：这些物品的配方材料可替换为同类物品 */
//#endif
    private static final Set<String> WILDCARD_ITEM_IDS = Set.of(
        "minecraft:note_block", "minecraft:jukebox",
        "minecraft:cartography_table", "minecraft:fletching_table", "minecraft:smithing_table",
        "minecraft:bookshelf",
        "minecraft:piston", "minecraft:sticky_piston", "minecraft:comparator",
        "minecraft:repeater", "minecraft:daylight_detector",
        "minecraft:wooden_sword", "minecraft:wooden_pickaxe", "minecraft:wooden_axe",
        "minecraft:wooden_shovel", "minecraft:wooden_hoe",
        "minecraft:item_frame",
//#if MC == 12110
        // 画（配方中的羊毛为 #minecraft:wool 标签，默认白色羊毛且可通配替换）
//#endif
//#if MC == 12110
        "minecraft:painting",
//#endif
//#if MC == 12110
        // 蛋糕（配方中的鸡蛋为 #minecraft:eggs 标签，1.21.5+ 共 3 种鸡蛋可通配互替）
//#endif
//#if MC == 12110
        "minecraft:cake",
//#endif
        "minecraft:stone_sword", "minecraft:stone_pickaxe", "minecraft:stone_axe",
        "minecraft:stone_shovel", "minecraft:stone_hoe",
        "minecraft:chiseled_bookshelf",
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 箱子、工作台（子材料木板应为通配可替换）
//#endif
        "minecraft:chest", "minecraft:trapped_chest", "minecraft:crafting_table",
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 木桶、织布机、熔炉、讲台（木板/半砖通配）
//#endif
        "minecraft:barrel", "minecraft:loom", "minecraft:furnace", "minecraft:lectern",
        // 烟熏炉（配方中的原木为 #minecraft:logs 标签，任意原木皆可）
        "minecraft:smoker",
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 16色羊毛（溯源为染料+通配白色羊毛）
//#endif
        "minecraft:white_wool", "minecraft:orange_wool", "minecraft:magenta_wool",
        "minecraft:light_blue_wool", "minecraft:yellow_wool", "minecraft:lime_wool",
        "minecraft:pink_wool", "minecraft:gray_wool", "minecraft:light_gray_wool",
        "minecraft:cyan_wool", "minecraft:purple_wool", "minecraft:blue_wool",
        "minecraft:brown_wool", "minecraft:green_wool", "minecraft:red_wool",
        "minecraft:black_wool",
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 16色床（溯源为染料+通配白色床）
//#endif
        "minecraft:white_bed", "minecraft:orange_bed", "minecraft:magenta_bed",
        "minecraft:light_blue_bed", "minecraft:yellow_bed", "minecraft:lime_bed",
        "minecraft:pink_bed", "minecraft:gray_bed", "minecraft:light_gray_bed",
        "minecraft:cyan_bed", "minecraft:purple_bed", "minecraft:blue_bed",
        "minecraft:brown_bed", "minecraft:green_bed", "minecraft:red_bed",
        "minecraft:black_bed",
        // 17色收纳袋（无色 + 16色，与羊毛同款染色通配，溯源为染料+通配无色收纳袋）
        "minecraft:bundle",
        "minecraft:white_bundle", "minecraft:orange_bundle", "minecraft:magenta_bundle",
        "minecraft:light_blue_bundle", "minecraft:yellow_bundle", "minecraft:lime_bundle",
        "minecraft:pink_bundle", "minecraft:gray_bundle", "minecraft:light_gray_bundle",
        "minecraft:cyan_bundle", "minecraft:purple_bundle", "minecraft:blue_bundle",
        "minecraft:brown_bundle", "minecraft:green_bundle", "minecraft:red_bundle",
        "minecraft:black_bundle",
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 枯叶堆（熔炉烧制任意树叶，默认橡木树叶，通配所有树叶）
//#endif
        "minecraft:leaf_litter",
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // TNT（沙子/砂砾通配）
//#endif
        "minecraft:tnt",
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        // 画（配方中的羊毛为 #minecraft:wool 标签，默认白色羊毛且可通配替换）
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        "minecraft:painting",
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$        // 蛋糕（配方中的鸡蛋为 #minecraft:eggs 标签）
//#elseif MC == 12108
//$$        // 蛋糕（配方中的鸡蛋为 #minecraft:eggs 标签，1.21.5+ 共 3 种鸡蛋可通配互替）
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        "minecraft:cake",
//#endif
//#if MC == 12111
//$$        // 画（配方中的羊毛为 #minecraft:wool 标签，默认白色羊毛且可通配替换）
//#endif
//#if MC == 12111
//$$        "minecraft:painting",
//#endif
//#if MC == 12111
//$$        // 蛋糕（配方中的鸡蛋为 #minecraft:eggs 标签，1.21.5+ 共 3 种鸡蛋可通配互替）
//#endif
//#if MC == 12111
//$$        "minecraft:cake",
//#endif
        // 发射器、投掷器、侦测器（圆石通配，同熔炉）
        "minecraft:dispenser", "minecraft:dropper", "minecraft:observer",
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 火把、灵魂火把、铜火把（煤炭/木炭通配，铜火把仅1.21.10+）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 火把、灵魂火把（煤炭/木炭通配）
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        "minecraft:torch", "minecraft:soul_torch", "minecraft:copper_torch",
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        "minecraft:torch", "minecraft:soul_torch",
//#endif
        // 苔石、苔石砖（圆石+苔藓或圆石+藤蔓，默认苔藓，苔藓/藤蔓可通配替换）
        "minecraft:mossy_cobblestone", "minecraft:mossy_stone_bricks",
        // 木棍（配方中的木板为 #minecraft:planks 标签，任意木板皆可）
        "minecraft:stick",
        // 营火、灵魂营火（配方中的煤炭/木炭通配，同火把）
        "minecraft:campfire", "minecraft:soul_campfire"
    );

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 通配原材料ID列表：成品通配物品的子材料中，这些物品可替换并显示齿轮图标 */
//#endif
    private static final Set<String> WILDCARD_MATERIAL_IDS = Set.of(
//#if MC == 12108
//$$        "minecraft:egg", "minecraft:brown_egg", "minecraft:blue_egg",
//#endif
//#if MC == 12111
//$$        "minecraft:egg", "minecraft:brown_egg", "minecraft:blue_egg",
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 木板变种（可替换为任意木头）
//#endif
        "minecraft:oak_planks", "minecraft:spruce_planks", "minecraft:birch_planks",
        "minecraft:jungle_planks", "minecraft:acacia_planks", "minecraft:dark_oak_planks",
        "minecraft:mangrove_planks", "minecraft:cherry_planks", "minecraft:bamboo_planks",
        "minecraft:crimson_planks", "minecraft:warped_planks",
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 木板半砖变种（可替换为对应木头，用于讲台、木桶等）
//#endif
        "minecraft:oak_slab", "minecraft:spruce_slab", "minecraft:birch_slab",
        "minecraft:jungle_slab", "minecraft:acacia_slab", "minecraft:dark_oak_slab",
        "minecraft:mangrove_slab", "minecraft:cherry_slab", "minecraft:bamboo_slab",
        "minecraft:crimson_slab", "minecraft:warped_slab",
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 圆石（可替换为黑石、深板岩圆石等）
//#endif
        "minecraft:cobblestone", "minecraft:blackstone", "minecraft:cobbled_deepslate",
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 羊毛/床/收纳袋支持染色通配（白色/无色为默认基底）
//#endif
        "minecraft:white_wool", "minecraft:white_bed", "minecraft:bundle",
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 沙子/砂砾互替（用于TNT等配方）
//#endif
        "minecraft:sand", "minecraft:gravel",
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 树叶（用于枯叶堆等配方，默认橡木树叶）
//#endif
        "minecraft:oak_leaves", "minecraft:spruce_leaves", "minecraft:birch_leaves",
        "minecraft:jungle_leaves", "minecraft:acacia_leaves", "minecraft:dark_oak_leaves",
        "minecraft:mangrove_leaves", "minecraft:cherry_leaves", "minecraft:azalea_leaves",
        "minecraft:flowering_azalea_leaves",
        // 煤炭/木炭互替（用于火把等配方）
        "minecraft:coal", "minecraft:charcoal",
        // 苔藓/藤蔓互替（用于苔石、苔石砖配方）
//#if MC == 12111
//$$        "minecraft:moss_block", "minecraft:vine"
//#endif
//#if MC == 12110
        "minecraft:moss_block", "minecraft:vine",
//#endif
//#if MC == 12110
        // 鸡蛋（用于蛋糕配方，1.21.5+ 共 3 种，互相通配，默认普通鸡蛋）
//#endif
//#if MC == 12110
        "minecraft:egg", "minecraft:brown_egg", "minecraft:blue_egg"
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        "minecraft:moss_block", "minecraft:vine"
//#endif
    );

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 判断指定物品ID是否为通配成品 */
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public static boolean isWildcard(String itemId) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return WILDCARD_ITEM_IDS.contains(itemId);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 判断指定物品ID是否为通配原材料（可替换） */
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public static boolean isWildcardMaterial(String itemId) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return WILDCARD_MATERIAL_IDS.contains(itemId) || isLogFamily(itemId) || isPlanksFamily(itemId);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    public static boolean isWildcard(String itemId) { return WILDCARD_ITEM_IDS.contains(itemId); }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    public static boolean isWildcardMaterial(String itemId) { return WILDCARD_MATERIAL_IDS.contains(itemId) || isLogFamily(itemId) || isPlanksFamily(itemId); }
//#endif

    /**
     * 判断物品是否为「原木类」物品（原木/木头/菌柄/菌核及其去皮变种）。
     * 烟熏炉等配方中的原木使用 {@code #minecraft:logs} 标签，任意原木皆可，故同样视为可替换的通配原材料。
     */
    public static boolean isLogFamily(String itemId) {
        if (itemId == null || !itemId.startsWith("minecraft:")) return false;
        String path = itemId.substring("minecraft:".length());
        return path.endsWith("_log") || path.endsWith("_wood")
                || path.endsWith("_stem") || path.endsWith("_hyphae");
    }

    /**
     * 判断物品是否为「木板类」物品（按后缀匹配，含未来新增木种如苍白橡木木板）。
     * 木棍等配方中的木板使用 {@code #minecraft:planks} 标签，任意木板皆可，故同样视为可替换的通配原材料。
     */
    public static boolean isPlanksFamily(String itemId) {
        if (itemId == null || !itemId.startsWith("minecraft:")) return false;
        return itemId.endsWith("_planks");
    }

    private final Set<Item> whitelist;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private final List<List<RecipeBookUtils.Type>> recipePriority;
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    private final List<List<String>> recipePriority;
//#endif
    private int depth = 0;
    private int maxDepth = 20;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 溯源记录：当前顶层条目溯源路径中出现过的物品，防止循环溯源 */
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private Set<String> visited = new HashSet<>();
//#endif

    /** 「RecipeManager 兜底查找失败」日志的最近输出时间（30 秒节流，防止逐物品溯源时日志爆炸） */
    private static long lastRecipeFallbackFailLogTime = 0;
    private static final long RECIPE_FALLBACK_FAIL_LOG_COOLDOWN = 30000;

    /** 兜底查找失败日志的频率限制：同一 30 秒内只输出一条 */
    private static void logRecipeFallbackFailure(Exception e) {
        long now = System.currentTimeMillis();
        if (now - lastRecipeFallbackFailLogTime < RECIPE_FALLBACK_FAIL_LOG_COOLDOWN) {
            return;
        }
        lastRecipeFallbackFailLogTime = now;
        LitematListMod.LOGGER.warn("RecipeManager 兜底查找失败: {}", e.getMessage());
    }

    /** 反射备用方案探测失败（预期内的分支回退）—— 仅 debug 记录，避免遍历所有物品时日志爆炸 */
    private static void logReflectionProbeFailure(Exception e) {
        LitematListMod.LOGGER.debug("[RawMaterialTreeAnalyzer] 反射备用方案不可用: {}", e.toString());
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 用户偏好的配方类型（用于切换合成方式） */
//#endif
    private static final Map<String, String> PREFERRED_RECIPE_TYPES = new HashMap<>();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 通配物品替换映射：原始物品ID → 替换后物品ID（全局同步替换） */
//#endif
    private static final Map<String, String> WILDCARD_REPLACEMENTS = new HashMap<>();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 通配物品按上下文替换映射：parentItemId:childItemId → 替换后物品ID（仅替换当前子合成表） */
//#endif
    private static final Map<String, String> PER_CONTEXT_REPLACEMENTS = new HashMap<>();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 设置全局通配物品替换（支持链式替换，自动查找原始 key） */
//#endif
    public static void setWildcardReplacement(String originalId, String newId) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 如果 originalId 本身是某个替换的值，找到真正的原始 key
//#endif
        String realKey = originalId;
        for (Map.Entry<String, String> e : WILDCARD_REPLACEMENTS.entrySet()) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (e.getValue().equals(originalId)) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                realKey = e.getKey();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                break;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (e.getValue().equals(originalId)) { realKey = e.getKey(); break; }
//#endif
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 检查 PER_CONTEXT_REPLACEMENTS（上下文替换），确保同步开时能正确追溯原始ID
//#endif
        if (realKey.equals(originalId)) {
            for (Map.Entry<String, String> e : PER_CONTEXT_REPLACEMENTS.entrySet()) {
                if (e.getValue().equals(originalId)) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    realKey = e.getKey().substring(e.getKey().indexOf(':') + 1);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    break;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    realKey = e.getKey().substring(e.getKey().indexOf(58) + 1); break;
//#endif
                }
            }
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 清除所有值等于 originalId 或 realKey 的上下文替换（全局替换应覆盖上下文替换）
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        final String finalOriginalId = originalId;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        final String finalRealKey = realKey;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        PER_CONTEXT_REPLACEMENTS.entrySet().removeIf(e -> e.getValue().equals(finalOriginalId) || e.getValue().equals(finalRealKey));
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (newId == null || newId.equals(realKey)) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            WILDCARD_REPLACEMENTS.remove(realKey);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        } else {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            WILDCARD_REPLACEMENTS.put(realKey, newId);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        final String fOrig = originalId, fReal = realKey;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        PER_CONTEXT_REPLACEMENTS.entrySet().removeIf(e -> e.getValue().equals(fOrig) || e.getValue().equals(fReal));
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (newId == null || newId.equals(realKey)) WILDCARD_REPLACEMENTS.remove(realKey);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        else WILDCARD_REPLACEMENTS.put(realKey, newId);
//#endif
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 获取通配物品替换后的ID */
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public static String getWildcardReplacement(String itemId) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return WILDCARD_REPLACEMENTS.getOrDefault(itemId, itemId);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    public static String getWildcardReplacement(String itemId) { return WILDCARD_REPLACEMENTS.getOrDefault(itemId, itemId); }
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 设置按上下文（父物品）的通配物品替换（支持链式替换） */
//#endif
    public static void setContextWildcardReplacement(String parentId, String childId, String newId) {
        String key = parentId + ":" + childId;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 如果 childId 本身是某个替换的值，找到真正的原始 childId
//#endif
        String realChildId = childId;
        boolean resolvedFromGlobal = false;
        for (Map.Entry<String, String> e : PER_CONTEXT_REPLACEMENTS.entrySet()) {
            if (e.getKey().startsWith(parentId + ":") && e.getValue().equals(childId)) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                realChildId = e.getKey().substring(parentId.length() + 1);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                break;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                realChildId = e.getKey().substring(parentId.length() + 1); break;
//#endif
            }
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 同样检查 WILDCARD_REPLACEMENTS（全局替换），确保同步关时也能正确追溯原始ID
//#endif
        if (realChildId.equals(childId)) {
            for (Map.Entry<String, String> e : WILDCARD_REPLACEMENTS.entrySet()) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (e.getValue().equals(childId)) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    realChildId = e.getKey();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    resolvedFromGlobal = true;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    break;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                if (e.getValue().equals(childId)) { realChildId = e.getKey(); resolvedFromGlobal = true; break; }
//#endif
            }
        }
        String realKey = parentId + ":" + realChildId;
        if (newId == null || newId.equals(realChildId)) {
            PER_CONTEXT_REPLACEMENTS.remove(realKey);
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 如果 realChildId 是从全局替换中解析出来的，说明当前显示的值来自全局替换
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 清除上下文后若不清除全局替换，树状图会回退到全局替换值而非原始值
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (resolvedFromGlobal) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                WILDCARD_REPLACEMENTS.remove(realChildId);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (resolvedFromGlobal) WILDCARD_REPLACEMENTS.remove(realChildId);
//#endif
        } else {
            PER_CONTEXT_REPLACEMENTS.put(realKey, newId);
        }
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 获取按上下文的通配物品替换后的ID（未找到返回原ID） */
//#endif
    public static String getContextWildcardReplacement(String parentId, String childId) {
        String key = parentId + ":" + childId;
        return PER_CONTEXT_REPLACEMENTS.getOrDefault(key, childId);
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 设置用户对某物品的偏好配方类型 */
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public static void setPreferredRecipeType(String itemId, String recipeType) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        PREFERRED_RECIPE_TYPES.put(itemId, recipeType);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    public static void setPreferredRecipeType(String itemId, String recipeType) { PREFERRED_RECIPE_TYPES.put(itemId, recipeType); }
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 切换物品的配方类型（循环切换备选类型） */
//#endif
    public static String toggleRecipeType(String itemId, String currentType, List<String> alternatives) {
        if (alternatives == null || alternatives.isEmpty()) return currentType;
        String preferred = PREFERRED_RECIPE_TYPES.get(itemId);
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (preferred == null) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 当前是默认类型，切换到第一个备选
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            String next = alternatives.get(0);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            PREFERRED_RECIPE_TYPES.put(itemId, next);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            return next;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 找到当前preferred在alternatives中的位置
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (preferred == null) { PREFERRED_RECIPE_TYPES.put(itemId, alternatives.get(0)); return alternatives.get(0); }
//#endif
        int idx = alternatives.indexOf(preferred);
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (idx < 0 || idx >= alternatives.size() - 1) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 切换回默认（移除preference）
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            PREFERRED_RECIPE_TYPES.remove(itemId);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            return null;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 切换到下一个备选
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (idx < 0 || idx >= alternatives.size() - 1) { PREFERRED_RECIPE_TYPES.remove(itemId); return null; }
//#endif
        String next = alternatives.get(idx + 1);
        PREFERRED_RECIPE_TYPES.put(itemId, next);
        return next;
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 冗余列表：合成过程中多出来的物品 */
//#endif
    private final List<MaterialDataStore.RawMaterialEntry> redundancy = new ArrayList<>();

    public RawMaterialTreeAnalyzer() {
        this.whitelist = RawMaterialConfig.currentWhitelist;
        this.recipePriority = RawMaterialConfig.getEnabledRecipeTypeGroups();
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public void setMaxDepth(int maxDepth) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        this.maxDepth = maxDepth;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    public void setMaxDepth(int maxDepth) { this.maxDepth = maxDepth; }
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 分析结果 */
//#endif
    public record TreeResult(List<RecipeTreeNode> roots, List<MaterialDataStore.RawMaterialEntry> redundancy) {}

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 分析材料列表，返回配方树根节点列表和冗余物品。
     */
//#endif
    public TreeResult analyze(List<MaterialEntry> entries) {
        List<RecipeTreeNode> roots = new ArrayList<>();
        redundancy.clear();
        for (MaterialEntry entry : entries) {
            Item item = entry.item();
            if (item == null) continue;
            // 特殊物品（带非默认组件）：
            // - 带特殊瓦片的陶罐按精确ID中的 sherds 分解为各瓦片；
            // - 其余按基础物品溯源，其材料与普通同类物品完全一致，同时作为独立条目呈现。
            if (entry.isExact()) {
                // 装有物品的潜影盒/收纳袋：按普通潜影盒/收纳袋溯源。
                // 其精确ID的 container / bundle_contents 组件内可能嵌套盔甲纹饰物品（含 pattern/material 字段），
                // 若继续走纹饰分支会把纹饰模板错误地加到潜影盒的溯源配方上。
                if (isContainerLikeExactItem(item)) {
                    depth = 0;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    visited = new HashSet<>();
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    Set<String> visited = new HashSet<>();
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    RecipeTreeNode containerNode = traceItem(item.getRegistryEntry(), entry.totalCount(), null,
//#elseif MC == 11701
//$$                    RecipeTreeNode containerNode = traceItem(item, entry.totalCount(), null,
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            "(顶层) " + entry.blockName());
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                            "(顶层) " + entry.blockName(), visited);
//#endif
                    if (containerNode != null) {
                        // 携带精确堆叠：界面按特殊物品渲染变种图标与名称
                        containerNode.exactStack = entry.exactStack();
                        roots.add(containerNode);
                    }
                    continue;
                }
                RecipeTreeNode potNode = traceDecoratedPot(entry);
                if (potNode != null) {
                    roots.add(potNode);
                    continue;
                }
                // 带纹饰的盔甲：按锻造台配方分解为 盔甲 / 纹饰模板 / 镶嵌材质 三个子节点
                RecipeTreeNode trimNode = traceTrimmedArmor(entry);
                if (trimNode != null) {
                    roots.add(trimNode);
                    continue;
                }
                depth = 0;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                visited = new HashSet<>();
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                Set<String> visited = new HashSet<>();
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                RecipeTreeNode exactNode = traceItem(item.getRegistryEntry(), entry.totalCount(), null,
//#elseif MC == 11701
//$$                RecipeTreeNode exactNode = traceItem(item, entry.totalCount(), null,
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        "(顶层) " + entry.blockName());
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                        "(顶层) " + entry.blockName(), visited);
//#endif
                if (exactNode != null) {
                    // 携带精确堆叠：界面按特殊物品渲染变种图标与名称
                    exactNode.exactStack = entry.exactStack();
                    roots.add(exactNode);
                }
                continue;
            }
            depth = 0;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            visited = new HashSet<>();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            RecipeTreeNode node = traceItem(item.getRegistryEntry(), entry.totalCount(), null,
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    "(顶层) " + entry.blockName());
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (node != null) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                roots.add(node);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            Set<String> visited = new HashSet<>();
//#endif
//#if MC == 11701
//$$            RecipeTreeNode node = traceItem(item, entry.totalCount(), null, "(顶层) " + entry.blockName(), visited);
//#elseif MC == 11904 || MC == 12001
//$$            RecipeTreeNode node = traceItem(item.getRegistryEntry(), entry.totalCount(), null, "(顶层) " + entry.blockName(), visited);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (node != null) roots.add(node);
//#endif
        }
        return new TreeResult(roots, new ArrayList<>(redundancy));
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 染色潜影盒专用：直接构造「无色潜影盒 + 对应颜色染料」两个子材料。
     * 染色潜影盒配方（早期的 crafting_special_shulkerboxcoloring，1.21.10+ 的 crafting_transmute）
     * 均无法通过配方书稳定溯源，故不依赖配方数据、按颜色名直接拼装。
     */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /**
//$$     * 染色潜影盒专用：直接构造「无色潜影盒 + 对应颜色染料」两个子材料。
//$$     * 染色潜影盒配方（crafting_special_shulkerboxcoloring）无法通过配方书稳定溯源，
//$$     * 故不依赖配方数据、按颜色名直接拼装。
//$$     */
//#endif
    private RecipeTreeNode tryTraceShulkerBoxDyeing(String itemIdStr, int count) {
        String color = itemIdStr.substring("minecraft:".length(), itemIdStr.length() - "_shulker_box".length());
        List<RecipeTreeNode> children = new ArrayList<>();
        children.add(RecipeTreeNode.base("minecraft:shulker_box", count));
        children.add(RecipeTreeNode.base("minecraft:" + color + "_dye", count));
        return new RecipeTreeNode(itemIdStr, count, "crafting_shapeless", List.of(), children);
    }

    /**
     * 判断精确物品的基础物品是否为「容器类」（潜影盒/收纳袋）。
     * 这类物品只有装有内容物时才会成为精确物品，而内容物可能嵌套盔甲纹饰组件，
     * 需按普通潜影盒/收纳袋的配方溯源，避免被纹饰分支误匹配。
     */
    private static boolean isContainerLikeExactItem(Item item) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (item == null) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            return false;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (item == null) return false;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Identifier id = Registries.ITEM.getId(item);
//#elseif MC == 11701
//$$        Identifier id = Registry.ITEM.getId(item);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (id == null) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            return false;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (id == null) return false;
//#endif
        String path = id.getPath();
        // 潜影盒（含全部染色变体 *_shulker_box）与收纳袋（含全部染色变体 *_bundle）
        return path.equals("bundle") || path.endsWith("_bundle") || path.endsWith("shulker_box");
    }

    /**
     * 带特殊瓦片的陶罐：从精确ID中解析 sherds 列表，分解为各瓦片子材料。
     * 非陶罐的精确物品返回 null（维持原有「不溯源」行为）。
     */
    private RecipeTreeNode traceDecoratedPot(MaterialEntry entry) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (entry.item() != Items.DECORATED_POT) {
//#endif
//#if MC == 11701
//$$        Item pot = decoratedPotItem();
//#endif
//#if MC == 11701
//$$        if (pot == null || entry.item() != pot) {
//#endif
            return null;
        }
        List<String> sherds = extractSherds(entry.blockName());
        if (sherds.isEmpty()) {
            return null;
        }
        // 同一瓦片可能出现多次（如两个 brick），按瓦片ID归并数量
        Map<String, Integer> sherdCounts = new LinkedHashMap<>();
        for (String sherd : sherds) {
            sherdCounts.merge(sherd, entry.totalCount(), Integer::sum);
        }
        List<RecipeTreeNode> subs = new ArrayList<>();
        for (Map.Entry<String, Integer> sherd : sherdCounts.entrySet()) {
            subs.add(RecipeTreeNode.base(sherd.getKey(), sherd.getValue()));
        }
        RecipeTreeNode node = new RecipeTreeNode("minecraft:decorated_pot", entry.totalCount(),
                "crafting_shaped", List.of(), subs);
        // 携带精确堆叠，使界面按特殊瓦片渲染变种材质（与【查看材料列表】一致）
        node.exactStack = entry.exactStack();
        return node;
    }

    /**
     * 带纹饰的盔甲：按锻造台配方（纹饰模板 + 底座盔甲 + 镶嵌材质）分解为三个子节点，
     * 与合金装备（下界合金）的锻造台分解方式一致。
     * 纹饰图案/材质从精确ID的 SNBT 中读取（如
     * {@code trim={material:"minecraft:iron",pattern:"minecraft:sentry"}}）；
     * 非纹饰物品返回 null（回退到按基础物品溯源）。
     */
    private RecipeTreeNode traceTrimmedArmor(MaterialEntry entry) {
        Item armor = entry.item();
        if (armor == null) {
            return null;
        }
        String exactId = entry.blockName();
        if (exactId == null) {
            return null;
        }
        String patternKey = extractSnbtString(exactId, "pattern");
        String materialKey = extractSnbtString(exactId, "material");
        if (patternKey == null || materialKey == null) {
            return null;
        }
        Item template = LitematicReader.trimTemplateItem(patternKey);
        Item trimMaterial = LitematicReader.trimMaterialItem(materialKey);
        if (template == null || trimMaterial == null) {
            return null;
        }
        int count = entry.totalCount();
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        String armorId = Registries.ITEM.getId(armor).toString();
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        String templateId = Registries.ITEM.getId(template).toString();
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        String materialId = Registries.ITEM.getId(trimMaterial).toString();
//#endif
//#if MC == 11701
//$$        Identifier armorIdObj = Registry.ITEM.getId(armor);
//#endif
//#if MC == 11701
//$$        Identifier templateIdObj = Registry.ITEM.getId(template);
//#endif
//#if MC == 11701
//$$        Identifier materialIdObj = Registry.ITEM.getId(trimMaterial);
//#endif
//#if MC == 11701
//$$        if (armorIdObj == null || templateIdObj == null || materialIdObj == null) {
//#endif
//#if MC == 11701
//$$            return null;
//#endif
//#if MC == 11701
//$$        }
//#endif
//#if MC == 11701
//$$        String armorId = armorIdObj.toString();
//#endif
//#if MC == 11701
//$$        String templateId = templateIdObj.toString();
//#endif
//#if MC == 11701
//$$        String materialId = materialIdObj.toString();
//#endif

        // 三个子节点各自独立溯源（重置 depth/visited），与合金装备分解行为一致
        depth = 0;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        visited = new HashSet<>();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        RecipeTreeNode baseNode = traceItem(armor.getRegistryEntry(), count, null, "(顶层) " + exactId);
//#endif
//#if MC == 11701
//$$        RecipeTreeNode baseNode = traceItem(armor, count, null, "(顶层) " + exactId, new HashSet<>());
//#elseif MC == 11904 || MC == 12001
//$$        RecipeTreeNode baseNode = traceItem(armor.getRegistryEntry(), count, null, "(顶层) " + exactId, new HashSet<>());
//#endif
        depth = 0;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        visited = new HashSet<>();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        RecipeTreeNode templateNode = traceItem(template.getRegistryEntry(), count, null, "(顶层) " + exactId);
//#endif
//#if MC == 11701
//$$        RecipeTreeNode templateNode = traceItem(template, count, null, "(顶层) " + exactId, new HashSet<>());
//#elseif MC == 11904 || MC == 12001
//$$        RecipeTreeNode templateNode = traceItem(template.getRegistryEntry(), count, null, "(顶层) " + exactId, new HashSet<>());
//#endif
        depth = 0;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        visited = new HashSet<>();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        RecipeTreeNode materialNode = traceItem(trimMaterial.getRegistryEntry(), count, null, "(顶层) " + exactId);
//#endif
//#if MC == 11701
//$$        RecipeTreeNode materialNode = traceItem(trimMaterial, count, null, "(顶层) " + exactId, new HashSet<>());
//#elseif MC == 11904 || MC == 12001
//$$        RecipeTreeNode materialNode = traceItem(trimMaterial.getRegistryEntry(), count, null, "(顶层) " + exactId, new HashSet<>());
//#endif

        List<RecipeTreeNode> subs = new ArrayList<>();
        subs.add(baseNode != null ? baseNode : RecipeTreeNode.base(armorId, count));
        subs.add(templateNode != null ? templateNode : RecipeTreeNode.base(templateId, count));
        subs.add(materialNode != null ? materialNode : RecipeTreeNode.base(materialId, count));

        RecipeTreeNode.RecipeDetail detail = new RecipeTreeNode.RecipeDetail();
        detail.type = "smithing";
        detail.smithingType = "trim";
        detail.resultCount = count;
        detail.template = new RecipeTreeNode.RecipeSlot(templateId);
        detail.base = new RecipeTreeNode.RecipeSlot(armorId);
        detail.addition = new RecipeTreeNode.RecipeSlot(materialId);

        RecipeTreeNode node = new RecipeTreeNode(armorId, count, "smithing", List.of(detail), subs);
        // 携带精确堆叠：根节点按特殊纹饰渲染变种图标与名称
        node.exactStack = entry.exactStack();
        return node;
    }

    /** 从精确ID的 SNBT 中提取形如 {@code field:"值"} 的字符串字段（用于读取 trim 的 pattern/material）。 */
    private static String extractSnbtString(String exactId, String field) {
        String key = field + ":\"";
        int start = exactId.indexOf(key);
        if (start < 0) {
            return null;
        }
        start += key.length();
        int end = exactId.indexOf('"', start);
        if (end < 0) {
            return null;
        }
        return exactId.substring(start, end);
    }

    /**
     * 从精确ID中提取瓦片列表。新格式（1.20.5+）为
     * {@code pot_decorations=["minecraft:a",...]}；旧格式回退 {@code sherds:["a",...]}。
     */
    private static List<String> extractSherds(String exactId) {
        List<String> result = new ArrayList<>();
        if (exactId == null) {
            return result;
        }
        String prefix = "pot_decorations=[";
        int key = exactId.indexOf(prefix);
        if (key < 0) {
            prefix = "sherds:[";
            key = exactId.indexOf(prefix);
        }
        if (key < 0) {
            return result;
        }
        int start = key + prefix.length();
        int end = exactId.indexOf(']', start);
        if (end < 0) {
            return result;
        }
        for (String part : exactId.substring(start, end).split(",")) {
            String value = part.trim();
            if (value.length() >= 2
                    && (value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"'
                        || value.charAt(0) == '\'' && value.charAt(value.length() - 1) == '\'')) {
                value = value.substring(1, value.length() - 1);
            }
            if (value.isEmpty() == false) {
                result.add(value);
            }
        }
        return result;
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 递归溯源单个物品，返回树节点。
     */
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private RecipeTreeNode traceItem(RegistryEntry<Item> item, int count,
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                     RegistryEntry<Item> prevItem, String sourceDesc) {
//#endif
//#if MC == 11701
//$$    private RecipeTreeNode traceItem(Item item, int count, Item prevItem, String sourceDesc, Set<String> visited) {
//#elseif MC == 11904 || MC == 12001
//$$    private RecipeTreeNode traceItem(RegistryEntry<Item> item, int count, RegistryEntry<Item> prevItem, String sourceDesc, Set<String> visited) {
//#endif
        if (count <= 0) return null;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (depth >= maxDepth) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            return RecipeTreeNode.base(item.getIdAsString(), count);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 11701
//$$        if (depth >= maxDepth) return RecipeTreeNode.base(Registry.ITEM.getId(item).toString(), count);
//#elseif MC == 11904 || MC == 12001
//$$        if (depth >= maxDepth) return RecipeTreeNode.base(Registries.ITEM.getId(item.value()).toString(), count);
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Item mcItem = item.value();
//#elseif MC == 11701
//$$        Item mcItem = item;
//#endif
        // 糖特殊处理：虽已列入溯源白名单，但仍保留配方溯源与工作台图标切换
        // （默认甘蔗配方，可切换为蜂蜜瓶配方）。须在白名单短路之前处理。
        if (mcItem == Items.SUGAR) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            RecipeTreeNode sugarNode = tryTraceSugar(count, item, sourceDesc);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            RecipeTreeNode sugarNode = tryTraceSugar(count, item, sourceDesc, visited);
//#endif
            if (sugarNode != null) return sugarNode;
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (whitelist.contains(mcItem)) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            return RecipeTreeNode.base(item.getIdAsString(), count);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 11701
//$$        if (whitelist.contains(mcItem)) return RecipeTreeNode.base(Registry.ITEM.getId(item).toString(), count);
//#elseif MC == 11904 || MC == 12001
//$$        if (whitelist.contains(mcItem)) return RecipeTreeNode.base(Registries.ITEM.getId(item.value()).toString(), count);
//#endif
        MinecraftClient client = MinecraftClient.getInstance();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (client.world == null) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            return RecipeTreeNode.base(item.getIdAsString(), count);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        ContextParameterMap map = RecipeBookUtils.getMap(client);
//#elseif MC == 12101
//$$        RegistryWrapper.WrapperLookup map = RecipeBookUtils.getMap(client);
//#endif
//#if MC == 11701
//$$        if (client.world == null) return RecipeTreeNode.base(Registry.ITEM.getId(item).toString(), count);
//#elseif MC == 11904 || MC == 12001
//$$        if (client.world == null) return RecipeTreeNode.base(Registries.ITEM.getId(item.value()).toString(), count);
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        String itemIdStr = Registries.ITEM.getId(mcItem).toString();
//#elseif MC == 11701
//$$        String itemIdStr = Registry.ITEM.getId(mcItem).toString();
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 溯源记录表：如果该物品已在当前溯源路径中出现过，停止进一步分解，防止循环溯源
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 溯源记录表：如果该物品已在当前溯源路径中，停止进一步分解
//#endif
        if (!visited.add(itemIdStr)) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            return RecipeTreeNode.base(item.getIdAsString(), count);
//#elseif MC == 11701
//$$            return RecipeTreeNode.base(Registry.ITEM.getId(item).toString(), count);
//#elseif MC == 11904 || MC == 12001
//$$            return RecipeTreeNode.base(Registries.ITEM.getId(item.value()).toString(), count);
//#endif
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 平滑石半砖特殊处理
//#endif
        if ("minecraft:smooth_stone_slab".equals(itemIdStr)) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            RecipeTreeNode smoothStoneNode = tryTraceSmoothStoneSlab(itemIdStr, count, map);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            RecipeTreeNode n = tryTraceSmoothStoneSlab(itemIdStr, count, client, visited);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (smoothStoneNode != null) return smoothStoneNode;
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            if (n != null) return n;
//#endif
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 雕纹石英特殊处理：优先使用石英块配方，而非石英台阶配方
//#endif
        if ("minecraft:chiseled_quartz_block".equals(itemIdStr)) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            RecipeTreeNode quartzNode = tryTraceChiseledQuartzBlock(itemIdStr, count, map);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            RecipeTreeNode n = tryTraceChiseledQuartzBlock(itemIdStr, count, client, visited);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (quartzNode != null) return quartzNode;
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            if (n != null) return n;
//#endif
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 白色床/白色羊毛：跳过"白色染料+任意彩色床/羊毛→白色"的回染配方，
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 白色床/白色羊毛：跳过"白染料+彩色床/羊毛→白色"的回染配方，避免白床被错误分解为「彩色床+白染料」
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 否则白床会被错误分解为「彩色床 + 白色染料」
//#endif
        if ("minecraft:white_bed".equals(itemIdStr) || "minecraft:white_wool".equals(itemIdStr)) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            RecipeTreeNode whiteNode = tryTraceWhiteDyeable(itemIdStr, count, item, sourceDesc, map);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            RecipeTreeNode n = tryTraceWhiteDyeable(itemIdStr, count, item, sourceDesc, client, visited);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (whiteNode != null) return whiteNode;
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            if (n != null) return n;
//#endif
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 染色床特殊处理：所有非白色床优先走染色配方（染料+白色床）
//#endif
        if (itemIdStr.endsWith("_bed") && !"minecraft:white_bed".equals(itemIdStr)) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            RecipeTreeNode bedNode = tryTraceBedDyeing(itemIdStr, count, item, sourceDesc, map);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            RecipeTreeNode n = tryTraceBedDyeing(itemIdStr, count, item, sourceDesc, client, visited);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (bedNode != null) return bedNode;
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            if (n != null) return n;
//#endif
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
        // 染色潜影盒特殊处理：直接按颜色拼出「无色潜影盒 + 对应染料」，不依赖配方数据
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // （染色潜影盒配方为特殊配方/transmute，配方书取不到 ingredients）
//#endif
        if (itemIdStr.endsWith("_shulker_box") && !"minecraft:shulker_box".equals(itemIdStr)) {
            RecipeTreeNode shulkerNode = tryTraceShulkerBoxDyeing(itemIdStr, count);
            if (shulkerNode != null) return shulkerNode;
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 按优先级分组查找配方，收集所有可用配方类型
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 如果有用户偏好的配方类型，将其提前
//#endif
        String preferred = PREFERRED_RECIPE_TYPES.get(itemIdStr);
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        List<List<RecipeBookUtils.Type>> orderedPriority = recipePriority;
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        List<List<String>> orderedPriority = recipePriority;
//#endif
        if (preferred != null) {
            orderedPriority = new ArrayList<>(recipePriority);
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 找到包含preferred类型的组，移到最前面
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            RecipeBookUtils.Type prefType = jsonToRecipeType(preferred);
//#endif
            for (int i = 0; i < orderedPriority.size(); i++) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (orderedPriority.get(i).contains(prefType)) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    if (i > 0) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        List<RecipeBookUtils.Type> group = orderedPriority.remove(i);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        orderedPriority.add(0, group);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                if (groupMatchesPreferred(orderedPriority.get(i), preferred)) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    if (i > 0) { List<String> g = orderedPriority.remove(i); orderedPriority.add(0, g); }
//#endif
                    break;
                }
            }
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        RecipeDisplayEntry primaryEntry = null;
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        RecipeHelper.RecipeLookupResult primaryResult = null;
//#endif
        List<Ingredient> primaryIngredients = null;
        int primaryResultCount = 0;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        RecipeBookUtils.Type primaryTypeEnum = null;
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        String primaryTypeStr = null;
//#endif
        List<String> alternativeTypes = new ArrayList<>();
        boolean waxedBothKinds = false;       // 涂蜡铜：同组内同时存在「成型」与「蜜脾涂蜡」配方
        boolean stonecutterAvailable = false; // 涂蜡铜：存在切石机配方（切换环的默认项）

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        for (List<RecipeBookUtils.Type> typeGroup : orderedPriority) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        for (List<String> typeGroup : orderedPriority) {
//#endif
            ItemStack shadow = new ItemStack(item);
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            List<Pair<NetworkRecipeId, RecipeDisplayEntry>> lookup =
//#elseif MC == 12101
//$$            List<Pair<Identifier, RecipeDisplayEntry>> lookup =
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    RecipeBookUtils.getDisplayEntryFromRecipeBook(shadow, typeGroup);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            List<RecipeHelper.RecipeLookupResult> lookup = RecipeHelper.lookupAllRecipes(shadow, typeGroup, client);
//#endif
            if (lookup.isEmpty()) continue;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            Pair<NetworkRecipeId, RecipeDisplayEntry> pair = selectMainEntry(lookup, itemIdStr, preferred, map);
//#elseif MC == 12101
//$$            Pair<Identifier, RecipeDisplayEntry> pair = selectMainEntry(lookup, itemIdStr, preferred);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (pair == null) continue;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            RecipeDisplayEntry entry = pair.getRight();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            String typeName = recipeTypeToJson(typeGroup.get(0));
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (typeGroup.contains(RecipeBookUtils.Type.STONECUTTER)) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                stonecutterAvailable = true;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if ((typeGroup.contains(RecipeBookUtils.Type.SHAPED)
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    || typeGroup.contains(RecipeBookUtils.Type.SHAPELESS))
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    && isWaxedCopperProduct(itemIdStr) && hasBothWaxKinds(lookup, map)) {
//#elseif MC == 12101
//$$                    && isWaxedCopperProduct(itemIdStr) && hasBothWaxKinds(lookup)) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            RecipeHelper.RecipeLookupResult sel = selectMainEntry(lookup, itemIdStr, preferred, client);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (typeGroup.contains("stonecutter")) stonecutterAvailable = true;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if ((typeGroup.contains("crafting_shaped") || typeGroup.contains("crafting_shapeless"))
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    && isWaxedCopperProduct(itemIdStr) && hasBothWaxKinds(lookup, client)) {
//#endif
                waxedBothKinds = true;
            }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (primaryEntry == null) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 第一个匹配的配方作为主配方
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                primaryEntry = entry;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                primaryIngredients = entry.craftingRequirements().get();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                List<ItemStack> resultStacks = entry.getStacks(map);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (resultStacks.isEmpty()) { primaryEntry = null; continue; }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                primaryResultCount = resultStacks.getFirst().getCount();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                primaryTypeEnum = typeGroup.get(0);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            } else if (!alternativeTypes.contains(typeName)) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 后续匹配的作为备选配方类型
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                alternativeTypes.add(typeName);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (primaryResult == null) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                primaryResult = sel;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                primaryIngredients = sel.ingredients();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                primaryResultCount = sel.resultCount();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                primaryTypeStr = typeGroup.get(0);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            } else {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                String tn = RecipeHelper.recipeTypeToJson(typeGroup.get(0));
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                if (!alternativeTypes.contains(tn)) alternativeTypes.add(tn);
//#endif
            }
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (primaryEntry != null) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            List<Ingredient> ingredients = primaryIngredients;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            int resultCount = primaryResultCount;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 循环检测
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (checkLoop(ingredients, item, prevItem, map)) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                return RecipeTreeNode.base(item.getIdAsString(), count);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (primaryResult != null) {
//#endif
//#if MC == 11701
//$$            if (checkLoop(primaryIngredients, item, prevItem, client)) return RecipeTreeNode.base(Registry.ITEM.getId(item).toString(), count);
//#elseif MC == 11904 || MC == 12001
//$$            if (checkLoop(primaryIngredients, item, prevItem, client)) return RecipeTreeNode.base(Registries.ITEM.getId(item.value()).toString(), count);
//#endif
            depth++;
            RecipeTreeNode node;
            if (WILDCARD_ITEM_IDS.contains(itemIdStr)) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                node = processWildcardRecipe(ingredients, resultCount, count, item, sourceDesc, primaryTypeEnum, map);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                node = processWildcardRecipe(primaryIngredients, primaryResultCount, count, item, sourceDesc, primaryTypeStr, client, visited);
//#endif
            } else {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                node = processRecipe(ingredients, resultCount, count, item, sourceDesc, primaryTypeEnum, map, primaryEntry.display());
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                node = processRecipe(primaryIngredients, primaryResultCount, count, item, sourceDesc, primaryTypeStr, client, primaryResult, visited);
//#endif
            }
            depth--;
            // 涂蜡铜：切换环固定为「切石机 → 成型 → 蜜脾涂蜡」，备选列表=默认项之外的部分
            if (waxedBothKinds) {
                alternativeTypes.clear();
                if (stonecutterAvailable) alternativeTypes.add("crafting_shaped");
                alternativeTypes.add("crafting_honeycomb");
            }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (node != null && !alternativeTypes.isEmpty()) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                node.alternativeRecipeTypes = alternativeTypes;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (node != null && !alternativeTypes.isEmpty()) node.alternativeRecipeTypes = alternativeTypes;
//#endif
            return node;
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 无匹配配方 — 尝试从RecipeManager直接兜底查找
//#endif
        RecipeFallback fallback = findRecipeFromManager(mcItem);
        if (fallback != null) {
            depth++;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            RecipeTreeNode node = processRecipe(fallback.ingredients, fallback.resultCount, count,
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    item, sourceDesc, fallback.type, map, null);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            depth--;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (node != null) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                LitematListMod.LOGGER.info("[RawMaterialTreeAnalyzer] RecipeManager兜底成功: {} (类型: {})",
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        itemIdStr, fallback.type);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            RecipeTreeNode node;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (WILDCARD_ITEM_IDS.contains(itemIdStr)) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                node = processWildcardRecipe(fallback.ingredients, fallback.resultCount, count, item, sourceDesc, fallback.type, client, visited);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            } else {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                node = processRecipe(fallback.ingredients, fallback.resultCount, count, item, sourceDesc, fallback.type, client, null, visited);
//#endif
            }
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            depth--;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (node != null) LitematListMod.LOGGER.info("[RawMaterialTreeAnalyzer] RecipeManager兜底成功: {} (类型: {})", itemIdStr, fallback.type);
//#endif
            return node;
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 完全无法溯源
//#endif
        LitematListMod.LOGGER.info("[RawMaterialTreeAnalyzer] 无法溯源: {} (depth={})", itemIdStr, depth);
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return RecipeTreeNode.base(item.getIdAsString(), count);
//#endif
//#if MC == 11701
//$$        return RecipeTreeNode.base(Registry.ITEM.getId(item).toString(), count);
//#elseif MC == 11904 || MC == 12001
//$$        return RecipeTreeNode.base(Registries.ITEM.getId(item.value()).toString(), count);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 判断配置分组是否匹配偏好类型（"crafting_honeycomb" 伪类型匹配工作台分组） */
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    private static boolean groupMatchesPreferred(List<String> group, String preferred) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (group.contains(preferred)) return true;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        return "crafting_honeycomb".equals(preferred)
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                && (group.contains("crafting_shaped") || group.contains("crafting_shapeless"));
//#endif
    }

    /**
     * 在同一配方类型组的多条配方中挑选主配方。
     * - 涂蜡铜制品：默认取「成型」配方（由已涂蜡父材料加工），用户偏好 "crafting_honeycomb" 时取「蜜脾涂蜡」配方；
     * - 苔石/苔石砖：默认取含苔藓的配方（圆石+苔藓）；
     * - 其余情况取第一条有效配方。
     */
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static Pair<NetworkRecipeId, RecipeDisplayEntry> selectMainEntry(
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            List<Pair<NetworkRecipeId, RecipeDisplayEntry>> lookup, String itemIdStr,
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            String preferred, ContextParameterMap map) {
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        List<Pair<NetworkRecipeId, RecipeDisplayEntry>> valid = new ArrayList<>();
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        for (Pair<NetworkRecipeId, RecipeDisplayEntry> p : lookup) {
//#endif
//#if MC == 12101
//$$    private static Pair<Identifier, RecipeDisplayEntry> selectMainEntry(
//#endif
//#if MC == 12101
//$$            List<Pair<Identifier, RecipeDisplayEntry>> lookup, String itemIdStr, String preferred) {
//#endif
//#if MC == 12101
//$$        List<Pair<Identifier, RecipeDisplayEntry>> valid = new ArrayList<>();
//#endif
//#if MC == 12101
//$$        for (Pair<Identifier, RecipeDisplayEntry> p : lookup) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (!p.getRight().craftingRequirements().isEmpty()) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                valid.add(p);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (valid.isEmpty()) return null;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    private static RecipeHelper.RecipeLookupResult selectMainEntry(
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            List<RecipeHelper.RecipeLookupResult> lookup, String itemIdStr, String preferred, MinecraftClient client) {
//#endif
        if (isWaxedCopperProduct(itemIdStr)) {
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            Pair<NetworkRecipeId, RecipeDisplayEntry> honeycombEntry = null;
//#elseif MC == 12101
//$$            Pair<Identifier, RecipeDisplayEntry> honeycombEntry = null;
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            Pair<NetworkRecipeId, RecipeDisplayEntry> normalEntry = null;
//#elseif MC == 12101
//$$            Pair<Identifier, RecipeDisplayEntry> normalEntry = null;
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            for (Pair<NetworkRecipeId, RecipeDisplayEntry> p : valid) {
//#elseif MC == 12101
//$$            for (Pair<Identifier, RecipeDisplayEntry> p : valid) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                boolean honeycomb = containsIngredient(
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        p.getRight().craftingRequirements().get(), Items.HONEYCOMB, map);
//#elseif MC == 12101
//$$                        p.getRight().craftingRequirements().get(), Items.HONEYCOMB);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            RecipeHelper.RecipeLookupResult honeycombEntry = null;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            RecipeHelper.RecipeLookupResult normalEntry = null;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            for (RecipeHelper.RecipeLookupResult p : lookup) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                boolean honeycomb = containsIngredient(p.ingredients(), "minecraft:honeycomb", client);
//#endif
                if (honeycomb && honeycombEntry == null) {
                    honeycombEntry = p;
                } else if (!honeycomb && normalEntry == null) {
                    normalEntry = p;
                }
            }
            if (honeycombEntry != null && normalEntry != null) {
                return "crafting_honeycomb".equals(preferred) ? honeycombEntry : normalEntry;
            }
        } else if (isMossyItem(itemIdStr)) {
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            for (Pair<NetworkRecipeId, RecipeDisplayEntry> p : valid) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            for (RecipeHelper.RecipeLookupResult p : lookup) {
//#elseif MC == 12101
//$$            for (Pair<Identifier, RecipeDisplayEntry> p : valid) {
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (containsIngredient(p.getRight().craftingRequirements().get(), Items.MOSS_BLOCK, map)) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                if (containsIngredient(p.ingredients(), "minecraft:moss_block", client)) {
//#elseif MC == 12101
//$$                if (containsIngredient(p.getRight().craftingRequirements().get(), Items.MOSS_BLOCK)) {
//#endif
                    return p;
                }
            }
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return valid.getFirst();
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        return lookup.get(0);
//#endif
    }

    /** 判断同一组配方中是否同时存在「蜜脾涂蜡」与「成型」两类配方（涂蜡铜可切换的前提） */
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static boolean hasBothWaxKinds(
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            List<Pair<NetworkRecipeId, RecipeDisplayEntry>> lookup, ContextParameterMap map) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    private static boolean hasBothWaxKinds(List<RecipeHelper.RecipeLookupResult> lookup, MinecraftClient client) {
//#elseif MC == 12101
//$$    private static boolean hasBothWaxKinds(List<Pair<Identifier, RecipeDisplayEntry>> lookup) {
//#endif
        boolean honeycomb = false;
        boolean normal = false;
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        for (Pair<NetworkRecipeId, RecipeDisplayEntry> p : lookup) {
//#elseif MC == 12101
//$$        for (Pair<Identifier, RecipeDisplayEntry> p : lookup) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (p.getRight().craftingRequirements().isEmpty()) continue;
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (containsIngredient(p.getRight().craftingRequirements().get(), Items.HONEYCOMB, map)) {
//#elseif MC == 12101
//$$            if (containsIngredient(p.getRight().craftingRequirements().get(), Items.HONEYCOMB)) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        for (RecipeHelper.RecipeLookupResult p : lookup) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (containsIngredient(p.ingredients(), "minecraft:honeycomb", client)) {
//#endif
                honeycomb = true;
            } else {
                normal = true;
            }
        }
        return honeycomb && normal;
    }

    /** 判断配方原料列表中是否包含指定物品 */
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static boolean containsIngredient(List<Ingredient> ingredients, Item target,
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            ContextParameterMap map) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    private static boolean containsIngredient(List<Ingredient> ingredients, String itemId, MinecraftClient client) {
//#elseif MC == 12101
//$$    private static boolean containsIngredient(List<Ingredient> ingredients, Item target) {
//#endif
        for (Ingredient ing : ingredients) {
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            for (ItemStack stack : ing.toDisplay().getStacks(map)) {
//#elseif MC == 12101
//$$            for (ItemStack stack : RecipeBookUtils.getIngredientStacks(ing)) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (stack.getItem() == target) return true;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (RecipeHelper.ingredientContainsItem(ing, itemId, client)) return true;
//#endif
        }
        return false;
    }

    /** 判断是否为涂蜡铜制品（waxed_ 前缀，且存在未涂蜡的对应物品，覆盖涂蜡块/台阶/铜链等所有变种） */
    private static boolean isWaxedCopperProduct(String itemIdStr) {
        if (!itemIdStr.startsWith("minecraft:waxed_")) return false;
        String baseId = "minecraft:" + itemIdStr.substring("minecraft:waxed_".length());
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return Registries.ITEM.containsId(Identifier.of(baseId));
//#elseif MC == 11904 || MC == 12001
//$$        return Registries.ITEM.containsId(Identifier.tryParse(baseId));
//#endif
//#if MC == 11701
//$$        Identifier base = Identifier.tryParse(baseId);
//#endif
//#if MC == 11701
//$$        return base != null && Registry.ITEM.containsId(base);
//#endif
    }

    /** 苔石/苔石砖（配方：圆石+苔藓 或 圆石+藤蔓，默认苔藓且苔藓/藤蔓可替换） */
    private static boolean isMossyItem(String itemIdStr) {
        return "minecraft:mossy_cobblestone".equals(itemIdStr)
                || "minecraft:mossy_stone_bricks".equals(itemIdStr);
    }

//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$    /**
//$$     * 糖特殊处理：默认使用甘蔗配方，可通过点击工作台图标切换为蜂蜜瓶配方。
//$$     * 两条配方同为无序合成，无法用配方类型区分，故按原料是否含蜂蜜瓶来挑选，
//$$     * 并以伪类型 "crafting_honey_bottle" 作为备选项。
//$$     */
//#endif
//#if MC == 11701
//$$    private RecipeTreeNode tryTraceSugar(int count, Item item, String sourceDesc, Set<String> visited) {
//#elseif MC == 11904 || MC == 12001
//$$    private RecipeTreeNode tryTraceSugar(int count, RegistryEntry<Item> item, String sourceDesc, Set<String> visited) {
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$    private RecipeTreeNode tryTraceSugar(int count, RegistryEntry<Item> item, String sourceDesc) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        MinecraftClient client = MinecraftClient.getInstance();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        if (client.world == null) return null;
//#endif
//#if MC == 12101
//$$        RegistryWrapper.WrapperLookup map = RecipeBookUtils.getMap(client);
//#elseif MC == 12104 || MC == 12108
//$$        ContextParameterMap map = RecipeBookUtils.getMap(client);
//#endif
//#if MC == 11701
//$$        ItemStack shadow = new ItemStack(item);
//#elseif MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        ItemStack shadow = new ItemStack(item.value());
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$        RecipeDisplayEntry caneEntry = null;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$        RecipeDisplayEntry honeyEntry = null;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$        RecipeBookUtils.Type recipeType = null;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$        for (List<RecipeBookUtils.Type> typeGroup : recipePriority) {
//#endif
//#if MC == 12101
//$$            List<Pair<Identifier, RecipeDisplayEntry>> lookup =
//#elseif MC == 12104 || MC == 12108
//$$            List<Pair<NetworkRecipeId, RecipeDisplayEntry>> lookup =
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$                    RecipeBookUtils.getDisplayEntryFromRecipeBook(shadow, typeGroup);
//#endif
//#if MC == 12101
//$$            for (Pair<Identifier, RecipeDisplayEntry> pair : lookup) {
//#elseif MC == 12104 || MC == 12108
//$$            for (Pair<NetworkRecipeId, RecipeDisplayEntry> pair : lookup) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$                RecipeDisplayEntry entry = pair.getRight();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$                if (entry.craftingRequirements().isEmpty()) continue;
//#endif
//#if MC == 12101
//$$                if (containsIngredient(entry.craftingRequirements().get(), Items.HONEY_BOTTLE)) {
//#elseif MC == 12104 || MC == 12108
//$$                if (containsIngredient(entry.craftingRequirements().get(), Items.HONEY_BOTTLE, map)) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$                    if (honeyEntry == null) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$                        honeyEntry = entry;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$                        recipeType = typeGroup.get(0);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$                    }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$                } else if (caneEntry == null) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$                    caneEntry = entry;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$                    recipeType = typeGroup.get(0);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        RecipeHelper.RecipeLookupResult caneEntry = null;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        RecipeHelper.RecipeLookupResult honeyEntry = null;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        for (RecipeHelper.RecipeLookupResult result : RecipeHelper.lookupAllRecipes(shadow, List.of("crafting_shapeless"), client)) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            boolean hasHoney = false;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            for (Ingredient ing : result.ingredients()) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                if (RecipeHelper.ingredientContainsItem(ing, "minecraft:honey_bottle", client)) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    hasHoney = true;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    break;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$            if (caneEntry != null && honeyEntry != null) break;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (hasHoney) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                if (honeyEntry == null) honeyEntry = result;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            } else if (caneEntry == null) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                caneEntry = result;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        String preferred = PREFERRED_RECIPE_TYPES.get("minecraft:sugar");
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$        RecipeDisplayEntry chosen = "crafting_honey_bottle".equals(preferred) ? honeyEntry : caneEntry;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$        if (chosen == null) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$            chosen = caneEntry != null ? caneEntry : honeyEntry;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$        }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$        if (chosen == null || recipeType == null) return null;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$        List<ItemStack> resultStacks = chosen.getStacks(map);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$        if (resultStacks.isEmpty()) return null;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        RecipeHelper.RecipeLookupResult chosen = "crafting_honey_bottle".equals(preferred) ? honeyEntry : caneEntry;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (chosen == null) chosen = caneEntry != null ? caneEntry : honeyEntry;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (chosen == null) return null;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        depth++;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        RecipeTreeNode node = processRecipe(chosen.ingredients(), chosen.resultCount(), count,
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        RecipeTreeNode node = processRecipe(chosen.craftingRequirements().get(),
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                item, sourceDesc, "crafting_shapeless", client, chosen, visited);
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$                resultStacks.getFirst().getCount(), count, item, sourceDesc, recipeType, map, chosen.display());
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        depth--;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$        // 仅当两种配方都存在时才提供切换项
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        if (node != null && caneEntry != null && honeyEntry != null) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            node.alternativeRecipeTypes = List.of("crafting_honey_bottle");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        return node;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$
//#endif
//#if MC == 12111
//$$    /**
//$$     * 糖特殊处理：默认使用甘蔗配方，可通过点击工作台图标切换为蜂蜜瓶配方。
//$$     * 两条配方同为无序合成，无法用配方类型区分，故按原料是否含蜂蜜瓶来挑选，
//$$     * 并以伪类型 "crafting_honey_bottle" 作为备选项。
//$$     */
//#endif
//#if MC == 12111
//$$    private RecipeTreeNode tryTraceSugar(int count, RegistryEntry<Item> item, String sourceDesc) {
//#endif
//#if MC == 12111
//$$        MinecraftClient client = MinecraftClient.getInstance();
//#endif
//#if MC == 12111
//$$        if (client.world == null) return null;
//#endif
//#if MC == 12111
//$$        ContextParameterMap map = RecipeBookUtils.getMap(client);
//#endif
//#if MC == 12111
//$$        ItemStack shadow = new ItemStack(item.value());
//#endif
//#if MC == 12111
//$$
//#endif
//#if MC == 12111
//$$        RecipeDisplayEntry caneEntry = null;
//#endif
//#if MC == 12111
//$$        RecipeDisplayEntry honeyEntry = null;
//#endif
//#if MC == 12111
//$$        RecipeBookUtils.Type recipeType = null;
//#endif
//#if MC == 12111
//$$        for (List<RecipeBookUtils.Type> typeGroup : recipePriority) {
//#endif
//#if MC == 12111
//$$            List<Pair<NetworkRecipeId, RecipeDisplayEntry>> lookup =
//#endif
//#if MC == 12111
//$$                    RecipeBookUtils.getDisplayEntryFromRecipeBook(shadow, typeGroup);
//#endif
//#if MC == 12111
//$$            for (Pair<NetworkRecipeId, RecipeDisplayEntry> pair : lookup) {
//#endif
//#if MC == 12111
//$$                RecipeDisplayEntry entry = pair.getRight();
//#endif
//#if MC == 12111
//$$                if (entry.craftingRequirements().isEmpty()) continue;
//#endif
//#if MC == 12111
//$$                if (containsIngredient(entry.craftingRequirements().get(), Items.HONEY_BOTTLE, map)) {
//#endif
//#if MC == 12111
//$$                    if (honeyEntry == null) {
//#endif
//#if MC == 12111
//$$                        honeyEntry = entry;
//#endif
//#if MC == 12111
//$$                        recipeType = typeGroup.get(0);
//#endif
//#if MC == 12111
//$$                    }
//#endif
//#if MC == 12111
//$$                } else if (caneEntry == null) {
//#endif
//#if MC == 12111
//$$                    caneEntry = entry;
//#endif
//#if MC == 12111
//$$                    recipeType = typeGroup.get(0);
//#endif
//#if MC == 12111
//$$                }
//#endif
//#if MC == 12111
//$$            }
//#endif
//#if MC == 12111
//$$            if (caneEntry != null && honeyEntry != null) break;
//#endif
//#if MC == 12111
//$$        }
//#endif
//#if MC == 12111
//$$
//#endif
//#if MC == 12111
//$$        String preferred = PREFERRED_RECIPE_TYPES.get("minecraft:sugar");
//#endif
//#if MC == 12111
//$$        RecipeDisplayEntry chosen = "crafting_honey_bottle".equals(preferred) ? honeyEntry : caneEntry;
//#endif
//#if MC == 12111
//$$        if (chosen == null) {
//#endif
//#if MC == 12111
//$$            chosen = caneEntry != null ? caneEntry : honeyEntry;
//#endif
//#if MC == 12111
//$$        }
//#endif
//#if MC == 12111
//$$        if (chosen == null || recipeType == null) return null;
//#endif
//#if MC == 12111
//$$
//#endif
//#if MC == 12111
//$$        List<ItemStack> resultStacks = chosen.getStacks(map);
//#endif
//#if MC == 12111
//$$        if (resultStacks.isEmpty()) return null;
//#endif
//#if MC == 12111
//$$
//#endif
//#if MC == 12111
//$$        depth++;
//#endif
//#if MC == 12111
//$$        RecipeTreeNode node = processRecipe(chosen.craftingRequirements().get(),
//#endif
//#if MC == 12111
//$$                resultStacks.getFirst().getCount(), count, item, sourceDesc, recipeType, map, chosen.display());
//#endif
//#if MC == 12111
//$$        depth--;
//#endif
//#if MC == 12111
//$$        // 仅当两种配方都存在时才提供切换项
//#endif
//#if MC == 12111
//$$        if (node != null && caneEntry != null && honeyEntry != null) {
//#endif
//#if MC == 12111
//$$            node.alternativeRecipeTypes = List.of("crafting_honey_bottle");
//#endif
//#if MC == 12111
//$$        }
//#endif
//#if MC == 12111
//$$        return node;
//#endif
//#if MC == 12111
//$$    }
//#endif
//#if MC == 12111
//$$
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private RecipeTreeNode tryTraceSmoothStoneSlab(String itemIdStr, int count, ContextParameterMap map) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    private RecipeTreeNode tryTraceSmoothStoneSlab(String itemIdStr, int count, MinecraftClient client, Set<String> visited) {
//#elseif MC == 12101
//$$    private RecipeTreeNode tryTraceSmoothStoneSlab(String itemIdStr, int count, RegistryWrapper.WrapperLookup map) {
//#endif
        String preferred = PREFERRED_RECIPE_TYPES.get(itemIdStr);
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        List<List<RecipeBookUtils.Type>> orderedPriority = recipePriority;
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        List<List<String>> orderedPriority = recipePriority;
//#endif
        if (preferred != null) {
            orderedPriority = new ArrayList<>(recipePriority);
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            RecipeBookUtils.Type prefType = jsonToRecipeType(preferred);
//#endif
            for (int i = 0; i < orderedPriority.size(); i++) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (orderedPriority.get(i).contains(prefType)) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    if (i > 0) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        List<RecipeBookUtils.Type> group = orderedPriority.remove(i);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        orderedPriority.add(0, group);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                if (groupMatchesPreferred(orderedPriority.get(i), preferred)) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    if (i > 0) { List<String> g = orderedPriority.remove(i); orderedPriority.add(0, g); }
//#endif
                    break;
                }
            }
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        RecipeDisplayEntry primaryEntry = null;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        RecipeHelper.RecipeLookupResult primaryResult = null;
//#endif
        List<Ingredient> primaryIngredients = null;
        int primaryResultCount = 0;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        RecipeBookUtils.Type primaryType = null;
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        String primaryType = null;
//#endif
        List<String> alternativeTypes = new ArrayList<>();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        for (List<RecipeBookUtils.Type> typeGroup : orderedPriority) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            ItemStack shadow = new ItemStack(Registries.ITEM.get(Identifier.of(itemIdStr)));
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            List<Pair<NetworkRecipeId, RecipeDisplayEntry>> lookup =
//#elseif MC == 12101
//$$            List<Pair<Identifier, RecipeDisplayEntry>> lookup =
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    RecipeBookUtils.getDisplayEntryFromRecipeBook(shadow, typeGroup);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (lookup.isEmpty()) continue;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            Pair<NetworkRecipeId, RecipeDisplayEntry> pair = lookup.getFirst();
//#elseif MC == 12101
//$$            Pair<Identifier, RecipeDisplayEntry> pair = lookup.getFirst();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            RecipeDisplayEntry entry = pair.getRight();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (entry.craftingRequirements().isEmpty()) continue;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            String typeName = recipeTypeToJson(typeGroup.get(0));
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (primaryEntry == null) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                primaryEntry = entry;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                primaryIngredients = entry.craftingRequirements().get();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                List<ItemStack> resultStacks = entry.getStacks(map);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (resultStacks.isEmpty()) { primaryEntry = null; continue; }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                primaryResultCount = resultStacks.getFirst().getCount();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                primaryType = typeGroup.get(0);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            } else if (!alternativeTypes.contains(typeName)) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                alternativeTypes.add(typeName);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        for (List<String> typeGroup : orderedPriority) {
//#endif
//#if MC == 11701
//$$            ItemStack shadow = new ItemStack(Registry.ITEM.get(Identifier.tryParse(itemIdStr)));
//#elseif MC == 11904 || MC == 12001
//$$            ItemStack shadow = new ItemStack(Registries.ITEM.get(Identifier.tryParse(itemIdStr)));
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            RecipeHelper.RecipeLookupResult lookup = RecipeHelper.lookupRecipe(shadow, typeGroup, client);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (lookup == null) continue;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (primaryResult == null) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                primaryResult = lookup; primaryIngredients = lookup.ingredients();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                primaryResultCount = lookup.resultCount(); primaryType = typeGroup.get(0);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            } else {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                String tn = RecipeHelper.recipeTypeToJson(typeGroup.get(0));
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                if (!alternativeTypes.contains(tn)) alternativeTypes.add(tn);
//#endif
            }
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (primaryEntry != null) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (primaryResult != null) {
//#endif
            depth++;
            RecipeTreeNode node = processRecipe(primaryIngredients, primaryResultCount, count,
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    Registries.ITEM.get(Identifier.of(itemIdStr)).getRegistryEntry(),
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    "平滑石半砖溯源", primaryType, map, primaryEntry.display());
//#endif
//#if MC == 11701
//$$                    getEntry(Registry.ITEM.get(Identifier.tryParse(itemIdStr))), "平滑石半砖溯源", primaryType, client, primaryResult, visited);
//#elseif MC == 11904 || MC == 12001
//$$                    Registries.ITEM.get(Identifier.tryParse(itemIdStr)).getRegistryEntry(), "平滑石半砖溯源", primaryType, client, primaryResult, visited);
//#endif
            depth--;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (node != null && !alternativeTypes.isEmpty()) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                node.alternativeRecipeTypes = alternativeTypes;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (node != null && !alternativeTypes.isEmpty()) node.alternativeRecipeTypes = alternativeTypes;
//#endif
            return node;
        }
        return null;
    }

//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private RecipeTreeNode tryTraceChiseledQuartzBlock(String itemIdStr, int count, ContextParameterMap map) {
//#elseif MC == 12101
//$$    private RecipeTreeNode tryTraceChiseledQuartzBlock(String itemIdStr, int count, RegistryWrapper.WrapperLookup map) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 优先使用石英块作为原料的配方（切石机或工作台），而非石英台阶
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    private RecipeTreeNode tryTraceChiseledQuartzBlock(String itemIdStr, int count, MinecraftClient client, Set<String> visited) {
//#endif
        String preferred = PREFERRED_RECIPE_TYPES.get(itemIdStr);
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        List<List<RecipeBookUtils.Type>> orderedPriority = recipePriority;
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        List<List<String>> orderedPriority = recipePriority;
//#endif
        if (preferred != null) {
            orderedPriority = new ArrayList<>(recipePriority);
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            RecipeBookUtils.Type prefType = jsonToRecipeType(preferred);
//#endif
            for (int i = 0; i < orderedPriority.size(); i++) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (orderedPriority.get(i).contains(prefType)) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    if (i > 0) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        List<RecipeBookUtils.Type> group = orderedPriority.remove(i);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        orderedPriority.add(0, group);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                if (groupMatchesPreferred(orderedPriority.get(i), preferred)) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    if (i > 0) { List<String> g = orderedPriority.remove(i); orderedPriority.add(0, g); }
//#endif
                    break;
                }
            }
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        RecipeDisplayEntry primaryEntry = null;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        RecipeHelper.RecipeLookupResult primaryResult = null;
//#endif
        List<Ingredient> primaryIngredients = null;
        int primaryResultCount = 0;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        RecipeBookUtils.Type primaryType = null;
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        String primaryType = null;
//#endif
        List<String> alternativeTypes = new ArrayList<>();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        for (List<RecipeBookUtils.Type> typeGroup : orderedPriority) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            ItemStack shadow = new ItemStack(Registries.ITEM.get(Identifier.of(itemIdStr)));
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            List<Pair<NetworkRecipeId, RecipeDisplayEntry>> lookup =
//#elseif MC == 12101
//$$            List<Pair<Identifier, RecipeDisplayEntry>> lookup =
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    RecipeBookUtils.getDisplayEntryFromRecipeBook(shadow, typeGroup);
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            for (Pair<NetworkRecipeId, RecipeDisplayEntry> pair : lookup) {
//#elseif MC == 12101
//$$            for (Pair<Identifier, RecipeDisplayEntry> pair : lookup) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                RecipeDisplayEntry entry = pair.getRight();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (entry.craftingRequirements().isPresent()) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    List<Ingredient> ingredients = entry.craftingRequirements().get();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    // 检查原料是否包含石英块（而非石英台阶）
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    boolean usesQuartzBlock = false;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    for (Ingredient ing : ingredients) {
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        SlotDisplay display = ing.toDisplay();
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        List<ItemStack> stacks = display.getStacks(map);
//#endif
//#if MC == 12101
//$$                        List<ItemStack> stacks = RecipeBookUtils.getIngredientStacks(ing);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        for (ItemStack stack : stacks) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            String id = Registries.ITEM.getId(stack.getItem()).toString();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            if ("minecraft:quartz_block".equals(id)) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                usesQuartzBlock = true;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                break;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        if (usesQuartzBlock) break;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    if (usesQuartzBlock) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        String typeName = recipeTypeToJson(typeGroup.get(0));
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        if (primaryEntry == null) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            primaryEntry = entry;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            primaryIngredients = ingredients;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            List<ItemStack> resultStacks = entry.getStacks(map);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            if (resultStacks.isEmpty()) { primaryEntry = null; continue; }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            primaryResultCount = resultStacks.getFirst().getCount();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            primaryType = typeGroup.get(0);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        } else if (!alternativeTypes.contains(typeName)) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            alternativeTypes.add(typeName);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        for (List<String> typeGroup : orderedPriority) {
//#endif
//#if MC == 11701
//$$            ItemStack shadow = new ItemStack(Registry.ITEM.get(Identifier.tryParse(itemIdStr)));
//#elseif MC == 11904 || MC == 12001
//$$            ItemStack shadow = new ItemStack(Registries.ITEM.get(Identifier.tryParse(itemIdStr)));
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            List<RecipeHelper.RecipeLookupResult> allResults = RecipeHelper.lookupAllRecipes(shadow, typeGroup, client);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            for (RecipeHelper.RecipeLookupResult lookup : allResults) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                boolean usesQuartzBlock = false;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                for (Ingredient ing : lookup.ingredients()) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    if (RecipeHelper.ingredientContainsItem(ing, "minecraft:quartz_block", client)) { usesQuartzBlock = true; break; }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                if (usesQuartzBlock) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    String tn = RecipeHelper.recipeTypeToJson(typeGroup.get(0));
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    if (primaryResult == null) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        primaryResult = lookup; primaryIngredients = lookup.ingredients();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        primaryResultCount = lookup.resultCount(); primaryType = typeGroup.get(0);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    } else if (!alternativeTypes.contains(tn)) alternativeTypes.add(tn);
//#endif
                }
            }
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (primaryEntry != null) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (primaryResult != null) {
//#endif
            depth++;
            RecipeTreeNode node = processRecipe(primaryIngredients, primaryResultCount, count,
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    Registries.ITEM.get(Identifier.of(itemIdStr)).getRegistryEntry(),
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    "雕纹石英溯源", primaryType, map, primaryEntry.display());
//#endif
//#if MC == 11701
//$$                    getEntry(Registry.ITEM.get(Identifier.tryParse(itemIdStr))), "雕纹石英溯源", primaryType, client, primaryResult, visited);
//#elseif MC == 11904 || MC == 12001
//$$                    Registries.ITEM.get(Identifier.tryParse(itemIdStr)).getRegistryEntry(), "雕纹石英溯源", primaryType, client, primaryResult, visited);
//#endif
            depth--;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (node != null && !alternativeTypes.isEmpty()) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                node.alternativeRecipeTypes = alternativeTypes;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (node != null && !alternativeTypes.isEmpty()) node.alternativeRecipeTypes = alternativeTypes;
//#endif
            return node;
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 没有石英块配方，回退到默认行为
//#endif
        return null;
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 染色床特殊处理：查找染色配方（染料+白色床）。
     */
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 白色床/白色羊毛专用：跳过"白染料+彩色床/羊毛→白色"的回染配方，避免循环分解。
     * 只接受不含床（白羊毛时还不含白色染料）的合成配方：白床=羊毛x3+木板x3，白羊毛=线x4。
     */
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private RecipeTreeNode tryTraceWhiteDyeable(String itemIdStr, int count, RegistryEntry<Item> item,
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            String sourceDesc, ContextParameterMap map) {
//#elseif MC == 12101
//$$            String sourceDesc, RegistryWrapper.WrapperLookup map) {
//#endif
//#if MC == 11701
//$$    private RecipeTreeNode tryTraceBedDyeing(String itemIdStr, int count, Item item, String sourceDesc, MinecraftClient client, Set<String> visited) {
//#elseif MC == 11904 || MC == 12001
//$$    private RecipeTreeNode tryTraceBedDyeing(String itemIdStr, int count, RegistryEntry<Item> item, String sourceDesc, MinecraftClient client, Set<String> visited) {
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        ItemStack shadow = new ItemStack(item.value());
//#elseif MC == 11701
//$$        ItemStack shadow = new ItemStack(item);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        boolean isWhiteWool = "minecraft:white_wool".equals(itemIdStr);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        for (List<RecipeBookUtils.Type> typeGroup : recipePriority) {
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            List<Pair<NetworkRecipeId, RecipeDisplayEntry>> lookup =
//#elseif MC == 12101
//$$            List<Pair<Identifier, RecipeDisplayEntry>> lookup =
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    RecipeBookUtils.getDisplayEntryFromRecipeBook(shadow, typeGroup);
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            for (Pair<NetworkRecipeId, RecipeDisplayEntry> pair : lookup) {
//#elseif MC == 12101
//$$            for (Pair<Identifier, RecipeDisplayEntry> pair : lookup) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                RecipeDisplayEntry entry = pair.getRight();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (!entry.craftingRequirements().isPresent()) continue;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                List<Ingredient> ingredients = entry.craftingRequirements().get();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                List<ItemStack> resultStacks = entry.getStacks(map);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (resultStacks.isEmpty()) continue;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                int resultCount = resultStacks.getFirst().getCount();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                boolean isRedye = false;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                for (Ingredient ing : ingredients) {
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    SlotDisplay display = ing.toDisplay();
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    List<ItemStack> stacks = display.getStacks(map);
//#endif
//#if MC == 12101
//$$                    List<ItemStack> stacks = RecipeBookUtils.getIngredientStacks(ing);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    for (ItemStack stack : stacks) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        String id = Registries.ITEM.getId(stack.getItem()).toString();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        if (id.endsWith("_bed") || (isWhiteWool && id.equals("minecraft:white_dye"))) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            isRedye = true;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            break;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    if (isRedye) break;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        for (List<String> typeGroup : recipePriority) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            List<RecipeHelper.RecipeLookupResult> allResults = RecipeHelper.lookupAllRecipes(shadow, typeGroup, client);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            for (RecipeHelper.RecipeLookupResult lookup : allResults) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                boolean hasBed = false;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                for (Ingredient ing : lookup.ingredients()) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    if (RecipeHelper.ingredientContainsBed(ing, client)) { hasBed = true; break; }
//#endif
                }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (!isRedye) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                if (hasBed) {
//#endif
                    depth++;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    RecipeTreeNode node = processWildcardRecipe(ingredients, resultCount,
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            count, item, sourceDesc, typeGroup.get(0), map);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    RecipeTreeNode node = processWildcardRecipe(lookup.ingredients(), lookup.resultCount(), count, item, sourceDesc, typeGroup.get(0), client, visited);
//#endif
                    depth--;
                    return node;
                }
            }
        }
        return null;
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private RecipeTreeNode tryTraceBedDyeing(String itemIdStr, int count, RegistryEntry<Item> item,
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            String sourceDesc, ContextParameterMap map) {
//#elseif MC == 12101
//$$            String sourceDesc, RegistryWrapper.WrapperLookup map) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    /**
//$$     * 白色床/白色羊毛专用：优先选择「合成配方」而非「回染配方」。
//$$     * 白床回染配方（白染料+任意彩色床→白床）与白羊毛回染配方（白染料+任意彩色羊毛→白羊毛）
//$$     * 会造成循环分解（白床→黑床+白染料→…）；这里只接受不含床/染料的配方，
//$$     * 即 羊毛x3+木板x3 与 线x4 的合成配方。
//$$     */
//#endif
//#if MC == 11701
//$$    private RecipeTreeNode tryTraceWhiteDyeable(String itemIdStr, int count, Item item,
//#elseif MC == 11904 || MC == 12001
//$$    private RecipeTreeNode tryTraceWhiteDyeable(String itemIdStr, int count, RegistryEntry<Item> item,
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            String sourceDesc, MinecraftClient client, Set<String> visited) {
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        ItemStack shadow = new ItemStack(item.value());
//#elseif MC == 11701
//$$        ItemStack shadow = new ItemStack(item);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        for (List<RecipeBookUtils.Type> typeGroup : recipePriority) {
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            List<Pair<NetworkRecipeId, RecipeDisplayEntry>> lookup =
//#elseif MC == 12101
//$$            List<Pair<Identifier, RecipeDisplayEntry>> lookup =
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    RecipeBookUtils.getDisplayEntryFromRecipeBook(shadow, typeGroup);
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            for (Pair<NetworkRecipeId, RecipeDisplayEntry> pair : lookup) {
//#elseif MC == 12101
//$$            for (Pair<Identifier, RecipeDisplayEntry> pair : lookup) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                RecipeDisplayEntry entry = pair.getRight();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (entry.craftingRequirements().isPresent()) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    List<Ingredient> ingredients = entry.craftingRequirements().get();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    List<ItemStack> resultStacks = entry.getStacks(map);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    if (resultStacks.isEmpty()) continue;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    int resultCount = resultStacks.getFirst().getCount();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    // 检查原料中是否包含白色床（确认是染色配方）
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    boolean hasBed = false;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    for (Ingredient ing : ingredients) {
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        SlotDisplay display = ing.toDisplay();
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        List<ItemStack> stacks = display.getStacks(map);
//#endif
//#if MC == 12101
//$$                        List<ItemStack> stacks = RecipeBookUtils.getIngredientStacks(ing);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        for (ItemStack stack : stacks) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            String id = Registries.ITEM.getId(stack.getItem()).toString();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            if (id.endsWith("_bed")) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                hasBed = true;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                break;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        if (hasBed) break;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    if (hasBed) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        depth++;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        RecipeTreeNode node = processWildcardRecipe(ingredients, resultCount,
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                count, item, sourceDesc, typeGroup.get(0), map);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        depth--;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        return node;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        boolean isWhiteWool = "minecraft:white_wool".equals(itemIdStr);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        for (List<String> typeGroup : recipePriority) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            List<RecipeHelper.RecipeLookupResult> allResults = RecipeHelper.lookupAllRecipes(shadow, typeGroup, client);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            for (RecipeHelper.RecipeLookupResult lookup : allResults) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                boolean isRedye = false;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                for (Ingredient ing : lookup.ingredients()) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    if (RecipeHelper.ingredientContainsBed(ing, client)) { isRedye = true; break; }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    if (isWhiteWool && RecipeHelper.ingredientContainsItem(ing, "minecraft:white_dye", client)) { isRedye = true; break; }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                if (!isRedye) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    depth++;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    RecipeTreeNode node = processWildcardRecipe(lookup.ingredients(), lookup.resultCount(), count, item,
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                            sourceDesc, typeGroup.get(0), client, visited);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    depth--;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    return node;
//#endif
                }
            }
        }
        return null;
//#if MC == 12110
    }
//#endif
//#if MC == 12110

//#endif
//#if MC == 12110
    /**
     * 糖特殊处理：默认使用甘蔗配方，可通过点击工作台图标切换为蜂蜜瓶配方。
     * 两条配方同为 crafting_shapeless 且 group 相同，无法用配方类型区分，
     * 故按原料是否含蜂蜜瓶来挑选，并以伪类型 "crafting_honey_bottle" 作为备选项。
     */
//#endif
//#if MC == 12110
    private RecipeTreeNode tryTraceSugar(int count, RegistryEntry<Item> item, String sourceDesc) {
//#endif
//#if MC == 12110
        MinecraftClient client = MinecraftClient.getInstance();
//#endif
//#if MC == 12110
        if (client.world == null) return null;
//#endif
//#if MC == 12110
        ContextParameterMap map = RecipeBookUtils.getMap(client);
//#endif
//#if MC == 12110
        ItemStack shadow = new ItemStack(item.value());
//#endif
//#if MC == 12110

//#endif
//#if MC == 12110
        RecipeDisplayEntry caneEntry = null;
//#endif
//#if MC == 12110
        RecipeDisplayEntry honeyEntry = null;
//#endif
//#if MC == 12110
        RecipeBookUtils.Type recipeType = null;
//#endif
//#if MC == 12110
        for (List<RecipeBookUtils.Type> typeGroup : recipePriority) {
//#endif
//#if MC == 12110
            List<Pair<NetworkRecipeId, RecipeDisplayEntry>> lookup =
//#endif
//#if MC == 12110
                    RecipeBookUtils.getDisplayEntryFromRecipeBook(shadow, typeGroup);
//#endif
//#if MC == 12110
            for (Pair<NetworkRecipeId, RecipeDisplayEntry> pair : lookup) {
//#endif
//#if MC == 12110
                RecipeDisplayEntry entry = pair.getRight();
//#endif
//#if MC == 12110
                if (entry.craftingRequirements().isEmpty()) continue;
//#endif
//#if MC == 12110
                if (containsIngredient(entry.craftingRequirements().get(), Items.HONEY_BOTTLE, map)) {
//#endif
//#if MC == 12110
                    if (honeyEntry == null) {
//#endif
//#if MC == 12110
                        honeyEntry = entry;
//#endif
//#if MC == 12110
                        recipeType = typeGroup.get(0);
//#endif
//#if MC == 12110
                    }
//#endif
//#if MC == 12110
                } else if (caneEntry == null) {
//#endif
//#if MC == 12110
                    caneEntry = entry;
//#endif
//#if MC == 12110
                    recipeType = typeGroup.get(0);
//#endif
//#if MC == 12110
                }
//#endif
//#if MC == 12110
            }
//#endif
//#if MC == 12110
            if (caneEntry != null && honeyEntry != null) break;
//#endif
//#if MC == 12110
        }
//#endif
//#if MC == 12110

//#endif
//#if MC == 12110
        String preferred = PREFERRED_RECIPE_TYPES.get("minecraft:sugar");
//#endif
//#if MC == 12110
        RecipeDisplayEntry chosen = "crafting_honey_bottle".equals(preferred) ? honeyEntry : caneEntry;
//#endif
//#if MC == 12110
        if (chosen == null) {
//#endif
//#if MC == 12110
            chosen = caneEntry != null ? caneEntry : honeyEntry;
//#endif
//#if MC == 12110
        }
//#endif
//#if MC == 12110
        if (chosen == null || recipeType == null) return null;
//#endif
//#if MC == 12110

//#endif
//#if MC == 12110
        List<ItemStack> resultStacks = chosen.getStacks(map);
//#endif
//#if MC == 12110
        if (resultStacks.isEmpty()) return null;
//#endif
//#if MC == 12110

//#endif
//#if MC == 12110
        depth++;
//#endif
//#if MC == 12110
        RecipeTreeNode node = processRecipe(chosen.craftingRequirements().get(),
//#endif
//#if MC == 12110
                resultStacks.getFirst().getCount(), count, item, sourceDesc, recipeType, map, chosen.display());
//#endif
//#if MC == 12110
        depth--;
//#endif
//#if MC == 12110
        // 仅当两种配方都存在时才提供切换项
//#endif
//#if MC == 12110
        if (node != null && caneEntry != null && honeyEntry != null) {
//#endif
//#if MC == 12110
            node.alternativeRecipeTypes = List.of("crafting_honey_bottle");
//#endif
//#if MC == 12110
        }
//#endif
//#if MC == 12110
        return node;
//#endif
    }

    private RecipeTreeNode processWildcardRecipe(List<Ingredient> ingredients, int resultCount, int total,
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            RegistryEntry<Item> inputItem, String sourceDesc, RecipeBookUtils.Type recipeType,
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            ContextParameterMap map) {
//#elseif MC == 12101
//$$            RegistryWrapper.WrapperLookup map) {
//#endif
//#if MC == 11701
//$$            Item inputItem, String sourceDesc, String recipeType, MinecraftClient client, Set<String> visited) {
//#elseif MC == 11904 || MC == 12001
//$$            RegistryEntry<Item> inputItem, String sourceDesc, String recipeType, MinecraftClient client, Set<String> visited) {
//#endif
        int adjustedTotal = total;
        if (resultCount > 1) {
            Fraction adjusted = Fraction.getFraction(total, resultCount);
            int floor = MathHelper.floor(adjusted.floatValue());
            float remainderCalc = resultCount * (adjusted.floatValue() - floor);
            int remainderCount = Math.round(remainderCalc);
            adjustedTotal = Math.max(floor + (remainderCount > 0 ? 1 : 0), (remainderCount > 0 ? 1 : 0));
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 计算冗余
//#endif
        int actualOutput = adjustedTotal * resultCount;
        int excess = actualOutput - total;
        if (excess > 0) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            String itemId = inputItem.getIdAsString();
//#elseif MC == 11701
//$$            String itemId = Registry.ITEM.getId(inputItem).toString();
//#elseif MC == 11904 || MC == 12001
//$$            String itemId = Registries.ITEM.getId(inputItem.value()).toString();
//#endif
            String desc = sourceDesc + " (冗余: 需要" + total + "个, 产出" + actualOutput + "个)";
            redundancy.add(new MaterialDataStore.RawMaterialEntry(itemId, excess, List.of(desc)));
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Map<RegistryEntry<Item>, Integer> dedup = new LinkedHashMap<>();
//#elseif MC == 11701
//$$        Map<Item, Integer> dedup = new LinkedHashMap<>();
//#endif
        for (Ingredient ing : ingredients) {
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            SlotDisplay display = ing.toDisplay();
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            List<ItemStack> displayStacks = display.getStacks(map);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            List<ItemStack> displayStacks = RecipeHelper.getIngredientStacks(ing, client);
//#elseif MC == 12101
//$$            List<ItemStack> displayStacks = RecipeBookUtils.getIngredientStacks(ing);
//#endif
            if (displayStacks.isEmpty()) continue;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            RegistryEntry<Item> ingItem;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 对于染色类配方，如果原料匹配了多个变种（如羊毛），优先选择白色/默认变种
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (displayStacks.size() > 1) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                ingItem = pickBaseDyeableIngredient(displayStacks);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            } else {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                ingItem = displayStacks.getFirst().getRegistryEntry();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 11701
//$$            Item ingItem = displayStacks.size() > 1 ? pickBaseDyeableIngredient(displayStacks) : getEntry(displayStacks.get(0));
//#elseif MC == 11904 || MC == 12001
//$$            RegistryEntry<Item> ingItem = displayStacks.size() > 1 ? pickBaseDyeableIngredient(displayStacks) : displayStacks.get(0).getRegistryEntry();
//#endif
            dedup.merge(ingItem, adjustedTotal, Integer::sum);
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
        List<RecipeTreeNode> subMaterials = new ArrayList<>();
        for (var entry : dedup.entrySet()) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            String childId = entry.getKey().getIdAsString();
//#elseif MC == 11701
//$$            String childId = Registry.ITEM.getId(entry.getKey()).toString();
//#elseif MC == 11904 || MC == 12001
//$$            String childId = Registries.ITEM.getId(entry.getKey().value()).toString();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            String parentId = inputItem.getIdAsString();
//#elseif MC == 11701
//$$            String parentId = Registry.ITEM.getId(inputItem).toString();
//#elseif MC == 11904 || MC == 12001
//$$            String parentId = Registries.ITEM.getId(inputItem.value()).toString();
//#endif
            // 苔石/苔石砖配方中的圆石是固定材料（黑石/深板岩圆石不能用于该配方），不参与通配替换
            boolean fixedMossyCobblestone = isMossyItem(parentId)
                    && "minecraft:cobblestone".equals(childId);
            // 只有溯源物品处于通配成品白名单中，其特定材料才允许标记为通配子材料
            boolean parentWildcard = WILDCARD_ITEM_IDS.contains(parentId) && !fixedMossyCobblestone;
            if (!fixedMossyCobblestone) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 优先检查按上下文（父物品）的替换
//#endif
                String contextReplaced = getContextWildcardReplacement(parentId, childId);
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (!contextReplaced.equals(childId)) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    childId = contextReplaced;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                } else {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    // 回退到全局同步替换
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    String replacedId = getWildcardReplacement(childId);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    if (!replacedId.equals(childId)) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        childId = replacedId;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                if (!contextReplaced.equals(childId)) childId = contextReplaced;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                else { String replacedId = getWildcardReplacement(childId); if (!replacedId.equals(childId)) childId = replacedId; }
//#endif
                // 防自环：替换后与当前溯源物品相同（如羊毛被替换为与染料同色的羊毛），回退为原始材料
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (childId.equals(parentId)) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    childId = entry.getKey().getIdAsString();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                }
//#endif
//#if MC == 11701
//$$                if (childId.equals(parentId)) childId = Registry.ITEM.getId(entry.getKey()).toString();
//#elseif MC == 11904 || MC == 12001
//$$                if (childId.equals(parentId)) childId = Registries.ITEM.getId(entry.getKey().value()).toString();
//#endif
            }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (isWildcardMaterial(entry.getKey().getIdAsString())) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 通配原材料，继续溯源但标记为可替换（如木板→原木）
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 如果 childId 被替换了，使用替换后的物品进行溯源
//#endif
//#if MC == 11701
//$$            if (isWildcardMaterial(Registry.ITEM.getId(entry.getKey()).toString())) {
//#elseif MC == 11904 || MC == 12001
//$$            if (isWildcardMaterial(Registries.ITEM.getId(entry.getKey().value()).toString())) {
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                RegistryEntry<Item> traceEntry = entry.getKey();
//#elseif MC == 11701
//$$                Item traceEntry = entry.getKey();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                String originalId = entry.getKey().getIdAsString();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (!childId.equals(originalId)) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    Item replacedItem = Registries.ITEM.get(Identifier.of(childId));
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    traceEntry = replacedItem.getRegistryEntry();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                RecipeTreeNode sub = traceItem(traceEntry, entry.getValue(), inputItem, sourceDesc);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (sub != null) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    sub.wildcardSubMaterial = parentWildcard;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    subMaterials.add(sub);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                } else {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    RecipeTreeNode base = RecipeTreeNode.base(entry.getKey().getIdAsString(), entry.getValue());
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    base.wildcardSubMaterial = parentWildcard;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    subMaterials.add(base);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                }
//#endif
//#if MC == 11701
//$$                String originalId = Registry.ITEM.getId(entry.getKey()).toString();
//#elseif MC == 11904 || MC == 12001
//$$                String originalId = Registries.ITEM.getId(entry.getKey().value()).toString();
//#endif
//#if MC == 11701
//$$                if (!childId.equals(originalId)) { Item ri = Registry.ITEM.get(Identifier.tryParse(childId)); if (ri != null) traceEntry = getEntry(ri); }
//#elseif MC == 11904 || MC == 12001
//$$                if (!childId.equals(originalId)) { Item ri = Registries.ITEM.get(Identifier.tryParse(childId)); traceEntry = ri.getRegistryEntry(); }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                boolean isChiseledBookshelfSlab = "minecraft:chiseled_bookshelf".equals(parentId) && childId.endsWith("_slab");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                RecipeTreeNode sub = traceItem(traceEntry, entry.getValue(), inputItem, sourceDesc, visited);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                if (sub != null) { sub.wildcardSubMaterial = parentWildcard && !isChiseledBookshelfSlab; subMaterials.add(sub); }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                else { RecipeTreeNode base = RecipeTreeNode.base(childId, entry.getValue()); base.wildcardSubMaterial = parentWildcard && !isChiseledBookshelfSlab; subMaterials.add(base); }
//#endif
            } else {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 非通配子材料，继续向上溯源
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 如果 childId 被替换了，使用替换后的物品进行溯源
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                RegistryEntry<Item> traceEntry = entry.getKey();
//#elseif MC == 11701
//$$                Item traceEntry = entry.getKey();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                String originalId = entry.getKey().getIdAsString();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (!childId.equals(originalId)) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    Item replacedItem = Registries.ITEM.get(Identifier.of(childId));
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    if (replacedItem != null) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        traceEntry = replacedItem.getRegistryEntry();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                RecipeTreeNode sub = traceItem(traceEntry, entry.getValue(), inputItem, sourceDesc);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (sub != null) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    subMaterials.add(sub);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                }
//#endif
//#if MC == 11701
//$$                String originalId = Registry.ITEM.getId(entry.getKey()).toString();
//#elseif MC == 11904 || MC == 12001
//$$                String originalId = Registries.ITEM.getId(entry.getKey().value()).toString();
//#endif
//#if MC == 11701
//$$                if (!childId.equals(originalId)) { Item ri = Registry.ITEM.get(Identifier.tryParse(childId)); if (ri != null) traceEntry = getEntry(ri); }
//#elseif MC == 11904 || MC == 12001
//$$                if (!childId.equals(originalId)) { Item ri = Registries.ITEM.get(Identifier.tryParse(childId)); if (ri != null) traceEntry = ri.getRegistryEntry(); }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                RecipeTreeNode sub = traceItem(traceEntry, entry.getValue(), inputItem, sourceDesc, visited);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                if (sub != null) subMaterials.add(sub);
//#endif
            }
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        String typeName = recipeTypeToJson(recipeType);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return new RecipeTreeNode(inputItem.getIdAsString(), total, typeName, List.of(), subMaterials);
//#endif
//#if MC == 11701
//$$        return new RecipeTreeNode(Registry.ITEM.getId(inputItem).toString(), total, RecipeHelper.recipeTypeToJson(recipeType), List.of(), subMaterials);
//#elseif MC == 11904 || MC == 12001
//$$        return new RecipeTreeNode(Registries.ITEM.getId(inputItem.value()).toString(), total, RecipeHelper.recipeTypeToJson(recipeType), List.of(), subMaterials);
//#endif
    }

    private RecipeTreeNode processRecipe(List<Ingredient> ingredients, int resultCount, int total,
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            RegistryEntry<Item> inputItem, String sourceDesc, RecipeBookUtils.Type recipeType,
//#elseif MC == 11701
//$$            Item inputItem, String sourceDesc, String recipeType,
//#elseif MC == 11904 || MC == 12001
//$$            RegistryEntry<Item> inputItem, String sourceDesc, String recipeType,
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            ContextParameterMap map, RecipeDisplay display) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            MinecraftClient client, RecipeHelper.RecipeLookupResult lookupResult, Set<String> visited) {
//#elseif MC == 12101
//$$            RegistryWrapper.WrapperLookup map, Recipe<?> display) {
//#endif
        int adjustedTotal = total;
        if (resultCount > 1) {
            Fraction adjusted = Fraction.getFraction(total, resultCount);
            int floor = MathHelper.floor(adjusted.floatValue());
            float remainderCalc = resultCount * (adjusted.floatValue() - floor);
            int remainderCount = Math.round(remainderCalc);
            adjustedTotal = Math.max(floor + (remainderCount > 0 ? 1 : 0), (remainderCount > 0 ? 1 : 0));
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 计算冗余（多合成的部分）
//#endif
        int actualOutput = adjustedTotal * resultCount;
        int excess = actualOutput - total;
        if (excess > 0) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            String itemId = inputItem.getIdAsString();
//#elseif MC == 11701
//$$            String itemId = Registry.ITEM.getId(inputItem).toString();
//#elseif MC == 11904 || MC == 12001
//$$            String itemId = Registries.ITEM.getId(inputItem.value()).toString();
//#endif
            String desc = sourceDesc + " (冗余: 需要" + total + "个, 产出" + actualOutput + "个)";
            redundancy.add(new MaterialDataStore.RawMaterialEntry(itemId, excess, List.of(desc)));
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Map<RegistryEntry<Item>, Integer> dedup = new LinkedHashMap<>();
//#elseif MC == 11701
//$$        Map<Item, Integer> dedup = new LinkedHashMap<>();
//#endif
        for (Ingredient ing : ingredients) {
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            SlotDisplay slotDisplay = ing.toDisplay();
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            List<ItemStack> displayStacks = slotDisplay.getStacks(map);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            List<ItemStack> displayStacks = RecipeHelper.getIngredientStacks(ing, client);
//#elseif MC == 12101
//$$            List<ItemStack> displayStacks = RecipeBookUtils.getIngredientStacks(ing);
//#endif
            if (displayStacks.isEmpty()) continue;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            RegistryEntry<Item> ingItem = displayStacks.getFirst().getRegistryEntry();
//#elseif MC == 11701
//$$            Item ingItem = getEntry(displayStacks.get(0));
//#elseif MC == 11904 || MC == 12001
//$$            RegistryEntry<Item> ingItem = displayStacks.get(0).getRegistryEntry();
//#endif
            dedup.merge(ingItem, adjustedTotal, Integer::sum);
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
        List<RecipeTreeNode> subMaterials = new ArrayList<>();
        for (var entry : dedup.entrySet()) {
            // processRecipe 仅用于非通配成品，子材料不应应用替换逻辑
            // 只有 processWildcardRecipe 才应处理通配替换
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            RecipeTreeNode sub = traceItem(entry.getKey(), entry.getValue(), inputItem, sourceDesc);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            RecipeTreeNode sub = traceItem(entry.getKey(), entry.getValue(), inputItem, sourceDesc, visited);
//#endif
            if (sub != null) {
                subMaterials.add(sub);
            } else {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                RecipeTreeNode base = RecipeTreeNode.base(entry.getKey().getIdAsString(), entry.getValue());
//#elseif MC == 11701
//$$                RecipeTreeNode base = RecipeTreeNode.base(Registry.ITEM.getId(entry.getKey()).toString(), entry.getValue());
//#elseif MC == 11904 || MC == 12001
//$$                RecipeTreeNode base = RecipeTreeNode.base(Registries.ITEM.getId(entry.getKey().value()).toString(), entry.getValue());
//#endif
                subMaterials.add(base);
            }
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        String typeName = recipeTypeToJson(recipeType);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        List<RecipeTreeNode.RecipeDetail> recipeDetails = extractRecipeDetails(display, recipeType, resultCount, map);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return new RecipeTreeNode(inputItem.getIdAsString(), total, typeName, recipeDetails, subMaterials);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        List<RecipeTreeNode.RecipeDetail> recipeDetails = List.of();
//#endif
//#if MC == 11701
//$$        return new RecipeTreeNode(Registry.ITEM.getId(inputItem).toString(), total, RecipeHelper.recipeTypeToJson(recipeType), recipeDetails, subMaterials);
//#elseif MC == 11904 || MC == 12001
//$$        return new RecipeTreeNode(Registries.ITEM.getId(inputItem.value()).toString(), total, RecipeHelper.recipeTypeToJson(recipeType), recipeDetails, subMaterials);
//#endif
    }

//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private List<RecipeTreeNode.RecipeDetail> extractRecipeDetails(RecipeDisplay display,
//#elseif MC == 12101
//$$    private List<RecipeTreeNode.RecipeDetail> extractRecipeDetails(Recipe<?> display,
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            RecipeBookUtils.Type recipeType, int resultCount, ContextParameterMap map) {
//#elseif MC == 12101
//$$            RecipeBookUtils.Type recipeType, int resultCount, RegistryWrapper.WrapperLookup map) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        List<RecipeTreeNode.RecipeDetail> details = new ArrayList<>();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        RecipeTreeNode.RecipeDetail detail = new RecipeTreeNode.RecipeDetail();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        detail.resultCount = resultCount;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (display == null) {
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 兜底查找无 display 信息，仅记录类型
//#elseif MC == 12101
//$$            // 兜底查找无配方信息，仅记录类型
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            detail.type = recipeTypeToJson(recipeType);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            details.add(detail);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            return details;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 使用反射安全地提取配方详情，避免直接依赖可能在不同Yarn映射中命名不同的类
//#elseif MC == 12101
//$$        // 1.21.1 没有 RecipeDisplay 体系，直接按原版配方类型提取详情
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        switch (recipeType) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            case SHAPED -> detail.type = "shaped";
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            case SHAPELESS -> detail.type = "shapeless";
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            case FURNACE -> {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                detail.type = "smelting";
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                tryExtractFurnaceDetails(display, detail, map);
//#elseif MC == 12101
//$$                tryExtractFurnaceDetails(display, detail);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            case SMITHING -> {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                detail.type = "smithing";
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                tryExtractSmithingDetails(display, detail, map);
//#elseif MC == 12101
//$$                tryExtractSmithingDetails(display, detail);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            case STONECUTTER -> {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                detail.type = "stonecutting";
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                tryExtractStonecutterDetails(display, detail, map);
//#elseif MC == 12101
//$$                tryExtractStonecutterDetails(display, detail);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        details.add(detail);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return details;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void tryExtractFurnaceDetails(RecipeDisplay display, RecipeTreeNode.RecipeDetail detail,
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                          ContextParameterMap map) {
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        try {
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            var clazz = display.getClass();
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            var cookingTimeMethod = clazz.getMethod("cookingTime");
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            var experienceMethod = clazz.getMethod("experience");
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            var ingredientMethod = clazz.getMethod("ingredient");
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            detail.cookingTime = (int) cookingTimeMethod.invoke(display);
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            detail.experience = (float) experienceMethod.invoke(display);
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            SlotDisplay slot = (SlotDisplay) ingredientMethod.invoke(display);
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            String itemId = getFirstItemFromSlot(slot, map);
//#endif
//#if MC == 12101
//$$    private void tryExtractFurnaceDetails(Recipe<?> display, RecipeTreeNode.RecipeDetail detail) {
//#endif
//#if MC == 12101
//$$        if (display instanceof AbstractCookingRecipe cooking) {
//#endif
//#if MC == 12101
//$$            detail.cookingTime = cooking.getCookingTime();
//#endif
//#if MC == 12101
//$$            detail.experience = cooking.getExperience();
//#endif
//#if MC == 12101
//$$            String itemId = firstIngredientSlot(cooking.getIngredients());
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (itemId != null) detail.ingredient = new RecipeTreeNode.RecipeSlot(itemId);
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        } catch (Exception e) { logReflectionProbeFailure(e); }
//#elseif MC == 12101
//$$        }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void tryExtractSmithingDetails(RecipeDisplay display, RecipeTreeNode.RecipeDetail detail,
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                           ContextParameterMap map) {
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        try {
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            var clazz = display.getClass();
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            var typeMethod = clazz.getMethod("getType");
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            var typeEnum = (Enum<?>) typeMethod.invoke(display);
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            detail.smithingType = typeEnum.name().toLowerCase();
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            var templateMethod = clazz.getMethod("template");
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            var baseMethod = clazz.getMethod("base");
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            var additionMethod = clazz.getMethod("addition");
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            SlotDisplay templateSlot = (SlotDisplay) templateMethod.invoke(display);
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            SlotDisplay baseSlot = (SlotDisplay) baseMethod.invoke(display);
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            SlotDisplay additionSlot = (SlotDisplay) additionMethod.invoke(display);
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            String templateId = getFirstItemFromSlot(templateSlot, map);
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            String baseId = getFirstItemFromSlot(baseSlot, map);
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            String additionId = getFirstItemFromSlot(additionSlot, map);
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (templateId != null) detail.template = new RecipeTreeNode.RecipeSlot(templateId);
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (baseId != null) detail.base = new RecipeTreeNode.RecipeSlot(baseId);
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (additionId != null) detail.addition = new RecipeTreeNode.RecipeSlot(additionId);
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        } catch (Exception e) { logReflectionProbeFailure(e); }
//#endif
//#if MC == 12101
//$$    private void tryExtractSmithingDetails(Recipe<?> display, RecipeTreeNode.RecipeDetail detail) {
//#endif
//#if MC == 12101
//$$        // 1.21.1 锻造台仅有扩展（transform）配方，纹饰配方在查询时已排除
//#endif
//#if MC == 12101
//$$        detail.smithingType = "transform";
//#endif
//#if MC == 12101
//$$        RecipeBookUtils.getCraftingRequirements(display).ifPresent(ingredients -> {
//#endif
//#if MC == 12101
//$$            if (ingredients.size() >= 3) {
//#endif
//#if MC == 12101
//$$                String templateId = firstItemOfIngredient(ingredients.get(0));
//#endif
//#if MC == 12101
//$$                String baseId = firstItemOfIngredient(ingredients.get(1));
//#endif
//#if MC == 12101
//$$                String additionId = firstItemOfIngredient(ingredients.get(2));
//#endif
//#if MC == 12101
//$$                if (templateId != null) detail.template = new RecipeTreeNode.RecipeSlot(templateId);
//#endif
//#if MC == 12101
//$$                if (baseId != null) detail.base = new RecipeTreeNode.RecipeSlot(baseId);
//#endif
//#if MC == 12101
//$$                if (additionId != null) detail.addition = new RecipeTreeNode.RecipeSlot(additionId);
//#endif
//#if MC == 12101
//$$            }
//#endif
//#if MC == 12101
//$$        });
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void tryExtractStonecutterDetails(RecipeDisplay display, RecipeTreeNode.RecipeDetail detail,
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                              ContextParameterMap map) {
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        try {
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            var clazz = display.getClass();
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            var inputMethod = clazz.getMethod("input");
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            SlotDisplay slot = (SlotDisplay) inputMethod.invoke(display);
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            String itemId = getFirstItemFromSlot(slot, map);
//#endif
//#if MC == 12101
//$$    private void tryExtractStonecutterDetails(Recipe<?> display, RecipeTreeNode.RecipeDetail detail) {
//#endif
//#if MC == 12101
//$$        if (display instanceof CuttingRecipe cutting) {
//#endif
//#if MC == 12101
//$$            String itemId = firstIngredientSlot(cutting.getIngredients());
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (itemId != null) detail.ingredient = new RecipeTreeNode.RecipeSlot(itemId);
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        } catch (Exception e) { logReflectionProbeFailure(e); }
//#elseif MC == 12101
//$$        }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private String getFirstItemFromSlot(SlotDisplay slot, ContextParameterMap map) {
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        List<ItemStack> stacks = slot.getStacks(map);
//#endif
//#if MC == 12101
//$$    /** 单个原料的候选物品中首个物品ID */
//#endif
//#if MC == 12101
//$$    private String firstItemOfIngredient(Ingredient ing) {
//#endif
//#if MC == 12101
//$$        List<ItemStack> stacks = RecipeBookUtils.getIngredientStacks(ing);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (stacks.isEmpty()) return null;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return Registries.ITEM.getId(stacks.getFirst().getItem()).toString();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private boolean checkLoop(List<Ingredient> ingredients, RegistryEntry<Item> inputItem,
//#elseif MC == 12101
//$$    /** 原料表中首个可用原料的物品ID */
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            RegistryEntry<Item> prevItem, ContextParameterMap map) {
//#elseif MC == 12101
//$$    private String firstIngredientSlot(List<Ingredient> ingredients) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        for (Ingredient ing : ingredients) {
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            SlotDisplay display = ing.toDisplay();
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            List<ItemStack> displayStacks = display.getStacks(map);
//#endif
//#if MC == 12101
//$$            String itemId = firstItemOfIngredient(ing);
//#endif
//#if MC == 12101
//$$            if (itemId != null) return itemId;
//#endif
//#if MC == 12101
//$$        }
//#endif
//#if MC == 12101
//$$        return null;
//#endif
//#if MC == 12101
//$$    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$
//#endif
//#if MC == 11701
//$$    private boolean checkLoop(List<Ingredient> ingredients, Item inputItem,
//#elseif MC == 11904 || MC == 12001 || MC == 12101
//$$    private boolean checkLoop(List<Ingredient> ingredients, RegistryEntry<Item> inputItem,
//#endif
//#if MC == 11701
//$$            Item prevItem, MinecraftClient client) {
//#elseif MC == 11904 || MC == 12001
//$$            RegistryEntry<Item> prevItem, MinecraftClient client) {
//#elseif MC == 12101
//$$            RegistryEntry<Item> prevItem, RegistryWrapper.WrapperLookup map) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$        for (Ingredient ing : ingredients) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            List<ItemStack> displayStacks = RecipeHelper.getIngredientStacks(ing, client);
//#elseif MC == 12101
//$$            List<ItemStack> displayStacks = RecipeBookUtils.getIngredientStacks(ing);
//#endif
            if (displayStacks.isEmpty()) continue;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            RegistryEntry<Item> ingItem = displayStacks.getFirst().getRegistryEntry();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (ingItem == inputItem || ingItem == prevItem) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                return true;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 11701
//$$            Item ingItem = getEntry(displayStacks.get(0));
//#elseif MC == 11904 || MC == 12001
//$$            RegistryEntry<Item> ingItem = displayStacks.get(0).getRegistryEntry();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (ingItem == inputItem || ingItem == prevItem) return true;
//#endif
        }
        return false;
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static String recipeTypeToJson(RecipeBookUtils.Type type) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return switch (type) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            case SHAPED -> "crafting_shaped";
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            case SHAPELESS -> "crafting_shapeless";
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            case FURNACE -> "smelting";
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            case SMITHING -> "smithing";
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            case STONECUTTER -> "stonecutting";
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            default -> "base";
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        };
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    private record RecipeFallback(List<Ingredient> ingredients, int resultCount, String type) {}
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** JSON配方类型 → RecipeBookUtils.Type 反向映射 */
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static RecipeBookUtils.Type jsonToRecipeType(String jsonType) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return switch (jsonType) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            case "crafting_shaped" -> RecipeBookUtils.Type.SHAPED;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            case "crafting_shapeless" -> RecipeBookUtils.Type.SHAPELESS;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 涂蜡铜「蜜脾涂蜡」变体（伪类型，用于把工作台配方组提前）
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            case "crafting_honeycomb" -> RecipeBookUtils.Type.SHAPELESS;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            case "smelting" -> RecipeBookUtils.Type.FURNACE;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            case "smithing" -> RecipeBookUtils.Type.SMITHING;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            case "stonecutting" -> RecipeBookUtils.Type.STONECUTTER;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            default -> null;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        };
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ==================== RecipeManager 兜底查找 ====================
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private record RecipeFallback(List<Ingredient> ingredients, int resultCount, RecipeBookUtils.Type type) {}
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 当 RecipeBookUtils.getDisplayEntryFromRecipeBook 找不到配方时，
     * 直接从 RecipeManager 遍历所有配方查找匹配项。
     * 使用反射避免 1.21.10 中 API 变更导致的编译问题。
     */
//#endif
    @SuppressWarnings("unchecked")
    private RecipeFallback findRecipeFromManager(Item targetItem) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null) return null;
            var recipeManager = client.world.getRecipeManager();
            var registryManager = client.world.getRegistryManager();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 通过反射获取所有 RecipeEntry
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            List<Object> recipeEntries = new ArrayList<>();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            try {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                var valuesMethod = recipeManager.getClass().getMethod("values");
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                Collection<?> values = (Collection<?>) valuesMethod.invoke(recipeManager);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                for (Object entry : values) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    recipeEntries.add(entry);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            } catch (NoSuchMethodException e) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 通过 getIds() + get() 组合获取
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                var keysMethod = recipeManager.getClass().getMethod("getIds");
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                var keys = (Collection<Identifier>) keysMethod.invoke(recipeManager);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                var getMethod = recipeManager.getClass().getMethod("get", Identifier.class);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                for (Identifier id : keys) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    var opt = (Optional<?>) getMethod.invoke(recipeManager, id);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    opt.ifPresent(o -> recipeEntries.add(o));
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            for (Object entryObj : recipeEntries) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // entryObj 是 RecipeEntry<?>
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                Object recipe = null;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                try {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    var valueMethod = entryObj.getClass().getMethod("value");
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    recipe = valueMethod.invoke(entryObj);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                } catch (Exception ignored) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    continue;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (recipe == null) continue;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 尝试获取配方结果
//#endif
//#if MC == 11701
//$$            // 1.17.1：RecipeManager.values() 返回 Collection<Recipe<?>>
//#elseif MC == 11904 || MC == 12001
//$$            // 1.20.1：RecipeManager.values() 直接返回 Collection<Recipe<?>>
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            for (Recipe<?> recipe : recipeManager.values()) {
//#endif
                ItemStack result = getRecipeResult(recipe, registryManager);
                if (result == null || result.isEmpty() || result.getItem() != targetItem) continue;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                List<Ingredient> ingredients = getIngredientsSafe((Recipe<?>) recipe);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                List<Ingredient> ingredients = getIngredientsSafe(recipe);
//#endif
                if (ingredients.isEmpty()) continue;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                RecipeBookUtils.Type type = getRecipeTypeSafe((Recipe<?>) recipe);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                String type = getRecipeTypeSafe(recipe);
//#endif
                if (type == null) continue;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                LitematListMod.LOGGER.debug("[RawMaterialTreeAnalyzer] RecipeManager找到配方: {} -> {} (类型: {})",
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        Registries.ITEM.getId(targetItem), recipe.getClass().getSimpleName(), type);
//#endif
                return new RecipeFallback(ingredients, result.getCount(), type);
            }
        } catch (Exception e) {
            logRecipeFallbackFailure(e);
        }
        return null;
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 通过反射获取配方结果 ItemStack。
     * 兼容 1.21.10 中 Recipe.getResult(RegistryWrapper.WrapperLookup) 方法。
     */
//#endif
    private ItemStack getRecipeResult(Object recipe, Object registryManager) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 尝试 getResult(RegistryWrapper.WrapperLookup)
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        try {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            var getResultMethod = recipe.getClass().getMethod("getResult",
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    RegistryWrapper.WrapperLookup.class);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            return (ItemStack) getResultMethod.invoke(recipe, registryManager);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        } catch (Exception e) { logReflectionProbeFailure(e); }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 尝试 getResult() 无参
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        try {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            var getResultMethod = recipe.getClass().getMethod("getResult");
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            return (ItemStack) getResultMethod.invoke(recipe);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        } catch (Exception e) { logReflectionProbeFailure(e); }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 尝试 getOutput() 方法
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        try {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            var getOutputMethod = recipe.getClass().getMethod("getOutput",
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    RegistryWrapper.WrapperLookup.class);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            return (ItemStack) getOutputMethod.invoke(recipe, registryManager);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        } catch (Exception e) { logReflectionProbeFailure(e); }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return null;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 通过反射安全获取配方原料列表 */
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    @SuppressWarnings("unchecked")
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private List<Ingredient> getIngredientsSafe(Recipe<?> recipe) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 尝试 getIngredients()（ShapedRecipe / ShapelessRecipe）
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        try {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            var method = recipe.getClass().getMethod("getIngredients");
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            return (List<Ingredient>) method.invoke(recipe);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        } catch (Exception e) { logReflectionProbeFailure(e); }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 尝试 ingredient 字段（AbstractCookingRecipe / SmeltingRecipe 等，可能在父类中）
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        try {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            var field = findFieldInHierarchy(recipe.getClass(), "ingredient");
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (field != null) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                field.setAccessible(true);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                return List.of((Ingredient) field.get(recipe));
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        } catch (Exception e) { logReflectionProbeFailure(e); }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 尝试 input 字段（StonecuttingRecipe）
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        try {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            var field = findFieldInHierarchy(recipe.getClass(), "input");
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (field != null) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                field.setAccessible(true);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                return List.of((Ingredient) field.get(recipe));
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        } catch (Exception e) { logReflectionProbeFailure(e); }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 尝试 template/base/addition 字段（SmithingTransformRecipe / SmithingTrimRecipe，可能在父类中）
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        try {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            var tField = findFieldInHierarchy(recipe.getClass(), "template");
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            var bField = findFieldInHierarchy(recipe.getClass(), "base");
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            var aField = findFieldInHierarchy(recipe.getClass(), "addition");
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (tField != null && bField != null && aField != null) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                tField.setAccessible(true);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                bField.setAccessible(true);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                aField.setAccessible(true);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                return List.of(
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        (Ingredient) tField.get(recipe),
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        (Ingredient) bField.get(recipe),
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        (Ingredient) aField.get(recipe));
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        } catch (Exception e) { logReflectionProbeFailure(e); }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return List.of();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 在类继承层次中查找指定名称的字段。
     * 因为 getDeclaredField 只查找当前类，某些字段在父类中定义（如 AbstractCookingRecipe.ingredient）。
     */
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private java.lang.reflect.Field findFieldInHierarchy(Class<?> clazz, String fieldName) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Class<?> current = clazz;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        while (current != null) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            try {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                return current.getDeclaredField(fieldName);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            } catch (NoSuchFieldException e) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                current = current.getSuperclass();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 11701
//$$        if (recipe instanceof Recipe<?> r && registryManager instanceof net.minecraft.util.registry.DynamicRegistryManager drm) {
//#elseif MC == 11904 || MC == 12001
//$$        if (recipe instanceof Recipe<?> r && registryManager instanceof net.minecraft.registry.DynamicRegistryManager drm) {
//#endif
//#if MC == 11701
//$$            try { return r.getOutput(); } catch (Exception e) { logReflectionProbeFailure(e); }
//#elseif MC == 11904 || MC == 12001
//$$            try { return r.getOutput(drm); } catch (Exception e) { logReflectionProbeFailure(e); }
//#endif
        }
        return null;
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 通过类名判断配方类型 */
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private RecipeBookUtils.Type getRecipeTypeSafe(Recipe<?> recipe) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        String name = recipe.getClass().getSimpleName();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (name.contains("Shaped")) return RecipeBookUtils.Type.SHAPED;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (name.contains("Shapeless")) return RecipeBookUtils.Type.SHAPELESS;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (name.contains("Smelting") || name.contains("Furnace")
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                || name.contains("Blasting") || name.contains("Smoking")
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                || name.contains("Campfire")) return RecipeBookUtils.Type.FURNACE;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (name.contains("Smithing")) return RecipeBookUtils.Type.SMITHING;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (name.contains("Stonecutting")) return RecipeBookUtils.Type.STONECUTTER;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    @SuppressWarnings("unchecked")
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    private List<Ingredient> getIngredientsSafe(net.minecraft.recipe.Recipe<?> recipe) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        try { var m = recipe.getClass().getMethod("getIngredients"); return (List<Ingredient>) m.invoke(recipe); } catch (Exception e) { logReflectionProbeFailure(e); }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        try { var f = findFieldInHierarchy(recipe.getClass(), "ingredient"); if (f != null) { f.setAccessible(true); return List.of((Ingredient) f.get(recipe)); } } catch (Exception e) { logReflectionProbeFailure(e); }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        try { var f = findFieldInHierarchy(recipe.getClass(), "input"); if (f != null) { f.setAccessible(true); return List.of((Ingredient) f.get(recipe)); } } catch (Exception e) { logReflectionProbeFailure(e); }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        try {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            var tF = findFieldInHierarchy(recipe.getClass(), "template");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            var bF = findFieldInHierarchy(recipe.getClass(), "base");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            var aF = findFieldInHierarchy(recipe.getClass(), "addition");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (tF != null && bF != null && aF != null) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                tF.setAccessible(true); bF.setAccessible(true); aF.setAccessible(true);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                return List.of((Ingredient) tF.get(recipe), (Ingredient) bF.get(recipe), (Ingredient) aF.get(recipe));
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        } catch (Exception e) { logReflectionProbeFailure(e); }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        return List.of();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    }
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 检查父类是否为 AbstractCookingRecipe（熔炉类配方）
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (isCookingRecipe(recipe)) return RecipeBookUtils.Type.FURNACE;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        LitematListMod.LOGGER.warn("[RawMaterialTreeAnalyzer] 未知配方类型: {}", name);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    private java.lang.reflect.Field findFieldInHierarchy(Class<?> clazz, String fieldName) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        Class<?> current = clazz;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        while (current != null) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            try { return current.getDeclaredField(fieldName); } catch (NoSuchFieldException e) { current = current.getSuperclass(); }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        }
//#endif
        return null;
    }

//#if MC == 12110
    /**
     * 从多匹配原料中选择白色/默认变种作为染色/通配基底。
     * 羊毛/床/收纳袋支持染色通配（默认白色/无色基底），枯叶堆默认橡木树叶，鸡蛋默认普通鸡蛋。
     */
//#elseif MC == 12101 || MC == 12104 || MC == 12108 || MC == 12111
//$$    /**
//$$     * 从多匹配原料中选择白色/默认变种作为染色/通配基底。
//$$     * 羊毛/床/收纳袋支持染色通配（默认白色/无色基底），枯叶堆默认橡木树叶。
//$$     */
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static RegistryEntry<Item> pickBaseDyeableIngredient(List<ItemStack> stacks) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 优先选择白色/无色/默认变种作为基底
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        for (ItemStack stack : stacks) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            String id = Registries.ITEM.getId(stack.getItem()).toString();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (id.equals("minecraft:white_wool") || id.equals("minecraft:white_bed")
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    || id.equals("minecraft:bundle")
//#endif
//#if MC == 12111
//$$                    || id.equals("minecraft:oak_leaves")) {
//#endif
//#if MC == 12110
                    || id.equals("minecraft:oak_leaves")
//#endif
//#if MC == 12110
                    || id.equals("minecraft:egg")) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$                    || id.equals("minecraft:oak_leaves")) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                return stack.getRegistryEntry();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 木板/半砖类：优先选择橡木变种作为默认基底
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        for (ItemStack stack : stacks) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            String id = Registries.ITEM.getId(stack.getItem()).toString();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (id.equals("minecraft:oak_planks") || id.equals("minecraft:oak_slab")) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                return stack.getRegistryEntry();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 圆石类：优先选择圆石作为默认基底（熔炉/发射器/投掷器/侦测器配方）
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        for (ItemStack stack : stacks) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            String id = Registries.ITEM.getId(stack.getItem()).toString();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (id.equals("minecraft:cobblestone")) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                return stack.getRegistryEntry();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return stacks.getFirst().getRegistryEntry();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    private String getRecipeTypeSafe(net.minecraft.recipe.Recipe<?> recipe) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        String name = recipe.getClass().getSimpleName();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (name.contains("Shaped")) return "SHAPED";
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (name.contains("Shapeless")) return "SHAPELESS";
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (name.contains("Smelting") || name.contains("Furnace") || name.contains("Blasting") || name.contains("Smoking") || name.contains("Campfire")) return "FURNACE";
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (name.contains("Smithing")) return "SMITHING";
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (name.contains("Stonecutting")) return "STONECUTTER";
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (isCookingRecipe(recipe)) return "FURNACE";
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        return null;
//#endif
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 检查是否继承自熔炉类配方（AbstractCookingRecipe） */
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private boolean isCookingRecipe(Recipe<?> recipe) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    private boolean isCookingRecipe(net.minecraft.recipe.Recipe<?> recipe) {
//#endif
        Class<?> current = recipe.getClass().getSuperclass();
        while (current != null) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            String name = current.getSimpleName();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (name.contains("Cooking") || name.contains("Furnace")) return true;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (current.getSimpleName().contains("Cooking") || current.getSimpleName().contains("Furnace")) return true;
//#endif
            current = current.getSuperclass();
        }
        return false;
    }
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701
//$$    private static Item pickBaseDyeableIngredient(List<ItemStack> stacks) {
//#elseif MC == 11904 || MC == 12001
//$$    private static RegistryEntry<Item> pickBaseDyeableIngredient(List<ItemStack> stacks) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        for (ItemStack stack : stacks) {
//#endif
//#if MC == 11701
//$$            String id = Registry.ITEM.getId(stack.getItem()).toString();
//#elseif MC == 11904 || MC == 12001
//$$            String id = Registries.ITEM.getId(stack.getItem()).toString();
//#endif
//#if MC == 11701
//$$            if (id.equals("minecraft:white_wool") || id.equals("minecraft:white_bed") || id.equals("minecraft:bundle") || id.equals("minecraft:oak_leaves")) return getEntry(stack);
//#elseif MC == 11904 || MC == 12001
//$$            if (id.equals("minecraft:white_wool") || id.equals("minecraft:white_bed") || id.equals("minecraft:bundle") || id.equals("minecraft:oak_leaves")) return stack.getRegistryEntry();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        for (ItemStack stack : stacks) {
//#endif
//#if MC == 11701
//$$            String id = Registry.ITEM.getId(stack.getItem()).toString();
//#elseif MC == 11904 || MC == 12001
//$$            String id = Registries.ITEM.getId(stack.getItem()).toString();
//#endif
//#if MC == 11701
//$$            if (id.equals("minecraft:oak_planks") || id.equals("minecraft:oak_slab")) return getEntry(stack);
//#elseif MC == 11904 || MC == 12001
//$$            if (id.equals("minecraft:oak_planks") || id.equals("minecraft:oak_slab")) return stack.getRegistryEntry();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        // 圆石类：优先选择圆石作为默认基底（熔炉/发射器/投掷器/侦测器配方）
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        for (ItemStack stack : stacks) {
//#endif
//#if MC == 11701
//$$            String id = Registry.ITEM.getId(stack.getItem()).toString();
//#elseif MC == 11904 || MC == 12001
//$$            String id = Registries.ITEM.getId(stack.getItem()).toString();
//#endif
//#if MC == 11701
//$$            if (id.equals("minecraft:cobblestone")) return getEntry(stack);
//#elseif MC == 11904 || MC == 12001
//$$            if (id.equals("minecraft:cobblestone")) return stack.getRegistryEntry();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        }
//#endif
//#if MC == 11701
//$$        return getEntry(stacks.get(0));
//#elseif MC == 11904 || MC == 12001
//$$        return stacks.get(0).getRegistryEntry();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    }
//#endif
}
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11904 || MC == 12001
//$$
//$$
//#endif