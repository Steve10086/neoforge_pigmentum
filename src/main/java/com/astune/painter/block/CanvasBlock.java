package com.astune.painter.block;

import com.astune.painter.api.CanvasDataHolder;
import com.astune.painter.client.ClientPistonCache;
import com.astune.painter.registry.ModDataComponents;
import com.astune.painter.util.CanvasBlacklist;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraft.server.level.ServerLevel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public abstract class CanvasBlock extends Block implements EntityBlock {


    public CanvasBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CanvasBlockEntity(pos, state);
    }

    // --- 辅助方法 ---
    @Nullable
    private CanvasBlockEntity getCanvasBE(BlockGetter level, BlockPos pos) {
        if (level == null || pos == null) return null;
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof CanvasBlockEntity ? (CanvasBlockEntity) be : null;
    }

    @Nullable
    private BlockState getMimicked(BlockGetter level, BlockPos pos) {
        CanvasBlockEntity be = getCanvasBE(level, pos);
        return be != null ? be.getMimickedState() : null;
    }


    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        // 需要光照遮挡计算，委托给 mimickedState 的同名方法（若可用）
        // 但此方法没有 level/pos，只能返回 true 让形状决定
        return true;
    }
    // --- interaction ---
    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        BlockState mimicked = getMimicked(level, pos);
        if (mimicked != null) {
            mimicked.getBlock().fallOn(level, mimicked, pos, entity, fallDistance);
        } else {
            super.fallOn(level, state, pos, entity, fallDistance);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        BlockState mimicked = getMimicked(level, pos);
        if (mimicked != null) {
            mimicked.getBlock().stepOn(level, pos, mimicked, entity);
        } else {
            super.stepOn(level, pos, state, entity);
        }
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        BlockState mimicked = getMimicked(level, pos);
        if (mimicked != null) {
            mimicked.getBlock().wasExploded(level, pos, explosion);
        } else {
            super.wasExploded(level, pos, explosion);
        }
    }

    @Override
    public void handlePrecipitation(BlockState state, Level level, BlockPos pos, Biome.Precipitation precipitation) {
        BlockState mimicked = getMimicked(level, pos);
        if (mimicked != null) {
            mimicked.getBlock().handlePrecipitation(mimicked, level, pos, precipitation);
        } else {
            super.handlePrecipitation(state, level, pos, precipitation);
        }
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        BlockState mimicked = getMimicked(level, pos);
        if (mimicked != null) {
            mimicked.getBlock().destroy(level, pos, mimicked);
        } else {
            super.destroy(level, pos, state);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        BlockState mimicked = getMimicked(level, pos);
        if (mimicked != null) {
            return mimicked.getBlock().getStateForPlacement(context);
        } else {
            return super.getStateForPlacement(context);
        }
    }

    // --- 光照委托 ---
    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        BlockState mimicked = getMimicked(level, pos);
        return mimicked != null ? mimicked.getLightBlock(level, pos) : super.getLightBlock(state, level, pos);
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        BlockState mimicked = getMimicked(level, pos);
        if (mimicked != null) {
            int light = mimicked.getLightEmission(level, pos);
            return light;
        }
        return super.getLightEmission(state, level, pos);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        BlockState mimicked = getMimicked(level, pos);
        return mimicked != null ? mimicked.propagatesSkylightDown(level, pos) : super.propagatesSkylightDown(state, level, pos);
    }

    // --- 破坏速度委托 ---
    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockState mimicked = getMimicked(level, pos);
        return mimicked != null ? mimicked.getDestroyProgress(player, level, pos) : super.getDestroyProgress(state, player, level, pos);
    }

    // --- 外观委托（染色） ---
    @Override
    public BlockState getAppearance(BlockState state, BlockAndTintGetter level, BlockPos pos,
                                    Direction side, @Nullable BlockState queryState, @Nullable BlockPos queryPos) {
        BlockState mimicked = getMimicked(level, pos);
        if (mimicked != null) {
            return mimicked.getBlock().getAppearance(mimicked, level, pos, side, queryState, queryPos);
        }
        return super.getAppearance(state, level, pos, side, queryState, queryPos);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide) return;

        CanvasBlockEntity be = getCanvasBE(level, pos);
        if (be == null) return;
        BlockState mimicked = be.getMimickedState();
        if (mimicked == null) return;

        try {
            mimicked.getBlock();
            Method method = mimicked.getBlock().getClass().getMethod(
                    "neighborChanged",
                    BlockState.class, Level.class, BlockPos.class,
                    Block.class, BlockPos.class, boolean.class
            );
            method.setAccessible(true);
            method.invoke(mimicked.getBlock(), mimicked, level, pos,
                    neighborBlock, neighborPos, movedByPiston);
            //System.out.println("[CanvasBlock] success to invoke neighborChanged for mimicked " + mimicked.getBlock().getName());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            //System.out.println("[CanvasBlock] failed to invoke neighborChanged for mimicked " + mimicked.getBlock().getName() + " because " + e);
        }
    }
    // --- 声音委托 ---
    @Override
    public SoundType getSoundType(BlockState state) {
        // 无法获取 mimickedState，返回默认值（石头音效覆盖大部分情况）
        return SoundType.STONE;
    }

    // --- 推动反应 ---
    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.NORMAL;
    }

    // --- 红石委托 ---
    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        BlockState mimicked = getMimicked(level, pos);
        return mimicked != null ? mimicked.getSignal(level, pos, direction) : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        BlockState mimicked = getMimicked(level, pos);
        return mimicked != null ? mimicked.getDirectSignal(level, pos, direction) : 0;
    }

    @Override
    public boolean shouldCheckWeakPower(BlockState state, SignalGetter level, BlockPos pos, Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CanvasBlockEntity canvasBE) {
            BlockState mimicked = canvasBE.getMimickedState();
            if (mimicked != null) {
                return mimicked.shouldCheckWeakPower(level, pos, side);
            }
        }
        return super.shouldCheckWeakPower(state, level, pos, side);
    }


    // --- 流体相关 ---
    @Override
    public FluidState getFluidState(BlockState state) {
        return net.minecraft.world.level.material.Fluids.EMPTY.defaultFluidState();
    }


    // --- 掉落物委托 ---
    // 之前在 CanvasBlock 中已有 getDrops / getCloneItemStack，保留它们以传递画布数据
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockState mimicked = getMimicked(level, pos);
        if (mimicked != null) {
            // 委托原方块的破坏前行为
            mimicked.getBlock().playerWillDestroy(level, pos, mimicked, player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity be, ItemStack tool) {
        BlockState mimicked = getMimicked(level, pos);
        if (mimicked != null) {
            mimicked.getBlock().playerDestroy(level, player, pos, mimicked, be, tool);
        } else {
            super.playerDestroy(level, player, pos, state, be, tool);
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        BlockState mimicked = getMimicked(level, pos);
        if (mimicked != null) {
            return mimicked.getBlock().getCloneItemStack(level, pos, state);
        } else {
            return super.getCloneItemStack(level, pos, state);
        }
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, Level level, @NotNull BlockPos pos,
                                                        @NotNull Player player, @NotNull BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockState mimicked = getMimicked(level, pos);
        if (mimicked != null) {
            // 委托给原方块的交互逻辑
            return mimicked.useWithoutItem(level, player, hit);
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    // --- 随机 tick / 动画等委托 ---
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        BlockState mimicked = getMimicked(level, pos);
        if (mimicked != null) {
            mimicked.getBlock().animateTick(mimicked, level, pos, rand);
        } else {
            super.animateTick(state, level, pos, rand);
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CanvasBlockEntity canvasBE) {
            BlockState mimicked = canvasBE.getMimickedState();
            if (mimicked != null) {
                // 委托给原方块的状态更新
                BlockState newMimicked = mimicked.updateShape(direction, neighborState, level, pos, neighborPos);
                if (!CanvasBlacklist.isAllowed(newMimicked.getBlock())){
                    return newMimicked;
                }else{
                    canvasBE.setMimickedState(newMimicked);
                }
            }
        }
        // 返回自身状态，保持画布方块的身份不变
        return state;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState mimicked = getMimicked(level, pos);
        if (mimicked != null) {
            //System.out.println("tick on " + mimicked.getBlock().getName());
            // 直接调用原方块的 tick，由于 setBlock 已被拦截，所有状态变更都会安全转换
            mimicked.tick(level, pos, random);
            return; // 不再调用 super
        }
        super.tick(state, level, pos, random);
    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        if (!level.isClientSide) { // 只在服务端包装
            BlockState mimicked = getMimicked(level, pos);
            if (mimicked != null) {
                return mimicked.getMenuProvider(level, pos);
            }
        }
        return super.getMenuProvider(state, level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            BlockState mimicked = getMimicked(level, pos);
            if (mimicked != null) {
                mimicked.onRemove(level, pos, newState, moved);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

}