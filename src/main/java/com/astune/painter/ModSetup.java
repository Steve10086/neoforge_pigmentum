package com.astune.painter;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.lang.reflect.Field;

@EventBusSubscriber(modid = Painter.MODID)
public class ModSetup {

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        // 为所有已注册方块添加 HAVE_CANVAS 属性
        BuiltInRegistries.BLOCK.forEach(block -> {
            try {
                addCanvasPropertyToBlock(block);
            } catch (Exception e) {
                Painter.LOGGER.error("Failed to add canvas property to block: {}", BuiltInRegistries.BLOCK.getKey(block), e);
            }
        });
    }

    private static void addCanvasPropertyToBlock(Block block) throws Exception {
        // 1. 通过反射获取 stateDefinition 字段
        Field stateDefField = Block.class.getDeclaredField("stateDefinition");
        stateDefField.setAccessible(true);
        StateDefinition<Block, BlockState> oldDef = (StateDefinition<Block, BlockState>) stateDefField.get(block);

        // 如果已经包含我们的属性则跳过（避免重复添加）
        if (oldDef.getProperties().contains(CanvasProperties.HAVE_CANVAS)) {
            return;
        }

        // 2. 创建新的 Builder 并复制原有属性 + 我们的属性
        StateDefinition.Builder<Block, BlockState> builder = new StateDefinition.Builder<>(block);
        oldDef.getProperties().forEach(builder::add);
        builder.add(CanvasProperties.HAVE_CANVAS);
        StateDefinition<Block, BlockState> newDef = builder.create(Block::defaultBlockState, BlockState::new);

        // 3. 替换方块的 stateDefinition
        stateDefField.set(block, newDef);

        // 4. 重新设置默认状态（包含 have_canvas=false）
        Field defaultStateField = Block.class.getDeclaredField("defaultBlockState");
        defaultStateField.setAccessible(true);
        BlockState newDefault = newDef.any().setValue(CanvasProperties.HAVE_CANVAS, false);
        defaultStateField.set(block, newDefault);
    }
}