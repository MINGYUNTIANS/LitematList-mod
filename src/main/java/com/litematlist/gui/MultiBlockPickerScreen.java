package com.litematlist.gui;
//#if MC == 11701
//$$import net.minecraft.text.LiteralText;
//#endif

import com.litematlist.I18n;
import com.litematlist.PinyinSearch;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.MinecraftClient;
//#if MC == 12104
//$$import net.minecraft.client.render.RenderLayer;
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.gui.DrawContext;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.gl.RenderPipelines;
//#endif
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.registry.Registries;
//#elseif MC == 11701
//$$import net.minecraft.util.registry.Registry;
//#endif
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.function.Consumer;
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.gui.DrawableHelper;
//#endif
//#if MC == 11701 || MC == 11904
//$$import com.mojang.blaze3d.systems.RenderSystem;
//#endif
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.util.math.MatrixStack;
//#endif

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
/**
 * 多选物品选择器：用于白名单批量添加物品。
 * 左侧物品网格 + 右侧已选预览栏（带×删除按钮）。
 */
//#elseif MC == 11701 || MC == 11904
//$$/**
//$$ * 澶氶€夌墿鍝侀€夋嫨鍣細鐢ㄤ簬鐧藉悕鍗曟壒閲忔坊鍔犵墿鍝併€? * 宸︿晶鐗╁搧缃戞牸 + 鍙充晶宸查€夐瑙堟爮锛堝甫脳鍒犻櫎鎸夐挳锛夈€? */
//#endif
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

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // 右侧预览栏
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final int PREVIEW_LEFT_OFFSET = 200; // 左侧网格右边界偏移
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final int PREVIEW_COLS = 5;
//#endif
//#if MC == 11701 || MC == 11904
//$$    // // 宸︿晶缃戞牸鍙宠竟鐣屽亸绉?    private static final int PREVIEW_COLS = 5;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
    private int previewScrollOffset = 0;

//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final Identifier SEARCH_ICON = Identifier.of("litematlist", "textures/gui/search.png");
//#elseif MC == 11701
//$$    private static final Identifier SEARCH_ICON = new Identifier("litematlist", "textures/gui/search.png");
//#endif
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

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 搜索框
//#elseif MC == 11701 || MC == 11904
//$$        // 鎼滅储妗?
//#endif
        int searchX = 20;
        int searchY = 22;
        int fieldX = searchX + SEARCH_ICON_SIZE + 2;
        this.searchField = new TextFieldWidget(this.textRenderer, fieldX + 4, searchY + 2, 94, 12, null);
        this.searchField.setMaxLength(100);
        this.searchField.setDrawsBackground(false);
        this.searchField.setText(searchText);
        this.searchField.setChangedListener(this::onSearchChanged);
        this.addDrawableChild(this.searchField);

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 确认按钮
//#elseif MC == 11701 || MC == 11904
//$$        // 纭鎸夐挳
//#endif
        int buttonY = this.height - 38;
        ButtonGeneric confirmBtn = new ButtonGeneric(this.width / 2 - 100, buttonY, 100, 20, I18n.tr("litematlist.button.confirm_selected"));
        confirmBtn.setRenderDefaultBackground(true);
        this.addButton(confirmBtn, (IButtonActionListener) (b, mb) -> {
            callback.accept(new LinkedHashSet<>(selectedItems));
            MinecraftClient.getInstance().setScreen(parent);
        });

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 取消按钮
//#elseif MC == 11701 || MC == 11904
//$$        // 鍙栨秷鎸夐挳
//#endif
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
//#if MC == 12110 || MC == 12111

//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        for (Identifier id : Registries.ITEM.getIds()) {
//#elseif MC == 11701
//$$        for (Identifier id : Registry.ITEM.getIds()) {
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            Item item = Registries.ITEM.get(id);
//#elseif MC == 11701
//$$            Item item = Registry.ITEM.get(id);
//#endif
            if (item == null) continue;
            // 四路匹配：中文名包含 / 英文物品ID包含 / 拼音全拼（xiangmumuban）/ 拼音首字母（xmmb）
            if (PinyinSearch.matches(text, item.getName().getString(), id.toString())) {
                filteredItems.add(item);
            }
        }
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 按创造模式物品栏顺序排序（注册ID顺序）
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        filteredItems.sort(Comparator.comparingInt(item -> Registries.ITEM.getRawId(item)));
//#endif
//#if MC == 11701
//$$        // 鎸夊垱閫犳ā寮忕墿鍝佹爮椤哄簭鎺掑簭锛堟敞鍐孖D椤哄簭锛?        filteredItems.sort(Comparator.comparingInt(item -> Registry.ITEM.getRawId(item)));
//#elseif MC == 11904
//$$        // 鎸夊垱閫犳ā寮忕墿鍝佹爮椤哄簭鎺掑簭锛堟敞鍐孖D椤哄簭锛?        filteredItems.sort(Comparator.comparingInt(item -> Registries.ITEM.getRawId(item)));
//#endif
    }

    @Override
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
//#elseif MC == 11701 || MC == 11904
//$$    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        super.render(drawContext, mouseX, mouseY, delta);
//#elseif MC == 11701 || MC == 11904
//$$        super.render(matrices, mouseX, mouseY, delta);
//#endif

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 搜索框背景（纯黑 + 白色边框）+ 搜索图标
//#elseif MC == 11701 || MC == 11904
//$$        // 鎼滅储妗嗚儗鏅紙绾粦 + 鐧借壊杈规锛? 鎼滅储鍥炬爣
//#endif
        int searchX = 20;
        int searchY = 22;
        int searchW = 100;
        int searchH = 16;

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 搜索图标
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, SEARCH_ICON,
//#elseif MC == 12001
//$$        drawContext.drawTexture(SEARCH_ICON,
//#elseif MC == 12101
//$$        drawContext.drawTexture( SEARCH_ICON,
//#elseif MC == 12104
//$$        drawContext.drawTexture(RenderLayer::getGuiTextured, SEARCH_ICON,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                searchX, searchY, 0.0f, 0.0f, SEARCH_ICON_SIZE, SEARCH_ICON_SIZE, SEARCH_ICON_SIZE, SEARCH_ICON_SIZE);
//#endif
//#if MC == 11701 || MC == 11904
//$$        // 鎼滅储鍥炬爣
//#endif
//#if MC == 11701 || MC == 11904
//$$        RenderSystem.setShaderTexture(0, SEARCH_ICON); DrawableHelper.drawTexture(matrices, searchX, searchY, 0.0f, 0.0f, SEARCH_ICON_SIZE, SEARCH_ICON_SIZE, SEARCH_ICON_SIZE, SEARCH_ICON_SIZE);
//#endif

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 搜索框背景（纯黑 + 白色边框）
//#elseif MC == 11701 || MC == 11904
//$$        // 鎼滅储妗嗚儗鏅紙绾粦 + 鐧借壊杈规锛?
//#endif
        int fieldX = searchX + SEARCH_ICON_SIZE + 2;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(fieldX - 1, searchY - 1, fieldX + searchW + 1, searchY + searchH + 1, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, fieldX - 1, searchY - 1, fieldX + searchW + 1, searchY + searchH + 1, 0xFFFFFFFF);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(fieldX, searchY, fieldX + searchW, searchY + searchH, 0xFF000000);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, fieldX, searchY, fieldX + searchW, searchY + searchH, 0xFF000000);
//#endif

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 搜索文字
//#elseif MC == 11701 || MC == 11904
//$$        // 鎼滅储鏂囧瓧
//#endif
        if (searchField != null) {
            String txt = searchField.getText();
            if (txt.isEmpty()) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.drawText(this.textRenderer, I18n.tr("litematlist.search.placeholder"),
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        fieldX + 4, searchY + 2, 0x80FFFFFF, false);
//#endif
//#if MC == 11701 || MC == 11904
//$$                this.textRenderer.draw(matrices, I18n.tr("litematlist.search.placeholder"), fieldX + 4, searchY + 2, 0x80FFFFFF);
//#endif
            } else {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.drawText(this.textRenderer, txt, fieldX + 4, searchY + 2, 0xFFFFFFFF, false);
//#elseif MC == 11701 || MC == 11904
//$$                this.textRenderer.draw(matrices, txt, fieldX + 4, searchY + 2, 0xFFFFFFFF);
//#endif
            }
            if (searchField.isFocused() && System.currentTimeMillis() % 1000 < 500) {
                int cp = searchField.getCursor();
                String before = txt.substring(0, Math.min(cp, txt.length()));
                int cx = fieldX + 4 + this.textRenderer.getWidth(before);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.fill(cx, searchY + 1, cx + 1, searchY + searchH - 1, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$                DrawableHelper.fill(matrices, cx, searchY + 1, cx + 1, searchY + searchH - 1, 0xFFFFFFFF);
//#endif
            }
        }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 左侧物品网格
//#elseif MC == 11701 || MC == 11904
//$$        // 宸︿晶鐗╁搧缃戞牸
//#endif
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
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, bgColor);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, x, y, x + ICON_SIZE, y + ICON_SIZE, bgColor);
//#endif

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawItem(new ItemStack(item), x + 1, y + 1);
//#elseif MC == 11701
//$$            MinecraftClient.getInstance().getItemRenderer().renderInGui(new ItemStack(item), x + 1, y + 1);
//#elseif MC == 11904
//$$            MinecraftClient.getInstance().getItemRenderer().renderInGui(matrices, new ItemStack(item), x + 1, y + 1);
//#endif

            if (mouseX >= x && mouseX <= x + ICON_SIZE && mouseY >= y && mouseY <= y + ICON_SIZE) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, 0x40FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$                DrawableHelper.fill(matrices, x, y, x + ICON_SIZE, y + ICON_SIZE, 0x40FFFFFF);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 悬停显示名称+完整ID
//#elseif MC == 11701 || MC == 11904
//$$                // 鎮仠鏄剧ず鍚嶇О+瀹屾暣ID
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                Identifier itemId = Registries.ITEM.getId(item);
//#elseif MC == 11701
//$$                Identifier itemId = Registry.ITEM.getId(item);
//#endif
                String tooltip = item.getName().getString() + " (" + (itemId != null ? itemId.toString() : "?") + ")";
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.drawTooltip(this.textRenderer, Text.literal(tooltip), mouseX, mouseY);
//#elseif MC == 11701
//$$                this.renderTooltip(matrices, new LiteralText(tooltip), mouseX, mouseY);
//#elseif MC == 11904
//$$                this.renderTooltip(matrices, Text.literal(tooltip), mouseX, mouseY);
//#endif
            }

            col++;
            if (col >= ITEMS_PER_ROW) {
                col = 0;
                row++;
            }
        }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 右侧预览栏：已选物品
//#elseif MC == 11701 || MC == 11904
//$$        // 鍙充晶棰勮鏍忥細宸查€夌墿鍝?
//#endif
        int previewLeft = 20 + ITEMS_PER_ROW * cellSize + 10;
        int previewWidth = this.width - previewLeft - 12;
        int previewCols = Math.min(10, Math.max(1, previewWidth / cellSize));

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawTextWithShadow(this.textRenderer,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                I18n.tr("litematlist.label.selected_count", selectedItems.size()),
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                previewLeft, 24, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$        this.textRenderer.drawWithShadow(matrices, I18n.tr("litematlist.label.selected_count", selectedItems.size()), previewLeft, 24, 0xFFFFFFFF);
//#endif

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

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(px, py, px + ICON_SIZE, py + ICON_SIZE, 0x40000000);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, px, py, px + ICON_SIZE, py + ICON_SIZE, 0x40000000);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawItem(new ItemStack(item), px + 1, py + 1);
//#elseif MC == 11701
//$$            MinecraftClient.getInstance().getItemRenderer().renderInGui(new ItemStack(item), px + 1, py + 1);
//#elseif MC == 11904
//$$            MinecraftClient.getInstance().getItemRenderer().renderInGui(matrices, new ItemStack(item), px + 1, py + 1);
//#endif

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // ×删除按钮（右上角）
//#elseif MC == 11701 || MC == 11904
//$$            // 脳鍒犻櫎鎸夐挳锛堝彸涓婅锛?
//#endif
            int crossX = px + ICON_SIZE - 6;
            int crossY = py + 1;
            boolean crossHovered = mouseX >= crossX && mouseX <= crossX + 6 && mouseY >= crossY && mouseY <= crossY + 6;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(crossX, crossY, crossX + 6, crossY + 6, crossHovered ? 0xFFFF0000 : 0x80FF0000);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, crossX, crossY, crossX + 6, crossY + 6, crossHovered ? 0xFFFF0000 : 0x80FF0000);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTextWithShadow(this.textRenderer, "×", crossX + 1, crossY - 1, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, "×", crossX + 1, crossY - 1, 0xFFFFFFFF);
//#endif

            previewCol++;
            if (previewCol >= previewCols) {
                previewCol = 0;
                previewRow++;
            }
        }
    }

    @Override
//#if MC == 12110 || MC == 12111
    public boolean onMouseClicked(net.minecraft.client.gui.Click click, boolean isDrag) {
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
        int mouseX = (int) click.x();
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        mouseX = (int) mouseX;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        
//#endif
//#if MC == 12110 || MC == 12111
        int mouseY = (int) click.y();
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        mouseY = (int) mouseY;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        
//#endif

//#if MC == 12110 || MC == 12111
        if (click.button() == 0) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        if (mouseButton == 0) {
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        if (button == 0) {
//#endif
            int cellSize = ICON_SIZE + GAP;
            int startY = LIST_TOP;
            int previewLeft = 20 + ITEMS_PER_ROW * cellSize + 10;

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 右侧预览栏×删除
//#elseif MC == 11701 || MC == 11904
//$$                        if (mouseX >= previewLeft) {
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (mouseX >= previewLeft) {
//#elseif MC == 11701 || MC == 11904
//$$
//#endif
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

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 左侧网格点击
//#elseif MC == 11701 || MC == 11904
//$$        
//#endif
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
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
//#endif
        int cellSize = ICON_SIZE + GAP;
        int previewLeft = 20 + ITEMS_PER_ROW * cellSize + 10;

        if (mouseY >= LIST_TOP && mouseY <= this.height - 50) {
            if (mouseX < previewLeft) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 左侧网格滚动
//#elseif MC == 11701 || MC == 11904
//$$                // 宸︿晶缃戞牸婊氬姩
//#endif
                int totalRows = (int) Math.ceil((double) filteredItems.size() / ITEMS_PER_ROW);
                int maxScroll = Math.max(0, totalRows - (this.height - 100) / cellSize);
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                scrollOffset = Math.max(0, scrollOffset - (int) verticalAmount);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                scrollOffset = Math.max(0, scrollOffset - (int) amount);
//#endif
                scrollOffset = Math.min(scrollOffset, maxScroll);
            } else {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 右侧预览栏滚动
//#endif
                int previewWidth = this.width - previewLeft - 12;
                int previewCols = Math.min(10, Math.max(1, previewWidth / cellSize));
                int totalRows = (int) Math.ceil((double) selectedItems.size() / previewCols);
                int maxScroll = Math.max(0, totalRows - (this.height - 100) / cellSize);
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                previewScrollOffset = Math.max(0, previewScrollOffset - (int) verticalAmount);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                previewScrollOffset = Math.max(0, previewScrollOffset - (int) amount);
//#endif
                previewScrollOffset = Math.min(previewScrollOffset, maxScroll);
            }
            return true;
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        return super.mouseScrolled(mouseX, mouseY, amount);
//#endif
    }
}
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$
//$$
//$$
//#endif