package com.astune.painter.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.BiConsumer;

/** Common-safe handoff for payload callbacks whose implementations are client-only. */
public final class ClientPayloadHandlerBridge {
    private static volatile BiConsumer<SyncCanvasPacket, IPayloadContext> syncCanvasHandler = (packet, context) -> {
    };
    private static volatile BiConsumer<CanvasBlockReplacePacket, IPayloadContext> blockReplaceHandler = (packet, context) -> {
    };

    private ClientPayloadHandlerBridge() {
    }

    public static void registerClientHandlers(
            BiConsumer<SyncCanvasPacket, IPayloadContext> syncCanvasHandler,
            BiConsumer<CanvasBlockReplacePacket, IPayloadContext> blockReplaceHandler
    ) {
        ClientPayloadHandlerBridge.syncCanvasHandler = syncCanvasHandler;
        ClientPayloadHandlerBridge.blockReplaceHandler = blockReplaceHandler;
    }

    public static void handleSyncCanvas(SyncCanvasPacket packet, IPayloadContext context) {
        syncCanvasHandler.accept(packet, context);
    }

    public static void handleBlockReplace(CanvasBlockReplacePacket packet, IPayloadContext context) {
        blockReplaceHandler.accept(packet, context);
    }
}
