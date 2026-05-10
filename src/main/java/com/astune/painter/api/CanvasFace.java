package com.astune.painter.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

public record CanvasFace(Direction primaryFace, Vec3 centerOffset, PixelMatrix pixels) {

    public static final Codec<CanvasFace> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Direction.CODEC.fieldOf("face").forGetter(CanvasFace::primaryFace),
                    Vec3.CODEC.fieldOf("offset").forGetter(CanvasFace::centerOffset),
                    PixelMatrix.CODEC.fieldOf("pixels").forGetter(CanvasFace::pixels)
            ).apply(instance, CanvasFace::new)
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC3_STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.DOUBLE, v -> v.x,
                    ByteBufCodecs.DOUBLE, v -> v.y,
                    ByteBufCodecs.DOUBLE, v -> v.z,
                    Vec3::new
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, CanvasFace> STREAM_CODEC =
            StreamCodec.composite(
                    Direction.STREAM_CODEC,
                    CanvasFace::primaryFace,
                    VEC3_STREAM_CODEC,
                    CanvasFace::centerOffset,
                    PixelMatrix.STREAM_CODEC,
                    CanvasFace::pixels,
                    CanvasFace::new
            );

    /**
     * 创建一个该面上空白的新画布。
     */
    public static CanvasFace createEmpty(Direction face, Vec3 offset) {
        return new CanvasFace(face, offset, new PixelMatrix());
    }

    /**
     * 判断两个画布是否属于方块的同一个表面（相同朝向且中心几乎重合）。
     */
    public boolean isSameSurface(CanvasFace other) {
        return this.primaryFace == other.primaryFace &&
                this.centerOffset.distanceToSqr(other.centerOffset) < 0.0001;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CanvasFace that)) return false;
        return primaryFace == that.primaryFace &&
                centerOffset.distanceToSqr(that.centerOffset) < 0.0001 &&
                pixels.equals(that.pixels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(primaryFace, centerOffset.x, centerOffset.y, centerOffset.z);
    }

    public int getPixelWidth() { return pixels.getWidth(); }
    public int getPixelHeight() { return pixels.getHeight(); }
}