package com.hex.kagune.control.server;

import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CPaleArm;
import com.hex.kagune.control.network.S2CPaleFx;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.EtoRules;
import com.hex.kagune.control.pale.ArmRules;
import com.hex.kagune.control.pale.PaleRules;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1.3.7: Eto's Mutated Arm, a Pale player's ({@link PaleArm}) on her: her right arm turned to flesh, shot out at what she fights,
 * swinging after it with its weight; whatever its hand meets is caught, hurt and carried right there. Thrown, it drags what it caught
 * back to her and flings it out to her side; held (the maniac, mostly), it swells and reaches on, and at the full charge folds into a
 * sphere over what it caught, crushing it, held out and opened; then it draws back into her. She walks on the whole time. Every client
 * draws it from the same {@link S2CPaleArm} a player's is drawn from (sent by her UUID), on her smaller body: its lengths are sent at a
 * player's size and are {@link EtoRules#SIZE} as long in the world.
 */
final class EtoArm {
    /** Her arm: what it is doing since when, the way it points, how far out (a player's size), what it holds. */
    private static final class Arm {
        ArmRules.Phase phase = ArmRules.Phase.NONE;
        long startedAt;
        long downAt;
        boolean held;
        double[] aim = {0.0, 0.0, 1.0};
        double length = ArmRules.ARM;
        double reached = ArmRules.ARM;
        double charge;
        final List<Integer> caught = new ArrayList<>();
        Vec3 sphere = Vec3.f_82478_;
        double radius = 1.0;
        int target;
        float power = 1.0F;
    }

    /** By her entity id. */
    private static final Map<Integer, Arm> ARMS = new HashMap<>();
    /** Held out this long with nothing caught, she lets it go (ticks into the hold). */
    private static final int EMPTY_HOLD = 24;

    private EtoArm() {
    }

    static boolean out(EtoNpc npc) {
        return ARMS.containsKey(npc.m_19879_());
    }

    /** Out it shoots at {@code target}: held to charge (by her phase, {@link EtoRules#armHeld}) or thrown. */
    static void start(EtoNpc npc, EtoOwl.Fight f, LivingEntity target, ServerLevel level, long now, EtoRules.Phase phase) {
        if (out(npc)) {
            return;
        }
        Arm arm = new Arm();
        arm.target = target.m_19879_();
        arm.power = phase.power;
        Vec3 way = GhoulNpcBrain.centre(target).m_82546_(shoulder(npc));
        if (way.m_82556_() > 1.0E-6) {
            way = way.m_82541_();
            arm.aim = new double[] {way.f_82479_, way.f_82480_, way.f_82481_};
        }
        arm.held = EtoRules.armHeld(phase, npc.m_217043_().m_188500_());
        arm.downAt = now - (arm.held ? PaleRules.HOLD_TICKS : 0);
        ARMS.put(npc.m_19879_(), arm);
        f.armReady = now + EtoRules.cooldown(EtoRules.ARM_EVERY, phase);
        set(npc, arm, ArmRules.Phase.SHOOT, now, ArmRules.ARM);
        UkakuFire.sound(level, npc.m_20182_(), "hexkagune:manifest_rip", 1.0F, 0.8F);
        UkakuFire.sound(level, npc.m_20182_(), "entity.player.attack.sweep", 0.9F, 0.5F);
    }

    static void tick(EtoNpc npc, EtoBrain.Mind mind, ServerLevel level, long now) {
        Arm arm = ARMS.get(npc.m_19879_());
        if (arm == null) {
            return;
        }
        if (!npc.m_6084_()) {
            forget(npc);
            return;
        }
        long age = now - arm.startedAt;
        if (arm.phase == ArmRules.Phase.SHOOT || arm.phase == ArmRules.Phase.HOLD || arm.phase == ArmRules.Phase.DRAG) {
            // It swings after what she fights (as a player's after where she looks), lagging with its weight.
            if (level.m_6815_(arm.target) instanceof LivingEntity target && target.m_6084_()) {
                Vec3 way = GhoulNpcBrain.centre(target).m_82546_(shoulder(npc));
                if (way.m_82556_() > 1.0E-6) {
                    way = way.m_82541_();
                    arm.aim = ArmRules.follow(arm.aim, new double[] {way.f_82479_, way.f_82480_, way.f_82481_});
                }
            }
        }
        Vec3 shoulder = shoulder(npc);
        Vec3 aim = new Vec3(arm.aim[0], arm.aim[1], arm.aim[2]);
        double clear = clear(level, npc, shoulder, aim) / EtoRules.SIZE;
        double length;
        switch (arm.phase) {
            case SHOOT -> {
                length = Math.min(clear, ArmRules.shot(age, ArmRules.shootTo(arm.startedAt - arm.downAt)));
                if (!arm.held && !arm.caught.isEmpty()) {
                    set(npc, arm, ArmRules.Phase.DRAG, now, arm.reached);
                    length = arm.reached;
                } else if (age >= ArmRules.SHOOT_TICKS) {
                    set(npc, arm, arm.held ? ArmRules.Phase.HOLD : ArmRules.Phase.DRAG, now, length);
                }
            }
            case HOLD -> {
                arm.charge = ArmRules.charge(now - arm.downAt);
                length = Math.min(clear, ArmRules.reach(arm.charge));
                if (!arm.caught.isEmpty()) {
                    length = Math.max(length, ArmRules.dragTo(arm.charge));
                } else if (age >= EMPTY_HOLD) {
                    arm.held = false; // nothing caught: she lets it go
                }
                if (!arm.held) {
                    set(npc, arm, ArmRules.Phase.DRAG, now, length);
                } else if (arm.charge >= 1.0) {
                    double tallest = 0.0;
                    for (LivingEntity body : bodies(level, arm)) {
                        tallest = Math.max(tallest, body.m_20206_());
                    }
                    arm.radius = ArmRules.sphereRadius(tallest / EtoRules.SIZE);
                    length = ArmRules.sphereOut(length, arm.radius);
                    arm.sphere = shoulder.m_82549_(aim.m_82490_(length * EtoRules.SIZE));
                    set(npc, arm, ArmRules.Phase.SPHERE, now, length);
                    UkakuFire.sound(level, npc.m_20182_(), "hexkagune:kagune_strain", 1.0F, 0.6F);
                }
            }
            case DRAG -> {
                length = ArmRules.dragged(age, arm.length, arm.charge);
                if (age >= ArmRules.DRAG_TICKS) {
                    fling(npc, arm, level, now);
                    set(npc, arm, ArmRules.Phase.RETRACT, now, length);
                }
            }
            case SPHERE -> {
                length = arm.sphere.m_82554_(shoulder) / EtoRules.SIZE;
                for (int i = 0; i < ArmRules.SQUEEZES; i++) {
                    if (age == ArmRules.squeezeTick(i)) {
                        squeeze(npc, arm, level, i);
                    }
                }
                boolean torn = length > ArmRules.MAX_REACH + 4.0;
                if (torn) {
                    length = ArmRules.MAX_REACH;
                }
                if (age == ArmRules.SPHERE_FORM + ArmRules.SPHERE_CRUSH + ArmRules.SPHERE_HELD || torn && !arm.caught.isEmpty()) {
                    for (LivingEntity body : bodies(level, arm)) {
                        body.m_20256_(new Vec3(0.0, 0.1, 0.0));
                        body.f_19864_ = true;
                    }
                    arm.caught.clear();
                    set(npc, arm, ArmRules.Phase.SPHERE, arm.startedAt, arm.length);
                }
                if (age >= ArmRules.SPHERE_TICKS || torn) {
                    Vec3 back = arm.sphere.m_82546_(shoulder);
                    if (back.m_82556_() > 1.0E-6) {
                        back = back.m_82541_();
                        arm.aim = new double[] {back.f_82479_, back.f_82480_, back.f_82481_};
                    }
                    set(npc, arm, ArmRules.Phase.RETRACT, now, length);
                }
            }
            case RETRACT -> {
                int ticks = arm.sphere != Vec3.f_82478_ ? ArmRules.RETRACT_TICKS : ArmRules.RETRACT_QUICK;
                length = ArmRules.retracted(age, arm.length, ticks);
                if (age >= ticks) {
                    ARMS.remove(npc.m_19879_());
                    set(npc, arm, ArmRules.Phase.NONE, now, ArmRules.ARM);
                    return;
                }
            }
            default -> length = ArmRules.ARM;
        }
        if (arm.phase == ArmRules.Phase.SHOOT || arm.phase == ArmRules.Phase.HOLD) {
            double from = arm.phase == ArmRules.Phase.SHOOT && age <= 1 ? 0.0 : arm.reached;
            double stopped = grab(npc, mind, arm, level, shoulder, aim, from, length);
            if (stopped >= 0.0) {
                length = Math.min(length, Math.max(ArmRules.ARM, stopped));
            }
        }
        arm.reached = length;
        Vec3 hand = arm.phase == ArmRules.Phase.SPHERE ? arm.sphere : shoulder.m_82549_(aim.m_82490_(length * EtoRules.SIZE));
        carry(npc, arm, level, hand, now);
    }

    /**
     * Whatever its hand meets between {@code from} and {@code to} out of her shoulder (a player's size) is caught, hurt; returns how far
     * out the first new catch was (the hand stops on it), or -1.
     */
    private static double grab(EtoNpc npc, EtoBrain.Mind mind, Arm arm, ServerLevel level, Vec3 shoulder, Vec3 aim, double from, double to) {
        if (arm.caught.size() >= ArmRules.MOST) {
            return -1.0;
        }
        double k = EtoRules.SIZE;
        double reach = ArmRules.CATCH + 1.5;
        Vec3 a = shoulder.m_82549_(aim.m_82490_(Math.min(from, to) * k));
        Vec3 b = shoulder.m_82549_(aim.m_82490_(Math.max(from, to) * k));
        AABB swept = new AABB(Math.min(a.f_82479_, b.f_82479_) - reach, Math.min(a.f_82480_, b.f_82480_) - reach, Math.min(a.f_82481_, b.f_82481_) - reach,
            Math.max(a.f_82479_, b.f_82479_) + reach, Math.max(a.f_82480_, b.f_82480_) + reach, Math.max(a.f_82481_, b.f_82481_) + reach);
        double[] s = {shoulder.f_82479_, shoulder.f_82480_, shoulder.f_82481_};
        double[] way = {aim.f_82479_, aim.f_82480_, aim.f_82481_};
        List<LivingEntity> met = new ArrayList<>();
        List<Double> at = new ArrayList<>();
        for (LivingEntity body : level.m_6443_(LivingEntity.class, swept, entity -> EtoOwl.fair(npc, mind, entity))) {
            if (arm.caught.contains(body.m_19879_())) {
                continue;
            }
            Vec3 c = GhoulNpcBrain.centre(body);
            double[] centre = {c.f_82479_, c.f_82480_, c.f_82481_};
            double t = ArmRules.along(s, way, centre, from * k, to * k);
            if (ArmRules.off(s, way, centre, t) > ArmRules.CATCH * k + body.m_20205_() * 0.5 + body.m_20206_() * 0.3) {
                continue;
            }
            int i = 0;
            while (i < at.size() && at.get(i) <= t / k) {
                i++;
            }
            met.add(i, body);
            at.add(i, t / k);
        }
        double first = -1.0;
        boolean caught = false;
        for (int i = 0; i < met.size() && arm.caught.size() < ArmRules.MOST; i++) {
            LivingEntity body = met.get(i);
            body.f_19802_ = 0;
            WoundHits.expect(body, com.hex.kagune.control.wound.WoundBook.Blow.CRUSH, GhoulNpcBrain.centre(body), aim, 0.0);
            if (!body.m_6469_(npc.m_269291_().m_269333_(npc), PaleRules.fist(EtoRules.MASTERY) * ArmRules.GRAB_DAMAGE * arm.power)) {
                continue;
            }
            if (first < 0.0) {
                first = at.get(i);
            }
            arm.caught.add(body.m_19879_());
            caught = true;
            EtoOwl.fx(npc, S2CPaleFx.CRUSHED, body.m_19879_(), GhoulNpcBrain.centre(body), aim, 0.5F, 0);
            UkakuFire.sound(level, GhoulNpcBrain.centre(body), "hexkagune:bone_crack", 0.7F, 1.0F);
        }
        if (caught) {
            set(npc, arm, arm.phase, arm.startedAt, arm.length);
        }
        return first;
    }

    /** What it holds, held at its hand (the first right there, the rest packed round it), nothing else moving them. */
    private static void carry(EtoNpc npc, Arm arm, ServerLevel level, Vec3 hand, long now) {
        List<LivingEntity> bodies = bodies(level, arm);
        for (int i = 0; i < bodies.size(); i++) {
            LivingEntity body = bodies.get(i);
            double turn = i * 2.4;
            double out = i == 0 ? 0.0 : 0.35 + 0.1 * i;
            Vec3 at = hand.m_82520_(Math.cos(turn) * out, -body.m_20206_() * 0.5 + (i == 0 ? 0.0 : Math.sin(turn) * out * 0.5), Math.sin(turn) * out);
            place(level, body, at, now);
        }
        if (bodies.size() != arm.caught.size()) {
            arm.caught.removeIf(id -> bodies.stream().noneMatch(body -> body.m_19879_() == id));
            set(npc, arm, arm.phase, arm.startedAt, arm.length);
        }
    }

    /** Held right there (its feet at {@code feet}), unable to act, no fall to take. */
    private static void place(ServerLevel level, LivingEntity body, Vec3 feet, long now) {
        PaleStun.stun(body, now, 2);
        body.f_19789_ = 0.0F;
        if (body instanceof ServerPlayer player && player.m_20182_().m_82554_(feet) <= 1.5) {
            player.m_20256_(feet.m_82546_(player.m_20182_()).m_82490_(0.5));
            player.f_19864_ = true;
            return;
        }
        Vec3 offset = feet.m_82546_(body.m_20182_());
        if (level.m_45756_(body, body.m_20191_().m_82386_(offset.f_82479_, offset.f_82480_, offset.f_82481_))) {
            body.m_6021_(feet.f_82479_, feet.f_82480_, feet.f_82481_);
        }
        body.m_20256_(Vec3.f_82478_);
        body.f_19864_ = true;
    }

    /** One of the sphere's squeezes: everything in it crushed. */
    private static void squeeze(EtoNpc npc, Arm arm, ServerLevel level, int index) {
        float damage = PaleRules.fist(EtoRules.MASTERY) * ArmRules.CRUSH_DAMAGE * arm.power;
        for (LivingEntity body : bodies(level, arm)) {
            body.f_19802_ = 0;
            WoundHits.expect(body, com.hex.kagune.control.wound.WoundBook.Blow.CRUSH, GhoulNpcBrain.centre(body), new Vec3(0.0, -1.0, 0.0), 0.0);
            body.m_6469_(npc.m_269291_().m_269333_(npc), damage);
        }
        EtoOwl.fx(npc, S2CPaleFx.CRUSHED, 0, arm.sphere, Vec3.f_82478_, (float)(arm.radius * EtoRules.SIZE), index);
        UkakuFire.sound(level, arm.sphere, "entity.slime.squish", 1.4F, 0.5F);
        UkakuFire.sound(level, arm.sphere, "hexkagune:bone_crack", 1.0F, 0.6F);
    }

    /** The drag's end: what it holds flung out to her side and up. */
    private static void fling(EtoNpc npc, Arm arm, ServerLevel level, long now) {
        double yaw = Math.toRadians(npc.m_146908_());
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        for (LivingEntity body : bodies(level, arm)) {
            body.m_20256_(right.m_82490_(ArmRules.FLING * (1.0 - 0.6 * EtoOwl.resist(body))).m_82520_(0.0, ArmRules.FLING_UP, 0.0));
            body.f_19864_ = true;
            PaleKnockdown.launched(body, now);
        }
        arm.caught.clear();
        UkakuFire.sound(level, npc.m_20182_(), "entity.player.attack.knockback", 1.0F, 0.6F);
    }

    private static List<LivingEntity> bodies(ServerLevel level, Arm arm) {
        List<LivingEntity> out = new ArrayList<>();
        for (int id : arm.caught) {
            if (level.m_6815_(id) instanceof LivingEntity body && body.m_6084_()) {
                out.add(body);
            }
        }
        return out;
    }

    /** How far (blocks, the world) the arm can reach from {@code from} along {@code aim} before something solid. */
    private static double clear(ServerLevel level, EtoNpc npc, Vec3 from, Vec3 aim) {
        double most = ArmRules.MAX_REACH * EtoRules.SIZE;
        Vec3 end = from.m_82549_(aim.m_82490_(most));
        HitResult hit = level.m_45547_(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, npc));
        return hit.m_6662_() == HitResult.Type.MISS ? most : Math.max(ArmRules.ARM * EtoRules.SIZE, hit.m_82450_().m_82554_(from) - 0.4);
    }

    /** Her right shoulder (the world), where the arm comes out (a player's, at her size). */
    private static Vec3 shoulder(EtoNpc npc) {
        double yaw = Math.toRadians(npc.f_20883_);
        double k = EtoRules.SIZE;
        return npc.m_20182_().m_82520_(-Math.cos(yaw) * 0.33 * k, 1.32 * k, -Math.sin(yaw) * 0.33 * k);
    }

    private static void set(EtoNpc npc, Arm arm, ArmRules.Phase phase, long at, double length) {
        arm.phase = phase;
        arm.startedAt = at;
        arm.length = length;
        ControlNetwork.broadcastAround(npc, message(npc, arm));
    }

    private static S2CPaleArm message(EtoNpc npc, Arm arm) {
        int[] caught = new int[arm.caught.size()];
        for (int i = 0; i < caught.length; i++) {
            caught[i] = arm.caught.get(i);
        }
        return new S2CPaleArm(npc.m_20148_(), arm.phase.ordinal(), arm.startedAt, arm.downAt, (float)arm.length, (float)arm.charge,
            (float)arm.aim[0], (float)arm.aim[1], (float)arm.aim[2], arm.sphere.f_82479_, arm.sphere.f_82480_, arm.sphere.f_82481_, (float)arm.radius, caught);
    }

    /** {@code viewer} has just come to see her: it learns where her arm is, if it is out. */
    static void tracked(ServerPlayer viewer, EtoNpc npc) {
        Arm arm = ARMS.get(npc.m_19879_());
        if (arm != null) {
            ControlNetwork.sendTo(viewer, message(npc, arm));
        }
    }

    /** Her arm goes at once (she died, she is gone): what it holds is let go where it is. */
    static void forget(EtoNpc npc) {
        Arm arm = ARMS.remove(npc.m_19879_());
        if (arm != null) {
            arm.caught.clear();
            set(npc, arm, ArmRules.Phase.NONE, npc.m_9236_().m_46467_(), ArmRules.ARM);
        }
    }
}
