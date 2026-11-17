package com.litematlist.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Mixin 配置插件：门控可选依赖相关的 mixin。
 * 目标 mod（syncmatica_r）未安装时跳过 MixinSyncmaticaMaterialHud，
 * 避免 mixin 目标类缺失导致崩溃；其余 mixin 照常应用。
 */
public class LitematListMixinPlugin implements IMixinConfigPlugin {

    /** syncmatica_r（共享原理图增强）认领材料 HUD 渲染类 */
    private static final String SYNCMATICA_MATERIAL_HUD = "cn.net.rms.syncmatica_r.client.hud.MaterialHudOverlay";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (SYNCMATICA_MATERIAL_HUD.equals(targetClassName)) {
            return FabricLoader.getInstance().isModLoaded("syncmatica_r");
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}