package com.hex.kagune.control.server;

import com.hex.kagune.control.pale.PaleMove;
import com.hex.kagune.control.pale.PaleRules;
import com.hex.kagune.control.network.S2CPaleFx;
import com.hex.kagune.control.wound.WoundBook;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.network.KaguneNetwork;
import com.hex.kagune.sound.HexKaguneSounds;
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
 * Pale's pale flesh (6.1.0): out of her upper back, over her right shoulder.
 *
 * <p>J tapped, the swipe: the mass erupts and rears, then rakes across everything in front of her from her right round to her
 * left, out to {@link PaleRules#SWIPE_REACH} blocks, flinging what it catches to her left (through weak blocks), and draws back in.
 *
 * <p>J held, the grab: the flesh flies at what she looks at (in range, in sight), its tip opening into a hand; it closes round the
 * body and holds it up before her, wherever she turns; let J go (or hold it as long as it can) and she draws it back over her
 * shoulder and whips it forward, flinging the body clean through weak blocks, and the flesh draws back in. Nothing is ever held
 * for good: a held body is let go the moment she can no longer hold it (no RC, the form put away, death, another world, a
 * lost key), and a missed grab just snaps back.
 */
final class PaleFlesh {
    /** How hard the hold pulls the body to where she holds it (a fraction of the gap a tick, at most so fast). */
    private static final double PULL = 0.55;
    private static final double PULL_MAX = 1.6;
    /** The most (blocks) the throw moves the body in a tick: it keeps up with the whip. */
    private static final double THROW_MAX = 9.0;

    private PaleFlesh() {
    }

    // ------------------------------------------------------------------ the swipe

    static void swipe(ServerPlayer player, PaleServer.PaleState state, long now) {
        if (!PaleCancel.allowed(state, now) || now < state.swipeReadyAt
            || PaleServer.locked(player, state, "Flesh Swipe", PaleRules.SWIPE_MASTERY)) {
            return;
        }
        if (!KaguneServerController.useEnergy(player, PaleRules.SWIPE_COST, true)) {
            return;
        }
        PaleCancel.cut(player, state, now);
        state.queuedAt = Long.MIN_VALUE;
        PaleServer.begin(player, state, PaleMove.SWIPE, 0, 0, 0.0F, null, now);
        state.swipeReadyAt = now + Cooldowns.of(player, PaleRules.SWIPE_COOLDOWN);
        KaguneServerController.playSelf(player, HexKaguneSounds.MANIFEST_RIP, 0.8F, KaguneServerController.vary(player, 1.15F, 0.05F));
    }

    private static void tickSwipe(ServerPlayer player, PaleServer.PaleState state, long now) {
        long age = now - state.startedAt;
        if (age == PaleRules.SWIPE_ERUPT) {
            KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 1.0F, KaguneServerController.vary(player, 0.55F, 0.05F));
        }
        if (age > PaleRules.SWIPE_ERUPT && age <= PaleRules.SWIPE_SWEEP_END + 1 + Math.ceil(PaleRules.SWIPE_LAG)) {
            sweep(player, state, age);
        }
        if (age >= PaleRules.SWIPE_TICKS) {
            PaleServer.end(player, state);
        }
    }

    /**
     * One tick of the blade's cut: everything whose bearing it crossed since the last tick (where it is, trailing the flesh's path
     * by its weight), within its reach, is cut once: it bleeds, and is thrown the way the blade swings.
     */
    private static void sweep(ServerPlayer player, PaleServer.PaleState state, long age) {
        ServerLevel level = player.m_284548_();
        double yaw = Math.toRadians(state.yaw);
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        double from = PaleRules.swipeBearing(age - 1.0 - PaleRules.SWIPE_LAG) + 6.0;
        double to = PaleRules.swipeBearing(age - PaleRules.SWIPE_LAG) - 6.0;
        double reach = PaleRules.SWIPE_REACH * Math.max(PaleRules.swipeExtent(age), PaleRules.swipeExtent(age - PaleRules.SWIPE_LAG)) + 0.6;
        AABB area = player.m_20191_().m_82400_(reach + 1.0);
        float damage = PaleRules.fist(KaguneProfile.mastery(player)) * PaleRules.SWIPE_DAMAGE;
        int landed = 0;
        for (LivingEntity target : level.m_6443_(LivingEntity.class, area, entity -> PaleBlitz.valid(player, entity))) {
            if (state.hit.contains(target.m_19879_())) {
                continue;
            }
            Vec3 rel = target.m_20182_().m_82546_(player.m_20182_());
            double flat = Math.sqrt(rel.f_82479_ * rel.f_82479_ + rel.f_82481_ * rel.f_82481_) - target.m_20205_() * 0.5;
            double bearing = Math.toDegrees(Math.atan2(rel.m_82526_(right), rel.m_82526_(forward)));
            if (flat > reach || bearing > from || bearing < to || rel.f_82480_ < -2.0 || rel.f_82480_ > 3.5) {
                continue;
            }
            state.hit.add(target.m_19879_());
            Vec3 centre = KaguneServerController.center(target);
            Vec3 drive = right.m_82490_(-1.0);
            target.f_19802_ = 0;
            WoundHits.expect(target, WoundBook.Blow.SLASH, centre.m_82546_(drive.m_82490_(target.m_20205_() * 0.5)), drive, 0.0);
            if (!target.m_6469_(player.m_269291_().m_269075_(player), damage)) {
                continue;
            }
            landed++;
            // Cut open: it bleeds.
            UkakuCrystalCombat.rinkakuBleed(target, player, level.m_46467_());
            // Thrown hard the way the blade swings (to her left) and away, through weak blocks.
            Vec3 knock = drive.m_82490_(1.45).m_82549_(forward.m_82490_(0.55)).m_82520_(0.0, 0.42, 0.0);
            BlockSmash.launch(player, target, knock, 24);
            PaleBlitz.fx(player, S2CPaleFx.IMPACT, target.m_19879_(), centre, knock, 1.6F, S2CPaleFx.BONE);
            KaguneServerController.playAt(player, target, HexKaguneSounds.FLESH_IMPACT, 1.0F, KaguneServerController.vary(player, 0.75F, 0.06F));
            UkakuFire.sound(level, centre, "entity.player.attack.sweep", 1.0F, KaguneServerController.vary(player, 0.6F, 0.05F));
        }
        if (landed > 0) {
            KaguneProfile.addMastery(player, "Swipe", 1);
            KaguneNetwork.broadcast(player);
            PaleServer.landed(player, landed);
        }
    }

    // ------------------------------------------------------------------ the grab

    static void grabKey(ServerPlayer player, PaleServer.PaleState state, boolean held, int targetId, long now) {
        if (!held) {
            if (state.move == PaleMove.GRAB_HOLD) {
                fling(player, state, now);
            } else if (state.move == PaleMove.GRAB_REACH) {
                state.grabLetGo = true; // thrown the moment it is caught
            }
            return;
        }
        state.grabHeardAt = now;
        if (state.move == PaleMove.GRAB_REACH || state.move == PaleMove.GRAB_HOLD || state.move == PaleMove.GRAB_THROW) {
            return;
        }
        if (!PaleCancel.allowed(state, now) || now < state.grabReadyAt || PaleServer.locked(player, state, "Flesh Grab", PaleRules.GRAB_MASTERY)) {
            return;
        }
        if (!KaguneServerController.useEnergy(player, PaleRules.GRAB_COST, true)) {
            return;
        }
        PaleCancel.cut(player, state, now);
        state.queuedAt = Long.MIN_VALUE;
        state.grabLetGo = false;
        LivingEntity target = PaleMoves.gaze(player, PaleRules.GRAB_RANGE);
        KaguneServerController.playSelf(player, HexKaguneSounds.MANIFEST_RIP, 0.7F, KaguneServerController.vary(player, 1.3F, 0.05F));
        if (target == null) {
            // Nothing there: it lashes out at what she looks at (the first block, or as far as it goes) and snaps back.
            Vec3 eye = player.m_146892_();
            Vec3 end = eye.m_82549_(player.m_20154_().m_82490_(PaleRules.GRAB_RANGE * 0.7));
            BlockHitResult wall = player.m_284548_().m_45547_(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 point = wall.m_6662_() == HitResult.Type.BLOCK ? wall.m_82450_() : end;
            PaleServer.begin(player, state, PaleMove.GRAB_MISS, 0, 0, 0.0F, point, now);
            state.grabReadyAt = now + PaleRules.GRAB_MISS_TICKS + Cooldowns.of(player, 10);
            return;
        }
        double distance = KaguneServerController.center(target).m_82554_(player.m_146892_());
        PaleServer.begin(player, state, PaleMove.GRAB_REACH, 0, target.m_19879_(), PaleRules.grabReachTicks(distance), null, now);
    }

    /** What the flesh holds, while it is still there to hold (alive, in this world, not too far). */
    private static LivingEntity held(ServerPlayer player, PaleServer.PaleState state) {
        int id = state.heldId != 0 ? state.heldId : state.targetId;
        Entity entity = id == 0 ? null : player.m_9236_().m_6815_(id);
        return entity instanceof LivingEntity living && living.m_6084_() && living.m_20280_(player) <= (PaleRules.GRAB_RANGE + 6.0) * (PaleRules.GRAB_RANGE + 6.0)
            ? living : null;
    }

    private static void tickGrab(ServerPlayer player, PaleServer.PaleState state, long now) {
        long age = now - state.startedAt;
        switch (state.move) {
            case GRAB_REACH -> {
                LivingEntity target = held(player, state);
                if (target == null || !PaleBlitz.valid(player, target)) {
                    Vec3 point = target == null ? player.m_146892_().m_82549_(player.m_20154_().m_82490_(6.0)) : KaguneServerController.center(target);
                    PaleServer.begin(player, state, PaleMove.GRAB_MISS, 0, 0, 0.0F, point, now);
                    return;
                }
                if (age >= (long)state.charge) {
                    // Caught: the hand closes round it.
                    state.heldId = target.m_19879_();
                    PaleServer.begin(player, state, PaleMove.GRAB_HOLD, 0, target.m_19879_(), 0.0F, null, now);
                    KaguneServerController.playAt(player, target, HexKaguneSounds.KAGUNE_GRAB, 1.0F, KaguneServerController.vary(player, 0.9F, 0.05F));
                    if (state.grabLetGo) {
                        fling(player, state, now);
                    }
                }
            }
            case GRAB_HOLD -> {
                LivingEntity target = held(player, state);
                if (target == null) {
                    release(player, state);
                    PaleServer.end(player, state);
                    state.grabReadyAt = now + Cooldowns.of(player, PaleRules.GRAB_COOLDOWN / 2);
                    return;
                }
                carry(player, target, holdPoint(player), PULL, PULL_MAX);
                boolean paid = KaguneServerController.useEnergy(player, PaleRules.GRAB_UPKEEP, false);
                if (!paid || now - state.grabHeardAt > PaleRules.HOLD_TIMEOUT || age >= PaleRules.GRAB_HOLD_MAX) {
                    fling(player, state, now);
                }
            }
            case GRAB_THROW -> {
                LivingEntity target = state.heldId == 0 ? null : held(player, state);
                int release = PaleRules.grabRelease();
                if (target != null && age < release) {
                    // Hauled over her shoulder and all the way back, hung there, then whipped over her and out: right where the
                    // throw has it each tick (it moves the whole way, however fast).
                    carry(player, target, around(player, PaleRules.throwAt(age + 1.0, state.thrownFrom)), 1.0, THROW_MAX);
                    if (age == PaleRules.GRAB_PULL + PaleRules.GRAB_HANG) {
                        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 1.0F, KaguneServerController.vary(player, 0.5F, 0.04F));
                    }
                } else if (target != null && age == release) {
                    throwIt(player, state, target);
                }
                if (age >= release + PaleRules.GRAB_RETRACT) {
                    state.heldId = 0;
                    PaleServer.end(player, state);
                }
            }
            case GRAB_MISS -> {
                if (age >= PaleRules.GRAB_MISS_TICKS) {
                    PaleServer.end(player, state);
                }
            }
            default -> {
            }
        }
    }

    /** Let go of J (or no longer able to hold): back over her shoulder, then whipped forward. */
    private static void fling(ServerPlayer player, PaleServer.PaleState state, long now) {
        LivingEntity target = held(player, state);
        if (target == null) {
            release(player, state);
            PaleServer.end(player, state);
            return;
        }
        Vec3 from = KaguneServerController.center(target);
        PaleServer.begin(player, state, PaleMove.GRAB_THROW, 0, target.m_19879_(), 0.0F, from, now);
        state.heldId = target.m_19879_();
        state.thrownFrom = relative(player, from);
        state.grabReadyAt = now + Cooldowns.of(player, PaleRules.GRAB_COOLDOWN);
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_STRAIN, 0.8F, KaguneServerController.vary(player, 1.1F, 0.05F));
    }

    /** The whip's end: the body goes, along her look, at {@link PaleRules#GRAB_THROW_SPEED}, smashing through weak blocks. */
    private static void throwIt(ServerPlayer player, PaleServer.PaleState state, LivingEntity target) {
        Vec3 look = player.m_20154_();
        Vec3 flight = look.m_82490_(PaleRules.GRAB_THROW_SPEED).m_82520_(0.0, 0.15, 0.0);
        target.f_19802_ = 0;
        target.m_6469_(player.m_269291_().m_269075_(player), PaleRules.fist(KaguneProfile.mastery(player)) * PaleRules.GRAB_THROW_DAMAGE);
        BlockSmash.launch(player, target, flight, 40);
        state.heldId = 0;
        KaguneProfile.addMastery(player, "Swipe", 2);
        KaguneNetwork.broadcast(player);
        PaleBlitz.fx(player, S2CPaleFx.IMPACT, target.m_19879_(), KaguneServerController.center(target), flight, 1.6F, S2CPaleFx.FLESH);
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 1.0F, KaguneServerController.vary(player, 0.7F, 0.05F));
    }

    /** Pulls the held body toward {@code at} (its middle there), {@code pull} of the way a tick and at most so fast, its fall forgotten. */
    private static void carry(ServerPlayer player, LivingEntity target, Vec3 at, double pull, double most) {
        Vec3 centre = KaguneServerController.center(target);
        Vec3 gap = at.m_82546_(centre);
        double length = gap.m_82553_();
        Vec3 move = length * pull > most ? gap.m_82490_(most / length) : gap.m_82490_(pull);
        target.m_20256_(move);
        target.f_19864_ = true;
        target.f_19812_ = true;
        target.f_19789_ = 0.0F;
    }

    /** Her frame: blocks to her right, up from her feet, ahead (along her look, level), as a point in the world. */
    private static Vec3 around(ServerPlayer player, double[] at) {
        Vec3 ahead = UkakuFire.horizontal(player.m_20154_());
        Vec3 right = new Vec3(-ahead.f_82481_, 0.0, ahead.f_82479_);
        return player.m_20182_().m_82549_(right.m_82490_(at[0])).m_82549_(ahead.m_82490_(at[2])).m_82520_(0.0, at[1], 0.0);
    }

    /** A point in the world in her frame (as {@link #around} takes it). */
    private static double[] relative(ServerPlayer player, Vec3 point) {
        Vec3 ahead = UkakuFire.horizontal(player.m_20154_());
        Vec3 right = new Vec3(-ahead.f_82481_, 0.0, ahead.f_82479_);
        Vec3 gap = point.m_82546_(player.m_20182_());
        return new double[] {gap.m_82526_(right), gap.f_82480_, gap.m_82526_(ahead)};
    }

    /** Where she holds what she caught: up before her, wherever she looks. */
    private static Vec3 holdPoint(ServerPlayer player) {
        Vec3 ahead = UkakuFire.horizontal(player.m_20154_());
        return player.m_20182_().m_82549_(ahead.m_82490_(PaleRules.GRAB_HOLD_AHEAD)).m_82520_(0.0, PaleRules.GRAB_HOLD_UP, 0.0);
    }

    /** Whatever the flesh holds is let go where it is (dropped, its fall forgotten so far). */
    static void release(ServerPlayer player, PaleServer.PaleState state) {
        if (state.heldId != 0) {
            Entity entity = player.m_9236_().m_6815_(state.heldId);
            if (entity != null) {
                entity.m_20256_(Vec3.f_82478_);
                entity.f_19864_ = true;
                entity.f_19789_ = 0.0F;
            }
        }
        state.heldId = 0;
    }

    static void tick(ServerPlayer player, PaleServer.PaleState state, long now) {
        switch (state.move) {
            case SWIPE -> tickSwipe(player, state, now);
            case GRAB_REACH, GRAB_HOLD, GRAB_THROW, GRAB_MISS -> tickGrab(player, state, now);
            case OWL_GRAB_REACH, OWL_GRAB_HOLD, OWL_GRAB_SLAM, OWL_GRAB_MISS -> {
                // (6.2.4: the Owl's great hand holds on through its own moves, OwlGrab)
            }
            default -> {
                if (state.heldId != 0) {
                    release(player, state);
                }
            }
        }
    }
}
