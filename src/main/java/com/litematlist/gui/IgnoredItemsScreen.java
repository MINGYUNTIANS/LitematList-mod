package com.litematlist.gui;

import com.litematlist.I18n;
import com.litematlist.config.Configs;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gl.RenderPipelines;


import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.file.Path;
import java.util.*;

/**
 * 已忽略物品列表界面。
 * 支持搜索筛选、按名称/总计排序、拖拽重排。
 */
public class IgnoredItemsScreen extends GuiBase {

    private final GuiBase parent;
    private final String schematicName;
    private final Path filePath;
    private final String pathKey;
    private final Set<Item> ignoredSet;
    private final Map<Item, Integer> ignoredTotals;
    private final List<Item> ignoredList;
    private int scrollOffset = 0;
    private static final int ROW_HEIGHT = 22;
    private static final int LIST_TOP = 48;
    private int visibleRows;
    private int hoveredRow = -1;

    // 搜索
    private static final Identifier SEARCH_ICON = Identifier.of("litematlist", "textures/gui/search.png");
    private boolean searchActive = false;
    private String searchText = "";
    private TextFieldWidget searchField;

    // 排序
    private enum SortMode { NONE(""), TOTAL_DESC("▼"), TOTAL_ASC("▲"), NAME_ASC("▲"), NAME_DESC("▼");
        final String arrow; SortMode(String a) { this.arrow = a; }
    }
    private SortMode sortMode = SortMode.NONE;

    // 拖拽重排
    private int draggedRow = -1;
    private int dragStartY = 0;
    private boolean isDragging = false;

    public IgnoredItemsScreen(GuiBase parent, String schematicName, Path filePath, String pathKey, Set<Item> ignoredSet, Map<Item, Integer> ignoredTotals) {
        super();
        this.parent = parent;
        this.schematicName = schematicName;
        this.filePath = filePath;
        this.pathKey = pathKey;
        this.ignoredSet = ignoredSet;
        this.ignoredTotals = ignoredTotals != null ? ignoredTotals : new HashMap<>();
        this.ignoredList = new ArrayList<>(ignoredSet);
        // 初始按总计数量从高到低排列
        ignoredList.sort(Comparator.comparingInt((Item i) -> ignoredTotals.getOrDefault(i, 0)).reversed());
        this.title = I18n.tr("litematlist.title.ignored_items", schematicName);
    }

    private List<Item> getDisplayList() {
        List<Item> list = new ArrayList<>(ignoredList);
        if (searchActive && !searchText.isEmpty()) {
            list.removeIf(item -> !item.getName().getString().toLowerCase().contains(searchText.toLowerCase()));
        }
        return list;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.visibleRows = (this.height - 100) / ROW_HEIGHT;

        int buttonY = this.height - 38;

        ButtonGeneric backButton = new ButtonGeneric(
                this.width / 2 - 60, buttonY, 120, 20, I18n.tr("litematlist.button.back"));
        backButton.setRenderDefaultBackground(true);
        this.addButton(backButton, (IButtonActionListener) (button, mouseButton) ->
                MinecraftClient.getInstance().setScreen(parent));

        ButtonGeneric clearAllBtn = new ButtonGeneric(
                this.width / 2 + 70, buttonY, 100, 20, I18n.tr("litematlist.button.clear_all_ignored"));
        clearAllBtn.setRenderDefaultBackground(true);
        this.addButton(clearAllBtn, (IButtonActionListener) (b, mb) -> {
            ignoredSet.clear();
            ignoredList.clear();
            // 同时清除原始持久化数据
            java.util.Set<String> persisted = com.litematlist.gui.MaterialDetailScreen.rawMaterialIgnored.get(pathKey);
            if (persisted != null) {
                persisted.clear();
            }
            // 同时清除源级忽略
            java.util.Set<String> srcIgnored = com.litematlist.gui.MaterialDetailScreen.rawMaterialSourceIgnored.get(pathKey);
            if (srcIgnored != null) {
                srcIgnored.clear();
            }
            com.litematlist.gui.MaterialDetailScreen.savePersistence();
            // 清除缓存的原材料分析数据，下次打开时重新分析
            com.litematlist.gui.MaterialDetailScreen.rawMaterials.remove(pathKey);
            com.litematlist.gui.MaterialDetailScreen.rawMaterialRedundancy.remove(pathKey);
            // 通知原材料列表刷新
            if (parent instanceof RawMaterialScreen rawScreen) {
                rawScreen.rebuildRows();
            }
            initGui();
        });

        // 搜索框
        if (searchField == null) {
            this.searchField = new TextFieldWidget(this.textRenderer, 28, 32, 80, 14, Text.empty());
            this.searchField.setMaxLength(50);
            this.searchField.setDrawsBackground(false);
            this.searchField.setText(searchText);
            this.addSelectableChild(this.searchField);
        } else {
            this.searchField.setX(28);
            this.searchField.setY(32);
            this.searchField.setText(searchText);
            this.addSelectableChild(this.searchField);
        }

        createRowButtons();
    }

    private void createRowButtons() {
        List<Item> display = getDisplayList();
        int maxIdx = Math.min(display.size(), scrollOffset + visibleRows);

        for (int i = scrollOffset; i < maxIdx; i++) {
            Item item = display.get(i);
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;

            int btnX = this.width - 120;
            ButtonGeneric restoreBtn = new ButtonGeneric(btnX, rowY + 1, 100, 20, I18n.tr("litematlist.button.cancel_ignore"));
            restoreBtn.setRenderDefaultBackground(true);
            final Item targetItem = item;
            this.addButton(restoreBtn, (IButtonActionListener) (b, mb) -> {
                ignoredSet.remove(targetItem);
                ignoredList.remove(targetItem);
                // 同时从原始持久化数据中移除
                String itemId = Registries.ITEM.getId(targetItem).toString();
                java.util.Set<String> persisted = com.litematlist.gui.MaterialDetailScreen.rawMaterialIgnored.get(pathKey);
                if (persisted != null) {
                    persisted.remove(itemId);
                }
                // 同时清除该物品的源级忽略（仅清除该物品相关的）
                java.util.Set<String> srcIgnored = com.litematlist.gui.MaterialDetailScreen.rawMaterialSourceIgnored.get(pathKey);
                if (srcIgnored != null) {
                    List<com.litematlist.gui.MaterialDetailScreen.RawMaterialEntry> rawEntries =
                        com.litematlist.gui.MaterialDetailScreen.rawMaterials.get(pathKey);
                    if (rawEntries != null) {
                        for (com.litematlist.gui.MaterialDetailScreen.RawMaterialEntry entry : rawEntries) {
                            if (entry.itemId().equals(itemId)) {
                                srcIgnored.removeAll(entry.sources());
                            }
                        }
                    }
                }
                com.litematlist.gui.MaterialListHudRenderer.refreshHud(); // 取消忽略后更新HUD
                com.litematlist.gui.MaterialDetailScreen.savePersistence(); // 保存持久化
                // 清除缓存的原材料分析数据，下次打开时重新分析
                com.litematlist.gui.MaterialDetailScreen.rawMaterials.remove(pathKey);
                com.litematlist.gui.MaterialDetailScreen.rawMaterialRedundancy.remove(pathKey);
                // 通知原材料列表刷新
                if (parent instanceof RawMaterialScreen rawScreen) {
                    rawScreen.rebuildRows();
                }
                initGui();
            });
        }
    }

    private String formatCountTooltip(int total) {
        int shulker = total / 1728;
        int remainder = total % 1728;
        int stacks = remainder / 64;
        int items = remainder % 64;
        double shulkerFloat = (double) total / 1728.0;
        return String.format("%d = %d×1728 + %d×64 + %d  |  %d×64+%d  |  %.2f 潜影盒",
                total, shulker, stacks, items,
                shulker * 27 + stacks, items,
                shulkerFloat);
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(drawContext, mouseX, mouseY, partialTicks);

        drawContext.drawCenteredTextWithShadow(
                this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFFFF);

        String countText = I18n.tr("litematlist.ignored.count", ignoredList.size());
        drawContext.drawCenteredTextWithShadow(this.textRenderer,
                countText, this.width / 2, 24, 0xFFAAAAAA);

        if (ignoredList.isEmpty()) {
            drawContext.drawCenteredTextWithShadow(
                    this.textRenderer, I18n.tr("litematlist.ignored.empty"),
                    this.width / 2, this.height / 2 - 10, 0xFFFFFFFF);
            super.render(drawContext, mouseX, mouseY, partialTicks);
            return;
        }

        drawContext.fill(8, 30, this.width - 8, 46, 0x60000000);

        int listBottom = LIST_TOP + visibleRows * ROW_HEIGHT;
        drawContext.fill(8, LIST_TOP - 2, this.width - 8, listBottom + 2, 0x40000000);

        List<Item> display = getDisplayList();
        hoveredRow = -1;
        int maxIdx = Math.min(display.size(), scrollOffset + visibleRows);
        for (int i = scrollOffset; i < maxIdx; i++) {
            Item item = display.get(i);
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;

            if (mouseX >= 10 && mouseX <= this.width - 10
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                hoveredRow = i;
                drawContext.fill(10, rowY, this.width - 10, rowY + ROW_HEIGHT, 0x40FFFFFF);
            } else if ((i - scrollOffset) % 2 == 0) {
                drawContext.fill(10, rowY, this.width - 10, rowY + ROW_HEIGHT, 0x10FFFFFF);
            }
        }

        super.render(drawContext, mouseX, mouseY, partialTicks);

        // 搜索图标
        drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, SEARCH_ICON, 10, 33, 0.0f, 0.0f, 14, 14, 14, 14);
        if (searchActive && searchField != null) {
            drawContext.fill(28, 31, 28 + 82, 46, 0xFF000000);
            drawContext.fill(29, 32, 29 + 80, 45, 0xFF222222);
            String st = searchField.getText();
            drawContext.drawText(this.textRenderer, st, 31, 34, 0xFFFFFFFF, false);
            if (searchField.isFocused() && System.currentTimeMillis() % 1000 < 500) {
                int cp = searchField.getCursor();
                String bc = st.substring(0, Math.min(cp, st.length()));
                int cx = 31 + this.textRenderer.getWidth(bc);
                drawContext.fill(cx, 33, cx + 1, 43, 0xFFFFFFFF);
            }
        }

        // 表头
        String nameHeader = I18n.tr("litematlist.header.item") + (sortMode == SortMode.NAME_ASC || sortMode == SortMode.NAME_DESC ? sortMode.arrow : "");
        drawContext.drawTextWithShadow(this.textRenderer, nameHeader, searchActive ? 115 : 35, 33, 0xFFAAAAAA);
        drawContext.drawTextWithShadow(this.textRenderer,
                I18n.tr("litematlist.header.total") + (sortMode == SortMode.TOTAL_DESC || sortMode == SortMode.TOTAL_ASC ? sortMode.arrow : ""),
                200, 33, 0xFFAAAAAA);

        for (int i = scrollOffset; i < maxIdx; i++) {
            Item item = display.get(i);
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;
            int total = ignoredTotals.getOrDefault(item, 0);

            drawContext.drawItem(new ItemStack(item), searchActive ? 60 : 15, rowY + 2);
            drawContext.drawTextWithShadow(this.textRenderer,
                    item.getName().getString(), searchActive ? 78 : 35, rowY + 5, 0xFFFFFFFF);
            drawContext.drawTextWithShadow(this.textRenderer,
                    String.valueOf(total), 200, rowY + 5, 0xFFFFFFFF);
        }

        if (hoveredRow >= 0 && hoveredRow < display.size()) {
            Item item = display.get(hoveredRow);
            int total = ignoredTotals.getOrDefault(item, 0);
            drawContext.drawTooltip(this.textRenderer, Text.literal(formatCountTooltip(total)), mouseX, mouseY);
        }

        if (display.size() > visibleRows) {
            int scrollInfoY = LIST_TOP + visibleRows * ROW_HEIGHT + 5;
            String scrollInfo = (scrollOffset + 1) + "-" +
                    Math.min(scrollOffset + visibleRows, display.size()) +
                    " / " + display.size();
            drawContext.drawCenteredTextWithShadow(this.textRenderer,
                    scrollInfo, this.width / 2, scrollInfoY, 0xFFFFFFFF);
        }
    }

    private int getRowAtY(int mouseY) {
        if (mouseY < LIST_TOP) return -1;
        int row = (mouseY - LIST_TOP) / ROW_HEIGHT + scrollOffset;
        return row;
    }

    @Override
    public boolean onMouseClicked(int mouseX, int mouseY, int button) {
        if (super.onMouseClicked(mouseX, mouseY, button)) return true;

        
        
        

        if (button == 0) {
            // 搜索图标点击
            if (mouseX >= 10 && mouseX <= 24 && mouseY >= 30 && mouseY <= 46) {
                searchActive = !searchActive;
                if (searchActive && searchField != null) searchField.setFocused(true);
                if (!searchActive) { searchText = ""; if (searchField != null) searchField.setText(""); }
                scrollOffset = 0;
                initGui();
                return true;
            }

            // 表头点击排序 - 物品名称
            int nameHeaderX = searchActive ? 115 : 35;
            if (mouseY >= 30 && mouseY <= 46 && mouseX >= nameHeaderX - 5 && mouseX <= nameHeaderX + 60) {
                SortMode newMode = (sortMode == SortMode.NAME_ASC) ? SortMode.NAME_DESC
                        : (sortMode == SortMode.NAME_DESC) ? SortMode.NONE : SortMode.NAME_ASC;
                sortMode = newMode;
                if (newMode != SortMode.NONE) {
                    Comparator<Item> cmp = (newMode == SortMode.NAME_ASC)
                        ? Comparator.comparing((Item i) -> i.getName().getString(), String.CASE_INSENSITIVE_ORDER)
                        : Comparator.comparing((Item i) -> i.getName().getString(), String.CASE_INSENSITIVE_ORDER).reversed();
                    ignoredList.sort(cmp);
                }
                scrollOffset = 0;
                initGui();
                return true;
            }

            // 表头点击排序 - 总计
            if (mouseY >= 30 && mouseY <= 46 && mouseX >= 190 && mouseX <= 250) {
                SortMode newMode = (sortMode == SortMode.TOTAL_DESC) ? SortMode.TOTAL_ASC
                        : (sortMode == SortMode.TOTAL_ASC) ? SortMode.NONE : SortMode.TOTAL_DESC;
                sortMode = newMode;
                if (newMode != SortMode.NONE) {
                    Comparator<Item> cmp = (newMode == SortMode.TOTAL_DESC)
                        ? Comparator.comparingInt((Item i) -> ignoredTotals.getOrDefault(i, 0)).reversed()
                        : Comparator.comparingInt(i -> ignoredTotals.getOrDefault(i, 0));
                    ignoredList.sort(cmp);
                }
                scrollOffset = 0;
                initGui();
                return true;
            }

            // 拖拽检测
            if (Configs.Generic.ALLOW_MANUAL_REORDER.getBooleanValue()) {
                int row = getRowAtY((int)mouseY);
                List<Item> display = getDisplayList();
                if (row >= 0 && row < display.size() && mouseX >= 10 && mouseX < this.width - 130) {
                    draggedRow = row;
                    dragStartY = (int)mouseY;
                    isDragging = false;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        
        

        if (draggedRow >= 0 && button == 0 && Configs.Generic.ALLOW_MANUAL_REORDER.getBooleanValue()) {
            List<Item> display = getDisplayList();
            int currentRow = getRowAtY((int)mouseY);
            if (currentRow >= 0 && currentRow < display.size() && currentRow != draggedRow && Math.abs(mouseY - dragStartY) > 5) {
                isDragging = true;
                Item draggedItem = display.get(draggedRow);
                int actualIdx = ignoredList.indexOf(draggedItem);
                Item targetItem = display.get(currentRow);
                int targetIdx = ignoredList.indexOf(targetItem);
                if (actualIdx >= 0 && targetIdx >= 0) {
                    Collections.swap(ignoredList, actualIdx, targetIdx);
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
        if (draggedRow >= 0) {
            draggedRow = -1;
            isDragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseY >= LIST_TOP && mouseY <= LIST_TOP + visibleRows * ROW_HEIGHT) {
            List<Item> display = getDisplayList();
            scrollOffset = Math.max(0, scrollOffset - (int) verticalAmount);
            int maxOffset = Math.max(0, display.size() - visibleRows);
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




