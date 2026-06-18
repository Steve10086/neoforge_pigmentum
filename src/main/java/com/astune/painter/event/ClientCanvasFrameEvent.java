package com.astune.painter.event;

import com.astune.painter.api.CanvasFace;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.Event;

import java.util.List;
import java.util.Map;

/**
 * Fired once per render frame at the end of {@code PaintInputHandler.onRenderFrame()}
 * when one or more canvas blocks were modified during that frame.
 * <p>
 * Contains the set of affected {@code BlockPos} and their modified {@code CanvasFace}s.
 * Runs on the <b>render thread</b> — subscribers should be lightweight.
 */
public class ClientCanvasFrameEvent extends Event {

    private final Map<BlockPos, List<CanvasFace>> affectedBlocks;

    public ClientCanvasFrameEvent(Map<BlockPos, List<CanvasFace>> affectedBlocks) {
        this.affectedBlocks = Map.copyOf(affectedBlocks);
    }

    /** Unmodifiable map of all blocks and their modified faces this frame. */
    public Map<BlockPos, List<CanvasFace>> getAffectedBlocks() {
        return affectedBlocks;
    }
}
