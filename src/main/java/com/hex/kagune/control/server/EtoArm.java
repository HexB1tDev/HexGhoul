package com.hex.kagune.control.server;

import com.hex.kagune.control.eto.ArmRules;
import com.hex.kagune.control.eto.EtoRules;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CEtoArm;
import com.hex.kagune.control.network.S2CEtoFx;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.network.KaguneNetwork;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 6.2.0, the Mutated Arm (N; {@link ArmRules}): her right arm turned to her flesh, shot out where she looks. Its hand is where the
 * arm reaches along the way it points (which follows where she looks, lagging with its weight); whatever its hand meets is caught,
 * hurt, and carried right there, every tick. Tapped, it drags what it caught back to her and flings it; held, it swells and reaches
 * further, and at the full charge folds in over what it caught into a sphere that crushes it, held out on the stretched arm, and
 * opens; then the arm draws back into her. She walks the whole time. Every client draws it from {@link S2CEtoArm}.
 */
final class EtoArm {
    /** Her arm: what it is doing since when, the way it points, how far out it was as this began, what it holds. */
    static final class Arm {
        ArmRules.Phase phase = ArmRules.Phase.NONE;
        long startedAt;
        /** When N went down (its charge counts from there), whether it is still down, and when that was last heard. */
        long downAt;
        boolean held;
        long heardAt;
        double[] aim = {0.0, 0.0, 1.0};
        double length = ArmRules.ARM;
        /** How far out its hand was last tick. */
        double reached = ArmRules.ARM;
        double charge;
        final List<Integer> caught = new ArrayList<>();
        Vec3 sphere = Vec3.f_82478_;
        double radius = 1.0;
    }

    private static final Map<UUID, Arm> ARMS = new HashMap<>();

    private EtoArm() {
    }

    /** N tapped ({@code hold} false) or held: out it shoots, if she can (her mastery, her RC, nothing else going on). */
    static void start(ServerPlayer player, EtoServer.EtoState state, boolean hold, long now) {
        if (ARMS.containsKey(player.m_20148_()) || !EtoCancel.allowed(state, now) || now < state.armReadyAt
            || EtoServer.locked(player, state, "Mutated Arm", ArmRules.MASTERY)) {
            return;
        }
        if (!KaguneServerController.useEnergy(player, ArmRules.COST, true)) {
            return;
        }
        // 6.2.0: whatever she was doing is cut off (she walks with the arm out: her body is her own again).
        if (state.move != com.hex.kagune.control.eto.EtoMove.NONE) {
            EtoCancel.cut(player, state, now);
            EtoServer.broadcast(player, state);
        }
        state.queuedAt = Long.MIN_VALUE;
        Arm arm = new Arm();
        Vec3 look = player.m_20154_();
        arm.aim = new double[] {look.f_82479_, look.f_82480_, look.f_82481_};
        arm.downAt = now - (hold ? EtoRules.HOLD_TICKS : 0);
        arm.held = hold;
        arm.heardAt = now;
        ARMS.put(player.m_20148_(), arm);
        state.armOut = true;
        set(player, arm, ArmRules.Phase.SHOOT, now, ArmRules.ARM);
        KaguneServerController.playSelf(player, HexKaguneSounds.MANIFEST_RIP, 1.0F, KaguneServerController.vary(player, 0.8F, 0.05F));
        AyatoFire.sound(player.m_284548_(), player.m_20182_(), "entity.player.attack.sweep", 0.9F, KaguneServerController.vary(player, 0.5F, 0.05F));
    }

    /** N held: {@code 2} as the hold starts (out it shoots), {@code 1} still down, {@code 0} let go. */
    static void key(ServerPlayer player, EtoServer.EtoState state, int down, long now) {
        Arm arm = ARMS.get(player.m_20148_());
        if (arm == null) {
            if (down == 2) {
                start(player, state, true, now);
            }
            return;
        }
        if (down == 2 && arm.phase != ArmRules.Phase.SHOOT && arm.phase != ArmRules.Phase.HOLD) {
            return; // a new hold while the last is still on its way back in
        }
        arm.held = down != 0;
        arm.heardAt = now;
    }

    /** Whether her arm is out. */
    static boolean out(ServerPlayer player) {
        return ARMS.containsKey(player.m_20148_());
    }

    static void tick(ServerPlayer player, EtoServer.EtoState state, long now) {
        Arm arm = ARMS.get(player.m_20148_());
        if (arm == null) {
            return;
        }
        ServerLevel level = player.m_284548_();
        long age = now - arm.startedAt;
        Vec3 look = player.m_20154_();
        if (arm.phase == ArmRules.Phase.SHOOT || arm.phase == ArmRules.Phase.HOLD || arm.phase == ArmRules.Phase.DRAG) {
            arm.aim = ArmRules.follow(arm.aim, new double[] {look.f_82479_, look.f_82480_, look.f_82481_});
        }
        if (arm.held && now - arm.heardAt > EtoRules.HOLD_TIMEOUT) {
            arm.held = false;
        }
        Vec3 shoulder = shoulder(player);
        Vec3 aim = new Vec3(arm.aim[0], arm.aim[1], arm.aim[2]);
        double clear = clear(level, player, shoulder, aim);
        double length;
        switch (arm.phase) {
            case SHOOT -> {
                length = Math.min(clear, ArmRules.shot(age, ArmRules.shootTo(arm.startedAt - arm.downAt)));
                if (!arm.held && !arm.caught.isEmpty()) {
                    // Tapped, it yanks what it caught straight back.
                    set(player, arm, ArmRules.Phase.DRAG, now, arm.reached);
                    length = arm.reached;
                } else if (age >= ArmRules.SHOOT_TICKS) {
                    if (arm.held) {
                        set(player, arm, ArmRules.Phase.HOLD, now, length);
                    } else {
                        set(player, arm, ArmRules.Phase.DRAG, now, length);
                    }
                }
            }
            case HOLD -> {
                arm.charge = ArmRules.charge(now - arm.downAt);
                length = Math.min(clear, ArmRules.reach(arm.charge));
                if (!arm.caught.isEmpty()) {
                    length = Math.max(length, ArmRules.dragTo(arm.charge)); // what it holds kept off her, wall or not
                }
                if (!arm.held) {
                    set(player, arm, ArmRules.Phase.DRAG, now, length);
                } else if (arm.charge >= 1.0) {
                    if (arm.caught.isEmpty() || !KaguneServerController.useEnergy(player, ArmRules.SPHERE_COST, true)) {
                        set(player, arm, ArmRules.Phase.RETRACT, now, length);
                    } else {
                        double tallest = 0.0;
                        for (LivingEntity body : bodies(player, arm)) {
                            tallest = Math.max(tallest, body.m_20206_());
                        }
                        arm.radius = ArmRules.sphereRadius(tallest);
                        length = ArmRules.sphereOut(length, arm.radius);
                        arm.sphere = shoulder.m_82549_(aim.m_82490_(length));
                        set(player, arm, ArmRules.Phase.SPHERE, now, length);
                        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_STRAIN, 1.0F, KaguneServerController.vary(player, 0.6F, 0.05F));
                    }
                }
            }
            case DRAG -> {
                length = ArmRules.dragged(age, arm.length, arm.charge);
                if (age >= ArmRules.DRAG_TICKS) {
                    fling(player, arm);
                    set(player, arm, ArmRules.Phase.RETRACT, now, length);
                }
            }
            case SPHERE -> {
                length = arm.sphere.m_82554_(shoulder);
                for (int i = 0; i < ArmRules.SQUEEZES; i++) {
                    if (age == ArmRules.squeezeTick(i)) {
                        squeeze(player, arm, i);
                    }
                }
                boolean torn = length > ArmRules.MAX_REACH + 4.0; // she was taken off far from it (a teleport): it lets go at once
                if (torn) {
                    length = ArmRules.MAX_REACH;
                }
                if (age == ArmRules.SPHERE_FORM + ArmRules.SPHERE_CRUSH + ArmRules.SPHERE_HELD || torn && !arm.caught.isEmpty()) {
                    // It opens: what is left of them drops out.
                    for (LivingEntity body : bodies(player, arm)) {
                        body.m_20256_(new Vec3(0.0, 0.1, 0.0));
                        body.f_19864_ = true;
                    }
                    arm.caught.clear();
                    set(player, arm, ArmRules.Phase.SPHERE, arm.startedAt, arm.length);
                }
                if (age >= ArmRules.SPHERE_TICKS || torn) {
                    // It draws back in from the sphere where it is now (she may have walked).
                    Vec3 back = arm.sphere.m_82546_(shoulder);
                    if (back.m_82556_() > 1.0E-6) {
                        back = back.m_82541_();
                        arm.aim = new double[] {back.f_82479_, back.f_82480_, back.f_82481_};
                    }
                    set(player, arm, ArmRules.Phase.RETRACT, now, length);
                }
            }
            case RETRACT -> {
                length = ArmRules.retracted(age, arm.length, retractTicks(arm));
                if (age >= retractTicks(arm)) {
                    ARMS.remove(player.m_20148_());
                    state.armOut = false;
                    state.armReadyAt = now + Cooldowns.of(player, ArmRules.COOLDOWN);
                    set(player, arm, ArmRules.Phase.NONE, now, ArmRules.ARM);
                    return;
                }
            }
            default -> length = ArmRules.ARM;
        }
        if (arm.phase == ArmRules.Phase.SHOOT || arm.phase == ArmRules.Phase.HOLD) {
            // 6.2.0: the hand sweeps all it went past since last tick (from her shoulder on the first): what it meets stops it there.
            double from = arm.phase == ArmRules.Phase.SHOOT && age <= 1 ? 0.0 : arm.reached;
            double stopped = grab(player, arm, shoulder, aim, from, length);
            if (stopped >= 0.0) {
                length = Math.min(length, Math.max(ArmRules.ARM, stopped));
            }
        }
        arm.reached = length;
        Vec3 hand = arm.phase == ArmRules.Phase.SPHERE ? arm.sphere : shoulder.m_82549_(aim.m_82490_(length));
        carry(player, arm, hand);
    }

    /** After the sphere it draws back in slowly; after a drag, quickly. */
    static int retractTicks(Arm arm) {
        return arm.radius > 0.0 && arm.sphere != Vec3.f_82478_ ? ArmRules.RETRACT_TICKS : ArmRules.RETRACT_QUICK;
    }

    /**
     * Whatever its hand meets is caught: hurt as the hand closes on it, and carried. 6.2.0: the hand is swept from {@code from} to
     * {@code to} blocks out of the shoulder along {@code aim} (all it went past since last tick), nearest first, so nothing close to
     * her or between two ticks' reach is missed. Returns how far out the first new catch was (the hand stops on it), or -1.
     */
    private static double grab(ServerPlayer player, Arm arm, Vec3 shoulder, Vec3 aim, double from, double to) {
        if (arm.caught.size() >= ArmRules.MOST) {
            return -1.0;
        }
        ServerLevel level = player.m_284548_();
        double reach = ArmRules.CATCH + 1.5;
        Vec3 a = shoulder.m_82549_(aim.m_82490_(Math.min(from, to)));
        Vec3 b = shoulder.m_82549_(aim.m_82490_(Math.max(from, to)));
        AABB swept = new AABB(Math.min(a.f_82479_, b.f_82479_) - reach, Math.min(a.f_82480_, b.f_82480_) - reach, Math.min(a.f_82481_, b.f_82481_) - reach,
            Math.max(a.f_82479_, b.f_82479_) + reach, Math.max(a.f_82480_, b.f_82480_) + reach, Math.max(a.f_82481_, b.f_82481_) + reach);
        double[] s = {shoulder.f_82479_, shoulder.f_82480_, shoulder.f_82481_};
        double[] way = {aim.f_82479_, aim.f_82480_, aim.f_82481_};
        List<LivingEntity> met = new ArrayList<>();
        List<Double> at = new ArrayList<>();
        for (LivingEntity body : level.m_6443_(LivingEntity.class, swept, entity -> EtoBlitz.valid(player, entity))) {
            if (arm.caught.contains(body.m_19879_())) {
                continue;
            }
            Vec3 c = KaguneServerController.center(body);
            double[] centre = {c.f_82479_, c.f_82480_, c.f_82481_};
            double t = ArmRules.along(s, way, centre, from, to);
            if (ArmRules.off(s, way, centre, t) > ArmRules.CATCH + body.m_20205_() * 0.5 + body.m_20206_() * 0.3) {
                continue;
            }
            int i = 0;
            while (i < at.size() && at.get(i) <= t) {
                i++;
            }
            met.add(i, body);
            at.add(i, t);
        }
        double first = -1.0;
        boolean caught = false;
        for (int i = 0; i < met.size() && arm.caught.size() < ArmRules.MOST; i++) {
            LivingEntity body = met.get(i);
            body.f_19802_ = 0;
            WoundHits.expect(body, com.hex.kagune.control.wound.WoundBook.Blow.CRUSH, KaguneServerController.center(body), aim, 0.0);
            if (!body.m_6469_(player.m_269291_().m_269075_(player), EtoRules.fist(KaguneProfile.mastery(player)) * ArmRules.GRAB_DAMAGE)) {
                continue;
            }
            if (first < 0.0) {
                first = at.get(i);
            }
            arm.caught.add(body.m_19879_());
            caught = true;
            EtoBlitz.fx(player, S2CEtoFx.CRUSHED, body.m_19879_(), KaguneServerController.center(body), aim, 0.5F, 0);
            KaguneServerController.playAt(player, body, HexKaguneSounds.BONE_CRACK, 0.7F, KaguneServerController.vary(player, 1.0F, 0.06F));
        }
        if (caught) {
            KaguneProfile.addMastery(player, "Swipe", 1);
            KaguneNetwork.broadcast(player);
            EtoServer.landed(player, arm.caught.size());
            set(player, arm, arm.phase, arm.startedAt, arm.length);
        }
        return first;
    }

    /** What it holds, held at its hand (the first right there, the rest packed round it), nothing else moving them. */
    private static void carry(ServerPlayer player, Arm arm, Vec3 hand) {
        List<LivingEntity> bodies = bodies(player, arm);
        for (int i = 0; i < bodies.size(); i++) {
            LivingEntity body = bodies.get(i);
            double turn = i * 2.4;
            double out = i == 0 ? 0.0 : 0.35 + 0.1 * i;
            Vec3 at = hand.m_82520_(Math.cos(turn) * out, -body.m_20206_() * 0.5 + (i == 0 ? 0.0 : Math.sin(turn) * out * 0.5), Math.sin(turn) * out);
            CentipedeHunt.place(player, body, at);
        }
        if (bodies.size() != arm.caught.size()) {
            arm.caught.removeIf(id -> bodies.stream().noneMatch(body -> body.m_19879_() == id));
            set(player, arm, arm.phase, arm.startedAt, arm.length);
        }
    }

    /** One of the sphere's squeezes: everything in it crushed, blood forced out of it. */
    private static void squeeze(ServerPlayer player, Arm arm, int index) {
        float damage = EtoRules.fist(KaguneProfile.mastery(player)) * ArmRules.CRUSH_DAMAGE;
        for (LivingEntity body : bodies(player, arm)) {
            body.f_19802_ = 0;
            WoundHits.expect(body, com.hex.kagune.control.wound.WoundBook.Blow.CRUSH, KaguneServerController.center(body), new Vec3(0.0, -1.0, 0.0), 0.0);
            body.m_6469_(player.m_269291_().m_269075_(player), damage);
        }
        EtoBlitz.fx(player, S2CEtoFx.CRUSHED, 0, arm.sphere, Vec3.f_82478_, (float)arm.radius, index);
        AyatoFire.sound(player.m_284548_(), arm.sphere, "entity.slime.squish", 1.4F, KaguneServerController.vary(player, 0.5F, 0.05F));
        KaguneServerController.playAt(player, player, HexKaguneSounds.BONE_CRACK, 1.0F, KaguneServerController.vary(player, 0.6F, 0.05F));
    }

    /** The drag's end: what it holds flung out to her side and up. */
    private static void fling(ServerPlayer player, Arm arm) {
        double yaw = Math.toRadians(player.m_146908_());
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        for (LivingEntity body : bodies(player, arm)) {
            BlockSmash.launch(player, body, right.m_82490_(ArmRules.FLING).m_82520_(0.0, ArmRules.FLING_UP, 0.0), 30);
        }
        arm.caught.clear();
        AyatoFire.sound(player.m_284548_(), player.m_20182_(), "entity.player.attack.knockback", 1.0F, KaguneServerController.vary(player, 0.6F, 0.05F));
    }

    /** What it holds that is still there. */
    static List<LivingEntity> bodies(ServerPlayer player, Arm arm) {
        List<LivingEntity> out = new ArrayList<>();
        for (int id : arm.caught) {
            if (player.m_9236_().m_6815_(id) instanceof LivingEntity body && body.m_6084_()) {
                out.add(body);
            }
        }
        return out;
    }

    /** How far (blocks) the arm can reach from {@code from} along {@code aim} before something solid. */
    static double clear(ServerLevel level, ServerPlayer player, Vec3 from, Vec3 aim) {
        Vec3 end = from.m_82549_(aim.m_82490_(ArmRules.MAX_REACH));
        HitResult hit = level.m_45547_(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.m_6662_() == HitResult.Type.MISS ? ArmRules.MAX_REACH : Math.max(ArmRules.ARM, hit.m_82450_().m_82554_(from) - 0.4);
    }

    /** Her right shoulder (the world), where the arm comes out. */
    static Vec3 shoulder(ServerPlayer player) {
        double yaw = Math.toRadians(player.f_20883_);
        return player.m_20182_().m_82520_(-Math.cos(yaw) * 0.33, 1.32, -Math.sin(yaw) * 0.33);
    }

    /** The arm starts something new (at {@code at}), out {@code length} as it does, and every client is told. */
    private static void set(ServerPlayer player, Arm arm, ArmRules.Phase phase, long at, double length) {
        arm.phase = phase;
        arm.startedAt = at;
        arm.length = length;
        ControlNetwork.broadcast(player, message(player, arm));
    }

    private static S2CEtoArm message(ServerPlayer player, Arm arm) {
        int[] caught = new int[arm.caught.size()];
        for (int i = 0; i < caught.length; i++) {
            caught[i] = arm.caught.get(i);
        }
        return new S2CEtoArm(player.m_20148_(), arm.phase.ordinal(), arm.startedAt, arm.downAt, (float)arm.length, (float)arm.charge,
            (float)arm.aim[0], (float)arm.aim[1], (float)arm.aim[2], arm.sphere.f_82479_, arm.sphere.f_82480_, arm.sphere.f_82481_, (float)arm.radius, caught);
    }

    /** {@code viewer} has just come to see {@code subject}: it learns where her arm is, if it is out. */
    static void tracked(ServerPlayer viewer, ServerPlayer subject) {
        Arm arm = ARMS.get(subject.m_20148_());
        if (arm != null) {
            ControlNetwork.sendTo(viewer, message(subject, arm));
        }
    }

    /**
     * 6.2.0: another move cuts it off ({@link EtoCancel}): what it holds is let go where it is and it draws straight back into her
     * from as far out as it is (its cooldown charged once it is in, as always); nothing else waits on it meanwhile.
     */
    static void cut(ServerPlayer player, EtoServer.EtoState state, long now) {
        Arm arm = ARMS.get(player.m_20148_());
        state.armOut = false;
        if (arm == null || arm.phase == ArmRules.Phase.RETRACT) {
            return;
        }
        for (LivingEntity body : bodies(player, arm)) {
            body.m_20256_(new Vec3(0.0, 0.1, 0.0));
            body.f_19864_ = true;
        }
        arm.caught.clear();
        arm.held = false;
        set(player, arm, ArmRules.Phase.RETRACT, now, arm.reached);
    }

    /** Her arm goes at once (her form put away, death, another world, gone): what it holds is let go where it is. */
    static void forget(ServerPlayer player, EtoServer.EtoState state) {
        Arm arm = ARMS.remove(player.m_20148_());
        if (state != null) {
            state.armOut = false;
        }
        if (arm != null) {
            arm.caught.clear();
            set(player, arm, ArmRules.Phase.NONE, player.m_9236_().m_46467_(), ArmRules.ARM);
        }
    }
}
