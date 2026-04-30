package com.astune.painter.api;

import net.minecraft.nbt.CompoundTag;

/**
 * 二维像素画矩阵，尺寸固定为 16x16。
 * 可被子类化以支持压缩存储、动画帧等高级特性。
 */
public interface IPixelMatrix {
    int SIZE = 16;
    int TOTAL = SIZE * SIZE;

    int getColor(int x, int y);
    void setColor(int x, int y, int color);
    int[] getRaw();
    void fill(int color);
}
