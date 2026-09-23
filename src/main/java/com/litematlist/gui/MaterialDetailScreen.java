package com.litematlist.gui;

import com.litematlist.LitematicReader;
import com.litematlist.LitematicaBridge;
import com.litematlist.MaterialListImporter;
import com.litematlist.MaterialListInjector;
import com.litematlist.I18n;
import com.litematlist.LitematListMod;
import com.litematlist.RawMaterialAnalyzer;
import com.litematlist.SyncmaticaBridge;
import com.litematlist.config.Configs;
import com.litematlist.PinyinSearch;
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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 鏉愭枡娓呭崟瀛愮晫闈?鈥斺€?鍙傜収鎶曞奖鐨?MaterialList 鐣岄潰甯冨眬銆?
 * 鎬昏/宸叉湁/缂哄け宸︾疆锛屾浛鎹?蹇界暐鎸夐挳璐村彸銆?
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
    private int hoveredColumn = -1; // 0=鎬昏, 1=宸叉湁, 2=缂哄け

    // ---- 宸插拷鐣ョ墿鍝?----
    private static final Map<Path, Set<Item>> ignoredItems = new HashMap<>();
    private Set<Item> currentIgnored;

    // ---- 鏇挎崲鏄犲皠锛堟寔涔呭寲锛氬師鐞嗗浘璺緞 鈫?鍘熷鐗╁搧ID 鈫?鏇挎崲鐗╁搧ID锛?----
    private static final Map<String, Map<String, String>> replacements = new HashMap<>();
    private static boolean persistenceLoaded = false;
    private static final Gson GSON = new Gson();
    private static Path persistenceFile;
    private static String currentWorldId = null;
    // ---- 鎸佷箙鍖栫殑灞忓箷寮€鍏崇姸鎬?----
    public static boolean persistedHideNoMissing = false;
    public static boolean persistedAllowPin = true;
    public static String persistedDetailSortMode = "NONE";
    public static String persistedProjectSortMode = "NONE";
    // ---- 鎸佷箙鍖栫殑HUD鐘舵€?----
    public static boolean persistedHudVisible = false;
    public static int persistedHudX = 10;
    public static int persistedHudY = 30;
    public static int persistedHudFontSize = 10;
    public static int persistedHudBgAlpha = 50;
    public static int persistedHudMaxLines = 10;
    public static boolean persistedHudShowBox = false;   // 盒(1728)开关
    public static boolean persistedHudShowGroup = false; // 组(64)开关

    // ---- 鍘熸潗鏂欏垎鏋愭寔涔呭寲鏁版嵁 ----
    /** 鍘熸潗鏂欏垪琛? pathKey -> raw material entries */
    public static final Map<String, List<RawMaterialEntry>> rawMaterials = new HashMap<>();
    /** 鍐椾綑鍒楄〃: pathKey -> redundant items */
    public static final Map<String, List<RawMaterialEntry>> rawMaterialRedundancy = new HashMap<>();
    /** 鍘熸潗鏂欐浛鎹? pathKey -> (originalItemId -> replacementItemId) */
    public static final Map<String, Map<String, String>> rawMaterialReplacements = new HashMap<>();
    /** 鍘熸潗鏂欏拷鐣? pathKey -> set of ignored item IDs */
    public static final Map<String, Set<String>> rawMaterialIgnored = new HashMap<>();
    public static final Map<String, Set<String>> rawMaterialSourceIgnored = new HashMap<>();
    /** 涓婁紶鑷?PlayerControl++ 鐨勬潯鐩?path */
    public static String playerControlPlusUploaded = null;
    /** PlayerControl++ 璇诲彇鐨勯厤鏂规爲鏍硅妭鐐瑰垪琛紙瀛楁 k锛夛紝娉ㄥ叆鍚庝緵鍏朵粬妯＄粍鎺ュ叆 */
    public static List<com.litematlist.RecipeTreeNode> playerControlPlusMaterialList = null;

    /** 鍘熸潗鏂欐潯鐩?*/
    public record RawMaterialEntry(String itemId, int count, List<String> sources) {
        public RawMaterialEntry withCount(int newCount) { return new RawMaterialEntry(itemId, newCount, sources); }
    }

    /** 鑾峰彇褰撳墠涓栫晫鐨勬爣璇嗙锛堝崟浜?瀛樻。鍚嶏紝澶氫汉=鏈嶅姟鍣↖P锛屼富鑿滃崟=null锛?*/
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

    // ---- 瀵煎叆鏉愭枡缂撳瓨锛坱xt 鏂囦欢瀵煎叆鐨勬潗鏂欏垪琛紝涓嶇粡杩?.litematic 瑙ｆ瀽锛?----
    private static final Map<Path, List<LitematicReader.MaterialEntry>> importedMaterials = new HashMap<>();

    /**
     * 缂撳瓨浠?txt 瀵煎叆鐨勬潗鏂欏垪琛紝渚?MaterialDetailScreen 鍔犺浇鏃朵娇鐢ㄣ€?
     */
    public static void cacheMaterialList(Path filePath, List<LitematicReader.MaterialEntry> materials) {
        importedMaterials.put(filePath, materials);
    }

    /** 渚涘閮ㄨ幏鍙栧鍏ユ潗鏂欑紦瀛?*/
    public static Map<Path, List<LitematicReader.MaterialEntry>> getImportedMaterials() {
        return importedMaterials;
    }

    /** 鑾峰彇鎸囧畾璺緞鐨勫凡蹇界暐鐗╁搧闆嗗悎 */
    public static Set<Item> getIgnoredForPath(Path path) {
        ensurePersistenceLoaded();
        return ignoredItems.computeIfAbsent(path, k -> new HashSet<>());
    }

    /** 鑾峰彇鎸囧畾璺緞鐨勭疆椤剁墿鍝侀泦鍚?*/
    public static Set<Item> getPinnedForPath(Path path) {
        ensurePersistenceLoaded();
        return pinnedItems.computeIfAbsent(path, k -> new HashSet<>());
    }

    private static void ensurePersistenceLoaded() {
        ensurePersistenceLoaded(null);
    }

    private static void ensurePersistenceLoaded(String explicitWorldId) {
        if (persistenceLoaded) return;
        
        String worldId = explicitWorldId != null ? explicitWorldId : getWorldId();
        
        if (worldId == null) return; // 不在世界中，不加载持久化数据
        
        persistenceLoaded = true;
        currentWorldId = worldId;
        persistenceFile = FabricLoader.getInstance().getConfigDir().resolve("litematlist/" + worldId + "/persistence.json");

        if (java.nio.file.Files.exists(persistenceFile)) {
            try (Reader reader = new InputStreamReader(new FileInputStream(persistenceFile.toFile()), java.nio.charset.StandardCharsets.UTF_8)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root != null) {
                    // 鍔犺浇 ignored锛堝悎骞跺埌宸叉湁闆嗗悎锛岄伩鍏嶈鐩?currentIgnored 寮曠敤锛?
                    JsonObject ignoredData = root.getAsJsonObject("ignored");
                    if (ignoredData != null) {
                        for (var entry : ignoredData.entrySet()) {
                            Path path = Path.of(entry.getKey());
                            Set<Item> existing = ignoredItems.computeIfAbsent(path, k -> new HashSet<>());
                            JsonObject itemsObj = entry.getValue().getAsJsonObject();
                            for (String itemId : itemsObj.keySet()) {
                                Item item = Registries.ITEM.get(Identifier.of(itemId));
                                if (item != null) existing.add(item);
                            }

                        }
                    }
                    // 鍔犺浇 replacements
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
                    // 鍔犺浇 entries
                    JsonArray entriesArr = root.getAsJsonArray("entries");
                    if (entriesArr != null) {
                        for (JsonElement elem : entriesArr) {
                            JsonObject obj = elem.getAsJsonObject();
                            String name = obj.get("name").getAsString();
                            // 项目文件夹持久化时 path 为空串，必须还原为 null，
                            // 否则会被当成假文件路径，导致上传区域【原材料表】走错加载分支
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
                        LitematListMod.LOGGER.info("Loaded persistence data ({0})", entriesArr.size());
                    }
                    // 鍔犺浇 pinnedItems锛堟潗鏂欐竻鍗曚腑鐨勭疆椤剁墿鍝侊紝鍚堝苟鍒板凡鏈夐泦鍚堥伩鍏嶈鐩?currentPinned 寮曠敤锛?
                    JsonObject pinnedData = root.getAsJsonObject("pinnedItems");
                    if (pinnedData != null) {
                        for (var entry : pinnedData.entrySet()) {
                            Path path = Path.of(entry.getKey());
                            Set<Item> existing = pinnedItems.computeIfAbsent(path, k -> new HashSet<>());
                            JsonArray arr = entry.getValue().getAsJsonArray();
                            for (JsonElement e : arr) {
                                Item item = Registries.ITEM.get(Identifier.of(e.getAsString()));
                                if (item != null) existing.add(item);
                            }

                        }
                    }
                    // 鍔犺浇灞忓箷寮€鍏崇姸鎬?
                    JsonObject screenData = root.getAsJsonObject("screenSettings");
                    if (screenData != null) {
                        if (screenData.has("hideNoMissing")) persistedHideNoMissing = screenData.get("hideNoMissing").getAsBoolean();
                        if (screenData.has("allowPin")) persistedAllowPin = screenData.get("allowPin").getAsBoolean();
                        if (screenData.has("detailSortMode")) persistedDetailSortMode = screenData.get("detailSortMode").getAsString();
                        if (screenData.has("projectSortMode")) persistedProjectSortMode = screenData.get("projectSortMode").getAsString();
                        if (screenData.has("hudVisible")) persistedHudVisible = screenData.get("hudVisible").getAsBoolean();
                        if (screenData.has("hudX")) persistedHudX = screenData.get("hudX").getAsInt();
                        if (screenData.has("hudY")) persistedHudY = screenData.get("hudY").getAsInt();
                        if (screenData.has("hudFontSize")) persistedHudFontSize = screenData.get("hudFontSize").getAsInt();
                        if (screenData.has("hudBgAlpha")) persistedHudBgAlpha = screenData.get("hudBgAlpha").getAsInt();
                        if (screenData.has("hudMaxLines")) persistedHudMaxLines = screenData.get("hudMaxLines").getAsInt();
                        if (screenData.has("hudShowBox")) persistedHudShowBox = screenData.get("hudShowBox").getAsBoolean();
                        if (screenData.has("hudShowGroup")) persistedHudShowGroup = screenData.get("hudShowGroup").getAsBoolean();
                    }
                    // 鍔犺浇鍘熸潗鏂欏垎鏋愭暟鎹?
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
                LitematListMod.LOGGER.info("Loaded persistence: {0} ignored sets, {1} replacement maps", ignoredItems.size(), replacements.size());
            } catch (Exception e) {
                LitematListMod.LOGGER.error("Failed to load persistence", e);
            }
        }
    }

    /** 鍒囨崲涓栫晫鏃朵繚瀛樻棫鏁版嵁骞跺姞杞芥柊鏁版嵁 */
    public static void switchToWorld(String newWorldId) {
        // 淇濆瓨鏃т笘鐣屾暟鎹?
        if (currentWorldId != null && persistenceFile != null) {
            savePersistence();
        }
        // 娓呯悊鍐呭瓨鏁版嵁
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
        // 绂诲紑涓栫晫鏃跺叧闂璈UD锛岄伩鍏嶆柊瀛樻。涓璈UD鐘舵€佹畫鐣?
        persistedHudVisible = false;
        MaterialListHudRenderer.clearHudItems();
        // 鏍囪闇€瑕侀噸鏂板姞杞?
        persistenceLoaded = false;
        persistenceFile = null;
        currentWorldId = null;
        // 鍔犺浇鏂颁笘鐣屾暟鎹?
        if (newWorldId != null) {
            ensurePersistenceLoaded(newWorldId);
        }
    }

    /** 淇濆瓨鎵€鏈夋寔涔呭寲鏁版嵁锛堝拷鐣ャ€佹浛鎹€佹潯鐩垪琛級 */
    public static void savePersistence() {
        if (persistenceFile == null) return;
        try {
            JsonObject root = new JsonObject();

            // ignored
            JsonObject ignoredData = new JsonObject();
            for (var entry : ignoredItems.entrySet()) {
                JsonObject items = new JsonObject();
                for (Item item : entry.getValue()) {
                    Identifier id = Registries.ITEM.getId(item);
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
                if (le.uploaded) obj.addProperty("uploaded", true);
                entriesArr.add(obj);
            }
            root.add("entries", entriesArr);

            // pinnedItems锛堟潗鏂欐竻鍗曚腑鐨勭疆椤剁墿鍝侊級
            JsonObject pinnedData = new JsonObject();
            for (var entry : pinnedItems.entrySet()) {
                JsonArray arr = new JsonArray();
                for (Item item : entry.getValue()) {
                    Identifier id = Registries.ITEM.getId(item);
                    if (id != null) arr.add(id.toString());
                }
                if (!arr.isEmpty()) {
                    pinnedData.add(entry.getKey().toString(), arr);
                }
            }
            root.add("pinnedItems", pinnedData);

            // 鍘熸潗鏂欏垎鏋愭暟鎹?
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

            // 灞忓箷寮€鍏崇姸鎬?
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
            screenData.addProperty("hudShowBox", persistedHudShowBox);
            screenData.addProperty("hudShowGroup", persistedHudShowGroup);
            root.add("screenSettings", screenData);

            // 纭繚鐩綍瀛樺湪
            java.nio.file.Files.createDirectories(persistenceFile.getParent());
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(persistenceFile.toFile()), java.nio.charset.StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            LitematListMod.LOGGER.info("Saved persistence data ({0}) entries", entriesArr.size());
        } catch (Exception e) {
            LitematListMod.LOGGER.error("Failed to save persistence", e);
        }
    }

    /** 鑾峰彇鏇挎崲鐗╁搧锛堝鏋滄湁锛夛紝鍚﹀垯杩斿洖鍘熺墿鍝?*/
    public static Item getReplacement(Path filePath, Item original) {
        ensurePersistenceLoaded();
        String pathKey = filePath.toString();
        Map<String, String> fileReplacements = replacements.get(pathKey);
        if (fileReplacements == null) return original;
        Identifier origId = Registries.ITEM.getId(original);
        if (origId == null) return original;
        String replId = fileReplacements.get(origId.toString());
        if (replId == null) return original;
        return Registries.ITEM.get(Identifier.of(replId));
    }

    /** 璁剧疆鏇挎崲鏄犲皠锛堜緵澶栭儴濡?ProjectSummaryScreen 璋冪敤锛?*/
    public static void setReplacement(Path filePath, Item original, Item replacement) {
        ensurePersistenceLoaded();
        String pathKey = filePath.toString();
        Map<String, String> fileReplacements = replacements.computeIfAbsent(pathKey, k -> new HashMap<>());
        Identifier origId = Registries.ITEM.getId(original);
        Identifier newId = Registries.ITEM.getId(replacement);
        if (origId != null && newId != null) {
            fileReplacements.put(origId.toString(), newId.toString());
        }
    }

    /** 绉婚櫎鏇挎崲鏄犲皠锛堜緵澶栭儴濡?ProjectSummaryScreen 璋冪敤锛?*/
    public static void removeReplacement(Path filePath, Item original) {
        ensurePersistenceLoaded();
        String pathKey = filePath.toString();
        Map<String, String> fileReplacements = replacements.get(pathKey);
        if (fileReplacements != null) {
            Identifier origId = Registries.ITEM.getId(original);
            if (origId != null) {
                fileReplacements.remove(origId.toString());
                if (fileReplacements.isEmpty()) {
                    replacements.remove(pathKey);
                }
            }
        }
    }

    /** 渚涘閮ㄨ皟鐢紝鍒濆鍖栨寔涔呭寲 */
    public static void initPersistence() {
        ensurePersistenceLoaded();
    }

    /** 娓呯悊鎸囧畾璺緞鐨勬寔涔呭寲鏁版嵁锛堟浛鎹㈡槧灏勫拰蹇界暐鐗╁搧锛?*/
    public static void cleanupPathData(Path filePath) {
        ensurePersistenceLoaded();
        String pathKey = filePath.toString();
        replacements.remove(pathKey);
        ignoredItems.remove(filePath);
        LitematListMod.LOGGER.info("Loaded persistence for: {0}", pathKey);
    }

    // ---- 鏇挎崲妯″紡 ----
    private int replacingRow = -1; // 褰撳墠澶勪簬鏇挎崲妯″紡鐨勮绱㈠紩

    // ---- 闅愯棌涓嶇己鏉愭枡 ----
    private boolean hideNoMissing = persistedHideNoMissing;
    // 闅愯棌涓嶇己鏉愭枡鎸夐挳浣嶇疆锛堢敤浜庤嚜瀹氫箟娓叉煋寮€/鍏抽鑹诧級
    private int hideBtnX, hideBtnY, hideBtnW;
    private String hideBtnLabel = "";
    // ---- 鎵嬪姩閲嶆帓 ----
    private static final Map<Path, List<Item>> reorderedItems = new HashMap<>();
    private int draggedRow = -1;
    private int dragStartY = 0;
    private boolean isDragging = false;

    // ---- 鍒椾綅缃?----
    private int colTotalX;
    private int colHaveX;
    private int colMissX;

    // ---- 鎺掑簭 ----
    private SortMode sortMode = SortMode.NONE;

    private void initSortMode() {
        try { sortMode = SortMode.valueOf(persistedDetailSortMode); } catch (Exception e) { sortMode = SortMode.NONE; }
    }

    private enum SortMode {
        NONE(""),
        TOTAL_DESC("\u25BC"),
        TOTAL_ASC("\u25B2"),
        HAVE_DESC("\u25BC"),
        HAVE_ASC("\u25B2"),
        MISS_DESC("\u25BC"),
        MISS_ASC("\u25B2");

        final String arrow;
        SortMode(String arrow) { this.arrow = arrow; }
    }

    // ---- 鎸夐挳甯冨眬 ----
    private static final int BTN_REPLACE_W = 52;
    private static final int BTN_IGNORE_W = 52;
    private static final int BTN_CANCEL_W = 52;
    private static final int BTN_GAP = 2;
    private static final int BTN_RIGHT_MARGIN = 12;

    // ---- 鏍囪鏉愭枡 ----
    private static final Identifier PIN_EMPTY = Identifier.of("litematlist", "textures/gui/pin/empty.png");
    private static final Identifier PIN_FAVORITE = Identifier.of("litematlist", "textures/gui/pin/favorite.png");
    private static final Identifier PIN_HOVERED_EMPTY = Identifier.of("litematlist", "textures/gui/pin/hovered_empty.png");
    private static final Identifier PIN_HOVERED_FAVORITE = Identifier.of("litematlist", "textures/gui/pin/hovered_favorite.png");
    private static final Identifier PIN_FOCUSED = Identifier.of("litematlist", "textures/gui/pin/focused_outline.png");
    private static final Identifier SEARCH_ICON = Identifier.of("litematlist", "textures/gui/search.png");

    private boolean allowPin = persistedAllowPin;
    private int pinBtnX, pinBtnY, pinBtnW;
    private String pinBtnLabel = "";
    private static final Map<Path, Set<Item>> pinnedItems = new HashMap<>();
    private Set<Item> currentPinned;

    // ---- 鎼滅储 ----
    private boolean searchActive = false;
    private String searchText = "";
    private TextFieldWidget searchField;
    private boolean fromUploadArea = false; // 鏄惁浠庝笂浼犲尯鍩熻繘鍏ワ紙绂佺敤鏇挎崲/蹇界暐锛?

    // 鎸夐挳鍖哄煙宸﹁竟鐣?
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

    /** 褰撳墠鏉＄洰鏄惁宸蹭笂浼犲埌涓婁紶鍖哄煙 */
    private boolean isEntryUploaded() {
        MaterialListScreen.LoadedEntry uploaded = MaterialListScreen.getUploadedEntry();
        return uploaded != null && uploaded.filePath() != null && uploaded.filePath().equals(filePath);
    }

    /** 濡傛灉褰撳墠鏉＄洰宸蹭笂浼狅紝绔嬪嵆閲嶆柊娉ㄥ叆鏉愭枡鍒楄〃 */
    private void reinjectIfUploaded() {
        MaterialListScreen.LoadedEntry uploaded = MaterialListScreen.getUploadedEntry();
        if (uploaded != null && uploaded.filePath() != null && uploaded.filePath().equals(filePath)) {
            MaterialListInjector.injectFromEntry(uploaded);
            MaterialListInjector.markInjected(uploaded);
            LitematListMod.LOGGER.info("[MaterialDetailScreen] 宸蹭笂浼犳潯鐩搷浣滃悗閲嶆柊娉ㄥ叆: {}", filePath);
        }
    }

    /**
     * 全局解析设置（如「是否计算容器数据」）切换后重新加载并刷新界面。
     */
    public void refreshAfterGlobalSettingChange() {
        this.loaded = false;
        initGui();
    }

    @Override
    public void initGui() {
        super.initGui();
        this.replacingRow = -1; // 姣忔閲嶅缓鐣岄潰鏃舵竻闄ら€夋嫨妯″紡
        this.visibleRows = (this.height - 100) / ROW_HEIGHT;

        if (!loaded) {
            loadMaterials();
            loaded = true;
        }
        // 閲嶆柊缁戝畾 currentIgnored/currentPinned锛岀‘淇濆畠浠紩鐢?ignoredItems/pinnedItems 涓殑瀹為檯闆嗗悎
        // 锛堝洜涓?ensurePersistenceLoaded 鍙兘鍦?loadMaterials 涓娆¤皟鐢紝闇€瑕佸埛鏂板紩鐢級
        this.currentIgnored = ignoredItems.computeIfAbsent(filePath, k -> new HashSet<>());
        this.currentPinned = pinnedItems.computeIfAbsent(filePath, k -> new HashSet<>());
        calculateColumnPositions();

        this.buttonAreaStart = this.width - BTN_RIGHT_MARGIN - BTN_IGNORE_W - BTN_GAP - BTN_REPLACE_W - BTN_GAP - BTN_CANCEL_W - BTN_GAP;

        int buttonY = this.height - 38;

        ButtonGeneric backButton = new ButtonGeneric(this.width / 2 - 60, buttonY, 120, 20, I18n.tr("litematlist.button.back"));
        backButton.setRenderDefaultBackground(true);
        this.addButton(backButton, (IButtonActionListener) (btn, mb) ->
                MinecraftClient.getInstance().setScreen(parent));

        int ignoredCount = currentIgnored.size();
        String ignoredLabel = ignoredCount > 0 ? I18n.tr("litematlist.button.ignored_count", ignoredCount) : I18n.tr("litematlist.button.ignored");
        int ignoredBtnW = Math.max(60, this.textRenderer.getWidth(ignoredLabel) + 16);

        // 闅愯棌涓嶇己鏉愭枡鎸夐挳 鈥斺€?鏀惧湪"鐗╁搧"琛ㄥご涓婃柟
        String hideLabel = hideNoMissing ? I18n.tr("litematlist.button.hide_no_missing_on") : I18n.tr("litematlist.button.hide_no_missing_off");
        int hideBtnW = Math.max(60, this.textRenderer.getWidth(hideLabel) + 16);
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
        // 淇濆瓨鎸夐挳浣嶇疆鐢ㄤ簬鑷畾涔夋覆鏌?
        this.hideBtnX = hideBtnX;
        this.hideBtnY = hideBtnY;
        this.hideBtnW = hideBtnW;
        this.hideBtnLabel = hideLabel;

        // 鍏佽鏍囪鏉愭枡鎸夐挳
        String pinLabel = allowPin ? I18n.tr("litematlist.button.allow_pin_on") : I18n.tr("litematlist.button.allow_pin_off");
        int pinBtnW = Math.max(60, this.textRenderer.getWidth(pinLabel) + 16);
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

        // 瀵煎嚭鎸夐挳
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

        // 鎼滅储妗?
        int searchIconX = allowPin ? 33 : 15;
        if (searchField == null) {
            this.searchField = new TextFieldWidget(this.textRenderer, searchIconX + 18, HEADER_Y, 80, 14, Text.empty());
            this.searchField.setMaxLength(50);
            this.searchField.setDrawsBackground(false);
            this.searchField.setText(searchText);
            this.addSelectableChild(this.searchField);
        } else {
            this.searchField.setX(searchIconX + 18);
            this.searchField.setY(HEADER_Y);
            this.searchField.setText(searchText);
            this.addSelectableChild(this.searchField);
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
            MinecraftClient.getInstance().setScreen(
                    new IgnoredItemsScreen(this, schematicName, filePath, filePath.toString(), currentIgnored, ignoredTotals));
        });

        createRowButtons();
    }

    private void calculateColumnPositions() {
        int maxTotalWidth = this.textRenderer.getWidth(I18n.tr("litematlist.header.total"));
        int maxHaveWidth = this.textRenderer.getWidth(I18n.tr("litematlist.header.have"));
        int maxMissWidth = this.textRenderer.getWidth(I18n.tr("litematlist.header.missing"));
        int maxNameWidth = this.textRenderer.getWidth(I18n.tr("litematlist.header.item"));

        for (MaterialRow row : rows) {
            maxNameWidth = Math.max(maxNameWidth, this.textRenderer.getWidth(row.name));
            maxTotalWidth = Math.max(maxTotalWidth, this.textRenderer.getWidth(String.valueOf(row.totalCount)));
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
            // 浼樺厛浠庡鍏ョ紦瀛樺姞杞斤紙txt 鏂囦欢瀵煎叆鐨勬潗鏂欙級
            List<LitematicReader.MaterialEntry> entries;
            if (importedMaterials.containsKey(filePath)) {
                entries = importedMaterials.get(filePath);
                LitematListMod.LOGGER.info("Loaded {} entries ({} ignored)", filePath, entries.size());
            } else if (filePath.toString().toLowerCase().endsWith(".txt") || filePath.toString().toLowerCase().endsWith(".json") || filePath.toString().toLowerCase().endsWith(".csv")) {
                // txt/json 鏂囦欢锛氶噸鍚悗缂撳瓨宸蹭涪澶憋紝閲嶆柊瑙ｆ瀽
                LitematListMod.LOGGER.info("Material list synced: {0}", filePath);
                MaterialListImporter.ImportResult result = MaterialListImporter.importFromFile(filePath);
                if (result != null) {
                    entries = result.materials();
                    importedMaterials.put(filePath, entries); // 閲嶆柊缂撳瓨
                } else {
                    LitematListMod.LOGGER.error("Failed to sync txt material list: {0}", filePath);
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
                // 搴旂敤宸蹭繚瀛樼殑鏇挎崲
                Item replaced = getReplacement(filePath, item);
                rows.add(new MaterialRow(
                        new ItemStack(replaced, entry.totalCount()),
                        replaced.getName().getString(),
                        entry.totalCount(),
                        replaced,
                        item, // originalItem 濮嬬粓鏄師鐞嗗浘鍘熷鐗╁搧
                        entry.missingCount()
                ));
            }
            LitematListMod.LOGGER.info("Synced {} rows, {} ignored", rows.size(), currentIgnored.size());
        } catch (Exception e) {
            LitematListMod.LOGGER.error("Failed to sync material list: {0}", filePath, e);
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

            // 鍙栨秷鏇挎崲鎸夐挳锛堢墿鍝佸凡琚浛鎹㈡椂鏄剧ず锛屽彲鎭㈠涓哄師鐞嗗浘鍘熷鐗╁搧锛?
            if (row.item != row.originalItem) {
                ButtonGeneric cancelBtn = new ButtonGeneric(cancelX, rowY + 1, BTN_CANCEL_W, 20, I18n.tr("litematlist.button.cancel_replace"));
                cancelBtn.setRenderDefaultBackground(true);
                this.addButton(cancelBtn, (IButtonActionListener) (b, mb) -> {
                    restoreOriginalItem(targetRow);
                    initGui();
                });
            }

            // 鏇挎崲鎸夐挳
            {
                ButtonGeneric replaceBtn = new ButtonGeneric(replaceX, rowY + 1, BTN_REPLACE_W, 20, I18n.tr("litematlist.button.replace"));
                replaceBtn.setRenderDefaultBackground(true);
                this.addButton(replaceBtn, (IButtonActionListener) (b, mb) -> {
                    replacingRow = visibleIdx;
                    MinecraftClient.getInstance().setScreen(
                            new BlockPickerScreen(this, newItem -> replaceItem(targetRow, newItem)));
                });
            }

            // 蹇界暐鎸夐挳锛堢疆椤舵潗鏂欎笉鏄剧ず蹇界暐鎸夐挳锛?
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
        replacingRow = -1; // 鐐瑰嚮蹇界暐鏃跺彇娑堟浛鎹㈡ā寮?
        List<MaterialRow> displayRows = getSortedRows();
        if (visibleIdx >= 0 && visibleIdx < displayRows.size()) {
            MaterialRow row = displayRows.get(visibleIdx);
            if (allowPin && currentPinned.contains(row.originalItem)) return;
            currentIgnored.add(row.item);
            // 娓呴櫎缂撳瓨鐨勫師鏉愭枡鍒嗘瀽鏁版嵁锛屼笅涓€娆℃墦寮€鍘熸潗鏂欏垪琛ㄦ椂閲嶆柊鍒嗘瀽锛堟帓闄ゅ凡蹇界暐鐗╁搧锛?
            String pathKey = filePath.toString();
            rawMaterials.remove(pathKey);
            rawMaterialRedundancy.remove(pathKey);
            savePersistence(); // 绔嬪嵆鎸佷箙鍖栧拷鐣?
            reinjectIfUploaded(); // 涓婁紶鏉＄洰闇€绔嬪嵆閲嶆柊娉ㄥ叆
            MaterialListHudRenderer.refreshHud(); // 鏇存柊HUD
            LitematListMod.LOGGER.info("Replaced: {0} ({1})", row.name, row.item);
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
                    String itemId = Registries.ITEM.getId(row.item).toString();
                    if (!PinyinSearch.matches(searchText, row.name, itemId)) continue;
                }
                visible.add(row);
            }
        }
        // 搴旂敤鎵嬪姩閲嶆帓椤哄簭
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
        // 鏍囪鏉愭枡妯″紡锛氬皢宸叉爣璁扮殑鏉愭枡鎺掑埌鏈€鍓嶉潰
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return 0;
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
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
        return String.format("%d = %d*1728 + %d*64 + %d | %d*64+%d | %.2f shulkers",
                count, shulker, stacks, items,
                shulker * 27 + stacks, items,
                shulkerFloat);
    }

    private List<MaterialRow> getSortedRows() {
        List<MaterialRow> visible = getVisibleRowsOnly();

        // 鍒嗙缃《鍜岄潪缃《
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
            // 涓€娆℃€ф帓搴?
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

    // ==================== 鏇挎崲閫昏緫 ====================

    private void replaceItem(MaterialRow target, Item newItem) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).item == target.item) {
                MaterialRow old = rows.get(i);
                rows.set(i, new MaterialRow(
                        new ItemStack(newItem, old.totalCount),
                        newItem.getName().getString(),
                        old.totalCount,
                        newItem,
                        old.originalItem, // 淇濈暀鍘熷鐗╁搧
                        old.missingCount
                ));
                break;
            }
        }
        // 绔嬪嵆鎸佷箙鍖栨浛鎹?
        String pathKey = filePath.toString();
        Map<String, String> fileReplacements = replacements.computeIfAbsent(pathKey, k -> new HashMap<>());
        Identifier origId = Registries.ITEM.getId(target.originalItem);
        Identifier newId = Registries.ITEM.getId(newItem);
        if (origId != null && newId != null) {
            fileReplacements.put(origId.toString(), newId.toString());
        }
        savePersistence();
        // 娓呴櫎缂撳瓨鐨勫師鏉愭枡鍒嗘瀽鏁版嵁锛岀‘淇濅笅娆℃墦寮€鍘熸潗鏂欏垪琛ㄦ椂閲嶆柊鍒嗘瀽
        rawMaterials.remove(pathKey);
        rawMaterialRedundancy.remove(pathKey);
        reinjectIfUploaded();
        MaterialListHudRenderer.refreshHud(); // 鏇存柊HUD
        initGui();
    }

    private void restoreOriginalItem(MaterialRow target) {
        for (int i = 0; i < rows.size(); i++) {
            // 閫氳繃 originalItem 鍖归厤锛氭浛鎹㈠悗 target.item 鏄棫鐗╁搧锛宺ows[i].item 鏄柊鐗╁搧锛屼笉鑳界洿鎺?== 姣旇緝
            if (rows.get(i).originalItem == target.originalItem) {
                MaterialRow old = rows.get(i);
                Item orig = old.originalItem;
                rows.set(i, new MaterialRow(
                        new ItemStack(orig, old.totalCount),
                        orig.getName().getString(),
                        old.totalCount,
                        orig,
                        orig,
                        old.missingCount
                ));
                break;
            }
        }
        // 浠庢寔涔呭寲涓Щ闄ゆ浛鎹?
        String pathKey = filePath.toString();
        Map<String, String> fileReplacements = replacements.get(pathKey);
        if (fileReplacements != null) {
            Identifier origId = Registries.ITEM.getId(target.originalItem);
            if (origId != null) {
                fileReplacements.remove(origId.toString());
                if (fileReplacements.isEmpty()) {
                    replacements.remove(pathKey);
                }
            }
        }
        savePersistence();
        // 娓呴櫎缂撳瓨鐨勫師鏉愭枡鍒嗘瀽鏁版嵁锛岀‘淇濅笅娆℃墦寮€鍘熸潗鏂欏垪琛ㄦ椂閲嶆柊鍒嗘瀽
        rawMaterials.remove(pathKey);
        rawMaterialRedundancy.remove(pathKey);
        reinjectIfUploaded();
        MaterialListHudRenderer.refreshHud(); // 鏇存柊HUD
        replacingRow = -1;
    }

    // ==================== 瀵煎嚭 ====================

    private void exportMaterials(String format) {
        try {
            Path mcRoot = MinecraftClient.getInstance().runDirectory.toPath().resolve("litematlist");
            Path dir;
            //  if (this.filePath != null && this.filePath.getParent() != null && this.filePath.getParent().getParent() != null) {
                Path grandParent = this.filePath.getParent().getParent();
                if ("project".equals(grandParent.getFileName().toString())) {
                    // 椤圭洰鏂囦欢澶瑰鍑? .minecraft/litematlist/<project>/
                    String projectName = this.filePath.getParent().getFileName().toString();
                    dir = mcRoot.resolve(projectName);
                } else {
                    dir = mcRoot;
                }

            //     dir = mcRoot;
            // }
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
            LitematListMod.LOGGER.info("Exported material list: {}", filePath);

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                String fmt = format.toUpperCase();
                Text link = Text.literal(fileName)
                        .styled(style -> style
                                .withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.OPEN_FILE, filePath.toString()))
                                .withUnderline(true)
                                .withColor(0x55FFFF));
                Text msg = Text.translatable("litematlist.message.exported", fmt).append(link);
                client.player.sendMessage(msg);
            }
        } catch (Exception e) {
            LitematListMod.LOGGER.error("Export failed", e);
        }
    }

    private void exportTxt(Path filePath) throws IOException {
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(filePath.toFile()), StandardCharsets.UTF_8))) {
            String title = "鍘熺悊鍥?" + schematicName + "'鏉愭枡鍒楄〃";
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
            String title = "鍘熺悊鍥?" + schematicName + "'鏉愭枡鍒楄〃";
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
                Identifier id = Registries.ITEM.getId(row.item);
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
            // 鏍囬琛岋紙涓?Litematica 瀵煎嚭鐨?CSV 鏍煎紡涓€鑷达級
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

    // ==================== 娓叉煋 ====================

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(drawContext, mouseX, mouseY, partialTicks);

        if (rows.isEmpty()) {
            drawContext.drawCenteredTextWithShadow(
                    this.textRenderer, I18n.tr("litematlist.error.no_data"),
                    this.width / 2, this.height / 2 - 10, 0xFFFFFFFF);
            super.render(drawContext, mouseX, mouseY, partialTicks);
            return;
        }

        List<MaterialRow> displayRows = getSortedRows();

        int listRight = this.width - 8; // 鑳屾櫙妗嗚鐩栨寜閽尯鍩?
        int listBottom = LIST_TOP + visibleRows * ROW_HEIGHT;

        // ===== 绗?灞傦細鑳屾櫙锛堝湪鎸夐挳涔嬪墠缁樺埗锛岄伩鍏嶇洊浣忔寜閽級 =====
        drawContext.fill(8, LIST_TOP - 2, listRight, listBottom + 2, 0x40000000);

        hoveredRow = -1;
        hoveredColumn = -1;
        int maxIdx = Math.min(displayRows.size(), scrollOffset + visibleRows);

        for (int i = scrollOffset; i < maxIdx; i++) {
            MaterialRow row = displayRows.get(i);
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;
            boolean isReplacing = (replacingRow == i);

            if (isReplacing) {
                drawContext.fill(10, rowY, listRight, rowY + ROW_HEIGHT, 0x60FFFF00);
            } else if (mouseX >= 10 && mouseX <= listRight
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                hoveredRow = i;
                drawContext.fill(10, rowY, listRight, rowY + ROW_HEIGHT, 0x40FFFFFF);

                int totalW = this.textRenderer.getWidth(String.valueOf(row.totalCount));
                int haveW = this.textRenderer.getWidth(String.valueOf(getPlayerItemCount(row.item)));
                int missW = this.textRenderer.getWidth(String.valueOf(Math.max(0, row.totalCount - getPlayerItemCount(row.item))));

                if (mouseX >= colTotalX - 10 && mouseX <= colTotalX + totalW + 10) {
                    hoveredColumn = 0;
                } else if (mouseX >= colHaveX - 10 && mouseX <= colHaveX + haveW + 10) {
                    hoveredColumn = 1;
                } else if (mouseX >= colMissX - 10 && mouseX <= colMissX + missW + 10) {
                    hoveredColumn = 2;
                }
            } else if ((i - scrollOffset) % 2 == 0) {
                drawContext.fill(10, rowY, listRight, rowY + ROW_HEIGHT, 0x10FFFFFF);
            }
        }

        // 琛ㄥご鑳屾櫙
        drawContext.fill(8, HEADER_Y, listRight, HEADER_Y + HEADER_HEIGHT, 0x60000000);

        int sortColX = switch (sortMode) {
            case TOTAL_DESC, TOTAL_ASC -> colTotalX;
            case HAVE_DESC, HAVE_ASC -> colHaveX;
            case MISS_DESC, MISS_ASC -> colMissX;
            default -> -1;
        };
        if (sortColX > 0) {
            drawContext.fill(sortColX - 20, HEADER_Y, sortColX + 30, HEADER_Y + HEADER_HEIGHT, 0x40FFFF88);
        }

        // ===== 绗?灞傦細鎸夐挳锛堝湪鑳屾櫙涔嬩笂锛?=====
        super.render(drawContext, mouseX, mouseY, partialTicks);

        // 鑷畾涔夋覆鏌撻殣钘忔寜閽枃瀛楋紙寮€=缁胯壊锛屽叧=绾㈣壊锛?
        if (!hideBtnLabel.isEmpty()) {
            int colonIdx = hideBtnLabel.indexOf(":");
            if (colonIdx < 0) colonIdx = hideBtnLabel.indexOf("：");
            if (colonIdx > 0) {
                String prefix = hideBtnLabel.substring(0, colonIdx + 1);
                String status = hideBtnLabel.substring(colonIdx + 1);
                int prefixW = this.textRenderer.getWidth(prefix);
                int textX = hideBtnX + 4;
                int textY = hideBtnY + 5;
                drawContext.drawTextWithShadow(this.textRenderer, prefix, textX, textY, 0xFFFFFFFF);
                int statusColor = hideNoMissing ? 0xFF55FF55 : 0xFFFF5555;
                drawContext.drawTextWithShadow(this.textRenderer, status, textX + prefixW, textY, statusColor);
            }
        }
        // 鑷畾涔夋覆鏌撴爣璁版寜閽枃瀛楋紙寮€=缁胯壊锛屽叧=绾㈣壊锛?
        if (!pinBtnLabel.isEmpty()) {
            int colonIdx = pinBtnLabel.indexOf(":");
            if (colonIdx < 0) colonIdx = pinBtnLabel.indexOf("：");
            if (colonIdx > 0) {
                String prefix = pinBtnLabel.substring(0, colonIdx + 1);
                String status = pinBtnLabel.substring(colonIdx + 1);
                int prefixW = this.textRenderer.getWidth(prefix);
                int textX = pinBtnX + 4;
                int textY = pinBtnY + 5;
                drawContext.drawTextWithShadow(this.textRenderer, prefix, textX, textY, 0xFFFFFFFF);
                int statusColor = allowPin ? 0xFF55FF55 : 0xFFFF5555;
                drawContext.drawTextWithShadow(this.textRenderer, status, textX + prefixW, textY, statusColor);
            }
        }

        // ===== 绗?灞傦細鏂囧瓧鍜岀墿鍝佸浘鏍囷紙鍦ㄦ渶涓婂眰锛?=====
        for (int i = scrollOffset; i < maxIdx; i++) {
            MaterialRow row = displayRows.get(i);
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;

            int iconX = 15;
            int nameX = 35;

            // 鏍囪鏉愭枡妯″紡锛氬厛鐢绘爣璁板浘鏍囷紝鍐嶅亸绉荤墿鍝佸浘鏍囧拰鏂囧瓧
            if (allowPin) {
                boolean isPinned = currentPinned.contains(row.originalItem);
                boolean pinHovered = mouseX >= 15 && mouseX <= 31 && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
                Identifier pinTexture;
                if (isPinned) {
                    pinTexture = pinHovered ? PIN_HOVERED_FAVORITE : PIN_FAVORITE;
                } else {
                    pinTexture = pinHovered ? PIN_HOVERED_EMPTY : PIN_EMPTY;
                }
                drawContext.drawTexture(pinTexture, 15, rowY + 2, 0.0f, 0.0f, 16, 16, 16, 16);
                iconX = 33;
                nameX = 53;
            }

            drawContext.drawItem(row.itemStack, iconX, rowY + 2);
            drawContext.drawTextWithShadow(this.textRenderer, row.name, nameX, rowY + 5, 0xFFFFFFFF);
            drawContext.drawTextWithShadow(this.textRenderer, String.valueOf(row.totalCount), colTotalX, rowY + 5, 0xFFFFFFFF);

            int haveCount = getPlayerItemCount(row.item);
            drawContext.drawTextWithShadow(this.textRenderer, String.valueOf(haveCount), colHaveX, rowY + 5, 0xFFFFFFFF);

            int missCount = Math.max(0, row.totalCount - haveCount);
            int missColor = missCount == 0 ? 0xFF55FF55
                          : (haveCount >= missCount ? 0xFFFFAA00 : 0xFFFF5555);
            drawContext.drawTextWithShadow(this.textRenderer, String.valueOf(missCount), colMissX, rowY + 5, missColor);
        }

        // 琛ㄥご鏂囧瓧
        int headerNameX = allowPin ? 53 : 35;
        // 鎼滅储鍥炬爣
        int searchIconX = allowPin ? 33 : 15;
        drawContext.drawTexture(SEARCH_ICON, searchIconX, HEADER_Y + 1, 0.0f, 0.0f, 14, 14, 14, 14);
        // 鍏堢敾"鐗╁搧"鏂囧瓧锛堟悳绱㈡鎵撳紑鏃惰鐩栧湪涓婇潰锛?
        if (!searchActive) {
            drawContext.drawTextWithShadow(this.textRenderer, I18n.tr("litematlist.header.item"), headerNameX, HEADER_Y + 3, 0xFFFFFFFF);
        }
        if (searchActive && searchField != null) {
            drawContext.fill(searchIconX + 18 - 2, HEADER_Y - 1, searchIconX + 18 + 82, HEADER_Y + 15, 0xFF000000);
            drawContext.fill(searchIconX + 18, HEADER_Y, searchIconX + 18 + 80, HEADER_Y + 14, 0xFF222222);
            String st = searchField.getText();
            drawContext.drawText(this.textRenderer, st, searchIconX + 21, HEADER_Y + 3, 0xFFFFFFFF, false);
            if (searchField.isFocused() && System.currentTimeMillis() % 1000 < 500) {
                int cp = searchField.getCursor();
                String bc = st.substring(0, Math.min(cp, st.length()));
                int cx = searchIconX + 21 + this.textRenderer.getWidth(bc);
                drawContext.fill(cx, HEADER_Y + 2, cx + 1, HEADER_Y + 12, 0xFFFFFFFF);
            }
        }
        drawContext.drawTextWithShadow(this.textRenderer,
                I18n.tr("litematlist.header.total") + (sortMode == SortMode.TOTAL_DESC || sortMode == SortMode.TOTAL_ASC ? sortMode.arrow : ""),
                colTotalX, HEADER_Y + 3, 0xFFFFFFFF);
        drawContext.drawTextWithShadow(this.textRenderer,
                I18n.tr("litematlist.header.have") + (sortMode == SortMode.HAVE_DESC || sortMode == SortMode.HAVE_ASC ? sortMode.arrow : ""),
                colHaveX, HEADER_Y + 3, 0xFFFFFFFF);
        drawContext.drawTextWithShadow(this.textRenderer,
                I18n.tr("litematlist.header.missing") + (sortMode == SortMode.MISS_DESC || sortMode == SortMode.MISS_ASC ? sortMode.arrow : ""),
                colMissX, HEADER_Y + 3, 0xFFFFFFFF);

        // 浠呭湪鎮仠鏁板€煎垪鏃舵樉绀洪噺绾?tooltip
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
            drawContext.drawTooltip(this.textRenderer, Text.literal(tooltip), mouseX, mouseY);
        }

        if (displayRows.size() > visibleRows) {
            String scrollInfo = (scrollOffset + 1) + "-" +
                    Math.min(scrollOffset + visibleRows, displayRows.size()) +
                    " / " + displayRows.size();
            drawContext.drawCenteredTextWithShadow(this.textRenderer,
                    scrollInfo, this.width / 2, listBottom + 5, 0xFFFFFFFF);

            // 婊氬姩鏉?
            int scrollbarX = this.width - 6;
            int scrollbarWidth = 3;
            int listHeight = listBottom - LIST_TOP;
            // 杞ㄩ亾
            drawContext.fill(scrollbarX, LIST_TOP, scrollbarX + scrollbarWidth, listBottom, 0x30FFFFFF);
            // 婊戝潡
            float ratio = (float) visibleRows / displayRows.size();
            int thumbHeight = Math.max(6, (int)(listHeight * ratio));
            int maxScroll = Math.max(1, displayRows.size() - visibleRows);
            int thumbY = LIST_TOP + (int)((listHeight - thumbHeight) * (float) scrollOffset / maxScroll);
            drawContext.fill(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, 0x80FFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (super.mouseClicked(mouseX, mouseY, mouseButton)) return true;

        
        mouseX = (int) mouseX;
        mouseY = (int) mouseY;
        
        int button = mouseButton;

        // 鎷栨嫿浜嬩欢澶勭悊锛坕sDrag=true锛?

        if (button == 0) {
            // 鎼滅储鍥炬爣鐐瑰嚮
            int searchIconX = allowPin ? 33 : 15;
            if (mouseX >= searchIconX && mouseX <= searchIconX + 16
                    && mouseY >= HEADER_Y && mouseY <= HEADER_Y + HEADER_HEIGHT) {
                searchActive = !searchActive;
                if (searchActive && searchField != null) {
                    searchField.setFocused(true);
                }
                if (!searchActive) {
                    searchText = "";
                    if (searchField != null) searchField.setText("");
                }
                initGui();
                return true;
            }
            if (mouseY >= HEADER_Y && mouseY <= HEADER_Y + HEADER_HEIGHT) {
                handleHeaderClick(mouseX, mouseY);
                return true;
            }

            // 鏍囪鏉愭枡鐐瑰嚮
            if (allowPin) {
                List<MaterialRow> displayRows = getSortedRows();
                int pinRow = getRowAtY((int)mouseY, displayRows);
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

            // 鎷栨嫿妫€娴嬶紙浠呭湪鍚敤鎵嬪姩閲嶆帓鏃讹級
            if (Configs.Generic.ALLOW_MANUAL_REORDER.getBooleanValue()) {
                List<MaterialRow> displayRows = getSortedRows();
                int row = getRowAtY((int)mouseY, displayRows);
                if (row >= 0) {
                    // 鎺掗櫎鎸夐挳鍖哄煙
                    if (mouseX >= 10 && mouseX < this.width - 210) {
                        draggedRow = row;
                        dragStartY = (int)mouseY;
                        isDragging = false;
                        return true;
                    }
                }
            }
        }

        // 婊氬姩鏉＄偣鍑?
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
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
        if (mouseButton == 0) {
            // 滚动条拖拽
            if (scrollbarDragging) {
                List<MaterialRow> displayRows = getSortedRows();
                int listBottom = LIST_TOP + visibleRows * ROW_HEIGHT;
                int trackHeight = listBottom - LIST_TOP;
                int maxScroll = Math.max(1, displayRows.size() - visibleRows);
                scrollOffset = Math.max(0, Math.min(maxScroll,
                        (int) ((float) ((int)mouseY - LIST_TOP) / trackHeight * maxScroll)));
                initGui();
                return true;
            }
            // 行拖拽重排
            if (draggedRow >= 0 && Configs.Generic.ALLOW_MANUAL_REORDER.getBooleanValue()) {
                List<MaterialRow> displayRows = getSortedRows();
                int currentRow = getRowAtY((int)mouseY, displayRows);
                if (currentRow >= 0 && currentRow != draggedRow && Math.abs((int)mouseY - dragStartY) > 5) {
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
                        sortMode = SortMode.NONE;
                    }
                    draggedRow = currentRow;
                    dragStartY = (int)mouseY;
                }
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, mouseButton, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (scrollbarDragging) {
            scrollbarDragging = false;
            return true;
        }
        if (draggedRow >= 0) {
            if (isDragging) {
                saveReorderedItems();
                initGui();
            }
            draggedRow = -1;
            isDragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, mouseButton);
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            if (searchActive) {
                searchActive = false;
                searchText = "";
                if (searchField != null) searchField.setText("");
                initGui();
                return true;
            }
            MinecraftClient.getInstance().setScreen(parent);
            return true;
        }
        if (searchActive && searchField != null && searchField.isFocused()) {
            if (searchField.keyPressed(keyCode, scanCode, modifiers)) {
                searchText = searchField.getText();
                scrollOffset = 0; // 输入字符后立刻回到列表最顶端
                initGui();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchActive && searchField != null && searchField.isFocused()) {
            if (searchField.charTyped(chr, modifiers)) {
                searchText = searchField.getText();
                scrollOffset = 0; // 输入字符后立刻回到列表最顶端
                initGui();
                return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }

    private record MaterialRow(ItemStack itemStack, String name, int totalCount, Item item, Item originalItem, int missingCount) {}
}





