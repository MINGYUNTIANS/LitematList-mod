package com.litematlist.gui;

import com.litematlist.I18n;
import com.litematlist.config.Configs;
import com.litematlist.config.RawMaterialConfig;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.MinecraftClient;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.gui.DrawContext;
//#endif
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.registry.Registries;
//#elseif MC == 11701
//$$import net.minecraft.util.registry.Registry;
//#endif
import net.minecraft.text.Text;

import java.util.*;
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.gui.DrawableHelper;
//#endif
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.util.math.MatrixStack;
//#endif

/**
 * 原材料配置界面：白名单管理（物品图标网格 + 删除确认）、配方优先级排序（滑块 + 锁定 + 启用/禁用复选框）。
 */
public class RawMaterialConfigScreen extends GuiBase {

    private final GuiBase parent;
    private static final int ICON_SIZE = 18;
    private static final int ICON_GAP = 2;
    private static final int GRID_COLS = 10;
    private static final int GRID_LEFT = 20;
    private static final int LIST_TOP = 80;
    private static final int ROW_HEIGHT = 22;

    private int scrollOffset = 0;
    private int visibleRows;

    private final List<Item> whitelistItems;
    private final Set<Item> whitelistSet;

    // 配方类型排序列表（直接引用 RawMaterialConfig.recipeTypeOrder）
    private final List<RawMaterialConfig.RecipeTypeConfig> recipeOrder;
    private boolean priorityLocked = false;

    // 配方优先级滑块区域
    private static final int SLIDER_LEFT = 0;
    private static final int SLIDER_TOP = 80;
    private static final int SLIDER_HEIGHT = 30;
    private static final int SLIDER_GAP = 6;
    private static final int CHECKBOX_SIZE = 12;

    // 删除确认
    private Item pendingDeleteItem = null;

    // 重置确认
    private boolean showResetConfirm = false;

    public RawMaterialConfigScreen(GuiBase parent) {
        super();
        this.parent = parent;
        this.title = I18n.tr("litematlist.title.raw_material_config");
        this.whitelistSet = new LinkedHashSet<>(RawMaterialConfig.currentWhitelist);
        this.whitelistItems = new ArrayList<>(this.whitelistSet);
        sortWhitelistByRegistry();
        this.recipeOrder = RawMaterialConfig.recipeTypeOrder;
    }

    private void sortWhitelistByRegistry() {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        whitelistItems.sort(Comparator.comparingInt(item -> Registries.ITEM.getRawId(item)));
//#elseif MC == 11701
//$$        whitelistItems.sort(Comparator.comparingInt(item -> Registry.ITEM.getRawId(item)));
//#endif
    }

    @Override
    public void initGui() {
        super.initGui();
        this.visibleRows = (this.height - 150) / ROW_HEIGHT;

        int buttonY = this.height - 38;

        // 返回
        ButtonGeneric backBtn = new ButtonGeneric(this.width / 2 - 60, buttonY, 120, 20, I18n.tr("litematlist.button.back"));
        backBtn.setRenderDefaultBackground(true);
        this.addButton(backBtn, (IButtonActionListener) (b, mb) -> {
            saveConfig();
            MinecraftClient.getInstance().setScreen(parent);
        });

        // 重置按钮（左下角）
        ButtonGeneric resetBtn = new ButtonGeneric(20, buttonY, 60, 20, I18n.tr("litematlist.button.reset"));
        resetBtn.setRenderDefaultBackground(true);
        this.addButton(resetBtn, (IButtonActionListener) (b, mb) -> {
            showResetConfirm = true;
        });

        // 添加物品按钮
        String addLabel = I18n.tr("litematlist.button.add_whitelist_item");
        int addW = this.textRenderer.getWidth(addLabel) + 16;
        ButtonGeneric addBtn = new ButtonGeneric(20, 38, addW, 20, addLabel);
        addBtn.setRenderDefaultBackground(true);
        this.addButton(addBtn, (IButtonActionListener) (b, mb) -> {
            MinecraftClient.getInstance().setScreen(new MultiBlockPickerScreen(this, (selected) -> {
                for (Item item : selected) {
                    whitelistSet.add(item);
                }
                whitelistItems.clear();
                whitelistItems.addAll(whitelistSet);
                sortWhitelistByRegistry();
                initGui();
            }));
        });

        // 锁定/解锁开关（右上角）
        String lockLabel = priorityLocked ? I18n.tr("litematlist.button.unlocked") : I18n.tr("litematlist.button.locked");
        int lockW = this.textRenderer.getWidth(lockLabel) + 16;
        int lockX = this.width - lockW - 10;
        ButtonGeneric lockBtn = new ButtonGeneric(lockX, 25, lockW, 20, lockLabel);
        lockBtn.setRenderDefaultBackground(true);
        this.addButton(lockBtn, (IButtonActionListener) (b, mb) -> {
            priorityLocked = !priorityLocked;
            initGui();
        });

        // 不启用的配方自动置底
        recipeOrder.sort((a, b) -> {
            if (a.enabled == b.enabled) return 0;
            return a.enabled ? -1 : 1;
        });

        // 配方优先级上/下按钮（解锁时显示，仅对启用项）
        if (!priorityLocked) {
            int sliderAreaLeft = this.width - 180;
            int sliderAreaWidth = 160;
            int sliderItemHeight = SLIDER_HEIGHT;
            for (int i = 0; i < recipeOrder.size(); i++) {
                RawMaterialConfig.RecipeTypeConfig cfg = recipeOrder.get(i);
                if (!cfg.enabled) continue;
                int y = SLIDER_TOP + i * (sliderItemHeight + SLIDER_GAP);
                // 上移：仅当上一项也是启用的
                if (i > 0 && recipeOrder.get(i - 1).enabled) {
                    ButtonGeneric upBtn = new ButtonGeneric(sliderAreaLeft + sliderAreaWidth - 45, y + 2, 18, 12, "▲");
                    upBtn.setRenderDefaultBackground(true);
                    final int idx = i;
                    this.addButton(upBtn, (IButtonActionListener) (b, mb) -> {
                        Collections.swap(recipeOrder, idx, idx - 1);
                        initGui();
                    });
                }
                // 下移：仅当下一项也是启用的
                if (i < recipeOrder.size() - 1 && recipeOrder.get(i + 1).enabled) {
                    ButtonGeneric downBtn = new ButtonGeneric(sliderAreaLeft + sliderAreaWidth - 24, y + 2, 18, 12, "▼");
                    downBtn.setRenderDefaultBackground(true);
                    final int idx = i;
                    this.addButton(downBtn, (IButtonActionListener) (b, mb) -> {
                        Collections.swap(recipeOrder, idx, idx + 1);
                        initGui();
                    });
                }
            }
        }
    }

    private void saveConfig() {
        RawMaterialConfig.currentWhitelist = new HashSet<>(whitelistSet);
    }

    // 阻止 MaLib 在 super.render() 中重复绘制全屏背景（背景已在本类 render 开头绘制，
    // 否则全屏深色背景会覆盖白名单物品图标）
    @Override
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110
    protected void drawScreenBackground(DrawContext drawContext, int mouseX, int mouseY) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    protected void drawScreenBackground(int mouseX, int mouseY) {
//#elseif MC == 12111
//$$    protected void drawScreenBackground(fi.dy.masa.malilib.render.GuiContext ctx, int mouseX, int mouseY) {
//#endif
    }

    @Override
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
//#elseif MC == 11701 || MC == 11904
//$$    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
//#endif
        // 全屏深色背景（最先绘制，否则 MaLib 背景会覆盖白名单物品）
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(0, 0, this.width, this.height, 0xB0000000);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, 0, 0, this.width, this.height, 0xB0000000);
//#endif
        // 先绘制所有背景，再绘制按钮，最后绘制文字
        int sliderAreaLeft = this.width - 180;
        int sliderAreaWidth = 160;
        int sliderItemHeight = SLIDER_HEIGHT;

        // 配方优先级滑块背景（右侧）
        // 注：排序标签移至 super.render() 之后渲染以确保最高图层

        for (int i = 0; i < recipeOrder.size(); i++) {
            int y = SLIDER_TOP + i * (sliderItemHeight + SLIDER_GAP);
            int bgColor = 0x40000000;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(sliderAreaLeft, y, sliderAreaLeft + sliderAreaWidth, y + sliderItemHeight, bgColor);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, sliderAreaLeft, y, sliderAreaLeft + sliderAreaWidth, y + sliderItemHeight, bgColor);
//#endif
        }

        // 白名单网格背景
        int startX = GRID_LEFT;
        int startY = LIST_TOP;
        int col = 0, row = 0;
        for (int i = scrollOffset; i < whitelistItems.size(); i++) {
            int x = startX + col * (ICON_SIZE + ICON_GAP);
            int y = startY + row * (ICON_SIZE + ICON_GAP + 2);
            if (y + ICON_SIZE > this.height - 50) break;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, 0x40000000);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, x, y, x + ICON_SIZE, y + ICON_SIZE, 0x40000000);
//#endif
            col++;
            if (col >= GRID_COLS) { col = 0; row++; }
        }

        // 第2层：白名单网格物品（绘制于黑框之下，防止物品覆盖黑框）
        col = 0; row = 0;
        for (int i = scrollOffset; i < whitelistItems.size(); i++) {
            Item item = whitelistItems.get(i);
            int x = startX + col * (ICON_SIZE + ICON_GAP);
            int y = startY + row * (ICON_SIZE + ICON_GAP + 2);
            if (y + ICON_SIZE > this.height - 50) break;

            // 物品图标
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawItem(new ItemStack(item), x + 1, y + 1);
//#elseif MC == 11701
//$$            MinecraftClient.getInstance().getItemRenderer().renderInGui(new ItemStack(item), x + 1, y + 1);
//#elseif MC == 11904
//$$            MinecraftClient.getInstance().getItemRenderer().renderInGui(matrices, new ItemStack(item), x + 1, y + 1);
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 删除按钮（右上角红叉）——绘制于物品图标之后，按绘制顺序覆盖在最上层
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            // 删除按钮（右上角红叉）——提升图层绘制在物品图标之上
//#endif
//#if MC == 11701 || MC == 11904
//$$            matrices.push();
//#elseif MC == 12001
//$$            drawContext.getMatrices().push();
//#endif
//#if MC == 11701 || MC == 11904
//$$            matrices.translate(0.0F, 0.0F, 300.0F);
//#elseif MC == 12001
//$$            drawContext.getMatrices().translate(0.0F, 0.0F, 300.0F);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(x + ICON_SIZE - 7, y, x + ICON_SIZE, y + 7, 0x80FF0000);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, x + ICON_SIZE - 7, y, x + ICON_SIZE, y + 7, 0x80FF0000);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTextWithShadow(this.textRenderer, "×", x + ICON_SIZE - 6, y + 1, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, "×", x + ICON_SIZE - 6, y + 1, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$            matrices.pop();
//#elseif MC == 12001
//$$            drawContext.getMatrices().pop();
//#endif

            col++;
            if (col >= GRID_COLS) { col = 0; row++; }
        }

        // 第3层：确认对话框（黑框与文字，绘制于物品之上、按钮之下）
        if (pendingDeleteItem != null || showResetConfirm) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            renderConfirmDialogBackground(drawContext);
//#elseif MC == 11701 || MC == 11904
//$$            renderConfirmDialogBackground(matrices);
//#endif
        }
        // 删除确认对话框
        if (pendingDeleteItem != null) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            renderDeleteConfirmDialog(drawContext, mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$            renderDeleteConfirmDialog(matrices, mouseX, mouseY);
//#endif
        }
        // 重置确认对话框
        if (showResetConfirm) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            renderResetConfirmDialog(drawContext, mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$            renderResetConfirmDialog(matrices, mouseX, mouseY);
//#endif
        }

        // 第4层：按钮（最上层，防止黑框覆盖按钮）
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        super.render(drawContext, mouseX, mouseY, delta);
//#elseif MC == 11701 || MC == 11904
//$$        super.render(matrices, mouseX, mouseY, delta);
//#endif

        // 第3层：文字和图标
        // 配方优先级标签（右上角）
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawTextWithShadow(this.textRenderer, I18n.tr("litematlist.label.recipe_priority"),
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                sliderAreaLeft, 20, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$        this.textRenderer.drawWithShadow(matrices, I18n.tr("litematlist.label.recipe_priority"), sliderAreaLeft, 20, 0xFFFFFFFF);
//#endif

        // 排序提示（纯白色，最高图层）
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawText(this.textRenderer, I18n.tr("litematlist.label.recipe_priority_order"),
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                sliderAreaLeft, SLIDER_TOP - 14, 0xFFFFFFFF, false);
//#endif
//#if MC == 11701 || MC == 11904
//$$        this.textRenderer.draw(matrices, I18n.tr("litematlist.label.recipe_priority_order"), sliderAreaLeft, SLIDER_TOP - 14, 0xFFFFFFFF);
//#endif

        // 锁定状态提示（右侧配方优先级区域下方）
        if (priorityLocked) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTextWithShadow(this.textRenderer, I18n.tr("litematlist.label.priority_locked_hint"),
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    sliderAreaLeft, SLIDER_TOP + recipeOrder.size() * (SLIDER_HEIGHT + SLIDER_GAP) + 4, 0xAAAAAA);
//#endif
//#if MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, I18n.tr("litematlist.label.priority_locked_hint"), sliderAreaLeft, SLIDER_TOP + recipeOrder.size() * (SLIDER_HEIGHT + SLIDER_GAP) + 4, 0xAAAAAA);
//#endif
        }

        // 配方优先级滑块文字 + 复选框
        for (int i = 0; i < recipeOrder.size(); i++) {
            int y = SLIDER_TOP + i * (sliderItemHeight + SLIDER_GAP);
            RawMaterialConfig.RecipeTypeConfig config = recipeOrder.get(i);

            // 复选框 [X] 或 [ ]
            int checkboxX = sliderAreaLeft + 4;
            int checkboxY = y + 9;
            String checkboxText = config.enabled ? "☑" : "☐";
            int checkboxColor = config.enabled ? 0xFF55FF55 : 0xFFAAAAAA;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTextWithShadow(this.textRenderer, checkboxText, checkboxX, checkboxY, checkboxColor);
//#elseif MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, checkboxText, checkboxX, checkboxY, checkboxColor);
//#endif

            if (!priorityLocked) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.drawTextWithShadow(this.textRenderer, "≡ ", sliderAreaLeft + 18, y + 8, 0xAAAAAA);
//#elseif MC == 11701 || MC == 11904
//$$                this.textRenderer.drawWithShadow(matrices, "≡ ", sliderAreaLeft + 18, y + 8, 0xAAAAAA);
//#endif
            }
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTextWithShadow(this.textRenderer, config.getDisplayName(), sliderAreaLeft + 34, y + 8, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, config.getDisplayName(), sliderAreaLeft + 34, y + 8, 0xFFFFFFFF);
//#endif
        }

        // 白名单标题
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawTextWithShadow(this.textRenderer, I18n.tr("litematlist.label.whitelist_title"),
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                GRID_LEFT, LIST_TOP - 14, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$        this.textRenderer.drawWithShadow(matrices, I18n.tr("litematlist.label.whitelist_title"), GRID_LEFT, LIST_TOP - 14, 0xFFFFFFFF);
//#endif

        // 悬停提示（最高图层）
        col = 0; row = 0;
        for (int i = scrollOffset; i < whitelistItems.size(); i++) {
            Item item = whitelistItems.get(i);
            int x = startX + col * (ICON_SIZE + ICON_GAP);
            int y = startY + row * (ICON_SIZE + ICON_GAP + 2);
            if (y + ICON_SIZE > this.height - 50) break;
            if (mouseX >= x && mouseX <= x + ICON_SIZE && mouseY >= y && mouseY <= y + ICON_SIZE) {
                String tooltip = item.getName().getString();
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.drawTooltip(this.textRenderer, Text.of(tooltip), mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$                this.renderTooltip(matrices, Text.of(tooltip), mouseX, mouseY);
//#endif
            }
            col++;
            if (col >= GRID_COLS) { col = 0; row++; }
        }
    }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void renderConfirmDialogBackground(DrawContext drawContext) {
//#elseif MC == 11701 || MC == 11904
//$$    private void renderConfirmDialogBackground(MatrixStack matrices) {
//#endif
        int dialogW = 240;
        int dialogH = 80;
        int dialogX = (this.width - dialogW) / 2;
        int dialogY = (this.height - dialogH) / 2;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(dialogX, dialogY, dialogX + dialogW, dialogY + dialogH, 0xE0000000);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, dialogX, dialogY, dialogX + dialogW, dialogY + dialogH, 0xE0000000);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(dialogX, dialogY, dialogX + dialogW, dialogY + 1, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, dialogX, dialogY, dialogX + dialogW, dialogY + 1, 0xFFFFFFFF);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(dialogX, dialogY + dialogH - 1, dialogX + dialogW, dialogY + dialogH, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, dialogX, dialogY + dialogH - 1, dialogX + dialogW, dialogY + dialogH, 0xFFFFFFFF);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(dialogX, dialogY, dialogX + 1, dialogY + dialogH, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, dialogX, dialogY, dialogX + 1, dialogY + dialogH, 0xFFFFFFFF);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(dialogX + dialogW - 1, dialogY, dialogX + dialogW, dialogY + dialogH, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, dialogX + dialogW - 1, dialogY, dialogX + dialogW, dialogY + dialogH, 0xFFFFFFFF);
//#endif
    }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void renderDeleteConfirmDialog(DrawContext drawContext, int mouseX, int mouseY) {
//#elseif MC == 11701 || MC == 11904
//$$    private void renderDeleteConfirmDialog(MatrixStack matrices, int mouseX, int mouseY) {
//#endif
        int dialogW = 240;
        int dialogH = 80;
        int dialogX = (this.width - dialogW) / 2;
        int dialogY = (this.height - dialogH) / 2;

        String msg = I18n.tr("litematlist.confirm.delete_whitelist",
                pendingDeleteItem.getName().getString());
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawCenteredTextWithShadow(this.textRenderer, msg, this.width / 2, dialogY + 15, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$        this.textRenderer.drawWithShadow(matrices, msg, this.width / 2 - this.textRenderer.getWidth(msg) / 2, dialogY + 15, 0xFFFFFFFF);
//#endif

        // 确认按钮
        ButtonGeneric confirmBtn = new ButtonGeneric(dialogX + 30, dialogY + 45, 80, 20, I18n.tr("litematlist.button.confirm"));
        confirmBtn.setRenderDefaultBackground(true);
        this.addButton(confirmBtn, (IButtonActionListener) (b, mb) -> {
            whitelistSet.remove(pendingDeleteItem);
            whitelistItems.clear();
            whitelistItems.addAll(whitelistSet);
            sortWhitelistByRegistry();
            pendingDeleteItem = null;
            initGui();
        });

        // 取消按钮
        ButtonGeneric cancelBtn = new ButtonGeneric(dialogX + 130, dialogY + 45, 80, 20, I18n.tr("litematlist.button.cancel"));
        cancelBtn.setRenderDefaultBackground(true);
        this.addButton(cancelBtn, (IButtonActionListener) (b, mb) -> {
            pendingDeleteItem = null;
            initGui();
        });
    }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void renderResetConfirmDialog(DrawContext drawContext, int mouseX, int mouseY) {
//#elseif MC == 11701 || MC == 11904
//$$    private void renderResetConfirmDialog(MatrixStack matrices, int mouseX, int mouseY) {
//#endif
        int dialogW = 240;
        int dialogH = 80;
        int dialogX = (this.width - dialogW) / 2;
        int dialogY = (this.height - dialogH) / 2;

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawCenteredTextWithShadow(this.textRenderer, I18n.tr("litematlist.confirm.reset_whitelist"),
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                this.width / 2, dialogY + 15, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$        this.textRenderer.drawWithShadow(matrices, I18n.tr("litematlist.confirm.reset_whitelist"), this.width / 2 - this.textRenderer.getWidth(I18n.tr("litematlist.confirm.reset_whitelist")) / 2, dialogY + 15, 0xFFFFFFFF);
//#endif

        // 确认按钮
        ButtonGeneric confirmBtn = new ButtonGeneric(dialogX + 30, dialogY + 45, 80, 20, I18n.tr("litematlist.button.confirm"));
        confirmBtn.setRenderDefaultBackground(true);
        this.addButton(confirmBtn, (IButtonActionListener) (b, mb) -> {
            whitelistSet.clear();
            whitelistSet.addAll(RawMaterialConfig.DEFAULT_WHITELIST);
            whitelistItems.clear();
            whitelistItems.addAll(whitelistSet);
            sortWhitelistByRegistry();
            // 重置配方类型顺序和启用状态
            recipeOrder.clear();
//#if MC == 12108 || MC == 12110 || MC == 12111
            recipeOrder.add(new RawMaterialConfig.RecipeTypeConfig("stonecutter", true, fi.dy.masa.malilib.util.game.RecipeBookUtils.Type.STONECUTTER));
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            recipeOrder.add(new RawMaterialConfig.RecipeTypeConfig("stonecutter", true, "stonecutter"));
//#elseif MC == 12101 || MC == 12104
//$$            recipeOrder.add(new RawMaterialConfig.RecipeTypeConfig("stonecutter", true, com.litematlist.RecipeBookUtils.Type.STONECUTTER));
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            recipeOrder.add(new RawMaterialConfig.RecipeTypeConfig("crafting", true, fi.dy.masa.malilib.util.game.RecipeBookUtils.Type.SHAPED, fi.dy.masa.malilib.util.game.RecipeBookUtils.Type.SHAPELESS));
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            recipeOrder.add(new RawMaterialConfig.RecipeTypeConfig("crafting", true, "crafting_shaped", "crafting_shapeless"));
//#elseif MC == 12101 || MC == 12104
//$$            recipeOrder.add(new RawMaterialConfig.RecipeTypeConfig("crafting", true, com.litematlist.RecipeBookUtils.Type.SHAPED, com.litematlist.RecipeBookUtils.Type.SHAPELESS));
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            recipeOrder.add(new RawMaterialConfig.RecipeTypeConfig("furnace", true, fi.dy.masa.malilib.util.game.RecipeBookUtils.Type.FURNACE));
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            recipeOrder.add(new RawMaterialConfig.RecipeTypeConfig("furnace", true, "smelting"));
//#elseif MC == 12101 || MC == 12104
//$$            recipeOrder.add(new RawMaterialConfig.RecipeTypeConfig("furnace", true, com.litematlist.RecipeBookUtils.Type.FURNACE));
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            recipeOrder.add(new RawMaterialConfig.RecipeTypeConfig("smithing", true, fi.dy.masa.malilib.util.game.RecipeBookUtils.Type.SMITHING));
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            recipeOrder.add(new RawMaterialConfig.RecipeTypeConfig("smithing", true, "smithing_transform"));
//#elseif MC == 12101 || MC == 12104
//$$            recipeOrder.add(new RawMaterialConfig.RecipeTypeConfig("smithing", true, com.litematlist.RecipeBookUtils.Type.SMITHING));
//#endif
            priorityLocked = false;
            showResetConfirm = false;
            initGui();
        });

        // 取消按钮
        ButtonGeneric cancelBtn = new ButtonGeneric(dialogX + 130, dialogY + 45, 80, 20, I18n.tr("litematlist.button.cancel"));
        cancelBtn.setRenderDefaultBackground(true);
        this.addButton(cancelBtn, (IButtonActionListener) (b, mb) -> {
            showResetConfirm = false;
            initGui();
        });
    }

    @Override
//#if MC == 12110 || MC == 12111
    public boolean onMouseClicked(net.minecraft.client.gui.Click click, boolean isDrag) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    public boolean onMouseClicked(int mouseX, int mouseY, int mouseButton) {
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$    public boolean onMouseClicked(int mouseX, int mouseY, int button) {
//#endif
//#if MC == 12110 || MC == 12111
        if (super.onMouseClicked(click, isDrag)) return true;
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        if (super.onMouseClicked(mouseX, mouseY, mouseButton)) return true;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        if (super.onMouseClicked(mouseX, mouseY, button)) return true;
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

        if (pendingDeleteItem != null || showResetConfirm) {
            return true;
        }

//#if MC == 12110 || MC == 12111
        if (click.button() == 0) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        if (mouseButton == 0) {
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        if (button == 0) {
//#endif
            // 检查配方类型复选框点击
            int sliderAreaLeft = this.width - 180;
            int sliderItemHeight = SLIDER_HEIGHT;
            for (int i = 0; i < recipeOrder.size(); i++) {
                int y = SLIDER_TOP + i * (sliderItemHeight + SLIDER_GAP);
                int checkboxX = sliderAreaLeft + 4;
                int checkboxY = y + 9;
                if (mouseX >= checkboxX && mouseX <= checkboxX + 12 && mouseY >= checkboxY && mouseY <= checkboxY + 12) {
                    recipeOrder.get(i).enabled = !recipeOrder.get(i).enabled;
                    // 不启用的配方自动置底
                    recipeOrder.sort((a, b) -> {
                        if (a.enabled == b.enabled) return 0;
                        return a.enabled ? -1 : 1;
                    });
                    initGui();
                    return true;
                }
            }

            // 检查白名单网格删除按钮
            int startX = GRID_LEFT;
            int startY = LIST_TOP;
            int col = 0, row = 0;

            for (int i = scrollOffset; i < whitelistItems.size(); i++) {
                int x = startX + col * (ICON_SIZE + ICON_GAP);
                int y = startY + row * (ICON_SIZE + ICON_GAP + 2);

                if (y + ICON_SIZE > this.height - 50) break;

                if (mouseX >= x + ICON_SIZE - 7 && mouseX <= x + ICON_SIZE
                        && mouseY >= y && mouseY <= y + 7) {
                    if (Configs.Generic.WHITELIST_DELETE_CONFIRM.getBooleanValue()) {
                        pendingDeleteItem = whitelistItems.get(i);
                        initGui();
                    } else {
                        whitelistSet.remove(whitelistItems.get(i));
                        whitelistItems.clear();
                        whitelistItems.addAll(whitelistSet);
                        sortWhitelistByRegistry();
                        initGui();
                    }
                    return true;
                }

                col++;
                if (col >= GRID_COLS) { col = 0; row++; }
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
        if (mouseX >= GRID_LEFT && mouseX <= this.width - 200 && mouseY >= LIST_TOP && mouseY <= this.height - 50) {
            int itemsPerRow = GRID_COLS;
            int totalRows = (int) Math.ceil((double) whitelistItems.size() / itemsPerRow);
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            scrollOffset = Math.max(0, scrollOffset - (int) verticalAmount * itemsPerRow);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            scrollOffset = Math.max(0, scrollOffset - (int) amount * itemsPerRow);
//#endif
            scrollOffset = Math.min(scrollOffset, Math.max(0, totalRows - visibleRows) * itemsPerRow);
            initGui();
            return true;
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        return super.mouseScrolled(mouseX, mouseY, amount);
//#endif
    }
}
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$
//$$
//$$
//$$
//#endif