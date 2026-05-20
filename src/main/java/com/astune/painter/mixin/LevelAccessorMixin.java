// mixin/LevelAccessorMixin.java
package com.astune.painter.mixin;

import com.astune.painter.block.CanvasBlock;
import com.astune.painter.block.CanvasBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelAccessor.class)
public interface LevelAccessorMixin {

    @Inject(method = "scheduleTick(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;I)V",
            at = @At("HEAD"), cancellable = true)
    default void onScheduleTick(BlockPos pos, Block block, int delay, CallbackInfo ci) {
        // 确保是服务端调用，可选
        if (this instanceof LevelAccessor self){
            BlockState current = self.getBlockState(pos);
            if (current.getBlock() instanceof CanvasBlock) {
                BlockEntity be = self.getBlockEntity(pos);
                if (be instanceof CanvasBlockEntity canvasBE) {
                    BlockState mimicked = canvasBE.getMimickedState();
                    if (mimicked != null && mimicked.getBlock() == block) {
                        // 用画布方块自身来调度计划刻
                        self.scheduleTick(pos, current.getBlock(), delay);
                        ci.cancel(); // 取消原本的调度
                    }
                }
            }
        }

    }
}