package com.astune.painter.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "painter");

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PAINTING_TAB =
            CREATIVE_TABS.register("painter_debug", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.painter.painting"))
                            .icon(() -> new ItemStack(ModItems.PAINTBRUSH.get()))
                            .displayItems((parameters, output) -> {
                                output.accept(ModItems.PAINTBRUSH.get());
                                output.accept(ModItems.ERASER.get());
                                // 未来添加更多物品时继续 output.accept(...)
                            })
                            .build()
            );
}
