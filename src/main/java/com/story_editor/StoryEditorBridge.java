package com.story_editor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class StoryEditorBridge {
    private final ItemStack stack;

    public StoryEditorBridge(ItemStack stack) {
        this.stack = stack;
    }

    // 供 JavaScript 调用，加载故事内容
    public String loadStory() {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("story_content")) {
            return tag.getString("story_content");
        }
        return "";
    }

    // 供 JavaScript 调用，保存故事内容
    public void saveStory(String content) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("story_content", content);
        stack.setTag(tag);
        // 可选：发送消息提示玩家（通过 Minecraft 客户端）
    }
}