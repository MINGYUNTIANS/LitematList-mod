package com.litematlist.gui;
//#if MC == 11701
//$$import net.minecraft.text.LiteralText;
//#endif

import com.litematlist.I18n;
import com.litematlist.config.Configs;
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
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.gui.DrawableHelper;
//#endif
//#if MC == 11701 || MC == 11904
//$$import com.mojang.blaze3d.systems.RenderSystem;
//#endif
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.util.math.MatrixStack;
//#endif
//#if MC == 12110 || MC == 12111
import net.minecraft.client.gui.Click;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$
//#endif
import net.minecraft.client.gui.widget.TextFieldWidget;
//#if MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.gl.RenderPipelines;
//#endif
//#if MC == 12110 || MC == 12111
import net.minecraft.client.input.KeyInput;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$
//#endif
//#if MC == 12110 || MC == 12111
import net.minecraft.client.input.CharInput;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$
//#endif
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.registry.Registries;
//#elseif MC == 11701
//$$import net.minecraft.util.registry.Registry;
//#endif
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.file.Path;
import java.util.*;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
/**
 * 已忽略物品列表界面。
 * 支持搜索筛选、按名称/总计排序、拖拽重排。
 */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$/**
//$$ * 宸插拷鐣ョ墿鍝佸垪琛ㄧ晫闈€? * 鏀寔鎼滅储绛涢€夈€佹寜鍚嶇О/鎬昏鎺掑簭銆佹嫋鎷介噸鎺掋€? */
//#endif
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

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // 搜索
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // 鎼滅储
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final Identifier SEARCH_ICON = Identifier.of("litematlist", "textures/gui/search.png");
//#elseif MC == 11701
//$$    private static final Identifier SEARCH_ICON = new Identifier("litematlist", "textures/gui/search.png");
//#endif
    private boolean searchActive = false;
    private String searchText = "";
    private TextFieldWidget searchField;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // 排序
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // 鎺掑簭
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private enum SortMode { NONE(""), TOTAL_DESC("▼"), TOTAL_ASC("▲"), NAME_ASC("▲"), NAME_DESC("▼");
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    private enum SortMode { NONE(""), TOTAL_DESC("\u25BC"), TOTAL_ASC("\u25B2"), NAME_ASC("\u25B2"), NAME_DESC("\u25BC");
//#endif
        final String arrow; SortMode(String a) { this.arrow = a; }
    }
    private SortMode sortMode = SortMode.NONE;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // 拖拽重排
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // 鎷栨嫿閲嶆帓
//#endif
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
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 初始按总计数量从高到低排列
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鍒濆鎸夋€昏鏁伴噺浠庨珮鍒颁綆鎺掑垪
//#endif
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
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 同时清除原始持久化数据
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // Also clear persisted data
//#endif
            java.util.Set<String> persisted = com.litematlist.gui.MaterialDataStore.rawMaterialIgnored.get(pathKey);
            if (persisted != null) {
                persisted.clear();
            }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 同时清除源级忽略
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 鍚屾椂娓呴櫎婧愮骇蹇界暐
//#endif
            java.util.Set<String> srcIgnored = com.litematlist.gui.MaterialDataStore.rawMaterialSourceIgnored.get(pathKey);
            if (srcIgnored != null) {
                srcIgnored.clear();
            }
            com.litematlist.gui.MaterialDataStore.savePersistence();
            // 立刻通知 HUD：清除全部忽略后所有材料恢复显示（与单行取消忽略同路径）。
            // 必须用 forceRefreshHud 绕过 200ms 冷却，否则紧跟在其它刷新之后时会被吞掉。
            com.litematlist.gui.MaterialListHudRenderer.forceRefreshHud();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 清除缓存的原材料分析数据，下次打开时重新分析
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // Clear cached raw material analysis data, re-analyze next time
//#endif
            com.litematlist.gui.MaterialDataStore.rawMaterials.remove(pathKey);
            com.litematlist.gui.MaterialDataStore.rawMaterialRedundancy.remove(pathKey);
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 通知原材料列表刷新
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // Notify raw material list to refresh
//#endif
            if (parent instanceof RawMaterialScreen rawScreen) {
                rawScreen.rebuildRows();
            }
            initGui();
        });

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 搜索框
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // Search box
//#endif
        if (searchField == null) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            this.searchField = new TextFieldWidget(this.textRenderer, 28, 32, 80, 14, Text.empty());
//#elseif MC == 11701
//$$            this.searchField = new TextFieldWidget(this.textRenderer, 28, 32, 80, 14, LiteralText.EMPTY);
//#endif
            this.searchField.setMaxLength(50);
            this.searchField.setDrawsBackground(false);
            this.searchField.setText(searchText);
            this.addSelectableChild(this.searchField);
        } else {
            this.searchField.setX(28);
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            this.searchField.setY(32);
//#elseif MC == 11701
//$$            this.searchField.y = 32;
//#endif
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
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 同时从原始持久化数据中移除
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                // 鍚屾椂浠庡師濮嬫寔涔呭寲鏁版嵁涓Щ闄?
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                String itemId = Registries.ITEM.getId(targetItem).toString();
//#elseif MC == 11701
//$$                String itemId = Registry.ITEM.getId(targetItem).toString();
//#endif
                java.util.Set<String> persisted = com.litematlist.gui.MaterialDataStore.rawMaterialIgnored.get(pathKey);
                if (persisted != null) {
                    persisted.remove(itemId);
                }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 同时清除该物品的源级忽略（仅清除该物品相关的）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                // Also clear source-level ignore for this item
//#endif
                java.util.Set<String> srcIgnored = com.litematlist.gui.MaterialDataStore.rawMaterialSourceIgnored.get(pathKey);
                if (srcIgnored != null) {
                    List<com.litematlist.gui.MaterialDataStore.RawMaterialEntry> rawEntries =
                        com.litematlist.gui.MaterialDataStore.rawMaterials.get(pathKey);
                    if (rawEntries != null) {
                        for (com.litematlist.gui.MaterialDataStore.RawMaterialEntry entry : rawEntries) {
                            if (entry.itemId().equals(itemId)) {
                                srcIgnored.removeAll(entry.sources());
                            }
                        }
                    }
                }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                com.litematlist.gui.MaterialListHudRenderer.refreshHud(); // 取消忽略后更新HUD
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                com.litematlist.gui.MaterialListHudRenderer.refreshHud(); // 鍙栨秷蹇界暐鍚庢洿鏂癏UD
//#endif
                com.litematlist.gui.MaterialDataStore.savePersistence(); // 保存持久化
                // 清除缓存的原材料分析数据，下次打开时重新分析
                com.litematlist.gui.MaterialDataStore.rawMaterials.remove(pathKey);
                com.litematlist.gui.MaterialDataStore.rawMaterialRedundancy.remove(pathKey);
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 通知原材料列表刷新
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                // Notify raw material list to refresh
//#endif
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
        drawContext.drawCenteredTextWithShadow(
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$        this.textRenderer.drawWithShadow(matrices, this.title, this.width / 2 - this.textRenderer.getWidth(this.title) / 2, 12, 0xFFFFFFFF);
//#endif

        String countText = I18n.tr("litematlist.ignored.count", ignoredList.size());
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawCenteredTextWithShadow(this.textRenderer,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                countText, this.width / 2, 24, 0xFFAAAAAA);
//#endif
//#if MC == 11701 || MC == 11904
//$$        this.textRenderer.drawWithShadow(matrices, countText, this.width / 2 - this.textRenderer.getWidth(countText) / 2, 24, 0xFFAAAAAA);
//#endif

        if (ignoredList.isEmpty()) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawCenteredTextWithShadow(
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    this.textRenderer, I18n.tr("litematlist.ignored.empty"),
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    this.width / 2, this.height / 2 - 10, 0xFFFFFFFF);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            super.render(drawContext, mouseX, mouseY, partialTicks);
//#endif
//#if MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, I18n.tr("litematlist.ignored.empty"), this.width / 2 - this.textRenderer.getWidth(I18n.tr("litematlist.ignored.empty")) / 2, this.height / 2 - 10, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$            super.render(matrices, mouseX, mouseY, partialTicks);
//#endif
            return;
        }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(8, 30, this.width - 8, 46, 0x60000000);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, 8, 30, this.width - 8, 46, 0x60000000);
//#endif

        int listBottom = LIST_TOP + visibleRows * ROW_HEIGHT;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(8, LIST_TOP - 2, this.width - 8, listBottom + 2, 0x40000000);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, 8, LIST_TOP - 2, this.width - 8, listBottom + 2, 0x40000000);
//#endif

        List<Item> display = getDisplayList();
        hoveredRow = -1;
        int maxIdx = Math.min(display.size(), scrollOffset + visibleRows);
        for (int i = scrollOffset; i < maxIdx; i++) {
            Item item = display.get(i);
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;

            if (mouseX >= 10 && mouseX <= this.width - 10
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                hoveredRow = i;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.fill(10, rowY, this.width - 10, rowY + ROW_HEIGHT, 0x40FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$                DrawableHelper.fill(matrices, 10, rowY, this.width - 10, rowY + ROW_HEIGHT, 0x40FFFFFF);
//#endif
            } else if ((i - scrollOffset) % 2 == 0) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.fill(10, rowY, this.width - 10, rowY + ROW_HEIGHT, 0x10FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$                DrawableHelper.fill(matrices, 10, rowY, this.width - 10, rowY + ROW_HEIGHT, 0x10FFFFFF);
//#endif
            }
        }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        super.render(drawContext, mouseX, mouseY, partialTicks);
//#elseif MC == 11701 || MC == 11904
//$$        super.render(matrices, mouseX, mouseY, partialTicks);
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 搜索图标
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鎼滅储鍥炬爣
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, SEARCH_ICON, 10, 33, 0.0f, 0.0f, 14, 14, 14, 14);
//#elseif MC == 11701 || MC == 11904
//$$        RenderSystem.setShaderTexture(0, SEARCH_ICON); DrawableHelper.drawTexture(matrices, 10, 33, 0.0f, 0.0f, 14, 14, 14, 14);
//#elseif MC == 12001
//$$        drawContext.drawTexture(SEARCH_ICON, 10, 33, 0.0f, 0.0f, 14, 14, 14, 14);
//#elseif MC == 12101
//$$        drawContext.drawTexture( SEARCH_ICON, 10, 33, 0.0f, 0.0f, 14, 14, 14, 14);
//#elseif MC == 12104
//$$        drawContext.drawTexture(RenderLayer::getGuiTextured, SEARCH_ICON, 10, 33, 0.0f, 0.0f, 14, 14, 14, 14);
//#endif
        if (searchActive && searchField != null) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(28, 31, 28 + 82, 46, 0xFF000000);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, 28, 31, 28 + 82, 46, 0xFF000000);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(29, 32, 29 + 80, 45, 0xFF222222);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, 29, 32, 29 + 80, 45, 0xFF222222);
//#endif
            String st = searchField.getText();
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawText(this.textRenderer, st, 31, 34, 0xFFFFFFFF, false);
//#elseif MC == 11701 || MC == 11904
//$$            this.textRenderer.draw(matrices, st, 31, 34, 0xFFFFFFFF);
//#endif
            if (searchField.isFocused() && System.currentTimeMillis() % 1000 < 500) {
                int cp = searchField.getCursor();
                String bc = st.substring(0, Math.min(cp, st.length()));
                int cx = 31 + this.textRenderer.getWidth(bc);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.fill(cx, 33, cx + 1, 43, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$                DrawableHelper.fill(matrices, cx, 33, cx + 1, 43, 0xFFFFFFFF);
//#endif
            }
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 表头
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 琛ㄥご
//#endif
        String nameHeader = I18n.tr("litematlist.header.item") + (sortMode == SortMode.NAME_ASC || sortMode == SortMode.NAME_DESC ? sortMode.arrow : "");
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawTextWithShadow(this.textRenderer, nameHeader, searchActive ? 115 : 35, 33, 0xFFAAAAAA);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawTextWithShadow(this.textRenderer,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                I18n.tr("litematlist.header.total") + (sortMode == SortMode.TOTAL_DESC || sortMode == SortMode.TOTAL_ASC ? sortMode.arrow : ""),
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                200, 33, 0xFFAAAAAA);
//#endif
//#if MC == 11701 || MC == 11904
//$$        this.textRenderer.drawWithShadow(matrices, nameHeader, searchActive ? 115 : 35, 33, 0xFFAAAAAA);
//#endif
//#if MC == 11701 || MC == 11904
//$$        this.textRenderer.drawWithShadow(matrices, I18n.tr("litematlist.header.total") + (sortMode == SortMode.TOTAL_DESC || sortMode == SortMode.TOTAL_ASC ? sortMode.arrow : ""), 200, 33, 0xFFAAAAAA);
//#endif

        for (int i = scrollOffset; i < maxIdx; i++) {
            Item item = display.get(i);
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;
            int total = ignoredTotals.getOrDefault(item, 0);

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawItem(new ItemStack(item), searchActive ? 60 : 15, rowY + 2);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTextWithShadow(this.textRenderer,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    item.getName().getString(), searchActive ? 78 : 35, rowY + 5, 0xFFFFFFFF);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTextWithShadow(this.textRenderer,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    String.valueOf(total), 200, rowY + 5, 0xFFFFFFFF);
//#endif
//#if MC == 11701
//$$            MinecraftClient.getInstance().getItemRenderer().renderInGui(new ItemStack(item), searchActive ? 60 : 15, rowY + 2);
//#elseif MC == 11904
//$$            MinecraftClient.getInstance().getItemRenderer().renderInGui(matrices, new ItemStack(item), searchActive ? 60 : 15, rowY + 2);
//#endif
//#if MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, item.getName().getString(), searchActive ? 78 : 35, rowY + 5, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, String.valueOf(total), 200, rowY + 5, 0xFFFFFFFF);
//#endif
        }

        if (hoveredRow >= 0 && hoveredRow < display.size()) {
            Item item = display.get(hoveredRow);
            int total = ignoredTotals.getOrDefault(item, 0);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTooltip(this.textRenderer, Text.literal(formatCountTooltip(total)), mouseX, mouseY);
//#elseif MC == 11701
//$$            this.renderTooltip(matrices, new LiteralText(formatCountTooltip(total)), mouseX, mouseY);
//#elseif MC == 11904
//$$            this.renderTooltip(matrices, Text.literal(formatCountTooltip(total)), mouseX, mouseY);
//#endif
        }

        if (display.size() > visibleRows) {
            int scrollInfoY = LIST_TOP + visibleRows * ROW_HEIGHT + 5;
            String scrollInfo = (scrollOffset + 1) + "-" +
                    Math.min(scrollOffset + visibleRows, display.size()) +
                    " / " + display.size();
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawCenteredTextWithShadow(this.textRenderer,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    scrollInfo, this.width / 2, scrollInfoY, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, scrollInfo, this.width / 2 - this.textRenderer.getWidth(scrollInfo) / 2, scrollInfoY, 0xFFFFFFFF);
//#endif
        }
    }

    private int getRowAtY(int mouseY) {
        if (mouseY < LIST_TOP) return -1;
        int row = (mouseY - LIST_TOP) / ROW_HEIGHT + scrollOffset;
        return row;
    }

    @Override
//#if MC == 12110 || MC == 12111
    public boolean onMouseClicked(Click click, boolean isDrag) {
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$    public boolean onMouseClicked(int mouseX, int mouseY, int button) {
//#endif
//#if MC == 12110 || MC == 12111
        if (super.onMouseClicked(click, isDrag)) return true;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        if (super.onMouseClicked(mouseX, mouseY, button)) return true;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12110 || MC == 12111
        int mouseX = (int) click.x();
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        
//#endif
//#if MC == 12110 || MC == 12111
        int mouseY = (int) click.y();
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        
//#endif
//#if MC == 12110 || MC == 12111
        int button = click.button();
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (super.mouseClicked(mouseX, mouseY, mouseButton)) return true;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        int button = mouseButton;
//#endif

        if (button == 0) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 搜索图标点击
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 鎼滅储鍥炬爣鐐瑰嚮
//#endif
            if (mouseX >= 10 && mouseX <= 24 && mouseY >= 30 && mouseY <= 46) {
                searchActive = !searchActive;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (searchActive && searchField != null) searchField.setFocused(true);
//#elseif MC == 11701
//$$                if (searchActive && searchField != null) this.setFocused(searchField);
//#endif
                if (!searchActive) { searchText = ""; if (searchField != null) searchField.setText(""); }
                scrollOffset = 0;
                initGui();
                return true;
            }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 表头点击排序 - 物品名称
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 琛ㄥご鐐瑰嚮鎺掑簭 - 鐗╁搧鍚嶇О
//#endif
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

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 表头点击排序 - 总计
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 琛ㄥご鐐瑰嚮鎺掑簭 - 鎬昏
//#endif
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

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 拖拽检测
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // Drag detection
//#endif
            if (Configs.Generic.ALLOW_MANUAL_REORDER.getBooleanValue()) {
//#if MC == 12110 || MC == 12111
                int row = getRowAtY(mouseY);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                int row = getRowAtY((int)mouseY);
//#endif
                List<Item> display = getDisplayList();
                if (row >= 0 && row < display.size() && mouseX >= 10 && mouseX < this.width - 130) {
                    draggedRow = row;
//#if MC == 12110 || MC == 12111
                    dragStartY = mouseY;
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                    dragStartY = (int)mouseY;
//#endif
                    isDragging = false;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
//#if MC == 12110 || MC == 12111
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
//#endif
//#if MC == 12110 || MC == 12111
        int mouseY = (int) click.y();
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        mouseY = (int) mouseY;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        
//#endif
//#if MC == 12110 || MC == 12111
        int button = click.button();
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        int button = mouseButton;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        
//#endif

        if (draggedRow >= 0 && button == 0 && Configs.Generic.ALLOW_MANUAL_REORDER.getBooleanValue()) {
            List<Item> display = getDisplayList();
//#if MC == 12110 || MC == 12111
            int currentRow = getRowAtY(mouseY);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            int currentRow = getRowAtY((int)mouseY);
//#endif
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
//#if MC == 12110 || MC == 12111
                dragStartY = mouseY;
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                dragStartY = (int)mouseY;
//#endif
            }
            return true;
        }
//#if MC == 12110 || MC == 12111
        return super.mouseDragged(click, deltaX, deltaY);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        return super.mouseDragged(mouseX, mouseY, mouseButton, deltaX, deltaY);
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
//#endif
    }

    @Override
//#if MC == 12110 || MC == 12111
    public boolean mouseReleased(Click click) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$    public boolean mouseReleased(double mouseX, double mouseY, int button) {
//#endif
        if (draggedRow >= 0) {
            draggedRow = -1;
            isDragging = false;
            return true;
        }
//#if MC == 12110 || MC == 12111
        return super.mouseReleased(click);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        return super.mouseReleased(mouseX, mouseY, mouseButton);
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        return super.mouseReleased(mouseX, mouseY, button);
//#endif
    }

    @Override
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
//#endif
        if (mouseY >= LIST_TOP && mouseY <= LIST_TOP + visibleRows * ROW_HEIGHT) {
            List<Item> display = getDisplayList();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            scrollOffset = Math.max(0, scrollOffset - (int) verticalAmount);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            scrollOffset = Math.max(0, scrollOffset - (int) amount);
//#endif
            int maxOffset = Math.max(0, display.size() - visibleRows);
            scrollOffset = Math.min(scrollOffset, maxOffset);
            initGui();
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
//#if MC == 12110 || MC == 12111
        if (keyInput.key() == 256) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        if (keyCode == 256) {
//#endif
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
//#if MC == 12110 || MC == 12111
            if (searchField.keyPressed(keyInput)) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            if (searchField.keyPressed(keyCode, scanCode, modifiers)) {
//#endif
                searchText = searchField.getText();
                initGui();
                return true;
            }
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
        if (searchActive && searchField != null && searchField.isFocused()) {
//#if MC == 12110 || MC == 12111
            if (searchField.charTyped(charInput)) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            if (searchField.charTyped(chr, modifiers)) {
//#endif
                searchText = searchField.getText();
                initGui();
                return true;
            }
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
//$$
//$$
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$
//$$
//#endif