package com.astune.painter.api.render;

import com.astune.painter.api.CanvasFace;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import com.mojang.blaze3d.vertex.PoseStack;

import java.util.function.Function;

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
    public final double offset;
    private final Function<ResourceLocation, RenderType> renderTypeFactory;

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
        this.offset = 0.01;
        this.renderTypeFactory = RenderType::entityTranslucent;

    }
    public RenderContext(CanvasFace face, ResourceLocation texture, PoseStack poseStack,
                         MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                         Level level, BlockPos pos, boolean isOcclusion, double offset) {
        this(face, texture, poseStack, bufferSource, packedLight, packedOverlay,
                level, pos, isOcclusion, offset, RenderType::entityTranslucent);
    }

    public RenderContext(CanvasFace face, ResourceLocation texture, PoseStack poseStack,
                         MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                         Level level, BlockPos pos, boolean isOcclusion, double offset,
                         Function<ResourceLocation, RenderType> renderTypeFactory) {
        this.face = face;
        this.texture = texture;
        this.poseStack = poseStack;
        this.bufferSource = bufferSource;
        this.packedLight = packedLight;
        this.packedOverlay = packedOverlay;
        this.level = level;
        this.pos = pos;
        this.isOcclusion = isOcclusion;
        this.offset = offset;
        this.renderTypeFactory = renderTypeFactory;
    }

    public RenderType renderType(ResourceLocation texture) {
        return renderTypeFactory.apply(texture);
    }
}
