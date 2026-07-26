package com.story_editor;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("story_editor")
public class StoryEditor {
    public static CreativeModeTab CUSTOM_TAB;

    public StoryEditor() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册物品
        StoryEditorItem.ITEMS.register(modEventBus);

        // 在模组加载时注册创造模式标签
        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // 创建自定义标签
        CUSTOM_TAB = CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.story_editor.tab"))
                .icon(() -> new ItemStack(StoryEditorItem.STORY_EDITOR_ITEM.get()))
                .displayItems((parameters, output) -> {
                    output.accept(StoryEditorItem.STORY_EDITOR_ITEM.get());
                    // 可添加更多物品
                })
                .build();

        // 手动注册到注册表
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
                new ResourceLocation("story_editor", "tab"), CUSTOM_TAB);
    }
}