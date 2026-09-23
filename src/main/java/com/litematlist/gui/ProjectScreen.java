package com.litematlist.gui;

import com.litematlist.I18n;
import com.litematlist.LitematListMod;
import com.litematlist.SchematicPreviewHelper;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import fi.dy.masa.malilib.render.GuiContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 项目文件夹内部视图 —— 显示项目内的材料列表。
 */
public class ProjectScreen extends GuiBase {

    private final GuiBase parent;
    private final String projectId;
    /** 居中显示的标题（左上角 MaLib 标题已禁用） */
    private String centerTitle = "";
    private final List<MaterialListScreen.LoadedEntry> projectEntries;
    private int scrollOffset = 0;
    private static final int ROW_HEIGHT = 22;
    private static final int HEADER_TOP = 35;
    private static final int HEADER_HEIGHT = 18;
    private static final int LIST_TOP = HEADER_TOP + HEADER_HEIGHT + 2;
    private int visibleRows;

    private enum SortMode { NONE, NAME_ASC, NAME_DESC }
    private SortMode sortMode = SortMode.NONE;

    public ProjectScreen(GuiBase parent, String projectId, List<MaterialListScreen.LoadedEntry> projectEntries) {
        super();
        this.parent = parent;
        this.projectId = projectId;
        this.projectEntries = projectEntries;
        this.centerTitle = I18n.tr("litematlist.title.project", projectId);
    }

    private List<MaterialListScreen.LoadedEntry> getDisplayEntries() {
        List<MaterialListScreen.LoadedEntry> entries = new ArrayList<>(projectEntries);
        if (sortMode == SortMode.NAME_ASC) {
            entries.sort(Comparator.comparing((MaterialListScreen.LoadedEntry e) -> e.name(), String.CASE_INSENSITIVE_ORDER));
        } else if (sortMode == SortMode.NAME_DESC) {
            entries.sort(Comparator.comparing((MaterialListScreen.LoadedEntry e) -> e.name(), String.CASE_INSENSITIVE_ORDER).reversed());
        }
        return entries;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.visibleRows = (this.height - 40 - 5 - LIST_TOP) / ROW_HEIGHT;

        int buttonY = this.height - 38;

        // 返回主页面按钮（左上角）
        ButtonGeneric backBtn = new ButtonGeneric(10, 8, 100, 20, I18n.tr("litematlist.button.back_to_main_page"));
        backBtn.setRenderDefaultBackground(true);
        this.addButton(backBtn, (IButtonActionListener) (b, mb) ->
                Minecraft.getInstance().setScreenAndShow(new MaterialListScreen()));

        createRowButtons();
    }

    private void createRowButtons() {
        List<MaterialListScreen.LoadedEntry> display = getDisplayEntries();
        int maxIdx = Math.min(display.size(), scrollOffset + visibleRows);
        boolean previewAvailable = SchematicPreviewHelper.isAvailable();

        // 检查父项目是否已上传到上传区域
        MaterialListScreen.LoadedEntry uploaded = MaterialListScreen.getUploadedEntry();
        boolean parentLocked = uploaded != null && uploaded.isProject && projectId.equals(uploaded.name());

        for (int i = scrollOffset; i < maxIdx; i++) {
            MaterialListScreen.LoadedEntry entry = display.get(i);
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;
            int realIdx = MaterialListScreen.getLoadedEntries().indexOf(entry);

            int previewBtnW = 70;
            int viewBtnW = 70;
            int settingsBtnW = 46;
            int moveOutBtnW = 50;
            int gap = 2;
            int moveOutX = this.width - 12 - moveOutBtnW;
            int settingsX = moveOutX - settingsBtnW - gap;
            int viewX = settingsX - viewBtnW - gap;
            int previewX = viewX - previewBtnW - gap;

            if (!MaterialListScreen.isTxtEntry(entry) && !MaterialListScreen.isJsonEntry(entry)) {
                ButtonGeneric previewBtn = new ButtonGeneric(previewX, rowY + 1, previewBtnW, 20, I18n.tr("litematlist.button.preview_schematic"));
                previewBtn.setRenderDefaultBackground(true);
                if (!previewAvailable) {
                    previewBtn.setEnabled(false);
                }
                this.addButton(previewBtn, (IButtonActionListener) (b, mb) ->
                        SchematicPreviewHelper.openFullscreenPreview(this, entry.filePath()));
            } else if (MaterialListScreen.isTxtEntry(entry) || MaterialListScreen.isJsonEntry(entry)) {
                ButtonGeneric renameBtn = new ButtonGeneric(previewX, rowY + 1, previewBtnW, 20, I18n.tr("litematlist.button.rename"));
                renameBtn.setRenderDefaultBackground(true);
                if (parentLocked) renameBtn.setEnabled(false);
                final int idx = realIdx;
                this.addButton(renameBtn, (IButtonActionListener) (b, mb) ->
                        Minecraft.getInstance().setScreenAndShow(new RenameScreen(this, idx, entry.name())));
            }

            ButtonGeneric viewBtn = new ButtonGeneric(viewX, rowY + 1, viewBtnW, 20, I18n.tr("litematlist.button.view_material_list"));
            viewBtn.setRenderDefaultBackground(true);
            final int idx2 = realIdx;
            this.addButton(viewBtn, (IButtonActionListener) (b, mb) ->
                    Minecraft.getInstance().setScreenAndShow(
                            new MaterialDetailScreen(this, entry.name(), entry.filePath())));

            ButtonGeneric settingsBtn = new ButtonGeneric(settingsX, rowY + 1, settingsBtnW, 20, I18n.tr("litematlist.button.settings"));
            settingsBtn.setRenderDefaultBackground(true);
            this.addButton(settingsBtn, (IButtonActionListener) (b, mb) ->
                    Minecraft.getInstance().setScreenAndShow(new MaterialConfigScreen(this, entry)));

            ButtonGeneric moveOutBtn = new ButtonGeneric(moveOutX, rowY + 1, moveOutBtnW, 20, I18n.tr("litematlist.button.move_out"));
            moveOutBtn.setRenderDefaultBackground(true);
            if (parentLocked) moveOutBtn.setEnabled(false);
            final int idx3 = realIdx;
            this.addButton(moveOutBtn, (IButtonActionListener) (b, mb) -> {
                MaterialListScreen.moveOutOfProject(idx3);
                // 刷新当前项目视图，留在文件夹中
                List<MaterialListScreen.LoadedEntry> updated = MaterialListScreen.getProjectEntries(projectId);
                Minecraft.getInstance().setScreenAndShow(new ProjectScreen(parent, projectId, updated));
            });
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks) {
        GuiContext ctx = GuiContext.fromGuiGraphics(gfx);
        this.extractBackground(gfx, mouseX, mouseY, partialTicks);
        drawListBackground(ctx, mouseX, mouseY);
        drawHeader(ctx, mouseX, mouseY);
        super.extractRenderState(gfx, mouseX, mouseY, partialTicks);
        drawListContent(ctx, mouseX, mouseY);
        drawHeaderText(ctx);
    }

    private void drawHeader(GuiContext ctx, int mouseX, int mouseY) {
        String fullText = I18n.tr("litematlist.header.name") + switch (sortMode) {
            case NAME_ASC -> "  ▲";
            case NAME_DESC -> "  ▼";
            case NONE -> "  ⇅";
        };
        int headerRight = 34 + this.font.width(fullText) + 10;
        ctx.fill(10, HEADER_TOP, headerRight, HEADER_TOP + HEADER_HEIGHT, 0x60000000);

        boolean hovered = mouseY >= HEADER_TOP && mouseY <= HEADER_TOP + HEADER_HEIGHT
                && mouseX >= 10 && mouseX <= headerRight;
        if (hovered) {
            ctx.fill(10, HEADER_TOP, headerRight, HEADER_TOP + HEADER_HEIGHT, 0x30FFFFFF);
        }
    }

    private void drawHeaderText(GuiContext ctx) {
        String fullText = I18n.tr("litematlist.header.name") + switch (sortMode) {
            case NAME_ASC -> "  ▲";
            case NAME_DESC -> "  ▼";
            case NONE -> "  ⇅";
        };
        ctx.drawString(this.font, fullText, 34, HEADER_TOP + 5, 0xFFFFFFFF);
    }

    private void drawListBackground(GuiContext ctx, int mouseX, int mouseY) {
        int visibleRows = this.visibleRows;
        int listBottom = LIST_TOP + visibleRows * ROW_HEIGHT;
        ctx.fill(8, LIST_TOP - 2, this.width - 8, listBottom + 2, 0x40000000);

        List<MaterialListScreen.LoadedEntry> display = getDisplayEntries();
        int maxIdx = Math.min(display.size(), scrollOffset + visibleRows);
        for (int i = scrollOffset; i < maxIdx; i++) {
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;
            int color = (i - scrollOffset) % 2 == 0 ? 0x20FFFFFF : 0x10FFFFFF;
            ctx.fill(10, rowY, this.width - 10, rowY + ROW_HEIGHT, color);
        }

        // 鼠标悬停整行高亮（在按钮下方渲染）
        for (int i = scrollOffset; i < maxIdx; i++) {
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;
            if (mouseX >= 10 && mouseX <= this.width - 10
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                ctx.fill(10, rowY, this.width - 10, rowY + ROW_HEIGHT, 0x40FFFFFF);
                break;
            }
        }
    }

    private void drawListContent(GuiContext ctx, int mouseX, int mouseY) {
        drawCenteredText(ctx, this.centerTitle, this.width / 2, 12, 0xFFFFFFFF);
        drawCenteredText(ctx,
                "共 " + projectEntries.size() + " 个材料列表", this.width / 2, 24, 0xFFFFFFFF);

        List<MaterialListScreen.LoadedEntry> display = getDisplayEntries();
        int maxIdx = Math.min(display.size(), scrollOffset + visibleRows);

        for (int i = scrollOffset; i < maxIdx; i++) {
            MaterialListScreen.LoadedEntry entry = display.get(i);
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;

            String displayName = entry.name();
            if (MaterialListScreen.isTxtEntry(entry)) displayName += "（txt）";
            else if (MaterialListScreen.isJsonEntry(entry)) displayName += "（JSON）";

            int maxNameWidth = this.width - 300;
            if (this.font.width(displayName) > maxNameWidth) {
                displayName = this.font.plainSubstrByWidth(displayName, maxNameWidth - 10) + "...";
            }
            ctx.drawString(this.font, displayName, 34, rowY + 5, 0xFFFFFFFF);
        }

        if (display.size() > visibleRows) {
            int listBottom = LIST_TOP + visibleRows * ROW_HEIGHT;
            String scrollInfo = (scrollOffset + 1) + "-" +
                    Math.min(scrollOffset + visibleRows, display.size()) + " / " + display.size();
            drawCenteredText(ctx, scrollInfo, this.width / 2, listBottom + 5, 0xFFFFFFFF);
        }
    }

    private void drawCenteredText(GuiContext ctx, String text, int centerX, int y, int color) {
        int width = this.font.width(text);
        ctx.drawString(this.font, text, centerX - width / 2, y, color);
    }

    private void cycleSortMode() {
        sortMode = switch (sortMode) {
            case NONE -> SortMode.NAME_ASC;
            case NAME_ASC -> SortMode.NAME_DESC;
            case NAME_DESC -> SortMode.NONE;
        };
        scrollOffset = 0;
        initGui();
    }

    @Override
    public boolean onMouseClicked(MouseButtonEvent event, boolean isDrag) {
        if (super.onMouseClicked(event, isDrag)) return true;

        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        int button = event.button();

        if (button == 0) {
            String fullText = I18n.tr("litematlist.header.name") + switch (sortMode) {
                case NAME_ASC -> "  ▲";
                case NAME_DESC -> "  ▼";
                case NONE -> "  ⇅";
            };
            int headerRight = 34 + this.font.width(fullText) + 10;
            if (mouseY >= HEADER_TOP && mouseY <= HEADER_TOP + HEADER_HEIGHT
                    && mouseX >= 10 && mouseX <= headerRight) {
                cycleSortMode();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseY >= LIST_TOP && mouseY <= LIST_TOP + visibleRows * ROW_HEIGHT) {
            scrollOffset = Math.max(0, scrollOffset - (int) verticalAmount);
            int maxOffset = Math.max(0, getDisplayEntries().size() - visibleRows);
            scrollOffset = Math.min(scrollOffset, maxOffset);
            initGui();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            Minecraft.getInstance().setScreenAndShow(new MaterialListScreen());
            return true;
        }
        return super.keyPressed(event);
    }
}