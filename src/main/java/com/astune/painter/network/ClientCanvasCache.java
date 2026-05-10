package com.astune.painter.network;

import com.astune.painter.api.CanvasData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class ClientCanvasCache {
    private static final Map<BlockPos, CanvasData> CANVAS_MAP = new ConcurrentHashMap<>();
    private static final Map<BlockPos, BlockState> STATE_MAP = new ConcurrentHashMap<>();

    public static void putCanvas(BlockPos pos, CanvasData data) {
        CANVAS_MAP.put(pos, data);
    }

    public static CanvasData getCanvas(BlockPos pos) {
        return CANVAS_MAP.get(pos);
    }

    public static void removeCanvas(BlockPos pos) {
        CANVAS_MAP.remove(pos);
    }

    public static void putMimickedState(BlockPos pos, BlockState state) {
        STATE_MAP.put(pos, state);
    }

    public static BlockState getMimickedState(BlockPos pos) {
        return STATE_MAP.get(pos);
    }

    public static void removeMimickedState(BlockPos pos) {
        STATE_MAP.remove(pos);
    }
}