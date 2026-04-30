package com.astune.painter.block;

import com.astune.painter.data.CanvasData;
import com.astune.painter.registry.ModBlocks;
import com.astune.painter.registry.ModDataComponents;
import com.astune.painter.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;

public class CanvasBlock extends Block implements EntityBlock {
    public CanvasBlock() {
        super(Properties.ofFullCopy(Blocks.BEDROCK).noOcclusion().dynamicShape());
    }




    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CanvasBlockEntity(pos, state);
    }

    // ------------------------------------------------------
    // 形状完全委托
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof CanvasBlockEntity cbe ? cbe.getMimickedState().getShape(level, pos, ctx) : super.getShape(state, level, pos, ctx);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof CanvasBlockEntity cbe ? cbe.getMimickedState().getCollisionShape(level, pos, ctx) : super.getCollisionShape(state, level, pos, ctx);
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof CanvasBlockEntity cbe ? cbe.getMimickedState().getOcclusionShape(level, pos) : super.getOcclusionShape(state, level, pos);
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof CanvasBlockEntity cbe ? cbe.getMimickedState().getVisualShape(level, pos, ctx) : super.getVisualShape(state, level, pos, ctx);
    }

    // 新增光照委托方法
    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CanvasBlockEntity cbe) {
            return cbe.getMimickedState().getLightBlock(level, pos);
        }
        return super.getLightBlock(state, level, pos);
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CanvasBlockEntity cbe) {
            return cbe.getMimickedState().getShadeBrightness(level, pos);
        }
        return super.getShadeBrightness(state, level, pos);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CanvasBlockEntity cbe) {
            return cbe.getMimickedState().propagatesSkylightDown(level, pos);
        }
        return super.propagatesSkylightDown(state, level, pos);
    }

    // ------------------------------------------------------
    // 破坏速度：委托给原方块，解除不可破坏限制
    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CanvasBlockEntity cbe) {
            BlockState mimicked = cbe.getMimickedState();
            return mimicked.getDestroyProgress(player, level, pos);
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    // ------------------------------------------------------
    // 粒子效果：显示原方块的粒子
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CanvasBlockEntity cbe) {
            BlockState mimicked = cbe.getMimickedState();
            // 委托原方块的粒子效果
            mimicked.getBlock().animateTick(mimicked, level, pos, random);
        }
    }

    // 还有一个可能影响粒子的是 getParticleIcon (如果需要，但在 Block 中没有此方法，忽略)

    // ------------------------------------------------------
    // 中键选取：返回带组件的原方块物品
    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CanvasBlockEntity cbe) {
            ItemStack stack = new ItemStack(ModItems.CANVAS.get());
            stack.set(ModDataComponents.BLOCK_STATE.get(), cbe.getMimickedState());
            if (cbe.getCanvasData() != null) {
                stack.set(ModDataComponents.CANVAS.get(), cbe.getCanvasData());
            }
            return stack;
        }
        return super.getCloneItemStack(state, target, level, pos, player);
    }

    // ------------------------------------------------------
    // 掉落（已实现，保持不变）
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockPos pos = new BlockPos(
                (int) builder.getParameter(LootContextParams.ORIGIN).x(),
                (int) builder.getParameter(LootContextParams.ORIGIN).y(),
                (int) builder.getParameter(LootContextParams.ORIGIN).z()
        );
        BlockEntity be = builder.getLevel().getBlockEntity(pos);
        if (be instanceof CanvasBlockEntity cbe) {
            // 掉落画布方块物品（而不是原方块物品）
            ItemStack stack = new ItemStack(ModItems.CANVAS.get());
            stack.set(ModDataComponents.BLOCK_STATE.get(), cbe.getMimickedState());
            if (cbe.getCanvasData() != null) {
                stack.set(ModDataComponents.CANVAS.get(), cbe.getCanvasData());
            }
            return List.of(stack);
        }
        return List.of();
    }

    // ------------------------------------------------------
    // onRemove：保留活塞推动时的方块实体
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            level.removeBlockEntity(pos);
        }
        super.onRemove(state, level, pos, newState, moved);
    }


}