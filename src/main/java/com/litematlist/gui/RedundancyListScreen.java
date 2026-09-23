package com.litematlist.gui;

import com.litematlist.I18n;
import com.litematlist.LitematListMod;
import com.litematlist.gui.MaterialDetailScreen.RawMaterialEntry;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

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

        List<RawMaterialEntry> stored = MaterialDetailScreen.rawMaterialRedundancy.get(pathKey);
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
                Text link = Text.literal(filename)
                        .styled(style -> style
                                .withClickEvent(new net.minecraft.text.ClickEvent.OpenFile(exportPath.toString()))
                                .withUnderline(true)
                                .withColor(0x55FFFF));
                Text msg = Text.literal("§a冗余列表已导出为" + fmt + ": ").append(link);
                client.player.sendMessage(msg, false);
            }
        } catch (Exception e) {
            LitematListMod.LOGGER.error("导出冗余列表失败", e);
        }
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        super.render(drawContext, mouseX, mouseY, delta);

        int listRight = this.width - 8;
        int maxRenderY = this.height - 60;

        if (redundancyItems.isEmpty()) {
            drawContext.drawCenteredTextWithShadow(this.textRenderer, I18n.tr("litematlist.redundancy.empty"),
                    this.width / 2, (this.height - 100) / 2, 0x80FFFFFF);
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
        drawContext.fill(8, LIST_TOP - 2, listRight, listBottom + 2, 0x40000000);

        cumulativeY = LIST_TOP;
        for (int i = scrollOffset; i < maxIdx; i++) {
            RawMaterialEntry entry = redundancyItems.get(i);
            int rowY = cumulativeY;
            if (rowY + ROW_HEIGHT > maxRenderY) break;

            drawContext.fill(10, rowY, listRight, rowY + ROW_HEIGHT, (i % 2 == 0) ? 0x10FFFFFF : 0x00FFFFFF);

            Item item = Registries.ITEM.get(Identifier.of(entry.itemId()));
            if (item != null) {
                drawContext.drawItem(new ItemStack(item), 30, rowY + 2);
                drawContext.drawTextWithShadow(this.textRenderer, item.getName().getString() + " ×" + entry.count(),
                        50, rowY + 5, 0xFFFFFFFF);
            }

            // 来源描述
            if (!entry.sources().isEmpty()) {
                String srcText = String.join(" | ", entry.sources());
                drawContext.drawTextWithShadow(this.textRenderer, srcText, 200, rowY + 5, 0x80FFFFFF);
            }

            cumulativeY += ROW_HEIGHT;
        }

        // 滚动条
        if (redundancyItems.size() > visibleRows) {
            int scrollbarX = this.width - 6;
            int trackHeight = maxRenderY - LIST_TOP;
            drawContext.fill(scrollbarX, LIST_TOP, scrollbarX + 3, listBottom, 0x30FFFFFF);
            float ratio = (float) visibleRows / redundancyItems.size();
            int thumbHeight = Math.max(8, (int) (trackHeight * ratio));
            int thumbY = LIST_TOP + (int) ((trackHeight - thumbHeight) * (float) scrollOffset / Math.max(1, redundancyItems.size() - visibleRows));
            drawContext.fill(scrollbarX, thumbY, scrollbarX + 3, thumbY + thumbHeight, 0x80FFFFFF);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseY >= LIST_TOP && mouseY <= this.height - 60) {
            scrollOffset = Math.max(0, scrollOffset - (int) verticalAmount);
            scrollOffset = Math.min(scrollOffset, Math.max(0, redundancyItems.size() - visibleRows));
            initGui();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}