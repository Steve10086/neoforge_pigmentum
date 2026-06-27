package com.astune.painter.api.blend;

public class DefaultBlendFunctions {
    public static final BlendFunction OVERWRITE = ctx -> {
        boolean result = ctx.face.pixels().setPixel(ctx.px, ctx.py, ctx.newColor);
        if (!result) return false;
        for (var key : ctx.face.getEffectLayers().keySet()) {
            ctx.setEffect(key, 0);
        }
        for (var entry : ctx.effectValues.entrySet()) {
            ctx.setEffect(entry.getKey(), entry.getValue());
        }
        return true;
    };

    public static final BlendFunction ADD = ctx -> {
        int eA = (ctx.existingColor >> 24) & 0xFF;
        int eR = (ctx.existingColor >> 16) & 0xFF;
        int eG = (ctx.existingColor >> 8) & 0xFF;
        int eB = ctx.existingColor & 0xFF;
        int nA = (ctx.newColor >> 24) & 0xFF;
        int nR = (ctx.newColor >> 16) & 0xFF;
        int nG = (ctx.newColor >> 8) & 0xFF;
        int nB = ctx.newColor & 0xFF;

        float srcA = nA / 255f;
        float dstA = eA / 255f;
        float outA = srcA + dstA * (1 - srcA);
        if (outA < 0.001f) {
            return ctx.face.pixels().setPixel(ctx.px, ctx.py, 0);
        }
        int outR = (int)((nR * srcA + eR * dstA * (1 - srcA)) / outA);
        int outG = (int)((nG * srcA + eG * dstA * (1 - srcA)) / outA);
        int outB = (int)((nB * srcA + eB * dstA * (1 - srcA)) / outA);
        int outAInt = Math.min(255, (int)(outA * 255));
        int finalColor = (outAInt << 24) | (outR << 16) | (outG << 8) | outB;
        boolean result = ctx.face.pixels().setPixel(ctx.px, ctx.py, finalColor);
        if (!result) return false;

        for (var entry : ctx.effectValues.entrySet()) {
            String key = entry.getKey();
            int newVal = entry.getValue();
            int existingVal = ctx.getEffect(key);
            ctx.setEffect(key, Math.max(existingVal, newVal));
        }
        return true;
    };

    public static final BlendFunction MULTIPLY = ctx -> {
        float srcA = ((ctx.newColor >> 24) & 0xFF) / 255f;
        int dstR = (ctx.existingColor >> 16) & 0xFF;
        int dstG = (ctx.existingColor >> 8) & 0xFF;
        int dstB = ctx.existingColor & 0xFF;
        int srcR = (ctx.newColor >> 16) & 0xFF;
        int srcG = (ctx.newColor >> 8) & 0xFF;
        int srcB = ctx.newColor & 0xFF;
        int mulR = (srcR * dstR) / 255;
        int mulG = (srcG * dstG) / 255;
        int mulB = (srcB * dstB) / 255;
        int outR = (int)(dstR + (mulR - dstR) * srcA);
        int outG = (int)(dstG + (mulG - dstG) * srcA);
        int outB = (int)(dstB + (mulB - dstB) * srcA);
        int outA = (ctx.existingColor >>> 24);
        int finalColor = (outA << 24) | (outR << 16) | (outG << 8) | outB;
        boolean result = ctx.face.pixels().setPixel(ctx.px, ctx.py, finalColor);
        if (!result) return false;

        for (var entry : ctx.effectValues.entrySet()) {
            String key = entry.getKey();
            int newVal = entry.getValue();
            int existingVal = ctx.getEffect(key);
            ctx.setEffect(key, (existingVal * newVal) / 255);
        }
        return true;
    };

    public static final BlendFunction ERASE = ctx -> {
        boolean result = ctx.face.pixels().setPixel(ctx.px, ctx.py, 0);
        if (!result) return false;
        // 清空所有效果值
        for (var entry : ctx.effectValues.entrySet()) {
            ctx.setEffect(entry.getKey(), 0);
        }
        return true;
    };
}