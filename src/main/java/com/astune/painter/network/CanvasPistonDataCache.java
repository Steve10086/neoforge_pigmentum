package com.astune.painter.network;

import com.astune.painter.api.CanvasFace;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CanvasPistonDataCache {
    private static final Map<BlockPos, CompoundTag> DATA = new ConcurrentHashMap<>();


    public static void store(BlockPos newPos, CompoundTag data) {
        DATA.put(newPos.immutable(), data);
    }


    public static CompoundTag consume(BlockPos pos) {
        //System.out.println("[CanvasPistonDataCache] Consuming data from " + pos);
        return DATA.remove(pos);
    }


    public static void clear() { DATA.clear(); }

}