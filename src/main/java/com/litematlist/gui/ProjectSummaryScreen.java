package com.litematlist.gui;

import com.litematlist.I18n;
import com.litematlist.LitematicReader;
import com.litematlist.LitematListMod;
import com.litematlist.MaterialListImporter;
import com.litematlist.PinyinSearch;
import com.litematlist.config.Configs;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.util.InventoryUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gl.RenderPipelines;


import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent;
import net.minecraft.util.Identifier;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 项目总材料列表 —— 合并项目内所有材料列表，支持共享忽略/置顶状态和来源明细展开。
 */
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

    // ---- 拖拽重排 ----
    private int draggedRow = -1;
    private int dragStartY = 0;
    private boolean isDragging = false;
    private int draggedSubIndex = -1;    // 子项拖拽：当前拖拽的可见子项索引
    private int draggedSubParentIdx = -1; // 子项拖拽：父 MergedRow 的显示索引

    // ---- 忽略/展开时序保护 ----
    private boolean ignoreJustClicked = false;

    // ---- 已忽略 ----
    private final Set<Item> currentIgnored = new HashSet<>();
    private final Set<Item> expandedItems = new HashSet<>(); // 记录展开的物品

    // ---- 隐藏不缺材料 ----
    private boolean hideNoMissing = MaterialDetailScreen.persistedHideNoMissing;
    private int hideBtnX, hideBtnY, hideBtnW;
    private String hideBtnLabel = "";

    // ---- 标记材料 ----
    private static final Identifier PIN_EMPTY = Identifier.of("litematlist", "textures/gui/pin/empty.png");
    private static final Identifier PIN_FAVORITE = Identifier.of("litematlist", "textures/gui/pin/favorite.png");
    private static final Identifier PIN_HOVERED_EMPTY = Identifier.of("litematlist", "textures/gui/pin/hovered_empty.png");
    private static final Identifier PIN_HOVERED_FAVORITE = Identifier.of("litematlist", "textures/gui/pin/hovered_favorite.png");
    private boolean allowPin = MaterialDetailScreen.persistedAllowPin;
    private int pinBtnX, pinBtnY, pinBtnW;
    private String pinBtnLabel = "";
    private final Set<Item> currentPinned = new HashSet<>();

    // ---- 展开/折叠图标 ----
    private static final Identifier EXPAND_NORMAL = Identifier.of("litematlist", "textures/gui/expand/expand_normal.png");
    private static final Identifier EXPAND_HOVERED = Identifier.of("litematlist", "textures/gui/expand/expand_hovered.png");
    private static final Identifier COLLAPSE_NORMAL = Identifier.of("litematlist", "textures/gui/expand/collapse_normal.png");
    private static final Identifier COLLAPSE_HOVERED = Identifier.of("litematlist", "textures/gui/expand/collapse_hovered.png");
    private static final Identifier SEARCH_ICON = Identifier.of("litematlist", "textures/gui/search.png");

    private int colTotalX, colHaveX, colMissX;
    private int ignoredBtnX, ignoredBtnY, ignoredBtnW, ignoredBtnH; // 【已忽略】按钮位置

    // ---- 搜索 ----
    private boolean searchActive = false;
    private String searchText = "";
    private TextFieldWidget searchField;

    private static final int BTN_IGNORE_W = 52;
    private static final int BTN_REPLACE_W = 52;
    private static final int BTN_CANCEL_W = 52;
    private static final int BTN_RIGHT_MARGIN = 12;
    private static final int BTN_GAP = 4;
    private static final int EXPAND_ICON_SIZE = 16;

    private enum SortMode {
        NONE(""), TOTAL_DESC("▼"), TOTAL_ASC("▲"),
        HAVE_DESC("▼"), HAVE_ASC("▲"), MISS_DESC("▼"), MISS_ASC("▲");
        final String arrow;
        SortMode(String arrow) { this.arrow = arrow; }
    }
    private SortMode sortMode = SortMode.NONE;

    private void initSortMode() {
        try { sortMode = SortMode.valueOf(MaterialDetailScreen.persistedProjectSortMode); } catch (Exception e) { sortMode = SortMode.NONE; }
    }

    // ---- 内部数据类 ----
    private static class SourceEntry {
        final String listName;
        final int count;
        final Item item;
        final Item originalItem; // 替换前的原始物品，用于精确匹配忽略集
        final Path filePath;
        SourceEntry(String listName, int count, Item item, Item originalItem, Path filePath) {
            this.listName = listName; this.count = count; this.item = item;
            this.originalItem = originalItem; this.filePath = filePath;
        }
    }

    private static class MergedRow {
        final ItemStack itemStack;
        final String name;
        int totalCount;
        final Item item;
        final Item originalItem;
        final List<SourceEntry> sources;
        boolean expanded;

        MergedRow(ItemStack itemStack, String name, int totalCount, Item item, List<SourceEntry> sources) {
            this.itemStack = itemStack; this.name = name; this.totalCount = totalCount;
            this.item = item; this.originalItem = item; this.sources = sources;
            this.expanded = false;
        }
        int getRowHeight() { return ROW_HEIGHT + (expanded ? (int) sources.stream().filter(s -> !isSourceIgnored(s)).count() * ROW_HEIGHT : 0); }

        boolean isSourceIgnored(SourceEntry src) {
            Set<Item> subIgnored = MaterialDetailScreen.getIgnoredForPath(src.filePath);
            if (subIgnored == null) return false;
            // 检查替换后的物品是否在忽略集中（通过 originalItem 精确匹配）
            Item checkItem = MaterialDetailScreen.getReplacement(src.filePath, src.originalItem);
            return subIgnored.contains(checkItem);
        }

        /** 计算非忽略来源的总数 */
        int getActiveTotal() {
            return sources.stream().filter(s -> !isSourceIgnored(s)).mapToInt(s -> s.count).sum();
        }

        /** 至少有一个来源未被忽略 */
        boolean hasActiveSources() {
            return sources.stream().anyMatch(s -> !isSourceIgnored(s));
        }

        /** 所有来源都被忽略 */
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

    // ---- 计算考虑展开行的位置 ----
    private int getRowY(int displayIndex) {
        List<MergedRow> display = getSortedRows();
        int y = LIST_TOP;
        for (int i = 0; i < displayIndex && i < display.size(); i++) {
            y += display.get(i).getRowHeight();
        }
        return y;
    }

    /**
     * 全局解析设置（如「是否计算容器数据」）切换后调用，强制重新加载材料并重建界面。
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
        // 【刷新列表】按钮
        ButtonGeneric refreshBtn = new ButtonGeneric(20, buttonY, 80, 20, I18n.tr("litematlist.button.refresh_list"));
        refreshBtn.setRenderDefaultBackground(true);
        this.addButton(refreshBtn, (IButtonActionListener) (btn, mb) -> { loaded = false; initGui(); });

        ButtonGeneric backButton = new ButtonGeneric(this.width / 2 - 60, buttonY, 120, 20, I18n.tr("litematlist.button.back"));
        backButton.setRenderDefaultBackground(true);
        this.addButton(backButton, (IButtonActionListener) (btn, mb) -> MinecraftClient.getInstance().setScreen(parent));

        boolean isEmpty = projectEntries.isEmpty();

        int ignoredCount = 0;
        for (MergedRow row : rows) {
            if (row.sources.stream().anyMatch(s -> {
                Set<Item> subIgnored = MaterialDetailScreen.getIgnoredForPath(s.filePath);
                return subIgnored != null && subIgnored.contains(s.item);
            })) ignoredCount++;
        }
        String ignoredLabel = ignoredCount > 0 ? I18n.tr("litematlist.button.ignored_count", ignoredCount) : I18n.tr("litematlist.button.ignored");
        int ignoredBtnW = Math.max(60, this.textRenderer.getWidth(ignoredLabel) + 16);

        String hideLabel = hideNoMissing ? I18n.tr("litematlist.button.hide_no_missing_on") : I18n.tr("litematlist.button.hide_no_missing_off");
        int hideBtnW = Math.max(60, this.textRenderer.getWidth(hideLabel) + 16);
        this.hideBtnX = 20; this.hideBtnY = 20; this.hideBtnW = hideBtnW; this.hideBtnLabel = hideLabel;
        ButtonGeneric hideBtn = new ButtonGeneric(hideBtnX, hideBtnY, hideBtnW, 20, "");
        hideBtn.setRenderDefaultBackground(true);
        if (isEmpty) hideBtn.setEnabled(false);
        this.addButton(hideBtn, (IButtonActionListener) (b, mb) -> { hideNoMissing = !hideNoMissing; MaterialDetailScreen.persistedHideNoMissing = hideNoMissing; MaterialDetailScreen.savePersistence(); initGui(); });

        String pinLabel = allowPin ? I18n.tr("litematlist.button.allow_pin_on") : I18n.tr("litematlist.button.allow_pin_off");
        int pinBtnW = Math.max(60, this.textRenderer.getWidth(pinLabel) + 16);
        this.pinBtnX = hideBtnX + hideBtnW + 4; this.pinBtnY = 20; this.pinBtnW = pinBtnW; this.pinBtnLabel = pinLabel;
        ButtonGeneric pinBtn = new ButtonGeneric(pinBtnX, pinBtnY, pinBtnW, 20, "");
        pinBtn.setRenderDefaultBackground(true);
        if (isEmpty) pinBtn.setEnabled(false);
        this.addButton(pinBtn, (IButtonActionListener) (b, mb) -> { allowPin = !allowPin; MaterialDetailScreen.persistedAllowPin = allowPin; MaterialDetailScreen.savePersistence(); initGui(); });

        // 导出按钮
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

        // 搜索框
        int searchIconX = (allowPin ? 53 : 35) - 20;
        if (searchField == null) {
            this.searchField = new TextFieldWidget(this.textRenderer, searchIconX + 18, HEADER_Y, 80, 14, Text.empty());
            this.searchField.setMaxLength(50);
            this.searchField.setDrawsBackground(false);
            this.searchField.setText(searchText);
            this.searchField.setChangedListener(this::onSearchChanged);
            this.addSelectableChild(this.searchField);
        } else {
            this.searchField.setX(searchIconX + 18);
            this.searchField.setY(HEADER_Y);
            if (!this.searchField.getText().equals(searchText)) {
                this.searchField.setText(searchText); // 相同文本不调用，防止触发监听器 → initGui 无限递归
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
        Map<Item, Integer> ignoredTotals = new HashMap<>();
        Set<Item> allIgnoredItems = new HashSet<>();
        for (MergedRow row : rows) {
            boolean anyIgnored = false;
            for (SourceEntry src : row.sources) {
                Set<Item> subIgnored = MaterialDetailScreen.getIgnoredForPath(src.filePath);
                if (subIgnored != null && subIgnored.contains(src.item)) {
                    anyIgnored = true;
                }
            }
            if (anyIgnored) {
                allIgnoredItems.add(row.item);
                ignoredTotals.put(row.item, row.totalCount);
            }
        }
        loaded = false; // 返回时重新加载
        MinecraftClient.getInstance().setScreen(
                new ProjectIgnoredItemsScreen(this, projectName, allIgnoredItems, ignoredTotals, projectEntries));
    }

    private void exportMaterials(String format) {
        try {
            Path dir = MinecraftClient.getInstance().runDirectory.toPath().resolve("litematlist").resolve("project");
            Files.createDirectories(dir);
            String dateStr = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date());
            String fileName = "material_list_" + dateStr + "." + format;
            Path filePath = dir.resolve(fileName);

            if ("txt".equals(format)) {
                exportTxt(filePath);
            } else if ("json".equals(format)) {
                exportJson(filePath);
            } else {
                exportCsv(filePath);
            }
            LitematListMod.LOGGER.info("已导出材料列表: {}", filePath);

            // 聊天栏提示
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                String fmt = format.toUpperCase();
                Text link = Text.literal(fileName)
                        .styled(style -> style
                                .withClickEvent(new ClickEvent.OpenFile(filePath.toString()))
                                .withUnderline(true)
                                .withColor(0x55FFFF));
                Text msg = Text.literal("§a材料列表已导出为" + fmt + ": ").append(link);
                client.player.sendMessage(msg, false);
            }
        } catch (Exception e) {
            LitematListMod.LOGGER.error("导出失败", e);
        }
    }

    private void exportTxt(Path filePath) throws IOException {
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(filePath.toFile()), StandardCharsets.UTF_8))) {
            // 表头（与投影格式完全一致）
            String title = "项目'" + projectName + "'材料列表";
            w.write("+---------+-------+---------+-----------+");
            w.newLine();
            // 标题行：填充到与表格等宽
            w.write("| " + padTitle(title, 43) + " |");
            w.newLine();
            w.write("+---------+-------+---------+-----------+");
            w.newLine();
            w.write("| Item    | Total | Missing | Available |");
            w.newLine();
            w.write("+---------+-------+---------+-----------+");
            w.newLine();

            for (MergedRow row : rows) {
                if (currentIgnored.contains(row.item)) continue;
                int have = getPlayerItemCount(row.item);
                int missing = Math.max(0, row.totalCount - have);
                int available = Math.min(have, row.totalCount);
                String name = row.name;
                // 投影格式：物品名最多9个字符宽（中文算2个显示宽）
                if (getDisplayWidth(name) > 9) name = truncateByWidth(name, 9);
                w.write(String.format("| %-9s | %5d | %7d | %9d |",
                        name, row.totalCount, missing, available));
                w.newLine();
            }
            w.write("+---------+-------+---------+-----------+");
            w.newLine();
        }
    }

    /** 计算字符串的显示宽度（ASCII 1, 中文等宽字符 2） */
    private static int getDisplayWidth(String s) {
        int w = 0;
        for (char c : s.toCharArray()) {
            w += (c >= 0x4E00 && c <= 0x9FFF) || (c >= 0x3000 && c <= 0x303F) || (c >= 0xFF00 && c <= 0xFFEF) ? 2 : 1;
        }
        return w;
    }

    /** 按显示宽度截断字符串 */
    private static String truncateByWidth(String s, int maxWidth) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            w += (c >= 0x4E00 && c <= 0x9FFF) || (c >= 0x3000 && c <= 0x303F) || (c >= 0xFF00 && c <= 0xFFEF) ? 2 : 1;
            if (w > maxWidth) return s.substring(0, i);
        }
        return s;
    }

    /** 标题填充到指定宽度 */
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
            w.write("  \"Name\": \"" + projectName + "\",");
            w.newLine();
            String title = "项目'" + projectName + "'材料列表";
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
            for (MergedRow row : rows) {
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
            // 标题行（与 Litematica 导出的 CSV 格式一致）
            w.write("\"Item\",\"Total\",\"Missing\",\"Available\"");
            w.newLine();
            for (MergedRow row : rows) {
                if (currentIgnored.contains(row.item)) continue;
                int have = getPlayerItemCount(row.item);
                int missing = Math.max(0, row.totalCount - have);
                int available = Math.min(have, row.totalCount);
                w.write("\"" + row.name + "\"," + row.totalCount + "," + missing + "," + available);
                w.newLine();
            }
        }
    }

    private void loadMergedMaterials() {
        rows.clear();
        Map<Item, List<SourceEntry>> sourceMap = new HashMap<>();
        Map<Item, String> itemNames = new HashMap<>();

        for (MaterialListScreen.LoadedEntry entry : projectEntries) {
            Path filePath = entry.filePath();
            if (filePath == null) continue;
            String listName = entry.name();
            if (MaterialListScreen.isTxtEntry(entry)) listName += "（txt）";
            else if (MaterialListScreen.isJsonEntry(entry)) listName += "（JSON）";

            try {
                List<LitematicReader.MaterialEntry> entries;
                if (MaterialDetailScreen.getImportedMaterials().containsKey(filePath)) {
                    entries = MaterialDetailScreen.getImportedMaterials().get(filePath);
                } else if (filePath.toString().toLowerCase().endsWith(".txt") || filePath.toString().toLowerCase().endsWith(".json") || filePath.toString().toLowerCase().endsWith(".csv")) {
                    MaterialListImporter.ImportResult result = MaterialListImporter.importFromFile(filePath);
                    if (result != null) { entries = result.materials(); MaterialDetailScreen.cacheMaterialList(filePath, entries); }
                    else continue;
                } else {
                    entries = LitematicReader.loadMaterialList(filePath);
                }
                for (LitematicReader.MaterialEntry me : entries) {
                    Item originalItem = me.item();
                    Item item = MaterialDetailScreen.getReplacement(filePath, originalItem);
                    sourceMap.computeIfAbsent(item, k -> new ArrayList<>())
                            .add(new SourceEntry(listName, me.totalCount(), item, originalItem, filePath));
                    itemNames.putIfAbsent(item, item.getName().getString());
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
            Set<Item> subPinned = MaterialDetailScreen.getPinnedForPath(fp);
            if (subPinned != null) currentPinned.addAll(subPinned);
        }

        // 保存展开状态
        Set<Item> savedExpanded = new HashSet<>(expandedItems);
        expandedItems.clear();

        for (var entry : sourceMap.entrySet()) {
            Item item = entry.getKey();
            List<SourceEntry> sources = entry.getValue();
            int total = sources.stream().mapToInt(s -> s.count).sum();
            MergedRow mr = new MergedRow(new ItemStack(item, total), itemNames.get(item), total, item, sources);
            if (savedExpanded.contains(item) && sources.size() > 1) { mr.expanded = true; expandedItems.add(item); }
            rows.add(mr);
        }

        // 只有所有来源都被忽略的才加入 currentIgnored
        for (MergedRow row : rows) {
            if (row.allSourcesIgnored()) currentIgnored.add(row.item);
        }
        // 初始按总计数量从高到低排列
        rows.sort(Comparator.comparingInt((MergedRow r) -> r.totalCount).reversed());
        LitematListMod.LOGGER.info("合并项目材料: {} 种物品, 忽略: {}", rows.size(), currentIgnored.size());
    }

    private void syncIgnoreToSubLists(Item item, boolean ignored) {
        for (MergedRow row : rows) {
            if (row.item == item) {
                for (SourceEntry src : row.sources) {
                    Set<Item> subIgnored = MaterialDetailScreen.getIgnoredForPath(src.filePath);
                    if (subIgnored != null) {
                        if (ignored) subIgnored.add(src.item); else subIgnored.remove(src.item);
                    }
                    // 同时同步到原材料忽略列表
                    String pathKey = src.filePath.toString();
                    Set<String> rawIgnored = MaterialDetailScreen.rawMaterialIgnored.computeIfAbsent(pathKey, k -> new HashSet<>());
                    String itemId = Registries.ITEM.getId(src.item).toString();
                    if (ignored) rawIgnored.add(itemId); else rawIgnored.remove(itemId);
                }
                break;
            }
        }
    }

    private void syncPinToSubLists(Item item, boolean pinned) {
        for (MergedRow row : rows) {
            if (row.item == item) {
                for (SourceEntry src : row.sources) {
                    Set<Item> subPinned = MaterialDetailScreen.getPinnedForPath(src.filePath);
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
            if (rowY + row.getRowHeight() > maxRenderY) break; // 底部按钮区域，不创建按钮（与 render 一致）

            if (row.sources.size() > 1) {
                // 多来源：展开时显示替换/取消替换和忽略按钮
                if (row.expanded) {
                    int subIdx = 0;
                    for (int j = 0; j < row.sources.size(); j++) {
                        SourceEntry src = row.sources.get(j);
                        boolean isIgnored = row.isSourceIgnored(src);
                        if (isIgnored) continue; // 已忽略的不显示按钮
                        int subY = rowY + ROW_HEIGHT + subIdx * ROW_HEIGHT;
                        if (subY >= maxRenderY) break; // 底部按钮区域
                        int ignoreX = this.width - BTN_RIGHT_MARGIN - BTN_IGNORE_W - 30; // 加宽
                        int replaceX = ignoreX - BTN_REPLACE_W - BTN_GAP;
                        final MergedRow r = row;
                        final SourceEntry s = src;

                        // 替换/取消替换按钮
                        if (src.item != src.originalItem) {
                            // 已替换：显示取消替换按钮
                            ButtonGeneric cancelBtn = new ButtonGeneric(replaceX, subY + 1, BTN_CANCEL_W, 20, I18n.tr("litematlist.button.cancel_replace"));
                            cancelBtn.setRenderDefaultBackground(true);
                            this.addButton(cancelBtn, (IButtonActionListener) (b, mb) -> restoreSourceReplacement(r, s));
                        } else {
                            // 未替换：显示替换按钮
                            ButtonGeneric replaceBtn = new ButtonGeneric(replaceX, subY + 1, BTN_REPLACE_W, 20, I18n.tr("litematlist.button.replace"));
                            replaceBtn.setRenderDefaultBackground(true);
                            this.addButton(replaceBtn, (IButtonActionListener) (b, mb) -> replaceSource(r, s));
                        }

                        // 忽略按钮
                        ButtonGeneric ignoreBtn = new ButtonGeneric(ignoreX, subY + 1, BTN_IGNORE_W + 30, 20, I18n.tr("litematlist.button.ignore"));
                        ignoreBtn.setRenderDefaultBackground(true);
                        this.addButton(ignoreBtn, (IButtonActionListener) (b, mb) -> ignoreSource(r, s));
                        subIdx++;
                    }
                }
            } else {
                // 单来源：显示替换/取消替换和忽略按钮
                SourceEntry src = row.sources.get(0);
                int ignoreX = this.width - BTN_RIGHT_MARGIN - BTN_IGNORE_W;
                int replaceX = ignoreX - BTN_REPLACE_W - BTN_GAP;
                final MergedRow r = row;
                final SourceEntry s = src;

                // 替换/取消替换按钮
                if (src.item != src.originalItem) {
                    ButtonGeneric cancelBtn = new ButtonGeneric(replaceX, rowY + 1, BTN_CANCEL_W, 20, I18n.tr("litematlist.button.cancel_replace"));
                    cancelBtn.setRenderDefaultBackground(true);
                    this.addButton(cancelBtn, (IButtonActionListener) (b, mb) -> restoreSourceReplacement(r, s));
                } else {
                    ButtonGeneric replaceBtn = new ButtonGeneric(replaceX, rowY + 1, BTN_REPLACE_W, 20, I18n.tr("litematlist.button.replace"));
                    replaceBtn.setRenderDefaultBackground(true);
                    this.addButton(replaceBtn, (IButtonActionListener) (b, mb) -> replaceSource(r, s));
                }

                // 忽略按钮（置顶材料不显示）
                if (!(allowPin && currentPinned.contains(row.originalItem))) {
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
            if (allowPin && currentPinned.contains(row.originalItem)) return;
            currentIgnored.add(row.item);
            syncIgnoreToSubLists(row.item, true);
            ignoreJustClicked = true;
            MaterialDetailScreen.savePersistence();
            MaterialListHudRenderer.refreshHud(); // 忽略后更新HUD
        }
    }

    private void ignoreSource(MergedRow parent, SourceEntry source) {
        Set<Item> subIgnored = MaterialDetailScreen.getIgnoredForPath(source.filePath);
        if (subIgnored != null) subIgnored.add(source.item);
        MaterialDetailScreen.savePersistence();
        ignoreJustClicked = true;
        loaded = false;
        MaterialListHudRenderer.refreshHud(); // 忽略后更新HUD
        initGui();
    }

    private void unignoreSource(SourceEntry source) {
        Set<Item> subIgnored = MaterialDetailScreen.getIgnoredForPath(source.filePath);
        if (subIgnored != null) subIgnored.remove(source.item);
        MaterialDetailScreen.savePersistence();
        loaded = false;
        MaterialListHudRenderer.refreshHud(); // 取消忽略后更新HUD
        initGui();
    }

    /** 替换来源物品：打开 BlockPickerScreen，选择后将替换写入持久化并重新合并 */
    private void replaceSource(MergedRow parent, SourceEntry source) {
        MinecraftClient.getInstance().setScreen(
                new BlockPickerScreen(this, newItem -> {
                    MaterialDetailScreen.setReplacement(source.filePath, source.originalItem, newItem);
                    MaterialDetailScreen.savePersistence();
                    loaded = false;
                    MaterialListHudRenderer.refreshHud();
                    initGui();
                }));
    }

    /** 取消来源物品的替换，恢复为原始物品 */
    private void restoreSourceReplacement(MergedRow parent, SourceEntry source) {
        MaterialDetailScreen.removeReplacement(source.filePath, source.originalItem);
        MaterialDetailScreen.savePersistence();
        loaded = false;
        MaterialListHudRenderer.refreshHud();
        initGui();
    }

    /** 搜索文本变化统一回调：任意输入路径（打字/粘贴/IME 提交）都会刷新过滤结果 */
    private void onSearchChanged(String text) {
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
                    if (row.totalCount - getPlayerItemCount(row.item) <= 0) continue;
                }
                // 搜索过滤（中文名/拼音全拼/拼音首字母/英文ID）
                if (searchActive) {
                    String query = searchField != null ? searchField.getText() : searchText;
                    if (!query.isEmpty()) {
                        String itemId = Registries.ITEM.getId(row.item).toString();
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
                if (currentPinned.contains(row.originalItem)) pinned.add(row);
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
            maxNameWidth = Math.max(maxNameWidth, this.textRenderer.getWidth(row.name));
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
                if (currentPinned.contains(row.originalItem)) pinned.add(row);
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
            case HAVE_DESC -> Comparator.comparingInt((MergedRow r) -> getPlayerItemCount(r.item)).reversed();
            case HAVE_ASC -> Comparator.comparingInt(r -> getPlayerItemCount(r.item));
            case MISS_DESC -> Comparator.comparingInt((MergedRow r) -> Math.max(0, r.totalCount - getPlayerItemCount(r.item))).reversed();
            case MISS_ASC -> Comparator.comparingInt(r -> Math.max(0, r.totalCount - getPlayerItemCount(r.item)));
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
        MaterialDetailScreen.persistedProjectSortMode = sortMode.name();
        MaterialDetailScreen.savePersistence();
        if (newMode != SortMode.NONE) {
            // 一次性排序：将 rows 按当前模式排序
            Comparator<MergedRow> cmp = sortComparator(newMode);
            if (cmp != null) rows.sort(cmp);
        }
        initGui();
    }

    private Comparator<MergedRow> sortComparator(SortMode mode) {
        return switch (mode) {
            case TOTAL_DESC -> Comparator.comparingInt((MergedRow r) -> r.totalCount).reversed();
            case TOTAL_ASC -> Comparator.comparingInt(r -> r.totalCount);
            case HAVE_DESC -> Comparator.comparingInt((MergedRow r) -> getPlayerItemCount(r.item)).reversed();
            case HAVE_ASC -> Comparator.comparingInt(r -> getPlayerItemCount(r.item));
            case MISS_DESC -> Comparator.comparingInt((MergedRow r) -> Math.max(0, r.totalCount - getPlayerItemCount(r.item))).reversed();
            case MISS_ASC -> Comparator.comparingInt(r -> Math.max(0, r.totalCount - getPlayerItemCount(r.item)));
            default -> null;
        };
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(drawContext, mouseX, mouseY, partialTicks);

        if (rows.isEmpty()) {
            drawContext.drawCenteredTextWithShadow(this.textRenderer, I18n.tr("litematlist.error.no_data"),
                    this.width / 2, this.height / 2 - 10, 0xFFFFFFFF);
            super.render(drawContext, mouseX, mouseY, partialTicks);
            return;
        }

        List<MergedRow> displayRows = getSortedRows();
        int listRight = this.width - 8;
        int maxRenderY = this.height - 45; // 底部按钮区域上方，不渲染到此行以下
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
        drawContext.fill(8, LIST_TOP - 2, listRight, listBottom + 2, 0x40000000);

        hoveredRow = -1;
        hoveredColumn = -1;
        cumulativeY = LIST_TOP;
        count = 0;

        for (int i = scrollOffset; i < displayRows.size() && count < visibleRows; i++) {
            MergedRow row = displayRows.get(i);
            int rowY = cumulativeY;
            int rowH = row.getRowHeight();
            if (rowY + rowH > maxRenderY) break;

            // 主行高亮（仅覆盖第一行，不含子项）
            if (mouseX >= 10 && mouseX <= listRight && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                hoveredRow = i;

                int totalW = this.textRenderer.getWidth(String.valueOf(row.totalCount));
                int haveW = this.textRenderer.getWidth(String.valueOf(getPlayerItemCount(row.item)));
                int missW = this.textRenderer.getWidth(String.valueOf(Math.max(0, row.totalCount - getPlayerItemCount(row.item))));
                if (mouseX >= colTotalX - 10 && mouseX <= colTotalX + totalW + 10) hoveredColumn = 0;
                else if (mouseX >= colHaveX - 10 && mouseX <= colHaveX + haveW + 10) hoveredColumn = 1;
                else if (mouseX >= colMissX - 10 && mouseX <= colMissX + missW + 10) hoveredColumn = 2;
            } else if ((i - scrollOffset) % 2 == 0) {
                drawContext.fill(10, rowY, listRight, rowY + ROW_HEIGHT, 0x10FFFFFF);
            }

            // 子项独立高亮检测（不在这里绘制，在 super.render 之后绘制以覆盖按钮区域）
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

        drawContext.fill(8, HEADER_Y, listRight, HEADER_Y + HEADER_HEIGHT, 0x60000000);
        super.render(drawContext, mouseX, mouseY, partialTicks);

        // 隐藏/标记按钮文字
        if (!hideBtnLabel.isEmpty()) {
            int colonIdx = hideBtnLabel.indexOf("：");
            if (colonIdx < 0) colonIdx = hideBtnLabel.indexOf(":");
            if (colonIdx > 0) {
                String prefix = hideBtnLabel.substring(0, colonIdx + 1);
                String status = hideBtnLabel.substring(colonIdx + 1);
                drawContext.drawTextWithShadow(this.textRenderer, prefix, hideBtnX + 4, hideBtnY + 5, 0xFFFFFFFF);
                drawContext.drawTextWithShadow(this.textRenderer, status, hideBtnX + 4 + this.textRenderer.getWidth(prefix), hideBtnY + 5,
                        hideNoMissing ? 0xFF55FF55 : 0xFFFF5555);
            }
        }
        if (!pinBtnLabel.isEmpty()) {
            int colonIdx = pinBtnLabel.indexOf("：");
            if (colonIdx < 0) colonIdx = pinBtnLabel.indexOf(":");
            if (colonIdx > 0) {
                String prefix = pinBtnLabel.substring(0, colonIdx + 1);
                String status = pinBtnLabel.substring(colonIdx + 1);
                drawContext.drawTextWithShadow(this.textRenderer, prefix, pinBtnX + 4, pinBtnY + 5, 0xFFFFFFFF);
                drawContext.drawTextWithShadow(this.textRenderer, status, pinBtnX + 4 + this.textRenderer.getWidth(prefix), pinBtnY + 5,
                        allowPin ? 0xFF55FF55 : 0xFFFF5555);
            }
        }

        // 在按钮上方绘制悬停高亮（覆盖整行包括按钮区域）
        if (hoveredRow >= 0 && hoveredRow < displayRows.size()) {
            MergedRow hovered = displayRows.get(hoveredRow);
            int hoverY = LIST_TOP;
            for (int i = scrollOffset; i < hoveredRow && i < displayRows.size(); i++) {
                hoverY += displayRows.get(i).getRowHeight();
            }
            int hoverH = ROW_HEIGHT; // 主行高
            // 如果鼠标悬停在子项上，计算子项位置
            if (hovered.expanded) {
                int cumulativeY2 = LIST_TOP;
                for (int i = scrollOffset; i < hoveredRow && i < displayRows.size(); i++) {
                    cumulativeY2 += displayRows.get(i).getRowHeight();
                }
                int mainRowY = cumulativeY2;
                int subIdx = 0;
                for (int j = 0; j < hovered.sources.size(); j++) {
                    if (hovered.isSourceIgnored(hovered.sources.get(j))) continue;
                    int subY = mainRowY + ROW_HEIGHT + subIdx * ROW_HEIGHT;
                    if (mouseY >= subY && mouseY < subY + ROW_HEIGHT) {
                        hoverY = subY;
                        hoverH = ROW_HEIGHT;
                        break;
                    }
                    subIdx++;
                }
            }
            // 使用 this.width 覆盖整个屏幕宽度，确保按钮区域也被高亮
            drawContext.fill(10, hoverY, this.width - 8, hoverY + hoverH, 0x40FFFFFF);
        }

        // 渲染每一行
        cumulativeY = LIST_TOP;
        count = 0;
        int maxIdx = Math.min(displayRows.size(), scrollOffset + visibleRows);

        for (int i = scrollOffset; i < maxIdx; i++) {
            MergedRow row = displayRows.get(i);
            int rowY = cumulativeY;
            int rowH = row.getRowHeight();
            if (rowY + rowH > maxRenderY) break; // 不渲染到底部按钮区域
            int iconX = 15, nameX = 35;

            if (allowPin) {
                boolean isPinned = currentPinned.contains(row.originalItem);
                boolean pinHovered = mouseX >= 15 && mouseX <= 31 && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
                Identifier pinTexture = isPinned ? (pinHovered ? PIN_HOVERED_FAVORITE : PIN_FAVORITE)
                        : (pinHovered ? PIN_HOVERED_EMPTY : PIN_EMPTY);
                drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, pinTexture, 15, rowY + 2, 0.0f, 0.0f, 16, 16, 16, 16);
                iconX = 33; nameX = 53;
            }

            drawContext.drawItem(row.itemStack, iconX, rowY + 2);
            drawContext.drawTextWithShadow(this.textRenderer, row.name, nameX, rowY + 5, 0xFFFFFFFF);
            drawContext.drawTextWithShadow(this.textRenderer, String.valueOf(row.totalCount), colTotalX, rowY + 5, 0xFFFFFFFF);
            int haveCount = getPlayerItemCount(row.item);
            drawContext.drawTextWithShadow(this.textRenderer, String.valueOf(haveCount), colHaveX, rowY + 5, 0xFFFFFFFF);
            int missCount = Math.max(0, row.totalCount - haveCount);
            int missColor = missCount == 0 ? 0xFF55FF55 : (haveCount >= missCount ? 0xFFFFAA00 : 0xFFFF5555);
            drawContext.drawTextWithShadow(this.textRenderer, String.valueOf(missCount), colMissX, rowY + 5, missColor);

            // 展开/折叠图标（居中于移除按钮中轴, 无按钮背景）
            if (row.sources.size() > 1) {
                int removeCenterX = this.width - 12 - 25; // 移除按钮宽50, 中轴
                int expandX = removeCenterX - EXPAND_ICON_SIZE / 2;
                boolean expHovered = mouseX >= expandX && mouseX <= expandX + EXPAND_ICON_SIZE
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
                Identifier expIcon;
                if (row.expanded) expIcon = expHovered ? COLLAPSE_HOVERED : COLLAPSE_NORMAL;
                else expIcon = expHovered ? EXPAND_HOVERED : EXPAND_NORMAL;
                drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, expIcon, expandX, rowY + 2, 0.0f, 0.0f, EXPAND_ICON_SIZE, EXPAND_ICON_SIZE, EXPAND_ICON_SIZE, EXPAND_ICON_SIZE);
            }

            // 展开的子项（仅显示未忽略的）
            if (row.expanded) {
                int subNameX = nameX + 10;
                int subIdx = 0;
                for (int j = 0; j < row.sources.size(); j++) {
                    SourceEntry src = row.sources.get(j);
                    if (row.isSourceIgnored(src)) continue; // 已忽略的不显示
                    int subY = rowY + ROW_HEIGHT + subIdx * ROW_HEIGHT;
                    drawContext.fill(10, subY, listRight, subY + ROW_HEIGHT, (subIdx % 2 == 0) ? 0x18FFFFFF : 0x08FFFFFF);
                    drawContext.drawItem(new ItemStack(src.item), subNameX, subY + 2);
                    String subText = " - " + src.item.getName().getString() + " " + src.count + "  (" + src.listName + ")";
                    int textEndX = subNameX + 18 + this.textRenderer.getWidth(subText);
                    // 如果被替换，显示原物品图标和名称
                    if (src.item != src.originalItem) {
                        int origIconX = textEndX + 4;
                        drawContext.drawItem(new ItemStack(src.originalItem), origIconX, subY + 2);
                        String origText = " " + src.originalItem.getName().getString() + "（原有）";
                        drawContext.drawTextWithShadow(this.textRenderer, origText, origIconX + 18, subY + 5, 0xCCAAAAAA);
                    }
                    drawContext.drawTextWithShadow(this.textRenderer, subText, subNameX + 18, subY + 5, 0xCCCCCCCC);
                    // 子项数量级 tooltip
                    if (mouseX >= subNameX && mouseX <= listRight && mouseY >= subY && mouseY < subY + ROW_HEIGHT) {
                        drawContext.drawTooltip(this.textRenderer, Text.literal(formatCountTooltip(src.count)), mouseX, mouseY);
                    }
                    subIdx++;
                }
            }

            cumulativeY += row.getRowHeight();
            count++;
            if (count >= visibleRows) break;
        }

        int headerNameX = allowPin ? 53 : 35;
        // 搜索图标
        int searchIconX = headerNameX - 20;
        drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, SEARCH_ICON, searchIconX, HEADER_Y + 1, 0.0f, 0.0f, 14, 14, 14, 14);
        // 搜索框打开时不绘制"物品"表头文字（搜索框覆盖该区域，避免与输入文字重叠冲突）
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

        if (hoveredRow >= 0 && hoveredColumn >= 0 && hoveredRow < displayRows.size()) {
            MergedRow hovered = displayRows.get(hoveredRow);
            int haveCount = getPlayerItemCount(hovered.item);
            int val = switch (hoveredColumn) {
                case 0 -> hovered.totalCount;
                case 1 -> haveCount;
                case 2 -> Math.max(0, hovered.totalCount - haveCount);
                default -> hovered.totalCount;
            };
            drawContext.drawTooltip(this.textRenderer, Text.literal(formatCountTooltip(val)), mouseX, mouseY);
        }

        // 计算总内容高度（像素）
            int totalContentHeight = 0;
            for (int i = 0; i < displayRows.size(); i++) {
                totalContentHeight += displayRows.get(i).getRowHeight();
            }
            int trackHeight = maxRenderY - LIST_TOP;
            // 只有总内容超出可见区域时才显示滚动条
            if (totalContentHeight > trackHeight) {
                int scrollInfoY = listBottom + 5;
                String scrollInfo = (scrollOffset + 1) + "-" + Math.min(scrollOffset + visibleRows, displayRows.size()) + " / " + displayRows.size();
                drawContext.drawCenteredTextWithShadow(this.textRenderer, scrollInfo, this.width / 2, scrollInfoY, 0xFFFFFFFF);

                // 滚动条（像素级计算，适配可变行高）
                int scrollbarX = this.width - 6;
                int scrollbarWidth = 3;
                // 轨道
                drawContext.fill(scrollbarX, LIST_TOP, scrollbarX + scrollbarWidth, listBottom, 0x30FFFFFF);

                // 计算 scrollOffset 对应的像素偏移
                int scrolledPixels = 0;
                for (int i = 0; i < scrollOffset && i < displayRows.size(); i++) {
                    scrolledPixels += displayRows.get(i).getRowHeight();
                }
                int maxScrollPixels = Math.max(1, totalContentHeight - trackHeight);
                float ratio = (float) trackHeight / totalContentHeight;
                int thumbHeight = Math.max(8, (int)(trackHeight * ratio));
                int thumbY = LIST_TOP + (int)((trackHeight - thumbHeight) * (float) scrolledPixels / maxScrollPixels);
                drawContext.fill(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, 0x80FFFFFF);
            }
        }

    @Override
    public boolean onMouseClicked(int mouseX, int mouseY, int button) {
        if (super.onMouseClicked(mouseX, mouseY, button)) return true;
        if (ignoreJustClicked) {
            ignoreJustClicked = false;
            return true;
        }
        
        
        
        if (button == 0) {
            // 搜索框本体点击：优先聚焦（否则点击被表头逻辑吞掉，输入框永远无法获得焦点）
            int searchBoxX = (allowPin ? 53 : 35) - 20 + 18;
            if (searchActive && searchField != null
                    && mouseX >= searchBoxX - 2 && mouseX <= searchBoxX + 82
                    && mouseY >= HEADER_Y - 1 && mouseY <= HEADER_Y + 15) {
                searchField.setFocused(true);
                return true;
            }
            // 搜索图标点击
            int searchIconX = (allowPin ? 53 : 35) - 20;
            if (mouseX >= searchIconX && mouseX <= searchIconX + 16
                    && mouseY >= HEADER_Y && mouseY <= HEADER_Y + HEADER_HEIGHT) {
                searchActive = !searchActive;
                if (searchActive && searchField != null) searchField.setFocused(true);
                if (!searchActive) { searchText = ""; if (searchField != null) searchField.setText(""); }
                initGui();
                if (searchActive && searchField != null) searchField.setFocused(true);
                return true;
            }

            // 【已忽略】按钮手动点击
            if (mouseX >= ignoredBtnX && mouseX <= ignoredBtnX + ignoredBtnW
                    && mouseY >= ignoredBtnY && mouseY <= ignoredBtnY + ignoredBtnH) {
                openIgnoredScreen();
                return true;
            }

            if (mouseY >= HEADER_Y && mouseY <= HEADER_Y + HEADER_HEIGHT) { handleHeaderClick(mouseX, mouseY); return true; }

            // 展开/折叠图标点击
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
                    if (row.expanded) expandedItems.add(row.item);
                    else expandedItems.remove(row.item);
                    initGui();
                    return true;
                }
                cumulativeY += row.getRowHeight();
                count++;
            }

            // 置顶点击
            if (allowPin) {
                displayRows = getSortedRows();
                int pinRow = getRowAtY(mouseY, displayRows);
                if (pinRow >= 0 && mouseX >= 15 && mouseX <= 31) {
                    MergedRow row = displayRows.get(pinRow);
                    if (currentPinned.contains(row.originalItem)) { currentPinned.remove(row.originalItem); syncPinToSubLists(row.item, false); }
                    else { currentPinned.add(row.originalItem); syncPinToSubLists(row.item, true); }
                    MaterialDetailScreen.savePersistence();
                    initGui();
                    return true;
                }
            }
            // 拖拽重排
            if (Configs.Generic.ALLOW_MANUAL_REORDER.getBooleanValue()) {
                List<MergedRow> dragDisplay = getSortedRows();
                int row = getRowAtY(mouseY, dragDisplay);
                if (row >= 0 && row < dragDisplay.size() && mouseX >= 10 && mouseX < this.width - 80) {
                    // 检查是否点击在子项上
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
                                    dragStartY = mouseY;
                                    return true;
                                }
                                break;
                            }
                            subY += ROW_HEIGHT;
                            subIdx++;
                        }
                    } else {
                        draggedRow = row;
                        dragStartY = mouseY;
                        isDragging = false;
                        return true;
                    }
                }
            }
        }

        // 滚动条点击（像素级）
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
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        
        

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
                    // 计算当前鼠标所在的可见子项索引
                    int targetSubIdx = 0;
                    int subY = getRowY(draggedSubParentIdx) + ROW_HEIGHT;
                    for (int j = 0; j < parent.sources.size(); j++) {
                        if (parent.isSourceIgnored(parent.sources.get(j))) continue;
                        if (mouseY >= subY && mouseY < subY + ROW_HEIGHT) {
                            if (targetSubIdx != draggedSubIndex) {
                                // 将可见索引映射到实际 sources 列表索引
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
            int currentRow = getRowAtY((int)mouseY, display);
            if (currentRow >= 0 && currentRow < display.size() && currentRow != draggedRow && Math.abs(mouseY - dragStartY) > 5) {
                // 不允许跨置顶边界拖拽
                if (allowPin) {
                    MergedRow dragged = display.get(draggedRow);
                    MergedRow target = display.get(currentRow);
                    boolean draggedPinned = currentPinned.contains(dragged.originalItem);
                    boolean targetPinned = currentPinned.contains(target.originalItem);
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
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
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
        return super.mouseReleased(mouseX, mouseY, button);
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
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseY >= LIST_TOP && mouseY <= this.height - 45) {
            scrollOffset = Math.max(0, scrollOffset - (int) verticalAmount);
            scrollOffset = Math.min(scrollOffset, Math.max(0, getSortedRows().size() - visibleRows));
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
                initGui();
                return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }
}




