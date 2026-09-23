package com.litematlist;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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

                if (inDataSection && line.startsWith("|")) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 3) {
                        String itemName = parts[1].trim();
                        String totalStr = parts[2].trim();
                        String missingStr = "0";
                        if (!rawFormat && parts.length >= 4) {
                            missingStr = parts[3].trim();
                        }
                        if (itemName.isEmpty() || itemName.equals("Item")) continue;

                        try {
                            int total = Integer.parseInt(totalStr);
                            int missing = Integer.parseInt(missingStr);
                            if (total > 0) {
                                Item item = findItem(itemName);
                                if (item == Items.AIR) {
                                    LOGGER.warn("跳过未匹配物品: '{}'", itemName);
                                    skippedCount++;
                                    continue; // 宽松模式：跳过不匹配的物品，不中断整个导入
                                }
                                itemCounts.merge(item, total, Integer::sum);
                                itemMissing.merge(item, missing, Integer::sum);
                                itemNames.putIfAbsent(item, itemName);
                                totalItems += total;
                                totalMissing += missing;
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }

            if (itemCounts.isEmpty()) {
                LOGGER.warn("未从文件中解析到有效材料: {}", filePath);
                return null;
            }

            List<LitematicReader.MaterialEntry> materials = new ArrayList<>();
            for (Map.Entry<Item, Integer> entry : itemCounts.entrySet()) {
                Item item = entry.getKey();
                materials.add(new LitematicReader.MaterialEntry(
                        item, itemNames.getOrDefault(item, new ItemStack(item).getHoverName().getString()), entry.getValue(), itemMissing.getOrDefault(item, 0)));
            }
            materials.sort((a, b) -> Integer.compare(b.totalCount(), a.totalCount()));

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

                String itemName = unquote(parts[0]);
                if (itemName.isEmpty()) continue;

                try {
                    int total = Integer.parseInt(parts[1].trim());
                    int missing = Integer.parseInt(parts[2].trim());
                    if (total > 0) {
                        Item item = findItem(itemName);
                        if (item == Items.AIR) {
                            LOGGER.warn("跳过未匹配物品: '{}'", itemName);
                            skippedCount++;
                            continue;
                        }
                        itemCounts.merge(item, total, Integer::sum);
                        itemMissing.merge(item, missing, Integer::sum);
                        itemNames.putIfAbsent(item, itemName);
                        totalItems += total;
                        totalMissing += missing;
                    }
                } catch (NumberFormatException ignored) {
                }
            }

            if (itemCounts.isEmpty()) {
                LOGGER.warn("未从 CSV 文件中解析到有效材料: {}", filePath);
                return null;
            }

            List<LitematicReader.MaterialEntry> materials = new ArrayList<>();
            for (Map.Entry<Item, Integer> entry : itemCounts.entrySet()) {
                Item item = entry.getKey();
                materials.add(new LitematicReader.MaterialEntry(
                        item, itemNames.getOrDefault(item, new ItemStack(item).getHoverName().getString()), entry.getValue(), itemMissing.getOrDefault(item, 0)));
            }
            materials.sort((a, b) -> Integer.compare(b.totalCount(), a.totalCount()));

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
                    int total = Integer.parseInt(parts[1].trim());
                    int missing = Integer.parseInt(parts[2].trim());
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
     * 解析一行 CSV 数据，处理双引号包裹的字段。
     * 例如：'"草方块",113,69,1' → ["草方块", "113", "69", "1"]
     */
    private static String[] parseCsvLine(String line) {
        java.util.List<String> result = new java.util.ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
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

    /**
     * 去除字符串两端的双引号。
     */
    private static String unquote(String s) {
        if (s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
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

        File file = filePath.toFile();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            JsonArray array;
            String overrideName = null;
            JsonElement root = GSON.fromJson(reader, JsonElement.class);
            if (root == null) {
                LOGGER.warn("JSON 文件为空: {}", filePath);
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
                        Item item = getItemFromId(id);
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
                if (id == null) {
                    LOGGER.warn("跳过无效物品ID: '{}'", rawItem);
                    skippedCount++;
                    continue;
                }
                Item item = getItemFromId(id);
                if (item == Items.AIR) {
                    LOGGER.warn("跳过未匹配物品: '{}'", rawItem);
                    skippedCount++;
                    continue;
                }
                itemCounts.merge(item, totalEstimate, Integer::sum);
                totalItems += totalEstimate;
            }

            if (itemCounts.isEmpty()) {
                LOGGER.warn("未从 JSON 文件中解析到有效材料: {}", filePath);
                return null;
            }

            List<LitematicReader.MaterialEntry> materials = new ArrayList<>();
            for (Map.Entry<Item, Integer> entry : itemCounts.entrySet()) {
                Item item = entry.getKey();
                materials.add(new LitematicReader.MaterialEntry(
                        item, new ItemStack(item).getHoverName().getString(), entry.getValue()));
            }
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

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            JsonArray array;
            String overrideName = null;
            JsonElement root = GSON.fromJson(reader, JsonElement.class);
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
            item = getItemFromId(id);
            if (item != Items.AIR) return item;
        }
        id = Identifier.tryParse("minecraft:" + name.toLowerCase().replace(' ', '_'));
        if (id != null) {
            item = getItemFromId(id);
            if (item != Items.AIR) return item;
        }
        return Items.AIR;
    }

    /** 从 Identifier 获取 Item */
    private static Item getItemFromId(Identifier id) {
        if (id == null) return Items.AIR;
        var result = BuiltInRegistries.ITEM.get(id);
        return result.isPresent() ? result.get().value() : Items.AIR;
    }
}
