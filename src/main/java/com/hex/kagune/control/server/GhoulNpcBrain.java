package com.hex.kagune.control.server;

import com.hex.kagune.control.ControlItems;
import com.hex.kagune.control.graft.GraftMove;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CGhoulNpc;
import com.hex.kagune.control.npc.GhoulNpc;
import com.hex.kagune.control.npc.NpcBonds;
import com.hex.kagune.control.npc.NpcIdentity;
import com.hex.kagune.control.npc.NpcRules;
import com.hex.kagune.control.npc.NpcTemper;
import com.hex.kagune.control.quest.QuestBook;
import com.hex.kagune.control.quest.QuestLog;
import com.hex.kagune.control.quest.Signal;
import com.hex.kagune.control.quest.Tags;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 7.0.2: what a ghoul NPC does, decided on the server every tick ({@link GhoulNpc}'s own AI step): who it goes for (its temper's
 * {@link NpcTemper#toward attitude} to each player, the NPCs its temper hunts, anyone who hurt it while its grudge lasts), when it
 * runs (a coward losing, or a ghoul too close), and the rest of the time it wanders and watches. Its kagune comes out when it fights
 * (the kakugan with it) and goes back a while after; the fighting itself is {@link GhoulNpcCombat}'s. Ghouls heal fast: unhurt for
 * a moment, it knits back up. At dawn, unwatched, it slips away. Every client tracking it is told what it needs to draw it.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class GhoulNpcBrain {
    /** One NPC's state on the server. */
    public static final class Mind {
        /** What it goes for now (an entity id, 0 none) and the game tick it last looked for something. */
        int prey;
        long preySought = Long.MIN_VALUE;
        /** Who hurt it, and until when it holds that against them. */
        final Map<UUID, Long> grudges = new HashMap<>();
        long hurtAt = Long.MIN_VALUE;
        /** 4.1.52: stepping out of a feared member's way until then. */
        long yieldUntil = Long.MIN_VALUE;
        /** The kagune: out or not, since when, and when it last fought. */
        boolean out;
        long outAt = Long.MIN_VALUE;
        long foughtAt = Long.MIN_VALUE;
        /** Rinkaku: the lash it is making, when it began, where it was aimed; the last one and when it ended; lashes so far. */
        GraftMove move = GraftMove.NONE;
        long moveAt = Long.MIN_VALUE;
        Vec3 point = Vec3.f_82478_;
        /** The way the lash faces (a Minecraft yaw, degrees): at its prey as it began. */
        double yaw;
        GraftMove last;
        long lastEnds = Long.MIN_VALUE;
        int strung;
        final Map<Integer, Set<Integer>> hits = new HashMap<>();
        /** Not before this tick does it start another string or volley. */
        long readyAt = Long.MIN_VALUE;
        /** Ukaku: the volley (when it began aiming, 0 shots so far), the next shot, the wing it comes from, the last shot. */
        long volleyAt = Long.MIN_VALUE;
        int shots;
        long nextShot = Long.MIN_VALUE;
        int side = 1;
        long shotAt = Long.MIN_VALUE;
        int shotSide = 1;
        final List<GhoulNpcCombat.Bolt> bolts = new ArrayList<>();
        /** Walking: when it next picks somewhere to wander to; where it circles its prey from (and until when). */
        long wanderAt = Long.MIN_VALUE;
        double circle = 1.0;
        long circleAt = Long.MIN_VALUE;
        boolean fleeing;
        /**
         * 1.3.36: the arm blade (BladeArm): the cut it is making (-1 none), when it began, whether it has yet to land, at whom; the
         * last one and when it ended; a token that tells a met cut (KaguneClash) from a later one.
         */
        int blade = -1;
        long bladeAt = Long.MIN_VALUE;
        boolean bladeStriking;
        int bladeTarget;
        int bladeLast = -1;
        long bladeEnds = Long.MIN_VALUE;
        int bladeToken;
        /** Something every client should be told. */
        boolean dirty = true;
        /** 7.0.2: the player it is talking to (an entity id, 0 none) and until when it waits on them. */
        int talking;
        long talkUntil = Long.MIN_VALUE;
        /** 7.0.2: the player it walks with (null: none), whether it follows them or waits, where it waits. */
        UUID owner;
        boolean follow = true;
        Vec3 waitAt;
        /** 7.0.2: sent off (it walks away and is gone), or scared off (it runs a while). */
        long leavingAt = Long.MIN_VALUE;
        long scaredUntil = Long.MIN_VALUE;
        /** 2.1.9: trembling before Peace or War (who, until when): it fights, but its nerve keeps giving. */
        int afraidOf;
        long afraidUntil = Long.MIN_VALUE;
        /** 1.3.68: its kakugan on with its kagune in (a wild ghoul prowling at night: NpcRules.prowls). */
        boolean eyes;
        /** 1.3.65: who scared it off (an entity id, 0: whoever is nearest): it runs from them (Sightings: War keeps it running). */
        int scaredOf;
        /** 1.0.41: its flip (an PaleRules.Flip, -1 none), when it took off, its push (renewed the tick after), when it may flip again. */
        int flip = -1;
        long flipAt = Long.MIN_VALUE;
        Vec3 flipPush;
        long flipReadyAt = Long.MIN_VALUE;
        /** 1.0.41: when its heavy is ready again, and the blow coming at it it last answered (when that set out). */
        long heavyReadyAt = Long.MIN_VALUE;
        long answered = Long.MIN_VALUE;
        /** 4.0.0: an S's or SS's frenzy (until when), and whether this fight's is spent. */
        long frenzyUntil = Long.MIN_VALUE;
        boolean frenzied;
    }

    /** 4.0.0: set (by its hideout's template) on one of Aogiri Tree's members. */
    static final String AOGIRI = "HexAogiri";
    /** 4.0.0: how long an Aogiri member holds it against whoever attacked it or its own (ticks): five minutes. */
    static final int AOGIRI_GRUDGE = 6000;
    /** 4.0.0: how far a hurt member's call reaches its comrades (blocks). */
    static final double AOGIRI_CALL = 24.0;

    /**
     * 4.0.0: {@code rolled} made one of Aogiri Tree's: the Pack's temper (violent, loyal to its own), its faction's mask (the Laughing Mask or
     * the Fanged Mask), a trained fighter's rank: C now and then, mostly B and A, an S one in ten ({@code a}, {@code b} rolls in 0..1).
     */
    static NpcIdentity aogiri(NpcIdentity rolled, double a, double b) {
        int rank = a < 0.2 ? NpcRules.C : a < 0.6 ? NpcRules.B : a < 0.9 ? NpcRules.A : NpcRules.S;
        int mask = 1 + Math.min(com.hex.kagune.control.outfit.Outfits.AOGIRI_MASKS.size() - 1,
            (int)(b * com.hex.kagune.control.outfit.Outfits.AOGIRI_MASKS.size()));
        return new NpcIdentity(rolled.kind(), NpcTemper.PACK, rank, rolled.skin(), rolled.name(), rolled.seed(), mask, true);
    }

    /**
     * 4.1.48 (the owner chose "One per world + clowns"): set (by the circus tent's template, tools/companion_structures.py) on the Clowns who
     * live there: which mask each wears (1-based, each a different one); where each first woke is its home ({@link #CIRCUS_HOME_X}).
     */
    static final String CIRCUS = "HexCircus";
    static final String CIRCUS_MASK = "HexCircusMask";
    static final String CIRCUS_HOME_X = "HexCircusHomeX";
    static final String CIRCUS_HOME_Y = "HexCircusHomeY";
    static final String CIRCUS_HOME_Z = "HexCircusHomeZ";
    /** 4.1.48: how far a circus Clown strolls from its spot; further than {@link #CIRCUS_LEASH} (a chase), it hurries back. */
    static final double CIRCUS_STROLL = 7.0;
    static final double CIRCUS_LEASH = 20.0;

    /**
     * 4.1.48: {@code rolled} made one of the circus's Clowns, in its mask {@code mask} (as its template set it; one picked with {@code roll} in
     * 0..1 if that is none of theirs): a Clown as a roaming one is (it starts nothing; whoever hurts one has them all on him).
     */
    static NpcIdentity circus(NpcIdentity rolled, int mask, double roll) {
        com.hex.kagune.control.npc.Gang clowns = com.hex.kagune.control.npc.Gang.CLOWNS;
        int n = clowns.masks().size();
        return rolled.ganged(clowns, mask >= 1 && mask <= n ? mask : 1 + Math.min(n - 1, (int)(Math.max(0.0, roll) * n)));
    }

    /** 4.1.45: a gang's own (which one in its saved data), how far one hurt calls the rest, how long they hold it against whoever did it. */
    static final String GANG_ID = "HexGang";
    static final double GANG_CALL = 24.0;
    static final int GANG_GRUDGE = 2400;
    /** 4.1.45: how far a gang's members look for each other, how close they keep to the one leading. */
    static final double GANG_KEEP = 48.0;
    static final double GANG_CLOSE = 4.5;

    /** 4.1.46: every one of the CCG's ghoul division within {@link #GANG_CALL} of {@code near} goes for {@code attacker} (an investigator was struck). */
    static void called(LivingEntity attacker, LivingEntity near, long until) {
        if (AgentBrain.ccg(attacker) || attacker instanceof Player player && player.m_7500_()) {
            return;
        }
        for (GhoulNpc one : near.m_9236_().m_6443_(GhoulNpc.class, near.m_20191_().m_82400_(GANG_CALL),
            o -> o.m_6084_() && o.mind != null && o.identity() != null && o.identity().ccg())) {
            Long was = one.mind.grudges.get(attacker.m_20148_());
            one.mind.grudges.put(attacker.m_20148_(), was == null ? until : Math.max(was, until));
            one.mind.preySought = Long.MIN_VALUE;
        }
    }

    /**
     * 4.1.46 (the owner chose "Like CCG agents": "hunt ghoul NPCs, attack ghoul players who show their kagune or are marked; fight with their
     * kagune; never attacked by CCG and never attack humans"): what one of the CCG's ghoul division goes for unprovoked, as an investigator
     * does ({@link AgentBrain#wanted}): a wild ghoul in sight once it shows what it is (its eyes on, its kagune out), a player who shows
     * what they are (marking them for the three of them, told so as the CCG tells them) or whom an investigator near holds something
     * against, a mob after it or one of its own, a monster too close. Never the CCG's, a human, Eto, a child or a thrall.
     */
    static boolean division(GhoulNpc npc, Mind mind, LivingEntity body, long now) {
        if (AgentBrain.ccg(body) || body instanceof com.hex.kagune.control.npc.HumanNpc || body instanceof com.hex.kagune.control.npc.EtoNpc
            || body instanceof com.hex.kagune.control.npc.ChildNpc || body instanceof com.hex.kagune.control.npc.FleshThrall) {
            return false;
        }
        double distance = Math.sqrt(body.m_20280_(npc));
        boolean current = body.m_19879_() == mind.prey;
        if (body instanceof ServerPlayer player) {
            if (AgentBrain.marked(npc, player, now)) {
                return true;
            }
            boolean seen = npc.m_142582_(player);
            if (!com.hex.kagune.control.npc.AgentRules.revealed(KaguneProfile.isUnlocked(player), AgentBrain.shows(player), distance, seen)) {
                return false;
            }
            Long was = mind.grudges.get(player.m_20148_());
            if (was == null || was <= now) {
                player.m_5661_(Component.m_237113_("\u00a7cThe CCG has seen what you are."), true);
            }
            long until = now + com.hex.kagune.control.npc.AgentRules.MARKED;
            mind.grudges.put(player.m_20148_(), until);
            for (GhoulNpc mate : npc.m_9236_().m_6443_(GhoulNpc.class, npc.m_20191_().m_82400_(GANG_KEEP),
                other -> other != npc && other.m_6084_() && other.mind != null && mates(npc, other))) {
                Long theirs = mate.mind.grudges.get(player.m_20148_());
                mate.mind.grudges.put(player.m_20148_(), theirs == null ? until : Math.max(theirs, until));
            }
            return true;
        }
        if (body instanceof Player) {
            return false;
        }
        if (body instanceof GhoulNpc ghoul) {
            boolean shows = ghoul.mind != null && (ghoul.mind.out || ghoul.mind.eyes);
            return current || shows && distance <= com.hex.kagune.control.npc.AgentRules.GHOUL_RANGE;
        }
        if (body instanceof Mob mob && (mob.m_5448_() == npc || mob.m_5448_() instanceof GhoulNpc target && mates(npc, target))) {
            return true;
        }
        return body instanceof Enemy && (current || com.hex.kagune.control.npc.AgentRules.tooClose(distance));
    }

    /** 4.1.45: true if {@code a} and {@code b} walk in the same gang (its kind and its number, kept in each one's saved data). */
    static boolean mates(GhoulNpc a, GhoulNpc b) {
        NpcIdentity x = a.identity();
        NpcIdentity y = b.identity();
        return x != null && y != null && x.gang() != 0 && x.gang() == y.gang()
            && a.getPersistentData().m_128451_(GANG_ID) == b.getPersistentData().m_128451_(GANG_ID);
    }

    /**
     * 4.1.45: walking with its gang: the one of them with the least id near leads (any of them, if the first is gone), wandering as any
     * ghoul does; each other keeps up with it, a few blocks off it at its own side. True while it is keeping up (nothing else to do).
     */
    static boolean roam(GhoulNpc npc, Mind mind, ServerLevel level, long now) {
        if (now % 10L != Math.floorMod(npc.m_19879_(), 10)) {
            return mind.wanderAt == Long.MAX_VALUE; // a follower does nothing else between looks (no stopping for passers-by)
        }
        GhoulNpc leader = npc;
        for (GhoulNpc other : level.m_6443_(GhoulNpc.class, npc.m_20191_().m_82400_(GANG_KEEP), o -> o != npc && o.m_6084_() && mates(npc, o))) {
            if (other.m_20148_().compareTo(leader.m_20148_()) < 0) {
                leader = other;
            }
        }
        if (leader == npc) {
            if (mind.wanderAt == Long.MAX_VALUE) {
                mind.wanderAt = now; // it led no one a moment ago: its own way now
            }
            return false;
        }
        mind.wanderAt = Long.MAX_VALUE; // never its own way while it follows
        double side = (Math.floorMod(npc.m_20148_().hashCode(), 360)) * Math.PI / 180.0;
        double x = leader.m_20185_() + Math.cos(side) * 2.5;
        double z = leader.m_20189_() + Math.sin(side) * 2.5;
        double distance = Math.sqrt(npc.m_20280_(leader));
        if (distance <= GANG_CLOSE && !leader.m_21573_().m_26571_()) {
            npc.m_21573_().m_26519_(x, leader.m_20186_(), z, 0.75); // walking on together
        } else if (distance > GANG_CLOSE) {
            npc.m_21573_().m_26519_(x, leader.m_20186_(), z, distance > 12.0 ? 1.0 : 0.85);
        } else {
            npc.m_21573_().m_26573_();
        }
        return true;
    }

    /**
     * 4.1.48: a circus Clown at home (true; false for anyone else): walked back to its spot once further than {@link #CIRCUS_STROLL} (at a
     * run past {@link #CIRCUS_LEASH}), else now and then a few steps to a spot near it that it can walk to in a straight line on the tent's
     * floor ({@link #clearWay}: never at the tent's wall, so never a path round the outside).
     */
    static boolean atCircus(GhoulNpc npc, Mind mind, ServerLevel level, long now) {
        CompoundTag saved = npc.getPersistentData();
        if (!saved.m_128441_(CIRCUS_HOME_X)) {
            return false;
        }
        double hx = saved.m_128451_(CIRCUS_HOME_X) + 0.5;
        double hy = saved.m_128451_(CIRCUS_HOME_Y);
        double hz = saved.m_128451_(CIRCUS_HOME_Z) + 0.5;
        double dx = npc.m_20185_() - hx;
        double dz = npc.m_20189_() - hz;
        double away = Math.sqrt(dx * dx + dz * dz);
        if (away > CIRCUS_STROLL + 2.0) {
            if (now % 20L == 0L || npc.m_21573_().m_26571_()) {
                npc.m_21573_().m_26519_(hx, hy, hz, away > CIRCUS_LEASH ? 1.0 : 0.7);
            }
            return true;
        }
        if (now < mind.wanderAt || !npc.m_21573_().m_26571_()) {
            return true;
        }
        mind.wanderAt = now + 120L + (long)(npc.m_217043_().m_188500_() * 200.0);
        for (int attempt = 0; attempt < 4; attempt++) {
            double angle = npc.m_217043_().m_188500_() * Math.PI * 2.0;
            double distance = 2.0 + npc.m_217043_().m_188500_() * (CIRCUS_STROLL - 2.0);
            double x = hx + Math.cos(angle) * distance;
            double z = hz + Math.sin(angle) * distance;
            if (clearWay(level, hx, hy, hz, x, z)) {
                npc.m_21573_().m_26519_(x, hy, z, 0.6);
                break;
            }
        }
        return true;
    }

    /** 4.1.48: true if the straight way from {@code x0, z0} to {@code x1, z1} at {@code y} is open (feet and head) over solid floor. */
    static boolean clearWay(ServerLevel level, double x0, double y, double z0, double x1, double z1) {
        int steps = Math.max(1, (int)Math.ceil(Math.sqrt((x1 - x0) * (x1 - x0) + (z1 - z0) * (z1 - z0)) * 2.0));
        for (int i = 1; i <= steps; i++) {
            double t = i / (double)steps;
            double x = x0 + (x1 - x0) * t;
            double z = z0 + (z1 - z0) * t;
            if (!level.m_8055_(BlockPos.m_274561_(x, y + 0.5, z)).m_60795_() || !level.m_8055_(BlockPos.m_274561_(x, y + 1.5, z)).m_60795_()
                || level.m_8055_(BlockPos.m_274561_(x, y - 0.5, z)).m_60795_()) {
                return false;
            }
        }
        return true;
    }

    /** Where an ally's player and whether it waits are kept in its saved data. */
    static final String ALLY = "HexGhoulAlly";
    static final String WAITS = "HexGhoulWaits";

    private GhoulNpcBrain() {
    }

    // ---------------------------------------------------------------- the tick

    public static void tick(GhoulNpc npc) {
        if (!(npc.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        NpcIdentity who = npc.identity();
        if (who == null) {
            // Summoned or hatched, not spawned at night: it becomes someone now.
            who = NpcIdentity.roll(0, () -> npc.m_217043_().m_188500_());
            CompoundTag saved = npc.getPersistentData();
            if (saved.m_128471_(AOGIRI)) {
                // 4.0.0: one of Aogiri Tree's, placed with its hideout: the faction's own temper, mask and rank, its Crimson Outfit.
                who = aogiri(who, npc.m_217043_().m_188500_(), npc.m_217043_().m_188500_());
            } else if (saved.m_128471_(CIRCUS)) {
                // 4.1.48: one of the Clowns, placed with the circus tent: its own mask (its template's), and where it woke its home.
                who = circus(who, saved.m_128451_(CIRCUS_MASK), npc.m_217043_().m_188500_());
                if (!saved.m_128441_(CIRCUS_HOME_X)) {
                    saved.m_128405_(CIRCUS_HOME_X, (int)Math.floor(npc.m_20185_()));
                    saved.m_128405_(CIRCUS_HOME_Y, (int)Math.floor(npc.m_20186_() + 0.01));
                    saved.m_128405_(CIRCUS_HOME_Z, (int)Math.floor(npc.m_20189_()));
                }
            }
            npc.identify(who);
            dress(npc);
        }
        Mind mind = npc.mind;
        if (mind == null) {
            mind = new Mind();
            CompoundTag saved = npc.getPersistentData();
            if (saved.m_128403_(ALLY)) {
                mind.owner = saved.m_128342_(ALLY);
                mind.follow = !saved.m_128471_(WAITS);
                mind.waitAt = npc.m_20182_();
            }
            npc.mind = mind;
        }
        long now = level.m_46467_();
        if (mind.leavingAt != Long.MIN_VALUE) {
            leave(npc, mind, level, now);
            return;
        }
        regen(npc, mind, now);
        if (!who.ccg() && dawn(npc, level, now)) {
            return; // 4.1.46: the CCG's ghoul division keeps the day as the investigators do (gone when far from everyone)
        }
        if (BikakuDevour.held(npc)) {
            // 3.6.3: being devoured: its kagune in at once, it does nothing at all.
            calm(npc, mind, now, true);
            mind.fleeing = false;
            sync(npc, mind, who);
            return;
        }
        GhoulNpcCombat.fly(npc, who, mind, level, now);
        if (PaleStun.stunned(npc)) {
            sync(npc, mind, who);
            return; // reeling: it does nothing until it shakes it off
        }
        // 2.1.9: trembling before Peace or War, its nerve giving now and then: it backs off a moment, then comes again.
        npc.afraid(now < mind.afraidUntil);
        if (now < mind.afraidUntil && now >= mind.scaredUntil && com.hex.kagune.control.npc.FinalSight.breaks(npc.m_19879_(), now)) {
            mind.scaredUntil = now + com.hex.kagune.control.npc.FinalSight.BACKS_OFF;
            mind.scaredOf = mind.afraidOf;
        }
        LivingEntity prey = prey(npc, who, mind, level, now);
        // 1.3.68: a wild ghoul prowls by night (its kakugan on) and hides what it is by day: anything it would fight, it runs from instead.
        // 4.0.0: Aogiri's own neither prowl nor hide: they hold their hideout. 4.1.45: nor a gang: they walk the night together.
        boolean wild = mind.owner == null && !who.aogiri() && who.gang() == 0;
        boolean eyes = NpcRules.prowls(wild, level.m_46468_());
        if (eyes != mind.eyes) {
            mind.eyes = eyes;
            mind.dirty = true;
        }
        if (prey != null && NpcRules.hides(wild, level.m_46468_())) {
            flee(npc, mind, prey, level, now);
        } else if (now < mind.scaredUntil || who.temper().coward() && afraid(npc, level, prey)) {
            flee(npc, mind, prey, level, now);
        } else if (prey == null && talking(npc, mind, level, now)) {
            mind.fleeing = false;
            calm(npc, mind, now, false); // it stands and talks
        } else if (prey != null) {
            mind.fleeing = false;
            fight(npc, who, mind, prey, level, now);
        } else {
            mind.fleeing = false;
            // 1.3.29: walking with a player and nothing to fight: now and then it comes up to them with something (SceneServer).
            ServerPlayer owner = mind.owner != null && now % 20L == 15L ? owner(npc, mind) : null;
            if (owner != null && owner.m_20280_(npc) <= 12.0 * 12.0 && SceneServer.allyTick(npc, owner, now)) {
                sync(npc, mind, who);
                return;
            }
            idle(npc, who, mind, level, now);
        }
        sync(npc, mind, who);
    }

    /**
     * Puts its rank on it (health, armor, speed), heals it whole and names it (4.0.0: with its rank). 4.0.0: its mask on its head (a wild
     * ghoul's generic one, an Aogiri member's faction one with the Crimson Outfit), each piece's drop its own small chance; their
     * protection is taken off its own, so its rank alone says how tough it is.
     */
    public static void dress(GhoulNpc npc) {
        NpcIdentity who = npc.identity();
        if (who == null) {
            return;
        }
        if (who.aogiri()) {
            OutfitServer.dress(npc, com.hex.kagune.control.outfit.Outfits.CRIMSON_SET);
        } else if (who.ccg()) {
            OutfitServer.dress(npc, com.hex.kagune.control.outfit.Outfits.FORMAL_SET); // 4.1.46: the CCG's armour (the owner: "CCG armor")
        } else if (who.gang() == 0 && who.maskItem() != null) {
            OutfitServer.mask(npc, who.maskItem());
        } // 4.1.45: a gang's own mask is drawn on its face (its identity tells every client which), nothing else on it
        for (net.minecraft.world.entity.EquipmentSlot slot : new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.HEAD,
            net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET}) {
            npc.m_21409_(slot, com.hex.kagune.control.outfit.Outfits.DROP_CHANCE);
        }
        npc.m_21051_(Attributes.f_22276_).m_22100_(NpcRules.health(who.tier()));
        npc.m_21051_(Attributes.f_22284_).m_22100_(OutfitServer.armour(npc, NpcRules.armor(who.tier())));
        npc.m_21051_(Attributes.f_22279_).m_22100_(NpcRules.speed(who.tier()));
        npc.m_21153_(npc.m_21233_());
        npc.m_6593_(Component.m_237113_(who.tagName()));
    }

    /** 4.0.0: how hard each unit of its blows lands: its rank's damage, a third more in an S's or SS's frenzy. */
    static float damage(GhoulNpc npc, NpcIdentity who) {
        float base = NpcRules.damage(who.tier());
        Mind mind = npc.mind;
        return mind != null && npc.m_9236_().m_46467_() < mind.frenzyUntil ? base * 1.35F : base;
    }

    /** What it leaves when it dies: Ghoul Meat (4.1.46: one of the CCG's ghoul division is human: a human's meat). */
    public static void drop(GhoulNpc npc) {
        NpcIdentity who = npc.identity();
        if (who != null && who.ccg()) {
            HumanBrain.drop(npc, 0);
        } else {
            int pieces = NpcRules.meat(who == null ? 0 : who.tier(), npc.m_217043_().m_188500_());
            npc.m_19983_(new ItemStack(ControlItems.GHOUL_MEAT.get(), pieces));
        }
        if (npc.mind != null) {
            GhoulNpcCombat.fizzleAll(npc, npc.mind);
            ServerPlayer owner = owner(npc, npc.mind);
            if (owner != null) {
                String key = npc.m_20148_().toString();
                QuestServer.bond(owner, key, QuestServer.bond(owner, key).with(NpcBonds.ALLY, false));
                owner.m_5661_(Component.m_237113_((who == null ? "Your ally" : who.displayName()) + " is dead."), false);
            }
        }
    }

    // ---------------------------------------------------------------- what it goes for

    /** What it goes for: the one it is after while that still holds, else the nearest it sees that it wants (looked for every half second). */
    private static LivingEntity prey(GhoulNpc npc, NpcIdentity who, Mind mind, ServerLevel level, long now) {
        LivingEntity current = mind.prey != 0 && level.m_6815_(mind.prey) instanceof LivingEntity living ? living : null;
        if (current != null && (!current.m_6084_() || current.m_20280_(npc) > NpcRules.LEASH * NpcRules.LEASH || !wanted(npc, who, mind, current, now))) {
            current = null;
        }
        // 1.0.15: "never looked" is Long.MIN_VALUE, and now minus that overflows negative: it never looked round at all (wild ghouls
        // never fought back). Never-looked looks at once.
        if (current == null && (mind.preySought == Long.MIN_VALUE || now - mind.preySought >= 10L)) {
            mind.preySought = now;
            double best = Double.MAX_VALUE;
            ServerPlayer owner = owner(npc, mind);
            if (owner != null && owner.m_21188_() != null && owner.m_21188_() != npc && owner.m_21188_().m_6084_()
                && owner.m_21188_().m_20280_(npc) <= NpcRules.LEASH * NpcRules.LEASH && !(owner.m_21188_() instanceof GhoulNpc kin && owner.equals(owner(kin, kin.mind)))) {
                current = owner.m_21188_(); // whatever just hurt its player
                best = 0.0;
            }
            double sight = NpcRules.sight(who.tier()); // 4.0.0: the stronger, the keener
            for (LivingEntity body : level.m_6443_(LivingEntity.class, npc.m_20191_().m_82400_(sight),
                living -> living != npc && living.m_6084_())) {
                double distance = body.m_20280_(npc);
                if (distance < best && distance <= sight * sight && wanted(npc, who, mind, body, now) && npc.m_142582_(body)) {
                    best = distance;
                    current = body;
                }
            }
        }
        int id = current == null ? 0 : current.m_19879_();
        if (id != mind.prey) {
            mind.prey = id;
            npc.m_6710_(current);
            mind.dirty = true;
        }
        return current;
    }

    /** True if it goes for {@code body}: a grudge, its temper's attitude to a player, or an NPC its temper hunts. */
    static boolean wanted(GhoulNpc npc, NpcIdentity who, Mind mind, LivingEntity body, long now) {
        if (body == npc || !body.m_6084_() || ReviveForget.presumedDead(body, now)) {
            return false; // 2.4.2: never one thought dead
        }
        if (body instanceof Player player && player.m_5833_()) {
            return false;
        }
        // 4.1.50 (the owner: "the gang will never attack the player again, even when hit, they will however help the player and defend him
        // against hostiles. IF the player kills over 10 members of his own gang, he will be banned from joining, tag removed and will be
        // attacked on sight"): its own never (grudge or not), the banned on sight, and what goes for its own.
        if (body instanceof ServerPlayer player && !player.m_7500_()) {
            int stance = GangServer.stance(player, who);
            if (stance == GangServer.OWN_GANG) {
                // 4.1.52 (the owner: "They attack after a warning"): its own, unless the bar is empty and they stayed in the hideout.
                return GangServer.turned(player);
            }
            if (stance == GangServer.OUTCAST) {
                return true;
            }
        }
        if (GangServer.threatens(npc, who, body)) {
            return true;
        }
        Long until = mind.grudges.get(body.m_20148_());
        if (until != null && until > now) {
            // 1.3.39: even a player in creative who hit it: it fights back (its kagune out, as the owner tests them), though its blows
            // can't hurt them. It never goes for one unprovoked.
            return true;
        }
        if (body instanceof Player player && player.m_7500_()) {
            return false;
        }
        if (who.ccg()) {
            return division(npc, mind, body, now); // 4.1.46: as an investigator
        }
        if (who.gang() != 0 && body instanceof GhoulNpc other && other.identity() != null && who.gangOf().rival(other.identity().gangOf())) {
            return true; // 4.1.48 (the owner chose "Yes, rivals"): a Clown and a Gas Mask fight on sight (it must see them: prey)
        }
        if (who.gang() != 0) {
            // 4.1.45 (the owner: "protect eachother) (Non-hostile unless provoked)"): a gang starts nothing: only what hurt one of them
            // (a grudge, above, the whole gang's: hurt) or is going for one of them.
            return body instanceof Mob mob && mob.m_5448_() instanceof GhoulNpc target && mates(npc, target)
                && !(body instanceof GhoulNpc kin && mates(npc, kin));
        }
        if (who.aogiri()) {
            // 4.0.0 (the owner: "Its members must ONLY attack CCG investigators and entities that are actively hostile toward them"): no
            // neutral human, no peaceful NPC, no player who has not provoked them (a grudge, above, is provocation). 4.0.2: nor one kept
            // out of the hideout for a wrong answer at the gate (the owner: "If he doesn't like it, they'll attack you. And they won't let
            // you in"), for as long as they are kept out.
            return aogiriFoe(body) || body instanceof Player player && AogiriGuard.unwelcome(npc, player, now);
        }
        ServerPlayer owner = owner(npc, mind);
        if (owner != null) {
            // An ally follows its player's lead: what hurts them, what hunts them, the monsters round them; never them or theirs.
            if (body == owner || body instanceof GhoulNpc kin && owner.equals(owner(kin, kin.mind))) {
                return false;
            }
            // (Not a creeper: it would blow up beside them.)
            return body == owner.m_21188_() || body instanceof Mob mob && mob.m_5448_() == owner && !(body instanceof Creeper)
                || body instanceof Enemy && !(body instanceof Creeper) && body.m_20280_(owner) <= 100.0;
        }
        // 1.3.68: by day a wild ghoul starts nothing (only what hurt it, above, and that it runs from); by night it hunts humans.
        long day = npc.m_9236_().m_46468_();
        if (NpcRules.hides(true, day)) {
            return false;
        }
        if (body instanceof com.hex.kagune.control.npc.HumanNpc) {
            return NpcRules.prowls(true, day);
        }
        if (body instanceof ServerPlayer player) {
            // 4.1.53 (the owner: "Aogiri - Wild ghouls no longer attack unless provoked"): one of Aogiri is left alone (what hurt it,
            // the grudge above, it still goes for).
            if (who.gang() == 0 && !who.aogiri() && GangServer.of(player).member(com.hex.kagune.control.npc.GangPlayer.AOGIRI)) {
                return false;
            }
            return attitude(npc, who, player) == NpcTemper.Attitude.HOSTILE;
        }
        if (AgentBrain.ccg(body)) {
            return true; // 1.3.27: the CCG: whenever a wild ghoul and an investigator meet, they fight (4.1.46: or one of its ghoul division)
        }
        return body instanceof GhoulNpc other && other.identity() != null && who.temper().hunts(other.identity().temper());
    }

    /** 4.0.0: true for what an Aogiri member goes for unprovoked: an investigator, or anything going for one of Aogiri's own. */
    static boolean aogiriFoe(LivingEntity body) {
        if (AgentBrain.ccg(body)) {
            return true; // 4.1.46: the CCG's ghoul division is the CCG
        }
        return body instanceof Mob mob && mob.m_5448_() instanceof GhoulNpc target && target.identity() != null && target.identity().aogiri()
            && !(body instanceof GhoulNpc kin && kin.identity() != null && kin.identity().aogiri());
    }

    /** How it takes to {@code player}: its temper's way with what they are and the side they took, over how it regards them. */
    static NpcTemper.Attitude attitude(GhoulNpc npc, NpcIdentity who, ServerPlayer player) {
        NpcTemper.Attitude base = who.temper().toward(KaguneProfile.isUnlocked(player), side(player));
        return NpcBonds.attitude(base, QuestServer.bond(player, npc.m_20148_().toString()).regard());
    }

    /** The side {@code player} took in the quests ("anteiku", "pack"), or null. */
    static String side(ServerPlayer player) {
        QuestLog log = QuestServer.log(player);
        int anteiku = log.side(QuestBook.ANTEIKU);
        int pack = log.side(QuestBook.PACK);
        return anteiku > pack ? QuestBook.ANTEIKU : pack > anteiku ? QuestBook.PACK : null;
    }

    /** True for what its blows and shots may land on: whatever it goes for, and anyone it is not friendly with that gets in the way. */
    static boolean fair(GhoulNpc npc, NpcIdentity who, Mind mind, LivingEntity body, long now) {
        if (body == npc || !body.m_6084_() || body.m_5833_()) {
            return false;
        }
        if (wanted(npc, who, mind, body, now)) {
            return true;
        }
        if (who.aogiri() || who.gang() != 0) {
            return false; // 4.0.0: an Aogiri member's blows land only on what it is fighting (4.1.45: a gang's too)
        }
        if (body instanceof ServerPlayer player) {
            return !player.m_7500_() && !player.equals(owner(npc, mind)) && attitude(npc, who, player) != NpcTemper.Attitude.FRIENDLY;
        }
        return !(body instanceof GhoulNpc);
    }

    /** A coward is afraid when it is losing, or when a ghoul is close (and it has not been cornered into fighting). */
    private static boolean afraid(GhoulNpc npc, ServerLevel level, LivingEntity prey) {
        if (npc.m_21223_() < npc.m_21233_() * NpcRules.FLEE_BELOW) {
            return true;
        }
        if (prey != null) {
            return false; // cornered: it fights back
        }
        for (ServerPlayer player : level.m_6907_()) {
            if (!player.m_5833_() && player.m_20280_(npc) <= NpcRules.SHY * NpcRules.SHY && KaguneProfile.isUnlocked(player)) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- moving

    private static void fight(GhoulNpc npc, NpcIdentity who, Mind mind, LivingEntity prey, ServerLevel level, long now) {
        mind.foughtAt = now;
        // 4.0.0: an S or an SS, badly hurt, frenzies once a fight: faster, harder, knitting faster (NpcRules.frenzies).
        if (NpcRules.frenzies(who.tier()) && !mind.frenzied && npc.m_21223_() < npc.m_21233_() * NpcRules.FRENZY_BELOW) {
            mind.frenzied = true;
            mind.frenzyUntil = now + NpcRules.FRENZY_TICKS;
            npc.m_7292_(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.f_19596_, NpcRules.FRENZY_TICKS, 1,
                false, false, false));
            level.m_6263_(null, npc.m_20185_(), npc.m_20186_() + 1.0, npc.m_20189_(), HexKaguneSounds.KAGUNE_WHIP.get(), SoundSource.HOSTILE, 1.4F, 0.55F);
            mind.dirty = true;
        }
        if (!mind.out) {
            mind.out = true;
            mind.outAt = now;
            mind.dirty = true;
            level.m_6263_(null, npc.m_20185_(), npc.m_20186_() + 1.0, npc.m_20189_(), HexKaguneSounds.KAGUNE_WHIP.get(), SoundSource.HOSTILE, 0.9F,
                0.8F + 0.2F * (float)npc.m_217043_().m_188500_());
        }
        npc.m_21563_().m_24960_(prey, 30.0F, 30.0F);
        if (now - mind.outAt < NpcRules.DEPLOY_TICKS) {
            npc.m_21573_().m_26573_(); // the kagune is still tearing out
            if (mind.heavyReadyAt == Long.MIN_VALUE) {
                mind.heavyReadyAt = now + 60L; // (1.0.41: not its heavy the moment it is out)
            }
            return;
        }
        // 1.0.41: its feet first: a blow coming at it answered, a flip in or out.
        if (GhoulNpcCombat.maneuver(npc, who, mind, prey, level, now)) {
            return;
        }
        if (who.kind() == NpcIdentity.Kind.RINKAKU) {
            GhoulNpcCombat.lash(npc, who, mind, prey, level, now);
        } else if (who.kind() == NpcIdentity.Kind.KOUKAKU) {
            GhoulNpcCombat.slash(npc, who, mind, prey, level, now);
        } else {
            GhoulNpcCombat.shoot(npc, who, mind, prey, level, now);
        }
    }

    private static void flee(GhoulNpc npc, Mind mind, LivingEntity from, ServerLevel level, long now) {
        Entity threat = from;
        double run = 14.0;
        if (now < mind.scaredUntil && mind.scaredOf != 0 && level.m_6815_(mind.scaredOf) instanceof LivingEntity scary && scary.m_6084_()
            && scary.m_20280_(npc) <= 48.0 * 48.0) {
            // 1.3.65: away from what scared it (War above all), not whoever is nearest; from a terror, further at a time.
            threat = scary;
            if (scary instanceof ServerPlayer player && Sightings.seen(player).terror()) {
                run = com.hex.kagune.control.npc.FinalSight.TERROR_RUN;
            }
        }
        if (threat == null) {
            double best = Double.MAX_VALUE;
            for (ServerPlayer player : level.m_6907_()) {
                double distance = player.m_20280_(npc);
                if (distance < best) {
                    best = distance;
                    threat = player;
                }
            }
        }
        calm(npc, mind, now, true);
        mind.fleeing = true;
        if (threat == null || now % 10L != 0L && !npc.m_21573_().m_26571_()) {
            return;
        }
        Vec3 away = GhoulNpcCombat.flat(npc.m_20182_().m_82546_(threat.m_20182_()));
        Vec3 to = npc.m_20182_().m_82549_(away.m_82490_(run));
        int y = level.m_6924_(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int)Math.floor(to.f_82479_), (int)Math.floor(to.f_82481_));
        npc.m_21573_().m_26519_(to.f_82479_, y, to.f_82481_, 1.3);
    }

    /** 4.1.52: how near (blocks) a feared member comes before one of their gang steps back, and how far it steps (a little). */
    static final double YIELD_NEAR = 2.5;
    static final double YIELD_STEP = 1.6;

    /**
     * 4.1.52: one of a gang (a ghoul NPC; its leaders and the official NPCs are never ghoul NPCs: they never step back) with a feared member
     * of its gang near steps back out of their way, a block or two, looking at them; only some do ({@link GangStanding#yields}: by its own
     * seed, always the same ones). True while it does.
     */
    static boolean yields(GhoulNpc npc, NpcIdentity who, Mind mind, ServerLevel level, long now) {
        int gang = com.hex.kagune.control.npc.GangPlayer.of(who);
        if (gang == com.hex.kagune.control.npc.GangPlayer.NONE || !com.hex.kagune.control.npc.GangStanding.yields(who.seed())) {
            return false;
        }
        if (now < mind.yieldUntil) {
            return true;
        }
        for (ServerPlayer player : level.m_6907_()) {
            if (player.m_5833_() || player.m_20280_(npc) > YIELD_NEAR * YIELD_NEAR || !GangServer.of(player).member(gang)
                || GangServer.reputation(player) < com.hex.kagune.control.npc.GangStanding.FEARED) {
                continue;
            }
            double dx = npc.m_20185_() - player.m_20185_();
            double dz = npc.m_20189_() - player.m_20189_();
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length < 0.05) {
                dx = 1.0;
                dz = 0.0;
                length = 1.0;
            }
            npc.m_21573_().m_26519_(npc.m_20185_() + dx / length * YIELD_STEP, npc.m_20186_(), npc.m_20189_() + dz / length * YIELD_STEP, 0.9);
            npc.m_21563_().m_24960_(player, 30.0F, 30.0F);
            mind.yieldUntil = now + 30L;
            return true;
        }
        return false;
    }

    private static void idle(GhoulNpc npc, NpcIdentity who, Mind mind, ServerLevel level, long now) {
        calm(npc, mind, now, false);
        ServerPlayer owner = owner(npc, mind);
        if (owner != null) {
            accompany(npc, mind, owner, level, now);
            return;
        }
        if (mind.owner != null) {
            // Its player is away (gone, another world): it waits where it is.
            npc.m_21573_().m_26573_();
            return;
        }
        // 4.1.52 (the owner: "high reputation means you're feared, they react to it, some stepping back to let you pass (just a little and
        // not the leaders, they do not step back this also applies for official NPCS)"): some of a gang step out of a feared member's way.
        if (yields(npc, who, mind, level, now)) {
            return;
        }
        // 4.0.2: an Aogiri member at the hideout at its work: at its post, asking a newcomer their business, on patrol, about its floor.
        if (who.aogiri() && AogiriGuard.idle(npc, mind, level, now)) {
            return;
        }
        // 4.1.48: a circus Clown keeps to its tent: strolling about its spot, never walking off with the others.
        if (atCircus(npc, mind, level, now)) {
            return;
        }
        // 4.1.45: a gang walks together: one leads (wandering as any ghoul does), the rest keep up with it.
        if (who.gang() != 0 && roam(npc, mind, level, now)) {
            return;
        }
        if (who.temper() == NpcTemper.ANTEIKU || who.temper() == NpcTemper.DRIFTER) {
            // A quiet one stops for someone who comes near.
            for (ServerPlayer player : level.m_6907_()) {
                if (!player.m_5833_() && player.m_20280_(npc) <= 36.0) {
                    npc.m_21573_().m_26573_();
                    return;
                }
            }
        }
        if (now < mind.wanderAt || !npc.m_21573_().m_26571_()) {
            return;
        }
        // 4.0.0: an Aogiri member keeps to its hideout: wandered past its edge, it walks back to its post.
        VillagePlace.Place place = who.aogiri() ? VillagePlace.of(npc) : null;
        if (place != null && !place.inside(npc.m_20185_(), npc.m_20189_())) {
            mind.wanderAt = now + 60L;
            npc.m_21573_().m_26519_(place.homeX() + 0.5, place.homeY(), place.homeZ() + 0.5, 0.8);
            return;
        }
        mind.wanderAt = now + 80L + (long)(npc.m_217043_().m_188500_() * 160.0);
        double angle = npc.m_217043_().m_188500_() * Math.PI * 2.0;
        double distance = 5.0 + npc.m_217043_().m_188500_() * 9.0;
        double x = npc.m_20185_() + Math.cos(angle) * distance;
        double z = npc.m_20189_() + Math.sin(angle) * distance;
        int y = level.m_6924_(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int)Math.floor(x), (int)Math.floor(z));
        if (Math.abs(y - npc.m_20186_()) <= 6.0) {
            npc.m_21573_().m_26519_(x, y, z, 0.7);
        }
    }

    /** No fight: whatever it was making ends; a while after its last fight (or at once when it runs) the kagune goes back in. */
    private static void calm(GhoulNpc npc, Mind mind, long now, boolean hide) {
        if (mind.move != GraftMove.NONE) {
            mind.move = GraftMove.NONE;
            mind.dirty = true;
        }
        mind.strung = 0;
        mind.shots = 0;
        if (mind.volleyAt != Long.MIN_VALUE) {
            mind.volleyAt = Long.MIN_VALUE;
            mind.dirty = true;
        }
        if (mind.out && (hide || now - mind.foughtAt > NpcRules.CALM_TICKS)) {
            mind.frenzied = false; // 4.0.0: the next fight may frenzy it again
            mind.out = false;
            mind.outAt = now;
            mind.dirty = true;
        }
    }

    // ---------------------------------------------------------------- talking and walking with a player (7.0.2)

    /** True if it talks to {@code player} now: its temper would, it regards them well enough, and it is not after them. */
    static boolean talks(GhoulNpc npc, NpcIdentity who, Mind mind, ServerPlayer player, long now) {
        if (mind.prey == player.m_19879_() || mind.leavingAt != Long.MIN_VALUE || PaleStun.stunned(npc)) {
            return false;
        }
        Long until = mind.grudges.get(player.m_20148_());
        if (until != null && until > now) {
            return false;
        }
        return player.equals(owner(npc, mind)) || com.hex.kagune.control.npc.NpcTalk.talks(who.temper(), attitude(npc, who, player));
    }

    /** It turns to {@code player} and waits on them a while (null: the talk is over). */
    static void listen(GhoulNpc npc, Mind mind, ServerPlayer player, long now) {
        mind.talking = player == null ? 0 : player.m_19879_();
        mind.talkUntil = player == null ? Long.MIN_VALUE : now + 600L;
    }

    /** True while it stands talking: it faces whoever it talks to (until they walk off or the talk runs long). */
    private static boolean talking(GhoulNpc npc, Mind mind, ServerLevel level, long now) {
        if (mind.talking == 0) {
            return false;
        }
        Entity talker = level.m_6815_(mind.talking);
        // 1.3.39: held as long as the talk is open on their screen (it used to walk off after 30 seconds of reading).
        if (talker instanceof ServerPlayer player && GhoulNpcTalk.talkingWith(player, npc) && talker.m_20280_(npc) <= 100.0) {
            mind.talkUntil = Math.max(mind.talkUntil, now + 40L);
        }
        if (talker == null || now > mind.talkUntil || talker.m_20280_(npc) > 100.0) {
            mind.talking = 0;
            return false;
        }
        npc.m_21573_().m_26573_();
        npc.m_21563_().m_24960_(talker, 30.0F, 30.0F);
        return true;
    }

    /** The player it walks with, if they are here (online, in its world). */
    static ServerPlayer owner(GhoulNpc npc, Mind mind) {
        if (mind == null || mind.owner == null || !(npc.m_9236_() instanceof ServerLevel level)) {
            return null;
        }
        for (ServerPlayer player : level.m_6907_()) {
            if (player.m_20148_().equals(mind.owner)) {
                return player.m_6084_() ? player : null;
            }
        }
        return null;
    }

    /** True if it walks with {@code player}. */
    static boolean allyOf(GhoulNpc npc, ServerPlayer player) {
        return npc.mind != null && player.m_20148_().equals(npc.mind.owner);
    }

    /** True if it follows its player (false: it waits where it was told). */
    static boolean following(GhoulNpc npc) {
        return npc.mind == null || npc.mind.follow;
    }

    /** It walks with {@code player} from now: kept (it never slips away), following them. */
    static void bind(GhoulNpc npc, ServerPlayer player) {
        Mind mind = npc.mind;
        if (mind == null) {
            return;
        }
        mind.owner = player.m_20148_();
        mind.follow = true;
        mind.grudges.remove(player.m_20148_());
        npc.getPersistentData().m_128362_(ALLY, player.m_20148_());
        npc.getPersistentData().m_128379_(WAITS, false);
        npc.m_21530_();
    }

    /** Sent off: it no longer walks with anyone, and goes its own way (out of sight in a few seconds). */
    static void unbind(GhoulNpc npc, long now) {
        Mind mind = npc.mind;
        if (mind == null) {
            return;
        }
        mind.owner = null;
        mind.leavingAt = now;
        npc.getPersistentData().m_128473_(ALLY);
    }

    /** Follow its player, or wait here. */
    static void follow(GhoulNpc npc, boolean follow) {
        Mind mind = npc.mind;
        if (mind == null) {
            return;
        }
        mind.follow = follow;
        mind.waitAt = npc.m_20182_();
        npc.getPersistentData().m_128379_(WAITS, !follow);
    }

    /** Provoked by {@code player}: it goes for them. */
    static void provoke(GhoulNpc npc, Mind mind, ServerPlayer player, NpcIdentity who, long now) {
        int grudge = who.temper().grudge();
        mind.grudges.put(player.m_20148_(), grudge == Integer.MAX_VALUE ? Long.MAX_VALUE : now + grudge);
        mind.preySought = Long.MIN_VALUE;
        listen(npc, mind, null, now);
    }

    /** Scared off: it runs a while. */
    static void scare(GhoulNpc npc, Mind mind, long now) {
        mind.scaredUntil = now + 120L;
        mind.scaredOf = 0;
        listen(npc, mind, null, now);
    }

    /** True if a ghoul NPC walking with {@code player} is near them (the quests' ally tag). */
    public static boolean allyNear(ServerPlayer player) {
        for (GhoulNpc npc : player.m_284548_().m_6443_(GhoulNpc.class, player.m_20191_().m_82400_(16.0), GhoulNpc::m_6084_)) {
            if (allyOf(npc, player)) {
                return true;
            }
        }
        return false;
    }

    /** How many ghoul NPCs walk with {@code player} round them now. */
    static int allies(ServerPlayer player) {
        int count = 0;
        for (GhoulNpc npc : player.m_284548_().m_6443_(GhoulNpc.class, player.m_20191_().m_82400_(128.0), GhoulNpc::m_6084_)) {
            if (allyOf(npc, player)) {
                count++;
            }
        }
        return count;
    }

    /** An ally at rest: it keeps near its player (and catches up at once when left far behind), or waits where it was told. */
    private static void accompany(GhoulNpc npc, Mind mind, ServerPlayer owner, ServerLevel level, long now) {
        if (!mind.follow) {
            Vec3 at = mind.waitAt == null ? npc.m_20182_() : mind.waitAt;
            if (npc.m_20238_(at) > 16.0 && npc.m_21573_().m_26571_()) {
                npc.m_21573_().m_26519_(at.f_82479_, at.f_82480_, at.f_82481_, 0.9);
            }
            return;
        }
        double distance = npc.m_20280_(owner);
        if (distance > 32.0 * 32.0 && owner.m_20096_()) {
            Vec3 behind = owner.m_20182_().m_82546_(GhoulNpcCombat.flat(owner.m_20154_()).m_82490_(2.0));
            npc.m_7678_(behind.f_82479_, owner.m_20186_(), behind.f_82481_, owner.m_146908_(), 0.0F);
            npc.m_21573_().m_26573_();
        } else if (distance > 25.0) {
            if (now % 10L == 0L || npc.m_21573_().m_26571_()) {
                npc.m_21573_().m_5624_(owner, distance > 100.0 ? 1.3 : 1.05);
            }
        } else if (distance < 6.0) {
            npc.m_21573_().m_26573_();
        }
    }

    /** Sent off: it walks away from whoever it was with, and is gone a few seconds on. */
    private static void leave(GhoulNpc npc, Mind mind, ServerLevel level, long now) {
        if (now - mind.leavingAt > 100L) {
            npc.m_146870_();
            return;
        }
        if (now - mind.leavingAt == 1L || npc.m_21573_().m_26571_()) {
            Vec3 away = GhoulNpcCombat.flat(npc.m_20154_()).m_82490_(-1.0);
            Vec3 to = npc.m_20182_().m_82549_(away.m_82490_(16.0));
            npc.m_21573_().m_26519_(to.f_82479_, npc.m_20186_(), to.f_82481_, 0.9);
        }
    }

    // ---------------------------------------------------------------- living

    private static void regen(GhoulNpc npc, Mind mind, long now) {
        boolean rested = mind.hurtAt == Long.MIN_VALUE || now - mind.hurtAt >= NpcRules.REGEN_AFTER; // 1.0.15: the same overflow
        // 4.0.0: the stronger, the faster it knits (its rank); an S or SS in its frenzy faster still.
        NpcIdentity who = npc.identity();
        int every = who == null ? NpcRules.REGEN_EVERY : NpcRules.regenEvery(who.tier());
        if (now < mind.frenzyUntil) {
            every = Math.max(6, every / 2);
            rested = true;
        }
        if (rested && now % every == 0L && npc.m_21223_() < npc.m_21233_()) {
            npc.m_5634_(1.0F);
        }
    }

    /** At dawn, with no one near enough to see it go, it slips away (one kept as an ally stays). True if it went. */
    private static boolean dawn(GhoulNpc npc, ServerLevel level, long now) {
        if (now % 100L != 0L || NpcRules.night(level.m_46468_()) || npc.m_21532_()) {
            return false;
        }
        for (ServerPlayer player : level.m_6907_()) {
            if (player.m_20280_(npc) <= NpcRules.DAWN_WATCHED * NpcRules.DAWN_WATCHED) {
                return false;
            }
        }
        if (npc.mind != null) {
            GhoulNpcCombat.fizzleAll(npc, npc.mind);
        }
        npc.m_146870_();
        return true;
    }

    /** It was hurt: whoever did it is remembered for as long as its temper holds a grudge. */
    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof GhoulNpc npc) || !(npc.m_9236_() instanceof ServerLevel)) {
            return;
        }
        Mind mind = npc.mind;
        NpcIdentity who = npc.identity();
        if (mind == null || who == null) {
            return;
        }
        long now = npc.m_9236_().m_46467_();
        mind.hurtAt = now;
        Entity source = event.getSource().m_7639_();
        if (source instanceof ServerPlayer member && GangServer.stance(member, who) == GangServer.OWN_GANG && !GangServer.turned(member)) {
            GangServer.struckOwn(member, now); // 4.1.52: their reputation down a little
            return; // 4.1.50: one of its own's blows: never held against them, nobody called
        }
        if (source instanceof LivingEntity attacker && attacker != npc) {
            int grudge = who.temper().grudge();
            boolean holds = true;
            if (attacker instanceof ServerPlayer player) {
                // A blow from a player cools its regard; a friend forgives one stray blow, but the blow that turns it is a betrayal.
                String key = npc.m_20148_().toString();
                QuestLog.Bond bond = QuestServer.bond(player, key);
                int after = NpcBonds.struck(bond.regard());
                if (NpcBonds.betrayal(bond.regard())) {
                    QuestServer.signal(player, Signal.BETRAY, 1, Tags.temper(who.temper().name().toLowerCase(java.util.Locale.ROOT)));
                    player.m_5661_(Component.m_237113_(who.displayName() + " trusted you."), true);
                }
                QuestServer.bond(player, key, bond.regard(after));
                holds = after < 0;
                listen(npc, mind, null, now);
            }
            if (who.aogiri()) {
                // 4.0.0: an Aogiri member always defends itself, and its comrades near it take up its fight.
                holds = true;
                grudge = AOGIRI_GRUDGE;
                if (!(attacker instanceof GhoulNpc kin && kin.identity() != null && kin.identity().aogiri())) {
                    for (GhoulNpc comrade : npc.m_9236_().m_6443_(GhoulNpc.class, npc.m_20191_().m_82400_(AOGIRI_CALL),
                        other -> other != npc && other.m_6084_() && other.identity() != null && other.identity().aogiri() && other.mind != null)) {
                        comrade.mind.grudges.put(attacker.m_20148_(), now + AOGIRI_GRUDGE);
                        comrade.mind.preySought = Long.MIN_VALUE;
                    }
                }
            }
            if (who.gang() != 0) {
                // 4.1.45: a gang always defends its own: whoever hurt one of them has every one near on him. 4.1.46: never one of its own
                // (a stray blow), nor, for the CCG's ghoul division, one of the CCG's; the investigators near take up its fight.
                boolean own = attacker instanceof GhoulNpc kin && mates(npc, kin) || who.ccg() && AgentBrain.ccg(attacker);
                holds = !own;
                grudge = GANG_GRUDGE;
                if (!own) {
                    for (GhoulNpc mate : npc.m_9236_().m_6443_(GhoulNpc.class, npc.m_20191_().m_82400_(GANG_CALL),
                        other -> other != npc && other.m_6084_() && other.mind != null && mates(npc, other))) {
                        mate.mind.grudges.put(attacker.m_20148_(), now + GANG_GRUDGE);
                        mate.mind.preySought = Long.MIN_VALUE;
                    }
                    if (who.ccg()) {
                        AgentBrain.called(attacker, npc, now + GANG_GRUDGE);
                    }
                }
            }
            if (holds) {
                mind.grudges.put(attacker.m_20148_(), grudge == Integer.MAX_VALUE ? Long.MAX_VALUE : now + grudge);
            }
            mind.preySought = Long.MIN_VALUE; // it looks round again at once
        }
    }

    /** An ally can't be hurt by its own player (a sweep that catches it, a stray shot): friendly fire is nothing between them. */
    @SubscribeEvent
    public static void attacked(LivingAttackEvent event) {
        if (event.getEntity() instanceof GhoulNpc npc && event.getSource().m_7639_() instanceof ServerPlayer player && allyOf(npc, player)) {
            event.setCanceled(true);
        }
    }

    // ---------------------------------------------------------------- what the clients see

    private static void sync(GhoulNpc npc, Mind mind, NpcIdentity who) {
        if (!mind.dirty) {
            return;
        }
        mind.dirty = false;
        ControlNetwork.broadcastAround(npc, message(npc, mind, who));
    }

    static S2CGhoulNpc message(GhoulNpc npc, Mind mind, NpcIdentity who) {
        if (mind == null) {
            return new S2CGhoulNpc(npc.m_19879_(), who.pack(), false, Long.MIN_VALUE, GraftMove.NONE.ordinal(), Long.MIN_VALUE, 0.0, 0.0, 0.0, 0,
                Long.MIN_VALUE, Long.MIN_VALUE, 1, -1, Long.MIN_VALUE);
        }
        // 1.3.36: an arm blade's cut goes where a lash would (its clients read it by its kind).
        boolean blade = who.kind() == NpcIdentity.Kind.KOUKAKU;
        return new S2CGhoulNpc(npc.m_19879_(), who.pack(), mind.out, mind.outAt, blade ? mind.blade : mind.move.ordinal(), blade ? mind.bladeAt : mind.moveAt,
            mind.point.f_82479_,
            mind.point.f_82480_, mind.point.f_82481_, mind.prey, mind.volleyAt, mind.shotAt, mind.shotSide, mind.flip, mind.flipAt, mind.eyes && !mind.out);
    }

    /** A player starts seeing it: they are told who it is and what it is doing. */
    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof GhoulNpc npc && event.getEntity() instanceof ServerPlayer player && npc.identity() != null) {
            ControlNetwork.sendTo(player, message(npc, npc.mind, npc.identity()));
        }
    }

    /** Where {@code body}'s middle is (for aiming at it). */
    static Vec3 centre(LivingEntity body) {
        return KaguneServerController.center(body);
    }

    /** The bodies a beat has struck (each at most once). */
    static Set<Integer> hitSet(Mind mind, int key) {
        return mind.hits.computeIfAbsent(key, ignored -> new HashSet<>());
    }
}
