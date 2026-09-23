package com.litematlist.gui;

import com.litematlist.I18n;
import com.litematlist.LitematicReader;
import com.litematlist.MaterialListImporter;
import com.litematlist.config.Configs;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import fi.dy.masa.malilib.render.GuiContext;
import org.apache.commons.lang3.tuple.Pair;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.GpuSampler;
import org.joml.Matrix3x2f;

import java.nio.file.Path;
import java.util.*;

/**
 * 项目总材料列表的已忽略物品界面。
 * 支持多来源合并展开，取消忽略时同步到所有子列表。
 */
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
    private int hoveredSubCount = -1;

    private static final Identifier SEARCH_ICON = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/search.png");
    private boolean searchActive = false;
    private String searchText = "";
    private EditBox searchField;

    private enum SortMode { NONE(""), TOTAL_DESC("▼"), TOTAL_ASC("▲"), NAME_ASC("▲"), NAME_DESC("▼");
        final String arrow; SortMode(String a) { this.arrow = a; }
    }
    private SortMode sortMode = SortMode.NONE;

    private int draggedRow = -1;
    private int dragStartY = 0;
    private boolean isDragging = false;

    private static final Identifier EXPAND_NORMAL = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/expand/expand_normal.png");
    private static final Identifier EXPAND_HOVERED = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/expand/expand_hovered.png");
    private static final Identifier COLLAPSE_NORMAL = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/expand/collapse_normal.png");
    private static final Identifier COLLAPSE_HOVERED = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/expand/collapse_hovered.png");
    private static final int EXPAND_ICON_SIZE = 16;

    private static class SourceEntry {
        final String listName;
        final int count;
        final Item item;
        final Item originalItem;
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
            if (MaterialListScreen.isTxtEntry(entry)) listName += "（txt）";
            else if (MaterialListScreen.isJsonEntry(entry)) listName += "（JSON）";

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
                itemNames.putIfAbsent(item, item.getName(new ItemStack(item)).getString());
                totalCounts.merge(item, count, Integer::sum);
            }
        }

        for (var entry : sourceMap.entrySet()) {
            Item item = entry.getKey();
            List<SourceEntry> sources = entry.getValue();
            int total = totalCounts.getOrDefault(item, 0);
            rows.add(new IgnoredRow(new ItemStack(item, total), itemNames.get(item), total, item, sources));
        }
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
                Minecraft.getInstance().setScreenAndShow(parent));

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

        int searchIconX = 10;
        if (searchField == null) {
            this.searchField = new EditBox(this.font, searchIconX + 18, 32, 80, 14, Component.empty());
            this.searchField.setMaxLength(50);
            this.searchField.setBordered(false);
            this.searchField.setValue(searchText);
            this.addRenderableWidget(this.searchField);
        } else {
            this.searchField.setX(searchIconX + 18);
            this.searchField.setY(32);
            this.searchField.setValue(searchText);
            this.addRenderableWidget(this.searchField);
        }

        createRowButtons();
    }

    private List<IgnoredRow> getSortedRows() {
        List<IgnoredRow> sorted = new ArrayList<>(rows);
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
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks) {
        GuiContext ctx = GuiContext.fromGuiGraphics(gfx);
        this.extractBackground(gfx, mouseX, mouseY, partialTicks);
        ctx.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);

        String countText = I18n.tr("litematlist.ignored.count", rows.size());
        ctx.drawCenteredString(this.font, countText, this.width / 2, 24, 0xFFAAAAAA);

        if (rows.isEmpty()) {
            ctx.drawCenteredString(this.font, I18n.tr("litematlist.ignored.empty"),
                    this.width / 2, this.height / 2 - 10, 0xFFFFFFFF);
            super.extractRenderState(gfx, mouseX, mouseY, partialTicks);
            return;
        }

        ctx.fill(8, 30, this.width - 8, 46, 0x60000000);
        List<IgnoredRow> display = getSortedRows();
        int listRight = this.width - 8;
        int cumulativeY = LIST_TOP;
        int count = 0;
        for (int i = scrollOffset; i < display.size() && count < visibleRows; i++) {
            cumulativeY += display.get(i).getRowHeight();
            count++;
        }
        int listBottom = cumulativeY;
        ctx.fill(8, LIST_TOP - 2, listRight, listBottom + 2, 0x40000000);

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
                ctx.fill(10, rowY, listRight, rowY + ROW_HEIGHT, 0x40FFFFFF);
            } else if ((i - scrollOffset) % 2 == 0) {
                ctx.fill(10, rowY, listRight, rowY + ROW_HEIGHT, 0x10FFFFFF);
            }

            if (row.expanded) {
                for (int j = 0; j < row.sources.size(); j++) {
                    int subY = rowY + ROW_HEIGHT + j * ROW_HEIGHT;
                    if (mouseX >= 10 && mouseX <= listRight && mouseY >= subY && mouseY < subY + ROW_HEIGHT) {
                        ctx.fill(10, subY, listRight, subY + ROW_HEIGHT, 0x30FFFFFF);
                    }
                }
            }

            cumulativeY += rowH;
            count++;
        }

        super.extractRenderState(gfx, mouseX, mouseY, partialTicks);

        // 搜索图标
        drawTextureIcon(ctx, SEARCH_ICON, 10, 33, 14, 14);
        if (searchActive && searchField != null) {
            ctx.fill(28, 31, 28 + 82, 46, 0xFF000000);
            ctx.fill(29, 32, 29 + 80, 45, 0xFF222222);
            String st = searchField.getValue();
            ctx.drawString(this.font, st, 31, 34, 0xFFFFFFFF);
            if (searchField.isFocused() && System.currentTimeMillis() % 1000 < 500) {
                int cp = searchField.getCursorPosition();
                String bc = st.substring(0, Math.min(cp, st.length()));
                int cx = 31 + this.font.width(bc);
                ctx.fill(cx, 33, cx + 1, 43, 0xFFFFFFFF);
            }
        }
        // 物品列表头
        String nameHeader = I18n.tr("litematlist.header.item") + (sortMode == SortMode.NAME_ASC || sortMode == SortMode.NAME_DESC ? sortMode.arrow : "");
        ctx.drawString(this.font, nameHeader, searchActive ? 115 : 35, 33, 0xFFAAAAAA);
        ctx.drawString(this.font,
                I18n.tr("litematlist.header.total") + (sortMode == SortMode.TOTAL_DESC || sortMode == SortMode.TOTAL_ASC ? sortMode.arrow : ""),
                200, 33, 0xFFAAAAAA);

        cumulativeY = LIST_TOP;
        count = 0;
        int maxIdx = Math.min(display.size(), scrollOffset + visibleRows);

        for (int i = scrollOffset; i < maxIdx; i++) {
            IgnoredRow row = display.get(i);
            int rowY = cumulativeY;

            ctx.renderItem(row.itemStack, 15, rowY + 2);
            ctx.drawString(this.font, row.name, 35, rowY + 5, 0xFFFFFFFF);
            ctx.drawString(this.font, String.valueOf(row.totalCount), 200, rowY + 5, 0xFFFFFFFF);

            if (row.sources.size() > 1) {
                int expandX = this.width - 70 - EXPAND_ICON_SIZE / 2;
                boolean expHovered = mouseX >= expandX && mouseX <= expandX + EXPAND_ICON_SIZE
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
                Identifier expIcon = row.expanded ? (expHovered ? COLLAPSE_HOVERED : COLLAPSE_NORMAL)
                        : (expHovered ? EXPAND_HOVERED : EXPAND_NORMAL);
                drawTextureIcon(ctx, expIcon, expandX, rowY + 2, EXPAND_ICON_SIZE, EXPAND_ICON_SIZE);
            }

            if (row.expanded) {
                for (int j = 0; j < row.sources.size(); j++) {
                    SourceEntry src = row.sources.get(j);
                    int subY = rowY + ROW_HEIGHT + j * ROW_HEIGHT;
                    ctx.fill(10, subY, listRight, subY + ROW_HEIGHT, (j % 2 == 0) ? 0x18FFFFFF : 0x08FFFFFF);
                    ctx.renderItem(new ItemStack(src.item), 30, subY + 2);
                    String subText = " - " + src.item.getName(new ItemStack(src.item)).getString() + " " + src.count + "  (" + src.listName + ")";
                    ctx.drawString(this.font, subText, 48, subY + 5, 0xCCCCCCCC);
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
            String tip1 = formatCountTooltip(row.totalCount);
            int tipW1 = this.font.width(tip1) + 8;
            int tipX1 = mouseX + 12;
            int tipY1 = mouseY - 20;
            if (tipX1 + tipW1 > this.width) tipX1 = this.width - tipW1 - 4;
            if (tipY1 < 4) tipY1 = mouseY + 16;
            ctx.fill(tipX1, tipY1, tipX1 + tipW1, tipY1 + 16, 0xCC000000);
            ctx.fill(tipX1 + 1, tipY1 + 1, tipX1 + tipW1 - 1, tipY1 + 15, 0xCC333333);
            ctx.drawString(this.font, tip1, tipX1 + 4, tipY1 + 4, 0xFFFFFFFF);
        }

        if (hoveredSubCount >= 0) {
            String tip2 = formatCountTooltip(hoveredSubCount);
            int tipW2 = this.font.width(tip2) + 8;
            int tipX2 = mouseX + 12;
            int tipY2 = mouseY - 20;
            if (tipX2 + tipW2 > this.width) tipX2 = this.width - tipW2 - 4;
            if (tipY2 < 4) tipY2 = mouseY + 16;
            ctx.fill(tipX2, tipY2, tipX2 + tipW2, tipY2 + 16, 0xCC000000);
            ctx.fill(tipX2 + 1, tipY2 + 1, tipX2 + tipW2 - 1, tipY2 + 15, 0xCC333333);
            ctx.drawString(this.font, tip2, tipX2 + 4, tipY2 + 4, 0xFFFFFFFF);
        }

        if (display.size() > visibleRows) {
            int scrollInfoY = LIST_TOP + visibleRows * ROW_HEIGHT + 5;
            String scrollInfo = (scrollOffset + 1) + "-" + Math.min(scrollOffset + visibleRows, display.size()) + " / " + display.size();
            ctx.drawCenteredString(this.font, scrollInfo, this.width / 2, scrollInfoY, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean onMouseClicked(MouseButtonEvent event, boolean isDrag) {
        if (super.onMouseClicked(event, isDrag)) return true;
        if (isDrag) return false;
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        if (event.button() == 0) {
            List<IgnoredRow> display = getSortedRows();
            int cumulativeY = LIST_TOP;
            int count = 0;
            int expandX = this.width - 70 - EXPAND_ICON_SIZE / 2;

            if (mouseX >= 10 && mouseX <= 24 && mouseY >= 30 && mouseY <= 46) {
                searchActive = !searchActive;
                if (searchActive && searchField != null) searchField.setFocused(true);
                if (!searchActive) { searchText = ""; if (searchField != null) searchField.setValue(""); }
                scrollOffset = 0;
                initGui();
                return true;
            }

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
                if (row.sources.size() > 1 && mouseX >= expandX && mouseX <= expandX + EXPAND_ICON_SIZE
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                    row.expanded = !row.expanded;
                    initGui();
                    return true;
                }
                cumulativeY += row.getRowHeight();
                count++;
            }
            if (Configs.Generic.ALLOW_MANUAL_REORDER.getBooleanValue()) {
                List<IgnoredRow> dragDisplay = getSortedRows();
                int row = getRowAtY(mouseY, dragDisplay);
                if (row >= 0 && row < dragDisplay.size() && mouseX >= 10 && mouseX < this.width - 80) {
                    draggedRow = row;
                    dragStartY = mouseY;
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
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        int mouseY = (int) event.y();
        int button = event.button();
        if (draggedRow >= 0 && button == 0 && Configs.Generic.ALLOW_MANUAL_REORDER.getBooleanValue()) {
            List<IgnoredRow> display = getSortedRows();
            int currentRow = getRowAtY(mouseY, display);
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
                dragStartY = mouseY;
            }
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggedRow >= 0) {
            draggedRow = -1;
            isDragging = false;
            return true;
        }
        return super.mouseReleased(event);
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