package com.astune.painter.api.blend;

@FunctionalInterface
public interface BlendFunction {
    /**
     * 执行颜色与效果的混合。
     * @param context 混合上下文，包含面、像素坐标、现有颜色、新颜色、效果值等
     * @return 混合后的最终颜色（ARGB），效果值可通过 context 直接写入面
     */
    boolean apply(BlendContext context);
}
