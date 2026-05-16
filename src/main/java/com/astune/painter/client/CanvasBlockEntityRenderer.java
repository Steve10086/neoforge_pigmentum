package com.astune.painter.client;

import com.astune.painter.api.CanvasData;
import com.astune.painter.api.CanvasDataHolder;
import com.astune.painter.api.CanvasFace;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class CanvasBlockEntityRenderer implements BlockEntityRenderer<BlockEntity> {

    private static final float OFFSET = 0.001f; // 防止 Z-fighting

    public CanvasBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(BlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        @Nullable List<Pair<CanvasFace, ResourceLocation>> textures = null;
        CanvasData canvas = null;

        if (be instanceof CanvasDataHolder holder) {
            textures = holder.painter$getCachedFaceTextures();
            if (textures == null) return;
            canvas = holder.painter$getCanvasData();
        }
        if (canvas == null) return;

        //System.out.println("rendering canvas <" + textures +  "> at " + be.getBlockPos());


        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5); // 方块中心

        for (var pair : textures) {
            CanvasFace face = pair.getFirst();
            ResourceLocation tex = pair.getSecond();
            if (tex == null) continue;

            Direction dir = face.primaryFace();
            Vec3[] corners = face.cornerWithOffset();


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

}