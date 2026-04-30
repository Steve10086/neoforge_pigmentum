package com.astune.painter.block;

import com.astune.painter.client.CanvasBlockBakedModel;
import com.astune.painter.data.CanvasData;
import com.astune.painter.registry.ModBlockEntities;
import com.astune.painter.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

public class CanvasBlockEntity extends BlockEntity {
    private BlockState mimickedState = Blocks.AIR.defaultBlockState();
    @Nullable
    private CanvasData canvasData;

    public CanvasBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CANVAS.get(), pos, state);
    }

    public BlockState getMimickedState() { return mimickedState; }
    public void setMimickedState(BlockState state) { this.mimickedState = state; setChanged(); }

    @Nullable
    public CanvasData getCanvasData() { return canvasData; }
    public void setCanvasData(@Nullable CanvasData data) { this.canvasData = data; setChanged(); }

    // ---- 持久化 ----
    // CanvasBlockEntity.java
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // 存储原方块状态，标签名必须与读取时一致
        tag.put("mimicked", NbtUtils.writeBlockState(this.mimickedState));
        if (canvasData != null) {
            var result = CanvasData.CODEC.encodeStart(NbtOps.INSTANCE, canvasData);
            result.ifSuccess(t -> tag.put("canvas", t));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.mimickedState = NbtUtils.readBlockState(
                registries.lookupOrThrow(Registries.BLOCK),
                tag.getCompound("mimicked")
        );
        if (tag.contains("canvas")) {
            this.canvasData = CanvasData.CODEC.parse(NbtOps.INSTANCE, tag.get("canvas"))
                    .result().orElse(null);
        } else {
            this.canvasData = null;
        }
    }

    // 客户端同步同样要携带数据（已经实现）
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);  // 确保写入所有数据
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // 新增方法
    @Override
    public ModelData getModelData() {
        return ModelData.builder()
                .with(CanvasBlockBakedModel.BE_PROPERTY, this)
                .build();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
        }
    }
}