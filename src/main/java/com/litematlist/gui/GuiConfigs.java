package com.litematlist.gui;
//#if MC == 11701
//$$import net.minecraft.text.TranslatableText;
//#endif
//#if MC == 11701
//$$import net.minecraft.text.LiteralText;
//#endif

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
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.gui.DrawContext;
//#elseif MC == 11701 || MC == 11904
//$$import net.minecraft.client.util.math.MatrixStack;
//#endif

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

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        // 修复首次打开配置菜单时 modSwitchWidget 未显示的问题
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        if (this.modSwitchWidget == null) {
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            this.buildConfigSwitcher();
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        }
//#endif
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111

//#endif
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

        // 作者署名 —— 放在标题 "LitematList 配置" 后面
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        String title = net.minecraft.text.Text.translatable("litematlist.gui.title.configs").getString();
//#elseif MC == 11701
//$$        String title = new TranslatableText("litematlist.gui.title.configs").getString();
//#endif
        int titleWidth = this.textRenderer.getWidth(title);
        int titleX = 10; // GuiConfigsBase 标题从 x=10 开始
        int titleY = 10;
        String sigText = "by  bilibili  命运天S";
        int sigX = titleX + titleWidth + 8;
        int sigW = this.textRenderer.getWidth(sigText);
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawText(this.textRenderer, sigText, sigX, titleY, 0xFF55FFFF, false);
//#elseif MC == 11701 || MC == 11904
//$$        this.textRenderer.draw(matrices, sigText, sigX, titleY, 0xFF55FFFF);
//#endif

        // 作者署名悬停提示
        if (mouseX >= sigX && mouseX <= sigX + sigW && mouseY >= titleY && mouseY <= titleY + 10) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTooltip(this.textRenderer,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    net.minecraft.text.Text.literal(I18n.tr("litematlist.tooltip.author_signature")), mouseX, mouseY);
//#endif
//#if MC == 11701
//$$            this.renderTooltip(matrices, new LiteralText(I18n.tr("litematlist.tooltip.author_signature")), mouseX, mouseY);
//#elseif MC == 11904
//$$            this.renderTooltip(matrices, net.minecraft.text.Text.literal(I18n.tr("litematlist.tooltip.author_signature")), mouseX, mouseY);
//#endif
        }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110
        // 当 TweakerMore 未安装时，在配置列表上方显示红色警告并支持悬停提示
//#elseif MC == 11701 || MC == 11904 || MC == 12111
//$$        // 当 TweakerMore 未安装时，将「显示上传区域按钮」的标签标红并显示悬停提示
//#endif
        if (!LitematListMod.isTweakermoreInstalled()) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawTweakermoreWarning(drawContext, mouseX, mouseY);
//#elseif MC == 11701 || MC == 11904
//$$            drawTweakermoreWarning(matrices, mouseX, mouseY);
//#endif
        }
    }

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    private void drawTweakermoreWarning(DrawContext drawContext, int mouseX, int mouseY) {
//#elseif MC == 11701 || MC == 11904
//$$    private void drawTweakermoreWarning(MatrixStack matrices, int mouseX, int mouseY) {
//#endif
        // 在配置列表上方显示红色警告
        int listX = 10;
        String warning = I18n.tr("litematlist.tooltip.no_tweakermore");
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.drawText(this.textRenderer, warning, listX, 50, 0xFFFF5555, false);
//#elseif MC == 11701 || MC == 11904
//$$        this.textRenderer.draw(matrices, warning, listX, 50, 0xFFFF5555);
//#endif
        // 悬停时显示详细提示
        if (mouseX >= listX && mouseX <= listX + this.textRenderer.getWidth(warning)
                && mouseY >= 50 && mouseY <= 50 + 10) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawTooltip(this.textRenderer,
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    net.minecraft.text.Text.literal(I18n.tr("litematlist.tooltip.no_tweakermore")), mouseX, mouseY);
//#endif
//#if MC == 11701
//$$            this.renderTooltip(matrices, new LiteralText(I18n.tr("litematlist.tooltip.no_tweakermore")), mouseX, mouseY);
//#elseif MC == 11904
//$$            this.renderTooltip(matrices, net.minecraft.text.Text.literal(I18n.tr("litematlist.tooltip.no_tweakermore")), mouseX, mouseY);
//#endif
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

//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110
    /**
     * 使用自定义列表部件：让「是否计算建筑材料」开关的名称文字渲染为橙色（#F17E18），
     * 其余配置行保持默认样式。
     */
//#elseif MC == 12111
//$$    /**
//$$     * 使用自定义列表部件：在 maLib 默认过滤候选上追加显示名/注释的小写文本与拼音全拼/首拼，
//$$     * 使配置搜索支持中文、拼音全拼、首拼与模糊匹配。
//$$     */
//#endif
    @Override
    protected WidgetListConfigOptions createListWidget(int listX, int listY)
    {
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110
        return new ToggleColoredListWidget(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(),
//#elseif MC == 12111
//$$        return new PinyinSearchListWidget(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(),
//#endif
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
            case COLORS -> Configs.Colors.OPTIONS;
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

//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110
    /**
     * 配置列表部件：为特定配置行的名称标签使用自定义颜色
     * （「是否计算建筑材料」橙色；统计页的雪傀儡/铁傀儡/凋零/铜傀儡浅蓝色），
     * 其余行保持默认样式。
     */
//#elseif MC == 12111
//$$    /**
//$$     * 配置列表部件：在 maLib 默认过滤候选（内部名/显示名）基础上，
//$$     * 追加显示名与注释的小写文本及拼音全拼/首拼，支持拼音全拼、首拼与模糊搜索。
//$$     */
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110
    private static class ToggleColoredListWidget extends WidgetListConfigOptions
//#elseif MC == 12111
//$$    private static class PinyinSearchListWidget extends WidgetListConfigOptions
//#endif
    {
        /** maLib 配置行标签的默认颜色（白色），返回该值表示不做特殊着色 */
        private static final int DEFAULT_LABEL_COLOR = 0xFFFFFFFF;

//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110
        public ToggleColoredListWidget(int x, int y, int width, int height, int configWidth,
//#elseif MC == 12111
//$$        public PinyinSearchListWidget(int x, int y, int width, int height, int configWidth,
//#endif
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
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110
            // 在 maLib 默认候选（内部名/显示名）基础上，
//#endif
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110
            // 追加显示名与注释的小写文本及拼音全拼/首拼，支持拼音全拼、首拼与模糊搜索
//#endif
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
//#if MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110
                    ToggleColoredListWidget.labelColorFor(this.wrapper.getConfig()), lines);
//#elseif MC == 12111
//$$                    PinyinSearchListWidget.labelColorFor(this.wrapper.getConfig()), lines);
//#endif
        }
    }
}