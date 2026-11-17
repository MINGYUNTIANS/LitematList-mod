package com.litematlist.gui;
//#if MC == 11701
//$$import net.minecraft.text.TranslatableText;
//#endif
//#if MC == 11701
//$$import net.minecraft.text.LiteralText;
//#endif

import com.litematlist.I18n;
import com.litematlist.LitematicReader;
import com.litematlist.LitematListMod;
//#if MC == 12110 || MC == 12111
import com.litematlist.MaterialListExporter;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
import com.litematlist.MaterialListImporter;
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$import com.litematlist.MaterialListExporter;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
import com.litematlist.TextWidthUtils;
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$import com.litematlist.MaterialListImporter;
//#endif
//#if MC == 11701 || MC == 11904
//$$import com.litematlist.MaterialListExporter;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$import com.litematlist.PinyinSearch;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$import com.litematlist.TextWidthUtils;
//#endif
import com.litematlist.config.Configs;
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$import com.litematlist.PinyinSearch;
//#endif
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.util.InventoryUtils;
import net.minecraft.client.MinecraftClient;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.gui.DrawContext;
//#endif
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.gui.DrawableHelper;
//#endif
//#if MC == 11701 || MC == 11904
//$$import com.mojang.blaze3d.systems.RenderSystem;
//#endif
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.util.math.MatrixStack;
//#endif
//#if MC == 12110 || MC == 12111
import net.minecraft.client.gui.Click;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$
//#endif
import net.minecraft.client.gui.widget.TextFieldWidget;
//#if MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.gl.RenderPipelines;
//#elseif MC == 12104
//$$import net.minecraft.client.render.RenderLayer;
//#endif
//#if MC == 12110 || MC == 12111
import net.minecraft.client.input.KeyInput;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$
//#endif
//#if MC == 12110 || MC == 12111
import net.minecraft.client.input.CharInput;
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$
//#endif
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
//#if MC == 12110 || MC == 12111
import com.litematlist.PinyinSearch;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.registry.Registries;
//#elseif MC == 11701
//$$import net.minecraft.util.registry.Registry;
//#endif
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent;
import net.minecraft.util.Identifier;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
/**
 * 项目总材料列表 —— 合并项目内所有材料列表，支持共享忽略/置顶状态和来源明细展开。
 */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$/**
//$$ * 椤圭洰鎬绘潗鏂欏垪琛?鈥斺€?鍚堝苟椤圭洰鍐呮墍鏈夋潗鏂欏垪琛紝鏀寔鍏变韩蹇界暐/缃《鐘舵€佸拰鏉ユ簮鏄庣粏灞曞紑銆? */
//#endif
public class ProjectSummaryScreen extends GuiBase {

    private final GuiBase parent;
    private final String projectName;
    private final List<MaterialListScreen.LoadedEntry> projectEntries;
    private final List<MergedRow> rows = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int ROW_HEIGHT = 22;
    private static final int HEADER_Y = 42;
    private static final int HEADER_HEIGHT = 16;
    private static final int LIST_TOP = HEADER_Y + HEADER_HEIGHT + 2;
    private int visibleRows;
    private boolean scrollbarDragging = false;
    private boolean loaded = false;
    private int hoveredRow = -1;
    private int hoveredColumn = -1;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 拖拽重排 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 鎷栨嫿閲嶆帓 ----
//#endif
    private int draggedRow = -1;
    private int dragStartY = 0;
    private boolean isDragging = false;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private int draggedSubIndex = -1;    // 子项拖拽：当前拖拽的可见子项索引
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private int draggedSubParentIdx = -1; // 子项拖拽：父 MergedRow 的显示索引
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 忽略/展开时序保护 ----
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    private int draggedSubIndex = -1;    // 瀛愰」鎷栨嫿锛氬綋鍓嶆嫋鎷界殑鍙瀛愰」绱㈠紩
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    private int draggedSubParentIdx = -1; // 瀛愰」鎷栨嫿锛氱埗 MergedRow 鐨勬樉绀虹储寮?
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 蹇界暐/灞曞紑鏃跺簭淇濇姢 ----
//#endif
    private boolean ignoreJustClicked = false;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 已忽略 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 宸插拷鐣?----
//#endif
    private final Set<String> currentIgnored = new HashSet<>();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private final Set<String> expandedItems = new HashSet<>(); // 记录展开的物品（稳定键）
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 隐藏不缺材料 ----
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    private final Set<String> expandedItems = new HashSet<>(); // 璁板綍灞曞紑鐨勭墿鍝?
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 闅愯棌涓嶇己鏉愭枡 ----
//#endif
    private boolean hideNoMissing = MaterialDataStore.persistedHideNoMissing;
    private int hideBtnX, hideBtnY, hideBtnW;
    private String hideBtnLabel = "";

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 标记材料 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 鏍囪鏉愭枡 ----
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final Identifier PIN_EMPTY = Identifier.of("litematlist", "textures/gui/pin/empty.png");
//#elseif MC == 11701
//$$    private static final Identifier PIN_EMPTY = new Identifier("litematlist", "textures/gui/pin/empty.png");
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final Identifier PIN_FAVORITE = Identifier.of("litematlist", "textures/gui/pin/favorite.png");
//#elseif MC == 11701
//$$    private static final Identifier PIN_FAVORITE = new Identifier("litematlist", "textures/gui/pin/favorite.png");
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final Identifier PIN_HOVERED_EMPTY = Identifier.of("litematlist", "textures/gui/pin/hovered_empty.png");
//#elseif MC == 11701
//$$    private static final Identifier PIN_HOVERED_EMPTY = new Identifier("litematlist", "textures/gui/pin/hovered_empty.png");
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final Identifier PIN_HOVERED_FAVORITE = Identifier.of("litematlist", "textures/gui/pin/hovered_favorite.png");
//#elseif MC == 11701
//$$    private static final Identifier PIN_HOVERED_FAVORITE = new Identifier("litematlist", "textures/gui/pin/hovered_favorite.png");
//#endif
    private boolean allowPin = MaterialDataStore.persistedAllowPin;
    private int pinBtnX, pinBtnY, pinBtnW;
    private String pinBtnLabel = "";
    private final Set<String> currentPinned = new HashSet<>();

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 展开/折叠图标 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 灞曞紑/鎶樺彔鍥炬爣 ----
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final Identifier EXPAND_NORMAL = Identifier.of("litematlist", "textures/gui/expand/expand_normal.png");
//#elseif MC == 11701
//$$    private static final Identifier EXPAND_NORMAL = new Identifier("litematlist", "textures/gui/expand/expand_normal.png");
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final Identifier EXPAND_HOVERED = Identifier.of("litematlist", "textures/gui/expand/expand_hovered.png");
//#elseif MC == 11701
//$$    private static final Identifier EXPAND_HOVERED = new Identifier("litematlist", "textures/gui/expand/expand_hovered.png");
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final Identifier COLLAPSE_NORMAL = Identifier.of("litematlist", "textures/gui/expand/collapse_normal.png");
//#elseif MC == 11701
//$$    private static final Identifier COLLAPSE_NORMAL = new Identifier("litematlist", "textures/gui/expand/collapse_normal.png");
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final Identifier COLLAPSE_HOVERED = Identifier.of("litematlist", "textures/gui/expand/collapse_hovered.png");
//#elseif MC == 11701
//$$    private static final Identifier COLLAPSE_HOVERED = new Identifier("litematlist", "textures/gui/expand/collapse_hovered.png");
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final Identifier SEARCH_ICON = Identifier.of("litematlist", "textures/gui/search.png");
//#elseif MC == 11701
//$$    private static final Identifier SEARCH_ICON = new Identifier("litematlist", "textures/gui/search.png");
//#endif

    private int colTotalX, colHaveX, colMissX;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private int ignoredBtnX, ignoredBtnY, ignoredBtnW, ignoredBtnH; // 【已忽略】按钮位置
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 搜索 ----
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    private int ignoredBtnX, ignoredBtnY, ignoredBtnW, ignoredBtnH; // 銆愬凡蹇界暐銆戞寜閽綅缃?
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 鎼滅储 ----
//#endif
    private boolean searchActive = false;
    private String searchText = "";
    private TextFieldWidget searchField;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // 程序性 setText 同步期间忽略变更回调（1.21.10 的 setText 回调旧值，会与 searchText 振荡造成无限递归）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // 程序性 setText 同步期间忽略变更回调（setText 回调旧值，会与 searchText 振荡造成无限递归）
//#endif
    private boolean updatingSearchField = false;

    private static final int BTN_IGNORE_W = 52;
    private static final int BTN_REPLACE_W = 52;
    private static final int BTN_CANCEL_W = 52;
    private static final int BTN_RIGHT_MARGIN = 12;
    private static final int BTN_GAP = 4;
    private static final int EXPAND_ICON_SIZE = 16;
    /** 精确物品名称/ID 的显示宽度上限（超出则截断，悬停显示完整值） */
    private static final int MAX_PROJECT_NAME_WIDTH = 200;

    private enum SortMode {
        NONE(""), TOTAL_DESC("▼"), TOTAL_ASC("▲"),
        HAVE_DESC("▼"), HAVE_ASC("▲"), MISS_DESC("▼"), MISS_ASC("▲");
        final String arrow;
        SortMode(String arrow) { this.arrow = arrow; }
    }
    private SortMode sortMode = SortMode.NONE;

    private void initSortMode() {
        try { sortMode = SortMode.valueOf(MaterialDataStore.persistedProjectSortMode); } catch (Exception e) { sortMode = SortMode.NONE; }
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 内部数据类 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 鍐呴儴鏁版嵁绫?----
//#endif
    private static class SourceEntry {
        final String listName;
        final int count;
        final Item item;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        final Item originalItem; // 替换前的原始物品，用于精确匹配忽略集
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        final Item originalItem; // 鏇挎崲鍓嶇殑鍘熷鐗╁搧锛岀敤浜庣簿纭尮閰嶅拷鐣ラ泦
//#endif
        final Path filePath;
        final String key;        // 稳定键：普通=替换后物品ID；精确=精确ID
        final boolean exact;     // 是否为精确物品来源
        final ItemStack exactStack; // 精确物品堆叠（普通为 null）
        SourceEntry(String listName, int count, Item item, Item originalItem, Path filePath,
                    String key, boolean exact, ItemStack exactStack) {
            this.listName = listName; this.count = count; this.item = item;
            this.originalItem = originalItem; this.filePath = filePath;
            this.key = key; this.exact = exact; this.exactStack = exactStack;
        }
    }

    private static class MergedRow {
        final ItemStack itemStack;
        final String name;       // 稳定显示名：普通=物品名；精确=精确ID
        final String key;        // 稳定键：普通=物品ID；精确=精确ID
        final Text displayText;  // 精确物品的带样式显示文本（普通为 null）
        int totalCount;
        final Item item;
        final boolean exact;     // 是否为精确物品行
        final ItemStack exactStack; // 精确物品堆叠（普通为 null）
        final List<SourceEntry> sources;
        boolean expanded;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        int category;    // 材料分类（建筑材料/容器数据/实体数据/实体容器数据），取各来源最早分类
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        int category;    // 材料分类，取各来源最早分类
//#endif

        MergedRow(ItemStack itemStack, String name, String key, Text displayText, int totalCount,
                  Item item, boolean exact, ItemStack exactStack, List<SourceEntry> sources, int category) {
            this.itemStack = itemStack; this.name = name; this.key = key; this.displayText = displayText;
            this.totalCount = totalCount; this.item = item; this.exact = exact; this.exactStack = exactStack;
            this.sources = sources;
            this.category = category;
            this.expanded = false;
        }
        int getRowHeight() { return ROW_HEIGHT + (expanded ? (int) sources.stream().filter(s -> !isSourceIgnored(s)).count() * ROW_HEIGHT : 0); }

        boolean isSourceIgnored(SourceEntry src) {
            if (src.exact) {
                // 精确物品：忽略状态按精确ID存放在子列表的精确忽略集合中
                return MaterialDataStore.getExactIgnoredForPath(src.filePath).contains(src.key);
            }
            Set<Item> subIgnored = MaterialDataStore.getIgnoredForPath(src.filePath);
            if (subIgnored == null) return false;
            // 检查替换后的物品是否在忽略集中（通过 originalItem 精确匹配）
            Item checkItem = MaterialDataStore.getReplacement(src.filePath, src.originalItem);
            return subIgnored.contains(checkItem);
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        /** 计算非忽略来源的总数 */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        /** 璁＄畻闈炲拷鐣ユ潵婧愮殑鎬绘暟 */
//#endif
        int getActiveTotal() {
            return sources.stream().filter(s -> !isSourceIgnored(s)).mapToInt(s -> s.count).sum();
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        /** 至少有一个来源未被忽略 */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        /** 鑷冲皯鏈変竴涓潵婧愭湭琚拷鐣?*/
//#endif
        boolean hasActiveSources() {
            return sources.stream().anyMatch(s -> !isSourceIgnored(s));
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        /** 所有来源都被忽略 */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        /** 鎵€鏈夋潵婧愰兘琚拷鐣?*/
//#endif
        boolean allSourcesIgnored() {
            return sources.stream().allMatch(this::isSourceIgnored);
        }
    }

    public ProjectSummaryScreen(GuiBase parent, String projectName, List<MaterialListScreen.LoadedEntry> projectEntries) {
        super();
        this.parent = parent;
        this.projectName = projectName;
        this.projectEntries = projectEntries;
        this.title = I18n.tr("litematlist.title.project_summary", projectName);
        initSortMode();
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 计算考虑展开行的位置 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 璁＄畻鑰冭檻灞曞紑琛岀殑浣嶇疆 ----
//#endif
    private int getRowY(int displayIndex) {
        List<MergedRow> display = getSortedRows();
        int y = LIST_TOP;
        for (int i = 0; i < displayIndex && i < display.size(); i++) {
            y += display.get(i).getRowHeight();
        }
        return y;
    }

    /**
     * 全局解析设置（如「是否计算容器数据」）切换后重新合并并刷新总材料列表。
     */
    public void refreshAfterGlobalSettingChange() {
        this.loaded = false;
        initGui();
    }

    @Override
    public void initGui() {
        super.initGui();
        this.visibleRows = (this.height - 100) / ROW_HEIGHT;

        if (!loaded) { loadMergedMaterials(); loaded = true; }
        calculateColumnPositions();

        int buttonY = this.height - 38;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 【刷新列表】按钮
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 銆愬埛鏂板垪琛ㄣ€戞寜閽?
//#endif
        ButtonGeneric refreshBtn = new ButtonGeneric(20, buttonY, 80, 20, I18n.tr("litematlist.button.refresh_list"));
        refreshBtn.setRenderDefaultBackground(true);
        this.addButton(refreshBtn, (IButtonActionListener) (btn, mb) -> { loaded = false; initGui(); });

        ButtonGeneric backButton = new ButtonGeneric(this.width / 2 - 60, buttonY, 120, 20, I18n.tr("litematlist.button.back"));
        backButton.setRenderDefaultBackground(true);
        this.addButton(backButton, (IButtonActionListener) (btn, mb) -> MinecraftClient.getInstance().setScreen(parent));

        boolean isEmpty = projectEntries.isEmpty();

        int ignoredCount = 0;
        for (MergedRow row : rows) {
            if (row.sources.stream().anyMatch(row::isSourceIgnored)) ignoredCount++;
        }
        String ignoredLabel = ignoredCount > 0 ? I18n.tr("litematlist.button.ignored_count", ignoredCount) : I18n.tr("litematlist.button.ignored");
        int ignoredBtnW = Math.max(60, this.textRenderer.getWidth(ignoredLabel) + 16);

        String hideLabel = hideNoMissing ? I18n.tr("litematlist.button.hide_no_missing_on") : I18n.tr("litematlist.button.hide_no_missing_off");
        int hideBtnW = Math.max(60, this.textRenderer.getWidth(hideLabel) + 16);
        this.hideBtnX = 20; this.hideBtnY = 20; this.hideBtnW = hideBtnW; this.hideBtnLabel = hideLabel;
        ButtonGeneric hideBtn = new ButtonGeneric(hideBtnX, hideBtnY, hideBtnW, 20, "");
        hideBtn.setRenderDefaultBackground(true);
        if (isEmpty) hideBtn.setEnabled(false);
        this.addButton(hideBtn, (IButtonActionListener) (b, mb) -> { hideNoMissing = !hideNoMissing; MaterialDataStore.persistedHideNoMissing = hideNoMissing; MaterialDataStore.savePersistence(); initGui(); });

        String pinLabel = allowPin ? I18n.tr("litematlist.button.allow_pin_on") : I18n.tr("litematlist.button.allow_pin_off");
        int pinBtnW = Math.max(60, this.textRenderer.getWidth(pinLabel) + 16);
        this.pinBtnX = hideBtnX + hideBtnW + 4; this.pinBtnY = 20; this.pinBtnW = pinBtnW; this.pinBtnLabel = pinLabel;
        ButtonGeneric pinBtn = new ButtonGeneric(pinBtnX, pinBtnY, pinBtnW, 20, "");
        pinBtn.setRenderDefaultBackground(true);
        if (isEmpty) pinBtn.setEnabled(false);
        this.addButton(pinBtn, (IButtonActionListener) (b, mb) -> { allowPin = !allowPin; MaterialDataStore.persistedAllowPin = allowPin; MaterialDataStore.savePersistence(); initGui(); });

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 导出按钮
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 瀵煎嚭鎸夐挳
//#endif
        int exportBtnX = pinBtnX + pinBtnW + 4;
        ButtonGeneric exportTxtBtn = new ButtonGeneric(exportBtnX, 20, 70, 20, I18n.tr("litematlist.button.export_txt"));
        exportTxtBtn.setRenderDefaultBackground(true);
        if (isEmpty) exportTxtBtn.setEnabled(false);
        this.addButton(exportTxtBtn, (IButtonActionListener) (b, mb) -> exportMaterials("txt"));

        ButtonGeneric exportJsonBtn = new ButtonGeneric(exportBtnX + 74, 20, 70, 20, I18n.tr("litematlist.button.export_json"));
        exportJsonBtn.setRenderDefaultBackground(true);
        if (isEmpty) exportJsonBtn.setEnabled(false);
        this.addButton(exportJsonBtn, (IButtonActionListener) (b, mb) -> exportMaterials("json"));

        ButtonGeneric exportCsvBtn = new ButtonGeneric(exportBtnX + 148, 20, 70, 20, I18n.tr("litematlist.button.export_csv"));
        exportCsvBtn.setRenderDefaultBackground(true);
        if (isEmpty) exportCsvBtn.setEnabled(false);
        this.addButton(exportCsvBtn, (IButtonActionListener) (b, mb) -> exportMaterials("csv"));

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 搜索框（与 render 中绘制的搜索框位置一致：图标右侧 18px）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鎼滅储妗?
//#endif
        int searchIconX = (allowPin ? 53 : 35) - 20;
        if (searchField == null) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            this.searchField = new TextFieldWidget(this.textRenderer, searchIconX + 18, HEADER_Y, 80, 14, Text.empty());
//#elseif MC == 11701
//$$            this.searchField = new TextFieldWidget(this.textRenderer, searchIconX + 18, HEADER_Y, 80, 14, LiteralText.EMPTY);
//#endif
            this.searchField.setMaxLength(50);
            this.searchField.setDrawsBackground(false);
            this.searchField.setText(searchText);
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 文本变化统一回调：覆盖输入/粘贴/IME 提交等所有路径
//#endif
            this.searchField.setChangedListener(this::onSearchChanged);
            this.addSelectableChild(this.searchField);
        } else {
            this.searchField.setX(searchIconX + 18);
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            this.searchField.setY(HEADER_Y);
//#elseif MC == 11701
//$$            this.searchField.y = HEADER_Y;
//#endif
            if (!this.searchField.getText().equals(searchText)) {
                // 同步期间忽略 setText 触发的变更回调，防止回调旧值与 searchText 振荡造成无限递归
                this.updatingSearchField = true;
                try {
                    this.searchField.setText(searchText);
                } finally {
                    this.updatingSearchField = false;
                }
            }
            this.addSelectableChild(this.searchField);
        }

        ButtonGeneric ignoredBtn = new ButtonGeneric(this.width - ignoredBtnW - 12, 10, ignoredBtnW, 20, ignoredLabel);
        ignoredBtn.setRenderDefaultBackground(true);
        if (isEmpty) ignoredBtn.setEnabled(false);
        this.ignoredBtnX = this.width - ignoredBtnW - 12;
        this.ignoredBtnY = 10;
        this.ignoredBtnW = ignoredBtnW;
        this.ignoredBtnH = 20;
        this.addButton(ignoredBtn, (IButtonActionListener) (b, mb) -> openIgnoredScreen());

        createRowButtons();
    }

    private void openIgnoredScreen() {
        Map<String, Integer> ignoredTotals = new HashMap<>();
        Set<String> allIgnoredKeys = new HashSet<>();
        for (MergedRow row : rows) {
            boolean anyIgnored = row.sources.stream().anyMatch(row::isSourceIgnored);
            if (anyIgnored) {
                allIgnoredKeys.add(row.key);
                ignoredTotals.put(row.key, row.totalCount);
            }
        }
        loaded = false; // 返回时重新加载
        MinecraftClient.getInstance().setScreen(
                new ProjectIgnoredItemsScreen(this, projectName, allIgnoredKeys, ignoredTotals, projectEntries));
    }

    private void exportMaterials(String format) {
        try {
            Path dir = MinecraftClient.getInstance().runDirectory.toPath().resolve("litematlist").resolve("project");
            Files.createDirectories(dir);
            String dateStr = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date());
            String fileName = "material_list_" + dateStr + "." + format;
            Path filePath = dir.resolve(fileName);

            List<MaterialListExporter.Row> exportRows = new ArrayList<>();
            for (MergedRow row : rows) {
                if (currentIgnored.contains(row.key)) continue;
                int have = getPlayerItemCount(row);
                exportRows.add(new MaterialListExporter.Row(
                        row.key,
                        row.name != null ? row.name : "",
                        row.totalCount,
                        Math.max(0, row.totalCount - have),
                        Math.min(have, row.totalCount)));
            }
            String title = "项目'" + projectName + "'材料列表";
            if ("txt".equals(format)) {
                MaterialListExporter.writeTxt(filePath, title, exportRows);
            } else if ("json".equals(format)) {
                MaterialListExporter.writeJson(filePath, projectName, title, exportRows);
            } else {
                MaterialListExporter.writeCsv(filePath, exportRows);
            }
            LitematListMod.LOGGER.info("已导出材料列表: {}", filePath);

            // 聊天栏提示
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                String fmt = format.toUpperCase();
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                Text link = Text.literal(fileName)
//#elseif MC == 11701
//$$                Text link = new LiteralText(fileName)
//#endif
                        .styled(style -> style
//#if MC == 12108 || MC == 12110 || MC == 12111
                                .withClickEvent(new ClickEvent.OpenFile(filePath.toString()))
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, filePath.toString()))
//#endif
                                .withUnderline(true)
                                .withColor(0x55FFFF));
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                Text msg = Text.literal("§a材料列表已导出为" + fmt + ": ").append(link);
//#elseif MC == 11701
//$$                Text msg = new TranslatableText("litematlist.message.exported", fmt).append(link);
//#elseif MC == 11904 || MC == 12001
//$$                Text msg = Text.translatable("litematlist.message.exported", fmt).append(link);
//#endif
                client.player.sendMessage(msg, false);
            }
        } catch (Exception e) {
            LitematListMod.LOGGER.error("导出失败", e);
        }
    }

    /**
     * 绘制悬停提示：内容过长触及屏幕边界时自动换行，避免超出屏幕。
     * 按鼠标位置取较宽一侧的可用宽度，逐字符折行后以多行提示绘制。
     */
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void drawWrappedTooltip(DrawContext drawContext, String text, int mouseX, int mouseY) {
//#elseif MC == 11701 || MC == 11904
//$$    private void drawWrappedTooltip(MatrixStack matrices, String text, int mouseX, int mouseY) {
//#endif
        if (text == null || text.isEmpty()) {
            return;
        }
        int available = (mouseX > this.width / 2) ? mouseX - 12 : this.width - mouseX - 12;
        available = Math.max(80, Math.min(available, this.width - 24));
        List<Text> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            line.append(c);
            if (this.textRenderer.getWidth(line.toString()) > available && line.length() > 1) {
                line.setLength(line.length() - 1);
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                lines.add(Text.literal(line.toString()));
//#elseif MC == 11701
//$$                lines.add(new LiteralText(line.toString()));
//#endif
                line.setLength(0);
                line.append(c);
            }
        }
        if (line.length() > 0) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            lines.add(Text.literal(line.toString()));
//#elseif MC == 11701
//$$            lines.add(new LiteralText(line.toString()));
//#endif
        }
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$        this.renderTooltip(matrices, lines, mouseX, mouseY);
//#endif
    }

    private void loadMergedMaterials() {
        rows.clear();
        Map<String, List<SourceEntry>> sourceMap = new LinkedHashMap<>();
        Map<String, String> itemNames = new HashMap<>();      // 普通键 → 物品显示名
        Map<String, Item> rowItems = new HashMap<>();         // 稳定键 → 基础物品
        Map<String, ItemStack> exactStacks = new HashMap<>(); // 精确键 → 精确堆叠
        Map<String, Text> exactTexts = new HashMap<>();       // 精确键 → 带样式显示文本
        Set<String> exactKeys = new HashSet<>();              // 精确键集合
        Map<String, Integer> keyCategory = new HashMap<>();   // 稳定键 → 材料分类（取最早分类）

        for (MaterialListScreen.LoadedEntry entry : projectEntries) {
            Path filePath = entry.filePath();
            if (filePath == null) continue;
            String listName = entry.name();
            if (MaterialListScreen.isTxtEntry(entry)) listName += "（txt）";
            else if (MaterialListScreen.isJsonEntry(entry)) listName += "（JSON）";

            try {
                List<LitematicReader.MaterialEntry> entries;
                if (MaterialDataStore.getImportedMaterials().containsKey(filePath)) {
                    entries = MaterialDataStore.getImportedMaterials().get(filePath);
                } else if (filePath.toString().toLowerCase().endsWith(".txt") || filePath.toString().toLowerCase().endsWith(".json") || filePath.toString().toLowerCase().endsWith(".csv")) {
                    MaterialListImporter.ImportResult result = MaterialListImporter.importFromFile(filePath);
                    if (result != null) { entries = result.materials(); MaterialDataStore.cacheMaterialList(filePath, entries); }
                    else continue;
                } else {
                    entries = LitematicReader.loadMaterialList(filePath);
                }
                for (LitematicReader.MaterialEntry me : entries) {
                    if (me.isExact()) {
                        // 精确物品（带非默认组件的特殊物品）：按精确ID独立成行，保留精确信息不塌缩进基础物品
                        String key = me.blockName();
                        ItemStack exactStack = me.toItemStack();
                        sourceMap.computeIfAbsent(key, k -> new ArrayList<>())
                                .add(new SourceEntry(listName, me.totalCount(), me.item(), me.item(), filePath, key, true, exactStack));
                        exactKeys.add(key);
                        keyCategory.merge(key, me.category(), Math::min);
                        rowItems.putIfAbsent(key, me.item());
                        exactStacks.putIfAbsent(key, exactStack);
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        exactTexts.putIfAbsent(key, exactStack.getFormattedName());
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$                        exactTexts.putIfAbsent(key, exactStack.getName());
//#endif
                        continue;
                    }
                    Item originalItem = me.item();
                    Item item = MaterialDataStore.getReplacement(filePath, originalItem);
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    String key = Registries.ITEM.getId(item).toString();
//#elseif MC == 11701
//$$                    String key = Registry.ITEM.getId(item).toString();
//#endif
                    keyCategory.merge(key, me.category(), Math::min);
                    sourceMap.computeIfAbsent(key, k -> new ArrayList<>())
                            .add(new SourceEntry(listName, me.totalCount(), item, originalItem, filePath, key, false, null));
                    itemNames.putIfAbsent(key, item.getName().getString());
                    rowItems.putIfAbsent(key, item);
                }
            } catch (Exception e) {
                LitematListMod.LOGGER.error("合并材料列表失败: {}", filePath, e);
            }
        }

        currentIgnored.clear();
        currentPinned.clear();
        for (MaterialListScreen.LoadedEntry entry : projectEntries) {
            Path fp = entry.filePath();
            if (fp == null) continue;
            for (Item it : MaterialDataStore.getPinnedForPath(fp)) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                Identifier id = Registries.ITEM.getId(it);
//#elseif MC == 11701
//$$                Identifier id = Registry.ITEM.getId(it);
//#endif
                if (id != null) currentPinned.add(id.toString());
            }
            currentPinned.addAll(MaterialDataStore.getExactPinnedForPath(fp));
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 保存展开状态
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 淇濆瓨灞曞紑鐘舵€?
//#endif
        Set<String> savedExpanded = new HashSet<>(expandedItems);
        expandedItems.clear();

        for (var entry : sourceMap.entrySet()) {
            String key = entry.getKey();
            List<SourceEntry> sources = entry.getValue();
            int total = sources.stream().mapToInt(s -> s.count).sum();
            MergedRow mr;
            if (exactKeys.contains(key)) {
                ItemStack exactStack = exactStacks.get(key);
                Item baseItem = rowItems.get(key);
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                ItemStack icon = exactStack != null ? exactStack.copyWithCount(total) : new ItemStack(baseItem, total);
//#endif
//#if MC == 11701
//$$                ItemStack icon;
//#endif
//#if MC == 11701
//$$                if (exactStack != null) { icon = exactStack.copy(); icon.setCount(total); }
//#endif
//#if MC == 11701
//$$                else { icon = new ItemStack(baseItem, total); }
//#endif
                mr = new MergedRow(icon, key, key, exactTexts.get(key), total, baseItem, true, exactStack, sources,
                        keyCategory.getOrDefault(key, LitematicReader.MaterialEntry.CATEGORY_BUILDING));
            } else {
                Item item = rowItems.get(key);
                mr = new MergedRow(new ItemStack(item, total), itemNames.get(key), key, null, total, item, false, null, sources,
                        keyCategory.getOrDefault(key, LitematicReader.MaterialEntry.CATEGORY_BUILDING));
            }
            if (savedExpanded.contains(key) && sources.size() > 1) { mr.expanded = true; expandedItems.add(key); }
            rows.add(mr);
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 只有所有来源都被忽略的才加入 currentIgnored
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鍙湁鎵€鏈夋潵婧愰兘琚拷鐣ョ殑鎵嶅姞鍏?currentIgnored
//#endif
        for (MergedRow row : rows) {
            if (row.allSourcesIgnored()) currentIgnored.add(row.key);
        }
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鍒濆鎸夋€昏鏁伴噺浠庨珮鍒颁綆鎺掑垪
//#endif
        // 初始顺序：分类优先（建筑材料→容器数据→实体数据→实体容器数据），组内按总计数量从高到低
        rows.sort(Comparator.comparingInt((MergedRow r) -> r.category)
                .thenComparing(Comparator.comparingInt((MergedRow r) -> r.totalCount).reversed()));
        LitematListMod.LOGGER.info("合并项目材料: {} 种物品, 忽略: {}", rows.size(), currentIgnored.size());
    }

    private void syncIgnoreToSubLists(String key, boolean ignored) {
        for (MergedRow row : rows) {
            if (row.key.equals(key)) {
                for (SourceEntry src : row.sources) {
                    if (src.exact) {
                        // 精确物品：同步到子列表的精确忽略集合
                        Set<String> subExact = MaterialDataStore.getExactIgnoredForPath(src.filePath);
                        if (ignored) subExact.add(src.key); else subExact.remove(src.key);
                        continue;
                    }
                    Set<Item> subIgnored = MaterialDataStore.getIgnoredForPath(src.filePath);
                    if (subIgnored != null) {
                        if (ignored) subIgnored.add(src.item); else subIgnored.remove(src.item);
                    }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    // 同时同步到原材料忽略列表
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    // 鍚屾椂鍚屾鍒板師鏉愭枡蹇界暐鍒楄〃
//#endif
                    String pathKey = src.filePath.toString();
                    Set<String> rawIgnored = MaterialDataStore.rawMaterialIgnored.computeIfAbsent(pathKey, k -> new HashSet<>());
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    String itemId = Registries.ITEM.getId(src.item).toString();
//#elseif MC == 11701
//$$                    String itemId = Registry.ITEM.getId(src.item).toString();
//#endif
                    if (ignored) rawIgnored.add(itemId); else rawIgnored.remove(itemId);
                }
                break;
            }
        }
    }

    private void syncPinToSubLists(String key, boolean pinned) {
        for (MergedRow row : rows) {
            if (row.key.equals(key)) {
                for (SourceEntry src : row.sources) {
                    if (src.exact) {
                        // 精确物品：同步到子列表的精确置顶集合
                        Set<String> subExact = MaterialDataStore.getExactPinnedForPath(src.filePath);
                        if (pinned) subExact.add(src.key); else subExact.remove(src.key);
                        continue;
                    }
                    Set<Item> subPinned = MaterialDataStore.getPinnedForPath(src.filePath);
                    if (subPinned != null) {
                        if (pinned) subPinned.add(src.item); else subPinned.remove(src.item);
                    }
                }
                break;
            }
        }
    }

    private void createRowButtons() {
        List<MergedRow> displayRows = getSortedRows();
        // 替换/忽略后可见行数可能减少，夹取 scrollOffset 防止按钮全部消失
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, displayRows.size() - visibleRows)));
        int maxRenderY = this.height - 45;
        int cumulativeY = LIST_TOP;
        int count = 0;

        for (int i = scrollOffset; i < displayRows.size(); i++) {
            MergedRow row = displayRows.get(i);
            int rowY = cumulativeY;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (rowY + row.getRowHeight() > maxRenderY) break; // 底部按钮区域，不创建按钮（与 render 一致）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            if (rowY + row.getRowHeight() > maxRenderY) break; // 搴曢儴鎸夐挳鍖哄煙锛屼笉鍒涘缓鎸夐挳锛堜笌 render 涓€鑷达級
//#endif

            if (row.sources.size() > 1) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 多来源：展开时显示替换/取消替换和忽略按钮
//#endif
                if (row.expanded) {
                    int subIdx = 0;
                    for (int j = 0; j < row.sources.size(); j++) {
                        SourceEntry src = row.sources.get(j);
                        boolean isIgnored = row.isSourceIgnored(src);
                        if (isIgnored) continue; // 已忽略的不显示按钮
                        int subY = rowY + ROW_HEIGHT + subIdx * ROW_HEIGHT;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        if (subY >= maxRenderY) break; // 底部按钮区域
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                        if (subY >= maxRenderY) break; // 搴曢儴鎸夐挳鍖哄煙
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        int ignoreX = this.width - BTN_RIGHT_MARGIN - BTN_IGNORE_W - 30; // 加宽
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                        int ignoreX = this.width - BTN_RIGHT_MARGIN - BTN_IGNORE_W - 30; // 鍔犲
//#endif
                        int replaceX = ignoreX - BTN_REPLACE_W - BTN_GAP;
                        final MergedRow r = row;
                        final SourceEntry s = src;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        // 替换/取消替换按钮（精确物品不支持按物品替换，隐藏）
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        if (!src.exact) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            if (src.item != src.originalItem) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                // 已替换：显示取消替换按钮
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                ButtonGeneric cancelBtn = new ButtonGeneric(replaceX, subY + 1, BTN_CANCEL_W, 20, I18n.tr("litematlist.button.cancel_replace"));
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                cancelBtn.setRenderDefaultBackground(true);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                this.addButton(cancelBtn, (IButtonActionListener) (b, mb) -> restoreSourceReplacement(r, s));
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            } else {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                // 未替换：显示替换按钮
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                ButtonGeneric replaceBtn = new ButtonGeneric(replaceX, subY + 1, BTN_REPLACE_W, 20, I18n.tr("litematlist.button.replace"));
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                replaceBtn.setRenderDefaultBackground(true);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                this.addButton(replaceBtn, (IButtonActionListener) (b, mb) -> replaceSource(r, s));
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        // 鏇挎崲/鍙栨秷鏇挎崲鎸夐挳
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        if (!src.exact && src.item != src.originalItem) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                            // 宸叉浛鎹細鏄剧ず鍙栨秷鏇挎崲鎸夐挳
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                            ButtonGeneric cancelBtn = new ButtonGeneric(replaceX, subY + 1, BTN_CANCEL_W, 20, I18n.tr("litematlist.button.cancel_replace"));
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                            cancelBtn.setRenderDefaultBackground(true);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                            this.addButton(cancelBtn, (IButtonActionListener) (b, mb) -> restoreSourceReplacement(r, s));
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        } else if (!src.exact) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                            // 鏈浛鎹細鏄剧ず鏇挎崲鎸夐挳
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                            ButtonGeneric replaceBtn = new ButtonGeneric(replaceX, subY + 1, BTN_REPLACE_W, 20, I18n.tr("litematlist.button.replace"));
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                            replaceBtn.setRenderDefaultBackground(true);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                            this.addButton(replaceBtn, (IButtonActionListener) (b, mb) -> replaceSource(r, s));
//#endif
                        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        // 忽略按钮
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                        // 蹇界暐鎸夐挳
//#endif
                        ButtonGeneric ignoreBtn = new ButtonGeneric(ignoreX, subY + 1, BTN_IGNORE_W + 30, 20, I18n.tr("litematlist.button.ignore"));
                        ignoreBtn.setRenderDefaultBackground(true);
                        this.addButton(ignoreBtn, (IButtonActionListener) (b, mb) -> ignoreSource(r, s));
                        subIdx++;
                    }
                }
            } else {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 单来源：显示替换/取消替换和忽略按钮
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                // 鍗曟潵婧愶細鏄剧ず鏇挎崲/鍙栨秷鏇挎崲鍜屽拷鐣ユ寜閽?
//#endif
                SourceEntry src = row.sources.get(0);
                int ignoreX = this.width - BTN_RIGHT_MARGIN - BTN_IGNORE_W;
                int replaceX = ignoreX - BTN_REPLACE_W - BTN_GAP;
                final MergedRow r = row;
                final SourceEntry s = src;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 替换/取消替换按钮（精确物品不支持按物品替换，隐藏）
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (!src.exact) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    if (src.item != src.originalItem) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        ButtonGeneric cancelBtn = new ButtonGeneric(replaceX, rowY + 1, BTN_CANCEL_W, 20, I18n.tr("litematlist.button.cancel_replace"));
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        cancelBtn.setRenderDefaultBackground(true);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        this.addButton(cancelBtn, (IButtonActionListener) (b, mb) -> restoreSourceReplacement(r, s));
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    } else {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        ButtonGeneric replaceBtn = new ButtonGeneric(replaceX, rowY + 1, BTN_REPLACE_W, 20, I18n.tr("litematlist.button.replace"));
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        replaceBtn.setRenderDefaultBackground(true);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        this.addButton(replaceBtn, (IButtonActionListener) (b, mb) -> replaceSource(r, s));
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                // 鏇挎崲/鍙栨秷鏇挎崲鎸夐挳
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                if (!src.exact && src.item != src.originalItem) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                            ButtonGeneric cancelBtn = new ButtonGeneric(replaceX, rowY + 1, BTN_CANCEL_W, 20, I18n.tr("litematlist.button.cancel_replace"));
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    cancelBtn.setRenderDefaultBackground(true);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    this.addButton(cancelBtn, (IButtonActionListener) (b, mb) -> restoreSourceReplacement(r, s));
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                } else if (!src.exact) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    ButtonGeneric replaceBtn = new ButtonGeneric(replaceX, rowY + 1, BTN_REPLACE_W, 20, I18n.tr("litematlist.button.replace"));
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    replaceBtn.setRenderDefaultBackground(true);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    this.addButton(replaceBtn, (IButtonActionListener) (b, mb) -> replaceSource(r, s));
//#endif
                }

                // 忽略按钮（置顶材料不显示）
                if (!(allowPin && currentPinned.contains(row.key))) {
                    ButtonGeneric ignoreBtn = new ButtonGeneric(ignoreX, rowY + 1, BTN_IGNORE_W, 20, I18n.tr("litematlist.button.ignore"));
                    ignoreBtn.setRenderDefaultBackground(true);
                    final int visibleIdx = i;
                    this.addButton(ignoreBtn, (IButtonActionListener) (b, mb) -> { ignoreItem(visibleIdx); initGui(); });
                }
            }

            cumulativeY += row.getRowHeight();
            count++;
            if (count >= visibleRows) break;
        }
    }

    private void ignoreItem(int visibleIdx) {
        List<MergedRow> displayRows = getSortedRows();
        if (visibleIdx >= 0 && visibleIdx < displayRows.size()) {
            MergedRow row = displayRows.get(visibleIdx);
            if (allowPin && currentPinned.contains(row.key)) return;
            currentIgnored.add(row.key);
            syncIgnoreToSubLists(row.key, true);
            ignoreJustClicked = true;
            MaterialDataStore.savePersistence();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            MaterialListHudRenderer.refreshHud(); // 忽略后更新HUD
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            MaterialListHudRenderer.refreshHud(); // 蹇界暐鍚庢洿鏂癏UD
//#endif
        }
    }

    private void ignoreSource(MergedRow parent, SourceEntry source) {
        if (source.exact) {
            MaterialDataStore.getExactIgnoredForPath(source.filePath).add(source.key);
        } else {
            Set<Item> subIgnored = MaterialDataStore.getIgnoredForPath(source.filePath);
            if (subIgnored != null) subIgnored.add(source.item);
        }
        MaterialDataStore.savePersistence();
        ignoreJustClicked = true;
        loaded = false;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        MaterialListHudRenderer.refreshHud(); // 忽略后更新HUD
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        MaterialListHudRenderer.refreshHud(); // 蹇界暐鍚庢洿鏂癏UD
//#endif
        initGui();
    }

    private void unignoreSource(SourceEntry source) {
        if (source.exact) {
            MaterialDataStore.getExactIgnoredForPath(source.filePath).remove(source.key);
        } else {
            Set<Item> subIgnored = MaterialDataStore.getIgnoredForPath(source.filePath);
            if (subIgnored != null) subIgnored.remove(source.item);
        }
        MaterialDataStore.savePersistence();
        loaded = false;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        MaterialListHudRenderer.refreshHud(); // 取消忽略后更新HUD
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        MaterialListHudRenderer.refreshHud(); // 鍙栨秷蹇界暐鍚庢洿鏂癏UD
//#endif
        initGui();
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 替换来源物品：打开 BlockPickerScreen，选择后将替换写入持久化并重新合并 */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 鏇挎崲鏉ユ簮鐗╁搧锛氭墦寮€ BlockPickerScreen锛岄€夋嫨鍚庡皢鏇挎崲鍐欏叆鎸佷箙鍖栧苟閲嶆柊鍚堝苟 */
//#endif
    private void replaceSource(MergedRow parent, SourceEntry source) {
        MinecraftClient.getInstance().setScreen(
                new BlockPickerScreen(this, newItem -> {
                    MaterialDataStore.setReplacement(source.filePath, source.originalItem, newItem);
                    MaterialDataStore.savePersistence();
                    loaded = false;
                    MaterialListHudRenderer.refreshHud();
                    initGui();
                }));
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 取消来源物品的替换，恢复为原始物品 */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 鍙栨秷鏉ユ簮鐗╁搧鐨勬浛鎹紝鎭㈠涓哄師濮嬬墿鍝?*/
//#endif
    private void restoreSourceReplacement(MergedRow parent, SourceEntry source) {
        MaterialDataStore.removeReplacement(source.filePath, source.originalItem);
        MaterialDataStore.savePersistence();
        loaded = false;
        MaterialListHudRenderer.refreshHud();
        initGui();
    }

    /** 搜索文本变化统一回调：任意输入路径（打字/粘贴/IME 提交）都会刷新过滤结果 */
    private void onSearchChanged(String text) {
        if (updatingSearchField) return; // 程序性 setText 的回调直接忽略
        if (text.equals(this.searchText)) return; // 相同文本不再重建，防止 setText 触发监听器造成无限递归
        this.searchText = text;
        scrollOffset = 0; // 输入字符后立刻回到列表最顶端
        initGui();
    }

    private List<MergedRow> getVisibleRowsOnly() {
        List<MergedRow> visible = new ArrayList<>();
        for (MergedRow row : rows) {
            if (row.hasActiveSources()) {
                if (hideNoMissing) {
                    if (row.totalCount - getPlayerItemCount(row) <= 0) continue;
                }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 搜索过滤（中文名/拼音全拼/拼音首字母/英文ID），直接读输入框实时文本
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                // 搜索过滤（中文名/拼音全拼/拼音首字母/英文ID）
//#endif
                if (searchActive) {
                    String query = searchField != null ? searchField.getText() : searchText;
                    if (!query.isEmpty()) {
                        String itemId = row.key;
                        if (!PinyinSearch.matches(query, row.name, itemId)) continue;
                    }
                }
                row.totalCount = row.getActiveTotal();
                visible.add(row);
            }
        }
        if (allowPin && !currentPinned.isEmpty()) {
            List<MergedRow> pinned = new ArrayList<>();
            List<MergedRow> unpinned = new ArrayList<>();
            for (MergedRow row : visible) {
                if (currentPinned.contains(row.key)) pinned.add(row);
                else unpinned.add(row);
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

    /** 计算行的已有数量：精确物品按「物品+组件」精确匹配，普通物品按物品类型匹配 */
    private int getPlayerItemCount(MergedRow row) {
        if (row.exact && row.exactStack != null && !row.exactStack.isEmpty()) {
            return getExactPlayerCount(row.exactStack);
        }
        return getPlayerItemCount(row.item);
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 玩家背包中与目标堆叠「物品+组件」完全一致的数量（含潜影盒内部） */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 玩家背包中与目标堆叠「物品+NBT 数据」完全一致（忽略数量）的数量（含潜影盒内部） */
//#endif
    private int getExactPlayerCount(ItemStack target) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return 0;
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (ItemStack.areItemsAndComponentsEqual(stack, target)) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            if (ItemStack.canCombine(stack, target)) {
//#endif
                count += stack.getCount();
            } else if (InventoryUtils.shulkerBoxHasItems(stack)) {
                for (ItemStack boxStack : InventoryUtils.getStoredItems(stack)) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    if (ItemStack.areItemsAndComponentsEqual(boxStack, target)) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    if (ItemStack.canCombine(boxStack, target)) {
//#endif
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
                count, shulker, stacks, items, shulker * 27 + stacks, items, shulkerFloat);
    }

    private void calculateColumnPositions() {
        int maxTotalWidth = this.textRenderer.getWidth(I18n.tr("litematlist.header.total"));
        int maxHaveWidth = this.textRenderer.getWidth(I18n.tr("litematlist.header.have"));
        int maxMissWidth = this.textRenderer.getWidth(I18n.tr("litematlist.header.missing"));
        int maxNameWidth = this.textRenderer.getWidth(I18n.tr("litematlist.header.item"));

        for (MergedRow row : rows) {
            int nameW = this.textRenderer.getWidth(row.name);
            if (row.exact) nameW = Math.min(nameW, MAX_PROJECT_NAME_WIDTH);
            maxNameWidth = Math.max(maxNameWidth, nameW);
            maxTotalWidth = Math.max(maxTotalWidth, this.textRenderer.getWidth(String.valueOf(row.totalCount)));
        }
        int nameEndX = 35 + maxNameWidth + 20;
        colTotalX = nameEndX + 24;
        colHaveX = colTotalX + maxTotalWidth + 24;
        colMissX = colHaveX + maxHaveWidth + 24;
    }

    private List<MergedRow> getSortedRows() {
        List<MergedRow> visible = getVisibleRowsOnly();
        if (allowPin && !currentPinned.isEmpty()) {
            List<MergedRow> pinned = new ArrayList<>();
            List<MergedRow> unpinned = new ArrayList<>();
            for (MergedRow row : visible) {
                if (currentPinned.contains(row.key)) pinned.add(row);
                else unpinned.add(row);
            }
            pinned.addAll(unpinned);
            return pinned;
        }
        return visible;
    }

    private List<MergedRow> sortRowList(List<MergedRow> list) {
        Comparator<MergedRow> cmp = switch (sortMode) {
            case TOTAL_DESC -> Comparator.comparingInt((MergedRow r) -> r.totalCount).reversed();
            case TOTAL_ASC -> Comparator.comparingInt(r -> r.totalCount);
            case HAVE_DESC -> Comparator.comparingInt((MergedRow r) -> getPlayerItemCount(r)).reversed();
            case HAVE_ASC -> Comparator.comparingInt(r -> getPlayerItemCount(r));
            case MISS_DESC -> Comparator.comparingInt((MergedRow r) -> Math.max(0, r.totalCount - getPlayerItemCount(r))).reversed();
            case MISS_ASC -> Comparator.comparingInt(r -> Math.max(0, r.totalCount - getPlayerItemCount(r)));
            default -> null;
        };
        if (cmp == null) return list;
        list.sort(cmp);
        return list;
    }

    private void handleHeaderClick(double mouseX, double mouseY) {
        if (mouseY < HEADER_Y || mouseY > HEADER_Y + HEADER_HEIGHT) return;
        SortMode newMode;
        if (mouseX >= colTotalX - 20 && mouseX <= colTotalX + 30)
            newMode = (sortMode == SortMode.TOTAL_DESC) ? SortMode.TOTAL_ASC : (sortMode == SortMode.TOTAL_ASC) ? SortMode.NONE : SortMode.TOTAL_DESC;
        else if (mouseX >= colHaveX - 20 && mouseX <= colHaveX + 30)
            newMode = (sortMode == SortMode.HAVE_DESC) ? SortMode.HAVE_ASC : (sortMode == SortMode.HAVE_ASC) ? SortMode.NONE : SortMode.HAVE_DESC;
        else if (mouseX >= colMissX - 20 && mouseX <= colMissX + 30)
            newMode = (sortMode == SortMode.MISS_DESC) ? SortMode.MISS_ASC : (sortMode == SortMode.MISS_ASC) ? SortMode.NONE : SortMode.MISS_DESC;
        else return;
        sortMode = newMode;
        MaterialDataStore.persistedProjectSortMode = sortMode.name();
        MaterialDataStore.savePersistence();
        if (newMode != SortMode.NONE) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 一次性排序：将 rows 按当前模式排序
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                // 一次性排序：将 rows 按当前模式排序
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            Comparator<MergedRow> cmp = sortComparator(newMode);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                Comparator<MergedRow> cmp = sortComparator(newMode);
//#endif
            if (cmp != null) rows.sort(cmp);
        }
        initGui();
    }

    private Comparator<MergedRow> sortComparator(SortMode mode) {
        return switch (mode) {
            case TOTAL_DESC -> Comparator.comparingInt((MergedRow r) -> r.totalCount).reversed();
            case TOTAL_ASC -> Comparator.comparingInt(r -> r.totalCount);
            case HAVE_DESC -> Comparator.comparingInt((MergedRow r) -> getPlayerItemCount(r)).reversed();
            case HAVE_ASC -> Comparator.comparingInt(r -> getPlayerItemCount(r));
            case MISS_DESC -> Comparator.comparingInt((MergedRow r) -> Math.max(0, r.totalCount - getPlayerItemCount(r))).reversed();
            case MISS_ASC -> Comparator.comparingInt(r -> Math.max(0, r.totalCount - getPlayerItemCount(r)));
            default -> null;
        };
    }

    @Override
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public void render(DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
//#elseif MC == 11701 || MC == 11904
//$$    public void render(MatrixStack matrices, int mouseX, int mouseY, float partialTicks) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        this.renderBackground(drawContext, mouseX, mouseY, partialTicks);
//#elseif MC == 11701 || MC == 11904
//$$        this.renderBackground(matrices);
//#elseif MC == 12001
//$$        this.renderBackground(drawContext);
//#endif

        if (rows.isEmpty()) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawCenteredTextWithShadow(this.textRenderer, I18n.tr("litematlist.error.no_data"),
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    this.width / 2, this.height / 2 - 10, 0xFFFFFFFF);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            super.render(drawContext, mouseX, mouseY, partialTicks);
//#endif
//#if MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, I18n.tr("litematlist.error.no_data"), this.width / 2 - this.textRenderer.getWidth(I18n.tr("litematlist.error.no_data")) / 2, this.height / 2 - 10, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$            super.render(matrices, mouseX, mouseY, partialTicks);
//#endif
            return;
        }

        List<MergedRow> displayRows = getSortedRows();
        int listRight = this.width - 8;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        int maxRenderY = this.height - 45; // 底部按钮区域上方，不渲染到此行以下
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        int maxRenderY = this.height - 45; // 搴曢儴鎸夐挳鍖哄煙涓婃柟锛屼笉娓叉煋鍒版琛屼互涓?
//#endif
        int listBottom = LIST_TOP;
        int cumulativeY = LIST_TOP;
        int count = 0;
        for (int i = scrollOffset; i < displayRows.size() && count < visibleRows; i++) {
            int rowH = displayRows.get(i).getRowHeight();
            if (cumulativeY + rowH > maxRenderY) break;
            cumulativeY += rowH;
            count++;
        }
        listBottom = cumulativeY;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(8, LIST_TOP - 2, listRight, listBottom + 2, 0x40000000);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, 8, LIST_TOP - 2, listRight, listBottom + 2, 0x40000000);
//#endif

        hoveredRow = -1;
        hoveredColumn = -1;
        cumulativeY = LIST_TOP;
        count = 0;

        for (int i = scrollOffset; i < displayRows.size() && count < visibleRows; i++) {
            MergedRow row = displayRows.get(i);
            int rowY = cumulativeY;
            int rowH = row.getRowHeight();
            if (rowY + rowH > maxRenderY) break;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 主行高亮（仅覆盖第一行，不含子项）
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (mouseX >= 10 && mouseX <= listRight && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (mouseX >= 10 && mouseX <= listRight && mouseY >= rowY && mouseY < rowY + rowH) {
//#endif
                hoveredRow = i;

                int totalW = this.textRenderer.getWidth(String.valueOf(row.totalCount));
                int haveW = this.textRenderer.getWidth(String.valueOf(getPlayerItemCount(row)));
                int missW = this.textRenderer.getWidth(String.valueOf(Math.max(0, row.totalCount - getPlayerItemCount(row))));
                if (mouseX >= colTotalX - 10 && mouseX <= colTotalX + totalW + 10) hoveredColumn = 0;
                else if (mouseX >= colHaveX - 10 && mouseX <= colHaveX + haveW + 10) hoveredColumn = 1;
                else if (mouseX >= colMissX - 10 && mouseX <= colMissX + missW + 10) hoveredColumn = 2;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            } else if ((i - scrollOffset) % 2 == 0) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if ((i - scrollOffset) % 2 == 0) {
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.fill(10, rowY, listRight, rowY + ROW_HEIGHT, 0x10FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$                DrawableHelper.fill(matrices, 10, rowY, listRight, rowY + ROW_HEIGHT, 0x10FFFFFF);
//#endif
            }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 子项独立高亮检测（不在这里绘制，在 super.render 之后绘制以覆盖按钮区域）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 瀛愰」鐙珛楂樹寒妫€娴嬶紙涓嶅湪杩欓噷缁樺埗锛屽湪 super.render 涔嬪悗缁樺埗浠ヨ鐩栨寜閽尯鍩燂級
//#endif
            if (row.expanded) {
                int subIdx = 0;
                for (int j = 0; j < row.sources.size(); j++) {
                    if (row.isSourceIgnored(row.sources.get(j))) continue;
                    int subY = rowY + ROW_HEIGHT + subIdx * ROW_HEIGHT;
                    boolean subHovered = mouseX >= 10 && mouseX <= listRight && mouseY >= subY && mouseY < subY + ROW_HEIGHT;
                    if (subHovered) {
                        hoveredRow = i;
                    }
                    subIdx++;
                }
            }

            cumulativeY += rowH;
            count++;
        }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(8, HEADER_Y, listRight, HEADER_Y + HEADER_HEIGHT, 0x60000000);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, 8, HEADER_Y, listRight, HEADER_Y + HEADER_HEIGHT, 0x60000000);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        super.render(drawContext, mouseX, mouseY, partialTicks);
//#elseif MC == 11701 || MC == 11904
//$$        super.render(matrices, mouseX, mouseY, partialTicks);
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 隐藏/标记按钮文字
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 闅愯棌/鏍囪鎸夐挳鏂囧瓧
//#endif
        if (!hideBtnLabel.isEmpty()) {
            int colonIdx = hideBtnLabel.indexOf("：");
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (colonIdx < 0) colonIdx = hideBtnLabel.indexOf(":");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (colonIdx < 0) colonIdx = hideBtnLabel.indexOf("：");
//$$            if (colonIdx < 0) colonIdx = hideBtnLabel.indexOf("：");
//#endif
            if (colonIdx > 0) {
                String prefix = hideBtnLabel.substring(0, colonIdx + 1);
                String status = hideBtnLabel.substring(colonIdx + 1);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.drawTextWithShadow(this.textRenderer, prefix, hideBtnX + 4, hideBtnY + 5, 0xFFFFFFFF);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.drawTextWithShadow(this.textRenderer, status, hideBtnX + 4 + this.textRenderer.getWidth(prefix), hideBtnY + 5,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        hideNoMissing ? 0xFF55FF55 : 0xFFFF5555);
//#endif
//#if MC == 11701 || MC == 11904
//$$                this.textRenderer.drawWithShadow(matrices, prefix, hideBtnX + 4, hideBtnY + 5, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$                this.textRenderer.drawWithShadow(matrices, status, hideBtnX + 4 + this.textRenderer.getWidth(prefix), hideBtnY + 5, hideNoMissing ? 0xFF55FF55 : 0xFFFF5555);
//#endif
            }
        }
        if (!pinBtnLabel.isEmpty()) {
            int colonIdx = pinBtnLabel.indexOf("：");
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (colonIdx < 0) colonIdx = pinBtnLabel.indexOf(":");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (colonIdx < 0) colonIdx = pinBtnLabel.indexOf("：");
//$$            if (colonIdx < 0) colonIdx = pinBtnLabel.indexOf("：");
//#endif
            if (colonIdx > 0) {
                String prefix = pinBtnLabel.substring(0, colonIdx + 1);
                String status = pinBtnLabel.substring(colonIdx + 1);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.drawTextWithShadow(this.textRenderer, prefix, pinBtnX + 4, pinBtnY + 5, 0xFFFFFFFF);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.drawTextWithShadow(this.textRenderer, status, pinBtnX + 4 + this.textRenderer.getWidth(prefix), pinBtnY + 5,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        allowPin ? 0xFF55FF55 : 0xFFFF5555);
//#endif
//#if MC == 11701 || MC == 11904
//$$                this.textRenderer.drawWithShadow(matrices, prefix, pinBtnX + 4, pinBtnY + 5, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$                this.textRenderer.drawWithShadow(matrices, status, pinBtnX + 4 + this.textRenderer.getWidth(prefix), pinBtnY + 5, allowPin ? 0xFF55FF55 : 0xFFFF5555);
//#endif
            }
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 在按钮上方绘制悬停高亮（覆盖整行包括按钮区域）
//#endif
        if (hoveredRow >= 0 && hoveredRow < displayRows.size()) {
            MergedRow hovered = displayRows.get(hoveredRow);
            int hoverY = LIST_TOP;
            for (int i = scrollOffset; i < hoveredRow && i < displayRows.size(); i++) {
                hoverY += displayRows.get(i).getRowHeight();
            }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            int hoverH = ROW_HEIGHT; // 主行高
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 如果鼠标悬停在子项上，计算子项位置
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (hovered.expanded) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                int cumulativeY2 = LIST_TOP;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                for (int i = scrollOffset; i < hoveredRow && i < displayRows.size(); i++) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    cumulativeY2 += displayRows.get(i).getRowHeight();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                int mainRowY = cumulativeY2;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                int subIdx = 0;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                for (int j = 0; j < hovered.sources.size(); j++) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    if (hovered.isSourceIgnored(hovered.sources.get(j))) continue;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    int subY = mainRowY + ROW_HEIGHT + subIdx * ROW_HEIGHT;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    if (mouseY >= subY && mouseY < subY + ROW_HEIGHT) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        hoverY = subY;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        hoverH = ROW_HEIGHT;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        break;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    subIdx++;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            int hoverH = ROW_HEIGHT;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            int cumulativeY2 = LIST_TOP;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            for (int i = scrollOffset; i < hoveredRow && i < displayRows.size(); i++) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                cumulativeY2 += displayRows.get(i).getRowHeight();
//#endif
            }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 使用 this.width 覆盖整个屏幕宽度，确保按钮区域也被高亮
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            int mainRowY = cumulativeY2;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            int subIdx = 0;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            for (int j = 0; j < hovered.sources.size(); j++) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                if (hovered.isSourceIgnored(hovered.sources.get(j))) continue;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                int subY = mainRowY + ROW_HEIGHT + subIdx * ROW_HEIGHT;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                if (mouseY >= subY && mouseY < subY + ROW_HEIGHT) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    hoverY = subY;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    hoverH = ROW_HEIGHT;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    break;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                subIdx++;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            }
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(10, hoverY, this.width - 8, hoverY + hoverH, 0x40FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, 10, hoverY, this.width - 8, hoverY + hoverH, 0x40FFFFFF);
//#endif
        }

        // 渲染每一行
        cumulativeY = LIST_TOP;
        count = 0;
        int maxIdx = Math.min(displayRows.size(), scrollOffset + visibleRows);
        String hoveredFullName = null;
        ItemStack hoveredExactStack = null;
        String hoveredItemId = null; // 悬停普通物品图标时显示的物品ID

        for (int i = scrollOffset; i < maxIdx; i++) {
            MergedRow row = displayRows.get(i);
            int rowY = cumulativeY;
            int rowH = row.getRowHeight();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (rowY + rowH > maxRenderY) break; // 不渲染到底部按钮区域
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            if (rowY + rowH > maxRenderY) break; // 涓嶆覆鏌撳埌搴曢儴鎸夐挳鍖哄煙
//#endif
            int iconX = 15, nameX = 35;

            if (allowPin) {
                boolean isPinned = currentPinned.contains(row.key);
                boolean pinHovered = mouseX >= 15 && mouseX <= 31 && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
                Identifier pinTexture = isPinned ? (pinHovered ? PIN_HOVERED_FAVORITE : PIN_FAVORITE)
                        : (pinHovered ? PIN_HOVERED_EMPTY : PIN_EMPTY);
//#if MC == 12108 || MC == 12110 || MC == 12111
                drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, pinTexture, 15, rowY + 2, 0.0f, 0.0f, 16, 16, 16, 16);
//#elseif MC == 11701 || MC == 11904
//$$                RenderSystem.setShaderTexture(0, pinTexture); DrawableHelper.drawTexture(matrices, 15, rowY + 2, 0.0f, 0.0f, 16, 16, 16, 16);
//#elseif MC == 12001
//$$                drawContext.drawTexture(pinTexture, 15, rowY + 2, 0.0f, 0.0f, 16, 16, 16, 16);
//#elseif MC == 12101
//$$                drawContext.drawTexture( pinTexture, 15, rowY + 2, 0.0f, 0.0f, 16, 16, 16, 16);
//#elseif MC == 12104
//$$                drawContext.drawTexture(RenderLayer::getGuiTextured, pinTexture, 15, rowY + 2, 0.0f, 0.0f, 16, 16, 16, 16);
//#endif
                iconX = 33; nameX = 53;
            }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawItem(row.itemStack, iconX, rowY + 2);
//#elseif MC == 11701
//$$            MinecraftClient.getInstance().getItemRenderer().renderInGui(row.itemStack, iconX, rowY + 2);
//#elseif MC == 11904
//$$            MinecraftClient.getInstance().getItemRenderer().renderInGui(matrices, row.itemStack, iconX, rowY + 2);
//#endif
            // 精确物品使用带样式文本渲染（自定义名自动斜体、稀有度颜色）；过长时截断，悬停显示完整精确ID
            String nameStr = row.displayText != null ? row.displayText.getString() : row.name;
            boolean nameTruncated = this.textRenderer.getWidth(nameStr) > MAX_PROJECT_NAME_WIDTH;
            if (nameTruncated) nameStr = TextWidthUtils.truncateByWidth(nameStr, MAX_PROJECT_NAME_WIDTH);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTextWithShadow(this.textRenderer, nameStr, nameX, rowY + 5, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, nameStr, nameX, rowY + 5, 0xFFFFFFFF);
//#endif
            if ((row.exact || nameTruncated)
                    && mouseX >= nameX && mouseX < nameX + MAX_PROJECT_NAME_WIDTH
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                hoveredFullName = row.name;
            }
            // 精确物品图标悬停：显示物品悬浮提示（如带纹饰陶罐的名称+四行瓦片信息）
            if (row.exact && mouseX >= iconX && mouseX < iconX + 16
                    && mouseY >= rowY + 2 && mouseY < rowY + 18) {
                hoveredExactStack = row.itemStack;
            }
            // 普通物品图标悬停：显示对应物品ID（精确物品保留原有物品预览提示）
            if (!row.exact && mouseX >= iconX && mouseX < iconX + 16
                    && mouseY >= rowY + 2 && mouseY < rowY + 18) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                hoveredItemId = Registries.ITEM.getId(row.item).toString();
//#elseif MC == 11701
//$$                hoveredItemId = Registry.ITEM.getId(row.item).toString();
//#endif
            }
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTextWithShadow(this.textRenderer, String.valueOf(row.totalCount), colTotalX, rowY + 5, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, String.valueOf(row.totalCount), colTotalX, rowY + 5, 0xFFFFFFFF);
//#endif
            int haveCount = getPlayerItemCount(row);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTextWithShadow(this.textRenderer, String.valueOf(haveCount), colHaveX, rowY + 5, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, String.valueOf(haveCount), colHaveX, rowY + 5, 0xFFFFFFFF);
//#endif
            int missCount = Math.max(0, row.totalCount - haveCount);
            int missColor = missCount == 0 ? 0xFF55FF55 : (haveCount >= missCount ? 0xFFFFAA00 : 0xFFFF5555);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTextWithShadow(this.textRenderer, String.valueOf(missCount), colMissX, rowY + 5, missColor);
//#elseif MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, String.valueOf(missCount), colMissX, rowY + 5, missColor);
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 展开/折叠图标（居中于移除按钮中轴, 无按钮背景）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 灞曞紑/鎶樺彔鍥炬爣锛堝眳涓簬绉婚櫎鎸夐挳涓酱, 鏃犳寜閽儗鏅級
//#endif
            if (row.sources.size() > 1) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                int removeCenterX = this.width - 12 - 25; // 移除按钮宽50, 中轴
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                int removeCenterX = this.width - 12 - 25; // 绉婚櫎鎸夐挳瀹?0, 涓酱
//#endif
                int expandX = removeCenterX - EXPAND_ICON_SIZE / 2;
                boolean expHovered = mouseX >= expandX && mouseX <= expandX + EXPAND_ICON_SIZE
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
                Identifier expIcon;
                if (row.expanded) expIcon = expHovered ? COLLAPSE_HOVERED : COLLAPSE_NORMAL;
                else expIcon = expHovered ? EXPAND_HOVERED : EXPAND_NORMAL;
//#if MC == 12108 || MC == 12110 || MC == 12111
                drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, expIcon, expandX, rowY + 2, 0.0f, 0.0f, EXPAND_ICON_SIZE, EXPAND_ICON_SIZE, EXPAND_ICON_SIZE, EXPAND_ICON_SIZE);
//#elseif MC == 11701 || MC == 11904
//$$                RenderSystem.setShaderTexture(0, expIcon); DrawableHelper.drawTexture(matrices, expandX, rowY + 2, 0.0f, 0.0f, EXPAND_ICON_SIZE, EXPAND_ICON_SIZE, EXPAND_ICON_SIZE, EXPAND_ICON_SIZE);
//#elseif MC == 12001
//$$                drawContext.drawTexture(expIcon, expandX, rowY + 2, 0.0f, 0.0f, EXPAND_ICON_SIZE, EXPAND_ICON_SIZE, EXPAND_ICON_SIZE, EXPAND_ICON_SIZE);
//#elseif MC == 12101
//$$                drawContext.drawTexture( expIcon, expandX, rowY + 2, 0.0f, 0.0f, EXPAND_ICON_SIZE, EXPAND_ICON_SIZE, EXPAND_ICON_SIZE, EXPAND_ICON_SIZE);
//#elseif MC == 12104
//$$                drawContext.drawTexture(RenderLayer::getGuiTextured, expIcon, expandX, rowY + 2, 0.0f, 0.0f, EXPAND_ICON_SIZE, EXPAND_ICON_SIZE, EXPAND_ICON_SIZE, EXPAND_ICON_SIZE);
//#endif
            }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 展开的子项（仅显示未忽略的）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 灞曞紑鐨勫瓙椤癸紙浠呮樉绀烘湭蹇界暐鐨勶級
//#endif
            if (row.expanded) {
                int subNameX = nameX + 10;
                int subIdx = 0;
                for (int j = 0; j < row.sources.size(); j++) {
                    SourceEntry src = row.sources.get(j);
                    if (row.isSourceIgnored(src)) continue; // 已忽略的不显示
                    int subY = rowY + ROW_HEIGHT + subIdx * ROW_HEIGHT;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    drawContext.fill(10, subY, listRight, subY + ROW_HEIGHT, (subIdx % 2 == 0) ? 0x18FFFFFF : 0x08FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$                    DrawableHelper.fill(matrices, 10, subY, listRight, subY + ROW_HEIGHT, (subIdx % 2 == 0) ? 0x18FFFFFF : 0x08FFFFFF);
//#endif
                    ItemStack subIcon = (src.exact && src.exactStack != null) ? src.exactStack : new ItemStack(src.item);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    drawContext.drawItem(subIcon, subNameX, subY + 2);
//#elseif MC == 11701
//$$                    MinecraftClient.getInstance().getItemRenderer().renderInGui(subIcon, subNameX, subY + 2);
//#elseif MC == 11904
//$$                    MinecraftClient.getInstance().getItemRenderer().renderInGui(matrices, subIcon, subNameX, subY + 2);
//#endif
                    String subLabel = src.exact ? src.key : src.item.getName().getString();
                    String subText = " - " + subLabel + " " + src.count + "  (" + src.listName + ")";
                    int textEndX = subNameX + 18 + this.textRenderer.getWidth(subText);
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    // 如果被替换，显示原物品图标和名称（精确物品不参与按物品替换）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    // 濡傛灉琚浛鎹紝鏄剧ず鍘熺墿鍝佸浘鏍囧拰鍚嶇О
//#endif
                    if (!src.exact && src.item != src.originalItem) {
                        int origIconX = textEndX + 4;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        drawContext.drawItem(new ItemStack(src.originalItem), origIconX, subY + 2);
//#elseif MC == 11701
//$$                        MinecraftClient.getInstance().getItemRenderer().renderInGui(new ItemStack(src.originalItem), origIconX, subY + 2);
//#elseif MC == 11904
//$$                        MinecraftClient.getInstance().getItemRenderer().renderInGui(matrices, new ItemStack(src.originalItem), origIconX, subY + 2);
//#endif
                        String origText = " " + src.originalItem.getName().getString() + "（原有）";
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        drawContext.drawTextWithShadow(this.textRenderer, origText, origIconX + 18, subY + 5, 0xCCAAAAAA);
//#elseif MC == 11701 || MC == 11904
//$$                        this.textRenderer.drawWithShadow(matrices, origText, origIconX + 18, subY + 5, 0xCCAAAAAA);
//#endif
                    }
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    drawContext.drawTextWithShadow(this.textRenderer, subText, subNameX + 18, subY + 5, 0xCCCCCCCC);
//#elseif MC == 11701 || MC == 11904
//$$                    this.textRenderer.drawWithShadow(matrices, subText, subNameX + 18, subY + 5, 0xCCCCCCCC);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    // 子项数量级 tooltip
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    // 瀛愰」鏁伴噺绾?tooltip
//#endif
                    if (mouseX >= subNameX && mouseX <= listRight && mouseY >= subY && mouseY < subY + ROW_HEIGHT) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        drawContext.drawTooltip(this.textRenderer, Text.literal(formatCountTooltip(src.count)), mouseX, mouseY);
//#elseif MC == 11701
//$$                        this.renderTooltip(matrices, new LiteralText(formatCountTooltip(src.count)), mouseX, mouseY);
//#elseif MC == 11904
//$$                        this.renderTooltip(matrices, Text.literal(formatCountTooltip(src.count)), mouseX, mouseY);
//#endif
                    }
                    subIdx++;
                }
            }

            cumulativeY += row.getRowHeight();
            count++;
            if (count >= visibleRows) break;
        }

        int headerNameX = allowPin ? 53 : 35;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 搜索图标
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鎼滅储鍥炬爣
//#endif
        int searchIconX = headerNameX - 20;
//#if MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, SEARCH_ICON, searchIconX, HEADER_Y + 1, 0.0f, 0.0f, 14, 14, 14, 14);
//#elseif MC == 11701 || MC == 11904
//$$        RenderSystem.setShaderTexture(0, SEARCH_ICON); DrawableHelper.drawTexture(matrices, searchIconX, HEADER_Y + 1, 0.0f, 0.0f, 14, 14, 14, 14);
//#elseif MC == 12001
//$$        drawContext.drawTexture(SEARCH_ICON, searchIconX, HEADER_Y + 1, 0.0f, 0.0f, 14, 14, 14, 14);
//#elseif MC == 12101
//$$        drawContext.drawTexture( SEARCH_ICON, searchIconX, HEADER_Y + 1, 0.0f, 0.0f, 14, 14, 14, 14);
//#elseif MC == 12104
//$$        drawContext.drawTexture(RenderLayer::getGuiTextured, SEARCH_ICON, searchIconX, HEADER_Y + 1, 0.0f, 0.0f, 14, 14, 14, 14);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 搜索框打开时不绘制"物品"表头文字（搜索框覆盖该区域，避免与输入文字重叠冲突）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 先画"物品"文字（搜索框打开时覆盖在上面）
//#endif
        if (!searchActive) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTextWithShadow(this.textRenderer, I18n.tr("litematlist.header.item"), headerNameX, HEADER_Y + 3, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, I18n.tr("litematlist.header.item"), headerNameX, HEADER_Y + 3, 0xFFFFFFFF);
//#endif
        }
        if (searchActive && searchField != null) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(searchIconX + 18 - 2, HEADER_Y - 1, searchIconX + 18 + 82, HEADER_Y + 15, 0xFF000000);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, searchIconX + 18 - 2, HEADER_Y - 1, searchIconX + 18 + 82, HEADER_Y + 15, 0xFF000000);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(searchIconX + 18, HEADER_Y, searchIconX + 18 + 80, HEADER_Y + 14, 0xFF222222);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, searchIconX + 18, HEADER_Y, searchIconX + 18 + 80, HEADER_Y + 14, 0xFF222222);
//#endif
            String st = searchField.getText();
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawText(this.textRenderer, st, searchIconX + 21, HEADER_Y + 3, 0xFFFFFFFF, false);
//#elseif MC == 11701 || MC == 11904
//$$            this.textRenderer.draw(matrices, st, searchIconX + 21, HEADER_Y + 3, 0xFFFFFFFF);
//#endif
            if (searchField.isFocused() && System.currentTimeMillis() % 1000 < 500) {
                int cp = searchField.getCursor();
                String bc = st.substring(0, Math.min(cp, st.length()));
                int cx = searchIconX + 21 + this.textRenderer.getWidth(bc);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.fill(cx, HEADER_Y + 2, cx + 1, HEADER_Y + 12, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$                DrawableHelper.fill(matrices, cx, HEADER_Y + 2, cx + 1, HEADER_Y + 12, 0xFFFFFFFF);
//#endif
            }
        }
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawTextWithShadow(this.textRenderer,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                I18n.tr("litematlist.header.total") + (sortMode == SortMode.TOTAL_DESC || sortMode == SortMode.TOTAL_ASC ? sortMode.arrow : ""),
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                colTotalX, HEADER_Y + 3, 0xFFFFFFFF);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawTextWithShadow(this.textRenderer,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                I18n.tr("litematlist.header.have") + (sortMode == SortMode.HAVE_DESC || sortMode == SortMode.HAVE_ASC ? sortMode.arrow : ""),
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                colHaveX, HEADER_Y + 3, 0xFFFFFFFF);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawTextWithShadow(this.textRenderer,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                I18n.tr("litematlist.header.missing") + (sortMode == SortMode.MISS_DESC || sortMode == SortMode.MISS_ASC ? sortMode.arrow : ""),
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                colMissX, HEADER_Y + 3, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$        this.textRenderer.drawWithShadow(matrices, I18n.tr("litematlist.header.total") + (sortMode == SortMode.TOTAL_DESC || sortMode == SortMode.TOTAL_ASC ? sortMode.arrow : ""), colTotalX, HEADER_Y + 3, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$        this.textRenderer.drawWithShadow(matrices, I18n.tr("litematlist.header.have") + (sortMode == SortMode.HAVE_DESC || sortMode == SortMode.HAVE_ASC ? sortMode.arrow : ""), colHaveX, HEADER_Y + 3, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$        this.textRenderer.drawWithShadow(matrices, I18n.tr("litematlist.header.missing") + (sortMode == SortMode.MISS_DESC || sortMode == SortMode.MISS_ASC ? sortMode.arrow : ""), colMissX, HEADER_Y + 3, 0xFFFFFFFF);
//#endif

        if (hoveredRow >= 0 && hoveredColumn >= 0 && hoveredRow < displayRows.size()) {
            MergedRow hovered = displayRows.get(hoveredRow);
            int haveCount = getPlayerItemCount(hovered);
            int val = switch (hoveredColumn) {
                case 0 -> hovered.totalCount;
                case 1 -> haveCount;
                case 2 -> Math.max(0, hovered.totalCount - haveCount);
                default -> hovered.totalCount;
            };
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTooltip(this.textRenderer, Text.literal(formatCountTooltip(val)), mouseX, mouseY);
//#elseif MC == 11701
//$$            this.renderTooltip(matrices, new LiteralText(formatCountTooltip(val)), mouseX, mouseY);
//#elseif MC == 11904
//$$            this.renderTooltip(matrices, Text.literal(formatCountTooltip(val)), mouseX, mouseY);
//#endif
        }
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 12110 || MC == 12111

//#endif
        // 精确物品图标悬停：物品悬浮提示（在名称提示之前绘制，名称提示叠于其上）
        if (hoveredExactStack != null) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawItemTooltip(this.textRenderer, hoveredExactStack, mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$            this.renderTooltip(matrices, hoveredExactStack, mouseX, mouseY);
//#endif
        }
        // 悬停物品名：精确物品或名称被截断时显示完整名称/精确ID（过长时按屏幕边界自动换行）
        if (hoveredFullName != null) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawWrappedTooltip(drawContext, hoveredFullName, mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$            drawWrappedTooltip(matrices, hoveredFullName, mouseX, mouseY);
//#endif
        }
        // 悬停普通物品图标显示对应物品ID
        if (hoveredItemId != null) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTooltip(this.textRenderer, Text.literal(hoveredItemId), mouseX, mouseY);
//#elseif MC == 11701
//$$            this.renderTooltip(matrices, new LiteralText(hoveredItemId), mouseX, mouseY);
//#elseif MC == 11904
//$$            this.renderTooltip(matrices, Text.literal(hoveredItemId), mouseX, mouseY);
//#endif
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 计算总内容高度（像素）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 璁＄畻鎬诲唴瀹归珮搴︼紙鍍忕礌锛?            
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            int totalContentHeight = 0;
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        int totalContentHeight = 0;
//#endif
            for (int i = 0; i < displayRows.size(); i++) {
                totalContentHeight += displayRows.get(i).getRowHeight();
            }
            int trackHeight = maxRenderY - LIST_TOP;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 只有总内容超出可见区域时才显示滚动条
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 鍙湁鎬诲唴瀹硅秴鍑哄彲瑙佸尯鍩熸椂鎵嶆樉绀烘粴鍔ㄦ潯
//#endif
            if (totalContentHeight > trackHeight) {
                int scrollInfoY = listBottom + 5;
                String scrollInfo = (scrollOffset + 1) + "-" + Math.min(scrollOffset + visibleRows, displayRows.size()) + " / " + displayRows.size();
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.drawCenteredTextWithShadow(this.textRenderer, scrollInfo, this.width / 2, scrollInfoY, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$                this.textRenderer.drawWithShadow(matrices, scrollInfo, this.width / 2 - this.textRenderer.getWidth(scrollInfo) / 2, scrollInfoY, 0xFFFFFFFF);
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 滚动条（像素级计算，适配可变行高）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                //  this.width - 6;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                int scrollbarX = this.width - 6;
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
                int scrollbarWidth = 3;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 轨道
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                int scrollbarX = this.width - 6;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                // 杞ㄩ亾
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.fill(scrollbarX, LIST_TOP, scrollbarX + scrollbarWidth, listBottom, 0x30FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$                DrawableHelper.fill(matrices, scrollbarX, LIST_TOP, scrollbarX + scrollbarWidth, listBottom, 0x30FFFFFF);
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 计算 scrollOffset 对应的像素偏移
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                // 璁＄畻 scrollOffset 瀵瑰簲鐨勫儚绱犲亸绉?
//#endif
                int scrolledPixels = 0;
                for (int i = 0; i < scrollOffset && i < displayRows.size(); i++) {
                    scrolledPixels += displayRows.get(i).getRowHeight();
                }
                int maxScrollPixels = Math.max(1, totalContentHeight - trackHeight);
                float ratio = (float) trackHeight / totalContentHeight;
                int thumbHeight = Math.max(8, (int)(trackHeight * ratio));
                int thumbY = LIST_TOP + (int)((trackHeight - thumbHeight) * (float) scrolledPixels / maxScrollPixels);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.fill(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, 0x80FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$                DrawableHelper.fill(matrices, scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, 0x80FFFFFF);
//#endif
            }
        }

    @Override
//#if MC == 12110 || MC == 12111
    public boolean onMouseClicked(Click click, boolean isDrag) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$    public boolean onMouseClicked(int mouseX, int mouseY, int button) {
//#endif
//#if MC == 12110 || MC == 12111
        if (super.onMouseClicked(click, isDrag)) return true;
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        if (super.mouseClicked(mouseX, mouseY, mouseButton)) return true;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        if (super.onMouseClicked(mouseX, mouseY, button)) return true;
//#endif
        if (ignoreJustClicked) {
            ignoreJustClicked = false;
            return true;
        }
//#if MC == 12110 || MC == 12111
        if (isDrag) return false;
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        
//#endif
//#if MC == 12110 || MC == 12111
        int mouseX = (int) click.x();
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        mouseX = (int) mouseX;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        
//#endif
//#if MC == 12110 || MC == 12111
        int mouseY = (int) click.y();
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        mouseY = (int) mouseY;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        
//#endif
//#if MC == 12110 || MC == 12111
        if (click.button() == 0) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        if (mouseButton == 0) {
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        if (button == 0) {
//#endif
            // 搜索框本体点击：优先聚焦（否则点击被表头逻辑吞掉，输入框永远无法获得焦点）
            int searchBoxX = (allowPin ? 53 : 35) - 20 + 18;
            if (searchActive && searchField != null
                    && mouseX >= searchBoxX - 2 && mouseX <= searchBoxX + 82
                    && mouseY >= HEADER_Y - 1 && mouseY <= HEADER_Y + 15) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                searchField.setFocused(true);
//#elseif MC == 11701
//$$                this.setFocused(searchField);
//#endif
                return true;
            }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 搜索图标点击
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 鎼滅储鍥炬爣鐐瑰嚮
//#endif
            int searchIconX = (allowPin ? 53 : 35) - 20;
            if (mouseX >= searchIconX && mouseX <= searchIconX + 16
                    && mouseY >= HEADER_Y && mouseY <= HEADER_Y + HEADER_HEIGHT) {
                searchActive = !searchActive;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (searchActive && searchField != null) searchField.setFocused(true);
//#elseif MC == 11701
//$$                if (searchActive && searchField != null) this.setFocused(searchField);
//#endif
                if (!searchActive) {
                    searchText = "";
                    if (searchField != null) {
                        updatingSearchField = true;
                        try { searchField.setText(""); }
                        finally { updatingSearchField = false; }
                    }
                }
                initGui();
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (searchActive && searchField != null) searchField.setFocused(true);
//#elseif MC == 11701
//$$                if (searchActive && searchField != null) this.setFocused(searchField);
//#endif
                return true;
            }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 【已忽略】按钮手动点击
//#endif
            if (mouseX >= ignoredBtnX && mouseX <= ignoredBtnX + ignoredBtnW
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    && mouseY >= ignoredBtnY && mouseY <= ignoredBtnY + ignoredBtnH) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                        && mouseY >= ignoredBtnY && mouseY <= ignoredBtnY + ignoredBtnH) {
//#endif
                openIgnoredScreen();
                return true;
            }

            if (mouseY >= HEADER_Y && mouseY <= HEADER_Y + HEADER_HEIGHT) { handleHeaderClick(mouseX, mouseY); return true; }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 展开/折叠图标点击
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 灞曞紑/鎶樺彔鍥炬爣鐐瑰嚮
//#endif
            List<MergedRow> displayRows = getSortedRows();
            int cumulativeY = LIST_TOP;
            int count = 0;
            int removeCenterX = this.width - 12 - 25;
            int expandX = removeCenterX - EXPAND_ICON_SIZE / 2;

            for (int i = scrollOffset; i < displayRows.size() && count < visibleRows; i++) {
                MergedRow row = displayRows.get(i);
                int rowY = cumulativeY;
                if (row.sources.size() > 1 && mouseX >= expandX && mouseX <= expandX + EXPAND_ICON_SIZE
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                    row.expanded = !row.expanded;
                    if (row.expanded) expandedItems.add(row.key);
                    else expandedItems.remove(row.key);
                    initGui();
                    return true;
                }
                cumulativeY += row.getRowHeight();
                count++;
            }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 置顶点击
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 缃《鐐瑰嚮
//#endif
            if (allowPin) {
                displayRows = getSortedRows();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                int pinRow = getRowAtY(mouseY, displayRows);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                int pinRow = getRowAtY((int)mouseY, displayRows);
//#endif
                if (pinRow >= 0 && mouseX >= 15 && mouseX <= 31) {
                    MergedRow row = displayRows.get(pinRow);
                    if (currentPinned.contains(row.key)) { currentPinned.remove(row.key); syncPinToSubLists(row.key, false); }
                    else { currentPinned.add(row.key); syncPinToSubLists(row.key, true); }
                    MaterialDataStore.savePersistence();
                    initGui();
                    return true;
                }
            }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 拖拽重排
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 鎷栨嫿閲嶆帓
//#endif
            if (Configs.Generic.ALLOW_MANUAL_REORDER.getBooleanValue()) {
                List<MergedRow> dragDisplay = getSortedRows();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                int row = getRowAtY(mouseY, dragDisplay);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                int row = getRowAtY((int)mouseY, dragDisplay);
//#endif
                if (row >= 0 && row < dragDisplay.size() && mouseX >= 10 && mouseX < this.width - 80) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    // 检查是否点击在子项上
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    // 检查是否点击在子项中
//#endif
                    MergedRow parentRow = dragDisplay.get(row);
                    if (parentRow.expanded && mouseY >= getRowY(row) + ROW_HEIGHT) {
                        // 计算可见（非忽略）子项索引
                        int subIdx = 0;
                        int subY = getRowY(row) + ROW_HEIGHT;
                        for (int j = 0; j < parentRow.sources.size(); j++) {
                            if (parentRow.isSourceIgnored(parentRow.sources.get(j))) continue;
                            if (mouseY >= subY && mouseY < subY + ROW_HEIGHT) {
                                if (parentRow.sources.size() > 1) {
                                    draggedSubIndex = subIdx;
                                    draggedSubParentIdx = row;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                    dragStartY = mouseY;
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                                    dragStartY = (int) mouseY;
//#endif
                                    return true;
                                }
                                break;
                            }
                            subY += ROW_HEIGHT;
                            subIdx++;
                        }
                    } else {
                        draggedRow = row;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        dragStartY = mouseY;
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                        dragStartY = (int) mouseY;
//#endif
                        isDragging = false;
                        return true;
                    }
                }
            }
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 滚动条点击（像素级）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 婊氬姩鏉＄偣鍑伙紙鍍忕礌绾э級
//#endif
        List<MergedRow> displayRows = getSortedRows();
        int trackHeight = (this.height - 45) - LIST_TOP;
        int scrollbarX = this.width - 6;
        int scrollbarWidth = 3;
        int totalContentHeight = 0;
        for (MergedRow r : displayRows) totalContentHeight += r.getRowHeight();
        if (totalContentHeight > trackHeight && mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth
                && mouseY >= LIST_TOP && mouseY <= LIST_TOP + trackHeight) {
            int maxScrollPixels = Math.max(1, totalContentHeight - trackHeight);
            int desiredPixels = Math.max(0, Math.min(maxScrollPixels, (int)((float)(mouseY - LIST_TOP) / trackHeight * maxScrollPixels)));
            int cumulativePixels = 0;
            int newOffset = 0;
            for (int i = 0; i < displayRows.size(); i++) {
                int h = displayRows.get(i).getRowHeight();
                if (cumulativePixels + h / 2 > desiredPixels) break;
                cumulativePixels += h;
                newOffset = i + 1;
            }
            scrollOffset = Math.max(0, Math.min(newOffset, Math.max(0, displayRows.size() - visibleRows)));
            scrollbarDragging = true;
            initGui();
            return true;
        }
        return false;
    }

    @Override
//#if MC == 12110 || MC == 12111
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
//#endif
//#if MC == 12110 || MC == 12111
        int mouseY = (int) click.y();
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        mouseY = (int) mouseY;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        
//#endif
//#if MC == 12110 || MC == 12111
        int button = click.button();
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        int button = mouseButton;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        
//#endif

        if (scrollbarDragging && button == 0) {
            List<MergedRow> displayRows = getSortedRows();
            int trackHeight = (this.height - 45) - LIST_TOP;
            int totalContentHeight = 0;
            for (MergedRow r : displayRows) totalContentHeight += r.getRowHeight();
            int maxScrollPixels = Math.max(1, totalContentHeight - trackHeight);
            int desiredPixels = Math.max(0, Math.min(maxScrollPixels, (int)((float)(mouseY - LIST_TOP) / trackHeight * maxScrollPixels)));
            int cumulativePixels = 0;
            int newOffset = 0;
            for (int i = 0; i < displayRows.size(); i++) {
                int h = displayRows.get(i).getRowHeight();
                if (cumulativePixels + h / 2 > desiredPixels) break;
                cumulativePixels += h;
                newOffset = i + 1;
            }
            scrollOffset = Math.max(0, Math.min(newOffset, Math.max(0, displayRows.size() - visibleRows)));
            initGui();
            return true;
        }

        if (draggedSubIndex >= 0 && draggedSubParentIdx >= 0 && button == 0 && Configs.Generic.ALLOW_MANUAL_REORDER.getBooleanValue()) {
            List<MergedRow> display = getSortedRows();
            if (draggedSubParentIdx < display.size()) {
                MergedRow parent = display.get(draggedSubParentIdx);
                if (parent.expanded && Math.abs(mouseY - dragStartY) > 5) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    // 计算当前鼠标所在的可见子项索引
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    // 璁＄畻褰撳墠榧犳爣鎵€鍦ㄧ殑鍙瀛愰」绱㈠紩
//#endif
                    int targetSubIdx = 0;
                    int subY = getRowY(draggedSubParentIdx) + ROW_HEIGHT;
                    for (int j = 0; j < parent.sources.size(); j++) {
                        if (parent.isSourceIgnored(parent.sources.get(j))) continue;
                        if (mouseY >= subY && mouseY < subY + ROW_HEIGHT) {
                            if (targetSubIdx != draggedSubIndex) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                // 将可见索引映射到实际 sources 列表索引
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                                // 灏嗗彲瑙佺储寮曟槧灏勫埌瀹為檯 sources 鍒楄〃绱㈠紩
//#endif
                                int actualFrom = -1, actualTo = -1;
                                int visCount = 0;
                                for (int k = 0; k < parent.sources.size(); k++) {
                                    if (parent.isSourceIgnored(parent.sources.get(k))) continue;
                                    if (visCount == draggedSubIndex) actualFrom = k;
                                    if (visCount == targetSubIdx) actualTo = k;
                                    visCount++;
                                }
                                if (actualFrom >= 0 && actualTo >= 0 && actualFrom != actualTo) {
                                    Collections.swap(parent.sources, actualFrom, actualTo);
                                    draggedSubIndex = targetSubIdx;
                                    initGui();
                                }
                            }
                            break;
                        }
                        subY += ROW_HEIGHT;
                        targetSubIdx++;
                    }
                    return true;
                }
            }
        }

        if (draggedRow >= 0 && button == 0 && Configs.Generic.ALLOW_MANUAL_REORDER.getBooleanValue()) {
            List<MergedRow> display = getSortedRows();
//#if MC == 12110 || MC == 12111
            int currentRow = getRowAtY(mouseY, display);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            int currentRow = getRowAtY((int)mouseY, display);
//#endif
            if (currentRow >= 0 && currentRow < display.size() && currentRow != draggedRow && Math.abs(mouseY - dragStartY) > 5) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 不允许跨置顶边界拖拽
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                // 涓嶅厑璁歌法缃《杈圭晫鎷栨嫿
//#endif
                if (allowPin) {
                    MergedRow dragged = display.get(draggedRow);
                    MergedRow target = display.get(currentRow);
                    boolean draggedPinned = currentPinned.contains(dragged.key);
                    boolean targetPinned = currentPinned.contains(target.key);
                    if (draggedPinned != targetPinned) {
                        return true;
                    }
                }
                isDragging = true;
                MergedRow draggedItem = display.get(draggedRow);
                MergedRow targetItem = display.get(currentRow);
                int actualDraggedIdx = rows.indexOf(draggedItem);
                int actualTargetIdx = rows.indexOf(targetItem);
                if (actualDraggedIdx >= 0 && actualTargetIdx >= 0) {
                    Collections.swap(rows, actualDraggedIdx, actualTargetIdx);
                    sortMode = SortMode.NONE;
                    initGui();
                }
                draggedRow = currentRow;
                dragStartY = (int)mouseY;
            }
            return true;
        }
//#if MC == 12110 || MC == 12111
        return super.mouseDragged(click, deltaX, deltaY);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        return super.mouseDragged(mouseX, mouseY, mouseButton, deltaX, deltaY);
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
//#endif
    }

    @Override
//#if MC == 12110 || MC == 12111
    public boolean mouseReleased(Click click) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$    public boolean mouseReleased(double mouseX, double mouseY, int button) {
//#endif
        if (scrollbarDragging) {
            scrollbarDragging = false;
            return true;
        }
        if (draggedSubIndex >= 0 || draggedSubParentIdx >= 0) {
            draggedSubIndex = -1;
            draggedSubParentIdx = -1;
            return true;
        }
        if (draggedRow >= 0) {
            draggedRow = -1;
            isDragging = false;
            return true;
        }
//#if MC == 12110 || MC == 12111
        return super.mouseReleased(click);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        return super.mouseReleased(mouseX, mouseY, mouseButton);
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        return super.mouseReleased(mouseX, mouseY, button);
//#endif
    }

    private int getRowAtY(int mouseY, List<MergedRow> displayRows) {
        if (mouseY < LIST_TOP) return -1;
        int cumulativeY = LIST_TOP;
        int count = 0;
        for (int i = scrollOffset; i < displayRows.size() && count < visibleRows; i++) {
            MergedRow row = displayRows.get(i);
            int rowH = row.getRowHeight();
            if (mouseY >= cumulativeY && mouseY < cumulativeY + rowH) return i;
            cumulativeY += rowH;
            count++;
        }
        return -1;
    }

    @Override
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
//#endif
        if (mouseY >= LIST_TOP && mouseY <= this.height - 45) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            scrollOffset = Math.max(0, scrollOffset - (int) verticalAmount);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            scrollOffset = Math.max(0, scrollOffset - (int) amount);
//#endif
            scrollOffset = Math.min(scrollOffset, Math.max(0, getSortedRows().size() - visibleRows));
            initGui();
            return true;
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        return super.mouseScrolled(mouseX, mouseY, amount);
//#endif
    }

    @Override
//#if MC == 12110 || MC == 12111
    public boolean keyPressed(KeyInput keyInput) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
//#endif
//#if MC == 12110 || MC == 12111
        if (keyInput.key() == 256) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        if (keyCode == 256) {
//#endif
            if (searchActive) {
                searchActive = false;
                searchText = "";
                if (searchField != null) {
                    updatingSearchField = true;
                    try { searchField.setText(""); }
                    finally { updatingSearchField = false; }
                }
                initGui();
                return true;
            }
            MinecraftClient.getInstance().setScreen(parent);
            return true;
        }
        if (searchActive && searchField != null && searchField.isFocused()) {
//#if MC == 12110 || MC == 12111
            if (searchField.keyPressed(keyInput)) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            if (searchField.keyPressed(keyCode, scanCode, modifiers)) {
//#endif
                searchText = searchField.getText();
                initGui();
                return true;
            }
        }
//#if MC == 12110 || MC == 12111
        return super.keyPressed(keyInput);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        return super.keyPressed(keyCode, scanCode, modifiers);
//#endif
    }

    @Override
//#if MC == 12110 || MC == 12111
    public boolean charTyped(CharInput charInput) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$    public boolean charTyped(char chr, int modifiers) {
//#endif
        if (searchActive && searchField != null && searchField.isFocused()) {
//#if MC == 12110 || MC == 12111
            if (searchField.charTyped(charInput)) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            if (searchField.charTyped(chr, modifiers)) {
//#endif
                searchText = searchField.getText();
                initGui();
                return true;
            }
        }
//#if MC == 12110 || MC == 12111
        return super.charTyped(charInput);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        return super.charTyped(chr, modifiers);
//#endif
    }
}
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$
//$$
//$$
//$$
//$$
//#endif