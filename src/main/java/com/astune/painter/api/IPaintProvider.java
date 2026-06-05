// api/IPaintProvider.java
package com.astune.painter.api;

import com.astune.painter.api.blend.BlendFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * 画笔提供者接口。任何物品实现此接口后，即可在手持时触发画布绘制。
 */
public interface IPaintProvider {

    /**
     * 返回此画笔在指定位置的绘制颜色。
     * @return ARGB 颜色值，返回 null 表示擦除像素。
     */
    @Nullable
    Integer getColor(ItemStack stack, Player player, Level level, BlockPos pos, CanvasFace face, int pixelX, int pixelY);

    @Nullable
    PaintPattern getPattern(ItemStack stack, Player player, Level level,
                            BlockPos pos, Vec3 hitLoc);

    default Double getStep(){
        return 0.01;
    }


    /**
     * 每次渲染帧调用，可用于播放音效、消耗耐久等。
     * 返回 true 表示本次绘制成功，false 表示不执行绘制。
     */
    default boolean onPaintTick(ItemStack stack, Player player, Level level) {
        return true;
    }

    /**
     * 画笔连续绘制时的间隔（渲染帧数）。
     * 返回 1 表示每帧都画，2 表示隔一帧画一次。
     */
    default int getPaintInterval() {
        return 1;
    }

    @Nullable
    default BlendFunction getCustomBlendFunction(ItemStack stack) {
        return null;
    }

    /**
     * 判断当前帧是否应该触发绘制。
     * 默认实现：检测鼠标右键是否按下（保持现有行为）。
     * 推荐重写 {@link #shouldPaint(Player, BlockHitResult)} 来按命中点过滤。
     */
    default boolean shouldPaint(Player player) {
        return player.level().isClientSide
                && net.minecraft.client.Minecraft.getInstance().options.keyUse.isDown();
    }

    /**
     * 判断当前帧是否应该在指定命中点触发绘制。
     * 默认实现：忽略命中点，委托给 {@link #shouldPaint(Player)}。
     *
     * @param hit 当前准星指向的方块命中结果，可能为 null
     */
    default boolean shouldPaint(Player player, @Nullable BlockHitResult hit) {
        return shouldPaint(player);
    }

    /**
     * 计算图案在面内的坐标系变换。
     * 返回长度为 3 的数组：{origin, rightAxis, upAxis}，其中 rightAxis/upAxis 已缩放至图案尺寸。
     * 默认实现：根据玩家视线旋转图案朝向，使图案始终正对玩家。
     *
     * @param player   玩家
     * @param hitPoint 命中点世界坐标
     * @param face     被命中的面
     * @param w        图案宽度（世界坐标）
     * @param h        图案高度（世界坐标）
     * @return {左下角世界坐标, 水平轴（已缩放到宽度）, 垂直轴（已缩放到高度）}
     */
    default Vec3[] transformPatternAxes(Player player, Vec3 hitPoint, Direction face, double w, double h) {
        Vec3 normalVec = Vec3.atLowerCornerOf(face.getNormal());
        Vec3 lookVec = player.getViewVector(1.0f);

        // rightInWorld：在面内且指向屏幕右侧
        Vec3 right = lookVec.cross(normalVec).normalize();
        // upInWorld：在面内且指向屏幕上方的方向
        Vec3 up = normalVec.cross(right).normalize();

        // 如果玩家正对法线，cross 结果为零向量，回退为 world up
        if (right.lengthSqr() < 0.001 || up.lengthSqr() < 0.001) {
            Vec3 worldUp = new Vec3(0, 1, 0);
            right = worldUp.cross(normalVec).normalize();
            up = normalVec.cross(right).normalize();
        }

        Vec3 origin = hitPoint.subtract(right.scale(w / 2.0)).subtract(up.scale(h / 2.0));
        Vec3 rightTotal = right.scale(w);
        Vec3 upTotal = up.scale(h);

        return new Vec3[]{origin, rightTotal, upTotal};
    }
}