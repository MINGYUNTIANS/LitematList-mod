package com.litematlist.gui;

import com.litematlist.I18n;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.render.GuiContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

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
    private EditBox fontSizeField, bgAlphaField, maxLinesField, formatField;
    // 格式标签的悬停区域（用于显示格式规则提示）
    private int formatLabelX, formatLabelY, formatLabelW, formatLabelH;

    // 配置页开关按钮（空文本占位，彩色状态文字在 render 中覆盖绘制）
    private int showHudBtnX, showHudBtnY, showHudBtnW;
    private String showHudBtnLabel = "";
    private int boxBtnX, boxBtnY, boxBtnW;
    private String boxBtnLabel = "";
    private int groupBtnX, groupBtnY, groupBtnW;
    private String groupBtnLabel = "";
    private int ratioBtnX, ratioBtnY, ratioBtnW;
    private String ratioBtnLabel = "";
    private int anchorBtnX, anchorBtnY, anchorBtnW;
    private String anchorBtnLabel = "";

    public HudConfigScreen(GuiBase parent) {
        super();
        this.parent = parent;
        this.title = I18n.tr("litematlist.title.hud_config");
        this.tempHudX = MaterialDataStore.persistedHudX;
        this.tempHudY = MaterialDataStore.persistedHudY;
    }

    @Override
    public void initGui() {
        super.initGui();

        // 页面切换时清除上一页创建的原版输入框并重置焦点，
        // 避免残留输入框抢占点击/键盘输入导致光标不显示
        this.setFocused((net.minecraft.client.gui.components.events.GuiEventListener) null);
        this.clearWidgets();

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
        this.addLabel(leftX, y, 200, 14, 0xFFFFFFFF, I18n.tr("litematlist.hud.font_size") + " (1-32):");
        y += 16;
        fontSizeField = new EditBox(this.font, leftX, y, fieldW, fieldH, Component.empty());
        fontSizeField.setValue(String.valueOf(MaterialDataStore.persistedHudFontSize));
        fontSizeField.setResponder(text -> {
            try {
                int val = Integer.parseInt(text.trim());
                if (val >= 1 && val <= 32) {
                    MaterialDataStore.persistedHudFontSize = val;
                    MaterialDataStore.savePersistence();
                    MaterialListHudRenderer.updateMissingCounts();
                }
            } catch (NumberFormatException ignored) {}
        });
        this.addRenderableWidget(fontSizeField);

        y += 24;
        // 背景透明度
        this.addLabel(leftX, y, 200, 14, 0xFFFFFFFF, I18n.tr("litematlist.hud.bg_alpha") + " (0-100):");
        y += 16;
        bgAlphaField = new EditBox(this.font, leftX, y, fieldW, fieldH, Component.empty());
        bgAlphaField.setValue(String.valueOf(MaterialDataStore.persistedHudBgAlpha));
        bgAlphaField.setResponder(text -> {
            try {
                int val = Integer.parseInt(text.trim());
                if (val >= 0 && val <= 100) {
                    MaterialDataStore.persistedHudBgAlpha = val;
                    MaterialDataStore.savePersistence();
                    MaterialListHudRenderer.updateMissingCounts();
                }
            } catch (NumberFormatException ignored) {}
        });
        this.addRenderableWidget(bgAlphaField);

        y += 24;
        // 写入行数
        this.addLabel(leftX, y, 200, 14, 0xFFFFFFFF, I18n.tr("litematlist.hud.max_lines") + " (1-20):");
        y += 16;
        maxLinesField = new EditBox(this.font, leftX, y, fieldW, fieldH, Component.empty());
        maxLinesField.setValue(String.valueOf(MaterialDataStore.persistedHudMaxLines));
        maxLinesField.setResponder(text -> {
            try {
                int val = Integer.parseInt(text.trim());
                if (val >= 1 && val <= 20) {
                    MaterialDataStore.persistedHudMaxLines = val;
                    MaterialDataStore.savePersistence();
                    MaterialListHudRenderer.updateMissingCounts();
                }
            } catch (NumberFormatException ignored) {}
        });
        this.addRenderableWidget(maxLinesField);

        // HUD 显示格式：五个变量 [icon] [name] [item] [box] [stack]，其余文字原样显示
        y += 20;
        formatLabelX = leftX;
        formatLabelY = y;
        String formatLabelText = I18n.tr("litematlist.hud.format") + ":";
        formatLabelW = Math.min(240, this.font.width(formatLabelText));
        formatLabelH = 14;
        this.addLabel(leftX, y, 240, 14, 0xFFFFFFFF, formatLabelText);
        y += 16;
        formatField = new EditBox(this.font, leftX, y, 190, 20, Component.empty());
        formatField.setMaxLength(256);
        formatField.setValue(MaterialListHudRenderer.getHudFormat());
        formatField.setResponder(text -> {
            MaterialDataStore.persistedHudFormat = text;
            MaterialDataStore.savePersistence();
            MaterialListHudRenderer.recomputeCachedDimensions();
        });
        this.addRenderableWidget(formatField);

        // 重置按钮（位于格式输入框左侧），确认后恢复默认格式
        String formatResetLabel = I18n.tr("litematlist.button.reset");
        int formatResetBtnW = this.font.width(formatResetLabel) + 16;
        int formatResetBtnX = Math.max(2, leftX - formatResetBtnW - 4);
        ButtonGeneric formatResetBtn = new ButtonGeneric(formatResetBtnX, y, formatResetBtnW, 20,
                formatResetLabel);
        formatResetBtn.setRenderDefaultBackground(true);
        this.addButton(formatResetBtn, (IButtonActionListener) (b, mb) ->
                Minecraft.getInstance().setScreenAndShow(new ConfirmScreen(this,
                        I18n.tr("litematlist.hud.format.reset.title"),
                        I18n.tr("litematlist.hud.format.reset.message"),
                        () -> {
                            MaterialDataStore.persistedHudFormat = MaterialListHudRenderer.DEFAULT_HUD_FORMAT;
                            MaterialDataStore.savePersistence();
                            MaterialListHudRenderer.recomputeCachedDimensions();
                        })));

        // ---- 开关按钮：三个输入框右边为盒(1728)/组(64)开关，下方为显示HUD开关 ----
        boxBtnLabel = MaterialDataStore.persistedHudShowBox
                ? I18n.tr("litematlist.hud.scale_box_on") : I18n.tr("litematlist.hud.scale_box_off");
        boxBtnX = leftX + 200; boxBtnY = 74;
        boxBtnW = this.font.width(I18n.tr("litematlist.hud.scale_box_on")) + 16;
        ButtonGeneric boxBtn = new ButtonGeneric(boxBtnX, boxBtnY, boxBtnW, 20, "");
        boxBtn.setRenderDefaultBackground(true);
        this.addButton(boxBtn, (IButtonActionListener) (b, mb) -> {
            MaterialDataStore.persistedHudShowBox = !MaterialDataStore.persistedHudShowBox;
            MaterialDataStore.savePersistence();
            MaterialListHudRenderer.recomputeCachedDimensions();
            initGui();
        });

        groupBtnLabel = MaterialDataStore.persistedHudShowGroup
                ? I18n.tr("litematlist.hud.scale_group_on") : I18n.tr("litematlist.hud.scale_group_off");
        groupBtnX = leftX + 200; groupBtnY = 104;
        groupBtnW = this.font.width(I18n.tr("litematlist.hud.scale_group_on")) + 16;
        ButtonGeneric groupBtn = new ButtonGeneric(groupBtnX, groupBtnY, groupBtnW, 20, "");
        groupBtn.setRenderDefaultBackground(true);
        this.addButton(groupBtn, (IButtonActionListener) (b, mb) -> {
            MaterialDataStore.persistedHudShowGroup = !MaterialDataStore.persistedHudShowGroup;
            MaterialDataStore.savePersistence();
            MaterialListHudRenderer.recomputeCachedDimensions();
            initGui();
        });

        // 按比例保存HUD位置 开关（位于【组(64)】按钮下方）
        ratioBtnLabel = MaterialDataStore.persistedHudUseRatio
                ? I18n.tr("litematlist.hud.ratio_save_on") : I18n.tr("litematlist.hud.ratio_save_off");
        ratioBtnX = leftX + 200; ratioBtnY = 134;
        ratioBtnW = this.font.width(I18n.tr("litematlist.hud.ratio_save_on")) + 16;
        ButtonGeneric ratioBtn = new ButtonGeneric(ratioBtnX, ratioBtnY, ratioBtnW, 20, "");
        ratioBtn.setRenderDefaultBackground(true);
        this.addButton(ratioBtn, (IButtonActionListener) (b, mb) -> {
            boolean enable = !MaterialDataStore.persistedHudUseRatio;
            if (enable) {
                // 绝对坐标 → 比例（按当前校准锚点换算）
                MaterialDataStore.persistedHudRatioX = MaterialListHudRenderer.ratioXFor(
                        MaterialDataStore.persistedHudX, this.width,
                        MaterialDataStore.persistedHudAnchor, MaterialListHudRenderer.getCachedTotalW());
                MaterialDataStore.persistedHudRatioY = MaterialListHudRenderer.ratioYFor(
                        MaterialDataStore.persistedHudY, this.height,
                        MaterialDataStore.persistedHudAnchor, MaterialListHudRenderer.getCachedTotalH());
            } else {
                // 比例 → 绝对坐标（按当前校准锚点换算）
                MaterialDataStore.persistedHudX = MaterialListHudRenderer.getEffectiveHudX();
                MaterialDataStore.persistedHudY = MaterialListHudRenderer.getEffectiveHudY();
            }
            MaterialDataStore.persistedHudUseRatio = enable;
            MaterialDataStore.savePersistence();
            initGui();
        });

        // HUD 校准位置（四状态循环）：决定【调整HUD位置】界面以哪个角为原点换算 HUD 位置
        anchorBtnLabel = I18n.tr(anchorLabelKey());
        anchorBtnW = this.font.width(I18n.tr("litematlist.hud.anchor.topleft")) + 16;
        anchorBtnX = leftX + 200; anchorBtnY = 164;
        ButtonGeneric anchorBtn = new ButtonGeneric(anchorBtnX, anchorBtnY, anchorBtnW, 20, "");
        anchorBtn.setRenderDefaultBackground(true);
        this.addButton(anchorBtn, (IButtonActionListener) (b, mb) -> {
            int newAnchor = (MaterialDataStore.persistedHudAnchor + 1) % 4;
            if (MaterialDataStore.persistedHudUseRatio) {
                // 比例模式下切换锚点：按旧锚点取当前绝对位置，再按新锚点重新换算比例，保持 HUD 不跳动
                int absX = MaterialListHudRenderer.getEffectiveHudX();
                int absY = MaterialListHudRenderer.getEffectiveHudY();
                int hudW = MaterialListHudRenderer.getCachedTotalW();
                int hudH = MaterialListHudRenderer.getCachedTotalH();
                MaterialDataStore.persistedHudRatioX =
                        MaterialListHudRenderer.ratioXFor(absX, this.width, newAnchor, hudW);
                MaterialDataStore.persistedHudRatioY =
                        MaterialListHudRenderer.ratioYFor(absY, this.height, newAnchor, hudH);
            }
            MaterialDataStore.persistedHudAnchor = newAnchor;
            MaterialDataStore.savePersistence();
            initGui();
        });

        // 底部按钮 Y 坐标
        int buttonY = this.height - 38;

        // 显示HUD 开关（原在【上传区域】界面，现移入本界面，位于【盒(1728)】按钮上方同列）
        showHudBtnLabel = MaterialDataStore.persistedHudVisible
                ? I18n.tr("litematlist.button.hud_on") : I18n.tr("litematlist.button.hud_off");
        showHudBtnW = this.font.width(showHudBtnLabel) + 16;
        showHudBtnX = leftX + 200;
        showHudBtnY = 44;
        ButtonGeneric showHudBtn = new ButtonGeneric(showHudBtnX, showHudBtnY, showHudBtnW, 20, "");
        showHudBtn.setRenderDefaultBackground(true);
        this.addButton(showHudBtn, (IButtonActionListener) (b, mb) -> {
            MaterialDataStore.persistedHudVisible = !MaterialDataStore.persistedHudVisible;
            MaterialDataStore.savePersistence();
            if (MaterialDataStore.persistedHudVisible) {
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
            tempHudX = MaterialListHudRenderer.getEffectiveHudX();
            tempHudY = MaterialListHudRenderer.getEffectiveHudY();
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
            if (MaterialDataStore.persistedHudUseRatio) {
                // 比例模式：把拖拽后的绝对坐标按当前校准锚点换算为屏幕比例保存
                MaterialDataStore.persistedHudRatioX = MaterialListHudRenderer.ratioXFor(
                        tempHudX, this.width, MaterialDataStore.persistedHudAnchor,
                        MaterialListHudRenderer.getCachedTotalW());
                MaterialDataStore.persistedHudRatioY = MaterialListHudRenderer.ratioYFor(
                        tempHudY, this.height, MaterialDataStore.persistedHudAnchor,
                        MaterialListHudRenderer.getCachedTotalH());
            } else {
                MaterialDataStore.persistedHudX = tempHudX;
                MaterialDataStore.persistedHudY = tempHudY;
            }
            MaterialDataStore.savePersistence();
            MaterialListHudRenderer.updateMissingCounts();
            page = 0;
            initGui();
        });

        // 取消
        ButtonGeneric cancelBtn = new ButtonGeneric(this.width / 2 + 64, buttonY, 120, 20,
                I18n.tr("litematlist.button.cancel"));
        cancelBtn.setRenderDefaultBackground(true);
        this.addButton(cancelBtn, (IButtonActionListener) (b, mb) -> {
            page = 0;
            initGui();
        });
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks) {
        this.extractBackground(gfx, mouseX, mouseY, partialTicks);
        super.extractRenderState(gfx, mouseX, mouseY, partialTicks);

        if (page == 0) {
            // 显式渲染输入框（GuiBase 不会自动渲染 vanila 的 EditBox 控件）
            if (fontSizeField != null) fontSizeField.extractWidgetRenderState(gfx, mouseX, mouseY, partialTicks);
            if (bgAlphaField != null) bgAlphaField.extractWidgetRenderState(gfx, mouseX, mouseY, partialTicks);
            if (maxLinesField != null) maxLinesField.extractWidgetRenderState(gfx, mouseX, mouseY, partialTicks);
            if (formatField != null) formatField.extractWidgetRenderState(gfx, mouseX, mouseY, partialTicks);

            // 开关按钮的彩色状态文字（绿色=开，红色=关）
            GuiContext ctx = GuiContext.fromGuiGraphics(gfx);
            drawFormatTooltip(ctx, mouseX, mouseY);
            drawToggleLabel(ctx, boxBtnLabel, boxBtnX, boxBtnY, boxBtnW, MaterialDataStore.persistedHudShowBox);
            drawToggleLabel(ctx, groupBtnLabel, groupBtnX, groupBtnY, groupBtnW, MaterialDataStore.persistedHudShowGroup);
            drawToggleLabel(ctx, ratioBtnLabel, ratioBtnX, ratioBtnY, ratioBtnW, MaterialDataStore.persistedHudUseRatio);
            drawToggleLabel(ctx, showHudBtnLabel, showHudBtnX, showHudBtnY, showHudBtnW, MaterialDataStore.persistedHudVisible);

            // 校准位置按钮：白色文字（四状态选项，非开关）
            int anchorTextW = this.font.width(anchorBtnLabel);
            ctx.drawString(this.font, anchorBtnLabel,
                    anchorBtnX + (anchorBtnW - anchorTextW) / 2, anchorBtnY + 6, 0xFFFFFFFF);
        }

        if (page == 1) {
            GuiContext ctx = GuiContext.fromGuiGraphics(gfx);
            drawPreviewHud(ctx, gfx);
        }
    }

    /** 绘制开关按钮的彩色状态文字（空标签按钮的白色文字由这里覆盖为绿/红） */
    private void drawToggleLabel(GuiContext ctx, String label, int bx, int by, int bw, boolean on) {
        int color = on ? 0xFF55FF55 : 0xFFFF5555;
        int textW = this.font.width(label);
        ctx.drawString(this.font, label, bx + (bw - textW) / 2, by + 6, color);
    }

    /** 根据当前 HUD 实际占用面积绘制预览框（无上传列表时用提示文字作为占位） */
    private void drawPreviewHud(GuiContext ctx, GuiGraphicsExtractor gfx) {
        int fontSize = MaterialDataStore.persistedHudFontSize;
        int maxLines = MaterialDataStore.persistedHudMaxLines;
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
            String hint = I18n.tr(dragHintKey());
            int hintW = (int)(this.font.width(hint) * scale);
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

        ctx.fill(tempHudX, tempHudY, tempHudX + totalW, tempHudY + totalH, fillColor);
        // 边框
        ctx.fill(tempHudX, tempHudY, tempHudX + totalW, tempHudY + 1, borderColor);
        ctx.fill(tempHudX, tempHudY + totalH - 1, tempHudX + totalW, tempHudY + totalH, borderColor);
        ctx.fill(tempHudX, tempHudY, tempHudX + 1, tempHudY + totalH, borderColor);
        ctx.fill(tempHudX + totalW - 1, tempHudY, tempHudX + totalW, tempHudY + totalH, borderColor);

        // 提示文字（※ 标记锚点所在角；字体大小与HUD相同，支持换行）
        // 文字整体按锚点四角对齐：右侧锚点（右上/右下）每行右端贴框右侧；
        // 下侧锚点（左下/右下）文字块底部贴框下方（此前固定贴左上，与HUD实际位置不符）
        String hint = I18n.tr(dragHintKey());
        String[] lines = hint.split("\n");
        int anchor = MaterialDataStore.persistedHudAnchor;
        boolean rightAlignLabel = (anchor == 2 || anchor == 3);
        boolean bottomAlignLabel = (anchor == 1 || anchor == 3);
        // 提示文字须完整落在判定框内：框比文字窄/矮时整体等比缩小（修复右侧锚点时 ※ 超出框右缘）
        int maxLineW = 1;
        for (String line : lines) {
            maxLineW = Math.max(maxLineW, this.font.width(line));
        }
        float textScale = Math.min(scale, (float)(totalW - 4) / maxLineW);
        textScale = Math.min(textScale, (float)(totalH - 4) / (lines.length * 10));
        int startY = bottomAlignLabel
                ? Math.max(0, (int)((totalH - 4) / textScale) - lines.length * 10)
                : 0;
        gfx.pose().pushMatrix();
        gfx.pose().translate((float)(tempHudX + 2), (float)(tempHudY + 2));
        gfx.pose().scale(textScale, textScale);
        for (int li = 0; li < lines.length; li++) {
            int lineX = 0;
            if (rightAlignLabel) {
                int lineW = this.font.width(lines[li]);
                lineX = Math.max(0, (int)((totalW - 4) / textScale) - lineW);
            }
            ctx.drawString(this.font, lines[li], lineX, startY + li * 10, 0xFFFFFFFF);
        }
        gfx.pose().popMatrix();
    }

    @Override
    public boolean onMouseClicked(MouseButtonEvent event, boolean isDrag) {
        if (page == 0 && !isDrag) {
            focusTextFieldAt(event.x(), event.y());
        }
        if (page == 1 && event.button() == 0 && !isDrag) {
            int totalW = getPreviewBoxWidth();
            int totalH = getPreviewBoxHeight();
            double mouseX = event.x();
            double mouseY = event.y();
            if (mouseX >= tempHudX && mouseX <= tempHudX + totalW
                    && mouseY >= tempHudY && mouseY <= tempHudY + totalH) {
                dragging = true;
                dragOffsetX = (int)(mouseX - tempHudX);
                dragOffsetY = (int)(mouseY - tempHudY);
                return true;
            }
        }
        return super.onMouseClicked(event, isDrag);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging) {
            tempHudX = (int)event.x() - dragOffsetX;
            tempHudY = (int)event.y() - dragOffsetY;
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) { // ESC
            if (page == 1) {
                page = 0;
                initGui();
                return true;
            }
            // page 0: ESC 退回上传区域
            goBack();
            return true;
        }
        return super.keyPressed(event);
    }

    private int getPreviewBoxWidth() {
        // 优先使用 HUD 实际缓存宽度（与真实渲染完全一致）
        int cachedW = MaterialListHudRenderer.getCachedTotalW();
        if (cachedW > 0) return cachedW;
        int fontSize = MaterialDataStore.persistedHudFontSize;
        float scale = fontSize / 10.0f;
        var hudItems = MaterialListHudRenderer.getHudItems();
        int maxLines = MaterialDataStore.persistedHudMaxLines;
        int lineCount = Math.min(hudItems.size(), maxLines);
        if (hudItems.isEmpty()) {
            String hint = I18n.tr(dragHintKey());
            String[] hintLines = hint.split("\n");
            int maxHintW = 0;
            for (String line : hintLines) {
                int w = (int)(this.font.width(line) * scale);
                if (w > maxHintW) maxHintW = w;
            }
            return maxHintW + 16;
        }
        int iconSize = (int)(16 * scale);
        int maxNameW = 80;
        Minecraft client = Minecraft.getInstance();
        for (int i = 0; i < lineCount; i++) {
            int w = (int)(client.font.width(hudItems.get(i).name) * scale);
            if (w > maxNameW) maxNameW = w;
        }
        int missW = (int)(client.font.width(MaterialListHudRenderer.formatHudCount(9999)) * scale);
        return (int)(iconSize + 4 + maxNameW + 4 + missW + 8);
    }

    private int getPreviewBoxHeight() {
        // 优先使用 HUD 实际缓存高度（与真实渲染完全一致）
        int cachedH = MaterialListHudRenderer.getCachedTotalH();
        if (cachedH > 0) return cachedH;
        int fontSize = MaterialDataStore.persistedHudFontSize;
        float scale = fontSize / 10.0f;
        int maxLines = MaterialDataStore.persistedHudMaxLines;
        var hudItems = MaterialListHudRenderer.getHudItems();
        int lineCount = Math.min(hudItems.size(), maxLines);
        int lineHeight = (int)(10 * scale);
        int pageH = (int)(8 * scale);
        if (hudItems.isEmpty()) {
            String hint = I18n.tr(dragHintKey());
            int hintLines = hint.split("\n").length;
            return hintLines * lineHeight + 4 + pageH + 2;
        }
        return lineCount * lineHeight + 4 + pageH + 2;
    }

    /** 校准位置按钮的标签 key（按当前锚点四状态循环）。 */
    private String anchorLabelKey() {
        return switch (MaterialDataStore.persistedHudAnchor) {
            case 1 -> "litematlist.hud.anchor.bottomleft";
            case 2 -> "litematlist.hud.anchor.topright";
            case 3 -> "litematlist.hud.anchor.bottomright";
            default -> "litematlist.hud.anchor.topleft";
        };
    }

    /** 拖拽页提示文本 key（按当前锚点选择「※」所在位置的变体）。 */
    private String dragHintKey() {
        return switch (MaterialDataStore.persistedHudAnchor) {
            case 1 -> "litematlist.hud.drag_hint.bottomleft";
            case 2 -> "litematlist.hud.drag_hint.topright";
            case 3 -> "litematlist.hud.drag_hint.bottomright";
            default -> "litematlist.hud.drag_hint.topleft";
        };
    }

    /** 鼠标悬停在「HUD显示格式」标签上时，显示五个变量与 | 对齐规则。 */
    private void drawFormatTooltip(GuiContext ctx, int mouseX, int mouseY) {
        if (formatLabelH <= 0 || mouseX < formatLabelX || mouseX > formatLabelX + formatLabelW
                || mouseY < formatLabelY || mouseY > formatLabelY + formatLabelH) {
            return;
        }
        String[] lines = I18n.tr("litematlist.hud.format.tooltip").split("\\n");
        int textW = 0;
        for (String line : lines) {
            textW = Math.max(textW, this.font.width(line));
        }
        int x = mouseX + 8;
        int y = mouseY + 12;
        int boxH = lines.length * 10 + 4;
        ctx.fill(x, y, x + textW + 8, y + boxH, 0xF0000000);
        for (int i = 0; i < lines.length; i++) {
            ctx.drawString(this.font, lines[i], x + 4, y + 2 + i * 10, 0xFFFFFFFF);
        }
    }

    /** 点击输入框时显式设置焦点，避免页面重建后原版焦点状态不同步导致光标不显示。 */
    private void focusTextFieldAt(double mouseX, double mouseY) {
        EditBox[] fields = { fontSizeField, bgAlphaField, maxLinesField, formatField };
        EditBox hit = null;
        for (EditBox field : fields) {
            if (field != null && mouseX >= field.getX() && mouseX <= field.getX() + field.getWidth()
                    && mouseY >= field.getY() && mouseY <= field.getY() + field.getHeight()) {
                hit = field;
                break;
            }
        }
        // 同时设置父界面的焦点，键盘/字符事件才会路由到该输入框
        this.setFocused((net.minecraft.client.gui.components.events.GuiEventListener) hit);
        for (EditBox field : fields) {
            if (field != null) field.setFocused(field == hit);
        }
    }

    private void goBack() {
        Minecraft.getInstance().setScreenAndShow(parent);
    }
}