// client/CanvasBlockClientExtensions.java
package com.astune.painter.client;

import com.astune.painter.block.CanvasBlockEntity;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;

public class CanvasBlockClientExtensions implements IClientBlockExtensions {

    @Override
    public boolean addDestroyEffects(BlockState canvasState, Level level, BlockPos pos, ParticleEngine manager) {
        // 获取当前画布所仿制的方块状态
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CanvasBlockEntity canvasBE)) {
            return false;   // 无数据，使用原版默认粒子
        }

        BlockState mimicked = canvasBE.getMimickedState();
        if (mimicked == null) {
            return false;
        }

        // 手动产生破坏粒子
        AABB box = mimicked.getShape(level, pos).bounds();
        int count = 48;   // 粒子数量，可根据需要调整
        for (int i = 0; i < count; i++) {
            double x = pos.getX() + level.random.nextDouble() * (box.maxX - box.minX) + box.minX;
            double y = pos.getY() + level.random.nextDouble() * (box.maxY - box.minY) + box.minY;
            double z = pos.getZ() + level.random.nextDouble() * (box.maxZ - box.minZ) + box.minZ;

            // 创建代表方块碎片的粒子，使用正确的纹理
            TerrainParticle particle = new TerrainParticle(
                    (net.minecraft.client.multiplayer.ClientLevel) level,
                    x, y, z, 0.0, 0.0, 0.0,
                    mimicked,  // 粒子将使用这个 BlockState 的模型来获取颜色/纹理
                    pos
            );
            manager.add(particle);
        }
        // 返回 true 表示我们已经完全接管了粒子生成，原版不会再尝试
        return true;
    }
}
