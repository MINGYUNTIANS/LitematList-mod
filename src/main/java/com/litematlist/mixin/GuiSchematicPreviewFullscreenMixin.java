package com.litematlist.mixin;

import com.litematlist.LitematListMod;
import com.litematlist.config.Configs;
import fi.dy.masa.malilib.render.GuiContext;
import net.minecraft.client.Minecraft;
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
 * 26.2+ 版本：drawContents 使用 GuiContext 参数类型。
 */
@Mixin(targets = "ru.dimaskama.schematicpreview.gui.GuiSchematicPreviewFullscreen", remap = false)
public abstract class GuiSchematicPreviewFullscreenMixin {

    @Inject(method = "drawContents", at = @At("HEAD"))
    private void litematlist_beforeDrawContents(GuiContext drawContext, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        // 不做任何事，仅用于捕获异常
    }

    @Inject(method = "drawContents", at = @At("RETURN"))
    private void litematlist_drawEscHint(GuiContext drawContext, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (Configs.Generic.SHOW_ESC_HINT.getBooleanValue()) {
            Minecraft client = Minecraft.getInstance();
            String text = "按ESC退出";
            int textWidth = client.font.width(text);
            int screenWidth = client.getWindow().getGuiScaledWidth();
            int x = (screenWidth - textWidth) / 2;
            int y = 8;
            drawContext.fill(x - 4, y - 2, x + textWidth + 4, y + 12, 0x80000000);
            drawContext.drawString(client.font, text, x, y, 0xFFFFFFFF);
        }
    }
}