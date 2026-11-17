package com.litematlist.preview;

import com.litematlist.LitematListMod;
import com.litematlist.LitematicaBridge.LayerFilter;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import fi.dy.masa.litematica.render.schematic.BlockModelRendererSchematic;
import fi.dy.masa.litematica.render.schematic.IBlockOutputSchematic;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DynamicGpuData;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.Optional;
import java.util.OptionalDouble;

public class SchematicPreviewRenderer implements AutoCloseable {

    private final WorldSchematicWrapper world;
    private final FluidRenderer fluidRenderer;
    private final ModelManager modelManager;
    private final BlockEntityRenderDispatcher blockEntityRenderManager;
    private final SubmitNodeStorage submitNodeStorage;
    private final FeatureRenderDispatcher renderDispatcher;
    private final List<ChunkEntry> chunks = new ArrayList<>();
    private final Vector3f pos = new Vector3f(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);
    private ChunkPos chunkPos = new ChunkPos(Integer.MIN_VALUE, Integer.MIN_VALUE);
    private CameraRenderState cameraRenderState;
    private boolean updated;
    private boolean canceled;
    private RenderTarget target;
    private GpuSampler textureSampler;

    public SchematicPreviewRenderer(Minecraft mc) {
        world = new WorldSchematicWrapper(mc);
        modelManager = mc.getModelManager();
        fluidRenderer = new FluidRenderer(modelManager.getFluidStateModelSet());
        blockEntityRenderManager = mc.getBlockEntityRenderDispatcher();
        submitNodeStorage = new SubmitNodeStorage();
        renderDispatcher = mc.gameRenderer.featureRenderDispatcher();
    }

    public void setup(LitematicaSchematic schematic, @Nullable LayerFilter layerFilter) {
        close();
        world.setSchematic(schematic, layerFilter);

        int height = world.getSize().getY();
        int chunksX = world.getSize().getX() >>> 4;
        int chunksZ = world.getSize().getZ() >>> 4;

        for (int chunkX = 0; chunkX <= chunksX; chunkX++) {
            for (int chunkZ = 0; chunkZ <= chunksZ; chunkZ++) {
                ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                chunks.add(new ChunkEntry(chunkPos, CompletableFuture.supplyAsync(() -> {
                    RandomSource random = new SingleThreadedRandomSource(0);
                    BuiltChunk chunk = new BuiltChunk();
                    BlockModelRendererSchematic blockModelRenderer = new BlockModelRendererSchematic();
                    blockModelRenderer.enableCache();
                    BlockPos.MutableBlockPos worldPos = new BlockPos.MutableBlockPos();
                    int chunkStartX = chunkPos.getMinBlockX();
                    int chunkStartZ = chunkPos.getMinBlockZ();
                    int chunkEndX = chunkStartX + 16;
                    int chunkEndZ = chunkStartZ + 16;

                    IBlockOutputSchematic blockOutput = (bx, by, bz, quad, inst) -> {
                        ChunkSectionLayer layer = quad.materialInfo().layer();
                        BufferBuilder builder = chunk.getBuilderByLayer(layer);
                        builder.putBlockBakedQuad(bx, by, bz, quad, inst);
                    };

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
                                boolean renderBlock = state.getRenderShape() == RenderShape.MODEL;
                                Vec3 offset = new Vec3(x & 0xF, y, z & 0xF);

                                if (renderFluid) {
                                    FluidRenderer.Output fluidOutput = chunk::getBuilderByLayer;
                                    fluidRenderer.tesselate(world, worldPos, fluidOutput, state, fluid);
                                }
                                if (renderBlock) {
                                    BlockStateModel model = modelManager.getBlockStateModelSet().get(state);
                                    blockModelRenderer.tessellateBlock(
                                            world,
                                            state,
                                            worldPos,
                                            offset,
                                            model,
                                            state.getSeed(worldPos),
                                            blockOutput
                                    );
                                }
                            }
                        }
                    }
                    blockModelRenderer.disableCache();
                    return chunk;
                })));
            }
        }
    }

    public void prepareRender(CameraRenderState cameraRenderState, RenderTarget target) {
        this.cameraRenderState = cameraRenderState;
        this.target = target;
        updated = !pos.equals((float) cameraRenderState.pos.x, (float) cameraRenderState.pos.y, (float) cameraRenderState.pos.z);
        if (updated) {
            pos.set(cameraRenderState.pos.x, cameraRenderState.pos.y, cameraRenderState.pos.z);
            ChunkPos newChunkPos = new ChunkPos(Mth.floor(cameraRenderState.pos.x) >> 4, Mth.floor(cameraRenderState.pos.z) >> 4);
            if (!this.chunkPos.equals(newChunkPos)) {
                this.chunkPos = newChunkPos;
                chunks.sort(Comparator.comparingInt(chunk -> -(Math.abs(chunk.pos().x() - newChunkPos.x()) + Math.abs(chunk.pos().z() - newChunkPos.z()))));
            }
        }
    }

    private ChunkSectionsToRender prepareChunks(GpuTextureView blockAtlas) {
        Iterator<ChunkEntry> chunkIterator = chunks.iterator();
        EnumMap<ChunkSectionLayer, List<RenderPass.Draw<GpuBufferSlice[]>>> drawsPerLayer = new EnumMap<>(ChunkSectionLayer.class);
        int maxIndices = 0;

        for (ChunkSectionLayer chunkSectionLayer : ChunkSectionLayer.values()) {
            drawsPerLayer.put(chunkSectionLayer, new ArrayList<>());
        }

        List<DynamicGpuData.ChunkSectionInfo> chunkSectionInfos = new ArrayList<>();

        while (chunkIterator.hasNext()) {
            ChunkEntry chunk = chunkIterator.next();
            if (!chunk.future.isDone()) {
                continue;
            }
            BuiltChunk built = chunk.future.join();
            if (built == null) {
                continue;
            }
            VertexSorting vertexSorter = VertexSorting.byDistance(pos.x - chunk.pos().getMinBlockX(), pos.y, pos.z - chunk.pos().getMinBlockZ());
            int uniformIndex = -1;

            for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
                built.uploadBuffer(layer, vertexSorter);
                if (updated) {
                    built.resortTransparent(layer, vertexSorter);
                }
                SectionBuffers sectionBuffers = built.buffers.get(layer);
                if (sectionBuffers == null) {
                    continue;
                }
                if (uniformIndex == -1) {
                    uniformIndex = chunkSectionInfos.size();
                    chunkSectionInfos.add(new DynamicGpuData.ChunkSectionInfo(
                            chunk.pos().getMinBlockX(),
                            0,
                            chunk.pos().getMinBlockZ(),
                            1.0F
                    ));
                }

                GpuBuffer indexBuffer;
                IndexType indexType;
                if (sectionBuffers.indexBuffer() == null) {
                    if (sectionBuffers.indexCount() > maxIndices) {
                        maxIndices = sectionBuffers.indexCount();
                    }
                    indexBuffer = null;
                    indexType = null;
                } else {
                    indexBuffer = sectionBuffers.indexBuffer();
                    indexType = sectionBuffers.indexType();
                }

                int finalUniformIndex = uniformIndex;
                drawsPerLayer.get(layer)
                        .add(new RenderPass.Draw<>(
                                0,
                                sectionBuffers.vertexBuffer(),
                                indexBuffer,
                                indexType,
                                0,
                                sectionBuffers.indexCount(),
                                0,
                                (gpuBufferSlices, uniformUploader) -> uniformUploader.setUniform("ChunkSection", gpuBufferSlices[finalUniformIndex])
                        ));
            }
        }

        // Translucent chunks are sorted back to front, so the furthest one has to be drawn first
        drawsPerLayer.computeIfPresent(ChunkSectionLayer.TRANSLUCENT, (layer, draws) -> draws.reversed());

        GpuBufferSlice terrainTransform = RenderSystem.getDynamicUniforms().writeTerrainTransform(
                RenderSystem.getModelViewMatrixCopy(),
                blockAtlas.getWidth(0),
                blockAtlas.getHeight(0)
        );
        GpuBufferSlice[] gpuBufferSlices = RenderSystem.getDynamicUniforms().writeChunkSections(
                chunkSectionInfos.toArray(new DynamicGpuData.ChunkSectionInfo[0])
        );
        if (maxIndices != 0) {
            // ChunkSectionsToRender only reads the already-sized buffer, and the shared one is not
            // resized again between the level pass and the GUI pass, so grow it here
            RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS).getBuffer(maxIndices);
        }
        return new ChunkSectionsToRender.DrawSeparate(terrainTransform, drawsPerLayer, maxIndices, gpuBufferSlices);
    }

    public void renderBlocks() {
        if (target == null) {
            return;
        }
        if (textureSampler == null) {
            textureSampler = RenderSystem.getDevice()
                    .createSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, 1, OptionalDouble.empty());
        }
        GpuTextureView blockAtlas = Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();
        ChunkSectionsToRender chunkSectionsToRender = prepareChunks(blockAtlas);
        renderChunkSectionsLayer(chunkSectionsToRender, blockAtlas, ChunkSectionLayerGroup.OPAQUE);
        renderChunkSectionsLayer(chunkSectionsToRender, blockAtlas, ChunkSectionLayerGroup.TRANSLUCENT);
    }

    private void renderChunkSectionsLayer(ChunkSectionsToRender chunks, GpuTextureView blockAtlas, ChunkSectionLayerGroup group) {
        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "SchematicPreview " + group.label(),
                target.getColorTextureView(),
                Optional.empty(),
                target.getDepthTextureView(),
                OptionalDouble.empty()
        )) {
            RenderSystem.bindDefaultUniforms(renderPass);
            chunks.renderGroup(group, renderPass, textureSampler, blockAtlas, false);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void renderBlockEntities(PoseStack stack, float tickDelta) {
        if (target == null || getBuiltChunksCount() != chunks.size()) {
            return;
        }
        world.getBlockEntities().forEach((pos, blockEntitySupplier) -> {
            BlockEntity blockEntity = blockEntitySupplier.get();
            if (blockEntity != null) {
                BlockEntityRenderer renderer = blockEntityRenderManager.getRenderer(blockEntity);
                if (renderer != null) {
                    BlockEntityRenderState renderState = renderer.createRenderState();
                    stack.pushPose();
                    stack.translate(pos.getX() - this.pos.x, pos.getY() - this.pos.y, pos.getZ() - this.pos.z);
                    try {
                        renderer.extractRenderState(blockEntity, renderState, tickDelta, cameraRenderState.pos, null);
                        renderer.submit(renderState, stack, submitNodeStorage, cameraRenderState);
                    } catch (Exception e) {
                        LitematListMod.LOGGER.debug("Exception while rendering preview block entity", e);
                    }
                    stack.popPose();
                }
            }
        });
        try (
                FeatureRenderDispatcher.PreparedFrame frame = renderDispatcher.prepareFrame(submitNodeStorage);
                RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                        () -> "SchematicPreview block entities",
                        target.getColorTextureView(),
                        Optional.empty(),
                        target.getDepthTextureView(),
                        OptionalDouble.empty()
                )
        ) {
            RenderSystem.bindDefaultUniforms(renderPass);
            FeatureRenderDispatcher.renderAllFeatures(renderPass, frame);
        }
    }

    public int getBuiltChunksCount() {
        return (int) chunks.stream().map(ChunkEntry::future).filter(CompletableFuture::isDone).count();
    }

    public boolean isBuildingTerrain() {
        return !chunks.isEmpty() && chunks.stream().map(ChunkEntry::future).noneMatch(CompletableFuture::isDone);
    }

    @Override
    public void close() {
        canceled = true;
        chunks.stream().map(ChunkEntry::future).map(CompletableFuture::join).filter(Objects::nonNull).forEach(BuiltChunk::close);
        canceled = false;
        chunks.clear();
        pos.set(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
        chunkPos = new ChunkPos(Integer.MIN_VALUE, Integer.MIN_VALUE);
        if (textureSampler != null) {
            textureSampler.close();
            textureSampler = null;
        }
    }

    private record ChunkEntry(ChunkPos pos, CompletableFuture<@Nullable BuiltChunk> future) {}

    private record SectionBuffers(
            GpuBuffer vertexBuffer,
            GpuBuffer indexBuffer,
            int indexCount,
            IndexType indexType
    ) implements AutoCloseable {

        @Override
        public void close() {
            vertexBuffer.close();
            if (indexBuffer != null) {
                indexBuffer.close();
            }
        }
    }

    private record BuiltChunk(
            Map<ChunkSectionLayer, BufferBuilder> builderCache,
            Map<ChunkSectionLayer, ByteBufferBuilder> allocatorCache,
            Map<ChunkSectionLayer, MeshData> builtBuffers,
            Map<ChunkSectionLayer, MeshData.SortState> sortStates,
            Map<ChunkSectionLayer, SectionBuffers> buffers
    ) implements AutoCloseable {

        private BuiltChunk() {
            this(new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
        }

        private void uploadBuffer(ChunkSectionLayer layer, VertexSorting vertexSorter) {
            if (buffers.containsKey(layer)) {
                return;
            }
            BufferBuilder builder = builderCache.get(layer);
            if (builder == null) {
                return;
            }
            MeshData built = builder.build();
            if (built == null) {
                return;
            }
            builtBuffers.put(layer, built);
            if (layer == ChunkSectionLayer.TRANSLUCENT) {
                MeshData.SortState sortState = built.sortQuads(allocatorCache.get(layer), vertexSorter);
                if (sortState != null) {
                    sortStates.put(layer, sortState);
                }
            }
            GpuBuffer vertexBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "SchematicPreview vertex buffer",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                    built.vertexBuffer()
            );
            GpuBuffer indexBuffer = built.indexBuffer() != null
                    ? RenderSystem.getDevice().createBuffer(
                    () -> "SchematicPreview index buffer",
                    GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST,
                    built.indexBuffer()
            )
                    : null;
            buffers.put(layer, new SectionBuffers(vertexBuffer, indexBuffer, built.drawState().indexCount(), built.drawState().indexType()));
        }

        private void resortTransparent(ChunkSectionLayer layer, VertexSorting vertexSorter) {
            MeshData.SortState sortState = sortStates.get(layer);
            if (sortState == null) {
                return;
            }
            SectionBuffers sectionBuffers = buffers.get(layer);
            if (sectionBuffers == null || sectionBuffers.indexBuffer() == null || sectionBuffers.indexBuffer().isClosed()) {
                return;
            }
            ByteBufferBuilder allocator = getAllocatorByLayer(layer);
            try (ByteBufferBuilder.Result result = sortState.buildSortedIndexBuffer(allocator, vertexSorter)) {
                if (result != null) {
                    RenderSystem.getDevice().createCommandEncoder()
                            .writeToBuffer(sectionBuffers.indexBuffer().slice(), result.byteBuffer());
                }
            }
        }

        private BufferBuilder getBuilderByLayer(ChunkSectionLayer layer) {
            return builderCache.computeIfAbsent(layer, ignored -> new BufferBuilder(
                    getAllocatorByLayer(layer),
                    layer.pipeline(false).getPrimitiveTopology(),
                    layer.vertexFormat()
            ));
        }

        private ByteBufferBuilder getAllocatorByLayer(ChunkSectionLayer layer) {
            return allocatorCache.computeIfAbsent(layer, ignored -> new ByteBufferBuilder(1536));
        }

        @Override
        public void close() {
            allocatorCache.values().forEach(ByteBufferBuilder::close);
            builtBuffers.values().forEach(MeshData::close);
            buffers.values().forEach(SectionBuffers::close);
        }
    }

}