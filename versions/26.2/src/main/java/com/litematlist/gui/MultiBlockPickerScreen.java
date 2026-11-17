package com.litematlist.gui;

import com.litematlist.I18n;
import com.litematlist.PinyinSearch;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.render.GuiContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.*;
import java.util.function.Consumer;

import org.apache.commons.lang3.tuple.Pair;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.GpuSampler;

/**
 * 多选物品选择器：用于白名单批量添加物品。
 * 左侧物品网格 + 右侧已选预览栏（带×删除按钮）。
 */
public class MultiBlockPickerScreen extends GuiBase {

    private final GuiBase parent;
    private final Consumer<Set<Item>> callback;
    private final Set<Item> selectedItems = new LinkedHashSet<>();
    private final List<Item> filteredItems = new ArrayList<>();
    private EditBox searchField;
    private String searchText = "";
    private int scrollOffset = 0;
    private int visibleRows;
    private static final int ROW_HEIGHT = 20;
    private static final int LIST_TOP = 50;
    private static final int ITEMS_PER_ROW = 10;
    private static final int ICON_SIZE = 18;
    private static final int GAP = 2;

    // 右侧预览栏
    private static final int PREVIEW_LEFT_OFFSET = 200; // 左侧网格右边界偏移
    private static final int PREVIEW_COLS = 5;
    private int previewScrollOffset = 0;

    private static final Identifier SEARCH_ICON = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/search.png");
    private static final int SEARCH_ICON_SIZE = 14;

    public MultiBlockPickerScreen(GuiBase parent, Consumer<Set<Item>> callback) {
        super();
        this.parent = parent;
        this.callback = callback;
        this.title = I18n.tr("litematlist.title.multi_item_picker");
        filterItems("");
    }

    @Override
    public void initGui() {
        super.initGui();
        this.visibleRows = (this.height - 100) / ROW_HEIGHT;

        // 搜索框
        int searchX = 20;
        int searchY = 22;
        int fieldX = searchX + SEARCH_ICON_SIZE + 2;
        this.searchField = new EditBox(Minecraft.getInstance().font, fieldX + 4, searchY + 2, 94, 12, Component.empty());
        this.searchField.setMaxLength(100);
        this.searchField.setBordered(false);
        this.searchField.setValue(searchText);
        this.searchField.setResponder(this::onSearchChanged);
        

        // 确认按钮
        int buttonY = this.height - 38;
        ButtonGeneric confirmBtn = new ButtonGeneric(this.width / 2 - 100, buttonY, 100, 20, I18n.tr("litematlist.button.confirm_selected"));
        confirmBtn.setRenderDefaultBackground(true);
        this.addButton(confirmBtn, (IButtonActionListener) (b, mb) -> {
            callback.accept(new LinkedHashSet<>(selectedItems));
            Minecraft.getInstance().setScreenAndShow(parent);
        });

        // 取消按钮
        ButtonGeneric cancelBtn = new ButtonGeneric(this.width / 2 + 4, buttonY, 100, 20, I18n.tr("litematlist.button.cancel"));
        cancelBtn.setRenderDefaultBackground(true);
        this.addButton(cancelBtn, (IButtonActionListener) (b, mb) ->
                Minecraft.getInstance().setScreenAndShow(parent));
    }

    private void onSearchChanged(String text) {
        searchText = text;
        scrollOffset = 0;
        filterItems(text);
    }

    private void filterItems(String text) {
        filteredItems.clear();

        for (var key : BuiltInRegistries.ITEM.keySet()) {
            Item item = BuiltInRegistries.ITEM.get(key).map(Holder::value).orElse(null);
            if (item == null) continue;
            // 四路匹配：中文名包含 / 英文物品ID包含 / 拼音全拼（xiangmumuban）/ 拼音首字母（xmmb）
            if (PinyinSearch.matches(text, new ItemStack(item).getHoverName().getString(), key.toString())) {
                filteredItems.add(item);
            }
        }
        // 按创造模式物品栏顺序排序（注册ID顺序）
        filteredItems.sort(Comparator.comparingInt(item -> BuiltInRegistries.ITEM.getId(item)));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(gfx, mouseX, mouseY, partialTicks);
        GuiContext ctx = GuiContext.fromGuiGraphics(gfx);

        // 搜索框背景（纯黑 + 白色边框）+ 搜索图标
        int searchX = 20;
        int searchY = 22;
        int searchW = 100;
        int searchH = 16;

        // 搜索图标（独立纹理渲染，blit参数为角点坐标）
        Pair<GpuTextureView, GpuSampler> searchPair = ctx.bindTexture(SEARCH_ICON);
        if (searchPair != null && searchPair.getLeft() != null) {
            ctx.blit(searchPair.getLeft(), searchPair.getRight(), searchX, searchY,
                    searchX + SEARCH_ICON_SIZE, searchY + SEARCH_ICON_SIZE, 0.0f, 1.0f, 0.0f, 1.0f);
        }

        // 搜索框背景（纯黑 + 白色边框）
        int fieldX = searchX + SEARCH_ICON_SIZE + 2;
        ctx.fill(fieldX - 1, searchY - 1, fieldX + searchW + 1, searchY + searchH + 1, 0xFFFFFFFF);
        ctx.fill(fieldX, searchY, fieldX + searchW, searchY + searchH, 0xFF000000);

        // 搜索文字
        if (searchField != null) {
            String txt = searchField.getValue();
            if (txt.isEmpty()) {
                ctx.drawString(this.font, I18n.tr("litematlist.search.placeholder"),
                        fieldX + 4, searchY + 2, 0x80FFFFFF, false);
            } else {
                ctx.drawString(this.font, txt, fieldX + 4, searchY + 2, 0xFFFFFFFF, false);
            }
            if (searchField.isFocused() && System.currentTimeMillis() % 1000 < 500) {
                int cp = searchField.getCursorPosition();
                String before = txt.substring(0, Math.min(cp, txt.length()));
                int cx = fieldX + 4 + this.font.width(before);
                ctx.fill(cx, searchY + 1, cx + 1, searchY + searchH - 1, 0xFFFFFFFF);
            }
        }

        // 左侧物品网格
        int startX = 20;
        int startY = LIST_TOP;
        int cellSize = ICON_SIZE + GAP;

        int startIdx = scrollOffset * ITEMS_PER_ROW;
        int col = 0, row = 0;
        int maxRow = (this.height - 100) / cellSize;

        for (int i = startIdx; i < filteredItems.size(); i++) {
            Item item = filteredItems.get(i);
            int x = startX + col * cellSize;
            int y = startY + row * cellSize;

            if (row >= maxRow) break;

            boolean isSelected = selectedItems.contains(item);
            int bgColor = isSelected ? 0x8000FF00 : 0x40000000;
            ctx.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, bgColor);

            ctx.renderItem(new ItemStack(item), x + 1, y + 1);

            if (mouseX >= x && mouseX <= x + ICON_SIZE && mouseY >= y && mouseY <= y + ICON_SIZE) {
                ctx.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, 0x40FFFFFF);
                // 悬停显示名称+完整ID
                Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
                String tooltip = new ItemStack(item).getHoverName().getString() + " (" + (itemId != null ? itemId.toString() : "?") + ")";
                // tooltip rendering handled by drawString
            }

            col++;
            if (col >= ITEMS_PER_ROW) {
                col = 0;
                row++;
            }
        }

        // 右侧预览栏：已选物品
        int previewLeft = 20 + ITEMS_PER_ROW * cellSize + 10;
        int previewWidth = this.width - previewLeft - 12;
        int previewCols = Math.min(10, Math.max(1, previewWidth / cellSize));

        ctx.drawString(this.font,
                I18n.tr("litematlist.label.selected_count", selectedItems.size()),
                previewLeft, 24, 0xFFFFFFFF);

        List<Item> selectedList = new ArrayList<>(selectedItems);
        int previewStartIdx = previewScrollOffset * previewCols;
        int previewRow = 0;
        int previewCol = 0;
        int previewMaxRows = (this.height - 100) / cellSize;

        for (int i = previewStartIdx; i < selectedList.size(); i++) {
            Item item = selectedList.get(i);
            int px = previewLeft + previewCol * cellSize;
            int py = startY + previewRow * cellSize;

            if (previewRow >= previewMaxRows) break;

            ctx.fill(px, py, px + ICON_SIZE, py + ICON_SIZE, 0x40000000);
            ctx.renderItem(new ItemStack(item), px + 1, py + 1);

            // ×删除按钮（右上角）
            int crossX = px + ICON_SIZE - 6;
            int crossY = py + 1;
            boolean crossHovered = mouseX >= crossX && mouseX <= crossX + 6 && mouseY >= crossY && mouseY <= crossY + 6;
            ctx.fill(crossX, crossY, crossX + 6, crossY + 6, crossHovered ? 0xFFFF0000 : 0x80FF0000);
            ctx.drawString(this.font, "×", crossX + 1, crossY - 1, 0xFFFFFFFF);

            previewCol++;
            if (previewCol >= previewCols) {
                previewCol = 0;
                previewRow++;
            }
        }
    }

    @Override
    public boolean onMouseClicked(MouseButtonEvent event, boolean isDrag) {
        if (super.onMouseClicked(event, isDrag)) return true;

        int mouseX = (int) event.x();
        int mouseY = (int) event.y();

        if (event.button() == 0) {
            int cellSize = ICON_SIZE + GAP;
            int startY = LIST_TOP;
            int previewLeft = 20 + ITEMS_PER_ROW * cellSize + 10;

            // 右侧预览栏×删除
            if (mouseX >= previewLeft) {
                int previewWidth = this.width - previewLeft - 12;
                int previewCols = Math.min(10, Math.max(1, previewWidth / cellSize));
                List<Item> selectedList = new ArrayList<>(selectedItems);
                int previewStartIdx = previewScrollOffset * previewCols;
                int previewRow = 0, previewCol = 0;
                int previewMaxRows = (this.height - 100) / cellSize;

                for (int i = previewStartIdx; i < selectedList.size(); i++) {
                    Item item = selectedList.get(i);
                    int px = previewLeft + previewCol * cellSize;
                    int py = startY + previewRow * cellSize;
                    if (previewRow >= previewMaxRows) break;

                    int crossX = px + ICON_SIZE - 6;
                    int crossY = py + 1;
                    if (mouseX >= crossX && mouseX <= crossX + 6 && mouseY >= crossY && mouseY <= crossY + 6) {
                        selectedItems.remove(item);
                        return true;
                    }

                    previewCol++;
                    if (previewCol >= previewCols) {
                        previewCol = 0;
                        previewRow++;
                    }
                }
                return false;
            }

            // 左侧网格点击
            int startX = 20;
            int startIdx = scrollOffset * ITEMS_PER_ROW;
            int col = 0, row = 0;
            int maxRow = (this.height - 100) / cellSize;

            for (int i = startIdx; i < filteredItems.size(); i++) {
                int x = startX + col * cellSize;
                int y = startY + row * cellSize;

                if (row >= maxRow) break;

                if (mouseX >= x && mouseX <= x + ICON_SIZE && mouseY >= y && mouseY <= y + ICON_SIZE) {
                    Item item = filteredItems.get(i);
                    if (selectedItems.contains(item)) {
                        selectedItems.remove(item);
                    } else {
                        selectedItems.add(item);
                    }
                    return true;
                }

                col++;
                if (col >= ITEMS_PER_ROW) {
                    col = 0;
                    row++;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int cellSize = ICON_SIZE + GAP;
        int previewLeft = 20 + ITEMS_PER_ROW * cellSize + 10;

        if (mouseY >= LIST_TOP && mouseY <= this.height - 50) {
            if (mouseX < previewLeft) {
                // 左侧网格滚动
                int totalRows = (int) Math.ceil((double) filteredItems.size() / ITEMS_PER_ROW);
                int maxScroll = Math.max(0, totalRows - (this.height - 100) / cellSize);
                scrollOffset = Math.max(0, scrollOffset - (int) verticalAmount);
                scrollOffset = Math.min(scrollOffset, maxScroll);
            } else {
                // 右侧预览栏滚动
                int previewWidth = this.width - previewLeft - 12;
                int previewCols = Math.min(10, Math.max(1, previewWidth / cellSize));
                int totalRows = (int) Math.ceil((double) selectedItems.size() / previewCols);
                int maxScroll = Math.max(0, totalRows - (this.height - 100) / cellSize);
                previewScrollOffset = Math.max(0, previewScrollOffset - (int) verticalAmount);
                previewScrollOffset = Math.min(previewScrollOffset, maxScroll);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}




