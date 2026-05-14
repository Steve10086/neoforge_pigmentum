package com.astune.painter.client;

import com.astune.painter.api.CanvasFace;
import com.astune.painter.api.PixelMatrix;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CanvasTextureManager {
    private static final Map<ResourceLocation, DynamicTexture> TEXTURES = new ConcurrentHashMap<>();

    /**
     * 为一个 CanvasFace 生成（或获取）动态纹理。
     * 纹理标识由 pos 和方向唯一确定，更新时先释放旧纹理再生成新纹理。
     */
    public static ResourceLocation getOrUpdateTexture(CanvasFace face, BlockPos pos) {
        ResourceLocation id = textureId(pos, face.primaryFace());

        // 如果已存在则先释放旧纹理
        DynamicTexture old = TEXTURES.remove(id);
        if (old != null) {
            Minecraft.getInstance().getTextureManager().release(id);
        }

        // 生成新纹理
        PixelMatrix matrix = face.pixels();
        int w = matrix.getWidth();
        int h = matrix.getHeight();
        NativeImage image = new NativeImage(w, h, false);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = matrix.getPixel(x, y);
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                // NativeImage 使用 ABGR 顺序
                int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                image.setPixelRGBA(x, y, abgr);
            }
        }

        DynamicTexture dynTex = new DynamicTexture(image);
        Minecraft.getInstance().getTextureManager().register(id, dynTex);
        TEXTURES.put(id, dynTex);
        return id;
    }

    /**
     * 释放与某个 BlockPos 相关联的所有面的纹理（方块被移除时调用）。
     */
    public static void releaseTextures(BlockPos pos) {
        for (Direction dir : Direction.values()) {
            ResourceLocation id = textureId(pos, dir);
            DynamicTexture tex = TEXTURES.remove(id);
            if (tex != null) {
                Minecraft.getInstance().getTextureManager().release(id);
            }
        }
    }

    private static ResourceLocation textureId(BlockPos pos, Direction dir) {
        return ResourceLocation.fromNamespaceAndPath("painter",
                "canvas/" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ() + "_" + dir.getName());
    }
}