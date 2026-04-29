package com.astune.painter.item;

import com.astune.painter.api.PaintingManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class DebugEraser extends Item {
    public DebugEraser(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();
        if (face == null) return InteractionResult.PASS;

        PaintingManager.eraseLayer(level, pos, face);

        return InteractionResult.PASS;
    }
}