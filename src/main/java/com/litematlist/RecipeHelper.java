package com.litematlist;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.recipe.display.FurnaceRecipeDisplay;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.recipe.display.ShapedCraftingRecipeDisplay;
import net.minecraft.recipe.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.recipe.display.SlotDisplayContexts;
import net.minecraft.recipe.display.SmithingRecipeDisplay;
import net.minecraft.recipe.display.StonecutterRecipeDisplay;
import net.minecraft.util.context.ContextParameterMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 配方查询辅助类，抽象不同 Minecraft 版本间的配方 API 差异。
 * 1.21.4 版：RecipeManager 不再提供 values()/listAllOfType()，
 * 改为从客户端配方簿 ClientRecipeBook 读取 RecipeDisplayEntry
 * （工作台配方携带服务器下发的真实 Ingredient 列表 craftingRequirements）。
 */
public class RecipeHelper {

    public record RecipeLookupResult(List<Ingredient> ingredients, List<ItemStack> resultStacks, int resultCount) {}

    public static RecipeLookupResult lookupRecipe(ItemStack shadow, List<String> typeGroup, MinecraftClient client) {
        List<RecipeLookupResult> all = lookupAllRecipes(shadow, typeGroup, client);
        return all.isEmpty() ? null : all.getFirst();
    }

    public static List<RecipeLookupResult> lookupAllRecipes(ItemStack shadow, List<String> typeGroup, MinecraftClient client) {
        List<RecipeLookupResult> results = new ArrayList<>();
        if (client.world == null || client.player == null) return results;
        ContextParameterMap map = SlotDisplayContexts.createParameters(client.world);
        Set<NetworkRecipeId> seen = new HashSet<>();
        for (RecipeResultCollection collection : client.player.getRecipeBook().getOrderedResults()) {
            for (RecipeDisplayEntry entry : collection.getAllRecipes()) {
                if (!seen.add(entry.id())) continue;
                try {
                    RecipeDisplay display = entry.display();
                    // 锻造纹饰配方的结果槽依赖输入上下文，配方簿中无法解析，跳过
                    if (display.result() instanceof SlotDisplay.SmithingTrimSlotDisplay) continue;
                    String typeStr = displayTypeToString(display);
                    if (typeStr == null || !typeGroup.contains(typeStr)) continue;
                    List<ItemStack> resultStacks = display.result().getStacks(map);
                    if (resultStacks.isEmpty()) continue;
                    ItemStack result = resultStacks.getFirst();
                    if (result.getItem() != shadow.getItem()) continue;
                    List<Ingredient> ingredients = extractIngredients(entry, display, map);
                    if (ingredients.isEmpty()) continue;
                    results.add(new RecipeLookupResult(ingredients, resultStacks, result.getCount()));
                } catch (Exception e) {
                    // 个别配方显示无法在客户端解析，跳过
                }
            }
        }
        return results;
    }

    /** 配方显示对象 → 配方类型字符串（对应 RawMaterialConfig 的 mappedTypes） */
    private static String displayTypeToString(RecipeDisplay display) {
        if (display instanceof ShapedCraftingRecipeDisplay) return "crafting_shaped";
        if (display instanceof ShapelessCraftingRecipeDisplay) return "crafting_shapeless";
        if (display instanceof FurnaceRecipeDisplay) return "smelting";
        if (display instanceof SmithingRecipeDisplay) return "smithing_transform";
        if (display instanceof StonecutterRecipeDisplay) return "stonecutter";
        return null;
    }

    /**
     * 提取配方原料：
     * - 工作台配方：优先使用服务器下发的真实 Ingredient 列表；
     * - 其他配方：解析槽位显示，按槽合成 Ingredient（仅需物品身份用于溯源）。
     */
    private static List<Ingredient> extractIngredients(RecipeDisplayEntry entry, RecipeDisplay display, ContextParameterMap map) {
        if (entry.craftingRequirements().isPresent() && !entry.craftingRequirements().get().isEmpty()) {
            return entry.craftingRequirements().get();
        }
        List<List<ItemStack>> slotStackGroups = new ArrayList<>();
        if (display instanceof FurnaceRecipeDisplay furnace) {
            slotStackGroups.add(furnace.ingredient().getStacks(map));
        } else if (display instanceof SmithingRecipeDisplay smithing) {
            slotStackGroups.add(smithing.base().getStacks(map));
            slotStackGroups.add(smithing.addition().getStacks(map));
        } else if (display instanceof StonecutterRecipeDisplay stonecutter) {
            slotStackGroups.add(stonecutter.input().getStacks(map));
        } else if (display instanceof ShapedCraftingRecipeDisplay shaped) {
            for (SlotDisplay slot : shaped.ingredients()) slotStackGroups.add(slot.getStacks(map));
        } else if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {
            for (SlotDisplay slot : shapeless.ingredients()) slotStackGroups.add(slot.getStacks(map));
        }
        List<Ingredient> result = new ArrayList<>();
        for (List<ItemStack> group : slotStackGroups) {
            List<ItemStack> valid = group.stream().filter(s -> !s.isEmpty()).toList();
            if (!valid.isEmpty()) {
                result.add(Ingredient.ofItems(valid.stream().map(ItemStack::getItem).distinct()));
            }
        }
        return result;
    }

    public static List<ItemStack> getIngredientStacks(Ingredient ing, MinecraftClient client) {
        if (client.world == null || ing.isEmpty()) return List.of();
        try {
            ContextParameterMap map = SlotDisplayContexts.createParameters(client.world);
            return ing.toDisplay().getStacks(map);
        } catch (Exception e) {
            return List.of();
        }
    }

    public static boolean ingredientContainsItem(Ingredient ing, String itemId, MinecraftClient client) {
        for (ItemStack stack : getIngredientStacks(ing, client)) {
            if (net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).toString().equals(itemId)) {
                return true;
            }
        }
        return false;
    }

    public static boolean ingredientContainsBed(Ingredient ing, MinecraftClient client) {
        for (ItemStack stack : getIngredientStacks(ing, client)) {
            if (net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).toString().endsWith("_bed")) {
                return true;
            }
        }
        return false;
    }

    public static String recipeTypeToJson(String typeStr) {
        return switch (typeStr) {
            // 配置字符串（RawMaterialConfig 的 mappedTypes）
            case "stonecutter" -> "stonecutting";
            case "crafting_shaped" -> "crafting_shaped";
            case "crafting_shapeless" -> "crafting_shapeless";
            case "smelting" -> "smelting";
            case "smithing", "smithing_transform" -> "smithing";
            // 旧版内部字符串（兜底）
            case "SHAPED" -> "crafting_shaped";
            case "SHAPELESS" -> "crafting_shapeless";
            case "FURNACE" -> "smelting";
            case "SMITHING" -> "smithing";
            case "STONECUTTER" -> "stonecutting";
            default -> "base";
        };
    }

    public static String jsonToRecipeType(String jsonType) {
        return switch (jsonType) {
            case "crafting_shaped" -> "SHAPED";
            case "crafting_shapeless" -> "SHAPELESS";
            case "smelting" -> "FURNACE";
            case "smithing" -> "SMITHING";
            case "stonecutting" -> "STONECUTTER";
            default -> null;
        };
    }
}