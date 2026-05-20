package com.astune.painter.api;


import javax.annotation.Nullable;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public interface CanvasDataHolder {
    @Nullable
    CanvasData painter$getCanvasData();
    void painter$setCanvasData(@Nullable CanvasData data);

    void painter$addCanvasData(@Nullable CanvasData data);

    @Nullable
    List<Pair<CanvasFace, ResourceLocation>> painter$getCachedFaceTextures();

    void painter$regenerateTextures(CanvasData data);

    void painter$releaseTextures();
}