package com.litematlist;

import fi.dy.masa.malilib.util.nbt.NbtUtils;
import net.minecraft.nbt.*;

import java.io.IOException;
import java.nio.file.*;

/**
 * 诊断工具：分析 example 目录下的投影文件 NBT 结构。
 * 运行方式: gradlew runNbtAnalyzer
 */
public class NbtAnalyzer {

    public static void main(String[] args) throws Exception {
        Path[] dirs = {Path.of("build", "example"), Path.of("build", "example", "no2")};
        for (Path dir : dirs) {
            if (!Files.exists(dir)) {
                System.out.println("WARNING: directory not found: " + dir);
                continue;
            }
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.litematic")) {
                for (Path file : stream) {
                    System.out.println("\n========================================");
                    System.out.println("=== " + dir.getFileName() + "/" + file.getFileName() + " ===");
                    System.out.println("File size: " + Files.size(file) + " bytes");
                    analyzeFile(file);
                }
            }
        }
    }

    private static void analyzeFile(Path filePath) {
        try {
            NbtCompound rootTag = NbtUtils.readNbtFromFileAsPath(filePath);
            if (rootTag == null) {
                System.out.println("  ERROR: NbtUtils.readNbtFromFileAsPath returned null");
                try {
                    byte[] raw = Files.readAllBytes(filePath);
                    System.out.println("  Raw first 4 bytes: " + String.format("%02X %02X %02X %02X",
                            raw[0] & 0xFF, raw[1] & 0xFF, raw[2] & 0xFF, raw[3] & 0xFF));
                    boolean isGzip = (raw[0] & 0xFF) == 0x1F && (raw[1] & 0xFF) == 0x8B;
                    System.out.println("  Is GZIP: " + isGzip);
                } catch (IOException e) {
                    System.out.println("  Cannot read raw bytes: " + e.getMessage());
                }
                return;
            }

            System.out.println("  Top-level keys: " + rootTag.getKeys());

            // Version
            if (rootTag.contains("Version")) {
                System.out.println("  Version: " + rootTag.getInt("Version"));
            } else {
                System.out.println("  Version: MISSING");
            }

            // MinecraftDataVersion
            if (rootTag.contains("MinecraftDataVersion")) {
                System.out.println("  MinecraftDataVersion: " + rootTag.getInt("MinecraftDataVersion"));
            } else {
                System.out.println("  MinecraftDataVersion: MISSING");
            }

            // SubVersion
            if (rootTag.contains("SubVersion")) {
                System.out.println("  SubVersion: " + rootTag.getInt("SubVersion"));
            }

            // Metadata
            NbtCompound meta = rootTag.getCompound("Metadata");
            if (meta != null) {
                System.out.println("  Metadata keys: " + meta.getKeys());
                System.out.println("  Metadata.EnclosingSize: " + (meta.contains("EnclosingSize") ? formatEnclosingSize(meta.getCompound("EnclosingSize")) : "MISSING"));
                System.out.println("  Metadata.Name: " + meta.getString("Name"));
            } else {
                System.out.println("  Metadata: MISSING");
            }

            // Regions
            NbtCompound regions = rootTag.getCompound("Regions");
            if (regions != null) {
                System.out.println("  Regions count: " + regions.getKeys().size());
                for (String regionName : regions.getKeys()) {
                    System.out.println("    Region: '" + regionName + "'");
                    NbtCompound region = regions.getCompound(regionName);
                    if (region != null) {
                        System.out.println("      Region keys: " + region.getKeys());

                        // Size
                        NbtCompound size = region.getCompound("Size");
                        if (size != null) {
                            for (String key : new String[]{"x", "y", "z"}) {
                                NbtElement elem = size.get(key);
                                if (elem != null) {
                                    System.out.println("        Size." + key + ": type=" + elem.getType() + " raw=" + getRawValue(elem));
                                }
                            }
                            int sx = Math.abs(size.getInt("x"));
                            int sy = Math.abs(size.getInt("y"));
                            int sz = Math.abs(size.getInt("z"));
                            System.out.println("      Size (abs): " + sx + "x" + sy + "x" + sz + " (" + (sx * sy * sz) + " blocks)");
                        } else {
                            System.out.println("      Size: MISSING");
                        }

                        // BlockStatePalette
                        NbtList palette = region.getList("BlockStatePalette", 10);
                        if (palette != null) {
                            System.out.println("      BlockStatePalette: " + palette.size() + " entries");
                            if (palette.size() > 0) {
                                NbtElement first = palette.get(0);
                                System.out.println("        First entry type: " + first.getType());
                                if (first instanceof NbtCompound firstTag) {
                                    String firstName = firstTag.getString("Name");
                                        System.out.println("        First entry Name: " + (firstName.isEmpty() ? "MISSING" : firstName));
                                }
                                // Show last few non-air entries
                                int shown = 0;
                                for (int i = palette.size() - 1; i >= 0 && shown < 3; i--) {
                                    NbtElement e = palette.get(i);
                                    if (e instanceof NbtCompound tag) {
                                        String name = tag.getString("Name");
                                        if (!name.contains("air")) {
                                            System.out.println("        Palette[" + i + "]: " + name);
                                            shown++;
                                        }
                                    }
                                }
                            }
                        } else {
                            System.out.println("      BlockStatePalette: MISSING");
                        }

                        // BlockStates
                        NbtElement blockStates = region.get("BlockStates");
                        if (blockStates != null) {
                            System.out.println("      BlockStates type: " + blockStates.getType() + " (" + blockStates.getClass().getSimpleName() + ")");
                            if (blockStates instanceof NbtLongArray longArray) {
                                System.out.println("      BlockStates length: " + longArray.getLongArray().length + " longs");
                            } else if (blockStates instanceof NbtIntArray intArray) {
                                System.out.println("      BlockStates length: " + intArray.getIntArray().length + " ints");
                            } else if (blockStates instanceof NbtByteArray byteArray) {
                                System.out.println("      BlockStates length: " + byteArray.getByteArray().length + " bytes");
                            }
                        } else {
                            System.out.println("      BlockStates: MISSING");
                        }
                    }
                }
            } else {
                System.out.println("  Regions: MISSING");
            }

            // Check for Sponge format
            if (rootTag.contains("Blocks")) {
                System.out.println("  Sponge 'Blocks' key present");
            }

        } catch (Exception e) {
            System.out.println("  EXCEPTION: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace(System.out);
        }
    }

        private static String formatEnclosingSize(NbtCompound size) {
        if (size == null) return "N/A";
        int x = size.getInt("x");
        int y = size.getInt("y");
        int z = size.getInt("z");
        return x + "x" + y + "x" + z;
    }

    private static String getRawValue(NbtElement elem) {
        if (elem instanceof NbtInt i) return String.valueOf(i.intValue());
        if (elem instanceof NbtShort s) return String.valueOf(s.shortValue());
        if (elem instanceof NbtByte b) return String.valueOf(b.byteValue());
        return "type=" + elem.getType();
    }
}

