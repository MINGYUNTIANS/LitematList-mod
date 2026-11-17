//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110
package com.litematlist.preview;

//#if MC == 12108 || MC == 12110
import com.mojang.blaze3d.buffers.GpuBuffer;
//#endif
//#if MC == 12108 || MC == 12110
import com.mojang.blaze3d.buffers.GpuBufferSlice;
//#endif
//#if MC == 12108 || MC == 12110
import com.mojang.blaze3d.systems.RenderPass;
//#endif
import com.mojang.blaze3d.systems.RenderSystem;
//#if MC == 12108 || MC == 12110
import com.mojang.blaze3d.textures.GpuTextureView;
//#endif
//#if MC == 12108 || MC == 12110
import com.mojang.blaze3d.vertex.VertexFormat;
//#endif
import net.minecraft.client.gl.Framebuffer;
//#if MC == 12108 || MC == 12110
import net.minecraft.client.gl.ScissorState;
//#endif
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
//#if MC == 12110
import org.joml.Vector3f;
//#endif
//#if MC == 12108 || MC == 12110
import org.joml.Vector4f;
//#endif
//#if MC == 12108 || MC == 12110

//#endif
//#if MC == 12108 || MC == 12110
import java.util.OptionalDouble;
//#endif
//#if MC == 12108 || MC == 12110
import java.util.OptionalInt;
//#endif

public class CustomVertexConsumerProvider extends VertexConsumerProvider.Immediate {

//#if MC == 12108 || MC == 12110
    private Framebuffer framebuffer;
//#elseif MC == 12101 || MC == 12104
//$$    private final Framebuffer framebuffer;
//#endif

//#if MC == 12108 || MC == 12110
    protected CustomVertexConsumerProvider(VertexConsumerProvider.Immediate delegate) {
//#elseif MC == 12101 || MC == 12104
//$$    protected CustomVertexConsumerProvider(VertexConsumerProvider.Immediate delegate, Framebuffer framebuffer) {
//#endif
        super(delegate.allocator, delegate.layerBuffers);
//#if MC == 12108 || MC == 12110
    }
//#endif
//#if MC == 12108 || MC == 12110

//#endif
//#if MC == 12108 || MC == 12110
    public void setFramebuffer(Framebuffer framebuffer) {
//#endif
        this.framebuffer = framebuffer;
    }

    @Override
//#if MC == 12110
    protected void draw(RenderLayer layer, BufferBuilder builder) {
//#elseif MC == 12108
//$$    protected void draw(RenderLayer renderLayer, BufferBuilder builder) {
//#endif
//#if MC == 12108 || MC == 12110
        if (framebuffer == null) {
//#endif
//#if MC == 12108 || MC == 12110
            return;
//#endif
//#if MC == 12108 || MC == 12110
        }
//#endif
//#if MC == 12108 || MC == 12110
        BuiltBuffer buffer = builder.endNullable();
//#endif
//#if MC == 12108 || MC == 12110
        if (buffer != null) {
//#endif
//#if MC == 12110
            if (layer.isTranslucent()) {
//#endif
//#if MC == 12110
                BufferAllocator bufferAllocator = layerBuffers.getOrDefault(layer, this.allocator);
//#endif
//#if MC == 12110
                buffer.sortQuads(bufferAllocator, RenderSystem.getProjectionType().getVertexSorter());
//#endif
//#if MC == 12110
            }
//#endif
//#if MC == 12110

//#endif
//#if MC == 12110
            layer.startDrawing();
//#endif
//#if MC == 12110
            GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
//#endif
//#if MC == 12110
                    .write(
//#endif
//#if MC == 12110
                            RenderSystem.getModelViewMatrix(),
//#endif
//#if MC == 12110
                            new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
//#endif
//#if MC == 12110
                            new Vector3f(),
//#endif
//#if MC == 12110
                            RenderSystem.getTextureMatrix(),
//#endif
//#if MC == 12110
                            RenderSystem.getShaderLineWidth()
//#endif
//#if MC == 12110
                    );
//#endif
//#if MC == 12110
            try {
//#endif
//#if MC == 12110
                GpuBuffer gpuBuffer = layer.getRenderPipeline().getVertexFormat().uploadImmediateVertexBuffer(buffer.getBuffer());
//#endif
//#if MC == 12110
                GpuBuffer gpuBuffer2;
//#endif
//#if MC == 12110
                VertexFormat.IndexType indexType;
//#endif
//#if MC == 12110
                if (buffer.getSortedBuffer() == null) {
//#endif
//#if MC == 12110
                    RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(buffer.getDrawParameters().mode());
//#endif
//#if MC == 12110
                    gpuBuffer2 = shapeIndexBuffer.getIndexBuffer(buffer.getDrawParameters().indexCount());
//#endif
//#if MC == 12110
                    indexType = shapeIndexBuffer.getIndexType();
//#endif
//#if MC == 12110
                } else {
//#endif
//#if MC == 12110
                    gpuBuffer2 = layer.getRenderPipeline().getVertexFormat().uploadImmediateIndexBuffer(buffer.getSortedBuffer());
//#endif
//#if MC == 12110
                    indexType = buffer.getDrawParameters().indexType();
//#endif
//#if MC == 12108
//$$            if (renderLayer instanceof RenderLayer.MultiPhase layer) {
//#endif
//#if MC == 12108
//$$                if (layer.isTranslucent()) {
//#endif
//#if MC == 12108
//$$                    BufferAllocator bufferAllocator = layerBuffers.getOrDefault(layer, this.allocator);
//#endif
//#if MC == 12108
//$$                    buffer.sortQuads(bufferAllocator, RenderSystem.getProjectionType().getVertexSorter());
//#endif
//#if MC == 12108 || MC == 12110
                }
//#endif
//#if MC == 12108 || MC == 12110

//#endif
//#if MC == 12110
                GpuTextureView gpuTextureView = RenderSystem.outputColorTextureOverride != null
//#endif
//#if MC == 12110
                        ? RenderSystem.outputColorTextureOverride
//#endif
//#if MC == 12110
                        : framebuffer.getColorAttachmentView();
//#endif
//#if MC == 12110
                GpuTextureView gpuTextureView2 = framebuffer.useDepthAttachment
//#endif
//#if MC == 12110
                        ? (RenderSystem.outputDepthTextureOverride != null ? RenderSystem.outputDepthTextureOverride : framebuffer.getDepthAttachmentView())
//#endif
//#if MC == 12110
                        : null;
//#endif
//#if MC == 12108
//$$                layer.startDrawing();
//#endif
//#if MC == 12108
//$$                GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
//#endif
//#if MC == 12108
//$$                        .write(
//#endif
//#if MC == 12108
//$$                                RenderSystem.getModelViewMatrix(),
//#endif
//#if MC == 12108
//$$                                new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
//#endif
//#if MC == 12108
//$$                                RenderSystem.getModelOffset(),
//#endif
//#if MC == 12108
//$$                                RenderSystem.getTextureMatrix(),
//#endif
//#if MC == 12108
//$$                                RenderSystem.getShaderLineWidth()
//#endif
//#if MC == 12108
//$$                        );
//#endif
//#if MC == 12108 || MC == 12110

//#endif
//#if MC == 12110
                try (RenderPass renderPass = RenderSystem.getDevice()
//#endif
//#if MC == 12110
                        .createCommandEncoder()
//#endif
//#if MC == 12110
                        .createRenderPass(() -> "Immediate draw for SchematicPreview", gpuTextureView, OptionalInt.empty(), gpuTextureView2, OptionalDouble.empty())) {
//#endif
//#if MC == 12110
                    renderPass.setPipeline(layer.getRenderPipeline());
//#endif
//#if MC == 12110
                    ScissorState scissorState = RenderSystem.getScissorStateForRenderTypeDraws();
//#endif
//#if MC == 12110
                    if (scissorState.isEnabled()) {
//#endif
//#if MC == 12110
                        renderPass.enableScissor(scissorState.getX(), scissorState.getY(), scissorState.getWidth(), scissorState.getHeight());
//#endif
//#if MC == 12108
//$$                try {
//#endif
//#if MC == 12108
//$$                    GpuBuffer gpuBuffer = layer.getVertexFormat().uploadImmediateVertexBuffer(buffer.getBuffer());
//#endif
//#if MC == 12108
//$$                    GpuBuffer gpuBuffer2;
//#endif
//#if MC == 12108
//$$                    VertexFormat.IndexType indexType;
//#endif
//#if MC == 12108
//$$                    if (buffer.getSortedBuffer() == null) {
//#endif
//#if MC == 12108
//$$                        RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(buffer.getDrawParameters().mode());
//#endif
//#if MC == 12108
//$$                        gpuBuffer2 = shapeIndexBuffer.getIndexBuffer(buffer.getDrawParameters().indexCount());
//#endif
//#if MC == 12108
//$$                        indexType = shapeIndexBuffer.getIndexType();
//#endif
//#if MC == 12108
//$$                    } else {
//#endif
//#if MC == 12108
//$$                        gpuBuffer2 = layer.getVertexFormat().uploadImmediateIndexBuffer(buffer.getSortedBuffer());
//#endif
//#if MC == 12108
//$$                        indexType = buffer.getDrawParameters().indexType();
//#endif
//#if MC == 12108 || MC == 12110
                    }
//#endif
//#if MC == 12108 || MC == 12110

//#endif
//#if MC == 12110
                    RenderSystem.bindDefaultUniforms(renderPass);
//#endif
//#if MC == 12110
                    renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
//#endif
//#if MC == 12110
                    renderPass.setVertexBuffer(0, gpuBuffer);
//#endif
//#if MC == 12108
//$$                    GpuTextureView gpuTextureView = RenderSystem.outputColorTextureOverride != null
//#endif
//#if MC == 12108
//$$                            ? RenderSystem.outputColorTextureOverride
//#endif
//#if MC == 12108
//$$                            : framebuffer.getColorAttachmentView();
//#endif
//#if MC == 12108
//$$                    GpuTextureView gpuTextureView2 = framebuffer.useDepthAttachment
//#endif
//#if MC == 12108
//$$                            ? (RenderSystem.outputDepthTextureOverride != null ? RenderSystem.outputDepthTextureOverride : framebuffer.getDepthAttachmentView())
//#endif
//#if MC == 12108
//$$                            : null;
//#endif
//#if MC == 12108 || MC == 12110

//#endif
//#if MC == 12110
                    for (int i = 0; i < 12; i++) {
//#endif
//#if MC == 12110
                        GpuTextureView gpuTextureView3 = RenderSystem.getShaderTexture(i);
//#endif
//#if MC == 12110
                        if (gpuTextureView3 != null) {
//#endif
//#if MC == 12110
                            renderPass.bindSampler("Sampler" + i, gpuTextureView3);
//#endif
//#if MC == 12108
//$$                    try (RenderPass renderPass = RenderSystem.getDevice()
//#endif
//#if MC == 12108
//$$                            .createCommandEncoder()
//#endif
//#if MC == 12108
//$$                            .createRenderPass(() -> "Immediate draw for SchematicPreview", gpuTextureView, OptionalInt.empty(), gpuTextureView2, OptionalDouble.empty())) {
//#endif
//#if MC == 12108
//$$                        renderPass.setPipeline(layer.pipeline);
//#endif
//#if MC == 12108
//$$                        ScissorState scissorState = RenderSystem.getScissorStateForRenderTypeDraws();
//#endif
//#if MC == 12108
//$$                        if (scissorState.method_72091()) {
//#endif
//#if MC == 12108
//$$                            renderPass.enableScissor(scissorState.method_72092(), scissorState.method_72093(), scissorState.method_72094(), scissorState.method_72095());
//#endif
//#if MC == 12108 || MC == 12110
                        }
//#endif
//#if MC == 12108
//$$
//#endif
//#if MC == 12108
//$$                        RenderSystem.bindDefaultUniforms(renderPass);
//#endif
//#if MC == 12108
//$$                        renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
//#endif
//#if MC == 12108
//$$                        renderPass.setVertexBuffer(0, gpuBuffer);
//#endif
//#if MC == 12108
//$$
//#endif
//#if MC == 12108
//$$                        for (int i = 0; i < 12; i++) {
//#endif
//#if MC == 12108
//$$                            GpuTextureView gpuTextureView3 = RenderSystem.getShaderTexture(i);
//#endif
//#if MC == 12108
//$$                            if (gpuTextureView3 != null) {
//#endif
//#if MC == 12108
//$$                                renderPass.bindSampler("Sampler" + i, gpuTextureView3);
//#endif
//#if MC == 12108
//$$                            }
//#endif
//#if MC == 12108
//$$                        }
//#endif
//#if MC == 12108
//$$
//#endif
//#if MC == 12108
//$$                        renderPass.setIndexBuffer(gpuBuffer2, indexType);
//#endif
//#if MC == 12108
//$$                        renderPass.drawIndexed(0, 0, buffer.getDrawParameters().indexCount(), 1);
//#endif
//#if MC == 12108
//$$                    }
//#endif
//#if MC == 12108
//$$                } catch (Throwable var17) {
//#endif
//#if MC == 12108
//$$                    try {
//#endif
//#if MC == 12108
//$$                        buffer.close();
//#endif
//#if MC == 12108
//$$                    } catch (Throwable var14) {
//#endif
//#if MC == 12108
//$$                        var17.addSuppressed(var14);
//#endif
//#if MC == 12108 || MC == 12110
                    }
//#endif
//#if MC == 12108 || MC == 12110

//#endif
//#if MC == 12110
                    renderPass.setIndexBuffer(gpuBuffer2, indexType);
//#endif
//#if MC == 12110
                    renderPass.drawIndexed(0, 0, buffer.getDrawParameters().indexCount(), 1);
//#endif
//#if MC == 12110
                }
//#endif
//#if MC == 12110
            } catch (Throwable var17) {
//#endif
//#if MC == 12110
                try {
//#endif
//#if MC == 12110
                    buffer.close();
//#endif
//#if MC == 12110
                } catch (Throwable var14) {
//#endif
//#if MC == 12110
                    var17.addSuppressed(var14);
//#endif
//#if MC == 12108
//$$                    throw var17;
//#endif
//#if MC == 12108 || MC == 12110
                }
//#endif
//#if MC == 12108 || MC == 12110

//#endif
//#if MC == 12110
                throw var17;
//#endif
//#if MC == 12110
            }
//#endif
//#if MC == 12108
//$$                buffer.close();
//#endif
//#if MC == 12108 || MC == 12110

//#endif
//#if MC == 12110
            buffer.close();
//#endif
//#if MC == 12110

//#endif
//#if MC == 12110
            layer.endDrawing();
//#endif
//#if MC == 12108
//$$                layer.endDrawing();
//#endif
//#if MC == 12108
//$$            } else {
//#endif
//#if MC == 12108
//$$                buffer.close();
//#endif
//#if MC == 12101 || MC == 12104
//$$    protected void draw(RenderLayer layer, BufferBuilder builder) {
//#endif
//#if MC == 12101 || MC == 12104
//$$        BuiltBuffer builtBuffer = builder.endNullable();
//#endif
//#if MC == 12101 || MC == 12104
//$$        if (builtBuffer != null) {
//#endif
//#if MC == 12101 || MC == 12104
//$$            if (layer.isTranslucent()) {
//#endif
//#if MC == 12101 || MC == 12104
//$$                BufferAllocator bufferAllocator = layerBuffers.getOrDefault(layer, this.allocator);
//#endif
//#if MC == 12101
//$$                builtBuffer.sortQuads(bufferAllocator, RenderSystem.getVertexSorting());
//#elseif MC == 12104
//$$                builtBuffer.sortQuads(bufferAllocator, RenderSystem.getProjectionType().getVertexSorter());
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$            }
//#endif
//#if MC == 12101 || MC == 12104
//$$
//#endif
//#if MC == 12101 || MC == 12104
//$$            layer.startDrawing();
//#endif
//#if MC == 12101 || MC == 12104
//$$            framebuffer.beginWrite(true);
//#endif
//#if MC == 12101 || MC == 12104
//$$            BufferRenderer.drawWithGlobalProgram(builtBuffer);
//#endif
//#if MC == 12101 || MC == 12104
//$$            layer.endDrawing();
//#endif
        }

//#if MC == 12101 || MC == 12104 || MC == 12110
        if (layer.equals(currentLayer)) {
//#elseif MC == 12108
//$$        if (renderLayer.equals(currentLayer)) {
//#endif
            currentLayer = null;
        }
    }

}

//#endif