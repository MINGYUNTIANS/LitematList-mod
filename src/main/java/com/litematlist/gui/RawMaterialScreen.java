package com.litematlist.gui;

import com.google.gson.*;
import com.litematlist.I18n;
import com.litematlist.LitematListMod;
import com.litematlist.RecipeTreeNode;
import com.litematlist.RawMaterialTreeAnalyzer;
import com.litematlist.LitematicReader.MaterialEntry;
import com.litematlist.gui.MaterialDetailScreen.RawMaterialEntry;
import com.litematlist.MaterialListInjector;
import com.litematlist.gui.MaterialListScreen;
import com.litematlist.PinyinSearch;
import com.litematlist.SyncmaticaBridge;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import com.litematlist.config.Configs;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 鍘熸潗鏂欐í鍚戞爲鐘跺浘鐣岄潰锛氫互閰嶆柟鏍戝舰寮忓睍绀哄師鏉愭枡婧簮缁撴灉銆? * 鏀寔閰嶆柟灞傛暟閫夋嫨銆佽妭鐐规姌鍙?灞曞紑銆丣SON瀵煎嚭銆佸啑浣欏垪琛ㄣ€? */
public class RawMaterialScreen extends GuiBase {

    private final GuiBase parent;
    private final String schematicName;
    private final Path filePath;
    private String pathKey;

    // ---- 鏍戞暟鎹?----
    private List<RecipeTreeNode> treeRoots = new ArrayList<>();
    private boolean initialLoad = true;
    private boolean analyzing = false;

    // ---- 鍐椾綑鏁版嵁 ----
    private List<RawMaterialEntry> redundancyItems = new ArrayList<>();

    // ---- 鏄剧ず灞傛暟 ----
    private int displayDepth = 5;
    private static final int MIN_DEPTH = 0;
    private static final int MAX_DEPTH = 10;

    // ---- 灞傛暟杈撳叆妗?----
    private net.minecraft.client.gui.widget.TextFieldWidget depthField;
    private boolean depthFieldFocused = false;

    // ---- tree view search ----
    private static final Identifier SEARCH_ICON = Identifier.of("litematlist", "textures/gui/search.png");
    private static final int SEARCH_ICON_SIZE = 21;
    private static final int SEARCH_ICON_DEFAULT_Y = 58;
    private static final int SEARCH_FIELD_W = 75;
    private static final int SEARCH_FIELD_H = 16;
    private static final int SEARCH_NAV_BTN_SIZE = 16;
    private static final int DRAG_THRESHOLD = 16;

    // ---- tree view zoom ----
    private static final Identifier ZOOM_IN_ICON = Identifier.of("litematlist", "textures/gui/zoom/zoom_in.png");
    private static final Identifier ZOOM_OUT_ICON = Identifier.of("litematlist", "textures/gui/zoom/zoom_out.png");
    private static final int ZOOM_BTN_SIZE = 16;
    private static final int ZOOM_MIN_PERCENT = 10;
    private static final int ZOOM_MAX_PERCENT = 200;
    private static final int ZOOM_STEP_PERCENT = 10;
    private static final int ZOOM_DEFAULT_PERCENT = 100;
    private boolean treeZoomEnabled = false;
    private static int treeZoomPercent = ZOOM_DEFAULT_PERCENT;
    private int zoomOutBtnX = 0;
    private int zoomInBtnX = 0;
    private int zoomBarY = 0;
    private int frameScreenMouseX = 0;
    private int frameScreenMouseY = 0;
    private boolean searchVisible = false;
    private net.minecraft.client.gui.widget.TextFieldWidget searchField;
    private final List<NodeLayout> searchMatches = new ArrayList<>();
    private int searchIndex = -1;
    private String lastSearchKeyword = "";
    private NodeLayout currentSearchMatch = null;
    private int searchIconX = -1;
    private int searchIconY = SEARCH_ICON_DEFAULT_Y;
    private boolean searchDragPending = false;
    private boolean searchDragging = false;
    private double searchDragMouseDownX = 0;
    private double searchDragMouseDownY = 0;
    private int searchDragStartIconX = 0;
    private int searchDragStartIconY = 0;
    private int searchFieldX = 0;
    private int searchFieldY = 0;
    private int searchNavX = 0;
    private int searchNavY = 0;
    private int searchUpBtnX = 0;
    private int searchDownBtnX = 0;
    private int searchBtnY = 0; // ↑↓按钮实际Y（右侧放不下时下移到信息行下方）

    // ---- 甯冨眬 ----
    private static final int NODE_WIDTH = 112;
    private static final int NODE_HEIGHT = 20;
    private static final int COLUMN_SPACING = 60;
    private static final int VERTICAL_SPACING = 6;
    private static final int TREE_LEFT_MARGIN = 20;
    private static final int CONTROLS_BOTTOM = 62;
    private static final int TREE_BOTTOM_OFFSET = 44;
    private static final int MAX_NAME_CHARS = 6;

    // ---- 婊氬姩 ----
    private double scrollX = 0;
    private double scrollY = 0;
    private double maxScrollX = 0;
    private double minScrollX = 0;
    private double maxScrollY = 0;
    private boolean isDraggingTree = false;
    private double dragStartX = 0;
    private double dragStartY = 0;
    private double dragScrollStartX = 0;
    private double dragScrollStartY = 0;

    // ---- 鑺傜偣甯冨眬缂撳瓨 ----
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

    // ---- 鎮仠 ----
    private NodeLayout hoveredNode = null;

    // ---- 鎶樺彔/灞曞紑鍥炬爣 ----
    private static final Identifier EXPAND_NORMAL = Identifier.of("litematlist", "textures/gui/expand/expand_normal.png");
    private static final Identifier EXPAND_HOVERED = Identifier.of("litematlist", "textures/gui/expand/expand_hovered.png");
    private static final Identifier COLLAPSE_NORMAL = Identifier.of("litematlist", "textures/gui/expand/collapse_normal.png");
    private static final Identifier COLLAPSE_HOVERED = Identifier.of("litematlist", "textures/gui/expand/collapse_hovered.png");
    private static final int EXPAND_ICON_SIZE = 12;

    // ---- 鏇挎崲鍥炬爣锛堥€氶厤鐗╁搧锛?----
    private static final Identifier GEAR_ICON = Identifier.of("litematlist", "textures/gui/gear.png");
    private static final int GEAR_ICON_SIZE = 12;

    // ---- 閰嶆柟鍥炬爣锛堣繛鎺ョ嚎涓婏級 ----
    private static final int RECIPE_ICON_SIZE = 14;
    private NodeLayout hoveredRecipeNode = null; // 榧犳爣鎮仠閰嶆柟鍥炬爣鐨勮妭鐐?
    // ---- 灞傛暟鍔犲噺鍥炬爣 ----
    private static final Identifier DEPTH_NORMAL = Identifier.of("litematlist", "textures/gui/depth/depth_normal.png");
    private static final Identifier DEPTH_HOVERED = Identifier.of("litematlist", "textures/gui/depth/depth_hovered.png");
    private static final Identifier DEPTH_LOCKED = Identifier.of("litematlist", "textures/gui/depth/depth_locked.png");
    private static final int DEPTH_ICON_SIZE = 20;
    private boolean depthIconHovered = false;

    // ---- 鎶樺彔鐘舵€佹寔涔呭寲 ----
    private static final Map<String, Set<String>> COLLAPSED_PATHS = new HashMap<>();

    // ---- 鍒涢€犳ā寮忔帓搴?----
    private static boolean creativeSortEnabled = false;

    // ---- 鍚屾鏇挎崲 ----
    private static boolean syncReplacementEnabled = true;

    // ---- 瀛愮晫闈㈡爣璁帮紙鐢ㄤ簬淇鏇挎崲鍚庢粴鍔ㄤ綅缃級 ----
    private boolean goingToSubScreen = false;

    // ---- 鎸夐挳浣嶇疆锛堢敤浜庢覆鏌撳僵鑹插紑鍏虫枃瀛楋級 ----
    private int creativeSortBtnX = 0;
    private int creativeSortBtnW = 0;
    private int syncReplaceBtnX = 0;
    private int syncReplaceBtnW = 0;
    private int exportRawBtnX = 0;
    private int exportRawBtnW = 0;

    // ---- 寤惰繜鏇存柊 ----
    private boolean hasPendingChanges = false;

    // ---- 婊氬姩鏉?----
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
     * 鍒ゆ柇鏄惁鏉ヨ嚜椤圭洰鏂囦欢澶广€?     * 椤圭洰鏂囦欢璺緞鍦?.minecraft/litematlist/project/<project>/...
     */
    private boolean isFromProjectFolder() {
        if (this.filePath == null) return false;
        // filePath 鏄?.minecraft/litematlist/project/<project>/<file>.litematic
        Path parent = this.filePath.getParent();
        if (parent == null) return false;
        Path grandParent = parent.getParent();
        if (grandParent == null) return false;
        return "project".equals(grandParent.getFileName().toString());
    }

    /** 鑾峰彇椤圭洰鏂囦欢澶瑰悕绉帮紙鐖舵枃浠跺す鍚嶇О锛?*/
    private String getProjectFolderName() {
        if (this.filePath == null || this.filePath.getParent() == null) return "";
        return this.filePath.getParent().getFileName().toString();
    }

    /** 鑾峰彇姝ｇ‘鐨勫鍑鸿矾寰勶紙鍘熸潗鏂欏鍑猴級 */
    private Path getRawMaterialExportPath(String filename) {
        Path mcDir = MinecraftClient.getInstance().runDirectory.toPath().resolve("litematlist");
        if (isFromProjectFolder()) {
            String projectName = getProjectFolderName();
            return mcDir.resolve(projectName).resolve("rawmaterials").resolve(filename);
        } else {
            return mcDir.resolve("rawmaterials").resolve(filename);
        }
    }

    /** 鑾峰彇琛ㄥ紡JSON姝ｇ‘鐨勫鍑鸿矾寰勶紙缁熶竴鏀?treetable 鏂囦欢澶癸級 */
    private Path getTreeTableExportPath(String filename) {
        Path mcDir = MinecraftClient.getInstance().runDirectory.toPath().resolve("litematlist");
        return mcDir.resolve("treetable").resolve(filename);
    }

    @Override
    public void initGui() {
        super.initGui();

        // tree view zoom control toggle (config - general)
        this.treeZoomEnabled = Configs.Generic.TREE_ZOOM_CONTROL.getBooleanValue();

        // ---- 灞傛暟鎺у埗 ----
        // 鏂板竷灞€: 鏍囩 鈫?杈撳叆妗?鈫?[+-]鍥炬爣
        int fieldX = 60;
        int depthCtrlY = 34;

        // 娣卞害杈撳叆妗嗭紙鍦ㄥ浘鏍囧墠闈級
        if (depthField == null) {
            this.depthField = new net.minecraft.client.gui.widget.TextFieldWidget(
                    this.textRenderer, fieldX + 3, depthCtrlY + 1, 24, 14, Text.empty());
            this.depthField.setMaxLength(2);
            this.depthField.setText(String.valueOf(displayDepth));
            this.depthField.setDrawsBackground(false);
            this.addSelectableChild(this.depthField);
        } else {
            this.depthField.setX(fieldX + 3);
            this.depthField.setY(depthCtrlY + 1);
            this.depthField.setText(String.valueOf(displayDepth));
            this.addSelectableChild(this.depthField);
        }

        // ---- tree view search input box (shown after clicking the search icon) ----
        if (searchField == null) {
            this.searchField = new net.minecraft.client.gui.widget.TextFieldWidget(
                    this.textRenderer, 0, 0, SEARCH_FIELD_W, SEARCH_FIELD_H, Text.empty());
            this.searchField.setMaxLength(32);
            this.searchField.setDrawsBackground(false);
            this.searchField.setText("");
            this.addSelectableChild(this.searchField);
        } else {
            this.addSelectableChild(this.searchField);
        }
        computeSearchLayout();
        this.searchField.setX(searchFieldX);
        this.searchField.setY(searchFieldY);
        this.searchField.setVisible(this.searchVisible);
        if (!this.searchVisible) {
            this.searchField.setFocused(false);
        }

        int depthIconX = fieldX + 30; // 鍥炬爣鍦ㄨ緭鍏ユ鍚庨潰

        // ---- 鍒涢€犳ā寮忔帓搴忓紑鍏筹紙鏀惧湪銆?-銆戝拰銆愬鍑恒€戜箣闂达級 ----
        String creativeSortLabel = creativeSortEnabled
                ? I18n.tr("litematlist.button.creative_sort_on")
                : I18n.tr("litematlist.button.creative_sort_off");
        creativeSortBtnW = this.textRenderer.getWidth(creativeSortLabel) + 10;
        creativeSortBtnX = depthIconX + DEPTH_ICON_SIZE + 4;
        // 鎸夐挳鐧藉瓧涓嶆覆鏌?寮€/鍏?锛屽僵鑹叉枃瀛楀崟鐙鐩?
        String creativeSortBtnLabel = creativeSortLabel.substring(0, creativeSortLabel.length() - 1);
        ButtonGeneric creativeSortBtn = new ButtonGeneric(creativeSortBtnX, depthCtrlY,
                creativeSortBtnW, 20, creativeSortBtnLabel);
        creativeSortBtn.setRenderDefaultBackground(true);
        this.addButton(creativeSortBtn, (IButtonActionListener) (b, mb) -> {
            creativeSortEnabled = !creativeSortEnabled;
            hasPendingChanges = true;
            initGui();
        });

        // ---- 鏄惁鍚屾鏇挎崲寮€鍏筹紙榛樿寮€鍚級 ----
        String syncReplaceLabel = syncReplacementEnabled
                ? I18n.tr("litematlist.button.sync_replace_on")
                : I18n.tr("litematlist.button.sync_replace_off");
        syncReplaceBtnW = this.textRenderer.getWidth(syncReplaceLabel) + 10;
        syncReplaceBtnX = creativeSortBtnX + creativeSortBtnW + 4;
        // 鎸夐挳鐧藉瓧涓嶆覆鏌?寮€/鍏?锛屽僵鑹叉枃瀛楀崟鐙鐩?
        String syncReplaceBtnLabel = syncReplaceLabel.substring(0, syncReplaceLabel.length() - 1);
        ButtonGeneric syncReplaceBtn = new ButtonGeneric(syncReplaceBtnX, depthCtrlY,
                syncReplaceBtnW, 20, syncReplaceBtnLabel);
        syncReplaceBtn.setRenderDefaultBackground(true);
        this.addButton(syncReplaceBtn, (IButtonActionListener) (b, mb) -> {
            syncReplacementEnabled = !syncReplacementEnabled;
            initGui();
        });

        // ---- 瀵煎嚭琛ㄥ紡JSON鎸夐挳锛堝悓姝ユ浛鎹㈠彸渚э級 ----
        String exportTableJsonLabel = I18n.tr("litematlist.button.export_table_json");
        int exportTableJsonBtnW = this.textRenderer.getWidth(exportTableJsonLabel) + 10;
        ButtonGeneric exportJsonBtn = new ButtonGeneric(syncReplaceBtnX + syncReplaceBtnW + 4, depthCtrlY,
                exportTableJsonBtnW, 20, exportTableJsonLabel);
        exportJsonBtn.setRenderDefaultBackground(true);
        this.addButton(exportJsonBtn, (IButtonActionListener) (b, mb) -> exportTreeAsJson());

        // ---- 閲嶆柊鍒嗘瀽鎸夐挳锛堝鍑鸿〃寮廕SON鍙充晶锛?----
        String reanalyzeLabel = I18n.tr("litematlist.button.reanalyze");
        int reanalyzeBtnW = this.textRenderer.getWidth(reanalyzeLabel) + 10;
        int reanalyzeBtnX = syncReplaceBtnX + syncReplaceBtnW + 4 + exportTableJsonBtnW + 4;
        ButtonGeneric reanalyzeBtn = new ButtonGeneric(reanalyzeBtnX, depthCtrlY,
                reanalyzeBtnW, 20, reanalyzeLabel);
        reanalyzeBtn.setRenderDefaultBackground(true);
        this.addButton(reanalyzeBtn, (IButtonActionListener) (b, mb) -> {
            initialLoad = true;
            initGui();
        });

        // ---- 瀵煎嚭鎸夐挳锛堥噸鏂板垎鏋愬彸渚э級锛氶粯璁ゅ鍑簍xt锛宻hift瀵煎嚭csv锛宎lt瀵煎嚭json ----
        String exportLabel = I18n.tr("litematlist.button.export_raw");
        int exportBtnW = this.textRenderer.getWidth(exportLabel) + 10;
        int exportRawBtnX = reanalyzeBtnX + reanalyzeBtnW + 4;
        ButtonGeneric exportBtn = new ButtonGeneric(exportRawBtnX, depthCtrlY,
                exportBtnW, 20, exportLabel);
        this.exportRawBtnX = exportRawBtnX;
        this.exportRawBtnW = exportBtnW;
        exportBtn.setRenderDefaultBackground(true);
        this.addButton(exportBtn, (IButtonActionListener) (b, mb) -> {
            long window = MinecraftClient.getInstance().getWindow().getHandle();
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

        // ---- 鍐椾綑鍒楄〃鎸夐挳锛堟渶鍙充晶锛?----
        // ---- 原材料配置按钮（冗余列表按钮上方） ----
        String rawConfigLabel = I18n.tr("litematlist.button.raw_material_config");
        int rawConfigBtnW = this.textRenderer.getWidth(rawConfigLabel) + 16;
        ButtonGeneric rawConfigBtn = new ButtonGeneric(this.width - rawConfigBtnW - 20, depthCtrlY - 24,
                rawConfigBtnW, 20, rawConfigLabel);
        rawConfigBtn.setRenderDefaultBackground(true);
        this.addButton(rawConfigBtn, (IButtonActionListener) (b, mb) -> {
            MinecraftClient.getInstance().setScreen(new RawMaterialConfigScreen(this));
        });

        String redundancyLabel = I18n.tr("litematlist.button.redundancy_list");
        int redundancyBtnW = this.textRenderer.getWidth(redundancyLabel) + 16;
        ButtonGeneric redundancyBtn = new ButtonGeneric(this.width - redundancyBtnW - 20, depthCtrlY,
                redundancyBtnW, 20, redundancyLabel);
        redundancyBtn.setRenderDefaultBackground(true);
        this.addButton(redundancyBtn, (IButtonActionListener) (b, mb) -> {
            MaterialDetailScreen.rawMaterialRedundancy.put(pathKey,
                    new ArrayList<>(redundancyItems));
            MaterialDetailScreen.savePersistence();
            MinecraftClient.getInstance().setScreen(
                    new RedundancyListScreen(this, schematicName, filePath));
        });

        // ---- 搴曢儴杩斿洖鎸夐挳 ----
        int buttonY = this.height - 38;
        ButtonGeneric backBtn = new ButtonGeneric(this.width / 2 - 60, buttonY, 120, 20, I18n.tr("litematlist.button.back"));
        backBtn.setRenderDefaultBackground(true);
        this.addButton(backBtn, (IButtonActionListener) (b, mb) -> {
            saveCollapseState();
            MinecraftClient.getInstance().setScreen(parent);
        });

        if (goingToSubScreen) {
            goingToSubScreen = false;
            //  this.scrollX;
            double savedSX = this.scrollX;
            double savedSY = this.scrollY;
            loadOrAnalyze();
            this.scrollX = MathHelper.clamp(savedSX, this.minScrollX, this.maxScrollX);
            this.scrollY = Math.min(savedSY, this.maxScrollY);
        } else if (!analyzing) {
            loadOrAnalyze();
        }
    }

    @Override
    public void removed() {
        saveCollapseState();
        if (!goingToSubScreen) {
            // 鐪熸绂诲紑鐣岄潰鏃舵爣璁伴渶瑕侀噸鏂板姞杞斤紝纭繚鏉愭枡鍒楄〃鍙樺姩鍚庡悓姝?
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

    /** 渚?IgnoredItemsScreen 鍥炶皟 */
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

        Set<net.minecraft.item.Item> materialIgnored = filePath != null
                ? MaterialDetailScreen.getIgnoredForPath(filePath) : Set.of();

        List<MaterialEntry> entries = null;
        if (filePath != null) {
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
        } else {
            entries = loadProjectEntries(schematicName);
        }

        // 鍏堝簲鐢?MaterialDetailScreen 涓繚瀛樼殑鏇挎崲鏄犲皠锛岀‘淇濇浛鎹㈠悗鐨勭墿鍝佷篃鑳借蹇界暐杩囨护鍖归厤
        if (filePath != null && entries != null) {
            entries = entries.stream()
                    .map(e -> {
                        if (e.item() == null) return e;
                        Item replaced = MaterialDetailScreen.getReplacement(filePath, e.item());
                        if (replaced != null && replaced != e.item()) {
                            return new MaterialEntry(replaced, e.blockName(), e.totalCount());
                        }
                        return e;
                    })
                    .collect(Collectors.toList());
        }

        // 鍐嶅簲鐢ㄥ拷鐣ヨ繃婊わ紙鏇挎崲鍚庢墽琛岋紝纭繚鏇挎崲鍚庣殑鐗╁搧涔熻兘琚纭繃婊わ級
        if (entries != null && !materialIgnored.isEmpty()) {
            entries = entries.stream()
                    .filter(e -> e.item() != null && !materialIgnored.contains(e.item()))
                    .collect(Collectors.toList());
        }

        if (entries != null && !entries.isEmpty()) {
            analyzing = true;
            // 鍦ㄤ富绾跨▼鎵ц鍒嗘瀽锛屽洜涓篟ecipeBookUtils闇€瑕佽闂鎴风鏁版嵁
            RawMaterialTreeAnalyzer analyzer = new RawMaterialTreeAnalyzer();
            analyzer.setMaxDepth(displayDepth);
            try {
                RawMaterialTreeAnalyzer.TreeResult result = analyzer.analyze(entries);
                treeRoots = result.roots();
                redundancyItems = result.redundancy();
                if (creativeSortEnabled) {
                    sortByCreativeOrder();
                }
                computeLayout();
            } finally {
                analyzing = false;
            }
        }

        if (initialLoad) {
            initialLoad = false;
        } else {
            // 闈為娆″姞杞芥椂鎭㈠婊氬姩浣嶇疆
            this.scrollX = MathHelper.clamp(savedScrollX, minScrollX, maxScrollX);
            this.scrollY = MathHelper.clamp(savedScrollY, 0, maxScrollY);
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
                    String key = Registries.ITEM.getId(effectiveItem).toString();
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

    // ==================== 甯冨眬璁＄畻锛堢函鍨傜洿鍫嗗彔锛屾棤鏂滅巼锛?====================

    private void computeLayout() {
        rootLayouts.clear();
        if (treeRoots.isEmpty()) return;

        // 鑾峰彇宸蹭繚瀛樼殑鎶樺彔璺緞
        Set<String> collapsed = COLLAPSED_PATHS.get(pathKey);
        if (collapsed == null) collapsed = Collections.emptySet();

        // 绗竴闃舵锛氶€掑綊璁＄畻姣忎釜鑺傜偣鐨剆ubtreeHeight锛堟姌鍙犺妭鐐归珮搴︿负NODE_HEIGHT锛?
        for (RecipeTreeNode root : treeRoots) {
            NodeLayout layout = computeNodeLayout(root, 0, "", collapsed);
            rootLayouts.add(layout);
        }

        // 绗簩闃舵锛氫粠涓婂埌涓嬪垎閰嶇粷瀵逛綅缃紝瀛愯妭鐐瑰瀭鐩村爢鍙?
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

        // max scroll ranges follow the current zoom level (or flat layout when zoom disabled)
        updateMaxScroll();
    }

    /**
     * 閫掑綊璁＄畻鑺傜偣甯冨眬锛氬瓙鑺傜偣鍨傜洿鍫嗗彔锛屾姌鍙犺妭鐐圭殑subtreeHeight涓篘ODE_HEIGHT銆?     */
    private NodeLayout computeNodeLayout(RecipeTreeNode node, int depth, String path, Set<String> collapsed) {
        NodeLayout layout = new NodeLayout();
        layout.node = node;

        String currentPath = path.isEmpty() ? node.itemId : path + ">" + node.itemId;

        if (collapsed.contains(currentPath)) {
            // 鎶樺彔鐨勮妭鐐癸細楂樺害浠呬负NODE_HEIGHT锛屼笉灞曞紑瀛愯妭鐐?
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
     * 鍒嗛厤缁濆浣嶇疆锛氬瓙鑺傜偣浠庣埗鑺傜偣subtree椤堕儴寮€濮嬪瀭鐩村爢鍙犮€?     */
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

    // ==================== tree search ====================

    private void computeSearchLayout() {
        if (searchIconX < 0) {
            searchIconX = Math.max(2, this.width - SCROLLBAR_THICKNESS - 4 - SEARCH_ICON_SIZE);
        }
        if (searchIconX + SEARCH_ICON_SIZE + 6 + SEARCH_FIELD_W + 2 <= this.width - 2) {
            searchFieldX = searchIconX + SEARCH_ICON_SIZE + 6;
            searchFieldY = searchIconY + (SEARCH_ICON_SIZE - SEARCH_FIELD_H) / 2;
        } else {
            searchFieldX = MathHelper.clamp(searchIconX, 0,
                    Math.max(0, this.width - SEARCH_FIELD_W - 2));
            searchFieldY = searchIconY + SEARCH_ICON_SIZE + 4;
        }
        searchNavY = Math.max(searchIconY + SEARCH_ICON_SIZE + 4, searchFieldY + SEARCH_FIELD_H + 4);
        searchNavX = searchFieldX;
    }

    private void clampSearchPosition() {
        int minX = 2;
        int maxX = this.width - SCROLLBAR_THICKNESS - 4 - SEARCH_ICON_SIZE;
        searchIconX = MathHelper.clamp(searchIconX, minX, Math.max(minX, maxX));
        int minY = CONTROLS_BOTTOM;
        int maxY = (this.height - TREE_BOTTOM_OFFSET) - SEARCH_ICON_SIZE;
        searchIconY = MathHelper.clamp(searchIconY, minY, Math.max(minY, maxY));
        computeSearchLayout();
    }

    // ==================== tree zoom ====================

    private float currentZoomScale() {
        return this.treeZoomEnabled ? treeZoomPercent / 100.0F : 1.0F;
    }

    public static int getTreeZoomPercent() {
        return treeZoomPercent;
    }

    public static void setTreeZoomPercent(int percent) {
        int clamped = Math.max(ZOOM_MIN_PERCENT, Math.min(ZOOM_MAX_PERCENT, percent));
        int steps = (clamped - ZOOM_MIN_PERCENT + ZOOM_STEP_PERCENT / 2) / ZOOM_STEP_PERCENT;
        treeZoomPercent = Math.min(ZOOM_MAX_PERCENT, ZOOM_MIN_PERCENT + steps * ZOOM_STEP_PERCENT);
    }

    public static void resetTreeZoomPercent() {
        treeZoomPercent = ZOOM_DEFAULT_PERCENT;
    }

    private int cullRightW() {
        float zs = currentZoomScale();
        return zs != 1.0F ? (int) (this.width / zs) : this.width;
    }

    private int cullTopW() {
        return CONTROLS_BOTTOM;
    }

    private int cullBottomW() {
        float zs = currentZoomScale();
        int bottom = this.height - TREE_BOTTOM_OFFSET;
        return zs != 1.0F ? Math.max(cullTopW() + 1, (int) ((bottom - CONTROLS_BOTTOM * (1.0F - zs)) / zs)) : bottom;
    }

    private void updateMaxScroll() {
        float zs = currentZoomScale();
        if (this.treeZoomEnabled) {
            int viewW = Math.max(1, (int) (this.width / zs));
            double a = TREE_LEFT_MARGIN + totalTreeWidth - viewW;
            double b = TREE_LEFT_MARGIN;
            minScrollX = Math.min(a, b);
            maxScrollX = Math.max(a, b);
        } else {
            int visibleWidth = Math.max(1, (int) ((this.width - TREE_LEFT_MARGIN) / zs));
            minScrollX = 0;
            maxScrollX = Math.max(0, totalTreeWidth - visibleWidth);
        }
        int visibleHeight = Math.max(1, (int) ((this.height - CONTROLS_BOTTOM - TREE_BOTTOM_OFFSET) / zs));
        maxScrollY = Math.max(0, totalTreeHeight - visibleHeight);
        scrollX = MathHelper.clamp(scrollX, minScrollX, maxScrollX);
        scrollY = MathHelper.clamp(scrollY, 0, maxScrollY);
    }

    private void drawTooltipFixed(DrawContext ctx, Text text) {
        float zs = currentZoomScale();
        if (zs != 1.0F) {
            ctx.getMatrices().push();
            ctx.getMatrices().translate(0.0D, (double) (-CONTROLS_BOTTOM * (1.0F - zs) / zs), 0.0D);
            ctx.getMatrices().scale(1.0F / zs, 1.0F / zs, 1.0F);
            ctx.drawTooltip(this.textRenderer, text, frameScreenMouseX, frameScreenMouseY);
            ctx.getMatrices().pop();
        } else {
            ctx.drawTooltip(this.textRenderer, text, frameScreenMouseX, frameScreenMouseY);
        }
    }

    private void computeZoomBarLayout() {
        int pctW = this.textRenderer.getWidth("200%");
        int totalW = ZOOM_BTN_SIZE * 2 + 12 + pctW;
        zoomOutBtnX = this.width - 8 - totalW;
        zoomInBtnX = zoomOutBtnX + ZOOM_BTN_SIZE + 12 + pctW;
        zoomBarY = this.height - TREE_BOTTOM_OFFSET - 2;
    }

    private void drawZoomBar(DrawContext ctx, int mouseX, int mouseY) {
        computeZoomBarLayout();
        String pct = treeZoomPercent + "%";
        int pctW = this.textRenderer.getWidth(pct);
        int pctX = (zoomOutBtnX + ZOOM_BTN_SIZE + zoomInBtnX) / 2 - pctW / 2;
        ctx.drawText(this.textRenderer, pct, pctX, zoomBarY + (ZOOM_BTN_SIZE - 8) / 2, 0xFFFFFFFF, false);

        boolean outHover = mouseX >= zoomOutBtnX - 2 && mouseX < zoomOutBtnX + ZOOM_BTN_SIZE + 2
                && mouseY >= zoomBarY - 2 && mouseY < zoomBarY + ZOOM_BTN_SIZE + 2;
        if (outHover) {
            ctx.fill(zoomOutBtnX - 2, zoomBarY - 2, zoomOutBtnX + ZOOM_BTN_SIZE + 2,
                    zoomBarY + ZOOM_BTN_SIZE + 2, 0x40FFFFFF);
        }
        ctx.drawTexture(ZOOM_OUT_ICON, zoomOutBtnX, zoomBarY,
                0.0f, 0.0f, ZOOM_BTN_SIZE, ZOOM_BTN_SIZE, ZOOM_BTN_SIZE, ZOOM_BTN_SIZE);

        boolean inHover = mouseX >= zoomInBtnX - 2 && mouseX < zoomInBtnX + ZOOM_BTN_SIZE + 2
                && mouseY >= zoomBarY - 2 && mouseY < zoomBarY + ZOOM_BTN_SIZE + 2;
        if (inHover) {
            ctx.fill(zoomInBtnX - 2, zoomBarY - 2, zoomInBtnX + ZOOM_BTN_SIZE + 2,
                    zoomBarY + ZOOM_BTN_SIZE + 2, 0x40FFFFFF);
        }
        ctx.drawTexture(ZOOM_IN_ICON, zoomInBtnX, zoomBarY,
                0.0f, 0.0f, ZOOM_BTN_SIZE, ZOOM_BTN_SIZE, ZOOM_BTN_SIZE, ZOOM_BTN_SIZE);
    }

    private void drawSearchNavButton(DrawContext ctx, int bx, int by, String arrow,
                                     int mouseX, int mouseY) {
        boolean hovered = mouseX >= bx && mouseX < bx + SEARCH_NAV_BTN_SIZE
                && mouseY >= by && mouseY < by + SEARCH_NAV_BTN_SIZE;
        int bg = hovered ? 0xFF555555 : 0xFF303030;
        int border = hovered ? 0xFFCCCCCC : 0xFF888888;
        ctx.fill(bx, by, bx + SEARCH_NAV_BTN_SIZE, by + SEARCH_NAV_BTN_SIZE, bg);
        ctx.fill(bx, by, bx + SEARCH_NAV_BTN_SIZE, by + 1, border);
        ctx.fill(bx, by + SEARCH_NAV_BTN_SIZE - 1, bx + SEARCH_NAV_BTN_SIZE, by + SEARCH_NAV_BTN_SIZE, border);
        ctx.fill(bx, by, bx + 1, by + SEARCH_NAV_BTN_SIZE, border);
        ctx.fill(bx + SEARCH_NAV_BTN_SIZE - 1, by, bx + SEARCH_NAV_BTN_SIZE, by + SEARCH_NAV_BTN_SIZE, border);
        ctx.drawCenteredTextWithShadow(this.textRenderer, arrow, bx + SEARCH_NAV_BTN_SIZE / 2,
                by + (SEARCH_NAV_BTN_SIZE - 8) / 2, 0xFFFFFFFF);
    }

    private void refreshSearchMatches() {
        if (searchField == null) {
            searchMatches.clear();
            searchIndex = -1;
            currentSearchMatch = null;
            return;
        }
        String kw = searchField.getText().trim().toLowerCase(Locale.ROOT);
        boolean kwChanged = !kw.equals(lastSearchKeyword);
        lastSearchKeyword = kw;

        List<NodeLayout> matches = new ArrayList<>();
        if (searchVisible && !kw.isEmpty()) {
            List<NodeLayout> allNodes = new ArrayList<>();
            for (NodeLayout root : rootLayouts) {
                collectVisibleNodes(root, allNodes);
            }
            allNodes.sort((a, b) -> a.x != b.x
                    ? Integer.compare(a.x, b.x)
                    : Integer.compare(a.y, b.y));
            boolean numericKw = kw.matches("\\d+");
            for (NodeLayout node : allNodes) {
                boolean hit;
                if (numericKw) {
                    hit = String.valueOf(node.node.count).contains(kw);
                } else {
                    String name = node.node.itemId;
                    Item item = node.node.getItem();
                    if (item != null) name = item.getName().getString();
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

    private void scrollToMatch(NodeLayout match) {
        int sx = match.x - (int) scrollX;
        int sy = match.y - (int) scrollY - NODE_HEIGHT / 2;
        int treeTop = cullTopW();
        int treeBottom = cullBottomW();
        int treeRight = cullRightW();
        if (sx < TREE_LEFT_MARGIN || sx + NODE_WIDTH > treeRight - TREE_LEFT_MARGIN) {
            scrollX = MathHelper.clamp(match.x - treeRight / 2, minScrollX, maxScrollX);
        }
        if (sy < treeTop || sy + NODE_HEIGHT > treeBottom) {
            scrollY = MathHelper.clamp(match.y - treeTop
                    - (treeBottom - treeTop) / 2, 0, maxScrollY);
        }
    }

    // ==================== 娓叉煋 ====================

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        this.renderBackground(drawContext, mouseX, mouseY, delta);

        refreshSearchMatches();

        // 鎸夐挳
        super.render(drawContext, mouseX, mouseY, delta);

        // ---- 娓叉煋寮€鍏虫寜閽笂鐨?寮€/鍏?褰╄壊鏂囧瓧 ----
        // 鍒涢€犳ā寮忔帓搴忔寜閽?
        {
            String fullLabel = creativeSortEnabled
                    ? I18n.tr("litematlist.button.creative_sort_on")
                    : I18n.tr("litematlist.button.creative_sort_off");
            String prefix = fullLabel.substring(0, fullLabel.length() - 1);
            String state = fullLabel.substring(fullLabel.length() - 1);
            int prefixW = this.textRenderer.getWidth(prefix);
            int stateColor = creativeSortEnabled ? 0xFF55FF55 : 0xFFFF5555;
            int textStartX = creativeSortBtnX + (creativeSortBtnW - this.textRenderer.getWidth(fullLabel)) / 2;
            drawContext.drawText(this.textRenderer, state, textStartX + prefixW, 39, stateColor, false);
        }
        // 鏄惁鍚屾鏇挎崲鎸夐挳
        {
            String fullLabel = syncReplacementEnabled
                    ? I18n.tr("litematlist.button.sync_replace_on")
                    : I18n.tr("litematlist.button.sync_replace_off");
            String prefix = fullLabel.substring(0, fullLabel.length() - 1);
            String state = fullLabel.substring(fullLabel.length() - 1);
            int prefixW = this.textRenderer.getWidth(prefix);
            int stateColor = syncReplacementEnabled ? 0xFF55FF55 : 0xFFFF5555;
            int textStartX = syncReplaceBtnX + (syncReplaceBtnW - this.textRenderer.getWidth(fullLabel)) / 2;
            drawContext.drawText(this.textRenderer, state, textStartX + prefixW, 39, stateColor, false);
        }

        // ---- tree view search magnifier icon: render before analyzing check ----
        computeSearchLayout();
        boolean searchIconHovered = mouseX >= searchIconX && mouseX < searchIconX + SEARCH_ICON_SIZE
                && mouseY >= searchIconY && mouseY < searchIconY + SEARCH_ICON_SIZE;
        if (searchIconHovered) {
            drawContext.fill(searchIconX - 1, searchIconY - 1,
                    searchIconX + SEARCH_ICON_SIZE + 1, searchIconY + SEARCH_ICON_SIZE + 1, 0x40FFFFFF);
        }
        drawContext.drawTexture(SEARCH_ICON,
                searchIconX, searchIconY, 0.0f, 0.0f, SEARCH_ICON_SIZE, SEARCH_ICON_SIZE,
                SEARCH_ICON_SIZE, SEARCH_ICON_SIZE);

        if (analyzing) {
            drawContext.drawCenteredTextWithShadow(this.textRenderer,
                    I18n.tr("litematlist.raw_materials.analyzing"),
                    this.width / 2, (this.height - 100) / 2, 0xFFFFFF55);
            return;
        }

        if (treeRoots.isEmpty()) {
            drawContext.drawCenteredTextWithShadow(this.textRenderer,
                    I18n.tr("litematlist.raw_materials.empty"),
                    this.width / 2, (this.height - 100) / 2, 0x80FFFFFF);
        } else {
            // 娓叉煋鏍?
            hoveredNode = null;
            hoveredRecipeNode = null;
            this.frameScreenMouseX = mouseX;
            this.frameScreenMouseY = mouseY;
            float zs = currentZoomScale();
            boolean zoomed = zs != 1.0F;
            float zsOffsetY = zoomed ? CONTROLS_BOTTOM * (1.0F - zs) : 0.0F;
            int wMouseX = zoomed ? (int) (mouseX / zs) : mouseX;
            int wMouseY = zoomed ? (int) ((mouseY - zsOffsetY) / zs) : mouseY;
            int treeAreaBottom = cullBottomW();

            // 缁樺埗杩炴帴绾?
            if (zoomed) {
                drawContext.getMatrices().push();
                drawContext.getMatrices().translate(0.0D, (double) zsOffsetY, 0.0D);
                drawContext.getMatrices().scale(zs, zs, 1.0F);
            }

            for (NodeLayout root : rootLayouts) {
                renderConnectors(drawContext, root, wMouseX, wMouseY);
            }

            // 缁樺埗鑺傜偣
            for (NodeLayout root : rootLayouts) {
                renderNodeTree(drawContext, root, wMouseX, wMouseY, treeAreaBottom);
            }

            if (zoomed) {
                drawContext.getMatrices().pop();
            }

            // 閰嶆柟鍥炬爣鎮仠鎻愮ず
            if (hoveredRecipeNode != null && hoveredNode == null) {
                RecipeTreeNode rn = hoveredRecipeNode.node;
                if (rn.alternativeRecipeTypes != null && !rn.alternativeRecipeTypes.isEmpty()) {
                    drawContext.drawTooltip(this.textRenderer,
                            Text.literal(I18n.tr("litematlist.recipe.alt_available")),
                            mouseX, mouseY);
                }
            }

            // 瀵煎嚭鎸夐挳鎮仠鎻愮ず锛堝琛岋級
            if (mouseX >= exportRawBtnX && mouseX < exportRawBtnX + exportRawBtnW
                    && mouseY >= 34 && mouseY < 54) {
                String[] lines = I18n.tr("litematlist.button.export_raw_tip").split("\n");
                java.util.List<Text> tooltipLines = new java.util.ArrayList<>();
                for (String line : lines) {
                    tooltipLines.add(Text.literal(line));
                }
                drawContext.drawTooltip(this.textRenderer, tooltipLines, mouseX, mouseY);
            }
        }

        // ---- 灞傛暟鎺у埗娓叉煋锛堟渶楂樺浘灞傦紝绾櫧鑹诧紝鍦ㄦ爲涔嬩笂娓叉煋锛?----
        // 鏂板竷灞€: 鏍囩 鈫?杈撳叆妗?鈫?[+-]鍥炬爣
        int fieldX = 60;
        int depthCtrlY = 34;

        // "灞傛暟:" 鏍囩锛堢函鐧借壊锛屾棤闃村奖锛?
        String depthLabel = I18n.tr("litematlist.tree.depth_label");
        drawContext.drawText(this.textRenderer, depthLabel, 18, depthCtrlY + 4, 0xFFFFFFFF, false);

        // 杈撳叆妗嗗尯鍩燂紙鍏堟覆鏌擄紝鍦ㄥ浘鏍囧乏杈癸級
        drawContext.fill(fieldX - 2, depthCtrlY, fieldX + 26, depthCtrlY + 18, 0xFFFFFFFF);
        drawContext.fill(fieldX - 1, depthCtrlY + 1, fieldX + 25, depthCtrlY + 17, 0xFF111111);
        if (depthField != null) {
            String txt = depthField.getText();
            drawContext.drawText(this.textRenderer, txt, fieldX + 3, depthCtrlY + 4, 0xFFFFFFFF, false);
            if (depthField.isFocused() && System.currentTimeMillis() % 1000 < 500) {
                int cp = depthField.getCursor();
                String bc = txt.substring(0, Math.min(cp, txt.length()));
                int cx = fieldX + 3 + this.textRenderer.getWidth(bc);
                drawContext.fill(cx, depthCtrlY + 3, cx + 1, depthCtrlY + 15, 0xFFFFFFFF);
            }
        }

        // 灞傛暟鍔犲噺鍥炬爣锛堝湪杈撳叆妗嗗悗闈級
        int depthIconX = fieldX + 30;
        depthIconHovered = mouseX >= depthIconX && mouseX < depthIconX + DEPTH_ICON_SIZE
                && mouseY >= depthCtrlY && mouseY < depthCtrlY + DEPTH_ICON_SIZE;
        Identifier depthIcon = depthIconHovered ? DEPTH_HOVERED : DEPTH_NORMAL;
        drawContext.drawTexture(depthIcon, depthIconX, depthCtrlY,
                0.0f, 0.0f, DEPTH_ICON_SIZE, DEPTH_ICON_SIZE, DEPTH_ICON_SIZE, DEPTH_ICON_SIZE);

        // ---- tree view search controls ----
        if (this.searchVisible) {
            drawContext.fill(searchFieldX - 2, searchFieldY - 1,
                    searchFieldX + SEARCH_FIELD_W + 2, searchFieldY + SEARCH_FIELD_H + 1, 0xFFFFFFFF);
            drawContext.fill(searchFieldX - 1, searchFieldY,
                    searchFieldX + SEARCH_FIELD_W + 1, searchFieldY + SEARCH_FIELD_H, 0xFF111111);
            if (searchField != null) {
                String txt = searchField.getText();
                String shown = txt;
                int availW = SEARCH_FIELD_W - 8;
                if (this.textRenderer.getWidth(txt) > availW) {
                    shown = this.textRenderer.trimToWidth(txt, availW - this.textRenderer.getWidth("\u2026")) + "\u2026";
                }
                drawContext.drawText(this.textRenderer, shown, searchFieldX + 3, searchFieldY + 3, 0xFFFFFFFF, false);
                if (searchField.isFocused() && System.currentTimeMillis() % 1000 < 500) {
                    int cp = Math.min(searchField.getCursor(), txt.length());
                    String bc = txt.substring(0, cp);
                    int cx = searchFieldX + 3 + this.textRenderer.getWidth(bc);
                    drawContext.fill(cx, searchFieldY + 2, cx + 1, searchFieldY + SEARCH_FIELD_H - 2, 0xFFFFFFFF);
                }
            }

            int total = searchMatches.size();
            String info = total > 0
                    ? "\u5171" + total + "\u4e2a\u5339\u914d\u9879," + (searchIndex + 1) + "/" + total
                    : "\u51710\u4e2a\u5339\u914d\u9879";
            int infoW = this.textRenderer.getWidth(info);
            drawContext.drawText(this.textRenderer, info, searchNavX, searchNavY + 4, 0xFFFFFFFF, false);

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
                drawSearchNavButton(drawContext, searchUpBtnX, searchBtnY, "\u2191", mouseX, mouseY);
                drawSearchNavButton(drawContext, searchDownBtnX, searchBtnY, "\u2193", mouseX, mouseY);
            }
        }

        if (this.treeZoomEnabled) {
            drawZoomBar(drawContext, mouseX, mouseY);
        }

        // 鍙充笅瑙掓枩浣撴彁绀猴細鐣岄潰鍙敤榧犳爣鎷栧姩
        String dragHint = I18n.tr("litematlist.raw_materials.drag_hint");
        int hintW = this.textRenderer.getWidth(dragHint);
        drawContext.drawText(this.textRenderer, Text.literal(dragHint).styled(s -> s.withItalic(true)),
                this.width - hintW - 10, this.height - 18, 0x60FFFFFF, false);

        // ---- 婊氬姩鏉℃覆鏌?----
        renderScrollbars(drawContext, mouseX, mouseY);

        // ---- 宸︿笅瑙掔粺璁′俊鎭?----
        int[] counts = countNodes();
        String stats = I18n.tr("litematlist.raw_materials.stats", counts[0], counts[1]);
        drawContext.drawTextWithShadow(this.textRenderer, stats, 10, this.height - 21, 0x80FFFFFF);
    }

    // ==================== 鑺傜偣娓叉煋 ====================

    private void renderNodeTree(DrawContext ctx, NodeLayout layout,
                                int mouseX, int mouseY, int treeAreaBottom) {
        int screenX = layout.x - (int) scrollX;
        int nodeCenterY = layout.y - (int) scrollY;
        int screenY = nodeCenterY - NODE_HEIGHT / 2;

        if (screenX + NODE_WIDTH < 0 || screenX > cullRightW()
                || screenY + NODE_HEIGHT < cullTopW() || screenY > treeAreaBottom) {
            if (!layout.collapsed) {
                for (NodeLayout child : layout.children) {
                    renderNodeTree(ctx, child, mouseX, mouseY, treeAreaBottom);
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

        ctx.fill(screenX, screenY, screenX + NODE_WIDTH, screenY + NODE_HEIGHT, bgColor);
        ctx.fill(screenX, screenY, screenX + 3, screenY + NODE_HEIGHT, borderColor);

        var item = layout.node.getItem();
        if (item != null) {
            ctx.drawItem(new ItemStack(item), screenX + 6, screenY + 2);
        }

        String fullName = layout.node.itemId;
        if (item != null) {
            fullName = item.getName().getString();
        }

        // 鏁伴噺绱ф尐鍚嶇О锛屾姌鍙犲浘鏍囧湪鏈€鍙宠竟锛涢€氶厤鐗╁搧鏄剧ず榻胯疆鏇挎崲鍥炬爣
        String countStr = "x" + layout.node.count;
        int countW = this.textRenderer.getWidth(countStr);

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

        // 鍚嶇О鍙敤瀹藉害 = 鍒版姌鍙犲浘鏍?鍙宠竟缂?- 鍥炬爣浣嶇疆 - 鏁伴噺瀹藉害 - 闂磋窛
        int nameStartX = screenX + 26;
        int maxAvailWidth = nameEndX - nameStartX - countW - 4;
        String displayName = this.textRenderer.trimToWidth(fullName, maxAvailWidth);
        if (displayName.length() < fullName.length()) {
            String withEllipsis = this.textRenderer.trimToWidth(fullName, maxAvailWidth - this.textRenderer.getWidth("..."));
            displayName = withEllipsis + "...";
        }
        int displayNameW = this.textRenderer.getWidth(displayName);

        // 鏁伴噺绱ц窡鍦ㄥ悕绉板悗闈?
        int countX = nameStartX + displayNameW + 4;
        ctx.drawTextWithShadow(this.textRenderer, displayName, nameStartX, screenY + 5, 0xFFFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, countStr, countX, screenY + 5, 0xFFAAAAAA);

        // 閫氶厤鐗╁搧榻胯疆鏇挎崲鍥炬爣
        if (isWildcard && gearIconX > 0) {
            int gearY = screenY + (NODE_HEIGHT - GEAR_ICON_SIZE) / 2;
            boolean gearHovered = mouseX >= gearIconX && mouseX < gearIconX + GEAR_ICON_SIZE
                    && mouseY >= gearY && mouseY < gearY + GEAR_ICON_SIZE;
            ctx.drawTexture(GEAR_ICON, gearIconX, gearY,
                    0.0f, 0.0f, GEAR_ICON_SIZE, GEAR_ICON_SIZE, GEAR_ICON_SIZE, GEAR_ICON_SIZE);
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
            ctx.drawTexture(icon, expandIconX, iconY, 0.0f, 0.0f,
                    EXPAND_ICON_SIZE, EXPAND_ICON_SIZE, EXPAND_ICON_SIZE, EXPAND_ICON_SIZE);
        }

        // 悬停提示：名称上显示完整ID，数量上显示换算量级
        if (item != null) {
            boolean nameHovered = mouseX >= nameStartX && mouseX < nameStartX + displayNameW
                    && mouseY >= screenY && mouseY < screenY + NODE_HEIGHT;
            boolean countHovered = mouseX >= countX && mouseX < countX + countW
                    && mouseY >= screenY && mouseY < screenY + NODE_HEIGHT;
            if (nameHovered) {
                String tip = fullName + " (" + layout.node.itemId + ")";
                drawTooltipFixed(ctx, Text.literal(tip));
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
                drawTooltipFixed(ctx, Text.literal(tip));
            } else if (isHovered) {
                // 整个条悬停，显示名称+ID
                String tip = fullName + " (" + layout.node.itemId + ")";
                drawTooltipFixed(ctx, Text.literal(tip));
            }
        }

        if (!layout.collapsed) {
            for (NodeLayout child : layout.children) {
                renderNodeTree(ctx, child, mouseX, mouseY, treeAreaBottom);
            }
        }
    }

    // ==================== 杩炴帴绾挎覆鏌擄紙妯嚎+绔栫嚎锛屾棤鏂滅嚎锛?====================

    private void renderConnectors(DrawContext ctx, NodeLayout layout,
                                   int mouseX, int mouseY) {
        if (layout.collapsed || layout.children.isEmpty()) return;

        int parentRightX = layout.x - (int) scrollX + NODE_WIDTH;
        int parentCenterY = layout.y - (int) scrollY;
        int treeAreaBottom = cullBottomW();

        if (parentRightX + COLUMN_SPACING < 0 || parentRightX > cullRightW()

                || parentCenterY < cullTopW() || parentCenterY > treeAreaBottom) {
            // 浣嗕粛闇€閫掑綊娓叉煋瀛愯妭鐐硅繛绾?
            for (NodeLayout child : layout.children) {
                renderConnectors(ctx, child, mouseX, mouseY);
            }
            return;
        }

        // 鏀堕泦鍙瀛愯妭鐐?
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
                renderConnectors(ctx, child, mouseX, mouseY);
            }
            return;
        }

        int midX = parentRightX + COLUMN_SPACING / 2;

        // 姘村钩绾匡細浠庣埗鑺傜偣鍙宠竟缂樺埌涓偣
        drawHLine(ctx, parentRightX, midX, parentCenterY, 0x60FFFFFF);

        // 閰嶆柟鍥炬爣锛堝湪姘村钩绾夸腑鐐癸級
        String recipeType = layout.node.recipeType;
        if (recipeType != null && !recipeType.isEmpty() && !"base".equals(recipeType)) {
            int iconX = (parentRightX + midX) / 2 - RECIPE_ICON_SIZE / 2;
            int iconY = parentCenterY - RECIPE_ICON_SIZE / 2;

            if (iconX + RECIPE_ICON_SIZE >= 0 && iconX <= cullRightW()

                    && iconY + RECIPE_ICON_SIZE >= cullTopW() && iconY <= treeAreaBottom) {
                ItemStack stationItem = getRecipeStationItem(recipeType);
                if (stationItem != null) {
                    ctx.drawItem(stationItem, iconX, iconY);
                }

                if (mouseX >= iconX && mouseX < iconX + RECIPE_ICON_SIZE

                        && mouseY >= iconY && mouseY < iconY + RECIPE_ICON_SIZE) {
                    hoveredRecipeNode = layout;
                }
            }
        }

        if (visibleChildren.size() == 1) {
            // 鍙湁涓€涓瓙鑺傜偣鏃讹紝鐩存帴鐢籐褰㈢嚎
            NodeLayout child = visibleChildren.get(0);
            int childCenterY = child.y - (int) scrollY;
            drawVLine(ctx, midX, parentCenterY, childCenterY, 0x60FFFFFF);
            int childLeftX = child.x - (int) scrollX;
            drawHLine(ctx, midX, childLeftX, childCenterY, 0x60FFFFFF);
        } else {
            // 澶氫釜瀛愯妭鐐癸細浠庢渶涓婂埌鏈€涓嬬敾绔栫嚎锛岀劧鍚庡垎鍒敾姘村钩绾胯繛鍒版瘡涓瓙鑺傜偣
            drawVLine(ctx, midX, minChildY, maxChildY, 0x60FFFFFF);

            for (NodeLayout child : visibleChildren) {
                int childLeftX = child.x - (int) scrollX;
                int childCenterY = child.y - (int) scrollY;
                // 浠庣珫绾垮埌瀛愯妭鐐瑰乏杈圭紭鐨勬按骞崇嚎
                drawHLine(ctx, midX, childLeftX, childCenterY, 0x60FFFFFF);
            }
        }

        // 閫掑綊娓叉煋瀛愯妭鐐硅繛绾?
        for (NodeLayout child : layout.children) {
            renderConnectors(ctx, child, mouseX, mouseY);
        }
    }

    /** 鐢绘按骞崇嚎锛岃鍓埌鍙鍖哄煙 */
    private void drawHLine(DrawContext ctx, int x1, int x2, int y, int color) {
        if (x1 > x2) { int t = x1; x1 = x2; x2 = t; }
        int treeAreaTop = cullTopW();
        int treeAreaBottom = cullBottomW();
        int treeAreaRight = cullRightW();
        if (y < treeAreaTop || y > treeAreaBottom) return;
        if (x2 < 0 || x1 > treeAreaRight) return;
        x1 = Math.max(x1, 0);
        x2 = Math.min(x2, treeAreaRight);
        if (x1 <= x2) ctx.fill(x1, y, x2 + 1, y + 1, color);
    }

    /** 鐢荤珫绾匡紝瑁佸壀鍒板彲瑙嗗尯鍩?*/
    private void drawVLine(DrawContext ctx, int x, int y1, int y2, int color) {
        if (y1 > y2) { int t = y1; y1 = y2; y2 = t; }
        int treeAreaTop = cullTopW();
        int treeAreaBottom = cullBottomW();
        int treeAreaRight = cullRightW();
        if (x < 0 || x > treeAreaRight) return;
        if (y2 < treeAreaTop || y1 > treeAreaBottom) return;
        y1 = Math.max(y1, treeAreaTop);
        y2 = Math.min(y2, treeAreaBottom);
        if (y1 <= y2) ctx.fill(x, y1, x + 1, y2 + 1, color);
    }

    // ==================== 榧犳爣浜嬩欢 ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (super.mouseClicked(mouseX, mouseY, mouseButton)) return true;
        mouseX = (int) mouseX;
        mouseY = (int) mouseY;
        float zoomScaleNow = currentZoomScale();
        int worldMouseX = zoomScaleNow != 1.0F ? (int) (mouseX / zoomScaleNow) : (int) mouseX;
        int worldMouseY = zoomScaleNow != 1.0F ? (int) ((mouseY - CONTROLS_BOTTOM * (1.0F - zoomScaleNow)) / zoomScaleNow) : (int) mouseY;

        // ---- 婊氬姩鏉＄偣鍑诲鐞?----
        int treeAreaTop = CONTROLS_BOTTOM;
        int treeAreaBottom = this.height - TREE_BOTTOM_OFFSET;
        int treeAreaHeight = treeAreaBottom - treeAreaTop;

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
                // 鐐瑰嚮杞ㄩ亾璺宠穬
                scrollY = MathHelper.clamp(
                        (double) (mouseY - treeAreaTop - thumbH / 2) / (trackH - thumbH) * maxScrollY,
                        0, maxScrollY);
                return true;
            }
        }

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

        // 灞傛暟鍥炬爣鐐瑰嚮锛堝湪杈撳叆妗嗗悗闈級
        int fieldX = 60;
        int depthCtrlY = 34;
        int depthIconX = fieldX + 30;
        if (mouseX >= depthIconX && mouseX < depthIconX + DEPTH_ICON_SIZE
                && mouseY >= depthCtrlY && mouseY < depthCtrlY + DEPTH_ICON_SIZE) {
            if (mouseButton == 0) {
                if (displayDepth < MAX_DEPTH) {

                    displayDepth++;
                    if (depthField != null) depthField.setText(String.valueOf(displayDepth));
                    reanalyze();
                }
            } else if (mouseButton == 1) {
                if (displayDepth > MIN_DEPTH) {

                    displayDepth--;
                    if (depthField != null) depthField.setText(String.valueOf(displayDepth));
                    reanalyze();
                }
            }
            return true;
        }

        if (mouseX >= fieldX && mouseX < fieldX + 26

                && mouseY >= depthCtrlY && mouseY < depthCtrlY + 18) {
            if (depthField != null) {
                depthField.setFocused(true);
            }
            if (searchField != null && searchVisible) searchField.setFocused(false);
            return true;
        }

        if (this.treeZoomEnabled && mouseButton == 0
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

        boolean onSearchIcon = mouseButton == 0
                && mouseX >= searchIconX && mouseX < searchIconX + SEARCH_ICON_SIZE
                && mouseY >= searchIconY && mouseY < searchIconY + SEARCH_ICON_SIZE;
        if (!onSearchIcon) {
            searchDragPending = false;
            searchDragging = false;
        }
        if (mouseButton == 0
                && mouseX >= searchIconX && mouseX < searchIconX + SEARCH_ICON_SIZE
                && mouseY >= searchIconY && mouseY < searchIconY + SEARCH_ICON_SIZE) {
            searchDragPending = true;
            searchDragMouseDownX = mouseX;
            searchDragMouseDownY = mouseY;
            searchDragStartIconX = searchIconX;
            searchDragStartIconY = searchIconY;
            if (depthField != null) depthField.setFocused(false);
            return true;
        }

        if (searchVisible) {
            if (mouseX >= searchFieldX - 2 && mouseX < searchFieldX + SEARCH_FIELD_W + 2
                    && mouseY >= searchFieldY - 1 && mouseY < searchFieldY + SEARCH_FIELD_H + 1) {
                if (searchField != null) {
                    searchField.setFocused(true);
                }
                if (depthField != null) depthField.setFocused(false);
                return true;
            }
            if (mouseButton == 0 && !searchMatches.isEmpty()
                    && mouseY >= searchBtnY && mouseY < searchBtnY + SEARCH_NAV_BTN_SIZE) {
                if (mouseX >= searchUpBtnX && mouseX < searchUpBtnX + SEARCH_NAV_BTN_SIZE) {
                    refreshSearchMatches();
                    if (!searchMatches.isEmpty()) {
                        searchIndex = (searchIndex - 1 + searchMatches.size()) % searchMatches.size();
                        currentSearchMatch = searchMatches.get(searchIndex);
                        scrollToMatch(currentSearchMatch);
                    }
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
                    if (searchField != null) searchField.setFocused(true);
                    return true;
                }
            }
        }

        if (mouseButton == 0) {
            // 鐐瑰嚮閰嶆柟鍥炬爣锛堝垏鎹㈠悎鎴愭柟寮忥級
            if (hoveredRecipeNode != null && hoveredNode == null) {
                RecipeTreeNode node = hoveredRecipeNode.node;
                if (node.alternativeRecipeTypes != null && !node.alternativeRecipeTypes.isEmpty()) {
                    final double savedSX = this.scrollX;
                    final double savedSY = this.scrollY;
                    String newType = com.litematlist.RawMaterialTreeAnalyzer.toggleRecipeType(
                            node.itemId, node.recipeType, node.alternativeRecipeTypes);
                    LitematListMod.LOGGER.info("[RawMaterialScreen] 鍒囨崲閰嶆柟: {} -> {}",
                            node.itemId, newType != null ? newType : "榛樿");
                    reanalyze();
                    this.scrollX = MathHelper.clamp(savedSX, this.minScrollX, this.maxScrollX);
                    this.scrollY = Math.min(savedSY, this.maxScrollY);
                    return true;
                }
            }

            // 鐐瑰嚮鑺傜偣
            if (hoveredNode != null) {
                int nodeScreenY = hoveredNode.y - (int) scrollY - NODE_HEIGHT / 2;
                int nodeScreenX = hoveredNode.x - (int) scrollX;

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
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
        if (draggingVScrollbar) {
            int treeAreaTop = CONTROLS_BOTTOM;
            int treeAreaBottom = this.height - TREE_BOTTOM_OFFSET;
            int treeAreaHeight = treeAreaBottom - treeAreaTop;
            int trackH = treeAreaHeight;
            int thumbH = Math.max(SCROLLBAR_MIN_THUMB,
                    (int) ((double) treeAreaHeight / (totalTreeHeight + treeAreaHeight) * treeAreaHeight));
            double delta = (mouseY - vScrollbarDragStartY) / (trackH - thumbH) * maxScrollY;
            scrollY = MathHelper.clamp(vScrollStartValue + delta, 0, maxScrollY);
            return true;
        }
        if (draggingHScrollbar) {
            int trackW = this.width;
            int thumbW = Math.max(SCROLLBAR_MIN_THUMB,
                    (int) ((double) this.width / (totalTreeWidth + this.width) * trackW));
            double delta = (mouseX - hScrollbarDragStartX) / (trackW - thumbW) * (maxScrollX - minScrollX);
            scrollX = MathHelper.clamp(hScrollStartValue + delta, minScrollX, maxScrollX);
            return true;
        }
        if (isDraggingTree) {
            float zs = currentZoomScale();
            double wx = zs != 1.0F ? mouseX / zs : mouseX;
            double wy = zs != 1.0F ? (mouseY - CONTROLS_BOTTOM * (1.0F - zs)) / zs : mouseY;
            scrollX = MathHelper.clamp(dragScrollStartX - ((int) wx - dragStartX), minScrollX, maxScrollX);
            scrollY = MathHelper.clamp(dragScrollStartY - ((int) wy - dragStartY), 0, maxScrollY);
            return true;
        }
        if (searchDragPending && !searchDragging) {
            double dx = mouseX - searchDragMouseDownX;
            double dy = mouseY - searchDragMouseDownY;
            if (dx * dx + dy * dy > DRAG_THRESHOLD) {
                searchDragging = true;
                if (searchField != null) {
                    searchField.setFocused(false);
                }
            }
        }
        if (searchDragging) {
            searchIconX = searchDragStartIconX + (int) (mouseX - searchDragMouseDownX);
            searchIconY = searchDragStartIconY + (int) (mouseY - searchDragMouseDownY);
            clampSearchPosition();
            if (searchField != null) {
                searchField.setX(searchFieldX);
                searchField.setY(searchFieldY);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, mouseButton, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
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
                searchVisible = !searchVisible;
                if (searchField != null) {
                    if (searchVisible) {
                        searchField.setVisible(true);
                        searchField.setFocused(true);
                        searchField.setCursorToEnd(false);
                    } else {
                        searchField.setFocused(false);
                        searchField.setVisible(false);
                    }
                }
            }
            searchDragPending = false;
            searchDragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseY >= CONTROLS_BOTTOM && mouseY <= this.height - TREE_BOTTOM_OFFSET) {
            float zs = currentZoomScale();
            scrollX = MathHelper.clamp(scrollX - horizontalAmount * 20 / zs, minScrollX, maxScrollX);
            scrollY = MathHelper.clamp(scrollY - verticalAmount * 20 / zs, 0, maxScrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    // ==================== 閿洏浜嬩欢 ====================

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            if (searchVisible && searchField != null && searchField.isFocused()) {
                searchVisible = false;
                searchField.setVisible(false);
                searchField.setFocused(false);
                return true;
            }
            if (depthField != null && depthField.isFocused()) {
                depthField.setFocused(false);
                applyDepthFromField();
                return true;
            }
            saveCollapseState();
            MinecraftClient.getInstance().setScreen(parent);
            return true;
        }
        if (keyCode == 257 || keyCode == 335) { // Enter or NumPad Enter
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
            if (depthField.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        if (searchVisible && searchField != null && searchField.isFocused()) {
            // Ctrl+V paste: support pasting Chinese/English from external sources into the search box
            long window = MinecraftClient.getInstance().getWindow().getHandle();
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_V
                    && org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL)
                    == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
                String clip = MinecraftClient.getInstance().keyboard.getClipboard();
                if (clip != null && !clip.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (char c : clip.toCharArray()) {
                        if (c >= 32 && c != 127) sb.append(c);
                    }
                    String insert = sb.toString();
                    String before = searchField.getText();
                    if (!insert.isEmpty() && before.length() < 32) {
                        if (insert.length() > 32 - before.length()) {
                            insert = insert.substring(0, 32 - before.length());
                        }
                        int pos = Math.min(searchField.getCursor(), before.length());
                        String after = before.substring(0, pos) + insert + before.substring(pos);
                        searchField.setText(after);
                        searchField.setCursor(pos + insert.length(), false);
                    }
                }
                return true;
            }
            if (searchField.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchVisible && searchField != null && searchField.isFocused()) {
            if ((modifiers & 0x2) != 0) {
                return true;
            }
            if (searchField.charTyped(chr, modifiers)) {
                return true;
            }
            int cp = (int) chr;
            if (cp >= 32 && cp != 127 && searchField.getText().length() < 32) {
                String before = searchField.getText();
                int pos = Math.min(searchField.getCursor(), before.length());
                String after = before.substring(0, pos) + Character.toString(cp) + before.substring(pos);
                searchField.setText(after);
                searchField.setCursor(pos + 1, false);
            }
            return true;
        }
        if (depthField != null && depthField.isFocused()) {
            if (Character.isDigit(chr)) {

                if (depthField.charTyped(chr, modifiers)) {
                    return true;
                }
            }
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    private void applyDepthFromField() {
        if (depthField == null) return;
        try {
            int val = Integer.parseInt(depthField.getText().trim());
            val = MathHelper.clamp(val, MIN_DEPTH, MAX_DEPTH);
            if (val != displayDepth) {
                displayDepth = val;
                depthField.setText(String.valueOf(displayDepth));
                reanalyze();
            }
        } catch (NumberFormatException e) {
            depthField.setText(String.valueOf(displayDepth));
        }
    }

    // ==================== JSON瀵煎嚭锛堣烦杩囨姌鍙犺妭鐐癸級 ====================

    private void exportTreeAsJson() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date());
        String baseName = schematicName.replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "_");
        String filename = baseName + "-treetable-" + timestamp + ".json";
        Path exportPath = getTreeTableExportPath(filename);

        // 鏀堕泦鎶樺彔鑺傜偣璺緞
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
            LitematListMod.LOGGER.info("閰嶆柟鏍戝凡瀵煎嚭: {}", exportPath);

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                Text link = Text.literal(filename)
                        .styled(style -> style
                                .withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.OPEN_FILE, exportPath.toString()))
                                .withUnderline(true)
                                .withColor(0x55FFFF));
                Text msg = Text.translatable("litematlist.message.exported_tree").append(link);
                client.player.sendMessage(msg, false);
            }
        } catch (Exception e) {
            LitematListMod.LOGGER.error("导出配方树失败", e);
        }
    }

    // ==================== 閫氱敤瀵煎嚭锛坱xt/csv锛?====================

    /**
     * 鏀堕泦鏍戠姸鍥剧殑鍙跺瓙鑺傜偣锛堝畬鍏ㄥ睍寮€锛屾姌鍙犺妭鐐逛綔涓哄彾瀛愶級銆?     * 姣忎釜鍙跺瓙鑺傜偣鍖呭惈锛氱墿鍝両D銆佹暟閲忋€侀厤鏂硅鏄庛€?     */
    private List<String[]> collectLeafNodes() {
        List<String[]> leaves = new ArrayList<>();
        for (NodeLayout root : rootLayouts) {
            collectLeafNodesRecursive(root, leaves);
        }
        return leaves;
    }

    private void collectLeafNodesRecursive(NodeLayout layout, List<String[]> out) {
        if (layout.collapsed || layout.children.isEmpty()) {
            // 鎶樺彔鑺傜偣鎴栨棤瀛愯妭鐐?= 鍙跺瓙鑺傜偣
            Item item = Registries.ITEM.get(Identifier.of(layout.node.itemId));
            String itemName = item != null ? item.getName().getString() : layout.node.itemId;
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

    /** 瀵煎嚭涓?TXT 鏍煎紡锛堜笌 MaterialDetailScreen 鏍煎紡涓€鑷达級 */
    private void exportAsTxt() {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date());
            String filename = "raw_materials_" + schematicName.replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "_") + "_" + timestamp + ".txt";
            Path exportPath = getRawMaterialExportPath(filename);
            java.nio.file.Files.createDirectories(exportPath.getParent());

            List<String[]> leaves = collectLeafNodes();
            try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(exportPath.toFile()), StandardCharsets.UTF_8))) {
                String title = "原理图\'" + schematicName + "\'原材料";
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
            LitematListMod.LOGGER.error("瀵煎嚭TXT澶辫触", e);
        }
    }

    /** 瀵煎嚭涓?JSON 鏍煎紡锛堜笌 MaterialDetailScreen 鏍煎紡涓€鑷达紝浠呭彾瀛愯妭鐐癸級 */
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
                String title = "原理图\'" + schematicName + "\'原材料";
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
            LitematListMod.LOGGER.error("瀵煎嚭JSON澶辫触", e);
        }
    }

    /** 瀵煎嚭涓?CSV 鏍煎紡锛堜笌 MaterialDetailScreen 鏍煎紡涓€鑷达級 */
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
            LitematListMod.LOGGER.error("瀵煎嚭CSV澶辫触", e);
        }
    }

    /** 鍙戦€佸鍑烘垚鍔熸秷鎭?*/
    private void sendExportMessage(String fmt, String filename, Path exportPath) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            Text link = Text.literal(filename)
                    .styled(style -> style
                            .withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.OPEN_FILE, exportPath.toString()))
                            .withUnderline(true).withColor(0x55FFFF));
            client.player.sendMessage(Text.translatable("litematlist.message.exported_raw", fmt).append(link), false);
        }
        LitematListMod.LOGGER.info("鍘熸潗鏂欏凡瀵煎嚭涓簕}: {}", fmt, exportPath);
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

    // ==================== 閫氶厤鐗╁搧鏇挎崲 ====================

    /**
     * 鏍规嵁閰嶆柟绫诲瀷杩斿洖瀵瑰簲鐨勫伐浣滅珯鐗╁搧鍥炬爣銆?     */
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
     * 鎵撳紑閫氶厤鐗╁搧鏇挎崲閫夋嫨鍣紝鍏佽鐜╁灏嗛€氶厤鐗╁搧鏇挎崲涓哄悓绫荤墿鍝併€?     */
    private void openWildcardReplacement(NodeLayout layout) {
        String itemId = layout.node.itemId;
        String parentId = layout.parentItemId;
        final double savedScrollX = this.scrollX;
        final double savedScrollY = this.scrollY;
        // 鏀堕泦鍚岀被鐗╁搧锛堢浉鍚岀墿鍝佹爣绛剧粍锛?
        Set<Item> compatibleItems = getCompatibleItems(itemId);
        goingToSubScreen = true;
        MinecraftClient.getInstance().setScreen(
                new BlockPickerScreen(this, selectedItem -> {
                    // 鏇挎崲鑺傜偣涓殑鐗╁搧ID
                    String newId = Registries.ITEM.getId(selectedItem).toString();
                    LitematListMod.LOGGER.info("[RawMaterialScreen] 閫氶厤鏇挎崲: {} -> {} (鍚屾={})",
                            itemId, newId, syncReplacementEnabled);
                    if (syncReplacementEnabled) {
                        // 鍏ㄥ眬鍚屾鏇挎崲锛氭墍鏈夌浉鍚屽瓙鏉愭枡閮借鏇挎崲
                        com.litematlist.RawMaterialTreeAnalyzer.setWildcardReplacement(itemId, newId);
                    } else {
                        // 浠呭綋鍓嶄笂涓嬫枃鏇挎崲锛氬彧鏇挎崲褰撳墠鐖剁墿鍝佷笅鐨勫瓙鏉愭枡
                        String effectiveParentId = parentId != null ? parentId : itemId;
                        com.litematlist.RawMaterialTreeAnalyzer.setContextWildcardReplacement(
                                effectiveParentId, itemId, newId);
                    }
                    // // 妫€鏌ョ洿鎺ョ埗鐗╁搧鎴栫鐖剁墿鍝佹槸鍚︿负闆曠汗涔︽灦

                    boolean isBookshelfChain = "minecraft:chiseled_bookshelf".equals(parentId);
                    if (!isBookshelfChain && parentId != null) {
                        // 涔熸鏌ユ槸鍚︿负鍗婄爾瀛愯妭鐐圭殑鏈ㄦ澘锛堢埗鐗╁搧鏄崐鐮栵紝绁栫埗鏄洉绾逛功鏋讹級
                        String grandparentId = layout.getGrandparentItemId();
                        isBookshelfChain = "minecraft:chiseled_bookshelf".equals(grandparentId);
                    }
                    if (isBookshelfChain) {
                        if (itemId.endsWith("_planks")) {
                            // 鏇挎崲鏈ㄦ澘鏃跺悓姝ユ浛鎹㈠搴斿崐鐮?
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
                            // 鏇挎崲鍗婄爾鏃跺悓姝ユ浛鎹㈠搴旀湪鏉?
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
                    // 寮哄埗閲嶅垎鏋愰厤鏂规爲锛岀‘淇濇浛鎹㈢珛鍗崇敓鏁?
                    loadOrAnalyze();
                    // 淇濆瓨婊氬姩浣嶇疆
                    this.scrollX = MathHelper.clamp(savedScrollX, this.minScrollX, this.maxScrollX);
                    this.scrollY = Math.min(savedScrollY, this.maxScrollY);
                }, compatibleItems));
    }

    /**
     * 鑾峰彇涓庢寚瀹氱墿鍝佸吋瀹圭殑鍚岀被鐗╁搧闆嗗悎锛堝熀浜庣墿鍝佹爣绛剧粍锛夈€?     * 渚嬪锛氭鏈ㄦ湪鏉?鈫?鎵€鏈夋湪鏉垮彉绉嶏紝鐭冲墤 鈫?鎵€鏈夌煶璐ㄥ伐鍏风瓑銆?     */
    private Set<Item> getCompatibleItems(String itemId) {
        Set<Item> collected = new LinkedHashSet<>();
        Item targetItem = Registries.ITEM.get(Identifier.of(itemId));
        if (targetItem == null) return collected;

        // 绛栫暐1锛氭煡鎵剧墿鍝佹墍灞炵殑鏍囩锛屾敹闆嗗悓鏍囩鐗╁搧
        var registry = MinecraftClient.getInstance().world != null
                ? MinecraftClient.getInstance().world.getRegistryManager()
                : null;
        if (registry != null) {
            var itemTags = registry.getOptional(net.minecraft.registry.RegistryKeys.ITEM);
            if (itemTags.isPresent()) {
                var tagRegistry = itemTags.get();
                tagRegistry.streamTags().forEach(tag -> {
                    var tagEntries = tagRegistry.getEntryList(tag);
                    if (tagEntries.isPresent()) {
                        boolean contains = tagEntries.get().stream().anyMatch(e -> e.value() == targetItem);
                        if (contains) {
                            tagEntries.get().forEach(e -> collected.add(e.value()));
                        }
                    }
                });
            }
        }

        // 绛栫暐2锛氬鏋滄病鏈夋壘鍒板悓绫荤墿鍝侊紝鑷冲皯鍖呭惈鐩爣鐗╁搧鑷韩
        if (collected.isEmpty()) {
            collected.add(targetItem);
        }

        // 绛栫暐2.5锛氭樉寮忔坊鍔犲悓绫荤墿鍝侊紙鏍囩鏌ユ壘鍙兘閬楁紡鐨勫彉绉嶏級
        addExplicitCompatibleItems(collected, itemId);

        // 绛栫暐3锛氭寜鐗╁搧绫诲瀷杩囨护锛岄伩鍏嶈法绫诲瀷娣峰叆锛堝缇婃瘺鏇挎崲椤甸潰鏄剧ず鎵€鏈夋煋鑹茬墿鍝侊級
        Set<Item> filtered = filterByItemType(collected, itemId);
        return filtered;
    }

    /**
     * 鏄惧紡娣诲姞鍚岀被鐗╁搧锛堟爣绛炬煡鎵惧彲鑳介仐婕忕殑鍙樼锛夈€?     * 渚嬪锛氬渾鐭冲拰榛戠煶鍦ㄥ悓涓€鏍囩锛屼絾娣辨澘宀╁渾鐭冲彲鑳戒笉鍦紝闇€瑕佹樉寮忔坊鍔犮€?     */
    private void addExplicitCompatibleItems(Set<Item> collected, String itemId) {
        if (itemId.equals("minecraft:cobblestone") || itemId.equals("minecraft:blackstone")

                || itemId.equals("minecraft:cobbled_deepslate")) {
            addItemById(collected, "minecraft:cobblestone");
            addItemById(collected, "minecraft:blackstone");
            addItemById(collected, "minecraft:cobbled_deepslate");
        }
        // 娌欏瓙/鐮傜牼
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
        Item item = Registries.ITEM.get(Identifier.of(itemId));
        if (item != null) {
            collected.add(item);
        }
    }

    /**
     * 鎸夌墿鍝佺被鍨嬭繃婊わ紝鍙繚鐣欎笌鐩爣鐗╁搧鍚岀被鍨嬬殑鍙樼銆?     * 渚嬪锛氱緤姣涘彧淇濈暀 _wool 缁撳熬鐨勶紝鏈ㄦ澘鍙繚鐣?_planks 缁撳熬鐨勩€?     */
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

        if (typeSuffix == null) return items; // 涓嶉渶瑕佽繃婊?
        Set<Item> filtered = new LinkedHashSet<>();
        for (Item item : items) {
            String id = Registries.ITEM.getId(item).toString();
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
                if (typeSuffix.equals("_slab")) {

                    String plankId = id.replace("_slab", "_planks");
                    if (Registries.ITEM.containsId(Identifier.of(plankId))) {
                        filtered.add(item);
                    }
                } else {
                    filtered.add(item);
                }
            }
        }
        return filtered.isEmpty() ? items : filtered;
    }

    // ==================== 鍒涢€犳ā寮忔帓搴?====================

    /**
     * 鎸夊垱閫犳ā寮忕墿鍝佹爮椤哄簭瀵?treeRoots 鎺掑簭銆?     */
    private void sortByCreativeOrder() {
        Map<String, Integer> sortOrder = buildCreativeSortOrder();
        treeRoots.sort((a, b) -> {
            int orderA = sortOrder.getOrDefault(a.itemId, Integer.MAX_VALUE);
            int orderB = sortOrder.getOrDefault(b.itemId, Integer.MAX_VALUE);
            return Integer.compare(orderA, orderB);
        });
    }

    /**
     * 鏋勫缓鍒涢€犳ā寮忕墿鍝佹爮鎺掑簭鏄犲皠锛氱墿鍝両D 鈫?鎺掑簭浣嶇疆銆?     */
    private Map<String, Integer> buildCreativeSortOrder() {
        Map<String, Integer> order = new LinkedHashMap<>();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return order;

        var registry = client.world.getRegistryManager().getOptional(RegistryKeys.ITEM_GROUP);
        if (registry.isEmpty()) return order;

        int groupIndex = 0;
        for (var group : registry.get()) {
            int itemIndex = 0;
            for (ItemStack stack : group.getDisplayStacks()) {
                String id = Registries.ITEM.getId(stack.getItem()).toString();
                if (!order.containsKey(id)) {
                    order.put(id, groupIndex * 10000 + itemIndex);
                }
                itemIndex++;
            }
            groupIndex++;
        }
        return order;
    }

    // ==================== 婊氬姩鏉?====================

    /**
     * 娓叉煋鍨傜洿鍜屾按骞虫粴鍔ㄦ潯銆?     */
    private void renderScrollbars(DrawContext ctx, int mouseX, int mouseY) {
        int treeAreaTop = CONTROLS_BOTTOM;
        int treeAreaBottom = this.height - TREE_BOTTOM_OFFSET;
        int treeAreaHeight = treeAreaBottom - treeAreaTop;
        int treeAreaWidth = this.width;

        // ---- 鍨傜洿婊氬姩鏉★紙鍙充晶锛?----
        if (maxScrollY > 0) {
            int trackX = this.width - SCROLLBAR_THICKNESS - 2;
            int trackY = treeAreaTop;
            int trackH = treeAreaHeight;

            // 婊氬姩鏉¤建閬?
            ctx.fill(trackX, trackY, trackX + SCROLLBAR_THICKNESS, trackY + trackH, 0x30FFFFFF);

            // 婊氬姩鏉℃粦鍧?
            int thumbH = Math.max(SCROLLBAR_MIN_THUMB,
                    (int) ((double) treeAreaHeight / (totalTreeHeight + treeAreaHeight) * treeAreaHeight));
            int thumbY = trackY + (int) ((double) scrollY / maxScrollY * (trackH - thumbH));
            int thumbColor = draggingVScrollbar ? 0xA0FFFFFF : 0x60FFFFFF;
            if (!draggingVScrollbar && mouseX >= trackX && mouseX < trackX + SCROLLBAR_THICKNESS
                    && mouseY >= thumbY && mouseY < thumbY + thumbH) {
                thumbColor = 0x80FFFFFF;
            }
            ctx.fill(trackX, thumbY, trackX + SCROLLBAR_THICKNESS, thumbY + thumbH, thumbColor);
        }

        // ---- 姘村钩婊氬姩鏉★紙搴曢儴锛?px楂橈紝瀹屽叏璐村簳锛?----
        if (maxScrollX > minScrollX) {
            int hScrollbarHeight = 4;
            int trackY = this.height - hScrollbarHeight;
            int trackX = 0;
            int trackW = treeAreaWidth;

            // 婊氬姩鏉¤建閬?
            ctx.fill(trackX, trackY, trackX + trackW, trackY + hScrollbarHeight, 0x30FFFFFF);

            // 婊氬姩鏉℃粦鍧?
            int thumbW = Math.max(SCROLLBAR_MIN_THUMB,
                    (int) ((double) treeAreaWidth / (totalTreeWidth + treeAreaWidth) * trackW));
            int thumbX = trackX + (int) ((scrollX - minScrollX) / (maxScrollX - minScrollX) * (trackW - thumbW));
            int thumbColor = draggingHScrollbar ? 0xA0FFFFFF : 0x60FFFFFF;
            if (!draggingHScrollbar && mouseX >= thumbX && mouseX < thumbX + thumbW
                    && mouseY >= trackY && mouseY < trackY + hScrollbarHeight) {
                thumbColor = 0x80FFFFFF;
            }
            ctx.fill(thumbX, trackY, thumbX + thumbW, trackY + hScrollbarHeight, thumbColor);
        }
    }

    // ==================== 鑺傜偣璁℃暟 ====================

    /**
     * 璁＄畻鏍戜腑鎬荤墿鍝侀」鏁板拰鍘熸潗鏂欓」鏁般€?     * @return [鎬荤墿鍝侀」鏁? 鍘熸潗鏂欓」鏁癩
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


