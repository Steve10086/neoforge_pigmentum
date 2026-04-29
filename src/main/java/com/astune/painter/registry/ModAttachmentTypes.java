package com.astune.painter.registry;

import com.astune.painter.capability.BlockPaintingsSerializer;
import com.astune.painter.data.PaintLayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ModAttachmentTypes {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "painter");

    public static final Supplier<AttachmentType<Map<BlockPos, Map<Direction, PaintLayerData>>>> BLOCK_PAINTINGS =
            ATTACHMENT_TYPES.register("block_paintings", () ->
                    AttachmentType.<Map<BlockPos, Map<Direction, PaintLayerData>>>builder(() -> new HashMap<>())
                            .serialize(new BlockPaintingsSerializer())
                            .build()
            );
}
