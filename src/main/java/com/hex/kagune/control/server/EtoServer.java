package com.hex.kagune.control.server;

import com.hex.kagune.control.ControlItems;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CCutscene;
import com.hex.kagune.control.network.S2CNpcTalk;
import com.hex.kagune.control.network.S2CPathPrompt;
import com.hex.kagune.control.npc.EtoGesture;
import com.hex.kagune.control.npc.EtoLines;
import com.hex.kagune.control.npc.EtoMood;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.EtoRules;
import com.hex.kagune.control.npc.EtoTalk;
import com.hex.kagune.control.npc.EtoTopics;
import com.hex.kagune.control.npc.GhoulNpc;
import com.hex.kagune.control.npc.NpcBonds;
import com.hex.kagune.control.npc.NpcMemory;
import com.hex.kagune.control.npc.NpcTalk;
import com.hex.kagune.control.npc.Romance;
import com.hex.kagune.control.npc.Script;
import com.hex.kagune.control.npc.UniqueNpc;
import com.hex.kagune.control.quest.Quest;
import com.hex.kagune.control.quest.QuestBook;
import com.hex.kagune.control.quest.QuestLog;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.network.KaguneNetwork;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 1.3.0: talking with Eto, on the server, on the ghoul NPCs' talk screen ({@link EtoTalk}). A right click opens it if she is not cross
 * with the player (an enemy of hers, or left hanging); each answer is checked against what they were offered, then carried out: her
 * regard (the player's bond {@link EtoRules#KEY}, a ghoul's or a human's), the book or flower handed over, a lesson taught (its quest
 * opens), her errands begun and counted (books, kills at night, a flower), dating, a kiss or a hug (she walks up), and her surgery
 * ({@link #surgery}). A player who kills her is her enemy from then on.
 *
 * <p>1.3.2: the talk is pages ({@link EtoTalk.Page}): the first, the topics and what is said back on them, her partner's sweet page,
 * the gifts. She belongs to one player for good once they date ({@link UniqueNpcs}): the ring marries them (the whole server is told),
 * the night together is a cutscene, and every other player gets her cold side. Cross with someone, she can be won back with a baby
 * animal on a lead (a puppy, a kitten...) or a letter. Anyone can learn from her: a ghoul through her lessons and her last trial, a
 * human through her pact; at the end she makes them one-eyed, gently for her partner, coldly for anyone else.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class EtoServer {
    static final double REACH = 6.0;
    static final double KEEP = 9.0;
    /** How far from her a baby animal on the player's lead counts as brought to her. */
    static final double PUPPY_NEAR = 8.0;

    /** What page a conversation is on. */
    enum Kind {
        MAIN, TALK, TOPIC, SWEET, GIFTS, OFFER, CLOSED
    }

    private record Talk(int npc, List<EtoTalk.Option> offered, Kind kind) {
    }

    private static final Map<UUID, Talk> TALKS = new HashMap<>();

    private EtoServer() {
    }

    @SubscribeEvent
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof EtoNpc npc)) {
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

    static void open(ServerPlayer player, EtoNpc npc) {
        long now = player.m_9236_().m_46467_();
        EtoBrain.Mind mind = npc.mind;
        if (!npc.m_6084_() || mind == null) {
            return;
        }
        if (mind.target != 0 && mind.target != player.m_19879_()) {
            player.m_5661_(Component.m_237113_(npc.persona().script().busy()), true);
            return;
        }
        QuestLog.Bond bond = QuestServer.bond(player, npc.persona().key());
        boolean enemy = !EtoTalk.talks(bond.regard());
        boolean cross = EtoBrain.ignoring(npc, player, now) || sulking(mind, player, now) || mind.grudges.containsKey(player.m_20148_())
            || mind.target == player.m_19879_();
        String line = null;
        if (enemy || cross) {
            // 1.3.2: won back with a little someone on a lead, or (cross, not her enemy) a letter.
            EtoTalk.Context c = context(player, npc);
            Mob puppy = puppy(player, npc);
            if (puppy != null) {
                give(puppy);
                if (enemy) {
                    QuestServer.bond(player, npc.persona().key(), bond.regard(Math.max(bond.regard(), 0)));
                } else {
                    QuestServer.bond(player, npc.persona().key(), bond.regard(bond.regard() + 10));
                }
                EtoBrain.forgive(npc, player);
                EtoBrain.gesture(mind, EtoGesture.PET, now);
                EtoBrain.giddy(npc, player, now);
                line = npc.persona().script().puppy(c, kind(puppy), true);
            } else if (!enemy && take(player, EtoRules::letter)) {
                QuestServer.bond(player, npc.persona().key(), bond.regard(bond.regard() + 6));
                EtoBrain.forgive(npc, player);
                EtoBrain.gesture(mind, EtoGesture.READ, now);
                line = npc.persona().script().letter(c, true);
            } else {
                player.m_5661_(Component.m_237113_(npc.persona().script().snub(npc.m_217043_().m_188503_(100), enemy)), true);
                return;
            }
            bond = QuestServer.bond(player, npc.persona().key());
        }
        if (!bond.has(NpcBonds.MET)) {
            QuestServer.bond(player, npc.persona().key(), bond.with(NpcBonds.MET, true));
        }
        migrate(player, npc);
        EtoBrain.listen(npc, player, now);
        EtoTalk.Context context = context(player, npc);
        NpcMemoryServer.set(player, npc.persona().key(), NpcMemory.SEEN, now);
        Script script = npc.persona().script();
        show(player, npc, context, script.main(context, line != null ? line : script.greeting(context)), Kind.MAIN);
    }

    private static boolean sulking(EtoBrain.Mind mind, ServerPlayer player, long now) {
        Long until = mind.sulk.get(player.m_20148_());
        return until != null && until > now;
    }

    /** 1.3.0's dating, kept in the player's bond alone, made the world's: she is theirs if she is no one else's yet. */
    private static void migrate(ServerPlayer player, EtoNpc npc) {
        QuestLog.Bond bond = QuestServer.bond(player, npc.persona().key());
        if (!bond.has(EtoRules.DATING)) {
            return;
        }
        ServerLevel level = player.m_284548_();
        UniqueNpc unique = UniqueNpcs.get(level, npc.persona().key());
        if (unique.stageOf(player.m_20148_()).together()) {
            return;
        }
        if (unique.claimedFrom(player.m_20148_()) || UniqueNpcs.spouseOf(level, player, npc.persona().key()) != null) {
            QuestServer.bond(player, npc.persona().key(), bond.with(EtoRules.DATING, false));
            return;
        }
        UniqueNpcs.pair(level, npc.persona().key(), player, Romance.Stage.DATING);
        NpcMemoryServer.set(player, npc.persona().key(), NpcMemory.SINCE, day(level));
    }

    /** An answer from the talk screen (routed here by {@code C2SNpcTalk} when the one talked to is her). */
    public static void heard(ServerPlayer player, int npcId, int ordinal) {
        Talk talk = TALKS.get(player.m_20148_());
        NpcTalk.Choice choice = NpcTalk.Choice.byOrdinal(ordinal);
        if (talk == null || talk.npc() != npcId) {
            return;
        }
        EtoTalk.Option option = null;
        for (EtoTalk.Option offered : talk.offered()) {
            if (offered.choice() == choice) {
                option = offered;
            }
        }
        if (option == null && choice != NpcTalk.Choice.LEAVE) {
            return;
        }
        ServerLevel level = player.m_284548_();
        if (!(level.m_6815_(npcId) instanceof EtoNpc npc) || !npc.m_6084_() || npc.m_20280_(player) > KEEP * KEEP || npc.mind == null) {
            TALKS.remove(player.m_20148_());
            return;
        }
        long now = level.m_46467_();
        if (choice == NpcTalk.Choice.LEAVE) {
            TALKS.remove(player.m_20148_());
            EtoBrain.listen(npc, null, now);
            return;
        }
        EtoBrain.listen(npc, player, now);
        EtoTalk.Context c = context(player, npc);
        Script script = npc.persona().script();
        String key = option.key();
        if (key.startsWith("topic:")) {
            EtoTopics.Topic topic = script.topicOf(key.substring(6));
            if (topic != null) {
                show(player, npc, c, script.topic(c, topic), Kind.TOPIC);
            }
            return;
        }
        if (key.startsWith("follow:")) {
            String[] parts = key.split(":");
            EtoTopics.Topic topic = script.topicOf(parts[1]);
            int index = Integer.parseInt(parts[2]);
            if (topic == null) {
                return;
            }
            EtoTalk.Reply reply = script.follow(c, topic, index);
            int bit = Math.min(13, script.topicIndex(topic)) * 4 + Math.min(3, index);
            carry(player, npc, c, choice, firstToday(player, npc, level, bit) ? reply : unpaid(reply), Kind.TALK);
            return;
        }
        if (key.startsWith("sweet:")) {
            String what = key.substring(6);
            int bit = 56 + Math.floorMod(what.hashCode(), 8);
            EtoTalk.Reply reply = script.sweetly(c, what);
            carry(player, npc, c, choice, firstToday(player, npc, level, bit) ? reply : unpaid(reply), Kind.SWEET);
            return;
        }
        if (key.equals("gifts")) {
            show(player, npc, c, script.gifts(c, script.giftsAsked(c.seed())), Kind.GIFTS);
            return;
        }
        if (choice == NpcTalk.Choice.PROPOSE) {
            propose(player, npc, c);
            return;
        }
        if (choice == NpcTalk.Choice.BACK && talk.kind() == Kind.TOPIC) {
            // Back from a topic: to the others.
            show(player, npc, c, script.talk(c, script.topicsAgain(c.seed())), Kind.TALK);
            return;
        }
        Kind after = talk.kind() == Kind.SWEET || talk.kind() == Kind.TALK ? talk.kind() : Kind.MAIN;
        carry(player, npc, c, choice, script.answer(c, choice), after);
    }

    private static EtoTalk.Reply unpaid(EtoTalk.Reply reply) {
        return new EtoTalk.Reply(reply.line(), Math.min(0, reply.regard()), reply.effect(), reply.gesture());
    }

    /** True the first time today {@code bit} of the talk is said (its regard paid once a day); marks it said. */
    private static boolean firstToday(ServerPlayer player, EtoNpc npc, ServerLevel level, int bit) {
        NpcMemory memory = NpcMemoryServer.of(player);
        long today = day(level);
        long said = memory.get(npc.persona().key(), NpcMemory.TALKED_DAY, -1L) == today ? memory.get(npc.persona().key(), NpcMemory.TOPICS, 0L) : 0L;
        boolean first = (said & 1L << bit) == 0L;
        memory.set(npc.persona().key(), NpcMemory.TALKED_DAY, today).set(npc.persona().key(), NpcMemory.TOPICS, said | 1L << bit);
        NpcMemoryServer.save(player);
        return first;
    }

    /** An answer carried out (her regard, what she does, what happens), then the page it leads to ({@code stay}: the page they were on). */
    private static void carry(ServerPlayer player, EtoNpc npc, EtoTalk.Context c, NpcTalk.Choice choice, EtoTalk.Reply reply, Kind stay) {
        ServerLevel level = player.m_284548_();
        long now = level.m_46467_();
        long day = day(level);
        QuestLog.Bond bond = QuestServer.bond(player, npc.persona().key());
        bond = bond.regard(bond.regard() + reply.regard());
        if (choice == NpcTalk.Choice.WHO) {
            bond = bond.with(NpcBonds.ASKED, true);
        }
        if (choice == NpcTalk.Choice.CHAT && !c.chattedToday()) {
            bond = bond.chatted(day);
        }
        if (reply.gesture() == EtoGesture.HUG || reply.gesture() == EtoGesture.CLING) {
            // 1.3.3: a hug or her arm round theirs: she comes to them for it (never a grab at the air from across the room).
            EtoBrain.walkUp(npc, npc.mind, player, reply.gesture(), now);
        } else if (reply.gesture() != EtoGesture.NONE) {
            EtoBrain.gesture(npc.mind, reply.gesture(), now);
        }
        if (reply.regard() >= 3 && c.dating()) {
            EtoBrain.giddy(npc, player, now);
            EtoBrain.hearts(level, npc, player, 3);
            RomanceServer.romanced(player, npc.persona().key());
        }
        Kind next = stay;
        String line = reply.line();
        switch (reply.effect()) {
            case BOOK, FLOWER -> {
                boolean isBook = reply.effect() == EtoTalk.Effect.BOOK;
                java.util.function.Predicate<String> gift = isBook ? npc.persona()::errandItem : npc.persona()::flower;
                bond = take(player, gift)
                    ? EtoRules.advance(EtoRules.gifted(bond, day), isBook ? EtoRules.Step.BOOKS : EtoRules.Step.FLOWER, 1)
                    : bond.regard(bond.regard() - reply.regard());
                next = Kind.MAIN;
            }
            case MEAT -> {
                if (takeStack(player, ControlItems::ghoulMeat)) {
                    bond = EtoRules.gifted(bond, day);
                    pact(player, EtoRules.Pact.MEAT, 1);
                } else {
                    bond = bond.regard(bond.regard() - reply.regard());
                }
                next = Kind.MAIN;
            }
            case LETTER -> {
                if (take(player, EtoRules::letter)) {
                    bond = EtoRules.gifted(bond, day);
                } else {
                    bond = bond.regard(bond.regard() - reply.regard());
                }
                next = Kind.MAIN;
            }
            case PUPPY -> {
                Mob puppy = puppy(player, npc);
                if (puppy != null) {
                    give(puppy);
                    NpcMemoryServer.set(player, npc.persona().key(), NpcMemory.PUPPY_DAY, day);
                    EtoBrain.giddy(npc, player, now);
                    EtoBrain.hearts(level, npc, puppy, 6);
                } else {
                    bond = bond.regard(bond.regard() - reply.regard());
                }
                next = Kind.MAIN;
            }
            case TEACH -> {
                if (c.next() != null) {
                    bond = bond.with(c.next().flag, true);
                }
            }
            case LEARN -> {
                // 1.3.14: a companion's lesson: its gate opens now (checked again: the mastery, the step before).
                if (c.teaching().ready()) {
                    taught(player, npc, c.teaching().next());
                    bond = bond.regard(bond.regard() + 10);
                }
            }
            case TORTURE -> {
                // 1.3.14: lived through first (the cutscene), then the gate.
                if (c.teaching().ready()) {
                    QuestServer.bond(player, npc.persona().key(), bond.regard(bond.regard() + 10));
                    TALKS.remove(player.m_20148_());
                    EtoBrain.listen(npc, null, now);
                    TortureServer.begin(player, npc, c.teaching().next());
                    return;
                }
            }
            case PACT -> {
                NpcMemoryServer.of(player).set(npc.persona().key(), NpcMemory.TRIAL, EtoRules.Pact.HUNT.ordinal()).set(npc.persona().key(), NpcMemory.TRIAL_COUNT, 0L);
                NpcMemoryServer.save(player);
            }
            case BEGIN -> bond = EtoRules.begin(bond);
            case DATE -> {
                if (c.mayDate() && npc.persona().likes(com.hex.kagune.control.Genders.female(player))) {
                    bond = EtoRules.date(bond);
                    UniqueNpcs.pair(level, npc.persona().key(), player, Romance.Stage.DATING);
                    NpcMemoryServer.set(player, npc.persona().key(), NpcMemory.SINCE, day);
                    EtoBrain.giddy(npc, player, now);
                    EtoBrain.hearts(level, npc, player, 10);
                    npc.mind.dirty = true;
                    RomanceServer.romanced(player, npc.persona().key());
                    player.m_5661_(Component.m_237113_(npc.persona().colour() + "\u2764 You and " + npc.displayName() + " are dating."), true);
                }
            }
            case KISS -> EtoBrain.walkUp(npc, npc.mind, player, EtoGesture.KISS, now);
            case HUG -> EtoBrain.walkUp(npc, npc.mind, player, EtoGesture.HUG, now);
            case CLING -> EtoBrain.walkUp(npc, npc.mind, player, EtoGesture.CLING, now);
            case HAND -> EtoBrain.walkUp(npc, npc.mind, player, EtoGesture.HAND, now);
            case ANGER -> {
                if (!EtoBrain.partnerOf(npc.mind, player)) {
                    npc.mind.grudges.put(player.m_20148_(), now + 400L);
                }
            }
            case FOLLOW -> npc.follow(player.m_20148_());
            case STAY -> {
                npc.follow(null);
                npc.settle();
            }
            case FORGIVE -> {
                EtoBrain.forgive(npc, player);
                NpcMemoryServer.of(player).forget(npc.persona().key(), NpcMemory.JEALOUS);
                NpcMemoryServer.save(player);
            }
            case NIGHT -> {
                QuestServer.bond(player, npc.persona().key(), bond);
                night(player, npc, c);
                return;
            }
            case BABY -> {
                // 1.3.10: asked for: the next night it comes.
                NpcMemoryServer.set(player, npc.persona().key(), NpcMemory.BABY, 1L);
                EtoBrain.hearts(player.m_284548_(), npc, player, 6);
            }
            case OFFER -> next = Kind.OFFER;
            case SURGERY -> {
                QuestServer.bond(player, npc.persona().key(), bond);
                TALKS.remove(player.m_20148_());
                ControlNetwork.sendTo(player, new S2CNpcTalk(npc.m_19879_(), npc.displayName(), npc.persona().script().title(c), bond.regard(), false, face(npc), line,
                    new int[] {NpcTalk.Choice.LEAVE.ordinal()}));
                surgery(player, c);
                return;
            }
            case TALK -> next = Kind.TALK;
            case SWEET -> next = Kind.SWEET;
            case MAIN -> next = Kind.MAIN;
            case GIFTS -> next = Kind.GIFTS;
            default -> {
            }
        }
        if (reply.effect() == EtoTalk.Effect.CLOSE || reply.effect() == EtoTalk.Effect.ANGER || reply.effect() == EtoTalk.Effect.KISS
            || reply.effect() == EtoTalk.Effect.HUG || reply.effect() == EtoTalk.Effect.CLING || reply.effect() == EtoTalk.Effect.HAND) {
            next = Kind.CLOSED;
        }
        if (choice == NpcTalk.Choice.DECLINE) {
            next = Kind.MAIN;
        }
        QuestServer.bond(player, npc.persona().key(), bond);
        EtoTalk.Context after = context(player, npc);
        if (next == Kind.CLOSED) {
            EtoBrain.listen(npc, null, now);
        }
        show(player, npc, after, page(npc.persona().script(), after, next, line), next);
    }

    /** The page {@code kind} with {@code line} hers. */
    private static EtoTalk.Page page(Script script, EtoTalk.Context c, Kind kind, String line) {
        return switch (kind) {
            case TALK -> script.talk(c, line);
            case SWEET -> c.dating() ? script.sweet(c, line) : script.main(c, line);
            case GIFTS -> script.gifts(c, line);
            case OFFER -> new EtoTalk.Page(line, List.of(new EtoTalk.Option(NpcTalk.Choice.ACCEPT, "Yes. Do it.", ""),
                new EtoTalk.Option(NpcTalk.Choice.DECLINE, "Not yet.", "")));
            case CLOSED -> new EtoTalk.Page(line, List.of(new EtoTalk.Option(NpcTalk.Choice.LEAVE, "", "")));
            default -> script.main(c, line);
        };
    }

    // ---------------------------------------------------------------- the ring

    private static void propose(ServerPlayer player, EtoNpc npc, EtoTalk.Context c) {
        ServerLevel level = player.m_284548_();
        long now = level.m_46467_();
        Romance.Refusal refusal = Romance.propose(c.stage(), c.claimed(), c.spouseElsewhere(), c.daysTogether(), c.regard());
        if (refusal == null && !hasRing(player)) {
            show(player, npc, c, npc.persona().script().sweet(c, "A ring? Where? ...Don't tease me like that."), Kind.SWEET);
            return;
        }
        Script script = npc.persona().script();
        String line = script.ring(c, refusal);
        if (refusal != null) {
            show(player, npc, c, c.dating() ? script.sweet(c, line) : script.main(c, line), c.dating() ? Kind.SWEET : Kind.MAIN);
            return;
        }
        takeRing(player);
        UniqueNpcs.pair(level, npc.persona().key(), player, Romance.Stage.MARRIED);
        NpcMemoryServer.set(player, npc.persona().key(), NpcMemory.WED, day(level));
        QuestLog.Bond bond = QuestServer.bond(player, npc.persona().key());
        QuestServer.bond(player, npc.persona().key(), bond.regard(NpcBonds.MOST).with(EtoRules.DATING, true));
        EtoBrain.gesture(npc.mind, EtoGesture.RING, now);
        EtoBrain.giddy(npc, player, now);
        RomanceServer.romanced(player, npc.persona().key());
        npc.mind.dirty = true;
        for (int i = 0; i < 4; i++) {
            level.m_8767_(ParticleTypes.f_123750_, npc.m_20185_(), npc.m_20186_() + 1.0 + i * 0.4, npc.m_20189_(), 8, 0.7, 0.4, 0.7, 0.05);
        }
        UkakuFire.sound(level, npc.m_20182_(), "entity.player.levelup", 1.0F, 1.2F);
        UkakuFire.sound(level, npc.m_20182_(), "block.bell.use", 0.9F, 1.4F);
        if (level.m_7654_() != null) {
            level.m_7654_().m_6846_().m_240416_(Component.m_237113_(npc.persona().colour() + "\u2764 " + player.m_36316_().getName() + " and "
                + npc.displayName() + " are married!"), false);
        }
        EtoTalk.Context after = context(player, npc);
        show(player, npc, after, script.main(after, line), Kind.MAIN);
    }

    private static boolean hasRing(ServerPlayer player) {
        return ControlItems.ring(player.m_21205_()) || ControlItems.ring(player.m_21206_());
    }

    private static void takeRing(ServerPlayer player) {
        for (ItemStack held : new ItemStack[] {player.m_21205_(), player.m_21206_()}) {
            if (ControlItems.ring(held)) {
                held.m_41774_(1);
                return;
            }
        }
    }

    // ---------------------------------------------------------------- the night

    /** The night together: the screen goes dark, and in the morning she says something. Once a night. */
    static void night(ServerPlayer player, EtoNpc npc, EtoTalk.Context c) {
        ServerLevel level = player.m_284548_();
        long now = level.m_46467_();
        TALKS.remove(player.m_20148_());
        EtoBrain.listen(npc, null, now);
        NpcMemoryServer.of(player).add(npc.persona().key(), NpcMemory.NIGHTS, 1L).set(npc.persona().key(), NpcMemory.NIGHT_DAY, day(level));
        NpcMemoryServer.save(player);
        // 1.3.10: asked for a baby, married, not already carrying: tonight she conceives.
        boolean conceived = conceive(level, player, npc, c);
        ControlNetwork.sendTo(player, new S2CCutscene(S2CCutscene.NIGHT, conceived ? npc.persona().script().conceived(c) : npc.persona().script().morning(c)));
        player.m_7292_(new MobEffectInstance(MobEffects.f_19605_, 1200, 1, false, true, true));
        player.m_7292_(new MobEffectInstance(MobEffects.f_19606_, 2400, 0, false, true, true));
        EtoBrain.giddy(npc, player, now);
        EtoBrain.gesture(npc.mind, EtoGesture.HUM, now);
        RomanceServer.romanced(player, npc.persona().key());
        EtoBrain.hearts(level, npc, player, 12);
        QuestLog.Bond bond = QuestServer.bond(player, npc.persona().key());
        QuestServer.bond(player, npc.persona().key(), bond.regard(bond.regard() + 5));
    }

    /**
     * 1.3.10: if they asked for a baby and are married, she conceives tonight (the child decided now); true if she did. 1.3.14: whoever is
     * the woman of the two carries it (a companion, or the player if the companion is a man), and only a woman and a man can.
     */
    private static boolean conceive(ServerLevel level, ServerPlayer player, EtoNpc npc, EtoTalk.Context c) {
        com.hex.kagune.control.npc.Persona p = npc.persona();
        if (!c.married() || !c.family().wantsBaby() || !c.mixed()) {
            return false;
        }
        String npcKey = Pregnancies.npc(p.key());
        String playerKey = Pregnancies.player(player.m_20148_());
        String mother = p.female() ? npcKey : playerKey;
        if (Pregnancies.pregnant(level, mother)) {
            return false;
        }
        NpcMemoryServer.of(player).forget(p.key(), NpcMemory.BABY);
        NpcMemoryServer.save(player);
        com.hex.kagune.control.family.Genome.Parent theirs = new com.hex.kagune.control.family.Genome.Parent(p.hair(), p.eyes(), p.skin(), true, p.oneEyed());
        java.util.Random random = new java.util.Random(level.m_46467_() ^ player.m_20148_().getLeastSignificantBits());
        if (p.female()) {
            com.hex.kagune.control.family.Genome genome = com.hex.kagune.control.family.Genome.of(theirs, Looks.parent(player), random);
            Pregnancies.conceive(level, npcKey, npc.displayName(), playerKey, player.m_36316_().getName(), genome);
        } else {
            com.hex.kagune.control.family.Genome genome = com.hex.kagune.control.family.Genome.of(Looks.parent(player), theirs, random);
            Pregnancies.conceive(level, playerKey, player.m_36316_().getName(), npcKey, npc.displayName(), genome);
        }
        return true;
    }

    // ---------------------------------------------------------------- what is brought

    /** One of what {@code is} takes from their hands; false if they hold none. */
    private static boolean take(ServerPlayer player, java.util.function.Predicate<String> is) {
        for (ItemStack held : new ItemStack[] {player.m_21205_(), player.m_21206_()}) {
            if (is.test(id(held))) {
                held.m_41774_(1);
                return true;
            }
        }
        return false;
    }

    private static boolean takeStack(ServerPlayer player, java.util.function.Predicate<ItemStack> is) {
        for (ItemStack held : new ItemStack[] {player.m_21205_(), player.m_21206_()}) {
            if (is.test(held)) {
                held.m_41774_(1);
                return true;
            }
        }
        return false;
    }

    private static boolean holds(ServerPlayer player, java.util.function.Predicate<String> is) {
        return is.test(id(player.m_21205_())) || is.test(id(player.m_21206_()));
    }

    private static String id(ItemStack stack) {
        if (stack == null || stack.m_41619_()) {
            return null;
        }
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.m_41720_());
        return key == null ? null : key.toString();
    }

    /** A baby animal on {@code player}'s lead near her, or null. */
    static Mob puppy(ServerPlayer player, EtoNpc npc) {
        for (Mob mob : npc.m_9236_().m_6443_(Mob.class, npc.m_20191_().m_82400_(PUPPY_NEAR), mob -> mob != npc && mob.m_6084_())) {
            if (mob.m_6162_() && mob.m_21523_() && mob.m_21524_() == player) {
                return mob;
            }
        }
        return null;
    }

    /** What kind of animal it is ({@code wolf}, {@code cat}...). */
    static String kind(Mob mob) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(mob.m_6095_());
        return key == null ? "animal" : key.m_135815_();
    }

    /** It's hers now: off the lead (the lead dropped), and it stays. */
    private static void give(Mob puppy) {
        puppy.m_21455_(true, true);
        puppy.m_21530_();
    }

    // ---------------------------------------------------------------- what a page depends on

    /** What the next page depends on, as it stands now. */
    /** 1.3.10: their family as she talks of it: carrying their child (how far along), asked for one, how many they have. */
    static com.hex.kagune.control.family.FamilyView family(ServerLevel level, ServerPlayer player, NpcMemory memory) {
        return family(level, player, memory, com.hex.kagune.control.npc.Persona.ETO);
    }

    /** 1.3.14: the same with any companion: whichever of the two carries their child (her, or the player carrying his). */
    static com.hex.kagune.control.family.FamilyView family(ServerLevel level, ServerPlayer player, NpcMemory memory, com.hex.kagune.control.npc.Persona p) {
        String npcKey = Pregnancies.npc(p.key());
        String playerKey = Pregnancies.player(player.m_20148_());
        Pregnancies.Carried carried = Pregnancies.get(level, p.female() ? npcKey : playerKey);
        String father = p.female() ? playerKey : npcKey;
        double along = carried != null && carried.father.equals(father)
            ? com.hex.kagune.control.family.Pregnancy.progress(carried.conceived, level.m_46467_()) : -1.0;
        return new com.hex.kagune.control.family.FamilyView(along, memory.get(p.key(), NpcMemory.BABY, 0L) == 1L,
            (int)memory.get(p.key(), NpcMemory.CHILDREN, 0L));
    }

    static EtoTalk.Context context(ServerPlayer player, EtoNpc npc) {
        ServerLevel level = player.m_284548_();
        long now = level.m_46467_();
        QuestLog log = QuestServer.log(player);
        QuestLog.Bond bond = log.bond(npc.persona().key());
        boolean ghoul = KaguneProfile.isUnlocked(player);
        com.hex.kagune.control.KagunePath path = PathServer.path(player);
        int mastery = KaguneProfile.mastery(player);
        java.util.function.Predicate<EtoRules.Lesson> known = lesson -> {
            Quest quest = QuestBook.get(lesson.quest);
            return quest != null && log.state(quest, path, mastery) == QuestLog.State.DONE || lesson.gate != null && log.open(lesson.gate);
        };
        com.hex.kagune.control.npc.Persona persona = npc.persona();
        boolean eto = persona == com.hex.kagune.control.npc.Persona.ETO;
        // 1.3.14: Eto's lessons are hers; every other companion teaches what Teaching says it does.
        EtoRules.Lesson next = eto ? EtoRules.next(bond, known) : null;
        com.hex.kagune.control.npc.Teaching.View teaching = eto ? com.hex.kagune.control.npc.Teaching.View.NONE
            : com.hex.kagune.control.npc.Teaching.view(persona.key(), ghoul, path, mastery, log::open);
        boolean beforeDone = next == null || next.before() == null || known.test(next.before());
        long day = day(level);
        UniqueNpc unique = UniqueNpcs.get(level, npc.persona().key());
        UUID id = player.m_20148_();
        Romance.Stage stage = unique.stageOf(id);
        String claimedBy = unique.claimedFrom(id) ? unique.partnerName : null;
        NpcMemory memory = NpcMemoryServer.of(player);
        long since = memory.get(npc.persona().key(), stage == Romance.Stage.MARRIED ? NpcMemory.WED : NpcMemory.SINCE, day);
        long seen = memory.get(npc.persona().key(), NpcMemory.SEEN, Long.MIN_VALUE);
        long daysAway = seen == Long.MIN_VALUE ? 0L : Math.max(0L, (now - seen) / 24000L);
        Mob puppy = puppy(player, npc);
        EtoRules.Pact pact = EtoRules.Pact.byOrdinal(memory.get(npc.persona().key(), NpcMemory.TRIAL, 0L));
        Quest trial = QuestBook.get(EtoRules.Lesson.TRIAL.quest);
        boolean trialDone = trial != null && log.state(trial, path, mastery) == QuestLog.State.DONE;
        EtoMood mood = EtoBrain.mood(npc, player, now);
        return new EtoTalk.Context(bond.regard(), bond.flags(), EtoRules.count(bond), ghoul, bond.chatted() == day, EtoRules.giftedOn(bond, day),
            holds(player, persona::errandItem), holds(player, persona::flower), next, beforeDone, KakuganServer.granted(player),
            npc.m_217043_().m_188503_(100000), stage, claimedBy, id.equals(npc.following()), level.m_46468_(), mood, Romance.days(since, day), daysAway,
            puppy == null ? null : kind(puppy), hasRing(player), ControlItems.ghoulMeat(player.m_21205_()) || ControlItems.ghoulMeat(player.m_21206_()),
            holds(player, EtoRules::letter), pact, (int)memory.get(npc.persona().key(), NpcMemory.TRIAL_COUNT, 0L), trialDone,
            memory.get(npc.persona().key(), NpcMemory.NIGHT_DAY, -1L) == day, player.m_36316_().getName(),
            UniqueNpcs.spouseOf(level, player, npc.persona().key()) != null, family(level, player, memory, persona), teaching,
            persona.female() != com.hex.kagune.control.Genders.female(player));
    }

    private static void show(ServerPlayer player, EtoNpc npc, EtoTalk.Context context, EtoTalk.Page page, Kind kind) {
        List<EtoTalk.Option> options = page.options().size() > S2CNpcTalk.MOST ? page.options().subList(0, S2CNpcTalk.MOST) : page.options();
        TALKS.put(player.m_20148_(), new Talk(npc.m_19879_(), List.copyOf(options), kind));
        int[] ordinals = new int[options.size()];
        String[] labels = new String[options.size()];
        for (int i = 0; i < ordinals.length; i++) {
            ordinals[i] = options.get(i).choice().ordinal();
            labels[i] = options.get(i).label();
        }
        String temper = npc.persona().script().title(context) + " - " + context.mood().word();
        ControlNetwork.sendTo(player, new S2CNpcTalk(npc.m_19879_(), npc.displayName(), temper, context.regard(), false, face(npc), page.line(), ordinals,
            labels));
    }

    /** 1.3.14: the face on the talk screen: -1 Eto's, -2 Touka's, -3 Ken's (Persona's order). */
    static int face(EtoNpc npc) {
        return -1 - Math.max(0, com.hex.kagune.control.npc.Persona.ALL.indexOf(npc.persona()));
    }

    /** 1.3.14: {@code lesson} taught: its gate opens, the game says so, and it is remembered. */
    static void taught(ServerPlayer player, EtoNpc npc, com.hex.kagune.control.npc.Teaching.Lesson lesson) {
        taught(player, npc.persona(), npc.displayName(), lesson);
    }

    /** The same, by who taught it (the trial ends after the talk has closed). */
    static void taught(ServerPlayer player, com.hex.kagune.control.npc.Persona persona, String name, com.hex.kagune.control.npc.Teaching.Lesson lesson) {
        if (lesson == null) {
            return;
        }
        QuestServer.grant(player, lesson.gate());
        ServerLevel level = player.m_284548_();
        UkakuFire.sound(level, player.m_20182_(), "entity.player.levelup", 0.8F, 0.8F);
        player.m_5661_(Component.m_237113_(persona.colour() + name + " taught you: \u00a7f" + lesson.name() + " \u00a77("
            + lesson.gate().displayName() + ")"), false);
    }

    /**
     * Her surgery: their screen goes black, they hear it, and she wakes them, warmly if they are hers, coldly if not. From then on they
     * may wear one kakugan, a ghoul's turning one-eyed at once; a human wakes a ghoul (one-eyed, the path to choose), as she is.
     */
    static void surgery(ServerPlayer player, EtoTalk.Context c) {
        boolean ghoul = KaguneProfile.isUnlocked(player);
        ControlNetwork.sendTo(player, new S2CCutscene(S2CCutscene.SURGERY, EtoLines.surgeryWords(c)));
        if (!ghoul) {
            KaguneProfile.setUnlocked(player, true);
            KaguneNetwork.broadcast(player);
            ControlNetwork.sendTo(player, new S2CPathPrompt());
        }
        KakuganServer.grant(player);
    }

    private static long day(ServerLevel level) {
        return Math.floorDiv(level.m_46468_(), 24000L);
    }

    // ---------------------------------------------------------------- a human's pact

    /** {@code amount} more of {@code doing} on {@code player}'s pact (if it is the step they are on). */
    private static void pact(ServerPlayer player, EtoRules.Pact doing, int amount) {
        NpcMemory memory = NpcMemoryServer.of(player);
        EtoRules.Pact step = EtoRules.Pact.byOrdinal(memory.get(EtoRules.KEY, NpcMemory.TRIAL, 0L));
        if (step != doing) {
            return;
        }
        long[] after = EtoRules.pact(step, (int)memory.get(EtoRules.KEY, NpcMemory.TRIAL_COUNT, 0L), doing, amount);
        memory.set(EtoRules.KEY, NpcMemory.TRIAL, after[0]).set(EtoRules.KEY, NpcMemory.TRIAL_COUNT, after[1]);
        NpcMemoryServer.save(player);
        EtoRules.Pact now = EtoRules.Pact.byOrdinal(after[0]);
        player.m_5661_(Component.m_237113_(now == step
            ? EtoRules.NAME + "'s price: " + after[1] + " of " + step.need
            : now == EtoRules.Pact.READY ? EtoRules.NAME + "'s price is paid. Go and see her." : EtoRules.NAME + "'s price: that part's done. Go and see her."), true);
    }

    // ---------------------------------------------------------------- deaths: her errand's kills, a human's pact, and hers

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void died(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        Entity source = event.getSource().m_7639_();
        if (!(source instanceof ServerPlayer killer) || killer == dead) {
            return;
        }
        if (dead instanceof EtoNpc npc) {
            String key = npc.persona().key();
            ServerLevel level = killer.m_284548_();
            UniqueNpc unique = UniqueNpcs.get(level, key);
            if (unique.stageOf(killer.m_20148_()).together()) {
                // Their partner killed them: whatever they had is over, for good.
                UniqueNpcs.pair(level, key, null, Romance.Stage.NONE);
                NpcMemoryServer.of(killer).forget(key, NpcMemory.SINCE).forget(key, NpcMemory.WED);
                NpcMemoryServer.save(killer);
                killer.m_5661_(Component.m_237113_("\u00a74You killed " + npc.persona().her() + ". Whatever you had is over."), false);
            }
            QuestServer.bond(killer, key, EtoRules.killedBy(QuestServer.bond(killer, key)));
            killer.m_5661_(Component.m_237113_(npc.displayName() + " will remember this."), true);
            return;
        }
        boolean night = EtoRules.night(killer.m_9236_().m_46468_());
        if (dead instanceof Enemy && night) {
            // 1.3.14: every companion's errand of kills at night counts (each is on its own step).
            for (com.hex.kagune.control.npc.Persona persona : com.hex.kagune.control.npc.Persona.ALL) {
                QuestLog.Bond bond = QuestServer.bond(killer, persona.key());
                if (EtoRules.step(bond) == EtoRules.Step.HUNT) {
                    QuestLog.Bond after = EtoRules.advance(bond, EtoRules.Step.HUNT, 1);
                    QuestServer.bond(killer, persona.key(), after);
                    killer.m_5661_(Component.m_237113_(EtoRules.step(after) == EtoRules.Step.HUNT
                        ? persona.name() + "'s errand: " + EtoRules.count(after) + " of " + EtoRules.Step.HUNT.need + " killed tonight"
                        : persona.name() + "'s errand: done. Go and see " + persona.her() + "."), true);
                }
            }
        }
        if (!KaguneProfile.isUnlocked(killer)) {
            if (dead instanceof Enemy && night) {
                pact(killer, EtoRules.Pact.HUNT, 1);
            }
            if (dead instanceof GhoulNpc) {
                pact(killer, EtoRules.Pact.GHOUL, 1);
            }
        }
    }
}
