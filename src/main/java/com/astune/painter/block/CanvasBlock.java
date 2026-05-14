package com.astune.painter.block;

import com.astune.painter.network.CanvasPistonDataCache;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import org.jetbrains.annotations.Nullable;
import net.minecraft.server.level.ServerLevel;
import com.astune.painter.client.CanvasBlockClientExtensions;

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

    // --- 光照委托 ---
    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        BlockState mimicked = getMimicked(level, pos);
        return mimicked != null ? mimicked.getLightBlock(level, pos) : super.getLightBlock(state, level, pos);
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CanvasBlockEntity canvasBE) {
            BlockState mimicked = canvasBE.getMimickedState();
            if (mimicked != null) {
                int light = mimicked.getLightEmission(level, pos);
                return light;
            }
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

        BlockState updated = mimicked;
        if (mimicked.getBlock() instanceof RedstoneLampBlock) {
            boolean hasSignal = level.hasNeighborSignal(pos);
            boolean currentLit = mimicked.getValue(RedstoneLampBlock.LIT);
            if (hasSignal != currentLit) {
                if (!hasSignal) {
                    level.scheduleTick(pos, this, 4);
                } else {
                    // 充能：立即点亮
                    updated = mimicked.setValue(RedstoneLampBlock.LIT, true);
                }
            }
        }

        // ==== 关键：仅在活塞推/拉时，将数据写入缓存 ====
        if (movedByPiston) {
            CompoundTag data = be.saveWithoutMetadata(level.registryAccess());   // 序列化全部数据
            // 计算移动方向：从邻居指向本方块的方向即为活塞推出方向
            Direction dir = Direction.fromDelta(
                    pos.getX() - neighborPos.getX(),
                    pos.getY() - neighborPos.getY(),
                    pos.getZ() - neighborPos.getZ()
            );
            BlockPos newPos = pos.relative(dir);           // 移动后的新坐标
            CanvasPistonDataCache.store(newPos, data);  // 以新坐标为键存储
        }

        // 模拟原方块对所有邻居方向的状态更新
        for (Direction dir : Direction.values()) {
            BlockPos offsetPos = pos.relative(dir);
            updated = updated.updateShape(dir, level.getBlockState(offsetPos),
                    level, pos, offsetPos);
        }

        if (updated != mimicked) {
            be.setMimickedState(updated);                     // 保存并标记脏
        }
        level.sendBlockUpdated(pos, state, state, 3);     // 通知客户端重新渲染
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


    // --- 掉落物委托（已实现，但在 block 类里更合适） ---
    // 之前在 CanvasBlock 中已有 getDrops / getCloneItemStack，保留它们以传递画布数据

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
                // 重要：将更新后的状态设置回 BlockEntity（但不在此处发同步包，以免递归）
                canvasBE.setMimickedState(newMimicked);
            }
        }
        // 返回自身状态，保持画布方块的身份不变
        return state;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        CanvasBlockEntity be = getCanvasBE(level, pos);
        if (be == null) return;
        BlockState mimicked = be.getMimickedState();
        if (mimicked != null && mimicked.getBlock() instanceof RedstoneLampBlock) {
            boolean hasSignal = level.hasNeighborSignal(pos);
            boolean currentLit = mimicked.getValue(RedstoneLampBlock.LIT);
            if (hasSignal) {
                level.scheduleTick(pos, this, 4);
            } else if (currentLit) {
                BlockState newMimicked = mimicked.setValue(RedstoneLampBlock.LIT, false);
                be.setMimickedState(newMimicked); // 这会同步
            }
        }
        super.tick(state, level, pos, random);
    }

}