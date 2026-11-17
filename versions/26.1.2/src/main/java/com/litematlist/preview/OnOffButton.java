package com.litematlist.preview;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class OnOffButton extends AbstractWidget {

    private final Component name;
    private final BooleanConsumer listener;
    private final Identifier onSprite;
    private final Identifier onSpriteFocused;
    private final Identifier offSprite;
    private final Identifier offSpriteFocused;
    private boolean on;

    public OnOffButton(int width, int height, Component name, BooleanConsumer listener, Identifier onSprite, Identifier onSpriteFocused, Identifier offSprite, Identifier offSpriteFocused) {
        super(0, 0, width, height, CommonComponents.EMPTY);
        this.name = name;
        this.listener = listener;
        this.onSprite = onSprite;
        this.onSpriteFocused = onSpriteFocused;
        this.offSprite = offSprite;
        this.offSpriteFocused = offSpriteFocused;
        updateTooltip();
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                on ? (isHovered() ? offSpriteFocused : offSprite) : (isHovered() ? onSpriteFocused : onSprite),
                getX(), getY(),
                getWidth(), getHeight()
        );
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        defaultButtonNarrationText(builder);
    }

    @Override
    public void onClick(MouseButtonEvent click, boolean doubled) {
        on = !on;
        listener.accept(on);
        updateTooltip();
    }

    /** 同步开关状态（不触发回调），用于把按钮状态与当前界面状态对齐 */
    public void setOn(boolean on) {
        this.on = on;
        updateTooltip();
    }

    private void updateTooltip() {
        setTooltip(Tooltip.create(Component.translatable(on ? "litematlist.preview.disable" : "litematlist.preview.enable", name)));
    }

}