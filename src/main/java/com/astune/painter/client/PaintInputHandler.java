// client/PaintInputHandler.java
package com.astune.painter.client;

import com.astune.painter.api.*;
import com.astune.painter.block.CanvasBlockEntity;
import com.astune.painter.item.DebugPaintbrush;
import com.astune.painter.network.CanvasAction;
import com.astune.painter.network.CanvasUploadPacket;
import com.astune.painter.network.PaintPixelPacket;
import com.astune.painter.network.SyncCanvasPacket;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;

import static com.astune.painter.api.CanvasData.getOrCreateCanvasFace;

@EventBusSubscriber(modid = "painter", value = Dist.CLIENT)
public class PaintInputHandler {

    private static final Map<BlockPos, CanvasUploadPacket> pendingCanvasRequests = new HashMap<>();
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

        boolean isDrawing = mc.player.getMainHandItem().getItem() instanceof DebugPaintbrush
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

        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return;
        BlockHitResult blockHit = (BlockHitResult) mc.hitResult;
        Vec3 currentHitLoc = blockHit.getLocation();

        // 首次绘制
        if (!wasDrawing || lastHitLoc == null) {
            paintAt(mc, blockHit, provider);
            lastHitLoc = currentHitLoc;
            wasDrawing = true;
            return;
        }

        // Bresenham 3D 插值
        Vec3 dir = currentHitLoc.subtract(lastHitLoc);
        double dist = dir.length();
        double stepSize = 0.01;
        int steps = (int) (dist / stepSize);
        Vec3 stepVec = dir.normalize().scale(stepSize);

        Vec3 pos = lastHitLoc;
        for (int i = 0; i <= steps; i++) {
            BlockHitResult midHit = traceHit(mc, pos);
            if (midHit != null) {
                paintAt(mc, midHit, provider);
            }
            pos = pos.add(stepVec);
        }

        paintAt(mc, blockHit, provider);
        lastHitLoc = currentHitLoc;
        wasDrawing = true;
    }

    private static void paintAt(Minecraft mc, BlockHitResult hit, IPaintProvider provider) {
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

            // 从画笔获取颜色
            Integer color = provider.getColor(mc.player.getMainHandItem(), mc.player, mc.level, pos, targetFace, pixel[0], pixel[1]);
            boolean result = false;
            if (color == null) {
                // 擦除：设为全透明
                result = targetFace.pixels().setPixel(pixel[0], pixel[1], 0x00000000);
            } else {
                result = targetFace.pixels().setPixel(pixel[0], pixel[1], color);
            }
            if (result){
                holder.painter$regenerateTextures(canvas);
                addPendingPixel(pos, be);
            }

        }
    }

    private static BlockHitResult traceHit(Minecraft mc, Vec3 targetPos) {
        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 dir = targetPos.subtract(eyePos).normalize();
        double reach = eyePos.distanceTo(targetPos) + 0.1;
        return mc.level.clip(new ClipContext(eyePos, eyePos.add(dir.scale(reach)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
    }

    /**
     * 每 Tick 结束时发送累积的像素（低频发包）。
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        if (!pendingCanvasRequests.isEmpty()) {
            System.out.println(pendingCanvasRequests.size() + " new updates!");
            for (CanvasUploadPacket packet : pendingCanvasRequests.values()) {
                PacketDistributor.sendToServer(packet);
                }
            }
            pendingCanvasRequests.clear();
    }

    private static void addPendingPixel(BlockPos pos, BlockEntity be) {
        CanvasData canvas = null;
        if (be instanceof CanvasDataHolder holder){
            canvas = holder.painter$getCanvasData();
        }
        if(canvas == null) return;
        pendingCanvasRequests.put(pos, new CanvasUploadPacket(pos, canvas, CanvasAction.ADD_CREATION));
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
}