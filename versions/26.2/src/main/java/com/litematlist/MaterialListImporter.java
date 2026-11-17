package com.litematlist;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 解析 Litematica 导出的 ASCII 表格 txt 文件，提取材料列表。
 * 支持三种格式：
 * 1. 放置原理图'...'的材料列表
 * 2. 原理图'...'材料列表（1/1区域）
 * 3. 选区'...'区域分析
 * 格式示例：
 * +---------+-------+---------+-----------+
 * | 放置原理图'Unnamed'的材料列表                   |
 * +---------+-------+---------+-----------+
 * | Item    | Total | Missing | Available |
 * +---------+-------+---------+-----------+
 * | 铁块      |   142 |      98 |         1 |
 * ...
 */
public class MaterialListImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger("litematlist.MaterialListImporter");
    private static final Gson GSON = new Gson();

    /**
     * 解析结果
     */
    public record ImportResult(String schematicName, List<LitematicReader.MaterialEntry> materials,
                                int totalItems, int totalMissing) {}

    /**
     * 预览信息（不完整解析，仅获取名称和统计）
     */
    public record PreviewInfo(String schematicName, int totalItems, int totalMissing) {}

    /**
     * 从 txt 文件导入材料列表（完整解析，含物品验证）。
     *
     * @return 解析结果，失败返回 null
     */
    public static ImportResult importFromFile(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".json")) {
            return importFromJson(filePath);
        }
        if (fileName.endsWith(".csv")) {
            return importFromCsv(filePath);
        }
        return importFromTxt(filePath);
    }

    private static ImportResult importFromTxt(Path filePath) {
        PreviewInfo preview = parsePreview(filePath);
        if (preview == null) return null;

        File file = filePath.toFile();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            Map<Item, Integer> itemCounts = new LinkedHashMap<>();
            Map<Item, Integer> itemMissing = new LinkedHashMap<>();
            Map<Item, String> itemNames = new LinkedHashMap<>();
            Map<String, Integer> exactTotal = new LinkedHashMap<>();
            Map<String, Integer> exactMissing = new LinkedHashMap<>();
            boolean inDataSection = false;
            boolean rawFormat = false;
            int totalItems = 0;
            int totalMissing = 0;
            int skippedCount = 0;

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("+") || line.isEmpty()) continue;
                if (line.contains("Item") && line.contains("Total")) {
                    inDataSection = true;
                    rawFormat = false;
                    continue;
                }
                if (line.contains("Item") && line.contains("Count")) {
                    inDataSection = true;
                    rawFormat = true;
                    continue;
                }
                if (line.contains("原理图") && line.contains("材料列表")) continue;
                if (line.contains("项目") && line.contains("材料列表")) continue;
                if (line.contains("选区") && line.contains("区域分析")) continue;
                if (line.contains("原理图") && line.contains("原材料")) continue;

                if (inDataSection && line.startsWith("|")) {
                    String[] parts = line.split("\\|");
                    if (parts.length < 3) continue;
                    String itemKey = parts[1].trim();
                    String totalStr = parts[2].trim();
                    String missingStr = "0";
                    // 原材料导出只有 3 列 (Item | Count | Recipe)，Litematica 导出有 4+ 列
                    // 我们只需要 Item 和 Count，Missing 默认为 0
                    if (!rawFormat && parts.length >= 4) {
                        // 4+列格式（Litematica 导出），第三列是 Missing
                        missingStr = parts[3].trim();
                    }
                    if (itemKey.isEmpty() || itemKey.equals("Item")) continue;

                    try {
                        int total = Integer.parseInt(totalStr);
                        int missing = !missingStr.isEmpty() ? Integer.parseInt(missingStr) : 0;
                        if (total > 0) {
                            if (addParsedEntry(itemKey, total, missing, itemCounts, itemMissing, itemNames,
                                    exactTotal, exactMissing)) {
                                totalItems += total;
                                totalMissing += missing;
                            } else {
                                skippedCount++;
                            }
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            if (itemCounts.isEmpty() && exactTotal.isEmpty()) {
                LOGGER.warn("未从文件中解析到有效材料: {}", filePath);
                return null;
            }

            List<LitematicReader.MaterialEntry> materials = buildMaterials(itemCounts, itemMissing, itemNames,
                    exactTotal, exactMissing);
            LOGGER.info("从 {} 导入成功: 原理图='{}', {} 种物品, 总计{}, 缺失{}, 跳过{}个",
                    filePath, preview.schematicName(), materials.size(), totalItems, totalMissing, skippedCount);
            return new ImportResult(preview.schematicName(), materials, totalItems, totalMissing);

        } catch (Exception e) {
            LOGGER.error("导入材料列表失败: {}", filePath, e);
            return null;
        }
    }

    // ---- CSV 导入 ----

    /**
     * 解析 Litematica 导出的 CSV 材料列表。
     * 格式：第一行为标题行 "Item","Total","Missing","Available"
     * 后续每行为一条材料，物品名用双引号包裹，数字不带引号。
     */
    private static ImportResult importFromCsv(Path filePath) {
        PreviewInfo preview = parseCsvPreview(filePath);
        if (preview == null) return null;

        File file = filePath.toFile();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            Map<Item, Integer> itemCounts = new LinkedHashMap<>();
            Map<Item, Integer> itemMissing = new LinkedHashMap<>();
            Map<Item, String> itemNames = new LinkedHashMap<>();
            Map<String, Integer> exactTotal = new LinkedHashMap<>();
            Map<String, Integer> exactMissing = new LinkedHashMap<>();
            int totalItems = 0;
            int totalMissing = 0;
            int skippedCount = 0;
            boolean headerSkipped = false;

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // 跳过标题行
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }

                String[] parts = parseCsvLine(line);
                if (parts.length < 3) continue;

                // 列布局检测：
                //  A. 本项目/Litematica 格式  Item,Total,Missing,Available（第2列为数字总数）
                //  B. 原材料格式            ItemID,Name,Count,Recipe（第2列为名称，第3列为数量）
                boolean totalSecond = isIntField(parts[1]);
                String itemKey = parts[0].trim();
                if (itemKey.isEmpty()) continue;

                try {
                    int total;
                    int missing = 0;
                    if (totalSecond) {
                        total = Integer.parseInt(parts[1].trim());
                        if (isIntField(parts[2])) missing = Integer.parseInt(parts[2].trim());
                    } else {
                        total = Integer.parseInt(parts[2].trim());
                        // 第四列若为数字则视为 Missing（兼容 ItemID,Name,Count,Missing）
                        if (parts.length >= 4 && isIntField(parts[3])) missing = Integer.parseInt(parts[3].trim());
                    }
                    if (total > 0) {
                        if (addParsedEntry(itemKey, total, missing, itemCounts, itemMissing, itemNames,
                                exactTotal, exactMissing)) {
                            totalItems += total;
                            totalMissing += missing;
                        } else {
                            skippedCount++;
                        }
                    }
                } catch (NumberFormatException ignored) {
                }
            }

            if (itemCounts.isEmpty() && exactTotal.isEmpty()) {
                LOGGER.warn("未从 CSV 文件中解析到有效材料: {}", filePath);
                return null;
            }

            List<LitematicReader.MaterialEntry> materials = buildMaterials(itemCounts, itemMissing, itemNames,
                    exactTotal, exactMissing);
            LOGGER.info("从 {} 导入 CSV 成功: 原理图='{}', {} 种物品, 总计{}, 缺失{}, 跳过{}个",
                    filePath, preview.schematicName(), materials.size(), totalItems, totalMissing, skippedCount);
            return new ImportResult(preview.schematicName(), materials, totalItems, totalMissing);

        } catch (Exception e) {
            LOGGER.error("导入 CSV 材料列表失败: {}", filePath, e);
            return null;
        }
    }

    /**
     * 快速预览 CSV 文件（只解析统计信息，不验证物品）。
     */
    private static PreviewInfo parseCsvPreview(Path filePath) {
        File file = filePath.toFile();
        if (!file.exists()) return null;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            int totalItems = 0;
            int totalMissing = 0;
            int entryCount = 0;
            boolean headerSkipped = false;

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }

                String[] parts = parseCsvLine(line);
                if (parts.length < 3) continue;

                try {
                    // 列布局检测：第2列为数字 → Item,Total,Missing,Available；否则 ItemID,Name,Count,Recipe
                    int total;
                    int missing = 0;
                    if (isIntField(parts[1])) {
                        total = Integer.parseInt(parts[1].trim());
                        if (isIntField(parts[2])) missing = Integer.parseInt(parts[2].trim());
                    } else {
                        total = Integer.parseInt(parts[2].trim());
                        if (parts.length >= 4 && isIntField(parts[3])) missing = Integer.parseInt(parts[3].trim());
                    }
                    if (total > 0) {
                        totalItems += total;
                        totalMissing += missing;
                        entryCount++;
                    }
                } catch (NumberFormatException ignored) {
                }
            }

            if (entryCount == 0) return null;

            String fileName = file.getName();
            int dot = fileName.lastIndexOf('.');
            String schematicName = dot > 0 ? fileName.substring(0, dot) : fileName;

            return new PreviewInfo(schematicName, totalItems, totalMissing);

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析一行 CSV 数据，处理双引号包裹的字段与 RFC4180 的双引号转义（{@code ""} → {@code "}）。
     * 例如：'"草方块",113,69,1' → ["草方块", "113", "69", "1"]
     */
    private static String[] parseCsvLine(String line) {
        java.util.List<String> result = new java.util.ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // 字段内的转义双引号
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }

    /** 判断字段是否为纯数字（用于 CSV 列布局检测）。 */
    private static boolean isIntField(String s) {
        if (s == null) return false;
        String t = s.trim();
        if (t.isEmpty()) return false;
        for (int i = 0; i < t.length(); i++) {
            if (!Character.isDigit(t.charAt(i))) return false;
        }
        return true;
    }

    /**
     * 将一条解析记录归入结果集合，支持普通物品与精确物品ID（带数据组件）。
     * 匹配顺序：物品ID → 精确物品ID → 显示名称。匹配失败返回 false。
     */
    private static boolean addParsedEntry(String rawItem, int total, int missing,
            Map<Item, Integer> itemCounts, Map<Item, Integer> itemMissing, Map<Item, String> itemNames,
            Map<String, Integer> exactTotal, Map<String, Integer> exactMissing) {
        if (rawItem == null || rawItem.isEmpty()) return false;
        // 1. 普通物品ID
        Identifier id = Identifier.tryParse(rawItem);
        if (id != null) {
            Item item = BuiltInRegistries.ITEM.get(id).map(Holder::value).orElse(Items.AIR);
            if (item != Items.AIR) {
                itemCounts.merge(item, total, Integer::sum);
                itemMissing.merge(item, missing, Integer::sum);
                itemNames.putIfAbsent(item, new ItemStack(item).getHoverName().getString());
                return true;
            }
        }
        // 2. 精确物品ID（基础ID可用即视为精确物品）
        Identifier baseId = Identifier.tryParse(baseItemIdOf(rawItem));
        if (baseId != null && BuiltInRegistries.ITEM.get(baseId).map(Holder::value).orElse(Items.AIR) != Items.AIR) {
            exactTotal.merge(rawItem, total, Integer::sum);
            exactMissing.merge(rawItem, missing, Integer::sum);
            return true;
        }
        // 3. 显示名称
        Item byName = findItem(rawItem);
        if (byName != Items.AIR) {
            itemCounts.merge(byName, total, Integer::sum);
            itemMissing.merge(byName, missing, Integer::sum);
            itemNames.putIfAbsent(byName, rawItem);
            return true;
        }
        LOGGER.warn("跳过未匹配物品: '{}'", rawItem);
        return false;
    }

    /** 由累计的普通/精确物品数据构建排序后的材料列表。 */
    private static List<LitematicReader.MaterialEntry> buildMaterials(
            Map<Item, Integer> itemCounts, Map<Item, Integer> itemMissing, Map<Item, String> itemNames,
            Map<String, Integer> exactTotal, Map<String, Integer> exactMissing) {
        List<LitematicReader.MaterialEntry> materials = new ArrayList<>();
        for (Map.Entry<Item, Integer> entry : itemCounts.entrySet()) {
            Item item = entry.getKey();
            materials.add(new LitematicReader.MaterialEntry(
                    item, itemNames.getOrDefault(item, new ItemStack(item).getHoverName().getString()),
                    entry.getValue(), itemMissing.getOrDefault(item, 0)));
        }
        for (Map.Entry<String, Integer> entry : exactTotal.entrySet()) {
            String exactId = entry.getKey();
            ItemStack exactStack = parseExactItemStack(exactId);
            if (exactStack == null || exactStack.isEmpty()) continue;
            int total = entry.getValue();
            materials.add(new LitematicReader.MaterialEntry(
                    exactStack.getItem(), exactId, total, exactMissing.getOrDefault(exactId, 0),
                    exactStack.copyWithCount(total)));
        }
        materials.sort((a, b) -> Integer.compare(b.totalCount(), a.totalCount()));
        return materials;
    }

    /**
     * 快速预览 txt 文件（只解析名称和统计，不验证物品）。
     */
    public static PreviewInfo parsePreview(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".json")) {
            return parseJsonPreview(filePath);
        }
        if (fileName.endsWith(".csv")) {
            return parseCsvPreview(filePath);
        }
        return parseTxtPreview(filePath);
    }

    private static PreviewInfo parseTxtPreview(Path filePath) {
        File file = filePath.toFile();
        if (!file.exists()) return null;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String schematicName = null;
            boolean inDataSection = false;
            int totalItems = 0;
            int totalMissing = 0;
            boolean hasValidFormat = false;

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("+") || line.isEmpty()) continue;

                if (line.contains("原理图") && line.contains("材料列表")) {
                    int start = line.indexOf("'");
                    int end = line.lastIndexOf("'");
                    if (start >= 0 && end > start) {
                        schematicName = line.substring(start + 1, end);
                    }
                    hasValidFormat = true;
                    continue;
                }
                if (line.contains("原理图") && line.contains("原材料")) {
                    int start = line.indexOf("'");
                    int end = line.lastIndexOf("'");
                    if (start >= 0 && end > start) {
                        schematicName = line.substring(start + 1, end);
                    }
                    hasValidFormat = true;
                    continue;
                }
                if (line.contains("项目") && line.contains("材料列表")) {
                    int start = line.indexOf("'");
                    int end = line.lastIndexOf("'");
                    if (start >= 0 && end > start) {
                        schematicName = line.substring(start + 1, end);
                    }
                    hasValidFormat = true;
                    continue;
                }
                if (line.contains("选区") && line.contains("区域分析")) {
                    int start = line.indexOf("'");
                    int end = line.lastIndexOf("'");
                    if (start >= 0 && end > start) {
                        schematicName = line.substring(start + 1, end);
                    }
                    hasValidFormat = true;
                    continue;
                }

                if (line.contains("Item") && (line.contains("Total") || line.contains("Count"))) {
                    inDataSection = true;
                    continue;
                }

                if (inDataSection && line.startsWith("|")) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 3) {
                        String itemName = parts[1].trim();
                        if (itemName.isEmpty() || itemName.equals("Item")) continue;
                        try {
                            totalItems += Integer.parseInt(parts[2].trim());
                            // 原材料导出只有 3 列，无 Missing 列
                            if (parts.length >= 4) {
                                try {
                                    totalMissing += Integer.parseInt(parts[3].trim());
                                } catch (NumberFormatException ignored) {
                                }
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }

            if (!hasValidFormat || totalItems == 0) return null;

            if (schematicName == null || schematicName.isEmpty()) {
                String fileName = file.getName();
                int dot = fileName.lastIndexOf('.');
                schematicName = dot > 0 ? fileName.substring(0, dot) : fileName;
            }

            return new PreviewInfo(schematicName, totalItems, totalMissing);

        } catch (Exception e) {
            return null;
        }
    }

    // ---- JSON 导入 ----

    public static ImportResult importFromJson(Path filePath) {
        PreviewInfo preview = parseJsonPreview(filePath);
        if (preview == null) return null;

        try {
            JsonArray array;
            String overrideName = null;
            JsonElement root = readJsonRoot(filePath);
            if (root == null) {
                LOGGER.warn("JSON 文件为空或语法错误: {}", filePath);
                return null;
            }
            // 支持三种 JSON 结构：顶层数组、对象内嵌 Materials 数组、对象内嵌 items 数组
            if (root.isJsonArray()) {
                array = root.getAsJsonArray();
            } else if (root.isJsonObject()) {
                JsonObject rootObj = root.getAsJsonObject();
                if (rootObj.has("Materials")) {
                    array = rootObj.getAsJsonArray("Materials");
                } else if (rootObj.has("Items")) {
                    array = rootObj.getAsJsonArray("Items");
                    if (rootObj.has("Name")) {
                        overrideName = rootObj.get("Name").getAsString();
                    } else if (rootObj.has("name")) {
                        overrideName = rootObj.get("name").getAsString();
                    }
                } else if (rootObj.has("items")) {
                    array = rootObj.getAsJsonArray("items");
                    if (rootObj.has("name")) {
                        overrideName = rootObj.get("name").getAsString();
                    }
                } else {
                    LOGGER.warn("JSON 文件格式不正确: {}", filePath);
                    return null;
                }
            } else {
                LOGGER.warn("JSON 文件格式不正确: {}", filePath);
                return null;
            }
            if (array == null || array.isEmpty()) {
                LOGGER.warn("JSON 文件为空或格式不正确: {}", filePath);
                return null;
            }

            Map<Item, Integer> itemCounts = new LinkedHashMap<>();
            List<LitematicReader.MaterialEntry> exactMaterials = new ArrayList<>();
            int totalItems = 0;
            int skippedCount = 0;

            for (JsonElement elem : array) {
                JsonObject obj = elem.getAsJsonObject();

                // 格式1: 原料列表（简化）—— RawItem + Results 子数组
                // 展开 Results 以获取最终成品（如石头→石砖台阶、石砖楼梯）
                if (obj.has("Results")) {
                    JsonArray results = obj.getAsJsonArray("Results");
                    for (JsonElement resultElem : results) {
                        JsonObject resultObj = resultElem.getAsJsonObject();
                        String resultItem = resultObj.get("ResultItem").getAsString();
                        int resultTotal = resultObj.get("ResultTotal").getAsInt();
                        if (resultTotal <= 0) continue;
                        Identifier id = Identifier.tryParse(resultItem);
                        if (id == null) {
                            LOGGER.warn("跳过无效物品ID: '{}'", resultItem);
                            skippedCount++;
                            continue;
                        }
                        Item item = BuiltInRegistries.ITEM.get(id).map(Holder::value).orElse(Items.AIR);
                        if (item == Items.AIR) {
                            LOGGER.warn("跳过未匹配物品: '{}'", resultItem);
                            skippedCount++;
                            continue;
                        }
                        itemCounts.merge(item, resultTotal, Integer::sum);
                        totalItems += resultTotal;
                    }
                    continue;
                }

                String rawItem;
                int totalEstimate;
                // 格式2: RawItem/TotalEstimate（旧版原料列表）
                if (obj.has("RawItem")) {
                    rawItem = obj.get("RawItem").getAsString();
                    totalEstimate = obj.get("TotalEstimate").getAsInt();
                // 格式3: Item/Total 或 Item/Count（旧版材料列表）
                // 优先使用 Missing（缺少数量），供 tweakermore 备货使用
                } else if (obj.has("Item")) {
                    rawItem = obj.get("Item").getAsString();
                    if (obj.has("Missing")) {
                        totalEstimate = obj.get("Missing").getAsInt();
                    } else if (obj.has("Total")) {
                        totalEstimate = obj.get("Total").getAsInt();
                    } else {
                        totalEstimate = obj.get("Count").getAsInt();
                    }
                // 格式4: id/count（简单材料列表 { name, items: [{id, count}] }）
                } else if (obj.has("id")) {
                    rawItem = obj.get("id").getAsString();
                    totalEstimate = obj.get("count").getAsInt();
                } else {
                    LOGGER.warn("跳过无效 JSON 条目，缺少可识别的字段");
                    skippedCount++;
                    continue;
                }

                if (totalEstimate <= 0) continue;

                Identifier id = Identifier.tryParse(rawItem);
                if (id != null) {
                    Item item = BuiltInRegistries.ITEM.get(id).map(Holder::value).orElse(Items.AIR);
                    if (item == Items.AIR) {
                        LOGGER.warn("跳过未匹配物品: '{}'", rawItem);
                        skippedCount++;
                        continue;
                    }
                    itemCounts.merge(item, totalEstimate, Integer::sum);
                    totalItems += totalEstimate;
                    continue;
                }

                // 精确物品ID（带数据组件，如 minecraft:decorated_pot[pot_decorations=[...]]）
                Identifier baseId = Identifier.tryParse(baseItemIdOf(rawItem));
                Item baseItem = baseId != null
                        ? BuiltInRegistries.ITEM.get(baseId).map(Holder::value).orElse(Items.AIR)
                        : Items.AIR;
                ItemStack exactStack = parseExactItemStack(rawItem);
                if (exactStack != null && exactStack.isEmpty() == false) {
                    if (baseItem == Items.AIR) baseItem = exactStack.getItem();
                    exactMaterials.add(new LitematicReader.MaterialEntry(
                            baseItem, rawItem, totalEstimate, 0, exactStack.copyWithCount(totalEstimate)));
                    totalItems += totalEstimate;
                    continue;
                }
                if (baseItem != Items.AIR) {
                    // 精确信息解码失败时退化为普通物品，保留数量
                    itemCounts.merge(baseItem, totalEstimate, Integer::sum);
                    totalItems += totalEstimate;
                    continue;
                }
                LOGGER.warn("跳过无效物品ID: '{}'", rawItem);
                skippedCount++;
            }

            if (itemCounts.isEmpty() && exactMaterials.isEmpty()) {
                LOGGER.warn("未从 JSON 文件中解析到有效材料: {}", filePath);
                return null;
            }

            List<LitematicReader.MaterialEntry> materials = new ArrayList<>();
            for (Map.Entry<Item, Integer> entry : itemCounts.entrySet()) {
                Item item = entry.getKey();
                materials.add(new LitematicReader.MaterialEntry(
                        item, new ItemStack(item).getHoverName().getString(), entry.getValue()));
            }
            materials.addAll(exactMaterials);
            materials.sort((a, b) -> Integer.compare(b.totalCount(), a.totalCount()));

            LOGGER.info("从 {} 导入成功: 原理图='{}', {} 种物品, 总计{}, 跳过{}个",
                    filePath, overrideName != null ? overrideName : preview.schematicName(), materials.size(), totalItems, skippedCount);
            return new ImportResult(overrideName != null ? overrideName : preview.schematicName(), materials, totalItems, 0);

        } catch (Exception e) {
            LOGGER.error("导入 JSON 材料列表失败: {}", filePath, e);
            return null;
        }
    }

    public static PreviewInfo parseJsonPreview(Path filePath) {
        File file = filePath.toFile();
        if (!file.exists()) return null;

        try {
            JsonArray array;
            String overrideName = null;
            JsonElement root = readJsonRoot(filePath);
            if (root == null) return null;
            // 支持三种 JSON 结构：顶层数组、对象内嵌 Materials 数组、对象内嵌 items 数组
            if (root.isJsonArray()) {
                array = root.getAsJsonArray();
            } else if (root.isJsonObject()) {
                JsonObject rootObj = root.getAsJsonObject();
                if (rootObj.has("Materials")) {
                    array = rootObj.getAsJsonArray("Materials");
                } else if (rootObj.has("Items")) {
                    array = rootObj.getAsJsonArray("Items");
                    if (rootObj.has("Name")) {
                        overrideName = rootObj.get("Name").getAsString();
                    } else if (rootObj.has("name")) {
                        overrideName = rootObj.get("name").getAsString();
                    }
                } else if (rootObj.has("Items")) {
                    array = rootObj.getAsJsonArray("Items");
                    if (rootObj.has("Name")) {
                        overrideName = rootObj.get("Name").getAsString();
                    } else if (rootObj.has("name")) {
                        overrideName = rootObj.get("name").getAsString();
                    }
                } else if (rootObj.has("items")) {
                    array = rootObj.getAsJsonArray("items");
                    if (rootObj.has("name")) {
                        overrideName = rootObj.get("name").getAsString();
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
            if (array == null || array.isEmpty()) return null;

            int totalItems = 0;
            int totalMissing = 0;
            int entryCount = 0;

            for (JsonElement elem : array) {
                JsonObject obj = elem.getAsJsonObject();

                // 格式: Results 子数组（原料列表简化），展开 Results
                if (obj.has("Results")) {
                    JsonArray results = obj.getAsJsonArray("Results");
                    for (JsonElement resultElem : results) {
                        JsonObject resultObj = resultElem.getAsJsonObject();
                        int resultTotal = resultObj.get("ResultTotal").getAsInt();
                        if (resultTotal > 0) {
                            totalItems += resultTotal;
                            entryCount++;
                        }
                    }
                    continue;
                }

                int totalEstimate;
                // 支持 TotalEstimate / Total / Count / count 四种字段名
                if (obj.has("TotalEstimate")) {
                    totalEstimate = obj.get("TotalEstimate").getAsInt();
                } else if (obj.has("Total")) {
                    totalEstimate = obj.get("Total").getAsInt();
                } else if (obj.has("Count")) {
                    totalEstimate = obj.get("Count").getAsInt();
                } else if (obj.has("count")) {
                    totalEstimate = obj.get("count").getAsInt();
                } else {
                    continue;
                }
                if (totalEstimate > 0) {
                    totalItems += totalEstimate;
                    entryCount++;
                    // 解析 Missing 字段
                    if (obj.has("Missing")) {
                        totalMissing += obj.get("Missing").getAsInt();
                    }
                }
            }

            if (entryCount == 0) return null;

            String schematicName;
            if (overrideName != null) {
                schematicName = overrideName;
            } else {
                String fileName = file.getName();
                int dot = fileName.lastIndexOf('.');
                schematicName = dot > 0 ? fileName.substring(0, dot) : fileName;
            }

            return new PreviewInfo(schematicName, totalItems, totalMissing);

        } catch (Exception e) {
            return null;
        }
    }

    // ---- JSON 读取（含历史非法转义修复） ----

    /**
     * 读取 JSON 根元素。早期版本导出时未对物品ID内的引号转义，
     * 导致 JSON 语法非法；此处先按标准解析，失败时按行修复后再解析。
     */
    private static JsonElement readJsonRoot(Path filePath) {
        String content;
        try {
            content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.error("读取 JSON 文件失败: {}", filePath, e);
            return null;
        }
        try {
            JsonElement root = GSON.fromJson(content, JsonElement.class);
            if (root != null && root.isJsonNull() == false) {
                return root;
            }
        } catch (Exception ignored) {
            // 语法非法：进入修复流程
        }
        try {
            JsonElement root = GSON.fromJson(repairMalformedJson(content), JsonElement.class);
            if (root != null && root.isJsonNull() == false) {
                LOGGER.info("已修复历史导出的非法 JSON 转义: {}", filePath);
                return root;
            }
        } catch (Exception e) {
            LOGGER.error("JSON 解析失败: {}", filePath, e);
        }
        return null;
    }

    /** 逐行修复 Item 值中未转义的双引号（精确物品ID）。 */
    private static String repairMalformedJson(String content) {
        String[] lines = content.split("\n", -1);
        StringBuilder sb = new StringBuilder(content.length() + 256);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) sb.append('\n');
            sb.append(repairItemLine(lines[i]));
        }
        return sb.toString();
    }

    private static String repairItemLine(String line) {
        String key = "\"Item\": \"";
        int keyIdx = line.indexOf(key);
        if (keyIdx < 0) return line;
        int valueStart = keyIdx + key.length();
        int totalIdx = line.lastIndexOf("\", \"Total\":");
        if (totalIdx <= valueStart) return line;
        String value = line.substring(valueStart, totalIdx);
        return line.substring(0, valueStart) + MaterialListExporter.jsonEscape(value) + line.substring(totalIdx);
    }

    /** 精确物品ID（id[组件...] / id{...}）取其基础物品ID部分。 */
    private static String baseItemIdOf(String exactId) {
        int end = exactId.length();
        int bracket = exactId.indexOf('[');
        int brace = exactId.indexOf('{');
        if (bracket >= 0) end = Math.min(end, bracket);
        if (brace >= 0) end = Math.min(end, brace);
        return exactId.substring(0, end);
    }

    /**
     * 用原版 ItemParser 将精确物品ID解码为 ItemStack（含数据组件）；失败返回 null。
     * 26.x mojmap 对应 yarn 的 ItemStringReader：ItemParser(HolderLookup.Provider).parse(StringReader)
     * 返回 ItemInput，再 createItemStack(int) 得到带组件的物品栈。
     */
    private static ItemStack parseExactItemStack(String exactId) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null) return null;
        try {
            ItemParser parser = new ItemParser(client.level.registryAccess());
            ItemInput input = parser.parse(new StringReader(exactId));
            return input.createItemStack(1);
        } catch (Exception e) {
            LOGGER.debug("精确物品ID解码失败: {} ({})", exactId, e.getMessage());
            return null;
        }
    }

    // ---- 物品名称缓存 ----
    private static Map<String, Item> itemByNameCache = null;

    private static void ensureItemCache() {
        if (itemByNameCache != null) return;
        itemByNameCache = new HashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            // 使用 getName().getString() 获取翻译后的显示名称（最可靠的方式）
            String name = new ItemStack(item).getHoverName().getString();
            if (name != null && !name.isEmpty()) {
                itemByNameCache.putIfAbsent(name, item);
            }
            // 同时用 registry path 作为英文名/ID 匹配的 fallback
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (id != null) {
                String path = id.getPath().replace('_', ' ');
                itemByNameCache.putIfAbsent(path, item);
            }
        }
        LOGGER.info("物品名称缓存已构建: {} 个条目", itemByNameCache.size());
        // 输出前 10 条缓存条目用于调试
        int count = 0;
        for (Map.Entry<String, Item> entry : itemByNameCache.entrySet()) {
            if (count++ >= 10) break;
            LOGGER.info("  缓存: '{}' -> {}", entry.getKey(), BuiltInRegistries.ITEM.getId(entry.getValue()));
        }
    }

    /**
     * 根据物品名（中文或英文）查找 Item。
     */
    private static Item findItem(String name) {
        ensureItemCache();
        // 直接匹配
        Item item = itemByNameCache.get(name);
        if (item != null) {
            LOGGER.debug("物品匹配成功: '{}' -> {}", name, BuiltInRegistries.ITEM.getId(item));
            return item;
        }
        LOGGER.debug("物品未在缓存中找到: '{}'，尝试 Identifier 匹配", name);
        // 尝试通过 Identifier 查找
        Identifier id = Identifier.tryParse(name);
        if (id != null) {
            item = BuiltInRegistries.ITEM.get(id).map(Holder::value).orElse(Items.AIR);
            if (item != Items.AIR) return item;
        }
        id = Identifier.tryParse("minecraft:" + name.toLowerCase().replace(' ', '_'));
        if (id != null) {
            item = BuiltInRegistries.ITEM.get(id).map(Holder::value).orElse(Items.AIR);
            if (item != Items.AIR) return item;
        }
        return Items.AIR;
    }
}