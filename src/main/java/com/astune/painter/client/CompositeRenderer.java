// com.astune.painter.client.CompositeRenderer.java
package com.astune.painter.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public class CompositeRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {

    @Nullable
    private final BlockEntityRenderer<T> original;
    private final BlockEntityRenderer<T> overlay;

    public CompositeRenderer(@Nullable BlockEntityRenderer<T> original, BlockEntityRenderer<T> overlay) {
        this.original = original;
        this.overlay = overlay;
    }

    @Override
    public void render(T be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // 1. 先渲染原版模型
        if (original != null) {
            original.render(be, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        }
        // 2. 再渲染画布覆盖层
        overlay.render(be, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
    }
}