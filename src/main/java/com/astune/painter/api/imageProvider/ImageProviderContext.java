package com.astune.painter.api.imageProvider;

import com.astune.painter.api.CanvasFace;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Context object passed to {@link CanvasImageProvider#canProvide(ImageProviderContext)}.
 * Contains the face being processed and optional world context for extensibility
 * (e.g. biome-aware or dimension-aware image providers).
 */
public class ImageProviderContext {
    public final CanvasFace face;
    @Nullable
    public final Level level;
    @Nullable
    public final BlockPos pos;

    public ImageProviderContext(CanvasFace face, @Nullable Level level, @Nullable BlockPos pos) {
        this.face = face;
        this.level = level;
        this.pos = pos;
    }
}
