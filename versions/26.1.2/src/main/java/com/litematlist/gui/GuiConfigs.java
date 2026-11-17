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
        String sigText = "by  bilibili  命运天S";
        int sigX = titleX + titleWidth + 8;
        int sigW = this.font.width(sigText);
        ctx.drawString(
                this.font, sigText, sigX, titleY, 0xFF55FFFF);

        // 作者署名悬停提示
        if (mouseX >= sigX && mouseX <= sigX + sigW && mouseY >= titleY && mouseY <= titleY + 10) {
            ctx.getGuiGraphics().setTooltipForNextFrame(this.font,
                    Component.literal(I18n.tr("litematlist.tooltip.author_signature")), mouseX, mouseY);
        }

        if (!LitematListMod.isTweakermoreInstalled()) { drawTweakermoreWarning(ctx, mouseX, mouseY); }
    }

    private void drawTweakermoreWarning(GuiContext ctx, int mouseX, int mouseY) {
        int listX = 10;
        String warning = I18n.tr("litematlist.tooltip.no_tweakermore");
        ctx.drawString(this.font, warning, listX, 50, 0xFFFF5555);
        // 悬停时显示详细提示
        if (mouseX >= listX && mouseX <= listX + this.font.width(warning)
                && mouseY >= 50 && mouseY <= 50 + 10) {
            ctx.getGuiGraphics().setTooltipForNextFrame(this.font,
                    Component.literal(I18n.tr("litematlist.tooltip.no_tweakermore")), mouseX, mouseY);
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
        if (currentTab == ConfigGuiTab.COLORS)
        {
            return 100;
        }
        return currentTab == ConfigGuiTab.HOTKEYS || currentTab == ConfigGuiTab.TOGGLES ? 200 : 140;
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
            case COLORS -> Configs.Colors.OPTIONS;
            case HOTKEYS -> Hotkeys.HOTKEY_LIST;
        };
        return ConfigOptionWrapper.createFor(configs);
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
     * 配置列表部件：为特定配置行的名称标签使用自定义颜色
     * （「是否计算建筑材料」橙色；统计页的雪傀儡/铁傀儡/凋零/铜傀儡浅蓝色），
     * 其余行保持默认样式。
     */
    private static class ToggleColoredListWidget extends WidgetListConfigOptions
    {
        /** maLib 配置行标签的默认颜色（白色），返回该值表示不做特殊着色 */
        private static final int DEFAULT_LABEL_COLOR = 0xFFFFFFFF;

        public ToggleColoredListWidget(int x, int y, int width, int height, int configWidth,
                float zLevel, boolean useKeybindSearch, GuiConfigsBase parent)
        {
            super(x, y, width, height, configWidth, zLevel, useKeybindSearch, parent);
        }

        @Override
        protected WidgetConfigOption createListEntryWidget(int x, int y, int listIndex, boolean isOdd,
                ConfigOptionWrapper wrapper)
        {
            if (labelColorFor(wrapper.getConfig()) != DEFAULT_LABEL_COLOR)
            {
                return new ColoredNameConfigOption(x, y, this.browserEntryWidth, this.browserEntryHeight,
                        this.maxLabelWidth, this.configWidth, wrapper, listIndex, this.parent, this);
            }
            return super.createListEntryWidget(x, y, listIndex, isOdd, wrapper);
        }

        /** 需要着色的配置项标签颜色；返回 DEFAULT_LABEL_COLOR 表示保持 maLib 默认样式 */
        private static int labelColorFor(IConfigBase config)
        {
            // 「是否计算建筑材料」：橙色 #F17E18
            if (config == Configs.FeatureToggles.CALC_BUILDING_MATERIALS)
            {
                return 0xFFF17E18;
            }
            // 统计页「雪傀儡 / 铁傀儡 / 凋零 / 铜傀儡」：浅蓝色 #5CA8E9
            if (config == Configs.Statistics.SNOW_GOLEM
                    || config == Configs.Statistics.IRON_GOLEM
                    || config == Configs.Statistics.WITHER
                    || config == Configs.Statistics.COPPER_GOLEM)
            {
                return 0xFF5CA8E9;
            }
            return DEFAULT_LABEL_COLOR;
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
     * 配置行部件：把名称标签渲染为对应配置指定的颜色。
     *
     * 注意（踩坑）：maLib 的 {@code WidgetConfigOption} 构造器会在「子类字段赋值之前」
     * 就调用 {@code addLabel()} 添加标签，因此颜色不能存子类实例字段——那时它还是默认值 0，
     * 会把文字画成全透明，表现为「彩色文字未渲染」。这里改为在此时已赋值的
     * 父类字段 {@code wrapper} 上实时推导颜色。
     */
    private static class ColoredNameConfigOption extends WidgetConfigOption
    {
        public ColoredNameConfigOption(int x, int y, int width, int height, int labelWidth, int configWidth,
                ConfigOptionWrapper wrapper, int listIndex, IKeybindConfigGui host,
                WidgetListConfigOptionsBase<?, ?> parent)
        {
            super(x, y, width, height, labelWidth, configWidth, wrapper, listIndex, host, parent);
        }

        @Override
        protected void addLabel(int x, int y, int width, int height, int textColor, String... lines)
        {
            super.addLabel(x, y, width, height,
                    ToggleColoredListWidget.labelColorFor(this.wrapper.getConfig()), lines);
        }
    }
}