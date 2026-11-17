package com.litematlist;

/**
 * 导出/表格文本的「显示宽度」工具：ASCII 记 1，中文等全角字符记 2。
 * 统一原先分散在 ProjectSummaryScreen / MaterialDetailScreen / RawMaterialScreen 中的三份实现。
 */
public final class TextWidthUtils {

    private TextWidthUtils() {}

    /** 计算字符串的显示宽度（ASCII 1，中文等宽字符 2） */
    public static int displayWidth(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            w += isWide(s.charAt(i)) ? 2 : 1;
        }
        return w;
    }

    /** 按显示宽度截断字符串 */
    public static String truncateByWidth(String s, int maxWidth) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            w += isWide(s.charAt(i)) ? 2 : 1;
            if (w > maxWidth) return s.substring(0, i);
        }
        return s;
    }

    /** 将标题用空格填充到指定显示宽度 */
    public static String padToWidth(String title, int totalWidth) {
        int w = displayWidth(title);
        if (w >= totalWidth) return title;
        StringBuilder sb = new StringBuilder(title);
        for (int i = 0; i < totalWidth - w; i++) sb.append(' ');
        return sb.toString();
    }

    private static boolean isWide(char c) {
        return (c >= 0x4E00 && c <= 0x9FFF) || (c >= 0x3000 && c <= 0x303F) || (c >= 0xFF00 && c <= 0xFFEF);
    }
}
