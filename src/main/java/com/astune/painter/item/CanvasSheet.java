package com.astune.painter.item;

import com.astune.painter.api.CanvasData;
import com.astune.painter.api.CanvasDataHolder;
import com.astune.painter.api.CanvasFace;
import com.astune.painter.api.PixelMatrix;
import com.astune.painter.client.CanvasTextureManager;
import com.astune.painter.registry.ModDataComponents;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;


public class CanvasSheet extends Item {
    public CanvasSheet(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;

        BlockPos pos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CanvasDataHolder holder)) return InteractionResult.PASS;

        CanvasData canvasData = holder.painter$getCanvasData();
        if (canvasData == null) return InteractionResult.PASS;

        Vec3 hitLoc = context.getClickLocation();
        Direction face = context.getClickedFace();
        BlockHitResult hitResult = new BlockHitResult(hitLoc, face, pos, false);

        CanvasFace hitFace = canvasData.getFaceAtHit(pos, hitResult);
        if (hitFace == null) return InteractionResult.PASS;

        CanvasFace copy = cloneCanvasFace(hitFace);
        if (copy == null) return InteractionResult.PASS;

        ItemStack stack = context.getItemInHand();

        // 释放旧纹理（如果存在）
        releaseTextureFromStack(stack);

        // 存储面数据
        stack.set(ModDataComponents.STORED_FACE.get(), copy);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1)); // 切换到填充外观

        // 客户端立即生成纹理并存入物品
        if (level.isClientSide) {
            ensureTexture(stack);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static CanvasFace cloneCanvasFace(CanvasFace face) {
        var encoded = CanvasFace.CODEC.encodeStart(NbtOps.INSTANCE, face).getOrThrow();
        return CanvasFace.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
    }

    /**
     * 确保物品拥有有效的纹理。若纹理缺失或失效则自动生成，并释放旧纹理。
     * 仅在客户端调用。
     */
    public static void ensureTexture(ItemStack stack) {
        if (!stack.has(ModDataComponents.STORED_FACE.get())) {
            releaseTextureFromStack(stack);
            return;
        }

        String texStr = stack.get(ModDataComponents.CANVAS_TEXTURE.get());
        ResourceLocation current = texStr != null ? ResourceLocation.tryParse(texStr) : null;
        if (current != null && Minecraft.getInstance().getTextureManager().getTexture(current) != null) {
            return;
        }

        // 释放无效旧纹理
        if (current != null) {
            Minecraft.getInstance().getTextureManager().release(current);
        }

        CanvasFace face = stack.get(ModDataComponents.STORED_FACE.get());
        PixelMatrix pixels = face.pixels();
        if (pixels == null) return;

        int srcW = pixels.getWidth();
        int srcH = pixels.getHeight();
        int maxDim = Math.max(Math.max(srcW, srcH), 16);

        // 创建正方形 NativeImage，背景透明
        NativeImage image = new NativeImage(maxDim, maxDim, false);
        // 计算偏移，使画布居中
        int offsetX = (maxDim - srcW) / 2;
        int offsetY = (maxDim - srcH) / 2;

        for (int y = 0; y < srcH; y++) {
            for (int x = 0; x < srcW; x++) {
                int argb = pixels.getPixel(x, y);
                // 写入偏移后的位置（原生 ARGB 转 ABGR）
                image.setPixelRGBA(offsetX + x, offsetY + y, argbToABGR(argb));
            }
        }

        DynamicTexture dynTex = new DynamicTexture(image);
        ResourceLocation newId = ResourceLocation.fromNamespaceAndPath("painter",
                "sheet/" + UUID.randomUUID());
        Minecraft.getInstance().getTextureManager().register(newId, dynTex);
        stack.set(ModDataComponents.CANVAS_TEXTURE.get(), newId.toString());
    }

    private static int argbToABGR(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    private static void releaseTextureFromStack(ItemStack stack) {
        String texStr = stack.get(ModDataComponents.CANVAS_TEXTURE.get());
        if (texStr != null) {
            ResourceLocation loc = ResourceLocation.tryParse(texStr);
            if (loc != null) {
                Minecraft.getInstance().getTextureManager().release(loc);
            }
            stack.remove(ModDataComponents.CANVAS_TEXTURE.get());
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        CanvasFace face = stack.getOrDefault(ModDataComponents.STORED_FACE.get(), null);
        if (face != null) {
            tooltip.add(Component.literal("Face: " + face.primaryFace().getName()));
            tooltip.add(Component.literal("Size: " + face.pixels().getWidth() + "×" + face.pixels().getHeight()));
        }
    }

}
