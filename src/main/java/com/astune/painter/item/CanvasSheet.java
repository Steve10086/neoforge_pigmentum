package com.astune.painter.item;

import com.astune.painter.api.CanvasData;
import com.astune.painter.api.CanvasDataHolder;
import com.astune.painter.api.CanvasFace;
import com.astune.painter.registry.ModDataComponents;
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
        CanvasSheetClientBridge.ensureTexture(stack);
    }

    private static void releaseTextureFromStack(ItemStack stack) {
        String texStr = stack.get(ModDataComponents.CANVAS_TEXTURE.get());
        if (texStr != null) {
            ResourceLocation loc = ResourceLocation.tryParse(texStr);
            if (loc != null) {
                CanvasSheetClientBridge.releaseTexture(loc);
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
