package com.astune.painter.api.imageProvider;

import com.astune.painter.api.CanvasFace;
import com.mojang.blaze3d.platform.NativeImage;
import org.jetbrains.annotations.Nullable;

public interface CanvasImageProvider {
    /**
     * 根据画布面创建 NativeImage。
     * @return 生成的图像，返回 null 表示失败，纹理将不会更新。
     */
    @Nullable
    NativeImage createImage(CanvasFace face);

    default String name(){
        return "DEFAULT";
    }
}