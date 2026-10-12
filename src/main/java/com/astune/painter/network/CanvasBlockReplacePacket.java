package com.astune.painter.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.astune.painter.Painter.MODID;

public record CanvasBlockReplacePacket(BlockPos pos, BlockState state) implements CustomPacketPayload {
    public static final Type<CanvasBlockReplacePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MODID, "canvas_block_replace"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CanvasBlockReplacePacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, CanvasBlockReplacePacket::pos,
                    ByteBufCodecs.fromCodec(BlockState.CODEC), CanvasBlockReplacePacket::state,
                    CanvasBlockReplacePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(CanvasBlockReplacePacket packet, IPayloadContext ctx) {
        ClientPayloadHandlerBridge.handleBlockReplace(packet, ctx);
    }
}
