package com.astune.painter.api;

public enum BlendMode {
    OVERWRITE,
    ADD,
    MULTIPLY,
    ERASE;

    public String getTranslationKey() {
        return "painter.config.blend_mode." + this.name().toLowerCase();
    }
}
