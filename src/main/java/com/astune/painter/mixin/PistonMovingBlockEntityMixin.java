package com.astune.painter.mixin;

import com.astune.painter.block.CanvasBlock;
import com.astune.painter.block.CanvasBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(PistonMovingBlockEntity.class)
public abstract class PistonMovingBlockEntityMixin extends BlockEntity {
    // 这行不能省略，因为 Mixin 需要继承目标类的父类

    @Unique
    @Nullable
    private BlockState painter$mimickedState;

    public PistonMovingBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    // 构造函数注入：在所有构造函数最后调用
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstructed(BlockPos pos, BlockState state, BlockState movedState,
                               Direction dir, boolean extending, boolean source, CallbackInfo ci) {
        if (!(movedState.getBlock() instanceof CanvasBlock) || level == null || level.isClientSide) return;
        // 从原位置获取 CanvasBlockEntity 的数据（此时原实体还未被移除，因为PistonMovingBlockEntity创建后原实体才会被覆盖）
        BlockEntity oldBE = level.getBlockEntity(pos);
        if (oldBE instanceof CanvasBlockEntity canvasBE) {
            this.painter$mimickedState = canvasBE.getMimickedState();
        }
    }

    // 序列化到 NBT（用于网络同步）
    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void onSave(CompoundTag tag, CallbackInfo ci) {
        if (painter$mimickedState != null && getMovedState().getBlock() instanceof CanvasBlock) {
            tag.put("painter_mimicked", NbtUtils.writeBlockState(painter$mimickedState));
        }
    }

    // 客户端反序列化（注入父类 load 方法）
    @Inject(method = "load", at = @At("TAIL"))
    private void onLoad(CompoundTag tag, CallbackInfo ci) {
        if (level != null && level.isClientSide && tag.contains("painter_mimicked")) {
            this.painter$mimickedState = NbtUtils.readBlockState(
                    level.registryAccess().lookupOrThrow(Registries.BLOCK),
                    tag.getCompound("painter_mimicked")
            );
        }
    }

    // 暴露 mimickedState 的 getter
    @Unique
    public BlockState painter$getMimickedState() {
        return painter$mimickedState;
    }

    // 保留原版 movedState 访问（从父类私有字段通过 @Accessor 获取）
    @Accessor("movedState")
    public abstract BlockState getMovedState();
}
