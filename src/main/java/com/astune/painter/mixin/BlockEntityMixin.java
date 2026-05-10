package com.astune.painter.mixin;

import com.astune.painter.api.CanvasData;
import com.astune.painter.api.CanvasDataHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin implements CanvasDataHolder {

    @Unique
    @Nullable
    private CanvasData painter$canvasData;

    @Override
    @Nullable
    public CanvasData painter$getCanvasData() {
        return painter$canvasData;
    }

    @Override
    public void painter$setCanvasData(@Nullable CanvasData data) {
        this.painter$canvasData = data;
    }

    // 持久化保存
    @Inject(method = "saveAdditional", at = @At("HEAD"))
    private void onSaveAdditional(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        if (painter$canvasData != null) {
            CanvasData.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), painter$canvasData)
                    .result().ifPresent(encoded -> tag.put("painter_canvas", encoded));
        }
    }

    // 持久化加载
    @Inject(method = "loadAdditional", at = @At("HEAD"))
    private void onLoadAdditional(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        if (tag.contains("painter_canvas")) {
            painter$canvasData = CanvasData.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get("painter_canvas"))
                    .result().orElse(null);
        }
    }
}