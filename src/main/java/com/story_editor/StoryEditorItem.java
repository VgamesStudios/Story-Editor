package com.story_editor;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.client.Minecraft;

public class StoryEditorItem extends Item {
    // ---------- 注册表 ----------
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, "story_editor");

    // 注册物品本身
    public static final RegistryObject<Item> STORY_EDITOR_ITEM =
            ITEMS.register("story_editor", () -> new StoryEditorItem(
                new Item.Properties().stacksTo(1)
            ));

    // ---------- 物品构造 ----------
    public StoryEditorItem(Properties properties) {
        super(properties);
    }

    // ---------- 物品行为 ----------
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            Minecraft.getInstance().setScreen(new StoryEditorUI(stack));
        }
        return InteractionResultHolder.success(stack);
    }
}