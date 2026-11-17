package com.litematlist.gui;
//#if MC == 11701
//$$import net.minecraft.text.LiteralText;
//#endif

import com.litematlist.I18n;
//#if MC == 12108 || MC == 12110 || MC == 12111
import com.litematlist.LitematicReader;
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import com.litematlist.LitematListMod;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$import com.litematlist.LitematicReader;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$import com.litematlist.LitematListMod;
//#endif
import com.litematlist.MaterialListImporter;
import com.litematlist.config.Configs;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
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
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.registry.Registries;
//#elseif MC == 11701
//$$import net.minecraft.util.registry.Registry;
//#endif
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.file.Path;
import java.util.*;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
/**
 * 项目总材料列表的已忽略物品界面。
 * 支持多来源合并展开，取消忽略时同步到所有子列表。
 */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$/**
//$$ * 椤圭洰鎬绘潗鏂欏垪琛ㄧ殑宸插拷鐣ョ墿鍝佺晫闈€? * 鏀寔澶氭潵婧愬悎骞跺睍寮€锛屽彇娑堝拷鐣ユ椂鍚屾鍒版墍鏈夊瓙鍒楄〃銆? */
//#endif
public class ProjectIgnoredItemsScreen extends GuiBase {

    private final GuiBase parent;
    private final String projectName;
    private final Set<String> ignoredKeys;
    private final Map<String, Integer> ignoredTotals;
    private final List<MaterialListScreen.LoadedEntry> projectEntries;
    private final List<IgnoredRow> rows = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int ROW_HEIGHT = 22;
    private static final int LIST_TOP = 48;
    private int visibleRows;
    private int hoveredRow = -1;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private int hoveredSubCount = -1; // 悬停子项的数量，用于tooltip
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    private int hoveredSubCount = -1; // 鎮仠瀛愰」鐨勬暟閲忥紝鐢ㄤ簬tooltip
//#endif

//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final Identifier SEARCH_ICON = Identifier.of("litematlist", "textures/gui/search.png");
//#elseif MC == 11701
//$$    private static final Identifier SEARCH_ICON = new Identifier("litematlist", "textures/gui/search.png");
//#endif
    private boolean searchActive = false;
    private String searchText = "";
    private TextFieldWidget searchField;

    private enum SortMode { NONE(""), TOTAL_DESC("▼"), TOTAL_ASC("▲"), NAME_ASC("▲"), NAME_DESC("▼");
        final String arrow; SortMode(String a) { this.arrow = a; }
    }
    private SortMode sortMode = SortMode.NONE;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // 拖拽重排
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // 鎷栨嫿閲嶆帓
//#endif
    private int draggedRow = -1;
    private int dragStartY = 0;
    private boolean isDragging = false;

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
    private static final int EXPAND_ICON_SIZE = 16;

    private static class SourceEntry {
        final String listName;
        final int count;
        final Item item;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        final Item originalItem; // 替换前的原始物品
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        final Item originalItem; // 鏇挎崲鍓嶇殑鍘熷鐗╁搧
//#endif
        final Path filePath;
        final String key;        // 稳定键：普通=更换后物品ID；精确=精确ID
        final boolean exact;     // 是否为精确物品来源
        final ItemStack exactStack; // 精确物品堆叠（普通为 null）
        SourceEntry(String listName, int count, Item item, Item originalItem, Path filePath,
                    String key, boolean exact, ItemStack exactStack) {
            this.listName = listName; this.count = count; this.item = item;
            this.originalItem = originalItem; this.filePath = filePath;
            this.key = key; this.exact = exact; this.exactStack = exactStack;
        }
    }

    private static class IgnoredRow {
        final ItemStack itemStack;
        final String name;
        final String key;
        final int totalCount;
        final boolean exact;
        final List<SourceEntry> sources;
        boolean expanded;

        IgnoredRow(ItemStack is, String name, String key, int total, boolean exact, List<SourceEntry> sources) {
            this.itemStack = is; this.name = name; this.key = key; this.totalCount = total;
            this.exact = exact; this.sources = sources; this.expanded = false;
        }
        int getRowHeight() { return ROW_HEIGHT + (expanded ? sources.size() * ROW_HEIGHT : 0); }
    }

    public ProjectIgnoredItemsScreen(GuiBase parent, String projectName, Set<String> ignoredKeys,
                                      Map<String, Integer> ignoredTotals, List<MaterialListScreen.LoadedEntry> projectEntries) {
        super();
        this.parent = parent;
        this.projectName = projectName;
        this.ignoredKeys = ignoredKeys;
        this.ignoredTotals = ignoredTotals != null ? ignoredTotals : new HashMap<>();
        this.projectEntries = projectEntries;
        this.title = I18n.tr("litematlist.title.ignored_items", projectName);
        buildRows();
    }

    private void buildRows() {
        rows.clear();
        Map<String, List<SourceEntry>> sourceMap = new LinkedHashMap<>();
        Map<String, String> itemNames = new HashMap<>();
        Map<String, Item> rowItems = new HashMap<>();
        Map<String, ItemStack> exactStacks = new HashMap<>();
        Set<String> exactKeys = new HashSet<>();
        Map<String, Integer> totalCounts = new HashMap<>();

        for (MaterialListScreen.LoadedEntry entry : projectEntries) {
            Path fp = entry.filePath();
            if (fp == null) continue;
            Set<Item> subIgnored = MaterialDataStore.getIgnoredForPath(fp);
            Set<String> subExactIgnored = MaterialDataStore.getExactIgnoredForPath(fp);
            boolean hasPlain = subIgnored != null && !subIgnored.isEmpty();
            boolean hasExact = subExactIgnored != null && !subExactIgnored.isEmpty();
            if (!hasPlain && !hasExact) continue;
            String listName = entry.name();
            if (MaterialListScreen.isTxtEntry(entry)) listName += "（txt）";
            else if (MaterialListScreen.isJsonEntry(entry)) listName += "（JSON）";

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 获取该文件的实际材料数据以获取每个物品/精确物品的数量（键：普通=替换后物品ID；精确=精确ID）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 鑾峰彇璇ユ枃浠剁殑瀹為檯鏉愭枡鏁版嵁浠ヨ幏鍙栨瘡涓墿鍝佺殑鏁伴噺
//#endif
            Map<String, Integer> itemCounts = new HashMap<>();
            Map<String, Item> keyItems = new HashMap<>();
            Map<String, ItemStack> keyExactStacks = new HashMap<>();
            try {
                List<LitematicReader.MaterialEntry> entries;
                if (MaterialDataStore.getImportedMaterials().containsKey(fp)) {
                    entries = MaterialDataStore.getImportedMaterials().get(fp);
                } else if (fp.toString().toLowerCase().endsWith(".txt") || fp.toString().toLowerCase().endsWith(".json") || fp.toString().toLowerCase().endsWith(".csv")) {
                    MaterialListImporter.ImportResult result = MaterialListImporter.importFromFile(fp);
                    if (result != null) entries = result.materials();
                    else entries = Collections.emptyList();
                } else {
                    entries = LitematicReader.loadMaterialList(fp);
                }
                for (LitematicReader.MaterialEntry me : entries) {
                    if (me.isExact()) {
                        String key = me.blockName();
                        itemCounts.merge(key, me.totalCount(), Integer::sum);
                        keyItems.putIfAbsent(key, me.item());
                        keyExactStacks.putIfAbsent(key, me.toItemStack());
                        continue;
                    }
                    Item originalItem = me.item();
                    Item actualItem = MaterialDataStore.getReplacement(fp, originalItem);
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    String key = Registries.ITEM.getId(actualItem).toString();
//#elseif MC == 11701
//$$                    String key = Registry.ITEM.getId(actualItem).toString();
//#endif
                    itemCounts.merge(key, me.totalCount(), Integer::sum);
                    keyItems.putIfAbsent(key, actualItem);
                }
            } catch (Exception e) {
                LitematListMod.LOGGER.warn("[ProjectIgnoredItemsScreen] 读取材料数据失败: {} ({})", fp, e.toString());
            }

            if (hasPlain) {
                for (Item item : subIgnored) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    String key = Registries.ITEM.getId(item).toString();
//#elseif MC == 11701
//$$                    String key = Registry.ITEM.getId(item).toString();
//#endif
                    int count = itemCounts.getOrDefault(key, 0);
                    sourceMap.computeIfAbsent(key, k -> new ArrayList<>())
                            .add(new SourceEntry(listName, count, item, item, fp, key, false, null));
                    itemNames.putIfAbsent(key, item.getName().getString());
                    rowItems.putIfAbsent(key, item);
                    totalCounts.merge(key, count, Integer::sum);
                }
            }
            if (hasExact) {
                for (String exactId : subExactIgnored) {
                    Item baseItem = keyItems.get(exactId);
                    if (baseItem == null) continue;
                    int count = itemCounts.getOrDefault(exactId, 0);
                    ItemStack exactStack = keyExactStacks.get(exactId);
                    sourceMap.computeIfAbsent(exactId, k -> new ArrayList<>())
                            .add(new SourceEntry(listName, count, baseItem, baseItem, fp, exactId, true, exactStack));
                    itemNames.putIfAbsent(exactId, exactId);
                    rowItems.putIfAbsent(exactId, baseItem);
                    exactStacks.putIfAbsent(exactId, exactStack);
                    exactKeys.add(exactId);
                    totalCounts.merge(exactId, count, Integer::sum);
                }
            }
        }

        for (var entry : sourceMap.entrySet()) {
            String key = entry.getKey();
            List<SourceEntry> sources = entry.getValue();
            int total = totalCounts.getOrDefault(key, 0);
            if (exactKeys.contains(key)) {
                ItemStack exactStack = exactStacks.get(key);
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                ItemStack icon = exactStack != null ? exactStack.copyWithCount(total) : new ItemStack(rowItems.get(key), total);
//#endif
//#if MC == 11701
//$$                ItemStack icon;
//#endif
//#if MC == 11701
//$$                if (exactStack != null) { icon = exactStack.copy(); icon.setCount(total); }
//#endif
//#if MC == 11701
//$$                else { icon = new ItemStack(rowItems.get(key), total); }
//#endif
                rows.add(new IgnoredRow(icon, key, key, total, true, sources));
            } else {
                Item item = rowItems.get(key);
                rows.add(new IgnoredRow(new ItemStack(item, total), itemNames.get(key), key, total, false, sources));
            }
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 初始按总计数量从高到低排列
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鍒濆鎸夋€昏鏁伴噺浠庨珮鍒颁綆鎺掑垪
//#endif
        rows.sort(Comparator.comparingInt((IgnoredRow r) -> r.totalCount).reversed());
    }

    private void unignoreFromAllSubLists(String key, boolean exact) {
        for (MaterialListScreen.LoadedEntry entry : projectEntries) {
            Path fp = entry.filePath();
            if (fp == null) continue;
            if (exact) {
                MaterialDataStore.getExactIgnoredForPath(fp).remove(key);
            } else {
                Set<Item> subIgnored = MaterialDataStore.getIgnoredForPath(fp);
                if (subIgnored != null) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    Item item = Registries.ITEM.get(Identifier.of(key));
//#elseif MC == 11701
//$$                    Item item = Registry.ITEM.get(Identifier.tryParse(key));
//#elseif MC == 11904 || MC == 12001
//$$                    Item item = Registries.ITEM.get(Identifier.tryParse(key));
//#endif
                    if (item != null) subIgnored.remove(item);
                }
            }
        }
        MaterialDataStore.savePersistence();
    }

    private void unignoreSource(SourceEntry src) {
        if (src.exact) {
            MaterialDataStore.getExactIgnoredForPath(src.filePath).remove(src.key);
        } else {
            Set<Item> subIgnored = MaterialDataStore.getIgnoredForPath(src.filePath);
            if (subIgnored != null) subIgnored.remove(src.item);
        }
        MaterialDataStore.savePersistence();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        MaterialListHudRenderer.refreshHud(); // 取消忽略后更新HUD
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        MaterialListHudRenderer.refreshHud(); // 鍙栨秷蹇界暐鍚庢洿鏂癏UD
//#endif
        buildRows();
        initGui();
    }

    @Override
    public void initGui() {
        super.initGui();
        this.visibleRows = (this.height - 100) / ROW_HEIGHT;

        int buttonY = this.height - 38;

        ButtonGeneric backButton = new ButtonGeneric(this.width / 2 - 60, buttonY, 120, 20, I18n.tr("litematlist.button.back"));
        backButton.setRenderDefaultBackground(true);
        this.addButton(backButton, (IButtonActionListener) (button, mouseButton) ->
                MinecraftClient.getInstance().setScreen(parent));

        ButtonGeneric clearAllBtn = new ButtonGeneric(this.width / 2 + 70, buttonY, 100, 20, I18n.tr("litematlist.button.clear_all_ignored"));
        clearAllBtn.setRenderDefaultBackground(true);
        this.addButton(clearAllBtn, (IButtonActionListener) (b, mb) -> {
            for (IgnoredRow row : new ArrayList<>(rows)) {
                unignoreFromAllSubLists(row.key, row.exact);
            }
            ignoredKeys.clear();
            rows.clear();
            // 立刻通知 HUD：清除全部忽略后所有材料恢复显示（与单行取消忽略同路径）。
            // 必须用 forceRefreshHud 绕过 200ms 冷却，否则紧跟在其它刷新之后时会被吞掉。
            MaterialListHudRenderer.forceRefreshHud();
            initGui();
        });

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 搜索框
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鎼滅储妗?
//#endif
        int searchIconX = 10;
        if (searchField == null) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            this.searchField = new TextFieldWidget(this.textRenderer, searchIconX + 18, 32, 80, 14, Text.empty());
//#elseif MC == 11701
//$$            this.searchField = new TextFieldWidget(this.textRenderer, searchIconX + 18, 32, 80, 14, LiteralText.EMPTY);
//#endif
            this.searchField.setMaxLength(50);
            this.searchField.setDrawsBackground(false);
            this.searchField.setText(searchText);
            this.addSelectableChild(this.searchField);
        } else {
            this.searchField.setX(searchIconX + 18);
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            this.searchField.setY(32);
//#elseif MC == 11701
//$$            this.searchField.y = 32;
//#endif
            this.searchField.setText(searchText);
            this.addSelectableChild(this.searchField);
        }

        createRowButtons();
    }

    private List<IgnoredRow> getSortedRows() {
        List<IgnoredRow> sorted = new ArrayList<>(rows);
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 搜索过滤
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鎼滅储杩囨护
//#endif
        if (searchActive && !searchText.isEmpty()) {
            sorted.removeIf(r -> !r.name.toLowerCase().contains(searchText.toLowerCase()));
        }
        return sorted;
    }

    private void createRowButtons() {
        List<IgnoredRow> display = getSortedRows();
        int cumulativeY = LIST_TOP;
        int count = 0;

        for (int i = scrollOffset; i < display.size(); i++) {
            IgnoredRow row = display.get(i);
            int rowY = cumulativeY;

            if (row.sources.size() > 1) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 多来源：展开时显示子项的取消忽略按钮
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                // 澶氭潵婧愶細灞曞紑鏃舵樉绀哄瓙椤圭殑鍙栨秷蹇界暐鎸夐挳
//#endif
                if (row.expanded) {
                    for (int j = 0; j < row.sources.size(); j++) {
                        SourceEntry src = row.sources.get(j);
                        int subY = rowY + ROW_HEIGHT + j * ROW_HEIGHT;
                        int btnX = this.width - 80;
                        ButtonGeneric restoreBtn = new ButtonGeneric(btnX, subY + 1, 60, 20, I18n.tr("litematlist.button.cancel_ignore"));
                        restoreBtn.setRenderDefaultBackground(true);
                        final SourceEntry s = src;
                        this.addButton(restoreBtn, (IButtonActionListener) (b, mb) -> unignoreSource(s));
                    }
                }
            } else {
                int btnX = this.width - 120;
                ButtonGeneric restoreBtn = new ButtonGeneric(btnX, rowY + 1, 100, 20, I18n.tr("litematlist.button.cancel_ignore"));
                restoreBtn.setRenderDefaultBackground(true);
                final int idx = i;
                this.addButton(restoreBtn, (IButtonActionListener) (b, mb) -> {
                    IgnoredRow r = rows.get(idx);
                    unignoreFromAllSubLists(r.key, r.exact);
                    ignoredKeys.remove(r.key);
                    rows.remove(idx);
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    MaterialListHudRenderer.refreshHud(); // 取消忽略后更新HUD
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    MaterialListHudRenderer.refreshHud(); // 鍙栨秷蹇界暐鍚庢洿鏂癏UD
//#endif
                    initGui();
                });
            }

            cumulativeY += row.getRowHeight();
            count++;
            if (count >= visibleRows) break;
        }
    }

    private String formatCountTooltip(int total) {
        int shulker = total / 1728;
        int remainder = total % 1728;
        int stacks = remainder / 64;
        int items = remainder % 64;
        double shulkerFloat = (double) total / 1728.0;
        return String.format("%d = %d×1728 + %d×64 + %d  |  %d×64+%d  |  %.2f 潜影盒",
                total, shulker, stacks, items, shulker * 27 + stacks, items, shulkerFloat);
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
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$        this.textRenderer.drawWithShadow(matrices, this.title, this.width / 2 - this.textRenderer.getWidth(this.title) / 2, 12, 0xFFFFFFFF);
//#endif

        String countText = I18n.tr("litematlist.ignored.count", rows.size());
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawCenteredTextWithShadow(this.textRenderer, countText, this.width / 2, 24, 0xFFAAAAAA);
//#elseif MC == 11701 || MC == 11904
//$$        this.textRenderer.drawWithShadow(matrices, countText, this.width / 2 - this.textRenderer.getWidth(countText) / 2, 24, 0xFFAAAAAA);
//#endif

        if (rows.isEmpty()) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawCenteredTextWithShadow(this.textRenderer, I18n.tr("litematlist.ignored.empty"),
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    this.width / 2, this.height / 2 - 10, 0xFFFFFFFF);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            super.render(drawContext, mouseX, mouseY, partialTicks);
//#endif
//#if MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, I18n.tr("litematlist.ignored.empty"), this.width / 2 - this.textRenderer.getWidth(I18n.tr("litematlist.ignored.empty")) / 2, this.height / 2 - 10, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$            super.render(matrices, mouseX, mouseY, partialTicks);
//#endif
            return;
        }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(8, 30, this.width - 8, 46, 0x60000000);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, 8, 30, this.width - 8, 46, 0x60000000);
//#endif
        List<IgnoredRow> display = getSortedRows();
        int listRight = this.width - 8;
        int cumulativeY = LIST_TOP;
        int count = 0;
        for (int i = scrollOffset; i < display.size() && count < visibleRows; i++) {
            cumulativeY += display.get(i).getRowHeight();
            count++;
        }
        int listBottom = cumulativeY;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(8, LIST_TOP - 2, listRight, listBottom + 2, 0x40000000);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, 8, LIST_TOP - 2, listRight, listBottom + 2, 0x40000000);
//#endif

        hoveredRow = -1;
        hoveredSubCount = -1;
        cumulativeY = LIST_TOP;
        count = 0;

        for (int i = scrollOffset; i < display.size() && count < visibleRows; i++) {
            IgnoredRow row = display.get(i);
            int rowY = cumulativeY;
            int rowH = row.getRowHeight();

            if (mouseX >= 10 && mouseX <= listRight && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                hoveredRow = i;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.fill(10, rowY, listRight, rowY + ROW_HEIGHT, 0x40FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$                DrawableHelper.fill(matrices, 10, rowY, listRight, rowY + ROW_HEIGHT, 0x40FFFFFF);
//#endif
            } else if ((i - scrollOffset) % 2 == 0) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.fill(10, rowY, listRight, rowY + ROW_HEIGHT, 0x10FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$                DrawableHelper.fill(matrices, 10, rowY, listRight, rowY + ROW_HEIGHT, 0x10FFFFFF);
//#endif
            }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 子项独立高亮
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 瀛愰」鐙珛楂樹寒
//#endif
            if (row.expanded) {
                for (int j = 0; j < row.sources.size(); j++) {
                    int subY = rowY + ROW_HEIGHT + j * ROW_HEIGHT;
                    if (mouseX >= 10 && mouseX <= listRight && mouseY >= subY && mouseY < subY + ROW_HEIGHT) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        drawContext.fill(10, subY, listRight, subY + ROW_HEIGHT, 0x30FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$                        DrawableHelper.fill(matrices, 10, subY, listRight, subY + ROW_HEIGHT, 0x30FFFFFF);
//#endif
                    }
                }
            }

            cumulativeY += rowH;
            count++;
        }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        super.render(drawContext, mouseX, mouseY, partialTicks);
//#elseif MC == 11701 || MC == 11904
//$$        super.render(matrices, mouseX, mouseY, partialTicks);
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 搜索图标（左置，不遮挡物品文字）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鎼滅储鍥炬爣锛堝乏缃紝涓嶉伄鎸＄墿鍝佹枃瀛楋級
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, SEARCH_ICON, 10, 33, 0.0f, 0.0f, 14, 14, 14, 14);
//#elseif MC == 11701 || MC == 11904
//$$        RenderSystem.setShaderTexture(0, SEARCH_ICON); DrawableHelper.drawTexture(matrices, 10, 33, 0.0f, 0.0f, 14, 14, 14, 14);
//#elseif MC == 12001
//$$        drawContext.drawTexture(SEARCH_ICON, 10, 33, 0.0f, 0.0f, 14, 14, 14, 14);
//#elseif MC == 12101
//$$        drawContext.drawTexture( SEARCH_ICON, 10, 33, 0.0f, 0.0f, 14, 14, 14, 14);
//#elseif MC == 12104
//$$        drawContext.drawTexture(RenderLayer::getGuiTextured, SEARCH_ICON, 10, 33, 0.0f, 0.0f, 14, 14, 14, 14);
//#endif
        // 搜索框
        if (searchActive && searchField != null) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(28, 31, 28 + 82, 46, 0xFF000000);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, 28, 31, 28 + 82, 46, 0xFF000000);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(29, 32, 29 + 80, 45, 0xFF222222);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, 29, 32, 29 + 80, 45, 0xFF222222);
//#endif
            String st = searchField.getText();
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawText(this.textRenderer, st, 31, 34, 0xFFFFFFFF, false);
//#elseif MC == 11701 || MC == 11904
//$$            this.textRenderer.draw(matrices, st, 31, 34, 0xFFFFFFFF);
//#endif
            if (searchField.isFocused() && System.currentTimeMillis() % 1000 < 500) {
                int cp = searchField.getCursor();
                String bc = st.substring(0, Math.min(cp, st.length()));
                int cx = 31 + this.textRenderer.getWidth(bc);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.fill(cx, 33, cx + 1, 43, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$                DrawableHelper.fill(matrices, cx, 33, cx + 1, 43, 0xFFFFFFFF);
//#endif
            }
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 物品列表头
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鐗╁搧鍒楄〃澶?
//#endif
        String nameHeader = I18n.tr("litematlist.header.item") + (sortMode == SortMode.NAME_ASC || sortMode == SortMode.NAME_DESC ? sortMode.arrow : "");
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawTextWithShadow(this.textRenderer, nameHeader, searchActive ? 115 : 35, 33, 0xFFAAAAAA);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawTextWithShadow(this.textRenderer,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                I18n.tr("litematlist.header.total") + (sortMode == SortMode.TOTAL_DESC || sortMode == SortMode.TOTAL_ASC ? sortMode.arrow : ""),
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                200, 33, 0xFFAAAAAA);
//#endif
//#if MC == 11701 || MC == 11904
//$$        this.textRenderer.drawWithShadow(matrices, nameHeader, searchActive ? 115 : 35, 33, 0xFFAAAAAA);
//#endif
//#if MC == 11701 || MC == 11904
//$$        this.textRenderer.drawWithShadow(matrices, I18n.tr("litematlist.header.total") + (sortMode == SortMode.TOTAL_DESC || sortMode == SortMode.TOTAL_ASC ? sortMode.arrow : ""), 200, 33, 0xFFAAAAAA);
//#endif

        cumulativeY = LIST_TOP;
        count = 0;
        int maxIdx = Math.min(display.size(), scrollOffset + visibleRows);

        for (int i = scrollOffset; i < maxIdx; i++) {
            IgnoredRow row = display.get(i);
            int rowY = cumulativeY;

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawItem(row.itemStack, 15, rowY + 2);
//#elseif MC == 11701
//$$            MinecraftClient.getInstance().getItemRenderer().renderInGui(row.itemStack, 15, rowY + 2);
//#elseif MC == 11904
//$$            MinecraftClient.getInstance().getItemRenderer().renderInGui(matrices, row.itemStack, 15, rowY + 2);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTextWithShadow(this.textRenderer, row.name, 35, rowY + 5, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, row.name, 35, rowY + 5, 0xFFFFFFFF);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTextWithShadow(this.textRenderer, String.valueOf(row.totalCount), 200, rowY + 5, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, String.valueOf(row.totalCount), 200, rowY + 5, 0xFFFFFFFF);
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 展开/折叠图标（居中于普通非子项"取消忽略"按钮的中线）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 灞曞紑/鎶樺彔鍥炬爣锛堝眳涓簬鏅€氶潪瀛愰」"鍙栨秷蹇界暐"鎸夐挳鐨勪腑绾匡級
//#endif
            if (row.sources.size() > 1) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 非子项按钮: btnX = this.width - 120, width = 100, 中线 = this.width - 70
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                //  this.width - 120, width = 100, 涓嚎 = this.width - 70
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                int expandX = this.width - 70 - EXPAND_ICON_SIZE / 2; // = this.width - 78
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                int expandX = this.width - 70 - EXPAND_ICON_SIZE / 2;
//#endif
                boolean expHovered = mouseX >= expandX && mouseX <= expandX + EXPAND_ICON_SIZE
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
                Identifier expIcon = row.expanded ? (expHovered ? COLLAPSE_HOVERED : COLLAPSE_NORMAL)
                        : (expHovered ? EXPAND_HOVERED : EXPAND_NORMAL);
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

            // 展开的子项
            if (row.expanded) {
                for (int j = 0; j < row.sources.size(); j++) {
                    SourceEntry src = row.sources.get(j);
                    int subY = rowY + ROW_HEIGHT + j * ROW_HEIGHT;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    drawContext.fill(10, subY, listRight, subY + ROW_HEIGHT, (j % 2 == 0) ? 0x18FFFFFF : 0x08FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$                    DrawableHelper.fill(matrices, 10, subY, listRight, subY + ROW_HEIGHT, (j % 2 == 0) ? 0x18FFFFFF : 0x08FFFFFF);
//#endif
                    ItemStack subIcon = (src.exact && src.exactStack != null) ? src.exactStack : new ItemStack(src.item);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    drawContext.drawItem(subIcon, 30, subY + 2);
//#elseif MC == 11701
//$$                    MinecraftClient.getInstance().getItemRenderer().renderInGui(subIcon, 30, subY + 2);
//#elseif MC == 11904
//$$                    MinecraftClient.getInstance().getItemRenderer().renderInGui(matrices, subIcon, 30, subY + 2);
//#endif
                    String subLabel = src.exact ? src.key : src.item.getName().getString();
                    String subText = " - " + subLabel + " " + src.count + "  (" + src.listName + ")";
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    drawContext.drawTextWithShadow(this.textRenderer, subText, 48, subY + 5, 0xCCCCCCCC);
//#elseif MC == 11701 || MC == 11904
//$$                    this.textRenderer.drawWithShadow(matrices, subText, 48, subY + 5, 0xCCCCCCCC);
//#endif
                    // 子项悬停数量级提示
                    if (mouseX >= 10 && mouseX <= listRight && mouseY >= subY && mouseY < subY + ROW_HEIGHT) {
                        hoveredSubCount = src.count;
                    }
                }
            }

            cumulativeY += row.getRowHeight();
            count++;
            if (count >= visibleRows) break;
        }

        if (hoveredRow >= 0 && hoveredRow < display.size()) {
            IgnoredRow row = display.get(hoveredRow);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTooltip(this.textRenderer, Text.literal(formatCountTooltip(row.totalCount)), mouseX, mouseY);
//#elseif MC == 11701
//$$            this.renderTooltip(matrices, new LiteralText(formatCountTooltip(row.totalCount)), mouseX, mouseY);
//#elseif MC == 11904
//$$            this.renderTooltip(matrices, Text.literal(formatCountTooltip(row.totalCount)), mouseX, mouseY);
//#endif
        }

        if (hoveredSubCount >= 0) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTooltip(this.textRenderer, Text.literal(formatCountTooltip(hoveredSubCount)), mouseX, mouseY);
//#elseif MC == 11701
//$$            this.renderTooltip(matrices, new LiteralText(formatCountTooltip(hoveredSubCount)), mouseX, mouseY);
//#elseif MC == 11904
//$$            this.renderTooltip(matrices, Text.literal(formatCountTooltip(hoveredSubCount)), mouseX, mouseY);
//#endif
        }

        if (display.size() > visibleRows) {
            int scrollInfoY = LIST_TOP + visibleRows * ROW_HEIGHT + 5;
            String scrollInfo = (scrollOffset + 1) + "-" + Math.min(scrollOffset + visibleRows, display.size()) + " / " + display.size();
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawCenteredTextWithShadow(this.textRenderer, scrollInfo, this.width / 2, scrollInfoY, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, scrollInfo, this.width / 2 - this.textRenderer.getWidth(scrollInfo) / 2, scrollInfoY, 0xFFFFFFFF);
//#endif
        }
    }

    @Override
//#if MC == 12110 || MC == 12111
    public boolean onMouseClicked(Click click, boolean isDrag) {
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$    public boolean onMouseClicked(int mouseX, int mouseY, int button) {
//#endif
//#if MC == 12110 || MC == 12111
        if (super.onMouseClicked(click, isDrag)) return true;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        if (super.onMouseClicked(mouseX, mouseY, button)) return true;
//#endif
//#if MC == 12110 || MC == 12111
        if (isDrag) return false;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        
//#endif
//#if MC == 12110 || MC == 12111
        int mouseX = (int) click.x();
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        
//#endif
//#if MC == 12110 || MC == 12111
        int mouseY = (int) click.y();
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        
//#endif
//#if MC == 12110 || MC == 12111
        if (click.button() == 0) {
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        if (button == 0) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (super.mouseClicked(mouseX, mouseY, mouseButton)) return true;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (mouseButton == 0) {
//#endif
            List<IgnoredRow> display = getSortedRows();
            int cumulativeY = LIST_TOP;
            int count = 0;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            int expandX = this.width - 70 - EXPAND_ICON_SIZE / 2; // = this.width - 78
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 搜索图标点击
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 鎼滅储鍥炬爣鐐瑰嚮
//#endif
            if (mouseX >= 10 && mouseX <= 24 && mouseY >= 30 && mouseY <= 46) {
                searchActive = !searchActive;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (searchActive && searchField != null) searchField.setFocused(true);
//#elseif MC == 11701
//$$                if (searchActive && searchField != null) this.setFocused(searchField);
//#endif
                if (!searchActive) { searchText = ""; if (searchField != null) searchField.setText(""); }
                scrollOffset = 0;
                initGui();
                return true;
            }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 表头点击排序 - 物品名称
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 琛ㄥご鐐瑰嚮鎺掑簭 - 鐗╁搧鍚嶇О
//#endif
            int nameHeaderX = searchActive ? 115 : 35;
            if (mouseY >= 30 && mouseY <= 46 && mouseX >= nameHeaderX - 5 && mouseX <= nameHeaderX + 60) {
                SortMode newMode = (sortMode == SortMode.NAME_ASC) ? SortMode.NAME_DESC
                        : (sortMode == SortMode.NAME_DESC) ? SortMode.NONE : SortMode.NAME_ASC;
                sortMode = newMode;
                if (newMode != SortMode.NONE) {
                    Comparator<IgnoredRow> cmp = (newMode == SortMode.NAME_ASC)
                        ? Comparator.comparing((IgnoredRow r) -> r.name, String.CASE_INSENSITIVE_ORDER)
                        : Comparator.comparing((IgnoredRow r) -> r.name, String.CASE_INSENSITIVE_ORDER).reversed();
                    rows.sort(cmp);
                }
                scrollOffset = 0;
                initGui();
                return true;
            }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 表头点击排序 - 总计
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 琛ㄥご鐐瑰嚮鎺掑簭 - 鎬昏
//#endif
            int totalHeaderX = 200;
            if (mouseY >= 30 && mouseY <= 46 && mouseX >= totalHeaderX - 10 && mouseX <= totalHeaderX + 50) {
                SortMode newMode = (sortMode == SortMode.TOTAL_DESC) ? SortMode.TOTAL_ASC
                        : (sortMode == SortMode.TOTAL_ASC) ? SortMode.NONE : SortMode.TOTAL_DESC;
                sortMode = newMode;
                if (newMode != SortMode.NONE) {
                    Comparator<IgnoredRow> cmp = (newMode == SortMode.TOTAL_DESC)
                        ? Comparator.comparingInt((IgnoredRow r) -> r.totalCount).reversed()
                        : Comparator.comparingInt(r -> r.totalCount);
                    rows.sort(cmp);
                }
                scrollOffset = 0;
                initGui();
                return true;
            }

            for (int i = scrollOffset; i < display.size() && count < visibleRows; i++) {
                IgnoredRow row = display.get(i);
                int rowY = cumulativeY;
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                int expandX = this.width - 70 - EXPAND_ICON_SIZE / 2;
//#endif
                if (row.sources.size() > 1 && mouseX >= expandX && mouseX <= expandX + EXPAND_ICON_SIZE
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                    row.expanded = !row.expanded;
                    initGui();
                    return true;
                }
                cumulativeY += row.getRowHeight();
                count++;
            }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 拖拽重排
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 鎷栨嫿閲嶆帓
//#endif
            if (Configs.Generic.ALLOW_MANUAL_REORDER.getBooleanValue()) {
                List<IgnoredRow> dragDisplay = getSortedRows();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                int row = getRowAtY(mouseY, dragDisplay);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                int row = getRowAtY((int)mouseY, dragDisplay);
//#endif
                if (row >= 0 && row < dragDisplay.size() && mouseX >= 10 && mouseX < this.width - 80) {
                    draggedRow = row;
//#if MC == 12110 || MC == 12111
                    dragStartY = mouseY;
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                    dragStartY = (int)mouseY;
//#endif
                    isDragging = false;
                    return true;
                }
            }
        }
        return false;
    }

    private int getRowAtY(int mouseY, List<IgnoredRow> display) {
        if (mouseY < LIST_TOP) return -1;
        int cumulativeY = LIST_TOP;
        int count = 0;
        for (int i = scrollOffset; i < display.size() && count < visibleRows; i++) {
            int rowH = display.get(i).getRowHeight();
            if (mouseY >= cumulativeY && mouseY < cumulativeY + rowH) return i;
            cumulativeY += rowH;
            count++;
        }
        return -1;
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
        if (draggedRow >= 0 && button == 0 && Configs.Generic.ALLOW_MANUAL_REORDER.getBooleanValue()) {
            List<IgnoredRow> display = getSortedRows();
//#if MC == 12110 || MC == 12111
            int currentRow = getRowAtY(mouseY, display);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            int currentRow = getRowAtY((int)mouseY, display);
//#endif
            if (currentRow >= 0 && currentRow < display.size() && currentRow != draggedRow && Math.abs(mouseY - dragStartY) > 5) {
                isDragging = true;
                IgnoredRow draggedItem = display.get(draggedRow);
                IgnoredRow targetItem = display.get(currentRow);
                int actualDraggedIdx = rows.indexOf(draggedItem);
                int actualTargetIdx = rows.indexOf(targetItem);
                if (actualDraggedIdx >= 0 && actualTargetIdx >= 0) {
                    Collections.swap(rows, actualDraggedIdx, actualTargetIdx);
                    sortMode = SortMode.NONE;
                    initGui();
                }
                draggedRow = currentRow;
//#if MC == 12110 || MC == 12111
                dragStartY = mouseY;
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                dragStartY = (int)mouseY;
//#endif
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

    @Override
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
//#endif
        if (mouseY >= LIST_TOP && mouseY <= LIST_TOP + visibleRows * ROW_HEIGHT) {
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
                if (searchField != null) searchField.setText("");
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
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$
//$$
//$$
//#endif