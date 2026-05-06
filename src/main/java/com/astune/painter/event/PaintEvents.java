package com.astune.painter.event;

import com.astune.painter.CanvasProperties;
import com.astune.painter.Painter;
import com.astune.painter.block.CanvasBlockEntity;
import com.astune.painter.data.CanvasData;
import com.astune.painter.item.DebugPaintbrush;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Painter.MODID)
public class PaintEvents {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof DebugPaintbrush)) return;

        Level level = event.getLevel();
        if (level.isClientSide) return;

        System.out.println("canvas event");

        BlockPos pos = event.getHitVec().getBlockPos();
        BlockState state = level.getBlockState(pos);
        BooleanProperty prop = CanvasProperties.HAVE_CANVAS;

        //if (!state.hasProperty(prop)) return; // 不可能没有，但防御一下

        BlockEntity be = level.getBlockEntity(pos);
        if (be != null && !(be instanceof CanvasBlockEntity)) {
            event.getEntity().displayClientMessage(
                    net.minecraft.network.chat.Component.literal("这个方块不能附加画布"),
                    true);
            return;
        }

        boolean hasCanvas = state.getValue(prop);
        Direction face = event.getFace();

        if (!hasCanvas) {
            System.out.println("create canvas");
            // 第一次绘制：添加画布属性并手动创建 CanvasBlockEntity
            BlockState newState = state.setValue(CanvasProperties.HAVE_CANVAS, true);
            level.setBlock(pos, newState, 3);
            CanvasBlockEntity canvasBE = new CanvasBlockEntity(pos, newState);
            level.setBlockEntity(canvasBE);
            canvasBE.setCanvasData(CanvasData.createEmpty(face));
            syncAndRefresh(level, pos); // 发送数据包
        } else {
            System.out.println("refresh canvas");
            // 已有画布，编辑像素
            CanvasBlockEntity canvasBE = (CanvasBlockEntity) be;
            CanvasData canvas = canvasBE.getCanvasData();
            if (canvas == null) {
                canvas = CanvasData.createEmpty(face);
            }
            // 简单地将整个面涂白（后续可改为单像素编辑）
            canvas = canvas.withFilledFace(face, 0xFFFFFFFF);
            canvasBE.setCanvasData(canvas);
            syncAndRefresh(level, pos);
        }
    }

    private static void syncAndRefresh(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            BlockState state = level.getBlockState(pos);
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CanvasBlockEntity canvasBE) {
                ClientboundBlockEntityDataPacket pkt = ClientboundBlockEntityDataPacket.create(canvasBE);
                PacketDistributor.sendToPlayersTrackingChunk(serverLevel, new ChunkPos(pos), (CustomPacketPayload) pkt);
            }
            level.setBlocksDirty(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), state);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Level level = (Level) event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(CanvasProperties.HAVE_CANVAS) && state.getValue(CanvasProperties.HAVE_CANVAS)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CanvasBlockEntity) {
                level.removeBlockEntity(pos);
            }
        }
    }
}