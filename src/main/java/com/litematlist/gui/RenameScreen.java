package com.litematlist.gui;

import com.litematlist.I18n;
import com.litematlist.LitematListMod;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import java.net.URI;

/**
 * 重命名界面 —— 允许玩家修改 txt 材料列表条目的显示名称。
 */
public class RenameScreen extends GuiBase {

    private final GuiBase parent;
    private final int entryIndex;
    private final String currentName;
    private TextFieldWidget textField;
    private boolean duplicateName = false;

    public RenameScreen(GuiBase parent, int entryIndex, String currentName) {
        super();
        this.parent = parent;
        this.entryIndex = entryIndex;
        this.currentName = currentName;
        this.title = I18n.tr("litematlist.title.rename");
    }

    @Override
    public void initGui() {
        super.initGui();

        // 输入框宽度自适应名字长度，保证玩家能看到整体名字
        int nameWidth = this.textRenderer.getWidth(this.currentName) + 24;
        int fieldWidth = Math.min(Math.max(300, this.width - 80), Math.max(300, nameWidth));
        int fieldX = this.width / 2 - fieldWidth / 2;
        int fieldY = this.height / 2 - 20;

        this.textField = new TextFieldWidget(this.textRenderer, fieldX, fieldY, fieldWidth, 20, Text.empty());
        this.textField.setText(this.currentName);
        this.textField.setMaxLength(100);
        this.textField.setCursorToEnd(true);
        this.textField.setDrawsBackground(false);
        this.addSelectableChild(this.textField);

        int buttonY = this.height - 38;

        ButtonGeneric confirmBtn = new ButtonGeneric(this.width / 2 - 85, buttonY, 80, 20,
                I18n.tr("litematlist.button.confirm"));
        confirmBtn.setRenderDefaultBackground(true);
        this.addButton(confirmBtn, (IButtonActionListener) (b, mb) -> confirmRename());

        ButtonGeneric cancelBtn = new ButtonGeneric(this.width / 2 + 5, buttonY, 80, 20,
                I18n.tr("litematlist.button.cancel"));
        cancelBtn.setRenderDefaultBackground(true);
        this.addButton(cancelBtn, (IButtonActionListener) (b, mb) ->
                MinecraftClient.getInstance().setScreen(parent));
    }

    private void confirmRename() {
        String newName = this.textField.getText().trim();
        if (newName.isEmpty()) return;
        if (newName.equals(currentName)) { MinecraftClient.getInstance().setScreen(parent); return; }
        // 阻止重命名为保留名称（项目文件夹）：rawmaterials / project / treetable
        if ("rawmaterials".equalsIgnoreCase(newName) || "project".equalsIgnoreCase(newName) || "treetable".equalsIgnoreCase(newName)) {
            // 检查当前条目是否为项目文件夹
            if (entryIndex >= 0 && entryIndex < MaterialListScreen.getLoadedEntries().size()) {
                MaterialListScreen.LoadedEntry cur = MaterialListScreen.getLoadedEntries().get(entryIndex);
                if (cur.isProject) {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player != null) {
                        Text msg1 = Text.literal("§cMINGYUNTIANS阻止了你创建此文件夹");
                        Text link = Text.literal(" §b§u点击进入我的空间")
                                .styled(style -> style
                                        .withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.OPEN_URL, "https://space.bilibili.com/388648945"))
                                        .withUnderline(true));
                        client.player.sendMessage(msg1.copy().append(link), false);
                    }
                    LitematListMod.LOGGER.warn("阻止重命名项目文件夹为 'rawmaterials'");
                    return;
                }
            }
        }
        // 检测同名项目文件夹
        for (MaterialListScreen.LoadedEntry entry : MaterialListScreen.getLoadedEntries()) {
            if (entry.isProject && entry.name().equals(newName)) {
                duplicateName = true;
                return;
            }
        }
        duplicateName = false;
        MaterialListScreen.renameEntry(this.entryIndex, newName);
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(drawContext, mouseX, mouseY, partialTicks);
        super.render(drawContext, mouseX, mouseY, partialTicks);

        // 重名提示
        if (duplicateName) {
            drawContext.drawCenteredTextWithShadow(this.textRenderer, "该文件夹名称已存在",
                    this.width / 2, this.height / 2 - 52, 0xFFFF5555);
        }

        if (this.textField != null) {
            // 半透明文本框背景（不透明度降低 70%）
            drawContext.fill(this.textField.getX() - 1, this.textField.getY() - 1,
                    this.textField.getX() + this.textField.getWidth() + 1,
                    this.textField.getY() + this.textField.getHeight() + 1, 0x4D000000);
            drawContext.fill(this.textField.getX(), this.textField.getY(),
                    this.textField.getX() + this.textField.getWidth(),
                    this.textField.getY() + this.textField.getHeight(), 0x4D333333);

            // 完全手动渲染白色文字，不调用 textField.render() 避免黑色文字重叠
            // 名字过长时按比例缩小字号，确保整体名字完整显示
            String text = this.textField.getText();
            int textX = this.textField.getX() + 4;
            int textY = this.textField.getY() + (this.textField.getHeight() - 8) / 2;
            int textW = Math.max(1, this.textRenderer.getWidth(text));
            float scale = Math.min(1.0f, (this.textField.getWidth() - 10) / (float) textW);
            drawContext.getMatrices().push();
            drawContext.getMatrices().translate(textX, textY, 0);
            drawContext.getMatrices().scale(scale, scale, 1.0f);
            drawContext.drawText(this.textRenderer, text, 0, 0, 0xFFFFFFFF, false);
            drawContext.getMatrices().pop();

            // 手动绘制光标（闪烁）
            if (this.textField.isFocused() && System.currentTimeMillis() % 1000 < 500) {
                int cursorPos = this.textField.getCursor();
                String beforeCursor = text.substring(0, Math.min(cursorPos, text.length()));
                int cursorX = textX + (int) (this.textRenderer.getWidth(beforeCursor) * scale);
                drawContext.fill(cursorX, this.textField.getY() + 2,
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
