package com.astune.painter.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.astune.painter.api.IPixelMatrix;
import com.astune.painter.data.PaintLayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class PaintTextureManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaintTextureManager.class);
    public static final int GRID_SIZE = 64;
    public static final int TEX_SIZE = GRID_SIZE * IPixelMatrix.SIZE;
    private static final Map<String, Cell> cells = new HashMap<>();
    private static DynamicTexture texture;
    private static NativeImage image;
    private static int nextCell = 0;

    public static void init() {
        image = new NativeImage(TEX_SIZE, TEX_SIZE, false);
        texture = new DynamicTexture(image);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("painter", "paint_atlas");
        Minecraft.getInstance().getTextureManager().register(id, texture);
        texture.upload();
        LOGGER.info("[Painter] Texture atlas initialized ({}×{})", TEX_SIZE, TEX_SIZE);
    }

    public static Cell getOrCreateCell(PaintLayerData layer) {
        String key = key(layer);
        return cells.computeIfAbsent(key, k -> {
            Cell cell = allocCell();
            LOGGER.debug("[Painter] Allocated new cell ({},{}) for {} face {}", cell.x, cell.y, layer.getPos(), layer.getFace());
            return cell;
        });
    }

    public static void updateCell(PaintLayerData layer) {
        Cell cell = getOrCreateCell(layer);
        IPixelMatrix pixels = layer.getPixels();
        int baseX = cell.x * IPixelMatrix.SIZE;
        int baseY = cell.y * IPixelMatrix.SIZE;
        int[] raw = pixels.getRawData();
        for (int y = 0; y < IPixelMatrix.SIZE; y++) {
            for (int x = 0; x < IPixelMatrix.SIZE; x++) {
                image.setPixelRGBA(baseX + x, baseY + y, raw[y * IPixelMatrix.SIZE + x]);
            }
        }
        texture.bind();
        image.upload(0, baseX, baseY, 0, 0, IPixelMatrix.SIZE, IPixelMatrix.SIZE, false, false);
        LOGGER.debug("[Painter] Texture cell updated at grid ({},{}) for {} face {}", cell.x, cell.y, layer.getPos(), layer.getFace());
    }

    public static void removeCell(PaintLayerData layer) {
        cells.remove(key(layer));
        LOGGER.debug("[Painter] Cell removed for {} face {}", layer.getPos(), layer.getFace());
    }

    public static ResourceLocation getTexture() {
        return ResourceLocation.fromNamespaceAndPath("painter", "paint_atlas");
    }

    private static Cell allocCell() {
        if (nextCell >= GRID_SIZE * GRID_SIZE) {
            throw new IllegalStateException("[Painter] Texture atlas full! Increase GRID_SIZE.");
        }
        int x = nextCell % GRID_SIZE;
        int y = nextCell / GRID_SIZE;
        nextCell++;
        return new Cell(x, y);
    }

    private static String key(PaintLayerData layer) {
        return layer.getPos().toShortString() + "." + layer.getFace().getName();
    }

    public record Cell(int x, int y) {
        public float getU0() { return (float) (x * IPixelMatrix.SIZE) / TEX_SIZE; }
        public float getV0() { return (float) (y * IPixelMatrix.SIZE) / TEX_SIZE; }
        public float getU1() { return (float) ((x + 1) * IPixelMatrix.SIZE) / TEX_SIZE; }
        public float getV1() { return (float) ((y + 1) * IPixelMatrix.SIZE) / TEX_SIZE; }
    }
}