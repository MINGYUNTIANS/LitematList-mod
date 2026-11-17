package com.litematlist;

import com.litematlist.LitematicReader.MaterialEntry;
import com.litematlist.config.RawMaterialConfig;
import com.litematlist.gui.MaterialDetailScreen;
import com.litematlist.gui.MaterialDataStore;
import fi.dy.masa.malilib.mixin.recipe.IMixinClientRecipeBook;
import fi.dy.masa.malilib.util.game.RecipeBookUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.util.Mth;
import net.minecraft.world.flag.FeatureFlagSet;
import org.apache.commons.lang3.math.Fraction;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * 配方树分析器：从成品材料列表递归溯源，构建配方树结构。
 * 基于 RawMaterialAnalyzer 逻辑，但输出 RecipeTreeNode 树而非扁平列表。
 */
public class RawMaterialTreeAnalyzer {

    /** 通配成品物品ID列表：这些物品的配方材料可替换为同类物品 */
    private static final Set<String> WILDCARD_ITEM_IDS = Set.of(
        "minecraft:note_block", "minecraft:jukebox",
        "minecraft:cartography_table", "minecraft:fletching_table", "minecraft:smithing_table",
        "minecraft:bookshelf",
        "minecraft:piston", "minecraft:sticky_piston", "minecraft:comparator",
        "minecraft:repeater", "minecraft:daylight_detector",
        "minecraft:wooden_sword", "minecraft:wooden_pickaxe", "minecraft:wooden_axe",
        "minecraft:wooden_shovel", "minecraft:wooden_hoe",
        "minecraft:item_frame",
        // 画（配方中的羊毛为 #minecraft:wool 标签，默认白色羊毛且可通配替换）
        "minecraft:painting",
        // 蛋糕（配方中的鸡蛋为 #minecraft:eggs 标签，1.21.5+ 共 3 种鸡蛋可通配互替）
        "minecraft:cake",
        "minecraft:stone_sword", "minecraft:stone_pickaxe", "minecraft:stone_axe",
        "minecraft:stone_shovel", "minecraft:stone_hoe",
        "minecraft:chiseled_bookshelf",
        // 箱子、工作台（子材料木板应为通配可替换）
        "minecraft:chest", "minecraft:trapped_chest", "minecraft:crafting_table",
        // 木桶、织布机、熔炉、讲台（木板/半砖通配）
        "minecraft:barrel", "minecraft:loom", "minecraft:furnace", "minecraft:lectern",
        // 烟熏炉（配方中的原木为 #minecraft:logs 标签，任意原木皆可）
        "minecraft:smoker",
        // 16色羊毛（溯源为染料+通配白色羊毛）
        "minecraft:white_wool", "minecraft:orange_wool", "minecraft:magenta_wool",
        "minecraft:light_blue_wool", "minecraft:yellow_wool", "minecraft:lime_wool",
        "minecraft:pink_wool", "minecraft:gray_wool", "minecraft:light_gray_wool",
        "minecraft:cyan_wool", "minecraft:purple_wool", "minecraft:blue_wool",
        "minecraft:brown_wool", "minecraft:green_wool", "minecraft:red_wool",
        "minecraft:black_wool",
        // 16色床（溯源为染料+通配白色床）
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
        // 枯叶堆（熔炉烧制任意树叶，默认橡木树叶，通配所有树叶）
        "minecraft:leaf_litter",
        // TNT（沙子/砂砾通配）
        "minecraft:tnt",
        // 发射器、投掷器、侦测器（圆石通配，同熔炉）
        "minecraft:dispenser", "minecraft:dropper", "minecraft:observer",
        // 火把、灵魂火把、铜火把（煤炭/木炭通配，铜火把仅1.21.10+）
        "minecraft:torch", "minecraft:soul_torch", "minecraft:copper_torch",
        // 苔石、苔石砖（圆石+苔藓或圆石+藤蔓，默认苔藓，苔藓/藤蔓可通配替换）
        "minecraft:mossy_cobblestone", "minecraft:mossy_stone_bricks",
        // 木棍（配方中的木板为 #minecraft:planks 标签，任意木板皆可）
        "minecraft:stick",
        // 营火、灵魂营火（配方中的煤炭/木炭通配，同火把）
        "minecraft:campfire", "minecraft:soul_campfire"
    );

    /** 通配原材料ID列表：成品通配物品的子材料中，这些物品可替换并显示齿轮图标 */
    private static final Set<String> WILDCARD_MATERIAL_IDS = Set.of(
        // 木板变种（可替换为任意木头）
        "minecraft:oak_planks", "minecraft:spruce_planks", "minecraft:birch_planks",
        "minecraft:jungle_planks", "minecraft:acacia_planks", "minecraft:dark_oak_planks",
        "minecraft:mangrove_planks", "minecraft:cherry_planks", "minecraft:bamboo_planks",
        "minecraft:crimson_planks", "minecraft:warped_planks",
        // 木板半砖变种（可替换为对应木头，用于雕文书架等）
        "minecraft:oak_slab", "minecraft:spruce_slab", "minecraft:birch_slab",
        "minecraft:jungle_slab", "minecraft:acacia_slab", "minecraft:dark_oak_slab",
        "minecraft:mangrove_slab", "minecraft:cherry_slab", "minecraft:bamboo_slab",
        "minecraft:crimson_slab", "minecraft:warped_slab",
        // 圆石（可替换为黑石、深板岩圆石等）
        "minecraft:cobblestone", "minecraft:blackstone", "minecraft:cobbled_deepslate",
        // 羊毛/床/收纳袋支持染色通配（白色/无色为默认基底）
        "minecraft:white_wool", "minecraft:white_bed", "minecraft:bundle",
        // 沙子/砂砾互替（用于TNT等配方）
        "minecraft:sand", "minecraft:gravel",
        // 树叶（用于枯叶堆等配方，默认橡木树叶）
        "minecraft:oak_leaves", "minecraft:spruce_leaves", "minecraft:birch_leaves",
        "minecraft:jungle_leaves", "minecraft:acacia_leaves", "minecraft:dark_oak_leaves",
        "minecraft:mangrove_leaves", "minecraft:cherry_leaves", "minecraft:azalea_leaves",
        "minecraft:flowering_azalea_leaves",
        // 煤炭/木炭互替（用于火把等配方）
        "minecraft:coal", "minecraft:charcoal",
        // 苔藓/藤蔓互替（用于苔石、苔石砖配方）
        "minecraft:moss_block", "minecraft:vine",
        // 鸡蛋（用于蛋糕配方，1.21.5+ 共 3 种，互相通配，默认普通鸡蛋）
        "minecraft:egg", "minecraft:brown_egg", "minecraft:blue_egg"
    );

    /** 判断指定物品ID是否为通配成品 */
    public static boolean isWildcard(String itemId) {
        return WILDCARD_ITEM_IDS.contains(itemId);
    }

    /** 判断指定物品ID是否为通配原材料（可替换） */
    public static boolean isWildcardMaterial(String itemId) {
        return WILDCARD_MATERIAL_IDS.contains(itemId) || isLogFamily(itemId) || isPlanksFamily(itemId);
    }

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
    private final List<List<RecipeBookUtils.Type>> recipePriority;
    private int depth = 0;
    private int maxDepth = 20;
    /** 溯源记忆名单：当前调用链上已出现过的物品（防循环分解） */
    private final Set<String> tracePath = new HashSet<>();

    /** 用户偏好的配方类型（用于切换合成方式） */
    private static final Map<String, String> PREFERRED_RECIPE_TYPES = new HashMap<>();

    /** 通配物品替换映射：原始物品ID → 替换后物品ID（全局同步替换） */
    private static final Map<String, String> WILDCARD_REPLACEMENTS = new HashMap<>();

    /** 通配物品按上下文替换映射：parentItemId:childItemId → 替换后物品ID（仅替换当前子合成表） */
    private static final Map<String, String> PER_CONTEXT_REPLACEMENTS = new HashMap<>();

    /** 设置全局通配物品替换（支持链式替换，自动查找原始 key） */
    public static void setWildcardReplacement(String originalId, String newId) {
        // 如果 originalId 本身是某个替换的值，找到真正的原始 key
        String realKey = originalId;
        for (Map.Entry<String, String> e : WILDCARD_REPLACEMENTS.entrySet()) {
            if (e.getValue().equals(originalId)) {
                realKey = e.getKey();
                break;
            }
        }
        // 检查 PER_CONTEXT_REPLACEMENTS（上下文替换），确保同步开时能正确追溯原始ID
        if (realKey.equals(originalId)) {
            for (Map.Entry<String, String> e : PER_CONTEXT_REPLACEMENTS.entrySet()) {
                if (e.getValue().equals(originalId)) {
                    realKey = e.getKey().substring(e.getKey().indexOf(':') + 1);
                    break;
                }
            }
        }
        // 清除所有值等于 originalId 或 realKey 的上下文替换（全局替换应覆盖上下文替换）
        final String finalOriginalId = originalId;
        final String finalRealKey = realKey;
        PER_CONTEXT_REPLACEMENTS.entrySet().removeIf(e -> e.getValue().equals(finalOriginalId) || e.getValue().equals(finalRealKey));
        if (newId == null || newId.equals(realKey)) {
            WILDCARD_REPLACEMENTS.remove(realKey);
        } else {
            WILDCARD_REPLACEMENTS.put(realKey, newId);
        }
    }

    /** 获取通配物品替换后的ID */
    public static String getWildcardReplacement(String itemId) {
        return WILDCARD_REPLACEMENTS.getOrDefault(itemId, itemId);
    }

    /** 设置按上下文（父物品）的通配物品替换（支持链式替换） */
    public static void setContextWildcardReplacement(String parentId, String childId, String newId) {
        String key = parentId + ":" + childId;
        // 如果 childId 本身是某个替换的值，找到真正的原始 childId
        String realChildId = childId;
        boolean resolvedFromGlobal = false;
        for (Map.Entry<String, String> e : PER_CONTEXT_REPLACEMENTS.entrySet()) {
            if (e.getKey().startsWith(parentId + ":") && e.getValue().equals(childId)) {
                realChildId = e.getKey().substring(parentId.length() + 1);
                break;
            }
        }
        // 同样检查 WILDCARD_REPLACEMENTS（全局替换），确保同步关时也能正确追溯原始ID
        if (realChildId.equals(childId)) {
            for (Map.Entry<String, String> e : WILDCARD_REPLACEMENTS.entrySet()) {
                if (e.getValue().equals(childId)) {
                    realChildId = e.getKey();
                    resolvedFromGlobal = true;
                    break;
                }
            }
        }
        String realKey = parentId + ":" + realChildId;
        if (newId == null || newId.equals(realChildId)) {
            PER_CONTEXT_REPLACEMENTS.remove(realKey);
            // 如果 realChildId 是从全局替换中解析出来的，说明当前显示的值来自全局替换
            // 清除上下文后若不清除全局替换，树状图会回退到全局替换值而非原始值
            if (resolvedFromGlobal) {
                WILDCARD_REPLACEMENTS.remove(realChildId);
            }
        } else {
            PER_CONTEXT_REPLACEMENTS.put(realKey, newId);
        }
    }

    /** 获取按上下文的通配物品替换后的ID（未找到返回原ID） */
    public static String getContextWildcardReplacement(String parentId, String childId) {
        String key = parentId + ":" + childId;
        return PER_CONTEXT_REPLACEMENTS.getOrDefault(key, childId);
    }

    /** 设置用户对某物品的偏好配方类型 */
    public static void setPreferredRecipeType(String itemId, String recipeType) {
        PREFERRED_RECIPE_TYPES.put(itemId, recipeType);
    }

    /** 切换物品的配方类型（循环切换备选类型） */
    public static String toggleRecipeType(String itemId, String currentType, List<String> alternatives) {
        if (alternatives == null || alternatives.isEmpty()) return currentType;
        String preferred = PREFERRED_RECIPE_TYPES.get(itemId);
        if (preferred == null) {
            // 当前是默认类型，切换到第一个备选
            String next = alternatives.get(0);
            PREFERRED_RECIPE_TYPES.put(itemId, next);
            return next;
        }
        // 找到当前preferred在alternatives中的位置
        int idx = alternatives.indexOf(preferred);
        if (idx < 0 || idx >= alternatives.size() - 1) {
            // 切换回默认（移除preference）
            PREFERRED_RECIPE_TYPES.remove(itemId);
            return null;
        }
        // 切换到下一个备选
        String next = alternatives.get(idx + 1);
        PREFERRED_RECIPE_TYPES.put(itemId, next);
        return next;
    }

    /** 冗余列表：合成过程中多出来的物品 */
    private final List<MaterialDataStore.RawMaterialEntry> redundancy = new ArrayList<>();

    public RawMaterialTreeAnalyzer() {
        this.whitelist = RawMaterialConfig.currentWhitelist;
        this.recipePriority = RawMaterialConfig.getEnabledRecipeTypeGroups();
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    /** 分析结果 */
    public record TreeResult(List<RecipeTreeNode> roots, List<MaterialDataStore.RawMaterialEntry> redundancy) {}

    /**
     * 分析材料列表，返回配方树根节点列表和冗余物品。
     */
    public TreeResult analyze(List<MaterialEntry> entries) {
        List<RecipeTreeNode> roots = new ArrayList<>();
        redundancy.clear();
        for (MaterialEntry entry : entries) {
            Item item = entry.item();
            if (item == null) continue;
            // 特殊物品（带非默认组件）：
            // - 带特殊瓦片的陶罐按精确ID中的 sherds 分解为各瓦片；
            // - 带纹饰的盔甲按锻造台配方分解为 盔甲 / 纹饰模板 / 镶嵌材质 三个子节点；
            // - 其余按基础物品溯源，其材料与普通同类物品完全一致，同时作为独立条目呈现。
            if (entry.isExact()) {
                // 装有物品的潜影盒/收纳袋：按普通潜影盒/收纳袋溯源。
                // 其精确ID的 container / bundle_contents 组件内可能嵌套盔甲纹饰物品（含 pattern/material 字段），
                // 若继续走纹饰分支会把纹饰模板错误地加到潜影盒的溯源配方上。
                if (isContainerLikeExactItem(item)) {
                    depth = 0;
                    RecipeTreeNode containerNode = traceItem(item.builtInRegistryHolder(), entry.totalCount(), null,
                            "(顶层) " + entry.blockName());
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
                RecipeTreeNode trimNode = traceTrimmedArmor(entry);
                if (trimNode != null) {
                    roots.add(trimNode);
                    continue;
                }
                depth = 0;
                RecipeTreeNode exactNode = traceItem(item.builtInRegistryHolder(), entry.totalCount(), null,
                        "(顶层) " + entry.blockName());
                if (exactNode != null) {
                    // 携带精确堆叠：界面按特殊物品渲染变种图标与名称
                    exactNode.exactStack = entry.exactStack();
                    roots.add(exactNode);
                }
                continue;
            }
            depth = 0;
            RecipeTreeNode node = traceItem(item.builtInRegistryHolder(), entry.totalCount(), null,
                    "(顶层) " + entry.blockName());
            if (node != null) {
                roots.add(node);
            }
        }
        return new TreeResult(roots, new ArrayList<>(redundancy));
    }

    /**
     * 染色潜影盒专用：直接构造「无色潜影盒 + 对应颜色染料」两个子材料。
     * 染色潜影盒配方（早期的 crafting_special_shulkerboxcoloring，1.21.10+ 的 crafting_transmute）
     * 均无法通过配方书稳定溯源，故不依赖配方数据、按颜色名直接拼装。
     */
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
        if (item == null) {
            return false;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return false;
        }
        String path = id.getPath();
        // 潜影盒（含全部染色变体 *_shulker_box）与收纳袋（含全部染色变体 *_bundle）
        return path.equals("bundle") || path.endsWith("_bundle") || path.endsWith("shulker_box");
    }

    /**
     * 带特殊瓦片的陶罐：从精确ID中解析 sherds 列表，分解为各瓦片子材料。
     * 非陶罐的精确物品返回 null（维持原有「不溯源」行为）。
     */
    private RecipeTreeNode traceDecoratedPot(MaterialEntry entry) {
        if (entry.item() != Items.DECORATED_POT) {
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
        String armorId = BuiltInRegistries.ITEM.getKey(armor).toString();
        String templateId = BuiltInRegistries.ITEM.getKey(template).toString();
        String materialId = BuiltInRegistries.ITEM.getKey(trimMaterial).toString();

        // 三个子节点各自独立溯源，与合金装备分解行为一致
        depth = 0;
        RecipeTreeNode baseNode = traceItem(armor.builtInRegistryHolder(), count, null, "(顶层) " + exactId);
        depth = 0;
        RecipeTreeNode templateNode = traceItem(template.builtInRegistryHolder(), count, null, "(顶层) " + exactId);
        depth = 0;
        RecipeTreeNode materialNode = traceItem(trimMaterial.builtInRegistryHolder(), count, null, "(顶层) " + exactId);

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

    /**
     * 递归溯源单个物品，返回树节点。
     */
    private RecipeTreeNode traceItem(Holder<Item> item, int count,
                                     Holder<Item> prevItem, String sourceDesc) {
        // 溯源记忆名单：当前调用链上已出现的物品不再重复分解（防止 A→B→A 无限循环）
        String traceId = item.getRegisteredName();
        if (!tracePath.add(traceId)) {
            return RecipeTreeNode.base(traceId, count);
        }
        try {
            return traceItemInternal(item, count, prevItem, sourceDesc);
        } finally {
            tracePath.remove(traceId);
        }
    }

    private RecipeTreeNode traceItemInternal(Holder<Item> item, int count,
                                     Holder<Item> prevItem, String sourceDesc) {
        if (count <= 0) return null;
        if (depth >= maxDepth) {
            return RecipeTreeNode.base(item.getRegisteredName(), count);
        }

        Item mcItem = item.value();
        // 糖特殊处理：虽已列入溯源白名单，但仍保留配方溯源与工作台图标切换
        // （默认甘蔗配方，可切换为蜂蜜瓶配方）。须在白名单短路之前处理。
        if (mcItem == Items.SUGAR) {
            RecipeTreeNode sugarNode = tryTraceSugar(count, item, sourceDesc);
            if (sugarNode != null) return sugarNode;
        }
        if (whitelist.contains(mcItem)) {
            return RecipeTreeNode.base(item.getRegisteredName(), count);
        }

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return RecipeTreeNode.base(item.getRegisteredName(), count);
        }

        ContextMap map = RecipeBookUtils.getMap(client);
        String itemIdStr = BuiltInRegistries.ITEM.getKey(mcItem).toString();

        // 平滑石半砖特殊处理
        if ("minecraft:smooth_stone_slab".equals(itemIdStr)) {
            RecipeTreeNode smoothStoneNode = tryTraceSmoothStoneSlab(itemIdStr, count, map);
            if (smoothStoneNode != null) return smoothStoneNode;
        }

        // 雕纹石英特殊处理：优先使用石英块配方，而非石英台阶配方
        if ("minecraft:chiseled_quartz_block".equals(itemIdStr)) {
            RecipeTreeNode quartzNode = tryTraceChiseledQuartzBlock(itemIdStr, count, map);
            if (quartzNode != null) return quartzNode;
        }

        // 白色羊毛/白色床特殊处理：跳过"任意羊毛/床+白色染料"的循环染色配方，只用合成配方
        if ("minecraft:white_bed".equals(itemIdStr) || "minecraft:white_wool".equals(itemIdStr)) {
            RecipeTreeNode whiteNode = tryTraceWhiteDyeable(itemIdStr, count, item, sourceDesc, map);
            if (whiteNode != null) return whiteNode;
        }

        // 染色床特殊处理：所有非白色床优先走染色配方（染料+白色床）
        if (itemIdStr.endsWith("_bed") && !"minecraft:white_bed".equals(itemIdStr)) {
            RecipeTreeNode bedNode = tryTraceBedDyeing(itemIdStr, count, item, sourceDesc, map);
            if (bedNode != null) return bedNode;
        }

        // 染色潜影盒特殊处理：直接按颜色拼出「无色潜影盒 + 对应染料」，不依赖配方数据
        // （染色潜影盒配方为特殊配方/transmute，配方书取不到 ingredients）
        if (itemIdStr.endsWith("_shulker_box") && !"minecraft:shulker_box".equals(itemIdStr)) {
            RecipeTreeNode shulkerNode = tryTraceShulkerBoxDyeing(itemIdStr, count);
            if (shulkerNode != null) return shulkerNode;
        }

        // 按优先级分组查找配方，收集所有可用配方类型
        // 如果有用户偏好的配方类型，将其提前
        String preferred = PREFERRED_RECIPE_TYPES.get(itemIdStr);
        List<List<RecipeBookUtils.Type>> orderedPriority = recipePriority;
        if (preferred != null) {
            orderedPriority = new ArrayList<>(recipePriority);
            // 找到包含preferred类型的组，移到最前面
            RecipeBookUtils.Type prefType = jsonToRecipeType(preferred);
            for (int i = 0; i < orderedPriority.size(); i++) {
                if (orderedPriority.get(i).contains(prefType)) {
                    if (i > 0) {
                        List<RecipeBookUtils.Type> group = orderedPriority.remove(i);
                        orderedPriority.add(0, group);
                    }
                    break;
                }
            }
        }

        RecipeDisplayEntry primaryEntry = null;
        List<SlotDisplay> primaryIngredients = null;
        int primaryResultCount = 0;
        RecipeBookUtils.Type primaryTypeEnum = null;
        List<String> alternativeTypes = new ArrayList<>();
        boolean waxedBothKinds = false;       // 涂蜡铜：同组内同时存在「成型」与「蜜脾涂蜡」配方
        boolean stonecutterAvailable = false; // 涂蜡铜：存在切石机配方（切换环的默认项）

        for (List<RecipeBookUtils.Type> typeGroup : orderedPriority) {
            ItemStack shadow = new ItemStack(item);
            List<Pair<RecipeDisplayId, RecipeDisplayEntry>> lookup =
                    lookupRecipeEntries(shadow, typeGroup, map);

            if (lookup.isEmpty()) continue;

            // 床类优先有序（SHAPED）配方：白床走羊毛+木板，避免选到"任意床+白色染料"的循环染色配方
            boolean preferShaped = itemIdStr.endsWith("_bed");
            List<RecipeDisplayEntry> candidates = new ArrayList<>();
            for (Pair<RecipeDisplayId, RecipeDisplayEntry> pair : lookup) {
                RecipeDisplayEntry candidate = pair.getRight();
                if (extractIngredientSlots(candidate).isEmpty()) continue;
                if (preferShaped
                        && RecipeBookUtils.Type.fromRecipeDisplay(candidate.display()) != RecipeBookUtils.Type.SHAPED) {
                    continue;
                }
                candidates.add(candidate);
            }
            if (candidates.isEmpty()) continue;
            RecipeDisplayEntry entry = selectMainEntry(candidates, itemIdStr, preferred, map);

            String typeName = recipeTypeToJson(typeGroup.get(0));

            if (typeGroup.contains(RecipeBookUtils.Type.STONECUTTER)) {
                stonecutterAvailable = true;
            }
            if ((typeGroup.contains(RecipeBookUtils.Type.SHAPED)
                    || typeGroup.contains(RecipeBookUtils.Type.SHAPELESS))
                    && isWaxedCopperProduct(itemIdStr) && hasBothWaxKinds(candidates, map)) {
                waxedBothKinds = true;
            }

            if (primaryEntry == null) {
                // 第一个匹配的配方作为主配方
                primaryEntry = entry;
                List<SlotDisplay> slots = extractIngredientSlots(entry);
                if (slots.isEmpty()) { primaryEntry = null; continue; }
                primaryIngredients = slots;
                List<ItemStack> resultStacks = entry.resultItems(map);
                if (resultStacks.isEmpty()) { primaryEntry = null; continue; }
                primaryResultCount = resultStacks.getFirst().getCount();
                primaryTypeEnum = typeGroup.get(0);
            } else if (!alternativeTypes.contains(typeName)) {
                // 后续匹配的作为备选配方类型
                alternativeTypes.add(typeName);
            }
        }

        if (primaryEntry != null) {
            List<SlotDisplay> ingredients = primaryIngredients;
            int resultCount = primaryResultCount;

            // 循环检测
            if (checkLoop(ingredients, item, prevItem, map)) {
                return RecipeTreeNode.base(item.getRegisteredName(), count);
            }

            depth++;
            RecipeTreeNode node;
            if (WILDCARD_ITEM_IDS.contains(itemIdStr)) {
                node = processWildcardRecipe(ingredients, resultCount, count, item, sourceDesc, primaryTypeEnum, map);
            } else {
                node = processRecipe(ingredients, resultCount, count, item, sourceDesc, primaryTypeEnum, map, primaryEntry.display());
            }
            depth--;
            // 涂蜡铜：切换环固定为「切石机 → 成型 → 蜜脾涂蜡」，备选列表=默认项之外的部分
            if (waxedBothKinds) {
                alternativeTypes.clear();
                if (stonecutterAvailable) alternativeTypes.add("crafting_shaped");
                alternativeTypes.add("crafting_honeycomb");
            }
            if (node != null && !alternativeTypes.isEmpty()) {
                node.alternativeRecipeTypes = alternativeTypes;
            }
            return node;
        }

        // 完全无法溯源
        LitematListMod.LOGGER.info("[RawMaterialTreeAnalyzer] 无法溯源: {} (depth={})", itemIdStr, depth);
        return RecipeTreeNode.base(item.getRegisteredName(), count);
    }

    /**
     * 在同一配方类型组的多条配方中挑选主配方。
     * - 涂蜡铜制品：默认取「成型」配方（由已涂蜡父材料加工），用户偏好 "crafting_honeycomb" 时取「蜜脾涂蜡」配方；
     * - 苔石/苔石砖：默认取含苔藓的配方（圆石+苔藓）；
     * - 其余情况取第一条有效配方。
     */
    private static RecipeDisplayEntry selectMainEntry(List<RecipeDisplayEntry> candidates,
            String itemIdStr, String preferred, ContextMap map) {
        if (isWaxedCopperProduct(itemIdStr)) {
            RecipeDisplayEntry honeycombEntry = null;
            RecipeDisplayEntry normalEntry = null;
            for (RecipeDisplayEntry c : candidates) {
                boolean honeycomb = containsIngredient(
                        extractIngredientSlots(c), Items.HONEYCOMB, map);
                if (honeycomb && honeycombEntry == null) {
                    honeycombEntry = c;
                } else if (!honeycomb && normalEntry == null) {
                    normalEntry = c;
                }
            }
            if (honeycombEntry != null && normalEntry != null) {
                return "crafting_honeycomb".equals(preferred) ? honeycombEntry : normalEntry;
            }
        } else if (isMossyItem(itemIdStr)) {
            for (RecipeDisplayEntry c : candidates) {
                if (containsIngredient(extractIngredientSlots(c), Items.MOSS_BLOCK, map)) {
                    return c;
                }
            }
        }
        return candidates.getFirst();
    }

    /** 判断同一组配方中是否同时存在「蜜脾涂蜡」与「成型」两类配方（涂蜡铜可切换的前提） */
    private static boolean hasBothWaxKinds(List<RecipeDisplayEntry> candidates, ContextMap map) {
        boolean honeycomb = false;
        boolean normal = false;
        for (RecipeDisplayEntry c : candidates) {
            if (containsIngredient(extractIngredientSlots(c), Items.HONEYCOMB, map)) {
                honeycomb = true;
            } else {
                normal = true;
            }
        }
        return honeycomb && normal;
    }

    /** 判断配方原料槽位中是否包含指定物品 */
    private static boolean containsIngredient(List<SlotDisplay> slots, Item target, ContextMap map) {
        for (SlotDisplay slot : slots) {
            for (ItemStack stack : slot.resolveForStacks(map)) {
                if (stack.getItem() == target) return true;
            }
        }
        return false;
    }

    /** 判断是否为涂蜡铜制品（waxed_ 前缀，且存在未涂蜡的对应物品，覆盖涂蜡块/台阶/铜链等所有变种） */
    private static boolean isWaxedCopperProduct(String itemIdStr) {
        if (!itemIdStr.startsWith("minecraft:waxed_")) return false;
        String baseId = "minecraft:" + itemIdStr.substring("minecraft:waxed_".length());
        return BuiltInRegistries.ITEM.getOptional(Identifier.parse(baseId)).isPresent();
    }

    /** 苔石/苔石砖（配方：圆石+苔藓 或 圆石+藤蔓，默认苔藓且苔藓/藤蔓可替换） */
    private static boolean isMossyItem(String itemIdStr) {
        return "minecraft:mossy_cobblestone".equals(itemIdStr)
                || "minecraft:mossy_stone_bricks".equals(itemIdStr);
    }

    /**
     * 26.2 自建配方条目查找。26.2 起服务器只通过 RecipeBookAddPacket 下发配方条目，
     * 且条目可能缺失 craftingRequirements，maLib 的 getDisplayEntryFromRecipeBook
     * 会因依赖该字段而过滤掉全部条目；这里直接遍历客户端配方书 known 图，
     * 采用与 maLib 相同的匹配条件（但不依赖 craftingRequirements）。
     */
    private static List<Pair<RecipeDisplayId, RecipeDisplayEntry>> lookupRecipeEntries(
            ItemStack shadow, List<RecipeBookUtils.Type> typeGroup, ContextMap map) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return List.of();
        var book = (IMixinClientRecipeBook) client.player.getRecipeBook();
        Map<RecipeDisplayId, RecipeDisplayEntry> known = book.malilib_getRecipeMap();
        if (known == null || known.isEmpty()) return List.of();
        FeatureFlagSet flags = client.level.enabledFeatures();
        List<Pair<RecipeDisplayId, RecipeDisplayEntry>> out = new ArrayList<>();
        for (Map.Entry<RecipeDisplayId, RecipeDisplayEntry> e : known.entrySet()) {
            RecipeDisplayEntry entry = e.getValue();
            RecipeBookUtils.Type type = RecipeBookUtils.Type.fromRecipeDisplay(entry.display());
            if (type == null || !typeGroup.contains(type)) continue;
            if (!entry.display().isEnabled(flags)) continue;
            if (entry.display().result() instanceof SlotDisplay.SmithingTrimDemoSlotDisplay) continue;
            ItemStack resolved = entry.display().result().resolveForFirstStack(map);
            if (resolved.isEmpty()) continue;
            if (!ItemStack.isSameItem(shadow, resolved)) continue;
            out.add(Pair.of(e.getKey(), entry));
            if (out.size() >= 3) break;
        }
        return out;
    }

    /**
     * 提取配方条目的原料槽位列表。
     * 优先使用服务器下发的 craftingRequirements（转为 SlotDisplay）；
     * 26.2 起服务器可能不再填充该字段，此时直接从 display 实现类读取原料。
     */
    private static List<SlotDisplay> extractIngredientSlots(RecipeDisplayEntry entry) {
        if (entry.craftingRequirements().isPresent()) {
            List<SlotDisplay> out = new ArrayList<>();
            for (Ingredient ing : entry.craftingRequirements().get()) {
                SlotDisplay slot = ing.display();
                if (!(slot instanceof SlotDisplay.Empty)) out.add(slot);
            }
            return out;
        }
        if (entry.display() instanceof ShapedCraftingRecipeDisplay shaped) {
            List<SlotDisplay> out = new ArrayList<>();
            for (SlotDisplay slot : shaped.ingredients()) {
                if (!(slot instanceof SlotDisplay.Empty)) out.add(slot);
            }
            return out;
        }
        if (entry.display() instanceof ShapelessCraftingRecipeDisplay shapeless) {
            List<SlotDisplay> out = new ArrayList<>();
            for (SlotDisplay slot : shapeless.ingredients()) {
                if (!(slot instanceof SlotDisplay.Empty)) out.add(slot);
            }
            return out;
        }
        if (entry.display() instanceof SmithingRecipeDisplay smithing) {
            return List.of(smithing.template(), smithing.base(), smithing.addition());
        }
        if (entry.display() instanceof StonecutterRecipeDisplay stonecutter) {
            return List.of(stonecutter.input());
        }
        if (entry.display() instanceof FurnaceRecipeDisplay furnace) {
            return List.of(furnace.ingredient());
        }
        return List.of();
    }

    /**
     * 糖特殊处理：默认使用甘蔗配方，可通过点击工作台图标切换为蜂蜜瓶配方。
     * 两条配方同为 crafting_shapeless 且 group 相同，无法用配方类型区分，
     * 故按原料是否含蜂蜜瓶来挑选，并以伪类型 "crafting_honey_bottle" 作为备选项。
     */
    private RecipeTreeNode tryTraceSugar(int count, Holder<Item> item, String sourceDesc) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return null;
        ContextMap map = RecipeBookUtils.getMap(client);
        ItemStack shadow = new ItemStack(item);

        RecipeDisplayEntry caneEntry = null;
        RecipeDisplayEntry honeyEntry = null;
        RecipeBookUtils.Type recipeType = null;
        for (List<RecipeBookUtils.Type> typeGroup : recipePriority) {
            List<Pair<RecipeDisplayId, RecipeDisplayEntry>> lookup =
                    lookupRecipeEntries(shadow, typeGroup, map);
            for (Pair<RecipeDisplayId, RecipeDisplayEntry> pair : lookup) {
                RecipeDisplayEntry entry = pair.getRight();
                List<SlotDisplay> slots = extractIngredientSlots(entry);
                if (slots.isEmpty()) continue;
                if (containsIngredient(slots, Items.HONEY_BOTTLE, map)) {
                    if (honeyEntry == null) {
                        honeyEntry = entry;
                        recipeType = typeGroup.get(0);
                    }
                } else if (caneEntry == null) {
                    caneEntry = entry;
                    recipeType = typeGroup.get(0);
                }
            }
            if (caneEntry != null && honeyEntry != null) break;
        }

        String preferred = PREFERRED_RECIPE_TYPES.get("minecraft:sugar");
        RecipeDisplayEntry chosen = "crafting_honey_bottle".equals(preferred) ? honeyEntry : caneEntry;
        if (chosen == null) {
            chosen = caneEntry != null ? caneEntry : honeyEntry;
        }
        if (chosen == null || recipeType == null) return null;

        List<ItemStack> resultStacks = chosen.resultItems(map);
        if (resultStacks.isEmpty()) return null;

        depth++;
        RecipeTreeNode node = processRecipe(extractIngredientSlots(chosen),
                resultStacks.getFirst().getCount(), count, item, sourceDesc, recipeType, map, chosen.display());
        depth--;
        // 仅当两种配方都存在时才提供切换项
        if (node != null && caneEntry != null && honeyEntry != null) {
            node.alternativeRecipeTypes = List.of("crafting_honey_bottle");
        }
        return node;
    }

    private RecipeTreeNode tryTraceSmoothStoneSlab(String itemIdStr, int count, ContextMap map) {
        String preferred = PREFERRED_RECIPE_TYPES.get(itemIdStr);
        List<List<RecipeBookUtils.Type>> orderedPriority = recipePriority;
        if (preferred != null) {
            orderedPriority = new ArrayList<>(recipePriority);
            RecipeBookUtils.Type prefType = jsonToRecipeType(preferred);
            for (int i = 0; i < orderedPriority.size(); i++) {
                if (orderedPriority.get(i).contains(prefType)) {
                    if (i > 0) {
                        List<RecipeBookUtils.Type> group = orderedPriority.remove(i);
                        orderedPriority.add(0, group);
                    }
                    break;
                }
            }
        }

        RecipeDisplayEntry primaryEntry = null;
        List<SlotDisplay> primaryIngredients = null;
        int primaryResultCount = 0;
        RecipeBookUtils.Type primaryType = null;
        List<String> alternativeTypes = new ArrayList<>();

        for (List<RecipeBookUtils.Type> typeGroup : orderedPriority) {
            ItemStack shadow = new ItemStack(BuiltInRegistries.ITEM.getOptional(Identifier.parse(itemIdStr)).orElse(null));
            List<Pair<RecipeDisplayId, RecipeDisplayEntry>> lookup =
                    lookupRecipeEntries(shadow, typeGroup, map);
            if (lookup.isEmpty()) continue;

            Pair<RecipeDisplayId, RecipeDisplayEntry> pair = lookup.getFirst();
            RecipeDisplayEntry entry = pair.getRight();
            List<SlotDisplay> slots = extractIngredientSlots(entry);
            if (slots.isEmpty()) continue;

            String typeName = recipeTypeToJson(typeGroup.get(0));

            if (primaryEntry == null) {
                primaryEntry = entry;
                primaryIngredients = slots;
                List<ItemStack> resultStacks = entry.resultItems(map);
                if (resultStacks.isEmpty()) { primaryEntry = null; continue; }
                primaryResultCount = resultStacks.getFirst().getCount();
                primaryType = typeGroup.get(0);
            } else if (!alternativeTypes.contains(typeName)) {
                alternativeTypes.add(typeName);
            }
        }

        if (primaryEntry != null) {
            depth++;
            RecipeTreeNode node = processRecipe(primaryIngredients, primaryResultCount, count,
                    BuiltInRegistries.ITEM.wrapAsHolder(BuiltInRegistries.ITEM.getOptional(Identifier.parse(itemIdStr)).orElse(null)),
                    "平滑石半砖溯源", primaryType, map, primaryEntry.display());
            depth--;
            if (node != null && !alternativeTypes.isEmpty()) {
                node.alternativeRecipeTypes = alternativeTypes;
            }
            return node;
        }
        return null;
    }

    private RecipeTreeNode tryTraceChiseledQuartzBlock(String itemIdStr, int count, ContextMap map) {
        // 优先使用石英块作为原料的配方（切石机或工作台），而非石英台阶
        String preferred = PREFERRED_RECIPE_TYPES.get(itemIdStr);
        List<List<RecipeBookUtils.Type>> orderedPriority = recipePriority;
        if (preferred != null) {
            orderedPriority = new ArrayList<>(recipePriority);
            RecipeBookUtils.Type prefType = jsonToRecipeType(preferred);
            for (int i = 0; i < orderedPriority.size(); i++) {
                if (orderedPriority.get(i).contains(prefType)) {
                    if (i > 0) {
                        List<RecipeBookUtils.Type> group = orderedPriority.remove(i);
                        orderedPriority.add(0, group);
                    }
                    break;
                }
            }
        }

        RecipeDisplayEntry primaryEntry = null;
        List<SlotDisplay> primaryIngredients = null;
        int primaryResultCount = 0;
        RecipeBookUtils.Type primaryType = null;
        List<String> alternativeTypes = new ArrayList<>();

        for (List<RecipeBookUtils.Type> typeGroup : orderedPriority) {
            ItemStack shadow = new ItemStack(BuiltInRegistries.ITEM.getOptional(Identifier.parse(itemIdStr)).orElse(null));
            List<Pair<RecipeDisplayId, RecipeDisplayEntry>> lookup =
                    lookupRecipeEntries(shadow, typeGroup, map);
            for (Pair<RecipeDisplayId, RecipeDisplayEntry> pair : lookup) {
                RecipeDisplayEntry entry = pair.getRight();
                List<SlotDisplay> ingredients = extractIngredientSlots(entry);
                if (!ingredients.isEmpty()) {
                    // 检查原料是否包含石英块（而非石英台阶）
                    boolean usesQuartzBlock = false;
                    for (SlotDisplay display : ingredients) {
                        List<ItemStack> stacks = display.resolveForStacks(map);
                        for (ItemStack stack : stacks) {
                            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                            if ("minecraft:quartz_block".equals(id)) {
                                usesQuartzBlock = true;
                                break;
                            }
                        }
                        if (usesQuartzBlock) break;
                    }
                    if (usesQuartzBlock) {
                        String typeName = recipeTypeToJson(typeGroup.get(0));
                        if (primaryEntry == null) {
                            primaryEntry = entry;
                            primaryIngredients = ingredients;
                            List<ItemStack> resultStacks = entry.resultItems(map);
                            if (resultStacks.isEmpty()) { primaryEntry = null; continue; }
                            primaryResultCount = resultStacks.getFirst().getCount();
                            primaryType = typeGroup.get(0);
                        } else if (!alternativeTypes.contains(typeName)) {
                            alternativeTypes.add(typeName);
                        }
                    }
                }
            }
        }

        if (primaryEntry != null) {
            depth++;
            RecipeTreeNode node = processRecipe(primaryIngredients, primaryResultCount, count,
                    BuiltInRegistries.ITEM.wrapAsHolder(BuiltInRegistries.ITEM.getOptional(Identifier.parse(itemIdStr)).orElse(null)),
                    "雕纹石英溯源", primaryType, map, primaryEntry.display());
            depth--;
            if (node != null && !alternativeTypes.isEmpty()) {
                node.alternativeRecipeTypes = alternativeTypes;
            }
            return node;
        }
        // 没有石英块配方，回退到默认行为
        return null;
    }

    /**
     * 白色床/白色羊毛专用：跳过"白染料+彩色床/羊毛→白色"的回染配方，避免循环分解。
     * 只接受不含床（白羊毛时还不含白色染料）的合成配方：白床=羊毛x3+木板x3，白羊毛=线x4。
     */
    private RecipeTreeNode tryTraceWhiteDyeable(String itemIdStr, int count, Holder<Item> item,
            String sourceDesc, ContextMap map) {
        ItemStack shadow = new ItemStack(item.value());
        boolean isWhiteWool = "minecraft:white_wool".equals(itemIdStr);
        for (List<RecipeBookUtils.Type> typeGroup : recipePriority) {
            List<Pair<RecipeDisplayId, RecipeDisplayEntry>> lookup =
                    lookupRecipeEntries(shadow, typeGroup, map);
            for (Pair<RecipeDisplayId, RecipeDisplayEntry> pair : lookup) {
                RecipeDisplayEntry entry = pair.getRight();
                List<SlotDisplay> ingredients = extractIngredientSlots(entry);
                if (ingredients.isEmpty()) continue;
                List<ItemStack> resultStacks = entry.resultItems(map);
                if (resultStacks.isEmpty()) continue;
                int resultCount = resultStacks.getFirst().getCount();
                // 跳过染色配方：原料含床，或白色羊毛原料含白色染料
                boolean isRedye = false;
                for (SlotDisplay display : ingredients) {
                    List<ItemStack> stacks = display.resolveForStacks(map);
                    for (ItemStack stack : stacks) {
                        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                        if (id.endsWith("_bed") || (isWhiteWool && id.equals("minecraft:white_dye"))) {
                            isRedye = true;
                            break;
                        }
                    }
                    if (isRedye) break;
                }
                if (!isRedye) {
                    depth++;
                    RecipeTreeNode node = processWildcardRecipe(ingredients, resultCount,
                            count, item, sourceDesc, typeGroup.get(0), map);
                    depth--;
                    return node;
                }
            }
        }
        return null;
    }

    /**
     * 染色床特殊处理：查找染色配方（染料+白色床）。
     */
    private RecipeTreeNode tryTraceBedDyeing(String itemIdStr, int count, Holder<Item> item,
            String sourceDesc, ContextMap map) {
        ItemStack shadow = new ItemStack(item.value());
        for (List<RecipeBookUtils.Type> typeGroup : recipePriority) {
            List<Pair<RecipeDisplayId, RecipeDisplayEntry>> lookup =
                    lookupRecipeEntries(shadow, typeGroup, map);
            for (Pair<RecipeDisplayId, RecipeDisplayEntry> pair : lookup) {
                RecipeDisplayEntry entry = pair.getRight();
                List<SlotDisplay> ingredients = extractIngredientSlots(entry);
                if (!ingredients.isEmpty()) {
                    List<ItemStack> resultStacks = entry.resultItems(map);
                    if (resultStacks.isEmpty()) continue;
                    int resultCount = resultStacks.getFirst().getCount();
                    // 检查原料中是否包含白色床（确认是染色配方）
                    boolean hasBed = false;
                    for (SlotDisplay display : ingredients) {
                        List<ItemStack> stacks = display.resolveForStacks(map);
                        for (ItemStack stack : stacks) {
                            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                            if (id.endsWith("_bed")) {
                                hasBed = true;
                                break;
                            }
                        }
                        if (hasBed) break;
                    }
                    if (hasBed) {
                        depth++;
                        RecipeTreeNode node = processWildcardRecipe(ingredients, resultCount,
                                count, item, sourceDesc, typeGroup.get(0), map);
                        depth--;
                        return node;
                    }
                }
            }
        }
        return null;
    }

    private RecipeTreeNode processWildcardRecipe(List<SlotDisplay> ingredients, int resultCount, int total,
            Holder<Item> inputItem, String sourceDesc, RecipeBookUtils.Type recipeType,
            ContextMap map) {
        int adjustedTotal = total;
        if (resultCount > 1) {
            Fraction adjusted = Fraction.getFraction(total, resultCount);
            int floor = Mth.floor(adjusted.floatValue());
            float remainderCalc = resultCount * (adjusted.floatValue() - floor);
            int remainderCount = Math.round(remainderCalc);
            adjustedTotal = Math.max(floor + (remainderCount > 0 ? 1 : 0), (remainderCount > 0 ? 1 : 0));
        }

        // 计算冗余
        int actualOutput = adjustedTotal * resultCount;
        int excess = actualOutput - total;
        if (excess > 0) {
            String itemId = inputItem.getRegisteredName();
            String desc = sourceDesc + " (冗余: 需要" + total + "个, 产出" + actualOutput + "个)";
            redundancy.add(new MaterialDataStore.RawMaterialEntry(itemId, excess, List.of(desc)));
        }

        Map<Holder<Item>, Integer> dedup = new LinkedHashMap<>();
        for (SlotDisplay display : ingredients) {
            List<ItemStack> displayStacks = display.resolveForStacks(map);
            if (displayStacks.isEmpty()) continue;
            Holder<Item> ingItem;
            // 对于染色类配方，如果原料匹配了多个变种（如羊毛），优先选择白色/默认变种
            if (displayStacks.size() > 1) {
                ingItem = pickBaseDyeableIngredient(displayStacks);
            } else {
                ingItem = displayStacks.getFirst().getItem().builtInRegistryHolder();
            }
            dedup.merge(ingItem, adjustedTotal, Integer::sum);
        }

        List<RecipeTreeNode> subMaterials = new ArrayList<>();
        for (var entry : dedup.entrySet()) {
            String childId = entry.getKey().getRegisteredName();
            String parentId = inputItem.getRegisteredName();
            // 苔石/苔石砖配方中的圆石是固定材料（黑石/深板岩圆石不能用于该配方），不参与通配替换
            boolean fixedMossyCobblestone = isMossyItem(parentId)
                    && "minecraft:cobblestone".equals(childId);
            // 只有溯源物品处于通配成品白名单中，其特定材料才允许标记为通配子材料
            boolean parentWildcard = WILDCARD_ITEM_IDS.contains(parentId) && !fixedMossyCobblestone;
            if (!fixedMossyCobblestone) {
                // 优先检查按上下文（父物品）的替换
                String contextReplaced = getContextWildcardReplacement(parentId, childId);
                if (!contextReplaced.equals(childId)) {
                    childId = contextReplaced;
                } else {
                    // 回退到全局同步替换
                    String replacedId = getWildcardReplacement(childId);
                    if (!replacedId.equals(childId)) {
                        childId = replacedId;
                    }
                }
                // 防自环：替换后与当前溯源物品相同（如羊毛被替换为与染料同色的羊毛），回退为原始材料
                if (childId.equals(parentId)) {
                    childId = entry.getKey().getRegisteredName();
                }
            }
            if (isWildcardMaterial(entry.getKey().getRegisteredName())) {
                // 通配原材料，继续溯源但标记为可替换（如木板→原木）
                // 如果 childId 被替换了，使用替换后的物品进行溯源
                Holder<Item> traceEntry = entry.getKey();
                String originalId = entry.getKey().getRegisteredName();
                if (!childId.equals(originalId)) {
                    Item replacedItem = BuiltInRegistries.ITEM.getOptional(Identifier.parse(childId)).orElse(null);
                    traceEntry = replacedItem.builtInRegistryHolder();
                }
                RecipeTreeNode sub = traceItem(traceEntry, entry.getValue(), inputItem, sourceDesc);
                if (sub != null) {
                    sub.wildcardSubMaterial = parentWildcard;
                    subMaterials.add(sub);
                } else {
                    RecipeTreeNode base = RecipeTreeNode.base(entry.getKey().getRegisteredName(), entry.getValue());
                    base.wildcardSubMaterial = parentWildcard;
                    subMaterials.add(base);
                }
            } else {
                // 非通配子材料，继续向上溯源
                // 如果 childId 被替换了，使用替换后的物品进行溯源
                Holder<Item> traceEntry = entry.getKey();
                String originalId = entry.getKey().getRegisteredName();
                if (!childId.equals(originalId)) {
                    Item replacedItem = BuiltInRegistries.ITEM.getOptional(Identifier.parse(childId)).orElse(null);
                    if (replacedItem != null) {
                        traceEntry = replacedItem.builtInRegistryHolder();
                    }
                }
                RecipeTreeNode sub = traceItem(traceEntry, entry.getValue(), inputItem, sourceDesc);
                if (sub != null) {
                    subMaterials.add(sub);
                }
            }
        }

        String typeName = recipeTypeToJson(recipeType);
        return new RecipeTreeNode(inputItem.getRegisteredName(), total, typeName, List.of(), subMaterials);
    }

    private RecipeTreeNode processRecipe(List<SlotDisplay> ingredients, int resultCount, int total,
            Holder<Item> inputItem, String sourceDesc, RecipeBookUtils.Type recipeType,
            ContextMap map, RecipeDisplay display) {
        int adjustedTotal = total;
        if (resultCount > 1) {
            Fraction adjusted = Fraction.getFraction(total, resultCount);
            int floor = Mth.floor(adjusted.floatValue());
            float remainderCalc = resultCount * (adjusted.floatValue() - floor);
            int remainderCount = Math.round(remainderCalc);
            adjustedTotal = Math.max(floor + (remainderCount > 0 ? 1 : 0), (remainderCount > 0 ? 1 : 0));
        }

        // 计算冗余（多合成的部分）
        int actualOutput = adjustedTotal * resultCount;
        int excess = actualOutput - total;
        if (excess > 0) {
            String itemId = inputItem.getRegisteredName();
            String desc = sourceDesc + " (冗余: 需要" + total + "个, 产出" + actualOutput + "个)";
            redundancy.add(new MaterialDataStore.RawMaterialEntry(itemId, excess, List.of(desc)));
        }

        Map<Holder<Item>, Integer> dedup = new LinkedHashMap<>();
        for (SlotDisplay slot : ingredients) {
            List<ItemStack> displayStacks = slot.resolveForStacks(map);
            if (displayStacks.isEmpty()) continue;
            Holder<Item> ingItem = displayStacks.getFirst().getItem().builtInRegistryHolder();
            dedup.merge(ingItem, adjustedTotal, Integer::sum);
        }

        List<RecipeTreeNode> subMaterials = new ArrayList<>();
        for (var entry : dedup.entrySet()) {
            // processRecipe 仅用于非通配成品，子材料不应应用替换逻辑
            // 只有 processWildcardRecipe 才应处理通配替换
            RecipeTreeNode sub = traceItem(entry.getKey(), entry.getValue(), inputItem, sourceDesc);
            if (sub != null) {
                subMaterials.add(sub);
            } else {
                RecipeTreeNode base = RecipeTreeNode.base(entry.getKey().getRegisteredName(), entry.getValue());
                subMaterials.add(base);
            }
        }
        String typeName = recipeTypeToJson(recipeType);
        List<RecipeTreeNode.RecipeDetail> recipeDetails = extractRecipeDetails(display, recipeType, resultCount, map);

        return new RecipeTreeNode(inputItem.getRegisteredName(), total, typeName, recipeDetails, subMaterials);
    }

    private List<RecipeTreeNode.RecipeDetail> extractRecipeDetails(RecipeDisplay display,
            RecipeBookUtils.Type recipeType, int resultCount, ContextMap map) {
        List<RecipeTreeNode.RecipeDetail> details = new ArrayList<>();
        RecipeTreeNode.RecipeDetail detail = new RecipeTreeNode.RecipeDetail();
        detail.resultCount = resultCount;

        if (display == null) {
            // 兜底查找无 display 信息，仅记录类型
            detail.type = recipeTypeToJson(recipeType);
            details.add(detail);
            return details;
        }

        // 使用反射安全地提取配方详情，避免直接依赖可能在不同Yarn映射中命名不同的类
        switch (recipeType) {
            case SHAPED -> detail.type = "shaped";
            case SHAPELESS -> detail.type = "shapeless";
            case FURNACE -> {
                detail.type = "smelting";
                tryExtractFurnaceDetails(display, detail, map);
            }
            case SMITHING -> {
                detail.type = "smithing";
                tryExtractSmithingDetails(display, detail, map);
            }
            case STONECUTTER -> {
                detail.type = "stonecutting";
                tryExtractStonecutterDetails(display, detail, map);
            }
        }
        details.add(detail);
        return details;
    }

    private void tryExtractFurnaceDetails(RecipeDisplay display, RecipeTreeNode.RecipeDetail detail,
                                          ContextMap map) {
        // 26.2 熔炉类配方统一为 FurnaceRecipeDisplay（duration/experience/ingredient 原生访问器）
        if (!(display instanceof FurnaceRecipeDisplay furnace)) return;
        detail.cookingTime = furnace.duration();
        detail.experience = furnace.experience();
        String itemId = getFirstItemFromSlot(furnace.ingredient(), map);
        if (itemId != null) detail.ingredient = new RecipeTreeNode.RecipeSlot(itemId);
    }

    private void tryExtractSmithingDetails(RecipeDisplay display, RecipeTreeNode.RecipeDetail detail,
                                           ContextMap map) {
        // 26.2 SmithingRecipeDisplay 不再携带 smithing 类型枚举（无 getType），仅保留原料槽位
        if (!(display instanceof SmithingRecipeDisplay smithing)) return;
        String templateId = getFirstItemFromSlot(smithing.template(), map);
        String baseId = getFirstItemFromSlot(smithing.base(), map);
        String additionId = getFirstItemFromSlot(smithing.addition(), map);
        if (templateId != null) detail.template = new RecipeTreeNode.RecipeSlot(templateId);
        if (baseId != null) detail.base = new RecipeTreeNode.RecipeSlot(baseId);
        if (additionId != null) detail.addition = new RecipeTreeNode.RecipeSlot(additionId);
    }

    private void tryExtractStonecutterDetails(RecipeDisplay display, RecipeTreeNode.RecipeDetail detail,
                                              ContextMap map) {
        if (!(display instanceof StonecutterRecipeDisplay stonecutter)) return;
        String itemId = getFirstItemFromSlot(stonecutter.input(), map);
        if (itemId != null) detail.ingredient = new RecipeTreeNode.RecipeSlot(itemId);
    }

    private String getFirstItemFromSlot(SlotDisplay slot, ContextMap map) {
        List<ItemStack> stacks = slot.resolveForStacks(map);
        if (stacks.isEmpty()) return null;
        return BuiltInRegistries.ITEM.getKey(stacks.getFirst().getItem()).toString();
    }

    private boolean checkLoop(List<SlotDisplay> ingredients, Holder<Item> inputItem,
            Holder<Item> prevItem, ContextMap map) {
        for (SlotDisplay display : ingredients) {
            List<ItemStack> displayStacks = display.resolveForStacks(map);
            if (displayStacks.isEmpty()) continue;
            Holder<Item> ingItem = displayStacks.getFirst().getItem().builtInRegistryHolder();
            if (ingItem == inputItem || ingItem == prevItem) {
                return true;
            }
        }
        return false;
    }

    private static String recipeTypeToJson(RecipeBookUtils.Type type) {
        return switch (type) {
            case SHAPED -> "crafting_shaped";
            case SHAPELESS -> "crafting_shapeless";
            case FURNACE -> "smelting";
            case SMITHING -> "smithing";
            case STONECUTTER -> "stonecutting";
            default -> "base";
        };
    }

    /** JSON配方类型 → RecipeBookUtils.Type 反向映射 */
    private static RecipeBookUtils.Type jsonToRecipeType(String jsonType) {
        return switch (jsonType) {
            case "crafting_shaped" -> RecipeBookUtils.Type.SHAPED;
            case "crafting_shapeless" -> RecipeBookUtils.Type.SHAPELESS;
            // 涂蜡铜「蜜脾涂蜡」变体（伪类型，用于把工作台配方组提前）
            case "crafting_honeycomb" -> RecipeBookUtils.Type.SHAPELESS;
            case "smelting" -> RecipeBookUtils.Type.FURNACE;
            case "smithing" -> RecipeBookUtils.Type.SMITHING;
            case "stonecutting" -> RecipeBookUtils.Type.STONECUTTER;
            default -> null;
        };
    }


    /**
     * 从多匹配原料中选择白色/默认变种作为染色/通配基底。
     * 仅羊毛和床支持染色通配，枯叶堆默认橡木树叶。
     */
    private static Holder<Item> pickBaseDyeableIngredient(List<ItemStack> stacks) {
        // 优先选择白色/默认变种作为基底
        for (ItemStack stack : stacks) {
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (id.equals("minecraft:white_wool") || id.equals("minecraft:white_bed")
                    || id.equals("minecraft:bundle")
                    || id.equals("minecraft:oak_leaves")
                    || id.equals("minecraft:egg")) {
                return BuiltInRegistries.ITEM.wrapAsHolder(stack.getItem());
            }
        }
        // 木板/半砖类：优先选择橡木变种作为默认基底
        for (ItemStack stack : stacks) {
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (id.equals("minecraft:oak_planks") || id.equals("minecraft:oak_slab")) {
                return BuiltInRegistries.ITEM.wrapAsHolder(stack.getItem());
            }
        }
        // 圆石类：优先选择圆石作为默认基底（熔炉/发射器/投掷器/侦测器配方）
        for (ItemStack stack : stacks) {
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (id.equals("minecraft:cobblestone")) {
                return BuiltInRegistries.ITEM.wrapAsHolder(stack.getItem());
            }
        }
        return BuiltInRegistries.ITEM.wrapAsHolder(stacks.getFirst().getItem());
    }
}
