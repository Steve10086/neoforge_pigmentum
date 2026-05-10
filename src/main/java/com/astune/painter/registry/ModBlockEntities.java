package com.astune.painter.registry;

import com.astune.painter.Painter;
import com.astune.painter.block.CanvasBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Painter.MODID);

    public static final Supplier<BlockEntityType<CanvasBlockEntity>> CANVAS_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("canvas_block_entity", () ->
                    BlockEntityType.Builder.of(CanvasBlockEntity::new,
                            ModBlocks.CANVAS_OCCLUSION.get(),
                            ModBlocks.CANVAS_NO_OCCLUSION.get()
                    ).build(null));
}