package com.astune.painter.client;

import com.astune.painter.api.CanvasFace;
import com.astune.painter.api.imageProvider.CanvasImageProvider;
import com.astune.painter.api.ResourcesBundle;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CanvasTextureManager {
    // 键: "entityId_directionName", 值: 当前有效的 ResourceLocation
    private static final Map<String, ResourceLocation> ACTIVE = new ConcurrentHashMap<>();
    private static long COUNTER = 0;
    public static int NEXT_TEXTURE_ID = 0;

    /** 可替换的纹理生成器，默认为内置实现 */
    private static final List<CanvasImageProvider> imageProviders = new ArrayList<>(List.of(new DefaultCanvasImageProvider()));

    /** 设置自定义的纹理图像生成器 */
    public static void setImageProvider(CanvasImageProvider provider) {
        if (provider == null) return;
        imageProviders.add(provider);
    }

    public static boolean removeImageProvider(CanvasImageProvider provider) {
        return imageProviders.remove(provider);
    }

    public static NativeImage createImage(CanvasFace face, CanvasImageProvider provider) {
        return provider.createImage(face);
    }

    /**
     * 生成或更新指定实体某个方向的画布纹理。
     * @param entityId 客户端侧 CanvasBlockEntity 的唯一 ID
     * @return 新纹理的 ResourceLocation，失败返回 null
     */
    public static ResourcesBundle createOrUpdateTexture(CanvasFace face, int entityId, int faceIndex) {
        String key = key(entityId, faceIndex);

        List<ResourceLocation> resources = new ArrayList<>();

        for (var imageProvider : imageProviders){
            NativeImage image = imageProvider.createImage(face);
            if (image == null) return null;

            long id = COUNTER++;

            ResourceLocation newLoc = ResourceLocation.fromNamespaceAndPath("painter",
                    "canvas/" + key + "_" + imageProvider.name() + "_" + id);

            DynamicTexture dynTex = new DynamicTexture(image);
            Minecraft.getInstance().getTextureManager().register(newLoc, dynTex);
            resources.add(newLoc);

            ResourceLocation oldLoc = ACTIVE.put(key, newLoc);
            if (oldLoc != null) {
                RenderSystem.recordRenderCall(() -> {
                    Minecraft.getInstance().getTextureManager().release(oldLoc);
                });
            }
        }

        return new ResourcesBundle(resources.toArray(new ResourceLocation[0]));
    }

    /** 释放指定实体的某个面 */
    public static void releaseTexture(int entityId, int faceIndex) {
        String key = key(entityId, faceIndex);
        ACTIVE.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(key + "_")) {
                RenderSystem.recordRenderCall(() -> {
                    Minecraft.getInstance().getTextureManager().release(entry.getValue());
                });
                return true;
            }
            return false;
        });
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

}