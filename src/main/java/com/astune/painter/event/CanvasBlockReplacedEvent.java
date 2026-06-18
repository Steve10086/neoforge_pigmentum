package com.astune.painter.event;

import com.astune.painter.block.CanvasBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;

/**
 * Fired when a normal block is replaced with a {@code CanvasBlock} — i.e. the first
 * time a player paints on a non-canvas block. Fires on both client and server.
 */
public class CanvasBlockReplacedEvent extends Event {

    private final BlockPos pos;
    private final BlockState originalState;
    private final CanvasBlockEntity canvasBE;

    public CanvasBlockReplacedEvent(BlockPos pos, BlockState originalState, CanvasBlockEntity canvasBE) {
        this.pos = pos;
        this.originalState = originalState;
        this.canvasBE = canvasBE;
    }

    public BlockPos getPos() { return pos; }
    public BlockState getOriginalState() { return originalState; }
    public CanvasBlockEntity getCanvasBE() { return canvasBE; }
}
