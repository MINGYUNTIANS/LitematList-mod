package com.litematlist.gui;
//#if MC == 11701
//$$import net.minecraft.text.LiteralText;
//#endif

import com.litematlist.I18n;
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$import com.litematlist.PinyinSearch;
//#endif
//#if MC == 12111
//$$import com.litematlist.PinyinSearch;
//#endif
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;
import net.minecraft.client.MinecraftClient;
//#if MC == 12110 || MC == 12111
import net.minecraft.client.gui.Click;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.gui.DrawContext;
//#endif
import net.minecraft.client.gui.widget.TextFieldWidget;
//#if MC == 12110 || MC == 12111
import net.minecraft.client.input.CharInput;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$
//#endif
//#if MC == 12110 || MC == 12111
import net.minecraft.client.input.KeyInput;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$
//#endif
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
//#if MC == 12110
import com.litematlist.PinyinSearch;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.registry.Registries;
//#elseif MC == 11701
//$$import net.minecraft.util.registry.Registry;
//#endif
import net.minecraft.text.Text;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.gui.DrawableHelper;
//#endif
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.util.math.MatrixStack;
//#endif

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
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        for (Item item : Registries.ITEM) {
//#elseif MC == 11701
//$$        for (Item item : Registry.ITEM) {
//#endif
            if (item != Items.AIR) {
                if (allowedItems == null || allowedItems.contains(item)) {
                    list.add(item);
                }
            }
        }
        // 按创造模式物品栏顺序排序（注册顺序）
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        list.sort(Comparator.comparingInt(item -> Registries.ITEM.getRawId(item)));
//#elseif MC == 11701
//$$        list.sort(Comparator.comparingInt(item -> Registry.ITEM.getRawId(item)));
//#endif
        return list;
    }

    @Override
    public void initGui() {
        super.initGui();

        // 搜索框
        this.searchField = new TextFieldWidget(
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                this.textRenderer, 12, 24, this.width - 24, 20, Text.empty());
//#elseif MC == 11701
//$$                this.textRenderer, 12, 24, this.width - 24, 20, LiteralText.EMPTY);
//#endif
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
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            Registries.ITEM.getId(item).toString()))
//#elseif MC == 11701
//$$                            Registry.ITEM.getId(item).toString()))
//#endif
                    .collect(Collectors.toList());
        }
        this.scrollOffset = 0;
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
        super.render(drawContext, mouseX, mouseY, partialTicks);
//#elseif MC == 11701 || MC == 11904
//$$        super.render(matrices, mouseX, mouseY, partialTicks);
//#endif

        // 标题
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawCenteredTextWithShadow(
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$        this.textRenderer.drawWithShadow(matrices, this.title, this.width / 2 - this.textRenderer.getWidth(this.title) / 2, 8, 0xFFFFFFFF);
//#endif

        // 搜索框
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        this.searchField.render(drawContext, mouseX, mouseY, partialTicks);
//#elseif MC == 11701 || MC == 11904
//$$        this.searchField.render(matrices, mouseX, mouseY, partialTicks);
//#endif

        // 搜索提示
        if (this.searchField.isFocused() && this.searchField.getText().isEmpty()) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTextWithShadow(this.textRenderer,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    "搜索方块...", 16, 29, 0x80FFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, "搜索方块...", 16, 29, 0x80FFFFFF);
//#endif
        }

        // 网格背景
        int gridHeight = visibleRows * CELL_SIZE;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(GRID_LEFT - 2, GRID_TOP - 2,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                GRID_LEFT + columns * CELL_SIZE + 2, GRID_TOP + gridHeight + 2, 0x40000000);
//#endif
//#if MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, GRID_LEFT - 2, GRID_TOP - 2, GRID_LEFT + columns * CELL_SIZE + 2, GRID_TOP + gridHeight + 2, 0x40000000);
//#endif

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
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.fill(cx, cy, cx + CELL_SIZE, cy + CELL_SIZE, 0x60FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$                DrawableHelper.fill(matrices, cx, cy, cx + CELL_SIZE, cy + CELL_SIZE, 0x60FFFFFF);
//#endif
            }

            // 方块图标
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawItem(new ItemStack(item), cx + 3, cy + 3);
//#elseif MC == 11701
//$$            MinecraftClient.getInstance().getItemRenderer().renderInGui(new ItemStack(item), cx + 3, cy + 3);
//#elseif MC == 11904
//$$            MinecraftClient.getInstance().getItemRenderer().renderInGui(matrices, new ItemStack(item), cx + 3, cy + 3);
//#endif
        }

        // 滚动信息
        if (filteredBlocks.size() > totalCells) {
            String scrollInfo = "共 " + filteredBlocks.size() + " 个方块";
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawCenteredTextWithShadow(this.textRenderer,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    scrollInfo, this.width / 2, GRID_TOP + gridHeight + 5, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, scrollInfo, this.width / 2 - this.textRenderer.getWidth(scrollInfo) / 2, GRID_TOP + gridHeight + 5, 0xFFFFFFFF);
//#endif
        }

        // 悬停提示
        if (hoveredIdx >= 0 && hoveredIdx < filteredBlocks.size()) {
            Item item = filteredBlocks.get(hoveredIdx);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTooltip(this.textRenderer,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    item.getName(), mouseX, mouseY);
//#endif
//#if MC == 11701 || MC == 11904
//$$            this.renderTooltip(matrices, item.getName(), mouseX, mouseY);
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

//#if MC == 12110 || MC == 12111
        if (click.button() == 0 && !isDrag) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        if (mouseButton == 0) {
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        if (button == 0) {
//#endif
            if (hoveredIdx >= 0 && hoveredIdx < filteredBlocks.size()) {
                callback.accept(filteredBlocks.get(hoveredIdx));
                MinecraftClient.getInstance().setScreen(parent);
                return true;
            }
        }
        return false;
    }

    @Override
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount,
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                  double verticalAmount) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
//#endif
        int totalCells = columns * visibleRows;
        if (mouseY >= GRID_TOP && mouseY <= GRID_TOP + visibleRows * CELL_SIZE) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            scrollOffset = Math.max(0, scrollOffset - (int) verticalAmount * columns);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            scrollOffset = Math.max(0, scrollOffset - (int) amount * columns);
//#endif
            int maxOffset = Math.max(0, filteredBlocks.size() - totalCells);
            scrollOffset = Math.min(scrollOffset, maxOffset);
            if (scrollOffset % columns != 0) {
                scrollOffset = (scrollOffset / columns) * columns;
            }
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
        // ESC 退出
//#if MC == 12110 || MC == 12111
        if (keyInput.key() == 256) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        if (keyCode == 256) {
//#endif
            MinecraftClient.getInstance().setScreen(parent);
            return true;
        }
        if (this.searchField.isFocused()) {
//#if MC == 12110 || MC == 12111
            return this.searchField.keyPressed(keyInput);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            return this.searchField.keyPressed(keyCode, scanCode, modifiers);
//#endif
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
        if (this.searchField.isFocused()) {
//#if MC == 12110 || MC == 12111
            return this.searchField.charTyped(charInput);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            return this.searchField.charTyped(chr, modifiers);
//#endif
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
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$
//$$
//#endif