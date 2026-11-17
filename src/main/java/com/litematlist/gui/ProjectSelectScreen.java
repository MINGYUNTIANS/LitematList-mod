package com.litematlist.gui;

import com.litematlist.I18n;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.MinecraftClient;
//#if MC == 12104
//$$import net.minecraft.client.render.RenderLayer;
//#endif
//#if MC == 12110 || MC == 12111
import net.minecraft.client.gui.Click;
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.gui.DrawContext;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.gl.RenderPipelines;
//#endif
//#if MC == 12110 || MC == 12111
import net.minecraft.client.input.KeyInput;
//#endif
import net.minecraft.util.Identifier;

import java.util.List;
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.gui.DrawableHelper;
//#endif
//#if MC == 11701 || MC == 11904
//$$import com.mojang.blaze3d.systems.RenderSystem;
//#endif
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.util.math.MatrixStack;
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
/**
 * 选择项目文件夹界面 —— 将材料列表移动到某个项目中。
 */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$/**
//$$ * 閫夋嫨椤圭洰鏂囦欢澶圭晫闈?鈥斺€?灏嗘潗鏂欏垪琛ㄧЩ鍔ㄥ埌鏌愪釜椤圭洰涓€? */
//#endif
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
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final Identifier FOLDER_ICON = Identifier.of("litematlist", "textures/gui/folder.png");
//#elseif MC == 11701
//$$    private static final Identifier FOLDER_ICON = new Identifier("litematlist", "textures/gui/folder.png");
//#endif

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
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public void render(DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        this.renderBackground(drawContext, mouseX, mouseY, partialTicks);
//#endif
//#if MC == 12101
//$$
//#endif
//#if MC == 12001
//$$        this.renderBackground(drawContext);
//#endif
//#if MC == 11701 || MC == 11904
//$$    public void render(MatrixStack matrices, int mouseX, int mouseY, float partialTicks) {
//#endif
//#if MC == 11701 || MC == 11904
//$$        this.renderBackground(matrices);
//#endif
//#if MC == 11701 || MC == 11904
//$$
//#endif
//#if MC == 12108 || MC == 12110

//#endif
        int maxIdx = Math.min(projects.size(), scrollOffset + visibleRows);
        int listBottom = LIST_TOP + visibleRows * ROW_HEIGHT;
//#if MC == 12101
//$$
//#endif
//#if MC == 12110

//#endif
        // 列表底色与「已上传」标红必须绘制在 super.render 之前：
        // MaLib 的 super.render 会叠加整屏 0xB0000000 遮罩（主界面已上传标红同样经此压暗），
        // 若标红画在 super.render 之后则亮度偏高，与主界面不一致。
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(8, LIST_TOP - 2, this.width - 8, listBottom + 2, 0x40000000);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, 8, LIST_TOP - 2, this.width - 8, listBottom + 2, 0x40000000);
//#endif
        for (int i = scrollOffset; i < maxIdx; i++) {
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;
            if (projects.get(i).uploaded()) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.fill(10, rowY, this.width - 10, rowY + ROW_HEIGHT, 0xFFAB0508);
//#elseif MC == 11701 || MC == 11904
//$$                DrawableHelper.fill(matrices, 10, rowY, this.width - 10, rowY + ROW_HEIGHT, 0xFFAB0508);
//#endif
            } else if (i % 2 == 0) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.fill(10, rowY, this.width - 10, rowY + ROW_HEIGHT, 0x10FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$                DrawableHelper.fill(matrices, 10, rowY, this.width - 10, rowY + ROW_HEIGHT, 0x10FFFFFF);
//#endif
            }
        }
//#if MC == 12101
//$$
//#endif
//#if MC == 12110

//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        super.render(drawContext, mouseX, mouseY, partialTicks);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawCenteredTextWithShadow(this.textRenderer, this.centerTitle,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                this.width / 2, 12, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$        super.render(matrices, mouseX, mouseY, partialTicks);
//#endif
//#if MC == 11701 || MC == 11904
//$$        this.textRenderer.drawWithShadow(matrices, this.centerTitle, this.width / 2 - this.textRenderer.getWidth(this.centerTitle) / 2, 12, 0xFFFFFFFF);
//#endif

        for (int i = scrollOffset; i < maxIdx; i++) {
            MaterialListScreen.LoadedEntry proj = projects.get(i);
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;

            boolean hovered = mouseX >= 10 && mouseX <= this.width - 10
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            if (i == selectedIdx) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.fill(10, rowY, this.width - 10, rowY + ROW_HEIGHT, 0x60FFFF00);
//#elseif MC == 11701 || MC == 11904
//$$                DrawableHelper.fill(matrices, 10, rowY, this.width - 10, rowY + ROW_HEIGHT, 0x60FFFF00);
//#endif
            } else if (hovered) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.fill(10, rowY, this.width - 10, rowY + ROW_HEIGHT, 0x40FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$                DrawableHelper.fill(matrices, 10, rowY, this.width - 10, rowY + ROW_HEIGHT, 0x40FFFFFF);
//#endif
            }

//#if MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, FOLDER_ICON, 20, rowY + 2,
//#elseif MC == 12001
//$$            drawContext.drawTexture(FOLDER_ICON, 20, rowY + 2,
//#elseif MC == 12101
//$$            drawContext.drawTexture( FOLDER_ICON, 20, rowY + 2,
//#elseif MC == 12104
//$$            drawContext.drawTexture(RenderLayer::getGuiTextured, FOLDER_ICON, 20, rowY + 2,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    0.0f, 0.0f, 16, 16, 16, 16);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTextWithShadow(this.textRenderer, proj.name(), 40, rowY + 5, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$            RenderSystem.setShaderTexture(0, FOLDER_ICON); DrawableHelper.drawTexture(matrices, 20, rowY + 2, 0.0f, 0.0f, 16, 16, 16, 16);
//#endif
//#if MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, proj.name(), 40, rowY + 5, 0xFFFFFFFF);
//#endif
        }

        if (projects.size() > visibleRows) {
            String scrollInfo = (scrollOffset + 1) + "-" +
                    Math.min(scrollOffset + visibleRows, projects.size()) + " / " + projects.size();
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawCenteredTextWithShadow(this.textRenderer, scrollInfo,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    this.width / 2, listBottom + 5, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, scrollInfo, this.width / 2 - this.textRenderer.getWidth(scrollInfo) / 2, listBottom + 5, 0xFFFFFFFF);
//#endif
        }
    }

    @Override
//#if MC == 12110 || MC == 12111
    public boolean onMouseClicked(Click click, boolean isDrag) {
//#endif
//#if MC == 12110 || MC == 12111
        if (super.onMouseClicked(click, isDrag)) return true;
//#endif
//#if MC == 12110 || MC == 12111
        if (isDrag) return false;
//#endif
//#if MC == 12110 || MC == 12111
        int mouseY = (int) click.y();
//#endif
//#if MC == 12110 || MC == 12111
        if (click.button() == 0 && mouseY >= LIST_TOP) {
//#endif
//#if MC == 12110 || MC == 12111
            int row = (mouseY - LIST_TOP) / ROW_HEIGHT + scrollOffset;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        if (super.mouseClicked(mouseX, mouseY, mouseButton)) return true;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        mouseY = (int) mouseY;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        if (mouseButton == 0 && mouseY >= LIST_TOP) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            int row = ((int)mouseY - LIST_TOP) / ROW_HEIGHT + scrollOffset;
//#endif
            if (row >= 0 && row < projects.size()) {
                selectedIdx = row;
                return true;
            }
        }
        return false;
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$    @Override
//#endif
//#if MC == 12104 || MC == 12108
//$$    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
//#endif
//#if MC == 12104 || MC == 12108
//$$        if (keyCode == 256) { // ESC：返回上一级菜单
//#endif
//#if MC == 12104 || MC == 12108
//$$            MinecraftClient.getInstance().setScreen(new MaterialListScreen());
//#endif
//#if MC == 12104 || MC == 12108
//$$            return true;
//#endif
//#if MC == 12104 || MC == 12108
//$$        }
//#endif
//#if MC == 12104 || MC == 12108
//$$        return super.keyPressed(keyCode, scanCode, modifiers);
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    @Override
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
//#endif
        if (mouseY >= LIST_TOP && mouseY <= LIST_TOP + visibleRows * ROW_HEIGHT) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            scrollOffset = Math.max(0, scrollOffset - (int) verticalAmount);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            scrollOffset = Math.max(0, scrollOffset - (int) amount);
//#endif
            scrollOffset = Math.min(scrollOffset, Math.max(0, projects.size() - visibleRows));
            return true;
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        return super.mouseScrolled(mouseX, mouseY, amount);
//#endif
    }
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$    @Override
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$        if (keyCode == 256) { // ESC：返回上一级菜单
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$            MinecraftClient.getInstance().setScreen(new MaterialListScreen());
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$            return true;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$        return super.keyPressed(keyCode, scanCode, modifiers);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$    }
//#endif
//#if MC == 12110 || MC == 12111

//#endif
//#if MC == 12110 || MC == 12111
    @Override
//#endif
//#if MC == 12110 || MC == 12111
    public boolean keyPressed(KeyInput keyInput) {
//#endif
//#if MC == 12110 || MC == 12111
        if (keyInput.key() == 256) { // ESC：返回上一级菜单
//#endif
//#if MC == 12110 || MC == 12111
            MinecraftClient.getInstance().setScreen(new MaterialListScreen());
//#endif
//#if MC == 12110 || MC == 12111
            return true;
//#endif
//#if MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12110 || MC == 12111
        return super.keyPressed(keyInput);
//#endif
//#if MC == 12110 || MC == 12111
    }
//#endif
}
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$
//$$
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$
//$$
//$$
//$$
//#endif