package com.hex.kagune.control.server;

import com.hex.kagune.control.family.ChildKaguneRules;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CPaleFx;
import com.hex.kagune.control.network.S2CPaleState;
import com.hex.kagune.control.npc.ChildNpc;
import com.hex.kagune.control.pale.PaleMove;
import com.hex.kagune.control.pale.PaleRules;
import com.hex.kagune.control.wound.WoundBook;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1.3.50: Eto's child's flesh, her Owl DNA kit's flesh moves on its smaller body (the owner: "Eto's child gets her kagune abilities"):
 * the rake of flesh out of its back across what it fights ({@link PaleMove#SWIPE}), the grab (the flesh reaches, holds it up before it,
 * hauls it over its shoulder and whips it out: {@link PaleMove#GRAB_REACH} on), and the club grown round its leg and brought down in a
 * shockwave ({@link PaleMove#HAMMER_CHARGE}, {@link PaleMove#HAMMER}); on the same ticks her moves are on ({@link PaleRules}), at its size
 * ({@link ChildKaguneRules#scale}). Every client draws it from {@link S2CPaleState} under its UUID (as hers is drawn), with her flesh.
 * Never the Owl: a child never has a kakuja.
 */
final class ChildFlesh {
    /** A held body pulled toward its spot this much of the way a tick, at most so fast; a throw's carry at most this fast. */
    private static final double PULL = 0.55;
    private static final double PULL_MAX = 1.6;
    private static final double THROW_MAX = 9.0;
    private static final double THROW_SHARE = 0.45;

    /** Its flesh: its move, its detail, when, at what, its charge, the way it faced, a point; what it holds; its cooldowns. */
    static final class Flesh {
        PaleMove move = PaleMove.NONE;
        int detail;
        long at = Long.MIN_VALUE;
        int target;
        float charge;
        float yaw;
        Vec3 point = Vec3.f_82478_;
        int heldId;
        int hold;
        double[] thrownFrom = new double[3];
        final Set<Integer> hit = new HashSet<>();
        long swipeReady = Long.MIN_VALUE;
        long grabReady = Long.MIN_VALUE;
        long hammerReady = Long.MIN_VALUE;
        boolean dirty = true;

        boolean busy() {
            return this.move != PaleMove.NONE;
        }
    }

    private ChildFlesh() {
    }

    // ---------------------------------------------------------------- beginning a move

    static void swipe(ChildNpc npc, Flesh f, LivingEntity target, ServerLevel level, long now) {
        npc.m_21573_().m_26573_();
        begin(npc, f, PaleMove.SWIPE, 0, target.m_19879_(), 0.0F, Vec3.f_82478_, now);
        f.hit.clear();
        f.swipeReady = now + ChildKaguneRules.SWIPE_EVERY;
        UkakuFire.sound(level, npc.m_20182_(), "hexkagune:manifest_rip", 0.6F, 1.35F);
    }

    static void grab(ChildNpc npc, Flesh f, LivingEntity target, ServerLevel level, long now) {
        npc.m_21573_().m_26573_();
        double distance = GhoulNpcBrain.centre(target).m_82554_(npc.m_146892_());
        begin(npc, f, PaleMove.GRAB_REACH, 0, target.m_19879_(), PaleRules.grabReachTicks(distance / Math.max(0.3, scale(npc))), Vec3.f_82478_, now);
        f.heldId = 0;
        f.grabReady = now + ChildKaguneRules.GRAB_EVERY;
        UkakuFire.sound(level, npc.m_20182_(), "hexkagune:manifest_rip", 0.5F, 1.5F);
    }

    static void hammer(ChildNpc npc, Flesh f, LivingEntity target, ServerLevel level, long now) {
        npc.m_21573_().m_26573_();
        begin(npc, f, PaleMove.HAMMER_CHARGE, 0, target.m_19879_(), 0.0F, Vec3.f_82478_, now);
        int raise = PaleRules.HAMMER_RAISE_TICKS;
        f.hold = raise + (int)Math.round(npc.m_217043_().m_188500_() * (PaleRules.HAMMER_GROW_TICKS - raise));
        f.hammerReady = now + ChildKaguneRules.HAMMER_EVERY;
    }

    // ---------------------------------------------------------------- each tick

    /** A tick of its move under way (any of them), what it hits limited to what it fights and monsters ({@link ChildKagune#fair}). */
    static void tick(ChildNpc npc, Flesh f, LivingEntity threat, float damage, ServerLevel level, long now) {
        if (!f.busy()) {
            return;
        }
        long age = now - f.at;
        npc.m_21573_().m_26573_();
        switch (f.move) {
            case SWIPE -> {
                if (age == PaleRules.SWIPE_ERUPT) {
                    UkakuFire.sound(level, npc.m_20182_(), "hexkagune:kagune_whip", 0.8F, 0.75F);
                }
                if (age > PaleRules.SWIPE_ERUPT && age <= PaleRules.SWIPE_SWEEP_END + 1 + Math.ceil(PaleRules.SWIPE_LAG)) {
                    sweep(npc, f, threat, damage, level, now, age);
                }
                if (age >= PaleRules.SWIPE_TICKS) {
                    end(f);
                }
            }
            case GRAB_REACH, GRAB_HOLD, GRAB_THROW, GRAB_MISS -> grabbing(npc, f, threat, damage, level, now, age);
            case HAMMER_CHARGE -> {
                if (age == PaleRules.HAMMER_SPROUT) {
                    UkakuFire.sound(level, npc.m_20182_(), "hexkagune:manifest_rip", 0.6F, 0.95F);
                }
                if (age >= Math.max(PaleRules.HAMMER_RAISE_TICKS, f.hold)) {
                    float charge = PaleRules.hammerCharge(age, false);
                    float yaw = f.yaw;
                    begin(npc, f, PaleMove.HAMMER, 0, f.target, charge, Vec3.f_82478_, now);
                    f.yaw = yaw;
                    UkakuFire.sound(level, npc.m_20182_(), "hexkagune:kagune_whip", 0.8F, 0.7F);
                }
            }
            case HAMMER -> {
                if (age == PaleRules.HAMMER_IMPACT) {
                    lands(npc, f, threat, damage, level, now);
                }
                if (age >= PaleRules.HAMMER_TICKS) {
                    end(f);
                }
            }
            default -> end(f);
        }
    }

    /** One tick of the rake: everything whose bearing it crossed since the last tick, within its reach, cut once and thrown. */
    private static void sweep(ChildNpc npc, Flesh f, LivingEntity threat, float damage, ServerLevel level, long now, long age) {
        double s = scale(npc);
        Vec3 forward = local(f, 0.0, 0.0, 1.0);
        Vec3 right = local(f, 1.0, 0.0, 0.0);
        double from = PaleRules.swipeBearing(age - 1.0 - PaleRules.SWIPE_LAG) + 6.0;
        double to = PaleRules.swipeBearing(age - PaleRules.SWIPE_LAG) - 6.0;
        double reach = PaleRules.SWIPE_REACH * s * Math.max(PaleRules.swipeExtent(age), PaleRules.swipeExtent(age - PaleRules.SWIPE_LAG)) + 0.5;
        for (LivingEntity body : level.m_6443_(LivingEntity.class, npc.m_20191_().m_82400_(reach + 1.0), b -> b != npc && b.m_6084_()
            && ChildKagune.fair(b, threat))) {
            if (f.hit.contains(body.m_19879_())) {
                continue;
            }
            Vec3 rel = body.m_20182_().m_82546_(npc.m_20182_());
            double flat = Math.sqrt(rel.f_82479_ * rel.f_82479_ + rel.f_82481_ * rel.f_82481_) - body.m_20205_() * 0.5;
            double bearing = Math.toDegrees(Math.atan2(rel.m_82526_(right), rel.m_82526_(forward)));
            if (flat > reach || bearing > from || bearing < to || rel.f_82480_ < -2.0 || rel.f_82480_ > 3.0) {
                continue;
            }
            f.hit.add(body.m_19879_());
            Vec3 centre = GhoulNpcBrain.centre(body);
            Vec3 drive = right.m_82490_(-1.0);
            if (!hurt(npc, body, damage * ChildKaguneRules.SWIPE_SHARE, centre.m_82546_(drive.m_82490_(body.m_20205_() * 0.5)), drive, WoundBook.Blow.SLASH)) {
                continue;
            }
            Vec3 knock = drive.m_82490_(0.9).m_82549_(forward.m_82490_(0.35)).m_82520_(0.0, 0.36, 0.0).m_82490_(1.0 - 0.6 * resist(body));
            body.m_20256_(knock);
            body.f_19864_ = true;
            PaleKnockdown.launched(body, now);
            fx(npc, S2CPaleFx.IMPACT, body.m_19879_(), centre, knock, 1.2F, S2CPaleFx.BONE);
            UkakuFire.sound(level, centre, "hexkagune:flesh_impact", 0.8F, 0.9F);
        }
    }

    /** The grab: the flesh reaches; caught, held up before it a moment; then hauled over its shoulder and whipped out ahead of it. */
    private static void grabbing(ChildNpc npc, Flesh f, LivingEntity threat, float damage, ServerLevel level, long now, long age) {
        double s = scale(npc);
        switch (f.move) {
            case GRAB_REACH -> {
                LivingEntity target = held(npc, f, level);
                if (target == null || !ChildKagune.fair(target, threat)) {
                    Vec3 point = target == null ? npc.m_146892_().m_82549_(local(f, 0.0, 0.0, 4.0 * s)) : GhoulNpcBrain.centre(target);
                    begin(npc, f, PaleMove.GRAB_MISS, 0, 0, 0.0F, point, now);
                    return;
                }
                if (age >= (long)f.charge) {
                    f.heldId = target.m_19879_();
                    begin(npc, f, PaleMove.GRAB_HOLD, 0, target.m_19879_(), 0.0F, Vec3.f_82478_, now);
                    f.hold = 12 + npc.m_217043_().m_188503_(12);
                    UkakuFire.sound(level, GhoulNpcBrain.centre(target), "hexkagune:kagune_grab", 0.8F, 1.1F);
                }
            }
            case GRAB_HOLD -> {
                LivingEntity target = held(npc, f, level);
                if (target == null) {
                    release(level, f);
                    end(f);
                    return;
                }
                carry(target, npc.m_20182_().m_82549_(local(f, 0.0, PaleRules.GRAB_HOLD_UP * s, PaleRules.GRAB_HOLD_AHEAD * s)), PULL, PULL_MAX);
                PaleStun.stun(target, now, 2);
                if (age >= f.hold) {
                    Vec3 from = GhoulNpcBrain.centre(target);
                    float yaw = f.yaw;
                    begin(npc, f, PaleMove.GRAB_THROW, 0, target.m_19879_(), 0.0F, from, now);
                    f.yaw = yaw;
                    f.heldId = target.m_19879_();
                    Vec3 gap = from.m_82546_(npc.m_20182_());
                    f.thrownFrom = new double[] {gap.m_82526_(local(f, 1.0, 0.0, 0.0)) / s, gap.f_82480_ / s, gap.m_82526_(local(f, 0.0, 0.0, 1.0)) / s};
                    UkakuFire.sound(level, npc.m_20182_(), "hexkagune:kagune_strain", 0.6F, 1.3F);
                }
            }
            case GRAB_THROW -> {
                LivingEntity target = f.heldId == 0 ? null : held(npc, f, level);
                int release = PaleRules.grabRelease();
                if (target != null && age < release) {
                    double[] at = PaleRules.throwAt(age + 1.0, f.thrownFrom);
                    carry(target, npc.m_20182_().m_82549_(local(f, at[0] * s, at[1] * s, at[2] * s)), 1.0, THROW_MAX);
                    PaleStun.stun(target, now, 2);
                } else if (target != null && age == release) {
                    Vec3 ahead = local(f, 0.0, 0.0, 1.0);
                    Vec3 flight = ahead.m_82490_(PaleRules.GRAB_THROW_SPEED * THROW_SHARE * s * (1.0 - 0.6 * resist(target))).m_82520_(0.0, 0.3, 0.0);
                    Vec3 centre = GhoulNpcBrain.centre(target);
                    if (hurt(npc, target, damage * ChildKaguneRules.FLESH_THROW_SHARE, centre, ahead, WoundBook.Blow.BLUNT)) {
                        target.m_20256_(flight);
                        target.f_19864_ = true;
                        target.f_19789_ = 0.0F;
                        PaleKnockdown.launched(target, now);
                    }
                    f.heldId = 0;
                    fx(npc, S2CPaleFx.IMPACT, target.m_19879_(), centre, flight, 1.2F, S2CPaleFx.FLESH);
                    UkakuFire.sound(level, npc.m_20182_(), "hexkagune:kagune_whip", 0.8F, 0.9F);
                }
                if (age >= release + PaleRules.GRAB_RETRACT) {
                    f.heldId = 0;
                    end(f);
                }
            }
            case GRAB_MISS -> {
                if (age >= PaleRules.GRAB_MISS_TICKS) {
                    end(f);
                }
            }
            default -> {
            }
        }
    }

    /** The club comes down ahead of it: a shockwave round where it lands (most at the middle), throwing things up and away. */
    private static void lands(ChildNpc npc, Flesh f, LivingEntity threat, float damage, ServerLevel level, long now) {
        double s = scale(npc);
        float charge = f.charge;
        Vec3 facing = local(f, 0.0, 0.0, 1.0);
        Vec3 head = npc.m_20182_().m_82549_(local(f, 0.1 * s, 0.0, PaleRules.hammerReach(charge) * s));
        BlockHitResult ground = level.m_45547_(new ClipContext(head.m_82520_(0.0, 1.5, 0.0), head.m_82520_(0.0, -3.5, 0.0), ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, npc));
        Vec3 at = ground.m_6662_() == HitResult.Type.BLOCK ? ground.m_82450_() : new Vec3(head.f_82479_, npc.m_20186_(), head.f_82481_);
        int groundState = net.minecraft.world.level.block.Block.m_49956_(level.m_8055_(net.minecraft.core.BlockPos.m_274561_(at.f_82479_, at.f_82480_ - 0.01,
            at.f_82481_)));
        double radius = PaleRules.hammerRadius(charge) * s;
        AABB area = new AABB(at.f_82479_ - radius - 1.0, at.f_82480_ - 2.0, at.f_82481_ - radius - 1.0, at.f_82479_ + radius + 1.0, at.f_82480_ + 3.0,
            at.f_82481_ + radius + 1.0);
        float most = damage * (ChildKaguneRules.HAMMER_SHARE + ChildKaguneRules.HAMMER_SHARE * charge);
        for (LivingEntity body : level.m_6443_(LivingEntity.class, area, b -> b != npc && b.m_6084_() && ChildKagune.fair(b, threat))) {
            Vec3 off = body.m_20182_().m_82546_(at);
            double flat = Math.sqrt(off.f_82479_ * off.f_82479_ + off.f_82481_ * off.f_82481_);
            if (flat - body.m_20205_() * 0.5 > radius) {
                continue;
            }
            double near = 1.0 - Math.max(0.0, Math.min(1.0, flat / radius));
            Vec3 away = flat < 0.2 ? facing : new Vec3(off.f_82479_ / flat, 0.0, off.f_82481_ / flat);
            if (!hurt(npc, body, most * (float)(0.4 + 0.6 * near), body.m_20182_(), away.m_82520_(0.0, 1.0, 0.0), WoundBook.Blow.SLAM)) {
                continue;
            }
            double launch = PaleRules.hammerLaunch(charge) * s * (0.35 + 0.65 * near) * (1.0 - 0.6 * resist(body));
            body.m_20256_(away.m_82490_(launch * 0.5).m_82520_(0.0, launch * 0.55 + 0.15, 0.0));
            body.f_19864_ = true;
            PaleKnockdown.launched(body, now);
        }
        fx(npc, S2CPaleFx.SHOCKWAVE, 0, at, facing, (float)radius, groundState);
        UkakuFire.sound(level, at, "entity.generic.explode", 0.5F + 0.3F * Math.min(1.0F, charge), 0.9F);
        UkakuFire.sound(level, at, "hexkagune:kagune_slam", 0.8F, 0.8F);
    }

    // ---------------------------------------------------------------- helpers

    private static void begin(ChildNpc npc, Flesh f, PaleMove move, int detail, int target, float charge, Vec3 point, long now) {
        f.move = move;
        f.detail = detail;
        f.at = now;
        f.target = target;
        f.charge = charge;
        f.point = point;
        f.yaw = npc.f_20883_;
        f.dirty = true;
    }

    static void end(Flesh f) {
        if (f.move != PaleMove.NONE) {
            f.move = PaleMove.NONE;
            f.at = Long.MIN_VALUE;
            f.dirty = true;
        }
    }

    /** Whatever the flesh holds is let go where it is. */
    static void release(ServerLevel level, Flesh f) {
        if (f.heldId != 0) {
            Entity entity = level.m_6815_(f.heldId);
            if (entity != null) {
                entity.m_20256_(Vec3.f_82478_);
                entity.f_19864_ = true;
                entity.f_19789_ = 0.0F;
            }
        }
        f.heldId = 0;
    }

    private static LivingEntity held(ChildNpc npc, Flesh f, ServerLevel level) {
        int id = f.heldId != 0 ? f.heldId : f.target;
        Entity entity = id == 0 ? null : level.m_6815_(id);
        double most = PaleRules.GRAB_RANGE * scale(npc) + 6.0;
        return entity instanceof LivingEntity living && living.m_6084_() && living.m_20280_(npc) <= most * most ? living : null;
    }

    private static void carry(LivingEntity target, Vec3 at, double pull, double most) {
        Vec3 gap = at.m_82546_(GhoulNpcBrain.centre(target));
        double length = gap.m_82553_();
        Vec3 move = length * pull > most ? gap.m_82490_(most / Math.max(1.0E-6, length)) : gap.m_82490_(pull);
        target.m_20256_(move);
        target.f_19864_ = true;
        target.f_19812_ = true;
        target.f_19789_ = 0.0F;
    }

    private static boolean hurt(ChildNpc npc, LivingEntity body, float damage, Vec3 at, Vec3 way, WoundBook.Blow blow) {
        body.f_19802_ = 0;
        WoundHits.expect(body, blow, at, way, 0.0);
        return body.m_6469_(npc.m_269291_().m_269333_(npc), damage);
    }

    private static double resist(LivingEntity body) {
        return Math.max(0.0, Math.min(1.0, body.m_21133_(net.minecraft.world.entity.ai.attributes.Attributes.f_22278_)));
    }

    /** A point {@code x} to its right, {@code y} up, {@code z} ahead of it, the way the move faces. */
    static Vec3 local(Flesh f, double x, double y, double z) {
        double yaw = Math.toRadians(f.yaw);
        Vec3 facing = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 right = new Vec3(-facing.f_82481_, 0.0, facing.f_82479_);
        return right.m_82490_(x).m_82549_(facing.m_82490_(z)).m_82520_(0.0, y, 0.0);
    }

    private static void fx(ChildNpc npc, int kind, int target, Vec3 at, Vec3 velocity, float size, int extra) {
        ControlNetwork.broadcastAround(npc, PaleBlitz.packet(kind, npc.m_19879_(), target, at, velocity, size, extra));
    }

    /** Its size against a grown body's (its stage's). */
    static double scale(ChildNpc npc) {
        return ChildKaguneRules.scale(npc.stage);
    }

    /** What its clients are told of its flesh (as a player's or Eto's: {@link S2CPaleState}, under its UUID). */
    static S2CPaleState message(ChildNpc npc, Flesh f) {
        return new S2CPaleState(npc.m_20148_(), f.move.ordinal(), f.detail, f.at, f.target, f.charge, f.yaw, f.point.f_82479_, f.point.f_82480_,
            f.point.f_82481_);
    }

    /** Sent when it changed. */
    static void send(ChildNpc npc, Flesh f) {
        if (f.dirty) {
            f.dirty = false;
            ControlNetwork.broadcastAround(npc, message(npc, f));
        }
    }

    /** A player starts seeing it: they learn its flesh's move. */
    static void tracked(ServerPlayer viewer, ChildNpc npc, Flesh f) {
        ControlNetwork.sendTo(viewer, message(npc, f));
    }
}
