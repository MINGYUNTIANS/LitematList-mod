package com.litematlist;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 中文拼音模糊搜索工具：物品中文名 ↔ 拼音全拼/首字母，供各界面搜索共用。
 * 匹配规则：关键字包含匹配（中文名 / 拼音全拼 / 拼音首字母 / 英文物品ID 任一命中）。
 */
public final class PinyinSearch {

    /** 拼音缓存：itemId → [全拼串, 拼音首字母串] */
    private static final Map<String, String[]> CACHE = new HashMap<>();

    private PinyinSearch() {
    }

    /** 关键字与物品是否匹配（kw 为空视为全匹配），itemId 可传 null */
    public static boolean matches(String keyword, String displayName, String itemId) {
        String kw = keyword.toLowerCase(Locale.ROOT);
        if (kw.isEmpty()) return true;
        if (displayName == null) displayName = itemId != null ? itemId : "";
        if (displayName.toLowerCase(Locale.ROOT).contains(kw)) return true;
        if (itemId != null && itemId.toLowerCase(Locale.ROOT).contains(kw)) return true;
        String[] pair = pinyinPair(itemId != null ? itemId : displayName, displayName);
        return pair[0].contains(kw) || pair[1].contains(kw);
    }

    /** 生成名称的拼音全拼与首字母串（带缓存），供配置列表等「候选包含关键字」的过滤场景使用 */
    public static String[] fullAndInitials(String name) {
        if (name == null || name.isEmpty()) {
            return new String[] { "", "" };
        }
        return pinyinPair("filter/" + name, name);
    }

    /** 汉字转拼音（无声调），非汉字返回 null */
    private static String hanziToPinyin(char c, HanyuPinyinOutputFormat fmt) {
        try {
            String[] arr = PinyinHelper.toHanyuPinyinStringArray(c, fmt);
            if (arr != null && arr.length > 0) return arr[0];
        } catch (BadHanyuPinyinOutputFormatCombination ignored) {
        }
        return null;
    }

    /** 计算名称的全拼串与拼音首字母串（带缓存）："橡木木板" → ["xiangmumuban", "xmmb"]，英文/数字原样保留 */
    private static String[] pinyinPair(String key, String name) {
        String[] cached = CACHE.get(key);
        if (cached != null) return cached;
        String full;
        String initials;
        try {
            HanyuPinyinOutputFormat fmt = new HanyuPinyinOutputFormat();
            fmt.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
            fmt.setVCharType(HanyuPinyinVCharType.WITH_V);
            fmt.setCaseType(HanyuPinyinCaseType.LOWERCASE);
            StringBuilder sb = new StringBuilder();
            for (char c : name.toCharArray()) {
                if (c >= 0x4E00 && c <= 0x9FFF) {
                    String p = hanziToPinyin(c, fmt);
                    if (p != null) sb.append(p);
                } else if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9') {
                    sb.append(c);
                } else if (c >= 'A' && c <= 'Z') {
                    sb.append((char) (c + 32));
                }
                // 其他字符跳过
            }
            full = sb.toString();
            StringBuilder ini = new StringBuilder();
            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);
                if (c >= 0x4E00 && c <= 0x9FFF) {
                    String p = hanziToPinyin(c, fmt);
                    if (p != null && !p.isEmpty()) ini.append(p.charAt(0));
                } else if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9') {
                    ini.append(c);
                } else if (c >= 'A' && c <= 'Z') {
                    ini.append((char) (c + 32));
                }
            }
            initials = ini.toString();
        } catch (Exception e) {
            // 拼音库异常时退化为原名匹配
            full = name.toLowerCase(Locale.ROOT);
            initials = full;
        }
        String[] pair = new String[] { full, initials };
        CACHE.put(key, pair);
        return pair;
    }
}