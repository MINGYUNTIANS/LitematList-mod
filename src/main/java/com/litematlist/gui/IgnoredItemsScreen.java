package com.litematlist.gui;

import com.litematlist.I18n;
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
 * 已忽略物品列表界面。
 * 支持搜索筛选、按名称/总计排序、拖拽重排。
 */
public class IgnoredItemsScreen extends GuiBase {

    private final GuiBase parent;
    private final String schematicName;
    private final Path filePath;
    private final Set<Item> ignoredSet;
    private final Map<Item, Integer> ignoredTotals;
    private final List<Item> ignoredList;
    private int scrollOffset = 0;
    private static final int ROW_HEIGHT = 22;
    private static final int LIST_TOP = 48;
    private int visibleRows;
    private int hoveredRow = -1;

    // 搜索
    private static final Identifier SEARCH_ICON = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/search.png");
    private boolean searchActive = false;
    private String searchText = "";
    private EditBox searchField;

    // 排序
    private enum SortMode { NONE(""), TOTAL_DESC("▼"), TOTAL_ASC("▲"), NAME_ASC("▲"), NAME_DESC("▼");
        final String arrow; SortMode(String a) { this.arrow = a; }
    }
    private SortMode sortMode = SortMode.NONE;

    // 拖拽重排
    private int draggedRow = -1;
    private int dragStartY = 0;
    private boolean isDragging = false;

    public IgnoredItemsScreen(GuiBase parent, String schematicName, Path filePath, Set<Item> ignoredSet, Map<Item, Integer> ignoredTotals) {
        super();
        this.parent = parent;
        this.schematicName = schematicName;
        this.filePath = filePath;
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
            list.removeIf(item -> !item.getName(new ItemStack(item)).getString().toLowerCase().contains(searchText.toLowerCase()));
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
                Minecraft.getInstance().setScreenAndShow(parent));

        ButtonGeneric clearAllBtn = new ButtonGeneric(
                this.width / 2 + 70, buttonY, 100, 20, I18n.tr("litematlist.button.clear_all_ignored"));
        clearAllBtn.setRenderDefaultBackground(true);
        this.addButton(clearAllBtn, (IButtonActionListener) (b, mb) -> {
            ignoredSet.clear();
            ignoredList.clear();
            initGui();
        });

        // 搜索框
        if (searchField == null) {
            this.searchField = new EditBox(this.font, 28, 32, 80, 14, Component.empty());
            this.searchField.setMaxLength(50);
            this.searchField.setBordered(false);
            this.searchField.setValue(searchText);
            this.addRenderableWidget(this.searchField);
        } else {
            this.searchField.setX(28);
            this.searchField.setY(32);
            this.searchField.setValue(searchText);
            this.addRenderableWidget(this.searchField);
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
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks) {
        GuiContext ctx = GuiContext.fromGuiGraphics(gfx);
        this.extractBackground(gfx, mouseX, mouseY, partialTicks);

        ctx.drawCenteredString(
                this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);

        String countText = I18n.tr("litematlist.ignored.count", ignoredList.size());
        ctx.drawCenteredString(this.font,
                countText, this.width / 2, 24, 0xFFAAAAAA);

        if (ignoredList.isEmpty()) {
            ctx.drawCenteredString(
                    this.font, I18n.tr("litematlist.ignored.empty"),
                    this.width / 2, this.height / 2 - 10, 0xFFFFFFFF);
            super.extractRenderState(gfx, mouseX, mouseY, partialTicks);
            return;
        }

        ctx.fill(8, 30, this.width - 8, 46, 0x60000000);

        int listBottom = LIST_TOP + visibleRows * ROW_HEIGHT;
        ctx.fill(8, LIST_TOP - 2, this.width - 8, listBottom + 2, 0x40000000);

        List<Item> display = getDisplayList();
        hoveredRow = -1;
        int maxIdx = Math.min(display.size(), scrollOffset + visibleRows);
        for (int i = scrollOffset; i < maxIdx; i++) {
            Item item = display.get(i);
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;

            if (mouseX >= 10 && mouseX <= this.width - 10
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                hoveredRow = i;
                ctx.fill(10, rowY, this.width - 10, rowY + ROW_HEIGHT, 0x40FFFFFF);
            } else if ((i - scrollOffset) % 2 == 0) {
                ctx.fill(10, rowY, this.width - 10, rowY + ROW_HEIGHT, 0x10FFFFFF);
            }
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

        // 表头
        String nameHeader = I18n.tr("litematlist.header.item") + (sortMode == SortMode.NAME_ASC || sortMode == SortMode.NAME_DESC ? sortMode.arrow : "");
        ctx.drawString(this.font, nameHeader, searchActive ? 115 : 35, 33, 0xFFAAAAAA);
        ctx.drawString(this.font,
                I18n.tr("litematlist.header.total") + (sortMode == SortMode.TOTAL_DESC || sortMode == SortMode.TOTAL_ASC ? sortMode.arrow : ""),
                200, 33, 0xFFAAAAAA);

        for (int i = scrollOffset; i < maxIdx; i++) {
            Item item = display.get(i);
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;
            int total = ignoredTotals.getOrDefault(item, 0);

            ctx.renderItem(new ItemStack(item), searchActive ? 60 : 15, rowY + 2);
            ctx.drawString(this.font,
                    item.getName(new ItemStack(item)).getString(), searchActive ? 78 : 35, rowY + 5, 0xFFFFFFFF);
            ctx.drawString(this.font,
                    String.valueOf(total), 200, rowY + 5, 0xFFFFFFFF);
        }

        if (hoveredRow >= 0 && hoveredRow < display.size()) {
            Item item = display.get(hoveredRow);
            int total = ignoredTotals.getOrDefault(item, 0);
            int tipW = this.font.width(formatCountTooltip(total)) + 8;
            int tipX = mouseX + 12;
            int tipY = mouseY - 20;
            if (tipX + tipW > this.width) tipX = this.width - tipW - 4;
            if (tipY < 4) tipY = mouseY + 16;
            ctx.fill(tipX, tipY, tipX + tipW, tipY + 16, 0xCC000000);
            ctx.fill(tipX + 1, tipY + 1, tipX + tipW - 1, tipY + 15, 0xCC333333);
            ctx.drawString(this.font, formatCountTooltip(total), tipX + 4, tipY + 4, 0xFFFFFFFF);
        }

        if (display.size() > visibleRows) {
            int scrollInfoY = LIST_TOP + visibleRows * ROW_HEIGHT + 5;
            String scrollInfo = (scrollOffset + 1) + "-" +
                    Math.min(scrollOffset + visibleRows, display.size()) +
                    " / " + display.size();
            ctx.drawCenteredString(this.font,
                    scrollInfo, this.width / 2, scrollInfoY, 0xFFFFFFFF);
        }
    }

    private int getRowAtY(int mouseY) {
        if (mouseY < LIST_TOP) return -1;
        int row = (mouseY - LIST_TOP) / ROW_HEIGHT + scrollOffset;
        return row;
    }

    @Override
    public boolean onMouseClicked(MouseButtonEvent event, boolean isDrag) {
        if (super.onMouseClicked(event, isDrag)) return true;

        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        int button = event.button();

        if (button == 0) {
            // 搜索图标点击
            if (mouseX >= 10 && mouseX <= 24 && mouseY >= 30 && mouseY <= 46) {
                searchActive = !searchActive;
                if (searchActive && searchField != null) searchField.setFocused(true);
                if (!searchActive) { searchText = ""; if (searchField != null) searchField.setValue(""); }
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
                        ? Comparator.comparing((Item i) -> i.getName(new ItemStack(i)).getString(), String.CASE_INSENSITIVE_ORDER)
                        : Comparator.comparing((Item i) -> i.getName(new ItemStack(i)).getString(), String.CASE_INSENSITIVE_ORDER).reversed();
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
                int row = getRowAtY(mouseY);
                List<Item> display = getDisplayList();
                if (row >= 0 && row < display.size() && mouseX >= 10 && mouseX < this.width - 130) {
                    draggedRow = row;
                    dragStartY = mouseY;
                    isDragging = false;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        int mouseY = (int) event.y();
        int button = event.button();

        if (draggedRow >= 0 && button == 0 && Configs.Generic.ALLOW_MANUAL_REORDER.getBooleanValue()) {
            List<Item> display = getDisplayList();
            int currentRow = getRowAtY(mouseY);
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