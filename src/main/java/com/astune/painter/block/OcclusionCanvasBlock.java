package com.astune.painter.block;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

public class OcclusionCanvasBlock extends CanvasBlock{

    public OcclusionCanvasBlock() {
        super(Properties.of()
                .strength(2.0f)            // 可被子类委托覆盖，但基类值仅作备用
                .sound(SoundType.STONE)
                .isValidSpawn((state, level, pos, type) -> true)
                .pushReaction(PushReaction.NORMAL));
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;  // 非完整方块不参与光照遮挡计算
    }
}
