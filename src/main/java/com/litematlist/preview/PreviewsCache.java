package com.litematlist.preview;

import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.util.FileType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;

//#if MC == 12108 || MC == 12110 || MC == 12111
import java.nio.file.Path;
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$import java.io.File;
//#endif
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 原理图异步加载缓存（搬自 SchematicPreview，仅保留全屏预览所需部分）。
 */
//#if MC == 12101 || MC == 12104
//$$import fi.dy.masa.litematica.schematic.LitematicaSchematic;
//#endif
//#if MC == 12101 || MC == 12104
//$$import fi.dy.masa.litematica.util.FileType;
//#endif
//#if MC == 12101 || MC == 12104
//$$import net.minecraft.client.MinecraftClient;
//#endif
//#if MC == 12101 || MC == 12104
//$$import net.minecraft.util.Util;
//#endif
//#if MC == 12101 || MC == 12104
//$$
//#endif
//#if MC == 12101 || MC == 12104
//$$import java.io.File;
//#endif
//#if MC == 12101 || MC == 12104
//$$import java.util.HashMap;
//#endif
//#if MC == 12101 || MC == 12104
//$$import java.util.Map;
//#endif
//#if MC == 12101 || MC == 12104
//$$import java.util.concurrent.CompletableFuture;
//#endif
//#if MC == 12101 || MC == 12104
//$$
//#endif
public class PreviewsCache implements AutoCloseable {

//#if MC == 12108 || MC == 12110 || MC == 12111
    private final Map<Path, CompletableFuture<LitematicaSchematic>> schematics = new HashMap<>();
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$    private final Map<File, CompletableFuture<LitematicaSchematic>> schematics = new HashMap<>();
//#endif
    private boolean closed = false;

//#if MC == 12108 || MC == 12110 || MC == 12111
    public CompletableFuture<LitematicaSchematic> getSchematic(Path file) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$    public CompletableFuture<LitematicaSchematic> getSchematic(File file) {
//#endif
        closed = false;
        return schematics.computeIfAbsent(file, f -> CompletableFuture.supplyAsync(() ->
//#if MC == 12108 || MC == 12110 || MC == 12111
                LitematicaSchematic.createFromFile(f.getParent(), f.getFileName().toString(), FileType.fromFile(f)),
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                LitematicaSchematic.createFromFile(f.getParentFile(), f.getName(), FileType.fromFile(f)),
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                Util.getDownloadWorkerExecutor()
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$                Util.getMainWorkerExecutor()
//#endif
        ));
    }

    public void tickClose() {
        if (!closed && MinecraftClient.getInstance().currentScreen == null) {
            close();
        }
    }

    @Override
    public void close() {
        schematics.values().forEach(f -> f.cancel(true));
        schematics.clear();
        closed = true;
    }

}
//#if MC == 11701
//$$
//#endif
//#if MC == 12101 || MC == 12104
//$$
//#endif