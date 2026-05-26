package com.astune.painter.item;

import com.astune.painter.api.*;
import com.astune.painter.client.inventory.BrushConfigScreen;
import com.astune.painter.registry.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.nio.IntBuffer;
import java.util.List;

import static com.astune.painter.client.ClientSetup.KEY_PICK_COLOR;

public class DebugPaintbrush extends Item implements IPaintProvider {
    private static final String COLOR_TAG = "currentColor";
    private static boolean sPickingColor = false;
    public DebugPaintbrush(Properties properties) {
        super(properties
                .component(ModDataComponents.BRUSH_SIZE.get(), 1.0)
                .component(ModDataComponents.FEATHER_STRENGTH.get(), 1.0f)
                .component(ModDataComponents.STEP_SIZE.get(), 0.01)
                .component(ModDataComponents.BLEND_MODE.get(), BlendMode.ADD.name())
                .component(ModDataComponents.OPACITY.get(), 0.1f));
        PaintProviders.register(this, this);
    }

    public DebugPaintbrush() {
        this(new Item.Properties());
    }

    @Override
    public Integer getColor(ItemStack stack, Player player, Level level, BlockPos pos, CanvasFace face, int pixelX, int pixelY) {
        return getCurrentColor(stack);
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            ItemStack stack = mc.player.getMainHandItem();
            if (stack.getItem() instanceof DebugPaintbrush) {
                return stack.getOrDefault(ModDataComponents.STEP_SIZE.get(), 1/8.0);
            }
        }
        return 1/8.0;
    }



    private int getCurrentColor(ItemStack stack) {
        // 使用 getOrDefault，如果组件不存在则返回默认值白色(0xFFFFFFFF)
        return stack.getOrDefault(ModDataComponents.CURRENT_COLOR.get(), 0xFFFFFFFF);
    }

    // 写颜色
    private static void setCurrentColor(ItemStack stack, int color) {
        // 使用 set 方法更新组件值
        stack.set(ModDataComponents.CURRENT_COLOR.get(), color);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int color = getCurrentColor(stack);
        tooltip.add(Component.literal("Color: #" + Integer.toHexString(color).toUpperCase()));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand == InteractionHand.MAIN_HAND) {
            if (level.isClientSide) {
                HitResult hit = Minecraft.getInstance().hitResult;
                if (hit == null || hit.getType() == HitResult.Type.MISS) {
                    Minecraft.getInstance().setScreen(new BrushConfigScreen(stack));
                    return InteractionResultHolder.success(stack);
                }
            }
            return InteractionResultHolder.pass(stack);
        }
        return InteractionResultHolder.success(stack);
    }


    private static void pickColorClient(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        //if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return;

        // 从屏幕中心读取像素颜色
        int centerX = mc.getWindow().getWidth() / 2;
        int centerY = mc.getWindow().getHeight() / 2;

        // 从帧缓冲区读取颜色
        int offset = 3;
        int color = readPixelFromScreen(centerX + offset, centerY + offset);
        //System.out.println("new color: " + color);
        if (color != 0) {
            setCurrentColor(stack, color);
            String hex = String.format("#%06X", color & 0x00FFFFFF);
            Component name = Component.literal("Debug Paintbrush (" + hex + ")")
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color & 0x00FFFFFF)));
            stack.set(DataComponents.CUSTOM_NAME, name);

            if (mc.player != null) {
                Component message = Component.literal("Picked color: " + hex)
                        .withStyle(Style.EMPTY.withColor(net.minecraft.network.chat.TextColor.fromRgb(color & 0x00FFFFFF)));
                mc.gui.setOverlayMessage(message, false);
            }
        }
    }

    /**
     * 从屏幕指定坐标读取像素颜色（ARGB格式）。
     */
    private static int readPixelFromScreen(int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        var window = mc.getWindow();

        // 1. 坐标系转换
        int glY = window.getHeight() - y;

        // 2. 准备一个缓冲区来接收像素数据
        IntBuffer buffer = BufferUtils.createIntBuffer(1);

        // 3. 从帧缓冲区读取 1x1 像素
        // 关键参数：位置(x, glY)，尺寸(1,1)，格式(RGBA)，数据类型，存储缓冲区
        GL11.glReadPixels(x, glY, 1, 1, GL12.GL_RGBA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, buffer);

        // 4. 格式转换
        int rgba = buffer.get(0);
        int a = (rgba >> 24) & 0xFF;
        int r = (rgba) & 0xFF;
        int g = (rgba >> 8) & 0xFF;
        int b = (rgba >> 16) & 0xFF;

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @EventBusSubscriber(modid = "painter", value = Dist.CLIENT)
    public static class CrosshairHandler {
        private static final ResourceLocation CROSSHAIR_ID = ResourceLocation.withDefaultNamespace("crosshair");

        @SubscribeEvent
        public static void onRenderCrosshair(RenderGuiLayerEvent.Pre event) {
            // 检查正在渲染的是否为 CROSSHAIR 覆盖层
            if (event.getName().equals(CROSSHAIR_ID) && sPickingColor) {
                event.setCanceled(true);      // 取消准星渲染
            }
        }
    }

    @EventBusSubscriber(modid = "painter", value = Dist.CLIENT)
    public static class PreventBlockUseHandler {
        @SubscribeEvent
        public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
            ItemStack stack = event.getItemStack();
            if (stack.getItem() instanceof DebugPaintbrush) {
                // 取消对原方块的交互，只让画笔处理
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
        }
    }

    @EventBusSubscriber(modid = "painter", value = Dist.CLIENT)
    private static class PickColorKeyHandler {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Pre event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;

            if (!sPickingColor && KEY_PICK_COLOR.isDown()) {
                ItemStack brush = mc.player.getMainHandItem();
                if (brush.getItem() instanceof DebugPaintbrush) {
                    sPickingColor = true;     // 设置标志，准备拦截下一帧的准星渲染
                }
            }

            if (sPickingColor) {
                ItemStack brush = mc.player.getMainHandItem();
                if (brush.getItem() instanceof DebugPaintbrush) {
                    pickColorClient(brush); // 执行吸色
                }
            }


            if (sPickingColor && !KEY_PICK_COLOR.isDown()){
                ItemStack brush = mc.player.getMainHandItem();
                if (brush.getItem() instanceof DebugPaintbrush) {

                    sPickingColor = false;     // 设置标志，准备拦截下一帧的准星渲染
                }
            }
        }
    }
}