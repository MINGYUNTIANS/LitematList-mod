//#if MC == 12101 || MC == 12104
//$$package com.litematlist;
//$$
//$$import net.minecraft.client.MinecraftClient;
//$$import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
//$$import net.minecraft.item.ItemStack;
//#if MC == 12104
//$$import net.minecraft.recipe.NetworkRecipeId;
//#endif
//#if MC == 12104
//$$import net.minecraft.recipe.RecipeDisplayEntry;
//#endif
//#if MC == 12104
//$$import net.minecraft.recipe.display.FurnaceRecipeDisplay;
//#endif
//#if MC == 12104
//$$import net.minecraft.recipe.display.RecipeDisplay;
//#endif
//#if MC == 12104
//$$import net.minecraft.recipe.display.ShapedCraftingRecipeDisplay;
//#endif
//#if MC == 12104
//$$import net.minecraft.recipe.display.ShapelessCraftingRecipeDisplay;
//#endif
//#if MC == 12104
//$$import net.minecraft.recipe.display.SlotDisplay;
//#endif
//#if MC == 12104
//$$import net.minecraft.recipe.display.SlotDisplayContexts;
//#endif
//#if MC == 12104
//$$import net.minecraft.recipe.display.SmithingRecipeDisplay;
//#endif
//#if MC == 12104
//$$import net.minecraft.recipe.display.StonecutterRecipeDisplay;
//#endif
//#if MC == 12104
//$$import net.minecraft.resource.featuretoggle.FeatureSet;
//#endif
//#if MC == 12104
//$$import net.minecraft.util.context.ContextParameterMap;
//#endif
//#if MC == 12101
//$$import net.minecraft.recipe.AbstractCookingRecipe;
//#endif
//#if MC == 12101
//$$import net.minecraft.recipe.CuttingRecipe;
//#endif
//#if MC == 12101
//$$import net.minecraft.recipe.Ingredient;
//#endif
//#if MC == 12101
//$$import net.minecraft.recipe.Recipe;
//#endif
//#if MC == 12101
//$$import net.minecraft.recipe.ShapedRecipe;
//#endif
//#if MC == 12101
//$$import net.minecraft.recipe.ShapelessRecipe;
//#endif
//#if MC == 12101
//$$import net.minecraft.recipe.SmithingRecipe;
//#endif
//#if MC == 12101
//$$import net.minecraft.recipe.SmithingTrimRecipe;
//#endif
//#if MC == 12101
//$$import net.minecraft.registry.RegistryWrapper;
//#endif
//#if MC == 12101
//$$import net.minecraft.util.Identifier;
//#endif
//$$import net.minecraft.world.World;
//$$import org.apache.commons.lang3.tuple.Pair;
//$$
//$$import java.util.ArrayList;
//#if MC == 12101
//$$import java.util.Arrays;
//#endif
//$$import java.util.LinkedHashSet;
//$$import java.util.List;
//#if MC == 12101
//$$import java.util.Optional;
//#endif
//$$import java.util.Set;
//$$
//#if MC == 12101
//$$/**
//$$ * 1.21.1 版 RecipeBookUtils 兼容类。
//$$ * 1.21.1 没有 1.21.2+ 的配方显示（RecipeDisplay）体系，故此处基于旧版
//$$ * ClientRecipeBook#getOrderedResults() + 原版配方对象实现（参考旧版 RecipeHelper 的做法），
//$$ * 对外仍提供与 malilib 0.25.7 / 高版本一致的 Type、getMap、getDisplayEntryFromRecipeBook 接口。
//$$ */
//#elseif MC == 12104
//$$/**
//$$ * 1.21.4 版 RecipeBookUtils 兼容类。
//$$ * 镜像 malilib 0.25.7 的 fi.dy.masa.malilib.util.game.RecipeBookUtils 中本项目用到的 API 面：
//$$ * 1.21.4 依赖的 malilib 0.23.5 不含该类，故此处基于
//$$ * ClientRecipeBook#getOrderedResults() 实现（参考旧版 RecipeHelper 的做法）。
//$$ */
//#endif
//$$public class RecipeBookUtils {
//$$
//#if MC == 12101
//$$    /** 配方类型（与 malilib 0.25.7 / 高版本的 Type 枚举一致） */
//#elseif MC == 12104
//$$    /** 配方类型（与 malilib 0.25.7 的 Type 枚举一致） */
//#endif
//$$    public enum Type {
//$$        FURNACE,
//$$        SHAPED,
//$$        SHAPELESS,
//$$        SMITHING,
//$$        STONECUTTER,
//$$        UNKNOWN;
//$$
//#if MC == 12104
//$$        public static Type fromRecipeDisplay(RecipeDisplay type) {
//#endif
//#if MC == 12104
//$$            return switch (type) {
//#endif
//#if MC == 12104
//$$                case FurnaceRecipeDisplay ignored -> FURNACE;
//#endif
//#if MC == 12104
//$$                case ShapelessCraftingRecipeDisplay ignored -> SHAPELESS;
//#endif
//#if MC == 12104
//$$                case ShapedCraftingRecipeDisplay ignored -> SHAPED;
//#endif
//#if MC == 12104
//$$                case SmithingRecipeDisplay ignored -> SMITHING;
//#endif
//#if MC == 12104
//$$                case StonecutterRecipeDisplay ignored -> STONECUTTER;
//#endif
//#if MC == 12104
//$$                case null, default -> UNKNOWN;
//#endif
//#if MC == 12104
//$$            };
//#endif
//#if MC == 12101
//$$        /** 按原版配方对象判定类型（对应高版本的 Type.fromRecipeDisplay） */
//#endif
//#if MC == 12101
//$$        public static Type fromRecipe(Recipe<?> recipe) {
//#endif
//#if MC == 12101
//$$            if (recipe instanceof AbstractCookingRecipe) return FURNACE;
//#endif
//#if MC == 12101
//$$            if (recipe instanceof ShapelessRecipe) return SHAPELESS;
//#endif
//#if MC == 12101
//$$            if (recipe instanceof ShapedRecipe) return SHAPED;
//#endif
//#if MC == 12101
//$$            if (recipe instanceof SmithingRecipe) return SMITHING;
//#endif
//#if MC == 12101
//$$            if (recipe instanceof CuttingRecipe) return STONECUTTER;
//#endif
//#if MC == 12101
//$$            return UNKNOWN;
//#endif
//$$        }
//$$    }
//$$
//#if MC == 12101
//$$    private static RegistryWrapper.WrapperLookup map;
//#elseif MC == 12104
//$$    private static ContextParameterMap map;
//#endif
//$$    private static World cachedWorld;
//$$    private static final int refreshTime = 300;
//$$    private static long lastRefresh = -1L;
//$$
//#if MC == 12101
//$$    /**
//$$     * 获取缓存的注册表查询（world 为 null 时返回 null）。
//$$     * 与 malilib 0.25.7 一致的 300 秒缓存策略，并额外按 world 实例判断，
//$$     * 避免切换存档后短时间内读取到旧世界的注册表。
//$$     */
//#elseif MC == 12104
//$$    /**
//$$     * 获取缓存的参数映射（world 为 null 时返回 null）。
//$$     * 与 malilib 0.25.7 一致的 300 秒缓存策略，并额外按 world 实例判断，
//$$     * 避免切换存档后短时间内地读取到旧世界的参数。
//$$     */
//#endif
//#if MC == 12101
//$$    public static RegistryWrapper.WrapperLookup getMap(MinecraftClient mc) {
//#elseif MC == 12104
//$$    public static ContextParameterMap getMap(MinecraftClient mc) {
//#endif
//$$        if (mc.world == null) return null;
//$$
//$$        if (map == null || cachedWorld != mc.world ||
//$$                (System.currentTimeMillis() - lastRefresh) > (refreshTime * 1000L)) {
//#if MC == 12101
//$$            map = mc.world.getRegistryManager();
//#elseif MC == 12104
//$$            map = SlotDisplayContexts.createParameters(mc.world);
//#endif
//$$            cachedWorld = mc.world;
//$$            lastRefresh = System.currentTimeMillis();
//$$        }
//$$
//$$        return map;
//$$    }
//$$
//#if MC == 12104
//$$    /**
//$$     * 查询配方簿中成品匹配、且类型在 types 内的配方显示条目（最多 3 条）。
//$$     * malilib 版经其内部 mixin 读取配方映射；1.21.4 版改为遍历
//$$     * ClientRecipeBook#getOrderedResults() 读取，其余过滤逻辑与 malilib 0.25.7 保持一致。
//$$     */
//#endif
//#if MC == 12104
//$$    public static List<Pair<NetworkRecipeId, RecipeDisplayEntry>> getDisplayEntryFromRecipeBook(ItemStack result, List<Type> types) {
//#endif
//#if MC == 12101
//$$    /** 原料的候选物品列表（对应高版本的 SlotDisplay#getStacks） */
//#endif
//#if MC == 12101
//$$    public static List<ItemStack> getIngredientStacks(Ingredient ing) {
//#endif
//#if MC == 12101
//$$        return Arrays.asList(ing.getMatchingStacks());
//#endif
//#if MC == 12101
//$$    }
//#endif
//#if MC == 12101
//$$
//#endif
//#if MC == 12101
//$$    /** 配方原料（对应高版本 RecipeDisplayEntry#craftingRequirements） */
//#endif
//#if MC == 12101
//$$    public static Optional<List<Ingredient>> getCraftingRequirements(Recipe<?> recipe) {
//#endif
//#if MC == 12101
//$$        return RecipeDisplayEntry.getCraftingRequirements(recipe);
//#endif
//#if MC == 12101
//$$    }
//#endif
//#if MC == 12101
//$$
//#endif
//#if MC == 12101
//$$    /**
//$$     * 查询配方簿中成品匹配、且类型在 types 内的配方条目（最多 3 条）。
//$$     * 高版本经 RecipeDisplayEntry#craftingRequirements 过滤；1.21.1 版改为遍历
//$$     * ClientRecipeBook#getOrderedResults() 读取原版配方（与旧版 RecipeHelper 的做法一致）。
//$$     */
//#endif
//#if MC == 12101
//$$    public static List<Pair<Identifier, RecipeDisplayEntry>> getDisplayEntryFromRecipeBook(ItemStack result, List<Type> types) {
//#endif
//$$        MinecraftClient mc = MinecraftClient.getInstance();
//$$
//$$        if (mc.world == null || mc.player == null) {
//$$            return new ArrayList<>();
//$$        }
//$$
//#if MC == 12101
//$$        RegistryWrapper.WrapperLookup lookup = getMap(mc);
//#elseif MC == 12104
//$$        ContextParameterMap map = getMap(mc);
//#endif
//#if MC == 12101
//$$        if (lookup == null) return new ArrayList<>();
//#elseif MC == 12104
//$$        if (map == null) return new ArrayList<>();
//#endif
//$$
//#if MC == 12104
//$$        FeatureSet features = mc.world.getEnabledFeatures();
//#endif
//#if MC == 12104
//$$        List<Pair<NetworkRecipeId, RecipeDisplayEntry>> list = new ArrayList<>();
//#endif
//#if MC == 12104
//$$        // 同一 NetworkRecipeId 可能出现在多个结果集合中，去重
//#endif
//#if MC == 12104
//$$        Set<NetworkRecipeId> seen = new LinkedHashSet<>();
//#endif
//#if MC == 12101
//$$        List<Pair<Identifier, RecipeDisplayEntry>> list = new ArrayList<>();
//#endif
//#if MC == 12101
//$$        // 同一配方 ID 可能出现在多个结果集合中，去重
//#endif
//#if MC == 12101
//$$        Set<Identifier> seen = new LinkedHashSet<>();
//#endif
//$$
//$$        for (RecipeResultCollection collection : mc.player.getRecipeBook().getOrderedResults()) {
//#if MC == 12101
//$$            for (net.minecraft.recipe.RecipeEntry<?> entry : collection.getAllRecipes()) {
//#elseif MC == 12104
//$$            for (RecipeDisplayEntry entry : collection.getAllRecipes()) {
//#endif
//$$                if (!seen.add(entry.id())) continue;
//$$
//#if MC == 12101
//$$                Recipe<?> recipe = entry.value();
//#elseif MC == 12104
//$$                Type type = Type.fromRecipeDisplay(entry.display());
//#endif
//$$
//#if MC == 12104
//$$                // 锻造纹饰配方的结果槽会在此崩溃，跳过（与 malilib 0.25.7 一致）
//#endif
//#if MC == 12104
//$$                if (entry.craftingRequirements().isPresent() &&
//#endif
//#if MC == 12104
//$$                        types.contains(type) &&
//#endif
//#if MC == 12104
//$$                        entry.display().isEnabled(features) &&
//#endif
//#if MC == 12104
//$$                        !(entry.display().result() instanceof SlotDisplay.SmithingTrimSlotDisplay)) {
//#endif
//#if MC == 12104
//$$                    ItemStack resultSlot = entry.display().result().getFirst(map);
//#endif
//#if MC == 12101
//$$                // 锻造纹饰配方在显示体系中会导致崩溃，旧系统同样跳过（与 malilib 0.25.7 一致）
//#endif
//#if MC == 12101
//$$                if (recipe instanceof SmithingTrimRecipe) continue;
//#endif
//$$
//#if MC == 12104
//$$                    if (resultSlot.isEmpty()) {
//#endif
//#if MC == 12104
//$$                        continue;
//#endif
//#if MC == 12104
//$$                    }
//#endif
//#if MC == 12101
//$$                if (!types.contains(Type.fromRecipe(recipe))) continue;
//#endif
//$$
//#if MC == 12104
//$$                    if (ItemStack.areItemsAndComponentsEqual(result, resultSlot)) {
//#endif
//#if MC == 12104
//$$                        list.add(Pair.of(entry.id(), entry));
//#endif
//#if MC == 12101
//$$                RecipeDisplayEntry wrapper = new RecipeDisplayEntry(entry.id(), recipe);
//#endif
//#if MC == 12101
//$$                Optional<List<Ingredient>> requirements = wrapper.craftingRequirements();
//#endif
//#if MC == 12101
//$$                if (requirements.isEmpty()) continue;
//#endif
//$$
//#if MC == 12104
//$$                        // 最多返回 3 条
//#endif
//#if MC == 12104
//$$                        if (list.size() > 2) {
//#endif
//#if MC == 12104
//$$                            return list;
//#endif
//#if MC == 12104
//$$                        }
//#endif
//#if MC == 12101
//$$                List<ItemStack> resultStacks = wrapper.getStacks(lookup);
//#endif
//#if MC == 12101
//$$                if (resultStacks.isEmpty()) continue;
//#endif
//#if MC == 12101
//$$                ItemStack resultSlot = resultStacks.getFirst();
//#endif
//#if MC == 12101
//$$
//#endif
//#if MC == 12101
//$$                if (resultSlot.isEmpty()) continue;
//#endif
//#if MC == 12101
//$$
//#endif
//#if MC == 12101
//$$                if (ItemStack.areItemsAndComponentsEqual(result, resultSlot)) {
//#endif
//#if MC == 12101
//$$                    list.add(Pair.of(entry.id(), wrapper));
//#endif
//#if MC == 12101
//$$
//#endif
//#if MC == 12101
//$$                    // 最多返回 3 条
//#endif
//#if MC == 12101
//$$                    if (list.size() > 2) {
//#endif
//#if MC == 12101
//$$                        return list;
//#endif
//$$                    }
//$$                }
//$$            }
//$$        }
//$$
//$$        return list;
//$$    }
//$$}
//#endif