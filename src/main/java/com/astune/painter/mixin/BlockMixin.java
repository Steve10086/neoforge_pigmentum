package com.astune.painter.mixin;

import com.astune.painter.CanvasProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public class BlockMixin {

    /**
     * 在Block构造函数的createBlockStateDefinition方法执行后，动态添加自定义属性。
     * 这个注入点对于继承Block的所有子类都是安全的。
     */
    @Inject(
            method = "createBlockStateDefinition(Lnet/minecraft/world/level/block/state/StateDefinition$Builder;)V",
            at = @At("HEAD")
    )
    protected void injectCanvasProperty(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo ci) {
        // 向状态定义构建器中添加我们的自定义属性
        System.out.println("inject to " + builder);
        builder.add(CanvasProperties.HAVE_CANVAS);
    }
}