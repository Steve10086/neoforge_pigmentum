package com.astune.painter.mixin;

import com.astune.painter.block.CanvasBlock;
import com.astune.painter.block.CanvasBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// mixin/LevelMixin.java
@Mixin(Level.class)
public abstract class LevelMixin {

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"), cancellable = true)
    private void onSetBlock(BlockPos pos, BlockState newState, int flags, int recursionLeft,
                            CallbackInfoReturnable<Boolean> cir) {
        Level self = (Level) (Object) this;
        BlockState oldState = self.getBlockState(pos);

        if (!(oldState.getBlock() instanceof CanvasBlock)) return;

        BlockEntity be = self.getBlockEntity(pos);
        if (!(be instanceof CanvasBlockEntity canvasBE)) return;

        BlockState mimicked = canvasBE.getMimickedState();
        if (mimicked == null) return;

        // ⚡ 关键判断：新状态是否和当前 mimickedState 同一种方块
        if (newState.is(mimicked.getBlock())) {
            // 内部状态切换 → 更新 mimickedState，不改变画布方块本身
            canvasBE.setMimickedState(newState);
            System.out.println("setBlock being blocked");
            if (!self.isClientSide) {
                ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(canvasBE);
                if (packet != null) {
                    ((ServerLevel) self).getChunkSource().chunkMap
                            .getPlayers(new ChunkPos(pos), false)
                            .forEach(p -> p.connection.send(packet));
                }
            }
            self.sendBlockUpdated(pos, oldState, oldState, Block.UPDATE_ALL_IMMEDIATE);
            cir.setReturnValue(true); // 拦截
        }
    }

}
