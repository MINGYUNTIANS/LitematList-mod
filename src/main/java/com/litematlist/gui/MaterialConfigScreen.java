package com.litematlist.gui;

import com.litematlist.I18n;
import com.litematlist.LitematListMod;
import com.litematlist.MaterialListInjector;
import com.litematlist.RawMaterialAnalyzer;
import com.litematlist.RawMaterialTreeAnalyzer;
import com.litematlist.LitematicReader.MaterialEntry;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import fi.dy.masa.malilib.util.InfoUtils;
import net.minecraft.client.MinecraftClient;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.gui.DrawContext;
//#endif
//#if MC == 12110 || MC == 12111
import net.minecraft.client.input.KeyInput;
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$
//#endif

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.gui.DrawableHelper;
//#endif
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.util.math.MatrixStack;
//#endif

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
/**
 * 单个材料列表/项目文件夹的配置界面。
 * 提供「快捷打开」的快捷键设置。
 */
//#elseif MC == 11701 || MC == 11904
//$$/**
//$$ * 鍗曚釜鏉愭枡鍒楄〃/椤圭洰鏂囦欢澶圭殑閰嶇疆鐣岄潰銆? * 鎻愪緵銆屽揩鎹锋墦寮€銆嶇殑蹇嵎閿缃€? */
//#endif
public class MaterialConfigScreen extends GuiBase {

    private final GuiBase parent;
    private final MaterialListScreen.LoadedEntry entry;
    private boolean listening = false;
    private final java.util.List<Integer> pressedKeys = new java.util.ArrayList<>();
    private final java.util.Set<Integer> heldKeys = new java.util.LinkedHashSet<>();

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    // 横向布局位置
//#elseif MC == 11701 || MC == 11904
//$$    // 妯‌鍚戝竷灞€浣嶇疆
//#endif
    private String labelText = "";
    private int labelX, labelY;
    private int setBtnX, setBtnY, setBtnW, setBtnH;

    // PlayerControl++ 注入失败日志的频率阈值：同一路径 30 秒内只输出一条，避免高频重试时刷屏
    private static final long PCP_FAIL_LOG_COOLDOWN = 30000;
    private static final Map<String, Long> lastPcpFailLogTimes = new java.util.HashMap<>();
//#if MC == 11701 || MC == 11904 || MC == 12001
//$$
//#endif
//#if MC == 12110 || MC == 12111

//#endif
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

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 横向排列：标签 + 快捷键按钮 + 重置按钮
//#elseif MC == 11701 || MC == 11904
//$$        // 妯悜鎺掑垪锛氭爣绛?+ 蹇嵎閿寜閽?+ 閲嶇疆鎸夐挳
//#endif
        String label = I18n.tr("litematlist.config.quick_open");
        this.labelText = label;
        String hotkeyLabel = listening ? I18n.tr("litematlist.config.press_key") : getHotkeyDisplay();
        int labelW = this.textRenderer.getWidth(label);
        int hotkeyW = Math.max(80, this.textRenderer.getWidth(hotkeyLabel) + 16);
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

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 「上传」按钮：仅主页面（根级）条目显示，项目文件夹内部不显示
//#endif
        if (entry.projectId == null) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            // 如果上传区域已有条目，且不是当前条目，则禁用上传按钮
//#endif
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

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                LitematListMod.LOGGER.info("[MaterialConfigScreen] 上传到上传区域: {}", entry.name());
//#elseif MC == 11701 || MC == 11904
//$$                LitematListMod.LOGGER.info("[MaterialConfigScreen] 涓婁紶鍒颁笂浼犲尯鍩? {}", entry.name());
//#endif
                // 先切回父界面再弹提示，避免消息随本界面关闭而丢失
                MinecraftClient.getInstance().setScreen(parent);
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
                MinecraftClient.getInstance().setScreen(parent));
    }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 获取当前快捷键的显示名称（未设置则返回「未设置」） */
//#elseif MC == 11701 || MC == 11904
//$$    /** 鑾峰彇褰撳墠蹇嵎閿殑鏄剧ず鍚嶇О锛堟湭璁剧疆鍒欒繑鍥炪€屾湭璁剧疆銆嶏級 */
//#endif
    private String getHotkeyDisplay() {
        if (entry.hotkey == null || entry.hotkey.isEmpty()) {
            return I18n.tr("litematlist.config.no_hotkey");
        }
        try {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            ConfigHotkey hk = new ConfigHotkey("entry_hotkey", entry.hotkey, KeybindSettings.RELEASE_EXCLUSIVE);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            ConfigHotkey hk = new ConfigHotkey("entry_hotkey", entry.hotkey, KeybindSettings.RELEASE_EXCLUSIVE, "");
//#endif
            String s = hk.getKeybind().getKeysDisplayString();
            return (s == null || s.isEmpty()) ? entry.hotkey : s;
        } catch (Exception e) {
            return entry.hotkey;
        }
    }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 取消录制 */
//#elseif MC == 11701 || MC == 11904
//$$    /** 鍙栨秷褰曞埗 */
//#endif
    private void cancelRecording() {
        this.listening = false;
        this.pressedKeys.clear();
        this.heldKeys.clear();
        initGui();
    }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 结束录制，把按下的键保存为快捷键 */
//#elseif MC == 11701 || MC == 11904
//$$    /** 缁撴潫褰曞埗锛屾妸鎸変笅鐨勯敭淇濆瓨涓哄揩鎹烽敭 */
//#endif
    private void finishRecording() {
        if (!this.pressedKeys.isEmpty()) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            ConfigHotkey tmp = new ConfigHotkey("entry_hotkey", "", KeybindSettings.RELEASE_EXCLUSIVE);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            ConfigHotkey tmp = new ConfigHotkey("entry_hotkey", "", KeybindSettings.RELEASE_EXCLUSIVE, "");
//#endif
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
//#if MC == 12110 || MC == 12111
    public boolean keyPressed(KeyInput keyInput) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
//#endif
        if (this.listening) {
//#if MC == 12110 || MC == 12111
            int key = keyInput.key();
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            int key = keyCode;
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            if (key == 256) { // ESC：取消录制
//#elseif MC == 11701 || MC == 11904
//$$            if (key == 256) {
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                cancelRecording();
//#elseif MC == 11701 || MC == 11904
//$$                cancelRecording(); // ESC
//#endif
                return true;
            }
            if (!this.pressedKeys.contains(key)) {
                this.pressedKeys.add(key);
            }
            this.heldKeys.add(key);
            return true;
        }
//#if MC == 12110 || MC == 12111
        if (keyInput.key() == 256) { // ESC：返回上一级菜单
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        if (keyCode == 256) { // ESC：返回上一级菜单
//#endif
            MinecraftClient.getInstance().setScreen(parent);
            return true;
        }
//#if MC == 12110 || MC == 12111
        return super.keyPressed(keyInput);
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$        return super.keyPressed(keyCode, scanCode, modifiers);
//#endif
    }

    @Override
//#if MC == 12110 || MC == 12111
    public boolean keyReleased(KeyInput keyInput) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
//#endif
        if (this.listening) {
//#if MC == 12110 || MC == 12111
            this.heldKeys.remove(Integer.valueOf(keyInput.key()));
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$            this.heldKeys.remove(Integer.valueOf(keyCode));
//#endif
            if (this.heldKeys.isEmpty()) {
                finishRecording();
            }
            return true;
        }
//#if MC == 12110 || MC == 12111
        return super.keyReleased(keyInput);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        return super.keyReleased(keyCode, 0, 0);
//#elseif MC == 12101 || MC == 12104 || MC == 12108
//$$        return super.keyReleased(keyCode, scanCode, modifiers);
//#endif
    }

    @Override
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public void render(DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
//#elseif MC == 11701 || MC == 11904
//$$    public void render(MatrixStack matrices, int mouseX, int mouseY, float partialTicks) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        this.renderBackground(drawContext, mouseX, mouseY, partialTicks);
//#elseif MC == 11701 || MC == 11904
//$$        this.renderBackground(matrices);
//#elseif MC == 12001
//$$        this.renderBackground(drawContext);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        super.render(drawContext, mouseX, mouseY, partialTicks);
//#elseif MC == 11701 || MC == 11904
//$$        super.render(matrices, mouseX, mouseY, partialTicks);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 绘制标签
//#elseif MC == 11701 || MC == 11904
//$$        // 缁樺埗鏍囩
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawText(this.textRenderer, this.labelText, this.labelX, this.labelY, 0xFFFFFFFF, false);
//#elseif MC == 11701 || MC == 11904
//$$        this.textRenderer.draw(matrices, this.labelText, this.labelX, this.labelY, 0xFFFFFFFF);
//#endif

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 录制快捷键时，实时显示当前按下的按键
//#elseif MC == 11701 || MC == 11904
//$$        // 褰曞埗蹇嵎閿椂锛屽疄鏃舵樉绀哄綋鍓嶆寜涓嬬殑鎸夐敭
//#endif
        if (this.listening && !this.heldKeys.isEmpty()) {
            String realtimeKeys = getRealtimeKeyDisplay();
            int textW = this.textRenderer.getWidth(realtimeKeys);
            int textX = this.setBtnX + (this.setBtnW - textW) / 2;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawText(this.textRenderer, realtimeKeys, textX, this.setBtnY + 5, 0xFFFFFF00, false);
//#elseif MC == 11701 || MC == 11904
//$$            this.textRenderer.draw(matrices, realtimeKeys, textX, this.setBtnY + 5, 0xFFFFFF00);
//#endif
        }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 快捷键按钮悬停提示
//#endif
        if (mouseX >= this.setBtnX && mouseX <= this.setBtnX + this.setBtnW &&
                mouseY >= this.setBtnY && mouseY <= this.setBtnY + this.setBtnH) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawTooltip(drawContext, I18n.tr("litematlist.tooltip.hotkey_conflict"), mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$            drawTooltip(matrices, I18n.tr("litematlist.tooltip.hotkey_conflict"), mouseX, mouseY);
//#endif
        }
    }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    /** 获取当前实时按下的按键显示字符串 */
//#elseif MC == 11701 || MC == 11904
//$$    /** 鑾峰彇褰撳墠瀹炴椂鎸変笅鐨勬寜閿樉绀哄瓧绗︿覆 */
//#endif
    private String getRealtimeKeyDisplay() {
        try {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            ConfigHotkey tmp = new ConfigHotkey("entry_hotkey", "", KeybindSettings.RELEASE_EXCLUSIVE);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            ConfigHotkey tmp = new ConfigHotkey("entry_hotkey", "", KeybindSettings.RELEASE_EXCLUSIVE, "");
//#endif
            for (int key : this.pressedKeys) {
                tmp.getKeybind().addKey(key);
            }
            String s = tmp.getKeybind().getKeysDisplayString();
            return (s == null || s.isEmpty()) ? "..." : s;
        } catch (Exception e) {
            return "...";
        }
    }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void drawTooltip(DrawContext drawContext, String tip, int mouseX, int mouseY) {
//#elseif MC == 11701 || MC == 11904
//$$    private void drawTooltip(MatrixStack matrices, String tip, int mouseX, int mouseY) {
//#endif
        int w = this.textRenderer.getWidth(tip) + 8;
        int x = mouseX + 8;
        int y = mouseY + 12;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(x, y, x + w, y + 14, 0xF0000000);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, x, y, x + w, y + 14, 0xF0000000);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawText(this.textRenderer, tip, x + 4, y + 3, 0xFFFFFFFF, false);
//#elseif MC == 11701 || MC == 11904
//$$        this.textRenderer.draw(matrices, tip, x + 4, y + 3, 0xFFFFFFFF);
//#endif
    }

//#if MC == 12104 || MC == 12110 || MC == 12111
    /**
     * 注入 PlayerControl++ 配方树数据：使用配方树分析器生成配方树，
     * 注入到 MaterialDataStore.playerControlPlusMaterialList 字段供 PlayerControl++ 读取。
     *
     * @return true 注入成功；false 材料加载失败（已回滚上传状态）
     */
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$/**
//$$     * 注入 PlayerControl++ 配方树数据：使用配方树分析器生成配方树，
//$$     * 注入到 MaterialDataStore.playerControlPlusMaterialList 字段供 PlayerControl++ 读取。
//$$     *
//$$     * @return true 注入成功；false 材料加载失败（已回滚上传状态）
//$$     */
//#elseif MC == 12101 || MC == 12108
//$$    /**
//$$     * 注入 PlayerControl++ 配方树数据：使用配方树分析器生成配方树，
//$$     * 注入到 MaterialDetailScreen.playerControlPlusMaterialList 字段供 PlayerControl++ 读取。
//$$     *
//$$     * @return true 注入成功；false 材料加载失败（已回滚上传状态）
//$$     */
//#endif
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
        Map<net.minecraft.item.Item, Integer> totals = new java.util.LinkedHashMap<>();
        Map<net.minecraft.item.Item, String> names = new java.util.LinkedHashMap<>();
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
                net.minecraft.item.Item item = m.item();
                net.minecraft.item.Item replaced = MaterialDataStore.getReplacement(child.filePath(), item);
                if (replaced != item) item = replaced;
                if (MaterialDataStore.getIgnoredForPath(child.filePath()).contains(item)) continue;
                totals.merge(item, m.totalCount(), Integer::sum);
                names.putIfAbsent(item, m.blockName());
            }
        }
        List<MaterialEntry> merged = new java.util.ArrayList<>();
        for (Map.Entry<net.minecraft.item.Item, Integer> e : totals.entrySet()) {
            merged.add(new MaterialEntry(e.getKey(), names.getOrDefault(e.getKey(), ""), e.getValue()));
        }
        return merged;
    }
}
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108
//$$
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108
//$$
//#endif