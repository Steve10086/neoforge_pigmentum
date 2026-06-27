package com.astune.painter.util;

import com.astune.painter.api.CanvasDataHolder;
import com.astune.painter.api.CanvasFace;
import com.astune.painter.api.ResourcesBundle;
import com.astune.painter.block.CanvasBlock;
import com.astune.painter.block.CanvasBlockEntity;
import com.astune.painter.client.ClientPistonCache;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Deque;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;

public final class CanvasBlockSetController {
    private static final ThreadLocal<Deque<ReplaceContext>> REPLACE_CONTEXTS = new ThreadLocal<>();

    private CanvasBlockSetController() {
    }

    public static boolean replaceBlock(Level level, BlockPos pos, BlockState target, int flags) {
        BlockPos immutablePos = pos.immutable();
        ReplaceContext context = new ReplaceContext(level, immutablePos, target);
        Deque<ReplaceContext> contexts = REPLACE_CONTEXTS.get();
        if (contexts == null) {
            contexts = new ArrayDeque<>();
            REPLACE_CONTEXTS.set(contexts);
        }
        contexts.push(context);
        try {
            if (level.getBlockState(pos).getBlock() instanceof CanvasBlock
                    && !(target.getBlock() instanceof CanvasBlock)) {
                level.removeBlockEntity(pos);
            }
            return level.setBlock(pos, target, flags);
        } finally {
            contexts.remove(context);
            if (contexts.isEmpty()) {
                REPLACE_CONTEXTS.remove();
            }
        }
    }

    public static boolean shouldAllowVanillaReplace(Level level, BlockPos pos, BlockState target) {
        Deque<ReplaceContext> contexts = REPLACE_CONTEXTS.get();
        if (contexts == null) {
            return false;
        }
        for (ReplaceContext context : contexts) {
            if (!context.consumed
                    && context.level == level
                    && context.pos.equals(pos)
                    && context.target.equals(target)) {
                context.consumed = true;
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static Boolean tryProxyMimickedSetBlock(Level level, BlockPos pos, BlockState oldState,
                                                   BlockState newState, int flags, int recursionLeft) {
        preserveScheduledTick(level, pos, oldState, newState);

        if (!(oldState.getBlock() instanceof CanvasBlock)) {
            return null;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CanvasBlockEntity canvasBE)) {
            return null;
        }

        BlockState mimicked = canvasBE.getMimickedState();
        if (mimicked == null) {
            return null;
        }

        cacheClientPistonData(level, pos, flags, be);

        if (!newState.is(mimicked.getBlock())) {
            return null;
        }

        if (newState.equals(mimicked)) {
            return false;
        }

        canvasBE.setMimickedState(newState);

        if (!level.isClientSide) {
            syncMimickedState((ServerLevel) level, pos, canvasBE);
            updateProxyNeighbors(level, pos, oldState, newState, flags, recursionLeft);
        }

        level.sendBlockUpdated(pos, oldState, oldState, flags);
        return true;
    }

    private static void preserveScheduledTick(Level level, BlockPos pos, BlockState oldState, BlockState newState) {
        if (!level.isClientSide && newState.getBlock() instanceof CanvasBlock
                && level.getBlockTicks().hasScheduledTick(pos, oldState.getBlock())) {
            level.scheduleTick(pos, newState.getBlock(), 1);
        }
    }

    private static void cacheClientPistonData(Level level, BlockPos pos, int flags, BlockEntity be) {
        if (level.isClientSide && (flags == 68 || flags == 82)) {
            CompoundTag data = be.saveWithoutMetadata(level.registryAccess());
            ClientPistonCache.store(pos, data);
            List<Pair<CanvasFace, ResourcesBundle>> texture = ((CanvasDataHolder) be).painter$getCachedFaceTextures();
            if (texture != null) {
                ClientPistonCache.storeCanvasTexture(pos, texture);
            }
        }
    }

    private static void syncMimickedState(ServerLevel level, BlockPos pos, CanvasBlockEntity canvasBE) {
        ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(canvasBE);
        if (packet != null) {
            level.getChunkSource().chunkMap
                    .getPlayers(new ChunkPos(pos), false)
                    .forEach(player -> player.connection.send(packet));
        }
    }

    private static void updateProxyNeighbors(Level level, BlockPos pos, BlockState oldState,
                                             BlockState newState, int flags, int recursionLeft) {
        int updateFlags = flags & -34;
        if ((flags & 16) == 0 && recursionLeft > 0) {
            oldState.updateIndirectNeighbourShapes(level, pos, updateFlags, recursionLeft - 1);
            newState.updateNeighbourShapes(level, pos, updateFlags, recursionLeft - 1);
            newState.updateIndirectNeighbourShapes(level, pos, updateFlags, recursionLeft - 1);
        }
        if ((flags & 1) != 0) {
            level.blockUpdated(pos, oldState.getBlock());
            if (newState.hasAnalogOutputSignal()) {
                level.updateNeighbourForOutputSignal(pos, newState.getBlock());
            }
        }
    }

    private static final class ReplaceContext {
        private final Level level;
        private final BlockPos pos;
        private final BlockState target;
        private boolean consumed;

        private ReplaceContext(Level level, BlockPos pos, BlockState target) {
            this.level = Objects.requireNonNull(level);
            this.pos = Objects.requireNonNull(pos);
            this.target = Objects.requireNonNull(target);
        }
    }
}
