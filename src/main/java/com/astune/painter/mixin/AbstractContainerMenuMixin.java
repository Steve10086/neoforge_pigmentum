// mixin/AbstractContainerMenuMixin.java
package com.astune.painter.mixin;

import com.astune.painter.block.CanvasBlock;
import com.astune.painter.block.CanvasBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerMenu.class)
public class AbstractContainerMenuMixin {

    @Inject(method = "stillValid(Lnet/minecraft/world/inventory/ContainerLevelAccess;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/block/Block;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void onStillValid(ContainerLevelAccess access, Player player, Block targetBlock,
                                     CallbackInfoReturnable<Boolean> cir) {
        // 获取 access 中的位置和世界
        // ContainerLevelAccess 内部有两个重载，这里我们利用 evaluate 来获取信息
        System.out.println("check menu");
        access.evaluate((level, pos) -> {
            BlockState state = level.getBlockState(pos);
            // 核心逻辑：如果方块是画布，检查内部的 mimickedState 是否与 targetBlock 匹配
            if (state.getBlock() instanceof CanvasBlock) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof CanvasBlockEntity canvasBE) {
                    BlockState mimicked = canvasBE.getMimickedState();
                    if (mimicked != null && mimicked.is(targetBlock)) {
                        System.out.println("menu valid");
                        // 匹配成功
                        cir.setReturnValue(player.canInteractWithBlock(pos, 4.0));
                    }
                }
            }
            return false; // 如果不是画布或内部不匹配，按原逻辑走
        });
    }
}