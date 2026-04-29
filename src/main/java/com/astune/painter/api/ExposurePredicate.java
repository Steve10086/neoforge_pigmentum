package com.astune.painter.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@FunctionalInterface
public interface ExposurePredicate {
    /**
     * 判断方块在指定面是否“暴露”（即该面可绘制且可见）。
     * 默认实现：相邻方块为空气或不完整方块（如草、半砖等）。
     */
    boolean isExposed(Level level, BlockPos pos, Direction face);

    ExposurePredicate DEFAULT = (level, pos, face) -> {
        BlockState neighbor = level.getBlockState(pos.relative(face));
        return neighbor.isAir() || !neighbor.isSolidRender(level, pos.relative(face));
    };
}
