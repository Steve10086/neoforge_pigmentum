package com.astune.painter.mixin;

import com.astune.painter.block.CanvasBlock;
import com.astune.painter.block.CanvasBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Mixin 类: LevelRendererMixin.java
@Mixin(BlockRenderDispatcher.class)
public abstract class BlockRenderDispatcherMixin {

    @Inject(method = "Lnet/minecraft/client/renderer/block/BlockRenderDispatcher;renderBreakingTexture(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/neoforged/neoforge/client/model/data/ModelData;)V", at = @At("HEAD"), cancellable = true)
    private void onRenderBreakingTexture(BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack poseStack, VertexConsumer consumer, ModelData modelData, CallbackInfo ci) {
        // 检查当前被破坏的方块是不是代理画布
        if (state.getBlock() instanceof CanvasBlock) {
            // 从方块实体中获取被模仿的状态
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CanvasBlockEntity canvasBE && canvasBE.getMimickedState() != null) {
                BlockState mimickedState = canvasBE.getMimickedState();
                Minecraft mc = Minecraft.getInstance();
                BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
                BakedModel model = dispatcher.getBlockModel(mimickedState);

                // 为模仿的模型渲染破坏纹理
                // 注意：这里是调用原版方法，但传入了正确的模型
                dispatcher.renderBreakingTexture(mimickedState, pos, level, poseStack, consumer, modelData);
                ci.cancel(); // 取消原方法的执行，因为我们已手动处理
            }
        }
    }
}
