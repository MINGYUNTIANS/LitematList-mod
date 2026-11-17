package com.litematlist;

import com.litematlist.preview.GuiSchematicPreviewFullscreen;
import com.litematlist.preview.PreviewManager;
import com.litematlist.preview.SchematicPreviewWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.nio.file.Path;

/**
 * 打开内置全屏原理图预览（渲染链路已内置在本模组内，不再依赖 SchematicPreview 模组）。
 * 预览会按「同步投影渲染层」的规则只渲染原理图对应层级，切割面正常显示。
 */
public class SchematicPreviewHelper {

    /** 内置预览恒可用 */
    public static boolean isAvailable() {
        return true;
    }

    /**
     * 打开全屏原理图预览。
     * ESC 提示由全屏界面内部绘制。
     */
    public static void openFullscreenPreview(Screen parent, Path schematicFile) {
        try {
            LitematicaBridge.LayerFilter filter = LitematicaBridge.computeRenderLayerFilter(schematicFile);
            LitematListMod.LOGGER.info("打开内置全屏预览: {} (层级过滤={})",
                    schematicFile, filter == null ? "无" : filter.signature());

            SchematicPreviewWidget widget = new SchematicPreviewWidget(PreviewManager.PREVIEWS_CACHE, true, filter);
            widget.setSchematic(schematicFile);
            Minecraft.getInstance().setScreen(new GuiSchematicPreviewFullscreen(parent, widget));
        } catch (Exception e) {
            LitematListMod.LOGGER.error("无法打开全屏原理图预览: {}", schematicFile, e);
        }
    }
}