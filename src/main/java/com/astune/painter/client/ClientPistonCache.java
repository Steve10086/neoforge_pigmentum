package com.astune.painter.client;

import com.astune.painter.api.CanvasFace;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class ClientPistonCache {
    private static final Map<BlockPos, CompoundTag> DATA_COPY = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Pair<List<Pair<CanvasFace, ResourceLocation>>, Integer>> TEXTURE_CACHE = new ConcurrentHashMap<>();

    public static void store(BlockPos oldPos, CompoundTag data) {
        //System.out.println("[CanvasPistonDataCache] Saving data to " + oldPos);

        DATA_COPY.remove(oldPos.immutable());
        DATA_COPY.put(oldPos.immutable(), data);
        TextureCleaner.addDelayedTask(20, () -> remove(oldPos));
    }

    public static void storeCanvasTexture(BlockPos oldPos, List<Pair<CanvasFace, ResourceLocation>> textures){
        if (textures != null && !textures.isEmpty()) {

            Pair<List<Pair<CanvasFace, ResourceLocation>>, Integer> oldTex = TEXTURE_CACHE.remove(oldPos);
            if (oldTex != null){
                CanvasTextureManager.releaseTextures(oldTex.getSecond());
            }

            //System.out.println("[CanvasPistonDataCache] Saving data to " + oldPos);
            List<Pair<CanvasFace, ResourceLocation>> newT = new ArrayList<>();
            int id = CanvasTextureManager.NEXT_TEXTURE_ID++;
            for (Pair<CanvasFace, ResourceLocation> p : textures){
                CanvasFace face = p.getFirst();
                ResourceLocation tex = CanvasTextureManager.getOrUpdateTexture(face, id, textures.indexOf(p));
                newT.add(new Pair<>(face, tex));
            }
            TEXTURE_CACHE.put(oldPos.immutable(), new Pair<>(newT, id));
        }
    }

    public static CompoundTag get(BlockPos pos) {
        return DATA_COPY.get(pos);
    }

    public static List<Pair<CanvasFace, ResourceLocation>> getCanvasTexture(BlockPos pos) {
        //System.out.println("[CanvasPistonDataCache] Loading data from " + pos);
        if (TEXTURE_CACHE.get(pos) != null) return TEXTURE_CACHE.get(pos).getFirst();
        return null;
    }

    public static void remove(BlockPos pos){
        //System.out.println("remove " + pos);
        DATA_COPY.remove(pos.immutable());
        Pair<List<Pair<CanvasFace, ResourceLocation>>, Integer> oldTex = TEXTURE_CACHE.remove(pos);
        if (oldTex != null){
            CanvasTextureManager.releaseTextures(oldTex.getSecond());
        }
    }

    @EventBusSubscriber(modid = "painter", value = Dist.CLIENT)
    private static class TextureCleaner {

        private static final List<Pair<Integer, Runnable>> counter = new ArrayList<>();

        public static void addDelayedTask(int ticks, Runnable task) {
            counter.add(new Pair<>(ticks, task));
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Pre event) {
            if (counter.isEmpty()) return;
            List<Pair<Integer, Runnable>> oldCounter = new ArrayList<>(counter);
            counter.clear();
            for (var p : oldCounter) {
                int newC = p.getFirst() - 1;
                if (newC <= 0) {
                    p.getSecond().run();
                } else {
                    counter.add(new Pair<>(newC, p.getSecond()));
                }
            }
        }
    }
}
