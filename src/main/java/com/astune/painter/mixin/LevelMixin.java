package com.astune.painter.mixin;

import com.astune.painter.util.CanvasBlockSetController;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"), cancellable = true)
    private void onSetBlock(BlockPos pos, BlockState newState, int flags, int recursionLeft,
                            CallbackInfoReturnable<Boolean> cir) {
        Level self = (Level) (Object) this;
        BlockState oldState = self.getBlockState(pos);

        if (CanvasBlockSetController.shouldAllowVanillaReplace(self, pos, newState)) {
            return;
        }

        Boolean result = CanvasBlockSetController.tryProxyMimickedSetBlock(self, pos, oldState, newState, flags, recursionLeft);
        if (result != null) {
            cir.setReturnValue(result);
        }
    }
}
