package com.astune.painter.event;

import com.astune.painter.api.CanvasData;
import com.astune.painter.api.CanvasDataHolder;
import com.astune.painter.api.CanvasFace;
import com.astune.painter.api.PixelMatrix;
import com.astune.painter.block.CanvasBlock;
import com.astune.painter.block.CanvasBlockEntity;
import com.astune.painter.item.DebugPaintbrush;
import com.astune.painter.network.SyncCanvasPacket;
import com.astune.painter.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Optional;

@EventBusSubscriber(modid = "painter")
public class PaintEvents {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof DebugPaintbrush)) return;

        Level level = event.getLevel();
        Player player = event.getEntity();
        BlockPos pos = event.getHitVec().getBlockPos();
        BlockState state = level.getBlockState(pos);
        Direction face = event.getFace();
        Vec3 hitLoc = event.getHitVec().getLocation();

        if (level.isClientSide) return;

        // 计算点击表面对应的画布面
        CanvasFace newFace = calculateCanvasFace(level, pos, state, hitLoc, face);
        if (newFace == null) return;

        // 当前仅测试：面设为全白
        newFace.pixels().fillWhite();  // 假设 PixelMatrix 有 fillWhite()

        BlockEntity be = level.getBlockEntity(pos);

        // 情况1：方块已有实体（原始实体或已是 CanvasBlockEntity）
        if (be instanceof CanvasDataHolder holder) {
            CanvasData canvas = holder.painter$getCanvasData();
            if (canvas == null) canvas = CanvasData.empty();
            canvas.setFace(newFace);                // 添加或替换
            holder.painter$setCanvasData(canvas);
            be.setChanged();

            // 同步网络包
            BlockState mimicked = (be instanceof CanvasBlockEntity canvasBE) ? canvasBE.getMimickedState() : null;
            syncCanvas(level, pos, canvas, mimicked);
        }
        // 情况2：无实体方块 → 替换为 CanvasBlock
        else {
            BlockState originalState = state;
            CanvasBlock canvasBlock = originalState.isSolidRender(level, pos)
                    ? ModBlocks.CANVAS_OCCLUSION.get()
                    : ModBlocks.CANVAS_NO_OCCLUSION.get();
            //System.out.println("Paint a " + canvasBlock);
            level.setBlock(pos, canvasBlock.defaultBlockState(), 3);
            BlockEntity newBe = level.getBlockEntity(pos);
            if (newBe instanceof CanvasBlockEntity canvasBE) {
                canvasBE.setMimickedState(originalState);
                CanvasData canvas = CanvasData.empty();
                if (newBe instanceof CanvasDataHolder holder) holder.painter$setCanvasData(canvas);
                canvas.setFace(newFace);
                canvasBE.setChanged();
                syncCanvas(level, pos, canvas, originalState);
            }
        }
    }

    /**
     * 根据命中点和方块形状，计算出被点击表面的 CanvasFace 描述。
     *
     * @param level  世界
     * @param pos    方块坐标
     * @param state  方块状态
     * @param hitLoc 击中点世界坐标
     * @param face   点击的大致面方向
     * @return 画布面，如果无法确定则返回 null
     */
    @Nullable
    private static CanvasFace calculateCanvasFace(Level level, BlockPos pos, BlockState state,
                                                  Vec3 hitLoc, Direction face) {
        VoxelShape shape = state.getShape(level, pos);
        if (shape.isEmpty()) return null;

        double localX = hitLoc.x - pos.getX();
        double localY = hitLoc.y - pos.getY();
        double localZ = hitLoc.z - pos.getZ();

        for (AABB aabb : shape.toAabbs()) {
            switch (face) {
                case UP:
                    if (Math.abs(aabb.maxY - localY) < 0.001) {
                        double width = (aabb.maxX - aabb.minX);
                        double height = (aabb.maxZ - aabb.minZ);
                        int pixelW = Math.max(1, (int)(width * 16.0 + 0.5));
                        int pixelH = Math.max(1, (int)(height * 16.0 + 0.5));
                        Vec3 offset = new Vec3(
                                aabb.getCenter().x - 0.5,
                                aabb.maxY - 0.5,
                                aabb.getCenter().z - 0.5
                        );
                        return new CanvasFace(face, offset, new PixelMatrix(pixelW, pixelH));
                    }
                    break;
                case DOWN:
                    if (Math.abs(aabb.minY - localY) < 0.001) {
                        double width = (aabb.maxX - aabb.minX);
                        double height = (aabb.maxZ - aabb.minZ);
                        int pixelW = Math.max(1, (int)(width * 16.0 + 0.5));
                        int pixelH = Math.max(1, (int)(height * 16.0 + 0.5));
                        Vec3 offset = new Vec3(
                                aabb.getCenter().x - 0.5,
                                aabb.minY - 0.5,
                                aabb.getCenter().z - 0.5
                        );
                        return new CanvasFace(face, offset, new PixelMatrix(pixelW, pixelH));
                    }
                    break;
                case NORTH:
                    if (Math.abs(aabb.minZ - localZ) < 0.001) {
                        double width = (aabb.maxX - aabb.minX);
                        double height = (aabb.maxY - aabb.minY);
                        int pixelW = Math.max(1, (int)(width * 16.0 + 0.5));
                        int pixelH = Math.max(1, (int)(height * 16.0 + 0.5));
                        Vec3 offset = new Vec3(
                                aabb.getCenter().x - 0.5,
                                aabb.getCenter().y - 0.5,
                                aabb.minZ - 0.5
                        );
                        return new CanvasFace(face, offset, new PixelMatrix(pixelW, pixelH));
                    }
                    break;
                case SOUTH:
                    if (Math.abs(aabb.maxZ - localZ) < 0.001) {
                        double width = (aabb.maxX - aabb.minX);
                        double height = (aabb.maxY - aabb.minY);
                        int pixelW = Math.max(1, (int)(width * 16.0 + 0.5));
                        int pixelH = Math.max(1, (int)(height * 16.0 + 0.5));
                        Vec3 offset = new Vec3(
                                aabb.getCenter().x - 0.5,
                                aabb.getCenter().y - 0.5,
                                aabb.maxZ - 0.5
                        );
                        return new CanvasFace(face, offset, new PixelMatrix(pixelW, pixelH));
                    }
                    break;
                case WEST:
                    if (Math.abs(aabb.minX - localX) < 0.001) {
                        double width = (aabb.maxZ - aabb.minZ);
                        double height = (aabb.maxY - aabb.minY);
                        int pixelW = Math.max(1, (int)(width * 16.0 + 0.5));
                        int pixelH = Math.max(1, (int)(height * 16.0 + 0.5));
                        Vec3 offset = new Vec3(
                                aabb.minX - 0.5,
                                aabb.getCenter().y - 0.5,
                                aabb.getCenter().z - 0.5
                        );
                        return new CanvasFace(face, offset, new PixelMatrix(pixelW, pixelH));
                    }
                    break;
                case EAST:
                    if (Math.abs(aabb.maxX - localX) < 0.001) {
                        double width = (aabb.maxZ - aabb.minZ);
                        double height = (aabb.maxY - aabb.minY);
                        int pixelW = Math.max(1, (int)(width * 16.0 + 0.5));
                        int pixelH = Math.max(1, (int)(height * 16.0 + 0.5));
                        Vec3 offset = new Vec3(
                                aabb.maxX - 0.5,
                                aabb.getCenter().y - 0.5,
                                aabb.getCenter().z - 0.5
                        );
                        return new CanvasFace(face, offset, new PixelMatrix(pixelW, pixelH));
                    }
                    break;
            }
        }
        return null;
    }

    private static void syncCanvas(Level level, BlockPos pos, CanvasData data, @Nullable BlockState mimickedState) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        PacketDistributor.sendToPlayersTrackingChunk(
                serverLevel,
                new ChunkPos(pos),
                new SyncCanvasPacket(pos, data, Optional.ofNullable(mimickedState))
        );
        // 强制客户端重绘
        level.setBlocksDirty(pos, level.getBlockState(pos), level.getBlockState(pos));
    }
}