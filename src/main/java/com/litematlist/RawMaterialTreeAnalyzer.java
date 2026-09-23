package com.litematlist;

import com.litematlist.LitematicReader.MaterialEntry;
import com.litematlist.config.RawMaterialConfig;
import com.litematlist.gui.MaterialDetailScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.math.Fraction;

import java.util.*;

import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.util.context.ContextParameterMap;

public class RawMaterialTreeAnalyzer {

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
        "minecraft:chest", "minecraft:trapped_chest", "minecraft:crafting_table",
        "minecraft:barrel", "minecraft:loom", "minecraft:furnace", "minecraft:lectern",
        "minecraft:white_wool", "minecraft:orange_wool", "minecraft:magenta_wool",
        "minecraft:light_blue_wool", "minecraft:yellow_wool", "minecraft:lime_wool",
        "minecraft:pink_wool", "minecraft:gray_wool", "minecraft:light_gray_wool",
        "minecraft:cyan_wool", "minecraft:purple_wool", "minecraft:blue_wool",
        "minecraft:brown_wool", "minecraft:green_wool", "minecraft:red_wool",
        "minecraft:black_wool",
        "minecraft:white_bed", "minecraft:orange_bed", "minecraft:magenta_bed",
        "minecraft:light_blue_bed", "minecraft:yellow_bed", "minecraft:lime_bed",
        "minecraft:pink_bed", "minecraft:gray_bed", "minecraft:light_gray_bed",
        "minecraft:cyan_bed", "minecraft:purple_bed", "minecraft:blue_bed",
        "minecraft:brown_bed", "minecraft:green_bed", "minecraft:red_bed",
        "minecraft:black_bed",
        "minecraft:leaf_litter",
        // 发射器、投掷器、侦测器（圆石通配，同熔炉）
        "minecraft:dispenser", "minecraft:dropper", "minecraft:observer",
        // 火把、灵魂火把（煤炭/木炭通配）
        "minecraft:torch", "minecraft:soul_torch"
    );

    private static final Set<String> WILDCARD_MATERIAL_IDS = Set.of(
        "minecraft:oak_planks", "minecraft:spruce_planks", "minecraft:birch_planks",
        "minecraft:jungle_planks", "minecraft:acacia_planks", "minecraft:dark_oak_planks",
        "minecraft:mangrove_planks", "minecraft:cherry_planks", "minecraft:bamboo_planks",
        "minecraft:crimson_planks", "minecraft:warped_planks",
        "minecraft:oak_slab", "minecraft:spruce_slab", "minecraft:birch_slab",
        "minecraft:jungle_slab", "minecraft:acacia_slab", "minecraft:dark_oak_slab",
        "minecraft:mangrove_slab", "minecraft:cherry_slab", "minecraft:bamboo_slab",
        "minecraft:crimson_slab", "minecraft:warped_slab",
        "minecraft:cobblestone", "minecraft:blackstone", "minecraft:cobbled_deepslate",
        "minecraft:white_wool", "minecraft:white_bed",
        "minecraft:sand", "minecraft:gravel",
        "minecraft:oak_leaves", "minecraft:spruce_leaves", "minecraft:birch_leaves",
        "minecraft:jungle_leaves", "minecraft:acacia_leaves", "minecraft:dark_oak_leaves",
        "minecraft:mangrove_leaves", "minecraft:cherry_leaves", "minecraft:azalea_leaves",
        "minecraft:flowering_azalea_leaves",
        // 煤炭/木炭互替（用于火把等配方）
        "minecraft:coal", "minecraft:charcoal"
    );

    public static boolean isWildcard(String itemId) { return WILDCARD_ITEM_IDS.contains(itemId); }
    public static boolean isWildcardMaterial(String itemId) { return WILDCARD_MATERIAL_IDS.contains(itemId); }

    private final Set<Item> whitelist;
    private final List<List<String>> recipePriority;
    private int depth = 0;
    private int maxDepth = 20;

    private static final Map<String, String> PREFERRED_RECIPE_TYPES = new HashMap<>();
    private static final Map<String, String> WILDCARD_REPLACEMENTS = new HashMap<>();
    private static final Map<String, String> PER_CONTEXT_REPLACEMENTS = new HashMap<>();
    public static void setWildcardReplacement(String originalId, String newId) {
        String realKey = originalId;
        for (Map.Entry<String, String> e : WILDCARD_REPLACEMENTS.entrySet()) {
            if (e.getValue().equals(originalId)) { realKey = e.getKey(); break; }
        }
        if (realKey.equals(originalId)) {
            for (Map.Entry<String, String> e : PER_CONTEXT_REPLACEMENTS.entrySet()) {
                if (e.getValue().equals(originalId)) {
                    realKey = e.getKey().substring(e.getKey().indexOf(58) + 1); break;
                }
            }
        }
        final String fOrig = originalId, fReal = realKey;
        PER_CONTEXT_REPLACEMENTS.entrySet().removeIf(e -> e.getValue().equals(fOrig) || e.getValue().equals(fReal));
        if (newId == null || newId.equals(realKey)) WILDCARD_REPLACEMENTS.remove(realKey);
        else WILDCARD_REPLACEMENTS.put(realKey, newId);
    }

    public static String getWildcardReplacement(String itemId) { return WILDCARD_REPLACEMENTS.getOrDefault(itemId, itemId); }

    public static void setContextWildcardReplacement(String parentId, String childId, String newId) {
        String key = parentId + ":" + childId;
        String realChildId = childId;
        boolean resolvedFromGlobal = false;
        for (Map.Entry<String, String> e : PER_CONTEXT_REPLACEMENTS.entrySet()) {
            if (e.getKey().startsWith(parentId + ":") && e.getValue().equals(childId)) {
                realChildId = e.getKey().substring(parentId.length() + 1); break;
            }
        }
        if (realChildId.equals(childId)) {
            for (Map.Entry<String, String> e : WILDCARD_REPLACEMENTS.entrySet()) {
                if (e.getValue().equals(childId)) { realChildId = e.getKey(); resolvedFromGlobal = true; break; }
            }
        }
        String realKey = parentId + ":" + realChildId;
        if (newId == null || newId.equals(realChildId)) {
            PER_CONTEXT_REPLACEMENTS.remove(realKey);
            if (resolvedFromGlobal) WILDCARD_REPLACEMENTS.remove(realChildId);
        } else {
            PER_CONTEXT_REPLACEMENTS.put(realKey, newId);
        }
    }

    public static String getContextWildcardReplacement(String parentId, String childId) {
        String key = parentId + ":" + childId;
        return PER_CONTEXT_REPLACEMENTS.getOrDefault(key, childId);
    }

    public static void setPreferredRecipeType(String itemId, String recipeType) { PREFERRED_RECIPE_TYPES.put(itemId, recipeType); }

    public static String toggleRecipeType(String itemId, String currentType, List<String> alternatives) {
        if (alternatives == null || alternatives.isEmpty()) return currentType;
        String preferred = PREFERRED_RECIPE_TYPES.get(itemId);
        if (preferred == null) { PREFERRED_RECIPE_TYPES.put(itemId, alternatives.get(0)); return alternatives.get(0); }
        int idx = alternatives.indexOf(preferred);
        if (idx < 0 || idx >= alternatives.size() - 1) { PREFERRED_RECIPE_TYPES.remove(itemId); return null; }
        String next = alternatives.get(idx + 1);
        PREFERRED_RECIPE_TYPES.put(itemId, next);
        return next;
    }

    private final List<MaterialDetailScreen.RawMaterialEntry> redundancy = new ArrayList<>();

    public RawMaterialTreeAnalyzer() {
        this.whitelist = RawMaterialConfig.currentWhitelist;
        this.recipePriority = RawMaterialConfig.getEnabledRecipeTypeGroups();
    }

    public void setMaxDepth(int maxDepth) { this.maxDepth = maxDepth; }

    public record TreeResult(List<RecipeTreeNode> roots, List<MaterialDetailScreen.RawMaterialEntry> redundancy) {}

    public TreeResult analyze(List<MaterialEntry> entries) {
        List<RecipeTreeNode> roots = new ArrayList<>();
        redundancy.clear();
        for (MaterialEntry entry : entries) {
            Item item = entry.item();
            if (item == null) continue;
            depth = 0;
            Set<String> visited = new HashSet<>();
            RecipeTreeNode node = traceItem(item.getRegistryEntry(), entry.totalCount(), null, "(顶层) " + entry.blockName(), visited);
            if (node != null) roots.add(node);
        }
        return new TreeResult(roots, new ArrayList<>(redundancy));
    }

    private RecipeTreeNode traceItem(RegistryEntry<Item> item, int count, RegistryEntry<Item> prevItem, String sourceDesc, Set<String> visited) {
        if (count <= 0) return null;
        if (depth >= maxDepth) return RecipeTreeNode.base(item.getIdAsString(), count);
        Item mcItem = item.value();
        if (whitelist.contains(mcItem)) return RecipeTreeNode.base(item.getIdAsString(), count);
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return RecipeTreeNode.base(item.getIdAsString(), count);
        String itemIdStr = Registries.ITEM.getId(mcItem).toString();

        // 溯源记录表：如果该物品已在当前溯源路径中出现过，停止进一步分解，防止循环溯源
        if (!visited.add(itemIdStr)) {
            return RecipeTreeNode.base(item.getIdAsString(), count);
        }

        if ("minecraft:smooth_stone_slab".equals(itemIdStr)) {
            RecipeTreeNode n = tryTraceSmoothStoneSlab(itemIdStr, count, client, visited);
            if (n != null) return n;
        }
        if ("minecraft:chiseled_quartz_block".equals(itemIdStr)) {
            RecipeTreeNode n = tryTraceChiseledQuartzBlock(itemIdStr, count, client, visited);
            if (n != null) return n;
        }
        // 白色床/白色羊毛：跳过"白染料+彩色床/羊毛→白色"的回染配方，避免白床被错误分解为「彩色床+白染料」
        if ("minecraft:white_bed".equals(itemIdStr) || "minecraft:white_wool".equals(itemIdStr)) {
            RecipeTreeNode n = tryTraceWhiteDyeable(itemIdStr, count, item, sourceDesc, client, visited);
            if (n != null) return n;
        }
        if (itemIdStr.endsWith("_bed") && !"minecraft:white_bed".equals(itemIdStr)) {
            RecipeTreeNode n = tryTraceBedDyeing(itemIdStr, count, item, sourceDesc, client, visited);
            if (n != null) return n;
        }

        String preferred = PREFERRED_RECIPE_TYPES.get(itemIdStr);
        List<List<String>> orderedPriority = recipePriority;
        if (preferred != null) {
            orderedPriority = new ArrayList<>(recipePriority);
            for (int i = 0; i < orderedPriority.size(); i++) {
                if (orderedPriority.get(i).contains(preferred)) {
                    if (i > 0) { List<String> g = orderedPriority.remove(i); orderedPriority.add(0, g); }
                    break;
                }
            }
        }

        RecipeHelper.RecipeLookupResult primaryResult = null;
        List<Ingredient> primaryIngredients = null;
        int primaryResultCount = 0;
        String primaryTypeStr = null;
        List<String> alternativeTypes = new ArrayList<>();

        for (List<String> typeGroup : orderedPriority) {
            ItemStack shadow = new ItemStack(item);
            RecipeHelper.RecipeLookupResult lookup = RecipeHelper.lookupRecipe(shadow, typeGroup, client);
            if (lookup == null) continue;
            if (primaryResult == null) {
                primaryResult = lookup;
                primaryIngredients = lookup.ingredients();
                primaryResultCount = lookup.resultCount();
                primaryTypeStr = typeGroup.get(0);
            } else {
                String tn = typeGroup.get(0);
                if (!alternativeTypes.contains(tn)) alternativeTypes.add(tn);
            }
        }

        if (primaryResult != null) {
            if (checkLoop(primaryIngredients, item, prevItem, client)) return RecipeTreeNode.base(item.getIdAsString(), count);
            depth++;
            RecipeTreeNode node;
            if (WILDCARD_ITEM_IDS.contains(itemIdStr)) {
                node = processWildcardRecipe(primaryIngredients, primaryResultCount, count, item, sourceDesc, primaryTypeStr, client, visited);
            } else {
                node = processRecipe(primaryIngredients, primaryResultCount, count, item, sourceDesc, primaryTypeStr, client, primaryResult, visited);
            }
            depth--;
            if (node != null && !alternativeTypes.isEmpty()) node.alternativeRecipeTypes = alternativeTypes;
            return node;
        }

        RecipeFallback fallback = findRecipeFromManager(mcItem);
        if (fallback != null) {
            depth++;
            RecipeTreeNode node;
            if (WILDCARD_ITEM_IDS.contains(itemIdStr)) {
                node = processWildcardRecipe(fallback.ingredients, fallback.resultCount, count, item, sourceDesc, fallback.type, client, visited);
            } else {
                node = processRecipe(fallback.ingredients, fallback.resultCount, count, item, sourceDesc, fallback.type, client, null, visited);
            }
            depth--;
            if (node != null) LitematListMod.LOGGER.info("[RawMaterialTreeAnalyzer] RecipeManager兜底成功: {} (类型: {})", itemIdStr, fallback.type);
            return node;
        }

        LitematListMod.LOGGER.info("[RawMaterialTreeAnalyzer] 无法溯源: {} (depth={})", itemIdStr, depth);
        return RecipeTreeNode.base(item.getIdAsString(), count);
    }

    private RecipeTreeNode tryTraceSmoothStoneSlab(String itemIdStr, int count, MinecraftClient client, Set<String> visited) {
        String preferred = PREFERRED_RECIPE_TYPES.get(itemIdStr);
        List<List<String>> orderedPriority = recipePriority;
        if (preferred != null) {
            orderedPriority = new ArrayList<>(recipePriority);
            for (int i = 0; i < orderedPriority.size(); i++) {
                if (orderedPriority.get(i).contains(preferred)) {
                    if (i > 0) { List<String> g = orderedPriority.remove(i); orderedPriority.add(0, g); }
                    break;
                }
            }
        }
        RecipeHelper.RecipeLookupResult primaryResult = null;
        List<Ingredient> primaryIngredients = null;
        int primaryResultCount = 0;
        String primaryType = null;
        List<String> alternativeTypes = new ArrayList<>();
        for (List<String> typeGroup : orderedPriority) {
            ItemStack shadow = new ItemStack(Registries.ITEM.get(Identifier.of(itemIdStr)));
            RecipeHelper.RecipeLookupResult lookup = RecipeHelper.lookupRecipe(shadow, typeGroup, client);
            if (lookup == null) continue;
            if (primaryResult == null) {
                primaryResult = lookup; primaryIngredients = lookup.ingredients();
                primaryResultCount = lookup.resultCount(); primaryType = typeGroup.get(0);
            } else {
                String tn = typeGroup.get(0);
                if (!alternativeTypes.contains(tn)) alternativeTypes.add(tn);
            }
        }
        if (primaryResult != null) {
            depth++;
            RecipeTreeNode node = processRecipe(primaryIngredients, primaryResultCount, count,
                    Registries.ITEM.get(Identifier.of(itemIdStr)).getRegistryEntry(), "平滑石半砖溯源", primaryType, client, primaryResult, visited);
            depth--;
            if (node != null && !alternativeTypes.isEmpty()) node.alternativeRecipeTypes = alternativeTypes;
            return node;
        }
        return null;
    }

    private RecipeTreeNode tryTraceChiseledQuartzBlock(String itemIdStr, int count, MinecraftClient client, Set<String> visited) {
        String preferred = PREFERRED_RECIPE_TYPES.get(itemIdStr);
        List<List<String>> orderedPriority = recipePriority;
        if (preferred != null) {
            orderedPriority = new ArrayList<>(recipePriority);
            for (int i = 0; i < orderedPriority.size(); i++) {
                if (orderedPriority.get(i).contains(preferred)) {
                    if (i > 0) { List<String> g = orderedPriority.remove(i); orderedPriority.add(0, g); }
                    break;
                }
            }
        }
        RecipeHelper.RecipeLookupResult primaryResult = null;
        List<Ingredient> primaryIngredients = null;
        int primaryResultCount = 0;
        String primaryType = null;
        List<String> alternativeTypes = new ArrayList<>();
        for (List<String> typeGroup : orderedPriority) {
            ItemStack shadow = new ItemStack(Registries.ITEM.get(Identifier.of(itemIdStr)));
            List<RecipeHelper.RecipeLookupResult> allResults = RecipeHelper.lookupAllRecipes(shadow, typeGroup, client);
            for (RecipeHelper.RecipeLookupResult lookup : allResults) {
                boolean usesQuartzBlock = false;
                for (Ingredient ing : lookup.ingredients()) {
                    if (RecipeHelper.ingredientContainsItem(ing, "minecraft:quartz_block", client)) { usesQuartzBlock = true; break; }
                }
                if (usesQuartzBlock) {
                    if (primaryResult == null) {
                        primaryResult = lookup; primaryIngredients = lookup.ingredients();
                        primaryResultCount = lookup.resultCount(); primaryType = typeGroup.get(0);
                    } else if (!alternativeTypes.contains(typeGroup.get(0))) alternativeTypes.add(typeGroup.get(0));
                }
            }
        }
        if (primaryResult != null) {
            depth++;
            RecipeTreeNode node = processRecipe(primaryIngredients, primaryResultCount, count,
                    Registries.ITEM.get(Identifier.of(itemIdStr)).getRegistryEntry(), "雕纹石英溯源", primaryType, client, primaryResult, visited);
            depth--;
            if (node != null && !alternativeTypes.isEmpty()) node.alternativeRecipeTypes = alternativeTypes;
            return node;
        }
        return null;
    }

    /**
     * 白色床/白色羊毛专用：优先选择「合成配方」而非「回染配方」。
     * 白床回染配方（白染料+任意彩色床→白床）与白羊毛回染配方（白染料+任意彩色羊毛→白羊毛）
     * 会造成循环分解（白床→黑床+白染料→…）；这里只接受不含床/染料的配方，
     * 即 羊毛x3+木板x3 与 线x4 的合成配方。
     */
    private RecipeTreeNode tryTraceWhiteDyeable(String itemIdStr, int count, RegistryEntry<Item> item,
            String sourceDesc, MinecraftClient client, Set<String> visited) {
        ItemStack shadow = new ItemStack(item.value());
        boolean isWhiteWool = "minecraft:white_wool".equals(itemIdStr);
        for (List<String> typeGroup : recipePriority) {
            List<RecipeHelper.RecipeLookupResult> allResults = RecipeHelper.lookupAllRecipes(shadow, typeGroup, client);
            for (RecipeHelper.RecipeLookupResult lookup : allResults) {
                boolean isRedye = false;
                for (Ingredient ing : lookup.ingredients()) {
                    if (RecipeHelper.ingredientContainsBed(ing, client)) { isRedye = true; break; }
                    if (isWhiteWool && RecipeHelper.ingredientContainsItem(ing, "minecraft:white_dye", client)) { isRedye = true; break; }
                }
                if (!isRedye) {
                    depth++;
                    RecipeTreeNode node = processWildcardRecipe(lookup.ingredients(), lookup.resultCount(), count, item,
                            sourceDesc, typeGroup.get(0), client, visited);
                    depth--;
                    return node;
                }
            }
        }
        return null;
    }

    private RecipeTreeNode tryTraceBedDyeing(String itemIdStr, int count, RegistryEntry<Item> item, String sourceDesc, MinecraftClient client, Set<String> visited) {
        ItemStack shadow = new ItemStack(item.value());
        for (List<String> typeGroup : recipePriority) {
            List<RecipeHelper.RecipeLookupResult> allResults = RecipeHelper.lookupAllRecipes(shadow, typeGroup, client);
            for (RecipeHelper.RecipeLookupResult lookup : allResults) {
                boolean hasBed = false;
                for (Ingredient ing : lookup.ingredients()) {
                    if (RecipeHelper.ingredientContainsBed(ing, client)) { hasBed = true; break; }
                }
                if (hasBed) {
                    depth++;
                    RecipeTreeNode node = processWildcardRecipe(lookup.ingredients(), lookup.resultCount(), count, item, sourceDesc, typeGroup.get(0), client, visited);
                    depth--;
                    return node;
                }
            }
        }
        return null;
    }

    private RecipeTreeNode processWildcardRecipe(List<Ingredient> ingredients, int resultCount, int total,
            RegistryEntry<Item> inputItem, String sourceDesc, String recipeType, MinecraftClient client, Set<String> visited) {
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
            redundancy.add(new MaterialDetailScreen.RawMaterialEntry(itemId, excess, List.of(desc)));
        }
        Map<RegistryEntry<Item>, Integer> dedup = new LinkedHashMap<>();
        for (Ingredient ing : ingredients) {
            List<ItemStack> displayStacks = RecipeHelper.getIngredientStacks(ing, client);
            if (displayStacks.isEmpty()) continue;
            RegistryEntry<Item> ingItem = displayStacks.size() > 1 ? pickBaseDyeableIngredient(displayStacks) : displayStacks.getFirst().getRegistryEntry();
            dedup.merge(ingItem, adjustedTotal, Integer::sum);
        }
        List<RecipeTreeNode> subMaterials = new ArrayList<>();
        for (var entry : dedup.entrySet()) {
            String childId = entry.getKey().getIdAsString();
            String parentId = inputItem.getIdAsString();
            // 只有溯源物品处于通配成品白名单中，其特定材料才允许标记为通配子材料
            boolean parentWildcard = WILDCARD_ITEM_IDS.contains(parentId);
            String contextReplaced = getContextWildcardReplacement(parentId, childId);
            if (!contextReplaced.equals(childId)) childId = contextReplaced;
            else { String replacedId = getWildcardReplacement(childId); if (!replacedId.equals(childId)) childId = replacedId; }
            if (WILDCARD_MATERIAL_IDS.contains(entry.getKey().getIdAsString())) {
                RegistryEntry<Item> traceEntry = entry.getKey();
                String originalId = entry.getKey().getIdAsString();
                if (!childId.equals(originalId)) { Item ri = Registries.ITEM.get(Identifier.of(childId)); traceEntry = ri.getRegistryEntry(); }
                boolean isChiseledBookshelfSlab = "minecraft:chiseled_bookshelf".equals(parentId) && childId.endsWith("_slab");
                RecipeTreeNode sub = traceItem(traceEntry, entry.getValue(), inputItem, sourceDesc, visited);
                if (sub != null) { sub.wildcardSubMaterial = parentWildcard && !isChiseledBookshelfSlab; subMaterials.add(sub); }
                else { RecipeTreeNode base = RecipeTreeNode.base(childId, entry.getValue()); base.wildcardSubMaterial = parentWildcard && !isChiseledBookshelfSlab; subMaterials.add(base); }
            } else {
                RegistryEntry<Item> traceEntry = entry.getKey();
                String originalId = entry.getKey().getIdAsString();
                if (!childId.equals(originalId)) { Item ri = Registries.ITEM.get(Identifier.of(childId)); if (ri != null) traceEntry = ri.getRegistryEntry(); }
                RecipeTreeNode sub = traceItem(traceEntry, entry.getValue(), inputItem, sourceDesc, visited);
                if (sub != null) subMaterials.add(sub);
            }
        }
        return new RecipeTreeNode(inputItem.getIdAsString(), total, RecipeHelper.recipeTypeToJson(recipeType), List.of(), subMaterials);
    }

    private RecipeTreeNode processRecipe(List<Ingredient> ingredients, int resultCount, int total,
            RegistryEntry<Item> inputItem, String sourceDesc, String recipeType,
            MinecraftClient client, RecipeHelper.RecipeLookupResult lookupResult, Set<String> visited) {
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
            redundancy.add(new MaterialDetailScreen.RawMaterialEntry(itemId, excess, List.of(desc)));
        }
        Map<RegistryEntry<Item>, Integer> dedup = new LinkedHashMap<>();
        for (Ingredient ing : ingredients) {
            List<ItemStack> displayStacks = RecipeHelper.getIngredientStacks(ing, client);
            if (displayStacks.isEmpty()) continue;
            RegistryEntry<Item> ingItem = displayStacks.getFirst().getRegistryEntry();
            dedup.merge(ingItem, adjustedTotal, Integer::sum);
        }
        // 安全移除：从dedup中移除与输入物品相同的原料，防止循环溯源
        dedup.entrySet().removeIf(e -> e.getKey() == inputItem);
        List<RecipeTreeNode> subMaterials = new ArrayList<>();
        for (var entry : dedup.entrySet()) {
            // processRecipe 仅用于非通配成品，子材料不应应用替换逻辑
            // 只有 processWildcardRecipe 才应处理通配替换
            RecipeTreeNode sub = traceItem(entry.getKey(), entry.getValue(), inputItem, sourceDesc, visited);
            if (sub != null) {
                subMaterials.add(sub);
            } else {
                RecipeTreeNode base = RecipeTreeNode.base(entry.getKey().getIdAsString(), entry.getValue());
                subMaterials.add(base);
            }
        }
        List<RecipeTreeNode.RecipeDetail> recipeDetails = extractRecipeDetails(lookupResult, recipeType, resultCount, client);
        return new RecipeTreeNode(inputItem.getIdAsString(), total, RecipeHelper.recipeTypeToJson(recipeType), recipeDetails, subMaterials);
    }

    private List<RecipeTreeNode.RecipeDetail> extractRecipeDetails(RecipeHelper.RecipeLookupResult lookupResult,
            String recipeType, int resultCount, MinecraftClient client) {
        List<RecipeTreeNode.RecipeDetail> details = new ArrayList<>();
        RecipeTreeNode.RecipeDetail detail = new RecipeTreeNode.RecipeDetail();
        detail.resultCount = resultCount;
        switch (recipeType) {
            case "crafting_shaped", "SHAPED" -> detail.type = "shaped";
            case "crafting_shapeless", "SHAPELESS" -> detail.type = "shapeless";
            case "smelting", "FURNACE" -> { detail.type = "smelting"; tryExtractFurnaceDetails(lookupResult, detail, client); }
            case "smithing", "smithing_transform", "SMITHING" -> { detail.type = "smithing"; tryExtractSmithingDetails(lookupResult, detail, client); }
            case "stonecutter", "STONECUTTER" -> { detail.type = "stonecutting"; tryExtractStonecutterDetails(lookupResult, detail, client); }
        }
        details.add(detail);
        return details;
    }

    private void tryExtractFurnaceDetails(RecipeHelper.RecipeLookupResult lookupResult,
            RecipeTreeNode.RecipeDetail detail, MinecraftClient client) {
        try {
            var recipeManager = client.world.getRecipeManager();
            List<ItemStack> resultStacks = lookupResult.resultStacks();
            if (resultStacks.isEmpty()) return;
            List<RecipeHelper.RecipeLookupResult> allResults = RecipeHelper.lookupAllRecipes(
                    resultStacks.getFirst(), List.of("FURNACE"), client);
        } catch (Exception ignored) {}
    }

    private void tryExtractSmithingDetails(RecipeHelper.RecipeLookupResult lookupResult,
            RecipeTreeNode.RecipeDetail detail, MinecraftClient client) {
        try {} catch (Exception ignored) {}
    }

    private void tryExtractStonecutterDetails(RecipeHelper.RecipeLookupResult lookupResult,
            RecipeTreeNode.RecipeDetail detail, MinecraftClient client) {
        try {} catch (Exception ignored) {}
    }

    private boolean checkLoop(List<Ingredient> ingredients, RegistryEntry<Item> inputItem,
            RegistryEntry<Item> prevItem, MinecraftClient client) {
        for (Ingredient ing : ingredients) {
            List<ItemStack> displayStacks = RecipeHelper.getIngredientStacks(ing, client);
            if (displayStacks.isEmpty()) continue;
            RegistryEntry<Item> ingItem = displayStacks.getFirst().getRegistryEntry();
            if (ingItem == inputItem || ingItem == prevItem) return true;
        }
        return false;
    }

    private record RecipeFallback(List<Ingredient> ingredients, int resultCount, String type) {}

    @SuppressWarnings("unchecked")
    private RecipeFallback findRecipeFromManager(Item targetItem) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null) return null;
            var recipeManager = client.world.getRecipeManager();
            var registryManager = client.world.getRegistryManager();
            List<Object> recipeEntries = new ArrayList<>();
            try {
                var valuesMethod = recipeManager.getClass().getMethod("values");
                Collection<?> values = (Collection<?>) valuesMethod.invoke(recipeManager);
                for (Object entry : values) recipeEntries.add(entry);
            } catch (NoSuchMethodException e) {
                var keysMethod = recipeManager.getClass().getMethod("getIds");
                var keys = (Collection<Identifier>) keysMethod.invoke(recipeManager);
                var getMethod = recipeManager.getClass().getMethod("get", Identifier.class);
                for (Identifier id : keys) {
                    var opt = (Optional<?>) getMethod.invoke(recipeManager, id);
                    opt.ifPresent(o -> recipeEntries.add(o));
                }
            }
            for (Object entryObj : recipeEntries) {
                Object recipe = null;
                try { var valueMethod = entryObj.getClass().getMethod("value"); recipe = valueMethod.invoke(entryObj); } catch (Exception ignored) { continue; }
                if (recipe == null) continue;
                ItemStack result = getRecipeResult(recipe, registryManager);
                if (result == null || result.isEmpty() || result.getItem() != targetItem) continue;
                List<Ingredient> ingredients = getIngredientsSafe((net.minecraft.recipe.Recipe<?>) recipe);
                if (ingredients.isEmpty()) continue;
                String type = getRecipeTypeSafe((net.minecraft.recipe.Recipe<?>) recipe);
                if (type == null) continue;
                return new RecipeFallback(ingredients, result.getCount(), type);
            }
        } catch (Exception e) {
            LitematListMod.LOGGER.warn("RecipeManager 兜底查找失败: {}", e.getMessage());
        }
        return null;
    }

    private ItemStack getRecipeResult(Object recipe, Object registryManager) {
        try { var m = recipe.getClass().getMethod("getResult", net.minecraft.registry.RegistryWrapper.WrapperLookup.class); return (ItemStack) m.invoke(recipe, registryManager); } catch (Exception ignored) {}
        try { var m = recipe.getClass().getMethod("getResult"); return (ItemStack) m.invoke(recipe); } catch (Exception ignored) {}
        try { var m = recipe.getClass().getMethod("getOutput", net.minecraft.registry.RegistryWrapper.WrapperLookup.class); return (ItemStack) m.invoke(recipe, registryManager); } catch (Exception ignored) {}
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Ingredient> getIngredientsSafe(net.minecraft.recipe.Recipe<?> recipe) {
        try { var m = recipe.getClass().getMethod("getIngredients"); return (List<Ingredient>) m.invoke(recipe); } catch (Exception ignored) {}
        try { var f = findFieldInHierarchy(recipe.getClass(), "ingredient"); if (f != null) { f.setAccessible(true); return List.of((Ingredient) f.get(recipe)); } } catch (Exception ignored) {}
        try { var f = findFieldInHierarchy(recipe.getClass(), "input"); if (f != null) { f.setAccessible(true); return List.of((Ingredient) f.get(recipe)); } } catch (Exception ignored) {}
        try {
            var tF = findFieldInHierarchy(recipe.getClass(), "template");
            var bF = findFieldInHierarchy(recipe.getClass(), "base");
            var aF = findFieldInHierarchy(recipe.getClass(), "addition");
            if (tF != null && bF != null && aF != null) {
                tF.setAccessible(true); bF.setAccessible(true); aF.setAccessible(true);
                return List.of((Ingredient) tF.get(recipe), (Ingredient) bF.get(recipe), (Ingredient) aF.get(recipe));
            }
        } catch (Exception ignored) {}
        return List.of();
    }

    private java.lang.reflect.Field findFieldInHierarchy(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try { return current.getDeclaredField(fieldName); } catch (NoSuchFieldException e) { current = current.getSuperclass(); }
        }
        return null;
    }

    private String getRecipeTypeSafe(net.minecraft.recipe.Recipe<?> recipe) {
        String name = recipe.getClass().getSimpleName();
        if (name.contains("Shaped")) return "SHAPED";
        if (name.contains("Shapeless")) return "SHAPELESS";
        if (name.contains("Smelting") || name.contains("Furnace") || name.contains("Blasting") || name.contains("Smoking") || name.contains("Campfire")) return "FURNACE";
        if (name.contains("Smithing")) return "SMITHING";
        if (name.contains("Stonecutting")) return "STONECUTTER";
        if (isCookingRecipe(recipe)) return "FURNACE";
        return null;
    }

    private boolean isCookingRecipe(net.minecraft.recipe.Recipe<?> recipe) {
        Class<?> current = recipe.getClass().getSuperclass();
        while (current != null) {
            if (current.getSimpleName().contains("Cooking") || current.getSimpleName().contains("Furnace")) return true;
            current = current.getSuperclass();
        }
        return false;
    }

    private static RegistryEntry<Item> pickBaseDyeableIngredient(List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            String id = Registries.ITEM.getId(stack.getItem()).toString();
            if (id.equals("minecraft:white_wool") || id.equals("minecraft:white_bed") || id.equals("minecraft:oak_leaves")) return stack.getRegistryEntry();
        }
        for (ItemStack stack : stacks) {
            String id = Registries.ITEM.getId(stack.getItem()).toString();
            if (id.equals("minecraft:oak_planks") || id.equals("minecraft:oak_slab")) return stack.getRegistryEntry();
        }
        // 圆石类：优先选择圆石作为默认基底（熔炉/发射器/投掷器/侦测器配方）
        for (ItemStack stack : stacks) {
            String id = Registries.ITEM.getId(stack.getItem()).toString();
            if (id.equals("minecraft:cobblestone")) return stack.getRegistryEntry();
        }
        return stacks.getFirst().getRegistryEntry();
    }
}

