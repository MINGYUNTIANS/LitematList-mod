package com.litematlist.preview;

import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.util.FileType;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 原理图异步加载缓存（搬自 SchematicPreview，仅保留全屏预览所需部分）。
 */
public class PreviewsCache implements AutoCloseable {

    private final Map<Path, CompletableFuture<LitematicaSchematic>> schematics = new HashMap<>();
    private boolean closed = false;

    public CompletableFuture<LitematicaSchematic> getSchematic(Path file) {
        closed = false;
        return schematics.computeIfAbsent(file, f -> CompletableFuture.supplyAsync(() ->
                LitematicaSchematic.createFromFile(f.getParent(), f.getFileName().toString(), FileType.fromFile(f)),
                Util.nonCriticalIoPool()
        ));
    }

    public void tickClose() {
        if (!closed && Minecraft.getInstance().gui.screen() == null) {
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