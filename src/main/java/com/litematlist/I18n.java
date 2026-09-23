package com.litematlist;

import net.minecraft.network.chat.Component;
import java.text.MessageFormat;

/**
 * 国际化工具类，用于按钮和界面文本的翻译。
 * 使用 MessageFormat 支持 {0}, {1} 等占位符。
 */
public class I18n {
    public static String tr(String key, Object... args) {
        String pattern = Component.translatable(key).getString();
        if (args.length > 0) {
            return MessageFormat.format(pattern, args);
        }
        return pattern;
    }
}