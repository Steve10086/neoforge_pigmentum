package com.astune.painter.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CanvasPistonDataCache {
    private static final Map<BlockPos, CompoundTag> DATA = new ConcurrentHashMap<>();

    public static void store(BlockPos newPos, CompoundTag data) {
        System.out.println("[CanvasPistonDataCache] Saving data to " + newPos);
        DATA.put(newPos.immutable(), data);
    }

    public static CompoundTag consume(BlockPos pos) {
        System.out.println("[CanvasPistonDataCache] Loading data from " + pos);
        return DATA.remove(pos);
    }

    public static void clear() { DATA.clear(); }

}