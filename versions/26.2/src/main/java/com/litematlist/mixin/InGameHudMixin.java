package com.litematlist.mixin;

import com.litematlist.gui.MaterialListHudRenderer;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class InGameHudMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void litematlist_renderHud(GuiGraphicsExtractor gfx, DeltaTracker deltaTracker, CallbackInfo ci) {
        MaterialListHudRenderer.render(gfx);
    }
}