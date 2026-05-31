package com.astune.painter.api.imageProvider;

import com.astune.painter.api.CanvasFace;
import com.mojang.blaze3d.platform.NativeImage;
import org.jetbrains.annotations.Nullable;

public interface CanvasImageProvider {
    /**
     * 根据画布面创建 NativeImage。
     * @return 生成的图像，返回 null 表示失败，该提供者的纹理将被跳过。
     */
    @Nullable
    NativeImage createImage(CanvasFace face);

    /**
     * 返回此图像提供者的名称，用于纹理资源路径标识。
     */
    default String name() {
        return "default";
    }

    /**
     * 判断此提供者是否可以为给定的上下文生成图像。
     * 默认返回 true，允许所有面使用。
     */
    default boolean canProvide(ImageProviderContext context) {
        return true;
    }
}