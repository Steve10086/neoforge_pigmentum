package com.astune.painter.network;

import com.astune.painter.api.CanvasData;
import com.astune.painter.api.CanvasDataHolder;
import com.astune.painter.block.CanvasBlock;
import com.astune.painter.block.CanvasBlockEntity;
import com.astune.painter.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
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

    // === 客户端处理器 ===
    public static void handleClient(SyncCanvasPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientCanvasCache.putCanvas(packet.pos, packet.canvasData);
            packet.mimickedState.ifPresentOrElse(
                    s -> ClientCanvasCache.putMimickedState(packet.pos, s),
                    () -> ClientCanvasCache.removeMimickedState(packet.pos)
            );

            if (Minecraft.getInstance().level != null) {
                BlockEntity be = Minecraft.getInstance().level.getBlockEntity(packet.pos);
                if (be instanceof CanvasBlockEntity canvasBE) {
                    packet.mimickedState.ifPresent(canvasBE::setMimickedState);
                    if (be instanceof CanvasDataHolder holder) holder.painter$regenerateTextures(packet.canvasData);
                }
            }

            var level = Minecraft.getInstance().level;
            if (level != null) {
                level.setBlocksDirty(packet.pos, level.getBlockState(packet.pos), level.getBlockState(packet.pos));
            }
        });
    }

    public static void handle(SyncCanvasPacket packet, IPayloadContext ctx) {

    }


}