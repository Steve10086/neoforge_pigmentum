package com.astune.painter.data;

import com.astune.painter.api.IPixelMatrix;
import com.astune.painter.api.PixelMatrix;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record CanvasData(Direction face, IPixelMatrix pixels) {

    // ======================== 序列化 ========================
    // 正确的 xmap，确保产生 Codec<IPixelMatrix>
    public static final Codec<CanvasData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Direction.CODEC.fieldOf("face").forGetter(CanvasData::face),
            PixelMatrix.CODEC
                    .xmap(
                            pm -> (IPixelMatrix) pm,          // 解码后：PixelMatrix → IPixelMatrix
                            ipm -> (PixelMatrix) ipm         // 编码前：IPixelMatrix → PixelMatrix
                    )
                    .fieldOf("pixels")
                    .forGetter(CanvasData::pixels)
    ).apply(instance, CanvasData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CanvasData> STREAM_CODEC = StreamCodec.composite(
            Direction.STREAM_CODEC, CanvasData::face,
            PixelMatrix.STREAM_CODEC,
            pm -> (PixelMatrix) pm.pixels,                             // IPixelMatrix → PixelMatrix（写入）
            CanvasData::new
    );

    // ======================== NBT 读写 ========================
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("face", this.face.getName());
        tag.put("pixels", this.pixels.toNbt());   // 运行时一定是 PixelMatrix
        return tag;
    }

    public static CanvasData fromNbt(CompoundTag tag) {
        Direction face = Direction.byName(tag.getString("face"));
        IPixelMatrix pixels = PixelMatrix.fromNbt(tag.getCompound("pixels"));
        return new CanvasData(face, pixels);
    }

    // ======================== 工厂方法 ========================
    /** 创建全透明的画布（指定面） */
    public static CanvasData createEmpty(Direction face) {
        return new CanvasData(face, new PixelMatrix());   // 默认 int[256]，全是 0（透明）
    }

    /** 创建一个画布，并用指定颜色填满整个面 */
    public static CanvasData createFilled(Direction face, int argbColor) {
        PixelMatrix matrix = new PixelMatrix();
        for (int x = 0; x < IPixelMatrix.SIZE; x++) {
            for (int y = 0; y < IPixelMatrix.SIZE; y++) {
                matrix.setColor(x, y, argbColor);
            }
        }
        return new CanvasData(face, matrix);
    }

    /** 返回一个新 CanvasData，指定面被填充为给定颜色（不可变） */
    public CanvasData withFilledFace(Direction face, int argbColor) {
        // 无论面是否相同，直接创建新画布（性能不是瓶颈）
        return createFilled(face, argbColor);
    }
}