package com.astune.painter.block;

import com.astune.painter.data.CanvasData;
import com.astune.painter.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

public class CanvasBlockEntity extends BlockEntity {

    public static final ModelProperty<CanvasData> CANVAS_PROPERTY = new ModelProperty<>();

    @Nullable
    private CanvasData canvasData;

    public CanvasBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CANVAS.get(), pos, state);
    }

    public @Nullable CanvasData getCanvasData() {
        return canvasData;
    }

    public void setCanvasData(@Nullable CanvasData data) {
        this.canvasData = data;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (canvasData != null) {
            tag.put("Canvas", canvasData.toNbt());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Canvas")) {
            canvasData = CanvasData.fromNbt(tag.getCompound("Canvas"));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (canvasData != null) {
            tag.put("Canvas", canvasData.toNbt());
        }
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        super.onDataPacket(net, pkt, registries);
        if (level != null && level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder()
                .with(CANVAS_PROPERTY, canvasData)
                .build();
    }
}