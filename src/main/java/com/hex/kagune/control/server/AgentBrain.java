package com.hex.kagune.control.server;

import com.hex.kagune.control.ControlItems;
import com.hex.kagune.control.npc.AgentRules;
import com.hex.kagune.control.npc.CcgAgent;
import com.hex.kagune.control.npc.ChildNpc;
import com.hex.kagune.control.npc.EtoNpc;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
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
        /** A finisher just landed and it means to flip clear of it. */
        boolean clear;
        /** Its feet: its flip and when, its push; its dash, when, which way; when it may flip and dash again. */
        long flipAt = Long.MIN_VALUE;
        Vec3 push;
        long dashAt = Long.MIN_VALUE;
        Vec3 dashWay = Vec3.f_82478_;
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
        boolean rounding;
        boolean swapped;
        long turnedSince = Long.MIN_VALUE;
    }

    /** Its partner's id in its saved data (who it is: {@link CcgAgent#KEY}, packed). */
    static final String PARTNER = CcgAgent.KEY + "Partner";

    private static final String[] SENIOR = {"First Class Investigator", "Senior Investigator", "Associate Special Class Investigator"};
    private static final String[] JUNIOR = {"Rank 3 Investigator", "Rank 2 Investigator", "Rank 1 Investigator"};

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
        } else {
            mind.look = (int)Math.floor(agent.m_217043_().m_188500_() * AgentRules.LOOKS.length);
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
        agent.dress(mind.look, mind.kind);
        int rank = Math.floorMod(agent.m_20148_().hashCode(), 3);
        agent.m_6593_(Component.m_237113_((mind.leader ? SENIOR : JUNIOR)[rank]));
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
            return;
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
        if (partner != null || ago(now, mind.partnerSeen) < AgentRules.PARTNER_LOST) {
            return false;
        }
        mind.leader = true;
        if (mind.prey != 0) {
            return false;
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
        if (ago(now, mind.hurtAt) > 200L && now % 40L == 0L && agent.m_21223_() < agent.m_21233_()) {
            agent.m_5634_(1.0F);
        }
    }

    // ---------------------------------------------------------------- what it goes for

    private static LivingEntity prey(CcgAgent agent, Mind mind, CcgAgent partner, ServerLevel level, long now) {
        if (now % 10L == Math.floorMod(agent.m_19879_(), 10)) {
            reveal(agent, mind, partner, level, now); // (kept up in a fight too: a kagune still out keeps them marked)
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
        int id = current == null ? 0 : current.m_19879_();
        if (id != mind.prey) {
            mind.prey = id;
            agent.m_6710_(current);
        }
        return current;
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
        if (body instanceof GhoulNpc) {
            return current || distance <= AgentRules.GHOUL_RANGE;
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
            if (!AgentRules.revealed(KaguneProfile.isUnlocked(player), KaguneProfile.isActive(player), distance, agent.m_142582_(player))) {
                continue;
            }
            Long was = mind.grudges.get(player.m_20148_());
            if (was == null || was <= now) {
                player.m_5661_(Component.m_237113_("§cThe CCG has seen your kagune."), true);
            }
            mark(mind, player.m_20148_(), now + AgentRules.MARKED);
            if (partner != null && partner.mind != null) {
                mark(partner.mind, player.m_20148_(), now + AgentRules.MARKED);
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
        if (mind.open && ago(now, mind.foughtAt) > AgentRules.CALM && QuinqueServer.hand(agent, level, agent.m_21205_(), mind.kind, false)) {
            mind.open = false;
        }
        mind.shots = 0;
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
        KaguneClash.swing(agent, now, lands, AgentRules.reach(q) + 0.5, aim, prey.m_19879_(), () -> mind.token == token && mind.striking,
            () -> stopped(agent, mind, token));
        agent.swing(mind.step, now);
        Vec3 at = agent.m_20182_().m_82520_(0.0, agent.m_20206_() * 0.6, 0.0);
        boolean heavy = mind.step == Quinque.FINISHER;
        UkakuFire.sound(level, at, heavy ? "hexkagune:sfx.blade_heavy" : "hexkagune:sfx.blade_arc", 0.6F, q == Quinque.MACE ? 0.75F : 1.1F
            + 0.15F * (float)agent.m_217043_().m_188500_());
        // The spear's and the katana's blows step in; the spear's thrust drives in hard.
        double step = q == Quinque.SPEAR ? (heavy ? 0.75 : 0.4) : q == Quinque.KATANA ? (heavy ? 0.55 : 0.25) : heavy ? 0.3 : 0.12;
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
        mind.readyAt = now + Math.max(0, q.swing - AgentRules.lands(q)) + AgentRules.between(q) + (step == Quinque.FINISHER ? AgentRules.AFTER_STRING : 0);
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
        float dealt = AgentRules.damage(q, step);
        Vec3 centre = GhoulNpcBrain.centre(prey);
        WoundHits.expect(prey, q == Quinque.SPEAR ? WoundBook.Blow.STAB : q == Quinque.MACE ? WoundBook.Blow.CRUSH : q == Quinque.LAUNCHER ? WoundBook.Blow.BLUNT
            : WoundBook.Blow.SLASH, centre.m_82546_(flat.m_82490_(0.3)), flat, 0.0);
        prey.f_19802_ = 0;
        if (prey.m_6469_(agent.m_269291_().m_269333_(agent), dealt) && step == Quinque.FINISHER) {
            QuinqueServer.finisher(agent, prey, q, dealt, level);
            mind.clear = agent.m_217043_().m_188500_() < 0.4; // and now and then it flips clear after it
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
        if (mind.dashAt != Long.MIN_VALUE && now - mind.dashAt < AgentRules.DASH_TICKS) {
            Vec3 velocity = agent.m_20184_();
            agent.m_20256_(new Vec3(mind.dashWay.f_82479_ * AgentRules.DASH_SPEED, velocity.f_82480_, mind.dashWay.f_82481_ * AgentRules.DASH_SPEED));
            agent.f_19864_ = true;
            agent.m_21573_().m_26573_();
            return true;
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
        if (dashes && !q.ranged && distance <= reach + 0.5 && now < mind.readyAt - 4L && roll < 0.06) {
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
        if (dashes && !mind.rounding && between && near && chance < AgentRules.SIDE_DASH && agent.m_142582_(prey)) {
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
        KaguneClash.swing(agent, now, lands, AgentRules.reach(mind.kind) + 1.5, aim, who.m_19879_(), () -> mind.token == token && mind.striking,
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
        mind.flipReadyAt = now + AgentRules.FLIP_EVERY;
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
        mind.dashReadyAt = now + AgentRules.DASH_EVERY;
        agent.dash(which, now);
        Vec3 velocity = agent.m_20184_();
        agent.m_20256_(new Vec3(way.f_82479_ * AgentRules.DASH_SPEED, Math.max(velocity.f_82480_, 0.05), way.f_82481_ * AgentRules.DASH_SPEED));
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
        CrystalShots.fire(agent, level, from, way, Quinque.SHARD_SPEED, Quinque.SHARD_RANGE, AgentRules.SHARD_DAMAGE, body -> foe(agent, body));
        agent.shot(now);
        UkakuFire.sound(level, from, "block.amethyst_block.hit", 0.55F, 1.5F + 0.15F * (float)agent.m_217043_().m_188500_());
    }

    // ---------------------------------------------------------------- what happens to it

    /** It falls: its case is left behind, all but never (only to a player's hand in its death). */
    public static void drop(CcgAgent agent, int looting, boolean byPlayer) {
        Mind mind = agent.mind;
        if (!byPlayer || mind == null || agent.m_217043_().m_188500_() >= AgentRules.dropChance(looting)) {
            return;
        }
        agent.m_19983_(new ItemStack(ControlItems.caseOf(mind.kind), 1));
    }

    /** It was struck: it and its partner go for whoever did it (and a ghoul who did is marked as one). */
    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof CcgAgent agent) || !(agent.m_9236_() instanceof ServerLevel level) || agent.mind == null) {
            return;
        }
        long now = level.m_46467_();
        agent.mind.hurtAt = now;
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
