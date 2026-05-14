package com.astune.painter.api;

import com.mojang.serialization.Codec;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record CanvasData(List<CanvasFace> faces) {

    public static final Codec<CanvasData> CODEC =
            Codec.list(CanvasFace.CODEC).xmap(CanvasData::new, CanvasData::faces);

    public static final StreamCodec<RegistryFriendlyByteBuf, CanvasData> STREAM_CODEC =
            StreamCodec.of(
                    (buf, data) -> {
                        buf.writeVarInt(data.faces.size());
                        for (CanvasFace face : data.faces) {
                            CanvasFace.STREAM_CODEC.encode(buf, face);
                        }
                    },
                    buf -> {
                        int size = buf.readVarInt();
                        List<CanvasFace> faces = new ArrayList<>(size);
                        for (int i = 0; i < size; i++) {
                            faces.add(CanvasFace.STREAM_CODEC.decode(buf));
                        }
                        return new CanvasData(faces);
                    }
            );

    public CanvasData(List<CanvasFace> faces) {
        this.faces = new ArrayList<>(faces);
    }

    public static CanvasData empty() {
        return new CanvasData(new ArrayList<>());
    }

    /**
     * 添加或替换一个画布面（如果相同表面已存在则替换）。
     */
    public void setFace(CanvasFace newFace) {
        for (int i = 0; i < faces.size(); i++) {
            if (faces.get(i).isSameSurface(newFace)) {
                faces.set(i, newFace);
                return;
            }
        }
        faces.add(newFace);
    }

    /**
     * 获取指定表面的画布（如果存在）。
     */
    public Optional<CanvasFace> getFace(Direction primaryFace, Vec3 centerOffset) {
        CanvasFace probe = new CanvasFace(primaryFace, centerOffset, new PixelMatrix());
        return faces.stream().filter(f -> f.isSameSurface(probe)).findFirst();
    }

    public boolean isEmpty() {
        return faces.isEmpty();
    }
}