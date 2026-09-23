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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Path;

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
    public java.io.File getDefaultDirectory() {
        return new java.io.File(MinecraftClient.getInstance().runDirectory, "litematlist");
    }

    @Override
    protected WidgetSchematicBrowser createListWidget(int listX, int listY) {
        return new WidgetSchematicBrowser(listX, listY, 100, 100, this, this.getSelectionListener()) {
            @Override
            protected java.io.FileFilter getFileFilter() {
                return FILE_FILTER;
            }

            @Override
            protected java.io.File getRootDirectory() {
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
        public IGuiIcon getIconForFile(java.io.File file) {
            String name = file.getName().toLowerCase();
            if (name.endsWith(".txt")) return TXT_GUI_ICON;
            if (name.endsWith(".json")) return JSON_GUI_ICON;
            if (name.endsWith(".csv")) return CSV_GUI_ICON;
            return delegate.getIconForFile(file);
        }
    }

    private static final IGuiIcon TXT_GUI_ICON = new SimpleGuiIcon(
            Identifier.of("litematlist", "textures/gui/file_txt.png"), 12, 12);
    private static final IGuiIcon JSON_GUI_ICON = new SimpleGuiIcon(
            Identifier.of("litematlist", "textures/gui/file_json.png"), 12, 12);
    private static final IGuiIcon CSV_GUI_ICON = new SimpleGuiIcon(
            Identifier.of("litematlist", "textures/gui/file_csv.png"), 12, 12);

    private record SimpleGuiIcon(Identifier texture, int width, int height) implements IGuiIcon {
        @Override
        public void renderAt(int x, int y, float zLevel, boolean disabled, boolean selected) {
            // MaLib 的 drawTexturedRect 假定纹理为 256x256（1/256 像素缩放），
            // 直接用它绘制 32x32 的小图标只会采到左上角几个像素（这里是白色），
            // 因此改用归一化纹理坐标直接绘制完整纹理。
            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
            buffer.vertex(x, y + height, zLevel).texture(0.0F, 1.0F);
            buffer.vertex(x + width, y + height, zLevel).texture(1.0F, 1.0F);
            buffer.vertex(x + width, y, zLevel).texture(1.0F, 0.0F);
            buffer.vertex(x, y, zLevel).texture(0.0F, 0.0F);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }
        @Override public int getU() { return 0; }
        @Override public int getV() { return 0; }
        @Override public int getWidth() { return width; }
        @Override public int getHeight() { return height; }
        @Override public Identifier getTexture() { return texture; }
    }

    /** 只接受 .txt 和 .json 文件的过滤器（目录由基类处理，此处不重复接受） */
    private static final java.io.FileFilter FILE_FILTER = new java.io.FileFilter() {
        @Override
        public boolean accept(java.io.File entry) {
            if (entry.isDirectory()) {
                return false; // 目录由基类的 DIRECTORY_FILTER 处理，避免重复
            }
            String name = entry.getName().toLowerCase();
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
    public void render(DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(drawContext, mouseX, mouseY, partialTicks);
        super.render(drawContext, mouseX, mouseY, partialTicks);
        drawPreviewInfo(drawContext);
    }

    private void drawPreviewInfo(DrawContext drawContext) {
        WidgetSchematicBrowser widget = getListWidget();
        if (widget == null) return;

        // 右侧预览面板 —— 固定右对齐
        int panelW = 178;
        int panelX = this.width - panelW;
        int panelY = 20;
        int panelH = this.height - 80;

        // 面板背景
        drawContext.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0x60000000);
        drawContext.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + panelH - 1, 0x40000000);

        // 面板标题
        drawContext.drawCenteredTextWithShadow(this.textRenderer,
                I18n.tr("litematlist.title.preview_info"),
                panelX + panelW / 2, panelY + 6, 0xFFFFFFFF);
        drawContext.fill(panelX + 8, panelY + 20, panelX + panelW - 8, panelY + 21, 0x40FFFFFF);

        DirectoryEntry entry = widget.getLastSelectedEntry();
        if (entry != null && entry.getType() == DirectoryEntryType.FILE) {
            String name = entry.getDisplayName();
            String lowerName = name.toLowerCase();
            if (lowerName.endsWith(".txt") || lowerName.endsWith(".json") || lowerName.endsWith(".csv")) {
                java.io.File pathFile = entry.getFullPath();
            Path path = pathFile.toPath();
            if (previewInfo == null || !path.equals(lastPreviewPath)) {
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
                    drawContext.drawText(this.textRenderer, nameText, x, y, color, false);

                    drawContext.drawText(this.textRenderer,
                            I18n.tr("litematlist.preview.total") + ": " + previewInfo.totalItems(),
                            x, y + 13, color, false);
                    drawContext.drawText(this.textRenderer,
                            I18n.tr("litematlist.preview.missing") + ": " + previewInfo.totalMissing(),
                            x, y + 26, color, false);
                } else {
                    drawContext.drawText(this.textRenderer,
                            I18n.tr("litematlist.import.error"),
                            panelX + 12, panelY + 30, 0xFFFF5555, false);
                }
            }
        } else {
            previewInfo = null;
            drawContext.drawText(this.textRenderer,
                    I18n.tr("litematlist.preview.name") + ": ---",
                    panelX + 12, panelY + 30, 0xFF666666, false);
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

        java.io.File pathFile = entry.getFullPath();
        Path path = pathFile.toPath();
        LitematListMod.LOGGER.info("[TxtBrowserScreen] 开始导入: {}", path);

        MaterialListImporter.ImportResult result = MaterialListImporter.importFromFile(path);
        if (result != null) {
                MaterialListScreen.addEntry(result.schematicName(), path, false);
                MaterialDetailScreen.cacheMaterialList(path, result.materials());
                LitematListMod.LOGGER.info("[TxtBrowserScreen] 导入成功: {}, {} 种物品", result.schematicName(), result.materials().size());
                MinecraftClient.getInstance().setScreen(new MaterialListScreen());
            } else {
                LitematListMod.LOGGER.warn("[TxtBrowserScreen] 导入失败: 格式不符或物品验证失败: {}", path);
                MaterialListScreen.setError(I18n.tr("litematlist.import.error"));
                MinecraftClient.getInstance().setScreen(new MaterialListScreen());
            }
    }
}