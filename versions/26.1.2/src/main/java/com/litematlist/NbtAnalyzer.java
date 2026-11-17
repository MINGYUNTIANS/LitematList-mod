package com.litematlist;

import fi.dy.masa.malilib.util.nbt.NbtUtils;
import net.minecraft.nbt.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.Optional;

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
            CompoundTag rootTag = NbtUtils.readNbtFromFileAsPath(filePath);
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

            System.out.println("  Top-level keys: " + rootTag.keySet());

            // Version
            if (rootTag.contains("Version")) {
                System.out.println("  Version: " + rootTag.getInt("Version").orElse(-1));
            } else {
                System.out.println("  Version: MISSING");
            }

            // MinecraftDataVersion
            if (rootTag.contains("MinecraftDataVersion")) {
                System.out.println("  MinecraftDataVersion: " + rootTag.getInt("MinecraftDataVersion").orElse(-1));
            } else {
                System.out.println("  MinecraftDataVersion: MISSING");
            }

            // SubVersion
            if (rootTag.contains("SubVersion")) {
                System.out.println("  SubVersion: " + rootTag.getInt("SubVersion").orElse(-1));
            }

            // Metadata
            Optional<CompoundTag> metaOpt = rootTag.getCompound("Metadata");
            if (metaOpt.isPresent()) {
                CompoundTag meta = metaOpt.get();
                System.out.println("  Metadata keys: " + meta.keySet());
                System.out.println("  Metadata.EnclosingSize: " + (meta.contains("EnclosingSize") ? meta.getCompound("EnclosingSize").map(c ->
                        c.getInt("x").orElse(0) + "x" + c.getInt("y").orElse(0) + "x" + c.getInt("z").orElse(0)
                ).orElse("N/A") : "MISSING"));
                meta.getString("Name").ifPresent(n -> System.out.println("  Metadata.Name: " + n));
            } else {
                System.out.println("  Metadata: MISSING");
            }

            // Regions
            Optional<CompoundTag> regionsOpt = rootTag.getCompound("Regions");
            if (regionsOpt.isPresent()) {
                CompoundTag regions = regionsOpt.get();
                System.out.println("  Regions count: " + regions.keySet().size());
                for (String regionName : regions.keySet()) {
                    System.out.println("    Region: '" + regionName + "'");
                    Optional<CompoundTag> regionOpt = regions.getCompound(regionName);
                    if (regionOpt.isPresent()) {
                        CompoundTag region = regionOpt.get();
                        System.out.println("      Region keys: " + region.keySet());

                        // Size
                        Optional<CompoundTag> sizeOpt = region.getCompound("Size");
                        if (sizeOpt.isPresent()) {
                            CompoundTag size = sizeOpt.get();
                            for (String key : new String[]{"x", "y", "z"}) {
                                Tag elem = size.get(key);
                                if (elem != null) {
                                    System.out.println("        Size." + key + ": type=" + elem.getId() + " raw=" + getRawValue(elem));
                                }
                            }
                            int sx = Math.abs(size.getInt("x").orElse(0));
                            int sy = Math.abs(size.getInt("y").orElse(0));
                            int sz = Math.abs(size.getInt("z").orElse(0));
                            System.out.println("      Size (abs): " + sx + "x" + sy + "x" + sz + " (" + (sx * sy * sz) + " blocks)");
                        } else {
                            System.out.println("      Size: MISSING");
                        }

                        // BlockStatePalette
                        Optional<ListTag> paletteOpt = region.getList("BlockStatePalette");
                        if (paletteOpt.isPresent()) {
                            ListTag palette = paletteOpt.get();
                            System.out.println("      BlockStatePalette: " + palette.size() + " entries");
                            if (palette.size() > 0) {
                                Tag first = palette.get(0);
                                System.out.println("        First entry type: " + first.getId());
                                if (first instanceof CompoundTag firstTag) {
                                    firstTag.getString("Name").ifPresentOrElse(
                                            name -> System.out.println("        First entry Name: " + name),
                                            () -> System.out.println("        First entry Name: MISSING")
                                    );
                                }
                                // Show last few non-air entries
                                int shown = 0;
                                for (int i = palette.size() - 1; i >= 0 && shown < 3; i--) {
                                    Tag e = palette.get(i);
                                    if (e instanceof CompoundTag tag) {
                                        String name = tag.getString("Name").orElse("");
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
                        Tag blockStates = region.get("BlockStates");
                        if (blockStates != null) {
                            System.out.println("      BlockStates type: " + blockStates.getId() + " (" + blockStates.getClass().getSimpleName() + ")");
                            if (blockStates instanceof LongArrayTag longArray) {
                                System.out.println("      BlockStates length: " + longArray.getAsLongArray().length + " longs");
                            } else if (blockStates instanceof IntArrayTag intArray) {
                                System.out.println("      BlockStates length: " + intArray.getAsIntArray().length + " ints");
                            } else if (blockStates instanceof ByteArrayTag byteArray) {
                                System.out.println("      BlockStates length: " + byteArray.getAsByteArray().length + " bytes");
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

    private static String getRawValue(Tag elem) {
        if (elem instanceof IntTag i) return String.valueOf(i.intValue());
        if (elem instanceof ShortTag s) return String.valueOf(s.shortValue());
        if (elem instanceof ByteTag b) return String.valueOf(b.byteValue());
        return "type=" + elem.getId();
    }
}