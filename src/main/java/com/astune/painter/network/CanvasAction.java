package com.astune.painter.network;

import com.astune.painter.api.CanvasData;
import com.mojang.datafixers.types.Type;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.function.IntFunction;

public enum CanvasAction implements StringRepresentable {
    ADD_CREATION("create"),
    ADD("add"),         // 添加像素
    ERASE("erase");     // 擦除像素

    private final String name;

    CanvasAction(String name) { this.name = name; }

    @Override
    public String getSerializedName() { return name; }

    public static final IntFunction<CanvasAction> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);


    static final StreamCodec<RegistryFriendlyByteBuf, CanvasAction> ACTION_CODEC =
            StreamCodec.of(
                    (buf, action) -> buf.writeVarInt(action.ordinal()),
                    buf -> CanvasAction.BY_ID.apply(buf.readVarInt())
            );

}
