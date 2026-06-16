package com.astune.painter.client;

import com.astune.painter.api.CanvasData;
import com.astune.painter.api.CanvasDataHolder;
import com.astune.painter.api.CanvasFace;
import com.astune.painter.api.ResourcesBundle;
import com.astune.painter.api.render.CanvasPixelRenderer;
import com.astune.painter.api.render.CanvasRendererRegistry;
import com.astune.painter.api.render.RenderContext;
import com.astune.painter.block.CanvasBlockEntity;
import com.astune.painter.block.OcclusionCanvasBlock;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class CanvasBlockEntityRenderer implements BlockEntityRenderer<BlockEntity> {
    private static float offset() { return com.astune.painter.Config.CANVAS_RENDER_LAYER_OFFSET.get().floatValue(); }

    public CanvasBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(BlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderState(be, poseStack);
        List<Pair<CanvasFace, ResourcesBundle>> textures = null;
        if (be instanceof CanvasDataHolder holder) {
            textures = holder.painter$getCachedFaceTextures();
            if (textures == null) {
                CanvasData canvasData = holder.painter$getCanvasData();
                if (canvasData != null && !canvasData.isEmpty()){
                    holder.painter$regenerateTextures(canvasData);
                    textures = holder.painter$getCachedFaceTextures();
                    if (textures == null) return;
                } else {
                    return;
                }
            }
        }else{
            return;
        }

        Level level = be.getLevel();
        if (level == null) return;
        BlockPos pos = be.getBlockPos();
        boolean occlusion = (be.getBlockState().getBlock() instanceof OcclusionCanvasBlock || be.getBlockState().isSolidRender(level, pos));

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        renderCanvasTexture(level, pos, poseStack, bufferSource, textures, packedLight, packedOverlay, occlusion);

        poseStack.popPose();
    }

    private void renderState(BlockEntity be, PoseStack poseStack){
        if (!(be instanceof CanvasBlockEntity cbe)) return;

        Set<BlockPos> seen = CanvasRenderEventHandler.SEEN_POSITIONS;

        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        RandomSource random = RandomSource.create();
        var bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

        var pos = be.getBlockPos();

        if (!seen.isEmpty() && seen.contains(pos)) {
            return;
        }

        BlockState mimicked = cbe.getMimickedState();
        if (mimicked == null) return;

        BakedModel model = dispatcher.getBlockModel(mimicked);
        ModelData mData;
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            mData = model.getModelData(level, pos, mimicked, ModelData.EMPTY);
            for (RenderType rt : model.getRenderTypes(mimicked, random, mData)) {
                poseStack.pushPose();
                VertexConsumer vc = bufferSource.getBuffer(rt);
                dispatcher.renderBatched(mimicked, pos, level, poseStack, vc, true, random);
                poseStack.popPose();
            }
        }

    }

    public static void renderCanvasTexture(Level level, BlockPos pos, PoseStack poseStack,
                                           MultiBufferSource bufferSource,
                                           List<Pair<CanvasFace, ResourcesBundle>> textures,
                                           int packedLight, int packedOverlay, boolean isOcclusion) {
        for (var pair : textures) {
            CanvasFace face = pair.getFirst();
            ResourcesBundle tex = pair.getSecond();
            if (tex == null) continue;

            // 处理光照
            int faceLight = packedLight;
            if (isOcclusion) {
                faceLight = getNeighborLight(level, pos, face.primaryFace());
            }
            double offset = offset();
            for (var t : tex.resourceLocations()){
                RenderContext context = new RenderContext(face, t, poseStack, bufferSource,
                        faceLight, packedOverlay, level, pos, isOcclusion, offset);
                offset += offset();
                CanvasPixelRenderer renderer = CanvasRendererRegistry.resolve(context);
                if (renderer != null) {
                    renderer.renderFace(context);
                }
            }
        }
    }

    // 保留原有的 add 和 getNeighborLight 可以移除，因为已移至 Default 渲染器
    // 但 getNeighborLight 仍需要，因为光照计算还在 BER 中
    private static int getNeighborLight(Level level, BlockPos pos, Direction dir) {
        BlockPos neighborPos = pos.relative(dir);
        int blockLight = level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, neighborPos);
        int skyLight = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, neighborPos);
        return skyLight << 20 | blockLight << 4;
    }
}
