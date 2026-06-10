package com.astune.painter.client.inventory;

import com.astune.painter.api.BlendMode;
import com.astune.painter.network.ItemSyncPacket;
import com.astune.painter.registry.ModDataComponents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class BrushConfigScreen extends Screen {
    private final ItemStack brushStack;

    public BrushConfigScreen(ItemStack stack) {
        // 标题改为可翻译
        super(Component.translatable("painter.config.title"));
        this.brushStack = stack;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = 40;

        // 1. 画笔大小 (世界直径 0.03 ~ 1.0)
        double size = brushStack.getOrDefault(ModDataComponents.BRUSH_SIZE.get(), 0.06);
        addRenderableWidget(new SliderWidget(centerX - 75, y, 150, 20,
                Component.translatable("painter.config.brush_size", String.format("%.3f", size)),
                (size - 0.03) / 0.97, val -> {
            double newSize = 0.03 + val * 0.97;
            brushStack.set(ModDataComponents.BRUSH_SIZE.get(), newSize);
            ((SliderWidget) this.children().get(0)).setMessage(
                    Component.translatable("painter.config.brush_size", String.format("%.3f", newSize)));
        }));
        y += 25;

        // 透明度滑块 (0.0 ~ 1.0)
        float opacity = brushStack.getOrDefault(ModDataComponents.OPACITY.get(), 1.0f);
        addRenderableWidget(new SliderWidget(centerX - 75, y, 150, 20,
                Component.translatable("painter.config.opacity", String.format("%.2f", opacity)),
                opacity, val -> {
            float f = val.floatValue();
            brushStack.set(ModDataComponents.OPACITY.get(), f);
            ((SliderWidget) this.children().get(1)).setMessage(
                    Component.translatable("painter.config.opacity", String.format("%.2f", f)));
        }));
        y += 25;

        // 2. 羽化强度 (0.0 ~ 1.0)
        float feather = brushStack.getOrDefault(ModDataComponents.FEATHER_STRENGTH.get(), 0.0f);
        addRenderableWidget(new SliderWidget(centerX - 75, y, 150, 20,
                Component.translatable("painter.config.feather", String.format("%.2f", feather)),
                feather, val -> {
            float f = val.floatValue();
            brushStack.set(ModDataComponents.FEATHER_STRENGTH.get(), f);
            ((SliderWidget) this.children().get(2)).setMessage(
                    Component.translatable("painter.config.feather", String.format("%.2f", f)));
        }));
        y += 25;

        // 混合模式按钮
        BlendMode currentMode = BlendMode.valueOf(
                brushStack.getOrDefault(ModDataComponents.BLEND_MODE.get(), BlendMode.OVERWRITE.name()));
        Button blendModeButton = Button.builder(
                Component.translatable("painter.config.blend_mode",
                        Component.translatable(blendModeKey(currentMode))),
                btn -> {
                    BlendMode[] values = BlendMode.values();
                    BlendMode next = values[
                            (BlendMode.valueOf(
                                    brushStack.getOrDefault(
                                            ModDataComponents.BLEND_MODE.get(),
                                            BlendMode.OVERWRITE.name()))
                                    .ordinal() + 1) % values.length];
                    brushStack.set(ModDataComponents.BLEND_MODE.get(), next.name());
                    btn.setMessage(Component.translatable("painter.config.blend_mode",
                            Component.translatable(blendModeKey(next))));
                }
        ).pos(centerX - 50, y).size(100, 20).build();
        addRenderableWidget(blendModeButton);

        y += 25;
        // 3. 插值步长 (0.005 ~ 0.5)
        double step = brushStack.getOrDefault(ModDataComponents.STEP_SIZE.get(), 0.01);
        double stepNorm = (step - 0.005) / 0.495;
        addRenderableWidget(new SliderWidget(centerX - 75, y, 150, 20,
                Component.translatable("painter.config.step_size", String.format("%.3f", step)),
                stepNorm, val -> {
            double newStep = 0.005 + val * 0.495;
            brushStack.set(ModDataComponents.STEP_SIZE.get(), newStep);
            ((SliderWidget) this.children().get(4)).setMessage(
                    Component.translatable("painter.config.step_size", String.format("%.3f", newStep)));
        }));
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(font, title, this.width / 2, 15, 0xFFFFFF);
    }

    @Override
    public void removed() {
        super.removed();
        if (minecraft != null && minecraft.player != null) {
            syncBrushToServer();
        }
    }
    private void syncBrushToServer() {
        if(brushStack.isEmpty()) return;
        int slot = minecraft.player.getInventory().selected;
        PacketDistributor.sendToServer(new ItemSyncPacket(slot, brushStack));
    }

    /** 根据 BlendMode 枚举生成对应的翻译 key */
    private static String blendModeKey(BlendMode mode) {
        return "painter.blend_mode." + mode.name().toLowerCase();
    }

    private static class SliderWidget extends AbstractSliderButton {
        private final Consumer<Double> onApply;
        public SliderWidget(int x, int y, int width, int height, Component message,
                            double initialValue, Consumer<Double> onApply) {
            super(x, y, width, height, message, initialValue);
            this.onApply = onApply;
        }
        @Override
        protected void updateMessage() {}
        @Override
        protected void applyValue() {
            onApply.accept(this.value);
        }
    }
}