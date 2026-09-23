package com.litematlist;

import com.litematlist.gui.MaterialDetailScreen;
import com.litematlist.gui.MaterialListScreen;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 将本模组的材料列表转译为 Litematica 的 MaterialListBase 格式，
 * 并注入到 DataManager 中供 TweakerMore 读取。
 */
public class MaterialListInjector {

    private static String lastInjectedPath = null;
    private static long lastInjectTime = 0;
    private static long lastReinjectTime = 0;
    private static long lastFailLogTime = 0;
    private static final long INJECT_COOLDOWN = 5000; // 5秒冷却

    // .litematic 文件解析缓存，避免每 5 秒重新解析大原理图导致卡顿
    private static final Map<Path, List<LitematicReader.MaterialEntry>> materialCache = new HashMap<>();
    private static final Map<Path, Long> fileLastModified = new HashMap<>();
    // 各文件最近一次解析使用的层级过滤签名（与 materialCache 对应）
    private static final Map<Path, String> fileFilterSig = new HashMap<>();
    private static final long FAIL_LOG_COOLDOWN = 30000; // 失败日志30秒冷却，防止日志爆炸

    /**
     * 将「上传区域」中的条目注入到 Litematica 的 DataManager。
     * 如果上传区域为空，则根据配置决定是否接管。
     */
    public static void injectIfNeeded() {
        MaterialListScreen.LoadedEntry uploaded = MaterialListScreen.getUploadedEntry();

        if (uploaded != null) {
            String pathKey = uploaded.isProject() ? ("project:" + uploaded.name()) :
                    uploaded.filePath() != null ? uploaded.filePath().toString() : uploaded.name();
            if (pathKey == null) return;

            // 检查当前 DataManager 中的材料列表是否已被覆盖（非我们的注入）
            MaterialListBase current = DataManager.getMaterialList();
            boolean needsReinject = !(current instanceof InjectedMaterialList);

            // 避免重复注入（相同路径且未被覆盖时，5秒内跳过）
            if (!needsReinject && pathKey.equals(lastInjectedPath)
                    && System.currentTimeMillis() - lastInjectTime < INJECT_COOLDOWN) {
                return;
            }

            // 重新注入也需要冷却，防止注入失败时高频重试
            boolean isAlwaysTakeOver = com.litematlist.config.Configs.Generic.ALWAYS_TAKE_OVER_MATERIAL_LIST.getBooleanValue();
            if (needsReinject && !isAlwaysTakeOver && System.currentTimeMillis() - lastReinjectTime < INJECT_COOLDOWN) {
                return;
            }

            boolean success = false;
            if (uploaded.isProject()) {
                injectFromProject(uploaded);
                // injectFromProject 在成功时会调用 DataManager.setMaterialList，失败时不会
                success = DataManager.getMaterialList() instanceof InjectedMaterialList;
            } else {
                injectFromEntry(uploaded);
                success = DataManager.getMaterialList() instanceof InjectedMaterialList;
            }

            if (success) {
                lastInjectedPath = pathKey;
                lastInjectTime = System.currentTimeMillis();
            } else {
                lastReinjectTime = System.currentTimeMillis();
            }
        }
    }

    /**
     * 从指定条目加载材料数据并注入到 DataManager。
     */
    /**
     * 带缓存的材料列表加载：优先命中缓存，避免重复解析大原理图。
     * .litematic 文件按「文件修改时间」缓存；txt/json/csv 依赖导入缓存。
     */
    public static List<LitematicReader.MaterialEntry> loadMaterialsCached(Path filePath) {
        if (filePath == null) {
            return null;
        }

        // 1. 优先从 MaterialDetailScreen 的导入缓存获取（txt/json/csv 导入的材料）
        List<LitematicReader.MaterialEntry> materials = MaterialDetailScreen.getImportedMaterials().get(filePath);
        if (materials != null && !materials.isEmpty()) {
            return materials;
        }

        String fileName = filePath.getFileName().toString().toLowerCase();

        // 2. .litematic 文件：按修改时间 + 层级过滤签名缓存解析结果
        if (fileName.endsWith(".litematic")) {
            // 「从投影同步」条目 + 「同步投影渲染层」开启时，按投影当前渲染层规则过滤解析
            LitematicaBridge.LayerFilter filter = null;
            if (isSyncLayerEntry(filePath)) {
                filter = LitematicaBridge.computeRenderLayerFilter(filePath);
            }
            String sig = filter == null ? "all" : filter.signature();
            long mtime = filePath.toFile().lastModified();
            Long cachedMtime = fileLastModified.get(filePath);
            String cachedSig = fileFilterSig.get(filePath);
            List<LitematicReader.MaterialEntry> cached = materialCache.get(filePath);
            if (cached != null && cachedMtime != null && cachedMtime == mtime && sig.equals(cachedSig)) {
                return cached;
            }
            materials = LitematicReader.loadMaterialList(filePath, filter);
            if (materials != null && !materials.isEmpty()) {
                materialCache.put(filePath, materials);
                fileLastModified.put(filePath, mtime);
                fileFilterSig.put(filePath, sig);
            }
            return materials;
        }

        // 3. txt/json/csv 文件导入
        MaterialListImporter.ImportResult result = MaterialListImporter.importFromFile(filePath);
        if (result != null) {
            return result.materials();
        }
        return null;
    }

    public static void injectFromEntry(MaterialListScreen.LoadedEntry entry) {
        if (entry == null || entry.filePath() == null) {
            LitematListMod.LOGGER.warn("[MaterialListInjector] 条目无效，无法注入");
            return;
        }

        Path filePath = entry.filePath();
        List<LitematicReader.MaterialEntry> materials = loadMaterialsCached(filePath);

        if (materials == null || materials.isEmpty()) {
            LitematListMod.LOGGER.warn("[MaterialListInjector] 无法解析材料列表: {}", filePath);
            return;
        }

        // 转译：将 MaterialEntry 转换为 MaterialListEntry
        List<MaterialListEntry> litematicaEntries = translate(materials, filePath);

        // 创建 MaterialListBase 子类并注入
        InjectedMaterialList injectedList = new InjectedMaterialList(entry.name());
        injectedList.setMaterialListEntries(litematicaEntries);

        // 覆写 DataManager 的材料列表
        DataManager.setMaterialList(injectedList);

        // 更新玩家背包中的已有数量
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            MaterialListUtils.updateAvailableCounts(injectedList.getMaterialsAll(), client.player);
        }

        LitematListMod.LOGGER.info("[MaterialListInjector] 已注入材料列表到 DataManager: {} ({} 种物品)",
                entry.name(), litematicaEntries.size());
    }

    /**
     * 判断指定 .litematic 文件是否属于「从投影同步」的材料列表条目。
     * 仅此类条目在「同步投影渲染层」开启时应用渲染层过滤。
     */
    private static boolean isSyncLayerEntry(Path filePath) {
        if (com.litematlist.config.Configs.FeatureToggles.SYNC_RENDER_LAYER.getBooleanValue() == false) {
            return false;
        }
        for (MaterialListScreen.LoadedEntry entry : MaterialListScreen.getLoadedEntries()) {
            if (entry.fromLitematica() && entry.filePath() != null && entry.filePath().equals(filePath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 清空 .litematic 解析缓存（全局解析设置如「是否计算容器数据」切换时调用，
     * 强制下次加载重新解析原理图）。
     */
    public static void invalidateCaches() {
        materialCache.clear();
        fileLastModified.clear();
        fileFilterSig.clear();
        LitematicReader.clearParseCache();
        LitematListMod.LOGGER.info("[MaterialListInjector] 已清空原理图解析缓存");
    }

    /**
     * 清除注入，恢复为 null。
     */
    public static void clearInjection() {
        MaterialListBase current = DataManager.getMaterialList();
        if (current instanceof InjectedMaterialList) {
            DataManager.setMaterialList(null);
            lastInjectedPath = null;
            lastInjectTime = 0;
            LitematListMod.LOGGER.info("[MaterialListInjector] 已清除注入");
        }
    }

    /**
     * 同步注入状态（由 uploadEntry 直接调用后使用，防止 injectIfNeeded 立即重复注入）
     */
    public static void markInjected(MaterialListScreen.LoadedEntry entry) {
        lastInjectedPath = entry.isProject() ? ("project:" + entry.name()) :
                entry.filePath() != null ? entry.filePath().toString() : entry.name();
        lastInjectTime = System.currentTimeMillis();
    }

    /**
     * 将项目文件夹的所有子条目材料聚合后注入到 DataManager。
     */
    public static void injectFromProject(MaterialListScreen.LoadedEntry projectEntry) {
        String projectName = projectEntry.name();
        List<MaterialListScreen.LoadedEntry> children = MaterialListScreen.getProjectEntries(projectName);
        if (children.isEmpty()) {
            LitematListMod.LOGGER.warn("[MaterialListInjector] 项目文件夹为空: {}", projectName);
            return;
        }

        // 聚合所有子条目的材料（按物品合并）
        Map<Item, Integer> itemTotals = new HashMap<>();
        Map<Item, String> itemNames = new HashMap<>();

        for (MaterialListScreen.LoadedEntry child : children) {
            if (child.filePath() == null) continue;
            List<LitematicReader.MaterialEntry> childMaterials = loadMaterialsCached(child.filePath());

            if (childMaterials == null) continue;

            for (LitematicReader.MaterialEntry m : childMaterials) {
                Item item = m.item();
                // 检查替换
                Item replaced = MaterialDetailScreen.getReplacement(child.filePath(), item);
                if (replaced != item) {
                    item = replaced;
                }
                // 检查忽略
                if (MaterialDetailScreen.getIgnoredForPath(child.filePath()).contains(item)) {
                    continue;
                }
                itemTotals.merge(item, m.totalCount(), Integer::sum);
                itemNames.putIfAbsent(item, m.blockName());
            }
        }

        if (itemTotals.isEmpty()) {
            LitematListMod.LOGGER.warn("[MaterialListInjector] 项目无有效材料: {}", projectName);
            return;
        }

        // 转译
        List<MaterialListEntry> litematicaEntries = new ArrayList<>();
        for (Map.Entry<Item, Integer> e : itemTotals.entrySet()) {
            Item item = e.getKey();
            int total = e.getValue();
            ItemStack stack = new ItemStack(item != null ? item : Items.AIR);
            if (stack.isEmpty()) continue;
            litematicaEntries.add(new MaterialListEntry(stack, total, total, 0, 0));
        }

        // 注入
        InjectedMaterialList injectedList = new InjectedMaterialList(projectName);
        injectedList.setMaterialListEntries(litematicaEntries);
        DataManager.setMaterialList(injectedList);

        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            MaterialListUtils.updateAvailableCounts(injectedList.getMaterialsAll(), client.player);
        }

        LitematListMod.LOGGER.info("[MaterialListInjector] 已注入项目材料列表到 DataManager: {} ({} 种物品)",
                projectName, litematicaEntries.size());
    }

    /**
     * 将本模组的 MaterialEntry 列表转译为 Litematica 的 MaterialListEntry 列表。
     */
    private static List<MaterialListEntry> translate(List<LitematicReader.MaterialEntry> materials, Path filePath) {
        List<MaterialListEntry> entries = new ArrayList<>(materials.size());

        for (LitematicReader.MaterialEntry m : materials) {
            Item item = m.item();
            int total = m.totalCount();

            // 检查替换映射
            Item replaced = MaterialDetailScreen.getReplacement(filePath, item);
            if (replaced != item) {
                item = replaced;
            }

            // 检查忽略
            if (MaterialDetailScreen.getIgnoredForPath(filePath).contains(item)) {
                continue;
            }

            ItemStack stack = new ItemStack(item != null ? item : Items.AIR);
            if (stack.isEmpty()) continue;

            // countMissing 设为 total（总需求量），countAvailable 由 updateAvailableCounts 更新
            entries.add(new MaterialListEntry(stack, total, total, 0, 0));
        }

        return entries;
    }

    /**
     * 统计玩家背包中指定物品的数量（包括潜影盒内容）。
     */
    private static int getPlayerItemCount(Item item) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return 0;
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() == item) {
                count += stack.getCount();
            } else if (fi.dy.masa.malilib.util.InventoryUtils.shulkerBoxHasItems(stack)) {
                for (ItemStack boxStack : fi.dy.masa.malilib.util.InventoryUtils.getStoredItems(stack)) {
                    if (boxStack.getItem() == item) {
                        count += boxStack.getCount();
                    }
                }
            }
        }
        return count;
    }

    /**
     * 自定义 MaterialListBase 子类，用于承载本模组注入的材料列表。
     */
    public static class InjectedMaterialList extends MaterialListBase {
        private final String name;

        public InjectedMaterialList(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public String getTitle() {
            return this.name;
        }

        @Override
        public void reCreateMaterialList() {
            // 不需要重新创建，由本模组手动管理
        }
    }
}
