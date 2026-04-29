package com.astune.painter.data;

import com.astune.painter.api.IPaintLayer;
import com.astune.painter.api.IPixelMatrix;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PaintLayerData implements IPaintLayer {
    private final BlockPos pos;
    private final Direction face;
    private final IPixelMatrix pixels;

    public PaintLayerData(BlockPos pos, Direction face) {
        this(pos, face, new PixelMatrix());
    }

    public PaintLayerData(BlockPos pos, Direction face, IPixelMatrix pixels) {
        this.pos = pos;
        this.face = face;
        this.pixels = pixels;
    }

    @Override public BlockPos getPos() { return pos; }
    @Override public Direction getFace() { return face; }
    @Override public IPixelMatrix getPixels() { return pixels; }

    @Override
    public BlockState getBlockState(Level level) {
        return level.getBlockState(pos);
    }

    @Override
    public BlockEntity getBlockEntity(Level level) {
        return level.getBlockEntity(pos);
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Pos", pos.asLong());
        tag.putInt("Face", face.get3DDataValue());
        CompoundTag pixelsTag = new CompoundTag();
        pixels.writeToNBT(pixelsTag);
        tag.put("Pixels", pixelsTag);
        return tag;
    }

    public static PaintLayerData load(CompoundTag tag) {
        BlockPos pos = BlockPos.of(tag.getLong("Pos"));
        Direction face = Direction.from3DDataValue(tag.getInt("Face"));
        IPixelMatrix pixels = new PixelMatrix();
        if (tag.contains("Pixels")) {
            pixels.readFromNBT(tag.getCompound("Pixels"));
        }
        return new PaintLayerData(pos, face, pixels);
    }

    public static final MapCodec<PaintLayerData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(PaintLayerData::getPos),
            Direction.CODEC.fieldOf("face").forGetter(PaintLayerData::getFace),
            PixelMatrix.CODEC.fieldOf("pixels").forGetter(l -> (PixelMatrix) l.getPixels())
    ).apply(instance, PaintLayerData::new));

}