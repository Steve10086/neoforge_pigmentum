package com.astune.painter.client;

import com.astune.painter.block.CanvasBlock;
import com.astune.painter.block.CanvasBlockEntity;
import com.astune.painter.network.ClientCanvasCache;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.joml.Vector3f;

public class CanvasRenderEventHandler {

    /** BlockPos processed by AddSectionGeometryEvent. Cleared per-section by LevelRendererMixin on setSectionDirty. */
    public static final Set<BlockPos> SEEN_POSITIONS = ConcurrentHashMap.newKeySet();

    private static final RenderType[] ALL_LAYERS = {
            RenderType.solid(),
            //RenderType.cutoutMipped(),
            //RenderType.cutout(),
            RenderType.translucent()
    };

    @SubscribeEvent
    public static void onAddGeometry(AddSectionGeometryEvent event) {
        BlockPos origin = event.getSectionOrigin();
        Minecraft mc = Minecraft.getInstance();
        Level level = event.getLevel();

        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();


        event.addRenderer(ctx -> {
            PoseStack poseStack = ctx.getPoseStack();
            Set<BlockPos> temp_position = new HashSet<>();
            int count = 0;
            for (int x = 0; x < 16; x++)
                for (int y = 0; y < 16; y++)
                    for (int z = 0; z < 16; z++) {
                        BlockPos pos = origin.offset(x, y, z);
                        BlockState state = level.getBlockState(pos);

                        if (!(state.getBlock() instanceof AirBlock)) count ++;

                        if (!(state.getBlock() instanceof CanvasBlock)) continue;

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

                        Vec3 offset = Vec3.atLowerCornerOf(pos)
                                .subtract(Vec3.atLowerCornerOf(origin));

                        BakedModel blockModel = dispatcher.getBlockModel(renderState);

                        temp_position.add(pos.immutable());

                        for (RenderType rendertype : blockModel.getRenderTypes(renderState, RandomSource.create(), null)){
                            poseStack.pushPose();
                            poseStack.translate(offset.x, offset.y, offset.z);
                            dispatcher.renderBatched(
                                    renderState, pos, ctx.getRegion(), poseStack,
                                    ctx.getOrCreateChunkBuffer(rendertype),
                                    true, RandomSource.create()
                            );
                            poseStack.popPose();
                        }
                    }
            if (origin.getX() < 1000000 || count > 1){
                SEEN_POSITIONS.addAll(temp_position);
            }
        });
    }
}