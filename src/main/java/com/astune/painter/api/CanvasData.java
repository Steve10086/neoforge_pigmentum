package com.astune.painter.api;

import com.astune.painter.block.CanvasBlock;
import com.astune.painter.block.CanvasBlockEntity;
import com.astune.painter.event.CanvasBlockReplacedEvent;
import com.astune.painter.registry.ModBlocks;
import com.astune.painter.util.CanvasBlacklist;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CanvasData {
    private long version = 0;
    private final List<CanvasFace> faces;
    public long getVersion() { return version; }
    public void incrementVersion() { this.version++; }

    public static final Codec<CanvasData> CODEC =
            Codec.list(CanvasFace.CODEC).xmap(CanvasData::new, CanvasData::faces);

    public List<CanvasFace> faces() {
        return faces;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, CanvasData> STREAM_CODEC =
            StreamCodec.of(
                    (buf, data) -> {
                        buf.writeVarInt(data.faces.size());
                        for (CanvasFace face : data.faces) {
                            CanvasFace.STREAM_CODEC.encode(buf, face);
                        }
                    },
                    buf -> {
                        int size = buf.readVarInt();
                        List<CanvasFace> faces = new ArrayList<>(size);
                        for (int i = 0; i < size; i++) {
                            faces.add(CanvasFace.STREAM_CODEC.decode(buf));
                        }
                        return new CanvasData(faces);
                    }
            );

    public CanvasData(List<CanvasFace> faces) {
        this.faces = new ArrayList<>(faces);
    }

    public static CanvasData empty() {
        return new CanvasData(new ArrayList<>());
    }

    /**
     * 添加或替换一个画布面（如果相同表面已存在则替换）。
     *
     * @return
     */
    public CanvasFace addOrGetFace(CanvasFace newFace) {
        for (int i = 0; i < faces.size(); i++) {
            if (faces.get(i).isSameSurface(newFace)) {
                return faces.get(i);
            }
        }
        //System.out.println("adding new face!");
        faces.add(newFace);
        return newFace;
    }

    public CanvasFace tryGetFace(CanvasFace newFace) {
        for (int i = 0; i < faces.size(); i++) {
            if (faces.get(i).isSameSurface(newFace)) {
                return faces.get(i);
            }
        }
        return null;
    }

    public List<CanvasFace> getFaceAtHit(BlockPos pos, Vec3 hitLoc) {
        return getFaceAtHit(pos, hitLoc, 0.01);
    }

    public CanvasFace getFaceAtHit(BlockPos pos, BlockHitResult hitResult) {
        Direction hitFace = hitResult.getDirection();
        Vec3 hitLoc = hitResult.getLocation();
        Vec3 localHit = hitLoc.subtract(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        for (CanvasFace face : faces) {
            if (face.primaryFace().equals(hitFace)){
                Vec3 c0 = face.corner0();
                Vec3 c1 = face.corner1();
                Vec3 c3 = face.corner3();

                Vec3 sideW = c1.subtract(c0);
                Vec3 sideH = c3.subtract(c0);

                // 提前缓存长度倒数
                double invWLen = 1.0 / sideW.length();
                double invHLen = 1.0 / sideH.length();

                // 归一化向量
                Vec3 normW = sideW.scale(invWLen);
                Vec3 normH = sideH.scale(invHLen);

                // 命中点相对面原点的偏移
                Vec3 relative = localHit.subtract(c0);

                // 计算 UV 坐标
                double u = relative.dot(normW) * invWLen;
                double v = relative.dot(normH) * invHLen;

                // 检查点是否在面内
                if (u >= 0 - 0.001 && u <= 1 + 0.001 && v >= 0 - 0.001 && v <= 1 + 0.001) {
                    // 检查点是否在平面上
                    Vec3 projected = c0.add(sideW.scale(u)).add(sideH.scale(v));
                    if (localHit.distanceToSqr(projected) < 0.001) {
                        //System.out.println("Hit at " + u + ", "+ v + "on face of size " + face.pixels().getWidth() + "," + face.pixels().getHeight());
                        return face;
                    }
                }
            }
        }
        return null;
    }

    public List<CanvasFace> getFaceAtHit(BlockPos pos, Vec3 hitLoc, double offset) {
        Vec3 localHit = hitLoc.subtract(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        List<CanvasFace> hit = new ArrayList<>();
        for (CanvasFace face : faces) {
            Vec3 c0 = face.corner0();
            Vec3 c1 = face.corner1();
            Vec3 c3 = face.corner3();

            Vec3 sideW = c1.subtract(c0);
            Vec3 sideH = c3.subtract(c0);

            // 提前缓存长度倒数
            double invWLen = 1.0 / sideW.length();
            double invHLen = 1.0 / sideH.length();

            // 归一化向量
            Vec3 normW = sideW.scale(invWLen);
            Vec3 normH = sideH.scale(invHLen);

            // 命中点相对面原点的偏移
            Vec3 relative = localHit.subtract(c0);

            // 计算 UV 坐标
            double u = relative.dot(normW) * invWLen;
            double v = relative.dot(normH) * invHLen;

            // 检查点是否在面内
            if (u >= 0 - offset && u <= 1 + offset && v >= 0 - offset && v <= 1 + offset) {
                // 检查点是否在平面上
                Vec3 projected = c0.add(sideW.scale(u)).add(sideH.scale(v));
                if (localHit.distanceToSqr(projected) < offset) {
                    //System.out.println("Hit at " + u + ", "+ v + "on face of size " + face.pixels().getWidth() + "," + face.pixels().getHeight());
                    hit.add(face);
                }
            }
        }
        return hit;
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
    public static CanvasFace calculateCanvasFace(Level level, BlockPos pos, BlockState state,
                                                  Vec3 hitLoc, Direction face) {
        VoxelShape shape = state.getShape(level, pos);
        if (shape.isEmpty()) return null;

        double localX = hitLoc.x - pos.getX();
        double localY = hitLoc.y - pos.getY();
        double localZ = hitLoc.z - pos.getZ();

        for (AABB aabb : shape.toAabbs()) {
            //System.out.println("looking at " + aabb + "on " + shape.toAabbs().size() + " boxes");
            double width = 0, height = 0, checkVal;
            double cx = aabb.getCenter().x - 0.5, cy = aabb.getCenter().y - 0.5, cz = aabb.getCenter().z - 0.5;
            Vec3 offset = null;

            switch (face) {
                case UP:
                    if (Math.abs(aabb.maxY - localY) >= 0.001) break;
                    width = aabb.maxX - aabb.minX; height = aabb.maxZ - aabb.minZ;
                    offset = new Vec3(cx, aabb.maxY - 0.5, cz);
                    break;
                case DOWN:
                    if (Math.abs(aabb.minY - localY) >= 0.001) break;
                    width = aabb.maxX - aabb.minX; height = aabb.maxZ - aabb.minZ;
                    offset = new Vec3(cx, aabb.minY - 0.5, cz);
                    break;
                case NORTH:
                    if (Math.abs(aabb.minZ - localZ) >= 0.001) break;
                    width = aabb.maxX - aabb.minX; height = aabb.maxY - aabb.minY;
                    offset = new Vec3(cx, cy, aabb.minZ - 0.5);
                    break;
                case SOUTH:
                    if (Math.abs(aabb.maxZ - localZ) >= 0.001) break;
                    width = aabb.maxX - aabb.minX; height = aabb.maxY - aabb.minY;
                    offset = new Vec3(cx, cy, aabb.maxZ - 0.5);
                    break;
                case WEST:
                    if (Math.abs(aabb.minX - localX) >= 0.001) break;
                    width = aabb.maxZ - aabb.minZ; height = aabb.maxY - aabb.minY;
                    offset = new Vec3(aabb.minX - 0.5, cy, cz);
                    break;
                case EAST:
                    if (Math.abs(aabb.maxX - localX) >= 0.001) break;
                    width = aabb.maxZ - aabb.minZ; height = aabb.maxY - aabb.minY;
                    offset = new Vec3(aabb.maxX - 0.5, cy, cz);
                    break;
                default: return null;
            }

            // 2. 命中点已在平面上，检查是否在面的实际区域内
            if (offset != null) {
                // 计算命中点相对于面左下角的局部坐标 (u, v)，范围应为 [0, 1]
                double u = 0, v = 0;
                switch (face) {
                    case UP:
                    case DOWN:
                        u = (localX - aabb.minX) / width;
                        v = (localZ - aabb.minZ) / height;
                        break;
                    case NORTH:
                    case SOUTH:
                        u = (localX - aabb.minX) / width;
                        v = (localY - aabb.minY) / height;
                        break;
                    case WEST:
                    case EAST:
                        u = (localZ - aabb.minZ) / width;
                        v = (localY - aabb.minY) / height;
                        break;
                }

                // 注意：对于某些方向，u 或 v 的方向可能需要反转，但因为我们用局部坐标相对于 min 点，且 width/height 是绝对值，所以 u,v 范围是 [0,1] 即命中点在内部。
                if (u >= 0 && u <= 1 && v >= 0 && v <= 1) {
                    int pixelW = Math.max(1, (int)(width * 16.0 + 0.5));
                    int pixelH = Math.max(1, (int)(height * 16.0 + 0.5));
                    //System.out.println("find at " + aabb + "on " + shape.toAabbs().size() + " boxes!");

                    return new CanvasFace(face, offset, new PixelMatrix(pixelW, pixelH));
                }
            }
        }
        return null;
    }

    @Nullable
    public static Pair<CanvasData, CanvasFace> getOrCreateCanvasFace(Level level, BlockPos pos, BlockState state, Vec3 hitLoc, Direction face) {
        BlockEntity be = level.getBlockEntity(pos);
        CanvasFace newFace = CanvasData.calculateCanvasFace(level, pos, state, hitLoc, face);
        if (newFace == null) return null;
        CanvasData canvas = null;

        // 情况1：方块已有实体（原始实体或已是 CanvasBlockEntity）
        if (be instanceof CanvasDataHolder holder) {
            canvas = holder.painter$getCanvasData();
            if (canvas == null) canvas = CanvasData.empty();
            //System.out.println("[paintEvent] adding paint to canvasdata " + canvas.faces());
            newFace = canvas.addOrGetFace(newFace);
            holder.painter$setCanvasData(canvas);
        }
        // 情况2：无实体方块 → 替换为 CanvasBlock
        else {
            if (CanvasBlacklist.isAllowed(state.getBlock())){
                BlockState originalState = state;
                replaceWithCanvas(level, pos, originalState);
                BlockEntity newBe = level.getBlockEntity(pos);
                canvas = CanvasData.empty();
                if (newBe instanceof CanvasDataHolder holder) holder.painter$setCanvasData(canvas);
                canvas.addOrGetFace(newFace);
            }
        }
        return new Pair<>(canvas, newFace);
    }

    private static void replaceWithCanvas(Level level, BlockPos pos, BlockState originalState) {
        CanvasBlock canvasBlock = originalState.isSolidRender(level, pos)
                ? ModBlocks.CANVAS_OCCLUSION.get()
                : ModBlocks.CANVAS_NO_OCCLUSION.get();
        //System.out.println("Paint a " + canvasBlock);

        level.setBlock(pos, canvasBlock.defaultBlockState(), 3);
        BlockEntity newBe = level.getBlockEntity(pos);
        if (newBe instanceof CanvasBlockEntity canvasBE) {
            canvasBE.setMimickedState(originalState);
            NeoForge.EVENT_BUS.post(new CanvasBlockReplacedEvent(pos, originalState, canvasBE));
        }
    }

    /**
     * 获取指定表面的画布（如果存在）。
     */
    public Optional<CanvasFace> getFace(Direction primaryFace, Vec3 centerOffset) {
        CanvasFace probe = new CanvasFace(primaryFace, centerOffset, new PixelMatrix());
        return faces.stream().filter(f -> f.isSameSurface(probe)).findFirst();
    }

    public boolean isEmpty() {
        return faces.isEmpty();
    }
}