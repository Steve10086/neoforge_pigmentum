package com.astune.painter.mixin;

import com.astune.painter.api.CompositePainting;
import com.astune.painter.item.CanvasSheet;
import com.astune.painter.registry.ModDataComponents;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ItemFrameRenderer.class)
public abstract class ItemFrameRendererMixin<T extends ItemFrame> {

    @Inject(method = "render",
            at = @At("HEAD"), cancellable = true)
    private void onRender(T entity, float entityYaw, float partialTicks, PoseStack poseStack,
                          MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        ItemStack item = entity.getItem();
        if (!(item.getItem() instanceof CanvasSheet)) return;

        CanvasSheet.ensureTexture(item); // 确保纹理存在

        String texStr = item.get(ModDataComponents.CANVAS_TEXTURE.get());
        if (texStr == null) return;
        ResourceLocation texture = ResourceLocation.tryParse(texStr);
        if (texture == null || Minecraft.getInstance().getTextureManager().getTexture(texture) == null) return;

        ci.cancel(); // 仅取消物品渲染，框架已由 super.render() 绘制

        ItemFrameRenderer self = (ItemFrameRenderer) (Object) this;

        // 绘制画布
        poseStack.pushPose();
        Direction direction = entity.getDirection();
        Vec3 vec3 = self.getRenderOffset(entity, partialTicks);
        poseStack.translate(-vec3.x(), -vec3.y(), -vec3.z());
        poseStack.translate((double)direction.getStepX() * 0.46875, (double)direction.getStepY() * 0.46875, (double)direction.getStepZ() * 0.46875);
        Direction dir = entity.getDirection();
        Vec3 normal = Vec3.atLowerCornerOf(dir.getNormal());
        Axis axis = Axis.of(normal.toVector3f());
        poseStack.mulPose(axis.rotationDegrees((float)entity.getRotation() % 4 * 2 * 360.0F / 8.0F));
        poseStack.mulPose(axis.rotationDegrees(180.0F));

        renderCanvasInFrame(poseStack, bufferSource, entity, texture);
        poseStack.popPose();
    }

    private void renderBackgroundCube(PoseStack.Pose last, MultiBufferSource bufferSource,
                                      Vec3[] vertices, Vec3 normal, Direction dir) {
        // ===== 背景立方体 =====
        float bgAlpha = 1.0f;
        float thickness = 0.0625f;
        Vec3 backOffset = normal.scale(-thickness);

        Vec3[] backVertices = new Vec3[4];
        for (int i = 0; i < 4; i++) {
            backVertices[i] = vertices[i].add(backOffset);
        }

        ResourceLocation canvasTex = ResourceLocation.fromNamespaceAndPath("painter", "textures/misc/canvas.png");
        ResourceLocation canvasSideTex = ResourceLocation.fromNamespaceAndPath("painter", "textures/misc/canvas_side.png");
        VertexConsumer bgVc = bufferSource.getBuffer(RenderType.entityTranslucent(canvasTex));
        int light = LightTexture.FULL_BRIGHT;
        int bgColor = ((int)(bgAlpha * 255) << 24) | 0xFFFFFF;
        float nx = (float) normal.x, ny = (float) normal.y, nz = (float) normal.z;

// 前后表面
        addQuad(bgVc, last, vertices[0], vertices[1], vertices[2], vertices[3], nx, ny, nz, bgColor, light);
        addQuad(bgVc, last, backVertices[1], backVertices[0], backVertices[3], backVertices[2], -nx, -ny, -nz, bgColor, light);


        bgVc = bufferSource.getBuffer(RenderType.entityTranslucent(canvasSideTex));

// 侧面（底、右、顶、左）
        Vec3[] sideN = sideNormals(dir);
        addQuad(bgVc, last, vertices[0], vertices[1], backVertices[1], backVertices[0],
                (float) sideN[0].x, (float) sideN[0].y, (float) sideN[0].z, bgColor, light);
        addQuad(bgVc, last, vertices[1], vertices[2], backVertices[2], backVertices[1],
                (float) sideN[1].x, (float) sideN[1].y, (float) sideN[1].z, bgColor, light);
        addQuad(bgVc, last, vertices[2], vertices[3], backVertices[3], backVertices[2],
                (float) sideN[2].x, (float) sideN[2].y, (float) sideN[2].z, bgColor, light);
        addQuad(bgVc, last, vertices[3], vertices[0], backVertices[0], backVertices[3],
                (float) sideN[3].x, (float) sideN[3].y, (float) sideN[3].z, bgColor, light);
    }

    private void addQuad(VertexConsumer vc, PoseStack.Pose pose,
                         Vec3 v0, Vec3 v1, Vec3 v2, Vec3 v3,
                         float nx, float ny, float nz,
                         int color, int light) {
        vc.addVertex(pose, (float) v0.x, (float) v0.y, (float) v0.z).setColor(color).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, (float) v1.x, (float) v1.y, (float) v1.z).setColor(color).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, (float) v2.x, (float) v2.y, (float) v2.z).setColor(color).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, (float) v3.x, (float) v3.y, (float) v3.z).setColor(color).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
    }

    // 计算四个侧面的法线（底、右、顶、左）
    private Vec3[] sideNormals(Direction faceDir) {
        Vec3 faceNormal = Vec3.atLowerCornerOf(faceDir.getNormal());
        // 确定“上方”在世界中的向量，排除平行于法线的方向
        Vec3 up;
        if (faceDir.getAxis() == Direction.Axis.Y) {
            up = faceDir == Direction.UP ? new Vec3(0, 0, 1) : new Vec3(0, 0, -1);
        } else {
            up = new Vec3(0, 1, 0);
        }
        Vec3 right = up.cross(faceNormal).normalize();
        // 重新计算真正的上方向（right × faceNormal）
        up = faceNormal.cross(right).normalize(); // 可能和原来的 up 同向或反向

        // 侧面法线顺序：底(-up)、右(right)、顶(up)、左(-right)
        return new Vec3[]{
                up.scale(-1),    // 底
                right,           // 右
                up,              // 顶
                right.scale(-1)  // 左
        };
    }

    private void renderCanvasInFrame(PoseStack poseStack, MultiBufferSource bufferSource,
                                     ItemFrame frame, ResourceLocation texture) {


        Direction dir = frame.getDirection();
        Vec3 normal = Vec3.atLowerCornerOf(dir.getNormal());

        // 构建四边形顶点（与展示框大小匹配）
        // 展示框内径约为 16×16 像素，这里以世界坐标 0.5 为半边长
        float half = 0.5f;
        Vec3[] vertices = switch (dir) {
            case UP -> new Vec3[]{
                    new Vec3(-half, -0.5, -half), // 左下
                    new Vec3(half, -0.5, -half),  // 右下
                    new Vec3(half, -0.5, half),   // 右上
                    new Vec3(-half, -0.5, half)   // 左上
            };
            case DOWN -> new Vec3[]{
                    new Vec3(-half, 0.5, -half), // 左下
                    new Vec3(half, 0.5, -half),  // 右下
                    new Vec3(half, 0.5, half),   // 右上
                    new Vec3(-half, 0.5, half)   // 左上
            };
            case SOUTH -> new Vec3[]{
                    new Vec3(-half, -half, -0.5),
                    new Vec3(half, -half, -0.5),
                    new Vec3(half, half, -0.5),
                    new Vec3(-half, half, -0.5)
            };
            case NORTH -> new Vec3[]{
                    new Vec3(-half, -half, 0.5),
                    new Vec3(half, -half, 0.5),
                    new Vec3(half, half, 0.5),
                    new Vec3(-half, half, 0.5)
            };
            case EAST -> new Vec3[]{
                    new Vec3(-0.5, -half, -half),
                    new Vec3(-0.5, -half, half),
                    new Vec3(-0.5, half, half),
                    new Vec3(-0.5, half, -half)
            };
            case WEST -> new Vec3[]{
                    new Vec3(0.5, -half, -half),
                    new Vec3(0.5, -half, half),
                    new Vec3(0.5, half, half),
                    new Vec3(0.5, half, -half)
            };
        };

        // 调整顶点方向（让正面朝向观察者）
        Vec3 offset = normal.scale(0.0625f); // 微突防止 Z-fighting
        for (int i = 0; i < 4; i++) {
            vertices[i] = vertices[i].add(offset);
        }

        var last = poseStack.last();
        float nx = (float) normal.x, ny = (float) normal.y, nz = (float) normal.z;
        // 满亮度渲染，避免环境光影响颜色
        int light = LightTexture.FULL_BRIGHT;

        renderBackgroundCube(last, bufferSource, vertices, normal, dir);

        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucent(texture));

        // 添加四边形
        vc.addVertex(last, (float) vertices[0].x, (float) vertices[0].y, (float) vertices[0].z)
                .setColor(-1).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(last, nx, ny, nz);
        vc.addVertex(last, (float) vertices[1].x, (float) vertices[1].y, (float) vertices[1].z)
                .setColor(-1).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(last, nx, ny, nz);
        vc.addVertex(last, (float) vertices[2].x, (float) vertices[2].y, (float) vertices[2].z)
                .setColor(-1).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(last, nx, ny, nz);
        vc.addVertex(last, (float) vertices[3].x, (float) vertices[3].y, (float) vertices[3].z)
                .setColor(-1).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(last, nx, ny, nz);

    }
}
