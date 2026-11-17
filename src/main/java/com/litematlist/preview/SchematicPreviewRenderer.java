package com.litematlist.preview;
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$import com.litematlist.LitematListMod;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$import com.litematlist.LitematicaBridge.LayerFilter;
//#endif
//#if MC == 12108
//$$import com.litematlist.LitematListMod;
//#endif
//#if MC == 12108
//$$import com.litematlist.LitematicaBridge.LayerFilter;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12111
//$$import com.litematlist.LitematicaBridge.LayerFilter;
//#endif
//#if MC == 12111
//$$import com.litematlist.LitematListMod;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
import com.mojang.blaze3d.buffers.GpuBuffer;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
import com.mojang.blaze3d.buffers.GpuBufferSlice;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
import com.mojang.blaze3d.systems.RenderPass;
//#endif
import com.mojang.blaze3d.systems.RenderSystem;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import com.mojang.blaze3d.systems.VertexSorter;
//#endif
//#if MC == 12111
//$$import com.mojang.blaze3d.textures.FilterMode;
//#endif
//#if MC == 12111
//$$import com.mojang.blaze3d.textures.GpuTextureView;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
import com.mojang.blaze3d.vertex.VertexFormat;
//#endif
//#if MC == 12101 || MC == 12104
//$$import com.litematlist.LitematicaBridge.LayerFilter;
//#endif
//#if MC == 12101 || MC == 12104
//$$import com.litematlist.LitematListMod;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import fi.dy.masa.litematica.render.schematic.IBufferBuilderPatch;
//#endif
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
//#if MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.gl.DynamicUniforms;
//#endif
import net.minecraft.client.gl.Framebuffer;
//#if MC == 12104
//$$import net.minecraft.client.gl.GlUsage;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$import net.minecraft.client.gl.ShaderProgram;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$import net.minecraft.client.gl.VertexBuffer;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.render.*;
//#endif
//#if MC == 11701
//$$import net.minecraft.client.render.BufferBuilder;
//#endif
//#if MC == 11701
//$$import net.minecraft.client.render.LightmapTextureManager;
//#endif
//#if MC == 11701
//$$import net.minecraft.client.render.OverlayTexture;
//#endif
//#if MC == 11701
//$$import net.minecraft.client.render.RenderLayer;
//#endif
//#if MC == 11701
//$$import net.minecraft.client.render.RenderLayers;
//#endif
//#if MC == 11701
//$$import net.minecraft.client.render.Shader;
//#endif
//#if MC == 11701
//$$import net.minecraft.client.render.VertexConsumerProvider;
//#endif
import net.minecraft.client.render.block.BlockRenderManager;
//#if MC == 12110 || MC == 12111
import net.minecraft.client.render.block.entity.BlockEntityRenderManager;
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
//#endif
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
//#if MC == 12110 || MC == 12111
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.render.chunk.Buffers;
//#endif
//#if MC == 12110 || MC == 12111
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
//#endif
//#if MC == 12110 || MC == 12111
import net.minecraft.client.render.command.RenderDispatcher;
//#endif
//#if MC == 12110 || MC == 12111
import net.minecraft.client.render.state.CameraRenderState;
//#endif
//#if MC == 12111
//$$import net.minecraft.client.texture.SpriteAtlasTexture;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.util.BufferAllocator;
//#endif
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
//#if MC == 12108
//$$import net.minecraft.util.math.Vec3d;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.util.math.random.LocalRandom;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.util.math.random.Random;
//#endif
//#if MC == 11701
//$$import net.minecraft.util.math.Vec3f;
//#endif
import org.jetbrains.annotations.Nullable;
//#if MC == 12108 || MC == 12110 || MC == 12111
import org.joml.Matrix4f;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import org.joml.Vector3f;
//#endif
//#if MC == 12108 || MC == 12110
import org.joml.Vector4f;
//#endif
//#if MC == 12110
import com.litematlist.LitematicaBridge.LayerFilter;
//#endif
//#if MC == 12110
import com.litematlist.LitematListMod;
//#endif

//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import java.util.*;
//#endif
//#if MC == 11701
//$$import java.util.ArrayList;
//#endif
//#if MC == 11701
//$$import java.util.Comparator;
//#endif
//#if MC == 11701
//$$import java.util.HashMap;
//#endif
//#if MC == 11701
//$$import java.util.List;
//#endif
//#if MC == 11701
//$$import java.util.Map;
//#endif
//#if MC == 11701
//$$import java.util.Objects;
//#endif
//#if MC == 11701
//$$import java.util.Random;
//#endif
import java.util.concurrent.CompletableFuture;

//#if MC == 12111
//$$/**
//$$ * 原理图离屏渲染器（1.21.11 渲染管线重构版）：
//$$ * 与原版区块渲染一致，按方块层把每个区块的顶点写进共享 UBO（ChunkSection），
//$$ * 再按 BlockRenderLayerGroup（OPAQUE/TRANSLUCENT/TRIPWIRE）分别提交绘制 pass。
//$$ */
//#endif
public class SchematicPreviewRenderer implements AutoCloseable {

//#if MC == 12108 || MC == 12110 || MC == 12111
    private static final BlockRenderLayer[] BLOCK_RENDER_LAYER_ORDER = new BlockRenderLayer[] {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            BlockRenderLayer.SOLID,
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            BlockRenderLayer.CUTOUT,
//#endif
//#if MC == 12108 || MC == 12110
            BlockRenderLayer.CUTOUT_MIPPED,
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            BlockRenderLayer.TRANSLUCENT,
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            BlockRenderLayer.TRIPWIRE
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
    };
//#endif
//#if MC == 12111
//$$    private final MinecraftClient mc;
//#endif
    private final WorldSchematicWrapper world;
    private final BlockRenderManager blockRenderManager;
//#if MC == 12110 || MC == 12111
    private final BlockEntityRenderManager blockEntityRenderManager;
//#endif
//#if MC == 12110 || MC == 12111
    private final OrderedRenderCommandQueueImpl orderedRenderCommandQueue;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$    private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
//#endif
//#if MC == 12108 || MC == 12110
    private final CustomVertexConsumerProvider customVertexConsumerProvider;
//#elseif MC == 12111
//$$    private final VertexConsumerProvider.Immediate immediate;
//#endif
//#if MC == 12110 || MC == 12111
    private final RenderDispatcher renderDispatcher;
//#endif
    private final List<ChunkEntry> chunks = new ArrayList<>();
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private final Vector3f pos = new Vector3f(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);
//#elseif MC == 11701
//$$    private final Vec3f pos = new Vec3f(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private final Vector3f lastPos = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
//#elseif MC == 11701
//$$    private final Vec3f lastPos = new Vec3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
//#endif
    private ChunkPos lastChunkPos = new ChunkPos(Integer.MIN_VALUE, Integer.MIN_VALUE);
//#if MC == 12110 || MC == 12111
    private CameraRenderState cameraRenderState;
//#endif
    private boolean updated;
    private boolean canceled;
    private Framebuffer target;

    public SchematicPreviewRenderer(MinecraftClient mc) {
//#if MC == 12111
//$$        this.mc = mc;
//#endif
        world = new WorldSchematicWrapper(mc);
        blockRenderManager = mc.getBlockRenderManager();
//#if MC == 12110 || MC == 12111
        blockEntityRenderManager = mc.getBlockEntityRenderDispatcher();
//#endif
//#if MC == 12110 || MC == 12111
        orderedRenderCommandQueue = new OrderedRenderCommandQueueImpl();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        blockEntityRenderDispatcher = mc.getBlockEntityRenderDispatcher();
//#endif
//#if MC == 12108 || MC == 12110
        customVertexConsumerProvider = new CustomVertexConsumerProvider(mc.getBufferBuilders().getEntityVertexConsumers());
//#elseif MC == 12111
//$$        immediate = mc.getBufferBuilders().getEntityVertexConsumers();
//#endif
//#if MC == 12110 || MC == 12111
        renderDispatcher = new RenderDispatcher(
//#endif
//#if MC == 12110 || MC == 12111
                orderedRenderCommandQueue,
//#endif
//#if MC == 12110 || MC == 12111
                blockRenderManager,
//#endif
//#if MC == 12110
                customVertexConsumerProvider,
//#elseif MC == 12111
//$$                immediate,
//#endif
//#if MC == 12110 || MC == 12111
                mc.getAtlasManager(),
//#endif
//#if MC == 12110 || MC == 12111
                new DummyOutlineVertexConsumerProvider(),
//#endif
//#if MC == 12110 || MC == 12111
                new DummyVertexConsumerProvider(),
//#endif
//#if MC == 12110 || MC == 12111
                mc.textRenderer
//#endif
//#if MC == 12110 || MC == 12111
        );
//#endif
    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public void setup(LitematicaSchematic schematic, @Nullable LayerFilter filter) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    public void setup(LitematicaSchematic schematic, @Nullable LayerFilter layerFilter) {
//#endif
        close();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        world.setSchematic(schematic, filter);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        world.setSchematic(schematic, layerFilter);
//#endif

        int height = world.getSize().getY();
        int chunksX = world.getSize().getX() >>> 4;
        int chunksZ = world.getSize().getZ() >>> 4;

        for (int chunkX = 0; chunkX <= chunksX; chunkX++) {
            for (int chunkZ = 0; chunkZ <= chunksZ; chunkZ++) {
                ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                chunks.add(new ChunkEntry(chunkPos, CompletableFuture.supplyAsync(() -> {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    Random random = new LocalRandom(0);
//#elseif MC == 11701
//$$                    Random random = new Random(0);
//#endif
                    BuiltChunk chunk = new BuiltChunk();
                    MatrixStack matrices = new MatrixStack();
                    BlockPos.Mutable worldPos = new BlockPos.Mutable();
                    int chunkStartX = chunkPos.getStartX();
                    int chunkStartZ = chunkPos.getStartZ();
                    int chunkEndX = chunkStartX + 16;
                    int chunkEndZ = chunkStartZ + 16;
                    for (int y = 0; y < height; y++) {
                        for (int z = chunkStartZ; z < chunkEndZ; z++) {
                            for (int x = chunkStartX; x < chunkEndX; x++) {
                                if (canceled) {
                                    chunk.close();
                                    return null;
                                }
                                worldPos.set(x, y, z);
                                BlockState state = world.getBlockState(worldPos);
                                FluidState fluid = state.getFluidState();
                                boolean renderFluid = !fluid.isEmpty();
                                boolean renderBlock = state.getRenderType() == BlockRenderType.MODEL;
                                if (renderFluid || renderBlock) {
                                    matrices.push();
                                    matrices.translate(worldPos.getX() & 0xF, worldPos.getY(), worldPos.getZ() & 0xF);
                                    if (renderFluid) {
//#if MC == 11904 || MC == 12001
//$$                                        // MC 1.20.1: fluid vertices use section-relative Y, so compensate the section origin
//#endif
//#if MC == 12108 || MC == 12110
                                        BlockRenderLayer layer = RenderLayers.getFluidLayer(fluid);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                                        RenderLayer layer = RenderLayers.getFluidLayer(fluid);
//#elseif MC == 12111
//$$                                        BlockRenderLayer layer = BlockRenderLayers.getFluidLayer(fluid);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                        BufferBuilder builder = chunk.getBuilderByLayer(layer);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                                        PreviewBufferBuilder builder = chunk.getBuilderByLayer(layer);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                        ((IBufferBuilderPatch) builder).litematica$setOffsetY((worldPos.getY() >> 4) << 4);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                                        builder.setYOffset((worldPos.getY() >> 4) << 4);
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                        blockRenderManager.renderFluid(worldPos, world, builder, state, fluid);
//#elseif MC == 11701
//$$                                        blockRenderManager.renderFluid(worldPos, world, builder, fluid);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                        ((IBufferBuilderPatch) builder).litematica$setOffsetY(0.0F);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                                        builder.setYOffset(0.0);
//#endif
                                    }
                                    if (renderBlock) {
//#if MC == 12108 || MC == 12110
                                        BlockRenderLayer layer = RenderLayers.getBlockLayer(state);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                                        RenderLayer layer = RenderLayers.getBlockLayer(state);
//#elseif MC == 12111
//$$                                        BlockRenderLayer layer = BlockRenderLayers.getBlockLayer(state);
//#endif
                                        BufferBuilder builder = chunk.getBuilderByLayer(layer);
                                        blockRenderManager.getModelRenderer().renderSmooth(
                                                world,
//#if MC == 12108 || MC == 12110 || MC == 12111
                                                blockRenderManager.getModel(state).getParts(random),
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                                                blockRenderManager.getModel(state),
//#endif
                                                state,
                                                worldPos,
                                                matrices,
                                                builder,
                                                true,
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                                                random,
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                                                state.getRenderingSeed(worldPos),
//#endif
                                                OverlayTexture.DEFAULT_UV
                                        );
                                    }
                                    matrices.pop();
                                }
                            }
                        }
                    }
                    return chunk;
                })));
            }
        }
    }

//#if MC == 12110 || MC == 12111
    public void prepareRender(CameraRenderState cameraRenderState, Framebuffer target) {
//#endif
//#if MC == 12110 || MC == 12111
        this.cameraRenderState = cameraRenderState;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$    public void prepareRender(float x, float y, float z, Framebuffer target) {
//#endif
        this.target = target;
//#if MC == 12110 || MC == 12111
        pos.set(cameraRenderState.pos.x, cameraRenderState.pos.y, cameraRenderState.pos.z);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        pos.set(x, y, z);
//#endif
        updated = !lastPos.equals(pos);
        if (updated) {
//#if MC == 12110 || MC == 12111
            lastPos.set(cameraRenderState.pos.x, cameraRenderState.pos.y, cameraRenderState.pos.z);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            lastPos.set(x, y, z);
//#endif
//#if MC == 12110 || MC == 12111
            ChunkPos chunkPos = new ChunkPos(MathHelper.floor(cameraRenderState.pos.x) >> 4, MathHelper.floor(cameraRenderState.pos.z) >> 4);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            ChunkPos chunkPos = new ChunkPos(MathHelper.floor(x) >> 4, MathHelper.floor(z) >> 4);
//#endif
            if (!lastChunkPos.equals(chunkPos)) {
                lastChunkPos = chunkPos;
                chunks.sort(Comparator.comparingInt(chunk -> -(Math.abs(chunk.pos.x - chunkPos.x) + Math.abs(chunk.pos.z - chunkPos.z))));
            }
        }
    }

//#if MC == 12111
//$$    /**
//$$     * 收集所有区块的绘制对象：每个区块一个 ChunkSection 信息（携带区块原点的绝对坐标），
//$$     * 顶点排序按「相机到区块原点」的距离进行，与原版区块渲染语义一致。
//$$     */
//#endif
//#if MC == 12111
//$$    private SectionRenderState prepareChunks() {
//#endif
//#if MC == 12108 || MC == 12110
    public void renderBlocks() {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        EnumMap<BlockRenderLayer, List<RenderPass.RenderObject<GpuBufferSlice[]>>> drawsPerLayer = new EnumMap<>(BlockRenderLayer.class);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        int maxIndicesRequired = 0;
//#endif
//#if MC == 12108 || MC == 12110
        List<DynamicUniforms.UniformValue> dynamicTransforms = new ArrayList<>();
//#endif
//#if MC == 12108 || MC == 12110

//#endif
//#if MC == 12108 || MC == 12110
        VertexSorter vertexSorter = VertexSorter.byDistance(pos.x, pos.y, pos.z);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        for (BlockRenderLayer layer : BLOCK_RENDER_LAYER_ORDER) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            drawsPerLayer.put(layer, new ArrayList<>());
//#endif
//#if MC == 12111
//$$        }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12111
//$$        List<DynamicUniforms.ChunkSectionsValue> chunkSectionInfos = new ArrayList<>();
//#endif
//#if MC == 12111
//$$        GpuTextureView atlasView = mc.getTextureManager().getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).getGlTextureView();
//#endif
//#if MC == 12111
//$$        int atlasWidth = atlasView.getWidth(0);
//#endif
//#if MC == 12111
//$$        int atlasHeight = atlasView.getHeight(0);
//#endif
//#if MC == 12108 || MC == 12110
            for (ChunkEntry chunk : chunks) {
//#endif
//#if MC == 12108 || MC == 12110
                if (chunk.future.isDone()) {
//#endif
//#if MC == 12108 || MC == 12110
                    BuiltChunk built = chunk.future.join();
//#endif
//#if MC == 12108 || MC == 12110
                    built.uploadBuffer(layer, vertexSorter);
//#endif
//#if MC == 12108 || MC == 12110
                    Buffers buffers = built.buffers.get(layer);
//#endif
//#if MC == 12108 || MC == 12110
                    if (buffers != null) {
//#endif
//#if MC == 12108 || MC == 12110
                        if (updated) {
//#endif
//#if MC == 12108 || MC == 12110
                            built.resortTransparent(layer, vertexSorter);
//#endif
//#if MC == 12108 || MC == 12110
                        }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12111
//$$        for (ChunkEntry chunk : chunks) {
//#endif
//#if MC == 12111
//$$            if (!chunk.future.isDone()) {
//#endif
//#if MC == 12111
//$$                continue;
//#endif
//#if MC == 12111
//$$            }
//#endif
//#if MC == 12111
//$$            BuiltChunk built = chunk.future.join();
//#endif
//#if MC == 12111
//$$            if (built == null) {
//#endif
//#if MC == 12111
//$$                continue;
//#endif
//#if MC == 12111
//$$            }
//#endif
//#if MC == 12111
//$$            VertexSorter vertexSorter = VertexSorter.byDistance(
//#endif
//#if MC == 12111
//$$                    pos.x - chunk.pos.getStartX(),
//#endif
//#if MC == 12111
//$$                    pos.y,
//#endif
//#if MC == 12111
//$$                    pos.z - chunk.pos.getStartZ()
//#endif
//#if MC == 12111
//$$            );
//#endif
//#if MC == 12108 || MC == 12110
                        GpuBuffer gpuBuffer;
//#endif
//#if MC == 12108 || MC == 12110
                        VertexFormat.IndexType indexType;
//#endif
//#if MC == 12108 || MC == 12110
                        if (buffers.getIndexBuffer() == null) {
//#endif
//#if MC == 12108 || MC == 12110
                            maxIndicesRequired = Math.max(maxIndicesRequired, buffers.getIndexCount());
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12111
//$$            int infoIndex = -1;
//#endif
//#if MC == 12111
//$$            for (BlockRenderLayer layer : BLOCK_RENDER_LAYER_ORDER) {
//#endif
//#if MC == 12111
//$$                built.uploadBuffer(layer, vertexSorter);
//#endif
//#if MC == 12111
//$$                if (updated) {
//#endif
//#if MC == 12111
//$$                    built.resortTransparent(layer, vertexSorter);
//#endif
//#if MC == 12111
//$$                }
//#endif
//#if MC == 12111
//$$                Buffers buffers = built.buffers.get(layer);
//#endif
//#if MC == 12111
//$$                if (buffers != null) {
//#endif
//#if MC == 12111
//$$                    if (infoIndex == -1) {
//#endif
//#if MC == 12111
//$$                        infoIndex = chunkSectionInfos.size();
//#endif
//#if MC == 12111
//$$                        chunkSectionInfos.add(new DynamicUniforms.ChunkSectionsValue(
//#endif
//#if MC == 12111
//$$                                new Matrix4f(RenderSystem.getModelViewMatrix()),
//#endif
//#if MC == 12111
//$$                                chunk.pos.getStartX(),
//#endif
//#if MC == 12111
//$$                                0,
//#endif
//#if MC == 12111
//$$                                chunk.pos.getStartZ(),
//#endif
//#if MC == 12111
//$$                                1.0F,
//#endif
//#if MC == 12111
//$$                                atlasWidth,
//#endif
//#if MC == 12111
//$$                                atlasHeight
//#endif
//#if MC == 12111
//$$                        ));
//#endif
//#if MC == 12111
//$$                    }
//#endif
//#if MC == 12108 || MC == 12110
                            gpuBuffer = null;
//#endif
//#if MC == 12108 || MC == 12110
                            indexType = null;
//#endif
//#if MC == 12108 || MC == 12110
                        } else {
//#endif
//#if MC == 12108 || MC == 12110
                            gpuBuffer = buffers.getIndexBuffer();
//#endif
//#if MC == 12108 || MC == 12110
                            indexType = buffers.getIndexType();
//#endif
//#if MC == 12108 || MC == 12110
                        }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12111
//$$                    GpuBuffer gpuBuffer;
//#endif
//#if MC == 12111
//$$                    VertexFormat.IndexType indexType;
//#endif
//#if MC == 12111
//$$                    if (buffers.getIndexBuffer() == null) {
//#endif
//#if MC == 12111
//$$                        maxIndicesRequired = Math.max(maxIndicesRequired, buffers.getIndexCount());
//#endif
//#if MC == 12111
//$$                        gpuBuffer = null;
//#endif
//#if MC == 12111
//$$                        indexType = null;
//#endif
//#if MC == 12111
//$$                    } else {
//#endif
//#if MC == 12111
//$$                        gpuBuffer = buffers.getIndexBuffer();
//#endif
//#if MC == 12111
//$$                        indexType = buffers.getIndexType();
//#endif
//#if MC == 12108 || MC == 12110
                        int j = dynamicTransforms.size();
//#endif
//#if MC == 12108 || MC == 12110
                        dynamicTransforms.add(
//#endif
//#if MC == 12108 || MC == 12110
                                new DynamicUniforms.UniformValue(
//#endif
//#if MC == 12108 || MC == 12110
                                        RenderSystem.getModelViewMatrix(),
//#endif
//#if MC == 12108 || MC == 12110
                                        new Vector4f(1.0F),
//#endif
//#if MC == 12108 || MC == 12110
                                        new Vector3f(
//#endif
//#if MC == 12108 || MC == 12110
                                                chunk.pos.getStartX() - pos.x,
//#endif
//#if MC == 12108 || MC == 12110
                                                -pos.y,
//#endif
//#if MC == 12108 || MC == 12110
                                                chunk.pos.getStartZ() - pos.z
//#endif
//#if MC == 12108 || MC == 12110
                                        ),
//#endif
//#if MC == 12108 || MC == 12110
                                        new Matrix4f(),
//#endif
//#if MC == 12108 || MC == 12110
                                        1.0F
//#endif
//#if MC == 12108 || MC == 12110
                                )
//#endif
//#if MC == 12108 || MC == 12110
                        );
//#endif
//#if MC == 12108 || MC == 12110
                        drawsPerLayer.get(layer).add(
//#endif
//#if MC == 12108 || MC == 12110
                                new RenderPass.RenderObject<>(
//#endif
//#if MC == 12108 || MC == 12110
                                        0,
//#endif
//#if MC == 12108 || MC == 12110
                                        buffers.getVertexBuffer(),
//#endif
//#if MC == 12108 || MC == 12110
                                        gpuBuffer,
//#endif
//#if MC == 12108 || MC == 12110
                                        indexType,
//#endif
//#if MC == 12108 || MC == 12110
                                        0,
//#endif
//#if MC == 12108 || MC == 12110
                                        buffers.getIndexCount(),
//#endif
//#if MC == 12108 || MC == 12110
                                        (gpuBufferSlicesx, uniformUploader) -> uniformUploader.upload("DynamicTransforms", gpuBufferSlicesx[j])
//#endif
//#if MC == 12108 || MC == 12110
                                )
//#endif
//#if MC == 12108 || MC == 12110
                        );
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    }
//#endif
//#if MC == 12111
//$$
//#endif
//#if MC == 12111
//$$                    int finalInfoIndex = infoIndex;
//#endif
//#if MC == 12111
//$$                    drawsPerLayer.get(layer).add(
//#endif
//#if MC == 12111
//$$                            new RenderPass.RenderObject<>(
//#endif
//#if MC == 12111
//$$                                    0,
//#endif
//#if MC == 12111
//$$                                    buffers.getVertexBuffer(),
//#endif
//#if MC == 12111
//$$                                    gpuBuffer,
//#endif
//#if MC == 12111
//$$                                    indexType,
//#endif
//#if MC == 12111
//$$                                    0,
//#endif
//#if MC == 12111
//$$                                    buffers.getIndexCount(),
//#endif
//#if MC == 12111
//$$                                    (gpuBufferSlices, uniformUploader) -> uniformUploader.upload("ChunkSection", gpuBufferSlices[finalInfoIndex])
//#endif
//#if MC == 12111
//$$                            )
//#endif
//#if MC == 12111
//$$                    );
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$    public void renderLayer(RenderLayer layer) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$        layer.startDrawing();
//#endif
//#if MC == 12101 || MC == 12104
//$$        chunks.forEach(chunkEntry -> {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        boolean startedDrawing = false;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        for (ChunkEntry chunkEntry : chunks) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            if (!chunkEntry.future.isDone()) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                continue;
//#elseif MC == 12101 || MC == 12104
//$$                return;
//#endif
            }
//#if MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12111
//$$        GpuBufferSlice[] chunkSections = RenderSystem.getDynamicUniforms().writeChunkSections(chunkSectionInfos.toArray(DynamicUniforms.ChunkSectionsValue[]::new));
//#endif
//#if MC == 12111
//$$        return new SectionRenderState(atlasView, drawsPerLayer, maxIndicesRequired, chunkSections);
//#endif
//#if MC == 12111
//$$    }
//#endif
//#if MC == 12111
//$$
//#endif
//#if MC == 12111
//$$    /**
//$$     * 提交一个渲染层组（原版 SectionRenderState.renderSection 会指向主帧缓冲，
//$$     * 这里必须自写 pass 指向预览自己的帧缓冲）。
//$$     */
//#endif
//#if MC == 12111
//$$    private void renderChunkSectionsLayer(SectionRenderState sections, BlockRenderLayerGroup group) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS);
//#endif
//#if MC == 12111
//$$        GpuBuffer indexBuffer = sections.maxIndicesRequired() == 0 ? null : shapeIndexBuffer.getIndexBuffer(sections.maxIndicesRequired());
//#endif
//#if MC == 12111
//$$        VertexFormat.IndexType indexType = sections.maxIndicesRequired() == 0 ? null : shapeIndexBuffer.getIndexType();
//#endif
//#if MC == 12108 || MC == 12110
        GpuBuffer gpuBuffer = maxIndicesRequired == 0 ? null : shapeIndexBuffer.getIndexBuffer(maxIndicesRequired);
//#endif
//#if MC == 12108 || MC == 12110
        VertexFormat.IndexType indexType = maxIndicesRequired == 0 ? null : shapeIndexBuffer.getIndexType();
//#endif
//#if MC == 12108 || MC == 12110

//#endif
//#if MC == 12108 || MC == 12110
        GpuBufferSlice[] dynamicTransformsGpuBuffers = RenderSystem.getDynamicUniforms().writeAll(dynamicTransforms.toArray(DynamicUniforms.UniformValue[]::new));
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        try (RenderPass renderPass = RenderSystem.getDevice()
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                .createCommandEncoder()
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                .createRenderPass(
//#endif
//#if MC == 12108 || MC == 12110
                        () -> "SchematicPreview",
//#elseif MC == 12111
//$$                        () -> "SchematicPreview " + group.getName(),
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                        target.getColorAttachmentView(),
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                        OptionalInt.empty(),
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                        target.getDepthAttachmentView(),
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                        OptionalDouble.empty()
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                )) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            RenderSystem.bindDefaultUniforms(renderPass);
//#endif
//#if MC == 12108 || MC == 12110
            renderPass.bindSampler("Sampler2", MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().getGlTextureView());
//#elseif MC == 12111
//$$            renderPass.bindTexture("Sampler2", mc.gameRenderer.getLightmapTextureManager().getGlTextureView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110
            for (BlockRenderLayer layer : BLOCK_RENDER_LAYER_ORDER) {
//#elseif MC == 12111
//$$            for (BlockRenderLayer layer : group.getLayers()) {
//#endif
//#if MC == 12108 || MC == 12110
                List<RenderPass.RenderObject<GpuBufferSlice[]>> draws = drawsPerLayer.get(layer);
//#elseif MC == 12111
//$$                List<RenderPass.RenderObject<GpuBufferSlice[]>> draws = sections.drawsPerLayer().get(layer);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                if (!draws.isEmpty()) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    if (layer == BlockRenderLayer.TRANSLUCENT) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                        draws = draws.reversed();
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    renderPass.setPipeline(layer.getPipeline());
//#endif
//#if MC == 12108 || MC == 12110
                    renderPass.bindSampler("Sampler0", layer.getTextureView());
//#elseif MC == 12111
//$$                    renderPass.bindTexture("Sampler0", sections.textureView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
//#endif
//#if MC == 12108 || MC == 12110
                    renderPass.drawMultipleIndexed(draws, gpuBuffer, indexType, List.of("DynamicTransforms"), dynamicTransformsGpuBuffers);
//#elseif MC == 12111
//$$                    renderPass.drawMultipleIndexed(draws, indexBuffer, indexType, List.of("ChunkSection"), sections.chunkSectionInfos());
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            BuiltChunk chunk = chunkEntry.future.join();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            if (chunk == null) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                continue;
//#elseif MC == 12101 || MC == 12104
//$$                return;
//#endif
            }
//#if MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            chunk.uploadBufferIfNot(layer);
//#elseif MC == 12101 || MC == 12104
//$$            boolean uploaded = chunk.uploadBufferIfNot(layer);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            VertexBuffer buffer = chunk.buffers.get(layer);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            if (buffer == null) {
//#endif
//#if MC == 12101 || MC == 12104
//$$                return;
//#endif
//#if MC == 12101 || MC == 12104
//$$            }
//#endif
//#if MC == 12101 || MC == 12104
//$$            if (updated || uploaded) {
//#endif
//#if MC == 12101 || MC == 12104
//$$                chunk.resortTransparent(
//#endif
//#if MC == 12101 || MC == 12104
//$$                        layer,
//#endif
//#if MC == 12101 || MC == 12104
//$$                        VertexSorter.byDistance(pos.x - chunkEntry.pos.getStartX(), pos.y, pos.z - chunkEntry.pos.getStartZ())
//#endif
//#if MC == 12101 || MC == 12104
//$$                );
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                continue;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            }
//#endif
//#if MC == 11701
//$$            Shader shader = RenderSystem.getShader();
//#elseif MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            ShaderProgram shader = RenderSystem.getShader();
//#endif
//#if MC == 11701
//$$            // 1.17.1：chunkOffset uniform 必须在该 (chunk) 程序使用时上传
//#elseif MC == 11904 || MC == 12001
//$$            // MC 1.20.1: the chunk offset uniform must be uploaded while the (chunk) program is in use
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            shader.bind();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$            if (shader.chunkOffset != null) {
//#elseif MC == 12104
//$$            if (shader.modelOffset != null) {
//#endif
//#if MC == 11701
//$$                shader.chunkOffset.set(chunkEntry.pos.getStartX() - pos.getX(), -pos.getY(), chunkEntry.pos.getStartZ() - pos.getZ());
//#elseif MC == 11904 || MC == 12001
//$$                shader.chunkOffset.set(chunkEntry.pos.getStartX() - pos.x, -pos.y, chunkEntry.pos.getStartZ() - pos.z);
//#elseif MC == 12101
//$$                shader.chunkOffset.set(chunkEntry.pos.getStartX() -pos.x, -pos.y, chunkEntry.pos.getStartZ() - pos.z);
//#elseif MC == 12104
//$$                shader.modelOffset.set(chunkEntry.pos.getStartX() -pos.x, -pos.y, chunkEntry.pos.getStartZ() - pos.z);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$                shader.chunkOffset.upload();
//#elseif MC == 12104
//$$                shader.modelOffset.upload();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            }
//#endif
//#if MC == 12101 || MC == 12104
//$$            buffer.bind();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            target.beginWrite(true);
//#endif
//#if MC == 11701
//$$            // 1.17.1：VertexBuffer 不会自行绑定顶点数组，需调用方绑定
//#elseif MC == 11904 || MC == 12001
//$$            // MC 1.20.1: VertexBuffer.draw(...) does not bind its own vertex array, the caller has to
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            buffer.bind();
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            buffer.draw(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), shader);
//#endif
//#if MC == 11701
//$$            buffer.setShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), shader);
//#endif
//#if MC == 11701
//$$            buffer.drawVertices();
//#endif
//#if MC == 12101 || MC == 12104
//$$            shader.bind();
//#endif
//#if MC == 12101
//$$            if (shader.chunkOffset != null) {
//#elseif MC == 12104
//$$            if (shader.modelOffset != null) {
//#endif
//#if MC == 12101
//$$                shader.chunkOffset.set(0.0F, 0.0F, 0.0F);
//#elseif MC == 12104
//$$                shader.modelOffset.set(0.0F, 0.0F, 0.0F);
//#endif
//#if MC == 12101
//$$                shader.chunkOffset.upload();
//#elseif MC == 12104
//$$                shader.modelOffset.upload();
//#endif
//#if MC == 12101 || MC == 12104
//$$            }
//#endif
//#if MC == 12101 || MC == 12104
//$$        });
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            VertexBuffer.unbind();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            startedDrawing = true;
//#endif
//#if MC == 11904 || MC == 12001
//$$        }
//#endif
//#if MC == 11904 || MC == 12001
//$$        if (startedDrawing) {
//#endif
//#if MC == 11904 || MC == 12001
//$$            layer.getVertexFormat().clearState();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        VertexBuffer.unbind();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$        layer.endDrawing();
//#endif
//#if MC == 12111
//$$    }
//#endif
//#if MC == 12111
//$$
//#endif
//#if MC == 12111
//$$    public void renderBlocks() {
//#endif
//#if MC == 12111
//$$        SectionRenderState sections = prepareChunks();
//#endif
//#if MC == 12111
//$$        renderChunkSectionsLayer(sections, BlockRenderLayerGroup.OPAQUE);
//#endif
//#if MC == 12111
//$$        renderChunkSectionsLayer(sections, BlockRenderLayerGroup.TRANSLUCENT);
//#endif
//#if MC == 12111
//$$        renderChunkSectionsLayer(sections, BlockRenderLayerGroup.TRIPWIRE);
//#endif
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
//#if MC == 12108 || MC == 12110 || MC == 12111
    public void renderBlockEntities(MatrixStack stack, float tickDelta) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$    public void renderBlockEntities(MatrixStack stack, VertexConsumerProvider.Immediate vertexConsumers, float tickDelta) {
//#endif
        if (getBuiltChunksCount() != chunks.size()) {
            return;
        }
//#if MC == 12111
//$$        // 方块实体走原版实体顶点缓冲管线：用 outputColorTextureOverride 把所有即时绘制重定向到预览帧缓冲
//#endif
//#if MC == 12111
//$$        GpuTextureView previousColor = RenderSystem.outputColorTextureOverride;
//#endif
//#if MC == 12111
//$$        GpuTextureView previousDepth = RenderSystem.outputDepthTextureOverride;
//#endif
//#if MC == 12111
//$$        RenderSystem.outputColorTextureOverride = target.getColorAttachmentView();
//#endif
//#if MC == 12111
//$$        RenderSystem.outputDepthTextureOverride = target.getDepthAttachmentView();
//#endif
//#if MC == 12111
//$$        try {
//#endif
//#if MC == 12111
//$$            world.getBlockEntities().forEach((pos, blockEntitySupplier) -> {
//#endif
//#if MC == 12111
//$$                BlockEntity blockEntity = blockEntitySupplier.get();
//#endif
//#if MC == 12111
//$$                if (blockEntity != null) {
//#endif
//#if MC == 12111
//$$                    BlockEntityRenderer renderer = blockEntityRenderManager.get(blockEntity);
//#endif
//#if MC == 12111
//$$                    if (renderer != null) {
//#endif
//#if MC == 12111
//$$                        BlockEntityRenderState renderState = renderer.createRenderState();
//#endif
//#if MC == 12111
//$$                        stack.push();
//#endif
//#if MC == 12111
//$$                        stack.translate(pos.getX() - lastPos.x, pos.getY() - lastPos.y, pos.getZ() - lastPos.z);
//#endif
//#if MC == 12111
//$$                        try {
//#endif
//#if MC == 12111
//$$                            renderer.updateRenderState(blockEntity, renderState, tickDelta, cameraRenderState.pos, null);
//#endif
//#if MC == 12111
//$$                            renderer.render(renderState, stack, orderedRenderCommandQueue, cameraRenderState);
//#endif
//#if MC == 12111
//$$                        } catch (Exception e) {
//#endif
//#if MC == 12111
//$$                            LitematListMod.LOGGER.debug("Exception while rendering preview block entity", e);
//#endif
//#if MC == 12111
//$$                        }
//#endif
//#if MC == 12111
//$$                        stack.pop();
//#endif
//#if MC == 12108 || MC == 12110
        customVertexConsumerProvider.setFramebuffer(target);
//#endif
//#if MC == 12108
//$$        Vec3d cameraPos = new Vec3d(pos);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        target.beginWrite(true);
//#elseif MC == 12101 || MC == 12104
//$$        CustomVertexConsumerProvider customVertexConsumers = new CustomVertexConsumerProvider(vertexConsumers, target);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110
        world.getBlockEntities().forEach((pos, blockEntitySupplier) -> {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110
            BlockEntity blockEntity = blockEntitySupplier.get();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110
            if (blockEntity != null) {
//#endif
//#if MC == 12110
                BlockEntityRenderer renderer = blockEntityRenderManager.get(blockEntity);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                BlockEntityRenderer renderer = blockEntityRenderDispatcher.get(blockEntity);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110
                if (renderer != null) {
//#endif
//#if MC == 12110
                    BlockEntityRenderState renderState = renderer.createRenderState();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110
                    stack.push();
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110
                    stack.translate(pos.getX() - lastPos.x, pos.getY() - lastPos.y, pos.getZ() - lastPos.z);
//#elseif MC == 11701
//$$                    stack.translate(pos.getX() - lastPos.getX(), pos.getY() - lastPos.getY(), pos.getZ() - lastPos.getZ());
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110
                    try {
//#endif
//#if MC == 12110
                        renderer.updateRenderState(blockEntity, renderState, tickDelta, cameraRenderState.pos, null);
//#endif
//#if MC == 12110
                        renderer.render(renderState, stack, orderedRenderCommandQueue, cameraRenderState);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        renderer.render(blockEntity, tickDelta, stack, vertexConsumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);
//#elseif MC == 12101 || MC == 12104
//$$                        renderer.render(blockEntity, tickDelta, stack, customVertexConsumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);
//#elseif MC == 12108
//$$                        renderer.render(blockEntity, tickDelta, stack, customVertexConsumerProvider, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, cameraPos);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110
                    } catch (Exception e) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110
                        LitematListMod.LOGGER.debug("Exception while rendering preview block entity", e);
//#endif
                    }
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110
                    stack.pop();
//#endif
                }
//#if MC == 12111
//$$            });
//#endif
//#if MC == 12111
//$$            renderDispatcher.render();
//#endif
//#if MC == 12111
//$$            immediate.draw();
//#endif
//#if MC == 12111
//$$        } finally {
//#endif
//#if MC == 12111
//$$            RenderSystem.outputColorTextureOverride = previousColor;
//#endif
//#if MC == 12111
//$$            RenderSystem.outputDepthTextureOverride = previousDepth;
//#endif
//#if MC == 12111
//$$        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110
            }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110
        });
//#endif
//#if MC == 12110
        renderDispatcher.render();
//#endif
//#if MC == 12108 || MC == 12110
        customVertexConsumerProvider.draw();
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        vertexConsumers.draw();
//#elseif MC == 12101 || MC == 12104
//$$        customVertexConsumers.draw();
//#endif
    }

    public int getBuiltChunksCount() {
        return (int) chunks.stream().map(ChunkEntry::future).filter(CompletableFuture::isDone).count();
    }

    public boolean isBuildingTerrain() {
        return !chunks.isEmpty() && chunks.stream().map(ChunkEntry::future).noneMatch(CompletableFuture::isDone);
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$    public boolean isBuildDone() {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$        return chunks.stream().map(ChunkEntry::future).allMatch(CompletableFuture::isDone);
//#endif
    }

    @Override
    public void close() {
        canceled = true;
        chunks.stream().map(ChunkEntry::future).map(CompletableFuture::join).filter(Objects::nonNull).forEach(BuiltChunk::close);
        canceled = false;
        chunks.clear();
        lastPos.set(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);
        pos.set(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
        lastChunkPos = new ChunkPos(Integer.MIN_VALUE, Integer.MIN_VALUE);
//#if MC == 12110 || MC == 12111
        renderDispatcher.close();
//#endif
    }

    private record ChunkEntry(ChunkPos pos, CompletableFuture<@Nullable BuiltChunk> future) {}

    private record BuiltChunk(
//#if MC == 12108 || MC == 12110 || MC == 12111
            Map<BlockRenderLayer, BufferBuilder> builderCache,
//#elseif MC == 12101 || MC == 12104
//$$            Map<RenderLayer, BufferBuilder> builderCache,
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            Map<BlockRenderLayer, BufferAllocator> allocatorCache,
//#elseif MC == 12101 || MC == 12104
//$$            Map<RenderLayer, BufferAllocator> allocatorCache,
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            Map<BlockRenderLayer, BuiltBuffer> builtBuffers,
//#elseif MC == 12101 || MC == 12104
//$$            Map<RenderLayer, BuiltBuffer> builtBuffers,
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            Map<BlockRenderLayer, BuiltBuffer.SortState> sortStates,
//#elseif MC == 12101 || MC == 12104
//$$            Map<RenderLayer, BuiltBuffer.SortState> sortStates,
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            Map<RenderLayer, PreviewBufferBuilder> builderCache,
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            Map<BlockRenderLayer, Buffers> buffers
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            Map<RenderLayer, VertexBuffer> buffers
//#endif
    ) implements AutoCloseable {

        private BuiltChunk() {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            this(new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            this(new HashMap<>(), new HashMap<>());
//#endif
        }

//#if MC == 12108 || MC == 12110 || MC == 12111
        private void uploadBuffer(BlockRenderLayer layer, VertexSorter vertexSorter) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$        private boolean uploadBufferIfNot(RenderLayer layer) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            Buffers buffers = this.buffers.get(layer);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            VertexBuffer buffer = buffers.get(layer);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            if (buffers != null) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            if (buffer != null) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                return;
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                return false;
//#endif
            }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            BufferBuilder builder = builderCache.get(layer);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            PreviewBufferBuilder builder = builderCache.get(layer);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            if (builder == null || !builder.building) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            if (builder == null) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                return;
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                return false;
//#endif
            }
//#if MC == 12108 || MC == 12110 || MC == 12111
            BuiltBuffer built = builder.endNullable();
//#endif
//#if MC == 12101 || MC == 12104
//$$            BuiltBuffer built;
//#endif
//#if MC == 11904 || MC == 12001
//$$            if (layer == RenderLayer.getTranslucent()) {
//#endif
//#if MC == 12001
//$$                builder.setSorter(VertexSorter.byDistance(8, 8, 8));
//#endif
//#if MC == 11904
//$$                // MC 1.19.4 sorts translucent quads while the buffer is built, the sorter needs the
//#endif
//#if MC == 11904
//$$                // camera position at that point (relative to the chunk origin)
//#endif
//#if MC == 11904
//$$                builder.sortFrom(8, 8, 8);
//#endif
//#if MC == 11904 || MC == 12001
//$$            }
//#endif
//#if MC == 11904 || MC == 12001
//$$            BufferBuilder.BuiltBuffer built;
//#endif
//#if MC == 11701
//$$            // 1.17.1：BufferBuilder.end() 后由 VertexBuffer.upload(BufferBuilder) 内部 popData 上传
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            try {
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                built = builder.endNullable();
//#endif
//#if MC == 11701
//$$                builder.end();
//#endif
//#if MC == 11701
//$$                buffer = new VertexBuffer();
//#endif
//#if MC == 11701
//$$                buffer.bind();
//#endif
//#if MC == 11701
//$$                buffer.upload(builder);
//#endif
//#if MC == 11701
//$$                VertexBuffer.unbind();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            } catch (Exception e) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                return false;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            }
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (built == null) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                return;
//#elseif MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                return false;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            builtBuffers.put(layer, built);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            if (layer == BlockRenderLayer.TRANSLUCENT) {
//#elseif MC == 12101 || MC == 12104
//$$            if (layer == RenderLayer.getTranslucent()) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                BuiltBuffer.SortState sortState = built.sortQuads(allocatorCache.get(layer), vertexSorter);
//#elseif MC == 12101 || MC == 12104
//$$                BuiltBuffer.SortState sortState = built.sortQuads(allocatorCache.get(layer), VertexSorter.BY_DISTANCE);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (sortState != null) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    sortStates.put(layer, sortState);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            GpuBuffer vertexBuffer = RenderSystem.getDevice().createBuffer(
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    () -> "SchematicPreview vertex buffer",
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    built.getBuffer()
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            );
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            GpuBuffer indexBuffer = built.getSortedBuffer() != null
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    ? RenderSystem.getDevice().createBuffer(
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    () -> "SchematicPreview index buffer",
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST,
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    built.getSortedBuffer()
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            )
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    : null;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            buffers = new Buffers(vertexBuffer, indexBuffer, built.getDrawParameters().indexCount(), built.getDrawParameters().indexType());
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            this.buffers.put(layer, buffers);
//#endif
//#if MC == 12001 || MC == 12101
//$$            buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
//#elseif MC == 12104
//$$            buffer = new VertexBuffer(GlUsage.STATIC_WRITE);
//#endif
//#if MC == 11904
//$$            // MC 1.19.4 has no VertexBuffer.Usage, static usage is implied
//#endif
//#if MC == 11904
//$$            buffer = new VertexBuffer();
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            buffer.bind();
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            buffer.upload(built);
//#endif
//#if MC == 11904 || MC == 12001
//$$            VertexBuffer.unbind();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            buffers.put(layer, buffer);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            return true;
//#endif
        }

//#if MC == 12108 || MC == 12110 || MC == 12111
        private void resortTransparent(BlockRenderLayer layer, VertexSorter vertexSorter) {
//#elseif MC == 12101 || MC == 12104
//$$        private void resortTransparent(RenderLayer layer, VertexSorter vertexSorter) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            BuiltBuffer.SortState sortState = sortStates.get(layer);
//#elseif MC == 12101 || MC == 12104
//$$            BuiltBuffer.SortState sortState = sortStates().get(layer);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (sortState != null) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                Buffers buffers = this.buffers.get(layer);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                if (buffers != null) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    GpuBuffer indexBuffer = buffers.getIndexBuffer();
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    if (indexBuffer != null && !indexBuffer.isClosed()) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                        BufferAllocator allocator = getAllocatorByLayer(layer);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                        try (BufferAllocator.CloseableBuffer result = sortState.sortAndStore(allocator, vertexSorter)) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                            if (result != null) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                                RenderSystem.getDevice().createCommandEncoder()
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                                        .writeToBuffer(indexBuffer.slice(), result.getBuffer());
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                            }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                        }
//#endif
//#if MC == 12101 || MC == 12104
//$$                VertexBuffer buffer = buffers().get(layer);
//#endif
//#if MC == 12101 || MC == 12104
//$$                if (buffer != null) {
//#endif
//#if MC == 12101 || MC == 12104
//$$                    BufferAllocator allocator = getAllocatorByLayer(layer);
//#endif
//#if MC == 12101 || MC == 12104
//$$                    BufferAllocator.CloseableBuffer result = sortState.sortAndStore(allocator, vertexSorter);
//#endif
//#if MC == 12101 || MC == 12104
//$$                    if (result != null) {
//#endif
//#if MC == 12101 || MC == 12104
//$$                        buffer.bind();
//#endif
//#if MC == 12101 || MC == 12104
//$$                        buffer.uploadIndexBuffer(result);
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
//#if MC == 12108 || MC == 12110 || MC == 12111
        private BufferBuilder getBuilderByLayer(BlockRenderLayer layer) {
//#elseif MC == 12101 || MC == 12104
//$$        private BufferBuilder getBuilderByLayer(RenderLayer layer) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            return builderCache.computeIfAbsent(layer, l -> new BufferBuilder(getAllocatorByLayer(l), l.getPipeline().getVertexFormatMode(), l.getPipeline().getVertexFormat()));
//#elseif MC == 12101 || MC == 12104
//$$            return builderCache.computeIfAbsent(layer, l -> new BufferBuilder(getAllocatorByLayer(l), l.getDrawMode(), l.getVertexFormat()));
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        private BufferAllocator getAllocatorByLayer(BlockRenderLayer layer) {
//#elseif MC == 12101 || MC == 12104
//$$        private BufferAllocator getAllocatorByLayer(RenderLayer layer) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            return allocatorCache.computeIfAbsent(layer, l -> new BufferAllocator(1536));
//#elseif MC == 12101 || MC == 12104
//$$            return allocatorCache.computeIfAbsent(layer, l -> new BufferAllocator(l.getExpectedBufferSize()));
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        private PreviewBufferBuilder getBuilderByLayer(RenderLayer layer) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            return builderCache.computeIfAbsent(layer, l -> {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                PreviewBufferBuilder builder = new PreviewBufferBuilder(l.getExpectedBufferSize());
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                builder.begin(l.getDrawMode(), l.getVertexFormat());
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                return builder;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            });
//#endif
        }

        @Override
        public void close() {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            allocatorCache.values().forEach(BufferAllocator::close);
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            builtBuffers.values().forEach(BuiltBuffer::close);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            buffers.values().forEach(Buffers::close);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            buffers.values().forEach(VertexBuffer::close);
//#endif
        }

    }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
}
//#endif
//#if MC == 11701
//$$    /**
//$$     * 1.17.1 的流体渲染器写入区块内相对 Y 坐标，而本预览按绝对原理图 Y 排布，补上缺失的 Y 偏移。
//$$     */
//#elseif MC == 11904 || MC == 12001
//$$    /**
//$$     * MC 1.20.1's fluid renderer writes section-relative Y coordinates, while this preview
//$$     * places everything at absolute schematic Y. This subclass adds the missing Y offset.
//$$     */
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    private static class PreviewBufferBuilder extends BufferBuilder {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110

//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        private double offsetY;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        private PreviewBufferBuilder(int initialCapacity) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            super(initialCapacity);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        private void setYOffset(double offsetY) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            this.offsetY = offsetY;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        @Override
//#endif
//#if MC == 11701
//$$        public net.minecraft.client.render.VertexConsumer vertex(double x, double y, double z) {
//#elseif MC == 11904 || MC == 12001
//$$        public VertexConsumer vertex(double x, double y, double z) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            return super.vertex(x, y + offsetY, z);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$}
//#endif
//#if MC == 11701
//$$
//#endif