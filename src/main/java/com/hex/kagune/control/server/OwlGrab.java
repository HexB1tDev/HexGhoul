package com.hex.kagune.control.server;

import com.hex.kagune.control.eto.EtoMove;
import com.hex.kagune.control.eto.EtoRules;
import com.hex.kagune.control.eto.OwlRules;
import com.hex.kagune.control.network.S2CEtoFx;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.network.KaguneNetwork;
import com.hex.kagune.sound.HexKaguneSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 6.2.4, the Owl's great hand (N in it, on the Mutated Arm's mastery, {@link OwlRules}): its right hand reaches for what she looks at, seizes it and holds it up
 * before its jaws while N stays down (worrying at it: a bite every {@link OwlRules#GRAB_CRUSH_EVERY} ticks); let go (or held as long
 * as it can, or out of RC), raised high and slammed down on the ground ahead: the blow, a crater, knocked flat. Tapped, it is slammed
 * the moment it is seized. Nothing there: the hand clutches at the air and comes back.
 */
final class OwlGrab {
    private OwlGrab() {
    }

    /** N held: {@code 2} as the hold starts (the hand reaches out), {@code 1} still down, {@code 0} let go (slammed down). */
    static void key(ServerPlayer player, EtoServer.EtoState state, int down, long now) {
        if (!state.move.grab()) {
            if (down == 2) {
                start(player, state, true, now);
            }
            return;
        }
        if (down != 0) {
            state.grabHeardAt = now;
        } else if (state.move == EtoMove.OWL_GRAB_HOLD) {
            slam(player, state, now);
        } else if (state.move == EtoMove.OWL_GRAB_REACH) {
            state.grabLetGo = true;
        }
    }

    /** N tapped ({@code held} false: slammed the moment it is caught) or its hold begun: the hand reaches, if she can. */
    static void start(ServerPlayer player, EtoServer.EtoState state, boolean held, long now) {
        if (state.move.grab()) {
            return;
        }
        if (!EtoCancel.allowedOwl(state, now) || now < state.armReadyAt
            || EtoServer.locked(player, state, "Mutated Arm", com.hex.kagune.control.eto.ArmRules.MASTERY)) {
            return;
        }
        if (!KaguneServerController.useEnergy(player, OwlRules.GRAB_COST, true)) {
            return;
        }
        EtoCancel.cut(player, state, now);
        state.queuedAt = Long.MIN_VALUE;
        state.grabLetGo = !held;
        state.grabHeardAt = now;
        LivingEntity target = EtoMoves.gaze(player, OwlRules.GRAB_RANGE);
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_STRAIN, 0.8F, KaguneServerController.vary(player, 0.7F, 0.05F));
        if (target == null) {
            Vec3 eye = player.m_146892_();
            Vec3 end = eye.m_82549_(player.m_20154_().m_82490_(OwlRules.GRAB_RANGE * 0.8));
            BlockHitResult wall = player.m_284548_().m_45547_(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 point = wall.m_6662_() == HitResult.Type.BLOCK ? wall.m_82450_() : end;
            EtoServer.begin(player, state, EtoMove.OWL_GRAB_MISS, 0, 0, 0.0F, point, now);
            state.armReadyAt = now + OwlRules.GRAB_MISS_TICKS + Cooldowns.of(player, 10);
            return;
        }
        double distance = KaguneServerController.center(target).m_82554_(player.m_146892_());
        EtoServer.begin(player, state, EtoMove.OWL_GRAB_REACH, 0, target.m_19879_(), OwlRules.grabReachTicks(distance), null, now);
    }

    /** What the hand reaches for or holds, while it is still there to hold (alive, in this world, not too far). */
    private static LivingEntity held(ServerPlayer player, EtoServer.EtoState state) {
        int id = state.heldId != 0 ? state.heldId : state.targetId;
        Entity entity = id == 0 ? null : player.m_9236_().m_6815_(id);
        double most = OwlRules.GRAB_RANGE + 6.0;
        return entity instanceof LivingEntity living && living.m_6084_() && living.m_20280_(player) <= most * most ? living : null;
    }

    static void tick(ServerPlayer player, EtoServer.EtoState state, long now) {
        long age = now - state.startedAt;
        switch (state.move) {
            case OWL_GRAB_REACH -> {
                LivingEntity target = held(player, state);
                if (target == null || !EtoBlitz.valid(player, target)) {
                    Vec3 point = target == null ? player.m_146892_().m_82549_(player.m_20154_().m_82490_(5.0)) : KaguneServerController.center(target);
                    EtoServer.begin(player, state, EtoMove.OWL_GRAB_MISS, 0, 0, 0.0F, point, now);
                    return;
                }
                if (age >= (long)state.charge) {
                    // Seized: the great hand closes round it.
                    state.heldId = target.m_19879_();
                    EtoServer.begin(player, state, EtoMove.OWL_GRAB_HOLD, 0, target.m_19879_(), 0.0F, null, now);
                    KaguneServerController.playAt(player, target, HexKaguneSounds.KAGUNE_GRAB, 1.0F, KaguneServerController.vary(player, 0.6F, 0.05F));
                    EtoStun.stun(target, now, OwlRules.GRAB_HOLD_MAX + OwlRules.GRAB_SLAM_TICKS);
                    if (state.grabLetGo) {
                        slam(player, state, now);
                    }
                }
            }
            case OWL_GRAB_HOLD -> {
                LivingEntity target = held(player, state);
                if (target == null) {
                    EtoFlesh.release(player, state);
                    state.armReadyAt = now + Cooldowns.of(player, OwlRules.GRAB_COOLDOWN / 2);
                    EtoServer.end(player, state);
                    return;
                }
                carry(target, world(player, OwlRules.GRAB_HOLD));
                if (age > 0 && age % OwlRules.GRAB_CRUSH_EVERY == 0) {
                    worry(player, target, now);
                }
                boolean paid = KaguneServerController.useEnergy(player, OwlRules.GRAB_UPKEEP, false);
                if (!paid || now - state.grabHeardAt > EtoRules.HOLD_TIMEOUT || age >= OwlRules.GRAB_HOLD_MAX) {
                    slam(player, state, now);
                }
            }
            case OWL_GRAB_SLAM -> {
                LivingEntity target = state.heldId == 0 ? null : held(player, state);
                if (target != null && age < OwlRules.GRAB_SLAM_IMPACT) {
                    // Raised with the hand, then brought down with it.
                    double u = age < OwlRules.GRAB_SLAM_IMPACT - 2 ? (double)age / (OwlRules.GRAB_SLAM_IMPACT - 2) : 1.0;
                    double[] up = {OwlRules.GRAB_HOLD[0], OwlRules.GRAB_HOLD[1] + 2.0 * u, OwlRules.GRAB_HOLD[2] - 0.6 * u};
                    double[] at = age < OwlRules.GRAB_SLAM_IMPACT - 2 ? up : OwlRules.GRAB_SLAM;
                    carry(target, world(player, at).m_82520_(0.0, target.m_20206_() * 0.5, 0.0));
                }
                if (age == OwlRules.GRAB_SLAM_IMPACT) {
                    smash(player, state, target, now);
                }
                if (age >= OwlRules.GRAB_SLAM_TICKS) {
                    state.heldId = 0;
                    EtoServer.end(player, state);
                }
            }
            case OWL_GRAB_MISS -> {
                if (age >= OwlRules.GRAB_MISS_TICKS) {
                    EtoServer.end(player, state);
                }
            }
            default -> {
            }
        }
    }

    /** Let go of N (or it can hold no longer): raised high and slammed down. */
    private static void slam(ServerPlayer player, EtoServer.EtoState state, long now) {
        LivingEntity target = held(player, state);
        if (target == null) {
            EtoFlesh.release(player, state);
            EtoServer.end(player, state);
            return;
        }
        EtoServer.begin(player, state, EtoMove.OWL_GRAB_SLAM, 0, target.m_19879_(), 0.0F, null, now);
        state.heldId = target.m_19879_();
        state.armReadyAt = now + Cooldowns.of(player, OwlRules.GRAB_COOLDOWN);
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_STRAIN, 0.9F, KaguneServerController.vary(player, 0.6F, 0.05F));
    }

    /** Its jaws worry at what it holds: the hurt, a bleed, the bite's crunch. */
    private static void worry(ServerPlayer player, LivingEntity target, long now) {
        target.f_19802_ = 0;
        float damage = EtoRules.fist(KaguneProfile.mastery(player)) * OwlRules.GRAB_CRUSH_DAMAGE;
        if (target.m_6469_(player.m_269291_().m_269075_(player), damage)) {
            AyatoCrystal.kanekiBleed(target, player, now);
            Vec3 at = KaguneServerController.center(target);
            EtoBlitz.fx(player, S2CEtoFx.IMPACT, target.m_19879_(), at, new Vec3(0.0, -0.4, 0.0), 0.9F, S2CEtoFx.FLESH);
            KaguneServerController.playAt(player, target, HexKaguneSounds.FLESH_IMPACT, 0.8F, KaguneServerController.vary(player, 0.7F, 0.08F));
            AyatoFire.sound(player.m_284548_(), at, "entity.generic.eat", 1.0F, KaguneServerController.vary(player, 0.5F, 0.05F));
        }
    }

    /** The slam lands: the blow, a crater where it struck, the ground's shock, knocked flat and held stunned a moment. */
    private static void smash(ServerPlayer player, EtoServer.EtoState state, LivingEntity target, long now) {
        ServerLevel level = player.m_284548_();
        Vec3 at = world(player, OwlRules.GRAB_SLAM);
        BlockSmash.crater(player, at, OwlRules.GRAB_SLAM_CRATER);
        net.minecraft.core.BlockPos below = net.minecraft.core.BlockPos.m_274561_(at.f_82479_, at.f_82480_ - 0.01, at.f_82481_);
        EtoBlitz.fx(player, S2CEtoFx.SHOCKWAVE, 0, at, AyatoFire.horizontal(player.m_20154_()), 1.6F,
            net.minecraft.world.level.block.Block.m_49956_(level.m_8055_(below)));
        AyatoFire.sound(level, at, "entity.generic.explode", 0.7F, KaguneServerController.vary(player, 0.6F, 0.05F));
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_SLAM, 1.0F, KaguneServerController.vary(player, 0.55F, 0.05F));
        if (target != null) {
            target.f_19802_ = 0;
            WoundHits.expect(target, com.hex.kagune.control.wound.WoundBook.Blow.BLUNT, KaguneServerController.center(target), new Vec3(0.0, -1.0, 0.0), 0.0);
            if (target.m_6469_(player.m_269291_().m_269075_(player), EtoRules.fist(KaguneProfile.mastery(player)) * OwlRules.GRAB_SLAM_DAMAGE)) {
                target.m_20256_(new Vec3(0.0, -0.4, 0.0));
                target.f_19864_ = true;
                EtoStun.stun(target, now, com.hex.kagune.control.eto.StunRules.HEAVY_TICKS);
                EtoKnockdown.launched(target, now);
                EtoBlitz.fx(player, S2CEtoFx.IMPACT, target.m_19879_(), KaguneServerController.center(target), new Vec3(0.0, -1.2, 0.0), 2.0F, S2CEtoFx.BONE);
                KaguneProfile.addMastery(player, "Swipe", 2);
                KaguneNetwork.broadcast(player);
                EtoServer.landed(player, 1);
            }
        }
        state.heldId = 0;
    }

    /** Pulls the held body's middle to {@code at}, its fall forgotten. */
    private static void carry(LivingEntity target, Vec3 at) {
        Vec3 gap = at.m_82546_(KaguneServerController.center(target));
        double length = gap.m_82553_();
        double most = 1.6;
        target.m_20256_(length > most ? gap.m_82490_(most / length) : gap.m_82490_(0.85));
        target.f_19864_ = true;
        target.f_19812_ = true;
        target.f_19789_ = 0.0F;
    }

    /** Her frame (blocks to her right, up from her feet, ahead: level, along her look) as a point in the world. */
    private static Vec3 world(ServerPlayer player, double[] at) {
        Vec3 ahead = AyatoFire.horizontal(player.m_20154_());
        Vec3 right = new Vec3(-ahead.f_82481_, 0.0, ahead.f_82479_);
        return player.m_20182_().m_82549_(right.m_82490_(at[0])).m_82549_(ahead.m_82490_(at[2])).m_82520_(0.0, at[1], 0.0);
    }
}
