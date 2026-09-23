package com.litematlist.gui;

import com.litematlist.I18n;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import fi.dy.masa.malilib.render.GuiContext;

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
            Minecraft.getInstance().setScreenAndShow(new MaterialListScreen());
        });

        ButtonGeneric cancelBtn = new ButtonGeneric(this.width / 2 + 5, buttonY, 80, 20,
                I18n.tr("litematlist.button.cancel"));
        cancelBtn.setRenderDefaultBackground(true);
        this.addButton(cancelBtn, (IButtonActionListener) (b, mb) ->
                Minecraft.getInstance().setScreenAndShow(new MaterialListScreen()));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks) {
        GuiContext ctx = GuiContext.fromGuiGraphics(gfx);
        this.extractBackground(gfx, mouseX, mouseY, partialTicks);
        super.extractRenderState(gfx, mouseX, mouseY, partialTicks);

        ctx.drawCenteredString(this.font, this.title,
                this.width / 2, this.height / 2 - 30, 0xFFFF5555);
        ctx.drawCenteredString(this.font, this.message,
                this.width / 2, this.height / 2 - 10, 0xFFFFFFFF);
    }
}