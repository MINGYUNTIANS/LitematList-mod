package com.litematlist;

import com.litematlist.config.RawMaterialConfig;
import com.litematlist.gui.MaterialDetailScreen.RawMaterialEntry;
import com.litematlist.LitematicReader.MaterialEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.recipe.display.SlotDisplayContexts;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.context.ContextParameterMap;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.math.Fraction;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * 原材料溯源分析器：参考 Litematica MaterialListJsonEntry/MaterialListJsonBase 逻辑。
 * 从成品材料列表递归溯源到基础原材料，计算冗余。
 * 使用 RecipeBookUtils.getDisplayEntryFromRecipeBook 查询配方。
 */
public class RawMaterialAnalyzer {

    /** 通配物品ID列表（可使用多种材料合成，溯源时停在配方原材料层级） */
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
    private final List<List<String>> recipePriority;
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
            int count = entry.totalCount();
            depth = 0;
            traceItem(item.getRegistryEntry(), count, null, "(顶层) " + entry.blockName());
        }

        return new RawMaterialResult(new ArrayList<>(rawMaterialList), redundancy);
    }

    /**
     * 递归溯源单个物品。
     * 按配置的优先级顺序查找配方，找到第一个匹配即使用。
     * 使用 RecipeBookUtils.getDisplayEntryFromRecipeBook 查询配方（参考 Litematica MaterialListJsonEntry.build）。
     */
    private void traceItem(RegistryEntry<Item> item, int count, RegistryEntry<Item> prevItem, String sourceDesc) {
        if (count <= 0) return;
        if (depth > RawMaterialConfig.MAX_RECURSION_DEPTH) {
            addRawMaterial(item, count, sourceDesc);
            return;
        }

        Item mcItem = item.value();
        if (whitelist.contains(mcItem)) {
            addRawMaterial(item, count, sourceDesc);
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            addRawMaterial(item, count, sourceDesc);
            return;
        }

        ContextParameterMap map = SlotDisplayContexts.createParameters(client.world);
        String itemIdStr = Registries.ITEM.getId(mcItem).toString();

        // 平滑石半砖特殊处理：若切石机配方不可用，尝试熔炉配方溯源到平滑石
        if ("minecraft:smooth_stone_slab".equals(itemIdStr)) {
            boolean found = tryTraceSmoothStoneSlab(itemIdStr, count, sourceDesc, map);
            if (found) return;
        }

        // 按优先级分组查找配方（参考 Litematica MaterialListJsonEntry.build()）
        for (List<String> typeGroup : recipePriority) {
            ItemStack shadow = new ItemStack(item);
            List<RecipeData> lookup = getDisplayEntryFromRecipeBook(shadow, typeGroup);

            if (lookup.isEmpty()) continue;

            RecipeData recipeData = lookup.getFirst();

            List<Ingredient> ingredients = recipeData.ingredients;
            if (ingredients.isEmpty()) continue;

            List<ItemStack> resultStacks = recipeData.resultStacks;
            if (resultStacks.isEmpty()) continue;

            int resultCount = resultStacks.getFirst().getCount();

            // 循环检测
            if (checkLoop(ingredients, item, prevItem, map)) {
                break; // 有循环，直接作为原材料
            }

            depth++;
            String primaryType = typeGroup.get(0);

            // 通配物品：溯源时停在配方原材料层级，不递归深入
            if (WILDCARD_ITEM_IDS.contains(itemIdStr)) {
                processWildcardRecipe(ingredients, resultCount, count, item, sourceDesc, primaryType, map);
            } else {
                processRecipe(ingredients, resultCount, count, item, sourceDesc, primaryType, map);
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
    private boolean tryTraceSmoothStoneSlab(String itemIdStr, int count, String sourceDesc, ContextParameterMap map) {
        // 尝试切石机配方（平滑石半砖 ← 平滑石）
        for (List<String> typeGroup : recipePriority) {
            // 只尝试切石机
            if (!typeGroup.contains("stonecutter")) continue;
            ItemStack shadow = new ItemStack(Registries.ITEM.get(Identifier.of(itemIdStr)));
            List<RecipeData> lookup = getDisplayEntryFromRecipeBook(shadow, typeGroup);
            if (!lookup.isEmpty()) {
                RecipeData recipeData = lookup.getFirst();
                List<Ingredient> ingredients = recipeData.ingredients;
                if (!ingredients.isEmpty()) {
                    List<ItemStack> resultStacks = recipeData.resultStacks;
                    if (!resultStacks.isEmpty()) {
                        int resultCount = resultStacks.getFirst().getCount();
                        depth++;
                        processRecipe(ingredients, resultCount, count,
                                Registries.ITEM.get(Identifier.of(itemIdStr)).getRegistryEntry(),
                                sourceDesc, "stonecutter", map);
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
            RegistryEntry<Item> inputItem, String sourceDesc, String recipeType,
            ContextParameterMap map) {
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
            String itemId = inputItem.getIdAsString();
            String desc = sourceDesc + " (冗余: 需要" + total + "个, 产出" + actualOutput + "个)";
            redundancy.add(new RawMaterialEntry(itemId, excess, List.of(desc)));
        }

        Identifier itemId = inputItem.getKey().get().getValue();
        String typeName = switch (recipeType) {
            case "stonecutter" -> "切石机";
            case "smelting" -> "熔炉";
            case "smithing_transform" -> "锻造台";
            default -> "工作台";
        };
        String desc = sourceDesc + " →[" + typeName + "]→ " + itemId.getPath();

        // 去重原料
        Map<RegistryEntry<Item>, Integer> dedup = new HashMap<>();
        for (Ingredient ing : ingredients) {
            SlotDisplay display = ing.toDisplay();
            List<ItemStack> displayStacks = display.getStacks(map);
            if (displayStacks.isEmpty()) continue;
            RegistryEntry<Item> ingItem = displayStacks.getFirst().getRegistryEntry();
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
            RegistryEntry<Item> inputItem, String sourceDesc, String recipeType,
            ContextParameterMap map) {
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
            String itemId = inputItem.getIdAsString();
            String desc = sourceDesc + " (冗余: 需要" + total + "个, 产出" + actualOutput + "个)";
            redundancy.add(new RawMaterialEntry(itemId, excess, List.of(desc)));
        }

        Identifier itemId = inputItem.getKey().get().getValue();
        String typeName = switch (recipeType) {
            case "stonecutter" -> "切石机";
            case "smelting" -> "熔炉";
            case "smithing_transform" -> "锻造台";
            default -> "工作台";
        };
        String desc = sourceDesc + " →[" + typeName + "]→ " + itemId.getPath();

        // 去重：同一原料在配方中可能出现多次（如工作台配方中的多个同种物品）
        Map<RegistryEntry<Item>, Integer> dedup = new HashMap<>();
        for (Ingredient ing : ingredients) {
            SlotDisplay display = ing.toDisplay();
            List<ItemStack> displayStacks = display.getStacks(map);
            if (displayStacks.isEmpty()) continue;
            RegistryEntry<Item> ingItem = displayStacks.getFirst().getRegistryEntry();
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

    private void addRawMaterial(RegistryEntry<Item> item, int count, String sourceDesc) {
        String itemId = item.getIdAsString();
        // 不合并条目：同一原材料从不同来源追踪时保留独立记录，
        // 由 RawMaterialScreen.buildRows 按 itemId 合并并折叠展示
        rawMaterialList.add(new RawMaterialEntry(itemId, count, List.of(sourceDesc + " (" + count + "个)")));
    }

    /** 分析结果 */
    public record RawMaterialResult(List<RawMaterialEntry> rawMaterials, List<RawMaterialEntry> redundancy) {}

    /** 配方数据：替代 RecipeDisplayEntry（1.21.4 中不存在） */
    private record RecipeData(List<Ingredient> ingredients, List<ItemStack> resultStacks) {}

    /**
     * 通过客户端配方簿查询配方（1.21.4 无 RecipeDisplayEntry 顶层 API 差异，
     * 统一走 RecipeHelper，内部从 ClientRecipeBook 读取）。
     */
    private static List<RecipeData> getDisplayEntryFromRecipeBook(ItemStack shadow, List<String> typeGroup) {
        List<RecipeData> results = new ArrayList<>();
        MinecraftClient client = MinecraftClient.getInstance();
        for (RecipeHelper.RecipeLookupResult lookup : RecipeHelper.lookupAllRecipes(shadow, typeGroup, client)) {
            results.add(new RecipeData(lookup.ingredients(), lookup.resultStacks()));
        }
        return results;
    }
}
