package com.astune.painter.mixin;

import com.astune.painter.block.CanvasBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// mixin/PistonBaseBlockMixin.java
@Mixin(PistonBaseBlock.class)
public abstract class PistonBaseBlockMixin {

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private static void onIsPushable(BlockState state, Level level, BlockPos pos,
                                     Direction pushDirection, boolean allowDestroy,
                                     Direction pistonDirection, CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof CanvasBlock) {
            // 允许画布方块被推动，我们自己处理数据保存
            cir.setReturnValue(true);
        }
    }
}