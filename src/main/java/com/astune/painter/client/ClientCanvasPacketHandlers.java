package com.astune.painter.client;

import com.astune.painter.api.CanvasDataHolder;
import com.astune.painter.api.CanvasTextureHolder;
import com.astune.painter.block.CanvasBlockEntity;
import com.astune.painter.network.CanvasBlockReplacePacket;
import com.astune.painter.network.ClientCanvasCache;
import com.astune.painter.network.SyncCanvasPacket;
import com.astune.painter.util.CanvasBlockSetController;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public final class ClientCanvasPacketHandlers {
    private ClientCanvasPacketHandlers() {
    }

    public static void handleSyncCanvas(SyncCanvasPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var pos = packet.pos();
            BlockState previousCachedState = ClientCanvasCache.getMimickedState(pos);
            boolean mimickedStateChanged = !Objects.equals(previousCachedState, packet.mimickedState().orElse(null));

            boolean clearCanvas = packet.canvasData().isEmpty() && packet.mimickedState().isEmpty();
            if (clearCanvas) {
                ClientCanvasCache.removeCanvas(pos);
            } else {
                ClientCanvasCache.putCanvas(pos, packet.canvasData());
            }
            packet.mimickedState().ifPresentOrElse(
                    state -> ClientCanvasCache.putMimickedState(pos, state),
                    () -> ClientCanvasCache.removeMimickedState(pos)
            );

            Minecraft minecraft = Minecraft.getInstance();
            Level level = minecraft.level;
            if (level != null) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof CanvasBlockEntity canvasBlockEntity) {
                    if (packet.mimickedState().isPresent()
                            && !Objects.equals(canvasBlockEntity.getMimickedState(), packet.mimickedState().get())) {
                        mimickedStateChanged = true;
                        canvasBlockEntity.setMimickedState(packet.mimickedState().get());
                    }
                    if (blockEntity instanceof CanvasDataHolder dataHolder) {
                        dataHolder.painter$setCanvasData(packet.canvasData());
                    }
                    if (blockEntity instanceof CanvasTextureHolder textureHolder) {
                        textureHolder.painter$regenerateTextures(packet.canvasData());
                    }
                }
            }

            if (mimickedStateChanged && minecraft.levelRenderer != null) {
                minecraft.levelRenderer.setSectionDirty(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
            }
        });
    }

    public static void handleBlockReplace(CanvasBlockReplacePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }

            ClientCanvasCache.removeCanvas(packet.pos());
            ClientCanvasCache.removeMimickedState(packet.pos());
            CanvasBlockSetController.replaceBlock(level, packet.pos(), packet.state(), 3);
            level.setBlocksDirty(packet.pos(), packet.state(), packet.state());
            Minecraft.getInstance().levelRenderer.setSectionDirty(
                    packet.pos().getX() >> 4,
                    packet.pos().getY() >> 4,
                    packet.pos().getZ() >> 4
            );
        });
    }
}
