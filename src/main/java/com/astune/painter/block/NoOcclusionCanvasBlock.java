package com.astune.painter.block;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.state.BlockState;

public class NoOcclusionCanvasBlock extends CanvasBlock {
    public NoOcclusionCanvasBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(2.0f)
                .sound(SoundType.STONE)
                .noOcclusion()                     // 关键：无遮挡
                .isValidSpawn((s, l, p, t) -> false)
                .pushReaction(PushReaction.NORMAL));
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return false;  // 非完整方块不参与光照遮挡计算
    }
}