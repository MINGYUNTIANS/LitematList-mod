package com.litematlist.preview;

import com.litematlist.LitematListMod;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.util.Identifier;

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

//#if MC == 11701 || MC == 11904 || MC == 12001
//$$    /**
//$$     * 1.19.4/1.20.1 无 GUI 精灵图集：图标必须以完整纹理路径引用
//$$     * （assets/litematlist/textures/gui/sprites/&lt;name&gt;.png），不能只给精灵名。
//$$     */
//#endif
    public static Identifier sprite(String path) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return Identifier.of(LitematListMod.MOD_ID, path);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        return new Identifier(LitematListMod.MOD_ID, "textures/gui/sprites/" + path + ".png");
//#endif
    }

}