package com.astune.painter.block;

import com.astune.painter.api.CanvasDataHolder;
import com.astune.painter.attachment.ModAttachments;
import com.astune.painter.api.CanvasData;
import com.astune.painter.registry.ModBlocks;
import com.astune.painter.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class CanvasBlockItem extends BlockItem {
    public CanvasBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, @Nullable Player player,
                                                 ItemStack stack, BlockState state) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CanvasBlockEntity canvasBE) {
            BlockState originalState = stack.get(ModDataComponents.BLOCK_STATE.get());
            if (originalState != null) {
                canvasBE.setMimickedState(originalState);
            }
            // 画布数据现在在物品的 CANVAS_DATA 组件中
            var canvasData = stack.get(ModDataComponents.CANVAS.get());
            if (canvasData != null) {
                if (be instanceof CanvasDataHolder holder) {
                    holder.painter$setCanvasData(canvasData);
                }
                // 旧路径兼容
                if (canvasBE instanceof CanvasBlockEntity) {
                    // 不再需要额外操作，CanvasBlockEntity 的 painter$setCanvasData 已调用
                }
            }
            return true;
        }
        return false;
    }
}