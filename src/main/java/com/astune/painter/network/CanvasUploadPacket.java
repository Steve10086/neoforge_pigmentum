// network/CanvasUploadPacket.java
package com.astune.painter.network;

import com.astune.painter.api.CanvasData;
import com.astune.painter.api.CanvasDataHolder;
import com.astune.painter.block.CanvasBlock;
import com.astune.painter.block.CanvasBlockEntity;
import com.astune.painter.registry.ModBlocks;
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

import static com.astune.painter.network.CanvasAction.ACTION_CODEC;
import static com.astune.painter.network.CanvasAction.ADD_CREATION;

public record CanvasUploadPacket(BlockPos pos, CanvasData canvasData, CanvasAction action)
        implements CustomPacketPayload {

    public static final Type<CanvasUploadPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("painter", "canvas_upload"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CanvasUploadPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, CanvasUploadPacket::pos,
                    CanvasData.STREAM_CODEC, CanvasUploadPacket::canvasData,
                    ACTION_CODEC, CanvasUploadPacket::action,
                    CanvasUploadPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    // === 服务端处理器 ===
    public static void handleServer(CanvasUploadPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            Level level = player.level();
            BlockPos pos = packet.pos();

            switch (packet.action){
                case ADD_CREATION -> {
                    BlockState state = level.getBlockState(pos);
                    if (level.getBlockEntity(pos) instanceof CanvasDataHolder) return; // 已存在

                    CanvasBlock canvasBlock = state.isSolidRender(level, pos)
                            ? ModBlocks.CANVAS_OCCLUSION.get()
                            : ModBlocks.CANVAS_NO_OCCLUSION.get();

                    level.setBlock(pos, canvasBlock.defaultBlockState(), 3);
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof CanvasBlockEntity canvasBE) {
                        canvasBE.setMimickedState(state);
                    }
                }
                case ADD -> {
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof CanvasBlockEntity canvasBE) {
                        CanvasData canvas = packet.canvasData() != null ? packet.canvasData() : CanvasData.empty();
                        if (be instanceof CanvasDataHolder holder) {
                            if (holder.painter$getCanvasData() != null && canvas.getVersion() > holder.painter$getCanvasData().getVersion()){
                                holder.painter$setCanvasData(canvas);
                                holder.painter$regenerateTextures(canvas);
                            }
                        }
                        canvasBE.setChanged();
                    }
                    break;
                }
                case ERASE -> {return;}
                case null, default -> {return;}
            }

            if (packet.action == ADD_CREATION) {
                // 请求创建画布
                BlockState state = level.getBlockState(pos);
                if (level.getBlockEntity(pos) instanceof CanvasDataHolder) return; // 已存在

                CanvasBlock canvasBlock = state.isSolidRender(level, pos)
                        ? ModBlocks.CANVAS_OCCLUSION.get()
                        : ModBlocks.CANVAS_NO_OCCLUSION.get();

                level.setBlock(pos, canvasBlock.defaultBlockState(), 3);
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof CanvasBlockEntity canvasBE) {
                    canvasBE.setMimickedState(state);
                    CanvasData canvas = packet.canvasData() != null ? packet.canvasData() : CanvasData.empty();
                    if (be instanceof CanvasDataHolder holder) {
                        holder.painter$setCanvasData(canvas);
                        holder.painter$regenerateTextures(canvas);
                    }
                    canvasBE.setChanged();
                }
            } else {
                // 普通同步：覆盖画布数据
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof CanvasDataHolder holder) {
                    holder.painter$setCanvasData(packet.canvasData());
                    holder.painter$regenerateTextures(packet.canvasData());
                    be.setChanged();
                }
            }

            // 同步回所有客户端
            syncToClients(level, pos, packet.canvasData(), level.getBlockState(pos));
        });
    }

    private static void syncToClients(Level level, BlockPos pos, CanvasData data, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        BlockEntity be = level.getBlockEntity(pos);
        BlockState mimicked = be instanceof CanvasBlockEntity canvasBE ? canvasBE.getMimickedState() : null;
        PacketDistributor.sendToPlayersTrackingChunk(
                serverLevel, new ChunkPos(pos),
                new SyncCanvasPacket(pos, data, Optional.ofNullable(mimicked), false)
        );
        level.setBlocksDirty(pos, state, state);
    }
}