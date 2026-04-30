package com.astune.painter.client;

import com.astune.painter.registry.ModBlocks;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

@EventBusSubscriber(modid = "painter", value = Dist.CLIENT)
public class CanvasBlockModelLoader {
    @SubscribeEvent
    public static void onModelBake(ModelEvent.ModifyBakingResult event) {
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath("painter", "canvas");
        // 方块的所有变体（包括空字符串变体，用于默认状态）
        for (BlockState state : ModBlocks.CANVAS.get().getStateDefinition().getPossibleStates()) {
            ModelResourceLocation variantLoc = BlockModelShaper.stateToModelLocation(state);
            BakedModel original = event.getModels().get(variantLoc);
            if (original != null) {
                event.getModels().put(variantLoc, new CanvasBlockBakedModel(original));
            }
        }
        // 物品模型
        ModelResourceLocation itemLoc = ModelResourceLocation.inventory(rl);
        BakedModel originalItem = event.getModels().get(itemLoc);
        if (originalItem != null) {
            event.getModels().put(itemLoc, new CanvasBlockBakedModel(originalItem));
        }
    }
}