package com.astune.painter.mixin;

import com.astune.painter.api.CanvasTextureHolder;
import com.astune.painter.client.ClientPistonCache;
import com.astune.painter.util.CanvasBlockSetController;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@OnlyIn(Dist.CLIENT)
@Mixin(CanvasBlockSetController.class)
public abstract class CanvasBlockSetControllerClientMixin {
    @Inject(method = "cacheClientPistonData", at = @At("HEAD"))
    private static void painter$cacheClientPistonData(Level level, BlockPos pos, int flags,
                                                      BlockEntity blockEntity, CallbackInfo ci) {
        if (!level.isClientSide || (flags != 68 && flags != 82)) {
            return;
        }

        CompoundTag data = blockEntity.saveWithoutMetadata(level.registryAccess());
        ClientPistonCache.store(pos, data);
        if (blockEntity instanceof CanvasTextureHolder textureHolder) {
            var textures = textureHolder.painter$getCachedFaceTextures();
            if (textures != null) {
                ClientPistonCache.storeCanvasTexture(pos, textures);
            }
        }
    }
}
