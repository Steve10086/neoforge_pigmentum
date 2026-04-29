package com.astune.painter.capability;

import com.astune.painter.data.PaintLayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class BlockPaintingsSerializer implements IAttachmentSerializer<CompoundTag, Map<BlockPos, Map<Direction, PaintLayerData>>> {

    @Override
    public Map<BlockPos, Map<Direction, PaintLayerData>> read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
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

    @Override
    @Nullable
    public CompoundTag write(Map<BlockPos, Map<Direction, PaintLayerData>> data, HolderLookup.Provider provider) {
        CompoundTag root = new CompoundTag();
        ListTag blocksList = new ListTag();
        for (Map.Entry<BlockPos, Map<Direction, PaintLayerData>> blockEntry : data.entrySet()) {
            CompoundTag blockTag = new CompoundTag();
            blockTag.putLong("Pos", blockEntry.getKey().asLong());
            ListTag facesList = new ListTag();
            for (Map.Entry<Direction, PaintLayerData> faceEntry : blockEntry.getValue().entrySet()) {
                CompoundTag faceTag = new CompoundTag();
                faceTag.putString("Dir", faceEntry.getKey().getSerializedName());
                faceTag.put("Layer", faceEntry.getValue().save());
                facesList.add(faceTag);
            }
            blockTag.put("Faces", facesList);
            blocksList.add(blockTag);
        }
        root.put("BlockPaintings", blocksList);
        return root;
    }
}
