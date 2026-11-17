package com.litematlist.gui;

import com.litematlist.LitematicReader;
import com.litematlist.MaterialListExporter;
import com.litematlist.MaterialListImporter;
import com.litematlist.MaterialListInjector;
import com.litematlist.I18n;
import com.litematlist.LitematListMod;
import com.litematlist.LitematicaBridge;
import com.litematlist.PinyinSearch;
import com.litematlist.RawMaterialAnalyzer;
import com.litematlist.SyncmaticaBridge;
import com.litematlist.config.Configs;
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
import net.minecraft.world.item.Items;
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

import static com.litematlist.gui.MaterialDataStore.*;

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
    /** 名称列最大显示宽度（像素）：过长（如精确物品ID）时截断显示 */
    private static final int MAX_NAME_DISPLAY_WIDTH = 200;
    private int visibleRows;
    private boolean scrollbarDragging = false;
    private boolean loaded = false;
    private int hoveredRow = -1;
    private int hoveredColumn = -1; // 0=总计, 1=已有, 2=缺失

    // ---- 已忽略物品 ----
    private Set<Item> currentIgnored;
    // ---- 已忽略的精确物品（按精确ID区分，与普通物品的按Item区分并存） ----
    private Set<String> currentExactIgnored;

    // ---- 替换模式 ----
    private int replacingRow = -1; // 当前处于替换模式的行索引

    // ---- 隐藏不缺材料 ----
    private boolean hideNoMissing = persistedHideNoMissing;
    // 隐藏不缺材料按钮位置（用于自定义渲染开/关颜色）
    private int hideBtnX, hideBtnY, hideBtnW;
    private String hideBtnLabel = "";
    // ---- 手动重排 ----
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
    private Set<Item> currentPinned;
    // ---- 置顶的精确物品（按精确ID区分） ----
    private Set<String> currentExactPinned;

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
     * 全局解析设置（如「是否计算容器数据」）切换后重新加载并刷新界面。
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
        // 重新绑定 currentIgnored/currentPinned，确保它们引用 ignoredItems/pinnedItems 中的实际集合
        // （因为 ensurePersistenceLoaded 可能在 loadMaterials 中首次调用，需要刷新引用）
        this.currentIgnored = ignoredItems.computeIfAbsent(filePath, k -> new HashSet<>());
        this.currentPinned = pinnedItems.computeIfAbsent(filePath, k -> new HashSet<>());
        this.currentExactIgnored = exactIgnoredItems.computeIfAbsent(filePath, k -> new HashSet<>());
        this.currentExactPinned = exactPinnedItems.computeIfAbsent(filePath, k -> new HashSet<>());
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
                // 精确物品行不参与忽略
                if (!row.exact && currentIgnored.contains(row.item)) {
                    ignoredTotals.put(row.item, row.totalCount);
                }
            }
            Minecraft.getInstance().setScreenAndShow(
                    new IgnoredItemsScreen(this, schematicName, filePath, filePath.toString(), currentIgnored, ignoredTotals));
        });

        createRowButtons();
    }

    private void calculateColumnPositions() {
        int maxTotalWidth = this.font.width(I18n.tr("litematlist.header.total"));
        int maxHaveWidth = this.font.width(I18n.tr("litematlist.header.have"));
        int maxMissWidth = this.font.width(I18n.tr("litematlist.header.missing"));
        int maxNameWidth = this.font.width(I18n.tr("litematlist.header.item"));

        for (MaterialRow row : rows) {
            // 钳制名称列宽：精确物品ID可能非常长，避免撑爆列布局（有样式显示文本时按显示文本计宽）
            int nameWidth = row.displayText != null
                    ? this.font.width(row.displayText)
                    : this.font.width(row.name);
            maxNameWidth = Math.max(maxNameWidth, Math.min(nameWidth, MAX_NAME_DISPLAY_WIDTH));
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
                if (entry.isExact()) {
                    // 精确物品（带非默认组件的特殊物品）：name 保存精确物品ID（导出/悬停用，也是忽略/置顶/替换的稳定键），
                    // 显示文本用 getHoverName()（与游戏内物品名一致：自定义名自动斜体、稀有度颜色）
                    ItemStack exactStack = entry.toItemStack();
                    String exactId = entry.blockName();
                    // 应用已保存的精确物品替换（展示替换后的物品，但保留精确键 row.name 与 exact 标记）
                    Map<String, String> exactFileRepl = exactReplacements.get(filePath.toString());
                    Item displayItem = item;
                    ItemStack displayStack = exactStack;
                    Component displayText = exactStack.getHoverName();
                    if (exactFileRepl != null) {
                        String replId = exactFileRepl.get(exactId);
                        if (replId != null) {
                            Item replaced = BuiltInRegistries.ITEM.get(Identifier.parse(replId)).map(h -> h.value()).orElse(null);
                            if (replaced != null && replaced != Items.AIR) {
                                displayItem = replaced;
                                displayStack = new ItemStack(replaced, entry.totalCount());
                                displayText = replaced.getName(new ItemStack(replaced)).copy();
                            }
                        }
                    }
                    rows.add(new MaterialRow(
                            displayStack,
                            exactId,
                            entry.totalCount(),
                            displayItem,
                            item, // originalItem 始终是原理图原始基础物品
                            entry.missingCount(),
                            true,
                            displayText
                    ));
                    continue;
                }
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

            // 取消替换按钮（物品已被替换时显示，可恢复为原理图原始物品/精确物品原状）
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

            // 忽略按钮（置顶材料不显示忽略按钮，精确物品按精确ID判断）
            if (!isRowPinned(row)) {
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
            if (row.exact) {
                // 精确物品：按精确ID加入忽略集合
                if (isRowPinned(row)) return;
                currentExactIgnored.add(row.name);
            } else {
                if (isRowPinned(row)) return;
                currentIgnored.add(row.item);
            }
            // 清除缓存的原材料分析数据，下一次打开原材料列表时重新分析（排除已忽略物品）
            String pathKey = filePath.toString();
            rawMaterials.remove(pathKey);
            rawMaterialRedundancy.remove(pathKey);
            savePersistence(); // 立即持久化忽略
            reinjectIfUploaded(); // 上传条目需立即重新注入
            MaterialListHudRenderer.refreshHud(); // 更新HUD
            LitematListMod.LOGGER.info("忽略物品: {} ({})", row.name, row.item);
        }
    }

    /** 该行是否已置顶（普通物品按 Item，精确物品按精确ID） */
    private boolean isRowPinned(MaterialRow row) {
        if (!allowPin) return false;
        return row.exact ? currentExactPinned.contains(row.name) : currentPinned.contains(row.originalItem);
    }

    /** 该行是否已忽略（普通物品按 Item，精确物品按精确ID） */
    private boolean isRowIgnored(MaterialRow row) {
        return row.exact ? currentExactIgnored.contains(row.name) : currentIgnored.contains(row.item);
    }

    /** 精确物品是否已被替换（替换映射按精确ID记录）；被替换后悬停不再显示原精确ID，取消替换后恢复。 */
    private boolean isExactReplaced(String exactId) {
        Map<String, String> exactFileRepl = exactReplacements.get(filePath.toString());
        return exactFileRepl != null && exactFileRepl.containsKey(exactId);
    }

    /**
     * 绘制悬停提示：内容过长触及屏幕边界时自动换行，避免超出屏幕。
     * 按鼠标位置取较宽一侧的可用宽度，逐字符折行后以多行提示绘制。
     */
    private void drawWrappedTooltip(GuiGraphicsExtractor gfx, String text, int mouseX, int mouseY) {
        if (text == null || text.isEmpty()) {
            return;
        }
        int available = (mouseX > this.width / 2) ? mouseX - 12 : this.width - mouseX - 12;
        available = Math.max(80, Math.min(available, this.width - 24));
        List<Component> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            line.append(c);
            if (this.font.width(line.toString()) > available && line.length() > 1) {
                line.setLength(line.length() - 1);
                lines.add(Component.literal(line.toString()));
                line.setLength(0);
                line.append(c);
            }
        }
        if (line.length() > 0) {
            lines.add(Component.literal(line.toString()));
        }
        gfx.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
    }

    private List<MaterialRow> getVisibleRowsOnly() {
        List<MaterialRow> visible = new ArrayList<>();
        for (MaterialRow row : rows) {
            if (isRowIgnored(row)) continue;
            if (hideNoMissing) {
                int haveCount = getRowHaveCount(row);
                if (row.totalCount - haveCount <= 0) continue;
            }
            // 搜索过滤（中文名/拼音全拼/拼音首字母/英文ID；精确物品同时支持按完整精确ID搜索）
            if (searchActive && !searchText.isEmpty()) {
                String itemId = BuiltInRegistries.ITEM.getKey(row.item).toString();
                String displayName = row.displayText != null ? row.displayText.getString() : row.name;
                if (!PinyinSearch.matches(searchText, displayName, itemId) && !row.name.contains(searchText)) continue;
            }
            visible.add(row);
        }
        // 应用手动重排顺序
        List<Item> reorder = reorderedItems.get(filePath);
        if (reorder != null && !reorder.isEmpty()) {
            Map<Item, MaterialRow> rowMap = new HashMap<>();
            List<MaterialRow> exactRows = new ArrayList<>();
            for (MaterialRow row : visible) {
                if (row.exact) {
                    // 精确物品行不参与手动重排，保持在列表末尾
                    exactRows.add(row);
                    continue;
                }
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
            ordered.addAll(exactRows);
            visible = ordered;
        }
        // 标记材料模式：将已标记的材料排到最前面（精确物品按精确ID一并参与）
        if (allowPin && (!currentPinned.isEmpty() || !currentExactPinned.isEmpty())) {
            List<MaterialRow> pinned = new ArrayList<>();
            List<MaterialRow> unpinned = new ArrayList<>();
            for (MaterialRow row : visible) {
                if (isRowPinned(row)) {
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

    /** 计算行的已有数量：精确物品按「物品+组件」精确匹配，普通物品按物品类型匹配 */
    private int getRowHaveCount(MaterialRow row) {
        if (row.exact && row.itemStack != null && !row.itemStack.isEmpty()) {
            return getExactPlayerCount(row.itemStack);
        }
        return getPlayerItemCount(row.item);
    }

    /** 玩家背包中与目标堆叠「物品+组件」完全一致的数量（含潜影盒内部） */
    private int getExactPlayerCount(ItemStack target) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return 0;
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (ItemStack.isSameItemSameComponents(stack, target)) {
                count += stack.getCount();
            } else if (InventoryUtils.shulkerBoxHasItems(stack)) {
                for (ItemStack boxStack : InventoryUtils.getStoredItems(stack)) {
                    if (ItemStack.isSameItemSameComponents(boxStack, target)) {
                        count += boxStack.getCount();
                    }
                }
            }
        }
        return count;
    }

    /** 按实际像素宽度截断显示名（超出时以…结尾）；未超出返回原文 */
    private String truncateNameByWidth(String name, int maxWidth) {
        if (this.font.width(name) <= maxWidth) return name;
        String ellipsis = "…";
        int limit = maxWidth - this.font.width(ellipsis);
        int w = 0;
        for (int i = 0; i < name.length(); i++) {
            int cw = this.font.width(String.valueOf(name.charAt(i)));
            if (w + cw > limit) return name.substring(0, i) + ellipsis;
            w += cw;
        }
        return name;
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

    /** 绘制简易悬停提示框（26.x 无原生 tooltip API，统一手绘黑框+白字） */
    private void drawTooltipBox(GuiContext ctx, String tip, int mouseX, int mouseY) {
        int tipW = this.font.width(tip) + 8;
        int tipH = 16;
        int tipX = mouseX + 12;
        int tipY = mouseY - 20;
        if (tipX + tipW > this.width) tipX = this.width - tipW - 4;
        if (tipY < 4) tipY = mouseY + 16;
        ctx.fill(tipX, tipY, tipX + tipW, tipY + tipH, 0xCC000000);
        ctx.fill(tipX + 1, tipY + 1, tipX + tipW - 1, tipY + tipH - 1, 0xCC333333);
        ctx.drawString(this.font, tip, tipX + 4, tipY + 4, 0xFFFFFFFF);
    }

    private List<MaterialRow> getSortedRows() {
        List<MaterialRow> visible = getVisibleRowsOnly();

        // 分离置顶和非置顶（精确物品按精确ID一并参与）
        if (allowPin && (!currentPinned.isEmpty() || !currentExactPinned.isEmpty())) {
            List<MaterialRow> pinned = new ArrayList<>();
            List<MaterialRow> unpinned = new ArrayList<>();
            for (MaterialRow row : visible) {
                if (isRowPinned(row)) {
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
            int haveCount = getRowHaveCount(row);
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
        String pathKey = filePath.toString();
        // 精确物品：按精确ID记录替换映射，随后重建行（保留精确键 row.name 与 exact 标记）
        if (target.exact) {
            Identifier newId = BuiltInRegistries.ITEM.getKey(newItem);
            if (newId != null) {
                Map<String, String> exactFileRepl = exactReplacements.computeIfAbsent(pathKey, k -> new HashMap<>());
                exactFileRepl.put(target.name, newId.toString());
            }
            savePersistence();
            rawMaterials.remove(pathKey);
            rawMaterialRedundancy.remove(pathKey);
            loadMaterials(); // 依据映射重建全部行，精确行展示替换后物品
            reinjectIfUploaded();
            MaterialListHudRenderer.refreshHud(); // 更新HUD
            initGui();
            return;
        }
        for (int i = 0; i < rows.size(); i++) {
            if (!rows.get(i).exact && rows.get(i).item == target.item) {
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
        reinjectIfUploaded();
        MaterialListHudRenderer.refreshHud(); // 更新HUD
        initGui();
    }

    private void restoreOriginalItem(MaterialRow target) {
        String pathKey = filePath.toString();
        // 精确物品：从精确替换映射移除，随后重建行恢复原始精确堆叠（含组件）
        if (target.exact) {
            Map<String, String> exactFileRepl = exactReplacements.get(pathKey);
            if (exactFileRepl != null) {
                exactFileRepl.remove(target.name);
                if (exactFileRepl.isEmpty()) {
                    exactReplacements.remove(pathKey);
                }
            }
            savePersistence();
            rawMaterials.remove(pathKey);
            rawMaterialRedundancy.remove(pathKey);
            loadMaterials(); // 依据映射重建全部行，精确行恢复原状
            reinjectIfUploaded();
            MaterialListHudRenderer.refreshHud(); // 更新HUD
            replacingRow = -1;
            return;
        }
        for (int i = 0; i < rows.size(); i++) {
            // 通过 originalItem 匹配：替换后 target.item 是旧物品，rows[i].item 是新物品，不能直接 == 比较
            if (!rows.get(i).exact && rows.get(i).originalItem == target.originalItem) {
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
        reinjectIfUploaded();
        MaterialListHudRenderer.refreshHud(); // 更新HUD
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

            List<MaterialListExporter.Row> exportRows = new ArrayList<>();
            for (MaterialRow row : rows) {
                if (isRowIgnored(row)) continue;
                Identifier id = BuiltInRegistries.ITEM.getKey(row.item);
                String itemId = row.exact ? row.name : (id != null ? id.toString() : "unknown");
                int have = getRowHaveCount(row);
                exportRows.add(new MaterialListExporter.Row(
                        itemId,
                        row.name != null ? row.name : "",
                        row.totalCount,
                        Math.max(0, row.totalCount - have),
                        Math.min(have, row.totalCount)));
            }
            String title = "原理图'" + schematicName + "'材料列表";
            if ("txt".equals(format)) {
                MaterialListExporter.writeTxt(filePath, title, exportRows);
            } else if ("csv".equals(format)) {
                MaterialListExporter.writeCsv(filePath, exportRows);
            } else {
                MaterialListExporter.writeJson(filePath, schematicName, title, exportRows);
            }
            LitematListMod.LOGGER.info("已导出材料列表: {}", filePath);

            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                String fmt = format.toUpperCase();
                Component link = Component.literal(fileName)
                        .withStyle(style -> style
                                .withClickEvent(new net.minecraft.network.chat.ClickEvent.OpenFile(filePath.toString()))
                                .withUnderlined(true)
                                .withColor(0x55FFFF));
                Component msg = Component.literal("§a材料列表已导出为" + fmt + ": ").append(link);
                client.player.sendSystemMessage(msg);
            }
        } catch (Exception e) {
            LitematListMod.LOGGER.error("导出失败", e);
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
                int haveW = this.font.width(String.valueOf(getRowHaveCount(row)));
                int missW = this.font.width(String.valueOf(Math.max(0, row.totalCount - getRowHaveCount(row))));

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
        // 悬停精确物品时待绘制的预览（在文字层之后绘制，保证在最上层）
        ItemStack hoveredExactStack = null;
        String hoveredFullName = null;
        String hoveredItemId = null; // 悬停普通物品图标时显示的物品ID
        for (int i = scrollOffset; i < maxIdx; i++) {
            MaterialRow row = displayRows.get(i);
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;

            int iconX = 15;
            int nameX = 35;

            // 标记材料模式：先画标记图标，再偏移物品图标和文字（精确物品按精确ID一并绘制）
            if (allowPin) {
                boolean isPinned = isRowPinned(row);
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
            // 精确物品使用带样式文本渲染（自定义名自动斜体、稀有度颜色，与游戏内物品名一致）；过长时退化为截断纯文本
            Component nameText = row.displayText != null ? row.displayText : Component.literal(row.name);
            if (this.font.width(nameText) > MAX_NAME_DISPLAY_WIDTH) {
                String truncated = truncateNameByWidth(nameText.getString(), MAX_NAME_DISPLAY_WIDTH);
                ctx.drawString(this.font, truncated, nameX, rowY + 5, 0xFFFFFFFF);
            } else {
                ctx.drawString(this.font, nameText, nameX, rowY + 5, 0xFFFFFFFF);
            }
            ctx.drawString(this.font, String.valueOf(row.totalCount), colTotalX, rowY + 5, 0xFFFFFFFF);

            int haveCount = getRowHaveCount(row);
            ctx.drawString(this.font, String.valueOf(haveCount), colHaveX, rowY + 5, 0xFFFFFFFF);

            int missCount = Math.max(0, row.totalCount - haveCount);
            int missColor = missCount == 0 ? 0xFF55FF55
                          : (haveCount >= missCount ? 0xFFFFAA00 : 0xFFFF5555);
            ctx.drawString(this.font, String.valueOf(missCount), colMissX, rowY + 5, missColor);

            // 精确物品：悬停图标时预览物品（含组件悬浮提示）
            if (row.exact && mouseX >= iconX && mouseX < iconX + 16 && mouseY >= rowY + 2 && mouseY < rowY + 18) {
                hoveredExactStack = row.itemStack;
            }
            // 普通物品：悬停图标显示对应物品ID（精确物品保留原有物品预览提示）
            if (!row.exact && mouseX >= iconX && mouseX < iconX + 16 && mouseY >= rowY + 2 && mouseY < rowY + 18) {
                hoveredItemId = BuiltInRegistries.ITEM.getKey(row.item).toString();
            }
            // 精确物品悬停名称显示完整精确物品ID（已被替换的精确物品不再显示原ID）；普通物品仅名称被截断时显示完整名称
            // 名称悬停宽度取实际显示宽度（截断时为 MAX_NAME_DISPLAY_WIDTH），
            // 避免悬停热点延伸到数量列而与数量提示叠加
            int shownNameWidth = Math.min(this.font.width(nameText), MAX_NAME_DISPLAY_WIDTH);
            boolean nameHovered = mouseX >= nameX && mouseX < nameX + shownNameWidth
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            boolean exactIdSuppressed = row.exact && isExactReplaced(row.name);
            if (nameHovered && ((row.exact && !exactIdSuppressed)
                    || this.font.width(nameText) > MAX_NAME_DISPLAY_WIDTH)) {
                hoveredFullName = row.name;
            }
        }

        // 表头文字
        int headerNameX = allowPin ? 53 : 35;
        // 搜索图标
        int searchIconX = allowPin ? 33 : 15;
        drawTextureIcon(ctx, SEARCH_ICON, searchIconX, HEADER_Y + 1, 14, 14);
        // 搜索框打开时不绘制"物品"表头文字（搜索框覆盖该区域，避免与输入文字重叠冲突）
        if (!searchActive) {
            ctx.drawString(this.font, I18n.tr("litematlist.header.item"), headerNameX, HEADER_Y + 3, 0xFFFFFFFF);
        }
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
            int haveCount = getRowHaveCount(hovered);
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

        // 精确物品图标悬停预览（物品+组件悬浮提示）
        if (hoveredExactStack != null) {
            gfx.setTooltipForNextFrame(this.font, hoveredExactStack, mouseX, mouseY);
        }
        // 截断名称悬停显示完整精确物品ID（过长时按屏幕边界自动换行）
        if (hoveredFullName != null) {
            drawWrappedTooltip(gfx, hoveredFullName, mouseX, mouseY);
        }
        // 悬停普通物品图标显示对应物品ID
        if (hoveredItemId != null) {
            drawTooltipBox(ctx, hoveredItemId, mouseX, mouseY);
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
                    // 精确物品按精确ID切换置顶
                    if (row.exact) {
                        if (currentExactPinned.contains(row.name)) {
                            currentExactPinned.remove(row.name);
                        } else {
                            currentExactPinned.add(row.name);
                        }
                    } else if (currentPinned.contains(row.originalItem)) {
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
                    boolean draggedPinned = isRowPinned(dragged);
                    boolean targetPinned = isRowPinned(target);
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
            // 精确物品行不参与手动重排持久化
            if (!row.exact && !currentIgnored.contains(row.item)) {
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
                initGui();
                return true;
            }
        }
        return super.charTyped(event);
    }

    private record MaterialRow(ItemStack itemStack, String name, int totalCount, Item item, Item originalItem,
                               int missingCount, boolean exact, Component displayText) {
        /** 普通行便捷构造（exact=false，显示名直接用 name 纯文本） */
        MaterialRow(ItemStack itemStack, String name, int totalCount, Item item, Item originalItem, int missingCount) {
            this(itemStack, name, totalCount, item, originalItem, missingCount, false, null);
        }
    }

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

