package com.astune.painter.mixin;

import com.astune.painter.api.CanvasDataHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin implements CanvasDataHolder {
}
