package com.astune.painter.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Arrays;

public class PixelMatrix implements IPixelMatrix {
    private final int[] colors = new int[TOTAL];

    public PixelMatrix() {
        fill(0x00000000);
    }

    // 用于 Codec 的构造器
    public PixelMatrix(int[] raw) {
        System.arraycopy(raw, 0, colors, 0, Math.min(raw.length, TOTAL));
    }

    @Override public int getColor(int x, int y) { return colors[y * SIZE + x]; }
    @Override public void setColor(int x, int y, int color) { colors[y * SIZE + x] = color; }
    @Override public int[] getRaw() { return colors; }
    @Override public void fill(int color) { Arrays.fill(colors, color); }

    // -- 序列化支持 --
    public static final Codec<PixelMatrix> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT_STREAM.xmap(
                            stream -> stream.toArray(),
                            Arrays::stream
                    ).fieldOf("colors").forGetter(m -> Arrays.copyOf(m.colors, TOTAL))
            ).apply(instance, PixelMatrix::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PixelMatrix> STREAM_CODEC = StreamCodec.of(
            (buf, m) -> buf.writeVarIntArray(m.colors),
            buf -> new PixelMatrix(buf.readVarIntArray())
    );

    @Override
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("data", this.colors); // 假设内部是 int[] pixels
        return tag;
    }

    public static PixelMatrix fromNbt(CompoundTag tag) {
        int[] data = tag.getIntArray("data");
        PixelMatrix matrix = new PixelMatrix();
        if (data.length == SIZE * SIZE) {
            System.arraycopy(data, 0, matrix.colors, 0, data.length);
        }
        return matrix;
    }
}