package com.litematlist.mixin;

import com.litematlist.gui.MaterialListHudRenderer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public class ContainerMenuMixin {
    @Inject(method = "broadcastChanges", at = @At("TAIL"))
    private void litematlist_onContainerSync(CallbackInfo ci) {
        MaterialListHudRenderer.markDirty();
    }
}