package com.litematlist.gui;
//#if MC == 11701
//$$import net.minecraft.text.LiteralText;
//#endif

import com.litematlist.LitematicaBridge;
import com.litematlist.LitematListMod;
import com.litematlist.I18n;
import com.litematlist.SchematicPreviewHelper;
import com.litematlist.SyncmaticaBridge;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;

import net.minecraft.client.MinecraftClient;
//#if MC == 12110 || MC == 12111
import net.minecraft.client.gui.Click;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$
//#endif
//#if MC == 12104
//$$import net.minecraft.client.render.RenderLayer;
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
import net.minecraft.util.Identifier;

import java.nio.file.Path;
import java.util.*;
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.gui.DrawableHelper;
//#endif
//#if MC == 11701 || MC == 11904
//$$import com.mojang.blaze3d.systems.RenderSystem;
//#endif
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.util.math.MatrixStack;
//#endif

public class MaterialListScreen extends GuiBase {

    private static final List<LoadedEntry> loadedEntries = new ArrayList<>();
    private static String lastKnownWorldId = null;  // 防御性检测：跨存档时自动清理

    private int scrollOffset = 0;
    private static final int ROW_HEIGHT = 22;
    private static final int HEADER_TOP = 35;
    private static final int HEADER_HEIGHT = 18;
    private static final int LIST_TOP = HEADER_TOP + HEADER_HEIGHT + 2;
    private static final int PIN_AREA_WIDTH = 22;
    private static final int PIN_ICON_SIZE = 16;
    private static final int PIN_ICON_X = 10;
    private static final int PIN_ICON_Y_OFFSET = 2;
    private int visibleRows;
    private boolean scrollbarDragging = false;

    // ---- 错误提示 ----
    private static String errorMessage = null;
    private static long errorMessageTime = 0;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 设置错误提示，2 秒后自动消失 */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 设置错误提示，3秒后自动消失 */
//#endif
    public static void setError(String msg) {
        errorMessage = msg;
        errorMessageTime = System.currentTimeMillis();
    }

    // ---- 置顶图标纹理 ----
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
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final Identifier PIN_FOCUSED = Identifier.of("litematlist", "textures/gui/pin/focused_outline.png");
//#elseif MC == 11701
//$$    private static final Identifier PIN_FOCUSED = new Identifier("litematlist", "textures/gui/pin/focused_outline.png");
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final Identifier FOLDER_ICON = Identifier.of("litematlist", "textures/gui/folder.png");
//#elseif MC == 11701
//$$    private static final Identifier FOLDER_ICON = new Identifier("litematlist", "textures/gui/folder.png");
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final Identifier SYNC_LOGO_TEXTURE = Identifier.of("litematlist", "textures/gui/syncmatica_logo.png");
//#elseif MC == 11701
//$$    private static final Identifier SYNC_LOGO_TEXTURE = new Identifier("litematlist", "textures/gui/syncmatica_logo.png");
//#endif

    // ---- 排序 ----
    private enum SortMode { NONE, NAME_ASC, NAME_DESC }
    private SortMode sortMode = SortMode.NONE;

    // ---- 拖拽 ----
    private int draggedIndex = -1;
    private int dragStartY = 0;
    private boolean isDragging = false;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- "从投影同步"按钮位置（用于 tooltip） ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- "从投影同步"按钮位置（用于 tooltip）----
//#endif
    private int syncBtnX, syncBtnY, syncBtnW, syncBtnH;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- "导入列表文件"和"添加材料列表"按钮位置（用于 tooltip） ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- "导入列表文件"和"添加材料列表"按钮位置（用于 tooltip）----
//#endif
    private int importBtnX, importBtnY, importBtnW, importBtnH;
    private int addBtnX, addBtnY, addBtnW, addBtnH;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 预览按钮位置列表（用于 tooltip） ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 预览按钮位置列表（用于 tooltip）----
//#endif
    private final List<PreviewBtnInfo> previewBtnInfos = new ArrayList<>();

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 上传区域按钮位置（用于 tooltip） ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 上传区域按钮位置（用于 tooltip）----
//#endif
    private int uploadAreaBtnX, uploadAreaBtnY, uploadAreaBtnW, uploadAreaBtnH;

    private record PreviewBtnInfo(int x, int y, int w, int h, boolean disabled) {}

    public MaterialListScreen() {
        super();
        this.title = I18n.tr("litematlist.title.material_list");
    }

    @Override
    public void initGui() {
        super.initGui();

        int listHeight = this.height - 40 - 5 - LIST_TOP;
        this.visibleRows = listHeight / ROW_HEIGHT;

        // "新建项目材料列表[+]" 按钮 —— 放在表头右侧
        String newProjLabel = I18n.tr("litematlist.button.new_project");
        int newProjW = this.textRenderer.getWidth(newProjLabel) + 16;
        ButtonGeneric newProjBtn = new ButtonGeneric(34 + getHeaderTextWidth() + 16, HEADER_TOP, newProjW, HEADER_HEIGHT, newProjLabel);
        newProjBtn.setRenderDefaultBackground(true);
        this.addButton(newProjBtn, (IButtonActionListener) (b, mb) ->
                MinecraftClient.getInstance().setScreen(new ProjectCreateScreen(this)));

        int buttonY = this.height - 38;
        int btnW = 80;
        int gap = 5;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        int totalBtnW = 5 * btnW + 4 * gap; // 5 个按钮 + 4 个间隔
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        int totalBtnW = 5 * btnW + 4 * gap;
//#endif
        int startX = (this.width - totalBtnW) / 2;

        // 导入列表文件
        importBtnX = startX;
        importBtnY = buttonY;
        importBtnW = btnW;
        importBtnH = 20;
        ButtonGeneric importBtn = new ButtonGeneric(importBtnX, importBtnY, importBtnW, importBtnH, I18n.tr("litematlist.button.import_list_file"));
        importBtn.setRenderDefaultBackground(true);
        this.addButton(importBtn, (IButtonActionListener) (b, mb) -> {
            LitematListMod.LOGGER.info("[MaterialListScreen] 点击「导入列表文件」");
            MinecraftClient.getInstance().setScreen(new TxtBrowserScreen(this));
        });

        // 添加材料列表
        addBtnX = startX + btnW + gap;
        addBtnY = buttonY;
        addBtnW = btnW;
        addBtnH = 20;
        ButtonGeneric addBtn = new ButtonGeneric(addBtnX, addBtnY, addBtnW, addBtnH, I18n.tr("litematlist.button.add_material_list"));
        addBtn.setRenderDefaultBackground(true);
        this.addButton(addBtn, (IButtonActionListener) (b, mb) -> {
            LitematListMod.LOGGER.info("[MaterialListScreen] 点击「添加材料列表」");
            MinecraftClient.getInstance().setScreen(new SchematicBrowserScreen(this));
        });

        // 从投影同步（记录按钮位置用于 tooltip 渲染）
        syncBtnX = startX + 2 * (btnW + gap);
        syncBtnY = buttonY;
        syncBtnW = btnW;
        syncBtnH = 20;
        ButtonGeneric syncBtn = new ButtonGeneric(syncBtnX, syncBtnY, syncBtnW, syncBtnH, I18n.tr("litematlist.button.sync_from_projection"));
        syncBtn.setRenderDefaultBackground(true);
        this.addButton(syncBtn, (IButtonActionListener) (b, mb) -> {
            LitematListMod.LOGGER.info("[MaterialListScreen] 点击「从投影同步」");
            if (SyncmaticaBridge.isModLoaded() && isShiftKeyDown()) {
                // shift+点击：从 Syncmatica 同步已认领的材料
                syncFromSyncmatica();
            } else {
                syncFromLitematica();
            }
            this.scrollOffset = 0;
            initGui();
        });

        // 关闭
        ButtonGeneric closeBtn = new ButtonGeneric(startX + 3 * (btnW + gap), buttonY, btnW, 20, I18n.tr("litematlist.button.close"));
        closeBtn.setRenderDefaultBackground(true);
        this.addButton(closeBtn, (IButtonActionListener) (b, mb) -> {
            LitematListMod.LOGGER.info("[MaterialListScreen] 点击「关闭」");
            closeGui(true);
        });

        // 配置
        ButtonGeneric configBtn = new ButtonGeneric(startX + 4 * (btnW + gap), buttonY, btnW, 20, I18n.tr("litematlist.button.config"));
        configBtn.setRenderDefaultBackground(true);
        this.addButton(configBtn, (IButtonActionListener) (b, mb) -> {
            LitematListMod.LOGGER.info("[MaterialListScreen] 点击「配置」");
            MinecraftClient.getInstance().setScreen(new GuiConfigs());
        });

        // 「上传区域」按钮（主页面右上角，作者署名下方；在右侧）
        {
            String uploadLabel = I18n.tr("litematlist.button.upload_area");
            int uploadW = this.textRenderer.getWidth(uploadLabel) + 16;
            uploadAreaBtnX = this.width - uploadW - 10;
            uploadAreaBtnY = 26;
            uploadAreaBtnW = uploadW;
            uploadAreaBtnH = 20;
            ButtonGeneric uploadAreaBtn = new ButtonGeneric(uploadAreaBtnX, uploadAreaBtnY, uploadAreaBtnW, uploadAreaBtnH, uploadLabel);
            uploadAreaBtn.setRenderDefaultBackground(true);
            this.addButton(uploadAreaBtn, (IButtonActionListener) (b, mb) -> {
                LitematListMod.LOGGER.info("[MaterialListScreen] 点击「上传区域」");
                MinecraftClient.getInstance().setScreen(new UploadAreaScreen(MaterialListScreen.this));
            });

            // HUD界面设置按钮（上传区域左侧）
            String hudConfigLabel = I18n.tr("litematlist.button.hud_config");
            int hudConfigW = this.textRenderer.getWidth(hudConfigLabel) + 16;
            ButtonGeneric hudConfigBtn = new ButtonGeneric(this.width - uploadW - hudConfigW - 14, 26, hudConfigW, 20, hudConfigLabel);
            hudConfigBtn.setRenderDefaultBackground(true);
            this.addButton(hudConfigBtn, (IButtonActionListener) (b, mb) -> {
                LitematListMod.LOGGER.info("[MaterialListScreen] 点击「HUD界面设置」");
                MinecraftClient.getInstance().setScreen(new HudConfigScreen(this));
            });
        }

        createRowButtons();
    }

    private void createRowButtons() {
        int visibleRows = this.getVisibleRows();
        List<LoadedEntry> display = getDisplayEntries();
        int maxIdx = Math.min(display.size(), scrollOffset + visibleRows);
        boolean previewAvailable = SchematicPreviewHelper.isAvailable();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        LitematListMod.LOGGER.info("[MaterialListScreen] createRowButtons: previewAvailable={}, entries={}, scrollOffset={}, visibleRows={}",
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                previewAvailable, loadedEntries.size(), scrollOffset, visibleRows);
//#endif

        previewBtnInfos.clear();

        for (int i = scrollOffset; i < maxIdx; i++) {
            LoadedEntry entry = display.get(i);
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;
            final int rowIdx = loadedEntries.indexOf(entry);

            if (entry.isProject) {
                // 项目文件夹：6个按钮 [打开] [重命名] [原材料] [查看总材料列表] [设置] [删除]
                boolean locked = entry.uploaded;
                int btnW = 70;
                int openBtnW = 50;
                int settingsBtnW = 46;
                int deleteBtnW = 50;
                int viewTotalW = 80;
                int rawMaterialsW = 60;
                int gap = 2;
                int deleteX = this.width - 12 - deleteBtnW;
                int settingsX = deleteX - settingsBtnW - gap;
                int viewTotalX = settingsX - viewTotalW - gap;
                int rawMaterialsX = viewTotalX - rawMaterialsW - gap;
                int renameX = rawMaterialsX - btnW - gap;
                int openX = renameX - openBtnW - gap;

                ButtonGeneric openBtn = new ButtonGeneric(openX, rowY + 1, openBtnW, 20, I18n.tr("litematlist.button.open"));
                openBtn.setRenderDefaultBackground(true);
                final String projectId = entry.name();
                this.addButton(openBtn, (IButtonActionListener) (b, mb) -> {
                    List<LoadedEntry> children = getProjectEntries(projectId);
                    MinecraftClient.getInstance().setScreen(new ProjectScreen(this, projectId, children));
                });

                ButtonGeneric renameBtn = new ButtonGeneric(renameX, rowY + 1, btnW, 20, I18n.tr("litematlist.button.rename"));
                renameBtn.setRenderDefaultBackground(true);
                if (locked) renameBtn.setEnabled(false);
                final int idx = rowIdx;
                this.addButton(renameBtn, (IButtonActionListener) (b, mb) ->
                        MinecraftClient.getInstance().setScreen(new RenameScreen(this, idx, entry.name())));

                ButtonGeneric rawMaterialsBtn = new ButtonGeneric(rawMaterialsX, rowY + 1, rawMaterialsW, 20, I18n.tr("litematlist.button.raw_materials"));
                rawMaterialsBtn.setRenderDefaultBackground(true);
                this.addButton(rawMaterialsBtn, (IButtonActionListener) (b, mb) -> {
                    List<LoadedEntry> children = getProjectEntries(projectId);
                    MinecraftClient.getInstance().setScreen(new RawMaterialScreen(this, projectId, null));
                });

                ButtonGeneric viewTotalBtn = new ButtonGeneric(viewTotalX, rowY + 1, viewTotalW, 20, I18n.tr("litematlist.button.view_total_material"));
                viewTotalBtn.setRenderDefaultBackground(true);
                this.addButton(viewTotalBtn, (IButtonActionListener) (b, mb) -> {
                    List<LoadedEntry> children = getProjectEntries(projectId);
                    MinecraftClient.getInstance().setScreen(new ProjectSummaryScreen(this, projectId, children));
                });

                ButtonGeneric settingsBtn = new ButtonGeneric(settingsX, rowY + 1, settingsBtnW, 20, I18n.tr("litematlist.button.settings"));
                settingsBtn.setRenderDefaultBackground(true);
                if (locked) settingsBtn.setEnabled(false);
                this.addButton(settingsBtn, (IButtonActionListener) (b, mb) ->
                        MinecraftClient.getInstance().setScreen(new MaterialConfigScreen(this, entry)));

                ButtonGeneric deleteBtn = new ButtonGeneric(deleteX, rowY + 1, deleteBtnW, 20, I18n.tr("litematlist.button.delete"));
                deleteBtn.setRenderDefaultBackground(true);
                if (locked) deleteBtn.setEnabled(false);
                final int idx2 = rowIdx;
                final String projName = entry.name();
                this.addButton(deleteBtn, (IButtonActionListener) (b, mb) -> {
                    MinecraftClient.getInstance().setScreen(new ConfirmScreen(
                            MaterialListScreen.this,
                            I18n.tr("litematlist.warn.delete_project_title"),
                            I18n.tr("litematlist.warn.delete_project", projName),
                            () -> deleteProject(idx2)));
                });
                continue;
            }

            boolean locked = entry.uploaded;

            if (SyncmaticaBridge.isSyncPath(entry.filePath())) {
                // Syncmatica 认领材料条目：仅 4 个按钮 [原材料] [查看材料列表] [设置] [移除]
                int rawMaterialsW = 60;
                int viewBtnW = 80;
                int settingsBtnW = 46;
                int removeBtnW = 50;
                int gap = 2;
                int removeX = this.width - 12 - removeBtnW;
                int settingsX = removeX - settingsBtnW - gap;
                int viewX = settingsX - viewBtnW - gap;
                int rawMaterialsX = viewX - rawMaterialsW - gap;

                ButtonGeneric rawMaterialsBtn = new ButtonGeneric(rawMaterialsX, rowY + 1, rawMaterialsW, 20, I18n.tr("litematlist.button.raw_materials"));
                rawMaterialsBtn.setRenderDefaultBackground(true);
                this.addButton(rawMaterialsBtn, (IButtonActionListener) (b, mb) ->
                        MinecraftClient.getInstance().setScreen(new RawMaterialScreen(this, entry.name(), entry.filePath())));

                ButtonGeneric viewBtn = new ButtonGeneric(viewX, rowY + 1, viewBtnW, 20, I18n.tr("litematlist.button.view_material_list"));
                viewBtn.setRenderDefaultBackground(true);
                this.addButton(viewBtn, (IButtonActionListener) (b, mb) ->
                        MinecraftClient.getInstance().setScreen(new MaterialDetailScreen(this, entry.name(), entry.filePath())));

                ButtonGeneric settingsBtn = new ButtonGeneric(settingsX, rowY + 1, settingsBtnW, 20, I18n.tr("litematlist.button.settings"));
                settingsBtn.setRenderDefaultBackground(true);
                this.addButton(settingsBtn, (IButtonActionListener) (b, mb) ->
                        MinecraftClient.getInstance().setScreen(new MaterialConfigScreen(this, entry)));

                ButtonGeneric removeBtn = new ButtonGeneric(removeX, rowY + 1, removeBtnW, 20, I18n.tr("litematlist.button.remove"));
                removeBtn.setRenderDefaultBackground(true);
                final int syncRemoveIdx = rowIdx;
                this.addButton(removeBtn, (IButtonActionListener) (b, mb) -> {
                    removeEntry(syncRemoveIdx);
                    scrollOffset = Math.min(scrollOffset, Math.max(0, loadedEntries.size() - getVisibleRows()));
                    initGui();
                });
                continue;
            }

            int addToProjW = 80;
            int previewBtnW = 70;
            int rawMaterialsW = 60;
            int viewBtnW = 80;
            int settingsBtnW = 46;
            int removeBtnW = 50;
            int gap = 2;
            int removeX = this.width - 12 - removeBtnW;
            int settingsX = removeX - settingsBtnW - gap;
            int viewX = settingsX - viewBtnW - gap;
            int rawMaterialsX = viewX - rawMaterialsW - gap;
            int previewX = rawMaterialsX - previewBtnW - gap;
            int addToProjX = previewX - addToProjW - gap;

            // 添加至项目按钮（上传后禁用）
            ButtonGeneric addToProjBtn = new ButtonGeneric(addToProjX, rowY + 1, addToProjW, 20, I18n.tr("litematlist.button.add_to_project"));
            addToProjBtn.setRenderDefaultBackground(true);
            if (locked) addToProjBtn.setEnabled(false);
            final int moveIdx = rowIdx;
            this.addButton(addToProjBtn, (IButtonActionListener) (b, mb) -> {
                List<LoadedEntry> projects = getProjectList();
                if (projects.isEmpty()) return;
                MinecraftClient.getInstance().setScreen(new ProjectSelectScreen(this, moveIdx, projects));
            });

            if (!isTxtEntry(entry) && !isJsonEntry(entry) && !isCsvEntry(entry)) {
                ButtonGeneric previewBtn = new ButtonGeneric(previewX, rowY + 1, previewBtnW, 20, I18n.tr("litematlist.button.preview_schematic"));
                previewBtn.setRenderDefaultBackground(true);
                if (!previewAvailable) {
                    previewBtn.setEnabled(false);
                }
                this.addButton(previewBtn, (IButtonActionListener) (b, mb) -> {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    LitematListMod.LOGGER.info("[MaterialListScreen] 点击「预览原理图」: name={}, path={}", entry.name(), entry.filePath());
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    LitematListMod.LOGGER.info("[MaterialListScreen] 点击「预览原理图」 name={}, path={}", entry.name(), entry.filePath());
//#endif
                    SchematicPreviewHelper.openFullscreenPreview(this, entry.filePath());
                });
                previewBtnInfos.add(new PreviewBtnInfo(previewX, rowY + 1, previewBtnW, 20, !previewAvailable));
            } else if (isTxtEntry(entry) || isJsonEntry(entry) || isCsvEntry(entry)) {
                ButtonGeneric renameBtn = new ButtonGeneric(previewX, rowY + 1, previewBtnW, 20, I18n.tr("litematlist.button.rename"));
                renameBtn.setRenderDefaultBackground(true);
                if (locked) renameBtn.setEnabled(false);
                final int renameIdx = rowIdx;
                this.addButton(renameBtn, (IButtonActionListener) (b, mb) -> {
                    MinecraftClient.getInstance().setScreen(new RenameScreen(this, renameIdx, entry.name()));
                });
            }

            ButtonGeneric rawMaterialsBtn = new ButtonGeneric(rawMaterialsX, rowY + 1, rawMaterialsW, 20, I18n.tr("litematlist.button.raw_materials"));
            rawMaterialsBtn.setRenderDefaultBackground(true);
            this.addButton(rawMaterialsBtn, (IButtonActionListener) (b, mb) -> {
                MinecraftClient.getInstance().setScreen(new RawMaterialScreen(this, entry.name(), entry.filePath()));
            });

            ButtonGeneric viewBtn = new ButtonGeneric(viewX, rowY + 1, viewBtnW, 20, I18n.tr("litematlist.button.view_material_list"));
            viewBtn.setRenderDefaultBackground(true);
            this.addButton(viewBtn, (IButtonActionListener) (b, mb) -> {
                MinecraftClient.getInstance().setScreen(new MaterialDetailScreen(this, entry.name(), entry.filePath()));
            });

            ButtonGeneric settingsBtn = new ButtonGeneric(settingsX, rowY + 1, settingsBtnW, 20, I18n.tr("litematlist.button.settings"));
            settingsBtn.setRenderDefaultBackground(true);
            if (locked) settingsBtn.setEnabled(false);
            this.addButton(settingsBtn, (IButtonActionListener) (b, mb) ->
                    MinecraftClient.getInstance().setScreen(new MaterialConfigScreen(this, entry)));

            ButtonGeneric removeBtn = new ButtonGeneric(removeX, rowY + 1, removeBtnW, 20, I18n.tr("litematlist.button.remove"));
            removeBtn.setRenderDefaultBackground(true);
            if (locked) removeBtn.setEnabled(false);
            final int removeIdx = rowIdx;
            this.addButton(removeBtn, (IButtonActionListener) (b, mb) -> {
                removeEntry(removeIdx);
                scrollOffset = Math.min(scrollOffset, Math.max(0, loadedEntries.size() - getVisibleRows()));
                initGui();
            });
        }
    }

    private int getVisibleRows() {
        return (this.height - 40 - 5 - LIST_TOP) / ROW_HEIGHT;
    }

    // ==================== 排序 ====================

    private List<LoadedEntry> getDisplayEntries() {
        // 只显示根级别条目（projectId == null）
        List<LoadedEntry> entries = new ArrayList<>();
        for (LoadedEntry e : loadedEntries) {
            if (e.projectId == null) entries.add(e);
        }

        // 分离项目和非项目
        List<LoadedEntry> projects = new ArrayList<>();
        List<LoadedEntry> materials = new ArrayList<>();
        for (LoadedEntry e : entries) {
            if (e.isProject) projects.add(e);
            else materials.add(e);
        }

        // 组内排序：置顶优先，再按名称
        Comparator<LoadedEntry> nameCmp = (sortMode == SortMode.NAME_ASC)
                ? Comparator.comparing(LoadedEntry::name, String.CASE_INSENSITIVE_ORDER)
                : (sortMode == SortMode.NAME_DESC)
                ? Comparator.comparing(LoadedEntry::name, String.CASE_INSENSITIVE_ORDER).reversed()
                : null;

        if (nameCmp != null) {
            projects.sort(nameCmp);
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 材料列表：置顶优先，然后按名称
//#endif
        if (nameCmp != null) {
            materials.sort((a, b) -> {
                int c = Boolean.compare(!a.pinned, !b.pinned);
                if (c != 0) return c;
                return nameCmp != null ? nameCmp.compare(a, b) : 0;
            });
        } else {
            materials.sort(Comparator.comparing((LoadedEntry e) -> !e.pinned));
        }

        // 合并：项目在前，材料在后
        projects.addAll(materials);
        return projects;
    }

    private void cycleSortMode() {
        switch (sortMode) {
            case NONE -> sortMode = SortMode.NAME_ASC;
            case NAME_ASC -> sortMode = SortMode.NAME_DESC;
            case NAME_DESC -> sortMode = SortMode.NONE;
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        LitematListMod.LOGGER.info("[MaterialListScreen] 排序模式切换为: {}", sortMode);
//#endif

        // 应用排序到 loadedEntries（保留项目内条目）
        List<LoadedEntry> subEntries = new ArrayList<>();
        for (LoadedEntry e : loadedEntries) {
            if (e.projectId != null) subEntries.add(e);
        }
        List<LoadedEntry> sorted = getDisplayEntries();
        loadedEntries.clear();
        loadedEntries.addAll(sorted);
        loadedEntries.addAll(subEntries);
        scrollOffset = 0;
        initGui();
    }

    // ==================== 置顶 ====================

    private void togglePin(int displayIndex) {
        List<LoadedEntry> display = getDisplayEntries();
        if (displayIndex < 0 || displayIndex >= display.size()) return;
        LoadedEntry entry = display.get(displayIndex);
        if (entry.isProject) return;
        if (SyncmaticaBridge.isSyncPath(entry.filePath())) return; // 认领材料条目不参与置顶
        entry.pinned = !entry.pinned;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        LitematListMod.LOGGER.info("[MaterialListScreen] 置顶切换: name={}, pinned={}", entry.name(), entry.pinned);
//#endif
        MaterialDataStore.savePersistence();

        // 重新排序：只重排根级别条目，保留项目内条目
        List<LoadedEntry> projects = new ArrayList<>();
        for (LoadedEntry e : loadedEntries) {
            if (e.projectId != null) projects.add(e);
        }
        List<LoadedEntry> reordered = getDisplayEntries();
        loadedEntries.clear();
        loadedEntries.addAll(reordered);
        loadedEntries.addAll(projects); // 恢复项目内条目
        initGui();
    }

    // ==================== 拖拽重排 ====================

    private int getRowAtY(int mouseY) {
        if (mouseY < LIST_TOP) return -1;
        List<LoadedEntry> display = getDisplayEntries();
        int row = (mouseY - LIST_TOP) / ROW_HEIGHT + scrollOffset;
        if (row >= display.size()) return -1;
        return row;
    }

    private boolean isInPinArea(int mouseX) {
        return mouseX >= PIN_ICON_X && mouseX <= PIN_ICON_X + PIN_ICON_SIZE;
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

        if (button == 0) { // 左键
            // 检测表头点击（仅"名称"文字区域）
            int headerRight = 34 + getHeaderTextWidth() + 10;
            if (mouseY >= HEADER_TOP && mouseY <= HEADER_TOP + HEADER_HEIGHT
                    && mouseX >= 10 && mouseX <= headerRight) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                LitematListMod.LOGGER.info("[MaterialListScreen] 点击表头「原理图名称」");
//#endif
                cycleSortMode();
                return true;
            }

            // 检测置顶区域点击（项目文件夹不响应）
            List<LoadedEntry> display = getDisplayEntries();
            int row = getRowAtY((int)mouseY);
            if (row >= 0 && row < display.size() && isInPinArea((int)mouseX)) {
                LoadedEntry entry = display.get(row);
                if (!entry.isProject) {
                    togglePin(row);
                    return true;
                }
            }

            // 检测列表区域拖拽（非按钮区域，非置顶区域）
            if (row >= 0 && row < display.size() && mouseX >= 10 + PIN_AREA_WIDTH && mouseX < this.width - 210) {
                draggedIndex = row;
                dragStartY = (int)mouseY;
                isDragging = false;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                LitematListMod.LOGGER.info("[MaterialListScreen] 开始拖拽: row={}, name={}", row, display.get(row).name());
//#endif
                return true;
            }
        }

        // 滚动条点击
        List<LoadedEntry> display = getDisplayEntries();
        int listBottom = LIST_TOP + visibleRows * ROW_HEIGHT;
        int scrollbarX = this.width - 6;
        int scrollbarWidth = 3;
        if (display.size() > visibleRows && mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth
                && mouseY >= LIST_TOP && mouseY <= listBottom) {
            int trackHeight = listBottom - LIST_TOP;
            int maxScroll = Math.max(1, display.size() - visibleRows);
            scrollOffset = Math.max(0, Math.min(maxScroll, (int) ((float) (mouseY - LIST_TOP) / trackHeight * maxScroll)));
            scrollbarDragging = true;
            initGui();
            return true;
        }
        return false;
    }

    @Override
//#if MC == 12110 || MC == 12111
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
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
//$$    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        int button = mouseButton;
//#endif

        if (scrollbarDragging && button == 0) {
            List<LoadedEntry> display = getDisplayEntries();
            int listBottom = LIST_TOP + visibleRows * ROW_HEIGHT;
            int trackHeight = listBottom - LIST_TOP;
            int maxScroll = Math.max(1, display.size() - visibleRows);
            scrollOffset = Math.max(0, Math.min(maxScroll, (int) ((float) (mouseY - LIST_TOP) / trackHeight * maxScroll)));
            initGui();
            return true;
        }

        if (draggedIndex >= 0 && button == 0) {
            List<LoadedEntry> display = getDisplayEntries();
//#if MC == 12110 || MC == 12111
            int currentRow = getRowAtY(mouseY);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            int currentRow = getRowAtY((int)mouseY);
//#endif
            if (currentRow >= 0 && currentRow < display.size() && currentRow != draggedIndex && Math.abs(mouseY - dragStartY) > 5) {
                // 防止跨置顶边界拖拽：置顶项不能拖到非置顶项下方，反之亦然
                LoadedEntry dragged = display.get(draggedIndex);
                LoadedEntry target = display.get(currentRow);
                if (dragged.isProject != target.isProject) {
                    return true; // 不允许跨项目/材料边界交换
                }
                if (!dragged.isProject && dragged.pinned != target.pinned) {
                    return true; // 不允许跨置顶边界交换
                }
                isDragging = true;
                Collections.swap(loadedEntries, loadedEntries.indexOf(dragged), loadedEntries.indexOf(target));
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                LitematListMod.LOGGER.info("[MaterialListScreen] 拖拽交换: {} <-> {}", draggedIndex, currentRow);
//#endif
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
        if (scrollbarDragging) {
            scrollbarDragging = false;
            return true;
        }
        if (draggedIndex >= 0) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            LitematListMod.LOGGER.info("[MaterialListScreen] 拖拽结束: draggedIndex={}, isDragging={}", draggedIndex, isDragging);
//#endif
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

    /**
     * 添加条目（手动添加，fromLitematica = false）。
     */
    public static void addEntry(String name, Path filePath) {
        addEntry(name, filePath, false);
    }

    static void addEntry(String name, Path filePath, boolean fromLitematica) {
        boolean exists = loadedEntries.stream().anyMatch(e -> e.filePath != null && e.filePath.equals(filePath));
        if (!exists) {
            loadedEntries.add(new LoadedEntry(name, filePath, fromLitematica));
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            LitematListMod.LOGGER.info("已添加材料列表: {} (fromLitematica={})", name, fromLitematica);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            LitematListMod.LOGGER.info("添加材料列表 {} (fromLitematica={})", name, fromLitematica);
//#endif
            MaterialDataStore.savePersistence();
        }
    }

    private void syncFromLitematica() {
        LitematListMod.LOGGER.info("[MaterialListScreen] 开始从投影同步...");
        if (!LitematicaBridge.isAvailable()) {
            LitematListMod.LOGGER.warn("[MaterialListScreen] Litematica 不可用，跳过同步");
            return;
        }

        List<LitematicaBridge.SchematicInfo> litematicaList = LitematicaBridge.getLoadedSchematics();
        LitematListMod.LOGGER.info("[MaterialListScreen] 从投影获取到 {} 个已加载原理图", litematicaList.size());
        Set<Path> loadedPaths = new HashSet<>();
        for (LitematicaBridge.SchematicInfo info : litematicaList) {
            if (info.filePath() != null) {
                loadedPaths.add(info.filePath());
                addEntry(info.name(), info.filePath(), true);
            }
        }

        loadedEntries.removeIf(entry ->
                entry.fromLitematica() && !loadedPaths.contains(entry.filePath()) && entry.projectId == null && !entry.pinned);
        MaterialDataStore.savePersistence();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        LitematListMod.LOGGER.info("[MaterialListScreen] 同步完成，当前共 {} 个条目", loadedEntries.size());
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        LitematListMod.LOGGER.info("[MaterialListScreen] 同步完成，共 {} 个条目", loadedEntries.size());
//#endif
    }
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    // ==================== 渲染 ====================
//#endif

    /** 当前是否按住 Shift 键（左右 Shift 任一） */
    private static boolean isShiftKeyDown() {
        long window = MinecraftClient.getInstance().getWindow().getHandle();
        return org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS
                || org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
    }

    /** 查找「在Syncmatica认领的材料」条目（不存在则返回 null） */
    private LoadedEntry findSyncmaticaEntry() {
        for (LoadedEntry e : loadedEntries) {
            if (SyncmaticaBridge.isSyncPath(e.filePath())) {
                return e;
            }
        }
        return null;
    }

    /**
     * shift+【从投影同步】：创建（或复用）「在Syncmatica认领的材料」条目并拉取最新认领数据。
     * 列表数据本身不持久化——每次打开该列表界面时都会重新向 Syncmatica 拉取。
     */
    private void syncFromSyncmatica() {
        LitematListMod.LOGGER.info("[MaterialListScreen] 开始从 Syncmatica 同步认领材料...");
        LoadedEntry syncEntry = findSyncmaticaEntry();
        if (syncEntry == null) {
            syncEntry = new LoadedEntry(I18n.tr("litematlist.name.syncmatica_claimed"), SyncmaticaBridge.SYNC_PATH, false);
            loadedEntries.add(syncEntry);
            LitematListMod.LOGGER.info("[MaterialListScreen] 已创建「在Syncmatica认领的材料」条目");
        }
        int count = SyncmaticaBridge.loadClaimedMaterialEntries().size();
        MaterialDataStore.savePersistence();
        setError(I18n.tr("litematlist.msg.syncmatica_synced", count));
        LitematListMod.LOGGER.info("[MaterialListScreen] Syncmatica 认领材料同步完成: {} 种", count);
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ==================== 渲染 ====================
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
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
        drawListBackground(drawContext);
//#elseif MC == 11701 || MC == 11904
//$$        drawListBackground(matrices);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawHeader(drawContext, mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$        drawHeader(matrices, mouseX, mouseY);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawUploadedRedBackgrounds(drawContext); // 红色标识在按钮下方
//#elseif MC == 11701 || MC == 11904
//$$        drawUploadedRedBackgrounds(matrices); // 红色标识在按钮下方
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
        drawHeaderText(drawContext); // 表头文字在最顶层
//#elseif MC == 11701 || MC == 11904
//$$        drawHeaderText(matrices); // 表头文字在最顶层
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawSyncTooltip(drawContext, mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$        drawSyncTooltip(matrices, mouseX, mouseY);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawImportBtnTooltip(drawContext, mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$        drawImportBtnTooltip(matrices, mouseX, mouseY);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawAddBtnTooltip(drawContext, mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$        drawAddBtnTooltip(matrices, mouseX, mouseY);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawPreviewTooltip(drawContext, mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$        drawPreviewTooltip(matrices, mouseX, mouseY);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawPreviewDisabledTooltip(drawContext, mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$        drawPreviewDisabledTooltip(matrices, mouseX, mouseY);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawPinTooltip(drawContext, mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$        drawPinTooltip(matrices, mouseX, mouseY);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawUploadAreaTooltip(drawContext, mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$        drawUploadAreaTooltip(matrices, mouseX, mouseY);
//#endif
    }

    private int getHeaderTextWidth() {
        String fullText = I18n.tr("litematlist.header.name") + switch (sortMode) {
            case NAME_ASC -> "  ▲";
            case NAME_DESC -> "  ▼";
            case NONE -> "  ⇅";
        };
        return this.textRenderer.getWidth(fullText);
    }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void drawHeader(DrawContext drawContext, int mouseX, int mouseY) {
//#elseif MC == 11701 || MC == 11904
//$$    private void drawHeader(MatrixStack matrices, int mouseX, int mouseY) {
//#endif
        // 表头背景仅覆盖"名称"文字区域
        int headerRight = 34 + getHeaderTextWidth() + 10;
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
        // 文字移到 drawHeaderText 中，在 render() 最顶层绘制
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
    private void drawListBackground(DrawContext drawContext) {
//#elseif MC == 11701 || MC == 11904
//$$    private void drawListBackground(MatrixStack matrices) {
//#endif
        int visibleRows = this.getVisibleRows();
        int listBottom = LIST_TOP + visibleRows * ROW_HEIGHT;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(8, LIST_TOP - 2, this.width - 8, listBottom + 2, 0x40000000);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, 8, LIST_TOP - 2, this.width - 8, listBottom + 2, 0x40000000);
//#endif

        List<LoadedEntry> display = getDisplayEntries();
        int maxIdx = Math.min(display.size(), scrollOffset + visibleRows);
        for (int i = scrollOffset; i < maxIdx; i++) {
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;
            LoadedEntry entry = (i < display.size()) ? display.get(i) : null;
            int color;
            if (i == draggedIndex && isDragging) {
                color = 0x60FFFF00;
            } else if (entry != null && isPcpUploaded(entry)) {
                color = 0x300044FF; // 浅蓝色背景（PlayerControl++）
            } else {
                color = ((i - scrollOffset) % 2 == 0) ? 0x20FFFFFF : 0x10FFFFFF;
            }
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(10, rowY, this.width - 10, rowY + ROW_HEIGHT, color);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, 10, rowY, this.width - 10, rowY + ROW_HEIGHT, color);
//#endif
        }
    }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void drawListContent(DrawContext drawContext, int mouseX, int mouseY) {
//#elseif MC == 11701 || MC == 11904
//$$    private void drawListContent(MatrixStack matrices, int mouseX, int mouseY) {
//#endif
        // 右上角作者署名（留出边距，便于以后添加按钮）
        String watermark = "By Bilibili 命运天S";
        int watermarkW = this.textRenderer.getWidth(watermark);
        int watermarkX = this.width - watermarkW - 28;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawText(this.textRenderer, watermark, watermarkX, 12, 0xFF55FFFF, false);
//#elseif MC == 11701 || MC == 11904
//$$        this.textRenderer.draw(matrices, watermark, watermarkX, 12, 0xFF55FFFF);
//#endif
        // 作者署名悬停提示
        if (mouseX >= watermarkX && mouseX <= watermarkX + watermarkW && mouseY >= 12 && mouseY <= 22) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTooltip(this.textRenderer,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    net.minecraft.text.Text.literal(I18n.tr("litematlist.tooltip.author_signature")), mouseX, mouseY);
//#endif
//#if MC == 11701
//$$            this.renderTooltip(matrices, new LiteralText(I18n.tr("litematlist.tooltip.author_signature")), mouseX, mouseY);
//#elseif MC == 11904
//$$            this.renderTooltip(matrices, net.minecraft.text.Text.literal(I18n.tr("litematlist.tooltip.author_signature")), mouseX, mouseY);
//#endif
        }
        List<LoadedEntry> displayForCount = getDisplayEntries();
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawCenteredText(drawContext,
//#elseif MC == 11701 || MC == 11904
//$$        drawCenteredText(matrices,
//#endif
                I18n.tr("litematlist.info.total_lists", displayForCount.size()), this.width / 2, 24, 0xFFFFFFFF);

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 错误提示（红字，3秒后消失）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 错误提示（红字，2秒后消失）
//#endif
        if (errorMessage != null) {
            if (System.currentTimeMillis() - errorMessageTime > 2000) {
                errorMessage = null;
            } else {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawCenteredText(drawContext, errorMessage, this.width / 2, this.height - 56, 0xFFFF5555);
//#elseif MC == 11701 || MC == 11904
//$$                drawCenteredText(matrices, errorMessage, this.width / 2, this.height - 56, 0xFFFF5555);
//#endif
            }
        }

        if (displayForCount.isEmpty()) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawCenteredText(drawContext,
//#elseif MC == 11701 || MC == 11904
//$$            drawCenteredText(matrices,
//#endif
                    I18n.tr("litematlist.info.no_lists"),
                    this.width / 2, this.height / 2 - 10, 0xFFFFFFFF);
            return;
        }

        int visibleRows = this.getVisibleRows();
        List<LoadedEntry> display = getDisplayEntries();
        int maxIdx = Math.min(display.size(), scrollOffset + visibleRows);
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 按钮区域起始位置
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        int buttonAreaStart = this.width - 280;
//#endif
        for (int i = scrollOffset; i < maxIdx; i++) {
            if (i >= display.size()) break;
            LoadedEntry entry = display.get(i);
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;

            // 行悬停高亮（覆盖整行，在按钮下层）
            boolean rowHovered = mouseX >= 10 && mouseX <= this.width - 10
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            if (rowHovered) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.fill(10, rowY, this.width - 10, rowY + ROW_HEIGHT, 0x40FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$                DrawableHelper.fill(matrices, 10, rowY, this.width - 10, rowY + ROW_HEIGHT, 0x40FFFFFF);
//#endif
            }

            if (entry.isProject) {
                // 项目文件夹：文件夹图标
//#if MC == 12108 || MC == 12110 || MC == 12111
                drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, FOLDER_ICON, PIN_ICON_X, rowY + PIN_ICON_Y_OFFSET,
//#elseif MC == 12001
//$$                drawContext.drawTexture(FOLDER_ICON, PIN_ICON_X, rowY + PIN_ICON_Y_OFFSET,
//#elseif MC == 12101
//$$                drawContext.drawTexture( FOLDER_ICON, PIN_ICON_X, rowY + PIN_ICON_Y_OFFSET,
//#elseif MC == 12104
//$$                drawContext.drawTexture(RenderLayer::getGuiTextured, FOLDER_ICON, PIN_ICON_X, rowY + PIN_ICON_Y_OFFSET,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        0.0f, 0.0f, PIN_ICON_SIZE, PIN_ICON_SIZE, PIN_ICON_SIZE, PIN_ICON_SIZE);
//#endif
//#if MC == 11701 || MC == 11904
//$$                RenderSystem.setShaderTexture(0, FOLDER_ICON); DrawableHelper.drawTexture(matrices, PIN_ICON_X, rowY + PIN_ICON_Y_OFFSET, 0.0f, 0.0f, PIN_ICON_SIZE, PIN_ICON_SIZE, PIN_ICON_SIZE, PIN_ICON_SIZE);
//#endif

                // 文件夹名称：最多 30 个英文字符，超出折叠为 "..."，悬停显示完整名称
                String folderName = entry.name();
                if (folderName.length() > 30) {
                    folderName = folderName.substring(0, 30) + "...";
                }
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.drawText(this.textRenderer, folderName, 34, rowY + 5, 0xFFFFFFFF, false);
//#elseif MC == 11701 || MC == 11904
//$$                this.textRenderer.draw(matrices, folderName, 34, rowY + 5, 0xFFFFFFFF);
//#endif
                if (mouseX >= 34 && mouseX <= 34 + this.textRenderer.getWidth(folderName)
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    drawTooltipBox(drawContext, entry.name(), mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$                    drawTooltipBox(matrices, entry.name(), mouseX, mouseY);
//#endif
                }
                continue;
            }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 置顶指示器（Syncmatica 认领材料条目显示专属 logo 图标，不参与置顶）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 置顶指示器
//#endif
            int pinX = PIN_ICON_X;
            int pinY = rowY + PIN_ICON_Y_OFFSET;
            boolean pinHovered = mouseX >= pinX && mouseX <= pinX + PIN_ICON_SIZE
                    && mouseY >= pinY && mouseY <= pinY + PIN_ICON_SIZE;

            if (SyncmaticaBridge.isSyncPath(entry.filePath())) {
//#if MC == 12108 || MC == 12110 || MC == 12111
                drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, SYNC_LOGO_TEXTURE, pinX, pinY,
//#elseif MC == 12101
//$$                drawContext.drawTexture( SYNC_LOGO_TEXTURE, pinX, pinY,
//#elseif MC == 12104
//$$                drawContext.drawTexture(RenderLayer::getGuiTextured, SYNC_LOGO_TEXTURE, pinX, pinY,
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        0.0f, 0.0f, PIN_ICON_SIZE, PIN_ICON_SIZE, PIN_ICON_SIZE, PIN_ICON_SIZE);
//#endif
//#if MC == 11701 || MC == 11904
//$$                RenderSystem.setShaderTexture(0, SYNC_LOGO_TEXTURE); DrawableHelper.drawTexture(matrices, pinX, pinY, 0.0f, 0.0f, PIN_ICON_SIZE, PIN_ICON_SIZE, PIN_ICON_SIZE, PIN_ICON_SIZE);
//#elseif MC == 12001
//$$                drawContext.drawTexture(SYNC_LOGO_TEXTURE, pinX, pinY, 0.0f, 0.0f, PIN_ICON_SIZE, PIN_ICON_SIZE, PIN_ICON_SIZE, PIN_ICON_SIZE);
//#endif
            } else {
                Identifier pinTexture;
                if (entry.pinned) {
                    pinTexture = pinHovered ? PIN_HOVERED_FAVORITE : PIN_FAVORITE;
                } else {
                    pinTexture = pinHovered ? PIN_HOVERED_EMPTY : PIN_EMPTY;
                }
//#if MC == 12108 || MC == 12110 || MC == 12111
                drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, pinTexture, pinX, pinY, 0.0f, 0.0f, PIN_ICON_SIZE, PIN_ICON_SIZE, PIN_ICON_SIZE, PIN_ICON_SIZE);
//#elseif MC == 11701 || MC == 11904
//$$                RenderSystem.setShaderTexture(0, pinTexture); DrawableHelper.drawTexture(matrices, pinX, pinY, 0.0f, 0.0f, PIN_ICON_SIZE, PIN_ICON_SIZE, PIN_ICON_SIZE, PIN_ICON_SIZE);
//#elseif MC == 12001
//$$                drawContext.drawTexture(pinTexture, pinX, pinY, 0.0f, 0.0f, PIN_ICON_SIZE, PIN_ICON_SIZE, PIN_ICON_SIZE, PIN_ICON_SIZE);
//#elseif MC == 12101
//$$                drawContext.drawTexture( pinTexture, pinX, pinY, 0.0f, 0.0f, PIN_ICON_SIZE, PIN_ICON_SIZE, PIN_ICON_SIZE, PIN_ICON_SIZE);
//#elseif MC == 12104
//$$                drawContext.drawTexture(RenderLayer::getGuiTextured, pinTexture, pinX, pinY, 0.0f, 0.0f, PIN_ICON_SIZE, PIN_ICON_SIZE, PIN_ICON_SIZE, PIN_ICON_SIZE);
//#endif
            }

            // 原理图名称：名称最多 30 个英文字符，超出折叠为 "..."，类型说明紧随其后
            String displayName = entry.name();
            String typeSuffix = "";
            if (isTxtEntry(entry)) {
                typeSuffix = "（txt）";
            } else if (isJsonEntry(entry)) {
                typeSuffix = "（JSON）";
            } else if (isCsvEntry(entry)) {
                typeSuffix = "（CSV）";
            }
            boolean truncated = false;
            if (displayName.length() > 30) {
                displayName = displayName.substring(0, 30) + "...";
                truncated = true;
            }
            displayName = displayName + typeSuffix;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawText(this.textRenderer, displayName, 34, rowY + 5, 0xFFFFFFFF, false);
//#elseif MC == 11701 || MC == 11904
//$$            this.textRenderer.draw(matrices, displayName, 34, rowY + 5, 0xFFFFFFFF);
//#endif
            // 被折叠时悬停显示完整名称
            if (truncated && mouseX >= 34 && mouseX <= 34 + this.textRenderer.getWidth(displayName)
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawTooltipBox(drawContext, entry.name(), mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$                drawTooltipBox(matrices, entry.name(), mouseX, mouseY);
//#endif
            }
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

            // 滚动条
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            int scrollbarX = this.width - 6;
//#endif
            int scrollbarWidth = 3;
            int listHeight = listBottom - LIST_TOP;
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            int scrollbarX = this.width - 6;
//#endif
            // 轨道
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(scrollbarX, LIST_TOP, scrollbarX + scrollbarWidth, listBottom, 0x30FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, scrollbarX, LIST_TOP, scrollbarX + scrollbarWidth, listBottom, 0x30FFFFFF);
//#endif
            // 滑块
            float ratio = (float) visibleRows / display.size();
            int thumbHeight = Math.max(6, (int)(listHeight * ratio));
            int maxScroll = Math.max(1, display.size() - visibleRows);
            int thumbY = LIST_TOP + (int)((listHeight - thumbHeight) * (float) scrollOffset / maxScroll);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, 0x80FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, 0x80FFFFFF);
//#endif
        }
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 绘制"从投影同步"按钮的悬停提示（未安装 Syncmatica 时不显示第二行） */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 绘制"从投影同步"按钮的悬停提示 */
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void drawSyncTooltip(DrawContext drawContext, int mouseX, int mouseY) {
//#elseif MC == 11701 || MC == 11904
//$$    private void drawSyncTooltip(MatrixStack matrices, int mouseX, int mouseY) {
//#endif
        if (mouseX >= syncBtnX && mouseX <= syncBtnX + syncBtnW &&
                mouseY >= syncBtnY && mouseY <= syncBtnY + syncBtnH) {
            String tip = I18n.tr("litematlist.tooltip.sync_from_projection");
            if (SyncmaticaBridge.isModLoaded()) {
                tip = tip + "\n" + I18n.tr("litematlist.tooltip.sync_shift_syncmatica");
            }
            String[] lines = tip.split("\n");
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            java.util.List<net.minecraft.text.Text> tooltipLines = new java.util.ArrayList<>();
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            java.util.List<net.minecraft.text.OrderedText> tooltipLines = new java.util.ArrayList<>();
//#endif
            for (String line : lines) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                tooltipLines.add(net.minecraft.text.Text.literal(line));
//#elseif MC == 11701
//$$                tooltipLines.add(new LiteralText(line).asOrderedText());
//#elseif MC == 11904 || MC == 12001
//$$                tooltipLines.add(net.minecraft.text.Text.literal(line).asOrderedText());
//#endif
            }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTooltip(this.textRenderer, tooltipLines, mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$            this.renderOrderedTooltip(matrices, tooltipLines, mouseX, mouseY);
//#elseif MC == 12001
//$$            drawContext.drawOrderedTooltip(this.textRenderer, tooltipLines, mouseX, mouseY);
//#endif
        }
    }

    /** 绘制"导入列表文件"按钮的悬停提示 */
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void drawImportBtnTooltip(DrawContext drawContext, int mouseX, int mouseY) {
//#elseif MC == 11701 || MC == 11904
//$$    private void drawImportBtnTooltip(MatrixStack matrices, int mouseX, int mouseY) {
//#endif
        if (mouseX >= importBtnX && mouseX <= importBtnX + importBtnW &&
                mouseY >= importBtnY && mouseY <= importBtnY + importBtnH) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawTooltipBox(drawContext, I18n.tr("litematlist.tooltip.import_list_file"), mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$            drawTooltipBox(matrices, I18n.tr("litematlist.tooltip.import_list_file"), mouseX, mouseY);
//#endif
        }
    }

    /** 绘制"添加材料列表"按钮的悬停提示 */
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void drawAddBtnTooltip(DrawContext drawContext, int mouseX, int mouseY) {
//#elseif MC == 11701 || MC == 11904
//$$    private void drawAddBtnTooltip(MatrixStack matrices, int mouseX, int mouseY) {
//#endif
        if (mouseX >= addBtnX && mouseX <= addBtnX + addBtnW &&
                mouseY >= addBtnY && mouseY <= addBtnY + addBtnH) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawTooltipBox(drawContext, I18n.tr("litematlist.tooltip.add_material_list"), mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$            drawTooltipBox(matrices, I18n.tr("litematlist.tooltip.add_material_list"), mouseX, mouseY);
//#endif
        }
    }

    /** 绘制"预览原理图"按钮的悬停提示 */
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void drawPreviewTooltip(DrawContext drawContext, int mouseX, int mouseY) {
//#elseif MC == 11701 || MC == 11904
//$$    private void drawPreviewTooltip(MatrixStack matrices, int mouseX, int mouseY) {
//#endif
        for (PreviewBtnInfo info : previewBtnInfos) {
            if (mouseX >= info.x && mouseX <= info.x + info.w &&
                    mouseY >= info.y && mouseY <= info.y + info.h) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawTooltipBox(drawContext, I18n.tr("litematlist.tooltip.fullscreen_preview") + " (ESC)", mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$                drawTooltipBox(matrices, I18n.tr("litematlist.tooltip.fullscreen_preview") + " (ESC)", mouseX, mouseY);
//#endif
                return;
            }
        }
    }

    /** 绘制被禁用的「预览原理图」按钮的悬停提示（未安装 SchematicPreview 时） */
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void drawPreviewDisabledTooltip(DrawContext drawContext, int mouseX, int mouseY) {
//#elseif MC == 11701 || MC == 11904
//$$    private void drawPreviewDisabledTooltip(MatrixStack matrices, int mouseX, int mouseY) {
//#endif
        for (PreviewBtnInfo info : previewBtnInfos) {
            if (info.disabled && mouseX >= info.x && mouseX <= info.x + info.w &&
                    mouseY >= info.y && mouseY <= info.y + info.h) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawTooltipBox(drawContext, I18n.tr("litematlist.tooltip.no_schematic_preview"), mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$                drawTooltipBox(matrices, I18n.tr("litematlist.tooltip.no_schematic_preview"), mouseX, mouseY);
//#endif
                return;
            }
        }
    }

    /** 绘制置顶图标的悬停提示 */
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void drawPinTooltip(DrawContext drawContext, int mouseX, int mouseY) {
//#elseif MC == 11701 || MC == 11904
//$$    private void drawPinTooltip(MatrixStack matrices, int mouseX, int mouseY) {
//#endif
        List<LoadedEntry> display = getDisplayEntries();
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12110 || MC == 12111
        int row = getRowAtY(mouseY);
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        int row = getRowAtY((int)mouseY);
//#endif
        if (row >= 0 && row < display.size() && isInPinArea(mouseX)) {
            LoadedEntry entry = display.get(row);
            if (entry.isProject) return;
            if (SyncmaticaBridge.isSyncPath(entry.filePath())) return; // 认领材料条目不参与置顶
            String tip = entry.pinned ? I18n.tr("litematlist.tooltip.unpin") : I18n.tr("litematlist.tooltip.pin");
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawTooltipBox(drawContext, tip, mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$            drawTooltipBox(matrices, tip, mouseX, mouseY);
//#endif
        }
    }

    /** 绘制已上传条目的红色背景（整行，在按钮层下方） */
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void drawUploadedRedBackgrounds(DrawContext drawContext) {
//#elseif MC == 11701 || MC == 11904
//$$    private void drawUploadedRedBackgrounds(MatrixStack matrices) {
//#endif
        int visibleRows = this.getVisibleRows();
        List<LoadedEntry> display = getDisplayEntries();
        int maxIdx = Math.min(display.size(), scrollOffset + visibleRows);
        for (int i = scrollOffset; i < maxIdx; i++) {
            if (i >= display.size()) break;
            LoadedEntry entry = display.get(i);
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;
            if (entry.uploaded) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.fill(10, rowY, this.width - 10, rowY + ROW_HEIGHT, 0xFFAB0508);
//#elseif MC == 11701 || MC == 11904
//$$                DrawableHelper.fill(matrices, 10, rowY, this.width - 10, rowY + ROW_HEIGHT, 0xFFAB0508);
//#endif
            }
        }
    }

    /** 绘制「上传区域」按钮的悬停提示 */
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void drawUploadAreaTooltip(DrawContext drawContext, int mouseX, int mouseY) {
//#elseif MC == 11701 || MC == 11904
//$$    private void drawUploadAreaTooltip(MatrixStack matrices, int mouseX, int mouseY) {
//#endif
        if (mouseX >= uploadAreaBtnX && mouseX <= uploadAreaBtnX + uploadAreaBtnW
                && mouseY >= uploadAreaBtnY && mouseY <= uploadAreaBtnY + uploadAreaBtnH) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawTooltipBox(drawContext, I18n.tr("litematlist.tooltip.upload_area"), mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$            drawTooltipBox(matrices, I18n.tr("litematlist.tooltip.upload_area"), mouseX, mouseY);
//#endif
        }
    }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void drawTooltipBox(DrawContext drawContext, String tip, int mouseX, int mouseY) {
//#elseif MC == 11701 || MC == 11904
//$$    private void drawTooltipBox(MatrixStack matrices, String tip, int mouseX, int mouseY) {
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawTooltip(this.textRenderer, net.minecraft.text.Text.literal(tip), mouseX, mouseY);
//#elseif MC == 11701
//$$        this.renderTooltip(matrices, new LiteralText(tip), mouseX, mouseY);
//#elseif MC == 11904
//$$        this.renderTooltip(matrices, net.minecraft.text.Text.literal(tip), mouseX, mouseY);
//#endif
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

    @Override
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
//#endif
        int visibleRows = this.getVisibleRows();
        List<LoadedEntry> display = getDisplayEntries();
        if (mouseY >= LIST_TOP && mouseY <= LIST_TOP + visibleRows * ROW_HEIGHT) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            scrollOffset = Math.max(0, scrollOffset - (int) verticalAmount);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            scrollOffset = Math.max(0, scrollOffset - (int) amount);
//#endif
            int maxOffset = Math.max(0, display.size() - visibleRows);
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

    public static class LoadedEntry {
        private final String name;
        private final Path filePath;
        private final boolean fromLitematica;
        boolean pinned;
        String projectId; // 所属项目ID（null=根列表）
        boolean isProject; // 是否为项目文件夹
        String hotkey; // 快捷打开此材料列表的快捷键（存储 GLFW 键码字符串，""=未设置）
        boolean uploaded; // 是否已上传到「上传区域」（不持久化，切换世界后重置）
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
        public LoadedEntry(String name, Path filePath, boolean fromLitematica) {
            this.name = name;
            this.filePath = filePath;
            this.fromLitematica = fromLitematica;
            this.pinned = false;
            this.projectId = null;
            this.isProject = false;
            this.hotkey = "";
            this.uploaded = false;
        }

        public String name() { return name; }
        public Path filePath() { return filePath; }
        public boolean fromLitematica() { return fromLitematica; }
        public String hotkey() { return hotkey; }
        public boolean uploaded() { return uploaded; }
        public boolean isProject() { return isProject; }
    }

    public static boolean isTxtEntry(LoadedEntry entry) {
        return entry.filePath != null && entry.filePath.getFileName().toString().toLowerCase().endsWith(".txt");
    }

    public static boolean isJsonEntry(LoadedEntry entry) {
        return entry.filePath != null && entry.filePath.getFileName().toString().toLowerCase().endsWith(".json");
    }

    public static boolean isCsvEntry(LoadedEntry entry) {
        return entry.filePath != null && entry.filePath.getFileName().toString().toLowerCase().endsWith(".csv");
    }

    /** 检查条目是否已上传至 PlayerControl++ */
    private static boolean isPcpUploaded(LoadedEntry entry) {
        if (MaterialDataStore.playerControlPlusUploaded == null) return false;
        String p = entry.filePath() != null ? entry.filePath().toString() : entry.name();
        return p.equals(MaterialDataStore.playerControlPlusUploaded);
    }

    /** 获取已加载条目列表（供持久化使用） */
    public static List<LoadedEntry> getLoadedEntries() {
        return new ArrayList<>(loadedEntries);
    }

    /** 清空所有已加载条目（世界切换时使用） */
    public static void clearLoadedEntries() {
        loadedEntries.clear();
    }

    /** 内部添加条目（供持久化加载使用），返回创建的条目或已存在的条目 */
    public static LoadedEntry addEntryInternal(String name, Path filePath, boolean fromLitematica, boolean pinned) {
        for (LoadedEntry e : loadedEntries) {
            if (e.filePath != null && e.filePath.equals(filePath)) return e;
        }
        LoadedEntry entry = new LoadedEntry(name, filePath, fromLitematica);
        entry.pinned = pinned;
        loadedEntries.add(entry);
        return entry;
    }

    /** 移除条目并清理其持久化数据（替换、忽略） */
    private void removeEntry(int index) {
        if (index < 0 || index >= loadedEntries.size()) return;
        LoadedEntry entry = loadedEntries.remove(index);
        if (entry.filePath() != null) {
            MaterialDataStore.cleanupPathData(entry.filePath());
        }
        MaterialDataStore.savePersistence();
    }

    /** 重命名条目 */
    public static void renameEntry(int index, String newName) {
        if (index < 0 || index >= loadedEntries.size()) return;
        LoadedEntry old = loadedEntries.get(index);
        String oldName = old.name();
        LoadedEntry renamed = new LoadedEntry(newName, old.filePath(), old.fromLitematica());
        renamed.pinned = old.pinned;
        renamed.projectId = old.projectId;
        renamed.isProject = old.isProject;
        loadedEntries.set(index, renamed);
        // 如果是项目文件夹，同步更新所有子条目的 projectId
        if (old.isProject) {
            for (LoadedEntry e : loadedEntries) {
                if (oldName.equals(e.projectId)) {
                    e.projectId = newName;
                }
            }
        }
        MaterialDataStore.savePersistence();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        LitematListMod.LOGGER.info("[MaterialListScreen] 重命名: '{}' -> '{}'", oldName, newName);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        LitematListMod.LOGGER.info("[MaterialListScreen] 重命名 '{}' -> '{}'", oldName, newName);
//#endif
    }

    // ==================== 项目操作 ====================

    /** 创建项目文件夹 */
    public static void createProject(String name) {
        LoadedEntry project = new LoadedEntry(name, null, false);
        project.isProject = true;
        project.pinned = true; // 始终置顶
        project.projectId = null;
        loadedEntries.add(project);
        MaterialDataStore.savePersistence();
        LitematListMod.LOGGER.info("[MaterialListScreen] 创建项目: {}", name);
    }

    /** 获取所有项目列表 */
    public static List<LoadedEntry> getProjectList() {
        List<LoadedEntry> projects = new ArrayList<>();
        for (LoadedEntry e : loadedEntries) {
            if (e.isProject && e.projectId == null) projects.add(e);
        }
        return projects;
    }

    /** 获取项目内的所有条目 */
    public static List<LoadedEntry> getProjectEntries(String projectId) {
        List<LoadedEntry> entries = new ArrayList<>();
        for (LoadedEntry e : loadedEntries) {
            if (projectId.equals(e.projectId) && !e.isProject) entries.add(e);
        }
        return entries;
    }

    // ==================== 上传区域 ====================

    /** 获取当前已上传到「上传区域」的条目（无则返回 null） */
    public static LoadedEntry getUploadedEntry() {
        for (LoadedEntry e : loadedEntries) {
            if (e.uploaded) return e;
        }
        return null;
    }

    /** 将条目上传到「上传区域」（同时只能有一个），并注入到 Litematica DataManager */
    public static void uploadEntry(LoadedEntry entry) {
        for (LoadedEntry e : loadedEntries) {
            e.uploaded = (e == entry);
        }
        // 立即注入材料列表到 Litematica 的 DataManager
        if (entry.isProject) {
            com.litematlist.MaterialListInjector.injectFromProject(entry);
        } else {
            com.litematlist.MaterialListInjector.injectFromEntry(entry);
        }
        // 同步注入状态，防止 injectIfNeeded 立即重复注入
        com.litematlist.MaterialListInjector.markInjected(entry);
        // 刷新 HUD 显示
        com.litematlist.gui.MaterialListHudRenderer.refreshHud();
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 撤出「上传区域」中的条目 */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 撤销「上传区域」中的条目 */
//#endif
    public static void clearUploadedEntry() {
        for (LoadedEntry e : loadedEntries) {
            e.uploaded = false;
        }
        com.litematlist.MaterialListInjector.clearInjection();
        // 立刻通知 HUD：撤出后 HUD 同步为空表。
        // 必须用 forceRefreshHud（先清零 lastRefreshTime 再刷新），否则会被普通刷新的 200ms 冷却吞掉；
        // 主循环只在 hudItems 为空时才自动重建，若不在此处主动刷新，已撤出的旧材料会一直留在 HUD 上。
        com.litematlist.gui.MaterialListHudRenderer.forceRefreshHud();
    }

    /** 将条目移动到项目中 */
    public static void moveToProject(int index, String projectId) {
        if (index < 0 || index >= loadedEntries.size()) return;
        LoadedEntry entry = loadedEntries.get(index);
        entry.projectId = projectId;
        MaterialDataStore.savePersistence();
        LitematListMod.LOGGER.info("[MaterialListScreen] 移动 '{}' 到项目 '{}'", entry.name(), projectId);
    }

    /** 将条目从项目中移出 */
    public static void moveOutOfProject(int index) {
        if (index < 0 || index >= loadedEntries.size()) return;
        LoadedEntry entry = loadedEntries.get(index);
        entry.projectId = null;
        MaterialDataStore.savePersistence();
        LitematListMod.LOGGER.info("[MaterialListScreen] 移出 '{}' 从项目", entry.name());
    }

    /** 删除项目及其所有子项 */
    private void deleteProject(int index) {
        if (index < 0 || index >= loadedEntries.size()) return;
        LoadedEntry project = loadedEntries.get(index);
        if (!project.isProject) return;

        // 删除所有子项
        loadedEntries.removeIf(e -> project.name().equals(e.projectId));
        // 删除项目本身（重新查找位置，因为子条目移除后索引可能已变化）
        for (int i = 0; i < loadedEntries.size(); i++) {
            if (loadedEntries.get(i).isProject && loadedEntries.get(i).name().equals(project.name())) {
                loadedEntries.remove(i);
                break;
            }
        }
        MaterialDataStore.savePersistence();
        LitematListMod.LOGGER.info("[MaterialListScreen] 删除项目: {}", project.name());
    }
}

//#if MC == 12101 || MC == 12104 || MC == 12108
//$$
//$$
//$$
//$$
//#endif