package com.litematlist.gui;

import com.litematlist.LitematicReader;
import com.litematlist.LitematListMod;
import com.litematlist.MaterialListImporter;
import com.litematlist.MaterialListInjector;
import net.minecraft.client.Minecraft;
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
        int maxLines = MaterialDetailScreen.persistedHudMaxLines;
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
    private static int cachedFontSize = -1, cachedMaxLines = -1, cachedBgAlpha = -1;
    private static int cachedCurrentPage = -1;
    private static float cachedScale = 1.0f;
    private static int cachedCountMaxW = 0;
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
        int maxLines = MaterialDetailScreen.persistedHudMaxLines;
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

        String sortName = MaterialDetailScreen.persistedDetailSortMode;
        boolean hideNoMissing = MaterialDetailScreen.persistedHideNoMissing;
        Set<Item> ignored = uploaded.filePath() != null
                ? MaterialDetailScreen.getIgnoredForPath(uploaded.filePath())
                : new HashSet<>();
        Set<Item> pinned = uploaded.filePath() != null
                ? MaterialDetailScreen.getPinnedForPath(uploaded.filePath())
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

        // 构建条目（使用 lastKnownInventory 计算初始 missing）
        List<HudItem> allItems = new ArrayList<>();
        for (LitematicReader.MaterialEntry m : materials) {
            Item item = m.item();
            if (uploaded.filePath() != null) {
                Item replaced = MaterialDetailScreen.getReplacement(uploaded.filePath(), item);
                if (replaced != item) item = replaced;
            }
            if (ignored.contains(item)) continue;
            int total = m.totalCount();
            int have = lastKnownInventory.getOrDefault(item, 0);
            int missing = Math.max(0, total - have);
            if (hideNoMissing && missing == 0) continue;
            ItemStack stack = new ItemStack(item != null ? item : Items.AIR);
            if (stack.isEmpty()) continue;
            String name = getCachedName(item, m);
            allItems.add(new HudItem(stack, name, total, missing));
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

        int maxLines = MaterialDetailScreen.persistedHudMaxLines;
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
        int fontSize = Math.max(1, Math.min(100, MaterialDetailScreen.persistedHudFontSize));
        int maxLines = Math.max(1, Math.min(20, MaterialDetailScreen.persistedHudMaxLines));
        int bgAlpha = Math.max(0, Math.min(100, MaterialDetailScreen.persistedHudBgAlpha));
        float scale = fontSize / 10.0f;

        int startIdx = currentPage * maxLines;
        int endIdx = Math.min(startIdx + maxLines, hudItems.size());

        if (endIdx <= startIdx) {
            cachedTotalW = 0; cachedTotalH = 0;
            cachedLineHeight = 0; cachedIconSize = 0;
            cachedPageText = "";
            cachedMaxNameW = 0;
            cachedCountMaxW = 0;
            cachedShowBox = MaterialDetailScreen.persistedHudShowBox;
            cachedShowGroup = MaterialDetailScreen.persistedHudShowGroup;
            return;
        }

        // 全部行都参与宽度计算（含 missing==0 的行，修复箱子/工作台等不缺物品时不显示的问题）
        int maxNameW = 0;
        int maxCountW = 0;
        for (int i = startIdx; i < endIdx; i++) {
            int w = (int)(client.font.width(hudItems.get(i).name) * scale);
            if (w > maxNameW) maxNameW = w;
            String countText = formatHudCount(hudItems.get(i).missing);
            int cw = (int)(client.font.width(countText) * scale);
            if (cw > maxCountW) maxCountW = cw;
        }

        int lineHeight = (int)(10 * scale);
        int iconSize = lineHeight;

        int totalW = (int)(iconSize + 4 + maxNameW + 4 + maxCountW + 8);
        // 优化 #2：HUD 高度固定 = 字体大小 × 写入行数（无论实际有几行可见）
        int totalH = maxLines * lineHeight + 4;

        String pageText = (startIdx + 1) + "-" + endIdx + "/" + hudItems.size();
        int pageW = (int)(client.font.width(pageText) * scale);
        int pageH = (int)(8 * scale);
        totalH += pageH + 2;

        cachedTotalW = totalW;
        cachedTotalH = totalH;
        cachedLineHeight = lineHeight;
        cachedIconSize = iconSize;
        cachedPageW = pageW;
        cachedPageText = pageText;
        cachedMaxNameW = maxNameW;
        cachedCountMaxW = maxCountW;
        cachedFontSize = fontSize;
        cachedMaxLines = maxLines;
        cachedBgAlpha = bgAlpha;
        cachedCurrentPage = currentPage;
        cachedScale = scale;
        cachedShowBox = MaterialDetailScreen.persistedHudShowBox;
        cachedShowGroup = MaterialDetailScreen.persistedHudShowGroup;
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

    /**
     * 格式化 HUD 中显示的数量：按配置追加盒(1728)/组(64)量级换算，两位小数。
     * 例如 1730 且双开关开启 → "1730(1.00盒,27.03组)"；只开盒 → "1730(1.00盒)；只开组 → "1730(27.03组)"。
     */
    public static String formatHudCount(int count) {
        if (count <= 0) return String.valueOf(count);
        boolean showBox = MaterialDetailScreen.persistedHudShowBox;
        boolean showGroup = MaterialDetailScreen.persistedHudShowGroup;
        if (!showBox && !showGroup) return String.valueOf(count);
        StringBuilder sb = new StringBuilder(String.valueOf(count)).append('(');
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
        int mainColor = count > 0 ? 0xFFFF5555 : 0xFF55FF55;
        String countStr = String.valueOf(count);
        if (count <= 0 || (!MaterialDetailScreen.persistedHudShowBox && !MaterialDetailScreen.persistedHudShowGroup)) {
            context.drawString(client.font, countStr, leftX, y, mainColor);
            return;
        }

        boolean showBox = MaterialDetailScreen.persistedHudShowBox;
        boolean showGroup = MaterialDetailScreen.persistedHudShowGroup;
        int cx = leftX;

        context.drawString(client.font, countStr, cx, y, mainColor);
        cx += client.font.width(countStr);

        cx = appendColored(context, "(", cx, y, 0xFFFFFFFF);
        if (showBox) {
            cx = appendColored(context, String.format(Locale.ROOT, "%.2f%s",
                    count / 1728.0, com.litematlist.I18n.tr("litematlist.hud.box_unit")), cx, y, 0xFF9542C1);
        }
        if (showBox && showGroup) {
            cx = appendColored(context, ",", cx, y, 0xFFFFFFFF);
        }
        if (showGroup) {
            cx = appendColored(context, String.format(Locale.ROOT, "%.2f%s",
                    count / 64.0, com.litematlist.I18n.tr("litematlist.hud.group_unit")), cx, y, 0xFFFFCA3A);
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
                Item replaced = MaterialDetailScreen.getReplacement(filePath, item);
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

        List<LitematicReader.MaterialEntry> cached = MaterialDetailScreen.getImportedMaterials().get(filePath);
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

        for (MaterialListScreen.LoadedEntry child : children) {
            if (child.filePath() == null) continue;
            List<LitematicReader.MaterialEntry> childMaterials = getEntryMaterials(child);
            if (childMaterials == null) continue;

            for (LitematicReader.MaterialEntry m : childMaterials) {
                Item item = m.item();
                Item replaced = MaterialDetailScreen.getReplacement(child.filePath(), item);
                if (replaced != item) item = replaced;
                if (MaterialDetailScreen.getIgnoredForPath(child.filePath()).contains(item)) continue;
                itemTotals.merge(item, m.totalCount(), Integer::sum);
                itemNames.putIfAbsent(item, m.blockName());
            }
        }

        List<LitematicReader.MaterialEntry> result = new ArrayList<>();
        for (Map.Entry<Item, Integer> e : itemTotals.entrySet()) {
            result.add(new LitematicReader.MaterialEntry(e.getKey(), itemNames.getOrDefault(e.getKey(), "?"), e.getValue()));
        }
        result.sort((a, b) -> Integer.compare(b.totalCount(), a.totalCount()));
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
        if (!MaterialDetailScreen.persistedHudVisible) return;
        if (Minecraft.getInstance().gui.hud.isHidden()) return;
        if (hudItems.isEmpty()) return;

        GuiContext ctx = GuiContext.fromGuiGraphics(gfx);

        int x = MaterialDetailScreen.persistedHudX;
        int y = MaterialDetailScreen.persistedHudY;

        // 检查是否需要重新计算尺寸（配置变化或翻页）
        int fontSize = Math.max(1, Math.min(100, MaterialDetailScreen.persistedHudFontSize));
        int maxLines = Math.max(1, Math.min(20, MaterialDetailScreen.persistedHudMaxLines));
        int bgAlpha = Math.max(0, Math.min(100, MaterialDetailScreen.persistedHudBgAlpha));
        if (fontSize != cachedFontSize || maxLines != cachedMaxLines
                || bgAlpha != cachedBgAlpha || currentPage != cachedCurrentPage
                || MaterialDetailScreen.persistedHudShowBox != cachedShowBox
                || MaterialDetailScreen.persistedHudShowGroup != cachedShowGroup) {
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

        // 优化 #1：图标始终等比缩放到与文字同高
        // 优化 #6：无缩放时跳过矩阵 push/pop（最快路径）
        // 所有行都渲染（含 missing==0），修复箱子/工作台等不缺物品时不显示的问题
        if (itemScale == 1.0f) {
            int visibleLine = 0;
            for (int i = startIdx; i < endIdx; i++) {
                HudItem hi = hudItems.get(i);
                int lineY = y + 2 + visibleLine * cachedLineHeight;
                visibleLine++;

                gfx.item(hi.stack, x + 2, lineY);

                ctx.drawString(client.font, hi.name,
                        x + 2 + cachedIconSize + 4, lineY + 1, 0xFFFFFFFF);

                drawHudCount(ctx, hi.missing, x + cachedTotalW - 8 - cachedCountMaxW, lineY + 1);
            }
        } else {
            int visibleLine = 0;
            for (int i = startIdx; i < endIdx; i++) {
                HudItem hi = hudItems.get(i);
                int lineY = y + 2 + visibleLine * cachedLineHeight;
                visibleLine++;

                drawScaledItem(gfx, hi.stack, x + 2, lineY, itemScale);

                ctx.drawString(client.font, hi.name,
                        (int)(x + 2 + cachedIconSize + 4), lineY + 1, 0xFFFFFFFF);

                drawHudCount(ctx, hi.missing, x + cachedTotalW - 8 - cachedCountMaxW, lineY + 1);
            }
        }

        // 页码
        int pageY = y + 2 + maxLines * cachedLineHeight + 2;
        ctx.drawString(client.font, cachedPageText,
                x + (cachedTotalW - cachedPageW) / 2, pageY, 0xFFAAAAAA);
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