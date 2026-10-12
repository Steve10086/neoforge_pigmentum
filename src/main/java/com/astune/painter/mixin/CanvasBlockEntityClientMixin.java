package com.astune.painter.mixin;

import com.astune.painter.api.CanvasData;
import com.astune.painter.api.CanvasDataHolder;
import com.astune.painter.api.CanvasTextureHolder;
import com.astune.painter.block.CanvasBlockEntity;
import com.astune.painter.client.ClientPistonCache;
import com.astune.painter.network.ClientCanvasCache;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@OnlyIn(Dist.CLIENT)
@Mixin(CanvasBlockEntity.class)
public abstract class CanvasBlockEntityClientMixin {
    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void painter$markClientSectionDirty(CompoundTag tag,
                                               net.minecraft.core.HolderLookup.Provider registries,
                                               CallbackInfo ci) {
        CanvasBlockEntity self = (CanvasBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null || !level.isClientSide) {
            return;
        }

        level.getLightEngine().checkBlock(self.getBlockPos());
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.levelRenderer != null) {
            var pos = self.getBlockPos();
            minecraft.levelRenderer.setSectionDirty(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
        }
    }

    @Inject(method = "onLoad", at = @At("TAIL"))
    private void painter$restoreClientPistonData(CallbackInfo ci) {
        CanvasBlockEntity self = (CanvasBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null || !level.isClientSide) {
            return;
        }

        CompoundTag movingData = ClientPistonCache.get(self.getBlockPos());
        if (movingData == null || !movingData.contains("mimicked_state")) {
            return;
        }

        self.loadWithComponents(movingData, level.registryAccess());
        if (self instanceof CanvasDataHolder dataHolder) {
            CanvasData canvasData = dataHolder.painter$getCanvasData();
            if (canvasData != null) {
                if (self instanceof CanvasTextureHolder textureHolder) {
                    textureHolder.painter$regenerateTextures(canvasData);
                }
                ClientCanvasCache.putCanvas(self.getBlockPos(), canvasData);
            }
        }

        if (self.getMimickedState() != null) {
            ClientCanvasCache.putMimickedState(self.getBlockPos(), self.getMimickedState());
        }
    }

    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void painter$removeMimickedStateCache(CallbackInfo ci) {
        CanvasBlockEntity self = (CanvasBlockEntity) (Object) this;
        if (self.getLevel() != null && self.getLevel().isClientSide) {
            ClientCanvasCache.removeMimickedState(self.getBlockPos());
        }
    }
}
