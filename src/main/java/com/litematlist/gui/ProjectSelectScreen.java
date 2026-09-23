package com.litematlist.gui;

import com.litematlist.I18n;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.util.Identifier;

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
    private static final Identifier FOLDER_ICON = Identifier.of("litematlist", "textures/gui/folder.png");

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
            MinecraftClient.getInstance().setScreen(new MaterialListScreen());
        });

        ButtonGeneric cancelBtn = new ButtonGeneric(this.width / 2 + 5, buttonY, 80, 20,
                I18n.tr("litematlist.button.cancel"));
        cancelBtn.setRenderDefaultBackground(true);
        this.addButton(cancelBtn, (IButtonActionListener) (b, mb) ->
                MinecraftClient.getInstance().setScreen(new MaterialListScreen()));
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(drawContext, mouseX, mouseY, partialTicks);
        super.render(drawContext, mouseX, mouseY, partialTicks);

        drawContext.drawCenteredTextWithShadow(this.textRenderer, this.centerTitle,
                this.width / 2, 12, 0xFFFFFFFF);

        int maxIdx = Math.min(projects.size(), scrollOffset + visibleRows);
        int listBottom = LIST_TOP + visibleRows * ROW_HEIGHT;
        drawContext.fill(8, LIST_TOP - 2, this.width - 8, listBottom + 2, 0x40000000);

        for (int i = scrollOffset; i < maxIdx; i++) {
            MaterialListScreen.LoadedEntry proj = projects.get(i);
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;

            boolean hovered = mouseX >= 10 && mouseX <= this.width - 10
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            if (i == selectedIdx) {
                drawContext.fill(10, rowY, this.width - 10, rowY + ROW_HEIGHT, 0x60FFFF00);
            } else if (hovered) {
                drawContext.fill(10, rowY, this.width - 10, rowY + ROW_HEIGHT, 0x40FFFFFF);
            } else if (i % 2 == 0) {
                drawContext.fill(10, rowY, this.width - 10, rowY + ROW_HEIGHT, 0x10FFFFFF);
            }

            drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, FOLDER_ICON, 20, rowY + 2,
                    0.0f, 0.0f, 16, 16, 16, 16);
            drawContext.drawTextWithShadow(this.textRenderer, proj.name(), 40, rowY + 5, 0xFFFFFFFF);
        }

        if (projects.size() > visibleRows) {
            String scrollInfo = (scrollOffset + 1) + "-" +
                    Math.min(scrollOffset + visibleRows, projects.size()) + " / " + projects.size();
            drawContext.drawCenteredTextWithShadow(this.textRenderer, scrollInfo,
                    this.width / 2, listBottom + 5, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean onMouseClicked(Click click, boolean isDrag) {
        if (super.onMouseClicked(click, isDrag)) return true;
        if (isDrag) return false;
        int mouseY = (int) click.y();
        if (click.button() == 0 && mouseY >= LIST_TOP) {
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
}