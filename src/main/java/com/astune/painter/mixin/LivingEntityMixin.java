package com.astune.painter.mixin;

import com.astune.painter.block.CanvasBlock;
import com.astune.painter.block.CanvasBlockEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @ModifyArg(
            method = "checkFallDamage(DZLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/particles/BlockParticleOption;<init>(Lnet/minecraft/core/particles/ParticleType;Lnet/minecraft/world/level/block/state/BlockState;)V"
            ),
            index = 1
    )
    private BlockState painter$useMimickedStateForLandingParticle(BlockState state) {
        if (!(state.getBlock() instanceof CanvasBlock)) {
            return state;
        }

        LivingEntity entity = (LivingEntity) (Object) this;
        BlockEntity blockEntity = entity.level().getBlockEntity(entity.getOnPos());
        if (blockEntity instanceof CanvasBlockEntity canvasBE) {
            BlockState mimicked = canvasBE.getMimickedState();
            if (mimicked != null) {
                return mimicked;
            }
        }
        return state;
    }
}
