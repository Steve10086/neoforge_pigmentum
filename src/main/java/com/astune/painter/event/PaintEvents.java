package com.astune.painter.event;

import com.astune.painter.api.CanvasFace;
import com.astune.painter.api.PixelMatrix;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class PaintEvents {

    @Nullable
    private static CanvasFace calculateCanvasFaceFromBakedModel(Level level, BlockPos pos, BlockState state,
                                                  Vec3 hitLoc, Direction face, Player player) {
        // 1. 获取客户端资源 (在服务端直接获取可能受限，但 Minecraft 类在集成环境服务端也可用)
        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();

        // 2. 获取原方块的 BakedModel
        BakedModel model = dispatcher.getBlockModel(state);
        System.out.println("hitting " + hitLoc + ", getting quads...");

        // 3. 提取模型的所有四边形
        List<BakedQuad> allQuads = new ArrayList<>();

        for (Direction direction : Direction.values()) {
            List<BakedQuad> quads = model.getQuads(state, direction, net.minecraft.util.RandomSource.create());
            allQuads.addAll(quads);
        }
        List<BakedQuad> quads = model.getQuads(state, null, net.minecraft.util.RandomSource.create());
        allQuads.addAll(quads);
        for (BakedQuad quad : allQuads) {
            System.out.println("Quad direction: " + quad.getDirection() + ", vertexData length: " + quad.getVertices().length);
        }
        if (allQuads.isEmpty()) return null;

        System.out.println("searching target face in " + allQuads);
        // 构建局部坐标的射线
        Vec3 rayOrigin = player.getEyePosition().subtract(pos.getX(), pos.getY(), pos.getZ());
        Vec3 rayDir = player.getLookAngle();

        BakedQuad bestQuad = null;
        Vec3 bestIntersection = null;
        double bestDistance = Double.MAX_VALUE;

        for (BakedQuad quad : allQuads) {
            Vec3[] verts = extractVertices(quad.getVertices());
            System.out.println(Arrays.toString(verts));

            if (verts.length < 3) continue;
            System.out.println("calculating quad 1...");
            // 计算平面
            Vec3 planeNormal = getPlaneNormal(verts);
            if (planeNormal == null) continue;
            Vec3 planePoint = verts[0];
            System.out.println("calculating quad 2...");
            // 射线与平面求交
            Vec3 intersection = rayPlaneIntersection(rayOrigin, rayDir, planePoint, planeNormal);
            if (intersection == null) continue;
            System.out.println("calculating quad 3...");

            // 检查交点是否在四边形内
            if (!isPointInQuad(intersection, verts)) continue;
            System.out.println("calculating quad 4...");

            double dist = intersection.distanceTo(rayOrigin);
            if (dist < bestDistance) {
                bestDistance = dist;
                bestQuad = quad;
                bestIntersection = intersection;
            }
        }

        if (bestQuad == null) return null;
        System.out.println("calculating offset...");
        // 6. 从四边形计算画布面的尺寸和偏移
        Vec3[] localVerts = extractVertices(bestQuad.getVertices());

        // 计算包围盒
        double minX = 1, maxX = 0, minY = 1, maxY = 0, minZ = 1, maxZ = 0;
        for (Vec3 v : localVerts) {
            if (v.x < minX) minX = v.x;
            if (v.x > maxX) maxX = v.x;
            if (v.y < minY) minY = v.y;
            if (v.y > maxY) maxY = v.y;
            if (v.z < minZ) minZ = v.z;
            if (v.z > maxZ) maxZ = v.z;
        }

        double width = maxX - minX;
        double height = maxY - minY;
        double depth = maxZ - minZ;

        // 确定主要的两个轴
        Direction.Axis faceAxis = bestQuad.getDirection().getAxis();
        double faceWidth, faceHeight;
        if (faceAxis == Direction.Axis.Y) {
            faceWidth = width;
            faceHeight = depth;
        } else if (faceAxis == Direction.Axis.Z) {
            faceWidth = width;
            faceHeight = height;
        } else {
            faceWidth = depth;
            faceHeight = height;
        }

        // 将世界尺寸转换为像素尺寸
        int pixelW = Math.max(1, (int)(faceWidth * 16.0 + 0.5));
        int pixelH = Math.max(1, (int)(faceHeight * 16.0 + 0.5));

        // 计算中心偏移
        Vec3 center = new Vec3(
                (minX + maxX) / 2.0 - 0.5,
                (minY + maxY) / 2.0 - 0.5,
                (minZ + maxZ) / 2.0 - 0.5
        );

        return new CanvasFace(bestQuad.getDirection(), center, new PixelMatrix(pixelW, pixelH));
    }

// ---- 辅助方法 ----

    /**
     * 从 BakedQuad 顶点数组解析三维坐标
     */
    private static Vec3[] extractVertices(int[] vertexData) {
        int vertexSize = 8; // 每顶点占用 8 个 int
        int vertexCount = vertexData.length / vertexSize; // 应为 4

        Vec3[] vertices = new Vec3[vertexCount];
        for (int i = 0; i < vertexCount; i++) {
            int offset = i * vertexSize;
            float x = Float.intBitsToFloat(vertexData[offset]);
            float y = Float.intBitsToFloat(vertexData[offset + 1]);
            float z = Float.intBitsToFloat(vertexData[offset + 2]);
            vertices[i] = new Vec3(x, y, z);
        }
        return vertices;
    }

    /**
     * 计算平面的法线向量 (假定顶点是按逆时针顺序的凸多边形)
     */
    @Nullable
    private static Vec3 getPlaneNormal(Vec3[] vertices) {
        if (vertices.length < 3) return null;
        Vec3 a = vertices[1].subtract(vertices[0]);
        Vec3 b = vertices[2].subtract(vertices[0]);
        Vec3 normal = a.cross(b).normalize();
        return normal.lengthSqr() > 0 ? normal : null;
    }

    /**
     * 检查四边形的方向是否与点击面方向大致匹配
     */
    private static boolean isDirectionMatching(Direction quadDir, Direction hitFace) {
        return quadDir == hitFace || quadDir.getOpposite() == hitFace;
    }

    /**
     * 射线与平面求交，返回交点或 null
     */
    @Nullable
    private static Vec3 rayPlaneIntersection(Vec3 rayOrigin, Vec3 rayDir, Vec3 planePoint, Vec3 planeNormal) {
        double denom = rayDir.dot(planeNormal);
        if (Math.abs(denom) < 1e-6) return null; // 平行
        double t = planePoint.subtract(rayOrigin).dot(planeNormal) / denom;
        if (t < 0) return null; // 平面在射线背后
        return rayOrigin.add(rayDir.scale(t));
    }

    /**
     * 检查点是否在凸四边形内（使用叉积符号法）
     */
    private static boolean isPointInQuad(Vec3 point, Vec3[] quad) {
        if (quad.length < 3) return false;
        System.out.println(point);
        // 尝试拆分为三角形 0-1-2 和 0-2-3 （兼容凸四边形和轻微非平面）
        return isPointInTriangle(point, quad[0], quad[1], quad[2])
                || isPointInTriangle(point, quad[0], quad[2], quad[3]);
    }

    /**
     * 重心坐标法判断点是否在三角形内（包括边界）
     */
    private static boolean isPointInTriangle(Vec3 p, Vec3 a, Vec3 b, Vec3 c) {
        Vec3 v0 = c.subtract(a);
        Vec3 v1 = b.subtract(a);
        Vec3 v2 = p.subtract(a);

        double dot00 = v0.dot(v0);
        double dot01 = v0.dot(v1);
        double dot02 = v0.dot(v2);
        double dot11 = v1.dot(v1);
        double dot12 = v1.dot(v2);

        double invDenom = 1.0 / (dot00 * dot11 - dot01 * dot01);
        double u = (dot11 * dot02 - dot01 * dot12) * invDenom;
        double v = (dot00 * dot12 - dot01 * dot02) * invDenom;

        return (u >= 0) && (v >= 0) && (u + v <= 1);
    }
}