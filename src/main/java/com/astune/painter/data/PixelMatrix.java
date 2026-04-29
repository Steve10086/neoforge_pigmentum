package com.astune.painter.data;

import com.astune.painter.api.IPixelMatrix;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import java.util.Arrays;

public class PixelMatrix implements IPixelMatrix {
    private final int[] colors = new int[TOTAL_PIXELS];

    public PixelMatrix() {
        fill(0x00000000); // 透明
    }

    @Override
    public int getColor(int x, int y) {
        return colors[y * SIZE + x];
    }

    @Override
    public void setColor(int x, int y, int color) {
        colors[y * SIZE + x] = color;
    }

    @Override
    public int[] getRawData() {
        return colors; // 注意：返回内部引用，调用者不应直接修改
    }

    @Override
    public void fill(int color) {
        Arrays.fill(colors, color);
    }

    @Override
    public void readFromNBT(CompoundTag tag) {
        int[] raw = tag.getIntArray("Pixels");
        System.arraycopy(raw, 0, colors, 0, Math.min(raw.length, TOTAL_PIXELS));
    }

    @Override
    public void writeToNBT(CompoundTag tag) {
        tag.putIntArray("Pixels", Arrays.copyOf(colors, TOTAL_PIXELS));
    }

    public static final MapCodec<PixelMatrix> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT_STREAM.xmap(
                    stream -> stream.toArray(),
                    Arrays::stream
            ).fieldOf("pixels").forGetter(m -> m.colors)
    ).apply(instance, PixelMatrix::new));

    public PixelMatrix(int[] raw) {
        System.arraycopy(raw, 0, colors, 0, Math.min(raw.length, TOTAL_PIXELS));
    }
}
