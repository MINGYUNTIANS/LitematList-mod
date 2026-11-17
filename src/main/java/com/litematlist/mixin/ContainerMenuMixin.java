package com.litematlist.mixin;

import com.litematlist.gui.MaterialListHudRenderer;
//#if MC == 11701 || MC == 11904
//$$import java.util.List;
//#endif
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.item.ItemStack;
//#endif
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
/**
 * 注入 ScreenHandler.updateSlotStacks()，标记 HUD 需要刷新。
 * tweakermore 的自动备货也通过此方法触发，因此可以捕获取货后的状态变化。
 * 只设置脏标记，由 tick 处理器统一刷新（避免每帧数十次调用 refreshHud 造成卡顿）。
 */
//#elseif MC == 11701 || MC == 11904
//$$/**
//$$ * 注入 ScreenHandler.updateSlotStacks()，标记 HUD 需要刷新。
//$$ * tweakermore 的自动备货也通过此方法触发，因此可以捕获取货后的状态变化。
//$$ * 只设置脏标记，由 tick 处理器统一刷新（避免每帧数十次调用 refreshHud 造成卡顿）。
//$$ * 注意：1.19.4 的 updateSlotStacks 为 3 参（1.20 才改为无参），注入方法须带相同参数。
//$$ */
//#endif
@Mixin(ScreenHandler.class)
public abstract class ContainerMenuMixin
{
    @Inject(method = "updateSlotStacks", at = @At("TAIL"))
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void litematlist_onContainerSync(CallbackInfo ci)
//#elseif MC == 11701 || MC == 11904
//$$    private void litematlist_onContainerSync(int syncId, List<ItemStack> stacks, ItemStack cursorStack, CallbackInfo ci)
//#endif
    {
        MaterialListHudRenderer.markDirty();
    }
}