package com.litematlist;

import com.litematlist.config.Configs;
import com.litematlist.config.Hotkeys;
import com.litematlist.gui.GuiConfigs;
import com.litematlist.gui.HudConfigScreen;
//#if MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import com.litematlist.gui.MaterialDataStore;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import com.litematlist.gui.MaterialDetailScreen;
//#endif
//#if MC == 12001 || MC == 12101
//$$import com.litematlist.gui.MaterialDataStore;
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$import com.litematlist.gui.MaterialDetailScreen;
//#endif
//#if MC == 11701 || MC == 11904
//$$import com.litematlist.gui.MaterialDataStore;
//#endif
import com.litematlist.gui.MaterialListHudRenderer;
import com.litematlist.gui.MaterialListScreen;
import com.litematlist.gui.ProjectScreen;
import com.litematlist.gui.ProjectSummaryScreen;
import com.litematlist.preview.PreviewManager;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.event.InitializationHandler;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.hotkeys.*;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import fi.dy.masa.malilib.registry.Registry;
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import fi.dy.masa.malilib.util.data.ModInfo;
//#endif
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.gui.DrawContext;
//#endif
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.gui.DrawableHelper;
//#endif
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.util.math.MatrixStack;
//#endif
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public class LitematListMod implements ModInitializer, ClientModInitializer, IInitializationHandler, IKeybindProvider {

    public static final String MOD_ID = "litematlist";
    public static final String MOD_NAME = "LitematList";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // 每个材料列表的「快捷打开」快捷键缓存（键：文件路径字符串）
    private static final java.util.Map<String, IKeybind> entryHotkeyCache = new java.util.HashMap<>();
    private static final java.util.Map<String, String> entryHotkeyCacheSrc = new java.util.HashMap<>();

    // 启动时检测 TweakerMore 是否安装
    private static boolean tweakermoreInstalled = false;
    // HUD 自动刷新：跟踪上一个屏幕以检测关闭背包/容器
    private static net.minecraft.client.gui.screen.Screen previousScreen = null;
    // HUD 开关快捷键：防止长按时持续切换
    private static boolean toggleHudProcessed = false;
    // HUD 翻页快捷键：防止长按时持续翻页
    private static boolean hudNextPageProcessed = false;
    private static boolean hudPrevPageProcessed = false;
    // HUD 定时刷新计数器（每4 tick = 200ms刷新一次，每秒最多5次）
    private static int hudRefreshTicks = 0;
    // 「是否计算容器数据」开关上一次已生效的值（null 表示尚未初始化）
    private static Boolean lastCalculateContainerData = null;
    // 开关变化后等待生效的新值（防抖：数值需保持稳定一段时间才真正生效）
    private static Boolean pendingCalculateContainerData = null;
    private static long pendingCalculateContainerDataTime = 0;
    // 刷新执行中标志：执行期间忽略新的开关变化，防止叠加触发
    private static boolean containerDataRefreshRunning = false;
    // 防抖时长：开关数值稳定 1 秒后才真正刷新，避免玩家频繁开关时反复解析大原理图导致卡顿或崩溃
    private static final long CONTAINER_DATA_DEBOUNCE_MS = 1000;
    // 「同步投影渲染层」开关上一次已生效的值（null 表示尚未初始化）
    private static Boolean lastSyncRenderLayer = null;
    private static Boolean pendingSyncRenderLayer = null;
    private static long pendingSyncRenderLayerTime = 0;
    private static boolean syncRenderLayerSwitchRunning = false;
    private static final long SYNC_RENDER_LAYER_DEBOUNCE_MS = 1000;
    // 投影渲染层变化检测：渲染层状态签名（玩家切换渲染层后，稳定 1 秒再把更新的材料列表同步至 HUD）
    private static boolean renderLayerSignatureInitialized = false;
    private static String lastRenderLayerSignature = null;
    private static boolean renderLayerPendingActive = false;
    private static String pendingRenderLayerSignature = null;
    private static long pendingRenderLayerSignatureTime = 0;
    private static boolean renderLayerSignatureRefreshRunning = false;
    private static final long RENDER_LAYER_SIGNATURE_DEBOUNCE_MS = 1000;
    // 「是否计算原理图实体」/「是否计算原理图实体容器数据」开关的上一次已生效值
    private static Boolean lastCalcSchematicEntities = null;
    private static Boolean lastCalcSchematicEntityContainers = null;
    // 两个开关共用的防抖记录（任一变化即重置计时，稳定后才统一刷新）
    private static boolean pendingEntityCalcChanged = false;
    private static long pendingEntityCalcTime = 0;
    // 刷新执行中标志：执行期间忽略新的开关变化，防止叠加触发
    private static boolean entityCalcRefreshRunning = false;
    // 防抖时长：开关数值稳定 1 秒后才真正刷新，避免玩家频繁开关时反复解析大原理图导致卡顿或崩溃
    private static final long ENTITY_CALC_DEBOUNCE_MS = 1000;
    // 「是否计算建筑材料」开关的上一次已生效值（null 表示尚未初始化）
    private static Boolean lastCalcBuildingMaterials = null;
    private static Boolean pendingCalcBuildingMaterials = null;
    private static long pendingCalcBuildingMaterialsTime = 0;
    private static boolean buildingMaterialsRefreshRunning = false;
    private static final long BUILDING_MATERIALS_DEBOUNCE_MS = 1000;
    // 「统计」页全部类型开关的状态快照（28 项拼接字符串，任一变化即整体变化）
    private static String lastStatsSnapshot = null;
    private static String pendingStatsSnapshot = null;
    private static long pendingStatsSnapshotTime = 0;
    private static boolean statsRefreshRunning = false;
    private static final long STATS_DEBOUNCE_MS = 1000;

    @Override
    public void onInitialize() {
        LOGGER.info("LitematListMod 通用初始化（服务端/客户端）...");
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("LitematListMod 正在初始化...");

        // 创建 litematlist 文件夹（用于存放导入的 txt 材料列表）
        try {
            Path litematlistDir = MinecraftClient.getInstance().runDirectory.toPath().resolve("litematlist");
            if (!Files.exists(litematlistDir)) {
                Files.createDirectories(litematlistDir);
                LOGGER.info("已创建 litematlist 文件夹: {}", litematlistDir);
            }
        } catch (Exception e) {
            LOGGER.error("创建 litematlist 文件夹失败", e);
        }

        // 内置全屏预览：注册缓存清理与每 tick 回调
        PreviewManager.init();

        // 检测 TweakerMore 是否安装
        tweakermoreInstalled = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("tweakermore");
        if (tweakermoreInstalled) {
            LOGGER.info("TweakerMore 模组已检测到");
        }

        // 加载持久化数据（替换映射和忽略物品）
        // 持久化数据将在玩家加入世界时加载（按世界隔离）
        MaterialDataStore.initPersistence();

        InitializationHandler.getInstance().registerInitializationHandler(this);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (Hotkeys.OPEN_MAIN_GUI.getKeybind().isPressed()) {
                MinecraftClient.getInstance().setScreen(new MaterialListScreen());
            }
            if (Hotkeys.OPEN_CONFIG_GUI.getKeybind().isPressed()) {
                MinecraftClient.getInstance().setScreen(new GuiConfigs());
            }
            handleEntryHotkeys(client);

            // 「始终接管材料列表」：持续将上传区域的数据注入到 Litematica DataManager
            if (Configs.Generic.ALWAYS_TAKE_OVER_MATERIAL_LIST.getBooleanValue()) {
                MaterialListInjector.injectIfNeeded();
            }

            // 「是否计算容器数据」开关变化检测：切换后立刻清缓存并刷新材料列表
            checkContainerDataSettingChanged();

            // 「同步投影渲染层」开关变化检测：保存/恢复替换内容并清解析缓存
            checkSyncRenderLayerChanged();

            // 投影渲染层变化检测：稳定 1 秒后把更新的材料列表同步至 HUD
            checkRenderLayerChanged();

            // 「是否计算原理图实体/实体容器数据」开关变化检测：切换后立刻清缓存并刷新材料列表
            checkEntityCalcSettingsChanged();

            // 「是否计算建筑材料」开关变化检测：切换后立刻清缓存并刷新材料列表
            checkBuildingMaterialsSettingChanged();

            // 「统计」页类型开关变化检测：切换后立刻清缓存并刷新材料列表
            checkStatisticsSettingsChanged();

            // HUD 自动刷新
            if (MaterialDataStore.persistedHudVisible) {
                // 启动时重建 HUD（重启后 hudItems 为空，需从持久化上传列表重建）
                if (MaterialListHudRenderer.getHudItems().isEmpty()) {
                    MaterialListHudRenderer.refreshHud();
                }

                // 脏标记刷新：容器内容同步时设置，统一在此处理（避免每帧数十次调用）
                if (MaterialListHudRenderer.hudDirty) {
                    MaterialListHudRenderer.hudDirty = false;
                    MaterialListHudRenderer.updateMissingCounts();
                }

                net.minecraft.client.gui.screen.Screen current = MinecraftClient.getInstance().currentScreen;
                boolean wasGame = previousScreen == null;
                boolean isGame = current == null;
                boolean wasInventory = previousScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen;
                boolean isInventory = current instanceof net.minecraft.client.gui.screen.ingame.HandledScreen;
                // 打开容器时刷新缺失数量
                if (wasGame && isInventory) {
                    MaterialListHudRenderer.updateMissingCounts();
                }
                // 关闭容器时刷新缺失数量
                if (wasInventory && isGame) {
                    MaterialListHudRenderer.updateMissingCounts();
                }
                previousScreen = current;

                // 手动刷新 HUD 快捷键
                if (Configs.Generic.ALLOW_MANUAL_HUD_REFRESH.getBooleanValue()
                        && Hotkeys.REFRESH_MATERIAL_HUD.getKeybind().isPressed()) {
                    MaterialListHudRenderer.updateMissingCounts();
                }

                // HUD 翻页快捷键（防止长按持续翻页）
                if (Hotkeys.HUD_NEXT_PAGE.getKeybind().isPressed()) {
                    if (!hudNextPageProcessed) {
                        hudNextPageProcessed = true;
                        MaterialListHudRenderer.nextPage();
                    }
                } else {
                    hudNextPageProcessed = false;
                }
                if (Hotkeys.HUD_PREV_PAGE.getKeybind().isPressed()) {
                    if (!hudPrevPageProcessed) {
                        hudPrevPageProcessed = true;
                        MaterialListHudRenderer.prevPage();
                    }
                } else {
                    hudPrevPageProcessed = false;
                }
            }

            // HUD 开关快捷键（防止长按持续切换）
            if (Hotkeys.TOGGLE_HUD.getKeybind().isPressed()) {
                if (!toggleHudProcessed) {
                    toggleHudProcessed = true;
                    MaterialDataStore.persistedHudVisible = !MaterialDataStore.persistedHudVisible;
                    MaterialDataStore.savePersistence();
                    if (MaterialDataStore.persistedHudVisible) {
                        MaterialListHudRenderer.refreshHud();
                    }
                }
            } else {
                toggleHudProcessed = false;
            }

            // 快捷打开 HUD 界面设置快捷键
            if (Hotkeys.QUICK_OPEN_HUD_CONFIG.getKeybind().isPressed()) {
                MinecraftClient.getInstance().setScreen(new HudConfigScreen(null));
            }
        });

        // 世界切换时切换持久化数据
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            String worldId = MaterialDataStore.getWorldId();
            LitematListMod.LOGGER.info("加入世界: {}", worldId);
            MaterialDataStore.switchToWorld(worldId);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LitematListMod.LOGGER.info("离开世界，保存持久化数据");
            MaterialDataStore.switchToWorld(null);
        });

        LOGGER.info("LitematListMod 初始化完成!");
    }

    /**
     * 检测「是否计算容器数据」开关是否被玩家中途切换。
     * 采用防抖机制：开关数值需保持稳定 CONTAINER_DATA_DEBOUNCE_MS 毫秒后才真正生效，
     * 玩家在防抖期内反复开关只会重置计时，不会触发任何解析，避免频繁开关造成
     * 「大量数据流」（反复解析大原理图）导致卡顿或崩溃。
     */
    private void checkContainerDataSettingChanged() {
        if (containerDataRefreshRunning) {
            return; // 上一次刷新仍未结束，直接忽略本次变化
        }

        boolean currentValue = Configs.FeatureToggles.CALCULATE_CONTAINER_DATA.getBooleanValue();

        if (lastCalculateContainerData == null) {
            lastCalculateContainerData = currentValue;
            return;
        }

        if (currentValue == lastCalculateContainerData) {
            // 数值已生效，清除未决的防抖记录（玩家可能 A→B→A 来回切换后回到原值）
            pendingCalculateContainerData = null;
            return;
        }

        long now = System.currentTimeMillis();

        if (pendingCalculateContainerData != null && pendingCalculateContainerData == currentValue) {
            if (now - pendingCalculateContainerDataTime < CONTAINER_DATA_DEBOUNCE_MS) {
                return; // 防抖期内，继续等待数值稳定
            }
            // 新值已稳定足够久，执行刷新
            applyContainerDataSettingChanged(currentValue);
        } else {
            // 首次观察到该新值（或玩家又切到了另一个值）：重置防抖计时
            pendingCalculateContainerData = currentValue;
            pendingCalculateContainerDataTime = now;
        }
    }

    /**
     * 执行「是否计算容器数据」切换后的刷新流程。
     * 全程异常保护：任何一步（解析/注入/HUD/界面刷新）异常都不会导致客户端崩溃。
     */
    private void applyContainerDataSettingChanged(boolean newValue) {
        containerDataRefreshRunning = true;
        lastCalculateContainerData = newValue;
        pendingCalculateContainerData = null;
        try {
            LOGGER.info("[LitematListMod] 「是否计算容器数据」已切换为 {}，立即刷新材料列表", newValue);

            // 1. 清空原理图解析缓存，强制按新设置重新解析
            MaterialListInjector.invalidateCaches();

            // 2. 重新注入材料列表到 DataManager（供 TweakerMore 读取，上传区域无条目时自动跳过）
            MaterialListInjector.clearInjection();
            MaterialListInjector.injectIfNeeded();

            // 3. 立刻刷新 HUD
            MaterialListHudRenderer.forceRefreshHud();

            // 4. 刷新当前打开的材料列表界面
            net.minecraft.client.gui.screen.Screen current = MinecraftClient.getInstance().currentScreen;
            if (current instanceof MaterialDetailScreen detailScreen) {
                detailScreen.refreshAfterGlobalSettingChange();
            } else if (current instanceof ProjectSummaryScreen projectSummaryScreen) {
                projectSummaryScreen.refreshAfterGlobalSettingChange();
            }
        } catch (Exception e) {
            LOGGER.error("[LitematListMod] 「是否计算容器数据」切换刷新失败（已捕获，不影响游戏运行）", e);
        } finally {
            containerDataRefreshRunning = false;
        }
    }

    /**
     * 检测「同步投影渲染层」开关是否被玩家中途切换（防抖，与容器数据开关同机制）。
     * 开关切换本身不触发原理图解析；仅保存/恢复替换内容并清空解析缓存，
     * 真正按新渲染层规则重新解析发生在玩家下次打开【查看材料列表】时。
     */
    private void checkSyncRenderLayerChanged() {
        if (syncRenderLayerSwitchRunning) {
            return; // 上一次处理仍未结束，忽略本次变化
        }

        boolean currentValue = Configs.FeatureToggles.SYNC_RENDER_LAYER.getBooleanValue();

        if (lastSyncRenderLayer == null) {
            lastSyncRenderLayer = currentValue;
            return;
        }

        if (currentValue == lastSyncRenderLayer) {
            pendingSyncRenderLayer = null;
            return;
        }

        long now = System.currentTimeMillis();

        if (pendingSyncRenderLayer != null && pendingSyncRenderLayer == currentValue) {
            if (now - pendingSyncRenderLayerTime < SYNC_RENDER_LAYER_DEBOUNCE_MS) {
                return; // 防抖期内，继续等待数值稳定
            }
            applySyncRenderLayerChanged(currentValue);
        } else {
            pendingSyncRenderLayer = currentValue;
            pendingSyncRenderLayerTime = now;
        }
    }

    /**
     * 执行「同步投影渲染层」切换处理：
     * 关→开 保存替换内容快照；开→关 恢复快照（丢弃开启期间的替换更改）。
     * 全程异常保护，任何一步异常都不会导致客户端崩溃。
     */
    private void applySyncRenderLayerChanged(boolean newValue) {
        syncRenderLayerSwitchRunning = true;
        lastSyncRenderLayer = newValue;
        pendingSyncRenderLayer = null;
        try {
            LOGGER.info("[LitematListMod] 「同步投影渲染层」已切换为 {}", newValue);

            if (newValue) {
                // 关→开：保存当前替换内容，供切换回关时恢复查看
                MaterialDataStore.snapshotReplacementsForLitematicaEntries();
            } else {
                // 开→关：恢复快照，不保存开启期间的替换更改
                MaterialDataStore.restoreReplacementsSnapshot();
            }

            // 仅清空解析缓存：下次打开【查看材料列表】时按新开关状态重新解析
            MaterialListInjector.invalidateCaches();
        } catch (Exception e) {
            LOGGER.error("[LitematListMod] 「同步投影渲染层」切换处理失败（已捕获，不影响游戏运行）", e);
        } finally {
            syncRenderLayerSwitchRunning = false;
        }
    }

    /**
     * 检测玩家是否切换了投影渲染层。采用防抖机制：渲染层状态需保持稳定
     * RENDER_LAYER_SIGNATURE_DEBOUNCE_MS 毫秒后，才把更新后的材料列表同步至 HUD，
     * 避免玩家连续滚轮切换渲染层时反复解析大原理图造成卡顿。
     */
    private void checkRenderLayerChanged() {
        if (renderLayerSignatureRefreshRunning) {
            return; // 上一次刷新仍未结束，忽略本次变化
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
        String currentSignature = LitematicaBridge.getRenderLayerStateSignature();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
        // 首次初始化：仅记录当前签名，不触发刷新
        if (!renderLayerSignatureInitialized) {
            lastRenderLayerSignature = currentSignature;
            renderLayerSignatureInitialized = true;
            return;
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
        if (java.util.Objects.equals(currentSignature, lastRenderLayerSignature)) {
            // 渲染层未变化（或未产生有效签名），清除未决的防抖记录
            renderLayerPendingActive = false;
            return;
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
        long now = System.currentTimeMillis();
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
        if (renderLayerPendingActive && java.util.Objects.equals(pendingRenderLayerSignature, currentSignature)) {
            if (now - pendingRenderLayerSignatureTime < RENDER_LAYER_SIGNATURE_DEBOUNCE_MS) {
                return; // 防抖期内，继续等待渲染层稳定
            }
            applyRenderLayerChanged(currentSignature);
        } else {
            // 首次观察到该新签名（或玩家又切到了另一个渲染层）：重置防抖计时
            pendingRenderLayerSignature = currentSignature;
            pendingRenderLayerSignatureTime = now;
            renderLayerPendingActive = true;
        }
    }

    /**
     * 执行渲染层切换后的材料列表同步：渲染层已稳定（防抖结束），按新渲染层规则重新解析
     * 「从投影同步」的材料列表（含已加入项目文件夹的条目），并更新「总材料列表」
     * （DataManager 注入）、HUD 与当前打开的材料列表界面。全程异常保护。
     */
    private void applyRenderLayerChanged(String newSignature) {
        renderLayerSignatureRefreshRunning = true;
        lastRenderLayerSignature = newSignature;
        renderLayerPendingActive = false;
        try {
            LOGGER.info("[LitematListMod] 投影渲染层已稳定，同步刷新材料列表至 HUD");
//#if MC == 12101
//$$
//#endif
//#if MC == 12110

//#endif
            // 1. 清空原理图解析缓存，强制按新渲染层规则重新解析（含「从投影同步」及已加入项目的条目）
            MaterialListInjector.invalidateCaches();
//#if MC == 12101
//$$
//#endif
//#if MC == 12110

//#endif
            // 2. 重新注入材料列表到 DataManager（总材料列表；上传区域无条目时自动跳过）
            MaterialListInjector.clearInjection();
            MaterialListInjector.injectIfNeeded();
//#if MC == 12101
//$$
//#endif
//#if MC == 12110

//#endif
            // 3. 立刻刷新 HUD
            MaterialListHudRenderer.forceRefreshHud();
//#if MC == 12101
//$$
//#endif
//#if MC == 12110

//#endif
            // 4. 刷新当前打开的材料列表界面
            net.minecraft.client.gui.screen.Screen current = MinecraftClient.getInstance().currentScreen;
            if (current instanceof MaterialDetailScreen detailScreen) {
                detailScreen.refreshAfterGlobalSettingChange();
            } else if (current instanceof ProjectSummaryScreen projectSummaryScreen) {
                projectSummaryScreen.refreshAfterGlobalSettingChange();
            }
        } catch (Exception e) {
            LOGGER.error("[LitematListMod] 渲染层变化后刷新 HUD 失败（已捕获，不影响游戏运行）", e);
        } finally {
            renderLayerSignatureRefreshRunning = false;
        }
    }

    /**
     * 检测「是否计算原理图实体」/「是否计算原理图实体容器数据」两个开关是否被玩家中途切换。
     * 两个开关共用一个防抖计时：任一开关变化即重置计时，数值稳定
     * ENTITY_CALC_DEBOUNCE_MS 毫秒后才统一刷新，避免频繁开关时反复解析大原理图。
     */
    private void checkEntityCalcSettingsChanged() {
        if (entityCalcRefreshRunning) {
            return; // 上一次刷新仍未结束，直接忽略本次变化
        }

        boolean entitiesNow = Configs.FeatureToggles.CALC_SCHEMATIC_ENTITIES.getBooleanValue();
        boolean containersNow = Configs.FeatureToggles.CALC_SCHEMATIC_ENTITY_CONTAINERS.getBooleanValue();

        if (lastCalcSchematicEntities == null || lastCalcSchematicEntityContainers == null) {
            lastCalcSchematicEntities = entitiesNow;
            lastCalcSchematicEntityContainers = containersNow;
            return;
        }

        if (entitiesNow == lastCalcSchematicEntities && containersNow == lastCalcSchematicEntityContainers) {
            // 数值已生效，清除未决的防抖记录（玩家可能 A→B→A 来回切换后回到原值）
            pendingEntityCalcChanged = false;
            return;
        }

        long now = System.currentTimeMillis();

        if (pendingEntityCalcChanged) {
            if (now - pendingEntityCalcTime < ENTITY_CALC_DEBOUNCE_MS) {
                return; // 防抖期内，继续等待数值稳定
            }
            // 新值已稳定足够久，执行刷新
            applyEntityCalcSettingsChanged(entitiesNow, containersNow);
        } else {
            // 首次观察到变化（或玩家又切到另一组值）：重置防抖计时
            pendingEntityCalcChanged = true;
            pendingEntityCalcTime = now;
        }
    }

    /**
     * 执行原理图实体相关开关切换后的刷新流程。
     * 全程异常保护：任何一步（解析/注入/HUD/界面刷新）异常都不会导致客户端崩溃。
     */
    private void applyEntityCalcSettingsChanged(boolean entitiesEnabled, boolean containersEnabled) {
        entityCalcRefreshRunning = true;
        lastCalcSchematicEntities = entitiesEnabled;
        lastCalcSchematicEntityContainers = containersEnabled;
        pendingEntityCalcChanged = false;
        try {
            LOGGER.info("[LitematListMod] 「是否计算原理图实体」={}，「是否计算原理图实体容器数据」={}，立即刷新材料列表",
                    entitiesEnabled, containersEnabled);

            // 1. 清空原理图解析缓存，强制按新设置重新解析
            MaterialListInjector.invalidateCaches();

            // 2. 重新注入材料列表到 DataManager（供 TweakerMore 读取，上传区域无条目时自动跳过）
            MaterialListInjector.clearInjection();
            MaterialListInjector.injectIfNeeded();

            // 3. 立刻刷新 HUD
            MaterialListHudRenderer.forceRefreshHud();

            // 4. 刷新当前打开的材料列表界面
            net.minecraft.client.gui.screen.Screen current = MinecraftClient.getInstance().currentScreen;
            if (current instanceof MaterialDetailScreen detailScreen) {
                detailScreen.refreshAfterGlobalSettingChange();
            } else if (current instanceof ProjectSummaryScreen projectSummaryScreen) {
                projectSummaryScreen.refreshAfterGlobalSettingChange();
            }
        } catch (Exception e) {
            LOGGER.error("[LitematListMod] 原理图实体相关开关切换刷新失败（已捕获，不影响游戏运行）", e);
        } finally {
            entityCalcRefreshRunning = false;
        }
    }

    /**
     * 检测「是否计算建筑材料」开关是否被玩家中途切换（防抖，与容器数据开关同机制）。
     * 数值稳定 BUILDING_MATERIALS_DEBOUNCE_MS 毫秒后才真正刷新，避免频繁开关反复解析大原理图。
     */
    private void checkBuildingMaterialsSettingChanged() {
        if (buildingMaterialsRefreshRunning) {
            return; // 上一次刷新仍未结束，直接忽略本次变化
        }

        boolean currentValue = Configs.FeatureToggles.CALC_BUILDING_MATERIALS.getBooleanValue();

        if (lastCalcBuildingMaterials == null) {
            lastCalcBuildingMaterials = currentValue;
            return;
        }

        if (currentValue == lastCalcBuildingMaterials) {
            // 数值已生效，清除未决的防抖记录（玩家可能 A→B→A 来回切换后回到原值）
            pendingCalcBuildingMaterials = null;
            return;
        }

        long now = System.currentTimeMillis();

        if (pendingCalcBuildingMaterials != null && pendingCalcBuildingMaterials == currentValue) {
            if (now - pendingCalcBuildingMaterialsTime < BUILDING_MATERIALS_DEBOUNCE_MS) {
                return; // 防抖期内，继续等待数值稳定
            }
            // 新值已稳定足够久，执行刷新
            applyBuildingMaterialsSettingChanged(currentValue);
        } else {
            // 首次观察到该新值（或玩家又切到了另一个值）：重置防抖计时
            pendingCalcBuildingMaterials = currentValue;
            pendingCalcBuildingMaterialsTime = now;
        }
    }

    /**
     * 执行「是否计算建筑材料」切换后的刷新流程（普通材料列表与项目文件夹总材料列表都会刷新）。
     * 全程异常保护：任何一步（解析/注入/HUD/界面刷新）异常都不会导致客户端崩溃。
     */
    private void applyBuildingMaterialsSettingChanged(boolean newValue) {
        buildingMaterialsRefreshRunning = true;
        lastCalcBuildingMaterials = newValue;
        pendingCalcBuildingMaterials = null;
        try {
            LOGGER.info("[LitematListMod] 「是否计算建筑材料」已切换为 {}，立即刷新材料列表", newValue);

            // 1. 清空原理图解析缓存（含注入器缓存），强制按新设置重新解析
            MaterialListInjector.invalidateCaches();

            // 2. 重新注入材料列表到 DataManager（供 TweakerMore 读取，上传区域无条目时自动跳过）
            MaterialListInjector.clearInjection();
            MaterialListInjector.injectIfNeeded();

            // 3. 立刻刷新 HUD
            MaterialListHudRenderer.forceRefreshHud();

            // 4. 刷新当前打开的材料列表界面（普通列表 / 项目总材料列表）
            net.minecraft.client.gui.screen.Screen current = MinecraftClient.getInstance().currentScreen;
            if (current instanceof MaterialDetailScreen detailScreen) {
                detailScreen.refreshAfterGlobalSettingChange();
            } else if (current instanceof ProjectSummaryScreen projectSummaryScreen) {
                projectSummaryScreen.refreshAfterGlobalSettingChange();
            }
        } catch (Exception e) {
            LOGGER.error("[LitematListMod] 「是否计算建筑材料」切换刷新失败（已捕获，不影响游戏运行）", e);
        } finally {
            buildingMaterialsRefreshRunning = false;
        }
    }

    /**
     * 检测「统计」页全部类型开关是否有变化（防抖，与容器数据开关同机制）。
     * 28 项开关状态拼为快照字符串，任一变化即重置计时，稳定 STATS_DEBOUNCE_MS 毫秒后才刷新。
     */
    private void checkStatisticsSettingsChanged() {
        if (statsRefreshRunning) {
            return; // 上一次刷新仍未结束，直接忽略本次变化
        }

        String currentValue = Configs.Statistics.snapshot();

        if (lastStatsSnapshot == null) {
            lastStatsSnapshot = currentValue;
            return;
        }

        if (currentValue.equals(lastStatsSnapshot)) {
            // 快照已生效，清除未决的防抖记录（玩家可能 A→B→A 来回切换后回到原值）
            pendingStatsSnapshot = null;
            return;
        }

        long now = System.currentTimeMillis();

        if (pendingStatsSnapshot != null && pendingStatsSnapshot.equals(currentValue)) {
            if (now - pendingStatsSnapshotTime < STATS_DEBOUNCE_MS) {
                return; // 防抖期内，继续等待快照稳定
            }
            // 新快照已稳定足够久，执行刷新
            applyStatisticsSettingsChanged(currentValue);
        } else {
            // 首次观察到该新快照（或玩家又改动了其它开关）：重置防抖计时
            pendingStatsSnapshot = currentValue;
            pendingStatsSnapshotTime = now;
        }
    }

    /**
     * 执行「统计」类型开关切换后的刷新流程（普通材料列表与项目文件夹总材料列表都会刷新）。
     * 全程异常保护：任何一步（解析/注入/HUD/界面刷新）异常都不会导致客户端崩溃。
     */
    private void applyStatisticsSettingsChanged(String newValue) {
        statsRefreshRunning = true;
        lastStatsSnapshot = newValue;
        pendingStatsSnapshot = null;
        try {
            LOGGER.info("[LitematListMod] 「统计」类型开关已变化（快照 {}），立即刷新材料列表", newValue);

            // 1. 清空原理图解析缓存（含注入器缓存），强制按新设置重新解析
            MaterialListInjector.invalidateCaches();

            // 2. 重新注入材料列表到 DataManager（供 TweakerMore 读取，上传区域无条目时自动跳过）
            MaterialListInjector.clearInjection();
            MaterialListInjector.injectIfNeeded();

            // 3. 立刻刷新 HUD
            MaterialListHudRenderer.forceRefreshHud();

            // 4. 刷新当前打开的材料列表界面（普通列表 / 项目总材料列表）
            net.minecraft.client.gui.screen.Screen current = MinecraftClient.getInstance().currentScreen;
            if (current instanceof MaterialDetailScreen detailScreen) {
                detailScreen.refreshAfterGlobalSettingChange();
            } else if (current instanceof ProjectSummaryScreen projectSummaryScreen) {
                projectSummaryScreen.refreshAfterGlobalSettingChange();
            }
        } catch (Exception e) {
            LOGGER.error("[LitematListMod] 「统计」类型开关切换刷新失败（已捕获，不影响游戏运行）", e);
        } finally {
            statsRefreshRunning = false;
        }
    }

    /** 清空各材料列表快捷键缓存（切换世界或修改快捷键时调用） */
    public static void clearEntryHotkeyCache() {
        entryHotkeyCache.clear();
        entryHotkeyCacheSrc.clear();
    }

    /** 每客户端 tick 检查各材料列表的「快捷打开」快捷键 */
    private void handleEntryHotkeys(MinecraftClient client) {
        java.util.List<MaterialListScreen.LoadedEntry> entries = MaterialListScreen.getLoadedEntries();
        if (entries.isEmpty()) return;

        // 快速检查是否有任何条目设置了快捷键，无需则跳过遍历
        boolean hasAnyHotkey = false;
        for (MaterialListScreen.LoadedEntry e : entries) {
            if (e.hotkey() != null && !e.hotkey().isEmpty()) {
                hasAnyHotkey = true;
                break;
            }
        }
        if (!hasAnyHotkey) return;

        for (MaterialListScreen.LoadedEntry entry : entries) {
            if (entry.hotkey() == null || entry.hotkey().isEmpty()) continue;
            String key = entry.filePath() != null ? entry.filePath().toString() : entry.name();

            IKeybind kb = entryHotkeyCache.get(key);
            String cachedSrc = entryHotkeyCacheSrc.get(key);
            if (cachedSrc == null || !cachedSrc.equals(entry.hotkey())) {
                kb = buildEntryHotkey(entry.hotkey());
                entryHotkeyCache.put(key, kb);
                entryHotkeyCacheSrc.put(key, entry.hotkey());
            }
            if (kb == null) continue;

            kb.updateIsPressed();
            if (kb.isPressed()) {
                if (entry.filePath() == null) {
                    // 项目文件夹：打开项目界面
                    java.util.List<MaterialListScreen.LoadedEntry> children = MaterialListScreen.getProjectEntries(entry.name());
                    MinecraftClient.getInstance().setScreen(new ProjectScreen(new MaterialListScreen(), entry.name(), children));
                } else {
                    MinecraftClient.getInstance().setScreen(
                            new MaterialDetailScreen(new MaterialListScreen(), entry.name(), entry.filePath()));
                }
            }
            kb.tick();
        }
    }

    private IKeybind buildEntryHotkey(String hotkey) {
        try {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            ConfigHotkey holder = new ConfigHotkey("entry_hotkey", hotkey, KeybindSettings.RELEASE_EXCLUSIVE);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            ConfigHotkey holder = new ConfigHotkey("entry_hotkey", hotkey, KeybindSettings.RELEASE_EXCLUSIVE, "");
//#endif
            return holder.getKeybind();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void registerModHandlers() {
        ConfigManager.getInstance().registerConfigHandler(MOD_ID, new Configs());
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        Registry.CONFIG_SCREEN.registerConfigScreenFactory(
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                new ModInfo(MOD_ID, MOD_NAME, () -> new GuiConfigs())
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        );
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        InputEventHandler.getKeybindManager().registerKeybindProvider(this);
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$                InputEventHandler.getKeybindManager().registerKeybindProvider(this);
//#endif
        LOGGER.info("LitematListMod 已注册 MaLib 处理器（Config + ConfigScreen + Keybind）");
    }

    @Override
    public void addKeysToMap(IKeybindManager manager) {
        for (IHotkey hotkey : Hotkeys.HOTKEY_LIST) {
            manager.addKeybindToMap(hotkey.getKeybind());
        }
        // 「功能开关」页开关自带的快捷键也要注册，否则按键不会触发切换回调
        for (IConfigBase config : Configs.FeatureToggles.TOGGLE_LIST) {
            if (config instanceof IHotkey hotkey) {
                manager.addKeybindToMap(hotkey.getKeybind());
            }
        }
    }

    @Override
    public void addHotkeys(IKeybindManager manager) {
        manager.addHotkeysForCategory(MOD_NAME, "litematlist.hotkeys.category.main", Hotkeys.HOTKEY_LIST);
    }

    /** 检查 TweakerMore 是否已安装 */
    public static boolean isTweakermoreInstalled() {
        return tweakermoreInstalled;
    }
}