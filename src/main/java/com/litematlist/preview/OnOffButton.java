package com.litematlist.preview;

//#if MC == 11701 || MC == 11904
//$$import com.mojang.blaze3d.systems.RenderSystem;
//#endif
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
//#if MC == 11701
//$$import net.minecraft.client.MinecraftClient;
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.gl.RenderPipelines;
//#endif
//#if MC == 12110 || MC == 12111
import net.minecraft.client.gui.Click;
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.gui.DrawContext;
//#elseif MC == 11701 || MC == 11904
//$$import net.minecraft.client.gui.DrawableHelper;
//#endif
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.gui.tooltip.Tooltip;
//#endif
import net.minecraft.client.gui.widget.ClickableWidget;
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.util.math.MatrixStack;
//#endif
//#if MC == 12104
//$$import net.minecraft.client.render.RenderLayer;
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.screen.ScreenTexts;
//#endif
import net.minecraft.text.Text;
//#if MC == 11701
//$$import net.minecraft.text.TranslatableText;
//#endif
import net.minecraft.util.Identifier;

public class OnOffButton extends ClickableWidget {

    private final Text name;
    private final BooleanConsumer listener;
    private final Identifier onSprite;
    private final Identifier onSpriteFocused;
    private final Identifier offSprite;
    private final Identifier offSpriteFocused;
    private boolean on;

    public OnOffButton(int width, int height, Text name, BooleanConsumer listener, Identifier onSprite, Identifier onSpriteFocused, Identifier offSprite, Identifier offSpriteFocused) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        super(0, 0, width, height, ScreenTexts.EMPTY);
//#elseif MC == 11701
//$$        super(0, 0, width, height, Text.of(""));
//#endif
        this.name = name;
        this.listener = listener;
        this.onSprite = onSprite;
        this.onSpriteFocused = onSpriteFocused;
        this.offSprite = offSprite;
        this.offSpriteFocused = offSpriteFocused;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        updateTooltip();
//#endif
    }

    @Override
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
//#elseif MC == 12001
//$$    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        context.drawGuiTexture(
//#elseif MC == 12001
//$$        context.drawTexture(
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                RenderPipelines.GUI_TEXTURED,
//#elseif MC == 12104
//$$                RenderLayer::getGuiTextured,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                on ? (isHovered() ? offSpriteFocused : offSprite) : (isHovered() ? onSpriteFocused : onSprite),
//#endif
//#if MC == 11701 || MC == 11904
//$$    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
//#endif
//#if MC == 11701
//$$        // 1.17.1 无独立 GUI 精灵图集，纹理需手动绑定；ClickableWidget 无 getX()/getY()，用字段 x/y
//#elseif MC == 11904
//$$        // MC 1.19.4: the sprite has to be bound manually, DrawableHelper.drawTexture only sets the shader
//#endif
//#if MC == 11701 || MC == 11904
//$$        RenderSystem.setShaderTexture(0, on ? (isHovered() ? offSpriteFocused : offSprite) : (isHovered() ? onSpriteFocused : onSprite));
//#endif
//#if MC == 11701 || MC == 11904
//$$        DrawableHelper.drawTexture(
//#endif
//#if MC == 11701 || MC == 11904
//$$                matrices,
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                getX(), getY(),
//#elseif MC == 11701
//$$                this.x, this.y,
//#endif
//#if MC == 11701 || MC == 11904
//$$                0,
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                0.0F, 0.0F,
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                getWidth(), getHeight(),
//#endif
                getWidth(), getHeight()
        );
//#if MC == 11701
//$$        // 1.17.1 的 ClickableWidget 无内置 tooltip，需手动绘制
//#endif
//#if MC == 11701
//$$        if (isHovered()) {
//#endif
//#if MC == 11701
//$$            renderTooltipText(matrices, mouseX, mouseY);
//#endif
//#if MC == 11701
//$$        }
//#endif
//#if MC == 11701
//$$    }
//#endif
//#if MC == 11701
//$$
//#endif
//#if MC == 11701
//$$    private void renderTooltipText(MatrixStack matrices, int mouseX, int mouseY) {
//#endif
//#if MC == 11701
//$$        Text tooltip = new TranslatableText(on ? "litematlist.preview.disable" : "litematlist.preview.enable", name);
//#endif
//#if MC == 11701
//$$        MinecraftClient mc = MinecraftClient.getInstance();
//#endif
//#if MC == 11701
//$$        int textWidth = mc.textRenderer.getWidth(tooltip);
//#endif
//#if MC == 11701
//$$        int textX = mouseX + 10;
//#endif
//#if MC == 11701
//$$        int textY = mouseY - 10;
//#endif
//#if MC == 11701
//$$        DrawableHelper.fill(matrices, textX - 3, textY - 3, textX + textWidth + 3, textY + 9, 0xF0100010);
//#endif
//#if MC == 11701
//$$        mc.textRenderer.drawWithShadow(matrices, tooltip, (float) textX, (float) textY, 0xFFFFFFFF);
//#endif
    }

    @Override
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
//#elseif MC == 11701
//$$    public void appendNarrations(NarrationMessageBuilder builder) {
//#endif
        appendDefaultNarrations(builder);
    }

    @Override
//#if MC == 12110 || MC == 12111
    public void onClick(Click click, boolean doubled) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$    public void onClick(double mouseX, double mouseY) {
//#endif
        on = !on;
        listener.accept(on);
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        updateTooltip();
//#endif
    }

    /** 同步开关状态（不触发回调），用于把按钮状态与当前界面状态对齐 */
    public void setOn(boolean on) {
        this.on = on;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        updateTooltip();
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    }
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void updateTooltip() {
//#endif
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        setTooltip(Tooltip.of(Text.translatable(on ? "litematlist.preview.disable" : "litematlist.preview.enable", name)));
//#endif
    }

}
//#if MC == 11701
//$$
//#endif
//#if MC == 12101 || MC == 12104
//$$
//#endif
//#if MC == 12110 || MC == 12111

//#endif