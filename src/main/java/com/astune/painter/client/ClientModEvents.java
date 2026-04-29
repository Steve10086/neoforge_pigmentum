package com.astune.painter.client;

import com.astune.painter.test.OverlayBakedModel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = "painter", value = Dist.CLIENT)
public class ClientModEvents {

    // 全局叠加
    @SubscribeEvent
    public static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        for (Map.Entry<ModelResourceLocation, BakedModel> entry : new HashMap<>(models).entrySet()) {
            if (entry.getValue() instanceof OverlayBakedModel) continue;
            models.put(entry.getKey(), new OverlayBakedModel(entry.getValue()));
        }
    }
}