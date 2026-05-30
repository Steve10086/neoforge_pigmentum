package com.astune.painter.api;

import net.minecraft.resources.ResourceLocation;

public record ResourcesBundle(ResourceLocation[] resourceLocations) {
    @Override
    public ResourceLocation[] resourceLocations() {
        return resourceLocations;
    }
}
