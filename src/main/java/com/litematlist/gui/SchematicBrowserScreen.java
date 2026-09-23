package com.litematlist.gui;

import com.litematlist.I18n;
import com.litematlist.LitematListMod;
import fi.dy.masa.litematica.gui.GuiSchematicBrowserBase;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicBrowser;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntry;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntryType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.nio.file.Path;

/**
 * 原理图文件浏览器 —— 继承 Litematica 的 GuiSchematicBrowserBase，
 * 复用 Litematica 原生文件浏览器界面（含三个方形图标按钮），
 * 并自动兼容 SchematicPreview 模组的预览注入。
 */
public class SchematicBrowserScreen extends GuiSchematicBrowserBase {

    private final GuiBase parent;

    public SchematicBrowserScreen(GuiBase parent) {
        super(10, 20);
        this.parent = parent;
        this.title = "选择原理图 - LitematList";
    }

    @Override
    public String getBrowserContext() {
        return "litematlist_schematic_browser";
    }

    @Override
    public java.io.File getDefaultDirectory() {
        return new java.io.File(MinecraftClient.getInstance().runDirectory, "schematics");
    }

    @Override
    public void initGui() {
        super.initGui();

        int buttonY = this.height - 38;

        // 「选择此原理图」按钮
        ButtonGeneric selectBtn = new ButtonGeneric(this.width / 2 - 190, buttonY, 120, 20, I18n.tr("litematlist.button.select_schematic"));
        selectBtn.setRenderDefaultBackground(true);
        this.addButton(selectBtn, (IButtonActionListener) (b, mb) -> selectSchematic());

        // 「返回」按钮
        ButtonGeneric backBtn = new ButtonGeneric(this.width / 2 + 60, buttonY, 120, 20, I18n.tr("litematlist.button.back"));
        backBtn.setRenderDefaultBackground(true);
        this.addButton(backBtn, (IButtonActionListener) (b, mb) ->
                MinecraftClient.getInstance().setScreen(parent));
    }

    private void selectSchematic() {
        WidgetSchematicBrowser widget = getListWidget();
        if (widget == null) return;

        DirectoryEntry entry = widget.getLastSelectedEntry();
        if (entry != null && entry.getType() == DirectoryEntryType.FILE) {
            String name = entry.getDisplayName();
            // 去掉 .litematic 后缀
            if (name.endsWith(".litematic")) {
                name = name.substring(0, name.length() - ".litematic".length());
            }
            java.io.File pathFile = entry.getFullPath();
            MaterialListScreen.addEntry(name, pathFile.toPath());
            LitematListMod.LOGGER.info("已添加原理图: {} ({})", name, pathFile);
            MinecraftClient.getInstance().setScreen(new MaterialListScreen());
        }
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(drawContext, mouseX, mouseY, partialTicks);
        try {
            super.render(drawContext, mouseX, mouseY, partialTicks);
        } catch (Exception e) {
            LitematListMod.LOGGER.warn("[SchematicBrowserScreen] SchematicPreview 渲染预览时出错，已跳过: {}", e.getMessage());
        }
    }
}
