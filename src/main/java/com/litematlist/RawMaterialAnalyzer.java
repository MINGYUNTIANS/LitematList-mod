package com.litematlist;

import com.litematlist.config.RawMaterialConfig;
import com.litematlist.gui.MaterialDataStore.RawMaterialEntry;
import com.litematlist.LitematicReader.MaterialEntry;
//#if MC == 12108 || MC == 12110 || MC == 12111
import fi.dy.masa.malilib.util.game.RecipeBookUtils;
//#elseif MC == 12101 || MC == 12104
//$$import com.litematlist.RecipeBookUtils;
//#endif
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.recipe.NetworkRecipeId;
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.recipe.RecipeDisplayEntry;
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.recipe.display.SlotDisplay;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.registry.Registries;
//#endif
//#if MC == 12101
//$$import net.minecraft.registry.RegistryWrapper;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.registry.entry.RegistryEntry;
//#endif
import net.minecraft.util.Identifier;
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$import net.minecraft.recipe.RecipeManager;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$import net.minecraft.recipe.Recipe;
//#endif
//#if MC == 11904 || MC == 12001
//$$import net.minecraft.registry.RegistryWrapper;
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.util.context.ContextParameterMap;
//#endif
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.math.Fraction;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
//#if MC == 11701
//$$import net.minecraft.util.registry.Registry;
//#endif

/**
 * 原材料溯源分析器：参考 Litematica MaterialListJsonEntry/MaterialListJsonBase 逻辑。
 * 从成品材料列表递归溯源到基础原材料，计算冗余。
 * 使用 RecipeBookUtils.getDisplayEntryFromRecipeBook 查询配方。
 */
public class RawMaterialAnalyzer {

    /** 通配物品ID列表（可使用多种材料合成，溯源时停在配方原材料层级） */
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
    private static final Set<String> WILDCARD_ITEM_IDS = Set.of(
        "minecraft:crafting_table", "minecraft:chest", "minecraft:trapped_chest",
        "minecraft:note_block", "minecraft:jukebox", "minecraft:loom",
        "minecraft:cartography_table", "minecraft:fletching_table", "minecraft:smithing_table",
        "minecraft:barrel", "minecraft:bookshelf", "minecraft:lectern",
        "minecraft:piston", "minecraft:sticky_piston", "minecraft:comparator",
        "minecraft:repeater", "minecraft:daylight_detector",
        "minecraft:wooden_sword", "minecraft:wooden_pickaxe", "minecraft:wooden_axe",
        "minecraft:wooden_shovel", "minecraft:wooden_hoe",
        "minecraft:item_frame",
        "minecraft:stone_sword", "minecraft:stone_pickaxe", "minecraft:stone_axe",
        "minecraft:stone_shovel", "minecraft:stone_hoe",
        "minecraft:furnace"
    );

    private final List<RawMaterialEntry> redundancy = new ArrayList<>();
    private final List<RawMaterialEntry> rawMaterialList = new ArrayList<>();
    private final Set<Item> whitelist;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private final List<List<RecipeBookUtils.Type>> recipePriority;
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    private final List<List<String>> recipePriority;
//#endif
    private int depth = 0;

    public RawMaterialAnalyzer() {
        this.whitelist = RawMaterialConfig.currentWhitelist;
        this.recipePriority = RawMaterialConfig.getEnabledRecipeTypeGroups();
    }

    public RawMaterialResult analyze(List<MaterialEntry> entries) {
        rawMaterialList.clear();
        redundancy.clear();

        for (MaterialEntry entry : entries) {
            Item item = entry.item();
            if (item == null) continue;
            // 精确物品（带非默认组件的特殊物品）不参与原材料溯源
            if (entry.isExact()) continue;
            int count = entry.totalCount();
            depth = 0;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            traceItem(item.getRegistryEntry(), count, null, "(顶层) " + entry.blockName());
//#elseif MC == 11701
//$$            traceItem(getEntry(item), count, null, "(顶层) " + entry.blockName());
//#endif
        }

        return new RawMaterialResult(new ArrayList<>(rawMaterialList), redundancy);
    }

    /**
     * 递归溯源单个物品。
     * 按配置的优先级顺序查找配方，找到第一个匹配即使用。
     * 使用 RecipeBookUtils.getDisplayEntryFromRecipeBook 查询配方（参考 Litematica MaterialListJsonEntry.build）。
     */
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void traceItem(RegistryEntry<Item> item, int count, RegistryEntry<Item> prevItem, String sourceDesc) {
//#elseif MC == 11701
//$$    private void traceItem(Item item, int count, Item prevItem, String sourceDesc) {
//#endif
        if (count <= 0) return;
        if (depth > RawMaterialConfig.MAX_RECURSION_DEPTH) {
            addRawMaterial(item, count, sourceDesc);
            return;
        }

//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Item mcItem = item.value();
//#elseif MC == 11701
//$$        Item mcItem = item;
//#endif
        if (whitelist.contains(mcItem)) {
            addRawMaterial(item, count, sourceDesc);
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            addRawMaterial(item, count, sourceDesc);
            return;
        }

//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        ContextParameterMap map = RecipeBookUtils.getMap(client);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // ContextParameterMap not available in 1.21
//#elseif MC == 12101
//$$        RegistryWrapper.WrapperLookup map = RecipeBookUtils.getMap(client);
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        String itemIdStr = Registries.ITEM.getId(mcItem).toString();
//#elseif MC == 11701
//$$        String itemIdStr = Registry.ITEM.getId(mcItem).toString();
//#endif

        // 平滑石半砖特殊处理：若切石机配方不可用，尝试熔炉配方溯源到平滑石
        if ("minecraft:smooth_stone_slab".equals(itemIdStr)) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            boolean found = tryTraceSmoothStoneSlab(itemIdStr, count, sourceDesc, map);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            boolean found = tryTraceSmoothStoneSlab(itemIdStr, count, sourceDesc);
//#endif
            if (found) return;
        }

        // 按优先级分组查找配方（参考 Litematica MaterialListJsonEntry.build()）
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        for (List<RecipeBookUtils.Type> typeGroup : recipePriority) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        for (List<String> typeGroup : recipePriority) {
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
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            List<RecipeData> lookup = getDisplayEntryFromRecipeBook(shadow, typeGroup);
//#endif

            if (lookup.isEmpty()) continue;

//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            Pair<NetworkRecipeId, RecipeDisplayEntry> pair = lookup.getFirst();
//#elseif MC == 12101
//$$            Pair<Identifier, RecipeDisplayEntry> pair = lookup.getFirst();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            RecipeDisplayEntry entry = pair.getRight();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            RecipeData recipeData = lookup.get(0);
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 检查是否有配方材料
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            List<Ingredient> ingredients = recipeData.ingredients;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (entry.craftingRequirements().isEmpty()) continue;
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            if (ingredients.isEmpty()) continue;
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            List<Ingredient> ingredients = entry.craftingRequirements().get();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            List<ItemStack> resultStacks = entry.getStacks(map);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            List<ItemStack> resultStacks = recipeData.resultStacks;
//#endif
            if (resultStacks.isEmpty()) continue;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            int resultCount = resultStacks.getFirst().getCount();
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            int resultCount = resultStacks.get(0).getCount();
//#endif

            // 循环检测
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (checkLoop(ingredients, item, prevItem, map)) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            if (checkLoop(ingredients, item, prevItem)) {
//#endif
                break; // 有循环，直接作为原材料
            }

            depth++;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            RecipeBookUtils.Type primaryType = typeGroup.get(0);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            String primaryType = typeGroup.get(0);
//#endif

            // 通配物品：溯源时停在配方原材料层级，不递归深入
            if (WILDCARD_ITEM_IDS.contains(itemIdStr)) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                processWildcardRecipe(ingredients, resultCount, count, item, sourceDesc, primaryType, map);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                processWildcardRecipe(ingredients, resultCount, count, item, sourceDesc, primaryType);
//#endif
            } else {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                processRecipe(ingredients, resultCount, count, item, sourceDesc, primaryType, map);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                processRecipe(ingredients, resultCount, count, item, sourceDesc, primaryType);
//#endif
            }
            depth--;
            return;
        }

        // 无匹配配方，作为原材料
        addRawMaterial(item, count, sourceDesc);
    }

    /**
     * 平滑石半砖特殊处理：尝试切石机→平滑石→石头，若熔炉未启用则停在平滑石。
     */
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private boolean tryTraceSmoothStoneSlab(String itemIdStr, int count, String sourceDesc, ContextParameterMap map) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    private boolean tryTraceSmoothStoneSlab(String itemIdStr, int count, String sourceDesc) {
//#elseif MC == 12101
//$$    private boolean tryTraceSmoothStoneSlab(String itemIdStr, int count, String sourceDesc, RegistryWrapper.WrapperLookup map) {
//#endif
        // 尝试切石机配方（平滑石半砖 ← 平滑石）
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        for (List<RecipeBookUtils.Type> typeGroup : recipePriority) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        for (List<String> typeGroup : recipePriority) {
//#endif
            // 只尝试切石机
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (!typeGroup.contains(RecipeBookUtils.Type.STONECUTTER)) continue;
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
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (!typeGroup.contains("stonecutter")) continue;
//#endif
//#if MC == 11701
//$$            ItemStack shadow = new ItemStack(Registry.ITEM.get(Identifier.tryParse(itemIdStr)));
//#elseif MC == 11904 || MC == 12001
//$$            ItemStack shadow = new ItemStack(Registries.ITEM.get(Identifier.tryParse(itemIdStr)));
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            List<RecipeData> lookup = getDisplayEntryFromRecipeBook(shadow, typeGroup);
//#endif
            if (!lookup.isEmpty()) {
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                Pair<NetworkRecipeId, RecipeDisplayEntry> pair = lookup.getFirst();
//#elseif MC == 12101
//$$                Pair<Identifier, RecipeDisplayEntry> pair = lookup.getFirst();
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
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                RecipeData recipeData = lookup.get(0);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                List<Ingredient> ingredients = recipeData.ingredients;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                if (!ingredients.isEmpty()) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    List<ItemStack> resultStacks = recipeData.resultStacks;
//#endif
                    if (!resultStacks.isEmpty()) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        int resultCount = resultStacks.getFirst().getCount();
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                        int resultCount = resultStacks.get(0).getCount();
//#endif
                        depth++;
                        processRecipe(ingredients, resultCount, count,
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                Registries.ITEM.get(Identifier.of(itemIdStr)).getRegistryEntry(),
//#elseif MC == 11701
//$$                                getEntry(Registry.ITEM.get(Identifier.tryParse(itemIdStr))),
//#elseif MC == 11904 || MC == 12001
//$$                                Registries.ITEM.get(Identifier.tryParse(itemIdStr)).getRegistryEntry(),
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                sourceDesc, RecipeBookUtils.Type.STONECUTTER, map);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                                sourceDesc, "stonecutter");
//#endif
                        depth--;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 处理通配物品的配方：将配方原料直接作为原材料（不递归溯源），
     * 具体变种由 RawMaterialScreen 的通配替代逻辑处理。
     */
    private void processWildcardRecipe(List<Ingredient> ingredients, int resultCount, int total,
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            RegistryEntry<Item> inputItem, String sourceDesc, RecipeBookUtils.Type recipeType,
//#elseif MC == 11701
//$$            Item inputItem, String sourceDesc, String recipeType
//#elseif MC == 11904 || MC == 12001
//$$            RegistryEntry<Item> inputItem, String sourceDesc, String recipeType
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            ContextParameterMap map) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    ) {
//#elseif MC == 12101
//$$            RegistryWrapper.WrapperLookup map) {
//#endif
        int adjustedTotal = total;

        if (resultCount > 1) {
            Fraction adjusted = Fraction.getFraction(total, resultCount);
            int floor = MathHelper.floor(adjusted.floatValue());
            float remainderCalc = resultCount * (adjusted.floatValue() - floor);
            int remainderCount = Math.round(remainderCalc);
            adjustedTotal = Math.max(floor + (remainderCount > 0 ? 1 : 0), (remainderCount > 0 ? 1 : 0));
        }

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
            redundancy.add(new RawMaterialEntry(itemId, excess, List.of(desc)));
        }

//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Identifier itemId = inputItem.getKey().get().getValue();
//#elseif MC == 11701
//$$        Identifier itemId = Registry.ITEM.getId(inputItem);
//#endif
        String typeName = switch (recipeType) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            case STONECUTTER -> "切石机";
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            case "stonecutter" -> "切石机";
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            case FURNACE -> "熔炉";
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            case "smelting" -> "熔炉";
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            case SMITHING -> "锻造台";
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            case "smithing_transform" -> "锻造台";
//#endif
            default -> "工作台";
        };
        String desc = sourceDesc + " →[" + typeName + "]→ " + itemId.getPath();

        // 去重原料
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Map<RegistryEntry<Item>, Integer> dedup = new HashMap<>();
//#elseif MC == 11701
//$$        Map<Item, Integer> dedup = new HashMap<>();
//#endif
        for (Ingredient ing : ingredients) {
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            SlotDisplay display = ing.toDisplay();
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            List<ItemStack> displayStacks = display.getStacks(map);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            ItemStack[] matchingStacks = ing.getMatchingStacks();
//#elseif MC == 12101
//$$            List<ItemStack> displayStacks = RecipeBookUtils.getIngredientStacks(ing);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (displayStacks.isEmpty()) continue;
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            if (matchingStacks.length == 0) continue;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            RegistryEntry<Item> ingItem = displayStacks.getFirst().getRegistryEntry();
//#elseif MC == 11701
//$$            Item ingItem = getEntry(matchingStacks[0]);
//#elseif MC == 11904 || MC == 12001
//$$            RegistryEntry<Item> ingItem = matchingStacks[0].getRegistryEntry();
//#endif
            dedup.merge(ingItem, adjustedTotal, Integer::sum);
        }

        // 通配物品的配方原料直接作为原材料，不递归溯源
        for (var entry : dedup.entrySet()) {
            addRawMaterial(entry.getKey(), entry.getValue(), desc);
        }
    }

    /**
     * 处理配方：计算批次数，递归处理每个原料。
     * 参考 MaterialListJsonEntry.build() 中的 adjustedTotal 逻辑。
     */
    private void processRecipe(List<Ingredient> ingredients, int resultCount, int total,
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            RegistryEntry<Item> inputItem, String sourceDesc, RecipeBookUtils.Type recipeType,
//#elseif MC == 11701
//$$            Item inputItem, String sourceDesc, String recipeType
//#elseif MC == 11904 || MC == 12001
//$$            RegistryEntry<Item> inputItem, String sourceDesc, String recipeType
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            ContextParameterMap map) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    ) {
//#elseif MC == 12101
//$$            RegistryWrapper.WrapperLookup map) {
//#endif
        int adjustedTotal = total;

        // 当配方一次产出多个物品时，计算需要的合成批次数
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
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            String itemId = inputItem.getIdAsString();
//#elseif MC == 11701
//$$            String itemId = Registry.ITEM.getId(inputItem).toString();
//#elseif MC == 11904 || MC == 12001
//$$            String itemId = Registries.ITEM.getId(inputItem.value()).toString();
//#endif
            String desc = sourceDesc + " (冗余: 需要" + total + "个, 产出" + actualOutput + "个)";
            redundancy.add(new RawMaterialEntry(itemId, excess, List.of(desc)));
        }

//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Identifier itemId = inputItem.getKey().get().getValue();
//#elseif MC == 11701
//$$        Identifier itemId = Registry.ITEM.getId(inputItem);
//#endif
        String typeName = switch (recipeType) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            case STONECUTTER -> "切石机";
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            case "stonecutter" -> "切石机";
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            case FURNACE -> "熔炉";
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            case "smelting" -> "熔炉";
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            case SMITHING -> "锻造台";
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            case "smithing_transform" -> "锻造台";
//#endif
            default -> "工作台";
        };
        String desc = sourceDesc + " →[" + typeName + "]→ " + itemId.getPath();

        // 去重：同一原料在配方中可能出现多次（如工作台配方中的多个同种物品）
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Map<RegistryEntry<Item>, Integer> dedup = new HashMap<>();
//#elseif MC == 11701
//$$        Map<Item, Integer> dedup = new HashMap<>();
//#endif
        for (Ingredient ing : ingredients) {
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            SlotDisplay display = ing.toDisplay();
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            List<ItemStack> displayStacks = display.getStacks(map);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            ItemStack[] matchingStacks = ing.getMatchingStacks();
//#elseif MC == 12101
//$$            List<ItemStack> displayStacks = RecipeBookUtils.getIngredientStacks(ing);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (displayStacks.isEmpty()) continue;
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            if (matchingStacks.length == 0) continue;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            RegistryEntry<Item> ingItem = displayStacks.getFirst().getRegistryEntry();
//#elseif MC == 11701
//$$            Item ingItem = getEntry(matchingStacks[0]);
//#elseif MC == 11904 || MC == 12001
//$$            RegistryEntry<Item> ingItem = matchingStacks[0].getRegistryEntry();
//#endif
            dedup.merge(ingItem, adjustedTotal, Integer::sum);
        }

        for (var entry : dedup.entrySet()) {
            traceItem(entry.getKey(), entry.getValue(), inputItem, desc);
        }
    }

    /**
     * 循环检测：检查配方原料中是否包含当前物品或上一级物品。
     * 参考 MaterialListJsonBase.checkIfLoop()。
     */
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private boolean checkLoop(List<Ingredient> ingredients, RegistryEntry<Item> inputItem,
//#elseif MC == 11701
//$$    private boolean checkLoop(List<Ingredient> ingredients, Item inputItem,
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            RegistryEntry<Item> prevItem, ContextParameterMap map) {
//#elseif MC == 11701
//$$            Item prevItem) {
//#elseif MC == 11904 || MC == 12001
//$$            RegistryEntry<Item> prevItem) {
//#elseif MC == 12101
//$$            RegistryEntry<Item> prevItem, RegistryWrapper.WrapperLookup map) {
//#endif
        for (Ingredient ing : ingredients) {
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            SlotDisplay display = ing.toDisplay();
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            List<ItemStack> displayStacks = display.getStacks(map);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            ItemStack[] matchingStacks = ing.getMatchingStacks();
//#elseif MC == 12101
//$$            List<ItemStack> displayStacks = RecipeBookUtils.getIngredientStacks(ing);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (displayStacks.isEmpty()) continue;
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            if (matchingStacks.length == 0) continue;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            RegistryEntry<Item> ingItem = displayStacks.getFirst().getRegistryEntry();
//#elseif MC == 11701
//$$            Item ingItem = getEntry(matchingStacks[0]);
//#elseif MC == 11904 || MC == 12001
//$$            RegistryEntry<Item> ingItem = matchingStacks[0].getRegistryEntry();
//#endif
            if (ingItem == inputItem || ingItem == prevItem) {
                return true;
            }
        }
        return false;
    }

//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void addRawMaterial(RegistryEntry<Item> item, int count, String sourceDesc) {
//#elseif MC == 11701
//$$    private void addRawMaterial(Item item, int count, String sourceDesc) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        String itemId = item.getIdAsString();
//#elseif MC == 11701
//$$        String itemId = Registry.ITEM.getId(item).toString();
//#elseif MC == 11904 || MC == 12001
//$$        String itemId = Registries.ITEM.getId(item.value()).toString();
//#endif
        // 不合并条目：同一原材料从不同来源追踪时保留独立记录，
        // 由 RawMaterialScreen.buildRows 按 itemId 合并并折叠展示
        rawMaterialList.add(new RawMaterialEntry(itemId, count, List.of(sourceDesc + " (" + count + "个)")));
    }

    /** 分析结果 */
    public record RawMaterialResult(List<RawMaterialEntry> rawMaterials, List<RawMaterialEntry> redundancy) {}
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 配方数据：替代 Recipe<?>Entry（1.21.4 中不存在） */
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    private record RecipeData(List<Ingredient> ingredients, List<ItemStack> resultStacks) {}
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    /**
//$$     * 直接使用 RecipeManager 查找配方。
//$$     * 替代 RecipeBookUtils.getDisplayEntryFromRecipeBook。
//$$     * 1.21.4 兼容版本：不使用 Recipe<?>Entry。
//$$     */
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    private static List<RecipeData> getDisplayEntryFromRecipeBook(ItemStack shadow, List<String> typeGroup) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        List<RecipeData> results = new ArrayList<>();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        MinecraftClient client = MinecraftClient.getInstance();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (client.world == null) return results;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        var recipeManager = client.world.getRecipeManager();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        var registryManager = client.world.getRegistryManager();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        // 1.20.1：RecipeManager.values() 直接返回 Collection<Recipe<?>>
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        for (Recipe<?> recipe : recipeManager.values()) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            // 获取配方结果
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            ItemStack result = getRecipeOutput(recipe, registryManager);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (result != null && !result.isEmpty() && result.getItem() == shadow.getItem()) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                List<Ingredient> ingredients = getRecipeIngredients(recipe);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                if (!ingredients.isEmpty()) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    results.add(new RecipeData(ingredients, List.of(result)));
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        return results;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 获取配方输出（1.20.1：Recipe.getOutput(DynamicRegistryManager)） */
//#endif
//#if MC == 11701
//$$    private static ItemStack getRecipeOutput(Recipe<?> recipe, net.minecraft.util.registry.DynamicRegistryManager registryManager) {
//#elseif MC == 11904 || MC == 12001
//$$    private static ItemStack getRecipeOutput(Recipe<?> recipe, net.minecraft.registry.DynamicRegistryManager registryManager) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        try {
//#endif
//#if MC == 11701
//$$            return recipe.getOutput();
//#elseif MC == 11904 || MC == 12001
//$$            return recipe.getOutput(registryManager);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        } catch (Exception ignored) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            return null;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 获取配方材料列表 */
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    private static List<Ingredient> getRecipeIngredients(Recipe<?> recipe) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        try {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            return new ArrayList<>(recipe.getIngredients());
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        } catch (Exception ignored) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            return List.of();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    }
//#endif
}
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif