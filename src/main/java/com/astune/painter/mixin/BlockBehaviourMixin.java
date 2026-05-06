package com.astune.painter.mixin;

import com.astune.painter.CanvasProperties;
import com.astune.painter.block.CanvasBlockEntity;
import com.astune.painter.data.CanvasData;
import com.astune.painter.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.List;

@Mixin(BlockBehaviour.class)
public class BlockBehaviourMixin {

    @Inject(method = "getDrops", at = @At("RETURN"), cancellable = true)
    private void onGetDrops(BlockState state, LootParams.Builder builder, CallbackInfoReturnable<List<ItemStack>> cir) {
        if (state.hasProperty(CanvasProperties.HAVE_CANVAS) && state.getValue(CanvasProperties.HAVE_CANVAS)) {
            // 从参数中获取 BlockEntity
            BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
            if (be instanceof CanvasBlockEntity canvasBE && canvasBE.getCanvasData() != null) {
                ItemStack drop = new ItemStack(state.getBlock().asItem());
                drop.set(com.astune.painter.registry.ModDataComponents.CANVAS_DATA.get(), canvasBE.getCanvasData());
                cir.setReturnValue(Collections.singletonList(drop));
            }
        }
    }
}