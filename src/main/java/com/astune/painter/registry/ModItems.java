package com.astune.painter.registry;

import com.astune.painter.item.DebugEraser;
import com.astune.painter.item.DebugPaintbrush;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, "painter");

    public static final DeferredHolder<Item, DebugPaintbrush> PAINTBRUSH =
            ITEMS.register("debug_paintbrush",
                    () -> new DebugPaintbrush(new Item.Properties()));

    public static final DeferredHolder<Item, DebugEraser> ERASER =
            ITEMS.register("debug_eraser",
                    () -> new DebugEraser(new Item.Properties()));
}
