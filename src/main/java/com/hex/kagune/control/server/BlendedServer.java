package com.hex.kagune.control.server;

import com.hex.kagune.control.family.Blended;
import com.hex.kagune.control.family.ChildCue;
import com.hex.kagune.control.family.ChildStage;
import com.hex.kagune.control.npc.ChildNpc;
import com.hex.kagune.control.npc.EtoGesture;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.NpcMemory;
import com.hex.kagune.control.npc.Persona;
import com.hex.kagune.control.npc.Romance;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 3.1.9: a blended family as it lives ({@link Blended}). Every few seconds, for each player: two of their partners near each other, both
 * free, talk (once a half day a pair): the first time as two people who love the same one, sniping while one of them is jealous, about
 * the children when both have some by them, or settled into it; and two of their children by different mothers or fathers near each other
 * talk about it (once a half day a pair). And a partner who sees the player being sweet on another says so to their face, and stays
 * jealous twice as long ({@link #witnessed}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class BlendedServer {
    private BlendedServer() {
    }

    /** How often it looks (ticks), how near two must be to talk (blocks), how far from the player it looks, how long before a pair talks again. */
    static final long EVERY = 100L;
    static final double NEAR = 9.0;
    static final double AROUND = 40.0;
    static final long AGAIN = 12000L;
    /** A jealousy seen happen lasts this many times as long. */
    static final long SEEN_LONGER = 2L;
    /** Memory: when a partner first met another of the player's partners (by the other's key). */
    static final String MET = "met_";

    private static final Map<String, Long> LAST = WorldTimes.map();

    @SubscribeEvent
    public static void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || level.m_46467_() % EVERY != 0L) {
            return;
        }
        long now = level.m_46467_();
        for (ServerPlayer player : level.m_6907_()) {
            try {
                partners(player, level, now);
                halfSiblings(player, level, now);
                sulks(player, level, now);
            } catch (RuntimeException failure) {
                com.hex.kagune.control.Safety.caught(failure, "a blended family's talk");
            }
        }
    }

    // ---------------------------------------------------------------- 3.3.6: a jealous partner, sulking

    /** In every this many of the partner checks (every 100 ticks) a jealous partner near enough and idle acts it out again (about 25 s). */
    static final int SULK_ODDS = 5;

    /**
     * 3.3.6 (the owner: a jealous animation): while a partner of {@code player}'s is still jealous ({@link NpcMemory#JEALOUS}), now and then,
     * near and seeing them, not busy, she turns her back on them again ({@link EtoGesture#JEALOUS}).
     */
    private static void sulks(ServerPlayer player, ServerLevel level, long now) {
        Map<String, Romance.Stage> partners = UniqueNpcs.partnersOf(level, player, "");
        if (partners.isEmpty()) {
            return;
        }
        NpcMemory memory = NpcMemoryServer.of(player);
        for (EtoNpc npc : level.m_6443_(EtoNpc.class, player.m_20191_().m_82400_(10.0), npc -> npc.m_6084_() && npc.mind != null)) {
            String key = npc.persona().key();
            if (!partners.containsKey(key) || memory.get(key, NpcMemory.JEALOUS, Long.MIN_VALUE) <= now || !SceneServer.sees(npc, player, 10.0)
                || !Conversation.free(npc, level, now) || EtoBrain.busy(npc) || npc.mind.kiss != null || npc.mind.holding != null || npc.mind.toward != null
                || npc.mind.gesture != null && npc.mind.gesture != EtoGesture.NONE && now - npc.mind.gestureAt < Math.max(npc.mind.gesture.ticks, 20)
                || npc.m_217043_().m_188503_(SULK_ODDS) != 0) {
                continue;
            }
            EtoBrain.gesture(npc.mind, EtoGesture.JEALOUS, now);
        }
    }

    // ---------------------------------------------------------------- two partners

    private static void partners(ServerPlayer player, ServerLevel level, long now) {
        Map<String, Romance.Stage> partners = UniqueNpcs.partnersOf(level, player, "");
        if (partners.size() < 2) {
            return;
        }
        List<EtoNpc> near = new ArrayList<>();
        for (EtoNpc npc : level.m_6443_(EtoNpc.class, player.m_20191_().m_82400_(AROUND), npc -> npc.m_6084_() && npc.mind != null)) {
            if (partners.containsKey(npc.persona().key())) {
                near.add(npc);
            }
        }
        for (int i = 0; i < near.size(); i++) {
            for (int j = i + 1; j < near.size(); j++) {
                EtoNpc a = near.get(i);
                EtoNpc b = near.get(j);
                String ka = a.persona().key();
                String kb = b.persona().key();
                if (ka.equals(kb) || a.m_20280_(b) > NEAR * NEAR) {
                    continue;
                }
                String key = player.m_20148_() + ":" + Blended.pair(ka, kb);
                Long last = LAST.get(key);
                if (last != null && now - last < AGAIN && now >= last || !Conversation.free(a, level, now) || !Conversation.free(b, level, now)) {
                    continue;
                }
                Blended.Mood mood = mood(player, level, ka, kb, now);
                Map<String, String> names = new HashMap<>();
                names.put("you", player.m_36316_().getName());
                names.put("akid", kidBy(player, level, ka));
                names.put("bkid", kidBy(player, level, kb));
                List<Blended.Said> talk = Blended.coWives(ka, kb, mood, a.m_217043_().m_188503_(1000), names);
                if (talk.isEmpty()) {
                    continue;
                }
                List<Conversation.Line> lines = new ArrayList<>();
                for (Blended.Said said : talk) {
                    EtoNpc speaker = said.who() == 0 ? a : b;
                    lines.add(new Conversation.Line(speaker.m_19879_(), said.text(), ChildCue.NONE, gesture(said.gesture())));
                }
                Conversation talkNow = Conversation.start(level, now, "partners", a, List.of(a, b), List.of(b), lines);
                if (talkNow != null) {
                    LAST.put(key, now);
                    if (mood == Blended.Mood.FIRST) {
                        NpcMemoryServer.set(player, ka, MET + kb, now);
                        NpcMemoryServer.set(player, kb, MET + ka, now);
                    }
                }
            }
        }
    }

    /** How {@code a} and {@code b} stand: first meeting, one jealous, both with children by the player, or settled. */
    static Blended.Mood mood(ServerPlayer player, ServerLevel level, String a, String b, long now) {
        if (NpcMemoryServer.get(player, a, MET + b, Long.MIN_VALUE) == Long.MIN_VALUE
            && NpcMemoryServer.get(player, b, MET + a, Long.MIN_VALUE) == Long.MIN_VALUE) {
            return Blended.Mood.FIRST;
        }
        if (NpcMemoryServer.get(player, a, NpcMemory.JEALOUS, Long.MIN_VALUE) > now || NpcMemoryServer.get(player, b, NpcMemory.JEALOUS, Long.MIN_VALUE) > now) {
            return Blended.Mood.RIVAL;
        }
        if (!kidBy(player, level, a).isEmpty() && !kidBy(player, level, b).isEmpty()) {
            return Blended.Mood.KIDS;
        }
        return Blended.Mood.EASY;
    }

    /** The name of a child {@code player} has with the partner {@code key}, or "". */
    static String kidBy(ServerPlayer player, ServerLevel level, String key) {
        String npc = "npc:" + key;
        for (Children.Child child : Children.of(level, Pregnancies.player(player.m_20148_()))) {
            if (npc.equals(child.mother) || npc.equals(child.father)) {
                return child.name;
            }
        }
        return "";
    }

    private static EtoGesture gesture(String word) {
        if (word == null || word.isEmpty()) {
            return EtoGesture.NONE;
        }
        try {
            return EtoGesture.valueOf(word.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return EtoGesture.NONE;
        }
    }

    // ---------------------------------------------------------------- children by different mothers or fathers

    private static void halfSiblings(ServerPlayer player, ServerLevel level, long now) {
        List<Children.Child> kids = Children.of(level, Pregnancies.player(player.m_20148_()));
        if (kids.size() < 2) {
            return;
        }
        Map<String, Children.Child> byId = new HashMap<>();
        for (Children.Child child : kids) {
            byId.put(child.id, child);
        }
        List<ChildNpc> near = new ArrayList<>();
        for (ChildNpc npc : level.m_6443_(ChildNpc.class, player.m_20191_().m_82400_(AROUND), npc -> npc.m_6084_() && npc.mind != null
            && byId.containsKey(npc.childId()))) {
            near.add(npc);
        }
        for (int i = 0; i < near.size(); i++) {
            for (int j = i + 1; j < near.size(); j++) {
                ChildNpc na = near.get(i);
                ChildNpc nb = near.get(j);
                Children.Child a = byId.get(na.childId());
                Children.Child b = byId.get(nb.childId());
                if (!half(a, b) || na.m_20280_(nb) > NEAR * NEAR || !talks(a, now) || !talks(b, now)) {
                    continue;
                }
                String key = player.m_20148_() + ":" + (a.id.compareTo(b.id) <= 0 ? a.id + "+" + b.id : b.id + "+" + a.id);
                Long last = LAST.get(key);
                if (last != null && now - last < AGAIN && now >= last || !Conversation.free(na, level, now) || !Conversation.free(nb, level, now)) {
                    continue;
                }
                boolean aBig = big(a, now);
                boolean bBig = big(b, now);
                // The older (or, both alike, the first) speaks first.
                if (!aBig && bBig) {
                    ChildNpc swapN = na;
                    na = nb;
                    nb = swapN;
                    Children.Child swap = a;
                    a = b;
                    b = swap;
                    boolean big = aBig;
                    aBig = bBig;
                    bBig = big;
                }
                Blended.Ages ages = aBig && bBig ? Blended.Ages.BIG : !aBig && !bBig ? Blended.Ages.LITTLE : Blended.Ages.MIXED;
                boolean mother = ChildBrain.isMother(a, player);
                boolean teen = aBig && bBig;
                Map<String, String> names = new HashMap<>();
                names.put("a", a.name);
                names.put("b", b.name);
                names.put("amom", otherParent(a, player));
                names.put("bmom", otherParent(b, player));
                names.put("par", mother ? teen ? "Mom" : "Mama" : teen ? "Dad" : "Papa");
                List<Conversation.Line> lines = new ArrayList<>();
                for (Blended.Said said : Blended.halfSiblings(ages, na.m_217043_().m_188503_(1000), names)) {
                    ChildNpc speaker = said.who() == 0 ? na : nb;
                    lines.add(new Conversation.Line(speaker.m_19879_(), said.text(), ChildCue.NONE, EtoGesture.NONE));
                }
                if (Conversation.start(level, now, "half siblings", na, List.of(na, nb), List.of(nb), lines) != null) {
                    LAST.put(key, now);
                }
            }
        }
    }

    /** True for two children who share one parent and not the other (the same player's, by different mothers or fathers). */
    static boolean half(Children.Child a, Children.Child b) {
        if (a == null || b == null || a.id.equals(b.id) || !SiblingLife.siblings(a, b)) {
            return false;
        }
        Set<String> pa = Set.of(nz(a.mother), nz(a.father));
        Set<String> pb = Set.of(nz(b.mother), nz(b.father));
        return !pa.equals(pb);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static boolean talks(Children.Child child, long now) {
        ChildStage stage = child.stage(now);
        return stage != ChildStage.BABY;
    }

    private static boolean big(Children.Child child, long now) {
        ChildStage stage = child.stage(now);
        return stage == ChildStage.TEEN || stage == ChildStage.ADULT;
    }

    /** {@code child}'s parent who isn't {@code player}: their name ("their other parent" if unknown). */
    static String otherParent(Children.Child child, ServerPlayer player) {
        String name = ChildBrain.isMother(child, player) ? child.fatherName : child.motherName;
        return name == null || name.isEmpty() ? "their other parent" : name;
    }

    // ---------------------------------------------------------------- seen

    /**
     * {@code player} was sweet on {@code key} (a kiss, a hug, a date, a ring, a night: {@code RomanceServer.romanced}): each other partner of
     * theirs who is near and sees it says so to their face (with a gesture), and is jealous {@link #SEEN_LONGER} times as long.
     */
    static void witnessed(ServerPlayer player, String key, long now) {
        witnessed(player, key, null, now);
    }

    /**
     * The same, {@code name} who it was ({@code null}: the companion {@code key}'s name; 3.3.6: a villager's own, never "Eto"). With a
     * {@code name} (3.3.6, {@code RomanceServer.tempted}) it was asking them out or confessing to them, and the words are for that.
     */
    static void witnessed(ServerPlayer player, String key, String name, long now) {
        ServerLevel level = player.m_284548_();
        Map<String, Romance.Stage> others = UniqueNpcs.partnersOf(level, player, key);
        if (others.isEmpty()) {
            return;
        }
        Persona sweet = Persona.of(key);
        for (EtoNpc npc : level.m_6443_(EtoNpc.class, player.m_20191_().m_82400_(24.0), npc -> npc.m_6084_() && npc.mind != null)) {
            String own = npc.persona().key();
            if (!others.containsKey(own) || own.equals(key) || !SceneServer.sees(npc, player, 24.0)) {
                continue;
            }
            Map<String, String> names = new HashMap<>();
            names.put("you", player.m_36316_().getName());
            names.put("other", name != null ? name : Persona.known(key) ? sweet.name() : "them");
            int seed = npc.m_217043_().m_188503_(1000);
            Blended.Said said = name != null ? Blended.tempted(own, seed, names) : Blended.witnessed(own, seed, names);
            Conversation.say(level, npc, said.text(), 0);
            EtoBrain.gesture(npc.mind, EtoGesture.JEALOUS, now); // 3.3.6: the whole act, not one gesture
            NpcMemoryServer.set(player, own, NpcMemory.JEALOUS, now + RomanceServer.JEALOUS_FOR * SEEN_LONGER);
        }
    }
}
