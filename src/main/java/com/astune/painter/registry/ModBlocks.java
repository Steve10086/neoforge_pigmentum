package com.astune.painter.registry;

import com.astune.painter.block.CanvasMarkerBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, "painter");
    public static final Supplier<Block> CANVAS_MARKER = BLOCKS.register("canvas_marker",
            () -> new CanvasMarkerBlock(Block.Properties.of()));
}