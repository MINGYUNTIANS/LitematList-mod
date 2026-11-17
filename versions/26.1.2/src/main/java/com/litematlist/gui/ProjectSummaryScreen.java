package com.litematlist.gui;

import com.litematlist.I18n;
import com.litematlist.LitematicReader;
import com.litematlist.LitematListMod;
import com.litematlist.MaterialListExporter;
import com.litematlist.MaterialListImporter;
import com.litematlist.TextWidthUtils;
import com.litematlist.PinyinSearch;
import com.litematlist.config.Configs;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.util.InventoryUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
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
    private final Set<String> currentIgnored = new HashSet<>();
    private final Set<String> expandedItems = new HashSet<>(); // 记录展开的物品（稳定键）

    // ---- 隐藏不缺材料 ----
    private boolean hideNoMissing = MaterialDataStore.persistedHideNoMissing;
    private int hideBtnX, hideBtnY, hideBtnW;
    private String hideBtnLabel = "";

    // ---- 标记材料 ----
    private static final Identifier PIN_EMPTY = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/pin/empty.png");
    private static final Identifier PIN_FAVORITE = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/pin/favorite.png");
    private static final Identifier PIN_HOVERED_EMPTY = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/pin/hovered_empty.png");
    private static final Identifier PIN_HOVERED_FAVORITE = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/pin/hovered_favorite.png");
    private boolean allowPin = MaterialDataStore.persistedAllowPin;
    private int pinBtnX, pinBtnY, pinBtnW;
    private String pinBtnLabel = "";
    private final Set<String> currentPinned = new HashSet<>();

    // ---- 展开/折叠图标 ----
    private static final Identifier EXPAND_NORMAL = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/expand/expand_normal.png");
    private static final Identifier EXPAND_HOVERED = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/expand/expand_hovered.png");
    private static final Identifier COLLAPSE_NORMAL = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/expand/collapse_normal.png");
    private static final Identifier COLLAPSE_HOVERED = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/expand/collapse_hovered.png");
    private static final Identifier SEARCH_ICON = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/search.png");

    private int colTotalX, colHaveX, colMissX;
    private int ignoredBtnX, ignoredBtnY, ignoredBtnW, ignoredBtnH; // 【已忽略】按钮位置

    // ---- 搜索 ----
    private boolean searchActive = false;
    private String searchText = "";
    private EditBox searchField;

    private static final int BTN_IGNORE_W = 52;
    private static final int BTN_REPLACE_W = 52;
    private static final int BTN_CANCEL_W = 52;
    private static final int BTN_RIGHT_MARGIN = 12;
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
        if (MaterialDataStore.persistedProjectSortMode != null && !MaterialDataStore.persistedProjectSortMode.equals("NONE")) {
            try {
                sortMode = SortMode.valueOf(MaterialDataStore.persistedProjectSortMode);
            } catch (IllegalArgumentException e) {
                sortMode = SortMode.NONE;
            }
        }
    }

    // ---- 内部数据类 ----
    private static class SourceEntry {
        final String listName;
        final int count;
        final Item item;
        final Item originalItem; // 替换前的原始物品，用于精确匹配忽略集
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
        final Component displayText;  // 精确物品的带样式显示文本（普通为 null）
        int totalCount;
        final Item item;
        final boolean exact;     // 是否为精确物品行
        final ItemStack exactStack; // 精确物品堆叠（普通为 null）
        final List<SourceEntry> sources;
        boolean expanded;
        int category;    // 材料分类（建筑材料/容器数据/实体数据/实体容器数据），取各来源最早分类

        MergedRow(ItemStack itemStack, String name, String key, Component displayText, int totalCount,
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
        // 【刷新列表】按钮
        ButtonGeneric refreshBtn = new ButtonGeneric(20, buttonY, 80, 20, I18n.tr("litematlist.button.refresh_list"));
        refreshBtn.setRenderDefaultBackground(true);
        this.addButton(refreshBtn, (IButtonActionListener) (btn, mb) -> { loaded = false; initGui(); });

        ButtonGeneric backButton = new ButtonGeneric(this.width / 2 - 60, buttonY, 120, 20, I18n.tr("litematlist.button.back"));
        backButton.setRenderDefaultBackground(true);
        this.addButton(backButton, (IButtonActionListener) (btn, mb) -> Minecraft.getInstance().setScreenAndShow(parent));

        boolean isEmpty = projectEntries.isEmpty();

        int ignoredCount = 0;
        for (MergedRow row : rows) {
            if (row.sources.stream().anyMatch(row::isSourceIgnored)) ignoredCount++;
        }
        String ignoredLabel = ignoredCount > 0 ? I18n.tr("litematlist.button.ignored_count", ignoredCount) : I18n.tr("litematlist.button.ignored");
        int ignoredBtnW = Math.max(60, this.font.width(ignoredLabel) + 16);

        String hideLabel = hideNoMissing ? I18n.tr("litematlist.button.hide_no_missing_on") : I18n.tr("litematlist.button.hide_no_missing_off");
        int hideBtnW = Math.max(60, this.font.width(hideLabel) + 16);
        this.hideBtnX = 20; this.hideBtnY = 20; this.hideBtnW = hideBtnW; this.hideBtnLabel = hideLabel;
        ButtonGeneric hideBtn = new ButtonGeneric(hideBtnX, hideBtnY, hideBtnW, 20, "");
        hideBtn.setRenderDefaultBackground(true);
        if (isEmpty) hideBtn.setEnabled(false);
        this.addButton(hideBtn, (IButtonActionListener) (b, mb) -> { hideNoMissing = !hideNoMissing; MaterialDataStore.persistedHideNoMissing = hideNoMissing; MaterialDataStore.savePersistence(); initGui(); });

        String pinLabel = allowPin ? I18n.tr("litematlist.button.allow_pin_on") : I18n.tr("litematlist.button.allow_pin_off");
        int pinBtnW = Math.max(60, this.font.width(pinLabel) + 16);
        this.pinBtnX = hideBtnX + hideBtnW + 4; this.pinBtnY = 20; this.pinBtnW = pinBtnW; this.pinBtnLabel = pinLabel;
        ButtonGeneric pinBtn = new ButtonGeneric(pinBtnX, pinBtnY, pinBtnW, 20, "");
        pinBtn.setRenderDefaultBackground(true);
        if (isEmpty) pinBtn.setEnabled(false);
        this.addButton(pinBtn, (IButtonActionListener) (b, mb) -> { allowPin = !allowPin; MaterialDataStore.persistedAllowPin = allowPin; MaterialDataStore.savePersistence(); initGui(); });

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

        // 搜索框（与 render 中绘制的搜索框位置一致：图标右侧 18px）
        int searchIconX = (allowPin ? 53 : 35) - 20;
        if (searchField == null) {
            this.searchField = new EditBox(this.font, searchIconX + 18, HEADER_Y, 80, 14, Component.empty());
            this.searchField.setMaxLength(50);
            this.searchField.setBordered(false);
            this.searchField.setValue(searchText);
            this.searchField.setResponder(this::onSearchChanged);
            this.addRenderableWidget(this.searchField);
        } else {
            this.searchField.setX(searchIconX + 18);
            this.searchField.setY(HEADER_Y);
            if (!this.searchField.getValue().equals(searchText)) {
                this.searchField.setValue(searchText); // 相同文本不调用，防止触发 responder → initGui 无限递归
            }
            this.addRenderableWidget(this.searchField);
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
        Minecraft.getInstance().setScreenAndShow(
                new ProjectIgnoredItemsScreen(this, projectName, allIgnoredKeys, ignoredTotals, projectEntries));
    }

    private void exportMaterials(String format) {
        try {
            Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("litematlist").resolve("project");
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
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                String fmt = format.toUpperCase();
                Component link = Component.literal(fileName)
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent.OpenFile(filePath.toString()))
                                .withUnderlined(true)
                                .withColor(0x55FFFF));
                Component msg = Component.literal("§a材料列表已导出为" + fmt + ": ").append(link);
                client.player.sendSystemMessage(msg);
            }
        } catch (Exception e) {
            LitematListMod.LOGGER.error("导出失败", e);
        }
    }

    /**
     * 绘制悬停提示：内容过长触及屏幕边界时自动换行，避免超出屏幕。
     * 按鼠标位置取较宽一侧的可用宽度，逐字符折行后以多行提示绘制。
     */
    private void drawWrappedTooltip(GuiContext ctx, String text, int mouseX, int mouseY) {
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
        ctx.renderTooltip(this.font, lines, mouseX, mouseY);
    }

    private void loadMergedMaterials() {
        rows.clear();
        Map<String, List<SourceEntry>> sourceMap = new LinkedHashMap<>();
        Map<String, String> itemNames = new HashMap<>();      // 普通键 → 物品显示名
        Map<String, Item> rowItems = new HashMap<>();         // 稳定键 → 基础物品
        Map<String, ItemStack> exactStacks = new HashMap<>(); // 精确键 → 精确堆叠
        Map<String, Component> exactTexts = new HashMap<>();  // 精确键 → 带样式显示文本
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
                        exactTexts.putIfAbsent(key, exactStack.getStyledHoverName());
                        continue;
                    }
                    Item originalItem = me.item();
                    Item item = MaterialDataStore.getReplacement(filePath, originalItem);
                    String key = BuiltInRegistries.ITEM.getKey(item).toString();
                    keyCategory.merge(key, me.category(), Math::min);
                    sourceMap.computeIfAbsent(key, k -> new ArrayList<>())
                            .add(new SourceEntry(listName, me.totalCount(), item, originalItem, filePath, key, false, null));
                    itemNames.putIfAbsent(key, item.getName(new ItemStack(item)).getString());
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
            Set<Item> subPinned = MaterialDataStore.getPinnedForPath(fp);
            if (subPinned != null) {
                for (Item it : subPinned) {
                    Identifier id = BuiltInRegistries.ITEM.getKey(it);
                    if (id != null) currentPinned.add(id.toString());
                }
            }
            currentPinned.addAll(MaterialDataStore.getExactPinnedForPath(fp));
        }

        // 保存展开状态
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
                ItemStack icon = exactStack != null ? exactStack.copyWithCount(total) : new ItemStack(baseItem, total);
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

        // 只有所有来源都被忽略的才加入 currentIgnored
        for (MergedRow row : rows) {
            if (row.allSourcesIgnored()) currentIgnored.add(row.key);
        }
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
                    // 同时同步到原材料忽略列表
                    String pathKey = src.filePath.toString();
                    Set<String> rawIgnored = MaterialDataStore.rawMaterialIgnored.computeIfAbsent(pathKey, k -> new HashSet<>());
                    String itemId = BuiltInRegistries.ITEM.getKey(src.item).toString();
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
                        int replaceX = ignoreX - BTN_REPLACE_W - 4;
                        final MergedRow r = row;
                        final SourceEntry s = src;

                        // 替换/取消替换按钮（精确物品不支持按物品替换，隐藏）
                        if (!src.exact) {
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
                int replaceX = ignoreX - BTN_REPLACE_W - 4;
                final MergedRow r = row;
                final SourceEntry s = src;

                // 替换/取消替换按钮（精确物品不支持按物品替换，隐藏）
                if (!src.exact) {
                    if (src.item != src.originalItem) {
                        ButtonGeneric cancelBtn = new ButtonGeneric(replaceX, rowY + 1, BTN_CANCEL_W, 20, I18n.tr("litematlist.button.cancel_replace"));
                        cancelBtn.setRenderDefaultBackground(true);
                        this.addButton(cancelBtn, (IButtonActionListener) (b, mb) -> restoreSourceReplacement(r, s));
                    } else {
                        ButtonGeneric replaceBtn = new ButtonGeneric(replaceX, rowY + 1, BTN_REPLACE_W, 20, I18n.tr("litematlist.button.replace"));
                        replaceBtn.setRenderDefaultBackground(true);
                        this.addButton(replaceBtn, (IButtonActionListener) (b, mb) -> replaceSource(r, s));
                    }
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
            MaterialListHudRenderer.refreshHud(); // 忽略后更新HUD
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
        MaterialListHudRenderer.refreshHud(); // 忽略后更新HUD
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
        MaterialListHudRenderer.refreshHud(); // 取消忽略后更新HUD
        initGui();
    }

    /** 替换来源物品：打开 BlockPickerScreen，选择后将替换写入持久化并重新合并 */
    private void replaceSource(MergedRow parent, SourceEntry source) {
        Minecraft.getInstance().setScreenAndShow(
                new BlockPickerScreen(this, newItem -> {
                    MaterialDataStore.setReplacement(source.filePath, source.originalItem, newItem);
                    MaterialDataStore.savePersistence();
                    loaded = false;
                    MaterialListHudRenderer.refreshHud();
                    initGui();
                }));
    }

    /** 取消来源物品的替换，恢复为原始物品 */
    private void restoreSourceReplacement(MergedRow parent, SourceEntry source) {
        MaterialDataStore.removeReplacement(source.filePath, source.originalItem);
        MaterialDataStore.savePersistence();
        loaded = false;
        MaterialListHudRenderer.refreshHud();
        initGui();
    }

    /** 搜索文本变化统一回调：任意输入路径（打字/粘贴/IME 提交）都会刷新过滤结果 */
    private void onSearchChanged(String text) {
        if (text.equals(this.searchText)) return; // 相同文本不再重建，防止 setValue 触发 responder 造成无限递归
        this.searchText = text;
        scrollOffset = 0;
        initGui();
    }

    private List<MergedRow> getVisibleRowsOnly() {
        List<MergedRow> visible = new ArrayList<>();
        for (MergedRow row : rows) {
            if (row.hasActiveSources()) {
                if (hideNoMissing) {
                    if (row.totalCount - getPlayerItemCount(row) <= 0) continue;
                }
                // 搜索过滤（中文名/拼音全拼/拼音首字母/英文ID），直接读输入框实时文本
                if (searchActive) {
                    String query = searchField != null ? searchField.getValue() : searchText;
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
    private int getPlayerItemCount(MergedRow row) {
        if (row.exact && row.exactStack != null && !row.exactStack.isEmpty()) {
            return getExactPlayerCount(row.exactStack);
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

    private String formatCountTooltip(int count) {
        int shulker = count / 1728;
        int remainder = count % 1728;
        int stacks = remainder / 64;
        int items = remainder % 64;
        double shulkerFloat = (double) count / 1728.0;
        return String.format("%d = %d×1728 + %d×64 + %d  |  %d×64+%d  |  %.2f 潜影盒",
                count, shulker, stacks, items, shulker * 27 + stacks, items, shulkerFloat);
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

    private void calculateColumnPositions() {
        int maxTotalWidth = this.font.width(I18n.tr("litematlist.header.total"));
        int maxHaveWidth = this.font.width(I18n.tr("litematlist.header.have"));
        int maxMissWidth = this.font.width(I18n.tr("litematlist.header.missing"));
        int maxNameWidth = this.font.width(I18n.tr("litematlist.header.item"));

        for (MergedRow row : rows) {
            int nameW = this.font.width(row.name);
            if (row.exact) nameW = Math.min(nameW, MAX_PROJECT_NAME_WIDTH);
            maxNameWidth = Math.max(maxNameWidth, nameW);
            maxTotalWidth = Math.max(maxTotalWidth, this.font.width(String.valueOf(row.totalCount)));
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
            case HAVE_DESC -> Comparator.comparingInt((MergedRow r) -> getPlayerItemCount(r)).reversed();
            case HAVE_ASC -> Comparator.comparingInt(r -> getPlayerItemCount(r));
            case MISS_DESC -> Comparator.comparingInt((MergedRow r) -> Math.max(0, r.totalCount - getPlayerItemCount(r))).reversed();
            case MISS_ASC -> Comparator.comparingInt(r -> Math.max(0, r.totalCount - getPlayerItemCount(r)));
            default -> null;
        };
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks) {
        GuiContext ctx = GuiContext.fromGuiGraphics(gfx);
        this.extractBackground(gfx, mouseX, mouseY, partialTicks);

        if (rows.isEmpty()) {
            ctx.drawCenteredString(this.font, I18n.tr("litematlist.error.no_data"),
                    this.width / 2, this.height / 2 - 10, 0xFFFFFFFF);
            super.extractRenderState(gfx, mouseX, mouseY, partialTicks);
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
        ctx.fill(8, LIST_TOP - 2, listRight, listBottom + 2, 0x40000000);

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

                int totalW = this.font.width(String.valueOf(row.totalCount));
                int haveW = this.font.width(String.valueOf(getPlayerItemCount(row)));
                int missW = this.font.width(String.valueOf(Math.max(0, row.totalCount - getPlayerItemCount(row))));
                if (mouseX >= colTotalX - 10 && mouseX <= colTotalX + totalW + 10) hoveredColumn = 0;
                else if (mouseX >= colHaveX - 10 && mouseX <= colHaveX + haveW + 10) hoveredColumn = 1;
                else if (mouseX >= colMissX - 10 && mouseX <= colMissX + missW + 10) hoveredColumn = 2;
            } else if ((i - scrollOffset) % 2 == 0) {
                ctx.fill(10, rowY, listRight, rowY + ROW_HEIGHT, 0x10FFFFFF);
            }

            // 子项独立高亮检测（不在这里绘制，在 super.extractRenderState 之后绘制以覆盖按钮区域）
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

        ctx.fill(8, HEADER_Y, listRight, HEADER_Y + HEADER_HEIGHT, 0x60000000);
        super.extractRenderState(gfx, mouseX, mouseY, partialTicks);

        // 隐藏/标记按钮文字
        if (!hideBtnLabel.isEmpty()) {
            int colonIdx = hideBtnLabel.indexOf("：");
            if (colonIdx < 0) colonIdx = hideBtnLabel.indexOf(":");
            if (colonIdx > 0) {
                String prefix = hideBtnLabel.substring(0, colonIdx + 1);
                String status = hideBtnLabel.substring(colonIdx + 1);
                ctx.drawString(this.font, prefix, hideBtnX + 4, hideBtnY + 5, 0xFFFFFFFF);
                ctx.drawString(this.font, status, hideBtnX + 4 + this.font.width(prefix), hideBtnY + 5,
                        hideNoMissing ? 0xFF55FF55 : 0xFFFF5555);
            }
        }
        if (!pinBtnLabel.isEmpty()) {
            int colonIdx = pinBtnLabel.indexOf("：");
            if (colonIdx < 0) colonIdx = pinBtnLabel.indexOf(":");
            if (colonIdx > 0) {
                String prefix = pinBtnLabel.substring(0, colonIdx + 1);
                String status = pinBtnLabel.substring(colonIdx + 1);
                ctx.drawString(this.font, prefix, pinBtnX + 4, pinBtnY + 5, 0xFFFFFFFF);
                ctx.drawString(this.font, status, pinBtnX + 4 + this.font.width(prefix), pinBtnY + 5,
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
            ctx.fill(10, hoverY, this.width - 8, hoverY + hoverH, 0x40FFFFFF);
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
            if (rowY + rowH > maxRenderY) break; // 不渲染到底部按钮区域
            int iconX = 15, nameX = 35;

            if (allowPin) {
                boolean isPinned = currentPinned.contains(row.key);
                boolean pinHovered = mouseX >= 15 && mouseX <= 31 && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
                Identifier pinTexture = isPinned ? (pinHovered ? PIN_HOVERED_FAVORITE : PIN_FAVORITE)
                        : (pinHovered ? PIN_HOVERED_EMPTY : PIN_EMPTY);
                drawTextureIcon(ctx, pinTexture, 15, rowY + 2, 16, 16);
                iconX = 33; nameX = 53;
            }

            ctx.renderItem(row.itemStack, iconX, rowY + 2);
            // 精确物品使用带样式文本渲染（自定义名自动斜体、稀有度颜色）；过长时截断，悬停显示完整精确ID
            String nameStr = row.displayText != null ? row.displayText.getString() : row.name;
            boolean nameTruncated = this.font.width(nameStr) > MAX_PROJECT_NAME_WIDTH;
            if (nameTruncated) nameStr = TextWidthUtils.truncateByWidth(nameStr, MAX_PROJECT_NAME_WIDTH);
            ctx.drawString(this.font, nameStr, nameX, rowY + 5, 0xFFFFFFFF);
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
                hoveredItemId = BuiltInRegistries.ITEM.getKey(row.item).toString();
            }
            ctx.drawString(this.font, String.valueOf(row.totalCount), colTotalX, rowY + 5, 0xFFFFFFFF);
            int haveCount = getPlayerItemCount(row);
            ctx.drawString(this.font, String.valueOf(haveCount), colHaveX, rowY + 5, 0xFFFFFFFF);
            int missCount = Math.max(0, row.totalCount - haveCount);
            int missColor = missCount == 0 ? 0xFF55FF55 : (haveCount >= missCount ? 0xFFFFAA00 : 0xFFFF5555);
            ctx.drawString(this.font, String.valueOf(missCount), colMissX, rowY + 5, missColor);

            // 展开/折叠图标（居中于移除按钮中轴, 无按钮背景）
            if (row.sources.size() > 1) {
                int removeCenterX = this.width - 12 - 25; // 移除按钮宽50, 中轴
                int expandX = removeCenterX - EXPAND_ICON_SIZE / 2;
                boolean expHovered = mouseX >= expandX && mouseX <= expandX + EXPAND_ICON_SIZE
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
                Identifier expIcon;
                if (row.expanded) expIcon = expHovered ? COLLAPSE_HOVERED : COLLAPSE_NORMAL;
                else expIcon = expHovered ? EXPAND_HOVERED : EXPAND_NORMAL;
                drawTextureIcon(ctx, expIcon, expandX, rowY + 2, EXPAND_ICON_SIZE, EXPAND_ICON_SIZE);
            }

            // 展开的子项（仅显示未忽略的）
            if (row.expanded) {
                int subNameX = nameX + 10;
                int subIdx = 0;
                for (int j = 0; j < row.sources.size(); j++) {
                    SourceEntry src = row.sources.get(j);
                    if (row.isSourceIgnored(src)) continue; // 已忽略的不显示
                    int subY = rowY + ROW_HEIGHT + subIdx * ROW_HEIGHT;
                    ctx.fill(10, subY, listRight, subY + ROW_HEIGHT, (subIdx % 2 == 0) ? 0x18FFFFFF : 0x08FFFFFF);
                    ItemStack subIcon = (src.exact && src.exactStack != null) ? src.exactStack : new ItemStack(src.item);
                    ctx.renderItem(subIcon, subNameX, subY + 2);
                    String subLabel = src.exact ? src.key : src.item.getName(new ItemStack(src.item)).getString();
                    String subText = " - " + subLabel + " " + src.count + "  (" + src.listName + ")";
                    int textEndX = subNameX + 18 + this.font.width(subText);
                    // 如果被替换，显示原物品图标和名称（精确物品不参与按物品替换）
                    if (!src.exact && src.item != src.originalItem) {
                        int origIconX = textEndX + 4;
                        ctx.renderItem(new ItemStack(src.originalItem), origIconX, subY + 2);
                        String origText = " " + src.originalItem.getName(new ItemStack(src.originalItem)).getString() + "（原有）";
                        ctx.drawString(this.font, origText, origIconX + 18, subY + 5, 0xCCAAAAAA);
                    }
                    ctx.drawString(this.font, subText, subNameX + 18, subY + 5, 0xCCCCCCCC);
                    // 子项数量级 tooltip
                    if (mouseX >= subNameX && mouseX <= listRight && mouseY >= subY && mouseY < subY + ROW_HEIGHT) {
                        drawTooltipBox(ctx, formatCountTooltip(src.count), mouseX, mouseY);
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

        if (hoveredRow >= 0 && hoveredColumn >= 0 && hoveredRow < displayRows.size()) {
            MergedRow hovered = displayRows.get(hoveredRow);
            int haveCount = getPlayerItemCount(hovered);
            int val = switch (hoveredColumn) {
                case 0 -> hovered.totalCount;
                case 1 -> haveCount;
                case 2 -> Math.max(0, hovered.totalCount - haveCount);
                default -> hovered.totalCount;
            };
            drawTooltipBox(ctx, formatCountTooltip(val), mouseX, mouseY);
        }

        // 精确物品图标悬停：物品悬浮提示（在名称提示之前绘制，名称提示叠于其上）
        if (hoveredExactStack != null) {
            ctx.renderTooltip(this.font, ctx.itemTooltips(hoveredExactStack), mouseX, mouseY);
        }
        // 悬停物品名：精确物品或名称被截断时显示完整名称/精确ID（过长时按屏幕边界自动换行）
        if (hoveredFullName != null) {
            drawWrappedTooltip(ctx, hoveredFullName, mouseX, mouseY);
        }
        // 悬停普通物品图标显示对应物品ID
        if (hoveredItemId != null) {
            drawTooltipBox(ctx, hoveredItemId, mouseX, mouseY);
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
            ctx.drawCenteredString(this.font, scrollInfo, this.width / 2, scrollInfoY, 0xFFFFFFFF);

            // 滚动条（像素级计算，适配可变行高）
            int scrollbarX = this.width - 6;
            int scrollbarWidth = 3;
            // 轨道
            ctx.fill(scrollbarX, LIST_TOP, scrollbarX + scrollbarWidth, listBottom, 0x30FFFFFF);

            // 计算 scrollOffset 对应的像素偏移
            int scrolledPixels = 0;
            for (int i = 0; i < scrollOffset && i < displayRows.size(); i++) {
                scrolledPixels += displayRows.get(i).getRowHeight();
            }
            int maxScrollPixels = Math.max(1, totalContentHeight - trackHeight);
            float ratio = (float) trackHeight / totalContentHeight;
            int thumbHeight = Math.max(8, (int)(trackHeight * ratio));
            int thumbY = LIST_TOP + (int)((trackHeight - thumbHeight) * (float) scrolledPixels / maxScrollPixels);
            ctx.fill(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, 0x80FFFFFF);
        }
    }

    @Override
    public boolean onMouseClicked(MouseButtonEvent event, boolean isDrag) {
        if (super.onMouseClicked(event, isDrag)) return true;
        if (ignoreJustClicked) {
            ignoreJustClicked = false;
            return true;
        }
        if (isDrag) return false;
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        if (event.button() == 0) {
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
                if (!searchActive) { searchText = ""; if (searchField != null) searchField.setValue(""); }
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
                    if (row.expanded) expandedItems.add(row.key);
                    else expandedItems.remove(row.key);
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
                    if (currentPinned.contains(row.key)) { currentPinned.remove(row.key); syncPinToSubLists(row.key, false); }
                    else { currentPinned.add(row.key); syncPinToSubLists(row.key, true); }
                    MaterialDataStore.savePersistence();
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
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        int mouseY = (int) event.y();
        int button = event.button();

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
            int currentRow = getRowAtY(mouseY, display);
            if (currentRow >= 0 && currentRow < display.size() && currentRow != draggedRow && Math.abs(mouseY - dragStartY) > 5) {
                // 不允许跨置顶边界拖拽
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
        return super.mouseReleased(event);
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