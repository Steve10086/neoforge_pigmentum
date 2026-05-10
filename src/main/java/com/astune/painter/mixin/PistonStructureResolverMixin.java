package com.astune.painter.mixin;

import com.astune.painter.block.CanvasBlock;
import com.astune.painter.block.CanvasBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.astune.painter.network.CanvasPistonDataCache;

import java.util.List;

@Mixin(PistonStructureResolver.class)
public abstract class PistonStructureResolverMixin {

    @Shadow
    @Final
    private Level level;
    @Shadow @Final private List<BlockPos> toPush;
    @Shadow @Final private Direction pushDirection;
    @Inject(method = "resolve", at = @At("RETURN"))
    private void onResolve(CallbackInfoReturnable<Boolean> cir) {
        // 只有 resolve 成功才处理
        if (!cir.getReturnValue()) return;


        if (level.isClientSide) return;

        List<BlockPos> toPush = this.toPush;
        Direction direction = this.pushDirection;

        for (BlockPos pos : toPush) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof CanvasBlock) {
                CanvasBlockEntity be = (CanvasBlockEntity) level.getBlockEntity(pos);
                if (be != null) {
                    CompoundTag data = be.saveWithoutMetadata(level.registryAccess());
                    BlockPos newPos = pos.relative(direction);
                    CanvasPistonDataCache.store(newPos, data);
                }
            }
        }
    }
}
