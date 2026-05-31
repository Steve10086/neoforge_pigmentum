package com.astune.painter.api.imageProvider;

import com.astune.painter.api.CanvasFace;
import com.astune.painter.api.PixelMatrix;
import com.mojang.blaze3d.platform.NativeImage;

public class DefaultCanvasImageProvider implements CanvasImageProvider {
    @Override
    public String name() {
        return "default";
    }

    @Override
    public NativeImage createImage(CanvasFace face) {
        PixelMatrix matrix = face.pixels();
        if (matrix == null || matrix.getWidth() <= 0 || matrix.getHeight() <= 0) return null;
        int w = matrix.getWidth(), h = matrix.getHeight();
        NativeImage image = null;
        try {
            image = new NativeImage(w, h, false);
            image.getPixelRGBA(0, 0); // 触发分配检查
        } catch (Exception e) {
            if (image != null) image.close();
            return null;
        }

        try {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int argb = matrix.getPixel(x, y);
                    int a = (argb >> 24) & 0xFF;
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;

                    int bgr = (b << 16) | (g << 8) | r;
                    int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                    if (bgr != 0 && a == 0) {
                        abgr = 255 << 24 | bgr;
                    }
                    image.setPixelRGBA(x, y, abgr);
                }
            }
        } catch (Exception e) {
            image.close();
            return null;
        }
        return image;
    }
}
