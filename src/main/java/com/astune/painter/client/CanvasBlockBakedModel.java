package com.astune.painter.client;

import com.astune.painter.api.PixelMatrix;
import com.astune.painter.block.CanvasBlockEntity;
import com.astune.painter.data.CanvasData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
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
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CanvasBlockBakedModel implements IDynamicBakedModel {

    public static final ModelProperty<CanvasBlockEntity> BE_PROPERTY = new ModelProperty<>();
    private final BakedModel originalModel;
    private static final ResourceLocation DOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("painter", "block/dot_white");

    public CanvasBlockBakedModel(BakedModel originalModel) {
        this.originalModel = originalModel;
    }
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                    RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        List<BakedQuad> quads = new ArrayList<>();
        CanvasBlockEntity be = data.get(BE_PROPERTY);
        if (be == null) return quads;

        BlockState mimicked = be.getMimickedState();
        if (mimicked.isAir()) return quads;

        BakedModel mimickedModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(mimicked);

        // 添加原方块四边形（包括 renderType == null）
        List<BakedQuad> originalQuads = mimickedModel.getQuads(mimicked, side, rand, data, renderType);
        quads.addAll(originalQuads);

        CanvasData canvas = be.getCanvasData();
        if (canvas != null && side == canvas.face()) {
            quads.addAll(createPixelQuads(canvas.face(), canvas.pixels()));
        }
        return quads;
    }

    // 3. 完整 createPixelQuads 方法（负责像素四边形）
    private List<BakedQuad> createPixelQuads(Direction face, PixelMatrix pixels) {
        List<BakedQuad> quads = new ArrayList<>();
        int size = PixelMatrix.SIZE;
        float step = 1.0f / size;

        // 微偏移防止穿插（沿用原逻辑：正方向 +0.005，负方向 -0.005）
        float out = 0.005f * (face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1);

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(DOT_TEXTURE);

        for (int x = 0; x < size; x++) {
            float x0 = x * step;
            float x1 = (x + 1) * step;
            for (int y = 0; y < size; y++) {
                int color = pixels.getColor(x, y);
                if ((color >> 24 & 0xFF) == 0) continue; // 透明跳过

                float r = ((color >> 16) & 0xFF) / 255f;
                float g = ((color >> 8) & 0xFF) / 255f;
                float b = (color & 0xFF) / 255f;
                float a = ((color >> 24) & 0xFF) / 255f;

                float y0 = y * step;
                float y1 = (y + 1) * step;

                float[][] quadVertices;
                float[][] uvs;

                switch (face) {
                    case DOWN:
                        // 面法线向下，水平轴为X (x)，垂直轴为Z (y)，深度为Y (1.0，向外微移)
                        quadVertices = new float[][]{
                                {y1, 1.0f + out, x0}, {y1, 1.0f + out, x1}, {y0, 1.0f + out, x1}, {y0, 1.0f + out, x0}
                        };
                        uvs = new float[][]{
                                {sprite.getU(x0 * 16), sprite.getV(y1 * 16)},
                                {sprite.getU(x1 * 16), sprite.getV(y1 * 16)},
                                {sprite.getU(x1 * 16), sprite.getV(y0 * 16)},
                                {sprite.getU(x0 * 16), sprite.getV(y0 * 16)}
                        };
                        break;
                    case UP:
                        // 面法线向上，水平轴为X (x)，垂直轴为Z (y)，深度为Y (0.0，向外微移)
                        quadVertices = new float[][]{
                                {y0, 0.0f + out, x0}, {y0, 0.0f + out, x1}, {y1, 0.0f + out, x1}, {y1, 0.0f + out, x0}
                        };
                        uvs = new float[][]{
                                {sprite.getU(x0 * 16), sprite.getV(y0 * 16)},
                                {sprite.getU(x1 * 16), sprite.getV(y0 * 16)},
                                {sprite.getU(x1 * 16), sprite.getV(y1 * 16)},
                                {sprite.getU(x0 * 16), sprite.getV(y1 * 16)}
                        };
                        break;
                    case NORTH:
                        // 面法线向北，水平轴为X (x)，垂直轴为Y (y)，深度为Z (0.0，向外微移)
                        quadVertices = new float[][]{
                                {y0, x0, 0.0f + out}, {y0, x1, 0.0f + out}, {y1, x1, 0.0f + out}, {y1, x0, 0.0f + out}
                        };
                        uvs = new float[][]{
                                {sprite.getU(x0 * 16), sprite.getV(y0 * 16)},
                                {sprite.getU(x1 * 16), sprite.getV(y0 * 16)},
                                {sprite.getU(x1 * 16), sprite.getV(y1 * 16)},
                                {sprite.getU(x0 * 16), sprite.getV(y1 * 16)}
                        };
                        break;
                    case SOUTH:
                        // 面法线向南，水平轴为X (x)，垂直轴为Y (y)，深度为Z (1.0，向外微移)
                        quadVertices = new float[][]{
                                {y0, x1, 1.0f + out}, {y0, x0, 1.0f + out}, {y1, x0, 1.0f + out}, {y1, x1, 1.0f + out}
                        };
                        uvs = new float[][]{
                                {sprite.getU(x1 * 16), sprite.getV(y0 * 16)},
                                {sprite.getU(x0 * 16), sprite.getV(y0 * 16)},
                                {sprite.getU(x0 * 16), sprite.getV(y1 * 16)},
                                {sprite.getU(x1 * 16), sprite.getV(y1 * 16)}
                        };
                        break;
                    case WEST:
                        // 面法线向西，水平轴为Z (x)，垂直轴为Y (y)，深度为X (0.0，向外微移)
                        quadVertices = new float[][]{
                                {0.0f + out, y0, x0}, {0.0f + out, y0, x1}, {0.0f + out, y1, x1}, {0.0f + out, y1, x0}
                        };
                        uvs = new float[][]{
                                {sprite.getU(x0 * 16), sprite.getV(y0 * 16)},
                                {sprite.getU(x1 * 16), sprite.getV(y0 * 16)},
                                {sprite.getU(x1 * 16), sprite.getV(y1 * 16)},
                                {sprite.getU(x0 * 16), sprite.getV(y1 * 16)}
                        };
                        break;
                    case EAST:
                        // 面法线向东，水平轴为Z (x)，垂直轴为Y (y)，深度为X (1.0，向外微移)
                        quadVertices = new float[][]{
                                {1.0f + out, y0, x1}, {1.0f + out, y0, x0}, {1.0f + out, y1, x0}, {1.0f + out, y1, x1}
                        };
                        uvs = new float[][]{
                                {sprite.getU(x1 * 16), sprite.getV(y0 * 16)},
                                {sprite.getU(x0 * 16), sprite.getV(y0 * 16)},
                                {sprite.getU(x0 * 16), sprite.getV(y1 * 16)},
                                {sprite.getU(x1 * 16), sprite.getV(y1 * 16)}
                        };
                        break;
                    default:
                        return quads;
                }

                int[] verts = new int[32];
                int packedFace = face.get3DDataValue();
                for (int i = 0; i < 4; i++) {
                    int idx = i * 8;
                    verts[idx]   = Float.floatToRawIntBits(quadVertices[i][0]);
                    verts[idx+1] = Float.floatToRawIntBits(quadVertices[i][1]);
                    verts[idx+2] = Float.floatToRawIntBits(quadVertices[i][2]);
                    verts[idx+3] = colorToInt(r, g, b, a);
                    verts[idx+4] = Float.floatToRawIntBits(uvs[i][0]);
                    verts[idx+5] = Float.floatToRawIntBits(uvs[i][1]);
                    verts[idx+6] = 0;
                    verts[idx+7] = packedFace;
                }
                quads.add(new BakedQuad(verts, -1, face, sprite, true));
            }
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
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        List<RenderType> types = new ArrayList<>();
        // 获取原方块的 RenderTypes
        CanvasBlockEntity be = data.get(BE_PROPERTY);
        if (be != null) {
            BakedModel mimickedModel = Minecraft.getInstance().getBlockRenderer()
                    .getBlockModel(be.getMimickedState());
            types.addAll(mimickedModel.getRenderTypes(be.getMimickedState(), rand, ModelData.EMPTY).asList());
        }
        if (be != null && be.getCanvasData() != null) {
            types.add(RenderType.translucent());
        }

        if (types.isEmpty()) {
            types.add(RenderType.solid());
        }

        return ChunkRenderTypeSet.of(types);
    }

    // 以下委托给原始模型
    @Override
    public boolean useAmbientOcclusion() { return true; }
    @Override
    public boolean isGui3d() { return originalModel.isGui3d(); }
    @Override
    public boolean usesBlockLight() { return true; }
    @Override
    public boolean isCustomRenderer() { return false; }
    @Override
    public TextureAtlasSprite getParticleIcon() { return originalModel.getParticleIcon(); }
    @Override
    public ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }
    @Override
    public ItemTransforms getTransforms() { return originalModel.getTransforms(); }
}