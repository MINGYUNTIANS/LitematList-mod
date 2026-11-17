//#if MC == 12101
//$$package com.litematlist;
//$$
//$$import net.minecraft.item.ItemStack;
//$$import net.minecraft.recipe.Ingredient;
//$$import net.minecraft.recipe.Recipe;
//$$import net.minecraft.recipe.SmithingTransformRecipe;
//$$import net.minecraft.recipe.SmithingTrimRecipe;
//$$import net.minecraft.registry.RegistryWrapper;
//$$import net.minecraft.util.Identifier;
//$$
//$$import java.lang.reflect.Field;
//$$import java.util.ArrayList;
//$$import java.util.Collections;
//$$import java.util.List;
//$$import java.util.Optional;
//$$
//$$/**
//$$ * 配方显示条目包装器（1.21.1）——替代 1.21.2+ 的 RecipeDisplayEntry，
//$$ * 镜像其在本项目中用到的 API 面（id / craftingRequirements / getStacks / display），
//$$ * 内部包装 Minecraft 原版配方对象。
//$$ */
//$$public class RecipeDisplayEntry {
//$$
//$$    private final Identifier id;
//$$    private final Recipe<?> recipe;
//$$
//$$    public RecipeDisplayEntry(Identifier id, Recipe<?> recipe) {
//$$        this.id = id;
//$$        this.recipe = recipe;
//$$    }
//$$
//$$    public Identifier id() {
//$$        return id;
//$$    }
//$$
//$$    public Recipe<?> recipe() {
//$$        return recipe;
//$$    }
//$$
//$$    public Optional<List<Ingredient>> craftingRequirements() {
//$$        return getCraftingRequirements(recipe);
//$$    }
//$$
//$$    /**
//$$     * 配方原料；与高版本一致：原料不可用的配方返回空。
//$$     * 锻造台扩展配方在 1.21.1 没有公开访问器，通过反射读取
//$$     * template/base/addition 字段（对应高版本 craftingRequirements 的三元素列表）。
//$$     */
//$$    public static Optional<List<Ingredient>> getCraftingRequirements(Recipe<?> recipe) {
//$$        if (recipe == null) return Optional.empty();
//$$
//$$        if (recipe instanceof SmithingTransformRecipe) {
//$$            List<Ingredient> ingredients = readSmithingIngredients(recipe);
//$$            return ingredients == null || ingredients.isEmpty() ? Optional.empty() : Optional.of(ingredients);
//$$        }
//$$
//$$        // 纹饰配方的原料无法完整表达（需要已有的盔甲与纹饰图案），此处不提供
//$$        if (recipe instanceof SmithingTrimRecipe) return Optional.empty();
//$$
//$$        try {
//$$            List<Ingredient> ingredients = recipe.getIngredients();
//$$            if (ingredients == null || ingredients.isEmpty()) return Optional.empty();
//$$            return Optional.of(ingredients);
//$$        } catch (Exception e) {
//$$            return Optional.empty();
//$$        }
//$$    }
//$$
//$$    /** 反射读取锻造台扩展配方的 template/base/addition 字段 */
//$$    private static List<Ingredient> readSmithingIngredients(Recipe<?> recipe) {
//$$        try {
//$$            List<Ingredient> list = new ArrayList<>(3);
//$$            for (String name : List.of("template", "base", "addition")) {
//$$                Field field = findFieldInHierarchy(recipe.getClass(), name);
//$$                if (field == null) return Collections.emptyList();
//$$                field.setAccessible(true);
//$$                list.add((Ingredient) field.get(recipe));
//$$            }
//$$            return list;
//$$        } catch (Exception e) {
//$$            return Collections.emptyList();
//$$        }
//$$    }
//$$
//$$    private static Field findFieldInHierarchy(Class<?> clazz, String name) {
//$$        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
//$$            try {
//$$                return c.getDeclaredField(name);
//$$            } catch (NoSuchFieldException ignored) {
//$$                // 继续向父类查找
//$$            }
//$$        }
//$$        return null;
//$$    }
//$$
//$$    /** 配方产物（首个元素为结果堆叠，对应高版本 RecipeDisplayEntry#getStacks） */
//$$    public List<ItemStack> getStacks(RegistryWrapper.WrapperLookup lookup) {
//$$        try {
//$$            return List.of(recipe.getResult(lookup));
//$$        } catch (Exception e) {
//$$            return Collections.emptyList();
//$$        }
//$$    }
//$$
//$$    /** 显示信息载体：1.21.1 直接返回原版配方对象（高版本为 RecipeDisplay） */
//$$    public Recipe<?> display() {
//$$        return recipe;
//$$    }
//$$}
//#endif