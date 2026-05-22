package com.astune.painter.mixin;

import com.astune.painter.api.CanvasData;
import com.astune.painter.api.CanvasDataHolder;
import com.astune.painter.api.CanvasFace;
import com.astune.painter.client.CanvasTextureManager;
import com.astune.painter.network.ClientCanvasCache;
import com.astune.painter.registry.ModDataComponents;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


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
    private List<Pair<CanvasFace, ResourceLocation>> painter$cachedFaceTextures;


    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void onSetRemoved(CallbackInfo ci) {
        BlockEntity self = (BlockEntity) (Object) this;
        if (self.getLevel() != null && self.getLevel().isClientSide) {
            painter$releaseTextures();
            painter$cachedFaceTextures = null;
            painter$clientTextureId = -1;
            ClientCanvasCache.removeCanvas(self.getBlockPos());
        }
    }

    @Override
    public List<Pair<CanvasFace, ResourceLocation>> painter$getCachedFaceTextures() {
        return painter$cachedFaceTextures;
    }

    @Override
    public void painter$regenerateTextures(CanvasData data) {
        if (painter$clientTextureId == -1) {
            painter$clientTextureId = CanvasTextureManager.NEXT_TEXTURE_ID++;
        }
        if (data == null) {
            CanvasTextureManager.releaseTextures(painter$clientTextureId);
            this.painter$cachedFaceTextures = null;
            return;
        }

        // 释放旧的所有面纹理
        painter$releaseTextures();

        //System.out.println("regenerate !");

        List<Pair<CanvasFace, ResourceLocation>> newList = new ArrayList<>();
        int faceIndex = 0;
        for (CanvasFace face : data.faces()) {
            if (face.pixels() == null || face.pixels().getWidth() <= 0 || face.pixels().getHeight() <= 0 || Arrays.stream(face.pixels().getPixels()).allMatch(a -> a == 0))
                continue;
            ResourceLocation tex = CanvasTextureManager.getOrUpdateTexture(face, painter$clientTextureId, faceIndex);
            if (tex != null) {
                newList.add(Pair.of(face, tex));
            }
            faceIndex++;
        }
        this.painter$cachedFaceTextures = newList.isEmpty() ? null : newList;
        //System.out.println(newList);
    }

    @Override
    public void painter$releaseTextures() {
        // 释放所有以当前 entityId 为前缀的纹理
        CanvasTextureManager.releaseTextures(painter$clientTextureId);
    }
}