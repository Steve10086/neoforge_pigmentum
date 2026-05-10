package com.astune.painter.api;


import javax.annotation.Nullable;

public interface CanvasDataHolder {
    @Nullable
    CanvasData painter$getCanvasData();
    void painter$setCanvasData(@Nullable CanvasData data);
}