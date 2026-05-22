package com.astune.painter.block;

import com.astune.painter.network.ClientCanvasCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class CanvasBlockModel implements BakedModel {

    public static final ModelProperty<BlockState> COPIED_BLOCK = new ModelProperty<>();
    private final BakedModel defaultModel;

    public CanvasBlockModel(BakedModel existingModel) {
        this.defaultModel = existingModel;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                             @NotNull RandomSource rand, @NotNull ModelData data,
                                             @Nullable RenderType renderType) {
        BakedModel renderModel = defaultModel;
        BlockState mimicked = data.get(COPIED_BLOCK);
        if (mimicked != null) {
            Minecraft mc = Minecraft.getInstance();
            BlockRenderDispatcher blockRendererDispatcher = mc.getBlockRenderer();
            renderModel = blockRendererDispatcher.getBlockModel(mimicked);

        }
        return renderModel.getQuads(state,side,rand,data,renderType);
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos,
                                           @NotNull BlockState state, @NotNull ModelData modelData) {
        BlockState mimicked = null;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CanvasBlockEntity canvasBE) {
            mimicked = canvasBE.getMimickedState();
        }
        if (mimicked == null) {
            mimicked = ClientCanvasCache.getMimickedState(pos);
        }
        if (mimicked != null) {
            return modelData.derive().with(COPIED_BLOCK, mimicked).build();
        }
        return modelData;
    }

    // ----- 以下委托给 defaultModel -----

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource rand) {
        // 这个方法在正确架构下不会被调用，但防御性委托
        return defaultModel.getQuads(state, direction, rand);
    }

    @Override
    public boolean useAmbientOcclusion() { return defaultModel.useAmbientOcclusion(); }

    @Override
    public boolean isGui3d() { return defaultModel.isGui3d(); }

    @Override
    public boolean usesBlockLight() { return defaultModel.usesBlockLight(); }

    @Override
    public boolean isCustomRenderer() { return defaultModel.isCustomRenderer(); }

    @Override
    public TextureAtlasSprite getParticleIcon() { return defaultModel.getParticleIcon(); }

    @Override
    public ItemOverrides getOverrides() { return defaultModel.getOverrides(); }
}
