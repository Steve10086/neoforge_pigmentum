package com.astune.painter.api;


import com.astune.painter.registry.ModAttachments;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

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

    /**
     * @deprecated Client rendering data is exposed by {@link CanvasTextureHolder}.
     * Kept as a compatibility shim for existing integrations.
     */
    @Deprecated
    @Nullable
    default List<Pair<CanvasFace, ResourcesBundle>> painter$getCachedFaceTextures() {
        return null;
    }

    /**
     * @deprecated Client rendering data is exposed by {@link CanvasTextureHolder}.
     * Kept as a compatibility shim for existing integrations.
     */
    @Deprecated
    default void painter$regenerateTextures(CanvasData data) {
    }

    /**
     * @deprecated Client rendering data is exposed by {@link CanvasTextureHolder}.
     * Kept as a compatibility shim for existing integrations.
     */
    @Deprecated
    default void painter$releaseTextures() {
    }
}
