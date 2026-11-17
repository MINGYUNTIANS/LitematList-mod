package com.litematlist.gui;
//#if MC == 11701
//$$import net.minecraft.text.LiteralText;
//#endif

import com.litematlist.I18n;
import com.litematlist.LitematListMod;
import com.litematlist.gui.MaterialDataStore.RawMaterialEntry;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.MinecraftClient;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.client.gui.DrawContext;
//#endif
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
import net.minecraft.registry.Registries;
//#elseif MC == 11701
//$$import net.minecraft.util.registry.Registry;
//#endif
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.gui.DrawableHelper;
//#endif
//#if MC == 11701 || MC == 11904
//$$import net.minecraft.client.util.math.MatrixStack;
//#endif

/**
 * 冗余列表界面：显示原材料溯源过程中多出来的物品。
 */
public class RedundancyListScreen extends GuiBase {

    private final GuiBase parent;
    private final String schematicName;
    private final Path filePath;
    private final String pathKey;
    private List<RawMaterialEntry> redundancyItems = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int ROW_HEIGHT = 22;
    private static final int LIST_TOP = 50;
    private int visibleRows;

    public RedundancyListScreen(GuiBase parent, String schematicName, Path filePath) {
        super();
        this.parent = parent;
        this.schematicName = schematicName;
        this.filePath = filePath;
        this.pathKey = filePath != null ? filePath.toString() : schematicName;
        this.title = I18n.tr("litematlist.title.redundancy_list", schematicName);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.visibleRows = (this.height - 100) / ROW_HEIGHT;

        List<RawMaterialEntry> stored = MaterialDataStore.rawMaterialRedundancy.get(pathKey);
        this.redundancyItems = stored != null ? stored : new ArrayList<>();

        // 导出按钮（上方）
        int exportY = 18;
        ButtonGeneric exportTxtBtn = new ButtonGeneric(20, exportY, 70, 20, I18n.tr("litematlist.button.export_txt"));
        exportTxtBtn.setRenderDefaultBackground(true);
        this.addButton(exportTxtBtn, (IButtonActionListener) (b, mb) -> exportRedundancy("txt"));

        ButtonGeneric exportJsonBtn = new ButtonGeneric(94, exportY, 70, 20, I18n.tr("litematlist.button.export_json"));
        exportJsonBtn.setRenderDefaultBackground(true);
        this.addButton(exportJsonBtn, (IButtonActionListener) (b, mb) -> exportRedundancy("json"));

        ButtonGeneric exportCsvBtn = new ButtonGeneric(168, exportY, 70, 20, I18n.tr("litematlist.button.export_csv"));
        exportCsvBtn.setRenderDefaultBackground(true);
        this.addButton(exportCsvBtn, (IButtonActionListener) (b, mb) -> exportRedundancy("csv"));

        int buttonY = this.height - 38;
        ButtonGeneric backBtn = new ButtonGeneric(this.width / 2 - 40, buttonY, 80, 20, I18n.tr("litematlist.button.back"));
        backBtn.setRenderDefaultBackground(true);
        this.addButton(backBtn, (IButtonActionListener) (b, mb) -> MinecraftClient.getInstance().setScreen(parent));
    }

    private void exportRedundancy(String format) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date());
        String filename = "redundancy_" + schematicName.replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "_") + "_" + timestamp + "." + format;
        Path exportPath = MinecraftClient.getInstance().runDirectory.toPath().resolve("litematlist").resolve("project").resolve("rawmaterials").resolve(filename);
        try {
            java.nio.file.Files.createDirectories(exportPath.getParent());
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(exportPath.toFile()), StandardCharsets.UTF_8))) {
                switch (format) {
                    case "txt" -> {
                        for (RawMaterialEntry entry : redundancyItems) {
                            writer.write(entry.itemId() + " " + entry.count());
                            writer.newLine();
                        }
                    }
                    case "json" -> {
                        writer.write("[");
                        boolean first = true;
                        for (RawMaterialEntry entry : redundancyItems) {
                            if (!first) writer.write(",");
                            first = false;
                            writer.newLine();
                            writer.write("  {\"Item\":\"" + entry.itemId() + "\",\"Count\":" + entry.count() + "}");
                        }
                        writer.newLine();
                        writer.write("]");
                    }
                    case "csv" -> {
                        writer.write("Item,Count");
                        writer.newLine();
                        for (RawMaterialEntry entry : redundancyItems) {
                            writer.write(entry.itemId() + "," + entry.count());
                            writer.newLine();
                        }
                    }
                }
            }
            LitematListMod.LOGGER.info("冗余列表已导出: {}", exportPath);
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                String fmt = format.toUpperCase();
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                Text link = Text.literal(filename)
//#elseif MC == 11701
//$$                Text link = new LiteralText(filename)
//#endif
                        .styled(style -> style
//#if MC == 12108 || MC == 12110 || MC == 12111
                                .withClickEvent(new net.minecraft.text.ClickEvent.OpenFile(exportPath.toString()))
//#elseif MC == 11701 || MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104
//$$                                .withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.OPEN_FILE, exportPath.toString()))
//#endif
                                .withUnderline(true)
                                .withColor(0x55FFFF));
//#if MC == 11904 || MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                Text msg = Text.literal("§a冗余列表已导出为" + fmt + ": ").append(link);
//#elseif MC == 11701
//$$                Text msg = new LiteralText("§a冗余列表已导出为" + fmt + ": ").append(link);
//#endif
                client.player.sendMessage(msg, false);
            }
        } catch (Exception e) {
            LitematListMod.LOGGER.error("导出冗余列表失败", e);
        }
    }

    @Override
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
//#elseif MC == 11701 || MC == 11904
//$$    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        super.render(drawContext, mouseX, mouseY, delta);
//#elseif MC == 11701 || MC == 11904
//$$        super.render(matrices, mouseX, mouseY, delta);
//#endif

        int listRight = this.width - 8;
        int maxRenderY = this.height - 60;

        if (redundancyItems.isEmpty()) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.drawCenteredTextWithShadow(this.textRenderer, I18n.tr("litematlist.redundancy.empty"),
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                    this.width / 2, (this.height - 100) / 2, 0x80FFFFFF);
//#endif
//#if MC == 11701 || MC == 11904
//$$            this.textRenderer.drawWithShadow(matrices, I18n.tr("litematlist.redundancy.empty"), this.width / 2 - this.textRenderer.getWidth(I18n.tr("litematlist.redundancy.empty")) / 2, (this.height - 100) / 2, 0x80FFFFFF);
//#endif
            return;
        }

        int maxIdx = Math.min(redundancyItems.size(), scrollOffset + visibleRows);
        int cumulativeY = LIST_TOP;
        int listBottom = LIST_TOP;

        for (int i = scrollOffset; i < maxIdx; i++) {
            if (cumulativeY + ROW_HEIGHT > maxRenderY) break;
            cumulativeY += ROW_HEIGHT;
            listBottom = cumulativeY;
        }
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        drawContext.fill(8, LIST_TOP - 2, listRight, listBottom + 2, 0x40000000);
//#elseif MC == 11701 || MC == 11904
//$$        DrawableHelper.fill(matrices, 8, LIST_TOP - 2, listRight, listBottom + 2, 0x40000000);
//#endif

        cumulativeY = LIST_TOP;
        for (int i = scrollOffset; i < maxIdx; i++) {
            RawMaterialEntry entry = redundancyItems.get(i);
            int rowY = cumulativeY;
            if (rowY + ROW_HEIGHT > maxRenderY) break;

//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(10, rowY, listRight, rowY + ROW_HEIGHT, (i % 2 == 0) ? 0x10FFFFFF : 0x00FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, 10, rowY, listRight, rowY + ROW_HEIGHT, (i % 2 == 0) ? 0x10FFFFFF : 0x00FFFFFF);
//#endif

//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            Item item = Registries.ITEM.get(Identifier.of(entry.itemId()));
//#elseif MC == 11701
//$$            Item item = Registry.ITEM.get(Identifier.tryParse(entry.itemId()));
//#elseif MC == 11904 || MC == 12001
//$$            Item item = Registries.ITEM.get(Identifier.tryParse(entry.itemId()));
//#endif
            if (item != null) {
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.drawItem(new ItemStack(item), 30, rowY + 2);
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.drawTextWithShadow(this.textRenderer, item.getName().getString() + " ×" + entry.count(),
//#endif
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                        50, rowY + 5, 0xFFFFFFFF);
//#endif
//#if MC == 11701
//$$                MinecraftClient.getInstance().getItemRenderer().renderInGui(new ItemStack(item), 30, rowY + 2);
//#elseif MC == 11904
//$$                MinecraftClient.getInstance().getItemRenderer().renderInGui(matrices, new ItemStack(item), 30, rowY + 2);
//#endif
//#if MC == 11701 || MC == 11904
//$$                this.textRenderer.drawWithShadow(matrices, item.getName().getString() + " ×" + entry.count(), 50, rowY + 5, 0xFFFFFFFF);
//#endif
            }

            // 来源描述
            if (!entry.sources().isEmpty()) {
                String srcText = String.join(" | ", entry.sources());
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
                drawContext.drawTextWithShadow(this.textRenderer, srcText, 200, rowY + 5, 0x80FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$                this.textRenderer.drawWithShadow(matrices, srcText, 200, rowY + 5, 0x80FFFFFF);
//#endif
            }

            cumulativeY += ROW_HEIGHT;
        }

        // 滚动条
        if (redundancyItems.size() > visibleRows) {
            int scrollbarX = this.width - 6;
            int trackHeight = maxRenderY - LIST_TOP;
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(scrollbarX, LIST_TOP, scrollbarX + 3, listBottom, 0x30FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, scrollbarX, LIST_TOP, scrollbarX + 3, listBottom, 0x30FFFFFF);
//#endif
            float ratio = (float) visibleRows / redundancyItems.size();
            int thumbHeight = Math.max(8, (int) (trackHeight * ratio));
            int thumbY = LIST_TOP + (int) ((trackHeight - thumbHeight) * (float) scrollOffset / Math.max(1, redundancyItems.size() - visibleRows));
//#if MC == 12001 || MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            drawContext.fill(scrollbarX, thumbY, scrollbarX + 3, thumbY + thumbHeight, 0x80FFFFFF);
//#elseif MC == 11701 || MC == 11904
//$$            DrawableHelper.fill(matrices, scrollbarX, thumbY, scrollbarX + 3, thumbY + thumbHeight, 0x80FFFFFF);
//#endif
        }
    }

    @Override
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
//#endif
        if (mouseY >= LIST_TOP && mouseY <= this.height - 60) {
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
            scrollOffset = Math.max(0, scrollOffset - (int) verticalAmount);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$            scrollOffset = Math.max(0, scrollOffset - (int) amount);
//#endif
            scrollOffset = Math.min(scrollOffset, Math.max(0, redundancyItems.size() - visibleRows));
            initGui();
            return true;
        }
//#if MC == 12101 || MC == 12104 || MC == 12108 || MC == 12110 || MC == 12111
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
//#elseif MC == 11701 || MC == 11904 || MC == 12001
//$$        return super.mouseScrolled(mouseX, mouseY, amount);
//#endif
    }
}