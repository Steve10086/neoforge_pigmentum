// network/ItemSyncPacket.java
package com.astune.painter.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ItemSyncPacket(int slot, ItemStack stack) implements CustomPacketPayload {

    public static final Type<ItemSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("painter", "item_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ItemSyncPacket::slot,
                    ItemStack.OPTIONAL_STREAM_CODEC, ItemSyncPacket::stack,
                    (slot, stack) -> new ItemSyncPacket(slot, stack != null ? stack : ItemStack.EMPTY)
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleItemSync(ItemSyncPacket packet, IPayloadContext ctx) {
        if (!ctx.flow().isServerbound()) {
            return;
        }
        ctx.enqueueWork(() -> {
            var player = ctx.player();

            int slot = packet.slot();
            // 安全检查：槽位必须在有效范围内（0 到 物品栏总大小-1）
            if (slot < 0 || slot >= player.getInventory().items.size() + player.getInventory().armor.size() + player.getInventory().offhand.size()) {
                return;
            }

            // 替换对应槽位的物品
            player.getInventory().setItem(slot, packet.stack());

            // 通知客户端该物品已变更
            player.inventoryMenu.broadcastFullState();
        });
    }
}