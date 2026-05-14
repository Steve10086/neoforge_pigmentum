package com.astune.painter.mixin;

import com.astune.painter.api.CanvasData;
import com.astune.painter.api.CanvasDataHolder;
import com.astune.painter.api.CanvasFace;
import com.astune.painter.client.CanvasTextureManager;
import com.astune.painter.network.ClientCanvasCache;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin implements CanvasDataHolder {

    @Unique
    @Nullable
    private CanvasData painter$canvasData;
    @Unique
    @Nullable
    private Map<Direction, ResourceLocation> painter$cachedFaceTextures;
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
            BlockEntity self = (BlockEntity) (Object) this;
            if (self.getLevel() != null && self.getLevel().isClientSide && painter$canvasData != null) {
                System.out.println("[Mixin] canvasData after load: " + (painter$canvasData != null));
                painter$regenerateTextures(painter$canvasData);
            }
        }
    }

    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void onSetRemoved(CallbackInfo ci) {
        BlockEntity self = (BlockEntity) (Object) this;
        if (self.getLevel() != null && self.getLevel().isClientSide) {
            painter$releaseTextures();
            painter$cachedFaceTextures = null;
            ClientCanvasCache.removeCanvas(self.getBlockPos());
            ClientCanvasCache.removeMimickedState(self.getBlockPos());
        }
    }

    @Override
    public Map<Direction, ResourceLocation> painter$getCachedFaceTextures() {
        return painter$cachedFaceTextures;
    }

    @Override
    public void painter$regenerateTextures(CanvasData data) {
        if (data == null) {
            painter$releaseTextures();
            this.painter$cachedFaceTextures = null;
            return;
        }
        Map<Direction, ResourceLocation> newMap = new HashMap<>();
        BlockEntity self = (BlockEntity) (Object) this;
        for (CanvasFace face : data.faces()) {
            ResourceLocation tex = CanvasTextureManager.getOrUpdateTexture(face, self.getBlockPos());
            newMap.put(face.primaryFace(), tex);
        }
        if (painter$cachedFaceTextures != null) {
            for (Map.Entry<Direction, ResourceLocation> old : painter$cachedFaceTextures.entrySet()) {
                if (!newMap.containsKey(old.getKey())) {
                    CanvasTextureManager.releaseTextures(self.getBlockPos());
                }
            }
        }
        this.painter$cachedFaceTextures = newMap;
    }

    @Override
    public void painter$releaseTextures() {
        if (painter$cachedFaceTextures != null) {
            BlockEntity self = (BlockEntity) (Object) this;
            CanvasTextureManager.releaseTextures(self.getBlockPos());
        }
    }
}