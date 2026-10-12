package com.astune.painter.network;

import com.astune.painter.api.CanvasData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

import static com.astune.painter.Painter.MODID;

// network/SyncCanvasPacket.java
public record SyncCanvasPacket(BlockPos pos, CanvasData canvasData,
                               Optional<BlockState> mimickedState, boolean createIfAbsent)
        implements CustomPacketPayload {

    public static final Type<SyncCanvasPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MODID, "sync_canvas"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncCanvasPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SyncCanvasPacket::pos,
                    CanvasData.STREAM_CODEC, SyncCanvasPacket::canvasData,
                    ByteBufCodecs.optional(ByteBufCodecs.fromCodec(BlockState.CODEC)), SyncCanvasPacket::mimickedState,
                    ByteBufCodecs.BOOL, SyncCanvasPacket::createIfAbsent,
                    SyncCanvasPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handleClient(SyncCanvasPacket packet, IPayloadContext ctx) {
        ClientPayloadHandlerBridge.handleSyncCanvas(packet, ctx);
    }


}
