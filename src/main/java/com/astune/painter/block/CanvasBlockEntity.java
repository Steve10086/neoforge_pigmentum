package com.astune.painter.block;

import com.astune.painter.api.CanvasDataHolder;
import com.astune.painter.attachment.ModAttachments;
import com.astune.painter.api.CanvasData;
import com.astune.painter.network.CanvasPistonDataCache;
import com.astune.painter.network.ClientCanvasCache;
import com.astune.painter.network.SyncCanvasPacket;
import com.astune.painter.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class CanvasBlockEntity extends BlockEntity {

    @Nullable
    private BlockState mimickedState;
    public CanvasBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CANVAS_BLOCK_ENTITY.get(), pos, blockState);
    }

    public void setMimickedState(BlockState state) {
        int oldLight = mimickedState != null ? mimickedState.getLightEmission(level, worldPosition) : 0;
        this.mimickedState = state;
        setChanged();
        if (level != null && !level.isClientSide) {
            ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(this);
            //System.out.println("[Server] setMimickedState packet=" + (packet != null) + " pos=" + worldPosition + " state=" + state);
            if (packet != null) {
                var players = ((ServerLevel) level).getChunkSource().chunkMap
                        .getPlayers(new ChunkPos(worldPosition), false);
                //System.out.println("[Server] players count=" + players.size());
                players.forEach(player -> player.connection.send(packet));
            }

            int newLight = state.getLightEmission(level, worldPosition);
            if (oldLight != newLight) {
                System.out.println("check light");
                level.getLightEngine().checkBlock(worldPosition);
            }
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        }

    }


    @Nullable
    public BlockState getMimickedState() {
        return mimickedState;
    }


    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (mimickedState != null) {
            tag.put("mimicked_state", NbtUtils.writeBlockState(mimickedState));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.contains("mimicked_state")) {
            this.mimickedState = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("mimicked_state"));
        }
        if (level != null && level.isClientSide) {
            level.getLightEngine().checkBlock(worldPosition);
            net.minecraft.client.Minecraft.getInstance().levelRenderer
                    .setSectionDirty(worldPosition.getX() >> 4, worldPosition.getY() >> 4, worldPosition.getZ() >> 4);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            CompoundTag movingData = CanvasPistonDataCache.consume(this.worldPosition);
            if (movingData != null) {
                // 恢复所有数据（包括 mimickedState、canvasData）
                this.loadWithComponents(movingData, level.registryAccess());
                // 注意：上面用的是 loadWithComponents，不是 load()
                setChanged();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        CanvasPistonDataCache.consume(this.worldPosition); // 确保不残留
    }
    
    // 用于客户端初始加载整个区块
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);   // 把 mimickedState 写入
        return tag;
    }

    // 用于后续的方块实体更新（可选）
    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

}