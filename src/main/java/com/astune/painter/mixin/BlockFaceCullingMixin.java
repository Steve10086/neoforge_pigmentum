package com.astune.painter.mixin;

import com.astune.painter.block.CanvasBlock;
import com.astune.painter.block.CanvasBlockEntity;
import com.astune.painter.network.ClientCanvasCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class BlockFaceCullingMixin {

    @Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true)
    private static void painter$checkCanvasNeighbor(BlockState state, BlockGetter level, BlockPos offset,
                                                    Direction face, BlockPos pos,
                                                    CallbackInfoReturnable<Boolean> cir) {
        BlockState neighborState = level.getBlockState(pos);
        if (!(neighborState.getBlock() instanceof CanvasBlock)) {
            return;
        }

        BlockState mimickedState = null;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CanvasBlockEntity canvasBlockEntity) {
            mimickedState = canvasBlockEntity.getMimickedState();
        }
        if (mimickedState == null) {
            mimickedState = ClientCanvasCache.getMimickedState(pos);
        }

        if (mimickedState != null && state.skipRendering(mimickedState, face)) {
            cir.setReturnValue(false);
        }
    }
}
