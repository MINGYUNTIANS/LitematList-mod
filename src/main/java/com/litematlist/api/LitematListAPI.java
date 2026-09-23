package com.litematlist.api;

import com.litematlist.LitematicReader;
import com.litematlist.MaterialListImporter;
import com.litematlist.gui.MaterialDetailScreen;
import com.litematlist.gui.MaterialListScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 供其他模组接入的公开 API。
 * <p>
 * 使用方式：直接调用静态方法即可获取当前上传区域中的材料列表。
 * <pre>{@code
 *   List<LitematListAPI.MaterialItem> list = LitematListAPI.getMaterialList();
 *   for (LitematListAPI.MaterialItem item : list) {
 *       System.out.println(item.itemStack().getName().getString()
 *           + " 需要 " + item.totalCount() + " 还缺 " + item.missingCount());
 *   }
 * }</pre>
 */
public class LitematListAPI {

    /**
     * API 返回的材料条目：包含物品堆、总需求量和缺失数量。
     */
    public record MaterialItem(ItemStack itemStack, int totalCount, int missingCount) {
        /** 已有数量 = 总需求 - 缺失 */
        public int availableCount() {
            return totalCount - missingCount;
        }
    }

    /**
     * 获取当前上传区域中的材料列表。
     * 自动处理替换、忽略、项目文件夹聚合，并计算玩家背包中的缺失数量。
     *
     * @return 材料列表（可能为空列表），如果上传区域为空则返回空列表
     */
    public static List<MaterialItem> getMaterialList() {
        MaterialListScreen.LoadedEntry uploaded = MaterialListScreen.getUploadedEntry();
        if (uploaded == null) return Collections.emptyList();

        List<LitematicReader.MaterialEntry> materials;
        if (uploaded.isProject()) {
            materials = getProjectMaterials(uploaded);
        } else {
            materials = getEntryMaterials(uploaded);
        }

        if (materials == null || materials.isEmpty()) return Collections.emptyList();

        // 扫描玩家背包
        Map<Item, Integer> inventory = scanPlayerInventory();

        // 应用替换/忽略，计算缺失
        List<MaterialItem> result = new ArrayList<>();
        for (LitematicReader.MaterialEntry m : materials) {
            Item item = m.item();
            if (uploaded.filePath() != null) {
                Item replaced = MaterialDetailScreen.getReplacement(uploaded.filePath(), item);
                if (replaced != item) item = replaced;
            }
            if (uploaded.filePath() != null
                    && MaterialDetailScreen.getIgnoredForPath(uploaded.filePath()).contains(item)) {
                continue;
            }
            int total = m.totalCount();
            int have = inventory.getOrDefault(item, 0);
            int missing = Math.max(0, total - have);
            ItemStack stack = new ItemStack(item != null ? item : Items.AIR);
            if (stack.isEmpty()) continue;
            result.add(new MaterialItem(stack, total, missing));
        }
        return result;
    }

    /**
     * 检查上传区域是否有材料列表可用。
     */
    public static boolean isAvailable() {
        return MaterialListScreen.getUploadedEntry() != null;
    }

    /**
     * 获取当前上传材料列表的名称。
     *
     * @return 列表名称，如果无上传列表则返回 null
     */
    public static String getMaterialListName() {
        MaterialListScreen.LoadedEntry uploaded = MaterialListScreen.getUploadedEntry();
        return uploaded != null ? uploaded.name() : null;
    }

    // ==================== 内部实现 ====================

    private static List<LitematicReader.MaterialEntry> getEntryMaterials(MaterialListScreen.LoadedEntry entry) {
        if (entry.filePath() == null) return null;
        return loadMaterialsCached(entry.filePath());
    }

    private static List<LitematicReader.MaterialEntry> getProjectMaterials(MaterialListScreen.LoadedEntry projectEntry) {
        List<MaterialListScreen.LoadedEntry> children = MaterialListScreen.getProjectEntries(projectEntry.name());
        if (children.isEmpty()) return null;

        Map<Item, Integer> itemTotals = new HashMap<>();
        Map<Item, String> itemNames = new HashMap<>();

        for (MaterialListScreen.LoadedEntry child : children) {
            if (child.filePath() == null) continue;
            List<LitematicReader.MaterialEntry> childMaterials = loadMaterialsCached(child.filePath());
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

        if (itemTotals.isEmpty()) return null;
        return itemTotals.entrySet().stream()
                .map(e -> new LitematicReader.MaterialEntry(e.getKey(),
                        itemNames.getOrDefault(e.getKey(), ""), e.getValue()))
                .collect(Collectors.toList());
    }

    private static List<LitematicReader.MaterialEntry> loadMaterialsCached(Path filePath) {
        List<LitematicReader.MaterialEntry> materials = MaterialDetailScreen.getImportedMaterials().get(filePath);
        if (materials != null && !materials.isEmpty()) return materials;

        String fileName = filePath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".litematic")) {
            return LitematicReader.loadMaterialList(filePath);
        }
        MaterialListImporter.ImportResult result = MaterialListImporter.importFromFile(filePath);
        return result != null ? result.materials() : null;
    }

    private static Map<Item, Integer> scanPlayerInventory() {
        MinecraftClient client = MinecraftClient.getInstance();
        Map<Item, Integer> inv = new HashMap<>();
        if (client.player == null) return inv;
        for (int i = 0; i < client.player.getInventory().size(); i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            inv.merge(stack.getItem(), stack.getCount(), Integer::sum);
            if (fi.dy.masa.malilib.util.InventoryUtils.shulkerBoxHasItems(stack)) {
                for (ItemStack boxStack : fi.dy.masa.malilib.util.InventoryUtils.getStoredItems(stack)) {
                    if (!boxStack.isEmpty()) {
                        inv.merge(boxStack.getItem(), boxStack.getCount(), Integer::sum);
                    }
                }
            }
        }
        return inv;
    }
}