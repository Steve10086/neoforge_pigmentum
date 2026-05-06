package com.astune.painter.mixin;

import com.astune.painter.CanvasProperties;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.function.Function;

@Mixin(StateDefinition.Builder.class)
public class StateDefinitionBuilderMixin {

    @Inject(method = "create", at = @At("HEAD"))
    private void injectCanvasProperty(
            Function<Block, BlockState> defaultStateFactory,
            StateDefinition.Factory<Block, BlockState> newStateFactory,
            CallbackInfoReturnable<StateDefinition<Block, BlockState>> cir) {

        StateDefinition.Builder<Block, BlockState> builder = (StateDefinition.Builder<Block, BlockState>) (Object) this;
        Block owner = getOwnerBlock(builder);

        // 排除所有可能破坏启动流程的技术性方块
        if (shouldAddProperty(owner)) {
            builder.add(CanvasProperties.HAVE_CANVAS);
        }
    }

    /**
     * 通过反射获取 build 对应的外部 Block 实例。
     * StateDefinition.Builder 是 Block 的非静态内部类，有 this$0 字段。
     */
    private Block getOwnerBlock(StateDefinition.Builder<Block, BlockState> builder) {
        try {
            Field field = builder.getClass().getDeclaredField("this$0");
            field.setAccessible(true);
            return (Block) field.get(builder);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查该方块是否为“可绘制”的普通方块，排除技术性方块。
     */
    private boolean shouldAddProperty(Block owner) {
        // 空气及其变体
        if (owner instanceof AirBlock) return false;
        // 流体（水、岩浆等）
        if (owner instanceof LiquidBlock) return false;
        // 火（普通火、灵魂火）
        if (owner instanceof FireBlock) return false;
        // 传送门
        if (owner instanceof EndPortalBlock) return false;
        if (owner instanceof NetherPortalBlock) return false;
        // 活塞移动中的技术性方块
        if (owner instanceof MovingPistonBlock) return false;
        // 基岩、末地传送门等特殊不可破坏方块也可以排除（可选）
        // if (owner instanceof BedrockBlock) return false;
        // if (owner instanceof EndPortalBlock) return false;

        return true;
    }
}