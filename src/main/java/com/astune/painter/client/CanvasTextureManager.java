package com.astune.painter.client;

import com.astune.painter.api.CanvasFace;
import com.astune.painter.api.PixelMatrix;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CanvasTextureManager {
    // 键: "entityId_directionName", 值: 当前有效的 ResourceLocation
    private static final Map<String, ResourceLocation> ACTIVE = new ConcurrentHashMap<>();
    private static long COUNTER = 0;

    /**
     * 生成或更新指定实体某个方向的画布纹理。
     * @param entityId 客户端侧 CanvasBlockEntity 的唯一 ID
     * @return 新纹理的 ResourceLocation，失败返回 null
     */
    public static ResourceLocation getOrUpdateTexture(CanvasFace face, int entityId, int faceIndex) {
        String key = key(entityId, faceIndex);

        NativeImage image = createNativeImage(face);
        if (image == null) return null;

        long id = COUNTER++;
        ResourceLocation newLoc = ResourceLocation.fromNamespaceAndPath("painter",
                "canvas/" + entityId + "_" + faceIndex + "_" + id);

        DynamicTexture dynTex = new DynamicTexture(image);
        Minecraft.getInstance().getTextureManager().register(newLoc, dynTex);

        ResourceLocation oldLoc = ACTIVE.put(key, newLoc);
        if (oldLoc != null) {
            RenderSystem.recordRenderCall(() -> {
                Minecraft.getInstance().getTextureManager().release(oldLoc);
            });
        }
        return newLoc;
    }

    /** 释放指定实体的某个面 */
    public static void releaseTexture(int entityId, int faceIndex) {
        String key = key(entityId, faceIndex);
        ResourceLocation loc = ACTIVE.remove(key);
        if (loc != null) {
            RenderSystem.recordRenderCall(() -> {
                Minecraft.getInstance().getTextureManager().release(loc);
            });
        }
    }

    /** 释放指定实体所有面 */
    public static void releaseTextures(int entityId) {
        ACTIVE.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(entityId + "_")) {
                RenderSystem.recordRenderCall(() -> {
                    Minecraft.getInstance().getTextureManager().release(entry.getValue());
                });
                return true;
            }
            return false;
        });
    }

    private static String key(int entityId, int faceIndex) {
        return entityId + "_" + faceIndex;
    }

    private static NativeImage createNativeImage(CanvasFace face) {
        PixelMatrix matrix = face.pixels();
        if (matrix == null || matrix.getWidth() <= 0 || matrix.getHeight() <= 0) return null;
        int w = matrix.getWidth(), h = matrix.getHeight();
        NativeImage image = null;
        try {
            image = new NativeImage(w, h, false);
            image.getPixelRGBA(0, 0); // 触发分配检查
        } catch (Exception e) {
            if (image != null) image.close();
            return null;
        }

        try {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int argb = matrix.getPixel(x, y);
                    int a = (argb >> 24) & 0xFF;
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;

                    int bgr = (b << 16) | (g << 8) | r;
                    int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                    if (bgr != 0){
                        abgr = 255 << 24 | bgr;
                        //System.out.println("[CanvasTextureManager] " + a + "," + r + "," + g + "," + b + " to " + abgr);
                    }
                    image.setPixelRGBA(x, y, abgr);
                }
            }
        } catch (Exception e) {
            image.close();
            return null;
        }
        return image;
    }
}