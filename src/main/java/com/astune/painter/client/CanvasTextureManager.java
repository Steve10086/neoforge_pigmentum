package com.astune.painter.client;

import com.astune.painter.api.CanvasFace;
import com.astune.painter.api.ResourcesBundle;
import com.astune.painter.api.imageProvider.CanvasImageProvider;
import com.astune.painter.api.imageProvider.CanvasImageProviderRegistry;
import com.astune.painter.api.imageProvider.ImageProviderContext;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@OnlyIn(Dist.CLIENT)
public final class CanvasTextureManager {
    private static final Map<TextureKey, TextureEntry> ACTIVE = new ConcurrentHashMap<>();
    public static int NEXT_TEXTURE_ID = 0;

    private CanvasTextureManager() {}

    public static NativeImage createImage(CanvasFace face, CanvasImageProvider provider) {
        return provider.createImage(face);
    }

    public static ResourcesBundle createOrUpdateTexture(CanvasFace face, int entityId, int faceIndex) {
        List<ResourceLocation> resources = new ArrayList<>();
        Set<TextureKey> retained = new HashSet<>();
        ImageProviderContext context = new ImageProviderContext(face, null, null);

        for (CanvasImageProvider provider : CanvasImageProviderRegistry.resolveAll(context)) {
            NativeImage image = provider.createImage(face);
            if (image == null) continue;

            TextureKey key = new TextureKey(entityId, faceIndex, provider.name());
            retained.add(key);

            TextureEntry entry = ACTIVE.get(key);
            if (entry == null) {
                ResourceLocation location = stableLocation(key);
                DynamicTexture texture = new DynamicTexture(image);
                entry = new TextureEntry(location, texture);
                ACTIVE.put(key, entry);
                Minecraft.getInstance().getTextureManager().register(location, texture);
            } else {
                updateTexture(key, entry, image);
            }
            resources.add(entry.location);
        }

        releaseMatching(key -> key.entityId == entityId
                && key.faceIndex == faceIndex
                && !retained.contains(key));

        return new ResourcesBundle(resources.toArray(new ResourceLocation[0]));
    }

    public static void releaseTexture(int entityId, int faceIndex) {
        releaseMatching(key -> key.entityId == entityId && key.faceIndex == faceIndex);
    }

    public static void releaseUnusedFaces(int entityId, Set<Integer> retainedFaces) {
        releaseMatching(key -> key.entityId == entityId && !retainedFaces.contains(key.faceIndex));
    }

    public static void releaseTextures(int entityId) {
        if (entityId < 0) return;
        releaseMatching(key -> key.entityId == entityId);
    }

    private static void updateTexture(TextureKey key, TextureEntry entry, NativeImage image) {
        Runnable update = () -> {
            if (ACTIVE.get(key) != entry) {
                image.close();
                return;
            }

            NativeImage current = entry.texture.getPixels();
            if (current != null
                    && current.getWidth() == image.getWidth()
                    && current.getHeight() == image.getHeight()) {
                entry.texture.setPixels(image);
                entry.texture.upload();
                return;
            }

            DynamicTexture replacement = new DynamicTexture(image);
            Minecraft.getInstance().getTextureManager().register(entry.location, replacement);
            entry.texture = replacement;
        };

        if (RenderSystem.isOnRenderThread()) {
            update.run();
        } else {
            RenderSystem.recordRenderCall(update::run);
        }
    }

    private static void releaseMatching(Predicate<TextureKey> predicate) {
        List<ResourceLocation> releasedLocations = new ArrayList<>();
        ACTIVE.entrySet().removeIf(mapEntry -> {
            if (!predicate.test(mapEntry.getKey())) return false;
            releasedLocations.add(mapEntry.getValue().location);
            return true;
        });
        for (ResourceLocation location : releasedLocations) {
            CanvasRenderTypes.release(location);
            releaseLocationWhenUnused(location);
        }
    }

    private static void releaseLocationWhenUnused(ResourceLocation location) {
        Runnable release = () -> {
            boolean reused = ACTIVE.values().stream().anyMatch(entry -> entry.location.equals(location));
            if (!reused) {
                Minecraft.getInstance().getTextureManager().release(location);
            }
        };

        if (RenderSystem.isOnRenderThread()) {
            release.run();
        } else {
            RenderSystem.recordRenderCall(release::run);
        }
    }

    private static ResourceLocation stableLocation(TextureKey key) {
        String providerName = key.providerName.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9/._-]", "_");
        if (providerName.isEmpty()) providerName = "provider";
        return ResourceLocation.fromNamespaceAndPath(
                "painter",
                "canvas/" + key.entityId + "_" + key.faceIndex + "_" + providerName + "_"
                        + Integer.toUnsignedString(key.providerName.hashCode(), 36) + "_texture"
        );
    }

    private record TextureKey(int entityId, int faceIndex, String providerName) {}

    private static final class TextureEntry {
        private final ResourceLocation location;
        private DynamicTexture texture;

        private TextureEntry(ResourceLocation location, DynamicTexture texture) {
            this.location = location;
            this.texture = texture;
        }
    }
}
