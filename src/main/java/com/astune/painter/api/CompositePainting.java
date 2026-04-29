package com.astune.painter.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.*;

public class CompositePainting {
    // 内层 Map：一个方块可能同时有多个面被绘制
    private final Map<BlockPos, Map<Direction, IPaintLayer>> data;

    private CompositePainting(Map<BlockPos, Map<Direction, IPaintLayer>> data) {
        this.data = Collections.unmodifiableMap(data);
    }

    /** 获取所有涉及的方块位置 */
    public Set<BlockPos> getInvolvedPositions() {
        return data.keySet();
    }

    /** 按方向分组，返回该方向上所有暴露的画层 */
    public Map<Direction, List<IPaintLayer>> groupByFacing() {
        Map<Direction, List<IPaintLayer>> result = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.values()) {
            result.put(dir, new ArrayList<>());
        }
        for (Map<Direction, IPaintLayer> faceMap : data.values()) {
            for (Map.Entry<Direction, IPaintLayer> entry : faceMap.entrySet()) {
                result.get(entry.getKey()).add(entry.getValue());
            }
        }
        return result;
    }

    /**
     * 将同一平面上的画拼接为一个 IPixelMatrix。
     * 简化实现：仅处理所有画都严格共面且方向相同的情况，
     * 否则抛出异常或返回 null。
     * 可被子类化以支持复杂拼接。
     */
    public IPixelMatrix combineToMatrix() {
        // 预留：判断所有 layer 是否同向并相邻，然后组合像素
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static class Builder {
        private final Map<BlockPos, Map<Direction, IPaintLayer>> map = new HashMap<>();

        public Builder add(IPaintLayer layer) {
            map.computeIfAbsent(layer.getPos(), k -> new EnumMap<>(Direction.class))
                    .put(layer.getFace(), layer);
            return this;
        }

        public CompositePainting build() {
            return new CompositePainting(map);
        }
    }
}