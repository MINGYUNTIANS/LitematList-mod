package com.litematlist.mixin;

import com.litematlist.config.Configs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 可选依赖「共享原理图增强」(syncmatica_r)：
 * 当本模组【禁用Syncmatica_Revolution认领材料HUD】开关开启时，
 * 取消其 MaterialHudOverlay 的 render() 渲染，从而隐藏认领材料 HUD。
 * 目标类可能不存在（未安装该模组），由 LitematListMixinPlugin 决定是否应用本 mixin。
 */
@Mixin(targets = "cn.net.rms.syncmatica_r.client.hud.MaterialHudOverlay")
public class MixinSyncmaticaMaterialHud {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void litematlist_blockSyncmaticaMaterialHud(CallbackInfo ci) {
        if (Configs.Generic.DISABLE_SYNCMATICA_MATERIAL_HUD.getBooleanValue()) {
            ci.cancel();
        }
    }
}