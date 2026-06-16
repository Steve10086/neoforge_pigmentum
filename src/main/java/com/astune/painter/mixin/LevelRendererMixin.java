package com.astune.painter.mixin;

import com.astune.painter.client.CanvasRenderEventHandler;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Inject(method = "setSectionDirty(III)V", at = @At("HEAD"))
    private void onSetSectionDirty(int sectionX, int sectionY, int sectionZ, CallbackInfo ci) {
        int baseX = sectionX << 4;
        int baseY = sectionY << 4;
        int baseZ = sectionZ << 4;
        CanvasRenderEventHandler.SEEN_POSITIONS.removeIf(pos ->
            pos.getX() >= baseX && pos.getX() < baseX + 16 &&
            pos.getY() >= baseY && pos.getY() < baseY + 16 &&
            pos.getZ() >= baseZ && pos.getZ() < baseZ + 16
        );
    }
}
