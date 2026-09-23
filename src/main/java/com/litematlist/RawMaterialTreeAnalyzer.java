package com.litematlist;

import com.litematlist.LitematicReader.MaterialEntry;
import com.litematlist.config.RawMaterialConfig;
import com.litematlist.gui.MaterialDetailScreen;
import fi.dy.masa.malilib.util.game.RecipeBookUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import net.minecraft.recipe.display.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.context.ContextParameterMap;
import net.minecraft.util.math.MathHelper;
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
        "minecraft:stone_sword", "minecraft:stone_pickaxe", "minecraft:stone_axe",
        "minecraft:stone_shovel", "minecraft:stone_hoe",
        "minecraft:chiseled_bookshelf",
        // 箱子、工作台（子材料木板应为通配可替换）
        "minecraft:chest", "minecraft:trapped_chest", "minecraft:crafting_table",
        // 木桶、织布机、熔炉、讲台（木板/半砖通配）
        "minecraft:barrel", "minecraft:loom", "minecraft:furnace", "minecraft:lectern",
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
        // 枯叶堆（熔炉烧制任意树叶，默认橡木树叶，通配所有树叶）
        "minecraft:leaf_litter",
        // TNT（沙子/砂砾通配）
        "minecraft:tnt",
        // 发射器、投掷器、侦测器（圆石通配，同熔炉）
        "minecraft:dispenser", "minecraft:dropper", "minecraft:observer",
        // 火把、灵魂火把、铜火把（煤炭/木炭通配，铜火把仅1.21.10+）
        "minecraft:torch", "minecraft:soul_torch", "minecraft:copper_torch"
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
        // 仅羊毛和床支持染色通配（白色为默认基底）
        "minecraft:white_wool", "minecraft:white_bed",
        // 沙子/砂砾互替（用于TNT等配方）
        "minecraft:sand", "minecraft:gravel",
        // 树叶（用于枯叶堆等配方，默认橡木树叶）
        "minecraft:oak_leaves", "minecraft:spruce_leaves", "minecraft:birch_leaves",
        "minecraft:jungle_leaves", "minecraft:acacia_leaves", "minecraft:dark_oak_leaves",
        "minecraft:mangrove_leaves", "minecraft:cherry_leaves", "minecraft:azalea_leaves",
        "minecraft:flowering_azalea_leaves",
        // 煤炭/木炭互替（用于火把等配方）
        "minecraft:coal", "minecraft:charcoal"
    );

    /** 判断指定物品ID是否为通配成品 */
    public static boolean isWildcard(String itemId) {
        return WILDCARD_ITEM_IDS.contains(itemId);
    }

    /** 判断指定物品ID是否为通配原材料（可替换） */
    public static boolean isWildcardMaterial(String itemId) {
        return WILDCARD_MATERIAL_IDS.contains(itemId);
    }

    private final Set<Item> whitelist;
    private final List<List<RecipeBookUtils.Type>> recipePriority;
    private int depth = 0;
    private int maxDepth = 20;
    /** 溯源记录：当前顶层条目溯源路径中出现过的物品，防止循环溯源 */
    private Set<String> visited = new HashSet<>();

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
    private final List<MaterialDetailScreen.RawMaterialEntry> redundancy = new ArrayList<>();

    public RawMaterialTreeAnalyzer() {
        this.whitelist = RawMaterialConfig.currentWhitelist;
        this.recipePriority = RawMaterialConfig.getEnabledRecipeTypeGroups();
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    /** 分析结果 */
    public record TreeResult(List<RecipeTreeNode> roots, List<MaterialDetailScreen.RawMaterialEntry> redundancy) {}

    /**
     * 分析材料列表，返回配方树根节点列表和冗余物品。
     */
    public TreeResult analyze(List<MaterialEntry> entries) {
        List<RecipeTreeNode> roots = new ArrayList<>();
        redundancy.clear();
        for (MaterialEntry entry : entries) {
            Item item = entry.item();
            if (item == null) continue;
            depth = 0;
            visited = new HashSet<>();
            RecipeTreeNode node = traceItem(item.getRegistryEntry(), entry.totalCount(), null,
                    "(顶层) " + entry.blockName());
            if (node != null) {
                roots.add(node);
            }
        }
        return new TreeResult(roots, new ArrayList<>(redundancy));
    }

    /**
     * 递归溯源单个物品，返回树节点。
     */
    private RecipeTreeNode traceItem(RegistryEntry<Item> item, int count,
                                     RegistryEntry<Item> prevItem, String sourceDesc) {
        if (count <= 0) return null;
        if (depth >= maxDepth) {
            return RecipeTreeNode.base(item.getIdAsString(), count);
        }

        Item mcItem = item.value();
        if (whitelist.contains(mcItem)) {
            return RecipeTreeNode.base(item.getIdAsString(), count);
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return RecipeTreeNode.base(item.getIdAsString(), count);
        }

        ContextParameterMap map = RecipeBookUtils.getMap(client);
        String itemIdStr = Registries.ITEM.getId(mcItem).toString();

        // 溯源记录表：如果该物品已在当前溯源路径中出现过，停止进一步分解，防止循环溯源
        if (!visited.add(itemIdStr)) {
            return RecipeTreeNode.base(item.getIdAsString(), count);
        }

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

        // 白色床/白色羊毛：跳过"白染料+彩色床/羊毛→白色"的回染配方，避免白床被错误分解为「彩色床+白染料」
        if ("minecraft:white_bed".equals(itemIdStr) || "minecraft:white_wool".equals(itemIdStr)) {
            RecipeTreeNode whiteNode = tryTraceWhiteDyeable(itemIdStr, count, item, sourceDesc, map);
            if (whiteNode != null) return whiteNode;
        }

        // 染色床特殊处理：所有非白色床优先走染色配方（染料+白色床）
        if (itemIdStr.endsWith("_bed") && !"minecraft:white_bed".equals(itemIdStr)) {
            RecipeTreeNode bedNode = tryTraceBedDyeing(itemIdStr, count, item, sourceDesc, map);
            if (bedNode != null) return bedNode;
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
        List<Ingredient> primaryIngredients = null;
        int primaryResultCount = 0;
        RecipeBookUtils.Type primaryTypeEnum = null;
        List<String> alternativeTypes = new ArrayList<>();

        for (List<RecipeBookUtils.Type> typeGroup : orderedPriority) {
            ItemStack shadow = new ItemStack(item);
            List<Pair<NetworkRecipeId, RecipeDisplayEntry>> lookup =
                    RecipeBookUtils.getDisplayEntryFromRecipeBook(shadow, typeGroup);

            if (lookup.isEmpty()) continue;

            Pair<NetworkRecipeId, RecipeDisplayEntry> pair = lookup.getFirst();
            RecipeDisplayEntry entry = pair.getRight();

            if (entry.craftingRequirements().isEmpty()) continue;

            String typeName = recipeTypeToJson(typeGroup.get(0));

            if (primaryEntry == null) {
                // 第一个匹配的配方作为主配方
                primaryEntry = entry;
                primaryIngredients = entry.craftingRequirements().get();
                List<ItemStack> resultStacks = entry.getStacks(map);
                if (resultStacks.isEmpty()) { primaryEntry = null; continue; }
                primaryResultCount = resultStacks.getFirst().getCount();
                primaryTypeEnum = typeGroup.get(0);
            } else if (!alternativeTypes.contains(typeName)) {
                // 后续匹配的作为备选配方类型
                alternativeTypes.add(typeName);
            }
        }

        if (primaryEntry != null) {
            List<Ingredient> ingredients = primaryIngredients;
            int resultCount = primaryResultCount;

            // 循环检测
            if (checkLoop(ingredients, item, prevItem, map)) {
                return RecipeTreeNode.base(item.getIdAsString(), count);
            }

            depth++;
            RecipeTreeNode node;
            if (WILDCARD_ITEM_IDS.contains(itemIdStr)) {
                node = processWildcardRecipe(ingredients, resultCount, count, item, sourceDesc, primaryTypeEnum, map);
            } else {
                node = processRecipe(ingredients, resultCount, count, item, sourceDesc, primaryTypeEnum, map, primaryEntry.display());
            }
            depth--;
            if (node != null && !alternativeTypes.isEmpty()) {
                node.alternativeRecipeTypes = alternativeTypes;
            }
            return node;
        }

        // 无匹配配方 — 尝试从RecipeManager直接兜底查找
        RecipeFallback fallback = findRecipeFromManager(mcItem);
        if (fallback != null) {
            depth++;
            RecipeTreeNode node = processRecipe(fallback.ingredients, fallback.resultCount, count,
                    item, sourceDesc, fallback.type, map, null);
            depth--;
            if (node != null) {
                LitematListMod.LOGGER.info("[RawMaterialTreeAnalyzer] RecipeManager兜底成功: {} (类型: {})",
                        itemIdStr, fallback.type);
            }
            return node;
        }

        // 完全无法溯源
        LitematListMod.LOGGER.info("[RawMaterialTreeAnalyzer] 无法溯源: {} (depth={})", itemIdStr, depth);
        return RecipeTreeNode.base(item.getIdAsString(), count);
    }

    private RecipeTreeNode tryTraceSmoothStoneSlab(String itemIdStr, int count, ContextParameterMap map) {
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
        List<Ingredient> primaryIngredients = null;
        int primaryResultCount = 0;
        RecipeBookUtils.Type primaryType = null;
        List<String> alternativeTypes = new ArrayList<>();

        for (List<RecipeBookUtils.Type> typeGroup : orderedPriority) {
            ItemStack shadow = new ItemStack(Registries.ITEM.get(Identifier.of(itemIdStr)));
            List<Pair<NetworkRecipeId, RecipeDisplayEntry>> lookup =
                    RecipeBookUtils.getDisplayEntryFromRecipeBook(shadow, typeGroup);
            if (lookup.isEmpty()) continue;

            Pair<NetworkRecipeId, RecipeDisplayEntry> pair = lookup.getFirst();
            RecipeDisplayEntry entry = pair.getRight();
            if (entry.craftingRequirements().isEmpty()) continue;

            String typeName = recipeTypeToJson(typeGroup.get(0));

            if (primaryEntry == null) {
                primaryEntry = entry;
                primaryIngredients = entry.craftingRequirements().get();
                List<ItemStack> resultStacks = entry.getStacks(map);
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
                    Registries.ITEM.get(Identifier.of(itemIdStr)).getRegistryEntry(),
                    "平滑石半砖溯源", primaryType, map, primaryEntry.display());
            depth--;
            if (node != null && !alternativeTypes.isEmpty()) {
                node.alternativeRecipeTypes = alternativeTypes;
            }
            return node;
        }
        return null;
    }

    private RecipeTreeNode tryTraceChiseledQuartzBlock(String itemIdStr, int count, ContextParameterMap map) {
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
        List<Ingredient> primaryIngredients = null;
        int primaryResultCount = 0;
        RecipeBookUtils.Type primaryType = null;
        List<String> alternativeTypes = new ArrayList<>();

        for (List<RecipeBookUtils.Type> typeGroup : orderedPriority) {
            ItemStack shadow = new ItemStack(Registries.ITEM.get(Identifier.of(itemIdStr)));
            List<Pair<NetworkRecipeId, RecipeDisplayEntry>> lookup =
                    RecipeBookUtils.getDisplayEntryFromRecipeBook(shadow, typeGroup);
            for (Pair<NetworkRecipeId, RecipeDisplayEntry> pair : lookup) {
                RecipeDisplayEntry entry = pair.getRight();
                if (entry.craftingRequirements().isPresent()) {
                    List<Ingredient> ingredients = entry.craftingRequirements().get();
                    // 检查原料是否包含石英块（而非石英台阶）
                    boolean usesQuartzBlock = false;
                    for (Ingredient ing : ingredients) {
                        SlotDisplay display = ing.toDisplay();
                        List<ItemStack> stacks = display.getStacks(map);
                        for (ItemStack stack : stacks) {
                            String id = Registries.ITEM.getId(stack.getItem()).toString();
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
                            List<ItemStack> resultStacks = entry.getStacks(map);
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
                    Registries.ITEM.get(Identifier.of(itemIdStr)).getRegistryEntry(),
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
     * 染色床特殊处理：查找染色配方（染料+白色床）。
     */
    /**
     * 白色床/白色羊毛专用：跳过"白染料+彩色床/羊毛→白色"的回染配方，避免循环分解。
     * 只接受不含床（白羊毛时还不含白色染料）的合成配方：白床=羊毛x3+木板x3，白羊毛=线x4。
     */
    private RecipeTreeNode tryTraceWhiteDyeable(String itemIdStr, int count, RegistryEntry<Item> item,
            String sourceDesc, ContextParameterMap map) {
        ItemStack shadow = new ItemStack(item.value());
        boolean isWhiteWool = "minecraft:white_wool".equals(itemIdStr);
        for (List<RecipeBookUtils.Type> typeGroup : recipePriority) {
            List<Pair<NetworkRecipeId, RecipeDisplayEntry>> lookup =
                    RecipeBookUtils.getDisplayEntryFromRecipeBook(shadow, typeGroup);
            for (Pair<NetworkRecipeId, RecipeDisplayEntry> pair : lookup) {
                RecipeDisplayEntry entry = pair.getRight();
                if (!entry.craftingRequirements().isPresent()) continue;
                List<Ingredient> ingredients = entry.craftingRequirements().get();
                List<ItemStack> resultStacks = entry.getStacks(map);
                if (resultStacks.isEmpty()) continue;
                int resultCount = resultStacks.getFirst().getCount();
                boolean isRedye = false;
                for (Ingredient ing : ingredients) {
                    SlotDisplay display = ing.toDisplay();
                    List<ItemStack> stacks = display.getStacks(map);
                    for (ItemStack stack : stacks) {
                        String id = Registries.ITEM.getId(stack.getItem()).toString();
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

    private RecipeTreeNode tryTraceBedDyeing(String itemIdStr, int count, RegistryEntry<Item> item,
            String sourceDesc, ContextParameterMap map) {
        ItemStack shadow = new ItemStack(item.value());
        for (List<RecipeBookUtils.Type> typeGroup : recipePriority) {
            List<Pair<NetworkRecipeId, RecipeDisplayEntry>> lookup =
                    RecipeBookUtils.getDisplayEntryFromRecipeBook(shadow, typeGroup);
            for (Pair<NetworkRecipeId, RecipeDisplayEntry> pair : lookup) {
                RecipeDisplayEntry entry = pair.getRight();
                if (entry.craftingRequirements().isPresent()) {
                    List<Ingredient> ingredients = entry.craftingRequirements().get();
                    List<ItemStack> resultStacks = entry.getStacks(map);
                    if (resultStacks.isEmpty()) continue;
                    int resultCount = resultStacks.getFirst().getCount();
                    // 检查原料中是否包含白色床（确认是染色配方）
                    boolean hasBed = false;
                    for (Ingredient ing : ingredients) {
                        SlotDisplay display = ing.toDisplay();
                        List<ItemStack> stacks = display.getStacks(map);
                        for (ItemStack stack : stacks) {
                            String id = Registries.ITEM.getId(stack.getItem()).toString();
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

    private RecipeTreeNode processWildcardRecipe(List<Ingredient> ingredients, int resultCount, int total,
            RegistryEntry<Item> inputItem, String sourceDesc, RecipeBookUtils.Type recipeType,
            ContextParameterMap map) {
        int adjustedTotal = total;
        if (resultCount > 1) {
            Fraction adjusted = Fraction.getFraction(total, resultCount);
            int floor = MathHelper.floor(adjusted.floatValue());
            float remainderCalc = resultCount * (adjusted.floatValue() - floor);
            int remainderCount = Math.round(remainderCalc);
            adjustedTotal = Math.max(floor + (remainderCount > 0 ? 1 : 0), (remainderCount > 0 ? 1 : 0));
        }

        // 计算冗余
        int actualOutput = adjustedTotal * resultCount;
        int excess = actualOutput - total;
        if (excess > 0) {
            String itemId = inputItem.getIdAsString();
            String desc = sourceDesc + " (冗余: 需要" + total + "个, 产出" + actualOutput + "个)";
            redundancy.add(new MaterialDetailScreen.RawMaterialEntry(itemId, excess, List.of(desc)));
        }

        Map<RegistryEntry<Item>, Integer> dedup = new LinkedHashMap<>();
        for (Ingredient ing : ingredients) {
            SlotDisplay display = ing.toDisplay();
            List<ItemStack> displayStacks = display.getStacks(map);
            if (displayStacks.isEmpty()) continue;
            RegistryEntry<Item> ingItem;
            // 对于染色类配方，如果原料匹配了多个变种（如羊毛），优先选择白色/默认变种
            if (displayStacks.size() > 1) {
                ingItem = pickBaseDyeableIngredient(displayStacks);
            } else {
                ingItem = displayStacks.getFirst().getRegistryEntry();
            }
            dedup.merge(ingItem, adjustedTotal, Integer::sum);
        }

        List<RecipeTreeNode> subMaterials = new ArrayList<>();
        for (var entry : dedup.entrySet()) {
            String childId = entry.getKey().getIdAsString();
            String parentId = inputItem.getIdAsString();
            // 只有溯源物品处于通配成品白名单中，其特定材料才允许标记为通配子材料
            boolean parentWildcard = WILDCARD_ITEM_IDS.contains(parentId);
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
                childId = entry.getKey().getIdAsString();
            }
            if (WILDCARD_MATERIAL_IDS.contains(entry.getKey().getIdAsString())) {
                // 通配原材料，继续溯源但标记为可替换（如木板→原木）
                // 如果 childId 被替换了，使用替换后的物品进行溯源
                RegistryEntry<Item> traceEntry = entry.getKey();
                String originalId = entry.getKey().getIdAsString();
                if (!childId.equals(originalId)) {
                    Item replacedItem = Registries.ITEM.get(Identifier.of(childId));
                    traceEntry = replacedItem.getRegistryEntry();
                }
                // 雕文书架半砖不显示替换按钮：半砖材质随木板变动，不允许单独替换
                boolean isChiseledBookshelfSlab = "minecraft:chiseled_bookshelf".equals(parentId)
                        && childId.endsWith("_slab");
                RecipeTreeNode sub = traceItem(traceEntry, entry.getValue(), inputItem, sourceDesc);
                if (sub != null) {
                    sub.wildcardSubMaterial = parentWildcard && !isChiseledBookshelfSlab;
                    subMaterials.add(sub);
                } else {
                    RecipeTreeNode base = RecipeTreeNode.base(entry.getKey().getIdAsString(), entry.getValue());
                    base.wildcardSubMaterial = parentWildcard && !isChiseledBookshelfSlab;
                    subMaterials.add(base);
                }
            } else {
                // 非通配子材料，继续向上溯源
                // 如果 childId 被替换了，使用替换后的物品进行溯源
                RegistryEntry<Item> traceEntry = entry.getKey();
                String originalId = entry.getKey().getIdAsString();
                if (!childId.equals(originalId)) {
                    Item replacedItem = Registries.ITEM.get(Identifier.of(childId));
                    if (replacedItem != null) {
                        traceEntry = replacedItem.getRegistryEntry();
                    }
                }
                RecipeTreeNode sub = traceItem(traceEntry, entry.getValue(), inputItem, sourceDesc);
                if (sub != null) {
                    subMaterials.add(sub);
                }
            }
        }

        String typeName = recipeTypeToJson(recipeType);
        return new RecipeTreeNode(inputItem.getIdAsString(), total, typeName, List.of(), subMaterials);
    }

    private RecipeTreeNode processRecipe(List<Ingredient> ingredients, int resultCount, int total,
            RegistryEntry<Item> inputItem, String sourceDesc, RecipeBookUtils.Type recipeType,
            ContextParameterMap map, RecipeDisplay display) {
        int adjustedTotal = total;
        if (resultCount > 1) {
            Fraction adjusted = Fraction.getFraction(total, resultCount);
            int floor = MathHelper.floor(adjusted.floatValue());
            float remainderCalc = resultCount * (adjusted.floatValue() - floor);
            int remainderCount = Math.round(remainderCalc);
            adjustedTotal = Math.max(floor + (remainderCount > 0 ? 1 : 0), (remainderCount > 0 ? 1 : 0));
        }

        // 计算冗余（多合成的部分）
        int actualOutput = adjustedTotal * resultCount;
        int excess = actualOutput - total;
        if (excess > 0) {
            String itemId = inputItem.getIdAsString();
            String desc = sourceDesc + " (冗余: 需要" + total + "个, 产出" + actualOutput + "个)";
            redundancy.add(new MaterialDetailScreen.RawMaterialEntry(itemId, excess, List.of(desc)));
        }

        Map<RegistryEntry<Item>, Integer> dedup = new LinkedHashMap<>();
        for (Ingredient ing : ingredients) {
            SlotDisplay slotDisplay = ing.toDisplay();
            List<ItemStack> displayStacks = slotDisplay.getStacks(map);
            if (displayStacks.isEmpty()) continue;
            RegistryEntry<Item> ingItem = displayStacks.getFirst().getRegistryEntry();
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
                RecipeTreeNode base = RecipeTreeNode.base(entry.getKey().getIdAsString(), entry.getValue());
                subMaterials.add(base);
            }
        }
        String typeName = recipeTypeToJson(recipeType);
        List<RecipeTreeNode.RecipeDetail> recipeDetails = extractRecipeDetails(display, recipeType, resultCount, map);

        return new RecipeTreeNode(inputItem.getIdAsString(), total, typeName, recipeDetails, subMaterials);
    }

    private List<RecipeTreeNode.RecipeDetail> extractRecipeDetails(RecipeDisplay display,
            RecipeBookUtils.Type recipeType, int resultCount, ContextParameterMap map) {
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
                                          ContextParameterMap map) {
        try {
            var clazz = display.getClass();
            var cookingTimeMethod = clazz.getMethod("cookingTime");
            var experienceMethod = clazz.getMethod("experience");
            var ingredientMethod = clazz.getMethod("ingredient");
            detail.cookingTime = (int) cookingTimeMethod.invoke(display);
            detail.experience = (float) experienceMethod.invoke(display);
            SlotDisplay slot = (SlotDisplay) ingredientMethod.invoke(display);
            String itemId = getFirstItemFromSlot(slot, map);
            if (itemId != null) detail.ingredient = new RecipeTreeNode.RecipeSlot(itemId);
        } catch (Exception ignored) {}
    }

    private void tryExtractSmithingDetails(RecipeDisplay display, RecipeTreeNode.RecipeDetail detail,
                                           ContextParameterMap map) {
        try {
            var clazz = display.getClass();
            var typeMethod = clazz.getMethod("getType");
            var typeEnum = (Enum<?>) typeMethod.invoke(display);
            detail.smithingType = typeEnum.name().toLowerCase();
            var templateMethod = clazz.getMethod("template");
            var baseMethod = clazz.getMethod("base");
            var additionMethod = clazz.getMethod("addition");
            SlotDisplay templateSlot = (SlotDisplay) templateMethod.invoke(display);
            SlotDisplay baseSlot = (SlotDisplay) baseMethod.invoke(display);
            SlotDisplay additionSlot = (SlotDisplay) additionMethod.invoke(display);
            String templateId = getFirstItemFromSlot(templateSlot, map);
            String baseId = getFirstItemFromSlot(baseSlot, map);
            String additionId = getFirstItemFromSlot(additionSlot, map);
            if (templateId != null) detail.template = new RecipeTreeNode.RecipeSlot(templateId);
            if (baseId != null) detail.base = new RecipeTreeNode.RecipeSlot(baseId);
            if (additionId != null) detail.addition = new RecipeTreeNode.RecipeSlot(additionId);
        } catch (Exception ignored) {}
    }

    private void tryExtractStonecutterDetails(RecipeDisplay display, RecipeTreeNode.RecipeDetail detail,
                                              ContextParameterMap map) {
        try {
            var clazz = display.getClass();
            var inputMethod = clazz.getMethod("input");
            SlotDisplay slot = (SlotDisplay) inputMethod.invoke(display);
            String itemId = getFirstItemFromSlot(slot, map);
            if (itemId != null) detail.ingredient = new RecipeTreeNode.RecipeSlot(itemId);
        } catch (Exception ignored) {}
    }

    private String getFirstItemFromSlot(SlotDisplay slot, ContextParameterMap map) {
        List<ItemStack> stacks = slot.getStacks(map);
        if (stacks.isEmpty()) return null;
        return Registries.ITEM.getId(stacks.getFirst().getItem()).toString();
    }

    private boolean checkLoop(List<Ingredient> ingredients, RegistryEntry<Item> inputItem,
            RegistryEntry<Item> prevItem, ContextParameterMap map) {
        for (Ingredient ing : ingredients) {
            SlotDisplay display = ing.toDisplay();
            List<ItemStack> displayStacks = display.getStacks(map);
            if (displayStacks.isEmpty()) continue;
            RegistryEntry<Item> ingItem = displayStacks.getFirst().getRegistryEntry();
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
            case "smelting" -> RecipeBookUtils.Type.FURNACE;
            case "smithing" -> RecipeBookUtils.Type.SMITHING;
            case "stonecutting" -> RecipeBookUtils.Type.STONECUTTER;
            default -> null;
        };
    }

    // ==================== RecipeManager 兜底查找 ====================

    private record RecipeFallback(List<Ingredient> ingredients, int resultCount, RecipeBookUtils.Type type) {}

    /**
     * 当 RecipeBookUtils.getDisplayEntryFromRecipeBook 找不到配方时，
     * 直接从 RecipeManager 遍历所有配方查找匹配项。
     * 使用反射避免 1.21.10 中 API 变更导致的编译问题。
     */
    @SuppressWarnings("unchecked")
    private RecipeFallback findRecipeFromManager(Item targetItem) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null) return null;
            var recipeManager = client.world.getRecipeManager();
            var registryManager = client.world.getRegistryManager();

            // 通过反射获取所有 RecipeEntry
            List<Object> recipeEntries = new ArrayList<>();
            try {
                var valuesMethod = recipeManager.getClass().getMethod("values");
                Collection<?> values = (Collection<?>) valuesMethod.invoke(recipeManager);
                for (Object entry : values) {
                    recipeEntries.add(entry);
                }
            } catch (NoSuchMethodException e) {
                // 通过 getIds() + get() 组合获取
                var keysMethod = recipeManager.getClass().getMethod("getIds");
                var keys = (Collection<Identifier>) keysMethod.invoke(recipeManager);
                var getMethod = recipeManager.getClass().getMethod("get", Identifier.class);
                for (Identifier id : keys) {
                    var opt = (Optional<?>) getMethod.invoke(recipeManager, id);
                    opt.ifPresent(o -> recipeEntries.add(o));
                }
            }

            for (Object entryObj : recipeEntries) {
                // entryObj 是 RecipeEntry<?>
                Object recipe = null;
                try {
                    var valueMethod = entryObj.getClass().getMethod("value");
                    recipe = valueMethod.invoke(entryObj);
                } catch (Exception ignored) {
                    continue;
                }
                if (recipe == null) continue;

                // 尝试获取配方结果
                ItemStack result = getRecipeResult(recipe, registryManager);
                if (result == null || result.isEmpty() || result.getItem() != targetItem) continue;

                List<Ingredient> ingredients = getIngredientsSafe((Recipe<?>) recipe);
                if (ingredients.isEmpty()) continue;

                RecipeBookUtils.Type type = getRecipeTypeSafe((Recipe<?>) recipe);
                if (type == null) continue;

                LitematListMod.LOGGER.debug("[RawMaterialTreeAnalyzer] RecipeManager找到配方: {} -> {} (类型: {})",
                        Registries.ITEM.getId(targetItem), recipe.getClass().getSimpleName(), type);
                return new RecipeFallback(ingredients, result.getCount(), type);
            }
        } catch (Exception e) {
            LitematListMod.LOGGER.warn("RecipeManager 兜底查找失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 通过反射获取配方结果 ItemStack。
     * 兼容 1.21.10 中 Recipe.getResult(RegistryWrapper.WrapperLookup) 方法。
     */
    private ItemStack getRecipeResult(Object recipe, Object registryManager) {
        // 尝试 getResult(RegistryWrapper.WrapperLookup)
        try {
            var getResultMethod = recipe.getClass().getMethod("getResult",
                    RegistryWrapper.WrapperLookup.class);
            return (ItemStack) getResultMethod.invoke(recipe, registryManager);
        } catch (Exception ignored) {}
        // 尝试 getResult() 无参
        try {
            var getResultMethod = recipe.getClass().getMethod("getResult");
            return (ItemStack) getResultMethod.invoke(recipe);
        } catch (Exception ignored) {}
        // 尝试 getOutput() 方法
        try {
            var getOutputMethod = recipe.getClass().getMethod("getOutput",
                    RegistryWrapper.WrapperLookup.class);
            return (ItemStack) getOutputMethod.invoke(recipe, registryManager);
        } catch (Exception ignored) {}
        return null;
    }

    /** 通过反射安全获取配方原料列表 */
    @SuppressWarnings("unchecked")
    private List<Ingredient> getIngredientsSafe(Recipe<?> recipe) {
        // 尝试 getIngredients()（ShapedRecipe / ShapelessRecipe）
        try {
            var method = recipe.getClass().getMethod("getIngredients");
            return (List<Ingredient>) method.invoke(recipe);
        } catch (Exception ignored) {}

        // 尝试 ingredient 字段（AbstractCookingRecipe / SmeltingRecipe 等，可能在父类中）
        try {
            var field = findFieldInHierarchy(recipe.getClass(), "ingredient");
            if (field != null) {
                field.setAccessible(true);
                return List.of((Ingredient) field.get(recipe));
            }
        } catch (Exception ignored) {}

        // 尝试 input 字段（StonecuttingRecipe）
        try {
            var field = findFieldInHierarchy(recipe.getClass(), "input");
            if (field != null) {
                field.setAccessible(true);
                return List.of((Ingredient) field.get(recipe));
            }
        } catch (Exception ignored) {}

        // 尝试 template/base/addition 字段（SmithingTransformRecipe / SmithingTrimRecipe，可能在父类中）
        try {
            var tField = findFieldInHierarchy(recipe.getClass(), "template");
            var bField = findFieldInHierarchy(recipe.getClass(), "base");
            var aField = findFieldInHierarchy(recipe.getClass(), "addition");
            if (tField != null && bField != null && aField != null) {
                tField.setAccessible(true);
                bField.setAccessible(true);
                aField.setAccessible(true);
                return List.of(
                        (Ingredient) tField.get(recipe),
                        (Ingredient) bField.get(recipe),
                        (Ingredient) aField.get(recipe));
            }
        } catch (Exception ignored) {}

        return List.of();
    }

    /**
     * 在类继承层次中查找指定名称的字段。
     * 因为 getDeclaredField 只查找当前类，某些字段在父类中定义（如 AbstractCookingRecipe.ingredient）。
     */
    private java.lang.reflect.Field findFieldInHierarchy(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /** 通过类名判断配方类型 */
    private RecipeBookUtils.Type getRecipeTypeSafe(Recipe<?> recipe) {
        String name = recipe.getClass().getSimpleName();
        if (name.contains("Shaped")) return RecipeBookUtils.Type.SHAPED;
        if (name.contains("Shapeless")) return RecipeBookUtils.Type.SHAPELESS;
        if (name.contains("Smelting") || name.contains("Furnace")
                || name.contains("Blasting") || name.contains("Smoking")
                || name.contains("Campfire")) return RecipeBookUtils.Type.FURNACE;
        if (name.contains("Smithing")) return RecipeBookUtils.Type.SMITHING;
        if (name.contains("Stonecutting")) return RecipeBookUtils.Type.STONECUTTER;

        // 检查父类是否为 AbstractCookingRecipe（熔炉类配方）
        if (isCookingRecipe(recipe)) return RecipeBookUtils.Type.FURNACE;

        LitematListMod.LOGGER.warn("[RawMaterialTreeAnalyzer] 未知配方类型: {}", name);
        return null;
    }

    /**
     * 从多匹配原料中选择白色/默认变种作为染色/通配基底。
     * 仅羊毛和床支持染色通配，枯叶堆默认橡木树叶。
     */
    private static RegistryEntry<Item> pickBaseDyeableIngredient(List<ItemStack> stacks) {
        // 优先选择白色/默认变种作为基底
        for (ItemStack stack : stacks) {
            String id = Registries.ITEM.getId(stack.getItem()).toString();
            if (id.equals("minecraft:white_wool") || id.equals("minecraft:white_bed")
                    || id.equals("minecraft:oak_leaves")) {
                return stack.getRegistryEntry();
            }
        }
        // 木板/半砖类：优先选择橡木变种作为默认基底
        for (ItemStack stack : stacks) {
            String id = Registries.ITEM.getId(stack.getItem()).toString();
            if (id.equals("minecraft:oak_planks") || id.equals("minecraft:oak_slab")) {
                return stack.getRegistryEntry();
            }
        }
        // 圆石类：优先选择圆石作为默认基底（熔炉/发射器/投掷器/侦测器配方）
        for (ItemStack stack : stacks) {
            String id = Registries.ITEM.getId(stack.getItem()).toString();
            if (id.equals("minecraft:cobblestone")) {
                return stack.getRegistryEntry();
            }
        }
        return stacks.getFirst().getRegistryEntry();
    }

    /** 检查是否继承自熔炉类配方（AbstractCookingRecipe） */
    private boolean isCookingRecipe(Recipe<?> recipe) {
        Class<?> current = recipe.getClass().getSuperclass();
        while (current != null) {
            String name = current.getSimpleName();
            if (name.contains("Cooking") || name.contains("Furnace")) return true;
            current = current.getSuperclass();
        }
        return false;
    }
}