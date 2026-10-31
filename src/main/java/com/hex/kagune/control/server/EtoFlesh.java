package com.hex.kagune.control.server;

import com.hex.kagune.control.network.S2CPaleFx;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.EtoRules;
import com.hex.kagune.control.pale.PaleMove;
import com.hex.kagune.control.pale.PaleRules;
import com.hex.kagune.control.wound.WoundBook;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1.3.6: Eto's Owl DNA flesh, a Pale player's moves on her body, on the same ticks and out of the same rules ({@link PaleRules}),
 * drawn by every client as a player's are ({@code PaleFleshRenderer}, her body's pose from {@code EtoFight}):
 * <ul>
 *   <li>The swipe: the flesh erupts out of her upper back and rakes across everything in front of her, right round to her left,
 *       flinging what it cuts to her left.</li>
 *   <li>The grab: the flesh flies at what she fights, its tip opening into a hand; it holds it up before her a moment, hauls it back
 *       over her shoulder and whips it out ahead of her.</li>
 *   <li>The Flesh Hammer: rooted on one leg, the club grows round the other as it rises; the axe kick slams it down ahead of her, the
 *       shockwave throwing everything round it. Maniac, it is True Anonymity's: grown on past its full size, spiked.</li>
 * </ul>
 * Everything that grows out of her is as much smaller as she is ({@link EtoRules#SIZE}). Nothing she does breaks the ground (her
 * Library stands). What she hits is what she fights and anything hostile caught with it; never her partner.
 */
final class EtoFlesh {
    /** How hard the hold pulls the body to where she holds it (a fraction of the gap a tick, at most so fast); the throw's most a tick. */
    private static final double PULL = 0.55;
    private static final double PULL_MAX = 1.6;
    private static final double THROW_MAX = 9.0;
    /** How hard the flung body goes (a share of a player's throw: hers carries no block smashing behind it). */
    private static final double THROW_SHARE = 0.45;

    private EtoFlesh() {
    }

    static void tick(EtoNpc npc, EtoBrain.Mind mind, EtoOwl.Fight f, LivingEntity target, ServerLevel level, long now, long age, EtoRules.Phase phase) {
        switch (f.move) {
            case SWIPE -> tickSwipe(npc, mind, f, level, now, age, phase);
            case GRAB_REACH, GRAB_HOLD, GRAB_THROW, GRAB_MISS -> tickGrab(npc, mind, f, level, now, age, phase);
            case HAMMER_CHARGE -> tickHammerCharge(npc, f, level, now, age);
            case HAMMER -> tickHammer(npc, mind, f, level, now, age, phase);
            default -> EtoOwl.end(f);
        }
    }

    // ---------------------------------------------------------------- the swipe

    static void swipe(EtoNpc npc, EtoOwl.Fight f, LivingEntity target, ServerLevel level, long now, EtoRules.Phase phase) {
        npc.m_21573_().m_26573_();
        EtoOwl.begin(npc, f, PaleMove.SWIPE, 0, target.m_19879_(), 0.0F, Vec3.f_82478_, now);
        f.hit.clear();
        f.swipeReady = now + EtoRules.cooldown(EtoRules.SWIPE_EVERY, phase);
        UkakuFire.sound(level, npc.m_20182_(), "hexkagune:manifest_rip", 0.8F, 1.15F);
    }

    private static void tickSwipe(EtoNpc npc, EtoBrain.Mind mind, EtoOwl.Fight f, ServerLevel level, long now, long age, EtoRules.Phase phase) {
        npc.m_21573_().m_26573_();
        if (age == PaleRules.SWIPE_ERUPT) {
            UkakuFire.sound(level, npc.m_20182_(), "hexkagune:kagune_whip", 1.0F, 0.55F);
        }
        if (age > PaleRules.SWIPE_ERUPT && age <= PaleRules.SWIPE_SWEEP_END + 1 + Math.ceil(PaleRules.SWIPE_LAG)) {
            sweep(npc, mind, f, level, now, age, phase);
        }
        if (age >= PaleRules.SWIPE_TICKS) {
            EtoOwl.end(f);
        }
    }

    /** One tick of the cut: everything whose bearing it crossed since the last tick, within its reach, is cut once and thrown. */
    private static void sweep(EtoNpc npc, EtoBrain.Mind mind, EtoOwl.Fight f, ServerLevel level, long now, long age, EtoRules.Phase phase) {
        double yaw = Math.toRadians(f.yaw);
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        double from = PaleRules.swipeBearing(age - 1.0 - PaleRules.SWIPE_LAG) + 6.0;
        double to = PaleRules.swipeBearing(age - PaleRules.SWIPE_LAG) - 6.0;
        double reach = PaleRules.SWIPE_REACH * EtoRules.SIZE * Math.max(PaleRules.swipeExtent(age), PaleRules.swipeExtent(age - PaleRules.SWIPE_LAG)) + 0.6;
        AABB area = npc.m_20191_().m_82400_(reach + 1.0);
        float damage = PaleRules.fist(EtoRules.MASTERY) * PaleRules.SWIPE_DAMAGE * phase.power;
        for (LivingEntity target : level.m_6443_(LivingEntity.class, area, body -> EtoOwl.fair(npc, mind, body))) {
            if (f.hit.contains(target.m_19879_())) {
                continue;
            }
            Vec3 rel = target.m_20182_().m_82546_(npc.m_20182_());
            double flat = Math.sqrt(rel.f_82479_ * rel.f_82479_ + rel.f_82481_ * rel.f_82481_) - target.m_20205_() * 0.5;
            double bearing = Math.toDegrees(Math.atan2(rel.m_82526_(right), rel.m_82526_(forward)));
            if (flat > reach || bearing > from || bearing < to || rel.f_82480_ < -2.0 || rel.f_82480_ > 3.0) {
                continue;
            }
            f.hit.add(target.m_19879_());
            Vec3 centre = GhoulNpcBrain.centre(target);
            Vec3 drive = right.m_82490_(-1.0);
            if (!hurt(npc, target, damage, centre.m_82546_(drive.m_82490_(target.m_20205_() * 0.5)), drive, WoundBook.Blow.SLASH)) {
                continue;
            }
            // Thrown hard the way the flesh swings (to her left) and away.
            Vec3 knock = drive.m_82490_(1.2).m_82549_(forward.m_82490_(0.45)).m_82520_(0.0, 0.42, 0.0).m_82490_(1.0 - 0.6 * EtoOwl.resist(target));
            target.m_20256_(knock);
            target.f_19864_ = true;
            PaleKnockdown.launched(target, now);
            EtoOwl.fx(npc, S2CPaleFx.IMPACT, target.m_19879_(), centre, knock, 1.6F, S2CPaleFx.BONE);
            UkakuFire.sound(level, centre, "hexkagune:flesh_impact", 1.0F, 0.75F);
            UkakuFire.sound(level, centre, "entity.player.attack.sweep", 1.0F, 0.6F);
        }
    }

    // ---------------------------------------------------------------- the grab

    static void grab(EtoNpc npc, EtoOwl.Fight f, LivingEntity target, ServerLevel level, long now, EtoRules.Phase phase) {
        npc.m_21573_().m_26573_();
        double distance = GhoulNpcBrain.centre(target).m_82554_(npc.m_146892_());
        EtoOwl.begin(npc, f, PaleMove.GRAB_REACH, 0, target.m_19879_(), PaleRules.grabReachTicks(distance), Vec3.f_82478_, now);
        f.heldId = 0;
        f.grabReady = now + EtoRules.cooldown(EtoRules.GRAB_EVERY, phase);
        UkakuFire.sound(level, npc.m_20182_(), "hexkagune:manifest_rip", 0.7F, 1.3F);
    }

    /** What the flesh reaches for or holds, while it is still there (alive, not too far). */
    private static LivingEntity held(EtoNpc npc, EtoOwl.Fight f, ServerLevel level) {
        int id = f.heldId != 0 ? f.heldId : f.target;
        Entity entity = id == 0 ? null : level.m_6815_(id);
        double most = PaleRules.GRAB_RANGE + 6.0;
        return entity instanceof LivingEntity living && living.m_6084_() && living.m_20280_(npc) <= most * most ? living : null;
    }

    private static void tickGrab(EtoNpc npc, EtoBrain.Mind mind, EtoOwl.Fight f, ServerLevel level, long now, long age, EtoRules.Phase phase) {
        npc.m_21573_().m_26573_();
        switch (f.move) {
            case GRAB_REACH -> {
                LivingEntity target = held(npc, f, level);
                if (target == null || !EtoOwl.fair(npc, mind, target)) {
                    Vec3 point = target == null ? npc.m_146892_().m_82549_(EtoOwl.local(f, 0.0, 0.0, 5.0)) : GhoulNpcBrain.centre(target);
                    EtoOwl.begin(npc, f, PaleMove.GRAB_MISS, 0, 0, 0.0F, point, now);
                    return;
                }
                if (age >= (long)f.charge) {
                    // Caught: the hand closes round it; held up before her a moment (the maniac hauls it straight over).
                    f.heldId = target.m_19879_();
                    EtoOwl.begin(npc, f, PaleMove.GRAB_HOLD, 0, target.m_19879_(), 0.0F, Vec3.f_82478_, now);
                    f.hold = phase == EtoRules.Phase.MANIAC ? 6 : 14 + npc.m_217043_().m_188503_(16);
                    UkakuFire.sound(level, GhoulNpcBrain.centre(target), "hexkagune:kagune_grab", 1.0F, 0.9F);
                }
            }
            case GRAB_HOLD -> {
                LivingEntity target = held(npc, f, level);
                if (target == null) {
                    release(level, f);
                    EtoOwl.end(f);
                    return;
                }
                carry(target, npc.m_20182_().m_82549_(EtoOwl.local(f, 0.0, PaleRules.GRAB_HOLD_UP * EtoRules.SIZE, PaleRules.GRAB_HOLD_AHEAD * EtoRules.SIZE)),
                    PULL, PULL_MAX);
                PaleStun.stun(target, now, 2);
                if (age >= f.hold) {
                    fling(npc, f, target, level, now);
                }
            }
            case GRAB_THROW -> {
                LivingEntity target = f.heldId == 0 ? null : held(npc, f, level);
                int release = PaleRules.grabRelease();
                if (target != null && age < release) {
                    // Hauled over her shoulder and all the way back, hung there, then whipped over her and out.
                    double[] at = PaleRules.throwAt(age + 1.0, f.thrownFrom);
                    carry(target, npc.m_20182_().m_82549_(EtoOwl.local(f, at[0] * EtoRules.SIZE, at[1] * EtoRules.SIZE, at[2] * EtoRules.SIZE)), 1.0, THROW_MAX);
                    PaleStun.stun(target, now, 2);
                    if (age == PaleRules.GRAB_PULL + PaleRules.GRAB_HANG) {
                        UkakuFire.sound(level, npc.m_20182_(), "hexkagune:kagune_whip", 1.0F, 0.5F);
                    }
                } else if (target != null && age == release) {
                    throwIt(npc, f, target, level, now, phase);
                }
                if (age >= release + PaleRules.GRAB_RETRACT) {
                    f.heldId = 0;
                    EtoOwl.end(f);
                }
            }
            case GRAB_MISS -> {
                if (age >= PaleRules.GRAB_MISS_TICKS) {
                    EtoOwl.end(f);
                }
            }
            default -> {
            }
        }
    }

    /** Back over her shoulder, then whipped forward. */
    private static void fling(EtoNpc npc, EtoOwl.Fight f, LivingEntity target, ServerLevel level, long now) {
        Vec3 from = GhoulNpcBrain.centre(target);
        float yaw = f.yaw;
        EtoOwl.begin(npc, f, PaleMove.GRAB_THROW, 0, target.m_19879_(), 0.0F, from, now);
        f.yaw = yaw;
        f.heldId = target.m_19879_();
        // Where it was held, in her frame, as a player's body would have it (the throw's path is laid out at a player's size).
        Vec3 gap = from.m_82546_(npc.m_20182_());
        Vec3 ahead = EtoOwl.local(f, 0.0, 0.0, 1.0);
        Vec3 right = EtoOwl.local(f, 1.0, 0.0, 0.0);
        f.thrownFrom = new double[] {gap.m_82526_(right) / EtoRules.SIZE, gap.f_82480_ / EtoRules.SIZE, gap.m_82526_(ahead) / EtoRules.SIZE};
        UkakuFire.sound(level, npc.m_20182_(), "hexkagune:kagune_strain", 0.8F, 1.1F);
    }

    /** The whip's end: the body goes, out ahead of her, hurt. */
    private static void throwIt(EtoNpc npc, EtoOwl.Fight f, LivingEntity target, ServerLevel level, long now, EtoRules.Phase phase) {
        Vec3 ahead = EtoOwl.local(f, 0.0, 0.0, 1.0);
        Vec3 flight = ahead.m_82490_(PaleRules.GRAB_THROW_SPEED * THROW_SHARE * (1.0 - 0.6 * EtoOwl.resist(target))).m_82520_(0.0, 0.35, 0.0);
        Vec3 centre = GhoulNpcBrain.centre(target);
        if (hurt(npc, target, PaleRules.fist(EtoRules.MASTERY) * PaleRules.GRAB_THROW_DAMAGE * phase.power, centre, ahead, WoundBook.Blow.BLUNT)) {
            target.m_20256_(flight);
            target.f_19864_ = true;
            target.f_19789_ = 0.0F;
            PaleKnockdown.launched(target, now);
        }
        f.heldId = 0;
        EtoOwl.fx(npc, S2CPaleFx.IMPACT, target.m_19879_(), centre, flight, 1.6F, S2CPaleFx.FLESH);
        UkakuFire.sound(level, npc.m_20182_(), "hexkagune:kagune_whip", 1.0F, 0.7F);
    }

    /** Pulls the held body toward {@code at} (its middle there), {@code pull} of the way a tick and at most so fast, its fall forgotten. */
    private static void carry(LivingEntity target, Vec3 at, double pull, double most) {
        Vec3 gap = at.m_82546_(GhoulNpcBrain.centre(target));
        double length = gap.m_82553_();
        Vec3 move = length * pull > most ? gap.m_82490_(most / length) : gap.m_82490_(pull);
        target.m_20256_(move);
        target.f_19864_ = true;
        target.f_19812_ = true;
        target.f_19789_ = 0.0F;
    }

    /** Whatever the flesh holds is let go where it is. */
    static void release(ServerLevel level, EtoOwl.Fight f) {
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

    // ---------------------------------------------------------------- the Flesh Hammer

    static void hammer(EtoNpc npc, EtoOwl.Fight f, LivingEntity target, ServerLevel level, long now, EtoRules.Phase phase) {
        npc.m_21573_().m_26573_();
        boolean huge = phase == EtoRules.Phase.MANIAC;
        EtoOwl.begin(npc, f, PaleMove.HAMMER_CHARGE, huge ? 1 : 0, target.m_19879_(), 0.0F, Vec3.f_82478_, now);
        f.hold = EtoRules.hammerHold(phase, npc.m_217043_().m_188500_());
        f.hammerReady = now + EtoRules.cooldown(EtoRules.HAMMER_EVERY, phase);
    }

    /** Rooted on her left leg while the club grows round the right, rising; at the hold she lets it go. */
    private static void tickHammerCharge(EtoNpc npc, EtoOwl.Fight f, ServerLevel level, long now, long age) {
        npc.m_21573_().m_26573_();
        if (age == PaleRules.HAMMER_SPROUT) {
            UkakuFire.sound(level, npc.m_20182_(), "hexkagune:manifest_rip", 0.8F, 0.75F);
        }
        if (age == PaleRules.HAMMER_GROW_TICKS) {
            UkakuFire.sound(level, npc.m_20182_(), "hexkagune:bone_crack", 0.6F, 0.55F);
        }
        if (age >= Math.max(PaleRules.HAMMER_RAISE_TICKS, f.hold)) {
            boolean huge = f.detail == 1;
            float charge = PaleRules.hammerCharge(age, huge);
            float yaw = f.yaw;
            EtoOwl.begin(npc, f, PaleMove.HAMMER, f.detail, f.target, charge, Vec3.f_82478_, now);
            f.yaw = yaw;
            UkakuFire.sound(level, npc.m_20182_(), "hexkagune:kagune_whip", 1.0F, 0.5F);
        }
    }

    private static void tickHammer(EtoNpc npc, EtoBrain.Mind mind, EtoOwl.Fight f, ServerLevel level, long now, long age, EtoRules.Phase phase) {
        npc.m_21573_().m_26573_();
        if (age == PaleRules.HAMMER_IMPACT) {
            lands(npc, mind, f, level, now, phase);
        }
        if (age >= PaleRules.HAMMER_TICKS) {
            EtoOwl.end(f);
        }
    }

    /** The club comes down ahead of her: the shockwave hurts everything round it (most at the middle) and throws it up and away. */
    private static void lands(EtoNpc npc, EtoBrain.Mind mind, EtoOwl.Fight f, ServerLevel level, long now, EtoRules.Phase phase) {
        float charge = f.charge;
        Vec3 facing = EtoOwl.local(f, 0.0, 0.0, 1.0);
        Vec3 head = npc.m_20182_().m_82549_(EtoOwl.local(f, 0.1 * EtoRules.SIZE, 0.0, PaleRules.hammerReach(charge) * EtoRules.SIZE));
        BlockHitResult ground = level.m_45547_(new ClipContext(head.m_82520_(0.0, 1.5, 0.0), head.m_82520_(0.0, -3.5, 0.0), ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, npc));
        Vec3 at = ground.m_6662_() == HitResult.Type.BLOCK ? ground.m_82450_() : new Vec3(head.f_82479_, npc.m_20186_(), head.f_82481_);
        int groundState = net.minecraft.world.level.block.Block.m_49956_(level.m_8055_(net.minecraft.core.BlockPos.m_274561_(at.f_82479_, at.f_82480_ - 0.01,
            at.f_82481_)));
        double radius = PaleRules.hammerRadius(charge) * EtoRules.SIZE;
        AABB area = new AABB(at.f_82479_ - radius - 1.0, at.f_82480_ - 2.0, at.f_82481_ - radius - 1.0, at.f_82479_ + radius + 1.0, at.f_82480_ + 3.0,
            at.f_82481_ + radius + 1.0);
        float damage = PaleRules.hammerDamage(EtoRules.MASTERY, charge) * phase.power;
        for (LivingEntity body : level.m_6443_(LivingEntity.class, area, entity -> EtoOwl.fair(npc, mind, entity))) {
            Vec3 off = body.m_20182_().m_82546_(at);
            double flat = Math.sqrt(off.f_82479_ * off.f_82479_ + off.f_82481_ * off.f_82481_);
            if (flat - body.m_20205_() * 0.5 > radius) {
                continue;
            }
            double near = 1.0 - Math.max(0.0, Math.min(1.0, flat / radius));
            Vec3 away = flat < 0.2 ? facing : new Vec3(off.f_82479_ / flat, 0.0, off.f_82481_ / flat);
            if (!hurt(npc, body, damage * (float)(0.4 + 0.6 * near), body.m_20182_(), away.m_82520_(0.0, 1.0, 0.0), WoundBook.Blow.SLAM)) {
                continue;
            }
            double launch = PaleRules.hammerLaunch(charge) * (0.35 + 0.65 * near) * (1.0 - 0.6 * EtoOwl.resist(body));
            body.m_20256_(away.m_82490_(launch * 0.5).m_82520_(0.0, launch * 0.55 + 0.2, 0.0));
            body.f_19864_ = true;
            PaleKnockdown.launched(body, now);
        }
        EtoOwl.fx(npc, S2CPaleFx.SHOCKWAVE, 0, at, facing, (float)radius, groundState);
        UkakuFire.sound(level, at, "entity.generic.explode", 0.8F + 0.4F * Math.min(1.0F, charge), 0.7F);
        UkakuFire.sound(level, at, "block.anvil.land", 0.6F, 0.5F);
        UkakuFire.sound(level, at, "hexkagune:kagune_slam", 1.0F, 0.6F);
    }

    // ---------------------------------------------------------------- helpers

    /** Her blow on {@code body} (a cut, a blunt blow, a slam): the hurt, the wound where it landed; true if it took it. */
    private static boolean hurt(EtoNpc npc, LivingEntity body, float damage, Vec3 at, Vec3 way, WoundBook.Blow blow) {
        body.f_19802_ = 0;
        WoundHits.expect(body, blow, at, way, 0.0);
        return body.m_6469_(npc.m_269291_().m_269333_(npc), damage);
    }
}
