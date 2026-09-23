package com.litematlist.mixin;

import com.litematlist.gui.MaterialListHudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;

/**
 * 在游戏 HUD 渲染完成后绘制本模组的材料列表 HUD。
 */
@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void litematlist_renderHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MaterialListHudRenderer.render(context);
    }
}