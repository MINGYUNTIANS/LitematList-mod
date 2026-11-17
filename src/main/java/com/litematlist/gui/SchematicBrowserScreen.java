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
//#if MC == 12111
//$$import net.minecraft.client.input.KeyInput;
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.gui.DrawContext;
//#endif
//#if MC == 12110
import net.minecraft.client.input.KeyInput;
//#endif

//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12108 || MC == 12110 || MC == 12111
import java.nio.file.Path;
//#elseif MC == 12101 || MC == 12104
//$$import java.io.File;
//#endif
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.util.math.MatrixStack;
//#endif

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
//#if MC == 12108 || MC == 12110 || MC == 12111
    public Path getDefaultDirectory() {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    public java.io.File getDefaultDirectory() {
//#elseif MC == 12101 || MC == 12104
//$$    public File getDefaultDirectory() {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
        return MinecraftClient.getInstance().runDirectory.toPath().resolve("schematics");
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        return new java.io.File(MinecraftClient.getInstance().runDirectory, "schematics");
//#elseif MC == 12101 || MC == 12104
//$$        return new File(MinecraftClient.getInstance().runDirectory, "schematics");
//#endif
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
//#if MC == 12108 || MC == 12110 || MC == 12111
            Path path = entry.getFullPath();
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            java.io.File pathFile = entry.getFullPath();
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            MaterialListScreen.addEntry(name, path);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            MaterialListScreen.addEntry(name, pathFile.toPath());
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            LitematListMod.LOGGER.info("已添加原理图: {} ({})", name, path);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$            LitematListMod.LOGGER.info("已添加原理图: {} ({})", name, pathFile);
//#endif
            MinecraftClient.getInstance().setScreen(new MaterialListScreen());
        }
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
        try {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            super.render(drawContext, mouseX, mouseY, partialTicks);
//#elseif MC == 11701 || MC == 11904
//$$            super.render(matrices, mouseX, mouseY, partialTicks);
//#endif
        } catch (Exception e) {
            LitematListMod.LOGGER.warn("[SchematicBrowserScreen] SchematicPreview 渲染预览时出错，已跳过: {}", e.getMessage());
        }
    }
}
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif