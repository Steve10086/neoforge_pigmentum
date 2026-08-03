package com.astune.painter.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CanvasRenderTypes {
    private static final Map<ResourceLocation, RenderType> WORLD_TRANSLUCENT = new ConcurrentHashMap<>();

    private CanvasRenderTypes() {}

    public static RenderType worldTranslucent(ResourceLocation texture) {
        return WORLD_TRANSLUCENT.computeIfAbsent(texture, CanvasRenderTypes::createWorldTranslucent);
    }

    public static void release(ResourceLocation texture) {
        WORLD_TRANSLUCENT.remove(texture);
    }

    private static RenderType createWorldTranslucent(ResourceLocation texture) {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                .setCullState(RenderStateShard.NO_CULL)
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setOverlayState(RenderStateShard.OVERLAY)
                .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                .setOutputState(RenderStateShard.TRANSLUCENT_TARGET)
                .createCompositeState(false);

        return RenderType.create(
                "painter_canvas_world_translucent",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                RenderType.TRANSIENT_BUFFER_SIZE,
                true,
                true,
                state
        );
    }
}
