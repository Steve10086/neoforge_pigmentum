package com.astune.painter.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.TriState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

final class CanvasMimicBakedModel implements BakedModel {
    private final BakedModel delegate;
    private final BlockAndTintGetter level;
    private final BlockPos pos;

    CanvasMimicBakedModel(BakedModel delegate, BlockAndTintGetter level, BlockPos pos) {
        this.delegate = delegate;
        this.level = level;
        this.pos = pos;
    }

    @Override
    @Deprecated
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
        if (shouldCull(state, side)) return List.of();
        return delegate.getQuads(state, side, random);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                   RandomSource random, ModelData modelData,
                                   @Nullable RenderType renderType) {
        if (shouldCull(state, side)) return List.of();
        return delegate.getQuads(state, side, random, modelData, renderType);
    }

    private boolean shouldCull(@Nullable BlockState state, @Nullable Direction side) {
        return state != null && side != null
                && !Block.shouldRenderFace(state, level, pos, side, pos.relative(side));
    }

    @Override
    public boolean useAmbientOcclusion() {
        return delegate.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return delegate.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return delegate.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return delegate.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return delegate.getParticleIcon();
    }

    @Override
    public ItemOverrides getOverrides() {
        return delegate.getOverrides();
    }

    @Override
    public ItemTransforms getTransforms() {
        return delegate.getTransforms();
    }

    @Override
    public TriState useAmbientOcclusion(BlockState state, ModelData modelData, @Nullable RenderType renderType) {
        return delegate.useAmbientOcclusion(state, modelData, renderType);
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        return delegate.getModelData(level, pos, state, modelData);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData modelData) {
        return delegate.getParticleIcon(modelData);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData modelData) {
        return delegate.getRenderTypes(state, random, modelData);
    }

    @Override
    public BakedModel applyTransform(net.minecraft.world.item.ItemDisplayContext transformType,
                                     com.mojang.blaze3d.vertex.PoseStack poseStack,
                                     boolean applyLeftHandTransform) {
        return delegate.applyTransform(transformType, poseStack, applyLeftHandTransform);
    }
}
