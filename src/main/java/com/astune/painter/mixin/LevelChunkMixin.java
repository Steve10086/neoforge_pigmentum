package com.astune.painter.mixin;

import com.astune.painter.CanvasProperties;
import com.astune.painter.block.CanvasBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public class LevelChunkMixin {

    // 正确的方法名：setBlockState
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void onSetBlockState(BlockPos pos, BlockState state, boolean isMoving, CallbackInfoReturnable<BlockState> cir) {
        // 当方块状态更新后，如果新状态不再具有画布属性则移除遗留的 CanvasBlockEntity
        if (!state.hasProperty(CanvasProperties.HAVE_CANVAS) || !state.getValue(CanvasProperties.HAVE_CANVAS)) {
            LevelChunk chunk = (LevelChunk)(Object)this;
            BlockEntity be = chunk.getBlockEntity(pos);
            if (be instanceof CanvasBlockEntity) {
                chunk.removeBlockEntity(pos);
            }
        }
    }
}