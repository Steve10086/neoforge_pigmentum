package com.astune.painter.client;

import com.astune.painter.Painter;
import com.astune.painter.api.CanvasDataHolder;
import com.astune.painter.block.CanvasBlock;
import com.astune.painter.block.CanvasBlockEntity;
import com.astune.painter.network.ClientCanvasCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.PistonEvent;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Painter.MODID, value = Dist.CLIENT)
public final class ClientPistonEventHandler {
    private ClientPistonEventHandler() {
    }

    @SubscribeEvent
    public static void onPistonPre(PistonEvent.Pre event) {
        if (event.getPistonMoveType() != PistonEvent.PistonMoveType.RETRACT
                || !(event.getLevel() instanceof Level level)
                || !level.isClientSide) {
            return;
        }

        Direction direction = event.getDirection();
        BlockPos pistonFace = event.getFaceOffsetPos();

        for (int distance = 1; distance <= PistonStructureResolver.MAX_PUSH_DEPTH; distance++) {
            BlockPos forwardPos = pistonFace.relative(direction, distance);
            BlockPos returnPos = forwardPos.relative(direction.getOpposite());
            BlockEntity blockEntity = level.getBlockEntity(forwardPos);
            BlockState blockState = level.getBlockState(forwardPos);

            if (blockEntity instanceof CanvasBlockEntity canvasBlockEntity
                    && canvasBlockEntity.getMimickedState() != null) {
                cacheCanvasAt(returnPos, canvasBlockEntity, level);
                aliasPendingCanvas(returnPos, forwardPos, level);
            } else if (blockState.getBlock() instanceof CanvasBlock) {
                // The block state has arrived but its block entity has not loaded
                // yet. The original-position cache is still the authoritative copy.
                aliasPendingCanvas(returnPos, forwardPos, level);
            } else if (blockEntity instanceof PistonMovingBlockEntity movingEntity
                    && movingEntity.isExtending()
                    && movingEntity.getMovedState().getBlock() instanceof CanvasBlock) {
                // Extending render data is keyed by the block's original position.
                // Keep it there for a possible return, and alias it to the forward
                // position used by a retracting renderer or an immediate finalTick.
                aliasPendingCanvas(returnPos, forwardPos, level);
            }
        }
    }

    private static void aliasPendingCanvas(BlockPos from, BlockPos to, Level level) {
        ClientPistonCache.copy(from, to);
        CompoundTag data = ClientPistonCache.get(to);
        if (data != null && data.contains("mimicked_state")) {
            ClientCanvasCache.putMimickedState(to, NbtUtils.readBlockState(
                    level.registryAccess().lookupOrThrow(Registries.BLOCK),
                    data.getCompound("mimicked_state")
            ));
            BlockState state = level.getBlockState(to);
            level.setBlocksDirty(to, state, state);
        }
    }

    private static void cacheCanvasAt(BlockPos pos, CanvasBlockEntity canvasBlockEntity, Level level) {
        CompoundTag data = canvasBlockEntity.saveWithoutMetadata(level.registryAccess());
        ClientPistonCache.store(pos, data);

        if (canvasBlockEntity instanceof CanvasDataHolder holder) {
            ClientPistonCache.storeCanvasTexture(pos, holder.painter$getCachedFaceTextures());
        }
    }
}
