package com.astune.painter.mixin;

import com.astune.painter.api.CanvasFace;
import com.astune.painter.api.ResourcesBundle;
import com.astune.painter.block.CanvasBlock;
import com.astune.painter.client.CanvasBlockEntityRenderer;
import com.astune.painter.client.ClientPistonCache;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ClientHooks.class)
public class ForgeHooksClientMixin {
    @Inject(method = "renderPistonMovedBlocks", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onRenderPistonMoved(
            BlockPos pos, BlockState state, PoseStack stack, MultiBufferSource bufferSource, Level level, boolean checkSides, int packedOverlay, BlockRenderDispatcher blockRenderer, CallbackInfo ci) {

        if (!(state.getBlock() instanceof CanvasBlock)) return;

        CompoundTag tag = ClientPistonCache.get(pos);
        if (tag != null && tag.contains("mimicked_state")){
            //System.out.println("render animated state");
            RegistryAccess registries = Minecraft.getInstance().level.registryAccess();
            BlockState state1 = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("mimicked_state"));

            BakedModel model = blockRenderer.getBlockModel(state1);

            for (RenderType renderType : model.getRenderTypes(state, RandomSource.create(state.getSeed(pos)), ModelData.EMPTY)) {
                VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderTypeHelper.getMovingBlockRenderType(renderType));
                blockRenderer.getModelRenderer().tesselateBlock(level, model, state, pos, stack, vertexConsumer, checkSides, RandomSource.create(), state.getSeed(pos), packedOverlay, ModelData.EMPTY, renderType);
            }
        }
        List<Pair<CanvasFace, ResourcesBundle>> texture = ClientPistonCache.getCanvasTexture(pos);
        if (texture != null){
            //System.out.println("get textures");
            stack.pushPose();
            stack.translate(0.5, 0.5, 0.5); // 方块中心
            CanvasBlockEntityRenderer.renderCanvasTexture(level, pos, stack, bufferSource, texture, 0, packedOverlay, true);
            stack.popPose();
        }

        if(tag != null){
            ci.cancel();
        }

    }
}