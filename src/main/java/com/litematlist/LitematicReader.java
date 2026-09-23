package com.litematlist;

import fi.dy.masa.malilib.util.nbt.NbtUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.nio.file.Path;
import java.util.*;

public class LitematicReader {

    public record MaterialEntry(Item item, String blockName, int totalCount, int missingCount) {
        public MaterialEntry(Item item, String blockName, int totalCount) {
            this(item, blockName, totalCount, 0);
        }
        public ItemStack toItemStack() {
            return new ItemStack(item, totalCount);
        }
    }

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
            NbtCompound rootTag = NbtUtils.readNbtFromFileAsPath(filePath);
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

    /** 清空解析缓存，供「是否计算容器数据」等全局解析设置变化时强制重新解析。 */
    public static void clearParseCache() {
        PARSE_CACHE.clear();
    }

    /** 解析结果缓存条目（按文件修改时间 + 层级过滤签名失效） */
    private record CachedResult(long lastModified, String filterSig, List<MaterialEntry> materials) {}

    /** .litematic 解析结果缓存 */
    private static final Map<Path, CachedResult> PARSE_CACHE = new HashMap<>();

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

        // 炼药锅：只要含液体（哪怕不满）即记一桶
        if (block == Blocks.WATER_CAULDRON) {
            addSpecialItem(Items.WATER_BUCKET, itemCounts, itemNames);
            return;
        }
        if (block == Blocks.LAVA_CAULDRON) {
            addSpecialItem(Items.LAVA_BUCKET, itemCounts, itemNames);
            return;
        }
        if (block == Blocks.POWDER_SNOW_CAULDRON) {
            addSpecialItem(Items.POWDER_SNOW_BUCKET, itemCounts, itemNames);
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
        }
    }

    private static List<MaterialEntry> parseBlockStates(NbtCompound rootTag, Path filePath, LitematicaBridge.LayerFilter filter) {
        Map<Item, Integer> itemCounts = new LinkedHashMap<>();
        Map<Item, String> itemNames = new LinkedHashMap<>();
        int regionCount = 0;
        int failedRegions = 0;

        if (!rootTag.contains("Version")) {
            LitematListMod.LOGGER.warn("缺少 Version 字段，不是有效的 Litematica 原理图");
            return Collections.emptyList();
        }
        int version = rootTag.getInt("Version");
        LitematListMod.LOGGER.info("原理图版本: {}", version);

        if (rootTag.contains("Regions", NbtElement.COMPOUND_TYPE)) {
            NbtCompound regions = rootTag.getCompound("Regions");
            LitematListMod.LOGGER.info("找到 Regions，共 {} 个子区域", regions.getKeys().size());

            for (String regionName : regions.getKeys()) {
                if (regions.contains(regionName, NbtElement.COMPOUND_TYPE)) {
                    try {
                        parseRegion(regions.getCompound(regionName), itemCounts, itemNames, regionName, version, filter);
                        regionCount++;
                    } catch (Exception e) {
                        failedRegions++;
                        LitematListMod.LOGGER.error("解析 Region '{}' 失败，跳过该区域: {}", regionName, e.getMessage());
                        LitematListMod.LOGGER.debug("Region '{}' 异常详情:", regionName, e);
                    }
                }
            }
        }

        if (regionCount == 0 && rootTag.contains("Blocks")) {
            LitematListMod.LOGGER.info("尝试解析 Sponge 格式...");
            try {
                parseSpongeFormat(rootTag, itemCounts, itemNames, filter);
                regionCount++;
            } catch (Exception e) {
                LitematListMod.LOGGER.error("Sponge 格式解析失败: {}", e.getMessage());
            }
        }

        LitematListMod.LOGGER.info("解析了 {} 个 Region，失败 {} 个，共 {} 种方块",
                regionCount, failedRegions, itemCounts.size());

        List<MaterialEntry> result = new ArrayList<>();
        for (Map.Entry<Item, Integer> entry : itemCounts.entrySet()) {
            Item item = entry.getKey();
            result.add(new MaterialEntry(item, itemNames.getOrDefault(item, item.getName().getString()), entry.getValue()));
        }
        result.sort((a, b) -> Integer.compare(b.totalCount(), a.totalCount()));
        return result;
    }

    private static void parseSpongeFormat(NbtCompound rootTag, Map<Item, Integer> itemCounts,
                                          Map<Item, String> itemNames, LitematicaBridge.LayerFilter filter) {
        short width = rootTag.contains("Width", NbtElement.SHORT_TYPE) ? (short) Math.abs(rootTag.getShort("Width")) : 0;
        short height = rootTag.contains("Height", NbtElement.SHORT_TYPE) ? (short) Math.abs(rootTag.getShort("Height")) : 0;
        short length = rootTag.contains("Length", NbtElement.SHORT_TYPE) ? (short) Math.abs(rootTag.getShort("Length")) : 0;
        int totalBlocks = width * height * length;
        LitematListMod.LOGGER.info("Sponge 格式尺寸: {}x{}x{} ({} 方块)", width, height, length, totalBlocks);

        NbtCompound palette;
        if (rootTag.contains("Palette", NbtElement.COMPOUND_TYPE)) {
            palette = rootTag.getCompound("Palette");
        } else {
            LitematListMod.LOGGER.warn("Sponge 格式缺少 Palette");
            return;
        }
        int paletteSize = palette.getKeys().size();
        LitematListMod.LOGGER.info("Sponge 调色板大小: {}", paletteSize);

        List<BlockState> paletteStates = new ArrayList<>(paletteSize);
        for (int i = 0; i < paletteSize; i++) {
            paletteStates.add(Blocks.AIR.getDefaultState());
        }
        for (String key : palette.getKeys()) {
            int index = palette.getInt(key);
            if (index >= 0 && index < paletteSize) {
                try {
                    BlockState state = parseSpongePaletteEntry(key, index);
                    paletteStates.set(index, state);
                } catch (Exception e) {
                    LitematListMod.LOGGER.warn("Sponge 调色板条目 '{}' 解析失败: {}", key, e.getMessage());
                }
            }
        }

        byte[] blockData;
        if (rootTag.contains("BlockData", NbtElement.BYTE_ARRAY_TYPE)) {
            blockData = rootTag.getByteArray("BlockData");
        } else {
            LitematListMod.LOGGER.warn("Sponge 格式缺少 BlockData");
            return;
        }
        LitematListMod.LOGGER.info("Sponge BlockData 大小: {} 字节", blockData.length);

        int bitsPerBlock = Math.max(2, 32 - Integer.numberOfLeadingZeros(paletteSize - 1));
        int[] decoded = decodeVarIntArray(blockData);
        // 「是否计算建筑材料」关闭时，方块（建筑材料）不计入材料列表
        boolean calcBuildingMaterials =
                com.litematlist.config.Configs.FeatureToggles.CALC_BUILDING_MATERIALS.getBooleanValue();

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
                if (!state.isAir() && calcBuildingMaterials) {
                    Item item = state.getBlock().asItem();
                    if (item != Items.AIR) {
                        itemCounts.merge(item, 1, Integer::sum);
                        itemNames.putIfAbsent(item, state.getBlock().getName().getString());
                    }
                    // 流体源头/含水方块/炼药锅液体（均属建筑材料）额外统计
                    countFluidSources(state, itemCounts, itemNames);
                }
            }
            decodedBlocks++;
        }
        LitematListMod.LOGGER.info("Sponge 格式解码了 {} 个方块", decodedBlocks);

        // Sponge 格式的容器数据（v1: TileEntities；v2+: BlockEntities）
        if (com.litematlist.config.Configs.FeatureToggles.CALCULATE_CONTAINER_DATA.getBooleanValue()) {
            parseTileEntityList(rootTag, "BlockEntities", itemCounts, itemNames, "Sponge", 0, 0, 0, filter);
            parseTileEntityList(rootTag, "TileEntities", itemCounts, itemNames, "Sponge", 0, 0, 0, filter);
        }
    }

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

    private static BlockState parseSpongePaletteEntry(String stateString, int index) {
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

        Block block = Registries.BLOCK.get(blockId);
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

    private static void parseRegion(NbtCompound region, Map<Item, Integer> itemCounts,
                                     Map<Item, String> itemNames, String regionName, int version,
                                     LitematicaBridge.LayerFilter filter) {
        // 0. 读取区域原点（Position），用于渲染层过滤时换算相对层级（缺失时视为 0）
        int posX = 0, posY = 0, posZ = 0;
        if (region.contains("Position", NbtElement.COMPOUND_TYPE)) {
            NbtCompound posTag = region.getCompound("Position");
            posX = posTag.getInt("x");
            posY = posTag.getInt("y");
            posZ = posTag.getInt("z");
        }
        NbtCompound sizeTag;
        if (region.contains("Size", NbtElement.COMPOUND_TYPE)) {
            sizeTag = region.getCompound("Size");
        } else {
            LitematListMod.LOGGER.warn("Region '{}' 缺少 Size 字段", regionName);
            return;
        }
        // 与投影 PositionUtils.getMinCorner 一致：Size 为负的轴，Position 保存的是该轴的
        // 「最大角」而非最小角，最小角 = Position + Size + 1（原理图可向负方向延伸）
        int rawSx = sizeTag.getInt("x");
        int rawSy = sizeTag.getInt("y");
        int rawSz = sizeTag.getInt("z");
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

        NbtList palette;
        if (region.contains("BlockStatePalette", NbtElement.LIST_TYPE)) {
            palette = region.getList("BlockStatePalette", NbtElement.COMPOUND_TYPE);
        } else {
            LitematListMod.LOGGER.warn("Region '{}' 缺少 BlockStatePalette", regionName);
            return;
        }
        LitematListMod.LOGGER.info("Region '{}' 调色板大小: {}", regionName, palette.size());

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
            int[] intArray = ((NbtIntArray) blockStatesElement).getIntArray();
            blockStates = new long[intArray.length];
            for (int i = 0; i < intArray.length; i++) {
                blockStates[i] = intArray[i] & 0xFFFFFFFFL;
            }
            LitematListMod.LOGGER.info("Region '{}' BlockStates 类型: NbtIntArray ({} 个 int)", regionName, blockStates.length);
        } else if (blockStatesType == NbtElement.BYTE_ARRAY_TYPE) {
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

        int paletteSize = paletteStates.size();
        int bitsPerBlock = Math.max(2, 32 - Integer.numberOfLeadingZeros(paletteSize - 1));
        long mask = (1L << bitsPerBlock) - 1;
        // 「是否计算建筑材料」关闭时，方块（建筑材料）不计入材料列表
        boolean calcBuildingMaterials =
                com.litematlist.config.Configs.FeatureToggles.CALC_BUILDING_MATERIALS.getBooleanValue();

        int decodedBlocks = 0;
        int airCount = 0;

        for (int i = 0; i < totalBlocks; i++) {
            int bitIndex = i * bitsPerBlock;
            int longIndex = bitIndex / 64;
            int bitOffset = bitIndex % 64;
            int paletteIndex = (int) ((blockStates[longIndex] >>> bitOffset) & mask);
            if (bitOffset + bitsPerBlock > 64) {
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
                    if (calcBuildingMaterials) {
                        Item item = state.getBlock().asItem();
                        if (item != Items.AIR) {
                            itemCounts.merge(item, 1, Integer::sum);
                            itemNames.putIfAbsent(item, state.getBlock().getName().getString());
                        }
                        // 流体源头/含水方块/炼药锅液体（均属建筑材料）额外统计
                        countFluidSources(state, itemCounts, itemNames);
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
        if (com.litematlist.config.Configs.FeatureToggles.CALCULATE_CONTAINER_DATA.getBooleanValue()) {
            parseTileEntityList(region, "TileEntities", itemCounts, itemNames, regionName, posX, posY, posZ, filter);
        }

        // 6. 读取实体（「是否计算原理图实体」开启时，统计矿车/盔甲架/船/箱船等实体对应的物品）
        if (com.litematlist.config.Configs.FeatureToggles.CALC_SCHEMATIC_ENTITIES.getBooleanValue()) {
            parseEntityList(region, itemCounts, itemNames, regionName, posX, posY, posZ, filter);
        }
    }

    /**
     * 解析原理图中的容器方块实体列表，将容器内物品并入材料统计。
     * 兼容 Litematica 原生格式（TileEntities，方块实体数据内联）
     * 与 Sponge 格式（v1: TileEntities + TileNBT 嵌套；v2+: BlockEntities）。
     */
    private static void parseTileEntityList(NbtCompound containerTag, String listKey,
                                            Map<Item, Integer> itemCounts,
                                            Map<Item, String> itemNames, String regionName,
                                            int regionPosX, int regionPosY, int regionPosZ,
                                            LitematicaBridge.LayerFilter filter) {
        if (containerTag.contains(listKey, NbtElement.LIST_TYPE) == false) {
            return;
        }
        NbtList teList = containerTag.getList(listKey, NbtElement.COMPOUND_TYPE);
        int containerItemCount = 0;

        // 预扫描：收集箱类方块实体坐标（用于相邻配对判定大型箱子）。
        // 配对是结构属性，与渲染层过滤无关，因此预扫描不做层过滤。
        Set<String> chestCoords = new HashSet<>();
        Set<String> trappedChestCoords = new HashSet<>();
        for (int i = 0; i < teList.size(); i++) {
            NbtElement element = teList.get(i);
            if (!(element instanceof NbtCompound teTag)) {
                continue;
            }
            NbtCompound tileNbt = teTag.contains("TileNBT", NbtElement.COMPOUND_TYPE)
                    ? teTag.getCompound("TileNBT") : teTag;
            String teId = tileNbt.getString("id");
            if (teId == null || teId.isEmpty()
                    || !teTag.contains("x") || !teTag.contains("y") || !teTag.contains("z")) {
                continue;
            }
            String coord = teTag.getInt("x") + "," + teTag.getInt("y") + "," + teTag.getInt("z");
            if ("minecraft:chest".equals(teId)) {
                chestCoords.add(coord);
            } else if ("minecraft:trapped_chest".equals(teId)) {
                trappedChestCoords.add(coord);
            }
        }

        for (int i = 0; i < teList.size(); i++) {
            NbtElement element = teList.get(i);
            if (!(element instanceof NbtCompound teTag)) {
                continue;
            }
            // 渲染层过滤：方块实体的 x/y/z 为区域内相对坐标（缺失时跳过过滤，保留原行为）
            if (filter != null && teTag.contains("x") && teTag.contains("y") && teTag.contains("z")) {
                int teX = teTag.getInt("x");
                int teY = teTag.getInt("y");
                int teZ = teTag.getInt("z");
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
            NbtCompound tileNbt = teTag.contains("TileNBT", NbtElement.COMPOUND_TYPE)
                    ? teTag.getCompound("TileNBT") : teTag;

            String teId = tileNbt.getString("id");
            if (teId == null || teId.isEmpty()) {
                // 无 id 的方块实体（少数模组容器）：维持旧行为直接解析 Items
                containerItemCount += parseContainerItems(tileNbt, itemCounts, itemNames);
                continue;
            }
            int teX = teTag.contains("x") ? teTag.getInt("x") : 0;
            int teY = teTag.contains("y") ? teTag.getInt("y") : 0;
            int teZ = teTag.contains("z") ? teTag.getInt("z") : 0;
            String typeKey = classifyContainerTe(teId, teX, teY, teZ, chestCoords, trappedChestCoords);

            // 「统计」页类型开关：已知类型未开启则跳过；未知类型不受影响（维持旧行为）
            if (typeKey != null && com.litematlist.config.Configs.Statistics.enabled(typeKey) == false) {
                continue;
            }

            switch (typeKey == null ? "" : typeKey) {
                case "lectern" -> {
                    // 讲台：只要有书就按「书与笔」计 1 本
                    if (tileNbt.contains("Book")) {
                        addSpecialItem(Items.WRITABLE_BOOK, itemCounts, itemNames);
                        containerItemCount++;
                    }
                }
                case "chiseledBookshelf" ->
                        containerItemCount += countChiseledBookshelfBooks(tileNbt, itemCounts, itemNames);
                case "decoratedPot" ->
                        containerItemCount += countDecoratedPotItem(tileNbt, itemCounts, itemNames);
                default -> containerItemCount += parseContainerItems(tileNbt, itemCounts, itemNames);
            }
        }

        if (containerItemCount > 0) {
            LitematListMod.LOGGER.info("Region '{}' 容器数据({})解析: 计入了 {} 个物品",
                    regionName, listKey, containerItemCount);
        }
    }

    /**
     * 箱子类方块实体分类：与水平相邻（±1 于 x/z、同 y）的同类箱配对时判定为大型箱，
     * 否则为普通箱；其余已知容器/特殊方块实体返回对应「统计」页类型 key；未知返回 null。
     */
    private static String classifyContainerTe(String teId, int x, int y, int z,
                                              Set<String> chestCoords,
                                              Set<String> trappedChestCoords) {
        if ("minecraft:chest".equals(teId)) {
            return hasAdjacentSameType(chestCoords, x, y, z) ? "largeChest" : "chest";
        }
        if ("minecraft:trapped_chest".equals(teId)) {
            return hasAdjacentSameType(trappedChestCoords, x, y, z) ? "largeTrappedChest" : "trappedChest";
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
     * 每个占用槽（有合法物品 id 且非空气）计 1 本 minecraft:book。
     */
    private static int countChiseledBookshelfBooks(NbtCompound tileNbt, Map<Item, Integer> itemCounts,
                                                   Map<Item, String> itemNames) {
        if (tileNbt.contains("Items", NbtElement.LIST_TYPE) == false) {
            return 0;
        }
        NbtList items = tileNbt.getList("Items", NbtElement.COMPOUND_TYPE);
        int books = 0;
        for (int i = 0; i < items.size(); i++) {
            NbtElement element = items.get(i);
            if (!(element instanceof NbtCompound itemTag)) {
                continue;
            }
            String id = itemTag.getString("id");
            if (id == null || id.isEmpty()) {
                continue;
            }
            Identifier itemId = Identifier.tryParse(id);
            if (itemId == null || Registries.ITEM.get(itemId) == Items.AIR) {
                continue;
            }
            books++;
        }
        if (books > 0) {
            addSpecialItem(Items.BOOK, itemCounts, itemNames, books);
        }
        return books;
    }

    /**
     * 陶罐：单格存储物品，读 "item" 键（新格式为 {id, count/Count} 复合标签，
     * 旧格式为物品 id 字符串），计入材料统计。
     */
    private static int countDecoratedPotItem(NbtCompound tileNbt, Map<Item, Integer> itemCounts,
                                             Map<Item, String> itemNames) {
        if (tileNbt.contains("item") == false) {
            return 0;
        }
        NbtElement itemElement = tileNbt.get("item");
        if (itemElement instanceof NbtCompound itemTag) {
            String id = itemTag.getString("id");
            if (id == null || id.isEmpty()) {
                return 0;
            }
            Identifier itemId = Identifier.tryParse(id);
            if (itemId == null) {
                return 0;
            }
            Item item = Registries.ITEM.get(itemId);
            if (item == null || item == Items.AIR) {
                return 0;
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
            return 1;
        } else if (itemElement instanceof NbtString idTag) {
            String idValue = idTag.asString();
            if (idValue == null || idValue.isEmpty()) {
                return 0;
            }
            Identifier itemId = Identifier.tryParse(idValue);
            if (itemId == null) {
                return 0;
            }
            Item item = Registries.ITEM.get(itemId);
            if (item == null || item == Items.AIR) {
                return 0;
            }
            addSpecialItem(item, itemCounts, itemNames);
            return 1;
        }
        return 0;
    }

    /** 从单个方块实体 NBT 中读取 Items 列表并计入材料统计，返回计入的物品数。 */
    private static int parseContainerItems(NbtCompound tileNbt, Map<Item, Integer> itemCounts,
                                           Map<Item, String> itemNames) {
        if (tileNbt.contains("Items", NbtElement.LIST_TYPE) == false) {
            return 0;
        }
        NbtList items = tileNbt.getList("Items", NbtElement.COMPOUND_TYPE);
        return parseItemList(items, itemCounts, itemNames);
    }

    /**
     * 解析物品列表（条目为 {id, count/Count}，如容器 Items），计入材料统计，返回计入的物品数。
     */
    private static int parseItemList(NbtList items, Map<Item, Integer> itemCounts,
                                     Map<Item, String> itemNames) {
        int count = 0;

        for (int i = 0; i < items.size(); i++) {
            NbtElement element = items.get(i);
            if (!(element instanceof NbtCompound itemTag)) {
                continue;
            }
            String id = itemTag.getString("id");
            if (id == null || id.isEmpty()) {
                continue;
            }
            Identifier itemId = Identifier.tryParse(id);
            if (itemId == null) {
                continue;
            }
            Item item = Registries.ITEM.get(itemId);
            if (item == null || item == Items.AIR) {
                continue;
            }
            // 兼容两种键名与数值类型：1.20.5+ 格式用 "count"，旧格式用 "Count"
            int itemCount = 1;
            net.minecraft.nbt.NbtElement countEl = itemTag.get("count");
            if (countEl == null) countEl = itemTag.get("Count");
            if (countEl instanceof net.minecraft.nbt.AbstractNbtNumber num) itemCount = num.intValue();
            if (itemCount <= 0) itemCount = 1;
            itemCounts.merge(item, itemCount, Integer::sum);
            itemNames.putIfAbsent(item, item.getName().getString());
            count++;
        }

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
     *    （oak/spruce/.../bamboo/pale_oak），映射为 &lt;type&gt;_boat / &lt;type&gt;_chest_boat
     *    （bamboo 为 bamboo_raft / bamboo_chest_raft）
     * 容器实体（漏斗矿车、箱船、驴背箱子等）在「是否计算原理图实体容器数据」
     * 开启时统计其容器数据；盔甲架穿戴/手持的物品跟随本开关一并统计。
     */
    private static void parseEntityList(NbtCompound region, Map<Item, Integer> itemCounts,
                                        Map<Item, String> itemNames, String regionName,
                                        int regionPosX, int regionPosY, int regionPosZ,
                                        LitematicaBridge.LayerFilter filter) {
        if (region.contains("Entities", NbtElement.LIST_TYPE) == false) {
            return;
        }
        NbtList entities = region.getList("Entities", NbtElement.COMPOUND_TYPE);
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
            if (filter != null && entityTag.contains("Pos", NbtElement.LIST_TYPE)) {
                NbtList pos = entityTag.getList("Pos", NbtElement.DOUBLE_TYPE);
                if (pos.size() >= 3) {
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
            String entityId = entityTag.getString("id");
            if (entityId == null || entityId.isEmpty()) {
                continue;
            }

            // 实体本身对应的物品（矿车/盔甲架/船/箱船等）
            Item entityItem = resolveEntityItem(entityId, entityTag);
            if (entityItem != null && entityItem != Items.AIR) {
                itemCounts.merge(entityItem, 1, Integer::sum);
                itemNames.putIfAbsent(entityItem, entityItem.getName().getString());
                entityCount++;
            }

            // 盔甲架：装备栏（新版 equipment 复合标签 / 旧版 ArmorItems+HandItems 列表）
            // 跟随「是否计算原理图实体」一并统计（盔甲物品 + 纹饰模板 + 镶嵌材质），
            // 受「统计」页「盔甲架上的盔甲」开关门控（默认开启）
            if ("minecraft:armor_stand".equals(entityId)) {
                if (com.litematlist.config.Configs.Statistics.enabled("armorStandArmor")) {
                    containerItemCount += parseArmorStandItems(entityTag, itemCounts, itemNames);
                }
            }

            // 雪傀儡/铁傀儡/凋零等实体的召唤材料（按实体数量 × 配方材料）
            countSummonMaterials(entityId, entityTag, itemCounts, itemNames);

            // 容器实体：统计容器数据
            if (calcEntityContainers) {
                containerItemCount += parseEntityContainer(entityId, entityTag, itemCounts, itemNames);
            }
        }

        if (entityCount > 0 || containerItemCount > 0) {
            LitematListMod.LOGGER.info("Region '{}' 实体解析: 计入了 {} 个实体、{} 个实体容器物品",
                    regionName, entityCount, containerItemCount);
        }
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
            Item direct = Registries.ITEM.get(directId);
            if (direct != null && direct != Items.AIR) {
                return direct;
            }
        }
        // 旧格式通用船实体：按 "Type" 字段映射为具体木种的船物品
        if ("minecraft:boat".equals(entityId) || "minecraft:chest_boat".equals(entityId)) {
            String type = entityTag.getString("Type");
            if (type != null && type.isEmpty() == false) {
                boolean chest = entityId.endsWith("chest_boat");
                String itemName;
                if ("bamboo".equals(type)) {
                    itemName = chest ? "bamboo_chest_raft" : "bamboo_raft";
                } else {
                    itemName = chest ? type + "_chest_boat" : type + "_boat";
                }
                Item typed = Registries.ITEM.get(Identifier.tryParse("minecraft:" + itemName));
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
    }

    /** 按 minecraft 命名空间从物品注册表取物品（不存在时为 AIR，由调用方忽略）。 */
    private static Item blockItem(String name) {
        return Registries.ITEM.get(Identifier.tryParse("minecraft:" + name));
    }

    /**
     * 统计容器实体中的容器数据（开关「是否计算原理图实体容器数据」开启时调用）。
     * 覆盖：漏斗矿车/箱子矿车（Items）、箱船/箱筏（Items）、有箱子的驴/羊驼（Items）。
     * （盔甲架穿戴/手持的物品不在本方法处理——它们跟随「是否计算原理图实体」开关，
     * 由 parseEntityList 直接统计。）
     * 已知容器实体类型受「统计」页对应类型开关门控。返回计入的物品数。
     */
    private static int parseEntityContainer(String entityId, NbtCompound entityTag,
                                            Map<Item, Integer> itemCounts,
                                            Map<Item, String> itemNames) {
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
            return parseContainerItems(entityTag, itemCounts, itemNames);
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
            return entityTag.getBoolean("ChestedHorse");
        }
        if (entityTag.contains("chestedHorse")) {
            return entityTag.getBoolean("chestedHorse");
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
    private static int parseArmorStandItems(NbtCompound entityTag, Map<Item, Integer> itemCounts,
                                             Map<Item, String> itemNames) {
        int count = 0;
        if (entityTag.contains("equipment", NbtElement.COMPOUND_TYPE)) {
            NbtCompound equipment = entityTag.getCompound("equipment");
            for (String slotKey : equipment.getKeys()) {
                NbtElement slotElement = equipment.get(slotKey);
                if (slotElement instanceof NbtCompound slotTag
                        && parseArmorItem(slotTag, itemCounts, itemNames)) {
                    count++;
                }
            }
        } else {
            if (entityTag.contains("ArmorItems", NbtElement.LIST_TYPE)) {
                count += parseArmorItemList(entityTag.getList("ArmorItems", NbtElement.COMPOUND_TYPE), itemCounts, itemNames);
            }
            if (entityTag.contains("HandItems", NbtElement.LIST_TYPE)) {
                count += parseArmorItemList(entityTag.getList("HandItems", NbtElement.COMPOUND_TYPE), itemCounts, itemNames);
            }
        }
        return count;
    }

    /**
     * 解析盔甲架单个物品栏列表（旧格式 ArmorItems/HandItems）：逐项统计物品本身，
     * 并额外展开其纹饰（模板 + 材质）。
     */
    private static int parseArmorItemList(NbtList items, Map<Item, Integer> itemCounts,
                                          Map<Item, String> itemNames) {
        int count = 0;
        for (int i = 0; i < items.size(); i++) {
            NbtElement element = items.get(i);
            if (element instanceof NbtCompound itemTag
                    && parseArmorItem(itemTag, itemCounts, itemNames)) {
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
                                          Map<Item, String> itemNames) {
        String id = itemTag.getString("id");
        if (id == null || id.isEmpty()) {
            return false;
        }
        Identifier itemId = Identifier.tryParse(id);
        if (itemId == null) {
            return false;
        }
        Item item = Registries.ITEM.get(itemId);
        if (item == null || item == Items.AIR) {
            return false;
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
        if (itemTag.contains("components", NbtElement.COMPOUND_TYPE)) {
            NbtCompound components = itemTag.getCompound("components");
            if (components.contains("minecraft:trim", NbtElement.COMPOUND_TYPE)) {
                NbtCompound trim = components.getCompound("minecraft:trim");
                pattern = trim.getString("pattern");
                material = trim.getString("material");
            }
        }
        if ((pattern == null || material == null) && itemTag.contains("tag", NbtElement.COMPOUND_TYPE)) {
            NbtCompound tag = itemTag.getCompound("tag");
            if (tag.contains("Trim", NbtElement.COMPOUND_TYPE)) {
                NbtCompound trim = tag.getCompound("Trim");
                pattern = trim.getString("pattern");
                material = trim.getString("material");
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

    /** 纹饰图案 key（如 minecraft:sentry）→ 对应纹饰锻造模板物品。 */
    private static Item trimTemplateItem(String patternKey) {
        Identifier patternId = Identifier.tryParse(patternKey);
        if (patternId == null) {
            return null;
        }
        Item item = Registries.ITEM.get(Identifier.tryParse(
                patternId.getNamespace() + ":" + patternId.getPath() + "_armor_trim_smithing_template"));
        return (item == null || item == Items.AIR) ? null : item;
    }

    /** 纹饰材质 key（如 minecraft:iron）→ 对应镶嵌材质物品（铁锭等）。 */
    private static Item trimMaterialItem(String materialKey) {
        String mapped = switch (materialKey) {
            case "minecraft:iron" -> "minecraft:iron_ingot";
            case "minecraft:gold" -> "minecraft:gold_ingot";
            case "minecraft:copper" -> "minecraft:copper_ingot";
            case "minecraft:netherite" -> "minecraft:netherite_ingot";
            case "minecraft:lapis" -> "minecraft:lapis_lazuli";
            case "minecraft:amethyst" -> "minecraft:amethyst_shard";
            default -> materialKey; // 石英/钻石/绿宝石/红石同名物品，模组材质按原键兜底
        };
        Identifier id = Identifier.tryParse(mapped);
        if (id == null) {
            return null;
        }
        Item item = Registries.ITEM.get(id);
        return (item == null || item == Items.AIR) ? null : item;
    }

    private static BlockState parsePaletteEntry(NbtCompound stateTag, int index) {
        String blockName = stateTag.getString("Name");
        if (blockName == null) {
            LitematListMod.LOGGER.warn("调色板条目 #{} 缺少 Name 字段", index);
            return Blocks.AIR.getDefaultState();
        }

        Identifier blockId = Identifier.tryParse(blockName);
        if (blockId == null) {
            LitematListMod.LOGGER.warn("调色板条目 #{} 无法解析 Block ID: {}", index, blockName);
            return Blocks.AIR.getDefaultState();
        }

        Block block = Registries.BLOCK.get(blockId);

        if (block == Blocks.AIR && !"minecraft:air".equals(blockName)) {
            Item item = Registries.ITEM.get(blockId);
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

        if (stateTag.contains("Properties", NbtElement.COMPOUND_TYPE)) {
            NbtCompound props = stateTag.getCompound("Properties");
            for (String key : props.getKeys()) {
                String value = props.getString(key);
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
