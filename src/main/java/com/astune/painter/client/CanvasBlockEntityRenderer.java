package com.astune.painter.client;

import com.astune.painter.api.CanvasData;
import com.astune.painter.api.CanvasDataHolder;
import com.astune.painter.block.CanvasBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import java.util.Map;

public class CanvasBlockEntityRenderer implements BlockEntityRenderer<BlockEntity> {

    private static final float OFFSET = 0.001f; // 防止 Z-fighting

    public CanvasBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(BlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Map<Direction, ResourceLocation> textures = null;
        CanvasData canvas = null;

        if (be instanceof CanvasDataHolder holder) {
            textures = holder.painter$getCachedFaceTextures();
            if (textures == null) return;
            canvas = holder.painter$getCanvasData();
        }
        if (canvas == null) return;

        System.out.println("rendering canvas <" + be.getBlockState().getBlock().getName() +  "> at " + be.getBlockPos());


        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5); // 方块中心

        for (var entry : textures.entrySet()) {
            Direction dir = entry.getKey();
            ResourceLocation tex = entry.getValue();
            var faceOpt = canvas.faces().stream().filter(f -> f.primaryFace() == dir).findFirst();
            if (faceOpt.isEmpty()) continue;
            var face = faceOpt.get();
            Vec3 offset = face.centerOffset();
            float w = face.pixels().getWidth() / 16f;
            float h = face.pixels().getHeight() / 16f;

            Vec3[] corners = buildCorners(dir, offset, w, h); // 已含偏移
            VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucent(tex));
            var last = poseStack.last();
            Vec3 normal = Vec3.atLowerCornerOf(dir.getNormal());
            float nx = (float) normal.x, ny = (float) normal.y, nz = (float) normal.z;

            add(vc, last, corners[0], 0, 0, nx, ny, nz, packedLight, packedOverlay);
            add(vc, last, corners[1], 1, 0, nx, ny, nz, packedLight, packedOverlay);
            add(vc, last, corners[2], 1, 1, nx, ny, nz, packedLight, packedOverlay);
            add(vc, last, corners[3], 0, 1, nx, ny, nz, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    private void add(VertexConsumer vc, PoseStack.Pose pose, Vec3 pos, float u, float v,
                     float nx, float ny, float nz, int light, int overlay) {
        vc.addVertex(pose, (float) pos.x, (float) pos.y, (float) pos.z)
                .setColor(255,255,255,255).setUv(u,v).setOverlay(overlay).setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    private Vec3[] buildCorners(Direction dir, Vec3 center, float w, float h) {
        Vec3 right, up;
        switch (dir) {
            case NORTH: right = new Vec3(-1,0,0); up = new Vec3(0,1,0);  break;
            case SOUTH: right = new Vec3(1,0,0);  up = new Vec3(0,1,0);  break;
            case EAST:  right = new Vec3(0,0,1);  up = new Vec3(0,1,0);  break;
            case WEST:  right = new Vec3(0,0,-1); up = new Vec3(0,1,0);  break;
            case UP:    right = new Vec3(1,0,0);  up = new Vec3(0,0,1);  break;
            case DOWN:  right = new Vec3(1,0,0);  up = new Vec3(0,0,-1); break;
            default: throw new IllegalArgumentException();
        }
        float hw = w / 2f, hh = h / 2f;
        Vec3 normal = Vec3.atLowerCornerOf(dir.getNormal()).scale(OFFSET);

        // 左下、右下、右上、左上，并向外微移
        Vec3 bottomLeft  = center.add(right.scale(-hw)).add(up.scale(-hh)).add(normal);
        Vec3 bottomRight = center.add(right.scale( hw)).add(up.scale(-hh)).add(normal);
        Vec3 topRight    = center.add(right.scale( hw)).add(up.scale( hh)).add(normal);
        Vec3 topLeft     = center.add(right.scale(-hw)).add(up.scale( hh)).add(normal);

        return new Vec3[] { bottomLeft, bottomRight, topRight, topLeft };
    }
}