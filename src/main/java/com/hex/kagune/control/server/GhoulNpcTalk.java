package com.hex.kagune.control.server;

import com.hex.kagune.control.ControlItems;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CNpcTalk;
import com.hex.kagune.control.npc.GhoulNpc;
import com.hex.kagune.control.npc.NpcBonds;
import com.hex.kagune.control.npc.NpcIdentity;
import com.hex.kagune.control.npc.NpcTalk;
import com.hex.kagune.control.quest.Favours;
import com.hex.kagune.control.quest.QuestLog;
import com.hex.kagune.control.quest.Signal;
import com.hex.kagune.control.quest.Tags;
import com.hex.kagune.data.KaguneProfile;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 7.0.2: talking with ghoul NPCs, on the server. A right click on one opens a conversation if it would talk to the player (its temper
 * and how it regards them: {@link NpcTalk}); each answer is checked against what they were offered, then carried out: the regard it
 * changes (kept in the player's quest log), the favour taken on ({@link Favours}), the Ghoul Meat handed over, the ally it becomes or
 * stops being, the fight it starts, the run it takes. The quests hear of it ({@link Signal#TALK}, {@link Signal#BEFRIEND}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class GhoulNpcTalk {
    /** How near a player must be to talk (blocks), and to keep talking. */
    static final double REACH = 6.0;
    static final double KEEP = 9.0;

    /** Who each player is talking to and the answers they were offered (1.3.29: and what each reply slot means: a scene's keys). */
    private record Talk(int npc, List<NpcTalk.Choice> offered, List<String> keys) {
    }

    private static final Map<UUID, Talk> TALKS = new HashMap<>();

    private GhoulNpcTalk() {
    }

    /** A right click on a ghoul NPC: nothing in the hand is used on it (either side); on the server, the talk opens. */
    @SubscribeEvent
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof GhoulNpc npc)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() == InteractionHand.MAIN_HAND && event.getEntity() instanceof ServerPlayer player && npc.m_20280_(player) <= REACH * REACH) {
            open(player, npc);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        TALKS.remove(event.getEntity().m_20148_());
    }

    /** 1.3.29: true while {@code player} has a talk with a ghoul open. */
    static boolean talking(ServerPlayer player) {
        return TALKS.containsKey(player.m_20148_());
    }

    static void open(ServerPlayer player, GhoulNpc npc) {
        NpcIdentity who = npc.identity();
        GhoulNpcBrain.Mind mind = npc.mind;
        long now = player.m_9236_().m_46467_();
        if (who == null || mind == null || !npc.m_6084_()) {
            return;
        }
        if (!GhoulNpcBrain.talks(npc, who, mind, player, now)) {
            player.m_5661_(Component.m_237113_(who.displayName() + " is in no mood to talk."), true);
            return;
        }
        String key = npc.m_20148_().toString();
        QuestLog.Bond bond = QuestServer.bond(player, key);
        String temper = Tags.temper(who.temper().name().toLowerCase(Locale.ROOT));
        if (bond.has(NpcBonds.MET)) {
            QuestServer.signal(player, Signal.TALK, 1, temper);
        } else {
            QuestServer.bond(player, key, bond.with(NpcBonds.MET, true));
            QuestServer.signal(player, Signal.TALK, 1, temper, Tags.FIRST);
        }
        befriended(player, who, key, QuestServer.bond(player, key)); // a favour done while apart may have made them friends
        GhoulNpcBrain.listen(npc, mind, player, now);
        NpcTalk.Context context = context(player, npc, who);
        // 1.3.29: it came up to them with something: the talk opens on it.
        SceneServer.Pending pending = SceneServer.take(player, npc.m_19879_());
        com.hex.kagune.control.npc.scene.Scene scene = pending == null ? null : SceneServer.scene(pending.scene());
        if (scene != null) {
            scene(player, npc, who, context, scene, scene.first(), SceneServer.context(player, npc, now), pending.line());
            return;
        }
        main(player, npc, who, context, NpcTalk.open(context).line());
    }

    /** 1.3.29: its first page: its own answers, and (one who walks with them) something to talk about. */
    private static void main(ServerPlayer player, GhoulNpc npc, NpcIdentity who, NpcTalk.Context context, String line) {
        List<NpcTalk.Choice> choices = new java.util.ArrayList<>(NpcTalk.choices(context));
        List<String> labels = new java.util.ArrayList<>();
        String[] keys = new String[NpcTalk.Choice.SAYS.length];
        java.util.Arrays.fill(keys, "");
        for (int i = 0; i < choices.size(); i++) {
            labels.add("");
        }
        // 1.3.29: hunts come only when asked for (never at random at dusk): a ghoul who talks to them knows where the prey is tonight.
        if (QuestServer.huntable(player) && !choices.contains(NpcTalk.Choice.SAY_7)) {
            int at = Math.max(0, choices.indexOf(NpcTalk.Choice.LEAVE));
            choices.add(at, NpcTalk.Choice.SAY_7);
            labels.add(at, "Any prey out tonight?");
            keys[6] = "hunt";
        }
        if (context.ally() && !SceneServer.allyTopics(player, npc, player.m_9236_().m_46467_()).isEmpty()) {
            int at = Math.max(0, choices.indexOf(NpcTalk.Choice.LEAVE));
            choices.add(at, NpcTalk.Choice.SAY_8);
            labels.add(at, "Let's talk.");
            keys[7] = "topics";
        }
        boolean any = false;
        for (String key : keys) {
            any |= !key.isEmpty();
        }
        send(player, npc, who, context, line, choices, labels, any ? java.util.Arrays.asList(keys) : List.of());
    }

    /** 1.3.29: a scene's page ({@code line}: what it says; null, the page's own), each answer keyed to it; a topic's has "back". */
    private static void scene(ServerPlayer player, GhoulNpc npc, NpcIdentity who, NpcTalk.Context context, com.hex.kagune.control.npc.scene.Scene scene,
                              com.hex.kagune.control.npc.scene.Scene.Page page, com.hex.kagune.control.npc.scene.SceneContext sc, String line) {
        List<NpcTalk.Choice> choices = new java.util.ArrayList<>();
        List<String> labels = new java.util.ArrayList<>();
        List<String> keys = new java.util.ArrayList<>();
        for (int i = 0; i < page.options().size() && i < NpcTalk.Choice.SAYS.length - 1; i++) {
            choices.add(NpcTalk.Choice.SAYS[i]);
            labels.add(com.hex.kagune.control.npc.scene.SceneBook.fill(page.options().get(i).label(), sc));
            keys.add("s:" + scene.key() + ":" + page.id() + ":" + i);
        }
        if (scene.kind() == com.hex.kagune.control.npc.scene.Scene.Kind.TOPIC) {
            choices.add(NpcTalk.Choice.SAYS[keys.size()]);
            labels.add("...");
            keys.add("topics");
        }
        choices.add(NpcTalk.Choice.LEAVE);
        labels.add("");
        String said = line != null ? line
            : com.hex.kagune.control.npc.scene.SceneBook.fill(com.hex.kagune.control.npc.scene.SceneBook.one(page.lines(), sc.seed()), sc);
        send(player, npc, who, context, said, choices, labels, keys);
    }

    /** 1.3.29: what can be talked about with it now (its topics, a different few each time), and back. */
    private static void topics(ServerPlayer player, GhoulNpc npc, NpcIdentity who, NpcTalk.Context context, String line) {
        long now = player.m_9236_().m_46467_();
        List<com.hex.kagune.control.npc.scene.Scene> all = new java.util.ArrayList<>(SceneServer.allyTopics(player, npc, now));
        java.util.Collections.shuffle(all, new java.util.Random(npc.m_217043_().m_188503_(100000)));
        com.hex.kagune.control.npc.scene.SceneContext sc = SceneServer.context(player, npc, now);
        List<NpcTalk.Choice> choices = new java.util.ArrayList<>();
        List<String> labels = new java.util.ArrayList<>();
        List<String> keys = new java.util.ArrayList<>();
        for (com.hex.kagune.control.npc.scene.Scene scene : all) {
            if (keys.size() >= 6) {
                break;
            }
            choices.add(NpcTalk.Choice.SAYS[keys.size()]);
            labels.add(com.hex.kagune.control.npc.scene.SceneBook.fill(scene.prompt(), sc));
            keys.add("t:" + scene.key());
        }
        choices.add(NpcTalk.Choice.SAYS[keys.size()]);
        labels.add("...");
        keys.add("back");
        choices.add(NpcTalk.Choice.LEAVE);
        labels.add("");
        send(player, npc, who, context, line, choices, labels, keys);
    }

    /** 1.3.29: the page sent with its own words on each answer and the keys behind its reply slots. */
    private static void send(ServerPlayer player, GhoulNpc npc, NpcIdentity who, NpcTalk.Context context, String line, List<NpcTalk.Choice> choices,
                             List<String> labels, List<String> keys) {
        TALKS.put(player.m_20148_(), new Talk(npc.m_19879_(), List.copyOf(choices), List.copyOf(keys)));
        int[] ordinals = new int[choices.size()];
        String[] shown = new String[choices.size()];
        for (int i = 0; i < ordinals.length; i++) {
            ordinals[i] = choices.get(i).ordinal();
            shown[i] = i < labels.size() ? labels.get(i) : "";
        }
        ControlNetwork.sendTo(player, new S2CNpcTalk(npc.m_19879_(), who.displayName(), who.temper().title(), context.regard(), context.ally(), who.skin(),
            line, ordinals, shown));
    }

    /** 1.3.29: a reply slot with a key behind it: a scene's answer, a topic, the topics page, back to its first page. */
    private static void keyed(ServerPlayer player, GhoulNpc npc, NpcIdentity who, String key) {
        long now = player.m_9236_().m_46467_();
        NpcTalk.Context context = context(player, npc, who);
        if (key.equals("topics")) {
            topics(player, npc, who, context, "...");
            return;
        }
        if (key.equals("back")) {
            main(player, npc, who, context, "...");
            return;
        }
        if (key.equals("hunt")) {
            // 1.3.29: tonight's hunt, asked for.
            com.hex.kagune.control.quest.Quest hunt = QuestServer.askHunt(player);
            String said = hunt == null ? "Nothing worth the trouble tonight. Ask me another night."
                : "Tonight? " + hunt.title() + ". " + hunt.objectives().get(0).text() + ", " + hunt.objectives().get(0).count() + " of them. Before dawn. Don't get greedy.";
            main(player, npc, who, context(player, npc, who), said);
            return;
        }
        if (key.startsWith("t:")) {
            com.hex.kagune.control.npc.scene.Scene scene = SceneServer.scene(key.substring(2));
            if (scene != null) {
                scene(player, npc, who, context, scene, scene.first(), SceneServer.context(player, npc, now), null);
            }
            return;
        }
        SceneServer.AllyAnswer answer = SceneServer.allyAnswer(player, npc, key, now);
        if (answer == null) {
            main(player, npc, who, context, "...");
            return;
        }
        NpcTalk.Context after = context(player, npc, who);
        befriended(player, who, npc.m_20148_().toString(), QuestServer.bond(player, npc.m_20148_().toString()));
        if (answer.next() != null && !answer.option().does("close")) {
            scene(player, npc, who, after, answer.scene(), answer.next(), answer.context(), answer.line());
            return;
        }
        if (answer.option().does("close")) {
            send(player, npc, who, after, answer.line(), List.of(NpcTalk.Choice.LEAVE), List.of(""), List.of());
            return;
        }
        main(player, npc, who, after, answer.line());
    }

    /** The first time it counts {@code player} a friend: they are told, and the quests hear of it. The bond as it now is. */
    private static QuestLog.Bond befriended(ServerPlayer player, NpcIdentity who, String key, QuestLog.Bond bond) {
        if (bond.has(NpcBonds.BEFRIENDED) || bond.regard() < NpcBonds.FRIEND) {
            return bond;
        }
        QuestLog.Bond now = bond.with(NpcBonds.BEFRIENDED, true);
        QuestServer.bond(player, key, now);
        QuestServer.signal(player, Signal.BEFRIEND, 1, Tags.temper(who.temper().name().toLowerCase(Locale.ROOT)));
        player.m_5661_(Component.m_237113_(who.displayName() + " counts you a friend."), true);
        return now;
    }

    /** An answer from the talk screen: carried out if it was one they were offered, and the next page sent. */
    public static void heard(ServerPlayer player, int npcId, int ordinal) {
        Talk talk = TALKS.get(player.m_20148_());
        NpcTalk.Choice choice = NpcTalk.Choice.byOrdinal(ordinal);
        if (choice == NpcTalk.Choice.ANSWER) {
            // 1.3.29: the Answer key: the ghoul who walks with them came up with something.
            if (talk == null && player.m_284548_().m_6815_(npcId) instanceof GhoulNpc npc && npc.m_6084_() && npc.m_20280_(player) <= 24.0 * 24.0) {
                open(player, npc);
            }
            return;
        }
        if (talk == null || talk.npc() != npcId || choice != NpcTalk.Choice.LEAVE && !talk.offered().contains(choice)) {
            return;
        }
        ServerLevel level = player.m_284548_();
        double keep = talk.keys().isEmpty() ? KEEP : 16.0;
        if (!(level.m_6815_(npcId) instanceof GhoulNpc npc) || !npc.m_6084_() || npc.m_20280_(player) > keep * keep || npc.mind == null
            || npc.identity() == null) {
            TALKS.remove(player.m_20148_());
            return;
        }
        int slot = choice.say();
        if (slot >= 0 && slot < talk.keys().size() && !talk.keys().get(slot).isEmpty()) {
            keyed(player, npc, npc.identity(), talk.keys().get(slot));
            return;
        }
        NpcIdentity who = npc.identity();
        GhoulNpcBrain.Mind mind = npc.mind;
        long now = level.m_46467_();
        if (choice == NpcTalk.Choice.LEAVE) {
            TALKS.remove(player.m_20148_());
            GhoulNpcBrain.listen(npc, mind, null, now);
            return;
        }
        if (!GhoulNpcBrain.talks(npc, who, mind, player, now)) {
            TALKS.remove(player.m_20148_());
            return;
        }
        NpcTalk.Context context = context(player, npc, who);
        NpcTalk.Reply reply = NpcTalk.answer(context, choice);
        String key = npc.m_20148_().toString();
        String temper = Tags.temper(who.temper().name().toLowerCase(Locale.ROOT));
        QuestLog.Bond bond = QuestServer.bond(player, key);
        bond = bond.regard(bond.regard() + reply.regard());
        if (choice == NpcTalk.Choice.WHO) {
            bond = bond.with(NpcBonds.ASKED, true);
        }
        if (choice == NpcTalk.Choice.CHAT && !context.chattedToday()) {
            bond = bond.chatted(day(level));
        }
        switch (reply.effect()) {
            case GIFT -> {
                ItemStack held = ControlItems.ghoulMeat(player.m_21205_()) ? player.m_21205_() : player.m_21206_();
                if (ControlItems.ghoulMeat(held)) {
                    held.m_41774_(1);
                    UkakuFire.sound(level, npc.m_20182_(), "entity.generic.eat", 0.8F, 0.8F);
                }
            }
            case ALLY -> {
                GhoulNpcBrain.bind(npc, player);
                bond = bond.with(NpcBonds.ALLY, true);
                QuestServer.signal(player, Signal.BEFRIEND, 1, temper, Tags.ALLY);
            }
            case PART -> {
                GhoulNpcBrain.unbind(npc, now);
                bond = bond.with(NpcBonds.ALLY, false);
            }
            case FOLLOW -> GhoulNpcBrain.follow(npc, true);
            case STAY -> GhoulNpcBrain.follow(npc, false);
            case ANGER -> GhoulNpcBrain.provoke(npc, mind, player, who, now);
            case FLEE -> GhoulNpcBrain.scare(npc, mind, now);
            default -> {
            }
        }
        QuestServer.bond(player, key, bond);
        bond = befriended(player, who, key, bond);
        if (reply.effect() == NpcTalk.Effect.START_FAVOUR) {
            Favours.Spec spec = Favours.offer(who, bond.favours());
            if (spec != null) {
                QuestServer.startFavour(player, spec, key, who);
            }
        }
        NpcTalk.Context after = context(player, npc, who);
        List<NpcTalk.Choice> next = switch (reply.effect()) {
            case OFFER -> NpcTalk.offer();
            case CLOSE, ANGER, FLEE, PART -> List.of(NpcTalk.Choice.LEAVE);
            default -> NpcTalk.choices(after);
        };
        if (reply.effect() == NpcTalk.Effect.ANGER || reply.effect() == NpcTalk.Effect.FLEE || reply.effect() == NpcTalk.Effect.PART) {
            GhoulNpcBrain.listen(npc, mind, null, now);
        }
        show(player, npc, who, after, reply.line(), next);
    }

    /** What the next page depends on, as it stands now. */
    static NpcTalk.Context context(ServerPlayer player, GhoulNpc npc, NpcIdentity who) {
        String key = npc.m_20148_().toString();
        QuestLog log = QuestServer.log(player);
        QuestLog.Bond bond = log.bond(key);
        boolean ghoul = KaguneProfile.isUnlocked(player);
        // A favour is a quest: only a ghoul keeps a journal of them.
        Favours.Spec spec = ghoul ? Favours.offer(who, bond.favours()) : null;
        boolean ally = GhoulNpcBrain.allyOf(npc, player);
        boolean meat = ControlItems.ghoulMeat(player.m_21205_()) || ControlItems.ghoulMeat(player.m_21206_());
        return new NpcTalk.Context(who, bond.regard(), ghoul, GhoulNpcBrain.side(player), ally, ally && GhoulNpcBrain.following(npc),
            bond.chatted() == day(player.m_284548_()), bond.has(NpcBonds.ASKED), meat, log.favourFrom(key) != null,
            log.favours().size() < NpcBonds.MOST_FAVOURS, spec == null ? null : Favours.asking(spec, who),
            GhoulNpcBrain.allies(player) < NpcBonds.MOST_ALLIES);
    }

    /** The page sent: who it is, how it regards them, what it says and what they can answer. */
    private static void show(ServerPlayer player, GhoulNpc npc, NpcIdentity who, NpcTalk.Context context, String line, List<NpcTalk.Choice> choices) {
        TALKS.put(player.m_20148_(), new Talk(npc.m_19879_(), List.copyOf(choices), List.of()));
        int[] ordinals = new int[choices.size()];
        for (int i = 0; i < ordinals.length; i++) {
            ordinals[i] = choices.get(i).ordinal();
        }
        ControlNetwork.sendTo(player, new S2CNpcTalk(npc.m_19879_(), who.displayName(), who.temper().title(), context.regard(), context.ally(), who.skin(),
            line, ordinals));
    }

    /** The game day (for a chat a night). */
    private static long day(ServerLevel level) {
        return Math.floorDiv(level.m_46468_(), 24000L);
    }
}
