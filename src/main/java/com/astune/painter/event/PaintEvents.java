package com.astune.painter.event;

import com.astune.painter.api.PixelMatrix;
import com.astune.painter.block.CanvasBlockEntity;
import com.astune.painter.data.CanvasData;
import com.astune.painter.item.DebugPaintbrush;
import com.astune.painter.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = "painter")
public class PaintEvents {
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(held.getItem() instanceof DebugPaintbrush)) return; // 自己的画笔

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        Direction face = event.getFace();
        if (face == null || level.isClientSide) return;

        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.CANVAS.get())) {
            // 已为画布方块，更新画布
            if (level.getBlockEntity(pos) instanceof CanvasBlockEntity cbe) {
                CanvasData existing = cbe.getCanvasData();
                PixelMatrix pixels = existing != null ? existing.pixels() : new PixelMatrix();
                // 示例：将点击处的所有像素设为白色（实际应计算命中像素）
                pixels.fill(0xFFFFFFFF); // 简单全白
                cbe.setCanvasData(new CanvasData(face, pixels));
            }
        } else {
            // 替换为代理方块
            BlockState oldState = level.getBlockState(pos);
            level.setBlock(pos, ModBlocks.CANVAS.get().defaultBlockState(), 3);
            if (level.getBlockEntity(pos) instanceof CanvasBlockEntity cbe) {
                cbe.setMimickedState(oldState);
                PixelMatrix pixels = new PixelMatrix();
                pixels.fill(0xFFFFFFFF); // 测试用
                cbe.setCanvasData(new CanvasData(face, pixels));
            }
        }
        // 1. 数据包
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CanvasBlockEntity cbe) {
            Packet<?> packet = cbe.getUpdatePacket();
            if (packet != null && level instanceof ServerLevel serverLevel) {
                for (ServerPlayer p : serverLevel.getChunkSource().chunkMap.getPlayers(new ChunkPos(pos), false)) {
                    p.connection.send(packet);
                }
            }
        }
        // 2. 刷新
        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
        level.getLightEngine().checkBlock(pos);

        event.setCanceled(true);
    }
}