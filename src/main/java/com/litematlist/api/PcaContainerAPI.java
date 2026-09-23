package com.litematlist.api;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.AABB;

import java.util.*;

/**
 * 基于 PCA 协议的容器数据 API。
 * <p>
 * PCA（Portable Container Access）协议由 MasaGadget 实现，用于将服务器端容器数据同步到客户端。
 * 当 MasaGadget 的 PCA 同步启用后，客户端世界的容器对象会持有准确的服务器端数据。
 * 本 API 扫描客户端世界中已加载的容器，聚合其中的物品，供其他备货模组使用。
 * <p>
 * 使用示例：
 * <pre>{@code
 *   // 获取世界中所有容器内的物品
 *   Map<Item, Integer> worldItems = PcaContainerAPI.getAllContainerItems();
 *
 *   // 结合材料列表，计算考虑世界容器后的缺失数量
 *   List<LitematListAPI.MaterialItem> list = PcaContainerAPI.getMaterialListWithWorldItems();
 * }</pre>
 */
public class PcaContainerAPI {

    /**
     * 容器物品信息，包含物品堆、所在容器位置和容器名称。
     */
    public record ContainerItemInfo(ItemStack itemStack, BlockPos pos, String containerName) {}

    // PCA 可用性缓存，避免频繁反射
    private static Boolean pcaAvailableCache = null;
    private static long pcaAvailableCacheTime = 0;
    private static final long PCA_CACHE_TTL = 5000; // 5秒缓存

    /**
     * 检查 MasaGadget 的 PCA 同步协议是否已启用。
     * 通过反射读取 com.plusls.MasaGadget.util.PcaSyncProtocol.enable 字段。
     *
     * @return true 如果 PCA 同步已启用
     */
    public static boolean isPcaAvailable() {
        long now = System.currentTimeMillis();
        if (pcaAvailableCache != null && now - pcaAvailableCacheTime < PCA_CACHE_TTL) {
            return pcaAvailableCache;
        }
        try {
            Class<?> pcaClass = Class.forName("com.plusls.MasaGadget.util.PcaSyncProtocol");
            java.lang.reflect.Field enableField = pcaClass.getDeclaredField("enable");
            enableField.setAccessible(true);
            pcaAvailableCache = enableField.getBoolean(null);
        } catch (Exception e) {
            pcaAvailableCache = false;
        }
        pcaAvailableCacheTime = now;
        return pcaAvailableCache;
    }

    /**
     * 获取世界中所有已加载容器内的物品（聚合计数）。
     * 扫描所有已加载区块中的方块实体容器和实体容器。
     *
     * @return 物品到数量的映射（可能为空 Map）
     */
    public static Map<Item, Integer> getAllContainerItems() {
        Map<Item, Integer> result = new HashMap<>();
        Minecraft client = Minecraft.getInstance();
        Level level = client.level;
        if (level == null || client.player == null) return result;

        // 扫描方块实体容器
        for (BlockEntity be : getLoadedBlockEntities(level)) {
            if (be instanceof Container inv) {
                addInventoryItems(result, inv);
            }
        }

        // 扫描实体容器
        for (Entity entity : getLoadedContainerEntities(level)) {
            if (entity instanceof Container inv) {
                addInventoryItems(result, inv);
            }
        }

        return result;
    }

    /**
     * 获取世界中所有容器物品的详细信息（含位置和容器名称）。
     *
     * @return 容器物品信息列表
     */
    public static List<ContainerItemInfo> getContainerItemsDetailed() {
        List<ContainerItemInfo> result = new ArrayList<>();
        Minecraft client = Minecraft.getInstance();
        Level level = client.level;
        if (level == null || client.player == null) return result;

        for (BlockEntity be : getLoadedBlockEntities(level)) {
            if (be instanceof Container inv) {
                String name = getContainerName(be);
                BlockPos pos = be.getBlockPos();
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack stack = inv.getItem(i);
                    if (!stack.isEmpty()) {
                        result.add(new ContainerItemInfo(stack.copy(), pos, name));
                    }
                }
            }
        }

        for (Entity entity : getLoadedContainerEntities(level)) {
            if (entity instanceof Container inv) {
                String name = entity.getName().getString();
                BlockPos pos = entity.blockPosition();
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack stack = inv.getItem(i);
                    if (!stack.isEmpty()) {
                        result.add(new ContainerItemInfo(stack.copy(), pos, name));
                    }
                }
            }
        }

        return result;
    }

    /**
     * 结合材料列表和世界容器物品，计算考虑世界容器后的缺失数量。
     * 已有数量 = 玩家背包 + 世界容器中的物品。
     *
     * @return 材料列表（totalCount 为总需求，missingCount 为考虑世界容器后的缺失数）
     */
    public static List<LitematListAPI.MaterialItem> getMaterialListWithWorldItems() {
        List<LitematListAPI.MaterialItem> baseList = LitematListAPI.getMaterialList();
        if (baseList.isEmpty()) return baseList;

        // 获取世界容器中的物品
        Map<Item, Integer> worldItems = getAllContainerItems();

        // 重新计算缺失数量（玩家背包已在 LitematListAPI 中计算，这里追加世界容器）
        List<LitematListAPI.MaterialItem> result = new ArrayList<>();
        for (LitematListAPI.MaterialItem item : baseList) {
            Item itemType = item.itemStack().getItem();
            int worldHave = worldItems.getOrDefault(itemType, 0);
            // missingCount 已经是减去玩家背包后的值，再减去世界容器中的数量
            int adjustedMissing = Math.max(0, item.missingCount() - worldHave);
            result.add(new LitematListAPI.MaterialItem(item.itemStack(), item.totalCount(), adjustedMissing));
        }
        return result;
    }

    // ==================== 内部实现 ====================

    /**
     * 获取世界中所有已加载的方块实体。
     * 遍历玩家所在区块及其相邻区块，收集所有方块实体。
     */
    private static List<BlockEntity> getLoadedBlockEntities(Level level) {
        List<BlockEntity> result = new ArrayList<>();
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return result;

        BlockPos playerPos = client.player.blockPosition();
        int radius = 8; // 扫描半径（区块）

        for (int cx = -radius; cx <= radius; cx++) {
            for (int cz = -radius; cz <= radius; cz++) {
                int chunkX = (playerPos.getX() >> 4) + cx;
                int chunkZ = (playerPos.getZ() >> 4) + cz;
                if (level.hasChunk(chunkX, chunkZ)) {
                    level.getChunk(chunkX, chunkZ).getBlockEntities().forEach((pos, be) -> {
                        if (be instanceof ChestBlockEntity
                                || be instanceof BarrelBlockEntity
                                || be instanceof ShulkerBoxBlockEntity
                                || be instanceof HopperBlockEntity
                                || be instanceof DispenserBlockEntity
                                || be instanceof AbstractFurnaceBlockEntity
                                || be instanceof BrewingStandBlockEntity) {
                            result.add(be);
                        }
                    });
                }
            }
        }
        return result;
    }

    /**
     * 获取世界中所有已加载的容器实体。
     */
    private static List<Entity> getLoadedContainerEntities(Level level) {
        List<Entity> result = new ArrayList<>();
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return result;

        BlockPos playerPos = client.player.blockPosition();
        int radius = 64;
        AABB box = new AABB(
                playerPos.getX() - radius, playerPos.getY() - radius, playerPos.getZ() - radius,
                playerPos.getX() + radius, playerPos.getY() + radius, playerPos.getZ() + radius);

        for (Entity entity : level.getEntitiesOfClass(Entity.class, box, e -> true)) {
            if (entity instanceof Container) {
                result.add(entity);
            }
        }
        return result;
    }

    /**
     * 将容器库存中的物品累加到结果 Map 中。
     */
    private static void addInventoryItems(Map<Item, Integer> result, Container inv) {
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                result.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
    }

    /**
     * 获取容器的显示名称。
     */
    private static String getContainerName(BlockEntity be) {
        if (be instanceof ChestBlockEntity) return "箱子";
        if (be instanceof BarrelBlockEntity) return "木桶";
        if (be instanceof ShulkerBoxBlockEntity) return "潜影盒";
        if (be instanceof HopperBlockEntity) return "漏斗";
        if (be instanceof DispenserBlockEntity) return "发射器";
        if (be instanceof AbstractFurnaceBlockEntity) return "熔炉";
        if (be instanceof BrewingStandBlockEntity) return "酿造台";
        return "容器";
    }
}