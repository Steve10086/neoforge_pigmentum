package com.astune.painter.api;


import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;

public interface CanvasDataHolder {
    @Nullable
    CanvasData painter$getCanvasData();
    void painter$setCanvasData(@Nullable CanvasData data);

    @Nullable
    Map<Direction, ResourceLocation> painter$getCachedFaceTextures();

    void painter$regenerateTextures(CanvasData data);

    void painter$releaseTextures();
}