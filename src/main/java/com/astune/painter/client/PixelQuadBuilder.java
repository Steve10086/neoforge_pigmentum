package com.astune.painter.client;

import com.astune.painter.api.CanvasFace;
import com.astune.painter.api.PixelMatrix;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class PixelQuadBuilder {

    /**
     * 从方块图集获取纯白 sprite（用于占位，实际颜色由顶点色决定）
     */
    public static TextureAtlasSprite getBlankSprite() {
        Function<ResourceLocation, TextureAtlasSprite> atlas = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS);
        // white 由原版在 ModelBakery 中自动添加，保证存在
        TextureAtlasSprite sprite = atlas.apply(ResourceLocation.withDefaultNamespace("dot_white"));
        if (sprite == null) {
            sprite = atlas.apply(ResourceLocation.withDefaultNamespace("missingno"));
        }
        return sprite;
    }


    /**
     * 为单个 CanvasFace 生成合并后的 BakedQuad 列表
     */
    public static List<BakedQuad> createMergedQuads(CanvasFace face) {
        PixelMatrix matrix = face.pixels();
        int w = matrix.getWidth();
        int h = matrix.getHeight();
        Direction dir = face.primaryFace();
        Vec3 offset = face.centerOffset();

        int[][] colors = new int[h][w];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                colors[y][x] = matrix.getPixel(x, y);

        boolean[][] visited = new boolean[h][w];
        List<BakedQuad> quads = new ArrayList<>();

        for (int y = 0; y < h; y++) {
            int x = 0;
            while (x < w) {
                if (visited[y][x] || (colors[y][x] >>> 24) == 0) {
                    x++;
                    continue;
                }
                int color = colors[y][x];
                int startX = x;
                while (x < w && colors[y][x] == color && !visited[y][x] && (color >>> 24) != 0) {
                    x++;
                }
                int endX = x - 1;
                for (int i = startX; i <= endX; i++) visited[y][i] = true;

                BakedQuad quad = buildQuad(startX, y, endX, y, w, h, color, dir, offset);
                if (quad != null) quads.add(quad);
            }
        }
        return quads;
    }

    private static BakedQuad buildQuad(int x1, int y1, int x2, int y2,
                                       int canvasW, int canvasH,
                                       int argb, Direction face, Vec3 centerOffset) {
        float unitU = 1f / canvasW;
        float unitV = 1f / canvasH;
        float uMin = x1 * unitU;
        float uMax = (x2 + 1) * unitU;
        float vMin = y1 * unitV;
        float vMax = (y2 + 1) * unitV;

        float localMinX = uMin - 0.5f;
        float localMaxX = uMax - 0.5f;
        float localMinY = 0.5f - vMax;
        float localMaxY = 0.5f - vMin;

        Vec3 v00 = new Vec3(localMinX, localMinY, 0);
        Vec3 v10 = new Vec3(localMaxX, localMinY, 0);
        Vec3 v11 = new Vec3(localMaxX, localMaxY, 0);
        Vec3 v01 = new Vec3(localMinX, localMaxY, 0);

        Vec3[] corners = rotateAndOffset(v00, v10, v11, v01, face, centerOffset);

        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        int abgr = (a << 24) | (b << 16) | (g << 8) | r;

        int nx = face.getStepX() * 127;
        int ny = face.getStepY() * 127;
        int nz = face.getStepZ() * 127;
        int normalPacked = (nz << 16) | (ny << 8) | nx;

        int[] vertexData = new int[4 * DefaultVertexFormat.BLOCK.getVertexSize()];
        for (int i = 0; i < 4; i++) {
            Vec3 corner = corners[i];
            int base = i * DefaultVertexFormat.BLOCK.getVertexSize();

            vertexData[base]     = Float.floatToRawIntBits((float) corner.x);
            vertexData[base + 1] = Float.floatToRawIntBits((float) corner.y);
            vertexData[base + 2] = Float.floatToRawIntBits((float) corner.z);
            vertexData[base + 3] = abgr;
            vertexData[base + 4] = 0;          // UV0 u
            vertexData[base + 5] = 0;          // UV0 v
            vertexData[base + 6] = (15 << 4) | 15; // UV2 (满光照)
            vertexData[base + 7] = normalPacked;
        }

        return new BakedQuad(vertexData, -1, face, getBlankSprite(), false);
    }

    private static Vec3[] rotateAndOffset(Vec3 v00, Vec3 v10, Vec3 v11, Vec3 v01,
                                          Direction face, Vec3 centerOffset) {
        Vec3 right, up;
        switch (face) {
            case NORTH: right = new Vec3(-1, 0, 0); up = new Vec3(0, 1, 0); break;
            case SOUTH: right = new Vec3(1, 0, 0);  up = new Vec3(0, 1, 0); break;
            case EAST:  right = new Vec3(0, 0, 1);  up = new Vec3(0, 1, 0); break;
            case WEST:  right = new Vec3(0, 0, -1); up = new Vec3(0, 1, 0); break;
            case UP:    right = new Vec3(1, 0, 0);  up = new Vec3(0, 0, 1); break;
            case DOWN:  right = new Vec3(1, 0, 0);  up = new Vec3(0, 0, -1); break;
            default: throw new IllegalArgumentException();
        }

        Vec3 blockCenter = new Vec3(0.5, 0.5, 0.5).add(centerOffset);
        Vec3 c00 = blockCenter.add(right.scale(v00.x)).add(up.scale(v00.y));
        Vec3 c10 = blockCenter.add(right.scale(v10.x)).add(up.scale(v10.y));
        Vec3 c11 = blockCenter.add(right.scale(v11.x)).add(up.scale(v11.y));
        Vec3 c01 = blockCenter.add(right.scale(v01.x)).add(up.scale(v01.y));

        return new Vec3[]{c00, c10, c11, c01};
    }
}