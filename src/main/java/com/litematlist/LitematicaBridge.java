package com.litematlist;

import com.litematlist.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.malilib.util.LayerMode;
import fi.dy.masa.malilib.util.LayerRange;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 与 Litematica 模组的桥接层。
 * 使用 Litematica 的公开 API 获取当前已加载的原理图列表。
 */
public class LitematicaBridge {

    public static boolean isAvailable() {
        try {
            // 检查 Litematica 是否已加载
            DataManager.getInstance();
            return true;
        } catch (NoClassDefFoundError | Exception e) {
            return false;
        }
    }

    /**
     * 获取 Litematica 中当前已加载的所有原理图。
     */
    public static List<SchematicInfo> getLoadedSchematics() {
        List<SchematicInfo> result = new ArrayList<>();
        try {
            SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
            List<SchematicPlacement> placements = manager.getAllSchematicsPlacements();

            LitematListMod.LOGGER.info("从 Litematica 获取到 {} 个已加载的原理图", placements.size());

            for (SchematicPlacement placement : placements) {
                String name = placement.getName();
                java.io.File schematicFile = placement.getSchematicFile();
                Path filePath = schematicFile != null ? schematicFile.toPath() : null;
                if (filePath != null) {
                    result.add(new SchematicInfo(name, filePath));
                    LitematListMod.LOGGER.info("  原理图: {} -> {}", name, filePath);
                } else {
                    LitematListMod.LOGGER.warn("  原理图 {} 没有关联的文件路径", name);
                }
            }
        } catch (Exception e) {
            LitematListMod.LOGGER.error("从 Litematica 获取原理图列表失败", e);
        }
        return result;
    }

    /**
     * 从 schematics 文件夹递归扫描 .litematic 文件。
     */
    public static List<SchematicInfo> scanSchematicsFolder() {
        List<SchematicInfo> result = new ArrayList<>();
        Path schematicsDir = MinecraftClient.getInstance().runDirectory.toPath().resolve("schematics");
        File dir = schematicsDir.toFile();

        if (!dir.exists() || !dir.isDirectory()) {
            return result;
        }

        scanDirectory(dir, schematicsDir, result);
        return result;
    }

    private static void scanDirectory(File currentDir, Path basePath, List<SchematicInfo> result) {
        File[] files = currentDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, basePath, result);
            } else if (file.getName().endsWith(".litematic")) {
                String name = file.getName();
                int dotIndex = name.lastIndexOf('.');
                String displayName = dotIndex > 0 ? name.substring(0, dotIndex) : name;
                // 显示相对路径
                Path relativePath = basePath.relativize(file.toPath());
                if (relativePath.getParent() != null) {
                    displayName = relativePath.getParent().toString()
                            .replace(File.separatorChar, '/') + "/" + displayName;
                }
                result.add(new SchematicInfo(displayName, file.toPath()));
            }
        }
    }

    public record SchematicInfo(String name, Path filePath) {}

    /**
     * 原理图层级过滤区间：相对放置原点的层级 [min, max]（世界层级减去原点 k 值），
     * axis 为 Direction.Axis 的 ordinal（0=X, 1=Y, 2=Z）。
     * 区间两端都可能为负数（原理图区域本身可处于原点负方向）。
     */
    public record LayerFilter(int axis, int min, int max) {
        public boolean contains(int k) {
            return k >= min && k <= max;
        }

        /** 过滤签名，用于解析缓存区分不同过滤结果 */
        public String signature() {
            return axis + ":" + min + ":" + max;
        }
    }

    /**
     * 计算指定原理图文件当前应统计的层级过滤区间。
     * 读取投影当前的渲染层规则（全部/单层/层级范围/上方所有/下方所有，数值为世界坐标），
     * 结合该原理图在投影中的放置原点 k 值，换算为原理图内部相对层级区间。
     *
     * @return 过滤区间；开关关闭、规则为「全部」或找不到对应放置实例时返回 null（表示不过滤/全量）
     */
    public static LayerFilter computeRenderLayerFilter(Path filePath) {
        if (Configs.FeatureToggles.SYNC_RENDER_LAYER.getBooleanValue() == false) {
            return null;
        }
        try {
            LayerRange range = DataManager.getRenderLayerRange();
            if (range == null || range.getLayerMode() == LayerMode.ALL) {
                return null; // 「全部」模式统计所有方块，无需过滤
            }
            Direction.Axis axis = range.getAxis();
            Path target = filePath.toAbsolutePath().normalize();
            for (SchematicPlacement placement : DataManager.getSchematicPlacementManager().getAllSchematicsPlacements()) {
                java.io.File schematicFile = placement.getSchematicFile();
                Path placementFile = schematicFile != null ? schematicFile.toPath() : null;
                if (placementFile == null || placementFile.toAbsolutePath().normalize().equals(target) == false) {
                    continue;
                }
                BlockPos origin = placement.getOrigin();
                int originK = switch (axis) {
                    case X -> origin.getX();
                    case Y -> origin.getY();
                    case Z -> origin.getZ();
                };
                // 规则命中区间（世界坐标）→ 相对放置原点的层级区间（可能为负）
                LayerFilter filter = new LayerFilter(axis.ordinal(),
                        range.getLayerMin() - originK, range.getLayerMax() - originK);
                LitematListMod.LOGGER.info("[LitematicaBridge] 渲染层规则: 轴={} 世界区间 [{}..{}] 原点k={} → 相对层级 [{}..{}]",
                        axis, range.getLayerMin(), range.getLayerMax(), originK, filter.min(), filter.max());
                return filter;
            }
            LitematListMod.LOGGER.warn("[LitematicaBridge] 未找到原理图对应的已加载放置实例: {}", filePath);
        } catch (Exception e) {
            LitematListMod.LOGGER.error("[LitematicaBridge] 计算渲染层过滤失败: {}", filePath, e);
        }
        return null;
    }
}