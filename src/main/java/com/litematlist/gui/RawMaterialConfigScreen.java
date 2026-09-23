package com.litematlist.gui;

import com.litematlist.I18n;
import com.litematlist.config.Configs;
import com.litematlist.config.RawMaterialConfig;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.util.*;

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
        whitelistItems.sort(Comparator.comparingInt(item -> Registries.ITEM.getRawId(item)));
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
    protected void drawScreenBackground(DrawContext drawContext, int mouseX, int mouseY) {
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        // 全屏深色背景（最先绘制，否则 MaLib 背景会覆盖白名单物品）
        drawContext.fill(0, 0, this.width, this.height, 0xB0000000);
        // 先绘制所有背景，再绘制按钮，最后绘制文字
        int sliderAreaLeft = this.width - 180;
        int sliderAreaWidth = 160;
        int sliderItemHeight = SLIDER_HEIGHT;

        // 配方优先级滑块背景（右侧）
        // 注：排序标签移至 super.render() 之后渲染以确保最高图层

        for (int i = 0; i < recipeOrder.size(); i++) {
            int y = SLIDER_TOP + i * (sliderItemHeight + SLIDER_GAP);
            int bgColor = 0x40000000;
            drawContext.fill(sliderAreaLeft, y, sliderAreaLeft + sliderAreaWidth, y + sliderItemHeight, bgColor);
        }

        // 白名单网格背景
        int startX = GRID_LEFT;
        int startY = LIST_TOP;
        int col = 0, row = 0;
        for (int i = scrollOffset; i < whitelistItems.size(); i++) {
            int x = startX + col * (ICON_SIZE + ICON_GAP);
            int y = startY + row * (ICON_SIZE + ICON_GAP + 2);
            if (y + ICON_SIZE > this.height - 50) break;
            drawContext.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, 0x40000000);
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
            drawContext.drawItem(new ItemStack(item), x + 1, y + 1);

            // 删除按钮（右上角红叉）——绘制于物品图标之后，按绘制顺序覆盖在最上层
            drawContext.fill(x + ICON_SIZE - 7, y, x + ICON_SIZE, y + 7, 0x80FF0000);
            drawContext.drawTextWithShadow(this.textRenderer, "×", x + ICON_SIZE - 6, y + 1, 0xFFFFFFFF);

            col++;
            if (col >= GRID_COLS) { col = 0; row++; }
        }

        // 第3层：确认对话框（黑框与文字，绘制于物品之上、按钮之下）
        if (pendingDeleteItem != null || showResetConfirm) {
            renderConfirmDialogBackground(drawContext);
        }
        // 删除确认对话框
        if (pendingDeleteItem != null) {
            renderDeleteConfirmDialog(drawContext, mouseX, mouseY);
        }
        // 重置确认对话框
        if (showResetConfirm) {
            renderResetConfirmDialog(drawContext, mouseX, mouseY);
        }

        // 第4层：按钮（最上层，防止黑框覆盖按钮）
        super.render(drawContext, mouseX, mouseY, delta);

        // 第3层：文字和图标
        // 配方优先级标签（右上角）
        drawContext.drawTextWithShadow(this.textRenderer, I18n.tr("litematlist.label.recipe_priority"),
                sliderAreaLeft, 20, 0xFFFFFFFF);

        // 排序提示（纯白色，最高图层）
        drawContext.drawText(this.textRenderer, I18n.tr("litematlist.label.recipe_priority_order"),
                sliderAreaLeft, SLIDER_TOP - 14, 0xFFFFFFFF, false);

        // 锁定状态提示（右侧配方优先级区域下方）
        if (priorityLocked) {
            drawContext.drawTextWithShadow(this.textRenderer, I18n.tr("litematlist.label.priority_locked_hint"),
                    sliderAreaLeft, SLIDER_TOP + recipeOrder.size() * (SLIDER_HEIGHT + SLIDER_GAP) + 4, 0xAAAAAA);
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
            drawContext.drawTextWithShadow(this.textRenderer, checkboxText, checkboxX, checkboxY, checkboxColor);

            if (!priorityLocked) {
                drawContext.drawTextWithShadow(this.textRenderer, "≡ ", sliderAreaLeft + 18, y + 8, 0xAAAAAA);
            }
            drawContext.drawTextWithShadow(this.textRenderer, config.getDisplayName(), sliderAreaLeft + 34, y + 8, 0xFFFFFFFF);
        }

        // 白名单标题
        drawContext.drawTextWithShadow(this.textRenderer, I18n.tr("litematlist.label.whitelist_title"),
                GRID_LEFT, LIST_TOP - 14, 0xFFFFFFFF);

        // 悬停提示（最高图层）
        col = 0; row = 0;
        for (int i = scrollOffset; i < whitelistItems.size(); i++) {
            Item item = whitelistItems.get(i);
            int x = startX + col * (ICON_SIZE + ICON_GAP);
            int y = startY + row * (ICON_SIZE + ICON_GAP + 2);
            if (y + ICON_SIZE > this.height - 50) break;
            if (mouseX >= x && mouseX <= x + ICON_SIZE && mouseY >= y && mouseY <= y + ICON_SIZE) {
                String tooltip = item.getName().getString();
                drawContext.drawTooltip(this.textRenderer, Text.of(tooltip), mouseX, mouseY);
            }
            col++;
            if (col >= GRID_COLS) { col = 0; row++; }
        }
    }

    private void renderConfirmDialogBackground(DrawContext drawContext) {
        int dialogW = 240;
        int dialogH = 80;
        int dialogX = (this.width - dialogW) / 2;
        int dialogY = (this.height - dialogH) / 2;
        drawContext.fill(dialogX, dialogY, dialogX + dialogW, dialogY + dialogH, 0xE0000000);
        drawContext.fill(dialogX, dialogY, dialogX + dialogW, dialogY + 1, 0xFFFFFFFF);
        drawContext.fill(dialogX, dialogY + dialogH - 1, dialogX + dialogW, dialogY + dialogH, 0xFFFFFFFF);
        drawContext.fill(dialogX, dialogY, dialogX + 1, dialogY + dialogH, 0xFFFFFFFF);
        drawContext.fill(dialogX + dialogW - 1, dialogY, dialogX + dialogW, dialogY + dialogH, 0xFFFFFFFF);
    }

    private void renderDeleteConfirmDialog(DrawContext drawContext, int mouseX, int mouseY) {
        int dialogW = 240;
        int dialogH = 80;
        int dialogX = (this.width - dialogW) / 2;
        int dialogY = (this.height - dialogH) / 2;

        String msg = I18n.tr("litematlist.confirm.delete_whitelist",
                pendingDeleteItem.getName().getString());
        drawContext.drawCenteredTextWithShadow(this.textRenderer, msg, this.width / 2, dialogY + 15, 0xFFFFFFFF);

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

    private void renderResetConfirmDialog(DrawContext drawContext, int mouseX, int mouseY) {
        int dialogW = 240;
        int dialogH = 80;
        int dialogX = (this.width - dialogW) / 2;
        int dialogY = (this.height - dialogH) / 2;

        drawContext.drawCenteredTextWithShadow(this.textRenderer, I18n.tr("litematlist.confirm.reset_whitelist"),
                this.width / 2, dialogY + 15, 0xFFFFFFFF);

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
            recipeOrder.add(new RawMaterialConfig.RecipeTypeConfig("stonecutter", true, fi.dy.masa.malilib.util.game.RecipeBookUtils.Type.STONECUTTER));
            recipeOrder.add(new RawMaterialConfig.RecipeTypeConfig("crafting", true, fi.dy.masa.malilib.util.game.RecipeBookUtils.Type.SHAPED, fi.dy.masa.malilib.util.game.RecipeBookUtils.Type.SHAPELESS));
            recipeOrder.add(new RawMaterialConfig.RecipeTypeConfig("furnace", true, fi.dy.masa.malilib.util.game.RecipeBookUtils.Type.FURNACE));
            recipeOrder.add(new RawMaterialConfig.RecipeTypeConfig("smithing", true, fi.dy.masa.malilib.util.game.RecipeBookUtils.Type.SMITHING));
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
    public boolean onMouseClicked(int mouseX, int mouseY, int button) {
        if (super.onMouseClicked(mouseX, mouseY, button)) return true;

        
        

        if (pendingDeleteItem != null || showResetConfirm) {
            return true;
        }

        if (button == 0) {
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
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= GRID_LEFT && mouseX <= this.width - 200 && mouseY >= LIST_TOP && mouseY <= this.height - 50) {
            int itemsPerRow = GRID_COLS;
            int totalRows = (int) Math.ceil((double) whitelistItems.size() / itemsPerRow);
            scrollOffset = Math.max(0, scrollOffset - (int) verticalAmount * itemsPerRow);
            scrollOffset = Math.min(scrollOffset, Math.max(0, totalRows - visibleRows) * itemsPerRow);
            initGui();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}



