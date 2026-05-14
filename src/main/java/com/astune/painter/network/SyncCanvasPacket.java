package com.astune.painter.network;

import com.astune.painter.api.CanvasData;
import com.astune.painter.api.CanvasDataHolder;
import com.astune.painter.block.CanvasBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

import static com.astune.painter.Painter.MODID;

public record SyncCanvasPacket(BlockPos pos, CanvasData canvasData, Optional<BlockState> mimickedState)
        implements CustomPacketPayload {
    public static final Type<SyncCanvasPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "sync_canvas"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncCanvasPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    SyncCanvasPacket::pos,
                    CanvasData.STREAM_CODEC,
                    SyncCanvasPacket::canvasData,
                    ByteBufCodecs.optional(ByteBufCodecs.fromCodec(BlockState.CODEC)),
                    SyncCanvasPacket::mimickedState,
                    SyncCanvasPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncCanvasPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            // 更新画布缓存
            ClientCanvasCache.putCanvas(packet.pos, packet.canvasData);
            // 更新原方块状态缓存
            packet.mimickedState.ifPresentOrElse(
                    state -> ClientCanvasCache.putMimickedState(packet.pos, state),
                    () -> ClientCanvasCache.removeMimickedState(packet.pos)
            );
            // update canvas cache
            BlockEntity be = ctx.player().level().getBlockEntity(packet.pos);
            if(be instanceof CanvasDataHolder cBe) {
                cBe.painter$setCanvasData(packet.canvasData);
                cBe.painter$regenerateTextures(packet.canvasData);
            }
            // 强制客户端重绘
            if (ctx.player() != null && ctx.player().level() != null) {
                var level = ctx.player().level();
                level.setBlocksDirty(packet.pos, level.getBlockState(packet.pos),
                        level.getBlockState(packet.pos));
            }
        });
    }
}