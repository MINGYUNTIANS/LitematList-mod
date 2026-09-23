package com.litematlist.gui;

import java.util.ArrayList;
import java.util.List;
import com.litematlist.I18n;
import com.litematlist.LitematListMod;
import com.litematlist.PinyinSearch;
import com.litematlist.config.ConfigGuiTab;
import com.litematlist.config.Configs;
import com.litematlist.config.Hotkeys;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.IKeybindConfigGui;
import fi.dy.masa.malilib.gui.widgets.WidgetConfigOption;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptionsBase;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class GuiConfigs extends GuiConfigsBase
{
    private static ConfigGuiTab currentTab = ConfigGuiTab.GENERIC;

    public GuiConfigs()
    {
        super(10, 50, LitematListMod.MOD_ID, null, "litematlist.gui.title.configs");
    }

    @Override
    public void initGui()
    {
        super.initGui();
        this.clearOptions();

        // 修复首次打开配置菜单时 modSwitchWidget 未显示的问题
        if (this.modSwitchWidget == null) {
            this.buildConfigSwitcher();
        }

        int x = 10;
        int y = 26;

        for (ConfigGuiTab tab : ConfigGuiTab.values())
        {
            x += this.createTabButton(x, y, tab);
        }

        // 右下角「回到主菜单」按钮
        int backBtnY = this.height - 38;
        ButtonGeneric backBtn = new ButtonGeneric(this.width - 130, backBtnY, 120, 20, I18n.tr("litematlist.button.back_to_main"));
        backBtn.setRenderDefaultBackground(true);
        this.addButton(backBtn, (IButtonActionListener) (b, mb) ->
                MinecraftClient.getInstance().setScreen(new MaterialListScreen()));
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(drawContext, mouseX, mouseY, partialTicks);
        super.render(drawContext, mouseX, mouseY, partialTicks);

        // 作者署名 —— 放在标题 "LitematList 配置" 后面
        String title = net.minecraft.text.Text.translatable("litematlist.gui.title.configs").getString();
        int titleWidth = this.textRenderer.getWidth(title);
        int titleX = 10; // GuiConfigsBase 标题从 x=10 开始
        int titleY = 10;
        String sigText = "by  bilibili  命运天S";
        int sigX = titleX + titleWidth + 8;
        int sigW = this.textRenderer.getWidth(sigText);
        drawContext.drawText(this.textRenderer, sigText, sigX, titleY, 0xFF55FFFF, false);

        // 作者署名悬停提示
        if (mouseX >= sigX && mouseX <= sigX + sigW && mouseY >= titleY && mouseY <= titleY + 10) {
            drawContext.drawTooltip(this.textRenderer,
                    net.minecraft.text.Text.literal(I18n.tr("litematlist.tooltip.author_signature")), mouseX, mouseY);
        }

        // 当 TweakerMore 未安装时，将「显示上传区域按钮」的标签标红并显示悬停提示
        if (!LitematListMod.isTweakermoreInstalled()) {
            drawTweakermoreWarning(drawContext, mouseX, mouseY);
        }
    }

    private void drawTweakermoreWarning(DrawContext drawContext, int mouseX, int mouseY) {
        // 在配置列表上方显示红色警告
        int listX = 10;
        String warning = I18n.tr("litematlist.tooltip.no_tweakermore");
        drawContext.drawText(this.textRenderer, warning, listX, 50, 0xFFFF5555, false);
        // 悬停时显示详细提示
        if (mouseX >= listX && mouseX <= listX + this.textRenderer.getWidth(warning)
                && mouseY >= 50 && mouseY <= 50 + 10) {
            drawContext.drawTooltip(this.textRenderer,
                    net.minecraft.text.Text.literal(I18n.tr("litematlist.tooltip.no_tweakermore")), mouseX, mouseY);
        }
    }

    private int createTabButton(int x, int y, ConfigGuiTab tab)
    {
        ButtonGeneric button = new ButtonGeneric(x, y, -1, 20, tab.getDisplayName());
        button.setEnabled(currentTab != tab);
        this.addButton(button, new TabButtonListener(tab, this));

        return button.getWidth() + 2;
    }

    @Override
    protected int getConfigWidth()
    {
        return currentTab == ConfigGuiTab.HOTKEYS || currentTab == ConfigGuiTab.TOGGLES ? 200 : 140;
    }

    /**
     * 使用自定义列表部件：让「是否计算建筑材料」开关的名称文字渲染为橙色（#F17E18），
     * 其余配置行保持默认样式。
     */
    @Override
    protected WidgetListConfigOptions createListWidget(int listX, int listY)
    {
        return new ToggleColoredListWidget(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(),
                this.getConfigWidth(), 0f, this.useKeybindSearch(), this);
    }

    @Override
    protected boolean useKeybindSearch()
    {
        return currentTab == ConfigGuiTab.HOTKEYS || currentTab == ConfigGuiTab.TOGGLES;
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs()
    {
        List<? extends IConfigBase> configs = switch (currentTab)
        {
            case GENERIC -> Configs.Generic.OPTIONS;
            case TOGGLES -> Configs.FeatureToggles.TOGGLE_LIST;
            case STATS -> Configs.Statistics.OPTIONS;
            case HOTKEYS -> Hotkeys.HOTKEY_LIST;
        };
        return ConfigOptionWrapper.createFor(configs);
    }

    private record TabButtonListener(ConfigGuiTab tab, GuiConfigs gui) implements IButtonActionListener
    {
        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            currentTab = this.tab;
            GuiConfigs newGui = new GuiConfigs();
            newGui.setParent(this.gui.getParent());
            fi.dy.masa.malilib.gui.GuiBase.openGui(newGui);
        }
    }

    /**
     * 配置列表部件：仅为「是否计算建筑材料」行创建橙色名称标签的部件，其余行保持默认样式。
     */
    private static class ToggleColoredListWidget extends WidgetListConfigOptions
    {
        public ToggleColoredListWidget(int x, int y, int width, int height, int configWidth,
                float zLevel, boolean useKeybindSearch, GuiConfigsBase parent)
        {
            super(x, y, width, height, configWidth, zLevel, useKeybindSearch, parent);
        }

        @Override
        protected WidgetConfigOption createListEntryWidget(int x, int y, int listIndex, boolean isOdd,
                ConfigOptionWrapper wrapper)
        {
            if (wrapper.getConfig() == Configs.FeatureToggles.CALC_BUILDING_MATERIALS)
            {
                return new OrangeNameConfigOption(x, y, this.browserEntryWidth, this.browserEntryHeight,
                        this.maxLabelWidth, this.configWidth, wrapper, listIndex, this.parent, this);
            }
            return super.createListEntryWidget(x, y, listIndex, isOdd, wrapper);
        }

        @Override
        protected List<String> getEntryStringsForFilter(ConfigOptionWrapper wrapper)
        {
            // 在 maLib 默认候选（内部名/显示名）基础上，
            // 追加显示名与注释的小写文本及拼音全拼/首拼，支持拼音全拼、首拼与模糊搜索
            List<String> strings = new ArrayList<>(super.getEntryStringsForFilter(wrapper));
            IConfigBase config = wrapper.getConfig();
            if (config != null)
            {
                addPinyinFilterStrings(strings, config.getConfigGuiDisplayName());
                addPinyinFilterStrings(strings, config.getComment());
            }
            return strings;
        }

        /** 把文本的小写形式及其拼音全拼/首拼加入过滤候选列表 */
        private static void addPinyinFilterStrings(List<String> strings, String text)
        {
            if (text == null || text.isEmpty())
            {
                return;
            }
            String lower = text.toLowerCase(java.util.Locale.ROOT);
            if (!strings.contains(lower))
            {
                strings.add(lower);
            }
            for (String s : PinyinSearch.fullAndInitials(lower))
            {
                if (!s.isEmpty() && !strings.contains(s))
                {
                    strings.add(s);
                }
            }
        }
    }

    /**
     * 配置行部件：把「是否计算建筑材料」的名称标签渲染为橙色（#F17E18）。
     */
    private static class OrangeNameConfigOption extends WidgetConfigOption
    {
        public OrangeNameConfigOption(int x, int y, int width, int height, int labelWidth, int configWidth,
                ConfigOptionWrapper wrapper, int listIndex, IKeybindConfigGui host,
                WidgetListConfigOptionsBase<?, ?> parent)
        {
            super(x, y, width, height, labelWidth, configWidth, wrapper, listIndex, host, parent);
        }

        @Override
        protected void addLabel(int x, int y, int width, int height, int textColor, String... lines)
        {
            // 名称文字固定使用 #F17E18
            super.addLabel(x, y, width, height, 0xFFF17E18, lines);
        }
    }
}