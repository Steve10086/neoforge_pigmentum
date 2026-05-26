package com.astune.painter.api;

import com.astune.painter.api.blend.BlendFunction;
import com.astune.painter.api.blend.DefaultBlendFunctions;

public enum BlendMode {
    OVERWRITE,
    ADD,
    MULTIPLY,
    ERASE;

    public String getTranslationKey() {
        return "painter.config.blend_mode." + this.name().toLowerCase();
    }

    public BlendFunction getDefaultFunction() {
        return switch (this) {
            case OVERWRITE -> DefaultBlendFunctions.OVERWRITE;
            case ADD -> DefaultBlendFunctions.ADD;
            case MULTIPLY -> DefaultBlendFunctions.MULTIPLY;
            case ERASE -> DefaultBlendFunctions.ERASE;
        };
    }
}
