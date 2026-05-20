package com.astune.painter.registry;

import com.astune.painter.Painter;
import com.astune.painter.api.CanvasData;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Painter.MODID);

    public static final Supplier<DataComponentType<CanvasData>> CANVAS =
            DATA_COMPONENT_TYPES.register("canvas",
                    () -> DataComponentType.<CanvasData>builder()
                            .persistent(CanvasData.CODEC)
                            .networkSynchronized(CanvasData.STREAM_CODEC)
                            .build());

    public static final Supplier<DataComponentType<BlockState>> BLOCK_STATE =
            DATA_COMPONENT_TYPES.register("block_state",
                    () -> DataComponentType.<BlockState>builder()
                            .persistent(BlockState.CODEC)
                            .networkSynchronized(ByteBufCodecs.fromCodec(BlockState.CODEC))
                            .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CURRENT_COLOR =
            DATA_COMPONENT_TYPES.register("current_color",
                    () -> DataComponentType.<Integer>builder()
                            .persistent(Codec.INT) // 使用 INT 类型的编解码器
                            .build()
            );
}