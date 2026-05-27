package com.astune.painter.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * 可变尺寸的像素矩阵，每个像素代表 1/16 方块单位的正方形。
 */
public class PixelMatrix implements IPixelMatrix{

    private final int width;   // 水平方向像素数
    private final int height;  // 垂直方向像素数
    private final int[] pixels; // ARGB，行优先

    public PixelMatrix(int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.pixels = new int[this.width * this.height];
        Arrays.fill(pixels, 0x00000000); // 透明
    }

    // 默认 16×16 构造
    public PixelMatrix() {
        this(16, 16);
    }

    @Override
    public IPixelMatrix copy() {
        return null;
    }

    @Override
    public int getWidth() { return width; }

    @Override
    public int getHeight() { return height; }

    @Override
    public int getPixel(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return 0;
        return pixels[y * width + x];
    }

    @Override
    public boolean setPixel(int x, int y, int color) {
        //System.out.println("[PixelMatrix] add pixel on " + x + ", " + y);
        if (x < 0 || x >= width || y < 0 || y >= height) return false;
        if (pixels[y * width + x] == color) return false;
        pixels[y * width + x] = color;
        return true;
    }

    @Override
    public void fill(int color) {
        Arrays.fill(pixels, color);
    }

    @Override
    public int[] getPixels() {
        return pixels;
    }

    @Override
    public boolean isEmpty() {
        for (int p : pixels) {
            if (p != 0) return false;
        }
        return true;
    }
    public void fillWhite() {
        fill(0xFFFFFFFF);
    }

    // 全白 16×16 静态方法（兼容旧测试）
    public static PixelMatrix fullWhite() {
        PixelMatrix m = new PixelMatrix(16, 16);
        m.fill(0xFFFFFFFF);
        return m;
    }

    // 序列化：宽、高、像素数据
    public static final Codec<PixelMatrix> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("width").forGetter(PixelMatrix::getWidth),
                    Codec.INT.fieldOf("height").forGetter(PixelMatrix::getHeight),
                    Codec.list(Codec.INT).fieldOf("pixels").forGetter(
                            m -> Arrays.stream(m.pixels).boxed().toList()
                    )
            ).apply(instance, (w, h, p) -> {
                PixelMatrix m = new PixelMatrix(w, h);
                for (int i = 0; i < Math.min(p.size(), w * h); i++) {
                    m.pixels[i] = p.get(i);
                }
                return m;
            })
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PixelMatrix> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public PixelMatrix decode(RegistryFriendlyByteBuf buf) {
                    int w = buf.readVarInt();
                    int h = buf.readVarInt();
                    PixelMatrix m = new PixelMatrix(w, h);
                    for (int i = 0; i < w * h; i++) {
                        m.pixels[i] = buf.readInt();
                    }
                    return m;
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, PixelMatrix matrix) {
                    buf.writeVarInt(matrix.width);
                    buf.writeVarInt(matrix.height);
                    for (int pixel : matrix.pixels) {
                        buf.writeInt(pixel);
                    }
                }
            };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PixelMatrix that)) return false;
        return width == that.width && height == that.height && Arrays.equals(pixels, that.pixels);
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(width);
        result = 31 * result + Integer.hashCode(height);
        result = 31 * result + Arrays.hashCode(pixels);
        return result;
    }
}