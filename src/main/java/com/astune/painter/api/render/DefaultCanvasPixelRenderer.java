package com.astune.painter.api.render;

import com.astune.painter.api.render.CanvasPixelRenderer;
import com.astune.painter.api.render.RenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class DefaultCanvasPixelRenderer implements CanvasPixelRenderer {

    @Override
    public boolean canRender(RenderContext context){
        //System.out.println(context);
        return !(context.texture == null || !context.texture.getPath().contains("_default_"));
    }

    @Override
    public boolean renderFace(RenderContext context) {
        var face = context.face;
        var texture = context.texture;
        if (texture == null) return false;

        Vec3[] corners = face.cornerWithOffset(context.offset);
        VertexConsumer vc = context.bufferSource.getBuffer(RenderType.entityTranslucent(texture));
        var last = context.poseStack.last();
        Direction dir = face.primaryFace();
        Vec3 normal = Vec3.atLowerCornerOf(dir.getNormal());
        float nx = (float) normal.x, ny = (float) normal.y, nz = (float) normal.z;

        int light = context.packedLight;
        // 注意：isOcclusion 光照调整已在 BER 中完成，传入的 packedLight 已经是正确的
        add(vc, last, corners[0], 0, 0, nx, ny, nz, light, context.packedOverlay);
        add(vc, last, corners[1], 1, 0, nx, ny, nz, light, context.packedOverlay);
        add(vc, last, corners[2], 1, 1, nx, ny, nz, light, context.packedOverlay);
        add(vc, last, corners[3], 0, 1, nx, ny, nz, light, context.packedOverlay);
        return true;
    }

    private static void add(VertexConsumer vc, PoseStack.Pose pose, Vec3 pos, float u, float v,
                            float nx, float ny, float nz, int light, int overlay) {
        vc.addVertex(pose, (float) pos.x, (float) pos.y, (float) pos.z)
                .setColor(255,255,255,255)
                .setUv(u,v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}