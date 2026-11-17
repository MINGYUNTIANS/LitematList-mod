package com.litematlist;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import fi.dy.masa.malilib.util.nbt.NbtUtils;
//#endif
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.enums.ChestType;
//#if MC == 12110
import net.minecraft.block.enums.DoubleBlockHalf;
//#endif
//#if MC == 12110
import net.minecraft.block.enums.SlabType;
//#endif
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.*;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.registry.Registries;
//#endif
import net.minecraft.util.Identifier;

import java.nio.file.Path;
import java.util.*;
//#if MC == 11701
//$$import net.minecraft.util.registry.Registry;
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
/**
 * 读取和解析 .litematic 文件，提取方块状态并统计每种方块对应的物品及数量。
 * 参照 Litematica 0.24.9 的 LitematicaSchematic 源码实现。
 * 支持 Litematica 原生格式（long[] BlockStates）和 Sponge 格式（byte[] BlockData）。
 */
//#endif
public class LitematicReader {

    public record MaterialEntry(Item item, String blockName, int totalCount, int missingCount,
                                ItemStack exactStack, int category) {
        /** 材料分类：建筑材料（原理图方块及其掉落物、流体源等） */
        public static final int CATEGORY_BUILDING = 0;
        /** 材料分类：容器内数据（箱子/潜影盒/陶罐/花盆等容器内的物品） */
        public static final int CATEGORY_CONTAINER = 1;
        /** 材料分类：实体数据（矿车/盔甲架/船等实体本身及其装备、召唤材料、展示框展示物） */
        public static final int CATEGORY_ENTITY = 2;
        /** 材料分类：实体容器数据（漏斗矿车/箱船/驴羊驼箱子/展示框内潜影盒等实体容器内的物品） */
        public static final int CATEGORY_ENTITY_CONTAINER = 3;

        public MaterialEntry(Item item, String blockName, int totalCount) {
            this(item, blockName, totalCount, 0, null, CATEGORY_BUILDING);
        }
        public MaterialEntry(Item item, String blockName, int totalCount, int missingCount) {
            this(item, blockName, totalCount, missingCount, null, CATEGORY_BUILDING);
        }
        public MaterialEntry(Item item, String blockName, int totalCount, int missingCount, ItemStack exactStack) {
            this(item, blockName, totalCount, missingCount, exactStack, CATEGORY_BUILDING);
        }
        /** 是否为精确物品（带非默认组件的特殊物品，按精确ID独立成条，不参与原材料溯源） */
        public boolean isExact() {
            return exactStack != null;
        }
        public ItemStack toItemStack() {
            if (exactStack != null) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                return exactStack.copyWithCount(totalCount);
//#endif
//#if MC == 11701
//$$                ItemStack copy = exactStack.copy();
//#endif
//#if MC == 11701
//$$                copy.setCount(totalCount);
//#endif
//#if MC == 11701
//$$                return copy;
//#endif
            }
            return new ItemStack(item, totalCount);
        }
    }

    /** 精确物品（带非默认组件的特殊物品）统计项：按精确ID归并，与普通物品分开统计。 */
    private record ExactItem(Item item, ItemStack stack, int count) {}

    /** 精确物品归并：同一精确ID累加数量。 */
    private static void addExactItem(Map<String, ExactItem> exactItems, String exactId,
                                     Item item, ItemStack stack, int count) {
        if (exactItems == null || exactId == null || item == null || item == Items.AIR || count <= 0) {
            return;
        }
        ExactItem existing = exactItems.get(exactId);
        if (existing == null) {
            exactItems.put(exactId, new ExactItem(item, stack, count));
        } else {
            exactItems.put(exactId, new ExactItem(existing.item(), existing.stack(), existing.count() + count));
        }
    }

    /**
     * 取物品标签中的非默认组件复合标签：1.20.5+ 为 components 键，旧格式为 tag 键。
     * 两者均缺失或为空时返回 null（普通物品）。
     */
    private static NbtCompound nonDefaultComponents(NbtCompound itemTag) {
//#if MC == 12108 || MC == 12110 || MC == 12111
        NbtCompound components = itemTag.getCompound("components")
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                .filter(c -> c.getKeys().isEmpty() == false).orElse(null);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        if (components != null) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$        NbtCompound components = itemTag.getCompound("components");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (components.getKeys().isEmpty() == false) {
//#elseif MC == 12101 || MC == 12104
//$$        if (components != null && components.getKeys().isEmpty() == false) {
//#endif
            return components;
        }
//#if MC == 12108 || MC == 12110 || MC == 12111
        return itemTag.getCompound("tag")
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                .filter(c -> c.getKeys().isEmpty() == false).orElse(null);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        NbtCompound tag = itemTag.getCompound("tag");
//#elseif MC == 12101 || MC == 12104
//$$        NbtCompound legacyTag = itemTag.getCompound("tag");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (tag.getKeys().isEmpty() == false) {
//#elseif MC == 12101 || MC == 12104
//$$        if (legacyTag != null && legacyTag.getKeys().isEmpty() == false) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            return tag;
//#elseif MC == 12101 || MC == 12104
//$$            return legacyTag;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$        return null;
//#endif
    }

    /** 组件键去掉 minecraft: 前缀显示（与示例 custom_name / stored_enchantments 一致），其余命名空间保留。 */
    private static String displayComponentKey(String key) {
        return key.startsWith("minecraft:") ? key.substring("minecraft:".length()) : key;
    }

    /**
     * 拼装精确物品ID：新格式 id[组件=SNBT,...]（如 minecraft:iron_nugget[custom_name='{"text":"111"}']），
     * 旧格式 id{tag SNBT}。组件值直接使用 NBT 的 SNBT 序列化（字符串自动带单引号）。
     */
    private static String buildExactId(Identifier itemId, NbtCompound components, boolean legacy) {
        StringBuilder sb = new StringBuilder(itemId.toString());
        if (components == null || components.getKeys().isEmpty()) {
            return sb.toString();
        }
        if (legacy) {
            sb.append(components.toString());
            return sb.toString();
        }
        sb.append('[');
        boolean first = true;
        for (String key : components.getKeys()) {
            NbtElement value = components.get(key);
            if (value == null) {
                continue;
            }
            if (first == false) {
                sb.append(',');
            }
            sb.append(displayComponentKey(key)).append('=').append(value.toString());
            first = false;
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * 将物品标签解码为 ItemStack（含全部组件），用于界面图标预览与悬浮提示；
     * 解码失败（旧格式 tag / 无效组件 / 无世界上下文）时退化为普通物品图标。
     */
    private static ItemStack decodeExactStack(NbtCompound itemTag, Item fallback, int count) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client != null && client.world != null) {
            try {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                net.minecraft.registry.RegistryOps<NbtElement> ops =
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        net.minecraft.registry.RegistryOps.of(NbtOps.INSTANCE, client.world.getRegistryManager());
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                Optional<ItemStack> parsed = ItemStack.CODEC.parse(ops, itemTag).result();
//#endif
//#if MC == 11701
//$$                Optional<ItemStack> parsed = ItemStack.CODEC.parse(NbtOps.INSTANCE, itemTag).result();
//#endif
                if (parsed.isPresent() && parsed.get().isEmpty() == false) {
                    return parsed.get();
                }
            } catch (Exception e) {
                LitematListMod.LOGGER.debug("精确物品解码失败，退化为普通图标: {}", e.getMessage());
            }
        }
        if (fallback == null || fallback == Items.AIR) {
            return null;
        }
        return new ItemStack(fallback, Math.max(1, count));
    }

    /**
     * 统一物品计入入口：带非默认组件（重命名/附魔/成书/地图/旗帜图案/烟花/美西螈桶/耐久等）
     * 的特殊物品以精确ID单独归并到 exactItems，不并入普通物品；普通物品并入 itemCounts/itemNames。
     * 返回是否成功计入。
     */
    private static boolean addItemEntry(NbtCompound itemTag, Map<Item, Integer> itemCounts,
                                        Map<Item, String> itemNames, Map<String, ExactItem> exactItems) {
//#if MC == 12108 || MC == 12110 || MC == 12111
        Optional<String> idOpt = itemTag.getString("id");
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        String idValue = itemTag.getString("id");
//#elseif MC == 12101 || MC == 12104
//$$        String idOpt = itemTag.getString("id");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        if (idOpt.isEmpty()) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        if (idValue == null || idValue.isEmpty()) {
//#elseif MC == 12101 || MC == 12104
//$$        if (idOpt == null || idOpt.isEmpty()) {
//#endif
            return false;
        }
//#if MC == 12108 || MC == 12110 || MC == 12111
        Identifier itemId = Identifier.tryParse(idOpt.get());
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        Identifier itemId = Identifier.tryParse(idValue);
//#elseif MC == 12101 || MC == 12104
//$$        Identifier itemId = Identifier.tryParse(idOpt);
//#endif
        if (itemId == null) {
            return false;
        }
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Item item = Registries.ITEM.get(itemId);
//#elseif MC == 11701
//$$        Item item = Registry.ITEM.get(itemId);
//#endif
        if (item == null || item == Items.AIR) {
            return false;
        }
        int itemCount = 1;
        NbtElement countElement = itemTag.get("count");
        if (countElement == null) {
            countElement = itemTag.get("Count");
        }
        if (countElement instanceof net.minecraft.nbt.AbstractNbtNumber numberTag) {
            itemCount = numberTag.intValue();
        }
        if (itemCount <= 0) {
            itemCount = 1;
        }
        NbtCompound components = exactItems != null ? nonDefaultComponents(itemTag) : null;
        if (components != null) {
//#if MC == 12108 || MC == 12110 || MC == 12111
            Optional<NbtCompound> modernOpt = itemTag.getCompound("components");
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            boolean legacy = itemTag.contains("components", NbtElement.COMPOUND_TYPE) == false
//#elseif MC == 12101 || MC == 12104
//$$            NbtCompound modern = itemTag.getCompound("components");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            boolean legacy = modernOpt.isEmpty() || modernOpt.get().getKeys().isEmpty();
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    || itemTag.getCompound("components").getKeys().isEmpty();
//#elseif MC == 12101 || MC == 12104
//$$            boolean legacy = modern.getKeys().isEmpty();
//#endif
            String exactId = buildExactId(itemId, components, legacy);
            addExactItem(exactItems, exactId, item, decodeExactStack(itemTag, item, itemCount), itemCount);
            return true;
        }
        itemCounts.merge(item, itemCount, Integer::sum);
        itemNames.putIfAbsent(item, item.getName().getString());
        return true;
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 从文件路径加载材料列表。
     * 解析结果按「文件修改时间」缓存，供注入/HUD/界面共享，
     * 避免同一次刷新流程中对同一份大原理图反复解析（防大量数据流/卡顿）。
     */
//#endif
    public static List<MaterialEntry> loadMaterialList(Path filePath) {
        return loadMaterialList(filePath, null);
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 读取压缩 NBT 文件（1.20.1 使用 NbtIo.readCompressed；旧版 malilib 无此工具方法） */
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    public static NbtCompound readNbtFile(Path filePath) throws java.io.IOException {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        try (java.io.InputStream in = java.nio.file.Files.newInputStream(filePath)) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            return NbtIo.readCompressed(in);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        }
//#endif
    }

    /**
     * 从文件路径加载材料列表，可按渲染层过滤（filter 为 null 时统计全部方块）。
     * 缓存同时按「文件修改时间 + 过滤签名」区分，避免不同过滤条件下互串结果。
     */
    public static List<MaterialEntry> loadMaterialList(Path filePath, LitematicaBridge.LayerFilter filter) {
        long mtime = filePath.toFile().lastModified();
        String sig = filter == null ? "all" : filter.signature();
        CachedResult cached = PARSE_CACHE.get(filePath);
        if (cached != null && cached.lastModified == mtime && cached.filterSig.equals(sig)) {
            return cached.materials;
        }

        LitematListMod.LOGGER.info("正在解析原理图: {} (层级过滤: {})", filePath.toAbsolutePath(), sig);

        List<MaterialEntry> result;
        try {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            NbtCompound rootTag = NbtUtils.readNbtFromFileAsPath(filePath);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            NbtCompound rootTag = readNbtFile(filePath);
//#endif
            if (rootTag == null) {
                LitematListMod.LOGGER.error("读取 NBT 失败: {}", filePath);
                return Collections.emptyList();
            }
            LitematListMod.LOGGER.info("成功读取 NBT，顶层键: {}", rootTag.getKeys());
            result = parseBlockStates(rootTag, filePath, filter);
        } catch (Exception e) {
            LitematListMod.LOGGER.error("解析原理图异常: {}", filePath, e);
            return Collections.emptyList();
        }

        if (result != null && !result.isEmpty()) {
            PARSE_CACHE.put(filePath, new CachedResult(mtime, sig, result));
        }
        return result;
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 清空解析缓存。当「是否计算容器数据」等全局解析设置变化时调用，
     * 强制下次加载按新设置重新解析。
     */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 清空解析缓存，供「是否计算容器数据」等全局解析设置变化时强制重新解析。 */
//#endif
    public static void clearParseCache() {
        PARSE_CACHE.clear();
    }

    /** 解析结果缓存条目（按文件修改时间 + 层级过滤签名失效） */
    private record CachedResult(long lastModified, String filterSig, List<MaterialEntry> materials) {}

    /** .litematic 解析结果缓存 */
    private static final Map<Path, CachedResult> PARSE_CACHE = new HashMap<>();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static List<MaterialEntry> parseBlockStates(NbtCompound rootTag, Path filePath, LitematicaBridge.LayerFilter filter) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 按四个分类分别累积，最后合并（同一物品取最早分类）并按分类分组、组内数量降序输出
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        MaterialSink buildingSink = new MaterialSink();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        MaterialSink containerSink = new MaterialSink();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        MaterialSink entitySink = new MaterialSink();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        MaterialSink entityContainerSink = new MaterialSink();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        int regionCount = 0;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        int failedRegions = 0;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 检查 Version
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (!rootTag.contains("Version")) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            LitematListMod.LOGGER.warn("缺少 Version 字段，不是有效的 Litematica 原理图");
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            return Collections.emptyList();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        int version = rootTag.getInt("Version").orElse(-1);
//#elseif MC == 12101 || MC == 12104
//$$        int version = rootTag.getInt("Version");
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        LitematListMod.LOGGER.info("原理图版本: {}", version);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 读取 Regions（Litematica 原生格式）
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        Optional<NbtCompound> regionsOpt = rootTag.getCompound("Regions");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        if (regionsOpt.isPresent()) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            NbtCompound regions = regionsOpt.get();
//#endif
//#if MC == 12101 || MC == 12104
//$$        NbtCompound regions = rootTag.getCompound("Regions");
//#endif
//#if MC == 12101 || MC == 12104
//$$        if (regions.isEmpty() == false) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            LitematListMod.LOGGER.info("找到 Regions，共 {} 个子区域", regions.getKeys().size());
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            for (String regionName : regions.getKeys()) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                Optional<NbtCompound> regionOpt = regions.getCompound(regionName);
//#elseif MC == 12101 || MC == 12104
//$$                NbtCompound regionOpt = regions.getCompound(regionName);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                if (regionOpt.isPresent()) {
//#elseif MC == 12101 || MC == 12104
//$$                if (regionOpt.isEmpty() == false) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    try {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                        parseRegion(regionOpt.get(), buildingSink, containerSink, entitySink,
//#elseif MC == 12101 || MC == 12104
//$$                        parseRegion(regionOpt, buildingSink, containerSink, entitySink,
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                entityContainerSink, regionName, version, filter);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        regionCount++;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    } catch (Exception e) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        failedRegions++;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        LitematListMod.LOGGER.error("解析 Region '{}' 失败，跳过该区域: {}", regionName, e.getMessage());
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        LitematListMod.LOGGER.debug("Region '{}' 异常详情:", regionName, e);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                }
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
        // 尝试 Sponge 格式（Blocks 键）
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (regionCount == 0 && rootTag.contains("Blocks")) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            LitematListMod.LOGGER.info("尝试解析 Sponge 格式...");
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            try {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                parseSpongeFormat(rootTag, buildingSink, containerSink, filter);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                regionCount++;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            } catch (Exception e) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                LitematListMod.LOGGER.error("Sponge 格式解析失败: {}", e.getMessage());
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
        LitematListMod.LOGGER.info("解析了 {} 个 Region，失败 {} 个，共 {} 种方块",
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                regionCount, failedRegions, buildingSink.itemCounts.size());
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        List<MaterialEntry> result = new ArrayList<>();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Map<Item, Integer> normalIndex = new HashMap<>();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Map<String, Integer> exactIndex = new HashMap<>();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        appendSink(result, normalIndex, exactIndex, buildingSink, MaterialEntry.CATEGORY_BUILDING);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        appendSink(result, normalIndex, exactIndex, containerSink, MaterialEntry.CATEGORY_CONTAINER);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        appendSink(result, normalIndex, exactIndex, entitySink, MaterialEntry.CATEGORY_ENTITY);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        appendSink(result, normalIndex, exactIndex, entityContainerSink, MaterialEntry.CATEGORY_ENTITY_CONTAINER);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 分类优先：建筑材料 → 容器数据 → 实体数据 → 实体容器数据（组内按数量降序）
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        result.sort(Comparator.comparingInt(MaterialEntry::category)
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                .thenComparing(Comparator.comparingInt(MaterialEntry::totalCount).reversed()));
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return result;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 材料分类累积器：每个分类各自维护普通物品/名称/精确物品映射。 */
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static final class MaterialSink {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        final Map<Item, Integer> itemCounts = new LinkedHashMap<>();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        final Map<Item, String> itemNames = new LinkedHashMap<>();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        final Map<String, ExactItem> exactItems = new LinkedHashMap<>();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 将某一分类的累积结果并入总列表：同一物品（或同一精确ID）已在更早分类出现时并入该条目，
     * 保留最早分类与名称；否则新建条目。index 映射用于跨分类去重。
     */
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static void appendSink(List<MaterialEntry> out, Map<Item, Integer> normalIndex,
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                   Map<String, Integer> exactIndex, MaterialSink sink, int category) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        for (Map.Entry<Item, Integer> entry : sink.itemCounts.entrySet()) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            Item item = entry.getKey();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            int count = entry.getValue();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            Integer idx = normalIndex.get(item);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (idx != null) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                MaterialEntry old = out.get(idx);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                out.set(idx, new MaterialEntry(old.item(), old.blockName(), old.totalCount() + count,
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        old.missingCount(), old.exactStack(), old.category()));
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            } else {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                normalIndex.put(item, out.size());
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                out.add(new MaterialEntry(item, sink.itemNames.getOrDefault(item, item.getName().getString()),
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        count, 0, null, category));
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        for (Map.Entry<String, ExactItem> entry : sink.exactItems.entrySet()) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            String exactId = entry.getKey();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            ExactItem exact = entry.getValue();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            Integer idx = exactIndex.get(exactId);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (idx != null) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                MaterialEntry old = out.get(idx);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                out.set(idx, new MaterialEntry(old.item(), old.blockName(), old.totalCount() + exact.count(),
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        old.missingCount(), old.exactStack(), old.category()));
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            } else {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                ItemStack stack = exact.stack() != null ? exact.stack() : new ItemStack(exact.item(), 1);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                exactIndex.put(exactId, out.size());
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                out.add(new MaterialEntry(exact.item(), exactId, exact.count(), 0, stack, category));
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif

    /**
     * 流体源头等级属性：从水方块默认状态解析一次，避免硬编码 Yarn 属性名。
     * level 值为 0 时代表静水/静岩浆源头。
     */
    private static final net.minecraft.state.property.IntProperty FLUID_LEVEL_PROPERTY = findFluidLevelProperty();

    private static net.minecraft.state.property.IntProperty findFluidLevelProperty() {
        for (net.minecraft.state.property.Property<?> property : Blocks.WATER.getDefaultState().getProperties()) {
            if (property instanceof net.minecraft.state.property.IntProperty intProp
                    && "level".equals(property.getName())) {
                return intProp;
            }
        }
        return null;
    }

    /**
     * 流体与含水方块的额外统计（均属「建筑材料」，受「是否计算建筑材料」门控）：
     * - 水源头 / 岩浆源头（level=0）→ 水桶 / 岩浆桶
     * - 细雪 → 细雪桶
     * - 炼药锅只要含液体（哪怕不满）→ 对应一桶（水 / 岩浆 / 细雪）
     * - 含水方块 → 额外记一个水桶
     */
    private static void countFluidSources(BlockState state, Map<Item, Integer> itemCounts,
                                          Map<Item, String> itemNames) {
        Block block = state.getBlock();

        // 静水 / 静岩浆源头
        if (FLUID_LEVEL_PROPERTY != null && (block == Blocks.WATER || block == Blocks.LAVA)) {
            if (state.get(FLUID_LEVEL_PROPERTY) == 0) {
                addSpecialItem(block == Blocks.WATER ? Items.WATER_BUCKET : Items.LAVA_BUCKET, itemCounts, itemNames);
            }
            return;
        }

        // 细雪（方块本身即源头）
        if (block == Blocks.POWDER_SNOW) {
            addSpecialItem(Items.POWDER_SNOW_BUCKET, itemCounts, itemNames);
            return;
        }

        // 炼药锅：只要含液体（哪怕不满）即记一桶（受「统计」页「炼药锅」开关门控）
        if (block == Blocks.WATER_CAULDRON) {
            if (com.litematlist.config.Configs.Statistics.enabled("cauldron")) {
                addSpecialItem(Items.WATER_BUCKET, itemCounts, itemNames);
            }
            return;
        }
        if (block == Blocks.LAVA_CAULDRON) {
            if (com.litematlist.config.Configs.Statistics.enabled("cauldron")) {
                addSpecialItem(Items.LAVA_BUCKET, itemCounts, itemNames);
            }
            return;
        }
        if (block == Blocks.POWDER_SNOW_CAULDRON) {
            if (com.litematlist.config.Configs.Statistics.enabled("cauldron")) {
                addSpecialItem(Items.POWDER_SNOW_BUCKET, itemCounts, itemNames);
            }
            return;
        }

        // 含水方块：额外记一个水桶
        if (state.contains(net.minecraft.state.property.Properties.WATERLOGGED)
                && state.get(net.minecraft.state.property.Properties.WATERLOGGED)) {
            addSpecialItem(Items.WATER_BUCKET, itemCounts, itemNames);
        }
    }

    private static void addSpecialItem(Item item, Map<Item, Integer> itemCounts,
                                       Map<Item, String> itemNames) {
        addSpecialItem(item, itemCounts, itemNames, 1);
    }

    private static void addSpecialItem(Item item, Map<Item, Integer> itemCounts,
                                       Map<Item, String> itemNames, int amount) {
        if (item != Items.AIR && amount > 0) {
            itemCounts.merge(item, amount, Integer::sum);
            itemNames.putIfAbsent(item, item.getName().getString());
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    private static List<MaterialEntry> parseBlockStates(NbtCompound rootTag, Path filePath, LitematicaBridge.LayerFilter filter) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        // 按四个分类分别累积，最后合并（同一物品取最早分类）并按分类分组、组内数量降序输出
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        MaterialSink buildingSink = new MaterialSink();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        MaterialSink containerSink = new MaterialSink();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        MaterialSink entitySink = new MaterialSink();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        MaterialSink entityContainerSink = new MaterialSink();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        int regionCount = 0;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        int failedRegions = 0;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (!rootTag.contains("Version")) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            LitematListMod.LOGGER.warn("缺少 Version 字段，不是有效的 Litematica 原理图");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            return Collections.emptyList();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        int version = rootTag.getInt("Version");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        LitematListMod.LOGGER.info("原理图版本: {}", version);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (rootTag.contains("Regions", NbtElement.COMPOUND_TYPE)) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            NbtCompound regions = rootTag.getCompound("Regions");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            LitematListMod.LOGGER.info("找到 Regions，共 {} 个子区域", regions.getKeys().size());
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            for (String regionName : regions.getKeys()) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                if (regions.contains(regionName, NbtElement.COMPOUND_TYPE)) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    try {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        parseRegion(regions.getCompound(regionName), buildingSink, containerSink, entitySink,
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                                entityContainerSink, regionName, version, filter);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        regionCount++;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    } catch (Exception e) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        failedRegions++;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        LitematListMod.LOGGER.error("解析 Region '{}' 失败，跳过该区域: {}", regionName, e.getMessage());
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        LitematListMod.LOGGER.debug("Region '{}' 异常详情:", regionName, e);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                }
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
//$$        if (regionCount == 0 && rootTag.contains("Blocks")) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            LitematListMod.LOGGER.info("尝试解析 Sponge 格式...");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            try {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                parseSpongeFormat(rootTag, buildingSink, containerSink, filter);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                regionCount++;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            } catch (Exception e) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                LitematListMod.LOGGER.error("Sponge 格式解析失败: {}", e.getMessage());
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
//$$        LitematListMod.LOGGER.info("解析了 {} 个 Region，失败 {} 个，共 {} 种方块",
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                regionCount, failedRegions, buildingSink.itemCounts.size());
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        List<MaterialEntry> result = new ArrayList<>();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        Map<Item, Integer> normalIndex = new HashMap<>();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        Map<String, Integer> exactIndex = new HashMap<>();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        appendSink(result, normalIndex, exactIndex, buildingSink, MaterialEntry.CATEGORY_BUILDING);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        appendSink(result, normalIndex, exactIndex, containerSink, MaterialEntry.CATEGORY_CONTAINER);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        appendSink(result, normalIndex, exactIndex, entitySink, MaterialEntry.CATEGORY_ENTITY);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        appendSink(result, normalIndex, exactIndex, entityContainerSink, MaterialEntry.CATEGORY_ENTITY_CONTAINER);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        // 分类优先：建筑材料 → 容器数据 → 实体数据 → 实体容器数据（组内按数量降序）
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        result.sort(Comparator.comparingInt((MaterialEntry m) -> m.category())
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                .thenComparing(Comparator.comparingInt((MaterialEntry m) -> m.totalCount()).reversed()));
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        return result;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    /** 材料分类累积器：每个分类各自维护普通物品/名称/精确物品映射。 */
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    private static final class MaterialSink {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        final Map<Item, Integer> itemCounts = new LinkedHashMap<>();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        final Map<Item, String> itemNames = new LinkedHashMap<>();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        final Map<String, ExactItem> exactItems = new LinkedHashMap<>();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    /**
//$$     * 将某一分类的累积结果并入总列表：同一物品（或同一精确ID）已在更早分类出现时并入该条目，
//$$     * 保留最早分类与名称；否则新建条目。index 映射用于跨分类去重。
//$$     */
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    private static void appendSink(List<MaterialEntry> out, Map<Item, Integer> normalIndex,
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                                   Map<String, Integer> exactIndex, MaterialSink sink, int category) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        for (Map.Entry<Item, Integer> entry : sink.itemCounts.entrySet()) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            Item item = entry.getKey();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            int count = entry.getValue();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            Integer idx = normalIndex.get(item);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (idx != null) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                MaterialEntry old = out.get(idx);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                out.set(idx, new MaterialEntry(old.item(), old.blockName(), old.totalCount() + count,
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        old.missingCount(), old.exactStack(), old.category()));
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            } else {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                normalIndex.put(item, out.size());
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                out.add(new MaterialEntry(item, sink.itemNames.getOrDefault(item, item.getName().getString()),
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        count, 0, null, category));
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        for (Map.Entry<String, ExactItem> entry : sink.exactItems.entrySet()) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            String exactId = entry.getKey();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            ExactItem exact = entry.getValue();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            Integer idx = exactIndex.get(exactId);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (idx != null) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                MaterialEntry old = out.get(idx);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                out.set(idx, new MaterialEntry(old.item(), old.blockName(), old.totalCount() + exact.count(),
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        old.missingCount(), old.exactStack(), old.category()));
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            } else {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                ItemStack stack = exact.stack() != null ? exact.stack() : new ItemStack(exact.item(), 1);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                exactIndex.put(exactId, out.size());
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                out.add(new MaterialEntry(exact.item(), exactId, exact.count(), 0, stack, category));
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            }
//#endif
        }
    }

//#if MC == 12110
    /**
     * 方块实际消耗/掉落的物品份数：海泡菜（pickles）、蜡烛含16色（candles）、
     * 粉色花瓣（flower_amount）、海龟蛋（eggs）等「一格内可堆叠多份」的方块按 state 数量属性计
     * （如 pickles=4 建造需 4 个海泡菜、掉落也是 4 个），其余方块固定为 1。
     * <p>门/床/高花等「一件物品占上下两格」的方块只在下半格计 1（上半格计 0，否则会被算成两件）；
     * 双半砖（slab type=double）一格内实为两块半砖，按 2 计。
     */
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12111
//$$    /**
//$$     * 方块实际消耗/掉落的物品份数：海泡菜（pickles）、蜡烛含16色（candles）、
//$$     * 粉色花瓣（flower_amount）、海龟蛋（eggs）等「一格内可堆叠多份」的方块按 state 数量属性计
//$$     * （如 pickles=4 建造需 4 个海泡菜、掉落也是 4 个），其余方块固定为 1。
//$$     */
//#endif
    private static int getBlockStateItemCount(BlockState state) {
        if (state.contains(net.minecraft.state.property.Properties.PICKLES)) {
            return state.get(net.minecraft.state.property.Properties.PICKLES);
        }
        if (state.contains(net.minecraft.state.property.Properties.CANDLES)) {
            return state.get(net.minecraft.state.property.Properties.CANDLES);
        }
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (state.contains(net.minecraft.state.property.Properties.FLOWER_AMOUNT)) {
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            return state.get(net.minecraft.state.property.Properties.FLOWER_AMOUNT);
//#endif
//#if MC == 11701
//$$        // 粉色花瓣（1.20+）：本版本无该属性常量，按属性序列化名读取
//#endif
//#if MC == 11701
//$$        int flowerAmount = getIntStateProperty(state, "flower_amount");
//#endif
//#if MC == 11701
//$$        if (flowerAmount > 0) {
//#endif
//#if MC == 11701
//$$            return flowerAmount;
//#endif
        }
        if (state.contains(net.minecraft.state.property.Properties.EGGS)) {
            return state.get(net.minecraft.state.property.Properties.EGGS);
        }
//#if MC == 12110
        // 蛋糕：被吃掉一部分的蛋糕（bites>0）仍按一整个计算（计 1 个蛋糕）
//#endif
//#if MC == 12110
        if (state.contains(net.minecraft.state.property.Properties.BITES)) {
//#endif
//#if MC == 12110
            return 1;
//#endif
//#if MC == 12110
        }
//#endif
        // 雪片（顶层雪，layers=1..8）：一格内可叠多层，按状态层数计
        int layers = getIntStateProperty(state, "layers");
        if (layers > 0) {
            return layers;
        }
        // 枯叶堆（segment_amount=1..4）：一格内可堆多段，按状态段数计
        int segments = getIntStateProperty(state, "segment_amount");
        if (segments > 0) {
            return segments;
        }
//#if MC == 12110
        // 门/高花（大花等）：同一件物品占据上下两格，只统计下半格
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        // 门/床/高花：同一件物品占据上下两格，只统计下半格
//#elseif MC == 12111
//$$        // 门/高花：同一件物品占据上下两格，只统计下半格
//#endif
        if (state.contains(net.minecraft.state.property.Properties.DOUBLE_BLOCK_HALF)) {
//#if MC == 12110
            return state.get(net.minecraft.state.property.Properties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER ? 1 : 0;
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12111
//$$            return state.get(net.minecraft.state.property.Properties.DOUBLE_BLOCK_HALF) == net.minecraft.block.enums.DoubleBlockHalf.LOWER ? 1 : 0;
//#endif
        }
//#if MC == 12110 || MC == 12111
        // 床：一张床由尾/头两格构成（part=foot/head），只统计尾端一格，避免一张床被计为两张；
//#endif
//#if MC == 12110 || MC == 12111
        // 26.3 新增的麦秆床同属床类、带同样的 part 属性，走本分支
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        // 床：一张床由尾/头两格构成（part=foot/head），只统计尾端一格，避免一张床被计为两张
//#endif
        String bedPart = getEnumStateConstant(state, "part");
        if ("FOOT".equals(bedPart)) {
            return 1;
        }
        if ("HEAD".equals(bedPart)) {
            return 0;
        }
        // 双半砖：一格内由两块半砖合成，按两件计
        if (state.contains(net.minecraft.state.property.Properties.SLAB_TYPE)) {
//#if MC == 12111
//$$            return state.get(net.minecraft.state.property.Properties.SLAB_TYPE) == net.minecraft.block.enums.SlabType.DOUBLE ? 2 : 1;
//#endif
//#if MC == 12111
//$$        }
//#endif
//#if MC == 12111
//$$        // 蛋糕：被吃掉一部分的蛋糕（bites>0）仍按一整个计算（计 1 个蛋糕）
//#endif
//#if MC == 12111
//$$        if (state.contains(net.minecraft.state.property.Properties.BITES)) {
//#endif
//#if MC == 12111
//$$            return 1;
//#endif
//#if MC == 12110
            return state.get(net.minecraft.state.property.Properties.SLAB_TYPE) == SlabType.DOUBLE ? 2 : 1;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            return state.get(net.minecraft.state.property.Properties.SLAB_TYPE) == net.minecraft.block.enums.SlabType.DOUBLE ? 2 : 1;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        // 蛋糕：被吃掉一部分的蛋糕（bites>0）仍按一整个计算（计 1 个蛋糕）
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        if (state.contains(net.minecraft.state.property.Properties.BITES)) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            return 1;
//#endif
        }
        return 1;
    }

    /**
     * 按状态属性的序列化名读取整型属性值（如 layers、segment_amount、charges）。
     * 属性名由原版以字符串定义、跨版本与映射保持稳定，故不依赖具体映射的常量字段。
     * 属性不存在时返回 -1。
     */
    private static int getIntStateProperty(BlockState state, String propertyName) {
        for (net.minecraft.state.property.Property<?> property : state.getProperties()) {
            if (property.getName().equals(propertyName)
                    && property instanceof net.minecraft.state.property.IntProperty intProperty) {
                return state.get(intProperty);
            }
        }
        return -1;
    }

    /** 按状态属性的序列化名读取布尔属性值（如 eye）；属性不存在时返回 false。 */
    private static boolean getBoolStateProperty(BlockState state, String propertyName) {
        for (net.minecraft.state.property.Property<?> property : state.getProperties()) {
            if (property.getName().equals(propertyName)
                    && property instanceof net.minecraft.state.property.BooleanProperty boolProperty) {
                return state.get(boolProperty);
            }
        }
        return false;
    }

    /**
     * 按状态属性的序列化名读取枚举属性值的枚举常量名（如床的 part 为 "FOOT"/"HEAD"）。
     * Java 枚举常量名不受映射影响、跨版本稳定；属性不存在时返回 null。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String getEnumStateConstant(BlockState state, String propertyName) {
        for (net.minecraft.state.property.Property<?> property : state.getProperties()) {
            if (property.getName().equals(propertyName)) {
                return String.valueOf(state.get((net.minecraft.state.property.Property) property));
            }
        }
        return null;
    }

    /**
     * 由方块状态本身携带的「容器数据」统计（仅「是否计算容器数据」开启时调用）：
     * - 重生锚：每一级充能由 1 个萤石提供，按 charges 计入对应数量的萤石
     * - 末地传送门框架：框架上插有末影之眼（eye=true）时计入 1 个末影之眼
     */
    private static void countContainerBlockStateData(BlockState state, MaterialSink containerSink) {
        int charges = getIntStateProperty(state, "charges");
        if (charges > 0 && com.litematlist.config.Configs.Statistics.enabled("respawnAnchor")) {
            addSpecialItem(Items.GLOWSTONE, containerSink.itemCounts, containerSink.itemNames, charges);
        }
        if (getBoolStateProperty(state, "eye")
                && com.litematlist.config.Configs.Statistics.enabled("endPortalFrame")) {
            addSpecialItem(Items.ENDER_EYE, containerSink.itemCounts, containerSink.itemNames);
        }
    }

    /**
     * 解析 Sponge 格式（byte[] BlockData + NbtCompound Palette）。
     */
    private static void parseSpongeFormat(NbtCompound rootTag, MaterialSink buildingSink,
                                      MaterialSink containerSink,
                                      LitematicaBridge.LayerFilter filter) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 读取尺寸（使用绝对值以防负数）
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        short width = (short) Math.abs(rootTag.getShort("Width").orElse((short) 0));
//#elseif MC == 12101 || MC == 12104
//$$        short width = (short) Math.abs(rootTag.getShort("Width"));
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        short height = (short) Math.abs(rootTag.getShort("Height").orElse((short) 0));
//#elseif MC == 12101 || MC == 12104
//$$        short height = (short) Math.abs(rootTag.getShort("Height"));
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        short length = (short) Math.abs(rootTag.getShort("Length").orElse((short) 0));
//#elseif MC == 12101 || MC == 12104
//$$        short length = (short) Math.abs(rootTag.getShort("Length"));
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        short width = rootTag.contains("Width", NbtElement.SHORT_TYPE) ? (short) Math.abs(rootTag.getShort("Width")) : 0;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        short height = rootTag.contains("Height", NbtElement.SHORT_TYPE) ? (short) Math.abs(rootTag.getShort("Height")) : 0;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        short length = rootTag.contains("Length", NbtElement.SHORT_TYPE) ? (short) Math.abs(rootTag.getShort("Length")) : 0;
//#endif
        int totalBlocks = width * height * length;
        LitematListMod.LOGGER.info("Sponge 格式尺寸: {}x{}x{} ({} 方块)", width, height, length, totalBlocks);

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 读取 Palette（NbtCompound 格式，key=blockStateString, value=index）
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        Optional<NbtCompound> paletteOpt = rootTag.getCompound("Palette");
//#elseif MC == 12101 || MC == 12104
//$$        NbtCompound palette = rootTag.getCompound("Palette");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        if (paletteOpt.isEmpty()) {
//#elseif MC == 12101 || MC == 12104
//$$        if (palette.isEmpty()) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        NbtCompound palette;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (rootTag.contains("Palette", NbtElement.COMPOUND_TYPE)) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            palette = rootTag.getCompound("Palette");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        } else {
//#endif
            LitematListMod.LOGGER.warn("Sponge 格式缺少 Palette");
            return;
        }
//#if MC == 12108 || MC == 12110 || MC == 12111
        NbtCompound palette = paletteOpt.get();
//#endif
        int paletteSize = palette.getKeys().size();
        LitematListMod.LOGGER.info("Sponge 调色板大小: {}", paletteSize);

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 解析调色板：index -> BlockState
//#endif
        List<BlockState> paletteStates = new ArrayList<>(paletteSize);
        for (int i = 0; i < paletteSize; i++) {
            paletteStates.add(Blocks.AIR.getDefaultState());
        }
        for (String key : palette.getKeys()) {
//#if MC == 12108 || MC == 12110 || MC == 12111
            int index = palette.getInt(key).orElse(-1);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            int index = palette.getInt(key);
//#endif
            if (index >= 0 && index < paletteSize) {
                try {
                    BlockState state = parseSpongePaletteEntry(key, index);
                    paletteStates.set(index, state);
                } catch (Exception e) {
                    LitematListMod.LOGGER.warn("Sponge 调色板条目 '{}' 解析失败: {}", key, e.getMessage());
                }
            }
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 读取 BlockData（byte[]，可变长度整数编码）
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        Optional<byte[]> blockDataOpt = rootTag.getByteArray("BlockData");
//#elseif MC == 12101 || MC == 12104
//$$        byte[] blockData = rootTag.getByteArray("BlockData");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        if (blockDataOpt.isEmpty()) {
//#elseif MC == 12101 || MC == 12104
//$$        if (blockData.length == 0) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        byte[] blockData;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (rootTag.contains("BlockData", NbtElement.BYTE_ARRAY_TYPE)) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            blockData = rootTag.getByteArray("BlockData");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        } else {
//#endif
            LitematListMod.LOGGER.warn("Sponge 格式缺少 BlockData");
            return;
        }
//#if MC == 12108 || MC == 12110 || MC == 12111
        byte[] blockData = blockDataOpt.get();
//#endif
        LitematListMod.LOGGER.info("Sponge BlockData 大小: {} 字节", blockData.length);

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 解码可变长度整数
//#endif
        int bitsPerBlock = Math.max(2, 32 - Integer.numberOfLeadingZeros(paletteSize - 1));
        int[] decoded = decodeVarIntArray(blockData);
        // 「是否计算建筑材料」关闭时，方块（建筑材料）不计入材料列表
        boolean calcBuildingMaterials =
                com.litematlist.config.Configs.FeatureToggles.CALC_BUILDING_MATERIALS.getBooleanValue();
        // 「是否计算容器数据」：花盆内的植物等按容器数据归类统计
        boolean calcContainerData =
                com.litematlist.config.Configs.FeatureToggles.CALCULATE_CONTAINER_DATA.getBooleanValue();

        int decodedBlocks = 0;
        for (int index : decoded) {
            if (decodedBlocks >= totalBlocks) break;
            if (index >= 0 && index < paletteStates.size()) {
                // 渲染层过滤：Sponge 区域原点视为 0（解码索引 x 最快）
                if (filter != null) {
                    int relX = decodedBlocks % width;
                    int relZ = (decodedBlocks / width) % length;
                    int relY = decodedBlocks / (width * length);
                    int relK = switch (filter.axis()) {
                        case 0 -> relX;
                        case 1 -> relY;
                        default -> relZ;
                    };
                    if (filter.contains(relK) == false) {
                        decodedBlocks++;
                        continue;
                    }
                }
                BlockState state = paletteStates.get(index);
                if (!state.isAir()) {
                    if (calcBuildingMaterials) {
                        Item item = state.getBlock().asItem();
                        int count = getBlockStateItemCount(state);
                        if (item != Items.AIR && count > 0) {
                            buildingSink.itemCounts.merge(item, count, Integer::sum);
                            buildingSink.itemNames.putIfAbsent(item, state.getBlock().getName().getString());
                        }
                        // 流体源头/含水方块/炼药锅液体（均属建筑材料）额外统计
                        countFluidSources(state, buildingSink.itemCounts, buildingSink.itemNames);
                    }
                    // 花盆：统计盆内种植的植物（归类为容器数据）
                    if (calcContainerData && com.litematlist.config.Configs.Statistics.enabled("flowerPot")) {
                        countFlowerPotContent(state, containerSink.itemCounts, containerSink.itemNames);
                    }
                    // 重生锚充能→萤石、末地传送门框架上的末影之眼（均归类为容器数据）
                    if (calcContainerData) {
                        countContainerBlockStateData(state, containerSink);
                    }
                }
            }
            decodedBlocks++;
        }
        LitematListMod.LOGGER.info("Sponge 格式解码了 {} 个方块", decodedBlocks);

        // Sponge 格式的容器数据（v1: TileEntities；v2+: BlockEntities）
        if (calcContainerData) {
            parseTileEntityList(rootTag, "BlockEntities", containerSink, "Sponge", 0, 0, 0, filter, null);
            parseTileEntityList(rootTag, "TileEntities", containerSink, "Sponge", 0, 0, 0, filter, null);
        }
    }

    /**
     * 解码可变长度整数数组（Sponge 格式）。
     */
    private static int[] decodeVarIntArray(byte[] data) {
        List<Integer> result = new ArrayList<>();
        int i = 0;
        while (i < data.length) {
            int value = 0;
            int shift = 0;
            byte b;
            do {
                if (i >= data.length) break;
                b = data[i++];
                value |= (b & 0x7F) << shift;
                shift += 7;
            } while ((b & 0x80) != 0);
            result.add(value);
        }
        int[] arr = new int[result.size()];
        for (int j = 0; j < result.size(); j++) {
            arr[j] = result.get(j);
        }
        return arr;
    }

    /**
     * 解析 Sponge 格式的调色板条目（blockstate 字符串格式）。
     */
    private static BlockState parseSpongePaletteEntry(String stateString, int index) {
        // 格式: "minecraft:block_name[prop1=val1,prop2=val2]"
        String blockName;
        String propsStr = null;

        int bracketIdx = stateString.indexOf('[');
        if (bracketIdx > 0) {
            blockName = stateString.substring(0, bracketIdx);
            propsStr = stateString.substring(bracketIdx + 1, stateString.length() - 1);
        } else {
            blockName = stateString;
        }

        Identifier blockId = Identifier.tryParse(blockName);
        if (blockId == null) {
            LitematListMod.LOGGER.warn("Sponge 调色板条目 #{} 无法解析 ID: {}", index, blockName);
            return Blocks.AIR.getDefaultState();
        }

//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Block block = Registries.BLOCK.get(blockId);
//#elseif MC == 11701
//$$        Block block = Registry.BLOCK.get(blockId);
//#endif
        BlockState state = block.getDefaultState();

        if (propsStr != null && !propsStr.isEmpty()) {
            String[] props = propsStr.split(",");
            for (String prop : props) {
                String[] kv = prop.split("=", 2);
                if (kv.length == 2) {
                    for (net.minecraft.state.property.Property<?> property : state.getProperties()) {
                        if (property.getName().equals(kv[0].trim())) {
                            state = applyProperty(state, property, kv[1].trim());
                            break;
                        }
                    }
                }
            }
        }

        return state;
    }

    /**
     * 解析单个 Region（Litematica 原生格式）。
     */
    private static void parseRegion(NbtCompound region, MaterialSink buildingSink, MaterialSink containerSink,
                                 MaterialSink entitySink, MaterialSink entityContainerSink,
                                 String regionName, int version,
                                 LitematicaBridge.LayerFilter filter) {
        // 0. 读取区域原点（Position），用于渲染层过滤时换算相对层级（缺失时视为 0）
        int posX = 0, posY = 0, posZ = 0;
//#if MC == 12108 || MC == 12110 || MC == 12111
        Optional<NbtCompound> posOpt = region.getCompound("Position");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        if (posOpt.isPresent()) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            NbtCompound posTag = posOpt.get();
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            posX = posTag.getInt("x").orElse(0);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            posY = posTag.getInt("y").orElse(0);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            posZ = posTag.getInt("z").orElse(0);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (region.contains("Position", NbtElement.COMPOUND_TYPE)) {
//#elseif MC == 12101 || MC == 12104
//$$        NbtCompound posTag = region.getCompound("Position");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            NbtCompound posTag = region.getCompound("Position");
//#elseif MC == 12101 || MC == 12104
//$$        if (posTag.isEmpty() == false) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            posX = posTag.getInt("x");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            posY = posTag.getInt("y");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            posZ = posTag.getInt("z");
//#endif
        }
        // 1. 读取 Size
//#if MC == 12108 || MC == 12110 || MC == 12111
        Optional<NbtCompound> sizeOpt = region.getCompound("Size");
//#elseif MC == 12101 || MC == 12104
//$$        NbtCompound sizeTag = region.getCompound("Size");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        if (sizeOpt.isEmpty()) {
//#elseif MC == 12101 || MC == 12104
//$$        if (sizeTag.isEmpty()) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        NbtCompound sizeTag;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (region.contains("Size", NbtElement.COMPOUND_TYPE)) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            sizeTag = region.getCompound("Size");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        } else {
//#endif
            LitematListMod.LOGGER.warn("Region '{}' 缺少 Size 字段", regionName);
            return;
        }
//#if MC == 12108 || MC == 12110 || MC == 12111
        NbtCompound sizeTag = sizeOpt.get();
//#endif
        // 与投影 PositionUtils.getMinCorner 一致：Size 为负的轴，Position 保存的是该轴的
        // 「最大角」而非最小角，最小角 = Position + Size + 1（原理图可向负方向延伸）
//#if MC == 12108 || MC == 12110 || MC == 12111
        int rawSx = sizeTag.getInt("x").orElse(0);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$        int rawSx = sizeTag.getInt("x");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        int rawSy = sizeTag.getInt("y").orElse(0);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$        int rawSy = sizeTag.getInt("y");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        int rawSz = sizeTag.getInt("z").orElse(0);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$        int rawSz = sizeTag.getInt("z");
//#endif
        if (rawSx < 0) { posX = posX + rawSx + 1; }
        if (rawSy < 0) { posY = posY + rawSy + 1; }
        if (rawSz < 0) { posZ = posZ + rawSz + 1; }
        // 实际方块数 = |x| * |y| * |z|
        int sx = Math.abs(rawSx);
        int sy = Math.abs(rawSy);
        int sz = Math.abs(rawSz);
        if (filter != null) {
            LitematListMod.LOGGER.info("Region '{}' 最小角: ({}, {}, {})", regionName, posX, posY, posZ);
        }
        int totalBlocks = sx * sy * sz;
        LitematListMod.LOGGER.info("Region '{}' 尺寸: {}x{}x{} ({} 方块)", regionName, sx, sy, sz, totalBlocks);

        // 2. 读取 BlockStatePalette
//#if MC == 12108 || MC == 12110 || MC == 12111
        Optional<NbtList> paletteOpt = region.getList("BlockStatePalette");
//#elseif MC == 12101 || MC == 12104
//$$        NbtList palette = region.getList("BlockStatePalette", 10);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        if (paletteOpt.isEmpty()) {
//#elseif MC == 12101 || MC == 12104
//$$        if (palette.isEmpty()) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        NbtList palette;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (region.contains("BlockStatePalette", NbtElement.LIST_TYPE)) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            palette = region.getList("BlockStatePalette", NbtElement.COMPOUND_TYPE);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        } else {
//#endif
            LitematListMod.LOGGER.warn("Region '{}' 缺少 BlockStatePalette", regionName);
            return;
        }
//#if MC == 12108 || MC == 12110 || MC == 12111
        NbtList palette = paletteOpt.get();
//#endif
        LitematListMod.LOGGER.info("Region '{}' 调色板大小: {}", regionName, palette.size());

        // 解析调色板条目
        List<BlockState> paletteStates = new ArrayList<>(palette.size());
        int failedEntries = 0;
        for (int i = 0; i < palette.size(); i++) {
            NbtElement element = palette.get(i);
            if (element instanceof NbtCompound stateTag) {
                try {
                    paletteStates.add(parsePaletteEntry(stateTag, i));
                } catch (Exception e) {
                    failedEntries++;
                    LitematListMod.LOGGER.warn("调色板条目 #{} 解析失败，使用空气方块替代: {}", i, e.getMessage());
                    paletteStates.add(Blocks.AIR.getDefaultState());
                }
            } else {
                failedEntries++;
                LitematListMod.LOGGER.warn("调色板条目 #{} 不是 NbtCompound，类型: {}", i,
                        element != null ? element.getClass().getSimpleName() : "null");
                paletteStates.add(Blocks.AIR.getDefaultState());
            }
        }
        if (failedEntries > 0) {
            LitematListMod.LOGGER.warn("Region '{}' 有 {} 个调色板条目解析失败", regionName, failedEntries);
        }
        if (paletteStates.isEmpty()) {
            LitematListMod.LOGGER.warn("Region '{}' 调色板为空", regionName);
            return;
        }

        // 3. 读取 BlockStates
        NbtElement blockStatesElement = region.get("BlockStates");
        if (blockStatesElement == null) {
            LitematListMod.LOGGER.warn("Region '{}' 缺少 BlockStates", regionName);
            return;
        }

        long[] blockStates = null;
        int blockStatesType = blockStatesElement.getType();

        if (blockStatesType == NbtElement.LONG_ARRAY_TYPE) {
            blockStates = ((NbtLongArray) blockStatesElement).getLongArray();
            LitematListMod.LOGGER.info("Region '{}' BlockStates 类型: NbtLongArray ({} 个 long)", regionName, blockStates.length);
        } else if (blockStatesType == NbtElement.INT_ARRAY_TYPE) {
            // 兼容：某些工具可能使用 int[] 存储
            int[] intArray = ((NbtIntArray) blockStatesElement).getIntArray();
            blockStates = new long[intArray.length];
            for (int i = 0; i < intArray.length; i++) {
                blockStates[i] = intArray[i] & 0xFFFFFFFFL;
            }
            LitematListMod.LOGGER.info("Region '{}' BlockStates 类型: NbtIntArray ({} 个 int)", regionName, blockStates.length);
        } else if (blockStatesType == NbtElement.BYTE_ARRAY_TYPE) {
            // 兼容：Sponge 格式内嵌
            byte[] byteArray = ((NbtByteArray) blockStatesElement).getByteArray();
            int[] decoded = decodeVarIntArray(byteArray);
            int bitsPerBlock = Math.max(2, 32 - Integer.numberOfLeadingZeros(paletteStates.size() - 1));
            long mask = (1L << bitsPerBlock) - 1;
            int longCount = (decoded.length * bitsPerBlock + 63) / 64;
            blockStates = new long[longCount];
            for (int i = 0; i < decoded.length; i++) {
                int bitIndex = i * bitsPerBlock;
                int longIdx = bitIndex / 64;
                int bitOffset = bitIndex % 64;
                blockStates[longIdx] |= ((long) decoded[i] & mask) << bitOffset;
                if (bitOffset + bitsPerBlock > 64) {
                    blockStates[longIdx + 1] |= ((long) decoded[i] & mask) >>> (64 - bitOffset);
                }
            }
            LitematListMod.LOGGER.info("Region '{}' BlockStates 类型: NbtByteArray ({} 字节，解码后 {} 个 long)",
                    regionName, byteArray.length, blockStates.length);
        } else if (blockStatesType == NbtElement.LIST_TYPE) {
            NbtList list = (NbtList) blockStatesElement;
            blockStates = new long[list.size()];
            int validCount = 0;
            for (int i = 0; i < list.size(); i++) {
                NbtElement item = list.get(i);
                if (item instanceof NbtLong nbtLong) {
                    blockStates[i] = nbtLong.longValue();
                    validCount++;
                } else if (item instanceof NbtInt nbtInt) {
                    blockStates[i] = nbtInt.intValue() & 0xFFFFFFFFL;
                    validCount++;
                }
            }
            LitematListMod.LOGGER.info("Region '{}' BlockStates 类型: NbtList ({} 个 long，{} 有效)",
                    regionName, blockStates.length, validCount);
        } else {
            LitematListMod.LOGGER.warn("Region '{}' BlockStates 类型不支持: type={}",
                    regionName, blockStatesType);
            return;
        }

        if (blockStates == null || blockStates.length == 0) {
            LitematListMod.LOGGER.warn("Region '{}' BlockStates 为空", regionName);
            return;
        }

        // 4. 解码方块状态（连续位流，条目可跨 long 边界）
        int paletteSize = paletteStates.size();
        int bitsPerBlock = Math.max(2, 32 - Integer.numberOfLeadingZeros(paletteSize - 1));
        long mask = (1L << bitsPerBlock) - 1;
        // 「是否计算建筑材料」关闭时，方块（建筑材料）不计入材料列表
        boolean calcBuildingMaterials =
                com.litematlist.config.Configs.FeatureToggles.CALC_BUILDING_MATERIALS.getBooleanValue();
        // 「是否计算容器数据」：花盆内的植物等按容器数据归类统计
        boolean calcContainerData =
                com.litematlist.config.Configs.FeatureToggles.CALCULATE_CONTAINER_DATA.getBooleanValue();
        // 「是否计算原理图实体」：特殊纹饰陶罐的瓦片数据同属方块实体数据，本开关也参与门控
        boolean calcSchematicEntities =
                com.litematlist.config.Configs.FeatureToggles.CALC_SCHEMATIC_ENTITIES.getBooleanValue();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
        // 特殊纹饰陶罐预扫描：带非默认瓦片（非 brick）的陶罐，其方块本身以「含特殊瓦片的变种」
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 精确物品计入材料列表（ID 含 block_entity_data），而非普通陶罐方块。
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 精确物品计入材料列表（ID 含 BlockEntityTag.sherds 瓦片），而非普通陶罐方块。
//#endif
        // 瓦片数据来自方块实体，故「是否计算原理图实体」「是否计算容器数据」任一项开启即解析。
        Map<String, NbtList> specialPots = (calcContainerData || calcSchematicEntities)
                ? collectSpecialDecoratedPots(region, "TileEntities", posX, posY, posZ, filter)
                : Collections.emptyMap();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110

//#endif
        // 箱类方块状态表（区域相对坐标 "x,y,z" → 方块状态），供容器方块实体分类：
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 1.21.9+ 箱子类方块实体的 id 统一为 minecraft:chest（含全部铜箱子），无法从 id 区分家族；
//#elseif MC == 11701
//$$        // 箱子类方块实体的 id 可能统一化为 minecraft:chest，无法从 id 区分家族；
//#endif
        // 且方块状态的 type=left/right 是「属于大型箱子」的权威标记，孤立半箱也应判为大型箱子，
        // 不应依赖相邻方块判定（原理图状态墙等场景中半箱彼此并不相邻）。
        Map<String, BlockState> chestFamilyStates = new HashMap<>();
//#if MC == 12104
//$$
//#endif
//#if MC == 12110 || MC == 12111

//#endif
        int decodedBlocks = 0;
        int airCount = 0;

        for (int i = 0; i < totalBlocks; i++) {
            int bitIndex = i * bitsPerBlock;
            int longIndex = bitIndex / 64;
            int bitOffset = bitIndex % 64;
            int paletteIndex = (int) ((blockStates[longIndex] >>> bitOffset) & mask);
            if (bitOffset + bitsPerBlock > 64) {
                // 条目跨两个相邻 long，从下一个 long 取高位
                paletteIndex |= (int) ((blockStates[longIndex + 1] << (64 - bitOffset)) & mask);
            }
            // 渲染层过滤：索引映射与 LitematicaBlockStateContainer 一致
            if (filter != null) {
                int relX = posX + i % sx;
                int relZ = posZ + (i / sx) % sz;
                int relY = posY + i / (sx * sz);
                int relK = switch (filter.axis()) {
                    case 0 -> relX;
                    case 1 -> relY;
                    default -> relZ;
                };
                if (filter.contains(relK) == false) {
                    continue;
                }
            }
            if (paletteIndex >= 0 && paletteIndex < paletteStates.size()) {
                BlockState state = paletteStates.get(paletteIndex);
                if (!state.isAir()) {
                    // 记录箱类方块的实际状态（坐标与 TileEntities 的 x/y/z 同为区域相对坐标）
                    if (state.getBlock() instanceof ChestBlock) {
                        chestFamilyStates.put((i % sx) + "," + (i / (sx * sz)) + "," + ((i / sx) % sz), state);
                    }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    // 特殊纹饰陶罐：以精确ID（含 block_entity_data.sherds）计入，不再按普通陶罐方块统计。
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    // 特殊纹饰陶罐：以精确ID（含 BlockEntityTag.sherds 瓦片）计入，不再按普通陶罐方块统计。
//#endif
                    // 方块索引 → 区域内相对坐标：x 最快、其次 z、最慢 y（与 BlockStates 位流一致）。
                    NbtList potSherds = specialPots.get(
                            (i % sx) + "," + (i / (sx * sz)) + "," + ((i / sx) % sz));
                    if (potSherds != null) {
                        addDecoratedPotExact(potSherds, buildingSink.exactItems);
                    } else if (calcBuildingMaterials) {
                        Item item = state.getBlock().asItem();
                        int count = getBlockStateItemCount(state);
                        if (item != Items.AIR && count > 0) {
                            buildingSink.itemCounts.merge(item, count, Integer::sum);
                            buildingSink.itemNames.putIfAbsent(item, state.getBlock().getName().getString());
                        }
                        // 流体源头/含水方块/炼药锅液体（均属建筑材料）额外统计
                        countFluidSources(state, buildingSink.itemCounts, buildingSink.itemNames);
                    }
                    // 花盆：统计盆内种植的植物（归类为容器数据）
                    if (calcContainerData && com.litematlist.config.Configs.Statistics.enabled("flowerPot")) {
                        countFlowerPotContent(state, containerSink.itemCounts, containerSink.itemNames);
                    }
                    // 重生锚充能→萤石、末地传送门框架上的末影之眼（均归类为容器数据）
                    if (calcContainerData) {
                        countContainerBlockStateData(state, containerSink);
                    }
                } else {
                    airCount++;
                }
            }
            decodedBlocks++;
        }
        LitematListMod.LOGGER.info("Region '{}' 解码了 {} 个方块，其中空气 {} 个，非空气 {} 个",
                regionName, decodedBlocks, airCount, decodedBlocks - airCount);

        // 5. 读取容器数据（开关开启时统计 TileEntities 中的物品）
        if (calcContainerData) {
            parseTileEntityList(region, "TileEntities", containerSink, regionName, posX, posY, posZ, filter,
                    chestFamilyStates);
        }

        // 6. 读取实体（「是否计算原理图实体」开启时，统计矿车/盔甲架/船/箱船等实体对应的物品）
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (com.litematlist.config.Configs.FeatureToggles.CALC_SCHEMATIC_ENTITIES.getBooleanValue()) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        if (calcSchematicEntities) {
//#endif
            parseEntityList(region, entitySink, entityContainerSink, regionName, posX, posY, posZ, filter);
        }
    }

    /**
     * 花盆：按 {@code potted_*} 方块 ID 推导盆内植物对应的物品并计入统计。
     * 花盆内物品归类为容器数据，仅当「是否计算容器数据」与「统计-花盆」开关均开启时调用。
     * 少数花盆方块名与物品名不一致（杜鹃），单独映射；无法解析出物品时忽略。
     */
    private static void countFlowerPotContent(BlockState state, Map<Item, Integer> itemCounts,
                                              Map<Item, String> itemNames) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Identifier blockId = Registries.BLOCK.getId(state.getBlock());
//#elseif MC == 11701
//$$        Identifier blockId = Registry.BLOCK.getId(state.getBlock());
//#endif
        if (blockId == null || !"minecraft".equals(blockId.getNamespace())) {
            return;
        }
        String path = blockId.getPath();
        if (!path.startsWith("potted_")) {
            return;
        }
        String plant = path.substring("potted_".length());
        // 杜鹃花盆方块名为 potted_azalea_bush，但物品名为 azalea，单独映射
        if ("azalea_bush".equals(plant)) {
            plant = "azalea";
        } else if ("flowering_azalea_bush".equals(plant)) {
            plant = "flowering_azalea";
        }
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Item item = Registries.ITEM.get(Identifier.tryParse("minecraft:" + plant));
//#endif
//#if MC == 11701
//$$        Identifier plantId = Identifier.tryParse("minecraft:" + plant);
//#endif
//#if MC == 11701
//$$        if (plantId == null) {
//#endif
//#if MC == 11701
//$$            return;
//#endif
//#if MC == 11701
//$$        }
//#endif
//#if MC == 11701
//$$        Item item = Registry.ITEM.get(plantId);
//#endif
        if (item == null || item == Items.AIR) {
            return;
        }
        itemCounts.merge(item, 1, Integer::sum);
        itemNames.putIfAbsent(item, item.getName().getString());
    }

    /**
     * 解析原理图中的容器方块实体列表，将容器内物品并入材料统计。
     * 兼容 Litematica 原生格式（TileEntities，方块实体数据内联）
     * 与 Sponge 格式（v1: TileEntities + TileNBT 嵌套；v2+: BlockEntities）。
     */
    private static void parseTileEntityList(NbtCompound containerTag, String listKey,
                                            MaterialSink sink, String regionName,
                                            int regionPosX, int regionPosY, int regionPosZ,
                                            LitematicaBridge.LayerFilter filter,
                                            Map<String, BlockState> chestFamilyStates) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (containerTag.contains(listKey) == false) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        if (containerTag.contains(listKey, NbtElement.LIST_TYPE) == false) {
//#endif
            return;
        }
//#if MC == 12108 || MC == 12110 || MC == 12111
        Optional<NbtList> teListOpt = containerTag.getList(listKey);
//#elseif MC == 12101 || MC == 12104
//$$        NbtList teList = containerTag.getList(listKey, 10);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        if (teListOpt.isEmpty()) {
//#elseif MC == 12101 || MC == 12104
//$$        if (teList.isEmpty()) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            return;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        NbtList teList = containerTag.getList(listKey, NbtElement.COMPOUND_TYPE);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        NbtList teList = teListOpt.get();
//#endif
        int containerItemCount = 0;

        // 预扫描：收集箱类方块实体坐标（用于相邻配对判定大型箱子）。
        // 配对是结构属性，与渲染层过滤无关，因此预扫描不做层过滤。
        Set<String> chestCoords = new HashSet<>();
        Set<String> trappedChestCoords = new HashSet<>();
        Set<String> copperChestCoords = new HashSet<>();
        for (int i = 0; i < teList.size(); i++) {
            NbtElement element = teList.get(i);
            if (!(element instanceof NbtCompound teTag)) {
                continue;
            }
//#if MC == 12108 || MC == 12110 || MC == 12111
            Optional<NbtCompound> tileNbtOpt = teTag.getCompound("TileNBT");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            NbtCompound tileNbt = tileNbtOpt.orElse(teTag);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            Optional<String> idOpt = tileNbt.getString("id");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            if (idOpt.isEmpty() || !teTag.contains("x") || !teTag.contains("y") || !teTag.contains("z")) {
//#endif
//#if MC == 12101 || MC == 12104
//$$            NbtCompound tileNbt = teTag.contains("TileNBT") ? teTag.getCompound("TileNBT") : teTag;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            NbtCompound tileNbt = teTag.contains("TileNBT", NbtElement.COMPOUND_TYPE)
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    ? teTag.getCompound("TileNBT") : teTag;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            String teId = tileNbt.getString("id");
//#endif
//#if MC == 12101 || MC == 12104
//$$            if (teId == null || teId.isEmpty() || !teTag.contains("x") || !teTag.contains("y") || !teTag.contains("z")) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (teId == null || teId.isEmpty()
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    || !teTag.contains("x") || !teTag.contains("y") || !teTag.contains("z")) {
//#endif
                continue;
            }
//#if MC == 12108 || MC == 12110 || MC == 12111
            String teId = idOpt.get();
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            String coord = teTag.getInt("x").orElse(0) + "," + teTag.getInt("y").orElse(0)
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    + "," + teTag.getInt("z").orElse(0);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            String coord = teTag.getInt("x") + "," + teTag.getInt("y") + "," + teTag.getInt("z");
//#endif
            if ("minecraft:chest".equals(teId)) {
                chestCoords.add(coord);
            } else if ("minecraft:trapped_chest".equals(teId)) {
                trappedChestCoords.add(coord);
            } else if (teId.contains("copper_chest")) {
                copperChestCoords.add(coord);
            }
        }

        for (int i = 0; i < teList.size(); i++) {
            NbtElement element = teList.get(i);
            if (!(element instanceof NbtCompound teTag)) {
                continue;
            }
            // 渲染层过滤：方块实体的 x/y/z 为区域内相对坐标（缺失时跳过过滤，保留原行为）
            if (filter != null && teTag.contains("x") && teTag.contains("y") && teTag.contains("z")) {
//#if MC == 12108 || MC == 12110 || MC == 12111
                int teX = teTag.getInt("x").orElse(0);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                int teX = teTag.getInt("x");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                int teY = teTag.getInt("y").orElse(0);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                int teY = teTag.getInt("y");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                int teZ = teTag.getInt("z").orElse(0);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                int teZ = teTag.getInt("z");
//#endif
                int relK = switch (filter.axis()) {
                    case 0 -> regionPosX + teX;
                    case 1 -> regionPosY + teY;
                    default -> regionPosZ + teZ;
                };
                if (filter.contains(relK) == false) {
                    continue;
                }
            }
            // 兼容 Sponge v1：方块实体数据嵌套在 TileNBT 键下
//#if MC == 12108 || MC == 12110 || MC == 12111
            Optional<NbtCompound> tileNbtOpt = teTag.getCompound("TileNBT");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            NbtCompound tileNbt = tileNbtOpt.orElse(teTag);
//#endif
//#if MC == 12101 || MC == 12104
//$$            NbtCompound tileNbt = teTag.contains("TileNBT") ? teTag.getCompound("TileNBT") : teTag;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            NbtCompound tileNbt = teTag.contains("TileNBT", NbtElement.COMPOUND_TYPE)
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    ? teTag.getCompound("TileNBT") : teTag;
//#endif

//#if MC == 12108 || MC == 12110 || MC == 12111
            Optional<String> idOpt = tileNbt.getString("id");
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            String teId = tileNbt.getString("id");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            if (idOpt.isEmpty()) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            if (teId == null || teId.isEmpty()) {
//#endif
                // 无 id 的方块实体（少数模组容器）：维持旧行为直接解析 Items
                containerItemCount += parseContainerItems(tileNbt, sink);
                continue;
            }
//#if MC == 12108 || MC == 12110 || MC == 12111
            String teId = idOpt.get();
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            int teX = teTag.getInt("x").orElse(0);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            int teY = teTag.getInt("y").orElse(0);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            int teZ = teTag.getInt("z").orElse(0);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            int teX = teTag.contains("x") ? teTag.getInt("x") : 0;
//#elseif MC == 12101 || MC == 12104
//$$            int teX = teTag.getInt("x");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            int teY = teTag.contains("y") ? teTag.getInt("y") : 0;
//#elseif MC == 12101 || MC == 12104
//$$            int teY = teTag.getInt("y");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            int teZ = teTag.contains("z") ? teTag.getInt("z") : 0;
//#elseif MC == 12101 || MC == 12104
//$$            int teZ = teTag.getInt("z");
//#endif
            String typeKey = classifyContainerTe(teId, teX, teY, teZ,
                    chestFamilyStates, chestCoords, trappedChestCoords, copperChestCoords);

            // 「统计」页类型开关：已知类型未开启则跳过；未知类型不受影响（维持旧行为）
            if (typeKey != null && com.litematlist.config.Configs.Statistics.enabled(typeKey) == false) {
                continue;
            }

            switch (typeKey == null ? "" : typeKey) {
                case "lectern" -> {
                    // 讲台：书带非默认组件（成书内容/重命名等）时按精确ID单独统计，否则按「书与笔」计 1 本
                    if (tileNbt.contains("Book")) {
                        NbtElement bookElement = tileNbt.get("Book");
                        boolean counted = false;
                        if (bookElement instanceof NbtCompound bookTag && nonDefaultComponents(bookTag) != null) {
                            counted = addItemEntry(bookTag, sink.itemCounts, sink.itemNames, sink.exactItems);
                        }
                        if (counted == false) {
                            addSpecialItem(Items.WRITABLE_BOOK, sink.itemCounts, sink.itemNames);
                        }
                        containerItemCount++;
                    }
                }
                case "chiseledBookshelf" ->
                        containerItemCount += countChiseledBookshelfBooks(tileNbt, sink.itemCounts, sink.itemNames, sink.exactItems);
                case "decoratedPot" ->
                        containerItemCount += countDecoratedPotItem(tileNbt, sink);
                case "jukebox" ->
                        containerItemCount += countJukeboxRecord(tileNbt, sink);
                default -> containerItemCount += parseContainerItems(tileNbt, sink);
            }
        }

        if (containerItemCount > 0) {
            LitematListMod.LOGGER.info("Region '{}' 容器数据({})解析: 计入了 {} 个物品",
                    regionName, listKey, containerItemCount);
        }
    }

    /**
     * 箱子类方块实体分类：优先按所在位置的实际方块状态判断（方块 id 区分箱子家族，
     * type=left/right 判定大型箱）；无方块状态表时回退为「方块实体 id + 水平相邻
     * （±1 于 x/z、同 y）同类配对」判定。其余已知容器/特殊方块实体返回对应
     * 「统计」页类型 key；未知返回 null。
     */
    private static String classifyContainerTe(String teId, int x, int y, int z,
                                              Map<String, BlockState> chestFamilyStates,
                                              Set<String> chestCoords,
                                              Set<String> trappedChestCoords,
                                              Set<String> copperChestCoords) {
        // 优先按方块实体所在位置的实际方块状态分类（精确到箱子家族与半箱类型）
        BlockState state = chestFamilyStates == null ? null : chestFamilyStates.get(x + "," + y + "," + z);
        if (state != null && state.getBlock() instanceof ChestBlock) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            String path = Registries.BLOCK.getId(state.getBlock()).getPath();
//#elseif MC == 11701
//$$            String path = Registry.BLOCK.getId(state.getBlock()).getPath();
//#endif
            boolean large = state.contains(ChestBlock.CHEST_TYPE)
                    && state.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE;
            if (path.contains("copper_chest")) {
                return large ? "largeCopperChest" : "copperChest";
            }
            if ("trapped_chest".equals(path)) {
                return large ? "largeTrappedChest" : "trappedChest";
            }
            if ("chest".equals(path)) {
                return large ? "largeChest" : "chest";
            }
        }
        // 回退（Sponge 格式等无方块状态表场景）：按方块实体 id + 相邻同类判定
        if ("minecraft:chest".equals(teId)) {
            return hasAdjacentSameType(chestCoords, x, y, z) ? "largeChest" : "chest";
        }
        if ("minecraft:trapped_chest".equals(teId)) {
            return hasAdjacentSameType(trappedChestCoords, x, y, z) ? "largeTrappedChest" : "trappedChest";
        }
        if (teId.contains("copper_chest")) {
            return hasAdjacentSameType(copperChestCoords, x, y, z) ? "largeCopperChest" : "copperChest";
        }
        if (teId.endsWith("_shulker_box")) {
            return "shulkerBox";
        }
        return switch (teId) {
            case "minecraft:furnace" -> "furnace";
            case "minecraft:smoker" -> "smoker";
            case "minecraft:blast_furnace" -> "blastFurnace";
            case "minecraft:brewing_stand" -> "brewingStand";
            case "minecraft:dispenser" -> "dispenser";
            case "minecraft:dropper" -> "dropper";
            case "minecraft:hopper" -> "hopper";
            case "minecraft:crafter" -> "crafter";
            case "minecraft:barrel" -> "barrel";
            case "minecraft:decorated_pot" -> "decoratedPot";
            case "minecraft:chiseled_bookshelf" -> "chiseledBookshelf";
            case "minecraft:lectern" -> "lectern";
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 展示架（1.21.9+，各木种共用方块实体 id）与唱片机
//#endif
            case "minecraft:shelf" -> "shelf";
            case "minecraft:jukebox" -> "jukebox";
            default -> null;
        };
    }

    /** 同类型箱是否存在于水平相邻位置（vanilla 相邻同类型箱必合并为大型箱）。 */
    private static boolean hasAdjacentSameType(Set<String> coords, int x, int y, int z) {
        return coords.contains((x + 1) + "," + y + "," + z)
                || coords.contains((x - 1) + "," + y + "," + z)
                || coords.contains(x + "," + y + "," + (z + 1))
                || coords.contains(x + "," + y + "," + (z - 1));
    }

    /**
     * 雕文书架：只统计其中包含的书本数量，附魔书按普通书计算。
     * 每个占用槽（有合法物品 id 且非空气）计 1 本 minecraft:book；
     * 带非默认组件（附魔/成书内容等）的书以精确ID单独统计，不并入普通书。
     */
    private static int countChiseledBookshelfBooks(NbtCompound tileNbt, Map<Item, Integer> itemCounts,
                                                   Map<Item, String> itemNames,
                                                   Map<String, ExactItem> exactItems) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (tileNbt.contains("Items") == false) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        if (tileNbt.contains("Items", NbtElement.LIST_TYPE) == false) {
//#endif
            return 0;
        }
//#if MC == 12108 || MC == 12110 || MC == 12111
        Optional<NbtList> itemsOpt = tileNbt.getList("Items");
//#elseif MC == 12101 || MC == 12104
//$$        NbtList items = tileNbt.getList("Items", 10);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        if (itemsOpt.isEmpty()) {
//#elseif MC == 12101 || MC == 12104
//$$        if (items.isEmpty()) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            return 0;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        NbtList items = tileNbt.getList("Items", NbtElement.COMPOUND_TYPE);
//#endif
        int books = 0;
        int exactBooks = 0;
//#if MC == 12108 || MC == 12110 || MC == 12111
        for (int i = 0; i < itemsOpt.get().size(); i++) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$        for (int i = 0; i < items.size(); i++) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            NbtElement element = itemsOpt.get().get(i);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            NbtElement element = items.get(i);
//#endif
            if (!(element instanceof NbtCompound itemTag)) {
                continue;
            }
//#if MC == 12108 || MC == 12110 || MC == 12111
            Optional<String> idOpt = itemTag.getString("id");
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            String id = itemTag.getString("id");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            if (idOpt.isEmpty()) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            if (id == null || id.isEmpty()) {
//#endif
                continue;
            }
//#if MC == 12108 || MC == 12110 || MC == 12111
            Identifier itemId = Identifier.tryParse(idOpt.get());
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            Identifier itemId = Identifier.tryParse(id);
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (itemId == null || Registries.ITEM.get(itemId) == Items.AIR) {
//#elseif MC == 11701
//$$            if (itemId == null || Registry.ITEM.get(itemId) == Items.AIR) {
//#endif
                continue;
            }
            if (nonDefaultComponents(itemTag) != null) {
                // 带组件的书（附魔书/成书等）：以精确ID单独统计，不再并入普通书
                if (addItemEntry(itemTag, itemCounts, itemNames, exactItems)) {
                    exactBooks++;
                }
                continue;
            }
            books++;
        }
        if (books > 0) {
            addSpecialItem(Items.BOOK, itemCounts, itemNames, books);
        }
        return books + exactBooks;
    }

    /**
     * 预扫描方块实体列表，收集「具有特殊纹饰」的陶罐：sherds 中存在非 brick 瓦片。
     * 返回：区域内相对坐标 "x,y,z" → 该陶罐的 sherds 列表副本。
     * 层过滤与 parseTileEntityList 保持一致（x/y/z 为区域相对坐标，比较时叠加区域原点）。
     */
    private static Map<String, NbtList> collectSpecialDecoratedPots(NbtCompound region, String listKey,
                                                                    int regionPosX, int regionPosY, int regionPosZ,
                                                                    LitematicaBridge.LayerFilter filter) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (region.contains(listKey) == false) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        if (region.contains(listKey, NbtElement.LIST_TYPE) == false) {
//#endif
            return Collections.emptyMap();
        }
//#if MC == 12108 || MC == 12110 || MC == 12111
        Optional<NbtList> teListOpt = region.getList(listKey);
//#elseif MC == 12101 || MC == 12104
//$$        NbtList teList = region.getList(listKey, 10);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        if (teListOpt.isEmpty()) {
//#elseif MC == 12101 || MC == 12104
//$$        if (teList.isEmpty()) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            return Collections.emptyMap();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        NbtList teList = region.getList(listKey, NbtElement.COMPOUND_TYPE);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        NbtList teList = teListOpt.get();
//#endif
        Map<String, NbtList> result = new LinkedHashMap<>();
        for (int i = 0; i < teList.size(); i++) {
            NbtElement element = teList.get(i);
            if (!(element instanceof NbtCompound teTag)) {
                continue;
            }
            // 兼容 Sponge v1：数据嵌套在 TileNBT 键下
//#if MC == 12108 || MC == 12110 || MC == 12111
            NbtCompound tileNbt = teTag.getCompound("TileNBT").orElse(teTag);
//#elseif MC == 12101 || MC == 12104
//$$            NbtCompound tileNbt = teTag.contains("TileNBT") ? teTag.getCompound("TileNBT") : teTag;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            Optional<String> idOpt = tileNbt.getString("id");
//#elseif MC == 12101 || MC == 12104
//$$            String teId = tileNbt.getString("id");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            if (idOpt.isEmpty() || !"minecraft:decorated_pot".equals(idOpt.get())) {
//#elseif MC == 12101 || MC == 12104
//$$            if (teId == null || !"minecraft:decorated_pot".equals(teId)) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            NbtCompound tileNbt = teTag.contains("TileNBT", NbtElement.COMPOUND_TYPE)
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    ? teTag.getCompound("TileNBT") : teTag;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            String idValue = tileNbt.getString("id");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (idValue == null || !"minecraft:decorated_pot".equals(idValue)) {
//#endif
                continue;
            }
//#if MC == 12108 || MC == 12110 || MC == 12111
            Optional<NbtList> sherdsOpt = tileNbt.getList("sherds");
//#elseif MC == 12101 || MC == 12104
//$$            NbtList sherds = tileNbt.getList("sherds", 8);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            if (sherdsOpt.isEmpty() || !hasSpecialSherds(sherdsOpt.get())) {
//#elseif MC == 12101 || MC == 12104
//$$            if (sherds.isEmpty() || !hasSpecialSherds(sherds)) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (!tileNbt.contains("sherds", NbtElement.LIST_TYPE)) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                continue;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            NbtList sherds = tileNbt.getList("sherds", NbtElement.STRING_TYPE);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (!hasSpecialSherds(sherds)) {
//#endif
                continue;
            }
            boolean hasCoords = teTag.contains("x") && teTag.contains("y") && teTag.contains("z");
//#if MC == 12108 || MC == 12110 || MC == 12111
            int teX = teTag.getInt("x").orElse(0);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            int teX = teTag.getInt("x");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            int teY = teTag.getInt("y").orElse(0);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            int teY = teTag.getInt("y");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            int teZ = teTag.getInt("z").orElse(0);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            int teZ = teTag.getInt("z");
//#endif
            if (filter != null && hasCoords) {
                int relK = switch (filter.axis()) {
                    case 0 -> regionPosX + teX;
                    case 1 -> regionPosY + teY;
                    default -> regionPosZ + teZ;
                };
                if (filter.contains(relK) == false) {
                    continue;
                }
            }
//#if MC == 12108 || MC == 12110 || MC == 12111
            result.put(teX + "," + teY + "," + teZ, (NbtList) sherdsOpt.get().copy());
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            result.put(teX + "," + teY + "," + teZ, (NbtList) sherds.copy());
//#endif
        }
        return result;
    }

    /** sherds 中是否存在非默认瓦片（minecraft:brick 为无纹饰默认值）。 */
    private static boolean hasSpecialSherds(NbtList sherds) {
        for (int i = 0; i < sherds.size(); i++) {
//#if MC == 12108 || MC == 12110 || MC == 12111
            Optional<String> value = sherds.getString(i);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            String value = sherds.getString(i);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            if (value.isPresent() && "minecraft:brick".equals(value.get()) == false) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            if (value != null && value.isEmpty() == false && "minecraft:brick".equals(value) == false) {
//#elseif MC == 12101 || MC == 12104
//$$            if (value != null && "minecraft:brick".equals(value) == false) {
//#endif
                return true;
            }
        }
        return false;
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 把「带特殊瓦片的陶罐」以精确物品计入：ID 形如
     * {@code minecraft:decorated_pot[pot_decorations=["minecraft:flow_pottery_sherd",...]]}。
     * 1.20.5+ 起陶罐纹饰使用物品组件 {@code minecraft:pot_decorations}（瓦片 ID 列表），
     * 该组件驱动物品模型（按瓦片显示纹饰材质）与悬停提示（名称下方四行瓦片信息）；
     * 旧实现用 {@code block_entity_data.sherds} 属方块实体数据，物品模型/提示均不识别，
     * 故图标恒为无纹饰默认材质、悬停也无四行瓦片信息。
     */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /**
//$$     * 把「带特殊瓦片的陶罐」以精确物品计入：本版本为物品 NBT 时代，陶罐纹饰存放于
//$$     * {@code tag.BlockEntityTag.sherds}（瓦片 ID 列表），物品模型与悬停提示均从该处读取；
//$$     * 精确ID 形如 {@code minecraft:decorated_pot{BlockEntityTag:{id:"minecraft:decorated_pot",
//$$     * sherds:["minecraft:flow_pottery_sherd",...]}}}。
//$$     * （1.20.5+ 起改为物品组件 {@code minecraft:pot_decorations}，本版本不适用。）
//$$     */
//#endif
    private static void addDecoratedPotExact(NbtList sherds, Map<String, ExactItem> exactItems) {
        if (exactItems == null) {
            return;
        }
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Item item = Items.DECORATED_POT;
//#elseif MC == 11701
//$$        Item item = blockItem("decorated_pot");
//#endif
        if (item == null || item == Items.AIR) {
            return;
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        NbtCompound components = new NbtCompound();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        components.put("minecraft:pot_decorations", sherds);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        String exactId = buildExactId(Registries.ITEM.getId(item), components, false);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        // 与游戏写出的物品形态一致：BlockEntityTag 含 id（方块实体写出）与 sherds（瓦片列表）
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        NbtCompound blockEntityTag = new NbtCompound();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        blockEntityTag.putString("id", "minecraft:decorated_pot");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        blockEntityTag.put("sherds", sherds.copy());
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        NbtCompound tag = new NbtCompound();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        tag.put("BlockEntityTag", blockEntityTag);
//#endif
//#if MC == 11701
//$$        String exactId = buildExactId(Registry.ITEM.getId(item), tag, true);
//#elseif MC == 11904 || MC == 12001
//$$        String exactId = buildExactId(Registries.ITEM.getId(item), tag, true);
//#endif
        NbtCompound itemTag = new NbtCompound();
        itemTag.putString("id", "minecraft:decorated_pot");
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        itemTag.putInt("count", 1);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        itemTag.putInt("Count", 1);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        itemTag.put("components", components);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        itemTag.put("tag", tag);
//#endif
        addExactItem(exactItems, exactId, item, decodeExactStack(itemTag, item, 1), 1);
    }

    /**
     * 陶罐：单格存储物品，读 "item" 键（新格式为 {id, count/Count} 复合标签，
     * 旧格式为物品 id 字符串），计入材料统计。
     */
    private static int countDecoratedPotItem(NbtCompound tileNbt, MaterialSink sink) {
        if (tileNbt.contains("item") == false) {
            return 0;
        }
        NbtElement itemElement = tileNbt.get("item");
        if (itemElement instanceof NbtCompound itemTag) {
            // 统一入口：带非默认组件时以精确ID单独统计
            if (addItemEntry(itemTag, sink.itemCounts, sink.itemNames, sink.exactItems) == false) {
                return 0;
            }
            // 罐内存物若本身带容器数据（潜影盒/收纳袋等），其内部物品一并解析
            parseItemEmbeddedContainer(itemTag, sink, 1);
            return 1;
        } else if (itemElement instanceof NbtString idTag) {
//#if MC == 12108 || MC == 12110 || MC == 12111
            Optional<String> idValue = idTag.asString();
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            String idValue = idTag.asString();
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            if (idValue.isEmpty()) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            if (idValue == null || idValue.isEmpty()) {
//#endif
                return 0;
            }
//#if MC == 12108 || MC == 12110 || MC == 12111
            Identifier itemId = Identifier.tryParse(idValue.get());
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            Identifier itemId = Identifier.tryParse(idValue);
//#endif
            if (itemId == null) {
                return 0;
            }
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            Item item = Registries.ITEM.get(itemId);
//#elseif MC == 11701
//$$            Item item = Registry.ITEM.get(itemId);
//#endif
            if (item == null || item == Items.AIR) {
                return 0;
            }
            addSpecialItem(item, sink.itemCounts, sink.itemNames);
            return 1;
        }
        return 0;
    }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 唱片机：统计机内放入的唱片（归类为容器数据）。1.21+ 唱片内容保存在方块实体的
     * RecordItem 复合标签中；为避免不同版本键名差异，依次尝试常见键名，
     * 均未命中时回退为通用 Items 列表解析。返回计入的物品数。
     */
//#elseif MC == 11701 || MC == 11904
//$$    /**
//$$     * 唱片机：统计机内放入的唱片（归类为容器数据）。不同版本唱片内容键名不同，
//$$     * 依次尝试常见键名，均未命中时回退为通用 Items 列表解析。返回计入的物品数。
//$$     */
//#endif
    private static int countJukeboxRecord(NbtCompound tileNbt, MaterialSink sink) {
        for (String key : new String[]{"RecordItem", "record_item", "Record"}) {
            NbtElement element = tileNbt.get(key);
            if (element instanceof NbtCompound recordTag
                    && addItemEntry(recordTag, sink.itemCounts, sink.itemNames, sink.exactItems)) {
                return 1;
            }
        }
        return parseContainerItems(tileNbt, sink);
    }

    /**
     * 从单个方块实体 NBT 中读取 Items 列表并计入材料统计，返回计入的物品数。
     */
    private static int parseContainerItems(NbtCompound tileNbt, MaterialSink sink) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (tileNbt.contains("Items") == false) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        if (tileNbt.contains("Items", NbtElement.LIST_TYPE) == false) {
//#endif
            return 0;
        }
//#if MC == 12108 || MC == 12110 || MC == 12111
        Optional<NbtList> itemsOpt = tileNbt.getList("Items");
//#elseif MC == 12101 || MC == 12104
//$$        NbtList items = tileNbt.getList("Items", 10);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        if (itemsOpt.isEmpty()) {
//#elseif MC == 12101 || MC == 12104
//$$        if (items.isEmpty()) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            return 0;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        NbtList items = tileNbt.getList("Items", NbtElement.COMPOUND_TYPE);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        return parseItemList(itemsOpt.get(), sink);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$        return parseItemList(items, sink);
//#endif
    }

    /** 嵌套容器（如箱子/陶罐内放潜影盒、收纳袋再套潜影盒）解析的最大递归深度，防异常数据无限递归。 */
    private static final int MAX_CONTAINER_NEST_DEPTH = 8;

    /**
     * 解析物品列表（条目为 {id, count/Count}，如容器 Items），计入材料统计，返回计入的物品数。
     * 带非默认组件的特殊物品以精确ID单独归并；普通物品并入 itemCounts/itemNames。
     * 条目自身若为潜影盒/收纳袋等自带容器的物品，其内部物品一并解析（陶罐内放潜影盒等场景）。
     */
    private static int parseItemList(NbtList items, MaterialSink sink) {
        return parseItemList(items, sink, 0);
    }

    private static int parseItemList(NbtList items, MaterialSink sink, int depth) {
        int count = 0;

        for (int i = 0; i < items.size(); i++) {
            NbtElement element = items.get(i);
            if (!(element instanceof NbtCompound itemTag)) {
                continue;
            }
            if (addItemEntry(itemTag, sink.itemCounts, sink.itemNames, sink.exactItems)) {
                count++;
                // 容器内物品自身可能带容器数据（潜影盒/收纳袋等），继续解析其内部物品
                if (depth < MAX_CONTAINER_NEST_DEPTH) {
                    count += parseItemEmbeddedContainer(itemTag, sink, depth + 1);
                }
            }
        }

        return count;
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 解析原理图 Region 中的实体列表（开关「是否计算原理图实体」开启时调用）。
     * 实体 "Pos" 为区域相对坐标（相对区域最小角，double 三元组），据此做渲染层过滤。
     * 支持：
     *  - 可直接映射物品的实体 id（minecraft:minecart/chest_minecart/hopper_minecart、
     *    armor_stand、tnt_minecart、furnace_minecart、spawner_minecart 及新版按木种拆分的
     *    船实体如 oak_chest_boat、bamboo_raft）
     *  - 旧格式通用船实体 minecraft:boat / minecraft:chest_boat 带 "Type" 字段
     *    （oak/spruce/.../bamboo/pale_oak），映射为 <type>_boat / <type>_chest_boat
     *    （bamboo 为 bamboo_raft / bamboo_chest_raft）
     * 容器实体（漏斗矿车、箱船、驴背箱子等）在「是否计算原理图实体容器数据」
     * 开启时统计其容器数据；盔甲架穿戴/手持的物品跟随本开关一并统计。
     */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /**
//$$     * 解析原理图 Region 中的实体列表（开关「是否计算原理图实体」开启时调用）。
//$$     * 实体 "Pos" 为区域相对坐标（相对区域最小角，double 三元组），据此做渲染层过滤。
//$$     * 支持：
//$$     *  - 可直接映射物品的实体 id（minecraft:minecart/chest_minecart/hopper_minecart、
//$$     *    armor_stand、tnt_minecart、furnace_minecart、spawner_minecart 及新版按木种拆分的
//$$     *    船实体如 oak_chest_boat、bamboo_raft）
//$$     *  - 旧格式通用船实体 minecraft:boat / minecraft:chest_boat 带 "Type" 字段
//$$     *    （oak/spruce/.../bamboo/pale_oak），映射为 &lt;type&gt;_boat / &lt;type&gt;_chest_boat
//$$     *    （bamboo 为 bamboo_raft / bamboo_chest_raft）
//$$     * 容器实体（漏斗矿车、箱船、驴背箱子等）在「是否计算原理图实体容器数据」
//$$     * 开启时统计其容器数据；盔甲架穿戴/手持的物品跟随本开关一并统计。
//$$     */
//#endif
    private static void parseEntityList(NbtCompound region, MaterialSink entitySink,
                                        MaterialSink entityContainerSink,
                                        String regionName,
                                        int regionPosX, int regionPosY, int regionPosZ,
                                        LitematicaBridge.LayerFilter filter) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (region.contains("Entities") == false) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        if (region.contains("Entities", NbtElement.LIST_TYPE) == false) {
//#endif
            return;
        }
//#if MC == 12108 || MC == 12110 || MC == 12111
        Optional<NbtList> entitiesOpt = region.getList("Entities");
//#elseif MC == 12101 || MC == 12104
//$$        NbtList entities = region.getList("Entities", 10);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        if (entitiesOpt.isEmpty()) {
//#elseif MC == 12101 || MC == 12104
//$$        if (entities.isEmpty()) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            return;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        NbtList entities = region.getList("Entities", NbtElement.COMPOUND_TYPE);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        NbtList entities = entitiesOpt.get();
//#endif
        boolean calcEntityContainers =
                com.litematlist.config.Configs.FeatureToggles.CALC_SCHEMATIC_ENTITY_CONTAINERS.getBooleanValue();
        int entityCount = 0;
        int containerItemCount = 0;

        for (int i = 0; i < entities.size(); i++) {
            NbtElement element = entities.get(i);
            if (!(element instanceof NbtCompound entityTag)) {
                continue;
            }
            // 渲染层过滤：Pos 为区域相对坐标（相对区域最小角）
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (filter != null && entityTag.contains("Pos")) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            if (filter != null && entityTag.contains("Pos", NbtElement.LIST_TYPE)) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                Optional<NbtList> posOpt = entityTag.getList("Pos");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                if (posOpt.isPresent() && posOpt.get().size() >= 3) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    NbtList pos = posOpt.get();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                NbtList pos = entityTag.getList("Pos", NbtElement.DOUBLE_TYPE);
//#elseif MC == 12101 || MC == 12104
//$$                NbtList pos = entityTag.getList("Pos", 6);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                if (pos.size() >= 3) {
//#endif
                    double px = listNumber(pos.get(0));
                    double py = listNumber(pos.get(1));
                    double pz = listNumber(pos.get(2));
                    int relK = switch (filter.axis()) {
                        case 0 -> regionPosX + (int) Math.floor(px);
                        case 1 -> regionPosY + (int) Math.floor(py);
                        default -> regionPosZ + (int) Math.floor(pz);
                    };
                    if (filter.contains(relK) == false) {
                        continue;
                    }
                }
            }
//#if MC == 12108 || MC == 12110 || MC == 12111
            Optional<String> idOpt = entityTag.getString("id");
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            String entityId = entityTag.getString("id");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            if (idOpt.isEmpty()) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            if (entityId == null || entityId.isEmpty()) {
//#endif
                continue;
            }
//#if MC == 12108 || MC == 12110 || MC == 12111
            String entityId = idOpt.get();
//#endif

            // 实体本身对应的物品（矿车/盔甲架/船/箱船等）
            Item entityItem = resolveEntityItem(entityId, entityTag);
            if (entityItem != null && entityItem != Items.AIR) {
                entitySink.itemCounts.merge(entityItem, 1, Integer::sum);
                entitySink.itemNames.putIfAbsent(entityItem, entityItem.getName().getString());
                entityCount++;
            }

            // 盔甲架：装备栏（新版 equipment 复合标签 / 旧版 ArmorItems+HandItems 列表）
            // 跟随「是否计算原理图实体」一并统计（盔甲物品 + 纹饰模板 + 镶嵌材质），
            // 受「统计」页「盔甲架上的盔甲」开关门控（默认开启）
            if ("minecraft:armor_stand".equals(entityId)) {
                if (com.litematlist.config.Configs.Statistics.enabled("armorStandArmor")) {
                    containerItemCount += parseArmorStandItems(entityTag, entitySink.itemCounts,
                            entitySink.itemNames, entitySink.exactItems);
                }
            }

            // 物品展示框/发光物品展示框：统计框内展示的物品（归类为实体数据）；
            // 受「统计」页对应展示框开关门控；框内含物品的潜影盒/收纳袋内容归类为实体容器数据
            if ("minecraft:item_frame".equals(entityId) || "minecraft:glow_item_frame".equals(entityId)) {
                containerItemCount += parseItemFrameContent(entityId, entityTag, entitySink,
                        entityContainerSink, calcEntityContainers);
            }

            // 傀儡/凋零等实体的召唤材料（按实体数量 × 配方材料）
            countSummonMaterials(entityId, entityTag, entitySink.itemCounts, entitySink.itemNames);

            // 容器实体：统计容器数据
            if (calcEntityContainers) {
                containerItemCount += parseEntityContainer(entityId, entityTag, entityContainerSink);
            }
        }

        if (entityCount > 0 || containerItemCount > 0) {
            LitematListMod.LOGGER.info("Region '{}' 实体解析: 计入了 {} 个实体、{} 个实体容器物品",
                    regionName, entityCount, containerItemCount);
        }
    }

    /**
     * 物品展示框/发光物品展示框：读取框内展示的物品（实体 "Item" 复合标签）。
     * 展示的物品归类为实体数据，受「统计」页对应展示框开关门控；
     * 若展示物品为内含物品的潜影盒/收纳袋，其内部物品归类为实体容器数据，
     * 需「是否计算原理图实体容器数据」开关开启。返回计入的物品数。
     */
    private static int parseItemFrameContent(String entityId, NbtCompound entityTag,
                                             MaterialSink entitySink, MaterialSink entityContainerSink,
                                             boolean calcEntityContainers) {
        String typeKey = "minecraft:glow_item_frame".equals(entityId) ? "glowItemFrame" : "itemFrame";
        if (com.litematlist.config.Configs.Statistics.enabled(typeKey) == false) {
            return 0;
        }
        if (entityTag.contains("Item") == false) {
            return 0;
        }
        NbtElement itemElement = entityTag.get("Item");
        if (!(itemElement instanceof NbtCompound frameItemTag)) {
            return 0;
        }
        int count = 0;
        if (addItemEntry(frameItemTag, entitySink.itemCounts, entitySink.itemNames, entitySink.exactItems)) {
            count++;
        }
        if (calcEntityContainers) {
            count += parseItemEmbeddedContainer(frameItemTag, entityContainerSink);
        }
        return count;
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 解析物品内嵌容器中的物品（展示框内潜影盒/收纳袋、以及任意容器内的潜影盒/收纳袋等），
     * 归类为容器数据。兼容现代组件格式：潜影盒 {@code components."minecraft:container"}
     * （{slot,item} 条目）、收纳袋 {@code components."minecraft:bundle_contents"}（物品条目列表）；
     * 以及旧格式：物品携带的 {@code BlockEntityTag.Items} 或顶层 {@code Items}。
     * depth 用于限制嵌套容器递归深度（潜影盒内再套潜影盒等）。
     */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    /**
//$$     * 解析物品内嵌容器中的物品（展示框内潜影盒/收纳袋、以及任意容器内的潜影盒/收纳袋等），
//$$     * 归类为容器数据。兼容现代组件格式：潜影盒 {@code components."minecraft:container"}
//$$     * （{slot,item} 条目）、收纳袋 {@code components."minecraft:bundle_contents"}（物品条目列表）；
//$$     * 以及旧格式（本版本原生）：物品自定义数据在 {@code tag} 键内——
//$$     * 潜影盒/箱类 {@code tag.BlockEntityTag.Items}、收纳袋 {@code tag.Items}。
//$$     * depth 用于限制嵌套容器递归深度（潜影盒内再套潜影盒等）。
//$$     */
//#endif
    private static int parseItemEmbeddedContainer(NbtCompound itemTag, MaterialSink sink) {
        return parseItemEmbeddedContainer(itemTag, sink, 0);
    }

    private static int parseItemEmbeddedContainer(NbtCompound itemTag, MaterialSink sink, int depth) {
        int count = 0;
//#if MC == 12108 || MC == 12110 || MC == 12111
        Optional<NbtCompound> componentsOpt = itemTag.getCompound("components");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        if (componentsOpt.isPresent()) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            NbtCompound components = componentsOpt.get();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (itemTag.contains("components", NbtElement.COMPOUND_TYPE)) {
//#elseif MC == 12101 || MC == 12104
//$$        NbtCompound components = itemTag.getCompound("components");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            NbtCompound components = itemTag.getCompound("components");
//#elseif MC == 12101 || MC == 12104
//$$        if (components.isEmpty() == false) {
//#endif
            // 潜影盒等容器组件：minecraft:container -> [{slot, item:{id,count,...}}, ...]
//#if MC == 12108 || MC == 12110 || MC == 12111
            Optional<NbtList> containerOpt = components.getList("minecraft:container");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            if (containerOpt.isPresent()) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                NbtList slots = containerOpt.get();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (components.contains("minecraft:container", NbtElement.LIST_TYPE)) {
//#elseif MC == 12101 || MC == 12104
//$$            NbtList slots = components.getList("minecraft:container", 10);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                NbtList slots = components.getList("minecraft:container", NbtElement.COMPOUND_TYPE);
//#elseif MC == 12101 || MC == 12104
//$$            if (slots.isEmpty() == false) {
//#endif
                for (int i = 0; i < slots.size(); i++) {
                    if (!(slots.get(i) instanceof NbtCompound slotEntry)) {
                        continue;
                    }
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    if (slotEntry.contains("item", NbtElement.COMPOUND_TYPE) == false) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        continue;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    Optional<NbtCompound> innerOpt = slotEntry.getCompound("item");
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                    NbtCompound inner = slotEntry.getCompound("item");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    if (innerOpt.isPresent()
//#elseif MC == 12101 || MC == 12104
//$$                    if (inner.isEmpty() == false
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                            && addItemEntry(innerOpt.get(), sink.itemCounts, sink.itemNames, sink.exactItems)) {
//#elseif MC == 12101 || MC == 12104
//$$                            && addItemEntry(inner, sink.itemCounts, sink.itemNames, sink.exactItems)) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    if (addItemEntry(inner, sink.itemCounts, sink.itemNames, sink.exactItems)) {
//#endif
                        count++;
                        // 容器内再放的潜影盒/收纳袋同样展开其内部物品
                        if (depth < MAX_CONTAINER_NEST_DEPTH) {
//#if MC == 12108 || MC == 12110 || MC == 12111
                            count += parseItemEmbeddedContainer(innerOpt.get(), sink, depth + 1);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                            count += parseItemEmbeddedContainer(inner, sink, depth + 1);
//#endif
                        }
                    }
                }
            }
            // 收纳袋组件：minecraft:bundle_contents -> [{id,count,...}, ...]
//#if MC == 12108 || MC == 12110 || MC == 12111
            Optional<NbtList> bundleOpt = components.getList("minecraft:bundle_contents");
//#elseif MC == 12101 || MC == 12104
//$$            NbtList bundle = components.getList("minecraft:bundle_contents", 10);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            if (bundleOpt.isPresent() && depth < MAX_CONTAINER_NEST_DEPTH) {
//#elseif MC == 12101 || MC == 12104
//$$            if (bundle.isEmpty() == false && depth < MAX_CONTAINER_NEST_DEPTH) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                count += parseItemList(bundleOpt.get(), sink, depth + 1);
//#elseif MC == 12101 || MC == 12104
//$$                count += parseItemList(bundle, sink, depth + 1);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (components.contains("minecraft:bundle_contents", NbtElement.LIST_TYPE)
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    && depth < MAX_CONTAINER_NEST_DEPTH) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                count += parseItemList(components.getList("minecraft:bundle_contents", NbtElement.COMPOUND_TYPE),
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        sink, depth + 1);
//#endif
            }
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 旧格式（1.20.4- 及更早）：物品自定义数据在 tag 键内——
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        // 旧格式（本版本原生格式）：物品自定义数据在 tag 键内——
//#endif
        // 潜影盒/箱类 tag.BlockEntityTag.Items、收纳袋 tag.Items
//#if MC == 12108 || MC == 12110 || MC == 12111
        Optional<NbtCompound> legacyTagOpt = itemTag.getCompound("tag");
//#elseif MC == 12101 || MC == 12104
//$$        NbtCompound legacyTag = itemTag.getCompound("tag");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        if (legacyTagOpt.isPresent()) {
//#elseif MC == 12101 || MC == 12104
//$$        if (legacyTag.isEmpty() == false) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            Optional<NbtCompound> legacyBlockEntityOpt = legacyTagOpt.get().getCompound("BlockEntityTag");
//#elseif MC == 12101 || MC == 12104
//$$            NbtCompound legacyBlockEntity = legacyTag.getCompound("BlockEntityTag");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            if (legacyBlockEntityOpt.isPresent()) {
//#elseif MC == 12101 || MC == 12104
//$$            if (legacyBlockEntity.isEmpty() == false) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                count += parseContainerItems(legacyBlockEntityOpt.get(), sink);
//#elseif MC == 12101 || MC == 12104
//$$                count += parseContainerItems(legacyBlockEntity, sink);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (itemTag.contains("tag", NbtElement.COMPOUND_TYPE)) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            NbtCompound legacyTag = itemTag.getCompound("tag");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (legacyTag.contains("BlockEntityTag", NbtElement.COMPOUND_TYPE)) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                count += parseContainerItems(legacyTag.getCompound("BlockEntityTag"), sink);
//#endif
            }
//#if MC == 12108 || MC == 12110 || MC == 12111
            count += parseContainerItems(legacyTagOpt.get(), sink);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            count += parseContainerItems(legacyTag, sink);
//#endif
        }
        // 旧格式：物品携带的 BlockEntityTag.Items（潜影盒物品）
//#if MC == 12108 || MC == 12110 || MC == 12111
        Optional<NbtCompound> blockEntityOpt = itemTag.getCompound("BlockEntityTag");
//#elseif MC == 12101 || MC == 12104
//$$        NbtCompound blockEntity = itemTag.getCompound("BlockEntityTag");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        if (blockEntityOpt.isPresent()) {
//#elseif MC == 12101 || MC == 12104
//$$        if (blockEntity.isEmpty() == false) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            count += parseContainerItems(blockEntityOpt.get(), sink);
//#elseif MC == 12101 || MC == 12104
//$$            count += parseContainerItems(blockEntity, sink);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (itemTag.contains("BlockEntityTag", NbtElement.COMPOUND_TYPE)) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            count += parseContainerItems(itemTag.getCompound("BlockEntityTag"), sink);
//#endif
        }
        // 旧格式（1.20.4-）：物品顶层直接带 Items
        count += parseContainerItems(itemTag, sink);
        return count;
    }

    /** 读取 NbtList 中第 index 个数值元素（须为 AbstractNbtNumber），失败返回 0。 */
    private static double listNumber(NbtElement element) {
        if (element instanceof net.minecraft.nbt.AbstractNbtNumber number) {
            return number.doubleValue();
        }
        return 0;
    }

    /**
     * 把实体 id 解析为对应物品。
     * 先直接按实体 id 查物品注册表（覆盖 minecart、chest_minecart、hopper_minecart、
     * armor_stand、tnt_minecart、furnace_minecart、spawner_minecart 及新版按木种拆分的
     * 船实体如 oak_boat / oak_chest_boat / bamboo_raft）；
     * 旧格式通用船实体（minecraft:boat、minecraft:chest_boat）按 "Type" 字段映射出
     * 具体木种的船/箱船物品；无法识别时返回 null。
     */
    private static Item resolveEntityItem(String entityId, NbtCompound entityTag) {
        Identifier directId = Identifier.tryParse(entityId);
        if (directId != null) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            Item direct = Registries.ITEM.get(directId);
//#elseif MC == 11701
//$$            Item direct = Registry.ITEM.get(directId);
//#endif
            if (direct != null && direct != Items.AIR) {
                return direct;
            }
        }
        // 旧格式通用船实体：按 "Type" 字段映射为具体木种的船物品
        if ("minecraft:boat".equals(entityId) || "minecraft:chest_boat".equals(entityId)) {
//#if MC == 12108 || MC == 12110 || MC == 12111
            Optional<String> typeOpt = entityTag.getString("Type");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            if (typeOpt.isPresent() && typeOpt.get().isEmpty() == false) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                String type = typeOpt.get();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            String type = entityTag.getString("Type");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            if (type != null && type.isEmpty() == false) {
//#endif
                boolean chest = entityId.endsWith("chest_boat");
                String itemName;
                if ("bamboo".equals(type)) {
                    itemName = chest ? "bamboo_chest_raft" : "bamboo_raft";
                } else {
                    itemName = chest ? type + "_chest_boat" : type + "_boat";
                }
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                Item typed = Registries.ITEM.get(Identifier.tryParse("minecraft:" + itemName));
//#endif
//#if MC == 11701
//$$                Identifier typedId = Identifier.tryParse("minecraft:" + itemName);
//#endif
//#if MC == 11701
//$$                if (typedId == null) {
//#endif
//#if MC == 11701
//$$                    return null;
//#endif
//#if MC == 11701
//$$                }
//#endif
//#if MC == 11701
//$$                Item typed = Registry.ITEM.get(typedId);
//#endif
                if (typed != null && typed != Items.AIR) {
                    return typed;
                }
            }
        }
        return null;
    }

    /**
     * 傀儡/凋零等实体的召唤材料：每个实体按其建造配方（数量 × 配方材料）计入统计。
     * - 雪傀儡：2 雪块 + 1 雕刻南瓜
     * - 铁傀儡：4 铁块 + 1 雕刻南瓜
     * - 凋零：4 灵魂沙 + 3 凋零骷髅头颅
     * - 铜傀儡：与实体氧化级别（0-3）及是否涂蜡一致的铜块 + 1 雕刻南瓜
     * 本方法在「是否计算原理图实体」开启的实体解析循环内调用，随实体数量叠加。
     */
    private static void countSummonMaterials(String entityId, NbtCompound entityTag,
                                             Map<Item, Integer> itemCounts, Map<Item, String> itemNames) {
        switch (entityId) {
            case "minecraft:snow_golem" -> {
                if (com.litematlist.config.Configs.Statistics.enabled("snowGolem")) {
                    addSpecialItem(blockItem("snow_block"), itemCounts, itemNames, 2);
                    addSpecialItem(blockItem("carved_pumpkin"), itemCounts, itemNames);
                }
            }
            case "minecraft:iron_golem" -> {
                if (com.litematlist.config.Configs.Statistics.enabled("ironGolem")) {
                    addSpecialItem(blockItem("iron_block"), itemCounts, itemNames, 4);
                    addSpecialItem(blockItem("carved_pumpkin"), itemCounts, itemNames);
                }
            }
            case "minecraft:wither" -> {
                if (com.litematlist.config.Configs.Statistics.enabled("wither")) {
                    addSpecialItem(blockItem("soul_sand"), itemCounts, itemNames, 4);
                    addSpecialItem(blockItem("wither_skeleton_skull"), itemCounts, itemNames, 3);
                }
            }
            default -> {
            }
        }
        // 铜傀儡：按 mod 命名空间任意匹配（如 friendsandfoes:copper_golem）
        if (entityId.endsWith(":copper_golem")
                && com.litematlist.config.Configs.Statistics.enabled("copperGolem")) {
            // 氧化级别 0-3 与是否涂蜡决定铜块变体
            int oxidation;
            boolean waxed;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 1.21.9+ 原版铜傀儡：weather_state 字符串枚举 + next_weather_age==-2 表示涂蜡
//#elseif MC == 11701
//$$            // 原版铜傀儡：weather_state 字符串枚举 + next_weather_age==-2 表示涂蜡
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            Optional<String> weatherStateOpt = entityTag.getString("weather_state");
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            String weatherState = entityTag.getString("weather_state");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            if (weatherStateOpt.isPresent()) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            if (weatherState != null && weatherState.isEmpty() == false) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                oxidation = switch (weatherStateOpt.get()) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                oxidation = switch (weatherState) {
//#endif
                    case "exposed" -> 1;
                    case "weathered" -> 2;
                    case "oxidized" -> 3;
                    default -> 0;
                };
//#if MC == 12108 || MC == 12110 || MC == 12111
                waxed = entityTag.getLong("next_weather_age").orElse(-1L) == -2L;
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                waxed = entityTag.getLong("next_weather_age") == -2L;
//#endif
            } else {
                // 旧格式（friendsandfoes 等模组）：OxidationLevel 整数 + Waxed 布尔（键名双写兼容大小写差异）
//#if MC == 12108 || MC == 12110 || MC == 12111
                oxidation = entityTag.getInt("OxidationLevel").orElse(entityTag.getInt("oxidationLevel").orElse(0));
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                waxed = entityTag.getBoolean("Waxed").orElse(entityTag.getBoolean("waxed").orElse(false));
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                oxidation = entityTag.contains("OxidationLevel") ? entityTag.getInt("OxidationLevel")
//#endif
//#if MC == 12101 || MC == 12104
//$$                        : entityTag.getInt("oxidationLevel");
//#endif
//#if MC == 12101 || MC == 12104
//$$                waxed = entityTag.contains("Waxed") ? entityTag.getBoolean("Waxed") : entityTag.getBoolean("waxed");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        : (entityTag.contains("oxidationLevel") ? entityTag.getInt("oxidationLevel") : 0);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                waxed = entityTag.contains("Waxed") ? entityTag.getBoolean("Waxed")
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        : (entityTag.contains("waxed") && entityTag.getBoolean("waxed"));
//#endif
            }
            oxidation = Math.min(3, Math.max(0, oxidation));
            String copperBlock = switch (oxidation) {
                case 1 -> waxed ? "waxed_exposed_copper" : "exposed_copper";
                case 2 -> waxed ? "waxed_weathered_copper" : "weathered_copper";
                case 3 -> waxed ? "waxed_oxidized_copper" : "oxidized_copper";
                default -> waxed ? "waxed_copper_block" : "copper_block";
            };
            addSpecialItem(blockItem(copperBlock), itemCounts, itemNames);
            addSpecialItem(blockItem("carved_pumpkin"), itemCounts, itemNames);
        }
    }

    /** 按 minecraft 命名空间从物品注册表取物品（不存在时为 AIR，由调用方忽略）。 */
    private static Item blockItem(String name) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return Registries.ITEM.get(Identifier.tryParse("minecraft:" + name));
//#endif
//#if MC == 11701
//$$        Identifier id = Identifier.tryParse("minecraft:" + name);
//#endif
//#if MC == 11701
//$$        if (id == null) {
//#endif
//#if MC == 11701
//$$            return Items.AIR;
//#endif
//#if MC == 11701
//$$        }
//#endif
//#if MC == 11701
//$$        return Registry.ITEM.get(id);
//#endif
    }

    /**
     * 统计容器实体中的容器数据（开关「是否计算原理图实体容器数据」开启时调用）。
     * 覆盖：漏斗矿车/箱子矿车（Items）、箱船/箱筏（Items）、有箱子的驴/羊驼（Items）。
     * （盔甲架穿戴/手持的物品不在本方法处理——它们跟随「是否计算原理图实体」开关，
     * 由 parseEntityList 直接统计。）
     * 已知容器实体类型受「统计」页对应类型开关门控。返回计入的物品数。
     */
    private static int parseEntityContainer(String entityId, NbtCompound entityTag,
                                            MaterialSink sink) {
        int count = 0;

        String typeKey = classifyEntityContainerType(entityId);
        if (typeKey != null) {
            // 「统计」页实体容器类型开关：未开启则跳过
            if (com.litematlist.config.Configs.Statistics.enabled(typeKey) == false) {
                return 0;
            }
            // 驴/羊驼：只统计真正带箱子的（无标记时以是否存在 Items 数据为准）
            if (("donkeyWithChest".equals(typeKey) || "llamaWithChest".equals(typeKey))
                    && !isChestedHorse(entityTag)) {
                return 0;
            }
            return parseContainerItems(entityTag, sink);
        }
        return count;
    }

    /**
     * 容器实体类型分类：返回「统计」页类型 key；不属于已知容器实体时返回 null。
     * 箱船覆盖各木种拆分实体（oak_chest_boat 等）与竹筏（bamboo_chest_raft）。
     */
    private static String classifyEntityContainerType(String entityId) {
        if ("minecraft:chest_minecart".equals(entityId)) {
            return "chestMinecart";
        }
        if ("minecraft:hopper_minecart".equals(entityId)) {
            return "hopperMinecart";
        }
        if (entityId.endsWith("chest_boat") || entityId.endsWith("chest_raft")) {
            return "chestBoat";
        }
        if ("minecraft:donkey".equals(entityId)) {
            return "donkeyWithChest";
        }
        if ("minecraft:llama".equals(entityId) || "minecraft:trader_llama".equals(entityId)) {
            return "llamaWithChest";
        }
        return null;
    }

    /** 驴/羊驼是否带箱子：显式标记优先（兼容 C/C 大小写键名），无标记时以是否存在 Items 数据为准。 */
    private static boolean isChestedHorse(NbtCompound entityTag) {
        if (entityTag.contains("ChestedHorse")) {
//#if MC == 12108 || MC == 12110 || MC == 12111
            return entityTag.getBoolean("ChestedHorse").orElse(false);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            return entityTag.getBoolean("ChestedHorse");
//#endif
        }
        if (entityTag.contains("chestedHorse")) {
//#if MC == 12108 || MC == 12110 || MC == 12111
            return entityTag.getBoolean("chestedHorse").orElse(false);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            return entityTag.getBoolean("chestedHorse");
//#endif
        }
        return entityTag.contains("Items");
    }

//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 统计盔甲架穿戴/手持的物品并展开盔甲上的纹饰（纹饰锻造模板 + 镶嵌材质），
     * 返回计入的物品数。
     * 1.21.10 存档使用新版 equipment 复合标签（feet/legs/chest/head/mainhand/offhand
     * 槽位，值为 {id, count, components} 物品），旧格式为 ArmorItems / HandItems 列表，
     * 两种格式均兼容。
     */
//#elseif MC == 11701
//$$    /**
//$$     * 统计盔甲架穿戴/手持的物品并展开盔甲上的纹饰（纹饰锻造模板 + 镶嵌材质），
//$$     * 返回计入的物品数。
//$$     * 新版存档使用 equipment 复合标签（feet/legs/chest/head/mainhand/offhand
//$$     * 槽位，值为 {id, count, components} 物品），旧格式为 ArmorItems / HandItems 列表，
//$$     * 两种格式均兼容。
//$$     */
//#endif
    private static int parseArmorStandItems(NbtCompound entityTag, Map<Item, Integer> itemCounts,
                                             Map<Item, String> itemNames, Map<String, ExactItem> exactItems) {
        int count = 0;
//#if MC == 12108 || MC == 12110 || MC == 12111
        Optional<NbtCompound> equipOpt = entityTag.getCompound("equipment");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        if (equipOpt.isPresent()) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            NbtCompound equipment = equipOpt.get();
//#endif
//#if MC == 12101 || MC == 12104
//$$        NbtCompound equipment = entityTag.getCompound("equipment");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$        if (entityTag.contains("equipment", NbtElement.COMPOUND_TYPE)) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            NbtCompound equipment = entityTag.getCompound("equipment");
//#endif
            for (String slotKey : equipment.getKeys()) {
                NbtElement slotElement = equipment.get(slotKey);
                if (slotElement instanceof NbtCompound slotTag
                        && parseArmorItem(slotTag, itemCounts, itemNames, exactItems)) {
                    count++;
                }
            }
        } else {
//#if MC == 12108 || MC == 12110 || MC == 12111
            Optional<NbtList> armorOpt = entityTag.getList("ArmorItems");
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            if (entityTag.contains("ArmorItems", NbtElement.LIST_TYPE)) {
//#elseif MC == 12101 || MC == 12104
//$$            NbtList armorItems = entityTag.getList("ArmorItems", 10);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            if (armorOpt.isPresent()) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                count += parseArmorItemList(entityTag.getList("ArmorItems", NbtElement.COMPOUND_TYPE),
//#elseif MC == 12101 || MC == 12104
//$$            if (armorItems.isEmpty() == false) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                count += parseArmorItemList(armorOpt.get(), itemCounts, itemNames, exactItems);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                        itemCounts, itemNames, exactItems);
//#elseif MC == 12101 || MC == 12104
//$$                count += parseArmorItemList(armorItems, itemCounts, itemNames, exactItems);
//#endif
            }
//#if MC == 12108 || MC == 12110 || MC == 12111
            Optional<NbtList> handOpt = entityTag.getList("HandItems");
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            if (entityTag.contains("HandItems", NbtElement.LIST_TYPE)) {
//#elseif MC == 12101 || MC == 12104
//$$            NbtList handItems = entityTag.getList("HandItems", 10);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            if (handOpt.isPresent()) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                count += parseArmorItemList(entityTag.getList("HandItems", NbtElement.COMPOUND_TYPE),
//#elseif MC == 12101 || MC == 12104
//$$            if (handItems.isEmpty() == false) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                count += parseArmorItemList(handOpt.get(), itemCounts, itemNames, exactItems);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                        itemCounts, itemNames, exactItems);
//#elseif MC == 12101 || MC == 12104
//$$                count += parseArmorItemList(handItems, itemCounts, itemNames, exactItems);
//#endif
            }
        }
        return count;
    }

    /**
     * 解析盔甲架单个物品栏列表（旧格式 ArmorItems/HandItems）：逐项统计物品本身，
     * 并额外展开其纹饰（模板 + 材质）。
     */
    private static int parseArmorItemList(NbtList items, Map<Item, Integer> itemCounts,
                                          Map<Item, String> itemNames, Map<String, ExactItem> exactItems) {
        int count = 0;
        for (int i = 0; i < items.size(); i++) {
            NbtElement element = items.get(i);
            if (element instanceof NbtCompound itemTag
                    && parseArmorItem(itemTag, itemCounts, itemNames, exactItems)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 统计单个装备槽位物品（{id, count/Count} 格式），并展开其纹饰（模板 + 材质）。
     * 返回是否成功计入。
     */
    private static boolean parseArmorItem(NbtCompound itemTag, Map<Item, Integer> itemCounts,
                                          Map<Item, String> itemNames, Map<String, ExactItem> exactItems) {
//#if MC == 12108 || MC == 12110 || MC == 12111
        Optional<String> idOpt = itemTag.getString("id");
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        String id = itemTag.getString("id");
//#elseif MC == 12101 || MC == 12104
//$$        String idOpt = itemTag.getString("id");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        if (idOpt.isEmpty()) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        if (id == null || id.isEmpty()) {
//#elseif MC == 12101 || MC == 12104
//$$        if (idOpt == null || idOpt.isEmpty()) {
//#endif
            return false;
        }
//#if MC == 12108 || MC == 12110 || MC == 12111
        Identifier itemId = Identifier.tryParse(idOpt.get());
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        Identifier itemId = Identifier.tryParse(id);
//#elseif MC == 12101 || MC == 12104
//$$        Identifier itemId = Identifier.tryParse(idOpt);
//#endif
        if (itemId == null) {
            return false;
        }
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Item item = Registries.ITEM.get(itemId);
//#elseif MC == 11701
//$$        Item item = Registry.ITEM.get(itemId);
//#endif
        if (item == null || item == Items.AIR) {
            return false;
        }
        // 带非默认组件（附魔/耐久/纹饰/重命名等）→ 精确条目（特殊物品）；
        // 纹饰不再单独展开为材料，由【原材料】树按锻造台配方分解为 盔甲/模板/材质 三个子节点
        if (nonDefaultComponents(itemTag) != null) {
            addItemEntry(itemTag, itemCounts, itemNames, exactItems);
            return true;
        }
        int itemCount = 1;
        // 兼容两种键名与数值类型
        NbtElement countElement = itemTag.get("count");
        if (countElement == null) {
            countElement = itemTag.get("Count");
        }
        if (countElement instanceof net.minecraft.nbt.AbstractNbtNumber numberTag) {
            itemCount = numberTag.intValue();
        }
        if (itemCount <= 0) {
            itemCount = 1;
        }
        itemCounts.merge(item, itemCount, Integer::sum);
        itemNames.putIfAbsent(item, item.getName().getString());
        // 纹饰展开：纹饰锻造模板 + 镶嵌材质
        addArmorTrimMaterials(itemTag, itemCounts, itemNames);
        return true;
    }

    /**
     * 解析盔甲物品上的纹饰并计入统计：
     * - 新格式（1.20.5+）：components["minecraft:trim"]{pattern, material}
     * - 旧格式（1.20.4 及以前）：tag.Trim{pattern, material}
     * 纹饰模板（如哨兵盔甲纹饰锻造模板）+ 镶嵌材质（如铁锭），供「原材料」
     * 列表的锻造台方式分解使用。
     */
    private static void addArmorTrimMaterials(NbtCompound itemTag, Map<Item, Integer> itemCounts,
                                              Map<Item, String> itemNames) {
        String pattern = null;
        String material = null;
//#if MC == 12108 || MC == 12110 || MC == 12111
        Optional<NbtCompound> componentsOpt = itemTag.getCompound("components");
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        if (itemTag.contains("components", NbtElement.COMPOUND_TYPE)) {
//#elseif MC == 12101 || MC == 12104
//$$        NbtCompound components = itemTag.getCompound("components");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        if (componentsOpt.isPresent()) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            NbtCompound components = itemTag.getCompound("components");
//#elseif MC == 12101 || MC == 12104
//$$        if (components.isEmpty() == false) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            Optional<NbtCompound> trimOpt = componentsOpt.get().getCompound("minecraft:trim");
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            if (components.contains("minecraft:trim", NbtElement.COMPOUND_TYPE)) {
//#elseif MC == 12101 || MC == 12104
//$$            NbtCompound trim = components.getCompound("minecraft:trim");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            if (trimOpt.isPresent()) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                NbtCompound trim = components.getCompound("minecraft:trim");
//#elseif MC == 12101 || MC == 12104
//$$            if (trim.isEmpty() == false) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                pattern = trimOpt.get().getString("pattern").orElse(null);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                pattern = trim.getString("pattern");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                material = trimOpt.get().getString("material").orElse(null);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                material = trim.getString("material");
//#endif
            }
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (pattern == null || material == null) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        if ((pattern == null || material == null) && itemTag.contains("tag", NbtElement.COMPOUND_TYPE)) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            Optional<NbtCompound> tagOpt = itemTag.getCompound("tag");
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            NbtCompound tag = itemTag.getCompound("tag");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            if (tagOpt.isPresent()) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            if (tag.contains("Trim", NbtElement.COMPOUND_TYPE)) {
//#elseif MC == 12101 || MC == 12104
//$$            if (tag.isEmpty() == false) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                Optional<NbtCompound> trimOpt = tagOpt.get().getCompound("Trim");
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                NbtCompound trim = tag.getCompound("Trim");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                if (trimOpt.isPresent()) {
//#elseif MC == 12101 || MC == 12104
//$$                if (trim.isEmpty() == false) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    pattern = trimOpt.get().getString("pattern").orElse(null);
//#elseif MC == 12101 || MC == 12104
//$$                    pattern = trim.getString("pattern");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    material = trimOpt.get().getString("material").orElse(null);
//#elseif MC == 12101 || MC == 12104
//$$                    material = trim.getString("material");
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                pattern = trim.getString("pattern");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                material = trim.getString("material");
//#endif
            }
        }
        if (pattern == null || material == null) {
            return;
        }
        Item template = trimTemplateItem(pattern);
        if (template != null) {
            addSpecialItem(template, itemCounts, itemNames);
        }
        Item matItem = trimMaterialItem(material);
        if (matItem != null) {
            addSpecialItem(matItem, itemCounts, itemNames);
        }
    }

    /** 纹饰图案 key（如 minecraft:sentry）→ 对应纹饰锻造模板物品。供原材料树分解复用。 */
    static Item trimTemplateItem(String patternKey) {
        Identifier patternId = Identifier.tryParse(patternKey);
        if (patternId == null) {
            return null;
        }
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Item item = Registries.ITEM.get(Identifier.tryParse(
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                patternId.getNamespace() + ":" + patternId.getPath() + "_armor_trim_smithing_template"));
//#endif
//#if MC == 11701
//$$        Identifier templateId = Identifier.tryParse(
//#endif
//#if MC == 11701
//$$                patternId.getNamespace() + ":" + patternId.getPath() + "_armor_trim_smithing_template");
//#endif
//#if MC == 11701
//$$        if (templateId == null) {
//#endif
//#if MC == 11701
//$$            return null;
//#endif
//#if MC == 11701
//$$        }
//#endif
//#if MC == 11701
//$$        Item item = Registry.ITEM.get(templateId);
//#endif
        return (item == null || item == Items.AIR) ? null : item;
    }

    /** 纹饰材质 key（如 minecraft:iron）→ 对应镶嵌材质物品（铁锭等）。供原材料树分解复用。 */
    static Item trimMaterialItem(String materialKey) {
        String mapped = switch (materialKey) {
            case "minecraft:iron" -> "minecraft:iron_ingot";
            case "minecraft:gold" -> "minecraft:gold_ingot";
            case "minecraft:copper" -> "minecraft:copper_ingot";
            case "minecraft:netherite" -> "minecraft:netherite_ingot";
            case "minecraft:lapis" -> "minecraft:lapis_lazuli";
            case "minecraft:amethyst" -> "minecraft:amethyst_shard";
            case "minecraft:resin" -> "minecraft:resin_brick";
            default -> materialKey; // 石英/钻石/绿宝石/红石同名物品，模组材质按原键兜底
        };
        Identifier id = Identifier.tryParse(mapped);
        if (id == null) {
            return null;
        }
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Item item = Registries.ITEM.get(id);
//#elseif MC == 11701
//$$        Item item = Registry.ITEM.get(id);
//#endif
        return (item == null || item == Items.AIR) ? null : item;
    }

    /**
     * 解析调色板条目（Litematica 原生格式）。
     * 返回 BlockState，如果解析失败则返回 AIR。
     */
    private static BlockState parsePaletteEntry(NbtCompound stateTag, int index) {
//#if MC == 12108 || MC == 12110 || MC == 12111
        Optional<String> nameOpt = stateTag.getString("Name");
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        String blockName = stateTag.getString("Name");
//#elseif MC == 12101 || MC == 12104
//$$        String nameOpt = stateTag.getString("Name");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        if (nameOpt.isEmpty()) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        if (blockName == null || blockName.isEmpty()) {
//#elseif MC == 12101 || MC == 12104
//$$        if (nameOpt == null || nameOpt.isEmpty()) {
//#endif
            LitematListMod.LOGGER.warn("调色板条目 #{} 缺少 Name 字段", index);
            return Blocks.AIR.getDefaultState();
        }

//#if MC == 12108 || MC == 12110 || MC == 12111
        String blockName = nameOpt.get();
//#elseif MC == 12101 || MC == 12104
//$$        String blockName = nameOpt;
//#endif
        // 部分工具会把完整状态串写进 Name（如 "minecraft:cake[bites=3]"），
        // 需剥离方括号内的属性，否则 Identifier.tryParse 失败导致方块被丢弃（如被吃掉一部分的蛋糕）
        String inlineProps = null;
        int bracketIdx = blockName.indexOf('[');
        if (bracketIdx > 0 && blockName.endsWith("]")) {
            inlineProps = blockName.substring(bracketIdx + 1, blockName.length() - 1);
            blockName = blockName.substring(0, bracketIdx);
        }
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$
//#endif
//#if MC == 12111
//$$
//#endif
        Identifier blockId = Identifier.tryParse(blockName);
        if (blockId == null) {
            LitematListMod.LOGGER.warn("调色板条目 #{} 无法解析 Block ID: {}", index, blockName);
            return Blocks.AIR.getDefaultState();
        }

        // 从方块注册表获取方块
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Block block = Registries.BLOCK.get(blockId);
//#elseif MC == 11701
//$$        Block block = Registry.BLOCK.get(blockId);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
        // 如果注册表返回 AIR 但方块名不是 air，尝试从物品注册表查找
        if (block == Blocks.AIR && !"minecraft:air".equals(blockName)) {
            // 尝试从 ITEM 注册表查找对应物品，再反查方块
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            Item item = Registries.ITEM.get(blockId);
//#elseif MC == 11701
//$$            Item item = Registry.ITEM.get(blockId);
//#endif
            if (item != null && item != Items.AIR) {
                Block itemBlock = Block.getBlockFromItem(item);
                if (itemBlock != Blocks.AIR) {
                    LitematListMod.LOGGER.info("调色板条目 #{} 通过物品注册表找到方块: {} -> {}", index, blockName, itemBlock);
                    block = itemBlock;
                }
            }
            if (block == Blocks.AIR) {
                LitematListMod.LOGGER.warn("调色板条目 #{} 方块未在注册表中找到: {} (可能是新版本方块或模组方块)", index, blockName);
            }
        }

        BlockState state = block.getDefaultState();

        // 解析 Properties
//#if MC == 12108 || MC == 12110 || MC == 12111
        Optional<NbtCompound> propsOpt = stateTag.getCompound("Properties");
//#elseif MC == 12101 || MC == 12104
//$$        NbtCompound propsOpt = stateTag.getCompound("Properties");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        if (propsOpt.isPresent()) {
//#elseif MC == 12101 || MC == 12104
//$$        if (propsOpt.isEmpty() == false) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            NbtCompound props = propsOpt.get();
//#elseif MC == 12101 || MC == 12104
//$$            NbtCompound props = propsOpt;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        if (stateTag.contains("Properties", NbtElement.COMPOUND_TYPE)) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            NbtCompound props = stateTag.getCompound("Properties");
//#endif
            for (String key : props.getKeys()) {
//#if MC == 12108 || MC == 12110 || MC == 12111
                Optional<String> valOpt = props.getString(key);
//#elseif MC == 12101 || MC == 12104
//$$                String valOpt = props.getString(key);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                if (valOpt.isEmpty()) continue;
//#elseif MC == 12101 || MC == 12104
//$$                if (valOpt == null || valOpt.isEmpty()) continue;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                String value = valOpt.get();
//#elseif MC == 12101 || MC == 12104
//$$                String value = valOpt;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                String value = props.getString(key);
//#endif
                boolean propertyFound = false;
                for (net.minecraft.state.property.Property<?> property : state.getProperties()) {
                    if (property.getName().equals(key)) {
                        state = applyProperty(state, property, value);
                        propertyFound = true;
                        break;
                    }
                }
                if (!propertyFound) {
                    LitematListMod.LOGGER.debug("调色板条目 #{} 属性 '{}' 未在当前方块状态中找到，可能已变更", index, key);
                }
            }
        }

        // 应用 Name 内联属性（如 "minecraft:cake[bites=3]" 中的 bites）
        if (inlineProps != null && !inlineProps.isEmpty()) {
            for (String prop : inlineProps.split(",")) {
                String[] kv = prop.split("=", 2);
                if (kv.length != 2) continue;
                String key = kv[0].trim();
                for (net.minecraft.state.property.Property<?> property : state.getProperties()) {
                    if (property.getName().equals(key)) {
                        state = applyProperty(state, property, kv[1].trim());
                        break;
                    }
                }
            }
        }

        return state;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState applyProperty(
            BlockState state, net.minecraft.state.property.Property<T> property, String value) {
        try {
            Optional<T> parsed = property.parse(value);
            if (parsed.isPresent()) {
                return state.with(property, parsed.get());
            }
            LitematListMod.LOGGER.debug("属性值解析失败: {}={} (无效值)", property.getName(), value);
            return state;
        } catch (Exception e) {
            LitematListMod.LOGGER.warn("应用方块属性失败: {}={}, 使用默认值", property.getName(), value);
            return state;
        }
    }
}
//#if MC == 11701
//$$
//#endif