package com.astune.painter.network;

import com.astune.painter.Config;
import com.astune.painter.Painter;
import com.astune.painter.api.CanvasData;
import com.astune.painter.api.CanvasDataHolder;
import com.astune.painter.api.CanvasFace;
import com.astune.painter.block.CanvasBlock;
import com.astune.painter.block.CanvasBlockEntity;
import com.astune.painter.event.ServerCanvasUpdateEvent;
import com.astune.painter.util.CanvasBlockSetController;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CanvasStrokeHistory {
    private static final LinkedList<StrokeOperation> HISTORY = new LinkedList<>();
    private static final LinkedList<StrokeOperation> REDO = new LinkedList<>();

    private CanvasStrokeHistory() {
    }

    public static BlockSnapshot capture(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        BlockState mimicked = be instanceof CanvasBlockEntity canvasBE ? canvasBE.getMimickedState() : null;
        CanvasData canvasData = be instanceof CanvasDataHolder holder ? copyCanvasData(holder.painter$getCanvasData()) : CanvasData.empty();
        return new BlockSnapshot(state, mimicked, canvasData);
    }

    public static void recordUpload(ServerLevel level, UUID playerId, UUID strokeId, BlockPos pos, BlockSnapshot before, BlockSnapshot after, CanvasAction action) {
        if (getHistoryLimit() <= 0) {
            return;
        }

        REDO.clear();
        StrokeOperation operation = findStroke(playerId, strokeId);
        if (operation == null) {
            operation = new StrokeOperation(playerId, strokeId);
            HISTORY.add(operation);
            trimToLimit(HISTORY, getHistoryLimit());
        }
        operation.changes.add(new BlockChange(level.dimension(), pos.immutable(), after.copy(), before.copy(), action));
    }

    public static boolean undoLastStroke(MinecraftServer server, UUID playerId) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            return false;
        }
        StrokeOperation operation = removeLastUndoOperation(playerId);
        if (operation == null) {
            return false;
        }
        StrokeOperation redoOperation = apply(server, player, operation);
        if (redoOperation == null) {
            HISTORY.add(operation);
            return false;
        }
        if (!redoOperation.changes.isEmpty() && getRedoLimit() > 0) {
            REDO.add(redoOperation);
            trimToLimit(REDO, getRedoLimit());
        }
        return !redoOperation.changes.isEmpty();
    }

    public static boolean redoLastStroke(MinecraftServer server, UUID playerId) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            return false;
        }
        StrokeOperation operation = removeLastForPlayer(REDO, playerId);
        if (operation == null) {
            return false;
        }
        StrokeOperation undoOperation = apply(server, player, operation);
        if (undoOperation == null) {
            REDO.add(operation);
            return false;
        }
        if (!undoOperation.changes.isEmpty() && getHistoryLimit() > 0) {
            HISTORY.add(undoOperation);
            trimToLimit(HISTORY, getHistoryLimit());
        }
        return !undoOperation.changes.isEmpty();
    }

    public static void clear() {
        HISTORY.clear();
        REDO.clear();
    }

    private static StrokeOperation apply(MinecraftServer server, Player player, StrokeOperation operation) {
        for (BlockChange change : operation.changes) {
            ServerLevel level = server.getLevel(change.dimension);
            if (level == null || !matchesExpected(level, change.pos, change.source)) {
                Painter.LOGGER.debug("Canceled stroke rollback at {} due to conflict", change.pos);
                return null;
            }
        }

        StrokeOperation opposite = new StrokeOperation(operation.playerId, UUID.randomUUID());
        for (BlockChange change : operation.changes) {
            ServerLevel level = server.getLevel(change.dimension);
            if (level == null) {
                return null;
            }
            BlockSnapshot beforeApply = capture(level, change.pos);
            boolean restoresOriginalBlock = restoresOriginalBlock(level, change.pos, change.target);
            if (!restoresOriginalBlock) {
                Painter.LOGGER.debug("ServerCanvasUpdateEvent.Pre: pos={}, action={}, player={}", change.pos, change.action, player.getName().getString());
                CanvasData eventData = copyCanvasData(change.target.canvasData());
                NeoForge.EVENT_BUS.post(new ServerCanvasUpdateEvent.Pre(change.pos, eventData, change.action, player));
            }

            if (applySnapshot(level, change.pos, change.target)) {
                BlockSnapshot afterApply = capture(level, change.pos);
                if (!restoresOriginalBlock) {
                    Painter.LOGGER.debug("ServerCanvasUpdateEvent: pos={}, action={}, player={}", change.pos, change.action, player.getName().getString());
                    NeoForge.EVENT_BUS.post(new ServerCanvasUpdateEvent(change.pos, copyCanvasData(change.target.canvasData()), change.action, player));
                    syncToClients(level, change.pos, change.target);
                } else {
                    clearClientCanvasCache(level, change.pos);
                }
                opposite.changes.add(new BlockChange(change.dimension, change.pos, afterApply.copy(), beforeApply.copy(), change.action));
            }
        }
        return opposite;
    }

    private static boolean applySnapshot(ServerLevel level, BlockPos pos, BlockSnapshot target) {
        boolean restoresOriginalBlock = level.getBlockState(pos).getBlock() instanceof CanvasBlock
                && !(target.blockState().getBlock() instanceof CanvasBlock);

        if (!level.getBlockState(pos).equals(target.blockState())) {
            boolean changed = restoresOriginalBlock
                    ? CanvasBlockSetController.replaceBlock(level, pos, target.blockState(), 3)
                    : level.setBlock(pos, target.blockState(), 3);
            if (!changed) {
                return false;
            }
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (target.blockState().getBlock() instanceof CanvasBlock && be instanceof CanvasBlockEntity canvasBE) {
            if (target.mimickedState() != null) {
                canvasBE.setMimickedState(target.mimickedState());
            }
            if (be instanceof CanvasDataHolder holder) {
                CanvasData canvasData = copyCanvasData(target.canvasData());
                holder.painter$setCanvasData(canvasData);
                holder.painter$regenerateTextures(canvasData);
                be.setChanged();
            }
            canvasBE.checkAndRestoreIfNeeded();
        }
        level.setBlocksDirty(pos, target.blockState(), target.blockState());
        return true;
    }

    private static boolean matchesExpected(ServerLevel level, BlockPos pos, BlockSnapshot expected) {
        BlockState currentState = level.getBlockState(pos);
        if (restoresOriginalBlock(level, pos, expected)) {
            return true;
        }
        if (!currentState.is(expected.blockState().getBlock())) {
            return false;
        }
        if (!(currentState.getBlock() instanceof CanvasBlock)) {
            return true;
        }
        BlockEntity be = level.getBlockEntity(pos);
        BlockState currentMimicked = be instanceof CanvasBlockEntity canvasBE ? canvasBE.getMimickedState() : null;
        return statesEqual(currentMimicked, expected.mimickedState());
    }

    private static boolean restoresOriginalBlock(ServerLevel level, BlockPos pos, BlockSnapshot target) {
        BlockState currentState = level.getBlockState(pos);
        if (!(currentState.getBlock() instanceof CanvasBlock) || target.blockState().getBlock() instanceof CanvasBlock) {
            return false;
        }
        BlockEntity be = level.getBlockEntity(pos);
        BlockState currentMimicked = be instanceof CanvasBlockEntity canvasBE ? canvasBE.getMimickedState() : null;
        return statesEqual(currentMimicked, target.blockState());
    }

    private static void syncToClients(ServerLevel level, BlockPos pos, BlockSnapshot target) {
        BlockState mimicked = target.blockState().getBlock() instanceof CanvasBlock ? target.mimickedState() : null;
        PacketDistributor.sendToPlayersTrackingChunk(
                level, new ChunkPos(pos),
                new SyncCanvasPacket(pos, target.canvasData(), java.util.Optional.ofNullable(mimicked), false)
        );
        level.setBlocksDirty(pos, level.getBlockState(pos), level.getBlockState(pos));
    }

    private static void clearClientCanvasCache(ServerLevel level, BlockPos pos) {
        PacketDistributor.sendToPlayersTrackingChunk(
                level, new ChunkPos(pos),
                new SyncCanvasPacket(pos, CanvasData.empty(), java.util.Optional.empty(), false)
        );
        PacketDistributor.sendToPlayersTrackingChunk(
                level, new ChunkPos(pos),
                new CanvasBlockReplacePacket(pos, level.getBlockState(pos))
        );
        level.setBlocksDirty(pos, level.getBlockState(pos), level.getBlockState(pos));
    }

    @Nullable
    private static StrokeOperation removeLastForPlayer(LinkedList<StrokeOperation> list, UUID playerId) {
        Iterator<StrokeOperation> iterator = list.descendingIterator();
        while (iterator.hasNext()) {
            StrokeOperation operation = iterator.next();
            if (operation.playerId.equals(playerId)) {
                iterator.remove();
                return operation;
            }
        }
        return null;
    }

    @Nullable
    private static StrokeOperation removeLastUndoOperation(UUID playerId) {
        for (int i = HISTORY.size() - 1; i >= 0; i--) {
            StrokeOperation operation = HISTORY.get(i);
            if (!operation.playerId.equals(playerId)) {
                continue;
            }
            if (hasLaterDifferentPlayerOverlap(i, operation)) {
                Painter.LOGGER.debug("Canceled stroke rollback for player {} due to later overlapping stroke", playerId);
                return null;
            }
            return HISTORY.remove(i);
        }
        return null;
    }

    private static boolean hasLaterDifferentPlayerOverlap(int operationIndex, StrokeOperation operation) {
        for (int i = operationIndex + 1; i < HISTORY.size(); i++) {
            StrokeOperation later = HISTORY.get(i);
            if (later.playerId.equals(operation.playerId)) {
                continue;
            }
            for (BlockChange change : operation.changes) {
                if (later.touches(change.dimension, change.pos)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    private static StrokeOperation findStroke(UUID playerId, UUID strokeId) {
        for (StrokeOperation operation : HISTORY) {
            if (operation.playerId.equals(playerId) && operation.strokeId.equals(strokeId)) {
                return operation;
            }
        }
        return null;
    }

    private static void trimToLimit(LinkedList<StrokeOperation> list, int limit) {
        while (list.size() > limit) {
            list.removeFirst();
        }
    }

    private static int getHistoryLimit() {
        return Config.STROKE_UNDO_HISTORY_LIMIT.get();
    }

    private static int getRedoLimit() {
        return Config.STROKE_REDO_HISTORY_LIMIT.get();
    }

    private static CanvasData copyCanvasData(@Nullable CanvasData data) {
        if (data == null) {
            return CanvasData.empty();
        }
        List<CanvasFace> faces = new ArrayList<>(data.faces().size());
        for (CanvasFace face : data.faces()) {
            faces.add(copyFace(face));
        }
        return new CanvasData(faces);
    }

    private static CanvasFace copyFace(CanvasFace face) {
        return new CanvasFace(
                face.primaryFace(),
                face.corner0(),
                face.corner1(),
                face.corner2(),
                face.corner3(),
                face.pixels().copy(),
                copyEffectLayers(face.getEffectLayers())
        );
    }

    private static Map<String, byte[]> copyEffectLayers(Map<String, byte[]> effectLayers) {
        java.util.HashMap<String, byte[]> copy = new java.util.HashMap<>();
        for (Map.Entry<String, byte[]> entry : effectLayers.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().clone());
        }
        return copy;
    }

    private static boolean statesEqual(@Nullable BlockState a, @Nullable BlockState b) {
        return a == null ? b == null : a.equals(b);
    }

    public record BlockSnapshot(BlockState blockState, @Nullable BlockState mimickedState, CanvasData canvasData) {
        public BlockSnapshot {
            canvasData = copyCanvasData(canvasData);
        }

        private BlockSnapshot copy() {
            return new BlockSnapshot(blockState, mimickedState, canvasData);
        }
    }

    private static final class StrokeOperation {
        private final UUID playerId;
        private final UUID strokeId;
        private final List<BlockChange> changes = new ArrayList<>();

        private StrokeOperation(UUID playerId, UUID strokeId) {
            this.playerId = playerId;
            this.strokeId = strokeId;
        }

        private boolean touches(ResourceKey<Level> dimension, BlockPos pos) {
            for (BlockChange change : changes) {
                if (change.dimension.equals(dimension) && change.pos.equals(pos)) {
                    return true;
                }
            }
            return false;
        }
    }

    private record BlockChange(ResourceKey<Level> dimension, BlockPos pos, BlockSnapshot source, BlockSnapshot target, CanvasAction action) {
    }
}
