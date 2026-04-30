package com.astune.painter.data;

import com.astune.painter.api.PixelMatrix;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record CanvasData(Direction face, PixelMatrix pixels) {
    public static final Codec<CanvasData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Direction.CODEC.fieldOf("face").forGetter(CanvasData::face),
            PixelMatrix.CODEC.fieldOf("pixels").forGetter(CanvasData::pixels)
    ).apply(instance, CanvasData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CanvasData> STREAM_CODEC = StreamCodec.composite(
            Direction.STREAM_CODEC, CanvasData::face,
            PixelMatrix.STREAM_CODEC, CanvasData::pixels,
            CanvasData::new
    );

    // 为 PixelMatrix 添加 Codec/StreamCodec 支持
    // 请见下方修改 PixelMatrix，将其放在 api 包内并添加序列化能力
}