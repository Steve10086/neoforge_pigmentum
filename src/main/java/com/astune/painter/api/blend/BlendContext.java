package com.astune.painter.api.blend;

import com.astune.painter.api.BlendMode;
import com.astune.painter.api.CanvasFace;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.Map;

public class BlendContext {
    public final CanvasFace face;
    public final int px, py;
    public final int existingColor;
    public final int newColor;
    public final BlendMode mode;           // 可空
    public final ItemStack brushStack;
    public final Map<String, Integer> effectValues;  // 画笔提供的效果值

    public BlendContext(CanvasFace face, int px, int py, int existingColor, int newColor,
                        BlendMode mode, ItemStack brushStack, Map<String, Integer> effectValues) {
        this.face = face;
        this.px = px;
        this.py = py;
        this.existingColor = existingColor;
        this.newColor = newColor;
        this.mode = mode;
        this.brushStack = brushStack;
        this.effectValues = effectValues != null ? effectValues : Collections.emptyMap();
    }

    // 便捷写入效果值
    public void setEffect(String key, int value) {
        face.setEffectValue(key, px, py, value);
    }

    // 便捷读取效果值
    public int getEffect(String key) {
        return face.getEffectValue(key, px, py);
    }
}
