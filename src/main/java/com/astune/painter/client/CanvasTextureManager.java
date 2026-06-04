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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class CanvasTextureManager {
    // 键: "entityId_faceIndex_providerName", 值: 当前有效的 ResourceLocation
    private static final Map<String, ResourceLocation> ACTIVE = new ConcurrentHashMap<>();
    private static long COUNTER = 0;
    public static int NEXT_TEXTURE_ID = 0;

    public static NativeImage createImage(CanvasFace face, CanvasImageProvider provider) {
        return provider.createImage(face);
    }

    /**
     * 生成或更新指定实体某个方向的画布纹理。
     * 通过 {@link CanvasImageProviderRegistry} 获取所有匹配的图像提供者，
     * 每个提供者生成一张纹理，最终打包为 {@link ResourcesBundle}。
     *
     * @param entityId 客户端侧 CanvasBlockEntity 的唯一 ID
     * @return 包含所有提供者纹理的 ResourcesBundle，若无提供者生成成功则 bundle 为空
     */

    public static ResourcesBundle createOrUpdateTexture(CanvasFace face, int entityId, int faceIndex) {
        String key = key(entityId, faceIndex);

        List<ResourceLocation> resources = new ArrayList<>();

        ImageProviderContext ctx = new ImageProviderContext(face, null, null);

        releaseTexture(entityId, faceIndex);

        for (CanvasImageProvider provider : CanvasImageProviderRegistry.resolveAll(ctx)) {
            NativeImage image = provider.createImage(face);
            if (image == null) continue; // 单个提供者失败跳过，不影响其他

            long id = COUNTER++;

            ResourceLocation newLoc = ResourceLocation.fromNamespaceAndPath("painter",
                    "canvas/" + key + "_" + provider.name() + "_" + id);

            DynamicTexture dynTex = new DynamicTexture(image);
            Minecraft.getInstance().getTextureManager().register(newLoc, dynTex);
            resources.add(newLoc);

            // 按提供者名称区分 ACTIVE 键，确保每个提供者的纹理可独立追踪释放
            String activeKey = key + "_" + provider.name();
            ACTIVE.put(activeKey, newLoc);
            //System.out.println(newLoc.getPath());
            //System.out.println(activeKey);
        }

        return new ResourcesBundle(resources.toArray(new ResourceLocation[0]));
    }

    /** 释放指定实体的某个面（所有提供者的纹理） */
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
