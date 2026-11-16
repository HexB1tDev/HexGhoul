package com.hex.kagune.control.server;

import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CPaleFx;
import com.hex.kagune.control.network.S2CPaleWatch;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.EtoRules;
import com.hex.kagune.control.pale.PaleMove;
import com.hex.kagune.control.pale.PaleRules;
import com.hex.kagune.control.pale.WatchRules;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1.3.6: Eto's Watch This, a Pale player's ({@link PaleWatch}) on her: her arms flung out in a cross, two tentacles burst out of her
 * back and for ten seconds hunt on their own while she fights on. Each goes for what she fights or anything hostile after her, near
 * her and in her sight, favouring its own side; the beak through its chest, lifted in a great sweep up over her and flung out to that
 * tentacle's side. Every client draws them from the same {@link S2CPaleWatch} a player's are drawn from (sent by her UUID), on her
 * smaller body ({@link EtoRules#SIZE}: the tentacles, their reach and their sweep are as much smaller as she is).
 */
final class EtoWatch {
    /** One tentacle: which (1 her right, -1 her left), what it is doing since when, at what, and where a carry began (her frame). */
    private static final class Tentacle {
        final int side;
        WatchRules.Phase phase = WatchRules.Phase.NONE;
        long startedAt;
        int targetId;
        int lunge;
        double[] start = new double[3];
        float yaw;
        Vec3 point = Vec3.f_82478_;

        Tentacle(int side) {
            this.side = side;
        }
    }

    /** Her tentacles: when they burst out, and the bodies just thrown (free again from that tick). */
    private static final class Watch {
        final long out;
        final Tentacle right = new Tentacle(1);
        final Tentacle left = new Tentacle(-1);
        final Map<Integer, Long> thrown = new HashMap<>();

        Watch(long out) {
            this.out = out;
        }

        Tentacle[] both() {
            return new Tentacle[] {this.right, this.left};
        }
    }

    /** By her entity id. */
    private static final Map<Integer, Watch> WATCHES = new HashMap<>();

    private EtoWatch() {
    }

    /** Whether her tentacles are out (or coming out, or going back in). */
    static boolean out(EtoNpc npc) {
        return WATCHES.containsKey(npc.m_19879_());
    }

    /** The cross and the tentacles bursting out of her back. */
    static void start(EtoNpc npc, EtoOwl.Fight f, ServerLevel level, long now, EtoRules.Phase phase) {
        if (out(npc)) {
            return;
        }
        npc.m_21573_().m_26573_();
        EtoOwl.begin(npc, f, PaleMove.WATCH, 0, 0, 0.0F, Vec3.f_82478_, now);
        Watch watch = new Watch(now + WatchRules.ERUPT_AT);
        WATCHES.put(npc.m_19879_(), watch);
        for (Tentacle tentacle : watch.both()) {
            set(npc, tentacle, WatchRules.Phase.OUT, watch.out);
        }
        f.watchReady = watch.out + WatchRules.ACTIVE + WatchRules.RETRACT + EtoRules.cooldown(EtoRules.WATCH_EVERY, phase);
        UkakuFire.sound(level, npc.m_20182_(), "hexkagune:manifest_rip", 1.0F, 0.7F);
    }

    /** Each tick of hers: the tentacles hunt, strike, carry and throw; after ten seconds they go back in. */
    static void tick(EtoNpc npc, EtoBrain.Mind mind, ServerLevel level, long now) {
        Watch watch = WATCHES.get(npc.m_19879_());
        if (watch == null) {
            return;
        }
        long end = watch.out + WatchRules.ACTIVE;
        if (now >= end || !npc.m_6084_()) {
            for (Tentacle tentacle : watch.both()) {
                if (tentacle.phase == WatchRules.Phase.CARRY) {
                    fling(npc, watch, tentacle, level, now);
                }
                if (tentacle.phase != WatchRules.Phase.IN) {
                    set(npc, tentacle, WatchRules.Phase.IN, Math.min(now, end));
                }
            }
            if (now >= end + WatchRules.RETRACT || !npc.m_6084_()) {
                forget(npc);
            }
            return;
        }
        watch.thrown.values().removeIf(free -> now >= free);
        for (Tentacle tentacle : watch.both()) {
            step(npc, mind, watch, tentacle, level, now);
        }
    }

    private static void step(EtoNpc npc, EtoBrain.Mind mind, Watch watch, Tentacle tentacle, ServerLevel level, long now) {
        long age = now - tentacle.startedAt;
        LivingEntity target = tentacle.targetId == 0 ? null
            : level.m_6815_(tentacle.targetId) instanceof LivingEntity living && living.m_6084_() ? living : null;
        switch (tentacle.phase) {
            case OUT -> {
                if (age >= WatchRules.ERUPT) {
                    set(npc, tentacle, WatchRules.Phase.IDLE, now);
                }
            }
            case IDLE -> {
                LivingEntity prey = hunt(npc, mind, watch, tentacle, level, now);
                if (prey != null) {
                    tentacle.targetId = prey.m_19879_();
                    double distance = PaleWatch.chest(prey).m_82554_(world(npc, npc.m_146908_(), WatchRules.pivot(tentacle.side)));
                    tentacle.lunge = WatchRules.lungeTicks(distance / EtoRules.SIZE);
                    set(npc, tentacle, WatchRules.Phase.STRIKE, now);
                }
            }
            case STRIKE -> {
                if (age < WatchRules.strikeTicks(tentacle.lunge)) {
                    return;
                }
                Vec3 pivot = world(npc, npc.m_146908_(), WatchRules.pivot(tentacle.side));
                if (target == null || !EtoOwl.fair(npc, mind, target) || PaleWatch.chest(target).m_82554_(pivot) > WatchRules.REACH * EtoRules.SIZE) {
                    // Gone, or got away: the beak stabs the air where it was going.
                    Vec3 aim = target == null ? pivot.m_82549_(npc.m_20154_().m_82490_(3.0)) : PaleWatch.chest(target);
                    Vec3 way = aim.m_82546_(pivot);
                    tentacle.point = pivot.m_82549_(way.m_82490_(Math.min(1.0, WatchRules.REACH * EtoRules.SIZE / Math.max(1.0E-6, way.m_82553_()))));
                    set(npc, tentacle, WatchRules.Phase.MISS, now);
                    return;
                }
                impale(npc, mind, tentacle, target, level, now);
            }
            case CARRY -> {
                if (target == null) {
                    tentacle.point = world(npc, tentacle.yaw, WatchRules.carry(1.0, tentacle.start, tentacle.side));
                    set(npc, tentacle, WatchRules.Phase.RECOVER, now);
                    return;
                }
                long lifted = age - WatchRules.IMPALE_HOLD;
                double u = lifted <= 0 ? 0.0 : WatchRules.sweep(lifted);
                hold(target, world(npc, tentacle.yaw, WatchRules.carry(u, tentacle.start, tentacle.side)), level, now);
                if (lifted >= WatchRules.LIFT) {
                    fling(npc, watch, tentacle, level, now);
                }
            }
            case RECOVER -> {
                if (age >= WatchRules.RECOVER) {
                    set(npc, tentacle, WatchRules.Phase.IDLE, now);
                }
            }
            case MISS -> {
                if (age >= WatchRules.MISS_RECOVER) {
                    set(npc, tentacle, WatchRules.Phase.IDLE, now);
                }
            }
            default -> {
            }
        }
    }

    /**
     * What this tentacle goes for: what she fights, or anything hostile after her, near her and in her sight, not the other
     * tentacle's, not just thrown; the nearest, favouring its own side of her. Never her partner, never a player she isn't fighting.
     */
    private static LivingEntity hunt(EtoNpc npc, EtoBrain.Mind mind, Watch watch, Tentacle tentacle, ServerLevel level, long now) {
        Vec3 from = PaleWatch.chest(npc);
        Tentacle other = tentacle == watch.right ? watch.left : watch.right;
        int taken = other.phase == WatchRules.Phase.STRIKE || other.phase == WatchRules.Phase.CARRY ? other.targetId : 0;
        double yaw = Math.toRadians(npc.m_146908_());
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        double range = WatchRules.RANGE * EtoRules.SIZE;
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        AABB area = npc.m_20191_().m_82400_(range + 1.0);
        for (LivingEntity candidate : level.m_6443_(LivingEntity.class, area, entity -> EtoOwl.fair(npc, mind, entity))) {
            int id = candidate.m_19879_();
            if (id == taken || watch.thrown.containsKey(id) || !(id == mind.target || candidate instanceof Mob mob && mob.m_5448_() == npc)) {
                continue;
            }
            Vec3 to = PaleWatch.chest(candidate).m_82546_(from);
            double distance = to.m_82553_();
            if (distance > range || !npc.m_142582_(candidate)) {
                continue;
            }
            double score = distance + (to.m_82526_(right) * tentacle.side >= 0.0 ? 0.0 : 2.5);
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    /** The beak through its chest: the hurt, a stab wound; it is caught on the beak where it is (her frame now). */
    private static void impale(EtoNpc npc, EtoBrain.Mind mind, Tentacle tentacle, LivingEntity target, ServerLevel level, long now) {
        Vec3 chest = PaleWatch.chest(target);
        Vec3 pivot = world(npc, npc.m_146908_(), WatchRules.pivot(tentacle.side));
        Vec3 through = chest.m_82546_(pivot).m_82541_();
        target.f_19802_ = 0;
        WoundHits.expect(target, com.hex.kagune.control.wound.WoundBook.Blow.STAB, chest.m_82546_(through.m_82490_(target.m_20205_() * 0.5)), through, 0.0);
        float damage = PaleRules.fist(EtoRules.MASTERY) * WatchRules.DAMAGE * mind.phase.power;
        if (!target.m_6469_(npc.m_269291_().m_269333_(npc), damage)) {
            tentacle.point = chest;
            set(npc, tentacle, WatchRules.Phase.MISS, now);
            return;
        }
        EtoOwl.fx(npc, S2CPaleFx.IMPALED, target.m_19879_(), chest, through, target.m_20206_(), tentacle.side);
        UkakuFire.sound(level, chest, "entity.player.attack.strong", 1.0F, 0.6F);
        UkakuFire.sound(level, chest, "entity.slime.squish", 1.0F, 0.7F);
        UkakuFire.sound(level, chest, "hexkagune:bone_crack", 0.9F, 0.85F);
        tentacle.yaw = npc.m_146908_();
        tentacle.start = frame(npc, tentacle.yaw, chest);
        set(npc, tentacle, WatchRules.Phase.CARRY, now);
        hold(target, chest, level, now);
    }

    /** Let go at the end of the sweep: thrown on at the sweep's own speed, out to that tentacle's side. */
    private static void fling(EtoNpc npc, Watch watch, Tentacle tentacle, ServerLevel level, long now) {
        if (level.m_6815_(tentacle.targetId) instanceof LivingEntity body && body.m_6084_()) {
            double[] v = WatchRules.throwVelocity(tentacle.start, tentacle.side);
            Vec3 launch = PaleWatch.turn(tentacle.yaw, v).m_82490_(EtoRules.SIZE * (1.0 - 0.5 * EtoOwl.resist(body)));
            body.m_20256_(launch);
            body.f_19864_ = true;
            body.f_19789_ = 0.0F;
            PaleKnockdown.launched(body, now);
            watch.thrown.put(body.m_19879_(), now + WatchRules.THROWN_FREE);
            UkakuFire.sound(level, PaleWatch.chest(body), "entity.player.attack.sweep", 1.2F, 0.5F);
        }
        tentacle.point = world(npc, tentacle.yaw, WatchRules.carry(1.0, tentacle.start, tentacle.side));
        set(npc, tentacle, WatchRules.Phase.RECOVER, now);
    }

    /** Held on the beak: its chest where the sweep has it, nothing else moving it, no fall to take. */
    private static void hold(LivingEntity body, Vec3 chest, ServerLevel level, long now) {
        Vec3 feet = chest.m_82520_(0.0, -body.m_20206_() * WatchRules.CHEST, 0.0);
        PaleStun.stun(body, now, 3);
        body.f_19789_ = 0.0F;
        if (body instanceof ServerPlayer player && player.m_20182_().m_82554_(feet) <= 2.5) {
            player.m_20256_(Vec3.f_82478_);
            return;
        }
        Vec3 offset = feet.m_82546_(body.m_20182_());
        if (level.m_45756_(body, body.m_20191_().m_82386_(offset.f_82479_, offset.f_82480_, offset.f_82481_))) {
            body.m_6021_(feet.f_82479_, feet.f_82480_, feet.f_82481_);
        }
        body.m_20256_(Vec3.f_82478_);
        body.f_19864_ = true;
    }

    /** The tentacle starts something new now (or at {@code at}) and every client about her is told. */
    private static void set(EtoNpc npc, Tentacle tentacle, WatchRules.Phase phase, long at) {
        tentacle.phase = phase;
        tentacle.startedAt = at;
        if (phase == WatchRules.Phase.IDLE || phase == WatchRules.Phase.OUT || phase == WatchRules.Phase.IN || phase == WatchRules.Phase.NONE) {
            tentacle.targetId = 0;
        }
        double[] p = phase == WatchRules.Phase.CARRY ? tentacle.start
            : new double[] {tentacle.point.f_82479_, tentacle.point.f_82480_, tentacle.point.f_82481_};
        ControlNetwork.broadcastAround(npc, new S2CPaleWatch(npc.m_20148_(), tentacle.side, phase.ordinal(), at, tentacle.targetId, tentacle.lunge,
            tentacle.yaw, p[0], p[1], p[2]));
    }

    /** Her tentacles go at once (she died, she is gone): every client is told, what they held is let go where it is. */
    static void forget(EtoNpc npc) {
        Watch watch = WATCHES.remove(npc.m_19879_());
        if (watch == null) {
            return;
        }
        long now = npc.m_9236_().m_46467_();
        for (Tentacle tentacle : watch.both()) {
            set(npc, tentacle, WatchRules.Phase.NONE, now);
        }
    }

    /** A point of her frame ({@code yaw} her facing, degrees; a player's size, shrunk to hers) in the world. */
    private static Vec3 world(EtoNpc npc, float yaw, double[] p) {
        return npc.m_20182_().m_82549_(PaleWatch.turn(yaw, p).m_82490_(EtoRules.SIZE));
    }

    /** A world point in her frame, at a player's size (as {@link #world} takes it). */
    private static double[] frame(EtoNpc npc, float yaw, Vec3 at) {
        Vec3 d = at.m_82546_(npc.m_20182_()).m_82490_(1.0 / EtoRules.SIZE);
        double r = Math.toRadians(yaw);
        double fx = -Math.sin(r);
        double fz = Math.cos(r);
        return new double[] {-fz * d.f_82479_ + fx * d.f_82481_, d.f_82480_, fx * d.f_82479_ + fz * d.f_82481_};
    }
}
