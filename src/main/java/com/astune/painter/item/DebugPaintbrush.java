package com.astune.painter.item;

import com.astune.painter.api.*;
import com.astune.painter.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DebugPaintbrush extends Item implements IPaintProvider {
    public DebugPaintbrush(Properties properties) {
        super(properties.stacksTo(1)
                .component(ModDataComponents.BRUSH_SIZE.get(), 1.0)
                .component(ModDataComponents.FEATHER_STRENGTH.get(), 1.0f)
                .component(ModDataComponents.STEP_SIZE.get(), 0.01)
                .component(ModDataComponents.BLEND_MODE.get(), BlendMode.ADD.name())
                .component(ModDataComponents.OPACITY.get(), 0.1f));
        PaintProviders.register(this, this);
    }

    public DebugPaintbrush() {
        super(new Item.Properties());
    }

    @Override
    public Integer getColor(ItemStack stack, Player player, Level level, BlockPos pos, CanvasFace face, int pixelX, int pixelY) {
        return getCurrentColor(stack);
    }
    @Override
    public boolean shouldPaint(Player player, BlockHitResult result){
        return DebugPaintbrushClientBridge.shouldPaint(player, result);
    }

    @Nullable
    @Override
    public PaintPattern getPattern(ItemStack stack, Player player, Level level, BlockPos pos, Vec3 hitLoc) {
        double diameter = stack.getOrDefault(ModDataComponents.BRUSH_SIZE.get(), 0.06);
        float feather = stack.getOrDefault(ModDataComponents.FEATHER_STRENGTH.get(), 0.0f);
        float opacity = stack.getOrDefault(ModDataComponents.OPACITY.get(), 1.0f);
        BlendMode mode = BlendMode.valueOf(stack.getOrDefault(ModDataComponents.BLEND_MODE.get(), BlendMode.OVERWRITE.name()));
        int color = getCurrentColor(stack);

        if (diameter <= 0) return null;

        final double radius = diameter / 2.0;
        return new PaintPattern(diameter, diameter, new PixelProvider() {
            @Override
            public BlendMode getBlendMode() {
                return mode;
            }

            @Override
            public @Nullable Integer getPixel(double dx, double dy) {
                // dx, dy 相对于图案左下角，计算相对于圆心的偏移
                double cx = dx - radius;
                double cy = dy - radius;
                double dist = Math.sqrt(cx * cx + cy * cy);
                if (dist > radius) return null;  // 超出圆形区域

                // 羽化：根据距离圆心的比例调整透明度
                float alphaFactor = 0.1f;
                if (feather > 0 && radius > 0) {
                    double ratio = dist / radius;    // 0 (圆心) → 1 (边缘)
                    alphaFactor = opacity * (float) Math.pow(1.0 - ratio, feather);
                }

                int a = (int) (255 * alphaFactor);
                if (a <= 0) return null;
                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;
                return (a << 24) | (r << 16) | (g << 8) | b;
            }
        });
    }

    @Override
    public Double getStep() {
        return DebugPaintbrushClientBridge.getStep();
    }



    private int getCurrentColor(ItemStack stack) {
        // 使用 getOrDefault，如果组件不存在则返回默认值白色(0xFFFFFFFF)
        return stack.getOrDefault(ModDataComponents.CURRENT_COLOR.get(), 0xFFFFFFFF);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // 显示当前颜色
        int color = getCurrentColor(stack);
        String hex = String.format("#%06X", color & 0x00FFFFFF);
        tooltip.add(Component.translatable("painter.tooltip.color", hex)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color & 0x00FFFFFF))));

        // 显示画笔大小（世界坐标单位）
        double brushSize = stack.getOrDefault(ModDataComponents.BRUSH_SIZE.get(), 0.0625);
        tooltip.add(Component.translatable("painter.tooltip.brush_size", String.format("%.2f", brushSize)));

        // 羽化强度
        float feather = stack.getOrDefault(ModDataComponents.FEATHER_STRENGTH.get(), 0.0f);
        tooltip.add(Component.translatable("painter.tooltip.feather", String.format("%.1f", feather * 100) + "%"));

        // 混合模式
        String blendMode = stack.getOrDefault(ModDataComponents.BLEND_MODE.get(), "overwrite");
        tooltip.add(Component.translatable("painter.tooltip.blend_mode",
                Component.translatable("painter.blend_mode." + blendMode)));

        // 步长
        double stepSize = stack.getOrDefault(ModDataComponents.STEP_SIZE.get(), 0.01);
        tooltip.add(Component.translatable("painter.tooltip.step_size", String.format("%.3f", stepSize)));

        // 透明度
        float opacity = stack.getOrDefault(ModDataComponents.OPACITY.get(), 1.0f);
        tooltip.add(Component.translatable("painter.tooltip.opacity", String.format("%.0f%%", opacity * 100)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand == InteractionHand.MAIN_HAND) {
            if (level.isClientSide && player.isShiftKeyDown()
                    && DebugPaintbrushClientBridge.tryOpenConfig(stack)) {
                return InteractionResultHolder.success(stack);
            }
            return InteractionResultHolder.pass(stack);
        }
        return InteractionResultHolder.success(stack);
    }
}
