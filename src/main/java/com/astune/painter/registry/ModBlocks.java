package com.astune.painter.registry;

import com.astune.painter.Painter;
import com.astune.painter.block.CanvasBlock;
import com.astune.painter.block.NoOcclusionCanvasBlock;
import com.astune.painter.block.OcclusionCanvasBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(BuiltInRegistries.BLOCK, Painter.MODID);

    public static final DeferredHolder<Block, NoOcclusionCanvasBlock> CANVAS_NO_OCCLUSION =
            BLOCKS.register("canvas_no_occlusion", NoOcclusionCanvasBlock::new);

    public static final DeferredHolder<Block, OcclusionCanvasBlock> CANVAS_OCCLUSION =
            BLOCKS.register("canvas_occlusion", OcclusionCanvasBlock::new);
}