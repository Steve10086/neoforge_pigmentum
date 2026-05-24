// util/CanvasBlacklist.java
package com.astune.painter.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class CanvasBlacklist {
    // 静态不可转换方块集合（线程安全，使用 ConcurrentHashSet 或仅服务端单线程访问）
    private static final Set<Block> BLACKLIST = new HashSet<>();

    static {
        // 默认黑名单：一些不应被画布化的方块
        BLACKLIST.add(Blocks.BEDROCK);
        BLACKLIST.add(Blocks.BARRIER);
        BLACKLIST.add(Blocks.COMMAND_BLOCK);
        BLACKLIST.add(Blocks.CHAIN_COMMAND_BLOCK);
        BLACKLIST.add(Blocks.REPEATING_COMMAND_BLOCK);
        BLACKLIST.add(Blocks.STRUCTURE_BLOCK);
        BLACKLIST.add(Blocks.STRUCTURE_VOID);
        BLACKLIST.add(Blocks.MOVING_PISTON); // 活塞移动中的方块
        BLACKLIST.add(Blocks.END_PORTAL);
        BLACKLIST.add(Blocks.END_PORTAL_FRAME);
        BLACKLIST.add(Blocks.NETHER_PORTAL);
        BLACKLIST.add(Blocks.CAKE);
        BLACKLIST.add(Blocks.FIRE);
        BLACKLIST.add(Blocks.REDSTONE_WIRE);
        BLACKLIST.add(Blocks.WHEAT);
        BLACKLIST.add(Blocks.POTATOES);
        BLACKLIST.add(Blocks.CARROTS);
        BLACKLIST.add(Blocks.SWEET_BERRY_BUSH);
        BLACKLIST.add(Blocks.STICKY_PISTON);
        BLACKLIST.add(Blocks.PISTON);
        BLACKLIST.add(Blocks.PISTON_HEAD);
        // 根据需要继续添加...
    }

    private static final Predicate<BlockState> AUTO_RULES = state -> {
        Block block = state.getBlock();
        // 1. 空气或流体
        if (state.isAir()) return true;
        if (!state.getFluidState().isEmpty()) return true;

        // 2. 不可破坏方块 (如基岩、屏障、命令方块等)
        if (state.getDestroySpeed(null, null) < 0) return true;

        if (block instanceof Portal) return true; // 传送门
        if (block instanceof BushBlock) return true;
        if (block instanceof AbstractCandleBlock) return true;

        return false;
    };

    public static boolean isAllowed(Block block) {
        return !BLACKLIST.contains(block) && !AUTO_RULES.test(block.defaultBlockState());
    }

    public static boolean isAllowed(ResourceLocation blockId) {
        Block block = BuiltInRegistries.BLOCK.get(blockId);
        return block != null && isAllowed(block);
    }

    /**
     * 允许其他模组或本模组配置动态添加黑名单
     */
    public static void addToBlacklist(Block block) {
        BLACKLIST.add(block);
    }

    public static void removeFromBlacklist(Block block) {
        BLACKLIST.remove(block);
    }
}