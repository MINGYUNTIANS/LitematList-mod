package com.litematlist.gui;

import com.litematlist.LitematicReader;
import com.litematlist.LitematListMod;
import com.litematlist.MaterialListImporter;
import com.litematlist.config.Configs;
import com.litematlist.MaterialListInjector;
import net.minecraft.client.MinecraftClient;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.gui.DrawContext;
//#endif
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.registry.Registries;
//#elseif MC == 11701
//$$import net.minecraft.util.registry.Registry;
//#endif

import java.nio.file.Path;
import java.util.*;
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.gui.DrawableHelper;
//#endif
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.util.math.MatrixStack;
//#endif

/**
 * 材料列表 HUD 渲染器 —— 在游戏屏幕上渲染已上传材料列表的前 N 项。
 * 多项性能优化：名称缓存、预计算尺寸、跳过空潜影盒、合并矩阵、增量更新。
 */
public class MaterialListHudRenderer {

    private static final List<HudItem> hudItems = new ArrayList<>();
    private static long lastRefreshTime = 0;
    private static final long REFRESH_COOLDOWN = 200;
    private static int currentPage = 0;

    /** 内部的 HUD 条目（mutable missing 用于增量更新） */
    public static class HudItem {
        public final ItemStack stack;
        public final String name;
        public final int totalCount;
        public int missing;
        public HudItem(ItemStack stack, String name, int totalCount, int missing) {
            this.stack = stack;
            this.name = name;
            this.totalCount = totalCount;
            this.missing = missing;
        }
    }

    public static List<HudItem> getHudItems() {
        return hudItems;
    }

    public static volatile boolean hudDirty = false;
    public static void markDirty() {
        hudDirty = true;
    }

    public static int getCurrentPage() {
        return currentPage;
    }

    public static void nextPage() {
        int maxLines = MaterialDataStore.persistedHudMaxLines;
        int totalPages = hudItems.isEmpty() ? 1 : (hudItems.size() + maxLines - 1) / maxLines;
        currentPage = Math.min(currentPage + 1, totalPages - 1);
        needsFullRefresh = true; // 翻页时强制全量更新
    }

    public static void prevPage() {
        currentPage = Math.max(currentPage - 1, 0);
        needsFullRefresh = true; // 翻页时强制全量更新
    }

    // ==================== 优化 #1：物品名称缓存 ====================
    private static final Map<Item, String> nameCache = new HashMap<>();

    private static String getCachedName(Item item, LitematicReader.MaterialEntry m) {
        return nameCache.computeIfAbsent(item, k ->
            new ItemStack(item).getName().getString());
    }

    public static void clearNameCache() {
        nameCache.clear();
    }

    /** 清空HUD所有数据（切换世界时调用） */
    public static void clearHudItems() {
        hudItems.clear();
        nameCache.clear();
        lastKnownInventory.clear();
        lastMaterialHash = null;
        currentPage = 0;
        cachedTotalW = 0;
        cachedTotalH = 0;
        cachedLineHeight = 0;
        cachedIconSize = 0;
        cachedPageW = 0;
        cachedPageText = "";
        cachedMaxNameW = 0;
        cachedCountMaxW = 0;
        cachedMaxNumW = 0;
        cachedMaxSuffixW = 0;
        cachedFontSize = -1;
        cachedMaxLines = -1;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        cachedBgAlpha = -1;}
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        cachedBgAlpha = -1;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    }
//#endif

    // ==================== 优化 #3：预计算尺寸字段 ====================
    private static int cachedTotalW = 0, cachedTotalH = 0;
    private static int cachedLineHeight = 0, cachedIconSize = 0;
    private static int cachedPageW = 0;
    private static String cachedPageText = "";
    private static int cachedMaxNameW = 0;
    private static int cachedCountMaxW = 0;
    private static int cachedMaxNumW = 0;      // 红字数字部分最大宽度
    private static int cachedMaxSuffixW = 0;   // 盒/组后缀最大宽度
    private static int cachedFontSize = -1, cachedMaxLines = -1, cachedBgAlpha = -1;
    private static int cachedCurrentPage = -1;
    private static float cachedScale = 1.0f;
    private static boolean cachedShowBox = false, cachedShowGroup = false;

    // ==================== 优化 #7：增量更新哈希 ====================
    private static String lastMaterialHash = "";

    // ==================== 优化 #8：分页懒刷新 ====================
    private static boolean needsFullRefresh = false;
    private static int refreshCounter = 0;
    private static final int FULL_REFRESH_INTERVAL = 5; // 每5次刷新同步一次非当前页

    // ==================== 事件驱动缺失数量缓存 ====================
    // 只在背包变化时扫描一次，所有 hudItems 的 missing 直接从缓存读取
    private static Map<Item, Integer> lastKnownInventory = new HashMap<>();
    private static int lastInventoryHash = 0;
    private static int lastLightHash = 0;

    // ==================== 调试计数器 ====================
    private static int totalAttempts = 0;
    private static int totalUpdates = 0;
    private static int totalSkipped = 0;

    private static int computeInventoryHash(Map<Item, Integer> inventory) {
        int hash = 0;
        for (Map.Entry<Item, Integer> entry : inventory.entrySet()) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            int id = Registries.ITEM.getRawId(entry.getKey());
//#elseif MC == 11701
//$$            int id = Registry.ITEM.getRawId(entry.getKey());
//#endif
            hash = hash * 31 + id * 31 + entry.getValue();
        }
        return hash;
    }

    /**
     * 轻量物品栏哈希：仅扫描 36 个原始槽位，不展开潜影盒。
     * 用于更新前的快速判断，避免在物品栏未变化时执行完整扫描。
     */
    private static int computeLightInventoryHash() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return 0;
        int hash = 0;
        for (int i = 0; i < client.player.getInventory().size(); i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            int id = Registries.ITEM.getRawId(stack.getItem());
//#elseif MC == 11701
//$$            int id = Registry.ITEM.getRawId(stack.getItem());
//#endif
            hash = hash * 31 + id * 31 + stack.getCount();
        }
        return hash;
    }

    /**
     * 事件驱动：背包变化时更新所有 hudItems 的 missing 数量。
     * 只在 tick 中由脏标记触发，render() 直接读取 hudItems[i].missing → O(1)。
     * 优化 #1：轻量哈希先判断，未变化时跳过完整扫描。
     * 优化 #2：完整扫描后哈希校验，仅在物品栏真正变化时才刷新。
     */
    public static void updateMissingCounts() {
        if (hudItems.isEmpty()) return;
        long now = System.currentTimeMillis();
        if (now - lastRefreshTime < REFRESH_COOLDOWN) return;
        lastRefreshTime = now;

        totalAttempts++;

        // 优化 #1：轻量哈希（36槽位，不展开潜影盒）快速判断物品栏是否变化
        int lightHash = computeLightInventoryHash();
        if (lightHash == lastLightHash && lastLightHash != 0) {
            totalSkipped++;
            return;
        }
        lastLightHash = lightHash;

        // 物品栏可能变化了，执行完整扫描（含潜影盒展开）
        Map<Item, Integer> inventory = scanInventory();
        int newHash = computeInventoryHash(inventory);

        // 优化 #2：完整哈希校验（含潜影盒），确定物品栏真的变了
        if (newHash == lastInventoryHash && lastInventoryHash != 0) {
            return; // 仅潜影盒内部变化导致轻量哈希不同，但展开后内容相同 → 跳过
        }
        lastInventoryHash = newHash;
        lastKnownInventory = inventory;
        totalUpdates++;

        // 调试日志：每次实际更新都输出
        LitematListMod.LOGGER.info("[HUD] 更新 #{}/{} (跳过 {}), 物品栏哈希={}, 材料数={}, 当前页={}",
                totalUpdates, totalAttempts, totalSkipped, newHash, hudItems.size(), currentPage);

        refreshCounter++;
        int maxLines = MaterialDataStore.persistedHudMaxLines;
        int startIdx = currentPage * maxLines;
        int endIdx = Math.min(startIdx + maxLines, hudItems.size());

        boolean fullRefresh = needsFullRefresh || (refreshCounter % FULL_REFRESH_INTERVAL == 0);
        if (needsFullRefresh) needsFullRefresh = false;

        if (fullRefresh) {
            for (HudItem hi : hudItems) {
                int have = lastKnownInventory.getOrDefault(hi.stack.getItem(), 0);
                hi.missing = Math.max(0, hi.totalCount - have);
            }
        } else {
            for (int i = startIdx; i < endIdx; i++) {
                HudItem hi = hudItems.get(i);
                int have = lastKnownInventory.getOrDefault(hi.stack.getItem(), 0);
                hi.missing = Math.max(0, hi.totalCount - have);
            }
        }
        recomputeCachedDimensions();
    }

    /**
     * 刷新 HUD 数据结构（仅在材料列表结构变化时调用：上传/替换/忽略/取消忽略）。
     * 不扫描背包，使用 lastKnownInventory 计算初始 missing 数量。
     */
    public static void refreshHud() {
        long now = System.currentTimeMillis();
        if (now - lastRefreshTime < REFRESH_COOLDOWN) return;
        lastRefreshTime = now;

        MaterialListScreen.LoadedEntry uploaded = MaterialListScreen.getUploadedEntry();
        if (uploaded == null) {
            hudItems.clear();
            lastMaterialHash = "";
            return;
        }

        String sortName = MaterialDataStore.persistedDetailSortMode;
        boolean hideNoMissing = MaterialDataStore.persistedHideNoMissing;
        Set<Item> ignored = uploaded.filePath() != null
                ? MaterialDataStore.getIgnoredForPath(uploaded.filePath())
                : new HashSet<>();
        Set<Item> pinned = uploaded.filePath() != null
                ? MaterialDataStore.getPinnedForPath(uploaded.filePath())
                : new HashSet<>();

        List<LitematicReader.MaterialEntry> materials;
        if (uploaded.isProject()) {
            materials = getProjectMaterials(uploaded);
        } else {
            materials = getEntryMaterials(uploaded);
        }
        if (materials == null || materials.isEmpty()) {
            hudItems.clear();
            lastMaterialHash = "";
            return;
        }

        // 使用缓存背包数据（由 updateMissingCounts 维护）
        // 首次调用时 lastKnownInventory 可能为空，此时扫描一次
        if (lastKnownInventory.isEmpty()) {
            lastKnownInventory = scanInventory();
        }

        // 计算材料结构哈希（用于增量更新判定）
        String currentHash = computeMaterialHash(materials, ignored, hideNoMissing, sortName, pinned, uploaded.filePath());

        // 结构未变——仅更新 missing（由 updateMissingCounts 保证最新）
        if (!hudItems.isEmpty() && currentHash.equals(lastMaterialHash)) {
            recomputeCachedDimensions();
            return;
        }
        lastMaterialHash = currentHash;

        // 结构变了，全量重建
        hudItems.clear();
        nameCache.clear();

        // 构建条目：先按替换后的物品ID合并（同一物品的多个来源，以及被替换成同一物品的项，
        // 在 HUD 显示层合并为一项、数量相加；材料列表表格界面不做合并），
        // 再使用 lastKnownInventory 计算初始 missing
        Map<Item, Integer> mergedTotals = new LinkedHashMap<>();
        Map<Item, String> mergedNames = new HashMap<>();
        for (LitematicReader.MaterialEntry m : materials) {
            Item item = m.item();
            if (uploaded.filePath() != null) {
                Item replaced = MaterialDataStore.getReplacement(uploaded.filePath(), item);
                if (replaced != item) item = replaced;
            }
            if (item == null || item == Items.AIR) continue;
            if (ignored.contains(item)) continue;
            mergedTotals.merge(item, m.totalCount(), Integer::sum);
            mergedNames.putIfAbsent(item, getCachedName(item, m));
        }
//#if MC == 12101
//$$
//#endif
//#if MC == 12110

//#endif
        List<HudItem> allItems = new ArrayList<>();
        for (Map.Entry<Item, Integer> e : mergedTotals.entrySet()) {
            Item item = e.getKey();
            int total = e.getValue();
            int have = lastKnownInventory.getOrDefault(item, 0);
            int missing = Math.max(0, total - have);
            if (hideNoMissing && missing == 0) continue;
            ItemStack stack = new ItemStack(item);
            if (stack.isEmpty()) continue;
            allItems.add(new HudItem(stack, mergedNames.get(item), total, missing));
        }

        // 排序：置顶优先
        List<HudItem> pinnedItems = new ArrayList<>();
        List<HudItem> normalItems = new ArrayList<>();
        for (HudItem hi : allItems) {
            if (pinned.contains(hi.stack.getItem())) {
                pinnedItems.add(hi);
            } else {
                normalItems.add(hi);
            }
        }

        Comparator<HudItem> cmp = getComparator(sortName);
        if (cmp != null) normalItems.sort(cmp);

        hudItems.addAll(pinnedItems);
        hudItems.addAll(normalItems);

        int maxLines = MaterialDataStore.persistedHudMaxLines;
        int totalPages = hudItems.isEmpty() ? 1 : (hudItems.size() + maxLines - 1) / maxLines;
        currentPage = Math.min(currentPage, totalPages - 1);

        // 优化 #3：预计算尺寸
        recomputeCachedDimensions();
    }

    /**
     * 强制刷新 HUD（忽略冷却），用于「是否计算容器数据」等全局解析设置切换时立即生效。
     */
    public static void forceRefreshHud() {
        lastRefreshTime = 0;
        refreshHud();
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 优化 #3：预计算 HUD 渲染所需的所有尺寸，避免每帧重复计算。
     * 包级可见：HudConfigScreen 切换盒/组开关时直接调用。
     */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /**
//$$     * 优化 #3：预计算 HUD 渲染所需的所有尺寸，避免每帧重复计算。
//$$     */
//#endif
    static void recomputeCachedDimensions() {
        MinecraftClient client = MinecraftClient.getInstance();
        int fontSize = Math.max(1, Math.min(32, MaterialDataStore.persistedHudFontSize));
        int maxLines = Math.max(1, Math.min(20, MaterialDataStore.persistedHudMaxLines));
        int bgAlpha = Math.max(0, Math.min(100, MaterialDataStore.persistedHudBgAlpha));
        float scale = fontSize / 10.0f;

        String format = getHudFormat();
        boolean customFormat = DEFAULT_HUD_FORMAT.equals(format) == false;
        List<HudFormatSegment> formatSegments = customFormat ? parseHudFormat(format) : List.of();
        cachedFormatString = format;
        cachedCustomFormat = customFormat;
        cachedFormatSegments = formatSegments;

        int startIdx = currentPage * maxLines;
        int endIdx = Math.min(startIdx + maxLines, hudItems.size());

        if (endIdx <= startIdx) {
            cachedTotalW = 0; cachedTotalH = 0;
            cachedLineHeight = 0; cachedIconSize = 0;
            cachedPageText = "";
            cachedMaxNameW = 0;
            cachedCountMaxW = 0;
            cachedMaxNumW = 0;
            cachedMaxSuffixW = 0;
            cachedPrefixMaxW = 0;
            cachedPipedMaxW = new int[0];
            cachedShowBox = MaterialDataStore.persistedHudShowBox;
            cachedShowGroup = MaterialDataStore.persistedHudShowGroup;
            return;
        }

        // 全部行都参与宽度计算（含 missing==0 的行，修复箱子/工作台等不缺物品时不显示的问题）
        // 默认格式沿用「名称列 + 数量列」布局；自定义格式中只有被两个 | 夹住的片段按列宽对齐。
        int lineHeight = (int)(10 * scale);
        int iconSize = lineHeight;
        int maxNameW = 0;
        int maxNumW = 0;
        int maxSuffixW = 0;
        int maxRowW = 0;
        int prefixMaxW = 0;
        int[] pipedMaxW = new int[formatSegments.size()];

        if (customFormat) {
            int segCount = formatSegments.size();
            boolean rightAligned = hudAnchor() == 2 || hudAnchor() == 3;
            if (segCount <= 2) {
                // 没有 | 或只有一个 |：不存在被两个 | 夹住的片段，全部按自然宽度排列
                for (int i = startIdx; i < endIdx; i++) {
                    HudItem hi = hudItems.get(i);
                    int rowW = 0;
                    for (HudFormatSegment segment : formatSegments) {
                        rowW += measureHudSegment(segment, hi, client.textRenderer, scale, iconSize);
                    }
                    if (rowW > maxRowW) maxRowW = rowW;
                }
            } else {
                // 前缀与各 |...| 片段：记录当前页的最大宽度作为对齐基准
                for (int i = startIdx; i < endIdx; i++) {
                    HudItem hi = hudItems.get(i);
                    int prefixW = measureHudSegment(formatSegments.get(0), hi, client.textRenderer, scale, iconSize);
                    if (prefixW > prefixMaxW) prefixMaxW = prefixW;
                    for (int s = 1; s < segCount - 1; s++) {
                        int w = measureHudSegment(formatSegments.get(s), hi, client.textRenderer, scale, iconSize);
                        if (w > pipedMaxW[s]) pipedMaxW[s] = w;
                    }
                }
                int lastPiped = segCount - 2;
                for (int i = startIdx; i < endIdx; i++) {
                    HudItem hi = hudItems.get(i);
                    int rowW = prefixMaxW;
                    for (int s = 1; s <= lastPiped; s++) {
                        // 左侧锚点时，最后一段的实际内容紧跟其后（不占满整列），
                        // 让 | 之后的普通文字按自然位置排列
                        rowW += (s == lastPiped && !rightAligned)
                                ? measureHudSegment(formatSegments.get(s), hi, client.textRenderer, scale, iconSize)
                                : pipedMaxW[s];
                    }
                    rowW += measureHudSegment(formatSegments.get(segCount - 1), hi, client.textRenderer, scale, iconSize);
                    if (rowW > maxRowW) maxRowW = rowW;
                }
            }
        } else {
            for (int i = startIdx; i < endIdx; i++) {
                int w = (int)(client.textRenderer.getWidth(hudItems.get(i).name) * scale);
                if (w > maxNameW) maxNameW = w;
                int nw = (int)(client.textRenderer.getWidth(String.valueOf(hudItems.get(i).missing)) * scale);
                if (nw > maxNumW) maxNumW = nw;
                int sw = (int)(client.textRenderer.getWidth(formatHudCountSuffix(hudItems.get(i).missing)) * scale);
                if (sw > maxSuffixW) maxSuffixW = sw;
            }
        }
        int maxCountW = maxNumW + maxSuffixW;

        // 优化 #2：HUD 高度固定 = 字体大小 × 写入行数（无论实际有几行可见）
        int totalH = maxLines * lineHeight + 4;

        String pageText = (startIdx + 1) + "-" + endIdx + "/" + hudItems.size();
        int pageW = (int)(client.textRenderer.getWidth(pageText) * scale);
        int pageH = (int)(8 * scale);
        totalH += pageH + 2;

        int totalW = customFormat
                ? Math.max(maxRowW, pageW) + 8
                : (int)(iconSize + 4 + maxNameW + 4 + maxCountW + 8);

        cachedTotalW = totalW;
        cachedTotalH = totalH;
        cachedLineHeight = lineHeight;
        cachedIconSize = iconSize;
        cachedPrefixMaxW = prefixMaxW;
        cachedPipedMaxW = pipedMaxW;
        cachedPageW = pageW;
        cachedPageText = pageText;
        cachedMaxNameW = maxNameW;
        cachedCountMaxW = maxCountW;
        cachedMaxNumW = maxNumW;
        cachedMaxSuffixW = maxSuffixW;
        cachedFontSize = fontSize;
        cachedMaxLines = maxLines;
        cachedBgAlpha = bgAlpha;
        cachedCurrentPage = currentPage;
        cachedScale = scale;
        cachedShowBox = MaterialDataStore.persistedHudShowBox;
        cachedShowGroup = MaterialDataStore.persistedHudShowGroup;
    }

    /** 包级可见：HudConfigScreen 拖拽预览使用实际 HUD 宽度 */
    static int getCachedTotalW() {
        if (cachedTotalW <= 0 && !hudItems.isEmpty()) recomputeCachedDimensions();
        return cachedTotalW;
    }

    /** 包级可见：HudConfigScreen 拖拽预览使用实际 HUD 高度 */
    static int getCachedTotalH() {
        if (cachedTotalH <= 0 && !hudItems.isEmpty()) recomputeCachedDimensions();
        return cachedTotalH;
    }

    /** HUD 校准锚点：0=左上(默认) 1=左下 2=右上 3=右下 */
    private static int hudAnchor() {
        int a = MaterialDataStore.persistedHudAnchor;
        return (a >= 0 && a <= 3) ? a : 0;
    }

    /**
     * HUD 左上角实际 X 坐标。
     * 绝对模式直接返回持久化的左上角 X；
     * 比例模式按锚点换算：左侧锚点从左边缘量起，右侧锚点从右边缘量起（HUD 右缘距右边距为偏移量）。
     */
    public static int getEffectiveHudX() {
        if (!MaterialDataStore.persistedHudUseRatio) return MaterialDataStore.persistedHudX;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return MaterialDataStore.persistedHudX;
        int sw = client.getWindow().getScaledWidth();
        if (sw <= 0) return MaterialDataStore.persistedHudX;
        int offset = Math.round(MaterialDataStore.persistedHudRatioX * sw);
        int a = hudAnchor();
        if (a == 2 || a == 3) {
            return sw - getCachedTotalW() - offset;
        }
        return offset;
    }

    /** HUD 左上角实际 Y 坐标：比例模式按锚点换算，下侧锚点从下边缘量起（HUD 下缘距下边距为偏移量）。 */
    public static int getEffectiveHudY() {
        if (!MaterialDataStore.persistedHudUseRatio) return MaterialDataStore.persistedHudY;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return MaterialDataStore.persistedHudY;
        int sh = client.getWindow().getScaledHeight();
        if (sh <= 0) return MaterialDataStore.persistedHudY;
        int offset = Math.round(MaterialDataStore.persistedHudRatioY * sh);
        int a = hudAnchor();
        if (a == 1 || a == 3) {
            return sh - getCachedTotalH() - offset;
        }
        return offset;
    }

    /** 由左上角绝对坐标按指定锚点换算比例 X（供锚点/比例开关切换时保持 HUD 位置一致）。 */
    public static float ratioXFor(int absX, int screenW, int anchor, int hudW) {
        if (screenW <= 0) return 0f;
        if (anchor == 2 || anchor == 3) return (screenW - absX - hudW) / (float) screenW;
        return absX / (float) screenW;
    }

    /** 由左上角绝对坐标按指定锚点换算比例 Y（供锚点/比例开关切换时保持 HUD 位置一致）。 */
    public static float ratioYFor(int absY, int screenH, int anchor, int hudH) {
        if (screenH <= 0) return 0f;
        if (anchor == 1 || anchor == 3) return (screenH - absY - hudH) / (float) screenH;
        return absY / (float) screenH;
    }

    // ==================== 自定义 HUD 行格式 ====================
    /** 默认格式：与原有 HUD 布局完全一致，走原有绘制路径。 */
    public static final String DEFAULT_HUD_FORMAT = "[icon][name]|[item]|([box],[stack])";

    private enum HudFormatTokenType { ICON, NAME, ITEM, BOX, STACK, LITERAL }

    private record HudFormatToken(HudFormatTokenType type, String text) {}

    /** 按 | 分隔的格式片段；aligned=true 表示位于两个 | 之间，按 HUD 校准位置居左/居右。 */
    private record HudFormatSegment(boolean aligned, List<HudFormatToken> tokens) {}

    private static String cachedFormatString = null;
    private static boolean cachedCustomFormat = false;
    private static List<HudFormatSegment> cachedFormatSegments = List.of();
    private static int cachedPrefixMaxW = 0;
    private static int[] cachedPipedMaxW = new int[0];

    /** 当前生效的 HUD 行格式；空字符串/未设置时回退到默认格式。 */
    public static String getHudFormat() {
        String format = MaterialDataStore.persistedHudFormat;
        return (format == null || format.isEmpty()) ? DEFAULT_HUD_FORMAT : format;
    }

    /**
     * 解析格式串：先按 | 分段，每段内再识别五个变量。
     * | 本身不显示，仅作为片段的边界；位于两个 | 之间的片段按 HUD 校准位置对齐，
     * 其余片段按格式顺序依次排列。片段宽度取当前页所有行中的最大值，形成对齐基准。
     */
    private static List<HudFormatSegment> parseHudFormat(String format) {
        List<HudFormatSegment> segments = new ArrayList<>();
        String[] parts = format.split("\\|", -1);
        for (int s = 0; s < parts.length; s++) {
            boolean aligned = s > 0 && s < parts.length - 1;
            segments.add(new HudFormatSegment(aligned, parseHudFormatTokens(parts[s])));
        }
        return segments;
    }

    /** 解析单个片段内的变量与字面文字。 */
    private static List<HudFormatToken> parseHudFormatTokens(String format) {
        List<HudFormatToken> tokens = new ArrayList<>();
        StringBuilder literal = new StringBuilder();
        int i = 0;
        while (i < format.length()) {
            HudFormatTokenType type = null;
            int len = 0;
            if (format.startsWith("[icon]", i)) { type = HudFormatTokenType.ICON; len = 6; }
            else if (format.startsWith("[name]", i)) { type = HudFormatTokenType.NAME; len = 6; }
            else if (format.startsWith("[item]", i)) { type = HudFormatTokenType.ITEM; len = 6; }
            else if (format.startsWith("[box]", i)) { type = HudFormatTokenType.BOX; len = 5; }
            else if (format.startsWith("[stack]", i)) { type = HudFormatTokenType.STACK; len = 7; }

            if (type == null) {
                literal.append(format.charAt(i));
                i++;
                continue;
            }
            if (literal.length() > 0) {
                tokens.add(new HudFormatToken(HudFormatTokenType.LITERAL, literal.toString()));
                literal.setLength(0);
            }
            tokens.add(new HudFormatToken(type, ""));
            i += len;
        }
        if (literal.length() > 0) {
            tokens.add(new HudFormatToken(HudFormatTokenType.LITERAL, literal.toString()));
        }
        return tokens;
    }

    private static String formatBoxValue(int count) {
        return String.format(Locale.ROOT, "%.2f%s", count / 1728.0,
                com.litematlist.I18n.tr("litematlist.hud.box_unit"));
    }

    private static String formatStackValue(int count) {
        return String.format(Locale.ROOT, "%.2f%s", count / 64.0,
                com.litematlist.I18n.tr("litematlist.hud.group_unit"));
    }

    /** 按格式片段测量宽度（供自定义格式计算各列宽度）。 */
    private static int measureHudSegment(HudFormatSegment segment, HudItem item,
            net.minecraft.client.font.TextRenderer tr, float scale, int iconSize) {
        int width = 0;
        boolean showBox = MaterialDataStore.persistedHudShowBox;
        boolean showGroup = MaterialDataStore.persistedHudShowGroup;
        for (HudFormatToken token : segment.tokens()) {
            switch (token.type()) {
                case ICON -> width += iconSize;
                case NAME -> width += (int)(tr.getWidth(item.name) * scale);
                case ITEM -> width += (int)(tr.getWidth(String.valueOf(item.missing)) * scale);
                case BOX -> {
                    if (showBox && item.missing > 0) {
                        width += (int)(tr.getWidth(formatBoxValue(item.missing)) * scale);
                    }
                }
                case STACK -> {
                    if (showGroup && item.missing > 0) {
                        width += (int)(tr.getWidth(formatStackValue(item.missing)) * scale);
                    }
                }
                case LITERAL -> width += (int)(tr.getWidth(token.text()) * scale);
            }
        }
        return width;
    }

    /**
     * 格式化 HUD 中显示的数量：按配置追加盒(1728)/组(64)量级换算，两位小数。
     * 例如 1730 且双开关开启 → "1730(1.00盒,27.03组)"；只开盒 → "1730(1.00盒)；只开组 → "1730(27.03组)"。
     */
    public static String formatHudCount(int count) {
        return String.valueOf(count) + formatHudCountSuffix(count);
    }

    /** 数量显示的「(盒,组)」后缀部分（不含红字数字）；不需要后缀时返回空串。 */
    public static String formatHudCountSuffix(int count) {
        if (count <= 0) return "";
        boolean showBox = MaterialDataStore.persistedHudShowBox;
        boolean showGroup = MaterialDataStore.persistedHudShowGroup;
        if (!showBox && !showGroup) return "";
        StringBuilder sb = new StringBuilder("(");
        if (showBox) {
            sb.append(String.format(Locale.ROOT, "%.2f%s", count / 1728.0, com.litematlist.I18n.tr("litematlist.hud.box_unit")));
        }
        if (showBox && showGroup) sb.append(',');
        if (showGroup) {
            sb.append(String.format(Locale.ROOT, "%.2f%s", count / 64.0, com.litematlist.I18n.tr("litematlist.hud.group_unit")));
        }
        return sb.append(')').toString();
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 按颜色分段绘制 HUD 数量（整列左对齐，绿"0"与红色数字同起点）：
     * 主数量保持红/绿，"(x盒,y组)"后缀中括号与逗号为白色、盒数为 #3E153E、组数为 #FFCA3A。
     */
//#elseif MC == 11701 || MC == 11904
//$$    /**
//$$     * 按颜色分段绘制 HUD 数量：
//$$     * 主数量保持红/绿，"(x盒,y组)"后缀中括号与逗号为白色、盒数为 #9542C1、组数为 #FFCA3A。
//$$     */
//#elseif MC == 12001
//$$    /**
//$$     * 按颜色分段绘制 HUD 数量（整列左对齐，绿"0"与红色数字同起点）：
//$$     * 主数量保持红/绿，"(x盒,y组)"后缀中括号与逗号为白色、盒数为 #9542C1、组数为 #FFCA3A。
//$$     */
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static void drawHudCount(DrawContext context, net.minecraft.client.font.TextRenderer tr, int count, int leftX, int y) {
//#elseif MC == 11701 || MC == 11904
//$$    private static void drawHudCount(MatrixStack matrices, net.minecraft.client.font.TextRenderer tr, int count, int leftX, int y) {
//#endif
        int mainColor = count > 0 ? Configs.Colors.HUD_MISSING_COUNT.getIntegerValue() : 0xFF55FF55;
        String countStr = String.valueOf(count);
        if (count <= 0 || (!MaterialDataStore.persistedHudShowBox && !MaterialDataStore.persistedHudShowGroup)) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            context.drawText(tr, countStr, leftX, y, mainColor, false);
//#elseif MC == 11701 || MC == 11904
//$$            tr.draw(matrices, countStr, leftX, y, mainColor);
//#endif
            return;
        }

        boolean showBox = MaterialDataStore.persistedHudShowBox;
        boolean showGroup = MaterialDataStore.persistedHudShowGroup;
        int cx = leftX;

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        context.drawText(tr, countStr, cx, y, mainColor, false);
//#elseif MC == 11701 || MC == 11904
//$$        tr.draw(matrices, countStr, cx, y, mainColor);
//#endif
        cx += tr.getWidth(countStr);

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        cx = appendColored(context, tr, "(", cx, y, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$        cx = appendColored(matrices, tr, "(", cx, y, 0xFFFFFFFF);
//#endif
        if (showBox) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            cx = appendColored(context, tr, String.format(Locale.ROOT, "%.2f%s",
//#elseif MC == 11701 || MC == 11904
//$$            cx = appendColored(matrices, tr, String.format(Locale.ROOT, "%.2f%s",
//#endif
                    count / 1728.0, com.litematlist.I18n.tr("litematlist.hud.box_unit")), cx, y,
                    Configs.Colors.HUD_MISSING_BOX.getIntegerValue());
        }
        if (showBox && showGroup) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            cx = appendColored(context, tr, ",", cx, y, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            cx = appendColored(matrices, tr, ",", cx, y, 0xFFFFFFFF);
//#endif
        }
        if (showGroup) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            cx = appendColored(context, tr, String.format(Locale.ROOT, "%.2f%s",
//#elseif MC == 11701 || MC == 11904
//$$            cx = appendColored(matrices, tr, String.format(Locale.ROOT, "%.2f%s",
//#endif
                    count / 64.0, com.litematlist.I18n.tr("litematlist.hud.group_unit")), cx, y,
                    Configs.Colors.HUD_MISSING_GROUP.getIntegerValue());
        }
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        appendColored(context, tr, ")", cx, y, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$        appendColored(matrices, tr, ")", cx, y, 0xFFFFFFFF);
//#endif
    }

    /** 以指定颜色绘制一段文字，并返回下一个片段的起始 X 坐标 */
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static int appendColored(DrawContext context, net.minecraft.client.font.TextRenderer tr, String text, int x, int y, int color) {
//#elseif MC == 11701 || MC == 11904
//$$    private static int appendColored(MatrixStack matrices, net.minecraft.client.font.TextRenderer tr, String text, int x, int y, int color) {
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        context.drawText(tr, text, x, y, color, false);
//#elseif MC == 11701 || MC == 11904
//$$        tr.draw(matrices, text, x, y, color);
//#endif
        return x + tr.getWidth(text);
    }

    /** 自定义格式的整行绘制：只有被两个 | 夹住的片段按 HUD 校准位置居左/居右。 */
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static void drawCustomHudRow(DrawContext context, MinecraftClient client, HudItem item,
            int startX, int textY, float fontScale) {
//#elseif MC == 11701 || MC == 11904
//$$    private static void drawCustomHudRow(MatrixStack matrices, MinecraftClient client, HudItem item,
//$$            int startX, int textY, float fontScale) {
//#endif
        int iconY = textY - 1;
        float itemScale = cachedIconSize / 16.0f;
        boolean showBox = MaterialDataStore.persistedHudShowBox;
        boolean showGroup = MaterialDataStore.persistedHudShowGroup;
        int countColor = item.missing > 0 ? Configs.Colors.HUD_MISSING_COUNT.getIntegerValue() : 0xFF55FF55;
        net.minecraft.client.font.TextRenderer tr = client.textRenderer;
        boolean rightAligned = hudAnchor() == 2 || hudAnchor() == 3;
        int segCount = cachedFormatSegments.size();

        if (segCount <= 2) {
            // 没有 | 或只有一个 |：不存在被两个 | 夹住的片段，所有内容按自然宽度依次绘制
            int cursorX = startX;
            for (HudFormatSegment segment : cachedFormatSegments) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                cursorX = drawHudSegmentTokens(context, tr, item, segment, cursorX, textY, fontScale,
                        itemScale, showBox, showGroup, countColor);
//#elseif MC == 11701 || MC == 11904
//$$                cursorX = drawHudSegmentTokens(matrices, tr, item, segment, cursorX, textY, fontScale,
//$$                        itemScale, showBox, showGroup, countColor);
//#endif
            }
            return;
        }

        // 第一个 | 之前：内容左对齐，列宽取本页最大值作为对齐基准
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawHudSegmentTokens(context, tr, item, cachedFormatSegments.get(0), startX, textY, fontScale,
                itemScale, showBox, showGroup, countColor);
//#elseif MC == 11701 || MC == 11904
//$$        drawHudSegmentTokens(matrices, tr, item, cachedFormatSegments.get(0), startX, textY, fontScale,
//$$                itemScale, showBox, showGroup, countColor);
//#endif
        int cursorX = startX + cachedPrefixMaxW;
        int lastPiped = segCount - 2;
        for (int s = 1; s <= lastPiped; s++) {
            HudFormatSegment segment = cachedFormatSegments.get(s);
            int segMaxW = s < cachedPipedMaxW.length ? cachedPipedMaxW[s] : 0;
            int segW = measureHudSegment(segment, item, tr, fontScale, cachedIconSize);
            int segX = rightAligned ? cursorX + segMaxW - segW : cursorX;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawHudSegmentTokens(context, tr, item, segment, segX, textY, fontScale,
                    itemScale, showBox, showGroup, countColor);
//#elseif MC == 11701 || MC == 11904
//$$            drawHudSegmentTokens(matrices, tr, item, segment, segX, textY, fontScale,
//$$                    itemScale, showBox, showGroup, countColor);
//#endif
            // 左侧锚点：最后一段之后按自然宽度继续，| 后的文字不会被人为分散对齐
            cursorX += (s == lastPiped && !rightAligned) ? segW : segMaxW;
        }

        // 最后一个 | 之后：紧跟对齐片段，按自然宽度绘制
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawHudSegmentTokens(context, tr, item, cachedFormatSegments.get(segCount - 1), cursorX, textY,
                fontScale, itemScale, showBox, showGroup, countColor);
//#elseif MC == 11701 || MC == 11904
//$$        drawHudSegmentTokens(matrices, tr, item, cachedFormatSegments.get(segCount - 1), cursorX, textY,
//$$                fontScale, itemScale, showBox, showGroup, countColor);
//#endif
    }

    /** 绘制一个格式片段内的所有变量与文字，返回绘制后的 X 坐标。 */
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static int drawHudSegmentTokens(DrawContext context, net.minecraft.client.font.TextRenderer tr,
            HudItem item, HudFormatSegment segment, int cursorX, int textY, float fontScale,
            float itemScale, boolean showBox, boolean showGroup, int countColor) {
//#elseif MC == 11701 || MC == 11904
//$$    private static int drawHudSegmentTokens(MatrixStack matrices, net.minecraft.client.font.TextRenderer tr,
//$$            HudItem item, HudFormatSegment segment, int cursorX, int textY, float fontScale,
//$$            float itemScale, boolean showBox, boolean showGroup, int countColor) {
//#endif
        int iconY = textY - 1;
        for (HudFormatToken token : segment.tokens()) {
            switch (token.type()) {
                case ICON -> {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    drawCustomHudItem(context, item.stack, cursorX, iconY, itemScale);
//#elseif MC == 11701 || MC == 11904
//$$                    drawCustomHudItem(matrices, item.stack, cursorX, iconY, itemScale);
//#endif
                    cursorX += cachedIconSize;
                }
                case NAME -> {
                    String text = item.name;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    drawScaledText(context, tr, text, cursorX, textY, 0xFFFFFFFF, fontScale);
//#elseif MC == 11701 || MC == 11904
//$$                    drawScaledText(matrices, tr, text, cursorX, textY, 0xFFFFFFFF, fontScale);
//#endif
                    cursorX += (int)(tr.getWidth(text) * fontScale);
                }
                case ITEM -> {
                    String text = String.valueOf(item.missing);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    drawScaledText(context, tr, text, cursorX, textY, countColor, fontScale);
//#elseif MC == 11701 || MC == 11904
//$$                    drawScaledText(matrices, tr, text, cursorX, textY, countColor, fontScale);
//#endif
                    cursorX += (int)(tr.getWidth(text) * fontScale);
                }
                case BOX -> {
                    if (showBox && item.missing > 0) {
                        String text = formatBoxValue(item.missing);
                        int color = Configs.Colors.HUD_MISSING_BOX.getIntegerValue();
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        drawScaledText(context, tr, text, cursorX, textY, color, fontScale);
//#elseif MC == 11701 || MC == 11904
//$$                        drawScaledText(matrices, tr, text, cursorX, textY, color, fontScale);
//#endif
                        cursorX += (int)(tr.getWidth(text) * fontScale);
                    }
                }
                case STACK -> {
                    if (showGroup && item.missing > 0) {
                        String text = formatStackValue(item.missing);
                        int color = Configs.Colors.HUD_MISSING_GROUP.getIntegerValue();
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        drawScaledText(context, tr, text, cursorX, textY, color, fontScale);
//#elseif MC == 11701 || MC == 11904
//$$                        drawScaledText(matrices, tr, text, cursorX, textY, color, fontScale);
//#endif
                        cursorX += (int)(tr.getWidth(text) * fontScale);
                    }
                }
                case LITERAL -> {
                    if (token.text().isEmpty() == false) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        drawScaledText(context, tr, token.text(), cursorX, textY, 0xFFFFFFFF, fontScale);
//#elseif MC == 11701 || MC == 11904
//$$                        drawScaledText(matrices, tr, token.text(), cursorX, textY, 0xFFFFFFFF, fontScale);
//#endif
                        cursorX += (int)(tr.getWidth(token.text()) * fontScale);
                    }
                }
            }
        }
        return cursorX;
    }

    /** 自定义格式下绘制物品图标（缩放与未缩放两种情况）。 */
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static void drawCustomHudItem(DrawContext context, ItemStack stack, int x, int y, float itemScale) {
        if (itemScale == 1.0f) {
            context.drawItem(stack, x, y);
        } else {
            drawScaledItem(context, stack, x, y, itemScale);
        }
    }
//#elseif MC == 11701
//$$    private static void drawCustomHudItem(MatrixStack matrices, ItemStack stack, int x, int y, float itemScale) {
//$$        if (itemScale == 1.0f) {
//$$            MinecraftClient.getInstance().getItemRenderer().renderGuiItemIcon(stack, x, y);
//$$        } else {
//$$            drawScaledItem(matrices, stack, x, y, itemScale);
//$$        }
//$$    }
//#elseif MC == 11904
//$$    private static void drawCustomHudItem(MatrixStack matrices, ItemStack stack, int x, int y, float itemScale) {
//$$        if (itemScale == 1.0f) {
//$$            MinecraftClient.getInstance().getItemRenderer().renderInGui(matrices, stack, x, y);
//$$        } else {
//$$            drawScaledItem(matrices, stack, x, y, itemScale);
//$$        }
//$$    }
//#endif

    /**
     * 扫描背包一次（无缓存，由 updateMissingCounts 调用）。
     * 优化 #2：shulkerBoxHasItems 检查确保空潜影盒不被遍历。
     */
    private static Map<Item, Integer> scanInventory() {
        MinecraftClient client = MinecraftClient.getInstance();
        Map<Item, Integer> playerInventory = new HashMap<>();
        if (client.player != null) {
            for (int i = 0; i < client.player.getInventory().size(); i++) {
                ItemStack stack = client.player.getInventory().getStack(i);
                if (stack.isEmpty()) continue;
                Item stackItem = stack.getItem();
                playerInventory.merge(stackItem, stack.getCount(), Integer::sum);
                // 优化 #2：仅当潜影盒有物品时才遍历内部
                if (fi.dy.masa.malilib.util.InventoryUtils.shulkerBoxHasItems(stack)) {
                    for (ItemStack boxStack : fi.dy.masa.malilib.util.InventoryUtils.getStoredItems(stack)) {
                        if (!boxStack.isEmpty()) {
                            playerInventory.merge(boxStack.getItem(), boxStack.getCount(), Integer::sum);
                        }
                    }
                }
            }
        }
        return playerInventory;
    }

    /**
     * 优化 #7：计算材料列表结构哈希，用于判断是否需要全量重建。
     */
    private static String computeMaterialHash(List<LitematicReader.MaterialEntry> materials,
            Set<Item> ignored, boolean hideNoMissing, String sortName,
            Set<Item> pinned, Path filePath) {
        StringBuilder sb = new StringBuilder();
        sb.append(sortName).append("|").append(hideNoMissing).append("|");
        List<LitematicReader.MaterialEntry> sorted = new ArrayList<>(materials);
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        sorted.sort(Comparator.comparing(m -> Registries.ITEM.getId(m.item()).toString()));
//#elseif MC == 11701
//$$        sorted.sort(Comparator.comparing(m -> Registry.ITEM.getId(m.item()).toString()));
//#endif
        for (LitematicReader.MaterialEntry m : sorted) {
            Item item = m.item();
            if (filePath != null) {
                Item replaced = MaterialDataStore.getReplacement(filePath, item);
                if (replaced != item) item = replaced;
            }
            if (ignored.contains(item)) continue;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            sb.append(Registries.ITEM.getId(item)).append(":")
//#elseif MC == 11701
//$$            sb.append(Registry.ITEM.getId(item)).append(":")
//#endif
              .append(m.totalCount()).append(":")
              .append(pinned.contains(item)).append(",");
        }
        return sb.toString();
    }

    private static Comparator<HudItem> getComparator(String sortName) {
        if (sortName == null || sortName.equals("NONE")) return null;
        return switch (sortName) {
            case "TOTAL_DESC" -> Comparator.comparingInt((HudItem h) -> h.totalCount).reversed();
            case "TOTAL_ASC" -> Comparator.comparingInt(h -> h.totalCount);
            case "MISS_DESC" -> Comparator.comparingInt((HudItem h) -> h.missing).reversed();
            case "MISS_ASC" -> Comparator.comparingInt(h -> h.missing);
            default -> null;
        };
    }

    private static List<LitematicReader.MaterialEntry> getEntryMaterials(MaterialListScreen.LoadedEntry entry) {
        if (entry.filePath() == null) return null;
        Path filePath = entry.filePath();

        List<LitematicReader.MaterialEntry> cached = MaterialDataStore.getImportedMaterials().get(filePath);
        if (cached != null) return cached;

        String fileName = filePath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".litematic")) {
            // 走注入器缓存加载：「同步投影渲染层」开启时对「从投影同步」条目自动应用层级过滤
            return MaterialListInjector.loadMaterialsCached(filePath);
        }

        MaterialListImporter.ImportResult result = MaterialListImporter.importFromFile(filePath);
        if (result != null) return result.materials();

        return null;
    }

    private static List<LitematicReader.MaterialEntry> getProjectMaterials(MaterialListScreen.LoadedEntry projectEntry) {
        List<MaterialListScreen.LoadedEntry> children = MaterialListScreen.getProjectEntries(projectEntry.name());
        if (children.isEmpty()) return null;

        Map<Item, Integer> itemTotals = new HashMap<>();
        Map<Item, String> itemNames = new HashMap<>();
        Map<Item, Integer> itemCategory = new HashMap<>();

        for (MaterialListScreen.LoadedEntry child : children) {
            if (child.filePath() == null) continue;
            List<LitematicReader.MaterialEntry> childMaterials = getEntryMaterials(child);
            if (childMaterials == null) continue;

            for (LitematicReader.MaterialEntry m : childMaterials) {
                Item item = m.item();
                Item replaced = MaterialDataStore.getReplacement(child.filePath(), item);
                if (replaced != item) item = replaced;
                if (MaterialDataStore.getIgnoredForPath(child.filePath()).contains(item)) continue;
                itemTotals.merge(item, m.totalCount(), Integer::sum);
                itemNames.putIfAbsent(item, m.blockName());
                itemCategory.merge(item, m.category(), Math::min);
            }
        }

        List<LitematicReader.MaterialEntry> result = new ArrayList<>();
        for (Map.Entry<Item, Integer> e : itemTotals.entrySet()) {
            result.add(new LitematicReader.MaterialEntry(e.getKey(), itemNames.getOrDefault(e.getKey(), "?"),
                    e.getValue(), 0, null,
                    itemCategory.getOrDefault(e.getKey(), LitematicReader.MaterialEntry.CATEGORY_BUILDING)));
        }
        // 分类优先（建筑材料→容器数据→实体数据→实体容器数据），组内按数量降序
        result.sort(Comparator.comparingInt(LitematicReader.MaterialEntry::category)
                .thenComparing(Comparator.comparingInt(LitematicReader.MaterialEntry::totalCount).reversed()));
        return result.isEmpty() ? null : result;
    }

    // ==================== 渲染 ====================
    /**
     * 优化后的渲染方法：
     * - 优化 #3：使用预计算尺寸，避免每帧遍历计算
     * - 优化 #6：无缩放时跳过矩阵 push/pop（最快路径），缩放时按需矩阵
     * - 优化 #4：背景用 fill（极快），尺寸已预计算
     */
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public static void render(DrawContext context) {
//#elseif MC == 11701 || MC == 11904
//$$    public static void render(MatrixStack matrices) {
//#endif
        if (!MaterialDataStore.persistedHudVisible) return;
        if (MinecraftClient.getInstance().options.hudHidden) return;
        if (hudItems.isEmpty()) return;

        int x = getEffectiveHudX();
        int y = getEffectiveHudY();

        // 检查是否需要重新计算尺寸（配置变化或翻页）
        int fontSize = Math.max(1, Math.min(32, MaterialDataStore.persistedHudFontSize));
        int maxLines = Math.max(1, Math.min(20, MaterialDataStore.persistedHudMaxLines));
        int bgAlpha = Math.max(0, Math.min(100, MaterialDataStore.persistedHudBgAlpha));
        if (fontSize != cachedFontSize || maxLines != cachedMaxLines
                || bgAlpha != cachedBgAlpha || currentPage != cachedCurrentPage
                || MaterialDataStore.persistedHudShowBox != cachedShowBox
                || MaterialDataStore.persistedHudShowGroup != cachedShowGroup
                || getHudFormat().equals(cachedFormatString) == false) {
            recomputeCachedDimensions();
        }

        if (cachedTotalW <= 0 || cachedTotalH <= 0) return;

        int startIdx = currentPage * maxLines;
        int endIdx = Math.min(startIdx + maxLines, hudItems.size());

        MinecraftClient client = MinecraftClient.getInstance();

        // 优化 #4：背景（fill 是单个 GL 矩形调用，极快，尺寸已预计算）
        int bgColor = (bgAlpha * 255 / 100) << 24 | 0x000000;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        context.fill(x, y, x + cachedTotalW, y + cachedTotalH, bgColor);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, x, y, x + cachedTotalW, y + cachedTotalH, bgColor);
//#endif

        float itemScale = cachedIconSize / 16.0f;
        float fontScale = fontSize / 10.0f;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
        // 图标等比缩放到与文字同高；文字按 fontScale 缩放，使「字号设置」真正生效，
        // 并继承游戏 GUI 缩放（HUD 绘制在 GUI 缩放坐标系中）→ 实际大小 = UI大小 × 字号倍率。
        int visibleLine = 0;
        for (int i = startIdx; i < endIdx; i++) {
            HudItem hi = hudItems.get(i);
            int lineY = y + 2 + visibleLine * cachedLineHeight;
            visibleLine++;
            if (cachedCustomFormat) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawCustomHudRow(context, client, hi, x + 2, lineY + 1, fontScale);
//#elseif MC == 11701 || MC == 11904
//$$                drawCustomHudRow(matrices, client, hi, x + 2, lineY + 1, fontScale);
//#endif
                continue;
            }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
            if (itemScale == 1.0f) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                context.drawItem(hi.stack, x + 2, lineY);
//#elseif MC == 11701
//$$                MinecraftClient.getInstance().getItemRenderer().renderGuiItemIcon(hi.stack, x + 2, lineY);
//#elseif MC == 11904
//$$                MinecraftClient.getInstance().getItemRenderer().renderInGui(matrices, hi.stack, x + 2, lineY);
//#endif
            } else {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawScaledItem(context, hi.stack, x + 2, lineY, itemScale);
//#elseif MC == 11701 || MC == 11904
//$$                drawScaledItem(matrices, hi.stack, x + 2, lineY, itemScale);
//#endif
            }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawScaledText(context, client.textRenderer, hi.name,
//#elseif MC == 11701 || MC == 11904
//$$            drawScaledText(matrices, client.textRenderer, hi.name,
//#endif
                    x + 2 + cachedIconSize + 4, lineY + 1, 0xFFFFFFFF, fontScale);
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
            // 数量起始 X：默认整列按最宽数量左对齐（保持原行为）；
            // 右侧锚点时红字统一右对齐到「HUD 右缘 - 盒/组最大宽度」，盒/组紧跟在红字右侧
            int countLeftX = x + cachedTotalW - 8 - cachedCountMaxW;
            int anchor = hudAnchor();
            if (anchor == 2 || anchor == 3) {
                int numW = (int)(client.textRenderer.getWidth(String.valueOf(hi.missing)) * fontScale);
                int numberRightX = x + cachedTotalW - 8 - cachedMaxSuffixW;
                countLeftX = numberRightX - numW;
            }
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawScaledHudCount(context, client.textRenderer, hi.missing,
//#elseif MC == 11701 || MC == 11904
//$$            drawScaledHudCount(matrices, client.textRenderer, hi.missing,
//#endif
                    countLeftX, lineY + 1, fontScale);
        }

        // 页码
        int pageY = y + 2 + maxLines * cachedLineHeight + 2;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawScaledText(context, client.textRenderer, cachedPageText,
//#elseif MC == 11701 || MC == 11904
//$$        drawScaledText(matrices, client.textRenderer, cachedPageText,
//#endif
                x + (cachedTotalW - cachedPageW) / 2, pageY, 0xFFAAAAAA, fontScale);
    }

    /** 绘制按 fontScale 缩放的文字（fontScale==1 时走最快路径）。 */
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static void drawScaledText(DrawContext context, net.minecraft.client.font.TextRenderer tr,
//#elseif MC == 11701 || MC == 11904
//$$    private static void drawScaledText(MatrixStack matrices, net.minecraft.client.font.TextRenderer tr,
//#endif
            String text, int x, int y, int color, float fontScale) {
        if (fontScale == 1.0f) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            context.drawText(tr, text, x, y, color, false);
//#elseif MC == 11701 || MC == 11904
//$$            tr.draw(matrices, text, x, y, color);
//#endif
            return;
        }
//#if MC == 12108 || MC == 12110 || MC == 12111
        context.getMatrices().pushMatrix();
//#elseif MC == 11701 || MC == 11904
//$$        matrices.push();
//#elseif MC == 12001 || MC == 12101 || MC == 12104
//$$        context.getMatrices().push();
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        context.getMatrices().translate((float)x, (float)y);
//#elseif MC == 11701 || MC == 11904
//$$        matrices.translate((float)x, (float)y, 0.0f);
//#elseif MC == 12001 || MC == 12101 || MC == 12104
//$$        context.getMatrices().translate((float)x, (float)y, 0.0f);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        context.getMatrices().scale(fontScale, fontScale);
//#elseif MC == 11701 || MC == 11904
//$$        matrices.scale(fontScale, fontScale, 1.0f);
//#elseif MC == 12001 || MC == 12101 || MC == 12104
//$$        context.getMatrices().scale(fontScale, fontScale, 1.0f);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        context.drawText(tr, text, 0, 0, color, false);
//#elseif MC == 11701 || MC == 11904
//$$        tr.draw(matrices, text, 0, 0, color);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        context.getMatrices().popMatrix();
//#elseif MC == 11701 || MC == 11904
//$$        matrices.pop();
//#elseif MC == 12001 || MC == 12101 || MC == 12104
//$$        context.getMatrices().pop();
//#endif
    }

    /** 绘制按 fontScale 缩放的彩色数量（右对齐由调用方给定的 leftX 决定）。 */
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static void drawScaledHudCount(DrawContext context, net.minecraft.client.font.TextRenderer tr,
//#elseif MC == 11701 || MC == 11904
//$$    private static void drawScaledHudCount(MatrixStack matrices, net.minecraft.client.font.TextRenderer tr,
//#endif
            int count, int leftX, int y, float fontScale) {
        if (fontScale == 1.0f) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawHudCount(context, tr, count, leftX, y);
//#elseif MC == 11701 || MC == 11904
//$$            drawHudCount(matrices, tr, count, leftX, y);
//#endif
            return;
        }
//#if MC == 12108 || MC == 12110 || MC == 12111
        context.getMatrices().pushMatrix();
//#elseif MC == 11701 || MC == 11904
//$$        matrices.push();
//#elseif MC == 12001 || MC == 12101 || MC == 12104
//$$        context.getMatrices().push();
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        context.getMatrices().translate((float)leftX, (float)y);
//#elseif MC == 11701 || MC == 11904
//$$        matrices.translate((float)leftX, (float)y, 0.0f);
//#elseif MC == 12001 || MC == 12101 || MC == 12104
//$$        context.getMatrices().translate((float)leftX, (float)y, 0.0f);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        context.getMatrices().scale(fontScale, fontScale);
//#elseif MC == 11701 || MC == 11904
//$$        matrices.scale(fontScale, fontScale, 1.0f);
//#elseif MC == 12001 || MC == 12101 || MC == 12104
//$$        context.getMatrices().scale(fontScale, fontScale, 1.0f);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawHudCount(context, tr, count, 0, 0);
//#elseif MC == 11701 || MC == 11904
//$$        drawHudCount(matrices, tr, count, 0, 0);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        context.getMatrices().popMatrix();
//#elseif MC == 11701 || MC == 11904
//$$        matrices.pop();
//#elseif MC == 12001 || MC == 12101 || MC == 12104
//$$        context.getMatrices().pop();
//#endif
    }

    /** 缩放绘制物品图标（仅在 scale != 1.0 时使用） */
//#if MC == 11701
//$$    private static void drawScaledItem(MatrixStack matrices, ItemStack stack, int x, int y, float scale) {
//$$        // 1.17.1 的 ItemRenderer 使用 RenderSystem 的模型视图栈，必须在全局栈上做位移与缩放，
//$$        // 否则传入的 MatrixStack 不会生效，图标会被画到 (0,0) 而看似未渲染
//$$        MatrixStack modelView = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
//$$        modelView.push();
//$$        modelView.translate((float)x, (float)y, 0.0f);
//$$        modelView.scale(scale, scale, 1.0f);
//$$        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
//$$        MinecraftClient.getInstance().getItemRenderer().renderGuiItemIcon(stack, 0, 0);
//$$        modelView.pop();
//$$        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
//$$    }
//#elseif MC == 11904
//$$    private static void drawScaledItem(MatrixStack matrices, ItemStack stack, int x, int y, float scale) {
//$$        matrices.push();
//$$        matrices.translate((float)x, (float)y, 0.0f);
//$$        matrices.scale(scale, scale, 1.0f);
//$$        MinecraftClient.getInstance().getItemRenderer().renderInGui(matrices, stack, 0, 0);
//$$        matrices.pop();
//$$    }
//#elseif MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static void drawScaledItem(DrawContext context, ItemStack stack, int x, int y, float scale) {
//#if MC == 12108 || MC == 12110 || MC == 12111
        context.getMatrices().pushMatrix();
//#elseif MC == 12001 || MC == 12101 || MC == 12104
//$$        context.getMatrices().push();
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        context.getMatrices().translate((float)x, (float)y);
//#elseif MC == 12001 || MC == 12101 || MC == 12104
//$$        context.getMatrices().translate((float)x, (float)y, 0.0f);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        context.getMatrices().scale(scale, scale);
//#elseif MC == 12001 || MC == 12101 || MC == 12104
//$$        context.getMatrices().scale(scale, scale, 1.0f);
//#endif
        context.drawItem(stack, 0, 0);
//#if MC == 12108 || MC == 12110 || MC == 12111
        context.getMatrices().popMatrix();
//#elseif MC == 12001 || MC == 12101 || MC == 12104
//$$        context.getMatrices().pop();
//#endif
    }
//#endif
}