package com.astune.painter.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

// 放在任意工具类中，例如 CanvasBlockHelper.java
public class CanvasBlockHelper {
    // 存储原位置 -> BlockEntity 的序列化 NBT
    private static final Map<BlockPos, CompoundTag> PENDING_PISTON_DATA = new HashMap<>();

    public static void saveForPistonMove(BlockPos pos, CompoundTag tag) {
        PENDING_PISTON_DATA.put(pos.immutable(), tag);
    }

    @Nullable
    public static CompoundTag consumePistonData(BlockPos pos) {
        return PENDING_PISTON_DATA.remove(pos);
    }
}