package com.litematlist.gui;

import com.litematlist.I18n;
import com.litematlist.PinyinSearch;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 方块选择器 —— 参照投影的方块选择界面。
 * 可滚动图标网格 + 搜索框 + ESC 退出。
 */
public class BlockPickerScreen extends GuiBase {

    private final GuiBase parent;
    private final Consumer<Item> callback;
    private final List<Item> allBlocks;
    private final Set<Item> allowedItems;
    private List<Item> filteredBlocks;
    private int scrollOffset = 0;
    private int columns;
    private int visibleRows;
    private static final int CELL_SIZE = 22;
    private static final int GRID_TOP = 50;
    private static final int GRID_LEFT = 12;
    private TextFieldWidget searchField;
    private String searchText = "";
    private int hoveredIdx = -1;

    public BlockPickerScreen(GuiBase parent, Consumer<Item> callback) {
        this(parent, callback, null);
    }

    public BlockPickerScreen(GuiBase parent, Consumer<Item> callback, Set<Item> allowedItems) {
        super();
        this.parent = parent;
        this.callback = callback;
        this.allowedItems = allowedItems;
        this.allBlocks = buildBlockList(allowedItems);
        this.filteredBlocks = new ArrayList<>(allBlocks);
        this.title = I18n.tr("litematlist.title.block_picker");
    }

    private static List<Item> buildBlockList(Set<Item> allowedItems) {
        List<Item> list = new ArrayList<>();
        for (Item item : Registries.ITEM) {
            if (item != Items.AIR) {
                if (allowedItems == null || allowedItems.contains(item)) {
                    list.add(item);
                }
            }
        }
        // 按创造模式物品栏顺序排序（注册顺序）
        list.sort(Comparator.comparingInt(item -> Registries.ITEM.getRawId(item)));
        return list;
    }

    @Override
    public void initGui() {
        super.initGui();

        // 搜索框
        this.searchField = new TextFieldWidget(
                this.textRenderer, 12, 24, this.width - 24, 20, Text.empty());
        this.searchField.setMaxLength(50);
        this.searchField.setText(searchText);
        this.searchField.setChangedListener(this::onSearchChanged);
        this.addSelectableChild(this.searchField);

        // 网格参数
        this.columns = Math.max(1, (this.width - 24) / CELL_SIZE);
        int gridHeight = this.height - GRID_TOP - 44;
        this.visibleRows = Math.max(1, gridHeight / CELL_SIZE);

        // 底部按钮
        int buttonY = this.height - 38;
        ButtonGeneric cancelBtn = new ButtonGeneric(
                this.width / 2 - 60, buttonY, 120, 20, "取消");
        cancelBtn.setRenderDefaultBackground(true);
        this.addButton(cancelBtn, (IButtonActionListener) (b, mb) ->
                MinecraftClient.getInstance().setScreen(parent));
    }

    private void onSearchChanged(String text) {
        this.searchText = text;
        if (text.isEmpty()) {
            this.filteredBlocks = new ArrayList<>(allBlocks);
        } else {
            // 搜索过滤（中文名/拼音全拼/拼音首字母/英文ID）
            this.filteredBlocks = allBlocks.stream()
                    .filter(item -> PinyinSearch.matches(text, item.getName().getString(),
                            Registries.ITEM.getId(item).toString()))
                    .collect(Collectors.toList());
        }
        this.scrollOffset = 0;
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(drawContext, mouseX, mouseY, partialTicks);
        super.render(drawContext, mouseX, mouseY, partialTicks);

        // 标题
        drawContext.drawCenteredTextWithShadow(
                this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFFFF);

        // 搜索框
        this.searchField.render(drawContext, mouseX, mouseY, partialTicks);

        // 搜索提示
        if (this.searchField.isFocused() && this.searchField.getText().isEmpty()) {
            drawContext.drawTextWithShadow(this.textRenderer,
                    "搜索方块...", 16, 29, 0x80FFFFFF);
        }

        // 网格背景
        int gridHeight = visibleRows * CELL_SIZE;
        drawContext.fill(GRID_LEFT - 2, GRID_TOP - 2,
                GRID_LEFT + columns * CELL_SIZE + 2, GRID_TOP + gridHeight + 2, 0x40000000);

        int totalCells = columns * visibleRows;
        int maxIdx = Math.min(filteredBlocks.size(), scrollOffset + totalCells);

        hoveredIdx = -1;

        for (int i = scrollOffset; i < maxIdx; i++) {
            int localIdx = i - scrollOffset;
            int col = localIdx % columns;
            int row = localIdx / columns;
            int cx = GRID_LEFT + col * CELL_SIZE;
            int cy = GRID_TOP + row * CELL_SIZE;

            Item item = filteredBlocks.get(i);

            // 悬停高亮
            if (mouseX >= cx && mouseX < cx + CELL_SIZE
                    && mouseY >= cy && mouseY < cy + CELL_SIZE) {
                hoveredIdx = i;
                drawContext.fill(cx, cy, cx + CELL_SIZE, cy + CELL_SIZE, 0x60FFFFFF);
            }

            // 方块图标
            drawContext.drawItem(new ItemStack(item), cx + 3, cy + 3);
        }

        // 滚动信息
        if (filteredBlocks.size() > totalCells) {
            String scrollInfo = "共 " + filteredBlocks.size() + " 个方块";
            drawContext.drawCenteredTextWithShadow(this.textRenderer,
                    scrollInfo, this.width / 2, GRID_TOP + gridHeight + 5, 0xFFFFFFFF);
        }

        // 悬停提示
        if (hoveredIdx >= 0 && hoveredIdx < filteredBlocks.size()) {
            Item item = filteredBlocks.get(hoveredIdx);
            drawContext.drawTooltip(this.textRenderer,
                    item.getName(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (super.mouseClicked(mouseX, mouseY, mouseButton)) return true;

        if (mouseButton == 0) {
            if (hoveredIdx >= 0 && hoveredIdx < filteredBlocks.size()) {
                callback.accept(filteredBlocks.get(hoveredIdx));
                MinecraftClient.getInstance().setScreen(parent);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount,
                                  double verticalAmount) {
        int totalCells = columns * visibleRows;
        if (mouseY >= GRID_TOP && mouseY <= GRID_TOP + visibleRows * CELL_SIZE) {
            scrollOffset = Math.max(0, scrollOffset - (int) verticalAmount * columns);
            int maxOffset = Math.max(0, filteredBlocks.size() - totalCells);
            scrollOffset = Math.min(scrollOffset, maxOffset);
            if (scrollOffset % columns != 0) {
                scrollOffset = (scrollOffset / columns) * columns;
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC 退出
        if (keyCode == 256) {
            MinecraftClient.getInstance().setScreen(parent);
            return true;
        }
        if (this.searchField.isFocused()) {
            return this.searchField.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.searchField.isFocused()) {
            return this.searchField.charTyped(chr, modifiers);
        }
        return super.charTyped(chr, modifiers);
    }
}
