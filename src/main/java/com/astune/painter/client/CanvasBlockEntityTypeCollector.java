// com.astune.painter.client.CanvasBlockEntityTypeCollector.java
package com.astune.painter.client;

import net.minecraft.world.level.block.entity.BlockEntityType;
import java.util.HashSet;
import java.util.Set;

public class CanvasBlockEntityTypeCollector {
    // 所有实现 CanvasDataHolder 的 BlockEntityType
    public static final Set<BlockEntityType<?>> TYPES = new HashSet<>();

    public static void register(BlockEntityType<?> type) {
        TYPES.add(type);
    }
}