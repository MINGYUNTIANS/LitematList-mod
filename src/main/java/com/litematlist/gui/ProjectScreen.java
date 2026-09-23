package com.litematlist.gui;

import com.litematlist.I18n;
import com.litematlist.LitematListMod;
import com.litematlist.SchematicPreviewHelper;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import com.litematlist.config.Configs;

import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 椤圭洰鏂囦欢澶瑰唴閮ㄨ鍥?鈥斺€?鏄剧ず椤圭洰鍐呯殑鏉愭枡鍒楄〃銆? */
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

    // 鎷栨嫿鎺掑簭
    private int draggedIndex = -1;
    private int dragStartY = 0;
    private boolean isDragging = false;

    private static final Identifier PIN_EMPTY = Identifier.of("litematlist", "textures/gui/pin/empty.png");
    private static final Identifier PIN_FAVORITE = Identifier.of("litematlist", "textures/gui/pin/favorite.png");
    private static final Identifier PIN_HOVERED_EMPTY = Identifier.of("litematlist", "textures/gui/pin/hovered_empty.png");
    private static final Identifier PIN_HOVERED_FAVORITE = Identifier.of("litematlist", "textures/gui/pin/hovered_favorite.png");

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

        // 杩斿洖涓婚〉闈㈡寜閽紙宸︿笂瑙掞級
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

        // 妫€鏌ョ埗椤圭洰鏄惁宸蹭笂浼犲埌涓婁紶鍖哄煙
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
                // 鍒锋柊褰撳墠椤圭洰瑙嗗浘锛岀暀鍦ㄦ枃浠跺す涓?
                List<MaterialListScreen.LoadedEntry> updated = MaterialListScreen.getProjectEntries(projectId);
                MinecraftClient.getInstance().setScreen(new ProjectScreen(parent, projectId, updated));
            });
        }
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(drawContext, mouseX, mouseY, partialTicks);
        drawListBackground(drawContext, mouseX, mouseY);
        drawHeader(drawContext, mouseX, mouseY);
        super.render(drawContext, mouseX, mouseY, partialTicks);
        drawListContent(drawContext, mouseX, mouseY);
        drawHeaderText(drawContext);
    }

    private void drawHeader(DrawContext drawContext, int mouseX, int mouseY) {
        String fullText = I18n.tr("litematlist.header.name") + switch (sortMode) {
            case NAME_ASC -> "  ▲";
            case NAME_DESC -> "  ▼";
            case NONE -> "  ⇅";
        };
        int headerRight = 34 + this.textRenderer.getWidth(fullText) + 10;
        drawContext.fill(10, HEADER_TOP, headerRight, HEADER_TOP + HEADER_HEIGHT, 0x60000000);

        boolean hovered = mouseY >= HEADER_TOP && mouseY <= HEADER_TOP + HEADER_HEIGHT
                && mouseX >= 10 && mouseX <= headerRight;
        if (hovered) {
            drawContext.fill(10, HEADER_TOP, headerRight, HEADER_TOP + HEADER_HEIGHT, 0x30FFFFFF);
        }
    }

    private void drawHeaderText(DrawContext drawContext) {
        String fullText = I18n.tr("litematlist.header.name") + switch (sortMode) {
            case NAME_ASC -> "  ▲";
            case NAME_DESC -> "  ▼";
            case NONE -> "  ⇅";
        };
        drawContext.drawTextWithShadow(this.textRenderer, fullText, 34, HEADER_TOP + 5, 0xFFFFFFFF);
    }

    private void drawListBackground(DrawContext drawContext, int mouseX, int mouseY) {
        int visibleRows = this.visibleRows;
        int listBottom = LIST_TOP + visibleRows * ROW_HEIGHT;
        drawContext.fill(8, LIST_TOP - 2, this.width - 8, listBottom + 2, 0x40000000);

        List<MaterialListScreen.LoadedEntry> display = getDisplayEntries();
        int maxIdx = Math.min(display.size(), scrollOffset + visibleRows);
        for (int i = scrollOffset; i < maxIdx; i++) {
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;
            int color = (i - scrollOffset) % 2 == 0 ? 0x20FFFFFF : 0x10FFFFFF;
            //  if (i == draggedIndex && isDragging) {
            //     color = 0x60FFFF00;
            // }
            drawContext.fill(10, rowY, this.width - 10, rowY + ROW_HEIGHT, color);
        }

        // 榧犳爣鎮仠鏁磋楂樹寒锛堝湪鎸夐挳涓嬫柟娓叉煋锛?
        for (int i = scrollOffset; i < maxIdx; i++) {
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;
            if (mouseX >= 10 && mouseX <= this.width - 10
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                drawContext.fill(10, rowY, this.width - 10, rowY + ROW_HEIGHT, 0x40FFFFFF);
                break;
            }
        }
    }

    private void drawListContent(DrawContext drawContext, int mouseX, int mouseY) {
        drawCenteredText(drawContext, this.centerTitle, this.width / 2, 12, 0xFFFFFFFF);
        drawCenteredText(drawContext,
                "Total: " + projectEntries.size() + " material lists", this.width / 2, 24, 0xFFFFFFFF);

        List<MaterialListScreen.LoadedEntry> display = getDisplayEntries();
        int maxIdx = Math.min(display.size(), scrollOffset + visibleRows);
        int buttonAreaStart = this.width - 210;

        for (int i = scrollOffset; i < maxIdx; i++) {
            MaterialListScreen.LoadedEntry entry = display.get(i);
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;

            String displayName = entry.name();
            if (MaterialListScreen.isTxtEntry(entry)) displayName += " (txt)";
            else if (MaterialListScreen.isJsonEntry(entry)) displayName += " (JSON)";

            int maxNameWidth = this.width - 300;
            if (this.textRenderer.getWidth(displayName) > maxNameWidth) {
                displayName = this.textRenderer.trimToWidth(displayName, maxNameWidth - 10) + "...";
            }
            drawContext.drawText(this.textRenderer, displayName, 34, rowY + 5, 0xFFFFFFFF, false);
        }

        if (display.size() > visibleRows) {
            int listBottom = LIST_TOP + visibleRows * ROW_HEIGHT;
            String scrollInfo = (scrollOffset + 1) + "-" +
                    Math.min(scrollOffset + visibleRows, display.size()) + " / " + display.size();
            drawCenteredText(drawContext, scrollInfo, this.width / 2, listBottom + 5, 0xFFFFFFFF);
        }
    }

    private void drawCenteredText(DrawContext drawContext, String text, int centerX, int y, int color) {
        int width = this.textRenderer.getWidth(text);
        drawContext.drawText(this.textRenderer, text, centerX - width / 2, y, color, false);
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
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (super.mouseClicked(mouseX, mouseY, mouseButton)) return true;
        int button = mouseButton;

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

            // 鎷栨嫿鎺掑簭锛堝湪闈炴寜閽尯鍩燂級
            if (Configs.Generic.ALLOW_MANUAL_REORDER.getBooleanValue()) {
                List<MaterialListScreen.LoadedEntry> display = getDisplayEntries();
                int row = getRowAtY((int)mouseY);
                int buttonAreaStart = this.width - 210;
                if (row >= 0 && row < display.size() && mouseX >= 10 && mouseX < buttonAreaStart) {
                    draggedIndex = row;
                    dragStartY = (int)mouseY;
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
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
        mouseY = (int) mouseY;
        int button = mouseButton;

        if (draggedIndex >= 0 && button == 0) {
            List<MaterialListScreen.LoadedEntry> display = getDisplayEntries();
            int currentRow = getRowAtY((int)mouseY);
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
                dragStartY = (int)mouseY;
                sortMode = SortMode.NONE;
                initGui();
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, mouseButton, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (draggedIndex >= 0) {
            draggedIndex = -1;
            isDragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, mouseButton);
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            MinecraftClient.getInstance().setScreen(new MaterialListScreen());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}

