package com.litematlist.gui;

import com.litematlist.I18n;
import com.litematlist.PinyinSearch;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.function.Consumer;

/**
 * 澶氶€夌墿鍝侀€夋嫨鍣細鐢ㄤ簬鐧藉悕鍗曟壒閲忔坊鍔犵墿鍝併€? * 宸︿晶鐗╁搧缃戞牸 + 鍙充晶宸查€夐瑙堟爮锛堝甫脳鍒犻櫎鎸夐挳锛夈€? */
public class MultiBlockPickerScreen extends GuiBase {

    private final GuiBase parent;
    private final Consumer<Set<Item>> callback;
    private final Set<Item> selectedItems = new LinkedHashSet<>();
    private final List<Item> filteredItems = new ArrayList<>();
    private TextFieldWidget searchField;
    private String searchText = "";
    private int scrollOffset = 0;
    private int visibleRows;
    private static final int ROW_HEIGHT = 20;
    private static final int LIST_TOP = 50;
    private static final int ITEMS_PER_ROW = 10;
    private static final int ICON_SIZE = 18;
    private static final int GAP = 2;

    // // 宸︿晶缃戞牸鍙宠竟鐣屽亸绉?    private static final int PREVIEW_COLS = 5;

    private int previewScrollOffset = 0;

    private static final Identifier SEARCH_ICON = Identifier.of("litematlist", "textures/gui/search.png");
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

        // 鎼滅储妗?
        int searchX = 20;
        int searchY = 22;
        int fieldX = searchX + SEARCH_ICON_SIZE + 2;
        this.searchField = new TextFieldWidget(this.textRenderer, fieldX + 4, searchY + 2, 94, 12, null);
        this.searchField.setMaxLength(100);
        this.searchField.setDrawsBackground(false);
        this.searchField.setText(searchText);
        this.searchField.setChangedListener(this::onSearchChanged);
        this.addDrawableChild(this.searchField);

        // 纭鎸夐挳
        int buttonY = this.height - 38;
        ButtonGeneric confirmBtn = new ButtonGeneric(this.width / 2 - 100, buttonY, 100, 20, I18n.tr("litematlist.button.confirm_selected"));
        confirmBtn.setRenderDefaultBackground(true);
        this.addButton(confirmBtn, (IButtonActionListener) (b, mb) -> {
            callback.accept(new LinkedHashSet<>(selectedItems));
            MinecraftClient.getInstance().setScreen(parent);
        });

        // 鍙栨秷鎸夐挳
        ButtonGeneric cancelBtn = new ButtonGeneric(this.width / 2 + 4, buttonY, 100, 20, I18n.tr("litematlist.button.cancel"));
        cancelBtn.setRenderDefaultBackground(true);
        this.addButton(cancelBtn, (IButtonActionListener) (b, mb) ->
                MinecraftClient.getInstance().setScreen(parent));
    }

    private void onSearchChanged(String text) {
        searchText = text;
        scrollOffset = 0;
        filterItems(text);
    }

    private void filterItems(String text) {
        filteredItems.clear();
        for (Identifier id : Registries.ITEM.getIds()) {
            Item item = Registries.ITEM.get(id);
            if (item == null) continue;
            // 四路匹配：中文名包含 / 英文物品ID包含 / 拼音全拼（xiangmumuban）/ 拼音首字母（xmmb）
            if (PinyinSearch.matches(text, item.getName().getString(), id.toString())) {
                filteredItems.add(item);
            }
        }
        // 鎸夊垱閫犳ā寮忕墿鍝佹爮椤哄簭鎺掑簭锛堟敞鍐孖D椤哄簭锛?        filteredItems.sort(Comparator.comparingInt(item -> Registries.ITEM.getRawId(item)));
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        super.render(drawContext, mouseX, mouseY, delta);

        // 鎼滅储妗嗚儗鏅紙绾粦 + 鐧借壊杈规锛? 鎼滅储鍥炬爣
        int searchX = 20;
        int searchY = 22;
        int searchW = 100;
        int searchH = 16;

        // 鎼滅储鍥炬爣
        drawContext.drawTexture(SEARCH_ICON,
                searchX, searchY, 0.0f, 0.0f, SEARCH_ICON_SIZE, SEARCH_ICON_SIZE, SEARCH_ICON_SIZE, SEARCH_ICON_SIZE);

        // 鎼滅储妗嗚儗鏅紙绾粦 + 鐧借壊杈规锛?
        int fieldX = searchX + SEARCH_ICON_SIZE + 2;
        drawContext.fill(fieldX - 1, searchY - 1, fieldX + searchW + 1, searchY + searchH + 1, 0xFFFFFFFF);
        drawContext.fill(fieldX, searchY, fieldX + searchW, searchY + searchH, 0xFF000000);

        // 鎼滅储鏂囧瓧
        if (searchField != null) {
            String txt = searchField.getText();
            if (txt.isEmpty()) {
                drawContext.drawText(this.textRenderer, I18n.tr("litematlist.search.placeholder"),
                        fieldX + 4, searchY + 2, 0x80FFFFFF, false);
            } else {
                drawContext.drawText(this.textRenderer, txt, fieldX + 4, searchY + 2, 0xFFFFFFFF, false);
            }
            if (searchField.isFocused() && System.currentTimeMillis() % 1000 < 500) {
                int cp = searchField.getCursor();
                String before = txt.substring(0, Math.min(cp, txt.length()));
                int cx = fieldX + 4 + this.textRenderer.getWidth(before);
                drawContext.fill(cx, searchY + 1, cx + 1, searchY + searchH - 1, 0xFFFFFFFF);
            }
        }

        // 宸︿晶鐗╁搧缃戞牸
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
            drawContext.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, bgColor);

            drawContext.drawItem(new ItemStack(item), x + 1, y + 1);

            if (mouseX >= x && mouseX <= x + ICON_SIZE && mouseY >= y && mouseY <= y + ICON_SIZE) {
                drawContext.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, 0x40FFFFFF);
                // 鎮仠鏄剧ず鍚嶇О+瀹屾暣ID
                Identifier itemId = Registries.ITEM.getId(item);
                String tooltip = item.getName().getString() + " (" + (itemId != null ? itemId.toString() : "?") + ")";
                drawContext.drawTooltip(this.textRenderer, Text.literal(tooltip), mouseX, mouseY);
            }

            col++;
            if (col >= ITEMS_PER_ROW) {
                col = 0;
                row++;
            }
        }

        // 鍙充晶棰勮鏍忥細宸查€夌墿鍝?
        int previewLeft = 20 + ITEMS_PER_ROW * cellSize + 10;
        int previewWidth = this.width - previewLeft - 12;
        int previewCols = Math.min(10, Math.max(1, previewWidth / cellSize));

        drawContext.drawTextWithShadow(this.textRenderer,
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

            drawContext.fill(px, py, px + ICON_SIZE, py + ICON_SIZE, 0x40000000);
            drawContext.drawItem(new ItemStack(item), px + 1, py + 1);

            // 脳鍒犻櫎鎸夐挳锛堝彸涓婅锛?
            int crossX = px + ICON_SIZE - 6;
            int crossY = py + 1;
            boolean crossHovered = mouseX >= crossX && mouseX <= crossX + 6 && mouseY >= crossY && mouseY <= crossY + 6;
            drawContext.fill(crossX, crossY, crossX + 6, crossY + 6, crossHovered ? 0xFFFF0000 : 0x80FF0000);
            drawContext.drawTextWithShadow(this.textRenderer, "×", crossX + 1, crossY - 1, 0xFFFFFFFF);

            previewCol++;
            if (previewCol >= previewCols) {
                previewCol = 0;
                previewRow++;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (super.mouseClicked(mouseX, mouseY, mouseButton)) return true;

        mouseX = (int) mouseX;
        mouseY = (int) mouseY;

        if (mouseButton == 0) {
            int cellSize = ICON_SIZE + GAP;
            int startY = LIST_TOP;
            int previewLeft = 20 + ITEMS_PER_ROW * cellSize + 10;

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
                // 宸︿晶缃戞牸婊氬姩
                int totalRows = (int) Math.ceil((double) filteredItems.size() / ITEMS_PER_ROW);
                int maxScroll = Math.max(0, totalRows - (this.height - 100) / cellSize);
                scrollOffset = Math.max(0, scrollOffset - (int) verticalAmount);
                scrollOffset = Math.min(scrollOffset, maxScroll);
            } else {
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
