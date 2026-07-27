package com.story_editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.util.List;

public class StoryEditorUI extends Screen {
    private final ItemStack stack;
    private final StoryEditorDataManager dataManager;

    private EditBox textBox;
    private EditBox titleBox, versionBox, startNodeBox;
    private Button chapterBtn, roleBtn, settingBtn;
    private Button extraBtnMeta, extraBtnNode;
    private Button metaSaveButton;

    private static final int LEFT_PANEL_WIDTH = 144;
    private static final int BTN_WIDTH = 48;
    private static final int BTN_HEIGHT = 20;
    private static final int EXTRA_BTN_WIDTH = LEFT_PANEL_WIDTH / 2;
    private static final int LIST_ITEM_HEIGHT = 26;
    private static final int LIST_PADDING = 4;

    private String currentMode = "编辑";
    private String currentSubTab = "metadata"; // metadata, node, filebrowser
    private List<StoryEditorListEntry> entryList;      // 节点列表
    private List<StoryEditorListEntry> fileList;        // 文件列表

    private int listX, listY, listWidth, listHeight;
    private int labelTitleY, labelVersionY, labelStartNodeY, labelDateY;
    private int fieldY;

    public StoryEditorUI(ItemStack stack) {
        super(Component.literal(""));
        this.stack = stack;
        this.dataManager = new StoryEditorDataManager();
        this.entryList = dataManager.getNodeList();
        // 注意：不要在构造函数中初始化 fileList，等 init 中刷新后再赋值
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
                    currentSubTab = "filebrowser";
                    dataManager.refreshFileList();
                    fileList = dataManager.getFileList();  // 直接赋值，不操作原列表
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

        // ---- 集中创建元数据输入框，并记录标签 Y 坐标 ----
        int fieldHeight = 20;
        int fieldWidth = rightWidth - 16;
        fieldY = 50;

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

        labelTitleY = fieldY - 10;
        labelVersionY = fieldY + fieldHeight + 12 - 10;
        labelStartNodeY = fieldY + 2 * (fieldHeight + 12) - 10;
        labelDateY = fieldY + 3 * (fieldHeight + 12) - 10;

        // ---- 元数据保存按钮 ----
        int saveBtnY = this.height - 24;
        int saveBtnH = 20;
        int saveBtnX = LEFT_PANEL_WIDTH + 8;
        int saveBtnW = this.width - saveBtnX - 8;

        this.metaSaveButton = Button.builder(
                Component.literal("保存"),
                b -> {
                    if (dataManager.hasData()) {
                        if (titleBox.visible) {
                            dataManager.updateMetadata(
                                    titleBox.getValue(),
                                    versionBox.getValue(),
                                    startNodeBox.getValue()
                            );
                        }
                        dataManager.saveCurrentFile();
                    } else {
                        sendFeedback("请先加载一个文件");
                    }
                }
        ).bounds(saveBtnX, saveBtnY, saveBtnW, saveBtnH).build();
        metaSaveButton.visible = false;
        addRenderableWidget(metaSaveButton);

        // ---- 文本编辑框 ----
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

        // ---- 列表区域 ----
        listX = 4;
        listY = extraY + BTN_HEIGHT + 4;
        listWidth = LEFT_PANEL_WIDTH - 8;
        listHeight = this.height - listY - 8;

        // 默认状态
        currentMode = "编辑";
        currentSubTab = "metadata";
        showExtraButtons(true);
        showMetadataForm();

        // ---- 初始化文件列表 ----
        dataManager.refreshFileList();
        fileList = dataManager.getFileList(); // 直接赋值
    }

    // ==================== UI 控制 ====================

    private void showMetadataForm() {
        entryList.clear();
        boolean hasData = dataManager.hasData();
        titleBox.visible = hasData;
        versionBox.visible = hasData;
        startNodeBox.visible = hasData;
        textBox.visible = false;
        if (hasData) {
            titleBox.setValue(dataManager.getStoryTitle());
            versionBox.setValue(dataManager.getStoryVersion());
            startNodeBox.setValue(dataManager.getStoryStartNode());
            titleBox.setCursorPosition(0);
            versionBox.setCursorPosition(0);
            startNodeBox.setCursorPosition(0);
        }
        if ("filebrowser".equals(currentSubTab)) {
            currentSubTab = "metadata";
        }
    }

    private void loadNodeList() {
        entryList.clear();
        textBox.visible = true;
        hideEditBoxes();
        entryList.addAll(dataManager.getNodeList());
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

    private void renderEntryList(GuiGraphics guiGraphics, int x, int y, int width, int height, List<StoryEditorListEntry> list) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFF181818);
        guiGraphics.fill(x - 1, y, x, y + height, 0xFF555555);

        int drawY = y + LIST_PADDING;
        for (StoryEditorListEntry entry : list) {
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
                    Component.literal("[故事编辑器] " + msg), false
            );
        }
    }

    // ==================== 绘制 ====================
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x80_000000);
        guiGraphics.fill(0, 0, this.width, this.height, 0x22_FFFFFF);

        guiGraphics.fill(0, 0, LEFT_PANEL_WIDTH, this.height, 0xFF_121212);
        guiGraphics.fill(LEFT_PANEL_WIDTH - 1, 0, LEFT_PANEL_WIDTH, this.height, 0x33_FFFFFF);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // ---- 元数据界面 ----
        if ("编辑".equals(currentMode) && "metadata".equals(currentSubTab)) {
            if (dataManager.hasData() && dataManager.getCurrentLoadedFile() != null) {
                guiGraphics.drawString(font, "存储文件: " + dataManager.getCurrentLoadedFile().getName(), LEFT_PANEL_WIDTH + 16, 10, 0x888888);
                guiGraphics.drawString(font, "故事元数据", LEFT_PANEL_WIDTH + 16, 25, 0xCCCCCC);
                guiGraphics.drawString(font, "标题", LEFT_PANEL_WIDTH + 16, labelTitleY, 0x888888);
                guiGraphics.drawString(font, "版本", LEFT_PANEL_WIDTH + 16, labelVersionY, 0x888888);
                guiGraphics.drawString(font, "开始节点", LEFT_PANEL_WIDTH + 16, labelStartNodeY, 0x888888);
                guiGraphics.drawString(font, "制作时间: " + dataManager.getStoryDate(), LEFT_PANEL_WIDTH + 16, labelDateY, 0x666666);
                metaSaveButton.visible = true;
            } else {
                guiGraphics.drawString(font, "点击“目录”选择故事文件", LEFT_PANEL_WIDTH + 16, 50, 0x888888);
                metaSaveButton.visible = false;
            }
        }

        // ---- 文件浏览列表 ----
        if ("目录".equals(currentMode) && "filebrowser".equals(currentSubTab)) {
            guiGraphics.drawString(font, "选择文件", LEFT_PANEL_WIDTH + 16, 30, 0xCCCCCC);
            if (fileList != null && !fileList.isEmpty()) {
                renderEntryList(guiGraphics, listX, listY, listWidth, listHeight, fileList);
            } else {
                guiGraphics.drawString(font, "暂无 .json 文件", LEFT_PANEL_WIDTH + 16, 50, 0x888888);
            }
        }

        // ---- 节点列表 ----
        if ("编辑".equals(currentMode) && "node".equals(currentSubTab) && !entryList.isEmpty()) {
            renderEntryList(guiGraphics, listX, listY, listWidth, listHeight, entryList);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if ("目录".equals(currentMode) && "filebrowser".equals(currentSubTab)) {
            int x = listX;
            int y = listY + LIST_PADDING;
            for (StoryEditorListEntry entry : fileList) {
                if (y + LIST_ITEM_HEIGHT > listY + listHeight) break;
                if (mouseX >= x && mouseX <= x + listWidth &&
                        mouseY >= y && mouseY <= y + LIST_ITEM_HEIGHT) {
                    File dir = new File(Minecraft.getInstance().gameDirectory, "storyeditor");
                    File target = new File(dir, entry.displayName);
                    if (target.exists()) {
                        if (dataManager.loadStoryData(target)) {
                            showMetadataForm();
                            currentMode = "编辑";
                            currentSubTab = "metadata";
                            showExtraButtons(true);
                            entryList.clear();
                            if (titleBox.visible) {
                                titleBox.setValue(dataManager.getStoryTitle());
                                versionBox.setValue(dataManager.getStoryVersion());
                                startNodeBox.setValue(dataManager.getStoryStartNode());
                            }
                        }
                        return true;
                    }
                }
                y += LIST_ITEM_HEIGHT;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}