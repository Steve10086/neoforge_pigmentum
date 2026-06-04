package com.astune.painter.api.render;

import com.astune.painter.item.EffectCreator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CanvasRendererRegistry {
    private static CanvasPixelRenderer defaultRenderer = new DefaultCanvasPixelRenderer();
    private static final List<Entry> RENDERERS = new ArrayList<>(List.of(new Entry(defaultRenderer, Integer.MIN_VALUE)));

    static class Entry {
        final CanvasPixelRenderer renderer;
        final int priority;

        Entry(CanvasPixelRenderer renderer, int priority) {
            this.renderer = renderer;
            this.priority = priority;
        }
    }

    /**
     * 注册自定义像素渲染器。优先级越高越先被调用。
     */
    public static synchronized void registerPixelRenderer(CanvasPixelRenderer renderer, int priority) {
        RENDERERS.add(new Entry(renderer, priority));
        RENDERERS.sort(Comparator.comparingInt(e -> -e.priority));
    }

    /**
     * 获取当前应使用的渲染器。
     * 遍历注册表，返回第一个 canRender 为 true 的渲染器；若无，返回默认渲染器。
     */
    public static CanvasPixelRenderer resolve(RenderContext context) {
        for (Entry entry : RENDERERS) {
            if (entry.renderer.canRender(context)) {
                return entry.renderer;
            }
        }
        return null;
    }

    /**
     * 设置默认渲染器（由模组在初始化时调用）。
     */
    public static synchronized void setDefaultRenderer(CanvasPixelRenderer renderer) {
        defaultRenderer = renderer;
    }
}