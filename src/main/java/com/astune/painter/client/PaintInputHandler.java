// client/PaintInputHandler.java
package com.astune.painter.client;

import com.astune.painter.api.*;
import com.astune.painter.api.blend.BlendContext;
import com.astune.painter.api.blend.BlendFunction;
import com.astune.painter.network.CanvasAction;
import com.astune.painter.network.CanvasUploadPacket;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;

import static com.astune.painter.api.CanvasData.getOrCreateCanvasFace;

@EventBusSubscriber(modid = "painter", value = Dist.CLIENT)
public class PaintInputHandler {

    private static final Set<BlockPos> pendingCanvasRequests = new HashSet<>();
    private static Vec3 lastHitLoc = null;
    private static boolean wasDrawing = false;
    private static int paintCounter = 0;

    @SubscribeEvent
    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        ItemStack stack = mc.player.getMainHandItem();
        IPaintProvider provider = PaintProviders.getProvider(stack);
        if (provider == null) {
            wasDrawing = false;
            lastHitLoc = null;
            return;
        }

        boolean isDrawing = mc.player.getMainHandItem().getItem() instanceof IPaintProvider
                && PaintProviders.getProvider(mc.player.getMainHandItem()) != null
                && mc.options.keyUse.isDown();

        if (!isDrawing) {
            wasDrawing = false;
            lastHitLoc = null;
            return;
        }

        // 控制绘制频率
        paintCounter++;
        if (paintCounter < provider.getPaintInterval()) return;
        paintCounter = 0;

        double pixelStep = 1.0/32;

        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return;
        BlockHitResult blockHit = (BlockHitResult) mc.hitResult;
        Vec3 currentHitLoc = blockHit.getLocation();

        // 首次绘制
        if (!wasDrawing || lastHitLoc == null) {
            paintPattern(mc, stack, blockHit, provider, pixelStep);
            lastHitLoc = currentHitLoc;
            wasDrawing = true;
            return;
        }

        // Bresenham 3D 插值
        Vec3 dir = currentHitLoc.subtract(lastHitLoc);
        double dist = dir.length();
        double stepSize = provider.getStep();
        int steps = (int) (dist / stepSize);
        Vec3 stepVec = dir.normalize().scale(stepSize);

        Vec3 pos = lastHitLoc;
        for (int i = 0; i <= steps; i++) {
            BlockHitResult midHit = traceHit(mc, pos);
            if (midHit != null) {
                paintPattern(mc, stack, midHit, provider, pixelStep);
            }
            pos = pos.add(stepVec);
        }

        paintPattern(mc, stack, blockHit, provider, pixelStep);
        lastHitLoc = currentHitLoc;
        wasDrawing = true;
    }

    private static void paintPattern(Minecraft mc, ItemStack stack,
                                     BlockHitResult hitLoc, IPaintProvider provider, double pixelStep) {
        PaintPattern pattern = provider.getPattern(stack, mc.player, mc.level,
                hitLoc.getBlockPos(), hitLoc.getLocation());
        if (pattern == null || pattern.width() <= 0 || pattern.height() <= 0) return;

        Player player = mc.player;

        Direction normal = hitLoc.getDirection();
        BlockPos pos = hitLoc.getBlockPos();
        Vec3 hitPoint = hitLoc.getLocation();
        double patternW = pattern.width();
        double patternH = pattern.height();

        // 根据玩家视线动态计算面上的局部坐标轴，使图案始终正对玩家
        Vec3 lookVec = player.getViewVector(1.0f);
        Vec3 normalVec = Vec3.atLowerCornerOf(normal.getNormal());

        // rightInWorld：在面内且指向屏幕右侧的方向
        Vec3 rightInWorld = lookVec.cross(normalVec).normalize();
        // upInWorld：在面内且指向屏幕上方的方向
        Vec3 upInWorld = normalVec.cross(rightInWorld).normalize();

        // 如果玩家正对法线，cross 结果为零向量，此时使用基于 world up 的回退方案
        if (rightInWorld.lengthSqr() < 0.001 || upInWorld.lengthSqr() < 0.001) {
            Vec3 worldUp = new Vec3(0, 1, 0);
            rightInWorld = worldUp.cross(normalVec).normalize();
            upInWorld = normalVec.cross(rightInWorld).normalize();
        }

        // 起始点：从命中点向左下移动半个图案尺寸
        Vec3 startPos = hitPoint
                .subtract(rightInWorld.scale((patternW / 2.0)))
                .subtract(upInWorld.scale((patternH / 2.0)));

        // 左下角世界坐标
        Vec3 c1 = hitPoint.subtract(rightInWorld.scale(patternW / 2.0))
                .subtract(upInWorld.scale(patternH / 2.0));

        // 将总长度编码到方向向量中
        Vec3 rightTotal = rightInWorld.scale(patternW);
        Vec3 upTotal = upInWorld.scale(patternH);

        paintAt(mc, c1, rightTotal, upTotal, pixelStep, pattern.provider(), 2, normalVec);





    }

    private static final int MIX;

    private static final int ADD;


    private static final int REPLACE;

    static {
        MIX = 1;
        ADD = 2;
        REPLACE = 3;
    }
    /**
     * 批量绘制像素（优化版），使用法线方向追踪。
     *
     * @param mc            Minecraft 实例
     * @param c1           图案左下角世界坐标
     * @param rightInWorld 图案水平方向总向量（长度 = 图案宽度）
     * @param upInWorld    图案垂直方向总向量（长度 = 图案高度）
     * @param pixelStep    像素步长（世界坐标距离）
     * @param provider     像素颜色提供者
     * @param type         绘制类型（暂未使用）
     * @param normalVec    绘制面的法线方向（单位向量）
     */
    private static void paintAt(Minecraft mc, Vec3 c1, Vec3 rightInWorld, Vec3 upInWorld,
                                double pixelStep, PixelProvider provider, int type, Vec3 normalVec) {
        double width = rightInWorld.length();
        double height = upInWorld.length();
        if (width < pixelStep || height < pixelStep) return;

        Vec3 dirX = rightInWorld.normalize();
        Vec3 dirY = upInWorld.normalize();

        int stepsX = (int) Math.ceil(width / pixelStep) + 1;
        int stepsY = (int) Math.ceil(height / pixelStep) + 1;

        Vec3[][] grid = new Vec3[stepsX][stepsY];
        for (int i = 0; i < stepsX; i++) {
            for (int j = 0; j < stepsY; j++) {
                grid[i][j] = c1.add(dirX.scale(i * pixelStep)).add(dirY.scale(j * pixelStep));
            }
        }

        ItemStack stack = null;
        if (mc.player != null) {
            stack = mc.player.getMainHandItem();
        }

        boolean[][] processed = new boolean[stepsX][stepsY];

        for (int j = 0; j < stepsY; j++) {
            for (int i = 0; i < stepsX; i++) {
                if (processed[i][j]) continue;

                Vec3 worldPos = grid[i][j];
                BlockHitResult hit = traceNormalDir(worldPos, normalVec, mc.player);
                if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
                    processed[i][j] = true;
                    continue;
                }

                BlockPos pos = hit.getBlockPos();
                Direction face = hit.getDirection();

                BlockState state = mc.level.getBlockState(pos);
                CanvasDataHolder holder = null;
                CanvasData canvas = null;

                Vec3 hitLocation = hit.getLocation();
                BlockEntity be = mc.level.getBlockEntity(pos);
                CanvasFace targetFace = null;

                if (be instanceof CanvasDataHolder h) {
                    holder = h;
                    canvas = h.painter$getCanvasData();
                    if (canvas != null) {
                        targetFace = canvas.getFaceAtHit(pos, hit);
                    }
                }

                if (targetFace == null) {
                    //System.out.println("failed to get existing face");
                    Pair<CanvasData, CanvasFace> p = getOrCreateCanvasFace(mc.level, pos, state, hitLocation, face);
                    if (p == null) {
                        processed[i][j] = true;
                        continue;
                    }
                    be = mc.level.getBlockEntity(pos);
                    if (be instanceof CanvasDataHolder h) holder = h;
                    else {
                        processed[i][j] = true;
                        continue;
                    }
                    canvas = p.getFirst();
                    targetFace = p.getSecond();
                }
                if (targetFace != null){
                    // ==== 第一步：轮询所有 slot，将在面内的 slot 标记为已完成 ====
                    for (int jj = 0; jj < stepsY; jj++) {
                        for (int ii = 0; ii < stepsX; ii++) {
                            if (processed[ii][jj]) continue;
                            Vec3 point = grid[ii][jj];
                            int[] pixel = calculatePixelFromHit(point, pos, targetFace);
                            if (pixel != null) {
                                processed[ii][jj] = true;
                            }
                        }
                    }

                    // ==== 第二步：遍历面像素，判断是否在图案矩形内并上色 ====
                    int pixelW = targetFace.pixels().getWidth();
                    int pixelH = targetFace.pixels().getHeight();
                    Vec3 c0 = targetFace.corner0();
                    Vec3 corner1 = targetFace.corner1();
                    Vec3 corner3 = targetFace.corner3();
                    Vec3 uAxis = corner1.subtract(c0);
                    Vec3 vAxis = corner3.subtract(c0);
                    double pixelSizeX = uAxis.length() / pixelW;
                    double pixelSizeY = vAxis.length() / pixelH;
                    Vec3 uDir = uAxis.normalize();
                    Vec3 vDir = vAxis.normalize();

                    // 方块中心的世界坐标（修复点）
                    Vec3 blockCenter = Vec3.atCenterOf(pos);
                    boolean result = false;

                    for (int py = 0; py < pixelH; py++) {
                        for (int px = 0; px < pixelW; px++) {
                            // 像素在面局部坐标系下的位置（相对于方块中心）
                            Vec3 localPixelCenter = c0.add(uDir.scale((px + 0.5) * pixelSizeX))
                                    .add(vDir.scale((py + 0.5) * pixelSizeY));
                            // 转换为世界坐标
                            Vec3 pixelWorld = blockCenter.add(localPixelCenter);

                            // 映射到图案局部坐标
                            Vec3 local = pixelWorld.subtract(c1);
                            double x = local.dot(dirX);
                            double y = local.dot(dirY);

                            if (x < -0.001 || x > width + 0.001 || y < -0.001 || y > height + 0.001) continue;

                            Integer color = provider.getPixel(x, y);
                            if (color != null) {
                                // 获取混合函数：优先从画笔获取自定义混合，其次使用 BlendMode 枚举的默认
                                BlendFunction blendFunc = provider instanceof IPaintProvider ip ? ip.getCustomBlendFunction(stack) : null;
                                if (blendFunc == null) {
                                    blendFunc = provider.getBlendMode(x, y).getDefaultFunction();
                                }

                                Map<String, Integer> effects = provider.getEffectValues(x, y);
                                BlendContext context = new BlendContext(targetFace, px, py, targetFace.pixels().getPixel(px, py), color,
                                        blendFunc == null ? provider.getBlendMode(x, y) : null,
                                        stack, effects);
                                result = blendFunc.apply(context) || result;
                            }
                        }
                    }
                    if(result){
                        holder.painter$setCanvasData(canvas);
                        holder.painter$regenerateTextures(canvas);
                        addPendingPixel(pos);
                    }
                }
            }
        }

    }

    private static void paintAt(Minecraft mc, BlockHitResult hit, Integer color, int type) {
        BlockPos pos = hit.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        Vec3 hitLoc = hit.getLocation();
        Direction face = hit.getDirection();

        BlockEntity be = mc.level.getBlockEntity(pos);
        CanvasData canvas = null;
        CanvasDataHolder holder = null;
        List<CanvasFace> canvasFaces = List.of();

        if (be instanceof CanvasDataHolder h) {
            holder = h;
            canvas = h.painter$getCanvasData();
            if (canvas != null) {
                canvasFaces = canvas.getFaceAtHit(pos, hitLoc);
            }
        }

        if (canvasFaces.isEmpty()) {
            Pair<CanvasData, CanvasFace> p = getOrCreateCanvasFace(mc.level, pos, state, hitLoc, face);
            be = mc.level.getBlockEntity(pos);
            if (be instanceof CanvasDataHolder h) {
                holder = h;
            } else {
                return;
            }
            if (p == null) return;
            canvas = p.getFirst();
            canvasFaces = List.of(p.getSecond());
        }

        if (canvas == null) return;

        for (CanvasFace targetFace : canvasFaces) {
            if (targetFace == null || targetFace.pixels() == null) continue;

            int[] pixel = calculatePixelFromHit(hitLoc, pos, targetFace);
            if (pixel == null) continue;

            boolean result = false;

            result = targetFace.pixels().setPixel(pixel[0], pixel[1], color);

            if (result){
                holder.painter$setCanvasData(canvas);
                holder.painter$regenerateTextures(canvas);

                addPendingPixel(pos);
            }

        }
    }

    @Nullable
    private static BlockHitResult traceNormalDir(Vec3 worldPos, Vec3 normalVec, Player player) {
        return player.level().clip(new ClipContext(
                worldPos,
                worldPos.add(normalVec.scale(-0.2)),
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        ));
    }

    private static BlockHitResult traceHit(Minecraft mc, Vec3 targetPos) {
        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 dir = targetPos.subtract(eyePos).normalize();
        double reach = eyePos.distanceTo(targetPos) + 0.1;
        return mc.level.clip(new ClipContext(eyePos, eyePos.add(dir.scale(reach)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
    }

    /**
     * 绘画结束后统一发送（低频发包）。
     */
    @SubscribeEvent
    public static void onMouseReleased(InputEvent.MouseButton.Pre event) {
        if (!pendingCanvasRequests.isEmpty()) {
            //System.out.println(pendingCanvasRequests.size() + " new updates!");
            for (BlockPos pos : pendingCanvasRequests) {
                BlockEntity be = Minecraft.getInstance().level.getBlockEntity(pos);
                    if (be instanceof CanvasDataHolder holder) {
                        CanvasUploadPacket packet = new CanvasUploadPacket(pos, holder.painter$getCanvasData(), CanvasAction.ADD_CREATION);
                        PacketDistributor.sendToServer(packet);
                    }
                }
            }
            pendingCanvasRequests.clear();
    }

    private static void addPendingPixel(BlockPos pos) {
        pendingCanvasRequests.add(pos);
    }

    /**
     * 根据命中点和画布面信息，计算具体的像素索引。
     */
    @Nullable
    private static int[] calculatePixelFromHit(Vec3 hitLoc, BlockPos pos, CanvasFace face) {
        Direction dir = face.primaryFace();
        Vec3 localHit = hitLoc.subtract(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

        // 获取面的本地角点坐标
        Vec3 c0 = face.corner0(); // 左下
        Vec3 c1 = face.corner1(); // 右下
        Vec3 c3 = face.corner3(); // 左上

        // 计算局部边向量
        Vec3 sideW = c1.subtract(c0); // 水平方向 (从 corner0 到 corner1)
        Vec3 sideH = c3.subtract(c0); // 垂直方向 (从 corner0 到 corner2)
        double wLen = sideW.lengthSqr();
        double hLen = sideH.lengthSqr();
        if (wLen < 1e-6 || hLen < 1e-6) return null;

        // 局部坐标系原点：corner0
        Vec3 relative = localHit.subtract(c0);
        //System.out.println(relative);

        // 计算 UV 坐标
        double u = relative.dot(sideW) / wLen;
        double v = relative.dot(sideH) / hLen;

        int pixelW = face.pixels().getWidth();
        int pixelH = face.pixels().getHeight();
        //System.out.println(u+" "+v);

        int px = (int)(u * pixelW);
        int py = (int)(v * pixelH);

        //System.out.println(px+" "+py);

        // 边界检查
        if (px < 0 || px > pixelW || py < 0 || py > pixelH) return null;
        return new int[]{px, py};
    }

    private static int applyBlendMode(int existing, int newColor, BlendMode mode) {
        return switch (mode) {
            case OVERWRITE -> newColor;
            case ADD -> {
                // 颜色不同，进行带 alpha 混合：result = src over dst
                int eA = (existing >> 24) & 0xFF;
                int eR = (existing >> 16) & 0xFF;
                int eG = (existing >> 8) & 0xFF;
                int eB = existing & 0xFF;
                int nA = (newColor >> 24) & 0xFF;
                int nR = (newColor >> 16) & 0xFF;
                int nG = (newColor >> 8) & 0xFF;
                int nB = newColor & 0xFF;

                float srcA = nA / 255f;
                float dstA = eA / 255f;
                float outA = srcA + dstA * (1 - srcA);
                if (outA < 0.001f) {
                    yield 0;
                }
                int outR = (int)((nR * srcA + eR * dstA * (1 - srcA)) / outA);
                int outG = (int)((nG * srcA + eG * dstA * (1 - srcA)) / outA);
                int outB = (int)((nB * srcA + eB * dstA * (1 - srcA)) / outA);
                int outAInt = Math.min(255, (int)(outA * 255));
                yield (outAInt << 24) | (outR << 16) | (outG << 8) | outB;
            }
            case MULTIPLY -> {
                float srcA = ((newColor >> 24) & 0xFF) / 255f;
                int dstR = (existing >> 16) & 0xFF;
                int dstG = (existing >> 8) & 0xFF;
                int dstB = existing & 0xFF;
                int srcR = (newColor >> 16) & 0xFF;
                int srcG = (newColor >> 8) & 0xFF;
                int srcB = newColor & 0xFF;
                // 正片叠底
                int mulR = (srcR * dstR) / 255;
                int mulG = (srcG * dstG) / 255;
                int mulB = (srcB * dstB) / 255;
                // 根据源 alpha 混合
                int outR = (int)(dstR + (mulR - dstR) * srcA);
                int outG = (int)(dstG + (mulG - dstG) * srcA);
                int outB = (int)(dstB + (mulB - dstB) * srcA);
                int outA = (existing >>> 24); // 保持目标 alpha 不变（或可组合）
                yield (outA << 24) | (outR << 16) | (outG << 8) | outB;
            }
            case ERASE -> 0;
        };
    }
}