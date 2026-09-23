package com.litematlist;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 配方显示条目包装器 —— 替代 malilib 的 RecipeDisplayEntry。
 * 包装 Minecraft 原版 RecipeEntry，提供与高版本 RecipeDisplayEntry 兼容的 API。
 */
public class RecipeEntry<T extends Recipe<?>> {
    private final Identifier id;
    private final Recipe<?> recipe;
    private final RecipeBookCategory category;

    public RecipeEntry(Identifier id, Recipe<?> recipe, RecipeBookCategory category) {
        this.id = id;
        this.recipe = recipe;
        this.category = category;
    }

    public Identifier id() {
        return id;
    }

    public Recipe<?> recipe() {
        return recipe;
    }

    public RecipeBookCategory category() {
        return category;
    }

    public Optional<List<Ingredient>> craftingRequirements() {
        try {
            return Optional.of(recipe.getIngredients());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<ItemStack> getStacks(RegistryWrapper.WrapperLookup lookup) {
        try {
            var method = recipe.getClass().getMethod("getResult", RegistryWrapper.WrapperLookup.class);
            ItemStack result = (ItemStack) method.invoke(recipe, lookup);
            return List.of(result);
        } catch (Exception e) {
            try {
                ItemStack result = recipe.getResult(lookup);
                return List.of(result);
            } catch (Exception e2) {
                return Collections.emptyList();
            }
        }
    }

    public Recipe<?> display() {
        return recipe;
    }
}