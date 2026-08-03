package com.astune.painter.mixin;

import com.astune.painter.api.CanvasData;
import com.astune.painter.api.CanvasDataHolder;
import com.astune.painter.api.CanvasFace;
import com.astune.painter.api.ResourcesBundle;
import com.astune.painter.client.CanvasTextureManager;
import com.astune.painter.network.ClientCanvasCache;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin implements CanvasDataHolder {

    @Unique
    private int painter$clientTextureId = -1;

    @Shadow
    protected Level level;
    @Shadow
    BlockPos worldPosition;

    @Unique
    @Nullable
    private List<Pair<CanvasFace, ResourcesBundle>> painter$cachedFaceTextures;


    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void onSetRemoved(CallbackInfo ci) {
        BlockEntity self = (BlockEntity) (Object) this;
        if (self.getLevel() != null && self.getLevel().isClientSide) {
            //System.out.println("removed!");
            painter$releaseTextures();
            painter$cachedFaceTextures = null;
            painter$clientTextureId = -1;
            ClientCanvasCache.removeCanvas(self.getBlockPos());
        }
    }

    @Override
    public List<Pair<CanvasFace, ResourcesBundle>> painter$getCachedFaceTextures() {
        return painter$cachedFaceTextures;
    }

    @Override
    public void painter$regenerateTextures(CanvasData data) {
        BlockEntity self = (BlockEntity) (Object) this;
        if(self.getLevel() == null || !self.getLevel().isClientSide) return;

        if (painter$clientTextureId == -1) {
            painter$clientTextureId = CanvasTextureManager.NEXT_TEXTURE_ID++;
        }
        if (data == null) {
            CanvasTextureManager.releaseTextures(painter$clientTextureId);
            painter$cachedFaceTextures = null;
            return;
        }

        List<Pair<CanvasFace, ResourcesBundle>> newList = new ArrayList<>();
        Set<Integer> retainedFaces = new HashSet<>();
        for (int faceIndex = 0; faceIndex < data.faces().size(); faceIndex++) {
            CanvasFace face = data.faces().get(faceIndex);
            retainedFaces.add(faceIndex);
            if (face.pixels() == null || face.pixels().getWidth() <= 0 || face.pixels().getHeight() <= 0 || Arrays.stream(face.pixels().getPixels()).allMatch(a -> a == 0)) {
                CanvasTextureManager.releaseTexture(painter$clientTextureId, faceIndex);
                continue;
            }
            ResourcesBundle tex = CanvasTextureManager.createOrUpdateTexture(face, painter$clientTextureId, faceIndex);
            if (tex != null && tex.resourceLocations().length > 0) {
                newList.add(Pair.of(face, tex));
            }
        }
        CanvasTextureManager.releaseUnusedFaces(painter$clientTextureId, retainedFaces);
        this.painter$cachedFaceTextures = newList.isEmpty() ? null : newList;
    }

    @Override
    public void painter$releaseTextures() {
        // 释放所有以当前 entityId 为前缀的纹理
        CanvasTextureManager.releaseTextures(painter$clientTextureId);
    }
}
