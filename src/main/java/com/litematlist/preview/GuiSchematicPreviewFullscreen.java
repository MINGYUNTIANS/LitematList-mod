package com.litematlist.preview;

import com.litematlist.config.Configs;
import fi.dy.masa.malilib.gui.GuiBase;
//#if MC == 12111
//$$import fi.dy.masa.malilib.render.GuiContext;
//#endif
import net.minecraft.client.MinecraftClient;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110
import net.minecraft.client.gui.DrawContext;
//#elseif MC == 11701 || MC == 11904
//$$import net.minecraft.client.gui.DrawableHelper;
//#endif
import net.minecraft.client.gui.screen.Screen;
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.util.math.MatrixStack;
//#endif

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
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110
    protected void drawContents(DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
//#elseif MC == 11701 || MC == 11904
//$$    protected void drawContents(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
//#elseif MC == 12111
//$$    protected void drawContents(GuiContext drawContext, int mouseX, int mouseY, float partialTicks) {
//#endif
//#if MC == 12104
//$$        drawContext.getMatrices().push();
//#endif
//#if MC == 12104
//$$        drawContext.getMatrices().translate(0.0F, 0.0F, 1000.0F);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        preview.renderPreviewAndOverlay(drawContext, 0, 0, width, height);
//#elseif MC == 11701 || MC == 11904
//$$        preview.renderPreviewAndOverlay(matrixStack, 0, 0, width, height);
//#endif
//#if MC == 12104
//$$        drawContext.getMatrices().pop();
//#endif
        // 在原版 SchematicPreview 全屏界面里该提示由模组的 mixin 绘制，这里内置实现
        if (Configs.Generic.SHOW_ESC_HINT.getBooleanValue()) {
            MinecraftClient client = MinecraftClient.getInstance();
            String text = "按ESC退出";
            int textWidth = client.textRenderer.getWidth(text);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            int x = (drawContext.getScaledWindowWidth() - textWidth) / 2;
//#endif
//#if MC == 11701 || MC == 11904
//$$            int screenWidth = client.getWindow().getScaledWidth();
//#endif
//#if MC == 11701 || MC == 11904
//$$            int x = (screenWidth - textWidth) / 2;
//#endif
            int y = 8;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(x - 4, y - 2, x + textWidth + 4, y + 12, 0x80000000);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrixStack, x - 4, y - 2, x + textWidth + 4, y + 12, 0x80000000);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawText(client.textRenderer, text, x, y, 0xFFFFFFFF, false);
//#elseif MC == 11701 || MC == 11904
//$$            client.textRenderer.draw(matrixStack, text, x, y, 0xFFFFFFFF);
//#endif
        }
    }

    @Override
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public void close() {
//#elseif MC == 11701
//$$    public void onClose() {
//#endif
        mc.setScreen(getParent());
    }

    @Override
    public void removed() {
        preview.setFullScreen(false);
        preview.removed();
    }

}
//#if MC == 12108 || MC == 12110 || MC == 12111

//#endif