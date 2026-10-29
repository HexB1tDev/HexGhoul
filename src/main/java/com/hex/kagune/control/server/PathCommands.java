package com.hex.kagune.control.server;

import com.hex.kagune.control.KagunePath;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /kagune path} shows your kagune path. Operators can assign one with
 * {@code /kagune path set <player> <kaneki|ayato|furuta|none>}; {@code none} sends the player back to the starting screen.
 * {@code /kagune reset <player>} (operators, 5.0.2) wipes everything kagune about a player, as if they had never picked one
 * ({@link PathServer#reset}).
 */
final class PathCommands {
    private PathCommands() {
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.m_82127_("kagune").then(Commands.m_82127_("path")
            .executes(PathCommands::report)
            .then(Commands.m_82127_("set").requires(source -> source.m_6761_(2))
                .then(Commands.m_82129_("player", EntityArgument.m_91466_())
                    .then(Commands.m_82129_("path", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.m_82970_(IDS, builder))
                        .executes(PathCommands::set))))
            // 6.2.0: "/kagune path furuta" (7.0.0; "owl" before): your own path, straight (operators only, as "set" is).
            .then(Commands.m_82129_("path", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.m_82970_(IDS, builder))
                .executes(PathCommands::setOwn))));
        // Merged into the original's "/kagune reset" (which resets yourself and keeps the path's own leftovers).
        dispatcher.register(Commands.m_82127_("kagune").then(Commands.m_82127_("reset").requires(source -> source.m_6761_(2))
            .then(Commands.m_82129_("player", EntityArgument.m_91466_()).executes(PathCommands::reset))));
    }

    private static int reset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.m_91474_(context, "player");
        PathServer.reset(player);
        player.m_5661_(Component.m_237115_("message.hexkagune.path.reset"), false);
        String name = player.m_36316_().getName();
        context.getSource().m_288197_(() -> Component.m_237113_(name + "'s kagune is reset: no path, mastery 0, no mutations, eyes unplaced."), true);
        return 1;
    }

    private static int report(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().m_81375_();
        KagunePath path = PathServer.path(player);
        context.getSource().m_288197_(() -> path.chosen()
            ? Component.m_237110_("message.hexkagune.path.current", path.displayName())
            : Component.m_237115_("message.hexkagune.path.undecided"), false);
        return path.ordinal();
    }

    /**
     * The path ids the commands take. 7.0.0: Owl DNA is dormant; its ids ("owl", "eto") still parse, unlisted, and put the player
     * on Furuta DNA, the path that replaced it.
     */
    static final List<String> IDS = List.of("kaneki", "ayato", "furuta", "none");

    private static int setOwn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (!context.getSource().m_6761_(2)) {
            context.getSource().m_81352_(Component.m_237113_("Only an operator can set a kagune path."));
            return 0;
        }
        return set(context, context.getSource().m_81375_());
    }

    private static int set(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return set(context, EntityArgument.m_91474_(context, "player"));
    }

    private static int set(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        String id = StringArgumentType.getString(context, "path");
        KagunePath path = KagunePath.fromId(id);
        if (!path.chosen() && !"none".equals(id.trim().toLowerCase(java.util.Locale.ROOT))) {
            context.getSource().m_81352_(Component.m_237113_("Use kaneki, ayato, furuta or none."));
            return 0;
        }
        PathServer.setPath(player, path);
        player.m_5661_(path.chosen()
            ? Component.m_237110_("message.hexkagune.path.assigned", path.displayName())
            : Component.m_237115_("message.hexkagune.path.reset"), false);
        context.getSource().m_288197_(() -> Component.m_237113_(player.m_36316_().getName() + "'s DNA is now " + path.displayName()), true);
        return 1;
    }
}
