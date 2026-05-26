package com.astune.painter.api.render;

public interface CanvasPixelRenderer {
    /**
     * 判断是否应使用此渲染器处理给定上下文。
     * 默认返回 true，允许所有面使用。
     */
    default boolean canRender(RenderContext context) {
        return true;
    }

    /**
     * 渲染画布面。实现应负责向 bufferSource 绘制几何体。
     * @return true 表示渲染已完成，调用链中断；false 表示未处理，将尝试下一个渲染器或默认实现。
     */
    boolean renderFace(RenderContext context);
}