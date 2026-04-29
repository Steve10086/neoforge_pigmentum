package com.astune.painter.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkHandler.class);

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar2 = event.registrar("painter").versioned("1");
        registrar2.playToClient(
                SyncBlockPaintingsPacket.TYPE,
                SyncBlockPaintingsPacket.STREAM_CODEC,
                SyncBlockPaintingsPacket::handleClient
        );
        LOGGER.info("[Painter] Network handler registered successfully");
    }

}