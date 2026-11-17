package com.litematlist.gui;

import com.litematlist.LitematicReader;
import com.litematlist.LitematListMod;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.registry.Registries;
//#elseif MC == 11701
//$$import net.minecraft.util.registry.Registry;
//#endif
import net.minecraft.util.Identifier;

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
//#if MC == 12001
//$$    // ---- 宸插拷鐣ョ墿鍝?----
//#endif
    static final Map<Path, Set<Item>> ignoredItems = new HashMap<>();
    static final Map<Path, Set<String>> exactIgnoredItems = new HashMap<>();
//#if MC == 11701 || MC == 11904
//$$
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 替换映射（持久化：原理图路径 → 原始物品ID → 替换物品ID） ----
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 鏇挎崲鏄犲皠锛堟寔涔呭寲锛氬師鐞嗗浘璺緞 鈫?鍘熷鐗╁搧ID 鈫?鏇挎崲鐗╁搧ID锛?----
//#endif
    static final Map<String, Map<String, String>> replacements = new HashMap<>();
    // ---- 精确物品替换映射（持久化：原理图路径 → 精确ID → 替换物品ID） ----
    static final Map<String, Map<String, String>> exactReplacements = new HashMap<>();
    private static boolean persistenceLoaded = false;
    private static final Gson GSON = new Gson();
    private static Path persistenceFile;
    private static String currentWorldId = null;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 持久化的屏幕开关状态 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 鎸佷箙鍖栫殑灞忓箷寮€鍏崇姸鎬?----
//#endif
    public static boolean persistedHideNoMissing = false;
    public static boolean persistedAllowPin = true;
    public static String persistedDetailSortMode = "NONE";
    public static String persistedProjectSortMode = "NONE";
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 持久化的HUD状态 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 鎸佷箙鍖栫殑HUD鐘舵€?----
//#endif
    public static boolean persistedHudVisible = false;
    public static int persistedHudX = 10;
    public static int persistedHudY = 30;
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

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 原材料分析持久化数据 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 鍘熸潗鏂欏垎鏋愭寔涔呭寲鏁版嵁 ----
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 原材料列表: pathKey -> raw material entries */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 鍘熸潗鏂欏垪琛? pathKey -> raw material entries */
//#endif
    public static final Map<String, List<RawMaterialEntry>> rawMaterials = new HashMap<>();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 冗余列表: pathKey -> redundant items */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 鍐椾綑鍒楄〃: pathKey -> redundant items */
//#endif
    public static final Map<String, List<RawMaterialEntry>> rawMaterialRedundancy = new HashMap<>();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 原材料替换: pathKey -> (originalItemId -> replacementItemId) */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 鍘熸潗鏂欐浛鎹? pathKey -> (originalItemId -> replacementItemId) */
//#endif
    public static final Map<String, Map<String, String>> rawMaterialReplacements = new HashMap<>();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 原材料忽略: pathKey -> set of ignored item IDs */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 鍘熸潗鏂欏拷鐣? pathKey -> set of ignored item IDs */
//#endif
    public static final Map<String, Set<String>> rawMaterialIgnored = new HashMap<>();
    public static final Map<String, Set<String>> rawMaterialSourceIgnored = new HashMap<>();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 上传至 PlayerControl++ 的条目 path */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 涓婁紶鑷?PlayerControl++ 鐨勬潯鐩?path */
//#endif
    public static String playerControlPlusUploaded = null;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** PlayerControl++ 读取的配方树根节点列表（字段 k），注入后供其他模组接入 */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** PlayerControl++ 璇诲彇鐨勯厤鏂规爲鏍硅妭鐐瑰垪琛紙瀛楁 k锛夛紝娉ㄥ叆鍚庝緵鍏朵粬妯＄粍鎺ュ叆 */
//#endif
    public static List<com.litematlist.RecipeTreeNode> playerControlPlusMaterialList = null;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 原材料条目 */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 鍘熸潗鏂欐潯鐩?*/
//#endif
    public record RawMaterialEntry(String itemId, int count, List<String> sources) {
        public RawMaterialEntry withCount(int newCount) { return new RawMaterialEntry(itemId, newCount, sources); }
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 获取当前世界的标识符（单人=存档名，多人=服务器IP，主菜单=null） */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 鑾峰彇褰撳墠涓栫晫鐨勬爣璇嗙锛堝崟浜?瀛樻。鍚嶏紝澶氫汉=鏈嶅姟鍣↖P锛屼富鑿滃崟=null锛?*/
//#endif
    public static String getWorldId() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getCurrentServerEntry() != null) {
            return "mp_" + client.getCurrentServerEntry().address.replace(":", "_");
        }
        if (client.getServer() != null) {
            // 使用存档文件夹名而非显示名称，确保同名存档的唯一性
            try {
                java.nio.file.Path levelDatPath = client.getServer().getSavePath(net.minecraft.util.WorldSavePath.LEVEL_DAT);
                String dirName = levelDatPath.getParent().getFileName().toString();
                return "sp_" + dirName;
            } catch (Exception e) {
                LitematListMod.LOGGER.warn("获取存档文件夹名失败，回退到显示名称", e);
                return "sp_" + client.getServer().getSaveProperties().getLevelName();
            }
        }
        return null;
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 导入材料缓存（txt 文件导入的材料列表，不经过 .litematic 解析） ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 瀵煎叆鏉愭枡缂撳瓨锛坱xt 鏂囦欢瀵煎叆鐨勬潗鏂欏垪琛紝涓嶇粡杩?.litematic 瑙ｆ瀽锛?----
//#endif
    static final Map<Path, List<LitematicReader.MaterialEntry>> importedMaterials = new HashMap<>();

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 缓存从 txt 导入的材料列表，供 MaterialDetailScreen 加载时使用。
     */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /**
//$$     * 缂撳瓨浠?txt 瀵煎叆鐨勬潗鏂欏垪琛紝渚?MaterialDetailScreen 鍔犺浇鏃朵娇鐢ㄣ€?
//$$     */
//#endif
    public static void cacheMaterialList(Path filePath, List<LitematicReader.MaterialEntry> materials) {
        importedMaterials.put(filePath, materials);
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 供外部获取导入材料缓存 */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 渚涘閮ㄨ幏鍙栧鍏ユ潗鏂欑紦瀛?*/
//#endif
    public static Map<Path, List<LitematicReader.MaterialEntry>> getImportedMaterials() {
        return importedMaterials;
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 获取指定路径的已忽略物品集合 */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 鑾峰彇鎸囧畾璺緞鐨勫凡蹇界暐鐗╁搧闆嗗悎 */
//#endif
    public static Set<Item> getIgnoredForPath(Path path) {
        ensurePersistenceLoaded();
        return ignoredItems.computeIfAbsent(path, k -> new HashSet<>());
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 获取指定路径的置顶物品集合 */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 鑾峰彇鎸囧畾璺緞鐨勭疆椤剁墿鍝侀泦鍚?*/
//#endif
    public static Set<Item> getPinnedForPath(Path path) {
        ensurePersistenceLoaded();
        return pinnedItems.computeIfAbsent(path, k -> new HashSet<>());
    }

    /** 获取指定路径的已忽略精确物品集合（按精确ID） */
    public static Set<String> getExactIgnoredForPath(Path path) {
        ensurePersistenceLoaded();
        return exactIgnoredItems.computeIfAbsent(path, k -> new HashSet<>());
    }
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
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
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        
//#endif
        if (worldId == null) return; // 不在世界中，不加载持久化数据
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        
//#endif
        if (persistenceLoaded && worldId.equals(currentWorldId)) return; // 已加载且 worldId 未变
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        
//#endif
        persistenceLoaded = true;
        currentWorldId = worldId;
        persistenceFile = FabricLoader.getInstance().getConfigDir().resolve("litematlist/" + worldId + "/persistence.json");

        if (java.nio.file.Files.exists(persistenceFile)) {
            try (Reader reader = new InputStreamReader(new FileInputStream(persistenceFile.toFile()), java.nio.charset.StandardCharsets.UTF_8)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root != null) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    // 加载 ignored（合并到已有集合，避免覆盖 currentIgnored 引用）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    // 鍔犺浇 ignored锛堝悎骞跺埌宸叉湁闆嗗悎锛岄伩鍏嶈鐩?currentIgnored 寮曠敤锛?
//#endif
                    JsonObject ignoredData = root.getAsJsonObject("ignored");
                    if (ignoredData != null) {
                        for (var entry : ignoredData.entrySet()) {
                            Path path = Path.of(entry.getKey());
                            Set<Item> existing = ignoredItems.computeIfAbsent(path, k -> new HashSet<>());
                            JsonObject itemsObj = entry.getValue().getAsJsonObject();
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            for (String itemId : itemsObj.keySet()) {
//#elseif MC == 11701
//$$                            for (var itemEntry : itemsObj.entrySet()) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                Item item = Registries.ITEM.get(Identifier.of(itemId));
//#elseif MC == 11701
//$$                                Item item = Registry.ITEM.get(Identifier.tryParse(itemEntry.getKey()));
//#elseif MC == 11904 || MC == 12001
//$$                                Item item = Registries.ITEM.get(Identifier.tryParse(itemId));
//#endif
                                if (item != null) existing.add(item);
                            }

                        }
                    }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    // 加载 replacements
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    // 鍔犺浇 replacements
//#endif
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
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    // 加载 entries
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    // 鍔犺浇 entries
//#endif
                    JsonArray entriesArr = root.getAsJsonArray("entries");
                    if (entriesArr != null) {
                        for (JsonElement elem : entriesArr) {
                            JsonObject obj = elem.getAsJsonObject();
                            String name = obj.get("name").getAsString();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            // 项目文件夹持久化时 path 为空串，必须还原为 null
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                            // 项目文件夹持久化时 path 为空串，必须还原为 null，
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                            // 否则会被当成假文件路径，导致上传区域【原材料表】走错加载分支
//#endif
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
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        LitematListMod.LOGGER.info("已加载 {} 个持久化条目", entriesArr.size());
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                        LitematListMod.LOGGER.info("Loaded persistence data ({0})", entriesArr.size());
//#endif
                    }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    // 加载 pinnedItems（材料清单中的置顶物品，合并到已有集合避免覆盖 currentPinned 引用）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    // 鍔犺浇 pinnedItems锛堟潗鏂欐竻鍗曚腑鐨勭疆椤剁墿鍝侊紝鍚堝苟鍒板凡鏈夐泦鍚堥伩鍏嶈鐩?currentPinned 寮曠敤锛?
//#endif
                    JsonObject pinnedData = root.getAsJsonObject("pinnedItems");
                    if (pinnedData != null) {
                        for (var entry : pinnedData.entrySet()) {
                            Path path = Path.of(entry.getKey());
                            Set<Item> existing = pinnedItems.computeIfAbsent(path, k -> new HashSet<>());
                            JsonArray arr = entry.getValue().getAsJsonArray();
                            for (JsonElement e : arr) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                Item item = Registries.ITEM.get(Identifier.of(e.getAsString()));
//#elseif MC == 11701
//$$                                Item item = Registry.ITEM.get(Identifier.tryParse(e.getAsString()));
//#elseif MC == 11904 || MC == 12001
//$$                                Item item = Registries.ITEM.get(Identifier.tryParse(e.getAsString()));
//#endif
                                if (item != null) existing.add(item);
                            }

                        }
                    }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    // 加载屏幕开关状态
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    // 鍔犺浇灞忓箷寮€鍏崇姸鎬?
//#endif
                    JsonObject screenData = root.getAsJsonObject("screenSettings");
                    if (screenData != null) {
                        if (screenData.has("hideNoMissing")) persistedHideNoMissing = screenData.get("hideNoMissing").getAsBoolean();
                        if (screenData.has("allowPin")) persistedAllowPin = screenData.get("allowPin").getAsBoolean();
                        if (screenData.has("detailSortMode")) persistedDetailSortMode = screenData.get("detailSortMode").getAsString();
                        if (screenData.has("projectSortMode")) persistedProjectSortMode = screenData.get("projectSortMode").getAsString();
                        if (screenData.has("hudVisible")) persistedHudVisible = screenData.get("hudVisible").getAsBoolean();
                        if (screenData.has("hudX")) persistedHudX = screenData.get("hudX").getAsInt();
                        if (screenData.has("hudY")) persistedHudY = screenData.get("hudY").getAsInt();
                        if (screenData.has("hudFontSize")) persistedHudFontSize = Math.max(1, Math.min(32, screenData.get("hudFontSize").getAsInt()));
                        if (screenData.has("hudBgAlpha")) persistedHudBgAlpha = screenData.get("hudBgAlpha").getAsInt();
                        if (screenData.has("hudMaxLines")) persistedHudMaxLines = screenData.get("hudMaxLines").getAsInt();
                        if (screenData.has("hudFormat")) persistedHudFormat = screenData.get("hudFormat").getAsString();
                        // 旧默认格式迁移为带 | 对齐基准的新默认格式
                        if ("[icon][name][item]([box],[stack])".equals(persistedHudFormat)) {
                            persistedHudFormat = "[icon][name]|[item]|([box],[stack])";
                        }
                        if (screenData.has("hudShowBox")) persistedHudShowBox = screenData.get("hudShowBox").getAsBoolean();
                        if (screenData.has("hudShowGroup")) persistedHudShowGroup = screenData.get("hudShowGroup").getAsBoolean();
                        if (screenData.has("hudUseRatio")) persistedHudUseRatio = screenData.get("hudUseRatio").getAsBoolean();
                        if (screenData.has("hudRatioX")) persistedHudRatioX = screenData.get("hudRatioX").getAsFloat();
                        if (screenData.has("hudRatioY")) persistedHudRatioY = screenData.get("hudRatioY").getAsFloat();
                        if (screenData.has("hudAnchor")) persistedHudAnchor = screenData.get("hudAnchor").getAsInt();
                    }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    // 加载原材料分析数据
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    // 鍔犺浇鍘熸潗鏂欏垎鏋愭暟鎹?
//#endif
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
                }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                LitematListMod.LOGGER.info("已加载持久化数据: {} 个忽略集, {} 个替换映射", ignoredItems.size(), replacements.size());
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                LitematListMod.LOGGER.info("Loaded persistence: {0} ignored sets, {1} replacement maps", ignoredItems.size(), replacements.size());
//#endif
            } catch (Exception e) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                LitematListMod.LOGGER.error("加载持久化数据失败", e);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                LitematListMod.LOGGER.error("Failed to load persistence", e);
//#endif
            }
        }
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 切换世界时保存旧数据并加载新数据 */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 鍒囨崲涓栫晫鏃朵繚瀛樻棫鏁版嵁骞跺姞杞芥柊鏁版嵁 */
//#endif
    public static void switchToWorld(String newWorldId) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 保存旧世界数据
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 淇濆瓨鏃т笘鐣屾暟鎹?
//#endif
        if (currentWorldId != null && persistenceFile != null) {
            savePersistence();
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 清理内存数据
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 娓呯悊鍐呭瓨鏁版嵁
//#endif
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
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        // 绂诲紑涓栫晫鏃跺叧闂璈UD锛岄伩鍏嶆柊瀛樻。涓璈UD鐘舵€佹畫鐣?
//#endif
        // HUD 开关状态随持久化保存：新世界有记录时由其恢复，无记录时沿用当前开关，
        // 避免上传区域没有列表时每次进世界都要重新打开 HUD
        MaterialListHudRenderer.clearHudItems();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 标记需要重新加载
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鏍囪闇€瑕侀噸鏂板姞杞?
//#endif
        persistenceLoaded = false;
        persistenceFile = null;
        currentWorldId = null;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 加载新世界数据
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鍔犺浇鏂颁笘鐣屾暟鎹?
//#endif
        if (newWorldId != null) {
            ensurePersistenceLoaded(newWorldId);
        }
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 保存所有持久化数据（忽略、替换、条目列表） */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 淇濆瓨鎵€鏈夋寔涔呭寲鏁版嵁锛堝拷鐣ャ€佹浛鎹€佹潯鐩垪琛級 */
//#endif
    public static void savePersistence() {
        if (persistenceFile == null) return;
        try {
            JsonObject root = new JsonObject();

            // ignored
            JsonObject ignoredData = new JsonObject();
            for (var entry : ignoredItems.entrySet()) {
                JsonObject items = new JsonObject();
                for (Item item : entry.getValue()) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    Identifier id = Registries.ITEM.getId(item);
//#elseif MC == 11701
//$$                    Identifier id = Registry.ITEM.getId(item);
//#endif
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
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
            // exactIgnored（精确物品忽略）
            JsonObject exactIgnData = new JsonObject();
            for (var entry : exactIgnoredItems.entrySet()) {
                JsonArray arr = new JsonArray();
                for (String s : entry.getValue()) arr.add(s);
                exactIgnData.add(entry.getKey().toString(), arr);
            }
            root.add("exactIgnored", exactIgnData);
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
            // exactPinned（精确物品置顶）
            JsonObject exactPinnedData = new JsonObject();
            for (var entry : exactPinnedItems.entrySet()) {
                JsonArray arr = new JsonArray();
                for (String s : entry.getValue()) arr.add(s);
                exactPinnedData.add(entry.getKey().toString(), arr);
            }
            root.add("exactPinned", exactPinnedData);
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
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

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // pinnedItems（材料清单中的置顶物品）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // pinnedItems锛堟潗鏂欐竻鍗曚腑鐨勭疆椤剁墿鍝侊級
//#endif
            JsonObject pinnedData = new JsonObject();
            for (var entry : pinnedItems.entrySet()) {
                JsonArray arr = new JsonArray();
                for (Item item : entry.getValue()) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    Identifier id = Registries.ITEM.getId(item);
//#elseif MC == 11701
//$$                    Identifier id = Registry.ITEM.getId(item);
//#endif
                    if (id != null) arr.add(id.toString());
                }
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (!arr.isEmpty()) {
//#elseif MC == 11701
//$$                if (arr.size() > 0) {
//#endif
                    pinnedData.add(entry.getKey().toString(), arr);
                }
            }
            root.add("pinnedItems", pinnedData);

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 原材料分析数据
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 鍘熸潗鏂欏垎鏋愭暟鎹?
//#endif
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
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 树状图缩放档位（随世界持久化）
//#endif
            root.addProperty("treeZoomPercent", RawMaterialScreen.getTreeZoomPercent());

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 屏幕开关状态
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 灞忓箷寮€鍏崇姸鎬?
//#endif
            JsonObject screenData = new JsonObject();
            screenData.addProperty("hideNoMissing", persistedHideNoMissing);
            screenData.addProperty("allowPin", persistedAllowPin);
            screenData.addProperty("detailSortMode", persistedDetailSortMode);
            screenData.addProperty("projectSortMode", persistedProjectSortMode);
            screenData.addProperty("hudVisible", persistedHudVisible);
            screenData.addProperty("hudX", persistedHudX);
            screenData.addProperty("hudY", persistedHudY);
            screenData.addProperty("hudFontSize", persistedHudFontSize);
            screenData.addProperty("hudBgAlpha", persistedHudBgAlpha);
            screenData.addProperty("hudMaxLines", persistedHudMaxLines);
            screenData.addProperty("hudFormat", persistedHudFormat);
            screenData.addProperty("hudShowBox", persistedHudShowBox);
            screenData.addProperty("hudShowGroup", persistedHudShowGroup);
            screenData.addProperty("hudUseRatio", persistedHudUseRatio);
            screenData.addProperty("hudRatioX", persistedHudRatioX);
            screenData.addProperty("hudRatioY", persistedHudRatioY);
            screenData.addProperty("hudAnchor", persistedHudAnchor);
            root.add("screenSettings", screenData);

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 确保目录存在
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 纭繚鐩綍瀛樺湪
//#endif
            java.nio.file.Files.createDirectories(persistenceFile.getParent());
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(persistenceFile.toFile()), java.nio.charset.StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            LitematListMod.LOGGER.info("已保存持久化数据 ({} 个条目)", entriesArr.size());
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            LitematListMod.LOGGER.info("Saved persistence data ({0}) entries", entriesArr.size());
//#endif
        } catch (Exception e) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            LitematListMod.LOGGER.error("保存持久化数据失败", e);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            LitematListMod.LOGGER.error("Failed to save persistence", e);
//#endif
        }
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 获取替换物品（如果有），否则返回原物品 */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 鑾峰彇鏇挎崲鐗╁搧锛堝鏋滄湁锛夛紝鍚﹀垯杩斿洖鍘熺墿鍝?*/
//#endif
    public static Item getReplacement(Path filePath, Item original) {
        ensurePersistenceLoaded();
        String pathKey = filePath.toString();
        Map<String, String> fileReplacements = replacements.get(pathKey);
        if (fileReplacements == null) return original;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Identifier origId = Registries.ITEM.getId(original);
//#elseif MC == 11701
//$$        Identifier origId = Registry.ITEM.getId(original);
//#endif
        if (origId == null) return original;
        String replId = fileReplacements.get(origId.toString());
        if (replId == null) return original;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return Registries.ITEM.get(Identifier.of(replId));
//#elseif MC == 11701
//$$        return Registry.ITEM.get(Identifier.tryParse(replId));
//#elseif MC == 11904 || MC == 12001
//$$        return Registries.ITEM.get(Identifier.tryParse(replId));
//#endif
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 设置替换映射（供外部如 ProjectSummaryScreen 调用） */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 璁剧疆鏇挎崲鏄犲皠锛堜緵澶栭儴濡?ProjectSummaryScreen 璋冪敤锛?*/
//#endif
    public static void setReplacement(Path filePath, Item original, Item replacement) {
        ensurePersistenceLoaded();
        String pathKey = filePath.toString();
        Map<String, String> fileReplacements = replacements.computeIfAbsent(pathKey, k -> new HashMap<>());
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Identifier origId = Registries.ITEM.getId(original);
//#elseif MC == 11701
//$$        Identifier origId = Registry.ITEM.getId(original);
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Identifier newId = Registries.ITEM.getId(replacement);
//#elseif MC == 11701
//$$        Identifier newId = Registry.ITEM.getId(replacement);
//#endif
        if (origId != null && newId != null) {
            fileReplacements.put(origId.toString(), newId.toString());
        }
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 移除替换映射（供外部如 ProjectSummaryScreen 调用） */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 绉婚櫎鏇挎崲鏄犲皠锛堜緵澶栭儴濡?ProjectSummaryScreen 璋冪敤锛?*/
//#endif
    public static void removeReplacement(Path filePath, Item original) {
        ensurePersistenceLoaded();
        String pathKey = filePath.toString();
        Map<String, String> fileReplacements = replacements.get(pathKey);
        if (fileReplacements != null) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            Identifier origId = Registries.ITEM.getId(original);
//#elseif MC == 11701
//$$            Identifier origId = Registry.ITEM.getId(original);
//#endif
            if (origId != null) {
                fileReplacements.remove(origId.toString());
                if (fileReplacements.isEmpty()) {
                    replacements.remove(pathKey);
                }
            }
        }
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 供外部调用，初始化持久化 */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 渚涘閮ㄨ皟鐢紝鍒濆鍖栨寔涔呭寲 */
//#endif
    public static void initPersistence() {
        ensurePersistenceLoaded();
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 清理指定路径的持久化数据（替换映射和忽略物品） */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 娓呯悊鎸囧畾璺緞鐨勬寔涔呭寲鏁版嵁锛堟浛鎹㈡槧灏勫拰蹇界暐鐗╁搧锛?*/
//#endif
    public static void cleanupPathData(Path filePath) {
        ensurePersistenceLoaded();
        String pathKey = filePath.toString();
        replacements.remove(pathKey);
        exactReplacements.remove(pathKey);
        ignoredItems.remove(filePath);
        exactIgnoredItems.remove(filePath);
        exactPinnedItems.remove(filePath);
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        LitematListMod.LOGGER.info("已清理路径持久化数据: {}", pathKey);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        LitematListMod.LOGGER.info("Loaded persistence for: {0}", pathKey);
//#endif
    }
//#if MC == 12001
//$$    // ---- 鎵嬪姩閲嶆帓 ----
//#elseif MC == 12101
//$$
//#endif
    static final Map<Path, List<Item>> reorderedItems = new HashMap<>();
    static final Map<Path, Set<Item>> pinnedItems = new HashMap<>();
    static final Map<Path, Set<String>> exactPinnedItems = new HashMap<>();
//#if MC == 12101
//$$
//#endif
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
