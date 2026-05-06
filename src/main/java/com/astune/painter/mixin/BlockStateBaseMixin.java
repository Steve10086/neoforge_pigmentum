package com.astune.painter.mixin;

import com.astune.painter.CanvasProperties;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {

    private boolean has_canvas = false;

    @Inject(method = "hasBlockEntity", at = @At("HEAD"), cancellable = true)
    private void checkCanvasBlockEntity(CallbackInfoReturnable<Boolean> cir) {
        BlockStateBaseMixin state = this;
        if (state.isHas_canvas()) {
            System.out.println("have entity");
            cir.setReturnValue(true);
        }
    }

    public boolean isHas_canvas() {
        return has_canvas;
    }
}