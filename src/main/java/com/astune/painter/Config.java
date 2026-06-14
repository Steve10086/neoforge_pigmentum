package com.astune.painter;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue CANVAS_RENDER_LAYER_OFFSET = BUILDER
            .comment("Z-offset between canvas rendering layers in world units.",
                     "Each layer is offset by this amount from the previous one to prevent z-fighting.",
                     "Default: 0.01")
            .defineInRange("canvasRenderLayerOffset", 0.001, 0.0, 1.0);

    static final ModConfigSpec SPEC = BUILDER.build();
}
