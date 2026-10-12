package com.astune.painter.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.BiPredicate;
import java.util.function.DoubleSupplier;
import java.util.function.Predicate;

/** Common-safe hooks for behavior implemented by the physical client. */
public final class DebugPaintbrushClientBridge {
    private static volatile BiPredicate<Player, BlockHitResult> paintPredicate = (player, hit) -> false;
    private static volatile DoubleSupplier stepSupplier = () -> 1 / 8.0;
    private static volatile Predicate<ItemStack> configScreenOpener = stack -> false;

    private DebugPaintbrushClientBridge() {
    }

    public static void registerClientHooks(BiPredicate<Player, BlockHitResult> paintPredicate,
                                           DoubleSupplier stepSupplier,
                                           Predicate<ItemStack> configScreenOpener) {
        DebugPaintbrushClientBridge.paintPredicate = paintPredicate;
        DebugPaintbrushClientBridge.stepSupplier = stepSupplier;
        DebugPaintbrushClientBridge.configScreenOpener = configScreenOpener;
    }

    public static boolean shouldPaint(Player player, BlockHitResult hit) {
        return paintPredicate.test(player, hit);
    }

    public static double getStep() {
        return stepSupplier.getAsDouble();
    }

    public static boolean tryOpenConfig(ItemStack stack) {
        return configScreenOpener.test(stack);
    }
}
