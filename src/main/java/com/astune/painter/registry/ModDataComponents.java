package com.astune.painter.registry;

import com.astune.painter.Painter;
import com.astune.painter.api.CanvasData;
import com.astune.painter.api.CanvasFace;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static net.minecraft.network.codec.ByteBufCodecs.DOUBLE;

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

    // 已有的 CURRENT_COLOR，需确保有 .persistent
    public static final Supplier<DataComponentType<Integer>> CURRENT_COLOR =
            DATA_COMPONENT_TYPES.register("current_color", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
                    .build());

    // 画笔大小
    public static final Supplier<DataComponentType<Double>> BRUSH_SIZE =
            DATA_COMPONENT_TYPES.register("brush_size", () -> DataComponentType.<Double>builder()
                    .persistent(Codec.DOUBLE)
                    .networkSynchronized(ByteBufCodecs.DOUBLE)
                    .build());

    // 羽化强度
    public static final Supplier<DataComponentType<Float>> FEATHER_STRENGTH =
            DATA_COMPONENT_TYPES.register("feather_strength", () -> DataComponentType.<Float>builder()
                    .persistent(Codec.FLOAT)
                    .networkSynchronized(ByteBufCodecs.FLOAT)
                    .build());

    // 混合模式（注意：正式发布建议用 Enum Codec，String 仅作简单演示）
    public static final Supplier<DataComponentType<String>> BLEND_MODE =
            DATA_COMPONENT_TYPES.register("blend_mode", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    // 插值步长
    public static final Supplier<DataComponentType<Double>> STEP_SIZE =
            DATA_COMPONENT_TYPES.register("step_size", () -> DataComponentType.<Double>builder()
                    .persistent(Codec.DOUBLE)
                    .networkSynchronized(ByteBufCodecs.DOUBLE)
                    .build());

    // 透明度
    public static final Supplier<DataComponentType<Float>> OPACITY =
            DATA_COMPONENT_TYPES.register("opacity", () -> DataComponentType.<Float>builder()
                    .persistent(Codec.FLOAT)
                    .networkSynchronized(ByteBufCodecs.FLOAT)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CanvasFace>> STORED_FACE =
            DATA_COMPONENT_TYPES.register("stored_face",
                    () -> DataComponentType.<CanvasFace>builder()
                            .persistent(CanvasFace.CODEC)
                            .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> CANVAS_TEXTURE =
            DATA_COMPONENT_TYPES.register("canvas_texture",
                    () -> DataComponentType.<String>builder()
                            .persistent(Codec.STRING)
                            .build());
}