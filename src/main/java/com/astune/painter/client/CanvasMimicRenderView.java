package com.astune.painter.client;

import com.astune.painter.block.CanvasBlock;
import com.astune.painter.block.CanvasBlockEntity;
import com.astune.painter.network.ClientCanvasCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;

import java.util.HashMap;
import java.util.Map;

/**
 * Block view used only while tesselating a mimicked canvas model.
 * Canvas neighbors are exposed as their mimicked states so vanilla face
 * occlusion checks see the same block on both sides of a shared face.
 */
final class CanvasMimicRenderView implements BlockAndTintGetter {
    private final BlockAndTintGetter delegate;
    private final Map<BlockPos, BlockState> resolvedStates = new HashMap<>();

    CanvasMimicRenderView(BlockAndTintGetter delegate) {
        this.delegate = delegate;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        BlockPos immutablePos = pos.immutable();
        BlockState cached = resolvedStates.get(immutablePos);
        if (cached != null) return cached;

        BlockState actual = delegate.getBlockState(immutablePos);
        BlockState resolved = actual;
        if (actual.getBlock() instanceof CanvasBlock) {
            BlockEntity blockEntity = delegate.getBlockEntity(immutablePos);
            BlockState mimicked = null;
            if (blockEntity instanceof CanvasBlockEntity canvasBlockEntity) {
                mimicked = canvasBlockEntity.getMimickedState();
            }
            if (mimicked == null) mimicked = ClientCanvasCache.getMimickedState(immutablePos);
            if (mimicked != null) resolved = mimicked;
        }

        resolvedStates.put(immutablePos, resolved);
        return resolved;
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return delegate.getBlockEntity(pos);
    }

    @Override
    public net.minecraft.world.level.material.FluidState getFluidState(BlockPos pos) {
        return delegate.getFluidState(pos);
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        return delegate.getShade(direction, shade);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return delegate.getLightEngine();
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver colorResolver) {
        return delegate.getBlockTint(pos, colorResolver);
    }

    @Override
    public int getBrightness(LightLayer lightType, BlockPos pos) {
        return delegate.getBrightness(lightType, pos);
    }

    @Override
    public int getRawBrightness(BlockPos pos, int amount) {
        return delegate.getRawBrightness(pos, amount);
    }

    @Override
    public int getMinBuildHeight() {
        return delegate.getMinBuildHeight();
    }

    @Override
    public int getHeight() {
        return delegate.getHeight();
    }

    @Override
    public net.neoforged.neoforge.client.model.data.ModelData getModelData(BlockPos pos) {
        return delegate.getModelData(pos);
    }
}
