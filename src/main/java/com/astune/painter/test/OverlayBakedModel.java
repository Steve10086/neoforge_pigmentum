package com.astune.painter.test;

import com.astune.painter.api.PaintingManager;
import com.astune.painter.data.PaintLayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OverlayBakedModel implements IDynamicBakedModel {

    private static final ResourceLocation DOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("painter", "block/dot_white");

    // ModelProperty 用于传递 BlockPos 和是否包含画层
    public static final ModelProperty<BlockPos> POS_PROPERTY = new ModelProperty<>();
    public static final ModelProperty<Boolean> HAS_PAINTING = new ModelProperty<>();

    private final BakedModel original;

    public OverlayBakedModel(BakedModel original) {
        this.original = original;
    }
    public static final ModelProperty<Integer> DATA_HASH = new ModelProperty<>();

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        ModelData originalData = original.getModelData(level, pos, state, modelData);
        boolean hasPainting = false;
        int hash = 0;
        if (level instanceof Level realLevel) {
            var clientLevel = realLevel.isClientSide ? realLevel : Minecraft.getInstance().level;
            if (clientLevel != null) {
                for (Direction face : Direction.values()) {
                    var opt = PaintingManager.getLayer(clientLevel, pos, face);
                    if (opt != null) {
                        hasPainting = true;
                        // 将画层数据混入哈希（像素颜色 + 位置 + 面）
                        hash = 31 * hash + face.ordinal();
                        hash = 31 * hash + Arrays.hashCode(opt.getPixels().getRawData());
                    }
                }
            }
        }
        System.out.println("Painting: " + pos);
        return originalData.derive()
                .with(POS_PROPERTY, pos)
                .with(HAS_PAINTING, hasPainting)
                .with(DATA_HASH, hash) // 变化时触发刷新
                .build();
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return original.getRenderTypes(state, rand, data);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                    RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        // 基础四边形列表
        List<BakedQuad> quads = new ArrayList<>(original.getQuads(state, side, rand, data, renderType));
        if (side == null) return quads;

        // 仅在半透明层处理画层，避免在固体层重复绘制
        //if (renderType != RenderType.translucent()) return quads;

        BlockPos pos = data.get(POS_PROPERTY);
        if (pos == null) return quads;

        // 获取客户端的实际 Level（优先使用 Minecraft.getInstance().level）
        var level = Minecraft.getInstance().level;
        if (level == null) return quads;
        PaintLayerData layer = PaintingManager.getLayer(level, pos, side);
        if (layer == null) return quads;

        // 获取该面的原始四边形集合，用于计算表面几何范围
        List<BakedQuad> faceQuads = original.getQuads(state, side, rand, ModelData.EMPTY, renderType);
        if (faceQuads.isEmpty()) return quads;

        // 准备像素渲染所需的几何信息
        int[] outerVerts = getOutermostQuad(faceQuads, side);
        if (outerVerts == null) return quads;

        // 计算该面实际的二维范围 (U,V) 与深度 (D)
        float minU = Float.MAX_VALUE, maxU = -Float.MAX_VALUE;
        float minV = Float.MAX_VALUE, maxV = -Float.MAX_VALUE;
        float depth = Float.NaN;
        for (int i = 0; i < 4; i++) {
            int idx = i * 8;
            float vx = Float.intBitsToFloat(outerVerts[idx]);
            float vy = Float.intBitsToFloat(outerVerts[idx + 1]);
            float vz = Float.intBitsToFloat(outerVerts[idx + 2]);
            float u, v, d;
            switch (side) {
                case NORTH: case SOUTH: u = vx; v = vy; d = vz; break;
                case EAST:  case WEST:  u = vz; v = vy; d = vx; break;
                case UP:    case DOWN:  u = vx; v = vz; d = vy; break;
                default: return quads;
            }
            minU = Math.min(minU, u); maxU = Math.max(maxU, u);
            minV = Math.min(minV, v); maxV = Math.max(maxV, v);
            if (Float.isNaN(depth)) depth = d;
        }

        // 像素网格尺寸（每像素世界坐标大小）
        float pixelSizeU = (maxU - minU) / 16.0f;
        float pixelSizeV = (maxV - minV) / 16.0f;

        // 获取白色纹理精灵（用于承载顶点颜色）
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(DOT_TEXTURE);
        if (sprite == null) return quads;

        // 遍历画层的所有像素，为每个不透明像素生成一个四边形
        var pixels = layer.getPixels();
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                int color = pixels.getColor(x, y);
                if ((color >> 24 & 0xFF) == 0) continue; // 完全透明跳过

                // 像素在世界中的位置（左下角坐标）
                float px = minU + x * pixelSizeU;
                float py = minV + y * pixelSizeV;
                // 四边形顶点
                // 注意：Y轴在面的局部坐标系中向上，与像素行 y 对应
                float px1 = px;
                float px2 = px + pixelSizeU;
                float py1 = py;
                float py2 = py + pixelSizeV;

                // 微偏移避免 Z-Fighting
                float out = 0.001f * (side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1);
                float posZ = depth + out;

                float[][] quadVertices;
                switch (side) {
                    case DOWN:
                        quadVertices = new float[][]{
                                {py2, posZ, px1}, {py2, posZ, px2}, {py1, posZ, px2}, {py1, posZ, px1}
                        };
                        break;
                    case UP:
                        quadVertices = new float[][]{
                                {py1, posZ, px1}, {py1, posZ, px2}, {py2, posZ, px2}, {py2, posZ, px1}
                        };
                        break;
                    case NORTH:
                        quadVertices = new float[][]{
                                {py1, px1, posZ}, {py1, px2, posZ}, {py2, px2, posZ}, {py2, px1, posZ}
                        };
                        break;
                    case SOUTH:
                        quadVertices = new float[][]{
                                {py1, px2, posZ}, {py1, px1, posZ}, {py2, px1, posZ}, {py2, px2, posZ}
                        };
                        break;
                    case WEST:
                        quadVertices = new float[][]{
                                {posZ, py1, px1}, {posZ, py1, px2}, {posZ, py2, px2}, {posZ, py2, px1}
                        };
                        break;
                    case EAST:
                        quadVertices = new float[][]{
                                {posZ, py1, px2}, {posZ, py1, px1}, {posZ, py2, px1}, {posZ, py2, px2}
                        };
                        break;
                    default: continue;
                }

                int[] verts = new int[32];
                for (int vi = 0; vi < 4; vi++) {
                    int idx = vi * 8;
                    verts[idx]   = Float.floatToRawIntBits(quadVertices[vi][0]);
                    verts[idx+1] = Float.floatToRawIntBits(quadVertices[vi][1]);
                    verts[idx+2] = Float.floatToRawIntBits(quadVertices[vi][2]);
                    verts[idx+3] = color;
                    // UV 映射到白色纹理的四个角即可（因为我们只用顶点颜色）
                    if (vi == 0) { verts[idx+4] = Float.floatToRawIntBits(sprite.getU0()); verts[idx+5] = Float.floatToRawIntBits(sprite.getV0()); }
                    else if (vi == 1) { verts[idx+4] = Float.floatToRawIntBits(sprite.getU1()); verts[idx+5] = Float.floatToRawIntBits(sprite.getV0()); }
                    else if (vi == 2) { verts[idx+4] = Float.floatToRawIntBits(sprite.getU1()); verts[idx+5] = Float.floatToRawIntBits(sprite.getV1()); }
                    else { verts[idx+4] = Float.floatToRawIntBits(sprite.getU0()); verts[idx+5] = Float.floatToRawIntBits(sprite.getV1()); }
                    verts[idx+6] = 0;
                    verts[idx+7] = side.get3DDataValue();
                }
                // shade = false 保证顶点颜色直接使用，不受环境影响
                quads.add(new BakedQuad(verts, -1, side, sprite, false));
            }
        }
        return quads;
    }

    @Nullable
    private static int[] getOutermostQuad(List<BakedQuad> quads, Direction face) {
        if (quads.isEmpty()) return null;
        int axis = face.getAxis().ordinal(); // 0:X, 1:Y, 2:Z
        boolean positive = face.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        int[] bestVerts = null;
        float bestDepth = positive ? -Float.MAX_VALUE : Float.MAX_VALUE;
        for (BakedQuad quad : quads) {
            int[] verts = quad.getVertices();
            float v = Float.intBitsToFloat(verts[axis]);
            if (positive ? (v > bestDepth) : (v < bestDepth)) {
                bestDepth = v;
                bestVerts = verts;
            }
        }
        return bestVerts;
    }

    // 委托方法保持不变
    @Override public boolean useAmbientOcclusion() { return original.useAmbientOcclusion(); }
    @Override public boolean isGui3d() { return original.isGui3d(); }
    @Override public boolean usesBlockLight() { return original.usesBlockLight(); }
    @Override public boolean isCustomRenderer() { return false; }
    @Override public TextureAtlasSprite getParticleIcon() { return original.getParticleIcon(); }
    @Override public ItemOverrides getOverrides() { return original.getOverrides(); }
    @Override public ItemTransforms getTransforms() { return original.getTransforms(); }
}