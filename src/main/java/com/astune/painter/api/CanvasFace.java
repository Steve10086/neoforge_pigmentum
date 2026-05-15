package com.astune.painter.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;


public class CanvasFace {
    // 保留方向用于缓存键和快速查询
    private final Direction primaryFace;
    // 四个角点相对于方块原点的坐标
    private final Vec3 corner0; // 左下
    private final Vec3 corner1; // 右下
    private final Vec3 corner2; // 右上
    private final Vec3 corner3; // 左上
    // 像素数据
    private final PixelMatrix pixels;

    // Codec 和 StreamCodec 需要序列化 Vec3
    public static final Codec<CanvasFace> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Direction.CODEC.fieldOf("primaryFace").forGetter(CanvasFace::primaryFace),
                    Vec3.CODEC.fieldOf("c0").forGetter(f -> f.corner0),
                    Vec3.CODEC.fieldOf("c1").forGetter(f -> f.corner1),
                    Vec3.CODEC.fieldOf("c2").forGetter(f -> f.corner2),
                    Vec3.CODEC.fieldOf("c3").forGetter(f -> f.corner3),
                    PixelMatrix.CODEC.fieldOf("pixels").forGetter(CanvasFace::pixels)
            ).apply(instance, CanvasFace::new));

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

    // 完整构造器
    public CanvasFace(Direction primaryFace, Vec3 c0, Vec3 c1, Vec3 c2, Vec3 c3, PixelMatrix pixels) {
        this.primaryFace = primaryFace;
        this.corner0 = c0;
        this.corner1 = c1;
        this.corner2 = c2;
        this.corner3 = c3;
        this.pixels = pixels;
    }

    // 兼容旧构造器：从中心偏移 + 尺寸生成四个角点
    public CanvasFace(Direction primaryFace, Vec3 centerOffset, PixelMatrix pixels) {
        this.primaryFace = primaryFace;
        this.pixels = pixels;
        float w = pixels.getWidth() / 16f;
        float h = pixels.getHeight() / 16f;

        Vec3[] corners = buildCornersFromOffset(primaryFace, centerOffset, w, h);
        this.corner0 = corners[0];
        this.corner1 = corners[1];
        this.corner2 = corners[2];
        this.corner3 = corners[3];
    }

    /**
     * 判断两个 CanvasFace 是否在同一个表面上（方向和四点坐标完全一致）
     */
    public boolean isSameSurface(CanvasFace other) {
        if (other == null) return false;
        if (this.primaryFace != other.primaryFace) return false;

        // 比较四个角点，使用足够小的容差
        double epsilon = 0.001;
        return cornersEqual(this.corner0, other.corner0, epsilon) &&
                cornersEqual(this.corner1, other.corner1, epsilon) &&
                cornersEqual(this.corner2, other.corner2, epsilon) &&
                cornersEqual(this.corner3, other.corner3, epsilon);
    }

    private static boolean cornersEqual(Vec3 a, Vec3 b, double epsilon) {
        return Math.abs(a.x - b.x) < epsilon &&
                Math.abs(a.y - b.y) < epsilon &&
                Math.abs(a.z - b.z) < epsilon;
    }

    private static final double OFFSET = 0.001;

    /**
     * 返回沿法线方向偏移后的四个角点（相对于方块中心）
     */
    public Vec3[] cornerWithOffset() {
        Vec3 normal = Vec3.atLowerCornerOf(primaryFace.getNormal()).scale(OFFSET);
        return new Vec3[]{
                corner0.add(normal),
                corner1.add(normal),
                corner2.add(normal),
                corner3.add(normal)
        };
    }

    // Getters
    public Direction primaryFace() { return primaryFace; }
    public Vec3 corner0() { return corner0; }
    public Vec3 corner1() { return corner1; }
    public Vec3 corner2() { return corner2; }
    public Vec3 corner3() { return corner3; }
    public PixelMatrix pixels() { return pixels; }

    // 获取中心偏移（用于向后兼容）
    public Vec3 centerOffset() {
        return corner0.add(corner1).add(corner2).add(corner3).scale(0.25);
    }

    /**
     * 生成四个角点相对于方块原点的坐标。
     * 注意：顶点顺序为从正面看的左下→右下→右上→左上。
     */
    private static Vec3[] buildCornersFromOffset(Direction dir, Vec3 centerOffset, float w, float h) {
        Vec3 right, up;
        switch (dir) {
            case NORTH: right = new Vec3(-1,0,0); up = new Vec3(0,1,0); break;
            case SOUTH: right = new Vec3(1,0,0);  up = new Vec3(0,1,0); break;
            case EAST:  right = new Vec3(0,0,1);  up = new Vec3(0,1,0); break;
            case WEST:  right = new Vec3(0,0,-1); up = new Vec3(0,1,0); break;
            case UP:    right = new Vec3(1,0,0);  up = new Vec3(0,0,1); break;
            case DOWN:  right = new Vec3(1,0,0);  up = new Vec3(0,0,-1); break;
            default: throw new IllegalArgumentException();
        }
        float hw = w / 2f, hh = h / 2f;
        Vec3 center = centerOffset;
        return new Vec3[]{
                center.add(right.scale(-hw)).add(up.scale(-hh)), // 左下
                center.add(right.scale( hw)).add(up.scale(-hh)), // 右下
                center.add(right.scale( hw)).add(up.scale( hh)), // 右上
                center.add(right.scale(-hw)).add(up.scale( hh))  // 左上
        };
    }
}