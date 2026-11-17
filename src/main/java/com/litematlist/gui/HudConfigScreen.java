package com.litematlist.gui;

import com.litematlist.I18n;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
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
import net.minecraft.client.input.KeyInput;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.text.Text;
//#elseif MC == 11701
//$$import net.minecraft.text.LiteralText;
//#endif
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.gui.DrawableHelper;
//#endif
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.util.math.MatrixStack;
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
/**
 * HUD 界面设置 —— 分两页：配置属性 + 拖拽位置。
 */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$/**
//$$ * HUD 鐣岄潰璁剧疆 鈥斺€?鍒嗕袱椤碉細閰嶇疆灞炴€?+ 鎷栨嫿浣嶇疆銆? */
//#endif
public class HudConfigScreen extends GuiBase {

    private final GuiBase parent;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private int page = 0; // 0=配置, 1=拖拽位置
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    private int page = 0; // 0=閰嶇疆, 1=鎷栨嫿浣嶇疆
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // 临时值（拖拽页面使用，保存时才写入持久化）
//#endif
    private int tempHudX, tempHudY;
    private boolean dragging = false;
    private int dragOffsetX, dragOffsetY;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // 配置页的输入框
//#endif
    private TextFieldWidget fontSizeField, bgAlphaField, maxLinesField, formatField;
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
//#if MC == 11701
//$$        if (fontSizeField != null) fontSizeField.setTextFieldFocused(false);
//$$        if (bgAlphaField != null) bgAlphaField.setTextFieldFocused(false);
//$$        if (maxLinesField != null) maxLinesField.setTextFieldFocused(false);
//$$        if (formatField != null) formatField.setTextFieldFocused(false);
//#else
        this.setFocused((net.minecraft.client.gui.Element) null);
//#endif
        this.clearChildren();

        if (page == 0) {
            initConfigPage();
        } else {
            initPositionPage();
        }
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ===================== 配置页 =====================
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ===================== 閰嶇疆椤?=====================
//#endif

    private void initConfigPage() {
        int leftX = this.width / 2 - 120;
        int y = 50;
        int fieldW = 60;
        int fieldH = 20;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 字体大小
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 瀛椾綋澶у皬
//#endif
        this.addLabel(leftX, y, 200, 14, 0xFFFFFFFF, I18n.tr("litematlist.hud.font_size") + " (1-32):");
        y += 16;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        fontSizeField = new TextFieldWidget(this.textRenderer, leftX, y, fieldW, fieldH, Text.empty());
//#elseif MC == 11701
//$$        fontSizeField = new TextFieldWidget(this.textRenderer, leftX, y, fieldW, fieldH, new LiteralText(""));
//#endif
        fontSizeField.setText(String.valueOf(MaterialDataStore.persistedHudFontSize));
        fontSizeField.setChangedListener(text -> {
            try {
                int val = Integer.parseInt(text.trim());
                if (val >= 1 && val <= 32) {
                    MaterialDataStore.persistedHudFontSize = val;
                    MaterialDataStore.savePersistence();
                    MaterialListHudRenderer.updateMissingCounts();
                }
            } catch (NumberFormatException ignored) {}
        });
        this.addSelectableChild(fontSizeField);

        y += 24;
        // 背景透明度
        this.addLabel(leftX, y, 200, 14, 0xFFFFFFFF, I18n.tr("litematlist.hud.bg_alpha") + " (0-100):");
        y += 16;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        bgAlphaField = new TextFieldWidget(this.textRenderer, leftX, y, fieldW, fieldH, Text.empty());
//#elseif MC == 11701
//$$        bgAlphaField = new TextFieldWidget(this.textRenderer, leftX, y, fieldW, fieldH, new LiteralText(""));
//#endif
        bgAlphaField.setText(String.valueOf(MaterialDataStore.persistedHudBgAlpha));
        bgAlphaField.setChangedListener(text -> {
            try {
                int val = Integer.parseInt(text.trim());
                if (val >= 0 && val <= 100) {
                    MaterialDataStore.persistedHudBgAlpha = val;
                    MaterialDataStore.savePersistence();
                    MaterialListHudRenderer.updateMissingCounts();
                }
            } catch (NumberFormatException ignored) {}
        });
        this.addSelectableChild(bgAlphaField);

        y += 24;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 写入行数
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鍐欏叆琛屾暟
//#endif
        this.addLabel(leftX, y, 200, 14, 0xFFFFFFFF, I18n.tr("litematlist.hud.max_lines") + " (1-20):");
        y += 16;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        maxLinesField = new TextFieldWidget(this.textRenderer, leftX, y, fieldW, fieldH, Text.empty());
//#elseif MC == 11701
//$$        maxLinesField = new TextFieldWidget(this.textRenderer, leftX, y, fieldW, fieldH, new LiteralText(""));
//#endif
        maxLinesField.setText(String.valueOf(MaterialDataStore.persistedHudMaxLines));
        maxLinesField.setChangedListener(text -> {
            try {
                int val = Integer.parseInt(text.trim());
                if (val >= 1 && val <= 20) {
                    MaterialDataStore.persistedHudMaxLines = val;
                    MaterialDataStore.savePersistence();
                    MaterialListHudRenderer.updateMissingCounts();
                }
            } catch (NumberFormatException ignored) {}
        });
        this.addSelectableChild(maxLinesField);

        // HUD 显示格式：五个变量 [icon] [name] [item] [box] [stack]，其余文字原样显示
        y += 20;
        formatLabelX = leftX;
        formatLabelY = y;
        String formatLabelText = I18n.tr("litematlist.hud.format") + ":";
        formatLabelW = Math.min(240, this.textRenderer.getWidth(formatLabelText));
        formatLabelH = 14;
        this.addLabel(leftX, y, 240, 14, 0xFFFFFFFF, formatLabelText);
        y += 16;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        formatField = new TextFieldWidget(this.textRenderer, leftX, y, 190, 20, Text.empty());
//#elseif MC == 11701
//$$        formatField = new TextFieldWidget(this.textRenderer, leftX, y, 190, 20, new LiteralText(""));
//#endif
        formatField.setMaxLength(256);
        formatField.setText(MaterialListHudRenderer.getHudFormat());
        formatField.setChangedListener(text -> {
            MaterialDataStore.persistedHudFormat = text;
            MaterialDataStore.savePersistence();
            MaterialListHudRenderer.recomputeCachedDimensions();
        });
        this.addSelectableChild(formatField);

        // 重置按钮（位于格式输入框左侧），确认后恢复默认格式
        String formatResetLabel = I18n.tr("litematlist.button.reset");
        int formatResetBtnW = this.textRenderer.getWidth(formatResetLabel) + 16;
        int formatResetBtnX = Math.max(2, leftX - formatResetBtnW - 4);
        ButtonGeneric formatResetBtn = new ButtonGeneric(formatResetBtnX, y, formatResetBtnW, 20,
                formatResetLabel);
        formatResetBtn.setRenderDefaultBackground(true);
        this.addButton(formatResetBtn, (IButtonActionListener) (b, mb) ->
                MinecraftClient.getInstance().setScreen(new ConfirmScreen(this,
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
        boxBtnW = this.textRenderer.getWidth(I18n.tr("litematlist.hud.scale_box_on")) + 16;
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
        groupBtnW = this.textRenderer.getWidth(I18n.tr("litematlist.hud.scale_group_on")) + 16;
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
        ratioBtnW = this.textRenderer.getWidth(I18n.tr("litematlist.hud.ratio_save_on")) + 16;
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
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
        // HUD 校准位置（四状态循环）：决定【调整HUD位置】界面以哪个角为原点换算 HUD 位置
        anchorBtnLabel = I18n.tr(anchorLabelKey());
        anchorBtnW = this.textRenderer.getWidth(I18n.tr("litematlist.hud.anchor.topleft")) + 16;
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
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
        // 底部按钮 Y 坐标
        int buttonY = this.height - 38;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
        // 显示HUD 开关（原在【上传区域】界面，现移入本界面，位于【盒(1728)】按钮上方同列）
        showHudBtnLabel = MaterialDataStore.persistedHudVisible
                ? I18n.tr("litematlist.button.hud_on") : I18n.tr("litematlist.button.hud_off");
        showHudBtnW = this.textRenderer.getWidth(showHudBtnLabel) + 16;
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
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
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

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 保存
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 淇濆瓨
//#endif
        ButtonGeneric saveBtn = new ButtonGeneric(this.width / 2 - 60, buttonY, btnW, 20,
                I18n.tr("litematlist.button.save"));
        saveBtn.setRenderDefaultBackground(true);
        this.addButton(saveBtn, (IButtonActionListener) (b, mb) -> goBack());

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 取消
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鍙栨秷
//#endif
        ButtonGeneric cancelBtn = new ButtonGeneric(this.width / 2 + 64, buttonY, btnW, 20,
                I18n.tr("litematlist.button.cancel"));
        cancelBtn.setRenderDefaultBackground(true);
        this.addButton(cancelBtn, (IButtonActionListener) (b, mb) -> goBack());
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ===================== 拖拽位置页 =====================
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ===================== 鎷栨嫿浣嶇疆椤?=====================
//#endif

    private void initPositionPage() {
        int buttonY = this.height - 38;
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
        // 按当前配置重新计算 HUD 实际尺寸，保证判定边框与真实 HUD 一致
        MaterialListHudRenderer.recomputeCachedDimensions();

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 重置
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 閲嶇疆
//#endif
        ButtonGeneric resetBtn = new ButtonGeneric(this.width / 2 - 184, buttonY, 120, 20,
                I18n.tr("litematlist.button.reset"));
        resetBtn.setRenderDefaultBackground(true);
        this.addButton(resetBtn, (IButtonActionListener) (b, mb) -> {
            int boxW = getPreviewBoxWidth();
            int boxH = getPreviewBoxHeight();
            tempHudX = (this.width - boxW) / 2;
            tempHudY = (this.height - boxH) / 2;
        });

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 保存
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 淇濆瓨
//#endif
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

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 取消
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鍙栨秷
//#endif
        ButtonGeneric cancelBtn = new ButtonGeneric(this.width / 2 + 64, buttonY, 120, 20,
                I18n.tr("litematlist.button.cancel"));
        cancelBtn.setRenderDefaultBackground(true);
        this.addButton(cancelBtn, (IButtonActionListener) (b, mb) -> {
            page = 0;
            initGui();
        });
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

        if (page == 0) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 显式渲染输入框（GuiBase 可能不会自动渲染 vanilla Drawable 子控件）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 鏄惧紡娓叉煋杈撳叆妗嗭紙GuiBase 鍙兘涓嶄細鑷姩娓叉煋 vanilla Drawable 瀛愭帶浠讹級
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (fontSizeField != null) fontSizeField.render(drawContext, mouseX, mouseY, partialTicks);
//#elseif MC == 11701 || MC == 11904
//$$            if (fontSizeField != null) fontSizeField.render(matrices, mouseX, mouseY, partialTicks);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (bgAlphaField != null) bgAlphaField.render(drawContext, mouseX, mouseY, partialTicks);
//#elseif MC == 11701 || MC == 11904
//$$            if (bgAlphaField != null) bgAlphaField.render(matrices, mouseX, mouseY, partialTicks);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (maxLinesField != null) maxLinesField.render(drawContext, mouseX, mouseY, partialTicks);
//#elseif MC == 11701 || MC == 11904
//$$            if (maxLinesField != null) maxLinesField.render(matrices, mouseX, mouseY, partialTicks);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (formatField != null) formatField.render(drawContext, mouseX, mouseY, partialTicks);
//#elseif MC == 11701 || MC == 11904
//$$            if (formatField != null) formatField.render(matrices, mouseX, mouseY, partialTicks);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawFormatTooltip(drawContext, mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$            drawFormatTooltip(matrices, mouseX, mouseY);
//#endif

            // 开关按钮的彩色状态文字（绿色=开，红色=关）
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawToggleLabel(drawContext, boxBtnLabel, boxBtnX, boxBtnY, boxBtnW,
//#elseif MC == 11701 || MC == 11904
//$$            drawToggleLabel(matrices, boxBtnLabel, boxBtnX, boxBtnY, boxBtnW,
//#endif
                    MaterialDataStore.persistedHudShowBox);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawToggleLabel(drawContext, groupBtnLabel, groupBtnX, groupBtnY, groupBtnW,
//#elseif MC == 11701 || MC == 11904
//$$            drawToggleLabel(matrices, groupBtnLabel, groupBtnX, groupBtnY, groupBtnW,
//#endif
                    MaterialDataStore.persistedHudShowGroup);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawToggleLabel(drawContext, ratioBtnLabel, ratioBtnX, ratioBtnY, ratioBtnW,
//#elseif MC == 11701 || MC == 11904
//$$            drawToggleLabel(matrices, ratioBtnLabel, ratioBtnX, ratioBtnY, ratioBtnW,
//#endif
                    MaterialDataStore.persistedHudUseRatio);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawToggleLabel(drawContext, showHudBtnLabel, showHudBtnX, showHudBtnY, showHudBtnW,
//#elseif MC == 11701 || MC == 11904
//$$            drawToggleLabel(matrices, showHudBtnLabel, showHudBtnX, showHudBtnY, showHudBtnW,
//#endif
                    MaterialDataStore.persistedHudVisible);
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
            // 校准位置按钮：白色文字（四状态选项，非开关）
            int anchorTextW = this.textRenderer.getWidth(anchorBtnLabel);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawText(this.textRenderer, anchorBtnLabel,
//#elseif MC == 11701 || MC == 11904
//$$            this.textRenderer.draw(matrices, anchorBtnLabel,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    anchorBtnX + (anchorBtnW - anchorTextW) / 2, anchorBtnY + 6, 0xFFFFFFFF, false);
//#elseif MC == 11701 || MC == 11904
//$$                    anchorBtnX + (anchorBtnW - anchorTextW) / 2, anchorBtnY + 6, 0xFFFFFFFF);
//#endif
        }

        if (page == 1) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawPreviewHud(drawContext);
//#elseif MC == 11701 || MC == 11904
//$$            drawPreviewHud(matrices);
//#endif
        }
    }

    /** 绘制开关按钮的彩色状态文字（空标签按钮的白色文字由这里覆盖为绿/红） */
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void drawToggleLabel(DrawContext drawContext, String label, int bx, int by, int bw, boolean on) {
//#elseif MC == 11701 || MC == 11904
//$$    private void drawToggleLabel(MatrixStack matrices, String label, int bx, int by, int bw, boolean on) {
//#endif
        int color = on ? 0xFF55FF55 : 0xFFFF5555;
        int textW = this.textRenderer.getWidth(label);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawText(this.textRenderer, label, bx + (bw - textW) / 2, by + 6, color, false);
//#elseif MC == 11701 || MC == 11904
//$$        this.textRenderer.draw(matrices, label, bx + (bw - textW) / 2, by + 6, color);
//#endif
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 根据当前 HUD 实际占用面积绘制预览框（无上传列表时用提示文字作为占位） */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 鏍规嵁褰撳墠 HUD 瀹為檯鍗犵敤闈㈢Н缁樺埗棰勮妗嗭紙鏃犱笂浼犲垪琛ㄦ椂鐢ㄦ彁绀烘枃瀛椾綔涓哄崰浣嶏級 */
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void drawPreviewHud(DrawContext drawContext) {
//#elseif MC == 11701 || MC == 11904
//$$    private void drawPreviewHud(MatrixStack matrices) {
//#endif
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
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 无上传列表：用提示文字宽度估算
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 鏃犱笂浼犲垪琛細鐢ㄦ彁绀烘枃瀛楀搴︿及绠?
//#endif
            String hint = I18n.tr(dragHintKey());
            int hintW = (int)(this.textRenderer.getWidth(hint) * scale);
            totalW = hintW + 16;
            totalH = (int)(lineHeight + 4 + 8 * scale + 2);
        } else {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 使用 HUD 实际计算出的宽高（渲染器缓存），判定边框与真实文字范围一致
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 浼扮畻鏈€澶у悕绉板搴?
//#endif
            totalW = MaterialListHudRenderer.getCachedTotalW();
            totalH = MaterialListHudRenderer.getCachedTotalH();
            if (totalW <= 0 || totalH <= 0) {
                totalW = (int)(iconSize + 4 + 120 + 8);
                totalH = Math.max(1, lineCount) * lineHeight + 4 + (int)(8 * scale) + 2;
            }
        }

        int borderColor = dragging ? 0xFFFFFF00 : 0xFFFFFFFF;
        int fillColor = 0x40000000;

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(tempHudX, tempHudY, tempHudX + totalW, tempHudY + totalH, fillColor);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, tempHudX, tempHudY, tempHudX + totalW, tempHudY + totalH, fillColor);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 边框
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 杈规
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(tempHudX, tempHudY, tempHudX + totalW, tempHudY + 1, borderColor);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, tempHudX, tempHudY, tempHudX + totalW, tempHudY + 1, borderColor);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(tempHudX, tempHudY + totalH - 1, tempHudX + totalW, tempHudY + totalH, borderColor);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, tempHudX, tempHudY + totalH - 1, tempHudX + totalW, tempHudY + totalH, borderColor);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(tempHudX, tempHudY, tempHudX + 1, tempHudY + totalH, borderColor);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, tempHudX, tempHudY, tempHudX + 1, tempHudY + totalH, borderColor);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(tempHudX + totalW - 1, tempHudY, tempHudX + totalW, tempHudY + totalH, borderColor);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, tempHudX + totalW - 1, tempHudY, tempHudX + totalW, tempHudY + totalH, borderColor);
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 提示文字（※ 标记锚点所在角；字体大小与HUD相同，支持换行）
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 文字整体按锚点四角对齐：右侧锚点（右上/右下）每行右端贴框右侧；
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 下侧锚点（左下/右下）文字块底部贴框下方（此前固定贴左上，与HUD实际位置不符）
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鎻愮ず鏂囧瓧锛堝乏涓婅瀵归綈锛屽瓧浣撳ぇ灏忎笌HUD鐩稿悓锛屾敮鎸佹崲琛岋級
//#endif
        String hint = I18n.tr(dragHintKey());
        String[] lines = hint.split("\n");
        int anchor = MaterialDataStore.persistedHudAnchor;
        boolean rightAlignLabel = (anchor == 2 || anchor == 3);
        boolean bottomAlignLabel = (anchor == 1 || anchor == 3);
        // 提示文字须完整落在判定框内：框比文字窄/矮时整体等比缩小（修复右侧锚点时 ※ 超出框右缘）
        int maxLineW = 1;
        for (String line : lines) {
            maxLineW = Math.max(maxLineW, this.textRenderer.getWidth(line));
        }
        float textScale = Math.min(scale, (float)(totalW - 4) / maxLineW);
        textScale = Math.min(textScale, (float)(totalH - 4) / (lines.length * 10));
        int startY = bottomAlignLabel
                ? Math.max(0, (int)((totalH - 4) / textScale) - lines.length * 10)
                : 0;
//#if MC == 12108 || MC == 12110 || MC == 12111
        drawContext.getMatrices().pushMatrix();
//#elseif MC == 11701 || MC == 11904
//$$        matrices.push();
//#elseif MC == 12001 || MC == 12101 || MC == 12104
//$$        drawContext.getMatrices().push();
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        drawContext.getMatrices().translate((float)(tempHudX + 2), (float)(tempHudY + 2));
//#elseif MC == 11701 || MC == 11904
//$$        matrices.translate((float)(tempHudX + 2), (float)(tempHudY + 2), 0.0f);
//#elseif MC == 12001 || MC == 12101 || MC == 12104
//$$        drawContext.getMatrices().translate((float)(tempHudX + 2), (float)(tempHudY + 2), 0.0f);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        drawContext.getMatrices().scale(textScale, textScale);
//#elseif MC == 11701 || MC == 11904
//$$        matrices.scale(textScale, textScale, 1.0f);
//#elseif MC == 12001 || MC == 12101 || MC == 12104
//$$        drawContext.getMatrices().scale(textScale, textScale, 1.0f);
//#endif
        for (int li = 0; li < lines.length; li++) {
            int lineX = 0;
            if (rightAlignLabel) {
                int lineW = this.textRenderer.getWidth(lines[li]);
                lineX = Math.max(0, (int)((totalW - 4) / textScale) - lineW);
            }
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawText(this.textRenderer, lines[li], lineX, startY + li * 10, 0xFFFFFFFF, true);
//#elseif MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, lines[li], lineX, startY + li * 10, 0xFFFFFFFF);
//#endif
        }
//#if MC == 12108 || MC == 12110 || MC == 12111
        drawContext.getMatrices().popMatrix();
//#elseif MC == 11701 || MC == 11904
//$$        matrices.pop();
//#elseif MC == 12001 || MC == 12101 || MC == 12104
//$$        drawContext.getMatrices().pop();
//#endif
    }

    @Override
//#if MC == 12110 || MC == 12111
    public boolean mouseClicked(Click click, boolean doubled) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$    public boolean mouseClicked(double mouseX, double mouseY, int button) {
//#endif
//#if MC == 12110 || MC == 12111
        if (page == 1 && click.button() == 0) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        if (page == 1 && mouseButton == 0) {
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        if (page == 1 && button == 0) {
//#endif
            int totalW = getPreviewBoxWidth();
            int totalH = getPreviewBoxHeight();
//#if MC == 12110 || MC == 12111
            double mouseX = click.x();
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$            
//#endif
//#if MC == 12110 || MC == 12111
            double mouseY = click.y();
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$            
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            // use parameters directly
//#endif
            if (mouseX >= tempHudX && mouseX <= tempHudX + totalW
                    && mouseY >= tempHudY && mouseY <= tempHudY + totalH) {
                dragging = true;
                dragOffsetX = (int)(mouseX - tempHudX);
                dragOffsetY = (int)(mouseY - tempHudY);
                return true;
            }
        }
//#if MC == 12110 || MC == 12111
        boolean handled = super.mouseClicked(click, doubled);
        if (page == 0) {
            focusTextFieldAt(click.x(), click.y());
        }
        return handled;
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        boolean handled = super.mouseClicked(mouseX, mouseY, mouseButton);
//$$        if (page == 0) {
//$$            focusTextFieldAt(mouseX, mouseY);
//$$        }
//$$        return handled;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        boolean handled = super.mouseClicked(mouseX, mouseY, button);
//$$        if (page == 0) {
//$$            focusTextFieldAt(mouseX, mouseY);
//$$        }
//$$        return handled;
//#endif
    }

    @Override
//#if MC == 12110 || MC == 12111
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
//#endif
        if (dragging) {
//#if MC == 12110 || MC == 12111
            double mouseX = click.x();
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$            
//#endif
//#if MC == 12110 || MC == 12111
            double mouseY = click.y();
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$            
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            // use parameters directly
//#endif
            tempHudX = (int)mouseX - dragOffsetX;
            tempHudY = (int)mouseY - dragOffsetY;
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
        if (dragging) {
            dragging = false;
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
//#if MC == 12110 || MC == 12111
    public boolean keyPressed(KeyInput keyInput) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
//#endif
//#if MC == 12110 || MC == 12111
        if (keyInput.key() == 256) { // ESC
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        if (keyCode == 256) { // ESC
//#endif
            if (page == 1) {
                page = 0;
                initGui();
                return true;
            }
            // page 0: ESC 退回上传区域
            goBack();
            return true;
        }
//#if MC == 12110 || MC == 12111
        return super.keyPressed(keyInput);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        return super.keyPressed(keyCode, scanCode, modifiers);
//#endif
    }

    private int getPreviewBoxWidth() {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 优先使用 HUD 实际缓存宽度（与真实渲染完全一致）
//#endif
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
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 优先使用 HUD 实际缓存高度（与真实渲染完全一致）
//#endif
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
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void drawFormatTooltip(DrawContext drawContext, int mouseX, int mouseY) {
//#elseif MC == 11701 || MC == 11904
//$$    private void drawFormatTooltip(MatrixStack matrices, int mouseX, int mouseY) {
//#endif
        if (formatLabelH <= 0 || mouseX < formatLabelX || mouseX > formatLabelX + formatLabelW
                || mouseY < formatLabelY || mouseY > formatLabelY + formatLabelH) {
            return;
        }
        String[] lines = I18n.tr("litematlist.hud.format.tooltip").split("\\n");
        int textW = 0;
        for (String line : lines) {
            textW = Math.max(textW, this.textRenderer.getWidth(line));
        }
        int x = mouseX + 8;
        int y = mouseY + 12;
        int boxH = lines.length * 10 + 4;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(x, y, x + textW + 8, y + boxH, 0xF0000000);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, x, y, x + textW + 8, y + boxH, 0xF0000000);
//#endif
        for (int i = 0; i < lines.length; i++) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawText(this.textRenderer, lines[i], x + 4, y + 2 + i * 10, 0xFFFFFFFF, false);
//#elseif MC == 11701 || MC == 11904
//$$            this.textRenderer.draw(matrices, lines[i], x + 4, y + 2 + i * 10, 0xFFFFFFFF);
//#endif
        }
    }

    /** 点击输入框时显式设置焦点，避免页面重建后原版焦点状态不同步导致光标不显示。 */
    private void focusTextFieldAt(double mouseX, double mouseY) {
        TextFieldWidget[] fields = { fontSizeField, bgAlphaField, maxLinesField, formatField };
        TextFieldWidget hit = null;
        for (TextFieldWidget field : fields) {
            if (field == null) continue;
//#if MC == 11701
//$$            int fieldX = field.x;
//$$            int fieldY = field.y;
//#else
            int fieldX = field.getX();
            int fieldY = field.getY();
//#endif
            if (mouseX >= fieldX && mouseX <= fieldX + field.getWidth()
                    && mouseY >= fieldY && mouseY <= fieldY + field.getHeight()) {
                hit = field;
                break;
            }
        }
//#if MC == 11701
//$$        this.setFocused(hit);
//$$        for (TextFieldWidget field : fields) {
//$$            if (field != null) field.setTextFieldFocused(field == hit);
//$$        }
//#else
        // 同时设置父界面焦点与控件自身焦点，键盘/字符事件才会路由且光标才会绘制
        this.setFocused((net.minecraft.client.gui.Element) hit);
        for (TextFieldWidget field : fields) {
            if (field != null) field.setFocused(field == hit);
        }
//#endif
    }

    private void goBack() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$
//$$
//$$
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$
//#endif