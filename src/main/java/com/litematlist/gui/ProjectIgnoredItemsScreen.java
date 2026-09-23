package com.litematlist.gui;

import com.litematlist.I18n;
import com.litematlist.LitematicReader;
import com.litematlist.MaterialListImporter;
import com.litematlist.config.Configs;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.file.Path;
import java.util.*;

/**
 * 椤圭洰鎬绘潗鏂欏垪琛ㄧ殑宸插拷鐣ョ墿鍝佺晫闈€? * 鏀寔澶氭潵婧愬悎骞跺睍寮€锛屽彇娑堝拷鐣ユ椂鍚屾鍒版墍鏈夊瓙鍒楄〃銆? */
public class ProjectIgnoredItemsScreen extends GuiBase {

    private final GuiBase parent;
    private final String projectName;
    private final Set<Item> ignoredSet;
    private final Map<Item, Integer> ignoredTotals;
    private final List<MaterialListScreen.LoadedEntry> projectEntries;
    private final List<IgnoredRow> rows = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int ROW_HEIGHT = 22;
    private static final int LIST_TOP = 48;
    private int visibleRows;
    private int hoveredRow = -1;
    private int hoveredSubCount = -1; // 鎮仠瀛愰」鐨勬暟閲忥紝鐢ㄤ簬tooltip

    private static final Identifier SEARCH_ICON = Identifier.of("litematlist", "textures/gui/search.png");
    private boolean searchActive = false;
    private String searchText = "";
    private TextFieldWidget searchField;

    private enum SortMode { NONE(""), TOTAL_DESC(""), TOTAL_ASC(""), NAME_ASC(""), NAME_DESC("");
        final String arrow; SortMode(String a) { this.arrow = a; }
    }
    private SortMode sortMode = SortMode.NONE;

    // 鎷栨嫿閲嶆帓
    private int draggedRow = -1;
    private int dragStartY = 0;
    private boolean isDragging = false;

    private static final Identifier EXPAND_NORMAL = Identifier.of("litematlist", "textures/gui/expand/expand_normal.png");
    private static final Identifier EXPAND_HOVERED = Identifier.of("litematlist", "textures/gui/expand/expand_hovered.png");
    private static final Identifier COLLAPSE_NORMAL = Identifier.of("litematlist", "textures/gui/expand/collapse_normal.png");
    private static final Identifier COLLAPSE_HOVERED = Identifier.of("litematlist", "textures/gui/expand/collapse_hovered.png");
    private static final int EXPAND_ICON_SIZE = 16;

    private static class SourceEntry {
        final String listName;
        final int count;
        final Item item;
        final Item originalItem; // 鏇挎崲鍓嶇殑鍘熷鐗╁搧
        final Path filePath;
        SourceEntry(String listName, int count, Item item, Item originalItem, Path filePath) {
            this.listName = listName; this.count = count; this.item = item;
            this.originalItem = originalItem; this.filePath = filePath;
        }
    }

    private static class IgnoredRow {
        final ItemStack itemStack;
        final String name;
        final int totalCount;
        final Item item;
        final List<SourceEntry> sources;
        boolean expanded;

        IgnoredRow(ItemStack is, String name, int total, Item item, List<SourceEntry> sources) {
            this.itemStack = is; this.name = name; this.totalCount = total;
            this.item = item; this.sources = sources; this.expanded = false;
        }
        int getRowHeight() { return ROW_HEIGHT + (expanded ? sources.size() * ROW_HEIGHT : 0); }
    }

    public ProjectIgnoredItemsScreen(GuiBase parent, String projectName, Set<Item> ignoredSet,
                                      Map<Item, Integer> ignoredTotals, List<MaterialListScreen.LoadedEntry> projectEntries) {
        super();
        this.parent = parent;
        this.projectName = projectName;
        this.ignoredSet = ignoredSet;
        this.ignoredTotals = ignoredTotals != null ? ignoredTotals : new HashMap<>();
        this.projectEntries = projectEntries;
        this.title = I18n.tr("litematlist.title.ignored_items", projectName);
        buildRows();
    }

    private void buildRows() {
        rows.clear();
        Map<Item, List<SourceEntry>> sourceMap = new HashMap<>();
        Map<Item, String> itemNames = new HashMap<>();
        Map<Item, Integer> totalCounts = new HashMap<>();

        for (MaterialListScreen.LoadedEntry entry : projectEntries) {
            Path fp = entry.filePath();
            if (fp == null) continue;
            Set<Item> subIgnored = MaterialDetailScreen.getIgnoredForPath(fp);
            if (subIgnored == null || subIgnored.isEmpty()) continue;
            String listName = entry.name();
            if (MaterialListScreen.isTxtEntry(entry)) listName += " (txt)";
            else if (MaterialListScreen.isJsonEntry(entry)) listName += " (JSON)";

            // 鑾峰彇璇ユ枃浠剁殑瀹為檯鏉愭枡鏁版嵁浠ヨ幏鍙栨瘡涓墿鍝佺殑鏁伴噺
            Map<Item, Integer> itemCounts = new HashMap<>();
            try {
                List<LitematicReader.MaterialEntry> entries;
                if (MaterialDetailScreen.getImportedMaterials().containsKey(fp)) {
                    entries = MaterialDetailScreen.getImportedMaterials().get(fp);
                } else if (fp.toString().toLowerCase().endsWith(".txt") || fp.toString().toLowerCase().endsWith(".json") || fp.toString().toLowerCase().endsWith(".csv")) {
                    MaterialListImporter.ImportResult result = MaterialListImporter.importFromFile(fp);
                    if (result != null) entries = result.materials();
                    else entries = Collections.emptyList();
                } else {
                    entries = LitematicReader.loadMaterialList(fp);
                }
                for (LitematicReader.MaterialEntry me : entries) {
                    Item originalItem = me.item();
                    Item actualItem = MaterialDetailScreen.getReplacement(fp, originalItem);
                    itemCounts.put(actualItem, me.totalCount());
                }
            } catch (Exception ignored) {}

            for (Item item : subIgnored) {
                int count = itemCounts.getOrDefault(item, 0);
                sourceMap.computeIfAbsent(item, k -> new ArrayList<>())
                        .add(new SourceEntry(listName, count, item, item, fp));
                itemNames.putIfAbsent(item, item.getName().getString());
                totalCounts.merge(item, count, Integer::sum);
            }
        }

        for (var entry : sourceMap.entrySet()) {
            Item item = entry.getKey();
            List<SourceEntry> sources = entry.getValue();
            int total = totalCounts.getOrDefault(item, 0);
            rows.add(new IgnoredRow(new ItemStack(item, total), itemNames.get(item), total, item, sources));
        }
        // 鍒濆鎸夋€昏鏁伴噺浠庨珮鍒颁綆鎺掑垪
        rows.sort(Comparator.comparingInt((IgnoredRow r) -> r.totalCount).reversed());
    }

    private void unignoreFromAllSubLists(Item item) {
        for (MaterialListScreen.LoadedEntry entry : projectEntries) {
            Path fp = entry.filePath();
            if (fp == null) continue;
            Set<Item> subIgnored = MaterialDetailScreen.getIgnoredForPath(fp);
            if (subIgnored != null) subIgnored.remove(item);
        }
        MaterialDetailScreen.savePersistence();
    }

    private void unignoreSource(SourceEntry src) {
        Set<Item> subIgnored = MaterialDetailScreen.getIgnoredForPath(src.filePath);
        if (subIgnored != null) subIgnored.remove(src.item);
        MaterialDetailScreen.savePersistence();
        MaterialListHudRenderer.refreshHud(); // 鍙栨秷蹇界暐鍚庢洿鏂癏UD
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
                unignoreFromAllSubLists(row.item);
            }
            ignoredSet.clear();
            rows.clear();
            initGui();
        });

        // 鎼滅储妗?
        int searchIconX = 10;
        if (searchField == null) {
            this.searchField = new TextFieldWidget(this.textRenderer, searchIconX + 18, 32, 80, 14, Text.empty());
            this.searchField.setMaxLength(50);
            this.searchField.setDrawsBackground(false);
            this.searchField.setText(searchText);
            this.addSelectableChild(this.searchField);
        } else {
            this.searchField.setX(searchIconX + 18);
            this.searchField.setY(32);
            this.searchField.setText(searchText);
            this.addSelectableChild(this.searchField);
        }

        createRowButtons();
    }

    private List<IgnoredRow> getSortedRows() {
        List<IgnoredRow> sorted = new ArrayList<>(rows);
        // 鎼滅储杩囨护
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
                // 澶氭潵婧愶細灞曞紑鏃舵樉绀哄瓙椤圭殑鍙栨秷蹇界暐鎸夐挳
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
                    unignoreFromAllSubLists(r.item);
                    ignoredSet.remove(r.item);
                    rows.remove(idx);
                    MaterialListHudRenderer.refreshHud(); // 鍙栨秷蹇界暐鍚庢洿鏂癏UD
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
    public void render(DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(drawContext, mouseX, mouseY, partialTicks);
        drawContext.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFFFF);

        String countText = I18n.tr("litematlist.ignored.count", rows.size());
        drawContext.drawCenteredTextWithShadow(this.textRenderer, countText, this.width / 2, 24, 0xFFAAAAAA);

        if (rows.isEmpty()) {
            drawContext.drawCenteredTextWithShadow(this.textRenderer, I18n.tr("litematlist.ignored.empty"),
                    this.width / 2, this.height / 2 - 10, 0xFFFFFFFF);
            super.render(drawContext, mouseX, mouseY, partialTicks);
            return;
        }

        drawContext.fill(8, 30, this.width - 8, 46, 0x60000000);
        List<IgnoredRow> display = getSortedRows();
        int listRight = this.width - 8;
        int cumulativeY = LIST_TOP;
        int count = 0;
        for (int i = scrollOffset; i < display.size() && count < visibleRows; i++) {
            cumulativeY += display.get(i).getRowHeight();
            count++;
        }
        int listBottom = cumulativeY;
        drawContext.fill(8, LIST_TOP - 2, listRight, listBottom + 2, 0x40000000);

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
                drawContext.fill(10, rowY, listRight, rowY + ROW_HEIGHT, 0x40FFFFFF);
            } else if ((i - scrollOffset) % 2 == 0) {
                drawContext.fill(10, rowY, listRight, rowY + ROW_HEIGHT, 0x10FFFFFF);
            }

            // 瀛愰」鐙珛楂樹寒
            if (row.expanded) {
                for (int j = 0; j < row.sources.size(); j++) {
                    int subY = rowY + ROW_HEIGHT + j * ROW_HEIGHT;
                    if (mouseX >= 10 && mouseX <= listRight && mouseY >= subY && mouseY < subY + ROW_HEIGHT) {
                        drawContext.fill(10, subY, listRight, subY + ROW_HEIGHT, 0x30FFFFFF);
                    }
                }
            }

            cumulativeY += rowH;
            count++;
        }

        super.render(drawContext, mouseX, mouseY, partialTicks);

        // 鎼滅储鍥炬爣锛堝乏缃紝涓嶉伄鎸＄墿鍝佹枃瀛楋級
        drawContext.drawTexture(SEARCH_ICON, 10, 33, 0.0f, 0.0f, 14, 14, 14, 14);
        //  if (searchActive && searchField != null) {
        //     drawContext.fill(28, 31, 28 + 82, 46, 0xFF000000);
        //     drawContext.fill(29, 32, 29 + 80, 45, 0xFF222222);
        //
     String st = searchField.getText();
        //     drawContext.drawText(this.textRenderer, st, 31, 34, 0xFFFFFFFF, false);
        //     if (searchField.isFocused() && System.currentTimeMillis() % 1000 < 500) {
        //
         int cp = searchField.getCursor();
        //
         String bc = st.substring(0, Math.min(cp, st.length()));
        //
         int cx = 31 + this.textRenderer.getWidth(bc);
        //         drawContext.fill(cx, 33, cx + 1, 43, 0xFFFFFFFF);
        //     }
        // }
        // 鐗╁搧鍒楄〃澶?
        String nameHeader = I18n.tr("litematlist.header.item") + (sortMode == SortMode.NAME_ASC || sortMode == SortMode.NAME_DESC ? sortMode.arrow : "");
        drawContext.drawTextWithShadow(this.textRenderer, nameHeader, searchActive ? 115 : 35, 33, 0xFFAAAAAA);
        drawContext.drawTextWithShadow(this.textRenderer,
                I18n.tr("litematlist.header.total") + (sortMode == SortMode.TOTAL_DESC || sortMode == SortMode.TOTAL_ASC ? sortMode.arrow : ""),
                200, 33, 0xFFAAAAAA);

        cumulativeY = LIST_TOP;
        count = 0;
        int maxIdx = Math.min(display.size(), scrollOffset + visibleRows);

        for (int i = scrollOffset; i < maxIdx; i++) {
            IgnoredRow row = display.get(i);
            int rowY = cumulativeY;

            drawContext.drawItem(row.itemStack, 15, rowY + 2);
            drawContext.drawTextWithShadow(this.textRenderer, row.name, 35, rowY + 5, 0xFFFFFFFF);
            drawContext.drawTextWithShadow(this.textRenderer, String.valueOf(row.totalCount), 200, rowY + 5, 0xFFFFFFFF);

            // 灞曞紑/鎶樺彔鍥炬爣锛堝眳涓簬鏅€氶潪瀛愰」"鍙栨秷蹇界暐"鎸夐挳鐨勪腑绾匡級
            if (row.sources.size() > 1) {
                //  this.width - 120, width = 100, 涓嚎 = this.width - 70
                int expandX = this.width - 70 - EXPAND_ICON_SIZE / 2;
                boolean expHovered = mouseX >= expandX && mouseX <= expandX + EXPAND_ICON_SIZE
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
                Identifier expIcon = row.expanded ? (expHovered ? COLLAPSE_HOVERED : COLLAPSE_NORMAL)
                        : (expHovered ? EXPAND_HOVERED : EXPAND_NORMAL);
                drawContext.drawTexture(expIcon, expandX, rowY + 2, 0.0f, 0.0f, EXPAND_ICON_SIZE, EXPAND_ICON_SIZE, EXPAND_ICON_SIZE, EXPAND_ICON_SIZE);
            }

            //  if (row.expanded) {
            //     for (int j = 0; j < row.sources.size(); j++) {
            //         SourceEntry src = row.sources.get(j);
            //
        // int subY = rowY + ROW_HEIGHT + j * ROW_HEIGHT;
            //         drawContext.fill(10, subY, listRight, subY + ROW_HEIGHT, (j % 2 == 0) ? 0x18FFFFFF : 0x08FFFFFF);
            //         drawContext.drawItem(new ItemStack(src.item), 30, subY + 2);
            //
        // String subText = " - " + src.item.getName().getString() + " " + src.count + "  (" + src.listName + ")";
            //         drawContext.drawTextWithShadow(this.textRenderer, subText, 48, subY + 5, 0xCCCCCCCC);
            //         if (mouseX >= 10 && mouseX <= listRight && mouseY >= subY && mouseY < subY + ROW_HEIGHT) {
            //             hoveredSubCount = src.count;
            //         }
            //     }
            // }

            cumulativeY += row.getRowHeight();
            count++;
            if (count >= visibleRows) break;
        }

        if (hoveredRow >= 0 && hoveredRow < display.size()) {
            IgnoredRow row = display.get(hoveredRow);
            drawContext.drawTooltip(this.textRenderer, Text.literal(formatCountTooltip(row.totalCount)), mouseX, mouseY);
        }

        if (hoveredSubCount >= 0) {
            drawContext.drawTooltip(this.textRenderer, Text.literal(formatCountTooltip(hoveredSubCount)), mouseX, mouseY);
        }

        if (display.size() > visibleRows) {
            int scrollInfoY = LIST_TOP + visibleRows * ROW_HEIGHT + 5;
            String scrollInfo = (scrollOffset + 1) + "-" + Math.min(scrollOffset + visibleRows, display.size()) + " / " + display.size();
            drawContext.drawCenteredTextWithShadow(this.textRenderer, scrollInfo, this.width / 2, scrollInfoY, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (super.mouseClicked(mouseX, mouseY, mouseButton)) return true;
        if (mouseButton == 0) {
            List<IgnoredRow> display = getSortedRows();
            int cumulativeY = LIST_TOP;
            int count = 0;

            // 鎼滅储鍥炬爣鐐瑰嚮
            if (mouseX >= 10 && mouseX <= 24 && mouseY >= 30 && mouseY <= 46) {
                searchActive = !searchActive;
                if (searchActive && searchField != null) searchField.setFocused(true);
                if (!searchActive) { searchText = ""; if (searchField != null) searchField.setText(""); }
                scrollOffset = 0;
                initGui();
                return true;
            }

            // 琛ㄥご鐐瑰嚮鎺掑簭 - 鐗╁搧鍚嶇О
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

            // 琛ㄥご鐐瑰嚮鎺掑簭 - 鎬昏
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
                int expandX = this.width - 70 - EXPAND_ICON_SIZE / 2;
                if (row.sources.size() > 1 && mouseX >= expandX && mouseX <= expandX + EXPAND_ICON_SIZE
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                    row.expanded = !row.expanded;
                    initGui();
                    return true;
                }
                cumulativeY += row.getRowHeight();
                count++;
            }
            // 鎷栨嫿閲嶆帓
            if (Configs.Generic.ALLOW_MANUAL_REORDER.getBooleanValue()) {
                List<IgnoredRow> dragDisplay = getSortedRows();
                int row = getRowAtY((int)mouseY, dragDisplay);
                if (row >= 0 && row < dragDisplay.size() && mouseX >= 10 && mouseX < this.width - 80) {
                    draggedRow = row;
                    dragStartY = (int)mouseY;
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
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
        mouseY = (int) mouseY;
        int button = mouseButton;
        if (draggedRow >= 0 && button == 0 && Configs.Generic.ALLOW_MANUAL_REORDER.getBooleanValue()) {
            List<IgnoredRow> display = getSortedRows();
            int currentRow = getRowAtY((int)mouseY, display);
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
                dragStartY = (int)mouseY;
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, mouseButton, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (draggedRow >= 0) {
            draggedRow = -1;
            isDragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseY >= LIST_TOP && mouseY <= LIST_TOP + visibleRows * ROW_HEIGHT) {
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

