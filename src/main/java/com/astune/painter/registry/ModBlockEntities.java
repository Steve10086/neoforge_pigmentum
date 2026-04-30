package com.astune.painter.registry;

import com.astune.painter.block.CanvasBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "painter");
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CanvasBlockEntity>> CANVAS = BLOCK_ENTITIES.register("canvas",
            () -> BlockEntityType.Builder.of(CanvasBlockEntity::new, ModBlocks.CANVAS.get()).build(null));
}