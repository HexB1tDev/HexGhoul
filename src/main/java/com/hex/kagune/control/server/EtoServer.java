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
            player.m_5661_(Component.m_237113_(EtoRules.NAME + " is busy. Very busy. With teeth."), true);
            return;
        }
        QuestLog.Bond bond = QuestServer.bond(player, EtoRules.KEY);
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
                    QuestServer.bond(player, EtoRules.KEY, bond.regard(Math.max(bond.regard(), 0)));
                } else {
                    QuestServer.bond(player, EtoRules.KEY, bond.regard(bond.regard() + 10));
                }
                EtoBrain.forgive(npc, player);
                EtoBrain.gesture(mind, EtoGesture.PET, now);
                EtoBrain.giddy(npc, player, now);
                line = EtoLines.puppy(c, kind(puppy), true);
            } else if (!enemy && take(player, EtoRules::letter)) {
                QuestServer.bond(player, EtoRules.KEY, bond.regard(bond.regard() + 6));
                EtoBrain.forgive(npc, player);
                EtoBrain.gesture(mind, EtoGesture.READ, now);
                line = EtoLines.letter(c, true);
            } else {
                player.m_5661_(Component.m_237113_(EtoLines.snub(npc.m_217043_().m_188503_(100), enemy)), true);
                return;
            }
            bond = QuestServer.bond(player, EtoRules.KEY);
        }
        if (!bond.has(NpcBonds.MET)) {
            QuestServer.bond(player, EtoRules.KEY, bond.with(NpcBonds.MET, true));
        }
        migrate(player, npc);
        EtoBrain.listen(npc, player, now);
        EtoTalk.Context context = context(player, npc);
        NpcMemoryServer.set(player, EtoRules.KEY, NpcMemory.SEEN, now);
        show(player, npc, context, EtoTalk.main(context, line != null ? line : EtoTalk.greeting(context)), Kind.MAIN);
    }

    private static boolean sulking(EtoBrain.Mind mind, ServerPlayer player, long now) {
        Long until = mind.sulk.get(player.m_20148_());
        return until != null && until > now;
    }

    /** 1.3.0's dating, kept in the player's bond alone, made the world's: she is theirs if she is no one else's yet. */
    private static void migrate(ServerPlayer player, EtoNpc npc) {
        QuestLog.Bond bond = QuestServer.bond(player, EtoRules.KEY);
        if (!bond.has(EtoRules.DATING)) {
            return;
        }
        ServerLevel level = player.m_284548_();
        UniqueNpc unique = UniqueNpcs.get(level, EtoRules.KEY);
        if (unique.stageOf(player.m_20148_()).together()) {
            return;
        }
        if (unique.claimedFrom(player.m_20148_()) || UniqueNpcs.spouseOf(level, player, EtoRules.KEY) != null) {
            QuestServer.bond(player, EtoRules.KEY, bond.with(EtoRules.DATING, false));
            return;
        }
        UniqueNpcs.pair(level, EtoRules.KEY, player, Romance.Stage.DATING);
        NpcMemoryServer.set(player, EtoRules.KEY, NpcMemory.SINCE, day(level));
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
        String key = option.key();
        if (key.startsWith("topic:")) {
            EtoTopics.Topic topic = EtoTopics.get(key.substring(6));
            if (topic != null) {
                show(player, npc, c, EtoTalk.topic(c, topic), Kind.TOPIC);
            }
            return;
        }
        if (key.startsWith("follow:")) {
            String[] parts = key.split(":");
            EtoTopics.Topic topic = EtoTopics.get(parts[1]);
            int index = Integer.parseInt(parts[2]);
            if (topic == null) {
                return;
            }
            EtoTalk.Reply reply = EtoTalk.follow(c, topic, index);
            int bit = EtoTopics.index(topic) * 4 + Math.min(3, index);
            carry(player, npc, c, choice, firstToday(player, level, bit) ? reply : unpaid(reply), Kind.TALK);
            return;
        }
        if (key.startsWith("sweet:")) {
            String what = key.substring(6);
            int bit = 56 + Math.floorMod(what.hashCode(), 8);
            EtoTalk.Reply reply = EtoTalk.sweetly(c, what);
            carry(player, npc, c, choice, firstToday(player, level, bit) ? reply : unpaid(reply), Kind.SWEET);
            return;
        }
        if (key.equals("gifts")) {
            show(player, npc, c, EtoTalk.gifts(c, EtoLines.one(c.seed(), "Oh? For me? What is it?", "A present? Show me.")), Kind.GIFTS);
            return;
        }
        if (choice == NpcTalk.Choice.PROPOSE) {
            propose(player, npc, c);
            return;
        }
        if (choice == NpcTalk.Choice.BACK && talk.kind() == Kind.TOPIC) {
            // Back from a topic: to the others.
            show(player, npc, c, EtoTalk.talk(c, EtoLines.one(c.seed(), "What else?", "Mm. Something else, then.", "Go on.")), Kind.TALK);
            return;
        }
        Kind after = talk.kind() == Kind.SWEET || talk.kind() == Kind.TALK ? talk.kind() : Kind.MAIN;
        carry(player, npc, c, choice, EtoTalk.answer(c, choice), after);
    }

    private static EtoTalk.Reply unpaid(EtoTalk.Reply reply) {
        return new EtoTalk.Reply(reply.line(), Math.min(0, reply.regard()), reply.effect(), reply.gesture());
    }

    /** True the first time today {@code bit} of the talk is said (its regard paid once a day); marks it said. */
    private static boolean firstToday(ServerPlayer player, ServerLevel level, int bit) {
        NpcMemory memory = NpcMemoryServer.of(player);
        long today = day(level);
        long said = memory.get(EtoRules.KEY, NpcMemory.TALKED_DAY, -1L) == today ? memory.get(EtoRules.KEY, NpcMemory.TOPICS, 0L) : 0L;
        boolean first = (said & 1L << bit) == 0L;
        memory.set(EtoRules.KEY, NpcMemory.TALKED_DAY, today).set(EtoRules.KEY, NpcMemory.TOPICS, said | 1L << bit);
        NpcMemoryServer.save(player);
        return first;
    }

    /** An answer carried out (her regard, what she does, what happens), then the page it leads to ({@code stay}: the page they were on). */
    private static void carry(ServerPlayer player, EtoNpc npc, EtoTalk.Context c, NpcTalk.Choice choice, EtoTalk.Reply reply, Kind stay) {
        ServerLevel level = player.m_284548_();
        long now = level.m_46467_();
        long day = day(level);
        QuestLog.Bond bond = QuestServer.bond(player, EtoRules.KEY);
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
            RomanceServer.romanced(player, EtoRules.KEY);
        }
        Kind next = stay;
        String line = reply.line();
        switch (reply.effect()) {
            case BOOK, FLOWER -> {
                boolean isBook = reply.effect() == EtoTalk.Effect.BOOK;
                java.util.function.Predicate<String> gift = isBook ? EtoRules::book : EtoRules::flower;
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
                    NpcMemoryServer.set(player, EtoRules.KEY, NpcMemory.PUPPY_DAY, day);
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
            case PACT -> {
                NpcMemoryServer.of(player).set(EtoRules.KEY, NpcMemory.TRIAL, EtoRules.Pact.HUNT.ordinal()).set(EtoRules.KEY, NpcMemory.TRIAL_COUNT, 0L);
                NpcMemoryServer.save(player);
            }
            case BEGIN -> bond = EtoRules.begin(bond);
            case DATE -> {
                if (c.mayDate()) {
                    bond = EtoRules.date(bond);
                    UniqueNpcs.pair(level, EtoRules.KEY, player, Romance.Stage.DATING);
                    NpcMemoryServer.set(player, EtoRules.KEY, NpcMemory.SINCE, day);
                    EtoBrain.giddy(npc, player, now);
                    EtoBrain.hearts(level, npc, player, 10);
                    npc.mind.dirty = true;
                    RomanceServer.romanced(player, EtoRules.KEY);
                    player.m_5661_(Component.m_237113_("§d❤ You and " + EtoRules.NAME + " are dating."), true);
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
                NpcMemoryServer.of(player).forget(EtoRules.KEY, NpcMemory.JEALOUS);
                NpcMemoryServer.save(player);
            }
            case NIGHT -> {
                QuestServer.bond(player, EtoRules.KEY, bond);
                night(player, npc, c);
                return;
            }
            case BABY -> {
                // 1.3.10: asked for: the next night it comes.
                NpcMemoryServer.set(player, EtoRules.KEY, NpcMemory.BABY, 1L);
                EtoBrain.hearts(player.m_284548_(), npc, player, 6);
            }
            case OFFER -> next = Kind.OFFER;
            case SURGERY -> {
                QuestServer.bond(player, EtoRules.KEY, bond);
                TALKS.remove(player.m_20148_());
                ControlNetwork.sendTo(player, new S2CNpcTalk(npc.m_19879_(), EtoRules.NAME, EtoLines.title(c), bond.regard(), false, -1, line,
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
        QuestServer.bond(player, EtoRules.KEY, bond);
        EtoTalk.Context after = context(player, npc);
        if (next == Kind.CLOSED) {
            EtoBrain.listen(npc, null, now);
        }
        show(player, npc, after, page(after, next, line), next);
    }

    /** The page {@code kind} with {@code line} hers. */
    private static EtoTalk.Page page(EtoTalk.Context c, Kind kind, String line) {
        return switch (kind) {
            case TALK -> EtoTalk.talk(c, line);
            case SWEET -> c.dating() ? EtoTalk.sweet(c, line) : EtoTalk.main(c, line);
            case GIFTS -> EtoTalk.gifts(c, line);
            case OFFER -> new EtoTalk.Page(line, List.of(new EtoTalk.Option(NpcTalk.Choice.ACCEPT, "Yes. Do it.", ""),
                new EtoTalk.Option(NpcTalk.Choice.DECLINE, "Not yet.", "")));
            case CLOSED -> new EtoTalk.Page(line, List.of(new EtoTalk.Option(NpcTalk.Choice.LEAVE, "", "")));
            default -> EtoTalk.main(c, line);
        };
    }

    // ---------------------------------------------------------------- the ring

    private static void propose(ServerPlayer player, EtoNpc npc, EtoTalk.Context c) {
        ServerLevel level = player.m_284548_();
        long now = level.m_46467_();
        Romance.Refusal refusal = Romance.propose(c.stage(), c.claimed(), c.spouseElsewhere(), c.daysTogether(), c.regard());
        if (refusal == null && !hasRing(player)) {
            show(player, npc, c, EtoTalk.sweet(c, "A ring? Where? ...Don't tease me like that."), Kind.SWEET);
            return;
        }
        String line = EtoLines.ring(c, refusal);
        if (refusal != null) {
            show(player, npc, c, c.dating() ? EtoTalk.sweet(c, line) : EtoTalk.main(c, line), c.dating() ? Kind.SWEET : Kind.MAIN);
            return;
        }
        takeRing(player);
        UniqueNpcs.pair(level, EtoRules.KEY, player, Romance.Stage.MARRIED);
        NpcMemoryServer.set(player, EtoRules.KEY, NpcMemory.WED, day(level));
        QuestLog.Bond bond = QuestServer.bond(player, EtoRules.KEY);
        QuestServer.bond(player, EtoRules.KEY, bond.regard(NpcBonds.MOST).with(EtoRules.DATING, true));
        EtoBrain.gesture(npc.mind, EtoGesture.RING, now);
        EtoBrain.giddy(npc, player, now);
        RomanceServer.romanced(player, EtoRules.KEY);
        npc.mind.dirty = true;
        for (int i = 0; i < 4; i++) {
            level.m_8767_(ParticleTypes.f_123750_, npc.m_20185_(), npc.m_20186_() + 1.0 + i * 0.4, npc.m_20189_(), 8, 0.7, 0.4, 0.7, 0.05);
        }
        UkakuFire.sound(level, npc.m_20182_(), "entity.player.levelup", 1.0F, 1.2F);
        UkakuFire.sound(level, npc.m_20182_(), "block.bell.use", 0.9F, 1.4F);
        if (level.m_7654_() != null) {
            level.m_7654_().m_6846_().m_240416_(Component.m_237113_("§d❤ " + player.m_36316_().getName() + " and " + EtoRules.NAME
                + " are married!"), false);
        }
        EtoTalk.Context after = context(player, npc);
        show(player, npc, after, EtoTalk.main(after, line), Kind.MAIN);
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
    private static void night(ServerPlayer player, EtoNpc npc, EtoTalk.Context c) {
        ServerLevel level = player.m_284548_();
        long now = level.m_46467_();
        TALKS.remove(player.m_20148_());
        EtoBrain.listen(npc, null, now);
        NpcMemoryServer.of(player).add(EtoRules.KEY, NpcMemory.NIGHTS, 1L).set(EtoRules.KEY, NpcMemory.NIGHT_DAY, day(level));
        NpcMemoryServer.save(player);
        // 1.3.10: asked for a baby, married, not already carrying: tonight she conceives.
        boolean conceived = conceive(level, player, c);
        ControlNetwork.sendTo(player, new S2CCutscene(S2CCutscene.NIGHT, conceived ? EtoLines.conceived(c) : EtoLines.morning(c)));
        player.m_7292_(new MobEffectInstance(MobEffects.f_19605_, 1200, 1, false, true, true));
        player.m_7292_(new MobEffectInstance(MobEffects.f_19606_, 2400, 0, false, true, true));
        EtoBrain.giddy(npc, player, now);
        EtoBrain.gesture(npc.mind, EtoGesture.HUM, now);
        RomanceServer.romanced(player, EtoRules.KEY);
        EtoBrain.hearts(level, npc, player, 12);
        QuestLog.Bond bond = QuestServer.bond(player, EtoRules.KEY);
        QuestServer.bond(player, EtoRules.KEY, bond.regard(bond.regard() + 5));
    }

    /** 1.3.10: if they asked for a baby and are married, she conceives tonight (the child decided now); true if she did. */
    private static boolean conceive(ServerLevel level, ServerPlayer player, EtoTalk.Context c) {
        String mother = Pregnancies.npc(EtoRules.KEY);
        if (!c.married() || !c.family().wantsBaby() || Pregnancies.pregnant(level, mother)) {
            return false;
        }
        NpcMemoryServer.of(player).forget(EtoRules.KEY, NpcMemory.BABY);
        NpcMemoryServer.save(player);
        com.hex.kagune.control.family.Genome.Parent her = new com.hex.kagune.control.family.Genome.Parent(EtoRules.HAIR, EtoRules.EYES, EtoRules.SKIN,
            true, true);
        com.hex.kagune.control.family.Genome genome = com.hex.kagune.control.family.Genome.of(her, Looks.parent(player),
            new java.util.Random(level.m_46467_() ^ player.m_20148_().getLeastSignificantBits()));
        Pregnancies.conceive(level, mother, EtoRules.NAME, Pregnancies.player(player.m_20148_()), player.m_36316_().getName(), genome);
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
        Pregnancies.Carried carried = Pregnancies.get(level, Pregnancies.npc(EtoRules.KEY));
        double along = carried != null && carried.father.equals(Pregnancies.player(player.m_20148_()))
            ? com.hex.kagune.control.family.Pregnancy.progress(carried.conceived, level.m_46467_()) : -1.0;
        return new com.hex.kagune.control.family.FamilyView(along, memory.get(EtoRules.KEY, NpcMemory.BABY, 0L) == 1L,
            (int)memory.get(EtoRules.KEY, NpcMemory.CHILDREN, 0L));
    }

    static EtoTalk.Context context(ServerPlayer player, EtoNpc npc) {
        ServerLevel level = player.m_284548_();
        long now = level.m_46467_();
        QuestLog log = QuestServer.log(player);
        QuestLog.Bond bond = log.bond(EtoRules.KEY);
        boolean ghoul = KaguneProfile.isUnlocked(player);
        com.hex.kagune.control.KagunePath path = PathServer.path(player);
        int mastery = KaguneProfile.mastery(player);
        java.util.function.Predicate<EtoRules.Lesson> known = lesson -> {
            Quest quest = QuestBook.get(lesson.quest);
            return quest != null && log.state(quest, path, mastery) == QuestLog.State.DONE || lesson.gate != null && log.open(lesson.gate);
        };
        EtoRules.Lesson next = EtoRules.next(bond, known);
        boolean beforeDone = next == null || next.before() == null || known.test(next.before());
        long day = day(level);
        UniqueNpc unique = UniqueNpcs.get(level, EtoRules.KEY);
        UUID id = player.m_20148_();
        Romance.Stage stage = unique.stageOf(id);
        String claimedBy = unique.claimedFrom(id) ? unique.partnerName : null;
        NpcMemory memory = NpcMemoryServer.of(player);
        long since = memory.get(EtoRules.KEY, stage == Romance.Stage.MARRIED ? NpcMemory.WED : NpcMemory.SINCE, day);
        long seen = memory.get(EtoRules.KEY, NpcMemory.SEEN, Long.MIN_VALUE);
        long daysAway = seen == Long.MIN_VALUE ? 0L : Math.max(0L, (now - seen) / 24000L);
        Mob puppy = puppy(player, npc);
        EtoRules.Pact pact = EtoRules.Pact.byOrdinal(memory.get(EtoRules.KEY, NpcMemory.TRIAL, 0L));
        Quest trial = QuestBook.get(EtoRules.Lesson.TRIAL.quest);
        boolean trialDone = trial != null && log.state(trial, path, mastery) == QuestLog.State.DONE;
        EtoMood mood = EtoBrain.mood(npc, player, now);
        return new EtoTalk.Context(bond.regard(), bond.flags(), EtoRules.count(bond), ghoul, bond.chatted() == day, EtoRules.giftedOn(bond, day),
            holds(player, EtoRules::book), holds(player, EtoRules::flower), next, beforeDone, KakuganServer.granted(player),
            npc.m_217043_().m_188503_(100000), stage, claimedBy, id.equals(npc.following()), level.m_46468_(), mood, Romance.days(since, day), daysAway,
            puppy == null ? null : kind(puppy), hasRing(player), ControlItems.ghoulMeat(player.m_21205_()) || ControlItems.ghoulMeat(player.m_21206_()),
            holds(player, EtoRules::letter), pact, (int)memory.get(EtoRules.KEY, NpcMemory.TRIAL_COUNT, 0L), trialDone,
            memory.get(EtoRules.KEY, NpcMemory.NIGHT_DAY, -1L) == day, player.m_36316_().getName(),
            UniqueNpcs.spouseOf(level, player, EtoRules.KEY) != null, family(level, player, memory));
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
        String temper = EtoLines.title(context) + " - " + context.mood().word();
        ControlNetwork.sendTo(player, new S2CNpcTalk(npc.m_19879_(), EtoRules.NAME, temper, context.regard(), false, -1, page.line(), ordinals, labels));
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
        if (dead instanceof EtoNpc) {
            ServerLevel level = killer.m_284548_();
            UniqueNpc unique = UniqueNpcs.get(level, EtoRules.KEY);
            if (unique.stageOf(killer.m_20148_()).together()) {
                // Her partner killed her: whatever they had is over, for good.
                UniqueNpcs.pair(level, EtoRules.KEY, null, Romance.Stage.NONE);
                NpcMemoryServer.of(killer).forget(EtoRules.KEY, NpcMemory.SINCE).forget(EtoRules.KEY, NpcMemory.WED);
                NpcMemoryServer.save(killer);
                killer.m_5661_(Component.m_237113_("§4You killed her. Whatever you had is over."), false);
            }
            QuestServer.bond(killer, EtoRules.KEY, EtoRules.killedBy(QuestServer.bond(killer, EtoRules.KEY)));
            killer.m_5661_(Component.m_237113_(EtoRules.NAME + " will remember this."), true);
            return;
        }
        boolean night = EtoRules.night(killer.m_9236_().m_46468_());
        if (dead instanceof Enemy && night) {
            QuestLog.Bond bond = QuestServer.bond(killer, EtoRules.KEY);
            if (EtoRules.step(bond) == EtoRules.Step.HUNT) {
                QuestLog.Bond after = EtoRules.advance(bond, EtoRules.Step.HUNT, 1);
                QuestServer.bond(killer, EtoRules.KEY, after);
                killer.m_5661_(Component.m_237113_(EtoRules.step(after) == EtoRules.Step.HUNT
                    ? EtoRules.NAME + "'s errand: " + EtoRules.count(after) + " of " + EtoRules.Step.HUNT.need + " killed tonight"
                    : EtoRules.NAME + "'s errand: done. Go and see her."), true);
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
