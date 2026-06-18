package com.astune.painter.event;

import com.astune.painter.api.CanvasFace;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.Event;

import java.util.List;
import java.util.Map;

/**
 * Fired on the <b>game thread</b> via {@code ClientTickEvent.Post} when canvas data
 * has been modified since the last tick. Accumulates changes across all render frames
 * within a single tick.
 * <p>
 * Use this event for heavier analysis (e.g. formation pattern matching) instead of
 * {@link ClientCanvasFrameEvent} which runs on the render thread.
 */
public class ClientCanvasTickEvent extends Event {

    private final Map<BlockPos, List<CanvasFace>> affectedBlocks;

    public ClientCanvasTickEvent(Map<BlockPos, List<CanvasFace>> affectedBlocks) {
        this.affectedBlocks = Map.copyOf(affectedBlocks);
    }

    /** Unmodifiable map of all blocks and their modified faces since the last tick. */
    public Map<BlockPos, List<CanvasFace>> getAffectedBlocks() {
        return affectedBlocks;
    }
}
