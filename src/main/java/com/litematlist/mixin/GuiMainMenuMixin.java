package com.litematlist.mixin;

import com.litematlist.LitematListMod;
import com.litematlist.gui.MaterialListScreen;
import fi.dy.masa.litematica.gui.GuiMainMenu;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin 注入 Litematica 的主菜单 (M 键)，在「任务管理器」按钮下方添加「材料列表菜单」按钮。
 */
@Mixin(value = GuiMainMenu.class, remap = false)
public abstract class GuiMainMenuMixin extends GuiBase {

    @Inject(method = "initGui", at = @At("RETURN"))
    private void litematlist_addMaterialListButton(CallbackInfo ci) {
        try {
            GuiMainMenu self = (GuiMainMenu) (Object) this;

            // 按钮位置：靠近底部，与其他按钮相同宽度
            int x = 12;
            int y = self.height - 60;  // 放在任务管理器按钮下方
            int width = 160;
            int height = 20;

            String label = "材料列表菜单";
            ButtonGeneric button = new ButtonGeneric(x, y, width, height, label);
            button.setRenderDefaultBackground(true);
            button.setHoverStrings("打开 LitematList 材料列表");

            self.addButton(button, (IButtonActionListener) (btn, mouseButton) -> {
                MinecraftClient.getInstance().setScreen(new MaterialListScreen());
            });

            LitematListMod.LOGGER.info("已在投影主菜单添加「材料列表菜单」按钮");
        } catch (Exception e) {
            LitematListMod.LOGGER.error("添加材料列表按钮失败", e);
        }
    }
}