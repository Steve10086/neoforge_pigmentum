package com.astune.painter.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.nbt.CompoundTag;

/**
 * 附着在方块单一表面上的绘画图层。
 * 提供对父级方块的完整访问，并支持子类化扩展行为。
 */
public interface IPaintLayer {
    BlockPos getPos();
    Direction getFace();
    IPixelMatrix getPixels();

    /** 获取父级方块状态，需要传入世界实例 */
    BlockState getBlockState(Level level);
    BlockEntity getBlockEntity(Level level);

    /** 序列化到 NBT */
    CompoundTag save();
}
