// registry/ModAttachments.java
package com.astune.painter.registry;

import com.astune.painter.api.CanvasData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "painter");

    public static final Supplier<AttachmentType<CanvasData>> CANVAS_DATA =
            ATTACHMENTS.register("canvas_data", () ->
                    AttachmentType.builder(CanvasData::empty) // 默认值
                            .serialize(CanvasData.CODEC)           // 自动序列化
                            .sync(CanvasData.STREAM_CODEC)
                            .build()
            );
}