package com.astune.painter.attachment;

import com.astune.painter.Painter;
import com.astune.painter.api.CanvasData;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Optional;
import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Painter.MODID);

    /**
     * 存储画布像素数据，附加到 CanvasBlockEntity 或任意原有 BlockEntity 上。
     * 序列化由 CanvasData.CODEC 提供，确保数据持久化。
     */
    // 画布数据附件
    public static final Supplier<AttachmentType<Optional<CanvasData>>> CANVAS_DATA =
            ATTACHMENT_TYPES.register("canvas_data", () ->
                    AttachmentType.builder(() -> Optional.<CanvasData>empty())
                            .serialize(CanvasData.CODEC
                                    .optionalFieldOf("canvas")
                                    .codec())
                            .build());

}