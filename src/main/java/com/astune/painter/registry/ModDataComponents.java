package com.astune.painter.registry;

import com.astune.painter.data.CanvasData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "painter");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CanvasData>> CANVAS = DATA_COMPONENTS.register("canvas_data",
            () -> DataComponentType.<CanvasData>builder()
                    .persistent(CanvasData.CODEC)
                    .networkSynchronized(CanvasData.STREAM_CODEC)
                    .build()
    );
    // 用于存储原方块状态
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockState>> BLOCK_STATE = DATA_COMPONENTS.register("block_state",
            () -> DataComponentType.<BlockState>builder()
                    .persistent(BlockState.CODEC)
                    .build()
    );
}