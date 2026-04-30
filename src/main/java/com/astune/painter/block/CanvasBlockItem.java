package com.astune.painter.block;

import com.astune.painter.data.CanvasData;
import com.astune.painter.registry.ModBlocks;
import com.astune.painter.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

public class CanvasBlockItem extends BlockItem {
    public CanvasBlockItem(Block block, Properties props) {
        super(block, props);
    }

    // 修改 place 方法，在设置完 BlockEntity 后添加同步
    @Override
    public InteractionResult place(BlockPlaceContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        ItemStack stack = ctx.getItemInHand();

        InteractionResult result = super.place(ctx);
        if (!result.consumesAction() || level.isClientSide) return result;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CanvasBlockEntity cbe) {
            BlockState mimicked = stack.getOrDefault(ModDataComponents.BLOCK_STATE.get(), Blocks.AIR.defaultBlockState());
            cbe.setMimickedState(mimicked);
            CanvasData canvas = stack.get(ModDataComponents.CANVAS.get());
            if (canvas != null) {
                cbe.setCanvasData(canvas);
            }
            if (!level.isClientSide) {
                // 1. 先发送 BlockEntity 数据包
                Packet<?> packet = cbe.getUpdatePacket();
                if (packet != null && level instanceof ServerLevel serverLevel) {
                    for (ServerPlayer player : serverLevel.getChunkSource().chunkMap.getPlayers(new ChunkPos(pos), false)) {
                        player.connection.send(packet);
                    }
                }
                // 2. 再通知方块更新（触发模型数据刷新）
                BlockState canvasState = level.getBlockState(pos);
                level.sendBlockUpdated(pos, canvasState, canvasState, 3);
            }
        }
        return InteractionResult.CONSUME;
    }
}