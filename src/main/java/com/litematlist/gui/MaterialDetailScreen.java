package com.litematlist.gui;
//#if MC == 11701
//$$import net.minecraft.text.TranslatableText;
//#endif
//#if MC == 11701
//$$import net.minecraft.text.LiteralText;
//#endif

//#if MC == 12110 || MC == 12111
import com.litematlist.LitematListMod;
//#endif
import com.litematlist.LitematicReader;
import com.litematlist.LitematicaBridge;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import com.litematlist.MaterialListExporter;
//#endif
import com.litematlist.MaterialListImporter;
import com.litematlist.MaterialListInjector;
//#if MC == 11701 || MC == 11904
//$$import com.litematlist.MaterialListExporter;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$import com.litematlist.PinyinSearch;
//#endif
import com.litematlist.I18n;
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$import com.litematlist.LitematListMod;
//#endif
import com.litematlist.RawMaterialAnalyzer;
import com.litematlist.SyncmaticaBridge;
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$import com.litematlist.LitematListMod;
//#endif
import com.litematlist.config.Configs;
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$import com.litematlist.PinyinSearch;
//#endif
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.util.InventoryUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.gui.DrawContext;
//#endif
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.gui.DrawableHelper;
//#endif
//#if MC == 11701 || MC == 11904
//$$import com.mojang.blaze3d.systems.RenderSystem;
//#endif
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.util.math.MatrixStack;
//#endif
//#if MC == 12110 || MC == 12111
import net.minecraft.client.gui.Click;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$
//#endif
import net.minecraft.client.gui.widget.TextFieldWidget;
//#if MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.gl.RenderPipelines;
//#elseif MC == 12104
//$$import net.minecraft.client.render.RenderLayer;
//#endif
//#if MC == 12110 || MC == 12111
import net.minecraft.client.input.KeyInput;
//#endif
//#if MC == 12110 || MC == 12111
import net.minecraft.client.input.CharInput;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$
//#endif
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
//#if MC == 12110 || MC == 12111
import com.litematlist.PinyinSearch;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.registry.Registries;
//#elseif MC == 11701
//$$import net.minecraft.util.registry.Registry;
//#endif
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.litematlist.gui.MaterialDataStore.*;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
/**
 * 材料清单子界面 —— 参照投影的 MaterialList 界面布局。
 * 总计/已有/缺失左置，替换/忽略按钮贴右。
 */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$/**
//$$ * 鏉愭枡娓呭崟瀛愮晫闈?鈥斺€?鍙傜収鎶曞奖鐨?MaterialList 鐣岄潰甯冨眬銆?
//$$ * 鎬昏/宸叉湁/缂哄け宸︾疆锛屾浛鎹?蹇界暐鎸夐挳璐村彸銆?
//$$ */
//#endif
public class MaterialDetailScreen extends GuiBase {

    private final GuiBase parent;
    private final String schematicName;
    private final Path filePath;
    private final List<MaterialRow> rows = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int ROW_HEIGHT = 22;
    private static final int HEADER_Y = 42;
    private static final int HEADER_HEIGHT = 16;
    private static final int LIST_TOP = HEADER_Y + HEADER_HEIGHT + 2;
    /** 名称列最大显示宽度（像素）：过长（如精确物品ID）时截断显示 */
    private static final int MAX_NAME_DISPLAY_WIDTH = 200;
    private int visibleRows;
    private boolean scrollbarDragging = false;
    private boolean loaded = false;
    private int hoveredRow = -1;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private int hoveredColumn = -1; // 0=总计, 1=已有, 2=缺失
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    private int hoveredColumn = -1; // 0=鎬昏, 1=宸叉湁, 2=缂哄け
//#endif

//#if MC == 11701 || MC == 11904
//$$    // ---- 宸插拷鐣ョ墿鍝?----
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 已忽略物品 ----
//#endif
    private Set<Item> currentIgnored;
    // ---- 已忽略的精确物品（按精确ID区分，与普通物品的按Item区分并存） ----
    private Set<String> currentExactIgnored;
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$
//#endif
//#if MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 替换模式 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 鏇挎崲妯″紡 ----
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private int replacingRow = -1; // 当前处于替换模式的行索引
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    private int replacingRow = -1; // 褰撳墠澶勪簬鏇挎崲妯″紡鐨勮绱㈠紩
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 隐藏不缺材料 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 闅愯棌涓嶇己鏉愭枡 ----
//#endif
    private boolean hideNoMissing = persistedHideNoMissing;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // 隐藏不缺材料按钮位置（用于自定义渲染开/关颜色）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // 闅愯棌涓嶇己鏉愭枡鎸夐挳浣嶇疆锛堢敤浜庤嚜瀹氫箟娓叉煋寮€/鍏抽鑹诧級
//#endif
    private int hideBtnX, hideBtnY, hideBtnW;
    private String hideBtnLabel = "";
//#if MC == 11701 || MC == 11904
//$$    // ---- 鎵嬪姩閲嶆帓 ----
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 手动重排 ----
//#endif
    private int draggedRow = -1;
    private int dragStartY = 0;
    private boolean isDragging = false;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 列位置 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 鍒椾綅缃?----
//#endif
    private int colTotalX;
    private int colHaveX;
    private int colMissX;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 排序 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 鎺掑簭 ----
//#endif
    private SortMode sortMode = SortMode.NONE;

    private void initSortMode() {
        try { sortMode = SortMode.valueOf(persistedDetailSortMode); } catch (Exception e) { sortMode = SortMode.NONE; }
    }

    private enum SortMode {
        NONE(""),
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        TOTAL_DESC("▼"),
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        TOTAL_DESC("\u25BC"),
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        TOTAL_ASC("▲"),
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        TOTAL_ASC("\u25B2"),
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        HAVE_DESC("▼"),
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        HAVE_DESC("\u25BC"),
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        HAVE_ASC("▲"),
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        HAVE_ASC("\u25B2"),
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        MISS_DESC("▼"),
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        MISS_DESC("\u25BC"),
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        MISS_ASC("▲");
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        MISS_ASC("\u25B2");
//#endif

        final String arrow;
        SortMode(String arrow) { this.arrow = arrow; }
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 按钮布局 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 鎸夐挳甯冨眬 ----
//#endif
    private static final int BTN_REPLACE_W = 52;
    private static final int BTN_IGNORE_W = 52;
    private static final int BTN_CANCEL_W = 52;
    private static final int BTN_GAP = 2;
    private static final int BTN_RIGHT_MARGIN = 12;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 标记材料 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 鏍囪鏉愭枡 ----
//#endif
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
    private static final Identifier SEARCH_ICON = Identifier.of("litematlist", "textures/gui/search.png");
//#elseif MC == 11701
//$$    private static final Identifier SEARCH_ICON = new Identifier("litematlist", "textures/gui/search.png");
//#endif

    private boolean allowPin = persistedAllowPin;
    private int pinBtnX, pinBtnY, pinBtnW;
    private String pinBtnLabel = "";
    private Set<Item> currentPinned;
    // ---- 已置顶的精确物品（按精确ID区分） ----
    private Set<String> currentExactPinned;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ---- 搜索 ----
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ---- 鎼滅储 ----
//#endif
    private boolean searchActive = false;
    private String searchText = "";
    private TextFieldWidget searchField;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private boolean fromUploadArea = false; // 是否从上传区域进入（禁用替换/忽略）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    private boolean fromUploadArea = false; // 鏄惁浠庝笂浼犲尯鍩熻繘鍏ワ紙绂佺敤鏇挎崲/蹇界暐锛?
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // 按钮区域左边界
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // 鎸夐挳鍖哄煙宸﹁竟鐣?
//#endif
    private int buttonAreaStart;

    public MaterialDetailScreen(GuiBase parent, String schematicName, Path filePath) {
        this(parent, schematicName, filePath, false);
    }

    public MaterialDetailScreen(GuiBase parent, String schematicName, Path filePath, boolean fromUploadArea) {
        super();
        this.parent = parent;
        this.schematicName = schematicName;
        this.filePath = filePath;
        this.fromUploadArea = fromUploadArea;
        this.title = I18n.tr("litematlist.title.material_detail", schematicName);
        this.currentIgnored = ignoredItems.computeIfAbsent(filePath, k -> new HashSet<>());
        this.currentPinned = pinnedItems.computeIfAbsent(filePath, k -> new HashSet<>());
        this.currentExactIgnored = exactIgnoredItems.computeIfAbsent(filePath, k -> new HashSet<>());
        this.currentExactPinned = exactPinnedItems.computeIfAbsent(filePath, k -> new HashSet<>());
        initSortMode();
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 当前条目是否已上传到上传区域 */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 褰撳墠鏉＄洰鏄惁宸蹭笂浼犲埌涓婁紶鍖哄煙 */
//#endif
    private boolean isEntryUploaded() {
        MaterialListScreen.LoadedEntry uploaded = MaterialListScreen.getUploadedEntry();
        return uploaded != null && uploaded.filePath() != null && uploaded.filePath().equals(filePath);
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 如果当前条目已上传，立即重新注入材料列表 */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 濡傛灉褰撳墠鏉＄洰宸蹭笂浼狅紝绔嬪嵆閲嶆柊娉ㄥ叆鏉愭枡鍒楄〃 */
//#endif
    private void reinjectIfUploaded() {
        MaterialListScreen.LoadedEntry uploaded = MaterialListScreen.getUploadedEntry();
        if (uploaded != null && uploaded.filePath() != null && uploaded.filePath().equals(filePath)) {
            MaterialListInjector.injectFromEntry(uploaded);
            MaterialListInjector.markInjected(uploaded);
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            LitematListMod.LOGGER.info("[MaterialDetailScreen] 已上传条目操作后重新注入: {}", filePath);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            LitematListMod.LOGGER.info("[MaterialDetailScreen] 宸蹭笂浼犳潯鐩搷浣滃悗閲嶆柊娉ㄥ叆: {}", filePath);
//#endif
        }
    }

    /**
     * 全局解析设置（如「是否计算容器数据」）切换后重新加载并刷新界面。
     */
    public void refreshAfterGlobalSettingChange() {
        this.loaded = false;
        initGui();
    }

    @Override
    public void initGui() {
        super.initGui();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        this.replacingRow = -1; // 每次重建界面时清除选择模式
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        this.replacingRow = -1; // 姣忔閲嶅缓鐣岄潰鏃舵竻闄ら€夋嫨妯″紡
//#endif
        this.visibleRows = (this.height - 100) / ROW_HEIGHT;

        if (!loaded) {
            loadMaterials();
            loaded = true;
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 重新绑定 currentIgnored/currentPinned，确保它们引用 ignoredItems/pinnedItems 中的实际集合
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 閲嶆柊缁戝畾 currentIgnored/currentPinned锛岀‘淇濆畠浠紩鐢?ignoredItems/pinnedItems 涓殑瀹為檯闆嗗悎
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // （因为 ensurePersistenceLoaded 可能在 loadMaterials 中首次调用，需要刷新引用）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 锛堝洜涓?ensurePersistenceLoaded 鍙兘鍦?loadMaterials 涓娆¤皟鐢紝闇€瑕佸埛鏂板紩鐢級
//#endif
        this.currentIgnored = ignoredItems.computeIfAbsent(filePath, k -> new HashSet<>());
        this.currentPinned = pinnedItems.computeIfAbsent(filePath, k -> new HashSet<>());
        this.currentExactIgnored = exactIgnoredItems.computeIfAbsent(filePath, k -> new HashSet<>());
        this.currentExactPinned = exactPinnedItems.computeIfAbsent(filePath, k -> new HashSet<>());
        calculateColumnPositions();

        this.buttonAreaStart = this.width - BTN_RIGHT_MARGIN - BTN_IGNORE_W - BTN_GAP - BTN_REPLACE_W - BTN_GAP - BTN_CANCEL_W - BTN_GAP;

        int buttonY = this.height - 38;

        ButtonGeneric backButton = new ButtonGeneric(this.width / 2 - 60, buttonY, 120, 20, I18n.tr("litematlist.button.back"));
        backButton.setRenderDefaultBackground(true);
        this.addButton(backButton, (IButtonActionListener) (btn, mb) ->
                MinecraftClient.getInstance().setScreen(parent));

        int ignoredCount = currentIgnored.size();
        String ignoredLabel = ignoredCount > 0 ? I18n.tr("litematlist.button.ignored_count", ignoredCount) : I18n.tr("litematlist.button.ignored");
        int ignoredBtnW = Math.max(60, this.textRenderer.getWidth(ignoredLabel) + 16);

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 隐藏不缺材料按钮 —— 放在"物品"表头上方
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 闅愯棌涓嶇己鏉愭枡鎸夐挳 鈥斺€?鏀惧湪"鐗╁搧"琛ㄥご涓婃柟
//#endif
        String hideLabel = hideNoMissing ? I18n.tr("litematlist.button.hide_no_missing_on") : I18n.tr("litematlist.button.hide_no_missing_off");
        int hideBtnW = Math.max(60, this.textRenderer.getWidth(hideLabel) + 16);
        int hideBtnX = 20;
        int hideBtnY = 20;
        ButtonGeneric hideBtn = new ButtonGeneric(hideBtnX, hideBtnY, hideBtnW, 20, "");
        hideBtn.setRenderDefaultBackground(true);
        this.addButton(hideBtn, (IButtonActionListener) (b, mb) -> {
            hideNoMissing = !hideNoMissing;
            persistedHideNoMissing = hideNoMissing;
            savePersistence();
            initGui();
        });
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 保存按钮位置用于自定义渲染
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 淇濆瓨鎸夐挳浣嶇疆鐢ㄤ簬鑷畾涔夋覆鏌?
//#endif
        this.hideBtnX = hideBtnX;
        this.hideBtnY = hideBtnY;
        this.hideBtnW = hideBtnW;
        this.hideBtnLabel = hideLabel;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 允许标记材料按钮
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鍏佽鏍囪鏉愭枡鎸夐挳
//#endif
        String pinLabel = allowPin ? I18n.tr("litematlist.button.allow_pin_on") : I18n.tr("litematlist.button.allow_pin_off");
        int pinBtnW = Math.max(60, this.textRenderer.getWidth(pinLabel) + 16);
        int pinBtnX = hideBtnX + hideBtnW + 4;
        int pinBtnY = 20;
        ButtonGeneric pinBtn = new ButtonGeneric(pinBtnX, pinBtnY, pinBtnW, 20, "");
        pinBtn.setRenderDefaultBackground(true);
        this.addButton(pinBtn, (IButtonActionListener) (b, mb) -> {
            allowPin = !allowPin;
            persistedAllowPin = allowPin;
            savePersistence();
            initGui();
        });
        this.pinBtnX = pinBtnX;
        this.pinBtnY = pinBtnY;
        this.pinBtnW = pinBtnW;
        this.pinBtnLabel = pinLabel;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 导出按钮
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 瀵煎嚭鎸夐挳
//#endif
        int exportBtnX = pinBtnX + pinBtnW + 4;
        ButtonGeneric exportTxtBtn = new ButtonGeneric(exportBtnX, 20, 70, 20, I18n.tr("litematlist.button.export_txt"));
        exportTxtBtn.setRenderDefaultBackground(true);
        this.addButton(exportTxtBtn, (IButtonActionListener) (b, mb) -> exportMaterials("txt"));

        ButtonGeneric exportJsonBtn = new ButtonGeneric(exportBtnX + 74, 20, 70, 20, I18n.tr("litematlist.button.export_json"));
        exportJsonBtn.setRenderDefaultBackground(true);
        this.addButton(exportJsonBtn, (IButtonActionListener) (b, mb) -> exportMaterials("json"));

        ButtonGeneric exportCsvBtn = new ButtonGeneric(exportBtnX + 148, 20, 70, 20, I18n.tr("litematlist.button.export_csv"));
        exportCsvBtn.setRenderDefaultBackground(true);
        this.addButton(exportCsvBtn, (IButtonActionListener) (b, mb) -> exportMaterials("csv"));

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 搜索框
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鎼滅储妗?
//#endif
        int searchIconX = allowPin ? 33 : 15;
        if (searchField == null) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            this.searchField = new TextFieldWidget(this.textRenderer, searchIconX + 18, HEADER_Y, 80, 14, Text.empty());
//#elseif MC == 11701
//$$            this.searchField = new TextFieldWidget(this.textRenderer, searchIconX + 18, HEADER_Y, 80, 14, LiteralText.EMPTY);
//#endif
            this.searchField.setMaxLength(50);
            this.searchField.setDrawsBackground(false);
            this.searchField.setText(searchText);
            this.addSelectableChild(this.searchField);
        } else {
            this.searchField.setX(searchIconX + 18);
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            this.searchField.setY(HEADER_Y);
//#elseif MC == 11701
//$$            this.searchField.y = HEADER_Y;
//#endif
            this.searchField.setText(searchText);
            this.addSelectableChild(this.searchField);
        }

        ButtonGeneric ignoredBtn = new ButtonGeneric(
                this.width - ignoredBtnW - 12, 10, ignoredBtnW, 20, ignoredLabel);
        ignoredBtn.setRenderDefaultBackground(true);
        this.addButton(ignoredBtn, (IButtonActionListener) (b, mb) -> {
            Map<Item, Integer> ignoredTotals = new HashMap<>();
            for (MaterialRow row : rows) {
                // 精确物品行不参与忽略
                if (!row.exact && currentIgnored.contains(row.item)) {
                    ignoredTotals.put(row.item, row.totalCount);
                }
            }
            MinecraftClient.getInstance().setScreen(
                    new IgnoredItemsScreen(this, schematicName, filePath, filePath.toString(), currentIgnored, ignoredTotals));
        });

        createRowButtons();
    }

    private void calculateColumnPositions() {
        int maxTotalWidth = this.textRenderer.getWidth(I18n.tr("litematlist.header.total"));
        int maxHaveWidth = this.textRenderer.getWidth(I18n.tr("litematlist.header.have"));
        int maxMissWidth = this.textRenderer.getWidth(I18n.tr("litematlist.header.missing"));
        int maxNameWidth = this.textRenderer.getWidth(I18n.tr("litematlist.header.item"));

        for (MaterialRow row : rows) {
            // 钳制名称列宽：精确物品ID可能非常长，避免撑爆列布局（有样式显示文本时按显示文本计宽）
            int nameWidth = row.displayText != null
                    ? this.textRenderer.getWidth(row.displayText)
                    : this.textRenderer.getWidth(row.name);
            maxNameWidth = Math.max(maxNameWidth, Math.min(nameWidth, MAX_NAME_DISPLAY_WIDTH));
            maxTotalWidth = Math.max(maxTotalWidth, this.textRenderer.getWidth(String.valueOf(row.totalCount)));
        }

        int nameEndX = 35 + maxNameWidth + 20;
        colTotalX = nameEndX + 24;
        colHaveX = colTotalX + maxTotalWidth + 24;
        colMissX = colHaveX + maxHaveWidth + 24;
    }

    private void loadMaterials() {
        rows.clear();
        ensurePersistenceLoaded();
        try {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 优先从导入缓存加载（txt 文件导入的材料）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 浼樺厛浠庡鍏ョ紦瀛樺姞杞斤紙txt 鏂囦欢瀵煎叆鐨勬潗鏂欙級
//#endif
            List<LitematicReader.MaterialEntry> entries;
            if (importedMaterials.containsKey(filePath)) {
                entries = importedMaterials.get(filePath);
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                LitematListMod.LOGGER.info("从导入缓存加载材料: {} ({} 种)", filePath, entries.size());
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                LitematListMod.LOGGER.info("Loaded {} entries ({} ignored)", filePath, entries.size());
//#endif
            } else if (filePath.toString().toLowerCase().endsWith(".txt") || filePath.toString().toLowerCase().endsWith(".json") || filePath.toString().toLowerCase().endsWith(".csv")) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // txt/json 文件：重启后缓存已丢失，重新解析
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                // txt/json 鏂囦欢锛氶噸鍚悗缂撳瓨宸蹭涪澶憋紝閲嶆柊瑙ｆ瀽
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                LitematListMod.LOGGER.info("文件重新解析: {}", filePath);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                LitematListMod.LOGGER.info("Material list synced: {0}", filePath);
//#endif
                MaterialListImporter.ImportResult result = MaterialListImporter.importFromFile(filePath);
                if (result != null) {
                    entries = result.materials();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    importedMaterials.put(filePath, entries); // 重新缓存
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    importedMaterials.put(filePath, entries); // 閲嶆柊缂撳瓨
//#endif
                } else {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    LitematListMod.LOGGER.error("txt 文件重新解析失败: {}", filePath);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    LitematListMod.LOGGER.error("Failed to sync txt material list: {0}", filePath);
//#endif
                    return;
                }
            } else if (SyncmaticaBridge.isSyncPath(filePath)) {
                // 「在Syncmatica认领的材料」：每次打开都向 Syncmatica 重新拉取最新认领数据
                entries = SyncmaticaBridge.loadClaimedMaterialEntries();
                LitematListMod.LOGGER.info("从 Syncmatica 拉取认领材料: {} 种", entries.size());
            } else {
                entries = loadLitematicEntries();
            }
            for (LitematicReader.MaterialEntry entry : entries) {
                Item item = entry.item();
                if (entry.isExact()) {
                    // 精确物品（带非默认组件的特殊物品）：name 保存精确物品ID（导出/悬停用，也是忽略/置顶/替换的稳定键），
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    // 显示文本用 getFormattedName()（与游戏内物品名一致：自定义名自动斜体、稀有度颜色）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    // 显示文本用 getName()（与游戏内物品名一致）
//#elseif MC == 12101
//$$                    // 显示文本用 getName()（1.21.1 无 getFormattedName，取翻译后的物品名）
//#endif
                    ItemStack exactStack = entry.toItemStack();
                    String exactId = entry.blockName();
                    // 应用已保存的精确物品替换（展示替换后的物品，但保留精确键 row.name 与 exact 标记）
                    Map<String, String> exactFileRepl = exactReplacements.get(filePath.toString());
                    Item displayItem = item;
                    ItemStack displayStack = exactStack;
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    Text displayText = exactStack.getFormattedName();
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$                    Text displayText = exactStack.getName();
//#endif
                    if (exactFileRepl != null) {
                        String replId = exactFileRepl.get(exactId);
                        if (replId != null) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            Item replaced = Registries.ITEM.get(Identifier.of(replId));
//#elseif MC == 11701
//$$                            Item replaced = Registry.ITEM.get(Identifier.tryParse(replId));
//#elseif MC == 11904 || MC == 12001
//$$                            Item replaced = Registries.ITEM.get(Identifier.tryParse(replId));
//#endif
                            if (replaced != null && replaced != Items.AIR) {
                                displayItem = replaced;
                                displayStack = new ItemStack(replaced, entry.totalCount());
                                displayText = replaced.getName().copy();
                            }
                        }
                    }
                    rows.add(new MaterialRow(
                            displayStack,
                            exactId,
                            entry.totalCount(),
                            displayItem,
                            item, // originalItem 始终是原理图原始基础物品
                            entry.missingCount(),
                            true,
                            displayText
                    ));
                    continue;
                }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                // 应用已保存的替换
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                // 搴旂敤宸蹭繚瀛樼殑鏇挎崲
//#endif
                Item replaced = getReplacement(filePath, item);
                rows.add(new MaterialRow(
                        new ItemStack(replaced, entry.totalCount()),
                        replaced.getName().getString(),
                        entry.totalCount(),
                        replaced,
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        item, // originalItem 始终是原理图原始物品
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                        item, // originalItem 濮嬬粓鏄師鐞嗗浘鍘熷鐗╁搧
//#endif
                        entry.missingCount()
                ));
            }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            LitematListMod.LOGGER.info("加载了 {} 种材料（已忽略 {} 种）", rows.size(), currentIgnored.size());
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            LitematListMod.LOGGER.info("Synced {} rows, {} ignored", rows.size(), currentIgnored.size());
//#endif
        } catch (Exception e) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            LitematListMod.LOGGER.error("解析材料列表失败: {}", filePath, e);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            LitematListMod.LOGGER.error("Failed to sync material list: {0}", filePath, e);
//#endif
        }
    }

    /**
     * 加载 .litematic 材料：
     * 本条目来自「从投影同步」且开启「同步投影渲染层」时，按投影当前渲染层规则过滤解析；
     * 解析完成后联动刷新上传区域数据与原材料表。
     */
    private List<LitematicReader.MaterialEntry> loadLitematicEntries() {
        LitematicaBridge.LayerFilter filter = null;
        boolean syncLayer = isSyncLayerEntry() && Configs.FeatureToggles.SYNC_RENDER_LAYER.getBooleanValue();
        if (syncLayer) {
            filter = LitematicaBridge.computeRenderLayerFilter(filePath);
            if (filter != null) {
                LitematListMod.LOGGER.info("[MaterialDetailScreen] 同步投影渲染层: 轴={} 相对层级区间 [{}..{}]: {}",
                        filter.axis(), filter.min(), filter.max(), filePath);
            }
        }
        List<LitematicReader.MaterialEntry> list = LitematicReader.loadMaterialList(filePath, filter);

        if (syncLayer) {
            // 上传状态条目：立即更新上传区域数据（重新注入 DataManager + HUD）
            // 原材料列表：解析完成后联动重新分析，无需玩家打开原材料界面
            try {
                reinjectIfUploaded();
                MaterialListHudRenderer.forceRefreshHud();
                autoRefreshRawMaterials();
            } catch (Exception e) {
                LitematListMod.LOGGER.error("[MaterialDetailScreen] 同步投影渲染层联动刷新失败（已捕获，不影响游戏运行）", e);
            }
        }
        return list;
    }

    /** 判断当前条目是否来自「从投影同步」 */
    private boolean isSyncLayerEntry() {
        for (MaterialListScreen.LoadedEntry e : MaterialListScreen.getLoadedEntries()) {
            if (e.fromLitematica() && e.filePath() != null && e.filePath().equals(filePath)) {
                return true;
            }
        }
        return false;
    }

    /** 按新渲染层规则解析完成后，自动重新分析原材料表与冗余数据 */
    private void autoRefreshRawMaterials() {
        List<LitematicReader.MaterialEntry> materials = MaterialListInjector.loadMaterialsCached(filePath);
        if (materials == null || materials.isEmpty()) {
            return;
        }
        RawMaterialAnalyzer rawAnalyzer = new RawMaterialAnalyzer();
        RawMaterialAnalyzer.RawMaterialResult rawResult = rawAnalyzer.analyze(materials);
        String pathKey = filePath.toString();
        rawMaterials.put(pathKey, rawResult.rawMaterials());
        rawMaterialRedundancy.put(pathKey, rawResult.redundancy());
        savePersistence();
        LitematListMod.LOGGER.info("[MaterialDetailScreen] 已按渲染层规则更新原材料表: {} ({} 项原材料)",
                filePath, rawResult.rawMaterials().size());
    }

    private void createRowButtons() {
        List<MaterialRow> displayRows = getSortedRows();
        int maxIdx = Math.min(displayRows.size(), scrollOffset + visibleRows);

        for (int i = scrollOffset; i < maxIdx; i++) {
            MaterialRow row = displayRows.get(i);
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;
            final int visibleIdx = i;

            int ignoreX = this.width - BTN_RIGHT_MARGIN - BTN_IGNORE_W;
            int replaceX = ignoreX - BTN_GAP - BTN_REPLACE_W;
            int cancelX = replaceX - BTN_GAP - BTN_CANCEL_W;

            final MaterialRow targetRow = row;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 取消替换按钮（物品已被替换时显示，可恢复为原理图原始物品/精确物品原状）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 鍙栨秷鏇挎崲鎸夐挳锛堢墿鍝佸凡琚浛鎹㈡椂鏄剧ず锛屽彲鎭㈠涓哄師鐞嗗浘鍘熷鐗╁搧锛?
//#endif
            if (row.item != row.originalItem) {
                ButtonGeneric cancelBtn = new ButtonGeneric(cancelX, rowY + 1, BTN_CANCEL_W, 20, I18n.tr("litematlist.button.cancel_replace"));
                cancelBtn.setRenderDefaultBackground(true);
                this.addButton(cancelBtn, (IButtonActionListener) (b, mb) -> {
                    restoreOriginalItem(targetRow);
                    initGui();
                });
            }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 替换按钮
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 鏇挎崲鎸夐挳
//#endif
            {
                ButtonGeneric replaceBtn = new ButtonGeneric(replaceX, rowY + 1, BTN_REPLACE_W, 20, I18n.tr("litematlist.button.replace"));
                replaceBtn.setRenderDefaultBackground(true);
                this.addButton(replaceBtn, (IButtonActionListener) (b, mb) -> {
                    replacingRow = visibleIdx;
                    MinecraftClient.getInstance().setScreen(
                            new BlockPickerScreen(this, newItem -> replaceItem(targetRow, newItem)));
                });
            }

//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            // 蹇界暐鎸夐挳锛堢疆椤舵潗鏂欎笉鏄剧ず蹇界暐鎸夐挳锛?
//#endif
            // 忽略按钮（置顶材料不显示忽略按钮，精确物品按精确ID判断）
            if (!isRowPinned(row)) {
                ButtonGeneric ignoreBtn = new ButtonGeneric(ignoreX, rowY + 1, BTN_IGNORE_W, 20, I18n.tr("litematlist.button.ignore"));
                ignoreBtn.setRenderDefaultBackground(true);
                this.addButton(ignoreBtn, (IButtonActionListener) (b, mb) -> {
                    ignoreItem(visibleIdx);
                    initGui();
                });
            }
        }
    }

    private void ignoreItem(int visibleIdx) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        replacingRow = -1; // 点击忽略时取消替换模式
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        replacingRow = -1; // 鐐瑰嚮蹇界暐鏃跺彇娑堟浛鎹㈡ā寮?
//#endif
        List<MaterialRow> displayRows = getSortedRows();
        if (visibleIdx >= 0 && visibleIdx < displayRows.size()) {
            MaterialRow row = displayRows.get(visibleIdx);
            if (row.exact) {
                // 精确物品：按精确ID加入忽略集合
                if (isRowPinned(row)) return;
                currentExactIgnored.add(row.name);
            } else {
                if (isRowPinned(row)) return;
                currentIgnored.add(row.item);
            }
            // 清除缓存的原材料分析数据，下一次打开原材料列表时重新分析（排除已忽略物品）
            String pathKey = filePath.toString();
            rawMaterials.remove(pathKey);
            rawMaterialRedundancy.remove(pathKey);
            savePersistence(); // 立即持久化忽略
            reinjectIfUploaded(); // 上传条目需立即重新注入
            MaterialListHudRenderer.refreshHud(); // 更新HUD
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            LitematListMod.LOGGER.info("忽略物品: {} ({})", row.name, row.item);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            LitematListMod.LOGGER.info("Replaced: {0} ({1})", row.name, row.item);
//#endif
        }
    }

    /** 该行是否已置顶（普通物品按 Item，精确物品按精确ID） */
    private boolean isRowPinned(MaterialRow row) {
        if (!allowPin) return false;
        return row.exact ? currentExactPinned.contains(row.name) : currentPinned.contains(row.originalItem);
    }
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 12110 || MC == 12111

//#endif
    /** 该行是否已忽略（普通物品按 Item，精确物品按精确ID） */
    private boolean isRowIgnored(MaterialRow row) {
        return row.exact ? currentExactIgnored.contains(row.name) : currentIgnored.contains(row.item);
    }
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 12110 || MC == 12111

//#endif
    /** 精确物品是否已被替换（替换映射按精确ID记录）；被替换后悬停不再显示原精确ID，取消替换后恢复。 */
    private boolean isExactReplaced(String exactId) {
        Map<String, String> exactFileRepl = exactReplacements.get(filePath.toString());
        return exactFileRepl != null && exactFileRepl.containsKey(exactId);
    }
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 12110 || MC == 12111

//#endif
    /**
     * 绘制悬停提示：内容过长触及屏幕边界时自动换行，避免超出屏幕。
     * 按鼠标位置取较宽一侧的可用宽度，逐字符折行后以多行提示绘制。
     */
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void drawWrappedTooltip(DrawContext drawContext, String text, int mouseX, int mouseY) {
//#elseif MC == 11701 || MC == 11904
//$$    private void drawWrappedTooltip(MatrixStack matrices, String text, int mouseX, int mouseY) {
//#endif
        if (text == null || text.isEmpty()) {
            return;
        }
        int available = (mouseX > this.width / 2) ? mouseX - 12 : this.width - mouseX - 12;
        available = Math.max(80, Math.min(available, this.width - 24));
        List<Text> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            line.append(c);
            if (this.textRenderer.getWidth(line.toString()) > available && line.length() > 1) {
                line.setLength(line.length() - 1);
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                lines.add(Text.literal(line.toString()));
//#elseif MC == 11701
//$$                lines.add(new LiteralText(line.toString()));
//#endif
                line.setLength(0);
                line.append(c);
            }
        }
        if (line.length() > 0) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            lines.add(Text.literal(line.toString()));
//#elseif MC == 11701
//$$            lines.add(new LiteralText(line.toString()));
//#endif
        }
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$        this.renderTooltip(matrices, lines, mouseX, mouseY);
//#endif
    }

    private List<MaterialRow> getVisibleRowsOnly() {
        List<MaterialRow> visible = new ArrayList<>();
        for (MaterialRow row : rows) {
            if (isRowIgnored(row)) continue;
            if (hideNoMissing) {
                int haveCount = getRowHaveCount(row);
                if (row.totalCount - haveCount <= 0) continue;
            }
            // 搜索过滤（中文名/拼音全拼/拼音首字母/英文ID；精确物品同时支持按完整精确ID搜索）
            if (searchActive && !searchText.isEmpty()) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                String itemId = Registries.ITEM.getId(row.item).toString();
//#elseif MC == 11701
//$$                String itemId = Registry.ITEM.getId(row.item).toString();
//#endif
                String displayName = row.displayText != null ? row.displayText.getString() : row.name;
                if (!PinyinSearch.matches(searchText, displayName, itemId) && !row.name.contains(searchText)) continue;
            }
            visible.add(row);
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 应用手动重排顺序
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 搴旂敤鎵嬪姩閲嶆帓椤哄簭
//#endif
        List<Item> reorder = reorderedItems.get(filePath);
        if (reorder != null && !reorder.isEmpty()) {
            Map<Item, MaterialRow> rowMap = new HashMap<>();
            List<MaterialRow> exactRows = new ArrayList<>();
            for (MaterialRow row : visible) {
                if (row.exact) {
                    // 精确物品行不参与手动重排，保持在列表末尾
                    exactRows.add(row);
                    continue;
                }
                rowMap.put(row.originalItem, row);
            }
            List<MaterialRow> ordered = new ArrayList<>();
            for (Item item : reorder) {
                MaterialRow row = rowMap.get(item);
                if (row != null) {
                    ordered.add(row);
                    rowMap.remove(item);
                }
            }
            ordered.addAll(rowMap.values());
            ordered.addAll(exactRows);
            visible = ordered;
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 标记材料模式：将已标记的材料排到最前面（精确物品按精确ID一并参与）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鏍囪鏉愭枡妯″紡锛氬皢宸叉爣璁扮殑鏉愭枡鎺掑埌鏈€鍓嶉潰
//#endif
        if (allowPin && (!currentPinned.isEmpty() || !currentExactPinned.isEmpty())) {
            List<MaterialRow> pinned = new ArrayList<>();
            List<MaterialRow> unpinned = new ArrayList<>();
            for (MaterialRow row : visible) {
                if (isRowPinned(row)) {
                    pinned.add(row);
                } else {
                    unpinned.add(row);
                }
            }
            pinned.addAll(unpinned);
            visible = pinned;
        }
        return visible;
    }

    private int getPlayerItemCount(Item item) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return 0;
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() == item) {
                count += stack.getCount();
            } else if (InventoryUtils.shulkerBoxHasItems(stack)) {
                for (ItemStack boxStack : InventoryUtils.getStoredItems(stack)) {
                    if (boxStack.getItem() == item) {
                        count += boxStack.getCount();
                    }
                }
            }
        }
        return count;
    }

    /** 计算行的已有数量：精确物品按「物品+组件」精确匹配，普通物品按物品类型匹配 */
    private int getRowHaveCount(MaterialRow row) {
        if (row.exact && row.itemStack != null && !row.itemStack.isEmpty()) {
            return getExactPlayerCount(row.itemStack);
        }
        return getPlayerItemCount(row.item);
    }
//#if MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 玩家背包中与目标堆叠「物品+组件」完全一致的数量（含潜影盒内部） */
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 玩家背包中与目标堆叠「物品+NBT 数据」完全一致（忽略数量）的数量（含潜影盒内部） */
//#endif
    private int getExactPlayerCount(ItemStack target) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return 0;
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (ItemStack.areItemsAndComponentsEqual(stack, target)) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            if (ItemStack.canCombine(stack, target)) {
//#endif
                count += stack.getCount();
            } else if (InventoryUtils.shulkerBoxHasItems(stack)) {
                for (ItemStack boxStack : InventoryUtils.getStoredItems(stack)) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    if (ItemStack.areItemsAndComponentsEqual(boxStack, target)) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    if (ItemStack.canCombine(boxStack, target)) {
//#endif
                        count += boxStack.getCount();
                    }
                }
            }
        }
        return count;
    }
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 12110 || MC == 12111

//#endif
    /** 按实际像素宽度截断显示名（超出时以…结尾）；未超出返回原文 */
    private String truncateNameByWidth(String name, int maxWidth) {
        if (this.textRenderer.getWidth(name) <= maxWidth) return name;
        String ellipsis = "…";
        int limit = maxWidth - this.textRenderer.getWidth(ellipsis);
        int w = 0;
        for (int i = 0; i < name.length(); i++) {
            int cw = this.textRenderer.getWidth(String.valueOf(name.charAt(i)));
            if (w + cw > limit) return name.substring(0, i) + ellipsis;
            w += cw;
        }
        return name;
    }

    private String formatCountTooltip(int count) {
        int shulker = count / 1728;
        int remainder = count % 1728;
        int stacks = remainder / 64;
        int items = remainder % 64;
        double shulkerFloat = (double) count / 1728.0;
        return String.format("%d = %d×1728 + %d×64 + %d  |  %d×64+%d  |  %.2f 潜影盒",
                count, shulker, stacks, items,
                shulker * 27 + stacks, items,
                shulkerFloat);
    }

    private List<MaterialRow> getSortedRows() {
        List<MaterialRow> visible = getVisibleRowsOnly();

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 分离置顶和非置顶（精确物品按精确ID一并参与）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鍒嗙缃《鍜岄潪缃《
//#endif
        if (allowPin && (!currentPinned.isEmpty() || !currentExactPinned.isEmpty())) {
            List<MaterialRow> pinned = new ArrayList<>();
            List<MaterialRow> unpinned = new ArrayList<>();
            for (MaterialRow row : visible) {
                if (isRowPinned(row)) {
                    pinned.add(row);
                } else {
                    unpinned.add(row);
                }
            }
            pinned.addAll(unpinned);
            return pinned;
        }

        return visible;
    }

    private List<MaterialRow> sortRowList(List<MaterialRow> list) {
        List<SortableRow> sortable = new ArrayList<>();
        for (MaterialRow row : list) {
            int haveCount = getRowHaveCount(row);
            sortable.add(new SortableRow(row, haveCount, Math.max(0, row.totalCount - haveCount)));
        }

        Comparator<SortableRow> cmp = switch (sortMode) {
            case TOTAL_DESC -> Comparator.comparingInt((SortableRow r) -> r.row.totalCount).reversed();
            case TOTAL_ASC -> Comparator.comparingInt(r -> r.row.totalCount);
            case HAVE_DESC -> Comparator.comparingInt((SortableRow r) -> r.haveCount).reversed();
            case HAVE_ASC -> Comparator.comparingInt(r -> r.haveCount);
            case MISS_DESC -> Comparator.comparingInt((SortableRow r) -> r.missCount).reversed();
            case MISS_ASC -> Comparator.comparingInt(r -> r.missCount);
            default -> null;
        };

        if (cmp == null) return list;
        sortable.sort(cmp);
        return sortable.stream().map(r -> r.row).toList();
    }

    private record SortableRow(MaterialRow row, int haveCount, int missCount) {}

    private void handleHeaderClick(double mouseX, double mouseY) {
        if (mouseY < HEADER_Y || mouseY > HEADER_Y + HEADER_HEIGHT) return;

        SortMode newMode;
        if (mouseX >= colTotalX - 20 && mouseX <= colTotalX + 30) {
            newMode = (sortMode == SortMode.TOTAL_DESC) ? SortMode.TOTAL_ASC
                     : (sortMode == SortMode.TOTAL_ASC) ? SortMode.NONE
                     : SortMode.TOTAL_DESC;
        } else if (mouseX >= colHaveX - 20 && mouseX <= colHaveX + 30) {
            newMode = (sortMode == SortMode.HAVE_DESC) ? SortMode.HAVE_ASC
                     : (sortMode == SortMode.HAVE_ASC) ? SortMode.NONE
                     : SortMode.HAVE_DESC;
        } else if (mouseX >= colMissX - 20 && mouseX <= colMissX + 30) {
            newMode = (sortMode == SortMode.MISS_DESC) ? SortMode.MISS_ASC
                     : (sortMode == SortMode.MISS_ASC) ? SortMode.NONE
                     : SortMode.MISS_DESC;
        } else return;
        sortMode = newMode;
        persistedDetailSortMode = sortMode.name();
        savePersistence();
        if (newMode != SortMode.NONE) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 一次性排序
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 涓€娆℃€ф帓搴?
//#endif
            Comparator<MaterialRow> cmp = switch (newMode) {
                case TOTAL_DESC -> Comparator.comparingInt((MaterialRow r) -> r.totalCount).reversed();
                case TOTAL_ASC -> Comparator.comparingInt(r -> r.totalCount);
                case HAVE_DESC -> Comparator.comparingInt((MaterialRow r) -> r.totalCount - r.missingCount).reversed();
                case HAVE_ASC -> Comparator.comparingInt(r -> r.totalCount - r.missingCount);
                case MISS_DESC -> Comparator.comparingInt((MaterialRow r) -> r.missingCount).reversed();
                case MISS_ASC -> Comparator.comparingInt(r -> r.missingCount);
                default -> null;
            };
            if (cmp != null) rows.sort(cmp);
        }
        initGui();
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ==================== 替换逻辑 ====================
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ==================== 鏇挎崲閫昏緫 ====================
//#endif

    private void replaceItem(MaterialRow target, Item newItem) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        String pathKey = filePath.toString();
//#endif
        // 精确物品：按精确ID记录替换映射，随后重建行（保留精确键 row.name 与 exact 标记）
        if (target.exact) {
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            String pathKey = filePath.toString();
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            Identifier newId = Registries.ITEM.getId(newItem);
//#elseif MC == 11701
//$$            Identifier newId = Registry.ITEM.getId(newItem);
//#endif
            if (newId != null) {
                Map<String, String> exactFileRepl = exactReplacements.computeIfAbsent(pathKey, k -> new HashMap<>());
                exactFileRepl.put(target.name, newId.toString());
            }
            savePersistence();
            rawMaterials.remove(pathKey);
            rawMaterialRedundancy.remove(pathKey);
            loadMaterials(); // 依据映射重建全部行，精确行展示替换后物品
            reinjectIfUploaded();
            MaterialListHudRenderer.refreshHud(); // 更新HUD
            initGui();
            return;
        }
        for (int i = 0; i < rows.size(); i++) {
            if (!rows.get(i).exact && rows.get(i).item == target.item) {
                MaterialRow old = rows.get(i);
                rows.set(i, new MaterialRow(
                        new ItemStack(newItem, old.totalCount),
                        newItem.getName().getString(),
                        old.totalCount,
                        newItem,
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        old.originalItem, // 保留原始物品
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                        old.originalItem, // 淇濈暀鍘熷鐗╁搧
//#endif
                        old.missingCount
                ));
                break;
            }
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 立即持久化替换
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        // 绔嬪嵆鎸佷箙鍖栨浛鎹?
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        String pathKey = filePath.toString();
//#endif
        Map<String, String> fileReplacements = replacements.computeIfAbsent(pathKey, k -> new HashMap<>());
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Identifier origId = Registries.ITEM.getId(target.originalItem);
//#elseif MC == 11701
//$$        Identifier origId = Registry.ITEM.getId(target.originalItem);
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Identifier newId = Registries.ITEM.getId(newItem);
//#elseif MC == 11701
//$$        Identifier newId = Registry.ITEM.getId(newItem);
//#endif
        if (origId != null && newId != null) {
            fileReplacements.put(origId.toString(), newId.toString());
        }
        savePersistence();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 清除缓存的原材料分析数据，确保下次打开原材料列表时重新分析
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 娓呴櫎缂撳瓨鐨勫師鏉愭枡鍒嗘瀽鏁版嵁锛岀‘淇濅笅娆℃墦寮€鍘熸潗鏂欏垪琛ㄦ椂閲嶆柊鍒嗘瀽
//#endif
        rawMaterials.remove(pathKey);
        rawMaterialRedundancy.remove(pathKey);
        reinjectIfUploaded();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        MaterialListHudRenderer.refreshHud(); // 更新HUD
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        MaterialListHudRenderer.refreshHud(); // 鏇存柊HUD
//#endif
        initGui();
    }

    private void restoreOriginalItem(MaterialRow target) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        String pathKey = filePath.toString();
//#endif
        // 精确物品：从精确替换映射移除，随后重建行恢复原始精确堆叠（含组件）
        if (target.exact) {
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            String pathKey = filePath.toString();
//#endif
            Map<String, String> exactFileRepl = exactReplacements.get(pathKey);
            if (exactFileRepl != null) {
                exactFileRepl.remove(target.name);
                if (exactFileRepl.isEmpty()) {
                    exactReplacements.remove(pathKey);
                }
            }
            savePersistence();
            rawMaterials.remove(pathKey);
            rawMaterialRedundancy.remove(pathKey);
            loadMaterials(); // 依据映射重建全部行，精确行恢复原状
            reinjectIfUploaded();
            MaterialListHudRenderer.refreshHud(); // 更新HUD
            replacingRow = -1;
            return;
        }
        for (int i = 0; i < rows.size(); i++) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 通过 originalItem 匹配：替换后 target.item 是旧物品，rows[i].item 是新物品，不能直接 == 比较
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 閫氳繃 originalItem 鍖归厤锛氭浛鎹㈠悗 target.item 鏄棫鐗╁搧锛宺ows[i].item 鏄柊鐗╁搧锛屼笉鑳界洿鎺?== 姣旇緝
//#endif
            if (!rows.get(i).exact && rows.get(i).originalItem == target.originalItem) {
                MaterialRow old = rows.get(i);
                Item orig = old.originalItem;
                rows.set(i, new MaterialRow(
                        new ItemStack(orig, old.totalCount),
                        orig.getName().getString(),
                        old.totalCount,
                        orig,
                        orig,
                        old.missingCount
                ));
                break;
            }
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 从持久化中移除替换
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        // 浠庢寔涔呭寲涓Щ闄ゆ浛鎹?
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        String pathKey = filePath.toString();
//#endif
        Map<String, String> fileReplacements = replacements.get(pathKey);
        if (fileReplacements != null) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            Identifier origId = Registries.ITEM.getId(target.originalItem);
//#elseif MC == 11701
//$$            Identifier origId = Registry.ITEM.getId(target.originalItem);
//#endif
            if (origId != null) {
                fileReplacements.remove(origId.toString());
                if (fileReplacements.isEmpty()) {
                    replacements.remove(pathKey);
                }
            }
        }
        savePersistence();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 清除缓存的原材料分析数据，确保下次打开原材料列表时重新分析
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 娓呴櫎缂撳瓨鐨勫師鏉愭枡鍒嗘瀽鏁版嵁锛岀‘淇濅笅娆℃墦寮€鍘熸潗鏂欏垪琛ㄦ椂閲嶆柊鍒嗘瀽
//#endif
        rawMaterials.remove(pathKey);
        rawMaterialRedundancy.remove(pathKey);
        reinjectIfUploaded();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        MaterialListHudRenderer.refreshHud(); // 更新HUD
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        MaterialListHudRenderer.refreshHud(); // 鏇存柊HUD
//#endif
        replacingRow = -1;
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ==================== 导出 ====================
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ==================== 瀵煎嚭 ====================
//#endif

    private void exportMaterials(String format) {
        try {
            Path mcRoot = MinecraftClient.getInstance().runDirectory.toPath().resolve("litematlist");
            Path dir;
            // 判断是否来自项目文件夹
            if (this.filePath != null && this.filePath.getParent() != null && this.filePath.getParent().getParent() != null) {
                Path grandParent = this.filePath.getParent().getParent();
                if ("project".equals(grandParent.getFileName().toString())) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    // 项目文件夹导出: .minecraft/litematlist/<project>/
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    // 椤圭洰鏂囦欢澶瑰鍑? .minecraft/litematlist/<project>/
//#endif
                    String projectName = this.filePath.getParent().getFileName().toString();
                    dir = mcRoot.resolve(projectName);
                } else {
                    dir = mcRoot;
                }
            } else {
                dir = mcRoot;
            }
            Files.createDirectories(dir);
            String dateStr = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date());
            String safeName = schematicName.replaceAll("[\\\\/:*?\"<>|]", "_");
            String fileName = safeName + "_" + dateStr + "." + format;
            Path filePath = dir.resolve(fileName);

            List<MaterialListExporter.Row> exportRows = new ArrayList<>();
            for (MaterialRow row : rows) {
                if (isRowIgnored(row)) continue;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                Identifier id = Registries.ITEM.getId(row.item);
//#elseif MC == 11701
//$$                Identifier id = Registry.ITEM.getId(row.item);
//#endif
                String itemId = row.exact ? row.name : (id != null ? id.toString() : "unknown");
                int have = getRowHaveCount(row);
                exportRows.add(new MaterialListExporter.Row(
                        itemId,
                        row.name != null ? row.name : "",
                        row.totalCount,
                        Math.max(0, row.totalCount - have),
                        Math.min(have, row.totalCount)));
            }
            String title = "原理图'" + schematicName + "'材料列表";
            if ("txt".equals(format)) {
                MaterialListExporter.writeTxt(filePath, title, exportRows);
            } else if ("csv".equals(format)) {
                MaterialListExporter.writeCsv(filePath, exportRows);
            } else {
                MaterialListExporter.writeJson(filePath, schematicName, title, exportRows);
            }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            LitematListMod.LOGGER.info("已导出材料列表: {}", filePath);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            LitematListMod.LOGGER.info("Exported material list: {}", filePath);
//#endif

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                String fmt = format.toUpperCase();
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                Text link = Text.literal(fileName)
//#elseif MC == 11701
//$$                Text link = new LiteralText(fileName)
//#endif
                        .styled(style -> style
//#if MC == 12108 || MC == 12110 || MC == 12111
                                .withClickEvent(new net.minecraft.text.ClickEvent.OpenFile(filePath.toString()))
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                                .withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.OPEN_FILE, filePath.toString()))
//#endif
                                .withUnderline(true)
                                .withColor(0x55FFFF));
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                Text msg = Text.literal("§a材料列表已导出为" + fmt + ": ").append(link);
//#elseif MC == 11701
//$$                Text msg = new TranslatableText("litematlist.message.exported", fmt).append(link);
//#elseif MC == 11904 || MC == 12001
//$$                Text msg = Text.translatable("litematlist.message.exported", fmt).append(link);
//#endif
//#if MC == 11701 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                client.player.sendMessage(msg, false);
//#elseif MC == 11904 || MC == 12001
//$$                client.player.sendMessage(msg);
//#endif
            }
        } catch (Exception e) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            LitematListMod.LOGGER.error("导出失败", e);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            LitematListMod.LOGGER.error("Export failed", e);
//#endif
        }
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // ==================== 渲染 ====================
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    // ==================== 娓叉煋 ====================
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

        if (rows.isEmpty()) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawCenteredTextWithShadow(
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    this.textRenderer, I18n.tr("litematlist.error.no_data"),
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    this.width / 2, this.height / 2 - 10, 0xFFFFFFFF);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            super.render(drawContext, mouseX, mouseY, partialTicks);
//#endif
//#if MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, I18n.tr("litematlist.error.no_data"), this.width / 2 - this.textRenderer.getWidth(I18n.tr("litematlist.error.no_data")) / 2, this.height / 2 - 10, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$            super.render(matrices, mouseX, mouseY, partialTicks);
//#endif
            return;
        }

        List<MaterialRow> displayRows = getSortedRows();

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        int listRight = this.width - 8; // 背景框覆盖按钮区域
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        int listRight = this.width - 8; // 鑳屾櫙妗嗚鐩栨寜閽尯鍩?
//#endif
        int listBottom = LIST_TOP + visibleRows * ROW_HEIGHT;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // ===== 第1层：背景（在按钮之前绘制，避免盖住按钮） =====
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // ===== 绗?灞傦細鑳屾櫙锛堝湪鎸夐挳涔嬪墠缁樺埗锛岄伩鍏嶇洊浣忔寜閽級 =====
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(8, LIST_TOP - 2, listRight, listBottom + 2, 0x40000000);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, 8, LIST_TOP - 2, listRight, listBottom + 2, 0x40000000);
//#endif

        hoveredRow = -1;
        hoveredColumn = -1;
        int maxIdx = Math.min(displayRows.size(), scrollOffset + visibleRows);

        for (int i = scrollOffset; i < maxIdx; i++) {
            MaterialRow row = displayRows.get(i);
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;
            boolean isReplacing = (replacingRow == i);

            if (isReplacing) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.fill(10, rowY, listRight, rowY + ROW_HEIGHT, 0x60FFFF00);
//#elseif MC == 11701 || MC == 11904
//$$                DrawableHelper.fill(matrices, 10, rowY, listRight, rowY + ROW_HEIGHT, 0x60FFFF00);
//#endif
            } else if (mouseX >= 10 && mouseX <= listRight
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                hoveredRow = i;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.fill(10, rowY, listRight, rowY + ROW_HEIGHT, 0x40FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$                DrawableHelper.fill(matrices, 10, rowY, listRight, rowY + ROW_HEIGHT, 0x40FFFFFF);
//#endif

                int totalW = this.textRenderer.getWidth(String.valueOf(row.totalCount));
                int haveW = this.textRenderer.getWidth(String.valueOf(getRowHaveCount(row)));
                int missW = this.textRenderer.getWidth(String.valueOf(Math.max(0, row.totalCount - getRowHaveCount(row))));

                if (mouseX >= colTotalX - 10 && mouseX <= colTotalX + totalW + 10) {
                    hoveredColumn = 0;
                } else if (mouseX >= colHaveX - 10 && mouseX <= colHaveX + haveW + 10) {
                    hoveredColumn = 1;
                } else if (mouseX >= colMissX - 10 && mouseX <= colMissX + missW + 10) {
                    hoveredColumn = 2;
                }
            } else if ((i - scrollOffset) % 2 == 0) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.fill(10, rowY, listRight, rowY + ROW_HEIGHT, 0x10FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$                DrawableHelper.fill(matrices, 10, rowY, listRight, rowY + ROW_HEIGHT, 0x10FFFFFF);
//#endif
            }
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 表头背景
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 琛ㄥご鑳屾櫙
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(8, HEADER_Y, listRight, HEADER_Y + HEADER_HEIGHT, 0x60000000);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, 8, HEADER_Y, listRight, HEADER_Y + HEADER_HEIGHT, 0x60000000);
//#endif

        int sortColX = switch (sortMode) {
            case TOTAL_DESC, TOTAL_ASC -> colTotalX;
            case HAVE_DESC, HAVE_ASC -> colHaveX;
            case MISS_DESC, MISS_ASC -> colMissX;
            default -> -1;
        };
        if (sortColX > 0) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(sortColX - 20, HEADER_Y, sortColX + 30, HEADER_Y + HEADER_HEIGHT, 0x40FFFF88);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, sortColX - 20, HEADER_Y, sortColX + 30, HEADER_Y + HEADER_HEIGHT, 0x40FFFF88);
//#endif
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // ===== 第2层：按钮（在背景之上） =====
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // ===== 绗?灞傦細鎸夐挳锛堝湪鑳屾櫙涔嬩笂锛?=====
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        super.render(drawContext, mouseX, mouseY, partialTicks);
//#elseif MC == 11701 || MC == 11904
//$$        super.render(matrices, mouseX, mouseY, partialTicks);
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 自定义渲染隐藏按钮文字（开=绿色，关=红色）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鑷畾涔夋覆鏌撻殣钘忔寜閽枃瀛楋紙寮€=缁胯壊锛屽叧=绾㈣壊锛?
//#endif
        if (!hideBtnLabel.isEmpty()) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            int colonIdx = hideBtnLabel.indexOf("：");
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            int colonIdx = hideBtnLabel.indexOf(":");
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (colonIdx < 0) colonIdx = hideBtnLabel.indexOf(":");
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            if (colonIdx < 0) colonIdx = hideBtnLabel.indexOf("：");
//#endif
            if (colonIdx > 0) {
                String prefix = hideBtnLabel.substring(0, colonIdx + 1);
                String status = hideBtnLabel.substring(colonIdx + 1);
                int prefixW = this.textRenderer.getWidth(prefix);
                int textX = hideBtnX + 4;
                int textY = hideBtnY + 5;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.drawTextWithShadow(this.textRenderer, prefix, textX, textY, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$                this.textRenderer.drawWithShadow(matrices, prefix, textX, textY, 0xFFFFFFFF);
//#endif
                int statusColor = hideNoMissing ? 0xFF55FF55 : 0xFFFF5555;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.drawTextWithShadow(this.textRenderer, status, textX + prefixW, textY, statusColor);
//#elseif MC == 11701 || MC == 11904
//$$                this.textRenderer.drawWithShadow(matrices, status, textX + prefixW, textY, statusColor);
//#endif
            }
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 自定义渲染标记按钮文字（开=绿色，关=红色）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鑷畾涔夋覆鏌撴爣璁版寜閽枃瀛楋紙寮€=缁胯壊锛屽叧=绾㈣壊锛?
//#endif
        if (!pinBtnLabel.isEmpty()) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            int colonIdx = pinBtnLabel.indexOf("：");
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            int colonIdx = pinBtnLabel.indexOf(":");
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (colonIdx < 0) colonIdx = pinBtnLabel.indexOf(":");
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            if (colonIdx < 0) colonIdx = pinBtnLabel.indexOf("：");
//#endif
            if (colonIdx > 0) {
                String prefix = pinBtnLabel.substring(0, colonIdx + 1);
                String status = pinBtnLabel.substring(colonIdx + 1);
                int prefixW = this.textRenderer.getWidth(prefix);
                int textX = pinBtnX + 4;
                int textY = pinBtnY + 5;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.drawTextWithShadow(this.textRenderer, prefix, textX, textY, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$                this.textRenderer.drawWithShadow(matrices, prefix, textX, textY, 0xFFFFFFFF);
//#endif
                int statusColor = allowPin ? 0xFF55FF55 : 0xFFFF5555;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.drawTextWithShadow(this.textRenderer, status, textX + prefixW, textY, statusColor);
//#elseif MC == 11701 || MC == 11904
//$$                this.textRenderer.drawWithShadow(matrices, status, textX + prefixW, textY, statusColor);
//#endif
            }
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // ===== 第3层：文字和物品图标（在最上层） =====
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // ===== 绗?灞傦細鏂囧瓧鍜岀墿鍝佸浘鏍囷紙鍦ㄦ渶涓婂眰锛?=====
//#endif
        // 悬停精确物品时待绘制的预览（在文字层之后绘制，保证在最上层）
        ItemStack hoveredExactStack = null;
        String hoveredFullName = null;
        String hoveredItemId = null; // 悬停普通物品图标时显示的物品ID
        for (int i = scrollOffset; i < maxIdx; i++) {
            MaterialRow row = displayRows.get(i);
            int rowY = LIST_TOP + (i - scrollOffset) * ROW_HEIGHT;

            int iconX = 15;
            int nameX = 35;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 标记材料模式：先画标记图标，再偏移物品图标和文字（精确物品按精确ID一并绘制）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 鏍囪鏉愭枡妯″紡锛氬厛鐢绘爣璁板浘鏍囷紝鍐嶅亸绉荤墿鍝佸浘鏍囧拰鏂囧瓧
//#endif
            if (allowPin) {
                boolean isPinned = isRowPinned(row);
                boolean pinHovered = mouseX >= 15 && mouseX <= 31 && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
                Identifier pinTexture;
                if (isPinned) {
                    pinTexture = pinHovered ? PIN_HOVERED_FAVORITE : PIN_FAVORITE;
                } else {
                    pinTexture = pinHovered ? PIN_HOVERED_EMPTY : PIN_EMPTY;
                }
//#if MC == 12108 || MC == 12110 || MC == 12111
                drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, pinTexture, 15, rowY + 2, 0.0f, 0.0f, 16, 16, 16, 16);
//#elseif MC == 11701 || MC == 11904
//$$                RenderSystem.setShaderTexture(0, pinTexture); DrawableHelper.drawTexture(matrices, 15, rowY + 2, 0.0f, 0.0f, 16, 16, 16, 16);
//#elseif MC == 12001
//$$                drawContext.drawTexture(pinTexture, 15, rowY + 2, 0.0f, 0.0f, 16, 16, 16, 16);
//#elseif MC == 12101
//$$                drawContext.drawTexture( pinTexture, 15, rowY + 2, 0.0f, 0.0f, 16, 16, 16, 16);
//#elseif MC == 12104
//$$                drawContext.drawTexture(RenderLayer::getGuiTextured, pinTexture, 15, rowY + 2, 0.0f, 0.0f, 16, 16, 16, 16);
//#endif
                iconX = 33;
                nameX = 53;
            }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawItem(row.itemStack, iconX, rowY + 2);
//#elseif MC == 11701
//$$            MinecraftClient.getInstance().getItemRenderer().renderInGui(row.itemStack, iconX, rowY + 2);
//#elseif MC == 11904
//$$            MinecraftClient.getInstance().getItemRenderer().renderInGui(matrices, row.itemStack, iconX, rowY + 2);
//#endif
            // 精确物品使用带样式文本渲染（自定义名自动斜体、稀有度颜色，与游戏内物品名一致）；过长时退化为截断纯文本
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            Text nameText = row.displayText != null ? row.displayText : Text.literal(row.name);
//#elseif MC == 11701
//$$            Text nameText = row.displayText != null ? row.displayText : new LiteralText(row.name);
//#endif
            if (this.textRenderer.getWidth(nameText) > MAX_NAME_DISPLAY_WIDTH) {
                String truncated = truncateNameByWidth(nameText.getString(), MAX_NAME_DISPLAY_WIDTH);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.drawTextWithShadow(this.textRenderer, truncated, nameX, rowY + 5, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$                this.textRenderer.drawWithShadow(matrices, truncated, nameX, rowY + 5, 0xFFFFFFFF);
//#endif
            } else {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.drawTextWithShadow(this.textRenderer, nameText, nameX, rowY + 5, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$                this.textRenderer.drawWithShadow(matrices, nameText, nameX, rowY + 5, 0xFFFFFFFF);
//#endif
            }
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTextWithShadow(this.textRenderer, String.valueOf(row.totalCount), colTotalX, rowY + 5, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, String.valueOf(row.totalCount), colTotalX, rowY + 5, 0xFFFFFFFF);
//#endif

            int haveCount = getRowHaveCount(row);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTextWithShadow(this.textRenderer, String.valueOf(haveCount), colHaveX, rowY + 5, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, String.valueOf(haveCount), colHaveX, rowY + 5, 0xFFFFFFFF);
//#endif

            int missCount = Math.max(0, row.totalCount - haveCount);
            int missColor = missCount == 0 ? 0xFF55FF55
                          : (haveCount >= missCount ? 0xFFFFAA00 : 0xFFFF5555);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTextWithShadow(this.textRenderer, String.valueOf(missCount), colMissX, rowY + 5, missColor);
//#elseif MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, String.valueOf(missCount), colMissX, rowY + 5, missColor);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 12110 || MC == 12111

//#endif
            // 精确物品：悬停图标时预览物品（含组件悬浮提示）
            if (row.exact && mouseX >= iconX && mouseX < iconX + 16 && mouseY >= rowY + 2 && mouseY < rowY + 18) {
                hoveredExactStack = row.itemStack;
            }
            // 普通物品：悬停图标显示对应物品ID（精确物品保留原有物品预览提示）
            if (!row.exact && mouseX >= iconX && mouseX < iconX + 16 && mouseY >= rowY + 2 && mouseY < rowY + 18) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                hoveredItemId = Registries.ITEM.getId(row.item).toString();
//#elseif MC == 11701
//$$                hoveredItemId = Registry.ITEM.getId(row.item).toString();
//#endif
            }
            // 精确物品悬停名称显示完整精确物品ID（已被替换的精确物品不再显示原ID）；普通物品仅名称被截断时显示完整名称
            // 名称悬停宽度取实际显示宽度（截断时为 MAX_NAME_DISPLAY_WIDTH），
            // 避免悬停热点延伸到数量列而与数量提示叠加
            int shownNameWidth = Math.min(this.textRenderer.getWidth(nameText), MAX_NAME_DISPLAY_WIDTH);
            boolean nameHovered = mouseX >= nameX && mouseX < nameX + shownNameWidth
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            boolean exactIdSuppressed = row.exact && isExactReplaced(row.name);
            if (nameHovered && ((row.exact && !exactIdSuppressed)
                    || this.textRenderer.getWidth(nameText) > MAX_NAME_DISPLAY_WIDTH)) {
                hoveredFullName = row.name;
            }
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 表头文字
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 琛ㄥご鏂囧瓧
//#endif
        int headerNameX = allowPin ? 53 : 35;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 搜索图标
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鎼滅储鍥炬爣
//#endif
        int searchIconX = allowPin ? 33 : 15;
//#if MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, SEARCH_ICON, searchIconX, HEADER_Y + 1, 0.0f, 0.0f, 14, 14, 14, 14);
//#elseif MC == 11701 || MC == 11904
//$$        RenderSystem.setShaderTexture(0, SEARCH_ICON); DrawableHelper.drawTexture(matrices, searchIconX, HEADER_Y + 1, 0.0f, 0.0f, 14, 14, 14, 14);
//#elseif MC == 12001
//$$        drawContext.drawTexture(SEARCH_ICON, searchIconX, HEADER_Y + 1, 0.0f, 0.0f, 14, 14, 14, 14);
//#elseif MC == 12101
//$$        drawContext.drawTexture( SEARCH_ICON, searchIconX, HEADER_Y + 1, 0.0f, 0.0f, 14, 14, 14, 14);
//#elseif MC == 12104
//$$        drawContext.drawTexture(RenderLayer::getGuiTextured, SEARCH_ICON, searchIconX, HEADER_Y + 1, 0.0f, 0.0f, 14, 14, 14, 14);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 搜索框打开时不绘制"物品"表头文字（搜索框覆盖该区域，避免与输入文字重叠冲突）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鍏堢敾"鐗╁搧"鏂囧瓧锛堟悳绱㈡鎵撳紑鏃惰鐩栧湪涓婇潰锛?
//#endif
        if (!searchActive) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTextWithShadow(this.textRenderer, I18n.tr("litematlist.header.item"), headerNameX, HEADER_Y + 3, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, I18n.tr("litematlist.header.item"), headerNameX, HEADER_Y + 3, 0xFFFFFFFF);
//#endif
        }
        if (searchActive && searchField != null) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(searchIconX + 18 - 2, HEADER_Y - 1, searchIconX + 18 + 82, HEADER_Y + 15, 0xFF000000);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, searchIconX + 18 - 2, HEADER_Y - 1, searchIconX + 18 + 82, HEADER_Y + 15, 0xFF000000);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(searchIconX + 18, HEADER_Y, searchIconX + 18 + 80, HEADER_Y + 14, 0xFF222222);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, searchIconX + 18, HEADER_Y, searchIconX + 18 + 80, HEADER_Y + 14, 0xFF222222);
//#endif
            String st = searchField.getText();
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawText(this.textRenderer, st, searchIconX + 21, HEADER_Y + 3, 0xFFFFFFFF, false);
//#elseif MC == 11701 || MC == 11904
//$$            this.textRenderer.draw(matrices, st, searchIconX + 21, HEADER_Y + 3, 0xFFFFFFFF);
//#endif
            if (searchField.isFocused() && System.currentTimeMillis() % 1000 < 500) {
                int cp = searchField.getCursor();
                String bc = st.substring(0, Math.min(cp, st.length()));
                int cx = searchIconX + 21 + this.textRenderer.getWidth(bc);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.fill(cx, HEADER_Y + 2, cx + 1, HEADER_Y + 12, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$                DrawableHelper.fill(matrices, cx, HEADER_Y + 2, cx + 1, HEADER_Y + 12, 0xFFFFFFFF);
//#endif
            }
        }
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawTextWithShadow(this.textRenderer,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                I18n.tr("litematlist.header.total") + (sortMode == SortMode.TOTAL_DESC || sortMode == SortMode.TOTAL_ASC ? sortMode.arrow : ""),
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                colTotalX, HEADER_Y + 3, 0xFFFFFFFF);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawTextWithShadow(this.textRenderer,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                I18n.tr("litematlist.header.have") + (sortMode == SortMode.HAVE_DESC || sortMode == SortMode.HAVE_ASC ? sortMode.arrow : ""),
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                colHaveX, HEADER_Y + 3, 0xFFFFFFFF);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawTextWithShadow(this.textRenderer,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                I18n.tr("litematlist.header.missing") + (sortMode == SortMode.MISS_DESC || sortMode == SortMode.MISS_ASC ? sortMode.arrow : ""),
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                colMissX, HEADER_Y + 3, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$        this.textRenderer.drawWithShadow(matrices, I18n.tr("litematlist.header.total") + (sortMode == SortMode.TOTAL_DESC || sortMode == SortMode.TOTAL_ASC ? sortMode.arrow : ""), colTotalX, HEADER_Y + 3, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$        this.textRenderer.drawWithShadow(matrices, I18n.tr("litematlist.header.have") + (sortMode == SortMode.HAVE_DESC || sortMode == SortMode.HAVE_ASC ? sortMode.arrow : ""), colHaveX, HEADER_Y + 3, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$        this.textRenderer.drawWithShadow(matrices, I18n.tr("litematlist.header.missing") + (sortMode == SortMode.MISS_DESC || sortMode == SortMode.MISS_ASC ? sortMode.arrow : ""), colMissX, HEADER_Y + 3, 0xFFFFFFFF);
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 仅在悬停数值列时显示量级 tooltip
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 浠呭湪鎮仠鏁板€煎垪鏃舵樉绀洪噺绾?tooltip
//#endif
        if (hoveredRow >= 0 && hoveredColumn >= 0 && hoveredRow < displayRows.size()) {
            MaterialRow hovered = displayRows.get(hoveredRow);
            int haveCount = getRowHaveCount(hovered);
            int missCount = Math.max(0, hovered.totalCount - haveCount);

            int tooltipValue = switch (hoveredColumn) {
                case 0 -> hovered.totalCount;
                case 1 -> haveCount;
                case 2 -> missCount;
                default -> hovered.totalCount;
            };

            String tooltip = formatCountTooltip(tooltipValue);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTooltip(this.textRenderer, Text.literal(tooltip), mouseX, mouseY);
//#elseif MC == 11701
//$$            this.renderTooltip(matrices, new LiteralText(tooltip), mouseX, mouseY);
//#elseif MC == 11904
//$$            this.renderTooltip(matrices, Text.literal(tooltip), mouseX, mouseY);
//#endif
        }
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 12110 || MC == 12111

//#endif
        // 精确物品图标悬停预览（物品+组件悬浮提示）
        if (hoveredExactStack != null) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawItemTooltip(this.textRenderer, hoveredExactStack, mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$            this.renderTooltip(matrices, hoveredExactStack, mouseX, mouseY);
//#endif
        }
        // 截断名称悬停显示完整精确物品ID（过长时按屏幕边界自动换行）
        if (hoveredFullName != null) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawWrappedTooltip(drawContext, hoveredFullName, mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$            drawWrappedTooltip(matrices, hoveredFullName, mouseX, mouseY);
//#endif
        }
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$
//#endif
        // 悬停普通物品图标显示对应物品ID
        if (hoveredItemId != null) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTooltip(this.textRenderer, Text.literal(hoveredItemId), mouseX, mouseY);
//#elseif MC == 11701
//$$            this.renderTooltip(matrices, new LiteralText(hoveredItemId), mouseX, mouseY);
//#elseif MC == 11904
//$$            this.renderTooltip(matrices, Text.literal(hoveredItemId), mouseX, mouseY);
//#endif
        }

        if (displayRows.size() > visibleRows) {
            String scrollInfo = (scrollOffset + 1) + "-" +
                    Math.min(scrollOffset + visibleRows, displayRows.size()) +
                    " / " + displayRows.size();
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawCenteredTextWithShadow(this.textRenderer,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    scrollInfo, this.width / 2, listBottom + 5, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, scrollInfo, this.width / 2 - this.textRenderer.getWidth(scrollInfo) / 2, listBottom + 5, 0xFFFFFFFF);
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 滚动条
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 婊氬姩鏉?
//#endif
            int scrollbarX = this.width - 6;
            int scrollbarWidth = 3;
            int listHeight = listBottom - LIST_TOP;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 轨道
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 杞ㄩ亾
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(scrollbarX, LIST_TOP, scrollbarX + scrollbarWidth, listBottom, 0x30FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, scrollbarX, LIST_TOP, scrollbarX + scrollbarWidth, listBottom, 0x30FFFFFF);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 滑块
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 婊戝潡
//#endif
            float ratio = (float) visibleRows / displayRows.size();
            int thumbHeight = Math.max(6, (int)(listHeight * ratio));
            int maxScroll = Math.max(1, displayRows.size() - visibleRows);
            int thumbY = LIST_TOP + (int)((listHeight - thumbHeight) * (float) scrollOffset / maxScroll);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, 0x80FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, 0x80FFFFFF);
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

//#endif
//#if MC == 12110 || MC == 12111
        int mouseX = (int) click.x();
//#endif
//#if MC == 12110 || MC == 12111
        int mouseY = (int) click.y();
//#endif
//#if MC == 12110 || MC == 12111
        int button = click.button();
//#endif
//#if MC == 12110 || MC == 12111

//#endif
//#if MC == 12110 || MC == 12111
        // 拖拽事件处理（isDrag=true）
//#endif
//#if MC == 12110 || MC == 12111
        if (isDrag) {
//#endif
//#if MC == 12110 || MC == 12111
            if (button == 0) {
//#endif
//#if MC == 12110 || MC == 12111
                // 滚动条拖拽
//#endif
//#if MC == 12110 || MC == 12111
                if (scrollbarDragging) {
//#endif
//#if MC == 12110 || MC == 12111
                    List<MaterialRow> displayRows = getSortedRows();
//#endif
//#if MC == 12110 || MC == 12111
                    int listBottom = LIST_TOP + visibleRows * ROW_HEIGHT;
//#endif
//#if MC == 12110 || MC == 12111
                    int trackHeight = listBottom - LIST_TOP;
//#endif
//#if MC == 12110 || MC == 12111
                    int maxScroll = Math.max(1, displayRows.size() - visibleRows);
//#endif
//#if MC == 12110 || MC == 12111
                    scrollOffset = Math.max(0, Math.min(maxScroll,
//#endif
//#if MC == 12110 || MC == 12111
                            (int) ((float) (mouseY - LIST_TOP) / trackHeight * maxScroll)));
//#endif
//#if MC == 12110 || MC == 12111
                    initGui();
//#endif
//#if MC == 12110 || MC == 12111
                    return true;
//#endif
//#if MC == 12110 || MC == 12111
                }
//#endif
//#if MC == 12110 || MC == 12111
                // 行拖拽重排
//#endif
//#if MC == 12110 || MC == 12111
                if (draggedRow >= 0 && Configs.Generic.ALLOW_MANUAL_REORDER.getBooleanValue()) {
//#endif
//#if MC == 12110 || MC == 12111
                    List<MaterialRow> displayRows = getSortedRows();
//#endif
//#if MC == 12110 || MC == 12111
                    int currentRow = getRowAtY(mouseY, displayRows);
//#endif
//#if MC == 12110 || MC == 12111
                    if (currentRow >= 0 && currentRow != draggedRow && Math.abs(mouseY - dragStartY) > 5) {
//#endif
//#if MC == 12110 || MC == 12111
                        // 不允许跨置顶边界拖拽
//#endif
//#if MC == 12110 || MC == 12111
                        if (allowPin) {
//#endif
//#if MC == 12110 || MC == 12111
                            MaterialRow dragged = displayRows.get(draggedRow);
//#endif
//#if MC == 12110 || MC == 12111
                            MaterialRow target = displayRows.get(currentRow);
//#endif
//#if MC == 12110 || MC == 12111
                            boolean draggedPinned = isRowPinned(dragged);
//#endif
//#if MC == 12110 || MC == 12111
                            boolean targetPinned = isRowPinned(target);
//#endif
//#if MC == 12110 || MC == 12111
                            if (draggedPinned != targetPinned) {
//#endif
//#if MC == 12110 || MC == 12111
                                return true;
//#endif
//#if MC == 12110 || MC == 12111
                            }
//#endif
//#if MC == 12110 || MC == 12111
                        }
//#endif
//#if MC == 12110 || MC == 12111
                        isDragging = true;
//#endif
//#if MC == 12110 || MC == 12111
                        // 交换 rows 中的元素
//#endif
//#if MC == 12110 || MC == 12111
                        MaterialRow draggedItem = displayRows.get(draggedRow);
//#endif
//#if MC == 12110 || MC == 12111
                        MaterialRow targetItem = displayRows.get(currentRow);
//#endif
//#if MC == 12110 || MC == 12111
                        int actualDraggedIdx = rows.indexOf(draggedItem);
//#endif
//#if MC == 12110 || MC == 12111
                        int actualTargetIdx = rows.indexOf(targetItem);
//#endif
//#if MC == 12110 || MC == 12111
                        if (actualDraggedIdx >= 0 && actualTargetIdx >= 0) {
//#endif
//#if MC == 12110 || MC == 12111
                            Collections.swap(rows, actualDraggedIdx, actualTargetIdx);
//#endif
//#if MC == 12110 || MC == 12111
                            sortMode = SortMode.NONE;
//#endif
//#if MC == 12110 || MC == 12111
                        }
//#endif
//#if MC == 12110 || MC == 12111
                        draggedRow = currentRow;
//#endif
//#if MC == 12110 || MC == 12111
                        dragStartY = mouseY;
//#endif
//#if MC == 12110 || MC == 12111
                    }
//#endif
//#if MC == 12110 || MC == 12111
                    return true;
//#endif
//#if MC == 12110 || MC == 12111
                }
//#endif
//#if MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 12110 || MC == 12111
            return false;
//#endif
//#if MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$    public boolean onMouseClicked(int mouseX, int mouseY, int button) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (super.mouseClicked(mouseX, mouseY, mouseButton)) return true;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        if (super.onMouseClicked(mouseX, mouseY, button)) return true;
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 非拖拽事件（isDrag=false）
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        mouseX = (int) mouseX;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        mouseY = (int) mouseY;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        int button = mouseButton;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        // 鎷栨嫿浜嬩欢澶勭悊锛坕sDrag=true锛?
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
        if (button == 0) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 搜索图标点击
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 鎼滅储鍥炬爣鐐瑰嚮
//#endif
            int searchIconX = allowPin ? 33 : 15;
            if (mouseX >= searchIconX && mouseX <= searchIconX + 16
                    && mouseY >= HEADER_Y && mouseY <= HEADER_Y + HEADER_HEIGHT) {
                searchActive = !searchActive;
                if (searchActive && searchField != null) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    searchField.setFocused(true);
//#elseif MC == 11701
//$$                    this.setFocused(searchField);
//#endif
                }
                if (!searchActive) {
                    searchText = "";
                    if (searchField != null) searchField.setText("");
                }
                initGui();
                return true;
            }
            if (mouseY >= HEADER_Y && mouseY <= HEADER_Y + HEADER_HEIGHT) {
                handleHeaderClick(mouseX, mouseY);
                return true;
            }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 标记材料点击
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 鏍囪鏉愭枡鐐瑰嚮
//#endif
            if (allowPin) {
                List<MaterialRow> displayRows = getSortedRows();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                int pinRow = getRowAtY(mouseY, displayRows);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                int pinRow = getRowAtY((int)mouseY, displayRows);
//#endif
                if (pinRow >= 0 && mouseX >= 15 && mouseX <= 31) {
                    MaterialRow row = displayRows.get(pinRow);
                    // 精确物品按精确ID切换置顶
                    if (row.exact) {
                        if (currentExactPinned.contains(row.name)) {
                            currentExactPinned.remove(row.name);
                        } else {
                            currentExactPinned.add(row.name);
                        }
                    } else if (currentPinned.contains(row.originalItem)) {
                        currentPinned.remove(row.originalItem);
                    } else {
                        currentPinned.add(row.originalItem);
                    }
                    savePersistence();
                    initGui();
                    return true;
                }
            }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 拖拽检测（仅在启用手动重排时）
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            // 鎷栨嫿妫€娴嬶紙浠呭湪鍚敤鎵嬪姩閲嶆帓鏃讹級
//#endif
            if (Configs.Generic.ALLOW_MANUAL_REORDER.getBooleanValue()) {
                List<MaterialRow> displayRows = getSortedRows();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                int row = getRowAtY(mouseY, displayRows);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                int row = getRowAtY((int)mouseY, displayRows);
//#endif
                if (row >= 0) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    // 排除按钮区域
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    // 鎺掗櫎鎸夐挳鍖哄煙
//#endif
                    if (mouseX >= 10 && mouseX < this.width - 210) {
                        draggedRow = row;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        dragStartY = mouseY;
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                        dragStartY = (int)mouseY;
//#endif
                        isDragging = false;
                        return true;
                    }
                }
            }
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 滚动条点击
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 婊氬姩鏉＄偣鍑?
//#endif
        List<MaterialRow> displayRows = getSortedRows();
        int listBottom = LIST_TOP + visibleRows * ROW_HEIGHT;
        int scrollbarX = this.width - 6;
        int scrollbarWidth = 3;
        if (displayRows.size() > visibleRows && mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth
                && mouseY >= LIST_TOP && mouseY <= listBottom) {
            int trackHeight = listBottom - LIST_TOP;
            int maxScroll = Math.max(1, displayRows.size() - visibleRows);
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
//#endif
//#if MC == 12110 || MC == 12111
        // 拖拽逻辑已移至 onMouseClicked(click, isDrag=true) 中处理
//#endif
//#if MC == 12110 || MC == 12111
        return super.mouseDragged(click, deltaX, deltaY);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (mouseButton == 0) {
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        if (button == 0) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            // 滚动条拖拽
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            if (scrollbarDragging) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                List<MaterialRow> displayRows = getSortedRows();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                int listBottom = LIST_TOP + visibleRows * ROW_HEIGHT;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                int trackHeight = listBottom - LIST_TOP;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                int maxScroll = Math.max(1, displayRows.size() - visibleRows);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                scrollOffset = Math.max(0, Math.min(maxScroll,
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        (int) ((float) ((int)mouseY - LIST_TOP) / trackHeight * maxScroll)));
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$                        (int) ((float) ((int) mouseY - LIST_TOP) / trackHeight * maxScroll)));
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                initGui();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                return true;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            // 行拖拽重排
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            if (draggedRow >= 0 && Configs.Generic.ALLOW_MANUAL_REORDER.getBooleanValue()) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                List<MaterialRow> displayRows = getSortedRows();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                int currentRow = getRowAtY((int)mouseY, displayRows);
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$                int currentRow = getRowAtY((int) mouseY, displayRows);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                if (currentRow >= 0 && currentRow != draggedRow && Math.abs((int)mouseY - dragStartY) > 5) {
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$                if (currentRow >= 0 && currentRow != draggedRow && Math.abs((int) mouseY - dragStartY) > 5) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                    // 不允许跨置顶边界拖拽
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                    if (allowPin) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                        MaterialRow dragged = displayRows.get(draggedRow);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                        MaterialRow target = displayRows.get(currentRow);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                        boolean draggedPinned = isRowPinned(dragged);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                        boolean targetPinned = isRowPinned(target);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                        if (draggedPinned != targetPinned) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                            return true;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                    isDragging = true;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                    // 交换 rows 中的元素
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                    MaterialRow draggedItem = displayRows.get(draggedRow);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                    MaterialRow targetItem = displayRows.get(currentRow);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                    int actualDraggedIdx = rows.indexOf(draggedItem);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                    int actualTargetIdx = rows.indexOf(targetItem);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                    if (actualDraggedIdx >= 0 && actualTargetIdx >= 0) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                        Collections.swap(rows, actualDraggedIdx, actualTargetIdx);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                        sortMode = SortMode.NONE;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                    draggedRow = currentRow;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    dragStartY = (int)mouseY;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$                    dragStartY = (int) mouseY;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                return true;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
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
        if (draggedRow >= 0) {
            if (isDragging) {
                saveReorderedItems();
                initGui();
            }
            draggedRow = -1;
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

    private int getRowAtY(int mouseY, List<MaterialRow> displayRows) {
        if (mouseY < LIST_TOP) return -1;
        int row = (mouseY - LIST_TOP) / ROW_HEIGHT + scrollOffset;
        if (row >= displayRows.size()) return -1;
        return row;
    }

    private void saveReorderedItems() {
        List<Item> order = new ArrayList<>();
        for (MaterialRow row : rows) {
            // 精确物品行不参与手动重排持久化
            if (!row.exact && !currentIgnored.contains(row.item)) {
                order.add(row.originalItem);
            }
        }
        reorderedItems.put(filePath, order);
    }

    @Override
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount,
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                  double verticalAmount) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
//#endif
        List<MaterialRow> displayRows = getSortedRows();
        if (mouseY >= LIST_TOP && mouseY <= LIST_TOP + visibleRows * ROW_HEIGHT) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            scrollOffset = Math.max(0, scrollOffset - (int) verticalAmount);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            scrollOffset = Math.max(0, scrollOffset - (int) amount);
//#endif
            int maxOffset = Math.max(0, displayRows.size() - visibleRows);
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
            if (searchActive) {
                searchActive = false;
                searchText = "";
                if (searchField != null) searchField.setText("");
                initGui();
                return true;
            }
            MinecraftClient.getInstance().setScreen(parent);
            return true;
        }
        if (searchActive && searchField != null && searchField.isFocused()) {
//#if MC == 12110 || MC == 12111
            if (searchField.keyPressed(keyInput)) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            if (searchField.keyPressed(keyCode, scanCode, modifiers)) {
//#endif
                searchText = searchField.getText();
                scrollOffset = 0; // 输入字符后立刻回到列表最顶端
                initGui();
                return true;
            }
        }
//#if MC == 12110 || MC == 12111
        return super.keyPressed(keyInput);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        return super.keyPressed(keyCode, scanCode, modifiers);
//#endif
    }

    @Override
//#if MC == 12110 || MC == 12111
    public boolean charTyped(CharInput charInput) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$    public boolean charTyped(char chr, int modifiers) {
//#endif
        if (searchActive && searchField != null && searchField.isFocused()) {
//#if MC == 12110 || MC == 12111
            if (searchField.charTyped(charInput)) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            if (searchField.charTyped(chr, modifiers)) {
//#endif
                searchText = searchField.getText();
                scrollOffset = 0; // 输入字符后立刻回到列表最顶端
                initGui();
                return true;
            }
        }
//#if MC == 12110 || MC == 12111
        return super.charTyped(charInput);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        return super.charTyped(chr, modifiers);
//#endif
    }

    private record MaterialRow(ItemStack itemStack, String name, int totalCount, Item item, Item originalItem,
                               int missingCount, boolean exact, Text displayText) {
        /** 普通行便捷构造（exact=false，显示名直接用 name 纯文本） */
        MaterialRow(ItemStack itemStack, String name, int totalCount, Item item, Item originalItem, int missingCount) {
            this(itemStack, name, totalCount, item, originalItem, missingCount, false, null);
        }
    }
}

//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$
//$$
//$$
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif