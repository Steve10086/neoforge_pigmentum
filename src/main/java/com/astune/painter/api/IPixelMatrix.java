package com.astune.painter.api;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * 16×16 像素矩阵接口，每个像素以 ARGB int 存储。
 * 提供基本的读写访问。
 */
public interface IPixelMatrix {
    int SIZE = 16;
    int PIXEL_COUNT = SIZE * SIZE;

    int getWidth();

    int getHeight();

    /**
     * 获取指定坐标的像素颜色（ARGB）。
     * @param x 0~15
     * @param y 0~15
     * @return ARGB 颜色值
     */
    int getPixel(int x, int y);

    /**
     * 设置指定坐标的像素颜色。
     * @param x 0~15
     * @param y 0~15
     * @param color ARGB 颜色值
     */
    boolean setPixel(int x, int y, int color);

    IPixelMatrix copy();

    /**
     * 填充整个矩阵为指定颜色。
     */
    void fill(int color);

    /**
     * 获取内部数据副本（大小为 256 的数组）。
     */
    int[] getPixels();

    /**
     * 创建一个空白的矩阵（全部透明：0x00000000）。
     */
    static IPixelMatrix createEmpty() {
        return (IPixelMatrix) new PixelMatrix();
    }

    boolean isEmpty();
}