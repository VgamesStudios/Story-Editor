package com.story_editor;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.Minecraft;

public class StoryEditorUI extends Screen {
    private final ItemStack stack;
    private EditBox textBox;
    private Button saveButton;

    public StoryEditorUI(ItemStack stack) {
        super(Component.literal("剧情编辑器"));
        this.stack = stack;
    }

    @Override
    protected void init() {
        super.init();
        String initialText = "";
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("story_content")) {
            initialText = tag.getString("story_content");
        }

        this.textBox = new EditBox(
            this.font,
            this.width / 2 - 100,
            this.height / 2 - 40,
            200,
            20,
            Component.literal("输入剧情")
        );
        textBox.setValue(initialText);
        textBox.setMaxLength(1000);
        addRenderableWidget(textBox);

        this.saveButton = Button.builder(
            Component.literal("保存"),
            button -> {
                String content = textBox.getValue();
                CompoundTag newTag = stack.getOrCreateTag();
                newTag.putString("story_content", content);
                stack.setTag(newTag);
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.displayClientMessage(
                        Component.literal("§a剧情已保存！"), false
                    );
                }
            }
        )
        .bounds(this.width / 2 - 50, this.height / 2 + 20, 100, 20)
        .build();
        addRenderableWidget(saveButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(
            this.font,
            Component.literal("✏️ 编辑剧情"),
            this.width / 2,
            this.height / 2 - 70,
            0xFFFFFF
        );
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}