package com.astune.painter.client;

import com.astune.painter.api.CanvasFace;
import com.astune.painter.registry.ModDataComponents;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class CanvasItemFrameRenderer {
    private static final Map<ItemFrame, ResourceLocation> TEXTURE_CACHE = new HashMap<>();

    public static ResourceLocation getOrCreateTexture(ItemFrame frame, CanvasFace face) {
        // 检查物品是否变化
        ItemStack current = frame.getItem();
        CanvasFace cachedFace = current.get(ModDataComponents.STORED_FACE.get());
        if (cachedFace == null) {
            releaseTexture(frame);
            return null;
        }
        ResourceLocation existing = TEXTURE_CACHE.get(frame);
        if (existing != null && Minecraft.getInstance().getTextureManager().getTexture(existing) != null) {
            // 简单比较内存地址，如需精确比较可使用序列化哈希
            return existing;
        }
        // 生成新纹理
        NativeImage image = CanvasTextureManager.createImage(face);
        if (image == null) return null;
        DynamicTexture dynTex = new DynamicTexture(image);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("painter", "item_frame/" + frame.getId() + "_" + System.nanoTime());
        Minecraft.getInstance().getTextureManager().register(id, dynTex);
        TEXTURE_CACHE.put(frame, id);
        return id;
    }

    public static void releaseTexture(ItemFrame frame) {
        ResourceLocation id = TEXTURE_CACHE.remove(frame);
        System.out.println("remove " + id);
        if (id != null) {
            Minecraft.getInstance().getTextureManager().release(id);
        }
    }
}
