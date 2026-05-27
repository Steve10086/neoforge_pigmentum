package com.astune.painter.registry;

import com.astune.painter.block.CanvasBlockItem;
import com.astune.painter.item.DebugPaintbrush;
import com.astune.painter.item.Paintbrush;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.astune.painter.api.BlendMode.ADD;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "painter");
    public static final DeferredHolder<Item, DebugPaintbrush> DEBUG_PAINTBRUSH = ITEMS.register("debug_paintbrush",
            () -> new DebugPaintbrush(new Item.Properties()
                    // 设置默认组件，让所有新画笔默认是白色
                    .component(ModDataComponents.CURRENT_COLOR.get(), 0xFFFFFFFF)
                    .component(ModDataComponents.BRUSH_SIZE.get(), 0.0625)
                    .component(ModDataComponents.FEATHER_STRENGTH.get(), 0.0f)
                    .component(ModDataComponents.BLEND_MODE.get(), ADD.name())
                    .component(ModDataComponents.STEP_SIZE.get(), 0.01)
                    .component(ModDataComponents.OPACITY.get(), 1.0f)
            )
    );

    public static final DeferredHolder<Item, Paintbrush> PAINTBRUSH = ITEMS.register("paintbrush",
            () -> new Paintbrush(new Item.Properties()
                    .stacksTo(1)
                    .component(ModDataComponents.CURRENT_COLOR.get(), 0xFFFFFFFF)
                    .component(ModDataComponents.BRUSH_SIZE.get(), 0.0625)
                    .component(ModDataComponents.FEATHER_STRENGTH.get(), 0.0f)
                    .component(ModDataComponents.BLEND_MODE.get(), ADD.name())
                    .component(ModDataComponents.STEP_SIZE.get(), 0.01)
                    .component(ModDataComponents.OPACITY.get(), 1.0f)
            )
    );
    public static final DeferredHolder<Item, BlockItem> CANVAS_NO_OCCLUSION_ITEM =
            ITEMS.register("canvas_no_occlusion",
                    () -> new CanvasBlockItem(ModBlocks.CANVAS_NO_OCCLUSION.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> CANVAS_OCCLUSION_ITEM =
            ITEMS.register("canvas_occlusion",
                    () -> new CanvasBlockItem(ModBlocks.CANVAS_OCCLUSION.get(), new Item.Properties()));
}
