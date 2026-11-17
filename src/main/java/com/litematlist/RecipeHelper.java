//#if MC == 11701 || MC == 11904 || MC == 12001
//$$package com.litematlist;
//$$
//$$import net.minecraft.client.MinecraftClient;
//$$import net.minecraft.item.ItemStack;
//$$import net.minecraft.recipe.Ingredient;
//$$import net.minecraft.recipe.Recipe;
//$$import net.minecraft.recipe.RecipeManager;
//$$import net.minecraft.recipe.RecipeType;
//$$
//$$import java.util.ArrayList;
//$$import java.util.List;
//#if MC == 11701
//$$import net.minecraft.util.registry.Registry;
//#endif
//$$
//$$/**
//$$ * 配方查询辅助类，抽象不同 Minecraft 版本间的配方 API 差异。
//$$ * 1.21.1 使用 RecipeManager 直接查询。
//$$ */
//$$public class RecipeHelper {
//$$
//$$    public record RecipeLookupResult(List<Ingredient> ingredients, List<ItemStack> resultStacks, int resultCount) {}
//$$
//$$    public static RecipeLookupResult lookupRecipe(ItemStack shadow, List<String> typeGroup, MinecraftClient client) {
//$$        RecipeManager recipeManager = client.world.getRecipeManager();
//$$        for (String typeStr : typeGroup) {
//$$            RecipeType<?> recipeType = getRecipeType(typeStr);
//$$            // 1.20.1：listAllOfType 直接返回 List<Recipe<?>>
//$$            List<Recipe<?>> recipes = recipeManager.listAllOfType((RecipeType)recipeType);
//$$            for (Recipe<?> recipe : recipes) {
//#if MC == 11701
//$$                ItemStack result = recipe.getOutput();
//#elseif MC == 11904 || MC == 12001
//$$                ItemStack result = recipe.getOutput(client.world.getRegistryManager());
//#endif
//$$                if (ItemStack.areItemsEqual(result, shadow)) {
//$$                    List<Ingredient> ingredients = recipe.getIngredients();
//$$                    return new RecipeLookupResult(ingredients, List.of(result), result.getCount());
//$$                }
//$$            }
//$$        }
//$$        return null;
//$$    }
//$$
//$$    public static List<RecipeLookupResult> lookupAllRecipes(ItemStack shadow, List<String> typeGroup, MinecraftClient client) {
//$$        RecipeManager recipeManager = client.world.getRecipeManager();
//$$        List<RecipeLookupResult> results = new ArrayList<>();
//$$        for (String typeStr : typeGroup) {
//$$            RecipeType<?> recipeType = getRecipeType(typeStr);
//$$            List<Recipe<?>> recipes = recipeManager.listAllOfType((RecipeType)recipeType);
//$$            for (Recipe<?> recipe : recipes) {
//#if MC == 11701
//$$                ItemStack result = recipe.getOutput();
//#elseif MC == 11904 || MC == 12001
//$$                ItemStack result = recipe.getOutput(client.world.getRegistryManager());
//#endif
//$$                if (ItemStack.areItemsEqual(result, shadow)) {
//$$                    List<Ingredient> ingredients = recipe.getIngredients();
//$$                    results.add(new RecipeLookupResult(ingredients, List.of(result), result.getCount()));
//$$                }
//$$            }
//$$        }
//$$        return results;
//$$    }
//$$
//$$    public static List<ItemStack> getIngredientStacks(Ingredient ing, MinecraftClient client) {
//$$        return List.of(ing.getMatchingStacks());
//$$    }
//$$
//$$    public static boolean ingredientContainsItem(Ingredient ing, String itemId, MinecraftClient client) {
//$$        for (ItemStack stack : ing.getMatchingStacks()) {
//#if MC == 11701
//$$            if (net.minecraft.util.registry.Registry.ITEM.getId(stack.getItem()).toString().equals(itemId)) {
//#elseif MC == 11904 || MC == 12001
//$$            if (net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).toString().equals(itemId)) {
//#endif
//$$                return true;
//$$            }
//$$        }
//$$        return false;
//$$    }
//$$
//$$    public static boolean ingredientContainsBed(Ingredient ing, MinecraftClient client) {
//$$        for (ItemStack stack : ing.getMatchingStacks()) {
//#if MC == 11701
//$$            if (net.minecraft.util.registry.Registry.ITEM.getId(stack.getItem()).toString().endsWith("_bed")) {
//#elseif MC == 11904 || MC == 12001
//$$            if (net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).toString().endsWith("_bed")) {
//#endif
//$$                return true;
//$$            }
//$$        }
//$$        return false;
//$$    }
//$$
//$$    private static RecipeType<?> getRecipeType(String typeStr) {
//$$        return switch (typeStr) {
//$$            // 配置字符串（RawMaterialConfig 的 mappedTypes）
//$$            case "stonecutter" -> RecipeType.STONECUTTING;
//$$            case "crafting_shaped", "crafting_shapeless" -> RecipeType.CRAFTING;
//$$            case "smelting" -> RecipeType.SMELTING;
//$$            case "smithing", "smithing_transform" -> RecipeType.SMITHING;
//$$            // 旧版内部字符串（兜底）
//$$            case "SHAPED", "SHAPELESS" -> RecipeType.CRAFTING;
//$$            case "STONECUTTER" -> RecipeType.STONECUTTING;
//$$            case "FURNACE" -> RecipeType.SMELTING;
//$$            case "SMITHING" -> RecipeType.SMITHING;
//$$            default -> RecipeType.CRAFTING;
//$$        };
//$$    }
//$$
//$$    public static String recipeTypeToJson(String typeStr) {
//$$        return switch (typeStr) {
//$$            // 配置字符串（RawMaterialConfig 的 mappedTypes）
//$$            case "stonecutter" -> "stonecutting";
//$$            case "crafting_shaped" -> "crafting_shaped";
//$$            case "crafting_shapeless" -> "crafting_shapeless";
//$$            case "smelting" -> "smelting";
//$$            case "smithing", "smithing_transform" -> "smithing";
//$$            // 旧版内部字符串（兜底）
//$$            case "SHAPED" -> "crafting_shaped";
//$$            case "SHAPELESS" -> "crafting_shapeless";
//$$            case "FURNACE" -> "smelting";
//$$            case "SMITHING" -> "smithing";
//$$            case "STONECUTTER" -> "stonecutting";
//$$            default -> "base";
//$$        };
//$$    }
//$$
//$$    public static String jsonToRecipeType(String jsonType) {
//$$        return switch (jsonType) {
//$$            case "crafting_shaped" -> "SHAPED";
//$$            case "crafting_shapeless" -> "SHAPELESS";
//$$            case "smelting" -> "FURNACE";
//$$            case "smithing" -> "SMITHING";
//$$            case "stonecutting" -> "STONECUTTER";
//$$            default -> null;
//$$        };
//$$    }
//$$}
//#endif