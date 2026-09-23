package com.litematlist.gui;

import com.litematlist.I18n;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.MinecraftClient;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;

import net.minecraft.text.Text;

/**
 * HUD 界面设置 —— 分两页：配置属性 + 拖拽位置。
 */
public class HudConfigScreen extends GuiBase {

    private final GuiBase parent;
    private int page = 0; // 0=配置, 1=拖拽位置

    // 临时值（拖拽页面使用，保存时才写入持久化）
    private int tempHudX, tempHudY;
    private boolean dragging = false;
    private int dragOffsetX, dragOffsetY;

    // 配置页的输入框
    private TextFieldWidget fontSizeField, bgAlphaField, maxLinesField;

    // 配置页开关按钮（空文本占位，彩色状态文字在 render 中覆盖绘制）
    private int showHudBtnX, showHudBtnY, showHudBtnW;
    private String showHudBtnLabel = "";
    private int boxBtnX, boxBtnY, boxBtnW;
    private String boxBtnLabel = "";
    private int groupBtnX, groupBtnY, groupBtnW;
    private String groupBtnLabel = "";

    public HudConfigScreen(GuiBase parent) {
        super();
        this.parent = parent;
        this.title = I18n.tr("litematlist.title.hud_config");
        this.tempHudX = MaterialDetailScreen.persistedHudX;
        this.tempHudY = MaterialDetailScreen.persistedHudY;
    }

    @Override
    public void initGui() {
        super.initGui();

        if (page == 0) {
            initConfigPage();
        } else {
            initPositionPage();
        }
    }

    // ===================== 配置页 =====================

    private void initConfigPage() {
        int leftX = this.width / 2 - 120;
        int y = 50;
        int fieldW = 60;
        int fieldH = 20;

        // 字体大小
        this.addLabel(leftX, y, 200, 14, 0xFFFFFFFF, I18n.tr("litematlist.hud.font_size") + " (1-100):");
        y += 16;
        fontSizeField = new TextFieldWidget(this.textRenderer, leftX, y, fieldW, fieldH, Text.empty());
        fontSizeField.setText(String.valueOf(MaterialDetailScreen.persistedHudFontSize));
        fontSizeField.setChangedListener(text -> {
            try {
                int val = Integer.parseInt(text.trim());
                if (val >= 1 && val <= 100) {
                    MaterialDetailScreen.persistedHudFontSize = val;
                    MaterialDetailScreen.savePersistence();
                    MaterialListHudRenderer.updateMissingCounts();
                }
            } catch (NumberFormatException ignored) {}
        });
        this.addSelectableChild(fontSizeField);

        y += 30;
        // 背景透明度
        this.addLabel(leftX, y, 200, 14, 0xFFFFFFFF, I18n.tr("litematlist.hud.bg_alpha") + " (0-100):");
        y += 16;
        bgAlphaField = new TextFieldWidget(this.textRenderer, leftX, y, fieldW, fieldH, Text.empty());
        bgAlphaField.setText(String.valueOf(MaterialDetailScreen.persistedHudBgAlpha));
        bgAlphaField.setChangedListener(text -> {
            try {
                int val = Integer.parseInt(text.trim());
                if (val >= 0 && val <= 100) {
                    MaterialDetailScreen.persistedHudBgAlpha = val;
                    MaterialDetailScreen.savePersistence();
                    MaterialListHudRenderer.updateMissingCounts();
                }
            } catch (NumberFormatException ignored) {}
        });
        this.addSelectableChild(bgAlphaField);

        y += 30;
        // 写入行数
        this.addLabel(leftX, y, 200, 14, 0xFFFFFFFF, I18n.tr("litematlist.hud.max_lines") + " (1-20):");
        y += 16;
        maxLinesField = new TextFieldWidget(this.textRenderer, leftX, y, fieldW, fieldH, Text.empty());
        maxLinesField.setText(String.valueOf(MaterialDetailScreen.persistedHudMaxLines));
        maxLinesField.setChangedListener(text -> {
            try {
                int val = Integer.parseInt(text.trim());
                if (val >= 1 && val <= 20) {
                    MaterialDetailScreen.persistedHudMaxLines = val;
                    MaterialDetailScreen.savePersistence();
                    MaterialListHudRenderer.updateMissingCounts();
                }
            } catch (NumberFormatException ignored) {}
        });
        this.addSelectableChild(maxLinesField);

        // ---- 开关按钮：三个输入框右边为盒(1728)/组(64)开关，下方为显示HUD开关 ----
        boxBtnLabel = MaterialDetailScreen.persistedHudShowBox
                ? I18n.tr("litematlist.hud.scale_box_on") : I18n.tr("litematlist.hud.scale_box_off");
        boxBtnX = leftX + 200; boxBtnY = 66;
        boxBtnW = this.textRenderer.getWidth(I18n.tr("litematlist.hud.scale_box_on")) + 16;
        ButtonGeneric boxBtn = new ButtonGeneric(boxBtnX, boxBtnY, boxBtnW, 20, "");
        boxBtn.setRenderDefaultBackground(true);
        this.addButton(boxBtn, (IButtonActionListener) (b, mb) -> {
            MaterialDetailScreen.persistedHudShowBox = !MaterialDetailScreen.persistedHudShowBox;
            MaterialDetailScreen.savePersistence();
            MaterialListHudRenderer.recomputeCachedDimensions();
            initGui();
        });

        groupBtnLabel = MaterialDetailScreen.persistedHudShowGroup
                ? I18n.tr("litematlist.hud.scale_group_on") : I18n.tr("litematlist.hud.scale_group_off");
        groupBtnX = leftX + 200; groupBtnY = 96;
        groupBtnW = this.textRenderer.getWidth(I18n.tr("litematlist.hud.scale_group_on")) + 16;
        ButtonGeneric groupBtn = new ButtonGeneric(groupBtnX, groupBtnY, groupBtnW, 20, "");
        groupBtn.setRenderDefaultBackground(true);
        this.addButton(groupBtn, (IButtonActionListener) (b, mb) -> {
            MaterialDetailScreen.persistedHudShowGroup = !MaterialDetailScreen.persistedHudShowGroup;
            MaterialDetailScreen.savePersistence();
            MaterialListHudRenderer.recomputeCachedDimensions();
            initGui();
        });

        // 底部按钮 Y 坐标（显示HUD开关放在其上方居中位置）
        int buttonY = this.height - 38;

        // 显示HUD 开关（原在【上传区域】界面，现移入本界面，居中偏下位于底部按钮上方）
        showHudBtnLabel = MaterialDetailScreen.persistedHudVisible
                ? I18n.tr("litematlist.button.hud_on") : I18n.tr("litematlist.button.hud_off");
        showHudBtnW = Math.max(110, this.textRenderer.getWidth(showHudBtnLabel) + 16);
        showHudBtnX = this.width / 2 - showHudBtnW / 2;
        showHudBtnY = buttonY - 28;
        ButtonGeneric showHudBtn = new ButtonGeneric(showHudBtnX, showHudBtnY, showHudBtnW, 20, "");
        showHudBtn.setRenderDefaultBackground(true);
        this.addButton(showHudBtn, (IButtonActionListener) (b, mb) -> {
            MaterialDetailScreen.persistedHudVisible = !MaterialDetailScreen.persistedHudVisible;
            MaterialDetailScreen.savePersistence();
            if (MaterialDetailScreen.persistedHudVisible) {
                MaterialListHudRenderer.refreshHud();
            }
            initGui();
        });

        // 底部按钮：同一行排列（调整HUD位置 | 保存 | 取消）
        int btnW = 120;

        // 调整HUD位置
        ButtonGeneric posBtn = new ButtonGeneric(this.width / 2 - 184, buttonY, btnW, 20,
                I18n.tr("litematlist.button.hud_position"));
        posBtn.setRenderDefaultBackground(true);
        this.addButton(posBtn, (IButtonActionListener) (b, mb) -> {
            tempHudX = MaterialDetailScreen.persistedHudX;
            tempHudY = MaterialDetailScreen.persistedHudY;
            page = 1;
            initGui();
        });

        // 保存
        ButtonGeneric saveBtn = new ButtonGeneric(this.width / 2 - 60, buttonY, btnW, 20,
                I18n.tr("litematlist.button.save"));
        saveBtn.setRenderDefaultBackground(true);
        this.addButton(saveBtn, (IButtonActionListener) (b, mb) -> goBack());

        // 取消
        ButtonGeneric cancelBtn = new ButtonGeneric(this.width / 2 + 64, buttonY, btnW, 20,
                I18n.tr("litematlist.button.cancel"));
        cancelBtn.setRenderDefaultBackground(true);
        this.addButton(cancelBtn, (IButtonActionListener) (b, mb) -> goBack());
    }

    // ===================== 拖拽位置页 =====================

    private void initPositionPage() {
        int buttonY = this.height - 38;
        // 按当前配置重新计算 HUD 实际尺寸，保证判定边框与真实 HUD 一致
        MaterialListHudRenderer.recomputeCachedDimensions();

        // 重置
        ButtonGeneric resetBtn = new ButtonGeneric(this.width / 2 - 184, buttonY, 120, 20,
                I18n.tr("litematlist.button.reset"));
        resetBtn.setRenderDefaultBackground(true);
        this.addButton(resetBtn, (IButtonActionListener) (b, mb) -> {
            int boxW = getPreviewBoxWidth();
            int boxH = getPreviewBoxHeight();
            tempHudX = (this.width - boxW) / 2;
            tempHudY = (this.height - boxH) / 2;
        });

        // 保存
        ButtonGeneric saveBtn = new ButtonGeneric(this.width / 2 - 60, buttonY, 120, 20,
                I18n.tr("litematlist.button.save"));
        saveBtn.setRenderDefaultBackground(true);
        this.addButton(saveBtn, (IButtonActionListener) (b, mb) -> {
            MaterialDetailScreen.persistedHudX = tempHudX;
            MaterialDetailScreen.persistedHudY = tempHudY;
            MaterialDetailScreen.savePersistence();
            MaterialListHudRenderer.updateMissingCounts();
            goBack();
        });

        // 取消
        ButtonGeneric cancelBtn = new ButtonGeneric(this.width / 2 + 64, buttonY, 120, 20,
                I18n.tr("litematlist.button.cancel"));
        cancelBtn.setRenderDefaultBackground(true);
        this.addButton(cancelBtn, (IButtonActionListener) (b, mb) -> goBack());
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(drawContext, mouseX, mouseY, partialTicks);
        super.render(drawContext, mouseX, mouseY, partialTicks);

        if (page == 0) {
            // 显式渲染输入框（GuiBase 可能不会自动渲染 vanilla Drawable 子控件）
            if (fontSizeField != null) fontSizeField.render(drawContext, mouseX, mouseY, partialTicks);
            if (bgAlphaField != null) bgAlphaField.render(drawContext, mouseX, mouseY, partialTicks);
            if (maxLinesField != null) maxLinesField.render(drawContext, mouseX, mouseY, partialTicks);

            // 开关按钮的彩色状态文字（绿色=开，红色=关）
            drawToggleLabel(drawContext, boxBtnLabel, boxBtnX, boxBtnY, boxBtnW,
                    MaterialDetailScreen.persistedHudShowBox);
            drawToggleLabel(drawContext, groupBtnLabel, groupBtnX, groupBtnY, groupBtnW,
                    MaterialDetailScreen.persistedHudShowGroup);
            drawToggleLabel(drawContext, showHudBtnLabel, showHudBtnX, showHudBtnY, showHudBtnW,
                    MaterialDetailScreen.persistedHudVisible);
        }

        if (page == 1) {
            drawPreviewHud(drawContext);
        }
    }

    /** 绘制开关按钮的彩色状态文字（空标签按钮的白色文字由这里覆盖为绿/红） */
    private void drawToggleLabel(DrawContext drawContext, String label, int bx, int by, int bw, boolean on) {
        int color = on ? 0xFF55FF55 : 0xFFFF5555;
        int textW = this.textRenderer.getWidth(label);
        drawContext.drawText(this.textRenderer, label, bx + (bw - textW) / 2, by + 6, color, false);
    }

    /** 根据当前 HUD 实际占用面积绘制预览框（无上传列表时用提示文字作为占位） */
    private void drawPreviewHud(DrawContext drawContext) {
        int fontSize = MaterialDetailScreen.persistedHudFontSize;
        int maxLines = MaterialDetailScreen.persistedHudMaxLines;
        float scale = fontSize / 10.0f;
        int lineHeight = (int)(10 * scale);
        int iconSize = (int)(16 * scale);

        MaterialListScreen.LoadedEntry uploaded = MaterialListScreen.getUploadedEntry();
        var hudItems = MaterialListHudRenderer.getHudItems();
        int lineCount = Math.min(hudItems.size(), maxLines);
        if (lineCount == 0 && uploaded != null) lineCount = 1; // 有上传但无数据时留一行
        int totalW, totalH;

        if (uploaded == null || hudItems.isEmpty()) {
            // 无上传列表：用提示文字宽度估算
            String hint = I18n.tr("litematlist.hud.drag_hint");
            int hintW = (int)(this.textRenderer.getWidth(hint) * scale);
            totalW = hintW + 16;
            totalH = (int)(lineHeight + 4 + 8 * scale + 2);
        } else {
            // 使用 HUD 实际计算出的宽高（渲染器缓存），判定边框与真实文字范围一致
            totalW = MaterialListHudRenderer.getCachedTotalW();
            totalH = MaterialListHudRenderer.getCachedTotalH();
            if (totalW <= 0 || totalH <= 0) {
                totalW = (int)(iconSize + 4 + 120 + 8);
                totalH = Math.max(1, lineCount) * lineHeight + 4 + (int)(8 * scale) + 2;
            }
        }

        int borderColor = dragging ? 0xFFFFFF00 : 0xFFFFFFFF;
        int fillColor = 0x40000000;

        drawContext.fill(tempHudX, tempHudY, tempHudX + totalW, tempHudY + totalH, fillColor);
        // 边框
        drawContext.fill(tempHudX, tempHudY, tempHudX + totalW, tempHudY + 1, borderColor);
        drawContext.fill(tempHudX, tempHudY + totalH - 1, tempHudX + totalW, tempHudY + totalH, borderColor);
        drawContext.fill(tempHudX, tempHudY, tempHudX + 1, tempHudY + totalH, borderColor);
        drawContext.fill(tempHudX + totalW - 1, tempHudY, tempHudX + totalW, tempHudY + totalH, borderColor);

        // 提示文字（左上角对齐，字体大小与HUD相同，支持换行）
        String hint = I18n.tr("litematlist.hud.drag_hint");
        String[] lines = hint.split("\n");
        drawContext.getMatrices().push();
        drawContext.getMatrices().translate((float)(tempHudX + 2), (float)(tempHudY + 2), 0.0f);
        drawContext.getMatrices().scale(scale, scale, 1.0f);
        for (int li = 0; li < lines.length; li++) {
            drawContext.drawText(this.textRenderer, lines[li], 0, li * 10, 0xFFFFFFFF, true);
        }
        drawContext.getMatrices().pop();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (page == 1 && button == 0) {
            int totalW = getPreviewBoxWidth();
            int totalH = getPreviewBoxHeight();
            
            
            if (mouseX >= tempHudX && mouseX <= tempHudX + totalW
                    && mouseY >= tempHudY && mouseY <= tempHudY + totalH) {
                dragging = true;
                dragOffsetX = (int)(mouseX - tempHudX);
                dragOffsetY = (int)(mouseY - tempHudY);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging) {
            
            
            tempHudX = (int)mouseX - dragOffsetX;
            tempHudY = (int)mouseY - dragOffsetY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC
            if (page == 1) {
                page = 0;
                initGui();
                return true;
            }
            // page 0: ESC 退回上传区域
            goBack();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private int getPreviewBoxWidth() {
        // 优先使用 HUD 实际缓存宽度（与真实渲染完全一致）
        int cachedW = MaterialListHudRenderer.getCachedTotalW();
        if (cachedW > 0) return cachedW;
        int fontSize = MaterialDetailScreen.persistedHudFontSize;
        float scale = fontSize / 10.0f;
        var hudItems = MaterialListHudRenderer.getHudItems();
        int maxLines = MaterialDetailScreen.persistedHudMaxLines;
        int lineCount = Math.min(hudItems.size(), maxLines);
        if (hudItems.isEmpty()) {
            String hint = I18n.tr("litematlist.hud.drag_hint");
            String[] hintLines = hint.split("\n");
            int maxHintW = 0;
            for (String line : hintLines) {
                int w = (int)(this.textRenderer.getWidth(line) * scale);
                if (w > maxHintW) maxHintW = w;
            }
            return maxHintW + 16;
        }
        int iconSize = (int)(16 * scale);
        int maxNameW = 80;
        MinecraftClient client = MinecraftClient.getInstance();
        for (int i = 0; i < lineCount; i++) {
            int w = (int)(client.textRenderer.getWidth(hudItems.get(i).name) * scale);
            if (w > maxNameW) maxNameW = w;
        }
        int missW = (int)(client.textRenderer.getWidth(MaterialListHudRenderer.formatHudCount(9999)) * scale);
        return (int)(iconSize + 4 + maxNameW + 4 + missW + 8);
    }

    private int getPreviewBoxHeight() {
        // 优先使用 HUD 实际缓存高度（与真实渲染完全一致）
        int cachedH = MaterialListHudRenderer.getCachedTotalH();
        if (cachedH > 0) return cachedH;
        int fontSize = MaterialDetailScreen.persistedHudFontSize;
        float scale = fontSize / 10.0f;
        int maxLines = MaterialDetailScreen.persistedHudMaxLines;
        var hudItems = MaterialListHudRenderer.getHudItems();
        int lineCount = Math.min(hudItems.size(), maxLines);
        int lineHeight = (int)(10 * scale);
        int pageH = (int)(8 * scale);
        if (hudItems.isEmpty()) {
            String hint = I18n.tr("litematlist.hud.drag_hint");
            int hintLines = hint.split("\n").length;
            return hintLines * lineHeight + 4 + pageH + 2;
        }
        return lineCount * lineHeight + 4 + pageH + 2;
    }

    private void goBack() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}



