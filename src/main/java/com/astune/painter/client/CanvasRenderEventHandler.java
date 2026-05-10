package com.astune.painter.client;

import com.astune.painter.api.CanvasData;
import com.astune.painter.api.CanvasDataHolder;
import com.astune.painter.attachment.ModAttachments;
import com.astune.painter.block.CanvasBlock;
import com.astune.painter.block.CanvasBlockEntity;
import com.astune.painter.network.ClientCanvasCache;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;

import java.util.Arrays;
import java.util.Optional;

import static net.minecraft.world.level.block.RedstoneLampBlock.LIT;

/**
 * 负责接管所有带有画布数据的方块的渲染。
 * 对于无实体的方块（已被 CanvasBlock 替换），渲染其原始模型（从 CanvasBlockEntity.getMimickedState() 获取）并叠加画布像素。
 * 对于有实体的方块（箱子、熔炉等），渲染其自身模型并叠加画布像素。
 * 若没有画布数据，则退回原版渲染逻辑（原版会自行处理）。
 */
public class CanvasRenderEventHandler {

    @SubscribeEvent
    public static void onAddGeometry(AddSectionGeometryEvent event) {
        BlockPos sectionOrigin = event.getSectionOrigin();
        Minecraft mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null) return;

        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();

        event.addRenderer(ctx -> {
            PoseStack poseStack = ctx.getPoseStack();

            // 遍历整个 section
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockPos pos = sectionOrigin.offset(x, y, z);
                        BlockState state = level.getBlockState(pos);
                        if (state.isAir()) continue;

                        // 尝试获取画布数据（优先客户端缓存，其次从实体附件读取）
                        CanvasData canvas = getCanvasData(level, pos);
                        if (canvas == null) {
                            continue; // 无画布数据，交给原版渲染
                        }

                        // 确定要渲染的原方块状态
                        BlockEntity be = level.getBlockEntity(pos);
                        BlockState renderState = null;
                        if (state.getBlock() instanceof CanvasBlock) {
                            // 优先用BlockEntity
                            if (be instanceof CanvasBlockEntity canvasBE) {
                                renderState = canvasBE.getMimickedState();
                            }
                            if (renderState == null) {
                                renderState = ClientCanvasCache.getMimickedState(pos);
                            } // 降级
                        } else {
                            renderState = state; // 有 BlockEntity 的原始方块
                        }
                        if (renderState == null) {
                            continue; // 没有可渲染的模型
                        }


                        // 1. 渲染原方块模型（不透明和 cutout 层可合并，这里简化只渲染 cutoutMipped）
                        System.out.println("rendering mimicked " + renderState.getBlock().getName());
                        System.out.println("rendering canvas " + Arrays.toString(canvas.faces().toArray()));

                        // 渲染原方块模型
                        {
                            poseStack.pushPose();
                            Vec3 offset = Vec3.atLowerCornerOf(pos).subtract(Vec3.atLowerCornerOf(sectionOrigin));
                            poseStack.translate(offset.x, offset.y, offset.z);
                            dispatcher.renderBatched(
                                    renderState,
                                    pos,
                                    level,
                                    poseStack,
                                    ctx.getOrCreateChunkBuffer(RenderType.cutoutMipped()),
                                    false,
                                    RandomSource.create()
                            );
                            poseStack.popPose();
                        }

                        // 渲染画布像素（透明层）
                        poseStack.pushPose();
                        Vec3 offset = Vec3.atLowerCornerOf(pos).subtract(Vec3.atLowerCornerOf(sectionOrigin));
                        poseStack.translate(offset.x, offset.y, offset.z);
                        renderPixelQuads(poseStack, ctx.getOrCreateChunkBuffer(RenderType.translucent()), canvas, pos);
                        poseStack.popPose();
                    }
                }
            }
        });
    }

    /**
     * 从客户端缓存或实体附件中获取画布数据。
     */
    private static CanvasData getCanvasData(net.minecraft.world.level.Level level, BlockPos pos) {
        CanvasData canvas = ClientCanvasCache.getCanvas(pos);
        if (canvas != null) return canvas;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CanvasDataHolder holder) {
            canvas = holder.painter$getCanvasData();
            if (canvas != null){
                ClientCanvasCache.putCanvas(pos, canvas);
                return canvas;
            }

        }
        return null;
    }

    /**
     * 获取渲染时使用的原方块模型。
     * 对于 CanvasBlock，从其 BlockEntity 中取出 mimickedState；
     * 对于其他方块，直接使用该方块自身的 BlockState。
     */
    private static BlockState getRenderState(net.minecraft.world.level.Level level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof CanvasBlock) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CanvasBlockEntity canvasBE) {
                return canvasBE.getMimickedState();
            }
            return null;
        } else {
            return state;
        }
    }

    /**
     * 生成并渲染画布像素的四边形（半透明层）。
     * 此处为占位实现，需从原 BakedModel 的 createPixelQuads 移植。
     * 当前仅输出日志，请根据实际像素生成逻辑补充。
     */
    private static void renderPixelQuads(PoseStack poseStack, VertexConsumer consumer, CanvasData canvas, BlockPos pos) {
        // TODO: 实现像素四边形生成，参考原 CanvasBlockBakedModel.createPixelQuads()
        // 遍历 canvas.pixels() 中的非透明像素，在对应的面上生成带顶点颜色的 BakedQuad，
        // 并通过 consumer 提交渲染。
    }
}