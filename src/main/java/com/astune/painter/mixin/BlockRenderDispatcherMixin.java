package com.astune.painter.mixin;

import com.astune.painter.attachment.ModAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderDispatcher.class)
public class BlockRenderDispatcherMixin {
    @Inject(method = "renderBatched", at = @At("HEAD"), cancellable = true)
    private void onRenderBatched(BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack poseStack,
                                 VertexConsumer consumer, boolean checkSides, RandomSource random,
                                 CallbackInfo ci) {
        if (level instanceof Level realLevel) {
            BlockEntity be = realLevel.getBlockEntity(pos);
            if (be != null && be.hasData(ModAttachments.CANVAS_DATA)) {
                System.out.println("canceled render");
                ci.cancel();
            }
        }
    }
}