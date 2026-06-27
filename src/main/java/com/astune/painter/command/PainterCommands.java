package com.astune.painter.command;

import com.astune.painter.network.CanvasStrokeHistory;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class PainterCommands {
    private PainterCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerRoot(dispatcher, "/pigmentum");
        registerRoot(dispatcher, "pigmentum");
    }

    private static void registerRoot(CommandDispatcher<CommandSourceStack> dispatcher, String root) {
        dispatcher.register(Commands.literal(root)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("undo")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> undo(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("redo")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> redo(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))));
    }

    private static int undo(CommandSourceStack source, ServerPlayer player) {
        boolean changed = CanvasStrokeHistory.undoLastStroke(source.getServer(), player.getUUID());
        source.sendSuccess(() -> Component.literal(changed
                ? "Undid last Pigmentum stroke for " + player.getGameProfile().getName()
                : "No Pigmentum stroke was undone for " + player.getGameProfile().getName()), true);
        return changed ? 1 : 0;
    }

    private static int redo(CommandSourceStack source, ServerPlayer player) {
        boolean changed = CanvasStrokeHistory.redoLastStroke(source.getServer(), player.getUUID());
        source.sendSuccess(() -> Component.literal(changed
                ? "Redid last Pigmentum stroke for " + player.getGameProfile().getName()
                : "No Pigmentum stroke was redone for " + player.getGameProfile().getName()), true);
        return changed ? 1 : 0;
    }
}
