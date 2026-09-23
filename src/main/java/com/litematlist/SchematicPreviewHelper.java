package com.litematlist;

import com.litematlist.config.Configs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;

/**
 * 反射调用 SchematicPreview 模组功能，避免编译时依赖。
 */
public class SchematicPreviewHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger("litematlist.SchematicPreviewHelper");
    private static final boolean AVAILABLE;

    static {
        boolean available = false;
        try {
            Class.forName("ru.dimaskama.schematicpreview.SchematicPreview");
            available = true;
            LOGGER.info("SchematicPreview 模组已检测到，预览功能可用");
        } catch (ClassNotFoundException ignored) {
            LOGGER.info("SchematicPreview 模组未安装，预览按钮将隐藏");
        }
        AVAILABLE = available;
    }

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    /**
     * 打开全屏原理图预览。
     * ESC 提示通过按钮 tooltip（"全屏预览 (按ESC退出)"）实现。
     */
    public static void openFullscreenPreview(Screen parent, Path schematicFile) {
        if (!AVAILABLE) {
            LOGGER.warn("openFullscreenPreview 调用被忽略：SchematicPreview 未安装");
            return;
        }

        try {
            LOGGER.info("打开全屏预览: {}", schematicFile);

            Class<?> spClass = Class.forName("ru.dimaskama.schematicpreview.SchematicPreview");
            Field cacheField = spClass.getField("PREVIEWS_CACHE");
            Object previewsCache = cacheField.get(null);

            Class<?> widgetClass = Class.forName("ru.dimaskama.schematicpreview.gui.widget.SchematicPreviewWidget");
            Class<?> cacheClass = Class.forName("ru.dimaskama.schematicpreview.render.PreviewsCache");
            Constructor<?> widgetCtor = widgetClass.getConstructor(cacheClass, boolean.class);
            Object widget = widgetCtor.newInstance(previewsCache, true);

            Method setSchematic = widgetClass.getMethod("setSchematic", Path.class);
            setSchematic.invoke(widget, schematicFile);

            Class<?> fullscreenClass = Class.forName("ru.dimaskama.schematicpreview.gui.GuiSchematicPreviewFullscreen");
            Constructor<?> fullscreenCtor = fullscreenClass.getConstructor(Screen.class, widgetClass);
            Screen previewScreen = (Screen) fullscreenCtor.newInstance(parent, widget);

            Minecraft client = Minecraft.getInstance();
            client.setScreenAndShow(previewScreen);

            LOGGER.info("全屏预览已打开: {}", schematicFile);
        } catch (Exception e) {
            LOGGER.error("无法打开全屏原理图预览: {}", schematicFile, e);
        }
    }
}