package com.astune.painter.client;

import com.astune.painter.api.CanvasDataHolder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CanvasWorldRenderQueue {
    private static final Map<BlockPos, Entry> QUEUE = new LinkedHashMap<>();
    private static final ByteBufferBuilder BUFFER = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
    private static final MultiBufferSource.BufferSource BUFFER_SOURCE = MultiBufferSource.immediate(BUFFER);
    private static Level queuedLevel;

    private CanvasWorldRenderQueue() {}

    public static void enqueue(BlockEntity blockEntity, int packedLight, int packedOverlay, boolean occlusion) {
        Level level = blockEntity.getLevel();
        if (level == null) return;

        if (queuedLevel != level) {
            QUEUE.clear();
            queuedLevel = level;
        }

        BlockPos pos = blockEntity.getBlockPos().immutable();
        QUEUE.put(pos, new Entry(blockEntity, pos, packedLight, packedOverlay, occlusion));
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null || level != queuedLevel || QUEUE.isEmpty()) {
            QUEUE.clear();
            queuedLevel = level;
            return;
        }

        Vec3 cameraPos = event.getCamera().getPosition();
        List<Entry> entries = new ArrayList<>(QUEUE.values());
        QUEUE.clear();
        entries.sort(Comparator.comparingDouble((Entry entry) ->
                Vec3.atCenterOf(entry.pos()).distanceToSqr(cameraPos)).reversed());

        PoseStack poseStack = event.getPoseStack();
        try {
            for (Entry entry : entries) {
                BlockEntity blockEntity = entry.blockEntity();
                if (blockEntity.isRemoved()
                        || blockEntity.getLevel() != level
                        || level.getBlockEntity(entry.pos()) != blockEntity
                        || !(blockEntity instanceof CanvasDataHolder holder)) {
                    continue;
                }

                var textures = holder.painter$getCachedFaceTextures();
                if (textures == null || textures.isEmpty()) continue;

                poseStack.pushPose();
                try {
                    poseStack.translate(
                            entry.pos().getX() - cameraPos.x + 0.5,
                            entry.pos().getY() - cameraPos.y + 0.5,
                            entry.pos().getZ() - cameraPos.z + 0.5
                    );
                    CanvasBlockEntityRenderer.renderCanvasTexture(
                            level, entry.pos(), poseStack, BUFFER_SOURCE, textures,
                            entry.packedLight(), entry.packedOverlay(), entry.occlusion(),
                            CanvasRenderTypes::worldTranslucent
                    );
                } finally {
                    poseStack.popPose();
                }
            }
        } finally {
            BUFFER_SOURCE.endBatch();
        }
    }

    private record Entry(BlockEntity blockEntity, BlockPos pos, int packedLight,
                         int packedOverlay, boolean occlusion) {}
}
