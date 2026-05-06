package com.astune.painter.registry;

import com.astune.painter.Painter;
import com.astune.painter.data.CanvasData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Supplier;

public class ModDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Painter.MODID);

    // 画布数据组件：存储绘画内容
    public static final Supplier<DataComponentType<CanvasData>> CANVAS_DATA =
            DATA_COMPONENTS.register("canvas_data",
                    () -> DataComponentType.<CanvasData>builder()
                            .persistent(CanvasData.CODEC)          // 用于 NBT 持久化
                            .networkSynchronized(CanvasData.STREAM_CODEC) // 用于网络同步
                            .build()
            );
}