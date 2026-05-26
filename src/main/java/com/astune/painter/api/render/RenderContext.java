package com.astune.painter.api.render;

import com.astune.painter.api.CanvasFace;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import com.mojang.blaze3d.vertex.PoseStack;

public class RenderContext {
    public final CanvasFace face;
    public final ResourceLocation texture;
    public final PoseStack poseStack;
    public final MultiBufferSource bufferSource;
    public final int packedLight;
    public final int packedOverlay;
    public final Level level;
    public final BlockPos pos;
    public final boolean isOcclusion;

    public RenderContext(CanvasFace face, ResourceLocation texture, PoseStack poseStack,
                         MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                         Level level, BlockPos pos, boolean isOcclusion) {
        this.face = face;
        this.texture = texture;
        this.poseStack = poseStack;
        this.bufferSource = bufferSource;
        this.packedLight = packedLight;
        this.packedOverlay = packedOverlay;
        this.level = level;
        this.pos = pos;
        this.isOcclusion = isOcclusion;
    }
}