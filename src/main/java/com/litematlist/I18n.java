package com.litematlist;

import net.minecraft.text.Text;

/**
 * 国际化工具类，用于按钮和界面文本的翻译。
 */
public class I18n {
    public static String tr(String key, Object... args) {
        String template = Text.translatable(key).getString();
        if (args.length == 0) {
            return template;
        }
        return java.text.MessageFormat.format(template, args);
    }
}