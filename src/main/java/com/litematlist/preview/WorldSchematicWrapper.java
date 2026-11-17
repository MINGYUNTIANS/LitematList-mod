package com.litematlist.preview;
//#if MC == 12108
//$$
//#endif
//#if MC == 12108
//$$import com.litematlist.LitematListMod;
//#endif
//#if MC == 12108
//$$import com.litematlist.LitematicaBridge.LayerFilter;
//#endif

import com.google.common.base.Suppliers;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import com.google.common.collect.ImmutableMap;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$import com.litematlist.LitematicaBridge.LayerFilter;
//#endif
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
//#if MC == 12111
//$$import fi.dy.masa.malilib.util.data.tag.CompoundData;
//#endif
//#if MC == 12111
//$$import fi.dy.masa.malilib.util.data.tag.converter.DataConverterNbt;
//#endif
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.component.type.MapIdComponent;
//#endif
import net.minecraft.entity.Entity;
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.entity.boss.dragon.EnderDragonPart;
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.entity.damage.DamageSource;
//#endif
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.item.FuelRegistry;
//#endif
import net.minecraft.item.map.MapState;
//#if MC == 11701
//$$import net.minecraft.particle.ParticleEffect;
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.nbt.NbtCompound;
//#endif
//#if MC == 12110 || MC == 12111
import net.minecraft.particle.BlockParticleEffect;
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.particle.ParticleEffect;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.recipe.BrewingRecipeRegistry;
//#endif
import net.minecraft.recipe.RecipeManager;
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.registry.Registries;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.registry.RegistryKeys;
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.registry.RegistryWrapper;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.registry.entry.RegistryEntry;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.resource.featuretoggle.FeatureFlags;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.resource.featuretoggle.FeatureSet;
//#endif
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
//#if MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.storage.NbtReadView;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.util.ErrorReporter;
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.util.Identifier;
//#endif
//#if MC == 12110 || MC == 12111
import net.minecraft.util.collection.Pool;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.util.math.*;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.world.*;
//#endif
//#if MC == 11701
//$$import net.minecraft.tag.TagManager;
//#endif
//#if MC == 11701
//$$import net.minecraft.util.math.BlockPos;
//#endif
//#if MC == 11701
//$$import net.minecraft.util.math.ChunkPos;
//#endif
//#if MC == 11701
//$$import net.minecraft.util.math.ChunkSectionPos;
//#endif
//#if MC == 11701
//$$import net.minecraft.util.math.Direction;
//#endif
//#if MC == 11701
//$$import net.minecraft.util.math.Vec3d;
//#endif
//#if MC == 11701
//$$import net.minecraft.util.math.Vec3i;
//#endif
//#if MC == 11701
//$$import net.minecraft.util.profiler.Profiler;
//#endif
//#if MC == 11701
//$$import net.minecraft.util.registry.DynamicRegistryManager;
//#endif
//#if MC == 11701
//$$import net.minecraft.util.registry.RegistryKey;
//#endif
//#if MC == 11701
//$$import net.minecraft.world.BlockView;
//#endif
//#if MC == 11701
//$$import net.minecraft.world.Difficulty;
//#endif
//#if MC == 11701
//$$import net.minecraft.world.LightType;
//#endif
//#if MC == 11701
//$$import net.minecraft.world.LocalDifficulty;
//#endif
//#if MC == 11701
//$$import net.minecraft.world.TickScheduler;
//#endif
//#if MC == 11701
//$$import net.minecraft.world.World;
//#endif
//#if MC == 11701
//$$import net.minecraft.world.WorldProperties;
//#endif
import net.minecraft.world.biome.Biome;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.world.biome.BiomeKeys;
//#elseif MC == 11701
//$$import net.minecraft.world.biome.source.BiomeAccess;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.world.biome.ColorResolver;
//#elseif MC == 11701
//$$import net.minecraft.world.chunk.Chunk;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.world.border.WorldBorder;
//#endif
//#if MC == 12111
//$$import net.minecraft.world.attribute.WorldEnvironmentAttributeAccess;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.world.chunk.*;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$import net.minecraft.world.chunk.ChunkManager;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$import net.minecraft.world.chunk.ChunkNibbleArray;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$import net.minecraft.world.chunk.ChunkProvider;
//#endif
//#if MC == 11701
//$$import net.minecraft.world.chunk.ChunkStatus;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$import net.minecraft.world.chunk.WorldChunk;
//#endif
//#if MC == 12111
//$$import net.minecraft.world.chunk.Chunk.TickSchedulers;
//#endif
import net.minecraft.world.chunk.light.ChunkLightingView;
import net.minecraft.world.chunk.light.LightingProvider;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.world.dimension.DimensionTypes;
//#elseif MC == 11701
//$$import net.minecraft.world.dimension.DimensionType;
//#endif
import net.minecraft.world.entity.EntityLookup;
import net.minecraft.world.event.GameEvent;
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.world.explosion.ExplosionBehavior;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.world.tick.BasicTickScheduler;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.world.tick.EmptyTickSchedulers;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.world.tick.QueryableTickScheduler;
//#elseif MC == 11701
//$$import net.minecraft.world.level.ColorResolver;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.world.tick.TickManager;
//#endif
//#if MC == 12110 || MC == 12111
import com.litematlist.LitematicaBridge.LayerFilter;
//#endif
//#if MC == 12110 || MC == 12111
import com.litematlist.LitematListMod;
//#endif
import org.jetbrains.annotations.Nullable;

//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import java.util.Collection;
//#endif
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//#if MC == 12108 || MC == 12110 || MC == 12111
import java.util.function.BooleanSupplier;
//#endif
import java.util.function.Supplier;

//#if MC == 11701
//$$/**
//$$ * 只读的原理图世界包装器（搬自 SchematicPreview，适配 1.17.1 的 World 构造与抽象方法）。
//$$ */
//#endif
public class WorldSchematicWrapper extends World implements ChunkProvider {

    private final LightingProvider lightingProvider = new FakeLightingProvider(this);
//#if MC == 12108 || MC == 12110 || MC == 12111
    private final FakeChunkManager fakeChunkManager = new FakeChunkManager();
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
    private final WorldBorder worldBorder = new WorldBorder();
//#endif
//#if MC == 12110 || MC == 12111
    private final PalettesFactory palettesFactory;
//#endif
    private final Map<String, BlockPos> areaPoses = new HashMap<>();
    private final Biome biome;
    private LitematicaSchematic schematic;
    private Vec3i areasOrigin = new Vec3i(0, 0, 0);
    private Vec3i size = new Vec3i(0, 0, 0);
    private BlockState[] blocksData;
    private Map<BlockPos, Supplier<BlockEntity>> blockEntities;
    private LayerFilter layerFilter;

    public WorldSchematicWrapper(MinecraftClient mc) {
        super(
                new ClientWorld.Properties(Difficulty.PEACEFUL, false, true),
                World.OVERWORLD,
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                mc.world.getRegistryManager(),
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                mc.world.getRegistryManager().getOrThrow(RegistryKeys.DIMENSION_TYPE).getOrThrow(DimensionTypes.OVERWORLD),
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101
//$$                mc.world.getRegistryManager().get(RegistryKeys.DIMENSION_TYPE).entryOf(DimensionTypes.OVERWORLD),
//#endif
//#if MC == 11701
//$$                mc.world.getDimension(),
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$                mc::getProfiler,
//#endif
                true,
                false,
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                0L,
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                0
//#endif
//#if MC == 11701
//$$                0L
//#endif
        );
//#if MC == 12110 || MC == 12111
        palettesFactory = mc.world.getPalettesFactory();
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        biome = getRegistryManager().getOrThrow(RegistryKeys.BIOME).get(BiomeKeys.PLAINS);
//#elseif MC == 11701
//$$        biome = mc.world.getBiomeAccess().getBiome(new BlockPos(0, 0, 0));
//#elseif MC == 11904 || MC == 12001 || MC == 12101
//$$        biome = getRegistryManager().get(RegistryKeys.BIOME).get(BiomeKeys.PLAINS);
//#endif
    }

    public void setSchematic(LitematicaSchematic schematic, @Nullable LayerFilter filter) {
        this.schematic = schematic;
        this.layerFilter = filter;
        areaPoses.clear();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        schematic.getAreas().forEach((region, box) -> areaPoses.put(region, BlockPos.min(box.getPos1(), box.getPos2())));
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        schematic.getAreas().forEach((region, box) -> {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            BlockPos pos1 = box.getPos1();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            BlockPos pos2 = box.getPos2();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            areaPoses.put(region, new BlockPos(
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    Math.min(pos1.getX(), pos2.getX()),
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    Math.min(pos1.getY(), pos2.getY()),
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    Math.min(pos1.getZ(), pos2.getZ())
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            ));
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        });
//#endif
        BlockPos.Mutable areasOrigin = new BlockPos.Mutable(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        BlockPos.Mutable areasEnd = new BlockPos.Mutable(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
        areaPoses.forEach((region, areaPos) -> {
            areasOrigin.set(
                    Math.min(areasOrigin.getX(), areaPos.getX()),
                    Math.min(areasOrigin.getY(), areaPos.getY()),
                    Math.min(areasOrigin.getZ(), areaPos.getZ())
            );
            Vec3i areaSize = schematic.getSubRegionContainer(region).getSize();
            areasEnd.set(
                    Math.max(areasEnd.getX(), areaPos.getX() + areaSize.getX()),
                    Math.max(areasEnd.getY(), areaPos.getY() + areaSize.getY()),
                    Math.max(areasEnd.getZ(), areaPos.getZ() + areaSize.getZ())
            );
        });
        this.areasOrigin = areasOrigin;
        size = areasEnd.subtract(areasOrigin);
        blocksData = new BlockState[size.getX() * size.getY() * size.getZ()];
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        ImmutableMap.Builder<BlockPos, Supplier<BlockEntity>> blockEntitiesBuilder = ImmutableMap.builder();
//#elseif MC == 11701
//$$        Map<BlockPos, Supplier<BlockEntity>> beMap = new HashMap<>();
//#endif
        areaPoses.forEach((region, areaPos) -> {
            BlockPos shift = areaPos.subtract(areasOrigin);
//#if MC == 12111
//$$            // 注意：litematica 不同版本 getBlockEntityMapForRegion 的返回值类型不一致
//#endif
//#if MC == 12111
//$$            //（旧版是原版 NbtCompound，新版是 malilib 的 CompoundData）。
//#endif
//#if MC == 12111
//$$            // 以 raw Map 接收可避免编译期泛型在 lambda 字节码里插入到 NbtCompound 的强制转换
//#endif
//#if MC == 12111
//$$            //（编译依赖签名与实际运行模组不一致时会在此处抛 ClassCastException），
//#endif
//#if MC == 12111
//$$            // 再在 toVanillaTag 里按运行时真实类型做转换，兼容两种模组版本。
//#endif
//#if MC == 12111
//$$            rawBlockEntityMap(region).forEach((relPos, nbtData) -> {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110
            schematic.getBlockEntityMapForRegion(region).forEach((relPos, nbtCompound) -> {
//#endif
                BlockPos pos = relPos.add(shift);
                if (isFilteredOut(pos)) {
                    return; // 层级过滤：范围外的方块实体不参与渲染，避免被切掉层出现悬空实体
                }
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                blockEntitiesBuilder.put(pos, Suppliers.memoize(() -> {
//#elseif MC == 11701
//$$                beMap.put(pos, Suppliers.memoize(() -> {
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110
                    BlockEntity blockEntity = silentCreateTileFromNbt(pos, getBlockState(pos), nbtCompound, getRegistryManager());
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                    BlockEntity blockEntity = BlockEntity.createFromNbt(pos, getBlockState(pos), nbtCompound);
//#elseif MC == 12101
//$$                    BlockEntity blockEntity = BlockEntity.createFromNbt(pos, getBlockState(pos), nbtCompound, getRegistryManager());
//#elseif MC == 12111
//$$                    BlockEntity blockEntity = silentCreateTileFromNbt(pos, getBlockState(pos), toVanillaTag(nbtData), getRegistryManager());
//#endif
                    if (blockEntity != null) {
                        blockEntity.setWorld(this);
                    }
                    return blockEntity;
                }));
            });
        });
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        blockEntities = blockEntitiesBuilder.buildKeepingLast();
//#endif
//#if MC == 11701
//$$        blockEntities = beMap;
//#endif
//#if MC == 11701
//$$    }
//#endif
//#if MC == 11701
//$$
//#endif
//#if MC == 11701
//$$    @Override
//#endif
//#if MC == 11701
//$$    public DynamicRegistryManager getRegistryManager() {
//#endif
//#if MC == 11701
//$$        return MinecraftClient.getInstance().world.getRegistryManager();
//#endif
//#if MC == 12111
//$$    }
//#endif
//#if MC == 12111
//$$
//#endif
//#if MC == 12111
//$$    @SuppressWarnings("unchecked")
//#endif
//#if MC == 12111
//$$    private Map<BlockPos, Object> rawBlockEntityMap(String region) {
//#endif
//#if MC == 12111
//$$        return (Map<BlockPos, Object>) (Map<?, ?>) schematic.getBlockEntityMapForRegion(region);
//#endif
//#if MC == 12111
//$$    }
//#endif
//#if MC == 12111
//$$
//#endif
//#if MC == 12111
//$$    private static NbtCompound toVanillaTag(Object nbtData) {
//#endif
//#if MC == 12111
//$$        if (nbtData instanceof NbtCompound compound) {
//#endif
//#if MC == 12111
//$$            return compound;
//#endif
//#if MC == 12111
//$$        }
//#endif
//#if MC == 12111
//$$        return DataConverterNbt.toVanillaCompound((CompoundData) nbtData);
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    @Nullable
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static BlockEntity silentCreateTileFromNbt(BlockPos pos, BlockState state, NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        String string = nbt.getString("id", "");
//#elseif MC == 12104
//$$        String string = nbt.getString("id");
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Identifier identifier = Identifier.tryParse(string);
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (identifier == null) {
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            return null;
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        } else {
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            return Registries.BLOCK_ENTITY_TYPE.getOptionalValue(identifier).map((type) -> {
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                try {
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    return type.instantiate(pos, state);
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                } catch (Exception e) {
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    return null;
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                }
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }).map((blockEntity) -> {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                try (ErrorReporter.Logging logging = new ErrorReporter.Logging(blockEntity.getReporterContext(), LitematListMod.LOGGER)) {
//#elseif MC == 12104
//$$                try {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    blockEntity.read(NbtReadView.create(logging, registries, nbt));
//#elseif MC == 12104
//$$                    blockEntity.read(nbt, registries);
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    return blockEntity;
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                } catch (Exception e) {
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    return null;
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            }).orElse(null);
//#endif
//#if MC == 12104
//$$            }).orElseGet(() -> {
//#endif
//#if MC == 12104
//$$                return null;
//#endif
//#if MC == 12104
//$$            });
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
    }

    public Vec3i getSize() {
        return size;
    }

    @Override
    public float getBrightness(Direction direction, boolean shaded) {
        return 1.0F;
    }

    @Override
    public LightingProvider getLightingProvider() {
        return lightingProvider;
    }

    @Override
    public int getColor(BlockPos pos, ColorResolver colorResolver) {
        return colorResolver.getColor(biome, pos.getX(), pos.getZ());
    }

    @Override
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public RegistryEntry<Biome> getGeneratorStoredBiome(int biomeX, int biomeY, int biomeZ) {
//#elseif MC == 11701
//$$    public Biome getGeneratorStoredBiome(int biomeX, int biomeY, int biomeZ) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        return this.getRegistryManager().getOrThrow(RegistryKeys.BIOME).getOrThrow(BiomeKeys.PLAINS);
//#elseif MC == 11701
//$$        return biome;
//#elseif MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$        return null;
//#endif
    }

//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    @Override
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public int getSeaLevel() {
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return 0;
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        Supplier<BlockEntity> supplier = blockEntities.get(pos);
        return supplier != null ? supplier.get() : null;
//#if MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 12110 || MC == 12111

//#endif
//#if MC == 12110 || MC == 12111
    @Override
//#endif
//#if MC == 12110 || MC == 12111
    public void setSpawnPoint(WorldProperties.SpawnPoint spawnPoint) {
//#endif
//#if MC == 12110 || MC == 12111

//#endif
//#if MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 12110 || MC == 12111

//#endif
//#if MC == 12110 || MC == 12111
    @Override
//#endif
//#if MC == 12110 || MC == 12111
    public WorldProperties.SpawnPoint getSpawnPoint() {
//#endif
//#if MC == 12110 || MC == 12111
        return null;
//#endif
    }

    @Nullable
    @Override
    public Entity getEntityById(int id) {
        return null;
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    @Override
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public Collection<EnderDragonPart> getEnderDragonParts() {
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return List.of();
//#endif
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    @Override
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public TickManager getTickManager() {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return null;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
    @Nullable
    @Override
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public MapState getMapState(MapIdComponent id) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    public MapState getMapState(String id) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$        return null;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$    @Override
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    public void putMapState(String id, MapState state) {
//#elseif MC == 12101 || MC == 12104
//$$    public void putMapState(MapIdComponent id, MapState state) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$    @Override
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    public int getNextMapId() {
//#elseif MC == 12101 || MC == 12104
//$$    public MapIdComponent increaseAndGetMapId() {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return null;
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        return 0;
//#endif
    }

    @Override
    public void setBlockBreakingInfo(int entityId, BlockPos pos, int progress) {

    }

    @Override
    public Scoreboard getScoreboard() {
        return null;
    }

    @Override
    public RecipeManager getRecipeManager() {
        return null;
    }

    @Override
//#if MC == 11701
//$$    public TagManager getTagManager() {
//#endif
//#if MC == 11701
//$$        return null;
//#endif
//#if MC == 11701
//$$    }
//#endif
//#if MC == 11701
//$$
//#endif
//#if MC == 11701
//$$    @Override
//#endif
    protected EntityLookup<Entity> getEntityLookup() {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return null;
//#endif
//#if MC == 12111
//$$    }
//#endif
//#if MC == 12111
//$$
//#endif
//#if MC == 12111
//$$    @Override
//#endif
//#if MC == 12111
//$$    public WorldEnvironmentAttributeAccess getEnvironmentAttributes() {
//#endif
//#if MC == 12111
//$$        return WorldEnvironmentAttributeAccess.builder().world(this).build();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    @Override
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public BrewingRecipeRegistry getBrewingRecipeRegistry() {
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return null;
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    @Override
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public FuelRegistry getFuelRegistry() {
//#endif
        return null;
    }

    public Map<BlockPos, Supplier<BlockEntity>> getBlockEntities() {
        return blockEntities;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        if (blocksData == null || isFilteredOut(pos) || isOutOfSize(pos.getX(), pos.getY(), pos.getZ(), size)) {
            return Blocks.AIR.getDefaultState();
        }
        int index = getBlocksDataIndex(pos);
        BlockState state = blocksData[index];
        if (state == null) {
            for (Map.Entry<String, BlockPos> areaEntry : areaPoses.entrySet()) {
                LitematicaBlockStateContainer container = schematic.getSubRegionContainer(areaEntry.getKey());
                int x = pos.getX() - areaEntry.getValue().getX() + areasOrigin.getX();
                int y = pos.getY() - areaEntry.getValue().getY() + areasOrigin.getY();
                int z = pos.getZ() - areaEntry.getValue().getZ() + areasOrigin.getZ();
                if (!isOutOfSize(x, y, z, container.getSize())) {
                    return blocksData[index] = container.get(x, y, z);
                }
            }
        }
        return state != null ? state : Blocks.AIR.getDefaultState();
    }

    private static boolean isOutOfSize(int x, int y, int z, Vec3i size) {
        return x < 0 || x >= size.getX() || y < 0 || y >= size.getY() || z < 0 || z >= size.getZ();
    }

//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /**
     * 层级过滤：把渲染空间坐标（file - areasOrigin）加上 areasOrigin 换算为原理图文件坐标后，
     * 判断该坐标是否在「同步投影渲染层」过滤区间之外。
     * 范围外一律视为 AIR，这样邻居面剔除会自然让切割处的面显示出来（切面不再破洞）。
     */
//#elseif MC == 11701
//$$    /**
//$$     * 层级过滤：把渲染空间坐标（file - areasOrigin）加上 areasOrigin 换算为原理图文件坐标后，
//$$     * 判断该坐标是否在「同步投影渲染层」过滤区间之外。
//$$     */
//#endif
    private boolean isFilteredOut(BlockPos renderPos) {
        if (layerFilter == null) {
            return false;
        }
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        int k = switch (layerFilter.axis()) {
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            case 0 -> renderPos.getX() + areasOrigin.getX();
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            case 2 -> renderPos.getZ() + areasOrigin.getZ();
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            default -> renderPos.getY() + areasOrigin.getY();
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        };
//#endif
//#if MC == 11701
//$$        int k;
//#endif
//#if MC == 11701
//$$        switch (layerFilter.axis()) {
//#endif
//#if MC == 11701
//$$            case 0 -> k = renderPos.getX() + areasOrigin.getX();
//#endif
//#if MC == 11701
//$$            case 2 -> k = renderPos.getZ() + areasOrigin.getZ();
//#endif
//#if MC == 11701
//$$            default -> k = renderPos.getY() + areasOrigin.getY();
//#endif
//#if MC == 11701
//$$        }
//#endif
        return !layerFilter.contains(k);
    }

    private int getBlocksDataIndex(BlockPos pos) {
        return pos.getY() * size.getX() * size.getZ() + pos.getZ() * size.getX() + pos.getX();
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Override
//#if MC == 12108 || MC == 12110 || MC == 12111
    public void playSound(@Nullable Entity source, double x, double y, double z, RegistryEntry<SoundEvent> sound, SoundCategory category, float volume, float pitch, long seed) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$    public void playSound(@Nullable PlayerEntity source, double x, double y, double z, RegistryEntry<SoundEvent> sound, SoundCategory category, float volume, float pitch, long seed) {}
//#endif
//#if MC == 11701
//$$    public void playSound(@Nullable PlayerEntity source, double x, double y, double z, SoundEvent sound, SoundCategory category, float volume, float pitch) {
//#endif
//#if MC == 11701
//$$    }
//#endif

    @Override
//#if MC == 12108 || MC == 12110 || MC == 12111
    public void playSoundFromEntity(@Nullable Entity source, Entity entity, RegistryEntry<SoundEvent> sound, SoundCategory category, float volume, float pitch, long seed) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$    public void playSoundFromEntity(@Nullable PlayerEntity source, Entity entity, RegistryEntry<SoundEvent> sound, SoundCategory category, float volume, float pitch, long seed) {}
//#endif
//#if MC == 11701
//$$    public void playSoundFromEntity(@Nullable PlayerEntity source, Entity entity, SoundEvent sound, SoundCategory category, float volume, float pitch) {
//#endif
//#if MC == 11701
//$$    }
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    @Override
//#endif
//#if MC == 12110 || MC == 12111
    public void createExplosion(@Nullable Entity entity, @Nullable DamageSource damageSource, @Nullable ExplosionBehavior behavior, double x, double y, double z, float power, boolean createFire, ExplosionSourceType explosionSourceType, ParticleEffect smallParticle, ParticleEffect largeParticle, Pool<BlockParticleEffect> blockParticles, RegistryEntry<SoundEvent> soundEvent) {
//#elseif MC == 12104 || MC == 12108
//$$    public void createExplosion(@Nullable Entity entity, @Nullable DamageSource damageSource, @Nullable ExplosionBehavior behavior, double x, double y, double z, float power, boolean createFire, ExplosionSourceType explosionSourceType, ParticleEffect smallParticle, ParticleEffect largeParticle, RegistryEntry<SoundEvent> soundEvent) {
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif

    @Override
    public String asString() {
        return getClass().getSimpleName();
    }

    @Override
    public int getHeight() {
        return 384;
    }

    @Override
    public int getBottomY() {
        return 0;
    }

    @Override
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$    public WorldChunk getChunk(int chunkX, int chunkZ) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$        return null;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$    @Override
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public FeatureSet getEnabledFeatures() {
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return FeatureFlags.DEFAULT_ENABLED_FEATURES;
//#endif
//#if MC == 11701
//$$    public void updateListeners(BlockPos pos, BlockState oldState, BlockState newState, int flags) {
//#endif
    }
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    @Override
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public void updateListeners(BlockPos pos, BlockState oldState, BlockState newState, int flags) {}
//#endif

    @Override
    public BlockView getWorld() {
        return this;
    }

    @Override
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public QueryableTickScheduler<Block> getBlockTickScheduler() {
//#elseif MC == 11701
//$$    public TickScheduler<Block> getBlockTickScheduler() {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        return EmptyTickSchedulers.getClientTickScheduler();
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$        return null;
//#endif
    }

    @Override
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public QueryableTickScheduler<Fluid> getFluidTickScheduler() {
//#elseif MC == 11701
//$$    public TickScheduler<Fluid> getFluidTickScheduler() {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        return EmptyTickSchedulers.getClientTickScheduler();
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$        return null;
//#endif
    }

    @Override
    public ChunkManager getChunkManager() {
//#if MC == 12108 || MC == 12110 || MC == 12111
        return fakeChunkManager;
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$        return null;
//#endif
    }

    @Override
//#if MC == 12108 || MC == 12110 || MC == 12111
    public void syncWorldEvent(@Nullable Entity source, int eventId, BlockPos pos, int data) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$    public void syncWorldEvent(@Nullable PlayerEntity player, int eventId, BlockPos pos, int data) {}
//#endif
//#if MC == 11701
//$$    public void syncWorldEvent(@Nullable PlayerEntity player, int eventId, BlockPos pos, int data) {
//#endif
//#if MC == 11701
//$$    }
//#endif

    @Override
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public void emitGameEvent(RegistryEntry<GameEvent> event, Vec3d emitterPos, GameEvent.Emitter emitter) {}
//#elseif MC == 11904 || MC == 12001
//$$    public void emitGameEvent(GameEvent event, Vec3d emitterPos, GameEvent.Emitter emitter) {}
//#endif
//#if MC == 11701
//$$    public void emitGameEvent(@Nullable Entity entity, GameEvent event, BlockPos pos) {
//#endif
//#if MC == 11701
//$$    }
//#endif
//#if MC == 11701
//$$
//#endif
//#if MC == 11701
//$$    @Override
//#endif
//#if MC == 11701
//$$    public void addParticle(ParticleEffect parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
//#endif
//#if MC == 11701
//$$    }
//#endif

    @Override
    public List<? extends PlayerEntity> getPlayers() {
        return List.of();
//#if MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
    @Override
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
    public WorldBorder getWorldBorder() {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        return worldBorder;
//#endif
    }

    private static class FakeLightingProvider extends LightingProvider {

        private final ChunkLightingView FULL_BRIGHT_VIEW = new ChunkLightingView() {
            @Nullable
            @Override
            public ChunkNibbleArray getLightSection(ChunkSectionPos pos) {
                return null;
            }
//#if MC == 11701
//$$
//#endif
            @Override
            public int getLightLevel(BlockPos pos) {
                return 15;
            }
//#if MC == 11701
//$$
//#endif
            @Override
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            public void checkBlock(BlockPos pos) {}
//#endif
//#if MC == 11701
//$$            public void checkBlock(BlockPos pos) {
//#endif
//#if MC == 11701
//$$            }
//#endif
//#if MC == 11701
//$$
//#endif
            @Override
//#if MC == 11904
//$$            public void addLightSource(BlockPos pos, int level) {}
//#endif
//#if MC == 11701
//$$            public void addLightSource(BlockPos pos, int level) {
//#endif
//#if MC == 11701
//$$            }
//#endif
//#if MC == 11701
//$$
//#endif
//#if MC == 11701 || MC == 11904
//$$            @Override
//#endif
            public boolean hasUpdates() {
                return false;
            }
//#if MC == 11701
//$$
//#endif
            @Override
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            public int doLightUpdates() {
//#elseif MC == 11701 || MC == 11904
//$$            public int doLightUpdates(int maxUpdateCount, boolean doSkylight, boolean skipEdgeLightPropagation) {
//#endif
                return 15;
            }
//#if MC == 11701
//$$
//#endif
            @Override
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            public void setSectionStatus(ChunkSectionPos pos, boolean notReady) {}
//#endif
//#if MC == 11701
//$$            public void setSectionStatus(ChunkSectionPos pos, boolean notReady) {
//#endif
//#if MC == 11701
//$$            }
//#endif
//#if MC == 11701
//$$
//#endif
            @Override
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            public void setColumnEnabled(ChunkPos pos, boolean retainData) {}
//#endif
//#if MC == 11701
//$$            public void setColumnEnabled(ChunkPos pos, boolean retainData) {
//#endif
//#if MC == 11701
//$$            }
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            @Override
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            public void propagateLight(ChunkPos chunkPos) {}
//#endif
        };

        public FakeLightingProvider(ChunkProvider chunkProvider) {
            super(chunkProvider, false, false);
        }

        @Override
        public ChunkLightingView get(LightType lightType) {
            return FULL_BRIGHT_VIEW;
        }

    }

//#if MC == 12108 || MC == 12110 || MC == 12111
    private class FakeChunkManager extends ChunkManager {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        private final Map<ChunkPos, FakeChunk> fakeChunks = new HashMap<>();
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        @Nullable
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        @Override
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        public Chunk getChunk(int x, int z, ChunkStatus leastStatus, boolean create) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            ChunkPos chunkPos = new ChunkPos(x, z);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            return fakeChunks.computeIfAbsent(chunkPos, FakeChunk::new);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        @Override
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        public void tick(BooleanSupplier shouldKeepTicking, boolean tickChunks) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        @Override
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        public String getDebugString() {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            return "FakeChunkManager";
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        @Override
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        public int getLoadedChunkCount() {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            return 0;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        @Override
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        public LightingProvider getLightingProvider() {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            return lightingProvider;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        @Override
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        public BlockView getWorld() {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            return WorldSchematicWrapper.this;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
    private class FakeChunk extends Chunk {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        private final BlockPos chunkOrigin;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        public FakeChunk(ChunkPos pos) {
//#endif
//#if MC == 12110 || MC == 12111
            super(pos, new UpgradeData(new NbtCompound(), WorldSchematicWrapper.this), WorldSchematicWrapper.this, palettesFactory, 0, null, null);
//#elseif MC == 12108
//$$            super(pos, new UpgradeData(new NbtCompound(), WorldSchematicWrapper.this), WorldSchematicWrapper.this, WorldSchematicWrapper.this.getRegistryManager().getOrThrow(RegistryKeys.BIOME), 0, null, null);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            chunkOrigin = pos.getStartPos();
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        @Nullable
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        @Override
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        public BlockState setBlockState(BlockPos pos, BlockState state, int flags) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            return null;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        @Override
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        public void setBlockEntity(BlockEntity blockEntity) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        @Override
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        public void addEntity(Entity entity) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        @Override
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        public ChunkStatus getStatus() {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            return ChunkStatus.FULL;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        @Override
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        public void removeBlockEntity(BlockPos pos) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        @Nullable
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        @Override
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        public NbtCompound getPackedBlockEntityNbt(BlockPos pos, RegistryWrapper.WrapperLookup registries) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            BlockEntity blockEntity = getBlockEntity(pos);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            if (blockEntity != null) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                NbtCompound nbtCompound = blockEntity.createNbtWithIdentifyingData(registries);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                nbtCompound.putBoolean("keepPacked", false);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                return nbtCompound;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            return null;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        @Override
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        public BasicTickScheduler<Block> getBlockTickScheduler() {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            return EmptyTickSchedulers.getReadOnlyTickScheduler();
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        @Override
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        public BasicTickScheduler<Fluid> getFluidTickScheduler() {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            return EmptyTickSchedulers.getReadOnlyTickScheduler();
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        @Override
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        public TickSchedulers getTickSchedulers(long time) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            return new TickSchedulers(List.of(), List.of());
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        @Nullable
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        @Override
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        public BlockEntity getBlockEntity(BlockPos pos) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            return WorldSchematicWrapper.this.getBlockEntity(chunkOrigin.add(pos));
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        @Override
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        public BlockState getBlockState(BlockPos pos) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            return WorldSchematicWrapper.this.getBlockState(chunkOrigin.add(pos));
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        @Override
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        public FluidState getFluidState(BlockPos pos) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            return WorldSchematicWrapper.this.getFluidState(chunkOrigin.add(pos));
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
}
//#if MC == 11701
//$$
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif