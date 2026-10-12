package com.astune.painter.api;

import com.mojang.datafixers.util.Pair;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Client-only access to the dynamic textures associated with a block entity. */
@OnlyIn(Dist.CLIENT)
public interface CanvasTextureHolder {
    @Nullable
    List<Pair<CanvasFace, ResourcesBundle>> painter$getCachedFaceTextures();

    void painter$regenerateTextures(CanvasData data);

    void painter$releaseTextures();
}
