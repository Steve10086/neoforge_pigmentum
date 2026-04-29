package com.astune.painter.item;

import com.astune.painter.api.PaintingManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebugPaintbrush extends Item {
    private static final Logger LOGGER = LoggerFactory.getLogger(DebugPaintbrush.class);

    public DebugPaintbrush(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();
        LOGGER.info("[Painter] Paintbrush used at {} face {} by {}", pos, face, context.getPlayer().getName().getString());
        PaintingManager.paintPixel(level, pos, face, 8, 8, 0xFFFFFFFF);


        return InteractionResult.CONSUME;
    }
}