package com.astune.painter.network;

import com.astune.painter.data.PaintLayerData;
import com.astune.painter.registry.ModAttachmentTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

public record SyncBlockPaintingsPacket(CompoundTag data) implements CustomPacketPayload {

    public static final Type<SyncBlockPaintingsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("painter", "sync_block_paintings"));

    public static final StreamCodec<FriendlyByteBuf, SyncBlockPaintingsPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, packet) -> packet.write(buf),
                    SyncBlockPaintingsPacket::read
            );

    public void write(FriendlyByteBuf buf) {
        buf.writeNbt(data);
    }

    public static SyncBlockPaintingsPacket read(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new SyncBlockPaintingsPacket(tag != null ? tag : new CompoundTag());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handleClient(SyncBlockPaintingsPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var level = Minecraft.getInstance().level;
            if (level == null) return;

            Map<BlockPos, Map<Direction, PaintLayerData>> map = deserializeBlockPaintings(packet.data());
            if (map.isEmpty()) return;

            // 1. 用第一个位置定位区块，更新 Attachment
            BlockPos firstPos = map.keySet().iterator().next();
            LevelChunk chunk = level.getChunkAt(firstPos);
            chunk.setData(ModAttachmentTypes.BLOCK_PAINTINGS.get(), map);

            // 2. 🔥 关键：强制所有涉及的方块刷新模型数据
            for (BlockPos pos : map.keySet()) {
                BlockState state = level.getBlockState(pos);
                level.sendBlockUpdated(pos, state, state, 3); // 3 = 通知客户端重绘
            }
        });
    }
    private static Map<BlockPos, Map<Direction, PaintLayerData>> deserializeBlockPaintings(CompoundTag tag) {
        Map<BlockPos, Map<Direction, PaintLayerData>> map = new HashMap<>();
        if (tag.contains("BlockPaintings")) {
            ListTag blocksList = tag.getList("BlockPaintings", Tag.TAG_COMPOUND);
            for (int i = 0; i < blocksList.size(); i++) {
                CompoundTag blockTag = blocksList.getCompound(i);
                BlockPos pos = BlockPos.of(blockTag.getLong("Pos"));
                Map<Direction, PaintLayerData> faces = new HashMap<>();
                ListTag facesList = blockTag.getList("Faces", Tag.TAG_COMPOUND);
                for (int j = 0; j < facesList.size(); j++) {
                    CompoundTag faceTag = facesList.getCompound(j);
                    Direction dir = Direction.byName(faceTag.getString("Dir"));
                    PaintLayerData layer = PaintLayerData.load(faceTag.getCompound("Layer"));
                    faces.put(dir, layer);
                }
                map.put(pos, faces);
            }
        }
        return map;
    }
}