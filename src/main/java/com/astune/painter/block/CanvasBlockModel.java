package com.astune.painter.block;

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
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

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

        // 走原版完整流程：被模仿方块的模型 + 被模仿方块的 ModelData
        // 确保多方块模型（栅栏、活板门）得到正确的属性填充
        BakedModel renderModel = modelShaper.getBlockModel(mimicked);
        // getModelData 需要真实的 level 和 pos 来计算方块属性（如连接状态）。
        // chunk builder 不传 level/pos 给 getQuads，但 getModelData 在 chunk 编译前
        // 已被调用并将结果放入 ModelData。这里我们不知道 level/pos，退而次之：
        // 传入 null level + BlockPos.ZERO，大多数方块属性基于 BlockState 本身即可解析。
        // 连接性属性（如栅栏）会走 BlockState 的 canSurvive/getStateDefinition 推导。
        ModelData md = renderModel.getModelData(null, BlockPos.ZERO, mimicked, ModelData.EMPTY);
        return renderModel.getQuads(mimicked, side, rand, md, renderType);
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
