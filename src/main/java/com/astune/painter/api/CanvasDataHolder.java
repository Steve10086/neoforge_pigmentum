package com.astune.painter.api;


import javax.annotation.Nullable;

import com.astune.painter.registry.ModAttachments;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public interface CanvasDataHolder {
    @Nullable
    default CanvasData painter$getCanvasData() {
        BlockEntity self = (BlockEntity) this;
        return self.getData(ModAttachments.CANVAS_DATA);
    }

    default void painter$setCanvasData(CanvasData data) {
        BlockEntity self = (BlockEntity) this;
        self.setData(ModAttachments.CANVAS_DATA, data);
    }

    @Nullable
    List<Pair<CanvasFace, ResourceLocation>> painter$getCachedFaceTextures();

    void painter$regenerateTextures(CanvasData data);

    void painter$releaseTextures();
}