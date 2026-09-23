package com.litematlist.gui;

import com.litematlist.LitematicReader;
import com.litematlist.MaterialListImporter;
import com.litematlist.MaterialListInjector;
import com.litematlist.PinyinSearch;
import com.litematlist.I18n;
import com.litematlist.LitematListMod;
import com.litematlist.config.Configs;
import com.litematlist.LitematicaBridge;
import com.litematlist.RawMaterialAnalyzer;
import com.litematlist.SyncmaticaBridge;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.util.InventoryUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import fi.dy.masa.malilib.render.GuiContext;
import org.apache.commons.lang3.tuple.Pair;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.GpuSampler;
import org.joml.Matrix3x2f;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.*;

/**
 * 材料清单子界面 —— 参照投影的 MaterialList 界面布局。
 * 总计/已有/缺失左置，替换/忽略按钮贴右。
 */
public class MaterialDetailScreen extends GuiBase {

    private final GuiBase parent;
    private final String schematicName;
    private final Path filePath;
    private final List<MaterialRow> rows = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int ROW_HEIGHT = 22;
    private static final int HEADER_Y = 42;
    private static final int HEADER_HEIGHT = 16;
    private static final int LIST_TOP = HEADER_Y + HEADER_HEIGHT + 2;
    private int visibleRows;
    private boolean scrollbarDragging = false;
    private boolean loaded = false;
    private int hoveredRow = -1;
    private int hoveredColumn = -1; // 0=总计, 1=已有, 2=缺失

    // ---- 已忽略物品 ----
    private static final Map<Path, Set<Item>> ignoredItems = new HashMap<>();
    private Set<Item> currentIgnored;

    // ---- 替换映射（持久化：原理图路径 → 原始物品ID → 替换物品ID） ----
    private static final Map<String, Map<String, String>> replacements = new HashMap<>();
    private static boolean persistenceLoaded = false;
    private static final Gson GSON = new Gson();
    private static Path persistenceFile;
    private static String currentWorldId = null;
    public static boolean persistedHideNoMissing = false;
    public static boolean persistedAllowPin = true;
    public static String persistedDetailSortMode = "NONE";
    public static String persistedProjectSortMode = "NONE";
    public static boolean persistedHudVisible = false;
    public static int persistedHudFontSize = 10;
    public static int persistedHudBgAlpha = 50;
    public static int persistedHudMaxLines = 10;
    public static boolean persistedHudShowBox = false;   // 盒(1728)开关
    public static boolean persistedHudShowGroup = false; // 组(64)开关
    public static int persistedHudX = 10;
    public static int persistedHudY = 30;
    // ---- 原材料分析持久化数据 ----
    /** 原材料列表: pathKey -> raw material entries */
    public static final Map<String, List<RawMaterialEntry>> rawMaterials = new HashMap<>();
    /** 冗余列表: pathKey -> redundant items */
    public static final Map<String, List<RawMaterialEntry>> rawMaterialRedundancy = new HashMap<>();
    /** 原材料替换: pathKey -> (originalItemId -> replacementItemId) */
    public static final Map<String, Map<String, String>> rawMaterialReplacements = new HashMap<>();
    /** 原材料忽略: pathKey -> set of ignored item IDs */
    public static final Map<String, Set<String>> rawMaterialIgnored = new HashMap<>();
    public static final Map<String, Set<String>> rawMaterialSourceIgnored = new HashMap<>();
    /** 上传至 PlayerControl++ 的条目 path */
    public static String playerControlPlusUploaded = null;
    /** PlayerControl++ 读取的配方树根节点列表（字段 k），注入后供其他模组接入 */
    public static List<com.litematlist.RecipeTreeNode> playerControlPlusMaterialList = null;

    /** 原材料条目 */
    public record RawMaterialEntry(String itemId, int count, List<String> sources) {
        public RawMaterialEntry withCount(int newCount) { return new RawMaterialEntry(itemId, newCount, sources); }
    }

    /** 获取当前世界的标识符（单人=存档名，多人=服务器IP，主菜单=null） */
    public static String getWorldId() {
        Minecraft client = Minecraft.getInstance();
        if (client.getCurrentServer() != null) {
            return "mp_" + client.getCurrentServer().ip.replace(":", "_");
        }
        if (client.getSingleplayerServer() != null) {
            // 使用存档文件夹名作为唯一标识（显示名称可能重复，导致不同存档共享持久化数据）
            // 通过 getWorldPath(LevelResource.ROOT) 获取存档目录路径
            net.minecraft.server.MinecraftServer server = client.getSingleplayerServer();
            java.nio.file.Path savePath = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize();
            return "sp_" + savePath.getFileName().toString();
        }
        return null;
    }

    // ---- 导入材料缓存（txt 文件导入的材料列表，不经过 .litematic 解析） ----
    private static final Map<Path, List<LitematicReader.MaterialEntry>> importedMaterials = new HashMap<>();

    /**
     * 缓存从 txt 导入的材料列表，供 MaterialDetailScreen 加载时使用。
     */
    public static void cacheMaterialList(Path filePath, List<LitematicReader.MaterialEntry> materials) {
        importedMaterials.put(filePath, materials);
    }

    /** 供外部获取导入材料缓存 */
    public static Map<Path, List<LitematicReader.MaterialEntry>> getImportedMaterials() {
        return importedMaterials;
    }

    /** 获取指定路径的已忽略物品集合 */
    public static Set<Item> getIgnoredForPath(Path path) {
        ensurePersistenceLoaded();
        return ignoredItems.computeIfAbsent(path, k -> new HashSet<>());
    }

    /** 获取指定路径的置顶物品集合 */
    public static Set<Item> getPinnedForPath(Path path) {
        ensurePersistenceLoaded();
        return pinnedItems.computeIfAbsent(path, k -> new HashSet<>());
    }

    private static void ensurePersistenceLoaded() {
        ensurePersistenceLoaded(null);
    }

    private static void ensurePersistenceLoaded(String explicitWorldId) {
        String worldId = explicitWorldId != null ? explicitWorldId : getWorldId();
        
        if (worldId == null) return; // 不在世界中，不加载持久化数据
        
        if (persistenceLoaded && worldId.equals(currentWorldId)) return; // 已加载且 worldId 未变
        
        persistenceLoaded = true;
        currentWorldId = worldId;
        persistenceFile = FabricLoader.getInstance().getConfigDir().resolve("litematlist/" + worldId + "/persistence.json");

        if (java.nio.file.Files.exists(persistenceFile)) {
            try (Reader reader = new InputStreamReader(new FileInputStream(persistenceFile.toFile()), java.nio.charset.StandardCharsets.UTF_8)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root != null) {
                    // 加载 ignored
                    JsonObject ignoredData = root.getAsJsonObject("ignored");
                    if (ignoredData != null) {
                        for (var entry : ignoredData.entrySet()) {
                            Path path = Path.of(entry.getKey());
                            Set<Item> items = new HashSet<>();
                            JsonObject itemsObj = entry.getValue().getAsJsonObject();
                            for (String itemId : itemsObj.keySet()) {
                                var holder = BuiltInRegistries.ITEM.get(Identifier.parse(itemId));
                                if (holder.isPresent()) {
                                    Item item = holder.get().value();
                                    if (item != null) items.add(item);
                                }
                            }
                            ignoredItems.put(path, items);
                        }
                    }
                    // 加载 replacements
                    JsonObject replData = root.getAsJsonObject("replacements");
                    if (replData != null) {
                        for (var entry : replData.entrySet()) {
                            Map<String, String> map = new HashMap<>();
                            JsonObject inner = entry.getValue().getAsJsonObject();
                            for (var e : inner.entrySet()) {
                                map.put(e.getKey(), e.getValue().getAsString());
                            }
                            replacements.put(entry.getKey(), map);
                        }
                    }
                    // 加载 entries
                    JsonArray entriesArr = root.getAsJsonArray("entries");
                    if (entriesArr != null) {
                        for (JsonElement elem : entriesArr) {
                            JsonObject obj = elem.getAsJsonObject();
                            String name = obj.get("name").getAsString();
                            // 项目文件夹持久化时 path 为空串，必须还原为 null
                            String pathStr = obj.get("path").getAsString();
                            Path path = pathStr.isEmpty() ? null : Path.of(pathStr);
                            boolean pinned = obj.has("pinned") && obj.get("pinned").getAsBoolean();
                            boolean fromLitematica = obj.has("fromLitematica") && obj.get("fromLitematica").getAsBoolean();
                            MaterialListScreen.LoadedEntry le = MaterialListScreen.addEntryInternal(name, path, fromLitematica, pinned);
                            if (le != null) {
                                if (obj.has("projectId")) le.projectId = obj.get("projectId").getAsString();
                                if (obj.has("isProject")) le.isProject = obj.get("isProject").getAsBoolean();
                                if (obj.has("hotkey")) le.hotkey = obj.get("hotkey").getAsString();
                                if (obj.has("uploaded")) le.uploaded = obj.get("uploaded").getAsBoolean();
                            }
                        }
                        LitematListMod.LOGGER.info("已加载 {} 个持久化条目", entriesArr.size());
                    }
                    // 加载 pinnedItems（材料清单中的置顶物品）
                    JsonObject pinnedData = root.getAsJsonObject("pinnedItems");
                    if (pinnedData != null) {
                        for (var entry : pinnedData.entrySet()) {
                            Path path = Path.of(entry.getKey());
                            Set<Item> items = new HashSet<>();
                            JsonArray arr = entry.getValue().getAsJsonArray();
                            for (JsonElement e : arr) {
                                var holder = BuiltInRegistries.ITEM.get(Identifier.parse(e.getAsString()));
                                if (holder.isPresent()) {
                                    Item item = holder.get().value();
                                    if (item != null) items.add(item);
                                }
                            }
                            pinnedItems.put(path, items);
                        }
                    }
                    // 加载原材料分析数据
                    JsonObject rawMatData = root.getAsJsonObject("rawMaterials");
                    if (rawMatData != null) {
                        for (var entry : rawMatData.entrySet()) {
                            List<RawMaterialEntry> list = new ArrayList<>();
                            JsonArray arr = entry.getValue().getAsJsonArray();
                            for (JsonElement e : arr) {
                                JsonObject obj = e.getAsJsonObject();
                                String itemId = obj.get("itemId").getAsString();
                                int count = obj.get("count").getAsInt();
                                List<String> sources = new ArrayList<>();
                                if (obj.has("sources")) {
                                    JsonArray srcArr = obj.getAsJsonArray("sources");
                                    for (JsonElement s : srcArr) sources.add(s.getAsString());
                                }
                                list.add(new RawMaterialEntry(itemId, count, sources));
                            }
                            rawMaterials.put(entry.getKey(), list);
                        }
                    }
                    JsonObject rawRedunData = root.getAsJsonObject("rawMaterialRedundancy");
                    if (rawRedunData != null) {
                        for (var entry : rawRedunData.entrySet()) {
                            List<RawMaterialEntry> list = new ArrayList<>();
                            JsonArray arr = entry.getValue().getAsJsonArray();
                            for (JsonElement e : arr) {
                                JsonObject obj = e.getAsJsonObject();
                                String itemId = obj.get("itemId").getAsString();
                                int count = obj.get("count").getAsInt();
                                List<String> sources = new ArrayList<>();
                                if (obj.has("sources")) {
                                    JsonArray srcArr = obj.getAsJsonArray("sources");
                                    for (JsonElement s : srcArr) sources.add(s.getAsString());
                                }
                                list.add(new RawMaterialEntry(itemId, count, sources));
                            }
                            rawMaterialRedundancy.put(entry.getKey(), list);
                        }
                    }
                    JsonObject rawReplData = root.getAsJsonObject("rawMaterialReplacements");
                    if (rawReplData != null) {
                        for (var entry : rawReplData.entrySet()) {
                            Map<String, String> map = new HashMap<>();
                            JsonObject inner = entry.getValue().getAsJsonObject();
                            for (var e : inner.entrySet()) {
                                map.put(e.getKey(), e.getValue().getAsString());
                            }
                            rawMaterialReplacements.put(entry.getKey(), map);
                        }
                    }
                    JsonObject rawIgnData = root.getAsJsonObject("rawMaterialIgnored");
                    if (rawIgnData != null) {
                        for (var entry : rawIgnData.entrySet()) {
                            Set<String> set = new HashSet<>();
                            JsonArray arr = entry.getValue().getAsJsonArray();
                            for (JsonElement e : arr) set.add(e.getAsString());
                            rawMaterialIgnored.put(entry.getKey(), set);
                        }
                    }
                    JsonObject rawSrcIgnData = root.getAsJsonObject("rawMaterialSourceIgnored");
                    if (rawSrcIgnData != null) {
                        for (var entry : rawSrcIgnData.entrySet()) {
                            Set<String> set = new HashSet<>();
                            JsonArray arr = entry.getValue().getAsJsonArray();
                            for (JsonElement e : arr) set.add(e.getAsString());
                            rawMaterialSourceIgnored.put(entry.getKey(), set);
                        }
                    }
                    if (root.has("playerControlPlusUploaded")) {
                        playerControlPlusUploaded = root.get("playerControlPlusUploaded").getAsString();
                    }
                    // 树状图缩放档位（按世界保存，退出存档/重启客户端后恢复）
                    if (root.has("treeZoomPercent")) {
                        RawMaterialScreen.setTreeZoomPercent(root.get("treeZoomPercent").getAsInt());
                    }
                    // 加载 screenSettings
                    if (root.has("screenSettings")) {
                        JsonObject screenSettings = root.getAsJsonObject("screenSettings");
                        if (screenSettings.has("hideNoMissing")) {
                            persistedHideNoMissing = screenSettings.get("hideNoMissing").getAsBoolean();
                        }
                        if (screenSettings.has("allowPin")) {
                            persistedAllowPin = screenSettings.get("allowPin").getAsBoolean();
                        }
                        if (screenSettings.has("detailSortMode")) {
                            persistedDetailSortMode = screenSettings.get("detailSortMode").getAsString();
                        }
                        if (screenSettings.has("projectSortMode")) {
                            persistedProjectSortMode = screenSettings.get("projectSortMode").getAsString();
                        }
                        if (screenSettings.has("hudVisible")) {
                            persistedHudVisible = screenSettings.get("hudVisible").getAsBoolean();
                        }
                        if (screenSettings.has("hudFontSize")) {
                            persistedHudFontSize = screenSettings.get("hudFontSize").getAsInt();
                        }
                        if (screenSettings.has("hudBgAlpha")) {
                            persistedHudBgAlpha = screenSettings.get("hudBgAlpha").getAsInt();
                        }
                        if (screenSettings.has("hudMaxLines")) {
                            persistedHudMaxLines = screenSettings.get("hudMaxLines").getAsInt();
                        }
                        if (screenSettings.has("hudShowBox")) {
                            persistedHudShowBox = screenSettings.get("hudShowBox").getAsBoolean();
                        }
                        if (screenSettings.has("hudShowGroup")) {
                            persistedHudShowGroup = screenSettings.get("hudShowGroup").getAsBoolean();
                        }
                        if (screenSettings.has("hudX")) {
                            persistedHudX = screenSettings.get("hudX").getAsInt();
                        }
                        if (screenSettings.has("hudY")) {
                            persistedHudY = screenSettings.get("hudY").getAsInt();
                        }
                    }
                }
                LitematListMod.LOGGER.info("已加载持久化数据: {} 个忽略集, {} 个替换映射", ignoredItems.size(), replacements.size());
            } catch (Exception e) {
                LitematListMod.LOGGER.error("加载持久化数据失败", e);
            }
        }
    }

    /** 切换世界时保存旧数据并加载新数据 */
    public static void switchToWorld(String newWorldId) {
        // 保存旧世界数据
        if (currentWorldId != null && persistenceFile != null) {
            savePersistence();
        }
        // 清理内存数据
        synchronized (MaterialListScreen.class) {
            MaterialListScreen.clearLoadedEntries();
        }
        ignoredItems.clear();
        replacements.clear();
        pinnedItems.clear();
        importedMaterials.clear();
        reorderedItems.clear();
        rawMaterials.clear();
        rawMaterialRedundancy.clear();
        rawMaterialReplacements.clear();
        rawMaterialIgnored.clear();
        rawMaterialSourceIgnored.clear();
        playerControlPlusUploaded = null;
        // 树状图缩放档位回到默认，随后由新世界持久化数据覆盖
        RawMaterialScreen.resetTreeZoomPercent();
        // 标记需要重新加载
        persistedHudVisible = false;
        MaterialListHudRenderer.clearHudItems();
        persistenceLoaded = false;
        persistenceFile = null;
        currentWorldId = null;
        // 加载新世界数据
        if (newWorldId != null) {
            ensurePersistenceLoaded(newWorldId);
        }
    }

    /** 保存所有持久化数据（忽略、替换、条目列表） */
    public static void savePersistence() {
        if (persistenceFile == null) return;
        try {
            JsonObject root = new JsonObject();

            // ignored
            JsonObject ignoredData = new JsonObject();
            for (var entry : ignoredItems.entrySet()) {
                JsonObject items = new JsonObject();
                for (Item item : entry.getValue()) {
                    Identifier id = BuiltInRegistries.ITEM.getKey(item);
                    if (id != null) items.addProperty(id.toString(), "");
                }
                ignoredData.add(entry.getKey().toString(), items);
            }
            root.add("ignored", ignoredData);

            // replacements
            JsonObject replData = new JsonObject();
            for (var entry : replacements.entrySet()) {
                JsonObject inner = new JsonObject();
                for (var e : entry.getValue().entrySet()) {
                    inner.addProperty(e.getKey(), e.getValue());
                }
                replData.add(entry.getKey(), inner);
            }
            root.add("replacements", replData);

            // entries
            JsonArray entriesArr = new JsonArray();
            for (MaterialListScreen.LoadedEntry le : MaterialListScreen.getLoadedEntries()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("name", le.name());
                obj.addProperty("path", le.filePath() != null ? le.filePath().toString() : "");
                obj.addProperty("pinned", le.pinned);
                obj.addProperty("fromLitematica", le.fromLitematica());
                if (le.projectId != null) obj.addProperty("projectId", le.projectId);
                if (le.isProject) obj.addProperty("isProject", true);
                if (le.hotkey != null && !le.hotkey.isEmpty()) obj.addProperty("hotkey", le.hotkey);
                entriesArr.add(obj);
            }
            root.add("entries", entriesArr);

            // pinnedItems（材料清单中的置顶物品）
            JsonObject pinnedData = new JsonObject();
            for (var entry : pinnedItems.entrySet()) {
                JsonArray arr = new JsonArray();
                for (Item item : entry.getValue()) {
                    Identifier id = BuiltInRegistries.ITEM.getKey(item);
                    if (id != null) arr.add(id.toString());
                }
                if (!arr.isEmpty()) {
                    pinnedData.add(entry.getKey().toString(), arr);
                }
            }
            root.add("pinnedItems", pinnedData);

            // 原材料分析数据
            JsonObject rawMatData = new JsonObject();
            for (var entry : rawMaterials.entrySet()) {
                JsonArray arr = new JsonArray();
                for (RawMaterialEntry rme : entry.getValue()) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("itemId", rme.itemId());
                    obj.addProperty("count", rme.count());
                    JsonArray srcArr = new JsonArray();
                    for (String s : rme.sources()) srcArr.add(s);
                    obj.add("sources", srcArr);
                    arr.add(obj);
                }
                rawMatData.add(entry.getKey(), arr);
            }
            root.add("rawMaterials", rawMatData);

            JsonObject rawRedunData = new JsonObject();
            for (var entry : rawMaterialRedundancy.entrySet()) {
                JsonArray arr = new JsonArray();
                for (RawMaterialEntry rme : entry.getValue()) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("itemId", rme.itemId());
                    obj.addProperty("count", rme.count());
                    JsonArray srcArr = new JsonArray();
                    for (String s : rme.sources()) srcArr.add(s);
                    obj.add("sources", srcArr);
                    arr.add(obj);
                }
                rawRedunData.add(entry.getKey(), arr);
            }
            root.add("rawMaterialRedundancy", rawRedunData);

            JsonObject rawReplData = new JsonObject();
            for (var entry : rawMaterialReplacements.entrySet()) {
                JsonObject inner = new JsonObject();
                for (var e : entry.getValue().entrySet()) {
                    inner.addProperty(e.getKey(), e.getValue());
                }
                rawReplData.add(entry.getKey(), inner);
            }
            root.add("rawMaterialReplacements", rawReplData);

            JsonObject rawIgnData = new JsonObject();
            for (var entry : rawMaterialIgnored.entrySet()) {
                JsonArray arr = new JsonArray();
                for (String s : entry.getValue()) arr.add(s);
                rawIgnData.add(entry.getKey(), arr);
            }
            root.add("rawMaterialIgnored", rawIgnData);

            JsonObject rawSrcIgnData = new JsonObject();
            for (var entry : rawMaterialSourceIgnored.entrySet()) {
                JsonArray arr = new JsonArray();
                for (String s : entry.getValue()) arr.add(s);
                rawSrcIgnData.add(entry.getKey(), arr);
            }
            root.add("rawMaterialSourceIgnored", rawSrcIgnData);

            if (playerControlPlusUploaded != null) {
                root.addProperty("playerControlPlusUploaded", playerControlPlusUploaded);
            }
            root.addProperty("treeZoomPercent", RawMaterialScreen.getTreeZoomPercent());

            // screenSettings
            JsonObject screenSettings = new JsonObject();
            screenSettings.addProperty("hideNoMissing", persistedHideNoMissing);
            screenSettings.addProperty("allowPin", persistedAllowPin);
            screenSettings.addProperty("detailSortMode", persistedDetailSortMode);
            screenSettings.addProperty("projectSortMode", persistedProjectSortMode);
            screenSettings.addProperty("hudVisible", persistedHudVisible);
            screenSettings.addProperty("hudFontSize", persistedHudFontSize);
            screenSettings.addProperty("hudBgAlpha", persistedHudBgAlpha);
            screenSettings.addProperty("hudMaxLines", persistedHudMaxLines);
            screenSettings.addProperty("hudShowBox", persistedHudShowBox);
            screenSettings.addProperty("hudShowGroup", persistedHudShowGroup);
            screenSettings.addProperty("hudX", persistedHudX);
            screenSettings.addProperty("hudY", persistedHudY);
            root.add("screenSettings", screenSettings);

            // 确保目录存在
            java.nio.file.Files.createDirectories(persistenceFile.getParent());
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(persistenceFile.toFile()), java.nio.charset.StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            LitematListMod.LOGGER.info("已保存持久化数据 ({} 个条目)", entriesArr.size());
        } catch (Exception e) {
            LitematListMod.LOGGER.error("保存持久化数据失败", e);
        }
    }

    /** 获取替换物品（如果有），否则返回原物品 */
    public static Item getReplacement(Path filePath, Item original) {
        ensurePersistenceLoaded();
        String pathKey = filePath.toString();
        Map<String, String> fileReplacements = replacements.get(pathKey);
        if (fileReplacements == null) return original;
        Identifier origId = BuiltInRegistries.ITEM.getKey(original);
        if (origId == null) return original;
        String replId = fileReplacements.get(origId.toString());
        if (replId == null) return original;
        var holder = BuiltInRegistries.ITEM.get(Identifier.parse(replId));
        return holder.isPresent() ? holder.get().value() : original;
    }

    /** 设置替换映射（供外部如 ProjectSummaryScreen 调用） */
    public static void setReplacement(Path filePath, Item original, Item replacement) {
        ensurePersistenceLoaded();
        String pathKey = filePath.toString();
        Map<String, String> fileReplacements = replacements.computeIfAbsent(pathKey, k -> new HashMap<>());
        Identifier origId = BuiltInRegistries.ITEM.getKey(original);
        Identifier newId = BuiltInRegistries.ITEM.getKey(replacement);
        if (origId != null && newId != null) {
            fileReplacements.put(origId.toString(), newId.toString());
        }
    }

    /** 移除替换映射（供外部如 ProjectSummaryScreen 调用） */
    public static void removeReplacement(Path filePath, Item original) {
        ensurePersistenceLoaded();
        String pathKey = filePath.toString();
        Map<String, String> fileReplacements = replacements.get(pathKey);
        if (fileReplacements != null) {
            Identifier origId = BuiltInRegistries.ITEM.getKey(original);
            if (origId != null) {
                fileReplacements.remove(origId.toString());
                if (fileReplacements.isEmpty()) {
                    replacements.remove(pathKey);
                }
            }
        }
    }

    /** 供外部调用，初始化持久化 */
    public static void initPersistence() {
        ensurePersistenceLoaded();
    }

    /** 清理指定路径的持久化数据（替换映射和忽略物品） */
    public static void cleanupPathData(Path filePath) {
        ensurePersistenceLoaded();
        String pathKey = filePath.toString();
        replacements.remove(pathKey);
        ignoredItems.remove(filePath);
        pinnedItems.remove(filePath);
        importedMaterials.remove(filePath);
        LitematListMod.LOGGER.info("已清理路径持久化数据: {}", pathKey);
    }

    // ---- 替换模式 ----
    private int replacingRow = -1; // 当前处于替换模式的行索引

    // ---- 隐藏不缺材料 ----
    private boolean hideNoMissing = persistedHideNoMissing;
    // 隐藏不缺材料按钮位置（用于自定义渲染开/关颜色）
    private int hideBtnX, hideBtnY, hideBtnW;
    private String hideBtnLabel = "";
    // ---- 手动重排 ----
    private static final Map<Path, List<Item>> reorderedItems = new HashMap<>();
    private int draggedRow = -1;
    private int dragStartY = 0;
    private boolean isDragging = false;

    // ---- 列位置 ----
    private int colTotalX;
    private int colHaveX;
    private int colMissX;

    // ---- 排序 ----
    private SortMode sortMode = SortMode.NONE;

    private void initSortMode() {
        if (persistedDetailSortMode != null && !persistedDetailSortMode.equals("NONE")) {
            try {
                sortMode = SortMode.valueOf(persistedDetailSortMode);
            } catch (IllegalArgumentException e) {
                sortMode = SortMode.NONE;
            }
        }
    }

    private enum SortMode {
        NONE(""),
        TOTAL_DESC("▼"),
        TOTAL_ASC("▲"),
        HAVE_DESC("▼"),
        HAVE_ASC("▲"),
        MISS_DESC("▼"),
        MISS_ASC("▲");

        final String arrow;
        SortMode(String arrow) { this.arrow = arrow; }
    }

    // ---- 按钮布局 ----
    private static final int BTN_REPLACE_W = 52;
    private static final int BTN_IGNORE_W = 52;
    private static final int BTN_CANCEL_W = 52;
    private static final int BTN_GAP = 2;
    private static final int BTN_RIGHT_MARGIN = 12;

    // ---- 标记材料 ----
    private static final Identifier PIN_EMPTY = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/pin/empty.png");
    private static final Identifier PIN_FAVORITE = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/pin/favorite.png");
    private static final Identifier PIN_HOVERED_EMPTY = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/pin/hovered_empty.png");
    private static final Identifier PIN_HOVERED_FAVORITE = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/pin/hovered_favorite.png");
    private static final Identifier PIN_FOCUSED = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/pin/focused_outline.png");
    private static final Identifier SEARCH_ICON = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/search.png");

    private boolean allowPin = persistedAllowPin;
    private int pinBtnX, pinBtnY, pinBtnW;
    private String pinBtnLabel = "";
    private static final Map<Path, Set<Item>> pinnedItems = new HashMap<>();
    private Set<Item> currentPinned;

    // ---- 搜索 ----
    private boolean searchActive = false;
    private String searchText = "";
    private EditBox searchField;
    private boolean fromUploadArea = false; // 是否从上传区域进入（禁用替换/忽略）

    // 按钮区域左边界
    private int buttonAreaStart;

    public MaterialDetailScreen(GuiBase parent, String schematicName, Path filePath) {
        this(parent, schematicName, filePath, false);
    }

    public MaterialDetailScreen(GuiBase parent, String schematicName, Path filePath, boolean fromUploadArea) {
        super();
        this.parent = parent;
        this.schematicName = schematicName;
        this.filePath = filePath;
        this.fromUploadArea = fromUploadArea;
        this.title = I18n.tr("litematlist.title.material_detail", schematicName);
        this.currentIgnored = ignoredItems.computeIfAbsent(filePath, k -> new HashSet<>());
        this.currentPinned = pinnedItems.computeIfAbsent(filePath, k -> new HashSet<>());
        initSortMode();
    }

    /** 当前条目是否已上传到上传区域 */
    private boolean isEntryUploaded() {
        MaterialListScreen.LoadedEntry uploaded = MaterialListScreen.getUploadedEntry();
        return uploaded != null && uploaded.filePath() != null && uploaded.filePath().equals(filePath);
    }

    /** 如果当前条目已上传，立即重新注入材料列表 */
    private void reinjectIfUploaded() {
        MaterialListScreen.LoadedEntry uploaded = MaterialListScreen.getUploadedEntry();
        if (uploaded != null && uploaded.filePath() != null && uploaded.filePath().equals(filePath)) {
            MaterialListInjector.injectFromEntry(uploaded);
            MaterialListInjector.markInjected(uploaded);
            LitematListMod.LOGGER.info("[MaterialDetailScreen] 已上传条目操作后重新注入: {}", filePath);
        }
    }

    /**
     * 全局解析设置（如「是否计算容器数据」）切换后重新加载材料并刷新界面。
     */
    public void refreshAfterGlobalSettingChange() {
        this.loaded = false;
        initGui();
    }

    @Override
    public void initGui() {
        super.initGui();
        this.replacingRow = -1; // 每次重建界面时清除选择模式
        this.visibleRows = (this.height - 100) / ROW_HEIGHT;

        if (!loaded) {
            loadMaterials();
            loaded = true;
        }
        calculateColumnPositions();

        this.buttonAreaStart = this.width - BTN_RIGHT_MARGIN - BTN_IGNORE_W - BTN_GAP - BTN_REPLACE_W - BTN_GAP - BTN_CANCEL_W - BTN_GAP;

        int buttonY = this.height - 38;

        ButtonGeneric backButton = new ButtonGeneric(this.width / 2 - 60, buttonY, 120, 20, I18n.tr("litematlist.button.back"));
        backButton.setRenderDefaultBackground(true);
        this.addButton(backButton, (IButtonActionListener) (btn, mb) ->
                Minecraft.getInstance().setScreenAndShow(parent));

        int ignoredCount = currentIgnored.size();
        String ignoredLabel = ignoredCount > 0 ? I18n.tr("litematlist.button.ignored_count", ignoredCount) : I18n.tr("litematlist.button.ignored");
        int ignoredBtnW = Math.max(60, this.font.width(ignoredLabel) + 16);

        // 隐藏不缺材料按钮 —— 放在"物品"表头上方
        String hideLabel = hideNoMissing ? I18n.tr("litematlist.button.hide_no_missing_on") : I18n.tr("litematlist.button.hide_no_missing_off");
        int hideBtnW = Math.max(60, this.font.width(hideLabel) + 16);
        int hideBtnX = 20;
        int hideBtnY = 20;
        ButtonGeneric hideBtn = new ButtonGeneric(hideBtnX, hideBtnY, hideBtnW, 20, "");
        hideBtn.setRenderDefaultBackground(true);
        this.addButton(hideBtn, (IButtonActionListener) (b, mb) -> {
            hideNoMissing = !hideNoMissing;
            persistedHideNoMissing = hideNoMissing;
            savePersistence();
            initGui();
        });
        // 保存按钮位置用于自定义渲染
        this.hideBtnX = hideBtnX;
        this.hideBtnY = hideBtnY;
        this.hideBtnW = hideBtnW;
        this.hideBtnLabel = hideLabel;

        // 允许标记材料按钮
        String pinLabel = allowPin ? I18n.tr("litematlist.button.allow_pin_on") : I18n.tr("litematlist.button.allow_pin_off");
        int pinBtnW = Math.max(60, this.font.width(pinLabel) + 16);
        int pinBtnX = hideBtnX + hideBtnW + 4;
        int pinBtnY = 20;
        ButtonGeneric pinBtn = new ButtonGeneric(pinBtnX, pinBtnY, pinBtnW, 20, "");
        pinBtn.setRenderDefaultBackground(true);
        this.addButton(pinBtn, (IButtonActionListener) (b, mb) -> {
            allowPin = !allowPin;
            persistedAllowPin = allowPin;
            savePersistence();
            initGui();
        });
        this.pinBtnX = pinBtnX;
        this.pinBtnY = pinBtnY;
        this.pinBtnW = pinBtnW;
        this.pinBtnLabel = pinLabel;

        // 导出按钮
        int exportBtnX = pinBtnX + pinBtnW + 4;
        ButtonGeneric exportTxtBtn = new ButtonGeneric(exportBtnX, 20, 70, 20, I18n.tr("litematlist.button.export_txt"));
        exportTxtBtn.setRenderDefaultBackground(true);
        this.addButton(exportTxtBtn, (IButtonActionListener) (b, mb) -> exportMaterials("txt"));

        ButtonGeneric exportJsonBtn = new ButtonGeneric(exportBtnX + 74, 20, 70, 20, I18n.tr("litematlist.button.export_json"));
        exportJsonBtn.setRenderDefaultBackground(true);
        this.addButton(exportJsonBtn, (IButtonActionListener) (b, mb) -> exportMaterials("json"));

        ButtonGeneric exportCsvBtn = new ButtonGeneric(exportBtnX + 148, 20, 70, 20, I18n.tr("litematlist.button.export_csv"));
        exportCsvBtn.setRenderDefaultBackground(true);
        this.addButton(exportCsvBtn, (IButtonActionListener) (b, mb) -> exportMaterials("csv"));

        // 搜索框
        int searchIconX = allowPin ? 33 : 15;
        if (searchField == null) {
            this.searchField = new EditBox(this.font, searchIconX + 18, HEADER_Y, 80, 14, Component.empty());
            this.searchField.setMaxLength(50);
            this.searchField.setBordered(false);
            this.searchField.setValue(searchText);
            this.addRenderableWidget(this.searchField);
        } else {
            this.searchField.setX(searchIconX + 18);
            this.searchField.setY(HEADER_Y);
            this.searchField.setValue(searchText);
            this.addRenderableWidget(this.searchField);
        }

        ButtonGeneric ignoredBtn = new ButtonGeneric(
                this.width - ignoredBtnW - 12, 10, ignoredBtnW, 20, ignoredLabel);
        ignoredBtn.setRenderDefaultBackground(true);
        this.addButton(ignoredBtn, (IButtonActionListener) (b, mb) -> {
            Map<Item, Integer> ignoredTotals = new HashMap<>();
            for (MaterialRow row : rows) {
                if (currentIgnored.contains(row.item)) {
                    ignoredTotals.put(row.item, row.totalCount);
                }
            }
            Minecraft.getInstance().setScreenAndShow(
                    new IgnoredItemsScreen(this, schematicName, filePath, currentIgnored, ignoredTotals));
        });

        createRowButtons();
    }

    private void calculateColumnPositions() {
        int maxTotalWidth = this.font.width(I18n.tr("litematlist.header.total"));
        int maxHaveWidth = this.font.width(I18n.tr("litematlist.header.have"));
        int maxMissWidth = this.font.width(I18n.tr("litematlist.header.missing"));
        int maxNameWidth = this.font.width(I18n.tr("litematlist.header.item"));

        for (MaterialRow row : rows) {
            maxNameWidth = Math.max(maxNameWidth, this.font.width(row.name));
            maxTotalWidth = Math.max(maxTotalWidth, this.font.width(String.valueOf(row.totalCount)));
        }

        int nameEndX = 35 + maxNameWidth + 20;
        colTotalX = nameEndX + 24;
        colHaveX = colTotalX + maxTotalWidth + 24;
        colMissX = colHaveX + maxHaveWidth + 24;
    }

    private void loadMaterials() {
        rows.clear();
        ensurePersistenceLoaded();
        try {
            // 优先从导入缓存加载（txt 文件导入的材料）
            List<LitematicReader.MaterialEntry> entries;
            if (importedMaterials.containsKey(filePath)) {
                entries = importedMaterials.get(filePath);
                LitematListMod.LOGGER.info("从导入缓存加载材料: {} ({} 种)", filePath, entries.size());
            } else if (filePath.toString().toLowerCase().endsWith(".txt") || filePath.toString().toLowerCase().endsWith(".json") || filePath.toString().toLowerCase().endsWith(".csv")) {
                // txt/json 文件：重启后缓存已丢失，重新解析
                LitematListMod.LOGGER.info("文件重新解析: {}", filePath);
                MaterialListImporter.ImportResult result = MaterialListImporter.importFromFile(filePath);
                if (result != null) {
                    entries = result.materials();
                    importedMaterials.put(filePath, entries); // 重新缓存
                } else {
                    LitematListMod.LOGGER.error("txt 文件重新解析失败: {}", filePath);
                    return;
                }
            } else if (SyncmaticaBridge.isSyncPath(filePath)) {
                // 「在Syncmatica认领的材料」：每次打开都向 Syncmatica 重新拉取最新认领数据
                entries = SyncmaticaBridge.loadClaimedMaterialEntries();
                LitematListMod.LOGGER.info("从 Syncmatica 拉取认领材料: {} 种", entries.size());
            } else {
                entries = loadLitematicEntries();
            }
            for (LitematicReader.MaterialEntry entry : entries) {
                Item item = entry.item();
                // 应用已保存的替换
                Item replaced = getReplacement(filePath, item);
                rows.add(new MaterialRow(
                        new ItemStack(replaced, entry.totalCount()),
                        replaced.getName(new ItemStack(replaced)).getString(),
                        entry.totalCount(),
                        replaced,
                        item, // originalItem 始终是原理图原始物品
                        entry.missingCount()
                ));
            }
            LitematListMod.LOGGER.info("加载了 {} 种材料（已忽略 {} 种）", rows.size(), currentIgnored.size());
        } catch (Exception e) {
            LitematListMod.LOGGER.error("解析材料列表失败: {}", filePath, e);
        }
    }

    /**
     * 加载 .litematic 材料：
     * 本条目来自「从投影同步」且开启「同步投影渲染层」时，按投影当前渲染层规则过滤解析；
     * 解析完成后联动刷新上传区域数据与原材料表。
     */
    private List<LitematicReader.MaterialEntry> loadLitematicEntries() {
        LitematicaBridge.LayerFilter filter = null;
        boolean syncLayer = isSyncLayerEntry() && Configs.FeatureToggles.SYNC_RENDER_LAYER.getBooleanValue();
        if (syncLayer) {
            filter = LitematicaBridge.computeRenderLayerFilter(filePath);
            if (filter != null) {
                LitematListMod.LOGGER.info("[MaterialDetailScreen] 同步投影渲染层: 轴={} 相对层级区间 [{}..{}]: {}",
                        filter.axis(), filter.min(), filter.max(), filePath);
            }
        }
        List<LitematicReader.MaterialEntry> list = LitematicReader.loadMaterialList(filePath, filter);

        if (syncLayer) {
            // 上传状态条目：立即更新上传区域数据（重新注入 DataManager + HUD）
            // 原材料列表：解析完成后联动重新分析，无需玩家打开原材料界面
            try {
                reinjectIfUploaded();
                MaterialListHudRenderer.forceRefreshHud();
                autoRefreshRawMaterials();
            } catch (Exception e) {
                LitematListMod.LOGGER.error("[MaterialDetailScreen] 同步投影渲染层联动刷新失败（已捕获，不影响游戏运行）", e);
            }
        }
        return list;
    }

    /** 判断当前条目是否来自「从投影同步」 */
    private boolean isSyncLayerEntry() {
        for (MaterialListScreen.LoadedEntry e : MaterialListScreen.getLoadedEntries()) {
            if (e.fromLitematica() && e.filePath() != null && e.filePath().equals(filePath)) {
                return true;
            }
        }
        return false;
    }

    /** 按新渲染层规则解析完成后，自动重新分析原材料表与冗余数据 */
    private void autoRefreshRawMaterials() {
        List<LitematicReader.MaterialEntry> materials = MaterialListInjector.loadMaterialsCached(filePath);
        if (materials == null || materials.isEmpty()) {
            return;
        }
        RawMaterialAnalyzer rawAnalyzer = new RawMaterialAnalyzer();
        RawMaterialAnalyzer.RawMaterialResult rawResult = rawAnalyzer.analyze(materials);
        String pathKey = filePath.toString();
        rawMaterials.put(pathKey, rawResult.rawMaterials());
        rawMaterialRedundancy.put(pathKey, rawResult.redundancy());
        savePersistence();
        LitematListMod.LOGGER.info("[MaterialDetailScreen] 已按渲染层规则更新原材料表: {} ({} 项原材料)",
                filePath, rawResult.rawMaterials().size());
    }

    /**
     * 「同步投影渲染层」替换内容快照。
     * 开关 关→开 时保存（此后玩家可正常替换）；开→关 时恢复，丢弃开启期间的替换更改。
     */
    private record ReplacementSnapshot(Map<String, Map<String, String>> detail, Map<String, Map<String, String>> raw) {}

    private static ReplacementSnapshot replacementSnapshot = null;

    /** 保存所有「从投影同步」条目的替换内容快照（开关 关→开 时调用） */
    public static void snapshotReplacementsForLitematicaEntries() {
        ensurePersistenceLoaded();
        Map<String, Map<String, String>> detail = new HashMap<>();
        Map<String, Map<String, String>> raw = new HashMap<>();
        for (MaterialListScreen.LoadedEntry e : MaterialListScreen.getLoadedEntries()) {
            if (!e.fromLitematica() || e.filePath() == null) continue;
            String key = e.filePath().toString();
            Map<String, String> d = replacements.get(key);
            if (d != null) detail.put(key, new HashMap<>(d));
            Map<String, String> r = rawMaterialReplacements.get(key);
            if (r != null) raw.put(key, new HashMap<>(r));
        }
        replacementSnapshot = new ReplacementSnapshot(detail, raw);
        LitematListMod.LOGGER.info("[MaterialDetailScreen] 已保存「同步投影渲染层」开启前的替换内容快照 ({} 个条目)", detail.size());
    }

    /** 恢复「从投影同步」条目的替换内容快照，丢弃开启期间的替换更改（开关 开→关 时调用） */
    public static void restoreReplacementsSnapshot() {
        if (replacementSnapshot == null) {
            return;
        }
        ensurePersistenceLoaded();
        for (MaterialListScreen.LoadedEntry e : MaterialListScreen.getLoadedEntries()) {
            if (!e.fromLitematica() || e.filePath() == null) continue;
            String key = e.filePath().toString();
            Map<String, String> d = replacementSnapshot.detail().get(key);
            if (d != null) {
                replacements.put(key, new HashMap<>(d));
            } else {
                replacements.remove(key);
            }
            Map<String, String> r = replacementSnapshot.raw().get(key);
            if (r != null) {
                rawMaterialReplacements.put(key, new HashMap<>(r));
            } else {
                rawMaterialReplacements.remove(key);
            }
        }
        replacementSnapshot = null;
        savePersistence();
        LitematListMod.LOGGER.info("[MaterialDetailScreen] 已恢复「同步投影渲染层」开启前的替换内容");
    }

    private void createRowButtons() {
        List<MaterialRow> displayRows = getSortedRows();
        int maxIdx = Math.min(displayRows.size(), scrollOffset + visibleRows);

        for (int i = scrollOffset; i < maxIdx; i++) {
            MaterialRow row = displayRows.get(i);
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;
            final int visibleIdx = i;

            int ignoreX = this.width - BTN_RIGHT_MARGIN - BTN_IGNORE_W;
            int replaceX = ignoreX - BTN_GAP - BTN_REPLACE_W;
            int cancelX = replaceX - BTN_GAP - BTN_CANCEL_W;

            final MaterialRow targetRow = row;

            // 取消替换按钮（物品已被替换时显示，可恢复为原理图原始物品）
            if (row.item != row.originalItem) {
                ButtonGeneric cancelBtn = new ButtonGeneric(cancelX, rowY + 1, BTN_CANCEL_W, 20, I18n.tr("litematlist.button.cancel_replace"));
                cancelBtn.setRenderDefaultBackground(true);
                this.addButton(cancelBtn, (IButtonActionListener) (b, mb) -> {
                    restoreOriginalItem(targetRow);
                    initGui();
                });
            }

            // 替换按钮
            {
                ButtonGeneric replaceBtn = new ButtonGeneric(replaceX, rowY + 1, BTN_REPLACE_W, 20, I18n.tr("litematlist.button.replace"));
                replaceBtn.setRenderDefaultBackground(true);
                this.addButton(replaceBtn, (IButtonActionListener) (b, mb) -> {
                    replacingRow = visibleIdx;
                    Minecraft.getInstance().setScreenAndShow(
                            new BlockPickerScreen(this, newItem -> replaceItem(targetRow, newItem)));
                });
            }

            // 忽略按钮（置顶材料不显示忽略按钮）
            if (!(allowPin && currentPinned.contains(row.originalItem))) {
                ButtonGeneric ignoreBtn = new ButtonGeneric(ignoreX, rowY + 1, BTN_IGNORE_W, 20, I18n.tr("litematlist.button.ignore"));
                ignoreBtn.setRenderDefaultBackground(true);
                this.addButton(ignoreBtn, (IButtonActionListener) (b, mb) -> {
                    ignoreItem(visibleIdx);
                    initGui();
                });
            }
        }
    }

    private void ignoreItem(int visibleIdx) {
        replacingRow = -1; // 点击忽略时取消替换模式
        List<MaterialRow> displayRows = getSortedRows();
        if (visibleIdx >= 0 && visibleIdx < displayRows.size()) {
            MaterialRow row = displayRows.get(visibleIdx);
            if (allowPin && currentPinned.contains(row.originalItem)) return;
            currentIgnored.add(row.item);
            // 清除缓存的原材料分析数据，下一次打开原材料列表时重新分析（排除已忽略物品）
            String pathKey = filePath.toString();
            rawMaterials.remove(pathKey);
            rawMaterialRedundancy.remove(pathKey);
            savePersistence(); // 立即持久化忽略
            MaterialListHudRenderer.refreshHud();
            reinjectIfUploaded(); // 上传条目需立即重新注入
            LitematListMod.LOGGER.info("忽略物品: {} ({})", row.name, row.item);
        }
    }

    private List<MaterialRow> getVisibleRowsOnly() {
        List<MaterialRow> visible = new ArrayList<>();
        for (MaterialRow row : rows) {
            if (!currentIgnored.contains(row.item)) {
                if (hideNoMissing) {
                    int haveCount = getPlayerItemCount(row.item);
                    if (row.totalCount - haveCount <= 0) continue;
                }
                // 搜索过滤（中文名/拼音全拼/拼音首字母/英文ID）
                if (searchActive && !searchText.isEmpty()) {
                    String itemId = BuiltInRegistries.ITEM.getKey(row.item).toString();
                    if (!PinyinSearch.matches(searchText, row.name, itemId)) continue;
                }
                visible.add(row);
            }
        }
        // 应用手动重排顺序
        List<Item> reorder = reorderedItems.get(filePath);
        if (reorder != null && !reorder.isEmpty()) {
            Map<Item, MaterialRow> rowMap = new HashMap<>();
            for (MaterialRow row : visible) {
                rowMap.put(row.originalItem, row);
            }
            List<MaterialRow> ordered = new ArrayList<>();
            for (Item item : reorder) {
                MaterialRow row = rowMap.get(item);
                if (row != null) {
                    ordered.add(row);
                    rowMap.remove(item);
                }
            }
            ordered.addAll(rowMap.values());
            visible = ordered;
        }
        // 标记材料模式：将已标记的材料排到最前面
        if (allowPin && !currentPinned.isEmpty()) {
            List<MaterialRow> pinned = new ArrayList<>();
            List<MaterialRow> unpinned = new ArrayList<>();
            for (MaterialRow row : visible) {
                if (currentPinned.contains(row.originalItem)) {
                    pinned.add(row);
                } else {
                    unpinned.add(row);
                }
            }
            pinned.addAll(unpinned);
            visible = pinned;
        }
        return visible;
    }

    private int getPlayerItemCount(Item item) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return 0;
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() == item) {
                count += stack.getCount();
            } else if (InventoryUtils.shulkerBoxHasItems(stack)) {
                for (ItemStack boxStack : InventoryUtils.getStoredItems(stack)) {
                    if (boxStack.getItem() == item) {
                        count += boxStack.getCount();
                    }
                }
            }
        }
        return count;
    }

    private String formatCountTooltip(int count) {
        int shulker = count / 1728;
        int remainder = count % 1728;
        int stacks = remainder / 64;
        int items = remainder % 64;
        double shulkerFloat = (double) count / 1728.0;
        return String.format("%d = %d×1728 + %d×64 + %d  |  %d×64+%d  |  %.2f 潜影盒",
                count, shulker, stacks, items,
                shulker * 27 + stacks, items,
                shulkerFloat);
    }

    private List<MaterialRow> getSortedRows() {
        List<MaterialRow> visible = getVisibleRowsOnly();

        // 分离置顶和非置顶
        if (allowPin && !currentPinned.isEmpty()) {
            List<MaterialRow> pinned = new ArrayList<>();
            List<MaterialRow> unpinned = new ArrayList<>();
            for (MaterialRow row : visible) {
                if (currentPinned.contains(row.originalItem)) {
                    pinned.add(row);
                } else {
                    unpinned.add(row);
                }
            }
            pinned.addAll(unpinned);
            return pinned;
        }

        return visible;
    }

    private List<MaterialRow> sortRowList(List<MaterialRow> list) {
        List<SortableRow> sortable = new ArrayList<>();
        for (MaterialRow row : list) {
            int haveCount = getPlayerItemCount(row.item);
            sortable.add(new SortableRow(row, haveCount, Math.max(0, row.totalCount - haveCount)));
        }

        Comparator<SortableRow> cmp = switch (sortMode) {
            case TOTAL_DESC -> Comparator.comparingInt((SortableRow r) -> r.row.totalCount).reversed();
            case TOTAL_ASC -> Comparator.comparingInt(r -> r.row.totalCount);
            case HAVE_DESC -> Comparator.comparingInt((SortableRow r) -> r.haveCount).reversed();
            case HAVE_ASC -> Comparator.comparingInt(r -> r.haveCount);
            case MISS_DESC -> Comparator.comparingInt((SortableRow r) -> r.missCount).reversed();
            case MISS_ASC -> Comparator.comparingInt(r -> r.missCount);
            default -> null;
        };

        if (cmp == null) return list;
        sortable.sort(cmp);
        return sortable.stream().map(r -> r.row).toList();
    }

    private record SortableRow(MaterialRow row, int haveCount, int missCount) {}

    private void handleHeaderClick(double mouseX, double mouseY) {
        if (mouseY < HEADER_Y || mouseY > HEADER_Y + HEADER_HEIGHT) return;

        SortMode newMode;
        if (mouseX >= colTotalX - 20 && mouseX <= colTotalX + 30) {
            newMode = (sortMode == SortMode.TOTAL_DESC) ? SortMode.TOTAL_ASC
                     : (sortMode == SortMode.TOTAL_ASC) ? SortMode.NONE
                     : SortMode.TOTAL_DESC;
        } else if (mouseX >= colHaveX - 20 && mouseX <= colHaveX + 30) {
            newMode = (sortMode == SortMode.HAVE_DESC) ? SortMode.HAVE_ASC
                     : (sortMode == SortMode.HAVE_ASC) ? SortMode.NONE
                     : SortMode.HAVE_DESC;
        } else if (mouseX >= colMissX - 20 && mouseX <= colMissX + 30) {
            newMode = (sortMode == SortMode.MISS_DESC) ? SortMode.MISS_ASC
                     : (sortMode == SortMode.MISS_ASC) ? SortMode.NONE
                     : SortMode.MISS_DESC;
        } else return;
        sortMode = newMode;
        persistedDetailSortMode = sortMode.name();
        savePersistence();
        if (newMode != SortMode.NONE) {
            // 一次性排序
            Comparator<MaterialRow> cmp = switch (newMode) {
                case TOTAL_DESC -> Comparator.comparingInt((MaterialRow r) -> r.totalCount).reversed();
                case TOTAL_ASC -> Comparator.comparingInt(r -> r.totalCount);
                case HAVE_DESC -> Comparator.comparingInt((MaterialRow r) -> r.totalCount - r.missingCount).reversed();
                case HAVE_ASC -> Comparator.comparingInt(r -> r.totalCount - r.missingCount);
                case MISS_DESC -> Comparator.comparingInt((MaterialRow r) -> r.missingCount).reversed();
                case MISS_ASC -> Comparator.comparingInt(r -> r.missingCount);
                default -> null;
            };
            if (cmp != null) rows.sort(cmp);
        }
        initGui();
    }

    // ==================== 替换逻辑 ====================

    private void replaceItem(MaterialRow target, Item newItem) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).item == target.item) {
                MaterialRow old = rows.get(i);
                rows.set(i, new MaterialRow(
                        new ItemStack(newItem, old.totalCount),
                        newItem.getName(new ItemStack(newItem)).getString(),
                        old.totalCount,
                        newItem,
                        old.originalItem, // 保留原始物品
                        old.missingCount
                ));
                break;
            }
        }
        // 立即持久化替换
        String pathKey = filePath.toString();
        Map<String, String> fileReplacements = replacements.computeIfAbsent(pathKey, k -> new HashMap<>());
        Identifier origId = BuiltInRegistries.ITEM.getKey(target.originalItem);
        Identifier newId = BuiltInRegistries.ITEM.getKey(newItem);
        if (origId != null && newId != null) {
            fileReplacements.put(origId.toString(), newId.toString());
        }
        savePersistence();
            // 清除缓存的原材料分析数据，确保下次打开原材料列表时重新分析
            rawMaterials.remove(pathKey);
            rawMaterialRedundancy.remove(pathKey);
        MaterialListHudRenderer.refreshHud();
        reinjectIfUploaded();
        initGui();
    }

    private void restoreOriginalItem(MaterialRow target) {
        for (int i = 0; i < rows.size(); i++) {
            // 通过 originalItem 匹配：替换后 target.item 是旧物品，rows[i].item 是新物品，不能直接 == 比较
            if (rows.get(i).originalItem == target.originalItem) {
                MaterialRow old = rows.get(i);
                Item orig = old.originalItem;
                rows.set(i, new MaterialRow(
                        new ItemStack(orig, old.totalCount),
                        orig.getName(new ItemStack(orig)).getString(),
                        old.totalCount,
                        orig,
                        orig,
                        old.missingCount
                ));
                break;
            }
        }
        // 从持久化中移除替换
        String pathKey = filePath.toString();
        Map<String, String> fileReplacements = replacements.get(pathKey);
        if (fileReplacements != null) {
            Identifier origId = BuiltInRegistries.ITEM.getKey(target.originalItem);
            if (origId != null) {
                fileReplacements.remove(origId.toString());
                if (fileReplacements.isEmpty()) {
                    replacements.remove(pathKey);
                }
            }
        }
        savePersistence();
            // 清除缓存的原材料分析数据，确保下次打开原材料列表时重新分析
            rawMaterials.remove(pathKey);
            rawMaterialRedundancy.remove(pathKey);
        MaterialListHudRenderer.refreshHud();
        reinjectIfUploaded();
        replacingRow = -1;
    }

    // ==================== 导出 ====================

    private void exportMaterials(String format) {
        try {
            Path mcRoot = Minecraft.getInstance().gameDirectory.toPath().resolve("litematlist");
            Path dir;
            // 判断是否来自项目文件夹
            if (this.filePath != null && this.filePath.getParent() != null && this.filePath.getParent().getParent() != null) {
                Path grandParent = this.filePath.getParent().getParent();
                if ("project".equals(grandParent.getFileName().toString())) {
                    // 项目文件夹导出: .minecraft/litematlist/<project>/
                    String projectName = this.filePath.getParent().getFileName().toString();
                    dir = mcRoot.resolve(projectName);
                } else {
                    dir = mcRoot;
                }
            } else {
                dir = mcRoot;
            }
            Files.createDirectories(dir);
            String dateStr = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date());
            String safeName = schematicName.replaceAll("[\\\\/:*?\"<>|]", "_");
            String fileName = safeName + "_" + dateStr + "." + format;
            Path filePath = dir.resolve(fileName);

            if ("txt".equals(format)) {
                exportTxt(filePath);
            } else if ("csv".equals(format)) {
                exportCsv(filePath);
            } else {
                exportJson(filePath);
            }
            LitematListMod.LOGGER.info("已导出材料列表: {}", filePath);

            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                String fmt = format.toUpperCase();
                Component link = Component.literal(fileName)
                        .withStyle(style -> style
                                .withUnderlined(true)
                                .withColor(0x55FFFF));
                Component msg = Component.literal("§a材料列表已导出为" + fmt + ": ").append(link);
                client.player.sendSystemMessage(msg);
            }
        } catch (Exception e) {
            LitematListMod.LOGGER.error("导出失败", e);
        }
    }

    private void exportTxt(Path filePath) throws IOException {
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(filePath.toFile()), StandardCharsets.UTF_8))) {
            String title = "原理图'" + schematicName + "'材料列表";
            w.write("+---------+-------+---------+-----------+");
            w.newLine();
            w.write("| " + padTitle(title, 43) + " |");
            w.newLine();
            w.write("+---------+-------+---------+-----------+");
            w.newLine();
            w.write("| Item    | Total | Missing | Available |");
            w.newLine();
            w.write("+---------+-------+---------+-----------+");
            w.newLine();

            for (MaterialRow row : rows) {
                if (currentIgnored.contains(row.item)) continue;
                int have = getPlayerItemCount(row.item);
                int missing = Math.max(0, row.totalCount - have);
                int available = Math.min(have, row.totalCount);
                String name = row.name;
                if (getDisplayWidth(name) > 9) name = truncateByWidth(name, 9);
                w.write(String.format("| %-9s | %5d | %7d | %9d |",
                        name, row.totalCount, missing, available));
                w.newLine();
            }
            w.write("+---------+-------+---------+-----------+");
            w.newLine();
        }
    }

    private static int getDisplayWidth(String s) {
        int w = 0;
        for (char c : s.toCharArray()) {
            w += (c >= 0x4E00 && c <= 0x9FFF) || (c >= 0x3000 && c <= 0x303F) || (c >= 0xFF00 && c <= 0xFFEF) ? 2 : 1;
        }
        return w;
    }

    private static String truncateByWidth(String s, int maxWidth) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            w += (c >= 0x4E00 && c <= 0x9FFF) || (c >= 0x3000 && c <= 0x303F) || (c >= 0xFF00 && c <= 0xFFEF) ? 2 : 1;
            if (w > maxWidth) return s.substring(0, i);
        }
        return s;
    }

    private static String padTitle(String title, int totalWidth) {
        int w = getDisplayWidth(title);
        if (w >= totalWidth) return title;
        StringBuilder sb = new StringBuilder(title);
        for (int i = 0; i < totalWidth - w; i++) sb.append(' ');
        return sb.toString();
    }

    private void exportJson(Path filePath) throws IOException {
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(filePath.toFile()), StandardCharsets.UTF_8))) {
            w.write("{");
            w.newLine();
            w.write("  \"Name\": \"" + schematicName + "\",");
            w.newLine();
            String title = "原理图'" + schematicName + "'材料列表";
            w.write("  \"Title\": \"" + title + "\",");
            w.newLine();
            w.write("  \"Multiplier\": 1,");
            w.newLine();
            String dateStr = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH).format(new Date());
            w.write("  \"Date\": \"" + dateStr + "\",");
            w.newLine();
            w.write("  \"Materials\": [");
            w.newLine();

            boolean first = true;
            for (MaterialRow row : rows) {
                if (currentIgnored.contains(row.item)) continue;
                if (!first) { w.write(","); w.newLine(); }
                first = false;
                Identifier id = BuiltInRegistries.ITEM.getKey(row.item);
                String itemId = id != null ? id.toString() : "unknown";
                int have = getPlayerItemCount(row.item);
                int missing = Math.max(0, row.totalCount - have);
                int available = Math.min(have, row.totalCount);
                w.write("    {");
                w.write("\"Item\": \"" + itemId + "\", ");
                w.write("\"Total\": " + row.totalCount + ", ");
                w.write("\"Missing\": " + missing + ", ");
                w.write("\"Mismatched\": 0, ");
                w.write("\"Available\": " + available);
                w.write("}");
            }
            w.newLine();
            w.write("  ]");
            w.newLine();
            w.write("}");
            w.newLine();
        }
    }

    private void exportCsv(Path filePath) throws IOException {
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(filePath.toFile()), StandardCharsets.UTF_8))) {
            w.write("\"Item\",\"Total\",\"Missing\",\"Available\"");
            w.newLine();
            for (MaterialRow row : rows) {
                if (currentIgnored.contains(row.item)) continue;
                int have = getPlayerItemCount(row.item);
                int missing = Math.max(0, row.totalCount - have);
                int available = Math.min(have, row.totalCount);
                w.write("\"" + row.name + "\"," + row.totalCount + "," + missing + "," + available);
                w.newLine();
            }
        }
    }

    // ==================== 渲染 ====================

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks) {
        GuiContext ctx = GuiContext.fromGuiGraphics(gfx);
        this.extractBackground(gfx, mouseX, mouseY, partialTicks);

        if (rows.isEmpty()) {
            ctx.drawCenteredString(
                    this.font, I18n.tr("litematlist.error.no_data"),
                    this.width / 2, this.height / 2 - 10, 0xFFFFFFFF);
            super.extractRenderState(gfx, mouseX, mouseY, partialTicks);
            return;
        }

        List<MaterialRow> displayRows = getSortedRows();

        int listRight = this.width - 8; // 背景框覆盖按钮区域
        int listBottom = LIST_TOP + visibleRows * ROW_HEIGHT;

        // ===== 第1层：背景（在按钮之前绘制，避免盖住按钮） =====
        ctx.fill(8, LIST_TOP - 2, listRight, listBottom + 2, 0x40000000);

        hoveredRow = -1;
        hoveredColumn = -1;
        int maxIdx = Math.min(displayRows.size(), scrollOffset + visibleRows);

        for (int i = scrollOffset; i < maxIdx; i++) {
            MaterialRow row = displayRows.get(i);
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;
            boolean isReplacing = (replacingRow == i);

            if (isReplacing) {
                ctx.fill(10, rowY, listRight, rowY + ROW_HEIGHT, 0x60FFFF00);
            } else if (mouseX >= 10 && mouseX <= listRight
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                hoveredRow = i;
                ctx.fill(10, rowY, listRight, rowY + ROW_HEIGHT, 0x40FFFFFF);

                int totalW = this.font.width(String.valueOf(row.totalCount));
                int haveW = this.font.width(String.valueOf(getPlayerItemCount(row.item)));
                int missW = this.font.width(String.valueOf(Math.max(0, row.totalCount - getPlayerItemCount(row.item))));

                if (mouseX >= colTotalX - 10 && mouseX <= colTotalX + totalW + 10) {
                    hoveredColumn = 0;
                } else if (mouseX >= colHaveX - 10 && mouseX <= colHaveX + haveW + 10) {
                    hoveredColumn = 1;
                } else if (mouseX >= colMissX - 10 && mouseX <= colMissX + missW + 10) {
                    hoveredColumn = 2;
                }
            } else if ((i - scrollOffset) % 2 == 0) {
                ctx.fill(10, rowY, listRight, rowY + ROW_HEIGHT, 0x10FFFFFF);
            }
        }

        // 表头背景
        ctx.fill(8, HEADER_Y, listRight, HEADER_Y + HEADER_HEIGHT, 0x60000000);

        int sortColX = switch (sortMode) {
            case TOTAL_DESC, TOTAL_ASC -> colTotalX;
            case HAVE_DESC, HAVE_ASC -> colHaveX;
            case MISS_DESC, MISS_ASC -> colMissX;
            default -> -1;
        };
        if (sortColX > 0) {
            ctx.fill(sortColX - 20, HEADER_Y, sortColX + 30, HEADER_Y + HEADER_HEIGHT, 0x40FFFF88);
        }

        // ===== 第2层：按钮（在背景之上） =====
        super.extractRenderState(gfx, mouseX, mouseY, partialTicks);

        // 自定义渲染隐藏按钮文字（开=绿色，关=红色）
        if (!hideBtnLabel.isEmpty()) {
            int colonIdx = hideBtnLabel.indexOf("：");
            if (colonIdx < 0) colonIdx = hideBtnLabel.indexOf(":");
            if (colonIdx > 0) {
                String prefix = hideBtnLabel.substring(0, colonIdx + 1);
                String status = hideBtnLabel.substring(colonIdx + 1);
                int prefixW = this.font.width(prefix);
                int textX = hideBtnX + 4;
                int textY = hideBtnY + 5;
                ctx.drawString(this.font, prefix, textX, textY, 0xFFFFFFFF);
                int statusColor = hideNoMissing ? 0xFF55FF55 : 0xFFFF5555;
                ctx.drawString(this.font, status, textX + prefixW, textY, statusColor);
            }
        }
        // 自定义渲染标记按钮文字（开=绿色，关=红色）
        if (!pinBtnLabel.isEmpty()) {
            int colonIdx = pinBtnLabel.indexOf("：");
            if (colonIdx < 0) colonIdx = pinBtnLabel.indexOf(":");
            if (colonIdx > 0) {
                String prefix = pinBtnLabel.substring(0, colonIdx + 1);
                String status = pinBtnLabel.substring(colonIdx + 1);
                int prefixW = this.font.width(prefix);
                int textX = pinBtnX + 4;
                int textY = pinBtnY + 5;
                ctx.drawString(this.font, prefix, textX, textY, 0xFFFFFFFF);
                int statusColor = allowPin ? 0xFF55FF55 : 0xFFFF5555;
                ctx.drawString(this.font, status, textX + prefixW, textY, statusColor);
            }
        }

        // ===== 第3层：文字和物品图标（在最上层） =====
        for (int i = scrollOffset; i < maxIdx; i++) {
            MaterialRow row = displayRows.get(i);
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;

            int iconX = 15;
            int nameX = 35;

            // 标记材料模式：先画标记图标，再偏移物品图标和文字
            if (allowPin) {
                boolean isPinned = currentPinned.contains(row.originalItem);
                boolean pinHovered = mouseX >= 15 && mouseX <= 31 && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
                Identifier pinTexture;
                if (isPinned) {
                    pinTexture = pinHovered ? PIN_HOVERED_FAVORITE : PIN_FAVORITE;
                } else {
                    pinTexture = pinHovered ? PIN_HOVERED_EMPTY : PIN_EMPTY;
                }
                drawTextureIcon(ctx, pinTexture, 15, rowY + 2, 16, 16);
                iconX = 33;
                nameX = 53;
            }

            ctx.renderItem(row.itemStack, iconX, rowY + 2);
            ctx.drawString(this.font, row.name, nameX, rowY + 5, 0xFFFFFFFF);
            ctx.drawString(this.font, String.valueOf(row.totalCount), colTotalX, rowY + 5, 0xFFFFFFFF);

            int haveCount = getPlayerItemCount(row.item);
            ctx.drawString(this.font, String.valueOf(haveCount), colHaveX, rowY + 5, 0xFFFFFFFF);

            int missCount = Math.max(0, row.totalCount - haveCount);
            int missColor = missCount == 0 ? 0xFF55FF55
                          : (haveCount >= missCount ? 0xFFFFAA00 : 0xFFFF5555);
            ctx.drawString(this.font, String.valueOf(missCount), colMissX, rowY + 5, missColor);
        }

        // 表头文字
        int headerNameX = allowPin ? 53 : 35;
        // 搜索图标
        int searchIconX = allowPin ? 33 : 15;
        drawTextureIcon(ctx, SEARCH_ICON, searchIconX, HEADER_Y + 1, 14, 14);
        // 先画"物品"文字（搜索框打开时覆盖在上面）
        ctx.drawString(this.font, I18n.tr("litematlist.header.item"), headerNameX, HEADER_Y + 3, 0xFFFFFFFF);
        if (searchActive && searchField != null) {
            ctx.fill(searchIconX + 18 - 2, HEADER_Y - 1, searchIconX + 18 + 82, HEADER_Y + 15, 0xFF000000);
            ctx.fill(searchIconX + 18, HEADER_Y, searchIconX + 18 + 80, HEADER_Y + 14, 0xFF222222);
            String st = searchField.getValue();
            ctx.drawString(this.font, st, searchIconX + 21, HEADER_Y + 3, 0xFFFFFFFF);
            if (searchField.isFocused() && System.currentTimeMillis() % 1000 < 500) {
                int cp = searchField.getCursorPosition();
                String bc = st.substring(0, Math.min(cp, st.length()));
                int cx = searchIconX + 21 + this.font.width(bc);
                ctx.fill(cx, HEADER_Y + 2, cx + 1, HEADER_Y + 12, 0xFFFFFFFF);
            }
        }
        ctx.drawString(this.font,
                I18n.tr("litematlist.header.total") + (sortMode == SortMode.TOTAL_DESC || sortMode == SortMode.TOTAL_ASC ? sortMode.arrow : ""),
                colTotalX, HEADER_Y + 3, 0xFFFFFFFF);
        ctx.drawString(this.font,
                I18n.tr("litematlist.header.have") + (sortMode == SortMode.HAVE_DESC || sortMode == SortMode.HAVE_ASC ? sortMode.arrow : ""),
                colHaveX, HEADER_Y + 3, 0xFFFFFFFF);
        ctx.drawString(this.font,
                I18n.tr("litematlist.header.missing") + (sortMode == SortMode.MISS_DESC || sortMode == SortMode.MISS_ASC ? sortMode.arrow : ""),
                colMissX, HEADER_Y + 3, 0xFFFFFFFF);

        // 仅在悬停数值列时显示量级 tooltip
        if (hoveredRow >= 0 && hoveredColumn >= 0 && hoveredRow < displayRows.size()) {
            MaterialRow hovered = displayRows.get(hoveredRow);
            int haveCount = getPlayerItemCount(hovered.item);
            int missCount = Math.max(0, hovered.totalCount - haveCount);

            int tooltipValue = switch (hoveredColumn) {
                case 0 -> hovered.totalCount;
                case 1 -> haveCount;
                case 2 -> missCount;
                default -> hovered.totalCount;
            };

            String tooltip = formatCountTooltip(tooltipValue);
            int tipW = this.font.width(tooltip) + 8;
            int tipX = mouseX + 12;
            int tipY = mouseY - 20;
            if (tipX + tipW > this.width) tipX = this.width - tipW - 4;
            if (tipY < 4) tipY = mouseY + 16;
            ctx.fill(tipX, tipY, tipX + tipW, tipY + 16, 0xCC000000);
            ctx.fill(tipX + 1, tipY + 1, tipX + tipW - 1, tipY + 15, 0xCC333333);
            ctx.drawString(this.font, tooltip, tipX + 4, tipY + 4, 0xFFFFFFFF);
        }

        if (displayRows.size() > visibleRows) {
            String scrollInfo = (scrollOffset + 1) + "-" +
                    Math.min(scrollOffset + visibleRows, displayRows.size()) +
                    " / " + displayRows.size();
            ctx.drawCenteredString(this.font,
                    scrollInfo, this.width / 2, listBottom + 5, 0xFFFFFFFF);

            // 滚动条
            int scrollbarX = this.width - 6;
            int scrollbarWidth = 3;
            int listHeight = listBottom - LIST_TOP;
            // 轨道
            ctx.fill(scrollbarX, LIST_TOP, scrollbarX + scrollbarWidth, listBottom, 0x30FFFFFF);
            // 滑块
            float ratio = (float) visibleRows / displayRows.size();
            int thumbHeight = Math.max(6, (int)(listHeight * ratio));
            int maxScroll = Math.max(1, displayRows.size() - visibleRows);
            int thumbY = LIST_TOP + (int)((listHeight - thumbHeight) * (float) scrollOffset / maxScroll);
            ctx.fill(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, 0x80FFFFFF);
        }
    }

    @Override
    public boolean onMouseClicked(MouseButtonEvent event, boolean isDrag) {
        if (super.onMouseClicked(event, isDrag)) return true;
        if (isDrag) return false;

        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        int button = event.button();

        if (button == 0) {
            // 搜索图标点击
            int searchIconX = allowPin ? 33 : 15;
            if (mouseX >= searchIconX && mouseX <= searchIconX + 16
                    && mouseY >= HEADER_Y && mouseY <= HEADER_Y + HEADER_HEIGHT) {
                searchActive = !searchActive;
                if (searchActive && searchField != null) {
                    searchField.setFocused(true);
                }
                if (!searchActive) {
                    searchText = "";
                    if (searchField != null) searchField.setValue("");
                }
                initGui();
                return true;
            }
            if (mouseY >= HEADER_Y && mouseY <= HEADER_Y + HEADER_HEIGHT) {
                handleHeaderClick(mouseX, mouseY);
                return true;
            }

            // 标记材料点击
            if (allowPin) {
                List<MaterialRow> displayRows = getSortedRows();
                int pinRow = getRowAtY(mouseY, displayRows);
                if (pinRow >= 0 && mouseX >= 15 && mouseX <= 31) {
                    MaterialRow row = displayRows.get(pinRow);
                    if (currentPinned.contains(row.originalItem)) {
                        currentPinned.remove(row.originalItem);
                    } else {
                        currentPinned.add(row.originalItem);
                    }
                    savePersistence();
                    initGui();
                    return true;
                }
            }

            // 拖拽检测（仅在启用手动重排时）
            if (Configs.Generic.ALLOW_MANUAL_REORDER.getBooleanValue()) {
                List<MaterialRow> displayRows = getSortedRows();
                int row = getRowAtY(mouseY, displayRows);
                if (row >= 0) {
                    // 排除按钮区域
                    if (mouseX >= 10 && mouseX < this.width - 210) {
                        draggedRow = row;
                        dragStartY = mouseY;
                        isDragging = false;
                        return true;
                    }
                }
            }
        }
        // 滚动条点击
        List<MaterialRow> displayRows = getSortedRows();
        int listBottom = LIST_TOP + visibleRows * ROW_HEIGHT;
        int scrollbarX = this.width - 6;
        int scrollbarWidth = 3;
        if (displayRows.size() > visibleRows && mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth
                && mouseY >= LIST_TOP && mouseY <= listBottom) {
            int trackHeight = listBottom - LIST_TOP;
            int maxScroll = Math.max(1, displayRows.size() - visibleRows);
            scrollOffset = Math.max(0, Math.min(maxScroll, (int) ((float) (mouseY - LIST_TOP) / trackHeight * maxScroll)));
            scrollbarDragging = true;
            initGui();
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        int mouseY = (int) event.y();
        int button = event.button();

        if (scrollbarDragging && button == 0) {
            List<MaterialRow> displayRows = getSortedRows();
            int listBottom = LIST_TOP + visibleRows * ROW_HEIGHT;
            int trackHeight = listBottom - LIST_TOP;
            int maxScroll = Math.max(1, displayRows.size() - visibleRows);
            scrollOffset = Math.max(0, Math.min(maxScroll, (int) ((float) (mouseY - LIST_TOP) / trackHeight * maxScroll)));
            initGui();
            return true;
        }

        if (draggedRow >= 0 && button == 0 && Configs.Generic.ALLOW_MANUAL_REORDER.getBooleanValue()) {
            List<MaterialRow> displayRows = getSortedRows();
            int currentRow = getRowAtY(mouseY, displayRows);
            if (currentRow >= 0 && currentRow != draggedRow && Math.abs(mouseY - dragStartY) > 5) {
                // 不允许跨置顶边界拖拽
                if (allowPin) {
                    MaterialRow dragged = displayRows.get(draggedRow);
                    MaterialRow target = displayRows.get(currentRow);
                    boolean draggedPinned = currentPinned.contains(dragged.originalItem);
                    boolean targetPinned = currentPinned.contains(target.originalItem);
                    if (draggedPinned != targetPinned) {
                        return true;
                    }
                }
                isDragging = true;
                // 交换 rows 中的元素
                MaterialRow draggedItem = displayRows.get(draggedRow);
                MaterialRow targetItem = displayRows.get(currentRow);
                int actualDraggedIdx = rows.indexOf(draggedItem);
                int actualTargetIdx = rows.indexOf(targetItem);
                if (actualDraggedIdx >= 0 && actualTargetIdx >= 0) {
                    Collections.swap(rows, actualDraggedIdx, actualTargetIdx);
                    // 更新重排顺序
                    saveReorderedItems();
                    sortMode = SortMode.NONE;
                    initGui();
                }
                draggedRow = currentRow;
                dragStartY = mouseY;
            }
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (scrollbarDragging) {
            scrollbarDragging = false;
            return true;
        }
        if (draggedRow >= 0) {
            if (isDragging) {
                saveReorderedItems();
            }
            draggedRow = -1;
            isDragging = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    private int getRowAtY(int mouseY, List<MaterialRow> displayRows) {
        if (mouseY < LIST_TOP) return -1;
        int row = (mouseY - LIST_TOP) / ROW_HEIGHT + scrollOffset;
        if (row >= displayRows.size()) return -1;
        return row;
    }

    private void saveReorderedItems() {
        List<Item> order = new ArrayList<>();
        for (MaterialRow row : rows) {
            if (!currentIgnored.contains(row.item)) {
                order.add(row.originalItem);
            }
        }
        reorderedItems.put(filePath, order);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount,
                                  double verticalAmount) {
        List<MaterialRow> displayRows = getSortedRows();
        if (mouseY >= LIST_TOP && mouseY <= LIST_TOP + visibleRows * ROW_HEIGHT) {
            scrollOffset = Math.max(0, scrollOffset - (int) verticalAmount);
            int maxOffset = Math.max(0, displayRows.size() - visibleRows);
            scrollOffset = Math.min(scrollOffset, maxOffset);
            initGui();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            if (searchActive) {
                searchActive = false;
                searchText = "";
                if (searchField != null) searchField.setValue("");
                initGui();
                return true;
            }
            Minecraft.getInstance().setScreenAndShow(parent);
            return true;
        }
        if (searchActive && searchField != null && searchField.isFocused()) {
            if (searchField.keyPressed(event)) {
                searchText = searchField.getValue();
                scrollOffset = 0;
                initGui();
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchActive && searchField != null && searchField.isFocused()) {
            if (searchField.charTyped(event)) {
                searchText = searchField.getValue();
                scrollOffset = 0;
                initGui();
                return true;
            }
        }
        return super.charTyped(event);
    }

    private record MaterialRow(ItemStack itemStack, String name, int totalCount, Item item, Item originalItem, int missingCount) {}

    private void drawTextureIcon(GuiContext ctx, Identifier textureId, int x, int y, int w, int h) {
        Pair<GpuTextureView, GpuSampler> pair = ctx.bindTexture(textureId);
        if (pair == null) return;
        TextureSetup texSetup = TextureSetup.singleTexture(pair.getLeft(), pair.getRight());
        BlitRenderState blit = new BlitRenderState(
                RenderPipelines.GUI_TEXTURED,
                texSetup,
                new Matrix3x2f(),
                x, y, x + w, y + h,
                0.0f, 1.0f, 0.0f, 1.0f,
                0xFFFFFFFF,
                null
        );
        ctx.addSimpleElementToCurrentLayer(blit);
    }
}

