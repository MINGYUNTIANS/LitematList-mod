package com.litematlist.gui;

import com.litematlist.I18n;
import com.litematlist.LitematListMod;
import com.litematlist.MaterialListInjector;
import com.litematlist.RawMaterialAnalyzer;
import com.litematlist.LitematicReader.MaterialEntry;
import com.litematlist.SchematicPreviewHelper;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.render.GuiContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 上传区域界面 —— 显示已上传到上传区域和 PlayerControl++ 的材料列表。
 * 双列表：上方红色框为 tweakermore，下方浅蓝色框为 PlayerControl++。
 */
public class UploadAreaScreen extends GuiBase {

    private final GuiBase parent;
    private static final int BOX_PADDING = 12;
    private static final int BOX_TOP_TWEAKERMORE = 45;
    private static final int BOX_TOP_PCP = 130;
    private static final int ROW_HEIGHT = 40;
    private static final int BTN_H = 20;

    // tweakermore 方框
    private int tmBoxLeft, tmBoxRight, tmBoxWidth, tmRowY;
    private boolean tmPreviewBtnDisabled;
    private int tmPreviewBtnX, tmPreviewBtnY, tmPreviewBtnW, tmPreviewBtnH;

    // PCP 方框
    private int pcpBoxLeft, pcpBoxRight, pcpBoxWidth, pcpRowY;
    private MaterialListScreen.LoadedEntry pcpEntry;

    public UploadAreaScreen(GuiBase parent) {
        super();
        this.parent = parent;
        this.title = I18n.tr("litematlist.title.upload_area");
    }

    @Override
    public void initGui() {
        super.initGui();

        MaterialListScreen.LoadedEntry uploaded = MaterialListScreen.getUploadedEntry();

        // 查找 PCP 上传条目
        pcpEntry = null;
        if (MaterialDetailScreen.playerControlPlusUploaded != null) {
            for (MaterialListScreen.LoadedEntry e : MaterialListScreen.getLoadedEntries()) {
                String p = e.filePath() != null ? e.filePath().toString() : e.name();
                if (p.equals(MaterialDetailScreen.playerControlPlusUploaded)) {
                    pcpEntry = e;
                    break;
                }
            }
        }

        // 跨会话恢复：配方树不持久化，若上传标记存在但配方树为空则自动重新生成
        if (pcpEntry != null && MaterialDetailScreen.playerControlPlusMaterialList == null) {
            if (!MaterialConfigScreen.injectPlayerControlPlus(pcpEntry)) {
                pcpEntry = null;
            }
        }

        // === tweakermore 方框 ===
        int btnGap = 6;
        int previewW = 0;
        if (uploaded != null && !uploaded.isProject && uploaded.filePath() != null) {
            previewW = this.font.width(I18n.tr("litematlist.button.preview_schematic")) + 16;
        }
        int viewW = this.font.width(I18n.tr("litematlist.button.view_material_list")) + 16;
        int reuploadW = this.font.width(I18n.tr("litematlist.button.reupload")) + 16;
        int withdrawW = this.font.width(I18n.tr("litematlist.button.withdraw")) + 16;

        int totalBtnW = 0;
        int btnCount = 0;
        if (previewW > 0) { totalBtnW += previewW; btnCount++; }
        totalBtnW += reuploadW; btnCount++;
        totalBtnW += viewW; btnCount++;
        totalBtnW += withdrawW; btnCount++;
        if (btnCount > 1) totalBtnW += (btnCount - 1) * btnGap;

        int nameAreaW = 200;
        int boxContentW = nameAreaW + 20 + totalBtnW;
        this.tmBoxWidth = Math.max(boxContentW + BOX_PADDING * 2, 420);
        this.tmBoxLeft = (this.width - tmBoxWidth) / 2;
        this.tmBoxRight = tmBoxLeft + tmBoxWidth;
        this.tmRowY = BOX_TOP_TWEAKERMORE + BOX_PADDING + (ROW_HEIGHT - BTN_H) / 2;

        if (uploaded != null) {
            int btnRight = tmBoxRight - BOX_PADDING;

            // 撤出按钮
            ButtonGeneric withdrawBtn = new ButtonGeneric(btnRight - withdrawW, tmRowY, withdrawW, BTN_H,
                    I18n.tr("litematlist.button.withdraw"));
            withdrawBtn.setRenderDefaultBackground(true);
            this.addButton(withdrawBtn, (IButtonActionListener) (b, mb) -> {
                MaterialDetailScreen.persistedHudVisible = false;
                MaterialDetailScreen.savePersistence();
                MaterialListScreen.clearUploadedEntry();
                initGui();
            });
            btnRight -= withdrawW + btnGap;

            // 查看材料列表按钮
            ButtonGeneric viewBtn = new ButtonGeneric(btnRight - viewW, tmRowY, viewW, BTN_H,
                    I18n.tr("litematlist.button.view_material_list"));
            viewBtn.setRenderDefaultBackground(true);
            this.addButton(viewBtn, (IButtonActionListener) (b, mb) -> {
                if (uploaded.isProject) {
                    List<MaterialListScreen.LoadedEntry> children = MaterialListScreen.getProjectEntries(uploaded.name());
                    Minecraft.getInstance().setScreenAndShow(
                            new ProjectSummaryScreen(this, uploaded.name(), children));
                } else {
                    Minecraft.getInstance().setScreenAndShow(
                            new MaterialDetailScreen(this, uploaded.name(), uploaded.filePath(), true));
                }
            });
            btnRight -= viewW + btnGap;

            // 重新上传按钮
            ButtonGeneric reuploadBtn = new ButtonGeneric(btnRight - reuploadW, tmRowY, reuploadW, BTN_H,
                    I18n.tr("litematlist.button.reupload"));
            reuploadBtn.setRenderDefaultBackground(true);
            this.addButton(reuploadBtn, (IButtonActionListener) (b, mb) -> {
                MaterialListInjector.injectIfNeeded();
            });
            btnRight -= reuploadW + btnGap;

            // 预览原理图按钮（仅非项目文件夹且有原理图文件时可用）
            if (previewW > 0) {
                boolean available = SchematicPreviewHelper.isAvailable();
                tmPreviewBtnDisabled = !available;
                tmPreviewBtnX = btnRight - previewW;
                tmPreviewBtnY = tmRowY;
                tmPreviewBtnW = previewW;
                tmPreviewBtnH = BTN_H;
                ButtonGeneric previewBtn = new ButtonGeneric(tmPreviewBtnX, tmPreviewBtnY, tmPreviewBtnW, tmPreviewBtnH,
                        I18n.tr("litematlist.button.preview_schematic"));
                previewBtn.setRenderDefaultBackground(true);
                if (!available) {
                    previewBtn.setEnabled(false);
                }
                this.addButton(previewBtn, (IButtonActionListener) (b, mb) -> {
                    SchematicPreviewHelper.openFullscreenPreview(this, uploaded.filePath());
                });
            }
        }

        // === PCP 方框 ===
        this.pcpBoxWidth = this.tmBoxWidth;
        this.pcpBoxLeft = this.tmBoxLeft;
        this.pcpBoxRight = this.tmBoxLeft + pcpBoxWidth;
        this.pcpRowY = BOX_TOP_PCP + BOX_PADDING + (ROW_HEIGHT - BTN_H) / 2;

        if (pcpEntry != null) {
            int btnRight = pcpBoxRight - BOX_PADDING;

            // 撤出
            ButtonGeneric pcpWithdrawBtn = new ButtonGeneric(btnRight - withdrawW, pcpRowY, withdrawW, BTN_H,
                    I18n.tr("litematlist.button.withdraw"));
            pcpWithdrawBtn.setRenderDefaultBackground(true);
            this.addButton(pcpWithdrawBtn, (IButtonActionListener) (b, mb) -> {
                MaterialDetailScreen.playerControlPlusUploaded = null;
                MaterialDetailScreen.playerControlPlusMaterialList = null;
                MaterialDetailScreen.savePersistence();
                initGui();
            });
            btnRight -= withdrawW + btnGap;

            // 查看材料列表（原材料列表）
            ButtonGeneric pcpViewBtn = new ButtonGeneric(btnRight - viewW, pcpRowY, viewW, BTN_H,
                    I18n.tr("litematlist.button.view_raw_material_list"));
            pcpViewBtn.setRenderDefaultBackground(true);
            this.addButton(pcpViewBtn, (IButtonActionListener) (b, mb) -> {
                Minecraft.getInstance().setScreenAndShow(
                        new RawMaterialScreen(this, pcpEntry.name(), pcpEntry.filePath()));
            });
            btnRight -= viewW + btnGap;

            // 重新上传（重新分析原材料并重建配方树）
            ButtonGeneric pcpReuploadBtn = new ButtonGeneric(btnRight - reuploadW, pcpRowY, reuploadW, BTN_H,
                    I18n.tr("litematlist.button.reupload"));
            pcpReuploadBtn.setRenderDefaultBackground(true);
            this.addButton(pcpReuploadBtn, (IButtonActionListener) (b, mb) -> {
                MaterialConfigScreen.injectPlayerControlPlus(pcpEntry);
            });
        }

        // === 界面右侧【全部撤出】按钮：同时清空 tweakermore 与 PlayerControl++ 上传 ===
        int withdrawAllW = this.font.width(I18n.tr("litematlist.button.withdraw_all")) + 16;
        int withdrawAllX = this.width - withdrawAllW - 12;
        int withdrawAllY = (BOX_TOP_TWEAKERMORE + BOX_TOP_PCP + BOX_PADDING * 2 + ROW_HEIGHT) / 2 - BTN_H / 2;
        ButtonGeneric withdrawAllBtn = new ButtonGeneric(withdrawAllX, withdrawAllY, withdrawAllW, BTN_H,
                I18n.tr("litematlist.button.withdraw_all"));
        withdrawAllBtn.setRenderDefaultBackground(true);
        if (uploaded == null && pcpEntry == null) withdrawAllBtn.setEnabled(false);
        this.addButton(withdrawAllBtn, (IButtonActionListener) (b, mb) -> {
            // 清空 tweakermore 上传
            MaterialDetailScreen.persistedHudVisible = false;
            MaterialListScreen.clearUploadedEntry();
            // 清空 PlayerControl++ 上传
            MaterialDetailScreen.playerControlPlusUploaded = null;
            MaterialDetailScreen.playerControlPlusMaterialList = null;
            MaterialDetailScreen.savePersistence();
            initGui();
        });

        // === 底部返回按钮 ===
        int buttonY = this.height - 38;
        ButtonGeneric backBtn = new ButtonGeneric(this.width / 2 - 60, buttonY, 120, 20, I18n.tr("litematlist.button.back"));
        backBtn.setRenderDefaultBackground(true);
        this.addButton(backBtn, (IButtonActionListener) (b, mb) ->
                Minecraft.getInstance().setScreenAndShow(parent));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks) {
        GuiContext ctx = GuiContext.fromGuiGraphics(gfx);
        this.extractBackground(gfx, mouseX, mouseY, partialTicks);

        MaterialListScreen.LoadedEntry uploaded = MaterialListScreen.getUploadedEntry();

        // === tweakermore 方框（红色，绘制在按钮层下方） ===
        int tmBoxBottom = BOX_TOP_TWEAKERMORE + BOX_PADDING * 2 + ROW_HEIGHT;
        if (uploaded != null) {
            ctx.fill(tmBoxLeft, BOX_TOP_TWEAKERMORE, tmBoxRight, tmBoxBottom, 0x50FF0000);
            ctx.fill(tmBoxLeft, BOX_TOP_TWEAKERMORE, tmBoxRight, BOX_TOP_TWEAKERMORE + 1, 0xFF888888);
            ctx.fill(tmBoxLeft, tmBoxBottom - 1, tmBoxRight, tmBoxBottom, 0xFF888888);
            ctx.fill(tmBoxLeft, BOX_TOP_TWEAKERMORE, tmBoxLeft + 1, tmBoxBottom, 0xFF888888);
            ctx.fill(tmBoxRight - 1, BOX_TOP_TWEAKERMORE, tmBoxRight, tmBoxBottom, 0xFF888888);
        }

        // === PCP 方框（浅蓝色） ===
        int pcpBoxBottom = BOX_TOP_PCP + BOX_PADDING * 2 + ROW_HEIGHT;
        if (pcpEntry != null) {
            ctx.fill(pcpBoxLeft, BOX_TOP_PCP, pcpBoxRight, pcpBoxBottom, 0x400044FF);
            ctx.fill(pcpBoxLeft, BOX_TOP_PCP, pcpBoxRight, BOX_TOP_PCP + 1, 0xFF888888);
            ctx.fill(pcpBoxLeft, pcpBoxBottom - 1, pcpBoxRight, pcpBoxBottom, 0xFF888888);
            ctx.fill(pcpBoxLeft, BOX_TOP_PCP, pcpBoxLeft + 1, pcpBoxBottom, 0xFF888888);
            ctx.fill(pcpBoxRight - 1, BOX_TOP_PCP, pcpBoxRight, pcpBoxBottom, 0xFF888888);
        }

        super.extractRenderState(gfx, mouseX, mouseY, partialTicks);

        // tweakermore 标签
        String tmLabel = I18n.tr("litematlist.label.upload_to_tweakermore");
        ctx.drawString(this.font, tmLabel, tmBoxLeft + 4, BOX_TOP_TWEAKERMORE + 2, 0xFFFFAAAA);

        if (uploaded == null) {
            ctx.drawCenteredString(this.font, I18n.tr("litematlist.upload_area.empty"),
                    this.width / 2, BOX_TOP_TWEAKERMORE + BOX_PADDING + ROW_HEIGHT / 2, 0xFFFFFFFF);
        } else {
            String displayName = uploaded.name();
            int nameMaxW = tmBoxWidth - BOX_PADDING * 2 - 200;
            if (this.font.width(displayName) > nameMaxW) {
                displayName = this.font.plainSubstrByWidth(displayName, nameMaxW - 10) + "...";
            }
            ctx.drawString(this.font, displayName, tmBoxLeft + BOX_PADDING, tmRowY + 5, 0xFFFFFFFF);
        }

        // PCP 标签
        String pcpLabel = I18n.tr("litematlist.label.upload_to_pcp");
        ctx.drawString(this.font, pcpLabel, pcpBoxLeft + 4, BOX_TOP_PCP + 2, 0xFFAAAACC);

        if (pcpEntry == null) {
            ctx.drawCenteredString(this.font, I18n.tr("litematlist.upload_area.empty"),
                    this.width / 2, BOX_TOP_PCP + BOX_PADDING + ROW_HEIGHT / 2, 0xFFFFFFFF);
        } else {
            String displayName = pcpEntry.name();
            int nameMaxW = pcpBoxWidth - BOX_PADDING * 2 - 200;
            if (this.font.width(displayName) > nameMaxW) {
                displayName = this.font.plainSubstrByWidth(displayName, nameMaxW - 10) + "...";
            }
            ctx.drawString(this.font, displayName, pcpBoxLeft + BOX_PADDING, pcpRowY + 5, 0xFFFFFFFF);
        }

        // 预览按钮禁用时的 tooltip
        if (tmPreviewBtnDisabled && mouseX >= tmPreviewBtnX && mouseX <= tmPreviewBtnX + tmPreviewBtnW
                && mouseY >= tmPreviewBtnY && mouseY <= tmPreviewBtnY + tmPreviewBtnH) {
            drawTooltipBox(ctx, I18n.tr("litematlist.tooltip.no_schematic_preview"), mouseX, mouseY);
        }
    }

    private void drawTooltipBox(GuiContext ctx, String tip, int mouseX, int mouseY) {
        int tipW = this.font.width(tip) + 8;
        int tipH = 16;
        int tipX = mouseX + 12;
        int tipY = mouseY - 20;
        if (tipX + tipW > this.width) tipX = this.width - tipW - 4;
        if (tipY < 4) tipY = mouseY + 16;

        ctx.fill(tipX, tipY, tipX + tipW, tipY + tipH, 0xCC000000);
        ctx.fill(tipX + 1, tipY + 1, tipX + tipW - 1, tipY + tipH - 1, 0xCC333333);
        ctx.drawString(this.font, tip, tipX + 4, tipY + 4, 0xFFFFFFFF);
    }
}
