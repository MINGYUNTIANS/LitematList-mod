package com.litematlist;

import com.litematlist.LitematicReader.MaterialEntry;
import com.litematlist.config.RawMaterialConfig;
import com.litematlist.gui.MaterialDetailScreen;
import fi.dy.masa.malilib.mixin.recipe.IMixinClientRecipeBook;
import fi.dy.masa.malilib.util.game.RecipeBookUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
            RecipeTreeNode node = traceItem(item.builtInRegistryHolder(), entry.totalCount(), null,
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

        for (List<RecipeBookUtils.Type> typeGroup : orderedPriority) {
            ItemStack shadow = new ItemStack(item);
            List<Pair<RecipeDisplayId, RecipeDisplayEntry>> lookup =
                    lookupRecipeEntries(shadow, typeGroup, map);

            if (lookup.isEmpty()) continue;

            // 床类优先有序（SHAPED）配方：白床走羊毛+木板，避免选到"任意床+白色染料"的循环染色配方
            boolean preferShaped = itemIdStr.endsWith("_bed");
            RecipeDisplayEntry entry = null;
            for (Pair<RecipeDisplayId, RecipeDisplayEntry> pair : lookup) {
                RecipeDisplayEntry candidate = pair.getRight();
                if (extractIngredientSlots(candidate).isEmpty()) continue;
                if (preferShaped
                        && RecipeBookUtils.Type.fromRecipeDisplay(candidate.display()) != RecipeBookUtils.Type.SHAPED) {
                    continue;
                }
                entry = candidate;
                break;
            }
            if (entry == null) continue;

            String typeName = recipeTypeToJson(typeGroup.get(0));

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
     * 白色羊毛/白色床特殊处理：跳过循环染色配方（任意羊毛/床+白色染料），
     * 只用合成配方（白色羊毛走线合成，白色床走羊毛+木板）。
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
            redundancy.add(new MaterialDetailScreen.RawMaterialEntry(itemId, excess, List.of(desc)));
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
                childId = entry.getKey().getRegisteredName();
            }
            if (WILDCARD_MATERIAL_IDS.contains(childId)) {
                // 通配原材料，继续溯源但标记为可替换（如木板→原木）
                // 如果 childId 被替换了，使用替换后的物品进行溯源
                Holder<Item> traceEntry = entry.getKey();
                String originalId = entry.getKey().getRegisteredName();
                if (!childId.equals(originalId)) {
                    Item replacedItem = BuiltInRegistries.ITEM.getOptional(Identifier.parse(childId)).orElse(null);
                    traceEntry = replacedItem.builtInRegistryHolder();
                }
                // 雕文书架半砖不显示替换按钮：半砖材质随木板变动，不允许单独替换
                boolean isChiseledBookshelfSlab = "minecraft:chiseled_bookshelf".equals(parentId)
                        && childId.endsWith("_slab");
                RecipeTreeNode sub = traceItem(traceEntry, entry.getValue(), inputItem, sourceDesc);
                if (sub != null) {
                    sub.wildcardSubMaterial = parentWildcard && !isChiseledBookshelfSlab;
                    subMaterials.add(sub);
                } else {
                    RecipeTreeNode base = RecipeTreeNode.base(entry.getKey().getRegisteredName(), entry.getValue());
                    base.wildcardSubMaterial = parentWildcard && !isChiseledBookshelfSlab;
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
            redundancy.add(new MaterialDetailScreen.RawMaterialEntry(itemId, excess, List.of(desc)));
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
                    || id.equals("minecraft:oak_leaves")) {
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
