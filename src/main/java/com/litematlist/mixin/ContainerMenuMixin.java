package com.litematlist.mixin;

import com.litematlist.gui.MaterialListHudRenderer;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 注入 ScreenHandler.updateSlotStacks()，标记 HUD 需要刷新。
 * tweakermore 的自动备货也通过此方法触发，因此可以捕获取货后的状态变化。
 * 只设置脏标记，由 tick 处理器统一刷新（避免每帧数十次调用 refreshHud 造成卡顿）。
 */
@Mixin(ScreenHandler.class)
public abstract class ContainerMenuMixin
{
    @Inject(method = "updateSlotStacks", at = @At("TAIL"))
    private void litematlist_onContainerSync(CallbackInfo ci)
    {
        MaterialListHudRenderer.markDirty();
    }
}