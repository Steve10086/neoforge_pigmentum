package com.astune.painter.client;

import com.astune.painter.client.inventory.BrushConfigScreen;
import com.astune.painter.item.DebugPaintbrush;
import com.astune.painter.item.DebugPaintbrushClientBridge;
import com.astune.painter.network.ItemSyncPacket;
import com.astune.painter.registry.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.nio.IntBuffer;

import static com.astune.painter.client.ClientSetup.KEY_PICK_COLOR;

@OnlyIn(Dist.CLIENT)
public final class DebugPaintbrushClientHandler {
    private static boolean pickingColor;
    private static net.minecraft.world.phys.Vec3 lastHitLoc;
    private static final ResourceLocation CROSSHAIR_ID = ResourceLocation.withDefaultNamespace("crosshair");

    private DebugPaintbrushClientHandler() {
    }

    public static void registerHooks() {
        DebugPaintbrushClientBridge.registerClientHooks(
                DebugPaintbrushClientHandler::shouldPaint,
                DebugPaintbrushClientHandler::getStep,
                DebugPaintbrushClientHandler::openConfigScreen
        );
    }

    private static boolean shouldPaint(net.minecraft.world.entity.player.Player player, BlockHitResult result) {
        if (!(player.level().isClientSide && Minecraft.getInstance().options.keyUse.isDown())) {
            return false;
        }
        if (lastHitLoc == null) {
            lastHitLoc = result.getLocation();
            return true;
        }
        if (lastHitLoc.distanceTo(result.getLocation()) > getStep()) {
            lastHitLoc = result.getLocation();
            return true;
        }
        return false;
    }

    private static double getStep() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            ItemStack stack = minecraft.player.getMainHandItem();
            if (stack.getItem() instanceof DebugPaintbrush) {
                return stack.getOrDefault(ModDataComponents.STEP_SIZE.get(), 1 / 8.0);
            }
        }
        return 1 / 8.0;
    }

    private static boolean openConfigScreen(ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        HitResult hit = minecraft.hitResult;
        if (hit != null && hit.getType() != HitResult.Type.MISS) {
            return false;
        }
        minecraft.setScreen(new BrushConfigScreen(stack));
        return true;
    }

    private static void pickColor(ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        int centerX = minecraft.getWindow().getWidth() / 2;
        int centerY = minecraft.getWindow().getHeight() / 2;
        int color = readPixel(centerX + 3, centerY + 3);
        if (color == 0) {
            return;
        }

        stack.set(ModDataComponents.CURRENT_COLOR.get(), color);
        String hex = String.format("#%06X", color & 0x00FFFFFF);
        Component name = Component.translatable(stack.getItem().getDescriptionId())
                .append(Component.literal(" (#" + hex + ")"))
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color & 0x00FFFFFF)));
        stack.set(DataComponents.CUSTOM_NAME, name);

        if (minecraft.player != null) {
            Component message = Component.literal("Picked color: " + hex)
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color & 0x00FFFFFF)));
            minecraft.gui.setOverlayMessage(message, false);
        }
    }

    private static int readPixel(int x, int y) {
        Minecraft minecraft = Minecraft.getInstance();
        int glY = minecraft.getWindow().getHeight() - y;
        IntBuffer buffer = BufferUtils.createIntBuffer(1);
        GL11.glReadPixels(x, glY, 1, 1, GL12.GL_RGBA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, buffer);

        int rgba = buffer.get(0);
        int alpha = (rgba >>> 24) & 0xFF;
        int red = rgba & 0xFF;
        int green = (rgba >>> 8) & 0xFF;
        int blue = (rgba >>> 16) & 0xFF;
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    @EventBusSubscriber(modid = "painter", value = Dist.CLIENT)
    public static class CrosshairHandler {
        @SubscribeEvent
        public static void onRenderCrosshair(RenderGuiLayerEvent.Pre event) {
            if (event.getName().equals(CROSSHAIR_ID) && pickingColor) {
                event.setCanceled(true);
            }
        }
    }

    @EventBusSubscriber(modid = "painter", value = Dist.CLIENT)
    public static class PreventBlockUseHandler {
        @SubscribeEvent
        public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
            ItemStack stack = event.getItemStack();
            if (stack.getItem() instanceof DebugPaintbrush) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
        }
    }

    @EventBusSubscriber(modid = "painter", value = Dist.CLIENT)
    public static class PickColorKeyHandler {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Pre event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.level == null) {
                return;
            }

            if (!pickingColor && KEY_PICK_COLOR.isDown()) {
                ItemStack brush = minecraft.player.getMainHandItem();
                if (brush.getItem() instanceof DebugPaintbrush) {
                    pickingColor = true;
                }
            }

            if (pickingColor) {
                ItemStack brush = minecraft.player.getMainHandItem();
                if (brush.getItem() instanceof DebugPaintbrush) {
                    pickColor(brush);
                }
            }

            if (pickingColor && !KEY_PICK_COLOR.isDown()) {
                ItemStack brush = minecraft.player.getMainHandItem();
                int slot = minecraft.player.getInventory().selected;
                if (brush.getItem() instanceof DebugPaintbrush && !brush.isEmpty()) {
                    PacketDistributor.sendToServer(new ItemSyncPacket(slot, brush));
                    pickingColor = false;
                }
            }
        }
    }
}
