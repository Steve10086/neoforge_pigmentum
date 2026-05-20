// network/PaintPixelPacket.java
package com.astune.painter.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public record PaintPixelPacket(BlockPos pos, int faceIndex, int pixelX, int pixelY, int color)
        implements CustomPacketPayload {

    public static final Type<PaintPixelPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("painter", "paint_pixel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PaintPixelPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PaintPixelPacket::pos,
            ByteBufCodecs.VAR_INT, PaintPixelPacket::faceIndex,
            ByteBufCodecs.VAR_INT, PaintPixelPacket::pixelX,
            ByteBufCodecs.VAR_INT, PaintPixelPacket::pixelY,
            ByteBufCodecs.INT, PaintPixelPacket::color,
            PaintPixelPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}