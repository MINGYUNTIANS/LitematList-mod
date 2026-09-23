package com.litematlist;

import com.google.gson.*;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.*;

/**
 * 配方树节点：用于构建横向树状图，匹配示例JSON导出格式。
 * 每个节点包含物品、数量、配方类型、配方详情和子材料列表。
 */
public class RecipeTreeNode {

    public String itemId;
    public final int count;
    public String recipeType; // "base", "crafting_shaped", "crafting_shapeless", "smelting", "smithing", "stonecutting"
    public final List<RecipeDetail> recipes;
    public final List<RecipeTreeNode> subMaterials;

    /** 备选配方类型列表（用于切换合成方式） */
    public List<String> alternativeRecipeTypes;

    /** 是否为通配物品的子材料（仅此类节点显示齿轮替换图标） */
    public boolean wildcardSubMaterial = false;

    public RecipeTreeNode(String itemId, int count, String recipeType,
                          List<RecipeDetail> recipes, List<RecipeTreeNode> subMaterials) {
        this.itemId = itemId;
        this.count = count;
        this.recipeType = recipeType;
        this.recipes = recipes != null ? recipes : new ArrayList<>();
        this.subMaterials = subMaterials != null ? subMaterials : new ArrayList<>();
    }

    /** 基础材料节点（无配方） */
    public static RecipeTreeNode base(String itemId, int count) {
        return new RecipeTreeNode(itemId, count, "base", List.of(), List.of());
    }

    public Item getItem() {
        var ref = BuiltInRegistries.ITEM.get(Identifier.parse(itemId)).orElse(null); return ref != null ? ref.value() : null;
    }

    /** 判断是否为叶子节点（无子材料的基础材料） */
    public boolean isLeaf() {
        return subMaterials.isEmpty();
    }

    /** 获取树的深度（包含当前节点） */
    public int getDepth() {
        if (subMaterials.isEmpty()) return 1;
        int maxSub = 0;
        for (RecipeTreeNode sub : subMaterials) {
            int subDepth = sub.getDepth();
            if (subDepth > maxSub) maxSub = subDepth;
        }
        return 1 + maxSub;
    }

    /** 获取指定层级的节点数（当前层=0） */
    public int getNodeCountAtLevel(int level) {
        if (level == 0) return 1;
        int total = 0;
        for (RecipeTreeNode sub : subMaterials) {
            total += sub.getNodeCountAtLevel(level - 1);
        }
        return total;
    }

    /** 获取所有节点（前序遍历） */
    public List<RecipeTreeNode> flatten() {
        List<RecipeTreeNode> result = new ArrayList<>();
        result.add(this);
        for (RecipeTreeNode sub : subMaterials) {
            result.addAll(sub.flatten());
        }
        return result;
    }

    /** 修剪到指定深度，超出部分标记为基础材料 */
    public RecipeTreeNode trimToDepth(int maxDepth) {
        if (maxDepth <= 1) {
            return new RecipeTreeNode(itemId, count, "base", List.of(), List.of());
        }
        List<RecipeTreeNode> trimmed = new ArrayList<>();
        for (RecipeTreeNode sub : subMaterials) {
            trimmed.add(sub.trimToDepth(maxDepth - 1));
        }
        return new RecipeTreeNode(itemId, count, recipeType, recipes, trimmed);
    }

    /** 序列化为JSON */
    public JsonObject toJson() {
        return toJson(Set.of());
    }

    /** 序列化为JSON，跳过已折叠节点的子节点 */
    public JsonObject toJson(Set<String> collapsedPaths) {
        return toJsonWithPath(collapsedPaths, "");
    }

    /** 递归序列化，携带当前路径上下文以匹配折叠路径 */
    private JsonObject toJsonWithPath(Set<String> collapsedPaths, String parentPath) {
        JsonObject obj = new JsonObject();
        obj.addProperty("item", itemId);
        obj.addProperty("count", count);
        obj.addProperty("recipe_type", recipeType);

        JsonArray recipesArr = new JsonArray();
        for (RecipeDetail detail : recipes) {
            recipesArr.add(detail.toJson());
        }
        obj.add("recipes", recipesArr);

        String currentPath = parentPath.isEmpty() ? itemId : parentPath + ">" + itemId;
        boolean isCollapsed = collapsedPaths.contains(currentPath);

        JsonArray subArr = new JsonArray();
        if (!isCollapsed) {
            for (RecipeTreeNode sub : subMaterials) {
                subArr.add(sub.toJsonWithPath(collapsedPaths, currentPath));
            }
        }
        obj.add("sub_materials", subArr);

        return obj;
    }

    /**
     * 配方详情：对应示例JSON中 recipes 数组的每个元素。
     */
    public static class RecipeDetail {
        public String type; // "shaped", "shapeless", "smelting", "smithing", "stonecutting"
        public String smithingType; // 仅smithing: "transform", "trim"
        public RecipeSlot template; // 仅smithing
        public RecipeSlot base; // 仅smithing
        public RecipeSlot addition; // 仅smithing
        public List<String> pattern; // 仅shaped
        public Map<String, RecipeSlot> key; // 仅shaped
        public List<RecipeSlot> ingredients; // 仅shapeless
        public int resultCount;
        // smelting
        public RecipeSlot ingredient; // 仅smelting/stonecutting
        public int cookingTime;
        public float experience;

        public RecipeDetail() {
            this.resultCount = 1;
        }

        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            switch (type) {
                case "smithing" -> {
                    if (smithingType != null) obj.addProperty("smithing_type", smithingType);
                    if (template != null) obj.add("template", template.toJson());
                    if (base != null) obj.add("base", base.toJson());
                    if (addition != null) obj.add("addition", addition.toJson());
                    obj.addProperty("result_count", resultCount);
                }
                case "shaped" -> {
                    if (pattern != null) {
                        JsonArray pat = new JsonArray();
                        for (String row : pattern) pat.add(row);
                        obj.add("pattern", pat);
                    }
                    if (key != null) {
                        JsonObject keyObj = new JsonObject();
                        for (var entry : key.entrySet()) {
                            keyObj.add(entry.getKey(), entry.getValue().toJson());
                        }
                        obj.add("key", keyObj);
                    }
                }
                case "shapeless" -> {
                    if (ingredients != null) {
                        JsonArray ingArr = new JsonArray();
                        for (RecipeSlot slot : ingredients) ingArr.add(slot.toJson());
                        obj.add("ingredients", ingArr);
                        obj.addProperty("result_count", resultCount);
                    }
                }
                case "smelting" -> {
                    obj.addProperty("cookingtime", cookingTime);
                    obj.addProperty("experience", experience);
                    if (ingredient != null) obj.add("ingredient", ingredient.toJson());
                }
                case "stonecutting" -> {
                    if (ingredient != null) obj.add("ingredient", ingredient.toJson());
                }
            }
            return obj;
        }
    }

    /**
     * 配方槽位：对应示例JSON中的 {"item": "xxx"} 格式。
     */
    public static class RecipeSlot {
        public String item;
        public int count = 1;

        public RecipeSlot(String item) {
            this.item = item;
        }

        public RecipeSlot(String item, int count) {
            this.item = item;
            this.count = count;
        }

        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("item", item);
            return obj;
        }
    }
}
