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
import fi.dy.masa.malilib.gui.widgets.WidgetConfigOption;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptionsBase;
import fi.dy.masa.malilib.gui.interfaces.IKeybindConfigGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import fi.dy.masa.malilib.render.GuiContext;

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
                Minecraft.getInstance().setScreenAndShow(new MaterialListScreen()));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks) {
        GuiContext ctx = GuiContext.fromGuiGraphics(gfx);
        this.extractBackground(gfx, mouseX, mouseY, partialTicks);
        super.extractRenderState(gfx, mouseX, mouseY, partialTicks);

        // 作者署名 —— 放在标题 "LitematList 配置" 后面
        String title = Component.translatable("litematlist.gui.title.configs").getString();
        int titleWidth = this.font.width(title);
        int titleX = 10; // GuiConfigsBase 标题从 x=10 开始
        int titleY = 10;
        int sigX = titleX + titleWidth + 8;
        ctx.drawString(
                this.font, "by  bilibili  命运天S", sigX, titleY, 0xFF55FFFF);

        if (!LitematListMod.isTweakermoreInstalled()) { drawTweakermoreWarning(ctx, mouseX, mouseY); }
    }

    private void drawTweakermoreWarning(GuiContext ctx, int mouseX, int mouseY) {
        int listX = 10;
        String warning = I18n.tr("litematlist.tooltip.no_tweakermore");
        ctx.drawString(this.font, warning, listX, 50, 0xFFFF5555);
        if (mouseX >= listX && mouseX <= listX + this.font.width(warning)
                && mouseY >= 50 && mouseY <= 50 + 10) {
            ctx.drawString(this.font, I18n.tr("litematlist.tooltip.no_tweakermore_desc"), mouseX, mouseY + 12, 0xFFFFFFFF);
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

    @Override
    protected boolean useKeybindSearch()
    {
        return currentTab == ConfigGuiTab.HOTKEYS || currentTab == ConfigGuiTab.TOGGLES;
    }

    /**
     * 使用自定义列表部件：在 maLib 默认过滤候选（内部名/显示名）基础上，
     * 追加显示名与注释的小写文本及拼音全拼/首拼，支持拼音全拼、首拼与模糊搜索。
     */
    @Override
    protected WidgetListConfigOptions createListWidget(int listX, int listY)
    {
        return new PinyinConfigListWidget(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(),
                this.getConfigWidth(), 0f, this.useKeybindSearch(), this);
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
     * 配置列表部件：在 maLib 默认候选（内部名/显示名）基础上，
     * 追加显示名与注释的小写文本及拼音全拼/首拼，支持拼音全拼、首拼与模糊搜索。
     */
    private static class PinyinConfigListWidget extends WidgetListConfigOptions
    {
        public PinyinConfigListWidget(int x, int y, int width, int height, int configWidth,
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