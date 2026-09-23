package com.litematlist.gui;

import com.litematlist.I18n;
import com.litematlist.PinyinSearch;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import fi.dy.masa.malilib.render.GuiContext;

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
    private EditBox searchField;
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
        for (Item item : BuiltInRegistries.ITEM) {
            if (item != Items.AIR) {
                if (allowedItems == null || allowedItems.contains(item)) {
                    list.add(item);
                }
            }
        }
        list.sort(Comparator.comparing(item -> item.getName(new ItemStack(item)).getString()));
        return list;
    }

    @Override
    public void initGui() {
        super.initGui();

        // 搜索框
        this.searchField = new EditBox(
                this.font, 12, 24, this.width - 24, 20, Component.empty());
        this.searchField.setMaxLength(50);
        this.searchField.setValue(searchText);
        this.searchField.setResponder(this::onSearchChanged);
        this.addRenderableWidget(this.searchField);

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
                Minecraft.getInstance().setScreenAndShow(parent));
    }

    private void onSearchChanged(String text) {
        this.searchText = text;
        if (text.isEmpty()) {
            this.filteredBlocks = new ArrayList<>(allBlocks);
        } else {
            // 搜索过滤（中文名/拼音全拼/拼音首字母/英文ID）
            this.filteredBlocks = allBlocks.stream()
                    .filter(item -> PinyinSearch.matches(text, item.getName(new ItemStack(item)).getString(),
                            BuiltInRegistries.ITEM.getKey(item).toString()))
                    .collect(Collectors.toList());
        }
        this.scrollOffset = 0;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks) {
        GuiContext ctx = GuiContext.fromGuiGraphics(gfx);
        this.extractBackground(gfx, mouseX, mouseY, partialTicks);
        super.extractRenderState(gfx, mouseX, mouseY, partialTicks);

        // 标题
        ctx.drawCenteredString(
                this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);

        // 搜索框
        this.searchField.extractWidgetRenderState(gfx, mouseX, mouseY, partialTicks);

        // 搜索提示
        if (this.searchField.isFocused() && this.searchField.getValue().isEmpty()) {
            ctx.drawString(this.font,
                    "搜索方块...", 16, 29, 0x80FFFFFF);
        }

        // 网格背景
        int gridHeight = visibleRows * CELL_SIZE;
        ctx.fill(GRID_LEFT - 2, GRID_TOP - 2,
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
                ctx.fill(cx, cy, cx + CELL_SIZE, cy + CELL_SIZE, 0x60FFFFFF);
            }

            // 方块图标
            ctx.renderItem(new ItemStack(item), cx + 3, cy + 3);
        }

        // 滚动信息
        if (filteredBlocks.size() > totalCells) {
            String scrollInfo = "共 " + filteredBlocks.size() + " 个方块";
            ctx.drawCenteredString(this.font,
                    scrollInfo, this.width / 2, GRID_TOP + gridHeight + 5, 0xFFFFFFFF);
        }

        // 悬停提示
        if (hoveredIdx >= 0 && hoveredIdx < filteredBlocks.size()) {
            Item item = filteredBlocks.get(hoveredIdx);
            String tip = item.getName(new ItemStack(item)).getString();
            int tipW = this.font.width(tip) + 8;
            int tipX = mouseX + 12;
            int tipY = mouseY - 20;
            if (tipX + tipW > this.width) tipX = this.width - tipW - 4;
            if (tipY < 4) tipY = mouseY + 16;
            ctx.fill(tipX, tipY, tipX + tipW, tipY + 16, 0xCC000000);
            ctx.fill(tipX + 1, tipY + 1, tipX + tipW - 1, tipY + 15, 0xCC333333);
            ctx.drawString(this.font, tip, tipX + 4, tipY + 4, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean onMouseClicked(MouseButtonEvent event, boolean isDrag) {
        if (super.onMouseClicked(event, isDrag)) return true;

        if (event.button() == 0 && !isDrag) {
            if (hoveredIdx >= 0 && hoveredIdx < filteredBlocks.size()) {
                callback.accept(filteredBlocks.get(hoveredIdx));
                Minecraft.getInstance().setScreenAndShow(parent);
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
    public boolean keyPressed(KeyEvent event) {
        // ESC 退出
        if (event.key() == 256) {
            Minecraft.getInstance().setScreenAndShow(parent);
            return true;
        }
        if (this.searchField.isFocused()) {
            return this.searchField.keyPressed(event);
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.searchField.isFocused()) {
            return this.searchField.charTyped(event);
        }
        return super.charTyped(event);
    }
}