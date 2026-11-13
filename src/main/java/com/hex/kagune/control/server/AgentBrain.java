package com.hex.kagune.control.server;

import com.hex.kagune.control.ControlItems;
import com.hex.kagune.control.npc.AgentRules;
import com.hex.kagune.control.network.S2CUkakuFx;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.npc.ReaperTendril;
import com.hex.kagune.control.npc.CcgAgent;
import com.hex.kagune.control.npc.ChildNpc;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.FearLines;
import com.hex.kagune.control.npc.FinalSight;
import com.hex.kagune.control.npc.FleshThrall;
import com.hex.kagune.control.npc.GhoulNpc;
import com.hex.kagune.control.npc.NpcRules;
import com.hex.kagune.control.pale.PaleRules;
import com.hex.kagune.control.quinque.Arm;
import com.hex.kagune.control.npc.ArmRules;
import com.hex.kagune.control.quinque.Quinque;
import com.hex.kagune.control.npc.ReaperTactics;
import com.hex.kagune.control.npc.ReaperBladeRules;
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
import net.minecraft.world.phys.AABB;
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
        /** 4.1.4: set by {@link AgentDesk} each tick it keeps its seat. */
        boolean seatKept;
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
        /** 2.0.48: the shot being aimed is the five-ball volley (DawnVolley), not a single ball. */
        boolean volley;
        /** 2.0.49: how long the shot being aimed is held (a quick one at a quarry in the air, or from the top of a soar). */
        int aimTicks = AgentRules.DAWN_AIM_TICKS;
        /** 2.0.49, the White Reaper's tactics (ReaperTactics): a soar (when it began, its way back, at whom, whether it fired), its cooldown, his string just landed. */
        long soarAt = Long.MIN_VALUE;
        Vec3 soarWay = Vec3.f_82478_;
        int soarTarget;
        boolean soarFired;
        long soarReadyAt = Long.MIN_VALUE;
        boolean strung;
        /**
         * 2.0.33: the ground tendril (npc.ReaperTendril): when it set out, at whom, where it rises (null until the lance is in the ground),
         * whether it has risen yet, and when it may again.
         */
        long groundAt = Long.MIN_VALUE;
        /** 4.1.25: the White Reaper's hand-to-hand: the strike under way (its ordinal; -1 none), since when, at whom, its string and place. */
        int brawlStrike = -1;
        long brawlAt = Long.MIN_VALUE;
        int brawlTarget;
        com.hex.kagune.control.pale.PaleStrike.Style brawlStyle = com.hex.kagune.control.pale.PaleStrike.Style.KICKS;
        int brawlIndex;
        long brawlReadyAt = Long.MIN_VALUE;
        int groundTarget;
        Vec3 groundSpot;
        boolean erupted;
        long groundReadyAt = Long.MIN_VALUE;
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
        /** 2.1.9: running from Peace or War (who, until when), or trembling before it (who, until when). */
        int routedFrom;
        long routedUntil = Long.MIN_VALUE;
        int afraidOf;
        long afraidUntil = Long.MIN_VALUE;
        long fallBackUntil = Long.MIN_VALUE;
        boolean rallies;
        // 3.0.0: a higher rank's kagune-made weapon (AgentArms; null: a regular quinque), a masterwork, out of its case and since when.
        Arm arm;
        boolean master;
        boolean armOut;
        long armAt = Long.MIN_VALUE;
        // One of its moves under way (null: none): when it began, at whom, its next blow, those its charge went through, its way.
        Arm.Ability move;
        long moveAt = Long.MIN_VALUE;
        int moveTarget;
        int moveHit;
        boolean bound;
        Vec3 moveWay = Vec3.f_82478_;
        final java.util.Set<Integer> moveStruck = new java.util.HashSet<>();
        /** When each of its two moves is ready again. */
        final long[] moveReady = {Long.MIN_VALUE, Long.MIN_VALUE};
        /** Wounds its Cleave left bleeding: {entity id, until, damage (float bits)}. */
        final java.util.List<long[]> bleeds = new java.util.ArrayList<>();
        /**
         * 4.1.28: the White Reaper's jacket: off (his second phase, kept with him under {@link #BARE}); when he began taking it off (none:
         * not now); due to come off (at half health, as soon as he stands on the ground).
         */
        boolean bare;
        long shedAt = Long.MIN_VALUE;
        boolean shedPending;
        /** 4.1.37: since when his change has waited for him to land; where he stands while he changes (or ends), pinned there. */
        long shedPendingAt = Long.MIN_VALUE;
        Vec3 pinnedAt;
        /** 4.1.38: when he began putting his jacket back on (none: not now). */
        long dressAt = Long.MIN_VALUE;
        /** 4.1.29: his blade: the blow of his string under way (when it began, which, whether it has landed). */
        long bladeAt = Long.MIN_VALUE;
        int bladeStep;
        boolean bladeLanded;
        /** 4.1.30: his rush in (until when, where it stops, when another may start) and his sidestep (until when, which way, when again). */
        long rushUntil = Long.MIN_VALUE;
        Vec3 rushTo = Vec3.f_82478_;
        long rushReadyAt = Long.MIN_VALUE;
        long dodgeUntil = Long.MIN_VALUE;
        Vec3 dodgeWay = Vec3.f_82478_;
        long dodgeReadyAt = Long.MIN_VALUE;
        /** 4.1.31: his end (when it began, the blow that brought it, whether the blade has shattered; the last blow let through). */
        long endAt = Long.MIN_VALUE;
        net.minecraft.world.damagesource.DamageSource endSource;
        boolean shattered;
        boolean finishing;
    }

    /** Its partner's id in its saved data (who it is: {@link CcgAgent#KEY}, packed). */
    static final String PARTNER = CcgAgent.KEY + "Partner";

    /** 1.3.70: its rank in its saved data. */
    static final String RANK = CcgAgent.KEY + "Rank";
    /** 2.0.17: its rank's number (0 to 10). */
    static final String RANK_LEVEL = CcgAgent.KEY + "RankLevel";
    /** 4.1.28: the White Reaper's jacket is off (his second phase). */
    static final String BARE = CcgAgent.KEY + "Bare";

    private AgentBrain() {
    }

    /** 2.1.5: {@code agent}'s rank (0 to 10): its mind's, else its saved one. */
    static int rankLevel(CcgAgent agent) {
        if (agent.mind != null && agent.mind.rank != null) {
            return agent.mind.rank.level();
        }
        return agent.getPersistentData().m_128451_(RANK_LEVEL);
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

    static void save(CcgAgent agent, int look, Quinque kind, boolean leader, UUID partner) {
        CompoundTag saved = agent.getPersistentData();
        saved.m_128356_(CcgAgent.KEY, (long)(look & 0xFF) | (long)kind.ordinal() << 8 | (leader ? 1L : 0L) << 16);
        if (partner != null) {
            saved.m_128362_(PARTNER, partner);
        }
    }

    /** 3.0.0: made by {@link #summonArmed} (its rank set already): at full health when it wakes. */
    static final String SUMMONED = CcgAgent.KEY + "Summoned";

    /**
     * 3.0.0: {@code /kagune investigator <weapon> [rank]} puts an investigator of {@code rank} (4 to 9; 8 and 9 a masterwork) with {@code arm}
     * five blocks ahead of {@code player}, alone, to see the weapon and its moves. What the operator is told back.
     */
    public static String summonArmed(ServerPlayer player, Arm arm, int rank) {
        ServerLevel level = player.m_284548_();
        Vec3 ahead = UkakuFire.horizontal(player.m_20154_()).m_82490_(5.0);
        int x = (int)Math.floor(player.m_20185_() + ahead.f_82479_);
        int z = (int)Math.floor(player.m_20189_() + ahead.f_82481_);
        CcgAgent agent = AgentSpawner.placeAt(level, x, (int)Math.floor(player.m_20186_() + 0.01), z);
        if (agent == null) {
            agent = AgentSpawner.place(level, x + 0.5, z + 0.5);
        }
        if (agent == null) {
            return "No room ahead for an investigator.";
        }
        int level4 = Math.max(ArmRules.FROM, Math.min(AgentRules.Rank.RANK_9.level(), rank));
        AgentRules.Rank given = AgentRules.Rank.byOrdinal(level4);
        int look = AgentRules.regularLook(agent.m_217043_().m_188500_());
        save(agent, look, arm.carried, true, null);
        CompoundTag data = agent.getPersistentData();
        data.m_128405_(RANK_LEVEL, level4);
        data.m_128405_(AgentArms.KEY, ArmRules.pack(arm, ArmRules.masterwork(given), false));
        data.m_128405_(SUMMONED, 1);
        agent.dress(look, arm.carried);
        return "A Rank " + level4 + " investigator with " + (ArmRules.masterwork(given) ? "a masterwork " : "a ") + arm.title + ".";
    }

    private static final String REAPER = "ccg:reaper";
    /** 4.1.24: saved by his mansion's template (tools/companion_structures.py): the White Reaper who lives there. */
    static final String MANSION = CcgAgent.KEY + "Mansion";
    /** 4.1.24: where in its garden he first woke: his home. */
    static final String HOME_X = CcgAgent.KEY + "HomeX";
    static final String HOME_Y = CcgAgent.KEY + "HomeY";
    static final String HOME_Z = CcgAgent.KEY + "HomeZ";
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
        boolean fresh = !data.m_128441_(RANK_LEVEL) && !data.m_128441_(RANK) || data.m_128441_(SUMMONED);
        data.m_128473_(SUMMONED);
        mind.rank = data.m_128441_(RANK_LEVEL) ? AgentRules.Rank.byOrdinal(data.m_128451_(RANK_LEVEL))
            : data.m_128441_(RANK) ? AgentRules.Rank.fromOld(data.m_128451_(RANK))
            : AgentRules.Rank.of(mind.leader, agent.m_217043_().m_188500_(), agent.m_217043_().m_188500_());
        boolean reaper = mind.rank == AgentRules.Rank.RANK_10 && !fresh && mind.partner == null
            && !saved.m_128441_(VillagePlace.KEY) && claimReaper(agent, level);
        if (reaper) {
            // 4.1.24: the mansion's (its template saves him so): his home is where he first wakes, in its garden.
            if (saved.m_128441_(MANSION) && !saved.m_128441_(HOME_X)) {
                saved.m_128405_(HOME_X, (int)Math.floor(agent.m_20185_()));
                saved.m_128405_(HOME_Y, (int)Math.floor(agent.m_20186_() + 0.01));
                saved.m_128405_(HOME_Z, (int)Math.floor(agent.m_20189_()));
            }
            // 2.0.28: his own skin, a Reaper saved before included; 4.1.28: the jacket off once he has shed it.
            mind.bare = saved.m_128441_(BARE);
            mind.look = mind.bare ? AgentRules.REAPER_BARE_LOOK : AgentRules.REAPER_LOOK;
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
        // 3.0.0: a higher rank's own kagune-made weapon, rolled once by its rank (kept in its saved data); its case carried at peace.
        if (!reaper) {
            AgentArms.wake(agent, mind);
        }
        agent.m_21051_(net.minecraft.world.entity.ai.attributes.Attributes.f_22276_).m_22100_(AgentRules.health(mind.rank, mind.superior));
        agent.m_21051_(net.minecraft.world.entity.ai.attributes.Attributes.f_22279_).m_22100_(speed(mind));
        // 4.0.0 (the owner: "All naturally spawning CCG investigators will now wear ... the complete Formal Outfit ... The White Reaper is the
        // only exception"): the whole Formal Outfit on every one but him; its protection taken off its rank's own, so it fights as before.
        if (!reaper) {
            OutfitServer.dress(agent, com.hex.kagune.control.outfit.Outfits.FORMAL_SET);
        } else {
            OutfitServer.undress(agent); // he keeps his own look, always
        }
        agent.m_21051_(net.minecraft.world.entity.ai.attributes.Attributes.f_22284_).m_22100_(OutfitServer.armour(agent, mind.rank.armor));
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
        // 4.1.4: one sat at its desk (AgentDesk) stays sat only while that is what it does: anything else gets it up.
        mind.seatKept = false;
        act(agent, mind, level, now);
        if (!mind.seatKept && agent.seated()) {
            AgentDesk.stand(agent);
        }
    }

    /** Its speed by its rank (4.1.28: the White Reaper's faster with his jacket off). */
    static double speed(Mind mind) {
        return mind.rank.speed * (mind.bare ? AgentRules.REAPER_BARE_SPEED : 1.0);
    }

    private static void act(CcgAgent agent, Mind mind, ServerLevel level, long now) {
        if (ending(agent, mind, level, now)) {
            return; // 4.1.31: the White Reaper's end: nothing else ever again
        }
        if (shed(agent, mind, level, now)) {
            return; // 4.1.28: the White Reaper taking his jacket off: nothing else until it is off
        }
        if (dress(agent, mind, level, now)) {
            return; // 4.1.38: putting it back on, the fight long over: nothing else until it is on
        }
        if (BikakuDevour.held(agent)) {
            // 3.6.3: being devoured: whatever it was doing stops, its kagune-made weapon back in its case, nothing more.
            if (mind.armOut) {
                AgentArms.sheathe(agent, mind, level, now);
            }
            AgentArms.cancel(agent, mind, now);
            cancelKick(agent, mind, now);
            cancelBrawl(agent, mind, now);
            cancelLightning(agent, mind, now);
            cancelGround(agent, mind, now);
            return;
        }
        arm(agent, mind, now);
        CcgAgent partner = partner(agent, mind, level, now);
        if (lost(agent, mind, partner, level, now)) {
            return;
        }
        regen(agent, mind, now);
        AgentArms.tick(agent, mind, level, now);
        if (rooted(agent, mind, level, now)) {
            return; // 2.0.33: its lance in the ground for its tendril: it holds there until it pulls it out
        }
        if (soar(agent, mind, level, now)) {
            return; // 2.0.49: the White Reaper soaring, aiming from the top of it
        }
        if (feet(agent, mind, now)) {
            return; // in a flip or a dash: nothing else this tick
        }
        if (PaleStun.stunned(agent)) {
            AgentArms.cancel(agent, mind, now);
            cancelKick(agent, mind, now);
            cancelBrawl(agent, mind, now);
            cancelLightning(agent, mind, now);
            cancelGround(agent, mind, now);
            return;
        }
        // 2.1.9: before Peace or War: a junior runs; a senior trembles, and its nerve keeps giving (it backs off, then comes again).
        agent.afraid(now < mind.afraidUntil || now < mind.routedUntil);
        if (routed(agent, mind, level, now)) {
            return;
        }
        if (now < mind.afraidUntil && now >= mind.fallBackUntil && FinalSight.breaks(agent.m_19879_(), now)) {
            mind.falterFrom = mind.afraidOf;
            mind.falteredAt = now;
            mind.falterUntil = now;
            mind.fallBackUntil = now + FinalSight.BACKS_OFF;
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

    /**
     * 4.1.31 (the owner: "After the fight is done, he breaks the weapon. The blade shatters at the end, and it's just broken. And he accepts his
     * death a little before he dies actually, and then he just falls on his knees and dies after the weapon shatters"): his end, begun by the
     * blow that would have brought him to {@link AgentRules#REAPER_END_HEALTH} (hurt): he stands where he is and lowers his blade; it
     * shatters ({@link AgentRules#REAPER_END_SHATTER}: shards, the sound of it); he goes down on his knees and bows his head; at
     * {@link AgentRules#REAPER_END_TICKS} he dies by that blow (its striker his killer: Slain Reaper, his mastery and Dawn as 4.1.23). True
     * while it plays.
     */
    private static boolean ending(CcgAgent agent, Mind mind, ServerLevel level, long now) {
        if (mind.endAt == Long.MIN_VALUE) {
            return false;
        }
        long age = ago(now, mind.endAt);
        pin(agent, mind, level);
        if (!mind.shattered && age >= AgentRules.REAPER_END_SHATTER) {
            mind.shattered = true;
            Vec3 at = agent.m_20182_().m_82520_(0.0, 1.0, 0.0);
            UkakuFire.sound(level, at, "block.glass.break", 1.2F, 0.6F);
            UkakuFire.sound(level, at, "hexkagune:sfx.blade_heavy", 1.0F, 0.5F);
        }
        if (age >= AgentRules.REAPER_END_TICKS && agent.m_6084_()) {
            mind.finishing = true;
            try {
                agent.f_19802_ = 0;
                agent.m_6469_(mind.endSource != null ? mind.endSource : agent.m_269291_().m_269333_(agent), Float.MAX_VALUE);
            } finally {
                mind.finishing = false;
            }
        }
        return true;
    }

    /**
     * 4.1.28 (the owner: "he pulls out his jacket, he just removes his jacket, revealing a new skin ... During this next phase, he gets way
     * faster"): at half health (once), as soon as he stands on the ground, the White Reaper stops whatever he was doing and takes his
     * jacket off ({@link CcgAgent#SHED}: both hands to his lapels, the coat swept back off his shoulders, a roll of his neck), no blow
     * landing on him while he does; off his shoulders ({@link AgentRules#REAPER_SHED_SWAP}) his look is the bare one and he is faster, kept
     * with him ({@link #BARE}). True while he does it.
     */
    private static boolean shed(CcgAgent agent, Mind mind, ServerLevel level, long now) {
        if (mind.rank != AgentRules.Rank.RANK_10 || mind.bare && mind.shedAt == Long.MIN_VALUE) {
            return false;
        }
        if (mind.shedAt == Long.MIN_VALUE) {
            if (!mind.shedPending && agent.m_21223_() > AgentRules.REAPER_SHED_HEALTH) {
                return false;
            }
            mind.shedPending = true;
            if (mind.shedPendingAt == Long.MIN_VALUE) {
                mind.shedPendingAt = now;
            }
            // 4.1.37: landing first (a soar, a flip, a dash play out), but never waited for forever: in water, on a ladder, held up
            // by something, he never "lands", and with the jacket on he could never fall below half.
            boolean waited = ago(now, mind.shedPendingAt) >= AgentRules.REAPER_SHED_WAIT;
            if (BikakuDevour.held(agent) || !waited && (!agent.m_20096_() || mind.soarAt != Long.MIN_VALUE)) {
                return false;
            }
            mind.shedPending = false;
            mind.shedPendingAt = Long.MIN_VALUE;
            mind.soarAt = Long.MIN_VALUE;
            mind.shedAt = now;
            mind.pinnedAt = agent.m_20182_();
            cancelKick(agent, mind, now);
            cancelBrawl(agent, mind, now);
            cancelLightning(agent, mind, now);
            mind.groundAt = Long.MIN_VALUE; // a lance in the ground pulled out with the rest
            mind.groundSpot = null;
            mind.striking = false;
            mind.parrying = false;
            agent.m_21573_().m_26573_();
            agent.swing(CcgAgent.SHED, now);
            UkakuFire.sound(level, agent.m_20182_(), "hexkagune:sfx.whoosh", 0.6F, 0.6F);
            return true;
        }
        long age = ago(now, mind.shedAt);
        pin(agent, mind, level);
        if (mind.prey != 0 && level.m_6815_(mind.prey) instanceof LivingEntity prey) {
            agent.m_21563_().m_24960_(prey, 30.0F, 30.0F);
        }
        if (!mind.bare && age >= AgentRules.REAPER_SHED_SWAP) {
            mind.bare = true;
            mind.look = AgentRules.REAPER_BARE_LOOK;
            agent.getPersistentData().m_128405_(BARE, 1);
            agent.dress(mind.look, mind.kind);
            agent.m_21051_(net.minecraft.world.entity.ai.attributes.Attributes.f_22279_).m_22100_(speed(mind));
            UkakuFire.sound(level, agent.m_20182_(), "hexkagune:sfx.whoosh", 0.9F, 0.75F);
        }
        if (age >= AgentRules.REAPER_CHANGE_TICKS || age < 0L) {
            mind.shedAt = Long.MIN_VALUE;
            mind.pinnedAt = null;
            mind.readyAt = Math.max(mind.readyAt, now + 4L);
            agent.swing(-1, now);
            return false;
        }
        return true;
    }

    /**
     * 4.1.38 (the owner: "after the fight ends, I should note, let's say 2 minutes without fighting, Arima puts back his jacket in his
     * normal"; asked how, they chose it reversed in place): bare, on the ground, with nobody to fight, struck by nobody and fighting nobody for
     * {@link AgentRules#REAPER_REDRESS_CALM}, he puts it back on where he stands ({@link CcgAgent#DRESS}: the blade back into the golden case
     * and the case away behind his back, then the coat back up his arms and onto his shoulders), untouchable and pinned as when he took it
     * off; under the coat ({@link AgentRules#REAPER_DRESS_SWAP}) his look, his speed and Dawn are his first phase's again and he is whole
     * (healed: the fight is over). True while he does it.
     */
    private static boolean dress(CcgAgent agent, Mind mind, ServerLevel level, long now) {
        if (mind.rank != AgentRules.Rank.RANK_10 || mind.endAt != Long.MIN_VALUE) {
            return false;
        }
        if (mind.dressAt == Long.MIN_VALUE) {
            if (!mind.bare || mind.prey != 0 || ago(now, mind.foughtAt) < AgentRules.REAPER_REDRESS_CALM
                || ago(now, mind.hurtAt) < AgentRules.REAPER_REDRESS_CALM || !agent.m_20096_() || mind.bladeAt != Long.MIN_VALUE
                || now < mind.dodgeUntil || now < mind.rushUntil) {
                return false;
            }
            mind.dressAt = now;
            mind.pinnedAt = agent.m_20182_();
            agent.m_21573_().m_26573_();
            agent.swing(CcgAgent.DRESS, now);
            UkakuFire.sound(level, agent.m_20182_(), "hexkagune:sfx.whoosh", 0.5F, 0.7F);
            return true;
        }
        long age = ago(now, mind.dressAt);
        pin(agent, mind, level);
        if (mind.bare && age >= AgentRules.REAPER_DRESS_SWAP) {
            mind.bare = false;
            mind.look = AgentRules.REAPER_LOOK;
            agent.getPersistentData().m_128473_(BARE);
            agent.dress(mind.look, mind.kind);
            agent.m_21051_(net.minecraft.world.entity.ai.attributes.Attributes.f_22279_).m_22100_(speed(mind));
            agent.m_21153_(agent.m_21233_());
            mind.bladeStep = 0;
            mind.shattered = false;
            UkakuFire.sound(level, agent.m_20182_(), "hexkagune:sfx.whoosh", 0.8F, 0.85F);
        }
        if (age >= AgentRules.REAPER_DRESS_TICKS || age < 0L) {
            mind.dressAt = Long.MIN_VALUE;
            mind.pinnedAt = null;
            mind.readyAt = Math.max(mind.readyAt, now + 4L);
            agent.swing(-1, now);
            return false;
        }
        return true;
    }

    /**
     * 4.1.37 (the owner: "whenever he's transforming, you can still grab him, which should not be allowed"; then: "he's still very hittable.
     * Like, I told you to make him completely immortal while he's transforming"): the White Reaper while he changes (the jacket, the case,
     * the blade) or in his end. Nothing lands on him then ({@link #attacked}: before vanilla's knockback, red flash and hurt sound, not only
     * the damage), nothing targets him (the original kagune's checks ask {@link EtoBrain#spared}, which asks this; the quinque arms ask
     * {@code PlayerArms.mayHit}), and nothing holds him ({@link #pin}).
     */
    public static boolean untouchable(Entity body) {
        if (!(body instanceof CcgAgent agent) || agent.mind == null) {
            return false;
        }
        Mind mind = agent.mind;
        return mind.rank == AgentRules.Rank.RANK_10 && !mind.finishing
            && (mind.shedAt != Long.MIN_VALUE || mind.endAt != Long.MIN_VALUE || mind.dressAt != Long.MIN_VALUE);
    }

    /**
     * 4.1.37: held where he stood when it began, every tick of his change or his end: anything that held him let go of him, his own way
     * and any push gone (he still falls), and put back if something moved him.
     */
    private static void pin(CcgAgent agent, Mind mind, ServerLevel level) {
        agent.m_21573_().m_26573_();
        if (mind.pinnedAt == null) {
            mind.pinnedAt = agent.m_20182_();
        }
        free(agent, level);
        Vec3 velocity = agent.m_20184_();
        agent.m_20256_(new Vec3(0.0, Math.min(0.0, velocity.f_82480_), 0.0));
        agent.f_19864_ = true;
        Vec3 at = agent.m_20182_();
        double dx = at.f_82479_ - mind.pinnedAt.f_82479_;
        double dz = at.f_82481_ - mind.pinnedAt.f_82481_;
        if (dx * dx + dz * dz > 0.01 || at.f_82480_ > mind.pinnedAt.f_82480_ + 0.3) {
            agent.m_6021_(mind.pinnedAt.f_82479_, Math.min(at.f_82480_, mind.pinnedAt.f_82480_), mind.pinnedAt.f_82481_);
        }
    }

    /** 4.1.37: every player's hold on him let go: the original kagune's tails, a Bikaku's, a Pale's flesh, a Graft's limbs, a bound arm. */
    private static void free(CcgAgent agent, ServerLevel level) {
        int id = agent.m_19879_();
        for (ServerPlayer player : level.m_6907_()) {
            if (player.m_20280_(agent) > 96.0 * 96.0) {
                continue;
            }
            com.hex.kagune.gameplay.KaguneRuntime.State tails = com.hex.kagune.gameplay.KaguneRuntime.peek(player.m_20148_());
            if (tails != null && tails.heldEntityIds != null) {
                for (int slot = 0; slot < tails.heldEntityIds.length; slot++) {
                    if (tails.heldEntityIds[slot] == id) {
                        com.hex.kagune.gameplay.KaguneServerController.releaseHeldSlot(level, tails, slot);
                    }
                }
            }
            if (BikakuServer.isBikaku(player)) {
                BikakuServer.State bikaku = BikakuServer.state(player);
                boolean holds = bikaku.heldId == id;
                for (int worm : bikaku.held) {
                    holds |= worm == id;
                }
                if (holds) {
                    BikakuMoves.release(player, bikaku, false);
                }
            }
            if (PaleServer.isPale(player) && PaleServer.state(player).heldId == id) {
                PaleFlesh.release(player, PaleServer.state(player));
            }
            if (GraftServer.isGraft(player) && GraftServer.state(player).heldId == id) {
                GraftCombat.release(player, GraftServer.state(player), false);
            }
            PlayerArms.unbind(player, id);
        }
    }

    /**
     * 4.1.29 (the owner: "a heavy weapon, dual-handed, that deals massive damage and, yeah, just causes bleed whenever hit. Arima can be super
     * fast while using it"): the White Reaper with his blade: he closes on his prey and, in reach, throws his string (ReaperBladePose.STRING:
     * a cleave, a rising cut, a sweep, a thrust), each blow landing at its impact on everything in its arc and reach (hurt by its weight,
     * thrown back, two stacks of bleeding), the next chained while his prey stays in reach, a rest after the last.
     */
    private static void blade(CcgAgent agent, Mind mind, LivingEntity prey, ServerLevel level, long now) {
        // 4.1.30: a sidestep under way, a rush under way; then, unless committed to a blow, whatever is coming at him dodged.
        if (now < mind.dodgeUntil) {
            Vec3 velocity = agent.m_20184_();
            agent.m_20256_(new Vec3(mind.dodgeWay.f_82479_ * ReaperBladeRules.DODGE_SPEED, Math.max(velocity.f_82480_, 0.0),
                mind.dodgeWay.f_82481_ * ReaperBladeRules.DODGE_SPEED));
            agent.f_19864_ = true;
            agent.m_21573_().m_26573_();
            return;
        }
        if (now < mind.rushUntil) {
            rush(agent, mind, prey, level, now);
            return;
        }
        boolean committed = mind.bladeAt != Long.MIN_VALUE && ReaperBladeRules.committed(mind.bladeStep, ago(now, mind.bladeAt));
        if (!committed && agent.m_20096_() && now >= mind.dodgeReadyAt && dodge(agent, mind, prey, level, now)) {
            return;
        }
        if (mind.bladeAt != Long.MIN_VALUE) {
            long age = ago(now, mind.bladeAt);
            ReaperBladeRules.Blow blow = ReaperBladeRules.blow(mind.bladeStep);
            agent.m_21573_().m_26573_();
            Vec3 velocity = agent.m_20184_();
            if (age >= blow.impact() - 2 && age <= blow.impact() && agent.m_20096_()) {
                // His step in as it drives.
                Vec3 ahead = UkakuFire.horizontal(prey.m_20182_().m_82546_(agent.m_20182_())).m_82490_(0.22);
                agent.m_20256_(new Vec3(ahead.f_82479_, velocity.f_82480_, ahead.f_82481_));
            } else {
                agent.m_20256_(new Vec3(velocity.f_82479_ * 0.4, velocity.f_82480_, velocity.f_82481_ * 0.4));
            }
            if (!mind.bladeLanded && age >= blow.impact()) {
                mind.bladeLanded = true;
                bladeLands(agent, mind, level, now);
                if (mind.bladeAt == Long.MIN_VALUE) {
                    return; // 4.1.37: it met a blow of theirs (KaguneClash): thrown apart, his string broken off
                }
            }
            if (age >= blow.total() || age < 0L) {
                mind.bladeAt = Long.MIN_VALUE;
                agent.swing(-1, now);
                boolean last = mind.bladeStep >= ReaperBladeRules.STEPS - 1;
                double gap = Math.sqrt(prey.m_20280_(agent)) - (prey.m_20205_() + agent.m_20205_()) * 0.5;
                if (!last && gap <= AgentRules.REAPER_BLADE_REACH) {
                    startBlade(agent, mind, level, now, mind.bladeStep + 1);
                } else {
                    mind.bladeStep = 0;
                    mind.readyAt = now + Math.round(AgentRules.REAPER_BLADE_REST * AgentRules.REAPER_BARE_PACE);
                }
            }
            return;
        }
        double gap = Math.sqrt(prey.m_20280_(agent)) - (prey.m_20205_() + agent.m_20205_()) * 0.5;
        // 4.1.30: out of reach and not too far, in sight: a rush straight in, stopping where his blade reaches, swinging as it stops.
        if (gap > AgentRules.REAPER_BLADE_REACH + 0.5 && gap < ReaperBladeRules.RUSH_FROM && now >= mind.rushReadyAt && agent.m_20096_()
            && agent.m_142582_(prey) && clear(agent, level, UkakuFire.horizontal(prey.m_20182_().m_82546_(agent.m_20182_())))) {
            Vec3 way = UkakuFire.horizontal(prey.m_20182_().m_82546_(agent.m_20182_()));
            double stop = (prey.m_20205_() + agent.m_20205_()) * 0.5 + ReaperBladeRules.RUSH_STOP;
            mind.rushTo = prey.m_20182_().m_82546_(way.m_82490_(stop));
            mind.rushUntil = now + ReaperBladeRules.RUSH_LONGEST;
            mind.rushReadyAt = now + ReaperBladeRules.RUSH_EVERY;
            agent.dash(0, now);
            UkakuFire.sound(level, agent.m_20182_(), "hexkagune:sfx.dash", 0.9F, 0.9F);
            rush(agent, mind, prey, level, now);
            return;
        }
        if (gap > AgentRules.REAPER_BLADE_REACH - 0.4 || now < mind.readyAt) {
            if (gap > 1.2) {
                agent.m_21573_().m_26519_(prey.m_20185_(), prey.m_20186_(), prey.m_20189_(), 1.35);
            } else {
                agent.m_21573_().m_26573_();
            }
            return;
        }
        startBlade(agent, mind, level, now, 0);
    }

    /**
     * 4.1.30: his rush in: straight at where it stops (kept on his prey as they move), as fast as {@link ReaperBladeRules#RUSH_SPEED} and never
     * past it; there (or out of time) it ends and his string begins at once.
     */
    private static void rush(CcgAgent agent, Mind mind, LivingEntity prey, ServerLevel level, long now) {
        Vec3 way = UkakuFire.horizontal(prey.m_20182_().m_82546_(agent.m_20182_()));
        double stop = (prey.m_20205_() + agent.m_20205_()) * 0.5 + ReaperBladeRules.RUSH_STOP;
        mind.rushTo = prey.m_20182_().m_82546_(way.m_82490_(stop));
        Vec3 left = new Vec3(mind.rushTo.f_82479_ - agent.m_20185_(), 0.0, mind.rushTo.f_82481_ - agent.m_20189_());
        double far = left.m_82553_();
        double step = ReaperBladeRules.rushStep(far);
        agent.m_21573_().m_26573_();
        face(agent, prey);
        Vec3 velocity = agent.m_20184_();
        if (step < 0.05 || now + 1 >= mind.rushUntil) {
            mind.rushUntil = Long.MIN_VALUE;
            agent.m_20256_(new Vec3(0.0, velocity.f_82480_, 0.0));
            agent.f_19864_ = true;
            if (mind.bladeAt == Long.MIN_VALUE) {
                startBlade(agent, mind, level, now, 0);
            }
            return;
        }
        Vec3 push = left.m_82490_(step / Math.max(1.0E-6, far));
        agent.m_20256_(new Vec3(push.f_82479_, Math.max(velocity.f_82480_, 0.0), push.f_82481_));
        agent.f_19864_ = true;
    }

    /**
     * 4.1.30 (the owner: "He'll dodge moves"): a shot coming at him (an arrow, a thrown thing, Ukaku's fire or crystal: as Rinkaku's tendril
     * shield sees them) or a blow (KaguneClash) sidestepped, square to its way, to the side of its line he stands on, if that side is clear;
     * a blow in its chamber given up for it. True if he stepped.
     */
    private static boolean dodge(CcgAgent agent, Mind mind, LivingEntity prey, ServerLevel level, long now) {
        Vec3 middle = GhoulNpcBrain.centre(agent);
        double[] centre = {middle.f_82479_, middle.f_82480_, middle.f_82481_};
        Vec3[] seen = {null, null};
        for (net.minecraft.world.entity.projectile.Projectile shot : level.m_6443_(net.minecraft.world.entity.projectile.Projectile.class,
            agent.m_20191_().m_82400_(24.0), shot -> shot.m_6084_() && shot.m_19749_() != agent)) {
            boolean straight = shot instanceof net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
            double gravity = straight ? 0.0 : shot instanceof net.minecraft.world.entity.projectile.AbstractArrow ? com.hex.kagune.control.TendrilShield.ARROW_GRAVITY
                : com.hex.kagune.control.TendrilShield.THROWN_GRAVITY;
            Vec3 v = shot.m_20184_();
            if (com.hex.kagune.control.TendrilShield.incoming(new double[] {shot.m_20185_(), shot.m_20186_(), shot.m_20189_()},
                new double[] {v.f_82479_, v.f_82480_, v.f_82481_}, gravity, straight ? 1.0 : com.hex.kagune.control.TendrilShield.AIR_DRAG, centre,
                new double[3], 12, 1.2)) {
                seen[0] = shot.m_20182_();
                seen[1] = v;
            }
        }
        java.util.UUID own = agent.m_20148_();
        TendrilShieldServer.Shots shots = (owner, world, position, velocity, radius) -> {
            if (seen[0] == null && !owner.equals(own) && world == level && position.m_82557_(middle) < 24.0 * 24.0
                && com.hex.kagune.control.TendrilShield.incoming(new double[] {position.f_82479_, position.f_82480_, position.f_82481_},
                    new double[] {velocity.f_82479_, velocity.f_82480_, velocity.f_82481_}, centre, 8.0, 1.2 + radius)) {
                seen[0] = position;
                seen[1] = velocity;
            }
        };
        UkakuFire.shots(shots);
        UkakuCrystalCombat.shots(shots);
        if (seen[0] == null) {
            KaguneClash.Threat threat = KaguneClash.threat(agent, now);
            if (threat == null || agent.m_217043_().m_188500_() >= ReaperBladeRules.DODGE_BLOW) {
                return false;
            }
            seen[0] = threat.who().m_20182_();
            seen[1] = agent.m_20182_().m_82546_(threat.who().m_20182_());
        }
        double[] side = ReaperBladeRules.dodge(seen[1].f_82479_, seen[1].f_82481_, agent.m_20185_() - seen[0].f_82479_, agent.m_20189_() - seen[0].f_82481_);
        Vec3 way = new Vec3(side[0], 0.0, side[1]);
        if (!clear(agent, level, way)) {
            way = way.m_82490_(-1.0);
            if (!clear(agent, level, way)) {
                return false;
            }
        }
        if (mind.bladeAt != Long.MIN_VALUE) {
            mind.bladeAt = Long.MIN_VALUE; // a blow still in its chamber given up
            mind.bladeStep = 0;
            KaguneClash.landed(agent);
        }
        mind.dodgeWay = way;
        mind.dodgeUntil = now + ReaperBladeRules.DODGE_TICKS;
        mind.dodgeReadyAt = now + ReaperBladeRules.DODGE_EVERY;
        mind.readyAt = Math.min(mind.readyAt, now + ReaperBladeRules.DODGE_TICKS); // and straight back at them
        mind.rushReadyAt = Math.min(mind.rushReadyAt, now + ReaperBladeRules.DODGE_TICKS);
        Vec3 facing = UkakuFire.horizontal(prey.m_20182_().m_82546_(agent.m_20182_()));
        boolean left = way.f_82479_ * -facing.f_82481_ + way.f_82481_ * facing.f_82479_ > 0.0;
        agent.dash(left ? 2 : 3, now);
        agent.swing(-1, now);
        UkakuFire.sound(level, agent.m_20182_(), "hexkagune:sfx.dash", 0.7F, 1.25F);
        return true;
    }

    /**
     * 4.1.29: blow {@code step} of his string begins. 4.1.37 (the owner: "his moves are currently not clashable ... You gotta patch that"):
     * on its way it can be met ({@link KaguneClash#swing}), as hard as it would hit.
     */
    private static void startBlade(CcgAgent agent, Mind mind, ServerLevel level, long now, int step) {
        mind.bladeAt = now;
        mind.bladeStep = step;
        mind.bladeLanded = false;
        agent.m_21573_().m_26573_();
        agent.swing(CcgAgent.BLADE + step, now);
        UkakuFire.sound(level, agent.m_20182_(), "hexkagune:sfx.whoosh", 0.9F, 0.55F + 0.05F * step);
        Entity seen = level.m_6815_(mind.prey);
        Vec3 aim = seen != null ? UkakuFire.horizontal(seen.m_20182_().m_82546_(agent.m_20182_())) : null;
        long began = now;
        KaguneClash.swing(agent, now, ReaperBladeRules.blow(step).impact(), AgentRules.REAPER_BLADE_REACH + 0.5, aim, seen != null ? seen.m_19879_() : 0,
            com.hex.kagune.control.ClashPower.agent(AgentRules.power(mind.rank, mind.superior), AgentRules.REAPER_BLADE_DAMAGE * AgentRules.REAPER_BLADE_WEIGHT[step]),
            () -> mind.bladeAt == began && !mind.bladeLanded, () -> bladeMet(agent, mind, began));
    }

    /** 4.1.37: his blow met another (KaguneClash: both thrown apart): nothing of it lands, his string broken off, a beat before the next. */
    private static void bladeMet(CcgAgent agent, Mind mind, long began) {
        if (mind.bladeAt != began) {
            return;
        }
        long now = agent.m_9236_().m_46467_();
        mind.bladeAt = Long.MIN_VALUE;
        mind.bladeLanded = true;
        mind.bladeStep = 0;
        mind.readyAt = Math.max(mind.readyAt, now + 10L);
        agent.swing(-1, now);
    }

    /** 4.1.29: a blow of his blade lands on everything before him in its arc and reach: hurt, thrown, left bleeding. */
    private static void bladeLands(CcgAgent agent, Mind mind, ServerLevel level, long now) {
        int step = mind.bladeStep;
        // 4.1.37: unless it meets a blow of his prey's coming back at him: then both are thrown apart and nothing lands.
        if (level.m_6815_(mind.prey) instanceof LivingEntity prey && KaguneClash.meets(agent, prey, now)) {
            return;
        }
        KaguneClash.landed(agent);
        double yaw = Math.toRadians(agent.f_20883_);
        Vec3 facing = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        float damage = AgentRules.REAPER_BLADE_DAMAGE * AgentRules.REAPER_BLADE_WEIGHT[step] * AgentRules.power(mind.rank, mind.superior);
        boolean hit = false;
        for (LivingEntity body : level.m_6443_(LivingEntity.class, agent.m_20191_().m_82400_(AgentRules.REAPER_BLADE_REACH + 1.0),
            b -> b != agent && b.m_6084_() && !ccg(b))) {
            Vec3 to = body.m_20182_().m_82546_(agent.m_20182_());
            double gap = Math.sqrt(body.m_20280_(agent)) - (body.m_20205_() + agent.m_20205_()) * 0.5;
            Vec3 flat = UkakuFire.horizontal(to);
            if (gap > AgentRules.REAPER_BLADE_REACH || flat.m_82526_(facing) < AgentRules.REAPER_BLADE_ARC || Math.abs(to.f_82480_) > 3.0) {
                continue;
            }
            if (body instanceof Player player && player.m_7500_()) {
                continue;
            }
            Vec3 at = GhoulNpcBrain.centre(body).m_82546_(flat.m_82490_(body.m_20205_() * 0.5));
            WoundHits.expect(body, step == 3 ? WoundBook.Blow.STAB : WoundBook.Blow.SLASH, at, flat, 0.0);
            body.f_19802_ = 0;
            if (body.m_6469_(agent.m_269291_().m_269333_(agent), damage)) {
                Vec3 knock = flat.m_82490_(AgentRules.REAPER_BLADE_KNOCK[step]).m_82520_(0.0, 0.12, 0.0);
                body.m_20256_(body.m_20184_().m_82490_(0.3).m_82549_(knock));
                body.f_19864_ = true;
                UkakuCrystalCombat.agentBleed(body, agent, AgentRules.REAPER_BLADE_BLEED, now);
                hit = true;
            }
        }
        UkakuFire.sound(level, agent.m_20182_(), hit ? "hexkagune:sfx.blade_heavy" : "entity.player.attack.sweep", 1.0F, hit ? 0.7F : 0.8F);
    }

    /**
     * The stack in its hand is its quinque: its case shut, or its weapon while it is open (put back if anything took it). 3.0.0: a
     * kagune-made weapon is no item: its hand is empty while it is out (and while it goes back into its case), the case in it at peace.
     */
    private static void arm(CcgAgent agent, Mind mind, long now) {
        ItemStack held = agent.m_21205_();
        if (mind.shedAt != Long.MIN_VALUE || mind.bare || mind.dressAt != Long.MIN_VALUE) {
            // 4.1.28: both hands on his jacket: Dawn put by while he takes it off; 4.1.29: and for good after (his blade is no item: drawn
            // by ReaperBladeLayer, never dropped). Dawn's case still falls with him (drop).
            if (!held.m_41619_()) {
                agent.m_21008_(InteractionHand.MAIN_HAND, ItemStack.f_41583_);
            }
            return;
        }
        if (mind.arm != null && (mind.armOut || ago(now, mind.armAt) < ArmRules.UNFOLD)) {
            if (!held.m_41619_()) {
                agent.m_21008_(InteractionHand.MAIN_HAND, ItemStack.f_41583_);
            }
            return;
        }
        if (mind.arm != null) {
            // 3.0.6: at peace, its own weapon's case (the one it leaves when it falls).
            if (ControlItems.armCase(held) != mind.arm) {
                agent.m_21008_(InteractionHand.MAIN_HAND, ControlItems.armCaseStack(mind.arm, mind.master));
            }
            return;
        }
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
                && shared.m_20280_(agent) <= AgentRules.LEASH * AgentRules.LEASH && !ccg(shared)) {
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
            if (by instanceof LivingEntity living && living.m_6084_() && !ccg(living) && !(living instanceof Player p && p.m_7500_())
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
        if (body == agent || !body.m_6084_() || ccg(body) || body.m_5833_() || ReviveForget.presumedDead(body, now)) {
            return false; // 2.4.2: and never one thought dead (4.1.46: nor one of the CCG's ghoul division)
        }
        if (body instanceof Player player && player.m_7500_()) {
            return false;
        }
        if (body instanceof ServerPlayer player) {
            // 4.1.71 (the owner, the CCG's traits: "CCG no longer attacks the player."): one of the CCG never, even struck or marked, unless
            // their bar is empty and they stayed in the tower past the warning; one it banned, on sight.
            int stance = GangServer.ccgStance(player);
            if (stance == GangServer.OWN_GANG) {
                return GangServer.turned(player);
            }
            if (stance == GangServer.OUTCAST) {
                return true;
            }
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
            if (player.m_5833_() || player.m_7500_() || player.m_20280_(agent) > AgentRules.REVEALED_RANGE * AgentRules.REVEALED_RANGE
                || GangServer.ccgStance(player) == GangServer.OWN_GANG) {
                continue; // 4.1.71: one of the CCG with a kagune is one of theirs
            }
            double distance = Math.sqrt(player.m_20280_(agent));
            // 1.3.65: a final kakuja (War, Peace, the Dragon: the kagune itself in) shows what they are as much as a kagune.
            FinalSight sight = Sightings.seen(player);
            // 4.0.7: kakugan behind a full mask is not seen (Sightings.masked); a kagune or a kakuja still is.
            boolean shown = shows(player); // 1.3.70: their eyes on too
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

    /** 2.1.9: running from Peace or War, as far as {@link FinalSight#RUNS_TO}, its weapon down; true while it is. */
    private static boolean routed(CcgAgent agent, Mind mind, ServerLevel level, long now) {
        if (now >= mind.routedUntil) {
            return false;
        }
        if (!(level.m_6815_(mind.routedFrom) instanceof LivingEntity dread) || !dread.m_6084_()) {
            mind.routedUntil = Long.MIN_VALUE;
            return false;
        }
        end(agent, mind);
        if (dread.m_20280_(agent) < FinalSight.RUNS_TO * FinalSight.RUNS_TO && (now % 5L == 0L || agent.m_21573_().m_26571_())) {
            double[] to = com.hex.kagune.control.npc.HumanRules.fleeStep(agent.m_20185_(), agent.m_20189_(), dread.m_20185_(), dread.m_20189_(), 0.0, 9.0);
            agent.m_21573_().m_26519_(to[0], agent.m_20186_(), to[1], AgentRules.PATROL_PACE + 0.6);
        }
        return true;
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
        cancelBrawl(agent, mind, now);
        cancelLightning(agent, mind, now);
        if (mind.open && ago(now, mind.foughtAt) > AgentRules.CALM && QuinqueServer.hand(agent, level, agent.m_21205_(), mind.kind, false)) {
            mind.open = false;
        }
        if (mind.armOut && ago(now, mind.foughtAt) > AgentRules.CALM) {
            AgentArms.sheathe(agent, mind, level, now); // 3.0.0: back into its case
        }
        mind.shots = 0;
        VillagePlace.Place place = VillagePlace.of(agent);
        if (place != null) {
            // 4.1.4: the CCG building's: at its post or its desk; the rest walk its patrol.
            if (!AgentDesk.idle(agent, mind, place, level, now)) {
                village(agent, mind, place, now);
            }
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
        CompoundTag saved = agent.getPersistentData();
        if (saved.m_128441_(HOME_X) && reaper(agent)) {
            // 4.1.24: the White Reaper at home: strolling his garden, never off his grounds; walked back from a chase at once.
            double hx = saved.m_128451_(HOME_X) + 0.5;
            double hy = saved.m_128451_(HOME_Y);
            double hz = saved.m_128451_(HOME_Z) + 0.5;
            double dx = agent.m_20185_() - hx;
            double dy = agent.m_20186_() - hy;
            double dz = agent.m_20189_() - hz;
            double away = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (away > AgentRules.REAPER_HOME_STROLL + 2.0 && (now % 20L == 0L || agent.m_21573_().m_26571_())) {
                agent.m_21573_().m_26519_(hx, hy, hz, away > AgentRules.REAPER_HOME_LEASH ? 1.0 : AgentRules.PATROL_PACE);
                return;
            }
            if (now < mind.wanderAt || !agent.m_21573_().m_26571_()) {
                return;
            }
            mind.wanderAt = now + 140L + (long)(agent.m_217043_().m_188500_() * 220.0);
            double angle = agent.m_217043_().m_188500_() * Math.PI * 2.0;
            double distance = agent.m_217043_().m_188500_() * AgentRules.REAPER_HOME_STROLL;
            agent.m_21573_().m_26519_(hx + Math.cos(angle) * distance, hy, hz + Math.sin(angle) * distance, AgentRules.PATROL_PACE * 0.8);
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

    /** Whatever blow it was making is let go of (3.0.0: and any move of its own under way). */
    static void end(CcgAgent agent, Mind mind) {
        if (mind.striking) {
            mind.striking = false;
            KaguneClash.landed(agent);
        }
        if (mind.move != null) {
            AgentArms.cancel(agent, mind, agent.m_9236_().m_46467_());
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
        if (mind.arm != null) {
            AgentArms.fight(agent, mind, partner, prey, level, now); // 3.0.0: its kagune-made weapon
            return;
        }
        if (mind.bare && mind.rank == AgentRules.Rank.RANK_10) {
            blade(agent, mind, prey, level, now); // 4.1.29: the White Reaper's second phase: his blade, nothing of Dawn's
            return;
        }
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
        if (tactics(agent, mind, prey, level, now)) {
            return; // 2.0.49: the White Reaper's read of the fight
        }
        if (ground(agent, mind, prey, level, now) || lightning(agent, mind, prey, level, now) || kick(agent, mind, prey, level, now)
            || brawl(agent, mind, prey, level, now)) {
            return;
        }
        if (mind.striking && now >= mind.landsAt) {
            land(agent, mind, level, now);
        }
        if (maneuver(agent, mind, prey, level, now)) {
            return;
        }
        if (ranged(mind)) {
            shoot(agent, mind, partner, prey, level, now);
        } else {
            close(agent, mind, partner, prey, level, now);
        }
    }

    // ---------------------------------------------------------------- 2.0.33: the White Reaper's ground tendril

    /** Broken off before the lance is in the ground (stunned, its prey gone): nothing rises. */
    private static void cancelGround(CcgAgent agent, Mind mind, long now) {
        if (mind.groundAt != Long.MIN_VALUE && mind.groundSpot == null) {
            mind.groundAt = Long.MIN_VALUE;
            agent.swing(-1, now);
        }
    }

    /**
     * The tendril, started ({@link ReaperTendril}): he raises Dawn while his prey is in range and in sight, and at {@link ReaperTendril#PLANT}
     * drives it into the ground ({@link #plant}). True while he is raising it (nothing else this tick); once it is in, {@link #rooted}
     * holds him.
     */
    private static boolean ground(CcgAgent agent, Mind mind, LivingEntity prey, ServerLevel level, long now) {
        if (mind.rank != AgentRules.Rank.RANK_10 || mind.kind != Quinque.DAWN) {
            return false;
        }
        if (mind.groundAt != Long.MIN_VALUE) {
            agent.m_21573_().m_26573_();
            if (mind.groundSpot == null && ago(now, mind.groundAt) >= ReaperTendril.PLANT) {
                plant(agent, mind, level, now);
            }
            return true;
        }
        if (mind.striking || mind.kickAt != Long.MIN_VALUE || mind.aimAt != Long.MIN_VALUE || now < mind.groundReadyAt || now < mind.readyAt
            || !agent.m_20096_()) {
            return false;
        }
        double range = Math.sqrt(prey.m_20280_(agent));
        if (range < ReaperTendril.NEAREST || range > ReaperTendril.FARTHEST || !agent.m_142582_(prey)
            || agent.m_217043_().m_188500_() >= ReaperTendril.CHANCE) {
            return false;
        }
        mind.groundAt = now;
        mind.groundTarget = prey.m_19879_();
        mind.groundSpot = null;
        mind.erupted = false;
        mind.groundReadyAt = now + ReaperTendril.COOLDOWN;
        agent.m_21573_().m_26573_();
        agent.swing(CcgAgent.GROUND, now);
        UkakuFire.sound(level, agent.m_20182_(), "hexkagune:sfx.blade_heavy", 0.8F, 0.65F);
        return true;
    }

    /** The lance goes into the ground: where its prey stands now (on the ground under it, if it is in the air) is marked. */
    private static void plant(CcgAgent agent, Mind mind, ServerLevel level, long now) {
        Entity found = level.m_6815_(mind.groundTarget);
        if (!(found instanceof LivingEntity body) || !body.m_6084_()) {
            cancelGround(agent, mind, now);
            return;
        }
        Vec3 feet = body.m_20182_();
        HitResult down = level.m_45547_(new ClipContext(feet.m_82520_(0.0, 0.5, 0.0), feet.m_82520_(0.0, -8.0, 0.0), ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, agent));
        Vec3 spot = down.m_6662_() == HitResult.Type.MISS ? feet : down.m_82450_();
        mind.groundSpot = spot;
        Vec3 lance = agent.m_20182_().m_82549_(UkakuFire.horizontal(agent.m_20154_()).m_82490_(1.5));
        UkakuFire.sound(level, lance, "hexkagune:sfx.crunch", 1.0F, 0.6F);
        UkakuFire.sound(level, lance, "hexkagune:sfx.shockwave", 0.7F, 0.55F);
        UkakuFire.sound(level, spot, "hexkagune:sfx.shockwave", 0.5F, 0.5F);
        ControlNetwork.sendNear(level, spot, 80.0, new S2CUkakuFx(S2CUkakuFx.REAPER_MARK, agent.m_19879_(), 0, spot.f_82479_, spot.f_82480_,
            spot.f_82481_, (float)lance.f_82479_, (float)lance.f_82480_, (float)lance.f_82481_, (float)ReaperTendril.RADIUS, ReaperTendril.WARN));
    }

    /** True while its lance is in the ground (it stands there, nothing else); the tendril rises on time, then the lance comes out. */
    private static boolean rooted(CcgAgent agent, Mind mind, ServerLevel level, long now) {
        if (mind.groundAt == Long.MIN_VALUE) {
            return false;
        }
        long age = ago(now, mind.groundAt);
        if (mind.groundSpot == null) {
            if (age > ReaperTendril.PLANT + 2) {
                cancelGround(agent, mind, now); // its prey went before the lance went in
            }
            return false;
        }
        if (!mind.erupted && age >= ReaperTendril.erupts()) {
            mind.erupted = true;
            erupt(agent, mind, level, now);
        }
        if (age >= ReaperTendril.DURATION) {
            mind.groundAt = Long.MIN_VALUE;
            mind.groundSpot = null;
            mind.readyAt = Math.max(mind.readyAt, now + 6L);
            agent.swing(-1, now);
            return false;
        }
        agent.m_21573_().m_26573_();
        Vec3 velocity = agent.m_20184_();
        agent.m_20256_(new Vec3(velocity.f_82479_ * 0.3, velocity.f_82480_, velocity.f_82481_ * 0.3));
        return true;
    }

    /** The tendril bursts up out of the mark: everyone on it it is after is struck and thrown up. */
    private static void erupt(CcgAgent agent, Mind mind, ServerLevel level, long now) {
        Vec3 spot = mind.groundSpot;
        double r = ReaperTendril.RADIUS + 0.8;
        AABB area = new AABB(spot.f_82479_ - r, spot.f_82480_ - 1.0, spot.f_82481_ - r, spot.f_82479_ + r, spot.f_82480_ + ReaperTendril.HEIGHT,
            spot.f_82481_ + r);
        for (LivingEntity body : level.m_6443_(LivingEntity.class, area, b -> b != agent && b.m_6084_() && foe(agent, b))) {
            Vec3 feet = body.m_20182_();
            if (!ReaperTendril.hits(feet.f_82479_ - spot.f_82479_, feet.f_82480_ - spot.f_82480_, feet.f_82481_ - spot.f_82481_)) {
                continue;
            }
            Vec3 centre = GhoulNpcBrain.centre(body);
            WoundHits.expect(body, WoundBook.Blow.STAB, centre.m_82520_(0.0, -0.4, 0.0), new Vec3(0.0, 1.0, 0.0), 0.0);
            body.f_19802_ = 0;
            if (body.m_6469_(agent.m_269291_().m_269333_(agent), ReaperTendril.DAMAGE)) {
                Vec3 away = UkakuFire.horizontal(feet.m_82546_(spot));
                body.m_20256_(new Vec3(away.f_82479_ * 0.35, ReaperTendril.THROW_UP, away.f_82481_ * 0.35));
                body.f_19864_ = true;
            }
        }
        int seed = (int)(now & 0x7FFFFFFF) ^ agent.m_19879_();
        ControlNetwork.sendNear(level, spot, 96.0, new S2CUkakuFx(S2CUkakuFx.REAPER_TENDRIL, agent.m_19879_(), seed, spot.f_82479_, spot.f_82480_,
            spot.f_82481_, 0.0F, 1.0F, 0.0F, ReaperTendril.THICKNESS, ReaperTendril.LIFE));
        UkakuFire.sound(level, spot, "hexkagune:sfx.burst", 1.1F, 0.6F);
        UkakuFire.sound(level, spot, "hexkagune:sfx.crunch", 1.0F, 0.75F);
        UkakuFire.sound(level, spot, "hexkagune:sfx.shockwave", 0.9F, 0.7F);
    }

    private static void cancelLightning(CcgAgent agent, Mind mind, long now) {
        if (mind.aimAt != Long.MIN_VALUE) {
            if (ControlItems.quinqueWeapon(agent.m_21205_()) == Quinque.DAWN && ago(now, mind.aimAt) < AgentRules.DAWN_AIM_TICKS) {
                QuinqueServer.opens(agent.m_21205_(), mind.aimAt, now); // 2.0.32: broken off: its point closes again
            }
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
            if (ago(now, mind.aimAt) >= mind.aimTicks) {
                release(agent, mind, prey, level, now);
            }
            return true;
        }
        if (range >= AgentRules.DAWN_SHOT_MIN && range <= AgentRules.DAWN_SHOT_RANGE && now >= mind.lightningReadyAt
            && now >= mind.readyAt && agent.m_142582_(prey)) {
            mind.aimAt = now;
            mind.aimTarget = prey.m_19879_();
            mind.aimTicks = AgentRules.DAWN_AIM_TICKS;
            mind.volley = AgentRules.volley(range, agent.m_217043_().m_188500_());
            if (ControlItems.quinqueWeapon(agent.m_21205_()) == Quinque.DAWN) {
                // 2.0.32: its point opens as it aims, the ball gathering there until it flies.
                QuinqueServer.opens(agent.m_21205_(), now, now + AgentRules.DAWN_AIM_TICKS);
            }
            mind.lightningReadyAt = now + AgentRules.DAWN_SHOT_COOLDOWN;
            agent.m_21573_().m_26573_();
            agent.swing(CcgAgent.AIM_DAWN, now);
            return true;
        }
        return false;
    }

    /**
     * The aimed shot leaves Dawn's point at {@code prey}: a single ball or (2.0.48) the volley. 2.0.49: led along the way it is moving
     * ({@link ReaperTactics#lead}: where it will be when the ball gets there; its way read off where it was last tick, which a player's
     * body keeps on the server where its own motion doesn't).
     */
    private static void release(CcgAgent agent, Mind mind, LivingEntity prey, ServerLevel level, long now) {
        cancelLightning(agent, mind, now);
        mind.readyAt = now + 7;
        mind.aimTicks = AgentRules.DAWN_AIM_TICKS;
        Vec3 hand = agent.m_146892_().m_82520_(0.0, -0.52, 0.0);
        Vec3 target = GhoulNpcBrain.centre(prey);
        if (mind.rank == AgentRules.Rank.RANK_10) {
            double[] moving = {prey.m_20185_() - prey.f_19790_, prey.m_20186_() - prey.f_19791_, prey.m_20189_() - prey.f_19792_};
            double[] led = ReaperTactics.lead(new double[] {target.f_82479_, target.f_82480_, target.f_82481_}, moving, target.m_82554_(hand),
                AgentRules.DAWN_SHOT_SPEED);
            target = new Vec3(led[0], led[1], led[2]);
        }
        Vec3 along = target.m_82546_(hand).m_82541_();
        Vec3 forward = UkakuFire.horizontal(along);
        hand = hand.m_82520_(-forward.f_82481_ * 0.30, 0.0, forward.f_82479_ * 0.30);
        along = target.m_82546_(hand).m_82541_();
        Vec3 tip = hand.m_82549_(along.m_82490_(Quinque.DAWN_TIP));
        if (level.m_45547_(new ClipContext(hand, tip, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, agent)).m_6662_() != HitResult.Type.MISS) {
            return;
        }
        if (mind.volley) {
            // 2.0.48: five from the one point, opening into a ring and closing on where the quarry will be.
            CrystalShots.volley(agent, level, tip, along, target.m_82554_(tip), body -> foe(agent, body),
                AgentRules.DAWN_SHOT_DAMAGE * com.hex.kagune.control.quinque.DawnVolley.SHARE);
            UkakuFire.sound(level, tip, "hexkagune:sfx.thunder", 0.9F, 1.25F);
        } else {
            CrystalShots.lightning(agent, level, tip, along, body -> foe(agent, body));
        }
        agent.shot(now);
        UkakuFire.sound(level, tip, "hexkagune:sfx.thunder", 0.65F, 1.45F);
    }

    // ---------------------------------------------------------------- 2.0.49: the White Reaper's tactics

    /** True if {@code prey} is guarding in front of {@code agent}: a Rinkaku's block up and facing him, or a shield raised. */
    private static boolean guarding(LivingEntity prey, CcgAgent agent) {
        if (!(prey instanceof ServerPlayer player)) {
            return false;
        }
        if (player.m_21254_()) {
            return true;
        }
        if (!RinkakuBlockServer.up(player)) {
            return false;
        }
        Vec3 look = player.m_20154_();
        Vec3 to = agent.m_20182_().m_82546_(player.m_20182_());
        return com.hex.kagune.control.RinkakuBlock.covers(new double[] {look.f_82479_, look.f_82480_, look.f_82481_},
            new double[] {to.f_82479_, to.f_82480_, to.f_82481_});
    }

    /** His read of the fight ({@link ReaperTactics#choose}) and the play he makes on it. True if he made one (nothing else this tick). */
    private static boolean tactics(CcgAgent agent, Mind mind, LivingEntity prey, ServerLevel level, long now) {
        if (mind.rank != AgentRules.Rank.RANK_10 || mind.kind != Quinque.DAWN) {
            return false;
        }
        boolean busy = mind.striking || mind.kickAt != Long.MIN_VALUE || mind.aimAt != Long.MIN_VALUE || mind.groundAt != Long.MIN_VALUE
            || mind.soarAt != Long.MIN_VALUE;
        if (now % 2L != 0L && !mind.strung) {
            return false; // he reads it every other tick (at once after his string lands)
        }
        double distance = Math.sqrt(prey.m_20280_(agent));
        long last = KaguneClash.lastSwing(prey);
        boolean whiffed = last != Long.MIN_VALUE && now - last >= 4L && now - last <= 14L && mind.hurtAt < last;
        boolean airborne = !prey.m_20096_() && !prey.m_20069_() && !prey.m_20159_();
        ReaperTactics.Read read = new ReaperTactics.Read(distance, AgentRules.reach(mind.kind), agent.m_142582_(prey), airborne,
            PaleStun.stunned(prey), whiffed, guarding(prey, agent), prey.m_21223_() / Math.max(1.0F, prey.m_21233_()),
            agent.m_21223_() / Math.max(1.0F, agent.m_21233_()), mind.strung, agent.m_20096_(), now >= mind.flipReadyAt,
            now >= mind.dashReadyAt, now >= mind.lightningReadyAt, now >= mind.soarReadyAt, busy);
        ReaperTactics.Play play = ReaperTactics.choose(read, agent.m_217043_().m_188500_());
        if (!busy) {
            mind.strung = false;
        }
        Vec3 toward = UkakuFire.horizontal(prey.m_20182_().m_82546_(agent.m_20182_()));
        switch (play) {
            case VAULT -> {
                // Clean over the guard, landing behind it, his string the moment he is down.
                flip(agent, mind, PaleRules.Flip.FORWARD, toward, NpcRules.flipSpeed(distance + ReaperTactics.VAULT_PAST),
                    AgentRules.FLIP_UP * ReaperTactics.VAULT_LIFT, prey, level, now);
                mind.readyAt = now;
                return true;
            }
            case SKY_SHOT -> {
                mind.aimAt = now;
                mind.aimTarget = prey.m_19879_();
                mind.aimTicks = ReaperTactics.SKY_AIM;
                mind.volley = distance >= AgentRules.VOLLEY_MIN;
                mind.lightningReadyAt = now + ReaperTactics.SKY_EVERY;
                QuinqueServer.opens(agent.m_21205_(), now, now + ReaperTactics.SKY_AIM);
                agent.m_21573_().m_26573_();
                agent.swing(CcgAgent.AIM_DAWN, now);
                return true;
            }
            case PUNISH -> {
                double reach = AgentRules.reach(mind.kind);
                if (distance > ReaperTactics.PUNISH_FLIP && now >= mind.flipReadyAt) {
                    flip(agent, mind, PaleRules.Flip.FORWARD, toward, NpcRules.flipSpeed(distance - reach), AgentRules.FLIP_UP, prey, level, now);
                } else if (now >= mind.dashReadyAt && clear(agent, level, toward)) {
                    dash(agent, mind, toward, 0, level, now);
                } else {
                    return false;
                }
                mind.readyAt = now;
                return true;
            }
            case SOAR -> {
                Vec3 away = toward.m_82490_(-1.0);
                if (!open(level, agent.m_20185_() + away.f_82479_ * 1.2, agent.m_20186_() + 0.2, agent.m_20189_() + away.f_82481_ * 1.2)
                    || !open(level, agent.m_20185_(), agent.m_20186_() + agent.m_20206_() + 0.8, agent.m_20189_())) {
                    return false; // a wall at his back or a ceiling over him
                }
                startSoar(agent, mind, prey, away, level, now);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    /** A backflip launched as Up We Go launches one (ReaperTactics.soar): the first tick of its ramp now. */
    private static void startSoar(CcgAgent agent, Mind mind, LivingEntity prey, Vec3 away, ServerLevel level, long now) {
        end(agent, mind);
        agent.m_21573_().m_26573_();
        mind.soarAt = now;
        mind.soarWay = away;
        mind.soarTarget = prey.m_19879_();
        mind.soarFired = false;
        mind.soarReadyAt = now + ReaperTactics.SOAR_EVERY;
        mind.flipAt = now;
        mind.push = null;
        mind.flipReadyAt = now + AgentRules.REAPER_FLIP_EVERY;
        mind.dashReadyAt = Math.max(mind.dashReadyAt, now + NpcRules.FLIP_BUSY);
        double[] launch = ReaperTactics.soar(0);
        agent.m_20256_(new Vec3(away.f_82479_ * launch[0], launch[1], away.f_82481_ * launch[0]));
        agent.f_19864_ = true;
        agent.f_19789_ = 0.0F;
        face(agent, prey);
        agent.flip(PaleRules.Flip.BACK.ordinal() + CcgAgent.SOARING, now);
        UkakuFire.sound(level, agent.m_20182_(), "entity.player.attack.sweep", 0.5F, 0.6F);
        UkakuFire.sound(level, agent.m_20182_(), "hexkagune:sfx.dash", 0.8F, 0.8F);
    }

    /**
     * The soar under way: its launch built up over its ramp, then (from the top of it) Dawn aimed down at his quarry while he hangs there a
     * moment, and the volley (a single ball from close) let go. Over once he is down. True while it lasts (nothing else this tick).
     */
    private static boolean soar(CcgAgent agent, Mind mind, ServerLevel level, long now) {
        if (mind.soarAt == Long.MIN_VALUE) {
            return false;
        }
        long age = now - mind.soarAt;
        boolean down = age > 2L && agent.m_20096_();
        if (down || age > ReaperTactics.LONGEST || age < 0L || PaleStun.stunned(agent) || !agent.m_6084_()) {
            mind.soarAt = Long.MIN_VALUE;
            if (mind.aimAt != Long.MIN_VALUE) {
                cancelLightning(agent, mind, now);
            }
            mind.aimTicks = AgentRules.DAWN_AIM_TICKS;
            if (down) {
                mind.readyAt = Math.max(mind.readyAt, now + 3L);
            }
            return false;
        }
        agent.m_21573_().m_26573_();
        agent.f_19789_ = 0.0F;
        if (age < ReaperTactics.RAMP) {
            double[] launch = ReaperTactics.soar((int)age);
            agent.m_20256_(new Vec3(mind.soarWay.f_82479_ * launch[0], launch[1], mind.soarWay.f_82481_ * launch[0]));
            agent.f_19864_ = true;
            return true;
        }
        Entity found = level.m_6815_(mind.soarTarget);
        if (!(found instanceof LivingEntity prey) || !prey.m_6084_()) {
            return true; // nobody to shoot at: he comes down
        }
        face(agent, prey);
        Vec3 eye = agent.m_146892_();
        Vec3 to = GhoulNpcBrain.centre(prey).m_82546_(eye);
        agent.m_146926_((float)Math.max(-60.0, Math.min(75.0, -Math.toDegrees(Math.atan2(to.f_82480_, Math.sqrt(to.m_165925_()))))));
        if (mind.aimAt == Long.MIN_VALUE && !mind.soarFired && agent.m_20184_().f_82480_ <= ReaperTactics.APEX && agent.m_142582_(prey)) {
            // The top of it: Dawn down at them, its point opening.
            mind.aimAt = now;
            mind.aimTarget = prey.m_19879_();
            mind.aimTicks = ReaperTactics.AIR_AIM;
            mind.volley = Math.sqrt(prey.m_20280_(agent)) >= com.hex.kagune.control.quinque.DawnVolley.NEAREST;
            QuinqueServer.opens(agent.m_21205_(), now, now + ReaperTactics.AIR_AIM);
            agent.swing(CcgAgent.AIM_DAWN, now);
        }
        if (mind.aimAt != Long.MIN_VALUE) {
            // He hangs there while he aims, his drift all but gone.
            Vec3 v = agent.m_20184_();
            agent.m_20256_(new Vec3(v.f_82479_ * ReaperTactics.HANG_DRIFT, Math.max(v.f_82480_, ReaperTactics.HANG_FALL), v.f_82481_ * ReaperTactics.HANG_DRIFT));
            agent.f_19864_ = true;
            if (ago(now, mind.aimAt) >= mind.aimTicks) {
                mind.soarFired = true;
                release(agent, mind, prey, level, now);
            }
        }
        return true;
    }

    /** 4.1.25: his hand-to-hand broken off (stunned, the fight over): no strike under way, his swing let go. */
    private static void cancelBrawl(CcgAgent agent, Mind mind, long now) {
        if (mind.brawlStrike >= 0) {
            mind.brawlStrike = -1;
            mind.brawlAt = Long.MIN_VALUE;
            agent.swing(-1, now);
        }
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

    /**
     * 4.1.25 (the owner: "use both styles of fighting that we got. Both the Kickmaster and the normal one"): the White Reaper's hand-to-hand.
     * Pressed close, now and then, he goes in with Kick Master's string or the Speed Blitz (half the time each: AgentRules.brawlStyle),
     * strike by strike as a player throws them (PaleStrike's ticks: its blink, its impact, its chain), each chained into the next while his
     * quarry is in reach, the finisher ending it; Dawn carried clear meanwhile (drawn by AgentPose.brawl). True while a strike is under way.
     */
    private static boolean brawl(CcgAgent agent, Mind mind, LivingEntity prey, ServerLevel level, long now) {
        if (mind.rank != AgentRules.Rank.RANK_10 || mind.kind != Quinque.DAWN) {
            return false;
        }
        if (mind.brawlStrike >= 0) {
            com.hex.kagune.control.pale.PaleStrike strike = com.hex.kagune.control.pale.PaleStrike.values()[mind.brawlStrike];
            long age = ago(now, mind.brawlAt);
            agent.m_21573_().m_26573_();
            Entity seen = level.m_6815_(mind.brawlTarget);
            LivingEntity target = seen instanceof LivingEntity body && body.m_6084_() ? body : null;
            if (target != null && age < strike.impact) {
                face(agent, target);
            }
            if (age == com.hex.kagune.control.pale.PaleStrike.BLINK_TICK && strike.blink != com.hex.kagune.control.pale.PaleStrike.Blink.NONE
                && target != null) {
                brawlBlink(agent, target, level, strike.blink);
            }
            if (age == strike.impact && target != null) {
                brawlLands(agent, target, strike, level, now);
                if (mind.brawlStrike < 0) {
                    return true; // 4.1.37: it met a blow of theirs (KaguneClash): thrown apart, his string broken off
                }
            }
            com.hex.kagune.control.pale.PaleStrike[] string = com.hex.kagune.control.pale.PaleStrike.of(mind.brawlStyle);
            boolean chain = age >= strike.chain && target != null && !strike.finisher() && mind.brawlIndex + 1 < string.length
                && target.m_20280_(agent) <= AgentRules.REAPER_BRAWL_NEAR * AgentRules.REAPER_BRAWL_NEAR * 1.3;
            if (chain) {
                mind.brawlIndex++;
                beginStrike(agent, mind, string[mind.brawlIndex], now);
            } else if (age >= strike.duration) {
                mind.brawlStrike = -1;
                mind.brawlAt = Long.MIN_VALUE;
                mind.brawlReadyAt = now + AgentRules.REAPER_BRAWL_REST;
                agent.swing(-1, now);
            }
            return true;
        }
        if (mind.striking || mind.kickAt != Long.MIN_VALUE || mind.aimAt != Long.MIN_VALUE || mind.groundAt != Long.MIN_VALUE
            || now < mind.brawlReadyAt || now < mind.readyAt || !agent.m_20096_()
            || prey.m_20280_(agent) > AgentRules.REAPER_BRAWL_NEAR * AgentRules.REAPER_BRAWL_NEAR || !agent.m_142582_(prey)
            || agent.m_217043_().m_188500_() >= AgentRules.REAPER_BRAWL_CHANCE) {
            return false;
        }
        mind.brawlStyle = AgentRules.brawlStyle(agent.m_217043_().m_188500_());
        mind.brawlIndex = 0;
        mind.brawlTarget = prey.m_19879_();
        beginStrike(agent, mind, com.hex.kagune.control.pale.PaleStrike.of(mind.brawlStyle)[0], now);
        return true;
    }

    /** 4.1.25: one strike of his string begins (every client draws it from the swing's code: AgentClient, AgentPose.brawl). */
    private static void beginStrike(CcgAgent agent, Mind mind, com.hex.kagune.control.pale.PaleStrike strike, long now) {
        mind.brawlStrike = strike.ordinal();
        mind.brawlAt = now;
        agent.swing(CcgAgent.STRIKE + strike.ordinal(), now);
        // 4.1.37: on its way it can be met (KaguneClash), as hard as it would hit.
        Entity seen = agent.m_9236_().m_6815_(mind.brawlTarget);
        Vec3 aim = seen != null ? UkakuFire.horizontal(seen.m_20182_().m_82546_(agent.m_20182_())) : null;
        long began = now;
        int ordinal = strike.ordinal();
        KaguneClash.swing(agent, now, strike.impact, (strike.kick() ? AgentRules.REAPER_BRAWL_KICK_LANDS : AgentRules.REAPER_BRAWL_LANDS) + 0.5, aim,
            mind.brawlTarget, com.hex.kagune.control.ClashPower.agent(AgentRules.power(mind.rank, mind.superior), AgentRules.REAPER_BRAWL_DAMAGE * strike.damage),
            () -> mind.brawlAt == began && mind.brawlStrike == ordinal && ago(agent.m_9236_().m_46467_(), began) < strike.impact,
            () -> brawlMet(agent, mind, began));
    }

    /** 4.1.37: a strike of his met another (KaguneClash: both thrown apart): nothing of it lands, his string over, a rest before the next. */
    private static void brawlMet(CcgAgent agent, Mind mind, long began) {
        if (mind.brawlAt != began) {
            return;
        }
        long now = agent.m_9236_().m_46467_();
        mind.brawlStrike = -1;
        mind.brawlAt = Long.MIN_VALUE;
        mind.brawlReadyAt = now + AgentRules.REAPER_BRAWL_REST;
        agent.swing(-1, now);
    }

    /** 4.1.25: he vanishes and is at its side ({@code SIDE}) or square in front of it ({@code FRONT}), already striking. */
    private static void brawlBlink(CcgAgent agent, LivingEntity target, ServerLevel level, com.hex.kagune.control.pale.PaleStrike.Blink kind) {
        Vec3 from = agent.m_20182_();
        Vec3 facing = UkakuFire.horizontal(target.m_20182_().m_82546_(from));
        Vec3 right = new Vec3(-facing.f_82481_, 0.0, facing.f_82479_);
        double stand = (target.m_20205_() + agent.m_20205_()) * 0.5 + 0.7;
        Vec3 to = kind == com.hex.kagune.control.pale.PaleStrike.Blink.SIDE ? target.m_20182_().m_82549_(right.m_82490_(stand))
            : target.m_20182_().m_82546_(facing.m_82490_(stand));
        if (!level.m_45756_(agent, agent.m_20191_().m_82386_(to.f_82479_ - from.f_82479_, to.f_82480_ - from.f_82480_, to.f_82481_ - from.f_82481_))) {
            return;
        }
        PaleBlitz.fxAround(agent, agent.m_19879_(), com.hex.kagune.control.network.S2CPaleFx.BLINK, from, to.m_82546_(from), 1.0F, 0);
        agent.m_6021_(to.f_82479_, to.f_82480_, to.f_82481_);
        face(agent, target);
        UkakuFire.sound(level, to, "entity.illusioner.mirror_move", 0.6F, 1.5F);
    }

    /** 4.1.25: a strike of his lands if its quarry is still in its reach: hurt, thrown back, to a side and up as the strike throws it. */
    private static void brawlLands(CcgAgent agent, LivingEntity target, com.hex.kagune.control.pale.PaleStrike strike, ServerLevel level, long now) {
        // 4.1.37: unless it meets a blow of theirs coming back at him: then both are thrown apart and nothing lands.
        if (KaguneClash.meets(agent, target, now)) {
            return;
        }
        KaguneClash.landed(agent);
        double gap = Math.sqrt(target.m_20280_(agent)) - (target.m_20205_() + agent.m_20205_()) * 0.5;
        if (gap > (strike.kick() ? AgentRules.REAPER_BRAWL_KICK_LANDS : AgentRules.REAPER_BRAWL_LANDS)) {
            UkakuFire.sound(level, agent.m_20182_(), "entity.player.attack.sweep", 0.5F, 1.3F);
            return;
        }
        Vec3 facing = UkakuFire.horizontal(target.m_20182_().m_82546_(agent.m_20182_()));
        Vec3 right = new Vec3(-facing.f_82481_, 0.0, facing.f_82479_);
        Vec3 knock = facing.m_82490_(strike.back).m_82549_(right.m_82490_(strike.sideways)).m_82520_(0.0, strike.up, 0.0);
        Vec3 at = GhoulNpcBrain.centre(target).m_82546_(facing.m_82490_(target.m_20205_() * 0.5));
        WoundHits.expect(target, WoundBook.Blow.BLUNT, at, facing, 0.0);
        target.f_19802_ = 0;
        if (target.m_6469_(agent.m_269291_().m_269333_(agent), AgentRules.REAPER_BRAWL_DAMAGE * strike.damage)) {
            target.m_20256_(target.m_20184_().m_82490_(0.3).m_82549_(knock));
            target.f_19864_ = true;
        }
        PaleBlitz.fxAround(target, agent.m_19879_(), com.hex.kagune.control.network.S2CPaleFx.IMPACT, at, knock, strike.finisher() ? 1.6F : 1.0F,
            strike.ordinal());
        UkakuFire.sound(level, at, strike.finisher() ? "entity.player.attack.strong" : "entity.player.attack.knockback", 0.9F, 1.0F);
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
    static void face(CcgAgent agent, Entity prey) {
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
    static void close(CcgAgent agent, Mind mind, CcgAgent partner, LivingEntity prey, ServerLevel level, long now) {
        double reach = reach(mind);
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
            if (mind.arm != null) {
                AgentArms.begin(agent, mind, prey, level, now);
            } else {
                begin(agent, mind, prey, level, now);
            }
        }
    }

    /** 3.0.0: true if it keeps its distance and shoots (a regular launcher; never one with a kagune-made weapon). */
    static boolean ranged(Mind mind) {
        return mind.arm == null && mind.kind.ranged;
    }

    /** 3.0.0: the quinque it fights as (its sides, its parries): a kagune-made weapon fights as a blade does. */
    static Quinque fighting(Mind mind) {
        return mind.arm != null ? Quinque.BLADE : mind.kind;
    }

    /** 3.0.0: how far its blows reach (its string's first, with a kagune-made weapon). */
    static double reach(Mind mind) {
        return mind.arm != null ? ArmRules.reach(mind.arm, 0, mind.master) : AgentRules.reach(mind.kind);
    }

    /** Its partner's mind if the two of them fight {@code prey} together (null: it fights on its own). */
    static Mind together(Mind mind, CcgAgent partner, LivingEntity prey) {
        if (partner == null || !partner.m_6084_() || partner.mind == null || partner.mind.prey != prey.m_19879_()) {
            return null;
        }
        return partner.mind;
    }

    /** True if it holds the front against the prey it fights with its partner ({@code theirs}); false, it goes round to the back. */
    private static boolean front(Mind mind, Mind theirs) {
        return AgentRules.front(mind.leader, fighting(mind), fighting(theirs), mind.leader ? mind.swapped : theirs.swapped);
    }

    /**
     * The leader keeps the pair's sides: once the prey has been turned on the one behind it (and its back on the one in front)
     * {@link AgentRules#SWAP_AFTER} ticks, with the one behind close enough to fight it, the two swap.
     */
    private static void sides(CcgAgent agent, Mind mind, CcgAgent partner, LivingEntity prey, long now) {
        Mind theirs = together(mind, partner, prey);
        if (!mind.leader || theirs == null || ranged(mind) || ranged(theirs)) {
            mind.turnedSince = Long.MIN_VALUE;
            return;
        }
        boolean mine = front(mind, theirs);
        CcgAgent ahead = mine ? agent : partner;
        CcgAgent behind = mine ? partner : agent;
        Vec3 look = prey.m_20154_();
        boolean close = behind.m_20280_(prey) <= Math.pow(reach(mine ? theirs : mind) + 1.5, 2.0);
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
    static double around(Entity body, Entity prey) {
        Vec3 from = body.m_20182_().m_82546_(prey.m_20182_());
        return from.f_82479_ * from.f_82479_ + from.f_82481_ * from.f_82481_ < 1.0E-8 ? 0.0 : Math.atan2(from.f_82481_, from.f_82479_);
    }

    /** The spot {@code at} blocks from {@code prey} at {@code angle} round it (never right on top of it). */
    static Vec3 toward(LivingEntity prey, double angle, double at) {
        double r = Math.max(0.8, at);
        return prey.m_20182_().m_82520_(Math.cos(angle) * r, 0.0, Math.sin(angle) * r);
    }

    /** The next of its string sets out at {@code prey}: a blow on its way (it can be met), landing a little on. */
    private static void begin(CcgAgent agent, Mind mind, LivingEntity prey, ServerLevel level, long now) {
        Quinque q = mind.kind;
        mind.step = Quinque.next(q, mind.step, ago(now, mind.stepAt));
        mind.stepAt = now;
        mind.striking = true;
        mind.parrying = false;
        mind.target = prey.m_19879_();
        int lands = AgentRules.lands(q);
        mind.landsAt = now + lands;
        int token = ++mind.token;
        Vec3 aim = UkakuFire.horizontal(prey.m_20182_().m_82546_(agent.m_20182_()));
        KaguneClash.swing(agent, now, lands, AgentRules.reach(q) + 0.5, aim, prey.m_19879_(), com.hex.kagune.control.ClashPower.agent(AgentRules.power(mind.rank, mind.superior), AgentRules.damage(q, mind.step)), () -> mind.token == token && mind.striking,
            () -> stopped(agent, mind, token));
        agent.swing(mind.step, now);
        Vec3 at = agent.m_20182_().m_82520_(0.0, agent.m_20206_() * 0.6, 0.0);
        boolean heavy = mind.step == q.finisher();
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
    static void stopped(CcgAgent agent, Mind mind, int token) {
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
    static void land(CcgAgent agent, Mind mind, ServerLevel level, long now) {
        Quinque q = mind.kind;
        boolean parry = mind.parrying;
        mind.striking = false;
        mind.parrying = false;
        int step = mind.step;
        mind.readyAt = now + Math.max(0, q.swing(step) - AgentRules.lands(q)) + Math.round(AgentRules.between(q) * mind.rank.pace * (mind.bare ? AgentRules.REAPER_BARE_PACE : 1.0)) // 2.0.17: by its rank
            + (step == q.finisher() ? AgentRules.AFTER_STRING : 0);
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
        if (hit && step == q.finisher()) {
            QuinqueServer.finisher(agent, prey, q, dealt, level);
            mind.strung = mind.rank == AgentRules.Rank.RANK_10; // 2.0.49: his string landed: a soar may follow (ReaperTactics)
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
    static boolean maneuver(CcgAgent agent, Mind mind, LivingEntity prey, ServerLevel level, long now) {
        Quinque q = fighting(mind); // 3.0.0: a kagune-made weapon fights as a blade does (never as a launcher)
        double distance = Math.sqrt(prey.m_20280_(agent));
        boolean flips = agent.m_20096_() && now >= mind.flipReadyAt;
        boolean dashes = agent.m_20096_() && now >= mind.dashReadyAt;
        KaguneClash.Threat threat = KaguneClash.threat(agent, now);
        if (threat != null && threat.began() != mind.answered && !mind.striking) {
            mind.answered = threat.began();
            double roll = agent.m_217043_().m_188500_();
            LivingEntity who = threat.who();
            double gap = Math.sqrt(who.m_20280_(agent));
            if (roll < AgentRules.parry(q) && gap <= reach(mind) + 2.5) {
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
        double reach = q.ranged ? AgentRules.SHOOT_NEAREST : reach(mind);
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
        KaguneClash.swing(agent, now, lands, AgentRules.reach(mind.kind) + 1.5, aim, who.m_19879_(), com.hex.kagune.control.ClashPower.agent(AgentRules.power(mind.rank, mind.superior), AgentRules.damage(mind.kind, 0)), () -> mind.token == token && mind.striking,
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
    static boolean clear(CcgAgent agent, ServerLevel level, Vec3 way) {
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
        mind.zigIn = prey != null && !ranged(mind) && Math.sqrt(prey.m_20280_(agent)) > reach(mind) + 0.5;
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

    /**
     * The Reaper leaves human meat. Regular investigators now and then leave their quinque case (3.0.2: the commoner, the likelier). 2.2.9: any of them, brought down by a player,
     * now and then leaves a field ration (food the cookbook takes: {@link AgentRules#RATIONS}); the Reaper's Dawn is a rare prize now.
     */
    public static void drop(CcgAgent agent, int looting, boolean byPlayer) {
        Mind mind = agent.mind;
        if (byPlayer && agent.m_217043_().m_188500_() < AgentRules.rationChance(looting)) {
            AgentRules.Ration ration = AgentRules.ration(agent.m_217043_().m_188500_());
            ItemStack food = HumanTalk.stack(ration.item(), AgentRules.rations(ration, agent.m_217043_().m_188500_()));
            if (!food.m_41619_()) {
                agent.m_19983_(food);
            }
        }
        // 4.0.0: each piece of its Formal Outfit its own small chance (the owner: "calculated separately for each piece").
        OutfitServer.drop(agent, looting, byPlayer);
        if (reaper(agent)) {
            HumanBrain.drop(agent, looting);
            // 2.0.32: Dawn, shut in its black case (2.2.9: one time in five; 4.1.23: every time he falls, however he falls).
            if (agent.m_217043_().m_188500_() < AgentRules.dawnChance(looting)) {
                agent.m_19983_(new ItemStack(ControlItems.caseOf(Quinque.DAWN), 1));
            }
            return;
        }
        // 3.0.2: the commoner what it fought with, the likelier its case is left (AgentRules.caseChance).
        if (!byPlayer || mind == null || agent.m_217043_().m_188500_() >= AgentRules.caseChance(mind.kind, mind.arm, mind.master, looting)) {
            return;
        }
        // 3.0.6: a kagune-made weapon's own case (the owner: "it had the new spear ... but it dropped the old spear"), a masterwork's as one.
        agent.m_19983_(mind.arm != null ? ControlItems.armCaseStack(mind.arm, mind.master) : new ItemStack(ControlItems.caseOf(mind.kind), 1));
    }

    /** 4.1.23: who struck each White Reaper and when (game ticks; his UUID, then theirs): everyone who did in the last five minutes shares his fall. */
    private static final Map<UUID, Map<UUID, Long>> REAPER_STRUCK = new HashMap<>();
    /** 4.1.23: the advancement every one of them earns. */
    static final String SLAIN_REAPER = "slain_reaper";

    /** 4.1.23: a player's blow on the White Reaper, remembered for his fall (blows older than five minutes forgotten as new ones come). */
    @SubscribeEvent
    public static void reaperStruck(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (!(event.getEntity() instanceof CcgAgent agent) || !(agent.m_9236_() instanceof ServerLevel level) || !reaper(agent)
            || event.getSource() == null || !(event.getSource().m_7639_() instanceof ServerPlayer player)) {
            return;
        }
        long now = level.m_46467_();
        Map<UUID, Long> struck = REAPER_STRUCK.computeIfAbsent(agent.m_20148_(), id -> new HashMap<>());
        struck.values().removeIf(at -> !AgentRules.sharesReaper(at, now));
        struck.put(player.m_20148_(), now);
    }

    /**
     * 4.1.23 (the owner: "When both players are kill him, they get like a special achievement from our mod called like Slain Reaper ... make
     * sure that both players get it"): the White Reaper fallen: whoever brought him down and every player who struck him in the last five
     * minutes earns Slain Reaper, and a ghoul among them his mastery.
     */
    static void reaperSlain(CcgAgent agent, ServerLevel level, net.minecraft.world.damagesource.DamageSource source) {
        long now = level.m_46467_();
        Map<UUID, Long> struck = REAPER_STRUCK.remove(agent.m_20148_());
        java.util.Set<UUID> who = new java.util.LinkedHashSet<>();
        if (source != null && source.m_7639_() instanceof ServerPlayer killer) {
            who.add(killer.m_20148_());
        }
        if (struck != null) {
            struck.forEach((id, at) -> {
                if (AgentRules.sharesReaper(at, now)) {
                    who.add(id);
                }
            });
        }
        int mastery = AgentRules.mastery(AgentRules.Rank.RANK_10);
        for (UUID id : who) {
            ServerPlayer player = level.m_7654_().m_6846_().m_11259_(id);
            if (player == null) {
                continue;
            }
            net.minecraft.advancements.Advancement slain = level.m_7654_().m_129889_()
                .m_136041_(net.minecraft.resources.ResourceLocation.m_214293_("hexkagune", SLAIN_REAPER));
            if (slain != null) {
                player.m_8960_().m_135988_(slain, "slain");
            } else {
                com.hex.kagune.KaguneMod.LOGGER.info("HexKagune: the advancement hexkagune:" + SLAIN_REAPER + " is missing");
            }
            if (KaguneProfile.isUnlocked(player)) {
                KaguneProfile.addMastery(player, mastery);
                com.hex.kagune.network.KaguneNetwork.broadcast(player);
                player.m_5661_(Component.m_237113_("\u00a7c+" + mastery + " mastery \u00a77(the White Reaper brought down)"), true);
            }
        }
    }

    /** 2.2.9: an investigator brought down by a ghoul: mastery for it, by its rank ({@link AgentRules#mastery}). */
    @SubscribeEvent
    public static void slain(LivingDeathEvent event) {
        if (event.getEntity() instanceof CcgAgent fallen && fallen.m_9236_() instanceof ServerLevel at && reaper(fallen)) {
            reaperSlain(fallen, at, event.getSource());
            return;
        }
        if (!(event.getEntity() instanceof CcgAgent agent) || !(agent.m_9236_() instanceof ServerLevel)
            || !(event.getSource().m_7639_() instanceof net.minecraft.server.level.ServerPlayer player) || !KaguneProfile.isUnlocked(player)) {
            return;
        }
        AgentRules.Rank rank = agent.mind != null ? agent.mind.rank : AgentRules.Rank.byOrdinal(agent.getPersistentData().m_128451_(RANK_LEVEL));
        int mastery = AgentRules.mastery(rank);
        KaguneProfile.addMastery(player, mastery);
        com.hex.kagune.network.KaguneNetwork.broadcast(player);
        player.m_5661_(net.minecraft.network.chat.Component.m_237113_("\u00a7c+" + mastery + " mastery \u00a77(an investigator brought down)"), true);
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
            if (agent.mind.finishing) {
                return; // 4.1.31: his end's own last blow
            }
            if (agent.mind.shedAt != Long.MIN_VALUE || agent.mind.endAt != Long.MIN_VALUE) {
                event.setCanceled(true); // 4.1.28: taking his jacket off: nothing lands on him; 4.1.31: nor in his end
                return;
            }
            event.setAmount(event.getAmount() * AgentRules.REAPER_TAKEN);
            if (!agent.mind.bare) {
                // 4.1.28: never past half while the jacket is on (the blow that gets there starts his second phase).
                float health = agent.m_21223_();
                event.setAmount(AgentRules.shedClamp(health, event.getAmount()));
                if (health - event.getAmount() <= AgentRules.REAPER_SHED_HEALTH + 1.0E-3F) {
                    agent.mind.shedPending = true;
                }
            } else if (agent.m_21223_() - event.getAmount() <= AgentRules.REAPER_END_HEALTH) {
                // 4.1.31: with his blade, the blow that would bring him this low begins his end (he is left at 1; his end takes him).
                event.setAmount(AgentRules.endClamp(agent.m_21223_(), event.getAmount()));
                agent.mind.endAt = now;
                agent.mind.endSource = event.getSource();
                agent.mind.bladeAt = Long.MIN_VALUE;
                agent.mind.rushUntil = Long.MIN_VALUE;
                agent.mind.dodgeUntil = Long.MIN_VALUE;
                agent.swing(CcgAgent.END, now);
            }
        }
        if (!(event.getSource().m_7639_() instanceof LivingEntity attacker) || ccg(attacker) || attacker == agent) {
            return;
        }
        if (attacker instanceof Player player && player.m_7500_()) {
            return; // (a creative player testing them)
        }
        if (attacker instanceof ServerPlayer member && GangServer.ccgStance(member) == GangServer.OWN_GANG && !GangServer.turned(member)) {
            GangServer.struckOwn(member, now); // 4.1.71: one of their own's blow: never held against them (their reputation down a little)
            return;
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
        GhoulNpcBrain.called(attacker, agent, until); // 4.1.46: and the CCG's ghoul division near
    }

    /**
     * 4.1.46: true for the CCG's own: an investigator, or one of its ghoul division (a ghoul NPC of {@code Gang.CCG_DIVISION}, read off its
     * saved identity: on the server). Never attacked by the CCG; never attacking it.
     */
    public static boolean ccg(Entity body) {
        return body instanceof CcgAgent || body instanceof GhoulNpc ghoul && ghoul.identity() != null && ghoul.identity().ccg();
    }

    /** 4.1.46: every investigator within {@link GhoulNpcBrain#GANG_CALL} of {@code near} goes for {@code attacker} (one of the division was struck). */
    static void called(LivingEntity attacker, LivingEntity near, long until) {
        if (ccg(attacker) || attacker instanceof Player player && player.m_7500_()) {
            return;
        }
        for (CcgAgent other : near.m_9236_().m_6443_(CcgAgent.class, near.m_20191_().m_82400_(GhoulNpcBrain.GANG_CALL), a -> a.m_6084_() && a.mind != null)) {
            mark(other.mind, attacker.m_20148_(), until);
            other.mind.preySought = Long.MIN_VALUE;
        }
    }

    /** 4.1.46: true if an investigator within 48 blocks of {@code near} holds something against {@code player} (marked, struck). */
    static boolean marked(LivingEntity near, ServerPlayer player, long now) {
        for (CcgAgent other : near.m_9236_().m_6443_(CcgAgent.class, near.m_20191_().m_82400_(48.0), a -> a.m_6084_() && a.mind != null)) {
            Long until = other.mind.grudges.get(player.m_20148_());
            if (until != null && until > now) {
                return true;
            }
        }
        return false;
    }

    /**
     * True if {@code player} shows what they are (as an investigator tells): their kagune out, a final kakuja, their kakugan unmasked or
     * their eyes on (4.1.46: shared with the CCG's ghoul division).
     */
    static boolean shows(ServerPlayer player) {
        FinalSight sight = Sightings.seen(player);
        return KaguneProfile.isActive(player) || sight.any() || GhoulServer.ghoul(player) && !Sightings.masked(player)
            || Sightings.last(player) != com.hex.kagune.control.npc.Display.NONE;
    }

    /**
     * Investigators never hurt each other (a stray shard, a finisher's sweep). 4.1.37: and nothing lands on the White Reaper while he
     * changes or ends ({@link #untouchable}): cancelled here, before vanilla's hurt does anything (its knockback, red flash and sound come
     * after the hurt event, which only stopped the damage); {@code /kill} and the void still take him.
     */
    @SubscribeEvent
    public static void attacked(LivingAttackEvent event) {
        if (ccg(event.getEntity()) && ccg(event.getSource().m_7639_())) {
            event.setCanceled(true); // 4.1.46: the CCG's ghoul division is the CCG's too (either way)
            return;
        }
        if (untouchable(event.getEntity()) && !com.hex.kagune.control.GhoulTank.bypasses(event.getSource().m_19385_())) {
            event.setCanceled(true);
        }
    }
}
