package com.astune.painter.mixin;

import com.astune.painter.api.CanvasData;
import com.astune.painter.api.CanvasFace;
import com.astune.painter.api.CanvasTextureHolder;
import com.astune.painter.api.ResourcesBundle;
import com.astune.painter.client.CanvasTextureManager;
import com.astune.painter.network.ClientCanvasCache;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
@Mixin(BlockEntity.class)
public abstract class BlockEntityClientMixin implements CanvasTextureHolder {
    @Unique
    private int painter$clientTextureId = -1;

    @Unique
    @Nullable
    private List<Pair<CanvasFace, ResourcesBundle>> painter$cachedFaceTextures;

    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void painter$releaseClientTextures(CallbackInfo ci) {
        BlockEntity self = (BlockEntity) (Object) this;
        if (self.getLevel() != null && self.getLevel().isClientSide) {
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
        if (self.getLevel() == null || !self.getLevel().isClientSide) {
            return;
        }

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
            if (face.pixels() == null || face.pixels().getWidth() <= 0 || face.pixels().getHeight() <= 0
                    || Arrays.stream(face.pixels().getPixels()).allMatch(pixel -> pixel == 0)) {
                CanvasTextureManager.releaseTexture(painter$clientTextureId, faceIndex);
                continue;
            }
            ResourcesBundle texture = CanvasTextureManager.createOrUpdateTexture(face, painter$clientTextureId, faceIndex);
            if (texture != null && texture.resourceLocations().length > 0) {
                newList.add(Pair.of(face, texture));
            }
        }
        CanvasTextureManager.releaseUnusedFaces(painter$clientTextureId, retainedFaces);
        painter$cachedFaceTextures = newList.isEmpty() ? null : newList;
    }

    @Override
    public void painter$releaseTextures() {
        CanvasTextureManager.releaseTextures(painter$clientTextureId);
    }
}
