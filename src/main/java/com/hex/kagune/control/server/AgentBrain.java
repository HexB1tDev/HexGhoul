package com.hex.kagune.control.server;

import com.hex.kagune.control.ControlItems;
import com.hex.kagune.control.npc.AgentRules;
import com.hex.kagune.control.npc.CcgAgent;
import com.hex.kagune.control.npc.ChildNpc;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.FearLines;
import com.hex.kagune.control.npc.FinalSight;
import com.hex.kagune.control.npc.FleshThrall;
import com.hex.kagune.control.npc.GhoulNpc;
import com.hex.kagune.control.npc.NpcRules;
import com.hex.kagune.control.pale.PaleRules;
import com.hex.kagune.control.quinque.Quinque;
import com.hex.kagune.control.wound.WoundBook;
import com.hex.kagune.data.KaguneProfile;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.3.27: what a CCG investigator does ({@link AgentRules}), decided on the server every tick ({@link CcgAgent}'s own AI step).
 *
 * <p>At peace it walks the world with its partner (the leader picks the way, the other keeps at its side), its quinque shut in its case.
 * It goes for a monster that comes too close, a wild ghoul in sight, a player who shows their kagune (marked a while: the pair keeps
 * after them), and anyone who strikes either of them; whatever its partner fights, it fights too.
 *
 * <p>1.3.28: two on one prey never come at it in a line ({@link AgentRules#front}): one holds the front (straight on, squared up just out
 * of reach, keeping its prey's eyes on it), the other goes round wide and comes in from behind; when the prey turns on the one behind,
 * the two swap.
 *
 * <p>In a fight: the case opens in its hand first (the quinque's own unfold). Then its feet: a blow coming at it ({@link KaguneClash#threat})
 * is met with its own (the two thrown apart: the kagune slapped aside, and it answers at once) or flipped and dashed clear of; prey too far
 * is closed on with a front flip, too close (or a string just finished) is left with a back flip. Then its weapon: a string of three M1s
 * (each registered as a blow on its way, so it clashes with a ghoul's as a player's does; the third the finisher, the weapon's own); the
 * launcher keeps its distance and fires bursts of crystal shards. After the fight, the case shuts again.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class AgentBrain {
    /** One investigator's state on the server. */
    public static final class Mind {
        int look;
        /** 1.3.70: its rank (AgentRules.Rank). 2.0.20: whether it is the superior of a pair (stronger than its partner whatever their ranks). */
        AgentRules.Rank rank = AgentRules.Rank.RANK_0;
        boolean superior;
        Quinque kind = Quinque.BLADE;
        boolean leader;
        UUID partner;
        int partnerId;
        long partnerSeen = Long.MIN_VALUE;
        /** What it goes for (an entity id, 0 none), and when it last looked round. */
        int prey;
        long preySought = Long.MIN_VALUE;
        /** Who it holds something against, and until when (a revealed ghoul's mark, a blow's grudge). */
        final Map<UUID, Long> grudges = new HashMap<>();
        long hurtAt = Long.MIN_VALUE;
        /** Its case: open or shut, and its last fight. */
        boolean open;
        long foughtAt = Long.MIN_VALUE;
        /** Its string: the last of the three (-1 none) and when it set out; the blow under way (or the parry), when it lands, at whom. */
        int step = -1;
        long stepAt = Long.MIN_VALUE;
        boolean striking;
        boolean parrying;
        long landsAt = Long.MIN_VALUE;
        int target;
        int token;
        long readyAt = Long.MIN_VALUE;
        long answered = Long.MIN_VALUE;
        long kickAt = Long.MIN_VALUE;
        long kickReadyAt = Long.MIN_VALUE;
        int kickTarget;
        long aimAt = Long.MIN_VALUE;
        long lightningReadyAt = Long.MIN_VALUE;
        int aimTarget;
        /** A finisher just landed and it means to flip clear of it. */
        boolean clear;
        /** Its feet: its flip and when, its push; its dash, when, which way; when it may flip and dash again. */
        long flipAt = Long.MIN_VALUE;
        Vec3 push;
        long dashAt = Long.MIN_VALUE;
        Vec3 dashWay = Vec3.f_82478_;
        /** 2.0.18: dashes still to come in its zigzag, the line to its prey it zigzags about, whether it zigzags in on them. */
        int zigzag;
        Vec3 zigAxis = Vec3.f_82478_;
        boolean zigIn;
        long flipReadyAt = Long.MIN_VALUE;
        long dashReadyAt = Long.MIN_VALUE;
        /** The launcher: shards of this burst, the next shard, when the next burst may start. */
        int shots;
        long nextShot = Long.MIN_VALUE;
        long burstReadyAt = Long.MIN_VALUE;
        /** Walking: when it next picks a way; which way it circles. */
        long wanderAt = Long.MIN_VALUE;
        double circle = 1.0;
        long circleAt = Long.MIN_VALUE;
        /**
         * 1.3.28: the two of them on one prey: when this fight began; whether it is going round to the prey's back now; (the leader's,
         * for the pair) whether their sides have swapped since it began, and since when the prey has been turned on the one behind.
         */
        long fightFrom = Long.MIN_VALUE;
        /** 1.3.71: the village patrol's next waypoint on its loop (-1: to be joined). */
        int waypoint = -1;
        boolean rounding;
        boolean swapped;
        long turnedSince = Long.MIN_VALUE;
        /**
         * 1.3.65: one of the worst sights it faltered at (an entity id), when; until when it stands frozen, then falls back; whether it
         * finds its nerve (a word) once that is over.
         */
        int falterFrom;
        long falteredAt = Long.MIN_VALUE;
        long falterUntil = Long.MIN_VALUE;
        long fallBackUntil = Long.MIN_VALUE;
        boolean rallies;
    }

    /** Its partner's id in its saved data (who it is: {@link CcgAgent#KEY}, packed). */
    static final String PARTNER = CcgAgent.KEY + "Partner";

    /** 1.3.70: its rank in its saved data. */
    static final String RANK = CcgAgent.KEY + "Rank";
    /** 2.0.17: its rank's number (0 to 10). */
    static final String RANK_LEVEL = CcgAgent.KEY + "RankLevel";

    private AgentBrain() {
    }

    /** Ticks from {@code at} to {@code now}; "never" is forever ago (never overflows). */
    static long ago(long now, long at) {
        return at == Long.MIN_VALUE ? Long.MAX_VALUE : now - at;
    }

    // ---------------------------------------------------------------- who it is

    /** {@code a} and {@code b} made a pair: each given its look and quinque, {@code a} leading; kept in their saved data. */
    public static void pair(CcgAgent a, int lookA, Quinque kindA, CcgAgent b, int lookB, Quinque kindB) {
        save(a, lookA, kindA, true, b.m_20148_());
        save(b, lookB, kindB, false, a.m_20148_());
        a.dress(lookA, kindA);
        b.dress(lookB, kindB);
    }

    private static void save(CcgAgent agent, int look, Quinque kind, boolean leader, UUID partner) {
        CompoundTag saved = agent.getPersistentData();
        saved.m_128356_(CcgAgent.KEY, (long)(look & 0xFF) | (long)kind.ordinal() << 8 | (leader ? 1L : 0L) << 16);
        if (partner != null) {
            saved.m_128362_(PARTNER, partner);
        }
    }

    private static final String REAPER = "ccg:reaper";
    private static final String REAPER_INITIALIZED = CcgAgent.KEY + "ReaperInitialized";

    static boolean claimReaper(CcgAgent agent, ServerLevel level) {
        return UniqueNpcs.claimExclusive(level, REAPER, agent.m_20148_());
    }

    /**
     * 2.0.29 (the owner: "Make sure that the rank 10 investigator can be spawned via command for testing purposes"): {@code /kagune reaper}
     * puts the White Reaper five blocks ahead of {@code player}. Never two: a Reaper already loaded anywhere is sent away first, and one
     * standing in unloaded ground gives the slot up (when that ground loads he wakes an ordinary Rank 9 investigator without Dawn). What
     * the operator is told back.
     */
    public static String summonReaper(ServerPlayer player) {
        ServerLevel level = player.m_284548_();
        com.hex.kagune.control.npc.UniqueNpc slot = UniqueNpcs.get(level, REAPER);
        boolean replaced = false;
        if (slot.alive && slot.body != null) {
            net.minecraft.server.MinecraftServer server = level.m_7654_();
            for (ServerLevel each : server == null ? java.util.List.of(level) : server.m_129785_()) {
                if (each.m_8791_(slot.body) instanceof CcgAgent old) {
                    old.m_146870_();
                    replaced = true;
                }
            }
            UniqueNpcs.died(level, REAPER, slot.body);
        }
        Vec3 ahead = UkakuFire.horizontal(player.m_20154_()).m_82490_(5.0);
        int x = (int)Math.floor(player.m_20185_() + ahead.f_82479_);
        int z = (int)Math.floor(player.m_20189_() + ahead.f_82481_);
        CcgAgent agent = AgentSpawner.placeAt(level, x, (int)Math.floor(player.m_20186_() + 0.01), z);
        if (agent == null) {
            agent = AgentSpawner.place(level, x + 0.5, z + 0.5);
        }
        if (agent == null) {
            return "No room for the White Reaper there.";
        }
        if (!claimReaper(agent, level)) {
            agent.m_146870_();
            return "The White Reaper's slot is held: try again.";
        }
        prepareReaper(agent, AgentRules.REAPER_LOOK);
        return replaced ? "The White Reaper (the one before him sent away)." : "The White Reaper.";
    }

    static void prepareReaper(CcgAgent agent, int look) {
        // 2.0.28: always in the owner's own skin for him (AgentRules.REAPER_LOOK), whatever look was rolled.
        save(agent, AgentRules.REAPER_LOOK, Quinque.DAWN, true, null);
        agent.getPersistentData().m_128405_(RANK_LEVEL, AgentRules.Rank.RANK_10.level());
        agent.m_21530_();
    }

    /** Its first tick: who it is, from its saved data (one made alone, by a command, becomes someone, and its partner turns up). */
    private static Mind wake(CcgAgent agent, ServerLevel level, long now) {
        Mind mind = new Mind();
        CompoundTag saved = agent.getPersistentData();
        if (saved.m_128441_(CcgAgent.KEY)) {
            long packed = saved.m_128454_(CcgAgent.KEY);
            mind.look = (int)(packed & 0xFF);
            mind.kind = Quinque.values()[Math.floorMod((int)(packed >> 8 & 0xFF), Quinque.values().length)];
            mind.leader = (packed >> 16 & 1L) != 0L;
            mind.partner = saved.m_128403_(PARTNER) ? saved.m_128342_(PARTNER) : null;
        } else if (saved.m_128441_(VillagePlace.KEY)) {
            // 1.3.71: one of the Brick Village's patrol: someone of its own, walking its loop alone (no partner turns up for it).
            mind.look = AgentRules.regularLook(agent.m_217043_().m_188500_());
            mind.kind = AgentRules.kind(agent.m_217043_().m_188500_());
            mind.leader = true;
            save(agent, mind.look, mind.kind, true, null);
        } else {
            mind.look = AgentRules.regularLook(agent.m_217043_().m_188500_());
            mind.kind = AgentRules.kind(agent.m_217043_().m_188500_());
            mind.leader = true;
            save(agent, mind.look, mind.kind, true, null);
            CcgAgent other = AgentSpawner.partner(agent, level, mind.look, mind.kind);
            if (other != null) {
                mind.partner = other.m_20148_();
                save(agent, mind.look, mind.kind, true, other.m_20148_());
            }
        }
        mind.partnerSeen = now;
        // 1.3.70: its rank, by chance (kept in its saved data; the one leading the pair takes the better of two chances), its health and its
        // speed by it, and its name.
        // 2.0.17: Rank 0 to 10 (saved by number under RANK_LEVEL; one saved before by the old order under RANK), its armour too.
        CompoundTag data = agent.getPersistentData();
        boolean fresh = !data.m_128441_(RANK_LEVEL) && !data.m_128441_(RANK);
        mind.rank = data.m_128441_(RANK_LEVEL) ? AgentRules.Rank.byOrdinal(data.m_128451_(RANK_LEVEL))
            : data.m_128441_(RANK) ? AgentRules.Rank.fromOld(data.m_128451_(RANK))
            : AgentRules.Rank.of(mind.leader, agent.m_217043_().m_188500_(), agent.m_217043_().m_188500_());
        boolean reaper = mind.rank == AgentRules.Rank.RANK_10 && !fresh && mind.partner == null
            && !saved.m_128441_(VillagePlace.KEY) && claimReaper(agent, level);
        if (reaper) {
            mind.look = AgentRules.REAPER_LOOK; // 2.0.28: his own skin, a Reaper saved before included
            mind.kind = Quinque.DAWN;
            agent.m_21530_();
            agent.m_21051_(net.minecraft.world.entity.ai.attributes.Attributes.f_22278_).m_22100_(AgentRules.REAPER_KNOCKBACK_RESISTANCE);
        } else {
            mind.rank = AgentRules.regularRank(mind.rank);
            if (mind.look >= AgentRules.REGULAR_LOOKS) {
                mind.look = Math.floorMod(mind.look, AgentRules.REGULAR_LOOKS); // 2.0.28: only the White Reaper wears his skin
            }
            if (mind.kind == Quinque.DAWN) {
                // 2.0.29: a Reaper who lost his slot (another summoned while he lay in unloaded ground) is an ordinary investigator now.
                mind.kind = AgentRules.kind(agent.m_217043_().m_188500_());
                save(agent, mind.look, mind.kind, mind.leader, mind.partner);
            }
        }
        agent.dress(mind.look, mind.kind);
        // 2.0.20: the pair's leader is its superior, always the stronger: set against its partner's rank if that is known already (the
        // one who wakes second is set by the other), and a superior's edge on top.
        if (mind.partner != null && level.m_8791_(mind.partner) instanceof CcgAgent other) {
            CompoundTag theirData = other.getPersistentData();
            AgentRules.Rank theirs = other.mind != null ? other.mind.rank
                : theirData.m_128441_(RANK_LEVEL) ? AgentRules.Rank.byOrdinal(theirData.m_128451_(RANK_LEVEL)) : null;
            if (theirs != null) {
                mind.rank = mind.leader ? AgentRules.superior(mind.rank, theirs) : AgentRules.subordinate(mind.rank, theirs);
            }
        }
        mind.superior = mind.leader && mind.partner != null;
        data.m_128405_(RANK_LEVEL, mind.rank.level());
        agent.m_21051_(net.minecraft.world.entity.ai.attributes.Attributes.f_22276_).m_22100_(AgentRules.health(mind.rank, mind.superior));
        agent.m_21051_(net.minecraft.world.entity.ai.attributes.Attributes.f_22279_).m_22100_(mind.rank.speed);
        agent.m_21051_(net.minecraft.world.entity.ai.attributes.Attributes.f_22284_).m_22100_(mind.rank.armor);
        if (fresh || reaper && !saved.m_128441_(REAPER_INITIALIZED)) {
            agent.m_21153_(agent.m_21233_());
            if (reaper) {
                saved.m_128405_(REAPER_INITIALIZED, 1);
            }
        }
        agent.m_6593_(Component.m_237113_(mind.rank.title));
        agent.mind = mind;
        return mind;
    }

    // ---------------------------------------------------------------- the tick

    public static void tick(CcgAgent agent) {
        if (!(agent.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        long now = level.m_46467_();
        Mind mind = agent.mind == null ? wake(agent, level, now) : agent.mind;
        arm(agent, mind);
        CcgAgent partner = partner(agent, mind, level, now);
        if (lost(agent, mind, partner, level, now)) {
            return;
        }
        regen(agent, mind, now);
        if (feet(agent, mind, now)) {
            return; // in a flip or a dash: nothing else this tick
        }
        if (PaleStun.stunned(agent)) {
            cancelKick(agent, mind, now);
            cancelLightning(agent, mind, now);
            return;
        }
        if (falter(agent, mind, level, now)) {
            return; // 1.3.65: frozen at War (or worse), or falling back from it
        }
        LivingEntity prey = prey(agent, mind, partner, level, now);
        if (prey != null) {
            fight(agent, mind, partner, prey, level, now);
        } else {
            patrol(agent, mind, partner, level, now);
        }
    }

    /** The stack in its hand is its quinque: its case shut, or its weapon while it is open (put back if anything took it). */
    private static void arm(CcgAgent agent, Mind mind) {
        ItemStack held = agent.m_21205_();
        Quinque want = mind.kind;
        boolean right = mind.open ? ControlItems.quinqueWeapon(held) == want : ControlItems.quinqueCase(held) == want;
        if (!right) {
            agent.m_21008_(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(mind.open ? ControlItems.weaponOf(want) : ControlItems.caseOf(want), 1));
        }
    }

    /** Its partner, if it is still about: known by its entity id once found, looked for round it each second while it isn't. */
    private static CcgAgent partner(CcgAgent agent, Mind mind, ServerLevel level, long now) {
        if (mind.partner == null) {
            return null;
        }
        if (mind.partnerId != 0 && level.m_6815_(mind.partnerId) instanceof CcgAgent known && known.m_6084_()
            && mind.partner.equals(known.m_20148_())) {
            mind.partnerSeen = now;
            return known;
        }
        mind.partnerId = 0;
        if (now % 20L != Math.floorMod(agent.m_19879_(), 20)) {
            return null;
        }
        for (CcgAgent other : level.m_6443_(CcgAgent.class, agent.m_20191_().m_82400_(72.0), a -> a != agent && a.m_6084_())) {
            if (mind.partner.equals(other.m_20148_())) {
                mind.partnerSeen = now;
                mind.partnerId = other.m_19879_();
                return other;
            }
        }
        return null;
    }

    /**
     * Its partner gone a while (dead, or gone off with the world's far edge): with no player near to see it, it goes too; with one near,
     * it carries on alone, leading itself. True if it went.
     */
    private static boolean lost(CcgAgent agent, Mind mind, CcgAgent partner, ServerLevel level, long now) {
        if (mind.rank == AgentRules.Rank.RANK_10 || partner != null || ago(now, mind.partnerSeen) < AgentRules.PARTNER_LOST) {
            return false;
        }
        mind.leader = true;
        if (mind.prey != 0 || agent.getPersistentData().m_128441_(VillagePlace.KEY)) {
            return false; // (1.3.71: the village's patrol never leaves it)
        }
        for (ServerPlayer player : level.m_6907_()) {
            if (player.m_20280_(agent) <= 48.0 * 48.0) {
                return false;
            }
        }
        agent.m_146870_();
        return true;
    }

    /** Unhurt a while, it slowly mends (a human: nothing like a ghoul). */
    private static void regen(CcgAgent agent, Mind mind, long now) {
        boolean reaper = mind.rank == AgentRules.Rank.RANK_10;
        if (ago(now, mind.hurtAt) > 200L && (!reaper || ago(now, mind.foughtAt) > 200L)
            && now % 40L == 0L && agent.m_21223_() < agent.m_21233_()) {
            agent.m_5634_(reaper ? 3.0F : 1.0F);
        }
    }

    // ---------------------------------------------------------------- what it goes for

    private static LivingEntity prey(CcgAgent agent, Mind mind, CcgAgent partner, ServerLevel level, long now) {
        LivingEntity defend = null;
        if (now % 10L == Math.floorMod(agent.m_19879_(), 10)) {
            reveal(agent, mind, partner, level, now); // (kept up in a fight too: a kagune still out keeps them marked)
            // 1.3.70 (the owner: "CCG agents top priority if to defend humans, so they will drop what they're doing to protect them"):
            // whatever is hurting or hunting a human near, it goes for, before anything else.
            defend = defend(agent, level, now);
            if (defend != null) {
                mark(mind, defend.m_20148_(), now + AgentRules.MARKED);
                if (partner != null && partner.mind != null) {
                    mark(partner.mind, defend.m_20148_(), now + AgentRules.MARKED);
                }
            }
        }
        LivingEntity current = mind.prey != 0 && level.m_6815_(mind.prey) instanceof LivingEntity living ? living : null;
        if (current != null && (!current.m_6084_() || current.m_20280_(agent) > AgentRules.LEASH * AgentRules.LEASH
            || !wanted(agent, mind, partner, current, now, true))) {
            current = null;
        }
        if (current == null && ago(now, mind.preySought) >= 10L) {
            mind.preySought = now;
            // What its partner fights, it fights.
            Mind theirs = partner == null ? null : partner.mind;
            if (theirs != null && theirs.prey != 0 && level.m_6815_(theirs.prey) instanceof LivingEntity shared && shared.m_6084_()
                && shared.m_20280_(agent) <= AgentRules.LEASH * AgentRules.LEASH && !(shared instanceof CcgAgent)) {
                current = shared;
            } else {
                double best = Double.MAX_VALUE;
                double range = Math.max(AgentRules.GHOUL_RANGE, AgentRules.REVEALED_RANGE);
                for (LivingEntity body : level.m_6443_(LivingEntity.class, agent.m_20191_().m_82400_(range), living -> living != agent && living.m_6084_())) {
                    double distance = body.m_20280_(agent);
                    if (distance < best && wanted(agent, mind, partner, body, now, false) && agent.m_142582_(body)) {
                        best = distance;
                        current = body;
                    }
                }
            }
        }
        if (defend != null && defend.m_20280_(agent) <= AgentRules.LEASH * AgentRules.LEASH) {
            current = defend; // it drops whatever it was doing
        }
        int id = current == null ? 0 : current.m_19879_();
        if (id != mind.prey) {
            mind.prey = id;
            agent.m_6710_(current);
        }
        return current;
    }

    /**
     * 1.3.70: something hurting a human within {@link AgentRules#DEFEND} (whoever struck it lately, investigators aside) or hunting one (a
     * ghoul NPC with a human for prey), the nearest; null if every human near is safe.
     */
    static LivingEntity defend(CcgAgent agent, ServerLevel level, long now) {
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (com.hex.kagune.control.npc.HumanNpc human : level.m_6443_(com.hex.kagune.control.npc.HumanNpc.class,
                agent.m_20191_().m_82400_(AgentRules.DEFEND), h -> h.m_6084_() && h.mind != null)) {
            if (human.mind.hurtBy == null || now >= human.mind.hurtUntil) {
                continue;
            }
            Entity by = level.m_8791_(human.mind.hurtBy);
            if (by instanceof LivingEntity living && living.m_6084_() && !(living instanceof CcgAgent) && !(living instanceof Player p && p.m_7500_())
                && living.m_20280_(agent) < bestDistance) {
                best = living;
                bestDistance = living.m_20280_(agent);
            }
        }
        for (GhoulNpc ghoul : level.m_6443_(GhoulNpc.class, agent.m_20191_().m_82400_(AgentRules.DEFEND), g -> g.m_6084_() && g.mind != null)) {
            if (ghoul.mind.prey != 0 && level.m_6815_(ghoul.mind.prey) instanceof com.hex.kagune.control.npc.HumanNpc
                && ghoul.m_20280_(agent) < bestDistance) {
                best = ghoul;
                bestDistance = ghoul.m_20280_(agent);
            }
        }
        return best;
    }

    /**
     * True if it goes for {@code body}: one it holds something against (a revealed ghoul's mark, a blow), a wild ghoul in sight, a
     * monster too close (or already fought: {@code current}, till it is dead or far off), a mob after it or its partner. Never another
     * investigator, nor anyone else's companion or child unless they struck first.
     */
    static boolean wanted(CcgAgent agent, Mind mind, CcgAgent partner, LivingEntity body, long now, boolean current) {
        if (body == agent || !body.m_6084_() || body instanceof CcgAgent || body.m_5833_()) {
            return false;
        }
        if (body instanceof Player player && player.m_7500_()) {
            return false;
        }
        Long until = mind.grudges.get(body.m_20148_());
        if (until != null && until > now) {
            return true;
        }
        if (body instanceof Player || body instanceof EtoNpc || body instanceof ChildNpc || body instanceof FleshThrall) {
            return false;
        }
        double distance = Math.sqrt(body.m_20280_(agent));
        if (body instanceof GhoulNpc ghoul) {
            // 1.3.70 (the owner: "CCG agents dont attack anyone unless their eyes are revealed or kagune shown"): a wild ghoul only once it
            // shows what it is (its eyes on, its kagune out); one already fought, till it is done.
            boolean shows = ghoul.mind != null && (ghoul.mind.out || ghoul.mind.eyes);
            return current || shows && distance <= AgentRules.GHOUL_RANGE;
        }
        if (body instanceof Mob mob && (mob.m_5448_() == agent || partner != null && mob.m_5448_() == partner)) {
            return true;
        }
        return body instanceof Enemy && (current || AgentRules.tooClose(distance));
    }

    /** What its shards and sweeps may land on: what it goes for, never a bystander. */
    static boolean foe(CcgAgent agent, LivingEntity body) {
        Mind mind = agent.mind;
        return mind != null && (body.m_19879_() == mind.prey || wanted(agent, mind, null, body, agent.m_9236_().m_46467_(), true));
    }

    /**
     * A ghoul player shows what they are (their kagune out, in its sight): the pair marks them ({@link AgentRules#MARKED}) and goes for
     * them; told the moment they are first marked.
     */
    private static void reveal(CcgAgent agent, Mind mind, CcgAgent partner, ServerLevel level, long now) {
        for (ServerPlayer player : level.m_6907_()) {
            if (player.m_5833_() || player.m_7500_() || player.m_20280_(agent) > AgentRules.REVEALED_RANGE * AgentRules.REVEALED_RANGE) {
                continue;
            }
            double distance = Math.sqrt(player.m_20280_(agent));
            // 1.3.65: a final kakuja (War, Peace, the Dragon: the kagune itself in) shows what they are as much as a kagune.
            FinalSight sight = Sightings.seen(player);
            boolean shown = KaguneProfile.isActive(player) || sight.any() || GhoulServer.ghoul(player)
                || Sightings.last(player) != com.hex.kagune.control.npc.Display.NONE; // 1.3.70: their eyes on too
            boolean seen = agent.m_142582_(player);
            if (!AgentRules.revealed(KaguneProfile.isUnlocked(player), shown, distance, seen)) {
                continue;
            }
            if (sight.any() && seen && ago(now, mind.falteredAt) >= AgentRules.FALTER_AGAIN) {
                falter(agent, mind, player, sight, level, now);
            }
            Long was = mind.grudges.get(player.m_20148_());
            if (was == null || was <= now) {
                player.m_5661_(Component.m_237113_(sight.any() || !KaguneProfile.isActive(player) ? "§cThe CCG has seen what you are."
                    : "§cThe CCG has seen your kagune."), true);
            }
            mark(mind, player.m_20148_(), now + AgentRules.MARKED);
            if (partner != null && partner.mind != null) {
                mark(partner.mind, player.m_20148_(), now + AgentRules.MARKED);
            }
        }
    }

    /**
     * 1.3.65: it first sees {@code sight} on {@code player}: frozen where it stands a moment ({@link AgentRules#falter}), a word (a senior
     * steadies itself, a junior shakes), then it falls back ({@link AgentRules#FALL_BACK}) before it finds its nerve.
     */
    private static void falter(CcgAgent agent, Mind mind, ServerPlayer player, FinalSight sight, ServerLevel level, long now) {
        mind.falterFrom = player.m_19879_();
        mind.falteredAt = now;
        mind.falterUntil = now + AgentRules.falter(sight, mind.leader);
        mind.fallBackUntil = mind.falterUntil + AgentRules.FALL_BACK;
        mind.rallies = true;
        end(agent, mind);
        say(agent, level, FearLines.falter(sight, mind.leader, agent.m_217043_().m_188503_(100)));
    }

    /**
     * 1.3.65: frozen (its eyes on it, nothing in its hands moving; a blow snaps it out of that) or falling back from it (off to
     * {@link AgentRules#FALL_BACK_TO} blocks); true while either lasts. Once over, a word as it goes in again.
     */
    private static boolean falter(CcgAgent agent, Mind mind, ServerLevel level, long now) {
        if (now >= mind.fallBackUntil) {
            if (mind.rallies) {
                mind.rallies = false;
                say(agent, level, FearLines.rally(mind.leader, agent.m_217043_().m_188503_(100)));
            }
            return false;
        }
        if (!(level.m_6815_(mind.falterFrom) instanceof LivingEntity dread) || !dread.m_6084_() || dread.m_20280_(agent) > AgentRules.LEASH * AgentRules.LEASH) {
            mind.falterUntil = Long.MIN_VALUE;
            mind.fallBackUntil = Long.MIN_VALUE;
            mind.rallies = false;
            return false;
        }
        end(agent, mind);
        agent.m_21563_().m_24960_(dread, 30.0F, 30.0F);
        if (now < mind.falterUntil && (mind.hurtAt == Long.MIN_VALUE || mind.hurtAt < mind.falteredAt)) {
            face(agent, dread);
            agent.m_21573_().m_26573_();
            agent.m_20256_(new Vec3(0.0, agent.m_20184_().f_82480_, 0.0));
            return true;
        }
        if (dread.m_20280_(agent) < AgentRules.FALL_BACK_TO * AgentRules.FALL_BACK_TO) {
            if (now % 5L == 0L || agent.m_21573_().m_26571_()) {
                double[] to = com.hex.kagune.control.npc.HumanRules.fleeStep(agent.m_20185_(), agent.m_20189_(), dread.m_20185_(), dread.m_20189_(), 0.0, 6.0);
                agent.m_21573_().m_26519_(to[0], agent.m_20186_(), to[1], AgentRules.PATROL_PACE + 0.4);
            }
        } else {
            agent.m_21573_().m_26573_();
            face(agent, dread);
        }
        return true;
    }

    /** 1.3.65: a word in a bubble over its head, to every player near who sees it. */
    private static void say(CcgAgent agent, ServerLevel level, String line) {
        if (line == null || line.isEmpty()) {
            return;
        }
        for (ServerPlayer player : level.m_6907_()) {
            if (player.m_20280_(agent) <= 24.0 * 24.0 && agent.m_142582_(player)) {
                com.hex.kagune.control.network.ControlNetwork.sendTo(player, new com.hex.kagune.control.network.S2CNpcSay(agent.m_19879_(), line));
            }
        }
    }

    private static void mark(Mind mind, UUID who, long until) {
        Long was = mind.grudges.get(who);
        mind.grudges.put(who, was == null ? until : Math.max(was, until));
        if (mind.grudges.size() > 32) {
            mind.grudges.values().removeIf(at -> at < until - AgentRules.MARKED);
        }
    }

    // ---------------------------------------------------------------- at peace

    private static void patrol(CcgAgent agent, Mind mind, CcgAgent partner, ServerLevel level, long now) {
        end(agent, mind);
        cancelKick(agent, mind, now);
        cancelLightning(agent, mind, now);
        if (mind.open && ago(now, mind.foughtAt) > AgentRules.CALM && QuinqueServer.hand(agent, level, agent.m_21205_(), mind.kind, false)) {
            mind.open = false;
        }
        mind.shots = 0;
        VillagePlace.Place place = VillagePlace.of(agent);
        if (place != null) {
            village(agent, mind, place, now);
            return;
        }
        if (!mind.leader && partner != null) {
            // At its leader's side (a step to its right), catching up when left behind.
            double gap = Math.sqrt(agent.m_20280_(partner));
            if (gap > 3.5 && (now % 10L == 0L || agent.m_21573_().m_26571_())) {
                Vec3 side = UkakuFire.horizontal(partner.m_20154_());
                Vec3 to = partner.m_20182_().m_82549_(new Vec3(-side.f_82481_, 0.0, side.f_82479_).m_82490_(1.6));
                agent.m_21573_().m_26519_(to.f_82479_, partner.m_20186_(), to.f_82481_, gap > 10.0 ? 1.25 : AgentRules.PATROL_PACE + 0.1);
            } else if (gap < 2.0) {
                agent.m_21573_().m_26573_();
            }
            return;
        }
        if (partner != null && partner.m_20280_(agent) > 12.0 * 12.0) {
            agent.m_21573_().m_26573_(); // the leader waits for the other
            return;
        }
        if (now < mind.wanderAt || !agent.m_21573_().m_26571_()) {
            return;
        }
        mind.wanderAt = now + 100L + (long)(agent.m_217043_().m_188500_() * 160.0);
        double angle = agent.m_217043_().m_188500_() * Math.PI * 2.0;
        double distance = 8.0 + agent.m_217043_().m_188500_() * 10.0;
        double x = agent.m_20185_() + Math.cos(angle) * distance;
        double z = agent.m_20189_() + Math.sin(angle) * distance;
        int y = level.m_6924_(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int)Math.floor(x), (int)Math.floor(z));
        if (Math.abs(y - agent.m_20186_()) <= 6.0) {
            agent.m_21573_().m_26519_(x, y, z, AgentRules.PATROL_PACE);
        }
    }

    /**
     * 1.3.71: the village's patrol (the owner: "CCG agents often patrol the streets in an orderly maner, never going into the houses"): it
     * walks the loop of street spots round the village ({@code VillageMap.ROUTE}) in order, joining it at the nearest spot; after a
     * fight, back to it.
     */
    private static void village(CcgAgent agent, Mind mind, VillagePlace.Place place, long now) {
        int[][] route = place.route();
        if (mind.waypoint < 0 || mind.waypoint >= route.length) {
            double[] local = place.local(agent.m_20185_(), agent.m_20189_());
            mind.waypoint = com.hex.kagune.control.village.VillageRules.nearest(route, local[0], local[1]);
        }
        double[] to = place.world(route[mind.waypoint]);
        double dx = to[0] - agent.m_20185_();
        double dz = to[2] - agent.m_20189_();
        if (dx * dx + dz * dz <= com.hex.kagune.control.village.VillageRules.REACHED * com.hex.kagune.control.village.VillageRules.REACHED) {
            mind.waypoint = com.hex.kagune.control.village.VillageRules.next(mind.waypoint, route.length, Math.floorMod(agent.m_19879_(), 2) == 0);
            to = place.world(route[mind.waypoint]);
        }
        if (now % 20L == 0L || agent.m_21573_().m_26571_()) {
            agent.m_21573_().m_26519_(to[0], to[1], to[2], com.hex.kagune.control.village.VillageRules.PATROL);
        }
    }

    /** Whatever blow it was making is let go of. */
    private static void end(CcgAgent agent, Mind mind) {
        if (mind.striking) {
            mind.striking = false;
            KaguneClash.landed(agent);
        }
    }

    // ---------------------------------------------------------------- a fight

    private static void fight(CcgAgent agent, Mind mind, CcgAgent partner, LivingEntity prey, ServerLevel level, long now) {
        if (ago(now, mind.foughtAt) > AgentRules.NEW_FIGHT) {
            // A new fight: sides as they start (the leader in front), nobody round yet (the one in front waits on the other).
            mind.fightFrom = now;
            mind.swapped = false;
            mind.turnedSince = Long.MIN_VALUE;
            mind.rounding = true;
        }
        mind.foughtAt = now;
        sides(agent, mind, partner, prey, now);
        face(agent, prey);
        agent.m_21563_().m_24960_(prey, 30.0F, 30.0F);
        if (!mind.open) {
            if (QuinqueServer.hand(agent, level, agent.m_21205_(), mind.kind, true)) {
                mind.open = true;
                mind.readyAt = now + Quinque.UNFOLD;
            }
        }
        ItemStack held = agent.m_21205_();
        CompoundTag tag = held.m_41783_();
        boolean unfolding = tag != null && tag.m_128441_(Quinque.AT) && Quinque.moving(now, tag.m_128454_(Quinque.AT));
        if (unfolding) {
            agent.m_21573_().m_26573_(); // the case still opening in its hand
            return;
        }
        if (lightning(agent, mind, prey, level, now) || kick(agent, mind, prey, level, now)) {
            return;
        }
        if (mind.striking && now >= mind.landsAt) {
            land(agent, mind, level, now);
        }
        if (maneuver(agent, mind, prey, level, now)) {
            return;
        }
        if (mind.kind.ranged) {
            shoot(agent, mind, partner, prey, level, now);
        } else {
            close(agent, mind, partner, prey, level, now);
        }
    }

    private static void cancelLightning(CcgAgent agent, Mind mind, long now) {
        if (mind.aimAt != Long.MIN_VALUE) {
            mind.aimAt = Long.MIN_VALUE;
            agent.swing(-1, now);
        }
    }

    /** Hold Dawn level, point at the quarry, then fire if the lane is still clear. */
    private static boolean lightning(CcgAgent agent, Mind mind, LivingEntity prey, ServerLevel level, long now) {
        if (mind.kind != Quinque.DAWN || mind.kickAt != Long.MIN_VALUE || mind.striking) {
            return false;
        }
        double range = Math.sqrt(prey.m_20280_(agent));
        if (mind.aimAt != Long.MIN_VALUE) {
            if (prey.m_19879_() != mind.aimTarget || range > AgentRules.DAWN_SHOT_RANGE || !agent.m_142582_(prey)) {
                cancelLightning(agent, mind, now);
                return false;
            }
            agent.m_21573_().m_26573_();
            if (ago(now, mind.aimAt) >= AgentRules.DAWN_AIM_TICKS) {
                cancelLightning(agent, mind, now);
                mind.readyAt = now + 7;
                Vec3 hand = agent.m_146892_().m_82520_(0.0, -0.52, 0.0);
                Vec3 target = GhoulNpcBrain.centre(prey);
                Vec3 along = target.m_82546_(hand).m_82541_();
                Vec3 forward = UkakuFire.horizontal(along);
                hand = hand.m_82520_(-forward.f_82481_ * 0.30, 0.0, forward.f_82479_ * 0.30);
                along = target.m_82546_(hand).m_82541_();
                Vec3 tip = hand.m_82549_(along.m_82490_(1.72));
                if (level.m_45547_(new ClipContext(hand, tip, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, agent))
                    .m_6662_() == HitResult.Type.MISS) {
                    CrystalShots.lightning(agent, level, tip, along, body -> foe(agent, body));
                    agent.shot(now);
                    UkakuFire.sound(level, tip, "hexkagune:sfx.thunder", 0.65F, 1.45F);
                }
            }
            return true;
        }
        if (range >= AgentRules.DAWN_SHOT_MIN && range <= AgentRules.DAWN_SHOT_RANGE && now >= mind.lightningReadyAt
            && now >= mind.readyAt && agent.m_142582_(prey)) {
            mind.aimAt = now;
            mind.aimTarget = prey.m_19879_();
            mind.lightningReadyAt = now + AgentRules.DAWN_SHOT_COOLDOWN;
            agent.m_21573_().m_26573_();
            agent.swing(CcgAgent.AIM_DAWN, now);
            return true;
        }
        return false;
    }

    private static void cancelKick(CcgAgent agent, Mind mind, long now) {
        if (mind.kickAt != Long.MIN_VALUE) {
            mind.kickAt = Long.MIN_VALUE;
            agent.swing(-1, now);
        }
    }

    /** A close-range kick with a visible windup and a chance for its target to get clear. */
    private static boolean kick(CcgAgent agent, Mind mind, LivingEntity prey, ServerLevel level, long now) {
        if (mind.rank != AgentRules.Rank.RANK_10) {
            return false;
        }
        if (mind.kickAt != Long.MIN_VALUE) {
            if (ago(now, mind.kickAt) >= AgentRules.REAPER_KICK_DURATION) {
                cancelKick(agent, mind, now);
                return false;
            }
            agent.m_21573_().m_26573_();
            if (ago(now, mind.kickAt) >= AgentRules.REAPER_KICK_WINDUP && mind.kickTarget != 0) {
                Entity target = level.m_6815_(mind.kickTarget);
                mind.kickTarget = 0;
                if (target instanceof LivingEntity body && body.m_6084_() && agent.m_142582_(body)
                    && body.m_20280_(agent) <= Math.pow(AgentRules.REAPER_KICK_REACH + 0.3, 2)) {
                    Vec3 away = UkakuFire.horizontal(body.m_20182_().m_82546_(agent.m_20182_()));
                    if (away.m_82526_(UkakuFire.horizontal(agent.m_20154_())) >= 0.15) {
                        kickLands(agent, body, away, level, now);
                    }
                }
            }
            return true;
        }
        if (!mind.striking && now >= mind.kickReadyAt && now >= mind.readyAt && agent.m_20096_()
            && prey.m_20280_(agent) <= AgentRules.REAPER_KICK_REACH * AgentRules.REAPER_KICK_REACH
            && agent.m_142582_(prey) && agent.m_217043_().m_188500_() < 0.25) {
            mind.kickAt = now;
            mind.kickReadyAt = now + AgentRules.REAPER_KICK_COOLDOWN;
            mind.kickTarget = prey.m_19879_();
            agent.m_21573_().m_26573_();
            agent.swing(CcgAgent.KICK, now);
            UkakuFire.sound(level, agent.m_20182_(), "hexkagune:sfx.dash", 0.7F, 1.3F);
            return true;
        }
        return false;
    }

    private static void kickLands(CcgAgent agent, LivingEntity body, Vec3 away, ServerLevel level, long now) {
        Vec3 centre = GhoulNpcBrain.centre(body);
        WoundHits.expect(body, WoundBook.Blow.BLUNT, centre, away, 0.0);
        body.f_19802_ = 0;
        if (!body.m_6469_(agent.m_269291_().m_269333_(agent), AgentRules.REAPER_KICK_DAMAGE)) {
            return;
        }
        double resist = Math.max(0.0, Math.min(1.0, body.m_21133_(net.minecraft.world.entity.ai.attributes.Attributes.f_22278_)));
        double speed = PaleRules.KICK_SPEED * 0.45 * (1.0 - PaleRules.KICK_RESIST * resist);
        Vec3 launch = away.m_82490_(speed).m_82520_(0.0, PaleRules.KICK_UP, 0.0);
        body.m_20256_(launch);
        body.f_19864_ = true;
        PaleKnockdown.launched(body, now);
        PaleBlitz.fxAround(body, agent.m_19879_(), com.hex.kagune.control.network.S2CPaleFx.KICKED, centre, launch, 1.0F, 0);
        UkakuFire.sound(level, centre, "hexkagune:sfx.shockwave", 1.0F, 1.1F);
    }

    /** It turns square to {@code prey}. */
    private static void face(CcgAgent agent, Entity prey) {
        Vec3 to = prey.m_20182_().m_82546_(agent.m_20182_());
        if (to.m_165925_() < 1.0E-6) {
            return;
        }
        float yaw = (float)Math.toDegrees(Math.atan2(-to.f_82479_, to.f_82481_));
        agent.m_146922_(yaw);
        agent.f_20883_ = yaw;
        agent.f_20885_ = yaw;
    }

    /** Melee: in to its weapon's reach (from the front, or round to the back: {@link #together}), then its string of three. */
    private static void close(CcgAgent agent, Mind mind, CcgAgent partner, LivingEntity prey, ServerLevel level, long now) {
        double reach = AgentRules.reach(mind.kind);
        double distance = Math.sqrt(prey.m_20280_(agent));
        if (mind.striking) {
            agent.m_21573_().m_26573_();
            return;
        }
        Mind theirs = together(mind, partner, prey);
        Vec3 spot;
        boolean hold = false;
        if (theirs == null) {
            mind.rounding = false;
            spot = toward(prey, around(agent, prey), reach - 0.6);
        } else if (front(mind, theirs)) {
            // The front: straight on; squared up just out of its reach while the other goes round, then in.
            mind.rounding = false;
            double at = around(agent, prey);
            hold = AgentRules.holding(!theirs.rounding, ago(now, mind.fightFrom));
            spot = toward(prey, at, hold ? reach + AgentRules.HOLD_OFF : reach - 0.6);
            if (hold && distance > reach && distance <= reach + AgentRules.HOLD_OFF + 0.5) {
                agent.m_21573_().m_26573_(); // it holds there, its eyes on its prey
                return;
            }
        } else {
            // The back: round, wide and out of its reach, the way round on its own side of the one in front; in from behind.
            double at = around(agent, prey);
            double front = around(partner, prey);
            double goal = AgentRules.back(front);
            if (mind.rounding ? !AgentRules.behind(at, goal) : !AgentRules.kept(at, goal)) {
                mind.rounding = true;
                if (now % 4L == 0L || agent.m_21573_().m_26571_()) {
                    double next = AgentRules.round(at, goal, AgentRules.sideOf(at, front, mind.circle));
                    Vec3 to = toward(prey, next, Math.max(AgentRules.ROUND_RADIUS, reach + 1.5));
                    agent.m_21573_().m_26519_(to.f_82479_, prey.m_20186_(), to.f_82481_, AgentRules.FIGHT_PACE * 1.1);
                }
                return;
            }
            mind.rounding = false;
            spot = toward(prey, goal, reach - 0.6);
        }
        if (distance > reach) {
            if (now % 4L == 0L || agent.m_21573_().m_26571_()) {
                agent.m_21573_().m_26519_(spot.f_82479_, prey.m_20186_(), spot.f_82481_, hold ? AgentRules.PATROL_PACE + 0.3 : AgentRules.FIGHT_PACE);
            }
            return;
        }
        agent.m_21573_().m_26573_();
        if (now >= mind.readyAt) {
            begin(agent, mind, prey, level, now);
        }
    }

    /** Its partner's mind if the two of them fight {@code prey} together (null: it fights on its own). */
    private static Mind together(Mind mind, CcgAgent partner, LivingEntity prey) {
        if (partner == null || !partner.m_6084_() || partner.mind == null || partner.mind.prey != prey.m_19879_()) {
            return null;
        }
        return partner.mind;
    }

    /** True if it holds the front against the prey it fights with its partner ({@code theirs}); false, it goes round to the back. */
    private static boolean front(Mind mind, Mind theirs) {
        return AgentRules.front(mind.leader, mind.kind, theirs.kind, mind.leader ? mind.swapped : theirs.swapped);
    }

    /**
     * The leader keeps the pair's sides: once the prey has been turned on the one behind it (and its back on the one in front)
     * {@link AgentRules#SWAP_AFTER} ticks, with the one behind close enough to fight it, the two swap.
     */
    private static void sides(CcgAgent agent, Mind mind, CcgAgent partner, LivingEntity prey, long now) {
        Mind theirs = together(mind, partner, prey);
        if (!mind.leader || theirs == null || mind.kind.ranged || theirs.kind.ranged) {
            mind.turnedSince = Long.MIN_VALUE;
            return;
        }
        boolean mine = front(mind, theirs);
        CcgAgent ahead = mine ? agent : partner;
        CcgAgent behind = mine ? partner : agent;
        Vec3 look = prey.m_20154_();
        boolean close = behind.m_20280_(prey) <= Math.pow(AgentRules.reach((mine ? theirs : mind).kind) + 1.5, 2.0);
        if (close && AgentRules.turned(Math.atan2(look.f_82481_, look.f_82479_), around(ahead, prey), around(behind, prey))) {
            if (mind.turnedSince == Long.MIN_VALUE) {
                mind.turnedSince = now;
            } else if (ago(now, mind.turnedSince) >= AgentRules.SWAP_AFTER) {
                mind.swapped = !mind.swapped;
                mind.turnedSince = Long.MIN_VALUE;
            }
        } else {
            mind.turnedSince = Long.MIN_VALUE;
        }
    }

    /** Where {@code body} stands round {@code prey}: the angle (radians, atan2 of z and x) of its offset from it. */
    private static double around(Entity body, Entity prey) {
        Vec3 from = body.m_20182_().m_82546_(prey.m_20182_());
        return from.f_82479_ * from.f_82479_ + from.f_82481_ * from.f_82481_ < 1.0E-8 ? 0.0 : Math.atan2(from.f_82481_, from.f_82479_);
    }

    /** The spot {@code at} blocks from {@code prey} at {@code angle} round it (never right on top of it). */
    private static Vec3 toward(LivingEntity prey, double angle, double at) {
        double r = Math.max(0.8, at);
        return prey.m_20182_().m_82520_(Math.cos(angle) * r, 0.0, Math.sin(angle) * r);
    }

    /** The next of its string sets out at {@code prey}: a blow on its way (it can be met), landing a little on. */
    private static void begin(CcgAgent agent, Mind mind, LivingEntity prey, ServerLevel level, long now) {
        Quinque q = mind.kind;
        mind.step = Quinque.next(mind.step, ago(now, mind.stepAt));
        mind.stepAt = now;
        mind.striking = true;
        mind.parrying = false;
        mind.target = prey.m_19879_();
        int lands = AgentRules.lands(q);
        mind.landsAt = now + lands;
        int token = ++mind.token;
        Vec3 aim = UkakuFire.horizontal(prey.m_20182_().m_82546_(agent.m_20182_()));
        KaguneClash.swing(agent, now, lands, AgentRules.reach(q) + 0.5, aim, prey.m_19879_(), AgentRules.damage(q, mind.step) * AgentRules.power(mind.rank, mind.superior), () -> mind.token == token && mind.striking,
            () -> stopped(agent, mind, token));
        agent.swing(mind.step, now);
        Vec3 at = agent.m_20182_().m_82520_(0.0, agent.m_20206_() * 0.6, 0.0);
        boolean heavy = mind.step == Quinque.FINISHER;
        UkakuFire.sound(level, at, heavy ? "hexkagune:sfx.blade_heavy" : "hexkagune:sfx.blade_arc", 0.6F, q == Quinque.MACE ? 0.75F : 1.1F
            + 0.15F * (float)agent.m_217043_().m_188500_());
        // The spear's and the katana's blows step in; the spear's thrust drives in hard.
        // 2.0.31: Dawn's thrust drives in as hard as the spear's, its sweeps step through.
        double step = q == Quinque.SPEAR ? (heavy ? 0.75 : 0.4) : q == Quinque.DAWN ? (heavy ? 0.75 : 0.3)
            : q == Quinque.KATANA ? (heavy ? 0.55 : 0.25) : heavy ? 0.3 : 0.12;
        Vec3 velocity = agent.m_20184_();
        agent.m_20256_(new Vec3(aim.f_82479_ * step, velocity.f_82480_, aim.f_82481_ * step));
    }

    /** Its blow met another (thrown apart, KaguneClash): nothing lands; a parry that met a blow answers at once. */
    private static void stopped(CcgAgent agent, Mind mind, int token) {
        if (mind.token != token) {
            return;
        }
        long now = agent.m_9236_().m_46467_();
        boolean parried = mind.parrying;
        mind.striking = false;
        mind.parrying = false;
        mind.readyAt = now + (parried ? 3L : 10L);
        if (parried) {
            mind.step = 0; // the riposte: the second of its string next
            mind.stepAt = now;
        }
    }

    /** The blow lands (or the parry ends): unless it met a blow of theirs coming back, it cuts whoever is still in reach in front. */
    private static void land(CcgAgent agent, Mind mind, ServerLevel level, long now) {
        Quinque q = mind.kind;
        boolean parry = mind.parrying;
        mind.striking = false;
        mind.parrying = false;
        int step = mind.step;
        mind.readyAt = now + Math.max(0, q.swing - AgentRules.lands(q)) + Math.round(AgentRules.between(q) * mind.rank.pace) // 2.0.17: by its rank
            + (step == Quinque.FINISHER ? AgentRules.AFTER_STRING : 0);
        Entity found = level.m_6815_(mind.target);
        if (parry || !(found instanceof LivingEntity prey) || !prey.m_6084_()) {
            KaguneClash.landed(agent);
            if (parry) {
                mind.readyAt = now + 2L;
            }
            return;
        }
        if (KaguneClash.meets(agent, prey, now)) {
            mind.readyAt = now + 10L; // met: both thrown apart, nothing hurt
            return;
        }
        KaguneClash.landed(agent);
        Vec3 to = prey.m_20182_().m_82546_(agent.m_20182_());
        double distance = Math.sqrt(to.m_165925_());
        Vec3 look = UkakuFire.horizontal(agent.m_20154_());
        Vec3 flat = UkakuFire.horizontal(to);
        if (distance > AgentRules.reach(q) + prey.m_20205_() * 0.5 + 0.6 || Math.abs(to.f_82480_) > 2.5 || flat.m_82526_(look) < 0.15) {
            return; // it slipped the blow
        }
        float dealt = AgentRules.damage(q, step) * AgentRules.power(mind.rank, mind.superior); // 2.0.17: by its rank
        Vec3 centre = GhoulNpcBrain.centre(prey);
        WoundHits.expect(prey, q == Quinque.SPEAR ? WoundBook.Blow.STAB : q == Quinque.MACE ? WoundBook.Blow.CRUSH : q == Quinque.LAUNCHER ? WoundBook.Blow.BLUNT
            : WoundBook.Blow.SLASH, centre.m_82546_(flat.m_82490_(0.3)), flat, 0.0);
        prey.f_19802_ = 0;
        boolean hit = prey.m_6469_(agent.m_269291_().m_269333_(agent), dealt);
        if (hit && step == Quinque.FINISHER) {
            QuinqueServer.finisher(agent, prey, q, dealt, level);
            mind.clear = agent.m_217043_().m_188500_() < 0.4; // and now and then it flips clear after it
        } else if (q.arc(step) > 0.0) {
            // 2.0.31: Dawn's wide sweep takes its other foes in front too (never a bystander).
            QuinqueServer.arc(agent, prey, q, step, dealt, level, body -> foe(agent, body));
        }
    }

    // ---------------------------------------------------------------- its feet

    /** In a flip or a dash: the push kept up (the ground's grip takes most of the first tick's). True while it is (nothing else). */
    private static boolean feet(CcgAgent agent, Mind mind, long now) {
        if (mind.flipAt != Long.MIN_VALUE) {
            long age = now - mind.flipAt;
            if (age == 1L && mind.push != null) {
                agent.m_20256_(mind.push);
                agent.f_19864_ = true;
            }
            if (age < NpcRules.FLIP_BUSY || age < NpcRules.FLIP_POSED && !agent.m_20096_()) {
                agent.m_21573_().m_26573_();
                return true;
            }
        }
        double speed = AgentRules.dashSpeed(mind.rank);
        if (mind.dashAt != Long.MIN_VALUE && now - mind.dashAt < AgentRules.DASH_TICKS) {
            Vec3 velocity = agent.m_20184_();
            agent.m_20256_(new Vec3(mind.dashWay.f_82479_ * speed, velocity.f_82480_, mind.dashWay.f_82481_ * speed));
            agent.f_19864_ = true;
            agent.m_21573_().m_26573_();
            return true;
        }
        // 2.0.18: a zigzag: the next dash at once, the other way (leaning in on prey out of reach); a wall or a drop that way ends it.
        if (mind.zigzag > 0 && mind.dashAt != Long.MIN_VALUE && now - mind.dashAt == AgentRules.DASH_TICKS && agent.m_9236_() instanceof ServerLevel level) {
            mind.zigzag--;
            double[] next = AgentRules.zig(mind.dashWay.f_82479_, mind.dashWay.f_82481_, mind.zigAxis.f_82479_, mind.zigAxis.f_82481_, mind.zigIn);
            Vec3 way = new Vec3(next[0], 0.0, next[1]);
            if (clear(agent, level, way)) {
                boolean left = way.f_82479_ * -mind.zigAxis.f_82481_ + way.f_82481_ * mind.zigAxis.f_82479_ > 0.0;
                int left0 = mind.zigzag;
                dash(agent, mind, way, left ? 2 : 3, level, now);
                mind.zigzag = left0; // (dash() starts a new zigzag: this one goes on)
                return true;
            }
            mind.zigzag = 0;
        }
        return false;
    }

    /**
     * Before it swings or shoots: a blow coming at it met with its own (the kagune slapped aside) or flipped and dashed clear of; a front
     * flip in on prey too far off, a back flip clear of prey too close or a finished string, a quick side step between blows. True if it
     * moved its feet (nothing else this tick).
     */
    private static boolean maneuver(CcgAgent agent, Mind mind, LivingEntity prey, ServerLevel level, long now) {
        Quinque q = mind.kind;
        double distance = Math.sqrt(prey.m_20280_(agent));
        boolean flips = agent.m_20096_() && now >= mind.flipReadyAt;
        boolean dashes = agent.m_20096_() && now >= mind.dashReadyAt;
        KaguneClash.Threat threat = KaguneClash.threat(agent, now);
        if (threat != null && threat.began() != mind.answered && !mind.striking) {
            mind.answered = threat.began();
            double roll = agent.m_217043_().m_188500_();
            LivingEntity who = threat.who();
            double gap = Math.sqrt(who.m_20280_(agent));
            if (roll < AgentRules.parry(q) && gap <= AgentRules.reach(q) + 2.5) {
                parry(agent, mind, threat, level, now);
                return false;
            }
            Vec3 away = UkakuFire.horizontal(agent.m_20182_().m_82546_(who.m_20182_()));
            Vec3 side = new Vec3(-away.f_82481_, 0.0, away.f_82479_);
            if (roll < AgentRules.parry(q) + AgentRules.DODGE * (1.0 - AgentRules.parry(q))) {
                double how = agent.m_217043_().m_188500_();
                boolean left = agent.m_217043_().m_188500_() < 0.5;
                if (flips && how < 0.45) {
                    flip(agent, mind, left ? PaleRules.Flip.LEFT : PaleRules.Flip.RIGHT, left ? side : side.m_82490_(-1.0), AgentRules.FLIP_SPEED * 0.85,
                        AgentRules.FLIP_UP * 0.85, who, level, now);
                    return true;
                }
                if (flips && how < 0.7) {
                    flip(agent, mind, PaleRules.Flip.BACK, away, AgentRules.FLIP_BACK_SPEED, AgentRules.FLIP_BACK_UP, who, level, now);
                    return true;
                }
                if (dashes) {
                    dash(agent, mind, left ? side : side.m_82490_(-1.0), left ? 2 : 3, level, now);
                    return true;
                }
            }
        }
        if (mind.striking) {
            return false;
        }
        Vec3 toward = UkakuFire.horizontal(prey.m_20182_().m_82546_(agent.m_20182_()));
        double reach = q.ranged ? AgentRules.SHOOT_NEAREST : AgentRules.reach(q);
        if (flips && mind.clear) {
            mind.clear = false;
            flip(agent, mind, PaleRules.Flip.BACK, toward.m_82490_(-1.0), AgentRules.FLIP_BACK_SPEED, AgentRules.FLIP_BACK_UP, prey, level, now);
            return true;
        }
        double roll = agent.m_217043_().m_188500_();
        if (flips && !mind.rounding && distance > reach + 3.0 && distance < 14.0 && roll < AgentRules.FLIP_CHANCE && agent.m_142582_(prey)) {
            flip(agent, mind, PaleRules.Flip.FORWARD, toward, NpcRules.flipSpeed(distance - reach), AgentRules.FLIP_UP, prey, level, now);
            return true;
        }
        boolean losing = agent.m_21223_() < agent.m_21233_() * 0.35;
        if (flips && (q.ranged ? distance < AgentRules.SHOOT_NEAREST - 2.5 : losing && distance < 3.0) && roll < AgentRules.FLIP_CHANCE * 2.0) {
            flip(agent, mind, PaleRules.Flip.BACK, toward.m_82490_(-1.0), AgentRules.FLIP_BACK_SPEED, AgentRules.FLIP_BACK_UP, prey, level, now);
            return true;
        }
        if (dashes && !q.ranged && distance <= reach + 0.5 && now < mind.readyAt - 4L && roll < 0.06 * AgentRules.dashOften(mind.rank)) {
            Vec3 side = new Vec3(-toward.f_82481_, 0.0, toward.f_82479_);
            boolean left = agent.m_217043_().m_188500_() < 0.5;
            dash(agent, mind, left ? side : side.m_82490_(-1.0), left ? 2 : 3, level, now);
            return true;
        }
        // 1.3.33: quick on its feet. In close it slips left and right between blows (never into a wall or off a ledge it can see);
        // a shooter strafes the same way between bursts; prey just out of reach it closes on in one push.
        double chance = agent.m_217043_().m_188500_();
        boolean between = q.ranged ? now < mind.nextShot || now < mind.burstReadyAt : now < mind.readyAt - 3L;
        boolean near = q.ranged ? distance <= AgentRules.SHOOT_FARTHEST : distance <= reach + 3.0;
        if (dashes && !mind.rounding && between && near && chance < AgentRules.SIDE_DASH * AgentRules.dashOften(mind.rank) && agent.m_142582_(prey)) {
            Vec3 side = new Vec3(-toward.f_82481_, 0.0, toward.f_82479_);
            boolean left = agent.m_217043_().m_188500_() < 0.5;
            if (!clear(agent, level, left ? side : side.m_82490_(-1.0))) {
                left = !left;
            }
            Vec3 way = left ? side : side.m_82490_(-1.0);
            if (clear(agent, level, way)) {
                dash(agent, mind, way, left ? 2 : 3, level, now);
                return true;
            }
        }
        if (dashes && !q.ranged && !mind.rounding && distance > reach + 0.8 && distance < reach + 4.5 && now >= mind.readyAt - 2L
            && chance > 1.0 - AgentRules.DASH_IN && agent.m_142582_(prey) && clear(agent, level, toward)) {
            dash(agent, mind, toward, 0, level, now);
            return true;
        }
        return false;
    }

    /** It meets the blow coming at it with its own: a quick cut aimed at the striker that lands as theirs does (KaguneClash: the two meet). */
    private static void parry(CcgAgent agent, Mind mind, KaguneClash.Threat threat, ServerLevel level, long now) {
        LivingEntity who = threat.who();
        mind.striking = true;
        mind.parrying = true;
        mind.target = who.m_19879_();
        int lands = (int)Math.max(1L, Math.min(20L, threat.until() - now + 1L));
        mind.landsAt = now + lands;
        int token = ++mind.token;
        face(agent, who);
        Vec3 aim = UkakuFire.horizontal(who.m_20182_().m_82546_(agent.m_20182_()));
        KaguneClash.swing(agent, now, lands, AgentRules.reach(mind.kind) + 1.5, aim, who.m_19879_(), AgentRules.damage(mind.kind, 0) * AgentRules.power(mind.rank, mind.superior), () -> mind.token == token && mind.striking,
            () -> stopped(agent, mind, token));
        agent.swing(CcgAgent.PARRY, now);
        UkakuFire.sound(level, agent.m_20182_().m_82520_(0.0, agent.m_20206_() * 0.6, 0.0), "hexkagune:sfx.blade_arc", 0.6F, 1.4F);
    }

    /** A flip ({@code kind}) along {@code way} (level, unit): whatever it was making stops, and every client sees it go over. */
    private static void flip(CcgAgent agent, Mind mind, PaleRules.Flip kind, Vec3 way, double speed, double up, LivingEntity facing, ServerLevel level,
                             long now) {
        end(agent, mind);
        agent.m_21573_().m_26573_();
        Vec3 push = new Vec3(way.f_82479_ * speed, up, way.f_82481_ * speed);
        agent.m_20256_(push);
        agent.f_19864_ = true;
        agent.f_19789_ = 0.0F;
        mind.push = push;
        mind.flipAt = now;
        mind.flipReadyAt = now + (mind.rank == AgentRules.Rank.RANK_10 ? AgentRules.REAPER_FLIP_EVERY : AgentRules.FLIP_EVERY);
        mind.dashReadyAt = Math.max(mind.dashReadyAt, now + NpcRules.FLIP_BUSY);
        if (facing != null) {
            face(agent, facing);
        }
        agent.flip(kind.ordinal(), now);
        UkakuFire.sound(level, agent.m_20182_(), "entity.player.attack.sweep", 0.4F, 0.75F);
    }

    /**
     * 1.3.33: room for a dash along {@code way} (level, unit): the blocks at its feet and head two and three blocks along are open, and
     * there is ground under where it lands.
     */
    private static boolean clear(CcgAgent agent, ServerLevel level, Vec3 way) {
        for (double along : new double[] {1.5, 3.0}) {
            double x = agent.m_20185_() + way.f_82479_ * along;
            double y = agent.m_20186_();
            double z = agent.m_20189_() + way.f_82481_ * along;
            if (!open(level, x, y + 0.2, z) || !open(level, x, y + 1.2, z)) {
                return false;
            }
            if (along > 2.0 && open(level, x, y - 0.8, z) && open(level, x, y - 1.8, z)) {
                return false; // a drop of two or more
            }
        }
        return true;
    }

    /** True if nothing solid is in the block at (x, y, z). */
    private static boolean open(ServerLevel level, double x, double y, double z) {
        net.minecraft.core.BlockPos at = net.minecraft.core.BlockPos.m_274561_(x, y, z);
        return level.m_8055_(at).m_60812_(level, at).m_83281_();
    }

    /** A dash along {@code way} (level, unit; {@code which} 0 forward, 1 back, 2 left, 3 right): low and fast, a few ticks. */
    private static void dash(CcgAgent agent, Mind mind, Vec3 way, int which, ServerLevel level, long now) {
        end(agent, mind);
        mind.dashAt = now;
        mind.dashWay = way;
        mind.dashReadyAt = now + AgentRules.dashEvery(mind.rank); // 2.0.18: sooner by its rank
        // 2.0.18: a zigzag of AgentRules.dashes(rank) about the line to its prey (or its own way, none known), in on them if out of reach.
        int preyId = mind.prey != 0 ? mind.prey : mind.target;
        LivingEntity prey = preyId == 0 ? null : level.m_6815_(preyId) instanceof LivingEntity body ? body : null;
        Vec3 axis = prey == null ? way : UkakuFire.horizontal(prey.m_20182_().m_82546_(agent.m_20182_()));
        mind.zigAxis = axis.m_82556_() < 1.0E-6 ? way : axis.m_82541_();
        mind.zigIn = prey != null && !mind.kind.ranged && Math.sqrt(prey.m_20280_(agent)) > AgentRules.reach(mind.kind) + 0.5;
        mind.zigzag = AgentRules.dashes(mind.rank) - 1;
        agent.dash(which, now);
        double speed = AgentRules.dashSpeed(mind.rank);
        Vec3 velocity = agent.m_20184_();
        agent.m_20256_(new Vec3(way.f_82479_ * speed, Math.max(velocity.f_82480_, 0.05), way.f_82481_ * speed));
        agent.f_19864_ = true;
        UkakuFire.sound(level, agent.m_20182_(), "entity.player.attack.sweep", 0.3F, 1.3F);
    }

    // ---------------------------------------------------------------- the launcher

    /** It keeps its distance (backing off, closing in, circling) and fires bursts of shards at {@code prey}; too close, it clubs them. */
    private static void shoot(CcgAgent agent, Mind mind, CcgAgent partner, LivingEntity prey, ServerLevel level, long now) {
        mind.rounding = false; // (the launcher always holds the front)
        double distance = Math.sqrt(prey.m_20280_(agent));
        boolean seen = agent.m_142582_(prey);
        if (mind.striking) {
            agent.m_21573_().m_26573_();
            return;
        }
        if (distance <= AgentRules.reach(Quinque.LAUNCHER) && now >= mind.readyAt) {
            agent.m_21573_().m_26573_();
            begin(agent, mind, prey, level, now);
            return;
        }
        if (now % 5L == 0L || agent.m_21573_().m_26571_()) {
            Vec3 from = UkakuFire.horizontal(agent.m_20182_().m_82546_(prey.m_20182_()));
            if (from.m_165925_() < 1.0E-6) {
                from = new Vec3(1.0, 0.0, 0.0);
            }
            double want = (AgentRules.SHOOT_NEAREST + AgentRules.SHOOT_FARTHEST) / 2.0;
            if (distance < AgentRules.SHOOT_NEAREST || distance > AgentRules.SHOOT_FARTHEST || !seen) {
                Vec3 to = prey.m_20182_().m_82549_(from.m_82490_(seen ? want : 4.0));
                agent.m_21573_().m_26519_(to.f_82479_, prey.m_20186_(), to.f_82481_, AgentRules.FIGHT_PACE);
            } else {
                if (now >= mind.circleAt) {
                    mind.circleAt = now + 30L + (long)(agent.m_217043_().m_188500_() * 40.0);
                    mind.circle = agent.m_217043_().m_188500_() < 0.5 ? -1.0 : 1.0;
                }
                double angle = Math.atan2(from.f_82481_, from.f_82479_) + mind.circle * 0.45;
                Vec3 to = prey.m_20182_().m_82520_(Math.cos(angle) * distance, 0.0, Math.sin(angle) * distance);
                agent.m_21573_().m_26519_(to.f_82479_, prey.m_20186_(), to.f_82481_, 0.9);
            }
        }
        if (!seen || distance > AgentRules.SHOOT_FARTHEST + 8.0) {
            return;
        }
        if (now < mind.burstReadyAt || now < mind.nextShot) {
            return;
        }
        fire(agent, prey, level, now);
        mind.shots++;
        mind.nextShot = now + AgentRules.SHOT_GAP;
        if (mind.shots >= AgentRules.BURST) {
            mind.shots = 0;
            mind.burstReadyAt = now + AgentRules.BURST_GAP + (long)(agent.m_217043_().m_188500_() * 20.0);
        }
    }

    /** A shard from its launcher's muzzle at where {@code prey} will be, a little spread. */
    private static void fire(CcgAgent agent, LivingEntity prey, ServerLevel level, long now) {
        Vec3 look = UkakuFire.horizontal(prey.m_20182_().m_82546_(agent.m_20182_()));
        Vec3 right = new Vec3(-look.f_82481_, 0.0, look.f_82479_);
        Vec3 from = agent.m_146892_().m_82549_(look.m_82490_(0.9)).m_82549_(right.m_82490_(0.22)).m_82520_(0.0, -0.4, 0.0);
        Vec3 target = GhoulNpcBrain.centre(prey);
        double flight = target.m_82554_(from) / Quinque.SHARD_SPEED;
        Vec3 lead = prey.m_20184_();
        target = target.m_82520_(lead.f_82479_ * flight, 0.0, lead.f_82481_ * flight);
        Vec3 way = target.m_82546_(from);
        if (way.m_82556_() < 1.0E-6) {
            return;
        }
        double spread = Math.toRadians(2.5);
        way = way.m_82541_().m_82520_((agent.m_217043_().m_188500_() - 0.5) * spread, (agent.m_217043_().m_188500_() - 0.5) * spread,
            (agent.m_217043_().m_188500_() - 0.5) * spread).m_82541_();
        CrystalShots.fire(agent, level, from, way, Quinque.SHARD_SPEED, Quinque.SHARD_RANGE, AgentRules.SHARD_DAMAGE * (agent.mind == null ? 1.0F : AgentRules.power(agent.mind.rank, agent.mind.superior)), body -> foe(agent, body));
        agent.shot(now);
        UkakuFire.sound(level, from, "block.amethyst_block.hit", 0.55F, 1.5F + 0.15F * (float)agent.m_217043_().m_188500_());
    }

    // ---------------------------------------------------------------- what happens to it

    /** The Reaper leaves human meat. Regular investigators rarely leave their quinque case. */
    public static void drop(CcgAgent agent, int looting, boolean byPlayer) {
        Mind mind = agent.mind;
        if (reaper(agent)) {
            HumanBrain.drop(agent, looting);
            return;
        }
        if (!byPlayer || mind == null || agent.m_217043_().m_188500_() >= AgentRules.dropChance(looting)) {
            return;
        }
        agent.m_19983_(new ItemStack(ControlItems.caseOf(mind.kind), 1));
    }

    private static boolean reaper(CcgAgent agent) {
        CompoundTag saved = agent.getPersistentData();
        return saved.m_128451_(RANK_LEVEL) == AgentRules.Rank.RANK_10.level()
            && !saved.m_128403_(PARTNER) && !saved.m_128441_(VillagePlace.KEY);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void died(LivingDeathEvent event) {
        if (event.getEntity() instanceof CcgAgent agent && agent.m_9236_() instanceof ServerLevel level
            && reaper(agent)) {
            // Death events run before vanilla rolls equipment drops.
            removeDawn(agent, InteractionHand.MAIN_HAND, agent.m_21205_());
            removeDawn(agent, InteractionHand.OFF_HAND, agent.m_21206_());
            UniqueNpcs.died(level, REAPER, agent.m_20148_());
        }
    }

    private static void removeDawn(CcgAgent agent, InteractionHand hand, ItemStack stack) {
        if (ControlItems.quinqueWeapon(stack) == Quinque.DAWN || ControlItems.quinqueCase(stack) == Quinque.DAWN) {
            agent.m_21008_(hand, ItemStack.f_41583_);
        }
    }

    /** It was struck: it and its partner go for whoever did it (and a ghoul who did is marked as one). */
    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof CcgAgent agent) || !(agent.m_9236_() instanceof ServerLevel level) || agent.mind == null) {
            return;
        }
        long now = level.m_46467_();
        agent.mind.hurtAt = now;
        if (agent.mind.rank == AgentRules.Rank.RANK_10 && !com.hex.kagune.control.GhoulTank.bypasses(event.getSource().m_19385_())) {
            event.setAmount(event.getAmount() * AgentRules.REAPER_TAKEN);
        }
        if (!(event.getSource().m_7639_() instanceof LivingEntity attacker) || attacker instanceof CcgAgent || attacker == agent) {
            return;
        }
        if (attacker instanceof Player player && player.m_7500_()) {
            return; // (a creative player testing them)
        }
        long until = now + AgentRules.GRUDGE;
        if (attacker instanceof ServerPlayer player && KaguneProfile.isUnlocked(player) && KaguneProfile.isActive(player)) {
            until = now + AgentRules.MARKED;
        }
        mark(agent.mind, attacker.m_20148_(), until);
        agent.mind.preySought = Long.MIN_VALUE;
        if (agent.mind.partner != null) {
            for (CcgAgent other : level.m_6443_(CcgAgent.class, agent.m_20191_().m_82400_(48.0), a -> a != agent && a.mind != null)) {
                if (agent.mind.partner.equals(other.m_20148_())) {
                    mark(other.mind, attacker.m_20148_(), until);
                    other.mind.preySought = Long.MIN_VALUE;
                }
            }
        }
    }

    /** Investigators never hurt each other (a stray shard, a finisher's sweep). */
    @SubscribeEvent
    public static void attacked(LivingAttackEvent event) {
        if (event.getEntity() instanceof CcgAgent && event.getSource().m_7639_() instanceof CcgAgent) {
            event.setCanceled(true);
        }
    }
}
