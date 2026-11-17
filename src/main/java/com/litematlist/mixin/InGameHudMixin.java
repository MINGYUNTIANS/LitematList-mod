package com.litematlist.mixin;

import com.litematlist.gui.MaterialListHudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.gui.DrawContext;
//#endif
import net.minecraft.client.gui.hud.InGameHud;
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.util.math.MatrixStack;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.render.RenderTickCounter;
//#endif

/**
 * 在游戏 HUD 渲染完成后绘制本模组的材料列表 HUD。
 */
@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "render", at = @At("TAIL"))
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void litematlist_renderHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
//#elseif MC == 11701 || MC == 11904
//$$    private void litematlist_renderHud(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
//#elseif MC == 12001
//$$    private void litematlist_renderHud(DrawContext context, float tickDelta, CallbackInfo ci) {
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        MaterialListHudRenderer.render(context);
//#elseif MC == 11701 || MC == 11904
//$$        MaterialListHudRenderer.render(matrices);
//#endif
    }
}