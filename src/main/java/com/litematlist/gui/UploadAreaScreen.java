package com.litematlist.gui;

import com.litematlist.I18n;
import com.litematlist.LitematListMod;
import com.litematlist.MaterialListInjector;
import com.litematlist.RawMaterialAnalyzer;
import com.litematlist.LitematicReader.MaterialEntry;
import com.litematlist.SchematicPreviewHelper;
import com.litematlist.gui.MaterialDataStore.RawMaterialEntry;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.MinecraftClient;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.gui.DrawContext;
//#endif
//#if MC == 12110 || MC == 12111
import net.minecraft.client.input.KeyInput;
//#endif

import java.nio.file.Path;
import java.util.*;
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.gui.DrawableHelper;
//#endif
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.util.math.MatrixStack;
//#endif

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
    private static final int BOX_GAP = 10;

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
        if (MaterialDataStore.playerControlPlusUploaded != null) {
            for (MaterialListScreen.LoadedEntry e : MaterialListScreen.getLoadedEntries()) {
                String p = e.filePath() != null ? e.filePath().toString() : e.name();
                if (p.equals(MaterialDataStore.playerControlPlusUploaded)) {
                    pcpEntry = e;
                    break;
                }
            }
        }

        // 跨会话恢复：配方树不持久化，若上传标记存在但配方树为空则自动重新生成
        if (pcpEntry != null && MaterialDataStore.playerControlPlusMaterialList == null) {
            if (!MaterialConfigScreen.injectPlayerControlPlus(pcpEntry)) {
                pcpEntry = null;
            }
        }

        // === tweakermore 方框 ===
        int btnGap = 6;
        int previewW = 0;
        if (uploaded != null && !uploaded.isProject && uploaded.filePath() != null) {
            previewW = this.textRenderer.getWidth(I18n.tr("litematlist.button.preview_schematic")) + 16;
        }
        int viewW = this.textRenderer.getWidth(I18n.tr("litematlist.button.view_material_list")) + 16;
        int reuploadW = this.textRenderer.getWidth(I18n.tr("litematlist.button.reupload")) + 16;
        int withdrawW = this.textRenderer.getWidth(I18n.tr("litematlist.button.withdraw")) + 16;

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

            // 撤出
            ButtonGeneric withdrawBtn = new ButtonGeneric(btnRight - withdrawW, tmRowY, withdrawW, BTN_H,
                    I18n.tr("litematlist.button.withdraw"));
            withdrawBtn.setRenderDefaultBackground(true);
            this.addButton(withdrawBtn, (IButtonActionListener) (b, mb) -> {
                // 撤出只清空上传区域，不改动 HUD 开关状态（开关随持久化保存）
                MaterialDataStore.savePersistence();
                MaterialListScreen.clearUploadedEntry();
                initGui();
            });
            btnRight -= withdrawW + btnGap;

            // 查看材料列表
            ButtonGeneric viewBtn = new ButtonGeneric(btnRight - viewW, tmRowY, viewW, BTN_H,
                    I18n.tr("litematlist.button.view_material_list"));
            viewBtn.setRenderDefaultBackground(true);
            this.addButton(viewBtn, (IButtonActionListener) (b, mb) -> {
                if (uploaded.isProject) {
                    List<MaterialListScreen.LoadedEntry> children = MaterialListScreen.getProjectEntries(uploaded.name());
                    MinecraftClient.getInstance().setScreen(new ProjectSummaryScreen(this, uploaded.name(), children));
                } else {
                    MinecraftClient.getInstance().setScreen(
                            new MaterialDetailScreen(this, uploaded.name(), uploaded.filePath(), true));
                }
            });
            btnRight -= viewW + btnGap;

            // 重新上传
            ButtonGeneric reuploadBtn = new ButtonGeneric(btnRight - reuploadW, tmRowY, reuploadW, BTN_H,
                    I18n.tr("litematlist.button.reupload"));
            reuploadBtn.setRenderDefaultBackground(true);
            this.addButton(reuploadBtn, (IButtonActionListener) (b, mb) -> {
                MaterialListInjector.injectIfNeeded();
            });
            btnRight -= reuploadW + btnGap;

            if (previewW > 0) {
                ButtonGeneric previewBtn = new ButtonGeneric(btnRight - previewW, tmRowY, previewW, BTN_H,
                        I18n.tr("litematlist.button.preview_schematic"));
                previewBtn.setRenderDefaultBackground(true);
                if (!SchematicPreviewHelper.isAvailable()) previewBtn.setEnabled(false);
                this.addButton(previewBtn, (IButtonActionListener) (b, mb) -> {
                    SchematicPreviewHelper.openFullscreenPreview(this, uploaded.filePath());
                });
                this.tmPreviewBtnDisabled = !SchematicPreviewHelper.isAvailable();
                this.tmPreviewBtnX = btnRight - previewW;
                this.tmPreviewBtnY = tmRowY;
                this.tmPreviewBtnW = previewW;
                this.tmPreviewBtnH = BTN_H;
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
                MaterialDataStore.playerControlPlusUploaded = null;
                MaterialDataStore.playerControlPlusMaterialList = null;
                MaterialDataStore.savePersistence();
                initGui();
            });
            btnRight -= withdrawW + btnGap;

            // 查看材料列表（原材料列表）
            ButtonGeneric pcpViewBtn = new ButtonGeneric(btnRight - viewW, pcpRowY, viewW, BTN_H,
                    I18n.tr("litematlist.button.view_raw_material_list"));
            pcpViewBtn.setRenderDefaultBackground(true);
            this.addButton(pcpViewBtn, (IButtonActionListener) (b, mb) -> {
                MinecraftClient.getInstance().setScreen(
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
        int withdrawAllW = this.textRenderer.getWidth(I18n.tr("litematlist.button.withdraw_all")) + 16;
        int withdrawAllX = this.width - withdrawAllW - 12;
        int withdrawAllY = (BOX_TOP_TWEAKERMORE + BOX_TOP_PCP + BOX_PADDING * 2 + ROW_HEIGHT) / 2 - BTN_H / 2;
        ButtonGeneric withdrawAllBtn = new ButtonGeneric(withdrawAllX, withdrawAllY, withdrawAllW, BTN_H,
                I18n.tr("litematlist.button.withdraw_all"));
        withdrawAllBtn.setRenderDefaultBackground(true);
        if (uploaded == null && pcpEntry == null) withdrawAllBtn.setEnabled(false);
        this.addButton(withdrawAllBtn, (IButtonActionListener) (b, mb) -> {
            // 清空 tweakermore 上传（不改动 HUD 开关状态，开关随持久化保存）
            MaterialListScreen.clearUploadedEntry();
            // 清空 PlayerControl++ 上传
            MaterialDataStore.playerControlPlusUploaded = null;
            MaterialDataStore.playerControlPlusMaterialList = null;
            MaterialDataStore.savePersistence();
            initGui();
        });
        // === 底部返回按钮 ===
        int buttonY = this.height - 38;
        ButtonGeneric backBtn = new ButtonGeneric(this.width / 2 - 60, buttonY, 120, 20, I18n.tr("litematlist.button.back"));
        backBtn.setRenderDefaultBackground(true);
        this.addButton(backBtn, (IButtonActionListener) (b, mb) ->
                MinecraftClient.getInstance().setScreen(parent));
    }

    @Override
//#if MC == 12110 || MC == 12111
    public boolean keyPressed(KeyInput keyInput) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
//#endif
//#if MC == 12110 || MC == 12111
        if (keyInput.key() == 256) { // ESC：返回上一级菜单
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        if (keyCode == 256) { // ESC：返回上一级菜单
//#endif
            MinecraftClient.getInstance().setScreen(parent);
            return true;
        }
//#if MC == 12110 || MC == 12111
        return super.keyPressed(keyInput);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        return super.keyPressed(keyCode, scanCode, modifiers);
//#endif
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

        MaterialListScreen.LoadedEntry uploaded = MaterialListScreen.getUploadedEntry();

        // === tweakermore 方框（红色） ===
        int tmBoxBottom = BOX_TOP_TWEAKERMORE + BOX_PADDING * 2 + ROW_HEIGHT;
        if (uploaded != null) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(tmBoxLeft, BOX_TOP_TWEAKERMORE, tmBoxRight, tmBoxBottom, 0x50FF0000);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, tmBoxLeft, BOX_TOP_TWEAKERMORE, tmBoxRight, tmBoxBottom, 0x50FF0000);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(tmBoxLeft, BOX_TOP_TWEAKERMORE, tmBoxRight, BOX_TOP_TWEAKERMORE + 1, 0xFF888888);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, tmBoxLeft, BOX_TOP_TWEAKERMORE, tmBoxRight, BOX_TOP_TWEAKERMORE + 1, 0xFF888888);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(tmBoxLeft, tmBoxBottom - 1, tmBoxRight, tmBoxBottom, 0xFF888888);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, tmBoxLeft, tmBoxBottom - 1, tmBoxRight, tmBoxBottom, 0xFF888888);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(tmBoxLeft, BOX_TOP_TWEAKERMORE, tmBoxLeft + 1, tmBoxBottom, 0xFF888888);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, tmBoxLeft, BOX_TOP_TWEAKERMORE, tmBoxLeft + 1, tmBoxBottom, 0xFF888888);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(tmBoxRight - 1, BOX_TOP_TWEAKERMORE, tmBoxRight, tmBoxBottom, 0xFF888888);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, tmBoxRight - 1, BOX_TOP_TWEAKERMORE, tmBoxRight, tmBoxBottom, 0xFF888888);
//#endif
        }

        // === PCP 方框（浅蓝色） ===
        int pcpBoxBottom = BOX_TOP_PCP + BOX_PADDING * 2 + ROW_HEIGHT;
        if (pcpEntry != null) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(pcpBoxLeft, BOX_TOP_PCP, pcpBoxRight, pcpBoxBottom, 0x400044FF);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, pcpBoxLeft, BOX_TOP_PCP, pcpBoxRight, pcpBoxBottom, 0x400044FF);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(pcpBoxLeft, BOX_TOP_PCP, pcpBoxRight, BOX_TOP_PCP + 1, 0xFF888888);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, pcpBoxLeft, BOX_TOP_PCP, pcpBoxRight, BOX_TOP_PCP + 1, 0xFF888888);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(pcpBoxLeft, pcpBoxBottom - 1, pcpBoxRight, pcpBoxBottom, 0xFF888888);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, pcpBoxLeft, pcpBoxBottom - 1, pcpBoxRight, pcpBoxBottom, 0xFF888888);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(pcpBoxLeft, BOX_TOP_PCP, pcpBoxLeft + 1, pcpBoxBottom, 0xFF888888);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, pcpBoxLeft, BOX_TOP_PCP, pcpBoxLeft + 1, pcpBoxBottom, 0xFF888888);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(pcpBoxRight - 1, BOX_TOP_PCP, pcpBoxRight, pcpBoxBottom, 0xFF888888);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, pcpBoxRight - 1, BOX_TOP_PCP, pcpBoxRight, pcpBoxBottom, 0xFF888888);
//#endif
        }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        super.render(drawContext, mouseX, mouseY, partialTicks);
//#elseif MC == 11701 || MC == 11904
//$$        super.render(matrices, mouseX, mouseY, partialTicks);
//#endif

        // tweakermore 标签
        String tmLabel = I18n.tr("litematlist.label.upload_to_tweakermore");
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawText(this.textRenderer, tmLabel, tmBoxLeft + 4, BOX_TOP_TWEAKERMORE + 2, 0xFFFFAAAA, false);
//#elseif MC == 11701 || MC == 11904
//$$        this.textRenderer.draw(matrices, tmLabel, tmBoxLeft + 4, BOX_TOP_TWEAKERMORE + 2, 0xFFFFAAAA);
//#endif

        if (uploaded == null) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawCenteredTextWithShadow(this.textRenderer, I18n.tr("litematlist.upload_area.empty"),
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    this.width / 2, BOX_TOP_TWEAKERMORE + BOX_PADDING + ROW_HEIGHT / 2, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, I18n.tr("litematlist.upload_area.empty"), this.width / 2 - this.textRenderer.getWidth(I18n.tr("litematlist.upload_area.empty")) / 2, BOX_TOP_TWEAKERMORE + BOX_PADDING + ROW_HEIGHT / 2, 0xFFFFFFFF);
//#endif
        } else {
            String displayName = uploaded.name();
            int nameMaxW = tmBoxWidth - BOX_PADDING * 2 - 200;
            if (this.textRenderer.getWidth(displayName) > nameMaxW) {
                displayName = this.textRenderer.trimToWidth(displayName, nameMaxW - 10) + "...";
            }
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawText(this.textRenderer, displayName,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    tmBoxLeft + BOX_PADDING, tmRowY + 5, 0xFFFFFFFF, false);
//#endif
//#if MC == 11701 || MC == 11904
//$$            this.textRenderer.draw(matrices, displayName, tmBoxLeft + BOX_PADDING, tmRowY + 5, 0xFFFFFFFF);
//#endif
        }

        // PCP 标签
        String pcpLabel = I18n.tr("litematlist.label.upload_to_pcp");
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawText(this.textRenderer, pcpLabel, pcpBoxLeft + 4, BOX_TOP_PCP + 2, 0xFFAAAACC, false);
//#elseif MC == 11701 || MC == 11904
//$$        this.textRenderer.draw(matrices, pcpLabel, pcpBoxLeft + 4, BOX_TOP_PCP + 2, 0xFFAAAACC);
//#endif

        if (pcpEntry == null) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawCenteredTextWithShadow(this.textRenderer, I18n.tr("litematlist.upload_area.empty"),
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    this.width / 2, BOX_TOP_PCP + BOX_PADDING + ROW_HEIGHT / 2, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, I18n.tr("litematlist.upload_area.empty"), this.width / 2 - this.textRenderer.getWidth(I18n.tr("litematlist.upload_area.empty")) / 2, BOX_TOP_PCP + BOX_PADDING + ROW_HEIGHT / 2, 0xFFFFFFFF);
//#endif
        } else {
            String displayName = pcpEntry.name();
            int nameMaxW = pcpBoxWidth - BOX_PADDING * 2 - 200;
            if (this.textRenderer.getWidth(displayName) > nameMaxW) {
                displayName = this.textRenderer.trimToWidth(displayName, nameMaxW - 10) + "...";
            }
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawText(this.textRenderer, displayName,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    pcpBoxLeft + BOX_PADDING, pcpRowY + 5, 0xFFFFFFFF, false);
//#endif
//#if MC == 11701 || MC == 11904
//$$            this.textRenderer.draw(matrices, displayName, pcpBoxLeft + BOX_PADDING, pcpRowY + 5, 0xFFFFFFFF);
//#endif
        }
    }
}