package com.hex.kagune.control.server;

import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CNpcWings;
import com.hex.kagune.control.network.S2CUkakuFx;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.WingRules;
import com.hex.kagune.control.ukaku.WingForm;
import com.hex.kagune.control.ukaku.WingRig;
import com.hex.kagune.control.wound.WoundBook;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1.3.19: a companion's Ukaku wings in a fight (Touka's, {@link WingRules}): out of her back the moment a fight finds her, turned to
 * crystal, throwing crystal spikes from their tips (a volley, a fan, the barrage), flown here a tick at a time and caught by the first
 * foe in their way (never her partner, a child, another companion: {@link EtoBrain#foe}); drawn on every client as an Ukaku player's
 * ({@link S2CUkakuFx}'s spike kinds; the wings by {@link S2CNpcWings}). Up close, her eyes' own kit ({@link EtoOwl}). Long out of a
 * fight, her wings go back in.
 */
final class NpcWings {
    /** Spike ids well clear of the players' and the ghouls' shots. */
    private static int nextSpike = (1 << 30) + (1 << 28);
    /** The kinds as the clients draw them: a shot, a fan's, the barrage's (S2CUkakuFx.SPIKE + these). */
    private static final int SHOT = 0;
    private static final int FAN = 2;
    private static final int BARRAGE = 4;

    /** A spike in flight. */
    static final class Spike {
        final int id = nextSpike++;
        int kind;
        Vec3 position;
        Vec3 velocity;
        int life;
        float damage;
        final Set<Integer> hit = new HashSet<>();
    }

    /** Her wings: out and since when, crystal and since when, what they aim at, her volley, fan and barrage, her spikes in flight. */
    static final class State {
        boolean out;
        long outAt = Long.MIN_VALUE;
        boolean crystal;
        long crystalAt = Long.MIN_VALUE;
        int prey;
        long shotAt = Long.MIN_VALUE;
        int shotSide = 1;
        int side = 1;
        long volleyAt = Long.MIN_VALUE;
        long nextShot = Long.MIN_VALUE;
        int shots;
        long fanReady = Long.MIN_VALUE;
        long barrageReady = Long.MIN_VALUE;
        long barrageUntil = Long.MIN_VALUE;
        long foughtAt = Long.MIN_VALUE;
        final List<Spike> spikes = new ArrayList<>();
        boolean dirty;
    }

    private NpcWings() {
    }

    // ---------------------------------------------------------------- every tick

    /** Every tick, in a fight or not: her spikes fly on, and her wings go back in once the fight is long over. */
    static void tick(EtoNpc npc, EtoBrain.Mind mind, ServerLevel level, long now) {
        if (!npc.persona().wings()) {
            return;
        }
        State st = mind.wings;
        for (Iterator<Spike> it = st.spikes.iterator(); it.hasNext(); ) {
            if (!step(npc, it.next(), level, now)) {
                it.remove();
            }
        }
        if (st.out && now - st.foughtAt > WingRules.PUT_AWAY) {
            st.out = false;
            st.outAt = now;
            if (st.crystal) {
                st.crystal = false;
                st.crystalAt = now;
                fx(npc, S2CUkakuFx.CRYSTALLIZE, 0, back(npc), Vec3.f_82478_, 1.0F, 0);
            }
            st.volleyAt = Long.MIN_VALUE;
            st.prey = 0;
            st.dirty = true;
            UkakuFire.sound(level, npc.m_20182_().m_82520_(0.0, 1.2, 0.0), "entity.ender_dragon.flap", 0.5F, 1.6F);
        }
        if (st.dirty) {
            st.dirty = false;
            ControlNetwork.broadcastAround(npc, message(npc, st));
        }
    }

    /** A tick of her fight; false when her eyes' own kit is to go on (too close for her spikes, out of sight, or a move of hers under way). */
    static boolean fight(EtoNpc npc, EtoBrain.Mind mind, LivingEntity target, ServerLevel level, long now) {
        if (!npc.persona().wings()) {
            return false;
        }
        State st = mind.wings;
        st.foughtAt = now;
        if (!st.out) {
            st.out = true;
            st.outAt = now;
            st.dirty = true;
            UkakuFire.sound(level, npc.m_20182_().m_82520_(0.0, 1.2, 0.0), "entity.ender_dragon.flap", 0.8F, 1.4F);
        }
        if (!st.crystal && now - st.outAt >= WingRules.CRYSTAL_AFTER) {
            st.crystal = true;
            st.crystalAt = now;
            st.dirty = true;
            fx(npc, S2CUkakuFx.CRYSTALLIZE, 0, back(npc), Vec3.f_82478_, 1.0F, 1);
            UkakuFire.sound(level, back(npc), "block.amethyst_block.resonate", 1.0F, 1.2F);
        }
        if (st.prey != target.m_19879_()) {
            st.prey = target.m_19879_();
            st.dirty = true;
        }
        if (mind.owl.busy()) {
            return false;
        }
        double gap = EtoOwl.gap(npc, target);
        boolean sees = npc.m_142582_(target);
        if (now < st.barrageUntil) {
            // The barrage: both wings in turn, fast, standing her ground.
            EtoOwl.face(npc, target);
            npc.m_21573_().m_26573_();
            if ((st.barrageUntil - now) % WingRules.BARRAGE_EVERY_TICK == 0L && sees) {
                double jitter = (npc.m_217043_().m_188500_() - 0.5) * 6.0;
                throwSpike(npc, st, target, level, now, BARRAGE, jitter, WingRules.BARRAGE);
            }
            if (now + 1L >= st.barrageUntil) {
                st.volleyAt = Long.MIN_VALUE;
                st.dirty = true;
            }
            return true;
        }
        if (!st.crystal) {
            return false; // her wings still turning: her feet meanwhile
        }
        WingRules.Act act = WingRules.choose(gap, sees, now, st.nextShot, st.fanReady, st.barrageReady, npc.m_217043_().m_188500_());
        if (act == WingRules.Act.NONE) {
            if (st.volleyAt != Long.MIN_VALUE) {
                st.volleyAt = Long.MIN_VALUE;
                st.dirty = true;
            }
            return false;
        }
        EtoOwl.face(npc, target);
        keepDistance(npc, target, gap);
        switch (act) {
            case BARRAGE -> {
                st.barrageUntil = now + (long)WingRules.BARRAGE_SPIKES * WingRules.BARRAGE_EVERY_TICK;
                st.barrageReady = now + WingRules.BARRAGE_EVERY;
                st.volleyAt = now;
                st.dirty = true;
                UkakuFire.sound(level, back(npc), "entity.ender_dragon.flap", 0.9F, 1.7F);
            }
            case FAN -> {
                for (int i = 0; i < WingRules.FAN_SPIKES; i++) {
                    throwSpike(npc, st, target, level, now, FAN, WingRules.fan(i, WingRules.FAN_SPIKES), WingRules.FAN);
                }
                st.fanReady = now + WingRules.FAN_EVERY;
                st.nextShot = now + WingRules.VOLLEY_REST;
                UkakuFire.sound(level, back(npc), "block.amethyst_cluster.break", 1.0F, 1.3F);
            }
            case VOLLEY -> {
                if (st.volleyAt == Long.MIN_VALUE) {
                    // The wings curl forward to aim; the first spike a moment after.
                    st.volleyAt = now;
                    st.nextShot = now + 4L;
                    st.shots = 0;
                    st.dirty = true;
                    break;
                }
                throwSpike(npc, st, target, level, now, SHOT, 0.0, WingRules.SHOT);
                st.shots++;
                st.nextShot = now + WingRules.SHOT_EVERY;
                if (st.shots >= WingRules.VOLLEY) {
                    st.shots = 0;
                    st.volleyAt = Long.MIN_VALUE;
                    st.nextShot = now + WingRules.VOLLEY_REST;
                    st.dirty = true;
                }
            }
            default -> {
            }
        }
        return true;
    }

    /** Thrown in close, she gives ground; far, she closes; between, she holds where she is. */
    private static void keepDistance(EtoNpc npc, LivingEntity target, double gap) {
        if (gap < WingRules.KEEP - 1.0) {
            Vec3 away = UkakuFire.horizontal(npc.m_20182_().m_82546_(target.m_20182_()));
            Vec3 to = npc.m_20182_().m_82549_(away.m_82490_(4.0));
            npc.m_21573_().m_26519_(to.f_82479_, npc.m_20186_(), to.f_82481_, 1.25);
        } else if (gap > WingRules.FAR - 4.0) {
            npc.m_21573_().m_5624_(target, 1.15);
        } else {
            npc.m_21573_().m_26573_();
        }
    }

    // ---------------------------------------------------------------- her spikes

    /** A crystal spike from the tip of the next wing at {@code target}'s middle, turned {@code spread} degrees off it. */
    private static void throwSpike(EtoNpc npc, State st, LivingEntity target, ServerLevel level, long now, int kind, double spread, float damage) {
        int side = st.side;
        st.side = -st.side;
        Vec3 aim = GhoulNpcBrain.centre(target);
        Vec3 from = tip(npc, side, aim);
        Vec3 way = aim.m_82546_(from);
        way = way.m_82556_() < 1.0E-6 ? npc.m_20154_() : way.m_82541_();
        if (spread != 0.0) {
            double turn = Math.toRadians(spread);
            way = new Vec3(way.f_82479_ * Math.cos(turn) - way.f_82481_ * Math.sin(turn), way.f_82480_, way.f_82479_ * Math.sin(turn) + way.f_82481_ * Math.cos(turn));
        }
        Spike spike = new Spike();
        spike.kind = kind;
        spike.position = from;
        spike.velocity = way.m_82490_(WingRules.SPEED);
        spike.life = (int)Math.ceil(WingRules.RANGE / WingRules.SPEED);
        spike.damage = damage;
        st.spikes.add(spike);
        st.shotAt = now;
        st.shotSide = side;
        st.dirty = true;
        fx(npc, S2CUkakuFx.SPIKE + kind, spike.id, from, spike.velocity, kind == BARRAGE ? 0.7F : 0.85F, spike.life);
        UkakuFire.sound(level, from, "block.amethyst_block.hit", 0.6F, 1.4F + 0.2F * (float)npc.m_217043_().m_188500_());
    }

    /** Where a wing's spike leaves: its tip as it curls to aim ({@link WingRig#aimTip}), at her size. */
    private static Vec3 tip(EtoNpc npc, int side, Vec3 aim) {
        double size = npc.persona().size();
        double yaw = Math.toRadians(npc.f_20883_);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        Vec3 toward = aim.m_82546_(npc.m_20182_().m_82520_(0.0, WingRig.ROOT_Y * size, 0.0));
        double[] rig = {toward.f_82479_ * rightX + toward.f_82481_ * rightZ, toward.f_82480_, toward.f_82479_ * forwardX + toward.f_82481_ * forwardZ};
        double length = Math.sqrt(rig[0] * rig[0] + rig[1] * rig[1] + rig[2] * rig[2]);
        double[] dir = length < 1.0E-6 ? WingRig.FORWARD : new double[] {rig[0] / length, rig[1] / length, rig[2] / length};
        double[] local = WingRig.aimTip(WingRules.MASTERY, WingForm.TWIN, side, dir, false);
        return new Vec3(npc.m_20185_() + (rightX * local[0] + forwardX * local[2]) * size, npc.m_20186_() + local[1] * size,
            npc.m_20189_() + (rightZ * local[0] + forwardZ * local[2]) * size);
    }

    /** A spike a tick on: the first foe it meets is struck, a wall stops it, and it crumbles at the end of its range. */
    private static boolean step(EtoNpc npc, Spike spike, ServerLevel level, long now) {
        Vec3 from = spike.position;
        Vec3 to = from.m_82549_(spike.velocity);
        BlockHitResult block = level.m_45547_(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, npc));
        boolean wall = block.m_6662_() == HitResult.Type.BLOCK;
        Vec3 end = wall ? block.m_82450_() : to;
        AABB search = new AABB(from.f_82479_, from.f_82480_, from.f_82481_, end.f_82479_, end.f_82480_, end.f_82481_).m_82400_(WingRules.RADIUS + 1.0);
        List<LivingEntity> near = level.m_6443_(LivingEntity.class, search, living -> living != npc && !spike.hit.contains(living.m_19879_())
            && EtoBrain.foe(npc, living));
        int samples = Math.max(1, (int)Math.ceil(from.m_82554_(end) / 0.25));
        for (int sample = 0; sample <= samples; sample++) {
            Vec3 point = from.m_82549_(end.m_82546_(from).m_82490_(sample / (double)samples));
            for (LivingEntity body : near) {
                if (!body.m_20191_().m_82400_(WingRules.RADIUS).m_82390_(point)) {
                    continue;
                }
                spike.hit.add(body.m_19879_());
                pierce(npc, body, spike);
                fx(npc, S2CUkakuFx.SPIKE_IMPACT, spike.id, point, spike.velocity, 1.0F, spike.kind);
                return false;
            }
        }
        if (wall) {
            fx(npc, S2CUkakuFx.SPIKE_IMPACT, spike.id, end, spike.velocity, 0.0F, spike.kind);
            return false;
        }
        spike.position = to;
        if (--spike.life <= 0) {
            fx(npc, S2CUkakuFx.SPIKE_FIZZLE, spike.id, to, spike.velocity, 1.0F, spike.kind);
            return false;
        }
        return true;
    }

    /** A spike's hit: hurt (her attack), a little pushed the way it flew. */
    private static void pierce(EtoNpc npc, LivingEntity body, Spike spike) {
        Vec3 centre = GhoulNpcBrain.centre(body);
        Vec3 into = spike.velocity.m_82541_();
        WoundHits.expect(body, WoundBook.Blow.STAB, centre.m_82546_(into.m_82490_(0.3)), into, 0.0);
        body.f_19802_ = 0;
        if (!body.m_6469_(npc.m_269291_().m_269333_(npc), spike.damage)) {
            return;
        }
        if (spike.velocity.m_165925_() > 1.0E-6) {
            body.m_147240_(0.15, -spike.velocity.f_82479_, -spike.velocity.f_82481_);
        }
        UkakuFire.sound((ServerLevel)npc.m_9236_(), centre, "block.amethyst_block.break", 0.6F, 1.3F);
    }

    /** Her spikes in flight crumble away (she died, or left). */
    static void forget(EtoNpc npc, EtoBrain.Mind mind) {
        if (mind == null) {
            return;
        }
        for (Spike spike : mind.wings.spikes) {
            fx(npc, S2CUkakuFx.SPIKE_FIZZLE, spike.id, spike.position, spike.velocity, 1.0F, spike.kind);
        }
        mind.wings.spikes.clear();
    }

    // ---------------------------------------------------------------- what her clients are told

    static S2CNpcWings message(EtoNpc npc, State st) {
        return new S2CNpcWings(npc.m_19879_(), st.out, st.outAt, st.crystal, st.crystalAt, st.prey, st.shotAt, st.shotSide, st.volleyAt);
    }

    /** A player starts seeing her: they are told how her wings are. */
    static void tracked(ServerPlayer player, EtoNpc npc) {
        if (npc.persona().wings() && npc.mind != null) {
            ControlNetwork.sendTo(player, message(npc, npc.mind.wings));
        }
    }

    private static Vec3 back(EtoNpc npc) {
        double yaw = Math.toRadians(npc.f_20883_);
        return npc.m_20182_().m_82520_(Math.sin(yaw) * 0.3, WingRig.ROOT_Y * npc.persona().size(), -Math.cos(yaw) * 0.3);
    }

    private static void fx(EtoNpc npc, int kind, int id, Vec3 at, Vec3 velocity, float size, int extra) {
        ControlNetwork.broadcastAround(npc, new S2CUkakuFx(kind, npc.m_19879_(), id, at.f_82479_, at.f_82480_, at.f_82481_, (float)velocity.f_82479_,
            (float)velocity.f_82480_, (float)velocity.f_82481_, size, extra));
    }
}
