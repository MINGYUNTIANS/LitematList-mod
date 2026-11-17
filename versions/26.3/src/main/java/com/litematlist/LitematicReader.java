package com.litematlist;

import fi.dy.masa.malilib.util.nbt.NbtUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.*;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.nio.file.Path;
import java.util.*;

/**
 * 读取和解析 .litematic 文件，提取方块状态并统计每种方块对应的物品及数量。
 * 参照 Litematica 0.24.9 的 LitematicaSchematic 源码实现。
 * 支持 Litematica 原生格式（long[] BlockStates）和 Sponge 格式（byte[] BlockData）。
 */
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
                return exactStack.copyWithCount(totalCount);
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
    private static CompoundTag nonDefaultComponents(CompoundTag itemTag) {
        CompoundTag components = itemTag.getCompound("components")
                .filter(c -> c.keySet().isEmpty() == false).orElse(null);
        if (components != null) {
            return components;
        }
        return itemTag.getCompound("tag")
                .filter(c -> c.keySet().isEmpty() == false).orElse(null);
    }

    /** 组件键去掉 minecraft: 前缀显示（与示例 custom_name / stored_enchantments 一致），其余命名空间保留。 */
    private static String displayComponentKey(String key) {
        return key.startsWith("minecraft:") ? key.substring("minecraft:".length()) : key;
    }

    /**
     * 拼装精确物品ID：新格式 id[组件=SNBT,...]（如 minecraft:iron_nugget[custom_name='{"text":"111"}']），
     * 旧格式 id{tag SNBT}。组件值直接使用 NBT 的 SNBT 序列化（字符串自动带单引号）。
     */
    private static String buildExactId(Identifier itemId, CompoundTag components, boolean legacy) {
        StringBuilder sb = new StringBuilder(itemId.toString());
        if (components == null || components.keySet().isEmpty()) {
            return sb.toString();
        }
        if (legacy) {
            sb.append(components.toString());
            return sb.toString();
        }
        sb.append('[');
        boolean first = true;
        for (String key : components.keySet()) {
            Tag value = components.get(key);
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
    private static ItemStack decodeExactStack(CompoundTag itemTag, Item fallback, int count) {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.level != null) {
            try {
                net.minecraft.resources.RegistryOps<Tag> ops =
                        net.minecraft.resources.RegistryOps.create(NbtOps.INSTANCE, client.level.registryAccess());
                Optional<ItemStack> parsed = ItemStack.CODEC.parse(ops, itemTag).result();
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
    private static boolean addItemEntry(CompoundTag itemTag, Map<Item, Integer> itemCounts,
                                        Map<Item, String> itemNames, Map<String, ExactItem> exactItems) {
        Optional<String> idOpt = itemTag.getString("id");
        if (idOpt.isEmpty()) {
            return false;
        }
        Identifier itemId = Identifier.tryParse(idOpt.get());
        if (itemId == null) {
            return false;
        }
        Item item = BuiltInRegistries.ITEM.get(itemId).map(Holder::value).orElse(null);
        if (item == null || item == Items.AIR) {
            return false;
        }
        int itemCount = 1;
        Tag countElement = itemTag.get("count");
        if (countElement == null) {
            countElement = itemTag.get("Count");
        }
        if (countElement instanceof net.minecraft.nbt.NumericTag numberTag) {
            itemCount = numberTag.intValue();
        }
        if (itemCount <= 0) {
            itemCount = 1;
        }
        CompoundTag components = exactItems != null ? nonDefaultComponents(itemTag) : null;
        if (components != null) {
            Optional<CompoundTag> modernOpt = itemTag.getCompound("components");
            boolean legacy = modernOpt.isEmpty() || modernOpt.get().keySet().isEmpty();
            String exactId = buildExactId(itemId, components, legacy);
            addExactItem(exactItems, exactId, item, decodeExactStack(itemTag, item, itemCount), itemCount);
            return true;
        }
        itemCounts.merge(item, itemCount, Integer::sum);
        itemNames.putIfAbsent(item, new ItemStack(item).getHoverName().getString());
        return true;
    }

    /**
     * 从文件路径加载材料列表。
     * 解析结果按「文件修改时间」缓存，供注入/HUD/界面共享，
     * 避免同一次刷新流程中对同一份大原理图反复解析（防大量数据流/卡顿）。
     */
    public static List<MaterialEntry> loadMaterialList(Path filePath) {
        return loadMaterialList(filePath, null);
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
            CompoundTag rootTag = NbtUtils.readNbtFromFileAsPath(filePath);
            if (rootTag == null) {
                LitematListMod.LOGGER.error("读取 NBT 失败: {}", filePath);
                return Collections.emptyList();
            }
            LitematListMod.LOGGER.info("成功读取 NBT，顶层键: {}", rootTag.keySet());
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

    /**
     * 清空解析缓存。当「是否计算容器数据」等全局解析设置变化时调用，
     * 强制下次加载按新设置重新解析。
     */
    public static void clearParseCache() {
        PARSE_CACHE.clear();
    }

    /** 解析结果缓存条目（按文件修改时间 + 层级过滤签名失效） */
    private record CachedResult(long lastModified, String filterSig, List<MaterialEntry> materials) {}

    /** .litematic 解析结果缓存 */
    private static final Map<Path, CachedResult> PARSE_CACHE = new HashMap<>();

    private static List<MaterialEntry> parseBlockStates(CompoundTag rootTag, Path filePath, LitematicaBridge.LayerFilter filter) {
        // 按四个分类分别累积，最后合并（同一物品取最早分类）并按分类分组、组内数量降序输出
        MaterialSink buildingSink = new MaterialSink();
        MaterialSink containerSink = new MaterialSink();
        MaterialSink entitySink = new MaterialSink();
        MaterialSink entityContainerSink = new MaterialSink();
        int regionCount = 0;
        int failedRegions = 0;

        // 检查 Version
        if (!rootTag.contains("Version")) {
            LitematListMod.LOGGER.warn("缺少 Version 字段，不是有效的 Litematica 原理图");
            return Collections.emptyList();
        }
        int version = rootTag.getInt("Version").orElse(-1);
        LitematListMod.LOGGER.info("原理图版本: {}", version);

        // 读取 Regions（Litematica 原生格式）
        Optional<CompoundTag> regionsOpt = rootTag.getCompound("Regions");
        if (regionsOpt.isPresent()) {
            CompoundTag regions = regionsOpt.get();
            LitematListMod.LOGGER.info("找到 Regions，共 {} 个子区域", regions.keySet().size());

            for (String regionName : regions.keySet()) {
                Optional<CompoundTag> regionOpt = regions.getCompound(regionName);
                if (regionOpt.isPresent()) {
                    try {
                        parseRegion(regionOpt.get(), buildingSink, containerSink, entitySink,
                                entityContainerSink, regionName, version, filter);
                        regionCount++;
                    } catch (Exception e) {
                        failedRegions++;
                        LitematListMod.LOGGER.error("解析 Region '{}' 失败，跳过该区域: {}", regionName, e.getMessage());
                        LitematListMod.LOGGER.debug("Region '{}' 异常详情:", regionName, e);
                    }
                }
            }
        }

        // 尝试 Sponge 格式（Blocks 键）
        if (regionCount == 0 && rootTag.contains("Blocks")) {
            LitematListMod.LOGGER.info("尝试解析 Sponge 格式...");
            try {
                parseSpongeFormat(rootTag, buildingSink, containerSink, filter);
                regionCount++;
            } catch (Exception e) {
                LitematListMod.LOGGER.error("Sponge 格式解析失败: {}", e.getMessage());
            }
        }

        LitematListMod.LOGGER.info("解析了 {} 个 Region，失败 {} 个，共 {} 种方块",
                regionCount, failedRegions, buildingSink.itemCounts.size());

        List<MaterialEntry> result = new ArrayList<>();
        Map<Item, Integer> normalIndex = new HashMap<>();
        Map<String, Integer> exactIndex = new HashMap<>();
        appendSink(result, normalIndex, exactIndex, buildingSink, MaterialEntry.CATEGORY_BUILDING);
        appendSink(result, normalIndex, exactIndex, containerSink, MaterialEntry.CATEGORY_CONTAINER);
        appendSink(result, normalIndex, exactIndex, entitySink, MaterialEntry.CATEGORY_ENTITY);
        appendSink(result, normalIndex, exactIndex, entityContainerSink, MaterialEntry.CATEGORY_ENTITY_CONTAINER);
        // 分类优先：建筑材料 → 容器数据 → 实体数据 → 实体容器数据（组内按数量降序）
        result.sort(Comparator.comparingInt(MaterialEntry::category)
                .thenComparing(Comparator.comparingInt(MaterialEntry::totalCount).reversed()));
        return result;
    }

    /** 材料分类累积器：每个分类各自维护普通物品/名称/精确物品映射。 */
    private static final class MaterialSink {
        final Map<Item, Integer> itemCounts = new LinkedHashMap<>();
        final Map<Item, String> itemNames = new LinkedHashMap<>();
        final Map<String, ExactItem> exactItems = new LinkedHashMap<>();
    }

    /**
     * 将某一分类的累积结果并入总列表：同一物品（或同一精确ID）已在更早分类出现时并入该条目，
     * 保留最早分类与名称；否则新建条目。index 映射用于跨分类去重。
     */
    private static void appendSink(List<MaterialEntry> out, Map<Item, Integer> normalIndex,
                                   Map<String, Integer> exactIndex, MaterialSink sink, int category) {
        for (Map.Entry<Item, Integer> entry : sink.itemCounts.entrySet()) {
            Item item = entry.getKey();
            int count = entry.getValue();
            Integer idx = normalIndex.get(item);
            if (idx != null) {
                MaterialEntry old = out.get(idx);
                out.set(idx, new MaterialEntry(old.item(), old.blockName(), old.totalCount() + count,
                        old.missingCount(), old.exactStack(), old.category()));
            } else {
                normalIndex.put(item, out.size());
                out.add(new MaterialEntry(item, sink.itemNames.getOrDefault(item, new ItemStack(item).getHoverName().getString()),
                        count, 0, null, category));
            }
        }
        for (Map.Entry<String, ExactItem> entry : sink.exactItems.entrySet()) {
            String exactId = entry.getKey();
            ExactItem exact = entry.getValue();
            Integer idx = exactIndex.get(exactId);
            if (idx != null) {
                MaterialEntry old = out.get(idx);
                out.set(idx, new MaterialEntry(old.item(), old.blockName(), old.totalCount() + exact.count(),
                        old.missingCount(), old.exactStack(), old.category()));
            } else {
                ItemStack stack = exact.stack() != null ? exact.stack() : new ItemStack(exact.item(), 1);
                exactIndex.put(exactId, out.size());
                out.add(new MaterialEntry(exact.item(), exactId, exact.count(), 0, stack, category));
            }
        }
    }

    /**
     * 流体源头等级属性：从水方块默认状态解析一次，避免硬编码属性名。
     * level 值为 0 时代表静水/静岩浆源头。
     */
    private static final net.minecraft.world.level.block.state.properties.IntegerProperty FLUID_LEVEL_PROPERTY =
            findFluidLevelProperty();

    private static net.minecraft.world.level.block.state.properties.IntegerProperty findFluidLevelProperty() {
        for (net.minecraft.world.level.block.state.properties.Property<?> property : Blocks.WATER.defaultBlockState().getProperties()) {
            if (property instanceof net.minecraft.world.level.block.state.properties.IntegerProperty intProp
                    && "level".equals(property.getName())) {
                return intProp;
            }
        }
        return null;
    }

    /**
     * 流体与含水方块的额外统计（均属「建筑材料」）：
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
            if (state.getValue(FLUID_LEVEL_PROPERTY) == 0) {
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
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)
                && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)) {
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
            itemNames.putIfAbsent(item, new ItemStack(item).getHoverName().getString());
        }
    }

    /**
     * 方块实际消耗/掉落的物品份数：海泡菜（pickles）、蜡烛含16色（candles）、
     * 粉色花瓣（flower_amount）、海龟蛋（eggs）等「一格内可堆叠多份」的方块按 state 数量属性计
     * （如 pickles=4 建造需 4 个海泡菜、掉落也是 4 个），其余方块固定为 1。
     */
    private static int getBlockStateItemCount(BlockState state) {
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.PICKLES)) {
            return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.PICKLES);
        }
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.CANDLES)) {
            return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.CANDLES);
        }
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FLOWER_AMOUNT)) {
            return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FLOWER_AMOUNT);
        }
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.EGGS)) {
            return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.EGGS);
        }
        // 蛋糕：被吃掉一部分的蛋糕（bites>0）仍按一整个计算（计 1 个蛋糕）
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.BITES)) {
            return 1;
        }
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
        // 门/高花：同一件物品占据上下两格，只统计下半格
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER ? 1 : 0;
        }
        // 床：一张床由尾/头两格构成（part=foot/head），只统计尾端一格，避免一张床被计为两张；
        // 26.3 新增的麦秆床同属床类、带同样的 part 属性，走本分支
        String bedPart = getEnumStateConstant(state, "part");
        if ("FOOT".equals(bedPart)) {
            return 1;
        }
        if ("HEAD".equals(bedPart)) {
            return 0;
        }
        // 双半砖：一格内由两块半砖合成，按两件计
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.SLAB_TYPE)) {
            return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.SLAB_TYPE) == net.minecraft.world.level.block.state.properties.SlabType.DOUBLE ? 2 : 1;
        }
        return 1;
    }

    /**
     * 按状态属性的序列化名读取整型属性值（如 layers、segment_amount、charges）。
     * 属性名由原版以字符串定义、跨版本与映射保持稳定，故不依赖具体映射的常量字段。
     * 属性不存在时返回 -1。
     */
    private static int getIntStateProperty(BlockState state, String propertyName) {
        for (net.minecraft.world.level.block.state.properties.Property<?> property : state.getProperties()) {
            if (property.getName().equals(propertyName)
                    && property instanceof net.minecraft.world.level.block.state.properties.IntegerProperty intProperty) {
                return state.getValue(intProperty);
            }
        }
        return -1;
    }

    /** 按状态属性的序列化名读取布尔属性值（如 eye）；属性不存在时返回 false。 */
    private static boolean getBoolStateProperty(BlockState state, String propertyName) {
        for (net.minecraft.world.level.block.state.properties.Property<?> property : state.getProperties()) {
            if (property.getName().equals(propertyName)
                    && property instanceof net.minecraft.world.level.block.state.properties.BooleanProperty boolProperty) {
                return state.getValue(boolProperty);
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
        for (net.minecraft.world.level.block.state.properties.Property<?> property : state.getProperties()) {
            if (property.getName().equals(propertyName)) {
                return String.valueOf(state.getValue((net.minecraft.world.level.block.state.properties.Property) property));
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
     * 解析 Sponge 格式（byte[] CompoundTag Palette）。
     */
    private static void parseSpongeFormat(CompoundTag rootTag, MaterialSink buildingSink,
                                          MaterialSink containerSink,
                                          LitematicaBridge.LayerFilter filter) {
        // 读取尺寸（使用绝对值以防负数）
        short width = (short) Math.abs(rootTag.getShort("Width").orElse((short) 0));
        short height = (short) Math.abs(rootTag.getShort("Height").orElse((short) 0));
        short length = (short) Math.abs(rootTag.getShort("Length").orElse((short) 0));
        int totalBlocks = width * height * length;
        LitematListMod.LOGGER.info("Sponge 格式尺寸: {}x{}x{} ({} 方块)", width, height, length, totalBlocks);

        // 读取 Palette（CompoundTag 格式，key=blockStateString, value=index）
        Optional<CompoundTag> paletteOpt = rootTag.getCompound("Palette");
        if (paletteOpt.isEmpty()) {
            LitematListMod.LOGGER.warn("Sponge 格式缺少 Palette");
            return;
        }
        CompoundTag palette = paletteOpt.get();
        int paletteSize = palette.keySet().size();
        LitematListMod.LOGGER.info("Sponge 调色板大小: {}", paletteSize);

        // 解析调色板：index -> BlockState
        List<BlockState> paletteStates = new ArrayList<>(paletteSize);
        for (int i = 0; i < paletteSize; i++) {
            paletteStates.add(Blocks.AIR.defaultBlockState());
        }
        for (String key : palette.keySet()) {
            int index = palette.getInt(key).orElse(-1);
            if (index >= 0 && index < paletteSize) {
                try {
                    BlockState state = parseSpongePaletteEntry(key, index);
                    paletteStates.set(index, state);
                } catch (Exception e) {
                    LitematListMod.LOGGER.warn("Sponge 调色板条目 '{}' 解析失败: {}", key, e.getMessage());
                }
            }
        }

        // 读取 BlockData（byte[]，可变长度整数编码）
        Optional<byte[]> blockDataOpt = rootTag.getByteArray("BlockData");
        if (blockDataOpt.isEmpty()) {
            LitematListMod.LOGGER.warn("Sponge 格式缺少 BlockData");
            return;
        }
        byte[] blockData = blockDataOpt.get();
        LitematListMod.LOGGER.info("Sponge BlockData 大小: {} 字节", blockData.length);

        // 解码可变长度整数
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
            return Blocks.AIR.defaultBlockState();
        }

        Block block = BuiltInRegistries.BLOCK.get(blockId).get().value();
        BlockState state = block.defaultBlockState();

        if (propsStr != null && !propsStr.isEmpty()) {
            String[] props = propsStr.split(",");
            for (String prop : props) {
                String[] kv = prop.split("=", 2);
                if (kv.length == 2) {
                    for (net.minecraft.world.level.block.state.properties.Property<?> property : state.getProperties()) {
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
    private static void parseRegion(CompoundTag region, MaterialSink buildingSink, MaterialSink containerSink,
                                     MaterialSink entitySink, MaterialSink entityContainerSink,
                                     String regionName, int version,
                                     LitematicaBridge.LayerFilter filter) {
        // 0. 读取区域原点（Position），用于渲染层过滤时换算相对层级（缺失时视为 0）
        int posX = 0, posY = 0, posZ = 0;
        Optional<CompoundTag> posOpt = region.getCompound("Position");
        if (posOpt.isPresent()) {
            CompoundTag posTag = posOpt.get();
            posX = posTag.getInt("x").orElse(0);
            posY = posTag.getInt("y").orElse(0);
            posZ = posTag.getInt("z").orElse(0);
        }
        // 1. 读取 Size
        Optional<CompoundTag> sizeOpt = region.getCompound("Size");
        if (sizeOpt.isEmpty()) {
            LitematListMod.LOGGER.warn("Region '{}' 缺少 Size 字段", regionName);
            return;
        }
        CompoundTag sizeTag = sizeOpt.get();
        // 与投影 PositionUtils.getMinCorner 一致：Size 为负的轴，Position 保存的是该轴的
        // 「最大角」而非最小角，最小角 = Position + Size + 1（原理图可向负方向延伸）
        int rawSx = sizeTag.getInt("x").orElse(0);
        int rawSy = sizeTag.getInt("y").orElse(0);
        int rawSz = sizeTag.getInt("z").orElse(0);
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
        Optional<ListTag> paletteOpt = region.getList("BlockStatePalette");
        if (paletteOpt.isEmpty()) {
            LitematListMod.LOGGER.warn("Region '{}' 缺少 BlockStatePalette", regionName);
            return;
        }
        ListTag palette = paletteOpt.get();
        LitematListMod.LOGGER.info("Region '{}' 调色板大小: {}", regionName, palette.size());

        // 解析调色板条目
        List<BlockState> paletteStates = new ArrayList<>(palette.size());
        int failedEntries = 0;
        for (int i = 0; i < palette.size(); i++) {
            Tag element = palette.get(i);
            if (element instanceof CompoundTag stateTag) {
                try {
                    paletteStates.add(parsePaletteEntry(stateTag, i));
                } catch (Exception e) {
                    failedEntries++;
                    LitematListMod.LOGGER.warn("调色板条目 #{} 解析失败，使用空气方块替代: {}", i, e.getMessage());
                    paletteStates.add(Blocks.AIR.defaultBlockState());
                }
            } else {
                failedEntries++;
                LitematListMod.LOGGER.warn("调色板条目 #{} 不是 CompoundTag，类型: {}", i,
                        element != null ? element.getClass().getSimpleName() : "null");
                paletteStates.add(Blocks.AIR.defaultBlockState());
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
        Tag blockStatesElement = region.get("BlockStates");
        if (blockStatesElement == null) {
            LitematListMod.LOGGER.warn("Region '{}' 缺少 BlockStates", regionName);
            return;
        }

        long[] blockStates = null;
        int blockStatesType = blockStatesElement.getId();

        if (blockStatesType == Tag.TAG_LONG_ARRAY) {
            blockStates = ((LongArrayTag) blockStatesElement).getAsLongArray();
            LitematListMod.LOGGER.info("Region '{}' BlockStates 类型: LongArrayTag ({} 个 long)", regionName, blockStates.length);
        } else if (blockStatesType == Tag.TAG_INT_ARRAY) {
            // 兼容：某些工具可能使用 int[] 存储
            int[] intArray = ((IntArrayTag) blockStatesElement).getAsIntArray();
            blockStates = new long[intArray.length];
            for (int i = 0; i < intArray.length; i++) {
                blockStates[i] = intArray[i] & 0xFFFFFFFFL;
            }
            LitematListMod.LOGGER.info("Region '{}' BlockStates 类型: IntArrayTag ({} 个 int)", regionName, blockStates.length);
        } else if (blockStatesType == Tag.TAG_BYTE_ARRAY) {
            // 兼容：Sponge 格式内嵌
            byte[] byteArray = ((ByteArrayTag) blockStatesElement).getAsByteArray();
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
            LitematListMod.LOGGER.info("Region '{}' BlockStates 类型: ByteArrayTag ({} 字节，解码后 {} 个 long)",
                    regionName, byteArray.length, blockStates.length);
        } else if (blockStatesType == Tag.TAG_LIST) {
            ListTag list = (ListTag) blockStatesElement;
            blockStates = new long[list.size()];
            int validCount = 0;
            for (int i = 0; i < list.size(); i++) {
                Tag item = list.get(i);
                if (item instanceof LongTag nbtLong) {
                    blockStates[i] = nbtLong.longValue();
                    validCount++;
                } else if (item instanceof IntTag nbtInt) {
                    blockStates[i] = nbtInt.intValue() & 0xFFFFFFFFL;
                    validCount++;
                }
            }
            LitematListMod.LOGGER.info("Region '{}' BlockStates 类型: ListTag ({} 个 long，{} 有效)",
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

        // 特殊纹饰陶罐预扫描：带非默认瓦片（非 brick）的陶罐，其方块本身以「含特殊瓦片的变种」
        // 精确物品计入材料列表（ID 含 pot_decorations），而非普通陶罐方块。
        // 瓦片数据来自方块实体，故「是否计算原理图实体」「是否计算容器数据」任一项开启即解析。
        Map<String, ListTag> specialPots = (calcContainerData || calcSchematicEntities)
                ? collectSpecialDecoratedPots(region, "TileEntities", posX, posY, posZ, filter)
                : Collections.emptyMap();

        // 箱类方块状态表（区域相对坐标 "x,y,z" → 方块状态），供容器方块实体分类：
        // 1.21.9+ 箱子类方块实体的 id 统一为 minecraft:chest（含全部铜箱子），无法从 id 区分家族；
        // 且方块状态的 type=left/right 是「属于大型箱子」的权威标记，孤立半箱也应判为大型箱子，
        // 不应依赖相邻方块判定（原理图状态墙等场景中半箱彼此并不相邻）。
        Map<String, BlockState> chestFamilyStates = new HashMap<>();

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
                    // 特殊纹饰陶罐：以精确ID计入，不再按普通陶罐方块统计。
                    // 方块索引 → 区域内相对坐标：x 最快、其次 z、最慢 y（与 BlockStates 位流一致）。
                    ListTag potSherds = specialPots.get(
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
        if (calcSchematicEntities) {
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
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
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
        Item item = BuiltInRegistries.ITEM.get(Identifier.tryParse("minecraft:" + plant)).map(Holder::value).orElse(null);
        if (item == null || item == Items.AIR) {
            return;
        }
        itemCounts.merge(item, 1, Integer::sum);
        itemNames.putIfAbsent(item, new ItemStack(item).getHoverName().getString());
    }

    /**
     * 解析原理图中的容器方块实体列表，将容器内物品并入材料统计。
     * 兼容 Litematica 原生格式（TileEntities，方块实体数据内联）
     * 与 Sponge 格式（v1: TileEntities + TileNBT 嵌套；v2+: BlockEntities）。
     */
    private static void parseTileEntityList(CompoundTag containerTag, String listKey,
                                            MaterialSink sink, String regionName,
                                            int regionPosX, int regionPosY, int regionPosZ,
                                            LitematicaBridge.LayerFilter filter,
                                            Map<String, BlockState> chestFamilyStates) {
        if (containerTag.contains(listKey) == false) {
            return;
        }
        Optional<ListTag> teListOpt = containerTag.getList(listKey);
        if (teListOpt.isEmpty()) {
            return;
        }
        ListTag teList = teListOpt.get();
        int containerItemCount = 0;

        // 预扫描：收集箱类方块实体坐标（用于相邻配对判定大型箱子）。
        // 配对是结构属性，与渲染层过滤无关，因此预扫描不做层过滤。
        Set<String> chestCoords = new HashSet<>();
        Set<String> trappedChestCoords = new HashSet<>();
        Set<String> copperChestCoords = new HashSet<>();
        for (int i = 0; i < teList.size(); i++) {
            Tag element = teList.get(i);
            if (!(element instanceof CompoundTag teTag)) {
                continue;
            }
            Optional<CompoundTag> tileNbtOpt = teTag.getCompound("TileNBT");
            CompoundTag tileNbt = tileNbtOpt.orElse(teTag);
            Optional<String> idOpt = tileNbt.getString("id");
            if (idOpt.isEmpty() || !teTag.contains("x") || !teTag.contains("y") || !teTag.contains("z")) {
                continue;
            }
            String teId = idOpt.get();
            String coord = teTag.getInt("x").orElse(0) + "," + teTag.getInt("y").orElse(0)
                    + "," + teTag.getInt("z").orElse(0);
            if ("minecraft:chest".equals(teId)) {
                chestCoords.add(coord);
            } else if ("minecraft:trapped_chest".equals(teId)) {
                trappedChestCoords.add(coord);
            } else if (teId.contains("copper_chest")) {
                copperChestCoords.add(coord);
            }
        }

        for (int i = 0; i < teList.size(); i++) {
            Tag element = teList.get(i);
            if (!(element instanceof CompoundTag teTag)) {
                continue;
            }
            // 渲染层过滤：方块实体的 x/y/z 为区域内相对坐标（缺失时跳过过滤，保留原行为）
            if (filter != null && teTag.contains("x") && teTag.contains("y") && teTag.contains("z")) {
                int teX = teTag.getInt("x").orElse(0);
                int teY = teTag.getInt("y").orElse(0);
                int teZ = teTag.getInt("z").orElse(0);
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
            Optional<CompoundTag> tileNbtOpt = teTag.getCompound("TileNBT");
            CompoundTag tileNbt = tileNbtOpt.orElse(teTag);

            Optional<String> idOpt = tileNbt.getString("id");
            if (idOpt.isEmpty()) {
                // 无 id 的方块实体（少数模组容器）：维持旧行为直接解析 Items
                containerItemCount += parseContainerItems(tileNbt, sink);
                continue;
            }
            String teId = idOpt.get();
            int teX = teTag.getInt("x").orElse(0);
            int teY = teTag.getInt("y").orElse(0);
            int teZ = teTag.getInt("z").orElse(0);
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
                        Tag bookElement = tileNbt.get("Book");
                        boolean counted = false;
                        if (bookElement instanceof CompoundTag bookTag && nonDefaultComponents(bookTag) != null) {
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
            String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
            boolean large = state.hasProperty(ChestBlock.TYPE)
                    && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE;
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
            // 展示架（1.21.9+，各木种共用方块实体 id）与唱片机
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
    private static int countChiseledBookshelfBooks(CompoundTag tileNbt, Map<Item, Integer> itemCounts,
                                                   Map<Item, String> itemNames,
                                                   Map<String, ExactItem> exactItems) {
        if (tileNbt.contains("Items") == false) {
            return 0;
        }
        Optional<ListTag> itemsOpt = tileNbt.getList("Items");
        if (itemsOpt.isEmpty()) {
            return 0;
        }
        int books = 0;
        int exactBooks = 0;
        for (int i = 0; i < itemsOpt.get().size(); i++) {
            Tag element = itemsOpt.get().get(i);
            if (!(element instanceof CompoundTag itemTag)) {
                continue;
            }
            Optional<String> idOpt = itemTag.getString("id");
            if (idOpt.isEmpty()) {
                continue;
            }
            Identifier itemId = Identifier.tryParse(idOpt.get());
            if (itemId == null || BuiltInRegistries.ITEM.get(itemId).map(Holder::value).orElse(Items.AIR) == Items.AIR) {
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
    private static Map<String, ListTag> collectSpecialDecoratedPots(CompoundTag region, String listKey,
                                                                    int regionPosX, int regionPosY, int regionPosZ,
                                                                    LitematicaBridge.LayerFilter filter) {
        if (region.contains(listKey) == false) {
            return Collections.emptyMap();
        }
        Optional<ListTag> teListOpt = region.getList(listKey);
        if (teListOpt.isEmpty()) {
            return Collections.emptyMap();
        }
        ListTag teList = teListOpt.get();
        Map<String, ListTag> result = new LinkedHashMap<>();
        for (int i = 0; i < teList.size(); i++) {
            Tag element = teList.get(i);
            if (!(element instanceof CompoundTag teTag)) {
                continue;
            }
            // 兼容 Sponge v1：数据嵌套在 TileNBT 键下
            CompoundTag tileNbt = teTag.getCompound("TileNBT").orElse(teTag);
            Optional<String> idOpt = tileNbt.getString("id");
            if (idOpt.isEmpty() || !"minecraft:decorated_pot".equals(idOpt.get())) {
                continue;
            }
            Optional<ListTag> sherdsOpt = tileNbt.getList("sherds");
            if (sherdsOpt.isEmpty() || !hasSpecialSherds(sherdsOpt.get())) {
                continue;
            }
            boolean hasCoords = teTag.contains("x") && teTag.contains("y") && teTag.contains("z");
            int teX = teTag.getInt("x").orElse(0);
            int teY = teTag.getInt("y").orElse(0);
            int teZ = teTag.getInt("z").orElse(0);
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
            result.put(teX + "," + teY + "," + teZ, sherdsOpt.get().copy());
        }
        return result;
    }

    /** sherds 中是否存在非默认瓦片（minecraft:brick 为无纹饰默认值）。 */
    private static boolean hasSpecialSherds(ListTag sherds) {
        for (int i = 0; i < sherds.size(); i++) {
            Optional<String> value = sherds.getString(i);
            if (value.isPresent() && "minecraft:brick".equals(value.get()) == false) {
                return true;
            }
        }
        return false;
    }

    /**
     * 把「带特殊瓦片的陶罐」以精确物品计入：ID 形如
     * {@code minecraft:decorated_pot[pot_decorations=["minecraft:flow_pottery_sherd",...]]}。
     * 1.20.5+ 起陶罐纹饰使用物品组件 {@code minecraft:pot_decorations}（瓦片 ID 列表），
     * 该组件驱动物品模型（按瓦片显示纹饰材质）与悬停提示（名称下方四行瓦片信息）；
     * 旧实现用 {@code block_entity_data.sherds} 属方块实体数据，物品模型/提示均不识别。
     */
    private static void addDecoratedPotExact(ListTag sherds, Map<String, ExactItem> exactItems) {
        if (exactItems == null) {
            return;
        }
        Item item = Items.DECORATED_POT;
        if (item == null || item == Items.AIR) {
            return;
        }
        CompoundTag components = new CompoundTag();
        components.put("minecraft:pot_decorations", sherds);
        String exactId = buildExactId(BuiltInRegistries.ITEM.getKey(item), components, false);
        CompoundTag itemTag = new CompoundTag();
        itemTag.putString("id", "minecraft:decorated_pot");
        itemTag.putInt("count", 1);
        itemTag.put("components", components);
        addExactItem(exactItems, exactId, item, decodeExactStack(itemTag, item, 1), 1);
    }

    /**
     * 陶罐：单格存储物品，读 "item" 键（新格式为 {id, count/Count} 复合标签，
     * 旧格式为物品 id 字符串），计入材料统计。
     */
    private static int countDecoratedPotItem(CompoundTag tileNbt, MaterialSink sink) {
        if (tileNbt.contains("item") == false) {
            return 0;
        }
        Tag itemElement = tileNbt.get("item");
        if (itemElement instanceof CompoundTag itemTag) {
            // 统一入口：带非默认组件时以精确ID单独统计
            if (addItemEntry(itemTag, sink.itemCounts, sink.itemNames, sink.exactItems) == false) {
                return 0;
            }
            // 罐内存物若本身带容器数据（潜影盒/收纳袋等），其内部物品一并解析
            parseItemEmbeddedContainer(itemTag, sink, 1);
            return 1;
        } else if (itemElement instanceof StringTag idTag) {
            Optional<String> idValue = idTag.asString();
            if (idValue.isEmpty()) {
                return 0;
            }
            Identifier itemId = Identifier.tryParse(idValue.get());
            if (itemId == null) {
                return 0;
            }
            Item item = BuiltInRegistries.ITEM.get(itemId).map(Holder::value).orElse(null);
            if (item == null || item == Items.AIR) {
                return 0;
            }
            addSpecialItem(item, sink.itemCounts, sink.itemNames);
            return 1;
        }
        return 0;
    }

    /**
     * 唱片机：统计机内放入的唱片（归类为容器数据）。1.21+ 唱片内容保存在方块实体的
     * RecordItem 复合标签中；为避免不同版本键名差异，依次尝试常见键名，
     * 均未命中时回退为通用 Items 列表解析。返回计入的物品数。
     */
    private static int countJukeboxRecord(CompoundTag tileNbt, MaterialSink sink) {
        for (String key : new String[]{"RecordItem", "record_item", "Record"}) {
            Tag element = tileNbt.get(key);
            if (element instanceof CompoundTag recordTag
                    && addItemEntry(recordTag, sink.itemCounts, sink.itemNames, sink.exactItems)) {
                return 1;
            }
        }
        return parseContainerItems(tileNbt, sink);
    }

    /**
     * 从单个方块实体 NBT 中读取 Items 列表并计入材料统计，返回计入的物品数。
     */
    private static int parseContainerItems(CompoundTag tileNbt, MaterialSink sink) {
        if (tileNbt.contains("Items") == false) {
            return 0;
        }
        Optional<ListTag> itemsOpt = tileNbt.getList("Items");
        if (itemsOpt.isEmpty()) {
            return 0;
        }
        return parseItemList(itemsOpt.get(), sink);
    }

    /** 嵌套容器（如箱子/陶罐内放潜影盒、收纳袋再套潜影盒）解析的最大递归深度，防异常数据无限递归。 */
    private static final int MAX_CONTAINER_NEST_DEPTH = 8;

    /**
     * 解析物品列表（条目为 {id, count/Count}，如容器 Items），计入材料统计，返回计入的物品数。
     * 带非默认组件的特殊物品以精确ID单独归并；普通物品并入 itemCounts/itemNames。
     * 条目自身若为潜影盒/收纳袋等自带容器的物品，其内部物品一并解析（陶罐内放潜影盒等场景）。
     */
    private static int parseItemList(ListTag items, MaterialSink sink) {
        return parseItemList(items, sink, 0);
    }

    private static int parseItemList(ListTag items, MaterialSink sink, int depth) {
        int count = 0;

        for (int i = 0; i < items.size(); i++) {
            Tag element = items.get(i);
            if (!(element instanceof CompoundTag itemTag)) {
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

    /**
     * 解析物品内嵌容器中的物品（展示框内潜影盒/收纳袋、以及任意容器内的潜影盒/收纳袋等），
     * 归类为容器数据。兼容现代组件格式：潜影盒 {@code components."minecraft:container"}
     * （{slot,item} 条目）、收纳袋 {@code components."minecraft:bundle_contents"}（物品条目列表）；
     * 以及旧格式：物品携带的 {@code BlockEntityTag.Items} 或顶层 {@code Items}。
     * depth 用于限制嵌套容器递归深度（潜影盒内再套潜影盒等）。
     */
    private static int parseItemEmbeddedContainer(CompoundTag itemTag, MaterialSink sink) {
        return parseItemEmbeddedContainer(itemTag, sink, 0);
    }

    private static int parseItemEmbeddedContainer(CompoundTag itemTag, MaterialSink sink, int depth) {
        int count = 0;
        Optional<CompoundTag> componentsOpt = itemTag.getCompound("components");
        if (componentsOpt.isPresent()) {
            CompoundTag components = componentsOpt.get();
            // 潜影盒等容器组件：minecraft:container -> [{slot, item:{id,count,...}}, ...]
            Optional<ListTag> containerOpt = components.getList("minecraft:container");
            if (containerOpt.isPresent()) {
                ListTag slots = containerOpt.get();
                for (int i = 0; i < slots.size(); i++) {
                    if (!(slots.get(i) instanceof CompoundTag slotEntry)) {
                        continue;
                    }
                    Optional<CompoundTag> innerOpt = slotEntry.getCompound("item");
                    if (innerOpt.isPresent()
                            && addItemEntry(innerOpt.get(), sink.itemCounts, sink.itemNames, sink.exactItems)) {
                        count++;
                        // 容器内再放的潜影盒/收纳袋同样展开其内部物品
                        if (depth < MAX_CONTAINER_NEST_DEPTH) {
                            count += parseItemEmbeddedContainer(innerOpt.get(), sink, depth + 1);
                        }
                    }
                }
            }
            // 收纳袋组件：minecraft:bundle_contents -> [{id,count,...}, ...]
            Optional<ListTag> bundleOpt = components.getList("minecraft:bundle_contents");
            if (bundleOpt.isPresent() && depth < MAX_CONTAINER_NEST_DEPTH) {
                count += parseItemList(bundleOpt.get(), sink, depth + 1);
            }
        }
        // 旧格式（1.20.4- 及更早）：物品自定义数据在 tag 键内——
        // 潜影盒/箱类 tag.BlockEntityTag.Items、收纳袋 tag.Items
        Optional<CompoundTag> legacyTagOpt = itemTag.getCompound("tag");
        if (legacyTagOpt.isPresent()) {
            Optional<CompoundTag> legacyBlockEntityOpt = legacyTagOpt.get().getCompound("BlockEntityTag");
            if (legacyBlockEntityOpt.isPresent()) {
                count += parseContainerItems(legacyBlockEntityOpt.get(), sink);
            }
            count += parseContainerItems(legacyTagOpt.get(), sink);
        }
        // 旧格式：物品携带的 BlockEntityTag.Items（潜影盒物品）
        Optional<CompoundTag> blockEntityOpt = itemTag.getCompound("BlockEntityTag");
        if (blockEntityOpt.isPresent()) {
            count += parseContainerItems(blockEntityOpt.get(), sink);
        }
        // 旧格式（1.20.4-）：物品顶层直接带 Items
        count += parseContainerItems(itemTag, sink);
        return count;
    }

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
    private static void parseEntityList(CompoundTag region, MaterialSink entitySink,
                                        MaterialSink entityContainerSink, String regionName,
                                        int regionPosX, int regionPosY, int regionPosZ,
                                        LitematicaBridge.LayerFilter filter) {
        if (region.contains("Entities") == false) {
            return;
        }
        Optional<ListTag> entitiesOpt = region.getList("Entities");
        if (entitiesOpt.isEmpty()) {
            return;
        }
        ListTag entities = entitiesOpt.get();
        boolean calcEntityContainers =
                com.litematlist.config.Configs.FeatureToggles.CALC_SCHEMATIC_ENTITY_CONTAINERS.getBooleanValue();
        int entityCount = 0;
        int containerItemCount = 0;

        for (int i = 0; i < entities.size(); i++) {
            Tag element = entities.get(i);
            if (!(element instanceof CompoundTag entityTag)) {
                continue;
            }
            // 渲染层过滤：Pos 为区域相对坐标（相对区域最小角）
            if (filter != null && entityTag.contains("Pos")) {
                Optional<ListTag> posOpt = entityTag.getList("Pos");
                if (posOpt.isPresent() && posOpt.get().size() >= 3) {
                    ListTag pos = posOpt.get();
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
            Optional<String> idOpt = entityTag.getString("id");
            if (idOpt.isEmpty()) {
                continue;
            }
            String entityId = idOpt.get();

            // 实体本身对应的物品（矿车/盔甲架/船/箱船等）
            Item entityItem = resolveEntityItem(entityId, entityTag);
            if (entityItem != null && entityItem != Items.AIR) {
                entitySink.itemCounts.merge(entityItem, 1, Integer::sum);
                entitySink.itemNames.putIfAbsent(entityItem, new ItemStack(entityItem).getHoverName().getString());
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

            // 硫方怪（26.2+）：吞下的方块存于装备栏 BODY 槽，计入实体数据
            if ("minecraft:sulfur_cube".equals(entityId)) {
                containerItemCount += parseSulfurCubeContent(entityTag, entitySink);
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
    private static int parseItemFrameContent(String entityId, CompoundTag entityTag,
                                             MaterialSink entitySink, MaterialSink entityContainerSink,
                                             boolean calcEntityContainers) {
        String typeKey = "minecraft:glow_item_frame".equals(entityId) ? "glowItemFrame" : "itemFrame";
        if (com.litematlist.config.Configs.Statistics.enabled(typeKey) == false) {
            return 0;
        }
        if (entityTag.contains("Item") == false) {
            return 0;
        }
        Tag itemElement = entityTag.get("Item");
        if (!(itemElement instanceof CompoundTag frameItemTag)) {
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

    /** 硫方怪吸收方块的备选顶层键（主位置 equipment.body 之外的兼容候选）。 */
    private static final String[] SULFUR_CUBE_ITEM_KEYS = {
            "absorbed_block", "absorbed_block_item_stack", "content", "sulfur_cube_content",
            "BodyItem", "body_item"
    };

    /** 硫方怪吸收方块的备选组件键（可能嵌在 components 复合标签内）。 */
    private static final String[] SULFUR_CUBE_COMPONENT_KEYS = {
            "minecraft:sulfur_cube_content", "sulfur_cube_content",
            "minecraft:absorbed_block_item_stack", "absorbed_block_item_stack",
            "content", "absorbed_block"
    };

    /**
     * 硫方怪（26.2+）吸收的方块：经 javap 确认 SulfurCube#setSulfurCubeContent 调用
     * setItemSlotAndDropWhenKilled(EquipmentSlot.BODY, ...)，而 LivingEntity 把装备栏序列化为
     * "equipment" 复合标签（键为装备槽序列化名，如 body/head/...）。故主位置是
     * entityTag.equipment.body 内的物品堆叠标签。为稳健起见依次尝试多个候选键，
     * 命中即以 addItemEntry 计入实体数据；全部未命中则不计数（命中/未命中均打日志）。
     * 返回计入的物品数。
     */
    private static int parseSulfurCubeContent(CompoundTag entityTag, MaterialSink entitySink) {
        CompoundTag itemTag = null;
        String hitKey = null;

        // 主位置：equipment.body（SulfurCube.setSulfurCubeContent -> EquipmentSlot.BODY）
        Optional<CompoundTag> equipmentOpt = entityTag.getCompound("equipment");
        if (equipmentOpt.isPresent()) {
            Optional<CompoundTag> bodyOpt = equipmentOpt.get().getCompound("body");
            if (bodyOpt.isPresent() && bodyOpt.get().contains("id")) {
                itemTag = bodyOpt.get();
                hitKey = "equipment.body";
            }
        }

        // 备选一：实体顶层候选键
        if (itemTag == null) {
            for (String key : SULFUR_CUBE_ITEM_KEYS) {
                Optional<CompoundTag> opt = entityTag.getCompound(key);
                if (opt.isPresent() && opt.get().contains("id")) {
                    itemTag = opt.get();
                    hitKey = key;
                    break;
                }
            }
        }

        // 备选二：components 复合标签中的组件同名键
        if (itemTag == null) {
            Optional<CompoundTag> componentsOpt = entityTag.getCompound("components");
            if (componentsOpt.isPresent()) {
                CompoundTag components = componentsOpt.get();
                for (String key : SULFUR_CUBE_COMPONENT_KEYS) {
                    Optional<CompoundTag> opt = components.getCompound(key);
                    if (opt.isPresent() && opt.get().contains("id")) {
                        itemTag = opt.get();
                        hitKey = "components." + key;
                        break;
                    }
                }
            }
        }

        if (itemTag != null
                && addItemEntry(itemTag, entitySink.itemCounts, entitySink.itemNames, entitySink.exactItems)) {
            LitematListMod.LOGGER.info("硫方怪吸收方块: 命中键 '{}'，计入物品 {}",
                    hitKey, itemTag.getString("id").orElse("?"));
            return 1;
        }

        // 未计数：若实体看起来含吸收方块（有装备栏/组件标签等）则告警，便于后续排查
        boolean maybeHasContent = entityTag.contains("equipment") || entityTag.contains("components");
        if (maybeHasContent == false) {
            for (String key : SULFUR_CUBE_ITEM_KEYS) {
                if (entityTag.contains(key)) {
                    maybeHasContent = true;
                    break;
                }
            }
        }
        if (maybeHasContent) {
            LitematListMod.LOGGER.warn("硫方怪吸收方块未识别: 实体标签键 {}", entityTag.keySet());
        }
        return 0;
    }

    /** 读取 ListTag 中第 index 个数值元素（须为 NumericTag），失败返回 0。 */
    private static double listNumber(Tag element) {
        if (element instanceof net.minecraft.nbt.NumericTag number) {
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
    private static Item resolveEntityItem(String entityId, CompoundTag entityTag) {
        // 坐垫（26.3+）：实体 id minecraft:cushion 没有同名物品，
        // 按存档 "color" 字段（DyeColor 序列化名，如 white/red/light_blue）映射为
        // minecraft:<color>_cushion；color 缺失/为空/拼不出物品时回退白色
        // （Cushion.DEFAULT_COLOR = DyeColor.WHITE -> minecraft:white_cushion）。
        if ("minecraft:cushion".equals(entityId)) {
            String color = entityTag.getString("color").orElse("");
            Item cushion = null;
            if (color.isEmpty() == false) {
                cushion = BuiltInRegistries.ITEM
                        .get(Identifier.tryParse("minecraft:" + color + "_cushion"))
                        .map(Holder::value).orElse(null);
            }
            if (cushion != null && cushion != Items.AIR) {
                return cushion;
            }
            return BuiltInRegistries.ITEM
                    .get(Identifier.tryParse("minecraft:white_cushion"))
                    .map(Holder::value).orElse(null);
        }
        Identifier directId = Identifier.tryParse(entityId);
        if (directId != null) {
            Item direct = BuiltInRegistries.ITEM.get(directId).map(Holder::value).orElse(null);
            if (direct != null && direct != Items.AIR) {
                return direct;
            }
        }
        // 旧格式通用船实体：按 "Type" 字段映射为具体木种的船物品
        if ("minecraft:boat".equals(entityId) || "minecraft:chest_boat".equals(entityId)) {
            Optional<String> typeOpt = entityTag.getString("Type");
            if (typeOpt.isPresent() && typeOpt.get().isEmpty() == false) {
                String type = typeOpt.get();
                boolean chest = entityId.endsWith("chest_boat");
                String itemName;
                if ("bamboo".equals(type)) {
                    itemName = chest ? "bamboo_chest_raft" : "bamboo_raft";
                } else {
                    itemName = chest ? type + "_chest_boat" : type + "_boat";
                }
                Item typed = BuiltInRegistries.ITEM.get(Identifier.tryParse("minecraft:" + itemName)).map(Holder::value).orElse(null);
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
    private static void countSummonMaterials(String entityId, CompoundTag entityTag,
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
            // 1.21.9+ 原版铜傀儡：weather_state 字符串枚举 + next_weather_age==-2 表示涂蜡
            Optional<String> weatherStateOpt = entityTag.getString("weather_state");
            if (weatherStateOpt.isPresent()) {
                oxidation = switch (weatherStateOpt.get()) {
                    case "exposed" -> 1;
                    case "weathered" -> 2;
                    case "oxidized" -> 3;
                    default -> 0;
                };
                waxed = entityTag.getLong("next_weather_age").orElse(-1L) == -2L;
            } else {
                // 旧格式（friendsandfoes 等模组）：OxidationLevel 整数 + Waxed 布尔（键名双写兼容大小写差异）
                oxidation = entityTag.getInt("OxidationLevel").orElse(entityTag.getInt("oxidationLevel").orElse(0));
                waxed = entityTag.getBoolean("Waxed").orElse(entityTag.getBoolean("waxed").orElse(false));
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
        return BuiltInRegistries.ITEM.get(Identifier.tryParse("minecraft:" + name))
                .map(Holder::value).orElse(Items.AIR);
    }

    /**
     * 统计容器实体中的容器数据（开关「是否计算原理图实体容器数据」开启时调用）。
     * 覆盖：漏斗矿车/箱子矿车（Items）、箱船/箱筏（Items）、有箱子的驴/羊驼（Items）。
     * （盔甲架穿戴/手持的物品不在本方法处理——它们跟随「是否计算原理图实体」开关，
     * 由 parseEntityList 直接统计。）
     * 已知容器实体类型受「统计」页对应类型开关门控。返回计入的物品数。
     */
    private static int parseEntityContainer(String entityId, CompoundTag entityTag, MaterialSink sink) {
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
    private static boolean isChestedHorse(CompoundTag entityTag) {
        if (entityTag.contains("ChestedHorse")) {
            return entityTag.getBoolean("ChestedHorse").orElse(false);
        }
        if (entityTag.contains("chestedHorse")) {
            return entityTag.getBoolean("chestedHorse").orElse(false);
        }
        return entityTag.contains("Items");
    }

    /**
     * 统计盔甲架穿戴/手持的物品并展开盔甲上的纹饰（纹饰锻造模板 + 镶嵌材质），
     * 返回计入的物品数。
     * 新版存档使用 equipment 复合标签（feet/legs/chest/head/mainhand/offhand
     * 槽位，值为 {id, count, components} 物品），旧格式为 ArmorItems / HandItems 列表，
     * 两种格式均兼容。
     */
    private static int parseArmorStandItems(CompoundTag entityTag, Map<Item, Integer> itemCounts,
                                             Map<Item, String> itemNames, Map<String, ExactItem> exactItems) {
        int count = 0;
        Optional<CompoundTag> equipOpt = entityTag.getCompound("equipment");
        if (equipOpt.isPresent()) {
            CompoundTag equipment = equipOpt.get();
            for (String slotKey : equipment.keySet()) {
                Tag slotElement = equipment.get(slotKey);
                if (slotElement instanceof CompoundTag slotTag
                        && parseArmorItem(slotTag, itemCounts, itemNames, exactItems)) {
                    count++;
                }
            }
        } else {
            Optional<ListTag> armorOpt = entityTag.getList("ArmorItems");
            if (armorOpt.isPresent()) {
                count += parseArmorItemList(armorOpt.get(), itemCounts, itemNames, exactItems);
            }
            Optional<ListTag> handOpt = entityTag.getList("HandItems");
            if (handOpt.isPresent()) {
                count += parseArmorItemList(handOpt.get(), itemCounts, itemNames, exactItems);
            }
        }
        return count;
    }

    /**
     * 解析盔甲架单个物品栏列表（旧格式 ArmorItems/HandItems）：逐项统计物品本身，
     * 并额外展开其纹饰（模板 + 材质）。
     */
    private static int parseArmorItemList(ListTag items, Map<Item, Integer> itemCounts,
                                          Map<Item, String> itemNames, Map<String, ExactItem> exactItems) {
        int count = 0;
        for (int i = 0; i < items.size(); i++) {
            Tag element = items.get(i);
            if (element instanceof CompoundTag itemTag
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
    private static boolean parseArmorItem(CompoundTag itemTag, Map<Item, Integer> itemCounts,
                                          Map<Item, String> itemNames, Map<String, ExactItem> exactItems) {
        Optional<String> idOpt = itemTag.getString("id");
        if (idOpt.isEmpty()) {
            return false;
        }
        Identifier itemId = Identifier.tryParse(idOpt.get());
        if (itemId == null) {
            return false;
        }
        Item item = BuiltInRegistries.ITEM.get(itemId).map(Holder::value).orElse(null);
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
        Tag countElement = itemTag.get("count");
        if (countElement == null) {
            countElement = itemTag.get("Count");
        }
        if (countElement instanceof NumericTag numberTag) {
            itemCount = numberTag.intValue();
        }
        if (itemCount <= 0) {
            itemCount = 1;
        }
        itemCounts.merge(item, itemCount, Integer::sum);
        itemNames.putIfAbsent(item, new ItemStack(item).getHoverName().getString());
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
    private static void addArmorTrimMaterials(CompoundTag itemTag, Map<Item, Integer> itemCounts,
                                              Map<Item, String> itemNames) {
        String pattern = null;
        String material = null;
        Optional<CompoundTag> componentsOpt = itemTag.getCompound("components");
        if (componentsOpt.isPresent()) {
            Optional<CompoundTag> trimOpt = componentsOpt.get().getCompound("minecraft:trim");
            if (trimOpt.isPresent()) {
                pattern = trimOpt.get().getString("pattern").orElse(null);
                material = trimOpt.get().getString("material").orElse(null);
            }
        }
        if (pattern == null || material == null) {
            Optional<CompoundTag> tagOpt = itemTag.getCompound("tag");
            if (tagOpt.isPresent()) {
                Optional<CompoundTag> trimOpt = tagOpt.get().getCompound("Trim");
                if (trimOpt.isPresent()) {
                    pattern = trimOpt.get().getString("pattern").orElse(null);
                    material = trimOpt.get().getString("material").orElse(null);
                }
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
        Item item = BuiltInRegistries.ITEM.get(Identifier.tryParse(
                patternId.getNamespace() + ":" + patternId.getPath() + "_armor_trim_smithing_template"))
                .map(Holder::value).orElse(null);
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
        Item item = BuiltInRegistries.ITEM.get(id).map(Holder::value).orElse(null);
        return (item == null || item == Items.AIR) ? null : item;
    }

    /**
     * 解析调色板条目（Litematica 原生格式）。
     * 返回 BlockState，如果解析失败则返回 AIR。
     */
    private static BlockState parsePaletteEntry(CompoundTag stateTag, int index) {
        Optional<String> nameOpt = stateTag.getString("Name");
        if (nameOpt.isEmpty()) {
            LitematListMod.LOGGER.warn("调色板条目 #{} 缺少 Name 字段", index);
            return Blocks.AIR.defaultBlockState();
        }

        String blockName = nameOpt.get();
        // 部分工具会把完整状态串写进 Name（如 "minecraft:cake[bites=3]"），
        // 需剥离方括号内的属性，否则 Identifier.tryParse 失败导致方块被丢弃（如被吃掉一部分的蛋糕）
        String inlineProps = null;
        int bracketIdx = blockName.indexOf('[');
        if (bracketIdx > 0 && blockName.endsWith("]")) {
            inlineProps = blockName.substring(bracketIdx + 1, blockName.length() - 1);
            blockName = blockName.substring(0, bracketIdx);
        }
        Identifier blockId = Identifier.tryParse(blockName);
        if (blockId == null) {
            LitematListMod.LOGGER.warn("调色板条目 #{} 无法解析 Block ID: {}", index, blockName);
            return Blocks.AIR.defaultBlockState();
        }

        // 从方块注册表获取方块
        Block block = BuiltInRegistries.BLOCK.get(blockId).get().value();

        // 如果注册表返回 AIR 但方块名不是 air，尝试从物品注册表查找
        if (block == Blocks.AIR && !"minecraft:air".equals(blockName)) {
            // 尝试从 ITEM 注册表查找对应物品，再反查方块
            Item item = BuiltInRegistries.ITEM.get(blockId).get().value();
            if (item != null && item != Items.AIR) {
                Block itemBlock = Block.byItem(item);
                if (itemBlock != Blocks.AIR) {
                    LitematListMod.LOGGER.info("调色板条目 #{} 通过物品注册表找到方块: {} -> {}", index, blockName, itemBlock);
                    block = itemBlock;
                }
            }
            if (block == Blocks.AIR) {
                LitematListMod.LOGGER.warn("调色板条目 #{} 方块未在注册表中找到: {} (可能是新版本方块或模组方块)", index, blockName);
            }
        }

        BlockState state = block.defaultBlockState();

        // 解析 Properties
        Optional<CompoundTag> propsOpt = stateTag.getCompound("Properties");
        if (propsOpt.isPresent()) {
            CompoundTag props = propsOpt.get();
            for (String key : props.keySet()) {
                Optional<String> valOpt = props.getString(key);
                if (valOpt.isEmpty()) continue;
                String value = valOpt.get();
                boolean propertyFound = false;
                for (net.minecraft.world.level.block.state.properties.Property<?> property : state.getProperties()) {
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
                for (net.minecraft.world.level.block.state.properties.Property<?> property : state.getProperties()) {
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
            BlockState state, net.minecraft.world.level.block.state.properties.Property<T> property, String value) {
        try {
            Optional<T> parsed = property.getValue(value);
            if (parsed.isPresent()) {
                return state.setValue(property, parsed.get());
            }
            LitematListMod.LOGGER.debug("属性值解析失败: {}={} (无效值)", property.getName(), value);
            return state;
        } catch (Exception e) {
            LitematListMod.LOGGER.warn("应用方块属性失败: {}={}, 使用默认值", property.getName(), value);
            return state;
        }
    }
}