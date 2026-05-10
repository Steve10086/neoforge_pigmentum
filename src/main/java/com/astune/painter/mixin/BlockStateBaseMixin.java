package com.astune.painter.mixin;

import com.astune.painter.block.CanvasBlock;
import com.astune.painter.block.CanvasBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {

    @Shadow
    public abstract Block getBlock();

    private CanvasBlockEntity painter$getBE(BlockGetter level, BlockPos pos) {
        if (level == null || pos == null) return null;
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof CanvasBlockEntity ? (CanvasBlockEntity) be : null;
    }

    private BlockState painter$getMimicked(BlockGetter level, BlockPos pos) {
        CanvasBlockEntity be = painter$getBE(level, pos);
        return be != null ? be.getMimickedState() : null;
    }

    @Inject(method = "getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("HEAD"), cancellable = true)
    private void onGetShape(BlockGetter level, BlockPos pos, CollisionContext ctx, CallbackInfoReturnable<VoxelShape> cir) {
        if (getBlock() instanceof CanvasBlock) {
            BlockState m = painter$getMimicked(level, pos);
            if (m != null) cir.setReturnValue(m.getShape(level, pos, ctx));
        }
    }

    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("HEAD"), cancellable = true)
    private void onGetCollisionShape(BlockGetter level, BlockPos pos, CollisionContext ctx, CallbackInfoReturnable<VoxelShape> cir) {
        if (getBlock() instanceof CanvasBlock) {
            BlockState m = painter$getMimicked(level, pos);
            if (m != null) cir.setReturnValue(m.getCollisionShape(level, pos, ctx));
        }
    }

    @Inject(method = "getVisualShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("HEAD"), cancellable = true)
    private void onGetVisualShape(BlockGetter level, BlockPos pos, CollisionContext ctx, CallbackInfoReturnable<VoxelShape> cir) {
        if (getBlock() instanceof CanvasBlock) {
            BlockState m = painter$getMimicked(level, pos);
            if (m != null) cir.setReturnValue(m.getVisualShape(level, pos, ctx));
        }
    }

    @Inject(method = "getOcclusionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("HEAD"), cancellable = true)
    private void onGetOcclusionShape(BlockGetter level, BlockPos pos, CallbackInfoReturnable<VoxelShape> cir) {
        if (getBlock() instanceof CanvasBlock) {
            BlockState m = painter$getMimicked(level, pos);
            if (m != null) cir.setReturnValue(m.getOcclusionShape(level, pos));
        }
    }

    // 支撑形状（实体推挤、跳跃检测等）
    @Inject(method = "getBlockSupportShape", at = @At("HEAD"), cancellable = true)
    private void onGetBlockSupportShape(BlockGetter level, BlockPos pos, CallbackInfoReturnable<VoxelShape> cir) {
        if (getBlock() instanceof CanvasBlock) {
            BlockState m = painter$getMimicked(level, pos);
            if (m != null) cir.setReturnValue(m.getBlockSupportShape(level, pos));
        }
    }

    // 交互形状（较少用，但为完整性）
    @Inject(method = "getInteractionShape", at = @At("HEAD"), cancellable = true)
    private void onGetInteractionShape(BlockGetter level, BlockPos pos, CallbackInfoReturnable<VoxelShape> cir) {
        if (getBlock() instanceof CanvasBlock) {
            BlockState m = painter$getMimicked(level, pos);
            if (m != null) cir.setReturnValue(m.getInteractionShape(level, pos));
        }
    }

    // 完整碰撞箱判断（影响推挤和路径寻找）
    @Inject(method = "isCollisionShapeFullBlock", at = @At("HEAD"), cancellable = true)
    private void onIsCollisionShapeFullBlock(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (getBlock() instanceof CanvasBlock) {
            BlockState m = painter$getMimicked(level, pos);
            if (m != null) cir.setReturnValue(m.isCollisionShapeFullBlock(level, pos));
        }
    }
}