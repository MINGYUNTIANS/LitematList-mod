package com.litematlist.mixin;

import com.litematlist.LitematListMod;
import com.litematlist.config.Configs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Mixin 注入 SchematicPreview 的全屏预览界面。
 * 在 drawContents 返回时（3D 预览已渲染完毕）绘制「按ESC退出」提示，
 * 确保提示在 3D 预览之上显示。
 * 同时捕获 3D 预览渲染时的异常（如 getWorld() 返回 null），防止崩溃。
 * 1.21.10+ 版本：DrawContext 使用 Matrix3x2fStack，无需 push/pop。
 */
@Mixin(targets = "ru.dimaskama.schematicpreview.gui.GuiSchematicPreviewFullscreen", remap = false)
public abstract class GuiSchematicPreviewFullscreenMixin {

    @Inject(method = "drawContents", at = @At("HEAD"))
    private void litematlist_beforeDrawContents(DrawContext drawContext, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        // 不做任何事，仅用于捕获异常
    }

    @Inject(method = "drawContents", at = @At("RETURN"))
    private void litematlist_drawEscHint(DrawContext drawContext, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (Configs.Generic.SHOW_ESC_HINT.getBooleanValue()) {
            MinecraftClient client = MinecraftClient.getInstance();
            String text = "按ESC退出";
            int textWidth = client.textRenderer.getWidth(text);
            int screenWidth = drawContext.getScaledWindowWidth();
            int x = (screenWidth - textWidth) / 2;
            int y = 8;
            drawContext.fill(x - 4, y - 2, x + textWidth + 4, y + 12, 0x80000000);
            drawContext.drawText(client.textRenderer, text, x, y, 0xFFFFFFFF, false);
        }
    }
}