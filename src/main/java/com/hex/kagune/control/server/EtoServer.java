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
        MAIN, TALK, TOPIC, SWEET, GIFTS, OFFER, CLOSED,
        // 1.3.29: a scene's page (something she came up with, or one of her scripted topics: SceneServer)
        SCENE
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

    /** 1.3.39: true while {@code player} has a talk with {@code npc} open on their screen. */
    static boolean talkingWith(ServerPlayer player, EtoNpc npc) {
        Talk talk = TALKS.get(player.m_20148_());
        return talk != null && talk.npc() == npc.m_19879_();
    }

    /** 1.3.29: true while {@code player} has a talk with a companion open. */
    static boolean talking(ServerPlayer player) {
        return TALKS.containsKey(player.m_20148_());
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
            } else if (!enemy && mind.target != player.m_19879_()) {
                // 2.2.0: put out, not an enemy: they can always say sorry (or come back with a little someone, or a letter).
                EtoBrain.listen(npc, player, now);
                List<EtoTalk.Option> options = new java.util.ArrayList<>();
                options.add(new EtoTalk.Option(NpcTalk.Choice.SAY_1, "I'm sorry.", "sorry"));
                options.add(new EtoTalk.Option(NpcTalk.Choice.SAY_2, "It was an accident. I swear.", "sorry"));
                options.add(new EtoTalk.Option(NpcTalk.Choice.LEAVE, "", ""));
                show(player, npc, c, new EtoTalk.Page(com.hex.kagune.control.npc.Tolerance.putOut(npc.persona().key(), npc.m_217043_().m_188503_(100)),
                    options), Kind.MAIN);
                return;
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
        if (npc.m_5803_() && mind.bed != null) {
            // 1.3.37: asleep in her bed at night: a hand on her shoulder only half wakes her, drowsy (HomeLife leaves her lying there).
            mind.drowsyUntil = now + DROWSY;
            EtoBrain.listen(npc, player, now);
            EtoTalk.Context sleepy = context(player, npc);
            show(player, npc, sleepy, drowsy(player, npc), Kind.MAIN);
            return;
        }
        EtoBrain.listen(npc, player, now);
        EtoTalk.Context context = context(player, npc);
        NpcMemoryServer.set(player, npc.persona().key(), NpcMemory.SEEN, now);
        Script script = npc.persona().script();
        // 1.3.29: she came up to them with something: the talk opens on it.
        if (line == null && scene(player, npc, context)) {
            return;
        }
        // 2.1.2: a courtship chapter's task done: its moment, before anything else.
        if (line == null && moment(player, npc, null)) {
            return;
        }
        show(player, npc, context, script.main(context, line != null ? line : script.greeting(context)), Kind.MAIN);
    }

    /**
     * 2.1.8: a lesson asked for that needs its proof first ({@code Teaching.proof}): set as it is asked (his words), or how far along it is;
     * true while it isn't done (the lesson waits). Done, it is taught as before, and the proof forgotten.
     */
    private static boolean proofFirst(ServerPlayer player, EtoNpc npc, EtoTalk.Context c) {
        if (!c.teaching().ready()) {
            return false;
        }
        com.hex.kagune.control.npc.Teaching.Lesson next = c.teaching().next();
        com.hex.kagune.control.npc.Teaching.Proof proof = com.hex.kagune.control.npc.Teaching.proof(next);
        if (proof == null) {
            return false;
        }
        String key = npc.persona().key();
        NpcMemory memory = NpcMemoryServer.of(player);
        long bit = com.hex.kagune.control.npc.Teaching.bit(next);
        if (memory.get(key, NpcMemory.PROOF, 0L) != bit) {
            memory.set(key, NpcMemory.PROOF, bit).set(key, NpcMemory.PROOF_COUNT, 0L);
            NpcMemoryServer.save(player);
            player.m_5661_(Component.m_237113_(npc.persona().colour() + npc.displayName() + "'s proof before " + next.name() + ": "
                + proof.bar(0)), false);
            show(player, npc, c, npc.persona().script().main(c, com.hex.kagune.control.npc.Teaching.fill(proof.ask(), proof, 0)), Kind.MAIN);
            return true;
        }
        int done = (int)memory.get(key, NpcMemory.PROOF_COUNT, 0L);
        if (done < proof.need()) {
            show(player, npc, c, npc.persona().script().main(c, com.hex.kagune.control.npc.Teaching.fill(proof.progress(), proof, done)), Kind.MAIN);
            return true;
        }
        memory.forget(key, NpcMemory.PROOF).forget(key, NpcMemory.PROOF_COUNT);
        NpcMemoryServer.save(player);
        return false;
    }

    /** 2.1.8: one more {@code deed} toward any proof a teacher set {@code player} ({@code rank}: a Dove's). */
    static void proved(ServerPlayer player, com.hex.kagune.control.npc.Teaching.Deed deed, int rank) {
        NpcMemory memory = NpcMemoryServer.of(player);
        for (com.hex.kagune.control.npc.Persona persona : com.hex.kagune.control.npc.Persona.ALL) {
            long bit = memory.get(persona.key(), NpcMemory.PROOF, 0L);
            if (bit == 0L) {
                continue;
            }
            for (com.hex.kagune.control.npc.Teaching.Lesson lesson : com.hex.kagune.control.npc.Teaching.of(persona.key())) {
                com.hex.kagune.control.npc.Teaching.Proof proof = com.hex.kagune.control.npc.Teaching.proof(lesson);
                if (com.hex.kagune.control.npc.Teaching.bit(lesson) != bit || proof == null || proof.deed() != deed || rank < proof.rank()) {
                    continue;
                }
                long done = memory.get(persona.key(), NpcMemory.PROOF_COUNT, 0L);
                if (done < proof.need()) {
                    memory.set(persona.key(), NpcMemory.PROOF_COUNT, done + 1);
                    NpcMemoryServer.save(player);
                    player.m_5661_(Component.m_237113_(persona.colour() + proof.bar((int)done + 1)), true);
                }
            }
        }
    }

    /** 1.3.37: how long a half-woken sleeper waits on a word before the talk would get them up (ticks). */
    static final long DROWSY = 600L;

    /** 1.3.37: the page of a sleeper half woken: what they mumble, and back to sleep, a goodnight kiss (their partner), or up. */
    private static EtoTalk.Page drowsy(ServerPlayer player, EtoNpc npc) {
        String persona = npc.persona().key();
        List<EtoTalk.Option> options = new java.util.ArrayList<>();
        options.add(new EtoTalk.Option(NpcTalk.Choice.SAY_1, "Sorry. Go back to sleep.", "sleep:back"));
        if (player.m_20148_().equals(npc.mind.partner)) {
            options.add(new EtoTalk.Option(NpcTalk.Choice.SAY_2, "*kiss them goodnight*", "sleep:kiss"));
        }
        options.add(new EtoTalk.Option(NpcTalk.Choice.SAY_3, "Wake up. I need you.", "sleep:up"));
        options.add(new EtoTalk.Option(NpcTalk.Choice.LEAVE, "", ""));
        return new EtoTalk.Page(com.hex.kagune.control.family.Drowsy.spouse(persona, npc.m_217043_().m_188503_(100)), options);
    }

    /** 1.3.29: the scene she came up to {@code player} with, opened (true), if she is still waiting on them. */
    private static boolean scene(ServerPlayer player, EtoNpc npc, EtoTalk.Context context) {
        SceneServer.Pending pending = SceneServer.take(player, npc.m_19879_());
        com.hex.kagune.control.npc.scene.Scene scene = pending == null ? null : SceneServer.scene(pending.scene());
        if (scene == null) {
            return false;
        }
        if (scene.kind() == com.hex.kagune.control.npc.scene.Scene.Kind.MOMENT) {
            // 2.1.2: she came up with a courtship chapter's moment: it plays (and the chapter closes) if it is still the one due.
            return moment(player, npc, pending.line());
        }
        com.hex.kagune.control.npc.scene.SceneContext sc = SceneServer.context(player, npc, context);
        show(player, npc, context, SceneServer.page(scene, scene.first(), sc, pending.line()), Kind.SCENE);
        return true;
    }

    // ---------------------------------------------------------------- 2.1.2: the courtship

    /**
     * The moment of the courtship's chapter on, if its task is done: the chapter closed (the next on), the game says where they are, and
     * the moment's scene plays ({@code line}: its first words, if already said; null its own). False if no moment is due.
     */
    static boolean moment(ServerPlayer player, EtoNpc npc, String line) {
        String key = npc.persona().key();
        QuestLog.Bond bond = courtBond(player, key);
        com.hex.kagune.control.npc.Courtship.Chapter chapter = com.hex.kagune.control.npc.Courtship.chapter(key, bond);
        if (chapter == null || !com.hex.kagune.control.npc.Courtship.ready(key, bond)) {
            return false;
        }
        ServerLevel level = player.m_284548_();
        long now = level.m_46467_();
        QuestLog.Bond closed = com.hex.kagune.control.npc.Courtship.close(key, bond);
        QuestServer.bond(player, key, closed.regard(closed.regard() + MOMENT_REGARD));
        int done = com.hex.kagune.control.npc.Courtship.closed(closed, com.hex.kagune.control.npc.Courtship.CHAPTERS);
        player.m_5661_(Component.m_237113_(npc.persona().colour() + "\u2764 " + npc.displayName() + ": chapter " + done + " of "
            + com.hex.kagune.control.npc.Courtship.CHAPTERS + ", " + chapter.title() + (done >= com.hex.kagune.control.npc.Courtship.CHAPTERS
            ? ". Every chapter closed: ask " + npc.persona().her() + " out." : ". (Your journal has what comes next.)")), false);
        UkakuFire.sound(level, npc.m_20182_(), "entity.player.levelup", 0.6F, 1.6F);
        EtoBrain.hearts(level, npc, player, 6);
        EtoBrain.listen(npc, player, now);
        com.hex.kagune.control.npc.scene.Scene scene = SceneServer.scene(key + "." + chapter.id());
        EtoTalk.Context c = context(player, npc);
        if (scene == null) {
            show(player, npc, c, npc.persona().script().main(c, "..."), Kind.MAIN);
            return true;
        }
        com.hex.kagune.control.npc.scene.SceneContext sc = SceneServer.context(player, npc, c);
        show(player, npc, c, SceneServer.page(scene, scene.first(), sc, line), Kind.SCENE);
        return true;
    }

    /** Regard a chapter's moment brings, before anything said in it. */
    static final int MOMENT_REGARD = 4;

    /** {@code player}'s bond with {@code key}, as the courtship keeps it (a bond from before it read as its chapters, and saved so). */
    static QuestLog.Bond courtBond(ServerPlayer player, String key) {
        QuestLog.Bond bond = QuestServer.bond(player, key);
        QuestLog.Bond migrated = com.hex.kagune.control.npc.Courtship.migrate(bond, com.hex.kagune.control.npc.Courtship.CHAPTERS);
        if (migrated != bond) {
            QuestServer.bond(player, key, migrated);
        }
        return migrated;
    }

    /** How many of what the courtship's chapter on asks for {@code player} holds (both hands). */
    static int held(ServerPlayer player, String key, QuestLog.Bond bond) {
        int n = 0;
        for (ItemStack held : new ItemStack[] {player.m_21205_(), player.m_21206_()}) {
            if (com.hex.kagune.control.npc.Courtship.wants(key, bond, id(held))) {
                n += held.m_41613_();
            }
        }
        return n;
    }

    /** What the chapter on asks for taken from {@code player}'s hands (as much as it still needs): how many. */
    private static int handOver(ServerPlayer player, String key, QuestLog.Bond bond) {
        int want = com.hex.kagune.control.npc.Courtship.takes(key, bond, held(player, key, bond));
        int taken = 0;
        for (ItemStack held : new ItemStack[] {player.m_21205_(), player.m_21206_()}) {
            if (taken < want && com.hex.kagune.control.npc.Courtship.wants(key, bond, id(held))) {
                int some = Math.min(want - taken, held.m_41613_());
                held.m_41774_(some);
                taken += some;
            }
        }
        return taken;
    }

    /** The dates they could go on now: a few of hers that fit, each an answer, and back. */
    private static EtoTalk.Page dates(ServerPlayer player, EtoNpc npc, EtoTalk.Context c) {
        String key = npc.persona().key();
        int seed = npc.m_217043_().m_188503_(100000);
        long today = day(player.m_284548_());
        if (NpcMemoryServer.of(player).get(key, NpcMemory.DATE_DAY, -1L) == today) {
            return npc.persona().script().sweet(c, com.hex.kagune.control.npc.Courtship.datedToday(key, seed));
        }
        com.hex.kagune.control.npc.scene.SceneContext sc = SceneServer.context(player, npc, c);
        List<com.hex.kagune.control.npc.scene.Scene> fit = new java.util.ArrayList<>(com.hex.kagune.control.npc.scene.SceneBook.fitting(
            SceneServer.book(key), com.hex.kagune.control.npc.scene.Scene.Kind.DATE, sc));
        java.util.Collections.shuffle(fit, new java.util.Random(seed));
        List<EtoTalk.Option> out = new java.util.ArrayList<>();
        for (int i = 0; i < fit.size() && i < DATES_OFFERED; i++) {
            out.add(new EtoTalk.Option(NpcTalk.Choice.SAYS[i], com.hex.kagune.control.npc.scene.SceneBook.fill(fit.get(i).prompt(), sc), "d:" + fit.get(i).key()));
        }
        out.add(new EtoTalk.Option(NpcTalk.Choice.BACK, "", ""));
        return new EtoTalk.Page(com.hex.kagune.control.npc.Courtship.datesAsked(key, seed), out);
    }

    /** At most this many dates are offered at once. */
    static final int DATES_OFFERED = 4;

    /** The date {@code key} ("d:eto.date_rooftop") begun: once a day; its scene plays. */
    private static void date(ServerPlayer player, EtoNpc npc, EtoTalk.Context c, String key) {
        com.hex.kagune.control.npc.scene.Scene scene = SceneServer.scene(key.substring(2));
        String persona = npc.persona().key();
        if (scene == null || scene.kind() != com.hex.kagune.control.npc.scene.Scene.Kind.DATE || !scene.speaker().equals(persona) || !c.dating()) {
            return;
        }
        long today = day(player.m_284548_());
        if (NpcMemoryServer.of(player).get(persona, NpcMemory.DATE_DAY, -1L) == today) {
            show(player, npc, c, npc.persona().script().sweet(c, com.hex.kagune.control.npc.Courtship.datedToday(persona, c.seed())), Kind.SWEET);
            return;
        }
        NpcMemoryServer.set(player, persona, NpcMemory.DATE_DAY, today);
        QuestLog.Bond bond = QuestServer.bond(player, persona);
        QuestServer.bond(player, persona, bond.regard(bond.regard() + DATE_REGARD));
        RomanceServer.romanced(player, persona);
        EtoBrain.giddy(npc, player, player.m_284548_().m_46467_());
        EtoTalk.Context after = context(player, npc);
        com.hex.kagune.control.npc.scene.SceneContext sc = SceneServer.context(player, npc, after);
        show(player, npc, after, SceneServer.page(scene, scene.first(), sc, null), Kind.SCENE);
    }

    /** Regard a date brings, before anything said on it. */
    static final int DATE_REGARD = 3;

    /** 1.3.29: the topics page: hers as her script has them, and her scripted topics with them (a different few each time). */
    static EtoTalk.Page topics(ServerPlayer player, EtoNpc npc, EtoTalk.Context c, String line) {
        EtoTalk.Page own = npc.persona().script().talk(c, line);
        List<com.hex.kagune.control.npc.scene.Scene> extra = new java.util.ArrayList<>(SceneServer.topics(player, npc, c));
        if (extra.isEmpty()) {
            return own;
        }
        List<EtoTalk.Option> keep = new java.util.ArrayList<>();
        for (EtoTalk.Option option : own.options()) {
            if (option.key().startsWith("topic:") && keep.size() < 3) {
                keep.add(option);
            }
        }
        java.util.Collections.shuffle(extra, new java.util.Random(c.seed()));
        List<EtoTalk.Option> out = new java.util.ArrayList<>();
        int slot = 0;
        for (EtoTalk.Option option : keep) {
            out.add(new EtoTalk.Option(NpcTalk.Choice.SAYS[slot++], option.label(), option.key()));
        }
        com.hex.kagune.control.npc.scene.SceneContext sc = SceneServer.context(player, npc, c);
        for (com.hex.kagune.control.npc.scene.Scene scene : extra) {
            if (slot >= NpcTalk.Choice.SAYS.length) {
                break;
            }
            out.add(new EtoTalk.Option(NpcTalk.Choice.SAYS[slot++], com.hex.kagune.control.npc.scene.SceneBook.fill(scene.prompt(), sc), "t:" + scene.key()));
        }
        out.add(new EtoTalk.Option(NpcTalk.Choice.BACK, "", ""));
        return new EtoTalk.Page(own.line(), out);
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
        if (choice == NpcTalk.Choice.ANSWER) {
            // 1.3.29: the Answer key: she came up to them with something; the talk opens on it (from where she stands, near enough).
            if (talk == null && player.m_284548_().m_6815_(npcId) instanceof EtoNpc npc && npc.m_6084_() && npc.m_20280_(player) <= 24.0 * 24.0) {
                open(player, npc);
            }
            return;
        }
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
        // 1.3.29: a scene she came up with may be answered while she is still on her way (further off).
        double keep = talk.kind() == Kind.SCENE ? 16.0 : KEEP;
        if (!(level.m_6815_(npcId) instanceof EtoNpc npc) || !npc.m_6084_() || npc.m_20280_(player) > keep * keep || npc.mind == null) {
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
        if (key.equals("sorry")) {
            // 2.2.0: an apology: forgiven at once, or (hurt again and again) forgiven once she's had a moment.
            boolean now2 = EtoBrain.apologised(npc, player, level.m_46467_());
            String line = com.hex.kagune.control.npc.Tolerance.forgiven(npc.persona().key(), !now2, npc.m_217043_().m_188503_(100));
            EtoTalk.Context after = context(player, npc);
            if (now2) {
                QuestLog.Bond bond = QuestServer.bond(player, npc.persona().key());
                QuestServer.bond(player, npc.persona().key(), bond.regard(bond.regard() + 1));
                EtoBrain.giddy(npc, player, level.m_46467_());
                show(player, npc, after, npc.persona().script().main(after, line), Kind.MAIN);
            } else {
                show(player, npc, after, new EtoTalk.Page(line, List.of(new EtoTalk.Option(NpcTalk.Choice.LEAVE, "", ""))), Kind.CLOSED);
            }
            return;
        }
        if (key.equals("gifts")) {
            show(player, npc, c, script.gifts(c, script.giftsAsked(c.seed())), Kind.GIFTS);
            return;
        }
        if (key.equals("dates")) {
            // 2.1.2: where to go together.
            if (c.dating()) {
                show(player, npc, c, dates(player, npc, c), Kind.SWEET);
            }
            return;
        }
        if (key.startsWith("d:")) {
            date(player, npc, c, key);
            return;
        }
        if (key.startsWith("t:")) {
            // 1.3.29: one of her scripted topics brought up.
            EtoTalk.Page page = SceneServer.topic(player, npc, c, key);
            if (page != null) {
                show(player, npc, c, page, Kind.SCENE);
            }
            return;
        }
        if (key.startsWith("s:")) {
            // 1.3.29: an answer on a scene's page: her reply, what it does, and the page it leads on to (or her own page).
            SceneServer.Answer answer = SceneServer.answer(player, npc, c, key, level.m_46467_());
            if (answer != null) {
                carry(player, npc, c, choice, answer.reply(), answer.topic() ? Kind.TALK : Kind.MAIN, answer.next());
            }
            return;
        }
        if (key.startsWith("sleep:")) {
            // 1.3.37: half woken: back to sleep, kissed goodnight, or up properly (HomeLife gets her up once she is no longer drowsy).
            String persona = npc.persona().key();
            int seed = npc.m_217043_().m_188503_(100);
            if (key.equals("sleep:up")) {
                npc.mind.drowsyUntil = Long.MIN_VALUE;
                show(player, npc, c, script.main(c, com.hex.kagune.control.family.Drowsy.roused(persona, seed)), Kind.MAIN);
            } else if (key.equals("sleep:kiss")) {
                carry(player, npc, c, choice, new EtoTalk.Reply(com.hex.kagune.control.family.Drowsy.kissed(persona, seed), 1, EtoTalk.Effect.CLOSE), Kind.MAIN);
            } else {
                carry(player, npc, c, choice, new EtoTalk.Reply(com.hex.kagune.control.family.Drowsy.settle(persona, seed), 0, EtoTalk.Effect.CLOSE), Kind.MAIN);
            }
            return;
        }
        if (key.equals("cookbook")) {
            // 1.3.41: Touka's cookbook, for the two cocoa beans in your hand.
            net.minecraft.world.item.ItemStack held = player.m_21205_();
            int seed = npc.m_217043_().m_188503_(100);
            net.minecraft.world.item.Item beans = com.hex.kagune.control.ControlItems.vanilla("minecraft:cocoa_beans");
            int count = beans != null && !held.m_41619_() && held.m_41720_() == beans ? held.m_41613_() : 0;
            if (!com.hex.kagune.control.kitchen.Cookbook.affordable(QuestServer.bond(player, npc.persona().key()).regard(), count)) {
                show(player, npc, c, script.main(c, com.hex.kagune.control.kitchen.Cookbook.price(seed)), Kind.MAIN);
                return;
            }
            if (!player.m_7500_()) {
                held.m_41774_(com.hex.kagune.control.kitchen.Cookbook.PRICE);
            }
            net.minecraft.world.item.ItemStack book = new net.minecraft.world.item.ItemStack(com.hex.kagune.control.ControlItems.COOKBOOK.get(), 1);
            if (!player.m_36356_(book)) {
                player.m_36176_(book, false);
            }
            carry(player, npc, c, choice, new EtoTalk.Reply(com.hex.kagune.control.kitchen.Cookbook.given(seed), 2, EtoTalk.Effect.NONE, EtoGesture.NONE),
                Kind.MAIN);
            return;
        }
        if (key.equals("gohome")) {
            // 1.3.29: "Go home, please.": to the home she shares with her spouse, or back where she belongs; the talk ends there.
            Going going = going(player, npc);
            if (going == null) {
                show(player, npc, c, script.main(c, "I'm already home."), Kind.MAIN);
                return;
            }
            EtoBrain.sendHome(npc, level, going.at(), going.shared(), now);
            String line = com.hex.kagune.control.family.Chores.goingHome(npc.persona().key(), going.shared(), npc.persona().home(), c.seed());
            carry(player, npc, c, choice, new EtoTalk.Reply(line, 0, EtoTalk.Effect.CLOSE, EtoGesture.WAVE), Kind.MAIN);
            return;
        }
        if (choice == NpcTalk.Choice.PROPOSE) {
            propose(player, npc, c);
            return;
        }
        if (choice == NpcTalk.Choice.BACK && (talk.kind() == Kind.TOPIC || talk.kind() == Kind.SCENE)) {
            // Back from a topic: to the others.
            show(player, npc, c, topics(player, npc, c, script.topicsAgain(c.seed())), Kind.TALK);
            return;
        }
        Kind after = talk.kind() == Kind.SWEET || talk.kind() == Kind.TALK ? talk.kind() : Kind.MAIN;
        if (choice == NpcTalk.Choice.LESSON && proofFirst(player, npc, c)) {
            return; // 2.1.8: a final kakuja is taught once its proof is done
        }
        if (choice == NpcTalk.Choice.LESSON) {
            // 1.3.29: asked for what they know already (a command, an old quest): "You already know...", and on to the next thing.
            String known = alreadyKnown(player, npc, c.seed());
            if (known != null) {
                EtoTalk.Context fresh = context(player, npc);
                EtoTalk.Reply reply = script.answer(fresh, choice);
                carry(player, npc, fresh, choice, new EtoTalk.Reply(known + " " + reply.line(), reply.regard(), reply.effect(), reply.gesture()), after);
                return;
            }
        }
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
        carry(player, npc, c, choice, reply, stay, null);
    }

    /** 1.3.29: the same, then {@code then} (a scene's next page) if the answer leads on to one and the talk is still open. */
    private static void carry(ServerPlayer player, EtoNpc npc, EtoTalk.Context c, NpcTalk.Choice choice, EtoTalk.Reply reply, Kind stay,
                              EtoTalk.Page then) {
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
                bond = take(player, gift) ? EtoRules.gifted(bond, day) : bond.regard(bond.regard() - reply.regard());
                next = Kind.MAIN;
            }
            case COURT -> {
                // 2.1.2: what the chapter asks for handed over (all of it that it still needs), then its moment if that was the last of it.
                String key = npc.persona().key();
                bond = com.hex.kagune.control.npc.Courtship.migrate(bond, com.hex.kagune.control.npc.Courtship.CHAPTERS);
                int taken = handOver(player, key, bond);
                if (taken > 0) {
                    bond = com.hex.kagune.control.npc.Courtship.progress(key, bond, com.hex.kagune.control.npc.Courtship.Task.GIVE, taken);
                    EtoBrain.gesture(npc.mind, EtoGesture.BOW, now);
                }
                if (com.hex.kagune.control.npc.Courtship.ready(key, bond)) {
                    QuestServer.bond(player, key, bond);
                    moment(player, npc, null);
                    return;
                }
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
            case BEGIN -> {
                // 2.1.2: the courtship begins: the first chapter on.
                bond = EtoRules.begin(bond);
                java.util.List<com.hex.kagune.control.npc.Courtship.Chapter> chapters = com.hex.kagune.control.npc.Courtship.of(npc.persona().key());
                if (!chapters.isEmpty()) {
                    player.m_5661_(Component.m_237113_(npc.persona().colour() + "\u2764 " + npc.displayName() + "'s courtship begins. Chapter 1 of "
                        + chapters.size() + ": " + chapters.get(0).title() + ". (Your journal's Romance tab keeps track.)"), false);
                }
            }
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
            case HOME -> {
                // 1.3.21: their home is here (where the player stands): hers from now on, and their children's.
                net.minecraft.core.BlockPos at = new net.minecraft.core.BlockPos((int)Math.floor(player.m_20185_()), (int)Math.floor(player.m_20186_()),
                    (int)Math.floor(player.m_20189_()));
                Households.Home home = Households.set(player.m_284548_(), player, at);
                npc.homeAt(home.pos());
                player.m_5661_(net.minecraft.network.chat.Component.m_237113_("§d\u2302 This is your home now: " + npc.displayName()
                    + " and your children live here (about " + home.radius() + " blocks round). Beds: the double bed is your spouse's, single beds the children's."), false);
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
        if (then != null && next != Kind.CLOSED && next != Kind.TALK && next != Kind.SWEET) {
            show(player, npc, after, then, Kind.SCENE);
            return;
        }
        show(player, npc, after, page(player, npc, after, next, line), next);
    }

    /** The page {@code kind} with {@code line} hers. */
    private static EtoTalk.Page page(ServerPlayer player, EtoNpc npc, EtoTalk.Context c, Kind kind, String line) {
        Script script = npc.persona().script();
        return switch (kind) {
            case TALK -> topics(player, npc, c, line);
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
        com.hex.kagune.control.family.Genome.Parent theirs = new com.hex.kagune.control.family.Genome.Parent(p.hair(), p.eyes(), p.skin(), true, p.oneEyed(),
            p.kagune(), p.kaguneColour()); // 1.3.43: and its kagune
        java.util.Random random = new java.util.Random(level.m_46467_() ^ player.m_20148_().getLeastSignificantBits());
        // 2.2.1: a ghoul's and a human's child is most often lost (HalfBlood); its fate drawn after the child, so nothing else changes.
        if (p.female()) {
            com.hex.kagune.control.family.Genome genome = com.hex.kagune.control.family.Genome.of(theirs, Looks.parent(player), random);
            double lostAt = genome.half() ? com.hex.kagune.control.family.HalfBlood.lostAt(random.nextDouble(), random.nextDouble()) : -1.0;
            Pregnancies.conceive(level, npcKey, npc.displayName(), playerKey, player.m_36316_().getName(), genome, lostAt);
        } else {
            com.hex.kagune.control.family.Genome genome = com.hex.kagune.control.family.Genome.of(Looks.parent(player), theirs, random);
            double lostAt = genome.half() ? com.hex.kagune.control.family.HalfBlood.lostAt(random.nextDouble(), random.nextDouble()) : -1.0;
            Pregnancies.conceive(level, playerKey, player.m_36316_().getName(), npcKey, npc.displayName(), genome, lostAt);
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
        QuestLog.Bond bond = courtBond(player, npc.persona().key());
        QuestLog log = QuestServer.log(player);
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
            persona.female() != com.hex.kagune.control.Genders.female(player), held(player, persona.key(), bond));
    }

    /**
     * 1.3.29: the lessons {@code npc} would teach {@code player} that they know already some other way (a command, an old quest): marked as
     * told (Eto's as taught), and her words for it; null if there are none.
     */
    private static String alreadyKnown(ServerPlayer player, EtoNpc npc, int seed) {
        List<String> names = knownLessons(player, npc, true);
        return names.isEmpty() ? null : com.hex.kagune.control.npc.Teaching.alreadyKnown(npc.persona().key(), names, seed);
    }

    /** 1.3.29: the names of the lessons {@code npc} has for {@code player} that they know some other way ({@code mark}: and mark them told). */
    private static List<String> knownLessons(ServerPlayer player, EtoNpc npc, boolean mark) {
        List<String> names = new java.util.ArrayList<>();
        if (!KaguneProfile.isUnlocked(player)) {
            return names;
        }
        QuestLog log = QuestServer.log(player);
        com.hex.kagune.control.KagunePath path = PathServer.path(player);
        int mastery = KaguneProfile.mastery(player);
        String key = npc.persona().key();
        if (npc.persona() == com.hex.kagune.control.npc.Persona.ETO) {
            QuestLog.Bond bond = log.bond(key);
            QuestLog.Bond marked = bond;
            for (EtoRules.Lesson lesson : EtoRules.Lesson.values()) {
                Quest quest = QuestBook.get(lesson.quest);
                boolean known = quest != null && log.state(quest, path, mastery) == QuestLog.State.DONE || lesson.gate != null && log.open(lesson.gate);
                if (!bond.has(lesson.flag) && known) {
                    names.add(quest != null ? quest.title() : lesson.name());
                    marked = marked.with(lesson.flag, true);
                }
            }
            if (mark && marked != bond) {
                QuestServer.bond(player, key, marked);
            }
            return names;
        }
        NpcMemory memory = NpcMemoryServer.of(player);
        long told = memory.get(key, NpcMemory.TAUGHT, 0L);
        for (com.hex.kagune.control.npc.Teaching.Lesson lesson : com.hex.kagune.control.npc.Teaching.knownElsewhere(key, path, log::open, told)) {
            names.add(lesson.name());
            told |= com.hex.kagune.control.npc.Teaching.bit(lesson);
        }
        if (mark && !names.isEmpty()) {
            NpcMemoryServer.set(player, key, NpcMemory.TAUGHT, told);
        }
        return names;
    }

    /** 1.3.29: Eto's first page offers her lesson when there is one they know already (she says so, then goes on). */
    private static EtoTalk.Page withLesson(ServerPlayer player, EtoNpc npc, EtoTalk.Page page) {
        if (npc.persona() != com.hex.kagune.control.npc.Persona.ETO || page.options().size() >= S2CNpcTalk.MOST) {
            return page;
        }
        int leave = -1;
        for (int i = 0; i < page.options().size(); i++) {
            NpcTalk.Choice choice = page.options().get(i).choice();
            if (choice == NpcTalk.Choice.LESSON || choice == NpcTalk.Choice.TRIAL) {
                return page;
            }
            if (choice == NpcTalk.Choice.LEAVE) {
                leave = i;
            }
        }
        if (leave < 0 || knownLessons(player, npc, false).isEmpty()) {
            return page;
        }
        List<EtoTalk.Option> out = new java.util.ArrayList<>(page.options());
        out.add(Math.max(0, leave - 1), new EtoTalk.Option(NpcTalk.Choice.LESSON, "", ""));
        return new EtoTalk.Page(page.line(), out);
    }

    /** 1.3.29: where "Go home, please." sends her: the home she shares with her spouse ({@code shared}), or where she belongs. */
    record Going(net.minecraft.core.BlockPos at, boolean shared) {
    }

    /**
     * 1.3.29: where she would go if sent home now, or null if she is home already (or her home is in another world): following someone,
     * or settled anywhere but her home (the one she shares with her spouse once they have one; else her own place).
     */
    static Going going(ServerPlayer player, EtoNpc npc) {
        ServerLevel level = player.m_284548_();
        boolean following = npc.following() != null;
        net.minecraft.core.BlockPos home = npc.home();
        Households.Home house = Households.of(level, EtoBrain.spouse(npc));
        if (house != null) {
            if (!house.in(level) || !following && house.at(home)) {
                return null;
            }
            return new Going(house.pos(), true);
        }
        net.minecraft.core.BlockPos origin = npc.origin();
        if (origin == null || home == null || UniqueNpcs.overworld(level) != level) {
            return null;
        }
        double dx = origin.m_123341_() - home.m_123341_();
        double dy = origin.m_123342_() - home.m_123342_();
        double dz = origin.m_123343_() - home.m_123343_();
        return following || dx * dx + dy * dy + dz * dz > 6.0 * 6.0 ? new Going(origin, false) : null;
    }

    /**
     * 1.3.29: her first page offers "Go home, please." next to "Follow me." / "Wait here." when she is away from home (following them,
     * waiting where she was told to). Only where she would follow or wait for them at all, only if there is room.
     */
    private static EtoTalk.Page withHome(ServerPlayer player, EtoNpc npc, EtoTalk.Page page) {
        int at = -1;
        for (int i = 0; i < page.options().size(); i++) {
            NpcTalk.Choice choice = page.options().get(i).choice();
            if (choice == NpcTalk.Choice.SAY_7) {
                return page;
            }
            if (choice == NpcTalk.Choice.FOLLOW || choice == NpcTalk.Choice.STAY) {
                at = i;
            }
        }
        if (at < 0 || page.options().size() >= S2CNpcTalk.MOST || going(player, npc) == null) {
            return page;
        }
        List<EtoTalk.Option> out = new java.util.ArrayList<>(page.options());
        out.add(at + 1, new EtoTalk.Option(NpcTalk.Choice.SAY_7, "Go home, please.", "gohome"));
        return new EtoTalk.Page(page.line(), out);
    }

    /**
     * 1.3.41: Touka's first page offers her cookbook ("Got any recipes? For ghouls.") once she likes you
     * ({@link com.hex.kagune.control.kitchen.Cookbook#REGARD}), before the goodbye, if there is room. She asks two cocoa beans for it.
     */
    private static EtoTalk.Page withCookbook(ServerPlayer player, EtoNpc npc, EtoTalk.Page page) {
        if (npc.persona() != com.hex.kagune.control.npc.Persona.TOUKA || page.options().size() >= S2CNpcTalk.MOST
            || QuestServer.bond(player, npc.persona().key()).regard() < com.hex.kagune.control.kitchen.Cookbook.REGARD) {
            return page;
        }
        int at = page.options().size();
        for (int i = 0; i < page.options().size(); i++) {
            NpcTalk.Choice choice = page.options().get(i).choice();
            if (choice == NpcTalk.Choice.SAY_6) {
                return page;
            }
            if (choice == NpcTalk.Choice.LEAVE || choice == NpcTalk.Choice.INSULT || choice == NpcTalk.Choice.TEASE) {
                at = Math.min(at, i);
            }
        }
        List<EtoTalk.Option> out = new java.util.ArrayList<>(page.options());
        out.add(at, new EtoTalk.Option(NpcTalk.Choice.SAY_6, com.hex.kagune.control.kitchen.Cookbook.ASK, "cookbook"));
        return new EtoTalk.Page(page.line(), out);
    }

    private static void show(ServerPlayer player, EtoNpc npc, EtoTalk.Context context, EtoTalk.Page page, Kind kind) {
        if (kind == Kind.MAIN) {
            page = withHome(player, npc, page);
            page = withCookbook(player, npc, page);
            page = withLesson(player, npc, page);
        }
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
        // 1.3.29: remembered as this teacher's (asked again, they never say it was known some other way).
        long told = NpcMemoryServer.of(player).get(persona.key(), NpcMemory.TAUGHT, 0L);
        NpcMemoryServer.set(player, persona.key(), NpcMemory.TAUGHT, told | com.hex.kagune.control.npc.Teaching.bit(lesson));
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

    // ---------------------------------------------------------------- deaths: a courtship's kills, a human's pact, and hers

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
        // 2.1.8: Ken's proofs: Doves of a rank, wild ghouls killed in Ukaku's first kakuja.
        if (dead instanceof com.hex.kagune.control.npc.CcgAgent agent) {
            proved(killer, com.hex.kagune.control.npc.Teaching.Deed.DOVES, AgentBrain.rankLevel(agent));
        }
        if (dead instanceof GhoulNpc && UkakuServer.isUkaku(killer) && KaguneProfile.isActive(killer) && UkakuServer.form(killer).kakuja()) {
            proved(killer, com.hex.kagune.control.npc.Teaching.Deed.GHOULS_IN_KAKUJA, 0);
        }
        // 2.1.2: a courtship chapter of kills counts. 2.1.5: Doves of a rank (alone or with the companion along), Pack ghouls, ghouls
        // preying on a village, as well as before (at night, at night with them along, any wild ghoul).
        int rank = dead instanceof com.hex.kagune.control.npc.CcgAgent agent ? AgentBrain.rankLevel(agent) : -1;
        boolean nearHumans = dead instanceof GhoulNpc && !killer.m_284548_().m_6443_(com.hex.kagune.control.npc.HumanNpc.class,
            dead.m_20191_().m_82400_(CourtshipServer.VILLAGE_NEAR), human -> human.m_6084_()).isEmpty();
        for (com.hex.kagune.control.npc.Persona persona : com.hex.kagune.control.npc.Persona.ALL) {
            QuestLog.Bond bond = courtBond(killer, persona.key());
            com.hex.kagune.control.npc.Courtship.Chapter chapter = com.hex.kagune.control.npc.Courtship.chapter(persona.key(), bond);
            if (chapter == null || EtoRules.count(bond) >= chapter.need()) {
                continue;
            }
            boolean along = CourtshipServer.along(killer, persona.key());
            boolean counts = switch (chapter.task()) {
                case KILLS -> dead instanceof Enemy && night;
                case KILLS_WITH -> dead instanceof Enemy && night && along;
                case GHOUL -> dead instanceof GhoulNpc;
                case DOVES -> rank >= chapter.rank();
                case DOVES_WITH -> rank >= chapter.rank() && along;
                case GHOUL_PACK -> dead instanceof GhoulNpc ghoul && ghoul.identity() != null
                    && ghoul.identity().temper() == com.hex.kagune.control.npc.NpcTemper.PACK;
                case VILLAGE_GHOUL -> nearHumans;
                default -> false;
            };
            if (counts) {
                QuestLog.Bond after = com.hex.kagune.control.npc.Courtship.progress(persona.key(), bond, chapter.task(), 1);
                QuestServer.bond(killer, persona.key(), after);
                CourtshipServer.tell(killer, persona, after);
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
