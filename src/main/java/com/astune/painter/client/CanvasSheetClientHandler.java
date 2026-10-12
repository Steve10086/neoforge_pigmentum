package com.astune.painter.client;

import com.astune.painter.api.CanvasFace;
import com.astune.painter.api.PixelMatrix;
import com.astune.painter.item.CanvasSheetClientBridge;
import com.astune.painter.registry.ModDataComponents;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public final class CanvasSheetClientHandler {
    private CanvasSheetClientHandler() {
    }

    public static void registerHooks() {
        CanvasSheetClientBridge.registerClientHandlers(
                CanvasSheetClientHandler::ensureTexture,
                CanvasSheetClientHandler::releaseTexture
        );
    }

    public static void ensureTexture(ItemStack stack) {
        if (!stack.has(ModDataComponents.STORED_FACE.get())) {
            releaseTextureFromStack(stack);
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        String textureString = stack.get(ModDataComponents.CANVAS_TEXTURE.get());
        ResourceLocation current = textureString != null ? ResourceLocation.tryParse(textureString) : null;
        if (current != null && minecraft.getTextureManager().getTexture(current) != null) {
            return;
        }
        if (current != null) {
            minecraft.getTextureManager().release(current);
        }

        CanvasFace face = stack.get(ModDataComponents.STORED_FACE.get());
        PixelMatrix pixels = face.pixels();
        if (pixels == null) {
            return;
        }

        int sourceWidth = pixels.getWidth();
        int sourceHeight = pixels.getHeight();
        int maxDimension = Math.max(Math.max(sourceWidth, sourceHeight), 16);
        NativeImage image = new NativeImage(maxDimension, maxDimension, false);
        int offsetX = (maxDimension - sourceWidth) / 2;
        int offsetY = (maxDimension - sourceHeight) / 2;

        for (int y = 0; y < sourceHeight; y++) {
            for (int x = 0; x < sourceWidth; x++) {
                image.setPixelRGBA(offsetX + x, offsetY + y, argbToAbgr(pixels.getPixel(x, y)));
            }
        }

        DynamicTexture dynamicTexture = new DynamicTexture(image);
        ResourceLocation newId = ResourceLocation.fromNamespaceAndPath("painter", "sheet/" + UUID.randomUUID());
        minecraft.getTextureManager().register(newId, dynamicTexture);
        stack.set(ModDataComponents.CANVAS_TEXTURE.get(), newId.toString());
    }

    public static void releaseTexture(ResourceLocation location) {
        Minecraft.getInstance().getTextureManager().release(location);
    }

    private static void releaseTextureFromStack(ItemStack stack) {
        String textureString = stack.get(ModDataComponents.CANVAS_TEXTURE.get());
        if (textureString != null) {
            ResourceLocation location = ResourceLocation.tryParse(textureString);
            if (location != null) {
                releaseTexture(location);
            }
            stack.remove(ModDataComponents.CANVAS_TEXTURE.get());
        }
    }

    private static int argbToAbgr(int argb) {
        int alpha = (argb >>> 24) & 0xFF;
        int red = (argb >>> 16) & 0xFF;
        int green = (argb >>> 8) & 0xFF;
        int blue = argb & 0xFF;
        return (alpha << 24) | (blue << 16) | (green << 8) | red;
    }
}
