package com.astune.painter.event;

import com.astune.painter.api.CanvasData;
import com.astune.painter.network.CanvasAction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;

/**
 * Fired on the <b>server thread</b> when a client uploads canvas modifications
 * to the server. Fires after the data is applied but before it is broadcast
 * to other clients.
 */
public class ServerCanvasUpdateEvent extends Event {

    private final BlockPos pos;
    private final CanvasData canvasData;
    private final CanvasAction action;
    private final Player player;

    public ServerCanvasUpdateEvent(BlockPos pos, CanvasData canvasData, CanvasAction action, Player player) {
        this.pos = pos;
        this.canvasData = canvasData;
        this.action = action;
        this.player = player;
    }

    public BlockPos getPos() { return pos; }
    public CanvasData getCanvasData() { return canvasData; }
    public CanvasAction getAction() { return action; }
    public Player getPlayer() { return player; }
}
