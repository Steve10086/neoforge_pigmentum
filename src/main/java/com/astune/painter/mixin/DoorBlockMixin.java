package com.astune.painter.mixin;

import com.astune.painter.block.CanvasBlock;
import com.astune.painter.block.CanvasBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minecraft.world.level.block.DoorBlock.HALF;
import static net.minecraft.world.level.block.DoorBlock.OPEN;

@Mixin(DoorBlock.class)
public class DoorBlockMixin {
    @Inject(method = "canSurvive(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z",
    at = @At("HEAD"), cancellable = true)
    private void onCanSurvive(BlockState state, LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (level.getBlockState(pos).getBlock() instanceof CanvasBlock) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CanvasBlockEntity canvasBE) {
                BlockState mimicked = canvasBE.getMimickedState();
                if (mimicked != null) {
                    cir.setReturnValue(mimicked.canSurvive(level, pos));
                }
            }
        }
    }

    @Inject(method = "updateShape(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("HEAD"), cancellable = true)
    private void onUpdateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos,
                               CallbackInfoReturnable<BlockState> cir) {
        // 检查相邻方块是否为 CanvasBlock，且其内部是门
        DoorBlock self = (DoorBlock) (Object) this;
        if (facingState.getBlock() instanceof CanvasBlock) {
            BlockEntity be = level.getBlockEntity(facingPos);
            //System.out.println("hacking original door... " + be);
            if (be instanceof CanvasBlockEntity canvasBE) {
                BlockState mimicked = canvasBE.getMimickedState();
                //System.out.println("hacking original door... " + mimicked);
                if (mimicked == null) {
                    // 相邻方块是画布方块，且内部是门，则保持当前方块不变（不破坏自身）
                    cir.setReturnValue(state);
                    //System.out.println("hack original door success but null");
                    return;
                }else if(mimicked.is(self)) {
                    cir.setReturnValue(state.setValue(OPEN, mimicked.getValue(OPEN)));
                }
            }
        }
    }
}
