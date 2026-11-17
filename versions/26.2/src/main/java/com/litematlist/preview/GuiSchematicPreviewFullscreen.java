package com.litematlist.preview;

import com.litematlist.config.Configs;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.render.GuiContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class GuiSchematicPreviewFullscreen extends GuiBase {

    private final SchematicPreviewWidget preview;

    public GuiSchematicPreviewFullscreen(Screen parent, SchematicPreviewWidget preview) {
        setParent(parent);
        this.preview = preview;
    }

    @Override
    public void initGui() {
        super.initGui();
        addWidget(preview);
        preview.setFullScreen(true);
    }

    @Override
    protected void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTicks) {
        preview.renderPreviewAndOverlay(ctx, 0, 0, width, height);
        // 在原版 SchematicPreview 全屏界面里该提示由模组的 mixin 绘制，这里内置实现
        if (Configs.Generic.SHOW_ESC_HINT.getBooleanValue()) {
            Minecraft client = Minecraft.getInstance();
            String text = "按ESC退出";
            int textWidth = client.font.width(text);
            int screenWidth = client.getWindow().getGuiScaledWidth();
            int x = (screenWidth - textWidth) / 2;
            int y = 8;
            ctx.fill(x - 4, y - 2, x + textWidth + 4, y + 12, 0x80000000);
            ctx.drawString(client.font, text, x, y, 0xFFFFFFFF);
        }
    }

    @Override
    public void onClose() {
        mc.gui.setScreen(getParent());
    }

    @Override
    public void removed() {
        preview.setFullScreen(false);
        preview.removed();
    }

}