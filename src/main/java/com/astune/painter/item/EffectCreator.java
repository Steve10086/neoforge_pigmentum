package com.astune.painter.item;

import com.astune.painter.api.*;
import com.astune.painter.api.blend.BlendContext;
import com.astune.painter.api.blend.BlendFunction;
import com.astune.painter.api.imageProvider.CanvasImageProvider;
import com.astune.painter.api.imageProvider.CanvasImageProviderRegistry;
import com.astune.painter.api.imageProvider.DefaultCanvasImageProvider;
import com.astune.painter.api.imageProvider.ImageProviderContext;
import com.astune.painter.api.render.CanvasPixelRenderer;
import com.astune.painter.api.render.CanvasRendererRegistry;
import com.astune.painter.api.render.RenderContext;
import com.astune.painter.registry.ModDataComponents;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class EffectCreator extends Item implements IPaintProvider {

    private static Vec3 lastHitLoc = null;

    public EffectCreator() {
        super(new Item.Properties());
        PaintProviders.register(this, this);
    }


    @Nullable
    @Override
    public Integer getColor(ItemStack stack, Player player, Level level, BlockPos pos, CanvasFace face, int pixelX, int pixelY) {
        return 0xFFFFFFFF;
    }

    @Override
    public boolean shouldPaint(Player player, BlockHitResult result){
        if (!(player.level().isClientSide
                && net.minecraft.client.Minecraft.getInstance().options.keyUse.isDown())) return false;
        if (lastHitLoc == null) {
            lastHitLoc = result.getLocation();
            return true;
        }
        if (lastHitLoc.distanceTo(result.getLocation()) > getStep()){
            lastHitLoc = result.getLocation();
            return true;
        }

        return false;
    }

    @Nullable
    @Override
    public PaintPattern getPattern(ItemStack stack, Player player, Level level, BlockPos pos, Vec3 hitLoc) {
        double diameter = stack.getOrDefault(ModDataComponents.BRUSH_SIZE.get(), 0.06);
        float feather = stack.getOrDefault(ModDataComponents.FEATHER_STRENGTH.get(), 0.0f);
        float opacity = stack.getOrDefault(ModDataComponents.OPACITY.get(), 1.0f);
        BlendMode mode = BlendMode.valueOf(stack.getOrDefault(ModDataComponents.BLEND_MODE.get(), BlendMode.OVERWRITE.name()));
        int color = 0xFFFFFFFF;

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

                int a = (int) (255);
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
            if (stack.getItem() instanceof EffectCreator) {
                return stack.getOrDefault(ModDataComponents.STEP_SIZE.get(), 1/8.0);
            }
        }
        return 1/8.0;
    }

    protected static BlendMode safeBlendMode(String name) {
        try {
            return BlendMode.valueOf(name);
        } catch (IllegalArgumentException e) {
            return BlendMode.OVERWRITE;
        }
    }

    @javax.annotation.Nullable
    public BlendFunction getCustomBlendFunction(ItemStack stack) {
        String modeStr = stack.getOrDefault(ModDataComponents.BLEND_MODE.get(), BlendMode.OVERWRITE.name());
        BlendMode mode = safeBlendMode(modeStr);

        return new BlendFunction() {
            @Override
            public boolean apply(BlendContext ctx) {
                // 1. 先执行默认混合
                boolean changed = mode.getDefaultFunction().apply(ctx);

                // 2. 写入 glow 效果层（固定值 255 = 全亮）
                ctx.setEffect("glow", 255);
                //System.out.println(ctx);

                return changed;
            }
        };
    }

    static {
        CanvasImageProviderRegistry.register(new GlowImageProvider(), 2);
        CanvasRendererRegistry.registerPixelRenderer(new GlowPixelRenderer(), 2);
    }

    public static class GlowPixelRenderer implements CanvasPixelRenderer {
        @Override
        public boolean canRender(RenderContext context) {
            return (context.texture != null && context.texture.getPath().contains("_glow_"));
        }

        @Override
        public boolean renderFace(RenderContext context) {
            var face = context.face;
            var texture = context.texture;
            if (texture == null) return false;
            //System.out.println("render glow " + context.texture.getPath());

            Vec3[] corners = face.cornerWithOffset(context.offset);
            VertexConsumer vc = context.bufferSource.getBuffer(RenderType.entityTranslucent(texture));
            var last = context.poseStack.last();
            Direction dir = face.primaryFace();
            Vec3 normal = Vec3.atLowerCornerOf(dir.getNormal());
            float nx = (float) normal.x, ny = (float) normal.y, nz = (float) normal.z;

            int light = 0x00F000F0;

            add(vc, last, corners[0], 0, 0, nx, ny, nz, light, context.packedOverlay);
            add(vc, last, corners[1], 1, 0, nx, ny, nz, light, context.packedOverlay);
            add(vc, last, corners[2], 1, 1, nx, ny, nz, light, context.packedOverlay);
            add(vc, last, corners[3], 0, 1, nx, ny, nz, light, context.packedOverlay);
            return true;
        }

        private static void add(VertexConsumer vc, PoseStack.Pose pose, Vec3 pos, float u, float v,
                                float nx, float ny, float nz, int light, int overlay) {
            vc.addVertex(pose, (float) pos.x, (float) pos.y, (float) pos.z)
                    .setColor(255,255,255,255)
                    .setUv(u,v)
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(pose, nx, ny, nz);
        }
    }
    public static class GlowImageProvider implements CanvasImageProvider {

        @Override
        public String name() {
            return "glow";
        }

        @Override
        public boolean canProvide(ImageProviderContext context) {
            // 仅当 face 上存在 glow 效果层时才生成纹理
            return context.face != null && context.face.getEffectLayer("glow") != null;
        }

        @Override
        public NativeImage createImage(CanvasFace face) {
            //System.out.println("glow image");
            byte[] glowLayer = face.getEffectLayer("glow");
            if (glowLayer == null) return null;

            PixelMatrix matrix = face.pixels();
            if (matrix == null || matrix.getWidth() <= 0 || matrix.getHeight() <= 0) return null;
            int w = matrix.getWidth(), h = matrix.getHeight();
            NativeImage image = null;
            try {
                image = new NativeImage(w, h, true);
                image.getPixelRGBA(0, 0); // 触发分配检查
            } catch (Exception e) {
                if (image != null) image.close();
                return null;
            }

            try {
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        if (glowLayer[y * w + x] == (byte)255){
                            int argb = matrix.getPixel(x, y);
                            int a = (argb >> 24) & 0xFF;
                            int r = (argb >> 16) & 0xFF;
                            int g = (argb >> 8) & 0xFF;
                            int b = argb & 0xFF;

                            int bgr = (b << 16) | (g << 8) | r;
                            int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                            if (bgr != 0 && a == 0) {
                                abgr = 255 << 24 | bgr;
                            }
                            image.setPixelRGBA(x, y, abgr);
                        }
                    }
                }
            } catch (Exception e) {
                image.close();
                return null;
            }
            return image;
        }
    }
}
