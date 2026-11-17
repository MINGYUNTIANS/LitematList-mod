package com.litematlist.gui;

import com.litematlist.I18n;
import com.litematlist.LitematListMod;
import com.litematlist.MaterialListInjector;
import com.litematlist.RawMaterialAnalyzer;
import com.litematlist.RawMaterialTreeAnalyzer;
import com.litematlist.LitematicReader.MaterialEntry;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.InfoUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 单个材料列表/项目文件夹的配置界面。
 * 提供「快捷打开」的快捷键设置。
 */
public class MaterialConfigScreen extends GuiBase {

    private final GuiBase parent;
    private final MaterialListScreen.LoadedEntry entry;
    private boolean listening = false;
    private final java.util.List<Integer> pressedKeys = new java.util.ArrayList<>();
    private final java.util.Set<Integer> heldKeys = new java.util.LinkedHashSet<>();

    // 横向布局位置
    private String labelText = "";
    private int labelX, labelY;
    private int setBtnX, setBtnY, setBtnW, setBtnH;

    // PlayerControl++ 注入失败日志的频率阈值：同一路径 30 秒内只输出一条，避免高频重试时刷屏
    private static final long PCP_FAIL_LOG_COOLDOWN = 30000;
    private static final Map<String, Long> lastPcpFailLogTimes = new java.util.HashMap<>();

    /** 按路径做频率限制的注入失败 warn：同一路径 30 秒内只输出一条。 */
    private static void logPcpInjectFailThrottled(String pathKey) {
        long now = System.currentTimeMillis();
        Long last = lastPcpFailLogTimes.get(pathKey);
        if (last != null && now - last < PCP_FAIL_LOG_COOLDOWN) {
            return;
        }
        lastPcpFailLogTimes.put(pathKey, now);
        LitematListMod.LOGGER.warn("[MaterialConfigScreen] PlayerControl++ 注入失败：无法加载材料列表: {}", pathKey);
    }

    public MaterialConfigScreen(GuiBase parent, MaterialListScreen.LoadedEntry entry) {
        super();
        this.parent = parent;
        this.entry = entry;
        this.title = I18n.tr("litematlist.title.material_config", entry.name());
    }

    @Override
    public void initGui() {
        super.initGui();

        // 横向排列：标签 + 快捷键按钮 + 重置按钮
        String label = I18n.tr("litematlist.config.quick_open");
        this.labelText = label;
        String hotkeyLabel = listening ? I18n.tr("litematlist.config.press_key") : getHotkeyDisplay();
        int labelW = this.font.width(label);
        int hotkeyW = Math.max(80, this.font.width(hotkeyLabel) + 16);
        int resetW = 60;
        int gap = 8;
        int totalW = labelW + gap + hotkeyW + 5 + resetW;
        int startX = (this.width - totalW) / 2;
        int rowY = 70;

        this.labelX = startX;
        this.labelY = rowY + 6;

        this.setBtnX = startX + labelW + gap;
        this.setBtnY = rowY;
        this.setBtnW = hotkeyW;
        this.setBtnH = 20;
        ButtonGeneric setBtn = new ButtonGeneric(this.setBtnX, this.setBtnY, hotkeyW, 20, hotkeyLabel);
        setBtn.setRenderDefaultBackground(true);
        this.addButton(setBtn, (IButtonActionListener) (b, mb) -> {
            this.listening = !this.listening;
            if (this.listening) {
                this.pressedKeys.clear();
                this.heldKeys.clear();
            }
            initGui();
        });

        int resetX = this.setBtnX + hotkeyW + 5;
        ButtonGeneric resetBtn = new ButtonGeneric(resetX, rowY, resetW, 20, I18n.tr("litematlist.button.reset"));
        resetBtn.setRenderDefaultBackground(true);
        this.addButton(resetBtn, (IButtonActionListener) (b, mb) -> {
            this.entry.hotkey = "";
            MaterialDataStore.savePersistence();
            LitematListMod.clearEntryHotkeyCache();
            initGui();
        });

        // 「上传」按钮：仅主页面（根级）条目显示，项目文件夹内部不显示
        if (entry.projectId == null) {
            // 如果上传区域已有条目，且不是当前条目，则禁用上传按钮
            MaterialListScreen.LoadedEntry alreadyUploaded = MaterialListScreen.getUploadedEntry();
            boolean uploadDisabled = (alreadyUploaded != null && alreadyUploaded != entry);
            ButtonGeneric uploadBtn = new ButtonGeneric(this.width / 2 - 60, 100, 120, 20, I18n.tr("litematlist.button.upload"));
            uploadBtn.setRenderDefaultBackground(true);
            if (uploadDisabled) {
                uploadBtn.setEnabled(false);
            }
            this.addButton(uploadBtn, (IButtonActionListener) (b, mb) -> {
                MaterialListScreen.uploadEntry(entry);

                // 无条件注入配方树到 PlayerControl++ 字段（加载失败会回滚上传状态）
                boolean pcpOk = injectPlayerControlPlus(entry);

                LitematListMod.LOGGER.info("[MaterialConfigScreen] 上传到上传区域: {}", entry.name());
                // 先切回父界面再弹提示，避免消息随本界面关闭而丢失
                Minecraft.getInstance().setScreenAndShow(parent);
                InfoUtils.showGuiOrInGameMessage(Message.MessageType.SUCCESS, "litematlist.message.upload_success");
                if (pcpOk) {
                    InfoUtils.showGuiOrInGameMessage(Message.MessageType.SUCCESS, "litematlist.message.pcp_upload_success");
                } else {
                    InfoUtils.showGuiOrInGameMessage(Message.MessageType.ERROR, "litematlist.message.pcp_upload_failed");
                }
            });
        }

        int buttonY = this.height - 38;
        ButtonGeneric backBtn = new ButtonGeneric(this.width / 2 - 60, buttonY, 120, 20, I18n.tr("litematlist.button.back"));
        backBtn.setRenderDefaultBackground(true);
        this.addButton(backBtn, (IButtonActionListener) (b, mb) ->
                Minecraft.getInstance().setScreenAndShow(parent));
    }

    /** 获取当前快捷键的显示名称（未设置则返回「未设置」） */
    private String getHotkeyDisplay() {
        if (entry.hotkey == null || entry.hotkey.isEmpty()) {
            return I18n.tr("litematlist.config.no_hotkey");
        }
        try {
            ConfigHotkey hk = new ConfigHotkey("entry_hotkey", entry.hotkey, KeybindSettings.RELEASE_EXCLUSIVE);
            String s = hk.getKeybind().getKeysDisplayString();
            return (s == null || s.isEmpty()) ? entry.hotkey : s;
        } catch (Exception e) {
            return entry.hotkey;
        }
    }

    /** 取消录制 */
    private void cancelRecording() {
        this.listening = false;
        this.pressedKeys.clear();
        this.heldKeys.clear();
        initGui();
    }

    /** 结束录制，把按下的键保存为快捷键 */
    private void finishRecording() {
        if (!this.pressedKeys.isEmpty()) {
            ConfigHotkey tmp = new ConfigHotkey("entry_hotkey", "", KeybindSettings.RELEASE_EXCLUSIVE);
            for (int key : this.pressedKeys) {
                tmp.getKeybind().addKey(key);
            }
            this.entry.hotkey = tmp.getStringValue();
            MaterialDataStore.savePersistence();
            LitematListMod.clearEntryHotkeyCache();
        }
        this.listening = false;
        this.pressedKeys.clear();
        this.heldKeys.clear();
        initGui();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.listening) {
            int key = event.key();
            if (key == 256) { // ESC：取消录制
                cancelRecording();
                return true;
            }
            if (!this.pressedKeys.contains(key)) {
                this.pressedKeys.add(key);
            }
            this.heldKeys.add(key);
            return true;
        }
        if (event.key() == 256) { // ESC：返回上一级菜单
            Minecraft.getInstance().setScreenAndShow(parent);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (this.listening) {
            this.heldKeys.remove(Integer.valueOf(event.key()));
            if (this.heldKeys.isEmpty()) {
                finishRecording();
            }
            return true;
        }
        return super.keyReleased(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks) {
        GuiContext ctx = GuiContext.fromGuiGraphics(gfx);
        this.extractBackground(gfx, mouseX, mouseY, partialTicks);
        super.extractRenderState(gfx, mouseX, mouseY, partialTicks);
        // 绘制标签
        ctx.drawString(this.font, this.labelText, this.labelX, this.labelY, 0xFFFFFFFF);

        // 录制快捷键时，实时显示当前按下的按键
        if (this.listening && !this.heldKeys.isEmpty()) {
            String realtimeKeys = getRealtimeKeyDisplay();
            int textW = this.font.width(realtimeKeys);
            int textX = this.setBtnX + (this.setBtnW - textW) / 2;
            ctx.drawString(this.font, realtimeKeys, textX, this.setBtnY + 5, 0xFFFFFF00);
        }

        // 快捷键按钮悬停提示
        if (mouseX >= this.setBtnX && mouseX <= this.setBtnX + this.setBtnW &&
                mouseY >= this.setBtnY && mouseY <= this.setBtnY + this.setBtnH) {
            drawTooltip(ctx, I18n.tr("litematlist.tooltip.hotkey_conflict"), mouseX, mouseY);
        }
    }

    /** 获取当前实时按下的按键显示字符串 */
    private String getRealtimeKeyDisplay() {
        try {
            ConfigHotkey tmp = new ConfigHotkey("entry_hotkey", "", KeybindSettings.RELEASE_EXCLUSIVE);
            for (int key : this.pressedKeys) {
                tmp.getKeybind().addKey(key);
            }
            String s = tmp.getKeybind().getKeysDisplayString();
            return (s == null || s.isEmpty()) ? "..." : s;
        } catch (Exception e) {
            return "...";
        }
    }

    private void drawTooltip(GuiContext ctx, String tip, int mouseX, int mouseY) {
        int w = this.font.width(tip) + 8;
        int x = mouseX + 8;
        int y = mouseY + 12;
        ctx.fill(x, y, x + w, y + 14, 0xF0000000);
        ctx.drawString(this.font, tip, x + 4, y + 3, 0xFFFFFFFF);
    }

    /**
     * 注入 PlayerControl++ 配方树数据：使用配方树分析器生成配方树，
     * 注入到 MaterialDetailScreen.playerControlPlusMaterialList 字段供 PlayerControl++ 读取。
     *
     * @return true 注入成功；false 材料加载失败（已回滚上传状态）
     */
    public static boolean injectPlayerControlPlus(MaterialListScreen.LoadedEntry entry) {
        String pathKey = entry.filePath() != null ? entry.filePath().toString() : entry.name();

        // 1. 加载材料列表（项目文件夹聚合全部子列表）
        List<MaterialEntry> materialEntries;
        if (entry.isProject) {
            materialEntries = loadProjectMaterials(entry.name());
        } else {
            materialEntries = MaterialListInjector.loadMaterialsCached(entry.filePath());
            if (materialEntries == null || materialEntries.isEmpty()) {
                Map<Path, List<MaterialEntry>> imported = MaterialDataStore.getImportedMaterials();
                materialEntries = imported.get(entry.filePath());
            }
        }

        if (materialEntries == null || materialEntries.isEmpty()) {
            // 加载失败：回滚上传状态，避免上传区域出现「假上传」条目
            MaterialDataStore.playerControlPlusUploaded = null;
            MaterialDataStore.playerControlPlusMaterialList = null;
            MaterialDataStore.savePersistence();
            logPcpInjectFailThrottled(pathKey);
            return false;
        }

        // 2. 记录上传状态
        MaterialDataStore.playerControlPlusUploaded = pathKey;

        // 3. 生成配方树
        RawMaterialTreeAnalyzer analyzer = new RawMaterialTreeAnalyzer();
        analyzer.setMaxDepth(20);
        RawMaterialTreeAnalyzer.TreeResult result = analyzer.analyze(materialEntries);
        MaterialDataStore.playerControlPlusMaterialList = result.roots();

        // 4. 同时生成原材料表和冗余数据（供原材料入口与冗余列表读取）
        RawMaterialAnalyzer rawAnalyzer = new RawMaterialAnalyzer();
        RawMaterialAnalyzer.RawMaterialResult rawResult = rawAnalyzer.analyze(materialEntries);
        MaterialDataStore.rawMaterials.put(pathKey, rawResult.rawMaterials());
        MaterialDataStore.rawMaterialRedundancy.put(pathKey, rawResult.redundancy());

        MaterialDataStore.savePersistence();
        LitematListMod.LOGGER.info("[MaterialConfigScreen] 已注入配方树到 PlayerControl++ 字段: {} 个根节点, {} 项原材料",
                result.roots().size(), rawResult.rawMaterials().size());
        LitematListMod.LOGGER.info("[MaterialConfigScreen] PlayerControl++ 配方树注入完成: {}", entry.name());
        return true;
    }

    /** 聚合项目文件夹全部子列表的材料（应用替换/忽略后按物品合并） */
    private static List<MaterialEntry> loadProjectMaterials(String projectName) {
        Map<net.minecraft.world.item.Item, Integer> totals = new java.util.LinkedHashMap<>();
        Map<net.minecraft.world.item.Item, String> names = new java.util.LinkedHashMap<>();
        for (MaterialListScreen.LoadedEntry child : MaterialListScreen.getProjectEntries(projectName)) {
            if (child.filePath() == null) continue;
            List<MaterialEntry> childMaterials = MaterialListInjector.loadMaterialsCached(child.filePath());
            if (childMaterials == null || childMaterials.isEmpty()) {
                Map<Path, List<MaterialEntry>> imported = MaterialDataStore.getImportedMaterials();
                childMaterials = imported.get(child.filePath());
            }
            if (childMaterials == null) continue;
            for (MaterialEntry m : childMaterials) {
                // 精确物品（带非默认组件）不参与溯源：聚合时剔除，避免塌缩进普通物品污染数量
                if (m.isExact()) continue;
                net.minecraft.world.item.Item item = m.item();
                net.minecraft.world.item.Item replaced = MaterialDataStore.getReplacement(child.filePath(), item);
                if (replaced != item) item = replaced;
                if (MaterialDataStore.getIgnoredForPath(child.filePath()).contains(item)) continue;
                totals.merge(item, m.totalCount(), Integer::sum);
                names.putIfAbsent(item, m.blockName());
            }
        }
        List<MaterialEntry> merged = new java.util.ArrayList<>();
        for (Map.Entry<net.minecraft.world.item.Item, Integer> e : totals.entrySet()) {
            merged.add(new MaterialEntry(e.getKey(), names.getOrDefault(e.getKey(), ""), e.getValue()));
        }
        return merged;
    }
}