package com.litematlist;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Syncmatica（共享原理图增强 / syncmatica_r）「认领材料」集成桥。
 * <p>
 * 全部通过反射调用 syncmatica_r 的公开 API：
 * {@code SyncmaticaMaterialApi.getClaimedMaterialRequirements(UUID)} =>
 * {@code List<MaterialRequirement(itemId, variant, missingAmount)>}。
 * 模组未安装或 API 缺失时静默降级（返回空列表），不产生编译期依赖。
 */
public final class SyncmaticaBridge {

    public static final String MOD_ID = "syncmatica_r";

    /** 认领材料列表条目的虚拟路径（非真实文件，仅作条目与忽略/替换数据的持久化键）。
     *  注意：不能含冒号/斜杠等 Windows 非法字符，否则 Path.of 静态初始化直接抛 InvalidPathException 崩溃。 */
    public static final Path SYNC_PATH = Path.of("syncmatica-claimed-materials");

    private SyncmaticaBridge() {
    }

    public static boolean isModLoaded() {
        try {
            return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(MOD_ID);
        } catch (Exception e) {
            return false;
        }
    }

    /** 该路径是否为「在Syncmatica认领的材料」条目的虚拟路径 */
    public static boolean isSyncPath(Path path) {
        return path != null && SYNC_PATH.equals(path);
    }

    /**
     * 拉取当前玩家在 Syncmatica 中已认领的材料（返回其缺失数量）。
     * 每次调用都会向 Syncmatica 重新拉取最新数据；
     * 未安装 Syncmatica / 未在游戏中 / 反射异常时返回空列表。
     */
    public static List<LitematicReader.MaterialEntry> loadClaimedMaterialEntries() {
        if (!isModLoaded()) {
            return Collections.emptyList();
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return Collections.emptyList();
        }
        try {
            Class<?> apiClass = Class.forName("cn.net.rms.syncmatica_r.api.SyncmaticaMaterialApi");
            java.lang.reflect.Method method = apiClass.getMethod("getClaimedMaterialRequirements", java.util.UUID.class);
            Object result = method.invoke(null, mc.player.getUUID());
            if (!(result instanceof List<?> claims)) {
                return Collections.emptyList();
            }

            List<LitematicReader.MaterialEntry> entries = new ArrayList<>();
            for (Object claim : claims) {
                Class<?> claimClass = claim.getClass();
                String itemIdStr = (String) claimClass.getMethod("itemId").invoke(claim);
                String variant = (String) claimClass.getMethod("variant").invoke(claim);
                int missingAmount = (Integer) claimClass.getMethod("missingAmount").invoke(claim);
                LitematicReader.MaterialEntry entry = toEntry(itemIdStr, variant, missingAmount);
                if (entry != null) {
                    entries.add(entry);
                }
            }
            return entries;
        } catch (Exception e) {
            LitematListMod.LOGGER.warn("[SyncmaticaBridge] 拉取认领材料失败: {}", e.toString());
            return Collections.emptyList();
        }
    }

    /**
     * itemId（+ variant）→ MaterialEntry。缺失量既作为总数也作为缺失数
     * （本模组无「已有」数据源，认领材料全部按缺失计）。
     * 物品无法解析或缺失量为 0 时返回 null（跳过）。
     */
    private static LitematicReader.MaterialEntry toEntry(String itemIdStr, String variant, int missingAmount) {
        if (itemIdStr == null || itemIdStr.isEmpty() || missingAmount <= 0) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.get(Identifier.tryParse(itemIdStr)).map(Holder::value).orElse(null);
        String variantSuffix = "";
        if (item == null && variant != null && !variant.isEmpty() && variant.length() <= 64) {
            // variant 非空时尝试 <itemId>_<variant> 组合（如氧化变体）
            item = BuiltInRegistries.ITEM.get(Identifier.tryParse(itemIdStr + "_" + variant)).map(Holder::value).orElse(null);
            variantSuffix = " (" + variant + ")";
        }
        if (item == null || item == Items.AIR) {
            return null;
        }
        String blockName = new ItemStack(item).getHoverName().getString() + variantSuffix;
        return new LitematicReader.MaterialEntry(item, blockName, missingAmount, missingAmount);
    }
}