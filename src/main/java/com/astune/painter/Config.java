package com.astune.painter;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue CANVAS_RENDER_LAYER_OFFSET = BUILDER
            .comment("Z-offset between canvas rendering layers in world units.",
                     "Each layer is offset by this amount from the previous one to prevent z-fighting.",
                     "Default: 0.001")
            .defineInRange("canvasRenderLayerOffset", 0.001, 0.0, 1.0);

    public static final ModConfigSpec.IntValue STROKE_UNDO_HISTORY_LIMIT = BUILDER
            .comment("Maximum number of stroke undo records kept globally.",
                     "Default: 20")
            .defineInRange("strokeUndoHistoryLimit", 20, 0, 1024);

    public static final ModConfigSpec.IntValue STROKE_REDO_HISTORY_LIMIT = BUILDER
            .comment("Maximum number of stroke redo records kept globally.",
                     "Default: 10")
            .defineInRange("strokeRedoHistoryLimit", 10, 0, 1024);

    static final ModConfigSpec SPEC = BUILDER.build();
}
