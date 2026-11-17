package com.litematlist;

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import fi.dy.masa.malilib.util.nbt.NbtUtils;
//#endif
import net.minecraft.nbt.*;

import java.io.IOException;
import java.nio.file.*;
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
import java.util.Optional;
//#endif

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
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            NbtCompound rootTag = NbtUtils.readNbtFromFileAsPath(filePath);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            NbtCompound rootTag = LitematicReader.readNbtFile(filePath);
//#endif
            if (rootTag == null) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                System.out.println("  ERROR: NbtUtils.readNbtFromFileAsPath returned null");
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                System.out.println("  ERROR: readNbtFile returned null");
//#endif
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
//#if MC == 12108 || MC == 12110 || MC == 12111
                System.out.println("  Version: " + rootTag.getInt("Version").orElse(-1));
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                System.out.println("  Version: " + rootTag.getInt("Version"));
//#endif
            } else {
                System.out.println("  Version: MISSING");
            }

            // MinecraftDataVersion
            if (rootTag.contains("MinecraftDataVersion")) {
//#if MC == 12108 || MC == 12110 || MC == 12111
                System.out.println("  MinecraftDataVersion: " + rootTag.getInt("MinecraftDataVersion").orElse(-1));
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                System.out.println("  MinecraftDataVersion: " + rootTag.getInt("MinecraftDataVersion"));
//#endif
            } else {
                System.out.println("  MinecraftDataVersion: MISSING");
            }

            // SubVersion
            if (rootTag.contains("SubVersion")) {
//#if MC == 12108 || MC == 12110 || MC == 12111
                System.out.println("  SubVersion: " + rootTag.getInt("SubVersion").orElse(-1));
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                System.out.println("  SubVersion: " + rootTag.getInt("SubVersion"));
//#endif
            }

            // Metadata
//#if MC == 12108 || MC == 12110 || MC == 12111
            Optional<NbtCompound> metaOpt = rootTag.getCompound("Metadata");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            if (metaOpt.isPresent()) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                NbtCompound meta = metaOpt.get();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (rootTag.contains("Metadata")) {
//#elseif MC == 12101 || MC == 12104
//$$            NbtCompound meta = rootTag.getCompound("Metadata");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                NbtCompound meta = rootTag.getCompound("Metadata");
//#elseif MC == 12101 || MC == 12104
//$$            if (meta != null) {
//#endif
                System.out.println("  Metadata keys: " + meta.getKeys());
//#if MC == 12108 || MC == 12110 || MC == 12111
                System.out.println("  Metadata.EnclosingSize: " + (meta.contains("EnclosingSize") ? meta.getCompound("EnclosingSize").map(c ->
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                        c.getInt("x").orElse(0) + "x" + c.getInt("y").orElse(0) + "x" + c.getInt("z").orElse(0)
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                ).orElse("N/A") : "MISSING"));
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                meta.getString("Name").ifPresent(n -> System.out.println("  Metadata.Name: " + n));
//#endif
//#if MC == 12101 || MC == 12104
//$$                NbtCompound enclosingSize = meta.getCompound("EnclosingSize");
//#endif
//#if MC == 12101 || MC == 12104
//$$                System.out.println("  Metadata.EnclosingSize: " + (meta.contains("EnclosingSize") ? (enclosingSize != null ?
//#endif
//#if MC == 12101 || MC == 12104
//$$                        enclosingSize.getInt("x") + "x" + enclosingSize.getInt("y") + "x" + enclosingSize.getInt("z")
//#endif
//#if MC == 12101 || MC == 12104
//$$                        : "N/A") : "MISSING"));
//#endif
//#if MC == 12101 || MC == 12104
//$$                String metaName = meta.getString("Name");
//#endif
//#if MC == 12101 || MC == 12104
//$$                if (!metaName.isEmpty()) {
//#endif
//#if MC == 12101 || MC == 12104
//$$                    System.out.println("  Metadata.Name: " + metaName);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                if (meta.contains("EnclosingSize")) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    NbtCompound size = meta.getCompound("EnclosingSize");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    System.out.println("  Metadata.EnclosingSize: " + size.getInt("x") + "x" + size.getInt("y") + "x" + size.getInt("z"));
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                } else {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    System.out.println("  Metadata.EnclosingSize: MISSING");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                }
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                if (meta.contains("Name")) {
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    System.out.println("  Metadata.Name: " + meta.getString("Name"));
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                }
//#endif
            } else {
                System.out.println("  Metadata: MISSING");
            }

            // Regions
//#if MC == 12108 || MC == 12110 || MC == 12111
            Optional<NbtCompound> regionsOpt = rootTag.getCompound("Regions");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
            if (regionsOpt.isPresent()) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                NbtCompound regions = regionsOpt.get();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$            if (rootTag.contains("Regions")) {
//#elseif MC == 12101 || MC == 12104
//$$            NbtCompound regions = rootTag.getCompound("Regions");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                NbtCompound regions = rootTag.getCompound("Regions");
//#elseif MC == 12101 || MC == 12104
//$$            if (regions != null) {
//#endif
                System.out.println("  Regions count: " + regions.getKeys().size());
                for (String regionName : regions.getKeys()) {
                    System.out.println("    Region: '" + regionName + "'");
//#if MC == 12108 || MC == 12110 || MC == 12111
                    Optional<NbtCompound> regionOpt = regions.getCompound(regionName);
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                    if (regionOpt.isPresent()) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                        NbtCompound region = regionOpt.get();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                    if (regions.contains(regionName)) {
//#elseif MC == 12101 || MC == 12104
//$$                    NbtCompound region = regions.getCompound(regionName);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        NbtCompound region = regions.getCompound(regionName);
//#elseif MC == 12101 || MC == 12104
//$$                    if (region != null) {
//#endif
                        System.out.println("      Region keys: " + region.getKeys());

                        // Size
//#if MC == 12108 || MC == 12110 || MC == 12111
                        Optional<NbtCompound> sizeOpt = region.getCompound("Size");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                        if (sizeOpt.isPresent()) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                            NbtCompound size = sizeOpt.get();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        if (region.contains("Size")) {
//#elseif MC == 12101 || MC == 12104
//$$                        NbtCompound size = region.getCompound("Size");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                            NbtCompound size = region.getCompound("Size");
//#elseif MC == 12101 || MC == 12104
//$$                        if (size != null) {
//#endif
                            for (String key : new String[]{"x", "y", "z"}) {
                                NbtElement elem = size.get(key);
                                if (elem != null) {
                                    System.out.println("        Size." + key + ": type=" + elem.getType() + " raw=" + getRawValue(elem));
                                }
                            }
//#if MC == 12108 || MC == 12110 || MC == 12111
                            int sx = Math.abs(size.getInt("x").orElse(0));
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                            int sx = Math.abs(size.getInt("x"));
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                            int sy = Math.abs(size.getInt("y").orElse(0));
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                            int sy = Math.abs(size.getInt("y"));
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                            int sz = Math.abs(size.getInt("z").orElse(0));
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                            int sz = Math.abs(size.getInt("z"));
//#endif
                            System.out.println("      Size (abs): " + sx + "x" + sy + "x" + sz + " (" + (sx * sy * sz) + " blocks)");
                        } else {
                            System.out.println("      Size: MISSING");
                        }

                        // BlockStatePalette
//#if MC == 12108 || MC == 12110 || MC == 12111
                        Optional<NbtList> paletteOpt = region.getList("BlockStatePalette");
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                        if (paletteOpt.isPresent()) {
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                            NbtList palette = paletteOpt.get();
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                        if (region.contains("BlockStatePalette")) {
//#elseif MC == 12101 || MC == 12104
//$$                        NbtList palette = region.getList("BlockStatePalette", 10);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                            NbtList palette = region.getList("BlockStatePalette", 10);
//#elseif MC == 12101 || MC == 12104
//$$                        if (palette != null) {
//#endif
                            System.out.println("      BlockStatePalette: " + palette.size() + " entries");
                            if (palette.size() > 0) {
                                NbtElement first = palette.get(0);
                                System.out.println("        First entry type: " + first.getType());
                                if (first instanceof NbtCompound firstTag) {
//#if MC == 12108 || MC == 12110 || MC == 12111
                                    firstTag.getString("Name").ifPresentOrElse(
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                                            name -> System.out.println("        First entry Name: " + name),
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                                            () -> System.out.println("        First entry Name: MISSING")
//#endif
//#if MC == 12108 || MC == 12110 || MC == 12111
                                    );
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                                    String name = firstTag.contains("Name") ? firstTag.getString("Name") : "MISSING";
//#elseif MC == 12101 || MC == 12104
//$$                                    String firstName = firstTag.getString("Name");
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                                    System.out.println("        First entry Name: " + name);
//#elseif MC == 12101 || MC == 12104
//$$                                    System.out.println("        First entry Name: " + (firstName.isEmpty() ? "MISSING" : firstName));
//#endif
                                }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                                // Show last few non-air entries
//#endif
                                int shown = 0;
                                for (int i = palette.size() - 1; i >= 0 && shown < 3; i--) {
                                    NbtElement e = palette.get(i);
                                    if (e instanceof NbtCompound tag) {
//#if MC == 12108 || MC == 12110 || MC == 12111
                                        String name = tag.getString("Name").orElse("");
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                                        String name = tag.contains("Name") ? tag.getString("Name") : "";
//#elseif MC == 12101 || MC == 12104
//$$                                        String name = tag.getString("Name");
//#endif
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
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
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

    private static String getRawValue(NbtElement elem) {
        if (elem instanceof NbtInt i) return String.valueOf(i.intValue());
        if (elem instanceof NbtShort s) return String.valueOf(s.shortValue());
        if (elem instanceof NbtByte b) return String.valueOf(b.byteValue());
        return "type=" + elem.getType();
    }
}
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif