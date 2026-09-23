package com.litematlist.mixin;

import com.litematlist.config.Configs;
import fi.dy.masa.litematica.materials.MaterialListHudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 根据配置开关禁用 Litematica 自带的材料列表 HUD 显示。
 */
@Mixin(value = MaterialListHudRenderer.class, remap = false)
public class MaterialListHudRendererMixin {

    @Inject(method = "getShouldRenderCustom", at = @At("HEAD"), cancellable = true)
    private void litematlist_disableHud(CallbackInfoReturnable<Boolean> cir) {
        if (Configs.Generic.DISABLE_MATERIAL_HUD.getBooleanValue()) {
            cir.setReturnValue(false);
        }
    }
}