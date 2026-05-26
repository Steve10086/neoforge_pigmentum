// api/PixelProvider.java
package com.astune.painter.api;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

/**
 * 提供图案中每个像素的颜色。
 * dx, dy 是相对于命中点的偏移（以像素为单位，可以是浮点数）。
 * 返回 null 表示该位置不绘制。
 */
public interface PixelProvider {
    BlendMode getBlendMode();
    default BlendMode getBlendMode(double dx, double dy) { return getBlendMode(); }

    @Nullable
    Integer getPixel(double dx, double dy);

    /**
     * 返回此像素点的效果值映射，默认空。效果键如 "glow"、"roughness" 等。
     */
    default Map<String, Integer> getEffectValues(double dx, double dy) {
        return Collections.emptyMap();
    }
}
