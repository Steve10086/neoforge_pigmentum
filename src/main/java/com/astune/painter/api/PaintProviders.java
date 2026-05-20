// api/PaintProviders.java
package com.astune.painter.api;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局画笔注册表。其他模组或你的其他物品可以在此注册。
 */
public class PaintProviders {

    private static final Map<Item, IPaintProvider> PROVIDERS = new ConcurrentHashMap<>();

    /**
     * 注册一个画笔提供者。
     */
    public static void register(Item item, IPaintProvider provider) {
        PROVIDERS.put(item, provider);
    }

    /**
     * 根据物品栈获取画笔提供者。
     */
    @javax.annotation.Nullable
    public static IPaintProvider getProvider(ItemStack stack) {
        return PROVIDERS.get(stack.getItem());
    }

    /**
     * 判断物品是否是画笔。
     */
    public static boolean isPaintbrush(ItemStack stack) {
        return PROVIDERS.containsKey(stack.getItem());
    }
}