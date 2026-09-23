package com.litematlist.gui;

import com.google.gson.*;
import com.litematlist.I18n;
import com.litematlist.LitematListMod;
import com.litematlist.RecipeTreeNode;
import com.litematlist.RawMaterialTreeAnalyzer;
import com.litematlist.LitematicReader;
import com.litematlist.LitematicReader.MaterialEntry;
import com.litematlist.SyncmaticaBridge;
import com.litematlist.gui.MaterialDetailScreen.RawMaterialEntry;
import com.litematlist.MaterialListInjector;
import com.litematlist.MaterialListInjector;
import com.litematlist.gui.MaterialListScreen;
import com.litematlist.PinyinSearch;
import com.litematlist.config.Configs;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import fi.dy.masa.malilib.render.GuiContext;
import org.apache.commons.lang3.tuple.Pair;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.GpuSampler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 原材料横向树状图界面：以配方树形式展示原材料溯源结果。
 * 支持配方层数选择、节点折叠/展开、JSON导出、冗余列表。
 */
public class RawMaterialScreen extends GuiBase {

    private final GuiBase parent;
    private final String schematicName;
    private final Path filePath;
    private String pathKey;

    // ---- 树数据 ----
    private List<RecipeTreeNode> treeRoots = new ArrayList<>();
    private boolean initialLoad = true;
    private boolean analyzing = false;

    // ---- 冗余数据 ----
    private List<RawMaterialEntry> redundancyItems = new ArrayList<>();

    // ---- 显示层数 ----
    private int displayDepth = 5;
    private static final int MIN_DEPTH = 0;
    private static final int MAX_DEPTH = 10;

    // ---- 层数输入框 ----
    private net.minecraft.client.gui.components.EditBox depthField;
    private boolean depthFieldFocused = false;

    // ---- 树状图搜索 ----
    private static final Identifier SEARCH_ICON = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/search.png");
    private static final int SEARCH_ICON_SIZE = 21; // 图标纹理实际为 21x21，按 1:1 显示
    private static final int SEARCH_ICON_DEFAULT_Y = 58;
    private static final int SEARCH_FIELD_W = 75;
    private static final int SEARCH_FIELD_H = 16;
    private static final int SEARCH_NAV_BTN_SIZE = 16;
    private static final int DRAG_THRESHOLD = 16; // 位移平方距离超过阈值判定为拖动

    // ---- 树状图整体缩放（开关在【配置】-【通用】；【-】百分比【+】显示在右下角） ----
    private static final Identifier ZOOM_IN_ICON = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/zoom/zoom_in.png");
    private static final Identifier ZOOM_OUT_ICON = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/zoom/zoom_out.png");
    private static final int ZOOM_BTN_SIZE = 16;
    private static final int ZOOM_MIN_PERCENT = 10;
    private static final int ZOOM_MAX_PERCENT = 200;
    private static final int ZOOM_STEP_PERCENT = 10;
    private static final int ZOOM_DEFAULT_PERCENT = 100;
    private boolean treeZoomEnabled = false;
    // 静态保存缩放百分比：跨界面往返、替换物品、退出界面后均保持（并根据世界持久化到 persistence.json）
    private static int treeZoomPercent = ZOOM_DEFAULT_PERCENT;
    private int zoomOutBtnX = 0;
    private int zoomInBtnX = 0;
    private int zoomBarY = 0;
    private boolean searchVisible = false;
    private net.minecraft.client.gui.components.EditBox searchField;
    private final List<NodeLayout> searchMatches = new ArrayList<>();
    private int searchIndex = -1;
    private String lastSearchKeyword = "";
    private NodeLayout currentSearchMatch = null;
    // 搜索控件的动态位置（放大镜图标为锚点，长按可拖动）；searchIconX = -1 表示尚未定位
    private int searchIconX = -1;
    private int searchIconY = SEARCH_ICON_DEFAULT_Y;
    // 拖动状态：按下放大镜后移动超过阈值才进入拖动，否则松开视为点击
    private boolean searchDragPending = false;
    private boolean searchDragging = false;
    private double searchDragMouseDownX = 0;
    private double searchDragMouseDownY = 0;
    private int searchDragStartIconX = 0;
    private int searchDragStartIconY = 0;
    // 每帧计算出的布局：输入框位置、信息行位置
    private int searchFieldX = 0;
    private int searchFieldY = 0;
    private int searchNavX = 0;
    private int searchNavY = 0;
    private int searchUpBtnX = 0;
    private int searchDownBtnX = 0;
    private int searchBtnY = 0; // ↑↓按钮实际Y（右侧放不下时下移到信息行下方）

    // ---- 布局 ----
    private static final int NODE_WIDTH = 112;
    private static final int NODE_HEIGHT = 20;
    private static final int COLUMN_SPACING = 60;
    private static final int VERTICAL_SPACING = 6;
    private static final int TREE_LEFT_MARGIN = 20;
    private static final int CONTROLS_BOTTOM = 62;
    private static final int TREE_BOTTOM_OFFSET = 44;
    private static final int MAX_NAME_CHARS = 6;

    // ---- 滚动 ----
    private double scrollX = 0;
    private double scrollY = 0;
    private double maxScrollX = 0;
    // 树状图水平滚动下限：缩放状态开启且树整体小于视口时为负数（允许整体平移、两侧不出屏），其余状态为 0
    private double minScrollX = 0;
    private double maxScrollY = 0;
    private boolean isDraggingTree = false;
    private double dragStartX = 0;
    private double dragStartY = 0;
    private double dragScrollStartX = 0;
    private double dragScrollStartY = 0;

    // ---- 节点布局缓存 ----
    private static class NodeLayout {
        RecipeTreeNode node;
        int x, y;
        int subtreeHeight;
        boolean collapsed = false;
        List<NodeLayout> children = new ArrayList<>();
        String parentItemId = null;
        String grandparentItemId = null;

        String getGrandparentItemId() {
            return grandparentItemId;
        }
    }
    private List<NodeLayout> rootLayouts = new ArrayList<>();
    private int totalTreeWidth = 0;
    private int totalTreeHeight = 0;

    // ---- 悬停 ----
    private NodeLayout hoveredNode = null;

    // ---- 折叠/展开图标 ----
    private static final Identifier EXPAND_NORMAL = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/expand/expand_normal.png");
    private static final Identifier EXPAND_HOVERED = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/expand/expand_hovered.png");
    private static final Identifier COLLAPSE_NORMAL = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/expand/collapse_normal.png");
    private static final Identifier COLLAPSE_HOVERED = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/expand/collapse_hovered.png");
    private static final int EXPAND_ICON_SIZE = 12;

    // ---- 替换图标（通配物品） ----
    private static final Identifier GEAR_ICON = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/gear.png");
    private static final int GEAR_ICON_SIZE = 12;

    // ---- 配方图标（连接线上） ----
    private static final int RECIPE_ICON_SIZE = 14;
    private NodeLayout hoveredRecipeNode = null; // 鼠标悬停配方图标的节点

    // ---- 层数加减图标 ----
    private static final Identifier DEPTH_NORMAL = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/depth/depth_normal.png");
    private static final Identifier DEPTH_HOVERED = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/depth/depth_hovered.png");
    private static final Identifier DEPTH_LOCKED = Identifier.fromNamespaceAndPath("litematlist", "textures/gui/depth/depth_locked.png");
    private static final int DEPTH_ICON_SIZE = 20;
    private boolean depthIconHovered = false;

    // ---- 折叠状态持久化 ----
    private static final Map<String, Set<String>> COLLAPSED_PATHS = new HashMap<>();

    // ---- 创造模式排序 ----
    private static boolean creativeSortEnabled = false;

    // ---- 同步替换 ----
    private static boolean syncReplacementEnabled = true;

    // ---- 子界面标记（用于修复替换后滚动位置） ----
    private boolean goingToSubScreen = false;

    // ---- 按钮位置（用于渲染彩色开关文字） ----
    private int creativeSortBtnX = 0;
    private int creativeSortBtnW = 0;
    private int syncReplaceBtnX = 0;
    private int syncReplaceBtnW = 0;
    private int exportRawBtnX = 0;
    private int exportRawBtnW = 0;

    // ---- 延迟更新 ----
    private boolean hasPendingChanges = false;

    // ---- 滚动条 ----
    private static final int SCROLLBAR_THICKNESS = 6;
    private static final int SCROLLBAR_MIN_THUMB = 16;
    private boolean draggingVScrollbar = false;
    private boolean draggingHScrollbar = false;
    private double vScrollbarDragStartY = 0;
    private double hScrollbarDragStartX = 0;
    private double vScrollStartValue = 0;
    private double hScrollStartValue = 0;

    public RawMaterialScreen(GuiBase parent, String schematicName, Path filePath) {
        super();
        this.parent = parent;
        this.schematicName = schematicName;
        this.filePath = filePath;
        this.pathKey = filePath != null ? filePath.toString() : schematicName;
        this.title = I18n.tr("litematlist.title.raw_materials", schematicName);
    }

    /**
     * 判断是否来自项目文件夹。
     * 项目文件路径在 .minecraft/litematlist/project/<project>/...
     */
    private boolean isFromProjectFolder() {
        if (this.filePath == null) return false;
        // filePath 是 .minecraft/litematlist/project/<project>/<file>.litematic
        Path parent = this.filePath.getParent();
        if (parent == null) return false;
        Path grandParent = parent.getParent();
        if (grandParent == null) return false;
        return "project".equals(grandParent.getFileName().toString());
    }

    /** 获取项目文件夹名称（父文件夹名称） */
    private String getProjectFolderName() {
        if (this.filePath == null || this.filePath.getParent() == null) return "";
        return this.filePath.getParent().getFileName().toString();
    }

    /** 获取正确的导出路径（原材料导出） */
    private Path getRawMaterialExportPath(String filename) {
        Path mcDir = Minecraft.getInstance().gameDirectory.toPath().resolve("litematlist");
        if (isFromProjectFolder()) {
            String projectName = getProjectFolderName();
            return mcDir.resolve(projectName).resolve("rawmaterials").resolve(filename);
        } else {
            return mcDir.resolve("rawmaterials").resolve(filename);
        }
    }

    /** 获取表式JSON正确的导出路径（统一放 treetable 文件夹） */
    private Path getTreeTableExportPath(String filename) {
        Path mcDir = Minecraft.getInstance().gameDirectory.toPath().resolve("litematlist");
        return mcDir.resolve("treetable").resolve(filename);
    }

    @Override
    public void initGui() {
        super.initGui();

        // 树状图缩放开关（【配置】-【通用】）
        this.treeZoomEnabled = Configs.Generic.TREE_ZOOM_CONTROL.getBooleanValue();

        // ---- 层数控制 ----
        // 新布局: 标签 → 输入框 → [+-]图标
        int fieldX = 60;
        int depthCtrlY = 34;

        // 深度输入框（在图标前面）
        if (depthField == null) {
            this.depthField = new net.minecraft.client.gui.components.EditBox(
                    this.font, fieldX + 3, depthCtrlY + 1, 24, 14, Component.empty());
            this.depthField.setMaxLength(2);
            this.depthField.setValue(String.valueOf(displayDepth));
            // this.depthField.setDrawsBackground(false);
            this.addWidget(this.depthField);
        } else {
            this.depthField.setX(fieldX + 3);
            this.depthField.setY(depthCtrlY + 1);
            this.depthField.setValue(String.valueOf(displayDepth));
            this.addWidget(this.depthField);
        }

        // ---- 树状图搜索输入框（点击搜索图标后显示） ----
        if (searchField == null) {
            this.searchField = new net.minecraft.client.gui.components.EditBox(
                    this.font, 0, 0, SEARCH_FIELD_W, SEARCH_FIELD_H, Component.empty());
            this.searchField.setMaxLength(32);
            this.searchField.setValue("");
            this.addWidget(this.searchField);
        } else {
            this.addWidget(this.searchField);
        }
        computeSearchLayout();
        this.searchField.setX(searchFieldX);
        this.searchField.setY(searchFieldY);
        if (!this.searchVisible) {
            this.searchField.setFocused(false);
        }

        int depthIconX = fieldX + 30; // 图标在输入框后面

        // ---- 创造模式排序开关（放在【+-】和【导出】之间） ----
        String creativeSortLabel = creativeSortEnabled
                ? I18n.tr("litematlist.button.creative_sort_on")
                : I18n.tr("litematlist.button.creative_sort_off");
        creativeSortBtnW = this.font.width(creativeSortLabel) + 10;
        creativeSortBtnX = depthIconX + DEPTH_ICON_SIZE + 4;
        // 按钮白字不渲染"开/关"，彩色文字单独覆盖
        String creativeSortBtnLabel = creativeSortLabel.substring(0, creativeSortLabel.length() - 1);
        ButtonGeneric creativeSortBtn = new ButtonGeneric(creativeSortBtnX, depthCtrlY,
                creativeSortBtnW, 20, creativeSortBtnLabel);
        creativeSortBtn.setRenderDefaultBackground(true);
        this.addButton(creativeSortBtn, (IButtonActionListener) (b, mb) -> {
            creativeSortEnabled = !creativeSortEnabled;
            hasPendingChanges = true;
            initGui();
        });

        // ---- 是否同步替换开关（默认开启） ----
        String syncReplaceLabel = syncReplacementEnabled
                ? I18n.tr("litematlist.button.sync_replace_on")
                : I18n.tr("litematlist.button.sync_replace_off");
        syncReplaceBtnW = this.font.width(syncReplaceLabel) + 10;
        syncReplaceBtnX = creativeSortBtnX + creativeSortBtnW + 4;
        // 按钮白字不渲染"开/关"，彩色文字单独覆盖
        String syncReplaceBtnLabel = syncReplaceLabel.substring(0, syncReplaceLabel.length() - 1);
        ButtonGeneric syncReplaceBtn = new ButtonGeneric(syncReplaceBtnX, depthCtrlY,
                syncReplaceBtnW, 20, syncReplaceBtnLabel);
        syncReplaceBtn.setRenderDefaultBackground(true);
        this.addButton(syncReplaceBtn, (IButtonActionListener) (b, mb) -> {
            syncReplacementEnabled = !syncReplacementEnabled;
            initGui();
        });

        // ---- 导出表式JSON按钮（同步替换右侧） ----
        String exportTableJsonLabel = I18n.tr("litematlist.button.export_table_json");
        int exportTableJsonBtnW = this.font.width(exportTableJsonLabel) + 10;
        ButtonGeneric exportJsonBtn = new ButtonGeneric(syncReplaceBtnX + syncReplaceBtnW + 4, depthCtrlY,
                exportTableJsonBtnW, 20, exportTableJsonLabel);
        exportJsonBtn.setRenderDefaultBackground(true);
        this.addButton(exportJsonBtn, (IButtonActionListener) (b, mb) -> exportTreeAsJson());

        // ---- 重新分析按钮（导出表式JSON右侧） ----
        String reanalyzeLabel = I18n.tr("litematlist.button.reanalyze");
        int reanalyzeBtnW = this.font.width(reanalyzeLabel) + 10;
        int reanalyzeBtnX = syncReplaceBtnX + syncReplaceBtnW + 4 + exportTableJsonBtnW + 4;
        ButtonGeneric reanalyzeBtn = new ButtonGeneric(reanalyzeBtnX, depthCtrlY,
                reanalyzeBtnW, 20, reanalyzeLabel);
        reanalyzeBtn.setRenderDefaultBackground(true);
        this.addButton(reanalyzeBtn, (IButtonActionListener) (b, mb) -> {
            initialLoad = true;
            initGui();
        });

        // ---- 导出按钮（重新分析右侧）：默认导出txt，shift导出csv，alt导出json ----
        String exportLabel = I18n.tr("litematlist.button.export_raw");
        int exportBtnW = this.font.width(exportLabel) + 10;
        int exportRawBtnX = reanalyzeBtnX + reanalyzeBtnW + 4;
        ButtonGeneric exportBtn = new ButtonGeneric(exportRawBtnX, depthCtrlY,
                exportBtnW, 20, exportLabel);
        this.exportRawBtnX = exportRawBtnX;
        this.exportRawBtnW = exportBtnW;
        exportBtn.setRenderDefaultBackground(true);
        this.addButton(exportBtn, (IButtonActionListener) (b, mb) -> {
            long window = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
            boolean shift = org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS
                    || org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            boolean alt = org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT) == org.lwjgl.glfw.GLFW.GLFW_PRESS
                    || org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_ALT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            if (alt) {
                exportAsJson();
            } else if (shift) {
                exportAsCsv();
            } else {
                exportAsTxt();
            }
        });

        // ---- 原材料配置按钮（冗余列表按钮上方） ----
        String rawConfigLabel = I18n.tr("litematlist.button.raw_material_config");
        int rawConfigBtnW = this.font.width(rawConfigLabel) + 16;
        ButtonGeneric rawConfigBtn = new ButtonGeneric(this.width - rawConfigBtnW - 20, depthCtrlY - 24,
                rawConfigBtnW, 20, rawConfigLabel);
        rawConfigBtn.setRenderDefaultBackground(true);
        this.addButton(rawConfigBtn, (IButtonActionListener) (b, mb) -> {
            Minecraft.getInstance().setScreenAndShow(new RawMaterialConfigScreen(this));
        });

        // ---- 冗余列表按钮（最右侧） ----
        String redundancyLabel = I18n.tr("litematlist.button.redundancy_list");
        int redundancyBtnW = this.font.width(redundancyLabel) + 16;
        ButtonGeneric redundancyBtn = new ButtonGeneric(this.width - redundancyBtnW - 20, depthCtrlY,
                redundancyBtnW, 20, redundancyLabel);
        redundancyBtn.setRenderDefaultBackground(true);
        this.addButton(redundancyBtn, (IButtonActionListener) (b, mb) -> {
            MaterialDetailScreen.rawMaterialRedundancy.put(pathKey,
                    new ArrayList<>(redundancyItems));
            MaterialDetailScreen.savePersistence();
            Minecraft.getInstance().setScreenAndShow(
                    new RedundancyListScreen(this, schematicName, filePath));
        });

        // ---- 底部返回按钮 ----
        int buttonY = this.height - 38;
        ButtonGeneric backBtn = new ButtonGeneric(this.width / 2 - 60, buttonY, 120, 20, I18n.tr("litematlist.button.back"));
        backBtn.setRenderDefaultBackground(true);
        this.addButton(backBtn, (IButtonActionListener) (b, mb) -> {
            saveCollapseState();
            Minecraft.getInstance().setScreenAndShow(parent);
        });

        if (goingToSubScreen) {
            goingToSubScreen = false;
            // 从子界面返回时，重新加载分析以确保忽略/替换状态同步
            double savedSX = this.scrollX;
            double savedSY = this.scrollY;
            loadOrAnalyze();
            this.scrollX = Mth.clamp(savedSX, this.minScrollX, this.maxScrollX);
            this.scrollY = Mth.clamp(savedSY, 0, this.maxScrollY);
        } else if (!analyzing) {
            loadOrAnalyze();
        }
    }

    @Override
    public void removed() {
        saveCollapseState();
        if (!goingToSubScreen) {
            // 真正离开界面时标记需要重新加载，确保材料列表变动后同步
            initialLoad = true;
            hasPendingChanges = false;
        }
        super.removed();
    }

    private void saveCollapseState() {
        Set<String> collapsed = new HashSet<>();
        for (NodeLayout root : rootLayouts) {
            collectCollapsedPaths(root, "", collapsed);
        }
        if (!collapsed.isEmpty()) {
            COLLAPSED_PATHS.put(pathKey, collapsed);
        } else {
            COLLAPSED_PATHS.remove(pathKey);
        }
    }

    private void collectCollapsedPaths(NodeLayout layout, String prefix, Set<String> out) {
        String path = prefix.isEmpty() ? layout.node.itemId : prefix + ">" + layout.node.itemId;
        if (layout.collapsed) {
            out.add(path);
        }
        for (NodeLayout child : layout.children) {
            collectCollapsedPaths(child, path, out);
        }
    }

    private void restoreCollapseState() {
        Set<String> collapsed = COLLAPSED_PATHS.get(pathKey);
        if (collapsed == null || collapsed.isEmpty()) return;
        for (NodeLayout root : rootLayouts) {
            restoreCollapseStateRecursive(root, "", collapsed);
        }
    }

    private void restoreCollapseStateRecursive(NodeLayout layout, String prefix, Set<String> collapsed) {
        String path = prefix.isEmpty() ? layout.node.itemId : prefix + ">" + layout.node.itemId;
        if (collapsed.contains(path)) {
            layout.collapsed = true;
        }
        for (NodeLayout child : layout.children) {
            restoreCollapseStateRecursive(child, path, collapsed);
        }
    }

    private void reanalyze() {
        initialLoad = true;
        initGui();
    }

    /** 供 IgnoredItemsScreen 回调 */
    void rebuildRows() {
        initialLoad = true;
        initGui();
    }

    private void loadOrAnalyze() {
        double savedScrollX = this.scrollX;
        double savedScrollY = this.scrollY;
        treeRoots.clear();
        rootLayouts.clear();
        redundancyItems.clear();
        scrollX = 0;
        scrollY = 0;
        String pathKey = filePath != null ? filePath.toString() : schematicName;

        Set<Item> materialIgnored;
        List<MaterialEntry> entries = null;
        if (filePath != null) {
            materialIgnored = MaterialDetailScreen.getIgnoredForPath(filePath);
            if (SyncmaticaBridge.isSyncPath(filePath)) {
                // 「在Syncmatica认领的材料」：每次打开都向 Syncmatica 重新拉取最新认领数据
                entries = SyncmaticaBridge.loadClaimedMaterialEntries();
            } else {
                entries = MaterialListInjector.loadMaterialsCached(filePath);
                if (entries == null || entries.isEmpty()) {
                    Map<Path, List<MaterialEntry>> imported = MaterialDetailScreen.getImportedMaterials();
                    entries = imported.get(filePath);
                }
            }
            // 应用替换映射
            if (entries != null) {
                Path fp = filePath;
                entries = entries.stream()
                        .map(e -> {
                            if (e.item() == null) return e;
                            Item replaced = MaterialDetailScreen.getReplacement(fp, e.item());
                            if (replaced != null && replaced != e.item()) {
                                return new MaterialEntry(replaced, e.blockName(), e.totalCount());
                            }
                            return e;
                        })
                        .collect(Collectors.toList());
            }
        } else {
            // 项目模式：忽略按子条目各自过滤（在 loadProjectEntries 内处理），
            // 避免"任一来源忽略→整体移除"，改为"部分来源忽略→数量削减"
            materialIgnored = new HashSet<>();
            entries = loadProjectEntries(schematicName);
        }

        // 应用忽略过滤（替换后执行，确保替换后的物品也能被正确过滤）
        if (entries != null && !materialIgnored.isEmpty()) {
            entries = entries.stream()
                    .filter(e -> e.item() != null && !materialIgnored.contains(e.item()))
                    .collect(Collectors.toList());
        }

        if (entries != null && !entries.isEmpty()) {
            analyzing = true;
            try {
                // 在主线程执行分析，因为RecipeBookUtils需要访问客户端数据
                RawMaterialTreeAnalyzer analyzer = new RawMaterialTreeAnalyzer();
                analyzer.setMaxDepth(displayDepth);
                RawMaterialTreeAnalyzer.TreeResult result = analyzer.analyze(entries);
                treeRoots = result.roots();
                redundancyItems = result.redundancy();
                if (creativeSortEnabled) {
                    sortByCreativeOrder();
                }
                computeLayout();
            } finally {
                // 无论分析成功与否都必须复位，否则渲染将一直卡在"正在分析中"、搜索图标和树都不再显示
                analyzing = false;
            }
        }

        if (initialLoad) {
            initialLoad = false;
        } else {
            // 非首次加载时恢复滚动位置
            this.scrollX = Mth.clamp(savedScrollX, minScrollX, maxScrollX);
            this.scrollY = Mth.clamp(savedScrollY, 0, maxScrollY);
        }
    }

    private List<MaterialEntry> loadProjectEntries(String projectId) {
        List<MaterialListScreen.LoadedEntry> children = MaterialListScreen.getProjectEntries(projectId);
        if (children == null || children.isEmpty()) return null;

        Map<String, MaterialEntry> merged = new LinkedHashMap<>();
        for (MaterialListScreen.LoadedEntry child : children) {
            List<MaterialEntry> childEntries = null;
            Path childPath = child.filePath();
            if (childPath != null) {
                childEntries = MaterialListInjector.loadMaterialsCached(childPath);
                if (childEntries == null || childEntries.isEmpty()) {
                    Map<Path, List<MaterialEntry>> imported = MaterialDetailScreen.getImportedMaterials();
                    childEntries = imported.get(childPath);
                }
            }
            if (childEntries != null) {
                // 按子条目各自的忽略集合过滤：同一物品只忽略部分来源时，数量应削减而非整体移除
                Set<Item> childIgnored = childPath != null
                        ? MaterialDetailScreen.getIgnoredForPath(childPath) : Set.of();
                for (MaterialEntry entry : childEntries) {
                    if (entry.item() == null) continue;
                    // 应用替换映射，使树状图物品与【查看总材料列表】保持一致（跟随替换实时更新）
                    Item effectiveItem = childPath != null
                            ? MaterialDetailScreen.getReplacement(childPath, entry.item()) : entry.item();
                    // 忽略检查针对替换后的物品，与 ProjectSummaryScreen.isSourceIgnored 逻辑一致
                    if (childIgnored.contains(effectiveItem)) continue;
                    String key = String.valueOf(BuiltInRegistries.ITEM.getKey(effectiveItem));
                    MaterialEntry existing = merged.get(key);
                    if (existing != null) {
                        merged.put(key, new MaterialEntry(
                                effectiveItem,
                                existing.blockName() + ", " + child.name() + ":" + entry.blockName(),
                                entry.totalCount() + existing.totalCount()
                        ));
                    } else {
                        merged.put(key, new MaterialEntry(
                                effectiveItem,
                                child.name() + ":" + entry.blockName(),
                                entry.totalCount()
                        ));
                    }
                }
            }
        }
        return new ArrayList<>(merged.values());
    }

    // ==================== 布局计算（纯垂直堆叠，无斜率） ====================

    private void computeLayout() {
        rootLayouts.clear();
        if (treeRoots.isEmpty()) return;

        // 获取已保存的折叠路径
        Set<String> collapsed = COLLAPSED_PATHS.get(pathKey);
        if (collapsed == null) collapsed = Collections.emptySet();

        // 第一阶段：递归计算每个节点的subtreeHeight（折叠节点高度为NODE_HEIGHT）
        for (RecipeTreeNode root : treeRoots) {
            NodeLayout layout = computeNodeLayout(root, 0, "", collapsed);
            rootLayouts.add(layout);
        }

        // 第二阶段：从上到下分配绝对位置，子节点垂直堆叠
        int currentY = CONTROLS_BOTTOM;
        for (NodeLayout root : rootLayouts) {
            int rootCenterY = currentY + root.subtreeHeight / 2;
            assignPositions(root, 0, rootCenterY);
            currentY += root.subtreeHeight + VERTICAL_SPACING;
        }

        totalTreeHeight = Math.max(0, currentY - VERTICAL_SPACING);

        int maxDepth = 0;
        for (NodeLayout rl : rootLayouts) {
            int d = computeDepth(rl);
            if (d > maxDepth) maxDepth = d;
        }
        totalTreeWidth = maxDepth * (NODE_WIDTH + COLUMN_SPACING) + TREE_LEFT_MARGIN;

        // 最大滚动距离随缩放档位变化
        updateMaxScroll();
    }

    /**
     * 递归计算节点布局：子节点垂直堆叠，折叠节点的subtreeHeight为NODE_HEIGHT。
     */
    private NodeLayout computeNodeLayout(RecipeTreeNode node, int depth, String path, Set<String> collapsed) {
        NodeLayout layout = new NodeLayout();
        layout.node = node;

        String currentPath = path.isEmpty() ? node.itemId : path + ">" + node.itemId;

        if (collapsed.contains(currentPath)) {
            // 折叠的节点：高度仅为NODE_HEIGHT，不展开子节点
            layout.collapsed = true;
            layout.subtreeHeight = NODE_HEIGHT;
            return layout;
        }

        if (node.isLeaf()) {
            layout.subtreeHeight = NODE_HEIGHT;
        } else {
            List<NodeLayout> childLayouts = new ArrayList<>();
            int totalHeight = 0;
            int n = node.subMaterials.size();
            for (int i = 0; i < n; i++) {
                NodeLayout cl = computeNodeLayout(node.subMaterials.get(i), depth + 1, currentPath, collapsed);
                cl.parentItemId = node.itemId;
                cl.grandparentItemId = layout.parentItemId;
                childLayouts.add(cl);
                totalHeight += cl.subtreeHeight;
                if (i < n - 1) {
                    totalHeight += VERTICAL_SPACING;
                }
            }
            layout.children = childLayouts;
            layout.subtreeHeight = Math.max(NODE_HEIGHT, totalHeight);
        }

        return layout;
    }

    /**
     * 分配绝对位置：子节点从父节点subtree顶部开始垂直堆叠。
     */
    private void assignPositions(NodeLayout layout, int depth, int parentCenterY) {
        layout.x = TREE_LEFT_MARGIN + depth * (NODE_WIDTH + COLUMN_SPACING);
        layout.y = parentCenterY;

        if (!layout.children.isEmpty() && !layout.collapsed) {
            int childY = parentCenterY - layout.subtreeHeight / 2;
            for (NodeLayout child : layout.children) {
                int childCenterY = childY + child.subtreeHeight / 2;
                assignPositions(child, depth + 1, childCenterY);
                childY += child.subtreeHeight + VERTICAL_SPACING;
            }
        }
    }

    private int computeDepth(NodeLayout layout) {
        int maxD = 1;
        for (NodeLayout child : layout.children) {
            int d = computeDepth(child) + 1;
            if (d > maxD) maxD = d;
        }
        return maxD;
    }

    // ==================== 渲染 ====================

    @Override
    public void extractRenderState(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
        this.extractBackground(drawContext, mouseX, mouseY, delta);

        // 刷新搜索匹配列表（树数据或关键字变化时重建，并自动定位首个匹配项）
        refreshSearchMatches();

        // 按钮
        super.extractRenderState(drawContext, mouseX, mouseY, delta);

        // ---- 渲染开关按钮上的"开/关"彩色文字 ----
        // 创造模式排序按钮
        {
            String fullLabel = creativeSortEnabled
                    ? I18n.tr("litematlist.button.creative_sort_on")
                    : I18n.tr("litematlist.button.creative_sort_off");
            String prefix = fullLabel.substring(0, fullLabel.length() - 1);
            String state = fullLabel.substring(fullLabel.length() - 1);
            int prefixW = this.font.width(prefix);
            int stateColor = creativeSortEnabled ? 0xFF55FF55 : 0xFFFF5555;
            int textStartX = creativeSortBtnX + (creativeSortBtnW - this.font.width(fullLabel)) / 2;
            drawContext.text(this.font, state, textStartX + prefixW, 39, stateColor, false);
        }
        // 是否同步替换按钮
        {
            String fullLabel = syncReplacementEnabled
                    ? I18n.tr("litematlist.button.sync_replace_on")
                    : I18n.tr("litematlist.button.sync_replace_off");
            String prefix = fullLabel.substring(0, fullLabel.length() - 1);
            String state = fullLabel.substring(fullLabel.length() - 1);
            int prefixW = this.font.width(prefix);
            int stateColor = syncReplacementEnabled ? 0xFF55FF55 : 0xFFFF5555;
            int textStartX = syncReplaceBtnX + (syncReplaceBtnW - this.font.width(fullLabel)) / 2;
            drawContext.text(this.font, state, textStartX + prefixW, 39, stateColor, false);
        }

        // ---- 树状图搜索放大镜图标：在 analyzing 检查之前渲染，保证任何状态异常都不会让图标消失 ----
        computeSearchLayout();
        boolean searchIconHovered = mouseX >= searchIconX && mouseX < searchIconX + SEARCH_ICON_SIZE
                && mouseY >= searchIconY && mouseY < searchIconY + SEARCH_ICON_SIZE;
        if (searchIconHovered) {
            drawContext.fill(searchIconX - 1, searchIconY - 1,
                    searchIconX + SEARCH_ICON_SIZE + 1, searchIconY + SEARCH_ICON_SIZE + 1, 0x40FFFFFF);
        }
        drawIconTexture(drawContext, SEARCH_ICON, searchIconX, searchIconY, SEARCH_ICON_SIZE);

        if (analyzing) {
            drawContext.centeredText(this.font,
                    I18n.tr("litematlist.raw_materials.analyzing"),
                    this.width / 2, (this.height - 100) / 2, 0xFFFFFF55);
            return;
        }

        if (treeRoots.isEmpty()) {
            drawContext.centeredText(this.font,
                    I18n.tr("litematlist.raw_materials.empty"),
                    this.width / 2, (this.height - 100) / 2, 0x80FFFFFF);
        } else {
            // 渲染树（整体缩放预览时应用矩阵缩放，命中检测使用换算后的世界坐标）
            hoveredNode = null;
            hoveredRecipeNode = null;
            float zs = currentZoomScale();
            boolean zoomed = zs != 1.0F;
            float zsOffsetY = zoomed ? CONTROLS_BOTTOM * (1.0F - zs) : 0.0F;
            int wMouseX = zoomed ? (int) (mouseX / zs) : mouseX;
            int wMouseY = zoomed ? (int) ((mouseY - zsOffsetY) / zs) : mouseY;
            int treeAreaBottom = cullBottomW();
            if (zoomed) {
                drawContext.pose().pushMatrix();
                drawContext.pose().translate(0.0F, zsOffsetY);
                drawContext.pose().scale(zs, zs);
            }

            // 绘制连接线
            for (NodeLayout root : rootLayouts) {
                renderConnectors(drawContext, root, wMouseX, wMouseY);
            }

            // 绘制节点
            for (NodeLayout root : rootLayouts) {
                renderNodeTree(drawContext, root, wMouseX, wMouseY, treeAreaBottom);
            }

            if (zoomed) {
                drawContext.pose().popMatrix();
            }

            // 配方图标悬停提示
            if (hoveredRecipeNode != null && hoveredNode == null) {
                RecipeTreeNode rn = hoveredRecipeNode.node;
                if (rn.alternativeRecipeTypes != null && !rn.alternativeRecipeTypes.isEmpty()) {
                    // tooltip replaced with manual rendering
                    String altTip = I18n.tr("litematlist.recipe.alt_available");
                    int altTipW = this.font.width(altTip) + 8;
                    drawContext.fill(mouseX + 12, mouseY - 12, mouseX + 12 + altTipW, mouseY + 4, 0xCC000000);
                }
            }

            // 导出按钮悬停提示（多行）
            if (mouseX >= exportRawBtnX && mouseX < exportRawBtnX + exportRawBtnW
                    && mouseY >= 34 && mouseY < 54) {
                String exportTip = I18n.tr("litematlist.button.export_raw_tip").replace("\n", " | ");
                int exportTipW = this.font.width(exportTip) + 8;
                drawContext.fill(mouseX + 12, mouseY - 12, mouseX + 12 + exportTipW, mouseY + 4, 0xCC000000);
            }
        }

        // ---- 层数控制渲染（最高图层，纯白色，在树之上渲染） ----
        // 新布局: 标签 → 输入框 → [+-]图标
        int fieldX = 60;
        int depthCtrlY = 34;

        // "层数:" 标签（纯白色，无阴影）
        String depthLabel = I18n.tr("litematlist.tree.depth_label");
        drawContext.text(this.font, depthLabel, 18, depthCtrlY + 4, 0xFFFFFFFF, false);

        // 输入框区域（先渲染，在图标左边）
        drawContext.fill(fieldX - 2, depthCtrlY, fieldX + 26, depthCtrlY + 18, 0xFFFFFFFF);
        drawContext.fill(fieldX - 1, depthCtrlY + 1, fieldX + 25, depthCtrlY + 17, 0xFF111111);
        if (depthField != null) {
            String txt = depthField.getValue();
            drawContext.text(this.font, txt, fieldX + 3, depthCtrlY + 4, 0xFFFFFFFF, false);
            if (depthField.isFocused() && System.currentTimeMillis() % 1000 < 500) {
                int cp = depthField.getCursorPosition();
                String bc = txt.substring(0, Math.min(cp, txt.length()));
                int cx = fieldX + 3 + this.font.width(bc);
                drawContext.fill(cx, depthCtrlY + 3, cx + 1, depthCtrlY + 15, 0xFFFFFFFF);
            }
        }

        // 层数加减图标（在输入框后面）
        int depthIconX = fieldX + 30;
        depthIconHovered = mouseX >= depthIconX && mouseX < depthIconX + DEPTH_ICON_SIZE
                && mouseY >= depthCtrlY && mouseY < depthCtrlY + DEPTH_ICON_SIZE;
        Identifier depthIcon = depthIconHovered ? DEPTH_HOVERED : DEPTH_NORMAL;
        drawIconTexture(drawContext, depthIcon, depthIconX, depthCtrlY, DEPTH_ICON_SIZE);

        // ---- 树状图搜索控件（搜索框/信息行/导航按钮，在树之上渲染） ----
        if (this.searchVisible) {
            // 输入框
            drawContext.fill(searchFieldX - 2, searchFieldY - 1,
                    searchFieldX + SEARCH_FIELD_W + 2, searchFieldY + SEARCH_FIELD_H + 1, 0xFFFFFFFF);
            drawContext.fill(searchFieldX - 1, searchFieldY,
                    searchFieldX + SEARCH_FIELD_W + 1, searchFieldY + SEARCH_FIELD_H, 0xFF111111);
            if (searchField != null) {
                String txt = searchField.getValue();
                String shown = txt;
                int availW = SEARCH_FIELD_W - 8;
                if (this.font.width(txt) > availW) {
                    shown = this.font.plainSubstrByWidth(txt, availW - this.font.width("…")) + "…";
                }
                drawContext.text(this.font, shown, searchFieldX + 3, searchFieldY + 3, 0xFFFFFFFF, false);
                if (searchField.isFocused() && System.currentTimeMillis() % 1000 < 500) {
                    int cp = Math.min(searchField.getCursorPosition(), txt.length());
                    String bc = txt.substring(0, cp);
                    int cx = searchFieldX + 3 + this.font.width(bc);
                    drawContext.fill(cx, searchFieldY + 2, cx + 1, searchFieldY + SEARCH_FIELD_H - 2, 0xFFFFFFFF);
                }
            }

            // 匹配信息 + 上下导航按钮（↑=上一个匹配项，↓=下一个匹配项）
            int total = searchMatches.size();
            String info = total > 0
                    ? "共" + total + "个匹配项," + (searchIndex + 1) + "/" + total
                    : "共0个匹配项";
            int infoW = this.font.width(info);
            // 信息行以 searchNavX 为起点，与输入框最左侧严格对齐
            drawContext.text(this.font, info, searchNavX, searchNavY + 4, 0xFFFFFFFF, false);

            if (total > 0) {
                searchUpBtnX = searchNavX + infoW + 8;
                searchDownBtnX = searchUpBtnX + SEARCH_NAV_BTN_SIZE + 4;
                // ↑↓按钮超出屏幕右缘时换行到信息行下方（第三排），左对齐信息行，避免按钮被截断出屏
                if (searchDownBtnX + SEARCH_NAV_BTN_SIZE + 2 > this.width - 2) {
                    searchUpBtnX = searchNavX;
                    searchDownBtnX = searchUpBtnX + SEARCH_NAV_BTN_SIZE + 4;
                    searchBtnY = searchNavY + 15;
                } else {
                    searchBtnY = searchNavY;
                }
                drawSearchNavButton(drawContext, searchUpBtnX, searchBtnY, "↑", mouseX, mouseY);
                drawSearchNavButton(drawContext, searchDownBtnX, searchBtnY, "↓", mouseX, mouseY);
            }
        }

        // ---- 右下角缩放控制条（【-】百分比【+】；【配置】-【通用】开关） ----
        if (this.treeZoomEnabled) {
            drawZoomBar(drawContext, mouseX, mouseY);
        }

        // 右下角斜体提示：界面可用鼠标拖动
        String dragHint = I18n.tr("litematlist.raw_materials.drag_hint");
        int hintW = this.font.width(dragHint);
        drawContext.text(this.font, Component.literal(dragHint),
                this.width - hintW - 10, this.height - 18, 0x60FFFFFF, false);

        // ---- 滚动条渲染 ----
        renderScrollbars(drawContext, mouseX, mouseY);

        // ---- 左下角统计信息 ----
        int[] counts = countNodes();
        String stats = I18n.tr("litematlist.raw_materials.stats", counts[0], counts[1]);
        drawContext.text(this.font, stats, 10, this.height - 21, 0x80FFFFFF);
    }

    // ==================== 节点渲染 ====================

    private void renderNodeTree(GuiGraphicsExtractor drawContext, NodeLayout layout,
                                int mouseX, int mouseY, int treeAreaBottom) {
        // Using gfx directly
        int screenX = layout.x - (int) scrollX;
        int nodeCenterY = layout.y - (int) scrollY;
        int screenY = nodeCenterY - NODE_HEIGHT / 2;

        if (screenX + NODE_WIDTH < 0 || screenX > cullRightW()
                || screenY + NODE_HEIGHT < cullTopW() || screenY > treeAreaBottom) {
            if (!layout.collapsed) {
                for (NodeLayout child : layout.children) {
                    renderNodeTree(drawContext, child, mouseX, mouseY, treeAreaBottom);
                }
            }
            return;
        }

        boolean isHovered = mouseX >= screenX && mouseX < screenX + NODE_WIDTH
                && mouseY >= screenY && mouseY < screenY + NODE_HEIGHT;
        boolean isSearchMatch = (layout == this.currentSearchMatch);
        int bgColor = isSearchMatch ? 0x90B8860B : (isHovered ? 0xB0404040 : 0x80000000);
        if (isHovered) {
            hoveredNode = layout;
        }

        boolean isLeaf = layout.node.isLeaf();
        int borderColor = isSearchMatch ? 0xFFB8860B : (isLeaf ? 0xFF558855 : 0xFF5588AA);
        if (layout.collapsed && !isSearchMatch) borderColor = 0xFFCC8844;

        drawContext.fill(screenX, screenY, screenX + NODE_WIDTH, screenY + NODE_HEIGHT, bgColor);
        drawContext.fill(screenX, screenY, screenX + 3, screenY + NODE_HEIGHT, borderColor);

        var item = layout.node.getItem();
        if (item != null) {
            drawContext.item(new ItemStack(item), screenX + 6, screenY + 2);
        }

        String fullName = layout.node.itemId;
        if (item != null) {
            fullName = new ItemStack(item).getHoverName().getString();
        }

        // 数量紧挨名称，折叠图标在最右边；通配物品显示齿轮替换图标
        String countStr = "x" + layout.node.count;
        int countW = this.font.width(countStr);

        boolean isWildcard = layout.node.wildcardSubMaterial;
        int expandIconX = 0;
        int gearIconX = 0;
        int nameEndX = screenX + NODE_WIDTH - 6;

        if (!layout.node.isLeaf()) {
            expandIconX = screenX + NODE_WIDTH - EXPAND_ICON_SIZE - 4;
            nameEndX = expandIconX - 4;
        }

        if (isWildcard) {
            if (expandIconX > 0) {
                gearIconX = expandIconX - GEAR_ICON_SIZE - 2;
                nameEndX = gearIconX - 4;
            } else {
                gearIconX = screenX + NODE_WIDTH - GEAR_ICON_SIZE - 4;
                nameEndX = gearIconX - 4;
            }
        }

        // 名称可用宽度 = 到折叠图标/右边缘 - 图标位置 - 数量宽度 - 间距
        int nameStartX = screenX + 26;
        int maxAvailWidth = nameEndX - nameStartX - countW - 4;
        String displayName = this.font.plainSubstrByWidth(fullName, maxAvailWidth);
        if (displayName.length() < fullName.length()) {
            String withEllipsis = this.font.plainSubstrByWidth(fullName, maxAvailWidth - this.font.width("…"));
            displayName = withEllipsis + "…";
        }
        int displayNameW = this.font.width(displayName);

        // 数量紧跟在名称后面
        int countX = nameStartX + displayNameW + 4;
        drawContext.text(this.font, displayName, nameStartX, screenY + 5, 0xFFFFFFFF);
        drawContext.text(this.font, countStr, countX, screenY + 5, 0xFFAAAAAA);

        // 通配物品齿轮替换图标
        if (isWildcard && gearIconX > 0) {
            int gearY = screenY + (NODE_HEIGHT - GEAR_ICON_SIZE) / 2;
            boolean gearHovered = mouseX >= gearIconX && mouseX < gearIconX + GEAR_ICON_SIZE
                    && mouseY >= gearY && mouseY < gearY + GEAR_ICON_SIZE;
            drawIconTexture(drawContext, GEAR_ICON, gearIconX, gearY, GEAR_ICON_SIZE);
        }

        if (!layout.node.isLeaf()) {
            int iconY = screenY + (NODE_HEIGHT - EXPAND_ICON_SIZE) / 2;
            boolean iconHovered = mouseX >= expandIconX && mouseX < expandIconX + EXPAND_ICON_SIZE
                    && mouseY >= iconY && mouseY < iconY + EXPAND_ICON_SIZE;
            Identifier icon;
            if (layout.collapsed) {
                icon = iconHovered ? EXPAND_HOVERED : EXPAND_NORMAL;
            } else {
                icon = iconHovered ? COLLAPSE_HOVERED : COLLAPSE_NORMAL;
            }
            drawIconTexture(drawContext, icon, expandIconX, iconY, EXPAND_ICON_SIZE);
        }

        // 悬停提示：名称上显示完整ID，数量上显示换算量级
        if (item != null) {
            boolean nameHovered = mouseX >= nameStartX && mouseX < nameStartX + displayNameW
                    && mouseY >= screenY && mouseY < screenY + NODE_HEIGHT;
            boolean countHovered = mouseX >= countX && mouseX < countX + countW
                    && mouseY >= screenY && mouseY < screenY + NODE_HEIGHT;
            if (nameHovered) {
                String tip = fullName + " (" + layout.node.itemId + ")";
                // manual tooltip replaced
            } else if (countHovered) {
                // 换算量级：与 MaterialDetailScreen 格式一致
                int count = layout.node.count;
                int shulker = count / 1728;
                int remainder = count % 1728;
                int stacks = remainder / 64;
                int items = remainder % 64;
                double shulkerFloat = (double) count / 1728.0;
                String tip = String.format("%d = %d×1728 + %d×64 + %d  |  %d×64+%d  |  %.2f 潜影盒",
                        count, shulker, stacks, items,
                        shulker * 27 + stacks, items,
                        shulkerFloat);
                // manual tooltip replaced
            } else if (isHovered) {
                // 整个条悬停，显示名称+ID
                String tip = fullName + " (" + layout.node.itemId + ")";
                // manual tooltip replaced
            }
        }

        if (!layout.collapsed) {
            for (NodeLayout child : layout.children) {
                renderNodeTree(drawContext, child, mouseX, mouseY, treeAreaBottom);
            }
        }
    }

    // ==================== 连接线渲染（横线+竖线，无斜线） ====================

    /** 渲染独立纹理图标（26.x：blitSprite 依赖图集注册，自定义纹理需用 bindTexture+blit，注意blit参数为角点坐标） */
    private void drawIconTexture(GuiGraphicsExtractor drawContext, Identifier textureId, int x, int y, int size) {
        Pair<GpuTextureView, GpuSampler> pair =
                GuiContext.fromGuiGraphics(drawContext).bindTexture(textureId);
        if (pair != null && pair.getLeft() != null) {
            drawContext.blit(pair.getLeft(), pair.getRight(), x, y, x + size, y + size, 0.0f, 1.0f, 0.0f, 1.0f);
        }
    }

    private void renderConnectors(GuiGraphicsExtractor drawContext, NodeLayout layout,
                                   int mouseX, int mouseY) {
        if (layout.collapsed || layout.children.isEmpty()) return;

        int parentRightX = layout.x - (int) scrollX + NODE_WIDTH;
        int parentCenterY = layout.y - (int) scrollY;
        int treeAreaBottom = cullBottomW();

        // 父节点超出可视区域则不渲染连线
        if (parentRightX + COLUMN_SPACING < 0 || parentRightX > cullRightW()
                || parentCenterY < cullTopW() || parentCenterY > treeAreaBottom) {
            // 但仍需递归渲染子节点连线
            for (NodeLayout child : layout.children) {
                renderConnectors(drawContext, child, mouseX, mouseY);
            }
            return;
        }

        // 收集可见子节点
        List<NodeLayout> visibleChildren = new ArrayList<>();
        int minChildY = Integer.MAX_VALUE;
        int maxChildY = Integer.MIN_VALUE;

        for (NodeLayout child : layout.children) {
            int childLeftX = child.x - (int) scrollX;
            int childCenterY = child.y - (int) scrollY;
            if (childLeftX > cullRightW() || childLeftX + NODE_WIDTH < 0
                    || childCenterY < cullTopW() || childCenterY > treeAreaBottom) {
                continue;
            }
            visibleChildren.add(child);
            if (childCenterY < minChildY) minChildY = childCenterY;
            if (childCenterY > maxChildY) maxChildY = childCenterY;
        }

        if (visibleChildren.isEmpty()) {
            for (NodeLayout child : layout.children) {
                renderConnectors(drawContext, child, mouseX, mouseY);
            }
            return;
        }

        int midX = parentRightX + COLUMN_SPACING / 2;

        // 水平线：从父节点右边缘到中点
        drawHLine(drawContext, parentRightX, midX, parentCenterY, 0x60FFFFFF);

        // 配方图标（在水平线中点）
        String recipeType = layout.node.recipeType;
        if (recipeType != null && !recipeType.isEmpty() && !"base".equals(recipeType)) {
            int iconX = (parentRightX + midX) / 2 - RECIPE_ICON_SIZE / 2;
            int iconY = parentCenterY - RECIPE_ICON_SIZE / 2;

            // 裁剪检查（使用已定义的treeAreaBottom）
            if (iconX + RECIPE_ICON_SIZE >= 0 && iconX <= cullRightW()
                    && iconY + RECIPE_ICON_SIZE >= cullTopW() && iconY <= treeAreaBottom) {
                ItemStack stationItem = getRecipeStationItem(recipeType);
                if (stationItem != null) {
                    drawContext.item(stationItem, iconX, iconY);
                }

                // 检测悬停
                if (mouseX >= iconX && mouseX < iconX + RECIPE_ICON_SIZE
                        && mouseY >= iconY && mouseY < iconY + RECIPE_ICON_SIZE) {
                    hoveredRecipeNode = layout;
                }
            }
        }

        if (visibleChildren.size() == 1) {
            // 只有一个子节点时，直接画L形线
            NodeLayout child = visibleChildren.get(0);
            int childCenterY = child.y - (int) scrollY;
            drawVLine(drawContext, midX, parentCenterY, childCenterY, 0x60FFFFFF);
            int childLeftX = child.x - (int) scrollX;
            drawHLine(drawContext, midX, childLeftX, childCenterY, 0x60FFFFFF);
        } else {
            // 多个子节点：从最上到最下画竖线，然后分别画水平线连到每个子节点
            drawVLine(drawContext, midX, minChildY, maxChildY, 0x60FFFFFF);

            for (NodeLayout child : visibleChildren) {
                int childLeftX = child.x - (int) scrollX;
                int childCenterY = child.y - (int) scrollY;
                // 从竖线到子节点左边缘的水平线
                drawHLine(drawContext, midX, childLeftX, childCenterY, 0x60FFFFFF);
            }
        }

        // 递归渲染子节点连线
        for (NodeLayout child : layout.children) {
            renderConnectors(drawContext, child, mouseX, mouseY);
        }
    }

    /** 画水平线，裁剪到可视区域 */
    private void drawHLine(GuiGraphicsExtractor drawContext, int x1, int x2, int y, int color) {
        if (x1 > x2) { int t = x1; x1 = x2; x2 = t; }
        int treeAreaTop = cullTopW();
        int treeAreaBottom = cullBottomW();
        int treeAreaRight = cullRightW();
        if (y < treeAreaTop || y > treeAreaBottom) return;
        if (x2 < 0 || x1 > treeAreaRight) return;
        x1 = Math.max(x1, 0);
        x2 = Math.min(x2, treeAreaRight);
        if (x1 <= x2) drawContext.fill(x1, y, x2 + 1, y + 1, color);
    }

    /** 画竖线，裁剪到可视区域 */
    private void drawVLine(GuiGraphicsExtractor drawContext, int x, int y1, int y2, int color) {
        if (y1 > y2) { int t = y1; y1 = y2; y2 = t; }
        int treeAreaTop = cullTopW();
        int treeAreaBottom = cullBottomW();
        int treeAreaRight = cullRightW();
        if (x < 0 || x > treeAreaRight) return;
        if (y2 < treeAreaTop || y1 > treeAreaBottom) return;
        y1 = Math.max(y1, treeAreaTop);
        y2 = Math.min(y2, treeAreaBottom);
        if (y1 <= y2) drawContext.fill(x, y1, x + 1, y2 + 1, color);
    }

    // ==================== 树状图搜索 ====================

    /** 计算搜索控件的当前布局：输入框位置（右侧优先、放不下转图标下方）、信息行位置 */
    private void computeSearchLayout() {
        // 首次布局：图标默认定位到屏幕右侧（贴近滚动条但不遮挡）
        if (searchIconX < 0) {
            searchIconX = Math.max(2, this.width - SCROLLBAR_THICKNESS - 4 - SEARCH_ICON_SIZE);
        }
        // 输入框默认在放大镜右侧；右侧展开会超出屏幕时改放图标下方
        if (searchIconX + SEARCH_ICON_SIZE + 6 + SEARCH_FIELD_W + 2 <= this.width - 2) {
            searchFieldX = searchIconX + SEARCH_ICON_SIZE + 6;
            searchFieldY = searchIconY + (SEARCH_ICON_SIZE - SEARCH_FIELD_H) / 2;
        } else {
            // 图标下方展开：输入框同样不能越出屏幕左右边界
            searchFieldX = Mth.clamp(searchIconX, 0,
                    Math.max(0, this.width - SEARCH_FIELD_W - 2));
            searchFieldY = searchIconY + SEARCH_ICON_SIZE + 4;
        }
        searchNavY = Math.max(searchIconY + SEARCH_ICON_SIZE + 4, searchFieldY + SEARCH_FIELD_H + 4);
        // 信息行与输入框最左侧严格对齐（与"共x个匹配项"左缘）
        searchNavX = searchFieldX;
    }

    /** 拖动时约束搜索图标位置：左右不超出屏幕（且不遮右侧滚动条），上下不越过滚动条的高度范围 */
    private void clampSearchPosition() {
        int minX = 2;
        int maxX = this.width - SCROLLBAR_THICKNESS - 4 - SEARCH_ICON_SIZE;
        searchIconX = Mth.clamp(searchIconX, minX, Math.max(minX, maxX));
        int minY = CONTROLS_BOTTOM;
        int maxY = (this.height - TREE_BOTTOM_OFFSET) - SEARCH_ICON_SIZE;
        searchIconY = Mth.clamp(searchIconY, minY, Math.max(minY, maxY));
        computeSearchLayout();
    }

    // ==================== 树状图整体缩放 ====================

    /** 当前生效的缩放倍率（100% 为原大小） */
    private float currentZoomScale() {
        return this.treeZoomEnabled ? treeZoomPercent / 100.0F : 1.0F;
    }

    /** 当前持久化的缩放百分比（供 MaterialDetailScreen 写 persistence.json 用） */
    public static int getTreeZoomPercent() {
        return treeZoomPercent;
    }

    /** 从持久化数据恢复缩放百分比，并归一化到合法档位 */
    public static void setTreeZoomPercent(int percent) {
        int clamped = Math.max(ZOOM_MIN_PERCENT, Math.min(ZOOM_MAX_PERCENT, percent));
        int steps = (clamped - ZOOM_MIN_PERCENT + ZOOM_STEP_PERCENT / 2) / ZOOM_STEP_PERCENT;
        treeZoomPercent = Math.min(ZOOM_MAX_PERCENT, ZOOM_MIN_PERCENT + steps * ZOOM_STEP_PERCENT);
    }

    /** 重置为默认档位（切换世界时调用，随后由新世界持久化数据覆盖） */
    public static void resetTreeZoomPercent() {
        treeZoomPercent = ZOOM_DEFAULT_PERCENT;
    }

    /** 可视区域右边界（世界坐标；缩放时按比例缩小可视范围） */
    private int cullRightW() {
        float zs = currentZoomScale();
        return zs != 1.0F ? (int) (this.width / zs) : this.width;
    }

    /** 可视区域上边界（世界坐标） */
    private int cullTopW() {
        return CONTROLS_BOTTOM;
    }

    /** 可视区域下边界（世界坐标） */
    private int cullBottomW() {
        float zs = currentZoomScale();
        int bottom = this.height - TREE_BOTTOM_OFFSET;
        return zs != 1.0F ? Math.max(cullTopW() + 1, (int) ((bottom - CONTROLS_BOTTOM * (1.0F - zs)) / zs)) : bottom;
    }

    /** 重算最大滚动距离（随缩放档位变化），并把当前滚动值夹回合法区间 */
    private void updateMaxScroll() {
        float zs = currentZoomScale();
        if (this.treeZoomEnabled) {
            // 缩放状态开启：树可在屏幕内整体平移，但左右两侧均不能被拖出屏幕。
            // 内容世界范围 [0, totalTreeWidth]，渲染时在此基础上再加 TREE_LEFT_MARGIN。
            int viewW = Math.max(1, (int) (this.width / zs));
            double a = TREE_LEFT_MARGIN + totalTreeWidth - viewW; // 树右缘贴屏幕右缘
            double b = TREE_LEFT_MARGIN;                          // 树左缘贴屏幕左缘
            minScrollX = Math.min(a, b); // 树整体小于视口（缩小档位）时 a<b：负值下限允许整体平移
            maxScrollX = Math.max(a, b);
        } else {
            int visibleWidth = Math.max(1, (int) ((this.width - TREE_LEFT_MARGIN) / zs));
            minScrollX = 0;
            maxScrollX = Math.max(0, totalTreeWidth - visibleWidth);
        }
        int visibleHeight = Math.max(1, (int) ((this.height - CONTROLS_BOTTOM - TREE_BOTTOM_OFFSET) / zs));
        maxScrollY = Math.max(0, totalTreeHeight - visibleHeight);
        scrollX = Mth.clamp(scrollX, minScrollX, maxScrollX);
        scrollY = Mth.clamp(scrollY, 0, maxScrollY);
    }

    /** 计算右下角缩放控制条布局：【-】 xxx% 【+】 */
    private void computeZoomBarLayout() {
        int pctW = this.font.width("200%"); // 按最大档位预留宽度，避免切换档位时按钮左右跳动
        int totalW = ZOOM_BTN_SIZE * 2 + 12 + pctW;
        zoomOutBtnX = this.width - 8 - totalW;
        zoomInBtnX = zoomOutBtnX + ZOOM_BTN_SIZE + 12 + pctW;
        zoomBarY = this.height - TREE_BOTTOM_OFFSET - 2; // 右下角，避开水平滚动条与拖动提示
    }

    /** 绘制右下角缩放控制条 */
    private void drawZoomBar(GuiGraphicsExtractor drawContext, int mouseX, int mouseY) {
        computeZoomBarLayout();
        String pct = treeZoomPercent + "%";
        int pctW = this.font.width(pct);
        int pctX = (zoomOutBtnX + ZOOM_BTN_SIZE + zoomInBtnX) / 2 - pctW / 2;
        drawContext.text(this.font, pct, pctX, zoomBarY + (ZOOM_BTN_SIZE - 8) / 2, 0xFFFFFFFF, false);

        boolean outHover = mouseX >= zoomOutBtnX - 2 && mouseX < zoomOutBtnX + ZOOM_BTN_SIZE + 2
                && mouseY >= zoomBarY - 2 && mouseY < zoomBarY + ZOOM_BTN_SIZE + 2;
        if (outHover) {
            drawContext.fill(zoomOutBtnX - 2, zoomBarY - 2, zoomOutBtnX + ZOOM_BTN_SIZE + 2,
                    zoomBarY + ZOOM_BTN_SIZE + 2, 0x40FFFFFF);
        }
        drawIconTexture(drawContext, ZOOM_OUT_ICON, zoomOutBtnX, zoomBarY, ZOOM_BTN_SIZE);

        boolean inHover = mouseX >= zoomInBtnX - 2 && mouseX < zoomInBtnX + ZOOM_BTN_SIZE + 2
                && mouseY >= zoomBarY - 2 && mouseY < zoomBarY + ZOOM_BTN_SIZE + 2;
        if (inHover) {
            drawContext.fill(zoomInBtnX - 2, zoomBarY - 2, zoomInBtnX + ZOOM_BTN_SIZE + 2,
                    zoomBarY + ZOOM_BTN_SIZE + 2, 0x40FFFFFF);
        }
        drawIconTexture(drawContext, ZOOM_IN_ICON, zoomInBtnX, zoomBarY, ZOOM_BTN_SIZE);
    }

    /** 绘制正方形导航按钮（↑/↓），悬停时变亮 */
    private void drawSearchNavButton(GuiGraphicsExtractor drawContext, int bx, int by, String arrow,
                                     int mouseX, int mouseY) {
        boolean hovered = mouseX >= bx && mouseX < bx + SEARCH_NAV_BTN_SIZE
                && mouseY >= by && mouseY < by + SEARCH_NAV_BTN_SIZE;
        int bg = hovered ? 0xFF555555 : 0xFF303030;
        int border = hovered ? 0xFFCCCCCC : 0xFF888888;
        drawContext.fill(bx, by, bx + SEARCH_NAV_BTN_SIZE, by + SEARCH_NAV_BTN_SIZE, bg);
        drawContext.fill(bx, by, bx + SEARCH_NAV_BTN_SIZE, by + 1, border);
        drawContext.fill(bx, by + SEARCH_NAV_BTN_SIZE - 1, bx + SEARCH_NAV_BTN_SIZE, by + SEARCH_NAV_BTN_SIZE, border);
        drawContext.fill(bx, by, bx + 1, by + SEARCH_NAV_BTN_SIZE, border);
        drawContext.fill(bx + SEARCH_NAV_BTN_SIZE - 1, by, bx + SEARCH_NAV_BTN_SIZE, by + SEARCH_NAV_BTN_SIZE, border);
        drawContext.centeredText(this.font, arrow, bx + SEARCH_NAV_BTN_SIZE / 2,
                by + (SEARCH_NAV_BTN_SIZE - 8) / 2, 0xFFFFFFFF);
    }

    /** 每帧刷新匹配列表：收集树中显示的所有叶子节点，按屏幕"从左到右、从上到下"排序后过滤出名称包含关键字的项 */
    private void refreshSearchMatches() {
        if (searchField == null) {
            searchMatches.clear();
            searchIndex = -1;
            currentSearchMatch = null;
            return;
        }
        String kw = searchField.getValue().trim().toLowerCase(Locale.ROOT);
        boolean kwChanged = !kw.equals(lastSearchKeyword);
        lastSearchKeyword = kw;

        List<NodeLayout> matches = new ArrayList<>();
        if (searchVisible && !kw.isEmpty()) {
            // 先收集所有可见节点（含带配方的中间节点，如海晶灯），再按屏幕坐标排序（x 优先、y 其次），实现"从左到右、从上到下"
            List<NodeLayout> allNodes = new ArrayList<>();
            for (NodeLayout root : rootLayouts) {
                collectVisibleNodes(root, allNodes);
            }
            allNodes.sort((a, b) -> a.x != b.x
                    ? Integer.compare(a.x, b.x)
                    : Integer.compare(a.y, b.y));
            // 纯数字关键字：匹配节点数量（数量文本包含该数字）；否则匹配中文名称或英文物品ID
            boolean numericKw = kw.matches("\\d+");
            for (NodeLayout node : allNodes) {
                boolean hit;
                if (numericKw) {
                    hit = String.valueOf(node.node.count).contains(kw);
                } else {
                    String name = node.node.itemId;
                    Item item = node.node.getItem();
                    if (item != null) name = new ItemStack(item).getHoverName().getString();
                    // 中文名 / 拼音全拼 / 拼音首字母 / 英文ID
                    hit = PinyinSearch.matches(kw, name, node.node.itemId);
                }
                if (hit) {
                    matches.add(node);
                }
            }
        }
        searchMatches.clear();
        searchMatches.addAll(matches);

        if (matches.isEmpty()) {
            searchIndex = -1;
            currentSearchMatch = null;
            return;
        }
        if (kwChanged || searchIndex < 0 || searchIndex >= matches.size()) {
            searchIndex = 0;
            scrollToMatch(matches.get(0));
        }
        currentSearchMatch = matches.get(searchIndex);
    }

    /** 收集树中所有可见节点（折叠节点只收集自身，展开节点连同子节点一起收集），供树状图搜索使用 */
    private void collectVisibleNodes(NodeLayout layout, List<NodeLayout> out) {
        out.add(layout);
        if (layout.collapsed || layout.children.isEmpty()) {
            return;
        }
        for (NodeLayout child : layout.children) {
            collectVisibleNodes(child, out);
        }
    }

    /** 自动滚动视口，使匹配节点可见（居中显示） */
    private void scrollToMatch(NodeLayout match) {
        int sx = match.x - (int) scrollX;
        int sy = match.y - (int) scrollY - NODE_HEIGHT / 2;
        int treeTop = cullTopW();
        int treeBottom = cullBottomW();
        int treeRight = cullRightW();
        if (sx < TREE_LEFT_MARGIN || sx + NODE_WIDTH > treeRight - TREE_LEFT_MARGIN) {
            scrollX = Mth.clamp(match.x - treeRight / 2, minScrollX, maxScrollX);
        }
        if (sy < treeTop || sy + NODE_HEIGHT > treeBottom) {
            scrollY = Mth.clamp(match.y - treeTop
                    - (treeBottom - treeTop) / 2, 0, maxScrollY);
        }
    }

    // ==================== 鼠标事件 ====================

    @Override
    public boolean onMouseClicked(MouseButtonEvent event, boolean isDrag) {
        if (super.onMouseClicked(event, isDrag)) return true;
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        // 树整体缩放时，树区域的命中检测使用换算后的世界坐标
        float zoomScaleNow = currentZoomScale();
        int worldMouseX = zoomScaleNow != 1.0F ? (int) (mouseX / zoomScaleNow) : mouseX;
        int worldMouseY = zoomScaleNow != 1.0F ? (int) ((mouseY - CONTROLS_BOTTOM * (1.0F - zoomScaleNow)) / zoomScaleNow) : mouseY;

        // ---- 滚动条点击处理 ----
        int treeAreaTop = CONTROLS_BOTTOM;
        int treeAreaBottom = this.height - TREE_BOTTOM_OFFSET;
        int treeAreaHeight = treeAreaBottom - treeAreaTop;

        // 垂直滚动条
        if (maxScrollY > 0 && mouseX >= this.width - SCROLLBAR_THICKNESS - 2
                && mouseX < this.width && mouseY >= treeAreaTop && mouseY <= treeAreaBottom) {
            int trackH = treeAreaHeight;
            int thumbH = Math.max(SCROLLBAR_MIN_THUMB,
                    (int) ((double) treeAreaHeight / (totalTreeHeight + treeAreaHeight) * treeAreaHeight));
            int thumbY = treeAreaTop + (int) ((double) scrollY / maxScrollY * (trackH - thumbH));
            if (mouseY >= thumbY && mouseY < thumbY + thumbH) {
                draggingVScrollbar = true;
                vScrollbarDragStartY = mouseY;
                vScrollStartValue = scrollY;
                return true;
            } else {
                // 点击轨道跳跃
                scrollY = Mth.clamp(
                        (double) (mouseY - treeAreaTop - thumbH / 2) / (trackH - thumbH) * maxScrollY,
                        0, maxScrollY);
                return true;
            }
        }

        // 水平滚动条
        if (maxScrollX > minScrollX && mouseY >= this.height - 4
                && mouseY < this.height && mouseX >= 0 && mouseX < this.width) {
            int trackW = this.width;
            int thumbW = Math.max(SCROLLBAR_MIN_THUMB,
                    (int) ((double) this.width / (totalTreeWidth + this.width) * trackW));
            double range = maxScrollX - minScrollX;
            int thumbX = (int) ((scrollX - minScrollX) / range * (trackW - thumbW));
            if (mouseX >= thumbX && mouseX < thumbX + thumbW) {
                draggingHScrollbar = true;
                hScrollbarDragStartX = mouseX;
                hScrollStartValue = scrollX;
                return true;
            } else {
                scrollX = minScrollX + (double) (mouseX - thumbW / 2) / (trackW - thumbW) * range;
                return true;
            }
        }

        // 层数图标点击（在输入框后面）
        int fieldX = 60;
        int depthCtrlY = 34;
        int depthIconX = fieldX + 30;
        if (mouseX >= depthIconX && mouseX < depthIconX + DEPTH_ICON_SIZE
                && mouseY >= depthCtrlY && mouseY < depthCtrlY + DEPTH_ICON_SIZE) {
            if (event.button() == 0) {
                // 左键：增加层数
                if (displayDepth < MAX_DEPTH) {
                    displayDepth++;
                    if (depthField != null) depthField.setValue(String.valueOf(displayDepth));
                    reanalyze();
                }
            } else if (event.button() == 1) {
                // 右键：减少层数
                if (displayDepth > MIN_DEPTH) {
                    displayDepth--;
                    if (depthField != null) depthField.setValue(String.valueOf(displayDepth));
                    reanalyze();
                }
            }
            return true;
        }

        // 层数输入框点击
        if (mouseX >= fieldX && mouseX < fieldX + 26
                && mouseY >= depthCtrlY && mouseY < depthCtrlY + 18) {
            if (depthField != null) {
                depthField.setFocused(true);
            }
            if (searchField != null && searchVisible) searchField.setFocused(false);
            return true;
        }

        // ---- 右下角缩放控制按钮（【-】/【+】） ----
        if (this.treeZoomEnabled && event.button() == 0
                && mouseY >= zoomBarY - 2 && mouseY < zoomBarY + ZOOM_BTN_SIZE + 2) {
            if (mouseX >= zoomOutBtnX - 2 && mouseX < zoomOutBtnX + ZOOM_BTN_SIZE + 2) {
                if (treeZoomPercent > ZOOM_MIN_PERCENT) {
                    treeZoomPercent = Math.max(ZOOM_MIN_PERCENT, treeZoomPercent - ZOOM_STEP_PERCENT);
                    updateMaxScroll();
                    MaterialDetailScreen.savePersistence();
                }
                return true;
            }
            if (mouseX >= zoomInBtnX - 2 && mouseX < zoomInBtnX + ZOOM_BTN_SIZE + 2) {
                if (treeZoomPercent < ZOOM_MAX_PERCENT) {
                    treeZoomPercent = Math.min(ZOOM_MAX_PERCENT, treeZoomPercent + ZOOM_STEP_PERCENT);
                    updateMaxScroll();
                    MaterialDetailScreen.savePersistence();
                }
                return true;
            }
        }

        // ---- 树状图搜索控件点击 ----
        // 自愈：点击不在放大镜上时，清除可能残留的按拖状态，
        // 防止后续 mouseReleased 误把"点击输入框/导航按钮"当成"点击放大镜"而收起搜索界面
        boolean onSearchIcon = event.button() == 0
                && mouseX >= searchIconX && mouseX < searchIconX + SEARCH_ICON_SIZE
                && mouseY >= searchIconY && mouseY < searchIconY + SEARCH_ICON_SIZE;
        if (!onSearchIcon) {
            searchDragPending = false;
            searchDragging = false;
        }
        // 放大镜按下：先记录按下状态（区分"点击切换显示"与"长按拖动位置"，判定在 mouseDragged / mouseReleased）
        if (event.button() == 0
                && mouseX >= searchIconX && mouseX < searchIconX + SEARCH_ICON_SIZE
                && mouseY >= searchIconY && mouseY < searchIconY + SEARCH_ICON_SIZE) {
            searchDragPending = true;
            searchDragMouseDownX = event.x();
            searchDragMouseDownY = event.y();
            searchDragStartIconX = searchIconX;
            searchDragStartIconY = searchIconY;
            if (depthField != null) depthField.setFocused(false);
            return true;
        }

        if (searchVisible) {
            // 搜索输入框点击
            if (mouseX >= searchFieldX - 2 && mouseX < searchFieldX + SEARCH_FIELD_W + 2
                    && mouseY >= searchFieldY - 1 && mouseY < searchFieldY + SEARCH_FIELD_H + 1) {
                if (searchField != null) {
                    searchField.setFocused(true);
                }
                if (depthField != null) depthField.setFocused(false);
                return true;
            }
            // 匹配导航按钮（↑=上一个匹配项，↓=下一个匹配项）
            if (event.button() == 0 && !searchMatches.isEmpty()
                    && mouseY >= searchBtnY && mouseY < searchBtnY + SEARCH_NAV_BTN_SIZE) {
                if (mouseX >= searchUpBtnX && mouseX < searchUpBtnX + SEARCH_NAV_BTN_SIZE) {
                    refreshSearchMatches();
                    if (!searchMatches.isEmpty()) {
                        searchIndex = (searchIndex - 1 + searchMatches.size()) % searchMatches.size();
                        currentSearchMatch = searchMatches.get(searchIndex);
                        scrollToMatch(currentSearchMatch);
                    }
                    // 导航后保持输入框焦点，用户可继续编辑搜索文字
                    if (searchField != null) searchField.setFocused(true);
                    return true;
                }
                if (mouseX >= searchDownBtnX && mouseX < searchDownBtnX + SEARCH_NAV_BTN_SIZE) {
                    refreshSearchMatches();
                    if (!searchMatches.isEmpty()) {
                        searchIndex = (searchIndex + 1) % searchMatches.size();
                        currentSearchMatch = searchMatches.get(searchIndex);
                        scrollToMatch(currentSearchMatch);
                    }
                    // 导航后保持输入框焦点，用户可继续编辑搜索文字
                    if (searchField != null) searchField.setFocused(true);
                    return true;
                }
            }
        }

        if (event.button() == 0) {
            // 点击配方图标（切换合成方式）
            if (hoveredRecipeNode != null && hoveredNode == null) {
                RecipeTreeNode node = hoveredRecipeNode.node;
                if (node.alternativeRecipeTypes != null && !node.alternativeRecipeTypes.isEmpty()) {
                    final double savedSX = this.scrollX;
                    final double savedSY = this.scrollY;
                    String newType = com.litematlist.RawMaterialTreeAnalyzer.toggleRecipeType(
                            node.itemId, node.recipeType, node.alternativeRecipeTypes);
                    LitematListMod.LOGGER.info("[RawMaterialScreen] 切换配方: {} -> {}",
                            node.itemId, newType != null ? newType : "默认");
                    reanalyze();
                    this.scrollX = Mth.clamp(savedSX, this.minScrollX, this.maxScrollX);
                    this.scrollY = Math.min(savedSY, this.maxScrollY);
                    return true;
                }
            }

            // 点击节点
            if (hoveredNode != null) {
                int nodeScreenY = hoveredNode.y - (int) scrollY - NODE_HEIGHT / 2;
                int nodeScreenX = hoveredNode.x - (int) scrollX;

                // 齿轮替换图标点击（通配物品的子材料）
                if (hoveredNode.node.wildcardSubMaterial) {
                    int gearX;
                    if (!hoveredNode.node.isLeaf()) {
                        gearX = nodeScreenX + NODE_WIDTH - EXPAND_ICON_SIZE - 4 - GEAR_ICON_SIZE - 2;
                    } else {
                        gearX = nodeScreenX + NODE_WIDTH - GEAR_ICON_SIZE - 4;
                    }
                    int gearY = nodeScreenY + (NODE_HEIGHT - GEAR_ICON_SIZE) / 2;
                    if (worldMouseX >= gearX && worldMouseX < gearX + GEAR_ICON_SIZE
                            && worldMouseY >= gearY && worldMouseY < gearY + GEAR_ICON_SIZE) {
                        openWildcardReplacement(hoveredNode);
                        return true;
                    }
                }

                if (!hoveredNode.node.isLeaf()) {
                    int iconX = hoveredNode.x - (int) scrollX + NODE_WIDTH - EXPAND_ICON_SIZE - 4;
                    int iconY = nodeScreenY + (NODE_HEIGHT - EXPAND_ICON_SIZE) / 2;
                    if (worldMouseX >= iconX && worldMouseX < iconX + EXPAND_ICON_SIZE
                            && worldMouseY >= iconY && worldMouseY < iconY + EXPAND_ICON_SIZE) {
                        hoveredNode.collapsed = !hoveredNode.collapsed;
                        saveCollapseState();
                        computeLayout();
                        return true;
                    }
                }

                return true;
            }

            // 拖拽开始
            if (mouseY >= CONTROLS_BOTTOM && mouseY <= this.height - TREE_BOTTOM_OFFSET) {
                isDraggingTree = true;
                dragStartX = worldMouseX;
                dragStartY = worldMouseY;
                dragScrollStartX = scrollX;
                dragScrollStartY = scrollY;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (draggingVScrollbar) {
            int treeAreaTop = CONTROLS_BOTTOM;
            int treeAreaBottom = this.height - TREE_BOTTOM_OFFSET;
            int treeAreaHeight = treeAreaBottom - treeAreaTop;
            int trackH = treeAreaHeight;
            int thumbH = Math.max(SCROLLBAR_MIN_THUMB,
                    (int) ((double) treeAreaHeight / (totalTreeHeight + treeAreaHeight) * treeAreaHeight));
            double delta = (event.y() - vScrollbarDragStartY) / (trackH - thumbH) * maxScrollY;
            scrollY = Mth.clamp(vScrollStartValue + delta, 0, maxScrollY);
            return true;
        }
        if (draggingHScrollbar) {
            int trackW = this.width;
            int thumbW = Math.max(SCROLLBAR_MIN_THUMB,
                    (int) ((double) this.width / (totalTreeWidth + this.width) * trackW));
            double delta = (event.x() - hScrollbarDragStartX) / (trackW - thumbW) * (maxScrollX - minScrollX);
            scrollX = Mth.clamp(hScrollStartValue + delta, minScrollX, maxScrollX);
            return true;
        }
        if (isDraggingTree) {
            float zs = currentZoomScale();
            double wx = zs != 1.0F ? event.x() / zs : event.x();
            double wy = zs != 1.0F ? (event.y() - CONTROLS_BOTTOM * (1.0F - zs)) / zs : event.y();
            scrollX = Mth.clamp(dragScrollStartX - ((int) wx - dragStartX), minScrollX, maxScrollX);
            scrollY = Mth.clamp(dragScrollStartY - ((int) wy - dragStartY), 0, maxScrollY);
            return true;
        }
        // 长按放大镜拖动：位移超过阈值进入拖动，实时更新控件位置
        if (searchDragPending && !searchDragging) {
            double dx = event.x() - searchDragMouseDownX;
            double dy = event.y() - searchDragMouseDownY;
            if (dx * dx + dy * dy > DRAG_THRESHOLD) {
                searchDragging = true;
                if (searchField != null) {
                    searchField.setFocused(false);
                }
            }
        }
        if (searchDragging) {
            searchIconX = searchDragStartIconX + (int) (event.x() - searchDragMouseDownX);
            searchIconY = searchDragStartIconY + (int) (event.y() - searchDragMouseDownY);
            clampSearchPosition();
            if (searchField != null) {
                searchField.setX(searchFieldX);
                searchField.setY(searchFieldY);
            }
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingVScrollbar) {
            draggingVScrollbar = false;
            return true;
        }
        if (draggingHScrollbar) {
            draggingHScrollbar = false;
            return true;
        }
        if (isDraggingTree) {
            isDraggingTree = false;
            return true;
        }
        if (searchDragPending || searchDragging) {
            if (!searchDragging) {
                // 未拖动视为点击：切换搜索框显示/隐藏
                searchVisible = !searchVisible;
                if (searchField != null) {
                    if (searchVisible) {
                        searchField.setFocused(true);
                        searchField.moveCursorToEnd(false);
                    } else {
                        searchField.setFocused(false);
                    }
                }
            }
            searchDragPending = false;
            searchDragging = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseY >= CONTROLS_BOTTOM && mouseY <= this.height - TREE_BOTTOM_OFFSET) {
            float zs = currentZoomScale();
            scrollX = Mth.clamp(scrollX - horizontalAmount * 20 / zs, minScrollX, maxScrollX);
            scrollY = Mth.clamp(scrollY - verticalAmount * 20 / zs, 0, maxScrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    // ==================== 键盘事件 ====================

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            if (searchVisible && searchField != null && searchField.isFocused()) {
                searchVisible = false;
                searchField.setFocused(false);
                return true;
            }
            if (depthField != null && depthField.isFocused()) {
                depthField.setFocused(false);
                applyDepthFromField();
                return true;
            }
            saveCollapseState();
            Minecraft.getInstance().setScreenAndShow(parent);
            return true;
        }
        if (event.key() == 257 || event.key() == 335) { // Enter or NumPad Enter
            if (searchVisible && searchField != null && searchField.isFocused()) {
                refreshSearchMatches();
                if (!searchMatches.isEmpty()) {
                    searchIndex = (searchIndex + 1) % searchMatches.size();
                    currentSearchMatch = searchMatches.get(searchIndex);
                    scrollToMatch(currentSearchMatch);
                }
                return true;
            }
            if (depthField != null && depthField.isFocused()) {
                depthField.setFocused(false);
                applyDepthFromField();
                return true;
            }
        }
        if (depthField != null && depthField.isFocused()) {
            if (depthField.keyPressed(event)) {
                return true;
            }
        }
        if (searchVisible && searchField != null && searchField.isFocused()) {
            // Ctrl+V 粘贴：支持把外部（聊天框、文本文件等）的中文/英文直接粘贴进搜索框
            if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_V) {
                long window = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
                boolean ctrl = org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL) == org.lwjgl.glfw.GLFW.GLFW_PRESS
                        || org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
                if (ctrl) {
                    String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
                    if (clip != null && !clip.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (char c : clip.toCharArray()) {
                            if (c >= 32 && c != 127) sb.append(c);
                        }
                        String insert = sb.toString();
                        String before = searchField.getValue();
                        if (!insert.isEmpty() && before.length() < 32) {
                            if (insert.length() > 32 - before.length()) {
                                insert = insert.substring(0, 32 - before.length());
                            }
                            int pos = Math.min(searchField.getCursorPosition(), before.length());
                            String after = before.substring(0, pos) + insert + before.substring(pos);
                            searchField.setValue(after);
                            searchField.setCursorPosition(pos + insert.length());
                        }
                    }
                    return true;
                }
            }
            if (searchField.keyPressed(event)) {
                return true;
            }
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchVisible && searchField != null && searchField.isFocused()) {
            if (searchField.charTyped(event)) {
                return true;
            }
            // 兜底：组件未处理时手动插入可见字符（覆盖中文等输入法提交码点）
            int cp = event.codepoint();
            if (cp >= 32 && cp != 127 && searchField.getValue().length() < 32) {
                String before = searchField.getValue();
                int pos = Math.min(searchField.getCursorPosition(), before.length());
                String after = before.substring(0, pos) + Character.toString(cp) + before.substring(pos);
                searchField.setValue(after);
                searchField.setCursorPosition(pos + 1);
            }
            return true;
        }
        if (depthField != null && depthField.isFocused()) {
            // 只允许数字
            if (Character.isDigit(event.codepoint())) {
                if (depthField.charTyped(event)) {
                    return true;
                }
            }
            return true;
        }
        return super.charTyped(event);
    }

    private void applyDepthFromField() {
        if (depthField == null) return;
        try {
            int val = Integer.parseInt(depthField.getValue().trim());
            val = Mth.clamp(val, MIN_DEPTH, MAX_DEPTH);
            if (val != displayDepth) {
                displayDepth = val;
                depthField.setValue(String.valueOf(displayDepth));
                reanalyze();
            }
        } catch (NumberFormatException e) {
            depthField.setValue(String.valueOf(displayDepth));
        }
    }

    // ==================== JSON导出（跳过折叠节点） ====================

    private void exportTreeAsJson() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date());
        String baseName = schematicName.replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "_");
        String filename = baseName + "-treetable-" + timestamp + ".json";
        Path exportPath = getTreeTableExportPath(filename);

        // 收集折叠节点路径
        Set<String> collapsedPaths = new HashSet<>();
        for (NodeLayout root : rootLayouts) {
            collectCollapsedPaths(root, "", collapsedPaths);
        }

        try {
            java.nio.file.Files.createDirectories(exportPath.getParent());
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(exportPath.toFile()), StandardCharsets.UTF_8))) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonArray rootArray = new JsonArray();
                for (RecipeTreeNode root : treeRoots) {
                    rootArray.add(root.toJson(collapsedPaths));
                }
                String json = gson.toJson(rootArray);
                writer.write(json);
            }
            LitematListMod.LOGGER.info("配方树已导出: {}", exportPath);

            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                Component link = Component.literal(filename);
                Component msg = Component.literal("§a配方树已导出为JSON: ").append(link);
                client.player.sendSystemMessage(msg);
            }
        } catch (Exception e) {
            LitematListMod.LOGGER.error("导出配方树失败", e);
        }
    }

    // ==================== 通用导出（txt/csv） ====================

    /**
     * 收集树状图的叶子节点（完全展开，折叠节点作为叶子）。
     * 每个叶子节点包含：物品ID、数量、配方说明。
     */
    private List<String[]> collectLeafNodes() {
        List<String[]> leaves = new ArrayList<>();
        for (NodeLayout root : rootLayouts) {
            collectLeafNodesRecursive(root, leaves);
        }
        return leaves;
    }

    private void collectLeafNodesRecursive(NodeLayout layout, List<String[]> out) {
        if (layout.collapsed || layout.children.isEmpty()) {
            // 折叠节点或无子节点 = 叶子节点
            var ref = BuiltInRegistries.ITEM.get(Identifier.parse(layout.node.itemId));
            Item item = ref.isPresent() ? ref.get().value() : null;
            String itemName = item != null ? new ItemStack(item).getHoverName().getString() : layout.node.itemId;
            out.add(new String[] {
                layout.node.itemId,
                itemName,
                String.valueOf(layout.node.count),
                layout.node.recipeType != null ? layout.node.recipeType : ""
            });
        } else {
            for (NodeLayout child : layout.children) {
                collectLeafNodesRecursive(child, out);
            }
        }
    }

    /** 导出为 TXT 格式（与 MaterialDetailScreen 格式一致） */
    private void exportAsTxt() {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date());
            String filename = "raw_materials_" + schematicName.replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "_") + "_" + timestamp + ".txt";
            Path exportPath = getRawMaterialExportPath(filename);
            java.nio.file.Files.createDirectories(exportPath.getParent());

            List<String[]> leaves = collectLeafNodes();
            try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(exportPath.toFile()), StandardCharsets.UTF_8))) {
                String title = "原理图'" + schematicName + "'原材料";
                w.write("+---------+-------+---------+");
                w.newLine();
                w.write("| " + padTitle(title, 25) + " |");
                w.newLine();
                w.write("+---------+-------+---------+");
                w.newLine();
                w.write("| Item    | Count | Recipe  |");
                w.newLine();
                w.write("+---------+-------+---------+");
                w.newLine();
                for (String[] leaf : leaves) {
                    String name = leaf[1];
                    if (getDisplayWidth(name) > 9) name = truncateByWidth(name, 9);
                    String recipe = leaf[3];
                    if (recipe.length() > 9) recipe = recipe.substring(0, 9);
                    w.write(String.format("| %-9s | %5s | %-9s |", name, leaf[2], recipe));
                    w.newLine();
                }
                w.write("+---------+-------+---------+");
                w.newLine();
            }

            sendExportMessage("TXT", filename, exportPath);
        } catch (Exception e) {
            LitematListMod.LOGGER.error("导出TXT失败", e);
        }
    }

    /** 导出为 JSON 格式（与 MaterialDetailScreen 格式一致，仅叶子节点） */
    private void exportAsJson() {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date());
            String filename = "raw_materials_" + schematicName.replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "_") + "_" + timestamp + ".json";
            Path exportPath = getRawMaterialExportPath(filename);
            java.nio.file.Files.createDirectories(exportPath.getParent());

            List<String[]> leaves = collectLeafNodes();
            try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(exportPath.toFile()), StandardCharsets.UTF_8))) {
                w.write("{");
                w.newLine();
                w.write("  \"Name\": \"" + schematicName + "\",");
                w.newLine();
                String title = "原理图'" + schematicName + "'原材料";
                w.write("  \"Title\": \"" + title + "\",");
                w.newLine();
                w.write("  \"Multiplier\": 1,");
                w.newLine();
                String dateStr = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", java.util.Locale.ENGLISH).format(new Date());
                w.write("  \"Date\": \"" + dateStr + "\",");
                w.newLine();
                w.write("  \"Items\": [");
                w.newLine();
                boolean first = true;
                for (String[] leaf : leaves) {
                    if (!first) { w.write(","); w.newLine(); }
                    first = false;
                    w.write("    {");
                    w.write("\"Item\": \"" + leaf[0] + "\", ");
                    w.write("\"Name\": \"" + leaf[1] + "\", ");
                    w.write("\"Count\": " + leaf[2] + "");
                    if (!leaf[3].isEmpty()) {
                        w.write(", \"Recipe\": \"" + leaf[3] + "\"");
                    }
                    w.write("}");
                }
                w.newLine();
                w.write("  ]");
                w.newLine();
                w.write("}");
                w.newLine();
            }

            sendExportMessage("JSON", filename, exportPath);
        } catch (Exception e) {
            LitematListMod.LOGGER.error("导出JSON失败", e);
        }
    }

    /** 导出为 CSV 格式（与 MaterialDetailScreen 格式一致） */
    private void exportAsCsv() {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date());
            String filename = "raw_materials_" + schematicName.replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "_") + "_" + timestamp + ".csv";
            Path exportPath = getRawMaterialExportPath(filename);
            java.nio.file.Files.createDirectories(exportPath.getParent());

            List<String[]> leaves = collectLeafNodes();
            try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(exportPath.toFile()), StandardCharsets.UTF_8))) {
                w.write("Item,Name,Count,Recipe");
                w.newLine();
                for (String[] leaf : leaves) {
                    String[] escaped = new String[leaf.length];
                    for (int i = 0; i < leaf.length; i++) {
                        if (leaf[i].contains(",") || leaf[i].contains("\"")) {
                            escaped[i] = "\"" + leaf[i].replace("\"", "\"\"") + "\"";
                        } else {
                            escaped[i] = leaf[i];
                        }
                    }
                    w.write(String.join(",", escaped));
                    w.newLine();
                }
            }

            sendExportMessage("CSV", filename, exportPath);
        } catch (Exception e) {
            LitematListMod.LOGGER.error("导出CSV失败", e);
        }
    }

    /** 发送导出成功消息 */
    private void sendExportMessage(String fmt, String filename, Path exportPath) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            Component link = Component.literal(filename);
            client.player.sendSystemMessage(Component.literal("§a原材料已导出为" + fmt + ": ").append(link));
        }
        LitematListMod.LOGGER.info("原材料已导出为{}: {}", fmt, exportPath);
    }

    private static int getDisplayWidth(String s) {
        int w = 0;
        for (char c : s.toCharArray()) {
            w += (c >= 0x4E00 && c <= 0x9FFF) || (c >= 0x3000 && c <= 0x303F) || (c >= 0xFF00 && c <= 0xFFEF) ? 2 : 1;
        }
        return w;
    }

    private static String truncateByWidth(String s, int maxWidth) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            w += (c >= 0x4E00 && c <= 0x9FFF) || (c >= 0x3000 && c <= 0x303F) || (c >= 0xFF00 && c <= 0xFFEF) ? 2 : 1;
            if (w > maxWidth) return s.substring(0, i);
        }
        return s;
    }

    private static String padTitle(String title, int totalWidth) {
        int w = getDisplayWidth(title);
        if (w >= totalWidth) return title;
        StringBuilder sb = new StringBuilder(title);
        for (int i = 0; i < totalWidth - w; i++) sb.append(' ');
        return sb.toString();
    }

    // ==================== 通配物品替换 ====================

    /**
     * 根据配方类型返回对应的工作站物品图标。
     */
    private static ItemStack getRecipeStationItem(String recipeType) {
        return switch (recipeType) {
            case "crafting_shaped", "crafting_shapeless" -> new ItemStack(Items.CRAFTING_TABLE);
            case "smelting" -> new ItemStack(Items.FURNACE);
            case "stonecutting" -> new ItemStack(Items.STONECUTTER);
            case "smithing" -> new ItemStack(Items.SMITHING_TABLE);
            default -> null;
        };
    }

    /**
     * 打开通配物品替换选择器，允许玩家将通配物品替换为同类物品。
     */
    private void openWildcardReplacement(NodeLayout layout) {
        String itemId = layout.node.itemId;
        String parentId = layout.parentItemId;
        final double savedScrollX = this.scrollX;
        final double savedScrollY = this.scrollY;
        // 收集同类物品（相同物品标签组）
        Set<Item> compatibleItems = getCompatibleItems(itemId);
        goingToSubScreen = true;
        Minecraft.getInstance().setScreenAndShow(
                new BlockPickerScreen(this, selectedItem -> {
                    // 替换节点中的物品ID
                    String newId = BuiltInRegistries.ITEM.getKey(selectedItem).toString();
                    LitematListMod.LOGGER.info("[RawMaterialScreen] 通配替换: {} -> {} (同步={})",
                            itemId, newId, syncReplacementEnabled);
                    if (syncReplacementEnabled) {
                        // 全局同步替换：所有相同子材料都被替换
                        com.litematlist.RawMaterialTreeAnalyzer.setWildcardReplacement(itemId, newId);
                    } else {
                        // 仅当前上下文替换：只替换当前父物品下的子材料
                        String effectiveParentId = parentId != null ? parentId : itemId;
                        com.litematlist.RawMaterialTreeAnalyzer.setContextWildcardReplacement(
                                effectiveParentId, itemId, newId);
                    }
                    // 雕文书架连锁替换：木板半砖和木板必须为相同材质
                    // 检查直接父物品或祖父物品是否为雕纹书架
                    boolean isBookshelfChain = "minecraft:chiseled_bookshelf".equals(parentId);
                    if (!isBookshelfChain && parentId != null) {
                        // 也检查是否为半砖子节点的木板（父物品是半砖，祖父是雕纹书架）
                        String grandparentId = layout.getGrandparentItemId();
                        isBookshelfChain = "minecraft:chiseled_bookshelf".equals(grandparentId);
                    }
                    if (isBookshelfChain) {
                        if (itemId.endsWith("_planks")) {
                            // 替换木板时同步替换对应半砖
                            String linkedSlab = itemId.replace("_planks", "_slab");
                            String newSlab = newId.replace("_planks", "_slab");
                            if (syncReplacementEnabled) {
                                com.litematlist.RawMaterialTreeAnalyzer.setWildcardReplacement(
                                        linkedSlab, newSlab);
                            } else {
                                com.litematlist.RawMaterialTreeAnalyzer.setContextWildcardReplacement(
                                        parentId, linkedSlab, newSlab);
                            }
                        } else if (itemId.endsWith("_slab")) {
                            // 替换半砖时同步替换对应木板
                            String linkedPlank = itemId.replace("_slab", "_planks");
                            String newPlank = newId.replace("_slab", "_planks");
                            if (syncReplacementEnabled) {
                                com.litematlist.RawMaterialTreeAnalyzer.setWildcardReplacement(
                                        linkedPlank, newPlank);
                            } else {
                                com.litematlist.RawMaterialTreeAnalyzer.setContextWildcardReplacement(
                                        parentId, linkedPlank, newPlank);
                            }
                        }
                    }
                    hasPendingChanges = true;
                    // 强制重分析配方树，确保替换立即生效
                    loadOrAnalyze();
                    // 保存滚动位置
                    this.scrollX = Mth.clamp(savedScrollX, this.minScrollX, this.maxScrollX);
                    this.scrollY = Mth.clamp(savedScrollY, 0, this.maxScrollY);
                }, compatibleItems));
    }

    /**
     * 获取与指定物品兼容的同类物品集合（基于物品标签组）。
     * 例如：橡木木板 → 所有木板变种，石剑 → 所有石质工具等。
     */
    private Set<Item> getCompatibleItems(String itemId) {
        Set<Item> collected = new LinkedHashSet<>();
        Item targetItem = BuiltInRegistries.ITEM.getOptional(Identifier.parse(itemId)).orElse(null);
        if (targetItem == null) return collected;

        // 策略1：查找物品所属的标签，收集同标签物品
        var registry = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.registryAccess()
                : null;
        if (registry != null) {
            var itemTags = registry.lookup(net.minecraft.core.registries.Registries.ITEM);
            if (itemTags.isPresent()) {
                var tagRegistry = itemTags.get();
                tagRegistry.listTags().forEach(tag -> {
                    boolean contains = false;
                    for (var entry : tag) {
                        if (entry.value() == targetItem) {
                            contains = true;
                            break;
                        }
                    }
                    if (contains) {
                        for (var entry : tag) {
                            collected.add(entry.value());
                        }
                    }
                });
            }
        }

        // 策略2：如果没有找到同类物品，至少包含目标物品自身
        if (collected.isEmpty()) {
            collected.add(targetItem);
        }

        // 策略2.5：显式添加同类物品（标签查找可能遗漏的变种）
        addExplicitCompatibleItems(collected, itemId);

        // 策略3：按物品类型过滤，避免跨类型混入（如羊毛替换页面显示所有染色物品）
        Set<Item> filtered = filterByItemType(collected, itemId);
        return filtered;
    }

    /**
     * 显式添加同类物品（标签查找可能遗漏的变种）。
     * 例如：圆石和黑石在同一标签，但深板岩圆石可能不在，需要显式添加。
     */
    private void addExplicitCompatibleItems(Set<Item> collected, String itemId) {
        // 圆石类：确保深板岩圆石和黑石都在列表中
        if (itemId.equals("minecraft:cobblestone") || itemId.equals("minecraft:blackstone")
                || itemId.equals("minecraft:cobbled_deepslate")) {
            addItemById(collected, "minecraft:cobblestone");
            addItemById(collected, "minecraft:blackstone");
            addItemById(collected, "minecraft:cobbled_deepslate");
        }
        // 沙子/砂砾
        if (itemId.equals("minecraft:sand") || itemId.equals("minecraft:gravel")) {
            addItemById(collected, "minecraft:sand");
            addItemById(collected, "minecraft:gravel");
        }
        // 煤炭/木炭
        if (itemId.equals("minecraft:coal") || itemId.equals("minecraft:charcoal")) {
            addItemById(collected, "minecraft:coal");
            addItemById(collected, "minecraft:charcoal");
        }
    }

    private void addItemById(Set<Item> collected, String itemId) {
        Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(itemId)).orElse(null);
        if (item != null) {
            collected.add(item);
        }
    }

    /**
     * 按物品类型过滤，只保留与目标物品同类型的变种。
     * 例如：羊毛只保留 _wool 结尾的，木板只保留 _planks 结尾的。
     */
    private Set<Item> filterByItemType(Set<Item> items, String itemId) {
        String typeSuffix = null;
        if (itemId.endsWith("_wool")) typeSuffix = "_wool";
        else if (itemId.endsWith("_planks")) typeSuffix = "_planks";
        else if (itemId.endsWith("_slab")) typeSuffix = "_slab";
        else if (itemId.endsWith("_bed")) typeSuffix = "_bed";
        else if (itemId.endsWith("_leaves") || itemId.equals("minecraft:azalea_leaves")
                || itemId.equals("minecraft:flowering_azalea_leaves")) typeSuffix = "_leaves";
        else if (itemId.startsWith("minecraft:wooden_")) typeSuffix = "wooden_";
        else if (itemId.startsWith("minecraft:stone_")) typeSuffix = "stone_";
        else if (itemId.equals("minecraft:cobblestone") || itemId.equals("minecraft:blackstone")
                || itemId.equals("minecraft:cobbled_deepslate")) typeSuffix = "cobblestone_like";
        else if (itemId.equals("minecraft:sand") || itemId.equals("minecraft:gravel")) typeSuffix = "sand_gravel";

        if (typeSuffix == null) return items; // 不需要过滤

        Set<Item> filtered = new LinkedHashSet<>();
        for (Item item : items) {
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            if (typeSuffix.equals("wooden_") && id.startsWith("minecraft:wooden_")) {
                filtered.add(item);
            } else if (typeSuffix.equals("stone_") && id.startsWith("minecraft:stone_")) {
                filtered.add(item);
            } else if (typeSuffix.equals("cobblestone_like")
                    && (id.equals("minecraft:cobblestone") || id.equals("minecraft:blackstone")
                        || id.equals("minecraft:cobbled_deepslate"))) {
                filtered.add(item);
            } else if (typeSuffix.equals("sand_gravel")
                    && (id.equals("minecraft:sand") || id.equals("minecraft:gravel"))) {
                filtered.add(item);
            } else if (typeSuffix.equals("_leaves") && id.endsWith("_leaves")) {
                filtered.add(item);
            } else if (typeSuffix.startsWith("_") && id.endsWith(typeSuffix)) {
                // 半砖特殊处理：只保留木板半砖（对应木板存在的才是木板半砖）
                if (typeSuffix.equals("_slab")) {
                    String plankId = id.replace("_slab", "_planks");
                    if (BuiltInRegistries.ITEM.getOptional(Identifier.parse(plankId)).isPresent()) {
                        filtered.add(item);
                    }
                } else {
                    filtered.add(item);
                }
            }
        }
        return filtered.isEmpty() ? items : filtered;
    }

    // ==================== 创造模式排序 ====================

    /**
     * 按创造模式物品栏顺序对 treeRoots 排序。
     */
    private void sortByCreativeOrder() {
        Map<String, Integer> sortOrder = buildCreativeSortOrder();
        treeRoots.sort((a, b) -> {
            int orderA = sortOrder.getOrDefault(a.itemId, Integer.MAX_VALUE);
            int orderB = sortOrder.getOrDefault(b.itemId, Integer.MAX_VALUE);
            return Integer.compare(orderA, orderB);
        });
    }

    /**
     * 构建创造模式物品栏排序映射：物品ID → 排序位置。
     */
    private Map<String, Integer> buildCreativeSortOrder() {
        Map<String, Integer> order = new LinkedHashMap<>();
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return order;

        var registry = client.level.registryAccess().lookup(Registries.CREATIVE_MODE_TAB);
        if (registry.isEmpty()) return order;

        int groupIndex = 0;
        for (var group : registry.get()) {
            int itemIndex = 0;
            for (ItemStack stack : group.getDisplayItems()) {
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                if (!order.containsKey(id)) {
                    order.put(id, groupIndex * 10000 + itemIndex);
                }
                itemIndex++;
            }
            groupIndex++;
        }
        return order;
    }

    // ==================== 滚动条 ====================

    /**
     * 渲染垂直和水平滚动条。
     */
    private void renderScrollbars(GuiGraphicsExtractor drawContext, int mouseX, int mouseY) {
        int treeAreaTop = CONTROLS_BOTTOM;
        int treeAreaBottom = this.height - TREE_BOTTOM_OFFSET;
        int treeAreaHeight = treeAreaBottom - treeAreaTop;
        int treeAreaWidth = this.width;

        // ---- 垂直滚动条（右侧） ----
        if (maxScrollY > 0) {
            int trackX = this.width - SCROLLBAR_THICKNESS - 2;
            int trackY = treeAreaTop;
            int trackH = treeAreaHeight;

            // 滚动条轨道
            drawContext.fill(trackX, trackY, trackX + SCROLLBAR_THICKNESS, trackY + trackH, 0x30FFFFFF);

            // 滚动条滑块
            int thumbH = Math.max(SCROLLBAR_MIN_THUMB,
                    (int) ((double) treeAreaHeight / (totalTreeHeight + treeAreaHeight) * treeAreaHeight));
            int thumbY = trackY + (int) ((double) scrollY / maxScrollY * (trackH - thumbH));
            int thumbColor = draggingVScrollbar ? 0xA0FFFFFF : 0x60FFFFFF;
            if (!draggingVScrollbar && mouseX >= trackX && mouseX < trackX + SCROLLBAR_THICKNESS
                    && mouseY >= thumbY && mouseY < thumbY + thumbH) {
                thumbColor = 0x80FFFFFF;
            }
            drawContext.fill(trackX, thumbY, trackX + SCROLLBAR_THICKNESS, thumbY + thumbH, thumbColor);
        }

        // ---- 水平滚动条（底部，4px高，完全贴底） ----
        if (maxScrollX > minScrollX) {
            int hScrollbarHeight = 4;
            int trackY = this.height - hScrollbarHeight;
            int trackX = 0;
            int trackW = treeAreaWidth;

            // 滚动条轨道
            drawContext.fill(trackX, trackY, trackX + trackW, trackY + hScrollbarHeight, 0x30FFFFFF);

            // 滚动条滑块
            int thumbW = Math.max(SCROLLBAR_MIN_THUMB,
                    (int) ((double) treeAreaWidth / (totalTreeWidth + treeAreaWidth) * trackW));
            double range = maxScrollX - minScrollX;
            int thumbX = trackX + (int) ((scrollX - minScrollX) / range * (trackW - thumbW));
            int thumbColor = draggingHScrollbar ? 0xA0FFFFFF : 0x60FFFFFF;
            if (!draggingHScrollbar && mouseX >= thumbX && mouseX < thumbX + thumbW
                    && mouseY >= trackY && mouseY < trackY + hScrollbarHeight) {
                thumbColor = 0x80FFFFFF;
            }
            drawContext.fill(thumbX, trackY, thumbX + thumbW, trackY + hScrollbarHeight, thumbColor);
        }
    }

    // ==================== 节点计数 ====================

    /**
     * 计算树中总物品项数和原材料项数。
     * @return [总物品项数, 原材料项数]
     */
    private int[] countNodes() {
        int totalItems = 0;
        int rawMaterials = 0;
        for (NodeLayout root : rootLayouts) {
            int[] counts = countNodesRecursive(root);
            totalItems += counts[0];
            rawMaterials += counts[1];
        }
        return new int[]{totalItems, rawMaterials};
    }

    private int[] countNodesRecursive(NodeLayout layout) {
        int totalItems = 1;
        int rawMaterials = layout.node.isLeaf() ? 1 : 0;
        if (!layout.collapsed) {
            for (NodeLayout child : layout.children) {
                int[] childCounts = countNodesRecursive(child);
                totalItems += childCounts[0];
                rawMaterials += childCounts[1];
            }
        }
        return new int[]{totalItems, rawMaterials};
    }
}









