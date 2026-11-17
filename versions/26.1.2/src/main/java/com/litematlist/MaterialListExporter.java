package com.litematlist;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 材料列表（Item / Total / Missing / Available 四列）导出器：txt / json / csv 三种格式。
 * 统一 ProjectSummaryScreen 与 MaterialDetailScreen 原先各自的一份实现，输出与投影导出的格式保持一致。
 */
public final class MaterialListExporter {

    private MaterialListExporter() {}

    /**
     * 导出行。
     * itemId 用于 JSON 的 "Item" 字段；displayName 用于 txt/csv 的名称列与表格对齐。
     */
    public record Row(String itemId, String displayName, int total, int missing, int available) {}

    private static final String TXT_BORDER = "+---------+-------+---------+-----------+";

    /** 导出与 Litematica 完全一致的 ASCII 表格 txt。 */
    public static void writeTxt(Path filePath, String title, List<Row> rows) throws IOException {
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(filePath.toFile()), StandardCharsets.UTF_8))) {
            w.write(TXT_BORDER);
            w.newLine();
            w.write("| " + TextWidthUtils.padToWidth(title, 43) + " |");
            w.newLine();
            w.write(TXT_BORDER);
            w.newLine();
            w.write("| Item    | Total | Missing | Available |");
            w.newLine();
            w.write(TXT_BORDER);
            w.newLine();

            for (Row row : rows) {
                String name = row.displayName() != null ? row.displayName() : "";
                // 不截断：精确物品ID需完整保留，否则导入时无法还原（%-9s 仅补足短名称的对齐）
                w.write(String.format("| %-9s | %5d | %7d | %9d |",
                        name, row.total(), row.missing(), row.available()));
                w.newLine();
            }
            w.write(TXT_BORDER);
            w.newLine();
        }
    }

    /** 导出为 Litematica 兼容的 JSON 材料列表。 */
    public static void writeJson(Path filePath, String name, String title, List<Row> rows) throws IOException {
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(filePath.toFile()), StandardCharsets.UTF_8))) {
            w.write("{");
            w.newLine();
            w.write("  \"Name\": \"" + jsonEscape(name) + "\",");
            w.newLine();
            w.write("  \"Title\": \"" + jsonEscape(title) + "\",");
            w.newLine();
            w.write("  \"Multiplier\": 1,");
            w.newLine();
            String dateStr = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH).format(new Date());
            w.write("  \"Date\": \"" + dateStr + "\",");
            w.newLine();
            w.write("  \"Materials\": [");
            w.newLine();

            boolean first = true;
            for (Row row : rows) {
                if (!first) { w.write(","); w.newLine(); }
                first = false;
                w.write("    {");
                w.write("\"Item\": \"" + jsonEscape(row.itemId()) + "\", ");
                w.write("\"Total\": " + row.total() + ", ");
                w.write("\"Missing\": " + row.missing() + ", ");
                w.write("\"Mismatched\": 0, ");
                w.write("\"Available\": " + row.available());
                w.write("}");
            }
            w.newLine();
            w.write("  ]");
            w.newLine();
            w.write("}");
            w.newLine();
        }
    }

    /** 导出为与 Litematica 一致的 CSV 材料列表。 */
    public static void writeCsv(Path filePath, List<Row> rows) throws IOException {
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(filePath.toFile()), StandardCharsets.UTF_8))) {
            w.write("\"Item\",\"Total\",\"Missing\",\"Available\"");
            w.newLine();
            for (Row row : rows) {
                w.write("\"" + csvEscape(row.displayName()) + "\"," + row.total() + ","
                        + row.missing() + "," + row.available());
                w.newLine();
            }
        }
    }

    /** JSON 字符串转义（精确物品ID含双引号，必须转义，否则导出文件非法）。 */
    public static String jsonEscape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }

    /** CSV 字段转义（RFC4180：字段内的双引号翻倍；字段已被双引号包裹）。 */
    public static String csvEscape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\"\"");
    }
}
