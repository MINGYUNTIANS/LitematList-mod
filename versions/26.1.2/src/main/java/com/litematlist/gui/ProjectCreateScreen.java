package com.litematlist.gui;

import com.litematlist.I18n;
import com.litematlist.LitematListMod;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import fi.dy.masa.malilib.render.GuiContext;

/**
 * 新建项目界面 —— 输入项目名称。
 */
public class ProjectCreateScreen extends GuiBase {

    private final GuiBase parent;
    private EditBox textField;
    private boolean duplicateName = false;

    public ProjectCreateScreen(GuiBase parent) {
        super();
        this.parent = parent;
        this.title = I18n.tr("litematlist.title.new_project");
    }

    @Override
    public void initGui() {
        super.initGui();

        int fieldWidth = 300;
        int fieldX = this.width / 2 - fieldWidth / 2;
        int fieldY = this.height / 2 - 20;

        this.textField = new EditBox(this.font, fieldX, fieldY, fieldWidth, 20, Component.empty());
        this.textField.setMaxLength(50);
        this.textField.setBordered(false);
        this.addRenderableWidget(this.textField);

        int buttonY = this.height - 38;

        ButtonGeneric confirmBtn = new ButtonGeneric(this.width / 2 - 85, buttonY, 80, 20,
                I18n.tr("litematlist.button.confirm"));
        confirmBtn.setRenderDefaultBackground(true);
        this.addButton(confirmBtn, (IButtonActionListener) (b, mb) -> confirmCreate());

        ButtonGeneric cancelBtn = new ButtonGeneric(this.width / 2 + 5, buttonY, 80, 20,
                I18n.tr("litematlist.button.cancel"));
        cancelBtn.setRenderDefaultBackground(true);
        this.addButton(cancelBtn, (IButtonActionListener) (b, mb) ->
                Minecraft.getInstance().setScreenAndShow(new MaterialListScreen()));
    }

    private void confirmCreate() {
        String name = this.textField.getValue().trim();
        if (name.isEmpty()) return;
        // 阻止保留名称（与导出目录重名）：rawmaterials / project / treetable
        if ("rawmaterials".equalsIgnoreCase(name) || "project".equalsIgnoreCase(name) || "treetable".equalsIgnoreCase(name)) {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                Component msg1 = Component.literal("§cMINGYUNTIANS阻止了你创建此文件夹");
                Component link = Component.literal(" §b§u点击进入我的空间")
                        .copy().setStyle(net.minecraft.network.chat.Style.EMPTY
                                .withClickEvent(new net.minecraft.network.chat.ClickEvent.OpenUrl(java.net.URI.create("https://space.bilibili.com/388648945")))
                                .withUnderlined(true));
                client.player.sendSystemMessage(msg1.copy().append(link));
            }
            LitematListMod.LOGGER.warn("阻止创建名为 'rawmaterials' 的项目文件夹");
            return;
        }
        // 检测同名项目
        for (MaterialListScreen.LoadedEntry entry : MaterialListScreen.getLoadedEntries()) {
            if (entry.isProject && entry.name().equals(name)) {
                duplicateName = true;
                return; // 已存在同名项目，不创建，下次渲染显示红字提示
            }
        }
        duplicateName = false;
        MaterialListScreen.createProject(name);
        Minecraft.getInstance().setScreenAndShow(new MaterialListScreen());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks) {
        GuiContext ctx = GuiContext.fromGuiGraphics(gfx);
        this.extractBackground(gfx, mouseX, mouseY, partialTicks);
        super.extractRenderState(gfx, mouseX, mouseY, partialTicks);

        // 提示文字
        String hint = I18n.tr("litematlist.title.enter_project_name");
        ctx.drawCenteredString(this.font, hint,
                this.width / 2, this.height / 2 - 40, 0xFFFFFFFF);

        // 重名提示
        if (duplicateName) {
            ctx.drawCenteredString(this.font, "该文件夹名称已存在",
                    this.width / 2, this.height / 2 - 52, 0xFFFF5555);
        }

        if (this.textField != null) {
            int x1 = this.textField.getX() - 2;
            int y1 = this.textField.getY() - 2;
            int x2 = this.textField.getX() + this.textField.getWidth() + 2;
            int y2 = this.textField.getY() + this.textField.getHeight() + 2;
            // 白色边框
            ctx.fill(x1, y1, x2, y1 + 1, 0xFFFFFFFF);
            ctx.fill(x1, y2 - 1, x2, y2, 0xFFFFFFFF);
            ctx.fill(x1, y1, x1 + 1, y2, 0xFFFFFFFF);
            ctx.fill(x2 - 1, y1, x2, y2, 0xFFFFFFFF);
            // 半透明填充
            ctx.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0x4D000000);
            ctx.fill(this.textField.getX(), this.textField.getY(),
                    this.textField.getX() + this.textField.getWidth(),
                    this.textField.getY() + this.textField.getHeight(), 0x4D333333);

            String text = this.textField.getValue();
            int textX = this.textField.getX() + 4;
            int textY = this.textField.getY() + (this.textField.getHeight() - 8) / 2;
            ctx.drawString(this.font, text, textX, textY, 0xFFFFFFFF);

            if (this.textField.isFocused() && System.currentTimeMillis() % 1000 < 500) {
                int cursorPos = this.textField.getCursorPosition();
                String beforeCursor = text.substring(0, Math.min(cursorPos, text.length()));
                int cursorX = textX + this.font.width(beforeCursor);
                ctx.fill(cursorX, this.textField.getY() + 2,
                        cursorX + 1, this.textField.getY() + this.textField.getHeight() - 2, 0xFFFFFFFF);
            }
        }
    }

    @Override
    public void removed() {
        super.removed();
        if (this.textField != null) {
            this.textField.setFocused(false);
        }
    }
}

