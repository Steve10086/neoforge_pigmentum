package com.astune.painter.api;

import com.astune.painter.data.PaintLayerData;
import com.astune.painter.data.PixelMatrix;
import com.astune.painter.network.NetworkHandler;
import com.astune.painter.network.SyncBlockPaintingsPacket;
import com.astune.painter.registry.ModAttachmentTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;

public class PaintingManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaintingManager.class);

    /**
     * 在指定方块面上绘制一个像素（自动创建画层）。
     * @return 绘制后的画层，如果失败返回 null。
     */
    public static PaintLayerData paintPixel(Level level, BlockPos pos, Direction face, int x, int y, int color) {
        PaintLayerData layer = getOrCreateLayer(level, pos, face);
        if (layer != null) {
            layer.getPixels().setColor(x, y, color);
            markDirty(level, pos);  // 🔥 触发同步+刷新
            System.out.println("paint!");
        }
        return layer;
    }

    /**
     * 擦除指定方块面的整个画层。
     */
    public static boolean eraseLayer(Level level, BlockPos pos, Direction face) {
        LevelChunk chunk = level.getChunkAt(pos);
        Map<BlockPos, Map<Direction, PaintLayerData>> all = chunk.getData(ModAttachmentTypes.BLOCK_PAINTINGS.get());
        Map<Direction, PaintLayerData> faces = all.get(pos);
        if (faces != null) {
            PaintLayerData removed = faces.remove(face);
            if (removed != null) {
                if (faces.isEmpty()) {
                    all.remove(pos);
                }
                markDirty(level, pos);  // 🔥 触发同步+刷新
                return true;
            }
        }
        return false;
    }

    /**
     * 获取指定方块面的画层（可能为 null）。
     */
    @Nullable
    public static PaintLayerData getLayer(Level level, BlockPos pos, Direction face) {
        LevelChunk chunk = level.getChunkAt(pos);
        Map<BlockPos, Map<Direction, PaintLayerData>> all = chunk.getData(ModAttachmentTypes.BLOCK_PAINTINGS.get());
        Map<Direction, PaintLayerData> faces = all.get(pos);
        return faces != null ? faces.get(face) : null;
    }

    /**
     * 获取指定方块所有面的画层（可能为空 Map）。
     */
    public static Map<Direction, PaintLayerData> getAllLayers(Level level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        Map<BlockPos, Map<Direction, PaintLayerData>> all = chunk.getData(ModAttachmentTypes.BLOCK_PAINTINGS.get());
        return all.getOrDefault(pos, Collections.emptyMap());
    }

    // ---------- 内部工具方法 ---------- //

    private static PaintLayerData getOrCreateLayer(Level level, BlockPos pos, Direction face) {
        LevelChunk chunk = level.getChunkAt(pos);
        Map<BlockPos, Map<Direction, PaintLayerData>> all = chunk.getData(ModAttachmentTypes.BLOCK_PAINTINGS.get());
        Map<Direction, PaintLayerData> faces = all.computeIfAbsent(pos, k -> new HashMap<>());
        PaintLayerData layer = faces.get(face);
        if (layer == null) {
            layer = new PaintLayerData(pos, face, new PixelMatrix());
            faces.put(face, layer);
        }
        return layer;
    }

    /**
     * 标记脏，触发保存和客户端同步。
     */
    private static void markDirty(Level level, BlockPos pos) {
        if (level.isClientSide) return;

        // 强制客户端重绘该方块
        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, 3);

        if (level instanceof ServerLevel serverLevel) {
            LevelChunk chunk = level.getChunkAt(pos);
            Map<BlockPos, Map<Direction, PaintLayerData>> all = chunk.getData(ModAttachmentTypes.BLOCK_PAINTINGS.get());
            chunk.setData(ModAttachmentTypes.BLOCK_PAINTINGS.get(), all);
            chunk.setUnsaved(true);
            CompoundTag root = serializeBlockPaintings(all);

            PacketDistributor.sendToPlayersTrackingChunk(serverLevel, new ChunkPos(pos), new SyncBlockPaintingsPacket(root));
        }
        LOGGER.debug("triggered webPacket");
    }

    // 序列化工具（与网络包配合）
    private static CompoundTag serializeBlockPaintings(Map<BlockPos, Map<Direction, PaintLayerData>> all) {
        CompoundTag root = new CompoundTag();
        ListTag blocks = new ListTag();
        all.forEach((pos, faces) -> {
            CompoundTag blockTag = new CompoundTag();
            blockTag.putLong("Pos", pos.asLong());
            ListTag faceList = new ListTag();
            faces.forEach((dir, layer) -> {
                CompoundTag faceTag = new CompoundTag();
                faceTag.putString("Dir", dir.getSerializedName());
                faceTag.put("Layer", layer.save());
                faceList.add(faceTag);
            });
            blockTag.put("Faces", faceList);
            blocks.add(blockTag);
        });
        root.put("BlockPaintings", blocks);
        return root;
    }
}