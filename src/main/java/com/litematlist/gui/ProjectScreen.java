package com.litematlist.gui;

import com.litematlist.I18n;
import com.litematlist.LitematListMod;
import com.litematlist.SchematicPreviewHelper;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.MinecraftClient;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.gui.DrawContext;
//#endif
import com.litematlist.config.Configs;
//#if MC == 12110 || MC == 12111
import net.minecraft.client.gui.Click;
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.gl.RenderPipelines;
//#endif
//#if MC == 12110 || MC == 12111
import net.minecraft.client.input.KeyInput;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$
//#endif
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.gui.DrawableHelper;
//#endif
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.util.math.MatrixStack;
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
/**
 * 项目文件夹内部视图 —— 显示项目内的材料列表。
 */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$/**
//$$ * 椤圭洰鏂囦欢澶瑰唴閮ㄨ鍥?鈥斺€?鏄剧ず椤圭洰鍐呯殑鏉愭枡鍒楄〃銆? */
//#endif
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

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // 拖拽排序
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // 鎷栨嫿鎺掑簭
//#endif
    private int draggedIndex = -1;
    private int dragStartY = 0;
    private boolean isDragging = false;

//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final Identifier PIN_EMPTY = Identifier.of("litematlist", "textures/gui/pin/empty.png");
//#elseif MC == 11701
//$$    private static final Identifier PIN_EMPTY = new Identifier("litematlist", "textures/gui/pin/empty.png");
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final Identifier PIN_FAVORITE = Identifier.of("litematlist", "textures/gui/pin/favorite.png");
//#elseif MC == 11701
//$$    private static final Identifier PIN_FAVORITE = new Identifier("litematlist", "textures/gui/pin/favorite.png");
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final Identifier PIN_HOVERED_EMPTY = Identifier.of("litematlist", "textures/gui/pin/hovered_empty.png");
//#elseif MC == 11701
//$$    private static final Identifier PIN_HOVERED_EMPTY = new Identifier("litematlist", "textures/gui/pin/hovered_empty.png");
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final Identifier PIN_HOVERED_FAVORITE = Identifier.of("litematlist", "textures/gui/pin/hovered_favorite.png");
//#elseif MC == 11701
//$$    private static final Identifier PIN_HOVERED_FAVORITE = new Identifier("litematlist", "textures/gui/pin/hovered_favorite.png");
//#endif

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

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 返回主页面按钮（左上角）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 杩斿洖涓婚〉闈㈡寜閽紙宸︿笂瑙掞級
//#endif
        ButtonGeneric backBtn = new ButtonGeneric(10, 8, 100, 20, I18n.tr("litematlist.button.back_to_main_page"));
        backBtn.setRenderDefaultBackground(true);
        this.addButton(backBtn, (IButtonActionListener) (b, mb) ->
                MinecraftClient.getInstance().setScreen(new MaterialListScreen()));

        createRowButtons();
    }

    private void createRowButtons() {
        List<MaterialListScreen.LoadedEntry> display = getDisplayEntries();
        int maxIdx = Math.min(display.size(), scrollOffset + visibleRows);
        boolean previewAvailable = SchematicPreviewHelper.isAvailable();

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 检查父项目是否已上传到上传区域
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 妫€鏌ョ埗椤圭洰鏄惁宸蹭笂浼犲埌涓婁紶鍖哄煙
//#endif
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
                        MinecraftClient.getInstance().setScreen(new RenameScreen(this, idx, entry.name())));
            }

            ButtonGeneric viewBtn = new ButtonGeneric(viewX, rowY + 1, viewBtnW, 20, I18n.tr("litematlist.button.view_material_list"));
            viewBtn.setRenderDefaultBackground(true);
            final int idx2 = realIdx;
            this.addButton(viewBtn, (IButtonActionListener) (b, mb) ->
                    MinecraftClient.getInstance().setScreen(
                            new MaterialDetailScreen(this, entry.name(), entry.filePath())));

            ButtonGeneric settingsBtn = new ButtonGeneric(settingsX, rowY + 1, settingsBtnW, 20, I18n.tr("litematlist.button.settings"));
            settingsBtn.setRenderDefaultBackground(true);
            this.addButton(settingsBtn, (IButtonActionListener) (b, mb) ->
                    MinecraftClient.getInstance().setScreen(new MaterialConfigScreen(this, entry)));

            ButtonGeneric moveOutBtn = new ButtonGeneric(moveOutX, rowY + 1, moveOutBtnW, 20, I18n.tr("litematlist.button.move_out"));
            moveOutBtn.setRenderDefaultBackground(true);
            if (parentLocked) moveOutBtn.setEnabled(false);
            final int idx3 = realIdx;
            this.addButton(moveOutBtn, (IButtonActionListener) (b, mb) -> {
                MaterialListScreen.moveOutOfProject(idx3);
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 刷新当前项目视图，留在文件夹中
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                // 鍒锋柊褰撳墠椤圭洰瑙嗗浘锛岀暀鍦ㄦ枃浠跺す涓?
//#endif
                List<MaterialListScreen.LoadedEntry> updated = MaterialListScreen.getProjectEntries(projectId);
                MinecraftClient.getInstance().setScreen(new ProjectScreen(parent, projectId, updated));
            });
        }
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
        drawListBackground(drawContext, mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$        drawListBackground(matrices, mouseX, mouseY);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawHeader(drawContext, mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$        drawHeader(matrices, mouseX, mouseY);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        super.render(drawContext, mouseX, mouseY, partialTicks);
//#elseif MC == 11701 || MC == 11904
//$$        super.render(matrices, mouseX, mouseY, partialTicks);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawListContent(drawContext, mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$        drawListContent(matrices, mouseX, mouseY);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawHeaderText(drawContext);
//#elseif MC == 11701 || MC == 11904
//$$        drawHeaderText(matrices);
//#endif
    }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void drawHeader(DrawContext drawContext, int mouseX, int mouseY) {
//#elseif MC == 11701 || MC == 11904
//$$    private void drawHeader(MatrixStack matrices, int mouseX, int mouseY) {
//#endif
        String fullText = I18n.tr("litematlist.header.name") + switch (sortMode) {
            case NAME_ASC -> "  ▲";
            case NAME_DESC -> "  ▼";
            case NONE -> "  ⇅";
        };
        int headerRight = 34 + this.textRenderer.getWidth(fullText) + 10;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(10, HEADER_TOP, headerRight, HEADER_TOP + HEADER_HEIGHT, 0x60000000);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, 10, HEADER_TOP, headerRight, HEADER_TOP + HEADER_HEIGHT, 0x60000000);
//#endif

        boolean hovered = mouseY >= HEADER_TOP && mouseY <= HEADER_TOP + HEADER_HEIGHT
                && mouseX >= 10 && mouseX <= headerRight;
        if (hovered) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(10, HEADER_TOP, headerRight, HEADER_TOP + HEADER_HEIGHT, 0x30FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, 10, HEADER_TOP, headerRight, HEADER_TOP + HEADER_HEIGHT, 0x30FFFFFF);
//#endif
        }
    }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void drawHeaderText(DrawContext drawContext) {
//#elseif MC == 11701 || MC == 11904
//$$    private void drawHeaderText(MatrixStack matrices) {
//#endif
        String fullText = I18n.tr("litematlist.header.name") + switch (sortMode) {
            case NAME_ASC -> "  ▲";
            case NAME_DESC -> "  ▼";
            case NONE -> "  ⇅";
        };
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawTextWithShadow(this.textRenderer, fullText, 34, HEADER_TOP + 5, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$        this.textRenderer.drawWithShadow(matrices, fullText, 34, HEADER_TOP + 5, 0xFFFFFFFF);
//#endif
    }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void drawListBackground(DrawContext drawContext, int mouseX, int mouseY) {
//#elseif MC == 11701 || MC == 11904
//$$    private void drawListBackground(MatrixStack matrices, int mouseX, int mouseY) {
//#endif
        int visibleRows = this.visibleRows;
        int listBottom = LIST_TOP + visibleRows * ROW_HEIGHT;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(8, LIST_TOP - 2, this.width - 8, listBottom + 2, 0x40000000);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, 8, LIST_TOP - 2, this.width - 8, listBottom + 2, 0x40000000);
//#endif

        List<MaterialListScreen.LoadedEntry> display = getDisplayEntries();
        int maxIdx = Math.min(display.size(), scrollOffset + visibleRows);
        for (int i = scrollOffset; i < maxIdx; i++) {
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;
            int color = (i - scrollOffset) % 2 == 0 ? 0x20FFFFFF : 0x10FFFFFF;
            // 拖拽中的行高亮
            if (i == draggedIndex && isDragging) {
                color = 0x60FFFF00;
            }
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(10, rowY, this.width - 10, rowY + ROW_HEIGHT, color);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, 10, rowY, this.width - 10, rowY + ROW_HEIGHT, color);
//#endif
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 鼠标悬停整行高亮（在按钮下方渲染）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 榧犳爣鎮仠鏁磋楂樹寒锛堝湪鎸夐挳涓嬫柟娓叉煋锛?
//#endif
        for (int i = scrollOffset; i < maxIdx; i++) {
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;
            if (mouseX >= 10 && mouseX <= this.width - 10
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.fill(10, rowY, this.width - 10, rowY + ROW_HEIGHT, 0x40FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$                DrawableHelper.fill(matrices, 10, rowY, this.width - 10, rowY + ROW_HEIGHT, 0x40FFFFFF);
//#endif
                break;
            }
        }
    }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void drawListContent(DrawContext drawContext, int mouseX, int mouseY) {
//#elseif MC == 11701 || MC == 11904
//$$    private void drawListContent(MatrixStack matrices, int mouseX, int mouseY) {
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawCenteredText(drawContext, this.centerTitle, this.width / 2, 12, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$        drawCenteredText(matrices, this.centerTitle, this.width / 2, 12, 0xFFFFFFFF);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawCenteredText(drawContext,
//#elseif MC == 11701 || MC == 11904
//$$        drawCenteredText(matrices,
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                "共 " + projectEntries.size() + " 个材料列表", this.width / 2, 24, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                "Total: " + projectEntries.size() + " material lists", this.width / 2, 24, 0xFFFFFFFF);
//#endif

        List<MaterialListScreen.LoadedEntry> display = getDisplayEntries();
        int maxIdx = Math.min(display.size(), scrollOffset + visibleRows);
        int buttonAreaStart = this.width - 210;

        for (int i = scrollOffset; i < maxIdx; i++) {
            MaterialListScreen.LoadedEntry entry = display.get(i);
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;

            String displayName = entry.name();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (MaterialListScreen.isTxtEntry(entry)) displayName += "（txt）";
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            if (MaterialListScreen.isTxtEntry(entry)) displayName += " (txt)";
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            else if (MaterialListScreen.isJsonEntry(entry)) displayName += "（JSON）";
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            else if (MaterialListScreen.isJsonEntry(entry)) displayName += " (JSON)";
//#endif

            int maxNameWidth = this.width - 300;
            if (this.textRenderer.getWidth(displayName) > maxNameWidth) {
                displayName = this.textRenderer.trimToWidth(displayName, maxNameWidth - 10) + "...";
            }
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawText(this.textRenderer, displayName, 34, rowY + 5, 0xFFFFFFFF, false);
//#elseif MC == 11701 || MC == 11904
//$$            this.textRenderer.draw(matrices, displayName, 34, rowY + 5, 0xFFFFFFFF);
//#endif
        }

        if (display.size() > visibleRows) {
            int listBottom = LIST_TOP + visibleRows * ROW_HEIGHT;
            String scrollInfo = (scrollOffset + 1) + "-" +
                    Math.min(scrollOffset + visibleRows, display.size()) + " / " + display.size();
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawCenteredText(drawContext, scrollInfo, this.width / 2, listBottom + 5, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            drawCenteredText(matrices, scrollInfo, this.width / 2, listBottom + 5, 0xFFFFFFFF);
//#endif
        }
    }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void drawCenteredText(DrawContext drawContext, String text, int centerX, int y, int color) {
//#elseif MC == 11701 || MC == 11904
//$$    private void drawCenteredText(MatrixStack matrices, String text, int centerX, int y, int color) {
//#endif
        int width = this.textRenderer.getWidth(text);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawText(this.textRenderer, text, centerX - width / 2, y, color, false);
//#elseif MC == 11701 || MC == 11904
//$$        this.textRenderer.draw(matrices, text, centerX - width / 2, y, color);
//#endif
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
//#if MC == 12110 || MC == 12111
    public boolean onMouseClicked(Click click, boolean isDrag) {
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$    public boolean onMouseClicked(int mouseX, int mouseY, int button) {
//#endif
//#if MC == 12110 || MC == 12111
        if (super.onMouseClicked(click, isDrag)) return true;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        if (super.onMouseClicked(mouseX, mouseY, button)) return true;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12110 || MC == 12111
        int mouseX = (int) click.x();
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        
//#endif
//#if MC == 12110 || MC == 12111
        int mouseY = (int) click.y();
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        
//#endif
//#if MC == 12110 || MC == 12111
        int button = click.button();
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (super.mouseClicked(mouseX, mouseY, mouseButton)) return true;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        int button = mouseButton;
//#endif

        if (button == 0) {
            String fullText = I18n.tr("litematlist.header.name") + switch (sortMode) {
                case NAME_ASC -> "  ▲";
                case NAME_DESC -> "  ▼";
                case NONE -> "  ⇅";
            };
            int headerRight = 34 + this.textRenderer.getWidth(fullText) + 10;
            if (mouseY >= HEADER_TOP && mouseY <= HEADER_TOP + HEADER_HEIGHT
                    && mouseX >= 10 && mouseX <= headerRight) {
                cycleSortMode();
                return true;
            }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 拖拽排序（在非按钮区域）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 鎷栨嫿鎺掑簭锛堝湪闈炴寜閽尯鍩燂級
//#endif
            if (Configs.Generic.ALLOW_MANUAL_REORDER.getBooleanValue()) {
                List<MaterialListScreen.LoadedEntry> display = getDisplayEntries();
//#if MC == 12110 || MC == 12111
                int row = getRowAtY(mouseY);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                int row = getRowAtY((int)mouseY);
//#endif
                int buttonAreaStart = this.width - 210;
                if (row >= 0 && row < display.size() && mouseX >= 10 && mouseX < buttonAreaStart) {
                    draggedIndex = row;
//#if MC == 12110 || MC == 12111
                    dragStartY = mouseY;
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                    dragStartY = (int)mouseY;
//#endif
                    isDragging = false;
                    return true;
                }
            }
        }
        return false;
    }

    private int getRowAtY(int mouseY) {
        if (mouseY < LIST_TOP) return -1;
        int relY = mouseY - LIST_TOP;
        int row = relY / ROW_HEIGHT;
        List<MaterialListScreen.LoadedEntry> display = getDisplayEntries();
        if (row + scrollOffset >= display.size()) return -1;
        return row + scrollOffset;
    }

    @Override
//#if MC == 12110 || MC == 12111
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
//#endif
//#if MC == 12110 || MC == 12111
        int mouseY = (int) click.y();
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        mouseY = (int) mouseY;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        
//#endif
//#if MC == 12110 || MC == 12111
        int button = click.button();
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        int button = mouseButton;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        
//#endif

        if (draggedIndex >= 0 && button == 0) {
            List<MaterialListScreen.LoadedEntry> display = getDisplayEntries();
//#if MC == 12110 || MC == 12111
            int currentRow = getRowAtY(mouseY);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            int currentRow = getRowAtY((int)mouseY);
//#endif
            if (currentRow >= 0 && currentRow < display.size() && currentRow != draggedIndex
                    && Math.abs(mouseY - dragStartY) > 5) {
                isDragging = true;
                MaterialListScreen.LoadedEntry dragged = display.get(draggedIndex);
                MaterialListScreen.LoadedEntry target = display.get(currentRow);
                int idx1 = projectEntries.indexOf(dragged);
                int idx2 = projectEntries.indexOf(target);
                if (idx1 >= 0 && idx2 >= 0) {
                    Collections.swap(projectEntries, idx1, idx2);
                }
                draggedIndex = currentRow;
//#if MC == 12110 || MC == 12111
                dragStartY = mouseY;
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                dragStartY = (int)mouseY;
//#endif
                sortMode = SortMode.NONE;
                initGui();
            }
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
        if (draggedIndex >= 0) {
            draggedIndex = -1;
            isDragging = false;
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
            int maxOffset = Math.max(0, getDisplayEntries().size() - visibleRows);
            scrollOffset = Math.min(scrollOffset, maxOffset);
            initGui();
            return true;
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        return super.mouseScrolled(mouseX, mouseY, amount);
//#endif
    }

    @Override
//#if MC == 12110 || MC == 12111
    public boolean keyPressed(KeyInput keyInput) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
//#endif
//#if MC == 12110 || MC == 12111
        if (keyInput.key() == 256) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        if (keyCode == 256) {
//#endif
            MinecraftClient.getInstance().setScreen(new MaterialListScreen());
            return true;
        }
//#if MC == 12110 || MC == 12111
        return super.keyPressed(keyInput);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        return super.keyPressed(keyCode, scanCode, modifiers);
//#endif
    }
}
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$
//$$
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$
//$$
//$$
//#endif