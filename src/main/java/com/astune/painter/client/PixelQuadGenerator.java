package com.astune.painter.client;

import com.astune.painter.Painter;
import com.astune.painter.data.CanvasData;
import com.astune.painter.api.PixelMatrix;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PixelQuadGenerator {

    // 白点纹理，用于像素四边形
    private static final ResourceLocation DOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(Painter.MODID, "block/pixel_dot");

    /**
     * 生成画布像素四边形
     * @param canvas 画布数据
     * @param state  当前方块状态（可选，用于获取属性）
     * @param side   要渲染的面，为 null 时渲染所有面
     * @return 指定面的像素四边形列表
     */
    public static List<BakedQuad> createQuads(CanvasData canvas, @Nullable BlockState state, @Nullable Direction side) {
        List<BakedQuad> quads = new ArrayList<>();
        if (canvas == null || canvas.pixels() == null) return quads;

        Direction face = canvas.face(); // 画布仅绘制在存储的面
        // 如果传入了 side 且与画布面不符，则不添加任何四边形
        if (side != null && side != face) return quads;

        // 生成该面所有像素四边形
        quads.addAll(createPixelQuads(face, (PixelMatrix) canvas.pixels()));
        return quads;
    }

    /**
     * 根据面方向和像素矩阵生成四边形列表（顶点顺序符合逆时针）
     */
    private static List<BakedQuad> createPixelQuads(Direction face, PixelMatrix pixels) {
        List<BakedQuad> quads = new ArrayList<>();
        int size = PixelMatrix.SIZE;
        float step = 1.0f / size;

        // 微偏移防止穿插
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

    private static int colorToInt(float r, float g, float b, float a) {
        int ri = (int)(r * 255) & 0xFF;
        int gi = (int)(g * 255) & 0xFF;
        int bi = (int)(b * 255) & 0xFF;
        int ai = (int)(a * 255) & 0xFF;
        return (ai << 24) | (ri << 16) | (gi << 8) | bi;
    }
}