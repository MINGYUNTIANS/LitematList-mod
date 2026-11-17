package com.litematlist.gui;

import com.litematlist.I18n;
import com.litematlist.LitematListMod;
import com.litematlist.MaterialListImporter;
import fi.dy.masa.litematica.gui.GuiSchematicBrowserBase;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicBrowser;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.IFileBrowserIconProvider;
import fi.dy.masa.malilib.gui.interfaces.IGuiIcon;
import fi.dy.masa.malilib.gui.widgets.WidgetDirectoryEntry;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntry;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntryType;
//#if MC == 12108 || MC == 12110 || MC == 12111
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.FileFilter;
//#endif
import net.minecraft.client.MinecraftClient;
//#if MC == 12104
//$$import net.minecraft.client.render.RenderLayer;
//#endif
//#if MC == 12111
//$$import net.minecraft.client.input.KeyInput;
//#endif
//#if MC == 12111
//$$import fi.dy.masa.malilib.render.GuiContext;
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.gui.DrawContext;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$import net.minecraft.client.render.BufferBuilder;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$import net.minecraft.client.render.BufferRenderer;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$import net.minecraft.client.render.GameRenderer;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$import com.mojang.blaze3d.systems.RenderSystem;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$import net.minecraft.client.render.Tessellator;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$import net.minecraft.client.render.VertexFormat;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$import net.minecraft.client.render.VertexFormats;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.gl.RenderPipelines;
//#endif
//#if MC == 12110
import net.minecraft.client.input.KeyInput;
//#endif
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Path;
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.gui.DrawableHelper;
//#endif
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.util.math.MatrixStack;
//#endif

/**
 * txt 材料列表文件浏览器 —— 继承 Litematica 的 GuiSchematicBrowserBase，
 * 浏览 litematlist 文件夹中的 .txt 文件，显示预览信息。
 * 只显示 .txt 文件，过滤掉 .litematic 等其他格式。
 */
public class TxtBrowserScreen extends GuiSchematicBrowserBase {

    private final GuiBase parent;
    private MaterialListImporter.PreviewInfo previewInfo;
    private Path lastPreviewPath;

    public TxtBrowserScreen(GuiBase parent) {
        super(10, 20);
        this.parent = parent;
        this.title = I18n.tr("litematlist.title.import_txt");
    }

    @Override
    public String getBrowserContext() {
        return "litematlist_txt_browser";
    }

    @Override
//#if MC == 12108 || MC == 12110 || MC == 12111
    public Path getDefaultDirectory() {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$    public java.io.File getDefaultDirectory() {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        return MinecraftClient.getInstance().runDirectory.toPath().resolve("litematlist");
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$        return new java.io.File(MinecraftClient.getInstance().runDirectory, "litematlist");
//#endif
    }

    @Override
    protected WidgetSchematicBrowser createListWidget(int listX, int listY) {
        return new WidgetSchematicBrowser(listX, listY, 100, 100, this, this.getSelectionListener()) {
            @Override
//#if MC == 12108 || MC == 12110 || MC == 12111
            protected FileFilter getFileFilter() {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            protected java.io.FileFilter getFileFilter() {
//#endif
                return FILE_FILTER;
            }

            @Override
//#if MC == 12108 || MC == 12110 || MC == 12111
            protected Path getRootDirectory() {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            protected java.io.File getRootDirectory() {
//#endif
                return getDefaultDirectory();
            }

            @Override
            protected WidgetDirectoryEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, DirectoryEntry entry) {
                // 使用自定义图标提供器替换默认的 Litematica 图标
                IFileBrowserIconProvider customProvider = new IconProviderWrapper(this.iconProvider);
                return new WidgetDirectoryEntry(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry),
                        isOdd, entry, listIndex, this, customProvider);
            }
        };
    }

    /**
     * 包装 IFileBrowserIconProvider，为 .txt/.json 文件返回自定义图标，其余委托给原始提供器。
     */
    private static class IconProviderWrapper implements IFileBrowserIconProvider {
        private final IFileBrowserIconProvider delegate;

        IconProviderWrapper(IFileBrowserIconProvider delegate) {
            this.delegate = delegate;
        }

        @Override public IGuiIcon getIconRoot() { return delegate.getIconRoot(); }
        @Override public IGuiIcon getIconUp() { return delegate.getIconUp(); }
        @Override public IGuiIcon getIconCreateDirectory() { return delegate.getIconCreateDirectory(); }
        @Override public IGuiIcon getIconSearch() { return delegate.getIconSearch(); }
        @Override public IGuiIcon getIconDirectory() { return delegate.getIconDirectory(); }

        @Override
//#if MC == 12108 || MC == 12110 || MC == 12111
        public IGuiIcon getIconForFile(Path file) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$        public IGuiIcon getIconForFile(java.io.File file) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            String name = file.getFileName().toString().toLowerCase();
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            String name = file.getName().toLowerCase();
//#endif
            if (name.endsWith(".txt")) return TXT_GUI_ICON;
            if (name.endsWith(".json")) return JSON_GUI_ICON;
            if (name.endsWith(".csv")) return CSV_GUI_ICON;
            return delegate.getIconForFile(file);
        }
    }

    private static final IGuiIcon TXT_GUI_ICON = new SimpleGuiIcon(
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            Identifier.of("litematlist", "textures/gui/file_txt.png"), 12, 12);
//#elseif MC == 11701
//$$            new Identifier("litematlist", "textures/gui/file_txt.png"), 12, 12);
//#endif
    private static final IGuiIcon JSON_GUI_ICON = new SimpleGuiIcon(
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            Identifier.of("litematlist", "textures/gui/file_json.png"), 12, 12);
//#elseif MC == 11701
//$$            new Identifier("litematlist", "textures/gui/file_json.png"), 12, 12);
//#endif
    private static final IGuiIcon CSV_GUI_ICON = new SimpleGuiIcon(
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            Identifier.of("litematlist", "textures/gui/file_csv.png"), 12, 12);
//#elseif MC == 11701
//$$            new Identifier("litematlist", "textures/gui/file_csv.png"), 12, 12);
//#endif

    private record SimpleGuiIcon(Identifier texture, int width, int height) implements IGuiIcon {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        @Override public int getU() { return 0; }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        @Override public int getV() { return 0; }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        @Override public int getWidth() { return width; }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        @Override public int getHeight() { return height; }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        @Override public Identifier getTexture() { return texture; }
//#endif
        @Override
//#if MC == 12108 || MC == 12110
        public void renderAt(DrawContext drawContext, int x, int y, float zLevel, boolean enabled, boolean selected) {
//#elseif MC == 12104
//$$        public void renderAt(int x, int y, float zLevel, boolean enabled, boolean selected, DrawContext drawContext) {
//#elseif MC == 12111
//$$        public void renderAt(GuiContext drawContext, int x, int y, float zLevel, boolean enabled, boolean selected) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, width, height, width, height);
//#elseif MC == 12104
//$$            drawContext.drawTexture(RenderLayer::getGuiTextured, texture, x, y, 0, 0, width, height, width, height);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$        public void renderAt(int x, int y, float zLevel, boolean disabled, boolean selected) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$            // MaLib 的 drawTexturedRect 假定纹理为 256x256（1/256 像素缩放），
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            // 直接用它绘制小图标只会采到左上角几个像素（这里是白色），
//#elseif MC == 12101
//$$            // 直接用它绘制 32x32 的小图标只会采到左上角几个像素（这里是白色），
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$            // 因此改用归一化纹理坐标直接绘制完整纹理。
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            // 注意：每个顶点末尾必须调用 .next() 提交顶点数据，否则整批顶点为空、图标不可见。
//#endif
//#if MC == 11701
//$$            RenderSystem.setShader(GameRenderer::getPositionTexShader);
//#elseif MC == 11904 || MC == 12001 || MC == 12101
//$$            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$            RenderSystem.applyModelViewMatrix();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$            RenderSystem.setShaderTexture(0, texture);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$            RenderSystem.enableBlend();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$            RenderSystem.defaultBlendFunc();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101
//$$            Tessellator tessellator = Tessellator.getInstance();
//#endif
//#if MC == 12101
//$$            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
//#endif
//#if MC == 12101
//$$            buffer.vertex(x, y + height, zLevel).texture(0.0F, 1.0F);
//#endif
//#if MC == 12101
//$$            buffer.vertex(x + width, y + height, zLevel).texture(1.0F, 1.0F);
//#endif
//#if MC == 12101
//$$            buffer.vertex(x + width, y, zLevel).texture(1.0F, 0.0F);
//#endif
//#if MC == 12101
//$$            buffer.vertex(x, y, zLevel).texture(0.0F, 0.0F);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            BufferBuilder buffer = tessellator.getBuffer();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            buffer.vertex(x, y + height, zLevel).texture(0.0F, 1.0F).next();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            buffer.vertex(x + width, y + height, zLevel).texture(1.0F, 1.0F).next();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            buffer.vertex(x + width, y, zLevel).texture(1.0F, 0.0F).next();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            buffer.vertex(x, y, zLevel).texture(0.0F, 0.0F).next();
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101
//$$            BufferRenderer.drawWithGlobalProgram(buffer.end());
//#endif
//#if MC == 11701
//$$            buffer.end();
//#endif
//#if MC == 11701
//$$            BufferRenderer.draw(buffer);
//#endif
        }
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        @Override public int getU() { return 0; }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        @Override public int getV() { return 0; }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        @Override public int getWidth() { return width; }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        @Override public int getHeight() { return height; }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$        @Override public Identifier getTexture() { return texture; }
//#endif
    }

    /** 只接受 .txt 和 .json 文件的过滤器（目录由基类处理，此处不重复接受） */
//#if MC == 12108 || MC == 12110 || MC == 12111
    private static final FileFilter FILE_FILTER = new FileFilter() {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$    private static final java.io.FileFilter FILE_FILTER = new java.io.FileFilter() {
//#endif
        @Override
//#if MC == 12108 || MC == 12110 || MC == 12111
        public boolean accept(Path entry) throws IOException {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$        public boolean accept(java.io.File entry) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            if (java.nio.file.Files.isDirectory(entry)) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            if (entry.isDirectory()) {
//#endif
                return false; // 目录由基类的 DIRECTORY_FILTER 处理，避免重复
            }
//#if MC == 12108 || MC == 12110 || MC == 12111
            String name = entry.getFileName().toString().toLowerCase();
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            String name = entry.getName().toLowerCase();
//#endif
            return name.endsWith(".txt") || name.endsWith(".json") || name.endsWith(".csv");
        }
    };

    @Override
    public void initGui() {
        super.initGui();

        int buttonY = this.height - 38;

        ButtonGeneric importBtn = new ButtonGeneric(this.width / 2 - 190, buttonY, 120, 20,
                I18n.tr("litematlist.button.import_this_file"));
        importBtn.setRenderDefaultBackground(true);
        this.addButton(importBtn, (IButtonActionListener) (b, mb) -> importSelected());

        ButtonGeneric backBtn = new ButtonGeneric(this.width / 2 + 60, buttonY, 120, 20,
                I18n.tr("litematlist.button.back"));
        backBtn.setRenderDefaultBackground(true);
        this.addButton(backBtn, (IButtonActionListener) (b, mb) ->
                MinecraftClient.getInstance().setScreen(parent));
    }

    @Override
//#if MC == 12110 || MC == 12111
    public boolean keyPressed(KeyInput keyInput) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
//#endif
//#if MC == 12110 || MC == 12111
        if (keyInput.key() == 256) { // ESC
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        if (keyCode == 256) { // ESC
//#endif
            // 优先让文件浏览器处理：搜索框开启时 ESC 先关闭搜索，而非退出本界面
            WidgetSchematicBrowser widget = getListWidget();
//#if MC == 12110 || MC == 12111
            if (widget != null && widget.onKeyTyped(keyInput)) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            if (widget != null && widget.onKeyTyped(keyCode, scanCode, modifiers)) {
//#endif
                return true;
            }
            // 搜索框未消费 ESC → 返回上一级菜单
            MinecraftClient.getInstance().setScreen(parent);
            return true;
        }
//#if MC == 12110 || MC == 12111
        return super.keyPressed(keyInput);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        return super.keyPressed(keyCode, scanCode, modifiers);
//#endif
    }

    @Override
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public void render(DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
//#elseif MC == 11701 || MC == 11904
//$$    public void render(MatrixStack matrices, int mouseX, int mouseY, float partialTicks) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        this.renderBackground(drawContext, mouseX, mouseY, partialTicks);
//#elseif MC == 11701 || MC == 11904
//$$        this.renderBackground(matrices);
//#elseif MC == 12001
//$$        this.renderBackground(drawContext);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        super.render(drawContext, mouseX, mouseY, partialTicks);
//#elseif MC == 11701 || MC == 11904
//$$        super.render(matrices, mouseX, mouseY, partialTicks);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawPreviewInfo(drawContext);
//#elseif MC == 11701 || MC == 11904
//$$        drawPreviewInfo(matrices);
//#endif
    }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void drawPreviewInfo(DrawContext drawContext) {
//#elseif MC == 11701 || MC == 11904
//$$    private void drawPreviewInfo(MatrixStack matrices) {
//#endif
        WidgetSchematicBrowser widget = getListWidget();
        if (widget == null) return;

        // 右侧预览面板 —— 固定右对齐
        int panelW = 178;
        int panelX = this.width - panelW;
        int panelY = 20;
        int panelH = this.height - 80;

        // 面板背景
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0x60000000);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, panelX, panelY, panelX + panelW, panelY + panelH, 0x60000000);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + panelH - 1, 0x40000000);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, panelX + 1, panelY + 1, panelX + panelW - 1, panelY + panelH - 1, 0x40000000);
//#endif

        // 面板标题
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawCenteredTextWithShadow(this.textRenderer,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                I18n.tr("litematlist.title.preview_info"),
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                panelX + panelW / 2, panelY + 6, 0xFFFFFFFF);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(panelX + 8, panelY + 20, panelX + panelW - 8, panelY + 21, 0x40FFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$        this.textRenderer.drawWithShadow(matrices, I18n.tr("litematlist.title.preview_info"), panelX + panelW / 2 - this.textRenderer.getWidth(I18n.tr("litematlist.title.preview_info")) / 2, panelY + 6, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, panelX + 8, panelY + 20, panelX + panelW - 8, panelY + 21, 0x40FFFFFF);
//#endif

        DirectoryEntry entry = widget.getLastSelectedEntry();
        if (entry != null && entry.getType() == DirectoryEntryType.FILE) {
            String name = entry.getDisplayName();
            String lowerName = name.toLowerCase();
            if (lowerName.endsWith(".txt") || lowerName.endsWith(".json") || lowerName.endsWith(".csv")) {
//#if MC == 12108 || MC == 12110 || MC == 12111
                Path path = entry.getFullPath();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                java.io.File pathFile = entry.getFullPath();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            Path path = pathFile.toPath();
//#elseif MC == 12101 || MC == 12104
//$$                Path path = pathFile.toPath();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                if (previewInfo == null || !path.equals(lastPreviewPath)) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            if (previewInfo == null || !path.equals(lastPreviewPath)) {
//#endif
                    previewInfo = MaterialListImporter.parsePreview(path);
                    lastPreviewPath = path;
                }
                if (previewInfo != null) {
                    int x = panelX + 12;
                    int y = panelY + 30;
                    int maxTextWidth = panelW - 24; // 左右各留 12px
                    int color = 0xFFAAAAAA;

                    String nameText = I18n.tr("litematlist.preview.name") + ": " + previewInfo.schematicName();
                    nameText = trimToWidth(nameText, maxTextWidth);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    drawContext.drawText(this.textRenderer, nameText, x, y, color, false);
//#elseif MC == 11701 || MC == 11904
//$$                    this.textRenderer.draw(matrices, nameText, x, y, color);
//#endif

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    drawContext.drawText(this.textRenderer,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            I18n.tr("litematlist.preview.total") + ": " + previewInfo.totalItems(),
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            x, y + 13, color, false);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    drawContext.drawText(this.textRenderer,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            I18n.tr("litematlist.preview.missing") + ": " + previewInfo.totalMissing(),
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            x, y + 26, color, false);
//#endif
//#if MC == 11701 || MC == 11904
//$$                    this.textRenderer.draw(matrices, I18n.tr("litematlist.preview.total") + ": " + previewInfo.totalItems(), x, y + 13, color);
//#endif
//#if MC == 11701 || MC == 11904
//$$                    this.textRenderer.draw(matrices, I18n.tr("litematlist.preview.missing") + ": " + previewInfo.totalMissing(), x, y + 26, color);
//#endif
                } else {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    drawContext.drawText(this.textRenderer,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            I18n.tr("litematlist.import.error"),
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                            panelX + 12, panelY + 30, 0xFFFF5555, false);
//#endif
//#if MC == 11701 || MC == 11904
//$$                    this.textRenderer.draw(matrices, I18n.tr("litematlist.import.error"), panelX + 12, panelY + 30, 0xFFFF5555);
//#endif
                }
            }
        } else {
            previewInfo = null;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawText(this.textRenderer,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    I18n.tr("litematlist.preview.name") + ": ---",
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    panelX + 12, panelY + 30, 0xFF666666, false);
//#endif
//#if MC == 11701 || MC == 11904
//$$            this.textRenderer.draw(matrices, I18n.tr("litematlist.preview.name") + ": ---", panelX + 12, panelY + 30, 0xFF666666);
//#endif
        }
    }

    private String trimToWidth(String text, int maxWidth) {
        if (this.textRenderer.getWidth(text) > maxWidth) {
            return this.textRenderer.trimToWidth(text, maxWidth - 10) + "...";
        }
        return text;
    }

    private void importSelected() {
        WidgetSchematicBrowser widget = getListWidget();
        if (widget == null) {
            LitematListMod.LOGGER.warn("[TxtBrowserScreen] widget is null");
            return;
        }

        DirectoryEntry entry = widget.getLastSelectedEntry();
        if (entry == null) {
            // 尝试从当前条目列表中获取
            var entries = widget.getCurrentEntries();
            if (entries != null && !entries.isEmpty()) {
                for (var e : entries) {
                    if (e.getType() == DirectoryEntryType.FILE) {
                        String lowerName = e.getDisplayName().toLowerCase();
                        if (lowerName.endsWith(".txt") || lowerName.endsWith(".json") || lowerName.endsWith(".csv")) {
                            entry = e;
                            break;
                        }
                    }
                }
            }
        }

        if (entry == null) {
            LitematListMod.LOGGER.warn("[TxtBrowserScreen] 未选中任何条目");
            return;
        }

        if (entry.getType() != DirectoryEntryType.FILE) {
            LitematListMod.LOGGER.warn("[TxtBrowserScreen] 选中的不是文件: {}", entry.getDisplayName());
            return;
        }

        String name = entry.getDisplayName();
        String lowerName = name.toLowerCase();
        if (!lowerName.endsWith(".txt") && !lowerName.endsWith(".json") && !lowerName.endsWith(".csv")) {
            LitematListMod.LOGGER.warn("[TxtBrowserScreen] 选中文件不是 .txt 或 .json 或 .csv: {}", name);
            return;
        }

//#if MC == 12108 || MC == 12110 || MC == 12111
        Path path = entry.getFullPath();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$        java.io.File pathFile = entry.getFullPath();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$        Path path = pathFile.toPath();
//#endif
        LitematListMod.LOGGER.info("[TxtBrowserScreen] 开始导入: {}", path);

        MaterialListImporter.ImportResult result = MaterialListImporter.importFromFile(path);
        if (result != null) {
                MaterialListScreen.addEntry(result.schematicName(), path, false);
                MaterialDataStore.cacheMaterialList(path, result.materials());
                LitematListMod.LOGGER.info("[TxtBrowserScreen] 导入成功: {}, {} 种物品", result.schematicName(), result.materials().size());
                MinecraftClient.getInstance().setScreen(new MaterialListScreen());
            } else {
                LitematListMod.LOGGER.warn("[TxtBrowserScreen] 导入失败: 格式不符或物品验证失败: {}", path);
                MaterialListScreen.setError(I18n.tr("litematlist.import.error"));
                MinecraftClient.getInstance().setScreen(new MaterialListScreen());
            }
    }
}