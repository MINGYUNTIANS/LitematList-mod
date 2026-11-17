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
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import java.net.URI;
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.gui.DrawableHelper;
//#endif
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.util.math.MatrixStack;
//#endif

/**
 * 新建项目界面 —— 输入项目名称。
 */
public class ProjectCreateScreen extends GuiBase {

    private final GuiBase parent;
    private TextFieldWidget textField;
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

//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        this.textField = new TextFieldWidget(this.textRenderer, fieldX, fieldY, fieldWidth, 20, Text.empty());
//#elseif MC == 11701
//$$        this.textField = new TextFieldWidget(this.textRenderer, fieldX, fieldY, fieldWidth, 20, LiteralText.EMPTY);
//#endif
        this.textField.setMaxLength(50);
        this.textField.setDrawsBackground(false);
        this.addSelectableChild(this.textField);

        int buttonY = this.height - 38;

        ButtonGeneric confirmBtn = new ButtonGeneric(this.width / 2 - 85, buttonY, 80, 20,
                I18n.tr("litematlist.button.confirm"));
        confirmBtn.setRenderDefaultBackground(true);
        this.addButton(confirmBtn, (IButtonActionListener) (b, mb) -> confirmCreate());

        ButtonGeneric cancelBtn = new ButtonGeneric(this.width / 2 + 5, buttonY, 80, 20,
                I18n.tr("litematlist.button.cancel"));
        cancelBtn.setRenderDefaultBackground(true);
        this.addButton(cancelBtn, (IButtonActionListener) (b, mb) ->
                MinecraftClient.getInstance().setScreen(new MaterialListScreen()));
    }

    private void confirmCreate() {
        String name = this.textField.getText().trim();
        if (name.isEmpty()) return;
        // 阻止保留名称（与导出目录重名）：rawmaterials / project / treetable
        if ("rawmaterials".equalsIgnoreCase(name) || "project".equalsIgnoreCase(name) || "treetable".equalsIgnoreCase(name)) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                Text msg1 = Text.literal("§cMINGYUNTIANS阻止了你创建此文件夹");
//#elseif MC == 11701
//$$                Text msg1 = new LiteralText("§cMINGYUNTIANS阻止了你创建此文件夹");
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                Text link = Text.literal(" §b§u点击进入我的空间")
//#elseif MC == 11701
//$$                Text link = new LiteralText(" §b§u点击进入我的空间")
//#endif
                        .styled(style -> style
//#if MC == 12108 || MC == 12110 || MC == 12111
                                .withClickEvent(new net.minecraft.text.ClickEvent.OpenUrl(URI.create("https://space.bilibili.com/388648945")))
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                                .withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.OPEN_URL, "https://space.bilibili.com/388648945"))
//#elseif MC == 12101 || MC == 12104
//$$                                .withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.OPEN_URL, URI.create("https://space.bilibili.com/388648945").toString()))
//#endif
                                .withUnderline(true));
                client.player.sendMessage(msg1.copy().append(link), false);
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
        MinecraftClient.getInstance().setScreen(new MaterialListScreen());
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

        // 提示文字
        String hint = I18n.tr("litematlist.title.enter_project_name");
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawCenteredTextWithShadow(this.textRenderer, hint,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                this.width / 2, this.height / 2 - 40, 0xFFFFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$        this.textRenderer.drawWithShadow(matrices, hint, this.width / 2 - this.textRenderer.getWidth(hint) / 2, this.height / 2 - 40, 0xFFFFFFFF);
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
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            int x1 = this.textField.getX() - 2;
//#elseif MC == 11701
//$$            int x1 = this.textField.x - 2;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            int y1 = this.textField.getY() - 2;
//#elseif MC == 11701
//$$            int y1 = this.textField.y - 2;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            int x2 = this.textField.getX() + this.textField.getWidth() + 2;
//#elseif MC == 11701
//$$            int x2 = this.textField.x + this.textField.getWidth() + 2;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            int y2 = this.textField.getY() + this.textField.getHeight() + 2;
//#elseif MC == 11701
//$$            int y2 = this.textField.y + this.textField.getHeight() + 2;
//#endif
            // 白色边框
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(x1, y1, x2, y1 + 1, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, x1, y1, x2, y1 + 1, 0xFFFFFFFF);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(x1, y2 - 1, x2, y2, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, x1, y2 - 1, x2, y2, 0xFFFFFFFF);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(x1, y1, x1 + 1, y2, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, x1, y1, x1 + 1, y2, 0xFFFFFFFF);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(x2 - 1, y1, x2, y2, 0xFFFFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, x2 - 1, y1, x2, y2, 0xFFFFFFFF);
//#endif
            // 半透明填充
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0x4D000000);
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
//#if MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0x4D000000);
//#endif
//#if MC == 11701
//$$            DrawableHelper.fill(matrices, this.textField.x, this.textField.y, this.textField.x + this.textField.getWidth(), this.textField.y + this.textField.getHeight(), 0x4D333333);
//#elseif MC == 11904
//$$            DrawableHelper.fill(matrices, this.textField.getX(), this.textField.getY(), this.textField.getX() + this.textField.getWidth(), this.textField.getY() + this.textField.getHeight(), 0x4D333333);
//#endif

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
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawText(this.textRenderer, text, textX, textY, 0xFFFFFFFF, false);
//#elseif MC == 11701 || MC == 11904
//$$            this.textRenderer.draw(matrices, text, textX, textY, 0xFFFFFFFF);
//#endif

            if (this.textField.isFocused() && System.currentTimeMillis() % 1000 < 500) {
                int cursorPos = this.textField.getCursor();
                String beforeCursor = text.substring(0, Math.min(cursorPos, text.length()));
                int cursorX = textX + this.textRenderer.getWidth(beforeCursor);
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
