package com.astune.painter.registry;

import com.astune.painter.block.CanvasBlockItem;
import com.astune.painter.item.DebugPaintbrush;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "painter");
    public static final DeferredHolder<Item, DebugPaintbrush> PAINTBRUSH = ITEMS.register("debug_paintbrush",
            DebugPaintbrush::new);
    public static final DeferredHolder<Item, BlockItem> CANVAS_NO_OCCLUSION_ITEM =
            ITEMS.register("canvas_no_occlusion",
                    () -> new CanvasBlockItem(ModBlocks.CANVAS_NO_OCCLUSION.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> CANVAS_OCCLUSION_ITEM =
            ITEMS.register("canvas_occlusion",
                    () -> new CanvasBlockItem(ModBlocks.CANVAS_OCCLUSION.get(), new Item.Properties()));
}
