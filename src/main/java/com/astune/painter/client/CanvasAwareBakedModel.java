package com.astune.painter.client;

import com.astune.painter.api.PixelMatrix;
import com.astune.painter.block.CanvasBlockEntity;
import com.astune.painter.data.CanvasData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CanvasAwareBakedModel implements IDynamicBakedModel {

    private final BakedModel original;

    public CanvasAwareBakedModel(BakedModel original) {
        this.original = original;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
                                             @NotNull ModelData data, @Nullable RenderType renderType) {
        List<BakedQuad> quads = new ArrayList<>(original.getQuads(state, side, rand, data, renderType));

        CanvasData canvas = data.get(CanvasBlockEntity.CANVAS_PROPERTY);
        if (canvas != null && renderType == RenderType.translucent()) {
            // 只在半透明层叠加像素，使方块原有纹理正常显示
            quads.addAll(PixelQuadGenerator.createQuads(canvas, state, side));
        }
        return quads;
    }


    private int colorToInt(float r, float g, float b, float a) {
        int red = (int)(r * 255) & 0xFF;
        int green = (int)(g * 255) & 0xFF;
        int blue = (int)(b * 255) & 0xFF;
        int alpha = (int)(a * 255) & 0xFF;
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    @Override
    public boolean useAmbientOcclusion() { return original.useAmbientOcclusion(); }
    @Override
    public boolean isGui3d() { return original.isGui3d(); }
    @Override
    public boolean usesBlockLight() { return original.usesBlockLight(); }
    @Override
    public boolean isCustomRenderer() { return original.isCustomRenderer(); }
    @Override
    public TextureAtlasSprite getParticleIcon() { return original.getParticleIcon(); }
    @Override
    public ItemTransforms getTransforms() { return original.getTransforms(); }
    @Override
    public ItemOverrides getOverrides() { return original.getOverrides(); }
    @Override
    public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        return ChunkRenderTypeSet.union(original.getRenderTypes(state, rand, data), ChunkRenderTypeSet.of(RenderType.translucent()));
    }
}