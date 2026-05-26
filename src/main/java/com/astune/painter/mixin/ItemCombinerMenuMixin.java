// mixin/ItemCombinerMenuMixin.java
package com.astune.painter.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;

@Mixin(ItemCombinerMenu.class)
public abstract  class ItemCombinerMenuMixin {

    @Shadow
    @Final
    protected ContainerLevelAccess access;

    private static final Method STILL_VALID;

    static {
        try {
            STILL_VALID = AbstractContainerMenu.class.getDeclaredMethod(
                    "stillValid", ContainerLevelAccess.class, Player.class, Block.class);
            STILL_VALID.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Inject(method = "stillValid(Lnet/minecraft/world/entity/player/Player;)Z", at = @At("HEAD"), cancellable = true)
    private void onStillValid(Player player, CallbackInfoReturnable<Boolean> cir) {
        access.evaluate((level, pos) -> {
            Block targetBlock = level.getBlockState(pos).getBlock();
            // 调用已注入的 AbstractContainerMenu.stillValid
            try {
                boolean result = (Boolean) STILL_VALID.invoke(null, access, player, targetBlock);
                cir.setReturnValue(result);
            } catch (Exception e) {
                cir.setReturnValue(false);
            }
            return true;
        });
    }
}