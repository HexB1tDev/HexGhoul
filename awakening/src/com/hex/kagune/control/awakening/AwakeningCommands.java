package com.hex.kagune.control.awakening;

import com.hex.kagune.control.king.KingForm;
import com.hex.kagune.control.server.KingServer;
import com.hex.kagune.control.server.QuestServer;
import com.hex.kagune.data.KaguneProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /kagune awakening status|complete|trigger|reset [player]} ({@code complete} finishes the quest "Kakuja V2: The Dragon"). Everything but {@code status}
 * on yourself needs permission level 2. Merges into the existing {@code /kagune} root. 1.0.5: {@code preview <player> war|peace|off} puts a
 * final kakuja's look on a player (no stats, no lock) to see it in game; {@code crown <player> grant|reset} gives the crown as if the
 * Dragon had been through them (to choose in the menu), or forgets it.
 */
final class AwakeningCommands {
    private AwakeningCommands() {
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("kagune").then(Commands.literal("awakening")
                .then(Commands.literal("status")
                        .executes(context -> status(context, context.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).requires(source -> source.hasPermission(2))
                                .executes(context -> status(context, EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("complete").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player()).executes(AwakeningCommands::complete)))
                .then(Commands.literal("trigger").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player()).executes(AwakeningCommands::trigger)))
                .then(Commands.literal("reset").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player()).executes(AwakeningCommands::reset)))
                .then(Commands.literal("preview").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("war").executes(context -> preview(context, KingForm.WAR)))
                                .then(Commands.literal("peace").executes(context -> preview(context, KingForm.PEACE)))
                                .then(Commands.literal("off").executes(context -> preview(context, KingForm.NONE)))))
                .then(Commands.literal("crown").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("grant").executes(context -> crown(context, true)))
                                .then(Commands.literal("reset").executes(context -> crown(context, false)))))));
    }

    /** 1.0.5: a final kakuja's look on a player, or off. */
    private static int preview(CommandContext<CommandSourceStack> context, KingForm form) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        KingServer.preview(player, form);
        String name = player.getGameProfile().getName();
        context.getSource().sendSuccess(() -> Component.literal(form == KingForm.NONE ? "Preview off for " + name
                : form.displayName() + " (preview: the look alone) on " + name), true);
        return 1;
    }

    /** 1.0.5: the crown given (as after the Dragon) or forgotten. */
    private static int crown(CommandContext<CommandSourceStack> context, boolean grant) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        if (grant) {
            KingServer.awaken(player);
        } else {
            KingServer.reset(player);
        }
        String name = player.getGameProfile().getName();
        context.getSource().sendSuccess(() -> Component.literal((grant ? "Crown given to " : "Crown forgotten for ") + name), true);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        AwakeningStage stage = AwakeningProfile.stage(player);
        String text = player.getGameProfile().getName() + ": " + stage.name().toLowerCase()
                + " | Rinkaku " + (AwakeningEvents.rinkakuPath(player) ? "yes" : "no") + " | mastery " + KaguneProfile.mastery(player)
                + " | quest " + AwakeningQuest.TITLE + " " + (AwakeningEvents.gateOpen(player) ? "DONE" : "not done (/kagune quests)")
                + " | final kakuja " + (FinalKakuja.isUnlocked(player) ? "UNLOCKED" : "locked") + " (" + KingServer.chosen(player).displayName() + ")"
                + (DragonSessions.isAwakening(player.getUUID()) ? " | dragon tick " + DragonSessions.tickOf(player.getUUID()) : "");
        context.getSource().sendSuccess(() -> Component.literal(text), false);
        return stage.ordinal();
    }

    /** Finishes the quest "Kakuja V2: The Dragon" (as {@code /kagune quests complete the_dragon}): the next death wakes the Dragon. */
    private static int complete(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        AwakeningStage stage = AwakeningProfile.stage(player);
        if (stage == AwakeningStage.DRAGON || stage == AwakeningStage.FINAL) {
            context.getSource().sendFailure(Component.literal("The Dragon has already awakened for " + player.getGameProfile().getName()));
            return 0;
        }
        if (!AwakeningEvents.rinkakuPath(player)) {
            context.getSource().sendFailure(Component.literal(player.getGameProfile().getName() + " is not on Rinkaku: the Dragon is Rinkaku's"));
            return 0;
        }
        if (!AwakeningEvents.gateOpen(player)) QuestServer.force(player, AwakeningQuest.QUEST_ID);
        AwakeningEvents.progress(player);
        context.getSource().sendSuccess(() -> Component.literal("Awakening armed for " + player.getGameProfile().getName()), true);
        return 1;
    }

    /** Starts the Dragon immediately, as if the player had just died. For testing the sequence. */
    private static int trigger(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        if (!DragonSessions.start(player)) {
            context.getSource().sendFailure(Component.literal("The Dragon cannot awaken for " + player.getGameProfile().getName() + " right now"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Dragon awakened for " + player.getGameProfile().getName()), true);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        DragonSessions.end(player.getUUID());
        DragonSessions.forget(player.getUUID());
        AwakeningProfile.reset(player);
        KingServer.reset(player);
        AwakeningNetwork.sendStatus(player);
        context.getSource().sendSuccess(() -> Component.literal("Awakening reset for " + player.getGameProfile().getName()), true);
        return 1;
    }
}
