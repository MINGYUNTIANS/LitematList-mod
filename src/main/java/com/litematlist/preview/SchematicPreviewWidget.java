package com.litematlist.preview;

//#if MC == 11701 || MC == 11904 || MC == 12001
//$$import com.litematlist.LitematicaBridge;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$import com.litematlist.LitematicaBridge.LayerFilter;
//#endif
//#if MC == 12111
//$$import com.mojang.blaze3d.buffers.GpuBuffer;
//#endif
//#if MC == 12111
//$$import com.mojang.blaze3d.buffers.Std140Builder;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
import com.mojang.blaze3d.systems.ProjectionType;
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$import com.mojang.blaze3d.platform.GlStateManager;
//#endif
import com.mojang.blaze3d.systems.RenderSystem;
//#if MC == 12001 || MC == 12101
//$$import com.mojang.blaze3d.systems.VertexSorter;
//#elseif MC == 12104
//$$import com.mojang.blaze3d.systems.ProjectionType;
//#endif
//#if MC == 12111
//$$import com.mojang.blaze3d.textures.FilterMode;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import com.litematlist.LitematicaBridge;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import com.litematlist.LitematicaBridge.LayerFilter;
//#endif
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
//#if MC == 12111
//$$import net.minecraft.client.gl.GlobalSettings;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.gl.RenderPipelines;
//#elseif MC == 12104
//$$import net.minecraft.client.gl.ShaderProgramKeys;
//#endif
import net.minecraft.client.gl.SimpleFramebuffer;
//#if MC == 12110 || MC == 12111
import net.minecraft.client.gui.Click;
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.gui.DrawContext;
//#elseif MC == 11701 || MC == 11904
//$$import net.minecraft.client.gui.DrawableHelper;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.gui.render.state.TexturedQuadGuiElementRenderState;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$import net.minecraft.client.gui.screen.Screen;
//#endif
import net.minecraft.client.gui.widget.ClickableWidget;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.render.*;
//#endif
//#if MC == 11701
//$$import net.minecraft.client.render.RenderLayer;
//#endif
//#if MC == 11701
//$$import net.minecraft.client.render.VertexConsumerProvider;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$import net.minecraft.client.util.Window;
//#endif
//#if MC == 12110 || MC == 12111
import net.minecraft.client.render.state.CameraRenderState;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.texture.TextureSetup;
//#endif
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
//#if MC == 11701
//$$import net.minecraft.text.TranslatableText;
//#endif
//#if MC == 12110 || MC == 12111
import net.minecraft.util.math.BlockPos;
//#endif
import net.minecraft.util.math.MathHelper;
//#if MC == 11701
//$$import net.minecraft.util.math.Matrix4f;
//#endif
//#if MC == 11701
//$$import net.minecraft.util.math.Quaternion;
//#endif
//#if MC == 11701
//$$import net.minecraft.util.math.Vec3f;
//#endif
//#if MC == 12110 || MC == 12111
import net.minecraft.util.math.Vec3d;
//#endif
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.Nullable;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import org.joml.*;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$import org.lwjgl.opengl.GL11;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$import org.lwjgl.opengl.GL30;
//#endif
//#if MC == 12111
//$$import org.lwjgl.system.MemoryStack;
//#endif

//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$import java.io.File;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import java.lang.Math;
//#endif
//#if MC == 12111
//$$import java.nio.ByteBuffer;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
import java.nio.file.Path;
//#endif
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

// Extending malilib widget, but using vanilla inside
public class SchematicPreviewWidget extends WidgetBase {

    private final Renderer renderer = new Renderer(mc);
    private final Runnable tickAction = this::tick;
    private final List<ClickableWidget> buttons;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private final Vector3f rotOrigin = new Vector3f();
//#elseif MC == 11701
//$$    private final Vec3f rotOrigin = new Vec3f();
//#endif
    private final PreviewsCache cache;
    private final boolean nonStatic;
//#if MC == 12108 || MC == 12110 || MC == 12111
    private Path schematicFile;
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$    private File schematicFile;
//#endif
    private LitematicaSchematic lastSchematic;
    private Framebuffer framebuffer;
    private boolean mouseDragging;
    private int lastMouseX;
    private int lastMouseY;
    private boolean fullscreen;
    private boolean freecam;
    private float distance;
    private float yRot;
    private float xRot;
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$    private boolean scissor;
//#endif
    // 层级过滤（「同步投影渲染层」）：非 null 时只渲染原理图对应层级（切面自动可见），
    // 界面存续期间跟随渲染层规则变化自动重建
    private static final long LAYER_FILTER_DEBOUNCE_MS = 1000;
    // 按钮尺寸与图标纹理尺寸一致（22px）：绘制与点击判定均为 22x22
    private static final int BUTTON_SIZE = 22;
    @Nullable
    private final OnOffButton fullscreenButton;
    @Nullable
    private LayerFilter layerFilter;
    @Nullable
    private String lastAppliedLayerSignature;
    @Nullable
    private String pendingLayerSignature;
    private long pendingLayerSignatureTime;
    private boolean disposed;

    public SchematicPreviewWidget(PreviewsCache cache, boolean nonStatic, @Nullable LayerFilter layerFilter) {
        super(0, 0, 0, 0);
        this.cache = cache;
        this.nonStatic = nonStatic;
        this.layerFilter = layerFilter;
        this.lastAppliedLayerSignature = LitematicaBridge.getRenderLayerStateSignature();
        buttons = nonStatic
                ? List.of(new OnOffButton(
                        BUTTON_SIZE,
                        BUTTON_SIZE,
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        Text.translatable("litematlist.preview.fullscreen"),
//#elseif MC == 11701
//$$                        new TranslatableText("litematlist.preview.fullscreen"),
//#endif
                        this::toggleFullscreen,
                        PreviewManager.sprite("fullscreen_on"),
                        PreviewManager.sprite("fullscreen_on_focused"),
                        PreviewManager.sprite("fullscreen_off"),
                        PreviewManager.sprite("fullscreen_off_focused")
                ), new OnOffButton(
                        BUTTON_SIZE,
                        BUTTON_SIZE,
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        Text.translatable("litematlist.preview.freecam"),
//#elseif MC == 11701
//$$                        new TranslatableText("litematlist.preview.freecam"),
//#endif
                        this::toggleFreecam,
                        PreviewManager.sprite("freecam_on"),
                        PreviewManager.sprite("freecam_on_focused"),
                        PreviewManager.sprite("freecam_off"),
                        PreviewManager.sprite("freecam_off_focused")
                ))
                : List.of();
        fullscreenButton = nonStatic ? (OnOffButton) buttons.get(0) : null;
    }

//#if MC == 12108 || MC == 12110 || MC == 12111
    public void setSchematic(Path schematicFile) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$    public void setSchematic(File schematicFile) {
//#endif
        this.schematicFile = schematicFile;
    }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public void renderPreviewAndOverlay(DrawContext context, int x, int y, int width, int height) {
//#elseif MC == 11701 || MC == 11904
//$$    public void renderPreviewAndOverlay(MatrixStack matrixStack, int x, int y, int width, int height) {
//#endif
        PreviewManager.addTickable(tickAction);
        if (x != this.x || y != this.y || width != this.width || height != this.height) {
            setPosition(x, y);
            setWidth(width);
            setHeight(height);
            resized();
        }
        int centerX = x + (width >> 1);
        int centerY = y + ((height - 10) >> 1);
        CompletableFuture<LitematicaSchematic> loadingSchematic = cache.getSchematic(schematicFile);
        if (loadingSchematic.isDone()) {
            LitematicaSchematic schematic;
            if (!loadingSchematic.isCompletedExceptionally() && (schematic = loadingSchematic.join()) != null) {
                int maxVolume = PreviewOptions.PREVIEW_MAX_VOLUME;
                Vec3i size;
                if (nonStatic || maxVolume == 0 || (size = schematic.getTotalSize()).getX() * size.getY() * size.getZ() <= maxVolume) {
                    if (lastSchematic == null || !(lastSchematic == schematic || Objects.equals(lastSchematic.getFile(), schematic.getFile()))) {
                        lastSchematic = schematic;
                        renderer.newSchematic(schematic, layerFilter);
                        resetCameraPos();
                    }
                    if (!renderer.isBuildingTerrainOrStart()) {
//#if MC == 12108 || MC == 12110 || MC == 12111
                        renderPreviewAndOverlay(context, mc.getRenderTickCounter().getTickProgress(true));
//#elseif MC == 11701 || MC == 11904
//$$                        renderPreviewAndOverlay(matrixStack, mc.getTickDelta());
//#elseif MC == 12001
//$$                        renderPreviewAndOverlay(context, mc.getTickDelta());
//#elseif MC == 12101 || MC == 12104
//$$                        renderPreviewAndOverlay(context, mc.getRenderTickCounter().getTickDelta(true));
//#endif
                    } else {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        context.drawCenteredTextWithShadow(textRenderer, "Building terrain...", centerX, centerY, 0xFFBBBBBB);
//#elseif MC == 11701 || MC == 11904
//$$                        drawCenteredStringWithShadow(centerX, centerY, 0xFFBBBBBB, "Building terrain...", matrixStack);
//#endif
                    }
                } else {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    context.drawCenteredTextWithShadow(textRenderer, "Too big", centerX, centerY, 0xFFBBBBBB);
//#elseif MC == 11701 || MC == 11904
//$$                    drawCenteredStringWithShadow(centerX, centerY, 0xFFBBBBBB, "Too big", matrixStack);
//#endif
                }
            } else {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                context.drawCenteredTextWithShadow(textRenderer, "Preview load failed", centerX, centerY, 0xFFFF5555);
//#elseif MC == 11701 || MC == 11904
//$$                drawCenteredStringWithShadow(centerX, centerY, 0xFFFF5555, "Preview load failed", matrixStack);
//#endif
            }
        } else {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            context.drawCenteredTextWithShadow(textRenderer, "Loading preview...", centerX, centerY, 0xFFBBBBBB);
//#elseif MC == 11701 || MC == 11904
//$$            drawCenteredStringWithShadow(centerX, centerY, 0xFFBBBBBB, "Loading preview...", matrixStack);
//#endif
        }
    }

    private void resized() {
        int x = this.x + 1;
        int y = this.y + 1;
        for (ClickableWidget button : buttons) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            button.setPosition(x, y);
//#endif
//#if MC == 11701
//$$            // 1.17.1 的 ClickableWidget 无 setPosition，直接写坐标字段 x/y
//#endif
//#if MC == 11701
//$$            button.x = x;
//#endif
//#if MC == 11701
//$$            button.y = y;
//#endif
            x += button.getWidth() + 2;
        }
    }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void renderPreviewAndOverlay(DrawContext context, float tickDelta) {
//#elseif MC == 11701 || MC == 11904
//$$    private void renderPreviewAndOverlay(MatrixStack matrixStack, float tickDelta) {
//#endif
        if (fullscreen) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), 0xFF000000);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrixStack, 0, 0, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), 0xFF000000);
//#endif
        }
//#if MC == 12104
//$$        // 1.21.2+ 的 DrawContext 会批量缓存 GUI 绘制，直接做原始 GL 渲染前必须先 flush
//#endif
//#if MC == 12104
//$$        context.draw();
//#endif
        boolean framebufferUpdated = prepareFramebuffer();
        if (nonStatic || framebufferUpdated || renderer.needsReRender()) {
//#if MC == 12108 || MC == 12110 || MC == 12111
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    framebuffer.getColorAttachment(),
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    0,
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    framebuffer.getDepthAttachment(),
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    1.0
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            );
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            if (scissor) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                GlStateManager._disableScissorTest();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            // make sure the clear (including its alpha) really lands in the framebuffer
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            RenderSystem.colorMask(true, true, true, true);
//#endif
//#if MC == 12104
//$$            framebuffer.clear();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$            framebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$            framebuffer.beginWrite(true);
//#endif
            renderer.render(framebuffer, tickDelta);
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            mc.getFramebuffer().beginWrite(true);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            if (scissor) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                GlStateManager._enableScissorTest();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            }
//#endif
        }
//#if MC == 12108 || MC == 12110 || MC == 12111
        context.state.addSimpleElement(new TexturedQuadGuiElementRenderState(
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                RenderPipelines.GUI_TEXTURED,
//#endif
//#if MC == 12108 || MC == 12110
                TextureSetup.withoutGlTexture(framebuffer.getColorAttachmentView()),
//#elseif MC == 12111
//$$                TextureSetup.of(framebuffer.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR)),
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                new Matrix3x2f(context.getMatrices()),
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                x,
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                y,
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                x + width,
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                y + height,
//#endif
//#if MC == 12101 || MC == 12104
//$$        RenderSystem.backupProjectionMatrix();
//#endif
//#if MC == 12101 || MC == 12104
//$$        Matrix4f projMat = new Matrix4f().setOrtho(
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                0.0F,
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                1.0F,
//#elseif MC == 12101 || MC == 12104
//$$                mc.getWindow().getFramebufferWidth(),
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                1.0F,
//#elseif MC == 12101 || MC == 12104
//$$                mc.getWindow().getFramebufferHeight(),
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                0.0F,
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                0xFFFFFFFF,
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                context.scissorStack.peekLast()
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        ));
//#endif
//#if MC == 12101 || MC == 12104
//$$                1000.0F,
//#endif
//#if MC == 12101 || MC == 12104
//$$                21000.0F
//#endif
//#if MC == 12101 || MC == 12104
//$$        );
//#endif
//#if MC == 12101
//$$        RenderSystem.setProjectionMatrix(projMat, VertexSorter.BY_Z);
//#elseif MC == 12104
//$$        RenderSystem.setProjectionMatrix(projMat, ProjectionType.ORTHOGRAPHIC);
//#endif
//#if MC == 12101 || MC == 12104
//$$        RenderSystem.setShaderTexture(0, framebuffer.getColorAttachment());
//#endif
//#if MC == 12101
//$$        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
//#elseif MC == 12104
//$$        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
//#endif
//#if MC == 12101 || MC == 12104
//$$        RenderSystem.disableDepthTest();
//#endif
//#if MC == 12101 || MC == 12104
//$$        RenderSystem.depthMask(false);
//#endif
//#if MC == 12101 || MC == 12104
//$$        Matrix4f matrix4f = context.getMatrices().peek().getPositionMatrix();
//#endif
//#if MC == 12101 || MC == 12104
//$$        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
//#endif
//#if MC == 12101 || MC == 12104
//$$        float scaleFactor = (float) mc.getWindow().getScaleFactor();
//#endif
//#if MC == 12101 || MC == 12104
//$$        float x1 = x * scaleFactor;
//#endif
//#if MC == 12101 || MC == 12104
//$$        float y1 = y * scaleFactor;
//#endif
//#if MC == 12101 || MC == 12104
//$$        float x2 = (x + width) * scaleFactor;
//#endif
//#if MC == 12101 || MC == 12104
//$$        float y2 = (y + height) * scaleFactor;
//#endif
//#if MC == 12101 || MC == 12104
//$$        bufferBuilder.vertex(matrix4f, x1, y1, 0.0F).texture(0.0F, 1.0F);
//#endif
//#if MC == 12101 || MC == 12104
//$$        bufferBuilder.vertex(matrix4f, x1, y2, 0.0F).texture(0.0F, 0.0F);
//#endif
//#if MC == 12101 || MC == 12104
//$$        bufferBuilder.vertex(matrix4f, x2, y2, 0.0F).texture(1.0F, 0.0F);
//#endif
//#if MC == 12101 || MC == 12104
//$$        bufferBuilder.vertex(matrix4f, x2, y1, 0.0F).texture(1.0F, 1.0F);
//#endif
//#if MC == 12101 || MC == 12104
//$$        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
//#endif
//#if MC == 12101 || MC == 12104
//$$        RenderSystem.depthMask(true);
//#endif
//#if MC == 12101 || MC == 12104
//$$        RenderSystem.enableDepthTest();
//#endif
//#if MC == 12101 || MC == 12104
//$$        RenderSystem.restoreProjectionMatrix();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        blitPreviewFramebuffer();
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        renderOverlay(context, tickDelta);
//#elseif MC == 11701 || MC == 11904
//$$        renderOverlay(matrixStack, tickDelta);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701
//$$    /**
//$$     * Copies the preview framebuffer into the main framebuffer with a raw GL blit.
//$$     */
//#elseif MC == 11904 || MC == 12001
//$$    /**
//$$     * Copies the preview framebuffer into the main framebuffer with a raw GL blit.
//$$     * Sampling the framebuffer texture through a shader proved unreliable here, a plain
//$$     * framebuffer-to-framebuffer copy involves no texture sampling, shaders or matrices.
//$$     */
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    private void blitPreviewFramebuffer() {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        Window window = mc.getWindow();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        Framebuffer main = mc.getFramebuffer();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        double scale = window.getScaleFactor();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        int destX1 = (int) Math.round(x * scale);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        int destX2 = (int) Math.round((x + width) * scale);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        int destY1 = window.getFramebufferHeight() - (int) Math.round((y + height) * scale);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        int destY2 = window.getFramebufferHeight() - (int) Math.round(y * scale);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, framebuffer.fbo);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, main.fbo);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        GL30.glBlitFramebuffer(
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                0, 0, framebuffer.viewportWidth, framebuffer.viewportHeight,
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                destX1, destY1, destX2, destY2,
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        );
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, main.fbo);
//#endif
//#if MC == 11904 || MC == 12001
//$$        // make sure both binding points are really back on the main framebuffer, GlStateManager does
//#endif
//#if MC == 11904 || MC == 12001
//$$        // not track the separate read/draw binding targets
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.fbo);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, main.fbo);
//#endif
    }

    private boolean prepareFramebuffer() {
        double scale = mc.getWindow().getScaleFactor();
        int scaledWidth = (int) (scale * width);
        int scaledHeight = (int) (scale * height);
        if (framebuffer == null) {
//#if MC == 12108 || MC == 12110 || MC == 12111
            framebuffer = new SimpleFramebuffer("SchematicPreview", scaledWidth, scaledHeight, true);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$            framebuffer = new SimpleFramebuffer(scaledWidth, scaledHeight, true, MinecraftClient.IS_SYSTEM_MAC);
//#elseif MC == 12104
//$$            framebuffer = new SimpleFramebuffer(scaledWidth, scaledHeight, true);
//#endif
//#if MC == 12101 || MC == 12104
//$$            framebuffer.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
//#endif
//#if MC == 11904 || MC == 12001
//$$            // the preview is copied into the main framebuffer with a raw blit, which replaces pixels
//#endif
//#if MC == 11904 || MC == 12001
//$$            // instead of blending them, so the preview background has to be opaque
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            framebuffer.setClearColor(0.07F, 0.07F, 0.07F, 1.0F);
//#endif
            return true;
        }
//#if MC == 12108 || MC == 12110 || MC == 12111
        if (framebuffer.textureWidth != scaledWidth || framebuffer.textureHeight != scaledHeight) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$        if (framebuffer.viewportWidth != scaledWidth || framebuffer.viewportHeight != scaledHeight) {
//#endif
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            framebuffer.resize(scaledWidth, scaledHeight);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$            framebuffer.resize(scaledWidth, scaledHeight, MinecraftClient.IS_SYSTEM_MAC);
//#endif
            return true;
        }
        return false;
    }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void renderOverlay(DrawContext context, float tickDelta) {
//#elseif MC == 11701 || MC == 11904
//$$    private void renderOverlay(MatrixStack matrixStack, float tickDelta) {
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        int mouseX = getMouseX();
//#elseif MC == 11701
//$$        int mouseX = screenMouseX();
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        int mouseY = getMouseY();
//#elseif MC == 11701
//$$        int mouseY = screenMouseY();
//#endif
        for (ClickableWidget button : buttons) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            button.render(context, mouseX, mouseY, tickDelta);
//#elseif MC == 11701 || MC == 11904
//$$            button.render(matrixStack, mouseX, mouseY, tickDelta);
//#endif
        }
    }

    private void toggleFullscreen(boolean fullscreen) {
        if (fullscreen) {
            if (this.fullscreen) {
                return; // 已处于全屏界面中，避免再叠加打开一层全屏界面
            }
            mc.setScreen(new GuiSchematicPreviewFullscreen(mc.currentScreen, this));
        } else {
            if (mc.currentScreen != null) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                mc.currentScreen.close();
//#elseif MC == 11701
//$$                mc.currentScreen.onClose();
//#endif
            }
        }
    }

    public void setFullScreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
        if (fullscreenButton != null) {
            fullscreenButton.setOn(fullscreen);
        }
    }

    private void toggleFreecam(boolean freecam) {
        this.freecam = freecam;
        if (!freecam) {
            resetCameraPos();
        }
    }

    private void resetCameraPos() {
        Vec3i size = lastSchematic.getMetadata().getEnclosingSize();
        rotOrigin.set(size.getX() * 0.5F, size.getY() * 0.3F, size.getZ() * 0.5F);
        yRot = (float) PreviewOptions.PREVIEW_ROTATION_Y;
        xRot = (float) PreviewOptions.PREVIEW_ROTATION_X;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        distance = MathHelper.sqrt(MathHelper.magnitude(size.getX() * 0.5F, size.getY() * 0.7F, size.getZ() * 0.5F)) * 2.0F;
//#endif
//#if MC == 11904 || MC == 12001
//$$        // MathHelper.magnitude is already the length of the vector, no extra sqrt here
//#endif
//#if MC == 11904 || MC == 12001
//$$        // (1.21's float overload returns the squared length, which is why the official code takes a sqrt)
//#endif
//#if MC == 11904 || MC == 12001
//$$        distance = (float) MathHelper.magnitude(size.getX() * 0.5F, size.getY() * 0.7F, size.getZ() * 0.5F) * 2.5F;
//#endif
//#if MC == 11701
//$$        float halfX = size.getX() * 0.5F;
//#endif
//#if MC == 11701
//$$        float height = size.getY() * 0.7F;
//#endif
//#if MC == 11701
//$$        float halfZ = size.getZ() * 0.5F;
//#endif
//#if MC == 11701
//$$       distance = MathHelper.sqrt(halfX * halfX + height * height + halfZ * halfZ) * 2.5F;
//#endif
        updateRendererCameraPos();
        renderer.resetPosition();
    }

    private void updateRendererCameraPos() {
        renderer.setRotation(-xRot, yRot);
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Vector3f offset = getRotationVec(xRot, 180.0F + yRot).mul(-distance);
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        renderer.setPos(rotOrigin.x + offset.x, rotOrigin.y + offset.y, rotOrigin.z + offset.z);
//#endif
//#if MC == 11701
//$$        Vec3f offset = getRotationVec(xRot, 180.0F + yRot);
//#endif
//#if MC == 11701
//$$        offset.scale(-distance);
//#endif
//#if MC == 11701
//$$        renderer.setPos(rotOrigin.getX() + offset.getX(), rotOrigin.getY() + offset.getY(), rotOrigin.getZ() + offset.getZ());
//#endif
    }

//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$    public void setScissor(boolean scissor) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$        this.scissor = scissor;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$    }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$
//#endif
    public void tick() {
        if (nonStatic) {
            renderer.tick();
            if (mouseDragging) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                int x = getMouseX();
//#elseif MC == 11701
//$$                int x = screenMouseX();
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                int y = getMouseY();
//#elseif MC == 11701
//$$                int y = screenMouseY();
//#endif
                int deltaX = x - lastMouseX;
                int deltaY = y - lastMouseY;
                if (deltaX != 0 || deltaY != 0) {
                    mouseDragged(deltaX, deltaY);
                    lastMouseX = x;
                    lastMouseY = y;
                }
            }
            checkLayerFilterChanged();
        }
    }

    /**
     * 跟随「同步投影渲染层」的渲染层规则变化自动重建预览：
     * 签名变化后稳定 1 秒（防抖）才应用，避免玩家切换渲染层时反复重建。
     */
    private void checkLayerFilterChanged() {
        if (lastSchematic == null || schematicFile == null) {
            return;
        }
        String signature = LitematicaBridge.getRenderLayerStateSignature();
        if (Objects.equals(signature, lastAppliedLayerSignature)) {
            pendingLayerSignature = null;
            return;
        }
        long now = System.currentTimeMillis();
        if (Objects.equals(signature, pendingLayerSignature)) {
            if (now - pendingLayerSignatureTime < LAYER_FILTER_DEBOUNCE_MS) {
                return;
            }
            pendingLayerSignature = null;
            lastAppliedLayerSignature = signature;
//#if MC == 12108 || MC == 12110 || MC == 12111
            layerFilter = LitematicaBridge.computeRenderLayerFilter(schematicFile);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            layerFilter = LitematicaBridge.computeRenderLayerFilter(schematicFile.toPath());
//#endif
            renderer.newSchematic(lastSchematic, layerFilter);
        } else {
            pendingLayerSignature = signature;
            pendingLayerSignatureTime = now;
        }
    }

//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private int getMouseX() {
//#elseif MC == 11701
//$$    private int screenMouseX() {
//#endif
        return (int) (mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth());
    }

//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private int getMouseY() {
//#elseif MC == 11701
//$$    private int screenMouseY() {
//#endif
        return (int) (mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight());
    }

    private void mouseDragged(int deltaX, int deltaY) {
        float dX = ((float) deltaX / width) * 180.0F;
        float dY = ((float) deltaY / height) * 180.0F;
        if (freecam) {
            renderer.setRotation(renderer.getPitch() - dY, renderer.getYaw() - dX);
        } else {
            yRot = MathHelper.wrapDegrees(yRot - dX);
            xRot = MathHelper.clamp(xRot + dY, -90.0F, 90.0F);
            updateRendererCameraPos();
        }
    }

    @Override
//#if MC == 12110 || MC == 12111
    protected boolean onMouseClickedImpl(Click click, boolean doubleClick) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$    protected boolean onMouseClickedImpl(int mouseX, int mouseY, int mouseButton) {
//#endif
        if (nonStatic) {
            for (ClickableWidget button : buttons) {
//#if MC == 12110 || MC == 12111
                if (button.mouseClicked(click, doubleClick)) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                if (button.mouseClicked(mouseX, mouseY, mouseButton)) {
//#endif
                    return true;
                }
            }
//#if MC == 12110 || MC == 12111
            if (click.button() == 0) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            if (mouseButton == 0) {
//#endif
//#if MC == 12110 || MC == 12111
                lastMouseX = (int) click.x();
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                lastMouseX = mouseX;
//#endif
//#if MC == 12110 || MC == 12111
                lastMouseY = (int) click.y();
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                lastMouseY = mouseY;
//#endif
                mouseDragging = true;
                return true;
            }
        }
        return false;
    }

    @Override
//#if MC == 12110 || MC == 12111
    public void onMouseReleasedImpl(Click click) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$    public void onMouseReleasedImpl(int mouseX, int mouseY, int mouseButton) {
//#endif
//#if MC == 12110 || MC == 12111
        if (nonStatic && click.button() == 0) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        if (nonStatic && mouseButton == 0) {
//#endif
            mouseDragging = false;
        }
    }

    @Override
//#if MC == 12110 || MC == 12111
    public boolean onMouseScrolledImpl(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    public boolean onMouseScrolledImpl(int mouseX, int mouseY, double verticalAmount) {
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$    public boolean onMouseScrolledImpl(int mouseX, int mouseY, double horizontalAmount, double verticalAmount) {
//#endif
        if (nonStatic) {
            if (verticalAmount != 0.0F) {
                float am = -(float) verticalAmount;
                if (freecam) {
//#if MC == 12110 || MC == 12111
                    am *= 2.0F * (MinecraftClient.getInstance().isShiftPressed() ? 2.0F : 1.0F) * (MinecraftClient.getInstance().isCtrlPressed() ? 2.0F : 1.0F);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$                    am *= 2.0F * (Screen.hasShiftDown() ? 2.0F : 1.0F) * (Screen.hasControlDown() ? 2.0F : 1.0F);
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    Vector3f move = getRotationVec(renderer.getPitch(), renderer.getYaw()).mul(am);
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    renderer.setPos(renderer.getX() + move.x, renderer.getY() + move.y, renderer.getZ() + move.z);
//#endif
//#if MC == 11701
//$$                    Vec3f move = getRotationVec(renderer.getPitch(), renderer.getYaw());
//#endif
//#if MC == 11701
//$$                    move.scale(am);
//#endif
//#if MC == 11701
//$$                    renderer.setPos(renderer.getX() + move.getX(), renderer.getY() + move.getY(), renderer.getZ() + move.getZ());
//#endif
                } else {
                    distance = MathHelper.square(Math.max(0.0F, MathHelper.sqrt(distance) + am * 0.5F));
                    updateRendererCameraPos();
                }
            }
            return true;
        }
        return false;
    }

    public void removed() {
        if (disposed) {
            return; // 防止界面关闭与缓存清理重复调用导致重复释放
        }
        disposed = true;
        if (framebuffer != null) {
            framebuffer.delete();
            framebuffer = null;
        }
        renderer.close();
        PreviewManager.removeTickable(tickAction);
    }

//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private static Vector3f getRotationVec(float pitch, float yaw) {
//#elseif MC == 11701
//$$    private static Vec3f getRotationVec(float pitch, float yaw) {
//#endif
        pitch = pitch * MathHelper.RADIANS_PER_DEGREE;
        yaw = yaw * MathHelper.RADIANS_PER_DEGREE;
        float h = MathHelper.cos(yaw);
        float i = MathHelper.sin(yaw);
        float j = MathHelper.cos(pitch);
        float k = MathHelper.sin(pitch);
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return new Vector3f(i * j, -k, h * j);
//#elseif MC == 11701
//$$        return new Vec3f(i * j, -k, h * j);
//#endif
    }

    private static class Renderer {

        private final MinecraftClient mc;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        private final Quaternionf rotation = new Quaternionf();
//#elseif MC == 11701
//$$        private final Vec3f lastRenderPos = new Vec3f();
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        private final Vector3f lastRenderPos = new Vector3f();
//#elseif MC == 11701
//$$        private final Vec3f prevPos = new Vec3f();
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        private final Vector3f prevPos = new Vector3f();
//#elseif MC == 11701
//$$        private final Vec3f pos = new Vec3f(0.0F, 2.0F, 100.0F);
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        private final Vector3f pos = new Vector3f(0.0F, 2.0F, 100.0F);
//#elseif MC == 11701
//$$        private float prevPitch;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        private final Vector2f lastRenderRot = new Vector2f();
//#elseif MC == 11701
//$$        private float prevYaw;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        private final Vector2f prevRot = new Vector2f();
//#elseif MC == 11701
//$$        private float pitch;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        private final Vector2f rot = new Vector2f();
//#elseif MC == 11701
//$$        private float yaw;
//#endif
//#if MC == 12110 || MC == 12111
        private final CameraRenderState cameraRenderState = new CameraRenderState();
//#endif
        private SchematicPreviewRenderer renderer;
        private int lastChunksBuilt;
        private boolean schematicNew;
//#if MC == 12108 || MC == 12110 || MC == 12111
        @Nullable
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        private RawProjectionMatrix projectionMatrix;
//#endif
//#if MC == 12111
//$$        @Nullable
//#endif
//#if MC == 12111
//$$        private GpuBuffer globalUniform;
//#endif

        private Renderer(MinecraftClient mc) {
            this.mc = mc;
        }

        private void tick() {
            resetPosition();
        }

        private void resetPosition() {
            prevPos.set(pos);
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            prevRot.set(rot);
//#endif
//#if MC == 11701
//$$            prevPitch = pitch;
//#endif
//#if MC == 11701
//$$            prevYaw = yaw;
//#endif
        }

        private float getX() {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            return pos.x;
//#elseif MC == 11701
//$$            return pos.getX();
//#endif
        }

        private float getY() {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            return pos.y;
//#elseif MC == 11701
//$$            return pos.getY();
//#endif
        }

        private float getZ() {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            return pos.z;
//#elseif MC == 11701
//$$            return pos.getZ();
//#endif
        }

        private void setPos(float x, float y, float z) {
            pos.set(x, y, z);
        }

        private float getPitch() {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            return rot.x;
//#elseif MC == 11701
//$$            return pitch;
//#endif
        }

        private float getYaw() {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            return rot.y;
//#elseif MC == 11701
//$$            return yaw;
//#endif
        }

        private void setRotation(float pitch, float yaw) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            rot.set(MathHelper.clamp(pitch, -90.0F, 90.0F), MathHelper.wrapDegrees(yaw));
//#endif
//#if MC == 11701
//$$            this.pitch = MathHelper.clamp(pitch, -90.0F, 90.0F);
//#endif
//#if MC == 11701
//$$            this.yaw = MathHelper.wrapDegrees(yaw);
//#endif
        }

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        private void newSchematic(LitematicaSchematic schematic, @Nullable LayerFilter filter) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        private void newSchematic(LitematicaSchematic schematic, @Nullable LayerFilter layerFilter) {
//#endif
            if (renderer == null) {
                renderer = new SchematicPreviewRenderer(mc);
            }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            renderer.setup(schematic, filter);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            renderer.setup(schematic, layerFilter);
//#endif
            schematicNew = true;
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        private boolean isBuildDone() {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            return renderer.isBuildDone();
//#endif
        }

        private boolean isBuildingTerrainOrStart() {
            return renderer.isBuildingTerrain();
        }

        private boolean needsReRender() {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            return schematicNew || !pos.equals(lastRenderPos) || !rot.equals(lastRenderRot) || renderer.getBuiltChunksCount() != lastChunksBuilt;
//#elseif MC == 11701
//$$            return schematicNew || !pos.equals(lastRenderPos) || pitch != prevPitch || yaw != prevYaw || renderer.getBuiltChunksCount() != lastChunksBuilt;
//#endif
        }

        private void render(Framebuffer framebuffer, float tickDelta) {
            schematicNew = false;

            if (isBuildingTerrainOrStart()) {
                return;
            }

            lastChunksBuilt = renderer.getBuiltChunksCount();
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            RenderSystem.backupProjectionMatrix();
//#endif
//#if MC == 11701
//$$            // 1.17.1 的 Matrix4f.projectionMatrix 是正交投影；
//$$            // 透视投影必须使用 viewboxMatrix（FOV 单位为角度）
//$$            Matrix4f projectionMatrix = Matrix4f.viewboxMatrix(
//#elseif MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            Matrix4f projectionMatrix = new Matrix4f().perspective(
//#endif
//#if MC == 11701
//$$                    (double) PreviewOptions.PREVIEW_FOV,
//#elseif MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                    (float) PreviewOptions.PREVIEW_FOV * MathHelper.RADIANS_PER_DEGREE,
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                    (float) framebuffer.viewportWidth / framebuffer.viewportHeight,
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                    0.05F,
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    4096.0F
//#elseif MC == 12101 || MC == 12104
//$$                    1024.0F
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            );
//#endif
//#if MC == 12001 || MC == 12101
//$$            RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorter.BY_DISTANCE);
//#elseif MC == 12104
//$$            RenderSystem.setProjectionMatrix(projectionMatrix, ProjectionType.PERSPECTIVE);
//#endif
//#if MC == 11904
//$$            // MC 1.19.4 has no per-frame vertex sorter here, translucent geometry is sorted when the
//#endif
//#if MC == 11904
//$$            // chunk buffer is built (see SchematicPreviewRenderer.BuiltChunk)
//#endif
//#if MC == 11701 || MC == 11904
//$$            RenderSystem.setProjectionMatrix(projectionMatrix);
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            lastRenderRot.set(MathHelper.lerp(tickDelta, prevRot.x, rot.x), MathHelper.lerpAngleDegrees(tickDelta, prevRot.y, rot.y));
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            Matrix4f rotationMatrix = new Matrix4f().rotation(rotation.rotationYXZ(
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                    lastRenderRot.y * MathHelper.RADIANS_PER_DEGREE,
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                    lastRenderRot.x * MathHelper.RADIANS_PER_DEGREE,
//#endif
//#if MC == 11701
//$$            float lastYaw = MathHelper.lerpAngleDegrees(tickDelta, prevYaw, yaw);
//#endif
//#if MC == 11701
//$$            float lastPitch = MathHelper.lerp(tickDelta, prevPitch, pitch);
//#endif
//#if MC == 11701
//$$            // 1.17.1 无 JOML：用 MC 自带的 Quaternion/Matrix4f 构建相机旋转矩阵
//#endif
//#if MC == 11701
//$$            Quaternion quaternion = Quaternion.fromEulerYxz(
//#endif
//#if MC == 11701
//$$                    lastYaw * MathHelper.RADIANS_PER_DEGREE,
//#endif
//#if MC == 11701
//$$                    lastPitch * MathHelper.RADIANS_PER_DEGREE,
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                    0.0F
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            ).conjugate());
//#endif
//#if MC == 11701
//$$            );
//#endif
//#if MC == 11701
//$$            quaternion.conjugate();
//#endif
//#if MC == 11701
//$$            Matrix4f rotationMatrix = new Matrix4f(quaternion);
//#endif
//#if MC == 12101 || MC == 12104
//$$            Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
//#endif
//#if MC == 12101 || MC == 12104
//$$            modelViewStack.pushMatrix();
//#endif
//#if MC == 12101 || MC == 12104
//$$            modelViewStack.set(rotationMatrix);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            MatrixStack modelViewStack = RenderSystem.getModelViewStack();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            modelViewStack.push();
//#endif
//#if MC == 11904 || MC == 12001
//$$            // MC 1.20.1: the GUI model view carries its own z offset (about -11000), which would push
//#endif
//#if MC == 11904 || MC == 12001
//$$            // the whole schematic far beyond the far plane. Start from identity instead.
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            modelViewStack.loadIdentity();
//#endif
//#if MC == 11701
//$$            modelViewStack.peek().getModel().multiply(rotationMatrix);
//#elseif MC == 11904 || MC == 12001
//$$            modelViewStack.multiplyPositionMatrix(rotationMatrix);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$            RenderSystem.applyModelViewMatrix();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            // Draw layers
//#endif
            lastRenderPos.set(
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    MathHelper.lerp(tickDelta, prevPos.x, pos.x),
//#elseif MC == 11701
//$$                    MathHelper.lerp(tickDelta, prevPos.getX(), pos.getX()),
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    MathHelper.lerp(tickDelta, prevPos.y, pos.y),
//#elseif MC == 11701
//$$                    MathHelper.lerp(tickDelta, prevPos.getY(), pos.getY()),
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    MathHelper.lerp(tickDelta, prevPos.z, pos.z)
//#elseif MC == 11701
//$$                    MathHelper.lerp(tickDelta, prevPos.getZ(), pos.getZ())
//#endif
            );
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            modelViewStack.pushMatrix();
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            modelViewStack.set(new Matrix4f().rotation(rotation.rotationYXZ(
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    lastRenderRot.y * MathHelper.RADIANS_PER_DEGREE,
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    lastRenderRot.x * MathHelper.RADIANS_PER_DEGREE,
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    0.0F
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            ).conjugate()));
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            RenderSystem.backupProjectionMatrix();
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            if (projectionMatrix == null) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                projectionMatrix = new RawProjectionMatrix("SchematicPreview");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            }
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            RenderSystem.setProjectionMatrix(projectionMatrix.set(new Matrix4f().perspective(
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    (float) PreviewOptions.PREVIEW_FOV * MathHelper.RADIANS_PER_DEGREE,
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    (float) framebuffer.textureWidth / framebuffer.textureHeight,
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    0.05F,
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    4096.0F
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            )), ProjectionType.PERSPECTIVE);
//#endif
//#if MC == 12111
//$$            // 1.21.11 起区块渲染依赖全局 UBO（相机所在方块坐标、相机位置等），
//#endif
//#if MC == 12111
//$$            // 离屏预览必须写入自己的相机参数，否则方块位置/雾效会跟随玩家相机
//#endif
//#if MC == 12111
//$$            GpuBuffer previousGlobalUniform = RenderSystem.getGlobalSettingsUniform();
//#endif
//#if MC == 12111
//$$            if (globalUniform == null) {
//#endif
//#if MC == 12111
//$$                globalUniform = RenderSystem.getDevice().createBuffer(
//#endif
//#if MC == 12111
//$$                        () -> "SchematicPreview Global Settings UBO",
//#endif
//#if MC == 12111
//$$                        GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
//#endif
//#if MC == 12111
//$$                        GlobalSettings.SIZE
//#endif
//#if MC == 12111
//$$                );
//#endif
//#if MC == 12111
//$$            }
//#endif
//#if MC == 12111
//$$            try (MemoryStack memoryStack = MemoryStack.stackPush()) {
//#endif
//#if MC == 12111
//$$                int floorX = MathHelper.floor(lastRenderPos.x);
//#endif
//#if MC == 12111
//$$                int floorY = MathHelper.floor(lastRenderPos.y);
//#endif
//#if MC == 12111
//$$                int floorZ = MathHelper.floor(lastRenderPos.z);
//#endif
//#if MC == 12111
//$$                ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, GlobalSettings.SIZE)
//#endif
//#if MC == 12111
//$$                        .putIVec3(floorX, floorY, floorZ)
//#endif
//#if MC == 12111
//$$                        .putVec3(floorX - lastRenderPos.x, floorY - lastRenderPos.y, floorZ - lastRenderPos.z)
//#endif
//#if MC == 12111
//$$                        .putVec2(mc.getWindow().getWidth(), mc.getWindow().getHeight())
//#endif
//#if MC == 12111
//$$                        .putFloat(1.0F)
//#endif
//#if MC == 12111
//$$                        .putFloat(0.0F)
//#endif
//#if MC == 12111
//$$                        .putInt(0)
//$$                        .putInt(0)
//#endif
//#if MC == 12111
//$$                        .get();
//#endif
//#if MC == 12111
//$$                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(globalUniform.slice(), byteBuffer);
//#endif
//#if MC == 12111
//$$            }
//#endif
//#if MC == 12111
//$$            RenderSystem.setGlobalSettingsUniform(globalUniform);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            // Draw layers
//#endif
//#if MC == 12110 || MC == 12111
            cameraRenderState.initialized = true;
//#endif
//#if MC == 12110 || MC == 12111
            cameraRenderState.orientation = rotation;
//#endif
//#if MC == 12110 || MC == 12111
            cameraRenderState.pos = new Vec3d(lastRenderPos.x, lastRenderPos.y, lastRenderPos.z);
//#endif
//#if MC == 12110 || MC == 12111
            cameraRenderState.entityPos = cameraRenderState.pos;
//#endif
//#if MC == 12110 || MC == 12111
            cameraRenderState.blockPos = BlockPos.ofFloored(cameraRenderState.pos);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            mc.gameRenderer.getDiffuseLighting().setShaderLights(DiffuseLighting.Type.LEVEL);
//#endif
//#if MC == 12110 || MC == 12111
            renderer.prepareRender(cameraRenderState, framebuffer);
//#elseif MC == 11701
//$$            renderer.prepareRender(lastRenderPos.getX(), lastRenderPos.getY(), lastRenderPos.getZ(), framebuffer);
//#elseif MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            renderer.prepareRender(lastRenderPos.x, lastRenderPos.y, lastRenderPos.z, framebuffer);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            renderer.renderBlocks();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            renderer.renderLayer(RenderLayer.getSolid());
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            renderer.renderLayer(RenderLayer.getCutoutMipped());
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            renderer.renderLayer(RenderLayer.getCutout());
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            renderer.renderLayer(RenderLayer.getTranslucent());
//#endif
            if (PreviewOptions.RENDER_TILE) {
//#if MC == 12108 || MC == 12110 || MC == 12111
                renderer.renderBlockEntities(new MatrixStack(), tickDelta);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                renderer.renderBlockEntities(new MatrixStack(), mc.getBufferBuilders().getEntityVertexConsumers(), tickDelta);
//#endif
            }

//#if MC == 12111
//$$            RenderSystem.setGlobalSettingsUniform(previousGlobalUniform);
//#endif
            RenderSystem.restoreProjectionMatrix();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            modelViewStack.popMatrix();
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            modelViewStack.pop();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$            RenderSystem.applyModelViewMatrix();
//#endif
        }

        private void close() {
            if (renderer != null) {
                renderer.close();
                renderer = null;
            }
//#if MC == 12111
//$$            if (projectionMatrix != null) {
//#endif
//#if MC == 12111
//$$                projectionMatrix.close();
//#endif
//#if MC == 12111
//$$                projectionMatrix = null;
//#endif
//#if MC == 12111
//$$            }
//#endif
//#if MC == 12111
//$$            if (globalUniform != null) {
//#endif
//#if MC == 12111
//$$                globalUniform.close();
//#endif
//#if MC == 12111
//$$                globalUniform = null;
//#endif
//#if MC == 12111
//$$            }
//#endif
        }

    }

}
//#if MC == 11701
//$$
//#endif
//#if MC == 12110 || MC == 12111

//#endif