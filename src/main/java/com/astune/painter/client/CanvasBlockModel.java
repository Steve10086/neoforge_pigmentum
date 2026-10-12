package com.astune.painter.client;

import com.astune.painter.block.CanvasBlockEntity;
import com.astune.painter.network.ClientCanvasCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class CanvasBlockModel implements BakedModel {

    public static final ModelProperty<BlockState> COPIED_BLOCK = new ModelProperty<>();
    private final BakedModel defaultModel;
    private final BlockModelShaper modelShaper;

    public CanvasBlockModel(BakedModel existingModel) {
        this.defaultModel = existingModel;
        this.modelShaper = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper();
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                             @NotNull RandomSource rand, @NotNull ModelData data,
                                             @Nullable RenderType renderType) {
        BlockState mimicked = data.get(COPIED_BLOCK);
        if (mimicked == null) {
            return List.of();
        }

        BakedModel renderModel = modelShaper.getBlockModel(mimicked);
        ModelData modelData = renderModel.getModelData(null, BlockPos.ZERO, mimicked, ModelData.EMPTY);
        return renderModel.getQuads(mimicked, side, rand, modelData, renderType);
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos,
                                           @NotNull BlockState state, @NotNull ModelData modelData) {
        BlockState mimicked = null;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CanvasBlockEntity canvasBlockEntity) {
            mimicked = canvasBlockEntity.getMimickedState();
        }
        if (mimicked == null) {
            mimicked = ClientCanvasCache.getMimickedState(pos);
        }
        if (mimicked != null) {
            return modelData.derive().with(COPIED_BLOCK, mimicked).build();
        }
        return modelData;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource rand) {
        return List.of();
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
