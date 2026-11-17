package com.litematlist.gui;
//#if MC == 11701
//$$import net.minecraft.text.LiteralText;
//#endif

import com.litematlist.I18n;
import com.litematlist.LitematListMod;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.MinecraftClient;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.gui.DrawContext;
//#endif
//#if MC == 12111
//$$import net.minecraft.client.input.KeyInput;
//#endif
import net.minecraft.client.gui.widget.TextFieldWidget;
//#if MC == 12110
import net.minecraft.client.input.KeyInput;
//#endif
import net.minecraft.text.Text;
import java.net.URI;
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.gui.DrawableHelper;
//#endif
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.util.math.MatrixStack;
//#endif

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

//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        this.textField = new TextFieldWidget(this.textRenderer, fieldX, fieldY, fieldWidth, 20, Text.empty());
//#elseif MC == 11701
//$$        this.textField = new TextFieldWidget(this.textRenderer, fieldX, fieldY, fieldWidth, 20, LiteralText.EMPTY);
//#endif
        this.textField.setText(this.currentName);
        this.textField.setMaxLength(100);
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        this.textField.setCursorToEnd(true);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        this.textField.setCursorToEnd();
//#endif
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
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        Text msg1 = Text.literal("§cMINGYUNTIANS阻止了你创建此文件夹");
//#elseif MC == 11701
//$$                        Text msg1 = new LiteralText("§cMINGYUNTIANS阻止了你创建此文件夹");
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        Text link = Text.literal(" §b§u点击进入我的空间")
//#elseif MC == 11701
//$$                        Text link = new LiteralText(" §b§u点击进入我的空间")
//#endif
                                .styled(style -> style
//#if MC == 12108 || MC == 12110 || MC == 12111
                                        .withClickEvent(new net.minecraft.text.ClickEvent.OpenUrl(URI.create("https://space.bilibili.com/388648945")))
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                                        .withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.OPEN_URL, "https://space.bilibili.com/388648945"))
//#elseif MC == 12101 || MC == 12104
//$$                                        .withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.OPEN_URL, URI.create("https://space.bilibili.com/388648945").toString()))
//#endif
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
//#if MC == 12110 || MC == 12111
    public boolean keyPressed(KeyInput keyInput) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
//#endif
//#if MC == 12110 || MC == 12111
        if (keyInput.key() == 256) { // ESC：返回上一级菜单
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        if (keyCode == 256) { // ESC：返回上一级菜单
//#endif
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

        // 重名提示
        if (duplicateName) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawCenteredTextWithShadow(this.textRenderer, "该文件夹名称已存在",
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    this.width / 2, this.height / 2 - 52, 0xFFFF5555);
//#endif
//#if MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, "该文件夹名称已存在", this.width / 2 - this.textRenderer.getWidth("该文件夹名称已存在") / 2, this.height / 2 - 52, 0xFFFF5555);
//#endif
        }

        if (this.textField != null) {
            // 半透明文本框背景（不透明度降低 70%）
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(this.textField.getX() - 1, this.textField.getY() - 1,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    this.textField.getX() + this.textField.getWidth() + 1,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    this.textField.getY() + this.textField.getHeight() + 1, 0x4D000000);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(this.textField.getX(), this.textField.getY(),
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    this.textField.getX() + this.textField.getWidth(),
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    this.textField.getY() + this.textField.getHeight(), 0x4D333333);
//#endif
//#if MC == 11701
//$$            DrawableHelper.fill(matrices, this.textField.x - 1, this.textField.y - 1, this.textField.x + this.textField.getWidth() + 1, this.textField.y + this.textField.getHeight() + 1, 0x4D000000);
//#elseif MC == 11904
//$$            DrawableHelper.fill(matrices, this.textField.getX() - 1, this.textField.getY() - 1, this.textField.getX() + this.textField.getWidth() + 1, this.textField.getY() + this.textField.getHeight() + 1, 0x4D000000);
//#endif
//#if MC == 11701
//$$            DrawableHelper.fill(matrices, this.textField.x, this.textField.y, this.textField.x + this.textField.getWidth(), this.textField.y + this.textField.getHeight(), 0x4D333333);
//#elseif MC == 11904
//$$            DrawableHelper.fill(matrices, this.textField.getX(), this.textField.getY(), this.textField.getX() + this.textField.getWidth(), this.textField.getY() + this.textField.getHeight(), 0x4D333333);
//#endif

            // 完全手动渲染白色文字，不调用 textField.render() 避免黑色文字重叠
            // 名字过长时按比例缩小字号，确保整体名字完整显示
            String text = this.textField.getText();
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            int textX = this.textField.getX() + 4;
//#elseif MC == 11701
//$$            int textX = this.textField.x + 4;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            int textY = this.textField.getY() + (this.textField.getHeight() - 8) / 2;
//#elseif MC == 11701
//$$            int textY = this.textField.y + (this.textField.getHeight() - 8) / 2;
//#endif
            int textW = Math.max(1, this.textRenderer.getWidth(text));
            float scale = Math.min(1.0f, (this.textField.getWidth() - 10) / (float) textW);
//#if MC == 12108 || MC == 12110 || MC == 12111
            drawContext.getMatrices().pushMatrix();
//#elseif MC == 11701 || MC == 11904
//$$            matrices.push();
//#elseif MC == 12001 || MC == 12101 || MC == 12104
//$$            drawContext.getMatrices().push();
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            drawContext.getMatrices().translate((float) textX, (float) textY);
//#elseif MC == 11701 || MC == 11904
//$$            matrices.translate(textX, textY, 0);
//#elseif MC == 12001 || MC == 12101 || MC == 12104
//$$            drawContext.getMatrices().translate(textX, textY, 0);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            drawContext.getMatrices().scale(scale, scale);
//#elseif MC == 11701 || MC == 11904
//$$            matrices.scale(scale, scale, 1.0f);
//#elseif MC == 12001 || MC == 12101 || MC == 12104
//$$            drawContext.getMatrices().scale(scale, scale, 1.0f);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawText(this.textRenderer, text, 0, 0, 0xFFFFFFFF, false);
//#elseif MC == 11701 || MC == 11904
//$$            this.textRenderer.draw(matrices, text, 0, 0, 0xFFFFFFFF);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            drawContext.getMatrices().popMatrix();
//#elseif MC == 11701 || MC == 11904
//$$            matrices.pop();
//#elseif MC == 12001 || MC == 12101 || MC == 12104
//$$            drawContext.getMatrices().pop();
//#endif

            // 手动绘制光标（闪烁）
            if (this.textField.isFocused() && System.currentTimeMillis() % 1000 < 500) {
                int cursorPos = this.textField.getCursor();
                String beforeCursor = text.substring(0, Math.min(cursorPos, text.length()));
                int cursorX = textX + (int) (this.textRenderer.getWidth(beforeCursor) * scale);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.fill(cursorX, this.textField.getY() + 2,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        cursorX + 1, this.textField.getY() + this.textField.getHeight() - 2, 0xFFFFFFFF);
//#endif
//#if MC == 11701
//$$                DrawableHelper.fill(matrices, cursorX, this.textField.y + 2, cursorX + 1, this.textField.y + this.textField.getHeight() - 2, 0xFFFFFFFF);
//#elseif MC == 11904
//$$                DrawableHelper.fill(matrices, cursorX, this.textField.getY() + 2, cursorX + 1, this.textField.getY() + this.textField.getHeight() - 2, 0xFFFFFFFF);
//#endif
            }
        }
    }

    @Override
    public void removed() {
        super.removed();
        if (this.textField != null) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            this.textField.setFocused(false);
//#elseif MC == 11701
//$$            this.setFocused(null);
//#endif
        }
    }
}
