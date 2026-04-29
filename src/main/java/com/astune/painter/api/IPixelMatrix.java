package com.astune.painter.api;

import net.minecraft.nbt.CompoundTag;

/**
 * 二维像素画矩阵，尺寸固定为 16x16。
 * 可被子类化以支持压缩存储、动画帧等高级特性。
 */
public interface IPixelMatrix {
    int SIZE = 16;
    int TOTAL_PIXELS = SIZE * SIZE;

    /** 获取指定坐标的颜色 (ARGB) */
    int getColor(int x, int y);

    /** 设置颜色 */
    void setColor(int x, int y, int color);

    /** 获取原始数组引用，用于批量操作或渲染 */
    int[] getRawData();

    /** 完全填充 */
    void fill(int color);

    /** 从 NBT 读取 */
    void readFromNBT(CompoundTag tag);

    /** 写入 NBT */
    void writeToNBT(CompoundTag tag);
}
