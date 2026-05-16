package com.astune.painter.mixin;

import com.astune.painter.api.CanvasData;
import com.astune.painter.api.CanvasDataHolder;
import com.astune.painter.api.CanvasFace;
import com.astune.painter.client.CanvasTextureManager;
import com.astune.painter.network.ClientCanvasCache;
import com.mojang.datafixers.util.Pair;
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

import java.util.ArrayList;
import java.util.List;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin implements CanvasDataHolder {

    @Unique
    private int painter$clientTextureId = -1;

    @Unique
    private static int NEXT_TEXTURE_ID = 0;

    @Unique
    @Nullable
    private CanvasData painter$canvasData;
    @Unique
    @Nullable
    private List<Pair<CanvasFace, ResourceLocation>> painter$cachedFaceTextures;
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
                if (painter$clientTextureId == -1) {
                    painter$clientTextureId = NEXT_TEXTURE_ID++;
                }
                painter$regenerateTextures(painter$canvasData);
            }
        }
    }

    @Inject(method = "onLoad", at = @At("HEAD"))
    private void onLoad(CallbackInfo ci) {
        BlockEntity self = (BlockEntity) (Object) this;
        if (self.getLevel() != null && self.getLevel().isClientSide) {
            if (painter$clientTextureId == -1) {
                painter$clientTextureId = NEXT_TEXTURE_ID++;
            }
        }
    }

    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void onSetRemoved(CallbackInfo ci) {
        BlockEntity self = (BlockEntity) (Object) this;
        if (self.getLevel() != null && self.getLevel().isClientSide) {
            painter$releaseTextures();
            painter$cachedFaceTextures = null;
            painter$clientTextureId = -1;
            ClientCanvasCache.removeCanvas(self.getBlockPos());
            ClientCanvasCache.removeMimickedState(self.getBlockPos());
        }
    }

    @Override
    public List<Pair<CanvasFace, ResourceLocation>> painter$getCachedFaceTextures() {
        return painter$cachedFaceTextures;
    }

    @Override
    public void painter$regenerateTextures(CanvasData data) {
        if (data == null) {
            CanvasTextureManager.releaseTextures(painter$clientTextureId);
            this.painter$cachedFaceTextures = null;
            return;
        }

        // 释放旧的所有面纹理
        painter$releaseTextures();

        List<Pair<CanvasFace, ResourceLocation>> newList = new ArrayList<>();
        int faceIndex = 0;
        for (CanvasFace face : data.faces()) {
            if (face.pixels() == null || face.pixels().getWidth() <= 0 || face.pixels().getHeight() <= 0)
                continue;
            ResourceLocation tex = CanvasTextureManager.getOrUpdateTexture(face, painter$clientTextureId, faceIndex);
            if (tex != null) {
                newList.add(Pair.of(face, tex));
            }
            faceIndex++;
        }
        this.painter$cachedFaceTextures = newList.isEmpty() ? null : newList;
    }

    @Override
    public void painter$releaseTextures() {
        // 释放所有以当前 entityId 为前缀的纹理
        CanvasTextureManager.releaseTextures(painter$clientTextureId);
    }
}