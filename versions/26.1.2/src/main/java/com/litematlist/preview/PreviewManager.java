package com.litematlist.preview;

import com.litematlist.LitematListMod;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.resources.Identifier;

import java.util.Set;

/**
 * 内置全屏预览的静态入口（对应上游 SchematicPreview 类）：
 * 管理原理图加载缓存与每 tick 回调（相机跟随、层级跟随重建）。
 */
public final class PreviewManager {

    public static final PreviewsCache PREVIEWS_CACHE = new PreviewsCache();
    private static final Set<Runnable> tickables = new ObjectArraySet<>();

    private PreviewManager() {
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            PREVIEWS_CACHE.tickClose();
            if (!tickables.isEmpty()) {
                for (Runnable tick : tickables) {
                    tick.run();
                }
            }
        });
    }

    public static void addTickable(Runnable tickAction) {
        tickables.add(tickAction);
    }

    public static void removeTickable(Runnable tickAction) {
        tickables.remove(tickAction);
    }

    public static Identifier sprite(String path) {
        return Identifier.fromNamespaceAndPath(LitematListMod.MOD_ID, path);
    }

}