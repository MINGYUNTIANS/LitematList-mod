package com.litematlist;
//#if MC == 11701
//$$import net.minecraft.text.TranslatableText;
//#endif

import net.minecraft.text.Text;

/**
 * 国际化工具类，用于按钮和界面文本的翻译。
 */
public class I18n {
    public static String tr(String key, Object... args) {
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        String template = Text.translatable(key).getString();
//#elseif MC == 11701
//$$        String template = new TranslatableText(key).getString();
//#endif
        if (args.length == 0) {
            return template;
        }
        return java.text.MessageFormat.format(template, args);
    }
}