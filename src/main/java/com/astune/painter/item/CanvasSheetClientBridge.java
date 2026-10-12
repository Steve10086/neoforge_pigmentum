package com.astune.painter.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/** Common-safe access to canvas-sheet texture operations implemented on the client. */
public final class CanvasSheetClientBridge {
    private static volatile Consumer<ItemStack> textureEnsurer = stack -> {
    };
    private static volatile Consumer<ResourceLocation> textureReleaser = location -> {
    };

    private CanvasSheetClientBridge() {
    }

    public static void registerClientHandlers(Consumer<ItemStack> textureEnsurer,
                                              Consumer<ResourceLocation> textureReleaser) {
        CanvasSheetClientBridge.textureEnsurer = textureEnsurer;
        CanvasSheetClientBridge.textureReleaser = textureReleaser;
    }

    public static void ensureTexture(ItemStack stack) {
        textureEnsurer.accept(stack);
    }

    public static void releaseTexture(ResourceLocation location) {
        textureReleaser.accept(location);
    }
}
