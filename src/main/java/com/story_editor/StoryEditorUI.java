package com.story_editor;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// 列表条目实体
class ListEntry {
    public String id;
    public String displayName;
    public String desc;
    public ListEntry(String id, String displayName, String desc) {
        this.id = id;
        this.displayName = displayName;
        this.desc = desc;
    }
}

public class StoryEditorUI extends Screen {
    private final ItemStack stack;
    private EditBox textBox;
    private EditBox titleBox, versionBox, startNodeBox;
    private Button saveBtn;
    private Button chapterBtn, roleBtn, settingBtn;
    private Button extraBtnMeta, extraBtnNode;

    private static final int LEFT_PANEL_WIDTH = 144;
    private static final int BTN_WIDTH = 48;
    private static final int BTN_HEIGHT = 20;
    private static final int EXTRA_BTN_WIDTH = LEFT_PANEL_WIDTH / 2;
    private static final int LIST_ITEM_HEIGHT = 26;
    private static final int LIST_PADDING = 4;

    private String currentMode = "编辑";
    private String currentSubTab = "metadata";
    private List<ListEntry> entryList = new ArrayList<>();

    private String storyTitle = "";
    private String storyVersion = "";
    private String storyDate = "";
    private String storyStartNode = "";

    private JsonObject currentStoryData;
    private File currentLoadedFile = null;  // 当前加载的文件

    private int listX, listY, listWidth, listHeight;

    public StoryEditorUI(ItemStack stack) {
        super(Component.literal(""));
        this.stack = stack;
    }

    @Override
    protected void init() {
        super.init();

        int btnY = 0;
        int startX = 0;

        // ---- 三个主按钮 ----
        this.chapterBtn = Button.builder(Component.translatable("gui.story_editor.mode_edit"), b -> {
                    currentMode = "编辑";
                    showExtraButtons(true);
                    currentSubTab = "metadata";
                    showMetadataForm();
                })
                .bounds(startX, btnY, BTN_WIDTH, BTN_HEIGHT).build();
        addRenderableWidget(chapterBtn);

        this.roleBtn = Button.builder(Component.translatable("gui.story_editor.mode_catalog"), b -> {
                    currentMode = "目录";
                    showExtraButtons(false);
                    entryList.clear();
                })
                .bounds(startX + BTN_WIDTH, btnY, BTN_WIDTH, BTN_HEIGHT).build();
        addRenderableWidget(roleBtn);

        this.settingBtn = Button.builder(Component.translatable("gui.story_editor.mode_settings"), b -> {
                    currentMode = "设置";
                    showExtraButtons(false);
                    entryList.clear();
                })
                .bounds(startX + 2 * BTN_WIDTH, btnY, BTN_WIDTH, BTN_HEIGHT).build();
        addRenderableWidget(settingBtn);

        // ---- 编辑模式下的两个子按钮 ----
        int extraY = btnY + BTN_HEIGHT;
        this.extraBtnMeta = Button.builder(Component.translatable("gui.story_editor.metadata"), b -> {
                    currentSubTab = "metadata";
                    showMetadataForm();
                })
                .bounds(startX, extraY, EXTRA_BTN_WIDTH, BTN_HEIGHT).build();
        extraBtnMeta.visible = false;
        addRenderableWidget(extraBtnMeta);

        this.extraBtnNode = Button.builder(Component.translatable("gui.story_editor.node"), b -> {
                    currentSubTab = "node";
                    loadNodeList();
                })
                .bounds(startX + EXTRA_BTN_WIDTH, extraY, EXTRA_BTN_WIDTH, BTN_HEIGHT).build();
        extraBtnNode.visible = false;
        addRenderableWidget(extraBtnNode);

        // ---- 右侧区域 ----
        int rightStartX = LEFT_PANEL_WIDTH + 8;
        int rightWidth = this.width - rightStartX - 16;

        // ---- 文件操作按钮（顶部） ----
        int openBtnX = rightStartX + 8;
        int openBtnY = 10;
        int btnW = 60;
        int btnH = 20;

        Button openBtn = Button.builder(
                Component.literal("打开"),
                b -> openFileDialog()
        ).bounds(openBtnX, openBtnY, btnW, btnH).build();
        addRenderableWidget(openBtn);

        Button saveFileBtn = Button.builder(
                Component.literal("保存"),
                b -> saveFileDialog()
        ).bounds(openBtnX + btnW + 8, openBtnY, btnW, btnH).build();
        addRenderableWidget(saveFileBtn);

        // ---- 元数据输入框 ----
        int fieldY = 50;
        int fieldHeight = 20;
        int fieldWidth = rightWidth - 16;

        this.titleBox = new EditBox(this.font, rightStartX + 8, fieldY, fieldWidth, fieldHeight, Component.literal(""));
        titleBox.setMaxLength(100);
        titleBox.visible = false;
        addRenderableWidget(titleBox);

        this.versionBox = new EditBox(this.font, rightStartX + 8, fieldY + fieldHeight + 12, fieldWidth, fieldHeight, Component.literal(""));
        versionBox.setMaxLength(20);
        versionBox.visible = false;
        addRenderableWidget(versionBox);

        this.startNodeBox = new EditBox(this.font, rightStartX + 8, fieldY + 2 * (fieldHeight + 12), fieldWidth, fieldHeight, Component.literal(""));
        startNodeBox.setMaxLength(100);
        startNodeBox.visible = false;
        addRenderableWidget(startNodeBox);

        // ---- 文本编辑框（节点详情） ----
        int textY = this.height - 80;
        int textHeight = 64;
        this.textBox = new EditBox(
                this.font,
                rightStartX + LIST_PADDING,
                textY,
                rightWidth - LIST_PADDING * 2,
                textHeight,
                Component.literal("选中条目在此编辑内容")
        );
        textBox.setMaxLength(10000);
        textBox.visible = false;
        addRenderableWidget(textBox);

        // ---- 列表绘制区域 ----
        listX = 4;
        listY = extraY + BTN_HEIGHT + 4;
        listWidth = LEFT_PANEL_WIDTH - 8;
        listHeight = this.height - listY - 8;

        // 默认状态
        currentMode = "编辑";
        currentSubTab = "metadata";
        showExtraButtons(true);
        showMetadataForm();
    }

    // ===================== 文件对话框 =====================

    private void openFileDialog() {
        java.awt.FileDialog fd = new java.awt.FileDialog(
                (java.awt.Frame) null,
                "选择故事文件",
                java.awt.FileDialog.LOAD
        );
        fd.setDirectory(getStoryDir().getAbsolutePath());
        fd.setFile("*.json");
        fd.setVisible(true);

        String file = fd.getFile();
        String dir = fd.getDirectory();
        if (file != null && dir != null) {
            File selected = new File(dir, file);
            if (selected.exists()) {
                loadStoryData(selected);
            } else {
                sendFeedback("文件不存在");
            }
        }
    }

    private void saveFileDialog() {
        if (currentStoryData == null) {
            sendFeedback("没有数据可保存，请先打开一个文件");
            return;
        }
        java.awt.FileDialog fd = new java.awt.FileDialog(
                (java.awt.Frame) null,
                "保存故事文件",
                java.awt.FileDialog.SAVE
        );
        fd.setDirectory(getStoryDir().getAbsolutePath());
        fd.setFile("story.json");
        fd.setVisible(true);

        String file = fd.getFile();
        String dir = fd.getDirectory();
        if (file != null && dir != null) {
            File target = new File(dir, file);
            saveAllData(target);
        }
    }

    // ===================== 数据加载/保存 =====================

    private void loadStoryData(File file) {
        if (!file.exists()) {
            sendFeedback("文件不存在: " + file.getName());
            return;
        }
        try (FileReader reader = new FileReader(file)) {
            currentStoryData = JsonParser.parseReader(reader).getAsJsonObject();
            storyTitle = getSafeString(currentStoryData, "title");
            storyVersion = getSafeString(currentStoryData, "version");
            storyDate = getSafeString(currentStoryData, "date");
            storyStartNode = getSafeString(currentStoryData, "startNode");
            currentLoadedFile = file;
            if (titleBox != null) titleBox.setValue(storyTitle);
            if (versionBox != null) versionBox.setValue(storyVersion);
            if (startNodeBox != null) startNodeBox.setValue(storyStartNode);
            showMetadataForm();
            sendFeedback("已加载: " + file.getName());
        } catch (Exception e) {
            e.printStackTrace();
            sendFeedback("加载失败: " + e.getMessage());
        }
    }

    private void saveAllData(File file) {
        if (currentStoryData == null) {
            sendFeedback("没有数据可保存");
            return;
        }
        if (titleBox != null && titleBox.visible) {
            currentStoryData.addProperty("title", titleBox.getValue());
        }
        if (versionBox != null && versionBox.visible) {
            currentStoryData.addProperty("version", versionBox.getValue());
        }
        if (startNodeBox != null && startNodeBox.visible) {
            currentStoryData.addProperty("startNode", startNodeBox.getValue());
        }
        currentStoryData.addProperty("date", LocalDate.now().toString());

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(new Gson().toJson(currentStoryData));
            currentLoadedFile = file;
            sendFeedback("已保存: " + file.getName());
        } catch (Exception e) {
            e.printStackTrace();
            sendFeedback("保存失败: " + e.getMessage());
        }
    }

    private File getStoryDir() {
        File gameDir = Minecraft.getInstance().gameDirectory;
        File storyDir = new File(gameDir, "storyeditor");
        if (!storyDir.exists()) storyDir.mkdirs();
        return storyDir;
    }

    private String getSafeString(JsonObject obj, String key) {
        return obj.has(key) ? obj.get(key).getAsString() : "";
    }

    // ===================== UI 控制 =====================

    private void showMetadataForm() {
        entryList.clear();
        boolean hasData = (currentStoryData != null);
        titleBox.visible = hasData;
        versionBox.visible = hasData;
        startNodeBox.visible = hasData;
        textBox.visible = false;
        if (hasData) {
            titleBox.setValue(getSafeString(currentStoryData, "title"));
            versionBox.setValue(getSafeString(currentStoryData, "version"));
            startNodeBox.setValue(getSafeString(currentStoryData, "startNode"));
            titleBox.setCursorPosition(0);
            versionBox.setCursorPosition(0);
            startNodeBox.setCursorPosition(0);
        }
    }

    private void loadNodeList() {
        entryList.clear();
        textBox.visible = true;
        hideEditBoxes();
        if (currentStoryData == null) return;

        JsonObject nodes = currentStoryData.getAsJsonObject("node");
        if (nodes == null) return;

        for (String key : nodes.keySet()) {
            JsonObject node = nodes.getAsJsonObject(key);
            String narrator = node.has("Narrator") ? node.get("Narrator").getAsString() : "未知";
            String text = node.has("text") ? node.get("text").getAsString() : "";
            int newlineIdx = text.indexOf('\n');
            String previewText = (newlineIdx != -1) ? text.substring(0, newlineIdx) : text;
            previewText = truncateByWidth(previewText, 25);
            entryList.add(new ListEntry(key, narrator + "：" + previewText, "节点ID: " + key));
        }
    }

    private String truncateByWidth(String text, int maxWidth) {
        if (text == null || text.isEmpty()) return text;
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean isWide = Character.isIdeographic(c) || (c >= 0xFF00 && c <= 0xFFEF);
            int charWidth = isWide ? 2 : 1;
            if (width + charWidth > maxWidth) {
                return text.substring(0, i) + "...";
            }
            width += charWidth;
        }
        return text;
    }

    private void hideEditBoxes() {
        titleBox.visible = false;
        versionBox.visible = false;
        startNodeBox.visible = false;
    }

    private void showExtraButtons(boolean show) {
        extraBtnMeta.visible = show;
        extraBtnNode.visible = show;
        if (!show) {
            entryList.clear();
            hideEditBoxes();
            textBox.visible = false;
        }
    }

    private void renderEntryList(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFF181818);
        guiGraphics.fill(x - 1, y, x, y + height, 0xFF555555);

        int drawY = y + LIST_PADDING;
        for (ListEntry entry : entryList) {
            if (drawY + LIST_ITEM_HEIGHT > y + height) break;
            guiGraphics.fill(x + 2, drawY, x + width - 2, drawY + LIST_ITEM_HEIGHT - 1, 0xFF282828);
            guiGraphics.drawString(font, entry.displayName, x + 6, drawY + 3, 0xFFFFFFFF);
            guiGraphics.drawString(font, entry.desc, x + 6, drawY + 14, 0xFFAAAAAA);
            drawY += LIST_ITEM_HEIGHT;
        }
    }

    private void sendFeedback(String msg) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.literal("§a[故事编辑器] " + msg), false
            );
        }
    }

    // ===================== 绘制 =====================

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x80_000000);
        guiGraphics.fill(0, 0, this.width, this.height, 0x22_FFFFFF);

        guiGraphics.fill(0, 0, LEFT_PANEL_WIDTH, this.height, 0xFF_121212);
        guiGraphics.fill(LEFT_PANEL_WIDTH - 1, 0, LEFT_PANEL_WIDTH, this.height, 0x33_FFFFFF);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if ("编辑".equals(currentMode) && "metadata".equals(currentSubTab)) {
            if (currentStoryData != null && currentLoadedFile != null) {
                guiGraphics.drawString(font, "存储文件: " + currentLoadedFile.getName(), LEFT_PANEL_WIDTH + 16, 30, 0x888888);
                guiGraphics.drawString(font, "故事元数据", LEFT_PANEL_WIDTH + 16, 50, 0xCCCCCC);
                guiGraphics.drawString(font, "标题", LEFT_PANEL_WIDTH + 16, 60, 0x888888);
                guiGraphics.drawString(font, "版本", LEFT_PANEL_WIDTH + 16, 88, 0x888888);
                guiGraphics.drawString(font, "开始节点", LEFT_PANEL_WIDTH + 16, 116, 0x888888);
                guiGraphics.drawString(font, "制作时间: " + storyDate, LEFT_PANEL_WIDTH + 16, 150, 0x666666);
            } else {
                guiGraphics.drawString(font, "点击“打开”按钮选择故事文件", LEFT_PANEL_WIDTH + 16, 50, 0x888888);
            }
        }

        if ("编辑".equals(currentMode) && "node".equals(currentSubTab) && !entryList.isEmpty()) {
            renderEntryList(guiGraphics, listX, listY, listWidth, listHeight);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}