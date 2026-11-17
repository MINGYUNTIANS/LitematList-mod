package com.litematlist.gui;

import com.litematlist.LitematicReader;
import com.litematlist.LitematListMod;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

/**
 * 材料数据仓库 —— 集中持有 litematlist 的持久化状态（忽略 / 替换 / 置顶 / 原材料 / HUD 与屏幕开关），
 * 并提供按世界隔离的加载与保存逻辑。
 *
 * <p>这些静态字段与持久化方法原先都堆在 MaterialDetailScreen 中，
 * 使其既是界面类又是全局数据仓库，并被十余个界面 / 桥接类直接静态依赖。
 * 现统一收拢到本类，界面类只保留渲染与交互逻辑。
 */
public class MaterialDataStore {
    static final Map<Path, Set<Item>> ignoredItems = new HashMap<>();
    static final Map<Path, Set<String>> exactIgnoredItems = new HashMap<>();
    // ---- 替换映射（持久化：原理图路径 → 原始物品ID → 替换物品ID） ----
    static final Map<String, Map<String, String>> replacements = new HashMap<>();
    // ---- 精确物品替换映射（持久化：原理图路径 → 精确ID → 替换物品ID） ----
    static final Map<String, Map<String, String>> exactReplacements = new HashMap<>();
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
    // HUD 每行的显示格式，支持 [icon] [name] [item] [box] [stack] 五个变量与任意自定义文字
    public static String persistedHudFormat = "[icon][name]|[item]|([box],[stack])";
    public static boolean persistedHudShowBox = false;   // 盒(1728)开关
    public static boolean persistedHudShowGroup = false; // 组(64)开关
    // 按比例保存HUD位置：开启后位置以「左上角占屏幕宽/高的比例」存储，屏幕UI缩放变化时相对位置不变
    public static boolean persistedHudUseRatio = false;
    public static float persistedHudRatioX = 0.02f;
    public static float persistedHudRatioY = 0.05f;
    // HUD 校准位置（比例模式的参照角）：0=左上(默认) 1=左下 2=右上 3=右下
    public static int persistedHudAnchor = 0;
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
            // 使用存档文件夹名而非显示名称，确保同名存档的唯一性
            net.minecraft.server.MinecraftServer server = client.getSingleplayerServer();
            try {
                java.nio.file.Path savePath = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize();
                return "sp_" + savePath.getFileName().toString();
            } catch (Exception e) {
                LitematListMod.LOGGER.warn("获取存档文件夹名失败，回退到显示名称", e);
                return "sp_" + server.getWorldData().getLevelName();
            }
        }
        return null;
    }

    // ---- 导入材料缓存（txt 文件导入的材料列表，不经过 .litematic 解析） ----
    static final Map<Path, List<LitematicReader.MaterialEntry>> importedMaterials = new HashMap<>();

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

    /** 获取指定路径的已忽略精确物品集合（按精确ID） */
    public static Set<String> getExactIgnoredForPath(Path path) {
        ensurePersistenceLoaded();
        return exactIgnoredItems.computeIfAbsent(path, k -> new HashSet<>());
    }

    /** 获取指定路径的置顶精确物品集合（按精确ID） */
    public static Set<String> getExactPinnedForPath(Path path) {
        ensurePersistenceLoaded();
        return exactPinnedItems.computeIfAbsent(path, k -> new HashSet<>());
    }

    static void ensurePersistenceLoaded() {
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
                    // 加载 ignored（合并到已有集合，避免覆盖 currentIgnored 引用）
                    JsonObject ignoredData = root.getAsJsonObject("ignored");
                    if (ignoredData != null) {
                        for (var entry : ignoredData.entrySet()) {
                            Path path = Path.of(entry.getKey());
                            Set<Item> existing = ignoredItems.computeIfAbsent(path, k -> new HashSet<>());
                            JsonObject itemsObj = entry.getValue().getAsJsonObject();
                            for (String itemId : itemsObj.keySet()) {
                                var holder = BuiltInRegistries.ITEM.get(Identifier.parse(itemId));
                                if (holder.isPresent()) {
                                    Item item = holder.get().value();
                                    if (item != null) existing.add(item);
                                }
                            }
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
                    // 加载 exactReplacements（精确物品替换映射）
                    JsonObject exactReplData = root.getAsJsonObject("exactReplacements");
                    if (exactReplData != null) {
                        for (var entry : exactReplData.entrySet()) {
                            Map<String, String> map = new HashMap<>();
                            JsonObject inner = entry.getValue().getAsJsonObject();
                            for (var e : inner.entrySet()) {
                                map.put(e.getKey(), e.getValue().getAsString());
                            }
                            exactReplacements.put(entry.getKey(), map);
                        }
                    }
                    // 加载 exactIgnored（精确物品忽略，合并到已有集合避免覆盖 currentExactIgnored 引用）
                    JsonObject exactIgnData = root.getAsJsonObject("exactIgnored");
                    if (exactIgnData != null) {
                        for (var entry : exactIgnData.entrySet()) {
                            Path path = Path.of(entry.getKey());
                            Set<String> existing = exactIgnoredItems.computeIfAbsent(path, k -> new HashSet<>());
                            JsonArray arr = entry.getValue().getAsJsonArray();
                            for (JsonElement e : arr) existing.add(e.getAsString());
                        }
                    }
                    // 加载 exactPinned（精确物品置顶，合并到已有集合避免覆盖 currentExactPinned 引用）
                    JsonObject exactPinnedData = root.getAsJsonObject("exactPinned");
                    if (exactPinnedData != null) {
                        for (var entry : exactPinnedData.entrySet()) {
                            Path path = Path.of(entry.getKey());
                            Set<String> existing = exactPinnedItems.computeIfAbsent(path, k -> new HashSet<>());
                            JsonArray arr = entry.getValue().getAsJsonArray();
                            for (JsonElement e : arr) existing.add(e.getAsString());
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
                    // 加载 pinnedItems（材料清单中的置顶物品，合并到已有集合避免覆盖 currentPinned 引用）
                    JsonObject pinnedData = root.getAsJsonObject("pinnedItems");
                    if (pinnedData != null) {
                        for (var entry : pinnedData.entrySet()) {
                            Path path = Path.of(entry.getKey());
                            Set<Item> existing = pinnedItems.computeIfAbsent(path, k -> new HashSet<>());
                            JsonArray arr = entry.getValue().getAsJsonArray();
                            for (JsonElement e : arr) {
                                var holder = BuiltInRegistries.ITEM.get(Identifier.parse(e.getAsString()));
                                if (holder.isPresent()) {
                                    Item item = holder.get().value();
                                    if (item != null) existing.add(item);
                                }
                            }
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
                            persistedHudFontSize = Math.max(1, Math.min(32, screenSettings.get("hudFontSize").getAsInt()));
                        }
                        if (screenSettings.has("hudBgAlpha")) {
                            persistedHudBgAlpha = screenSettings.get("hudBgAlpha").getAsInt();
                        }
                        if (screenSettings.has("hudMaxLines")) {
                            persistedHudMaxLines = screenSettings.get("hudMaxLines").getAsInt();
                        }
                        if (screenSettings.has("hudFormat")) {
                            persistedHudFormat = screenSettings.get("hudFormat").getAsString();
                            // 旧默认格式迁移为带 | 对齐基准的新默认格式
                            if ("[icon][name][item]([box],[stack])".equals(persistedHudFormat)) {
                                persistedHudFormat = "[icon][name]|[item]|([box],[stack])";
                            }
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
                        if (screenSettings.has("hudUseRatio")) {
                            persistedHudUseRatio = screenSettings.get("hudUseRatio").getAsBoolean();
                        }
                        if (screenSettings.has("hudRatioX")) {
                            persistedHudRatioX = screenSettings.get("hudRatioX").getAsFloat();
                        }
                        if (screenSettings.has("hudRatioY")) {
                            persistedHudRatioY = screenSettings.get("hudRatioY").getAsFloat();
                        }
                        if (screenSettings.has("hudAnchor")) {
                            persistedHudAnchor = screenSettings.get("hudAnchor").getAsInt();
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
        exactIgnoredItems.clear();
        exactPinnedItems.clear();
        exactReplacements.clear();
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
        // HUD 开关状态随持久化保存：新世界有记录时由其恢复，无记录时沿用当前开关，
        // 避免上传区域没有列表时每次进世界都要重新打开 HUD
        MaterialListHudRenderer.clearHudItems();
        // 标记需要重新加载
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

            // exactReplacements（精确物品替换）
            JsonObject exactReplData = new JsonObject();
            for (var entry : exactReplacements.entrySet()) {
                JsonObject inner = new JsonObject();
                for (var e : entry.getValue().entrySet()) {
                    inner.addProperty(e.getKey(), e.getValue());
                }
                exactReplData.add(entry.getKey(), inner);
            }
            root.add("exactReplacements", exactReplData);

            // exactIgnored（精确物品忽略）
            JsonObject exactIgnData = new JsonObject();
            for (var entry : exactIgnoredItems.entrySet()) {
                JsonArray arr = new JsonArray();
                for (String s : entry.getValue()) arr.add(s);
                exactIgnData.add(entry.getKey().toString(), arr);
            }
            root.add("exactIgnored", exactIgnData);

            // exactPinned（精确物品置顶）
            JsonObject exactPinnedData = new JsonObject();
            for (var entry : exactPinnedItems.entrySet()) {
                JsonArray arr = new JsonArray();
                for (String s : entry.getValue()) arr.add(s);
                exactPinnedData.add(entry.getKey().toString(), arr);
            }
            root.add("exactPinned", exactPinnedData);

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
                if (le.uploaded) obj.addProperty("uploaded", true);
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
            screenSettings.addProperty("hudFormat", persistedHudFormat);
            screenSettings.addProperty("hudShowBox", persistedHudShowBox);
            screenSettings.addProperty("hudShowGroup", persistedHudShowGroup);
            screenSettings.addProperty("hudX", persistedHudX);
            screenSettings.addProperty("hudY", persistedHudY);
            screenSettings.addProperty("hudUseRatio", persistedHudUseRatio);
            screenSettings.addProperty("hudRatioX", persistedHudRatioX);
            screenSettings.addProperty("hudRatioY", persistedHudRatioY);
            screenSettings.addProperty("hudAnchor", persistedHudAnchor);
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
        exactReplacements.remove(pathKey);
        ignoredItems.remove(filePath);
        exactIgnoredItems.remove(filePath);
        exactPinnedItems.remove(filePath);
        pinnedItems.remove(filePath);
        importedMaterials.remove(filePath);
        LitematListMod.LOGGER.info("已清理路径持久化数据: {}", pathKey);
    }

    static final Map<Path, List<Item>> reorderedItems = new HashMap<>();
    static final Map<Path, Set<Item>> pinnedItems = new HashMap<>();
    static final Map<Path, Set<String>> exactPinnedItems = new HashMap<>();

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
}
