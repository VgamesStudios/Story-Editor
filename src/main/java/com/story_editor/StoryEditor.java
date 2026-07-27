package com.story_editor;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("story_editor")
public class StoryEditor {
    public StoryEditor() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        StoryEditorItem.ITEMS.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);
    }
}