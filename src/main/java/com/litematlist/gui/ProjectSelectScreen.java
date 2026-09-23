package com.litematlist.gui;

import com.litematlist.I18n;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.resources.Identifier;
import fi.dy.masa.malilib.render.GuiContext;
import org.apache.commons.lang3.tuple.Pair;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.GpuSampler;
import org.joml.Matrix3x2f;

import java.util.List;

/**
 * 选择项目文件夹界面 —— 将材料列表移动到某个项目中。
 */
public class ProjectSelectScreen extends GuiBase {

    private final GuiBase parent;
    private final int entryIndex;
    private final List<MaterialListScreen.LoadedEntry> projects;
    private int selectedIdx = -1;
    private int scrollOffset = 0;
    /** 居中显示的标题（左上角 MaLib 标题已禁用） */
    private String centerTitle = "";
    private static final int ROW_HEIGHT = 22;
    private static final int LIST_TOP = 50;
    private int visibleRows;
    private static final Identifier FOLDER_ICON = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/folder.png");

    public ProjectSelectScreen(GuiBase parent, int entryIndex, List<MaterialListScreen.LoadedEntry> projects) {
        super();
        this.parent = parent;
        this.entryIndex = entryIndex;
        this.projects = projects;
        // 左上角标题由 MaLib 绘制，此处置空仅保留居中标题
        this.centerTitle = I18n.tr("litematlist.title.select_project");
    }

    @Override
    public void initGui() {
        super.initGui();
        this.visibleRows = (this.height - 120) / ROW_HEIGHT;

        int buttonY = this.height - 38;

        ButtonGeneric confirmBtn = new ButtonGeneric(this.width / 2 - 85, buttonY, 80, 20,
                I18n.tr("litematlist.button.ok"));
        confirmBtn.setRenderDefaultBackground(true);
        this.addButton(confirmBtn, (IButtonActionListener) (b, mb) -> {
            if (selectedIdx >= 0 && selectedIdx < projects.size()) {
                MaterialListScreen.moveToProject(entryIndex, projects.get(selectedIdx).name());
            }
            Minecraft.getInstance().setScreenAndShow(new MaterialListScreen());
        });

        ButtonGeneric cancelBtn = new ButtonGeneric(this.width / 2 + 5, buttonY, 80, 20,
                I18n.tr("litematlist.button.cancel"));
        cancelBtn.setRenderDefaultBackground(true);
        this.addButton(cancelBtn, (IButtonActionListener) (b, mb) ->
                Minecraft.getInstance().setScreenAndShow(new MaterialListScreen()));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks) {
        GuiContext ctx = GuiContext.fromGuiGraphics(gfx);
        this.extractBackground(gfx, mouseX, mouseY, partialTicks);
        super.extractRenderState(gfx, mouseX, mouseY, partialTicks);

        ctx.drawCenteredString(this.font, this.centerTitle,
                this.width / 2, 12, 0xFFFFFFFF);

        int maxIdx = Math.min(projects.size(), scrollOffset + visibleRows);
        int listBottom = LIST_TOP + visibleRows * ROW_HEIGHT;
        ctx.fill(8, LIST_TOP - 2, this.width - 8, listBottom + 2, 0x40000000);

        for (int i = scrollOffset; i < maxIdx; i++) {
            MaterialListScreen.LoadedEntry proj = projects.get(i);
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;

            boolean hovered = mouseX >= 10 && mouseX <= this.width - 10
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            if (i == selectedIdx) {
                ctx.fill(10, rowY, this.width - 10, rowY + ROW_HEIGHT, 0x60FFFF00);
            } else if (hovered) {
                ctx.fill(10, rowY, this.width - 10, rowY + ROW_HEIGHT, 0x40FFFFFF);
            } else if (i % 2 == 0) {
                ctx.fill(10, rowY, this.width - 10, rowY + ROW_HEIGHT, 0x10FFFFFF);
            }

            drawTextureIcon(ctx, FOLDER_ICON, 20, rowY + 2, 16, 16);
            ctx.drawString(this.font, proj.name(), 40, rowY + 5, 0xFFFFFFFF);
        }

        if (projects.size() > visibleRows) {
            String scrollInfo = (scrollOffset + 1) + "-" +
                    Math.min(scrollOffset + visibleRows, projects.size()) + " / " + projects.size();
            ctx.drawCenteredString(this.font, scrollInfo,
                    this.width / 2, listBottom + 5, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean onMouseClicked(MouseButtonEvent event, boolean isDrag) {
        if (super.onMouseClicked(event, isDrag)) return true;
        if (isDrag) return false;
        int mouseY = (int) event.y();
        if (event.button() == 0 && mouseY >= LIST_TOP) {
            int row = (mouseY - LIST_TOP) / ROW_HEIGHT + scrollOffset;
            if (row >= 0 && row < projects.size()) {
                selectedIdx = row;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseY >= LIST_TOP && mouseY <= LIST_TOP + visibleRows * ROW_HEIGHT) {
            scrollOffset = Math.max(0, scrollOffset - (int) verticalAmount);
            scrollOffset = Math.min(scrollOffset, Math.max(0, projects.size() - visibleRows));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
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