package com.litematlist.gui;

import com.litematlist.LitematicReader;
import com.litematlist.LitematListMod;
import com.litematlist.MaterialListImporter;
import com.litematlist.MaterialListInjector;
import com.litematlist.config.Configs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import fi.dy.masa.malilib.render.GuiContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;

import java.nio.file.Path;
import java.util.*;

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
            new ItemStack(item).getHoverName().getString());
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
        cachedBgAlpha = -1;
    }

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
            int id = BuiltInRegistries.ITEM.getId(entry.getKey());
            hash = hash * 31 + id * 31 + entry.getValue();
        }
        return hash;
    }

    /**
     * 轻量物品栏哈希：仅扫描 36 个原始槽位，不展开潜影盒。
     * 用于更新前的快速判断，避免在物品栏未变化时执行完整扫描。
     */
    private static int computeLightInventoryHash() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return 0;
        int hash = 0;
        for (int i = 0; i < client.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            int id = BuiltInRegistries.ITEM.getId(stack.getItem());
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

    /**
     * 优化 #3：预计算 HUD 渲染所需的所有尺寸，避免每帧重复计算。
     * 包级可见：HudConfigScreen 切换盒/组开关时直接调用。
     */
    static void recomputeCachedDimensions() {
        Minecraft client = Minecraft.getInstance();
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
                        rowW += measureHudSegment(segment, hi, client.font, scale, iconSize);
                    }
                    if (rowW > maxRowW) maxRowW = rowW;
                }
            } else {
                // 前缀与各 |...| 片段：记录当前页的最大宽度作为对齐基准
                for (int i = startIdx; i < endIdx; i++) {
                    HudItem hi = hudItems.get(i);
                    int prefixW = measureHudSegment(formatSegments.get(0), hi, client.font, scale, iconSize);
                    if (prefixW > prefixMaxW) prefixMaxW = prefixW;
                    for (int s = 1; s < segCount - 1; s++) {
                        int w = measureHudSegment(formatSegments.get(s), hi, client.font, scale, iconSize);
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
                                ? measureHudSegment(formatSegments.get(s), hi, client.font, scale, iconSize)
                                : pipedMaxW[s];
                    }
                    rowW += measureHudSegment(formatSegments.get(segCount - 1), hi, client.font, scale, iconSize);
                    if (rowW > maxRowW) maxRowW = rowW;
                }
            }
        } else {
            for (int i = startIdx; i < endIdx; i++) {
                int w = (int)(client.font.width(hudItems.get(i).name) * scale);
                if (w > maxNameW) maxNameW = w;
                int nw = (int)(client.font.width(String.valueOf(hudItems.get(i).missing)) * scale);
                if (nw > maxNumW) maxNumW = nw;
                int sw = (int)(client.font.width(formatHudCountSuffix(hudItems.get(i).missing)) * scale);
                if (sw > maxSuffixW) maxSuffixW = sw;
            }
        }
        int maxCountW = maxNumW + maxSuffixW;

        // 优化 #2：HUD 高度固定 = 字体大小 × 写入行数（无论实际有几行可见）
        int totalH = maxLines * lineHeight + 4;

        String pageText = (startIdx + 1) + "-" + endIdx + "/" + hudItems.size();
        int pageW = (int)(client.font.width(pageText) * scale);
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
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getWindow() == null) return MaterialDataStore.persistedHudX;
        int sw = client.getWindow().getGuiScaledWidth();
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
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getWindow() == null) return MaterialDataStore.persistedHudY;
        int sh = client.getWindow().getGuiScaledHeight();
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

    /**
     * 格式化 HUD 中显示的数量：按配置追加盒(1728)/组(64)量级换算，两位小数。
     * 例如 1730 且双开关开启 → "1730(1.00盒,27.03组)"；只开盒 → "1730(1.00盒)；只开组 → "1730(27.03组)"。
     */
    public static String formatHudCount(int count) {
        return String.valueOf(count) + formatHudCountSuffix(count);
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
    private static int measureHudSegment(HudFormatSegment segment, HudItem item, Font font,
            float scale, int iconSize) {
        int width = 0;
        boolean showBox = MaterialDataStore.persistedHudShowBox;
        boolean showGroup = MaterialDataStore.persistedHudShowGroup;
        for (HudFormatToken token : segment.tokens()) {
            switch (token.type()) {
                case ICON -> width += iconSize;
                case NAME -> width += (int)(font.width(item.name) * scale);
                case ITEM -> width += (int)(font.width(String.valueOf(item.missing)) * scale);
                case BOX -> {
                    if (showBox && item.missing > 0) {
                        width += (int)(font.width(formatBoxValue(item.missing)) * scale);
                    }
                }
                case STACK -> {
                    if (showGroup && item.missing > 0) {
                        width += (int)(font.width(formatStackValue(item.missing)) * scale);
                    }
                }
                case LITERAL -> width += (int)(font.width(token.text()) * scale);
            }
        }
        return width;
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

    /**
     * 按颜色分段绘制 HUD 数量（整列左对齐，绿"0"与红色数字同起点）：
     * 主数量保持红/绿，"(x盒,y组)"后缀中括号与逗号为白色、盒数为 #9542C1、组数为 #FFCA3A。
     */
    private static void drawHudCount(GuiContext context, int count, int leftX, int y) {
        Minecraft client = Minecraft.getInstance();
        int mainColor = count > 0 ? Configs.Colors.HUD_MISSING_COUNT.getIntegerValue() : 0xFF55FF55;
        String countStr = String.valueOf(count);
        if (count <= 0 || (!MaterialDataStore.persistedHudShowBox && !MaterialDataStore.persistedHudShowGroup)) {
            context.drawString(client.font, countStr, leftX, y, mainColor);
            return;
        }

        boolean showBox = MaterialDataStore.persistedHudShowBox;
        boolean showGroup = MaterialDataStore.persistedHudShowGroup;
        int cx = leftX;

        context.drawString(client.font, countStr, cx, y, mainColor);
        cx += client.font.width(countStr);

        cx = appendColored(context, "(", cx, y, 0xFFFFFFFF);
        if (showBox) {
            cx = appendColored(context, String.format(Locale.ROOT, "%.2f%s",
                    count / 1728.0, com.litematlist.I18n.tr("litematlist.hud.box_unit")), cx, y,
                    Configs.Colors.HUD_MISSING_BOX.getIntegerValue());
        }
        if (showBox && showGroup) {
            cx = appendColored(context, ",", cx, y, 0xFFFFFFFF);
        }
        if (showGroup) {
            cx = appendColored(context, String.format(Locale.ROOT, "%.2f%s",
                    count / 64.0, com.litematlist.I18n.tr("litematlist.hud.group_unit")), cx, y,
                    Configs.Colors.HUD_MISSING_GROUP.getIntegerValue());
        }
        appendColored(context, ")", cx, y, 0xFFFFFFFF);
    }

    /** 以指定颜色绘制一段文字，并返回下一个片段的起始 X 坐标 */
    private static int appendColored(GuiContext context, String text, int x, int y, int color) {
        Minecraft client = Minecraft.getInstance();
        context.drawString(client.font, text, x, y, color);
        return x + client.font.width(text);
    }

    /**
     * 扫描背包一次（无缓存，由 updateMissingCounts 调用）。
     * 优化 #2：shulkerBoxHasItems 检查确保空潜影盒不被遍历。
     */
    private static Map<Item, Integer> scanInventory() {
        Minecraft client = Minecraft.getInstance();
        Map<Item, Integer> playerInventory = new HashMap<>();
        if (client.player != null) {
            for (int i = 0; i < client.player.getInventory().getContainerSize(); i++) {
                ItemStack stack = client.player.getInventory().getItem(i);
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
        sorted.sort(Comparator.comparing(m -> BuiltInRegistries.ITEM.getKey(m.item()).toString()));
        for (LitematicReader.MaterialEntry m : sorted) {
            Item item = m.item();
            if (filePath != null) {
                Item replaced = MaterialDataStore.getReplacement(filePath, item);
                if (replaced != item) item = replaced;
            }
            if (ignored.contains(item)) continue;
            sb.append(BuiltInRegistries.ITEM.getKey(item)).append(":")
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
    public static void render(GuiGraphicsExtractor gfx) {
        if (!MaterialDataStore.persistedHudVisible) return;
        if (Minecraft.getInstance().gui.hud.isHidden()) return;
        if (hudItems.isEmpty()) return;

        GuiContext ctx = GuiContext.fromGuiGraphics(gfx);

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

        Minecraft client = Minecraft.getInstance();

        // 优化 #4：背景（fill 是单个 GL 矩形调用，极快，尺寸已预计算）
        int bgColor = (bgAlpha * 255 / 100) << 24 | 0x000000;
        ctx.fill(x, y, x + cachedTotalW, y + cachedTotalH, bgColor);

        float itemScale = cachedIconSize / 16.0f;
        float fontScale = fontSize / 10.0f;

        // 图标等比缩放到与文字同高；文字按 fontScale 缩放，使「字号设置」真正生效，
        // 并继承游戏 GUI 缩放（HUD 绘制在 GUI 缩放坐标系中）→ 实际大小 = UI大小 × 字号倍率。
        // 优化 #4：所有行都渲染（含 missing==0），visibleLine 独立计数
        int visibleLine = 0;
        for (int i = startIdx; i < endIdx; i++) {
            HudItem hi = hudItems.get(i);
            int lineY = y + 2 + visibleLine * cachedLineHeight;
            visibleLine++;
            if (cachedCustomFormat) {
                drawCustomHudRow(gfx, ctx, client.font, hi, x + 2, lineY + 1, fontScale);
                continue;
            }

            if (itemScale == 1.0f) {
                gfx.item(hi.stack, x + 2, lineY);
            } else {
                drawScaledItem(gfx, hi.stack, x + 2, lineY, itemScale);
            }

            drawScaledText(gfx, ctx, client.font, hi.name,
                    x + 2 + cachedIconSize + 4, lineY + 1, 0xFFFFFFFF, fontScale);

            // 数量起始 X：默认整列按最宽数量左对齐（保持原行为）；
            // 右侧锚点时红字统一右对齐到「HUD 右缘 - 盒/组最大宽度」，盒/组紧跟在红字右侧
            int countLeftX = x + cachedTotalW - 8 - cachedCountMaxW;
            int anchor = hudAnchor();
            if (anchor == 2 || anchor == 3) {
                int numW = (int)(client.font.width(String.valueOf(hi.missing)) * fontScale);
                int numberRightX = x + cachedTotalW - 8 - cachedMaxSuffixW;
                countLeftX = numberRightX - numW;
            }
            drawScaledHudCount(gfx, ctx, client.font, hi.missing,
                    countLeftX, lineY + 1, fontScale);
        }

        // 页码
        int pageY = y + 2 + maxLines * cachedLineHeight + 2;
        drawScaledText(gfx, ctx, client.font, cachedPageText,
                x + (cachedTotalW - cachedPageW) / 2, pageY, 0xFFAAAAAA, fontScale);
    }

    /** 自定义格式的整行绘制：只有被两个 | 夹住的片段按 HUD 校准位置居左/居右。 */
    private static void drawCustomHudRow(GuiGraphicsExtractor gfx, GuiContext ctx, Font font,
            HudItem item, int startX, int textY, float fontScale) {
        float itemScale = cachedIconSize / 16.0f;
        boolean showBox = MaterialDataStore.persistedHudShowBox;
        boolean showGroup = MaterialDataStore.persistedHudShowGroup;
        int countColor = item.missing > 0 ? Configs.Colors.HUD_MISSING_COUNT.getIntegerValue() : 0xFF55FF55;
        boolean rightAligned = hudAnchor() == 2 || hudAnchor() == 3;
        int segCount = cachedFormatSegments.size();

        if (segCount <= 2) {
            // 没有 | 或只有一个 |：不存在被两个 | 夹住的片段，所有内容按自然宽度依次绘制
            int cursorX = startX;
            for (HudFormatSegment segment : cachedFormatSegments) {
                cursorX = drawHudSegmentTokens(gfx, ctx, font, item, segment, cursorX, textY, fontScale,
                        itemScale, showBox, showGroup, countColor);
            }
            return;
        }

        // 第一个 | 之前：内容左对齐，列宽取本页最大值作为对齐基准
        drawHudSegmentTokens(gfx, ctx, font, item, cachedFormatSegments.get(0), startX, textY, fontScale,
                itemScale, showBox, showGroup, countColor);
        int cursorX = startX + cachedPrefixMaxW;
        int lastPiped = segCount - 2;
        for (int s = 1; s <= lastPiped; s++) {
            HudFormatSegment segment = cachedFormatSegments.get(s);
            int segMaxW = s < cachedPipedMaxW.length ? cachedPipedMaxW[s] : 0;
            int segW = measureHudSegment(segment, item, font, fontScale, cachedIconSize);
            int segX = rightAligned ? cursorX + segMaxW - segW : cursorX;
            drawHudSegmentTokens(gfx, ctx, font, item, segment, segX, textY, fontScale,
                    itemScale, showBox, showGroup, countColor);
            // 左侧锚点：最后一段之后按自然宽度继续，| 后的文字不会被人为分散对齐
            cursorX += (s == lastPiped && !rightAligned) ? segW : segMaxW;
        }

        // 最后一个 | 之后：紧跟对齐片段，按自然宽度绘制
        drawHudSegmentTokens(gfx, ctx, font, item, cachedFormatSegments.get(segCount - 1), cursorX, textY,
                fontScale, itemScale, showBox, showGroup, countColor);
    }

    /** 绘制一个格式片段内的所有变量与文字，返回绘制后的 X 坐标。 */
    private static int drawHudSegmentTokens(GuiGraphicsExtractor gfx, GuiContext ctx, Font font,
            HudItem item, HudFormatSegment segment, int cursorX, int textY, float fontScale,
            float itemScale, boolean showBox, boolean showGroup, int countColor) {
        int iconY = textY - 1;
        for (HudFormatToken token : segment.tokens()) {
            switch (token.type()) {
                case ICON -> {
                    if (itemScale == 1.0f) {
                        gfx.item(item.stack, cursorX, iconY);
                    } else {
                        drawScaledItem(gfx, item.stack, cursorX, iconY, itemScale);
                    }
                    cursorX += cachedIconSize;
                }
                case NAME -> {
                    String text = item.name;
                    drawScaledText(gfx, ctx, font, text, cursorX, textY, 0xFFFFFFFF, fontScale);
                    cursorX += (int)(font.width(text) * fontScale);
                }
                case ITEM -> {
                    String text = String.valueOf(item.missing);
                    drawScaledText(gfx, ctx, font, text, cursorX, textY, countColor, fontScale);
                    cursorX += (int)(font.width(text) * fontScale);
                }
                case BOX -> {
                    if (showBox && item.missing > 0) {
                        String text = formatBoxValue(item.missing);
                        drawScaledText(gfx, ctx, font, text, cursorX, textY,
                                Configs.Colors.HUD_MISSING_BOX.getIntegerValue(), fontScale);
                        cursorX += (int)(font.width(text) * fontScale);
                    }
                }
                case STACK -> {
                    if (showGroup && item.missing > 0) {
                        String text = formatStackValue(item.missing);
                        drawScaledText(gfx, ctx, font, text, cursorX, textY,
                                Configs.Colors.HUD_MISSING_GROUP.getIntegerValue(), fontScale);
                        cursorX += (int)(font.width(text) * fontScale);
                    }
                }
                case LITERAL -> {
                    if (token.text().isEmpty() == false) {
                        drawScaledText(gfx, ctx, font, token.text(), cursorX, textY, 0xFFFFFFFF, fontScale);
                        cursorX += (int)(font.width(token.text()) * fontScale);
                    }
                }
            }
        }
        return cursorX;
    }

    /** 绘制按 fontScale 缩放的文字（fontScale==1 时走最快路径）。 */
    private static void drawScaledText(GuiGraphicsExtractor gfx, GuiContext ctx, Font font,
            String text, int x, int y, int color, float fontScale) {
        if (fontScale == 1.0f) {
            ctx.drawString(font, text, x, y, color);
            return;
        }
        gfx.pose().pushMatrix();
        gfx.pose().translate((float)x, (float)y);
        gfx.pose().scale(fontScale, fontScale);
        ctx.drawString(font, text, 0, 0, color);
        gfx.pose().popMatrix();
    }

    /** 绘制按 fontScale 缩放的彩色数量（对齐由调用方给定的 leftX 决定）。 */
    private static void drawScaledHudCount(GuiGraphicsExtractor gfx, GuiContext ctx, Font font,
            int count, int leftX, int y, float fontScale) {
        if (fontScale == 1.0f) {
            drawHudCount(ctx, count, leftX, y);
            return;
        }
        gfx.pose().pushMatrix();
        gfx.pose().translate((float)leftX, (float)y);
        gfx.pose().scale(fontScale, fontScale);
        drawHudCount(ctx, count, 0, 0);
        gfx.pose().popMatrix();
    }

    /** 缩放绘制物品图标（仅在 scale != 1.0 时使用） */
    private static void drawScaledItem(GuiGraphicsExtractor gfx, ItemStack stack, int x, int y, float scale) {
        gfx.pose().pushMatrix();
        gfx.pose().translate((float)x, (float)y);
        gfx.pose().scale(scale, scale);
        gfx.item(stack, 0, 0);
        gfx.pose().popMatrix();
    }
}
