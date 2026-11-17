package com.litematlist.preview;

import com.litematlist.LitematicaBridge;
import com.litematlist.LitematicaBridge.LayerFilter;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.PoseStack;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.lwjgl.system.MemoryStack;

import java.lang.Math;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

// Extending malilib widget, but using vanilla inside
public class SchematicPreviewWidget extends WidgetBase {

    private final Renderer renderer = new Renderer(mc);
    private final Runnable tickAction = this::tick;
    private final List<AbstractWidget> buttons;
    private final Vector3f rotOrigin = new Vector3f();
    private final PreviewsCache cache;
    private final boolean nonStatic;
    private Path schematicFile;
    private LitematicaSchematic lastSchematic;
    private RenderTarget framebuffer;
    private boolean mouseDragging;
    private int lastMouseX;
    private int lastMouseY;
    private boolean fullscreen;
    private boolean freecam;
    private float distance;
    private float yRot;
    private float xRot;
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
                        Component.translatable("litematlist.preview.fullscreen"),
                        this::toggleFullscreen,
                        PreviewManager.sprite("fullscreen_on"),
                        PreviewManager.sprite("fullscreen_on_focused"),
                        PreviewManager.sprite("fullscreen_off"),
                        PreviewManager.sprite("fullscreen_off_focused")
                ), new OnOffButton(
                        BUTTON_SIZE,
                        BUTTON_SIZE,
                        Component.translatable("litematlist.preview.freecam"),
                        this::toggleFreecam,
                        PreviewManager.sprite("freecam_on"),
                        PreviewManager.sprite("freecam_on_focused"),
                        PreviewManager.sprite("freecam_off"),
                        PreviewManager.sprite("freecam_off_focused")
                ))
                : List.of();
        fullscreenButton = nonStatic ? (OnOffButton) buttons.get(0) : null;
    }

    public void setSchematic(Path schematicFile) {
        this.schematicFile = schematicFile;
    }

    public void renderPreviewAndOverlay(GuiGraphicsExtractor context, int x, int y, int width, int height) {
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
                        renderPreviewAndOverlay(context, mc.getDeltaTracker().getGameTimeDeltaPartialTick(true));
                    } else {
                        context.centeredText(textRenderer, "Building terrain...", centerX, centerY, 0xFFBBBBBB);
                    }
                } else {
                    context.centeredText(textRenderer, "Too big", centerX, centerY, 0xFFBBBBBB);
                }
            } else {
                context.centeredText(textRenderer, "Preview load failed", centerX, centerY, 0xFFFF5555);
            }
        } else {
            context.centeredText(textRenderer, "Loading preview...", centerX, centerY, 0xFFBBBBBB);
        }
    }

    private void resized() {
        int x = this.x + 1;
        int y = this.y + 1;
        for (AbstractWidget button : buttons) {
            button.setPosition(x, y);
            x += button.getWidth() + 2;
        }
    }

    private void renderPreviewAndOverlay(GuiGraphicsExtractor context, float tickDelta) {
        if (fullscreen) {
            context.fill(0, 0, context.guiWidth(), context.guiHeight(), 0xFF000000);
        }
        boolean framebufferUpdated = prepareFramebuffer();
        if (nonStatic || framebufferUpdated || renderer.needsReRender()) {
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                    framebuffer.getColorTexture(),
                    new Vector4f(0, 0, 0, 0),
                    framebuffer.getDepthTexture(),
                    0.0
            );
            renderer.render(framebuffer, tickDelta);
        }
        context.guiRenderState.addBlitToCurrentLayer(new BlitRenderState(
                RenderPipelines.GUI_TEXTURED,
                TextureSetup.singleTexture(framebuffer.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)),
                new Matrix3x2f(context.pose()),
                x,
                y,
                x + width,
                y + height,
                0.0F,
                1.0F,
                1.0F,
                0.0F,
                0xFFFFFFFF,
                context.scissorStack.peek()
        ));
        renderOverlay(context, tickDelta);
    }

    private boolean prepareFramebuffer() {
        double scale = mc.getWindow().getGuiScale();
        int scaledWidth = (int) (scale * width);
        int scaledHeight = (int) (scale * height);
        if (framebuffer == null) {
            framebuffer = new TextureTarget("SchematicPreview", scaledWidth, scaledHeight, true, GpuFormat.RGBA8_UNORM);
            return true;
        }
        if (framebuffer.width != scaledWidth || framebuffer.height != scaledHeight) {
            framebuffer.resize(scaledWidth, scaledHeight);
            return true;
        }
        return false;
    }

    private void renderOverlay(GuiGraphicsExtractor context, float tickDelta) {
        int mouseX = getMouseX();
        int mouseY = getMouseY();
        for (AbstractWidget button : buttons) {
            button.extractRenderState(context, mouseX, mouseY, tickDelta);
        }
    }

    private void toggleFullscreen(boolean fullscreen) {
        if (fullscreen) {
            if (this.fullscreen) {
                return; // 已处于全屏界面中，避免再叠加打开一层全屏界面
            }
            mc.gui.setScreen(new GuiSchematicPreviewFullscreen(mc.gui.screen(), this));
        } else {
            if (mc.gui.screen() != null) {
                mc.gui.screen().onClose();
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
        distance = Mth.sqrt(Mth.lengthSquared(size.getX() * 0.5F, size.getY() * 0.7F, size.getZ() * 0.5F)) * 2.0F;
        updateRendererCameraPos();
        renderer.resetPosition();
    }

    private void updateRendererCameraPos() {
        renderer.setRotation(-xRot, yRot);
        Vector3f offset = getRotationVec(xRot, 180.0F + yRot).mul(-distance);
        renderer.setPos(rotOrigin.x + offset.x, rotOrigin.y + offset.y, rotOrigin.z + offset.z);
    }

    public void tick() {
        if (nonStatic) {
            renderer.tick();
            if (mouseDragging) {
                int x = getMouseX();
                int y = getMouseY();
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
            layerFilter = LitematicaBridge.computeRenderLayerFilter(schematicFile);
            renderer.newSchematic(lastSchematic, layerFilter);
        } else {
            pendingLayerSignature = signature;
            pendingLayerSignatureTime = now;
        }
    }

    private int getMouseX() {
        return (int) (mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth());
    }

    private int getMouseY() {
        return (int) (mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight());
    }

    private void mouseDragged(int deltaX, int deltaY) {
        float dX = ((float) deltaX / width) * 180.0F;
        float dY = ((float) deltaY / height) * 180.0F;
        if (freecam) {
            renderer.setRotation(renderer.getPitch() - dY, renderer.getYaw() - dX);
        } else {
            yRot = Mth.wrapDegrees(yRot - dX);
            xRot = Mth.clamp(xRot + dY, -90.0F, 90.0F);
            updateRendererCameraPos();
        }
    }

    @Override
    protected boolean onMouseClickedImpl(MouseButtonEvent click, boolean doubleClick) {
        if (nonStatic) {
            for (AbstractWidget button : buttons) {
                if (button.mouseClicked(click, doubleClick)) {
                    return true;
                }
            }
            if (click.button() == 0) {
                lastMouseX = (int) click.x();
                lastMouseY = (int) click.y();
                mouseDragging = true;
                return true;
            }
        }
        return false;
    }

    @Override
    public void onMouseReleasedImpl(MouseButtonEvent click) {
        if (nonStatic && click.button() == 0) {
            mouseDragging = false;
        }
    }

    @Override
    public boolean onMouseScrolledImpl(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (nonStatic) {
            if (verticalAmount != 0.0F) {
                float am = -(float) verticalAmount;
                if (freecam) {
                    am *= 2.0F * (Minecraft.getInstance().hasShiftDown() ? 2.0F : 1.0F) * (Minecraft.getInstance().hasControlDown() ? 2.0F : 1.0F);
                    Vector3f move = getRotationVec(renderer.getPitch(), renderer.getYaw()).mul(am);
                    renderer.setPos(renderer.getX() + move.x, renderer.getY() + move.y, renderer.getZ() + move.z);
                } else {
                    distance = Mth.square(Math.max(0.0F, Mth.sqrt(distance) + am * 0.5F));
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
            framebuffer.destroyBuffers();
            framebuffer = null;
        }
        renderer.close();
        PreviewManager.removeTickable(tickAction);
    }

    private static Vector3f getRotationVec(float pitch, float yaw) {
        pitch = pitch * Mth.DEG_TO_RAD;
        yaw = yaw * Mth.DEG_TO_RAD;
        float h = Mth.cos(yaw);
        float i = Mth.sin(yaw);
        float j = Mth.cos(pitch);
        float k = Mth.sin(pitch);
        return new Vector3f(i * j, -k, h * j);
    }

    private static class Renderer {

        private final Minecraft mc;
        private final Quaternionf rotation = new Quaternionf();
        private final Vector3f lastRenderPos = new Vector3f();
        private final Vector3f prevPos = new Vector3f();
        private final Vector3f pos = new Vector3f(0.0F, 2.0F, 100.0F);
        private final Vector2f lastRenderRot = new Vector2f();
        private final Vector2f prevRot = new Vector2f();
        private final Vector2f rot = new Vector2f();
        private final CameraRenderState cameraRenderState = new CameraRenderState();
        private SchematicPreviewRenderer renderer;
        private int lastChunksBuilt;
        private boolean schematicNew;
        @Nullable
        private ProjectionMatrixBuffer projectionMatrix;
        @Nullable
        private GpuBuffer globalUniform;

        private Renderer(Minecraft mc) {
            this.mc = mc;
        }

        private void tick() {
            resetPosition();
        }

        private void resetPosition() {
            prevPos.set(pos);
            prevRot.set(rot);
        }

        private float getX() {
            return pos.x;
        }

        private float getY() {
            return pos.y;
        }

        private float getZ() {
            return pos.z;
        }

        private void setPos(float x, float y, float z) {
            pos.set(x, y, z);
        }

        private float getPitch() {
            return rot.x;
        }

        private float getYaw() {
            return rot.y;
        }

        private void setRotation(float pitch, float yaw) {
            rot.set(Mth.clamp(pitch, -90.0F, 90.0F), Mth.wrapDegrees(yaw));
        }

        private void newSchematic(LitematicaSchematic schematic, @Nullable LayerFilter layerFilter) {
            if (renderer == null) {
                renderer = new SchematicPreviewRenderer(mc);
            }
            renderer.setup(schematic, layerFilter);
            schematicNew = true;
        }

        private boolean isBuildingTerrainOrStart() {
            return renderer.isBuildingTerrain();
        }

        private boolean needsReRender() {
            return schematicNew || !pos.equals(lastRenderPos) || !rot.equals(lastRenderRot) || renderer.getBuiltChunksCount() != lastChunksBuilt;
        }

        private void render(RenderTarget framebuffer, float tickDelta) {
            schematicNew = false;

            if (isBuildingTerrainOrStart()) {
                return;
            }

            lastChunksBuilt = renderer.getBuiltChunksCount();
            lastRenderRot.set(Mth.lerp(tickDelta, prevRot.x, rot.x), Mth.rotLerp(tickDelta, prevRot.y, rot.y));
            lastRenderPos.set(
                    Mth.lerp(tickDelta, prevPos.x, pos.x),
                    Mth.lerp(tickDelta, prevPos.y, pos.y),
                    Mth.lerp(tickDelta, prevPos.z, pos.z)
            );

            Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
            modelViewStack.pushMatrix();
            modelViewStack.set(new Matrix4f().rotation(rotation.rotationYXZ(
                    lastRenderRot.y * Mth.DEG_TO_RAD,
                    lastRenderRot.x * Mth.DEG_TO_RAD,
                    0.0F
            ).conjugate()));
            RenderSystem.backupProjectionMatrix();
            if (projectionMatrix == null) {
                projectionMatrix = new ProjectionMatrixBuffer("SchematicPreview");
            }
            RenderSystem.setProjectionMatrix(projectionMatrix.getBuffer(new Matrix4f().setPerspective(
                    (float) PreviewOptions.PREVIEW_FOV * Mth.DEG_TO_RAD,
                    (float) framebuffer.width / framebuffer.height,
                    4096.0F,
                    0.05F,
                    RenderSystem.getDevice().getDeviceInfo().isZZeroToOne()
            )), ProjectionType.PERSPECTIVE);
            // 手动写入 GlobalSettingsUniform（与26.1.2一致）
            GpuBuffer previousGlobalUniform = RenderSystem.getGlobalSettingsUniform();
            if (globalUniform == null) {
                globalUniform = RenderSystem.getDevice().createBuffer(() -> "SchematicPreview Global Settings UBO", 136, GlobalSettingsUniform.UBO_SIZE);
            }
            try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                int floorX = Mth.floor(lastRenderPos.x);
                int floorY = Mth.floor(lastRenderPos.y);
                int floorZ = Mth.floor(lastRenderPos.z);
                ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, GlobalSettingsUniform.UBO_SIZE)
                        .putIVec3(floorX, floorY, floorZ)
                        .putVec3(floorX - lastRenderPos.x, floorY - lastRenderPos.y, floorZ - lastRenderPos.z)
                        .putVec2(mc.getWindow().getWidth(), mc.getWindow().getHeight())
                        .putFloat(1.0F)
                        .putFloat(0.0F)
                        .putInt(0)
                        .putInt(0)
                        .get();
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(globalUniform.slice(), byteBuffer);
            }
            RenderSystem.setGlobalSettingsUniform(globalUniform);

            // 设置 dummy fog 避免未绑定 Fog uniform 导致片段着色器输出零
            GpuBufferSlice previousFog = RenderSystem.getShaderFog();
            RenderSystem.setShaderFog(renderer.getDummyFog());

            // Draw layers
            cameraRenderState.initialized = true;
            cameraRenderState.orientation = rotation;
            cameraRenderState.pos = new Vec3(lastRenderPos.x, lastRenderPos.y, lastRenderPos.z);
            cameraRenderState.blockPos = BlockPos.containing(cameraRenderState.pos);
            mc.gameRenderer.lighting().setupFor(Lighting.Entry.LEVEL);
            renderer.prepareRender(cameraRenderState, framebuffer);
            renderer.renderBlocks();
            if (PreviewOptions.RENDER_TILE) {
                renderer.renderBlockEntities(new PoseStack(), tickDelta);
            }

            RenderSystem.setGlobalSettingsUniform(previousGlobalUniform);
            RenderSystem.setShaderFog(previousFog);
            RenderSystem.restoreProjectionMatrix();
            modelViewStack.popMatrix();
        }

        private void close() {
            if (renderer != null) {
                renderer.close();
                renderer = null;
            }
            if (projectionMatrix != null) {
                projectionMatrix.close();
                projectionMatrix = null;
            }
            if (globalUniform != null) {
                globalUniform.close();
                globalUniform = null;
            }
        }

    }

}