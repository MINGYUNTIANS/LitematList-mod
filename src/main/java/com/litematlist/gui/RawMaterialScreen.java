package com.litematlist.gui;

import com.google.gson.*;
import com.litematlist.I18n;
import com.litematlist.LitematListMod;
//#if MC == 12001
//$$import com.litematlist.TextWidthUtils;
//#endif
import com.litematlist.RecipeTreeNode;
import com.litematlist.RawMaterialTreeAnalyzer;
//#if MC == 11701
//$$import com.litematlist.TextWidthUtils;
//#endif
//#if MC == 12101
//$$import com.litematlist.TextWidthUtils;
//#endif
import com.litematlist.LitematicReader.MaterialEntry;
import com.litematlist.gui.MaterialDataStore.RawMaterialEntry;
import com.litematlist.MaterialListInjector;
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$import com.litematlist.gui.MaterialListScreen;
//#endif
//#if MC == 12110 || MC == 12111
import com.litematlist.gui.MaterialListScreen;
//#endif
import com.litematlist.PinyinSearch;
//#if MC == 12110 || MC == 12111
import com.litematlist.SyncmaticaBridge;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
import com.litematlist.TextWidthUtils;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$import com.litematlist.SyncmaticaBridge;
//#endif
//#if MC == 11904
//$$import com.litematlist.TextWidthUtils;
//#endif
//#if MC == 12104
//$$import com.litematlist.TextWidthUtils;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$import com.litematlist.config.Configs;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$import com.litematlist.gui.MaterialListScreen;
//#endif
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.MinecraftClient;
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
//#elseif MC == 12104
//$$import net.minecraft.client.render.RenderLayer;
//#endif
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.registry.Registries;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.registry.RegistryKeys;
//#endif
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$import com.litematlist.config.Configs;
//#endif
//#if MC == 12110 || MC == 12111
import com.litematlist.config.Configs;
//#endif

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.gui.DrawableHelper;
//#endif
//#if MC == 11701 || MC == 11904
//$$import com.mojang.blaze3d.systems.RenderSystem;
//#endif
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.util.math.MatrixStack;
//#endif
//#if MC == 11701
//$$import net.minecraft.util.registry.Registry;
//#endif
//#if MC == 11701
//$$import net.minecraft.text.TranslatableText;
//#endif
//#if MC == 11701
//$$import net.minecraft.text.LiteralText;
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
/**
 * 原材料横向树状图界面：以配方树形式展示原材料溯源结果。
 * 支持配方层数选择、节点折叠/展开、JSON导出、冗余列表。
 */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$/**
//$$ * 鍘熸潗鏂欐í鍚戞爲鐘跺浘鐣岄潰锛氫互閰嶆柟鏍戝舰寮忓睍绀哄師鏉愭枡婧簮缁撴灉銆? * 鏀寔閰嶆柟灞傛暟閫夋嫨銆佽妭鐐规姌鍙?灞曞紑銆丣SON瀵煎嚭銆佸啑浣欏垪琛ㄣ€? */
//#endif
public class RawMaterialScreen extends GuiBase {

    private final GuiBase parent;
    private final String schematicName;
    private final Path filePath;
    private String pathKey;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 树数据 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 鏍戞暟鎹?----
//#endif
    private List<RecipeTreeNode> treeRoots = new ArrayList<>();
    private boolean initialLoad = true;
    private boolean analyzing = false;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 冗余数据 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 鍐椾綑鏁版嵁 ----
//#endif
    private List<RawMaterialEntry> redundancyItems = new ArrayList<>();

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 显示层数 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 鏄剧ず灞傛暟 ----
//#endif
    private int displayDepth = 5;
    private static final int MIN_DEPTH = 0;
    private static final int MAX_DEPTH = 10;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 层数输入框 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 灞傛暟杈撳叆妗?----
//#endif
    private net.minecraft.client.gui.widget.TextFieldWidget depthField;
    private boolean depthFieldFocused = false;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 树状图搜索（1.21.10 专属功能） ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- tree view search ----
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final Identifier SEARCH_ICON = Identifier.of("litematlist", "textures/gui/search.png");
//#elseif MC == 11701
//$$    private static final Identifier SEARCH_ICON = new Identifier("litematlist", "textures/gui/search.png");
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final int SEARCH_ICON_SIZE = 21; // 图标纹理实际为 21x21，按 1:1 显示
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    private static final int SEARCH_ICON_SIZE = 21;
//#endif
    private static final int SEARCH_ICON_DEFAULT_Y = 58;
    private static final int SEARCH_FIELD_W = 75;
    private static final int SEARCH_FIELD_H = 16;
    private static final int SEARCH_NAV_BTN_SIZE = 16;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final int DRAG_THRESHOLD = 16; // 位移超过 4px（平方距离）判定为拖动
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    private static final int DRAG_THRESHOLD = 16;
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 树状图整体缩放（开关在【配置】-【通用】；【-】百分比【+】显示在右下角） ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- tree view zoom ----
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final Identifier ZOOM_IN_ICON = Identifier.of("litematlist", "textures/gui/zoom/zoom_in.png");
//#elseif MC == 11701
//$$    private static final Identifier ZOOM_IN_ICON = new Identifier("litematlist", "textures/gui/zoom/zoom_in.png");
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final Identifier ZOOM_OUT_ICON = Identifier.of("litematlist", "textures/gui/zoom/zoom_out.png");
//#elseif MC == 11701
//$$    private static final Identifier ZOOM_OUT_ICON = new Identifier("litematlist", "textures/gui/zoom/zoom_out.png");
//#endif
    private static final int ZOOM_BTN_SIZE = 16;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final int ZOOM_MIN_PERCENT = 10;     // 最小一档：10%（缩到极限）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    private static final int ZOOM_MIN_PERCENT = 10;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final int ZOOM_MAX_PERCENT = 200;    // 最大一档：200%（放大的上限）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    private static final int ZOOM_MAX_PERCENT = 200;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final int ZOOM_STEP_PERCENT = 10;    // 每档 ±10%，共 20 档（10 → 20 → … → 200）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    private static final int ZOOM_STEP_PERCENT = 10;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final int ZOOM_DEFAULT_PERCENT = 100; // 默认档位：100%（原大小）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    private static final int ZOOM_DEFAULT_PERCENT = 100;
//#endif
    private boolean treeZoomEnabled = false;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // 静态保存缩放百分比：跨界面往返、替换物品、退出界面后均保持（并根据世界持久化到 persistence.json）
//#endif
    private static int treeZoomPercent = ZOOM_DEFAULT_PERCENT;
    private int zoomOutBtnX = 0;
    private int zoomInBtnX = 0;
    private int zoomBarY = 0;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // 缩放渲染时：原始屏幕鼠标坐标（悬浮提示用）
//#endif
    private int frameScreenMouseX = 0;
    private int frameScreenMouseY = 0;
    private boolean searchVisible = false;
    private net.minecraft.client.gui.widget.TextFieldWidget searchField;
    private final List<NodeLayout> searchMatches = new ArrayList<>();
    private int searchIndex = -1;
    private String lastSearchKeyword = "";
    private NodeLayout currentSearchMatch = null;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // 搜索控件的动态位置（放大镜图标为锚点，长按可拖动）；searchIconX = -1 表示尚未定位，首次布局时默认放到屏幕右侧
//#endif
    private int searchIconX = -1;
    private int searchIconY = SEARCH_ICON_DEFAULT_Y;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // 拖动状态：按下放大镜后移动超过阈值才进入拖动，否则松开视为点击
//#endif
    private boolean searchDragPending = false;
    private boolean searchDragging = false;
    private double searchDragMouseDownX = 0;
    private double searchDragMouseDownY = 0;
    private int searchDragStartIconX = 0;
    private int searchDragStartIconY = 0;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // 每帧计算出的布局：输入框位置、信息行位置
//#endif
    private int searchFieldX = 0;
    private int searchFieldY = 0;
    private int searchNavX = 0;
    private int searchNavY = 0;
    private int searchUpBtnX = 0;
    private int searchDownBtnX = 0;
    private int searchBtnY = 0; // ↑↓按钮实际Y（右侧放不下时下移到信息行下方）

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 布局 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 甯冨眬 ----
//#endif
    private static final int NODE_WIDTH = 112;
    private static final int NODE_HEIGHT = 20;
    private static final int COLUMN_SPACING = 60;
    private static final int VERTICAL_SPACING = 6;
    private static final int TREE_LEFT_MARGIN = 20;
    private static final int CONTROLS_BOTTOM = 62;
    private static final int TREE_BOTTOM_OFFSET = 44;
    private static final int MAX_NAME_CHARS = 6;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 滚动 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 婊氬姩 ----
//#endif
    private double scrollX = 0;
    private double scrollY = 0;
    private double maxScrollX = 0;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // 树状图水平滚动下限：缩放状态开启且树整体小于视口时为负数（允许整体平移、两侧不出屏），其余状态为 0
//#endif
    private double minScrollX = 0;
    private double maxScrollY = 0;
    private boolean isDraggingTree = false;
    private double dragStartX = 0;
    private double dragStartY = 0;
    private double dragScrollStartX = 0;
    private double dragScrollStartY = 0;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 节点布局缓存 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 鑺傜偣甯冨眬缂撳瓨 ----
//#endif
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

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 悬停 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 鎮仠 ----
//#endif
    private NodeLayout hoveredNode = null;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 折叠/展开图标 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 鎶樺彔/灞曞紑鍥炬爣 ----
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final Identifier EXPAND_NORMAL = Identifier.of("litematlist", "textures/gui/expand/expand_normal.png");
//#elseif MC == 11701
//$$    private static final Identifier EXPAND_NORMAL = new Identifier("litematlist", "textures/gui/expand/expand_normal.png");
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final Identifier EXPAND_HOVERED = Identifier.of("litematlist", "textures/gui/expand/expand_hovered.png");
//#elseif MC == 11701
//$$    private static final Identifier EXPAND_HOVERED = new Identifier("litematlist", "textures/gui/expand/expand_hovered.png");
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final Identifier COLLAPSE_NORMAL = Identifier.of("litematlist", "textures/gui/expand/collapse_normal.png");
//#elseif MC == 11701
//$$    private static final Identifier COLLAPSE_NORMAL = new Identifier("litematlist", "textures/gui/expand/collapse_normal.png");
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final Identifier COLLAPSE_HOVERED = Identifier.of("litematlist", "textures/gui/expand/collapse_hovered.png");
//#elseif MC == 11701
//$$    private static final Identifier COLLAPSE_HOVERED = new Identifier("litematlist", "textures/gui/expand/collapse_hovered.png");
//#endif
    private static final int EXPAND_ICON_SIZE = 12;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 替换图标（通配物品） ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 鏇挎崲鍥炬爣锛堥€氶厤鐗╁搧锛?----
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final Identifier GEAR_ICON = Identifier.of("litematlist", "textures/gui/gear.png");
//#elseif MC == 11701
//$$    private static final Identifier GEAR_ICON = new Identifier("litematlist", "textures/gui/gear.png");
//#endif
    private static final int GEAR_ICON_SIZE = 12;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 配方图标（连接线上） ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 閰嶆柟鍥炬爣锛堣繛鎺ョ嚎涓婏級 ----
//#endif
    private static final int RECIPE_ICON_SIZE = 14;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private NodeLayout hoveredRecipeNode = null; // 鼠标悬停配方图标的节点
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 层数加减图标 ----
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    private NodeLayout hoveredRecipeNode = null; // 榧犳爣鎮仠閰嶆柟鍥炬爣鐨勮妭鐐?
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 灞傛暟鍔犲噺鍥炬爣 ----
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final Identifier DEPTH_NORMAL = Identifier.of("litematlist", "textures/gui/depth/depth_normal.png");
//#elseif MC == 11701
//$$    private static final Identifier DEPTH_NORMAL = new Identifier("litematlist", "textures/gui/depth/depth_normal.png");
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final Identifier DEPTH_HOVERED = Identifier.of("litematlist", "textures/gui/depth/depth_hovered.png");
//#elseif MC == 11701
//$$    private static final Identifier DEPTH_HOVERED = new Identifier("litematlist", "textures/gui/depth/depth_hovered.png");
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final Identifier DEPTH_LOCKED = Identifier.of("litematlist", "textures/gui/depth/depth_locked.png");
//#elseif MC == 11701
//$$    private static final Identifier DEPTH_LOCKED = new Identifier("litematlist", "textures/gui/depth/depth_locked.png");
//#endif
    private static final int DEPTH_ICON_SIZE = 20;
    private boolean depthIconHovered = false;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 折叠状态持久化 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 鎶樺彔鐘舵€佹寔涔呭寲 ----
//#endif
    private static final Map<String, Set<String>> COLLAPSED_PATHS = new HashMap<>();

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 创造模式排序 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 鍒涢€犳ā寮忔帓搴?----
//#endif
    private static boolean creativeSortEnabled = false;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 同步替换 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 鍚屾鏇挎崲 ----
//#endif
    private static boolean syncReplacementEnabled = true;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 子界面标记（用于修复替换后滚动位置） ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 瀛愮晫闈㈡爣璁帮紙鐢ㄤ簬淇鏇挎崲鍚庢粴鍔ㄤ綅缃級 ----
//#endif
    private boolean goingToSubScreen = false;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 按钮位置（用于渲染彩色开关文字） ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 鎸夐挳浣嶇疆锛堢敤浜庢覆鏌撳僵鑹插紑鍏虫枃瀛楋級 ----
//#endif
    private int creativeSortBtnX = 0;
    private int creativeSortBtnW = 0;
    private int syncReplaceBtnX = 0;
    private int syncReplaceBtnW = 0;
    private int exportRawBtnX = 0;
    private int exportRawBtnW = 0;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 延迟更新 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 寤惰繜鏇存柊 ----
//#endif
    private boolean hasPendingChanges = false;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 滚动条 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 婊氬姩鏉?----
//#endif
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

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 判断是否来自项目文件夹。
     * 项目文件路径在 .minecraft/litematlist/project/<project>/...
     */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /**
//$$     * 鍒ゆ柇鏄惁鏉ヨ嚜椤圭洰鏂囦欢澶广€?     * 椤圭洰鏂囦欢璺緞鍦?.minecraft/litematlist/project/<project>/...
//$$     */
//#endif
    private boolean isFromProjectFolder() {
        if (this.filePath == null) return false;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // filePath 是 .minecraft/litematlist/project/<project>/<file>.litematic
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // filePath 鏄?.minecraft/litematlist/project/<project>/<file>.litematic
//#endif
        Path parent = this.filePath.getParent();
        if (parent == null) return false;
        Path grandParent = parent.getParent();
        if (grandParent == null) return false;
        return "project".equals(grandParent.getFileName().toString());
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 获取项目文件夹名称（父文件夹名称） */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 鑾峰彇椤圭洰鏂囦欢澶瑰悕绉帮紙鐖舵枃浠跺す鍚嶇О锛?*/
//#endif
    private String getProjectFolderName() {
        if (this.filePath == null || this.filePath.getParent() == null) return "";
        return this.filePath.getParent().getFileName().toString();
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 获取正确的导出路径（原材料导出） */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 鑾峰彇姝ｇ‘鐨勫鍑鸿矾寰勶紙鍘熸潗鏂欏鍑猴級 */
//#endif
    private Path getRawMaterialExportPath(String filename) {
        Path mcDir = MinecraftClient.getInstance().runDirectory.toPath().resolve("litematlist");
        if (isFromProjectFolder()) {
            String projectName = getProjectFolderName();
            return mcDir.resolve(projectName).resolve("rawmaterials").resolve(filename);
        } else {
            return mcDir.resolve("rawmaterials").resolve(filename);
        }
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 获取表式JSON正确的导出路径（统一放 treetable 文件夹） */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 鑾峰彇琛ㄥ紡JSON姝ｇ‘鐨勫鍑鸿矾寰勶紙缁熶竴鏀?treetable 鏂囦欢澶癸級 */
//#endif
    private Path getTreeTableExportPath(String filename) {
        Path mcDir = MinecraftClient.getInstance().runDirectory.toPath().resolve("litematlist");
        return mcDir.resolve("treetable").resolve(filename);
    }

    @Override
    public void initGui() {
        super.initGui();

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 树状图缩放控制（配置-通用开关）；缩放百分比为静态持久化值，进入界面不再重置
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // tree view zoom control toggle (config - general)
//#endif
        this.treeZoomEnabled = Configs.Generic.TREE_ZOOM_CONTROL.getBooleanValue();

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // ---- 层数控制 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // ---- 灞傛暟鎺у埗 ----
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 新布局: 标签 → 输入框 → [+-]图标
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鏂板竷灞€: 鏍囩 鈫?杈撳叆妗?鈫?[+-]鍥炬爣
//#endif
        int fieldX = 60;
        int depthCtrlY = 34;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 深度输入框（在图标前面）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 娣卞害杈撳叆妗嗭紙鍦ㄥ浘鏍囧墠闈級
//#endif
        if (depthField == null) {
            this.depthField = new net.minecraft.client.gui.widget.TextFieldWidget(
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    this.textRenderer, fieldX + 3, depthCtrlY + 1, 24, 14, Text.empty());
//#elseif MC == 11701
//$$                    this.textRenderer, fieldX + 3, depthCtrlY + 1, 24, 14, new LiteralText(""));
//#endif
            this.depthField.setMaxLength(2);
            this.depthField.setText(String.valueOf(displayDepth));
            this.depthField.setDrawsBackground(false);
            this.addSelectableChild(this.depthField);
        } else {
            this.depthField.setX(fieldX + 3);
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            this.depthField.setY(depthCtrlY + 1);
//#elseif MC == 11701
//$$            this.depthField.y = depthCtrlY + 1;
//#endif
            this.depthField.setText(String.valueOf(displayDepth));
            this.addSelectableChild(this.depthField);
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // ---- 树状图搜索输入框（点击搜索图标后显示） ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // ---- tree view search input box (shown after clicking the search icon) ----
//#endif
        if (searchField == null) {
            this.searchField = new net.minecraft.client.gui.widget.TextFieldWidget(
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    this.textRenderer, 0, 0, SEARCH_FIELD_W, SEARCH_FIELD_H, Text.empty());
//#elseif MC == 11701
//$$                    this.textRenderer, 0, 0, SEARCH_FIELD_W, SEARCH_FIELD_H, new LiteralText(""));
//#endif
            this.searchField.setMaxLength(32);
            this.searchField.setDrawsBackground(false);
            this.searchField.setText("");
            this.addSelectableChild(this.searchField);
        } else {
            this.addSelectableChild(this.searchField);
        }
        computeSearchLayout();
        this.searchField.setX(searchFieldX);
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        this.searchField.setY(searchFieldY);
//#elseif MC == 11701
//$$        this.searchField.y = searchFieldY;
//#endif
        this.searchField.setVisible(this.searchVisible);
        if (!this.searchVisible) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            this.searchField.setFocused(false);
//#elseif MC == 11701
//$$            this.setFocused(null);
//#endif
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        int depthIconX = fieldX + 30; // 图标在输入框后面
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        int depthIconX = fieldX + 30; // 鍥炬爣鍦ㄨ緭鍏ユ鍚庨潰
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // ---- 创造模式排序开关（放在【+-】和【导出】之间） ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // ---- 鍒涢€犳ā寮忔帓搴忓紑鍏筹紙鏀惧湪銆?-銆戝拰銆愬鍑恒€戜箣闂达級 ----
//#endif
        String creativeSortLabel = creativeSortEnabled
                ? I18n.tr("litematlist.button.creative_sort_on")
                : I18n.tr("litematlist.button.creative_sort_off");
        creativeSortBtnW = this.textRenderer.getWidth(creativeSortLabel) + 10;
        creativeSortBtnX = depthIconX + DEPTH_ICON_SIZE + 4;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 按钮白字不渲染"开/关"，彩色文字单独覆盖
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鎸夐挳鐧藉瓧涓嶆覆鏌?寮€/鍏?锛屽僵鑹叉枃瀛楀崟鐙鐩?
//#endif
        String creativeSortBtnLabel = creativeSortLabel.substring(0, creativeSortLabel.length() - 1);
        ButtonGeneric creativeSortBtn = new ButtonGeneric(creativeSortBtnX, depthCtrlY,
                creativeSortBtnW, 20, creativeSortBtnLabel);
        creativeSortBtn.setRenderDefaultBackground(true);
        this.addButton(creativeSortBtn, (IButtonActionListener) (b, mb) -> {
            creativeSortEnabled = !creativeSortEnabled;
            hasPendingChanges = true;
            initGui();
        });

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // ---- 是否同步替换开关（默认开启） ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // ---- 鏄惁鍚屾鏇挎崲寮€鍏筹紙榛樿寮€鍚級 ----
//#endif
        String syncReplaceLabel = syncReplacementEnabled
                ? I18n.tr("litematlist.button.sync_replace_on")
                : I18n.tr("litematlist.button.sync_replace_off");
        syncReplaceBtnW = this.textRenderer.getWidth(syncReplaceLabel) + 10;
        syncReplaceBtnX = creativeSortBtnX + creativeSortBtnW + 4;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 按钮白字不渲染"开/关"，彩色文字单独覆盖
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鎸夐挳鐧藉瓧涓嶆覆鏌?寮€/鍏?锛屽僵鑹叉枃瀛楀崟鐙鐩?
//#endif
        String syncReplaceBtnLabel = syncReplaceLabel.substring(0, syncReplaceLabel.length() - 1);
        ButtonGeneric syncReplaceBtn = new ButtonGeneric(syncReplaceBtnX, depthCtrlY,
                syncReplaceBtnW, 20, syncReplaceBtnLabel);
        syncReplaceBtn.setRenderDefaultBackground(true);
        this.addButton(syncReplaceBtn, (IButtonActionListener) (b, mb) -> {
            syncReplacementEnabled = !syncReplacementEnabled;
            initGui();
        });

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // ---- 导出表式JSON按钮（同步替换右侧） ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // ---- 瀵煎嚭琛ㄥ紡JSON鎸夐挳锛堝悓姝ユ浛鎹㈠彸渚э級 ----
//#endif
        String exportTableJsonLabel = I18n.tr("litematlist.button.export_table_json");
        int exportTableJsonBtnW = this.textRenderer.getWidth(exportTableJsonLabel) + 10;
        ButtonGeneric exportJsonBtn = new ButtonGeneric(syncReplaceBtnX + syncReplaceBtnW + 4, depthCtrlY,
                exportTableJsonBtnW, 20, exportTableJsonLabel);
        exportJsonBtn.setRenderDefaultBackground(true);
        this.addButton(exportJsonBtn, (IButtonActionListener) (b, mb) -> exportTreeAsJson());

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // ---- 重新分析按钮（导出表式JSON右侧） ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // ---- 閲嶆柊鍒嗘瀽鎸夐挳锛堝鍑鸿〃寮廕SON鍙充晶锛?----
//#endif
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

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // ---- 导出按钮（重新分析右侧）：默认导出txt，shift导出csv，alt导出json ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // ---- 瀵煎嚭鎸夐挳锛堥噸鏂板垎鏋愬彸渚э級锛氶粯璁ゅ鍑簍xt锛宻hift瀵煎嚭csv锛宎lt瀵煎嚭json ----
//#endif
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

//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        // ---- 鍐椾綑鍒楄〃鎸夐挳锛堟渶鍙充晶锛?----
//#endif
        // ---- 原材料配置按钮（冗余列表按钮上方） ----
        String rawConfigLabel = I18n.tr("litematlist.button.raw_material_config");
        int rawConfigBtnW = this.textRenderer.getWidth(rawConfigLabel) + 16;
        ButtonGeneric rawConfigBtn = new ButtonGeneric(this.width - rawConfigBtnW - 20, depthCtrlY - 24,
                rawConfigBtnW, 20, rawConfigLabel);
        rawConfigBtn.setRenderDefaultBackground(true);
        this.addButton(rawConfigBtn, (IButtonActionListener) (b, mb) -> {
            MinecraftClient.getInstance().setScreen(new RawMaterialConfigScreen(this));
        });

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // ---- 冗余列表按钮（最右侧） ----
//#endif
        String redundancyLabel = I18n.tr("litematlist.button.redundancy_list");
        int redundancyBtnW = this.textRenderer.getWidth(redundancyLabel) + 16;
        ButtonGeneric redundancyBtn = new ButtonGeneric(this.width - redundancyBtnW - 20, depthCtrlY,
                redundancyBtnW, 20, redundancyLabel);
        redundancyBtn.setRenderDefaultBackground(true);
        this.addButton(redundancyBtn, (IButtonActionListener) (b, mb) -> {
            MaterialDataStore.rawMaterialRedundancy.put(pathKey,
                    new ArrayList<>(redundancyItems));
            MaterialDataStore.savePersistence();
            MinecraftClient.getInstance().setScreen(
                    new RedundancyListScreen(this, schematicName, filePath));
        });

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // ---- 底部返回按钮 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // ---- 搴曢儴杩斿洖鎸夐挳 ----
//#endif
        int buttonY = this.height - 38;
        ButtonGeneric backBtn = new ButtonGeneric(this.width / 2 - 60, buttonY, 120, 20, I18n.tr("litematlist.button.back"));
        backBtn.setRenderDefaultBackground(true);
        this.addButton(backBtn, (IButtonActionListener) (b, mb) -> {
            saveCollapseState();
            MinecraftClient.getInstance().setScreen(parent);
        });

        if (goingToSubScreen) {
            goingToSubScreen = false;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 从子界面返回时，重新加载分析以确保忽略/替换状态同步
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            //  this.scrollX;
//#endif
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
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 真正离开界面时标记需要重新加载，确保材料列表变动后同步
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 鐪熸绂诲紑鐣岄潰鏃舵爣璁伴渶瑕侀噸鏂板姞杞斤紝纭繚鏉愭枡鍒楄〃鍙樺姩鍚庡悓姝?
//#endif
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

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 供 IgnoredItemsScreen 回调 */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 渚?IgnoredItemsScreen 鍥炶皟 */
//#endif
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

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Set<net.minecraft.item.Item> materialIgnored;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        Set<net.minecraft.item.Item> materialIgnored = filePath != null
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                ? MaterialDataStore.getIgnoredForPath(filePath) : Set.of();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
        List<MaterialEntry> entries = null;
        if (filePath != null) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            materialIgnored = MaterialDataStore.getIgnoredForPath(filePath);
//#endif
            if (SyncmaticaBridge.isSyncPath(filePath)) {
                // 「在Syncmatica认领的材料」：每次打开都向 Syncmatica 重新拉取最新认领数据
                entries = SyncmaticaBridge.loadClaimedMaterialEntries();
            } else {
                entries = MaterialListInjector.loadMaterialsCached(filePath);
                if (entries == null || entries.isEmpty()) {
                    Map<Path, List<MaterialEntry>> imported = MaterialDataStore.getImportedMaterials();
                    entries = imported.get(filePath);
                }
            }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 应用替换映射
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (entries != null) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                Path fp = filePath;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                entries = entries.stream()
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        .map(e -> {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            if (e.item() == null || e.isExact()) return e;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            Item replaced = MaterialDataStore.getReplacement(fp, e.item());
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            if (replaced != null && replaced != e.item()) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                return new MaterialEntry(replaced, e.blockName(), e.totalCount());
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            return e;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        })
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        .collect(Collectors.toList());
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
        } else {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 项目模式：忽略按子条目各自过滤（在 loadProjectEntries 内处理），
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 避免"任一来源忽略→整体移除"，改为"部分来源忽略→数量削减"
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            materialIgnored = new HashSet<>();
//#endif
            entries = loadProjectEntries(schematicName);
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 应用忽略过滤（替换后执行，确保替换后的物品也能被正确过滤）
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鍏堝簲鐢?MaterialDetailScreen 涓繚瀛樼殑鏇挎崲鏄犲皠锛岀‘淇濇浛鎹㈠悗鐨勭墿鍝佷篃鑳借蹇界暐杩囨护鍖归厤
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (filePath != null && entries != null) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            entries = entries.stream()
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    .map(e -> {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        if (e.item() == null || e.isExact()) return e;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        Item replaced = MaterialDataStore.getReplacement(filePath, e.item());
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        if (replaced != null && replaced != e.item()) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                            return new MaterialEntry(replaced, e.blockName(), e.totalCount());
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        return e;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    })
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    .collect(Collectors.toList());
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鍐嶅簲鐢ㄥ拷鐣ヨ繃婊わ紙鏇挎崲鍚庢墽琛岋紝纭繚鏇挎崲鍚庣殑鐗╁搧涔熻兘琚纭繃婊わ級
//#endif
        if (entries != null && !materialIgnored.isEmpty()) {
            entries = entries.stream()
                    .filter(e -> e.item() != null && !materialIgnored.contains(e.item()))
                    .collect(Collectors.toList());
        }

        if (entries != null && !entries.isEmpty()) {
            analyzing = true;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 在主线程执行分析，因为RecipeBookUtils需要访问客户端数据
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 鍦ㄤ富绾跨▼鎵ц鍒嗘瀽锛屽洜涓篟ecipeBookUtils闇€瑕佽闂鎴风鏁版嵁
//#endif
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
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 无论分析成功与否都必须复位，否则渲染将一直卡在"正在分析中"、搜索图标和树都不再显示
//#endif
                analyzing = false;
            }
        }

        if (initialLoad) {
            initialLoad = false;
        } else {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 非首次加载时恢复滚动位置
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 闈為娆″姞杞芥椂鎭㈠婊氬姩浣嶇疆
//#endif
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
                    Map<Path, List<MaterialEntry>> imported = MaterialDataStore.getImportedMaterials();
                    childEntries = imported.get(childPath);
                }
            }
            if (childEntries != null) {
                // 按子条目各自的忽略集合过滤：同一物品只忽略部分来源时，数量应削减而非整体移除
                Set<Item> childIgnored = childPath != null
                        ? MaterialDataStore.getIgnoredForPath(childPath) : Set.of();
                Set<String> childExactIgnored = childPath != null
                        ? MaterialDataStore.getExactIgnoredForPath(childPath) : Set.of();
                for (MaterialEntry entry : childEntries) {
                    if (entry.item() == null) continue;
                    // 精确物品（带非默认组件）：按精确ID独立聚合，blockName 保留精确ID，
                    // 使特殊纹饰陶罐能在原材料树中按 sherds 分解为瓦片；其余精确物品在分析阶段被丢弃
                    if (entry.isExact()) {
                        if (childExactIgnored.contains(entry.blockName())) continue;
                        String exactKey = entry.blockName();
                        MaterialEntry exactExisting = merged.get(exactKey);
                        if (exactExisting != null) {
                            merged.put(exactKey, new MaterialEntry(
                                    entry.item(), entry.blockName(),
                                    entry.totalCount() + exactExisting.totalCount(), 0, entry.exactStack()));
                        } else {
                            merged.put(exactKey, new MaterialEntry(
                                    entry.item(), entry.blockName(),
                                    entry.totalCount(), 0, entry.exactStack()));
                        }
                        continue;
                    }
                    // 应用替换映射，使树状图物品与【查看总材料列表】保持一致（跟随替换实时更新）
                    Item effectiveItem = childPath != null
                            ? MaterialDataStore.getReplacement(childPath, entry.item()) : entry.item();
                    // 忽略检查针对替换后的物品，与 ProjectSummaryScreen.isSourceIgnored 逻辑一致
                    if (childIgnored.contains(effectiveItem)) continue;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    String key = Registries.ITEM.getId(effectiveItem).toString();
//#elseif MC == 11701
//$$                    String key = Registry.ITEM.getId(effectiveItem).toString();
//#endif
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

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ==================== 布局计算（纯垂直堆叠，无斜率） ====================
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ==================== 甯冨眬璁＄畻锛堢函鍨傜洿鍫嗗彔锛屾棤鏂滅巼锛?====================
//#endif

    private void computeLayout() {
        rootLayouts.clear();
        if (treeRoots.isEmpty()) return;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 获取已保存的折叠路径
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鑾峰彇宸蹭繚瀛樼殑鎶樺彔璺緞
//#endif
        Set<String> collapsed = COLLAPSED_PATHS.get(pathKey);
        if (collapsed == null) collapsed = Collections.emptySet();

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 第一阶段：递归计算每个节点的subtreeHeight（折叠节点高度为NODE_HEIGHT）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 绗竴闃舵锛氶€掑綊璁＄畻姣忎釜鑺傜偣鐨剆ubtreeHeight锛堟姌鍙犺妭鐐归珮搴︿负NODE_HEIGHT锛?
//#endif
        for (RecipeTreeNode root : treeRoots) {
            NodeLayout layout = computeNodeLayout(root, 0, "", collapsed);
            rootLayouts.add(layout);
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 第二阶段：从上到下分配绝对位置，子节点垂直堆叠
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 绗簩闃舵锛氫粠涓婂埌涓嬪垎閰嶇粷瀵逛綅缃紝瀛愯妭鐐瑰瀭鐩村爢鍙?
//#endif
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

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 最大滚动距离随缩放档位变化
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // max scroll ranges follow the current zoom level (or flat layout when zoom disabled)
//#endif
        updateMaxScroll();
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 递归计算节点布局：子节点垂直堆叠，折叠节点的subtreeHeight为NODE_HEIGHT。
     */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /**
//$$     * 閫掑綊璁＄畻鑺傜偣甯冨眬锛氬瓙鑺傜偣鍨傜洿鍫嗗彔锛屾姌鍙犺妭鐐圭殑subtreeHeight涓篘ODE_HEIGHT銆?     */
//#endif
    private NodeLayout computeNodeLayout(RecipeTreeNode node, int depth, String path, Set<String> collapsed) {
        NodeLayout layout = new NodeLayout();
        layout.node = node;

        String currentPath = path.isEmpty() ? node.itemId : path + ">" + node.itemId;

        if (collapsed.contains(currentPath)) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 折叠的节点：高度仅为NODE_HEIGHT，不展开子节点
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 鎶樺彔鐨勮妭鐐癸細楂樺害浠呬负NODE_HEIGHT锛屼笉灞曞紑瀛愯妭鐐?
//#endif
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

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 分配绝对位置：子节点从父节点subtree顶部开始垂直堆叠。
     */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /**
//$$     * 鍒嗛厤缁濆浣嶇疆锛氬瓙鑺傜偣浠庣埗鑺傜偣subtree椤堕儴寮€濮嬪瀭鐩村爢鍙犮€?     */
//#endif
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

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ==================== 渲染 ====================
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ==================== tree search ====================
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    @Override
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        this.renderBackground(drawContext, mouseX, mouseY, delta);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 刷新搜索匹配列表（树数据或关键字变化时重建，并自动定位首个匹配项）
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        refreshSearchMatches();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 按钮
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        super.render(drawContext, mouseX, mouseY, delta);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // ---- 渲染开关按钮上的"开/关"彩色文字 ----
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 创造模式排序按钮
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            String fullLabel = creativeSortEnabled
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    ? I18n.tr("litematlist.button.creative_sort_on")
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    : I18n.tr("litematlist.button.creative_sort_off");
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            String prefix = fullLabel.substring(0, fullLabel.length() - 1);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            String state = fullLabel.substring(fullLabel.length() - 1);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            int prefixW = this.textRenderer.getWidth(prefix);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            int stateColor = creativeSortEnabled ? 0xFF55FF55 : 0xFFFF5555;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            int textStartX = creativeSortBtnX + (creativeSortBtnW - this.textRenderer.getWidth(fullLabel)) / 2;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawText(this.textRenderer, state, textStartX + prefixW, 39, stateColor, false);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 是否同步替换按钮
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            String fullLabel = syncReplacementEnabled
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    ? I18n.tr("litematlist.button.sync_replace_on")
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    : I18n.tr("litematlist.button.sync_replace_off");
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            String prefix = fullLabel.substring(0, fullLabel.length() - 1);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            String state = fullLabel.substring(fullLabel.length() - 1);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            int prefixW = this.textRenderer.getWidth(prefix);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            int stateColor = syncReplacementEnabled ? 0xFF55FF55 : 0xFFFF5555;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            int textStartX = syncReplaceBtnX + (syncReplaceBtnW - this.textRenderer.getWidth(fullLabel)) / 2;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawText(this.textRenderer, state, textStartX + prefixW, 39, stateColor, false);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // ---- 树状图搜索放大镜图标：在 analyzing 检查之前渲染，保证任何状态异常都不会让图标消失 ----
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        computeSearchLayout();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        boolean searchIconHovered = mouseX >= searchIconX && mouseX < searchIconX + SEARCH_ICON_SIZE
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                && mouseY >= searchIconY && mouseY < searchIconY + SEARCH_ICON_SIZE;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (searchIconHovered) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(searchIconX - 1, searchIconY - 1,
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    searchIconX + SEARCH_ICON_SIZE + 1, searchIconY + SEARCH_ICON_SIZE + 1, 0x40FFFFFF);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 21x21 纹理按 1:1 绘制
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, SEARCH_ICON,
//#elseif MC == 12101
//$$        drawContext.drawTexture( SEARCH_ICON,
//#elseif MC == 12104
//$$        drawContext.drawTexture(RenderLayer::getGuiTextured, SEARCH_ICON,
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                searchIconX, searchIconY, 0.0f, 0.0f, SEARCH_ICON_SIZE, SEARCH_ICON_SIZE,
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                SEARCH_ICON_SIZE, SEARCH_ICON_SIZE);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (analyzing) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawCenteredTextWithShadow(this.textRenderer,
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    I18n.tr("litematlist.raw_materials.analyzing"),
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    this.width / 2, (this.height - 100) / 2, 0xFFFFFF55);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            return;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (treeRoots.isEmpty()) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawCenteredTextWithShadow(this.textRenderer,
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    I18n.tr("litematlist.raw_materials.empty"),
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    this.width / 2, (this.height - 100) / 2, 0x80FFFFFF);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        } else {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 渲染树（整体缩放预览时应用矩阵缩放，命中检测使用换算后的世界坐标）
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            hoveredNode = null;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            hoveredRecipeNode = null;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            this.frameScreenMouseX = mouseX;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            this.frameScreenMouseY = mouseY;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            float zs = currentZoomScale();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            boolean zoomed = zs != 1.0F;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 缩放矩阵以屏幕原点为锚点会向原点收缩，先平移补偿使世界 y=CONTROLS_BOTTOM 恒对齐屏幕 y=CONTROLS_BOTTOM
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            float zsOffsetY = zoomed ? CONTROLS_BOTTOM * (1.0F - zs) : 0.0F;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            int wMouseX = zoomed ? (int) (mouseX / zs) : mouseX;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            int wMouseY = zoomed ? (int) ((mouseY - zsOffsetY) / zs) : mouseY;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            int treeAreaBottom = cullBottomW();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (zoomed) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                drawContext.getMatrices().pushMatrix();
//#elseif MC == 12101 || MC == 12104
//$$                drawContext.getMatrices().push();
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                drawContext.getMatrices().translate(0.0F, zsOffsetY);
//#elseif MC == 12101 || MC == 12104
//$$                drawContext.getMatrices().translate(0.0F, zsOffsetY, 0.0f);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                drawContext.getMatrices().scale(zs, zs);
//#elseif MC == 12101 || MC == 12104
//$$                drawContext.getMatrices().scale(zs, zs, 1.0f);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 绘制连接线
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            for (NodeLayout root : rootLayouts) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                renderConnectors(drawContext, root, wMouseX, wMouseY);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 绘制节点
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            for (NodeLayout root : rootLayouts) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                renderNodeTree(drawContext, root, wMouseX, wMouseY, treeAreaBottom);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (zoomed) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                drawContext.getMatrices().popMatrix();
//#elseif MC == 12101 || MC == 12104
//$$                drawContext.getMatrices().pop();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 配方图标悬停提示
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (hoveredRecipeNode != null && hoveredNode == null) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                RecipeTreeNode rn = hoveredRecipeNode.node;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (rn.alternativeRecipeTypes != null && !rn.alternativeRecipeTypes.isEmpty()) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    drawContext.drawTooltip(this.textRenderer,
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            Text.literal(I18n.tr("litematlist.recipe.alt_available")),
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            mouseX, mouseY);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 导出按钮悬停提示（多行）
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (mouseX >= exportRawBtnX && mouseX < exportRawBtnX + exportRawBtnW
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    && mouseY >= 34 && mouseY < 54) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                String[] lines = I18n.tr("litematlist.button.export_raw_tip").split("\n");
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                java.util.List<Text> tooltipLines = new java.util.ArrayList<>();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                for (String line : lines) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    tooltipLines.add(Text.literal(line));
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.drawTooltip(this.textRenderer, tooltipLines, mouseX, mouseY);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // ---- 层数控制渲染（最高图层，纯白色，在树之上渲染） ----
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 新布局: 标签 → 输入框 → [+-]图标
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        int fieldX = 60;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        int depthCtrlY = 34;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // "层数:" 标签（纯白色，无阴影）
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        String depthLabel = I18n.tr("litematlist.tree.depth_label");
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawText(this.textRenderer, depthLabel, 18, depthCtrlY + 4, 0xFFFFFFFF, false);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 输入框区域（先渲染，在图标左边）
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(fieldX - 2, depthCtrlY, fieldX + 26, depthCtrlY + 18, 0xFFFFFFFF);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(fieldX - 1, depthCtrlY + 1, fieldX + 25, depthCtrlY + 17, 0xFF111111);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (depthField != null) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            String txt = depthField.getText();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawText(this.textRenderer, txt, fieldX + 3, depthCtrlY + 4, 0xFFFFFFFF, false);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (depthField.isFocused() && System.currentTimeMillis() % 1000 < 500) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                int cp = depthField.getCursor();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                String bc = txt.substring(0, Math.min(cp, txt.length()));
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                int cx = fieldX + 3 + this.textRenderer.getWidth(bc);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.fill(cx, depthCtrlY + 3, cx + 1, depthCtrlY + 15, 0xFFFFFFFF);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 层数加减图标（在输入框后面）
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        int depthIconX = fieldX + 30;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        depthIconHovered = mouseX >= depthIconX && mouseX < depthIconX + DEPTH_ICON_SIZE
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                && mouseY >= depthCtrlY && mouseY < depthCtrlY + DEPTH_ICON_SIZE;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Identifier depthIcon = depthIconHovered ? DEPTH_HOVERED : DEPTH_NORMAL;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, depthIcon, depthIconX, depthCtrlY,
//#elseif MC == 12101
//$$        drawContext.drawTexture( depthIcon, depthIconX, depthCtrlY,
//#elseif MC == 12104
//$$        drawContext.drawTexture(RenderLayer::getGuiTextured, depthIcon, depthIconX, depthCtrlY,
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                0.0f, 0.0f, DEPTH_ICON_SIZE, DEPTH_ICON_SIZE, DEPTH_ICON_SIZE, DEPTH_ICON_SIZE);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // ---- 树状图搜索控件（搜索框/信息行/导航按钮，在树之上渲染） ----
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (this.searchVisible) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 输入框
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(searchFieldX - 2, searchFieldY - 1,
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    searchFieldX + SEARCH_FIELD_W + 2, searchFieldY + SEARCH_FIELD_H + 1, 0xFFFFFFFF);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(searchFieldX - 1, searchFieldY,
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    searchFieldX + SEARCH_FIELD_W + 1, searchFieldY + SEARCH_FIELD_H, 0xFF111111);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (searchField != null) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                String txt = searchField.getText();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                String shown = txt;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                int availW = SEARCH_FIELD_W - 8;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (this.textRenderer.getWidth(txt) > availW) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    shown = this.textRenderer.trimToWidth(txt, availW - this.textRenderer.getWidth("…")) + "…";
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.drawText(this.textRenderer, shown, searchFieldX + 3, searchFieldY + 3, 0xFFFFFFFF, false);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (searchField.isFocused() && System.currentTimeMillis() % 1000 < 500) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    int cp = Math.min(searchField.getCursor(), txt.length());
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    String bc = txt.substring(0, cp);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    int cx = searchFieldX + 3 + this.textRenderer.getWidth(bc);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    drawContext.fill(cx, searchFieldY + 2, cx + 1, searchFieldY + SEARCH_FIELD_H - 2, 0xFFFFFFFF);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 匹配信息 + 上下导航按钮（↑=上一个匹配项，↓=下一个匹配项）
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            int total = searchMatches.size();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            String info = total > 0
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    ? "共" + total + "个匹配项," + (searchIndex + 1) + "/" + total
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    : "共0个匹配项";
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            int infoW = this.textRenderer.getWidth(info);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 信息行以 searchNavX 为起点（图标靠右时整体左移，保证不越出屏幕）
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawText(this.textRenderer, info, searchNavX, searchNavY + 4, 0xFFFFFFFF, false);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (total > 0) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                searchUpBtnX = searchNavX + infoW + 8;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                searchDownBtnX = searchUpBtnX + SEARCH_NAV_BTN_SIZE + 4;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // ↑↓按钮超出屏幕右缘时换行到信息行下方（第三排），左对齐信息行，避免按钮被截断出屏
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (searchDownBtnX + SEARCH_NAV_BTN_SIZE + 2 > this.width - 2) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    searchUpBtnX = searchNavX;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    searchDownBtnX = searchUpBtnX + SEARCH_NAV_BTN_SIZE + 4;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    searchBtnY = searchNavY + 15;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                } else {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    searchBtnY = searchNavY;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawSearchNavButton(drawContext, searchUpBtnX, searchBtnY, "↑", mouseX, mouseY);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawSearchNavButton(drawContext, searchDownBtnX, searchBtnY, "↓", mouseX, mouseY);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // ---- 右下角缩放控制条（【-】百分比【+】；【配置】-【通用】开关） ----
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (this.treeZoomEnabled) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawZoomBar(drawContext, mouseX, mouseY);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 右下角斜体提示：界面可用鼠标拖动
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        String dragHint = I18n.tr("litematlist.raw_materials.drag_hint");
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        int hintW = this.textRenderer.getWidth(dragHint);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawText(this.textRenderer, Text.literal(dragHint).styled(s -> s.withItalic(true)),
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                this.width - hintW - 10, this.height - 18, 0x60FFFFFF, false);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // ---- 滚动条渲染 ----
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        renderScrollbars(drawContext, mouseX, mouseY);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // ---- 左下角统计信息 ----
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        int[] counts = countNodes();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        String stats = I18n.tr("litematlist.raw_materials.stats", counts[0], counts[1]);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawTextWithShadow(this.textRenderer, stats, 10, this.height - 21, 0x80FFFFFF);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ==================== 树状图搜索 ====================
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 计算搜索控件的当前布局：输入框位置（右侧优先、放不下转图标下方）、信息行位置 */
//#endif
    private void computeSearchLayout() {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 首次布局：图标默认定位到屏幕右侧（贴近滚动条但不遮挡）
//#endif
        if (searchIconX < 0) {
            searchIconX = Math.max(2, this.width - SCROLLBAR_THICKNESS - 4 - SEARCH_ICON_SIZE);
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 输入框默认在放大镜右侧；右侧展开会超出屏幕时改放图标下方
//#endif
        if (searchIconX + SEARCH_ICON_SIZE + 6 + SEARCH_FIELD_W + 2 <= this.width - 2) {
            searchFieldX = searchIconX + SEARCH_ICON_SIZE + 6;
            searchFieldY = searchIconY + (SEARCH_ICON_SIZE - SEARCH_FIELD_H) / 2;
        } else {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 图标下方展开：输入框同样不能越出屏幕左右边界
//#endif
            searchFieldX = MathHelper.clamp(searchIconX, 0,
                    Math.max(0, this.width - SEARCH_FIELD_W - 2));
            searchFieldY = searchIconY + SEARCH_ICON_SIZE + 4;
        }
        searchNavY = Math.max(searchIconY + SEARCH_ICON_SIZE + 4, searchFieldY + SEARCH_FIELD_H + 4);
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 信息行与输入框最左侧严格对齐（右缘超出屏幕时允许越界显示，跟随输入框移动）
//#endif
        searchNavX = searchFieldX;
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 拖动时约束搜索图标位置：只按图标自身体积计算，左右不超出屏幕（且不遮右侧滚动条），上下不越过滚动条的高度范围 */
//#endif
    private void clampSearchPosition() {
        int minX = 2;
        int maxX = this.width - SCROLLBAR_THICKNESS - 4 - SEARCH_ICON_SIZE;
        searchIconX = MathHelper.clamp(searchIconX, minX, Math.max(minX, maxX));
        int minY = CONTROLS_BOTTOM;
        int maxY = (this.height - TREE_BOTTOM_OFFSET) - SEARCH_ICON_SIZE;
        searchIconY = MathHelper.clamp(searchIconY, minY, Math.max(minY, maxY));
        computeSearchLayout();
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ==================== 树状图整体缩放 ====================
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ==================== tree zoom ====================
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 当前生效的缩放倍率（100% 为原大小） */
//#endif
    private float currentZoomScale() {
        return this.treeZoomEnabled ? treeZoomPercent / 100.0F : 1.0F;
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 当前持久化的缩放百分比（供 MaterialDetailScreen 写 persistence.json 用） */
//#endif
    public static int getTreeZoomPercent() {
        return treeZoomPercent;
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 从持久化数据恢复缩放百分比，并归一化到合法档位 */
//#endif
    public static void setTreeZoomPercent(int percent) {
        int clamped = Math.max(ZOOM_MIN_PERCENT, Math.min(ZOOM_MAX_PERCENT, percent));
        int steps = (clamped - ZOOM_MIN_PERCENT + ZOOM_STEP_PERCENT / 2) / ZOOM_STEP_PERCENT;
        treeZoomPercent = Math.min(ZOOM_MAX_PERCENT, ZOOM_MIN_PERCENT + steps * ZOOM_STEP_PERCENT);
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 重置为默认档位（切换世界时调用，随后由新世界持久化数据覆盖） */
//#endif
    public static void resetTreeZoomPercent() {
        treeZoomPercent = ZOOM_DEFAULT_PERCENT;
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 可视区域右边界（世界坐标；缩放时按比例缩小可视范围） */
//#endif
    private int cullRightW() {
        float zs = currentZoomScale();
        return zs != 1.0F ? (int) (this.width / zs) : this.width;
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 可视区域上边界（世界坐标；缩放矩阵已含平移锚点补偿，上边界恒等于控件区下沿） */
//#endif
    private int cullTopW() {
        return CONTROLS_BOTTOM;
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 可视区域下边界（世界坐标；扣除缩放矩阵的平移补偿后再按比例换算） */
//#endif
    private int cullBottomW() {
        float zs = currentZoomScale();
        int bottom = this.height - TREE_BOTTOM_OFFSET;
        return zs != 1.0F ? Math.max(cullTopW() + 1, (int) ((bottom - CONTROLS_BOTTOM * (1.0F - zs)) / zs)) : bottom;
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 重算最大滚动距离（随缩放档位变化），并把当前滚动值夹回合法区间 */
//#endif
    private void updateMaxScroll() {
        float zs = currentZoomScale();
        if (this.treeZoomEnabled) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 缩放状态开启：树可在屏幕内整体平移，但左右两侧均不能被拖出屏幕。
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 内容世界范围 [0, totalTreeWidth]，渲染时在此基础上再加 TREE_LEFT_MARGIN。
//#endif
            int viewW = Math.max(1, (int) (this.width / zs));
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            double a = TREE_LEFT_MARGIN + totalTreeWidth - viewW; // 树右缘贴屏幕右缘
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            double a = TREE_LEFT_MARGIN + totalTreeWidth - viewW;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            double b = TREE_LEFT_MARGIN;                          // 树左缘贴屏幕左缘
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            double b = TREE_LEFT_MARGIN;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            minScrollX = Math.min(a, b); // 树整体小于视口（缩小档位）时 a<b：负值下限允许整体平移
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            minScrollX = Math.min(a, b);
//#endif
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

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 缩放渲染中以屏幕坐标绘制不随缩放放大的节点悬浮提示 */
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void drawTooltipFixed(DrawContext ctx, Text text) {
//#elseif MC == 11701 || MC == 11904
//$$    private void drawTooltipFixed(MatrixStack matrices, Text text) {
//#endif
        float zs = currentZoomScale();
        if (zs != 1.0F) {
//#if MC == 12108 || MC == 12110 || MC == 12111
            ctx.getMatrices().pushMatrix();
//#elseif MC == 11701 || MC == 11904
//$$            matrices.push();
//#elseif MC == 12001 || MC == 12101 || MC == 12104
//$$            ctx.getMatrices().push();
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            ctx.getMatrices().translate(0.0F, -CONTROLS_BOTTOM * (1.0F - zs) / zs);
//#elseif MC == 11701 || MC == 11904
//$$            matrices.translate(0.0D, (double) (-CONTROLS_BOTTOM * (1.0F - zs) / zs), 0.0D);
//#elseif MC == 12001
//$$            ctx.getMatrices().translate(0.0D, (double) (-CONTROLS_BOTTOM * (1.0F - zs) / zs), 0.0D);
//#elseif MC == 12101 || MC == 12104
//$$            ctx.getMatrices().translate(0.0F, -CONTROLS_BOTTOM * (1.0F - zs) / zs, 0.0f);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            ctx.getMatrices().scale(1.0F / zs, 1.0F / zs);
//#elseif MC == 11701 || MC == 11904
//$$            matrices.scale(1.0F / zs, 1.0F / zs, 1.0F);
//#elseif MC == 12001
//$$            ctx.getMatrices().scale(1.0F / zs, 1.0F / zs, 1.0F);
//#elseif MC == 12101 || MC == 12104
//$$            ctx.getMatrices().scale(1.0F / zs, 1.0F / zs, 1.0f);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            ctx.drawTooltip(this.textRenderer, text, frameScreenMouseX, frameScreenMouseY);
//#elseif MC == 11701 || MC == 11904
//$$            this.renderTooltip(matrices, text, frameScreenMouseX, frameScreenMouseY);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            ctx.getMatrices().popMatrix();
//#elseif MC == 11701 || MC == 11904
//$$            matrices.pop();
//#elseif MC == 12001 || MC == 12101 || MC == 12104
//$$            ctx.getMatrices().pop();
//#endif
        } else {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            ctx.drawTooltip(this.textRenderer, text, frameScreenMouseX, frameScreenMouseY);
//#elseif MC == 11701 || MC == 11904
//$$            this.renderTooltip(matrices, text, frameScreenMouseX, frameScreenMouseY);
//#endif
        }
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 计算右下角缩放控制条布局：【-】 xxx% 【+】 */
//#endif
    private void computeZoomBarLayout() {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        int pctW = this.textRenderer.getWidth("200%"); // 按最大档位预留宽度，避免切换档位时按钮左右跳动
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        int pctW = this.textRenderer.getWidth("200%");
//#endif
        int totalW = ZOOM_BTN_SIZE * 2 + 12 + pctW;
        zoomOutBtnX = this.width - 8 - totalW;
        zoomInBtnX = zoomOutBtnX + ZOOM_BTN_SIZE + 12 + pctW;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        zoomBarY = this.height - TREE_BOTTOM_OFFSET - 2; // 右下角，避开水平滚动条与拖动提示
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        zoomBarY = this.height - TREE_BOTTOM_OFFSET - 2;
//#endif
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 绘制右下角缩放控制条 */
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void drawZoomBar(DrawContext ctx, int mouseX, int mouseY) {
//#elseif MC == 11701 || MC == 11904
//$$    private void drawZoomBar(MatrixStack matrices, int mouseX, int mouseY) {
//#endif
        computeZoomBarLayout();
        String pct = treeZoomPercent + "%";
        int pctW = this.textRenderer.getWidth(pct);
        int pctX = (zoomOutBtnX + ZOOM_BTN_SIZE + zoomInBtnX) / 2 - pctW / 2;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        ctx.drawText(this.textRenderer, pct, pctX, zoomBarY + (ZOOM_BTN_SIZE - 8) / 2, 0xFFFFFFFF, false);
//#elseif MC == 11701 || MC == 11904
//$$        this.textRenderer.draw(matrices, pct, pctX, zoomBarY + (ZOOM_BTN_SIZE - 8) / 2, 0xFFFFFFFF);
//#endif

        boolean outHover = mouseX >= zoomOutBtnX - 2 && mouseX < zoomOutBtnX + ZOOM_BTN_SIZE + 2
                && mouseY >= zoomBarY - 2 && mouseY < zoomBarY + ZOOM_BTN_SIZE + 2;
        if (outHover) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            ctx.fill(zoomOutBtnX - 2, zoomBarY - 2, zoomOutBtnX + ZOOM_BTN_SIZE + 2,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    zoomBarY + ZOOM_BTN_SIZE + 2, 0x40FFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, zoomOutBtnX - 2, zoomBarY - 2, zoomOutBtnX + ZOOM_BTN_SIZE + 2, zoomBarY + ZOOM_BTN_SIZE + 2, 0x40FFFFFF);
//#endif
        }
//#if MC == 12108 || MC == 12110 || MC == 12111
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, ZOOM_OUT_ICON,
//#elseif MC == 12001
//$$        ctx.drawTexture(ZOOM_OUT_ICON, zoomOutBtnX, zoomBarY,
//#elseif MC == 12101
//$$        ctx.drawTexture( ZOOM_OUT_ICON,
//#elseif MC == 12104
//$$        ctx.drawTexture(RenderLayer::getGuiTextured, ZOOM_OUT_ICON,
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                zoomOutBtnX, zoomBarY, 0.0f, 0.0f, ZOOM_BTN_SIZE, ZOOM_BTN_SIZE, ZOOM_BTN_SIZE, ZOOM_BTN_SIZE);
//#elseif MC == 12001
//$$                0.0f, 0.0f, ZOOM_BTN_SIZE, ZOOM_BTN_SIZE, ZOOM_BTN_SIZE, ZOOM_BTN_SIZE);
//#endif
//#if MC == 11701 || MC == 11904
//$$        RenderSystem.setShaderTexture(0, ZOOM_OUT_ICON); DrawableHelper.drawTexture(matrices, zoomOutBtnX, zoomBarY, 0.0f, 0.0f, ZOOM_BTN_SIZE, ZOOM_BTN_SIZE, ZOOM_BTN_SIZE, ZOOM_BTN_SIZE);
//#endif

        boolean inHover = mouseX >= zoomInBtnX - 2 && mouseX < zoomInBtnX + ZOOM_BTN_SIZE + 2
                && mouseY >= zoomBarY - 2 && mouseY < zoomBarY + ZOOM_BTN_SIZE + 2;
        if (inHover) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            ctx.fill(zoomInBtnX - 2, zoomBarY - 2, zoomInBtnX + ZOOM_BTN_SIZE + 2,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    zoomBarY + ZOOM_BTN_SIZE + 2, 0x40FFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, zoomInBtnX - 2, zoomBarY - 2, zoomInBtnX + ZOOM_BTN_SIZE + 2, zoomBarY + ZOOM_BTN_SIZE + 2, 0x40FFFFFF);
//#endif
        }
//#if MC == 12108 || MC == 12110 || MC == 12111
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, ZOOM_IN_ICON,
//#elseif MC == 12001
//$$        ctx.drawTexture(ZOOM_IN_ICON, zoomInBtnX, zoomBarY,
//#elseif MC == 12101
//$$        ctx.drawTexture( ZOOM_IN_ICON,
//#elseif MC == 12104
//$$        ctx.drawTexture(RenderLayer::getGuiTextured, ZOOM_IN_ICON,
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                zoomInBtnX, zoomBarY, 0.0f, 0.0f, ZOOM_BTN_SIZE, ZOOM_BTN_SIZE, ZOOM_BTN_SIZE, ZOOM_BTN_SIZE);
//#elseif MC == 12001
//$$                0.0f, 0.0f, ZOOM_BTN_SIZE, ZOOM_BTN_SIZE, ZOOM_BTN_SIZE, ZOOM_BTN_SIZE);
//#endif
//#if MC == 11701 || MC == 11904
//$$        RenderSystem.setShaderTexture(0, ZOOM_IN_ICON); DrawableHelper.drawTexture(matrices, zoomInBtnX, zoomBarY, 0.0f, 0.0f, ZOOM_BTN_SIZE, ZOOM_BTN_SIZE, ZOOM_BTN_SIZE, ZOOM_BTN_SIZE);
//#endif
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 绘制正方形导航按钮（↑/↓），悬停时变亮 */
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void drawSearchNavButton(DrawContext ctx, int bx, int by, String arrow,
//#elseif MC == 11701 || MC == 11904
//$$    private void drawSearchNavButton(MatrixStack matrices, int bx, int by, String arrow,
//#endif
                                     int mouseX, int mouseY) {
        boolean hovered = mouseX >= bx && mouseX < bx + SEARCH_NAV_BTN_SIZE
                && mouseY >= by && mouseY < by + SEARCH_NAV_BTN_SIZE;
        int bg = hovered ? 0xFF555555 : 0xFF303030;
        int border = hovered ? 0xFFCCCCCC : 0xFF888888;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        ctx.fill(bx, by, bx + SEARCH_NAV_BTN_SIZE, by + SEARCH_NAV_BTN_SIZE, bg);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        ctx.fill(bx, by, bx + SEARCH_NAV_BTN_SIZE, by + 1, border);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        ctx.fill(bx, by + SEARCH_NAV_BTN_SIZE - 1, bx + SEARCH_NAV_BTN_SIZE, by + SEARCH_NAV_BTN_SIZE, border);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        ctx.fill(bx, by, bx + 1, by + SEARCH_NAV_BTN_SIZE, border);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        ctx.fill(bx + SEARCH_NAV_BTN_SIZE - 1, by, bx + SEARCH_NAV_BTN_SIZE, by + SEARCH_NAV_BTN_SIZE, border);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        ctx.drawCenteredTextWithShadow(this.textRenderer, arrow, bx + SEARCH_NAV_BTN_SIZE / 2,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                by + (SEARCH_NAV_BTN_SIZE - 8) / 2, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, bx, by, bx + SEARCH_NAV_BTN_SIZE, by + SEARCH_NAV_BTN_SIZE, bg);
//#endif
//#if MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, bx, by, bx + SEARCH_NAV_BTN_SIZE, by + 1, border);
//#endif
//#if MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, bx, by + SEARCH_NAV_BTN_SIZE - 1, bx + SEARCH_NAV_BTN_SIZE, by + SEARCH_NAV_BTN_SIZE, border);
//#endif
//#if MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, bx, by, bx + 1, by + SEARCH_NAV_BTN_SIZE, border);
//#endif
//#if MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, bx + SEARCH_NAV_BTN_SIZE - 1, by, bx + SEARCH_NAV_BTN_SIZE, by + SEARCH_NAV_BTN_SIZE, border);
//#endif
//#if MC == 11701 || MC == 11904
//$$        this.textRenderer.drawWithShadow(matrices, arrow, bx + SEARCH_NAV_BTN_SIZE / 2 - this.textRenderer.getWidth(arrow) / 2, by + (SEARCH_NAV_BTN_SIZE - 8) / 2, 0xFFFFFFFF);
//#endif
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 每帧刷新匹配列表：收集树中所有可见节点（含中间节点），按屏幕"从左到右、从上到下"排序后过滤出名称包含关键字的项 */
//#endif
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
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 先收集所有可见节点（含带配方的中间节点，如海晶灯），再按屏幕坐标排序（x 优先、y 其次），实现"从左到右、从上到下"
//#endif
            List<NodeLayout> allNodes = new ArrayList<>();
            for (NodeLayout root : rootLayouts) {
                collectVisibleNodes(root, allNodes);
            }
            allNodes.sort((a, b) -> a.x != b.x
                    ? Integer.compare(a.x, b.x)
                    : Integer.compare(a.y, b.y));
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 纯数字关键字：匹配节点数量（数量文本包含该数字）；否则匹配中文名称或英文物品ID
//#endif
            boolean numericKw = kw.matches("\\d+");
            for (NodeLayout node : allNodes) {
                boolean hit;
                if (numericKw) {
                    hit = String.valueOf(node.node.count).contains(kw);
                } else {
                    String name = node.node.itemId;
                    Item item = node.node.getItem();
                    if (item != null) name = item.getName().getString();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    // 中文名 / 拼音全拼（xiangmumuban）/ 拼音首字母（xmmb）/ 英文ID
//#endif
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

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 自动滚动视口，使匹配节点可见（居中显示） */
//#endif
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

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ==================== 节点渲染 ====================
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    // ==================== 娓叉煋 ====================
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    @Override
//#endif
//#if MC == 11701 || MC == 11904
//$$    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
//#elseif MC == 12001
//$$    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
//#endif
//#if MC == 11701 || MC == 11904
//$$        this.renderBackground(matrices);
//#elseif MC == 12001
//$$        this.renderBackground(drawContext);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        refreshSearchMatches();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鎸夐挳
//#endif
//#if MC == 11701 || MC == 11904
//$$        super.render(matrices, mouseX, mouseY, delta);
//#elseif MC == 12001
//$$        super.render(drawContext, mouseX, mouseY, delta);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        // ---- 娓叉煋寮€鍏虫寜閽笂鐨?寮€/鍏?褰╄壊鏂囧瓧 ----
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鍒涢€犳ā寮忔帓搴忔寜閽?
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            String fullLabel = creativeSortEnabled
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    ? I18n.tr("litematlist.button.creative_sort_on")
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    : I18n.tr("litematlist.button.creative_sort_off");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            String prefix = fullLabel.substring(0, fullLabel.length() - 1);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            String state = fullLabel.substring(fullLabel.length() - 1);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            int prefixW = this.textRenderer.getWidth(prefix);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            int stateColor = creativeSortEnabled ? 0xFF55FF55 : 0xFFFF5555;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            int textStartX = creativeSortBtnX + (creativeSortBtnW - this.textRenderer.getWidth(fullLabel)) / 2;
//#endif
//#if MC == 11701 || MC == 11904
//$$            this.textRenderer.draw(matrices, state, textStartX + prefixW, 39, stateColor);
//#elseif MC == 12001
//$$            drawContext.drawText(this.textRenderer, state, textStartX + prefixW, 39, stateColor, false);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鏄惁鍚屾鏇挎崲鎸夐挳
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            String fullLabel = syncReplacementEnabled
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    ? I18n.tr("litematlist.button.sync_replace_on")
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    : I18n.tr("litematlist.button.sync_replace_off");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            String prefix = fullLabel.substring(0, fullLabel.length() - 1);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            String state = fullLabel.substring(fullLabel.length() - 1);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            int prefixW = this.textRenderer.getWidth(prefix);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            int stateColor = syncReplacementEnabled ? 0xFF55FF55 : 0xFFFF5555;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            int textStartX = syncReplaceBtnX + (syncReplaceBtnW - this.textRenderer.getWidth(fullLabel)) / 2;
//#endif
//#if MC == 11701 || MC == 11904
//$$            this.textRenderer.draw(matrices, state, textStartX + prefixW, 39, stateColor);
//#elseif MC == 12001
//$$            drawContext.drawText(this.textRenderer, state, textStartX + prefixW, 39, stateColor, false);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        // ---- tree view search magnifier icon: render before analyzing check ----
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        computeSearchLayout();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        boolean searchIconHovered = mouseX >= searchIconX && mouseX < searchIconX + SEARCH_ICON_SIZE
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                && mouseY >= searchIconY && mouseY < searchIconY + SEARCH_ICON_SIZE;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (searchIconHovered) {
//#endif
//#if MC == 12001
//$$            drawContext.fill(searchIconX - 1, searchIconY - 1,
//#endif
//#if MC == 12001
//$$                    searchIconX + SEARCH_ICON_SIZE + 1, searchIconY + SEARCH_ICON_SIZE + 1, 0x40FFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, searchIconX - 1, searchIconY - 1, searchIconX + SEARCH_ICON_SIZE + 1, searchIconY + SEARCH_ICON_SIZE + 1, 0x40FFFFFF);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        }
//#endif
//#if MC == 12001
//$$        drawContext.drawTexture(SEARCH_ICON,
//#endif
//#if MC == 12001
//$$                searchIconX, searchIconY, 0.0f, 0.0f, SEARCH_ICON_SIZE, SEARCH_ICON_SIZE,
//#endif
//#if MC == 12001
//$$                SEARCH_ICON_SIZE, SEARCH_ICON_SIZE);
//#endif
//#if MC == 11701 || MC == 11904
//$$        RenderSystem.setShaderTexture(0, SEARCH_ICON); DrawableHelper.drawTexture(matrices, searchIconX, searchIconY, 0.0f, 0.0f, SEARCH_ICON_SIZE, SEARCH_ICON_SIZE, SEARCH_ICON_SIZE, SEARCH_ICON_SIZE);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (analyzing) {
//#endif
//#if MC == 12001
//$$            drawContext.drawCenteredTextWithShadow(this.textRenderer,
//#endif
//#if MC == 12001
//$$                    I18n.tr("litematlist.raw_materials.analyzing"),
//#endif
//#if MC == 12001
//$$                    this.width / 2, (this.height - 100) / 2, 0xFFFFFF55);
//#endif
//#if MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, I18n.tr("litematlist.raw_materials.analyzing"), this.width / 2 - this.textRenderer.getWidth(I18n.tr("litematlist.raw_materials.analyzing")) / 2, (this.height - 100) / 2, 0xFFFFFF55);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            return;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (treeRoots.isEmpty()) {
//#endif
//#if MC == 12001
//$$            drawContext.drawCenteredTextWithShadow(this.textRenderer,
//#endif
//#if MC == 12001
//$$                    I18n.tr("litematlist.raw_materials.empty"),
//#endif
//#if MC == 12001
//$$                    this.width / 2, (this.height - 100) / 2, 0x80FFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, I18n.tr("litematlist.raw_materials.empty"), this.width / 2 - this.textRenderer.getWidth(I18n.tr("litematlist.raw_materials.empty")) / 2, (this.height - 100) / 2, 0x80FFFFFF);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        } else {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            // 娓叉煋鏍?
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            hoveredNode = null;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            hoveredRecipeNode = null;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            this.frameScreenMouseX = mouseX;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            this.frameScreenMouseY = mouseY;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            float zs = currentZoomScale();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            boolean zoomed = zs != 1.0F;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            float zsOffsetY = zoomed ? CONTROLS_BOTTOM * (1.0F - zs) : 0.0F;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            int wMouseX = zoomed ? (int) (mouseX / zs) : mouseX;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            int wMouseY = zoomed ? (int) ((mouseY - zsOffsetY) / zs) : mouseY;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            int treeAreaBottom = cullBottomW();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            // 缁樺埗杩炴帴绾?
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (zoomed) {
//#endif
//#if MC == 11701 || MC == 11904
//$$                matrices.push();
//#elseif MC == 12001
//$$                drawContext.getMatrices().push();
//#endif
//#if MC == 11701 || MC == 11904
//$$                matrices.translate(0.0D, (double) zsOffsetY, 0.0D);
//#elseif MC == 12001
//$$                drawContext.getMatrices().translate(0.0D, (double) zsOffsetY, 0.0D);
//#endif
//#if MC == 11701 || MC == 11904
//$$                matrices.scale(zs, zs, 1.0F);
//#elseif MC == 12001
//$$                drawContext.getMatrices().scale(zs, zs, 1.0F);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            for (NodeLayout root : rootLayouts) {
//#endif
//#if MC == 11701 || MC == 11904
//$$                renderConnectors(matrices, root, wMouseX, wMouseY);
//#elseif MC == 12001
//$$                renderConnectors(drawContext, root, wMouseX, wMouseY);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            // 缁樺埗鑺傜偣
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            for (NodeLayout root : rootLayouts) {
//#endif
//#if MC == 11701 || MC == 11904
//$$                renderNodeTree(matrices, root, wMouseX, wMouseY, treeAreaBottom);
//#elseif MC == 12001
//$$                renderNodeTree(drawContext, root, wMouseX, wMouseY, treeAreaBottom);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (zoomed) {
//#endif
//#if MC == 11701 || MC == 11904
//$$                matrices.pop();
//#elseif MC == 12001
//$$                drawContext.getMatrices().pop();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            // 閰嶆柟鍥炬爣鎮仠鎻愮ず
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (hoveredRecipeNode != null && hoveredNode == null) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                RecipeTreeNode rn = hoveredRecipeNode.node;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                if (rn.alternativeRecipeTypes != null && !rn.alternativeRecipeTypes.isEmpty()) {
//#endif
//#if MC == 12001
//$$                    drawContext.drawTooltip(this.textRenderer,
//#endif
//#if MC == 12001
//$$                            Text.literal(I18n.tr("litematlist.recipe.alt_available")),
//#endif
//#if MC == 12001
//$$                            mouseX, mouseY);
//#endif
//#if MC == 11701
//$$                    this.renderTooltip(matrices, new LiteralText(I18n.tr("litematlist.recipe.alt_available")), mouseX, mouseY);
//#elseif MC == 11904
//$$                    this.renderTooltip(matrices, Text.literal(I18n.tr("litematlist.recipe.alt_available")), mouseX, mouseY);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            // 瀵煎嚭鎸夐挳鎮仠鎻愮ず锛堝琛岋級
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (mouseX >= exportRawBtnX && mouseX < exportRawBtnX + exportRawBtnW
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    && mouseY >= 34 && mouseY < 54) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                String[] lines = I18n.tr("litematlist.button.export_raw_tip").split("\n");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                java.util.List<Text> tooltipLines = new java.util.ArrayList<>();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                for (String line : lines) {
//#endif
//#if MC == 11701
//$$                    tooltipLines.add(new LiteralText(line));
//#elseif MC == 11904 || MC == 12001
//$$                    tooltipLines.add(Text.literal(line));
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                }
//#endif
//#if MC == 11701 || MC == 11904
//$$                this.renderTooltip(matrices, tooltipLines, mouseX, mouseY);
//#elseif MC == 12001
//$$                drawContext.drawTooltip(this.textRenderer, tooltipLines, mouseX, mouseY);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        // ---- 灞傛暟鎺у埗娓叉煋锛堟渶楂樺浘灞傦紝绾櫧鑹诧紝鍦ㄦ爲涔嬩笂娓叉煋锛?----
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鏂板竷灞€: 鏍囩 鈫?杈撳叆妗?鈫?[+-]鍥炬爣
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        int fieldX = 60;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        int depthCtrlY = 34;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        // "灞傛暟:" 鏍囩锛堢函鐧借壊锛屾棤闃村奖锛?
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        String depthLabel = I18n.tr("litematlist.tree.depth_label");
//#endif
//#if MC == 11701 || MC == 11904
//$$        this.textRenderer.draw(matrices, depthLabel, 18, depthCtrlY + 4, 0xFFFFFFFF);
//#elseif MC == 12001
//$$        drawContext.drawText(this.textRenderer, depthLabel, 18, depthCtrlY + 4, 0xFFFFFFFF, false);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        // 杈撳叆妗嗗尯鍩燂紙鍏堟覆鏌擄紝鍦ㄥ浘鏍囧乏杈癸級
//#endif
//#if MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, fieldX - 2, depthCtrlY, fieldX + 26, depthCtrlY + 18, 0xFFFFFFFF);
//#elseif MC == 12001
//$$        drawContext.fill(fieldX - 2, depthCtrlY, fieldX + 26, depthCtrlY + 18, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, fieldX - 1, depthCtrlY + 1, fieldX + 25, depthCtrlY + 17, 0xFF111111);
//#elseif MC == 12001
//$$        drawContext.fill(fieldX - 1, depthCtrlY + 1, fieldX + 25, depthCtrlY + 17, 0xFF111111);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (depthField != null) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            String txt = depthField.getText();
//#endif
//#if MC == 11701 || MC == 11904
//$$            this.textRenderer.draw(matrices, txt, fieldX + 3, depthCtrlY + 4, 0xFFFFFFFF);
//#elseif MC == 12001
//$$            drawContext.drawText(this.textRenderer, txt, fieldX + 3, depthCtrlY + 4, 0xFFFFFFFF, false);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (depthField.isFocused() && System.currentTimeMillis() % 1000 < 500) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                int cp = depthField.getCursor();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                String bc = txt.substring(0, Math.min(cp, txt.length()));
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                int cx = fieldX + 3 + this.textRenderer.getWidth(bc);
//#endif
//#if MC == 11701 || MC == 11904
//$$                DrawableHelper.fill(matrices, cx, depthCtrlY + 3, cx + 1, depthCtrlY + 15, 0xFFFFFFFF);
//#elseif MC == 12001
//$$                drawContext.fill(cx, depthCtrlY + 3, cx + 1, depthCtrlY + 15, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        // 灞傛暟鍔犲噺鍥炬爣锛堝湪杈撳叆妗嗗悗闈級
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        int depthIconX = fieldX + 30;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        depthIconHovered = mouseX >= depthIconX && mouseX < depthIconX + DEPTH_ICON_SIZE
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                && mouseY >= depthCtrlY && mouseY < depthCtrlY + DEPTH_ICON_SIZE;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        Identifier depthIcon = depthIconHovered ? DEPTH_HOVERED : DEPTH_NORMAL;
//#endif
//#if MC == 12001
//$$        drawContext.drawTexture(depthIcon, depthIconX, depthCtrlY,
//#endif
//#if MC == 12001
//$$                0.0f, 0.0f, DEPTH_ICON_SIZE, DEPTH_ICON_SIZE, DEPTH_ICON_SIZE, DEPTH_ICON_SIZE);
//#endif
//#if MC == 11701 || MC == 11904
//$$        RenderSystem.setShaderTexture(0, depthIcon); DrawableHelper.drawTexture(matrices, depthIconX, depthCtrlY, 0.0f, 0.0f, DEPTH_ICON_SIZE, DEPTH_ICON_SIZE, DEPTH_ICON_SIZE, DEPTH_ICON_SIZE);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        // ---- tree view search controls ----
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (this.searchVisible) {
//#endif
//#if MC == 12001
//$$            drawContext.fill(searchFieldX - 2, searchFieldY - 1,
//#endif
//#if MC == 12001
//$$                    searchFieldX + SEARCH_FIELD_W + 2, searchFieldY + SEARCH_FIELD_H + 1, 0xFFFFFFFF);
//#endif
//#if MC == 12001
//$$            drawContext.fill(searchFieldX - 1, searchFieldY,
//#endif
//#if MC == 12001
//$$                    searchFieldX + SEARCH_FIELD_W + 1, searchFieldY + SEARCH_FIELD_H, 0xFF111111);
//#endif
//#if MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, searchFieldX - 2, searchFieldY - 1, searchFieldX + SEARCH_FIELD_W + 2, searchFieldY + SEARCH_FIELD_H + 1, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, searchFieldX - 1, searchFieldY, searchFieldX + SEARCH_FIELD_W + 1, searchFieldY + SEARCH_FIELD_H, 0xFF111111);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (searchField != null) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                String txt = searchField.getText();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                String shown = txt;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                int availW = SEARCH_FIELD_W - 8;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                if (this.textRenderer.getWidth(txt) > availW) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    shown = this.textRenderer.trimToWidth(txt, availW - this.textRenderer.getWidth("\u2026")) + "\u2026";
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                }
//#endif
//#if MC == 11701 || MC == 11904
//$$                this.textRenderer.draw(matrices, shown, searchFieldX + 3, searchFieldY + 3, 0xFFFFFFFF);
//#elseif MC == 12001
//$$                drawContext.drawText(this.textRenderer, shown, searchFieldX + 3, searchFieldY + 3, 0xFFFFFFFF, false);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                if (searchField.isFocused() && System.currentTimeMillis() % 1000 < 500) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    int cp = Math.min(searchField.getCursor(), txt.length());
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    String bc = txt.substring(0, cp);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    int cx = searchFieldX + 3 + this.textRenderer.getWidth(bc);
//#endif
//#if MC == 11701 || MC == 11904
//$$                    DrawableHelper.fill(matrices, cx, searchFieldY + 2, cx + 1, searchFieldY + SEARCH_FIELD_H - 2, 0xFFFFFFFF);
//#elseif MC == 12001
//$$                    drawContext.fill(cx, searchFieldY + 2, cx + 1, searchFieldY + SEARCH_FIELD_H - 2, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            int total = searchMatches.size();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            String info = total > 0
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    ? "\u5171" + total + "\u4e2a\u5339\u914d\u9879," + (searchIndex + 1) + "/" + total
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    : "\u51710\u4e2a\u5339\u914d\u9879";
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            int infoW = this.textRenderer.getWidth(info);
//#endif
//#if MC == 11701 || MC == 11904
//$$            this.textRenderer.draw(matrices, info, searchNavX, searchNavY + 4, 0xFFFFFFFF);
//#elseif MC == 12001
//$$            drawContext.drawText(this.textRenderer, info, searchNavX, searchNavY + 4, 0xFFFFFFFF, false);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (total > 0) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                searchUpBtnX = searchNavX + infoW + 8;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                searchDownBtnX = searchUpBtnX + SEARCH_NAV_BTN_SIZE + 4;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                // ↑↓按钮超出屏幕右缘时换行到信息行下方（第三排），左对齐信息行，避免按钮被截断出屏
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                if (searchDownBtnX + SEARCH_NAV_BTN_SIZE + 2 > this.width - 2) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    searchUpBtnX = searchNavX;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    searchDownBtnX = searchUpBtnX + SEARCH_NAV_BTN_SIZE + 4;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    searchBtnY = searchNavY + 15;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                } else {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    searchBtnY = searchNavY;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                }
//#endif
//#if MC == 11701 || MC == 11904
//$$                drawSearchNavButton(matrices, searchUpBtnX, searchBtnY, "\u2191", mouseX, mouseY);
//#elseif MC == 12001
//$$                drawSearchNavButton(drawContext, searchUpBtnX, searchBtnY, "\u2191", mouseX, mouseY);
//#endif
//#if MC == 11701 || MC == 11904
//$$                drawSearchNavButton(matrices, searchDownBtnX, searchBtnY, "\u2193", mouseX, mouseY);
//#elseif MC == 12001
//$$                drawSearchNavButton(drawContext, searchDownBtnX, searchBtnY, "\u2193", mouseX, mouseY);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (this.treeZoomEnabled) {
//#endif
//#if MC == 11701 || MC == 11904
//$$            drawZoomBar(matrices, mouseX, mouseY);
//#elseif MC == 12001
//$$            drawZoomBar(drawContext, mouseX, mouseY);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鍙充笅瑙掓枩浣撴彁绀猴細鐣岄潰鍙敤榧犳爣鎷栧姩
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        String dragHint = I18n.tr("litematlist.raw_materials.drag_hint");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        int hintW = this.textRenderer.getWidth(dragHint);
//#endif
//#if MC == 12001
//$$        drawContext.drawText(this.textRenderer, Text.literal(dragHint).styled(s -> s.withItalic(true)),
//#endif
//#if MC == 12001
//$$                this.width - hintW - 10, this.height - 18, 0x60FFFFFF, false);
//#endif
//#if MC == 11701
//$$        this.textRenderer.draw(matrices, new LiteralText(dragHint).styled(s -> s.withItalic(true)), this.width - hintW - 10, this.height - 18, 0x60FFFFFF);
//#elseif MC == 11904
//$$        this.textRenderer.draw(matrices, Text.literal(dragHint).styled(s -> s.withItalic(true)), this.width - hintW - 10, this.height - 18, 0x60FFFFFF);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        // ---- 婊氬姩鏉℃覆鏌?----
//#endif
//#if MC == 11701 || MC == 11904
//$$        renderScrollbars(matrices, mouseX, mouseY);
//#elseif MC == 12001
//$$        renderScrollbars(drawContext, mouseX, mouseY);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        // ---- 宸︿笅瑙掔粺璁′俊鎭?----
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        int[] counts = countNodes();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        String stats = I18n.tr("litematlist.raw_materials.stats", counts[0], counts[1]);
//#endif
//#if MC == 11701 || MC == 11904
//$$        this.textRenderer.drawWithShadow(matrices, stats, 10, this.height - 21, 0x80FFFFFF);
//#elseif MC == 12001
//$$        drawContext.drawTextWithShadow(this.textRenderer, stats, 10, this.height - 21, 0x80FFFFFF);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    // ==================== 鑺傜偣娓叉煋 ====================
//#endif

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void renderNodeTree(DrawContext ctx, NodeLayout layout,
//#elseif MC == 11701 || MC == 11904
//$$    private void renderNodeTree(MatrixStack matrices, NodeLayout layout,
//#endif
                                int mouseX, int mouseY, int treeAreaBottom) {
        int screenX = layout.x - (int) scrollX;
        int nodeCenterY = layout.y - (int) scrollY;
        int screenY = nodeCenterY - NODE_HEIGHT / 2;

        if (screenX + NODE_WIDTH < 0 || screenX > cullRightW()
                || screenY + NODE_HEIGHT < cullTopW() || screenY > treeAreaBottom) {
            if (!layout.collapsed) {
                for (NodeLayout child : layout.children) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    renderNodeTree(ctx, child, mouseX, mouseY, treeAreaBottom);
//#elseif MC == 11701 || MC == 11904
//$$                    renderNodeTree(matrices, child, mouseX, mouseY, treeAreaBottom);
//#endif
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

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        ctx.fill(screenX, screenY, screenX + NODE_WIDTH, screenY + NODE_HEIGHT, bgColor);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, screenX, screenY, screenX + NODE_WIDTH, screenY + NODE_HEIGHT, bgColor);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        ctx.fill(screenX, screenY, screenX + 3, screenY + NODE_HEIGHT, borderColor);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, screenX, screenY, screenX + 3, screenY + NODE_HEIGHT, borderColor);
//#endif

        var item = layout.node.getItem();
        ItemStack renderStack = layout.node.exactStack != null ? layout.node.exactStack : (item != null ? new ItemStack(item) : ItemStack.EMPTY);
        if (!renderStack.isEmpty()) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            ctx.drawItem(renderStack, screenX + 6, screenY + 2);
//#elseif MC == 11701
//$$            MinecraftClient.getInstance().getItemRenderer().renderInGui(renderStack, screenX + 6, screenY + 2);
//#elseif MC == 11904
//$$            MinecraftClient.getInstance().getItemRenderer().renderInGui(matrices, renderStack, screenX + 6, screenY + 2);
//#endif
        }

        String fullName = layout.node.itemId;
        if (layout.node.exactStack != null) {
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            fullName = layout.node.exactStack.getFormattedName().getString();
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$            fullName = layout.node.exactStack.getName().getString();
//#endif
        } else if (item != null) {
            fullName = item.getName().getString();
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 数量紧挨名称，折叠图标在最右边；通配物品显示齿轮替换图标
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鏁伴噺绱ф尐鍚嶇О锛屾姌鍙犲浘鏍囧湪鏈€鍙宠竟锛涢€氶厤鐗╁搧鏄剧ず榻胯疆鏇挎崲鍥炬爣
//#endif
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

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 名称可用宽度 = 到折叠图标/右边缘 - 图标位置 - 数量宽度 - 间距
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鍚嶇О鍙敤瀹藉害 = 鍒版姌鍙犲浘鏍?鍙宠竟缂?- 鍥炬爣浣嶇疆 - 鏁伴噺瀹藉害 - 闂磋窛
//#endif
        int nameStartX = screenX + 26;
        int maxAvailWidth = nameEndX - nameStartX - countW - 4;
        String displayName = this.textRenderer.trimToWidth(fullName, maxAvailWidth);
        if (displayName.length() < fullName.length()) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            String withEllipsis = this.textRenderer.trimToWidth(fullName, maxAvailWidth - this.textRenderer.getWidth("…"));
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            String withEllipsis = this.textRenderer.trimToWidth(fullName, maxAvailWidth - this.textRenderer.getWidth("..."));
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            displayName = withEllipsis + "…";
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            displayName = withEllipsis + "...";
//#endif
        }
        int displayNameW = this.textRenderer.getWidth(displayName);

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 数量紧跟在名称后面
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鏁伴噺绱ц窡鍦ㄥ悕绉板悗闈?
//#endif
        int countX = nameStartX + displayNameW + 4;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        ctx.drawTextWithShadow(this.textRenderer, displayName, nameStartX, screenY + 5, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$        this.textRenderer.drawWithShadow(matrices, displayName, nameStartX, screenY + 5, 0xFFFFFFFF);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        ctx.drawTextWithShadow(this.textRenderer, countStr, countX, screenY + 5, 0xFFAAAAAA);
//#elseif MC == 11701 || MC == 11904
//$$        this.textRenderer.drawWithShadow(matrices, countStr, countX, screenY + 5, 0xFFAAAAAA);
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 通配物品齿轮替换图标
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 閫氶厤鐗╁搧榻胯疆鏇挎崲鍥炬爣
//#endif
        if (isWildcard && gearIconX > 0) {
            int gearY = screenY + (NODE_HEIGHT - GEAR_ICON_SIZE) / 2;
            boolean gearHovered = mouseX >= gearIconX && mouseX < gearIconX + GEAR_ICON_SIZE
                    && mouseY >= gearY && mouseY < gearY + GEAR_ICON_SIZE;
//#if MC == 12108 || MC == 12110 || MC == 12111
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, GEAR_ICON, gearIconX, gearY,
//#elseif MC == 12001
//$$            ctx.drawTexture(GEAR_ICON, gearIconX, gearY,
//#elseif MC == 12101
//$$            ctx.drawTexture( GEAR_ICON, gearIconX, gearY,
//#elseif MC == 12104
//$$            ctx.drawTexture(RenderLayer::getGuiTextured, GEAR_ICON, gearIconX, gearY,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    0.0f, 0.0f, GEAR_ICON_SIZE, GEAR_ICON_SIZE, GEAR_ICON_SIZE, GEAR_ICON_SIZE);
//#endif
//#if MC == 11701 || MC == 11904
//$$            RenderSystem.setShaderTexture(0, GEAR_ICON); DrawableHelper.drawTexture(matrices, gearIconX, gearY, 0.0f, 0.0f, GEAR_ICON_SIZE, GEAR_ICON_SIZE, GEAR_ICON_SIZE, GEAR_ICON_SIZE);
//#endif
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
//#if MC == 12108 || MC == 12110 || MC == 12111
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, icon, expandIconX, iconY, 0.0f, 0.0f,
//#elseif MC == 12001
//$$            ctx.drawTexture(icon, expandIconX, iconY, 0.0f, 0.0f,
//#elseif MC == 12101
//$$            ctx.drawTexture( icon, expandIconX, iconY, 0.0f, 0.0f,
//#elseif MC == 12104
//$$            ctx.drawTexture(RenderLayer::getGuiTextured, icon, expandIconX, iconY, 0.0f, 0.0f,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    EXPAND_ICON_SIZE, EXPAND_ICON_SIZE, EXPAND_ICON_SIZE, EXPAND_ICON_SIZE);
//#endif
//#if MC == 11701 || MC == 11904
//$$            RenderSystem.setShaderTexture(0, icon); DrawableHelper.drawTexture(matrices, expandIconX, iconY, 0.0f, 0.0f, EXPAND_ICON_SIZE, EXPAND_ICON_SIZE, EXPAND_ICON_SIZE, EXPAND_ICON_SIZE);
//#endif
        }

        // 悬停提示：名称上显示完整ID，数量上显示换算量级
        if (item != null) {
            boolean nameHovered = mouseX >= nameStartX && mouseX < nameStartX + displayNameW
                    && mouseY >= screenY && mouseY < screenY + NODE_HEIGHT;
            boolean countHovered = mouseX >= countX && mouseX < countX + countW
                    && mouseY >= screenY && mouseY < screenY + NODE_HEIGHT;
            if (nameHovered) {
                String tip = fullName + " (" + layout.node.itemId + ")";
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawTooltipFixed(ctx, Text.literal(tip));
//#elseif MC == 11701
//$$                drawTooltipFixed(matrices, new LiteralText(tip));
//#elseif MC == 11904
//$$                drawTooltipFixed(matrices, Text.literal(tip));
//#endif
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
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawTooltipFixed(ctx, Text.literal(tip));
//#elseif MC == 11701
//$$                drawTooltipFixed(matrices, new LiteralText(tip));
//#elseif MC == 11904
//$$                drawTooltipFixed(matrices, Text.literal(tip));
//#endif
            } else if (isHovered) {
                // 整个条悬停，显示名称+ID
                String tip = fullName + " (" + layout.node.itemId + ")";
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawTooltipFixed(ctx, Text.literal(tip));
//#elseif MC == 11701
//$$                drawTooltipFixed(matrices, new LiteralText(tip));
//#elseif MC == 11904
//$$                drawTooltipFixed(matrices, Text.literal(tip));
//#endif
            }
        }

        if (!layout.collapsed) {
            for (NodeLayout child : layout.children) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                renderNodeTree(ctx, child, mouseX, mouseY, treeAreaBottom);
//#elseif MC == 11701 || MC == 11904
//$$                renderNodeTree(matrices, child, mouseX, mouseY, treeAreaBottom);
//#endif
            }
        }
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ==================== 连接线渲染（横线+竖线，无斜线） ====================
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ==================== 杩炴帴绾挎覆鏌擄紙妯嚎+绔栫嚎锛屾棤鏂滅嚎锛?====================
//#endif

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void renderConnectors(DrawContext ctx, NodeLayout layout,
//#elseif MC == 11701 || MC == 11904
//$$    private void renderConnectors(MatrixStack matrices, NodeLayout layout,
//#endif
                                   int mouseX, int mouseY) {
        if (layout.collapsed || layout.children.isEmpty()) return;

        int parentRightX = layout.x - (int) scrollX + NODE_WIDTH;
        int parentCenterY = layout.y - (int) scrollY;
        int treeAreaBottom = cullBottomW();

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 父节点超出可视区域则不渲染连线
//#endif
        if (parentRightX + COLUMN_SPACING < 0 || parentRightX > cullRightW()
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
                || parentCenterY < cullTopW() || parentCenterY > treeAreaBottom) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 但仍需递归渲染子节点连线
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 浣嗕粛闇€閫掑綊娓叉煋瀛愯妭鐐硅繛绾?
//#endif
            for (NodeLayout child : layout.children) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                renderConnectors(ctx, child, mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$                renderConnectors(matrices, child, mouseX, mouseY);
//#endif
            }
            return;
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 收集可见子节点
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鏀堕泦鍙瀛愯妭鐐?
//#endif
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
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                renderConnectors(ctx, child, mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$                renderConnectors(matrices, child, mouseX, mouseY);
//#endif
            }
            return;
        }

        int midX = parentRightX + COLUMN_SPACING / 2;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 水平线：从父节点右边缘到中点
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 姘村钩绾匡細浠庣埗鑺傜偣鍙宠竟缂樺埌涓偣
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawHLine(ctx, parentRightX, midX, parentCenterY, 0x60FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$        drawHLine(matrices, parentRightX, midX, parentCenterY, 0x60FFFFFF);
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 配方图标（在水平线中点）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 閰嶆柟鍥炬爣锛堝湪姘村钩绾夸腑鐐癸級
//#endif
        String recipeType = layout.node.recipeType;
        if (recipeType != null && !recipeType.isEmpty() && !"base".equals(recipeType)) {
            int iconX = (parentRightX + midX) / 2 - RECIPE_ICON_SIZE / 2;
            int iconY = parentCenterY - RECIPE_ICON_SIZE / 2;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 裁剪检查（使用已定义的treeAreaBottom）
//#endif
            if (iconX + RECIPE_ICON_SIZE >= 0 && iconX <= cullRightW()
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
                    && iconY + RECIPE_ICON_SIZE >= cullTopW() && iconY <= treeAreaBottom) {
                ItemStack stationItem = getRecipeStationItem(recipeType);
                if (stationItem != null) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    ctx.drawItem(stationItem, iconX, iconY);
//#elseif MC == 11701
//$$                    MinecraftClient.getInstance().getItemRenderer().renderInGui(stationItem, iconX, iconY);
//#elseif MC == 11904
//$$                    MinecraftClient.getInstance().getItemRenderer().renderInGui(matrices, stationItem, iconX, iconY);
//#endif
                }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 检测悬停
//#endif
                if (mouseX >= iconX && mouseX < iconX + RECIPE_ICON_SIZE
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
                        && mouseY >= iconY && mouseY < iconY + RECIPE_ICON_SIZE) {
                    hoveredRecipeNode = layout;
                }
            }
        }

        if (visibleChildren.size() == 1) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 只有一个子节点时，直接画L形线
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 鍙湁涓€涓瓙鑺傜偣鏃讹紝鐩存帴鐢籐褰㈢嚎
//#endif
            NodeLayout child = visibleChildren.get(0);
            int childCenterY = child.y - (int) scrollY;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawVLine(ctx, midX, parentCenterY, childCenterY, 0x60FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            drawVLine(matrices, midX, parentCenterY, childCenterY, 0x60FFFFFF);
//#endif
            int childLeftX = child.x - (int) scrollX;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawHLine(ctx, midX, childLeftX, childCenterY, 0x60FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            drawHLine(matrices, midX, childLeftX, childCenterY, 0x60FFFFFF);
//#endif
        } else {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 多个子节点：从最上到最下画竖线，然后分别画水平线连到每个子节点
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 澶氫釜瀛愯妭鐐癸細浠庢渶涓婂埌鏈€涓嬬敾绔栫嚎锛岀劧鍚庡垎鍒敾姘村钩绾胯繛鍒版瘡涓瓙鑺傜偣
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawVLine(ctx, midX, minChildY, maxChildY, 0x60FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            drawVLine(matrices, midX, minChildY, maxChildY, 0x60FFFFFF);
//#endif

            for (NodeLayout child : visibleChildren) {
                int childLeftX = child.x - (int) scrollX;
                int childCenterY = child.y - (int) scrollY;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 从竖线到子节点左边缘的水平线
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                // 浠庣珫绾垮埌瀛愯妭鐐瑰乏杈圭紭鐨勬按骞崇嚎
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawHLine(ctx, midX, childLeftX, childCenterY, 0x60FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$                drawHLine(matrices, midX, childLeftX, childCenterY, 0x60FFFFFF);
//#endif
            }
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 递归渲染子节点连线
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 閫掑綊娓叉煋瀛愯妭鐐硅繛绾?
//#endif
        for (NodeLayout child : layout.children) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            renderConnectors(ctx, child, mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$            renderConnectors(matrices, child, mouseX, mouseY);
//#endif
        }
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 画水平线，裁剪到可视区域 */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 鐢绘按骞崇嚎锛岃鍓埌鍙鍖哄煙 */
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void drawHLine(DrawContext ctx, int x1, int x2, int y, int color) {
//#elseif MC == 11701 || MC == 11904
//$$    private void drawHLine(MatrixStack matrices, int x1, int x2, int y, int color) {
//#endif
        if (x1 > x2) { int t = x1; x1 = x2; x2 = t; }
        int treeAreaTop = cullTopW();
        int treeAreaBottom = cullBottomW();
        int treeAreaRight = cullRightW();
        if (y < treeAreaTop || y > treeAreaBottom) return;
        if (x2 < 0 || x1 > treeAreaRight) return;
        x1 = Math.max(x1, 0);
        x2 = Math.min(x2, treeAreaRight);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (x1 <= x2) ctx.fill(x1, y, x2 + 1, y + 1, color);
//#elseif MC == 11701 || MC == 11904
//$$        if (x1 <= x2) DrawableHelper.fill(matrices, x1, y, x2 + 1, y + 1, color);
//#endif
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 画竖线，裁剪到可视区域 */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 鐢荤珫绾匡紝瑁佸壀鍒板彲瑙嗗尯鍩?*/
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void drawVLine(DrawContext ctx, int x, int y1, int y2, int color) {
//#elseif MC == 11701 || MC == 11904
//$$    private void drawVLine(MatrixStack matrices, int x, int y1, int y2, int color) {
//#endif
        if (y1 > y2) { int t = y1; y1 = y2; y2 = t; }
        int treeAreaTop = cullTopW();
        int treeAreaBottom = cullBottomW();
        int treeAreaRight = cullRightW();
        if (x < 0 || x > treeAreaRight) return;
        if (y2 < treeAreaTop || y1 > treeAreaBottom) return;
        y1 = Math.max(y1, treeAreaTop);
        y2 = Math.min(y2, treeAreaBottom);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (y1 <= y2) ctx.fill(x, y1, x + 1, y2 + 1, color);
//#elseif MC == 11701 || MC == 11904
//$$        if (y1 <= y2) DrawableHelper.fill(matrices, x, y1, x + 1, y2 + 1, color);
//#endif
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ==================== 鼠标事件 ====================
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ==================== 榧犳爣浜嬩欢 ====================
//#endif

    @Override
//#if MC == 12110 || MC == 12111
    public boolean onMouseClicked(Click click, boolean isDrag) {
//#endif
//#if MC == 12110 || MC == 12111
        if (super.onMouseClicked(click, isDrag)) return true;
//#endif
//#if MC == 12110 || MC == 12111
        int mouseX = (int) click.x();
//#endif
//#if MC == 12110 || MC == 12111
        int mouseY = (int) click.y();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        if (super.mouseClicked(mouseX, mouseY, mouseButton)) return true;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 树整体缩放时，树区域的命中检测使用换算后的世界坐标（扣除缩放矩阵的平移补偿）
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        mouseX = (int) mouseX;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        mouseY = (int) mouseY;
//#endif
        float zoomScaleNow = currentZoomScale();
//#if MC == 12110 || MC == 12111
        int worldMouseX = zoomScaleNow != 1.0F ? (int) (mouseX / zoomScaleNow) : mouseX;
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        int worldMouseX = zoomScaleNow != 1.0F ? (int) (mouseX / zoomScaleNow) : (int) mouseX;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        double worldMouseX = zoomScaleNow != 1.0F ? (mouseX / zoomScaleNow) : mouseX;
//#endif
//#if MC == 12110 || MC == 12111
        int worldMouseY = zoomScaleNow != 1.0F ? (int) ((mouseY - CONTROLS_BOTTOM * (1.0F - zoomScaleNow)) / zoomScaleNow) : mouseY;
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        int worldMouseY = zoomScaleNow != 1.0F ? (int) ((mouseY - CONTROLS_BOTTOM * (1.0F - zoomScaleNow)) / zoomScaleNow) : (int) mouseY;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        double worldMouseY = zoomScaleNow != 1.0F ? ((mouseY - CONTROLS_BOTTOM * (1.0F - zoomScaleNow)) / zoomScaleNow) : mouseY;
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // ---- 滚动条点击处理 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // ---- 婊氬姩鏉＄偣鍑诲鐞?----
//#endif
        int treeAreaTop = CONTROLS_BOTTOM;
        int treeAreaBottom = this.height - TREE_BOTTOM_OFFSET;
        int treeAreaHeight = treeAreaBottom - treeAreaTop;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 垂直滚动条
//#endif
        if (maxScrollY > 0 && mouseX >= this.width - SCROLLBAR_THICKNESS - 2
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
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
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 点击轨道跳跃
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                // 鐐瑰嚮杞ㄩ亾璺宠穬
//#endif
                scrollY = MathHelper.clamp(
                        (double) (mouseY - treeAreaTop - thumbH / 2) / (trackH - thumbH) * maxScrollY,
                        0, maxScrollY);
                return true;
            }
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 水平滚动条
//#endif
        if (maxScrollX > minScrollX && mouseY >= this.height - 4
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
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

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 层数图标点击（在输入框后面）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 灞傛暟鍥炬爣鐐瑰嚮锛堝湪杈撳叆妗嗗悗闈級
//#endif
        int fieldX = 60;
        int depthCtrlY = 34;
        int depthIconX = fieldX + 30;
        if (mouseX >= depthIconX && mouseX < depthIconX + DEPTH_ICON_SIZE
                && mouseY >= depthCtrlY && mouseY < depthCtrlY + DEPTH_ICON_SIZE) {
//#if MC == 12110 || MC == 12111
            if (click.button() == 0) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            if (mouseButton == 0) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 左键：增加层数
//#endif
                if (displayDepth < MAX_DEPTH) {
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
                    displayDepth++;
                    if (depthField != null) depthField.setText(String.valueOf(displayDepth));
                    reanalyze();
                }
//#if MC == 12110 || MC == 12111
            } else if (click.button() == 1) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            } else if (mouseButton == 1) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 右键：减少层数
//#endif
                if (displayDepth > MIN_DEPTH) {
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
                    displayDepth--;
                    if (depthField != null) depthField.setText(String.valueOf(displayDepth));
                    reanalyze();
                }
            }
            return true;
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 层数输入框点击
//#endif
        if (mouseX >= fieldX && mouseX < fieldX + 26
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
                && mouseY >= depthCtrlY && mouseY < depthCtrlY + 18) {
            if (depthField != null) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                depthField.setFocused(true);
//#elseif MC == 11701
//$$                this.setFocused(depthField);
//#endif
            }
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (searchField != null && searchVisible) searchField.setFocused(false);
//#elseif MC == 11701
//$$            if (searchField != null && searchVisible) this.setFocused(null);
//#endif
            return true;
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // ---- 右下角缩放控制按钮（【-】/【+】） ----
//#endif
//#if MC == 12110 || MC == 12111
        if (this.treeZoomEnabled && click.button() == 0
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        if (this.treeZoomEnabled && mouseButton == 0
//#endif
                && mouseY >= zoomBarY - 2 && mouseY < zoomBarY + ZOOM_BTN_SIZE + 2) {
            if (mouseX >= zoomOutBtnX - 2 && mouseX < zoomOutBtnX + ZOOM_BTN_SIZE + 2) {
                if (treeZoomPercent > ZOOM_MIN_PERCENT) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    treeZoomPercent = Math.max(ZOOM_MIN_PERCENT, treeZoomPercent - ZOOM_STEP_PERCENT); // 缩小一档
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    treeZoomPercent = Math.max(ZOOM_MIN_PERCENT, treeZoomPercent - ZOOM_STEP_PERCENT);
//#endif
                    updateMaxScroll();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    MaterialDataStore.savePersistence(); // 缩放档位立即持久化（按世界隔离）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    MaterialDataStore.savePersistence();
//#endif
                }
                return true;
            }
            if (mouseX >= zoomInBtnX - 2 && mouseX < zoomInBtnX + ZOOM_BTN_SIZE + 2) {
                if (treeZoomPercent < ZOOM_MAX_PERCENT) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    treeZoomPercent = Math.min(ZOOM_MAX_PERCENT, treeZoomPercent + ZOOM_STEP_PERCENT); // 放大一档
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    treeZoomPercent = Math.min(ZOOM_MAX_PERCENT, treeZoomPercent + ZOOM_STEP_PERCENT);
//#endif
                    updateMaxScroll();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    MaterialDataStore.savePersistence(); // 缩放档位立即持久化（按世界隔离）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    MaterialDataStore.savePersistence();
//#endif
                }
                return true;
            }
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // ---- 树状图搜索控件点击 ----
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 自愈：点击不在放大镜上时，清除可能残留的按拖状态，
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 防止后续 mouseReleased 误把"点击输入框/导航按钮"当成"点击放大镜"而收起搜索界面
//#endif
//#if MC == 12110 || MC == 12111
        boolean onSearchIcon = click.button() == 0
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        boolean onSearchIcon = mouseButton == 0
//#endif
                && mouseX >= searchIconX && mouseX < searchIconX + SEARCH_ICON_SIZE
                && mouseY >= searchIconY && mouseY < searchIconY + SEARCH_ICON_SIZE;
        if (!onSearchIcon) {
            searchDragPending = false;
            searchDragging = false;
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 放大镜按下：先记录按下状态（区分"点击切换显示"与"长按拖动位置"，判定在 mouseDragged / mouseReleased）
//#endif
//#if MC == 12110 || MC == 12111
        if (click.button() == 0
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        if (mouseButton == 0
//#endif
                && mouseX >= searchIconX && mouseX < searchIconX + SEARCH_ICON_SIZE
                && mouseY >= searchIconY && mouseY < searchIconY + SEARCH_ICON_SIZE) {
            searchDragPending = true;
//#if MC == 12110 || MC == 12111
            searchDragMouseDownX = click.x();
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            searchDragMouseDownX = mouseX;
//#endif
//#if MC == 12110 || MC == 12111
            searchDragMouseDownY = click.y();
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            searchDragMouseDownY = mouseY;
//#endif
            searchDragStartIconX = searchIconX;
            searchDragStartIconY = searchIconY;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (depthField != null) depthField.setFocused(false);
//#elseif MC == 11701
//$$            if (depthField != null) this.setFocused(null);
//#endif
            return true;
        }
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 12110 || MC == 12111

//#endif
        if (searchVisible) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 搜索输入框点击
//#endif
            if (mouseX >= searchFieldX - 2 && mouseX < searchFieldX + SEARCH_FIELD_W + 2
                    && mouseY >= searchFieldY - 1 && mouseY < searchFieldY + SEARCH_FIELD_H + 1) {
                if (searchField != null) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    searchField.setFocused(true);
//#elseif MC == 11701
//$$                    this.setFocused(searchField);
//#endif
                }
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (depthField != null) depthField.setFocused(false);
//#elseif MC == 11701
//$$                if (depthField != null) this.setFocused(null);
//#endif
                return true;
            }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 匹配导航按钮（↑=上一个匹配项，↓=下一个匹配项）
//#endif
//#if MC == 12110 || MC == 12111
            if (click.button() == 0 && !searchMatches.isEmpty()
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            if (mouseButton == 0 && !searchMatches.isEmpty()
//#endif
                    && mouseY >= searchBtnY && mouseY < searchBtnY + SEARCH_NAV_BTN_SIZE) {
                if (mouseX >= searchUpBtnX && mouseX < searchUpBtnX + SEARCH_NAV_BTN_SIZE) {
                    refreshSearchMatches();
                    if (!searchMatches.isEmpty()) {
                        searchIndex = (searchIndex - 1 + searchMatches.size()) % searchMatches.size();
                        currentSearchMatch = searchMatches.get(searchIndex);
                        scrollToMatch(currentSearchMatch);
                    }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    // 导航后保持输入框焦点，用户可继续编辑搜索文字
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    if (searchField != null) searchField.setFocused(true);
//#elseif MC == 11701
//$$                    if (searchField != null) this.setFocused(searchField);
//#endif
                    return true;
                }
                if (mouseX >= searchDownBtnX && mouseX < searchDownBtnX + SEARCH_NAV_BTN_SIZE) {
                    refreshSearchMatches();
                    if (!searchMatches.isEmpty()) {
                        searchIndex = (searchIndex + 1) % searchMatches.size();
                        currentSearchMatch = searchMatches.get(searchIndex);
                        scrollToMatch(currentSearchMatch);
                    }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    // 导航后保持输入框焦点，用户可继续编辑搜索文字
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    if (searchField != null) searchField.setFocused(true);
//#elseif MC == 11701
//$$                    if (searchField != null) this.setFocused(searchField);
//#endif
                    return true;
                }
            }
        }

//#if MC == 12110 || MC == 12111
        if (click.button() == 0) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        if (mouseButton == 0) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 点击配方图标（切换合成方式）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 鐐瑰嚮閰嶆柟鍥炬爣锛堝垏鎹㈠悎鎴愭柟寮忥級
//#endif
            if (hoveredRecipeNode != null && hoveredNode == null) {
                RecipeTreeNode node = hoveredRecipeNode.node;
                if (node.alternativeRecipeTypes != null && !node.alternativeRecipeTypes.isEmpty()) {
                    final double savedSX = this.scrollX;
                    final double savedSY = this.scrollY;
                    String newType = com.litematlist.RawMaterialTreeAnalyzer.toggleRecipeType(
                            node.itemId, node.recipeType, node.alternativeRecipeTypes);
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    LitematListMod.LOGGER.info("[RawMaterialScreen] 切换配方: {} -> {}",
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    LitematListMod.LOGGER.info("[RawMaterialScreen] 鍒囨崲閰嶆柟: {} -> {}",
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            node.itemId, newType != null ? newType : "默认");
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                            node.itemId, newType != null ? newType : "榛樿");
//#endif
                    reanalyze();
                    this.scrollX = MathHelper.clamp(savedSX, this.minScrollX, this.maxScrollX);
                    this.scrollY = Math.min(savedSY, this.maxScrollY);
                    return true;
                }
            }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 点击节点
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 鐐瑰嚮鑺傜偣
//#endif
            if (hoveredNode != null) {
                int nodeScreenY = hoveredNode.y - (int) scrollY - NODE_HEIGHT / 2;
                int nodeScreenX = hoveredNode.x - (int) scrollX;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 齿轮替换图标点击（通配物品的子材料）
//#endif
                if (hoveredNode.node.wildcardSubMaterial) {
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
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

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 拖拽开始
//#endif
            if (mouseY >= CONTROLS_BOTTOM && mouseY <= this.height - TREE_BOTTOM_OFFSET) {
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
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
//#if MC == 12110 || MC == 12111
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
//#endif
        if (draggingVScrollbar) {
            int treeAreaTop = CONTROLS_BOTTOM;
            int treeAreaBottom = this.height - TREE_BOTTOM_OFFSET;
            int treeAreaHeight = treeAreaBottom - treeAreaTop;
            int trackH = treeAreaHeight;
            int thumbH = Math.max(SCROLLBAR_MIN_THUMB,
                    (int) ((double) treeAreaHeight / (totalTreeHeight + treeAreaHeight) * treeAreaHeight));
//#if MC == 12110 || MC == 12111
            double delta = (click.y() - vScrollbarDragStartY) / (trackH - thumbH) * maxScrollY;
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            double delta = (mouseY - vScrollbarDragStartY) / (trackH - thumbH) * maxScrollY;
//#endif
            scrollY = MathHelper.clamp(vScrollStartValue + delta, 0, maxScrollY);
            return true;
        }
        if (draggingHScrollbar) {
            int trackW = this.width;
            int thumbW = Math.max(SCROLLBAR_MIN_THUMB,
                    (int) ((double) this.width / (totalTreeWidth + this.width) * trackW));
//#if MC == 12110 || MC == 12111
            double delta = (click.x() - hScrollbarDragStartX) / (trackW - thumbW) * (maxScrollX - minScrollX);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            double delta = (mouseX - hScrollbarDragStartX) / (trackW - thumbW) * (maxScrollX - minScrollX);
//#endif
            scrollX = MathHelper.clamp(hScrollStartValue + delta, minScrollX, maxScrollX);
            return true;
        }
        if (isDraggingTree) {
            float zs = currentZoomScale();
//#if MC == 12110 || MC == 12111
            double wx = zs != 1.0F ? click.x() / zs : click.x();
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            double wx = zs != 1.0F ? mouseX / zs : mouseX;
//#endif
//#if MC == 12110 || MC == 12111
            double wy = zs != 1.0F ? (click.y() - CONTROLS_BOTTOM * (1.0F - zs)) / zs : click.y();
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            double wy = zs != 1.0F ? (mouseY - CONTROLS_BOTTOM * (1.0F - zs)) / zs : mouseY;
//#endif
            scrollX = MathHelper.clamp(dragScrollStartX - ((int) wx - dragStartX), minScrollX, maxScrollX);
            scrollY = MathHelper.clamp(dragScrollStartY - ((int) wy - dragStartY), 0, maxScrollY);
            return true;
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 长按放大镜拖动：位移超过阈值进入拖动，实时更新控件位置
//#endif
        if (searchDragPending && !searchDragging) {
//#if MC == 12110 || MC == 12111
            double dx = click.x() - searchDragMouseDownX;
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            double dx = mouseX - searchDragMouseDownX;
//#endif
//#if MC == 12110 || MC == 12111
            double dy = click.y() - searchDragMouseDownY;
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            double dy = mouseY - searchDragMouseDownY;
//#endif
            if (dx * dx + dy * dy > DRAG_THRESHOLD) {
                searchDragging = true;
                if (searchField != null) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    searchField.setFocused(false);
//#elseif MC == 11701
//$$                    this.setFocused(null);
//#endif
                }
            }
        }
        if (searchDragging) {
//#if MC == 12110 || MC == 12111
            searchIconX = searchDragStartIconX + (int) (click.x() - searchDragMouseDownX);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            searchIconX = searchDragStartIconX + (int) (mouseX - searchDragMouseDownX);
//#endif
//#if MC == 12110 || MC == 12111
            searchIconY = searchDragStartIconY + (int) (click.y() - searchDragMouseDownY);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            searchIconY = searchDragStartIconY + (int) (mouseY - searchDragMouseDownY);
//#endif
            clampSearchPosition();
            if (searchField != null) {
                searchField.setX(searchFieldX);
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                searchField.setY(searchFieldY);
//#elseif MC == 11701
//$$                searchField.y = searchFieldY;
//#endif
            }
            return true;
        }
//#if MC == 12110 || MC == 12111
        return super.mouseDragged(click, deltaX, deltaY);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        return super.mouseDragged(mouseX, mouseY, mouseButton, deltaX, deltaY);
//#endif
    }

    @Override
//#if MC == 12110 || MC == 12111
    public boolean mouseReleased(Click click) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
//#endif
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
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 未拖动视为点击：切换搜索框显示/隐藏
//#endif
                searchVisible = !searchVisible;
                if (searchField != null) {
                    if (searchVisible) {
                        searchField.setVisible(true);
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        searchField.setFocused(true);
//#elseif MC == 11701
//$$                        this.setFocused(searchField);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        searchField.setCursorToEnd(false);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                        searchField.setCursorToEnd();
//#endif
                    } else {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        searchField.setFocused(false);
//#elseif MC == 11701
//$$                        this.setFocused(null);
//#endif
                        searchField.setVisible(false);
                    }
                }
            }
            searchDragPending = false;
            searchDragging = false;
            return true;
        }
//#if MC == 12110 || MC == 12111
        return super.mouseReleased(click);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        return super.mouseReleased(mouseX, mouseY, mouseButton);
//#endif
    }

    @Override
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
//#endif
        if (mouseY >= CONTROLS_BOTTOM && mouseY <= this.height - TREE_BOTTOM_OFFSET) {
            float zs = currentZoomScale();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            scrollX = MathHelper.clamp(scrollX - horizontalAmount * 20 / zs, minScrollX, maxScrollX);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            scrollY = MathHelper.clamp(scrollY - verticalAmount * 20 / zs, 0, maxScrollY);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            // 1.20.1 的滚轮事件只有纵向分量：按住 Shift 时视为横向滚动
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (net.minecraft.client.gui.screen.Screen.hasShiftDown()) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                scrollX = MathHelper.clamp(scrollX - amount * 20 / zs, minScrollX, maxScrollX);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            } else {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                scrollY = MathHelper.clamp(scrollY - amount * 20 / zs, 0, maxScrollY);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            }
//#endif
            return true;
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        return super.mouseScrolled(mouseX, mouseY, amount);
//#endif
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ==================== 键盘事件 ====================
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ==================== 閿洏浜嬩欢 ====================
//#endif

    @Override
//#if MC == 12110 || MC == 12111
    public boolean keyPressed(net.minecraft.client.input.KeyInput keyInput) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
//#endif
//#if MC == 12110 || MC == 12111
        if (keyInput.key() == 256) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        if (keyCode == 256) {
//#endif
            if (searchVisible && searchField != null && searchField.isFocused()) {
                searchVisible = false;
                searchField.setVisible(false);
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                searchField.setFocused(false);
//#elseif MC == 11701
//$$                this.setFocused(null);
//#endif
                return true;
            }
            if (depthField != null && depthField.isFocused()) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                depthField.setFocused(false);
//#elseif MC == 11701
//$$                this.setFocused(null);
//#endif
                applyDepthFromField();
                return true;
            }
            saveCollapseState();
            MinecraftClient.getInstance().setScreen(parent);
            return true;
        }
//#if MC == 12110 || MC == 12111
        if (keyInput.key() == 257 || keyInput.key() == 335) { // Enter or NumPad Enter
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        if (keyCode == 257 || keyCode == 335) { // Enter or NumPad Enter
//#endif
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
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                depthField.setFocused(false);
//#elseif MC == 11701
//$$                this.setFocused(null);
//#endif
                applyDepthFromField();
                return true;
            }
        }
        if (depthField != null && depthField.isFocused()) {
//#if MC == 12110 || MC == 12111
            if (depthField.keyPressed(keyInput)) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            if (depthField.keyPressed(keyCode, scanCode, modifiers)) {
//#endif
                return true;
            }
        }
        if (searchVisible && searchField != null && searchField.isFocused()) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // Ctrl+V 粘贴：支持把外部（聊天框、文本文件等）的中文/英文直接粘贴进搜索框
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            // Ctrl+V paste: support pasting Chinese/English from external sources into the search box
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            long window = MinecraftClient.getInstance().getWindow().getHandle();
//#endif
//#if MC == 12110 || MC == 12111
            if (keyInput.key() == net.minecraft.client.util.InputUtil.GLFW_KEY_V
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_V
//#endif
//#if MC == 12110 || MC == 12111
                    && net.minecraft.client.util.InputUtil.isKeyPressed(
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$                    && org.lwjgl.glfw.GLFW.glfwGetKey(
//#endif
//#if MC == 12110 || MC == 12111
                    MinecraftClient.getInstance().getWindow(),
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$                    MinecraftClient.getInstance().getWindow().getHandle(),
//#endif
//#if MC == 12110 || MC == 12111
                    net.minecraft.client.util.InputUtil.GLFW_KEY_LEFT_CONTROL)) {
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$                    org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL) == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    && org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL)
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
//#endif
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
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        searchField.setCursor(pos + insert.length(), false);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                        searchField.setCursor(pos + insert.length());
//#endif
                    }
                }
                return true;
            }
//#if MC == 12110 || MC == 12111
            if (searchField.keyPressed(keyInput)) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            if (searchField.keyPressed(keyCode, scanCode, modifiers)) {
//#endif
                return true;
            }
            return true;
        }
//#if MC == 12110 || MC == 12111
        return super.keyPressed(keyInput);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        return super.keyPressed(keyCode, scanCode, modifiers);
//#endif
    }

    @Override
//#if MC == 12110 || MC == 12111
    public boolean charTyped(net.minecraft.client.input.CharInput charInput) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$    public boolean charTyped(char chr, int modifiers) {
//#endif
        if (searchVisible && searchField != null && searchField.isFocused()) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // Ctrl 组合字符（如 Ctrl+V 产生的'v'）不写入搜索框，避免与快捷键处理重复
//#endif
//#if MC == 12110 || MC == 12111
            if ((charInput.modifiers() & 0x2) != 0) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            if ((modifiers & 0x2) != 0) {
//#endif
                return true;
            }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 与【查看材料列表】【原材料配置】界面一致：优先由组件自身处理字符
//#endif
//#if MC == 12110 || MC == 12111
            if (searchField.charTyped(charInput)) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            if (searchField.charTyped(chr, modifiers)) {
//#endif
                return true;
            }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 兜底：组件未处理时手动插入可见字符（覆盖中文等输入法提交码点）
//#endif
//#if MC == 12110 || MC == 12111
            int cp = charInput.codepoint();
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$            int cp = chr;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            int cp = (int) chr;
//#endif
            if (cp >= 32 && cp != 127 && searchField.getText().length() < 32) {
                String before = searchField.getText();
                int pos = Math.min(searchField.getCursor(), before.length());
                String after = before.substring(0, pos) + Character.toString(cp) + before.substring(pos);
                searchField.setText(after);
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                searchField.setCursor(pos + 1, false);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                searchField.setCursor(pos + 1);
//#endif
            }
            return true;
        }
        if (depthField != null && depthField.isFocused()) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 只允许数字
//#endif
//#if MC == 12110 || MC == 12111
            if (Character.isDigit(charInput.codepoint())) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            if (Character.isDigit(chr)) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 12110 || MC == 12111
                if (depthField.charTyped(charInput)) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                if (depthField.charTyped(chr, modifiers)) {
//#endif
                    return true;
                }
            }
            return true;
        }
//#if MC == 12110 || MC == 12111
        return super.charTyped(charInput);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        return super.charTyped(chr, modifiers);
//#endif
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

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ==================== JSON导出（跳过折叠节点） ====================
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ==================== JSON瀵煎嚭锛堣烦杩囨姌鍙犺妭鐐癸級 ====================
//#endif

    private void exportTreeAsJson() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date());
        String baseName = schematicName.replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "_");
        String filename = baseName + "-treetable-" + timestamp + ".json";
        Path exportPath = getTreeTableExportPath(filename);

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 收集折叠节点路径
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鏀堕泦鎶樺彔鑺傜偣璺緞
//#endif
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
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            LitematListMod.LOGGER.info("配方树已导出: {}", exportPath);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            LitematListMod.LOGGER.info("閰嶆柟鏍戝凡瀵煎嚭: {}", exportPath);
//#endif

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                Text link = Text.literal(filename)
//#elseif MC == 11701
//$$                Text link = new LiteralText(filename)
//#endif
                        .styled(style -> style
//#if MC == 12108 || MC == 12110 || MC == 12111
                                .withClickEvent(new net.minecraft.text.ClickEvent.OpenFile(exportPath.toString()))
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                                .withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.OPEN_FILE, exportPath.toString()))
//#endif
                                .withUnderline(true)
                                .withColor(0x55FFFF));
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                Text msg = Text.literal("§a配方树已导出为JSON: ").append(link);
//#elseif MC == 11701
//$$                Text msg = new TranslatableText("litematlist.message.exported_tree").append(link);
//#elseif MC == 11904 || MC == 12001
//$$                Text msg = Text.translatable("litematlist.message.exported_tree").append(link);
//#endif
                client.player.sendMessage(msg, false);
            }
        } catch (Exception e) {
            LitematListMod.LOGGER.error("导出配方树失败", e);
        }
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ==================== 通用导出（txt/csv） ====================
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ==================== 閫氱敤瀵煎嚭锛坱xt/csv锛?====================
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 收集树状图的叶子节点（完全展开，折叠节点作为叶子）。
     * 每个叶子节点包含：物品ID、数量、配方说明。
     */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /**
//$$     * 鏀堕泦鏍戠姸鍥剧殑鍙跺瓙鑺傜偣锛堝畬鍏ㄥ睍寮€锛屾姌鍙犺妭鐐逛綔涓哄彾瀛愶級銆?     * 姣忎釜鍙跺瓙鑺傜偣鍖呭惈锛氱墿鍝両D銆佹暟閲忋€侀厤鏂硅鏄庛€?     */
//#endif
    private List<String[]> collectLeafNodes() {
        List<String[]> leaves = new ArrayList<>();
        for (NodeLayout root : rootLayouts) {
            collectLeafNodesRecursive(root, leaves);
        }
        return leaves;
    }

    private void collectLeafNodesRecursive(NodeLayout layout, List<String[]> out) {
        if (layout.collapsed || layout.children.isEmpty()) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 折叠节点或无子节点 = 叶子节点
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 鎶樺彔鑺傜偣鎴栨棤瀛愯妭鐐?= 鍙跺瓙鑺傜偣
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            Item item = Registries.ITEM.get(Identifier.of(layout.node.itemId));
//#elseif MC == 11701
//$$            Item item = Registry.ITEM.get(Identifier.tryParse(layout.node.itemId));
//#elseif MC == 11904 || MC == 12001
//$$            Item item = Registries.ITEM.get(Identifier.tryParse(layout.node.itemId));
//#endif
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

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 导出为 TXT 格式（与 MaterialDetailScreen 格式一致） */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 瀵煎嚭涓?TXT 鏍煎紡锛堜笌 MaterialDetailScreen 鏍煎紡涓€鑷达級 */
//#endif
    private void exportAsTxt() {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date());
            String filename = "raw_materials_" + schematicName.replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "_") + "_" + timestamp + ".txt";
            Path exportPath = getRawMaterialExportPath(filename);
            java.nio.file.Files.createDirectories(exportPath.getParent());

            List<String[]> leaves = collectLeafNodes();
            try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(exportPath.toFile()), StandardCharsets.UTF_8))) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                String title = "原理图'" + schematicName + "'原材料";
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                String title = "原理图\'" + schematicName + "\'原材料";
//#endif
                w.write("+---------+-------+---------+");
                w.newLine();
                w.write("| " + TextWidthUtils.padToWidth(title, 25) + " |");
                w.newLine();
                w.write("+---------+-------+---------+");
                w.newLine();
                w.write("| Item    | Count | Recipe  |");
                w.newLine();
                w.write("+---------+-------+---------+");
                w.newLine();
                for (String[] leaf : leaves) {
                    String name = leaf[1];
                    if (TextWidthUtils.displayWidth(name) > 9) name = TextWidthUtils.truncateByWidth(name, 9);
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
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            LitematListMod.LOGGER.error("导出TXT失败", e);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            LitematListMod.LOGGER.error("瀵煎嚭TXT澶辫触", e);
//#endif
        }
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 导出为 JSON 格式（与 MaterialDetailScreen 格式一致，仅叶子节点） */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 瀵煎嚭涓?JSON 鏍煎紡锛堜笌 MaterialDetailScreen 鏍煎紡涓€鑷达紝浠呭彾瀛愯妭鐐癸級 */
//#endif
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
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                String title = "原理图'" + schematicName + "'原材料";
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                String title = "原理图\'" + schematicName + "\'原材料";
//#endif
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
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            LitematListMod.LOGGER.error("导出JSON失败", e);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            LitematListMod.LOGGER.error("瀵煎嚭JSON澶辫触", e);
//#endif
        }
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 导出为 CSV 格式（与 MaterialDetailScreen 格式一致） */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 瀵煎嚭涓?CSV 鏍煎紡锛堜笌 MaterialDetailScreen 鏍煎紡涓€鑷达級 */
//#endif
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
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            LitematListMod.LOGGER.error("导出CSV失败", e);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            LitematListMod.LOGGER.error("瀵煎嚭CSV澶辫触", e);
//#endif
        }
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 发送导出成功消息 */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 鍙戦€佸鍑烘垚鍔熸秷鎭?*/
//#endif
    private void sendExportMessage(String fmt, String filename, Path exportPath) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            Text link = Text.literal(filename)
//#elseif MC == 11701
//$$            Text link = new LiteralText(filename)
//#endif
                    .styled(style -> style
//#if MC == 12108 || MC == 12110 || MC == 12111
                            .withClickEvent(new net.minecraft.text.ClickEvent.OpenFile(exportPath.toString()))
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                            .withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.OPEN_FILE, exportPath.toString()))
//#endif
                            .withUnderline(true).withColor(0x55FFFF));
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            client.player.sendMessage(Text.literal("§a原材料已导出为" + fmt + ": ").append(link), false);
//#elseif MC == 11701
//$$            client.player.sendMessage(new TranslatableText("litematlist.message.exported_raw", fmt).append(link), false);
//#elseif MC == 11904 || MC == 12001
//$$            client.player.sendMessage(Text.translatable("litematlist.message.exported_raw", fmt).append(link), false);
//#endif
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        LitematListMod.LOGGER.info("原材料已导出为{}: {}", fmt, exportPath);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        LitematListMod.LOGGER.info("鍘熸潗鏂欏凡瀵煎嚭涓簕}: {}", fmt, exportPath);
//#endif
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ==================== 通配物品替换 ====================
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ==================== 閫氶厤鐗╁搧鏇挎崲 ====================
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 根据配方类型返回对应的工作站物品图标。
     */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /**
//$$     * 鏍规嵁閰嶆柟绫诲瀷杩斿洖瀵瑰簲鐨勫伐浣滅珯鐗╁搧鍥炬爣銆?     */
//#endif
    private static ItemStack getRecipeStationItem(String recipeType) {
        return switch (recipeType) {
            case "crafting_shaped", "crafting_shapeless" -> new ItemStack(Items.CRAFTING_TABLE);
            case "smelting" -> new ItemStack(Items.FURNACE);
            case "stonecutting" -> new ItemStack(Items.STONECUTTER);
            case "smithing" -> new ItemStack(Items.SMITHING_TABLE);
            default -> null;
        };
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 打开通配物品替换选择器，允许玩家将通配物品替换为同类物品。
     */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /**
//$$     * 鎵撳紑閫氶厤鐗╁搧鏇挎崲閫夋嫨鍣紝鍏佽鐜╁灏嗛€氶厤鐗╁搧鏇挎崲涓哄悓绫荤墿鍝併€?     */
//#endif
    private void openWildcardReplacement(NodeLayout layout) {
        String itemId = layout.node.itemId;
        String parentId = layout.parentItemId;
        final double savedScrollX = this.scrollX;
        final double savedScrollY = this.scrollY;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 收集同类物品（相同物品标签组）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鏀堕泦鍚岀被鐗╁搧锛堢浉鍚岀墿鍝佹爣绛剧粍锛?
//#endif
        Set<Item> compatibleItems = getCompatibleItems(itemId);
        goingToSubScreen = true;
        MinecraftClient.getInstance().setScreen(
                new BlockPickerScreen(this, selectedItem -> {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    // 替换节点中的物品ID
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    // 鏇挎崲鑺傜偣涓殑鐗╁搧ID
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    String newId = Registries.ITEM.getId(selectedItem).toString();
//#elseif MC == 11701
//$$                    String newId = Registry.ITEM.getId(selectedItem).toString();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    LitematListMod.LOGGER.info("[RawMaterialScreen] 通配替换: {} -> {} (同步={})",
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    LitematListMod.LOGGER.info("[RawMaterialScreen] 閫氶厤鏇挎崲: {} -> {} (鍚屾={})",
//#endif
                            itemId, newId, syncReplacementEnabled);
                    if (syncReplacementEnabled) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        // 全局同步替换：所有相同子材料都被替换
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                        // 鍏ㄥ眬鍚屾鏇挎崲锛氭墍鏈夌浉鍚屽瓙鏉愭枡閮借鏇挎崲
//#endif
                        com.litematlist.RawMaterialTreeAnalyzer.setWildcardReplacement(itemId, newId);
                    } else {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        // 仅当前上下文替换：只替换当前父物品下的子材料
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                        // 浠呭綋鍓嶄笂涓嬫枃鏇挎崲锛氬彧鏇挎崲褰撳墠鐖剁墿鍝佷笅鐨勫瓙鏉愭枡
//#endif
                        String effectiveParentId = parentId != null ? parentId : itemId;
                        com.litematlist.RawMaterialTreeAnalyzer.setContextWildcardReplacement(
                                effectiveParentId, itemId, newId);
                    }
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    // // 妫€鏌ョ洿鎺ョ埗鐗╁搧鎴栫鐖剁墿鍝佹槸鍚︿负闆曠汗涔︽灦
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    boolean isBookshelfChain = "minecraft:chiseled_bookshelf".equals(parentId);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    if (!isBookshelfChain && parentId != null) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        // 涔熸鏌ユ槸鍚︿负鍗婄爾瀛愯妭鐐圭殑鏈ㄦ澘锛堢埗鐗╁搧鏄崐鐮栵紝绁栫埗鏄洉绾逛功鏋讹級
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        String grandparentId = layout.getGrandparentItemId();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        isBookshelfChain = "minecraft:chiseled_bookshelf".equals(grandparentId);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    if (isBookshelfChain) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        if (itemId.endsWith("_planks")) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                            // 鏇挎崲鏈ㄦ澘鏃跺悓姝ユ浛鎹㈠搴斿崐鐮?
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                            String linkedSlab = itemId.replace("_planks", "_slab");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                            String newSlab = newId.replace("_planks", "_slab");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                            if (syncReplacementEnabled) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                                com.litematlist.RawMaterialTreeAnalyzer.setWildcardReplacement(
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                                        linkedSlab, newSlab);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                            } else {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                                com.litematlist.RawMaterialTreeAnalyzer.setContextWildcardReplacement(
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                                        parentId, linkedSlab, newSlab);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                            }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        } else if (itemId.endsWith("_slab")) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                            // 鏇挎崲鍗婄爾鏃跺悓姝ユ浛鎹㈠搴旀湪鏉?
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                            String linkedPlank = itemId.replace("_slab", "_planks");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                            String newPlank = newId.replace("_slab", "_planks");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                            if (syncReplacementEnabled) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                                com.litematlist.RawMaterialTreeAnalyzer.setWildcardReplacement(
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                                        linkedPlank, newPlank);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                            } else {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                                com.litematlist.RawMaterialTreeAnalyzer.setContextWildcardReplacement(
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                                        parentId, linkedPlank, newPlank);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                            }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    }
//#endif
                    hasPendingChanges = true;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    // 强制重分析配方树，确保替换立即生效
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    // 寮哄埗閲嶅垎鏋愰厤鏂规爲锛岀‘淇濇浛鎹㈢珛鍗崇敓鏁?
//#endif
                    loadOrAnalyze();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    // 保存滚动位置
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    // 淇濆瓨婊氬姩浣嶇疆
//#endif
                    this.scrollX = MathHelper.clamp(savedScrollX, this.minScrollX, this.maxScrollX);
                    this.scrollY = Math.min(savedScrollY, this.maxScrollY);
                }, compatibleItems));
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 获取与指定物品兼容的同类物品集合（基于物品标签组）。
     * 例如：橡木木板 → 所有木板变种，石剑 → 所有石质工具等。
     */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /**
//$$     * 鑾峰彇涓庢寚瀹氱墿鍝佸吋瀹圭殑鍚岀被鐗╁搧闆嗗悎锛堝熀浜庣墿鍝佹爣绛剧粍锛夈€?     * 渚嬪锛氭鏈ㄦ湪鏉?鈫?鎵€鏈夋湪鏉垮彉绉嶏紝鐭冲墤 鈫?鎵€鏈夌煶璐ㄥ伐鍏风瓑銆?     */
//#endif
    private Set<Item> getCompatibleItems(String itemId) {
        Set<Item> collected = new LinkedHashSet<>();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Item targetItem = Registries.ITEM.get(Identifier.of(itemId));
//#elseif MC == 11701
//$$        Item targetItem = Registry.ITEM.get(Identifier.tryParse(itemId));
//#elseif MC == 11904 || MC == 12001
//$$        Item targetItem = Registries.ITEM.get(Identifier.tryParse(itemId));
//#endif
        if (targetItem == null) return collected;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 策略1：查找物品所属的标签，收集同标签物品
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 绛栫暐1锛氭煡鎵剧墿鍝佹墍灞炵殑鏍囩锛屾敹闆嗗悓鏍囩鐗╁搧
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        var registry = MinecraftClient.getInstance().world != null
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                ? MinecraftClient.getInstance().world.getRegistryManager()
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                : null;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (registry != null) {
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            var itemTags = registry.getOptional(net.minecraft.registry.RegistryKeys.ITEM);
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (itemTags.isPresent()) {
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                var tagRegistry = itemTags.get();
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                tagRegistry.streamTags().forEach(tag -> {
//#endif
//#if MC == 12101
//$$                tagRegistry.streamTagsAndEntries().forEach(pair -> {
//#endif
//#if MC == 12101
//$$                    var tag = pair.getSecond();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    boolean contains = false;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    for (var entry : tag) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        if (entry.value() == targetItem) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            contains = true;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            break;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    if (contains) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        for (var entry : tag) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            collected.add(entry.value());
//#endif
//#if MC == 11904 || MC == 12001
//$$                tagRegistry.streamTags().forEach(tag -> {
//#endif
//#if MC == 11904 || MC == 12001
//$$                    var tagEntries = tagRegistry.getEntryList(tag);
//#endif
//#if MC == 11904 || MC == 12001
//$$                    if (tagEntries.isPresent()) {
//#endif
//#if MC == 11904 || MC == 12001
//$$                        boolean contains = tagEntries.get().stream().anyMatch(e -> e.value() == targetItem);
//#endif
//#if MC == 11904 || MC == 12001
//$$                        if (contains) {
//#endif
//#if MC == 11904 || MC == 12001
//$$                            tagEntries.get().forEach(e -> collected.add(e.value()));
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        }
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    }
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                });
//#endif
//#if MC == 11701
//$$        // 1.17.1：标签体系为 TagGroup（1.18.2 起才有 Registry.getEntryList/streamTags）
//#endif
//#if MC == 11701
//$$        var tagWorld = MinecraftClient.getInstance().world;
//#endif
//#if MC == 11701
//$$        if (tagWorld != null) {
//#endif
//#if MC == 11701
//$$            net.minecraft.tag.TagGroup<Item> itemTags = tagWorld.getTagManager().getOrCreateTagGroup(Registry.ITEM_KEY);
//#endif
//#if MC == 11701
//$$            for (Identifier tagId : itemTags.getTagsFor(targetItem)) {
//#endif
//#if MC == 11701
//$$                net.minecraft.tag.Tag<Item> tag = itemTags.getTag(tagId);
//#endif
//#if MC == 11701
//$$                if (tag != null) {
//#endif
//#if MC == 11701
//$$                    collected.addAll(tag.values());
//#endif
//#if MC == 11701
//$$                }
//#endif
            }
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 策略2：如果没有找到同类物品，至少包含目标物品自身
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 绛栫暐2锛氬鏋滄病鏈夋壘鍒板悓绫荤墿鍝侊紝鑷冲皯鍖呭惈鐩爣鐗╁搧鑷韩
//#endif
        if (collected.isEmpty()) {
            collected.add(targetItem);
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 策略2.5：显式添加同类物品（标签查找可能遗漏的变种）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 绛栫暐2.5锛氭樉寮忔坊鍔犲悓绫荤墿鍝侊紙鏍囩鏌ユ壘鍙兘閬楁紡鐨勫彉绉嶏級
//#endif
        addExplicitCompatibleItems(collected, itemId);

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 策略3：按物品类型过滤，避免跨类型混入（如羊毛替换页面显示所有染色物品）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 绛栫暐3锛氭寜鐗╁搧绫诲瀷杩囨护锛岄伩鍏嶈法绫诲瀷娣峰叆锛堝缇婃瘺鏇挎崲椤甸潰鏄剧ず鎵€鏈夋煋鑹茬墿鍝侊級
//#endif
        Set<Item> filtered = filterByItemType(collected, itemId);
        return filtered;
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 显式添加同类物品（标签查找可能遗漏的变种）。
     * 例如：圆石和黑石在同一标签，但深板岩圆石可能不在，需要显式添加。
     */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /**
//$$     * 鏄惧紡娣诲姞鍚岀被鐗╁搧锛堟爣绛炬煡鎵惧彲鑳介仐婕忕殑鍙樼锛夈€?     * 渚嬪锛氬渾鐭冲拰榛戠煶鍦ㄥ悓涓€鏍囩锛屼絾娣辨澘宀╁渾鐭冲彲鑳戒笉鍦紝闇€瑕佹樉寮忔坊鍔犮€?     */
//#endif
    private void addExplicitCompatibleItems(Set<Item> collected, String itemId) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 圆石类：确保深板岩圆石和黑石都在列表中
//#endif
        if (itemId.equals("minecraft:cobblestone") || itemId.equals("minecraft:blackstone")
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
                || itemId.equals("minecraft:cobbled_deepslate")) {
            addItemById(collected, "minecraft:cobblestone");
            addItemById(collected, "minecraft:blackstone");
            addItemById(collected, "minecraft:cobbled_deepslate");
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 沙子/砂砾
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 娌欏瓙/鐮傜牼
//#endif
        if (itemId.equals("minecraft:sand") || itemId.equals("minecraft:gravel")) {
            addItemById(collected, "minecraft:sand");
            addItemById(collected, "minecraft:gravel");
        }
        // 煤炭/木炭
        if (itemId.equals("minecraft:coal") || itemId.equals("minecraft:charcoal")) {
            addItemById(collected, "minecraft:coal");
            addItemById(collected, "minecraft:charcoal");
        }
//#if MC == 12108
//$$        // 鸡蛋（1.21.5+ 共 3 种，用于蛋糕配方，互相通配）
//#endif
//#if MC == 12108
//$$        if (itemId.equals("minecraft:egg") || itemId.equals("minecraft:brown_egg")
//#endif
//#if MC == 12108
//$$                || itemId.equals("minecraft:blue_egg")) {
//#endif
//#if MC == 12108
//$$            addItemById(collected, "minecraft:egg");
//#endif
//#if MC == 12108
//$$            addItemById(collected, "minecraft:brown_egg");
//#endif
//#if MC == 12108
//$$            addItemById(collected, "minecraft:blue_egg");
//#endif
//#if MC == 12108
//$$        }
//#endif
//#if MC == 12111
//$$        // 鸡蛋（1.21.5+ 共 3 种，用于蛋糕配方，互相通配）
//#endif
//#if MC == 12111
//$$        if (itemId.equals("minecraft:egg") || itemId.equals("minecraft:brown_egg")
//#endif
//#if MC == 12111
//$$                || itemId.equals("minecraft:blue_egg")) {
//#endif
//#if MC == 12111
//$$            addItemById(collected, "minecraft:egg");
//#endif
//#if MC == 12111
//$$            addItemById(collected, "minecraft:brown_egg");
//#endif
//#if MC == 12111
//$$            addItemById(collected, "minecraft:blue_egg");
//#endif
//#if MC == 12111
//$$        }
//#endif
        // 收纳袋（无色 + 16色），确保17色家族完整（标签查找可能遗漏部分变种）
        if (itemId.equals("minecraft:bundle") || itemId.endsWith("_bundle")) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            for (Item item : Registries.ITEM) {
//#elseif MC == 11701
//$$            for (Item item : Registry.ITEM) {
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                String id = Registries.ITEM.getId(item).toString();
//#elseif MC == 11701
//$$                String id = Registry.ITEM.getId(item).toString();
//#endif
                if (id.equals("minecraft:bundle") || id.endsWith("_bundle")) {
                    collected.add(item);
                }
            }
        }
        // 苔藓/藤蔓互替（苔石、苔石砖的溯源配方）
        if (itemId.equals("minecraft:moss_block") || itemId.equals("minecraft:vine")) {
            addItemById(collected, "minecraft:moss_block");
            addItemById(collected, "minecraft:vine");
//#if MC == 12110
        }
//#endif
//#if MC == 12110
        // 鸡蛋（1.21.5+ 共 3 种，用于蛋糕配方，互相通配）
//#endif
//#if MC == 12110
        if (itemId.equals("minecraft:egg") || itemId.equals("minecraft:brown_egg")
//#endif
//#if MC == 12110
                || itemId.equals("minecraft:blue_egg")) {
//#endif
//#if MC == 12110
            addItemById(collected, "minecraft:egg");
//#endif
//#if MC == 12110
            addItemById(collected, "minecraft:brown_egg");
//#endif
//#if MC == 12110
            addItemById(collected, "minecraft:blue_egg");
//#endif
        }
    }

    private void addItemById(Set<Item> collected, String itemId) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Item item = Registries.ITEM.get(Identifier.of(itemId));
//#elseif MC == 11701
//$$        Item item = Registry.ITEM.get(Identifier.tryParse(itemId));
//#elseif MC == 11904 || MC == 12001
//$$        Item item = Registries.ITEM.get(Identifier.tryParse(itemId));
//#endif
        if (item != null) {
            collected.add(item);
        }
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 按物品类型过滤，只保留与目标物品同类型的变种。
     * 例如：羊毛只保留 _wool 结尾的，木板只保留 _planks 结尾的。
     */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /**
//$$     * 鎸夌墿鍝佺被鍨嬭繃婊わ紝鍙繚鐣欎笌鐩爣鐗╁搧鍚岀被鍨嬬殑鍙樼銆?     * 渚嬪锛氱緤姣涘彧淇濈暀 _wool 缁撳熬鐨勶紝鏈ㄦ澘鍙繚鐣?_planks 缁撳熬鐨勩€?     */
//#endif
    private Set<Item> filterByItemType(Set<Item> items, String itemId) {
        String typeSuffix = null;
        if (itemId.endsWith("_wool")) typeSuffix = "_wool";
        else if (itemId.endsWith("_planks")) typeSuffix = "_planks";
        else if (itemId.endsWith("_slab")) typeSuffix = "_slab";
        else if (itemId.endsWith("_bed")) typeSuffix = "_bed";
        else if (itemId.equals("minecraft:bundle") || itemId.endsWith("_bundle")) typeSuffix = "_bundle";
        else if (itemId.endsWith("_leaves") || itemId.equals("minecraft:azalea_leaves")
                || itemId.equals("minecraft:flowering_azalea_leaves")) typeSuffix = "_leaves";
        else if (itemId.startsWith("minecraft:wooden_")) typeSuffix = "wooden_";
        else if (itemId.startsWith("minecraft:stone_")) typeSuffix = "stone_";
        else if (itemId.equals("minecraft:cobblestone") || itemId.equals("minecraft:blackstone")
                || itemId.equals("minecraft:cobbled_deepslate")) typeSuffix = "cobblestone_like";
        else if (itemId.equals("minecraft:sand") || itemId.equals("minecraft:gravel")) typeSuffix = "sand_gravel";
        else if (itemId.equals("minecraft:moss_block") || itemId.equals("minecraft:vine")) typeSuffix = "moss_vine";
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        else if (com.litematlist.RawMaterialTreeAnalyzer.isLogFamily(itemId)) typeSuffix = "log_family";
//#endif
//#if MC == 12111
//$$        else if (com.litematlist.RawMaterialTreeAnalyzer.isLogFamily(itemId)) typeSuffix = "log_family";
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        else if (itemId.equals("minecraft:egg") || itemId.equals("minecraft:brown_egg")
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                || itemId.equals("minecraft:blue_egg")) typeSuffix = "egg_family";
//#endif
//#if MC == 12110
        else if (com.litematlist.RawMaterialTreeAnalyzer.isLogFamily(itemId)) typeSuffix = "log_family";
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (typeSuffix == null) return items; // 不需要过滤
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (typeSuffix == null) return items; // 涓嶉渶瑕佽繃婊?
//#endif
        Set<Item> filtered = new LinkedHashSet<>();
        for (Item item : items) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            String id = Registries.ITEM.getId(item).toString();
//#elseif MC == 11701
//$$            String id = Registry.ITEM.getId(item).toString();
//#endif
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
            } else if (typeSuffix.equals("moss_vine")
                    && (id.equals("minecraft:moss_block") || id.equals("minecraft:vine"))) {
                filtered.add(item);
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            } else if (typeSuffix.equals("log_family")
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                    && com.litematlist.RawMaterialTreeAnalyzer.isLogFamily(id)) {
//#endif
//#if MC == 11904
//$$                // 原木家族：只有原木/木头/菌柄/菌核互相替换（标签查找会带上树叶、疣块等，需过滤）
//#endif
//#if MC == 12108
//$$                filtered.add(item);
//#endif
//#if MC == 12111
//$$            } else if (typeSuffix.equals("log_family")
//#endif
//#if MC == 12111
//$$                    && com.litematlist.RawMaterialTreeAnalyzer.isLogFamily(id)) {
//#endif
//#if MC == 12111
//$$                filtered.add(item);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            } else if (typeSuffix.equals("egg_family")
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    && (id.equals("minecraft:egg") || id.equals("minecraft:brown_egg")
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                        || id.equals("minecraft:blue_egg"))) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                // 鸡蛋家族：仅 1.21.5+ 的 3 种鸡蛋互相替换（避免混入海龟蛋、龙蛋等）
//#endif
//#if MC == 12110
                filtered.add(item);
//#endif
//#if MC == 12110
            } else if (typeSuffix.equals("log_family")
//#endif
//#if MC == 12110
                    && com.litematlist.RawMaterialTreeAnalyzer.isLogFamily(id)) {
//#endif
//#if MC == 12110
                // 原木家族：只有原木/木头/菌柄/菌核互相替换（标签查找会带上树叶、疣块等，需过滤）
//#endif
                filtered.add(item);
            } else if (typeSuffix.equals("_leaves") && id.endsWith("_leaves")) {
                filtered.add(item);
            } else if (typeSuffix.equals("_bundle")
                    && (id.equals("minecraft:bundle") || id.endsWith("_bundle"))) {
                // 收纳袋家族：无色 bundle + 16色 *_bundle（与羊毛同为染色通配，避免混入其他可染色物品）
                filtered.add(item);
            } else if (typeSuffix.startsWith("_") && id.endsWith(typeSuffix)) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 半砖特殊处理：只保留木板半砖（对应木板存在的才是木板半砖）
//#endif
                if (typeSuffix.equals("_slab")) {
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
                    String plankId = id.replace("_slab", "_planks");
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    if (Registries.ITEM.containsId(Identifier.of(plankId))) {
//#elseif MC == 11701
//$$                    if (Registry.ITEM.containsId(Identifier.tryParse(plankId))) {
//#elseif MC == 11904 || MC == 12001
//$$                    if (Registries.ITEM.containsId(Identifier.tryParse(plankId))) {
//#endif
                        filtered.add(item);
                    }
                } else {
                    filtered.add(item);
                }
            }
        }
        return filtered.isEmpty() ? items : filtered;
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ==================== 创造模式排序 ====================
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ==================== 鍒涢€犳ā寮忔帓搴?====================
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 按创造模式物品栏顺序对 treeRoots 排序。
     */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /**
//$$     * 鎸夊垱閫犳ā寮忕墿鍝佹爮椤哄簭瀵?treeRoots 鎺掑簭銆?     */
//#endif
    private void sortByCreativeOrder() {
        Map<String, Integer> sortOrder = buildCreativeSortOrder();
        treeRoots.sort((a, b) -> {
            int orderA = sortOrder.getOrDefault(a.itemId, Integer.MAX_VALUE);
            int orderB = sortOrder.getOrDefault(b.itemId, Integer.MAX_VALUE);
            return Integer.compare(orderA, orderB);
        });
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 构建创造模式物品栏排序映射：物品ID → 排序位置。
     */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /**
//$$     * 鏋勫缓鍒涢€犳ā寮忕墿鍝佹爮鎺掑簭鏄犲皠锛氱墿鍝両D 鈫?鎺掑簭浣嶇疆銆?     */
//#endif
    private Map<String, Integer> buildCreativeSortOrder() {
        Map<String, Integer> order = new LinkedHashMap<>();
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        MinecraftClient client = MinecraftClient.getInstance();
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (client.world == null) return order;
//#endif

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        var registry = client.world.getRegistryManager().getOptional(RegistryKeys.ITEM_GROUP);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (registry.isEmpty()) return order;
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 11701
//$$        // 1.18.2：物品组为静态数组（ItemGroup.GROUPS），物品列表通过 appendStacks 填充
//#elseif MC == 11904
//$$        // 1.19.4：物品组为静态列表（ItemGroups.getGroups()），无 ITEM_GROUP 注册表
//#endif
        int groupIndex = 0;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        for (var group : registry.get()) {
//#elseif MC == 11904
//$$        for (net.minecraft.item.ItemGroup group : net.minecraft.item.ItemGroups.getGroups()) {
//#endif
//#if MC == 11701
//$$        for (net.minecraft.item.ItemGroup group : net.minecraft.item.ItemGroup.GROUPS) {
//#endif
//#if MC == 11701
//$$            net.minecraft.util.collection.DefaultedList<ItemStack> stacks = net.minecraft.util.collection.DefaultedList.of();
//#endif
//#if MC == 11701
//$$            group.appendStacks(stacks);
//#endif
            int itemIndex = 0;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            for (ItemStack stack : group.getDisplayStacks()) {
//#elseif MC == 11701
//$$            for (ItemStack stack : stacks) {
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                String id = Registries.ITEM.getId(stack.getItem()).toString();
//#elseif MC == 11701
//$$                String id = Registry.ITEM.getId(stack.getItem()).toString();
//#endif
                if (!order.containsKey(id)) {
                    order.put(id, groupIndex * 10000 + itemIndex);
                }
                itemIndex++;
            }
            groupIndex++;
        }
        return order;
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ==================== 滚动条 ====================
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ==================== 婊氬姩鏉?====================
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 渲染垂直和水平滚动条。
     */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /**
//$$     * 娓叉煋鍨傜洿鍜屾按骞虫粴鍔ㄦ潯銆?     */
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void renderScrollbars(DrawContext ctx, int mouseX, int mouseY) {
//#elseif MC == 11701 || MC == 11904
//$$    private void renderScrollbars(MatrixStack matrices, int mouseX, int mouseY) {
//#endif
        int treeAreaTop = CONTROLS_BOTTOM;
        int treeAreaBottom = this.height - TREE_BOTTOM_OFFSET;
        int treeAreaHeight = treeAreaBottom - treeAreaTop;
        int treeAreaWidth = this.width;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // ---- 垂直滚动条（右侧） ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // ---- 鍨傜洿婊氬姩鏉★紙鍙充晶锛?----
//#endif
        if (maxScrollY > 0) {
            int trackX = this.width - SCROLLBAR_THICKNESS - 2;
            int trackY = treeAreaTop;
            int trackH = treeAreaHeight;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 滚动条轨道
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 婊氬姩鏉¤建閬?
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            ctx.fill(trackX, trackY, trackX + SCROLLBAR_THICKNESS, trackY + trackH, 0x30FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, trackX, trackY, trackX + SCROLLBAR_THICKNESS, trackY + trackH, 0x30FFFFFF);
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 滚动条滑块
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 婊氬姩鏉℃粦鍧?
//#endif
            int thumbH = Math.max(SCROLLBAR_MIN_THUMB,
                    (int) ((double) treeAreaHeight / (totalTreeHeight + treeAreaHeight) * treeAreaHeight));
            int thumbY = trackY + (int) ((double) scrollY / maxScrollY * (trackH - thumbH));
            int thumbColor = draggingVScrollbar ? 0xA0FFFFFF : 0x60FFFFFF;
            if (!draggingVScrollbar && mouseX >= trackX && mouseX < trackX + SCROLLBAR_THICKNESS
                    && mouseY >= thumbY && mouseY < thumbY + thumbH) {
                thumbColor = 0x80FFFFFF;
            }
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            ctx.fill(trackX, thumbY, trackX + SCROLLBAR_THICKNESS, thumbY + thumbH, thumbColor);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, trackX, thumbY, trackX + SCROLLBAR_THICKNESS, thumbY + thumbH, thumbColor);
//#endif
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // ---- 水平滚动条（底部，4px高，完全贴底） ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // ---- 姘村钩婊氬姩鏉★紙搴曢儴锛?px楂橈紝瀹屽叏璐村簳锛?----
//#endif
        if (maxScrollX > minScrollX) {
            int hScrollbarHeight = 4;
            int trackY = this.height - hScrollbarHeight;
            int trackX = 0;
            int trackW = treeAreaWidth;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 滚动条轨道
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 婊氬姩鏉¤建閬?
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            ctx.fill(trackX, trackY, trackX + trackW, trackY + hScrollbarHeight, 0x30FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, trackX, trackY, trackX + trackW, trackY + hScrollbarHeight, 0x30FFFFFF);
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 滚动条滑块
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 婊氬姩鏉℃粦鍧?
//#endif
            int thumbW = Math.max(SCROLLBAR_MIN_THUMB,
                    (int) ((double) treeAreaWidth / (totalTreeWidth + treeAreaWidth) * trackW));
            int thumbX = trackX + (int) ((scrollX - minScrollX) / (maxScrollX - minScrollX) * (trackW - thumbW));
            int thumbColor = draggingHScrollbar ? 0xA0FFFFFF : 0x60FFFFFF;
            if (!draggingHScrollbar && mouseX >= thumbX && mouseX < thumbX + thumbW
                    && mouseY >= trackY && mouseY < trackY + hScrollbarHeight) {
                thumbColor = 0x80FFFFFF;
            }
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            ctx.fill(thumbX, trackY, thumbX + thumbW, trackY + hScrollbarHeight, thumbColor);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, thumbX, trackY, thumbX + thumbW, trackY + hScrollbarHeight, thumbColor);
//#endif
        }
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ==================== 节点计数 ====================
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ==================== 鑺傜偣璁℃暟 ====================
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 计算树中总物品项数和原材料项数。
     * @return [总物品项数, 原材料项数]
     */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /**
//$$     * 璁＄畻鏍戜腑鎬荤墿鍝侀」鏁板拰鍘熸潗鏂欓」鏁般€?     * @return [鎬荤墿鍝侀」鏁? 鍘熸潗鏂欓」鏁癩
//$$     */
//#endif
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
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif