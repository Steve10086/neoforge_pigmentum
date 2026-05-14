package com.astune.painter.client;

import com.astune.painter.block.CanvasBlockModel;
import com.astune.painter.mixin.BlockEntityRenderersAccessor;
import com.astune.painter.registry.ModBlockEntities;
import com.astune.painter.registry.ModBlocks;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import java.util.Map;

@EventBusSubscriber(modid = "painter", value = Dist.CLIENT)
public class ClientSetup {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // 获取原版渲染器映射
        Map<BlockEntityType<?>, BlockEntityRendererProvider<?>> providers =
                BlockEntityRenderersAccessor.getProviders();

        // 为所有 BlockEntityType 注册复合渲染器
        for (BlockEntityType type : BuiltInRegistries.BLOCK_ENTITY_TYPE) {
            BlockEntityRendererProvider originalProvider = providers.get(type);

            event.registerBlockEntityRenderer(
                    (BlockEntityType) type,
                    ctx -> new CompositeRenderer<>(
                            originalProvider != null ? originalProvider.create(ctx) : null,
                            new CanvasBlockEntityRenderer(ctx)
                    )
            );
        }
    }


    //@SubscribeEvent
    public static void onBakingComplete(ModelEvent.ModifyBakingResult event) {
        // 获取画布方块的两个变体
        replaceModel(event, ModBlocks.CANVAS_OCCLUSION.get());
        replaceModel(event, ModBlocks.CANVAS_NO_OCCLUSION.get());
    }

    private static void replaceModel(ModelEvent.ModifyBakingResult event, Block block) {
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            ModelResourceLocation location = BlockModelShaper.stateToModelLocation(state);
            BakedModel original = event.getModels().get(location);
            if (original != null) {
                event.getModels().put(location, new CanvasBlockModel(original));
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerBlock(
                new CanvasBlockClientExtensions(),
                ModBlocks.CANVAS_OCCLUSION.get(),
                ModBlocks.CANVAS_NO_OCCLUSION.get()
        );
    }
}