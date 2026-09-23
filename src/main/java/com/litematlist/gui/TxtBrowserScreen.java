package com.litematlist.gui;

import com.litematlist.I18n;
import com.litematlist.LitematListMod;
import com.litematlist.MaterialListImporter;
import fi.dy.masa.litematica.gui.GuiSchematicBrowserBase;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicBrowser;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetDirectoryEntry;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntry;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntryType;
import fi.dy.masa.malilib.render.GuiContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.resources.Identifier;
import org.apache.commons.lang3.tuple.Pair;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.GpuSampler;
import org.joml.Matrix3x2f;

import java.nio.file.Path;

/**
 * txt/JSON/CSV 材料列表文件浏览器（26.2 API）。
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
    public Path getDefaultDirectory() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("litematlist");
    }

    @Override
    protected WidgetSchematicBrowser createListWidget(int listX, int listY) {
        return new WidgetSchematicBrowser(listX, listY, 100, 100, this, this.getSelectionListener()) {
            @Override
            protected WidgetFileBrowserBase.FileFilter getFileFilter() {
                return FILE_FILTER;
            }

            @Override
            protected Path getRootDirectory() {
                return getDefaultDirectory();
            }

            @Override
            protected WidgetDirectoryEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, DirectoryEntry entry) {
                return new WidgetDirectoryEntry(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry),
                        isOdd, entry, listIndex, this, new CustomIconProvider(this.iconProvider)) {};
            }
        };
    }

    /** 自定义图标提供器，为 txt/json/csv 返回自定义图标，其余委托原提供器 */
    private static class CustomIconProvider implements fi.dy.masa.malilib.gui.interfaces.IFileBrowserIconProvider {
        private final fi.dy.masa.malilib.gui.interfaces.IFileBrowserIconProvider delegate;
        CustomIconProvider(fi.dy.masa.malilib.gui.interfaces.IFileBrowserIconProvider d) { this.delegate = d; }
        @Override public fi.dy.masa.malilib.gui.interfaces.IGuiIcon getIconRoot() { return delegate.getIconRoot(); }
        @Override public fi.dy.masa.malilib.gui.interfaces.IGuiIcon getIconUp() { return delegate.getIconUp(); }
        @Override public fi.dy.masa.malilib.gui.interfaces.IGuiIcon getIconCreateDirectory() { return delegate.getIconCreateDirectory(); }
        @Override public fi.dy.masa.malilib.gui.interfaces.IGuiIcon getIconSearch() { return delegate.getIconSearch(); }
        @Override public fi.dy.masa.malilib.gui.interfaces.IGuiIcon getIconDirectory() { return delegate.getIconDirectory(); }
        @Override public fi.dy.masa.malilib.gui.interfaces.IGuiIcon getIconForFile(Path file) {
            String name = file.getFileName().toString().toLowerCase();
            if (name.endsWith(".txt")) return TXT_GUI_ICON;
            if (name.endsWith(".json")) return JSON_GUI_ICON;
            if (name.endsWith(".csv")) return CSV_GUI_ICON;
            return delegate.getIconForFile(file);
        }
    }

    private static final fi.dy.masa.malilib.gui.interfaces.IGuiIcon TXT_GUI_ICON = new FileIcon(Identifier.fromNamespaceAndPath("litematlist", "textures/gui/file_txt.png"), 12, 12);
    private static final fi.dy.masa.malilib.gui.interfaces.IGuiIcon JSON_GUI_ICON = new FileIcon(Identifier.fromNamespaceAndPath("litematlist", "textures/gui/file_json.png"), 12, 12);
    private static final fi.dy.masa.malilib.gui.interfaces.IGuiIcon CSV_GUI_ICON = new FileIcon(Identifier.fromNamespaceAndPath("litematlist", "textures/gui/file_csv.png"), 12, 12);

    private static class FileIcon implements fi.dy.masa.malilib.gui.interfaces.IGuiIcon {
        private final Identifier texture;
        private final int w, h;
        FileIcon(Identifier t, int w, int h) { this.texture = t; this.w = w; this.h = h; }
        @Override public int getWidth() { return w; }
        @Override public int getHeight() { return h; }
        @Override public int getU() { return 0; }
        @Override public int getV() { return 0; }
        @Override public Identifier getTexture() { return texture; }
        @Override
        public void renderAt(GuiContext ctx, int x, int y, float zLevel, boolean enabled, boolean selected) {
            Pair<GpuTextureView, GpuSampler> pair = ctx.bindTexture(texture);
            if (pair == null) return;
            TextureSetup texSetup = TextureSetup.singleTexture(pair.getLeft(), pair.getRight());
            BlitRenderState blit = new BlitRenderState(
                    RenderPipelines.GUI_TEXTURED,
                    texSetup,
                    new Matrix3x2f(),
                    x, y, x + w, y + h,
                    0.0f, 1.0f, 0.0f, 1.0f,
                    0xFFFFFFFF,
                    null
            );
            ctx.addSimpleElementToCurrentLayer(blit);
        }
    }

    /** 只接受 .txt, .json 和 .csv 文件的过滤器（目录由基类处理，此处不重复接受） */
    private static final WidgetFileBrowserBase.FileFilter FILE_FILTER = new WidgetFileBrowserBase.FileFilter() {
        @Override
        public boolean accept(Path entry) throws java.io.IOException {
            if (java.nio.file.Files.isDirectory(entry)) return false;
            String name = entry.getFileName().toString().toLowerCase();
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
                Minecraft.getInstance().setScreenAndShow(parent));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks) {
        GuiContext ctx = GuiContext.fromGuiGraphics(gfx);
        this.extractBackground(gfx, mouseX, mouseY, partialTicks);
        super.extractRenderState(gfx, mouseX, mouseY, partialTicks);
        drawPreviewInfo(ctx, mouseX, mouseY);
    }

    private void drawPreviewInfo(GuiContext ctx, int mouseX, int mouseY) {
        WidgetSchematicBrowser widget = getListWidget();
        if (widget == null) return;

        int panelW = 178;
        int panelX = this.width - panelW;
        int panelY = 20;
        int panelH = this.height - 80;

        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0x60000000);
        ctx.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + panelH - 1, 0x40000000);

        ctx.drawCenteredString(this.font,
                I18n.tr("litematlist.title.preview_info"),
                panelX + panelW / 2, panelY + 6, 0xFFFFFFFF);
        ctx.fill(panelX + 8, panelY + 20, panelX + panelW - 8, panelY + 21, 0x40FFFFFF);

        DirectoryEntry entry = widget.getLastSelectedEntry();
        if (entry != null && entry.getType() == DirectoryEntryType.FILE) {
            String name = entry.getDisplayName();
            String lowerName = name.toLowerCase();
            if (lowerName.endsWith(".txt") || lowerName.endsWith(".json") || lowerName.endsWith(".csv")) {
                Path path = entry.getFullPath();
                if (previewInfo == null || !path.equals(lastPreviewPath)) {
                    previewInfo = MaterialListImporter.parsePreview(path);
                    lastPreviewPath = path;
                }
                if (previewInfo != null) {
                    int x = panelX + 12;
                    int y = panelY + 30;
                    int maxTextWidth = panelW - 24;
                    int color = 0xFFAAAAAA;

                    String nameText = I18n.tr("litematlist.preview.name") + ": " + previewInfo.schematicName();
                    if (this.font.width(nameText) > maxTextWidth) {
                        nameText = this.font.plainSubstrByWidth(nameText, maxTextWidth - 10) + "...";
                    }
                    ctx.drawString(this.font, nameText, x, y, color);

                    ctx.drawString(this.font,
                            I18n.tr("litematlist.preview.total") + ": " + previewInfo.totalItems(),
                            x, y + 13, color);
                    ctx.drawString(this.font,
                            I18n.tr("litematlist.preview.missing") + ": " + previewInfo.totalMissing(),
                            x, y + 26, color);
                } else {
                    ctx.drawString(this.font,
                            I18n.tr("litematlist.import.error"),
                            panelX + 12, panelY + 30, 0xFFFF5555);
                }
            }
        } else {
            previewInfo = null;
            ctx.drawString(this.font,
                    I18n.tr("litematlist.preview.name") + ": ---",
                    panelX + 12, panelY + 30, 0xFF666666);
        }
    }

    private void importSelected() {
        WidgetSchematicBrowser widget = getListWidget();
        if (widget == null) return;

        DirectoryEntry entry = widget.getLastSelectedEntry();
        if (entry == null) {
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

        if (entry == null || entry.getType() != DirectoryEntryType.FILE) return;

        String name = entry.getDisplayName();
        String lowerName = name.toLowerCase();
        if (!lowerName.endsWith(".txt") && !lowerName.endsWith(".json") && !lowerName.endsWith(".csv")) return;

        Path path = entry.getFullPath();
        LitematListMod.LOGGER.info("[TxtBrowserScreen] 开始导入: {}", path);

        MaterialListImporter.ImportResult result = MaterialListImporter.importFromFile(path);
        if (result != null) {
            MaterialListScreen.addEntry(result.schematicName(), path);
            MaterialDetailScreen.cacheMaterialList(path, result.materials());
            LitematListMod.LOGGER.info("[TxtBrowserScreen] 导入成功: {}, {} 种物品", result.schematicName(), result.materials().size());
            Minecraft.getInstance().setScreenAndShow(new MaterialListScreen());
        } else {
            LitematListMod.LOGGER.warn("[TxtBrowserScreen] 导入失败: {}", path);
            MaterialListScreen.setError(I18n.tr("litematlist.import.error"));
            Minecraft.getInstance().setScreenAndShow(new MaterialListScreen());
        }
    }
}