package com.litematlist.gui;

import com.litematlist.I18n;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * 确认对话框 —— 带确认/取消按钮。
 */
public class ConfirmScreen extends GuiBase {

    private final GuiBase parent;
    private final String message;
    private final Runnable onConfirm;

    public ConfirmScreen(GuiBase parent, String title, String message, Runnable onConfirm) {
        super();
        this.parent = parent;
        this.message = message;
        this.onConfirm = onConfirm;
        this.title = title;
    }

    @Override
    public void initGui() {
        super.initGui();

        int buttonY = this.height - 38;

        ButtonGeneric confirmBtn = new ButtonGeneric(this.width / 2 - 85, buttonY, 80, 20,
                I18n.tr("litematlist.button.confirm"));
        confirmBtn.setRenderDefaultBackground(true);
        this.addButton(confirmBtn, (IButtonActionListener) (b, mb) -> {
            onConfirm.run();
            MinecraftClient.getInstance().setScreen(new MaterialListScreen());
        });

        ButtonGeneric cancelBtn = new ButtonGeneric(this.width / 2 + 5, buttonY, 80, 20,
                I18n.tr("litematlist.button.cancel"));
        cancelBtn.setRenderDefaultBackground(true);
        this.addButton(cancelBtn, (IButtonActionListener) (b, mb) ->
                MinecraftClient.getInstance().setScreen(new MaterialListScreen()));
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(drawContext, mouseX, mouseY, partialTicks);
        super.render(drawContext, mouseX, mouseY, partialTicks);

        drawContext.drawCenteredTextWithShadow(this.textRenderer, this.title,
                this.width / 2, this.height / 2 - 30, 0xFFFF5555);
        drawContext.drawCenteredTextWithShadow(this.textRenderer, this.message,
                this.width / 2, this.height / 2 - 10, 0xFFFFFFFF);
    }
}