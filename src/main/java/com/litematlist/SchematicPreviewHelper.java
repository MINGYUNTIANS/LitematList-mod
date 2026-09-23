package com.litematlist;

import com.litematlist.config.Configs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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

            // SchematicPreview 0.0.14-1.21.4 的 setSchematic 参数为 java.io.File（不是 Path）
            Method setSchematic = widgetClass.getMethod("setSchematic", File.class);
            setSchematic.invoke(widget, schematicFile.toFile());

            Class<?> fullscreenClass = Class.forName("ru.dimaskama.schematicpreview.gui.GuiSchematicPreviewFullscreen");
            Screen previewScreen = null;

            // 尝试多种构造函数签名
            try {
                Constructor<?> fullscreenCtor = fullscreenClass.getConstructor(Screen.class, widgetClass);
                previewScreen = (Screen) fullscreenCtor.newInstance(parent, widget);
            } catch (NoSuchMethodException e1) {
                try {
                    Constructor<?> fullscreenCtor = fullscreenClass.getConstructor(Screen.class, widgetClass, boolean.class);
                    previewScreen = (Screen) fullscreenCtor.newInstance(parent, widget, false);
                } catch (NoSuchMethodException e2) {
                    Constructor<?> fullscreenCtor = fullscreenClass.getConstructor(widgetClass);
                    previewScreen = (Screen) fullscreenCtor.newInstance(widget);
                }
            }

            if (previewScreen != null) {
                MinecraftClient client = MinecraftClient.getInstance();
                client.setScreen(previewScreen);
                LOGGER.info("全屏预览已打开: {}", schematicFile);
            }
        } catch (Exception e) {
            LOGGER.error("无法打开全屏原理图预览: {}", schematicFile, e);
        }
    }
}