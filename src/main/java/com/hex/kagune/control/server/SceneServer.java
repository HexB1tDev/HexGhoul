package com.hex.kagune.control.server;

import com.hex.kagune.control.ControlItems;
import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.family.ChildStage;
import com.hex.kagune.control.family.ChildTemper;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CNpcApproach;
import com.hex.kagune.control.npc.ChildNpc;
import com.hex.kagune.control.npc.EtoGesture;
import com.hex.kagune.control.npc.EtoMood;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.EtoTalk;
import com.hex.kagune.control.npc.EtoVoice;
import com.hex.kagune.control.npc.NpcTalk;
import com.hex.kagune.control.npc.scene.Scene;
import com.hex.kagune.control.npc.scene.SceneBook;
import com.hex.kagune.control.npc.scene.SceneContext;
import com.hex.kagune.control.quest.QuestLog;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.3.29: the scenes, played (their words are in {@code data/hexkagune/talk/*.txt}, read by {@link SceneBook}).
 *
 * <p>Now and then someone who cares about the player comes up to them with something to say: a companion to their partner (every few
 * minutes they are near, sooner when she is clingy or lonely) or to a friend (less often), a child to its parent. They walk up, a card
 * pops up with their words ({@code S2CNpcApproach}), and they wait: the Answer key (or a right-click on them) opens the talk screen on
 * what they came with, a scene picked by who the player is to them, their mood and what is going on (night, rain, the player hurt, a
 * baby on the way, the children...). Each answer has its own reply and does its own thing (a hug, their hand, a gift, a sulk); some lead
 * on. Left unanswered, they say so, and it stings a little (a clingy partner's regard, a child's mood).
 *
 * <p>The player can bring things up too: every speaker's topics join its talk page (a companion's "Talk a while", a child's "Talk").
 * Never two people at once, never more than one every {@link #BETWEEN} ticks, never in the middle of another talk.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class SceneServer {
    /** How long they wait for an answer (ticks), and the least time between two people coming up to the same player. */
    static final long WAIT = 500L;
    static final long BETWEEN = 1800L;
    /** How many of a speaker's scenes are remembered per player, so they are not said again soon. */
    static final int RECENT = 18;

    private SceneServer() {
    }

    // ---------------------------------------------------------------- the scripts

    private static final Map<String, List<Scene>> BOOKS = new HashMap<>();

    /** {@code speaker}'s scenes, read once from the mod's jar (a mistake in it is logged; the scenes that read cleanly are kept). */
    static synchronized List<Scene> book(String speaker) {
        return BOOKS.computeIfAbsent(speaker, SceneServer::load);
    }

    private static List<Scene> load(String speaker) {
        try (InputStream in = SceneServer.class.getResourceAsStream("/data/hexkagune/talk/" + speaker + ".txt")) {
            if (in == null) {
                KaguneMod.LOGGER.info("HexKagune: no talk script for " + speaker);
                return List.of();
            }
            SceneBook.Parsed parsed = SceneBook.parse(speaker, new String(in.readAllBytes(), StandardCharsets.UTF_8));
            for (String error : parsed.errors()) {
                KaguneMod.LOGGER.info("HexKagune talk/" + speaker + ".txt: " + error);
            }
            return parsed.scenes();
        } catch (IOException | RuntimeException failure) {
            KaguneMod.LOGGER.error("HexKagune could not read the talk script for " + speaker, failure);
            return List.of();
        }
    }

    /** The scene {@code key} ("eto.rain_window"), or null. */
    static Scene scene(String key) {
        int dot = key.indexOf('.');
        if (dot <= 0) {
            return null;
        }
        for (Scene scene : book(key.substring(0, dot))) {
            if (scene.key().equals(key)) {
                return scene;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- who waits on whom

    /** Someone came up to a player with {@code scene} and waits ({@code line}: what they said, so the talk opens on the same words). */
    record Pending(int npc, String scene, String line, long until, boolean child, String about) {
    }

    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private static final Map<String, ArrayDeque<String>> RECENT_SAID = new HashMap<>();
    private static final Map<UUID, Long> LAST = new HashMap<>();
    /** When each speaker may next come up to each player ("eto|<uuid>", "child:<id>|<uuid>"). */
    private static final Map<String, Long> NEXT = new HashMap<>();
    /** A child brushed off: sad until then (child id). */
    private static final Map<String, Long> SAD = new HashMap<>();
    /** A child praised or taught: proud until then. */
    private static final Map<String, Long> PROUD = new HashMap<>();
    /** A topic's answer paid once a day ("<uuid>|<scene>|<page>|<i>" to its day). */
    private static final Map<String, Long> PAID = new HashMap<>();

    /** The one waiting on {@code player} if it is {@code npc} and still waiting, taken off them (its talk is opening now). */
    static Pending take(ServerPlayer player, int npc) {
        Pending pending = PENDING.get(player.m_20148_());
        if (pending == null || pending.npc() != npc) {
            return null;
        }
        PENDING.remove(player.m_20148_());
        ControlNetwork.sendTo(player, new S2CNpcApproach(npc, "", "", "", 0));
        long now = player.m_9236_().m_46467_();
        return now <= pending.until() + 40L ? pending : null;
    }

    /** True if nobody is waiting on {@code player}, nobody came up to them a moment ago, and they are not in another talk. */
    static boolean free(ServerPlayer player, long now) {
        Long last = LAST.get(player.m_20148_());
        return !PENDING.containsKey(player.m_20148_()) && (last == null || now - last >= BETWEEN) && !EtoServer.talking(player)
            && !ChildTalk.talking(player) && player.m_6084_() && !player.m_5803_();
    }

    /**
     * True if {@code who} may come up to {@code player} now: its own time has come ({@code every}: about how long between, ticks; the
     * first time it is set, a while off, so nobody comes the moment they arrive).
     */
    static boolean due(String who, ServerPlayer player, long now, long every, java.util.Random random) {
        String key = who + "|" + player.m_20148_();
        Long next = NEXT.get(key);
        if (next == null) {
            NEXT.put(key, now + every / 3 + (long)(random.nextDouble() * every / 2));
            return false;
        }
        return now >= next && free(player, now);
    }

    private static void later(String who, ServerPlayer player, long now, long every, java.util.Random random) {
        NEXT.put(who + "|" + player.m_20148_(), now + (long)(every * (0.75 + random.nextDouble() * 0.5)));
    }

    private static List<String> recent(ServerPlayer player, String speaker) {
        return new ArrayList<>(RECENT_SAID.getOrDefault(player.m_20148_() + "|" + speaker, new ArrayDeque<>()));
    }

    private static void said(ServerPlayer player, String speaker, Scene scene) {
        ArrayDeque<String> said = RECENT_SAID.computeIfAbsent(player.m_20148_() + "|" + speaker, k -> new ArrayDeque<>());
        said.remove(scene.key());
        said.addLast(scene.key());
        while (said.size() > RECENT) {
            said.removeFirst();
        }
    }

    // ---------------------------------------------------------------- companions

    /** About how long between a companion coming up to its partner (ticks), by her mood; and to a friend. */
    static long every(EtoMood mood) {
        return switch (mood) {
            case CLINGY, LONELY, AFFECTIONATE, GIDDY -> 3600L;
            case JEALOUS, SULKY -> 5400L;
            case SLEEPY, BORED -> 6000L;
            default -> 4800L;
        };
    }

    static final long FRIEND_EVERY = 12000L;

    /** Her partner near: time for her to come up to them with something? (Called now and then from her mind.) */
    static boolean partnerTick(EtoNpc npc, ServerPlayer partner, long now) {
        EtoMood mood = EtoBrain.mood(npc, partner, now);
        java.util.Random random = new java.util.Random(npc.m_217043_().m_188503_(Integer.MAX_VALUE));
        String who = npc.persona().key();
        if (!due(who, partner, now, every(mood), random)) {
            return false;
        }
        later(who, partner, now, every(mood), random);
        return approach(npc, partner, now);
    }

    /** A friend near (not her partner): now and then she comes up to them too. */
    static boolean friendTick(EtoNpc npc, ServerPlayer friend, long now) {
        java.util.Random random = new java.util.Random(npc.m_217043_().m_188503_(Integer.MAX_VALUE));
        String who = npc.persona().key();
        if (!due(who, friend, now, FRIEND_EVERY, random)) {
            return false;
        }
        later(who, friend, now, FRIEND_EVERY, random);
        return approach(npc, friend, now);
    }

    /** She comes up to {@code player} with something to say (a scene that fits them both now). False if nothing fits. */
    static boolean approach(EtoNpc npc, ServerPlayer player, long now) {
        if (npc.mind == null || !free(player, now)) {
            return false;
        }
        EtoTalk.Context c = EtoServer.context(player, npc);
        SceneContext sc = context(player, npc, c);
        if (!Set.of("friendly", "close", "partner", "wife").contains(sc.tier())) {
            return false;
        }
        String speaker = npc.persona().key();
        Scene scene = SceneBook.pick(book(speaker), Scene.Kind.APPROACH, sc, npc.m_217043_().m_188500_(), recent(player, speaker));
        if (scene == null) {
            return false;
        }
        said(player, speaker, scene);
        String line = SceneBook.fill(SceneBook.one(scene.first().lines(), sc.seed()), sc);
        PENDING.put(player.m_20148_(), new Pending(npc.m_19879_(), scene.key(), line, now + WAIT, false, sc.tier() + "|" + sc.mood()));
        LAST.put(player.m_20148_(), now);
        ControlNetwork.sendTo(player, new S2CNpcApproach(npc.m_19879_(), npc.displayName(), npc.persona().colour(), line, (int)WAIT));
        EtoBrain.walkUp(npc, npc.mind, player, EtoGesture.WAVE, now);
        return true;
    }

    /** What a scene of hers is picked and filled by: who they are to her, her mood, the hour and the weather, their family. */
    static SceneContext context(ServerPlayer player, EtoNpc npc, EtoTalk.Context c) {
        ServerLevel level = player.m_284548_();
        String tier = switch (EtoVoice.of(c)) {
            case CLAIMED -> "claimed";
            case WARY -> "wary";
            case FRIENDLY -> "friendly";
            case CLOSE -> "close";
            case PARTNER -> "partner";
            case WIFE -> "wife";
        };
        Set<String> flags = new HashSet<>();
        hours(flags, c.dayTime());
        weather(flags, level);
        if (player.m_21223_() < player.m_21233_() * 0.5F) {
            flags.add("hurt");
        }
        flags.add(c.ghoul() ? "ghoul" : "human");
        flags.add(com.hex.kagune.control.Genders.female(player) ? "woman" : "man");
        if (c.pregnant()) {
            flags.add("pregnant");
        }
        Map<String, String> names = new HashMap<>();
        List<Children.Child> kids = new ArrayList<>();
        if (c.family().children() > 0) {
            String us = Pregnancies.npc(npc.persona().key());
            for (Children.Child child : Children.of(level, Pregnancies.player(player.m_20148_()))) {
                if (child.parent(us)) {
                    kids.add(child);
                }
            }
        }
        if (!kids.isEmpty()) {
            flags.add("kids");
            long now = level.m_46467_();
            for (Children.Child child : kids) {
                flags.add(switch (child.stage(now)) {
                    case BABY -> "baby";
                    case TODDLER -> "toddler";
                    case CHILD -> "kid";
                    case TEEN -> "teen";
                });
            }
            Children.Child one = kids.get(Math.floorMod(c.seed(), kids.size()));
            names.put("kid", one.name);
            names.put("kids", kids.size() == 1 ? one.name : "the kids");
        }
        if (c.daysAway() >= 2L) {
            flags.add("away");
        }
        if (c.dating() && c.daysTogether() <= 2L) {
            flags.add("new");
        }
        if (c.dating() && c.daysTogether() >= 14L) {
            flags.add("long");
        }
        Households.Home home = Households.of(level, player.m_20148_());
        if (home != null && home.at(BlockPos.m_274561_(player.m_20185_(), player.m_20186_(), player.m_20189_()))) {
            flags.add("home");
        }
        if (c.following()) {
            flags.add("following");
        }
        if (c.meat()) {
            flags.add("meat");
        }
        if (c.book()) {
            flags.add("book");
        }
        if (c.flower()) {
            flags.add("flower");
        }
        BlockPos origin = npc.origin();
        if (origin != null && npc.m_20238_(new net.minecraft.world.phys.Vec3(origin.m_123341_() + 0.5, origin.m_123342_(), origin.m_123343_() + 0.5)) < 28.0 * 28.0) {
            flags.add("ownplace");
        }
        if (c.mixed()) {
            flags.add("mixed");
        }
        if (c.granted()) {
            flags.add("taught");
        }
        names.put("name", c.name());
        if (c.claimedBy() != null) {
            names.put("partner", c.claimedBy());
        }
        names.put("self", npc.displayName());
        names.put("she", npc.persona().she());
        names.put("her", npc.persona().her());
        return new SceneContext(tier, c.mood().word(), "", Set.copyOf(flags), Map.copyOf(names), c.seed());
    }

    /** The talk page of {@code scene}'s {@code page} ({@code line}: what she says; null, the page's own), each answer keyed to it. */
    static EtoTalk.Page page(Scene scene, Scene.Page page, SceneContext sc, String line) {
        List<EtoTalk.Option> out = new ArrayList<>();
        for (int i = 0; i < page.options().size() && i < NpcTalk.Choice.SAYS.length; i++) {
            out.add(new EtoTalk.Option(NpcTalk.Choice.SAYS[i], SceneBook.fill(page.options().get(i).label(), sc),
                "s:" + scene.key() + ":" + page.id() + ":" + i));
        }
        out.add(new EtoTalk.Option(scene.kind() == Scene.Kind.TOPIC ? NpcTalk.Choice.BACK : NpcTalk.Choice.LEAVE, "", ""));
        String said = line != null ? line : SceneBook.fill(SceneBook.one(page.lines(), sc.seed()), sc);
        return new EtoTalk.Page(said, out);
    }

    /** What an answer on a scene brings: her reply (its words, regard, deed, gesture), and the page it leads to (null: her main page). */
    record Answer(EtoTalk.Reply reply, EtoTalk.Page next, boolean topic) {
    }

    /** The answer {@code key} ("s:eto.rain_window:start:1") chosen by {@code player} on a page of hers. Null if it is no answer of hers. */
    static Answer answer(ServerPlayer player, EtoNpc npc, EtoTalk.Context c, String key, long now) {
        Option found = option(key);
        if (found == null || !found.scene().speaker().equals(npc.persona().key())) {
            return null;
        }
        SceneContext sc = context(player, npc, c).reseed(npc.m_217043_().m_188503_(100000));
        Scene.Option option = found.option();
        String line = option.replies().isEmpty() ? null : SceneBook.fill(SceneBook.one(option.replies(), sc.seed()), sc);
        EtoTalk.Effect effect = EtoTalk.Effect.NONE;
        EtoGesture gesture = EtoGesture.NONE;
        boolean together = c.dating();
        for (String word : option.effects()) {
            switch (word) {
                case "hug" -> effect = EtoTalk.Effect.HUG;
                case "cling" -> effect = together ? EtoTalk.Effect.CLING : effect;
                case "hand" -> effect = together ? EtoTalk.Effect.HAND : effect;
                case "kiss" -> effect = together ? EtoTalk.Effect.KISS : effect;
                case "follow" -> effect = EtoTalk.Effect.FOLLOW;
                case "stay" -> effect = EtoTalk.Effect.STAY;
                case "talk" -> effect = EtoTalk.Effect.TALK;
                case "sweet" -> effect = together ? EtoTalk.Effect.SWEET : effect;
                case "forgive" -> effect = EtoTalk.Effect.FORGIVE;
                case "night" -> effect = together && c.night() && !c.nightUsed() ? EtoTalk.Effect.NIGHT : effect;
                case "close" -> effect = EtoTalk.Effect.CLOSE;
                case "peck" -> {
                    if (together) {
                        EtoBrain.walkUp(npc, npc.mind, player, EtoGesture.PECK, now);
                        effect = EtoTalk.Effect.CLOSE;
                    }
                }
                case "sulk" -> npc.mind.sulk.put(player.m_20148_(), now + 2400L);
                case "giddy" -> EtoBrain.giddy(npc, player, now);
                case "hearts" -> EtoBrain.hearts(player.m_284548_(), npc, player, 5);
                default -> {
                    if (word.startsWith("gift:")) {
                        give(player, word.substring(5));
                    } else {
                        try {
                            gesture = EtoGesture.valueOf(word.toUpperCase(java.util.Locale.ROOT));
                        } catch (IllegalArgumentException unknown) {
                            // (the tests keep every word known)
                        }
                    }
                }
            }
        }
        int regard = option.regard();
        if (found.scene().kind() == Scene.Kind.TOPIC && regard > 0 && !paid(player, key, now)) {
            regard = 0; // a topic's kind words count once a day
        }
        EtoTalk.Page next = null;
        if (option.next() != null) {
            Scene.Page to = found.scene().page(option.next());
            if (to != null) {
                next = page(found.scene(), to, sc, line);
            }
        }
        if (line == null) {
            line = next != null ? next.line() : "...";
        }
        return new Answer(new EtoTalk.Reply(line, regard, effect, gesture), next, found.scene().kind() == Scene.Kind.TOPIC);
    }

    /** True the first time today {@code player} picked {@code key} (and marks it). */
    private static boolean paid(ServerPlayer player, String key, long now) {
        String id = player.m_20148_() + "|" + key;
        long day = Math.floorDiv(now, 24000L);
        Long was = PAID.put(id, day);
        if (PAID.size() > 20000) {
            PAID.clear();
        }
        return was == null || was != day;
    }

    /** The topics of hers that fit now, as answers on her talk page ({@code room}: how many at most), a different few each time. */
    static List<Scene> topics(ServerPlayer player, EtoNpc npc, EtoTalk.Context c) {
        SceneContext sc = context(player, npc, c);
        return SceneBook.fitting(book(npc.persona().key()), Scene.Kind.TOPIC, sc);
    }

    /** {@code key} ("t:eto.reading") opened: her answer, and what can be said back. */
    static EtoTalk.Page topic(ServerPlayer player, EtoNpc npc, EtoTalk.Context c, String key) {
        Scene scene = scene(key.substring(2));
        if (scene == null || scene.kind() != Scene.Kind.TOPIC || !scene.speaker().equals(npc.persona().key())) {
            return null;
        }
        SceneContext sc = context(player, npc, c).reseed(npc.m_217043_().m_188503_(100000));
        return page(scene, scene.first(), sc, null);
    }

    record Option(Scene scene, Scene.Page page, Scene.Option option) {
    }

    /** The answer a key names ("s:<speaker>.<scene>:<page>:<index>"), or null. */
    static Option option(String key) {
        if (!key.startsWith("s:")) {
            return null;
        }
        String[] parts = key.substring(2).split(":");
        if (parts.length != 3) {
            return null;
        }
        Scene scene = scene(parts[0]);
        if (scene == null) {
            return null;
        }
        Scene.Page page = scene.page(parts[1]);
        int index;
        try {
            index = Integer.parseInt(parts[2]);
        } catch (NumberFormatException bad) {
            return null;
        }
        if (page == null || index < 0 || index >= page.options().size()) {
            return null;
        }
        return new Option(scene, page, page.options().get(index));
    }

    /** A gift into {@code player}'s hands (or at their feet): one of {@link com.hex.kagune.control.npc.scene.SceneWords#GIFTS}. */
    static void give(ServerPlayer player, String item) {
        ItemStack stack = "ghoul_meat".equals(item) ? new ItemStack(ControlItems.GHOUL_MEAT.get(), 1) : EtoBrain.stack("minecraft:" + item);
        if (stack.m_41619_()) {
            return;
        }
        if (!player.m_36356_(stack)) {
            player.m_36176_(stack, false);
        }
    }

    // ---------------------------------------------------------------- children

    /** About how long between a child coming up to a parent (ticks). */
    static final long CHILD_EVERY = 4200L;

    /** A parent near: time for the child to come up to them with something? */
    static boolean childTick(ChildNpc npc, Children.Child child, ServerPlayer parent, long now) {
        java.util.Random random = new java.util.Random(npc.m_217043_().m_188503_(Integer.MAX_VALUE));
        String who = "child:" + child.id;
        if (!due(who, parent, now, CHILD_EVERY, random)) {
            return false;
        }
        later(who, parent, now, CHILD_EVERY, random);
        return approach(npc, child, parent, now);
    }

    /** The child comes up to its parent with something. */
    static boolean approach(ChildNpc npc, Children.Child child, ServerPlayer parent, long now) {
        if (npc.mind == null || !free(parent, now) || npc.m_5803_()) {
            return false;
        }
        SceneContext sc = context(parent, npc, child, now);
        Scene scene = SceneBook.pick(book("child"), Scene.Kind.APPROACH, sc, npc.m_217043_().m_188500_(), recent(parent, "child:" + child.id));
        if (scene == null) {
            return false;
        }
        said(parent, "child:" + child.id, scene);
        String line = SceneBook.fill(SceneBook.one(scene.first().lines(), sc.seed()), sc);
        PENDING.put(parent.m_20148_(), new Pending(npc.m_19879_(), scene.key(), line, now + WAIT, true, child.id));
        LAST.put(parent.m_20148_(), now);
        ControlNetwork.sendTo(parent, new S2CNpcApproach(npc.m_19879_(), child.name, "§b", line, (int)WAIT));
        if (!parent.m_20148_().equals(npc.mind.carrier)) {
            com.hex.kagune.control.family.ChildMoment hello = sc.tier().equals("baby") ? com.hex.kagune.control.family.ChildMoment.CLAP
                : com.hex.kagune.control.family.ChildMoment.WAVE;
            ChildBrain.start(npc, hello, parent, now);
        }
        return true;
    }

    /** What a child's scene is picked and filled by: its age, its temperament, its mood, who it talks to, its family. */
    static SceneContext context(ServerPlayer player, ChildNpc npc, Children.Child child, long now) {
        ServerLevel level = player.m_284548_();
        ChildStage stage = child.stage(now);
        ChildTemper temper = ChildTemper.of(child.id, child.mother, child.father);
        Set<String> flags = new HashSet<>();
        long dayTime = level.m_46468_();
        hours(flags, dayTime);
        weather(flags, level);
        long time = Math.floorMod(dayTime, 24000L);
        boolean night = time >= 12542L && time <= 23000L;
        boolean late = time >= 18000L && time <= 23500L;
        if (player.m_21223_() < player.m_21233_() * 0.5F) {
            flags.add("hurt");
        }
        boolean ghoul = child.genome().ghoul();
        flags.add(ghoul ? "ghoul" : "human");
        flags.add(child.genome().gender().female() ? "girl" : "boy");
        boolean mother = ChildBrain.isMother(child, player);
        flags.add(mother ? "mother" : "father");
        flags.add(com.hex.kagune.control.Genders.female(player) ? "woman" : "man");
        String otherKey = mother ? child.father : child.mother;
        String otherName = mother ? child.fatherName : child.motherName;
        if (otherKey != null && otherKey.startsWith("npc:")) {
            flags.add("npcparent");
            String which = otherKey.substring(4);
            if (Set.of("eto", "touka", "ken").contains(which)) {
                flags.add(which);
            }
        }
        List<Children.Child> siblings = new ArrayList<>();
        for (Children.Child other : Children.of(level, Pregnancies.player(player.m_20148_()))) {
            if (!other.id.equals(child.id)) {
                siblings.add(other);
            }
        }
        if (!siblings.isEmpty()) {
            flags.add("siblings");
        }
        if (npc.mind != null && player.m_20148_().equals(npc.mind.carrier)) {
            flags.add("carried");
        }
        if (child.skill > 0) {
            flags.add("taught");
        }
        Households.Home home = Households.forChild(level, child);
        if (home != null && home.at(BlockPos.m_274561_(npc.m_20185_(), npc.m_20186_(), npc.m_20189_()))) {
            flags.add("home");
        }
        if (player.m_20148_().equals(npc.following())) {
            flags.add("following");
        }
        Long sad = SAD.get(child.id);
        Long proud = PROUD.get(child.id);
        boolean taughtToday = child.lesson != Long.MIN_VALUE && now - child.lesson < 24000L || proud != null && proud > now;
        boolean danger = npc.mind != null && npc.mind.target != 0;
        boolean missed = npc.mind != null && npc.mind.reunited != Long.MIN_VALUE && now - npc.mind.reunited < 2400L;
        boolean lonely = npc.mind != null && npc.mind.reunited != Long.MIN_VALUE && now - npc.mind.reunited < 6000L;
        ChildTemper.Mood mood = ChildTemper.mood(npc.m_21223_() < npc.m_21233_() * 0.6F, sad != null && sad > now, level.m_46470_(), danger, missed,
            taughtToday, night, late, lonely, npc.m_217043_().m_188500_());
        Map<String, String> names = new HashMap<>();
        String call = com.hex.kagune.control.family.ChildLines.call(stage, mother);
        names.put("call", call);
        names.put("other", com.hex.kagune.control.family.ChildLines.call(stage, !mother));
        names.put("othername", otherName == null || otherName.isEmpty() ? com.hex.kagune.control.family.ChildLines.call(stage, !mother) : otherName);
        names.put("me", child.name);
        names.put("name", player.m_36316_().getName());
        if (!siblings.isEmpty()) {
            names.put("sib", siblings.get(npc.m_217043_().m_188503_(siblings.size())).name);
        }
        String tier = switch (stage) {
            case BABY -> "baby";
            case TODDLER -> "toddler";
            case CHILD -> "child";
            case TEEN -> "teen";
        };
        return new SceneContext(tier, mood.word(), temper.word(), Set.copyOf(flags), Map.copyOf(names), npc.m_217043_().m_188503_(100000));
    }

    /** A child's answer: its words, what it does (moments, deeds, a gift), and the page it leads to (keys and labels, or null). */
    record ChildAnswer(String line, Scene.Option option, Scene scene, Scene.Page next, SceneContext context) {
    }

    /** The answer {@code key} chosen on a page of {@code child}'s. */
    static ChildAnswer childAnswer(ServerPlayer player, ChildNpc npc, Children.Child child, String key, long now) {
        Option found = option(key);
        if (found == null || !"child".equals(found.scene().speaker())) {
            return null;
        }
        SceneContext sc = context(player, npc, child, now);
        Scene.Option option = found.option();
        String line = option.replies().isEmpty() ? null : SceneBook.fill(SceneBook.one(option.replies(), sc.seed()), sc);
        if (option.does("sad")) {
            SAD.put(child.id, now + 2400L);
        }
        if (option.does("proud")) {
            PROUD.put(child.id, now + 6000L);
            SAD.remove(child.id);
        }
        if (option.regard() > 0) {
            SAD.remove(child.id);
        }
        String gift = option.gift();
        if (gift != null) {
            give(player, gift);
        }
        Scene.Page next = option.next() == null ? null : found.scene().page(option.next());
        if (line == null) {
            line = next != null ? SceneBook.fill(SceneBook.one(next.lines(), sc.seed()), sc) : "...";
        }
        return new ChildAnswer(line, option, found.scene(), next, sc);
    }

    /** A child's topics that fit now. */
    static List<Scene> childTopics(ServerPlayer player, ChildNpc npc, Children.Child child, long now) {
        return SceneBook.fitting(book("child"), Scene.Kind.TOPIC, context(player, npc, child, now));
    }

    // ---------------------------------------------------------------- the hour, the weather

    static void hours(Set<String> flags, long dayTime) {
        long time = Math.floorMod(dayTime, 24000L);
        boolean night = time >= 12542L && time <= 23000L;
        flags.add(night ? "night" : "day");
        if (time >= 11800L && time < 13600L) {
            flags.add("dusk");
        }
        if (time >= 22300L || time < 900L) {
            flags.add("dawn");
        }
        if (time >= 16500L && time < 21500L) {
            flags.add("deep");
        }
        if (time < 4500L) {
            flags.add("morning");
        }
    }

    static void weather(Set<String> flags, ServerLevel level) {
        if (level.m_46471_()) {
            flags.add("rain");
        }
        if (level.m_46470_()) {
            flags.add("thunder");
        }
    }

    // ---------------------------------------------------------------- left unanswered

    /** Default words for an approach left unanswered, by speaker. */
    static String ignored(String speaker, int seed) {
        String[] lines = switch (speaker) {
            case "eto" -> new String[] {"...Fine. I'll tell the books instead.", "Hm. Busy. I'll remember this. I remember everything.",
                "You'll come looking for me later. They always do."};
            case "touka" -> new String[] {"Tch. Forget it.", "...Whatever. It wasn't important.", "Fine. Ignore me. See if I care."};
            case "ken" -> new String[] {"...It's okay. Another time.", "Ah. You're busy. Sorry.", "I'll wait. I'm good at waiting."};
            default -> new String[] {"*looks down and wanders off*", "*waits a little, then goes back to playing*", "*sighs a very small sigh*"};
        };
        return lines[Math.floorMod(seed, lines.length)];
    }

    @SubscribeEvent
    public static void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || level.m_46467_() % 20L != 3L || PENDING.isEmpty()) {
            return;
        }
        long now = level.m_46467_();
        Iterator<Map.Entry<UUID, Pending>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Pending> entry = it.next();
            Pending pending = entry.getValue();
            if (now <= pending.until()) {
                continue;
            }
            ServerPlayer player = Pregnancies.playerOf(level, Pregnancies.player(entry.getKey()));
            if (player == null || player.m_284548_() != level) {
                if (now > pending.until() + 1200L) {
                    it.remove(); // gone, or in another world: forgotten after a while
                }
                continue;
            }
            it.remove();
            try {
                unanswered(player, level, pending, now);
            } catch (RuntimeException failure) {
                KaguneMod.LOGGER.error("HexKagune: an unanswered approach failed", failure);
            }
        }
    }

    /** Left waiting: what they say, and what it costs (a clingy partner a little regard; a child its smile, a while). */
    static void unanswered(ServerPlayer player, ServerLevel level, Pending pending, long now) {
        ControlNetwork.sendTo(player, new S2CNpcApproach(pending.npc(), "", "", "", 0));
        Scene scene = scene(pending.scene());
        net.minecraft.world.entity.Entity body = level.m_6815_(pending.npc());
        int seed = new java.util.Random().nextInt(1000);
        if (pending.child()) {
            SAD.put(pending.about(), now + 1800L);
            Children.Child child = Children.get(level, pending.about());
            if (child != null && body instanceof ChildNpc npc) {
                SceneContext sc = context(player, npc, child, now);
                String line = scene != null && !scene.ignored().isEmpty() ? SceneBook.fill(SceneBook.one(scene.ignored(), seed), sc) : ignored("child", seed);
                ChildBrain.say(player, child, line);
            }
            return;
        }
        if (!(body instanceof EtoNpc npc) || npc.mind == null) {
            return;
        }
        EtoTalk.Context c = EtoServer.context(player, npc);
        SceneContext sc = context(player, npc, c);
        String line = scene != null && !scene.ignored().isEmpty() ? SceneBook.fill(SceneBook.one(scene.ignored(), seed), sc)
            : ignored(npc.persona().key(), seed);
        EtoBrain.say(npc, player, line);
        if (c.dating() && Set.of("clingy", "lonely", "affectionate", "giddy").contains(sc.mood())) {
            QuestLog.Bond bond = QuestServer.bond(player, npc.persona().key());
            QuestServer.bond(player, npc.persona().key(), bond.regard(bond.regard() - 1));
            EtoBrain.gesture(npc.mind, EtoGesture.POUT, now);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().m_20148_();
        PENDING.remove(id);
        LAST.remove(id);
        NEXT.keySet().removeIf(key -> key.endsWith("|" + id));
    }
}
