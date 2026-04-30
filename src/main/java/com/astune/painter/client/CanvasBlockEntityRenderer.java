package com.astune.painter.client;

import com.astune.painter.block.CanvasBlockEntity;
import com.astune.painter.data.CanvasData;
import com.astune.painter.api.PixelMatrix;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;

@OnlyIn(Dist.CLIENT)
public class CanvasBlockEntityRenderer implements BlockEntityRenderer<CanvasBlockEntity> {

    public CanvasBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(CanvasBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState mimicked = be.getMimickedState();
        if (mimicked.isAir()) return;

        poseStack.pushPose();

        // 渲染原方块模型（使用原方块的全部渲染信息）
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        // 传递原方块对应位置的环境光照？这里用固定 light 也可以，但更好的做法是从 be 获取
        // 我们保持 packedLight 不变，传入 ModelData.EMPTY 对于大多数方块已足够
        dispatcher.renderSingleBlock(mimicked, poseStack, buffer, packedLight, packedOverlay,
                ModelData.EMPTY, null);

        // 渲染画布层
        CanvasData data = be.getCanvasData();
        if (data != null) {
            renderPixels(poseStack, buffer, data.face(), data.pixels(), packedLight, packedOverlay);
        }

        poseStack.popPose();
    }

    // 简单规定每个像素都是边长 1/16 的正方形，覆盖整个面
    private void renderPixels(PoseStack poseStack, MultiBufferSource buffer,
                              Direction face, PixelMatrix pixels,
                              int packedLight, int packedOverlay) {
        VertexConsumer builder = buffer.getBuffer(RenderType.translucent());
        int size = PixelMatrix.SIZE; // 16
        float step = 1.0f / size;    // 每个像素的宽度

        // 面外移一点，避免 Z-fighting
        float offset = 0.005f;
        float nx = face.getStepX() * offset;
        float ny = face.getStepY() * offset;
        float nz = face.getStepZ() * offset;

        // 为每个非透明像素绘制四边形
        for (int x = 0; x < size; x++) {
            float u0 = x * step;
            float u1 = (x + 1) * step;
            for (int y = 0; y < size; y++) {
                int color = pixels.getColor(x, y);
                if ((color >> 24 & 0xFF) == 0) continue; // 完全透明跳过

                float v0 = y * step;
                float v1 = (y + 1) * step;

                float r = ((color >> 16) & 0xFF) / 255f;
                float g = ((color >> 8) & 0xFF) / 255f;
                float b = (color & 0xFF) / 255f;
                float a = ((color >> 24) & 0xFF) / 255f;

                // 根据面方向定义四个角的局部坐标 (0..1)
                float[] va = getVertex(face, u0, v0, 1.0f, nx, ny, nz);
                float[] vb = getVertex(face, u1, v0, 1.0f, nx, ny, nz);
                float[] vc = getVertex(face, u1, v1, 1.0f, nx, ny, nz);
                float[] vd = getVertex(face, u0, v1, 1.0f, nx, ny, nz);

                addColoredVertex(builder, poseStack, va[0], va[1], va[2], r, g, b, a, u0, v0, packedLight, packedOverlay);
                addColoredVertex(builder, poseStack, vb[0], vb[1], vb[2], r, g, b, a, u1, v0, packedLight, packedOverlay);
                addColoredVertex(builder, poseStack, vc[0], vc[1], vc[2], r, g, b, a, u1, v1, packedLight, packedOverlay);
                addColoredVertex(builder, poseStack, vd[0], vd[1], vd[2], r, g, b, a, u0, v1, packedLight, packedOverlay);
            }
        }
    }

    // 根据面方向计算一个点的世界坐标（方块局部范围 0..1）
    private float[] getVertex(Direction face, float u, float v, float depth,
                              float nx, float ny, float nz) {
        // depth 通常为1，表示表面外侧，再加上微偏移
        float out = 1.0f; // 面的外侧位置
        float x, y, z;
        switch (face) {
            case DOWN:
                x = u; y = out; z = v; break;
            case UP:
                x = u; y = 1.0f - out; z = v; break;
            case NORTH:
                x = u; y = v; z = out; break;
            case SOUTH:
                x = 1.0f - u; y = v; z = 1.0f - out; break;
            case WEST:
                x = out; y = v; z = u; break;
            case EAST:
                x = 1.0f - out; y = v; z = 1.0f - u; break;
            default:
                x = u; y = v; z = 0;
        }
        // 微偏移
        x += nx;
        y += ny;
        z += nz;
        return new float[]{x, y, z};
    }

    private void addColoredVertex(VertexConsumer builder, PoseStack poseStack,
                                  float x, float y, float z,
                                  float r, float g, float b, float a,
                                  float u, float v, int light, int overlay) {
        PoseStack.Pose pose = poseStack.last();
        builder.addVertex(pose.pose(), x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setLight(light)
                .setOverlay(overlay)
                .setNormal(pose, 0, 1, 0);
    }
}