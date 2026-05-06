package com.astune.painter.mixin;

import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockBehaviour.Properties.class)
public abstract class Block_PropertiesMixin {

    @Unique
    private boolean has_canvas = false;


    public boolean isHas_canvas() {
        return has_canvas;
    }
}
