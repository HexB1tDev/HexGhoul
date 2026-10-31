package com.hex.kagune.control.server;

import com.hex.kagune.control.ComboStyle;
import com.hex.kagune.control.ControlMath;
import com.hex.kagune.control.UrgeType;
import com.hex.kagune.data.KaguneProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * {@code /kagune control} shows kagune control progress. Operators also get test hooks:
 * {@code /kagune control frenzy <0-1>} and {@code /kagune control urge <type>}. 6.2.0: {@code /kagune cooldowns off|on [player]}
 * ({@link Cooldowns}: off, every ability, kakuja and mutation is ready again the moment it is over).
 * Registered as children of the existing {@code /kagune} literal; Brigadier merges the nodes.
 */
final class ControlCommands {
    private ControlCommands() {
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.m_82127_("kagune").then(Commands.m_82127_("control")
            .executes(ControlCommands::report)
            .then(Commands.m_82127_("frenzy").requires(source -> source.m_6761_(2))
                .then(Commands.m_82129_("value", FloatArgumentType.floatArg(0.0F, 1.0F)).executes(ControlCommands::setFrenzy)))
            .then(Commands.m_82127_("urge").requires(source -> source.m_6761_(2))
                .then(Commands.m_82129_("type", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.m_82970_(List.of("glance", "step", "lunge", "strike"), builder))
                    .executes(ControlCommands::forceUrge)))));
        // 6.2.0: cooldowns off (everything ready again the moment it is over) and back on, for yourself or a player.
        dispatcher.register(Commands.m_82127_("kagune").then(Commands.m_82127_("cooldowns").requires(source -> source.m_6761_(2))
            .executes(ControlCommands::reportCooldowns)
            .then(Commands.m_82127_("off").executes(context -> cooldowns(context, true, context.getSource().m_81375_()))
                .then(Commands.m_82129_("player", EntityArgument.m_91466_())
                    .executes(context -> cooldowns(context, true, EntityArgument.m_91474_(context, "player")))))
            .then(Commands.m_82127_("on").executes(context -> cooldowns(context, false, context.getSource().m_81375_()))
                .then(Commands.m_82129_("player", EntityArgument.m_91466_())
                    .executes(context -> cooldowns(context, false, EntityArgument.m_91474_(context, "player")))))));
        // 3.1.0: the abilities board above the chat, on and off (each player's own, kept in their client config).
        dispatcher.register(Commands.m_82127_("kagune").then(Commands.m_82127_("hud").then(Commands.m_82127_("toggle")
            .executes(ControlCommands::toggleHud))));
        // 1.0.11: /kagune quest complete all (or one quest by id), the singular spelling.
        dispatcher.register(Commands.m_82127_("kagune").then(Commands.m_82127_("quest")
            .then(Commands.m_82127_("complete").requires(source -> source.m_6761_(2))
                .then(Commands.m_82129_("quest", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.m_82970_(List.of("all"), builder))
                    .executes(ControlCommands::completeQuest)))));
        // 7.0.1: the quests: what is open and how far along; an operator can reset, finish, open a gate, or call a hunt.
        dispatcher.register(Commands.m_82127_("kagune").then(Commands.m_82127_("quests")
            .executes(ControlCommands::reportQuests)
            .then(Commands.m_82127_("reset").requires(source -> source.m_6761_(2)).executes(context -> {
                QuestServer.reset(context.getSource().m_81375_());
                context.getSource().m_288197_(() -> Component.m_237113_("Quests reset: the new progression from the start."), false);
                return 1;
            }))
            .then(Commands.m_82127_("complete").requires(source -> source.m_6761_(2))
                .then(Commands.m_82129_("quest", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.m_82970_(List.of("all"), builder))
                    .executes(ControlCommands::completeQuest)))
            .then(Commands.m_82127_("gate").requires(source -> source.m_6761_(2))
                .then(Commands.m_82129_("gate", StringArgumentType.word()).executes(context -> {
                    com.hex.kagune.control.quest.Gate gate = com.hex.kagune.control.quest.Gate.fromId(StringArgumentType.getString(context, "gate"));
                    if (gate == null) {
                        context.getSource().m_81352_(Component.m_237113_("No such gate."));
                        return 0;
                    }
                    QuestServer.grant(context.getSource().m_81375_(), gate);
                    context.getSource().m_288197_(() -> Component.m_237113_("Opened: " + gate.displayName()), false);
                    return 1;
                })))
            .then(Commands.m_82127_("hunt").requires(source -> source.m_6761_(2)).executes(context -> {
                QuestServer.hunt(context.getSource().m_81375_());
                return 1;
            }))));
        // 1.3.1: Eto's regard for a player set to the most (a testing shortcut for operators).
        dispatcher.register(Commands.m_82127_("kagune").then(Commands.m_82127_("eto").requires(source -> source.m_6761_(2))
            .then(Commands.m_82127_("max").executes(context -> maxEto(context, context.getSource().m_81375_()))
                .then(Commands.m_82129_("player", EntityArgument.m_91466_())
                    .executes(context -> maxEto(context, EntityArgument.m_91474_(context, "player")))))));
        // 1.3.10: a testing shortcut for operators: every pregnancy the player is part of brought to ten seconds before the birth.
        dispatcher.register(Commands.m_82127_("kagune").then(Commands.m_82127_("family").requires(source -> source.m_6761_(2))
            .then(Commands.m_82127_("soon").executes(context -> familySoon(context, context.getSource().m_81375_())))));
        // 7.0.1: Ghoul Meat devoured: what it gives now; an operator can set the count.
        dispatcher.register(Commands.m_82127_("kagune").then(Commands.m_82127_("ghoulmeat")
            .executes(ControlCommands::reportGhoulMeat)
            .then(Commands.m_82127_("set").requires(source -> source.m_6761_(2))
                .then(Commands.m_82129_("count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 100000))
                    .executes(ControlCommands::setGhoulMeat)))));
        // 7.0.2: a ghoul NPC five blocks ahead (an operator's: to meet one without waiting for the night), its kagune, temper and tier
        // as asked (rinkaku/ukaku; feral, gourmet, pack, drifter, scavenger, anteiku; 0 to 2), else rolled.
        dispatcher.register(Commands.m_82127_("kagune").then(Commands.m_82127_("npc").requires(source -> source.m_6761_(2))
            .executes(context -> spawnNpc(context, null, null, -1))
            .then(Commands.m_82129_("kind", StringArgumentType.word())
                .executes(context -> spawnNpc(context, StringArgumentType.getString(context, "kind"), null, -1))
                .then(Commands.m_82129_("temper", StringArgumentType.word())
                    .executes(context -> spawnNpc(context, StringArgumentType.getString(context, "kind"), StringArgumentType.getString(context, "temper"), -1))
                    .then(Commands.m_82129_("tier", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 2))
                        .executes(context -> spawnNpc(context, StringArgumentType.getString(context, "kind"), StringArgumentType.getString(context, "temper"),
                            com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "tier"))))))));
    }

    private static int spawnNpc(CommandContext<CommandSourceStack> context, String kind, String temper, int tier) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().m_81375_();
        com.hex.kagune.control.npc.NpcIdentity rolled = com.hex.kagune.control.npc.NpcIdentity.roll(KaguneProfile.mastery(player),
            () -> player.m_217043_().m_188500_());
        com.hex.kagune.control.npc.NpcIdentity.Kind k = rolled.kind();
        if (kind != null) {
            k = kind.equalsIgnoreCase("ukaku") ? com.hex.kagune.control.npc.NpcIdentity.Kind.UKAKU : com.hex.kagune.control.npc.NpcIdentity.Kind.RINKAKU;
        }
        com.hex.kagune.control.npc.NpcTemper t = rolled.temper();
        if (temper != null) {
            for (com.hex.kagune.control.npc.NpcTemper each : com.hex.kagune.control.npc.NpcTemper.values()) {
                if (each.title().equalsIgnoreCase(temper)) {
                    t = each;
                }
            }
        }
        com.hex.kagune.control.npc.NpcIdentity who = new com.hex.kagune.control.npc.NpcIdentity(k, t, tier < 0 ? rolled.tier() : tier, rolled.skin(),
            rolled.name(), rolled.seed());
        net.minecraft.world.phys.Vec3 ahead = UkakuFire.horizontal(player.m_20154_()).m_82490_(5.0);
        com.hex.kagune.control.npc.GhoulNpc npc = GhoulNpcSpawner.place(player.m_284548_(), player.m_20185_() + ahead.f_82479_,
            player.m_20189_() + ahead.f_82481_, who);
        String said = npc == null ? "No room for a ghoul there." : "A ghoul: " + who.displayName() + " (" + who.kind().name().toLowerCase(java.util.Locale.ROOT)
            + ", tier " + who.tier() + ").";
        context.getSource().m_288197_(() -> Component.m_237113_(said), false);
        return npc == null ? 0 : 1;
    }

    /** {@code complete <quest>}: one quest by its id, or 1.0.11 {@code all}: every quest on right now. */
    private static int completeQuest(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = StringArgumentType.getString(context, "quest");
        ServerPlayer player = context.getSource().m_81375_();
        if (id.equalsIgnoreCase("all")) {
            List<String> done = QuestServer.completeOpen(player);
            String said = done.isEmpty() ? "No quests on right now." : "Completed " + done.size() + (done.size() == 1 ? " quest: " : " quests: ")
                + String.join(", ", done) + ".";
            context.getSource().m_288197_(() -> Component.m_237113_(said), false);
            return done.size();
        }
        boolean done = QuestServer.force(player, id);
        if (!done) {
            context.getSource().m_81352_(Component.m_237113_("No quest " + id + "."));
        }
        return done ? 1 : 0;
    }

    private static int reportQuests(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().m_81375_();
        com.hex.kagune.control.quest.QuestLog log = QuestServer.log(player);
        com.hex.kagune.control.KagunePath path = PathServer.path(player);
        int mastery = KaguneProfile.mastery(player);
        int open = 0;
        StringBuilder text = new StringBuilder("Open quests:");
        for (com.hex.kagune.control.quest.Quest quest : log.quests()) {
            if (log.state(quest, path, mastery) != com.hex.kagune.control.quest.QuestLog.State.OPEN) {
                continue;
            }
            open++;
            text.append("\n ").append(quest.title()).append(" [").append(quest.id()).append("]");
            for (int i = 0; i < quest.objectives().size(); i++) {
                com.hex.kagune.control.quest.Objective objective = quest.objectives().get(i);
                text.append("\n   ").append(objective.text()).append(" ").append(log.progress(quest, i)).append("/").append(objective.count());
            }
        }
        if (open == 0) {
            text.append(" none right now.");
        }
        String shown = text.toString();
        context.getSource().m_288197_(() -> Component.m_237113_(shown), false);
        return open;
    }

    private static int reportGhoulMeat(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().m_81375_();
        int eaten = GhoulMeatServer.eaten(player);
        context.getSource().m_288197_(() -> Component.m_237113_("Ghoul meat devoured: " + eaten + " (strength "
            + com.hex.kagune.control.GhoulMeat.percent(com.hex.kagune.control.GhoulMeat.strength(eaten)) + ", RC lasts "
            + com.hex.kagune.control.GhoulMeat.times(com.hex.kagune.control.GhoulMeat.endurance(eaten)) + ")"), false);
        return eaten;
    }

    private static int setGhoulMeat(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().m_81375_();
        GhoulMeatServer.setEaten(player, com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "count"));
        return reportGhoulMeat(context);
    }

    private static int cooldowns(CommandContext<CommandSourceStack> context, boolean off, ServerPlayer player) {
        Cooldowns.set(player, off);
        String text = off ? "Cooldowns off: every ability, kakuja and mutation is ready again the moment it is over."
            : "Cooldowns back on.";
        player.m_5661_(Component.m_237113_(text), false);
        ServerPlayer self = null;
        try {
            self = context.getSource().m_81375_();
        } catch (CommandSyntaxException console) {
            // run from the console or a command block
        }
        if (self != player) {
            String name = player.m_36316_().getName();
            context.getSource().m_288197_(() -> Component.m_237113_("Cooldowns " + (off ? "off" : "on") + " for " + name + "."), true);
        }
        return 1;
    }

    private static int reportCooldowns(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().m_81375_();
        boolean off = Cooldowns.switchedOff(player);
        context.getSource().m_288197_(() -> Component.m_237113_(off ? "Your cooldowns are off (/kagune cooldowns on to bring them back)."
            : "Your cooldowns are on (/kagune cooldowns off to skip them)."), false);
        return off ? 0 : 1;
    }

    private static int toggleHud(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().m_81375_();
        com.hex.kagune.control.network.ControlNetwork.sendTo(player, new com.hex.kagune.control.network.S2CAbilityHud());
        context.getSource().m_288197_(() -> Component.m_237113_("Ability board toggled."), false);
        return 1;
    }

    private static int report(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().m_81375_();
        float control = ControlServer.control(player);
        float hunger = ControlServer.hunger(player);
        float frenzy = ControlServer.frenzy(player);
        float instinct = ControlMath.instinct(control, hunger, frenzy);
        ComboStyle style = UkakuServer.isUkaku(player) ? UkakuServer.comboStyle(player, player.m_9236_().m_46467_())
            : ComboStyle.forForm(KaguneProfile.form(player));
        String text = String.format(Locale.ROOT,
            "Kagune control %d%% (mastery %d, restraint %d) | hunger %d%% | bloodlust %d%% | instinct %d%%%s | combat style %s",
            Math.round(control * 100.0F), KaguneProfile.mastery(player), KaguneProfile.actionMastery(player, ControlServer.RESTRAINT),
            Math.round(hunger * 100.0F), Math.round(frenzy * 100.0F), Math.round(instinct * 100.0F),
            instinct <= ControlMath.URGE_THRESHOLD ? " (suppressed)" : "", style.name().toLowerCase(Locale.ROOT));
        context.getSource().m_288197_(() -> Component.m_237113_(text), false);
        return Math.round(control * 100.0F);
    }

    private static int setFrenzy(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().m_81375_();
        float value = FloatArgumentType.getFloat(context, "value");
        ServerControlState state = ControlServer.state(player);
        state.frenzy = ControlMath.clamp01(value);
        state.dirty = true;
        context.getSource().m_288197_(() -> Component.m_237113_("Kagune bloodlust set to " + Math.round(value * 100.0F) + "%"), false);
        return 1;
    }

    private static int forceUrge(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().m_81375_();
        UrgeType type;
        try {
            type = UrgeType.valueOf(StringArgumentType.getString(context, "type").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            type = UrgeType.NONE;
        }
        if (type == UrgeType.NONE) {
            context.getSource().m_81352_(Component.m_237113_("Use glance, step, lunge or strike."));
            return 0;
        }
        LivingEntity target = KaguneInstinct.forceUrge(player, type);
        if (target == null) {
            context.getSource().m_81352_(Component.m_237113_("No manifested kagune, or no valid prey within 10 blocks in view."));
            return 0;
        }
        String name = type.name().toLowerCase(Locale.ROOT);
        context.getSource().m_288197_(() -> Component.m_237113_("Forced a " + name + " instinct urge toward the nearest prey"), false);
        return 1;
    }

    /** Eto's regard for the player at the most, met (any grudge from killing her forgotten): only with her within 10 blocks of them. */
    private static int familySoon(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        int hurried = Pregnancies.hurry(player.m_284548_(), Pregnancies.player(player.m_20148_()), 200L);
        if (hurried == 0) {
            context.getSource().m_81352_(Component.m_237113_("No one is carrying your child, and you aren't carrying one."));
            return 0;
        }
        context.getSource().m_288197_(() -> Component.m_237113_("The birth is ten seconds away."), false);
        return 1;
    }

    private static int maxEto(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        if (player.m_9236_().m_6443_(com.hex.kagune.control.npc.EtoNpc.class, player.m_20191_().m_82400_(10.0), eto -> eto.m_6084_()).isEmpty()) {
            context.getSource().m_81352_(Component.m_237113_("No Eto within 10 blocks."));
            return 0;
        }
        com.hex.kagune.control.quest.QuestLog.Bond bond = QuestServer.bond(player, com.hex.kagune.control.npc.EtoRules.KEY);
        QuestServer.bond(player, com.hex.kagune.control.npc.EtoRules.KEY,
            bond.regard(com.hex.kagune.control.npc.NpcBonds.MOST).with(com.hex.kagune.control.npc.NpcBonds.MET, true));
        context.getSource().m_288197_(() -> Component.m_237113_("Eto's regard is at the most (100)."), false);
        return 1;
    }
}
