package com.astune.painter.registry;

import com.astune.painter.block.CanvasBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, "painter");
    public static final DeferredHolder<Block, CanvasBlock> CANVAS = BLOCKS.register("canvas", CanvasBlock::new);
}